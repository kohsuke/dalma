package dalma;

import dalma.spi.ConditionListener;

import java.io.Serializable;

/**
 * TODO.
 *
 * Derived by endpoint specific implementation to capture endpoint-specific information.
 * Intances created by a {@link EndPoint} in a endPoint specific way.
 *
 * <p>
 * Condition stays in memory even when the continuation is persisted to a disk,
 * to wait for the activation event.
 *
 * <p>
 * Condition is serialized as a part of the conversation, allowing conversation
 * to requeue when the engine is loaded from a file.
 *
 * <p>
 * Note that an activated {@link Condition} may still be serialized,
 * (which can happen when the engine is persisted before the fiber wakes up.)
 * Thus {@code T} needs to be serializable.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Condition<T> implements Serializable {
    /**
     * Receives activation event.
     */
    private ConditionListener owner;

    /**
     * Activation value from this dock.
     *
     * Transient because this field is always null when a conversation is serialized.
     */
    private transient T returnValue;

    /**
     * Set to true once this {@link Condition} becomes active.
     */
    private boolean isActive = false;

    protected Condition() {
    }

    public final ConditionListener getOwner() {
        return owner;
    }

    public final boolean isActive() {
        return isActive;
    }

    public final void park(ConditionListener owner) {
        this.owner = owner;
        onParked();
    }

    /**
     * Called after the conversation is parked on this dock.
     *
     * {@link #owner}!=null guaranteed. Typically used to queue
     * this dock.
     * TODO: should this be the same with onLoad?
     *
     * TODO: error handling semantics. what happens if the park fails?
     */
    public abstract void onParked();

    /**
     * Called when a {@link Conversation} parking on this endPoint is
     * {@link Conversation#remove(Throwable) removed}.
     *
     * <p>
     * The implementation is expected to remove this conversation
     * from any queue it maintains. If the conversation isn't parked,
     * throw an assertion faillure, as it's a bug in the dalma engine.
     */
    public abstract void interrupt();

    /**
     * Called after this dock is restored from disk.
     *
     * <p>
     * Typically used to requeue this object.
     * This happens while the conversation is being restored from the disk.
     *
     * <p>
     * Note that a {@link Condition} may be serialized even after
     * it gets activated (for example, the conversation may be forced
     * to persist before a fiber that owns it gets a chance to be executed.)
     */
    // TODO: update to work with the new semantics
    public abstract void onLoad();

    /**
     * Resumes the conversation parked on this dock.
     * Typically invoked by an end point when the conversation should be resumed.
     *
     * <p>
     * Note that the resumed conversation may start executed even before this method
     * returns.
     */
    public final void activate(T retVal) {
        synchronized(this) {
            if(isActive)
                throw new IllegalStateException("condition is already active : "+toString());
            isActive = true;
        }
        assert owner!=null;
        synchronized(this) {
            this.returnValue = retVal;
        }
        // ((ConversationImpl)conv).resume(this);
        owner.onActivated(this);
    }

    public final synchronized T getReturnValue() {
        assert isActive;
        return returnValue;
    }


    private static final long serialVersionUID = 1L;
}
