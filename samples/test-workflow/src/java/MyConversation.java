import static dalma.TimeUnit.SECONDS;
import dalma.Workflow;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;

/**
 * @author Kohsuke Kawaguchi
 */
public class MyConversation extends Workflow {

    int id = iota++;
    Random r = new Random();
    public void run() {
        for( int i=0; i<5; i++ ) {
            setTitle(String.format("ID %1$s in its %2$s life",id,i));
            System.out.print('.');
            getLogger().fine("Rolling a dice");
            switch(r.nextInt(3)) {
            case 0:
                // reproduce
                getLogger().fine("reproduce");
                try {
                    getOwner().getEngine().createConversation(new MyConversation());
                } catch (IOException e) {
                    e.printStackTrace();
                    return; //hmm?
                }
                break;
            case 1:
                // noop
                getLogger().fine("noop");
                break;
            case 2:
                // die
                getLogger().log(Level.INFO, "exception test",
                    new FooException("msg1",new FooException("nested")));
                getLogger().warning("die");
                return;
            }
            sleep(3, SECONDS);
        }
    }

    static int iota = 0;
}
