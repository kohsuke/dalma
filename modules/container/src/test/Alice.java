import dalma.endpoints.invm.Channel;
import dalma.endpoints.invm.Message;
import junit.framework.Assert;

import java.io.Serializable;
import java.util.UUID;

/**
 * Expects an echo and checks the reply.
 *
 * @author Kohsuke Kawaguchi
 */
public class Alice extends Assert implements Runnable, Serializable {
    private final Channel mine;
    private final Channel his;

    public Alice(Channel mine, Channel his) {
        this.mine = mine;
        this.his = his;
    }

    public void run() {
        String token = UUID.randomUUID().toString();
        for( int i=0; i<3; i++ ) {
            Message<String> msg = new Message<String>(token);
            System.out.println(toString()+" : sending  "+token);
            mine.send(msg,his);
            msg = mine.receive();
            System.out.println(toString()+" : received "+token);
            assertEquals(token,msg.payload);
        }
        System.out.println(toString()+" : quitting");
    }
}
