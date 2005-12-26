package dalma;

import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.List;
import java.util.Date;

/**
 * Represents a running instance of a workflow.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Conversation {

    /**
     * Gets a unique number that identifies this conversation among
     * other conversations in the same engine.
     */
    int getId();

    /**
     * Gets the current state of the conversation.
     */
    ConversationState getState();

    /**
     * Gets the engine to which this conversation belongs to.
     */
    Engine getEngine();

    /**
     * Kills a conversation forcibly, even if it's running.
     */
    void remove();

    /**
     * Gets the log that this conversation left.
     *
     * <p>
     *
     */
    List<LogRecord> getLog();

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
    void join() throws InterruptedException;

    /**
     * Gets the title of this conversation.
     *
     * <p>
     * This method returns the last value set by {@link #setTitle(String)}.
     * Initially the value is null.
     *
     * @return
     *      any String. Possibly null.
     */
    String getTitle();

    /**
     * Returns the time when this conversation is created.
     *
     * @return
     *      always non-null.
     */
    Date getStartDate();

    /**
     * Returns the time when this conversation is completed.
     *
     * @return
     *      null if the conversation is not finished yet.
     */
    Date getCompletionDate();
}
