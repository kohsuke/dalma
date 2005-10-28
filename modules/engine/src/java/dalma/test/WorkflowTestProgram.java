package dalma.test;

import dalma.Conversation;
import dalma.Engine;
import dalma.helpers.ThreadPoolExecutor;
import dalma.impl.EngineImpl;
import dalma.impl.Util;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * Base class for workflow-based tests.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WorkflowTestProgram extends TestCase {
    protected Engine engine;
    private File root;
    private ClassLoader classLoader;

    protected WorkflowTestProgram(String name) {
        super(name);
    }

    protected final void setUp() throws Exception {
        // creates a new class loader that loads test classes with javaflow enhancements
        classLoader = new TestClassLoader(Launcher.class.getClassLoader());

        root = File.createTempFile("dalma","test-case");
        root.delete();
        root.mkdirs();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Util.deleteRecursive(root);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        engine = new EngineImpl(root,classLoader,new ThreadPoolExecutor(3,true));

        setupEndPoints();

        engine.start();
    }

    protected abstract void setupEndPoints() throws Exception;

    /**
     * Creates a new conversation instance with given parameters.
     */
    protected final Conversation createConversation(Class<? extends Runnable> c, Object... args ) throws Exception {
        Class clazz = classLoader.loadClass(c.getName());
        Constructor constructor = findConstructor(clazz, args.length);
        constructor.setAccessible(true);
        Runnable r = (Runnable)constructor.newInstance(args);

        return engine.createConversation(r);
    }

    private Constructor findConstructor(Class c, int length) throws NoSuchMethodException {
        for( Constructor init : c.getConstructors() )
            if(init.getParameterTypes().length==length)
                return init;

        throw new NoSuchMethodException("Unable to find a matching constructor on "+c.getName());
    }

    protected void tearDown() throws Exception {
        System.out.println("tearing down");
        engine.stop();
        Util.deleteRecursive(root);
    }

    /**
     * Obtains the property value of the given key.
     *
     * This mechanism allows the test harness to configure the tests.
     */
    protected final String getProperty(String key) {
        // Maven can pass in the property value as a system property
        String v = System.getProperty(key);
        if(v!=null)     return v;

        // otherwise let's check those properties by ourselves.
        File f = new File(".").getAbsoluteFile();
        do {
            File buildProperties = new File(f,"build.properties");
            if(buildProperties.exists()) {
                try {
                    Properties props = new Properties();
                    props.load(new FileInputStream(buildProperties));
                    if(props.containsKey(key))
                        return props.getProperty(key);
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        } while(!new File(f,"dalma.iml").exists());

        // TODO: explain what it means better
        throw new Error("test property "+key+" is not set.");
    }
}
