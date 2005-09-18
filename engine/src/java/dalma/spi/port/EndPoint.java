package dalma.spi.port;

import dalma.Engine;
import dalma.impl.EngineImpl;

import java.io.Serializable;

/**
 * Root of the endPoint SPI.
 *
 * <p>
 * A endPoint needs to be serializable, because it maybe referenced from
 * user conversations. When deserialized, a endPoint should bind to the
 * running instances of the endPoint.
 *
 * TODO: One engine may have more than one instances of the same endPoint
 * (such as using multiple POP3 ports, etc.) How do we configure this?
 * Perhaps by using Spring?
 *
 * TODO: allow JMX to monitor the endPoint status
 *
 * TODO: endPoint setting needs to be persistable. How do we do this?
 * ports tend to have other native resources.
 *
 * TODO: check if the endPoint is really necessary at the engine level.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class EndPoint implements Serializable {
    private final String name;

    protected EndPoint(String name) {
        this.name = name;
    }

    /**
     * Gets the unique name that identifies this {@link EndPoint} within an {@link Engine}.
     *
     * @return
     *      always non-null valid object.
     */
    public String getName() {
        return name;
    }

    private Object writeReplace() {
        if(EngineImpl.SERIALIZATION_CONTEXT.get()==null)
            return this;
        else
            return new Moniker(name);
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

    private static final long serialVersionUID = 1L;
}
