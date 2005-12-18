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
    void start() throws IOException;
    void stop();

    void undeploy() throws IOException, InterruptedException;

    String getName();
    String getDescription();
    boolean isRunning();
    File getConfigFile();
}
