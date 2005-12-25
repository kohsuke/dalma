package test;

import dalma.endpoints.irc.Channel;
import dalma.endpoints.irc.IRCEndPoint;
import dalma.endpoints.irc.Message;
import dalma.endpoints.irc.NewSessionListener;
import dalma.endpoints.irc.PrivateChat;
import dalma.endpoints.irc.Buddy;
import dalma.spi.ConversationSPI;
import dalma.test.Launcher;

import java.io.Serializable;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class IRCDemo extends Launcher {
    private IRCEndPoint iep;

    public IRCDemo(String[] args) throws Exception {
        super(args);
    }

    protected void setUpEndPoints() throws Exception {
        iep=new IRCEndPoint("irc1",/*"irc.blessed.net"*/"irc.central.sun.com","dalma");
        iep.setNewSessionListener(new NewSessionListener() {
            public void onNewPrivateChat(PrivateChat chat) {
                try {
                    createConversation(ConversationImpl.class,iep,chat);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onInvite(Buddy sender, Channel channel) {
                try {
                    createConversation(ChannelConversationImpl.class,iep,channel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        engine.addEndPoint(iep);
    }

    public static void main(String[] args) throws Exception {
        new IRCDemo(args);
    }

    public static final class ConversationImpl implements Runnable, Serializable {
        private final PrivateChat chat;
        private final IRCEndPoint iep;

        public ConversationImpl(IRCEndPoint iep,PrivateChat chat) {
            this.iep = iep;
            this.chat = chat;
        }

        public void run() {
            chat.send("started");
            while(true) {
                Message msg = chat.waitForNextMessage();
                String text = msg.getText();
                if(text.equals("bye")) {
                    chat.send("bye!");
                    chat.close();
                    return;
                }
                if(text.startsWith("join ")) {
                    text = text.substring(5);
                    try {
                        ConversationSPI.currentConversation().getEngine()
                            .createConversation(new ChannelConversationImpl(iep,iep.getChannel(text)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    chat.send("OK.");
                    continue;
                }
                chat.send("You said "+text);
            }
        }
    }

    public static final class ChannelConversationImpl implements Runnable, Serializable {
        private final Channel channel;
        private final IRCEndPoint iep;


        public ChannelConversationImpl(IRCEndPoint iep, Channel channel) {
            this.iep = iep;
            this.channel = channel;
        }

        public void run() {
            channel.join();
            channel.send("joined");
            while(true) {
                Message msg = channel.waitForNextMessage();
                String text = msg.getText();
                if(text.equals("bye")) {
                    channel.send("bye!");
                    channel.close();
                    return;
                }
                channel.send("You said "+text);
            }
        }
    }
}
