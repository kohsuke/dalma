package dalma;

/**
 * @author Kohsuke Kawaguchi
 */
public interface Fiber {
    /**
     * Gets the current state of the fiber.
     *
     * @return never null
     */
    FiberState getState();

    /**
     * Gets the {@link Conversation} that this fiber belongs to.
     *
     * A {@link Fiber} always belongs to the same {@link Conversation}.
     *
     * @return never null
     */
    Conversation getOwner();
}
