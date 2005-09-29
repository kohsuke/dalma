package dalma.ports.email;

import dalma.TimeUnit;

import javax.mail.internet.MimeMessage;
import java.util.Iterator;
import java.util.Date;

/**
 * {@link Iterator} that returns the replies to a message
 * that was sent out.
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
public interface ReplyIterator extends Iterator<MimeMessage> {
    /**
     * Sets the expiration date.
     *
     * @param dt
     *      null to indicate no expiration at all.
     */
    void setExpirationDate(Date dt);

    /**
     * Sets the expiration date in terms of the timespan from right now.
     */
    void setExpirationDate(long time, TimeUnit unit);
    Date getExpirationDate();
}
