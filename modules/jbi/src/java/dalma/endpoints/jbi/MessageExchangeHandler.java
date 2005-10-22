package dalma.endpoints.jbi;

import javax.jbi.messaging.MessageExchange;

/**
 * @author Kohsuke Kawaguchi
 */
public interface MessageExchangeHandler {
    /**
     * Called for each new {@link MessageExchange}.
     *
     * @param message
     *      represents a received message.
     *      always non-null.
     * @throws Exception
     *      if the method throws any {@link Throwable},
     *      it will be simply logged.
     */
    void onNewMessage(MessageExchange message) throws Exception;
}
