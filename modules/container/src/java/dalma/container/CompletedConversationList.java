package dalma.container;

import dalma.Conversation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Observable;
import java.util.Iterator;
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

    private Vector<CompletedConversation> convs;

    /**
     * Determines when to discard a log record.
     * Transient because it's an application object that may not be
     * persistable.
     */
    private transient LogRotationPolicy policy = LogRotationPolicy.NEVER;

    public CompletedConversationList(File dir) {
        this.dir = dir;
        // load the first list now
        reloadTask.run();
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
    }

    public void add(Conversation _conv) {
        CompletedConversation conv = new CompletedConversation(_conv);

        synchronized(convs) {
            // apply log policy and trim the entries
            for (Iterator<CompletedConversation> itr = convs.iterator(); itr.hasNext();) {
                CompletedConversation c = itr.next();
                if(!policy.keep(c))
                    itr.remove();
            }
            convs.add(conv);
        }

        File dt = getDataFile(conv);
        try {
            conv.save(dt);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to save "+dt, e);
        }
    }

    public void remove(Conversation conv) {
        if(!convs.remove(conv))
            throw new IllegalArgumentException();
        // delete from disk, too
        getDataFile(conv).delete();
    }

    /**
     * Gets a snapshot view of all the {@link CompletedConversation}s.
     *
     * @return
     *      always non-null, possibly empty.
     */
    public List<Conversation> getList() {
        assert convs!=null;
        if(nextUpdate<System.currentTimeMillis() && !reloadingInProgress) {
            synchronized(this) {
                if(!reloadingInProgress) {
                    // avoid scheduling the task twice
                    reloadingInProgress = true;
                    // schedule a new reloading operation.
                    // we don't want to block the current thread,
                    // so reloading is done asynchronously.
                    reloader.execute(reloadTask);
                }
            }
        }
        // return a new copy to avoid synchronization issue
        return new ArrayList<Conversation>(convs);
    }


    private final Runnable reloadTask = new Runnable() {
        public void run() {
            Vector<CompletedConversation> convs = new Vector<CompletedConversation>();

            File[] data = dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.getPath().endsWith(".dat");
                }
            });
            if(data!=null) {
                for (File dt : data) {
                    try {
                        convs.add(CompletedConversation.load(dt));
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Unable to load "+dt, e);
                        dt.delete();    // discard this entry to avoid repeating this error in the future
                    }
                }
            }

            CompletedConversationList.this.convs = convs;
            reloadingInProgress = false;
            nextUpdate = System.currentTimeMillis()+5000;
        }
    };

    private static final Executor reloader = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    private static final Logger logger = Logger.getLogger(CompletedConversationList.class.getName());
}
