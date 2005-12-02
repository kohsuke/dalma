package dalma.container;

import dalma.Engine;
import dalma.impl.EngineImpl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ArrayList;

/**
 * Wrapper around each workflow application.
 *
 * Each workflow application will have a separate {@link Engine}
 * and {@link ClassLoader}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class WorkflowApplication {

    /**
     * The name of the workflow application
     * that uniquely identifies a {@link WorkflowApplication}.
     */
    public final String name;

    private Engine engine;

    /**
     * The {@link Container} that owns this.
     */
    public final Container owner;

    /**
     * Root of the working directory.
     */
    private final File workDir;

    /**
     * Root of the class directory.
     */
    private final File appDir;

    public WorkflowApplication(Container owner,File appDir) {
        this.owner = owner;
        this.name = appDir.getName();
        this.workDir = new File(new File(owner.rootDir, "work"), name);
        this.appDir  = appDir;
    }

    public void start() throws IOException {
        if(engine!=null)
            return; // already started

        engine = new EngineImpl(
            new File(workDir,"data"),
            createClassLoader(),
            owner.executor);

        // TODO: set up endpoints

        engine.start();
    }

    /**
     * Creates a new {@link ClassLoader} that loads workflow application classes.
     */
    private ClassLoader createClassLoader() throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();

        // list up *.jar files in the appDir
        File[] jarFiles = appDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        for (File jar : jarFiles)
            urls.add(jar.toURI().toURL());

        // and add the workflow application folder itself
        urls.add(appDir.toURI().toURL());

        return new URLClassLoader(
            urls.toArray(new URL[urls.size()]),
            getClass().getClassLoader());
    }

    public void stop() {
        if(engine==null)
            return; // already stopped

        try {
            engine.stop();
        } catch (InterruptedException e) {
            // process the interruption later
            Thread.currentThread().interrupt();
        }
        engine = null;
    }
}
