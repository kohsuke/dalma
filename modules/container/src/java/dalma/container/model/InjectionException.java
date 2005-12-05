package dalma.container.model;

/**
 * @author Kohsuke Kawaguchi
 */
public class InjectionException extends Exception {
    public InjectionException(String message) {
        super(message);
    }

    public InjectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InjectionException(Throwable cause) {
        super(cause);
    }
}
