package dalma.sample.hangman;

import dalma.EndPoint;
import dalma.TimeUnit;
import dalma.endpoints.email.EmailEndPoint;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.Serializable;

/**
 * A hangman game.
 *
 * <p>
 * One instance of this class represents one on-going game of Hangman.
 *
 * <p>
 * This class is bytecode-instrumented before execution.
 *
 * @author Kohsuke Kawaguchi
 */
public class HangmanWorkflow implements Runnable, Serializable {
    /**
     * {@link EndPoint} that we are talking to.
     */
    private final EmailEndPoint ep;
    private final MimeMessage msg; // the first message

    public HangmanWorkflow(EmailEndPoint ep, MimeMessage msg) {
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

                mail = ep.waitForReply(mail,7,TimeUnit.DAYS);
                if(mail==null) {
                    // it's been 1 week. sorry.
                    System.out.println("timeout");
                    return;
                }

                System.out.println("Received a reply from "+mail.getFrom()[0]);

                // pick up the char the user chose
                char ch =getSelectedChar(mail.getContent());
                if(ch==0)
                    continue;

                if(word.indexOf(ch)<0) {
                    // bzzzt!
                    retry--;
                }

                opened[ch-'a']=true;

                if(maskWord(word,opened).equals(word)) {
                    // bingo!
                    mail = (MimeMessage)mail.reply(false);
                    mail.setText("Bingo! The word was\n\n   "+word);
                    ep.send(mail);
                    return;
                }
            }

            MimeMessage reply = (MimeMessage)mail.reply(false);
            reply.setText("Bzzzt! The word was\n\n   "+word);
            ep.send(reply);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Picks up a character chosen by the user.
     *
     * The algorithm is to find the first line that contains just one character.
     */
    private static char getSelectedChar(Object content) throws MessagingException, IOException {
        String body = getMailBody(content);
        for( String line : body.split("\n") ) {
            line = line.trim();
            if(line.length()==1) {
                char ch = Character.toLowerCase(line.charAt(0));
                if('a'<=ch && ch<='z')
                    return ch;
            }
        }
        return 0;
    }

    /**
     * Obtains the mail body as a string.
     *
     * E-mail contents can be delivered in so many forms
     * (plain text, HTML, or S/MIME packaged, ...), so this has to be
     * somewhat arbitrary and ugly.
     */
    private static String getMailBody(Object content) throws MessagingException, IOException {
        if(content instanceof MimeMultipart) {
            MimeMultipart mp= (MimeMultipart) content;
            return getMailBody(mp.getBodyPart(0).getContent());
        }
        return content.toString();
    }

    /**
     * Creates a mask of a word (e.g., p__tt_ from pretty) based on
     * the currently selected set of characters.
     *
     * @param word
     *      word to mask
     * @param opened
     *      each item in the array represents whether or not the character is
     *      chosen.
     */
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
