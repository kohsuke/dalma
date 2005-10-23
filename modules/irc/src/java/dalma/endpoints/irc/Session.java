package dalma.endpoints.irc;

/**
 * Represents a "place" where the communication happens in IRC.
 *
 * Either {@link Channel} or {@link Buddy}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Session {

    /**
     * {@link IRCEndPoint} to which this {@link Session} belongs to.
     */
    protected final IRCEndPoint endpoint;

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
}
