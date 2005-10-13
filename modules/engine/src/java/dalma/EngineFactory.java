package dalma;

import dalma.impl.EngineImpl;
import dalma.helpers.ThreadPoolExecutor;
import dalma.helpers.Java5Executor;
import dalma.helpers.ReloadingConversationClassLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;

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
     *
     * @throws IOException
     *      if the engine failed to set up the files for persistence,
     *      or fails to read from the existing persisted conversations.
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

    /**
     * The easiest way to create a new {@link Engine}.
     *
     * <p>
     * This method creates a new {@link Engine} with the following configuration.
     *
     * <ol>
     *  <li>"./dalma" directory is used to store the data.
     *  <li>the conversation programs are assumed to be in the specified
     *      package name. A new {@link ClassLoader} is created to load
     *      classes in this package with necessary bytecode instrumentation.
     *  <li>If you are running in JRE 5.0 or later,
     *      {@link Executors#newCachedThreadPool()}
     *      is used to run conversions. If you are running in earlier versions
     *      of JRE, then a single worker thread is used to run conversations.
     *
     * @param packagePrefix
     *      String like "org.acme.foo." See {@link ReloadingConversationClassLoader#ReloadingConversationClassLoader(ClassLoader, String)}
     *      for details.
     */
    public static Engine newInstance(String packagePrefix) throws IOException {
        Executor exec;

        try {
            exec = new Java5Executor(Executors.newCachedThreadPool());
        } catch (Throwable e) {
            // must be running in earlier JVM
            // TODO: implement CachedThreadPool
            exec = new ThreadPoolExecutor(1);
        }

        ClassLoader cl = new ReloadingConversationClassLoader(
            EngineFactory.class.getClassLoader(), packagePrefix );

        return newInstance(new File("dalma"), cl, exec );
    }
}
