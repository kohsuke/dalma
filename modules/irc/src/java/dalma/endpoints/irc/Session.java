package dalma.endpoints.irc;

import dalma.Dock;

import java.util.List;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Represents a "place" where the communication happens in IRC.
 *
 * Either {@link Channel} or {@link PrivateChat}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Session {

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
     */
    private final List<Message> msgs = Collections.synchronizedList(new LinkedList<Message>());


    protected Session(IRCEndPoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Sends a message.
     */
    public void send(String message) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    /**
     * Blocks until a message is received.
     */
    public String waitForNextMessage() {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
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
}
