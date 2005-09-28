package dalma.impl;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class GeneratorImpl implements Serializable {

    /**
     * {@link GeneratorImpl} has an unique ID so that it can always deserialize back
     * to the same instance.
     */
    /*package*/ final UUID id = UUID.randomUUID();

    /**
     * Conversation to which this generator belongs to.
     */
    private ConversationImpl conv;


    /**
     * Called after this generator is restored from disk.
     *
     * Typically used to requeue this object.
     * This happens while the conversation is being restored from the disk.
     */
    protected abstract void onLoad();

    /**
     * Called when the conversation completes. Used to dequeue this object.
     */
    protected abstract void interrupt();

    /*package*/ final void setConversation(ConversationImpl conv) {
        assert this.conv==null;
        this.conv = conv;
    }

    protected Object writeReplace() {
        return new Moniker(conv,id);
    }

    private static final class Moniker implements Serializable {
        private final ConversationImpl conv;
        private final UUID id;

        public Moniker(ConversationImpl conv, UUID id) {
            this.conv = conv;
            this.id = id;
        }

        private Object readResolve() {
            // TODO: what if the id is already removed from engine?
            // we can fix this by allowing Conversation object itself to be persisted
            // (and then readResolve may replace if it's still running),
            // but how do we do about the classLoader field?
            return conv.getGenerator(id);
        }

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
