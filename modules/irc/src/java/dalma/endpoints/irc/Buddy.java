package dalma.endpoints.irc;

import dalma.Dock;

/**
 * An IRC user (and an implicit {@link Session} that represents
 * a private communication channel between this user.)
 *
 * @author Kohsuke Kawaguchi
 */
public class Buddy {
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
     * @throws IllegalStateException
     *      If a {@link PrivateChat} is already in progress.
     */
    public synchronized PrivateChat openChat() {
        if(chat!=null)
            throw new IllegalStateException("a chat is already in progress");
        chat = new PrivateChat(endpoint,this);
        return chat;
    }

    synchronized PrivateChat getChat() {
        return chat;
    }

    synchronized void onChatClosed() {
        chat = null;
    }

}
