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

    private Object writeReplace() {
        if(EngineImpl.SERIALIZATION_CONTEXT.get()==null)
            return this;
        else
            return new Moniker(getName());
    }

    private static final class Moniker implements Serializable {
        private final String name;

        public Moniker(String name) {
            this.name = name;
        }

        private Object readResolve() {
            return EngineImpl.SERIALIZATION_CONTEXT.get().getEndPoint(name);
        }

        private static final long serialVersionUID = 1L;
    }

}
