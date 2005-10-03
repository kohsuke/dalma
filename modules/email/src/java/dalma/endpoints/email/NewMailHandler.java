package dalma.endpoints.email;

import dalma.Conversation;

import javax.mail.internet.MimeMessage;

/**
 * Callback interface that receives e-mails that are not correlated
 * to existing {@link Conversation}s.
 *
 * <p>
 * When a {@link Conversation} sends out an e-mail through
 * the {@link EmailEndPoint#waitForReply} methods, the reply e-mail
 * will go directly to that conversation. Other e-mails will be
 * sent to this listener.
 *
 * @see EmailEndPoint#setNewMailHandler(NewMailHandler)
 * @author Kohsuke Kawaguchi
 */
public interface NewMailHandler {
    /**
     * Called for each new e-mail.
     *
     * @param mail
     *      represents a received e-mail.
     *      always non-null.
     * @throws Exception
     *      if the method throws any {@link Throwable},
     *      it will be simply logged.
     */
    void onNewMail(MimeMessage mail) throws Exception;
}
