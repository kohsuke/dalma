package dalma.container.model;

import dalma.Resource;

/**
 * Signals an incorrect placement of {@link Resource}.
 *
 * @author Kohsuke Kawaguchi
 */
public class IllegalResourceException extends Exception {
    public IllegalResourceException(String message) {
        super(message);
    }

    public IllegalResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalResourceException(Throwable cause) {
        super(cause);
    }
}
