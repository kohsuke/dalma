package dalma.container;

import dalma.Executor;

import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.JMException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.management.ManagementFactory;

/**
 * Roof ot the dalma container.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Container implements ContainerMBean {
    private static final Logger logger = Logger.getLogger(Container.class.getName());

    /**
     * Root directory of the dalma installation. The value of DALMA_HOME.
     *
     * We want the absolute version since we send this across JMX.
     */
    public final File rootDir;

    /**
     * {@link Executor} that is shared by all {@link WorkflowApplication}s.
     */
    protected final Executor executor;

    private Set<WorkflowApplication> applications;

    public Container(File root, Executor executor) {
        this.rootDir = root.getAbsoluteFile();
        this.executor = executor;
        this.applications = findApps();

        // TODO: monitor the file system and find added applications on the fly

        for (WorkflowApplication app : applications) {
            try {
                app.start();
            } catch (IOException e) {
                logger.log(Level.WARNING,"Failed to start "+app.getName(),e);
            }
        }

        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(this,new ObjectName("dalma:dir="+root));
        } catch (JMException e) {
            logger.log(Level.WARNING,"Failed to register to JMX",e);
        }
    }

    public void stop() {
        for (WorkflowApplication app : applications)
            app.stop();
    }

    public void deploy(String name, byte[] data) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the read-only list of all {@link WorkflowApplication}s
     * in this container.
     */
    public Set<WorkflowApplication> getApplications() {
        return Collections.unmodifiableSet(applications);
    }

    /**
     * Finds all the workflow applications.
     */
    private Set<WorkflowApplication> findApps() {
        File appdir = new File(rootDir, "apps");
        if(!appdir.exists()) {
            logger.severe("Workflow application directory doesn't exist: "+appdir);
            return Collections.emptySet(); // no apps
        }

        File[] subdirs = appdir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if(subdirs==null) {
            assert false; // shall never happen, given that we check for appdir.exists()
            return Collections.emptySet();
        }

        Set<WorkflowApplication> apps = new HashSet<WorkflowApplication>();
        for (File subdir : subdirs) {
            apps.add(new WorkflowApplication(this,subdir));
        }

        return apps;
    }

    public File getRootDir() {
        return rootDir;
    }
}
