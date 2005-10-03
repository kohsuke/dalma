package dalma.endpoints.jms;

/**
 * Represents an error in the JMS layer.
 * 
 * @author Kohsuke Kawaguchi
 */
public class QueueException extends RuntimeException {
    public QueueException() {
    }

    public QueueException(String message) {
        super(message);
    }

    public QueueException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueueException(Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = 1L;
}
