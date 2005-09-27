package dalma.ports.email;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.UUID;

/**
 * Hides the detail about the handling of the outgoing e-mail.
 *
 * @author Kohsuke Kawaguchi
 */
final class Sender {
    public final UUID uuid;
    private MimeMessage msg;

    Sender(MimeMessage msg) throws MessagingException {
        this.msg = msg;

        // this creates a cryptographically strong GUID,
        // meaning someone who knows any number of GUIDs can't
        // predict another one (to steal the session)
        uuid = UUID.randomUUID();
        msg.setHeader("Message-ID",'<'+uuid.toString()+"@localhost>");
    }

    void send() {
        if(msg==null)
            return;

        try {
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new EmailException(e);
        } finally {
            msg = null;
        }
    }
}
