package dalma.container;

import dalma.Executor;

import javax.management.JMException;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    final Map<String,WorkflowApplication> applications;

    private Redeployer redeployer;

    public Container(File root, Executor executor) {
        this.rootDir = root.getAbsoluteFile();
        this.executor = executor;
        this.applications = findApps();

        // TODO: monitor the file system and find added applications on the fly

        for (WorkflowApplication app : applications.values()) {
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

    public void enableAutoRedeploy() {
        if(redeployer==null) {
            logger.info("Auto-redeployment activated");
            redeployer = new Redeployer(this);
        }
    }

    public void disableAutoRedeploy() {
        if(redeployer!=null) {
            redeployer.cancel();
            redeployer = null;
            logger.info("Auto-redeployment deactivated");
        }
    }

    public void stop() {
        for (WorkflowApplication app : applications.values())
            app.stop();
    }

    public void deploy(String name, byte[] data) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    /**
     * Called when a new directory is created in the 'apps' folder,
     * to create a {@link WorkflowApplication} over it.
     */
    protected WorkflowApplication deploy(File appsubdir) throws IOException {
        WorkflowApplication wa = new WorkflowApplication(this, appsubdir);
        applications.put(wa.getName(),wa);
        wa.start();
        return wa;
    }

    /**
     * Returns the read-only list of all {@link WorkflowApplication}s
     * in this container.
     */
    public Collection<WorkflowApplication> getApplications() {
        return Collections.unmodifiableCollection(applications.values());
    }

    public WorkflowApplication getApplication(String name) {
        return applications.get(name);
    }

    /**
     * Finds all the workflow applications.
     */
    private Map<String,WorkflowApplication> findApps() {
        File appdir = new File(rootDir, "apps");
        if(!appdir.exists()) {
            logger.severe("Workflow application directory doesn't exist: "+appdir);
            return Collections.emptyMap(); // no apps
        }

        File[] subdirs = appdir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        Map<String,WorkflowApplication> apps = new Hashtable<String,WorkflowApplication>();

        for (File subdir : subdirs)
            apps.put(subdir.getName(), new WorkflowApplication(this, subdir));

        return apps;
    }

    public File getRootDir() {
        return rootDir;
    }
}
