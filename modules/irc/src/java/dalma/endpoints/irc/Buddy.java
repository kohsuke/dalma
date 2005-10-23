package dalma.endpoints.irc;

import java.io.Serializable;

/**
 * An IRC user (and an implicit {@link Session} that represents
 * a private communication channel between this user.)
 *
 * @author Kohsuke Kawaguchi
 */
public class Buddy implements Serializable {
    /*package*/ String name;

    private final IRCEndPoint endpoint;

    /**
     * Non-null if there's an active {@link Session} between this buddy.
     * Access needs to be synchronized.
     */
    private PrivateChat chat;

    public Buddy(IRCEndPoint endpoint, String name) {
        this.endpoint = endpoint;
        this.name = name;
    }

    /**
     * Gets the name of this user.
     * @return always non-null.
     */
    public String getName() {
        return name;
    }

    public boolean isOnline() {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
        // TODO: use ISON command
    }

    /**
     * Starts a new private {@link PrivateChat} with this buddy.
     *
     * <p>
     * If a {@link PrivateChat} session is already in progress with
     * this buddy, it will be cut off and a new one is created.
     * The old {@link PrivateChat} object will no longer return
     * new messages.
     */
    public synchronized PrivateChat openChat() {
        if(chat!=null)
            chat.close();
        chat = new PrivateChat(endpoint,this);
        return chat;
    }

    synchronized PrivateChat getChat() {
        return chat;
    }

    synchronized void onChatClosed() {
        chat = null;
    }

    private Object writeReplace() {
        return new Moniker(endpoint,getName());
    }

    private static final class Moniker implements Serializable {
        private final IRCEndPoint endPoint;
        private final String buddyName;

        public Moniker(IRCEndPoint endPoint, String buddyName) {
            this.endPoint = endPoint;
            this.buddyName = buddyName;
        }

        private Object readResolve() {
            return endPoint.getBuddy(buddyName);
        }

        private static final long serialVersionUID = 1L;
    }
}
