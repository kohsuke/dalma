package dalma.container;

import dalma.Executor;
import dalma.helpers.Java5Executor;
import dalma.impl.Util;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Root of Dalmacon.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Container implements ContainerMBean {
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

    /**
     * All installed {@link Module}s.
     */
    final List<Module> modules;

    private final Redeployer redeployer;

    protected final MBeanServer mbeanServer;

    /**
     * {@link ClassLoader} that can load lib/*.jar
     */
    final ClassLoader appClassLoader;

    /**
     * {@link Logger} for static methods.
     */
    private static final Logger DEFAULT_LOGGER = Logger.getLogger(Container.class.getName());

    /**
     * {@link Logger} that receives all logs from the whole dalmacon.
     */
    /*package*/ final Logger loggerAggregate = LogUtil.newAnonymousLogger(DEFAULT_LOGGER);

    /**
     * {@link Logger} that just receives container-level events.
     */
    private final Logger logger = LogUtil.newAnonymousLogger(loggerAggregate);

    /**
     * Creates a new container.
     *
     * @param root
     *      Home directory to store data. Must exist.
     * @param executor
     *      used to execute workflow applications.
     */
    public Container(File root, Executor executor) throws IOException {
        this.homeDir = root.getAbsoluteFile();
        this.modules = findModules();
        this.appsDir = new File(homeDir, "apps");
        this.executor = executor;
        this.appClassLoader = createClassLoader();
        this.mbeanServer = ManagementFactory.getPlatformMBeanServer();
        this.applications = findApps();

        for (WorkflowApplication app : applications.values()) {
            try {
                if(app.isConfigured())
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

    /**
     * Returns the {@link Logger} that receives all the events recorded in this container.
     *
     * @return
     *      always the same non-null object.
     */
    public Logger getAggregateLogger() {
        return loggerAggregate;
    }

    /**
     * Returns the {@link Logger} that receives container-level events.
     *
     * @return
     *      always the same non-null object.
     */
    public Logger getLogger() {
        return logger;
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
    private ClassLoader createClassLoader() throws IOException {
        ClassLoaderImpl cl = new ClassLoaderImpl(getClass().getClassLoader());
        // lib/*.jar
        cl.addJarFiles(new File(homeDir,"lib"));

        // modules/*/*.jar
        for (Module mod : modules)
            cl.addJarFiles(mod.dir);

        return cl;
    }

    public void stop() {
        for (WorkflowApplication app : applications.values())
            app.stop();
    }

    public void unload() {
        for (WorkflowApplication app : applications.values()) {
            app.unload();
        }
    }

    public synchronized WorkflowApplication deploy(String name, byte[] data) throws FailedOperationException, InterruptedException {
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

        Future<WorkflowApplication> ft = redeployer.getFuture(new File(appsDir, name));

        File darFile = new File(appsDir,name+".dar");
        if(darFile.exists())
            darFile.delete();
        tmpFile.renameTo(darFile);
        // the rest is up to redeployer to pick up

        try {
            return ft.get(15, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new FailedOperationException("Deployment failed",e.getCause());
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
        if(wa.isConfigured())
            wa.start(); // looks like it's configured enough to start
        // otherwise leave it in the loaded state
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

    private List<Module> findModules() {
        List<Module> r = new ArrayList<Module>();

        File[] modules = new File(homeDir,"modules").listFiles(new FileFilter() {
            public boolean accept(File path) {
                return path.isDirectory();
            }
        });

        if(modules !=null)
            for (File mod : modules)
                r.add(new Module(this,mod));

        return Collections.unmodifiableList(r);
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
        Logger logger = container.logger;

        int jmxPort = container.readProperty(conf, "jmx.port", -1);
        if(jmxPort>=0) {
            try {
                logger.info("Initializing JMXMP connector at port "+jmxPort);
                JMXServiceURL url = new JMXServiceURL("jmxmp", null, jmxPort);
                JMXConnectorServer cs =
                    JMXConnectorServerFactory.newJMXConnectorServer(url, null, ManagementFactory.getPlatformMBeanServer());

                cs.start();
                logger.info("Started JMXMP connector");
            } catch (IOException e) {
                logger.log(Level.WARNING,"Unable to start JMXMP",e);
            }
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
            DEFAULT_LOGGER.severe("Configuration value for "+key+" must be int, but found \""+value+"\"");
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
                DEFAULT_LOGGER.log(Level.SEVERE,"Failed to read "+config,e);
            }
        }
        return props;
    }

    /**
     * Extracts the given dar file into the same directory.
     */
    void explode(File dar) {
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

    static {
        try {
            // avoid cache that causes jar leaks
            new URL("http://dummy/").openConnection().setDefaultUseCaches(false);
        } catch (IOException e) {
            throw new Error(e);
        }

    }
}
