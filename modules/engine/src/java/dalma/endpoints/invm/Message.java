package dalma.endpoints.invm;

import java.io.Serializable;

/**
 * @author Kohsuke Kawaguchi
 */
public final class Message<T> implements Serializable {
    /**
     * Payload of the message.
     */
    public T payload;

    protected Channel from;
    protected Channel to;

    public Message() {
    }

    public Message(T payload) {
        this.payload = payload;
    }

    /**
     * Gets the {@link Channel} from which a message was sent.
     */
    public Channel getFrom() {
        return from;
    }

    /**
     * Gets the {@link Channel} to which a message was sent.
     */
    public Channel getTo() {
        return to;
    }
}
