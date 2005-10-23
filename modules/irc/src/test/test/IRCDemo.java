package test;

import dalma.test.Launcher;
import dalma.endpoints.irc.IRCEndPoint;
import dalma.endpoints.irc.NewSessionListener;
import dalma.endpoints.irc.PrivateChat;
import dalma.endpoints.irc.Channel;
import dalma.endpoints.irc.Message;

import java.io.Serializable;

/**
 * @author Kohsuke Kawaguchi
 */
public class IRCDemo extends Launcher {
    private IRCEndPoint iep;

    public IRCDemo(String[] args) throws Exception {
        super(args);
    }

    protected void setUpEndPoints() throws Exception {
        iep=new IRCEndPoint("irc1","irc.blessed.net","dalma");
        iep.setNewSessionListener(new NewSessionListener() {
            public void onNewPrivateChat(PrivateChat chat) {
                try {
                    createConversation(ConversationImpl.class,chat);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onInvite(Channel channel) {
            }
        });
        engine.addEndPoint(iep);
    }

    public static void main(String[] args) throws Exception {
        new IRCDemo(args);
    }

    public static final class ConversationImpl implements Runnable, Serializable {
        private final PrivateChat chat;

        public ConversationImpl(PrivateChat chat) {
            this.chat = chat;
        }

        public void run() {
            chat.send("started");
            while(true) {
                Message msg = chat.waitForNextMessage();
                if(msg.getText().equals("bye")) {
                    chat.send("bye!");
                    chat.close();
                    return;
                }
                chat.send("You said "+msg.getText());
            }
        }
    }
}
