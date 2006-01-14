package dalma.container;

import java.io.File;
import java.io.IOException;

/**
 * JMX interface for {@link Container}.
 *
 * @author Kohsuke Kawaguchi
 */
public interface ContainerMBean {
    /**
     * Gets the root directory of the dalma container.
     * @return never null
     */
    File getHomeDir();

    /**
     * Stops all the applications in the container.
     */
    void stop();

    /**
     * Unloads all the applications in the container.
     */
    void unload();

    /**
     * Deploys a new application.
     *
     * <p>
     * This method blocks until the application is installed.
     * If an application of the same name has been already installed,
     * this method re-deploys it.
     *
     * @param name
     *      name of the new application, such as "foo."
     *      A directory of this name will be created.
     *
     * @throws FailedOperationException
     *      If for some reason the operation failed, such as an I/O error.
     *
     * @return
     *      A {@link WorkflowApplication} object that represents the deployed application.
     *      It maybe in the {@link WorkflowState#RUNNING running} state or in
     *      the {@link WorkflowState#STOPPED stopped} state, depending on various factors,
     *      such as whether the application is configured enough or whether it was running
     *      before (in case of update.)
     */
    WorkflowApplication deploy(String name,byte[] data) throws InterruptedException, FailedOperationException;

    ///**
    // * Enables the auto-redeployment feature.
    // *
    // * <p>
    // * With this switch on, the container checks for addition/deletion/update
    // * in the apps folder, and deploy/undeploy/redeploy applications accordingly.
    // *
    // * <p>
    // * This feature is off by default.
    // */
    //void enableAutoRedeploy();

    ///**
    // * Disables the auto-redeployment feature.
    // *
    // * @see #enableAutoRedeploy()
    // */
    //void disableAutoRedeploy();
}
