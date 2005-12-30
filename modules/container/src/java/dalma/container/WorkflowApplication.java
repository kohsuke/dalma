package dalma.container;

import dalma.Conversation;
import dalma.Description;
import dalma.Engine;
import dalma.EngineListener;
import dalma.Program;
import static dalma.container.WorkflowState.*;
import dalma.container.model.IllegalResourceException;
import dalma.container.model.InjectionException;
import dalma.container.model.Model;
import dalma.impl.EngineImpl;
import dalma.impl.Util;

import javax.management.JMException;
import javax.management.ObjectName;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;
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
     * <tt>DALMA_HOME/apps/&lt;name></tt>
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

    private final CompletedConversationList ccList;

    public WorkflowApplication(Container owner,File appDir) throws FailedOperationException {
        this.owner = owner;
        this.name = appDir.getName();
        this.workDir = new File(new File(owner.getHomeDir(), "work"), name);
        this.confFile = new File(new File(new File(owner.getHomeDir(), "conf"), "apps"), name+".properties");
        this.appDir  = appDir;
        this.state = UNLOADED;

        File clog = new File(workDir, "completed-logs");
        clog.mkdir();
        this.ccList = new CompletedConversationList(clog);

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
            mainClass = classLoader.loadClass(findMainClass());
        } catch (ClassNotFoundException e) {
            throw new FailedOperationException("Failed to load the main class from application",e);
        } catch (IOException e) {
            throw new FailedOperationException("Failed to load the main class from application",e);
        } catch (LinkageError e) {
            throw new FailedOperationException("Failed to load the main class from application",e);
        }

        try {
            model = new Model(mainClass);
        } catch (IllegalResourceException e) {
            throw new FailedOperationException("Failed to configure program",e);
        } catch (LinkageError e) {
            throw new FailedOperationException("Failed to configure program",e);
        }

        state = STOPPED;

        logger.info("Loaded "+name);
    }

    private String findMainClass() throws IOException {
        // determine the Main class name
        Enumeration<URL> res = classLoader.getResources("META-INF/MANIFEST.MF");
        while(res.hasMoreElements()) {
            URL url = res.nextElement();
            InputStream is = new BufferedInputStream(url.openStream());
            try {
                Manifest mf = new Manifest(is);
                String value = mf.getMainAttributes().getValue("Dalma-Main-Class");
                if(value!=null) {
                    logger.info("Found Dalma-Main-Class="+value+" in "+url);
                    return value;
                }
            } finally {
                is.close();
            }
        }

        // default location
        return "Main";
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

        // set the context class loader when configuring the engine
        // and calling into application classes
        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
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
                // TODO: replace with a real logger
                program.setLogger(logger);
            } catch (InstantiationException e) {
                throw new FailedOperationException("Failed to load the main class from application",e);
            } catch (IllegalAccessException e) {
                throw new FailedOperationException("Failed to load the main class from application",e);
            }

            // perform resource injection
            try {
                ((Model)model).inject(engine,program,loadConfigProperties());
            } catch (InjectionException e) {
                throw new FailedOperationException("Failed to configure program",e);
            } catch (ParseException e) {
                throw new FailedOperationException("Failed to configure program",e);
            } catch (IOException e) {
                throw new FailedOperationException("Failed to configure program",e);
            }

            try {
                program.init(engine);
            } catch (Throwable e) {
                // faled
                throw new FailedOperationException(mainClass.getName()+".init() method reported an exception",e);
            }

            // hook things up so that completed conversations will be added to the record
            engine.addListener(new EngineListener() {
                public void onConversationCompleted(Conversation conv) {
                    ccList.add(conv);
                }
            });
            engine.start();

            try {
                program.main(engine);
            } catch (Throwable e) {
                // faled
                throw new FailedOperationException(mainClass.getName()+".main() method reported an exception",e);
            }

            state = RUNNING;

            logger.info("Started "+name);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
            if(state!=RUNNING) {
                // compensation
                program = null;
                if(engine.isStarted())
                    engine.stop();
                engine = null;
            }
        }
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
        ClassLoaderBuilder clb = new ClassLoaderBuilder(owner.appClassLoader);
        clb.addJarFiles(appDir);
        clb.addPathElement(appDir);

        return clb.makeContinuable();
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

        engine.stop();
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

        logger.info("Unloaded "+name);
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
     * Gets the engine that's executing the workflow.
     * <p>
     * The engine is owned by this {@link WorkflowApplication}, so the caller shouldn't
     * try to alter its state.
     */
    public Engine getEngine() {
        return engine;
    }

    /**
     * Gets the model object that describes resources needed by this application.
     */
    public Model<?> getModel() {
        return model;
    }

    /**
     * Gets records about completed conversations.
     *
     * @return
     *      always non-null. Map is keyed by ID.
     */
    public Map<Integer,Conversation> getCompletedConversations() {
        return ccList.getList();
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
     * Completely removes this workflow application.
     */
    public void undeploy() {
        synchronized (undeployLock) {
            if(!undeployed) {
                File dar = new File(owner.appsDir, name + ".dar");
                if(dar.exists() && !dar.delete()) {
                    logger.log(Level.WARNING, "failed to delete "+appDir);
                }

                try {
                    Util.deleteRecursive(appDir);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "failed to delete "+appDir,e);
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

    private final Object undeployLock = new Object();
    /**
     * True when this workflow is already undeployed.
     */
    private boolean undeployed = false;
}
