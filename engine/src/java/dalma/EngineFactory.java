package dalma;

import dalma.impl.EngineImpl;

import java.io.File;
import java.io.IOException;

/**
 * Factory for {@link Engine}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class EngineFactory {
    private EngineFactory() {}  // no instanciation

    /**
     * Creates or loads a new {@link Engine}.
     */
    public Engine newInstance(File rootDir,ClassLoader classLoader, Executor executor) throws IOException {
        return new EngineImpl(rootDir,classLoader,executor);
    }
}
