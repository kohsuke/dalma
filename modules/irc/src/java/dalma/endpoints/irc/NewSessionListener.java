package dalma.endpoints.irc;

/**
 * Receives a notification when a new {@link Session} is
 * opened from the other party.
 * @author Kohsuke Kawaguchi
 */
public interface NewSessionListener {
    void onNewPrivateChat(PrivateChat chat);
    void onInvite(Channel channel);
}
