/**
 * @author Kohsuke Kawaguchi
 */
public class FooException extends Exception {
    public FooException(String message) {
        super(message);
    }

    public FooException(String message, Throwable cause) {
        super(message, cause);
    }
}
