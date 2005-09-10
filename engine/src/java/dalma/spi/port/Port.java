package dalma.spi.port;

import dalma.Conversation;
import dalma.spi.ConversationSPI;

/**
 * Root of the port SPI.
 *
 * <p>
 * A port needs to be serializable, because it maybe referenced from
 * user conversations. When deserialized, a port should bind to the
 * running instances of the port.
 *
 * TODO: One engine may have more than one instances of the same port
 * (such as using multiple POP3 ports, etc.) How do we configure this?
 * Perhaps by using Spring?
 *
 * TODO: allow JMX to monitor the port status
 *
 * TODO: port setting needs to be persistable. How do we do this?
 * ports tend to have other native resources.
 *
 * TODO: check if the port is really necessary at the engine level.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Port {
}
