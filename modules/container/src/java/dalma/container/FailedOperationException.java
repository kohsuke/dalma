package dalma.container;

/**
 * Signals a failure in the container operation.
 * 
 * @author Kohsuke Kawaguchi
 */
public class FailedOperationException extends Exception {
    public FailedOperationException(String message) {
        super(message);
    }

    public FailedOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedOperationException(Throwable cause) {
        super(cause);
    }
}
