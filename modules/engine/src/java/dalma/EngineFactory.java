package dalma;

import dalma.impl.EngineImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

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
    private final Map<String,EndPoint> endPoints = new HashMap<String,EndPoint>();

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
     * Adds a new {@link EndPoint} to the engine.
     *
     * @throws IllegalArgumentException
     *      if there's already an {@link EndPoint} that has the same name.
     */
    public void addEndPoint(EndPoint ep) {
        endPoints.put(ep.getName(),ep);
    }

    /**
     * Copies the list of {@link EndPoint}s from the given list.
     *
     * <p>
     * This method completely removes all the {@link EndPoint}s configured so far
     * by the specified {@link EndPoint}s.
     *
     * <p>
     * Note that this method copies the values from the collection but not the
     * collection itself. Therefore once the method returns, changes can be
     * made to the collection object that is used for this method invocation
     * and it will not affect the engine.
     */
    public void setEndPoints(Collection<? extends EndPoint> endPoints) {
        for (EndPoint endPoint : endPoints)
            addEndPoint(endPoint);
    }

    /**
     * Creates a new {@link Engine} based on the current configuration.
     */
    public Engine newInstance() throws IOException {
        EngineImpl engine = new EngineImpl(rootDir, classLoader, executor);
        for (EndPoint endPoint : endPoints.values()) {
            engine.addEndPoint(endPoint);
        }
        return engine;
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
