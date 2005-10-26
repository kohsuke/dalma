package dalma.impl;

import dalma.Condition;
import dalma.Conversation;
import dalma.FiberState;
import dalma.spi.ConditionListener;
import dalma.spi.FiberSPI;

import java.io.Serializable;

import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.bytecode.StackRecorder;

/**
 * @author Kohsuke Kawaguchi
 */
public final class FiberImpl extends FiberSPI implements Serializable, ConditionListener {

    /**
     * Uniquely identifies {@link FiberImpl} among other fibers that belong to the same owner.
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

    public FiberImpl(ConversationImpl owner, int id, Continuation init) {
        this.owner = owner;
        this.id = id;
        this.continuation = init;
        queue();
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
        assert c==cond;
        T r = c.getReturnValue();
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
            // handle unexpected death
            onDied();
            throw e;
        } catch(RuntimeException e) {
            onDied();
            throw e;
        }

        assert state == FiberState.RUNNING;

        if(continuation==null) {
            // conversation has finished execution.
            state = FiberState.ENDED;
            assert cond==null;
            owner.onFiberCompleted(this);
        } else {
            // conversation has suspended
            state = FiberState.WAITING;
            assert cond!=null;

            // let the condition know that we are parked
            cond.onParked();
        }
    }

    /**
     * Called when a fiber's execution died unexpectedly.
     */
    private void onDied() {
        // this method is supposed to handle an error in the user code,
        // not an unexpected termination inside the engine
        assert state == FiberState.RUNNING;

        // clean up if we own a condition
        remove();
        owner.onFiberCompleted(this);
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

    private static final long serialVersionUID = 1L;

    /**
     * Records the currently running {@link FiberImpl} in the thread.
     */
    private static final ThreadLocal<FiberImpl> currentFiber = new ThreadLocal<FiberImpl>();
}
