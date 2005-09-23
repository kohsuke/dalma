package dalma;

/**
 * Signals a irrecoverable death of a conversation.
 *
 * <p>
 * This error happens typically when there was an error while
 * persisting/restoring the state of the conversation.
 *
 * @author Kohsuke Kawaguchi
 */
public class ConversationDeath extends Error {

    private Throwable cause;

    public ConversationDeath(String message,Throwable cause) {
        super(message);
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }
}
