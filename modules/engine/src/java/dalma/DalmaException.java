package dalma;

/**
 * Represents a generally unexpected error condition that cannot be meaningfully recovered.
 *
 * @author Kohsuke Kawaguchi
 */
public class DalmaException extends RuntimeException {
    public DalmaException(String message) {
        super(message);
    }

    public DalmaException(String message, Throwable cause) {
        super(message, cause);
    }

    public DalmaException(Throwable cause) {
        super(cause);
    }
}
