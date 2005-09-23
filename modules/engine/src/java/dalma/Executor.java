package dalma;

/**
 * An object that executes the submitted {@link Runnable}s.
 *
 * This interface allows the engine to be used with JDK 5.0's concurrency utilities,
 * without introducing a hard-coded dependency.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Executor {
    void execute(Runnable command);
}
