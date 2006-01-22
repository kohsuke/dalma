package dalma.impl;

import dalma.Executor;
import dalma.helpers.ThreadPoolExecutor;

/**
 * Base class that provides regular reloading logic.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Reloadable {
    private static final Executor reloader = new ThreadPoolExecutor(1,true);

    private long nextReloadTime;
    private boolean updateInProgress;

    /**
     * True if the data is loaded at least once.
     */
    private boolean initialDataFetched = false;

    private final Runnable task = new Runnable() {
        public void run() {
            try {
                initialDataFetched = true;
                reload();
            } finally {
                setNextReloadTime();
            }
        }
    };

    protected Reloadable() {
        setNextReloadTime();
    }

    private void setNextReloadTime() {
        this.nextReloadTime = System.currentTimeMillis()+5000;
    }

    public void update() {
        if(!initialDataFetched) {
            // if no data has loaded before, fetch it now synchronously
            synchronized(this) {
                if(!initialDataFetched) {
                    initialDataFetched = true;
                    task.run();
                    return;
                }
            }
        }
        if(System.currentTimeMillis() > nextReloadTime && !updateInProgress) {
            updateInProgress = true;
            reloader.execute(task);
        }
    }

    public abstract void reload();
}
