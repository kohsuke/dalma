package dalma.helpers;

import dalma.Executor;
import dalma.Engine;

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

    /**
     * This object signals when all the threads terminate.
     */
    private final Object terminationSignal = new Object();

    /**
     * True to make worker threads daemon thread.
     */
    private final boolean daemon;

    /**
     * Creates a new thread pool executor.
     *
     * @param nThreads
     *      number of worker threads to create.
     * @param daemon
     *      true to make worker threads daemon threads.
     *      daemon threads allows the VM to shut down as soon as
     *      the application thread exits. Otherwise, you have to
     *      call {@link Engine#stop()} before your main thread exits,
     *      or else the JVM will run forever.
     */
    public ThreadPoolExecutor(int nThreads, boolean daemon) {
        this.daemon = daemon;
        synchronized(threads) {
            for( int i=0; i<nThreads; i++ ) {
                WorkerThread thread = new WorkerThread();
                thread.start();
                threads.add(thread);
            }
        }
    }

    public ThreadPoolExecutor(int nThreads) {
        this(nThreads,false);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        synchronized(threads) {
            // let them die, but don't wait
            for( WorkerThread t : threads )
                t.interrupt();
        }
    }

    public void execute(Runnable command) {
        synchronized(jobQueue) {
            jobQueue.add(command);
            jobQueue.notify();
        }
    }

    public void stop(long timeout) throws InterruptedException {
        synchronized(terminationSignal) {
            for( Thread t : threads )
                t.interrupt();
            if(timeout==-1)
                terminationSignal.wait();
            else
                terminationSignal.wait(timeout);
        }
    }

    private final class WorkerThread extends Thread {
        public WorkerThread() {
            super("Dalma engine worker thread");
            setDaemon(daemon);
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
                synchronized(threads) {
                    threads.remove(this);
                    if(threads.isEmpty()) {
                        synchronized(terminationSignal) {
                            terminationSignal.notifyAll();
                        }
                    }
                }
            }
        }
    }
}
