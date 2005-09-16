package dalma.spi.port;

import dalma.Conversation;
import dalma.spi.ConversationSPI;

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
public interface EndPoint {
}
