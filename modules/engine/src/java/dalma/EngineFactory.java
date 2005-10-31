package dalma;

import dalma.helpers.Java5Executor;
import dalma.helpers.ParallelInstrumentingClassLoader;
import dalma.helpers.ThreadPoolExecutor;
import dalma.impl.EngineImpl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Factory for {@link Engine}.
 *
 * <p>
 * This class is mostly useful when you are configuring {@link Engine} from IoC containers
 * such as Spring. When you are creating an {@link Engine} programatically, consider
 * using {@link #newEngine(File, ClassLoader, Executor)} directly.
 *
 * @author Kohsuke Kawaguchi
 */
public class EngineFactory {

    private File rootDir;
    private ClassLoader classLoader;
    private Executor executor;
    private final Map<String,EndPoint> endPoints = new HashMap<String,EndPoint>();

    /**
     * Creates a new uninitialized {@link EngineFactory}.
     */
    public EngineFactory() {
    }

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
     * Adds multiple {@link EndPoint}s from the given list.
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
     * Creates {@link EndPoint}s from endpoint URLs and adds them
     * to the engine when it's created.
     *
     * @param endPoints
     *      Keys are endpoint names, and values are endpoint URLs.
     */
    public void setEndPointURLs(Map<String,String> endPoints) {

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
    public static Engine newEngine(File rootDir,ClassLoader classLoader, Executor executor) throws IOException {
        return new EngineImpl(rootDir,classLoader,executor);
    }

    /**
     * Creates or loads a new {@link Engine} with a single invocation.
     *
     * <p>
     * This method is a convenient version of {@link #newEngine(File, ClassLoader, Executor)}
     * that uses the current thread's context class loader.
     *
     * @param rootDir
     *      see {@link #setRootDir(File)}
     * @param executor
     *      see {@link #setExecutor(Executor)}
     */
    public static Engine newEngine(File rootDir,Executor executor) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if(cl==null)    cl = EngineFactory.class.getClassLoader();
        return newEngine(rootDir,cl,executor);
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
     *      String like "org.acme.foo." See {@link ParallelInstrumentingClassLoader#ParallelInstrumentingClassLoader(ClassLoader, String)}
     *      for details.
     */
    public static Engine newEngine(String packagePrefix) throws IOException {
        Executor exec;

        try {
            exec = new Java5Executor(Executors.newCachedThreadPool());
        } catch (Throwable e) {
            // must be running in earlier JVM
            // TODO: implement CachedThreadPool
            exec = new ThreadPoolExecutor(1);
        }

        ClassLoader cl = new ParallelInstrumentingClassLoader(
            EngineFactory.class.getClassLoader(), packagePrefix );

        return newEngine(new File("dalma"), cl, exec );
    }
}
