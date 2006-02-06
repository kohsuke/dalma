package dalma.impl;

import dalma.Conversation;
import dalma.ConversationDeath;
import dalma.ConversationState;
import dalma.Fiber;
import dalma.FiberState;
import dalma.Workflow;
import dalma.spi.ConversationSPI;
import org.apache.commons.javaflow.Continuation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Represents a running conversation.
 *
 * TODO: we need a better way for a running user's conversation to expose information
 * to the caller.
 *
 * <p>
 * The monitor of this object is used to notify the completion of a conversation.
 *
 * <h2>Persisting Conversation</h2>
 * <p>
 * There are two different modes of 'persistence' for this object (and fibers.)
 * One is called hydration/dehydration, which is when we just persist the execution state
 * of fibers to the disk to save memory usage (and to improve fault tolerance.)
 * <p>
 * The other is called save/load, which is when we persist the conversation
 * object itself, but excluding the execution state of user code, to prepare
 * for the engine to go down.
 *
 *
 * @author Kohsuke Kawaguchi
 */
public final class ConversationImpl extends ConversationSPI implements Serializable {
    private transient /*final*/ EngineImpl engine;

    /**
     * All the {@link FiberImpl}s that belong to this conversation.
     * Indexed by their id.
     */
    protected final List<FiberImpl> fibers = new Vector<FiberImpl>();

    /**
     * Generates fiber id.
     */
    /*package*/ final Counter fiberId = new Counter();

    /**
     * The number of {@link Continuation}s that are {@link FiberState.RUNNING running} right now.
     */
    // when inc()==0, load state
    // when dec()==0, persist to disk
    transient /*final*/ Counter runningCounts;

    /**
     * The directory to save the state of this conversation.
     */
    private transient /*final*/ File rootDir;

    private final LogRecorder logRecorder;

    /**
     * {@link GeneratorImpl}s that belong to this conversation.
     */
    private Map<UUID,GeneratorImpl> generators = new Hashtable<UUID,GeneratorImpl>();

    /**
     * Other conversations that are blocking for the completion of this conversation.
     *
     * Transient, because {@link ConversationCondition}s in this queue re-register themselves.
     * Always non-null.
     */
    transient Set<ConversationCondition> waitList;

    /**
     * Set to true until the first {@link FiberImpl} runs.
     * This is necessary because the first fiber has in-memory {@link Continuation}.
     */
    private boolean justCreated;

    /**
     * Set to true once the {@link Conversation#remove(Throwable)} operation is in progress.
     *
     * <p>
     * When true, {@link Fiber}s are prevented from being executed,
     * and instead they die. In this way, we can guarantee that all
     * {@link Fiber}s die eventually, to remove the conversation from memory.
     */
    /*package*/ transient boolean isRemoved;

    /**
     * This flag is used to check if the conversation exited abnormally.
     */
    private boolean isAborted;

    /**
     * Synchronization for handling multiple concurrent {@link Conversation#remove(Throwable)} method invocation.
     */
    private transient /*final*/ Object removeLock;

    /**
     * Every conversation gets unique ID (per engine).
     * This is used so that a serialized {@link Conversation}
     * (as a part of the stack frame) can connect back to the running {@link Conversation} instance.
     */
    final int id;

    /**
     * Represents the inner shell of this conversation.
     * Null when this conversation is dehydrated.
     */
    private transient Workflow workflow;

    private String title;

    /**
     * The timestamp when this conversation is created.
     * @see System#currentTimeMillis()
     */
    private final long startDate;

    /**
     * -1 if not completed yet.
     * @see #getCompletionDate()
     */
    private long endDate = -1;

    /**
     * This logger is connected to {@link EngineImpl#logger}, and also to the log recorder
     * of this conversation.
     */
    private transient Logger logger;


    /**
     * Creates a new conversation that starts with the given target.
     */
    ConversationImpl(EngineImpl engine, Workflow target) throws IOException {
        id = engine.generateUniqueId();
        startDate = System.currentTimeMillis();
        File rootDir = new File(engine.getConversationsDir(), String.valueOf(id));
        if(!rootDir.mkdirs())
            throw new IOException("Unable to create "+this.rootDir);

        File logDir = new File(rootDir,"log");
        logDir.mkdirs();
        logRecorder = new LogRecorder(logDir);

        init(engine,rootDir);


        justCreated = true;
        engine.conversations.put(id,this);
        this.workflow = target;
        workflow.setOwner(this);

        // create a persisted data store for this conversation first
        save();

        engine.listeners.onConversationStarted(this);

        // start the first fiber in this conversation.
        // as soon as we call 'start', conversation may end in any minute,
        // so this has to be the last
        FiberImpl<Workflow> f = new FiberImpl<Workflow>(this,target);
        f.start();
    }

