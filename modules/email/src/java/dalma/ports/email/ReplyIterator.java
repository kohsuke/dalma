package dalma.ports.email;

import dalma.Dock;
import dalma.impl.GeneratorImpl;
import dalma.spi.ConversationSPI;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 *
 * <p>
 * Remains in memory even if the continuation is suspended.
 *
 * TODO: this needs to be easier to write.
 *
 * @author Kohsuke Kawaguchi
 */
final class ReplyIterator extends GeneratorImpl implements Iterator<MimeMessage>, MailReceiver {

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

    ReplyIterator(EmailEndPoint endPoint,MimeMessage outgoing) throws MessagingException {
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

    public synchronized MimeMessage next() {
        while(replies.isEmpty()) {
            lock = new DockImpl(endPoint);
            ConversationSPI.getCurrentConversation().suspend(lock);
        }

        return replies.remove(0);
    }

    public boolean hasNext() {
        return true;
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
            synchronized(ReplyIterator.this) {
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
