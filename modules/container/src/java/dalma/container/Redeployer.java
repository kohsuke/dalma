package dalma.container;

import dalma.impl.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Monitors a ".dar" file for its update and redeploy
 * if necessary.
 *
 * @author Kohsuke Kawaguchi
 */
final class Redeployer extends FileChangeMonitor {

    private static final Logger logger = Logger.getLogger(Redeployer.class.getName());

    private final Container container;

    Redeployer(Container container) {
        super(container.appsDir);
        this.container = container;
    }

    @Override
    protected void onAdded(File file) {
        if(isDar(file))
            Container.explode(file);
        if(file.isDirectory()) {
            logger.info("New application '"+file.getName()+"' detected. Deploying.");
            try {
                container.deploy(file);
            } catch (FailedOperationException e) {
                logger.log(Level.SEVERE, "Unable to deploy", e );
            }
        }
    }

    @Override
    protected void onUpdated(File file) {
        if(isDar(file))
            Container.explode(file);
        if(file.isDirectory()) {
            try {
                WorkflowApplication wa = container.getApplication(file.getName());
                if(wa!=null) {
                    logger.info("Changed detected in application '"+wa.getName()+"'. Re-deploying.");
                    wa.unload();
                    wa.start();
                }
            } catch (FailedOperationException e) {
                logger.log(Level.SEVERE, "Unable to redeploy", e );
            }
        }
    }

    protected void onDeleted(File file) {
        WorkflowApplication wa = container.getApplication(file.getName());
        if(wa!=null) {
            logger.info("Application '"+file.getName()+"' is removed. Undeploying.");
            wa.remove();
        }
    }

    private static boolean isDar(File f) {
        return f.getName().endsWith(".dar");
    }

}
