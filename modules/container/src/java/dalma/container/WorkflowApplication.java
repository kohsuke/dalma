package dalma.container;

import dalma.Conversation;
import dalma.Description;
import dalma.Engine;
import dalma.EngineListener;
import dalma.Program;
import dalma.ErrorHandler;
import static dalma.container.WorkflowState.*;
import dalma.container.model.IllegalResourceException;
import dalma.container.model.InjectionException;
import dalma.container.model.Model;
import dalma.impl.EngineImpl;
import dalma.impl.Util;
import dalma.impl.LogRecorder;

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
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

/**
 * Wrapper around each workflow application.
 *
 * Each workflow application will have a separate {@link Engine}
 * and {@link ClassLoader}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class WorkflowApplication implements WorkflowApplicationMBean {

    private final String name;

    /**
     * The engine that executes workflow.
     *
     * non-null in {@link WorkflowState#RUNNING} but null in other cases.
     */
    private EngineImpl engine;

    /**
     * The {@link ClassLoader} to load workflow applications.
     *
     * null if {@link WorkflowState#UNLOADED}.
     */
    private ClassLoaderImpl classLoader;

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

    /**
     * {@link Logger} that receives all logs from the whole application.
     */
    /*package*/ final Logger loggerAggregate;

    /**
     * {@link Logger} that just receives workflow-level events.
     */
    private final Logger logger;

    private int daysToKeepLog = -1;
    private final LogRotationPolicy logPolicy = new LogRotationPolicy() {
        public boolean keep(Conversation conv) {
            if(daysToKeepLog ==-1)
                return true;    // no rotation

            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_YEAR, -daysToKeepLog);

            return conv.getCompletionDate().getTime() > cal.getTimeInMillis();
        }
    };

    private final CompletedConversationList ccList;

    private final LogRecorder inclusiveLogs;
    private final LogRecorder exclusiveLogs;



    public WorkflowApplication(Container owner,File appDir) throws FailedOperationException {
        this.owner = owner;
        this.name = appDir.getName();
        this.workDir = new File(new File(owner.getHomeDir(), "work"), name);
        this.confFile = new File(new File(new File(owner.getHomeDir(), "conf"), "apps"), name+".properties");
        this.appDir  = appDir;
        this.state = UNLOADED;
        this.loggerAggregate = LogUtil.newAnonymousLogger(owner.loggerAggregate);
        this.logger = LogUtil.newAnonymousLogger(loggerAggregate);

        File clog = new File(workDir, "completed-logs");
        clog.mkdirs();
        this.ccList = new CompletedConversationList(clog);

        File excLog = new File(workDir, "logs/exclusive");
        excLog.mkdirs();
        this.logger.addHandler(exclusiveLogs=new LogRecorder(excLog));

        File incLog = new File(workDir, "logs/inclusive");
        incLog.mkdirs();
        this.loggerAggregate.addHandler(inclusiveLogs=new LogRecorder(incLog));

        try {
            Properties props = loadConfigProperties();
            this.daysToKeepLog = Integer.parseInt(props.getProperty(LOG_ROTATION_KEY,"-1"));
        } catch (IOException e) {
            throw new FailedOperationException("Failed to load configuration "+confFile,e);
        }
        setLogPolicy();

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
     * Sets the # of days the completed conversation logs are kept.
     *
     * @param d
     *      -1 to keep indefinitely.
     */
    public void setDaysToKeepLog( int d ) throws IOException {
        daysToKeepLog = d;
        Properties props = loadConfigProperties();
        props.setProperty(LOG_ROTATION_KEY,String.valueOf(d));
        saveConfigProperties(props);
        setLogPolicy();
    }

    private void setLogPolicy() {
        ccList.setPolicy(logPolicy);
        inclusiveLogs.setDaysToKeepLog(daysToKeepLog);
        exclusiveLogs.setDaysToKeepLog(daysToKeepLog);
    }

    /**
     * Gets the recorded logs.
     */
    public List<LogRecord> getLogs(boolean inclusive) {
        return (inclusive?inclusiveLogs:exclusiveLogs).getLogs();
    }

    /**
     * Gets the # of days the completed conversation logs are kept.
     *
     * Defaults to -1 (keep indefinitely)
     */
    public int getDaysToKeepLog() {
        return daysToKeepLog;
    }

    public boolean isConfigured() {
        if(state==UNLOADED)     return false;
        try {
            return model.checkConfiguration(loadConfigProperties());
        } catch (IOException e) {
            log("Failed to check the configuration",e);
            return false;
        }
    }

    /**
     * Returns the date when this application was deployed.
     */
    public Date getDeployDate() {
        return new Date(appDir.lastModified());
    }

    public synchronized void load() throws FailedOperationException {
        if(state!=UNLOADED) return; // nothing to do

        logger.info("Loading "+name);

        try {
            assert classLoader==null;
            classLoader = createClassLoader();
        } catch (IOException e) {
            throw new FailedOperationException("Failed to set up a ClassLoader",e);
        }

        try {
            mainClass = classLoader.loadMainClass();
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
                engine.setOwner(this);
                engine.setErrorHandler(new ErrorHandler() {
                    public void onError(Throwable t) {
                        logger.log(Level.SEVERE, t.getMessage(), t);
                    }
                });
            } catch (IOException e) {
                throw new FailedOperationException("Failed to start engine",e);
            }

            engine.getAggregateLogger().setParent(loggerAggregate);

            try {
                Object main = mainClass.newInstance();
                if(!(main instanceof Program)) {
                    logger.severe(mainClass.getName()+" doesn't extend the Program class");
                    return;
                }
                program = (Program)main;
                // TODO: replace with a real logger
                program.setLogger(logger);
                program.setEngine(engine);
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
        confFile.getParentFile().mkdirs();
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(confFile));
        try {
            props.store(fos,null);
        } finally {
            fos.close();
        }
    }

    private void log(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t );
    }

    /**
     * Creates a new {@link ClassLoader} that loads workflow application classes.
     */
    private ClassLoaderImpl createClassLoader() throws IOException {
        ClassLoaderImpl cl = new ClassLoaderImpl(owner.appClassLoader);
        cl.addJarFiles(appDir);
        cl.addPathFile(appDir);
        cl.makeContinuable();
        return cl;
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

        classLoader.cleanup();
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
     * Gets the {@link Program} instance of this workflow application.
     *
     * @return
     *      null if the workflow has not started yet.
     */
    public Program getProgram() {
        return program;
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
    public void undeploy() throws FailedOperationException {
        unload();
        synchronized (undeployLock) {
            if(!undeployed) {
                File dar = new File(owner.appsDir, name + ".dar");
                if(dar.exists() && !dar.delete()) {
                    throw new FailedOperationException("failed to delete "+appDir);
                }

                try {
                    Util.deleteRecursive(appDir);
                    Util.deleteRecursive(workDir);
                } catch (IOException e) {
                    throw new FailedOperationException("Unable to clean up the application directory "+appDir,e);
                }
                try {
                    undeployLock.wait(15*1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // process it later
                }
                if(!undeployed)
                    throw new FailedOperationException("Operation timed out");
            }
        }

    }

    private final Object undeployLock = new Object();
    /**
     * True when this workflow is already undeployed.
     */
    private boolean undeployed = false;

    private static final String LOG_ROTATION_KEY = "!log-rotation-days";
}
