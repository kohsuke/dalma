package dalma.container;

/**
 * State of {@link WorkflowApplication}.
 *
 * @author Kohsuke Kawaguchi
 */
public enum WorkflowState {
    /**
     * The workflow application is up and running, and is processing messages.
     */
    RUNNING,
    /**
     * The workflow application is loaded into memory, but not processing messages.
     *
     * <p>
     * In this omode, information about workflow can be accessed, but application
     * code is locked since it's loaded.
     */
    STOPPED,
    /**
     * The workflow application is completely removed from the memory.
     *
     * <p>
     * Many useful information about workflow cannot be accessed, but
     * application code can be replaced on disk while in this state.
     */
    UNLOADED
}
