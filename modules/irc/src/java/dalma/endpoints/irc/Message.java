package dalma.endpoints.irc;

import java.io.Serializable;

/**
 * Message received from IRC.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Message implements Serializable {
    private final Buddy sender;
    private final String text;
    private final Channel receiver;

    protected Message(Buddy sender, String text, Channel channel) {
        this.sender = sender;
        this.text = text;
        this.receiver = channel;
    }

    /**
     * Gets the {@link Buddy} who sent the message.
     *
     * @return  never null.
     */
    public Buddy getSender() {
        return sender;
    }

    /**
     * Gets the message that was received.
     *
     * @return  never null.
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the {@link Channel} to which this message was sent.
     *
     * @return
     *      null if this message wasn't sent to a channel.
     */
    public Channel getReceiver() {
        return receiver;
    }

    public String toString() {
        return sender.toString()+" : "+text;
    }

    private static final long serialVersionUID = 1L;
}
