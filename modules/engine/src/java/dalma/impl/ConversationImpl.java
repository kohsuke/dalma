package dalma.impl;

import dalma.Conversation;
import dalma.ConversationDeath;
import dalma.ConversationState;
import dalma.Fiber;
import dalma.FiberState;
import dalma.spi.ConversationSPI;
import org.apache.commons.javaflow.Continuation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
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
     * Set to true if the {@link #remove()} operation is in progress.
     * When true, {@link Fiber}s are prevented from being executed.
     */
    /*package*/ transient boolean isRemoving;

    /**
     * Synchronization for handling multiple concurrent {@link #remove()} method invocation.
     */
    private transient /*final*/ Object removeLock;

    /**
     * Every conversation gets unique ID (per engine).
     * This is used so that a serialized {@link Conversation}
     * (as a part of the stack frame) can connect back to the running {@link Conversation} instance.
     */
    final int id;

    private static final Logger logger = Logger.getLogger(ConversationImpl.class.getName());

    /**
     * Creates a new conversation that starts with the given target.
     */
    ConversationImpl(EngineImpl engine,Runnable target) throws IOException {
        id = engine.generateUniqueId();
        init(engine,new File(engine.getConversationsDir(),String.valueOf(id)));
        if(!rootDir.mkdirs())
            throw new IOException("Unable to create "+rootDir);

        justCreated = true;
        engine.conversations.put(id,this);

        // save needs to happen before start, or else
        // by the time we save the conversation might be gone.
        save();

        // start the first fiber in this conversation
        FiberImpl f = new FiberImpl(this,target);
        f.start();
    }

    private void init(EngineImpl engine,File rootDir) {
        this.engine = engine;
        this.rootDir = rootDir;
        waitList = Collections.synchronizedSet(new HashSet<ConversationCondition>());
        runningCounts = new Counter();
        removeLock = new Object();
    }

    public void addGenerator(GeneratorImpl g) {
        generators.put(g.id,g);
        g.setConversation(this);
        g.onLoad();
    }

    public GeneratorImpl getGenerator(UUID id) {
        return generators.get(id);
    }

    /**
     * Loads a {@link ConversationImpl} object from the disk.
     */
    public static ConversationImpl load(EngineImpl engine, File dir) throws IOException {
        ConversationImpl conv;
        try {
            SerializationContext.set(engine,SerializationContext.Mode.CONVERSATION);
            conv = (ConversationImpl) new XmlFile(new File(dir,"conversation.xml")).read(engine.classLoader);
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

    /**
     * Gets the current state of the conversation.
     *
     * @return always non-null.
     */
    public ConversationState getState() {
        if(runningCounts.get()!=0)
            return ConversationState.RUNNING;

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
        if(isRemoving)
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
            } finally {
                ois.close();
            }
            cont.delete();

            if(fibers.size()!=list.size())
                throw new ConversationDeath(list.size()+" fibers are found in the disk but the memory says "+fibers.size()+" fibers",null);
            for (FiberImpl f : fibers) {
                f.hydrate(list.get(f.id));
            }
        } catch (IOException e) {
            throw new ConversationDeath("failed to restore the state of the conversation "+cont,e);
        } catch (ClassNotFoundException e) {
            throw new ConversationDeath("failed to restore the state of the conversation "+cont,e);
        } finally {
            SerializationContext.remove();
        }
    }

    synchronized void onFiberEndedRunning(FiberImpl fiber) {
        if(runningCounts.dec()>0)
            return;


        if(getState()== ConversationState.ENDED) {
            // no fiber is there to run. conversation is complete
            remove();
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

    public void remove() {
        // this lock is to allow multiple concurrent invocations of the remove method
        synchronized(removeLock) {
            // the first thing we have to do is to wait for all the executing fibers
            // to complete. when we are doing that, we don't want new fibers to
            // start executing. We use isRemoving==true for this purpose.
            if(isRemoving)
                return; // already removed.

            isRemoving = true;

            try {
                runningCounts.waitForZero();
            } catch (InterruptedException e) {
                // can't process it now. later.
                Thread.currentThread().interrupt();
            }

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
        FiberImpl fiber = FiberImpl.currentFiber(false);
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
