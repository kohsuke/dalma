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

    /**
     * Shuts down the executor.
     *
     * This method blocks until all the scheduled {@link Runnable}s are completed,
     * or the timeout occurs.
     *
     * @param timeout
     *      Number of milliseconds to wait. 0 for no timeout.
     */
    void stop(long timeout) throws InterruptedException;
}
