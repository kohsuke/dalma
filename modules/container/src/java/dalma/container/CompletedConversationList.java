package dalma.container;

import dalma.Conversation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * List of {@link CompletedConversation}s.
 *
 * <p>
 * This class handles persistence of {@link CompletedConversation}s.
 *
 * <p>
 * To keep the data in sync with the disk, we occasionally rescan
 * the disk.
 *
 * @author Kohsuke Kawaguchi
 */
final class CompletedConversationList extends Observable {
    /**
     * Directory to store conversations.
     */
    private final File dir;

    /**
     * We occasionally update the list from a file system.
     * The next scheduled update time.
     */
    private transient long nextUpdate = 0;

    /**
     * If the reloading of runs are in progress (in another thread,
     * set to true.)
     */
    private transient boolean reloadingInProgress;

    /**
     * Keyed by ID.
     */
    private Map<Integer,CompletedConversation> convs;

    /**
     * Determines when to discard a log record.
     * Transient because it's an application object that may not be
     * persistable.
     */
    private transient LogRotationPolicy policy = LogRotationPolicy.NEVER;

    public CompletedConversationList(File dir) {
        this.dir = dir;
        scheduleReload();
    }

    /**
     * Gets the data file for the given conversation.
     */
    private File getDataFile(Conversation conv) {
        return new File(dir, String.format("%06d.dat", conv.getId()));
    }

    /**
     * Sets the log rotation policy.
     */
    public void setPolicy(LogRotationPolicy policy) {
        this.policy = policy;
        scheduleReload();
    }

    public void add(Conversation _conv) {
        loadSync();
        CompletedConversation conv = new CompletedConversation(_conv);

        synchronized(convs) {
            convs.put(conv.getId(),conv);
            applyLogRotation(convs);
        }

        File dt = getDataFile(conv);
        try {
            conv.save(dt);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to save "+dt, e);
        }
    }

    public void remove(Conversation conv) {
        loadSync();
        if(convs.remove(conv.getId())==null)
            throw new IllegalArgumentException();
        // delete from disk, too
        getDataFile(conv).delete();
    }

    /**
     * Gets a snapshot view of all the {@link CompletedConversation}s.
     *
     * @return
     *      always non-null, possibly empty. Map is keyed by ID.
     */
    public Map<Integer,Conversation> getList() {
        loadSync();
        if(nextUpdate<System.currentTimeMillis() && !reloadingInProgress)
            scheduleReload();
        // return a new copy to avoid synchronization issue
        return new HashMap<Integer,Conversation>(convs);
    }

    private synchronized void scheduleReload() {
        if(!reloadingInProgress) {
            // avoid scheduling the task twice
            reloadingInProgress = true;
            // schedule a new reloading operation.
            // we don't want to block the current thread,
            // so reloading is done asynchronously.
            reloader.execute(reloadTask);
        }
    }

    private void loadSync() {
        if(convs==null)
            reloadTask.run();
    }


    private final Runnable reloadTask = new Runnable() {
        public void run() {
            Map<Integer,CompletedConversation> convs = new TreeMap<Integer,CompletedConversation>();
            convs = Collections.synchronizedMap(convs);

            File[] data = dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.getPath().endsWith(".dat");
                }
            });
            if(data!=null) {
                for (File dt : data) {
                    try {
                        CompletedConversation conv = CompletedConversation.load(dt);
                        convs.put(conv.getId(),conv);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Unable to load "+dt, e);
                        dt.delete();    // discard this entry to avoid repeating this error in the future
                    }
                }
            }

            applyLogRotation(convs);

            CompletedConversationList.this.convs = convs;
            reloadingInProgress = false;
            nextUpdate = System.currentTimeMillis()+5000;
        }
    };

    private void applyLogRotation(Map<Integer, CompletedConversation> convs) {
        // apply log policy and trim the entries
        for (Iterator<Map.Entry<Integer,CompletedConversation>> itr = convs.entrySet().iterator(); itr.hasNext();) {
            CompletedConversation c = itr.next().getValue();
            if(!policy.keep(c))
                itr.remove();
        }
    }

    private static final Executor reloader = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    private static final Logger logger = Logger.getLogger(CompletedConversationList.class.getName());
}
