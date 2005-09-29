package dalma.ports.email;

import dalma.Dock;
import dalma.TimeUnit;
import dalma.endpoints.timer.TimerEndPoint;
import dalma.impl.GeneratorImpl;
import dalma.spi.ConversationSPI;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.Date;
import java.util.NoSuchElementException;

/**
 * {@link Iterator} that waits for multiple replies to one e-mail.
 *
 * <p>
 * Remains in memory even if the continuation is suspended.
 *
 * TODO: this needs to be easier to write.
 *
 * @author Kohsuke Kawaguchi
 */
final class ReplyIteratorImpl extends GeneratorImpl implements ReplyIterator, MailReceiver {

    /**
     * EndPoint to which this iterator belongs.
     */
    private final EmailEndPoint endPoint;

    private final List<MimeMessage> replies = new LinkedList<MimeMessage>();

    /**
     * If the {@link #replies} become empty, the conversation
     * will wait until a new one arrives by using this lock.
     */
    private transient DockImpl lock;

    private final UUID uuid;

    /**
     * @see #setExpirationDate(Date)
     */
    private Date expirationDate;

    ReplyIteratorImpl(EmailEndPoint endPoint,MimeMessage outgoing) throws MessagingException {
        this.endPoint = endPoint;
        Sender s = new Sender(outgoing);
        this.uuid = s.uuid;
        s.send();
    }

    protected void onLoad() {
        EmailEndPoint.register(this);
    }

    protected void interrupt() {
        EmailEndPoint.unregister(this);
    }

    public UUID getUUID() {
        return uuid;
    }

    /**
     * Gets the value set by the {@link #setExpirationDate(Date)} method.
     *
     * @see #setExpirationDate(Date)
     */
    public synchronized Date getExpirationDate() {
        return expirationDate;
    }

    public synchronized void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setExpirationDate(long time, TimeUnit unit) {
        setExpirationDate(unit.fromNow(time));
    }

    public synchronized MimeMessage next() {
        if(replies.isEmpty())
            throw new NoSuchElementException();

        return replies.remove(0);
    }

    public synchronized boolean hasNext() {
        if(replies.isEmpty()) {
            // no replies in the queue, wait for one
            lock = new DockImpl(endPoint);
            ConversationSPI.getCurrentConversation().suspend(
                lock,TimerEndPoint.createDock(expirationDate));
        }
        return !replies.isEmpty();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public synchronized void handleMessage(MimeMessage msg) {
        replies.add(msg);
        if(lock!=null) {
            lock.resume(null);
            lock = null;
        }
    }

    private class DockImpl extends Dock<Void> {
        public DockImpl(EmailEndPoint endPoint) {
            super(endPoint);
        }

        public void park() {
            synchronized(ReplyIteratorImpl.this) {
                if(!replies.isEmpty()) {
                    resume(null);
                    return;
                }
                lock = this;
            }
        }

        public void interrupt() {
            // noop
        }

        public void onLoad() {
            park();
        }
    }
}
