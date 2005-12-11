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
    File getRootDir();

    /**
     * Stops all the applications in the container.
     */
    void stop();

    /**
     * Deploys a new application.
     *
     * @param name
     *      name of the new application. The directory of
     */
    void deploy(String name,byte[] data) throws IOException;

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
