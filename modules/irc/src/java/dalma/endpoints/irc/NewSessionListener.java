package dalma.endpoints.irc;

/**
 * Receives a notification when a new {@link Session} is
 * opened from the other party.
 *
 * @author Kohsuke Kawaguchi
 */
public interface NewSessionListener {
    /**
     * If a {@link Buddy} (with whom we don't have {@link PrivateChat} with)
     * sends us a message, a new {@link PrivateChat} is opened and this method
     * gets invoked.
     *
     * <p>
     * If the application decides to ignore the buddy, call {@link PrivateChat#close()}
     * to terminate the session immediately.
     *
     * @param chat
     *      the newly created {@link PrivateChat} instance that represents
     *      the conversation just started. The message received can be obtained
     *      by calling {@link PrivateChat#waitForNextMessage()}.
     */
    void onNewPrivateChat(PrivateChat chat);

    /**
     * If someone invites us to a {@link Channel}. this method is invoked.
     *
     * <p>
     * If the application is not interested in joining, it can just return from this method
     * without doing anything. Or it can call {@link Channel#join()} to join this channel.
     *
     * @param sender
     *      The {@link Buddy} who sent us the invitation. never null.
     * @param channel
     *      The {@link Channel} to which we are invited to.
     */
    void onInvite(Buddy sender, Channel channel);
}
