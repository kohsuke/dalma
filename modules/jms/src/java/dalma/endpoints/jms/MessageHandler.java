package dalma.endpoints.jms;

import dalma.Conversation;

import javax.jms.Message;

/**
 * Callback interface that receives JMS messages that are not correlated
 * to existing {@link Conversation}s.
 *
 * <p>
 * Those {@link Message}s that have a correlation ID back to to the message
 * sent from a conversation will be considered as replies and therefore
 * routed to the appropriate conversation. This handler receives other
 * messages.
 *
 * @see JMSEndPoint#setNewMessageHandler(MessageHandler)
 * @author Kohsuke Kawaguchi
 */
public interface MessageHandler {
    /**
     * Called for each new message.
     *
     * @param message
     *      represents a received message.
     *      always non-null.
     * @throws Exception
     *      if the method throws any {@link Throwable},
     *      it will be simply logged.
     */
    void onNewMessage(Message message) throws Exception;
}
