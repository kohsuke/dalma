package dalma.container;

import static dalma.container.WorkflowState.*;
import dalma.Engine;
import dalma.Program;
import dalma.Description;
import dalma.container.model.IllegalResourceException;
import dalma.container.model.InjectionException;
import dalma.container.model.Model;
import dalma.impl.EngineImpl;
import dalma.impl.Util;
import org.apache.commons.javaflow.ContinuationClassLoader;

import javax.management.JMException;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
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

    /**
     * The engine that executes workflow.
     *
     * non-null in {@link WorkflowState#RUNNING} but null in other cases.
     */
    private Engine engine;

    /**
     * The {@link ClassLoader} to load workflow applications.
     *
     * null if {@link WorkflowState#UNLOADED}.
     */
    private ClassLoader classLoader;

    /**
     * The Main class in the {@link #classLoader}.
     *
     * The type isn't checked, hence the parameterization is '?'.
     * null if {@link WorkflowState#UNLOADED}.
     */
    private Class<?> mainClass;

    /**
     * The model of the {@link #mainClass}.
     *
     * Null when {@link #mainClass} is null.
     */
    private Model<?> model;

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

    /**
     * The workflow application. Null when not started.
     */
    private Program program;


    private ObjectName objectName;

    private WorkflowState state;

    public WorkflowApplication(Container owner,File appDir) throws FailedOperationException {
        this.owner = owner;
        this.name = appDir.getName();
        this.workDir = new File(new File(owner.getHomeDir(), "work"), name);
        this.confFile = new File(new File(new File(owner.getHomeDir(), "conf"), "apps"), name+".properties");
        this.appDir  = appDir;
        this.state = UNLOADED;

        try {
            objectName = new ObjectName("dalma:container=" + ObjectName.quote(owner.getHomeDir().toString()) + ",name=" + name);
            MBeanProxy.register( owner.mbeanServer,
                objectName,
                WorkflowApplicationMBean.class, this );
        } catch (JMException e) {
            logger.log(Level.WARNING,"Failed to register to JMX",e);
        }

        load();
    }

    /**
     * Moves the state to {@link WorkflowState#STOPPED}.
     */
    public synchronized void load() throws FailedOperationException {
        if(state!=UNLOADED) return; // nothing to do

        logger.info("Loading "+name);

        classLoader = createClassLoader();

        try {
            mainClass = classLoader.loadClass("Main");
        } catch (ClassNotFoundException e) {
            throw new FailedOperationException("Failed to load the main class from application",e);
        }

        try {
            model = new Model(mainClass);
        } catch (IllegalResourceException e) {
            throw new FailedOperationException("Failed to configure program",e);
        }

        state = STOPPED;

        logger.info("Loaded "+name);
    }

    /**
     * Starts executing the workflow application.
     *
     * <p>
     * Moves the state to {@link WorkflowState#RUNNING}.
     */
    public synchronized void start() throws FailedOperationException {
        load();
        if(state==RUNNING)
            return; // already started

        logger.info("Starting "+name);

        try {
            engine = new EngineImpl(
                new File(workDir,"data"),
                classLoader,
                owner.executor);
        } catch (IOException e) {
            throw new FailedOperationException("Failed to start engine",e);
        }

        try {
            Object main = mainClass.newInstance();
            if(!(main instanceof Program)) {
                logger.severe(mainClass.getName()+" doesn't extend the Program class");
                return;
            }
            program = (Program)main;
        } catch (InstantiationException e) {
            throw new FailedOperationException("Failed to load the main class from application",e);
        } catch (IllegalAccessException e) {
            throw new FailedOperationException("Failed to load the main class from application",e);
        }

        // perform resource injection
        try {
            ((Model)model).inject(program,loadConfigProperties());
        } catch (InjectionException e) {
            throw new FailedOperationException("Failed to configure program",e);
        } catch (ParseException e) {
            throw new FailedOperationException("Failed to configure program",e);
        } catch (IOException e) {
            throw new FailedOperationException("Failed to configure program",e);
        }

        try {
            program.init(engine);
        } catch (Exception e) {
            // faled
            throw new FailedOperationException(mainClass.getName()+".init() method reported an exception",e);
        }

        engine.start();

        try {
            program.main(engine);
        } catch (Exception e) {
            // faled
            throw new FailedOperationException(mainClass.getName()+".main() method reported an exception",e);
        }

        state = RUNNING;

        logger.info("Started "+name);
    }

    /**
     * Loads {@link #confFile} into a {@link Properties}.
     *
     * @return always non-null.
     */
    public Properties loadConfigProperties() throws IOException {
        Properties props = new Properties();
        if(confFile.exists()) {
            InputStream in = new BufferedInputStream(new FileInputStream(confFile));
            try {
                props.load(in);
            } finally {
                in.close();
            }
        }
        return props;
    }

    /**
     * Saves the given propertie sinto {@link #confFile}.
     */
    public void saveConfigProperties(Properties props) throws IOException {
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(confFile));
        try {
            props.store(fos,null);
        } finally {
            fos.close();
        }
    }

    private static void log(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t );
    }

    /**
     * Creates a new {@link ClassLoader} that loads workflow application classes.
     */
    private ClassLoader createClassLoader() {
        try {
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
        } catch (MalformedURLException e) {
            // given that jar files and appDir exists,
            // I don't think this ever happens
            throw new Error(e);
        }
    }

    /**
     * Stops the execution of the workflow application.
     *
     * <p>
     * Moves the state to {@link WorkflowState#STOPPED}.
     */
    public synchronized void stop() {
        if(state!=RUNNING)
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
        state = STOPPED;

        logger.info("Stopped "+name);
    }

    /**
     * Unloads the workflow application from memory.
     *
     * <p>
     * Moves the state to {@link WorkflowState#UNLOADED}.
     */
    public synchronized void unload() {
        stop();
        if(state==UNLOADED)
            return; // nothing to do

        classLoader = null;
        mainClass = null;
        model = null;
        state = UNLOADED;

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
     * Gets the human-readable description of this web application.
     */
    public String getDescription() {
        if(mainClass==null)
            return "(not available for unloaded workflow)";

        Description d = mainClass.getAnnotation(Description.class);
        if(d==null) return "(no description available)";
        return d.value();
    }

    /**
     * Returns true if this application is currently running.
     */
    public WorkflowState getState() {
        return state;
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
        synchronized (undeployLock) {
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
            undeployed = true;
            undeployLock.notifyAll();
        }
    }

    /**
     * Undeploys this workflow application.
     */
    public void undeploy() {
        synchronized (undeployLock) {
            if(!undeployed) {
                try {
                    Util.deleteRecursive(appDir);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "failed to clean up the app directory",e);
                    // this leaves the app dir in an inconsistent state,
                    // but otherwise it's not a fatal error.
                }
                try {
                    undeployLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // process it later
                }
            }
        }

    }

    private Object undeployLock = new Object();
    /**
     * True when this workflow is already undeployed.
     */
    private boolean undeployed = false;
}
