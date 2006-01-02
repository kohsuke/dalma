package dalma.container;

import static dalma.container.WorkflowState.UNLOADED;

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

    /**
     * Moves the state to {@link WorkflowState#STOPPED}.
     */
    void load() throws FailedOperationException ;

    void undeploy() throws FailedOperationException;

    String getName();
    String getDescription();
    WorkflowState getState();
    File getConfigFile();

    /**
     * Returns true if this {@link WorkflowApplication} is configured enough
     * to be able to {@link #start() start}.
     *
     * @return
     *      false if some mandatory configuration entries are missing,
     *      or if the current state is {@link WorkflowState#UNLOADED}
     *      (in which case we can't tell if it's configured or not.)
     */
    boolean isConfigured();
}
