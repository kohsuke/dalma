package dalma.impl;

import java.io.Serializable;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class GeneratorImpl implements Serializable {

    /**
     * Called after this generator is restored from disk.
     *
     * Typically used to requeue this object.
     * This happens while the conversation is being restored from the disk.
     */
    protected abstract void onLoad();

    /**
     * Called when the conversation completes. Used to dequeue this object.
     */
    protected abstract void interrupt();

    private static final long serialVersionUID = 1L;
}
