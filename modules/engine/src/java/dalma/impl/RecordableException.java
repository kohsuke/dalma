package dalma.impl;

/**
 * Persistable {@link Exception} used to capture exceptions.
 *
 * @author Kohsuke Kawaguchi
 */
public class RecordableException extends Throwable {
    private final String className;

    private RecordableException(Throwable base) {
        super(base.getMessage(),create(base.getCause()));
        className = base.getClass().getName();
        setStackTrace(base.getStackTrace());
    }

    public static RecordableException create(Throwable t) {
        if(t==null) return null;
        else        return new RecordableException(t);
    }

    /**
     * Override to print the original class name.
     */
    public String toString() {
        String s = className;
        String message = getLocalizedMessage();
        return (message != null) ? (s + ": " + message) : s;
    }
}
