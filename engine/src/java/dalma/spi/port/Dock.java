package dalma.spi.port;

import dalma.Conversation;
import dalma.impl.ConversationImpl;
import dalma.spi.ConversationSPI;

import java.io.Serializable;

/**
 * TODO.
 *
 * Derived by endPointoint specific implementation to capture endPointoint-specific information.
 * Intances created by a {@link EndPoint} in a endPoint specific way.
 *
 * <p>
 * Dock stays in a memory even when the continuation is persisted to a disk,
 * to wait for the activation event.
 *
 * <p>
 * Dock is serialized as a part of the conversation, allowing conversation
 * to requeue when the engine is loaded from a file.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Dock<T> implements Serializable {
    /**
     * Owner endPoint.
     */
    public final EndPoint endPoint;

    /**
     * {@link ConversationSPI} parking on this dock.
     */
    public ConversationSPI conv;

    /**
     * Activation value from this dock.
     *
     * Transient because this field is always null when a conversation is serialized.
     */
    private transient T returnValue;

    protected Dock(EndPoint endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * Called after the conversation is parked on this dock.
     *
     * {@link #conv}!=null guaranteed. Typically used to queue
     * this dock.
     * TODO: should this be the same with onLoad?
     */
    public abstract void park();

    /**
     * Called when a {@link Conversation} parking on this endPoint is
     * {@link Conversation#remove() removed}.
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
     * Typically used to requeue this object.
     */
    public abstract void onLoad();

    /**
     * Resumes the conversation parked on this dock.
     * Typically invoked by a endPoint when the conversation should be resumed.
     */
    public final void resume(T retVal) {
        assert conv!=null;
        synchronized(this) {
            this.returnValue = retVal;
        }
        ((ConversationImpl)conv).resume(this);
    }

    public synchronized T getReturnValue() {
        return returnValue;
    }


    private static final long serialVersionUID = 1L;
}
