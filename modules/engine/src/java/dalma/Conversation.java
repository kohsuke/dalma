package dalma;

/**
 * Represents a running instance of a workflow.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Conversation {
    /**
     * Gets the current state of the conversation.
     */
    ConversationState getState();

    /**
     * Gets the engine to which this conversation belongs to.
     */
    public Engine getEngine();

    /**
     * Kills a running conversation.
     */
    public void remove();

    /**
     * Waits for the completion of this conversation.
     *
     * <p>
     * If called from a conversation, the calling conversation
     * suspends and blocks until this conversation exits (and
     * the thread will be reused to execute other conversations.)
     *
     * <p>
     * If called from outside conversations, the calling
     * method simply {@link #wait() waits}.
     *
     * @throws IllegalStateException
     *      If a conversation tries to join itself.
     * @throws InterruptedException
     *      If this thread is interrupted while waiting.
     */
    // we don't need exit code for the same reason Thread doesn't need one.
    public void join() throws InterruptedException;
}
