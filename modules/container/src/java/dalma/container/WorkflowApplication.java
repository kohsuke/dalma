package dalma.container;

import dalma.Engine;
import dalma.Program;
import dalma.container.model.IllegalResourceException;
import dalma.container.model.InjectionException;
import dalma.container.model.Model;
import dalma.impl.EngineImpl;
import org.apache.commons.javaflow.ContinuationClassLoader;

import javax.management.JMException;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper around each workflow application.
 *
 * Each workflow application will have a separate {@link Engine}
 * and {@link ClassLoader}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class WorkflowApplication implements WorkflowApplicationMBean {
    private static final Logger logger = Logger.getLogger(WorkflowApplication.class.getName());

    private final String name;

    private Engine engine;

    /**
     * The {@link Container} that owns this.
     */
    public final Container owner;

    /**
     * Root of the working directory.
     */
    private final File workDir;

    /**
     * Root of the class directory.
     */
    private final File appDir;

    /**
     * Configuration property file.
     */
    private final File confFile;

    private Program program;

    private Model<?> model;

    private ObjectName objectName;

    public WorkflowApplication(Container owner,File appDir) {
        this.owner = owner;
        this.name = appDir.getName();
        this.workDir = new File(new File(owner.getHomeDir(), "work"), name);
        this.confFile = new File(new File(new File(owner.getHomeDir(), "conf"), "apps"), name+".properties");
        this.appDir  = appDir;

        try {
            objectName = new ObjectName("dalma:container=" + ObjectName.quote(owner.getHomeDir().toString()) + ",name=" + name);
            owner.mbeanServer.registerMBean(this,objectName);
        } catch (JMException e) {
            logger.log(Level.WARNING,"Failed to register to JMX",e);
        }
    }

    public synchronized void start() throws IOException {
        if(engine!=null)
            return; // already started

        logger.info("Starting "+name);

        ClassLoader classLoader = createClassLoader();
        engine = new EngineImpl(
            new File(workDir,"data"),
            classLoader,
            owner.executor);

        Class<?> mainClass;
        try {
            mainClass = classLoader.loadClass("Main");
            Object main = mainClass.newInstance();
            if(!(main instanceof Program)) {
                logger.severe(mainClass.getName()+" doesn't extend the Program class");
                return;
            }
            program = (Program)main;
        } catch (ClassNotFoundException e) {
            log("Failed to load the main class from application",e);
            return;
        } catch (InstantiationException e) {
            log("Failed to load the main class from application",e);
            return;
        } catch (IllegalAccessException e) {
            log("Failed to load the main class from application",e);
            return;
        }

        // perform resource injection
        try {
            model = new Model(mainClass);
            ((Model)model).inject(program,loadConfigProperties());
        } catch (InjectionException e) {
            log("Failed to configure program",e);
            return;
        } catch (ParseException e) {
            log("Failed to configure program",e);
            return;
        } catch (IllegalResourceException e) {
            log("Failed to configure program",e);
            return;
        }

        try {
            program.init(engine);
        } catch (Exception e) {
            // faled
            log(mainClass.getName()+".init() method reported an exception",e);
            return;
        }

        engine.start();

        try {
            program.main(engine);
        } catch (Exception e) {
            // faled
            log(mainClass.getName()+".main() method reported an exception",e);
            return;
        }

        logger.info("Started "+name);
    }

    /**
     * Loads {@link #confFile} into a {@link Properties}.
     *
     * @return always non-null.
     */
    private Properties loadConfigProperties() throws IOException {
        Properties props = new Properties();
        if(confFile.exists())
            props.load(new FileInputStream(confFile));
        return props;
    }

    private static void log(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t );
    }

    /**
     * Creates a new {@link ClassLoader} that loads workflow application classes.
     */
    private ClassLoader createClassLoader() throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();

        // list up *.jar files in the appDir
        File[] jarFiles = appDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        for (File jar : jarFiles)
            urls.add(jar.toURI().toURL());

        // and add the workflow application folder itself
        urls.add(appDir.toURI().toURL());

        return new ContinuationClassLoader(
            urls.toArray(new URL[urls.size()]),
            getClass().getClassLoader());
    }

    public synchronized void stop() {
        if(engine==null)
            return; // already stopped

        logger.info("Stopping "+name);

        if(program!=null) {
            try {
                program.cleanup(engine);
            } catch (Exception e) {
                log(program.getClass().getName()+".cleanup() method reported an exception",e);
            }
            program = null;
        }

        try {
            engine.stop();
        } catch (InterruptedException e) {
            // process the interruption later
            Thread.currentThread().interrupt();
        }
        engine = null;

        logger.info("Stopped "+name);
    }

    /**
     * The name of the workflow application
     * that uniquely identifies a {@link WorkflowApplication}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if this application is currently running.
     */
    public synchronized boolean isRunning() {
        return engine!=null;
    }

    /**
     * Gets the location of resource configuration file.
     */
    public File getConfigFile() {
        return confFile;
    }

    /**
     * Gets the model object that describes resources needed by this application.
     */
    public Model<?> getModel() {
        return model;
    }

    /**
     * Called when the directory is removed from the apps folder to remove
     * this application from the container.
     */
    protected synchronized void remove() {
        owner.applications.remove(getName());
        if(objectName!=null) {
            try {
                owner.mbeanServer.unregisterMBean(objectName);
            } catch(JMException e) {
                logger.log(Level.WARNING,"Failed to unregister "+objectName);
            } finally {
                objectName = null;
            }
        }
        stop();
    }
}
