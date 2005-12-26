package dalma;

import dalma.endpoints.timer.TimerEndPoint;
import dalma.impl.ConversationImpl;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Base class of a workflow program.
 *
 * <p>
 * In Dalma, a workflow consists of two instances.
 * A {@link Conversation}, which is always retained in memory
 * to act as the outer shell of a workflow, and
 * a {@link Workflow}, which contains application state and
 * persisted to the disk. This class represents the inner shell part.
 *
 * <p>
 * This class also defines a bunch of convenience methods for
 * workflow programs to access various parts of dalma.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Workflow implements Runnable, Serializable {

    private ConversationImpl owner;

    /**
     * This method shall be overrided to implement the actual workflow code
     * (much like {@link Thread#run()}.
     */
    public abstract void run();


    /**
     * Gets the {@link Conversation} coupled with this workflow.
     *
     * @return
     *      always non-null. Same object.
     */
    public Conversation getOwner() {
        return owner;
    }

    /**
     * Invoked by the dalma implementation to set the owner.
     *
     * Shall never be used by anyone else.
     */
    public void setOwner(ConversationImpl owner) {
        this.owner = owner;
    }

    /**
     * Sets the title of this workflow.
     *
     * The title set from here will be made accessible
     * to {@link Conversation#getTitle()}
     *
     * @see Conversation#getTitle()
     */
    public void setTitle(String title) {
        owner.setTitle(title);
    }

    /**
     * Gets the {@link Logger} for this workflow.
     *
     * <p>
     * Logs recorded to this logger are persisted
     * as a part of {@link Conversation}, and allows the monitoring
     * application (and users) to see what's going on.
     */
    protected Logger getLogger() {
        return owner.getLogger();
    }

    /**
     * Sleeps the workflow for the given amount of time.
     *
     * <p>
     * This method differs from {@link Thread#sleep(long)} in that
     * this method doesn't block the thread that's running workflow.
     *
     * <p>
     * For this reason,
     * this method is recommended over {@link Thread#sleep(long)}.
     */
    protected static void sleep(long delay,TimeUnit unit) {
        TimerEndPoint.waitFor(delay,unit);
    }

    private static final long serialVersionUID = 1L;
}
