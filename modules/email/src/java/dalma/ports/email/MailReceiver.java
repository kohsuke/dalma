package dalma.ports.email;

import javax.mail.internet.MimeMessage;
import java.util.UUID;

/**
 * @author Kohsuke Kawaguchi
 */
interface MailReceiver {
    UUID getUUID();
    void handleMessage(MimeMessage msg);
}
