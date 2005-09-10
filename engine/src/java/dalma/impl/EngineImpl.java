package dalma.impl;

import dalma.Conversation;
import dalma.Executor;
import dalma.spi.EngineSPI;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public final class EngineImpl implements EngineSPI, Serializable {

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
    transient final Map<Integer,ConversationImpl> conversations = new HashMap<Integer,ConversationImpl>();

    /**
     * Records the currently running conversations.
     */
    private static final ThreadLocal<ConversationImpl> currentConversations = new ThreadLocal<ConversationImpl>();

    public EngineImpl(File rootDir,ClassLoader classLoader,Executor executor) throws IOException {
        this.rootDir = rootDir;
        this.executor = executor;
        this.classLoader = classLoader;
        load();
    }

    /**
     * Loads the configuration from disk.
     */
    private void load() throws IOException {
        XmlFile df = getDataFile();
        if(df.exists()) {
            // load data into this object
            df.unmarshal(this);
        }

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
                // TODO: log this error somewhere
                e.printStackTrace();
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
        getDataFile().write(this);
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
    void queue(final ConversationImpl conv) {
        executor.execute(new Runnable() {
            public void run() {
                ConversationImpl old = currentConversations.get();
                currentConversations.set(conv);
                try {
                    conv.run();
                } catch( Throwable t ) {
                    // if the conversation stops unexpectedly, kill that conversation
                    // because we won't be able to resume it.
                    t.printStackTrace();    // TODO: how should we report this?
                    conv.remove();
                } finally {
                    if(old==null)
                        currentConversations.remove();
                    else
                        currentConversations.set(old);
                }
            }
        });
    }

    /**
     * Returns a {@link ConversationImpl} instance that the current thread is executing.
     */
    public static ConversationImpl getCurrentConversation() {
        ConversationImpl conv = currentConversations.get();
        if(conv==null)
            throw new IllegalStateException("this thread isn't executing a conversation");
        return conv;
    }

    public synchronized Collection<Conversation> getConversations() {
        synchronized(conversations) {
            return new ArrayList<Conversation>(conversations.values());
        }
    }

    ConversationImpl getConversation(int id) {
        synchronized(conversations) {
            return conversations.get(id);
        }
    }

    public ConversationImpl createConversation(Runnable target) throws IOException {
        ConversationImpl conv = new ConversationImpl(this,target);
        synchronized(conversations) {
            conversations.put(conv.id,conv);
        }
        queue(conv);
        return conv;
    }

//    public void save(OutputStream os) throws IOException {
//        if(!(os instanceof BufferedOutputStream))
//            os = new BufferedOutputStream(os);
//        // TODO: we need to make sure that none of the conversations are running
//        ObjectOutputStream oos = new ObjectOutputStream(os);
//        oos.writeObject(this);
//        oos.flush();
//    }

    private Object writeReplace() {
        if(SERIALIZATION_CONTEXT.get()!=null)
            // if the engine is written as a part of dehydration,
            // return a moniker to avoid the whole engine to be serialized.
            return MONIKER;
        else
            // otherwise we are serializing the whole engine.
            return this;
    }

    /**
     * While the hydration of the conversation is in progress,
     * this variable stores the {@link EngineImpl} that owns the conversation.
     *
     * <p>
     * This is used to resolve serialized instances to running instances.
     */
    static final ThreadLocal<EngineImpl> SERIALIZATION_CONTEXT = new ThreadLocal<EngineImpl>();

    private static final long serialVersionUID = 1L;

    private static final class EngineMoniker implements Serializable {
        private static final long serialVersionUID = 1L;
        private Object readResolve() {
            return SERIALIZATION_CONTEXT.get();
        }
    }
    private static final EngineMoniker MONIKER = new EngineMoniker();
}
