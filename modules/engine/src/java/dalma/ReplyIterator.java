package dalma;

import java.util.Date;
import java.util.Iterator;

/**
 * {@link Iterator} that enumerates the replies received to a message
 * that was sent out from an {@link EndPoint}.
 *
 * <p>
 * In a workflow, it's common for your program to send out a 'message'
 * (such as an e-mail, JMS message, etc), then wait for replies to it.
 * {@link ReplyIterator} defines a programming pattern that collects
 * all the replies in an easy way.
 *
 * <p>
 * This message exchange pattern (MEP) is commonly seen across
 * many different messaging mechanisms, so {@link ReplyIterator} is parameterized
 * with the type {@code T} that represents the type of a reply.
 *
 * <p>
 * Replies are received over the time, so at some point your application
 * has to decide that you are not going to wait for any more replies.
 * For this purpose, a {@link ReplyIterator} has an <b>expiration date</b>.
 * Replies received beyond this expiration date will not be returned from
 * a {@link ReplyIterator} (what happens to such messages depend on the endpoint
 * implementation.) Alternatively, you can call {@link #dispose()} method
 * any time to discard the iterator. This allows the implementation to possibly
 * clean up the relevant resources.
 *
 * <p>
 * Every time you call the iterator's {@link Iterator#hasNext() hasNext} method,
 * it checks if a reply is received. If not, the conversation suspends
 * and then resumes only when either (1) a reply is received or (2) the expiration date
 * of this iterator is reached.
 *
 * <p>
 * If a reply is received, {@link #hasNext() hasNext} method returns true,
 * and the reply can be fetched by calling {@link #next() next} method,
 * just like a normal {@link Iterator}.
 *
 * <p>
 * If the expiration date is reached, the {@link #hasNext() hasNext} method
 * returns false, indicating that it will not wait for another message.
 *
 * <p>
 * Together, this allows the calling conversation to efficiently handle
 * all the reply messages received during a particular time period, then
 * move on to do something else.
 *
 * @author Kohsuke Kawaguchi
 */
public interface ReplyIterator<T> extends Iterator<T> {
    /**
     * Gets the expiration date of this iterator.
     *
     * Once this date is reached, the iterator no longer waits for new messages,
     * and simply return <tt>false</tt> from {@link #hasNext()} method (unless
     * there are messages that are received before the expiration date, which are
     * not read yet.)
     */
    Date getExpirationDate();

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    void remove() throws UnsupportedOperationException;

    /**
     * Possibly clean up resources allocated for this object.
     *
     * <p>
     * When an application decides that it no longer needs to check
     * any message from this {@link ReplyIterator}, it may call this
     * method to let the implementation release resources earlier.
     *
     * <p>
     * This method is optional for both callers and callees;
     * a caller is not required to invoke this method, and the callee
     * is not required to take any action.
     *
     * <p>
     * This method can be invoked multiple times. 
     */
    void dispose();

    ///**
    // * Works like {@link #hasNext(Date)} but with a timeout.
    // *
    // * @param timeout
    // *      If no reply is received by this date,
    // *      this method returns {@code false}.
    // */
    //boolean hasNext(Date timeout);

    ///**
    // * Works like {@link #hasNext(Date)} but with a timeout.
    // */
    //boolean hasNext(long timeout, TimeUnit unit);
}
