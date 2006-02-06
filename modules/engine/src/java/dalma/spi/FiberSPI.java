package dalma.spi;

import dalma.Condition;
import dalma.Fiber;
import dalma.TimeUnit;
import dalma.impl.FiberImpl;
import dalma.impl.OrCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class FiberSPI<T extends Runnable> extends Fiber<T> {
    /**
     * Suspends the fiber.
     *
     * @param condition
     *      The condition to which this conversation is parked.
     *      Must not be null. This condition is resonpsible for
     *      resuming this conversation.
     * @return
     *      This method returns when the conversation is resumed.
     *      the parameter given to {@link Condition#activate(Object)} will be
     *      returned.
     */
    public abstract <T> T suspend(Condition<T> condition);

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
    public abstract void again(long delay, TimeUnit unit);

    public final <T> T suspend(List<? extends Condition<? extends T>> conditions) {
        return suspend(new OrCondition<T>(new ArrayList<Condition<? extends T>>(conditions))).getReturnValue();
    }

    public final <T> T suspend(Condition<? extends T>... conditions) {
        if(conditions ==null)
            throw new IllegalArgumentException("condition list is null");
        return suspend(new OrCondition<T>(conditions)).getReturnValue();
    }

    public abstract ConversationSPI getOwner();

    /**
     * Returns the {@link FiberSPI} that the current thread is executing.
     */
    public static FiberSPI<?> currentFiber(boolean mustReturnNonNull) {
        return FiberImpl.currentFiber(mustReturnNonNull);
    }
}
