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
class Redeployer extends FileChangeMonitor {

    private static final Logger logger = Logger.getLogger(Redeployer.class.getName());

    private final Container container;

    public Redeployer(Container container) {
        super(new File(container.rootDir, "apps"));
        this.container = container;
    }

    @Override
    protected void onAdded(File file) {
        if(isDar(file))
            explode(file);
        if(file.isDirectory()) {
            logger.info("New application '"+file.getName()+"' detected. Deploying.");
            try {
                container.deploy(file);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unable to deploy", e );
            }
        }
    }

    @Override
    protected void onUpdated(File file) {
        if(isDar(file))
            explode(file);
        if(file.isDirectory()) {
            try {
                WorkflowApplication wa = container.getApplication(file.getName());
                if(wa!=null) {
                    logger.info("Changed detected in application '"+wa.getName()+"'. Re-deploying.");
                    wa.stop();
                    wa.start();
                }
            } catch (IOException e) {
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

    /**
     * Extracts the given dar file.
     */
    private void explode(File dar) {
        try {
            String name = dar.getName();
            File exploded = new File(dar.getParentFile(),name.substring(0,name.length()-4));
            if(exploded.exists())
                Util.deleteRecursive(exploded);

            byte[] buf = new byte[1024];    // buffer

            JarFile archive = new JarFile(dar);
            Enumeration<JarEntry> e = archive.entries();
            while(e.hasMoreElements()) {
                JarEntry j = e.nextElement();
                File dst = new File(exploded, j.getName());
                dst.getParentFile().mkdirs();

                InputStream in = archive.getInputStream(j);
                FileOutputStream out = new FileOutputStream(dst);
                try {
                    while(true) {
                        int sz = in.read(buf);
                        if(sz<0)
                            break;
                        out.write(buf,0,sz);
                    }
                } finally {
                    in.close();
                    out.close();
                }
            }

            archive.close();
        } catch (IOException x) {
            logger.log(Level.SEVERE,"Unable to extract the dar file "+dar,x);
            // leave the engine stopped,
            // so that if the user updates the file again, it will restart the engine
        }
    }
}
