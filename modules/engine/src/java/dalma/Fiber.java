package dalma;

import dalma.impl.FiberImpl;

import java.util.Date;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Fiber<T extends Runnable> {
    /**
     * Gets the {@link Runnable} object that represents the entry point of this fiber.
     *
     * <p>
     * The state of {@link Fiber} is not always kept in memory. For example,
     * if a conversation is blocking on an event, its state is purged to the disk.
     * Because of this, this method can be only invoked from other {@link Fiber}s
     * in the same {@link Conversation}.
     *
     * @return
     *      never null. Always return the same {@link Runnable} object
     *      given to create a new {@link Fiber}.
     *
     * @throws IllegalStateException
     *      if this method is not invoked from a {@link Fiber} that belongs
     *      to the same {@link Conversation}.
     */
    public abstract T getRunnable();

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
    public static <T extends Runnable> Fiber<T> create(T entryPoint) {
        return FiberImpl.create(entryPoint);
    }

    /**
     * Returns the {@link Fiber} that the current thread is executing.
     *
     * <p>
     * This mehtod can be only called from within the workflow.
     *
     * @throws IllegalStateException
     *      if the calling thread isn't a workflow thread.
     */
    public static Fiber<?> currentFiber() {
        return FiberImpl.currentFiber(true);
    }

    /**
     * Completes the execution of the current fiber,
     * as if the {@link Runnable#run()} method returned
     * normally.
     *
     * <p>
     * This method works like how {@link System#exit(int)} works
     * for the VM
     */
    public static void exit() {
        FiberImpl.currentFiber(true).doExit();
    }

    /**
     * Jumps to the point where the fiber suspended for the last time,
     * and re-executes the execution when the specified time is reached.
     *
     * <p>
     * This is a tricky method, so it probably needs some explanation.
     * In a workflow, often because of an error, one wants to retry
     * the execution. You can do this by using a loop statement, but
     * the {@link #again(Date)} method provides an interesting way of
     * doing this.
     *
     * <p>
     * The semantics of the {@link #again(Date)} method is really just
     * "please retry what you just did later." First, the execution
     * magically jumps to the point where the {@link Fiber} suspended
     * the last time (which is typically one of the blocking endpoint invocation.),
     * and then sleeps until the given date is hit.
     *
     * Then the execution resumes, just like it did for the last time,
     * with the same object. Effectively causing the workflow to retry
     * the work.
     *
     * <pre>
     * // example
     * msg = emailEndPoint.waitForReply(msg);
     * try {
     *   ... = new URL("http://www.sun.com/").openStream();
     * } catch(IOException e) {
     *   // OK, the website is down.
     *   // let's try this later, again.
     *   again(1,HOURS);
     * }
     * </pre>
     */
    public static void again(long delay, TimeUnit unit) {
        FiberImpl.currentFiber(true).doAgain(delay,unit);
    }
}
