package dalma;

import dalma.spi.EngineSPI;


/**
 * @author Kohsuke Kawaguchi
 */
public interface Conversation {
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
     * @throws InterruptedException
     *      If this thread is interrupted while waiting.
     */
    // we don't need exit code for the same reason Thread doesn't need one.
    public void join() throws InterruptedException;
}
