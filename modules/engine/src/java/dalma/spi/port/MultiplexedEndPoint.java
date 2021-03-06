package dalma.spi.port;

import dalma.Condition;
import dalma.ReplyIterator;
import dalma.endpoints.timer.TimerEndPoint;
import dalma.impl.EndPointImpl;
import dalma.spi.FiberSPI;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class MultiplexedEndPoint<Key,Msg> extends EndPointImpl {
    /**
     * Conversations waiting for replies.
     */
    protected final Map<Key,Receiver<Key,Msg>> queue = new Hashtable<Key,Receiver<Key,Msg>>();

    /**
     * Logger for event logging.
     */
    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected MultiplexedEndPoint(String name) {
        super(name);
    }

    /**
     * Sends out an message and waits for a single reply.
     *
     * <p>
     * This method blocks the conversation indefinitely until a reply is received.
     *
     * @return  always non-null.
     */
    protected Msg waitForReply(Msg msg) {
        return FiberSPI.currentFiber(true).suspend(new OneTimeCondition<Key,Msg>(this,msg));
    }

    /**
     * Sends out an message and waits for a single reply with timeout.
     *
     * TODO:javadoc
     */
    protected Msg waitForReply(Msg msg, Date timeout) {
        return FiberSPI.currentFiber(true).suspend(
            new OneTimeCondition<Key,Msg>(this,msg), TimerEndPoint.<Msg>createDock(timeout) );
    }

    /**
     * Sends out an message and waits for multiple replies.
     *
     * TODO:javadoc
     */
    protected ReplyIterator<? extends Msg> waitForMultipleReplies(Msg outgoing, Date expirationDate ) {
        return new ReplyIteratorImpl<Key,Msg>(this,outgoing,expirationDate);
    }



//
//
// implementation detail
//
//

    /*package*/ void register(Receiver<Key,Msg> mr) {
        queue.put(mr.getKey(),mr);
    }

    /*paclage*/ void unregister(Receiver<Key,Msg> mr) {
        queue.remove(mr.getKey());
    }

    /**
     * Obtains the key of the message.
     * Used to find the key of the incoming message.
     *
     * @param msg
     *      never be null.
     */
    protected abstract Key getKey(Msg msg);

    /**
     * Invoked upon receiving a new message that doesn't have any key.
     */
    protected abstract void onNewMessage(Msg msg);

    /**
     * Sends an out-going message, and returns a key that will identify replies.
     */
    protected abstract Key send(Msg msg);

    /**
     * Dispatches a newly received message to the right receiver.
     *
     * This method needs to be invoked when a new message is received.
     */
    protected void handleMessage(Msg msg) {
        Key key = getKey(msg);
        if(key==null) {
            onNewMessage(msg);
            return;
        }

        Receiver<Key, Msg> receiver = queue.get(key);
        if(receiver==null) {
            // TODO: or shall it be exception?
            logger.warning("No conversation is waiting for the message key="+key);
            return;
        }
        receiver.handleMessage(msg);
    }

    /**
     * Condition used for waiting a single reply.
     */
    private static final class OneTimeCondition<Key,Msg> extends Condition<Msg> implements Receiver<Key,Msg> {
        private Key key;

        /**
         * The out-going message to be sent.
         *
         * The field is transient because we'll send it before
         * the dock is serialized, and thereafter never be used.
         */
        private transient Msg outgoing;

        private final MultiplexedEndPoint<Key,Msg> endPoint;

        public OneTimeCondition(MultiplexedEndPoint<Key,Msg> endPoint, Msg outgoing) {
            this.outgoing = outgoing;
            this.endPoint = endPoint;
        }

        private MultiplexedEndPoint<Key,Msg> getEndPoint() {
            return endPoint;
        }

        public Key getKey() {
            return key;
        }

        public void handleMessage(Msg msg) {
            getEndPoint().unregister(this);
            activate(msg);
        }

        public void onParked() {
            MultiplexedEndPoint<Key,Msg> endPoint = getEndPoint();
            try {
                key = endPoint.send(outgoing);
                assert key!=null;
                endPoint.register(this);
            } finally {
                outgoing = null;
            }
        }

        public void interrupt() {
            if(key!=null)
                getEndPoint().unregister(this);
        }

        public void onLoad() {
            // key==null if the condition is created but left unused
            if(key!=null)
                getEndPoint().register(this);
        }
    }
}
