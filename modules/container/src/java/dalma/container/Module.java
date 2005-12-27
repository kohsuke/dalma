package dalma.container;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Represents a library module made available to all workflow applications.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Module {
    /**
     * Container that owns this module.
     */
    public final Container owner;

    /**
     * Home directory of this module.
     *
     * <tt>$DALMA_HOME/modules/xxx</tt>.
     */
    public final File dir;

    /**
     * Module manifest.
     */
    public final Manifest manifest;

    public Module(Container owner, File dir) {
        this.owner = owner;
        this.dir = dir;

        this.manifest = new Manifest();

        File manifestFile = new File(dir,"META-INF/MANIFEST.MF");
        if(manifestFile.exists()) {
            // read the manifest file

            try {
                InputStream is = new BufferedInputStream(new FileInputStream(manifestFile));
                try {
                    manifest.read(is);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to read manifest file "+manifestFile,e);
                // this isn't considered a fatal error
            }
        }
    }

    /**
     * Gets the title of this module.
     *
     * <p>
     * The <tt>Specification-Title</tt> (or if it's absent, <tt>Implementation-Title</tt>)
     * of the manifest is used.
     *
     * @return
     *      null if this information is not available.
     */
    public String getTitle() {
        Attributes atts = manifest.getMainAttributes();
        String value;

        value = atts.getValue(Attributes.Name.SPECIFICATION_TITLE);
        if(value!=null) return value;

        value = atts.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
        if(value!=null) return value;

        return null;
    }

    /**
     * Returns the module name.
     *
     * <p>
     * Module name uniquely identifies a {@link Module}. This is the <tt>xxx</tt>
     * portion of <tt>$DALMA_HOME/modules/xxx</tt>
     *
     * @return
     *      always non-null.
     */
    public String getName() {
        return dir.getName();
    }

    private static final Logger logger = Logger.getLogger(Module.class.getName());
}
