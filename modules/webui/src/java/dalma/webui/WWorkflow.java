package dalma.webui;

import dalma.container.WorkflowApplication;
import dalma.container.WorkflowState;
import dalma.container.FailedOperationException;
import dalma.container.model.Model;
import dalma.Engine;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.Properties;
import java.util.Map;

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
        return core.getState()==WorkflowState.RUNNING;
    }

    public String getUrl() {
        return "workflow/"+getName()+'/';
    }

    public Model getModel() {
        return core.getModel();
    }

    public String getConversationSize() {
        Engine engine = core.getEngine();
        return engine!=null ? String.valueOf(engine.getConversationsSize()) : "N/A";
    }

    /**
     * Has to be named as "get" to make JSTL happy. Ugly.
     */
    public Properties getConfigProperties() throws IOException {
        return core.loadConfigProperties();
    }


    public static WWorkflow wrap(WorkflowApplication app) {
        if(app==null)   return null;
        return new WWorkflow(app);
    }

    public void doStop(StaplerRequest req, StaplerResponse resp) throws IOException {
        core.stop();
        resp.sendRedirect(".");
    }

    public void doStart(StaplerRequest req, StaplerResponse resp) throws IOException, FailedOperationException {
        // TODO: report failed start operation correctly
        core.start();
        resp.sendRedirect(".");
    }

    public void doDoDelete(StaplerRequest req, StaplerResponse resp) throws IOException {
        core.undeploy();
        resp.sendRedirect(req.getContextPath());
    }

    /**
     * Accepts the configuration page submission.
     */
    public void doPostConfigure(StaplerRequest req, StaplerResponse resp) throws IOException {
        // TODO: report failed start operation correctly
        Properties props = core.loadConfigProperties();
        for( Map.Entry<String,String[]> e : ((Map<String,String[]>)req.getParameterMap()).entrySet() ) {
            String name = e.getKey();
            if(!name.startsWith("config-"))
                continue;
            name = name.substring(7);
            props.put(name,e.getValue()[0]);
        }
        core.saveConfigProperties(props);

        resp.sendRedirect(".");
    }
}
