package dalma.container;

import dalma.Executor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Roof ot the dalma container.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Container {
    private static final Logger logger = Logger.getLogger(Container.class.getName());

    /**
     * Root directory of the dalma installation. The value of DALMA_HOME.
     */
    public final File rootDir;

    /**
     * {@link Executor} that is shared by all {@link WorkflowApplication}s.
     */
    protected final Executor executor;

    private Set<WorkflowApplication> applications;

    public Container(File root, Executor executor) {
        this.rootDir = root;
        this.executor = executor;
        this.applications = findApps();

        // TODO: monitor the file system and find added applications on the fly

        for (WorkflowApplication app : applications) {
            try {
                app.start();
            } catch (IOException e) {
                logger.log(Level.WARNING,"Failed to start "+app.name,e);
            }
        }
    }

    public void stop() {
        for (WorkflowApplication app : applications)
            app.stop();
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
}
