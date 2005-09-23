package dalma.helpers;

import dalma.Executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link Executor} implemented as a thread pool.
 *
 * @author Kohsuke Kawaguchi
 */
public class ThreadPoolExecutor implements Executor {

    /**
     * Queue of conversations that can be run.
     * Needs to be synchronized before use.
     */
    private final List<Runnable> jobQueue = new LinkedList<Runnable>();

    private final Collection<WorkerThread> threads = new ArrayList<WorkerThread>();

    public ThreadPoolExecutor(int nThreads) {
        for( int i=0; i<nThreads; i++ ) {
            WorkerThread thread = new WorkerThread();
            thread.start();
            threads.add(thread);
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        for( WorkerThread t : threads )
            t.interrupt();
    }

    public void execute(Runnable command) {
        synchronized(jobQueue) {
            jobQueue.add(command);
            jobQueue.notify();
        }
    }

    private final class WorkerThread extends Thread {
        public WorkerThread() {
            super("Dalma engine worker thread");
        }

        public void run() {
            try {
                while(true) {
                    Runnable job;
                    synchronized(jobQueue) {
                        while(jobQueue.isEmpty())
                            jobQueue.wait();
                        job = jobQueue.remove(0);
                    }

                    job.run();
                }
            } catch (InterruptedException e) {
                // treat this as a signal to die
            }
        }
    }
}
