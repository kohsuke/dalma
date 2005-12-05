package dalma.container;

import dalma.Engine;
import dalma.Program;
import dalma.impl.EngineImpl;
import org.apache.commons.javaflow.ContinuationClassLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper around each workflow application.
 *
 * Each workflow application will have a separate {@link Engine}
 * and {@link ClassLoader}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class WorkflowApplication {
    private static final Logger logger = Logger.getLogger(WorkflowApplication.class.getName());

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

    private Program program;

    public WorkflowApplication(Container owner,File appDir) {
        this.owner = owner;
        this.name = appDir.getName();
        this.workDir = new File(new File(owner.rootDir, "work"), name);
        this.appDir  = appDir;
    }

    public void start() throws IOException {
        if(engine!=null)
            return; // already started

        logger.info("Starting "+name);

        ClassLoader classLoader = createClassLoader();
        engine = new EngineImpl(
            new File(workDir,"data"),
            classLoader,
            owner.executor);

        Class<?> mainClass;
        try {
            mainClass = classLoader.loadClass("Main");
            Object main = mainClass.newInstance();
            if(!(main instanceof Program)) {
                logger.severe(mainClass.getName()+" doesn't extend the Program class");
                return;
            }
            program = (Program)main;
        } catch (ClassNotFoundException e) {
            log("Failed to load the main class from application",e);
            return;
        } catch (InstantiationException e) {
            log("Failed to load the main class from application",e);
            return;
        } catch (IllegalAccessException e) {
            log("Failed to load the main class from application",e);
            return;
        }

        // TODO: set up endpoints

        try {
            program.init(engine);
        } catch (Exception e) {
            // faled
            log(mainClass.getName()+".init() method reported an exception",e);
            return;
        }

        engine.start();

        try {
            program.main(engine);
        } catch (Exception e) {
            // faled
            log(mainClass.getName()+".main() method reported an exception",e);
            return;
        }

        logger.info("Started "+name);
    }

    private static void log(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t );
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

        return new ContinuationClassLoader(
            urls.toArray(new URL[urls.size()]),
            getClass().getClassLoader());
    }

    public void stop() {
        if(engine==null)
            return; // already stopped

        logger.info("Stopping "+name);

        if(program!=null) {
            try {
                program.cleanup(engine);
            } catch (Exception e) {
                log(program.getClass().getName()+".cleanup() method reported an exception",e);
            }
            program = null;
        }

        try {
            engine.stop();
        } catch (InterruptedException e) {
            // process the interruption later
            Thread.currentThread().interrupt();
        }
        engine = null;

        logger.info("Stopped "+name);
    }
}
