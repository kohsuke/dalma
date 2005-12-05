import dalma.endpoints.invm.Channel;
import dalma.endpoints.invm.Message;

import java.io.Serializable;

/**
 * Echo back a message slowly.
 *
 * @author Kohsuke Kawaguchi
 */
public class Bob implements Runnable, Serializable {
    private final Channel mine;

    public Bob(Channel mine) {
        this.mine = mine;
    }

    public void run() {
        while(true) {
            Message m = mine.receive();
            System.out.println(toString()+" : got "+m.payload);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new Error(e); // impossible
            }
            // send it back
            mine.send(m,m.getFrom());
        }
    }
}
