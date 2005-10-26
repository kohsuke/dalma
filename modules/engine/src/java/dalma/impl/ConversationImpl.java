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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
 * @author Kohsuke Kawaguchi
 */
public final class ConversationImpl extends ConversationSPI implements Serializable {
    private transient /*final*/ EngineImpl engine;

    /**
     * All the {@link FiberImpl}s that belong to this conversation.
     */
    private Set<FiberImpl> fibers = Collections.synchronizedSet(new HashSet<FiberImpl>());

    /**
     * The number of {@link Continuation}s that are {@link FiberState.RUNNING running} right now.
     */
    // when inc()==0, load state
    // when dec()==0, persist to disk
    transient Counter runningCounts = new Counter();


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
    private transient final Object removeLock = new Object();

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

        FiberImpl f = new FiberImpl(this,0,Continuation.startSuspendedWith(target));
        fibers.add(f);
        justCreated = true;

        save();
    }

    private void init(EngineImpl engine,File rootDir) {
        this.engine = engine;
        this.rootDir = rootDir;
        waitList = Collections.synchronizedSet(new HashSet<ConversationCondition>());
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

    private void save() throws IOException {
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

        if(fibers.isEmpty())
            return ConversationState.ENDED;

        synchronized(fibers) {
            for (FiberImpl f : fibers) {
                if(f.getState()== FiberState.RUNNABLE)
                    return ConversationState.RUNNABLE;
            }
        }
        return ConversationState.SUSPENDED;
    }

    public EngineImpl getEngine() {
        return engine;
    }

    synchronized void onFiberStartedRunning(FiberImpl fiber) {
        if(isRemoving)
            // this conversation is going to be removed now
            // no further
            throw new FiberDeath();

        if(runningCounts.inc()>0)
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
            Map<Integer,Continuation> list = (Map<Integer, Continuation>) ois.readObject();

            ois.close();
            cont.delete();

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

        // create the object that represents the persisted state
        Map<Integer,Continuation> state = new HashMap<Integer, Continuation>();
        for (FiberImpl f : fibers) {
            state.put(f.id,f.dehydrate());
        }

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

    synchronized void onFiberCompleted(FiberImpl fiber) {
        boolean modified = fibers.remove(fiber);
        assert modified;
        if(fibers.isEmpty())
            // conversation has finished execution.
            remove();
    }

    public void remove() {
        // this lock is to allow multiple concurrent invocations of the remove method
        synchronized(removeLock) {
            if(getState()== ConversationState.ENDED)
                return; // already removed.

            // the first thing we have to do is to wait for all the executing fibers
            // to complete. when we are doing that, we don't want new fibers to
            // start executing. We use isRemoving==true for this purpose.
            isRemoving = true;

            try {
                runningCounts.waitForZero();
            } catch (InterruptedException e) {
                // can't process it now. later.
                Thread.currentThread().interrupt();
            }

            Map<Integer,ConversationImpl> convs = engine.conversations;
            synchronized(convs) {
                convs.remove(id);
                if(convs.isEmpty()) {
                    synchronized(engine.completionLock) {
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
            notifyAll();

            // notify all conversations that are blocked on this
            synchronized(waitList) {
                for (ConversationCondition cd : waitList)
                    cd.activate(this);
                waitList.clear();
            }
        }
    }

    public synchronized void join() throws InterruptedException {
        FiberImpl fiber = FiberImpl.currentFiber();
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
            return SerializationContext.get().engine.getConversation(id);
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Returns a {@link ConversationImpl} instance that the current thread is executing.
     */
    public static ConversationImpl currentConversation() {
        return FiberImpl.currentFiber().owner;
    }

    private static final long serialVersionUID = 1L;
}