    private void init(EngineImpl engine,File rootDir) {
        this.engine = engine;
        this.rootDir = rootDir;
        this.waitList = Collections.synchronizedSet(new HashSet<ConversationCondition>());
        this.runningCounts = new Counter();
        this.removeLock = new Object();
        this.logger = Logger.getAnonymousLogger();
        this.logger.setParent(engine.loggerAggregate);
        this.logger.addHandler(logRecorder);
        this.logger.setLevel(Level.ALL);
    }

    public void addGenerator(GeneratorImpl g) {
        generators.put(g.id,g);
        g.setConversation(this);
        g.onLoad();
    }

    public GeneratorImpl getGenerator(UUID id) {
        return generators.get(id);
    }

    public List<LogRecord> getLog() {
        return logRecorder.getLogs();
    }

    /**
     * Loads a {@link ConversationImpl} object from the disk.
     */
    public static ConversationImpl load(EngineImpl engine, File dir) throws IOException {
        ConversationImpl conv;
        File config = new File(dir, "conversation.xml");

        if(!config.exists()) {
            // bogus directory?
            Util.deleteRecursive(dir);
            throw new FileNotFoundException(config+" not found. deleting this conversation");
        }

        try {
            SerializationContext.set(engine,SerializationContext.Mode.CONVERSATION);
            conv = (ConversationImpl) new XmlFile(config).read(engine.classLoader);
        } finally {
            SerializationContext.remove();
        }
        conv.init(engine,dir);
        for (GeneratorImpl g : conv.generators.values())
            g.onLoad();
        for (FiberImpl fiber : conv.fibers)
            fiber.onLoad();
        return conv;
    }

    private synchronized void save() throws IOException {
        try {
            SerializationContext.set(engine,SerializationContext.Mode.CONVERSATION);
            new XmlFile(new File(rootDir,"conversation.xml")).write(this);
        } finally {
            SerializationContext.remove();
        }
    }

    public int getId() {
        return id;
    }

    /**
     * Gets the current state of the conversation.
     *
     * @return always non-null.
     */
    public ConversationState getState() {
        if(runningCounts.get()!=0)
            return ConversationState.RUNNING;
        if(isAborted)
            return ConversationState.ABORTED;

        ConversationState r = ConversationState.ENDED;

        synchronized(fibers) {
            for (FiberImpl f : fibers) {
                switch(f.getState()) {
                case RUNNABLE:
                    return ConversationState.RUNNABLE;
                case WAITING:
                    r = ConversationState.SUSPENDED;
                    break;
                }
            }
        }
        return r;
    }

    public EngineImpl getEngine() {
        return engine;
    }

    synchronized void onFiberStartedRunning(FiberImpl fiber) {
        if(isRemoved)
            // this conversation is going to be removed now
            // no further execution is allowed
            throw new FiberDeath();

        if(runningCounts.inc()>0)
            // another fiber is already running, and therefore
            // all the fibers are already hydrated. just go ahead and run
            return;

        if(justCreated) {
            // we are about to run the first fiber, and it has in-memory continuation.
            assert fibers.size()==1;
            justCreated = false;
            return;
        }

        File cont = new File(rootDir,"continuation");
        try {
            SerializationContext.set(engine,SerializationContext.Mode.CONTINUATION);

            ObjectInputStream ois = new ObjectInputStreamEx(
                new BufferedInputStream(new FileInputStream(cont)),engine.classLoader);
            List<FiberImpl.PersistedData> list;
            try {
                list = (List<FiberImpl.PersistedData>) ois.readObject();
                assert workflow==null;
                workflow = (Workflow) ois.readObject();
            } finally {
                ois.close();
            }

            if(fibers.size()!=list.size())
                throw new ConversationDeath(list.size()+" fibers are found in the disk but the memory says "+fibers.size()+" fibers",null);
            for (FiberImpl<?> f : fibers) {
                f.hydrate(list.get(f.id));
            }
        } catch (IOException e) {
            runningCounts.dec();
            throw new ConversationDeath("failed to restore the state of the conversation "+cont,e);
        } catch (ClassNotFoundException e) {
            runningCounts.dec();
            throw new ConversationDeath("failed to restore the state of the conversation "+cont,e);
        } finally {
            SerializationContext.remove();
        }
    }

