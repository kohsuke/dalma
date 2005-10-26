package dalma.impl;

/**
 * Thread-safe counter.
 *
 * @author Kohsuke Kawaguchi
 */
final class Counter {
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
}
