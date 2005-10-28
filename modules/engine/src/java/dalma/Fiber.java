package dalma;

import dalma.spi.ConversationSPI;
import dalma.impl.FiberImpl;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Fiber {
    ///**
    // * Gets the {@link Runnable} object that represents the entry point of this fiber.
    // *
    // * @return
    // *      never null. Always return the same {@link Runnable} object
    // *      given to create a new {@link Fiber}.
    // */
    //T getRunnable();

    /**
     * Starts executing this {@link Fiber}.
     *
     * This method works just like {@link Thread#start()}.
     *
     * @throws IllegalStateException
     *      if this fiber is already started.
     */
    public abstract void start();

    /**
     * Waits until the execution of the fiber completes.
     * This method works like {@link Thread#join()}
     *
     * <p>
     * If called from another fiber, the calling fiber
     * suspends and blocks until this fiber exits (and
     * the thread will be reused to execute other fibers.)
     *
     * <p>
     * If called from outside conversations, the calling
     * method simply {@link #wait() waits}.
     *
     * @throws IllegalStateException
     *      If a fiber tries to join itself.
     * @throws InterruptedException
     *      If this thread is interrupted while waiting.
     */
    public abstract void join() throws InterruptedException;

    /**
     * Gets the current state of the fiber.
     *
     * @return never null
     */
    public abstract FiberState getState();

    /**
     * Gets the {@link Conversation} that this fiber belongs to.
     *
     * A {@link Fiber} always belongs to the same {@link Conversation}.
     *
     * @return never null
     */
    public abstract Conversation getOwner();

    /**
     * Creates a new {@link Fiber} within the current conversation.
     */
    public static Fiber create(Runnable entryPoint) {
        return FiberImpl.create(entryPoint);
    }
}
