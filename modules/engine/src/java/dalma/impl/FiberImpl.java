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
 * Smallest execution unit inside a {@link Conversation}.
 *
 * <h3>Persistence and Fiber</h3>
 * <p>
 * Fiber can be persisted when it's {@link FiberState#CREATED}
 * and {@link FiberState#WAITING}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class FiberImpl<T extends Runnable> extends FiberSPI<T> implements Serializable, ConditionListener {

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

    static class PersistedData<T extends Runnable> implements Serializable {
        private Continuation continuation;
        public final T runnable;

        public PersistedData(T runnable) {
            this.runnable = runnable;
            this.continuation = Continuation.startSuspendedWith(runnable);
        }

        public void execute() {
            continuation = Continuation.continueWith(continuation);
        }

        public boolean isCompleted() {
            return continuation==null;
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * The {@link PersistedData} that includes continuation to be executed.
     */
    private PersistedData<T> execution;

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


    /*package*/ FiberImpl(ConversationImpl owner, T init) {
        this.owner = owner;
        this.id = owner.fiberId.inc();
        this.execution = new PersistedData<T>(init);
        state = FiberState.CREATED;
        assert owner.fibers.size()==id;
        owner.fibers.add(this);
    }

    public T getRunnable() {
        FiberImpl<?> f = currentFiber(false);
        if(f==null)
            throw new IllegalStateException("Cannot be invoked from outside a conversation");
        if(f.owner!=owner)
            throw new IllegalStateException("Cannot be invoked from a fiber that belongs to another conversation");

        assert execution!=null;

        return execution.runnable;
    }

    public void start() {
        if(state!= FiberState.CREATED)
            throw new IllegalStateException("fiber is already started");
        queue();
    }

    public synchronized void join() throws InterruptedException {
        FiberImpl<?> fiber = FiberImpl.currentFiber(false);
        
        if(!StackRecorder.get().isRestoring()) {
            if(getState()==FiberState.ENDED)
                return;

            if(fiber==null) {
                // called from outside conversations
                wait();
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
            execution.execute();
        } catch(Error e) {
            die(e);
        } catch(RuntimeException e) {
            die(e);
        }

        assert state == FiberState.RUNNING;

        if(execution.isCompleted()) {
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
    /*package*/ void hydrate(PersistedData<T> c) {
        assert state!= FiberState.RUNNING;
        assert execution==null;
        assert c!=null;
        execution = c;
    }

    /**
     * Called when the state of the {@link FiberImpl} is being moved to the disk.
     */
    /*package*/ PersistedData<T> dehydrate() {
        assert state==FiberState.RUNNABLE || state==FiberState.WAITING || state==FiberState.ENDED;
        assert execution!=null;
        PersistedData<T> r = execution;
        execution = null;
        return r;
    }

    /**
     * Called when the conversation is restored from the disk.
     */
    /*package*/ void onLoad() {
        if(state== FiberState.CREATED)
            start();
        else
        if(cond!=null)
            cond.onLoad();
        assert execution==null;
        assert state==FiberState.WAITING || state==FiberState.RUNNABLE || state== FiberState.ENDED;
    }

    /**
     * Gets the {@link Fiber} that the current thread is executing.
     *
     * @param mustReturnNonNull
     *      if true and the current thread isn't executing any fiber, this method
     *      throws an exception.
     */
    public static FiberImpl<?> currentFiber(boolean mustReturnNonNull) {
        FiberImpl f = currentFiber.get();
        if(f==null && mustReturnNonNull)
            throw new IllegalStateException("this thread isn't executing a conversation");
        return f;
    }

    /**
     * @see Fiber#create(Runnable)
     */
    public static <T extends Runnable> FiberImpl<T> create(T entryPoint) {
        return new FiberImpl<T>(currentFiber(true).owner,entryPoint);
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
