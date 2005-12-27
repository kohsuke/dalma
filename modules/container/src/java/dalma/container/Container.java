package dalma.container;

import dalma.Executor;
import dalma.impl.Util;
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
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Root of Dalmacon.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Container implements ContainerMBean {
    private static final Logger logger = Logger.getLogger(Container.class.getName());

    /**
     * The root directory of the dalma installation. The value of DALMA_HOME.
     * We want the absolute version since we send this across JMX.
     */
    final File homeDir;

    /**
     * DALMA_HOME/apps
     */
    final File appsDir;

    /**
     * {@link Executor} that is shared by all {@link WorkflowApplication}s.
     */
    protected final Executor executor;

    final Map<String,WorkflowApplication> applications;

    private final Redeployer redeployer;

    protected final MBeanServer mbeanServer;

    /**
     * {@link ClassLoader} that can load lib/*.jar
     */
    final ClassLoader appClassLoader;

    public Container(File root, Executor executor) {
        this.homeDir = root.getAbsoluteFile();
        this.appsDir = new File(homeDir, "apps");
        this.executor = executor;
        this.appClassLoader = createClassLoader();
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

        redeployer = new Redeployer(this);
        logger.info("Auto-redeployment activated");
    }

    //private void disableAutoRedeploy() {
    //    if(redeployer!=null) {
    //        redeployer.cancel();
    //        redeployer = null;
    //        logger.info("Auto-redeployment deactivated");
    //    }
    //}

    /**
     * Creates a {@link ClassLoader} that loads all lib/*.jar.
     */
    private ClassLoader createClassLoader() {
        ClassLoaderBuilder clb = new ClassLoaderBuilder(getClass().getClassLoader());
        clb.addJarFiles(new File(homeDir,"lib"));
        return clb.make();
    }

    public void stop() {
        for (WorkflowApplication app : applications.values())
            app.stop();
    }

    public synchronized void deploy(String name, byte[] data) throws FailedOperationException, InterruptedException {
        logger.info("Accepting application '"+name+"'");
        // use a temp file first to hide from auto redeployer
        File tmpFile = new File(appsDir,name+".tmp");
        try {
            OutputStream os = new FileOutputStream(tmpFile);
            try {
                os.write(data);
            } finally {
                os.close();
            }
        } catch (IOException e) {
            throw new FailedOperationException("Failed to write to a file",e);
        }

        Future<FailedOperationException> ft = redeployer.getFuture(new File(appsDir, name));

        File darFile = new File(appsDir,name+".dar");
        if(darFile.exists())
            darFile.delete();
        tmpFile.renameTo(darFile);
        // the rest is up to redeployer to pick up

        try {
            FailedOperationException err = ft.get(15, TimeUnit.SECONDS);
            if(err!=null)
                // wrap to a new exception to get a stack trace that makes sense
                throw new FailedOperationException("Deployment failed",err);
        } catch (ExecutionException e) {
            throw new AssertionError(e);    // impossible
        } catch (TimeoutException e) {
            throw new FailedOperationException("Operation timed out",e);
        }
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
        if(!appsDir.exists()) {
            logger.severe("Workflow application directory doesn't exist: "+appsDir);
            return Collections.emptyMap(); // no apps
        }

        // first extract all dars (unless they are up-to-date)
        File[] dars = appsDir.listFiles(new FileFilter() {
            public boolean accept(File path) {
                return path.getPath().endsWith(".dar");
            }
        });
        for( File dar : dars )
            explode(dar);

        File[] subdirs = appsDir.listFiles(new FileFilter() {
            public boolean accept(File path) {
                return path.isDirectory();
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

    /**
     * Extracts the given dar file into the same directory.
     */
    static void explode(File dar) {
        try {
            String name = dar.getName();
            File exploded = new File(dar.getParentFile(),name.substring(0,name.length()-4));
            if(exploded.exists()) {
                if(exploded.lastModified() > dar.lastModified()) {
                    return;
                }
                Util.deleteRecursive(exploded);
            }

            logger.info("Extracting "+dar);

            byte[] buf = new byte[1024];    // buffer

            JarFile archive = new JarFile(dar);
            Enumeration<JarEntry> e = archive.entries();
            while(e.hasMoreElements()) {
                JarEntry j = e.nextElement();
                File dst = new File(exploded, j.getName());

                if(j.isDirectory()) {
                    dst.mkdirs();
                    continue;
                }

                dst.getParentFile().mkdirs();


                InputStream in = archive.getInputStream(j);
                FileOutputStream out = new FileOutputStream(dst);
                try {
                    while(true) {
                        int sz = in.read(buf);
                        if(sz<0)
                            break;
                        out.write(buf,0,sz);
                    }
                } finally {
                    in.close();
                    out.close();
                }
            }

            archive.close();
        } catch (IOException x) {
            logger.log(Level.SEVERE,"Unable to extract "+dar,x);
            // leave the engine stopped,
            // so that if the user updates the file again, it will restart the engine
        }
    }
}
