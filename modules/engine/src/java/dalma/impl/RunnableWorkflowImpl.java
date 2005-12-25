package dalma.impl;

import dalma.Workflow;

/**
 * {@link Workflow} implementation that wraps a {@link Runnable}.
 *
 * @author Kohsuke Kawaguchi
 */
final class RunnableWorkflowImpl extends Workflow {
    private final Runnable runnable;

    public RunnableWorkflowImpl(Runnable runnable) {
        this.runnable = runnable;
    }

    public void run() {
        runnable.run();
    }

    private static final long serialVersionUID = 1L;
}
