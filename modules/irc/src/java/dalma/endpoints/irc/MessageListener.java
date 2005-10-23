package dalma.endpoints.irc;

import f00f.net.irc.martyr.Command;
import f00f.net.irc.martyr.commands.MessageCommand;
import f00f.net.irc.martyr.commands.InviteCommand;

import java.util.Observable;
import java.util.Observer;

/**
 * Listens to the incoming {@link Command} objects.
 *
 * @author Kohsuke Kawaguchi
 */
final class MessageListener implements Observer {
    private final IRCEndPoint endPoint;

    public MessageListener(IRCEndPoint endPoint) {
        this.endPoint = endPoint;
    }

    public void update(Observable _, Object cmd) {
        handleCommand((Command)cmd);
    }

    private void handleCommand(Command cmd) {
        if (cmd instanceof MessageCommand) {
            MessageCommand msgcmd = (MessageCommand) cmd;
            endPoint.onMessageReceived(msgcmd);
        }
        if (cmd instanceof InviteCommand) {
            InviteCommand invcmd = (InviteCommand)cmd;
            endPoint.onInvite(invcmd);
        }
    }
}
