package dalma.impl;

import dalma.Conversation;
import dalma.ConversationDeath;
import dalma.EndPoint;
import dalma.Engine;
import dalma.ErrorHandler;
import dalma.Executor;
import dalma.Workflow;
import dalma.endpoints.timer.TimerEndPoint;
import dalma.spi.EngineSPI;
import org.apache.bsf.BSFManager;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Date;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * {@link Engine} implementation.
 *
 * @author Kohsuke Kawaguchi
 */
public final class EngineImpl extends EngineSPI implements Serializable {

    /**
     * Logger that logs events.
     */
    private transient Logger logger = Logger.getLogger(EngineImpl.class.getName());

    /**
     * Executes conversations that can be run.
     */
    private transient final Executor executor;

    /**
     * Root directory of the system.
     */
    private transient final File rootDir;

    /**
     * Generates the unique ID.
     */
    private final SequenceGenerator idGen = new SequenceGenerator();

    /**
     * ClassLoader used to restore conversations.
     *
     * TODO: allow each conversation to have its own class loader,
     * but this has an issue in the restoration.
     *
     * Transient because ClassLoaders can't be serialized in general.
     */
    /*package*/ final transient ClassLoader classLoader;

    /**
     * All conversations that belong to this engine.
     * access need to be synchronized.
     * Keyed by their {@link ConversationImpl#id}.
     */
    transient final Map<Integer,ConversationImpl> conversations = new Hashtable<Integer,ConversationImpl>();

    /**
     * All {@link EndPoint}s that bleong to this engine.
     * access need to be synchronized.
     */
    transient final Map<String,EndPointImpl> endPoints = new Hashtable<String,EndPointImpl>();


    /**
     * Signals when all the conversation completes.
     */
    transient final Object completionLock = new Object();

    /**
     * This lock is used to control "dalma.xml" access.
     */
    transient final Object saveLoadLock = new Object();

    /**
     * True once the engine is started.
     */
    transient private boolean started;

    /**
     * Possibly null {@link ErrorHandler}.
     */
    transient private ErrorHandler errorHandler;

    /**
     * See {@link #getLastActiveTime()}
     */
    private long lastActiveTime = 0;

    public EngineImpl(File rootDir,ClassLoader classLoader,Executor executor) throws IOException {
        this.rootDir = rootDir;
        this.executor = executor;
        this.classLoader = classLoader;
        setLogger(Logger.getLogger(getClass().getName()));
        load();

        addEndPoint(new TimerEndPoint());
    }

    /**
     * Loads the configuration from disk.
     */
    private void load() throws IOException {
        synchronized(saveLoadLock) {
            XmlFile df = getDataFile();
            if(df.exists()) {
                // load data into this object
                df.unmarshal(this);
            }
        }
    }

