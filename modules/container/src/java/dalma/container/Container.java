package dalma.container;

import dalma.Executor;
import dalma.helpers.Java5Executor;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
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
     * The root directory of the dalma installation. The value of DALMA_HOME.
     * We want the absolute version since we send this across JMX.
     */
    private final File homeDir;

    /**
     * {@link Executor} that is shared by all {@link WorkflowApplication}s.
     */
    protected final Executor executor;

    final Map<String,WorkflowApplication> applications;

    private Redeployer redeployer;

    protected final MBeanServer mbeanServer;

    public Container(File root, Executor executor) {
        this.homeDir = root.getAbsoluteFile();
        this.executor = executor;
        this.mbeanServer = ManagementFactory.getPlatformMBeanServer();
        this.applications = findApps();

        // TODO: monitor the file system and find added applications on the fly

        for (WorkflowApplication app : applications.values()) {
            try {
                app.start();
            } catch (FailedOperationException e) {
                logger.log(Level.WARNING,"Failed to start "+app.getName(),e);
            }
        }


        try {
            MBeanProxy.register( mbeanServer,
                new ObjectName("dalma:dir="+ObjectName.quote(homeDir.toString())),
                ContainerMBean.class, this );
        } catch (JMException e) {
            logger.log(Level.WARNING,"Failed to register to JMX",e);
        }

        enableAutoRedeploy();
    }

    private void enableAutoRedeploy() {
        if(redeployer==null) {
            logger.info("Auto-redeployment activated");
            redeployer = new Redeployer(this);
        }
    }

    private void disableAutoRedeploy() {
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

    public synchronized void deploy(String name, byte[] data) throws IOException {
        logger.info("Accepting application '"+name+"' from JMX");
        File tmpFile = new File(getAppDir(),name+".tmp");
        OutputStream os = new FileOutputStream(tmpFile);
        os.write(data);
        os.close();
        File darFile = new File(getAppDir(),name+".dar");
        if(darFile.exists())
            darFile.delete();
        tmpFile.renameTo(darFile);
        // the rest is up to redeployer to pick up
    }

    /**
     * Called when a new directory is created in the 'apps' folder,
     * to create a {@link WorkflowApplication} over it.
     */
    protected WorkflowApplication deploy(File appsubdir) throws FailedOperationException {
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
        File appdir = getAppDir();
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
            try {
                apps.put(subdir.getName(), new WorkflowApplication(this, subdir));
            } catch (FailedOperationException e) {
                logger.log(Level.WARNING,"Failed to load from "+subdir,e);
            }

        return apps;
    }

    /**
     * Gets the 'apps' directory in which the application class files / dar files are stored.
     */
    public File getAppDir() {
        return new File(homeDir, "apps");
    }

    /**
     * Gets the name of the DALMA_HOME directory.
     */
    public File getHomeDir() {
        return homeDir;
    }

    /**
     * Gets the name of the container configuration file.
     */
    public File getConfigFile() {
        return new File(new File(homeDir,"conf"),"dalma.properties");
    }

    /**
     * Creates a configured {@link Container} from HOME/conf/dalma.properties
     */
    public static Container create(File home) throws IOException {
        Properties conf = loadProperties(home);

        Container container = new Container(home, new Java5Executor(
            Executors.newFixedThreadPool(readProperty(conf,"thread.count",5))));

        int jmxPort = readProperty(conf, "jmx.port", -1);
        if(jmxPort>=0) {
            logger.info("Initializing JMXMP connector at port "+jmxPort);
            JMXServiceURL url = new JMXServiceURL("jmxmp", null, jmxPort);
            JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, ManagementFactory.getPlatformMBeanServer());

            cs.start();
            logger.info("Started JMXMP connector");
        }

        return container;
    }

    private static int readProperty( Properties props, String key, int defaultValue ) {
        String value = props.getProperty(key);
        if(value==null)
            return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.severe("Configuration value for "+key+" must be int, but found \""+value+"\"");
            return defaultValue;
        }
    }

    private static File getConfigFile(File home, String name) {
        return new File(new File(home,"conf"),name);
    }

    private static Properties loadProperties(File home) {
        Properties props = new Properties();
        File config = getConfigFile(home,"dalma.properties");
        if(config.exists()) {
            try {
                FileInputStream in = new FileInputStream(config);
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE,"Failed to read "+config,e);
            }
        }
        return props;
    }
}
