package test.infra;

import test.ClickTest;
import test.ClickConversation;

import java.net.URLClassLoader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.javaflow.ContinuationClassLoader;
import org.apache.commons.javaflow.bytecode.transformation.bcel.BcelClassTransformer;
import dalma.impl.Util;
import dalma.impl.EngineImpl;
import dalma.Engine;
import dalma.Conversation;
import dalma.helpers.ThreadPoolExecutor;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Launcher {

    /**
     * The {@link ClassLoader} that can load instrumented conversation classes.
     */
    protected final ClassLoader classLoader;

    /**
     * Dalma engine.
     */
    protected final Engine engine;

    protected Launcher(String[] args) throws Exception {
        BcelClassTransformer.debug = true;

        // creates a new class loader that loads test classes with javaflow enhancements
        URLClassLoader baseLoader = (URLClassLoader)Launcher.class.getClassLoader();
        classLoader = new ContinuationClassLoader(
            baseLoader.getURLs(),
            new MaskingClassLoader(ClickTest.class.getClassLoader()));

        File root = new File("dalma-test");
        root.mkdirs();
        if(args.length>0) {
            // start fresh
            System.out.println("Starting fresh");
            Util.deleteContentsRecursive(root);
        } else {
            System.out.println("Picking up existing conversations");
        }

        engine = new EngineImpl(root,classLoader,new ThreadPoolExecutor(1));

        if(args.length>0) {
            init();
        }
    }

    /**
     * Creates initial set of conversations.
     */
    protected abstract void init() throws Exception;

    /**
     * Creates a new conversation instance with given parameters.
     */
    protected Conversation createConversation(Class<? extends Runnable> c, Object... args ) throws Exception {
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
}
