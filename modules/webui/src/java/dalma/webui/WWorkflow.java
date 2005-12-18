package dalma.webui;

import dalma.container.WorkflowApplication;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class WWorkflow implements UIObject {
    private final WorkflowApplication core;

    public WWorkflow(WorkflowApplication core) {
        this.core = core;
    }

    public String getName() {
        return core.getName();
    }

    public String getDisplayName() {
        return core.getName();
    }

    public String getDescription() {
        return core.getDescription();
    }

    public boolean isRunning () {
        return core.isRunning();
    }

    public String getUrl() {
        return "workflow/"+getName()+'/';
    }


    public static WWorkflow wrap(WorkflowApplication app) {
        if(app==null)   return null;
        return new WWorkflow(app);
    }

    public void doStop(StaplerRequest req, StaplerResponse resp) throws IOException {
        core.stop();
        resp.sendRedirect(".");
    }

    public void doStart(StaplerRequest req, StaplerResponse resp) throws IOException {
        core.start();
        resp.sendRedirect(".");
    }

    public void doDoDelete(StaplerRequest req, StaplerResponse resp) throws IOException {
        core.undeploy();
        resp.sendRedirect(req.getContextPath());
    }
}
