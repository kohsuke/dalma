package dalma.endpoints.irc;

import f00f.net.irc.martyr.IRCConnection;
import f00f.net.irc.martyr.services.AutoReconnect;

/**
 * {@link AutoReconnect} that remembers the server name and the port.
 * @author Kohsuke Kawaguchi
 */
final class AutoReconnectEx extends AutoReconnect {
    private final String ircServer;
    private final int port;

    public AutoReconnectEx(IRCConnection connection, String ircServer, int port) {
        super(connection);
        this.ircServer = ircServer;
        this.port = port;
    }

    public void go() {
        super.go(ircServer,port);
    }
}
