package dalma.container;

import dalma.Conversation;
import dalma.impl.Reloadable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.TreeMap;
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
        reloader.update();
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
        reloader.update();
        // return a new copy to avoid synchronization issue
        return new HashMap<Integer,Conversation>(convs);
    }

    /**
     * Load data if it hasn't been loaded.
     */
    private void loadSync() {
        if(convs==null)
            reloader.reload();
    }


    private final Reloadable reloader = new Reloadable() {
        public void reload() {
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

    private static final Logger logger = Logger.getLogger(CompletedConversationList.class.getName());
}
