package dalma.impl;

import dalma.Condition;
import dalma.Conversation;
import dalma.Fiber;
import dalma.FiberState;
import dalma.spi.ConditionListener;
import dalma.spi.FiberSPI;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.bytecode.StackRecorder;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public final class FiberImpl extends FiberSPI implements Serializable, ConditionListener {

    /**
     * Uniquely identifies {@link FiberImpl} among other fibers that belong to the same owner.
     * Necessary for serialization of the continuation to work correctly.
     */
    final int id;

    /**
     * {@link Conversation} to which this {@link FiberImpl} belongs to.
     */
    final ConversationImpl owner;

    /**
     * Non-null if this {@link FiberImpl}'s execution is blocked on a specific condition
     * (in which case {@link Condition} is not active), or if the {@link Condition}
     * is active but {@link FiberImpl} is waiting for a scheduling.
     */
    private Condition cond;

    /**
     * The {@link Continuation} to be executed.
     */
    private Continuation continuation;

    /**
     * The current state of the {@link FiberImpl}.
     */
    private FiberState state;

    /**
     * Other fibers that are blocking for the completion of this fiber.
     *
     * Transient, because {@link FiberCompletionCondition}s in this queue re-register themselves.
     * Always non-null.
     */
    private transient Set<FiberCompletionCondition> waitList;


    /*package*/ FiberImpl(ConversationImpl owner, Runnable init) {
        this.owner = owner;
        this.id = owner.fiberId.inc();
        this.continuation = Continuation.startSuspendedWith(init);
        state = FiberState.CREATED;
        owner.fibers.add(this);
    }

    public void start() {
        if(state!= FiberState.CREATED)
            throw new IllegalStateException("fiber is already started");
        queue();
    }

    public synchronized void join() throws InterruptedException {
        FiberImpl fiber = FiberImpl.currentFiber();
        
        if(!StackRecorder.get().isRestoring()) {
            if(fiber==null) {
                // called from outside conversations
                if(getState()!= FiberState.ENDED) {
                    wait();
                }
                return;
            }

            if(fiber==this)
                throw new IllegalStateException("a fiber can't wait for its own completion");
        }

        fiber.suspend(new FiberCompletionCondition(this));
    }

    public FiberState getState() {
        return state;
    }

    public ConversationImpl getOwner() {
        return owner;
    }

    private void queue() {
        state = FiberState.RUNNABLE;
        owner.getEngine().queue(this);
    }

    // called by the continuation thread
    public synchronized <T> T suspend(Condition<T> c) {
        if(!StackRecorder.get().isRestoring()) {
            if(c ==null)
                throw new IllegalArgumentException("dock cannot be null");
            assert cond==null;
            cond = c;

            assert state== FiberState.RUNNING;
        }

        Continuation.suspend();
        if(StackRecorder.get().isCapturing()) {
            StackRecorder.get().pushReference(this);
            return null;
        }

        assert cond!=null;
        // assert c==cond;  this isn't correct, because cond is persisted as a part of conversation.xml
        // while c is persisted in the continuation. they are different objects
        T r = (T)cond.getReturnValue();
        cond = null;

        assert state== FiberState.RUNNING;

        return r;
    }

    /**
     * Called from the executor thread to run this fiber until
     * it suspends or completes.
     *
     * This method is synchronized to prevent a still-running conversation
     * from being run again concurrently, which happens when:
     *
     * 1. a dock parks
     * 2. a signal arrives and conversation resumes
     * 3. the conversation gets queued and picked up
     * 4. the conversation gets run
     */
    public synchronized void run() {
        FiberImpl old = currentFiber.get();
        currentFiber.set(this);
        try {
            run0();
        } finally {
            if(old==null)
                currentFiber.remove();
            else
                currentFiber.set(old);
        }
    }
    private void run0() {
        owner.onFiberStartedRunning(this);
        try {
            run1();
        } finally {
            owner.onFiberEndedRunning(this);
        }
    }

    private void run1() {
        assert state== FiberState.RUNNABLE;
        state = FiberState.RUNNING;

        // this runs the conversation until it blocks
        try {
            continuation = Continuation.continueWith(continuation);
        } catch(Error e) {
            die(e);
        } catch(RuntimeException e) {
            die(e);
        }

        assert state == FiberState.RUNNING;

        if(continuation==null) {
            synchronized(this) {
                // conversation has finished execution.
                state = FiberState.ENDED;

                // notify any threads that are blocked on this conversation.
                notifyAll();

                // notify all conversations that are blocked on this
                if(waitList!=null) {
                    synchronized(waitList) {
                        for (FiberCompletionCondition cd : waitList)
                            cd.activate(this);
                        waitList.clear();
                    }
                }
            }

            assert cond==null;
            owner.onFiberCompleted(this);

        } else {
            // conversation has suspended
            state = FiberState.WAITING;
            assert cond!=null;

            // let the condition know that we are parked
            cond.park(this);
        }
    }

    /**
     * Called when a fiber dies unexpectedly in the user code.
     */
    private void die(Throwable t) {
        // this method is supposed to handle an error in the user code,
        // not an unexpected termination inside the engine
        assert state == FiberState.RUNNING;
        state = FiberState.ENDED;

        // clean up if we own a condition
        remove();
        owner.onFiberCompleted(this);
        owner.getEngine().addToErrorQueue(t);
        throw new FiberDeath();
    }

    protected synchronized Set<FiberCompletionCondition> getWaitList() {
        if(waitList==null)
            waitList = Collections.synchronizedSet(new HashSet<FiberCompletionCondition>());
        return waitList;
    }

    /**
     * Called by {@link ConversationImpl} to clean up this fiber
     * (as a part of removing the whole conversation.)
     */
    /*package*/ synchronized void remove() {
        if(cond!=null) {
            cond.interrupt();
            cond = null;
        }
        state = FiberState.ENDED;
    }

    /**
     * Called by the endpoint threads when {@link #cond} becomes active.
     */
    public synchronized void onActivated(Condition cond) {
        assert this.cond==cond;
        assert state== FiberState.WAITING;
        state = FiberState.RUNNABLE;
        queue();
    }

    // TODO: think about synchronization between hydration and activation
    /**
     * Called when the state of the {@link FiberImpl} is being moved to the disk.
     */
    /*package*/ void hydrate(Continuation c) {
        assert state!= FiberState.RUNNING;
        assert continuation==null;
        assert c!=null;
        continuation = c;
    }

    /**
     * Called when the state of the {@link FiberImpl} is being moved to the disk.
     */
    /*package*/ Continuation dehydrate() {
        assert state== FiberState.RUNNABLE || state== FiberState.WAITING;
        assert continuation!=null;
        Continuation r = continuation;
        continuation = null;
        return r;
    }

    /**
     * Called when the conversation is restored from the disk.
     */
    /*package*/ void onLoad() {
        // 'cond' is null only when the fiber is RUNNING
        cond.onLoad();
        assert continuation==null;
        assert state==FiberState.WAITING || state==FiberState.RUNNABLE;
    }

    public static FiberImpl currentFiber() {
        FiberImpl f = currentFiber.get();
        if(f==null)
            throw new IllegalStateException("this thread isn't executing a conversation");
        return f;
    }

    /**
     * @see Fiber#create(Runnable)
     */
    public static FiberImpl create(Runnable entryPoint) {
        return new FiberImpl(currentFiber().owner,entryPoint);
    }

    private Object writeReplace() {
        if(SerializationContext.get().mode==SerializationContext.Mode.CONVERSATION)
            return this;
        else
            return new FiberMoniker(owner,id);
    }

    private static final class FiberMoniker implements Serializable {
        private final ConversationImpl conv;
        private final int id;

        public FiberMoniker(ConversationImpl conv,int id) {
            this.conv = conv;
            this.id = id;
        }

        private Object readResolve() {
            return conv.getFiber(id);
        }

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;

    /**
     * Records the currently running {@link FiberImpl} in the thread.
     */
    private static final ThreadLocal<FiberImpl> currentFiber = new ThreadLocal<FiberImpl>();
}
