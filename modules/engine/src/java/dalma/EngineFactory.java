package dalma;

import dalma.impl.EngineImpl;

import java.io.File;
import java.io.IOException;

/**
 * Factory for {@link Engine}.
 *
 * <p>
 * This class is mostly useful when you are configuring {@link Engine} from IoC containers
 * such as Spring. When you are creating an {@link Engine} programatically, consider
 * using {@link #newInstance(File, ClassLoader, Executor)} directly.
 *
 * @author Kohsuke Kawaguchi
 */
public class EngineFactory {

    private File rootDir;
    private ClassLoader classLoader;
    private Executor executor;

    /**
     * Sets the directory to be used for persisting the state of conversations.
     *
     * @param rootDir
     *      must not be null. This directory must exist.
     */
    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
    }

    // TODO
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Sets the thread pool that executes the conversations.
     *
     * @param executor
     *      must not be null.
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * Creates a new {@link Engine} based on the current configuration.
     */
    public Engine newInstance() throws IOException {
        return new EngineImpl(rootDir,classLoader,executor);
    }

    /**
     * Creates or loads a new {@link Engine} with a single invocation.
     *
     * @param rootDir
     *      see {@link #setRootDir(File)}
     * @param classLoader
     *      see {@link #setClassLoader(ClassLoader)}
     * @param executor
     *      see {@link #setExecutor(Executor)} 
     */
    public static Engine newInstance(File rootDir,ClassLoader classLoader, Executor executor) throws IOException {
        return new EngineImpl(rootDir,classLoader,executor);
    }
}
