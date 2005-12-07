package dalma.endpoints.invm;

import dalma.Condition;
import dalma.spi.FiberSPI;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

/**
 * Works like a socket in the in-VM communicaiton.
 *
 * @author Kohsuke Kawaguchi
 */
public class Channel extends Observable implements Serializable {
    /**
     * {@link Message}s that were delivered but not read by the application.
     */
    private final List<Message> queue = new Vector<Message>();

    /**
     * {@link Channel}s are uniquely identified names (so that serialization
     * will bind back to the same instance.)
     */
    private static final Map<String,Channel> channels = new Hashtable<String,Channel>();

    private final String name;
    private static int iota;

    public Channel() {
        synchronized(Channel.class) {
            name = Integer.toString(iota++);
        }
        channels.put(name,this);
    }

    /**
     * Sends a {@link Message} from this channel to the specified channel.
     */
    public void send(Message msg, Channel to) {
        msg.from = this;
        msg.to = to;

        if(msg==null)
            throw new IllegalArgumentException("message is null");

        synchronized(to) {
            to.queue.add(msg);
            to.setChanged();
            to.notifyObservers();
            to.notify();
        }
    }

    /**
     * Waits until there's a new {@link Message}.
     */
    public synchronized <T> Message<T> receive() {
        while(queue.isEmpty()) {
            FiberSPI<?> fiber = FiberSPI.currentFiber(false);
            if(fiber!=null)
                fiber.suspend(new ConditionImpl());
            else
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // process it later
                }
        }

        return queue.remove(0);
    }

    private final class ConditionImpl extends Condition<Void> implements Observer {
        public ConditionImpl() {
        }

        public void onParked() {
            synchronized(Channel.this) {
                if(!queue.isEmpty())
                    activate(null);
                else
                    addObserver(this);
            }
        }

        public void onLoad() {
            onParked();
        }

        public void interrupt() {
            deleteObserver(this);
        }

        public void update(Observable o, Object arg) {
            synchronized(Channel.this) {
                if(!queue.isEmpty()) {
                    deleteObserver(this);
                    activate(null);
                }
            }
        }
    }

    protected Object writeReplace() {
        return new Moniker(name);
    }

    private static final class Moniker implements Serializable {
        private final String name;

        public Moniker(String name) {
            this.name = name;
        }

        private Object readResolve() {
            return channels.get(name);
        }

        private static final long serialVersionUID = 1L;
    }
}
