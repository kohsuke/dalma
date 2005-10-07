package dalma.impl;

import dalma.EndPoint;
import dalma.Engine;
import dalma.Conversation;

import java.io.Serializable;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class EndPointImpl extends EndPoint {
    protected EndPointImpl(String name) {
        super(name);
    }

    /**
     * Called when {@link Engine#stop()} is invoked.
     *
     * <p>
     * {@link EndPoint} should release resources that are used to listen to incoming events.
     * Note that it's possible for new {@link Conversation}s to part to the {@link EndPoint}
     * even after this method is called, due to the synchronization issue.
     *
     * <p>
     * Once this method is called, the end point must not awake conversations.  
     */
    protected abstract void stop();

    protected Object writeReplace() {
        return new Moniker(getName());
    }

    private static final class Moniker implements Serializable {
        private final String name;

        public Moniker(String name) {
            this.name = name;
        }

        private Object readResolve() {
            EndPoint endPoint = SerializationContext.get().engine.getEndPoint(name);
            if(endPoint==null)
                throw new Error("no endpoint of the name "+name+" exists in the engine");
            return endPoint;
        }

        private static final long serialVersionUID = 1L;
    }

}
