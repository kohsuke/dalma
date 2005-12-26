package dalma.container.model;

/**
 * Signals an error during the injection.
 * 
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
