package dalma;

import java.io.Serializable;

/**
 * Base class of a workflow program.
 *
 * <p>
 * In Dalma, a workflow consists of two instances.
 * A {@link Conversation}, which is always retained in memory
 * to act as the outer shell of a workflow, and
 * a {@link Workflow}, which contains application state and
 * persisted to the disk.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Workflow implements Runnable, Serializable {

    private Conversation owner;

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
    public void setOwner(Conversation owner) {
        this.owner = owner;
    }

    /**
     * Sets the title of this workflow.
     *
     *
     */
    public void setTitle(String title) {
        owner.setTitle(title);
    }


    private static final long serialVersionUID = 1L;
}
