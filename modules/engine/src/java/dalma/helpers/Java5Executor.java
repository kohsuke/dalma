package dalma.helpers;

import dalma.Executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link Executor} that simply delegates to JDK 5.0's
 * {@link java.util.concurrent.Executor}.
 *
 * @author Kohsuke Kawaguchi
 */
public class Java5Executor implements Executor {
    private final ExecutorService core;

    public Java5Executor(ExecutorService core) {
        this.core = core;
    }

    public void execute(Runnable command) {
        core.execute(command);
    }

    public void stop(long timeout) throws InterruptedException {
        core.shutdown();
        if(timeout==0) {
            while(!core.isTerminated())
                core.awaitTermination(1000, TimeUnit.SECONDS);
        } else
            core.awaitTermination(timeout, TimeUnit.MILLISECONDS);
    }
}
