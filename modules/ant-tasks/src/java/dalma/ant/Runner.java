package dalma.ant;

import dalma.Engine;
import dalma.EngineFactory;
import dalma.Program;
import dalma.container.ClassLoaderImpl;
import dalma.container.model.IllegalResourceException;
import dalma.container.model.InjectionException;
import dalma.container.model.Model;
import dalma.helpers.ThreadPoolExecutor;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * Runs a workflow application in a stand-alone mode.
 *
 * @author Kohsuke Kawaguchi
 */
public class Runner extends Task {
    private File workDir;

    private final Path classpath = new Path(null);

    private final PropList props = new PropList();
    private boolean parentFirst = true;

    private String mainClassName = null;

    /**
     * Sets the work directory.
     */
    public void setWorkDir(File workDir) {
        this.workDir = workDir;
    }

    // for nested <classpath> element
    public Path createClasspath() {
        return classpath.createPath();
    }

    // for classpath attribute
    public void setClasspath( Path cp ) {
        classpath.createPath().append(cp);
    }

    /**
     * Configures the resource injection.
     */
    public void addProperty(Environment.Variable p) {
        props.addVariable(p);
    }

    public void setPropertyFile(File propFile) {
        try {
            props.addPropertyFile(propFile);
        } catch (IOException e) {
            throw new BuildException("Failed to load "+propFile,e);
        }
    }

    public void setParentFirst(boolean b) {
        this.parentFirst = b;
    }

    public File getWorkDir() {
        if(workDir==null) {
            try {
                workDir = File.createTempFile("dalma","ws");
                workDir.delete();
                workDir.mkdir();
            } catch (IOException e) {
                // very unlikely
                throw new Error("Unable to allocate a temporary directory");
            }
        }
        return workDir;
    }

    /**
     * Sets the main {@link Program} class to run.
     *
     * @param mainClass
     *      Can be null, in which case the main class will be determined by
     *      the manifest.
     */
    public void setMainClass(String mainClass) {
        this.mainClassName = mainClass;
    }

    public void execute() {
        // Container con = Container.create(getWorkDir());
        System.out.println(getWorkDir());

        ClassLoaderImpl loader = new ClassLoaderImpl(getClass().getClassLoader());
        loader.makeContinuable();
        loader.setParentFirst(parentFirst);
        Engine engine;

        try {
            classpath.setProject(getProject());
            for( String pathElement : classpath.list() ) {
                loader.addPathFile(getProject().resolveFile(pathElement));
            }

            engine = EngineFactory.newEngine(getWorkDir(),loader,
                new ThreadPoolExecutor(1,true));
        } catch (IOException e) {
            throw new BuildException("Unable to initialize engine: "+e.getMessage(),e);
        }

        Class<?> mainClass;
        Program program;

        try {
            if(this.mainClassName!=null)
                mainClass = loader.loadClass(mainClassName);
            else
                mainClass = loader.loadMainClass();
            Object _program = mainClass.newInstance();
            if (!(_program instanceof Program)) {
                throw new BuildException(mainClass.getName()+" doesn't inherit Program");
            }

            program = (Program) _program;
        } catch (ClassNotFoundException e) {
            throw new BuildException("Failed to load the main class: "+e.getMessage(),e);
        } catch (IOException e) {
            throw new BuildException("Failed to load the main class: "+e.getMessage(),e);
        } catch (IllegalAccessException e) {
            throw new BuildException("Failed to load the main class: "+e.getMessage(),e);
        } catch (InstantiationException e) {
            throw new BuildException("Failed to load the main class: "+e.getMessage(),e);
        }

        try {
            Model model = new Model(mainClass);
            model.inject(engine,program,props.toProperties());
        } catch (InjectionException e) {
            throw new BuildException("Failed to configure program: "+e.getMessage(),e);
        } catch (IllegalResourceException e) {
            throw new BuildException("Failed to configure program: "+e.getMessage(),e);
        } catch (ParseException e) {
            throw new BuildException("Failed to configure program: "+e.getMessage(),e);
        }

        try {
            program.init(engine);
        } catch (Throwable e) {
            throw new BuildException(mainClass.getName()+".init() method reported an exception",e);
        }

        engine.start();

        try {
            program.main(engine);
        } catch (Throwable e) {
            throw new BuildException(mainClass.getName()+".main() method reported an exception",e);
        }
    }
}
