package dalma.container;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Periodically checks for an update to a file in a specific directory and invokes a callback.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class FileChangeMonitor {
    private static final Logger logger = Logger.getLogger(FileChangeMonitor.class.getName());

    private static final Set<WeakReference<FileChangeMonitor>> monitors = new HashSet<WeakReference<FileChangeMonitor>>();

    /**
     * The singleton thread that does all the monitoring.
     */
    private static final Thread monitorThread;

    public static void add(FileChangeMonitor job) {
        synchronized(monitors) {
            monitors.add(new WeakReference<FileChangeMonitor>(job));
            monitors.notify();
        }
    }

    /**
     * Directory to monitor.
     */
    private final File dir;

    /**
     * Files in the {@link #dir} known currently, keyed by their names.
     */
    private Map<String,Entry> files;

    private class Entry {
        /**
         * The {@link File} instance that this object represents.
         */
        private final File file;

        /**
         * The last-modified timestamp known to us.
         */
        private long timestamp;

        public Entry(File file) {
            this.file = file;
            timestamp = file.lastModified();
        }
    }

    public FileChangeMonitor(File dir) {
        this.dir = dir;

        if(!dir.exists())
            throw new IllegalArgumentException("No such directory "+dir);

        // fill in the initial values
        files = new HashMap<String, Entry>();
        for( File f : dir.listFiles() ) {
            files.put( f.getName(), new Entry(f));
        }

        synchronized(monitors) {
            monitors.add(new WeakReference<FileChangeMonitor>(this));
        }
    }

    /**
     * Cancels the monitor.
     *
     * The monitor is also implicitly cancelled when a {@link FileChangeMonitor} is
     * garbage-collected.
     */
    public void cancel() {
        synchronized(monitors) {
            Iterator<WeakReference<FileChangeMonitor>> itr = monitors.iterator();
            while(itr.hasNext()) {
                WeakReference<FileChangeMonitor> wr = itr.next();
                if(wr.get()==this) {
                    itr.remove();
                    return;
                }
            }
        }
        throw new IllegalStateException("already cancelled");
    }

    /**
     * Invoked when a file/directory is changed.
     */
    protected abstract void onUpdated(File file);

    /**
     * Invoked when a new file/directory is added.
     */
    protected abstract void onAdded(File file);

    /**
     * Invoked when a file/directory is removed.
     */
    protected abstract void onDeleted(File file);

    /**
     * Checks if there's an update, and invokes callbacks if there is.
     *
     * @return false
     *      if the monitoring should terminate.
     */
    private boolean check() {
        Map<String,Entry> newMap = new HashMap<String, Entry>();

        if(!dir.isDirectory())
            return false;   // directory itself no longer exists

        for( File f : dir.listFiles() ) {
            String name = f.getName();
            Entry entry = files.get(name);
            if(entry==null) {
                // new file
                newMap.put(name,new Entry(f));
                onAdded(f);
            } else {
                long t = f.lastModified();
                if(entry.timestamp<t) {
                    onUpdated(f);
                    entry.timestamp=t;
                }
                newMap.put(name,entry);
                files.remove(name);
            }
        }

        // anything left in 'files' are deleted files
        for (Entry e : files.values()) {
            onDeleted(e.file);
        }
        files = newMap;

        return true;
    }



    static {
        monitorThread = new Thread() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(3000); // every once in 3 seconds

                        synchronized(monitors) {
                            Iterator<WeakReference<FileChangeMonitor>> itr = monitors.iterator();
                            while(itr.hasNext()) {
                                WeakReference<FileChangeMonitor> wr = itr.next();
                                FileChangeMonitor job = wr.get();

                                if(job ==null) {
                                    itr.remove();
                                    continue;
                                }

                                try {
                                    if(!job.check())
                                        itr.remove();
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    itr.remove();
                                }
                            }

                            while(monitors.isEmpty()) {
                                monitors.wait();
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        // don't let the thread die
                    }
                }
            }
        };
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
}
