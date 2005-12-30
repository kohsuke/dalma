package @PACKAGE@;

import dalma.Workflow;
import static dalma.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.Random;

/**
 * A sample workflow.
 */
public class ConversationImpl extends Workflow {
    
    // these instance variables live across JVM sessions
    private final int id = iota++;
    private final Random r = new Random();
    
    @Override
    public void run() {
        for( int i=0; i<5; i++ ) {
            // setting the title allows the management UI to see what this workflow is about
            setTitle(String.format("ID %1$s in its iteration %2$s",id,i));
            
            // logged messages can be seen from the management UI, and therefore
            // ideal for recording what happened during the course of a workflow.
            getLogger().fine("Rolling a dice");
            switch(r.nextInt(3)) {
            case 0:
                // reproduce : create another workflow
                getLogger().fine("reproduce");
                try {
                    getOwner().getEngine().createConversation(new ConversationImpl());
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
                getLogger().fine("die");
                return;
            }
            
            // sleep 3 seconds
            sleep(3, SECONDS);
        }
    }

    static int iota = 0;
}
