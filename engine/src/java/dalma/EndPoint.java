package dalma;

import dalma.impl.EngineImpl;

import java.io.Serializable;

/**
 * Represents a gate through which {@link Conversation}s communicate with
 * outer world.
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

    private static final long serialVersionUID = 1L;
}
