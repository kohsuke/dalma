import dalma.Engine;
import static dalma.TimeUnit.SECONDS;
import dalma.endpoints.timer.TimerEndPoint;

import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

/**
 * @author Kohsuke Kawaguchi
 */
public class MyConversation implements Runnable, Serializable {
    int i = iota++;
    Random r = new Random();
    public void run() {
        for( int i=0; i<5; i++ ) {
            System.out.print('.');
            switch(r.nextInt(3)) {
            case 0:
                // reproduce
                try {
                    Engine.currentEngine().createConversation(new MyConversation());
                } catch (IOException e) {
                    e.printStackTrace();
                    return; //hmm?
                }
                break;
            case 1:
                // noop
                break;
            case 2:
                // die
                return;
            }
            TimerEndPoint.waitFor(3, SECONDS);
        }
    }

    static int iota = 0;
}
