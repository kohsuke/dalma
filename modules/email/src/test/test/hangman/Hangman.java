package test.hangman;

import dalma.ports.email.EmailEndPoint;
import dalma.ports.email.NewMailHandler;
import dalma.ports.email.POP3Listener;
import dalma.test.Launcher;
import dalma.test.PasswordStore;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.Serializable;

/**
 * Hangman program written with Dalma.
 *
 * @author Kohsuke Kawaguchi
 */
public class Hangman extends Launcher implements NewMailHandler {
    public Hangman(String[] args) throws Exception {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new Hangman(args);
    }

    EmailEndPoint ep;

    protected void setUpEndPoints() throws Exception {
        ep = new EmailEndPoint(
            "email",
            new InternetAddress("dalma@kohsuke.org","hangman"),
            new POP3Listener("mail.kohsuke.org","dalma",PasswordStore.get("dalma@kohsuke.org"),3000));
        ep.setNewMailHandler(this);
        engine.addEndPoint(ep);
    }

    public void onNewMail(MimeMessage mail) throws Exception {
        System.out.println("Received a new e-mail");
        createConversation(ConversationImpl.class,ep,mail);
    }

    public static final class ConversationImpl implements Runnable, Serializable {
        private final EmailEndPoint ep;
        private final MimeMessage msg; // the first message

        public ConversationImpl(EmailEndPoint ep,MimeMessage msg) {
            this.ep = ep;
            this.msg = msg;
        }

        public void run() {
            try {
                // the answer
                String word = WordList.getRandomWord();

                int retry = 6;  // allow 6 guesses

                // the characters the user chose
                // true means selected
                boolean[] opened = new boolean[26];

                MimeMessage mail = msg; // last e-mail received

                while(retry>0) {
                    // send the hint
                    mail = (MimeMessage)mail.reply(false);
                    mail.setText(
                        "Word: "+maskWord(word,opened)+"\n\n" +
                        "You Chose: "+maskWord("abcdefghijklmnopqrstuvwxyz",opened)+"\n\n"+
                        retry+" more guesses\n");

                    mail = ep.waitForReply(mail);

                    // the char the user chose
                    String body = getMailBody(mail.getContent()).trim();
                    char ch = Character.toLowerCase(body.charAt(0));

                    if(word.indexOf(ch)<0) {
                        // bzzzt!
                        retry--;
                    }

                    opened[ch-'a']=true;

                    if(maskWord(word,opened).equals(word)) {
                        // bingo!
                        mail = (MimeMessage)mail.reply(false);
                        mail.setText("Bingo! The word was "+word);
                        ep.send(mail);
                        return;
                    }
                }

                MimeMessage reply = (MimeMessage)mail.reply(false);
                reply.setText("Bzzzt! The word was "+word);
                ep.send(reply);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String getMailBody(Object content) throws MessagingException, IOException {
            if(content instanceof MimeMultipart) {
                MimeMultipart mp= (MimeMultipart) content;
                return getMailBody(mp.getBodyPart(0).getContent());
            }
            return content.toString();
        }

        private String maskWord(String word, boolean[] opened) {
            String mask = word;
            for( int i=0; i<opened.length; i++ ) {
                if(!opened[i])
                    mask = mask.replace((char)('a'+i),'_');
            }
            return mask;
        }

        private static final long serialVersionUID = 1L;
    }
}
