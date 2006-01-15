package dalma.maven;

import dalma.container.Container;

import java.io.File;
import java.io.IOException;

/**
 * Runs a workflow application in a stand-alone mode.
 *
 * @author Kohsuke Kawaguchi
 */
public class Runner {
    private File workDir;

    /**
     * Sets the work directory.
     */
    public void setWorkDir(File workDir) {
        this.workDir = workDir;
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

    public void run() {
        // Container con = Container.create(getWorkDir());
        System.out.println(getWorkDir());

    }
}
