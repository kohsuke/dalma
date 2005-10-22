package dalma.endpoints.jbi;

/**
 * Represents an error in the JBI layer.
 *
 * @author Kohsuke Kawaguchi
 */
public class JBIException extends RuntimeException {
    public JBIException() {
    }

    public JBIException(String message) {
        super(message);
    }

    public JBIException(String message, Throwable cause) {
        super(message, cause);
    }

    public JBIException(Throwable cause) {
        super(cause);
    }
}
