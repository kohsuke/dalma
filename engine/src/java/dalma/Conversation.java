package dalma;



/**
 * @author Kohsuke Kawaguchi
 */
public interface Conversation {
    ConversationState getState();

    /**
     * Kills a running conversation.
     */
    public void remove();

    /**
     * Waits for the completion of this conversation.
     *
     * @throws InterruptedException
     *      If this thread is interrupted while waiting.
     */
    // we don't need exit code for the same reason Thread doesn't need one.
    public void join() throws InterruptedException;
}
