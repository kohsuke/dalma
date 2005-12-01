package dalma.container;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import java.io.File;

/**
 * Periodically checks for an update to a file and invokes a callback.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class FileChangeMonitor {
    private static final Logger logger = Logger.getLogger(FileChangeMonitor.class.getName());

    private static final Set<WeakReference<FileChangeMonitor>> monitors = new HashSet<WeakReference<FileChangeMonitor>>();

    private static final Thread monitorThread;

    public static void add(FileChangeMonitor job) {
        synchronized(monitors) {
            monitors.add(new WeakReference<FileChangeMonitor>(job));
            monitors.notify();
        }
    }

    /**
     * File to monitor the timestamp.
     */
    private final File file;

    /**
     * The timestamp of the file checked last time.
     */
    private long timestamp;

    public FileChangeMonitor(File file, long timestamp) {
        this.file = file;
        this.timestamp = timestamp;

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
     * Invoked when a file is changed.
     */
    protected abstract void onUpdated();

    /**
     * Checks if there's an update, and invokes {@link #onUpdated()}
     * if there is.
     */
    private void check() {
        long lm = file.lastModified();
        if(timestamp<lm) {
            timestamp = lm;
            onUpdated();
        }
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

                                job.check();
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
