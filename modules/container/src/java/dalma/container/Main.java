package dalma.container;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) {
        // we'll create an Engine instance for each application
        // but share the executor for better scheduling

        Executor exec = Executors.newFixedThreadPool(5,new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
    }
}
