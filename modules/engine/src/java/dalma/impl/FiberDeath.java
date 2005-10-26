package dalma.impl;

import dalma.Conversation;

/**
 * Thrown to forcibly kill the fiber.
 *
 * Used to remove a {@link Conversation}.
 *
 * @author Kohsuke Kawaguchi
 */
public class FiberDeath extends Error {
}
