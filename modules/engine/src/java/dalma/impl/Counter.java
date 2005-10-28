package dalma.impl;

import java.io.Serializable;

/**
 * Thread-safe counter.
 *
 * @author Kohsuke Kawaguchi
 */
final class Counter implements Serializable {
    private int value;

    public synchronized int inc() {
        return value++;
    }

    public synchronized int dec() {
        return --value;
    }

    public synchronized int get() {
        return value;
    }

    public synchronized void waitForZero() throws InterruptedException {
        while(value!=0)
            wait();
    }

    private static final long serialVersionUID = 1L;
}
