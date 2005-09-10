package dalma.helpers;

import dalma.Executor;

/**
 * {@link Executor} that simply delegates to JDK 5.0's
 * {@link java.util.concurrent.Executor}.
 *
 * @author Kohsuke Kawaguchi
 */
public class Java5Executor implements Executor {
    private final java.util.concurrent.Executor core;

    public Java5Executor(java.util.concurrent.Executor core) {
        this.core = core;
    }

    public void execute(Runnable command) {
        core.execute(command);
    }
}
