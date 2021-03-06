package dalma.spi.port;

import dalma.Condition;
import dalma.ReplyIterator;
import dalma.endpoints.timer.TimerEndPoint;
import dalma.impl.GeneratorImpl;
import dalma.spi.ConversationSPI;
import dalma.spi.FiberSPI;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * {@link ReplyIterator} implementation for {@link MultiplexedEndPoint}.
 *
 * @author Kohsuke Kawaguchi
 */
final class ReplyIteratorImpl<Key,Msg> extends GeneratorImpl implements ReplyIterator<Msg>, Receiver<Key,Msg> {

    /**
     * EndPoint to which this iterator belongs.
     */
    private final MultiplexedEndPoint<Key,Msg> endPoint;

    private final List<Msg> replies = new LinkedList<Msg>();

    /**
     * If the {@link #replies} become empty, the conversation
     * will wait until a new one arrives by using this lock.
     */
    private transient ConditionImpl lock;

    private final Key key;

    /**
     * @see #getExpirationDate()
     */
    private final Date expirationDate;

    ReplyIteratorImpl(MultiplexedEndPoint<Key,Msg> endPoint, Msg outgoing, Date expirationDate) {
        this.endPoint = endPoint;
        this.expirationDate = expirationDate;
        synchronized(endPoint.queue) {
            this.key = endPoint.send(outgoing);
            ConversationSPI.currentConversation().addGenerator(this);
        }
    }

    protected void onLoad() {
        endPoint.register(this);
    }

    public void dispose() {
        endPoint.unregister(this);
    }

    public Key getKey() {
        return key;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public synchronized Msg next() {
        if(replies.isEmpty())
            throw new NoSuchElementException();

        return replies.remove(0);
    }

    public synchronized boolean hasNext() {
        if(replies.isEmpty()) {
            // no replies in the queue
            if(!isExpired()) {
                // block until we receive another one
                lock = new ConditionImpl();
                if(expirationDate==null)
                    FiberSPI.currentFiber(true).suspend(lock);
                else
                    FiberSPI.currentFiber(true).suspend(
                        lock, TimerEndPoint.createDock(expirationDate));
            }
        }
        return !replies.isEmpty();
    }

    private boolean isExpired() {
        return expirationDate!=null && expirationDate.before(new Date());
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public synchronized void handleMessage(Msg msg) {
        if(isExpired())
            return; // we are no longer collecting replies. discard.
        replies.add(msg);
        if(lock!=null) {
            lock.activate(null);
            lock = null;
        }
    }

    /**
     * Blocks until a next message is received.
     */
    private final class ConditionImpl extends Condition<Void> {
        public ConditionImpl() {
        }

        public void onParked() {
            synchronized(ReplyIteratorImpl.this) {
                if(!replies.isEmpty()) {
                    activate(null);
                    return;
                }
                lock = this;
            }
        }

        public void interrupt() {
            // noop
        }

        public void onLoad() {
            onParked();
        }
    }
}