    /**
     * Loads persisted conversations from disk.
     */
    private void loadConversations() {
        // restore conversations
        File convDir = getConversationsDir();
        convDir.mkdirs();
        File[] subdirs = convDir.listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.isDirectory();
            }
        });

        for( File subdir : subdirs ) {
            try {
                ConversationImpl conv = ConversationImpl.load(this, subdir);
                conversations.put(conv.id,conv);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to load conversation "+subdir,e);
            }
        }
    }

    /**
     * Directory to store conversations.
     */
    File getConversationsDir() {
        return new File(rootDir,"conversations");
    }

    /**
     * Persists ths state of this object (but not conversations)
     * into the data file.
     */
    private void save() throws IOException {
        synchronized(saveLoadLock) {
            try {
                SerializationContext.set(this,SerializationContext.Mode.ENGINE);
                getDataFile().write(this);
            } finally {
                SerializationContext.remove();
            }
        }
    }

    /**
     * Name of the data file to persist this object.
     */
    private XmlFile getDataFile() {
        return new XmlFile(new File(rootDir,"dalma.xml"));
    }

    /**
     * Generates unique IDs for {@link ConversationImpl}.
     */
    int generateUniqueId() throws IOException {
        int r = idGen.next();
        save();
        return r;
    }

    /**
     * Queues a conversation that became newly runnable.
     */
    void queue(final FiberImpl f) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    lastActiveTime = System.currentTimeMillis();
                    f.run();
                } catch(FiberDeath t) {
                    // this fiber is dead!
                } catch(ConversationDeath t) {
                    // some fatal error caused the conversation to die.
                    // report the error first before removing the conversation,
                    // which might cause the engine to signal "we are done!" event.
                    addToErrorQueue(t.getCause());
                    f.owner.remove();
                } catch(Throwable t) {
                    // even if the error recovery process fails,
                    // don't let the worker thread die.
                    addToErrorQueue(t);
                } finally {
                    lastActiveTime = System.currentTimeMillis();
                }
            }
        });
    }

    protected void addToErrorQueue(Throwable t) {
        if(errorHandler==null)
            ErrorHandler.DEFAULT.onError(t);
        else
            errorHandler.onError(t);
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public synchronized Collection<Conversation> getConversations() {
        makeSureStarted();
        ArrayList<Conversation> convs = new ArrayList<Conversation>(conversations.values());
        Collections.sort(convs,ID_COMPARATOR);
        return convs;
    }

    public int getConversationsSize() {
        return conversations.size();
    }

    public Date getLastActiveTime() {
        return new Date(lastActiveTime);
    }

    public Map<String,EndPoint> getEndPoints() {
        synchronized(endPoints) {
            return Collections.<String,EndPoint>unmodifiableMap(endPoints);
        }
    }

    public EndPoint getEndPoint(String name) {
        return endPoints.get(name);
    }

    public void addEndPoint(EndPoint ep) {
        makeSureNotStarted();
        synchronized(endPoints) {
            if(endPoints.containsKey(ep.getName()))
                throw new IllegalArgumentException("There's already an EndPoint of the name "+ep.getName());
            if(!(ep instanceof EndPointImpl))
                throw new IllegalArgumentException(ep.getClass().getName()+" doesn't derive from EndPointImpl");
            endPoints.put(ep.getName(),(EndPointImpl)ep);
        }
    }

    public synchronized EndPoint addEndPoint(String name, String endpointURL) throws ParseException {
        EndPoint ep = EndPoint.create(name, endpointURL);
        addEndPoint(ep);
        return ep;
    }

    public Map<String, EndPoint> addEndPoints(Properties endpointURLs) throws ParseException {
        Map<String,EndPoint> r = new TreeMap<String,EndPoint>();

        for (Map.Entry e : endpointURLs.entrySet()) {
            EndPoint ep = addEndPoint(e.getKey().toString(), e.getValue().toString());
            r.put(ep.getName(),ep);
        }

        return r;
    }

    public void configureWithBSF(File scriptFile) throws IOException {
        Reader f = new FileReader(scriptFile);
        try {
            try {
                BSFManager bsfm = new BSFManager();
                bsfm.declareBean("engine",this, Engine.class);
                String language = BSFManager.getLangFromFilename(scriptFile.getPath());
                bsfm.exec(language,scriptFile.getPath(),1,1,IOUtils.toString(f));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // we'd really like to just catch BSFException, but if I do that,
                // we'll need BSF just to load this class
                IOException x = new IOException(e.getMessage());
                x.initCause(e);
                throw x;
            }
        } finally {
            f.close();
        }
    }

    public void start() {
        makeSureNotStarted();
        started = true;
        synchronized(endPoints) {
            for (EndPointImpl ep : endPoints.values())
                ep.start();
        }
        loadConversations();
    }

    private void makeSureStarted() {
        if(!started)
            throw new IllegalStateException("engine is not started");
    }

    private void makeSureNotStarted() {
        if(started)
            throw new IllegalStateException("engine is already started");
    }

    public void stop() {
        makeSureStarted();

        // clone first to avoid concurrent modification
        Collection<EndPointImpl> eps;
        synchronized(endPoints) {
            eps = new ArrayList<EndPointImpl>(endPoints.values());
        }

        for( EndPointImpl ep : eps )
            ep.stop();

        // write any pending changes
        try {
            save();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save state",e);
        }
    }

    public void setLogger(Logger logger) {
        if(logger==null) {
            // use unconnected anonymous logger to ignore log
            logger = Logger.getAnonymousLogger();
            logger.setUseParentHandlers(false);
        }
        this.logger = logger;
    }

    public void waitForCompletion() throws InterruptedException {
        makeSureStarted();
        synchronized(completionLock) {
            while(!conversations.isEmpty())
                completionLock.wait();
            }
    }

    ConversationImpl getConversation(int id) {
        makeSureStarted();
        return conversations.get(id);
    }

    public ConversationImpl createConversation(Runnable target) throws IOException {
        return createConversation(new RunnableWorkflowImpl(target));
    }

    public ConversationImpl createConversation(Workflow workflow) throws IOException {
        makeSureStarted();
        return new ConversationImpl(this,workflow);
    }

    private Object writeReplace() {
        if(SerializationContext.get().mode!=SerializationContext.Mode.ENGINE)
            // if the engine is written as a part of dehydration,
            // return a moniker to avoid the whole engine to be serialized.
            return MONIKER;
        else
            // otherwise we are serializing the whole engine.
            return this;
    }

    private static final long serialVersionUID = 1L;

    private static final class EngineMoniker implements Serializable {
        private static final long serialVersionUID = 1L;
        private Object readResolve() {
            return SerializationContext.get().engine;
        }
    }

    private static final EngineMoniker MONIKER = new EngineMoniker();

    private static final Comparator<Conversation> ID_COMPARATOR = new Comparator<Conversation>() {
        public int compare(Conversation lhs, Conversation rhs) {
            return lhs.getId()-rhs.getId();
        }
    };
}
