package dalma.ports.email;

/**
 * Signals an error in the underlying e-mail handling layer.
 * 
 * @author Kohsuke Kawaguchi
 */
public class EmailException extends RuntimeException {
    public EmailException() {
    }

    public EmailException(String message) {
        super(message);
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailException(Throwable cause) {
        super(cause);
    }
}
