package dalma.container;

import java.io.File;

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
    void deploy(String name,byte[] data);
}
