package dalma.webui;

import dalma.container.WorkflowApplication;

/**
 * @author Kohsuke Kawaguchi
 */
public class WWorkflow {
    private final WorkflowApplication core;

    public WWorkflow(WorkflowApplication core) {
        this.core = core;
    }

    public String getName() {
        return core.getName();
    }

    public boolean isRunning () {
        return core.isRunning();
    }
}
