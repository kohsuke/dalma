package dalma.container;

import dalma.Executor;

import java.util.Set;
import java.util.HashSet;
import java.io.File;

/**
 * Roof ot the dalma container.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Container {
    private final Set<WorkflowApplication> apps = new HashSet<WorkflowApplication>();

    /**
     * Root directory of the dalma installation. The value of DALMA_HOME.
     */
    public final File rootDir;

    /**
     * {@link Executor} that is shared by all {@link WorkflowApplication}s.
     */
    protected final Executor executor;

    public Container(File root, Executor executor) {
        this.rootDir = root;
        this.executor = executor;
    }
}
