package dalma;

/**
 * @author Kohsuke Kawaguchi
 */
public enum FiberState {
    CREATED,
    WAITING,
    RUNNING,
    RUNNABLE,
    ABORTED,
    ENDED
}
