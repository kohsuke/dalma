package dalma.test;

import dalma.Engine;
import dalma.Conversation;
import dalma.helpers.ThreadPoolExecutor;
import dalma.impl.EngineImpl;
import dalma.impl.Util;
import junit.framework.TestCase;
import org.apache.commons.javaflow.ContinuationClassLoader;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.net.URLClassLoader;
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

    protected void setUp() throws Exception {
        // creates a new class loader that loads test classes with javaflow enhancements
        URLClassLoader baseLoader = (URLClassLoader)Launcher.class.getClassLoader();
        classLoader = new ContinuationClassLoader(
            baseLoader.getURLs(),
            new MaskingClassLoader(baseLoader));

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

        engine = new EngineImpl(root,classLoader,new ThreadPoolExecutor(3));
    }

    /**
     * Creates a new conversation instance with given parameters.
     */
    protected final Conversation createConversation(Class<? extends Runnable> c, Object... args ) throws Exception {
        Class clazz = classLoader.loadClass(c.getName());
        Runnable r = (Runnable)findConstructor(clazz,args.length).newInstance(args);

        return engine.createConversation(r);
    }

    private Constructor findConstructor(Class c, int length) throws NoSuchMethodException {
        for( Constructor init : c.getConstructors() )
            if(init.getParameterTypes().length==length)
                return init;

        throw new NoSuchMethodException("Unable to find a matching constructor on "+c.getName());
    }

    protected void tearDown() throws Exception {
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
        while(!new File(f,"dalma.iml").exists()) {
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
        }

        // TODO: explain what it means better
        throw new Error("test property "+key+" is not set.");
    }
}
