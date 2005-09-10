package test.infra;

import test.ClickTest;
import test.ClickConversation;

import java.net.URLClassLoader;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

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
public class Launcher {
    public static void main(Class<? extends Runnable>... convClazz) throws Exception {
        BcelClassTransformer.debug = true;

        // creates a new class loader that loads test classes with javaflow enhancements
        URLClassLoader baseLoader = (URLClassLoader)Launcher.class.getClassLoader();
        ClassLoader cl = new ContinuationClassLoader(
            baseLoader.getURLs(),
            new MaskingClassLoader(ClickTest.class.getClassLoader()));

        File root = new File("dalma-test");
        root.mkdirs();
        if(convClazz.length>0) {
            // start fresh
            System.out.println("Starting fresh");
            Util.deleteContentsRecursive(root);
        } else {
            System.out.println("Picking up existing conversations");
        }

        Engine engine = new EngineImpl(root,cl,new ThreadPoolExecutor(1));

        List<Conversation> convs = new ArrayList<Conversation>();

        for (Class<? extends Runnable> c : convClazz) {
            Class clazz = cl.loadClass(c.getName());
            Runnable r = (Runnable)clazz.newInstance();

            convs.add( engine.createConversation(r) );
        }

        // press Ctrl+C to break.
    }
}
