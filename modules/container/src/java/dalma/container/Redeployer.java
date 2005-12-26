package dalma.container;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Monitors a ".dar" file for its update and redeploy
 * if necessary.
 *
 * @author Kohsuke Kawaguchi
 */
final class Redeployer extends FileChangeMonitor {

    private static final Logger logger = Logger.getLogger(Redeployer.class.getName());

    private final Container container;

    static class PassiveFutureTask<V> extends FutureTask<V> {
        private NoopCallable<V> callable;

        public PassiveFutureTask() {
            this(new NoopCallable<V>());
        }

        private PassiveFutureTask(NoopCallable<V> c) {
            super(c);
            this.callable = c;
        }

        /**
         * Sets this {@link Future} as 'completed' with the given value.
         */
        public void completeWith(V v) {
            this.callable.v = v;
            run();
        }

        static class NoopCallable<V> implements Callable<V> {
            private V v;
            public V call() throws Exception {
                return v;
            }
        }
    }

    /**
     * {@link Future} objects that are listening for the completion of the redeployment.
     *
     * Access is synchronized.
     */
    private final Map<File,PassiveFutureTask<FailedOperationException>> futures
        = new Hashtable<File,PassiveFutureTask<FailedOperationException>>();

    Redeployer(Container container) {
        super(container.appsDir);
        this.container = container;
    }

    /**
     * Gets a {@link Future} object that receives the result of
     * install/uninstall/reinstall operations.
     *
     * @param dirName
     *      This has to be a directory (either existing or expected to be created)
     *      under the redeployer's supervision.
     *
     * @return
     *      always non-null. This future object receives a null value
     *      if the operation is successful, or otherwise an exception that
     *      indicates a failure.
     */
    public Future<FailedOperationException> getFuture(File dirName) {
        synchronized(futures) {
            PassiveFutureTask<FailedOperationException> ft = futures.get(dirName);
            if(ft==null) {
                ft = new PassiveFutureTask<FailedOperationException>();
                futures.put(dirName,ft);
            }
            return ft;
        }
    }

    @Override
    protected void onAdded(File file) {
        if(isDar(file))
            Container.explode(file);
        if(file.isDirectory()) {
            logger.info("New application '"+file.getName()+"' detected. Deploying.");
            FailedOperationException ex = null;
            try {
                container.deploy(file);
            } catch (FailedOperationException e) {
                ex = e;
                logger.log(Level.SEVERE, "Unable to deploy", e );
            }
            notifyFutures(file,ex);
        }
    }

    @Override
    protected void onUpdated(File file) {
        if(isDar(file))
            Container.explode(file);
        if(file.isDirectory()) {
            FailedOperationException ex = null;
            try {
                WorkflowApplication wa = container.getApplication(file.getName());
                if(wa!=null) {
                    logger.info("Changed detected in application '"+wa.getName()+"'. Re-deploying.");
                    wa.unload();
                    wa.start();
                }
            } catch (FailedOperationException e) {
                ex = e;
                logger.log(Level.SEVERE, "Unable to redeploy", e );
            }
            notifyFutures(file,ex);
        }
    }

    protected void onDeleted(File file) {
        WorkflowApplication wa = container.getApplication(file.getName());
        if(wa!=null) {
            logger.info("Application '"+file.getName()+"' is removed. Undeploying.");
            wa.remove();
            notifyFutures(file,null);
        }
    }

    /**
     * Updates {@link Future} objects blocking on a directory.
     */
    private void notifyFutures(File file, FailedOperationException e) {
        PassiveFutureTask<FailedOperationException> ft = futures.remove(file);
        if(ft!=null) {
            ft.completeWith(e);
        }
    }

    private static boolean isDar(File f) {
        return f.getName().endsWith(".dar");
    }

}