    synchronized void onFiberEndedRunning(FiberImpl fiber,Throwable cause) {
        if(runningCounts.dec()>0)
            return;

        if(getState()==ConversationState.ENDED) {
            // no fiber is there to run. conversation is complete
            remove(cause);
            return;
        }

        // create the object that represents the persisted state
        List<FiberImpl.PersistedData> state = new ArrayList<FiberImpl.PersistedData>(fibers.size());

        for (FiberImpl f : fibers)
            state.add(f.dehydrate());

        // persist the state
        File cont = new File(rootDir,"continuation");
        ObjectOutputStream oos = null;
        try {
            SerializationContext.set(engine,SerializationContext.Mode.CONTINUATION);

            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cont)));
            oos.writeObject(state);
            assert workflow!=null;
            oos.writeObject(workflow);
            workflow = null;
        } catch (IOException e) {
            throw new ConversationDeath("failed to persist the state of the conversation "+cont, e);
        } finally {
            SerializationContext.remove();
            if(oos!=null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        try { // this needs to be done outside the EngineImpl.SERIALIZATION_CONTEXT
            save();
        } catch (IOException e) {
            throw new ConversationDeath("failed to persist the state of the conversation "+cont, e);
        }
    }

    public void remove(Throwable cause) {
        // this lock is to handle multiple concurrent invocations of the remove method
        synchronized(removeLock) {
            // the first thing we have to do is to wait for all the executing fibers
            // to complete. when we are doing that, we don't want new fibers to
            // start executing. We use isRemoved==true for this purpose.
            if(isRemoved)
                return; // already removed.

            isRemoved = true;

            if(cause!=null) {
                getLogger().log(Level.SEVERE, "Conversation is exiting abnormally", cause);
                isAborted = true;
            }

            try {
                runningCounts.waitForZero();
            } catch (InterruptedException e) {
                // can't process it now. later.
                Thread.currentThread().interrupt();
            }

            endDate = System.currentTimeMillis();
            engine.listeners.onConversationCompleted(this);

            synchronized(engine.completionLock) {
                Map<Integer,ConversationImpl> convs = engine.conversations;
                synchronized(convs) {
                    ConversationImpl removed = convs.remove(id);
                    assert removed==this;
                    if(convs.isEmpty()) {
                        engine.completionLock.notifyAll();
                    }
                }
            }

            try {
                Util.deleteRecursive(rootDir);
            } catch (IOException e) {
                // there's really nothing we nor appliation can do to recover from this.
                logger.log(Level.WARNING,"Unable to delete the conversation data directory",e);
            }

            synchronized(this) {
                // remove this conversation from the endPoint
                synchronized(fibers) {
                    for (FiberImpl f : fibers)
                        f.remove();
                    fibers.clear();
                }

                synchronized(generators) {
                    for (GeneratorImpl g : generators.values()) {
                        g.dispose();
                    }
                    generators.clear();
                }

                // notify any threads that are blocked on this conversation.
                // the lock needs to be held before removing all fibers, as
                // that changes the getState() value
                notifyAll();

                // notify all conversations that are blocked on this
                synchronized(waitList) {
                    for (ConversationCondition cd : waitList)
                        cd.activate(this);
                    waitList.clear();
                }
            }
        }
    }

    public synchronized void join() throws InterruptedException {
        FiberImpl<?> fiber = FiberImpl.currentFiber(false);
        if(fiber==null) {
            // called from outside conversations
            if(getState()!=ConversationState.ENDED) {
                wait();
            }
        } else {
            if(this==fiber.owner)
                throw new IllegalStateException("a conversation can't wait for its own completion");
            fiber.suspend(new ConversationCondition(this));
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public Date getStartDate() {
        return new Date(startDate);
    }

    public Date getCompletionDate() {
        if(endDate==-1)
            return null;
        else
            return new Date(endDate);
    }

    public Logger getLogger() {
        return logger;
    }

    private Object writeReplace() {
        if(SerializationContext.get().mode==SerializationContext.Mode.CONVERSATION)
            return this;
        else
            return new ConversationMoniker(id);
    }

    protected FiberImpl getFiber(int id) {
        return fibers.get(id);
    }

    private static final class ConversationMoniker implements Serializable {
        private final int id;

        public ConversationMoniker(int id) {
            this.id = id;
        }

        private Object readResolve() {
            // TODO: what if the id is already removed from engine?
            // we can fix this by allowing Conversation object itself to be persisted
            // (and then readResolve may replace if it's still running),
            // but how do we do about the classLoader field?
            ConversationImpl conv = SerializationContext.get().engine.getConversation(id);
            assert conv!=null;
            return conv;
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Returns a {@link ConversationImpl} instance that the current thread is executing.
     */
    public static ConversationImpl currentConversation() {
        return FiberImpl.currentFiber(true).owner;
    }

    private static final long serialVersionUID = 1L;
}
