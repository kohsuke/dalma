package dalma.container;

import org.apache.commons.javaflow.ContinuationClassLoader;
import org.apache.commons.javaflow.bytecode.transformation.bcel.BcelClassTransformer;
import org.apache.bcel.util.ClassLoaderRepository;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link ClassLoader}.
 * 
 * @author Kohsuke Kawaguchi
 */
class ClassLoaderBuilder {
    /**
     * List of {@link URL}s to be made available.
     */
    private final List<URL> urls = new ArrayList<URL>();

    private final ClassLoader parent;

    public ClassLoaderBuilder(ClassLoader parent) {
        this.parent = parent;
    }

    /**
     * Adds all the jar files in the given directory.
     */
    public void addJarFiles(File dir) {
        // list up *.jar files in the appDir
        File[] jarFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        if(jarFiles==null)
            return;     // no such dir

        for (File jar : jarFiles)
            addPathElement(jar);
    }

    /**
     * Adds a path element.
     *
     * @param dir
     *      A directory or a jar file.
     */
    public void addPathElement(File dir) {
        try {
            urls.add(dir.toURI().toURL());
        } catch (IOException e) {
            throw new AssertionError(e);    // impossible
        }
    }

    public ClassLoader make() {
        return new URLClassLoader(urls.toArray(new URL[urls.size()]),parent);
    }

    public ClassLoader makeContinuable() {
        DelegatingRepository dr = new DelegatingRepository();
        ClassLoader cl = new ContinuationClassLoader(urls.toArray(new URL[urls.size()]), parent,
            new BcelClassTransformer(dr));
        dr.setRepository(new ClassLoaderRepository(cl));
        return cl;
    }
}
