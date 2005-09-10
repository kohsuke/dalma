package dalma.impl;

import dalma.Conversation;
import dalma.ConversationDeath;
import dalma.ConversationState;
import dalma.spi.ConversationSPI;
import dalma.spi.port.Dock;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.bytecode.StackRecorder;

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
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

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
     * Snapshot of the execution state.
     *
     * <p>
     * This field is null if:
     * <ol>
     *  <li>the state is {@link ConversationState#ENDED} or
     *  <li>the state is {@link ConversationState#SUSPENDED}, in which case the continuation is
     *      serialized.
     */
    private transient Continuation continuation;

    /**
     * The directory to save the state of this conversation.
     */
    private transient /*final*/ File rootDir;

    /**
     * Current state of the conversation.
     *
     * Transient because we only persist {@link ConversationState#SUSPENDED} conversations.
     */
    private transient ConversationState state = ConversationState.RUNNABLE;

    /**
     * If in the {@link ConversationState#SUSPENDED} state, this field
     * points to the dock to which this conversation is docked.
     *
     * Can be empty but never null.
     */
    private List<? extends Dock<?>> docks = new ArrayList<Dock<?>>();

    /**
     * Other conversations that are blocking for the completion of this conversation.
     *
     * Transient, because {@link ConversationDock}s in this queue re-register themselves.
     * Always non-null.
     */
    transient Set<ConversationDock> waitList;

    /**
     * Every conversation gets unique ID (per engine).
     * This is used so that a serialized {@link Conversation}
     * (as a part of the stack frame) can connect back to the running {@link Conversation} instance.
     */
    final int id;

    /**
     * Creates a new conversation that starts with the given target.
     */
    ConversationImpl(EngineImpl engine,Runnable target) throws IOException {
        id = engine.generateUniqueId();
        init(engine,new File(engine.getConversationsDir(),String.valueOf(id)));
        if(!rootDir.mkdirs())
            throw new IOException("Unable to create "+rootDir);
        continuation = Continuation.startSuspendedWith(target);
        save();
    }

    private void init(EngineImpl engine,File rootDir) {
        this.engine = engine;
        this.rootDir = rootDir;
        waitList = Collections.synchronizedSet(new HashSet<ConversationDock>());
    }

    /**
     * Loads a {@link ConversationImpl} object from the disk.
     */
    public static ConversationImpl load(EngineImpl engine, File dir) throws IOException {
        ConversationImpl conv = (ConversationImpl) new XmlFile(new File(dir,"conversation.xml")).read(engine.classLoader);
        conv.init(engine,dir);
        conv.state = ConversationState.SUSPENDED;
        for( Dock d : conv.docks )
            d.onLoad();
        return conv;
    }

    private void save() throws IOException {
        new XmlFile(new File(rootDir,"conversation.xml")).write(this);
    }

    /**
     * Gets the current state of the conversation.
     *
     * @return always non-null.
     */
    public ConversationState getState() {
        return state;
    }

    // this method needs to be continuation aware.
    public <T> T suspend(Dock<T> dock) {
        return suspend(Collections.singletonList(dock));
    }

    public <T> T suspend(List<? extends Dock<? extends T>> dock) {
        if(!StackRecorder.get().isRestoring()) {
            if(dock==null)
                throw new IllegalArgumentException("dock cannot be null");
            if(dock.isEmpty())
                throw new IllegalArgumentException("dock cannot be null");
            for( Dock d : dock ) {
                if(d.conv!=null)
                    throw new IllegalStateException("dock is already in use");
                d.conv = this;
            }
            assert this.docks.isEmpty();
            this.docks = new ArrayList<Dock<?>>(dock);
        }

        Continuation.suspend();
        if(StackRecorder.get().isCapturing()) {
            StackRecorder.get().pushReference(this);
            return null;
        }

        Object r = null;

        for(Dock d : docks) {
            synchronized(d) {
                // these two lines need to be done atomically
                if(d.getReturnValue()==null)
                    d.interrupt();
                else
                    // if more than one docks are activated around the same time, we don't care
                    // which one we report
                    r = d.getReturnValue();

                assert d.conv==this;
                d.conv = null;
            }
        }
        this.docks.clear();

        return (T)r;
    }

    /**
     * Called from the executor thread to run this conversation until
     * it suspends.
     */
    public void run() {
        if(state==ConversationState.ENDED) {
            return; // no-op
        }
        if(state!=ConversationState.RUNNABLE) {
            throw new IllegalStateException();
        }

        // restore the state
        if(continuation==null) {
            File cont = new File(rootDir,"continuation");
            try {
                EngineImpl.SERIALIZATION_CONTEXT.set(engine);

                ObjectInputStream ois = new ObjectInputStreamEx(
                    new BufferedInputStream(new FileInputStream(cont)),engine.classLoader);
                continuation = (Continuation)ois.readObject();
                ois.close();
                cont.delete();
            } catch (IOException e) {
                throw new ConversationDeath("failed to restore the state of the conversation "+cont,e);
            } catch (ClassNotFoundException e) {
                throw new ConversationDeath("failed to restore the state of the conversation "+cont,e);
            } finally {
                EngineImpl.SERIALIZATION_CONTEXT.remove();
            }
        }

        state = ConversationState.RUNNING;

        // this runs the conversation until it blocks
        continuation = Continuation.continueWith(continuation);
        state = ConversationState.SUSPENDED;

        if(continuation==null) {
            // conversation has finished execution.
            remove();
        } else {
            // conversation has suspended

            // let the dock know that we are parked
            for( Dock d : docks )
                d.park();

            // persist the state
            File cont = new File(rootDir,"continuation");
            try {
                EngineImpl.SERIALIZATION_CONTEXT.set(engine);

                ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(cont)));
                oos.writeObject(continuation);
                continuation = null;
                oos.close();
            } catch (IOException e) {
                throw new ConversationDeath("failed to persist the state of the conversation "+cont, e);
            } finally {
                EngineImpl.SERIALIZATION_CONTEXT.remove();
            }

            try { // this needs to be done outside the EngineImpl.SERIALIZATION_CONTEXT
                save();
            } catch (IOException e) {
                throw new ConversationDeath("failed to persist the state of the conversation "+cont, e);
            }
        }
    }

    /**
     * Resumes the suspended conversation.
     *
     * @param activated
     *      {@link Dock} object that was activated. Used to determine
     *      what caused the conversation to wake up.
     */
    public synchronized <T> void resume(Dock<T> activated) {
        if(state==ConversationState.SUSPENDED) {
            state = ConversationState.RUNNABLE;
            engine.queue(this);
        } else
        if(state==ConversationState.RUNNABLE) {
            // this happens if two docks try to awake a conversation around the same time
            return;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Removes this conversation.
     */
    public synchronized void remove() {
        if(!state.isRemovable)
            throw new IllegalStateException("A conversation in "+state+" state can't be removed");

        Map<Integer,ConversationImpl> convs = engine.conversations;
        synchronized(convs) {
            convs.remove(id);
        }

        if(state==ConversationState.SUSPENDED) {
            try {
                Util.deleteRecursive(rootDir);
            } catch (IOException e) {
                // there's really nothing we nor appliation can do to recover from this.
                // TODO: log
            }
            // remove this conversation from the port
            for(Dock d : docks) {
                assert d.conv==this;
                d.interrupt();
                d.conv = null;
            }
            docks.clear();
        }

        state = ConversationState.ENDED;

        // notify any threads that are blocked on this conversation.
        notifyAll();
        // notify all conversations that are blocked on this
        synchronized(waitList) {
            for (ConversationDock cd : waitList)
                cd.resume(this);
        }
    }

    public synchronized void join() throws InterruptedException {
        ConversationImpl cur = EngineImpl.currentConversations.get();
        if(cur==null) {
            // called from outside conversations
            if(state!=ConversationState.ENDED) {
                // TODO: allow this method to be invoked from another conversation,
                // and in that case suspend instead of block
                wait();
            }
        } else {
            cur.suspend(new ConversationDock(this));
        }
    }

    private Object writeReplace() {
        if(EngineImpl.SERIALIZATION_CONTEXT.get()==null)
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
            return EngineImpl.SERIALIZATION_CONTEXT.get().getConversation(id);
        }

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
