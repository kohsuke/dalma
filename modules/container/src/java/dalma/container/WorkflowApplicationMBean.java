package dalma.container;

import java.io.IOException;
import java.io.File;

/**
 * JMX interface for {@link WorkflowApplication}.
 *
 * @see WorkflowApplication
 * @author Kohsuke Kawaguchi
 */
public interface WorkflowApplicationMBean {
    void start() throws FailedOperationException;
    void stop();
    void unload();
    void load() throws FailedOperationException ;

    void undeploy();

    String getName();
    String getDescription();
    WorkflowState getState();
    File getConfigFile();
}
