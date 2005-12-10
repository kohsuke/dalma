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

    String getName();
    boolean isRunning();
    File getConfigFile();
}
