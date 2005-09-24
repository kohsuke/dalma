package dalma.ports.email;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * A wrapper around {@link MimeMessage} to make it serializable.
 *
 * @author Kohsuke Kawaguchi
 */
public class MimeMessageEx extends MimeMessage implements Serializable {
    public MimeMessageEx(MimeMessage source) throws MessagingException {
        super(source);
    }

    public MimeMessageEx(Session session, InputStream is) throws MessagingException {
        super(session, is);
    }

    protected void updateHeaders() throws MessagingException {
        // JavaMail clears the message-id with its own, so restore the one
        // we set
        String[] mid = getHeader("Message-ID");
        super.updateHeaders();
        if(mid!=null) {
            removeHeader("Message-ID");
            for( String h : mid )
                addHeader("Message-ID",h);
        }
    }

    public Message reply(boolean replyToAll) throws MessagingException {
        MimeMessage msg = (MimeMessage)super.reply(replyToAll);

        // set References header
        String msgId = getHeader("Message-Id", null);

        String header = getHeader("References"," ");
        if(header==null)
            header = "";
        header += ' '+msgId.trim();

        msg.setHeader("References",header);
        msg.setText("");    // set the dummy body otherwise the following method fails

        return new MimeMessageEx(msg);
    }

    private Object writeReplace() throws IOException, MessagingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo(baos);
        return new Moniker(baos.toByteArray());
    }

    private static final class Moniker implements Serializable {
        private final byte[] data;

        public Moniker(byte[] data) {
            this.data = data;
        }

        private Object readResolve() throws MessagingException {
            return new MimeMessageEx(
                Session.getInstance(System.getProperties()),
                new ByteArrayInputStream(data));
        }

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
