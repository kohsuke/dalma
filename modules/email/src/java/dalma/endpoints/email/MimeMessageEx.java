package dalma.endpoints.email;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.BodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
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

    public MimeMessageEx(Session session) {
        super(session);
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

    /**
     * Working around another bug in JavaMail.
     */
    public synchronized void setDataHandler(DataHandler dh) throws MessagingException {
        super.setDataHandler(dh);
//        content = null;
//        contentStream = null;
        saveChanges();
    }

    /**
     * Returns the real meat of the e-mail in text.
     *
     * <p>
     * The "real meat" is defined to be the primary text portion of the e-mail.
     * This method allows you to forget about such details like attachments,
     * SMIME, or HTML mails, and let you access the core part of the message.
     *
     * <p>
     * For example, if the e-mail is simply "text/plain", this method returns
     * the same string as {@link MimeMessage#getContent()}. If the e-mail is
     * a signed message where the primary part is "text/plain", this method
     * returns the string content of that "text/plain" part.
     *
     * <p>
     * This method correctly handles nesting of such elements (for example,
     * a signed HTML mail with attachments.)
     *
     * @return
     *      always non-null
     * @throws MessagingException
     *      if the e-mail fails to parse.
     * @throws IOException
     *      thrown by {@link DataHandler}. See {@link MimeMessage#getContent()}.
     */
    public String getMainContent() throws MessagingException, IOException {
        Object data = getContent();
        while(true) {
            if (data instanceof MimeMultipart) {
                MimeMultipart mul = (MimeMultipart) data;
                BodyPart bodyPart = mul.getBodyPart(0);
                data = bodyPart.getContent();
                continue;
            }
            if (data instanceof String)
                return (String)data;
            // unknown format
            throw new MessagingException("Unable to convert "+data+" to string");
        }
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


    public static void main(String[] args) throws Exception {
        System.out.println(new MimeMessageEx(
            Session.getInstance(System.getProperties()),System.in).getMainContent());
    }
}
