package dalma.endpoints.irc;

import dalma.Dock;
import dalma.spi.ConversationSPI;

import java.util.LinkedList;
import java.util.List;
import java.io.Serializable;

/**
 * Represents a "place" where the communication happens in IRC.
 *
 * Either {@link Channel} or {@link PrivateChat}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Session implements Serializable {

    /**
     * {@link IRCEndPoint} to which this {@link Session} belongs to.
     */
    protected final IRCEndPoint endpoint;

    /**
     * If a conversation is blocking on the next message from this buddy.
     */
    private Dock<Message> dock;

    /**
     * Messages that are received.
     *
     * Access is synchronized against {@code this}.
     */
    private final List<Message> msgs = new LinkedList<Message>();


    protected Session(IRCEndPoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Sends a message to this {@link Session}.
     */
    public abstract void send(String message);

    /**
     * Blocks until a message is received.
     */
    public synchronized Message waitForNextMessage() {
        while(msgs.isEmpty())
            // block until we get a new message
            ConversationSPI.getCurrentConversation().suspend(
                new MessageDock(endpoint) );

        return msgs.remove(0);
    }

    /**
     * Closes this session and stops receiving futher messages
     * from this {@link Session}.
     */
    public abstract void close();

    /**
     * Called when a new message is received from IRC.
     */
    protected final synchronized void onMessageReceived(Message msg) {
        msgs.add(msg);
        if(dock!=null) {
            dock.resume(msg);
        }
    }

    /**
     * This object needs to be serializable as it's referenced from conversations,
     * we serialize monikers so that they connect back to the running Session instance.
     */
    protected abstract Object writeReplace();

    private final class MessageDock extends Dock<Message> {
        public MessageDock(IRCEndPoint endPoint) {
            super(endPoint);
        }

        public void park() {
            dock = this;
        }

        public void interrupt() {
            assert dock==this;
            dock = null;
        }

        public void onLoad() {
            park();
        }
    }
}
