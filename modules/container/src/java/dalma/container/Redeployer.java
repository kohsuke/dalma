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

    static Redeployer create(WorkflowApplication app) {
        File appdir = new File(app.owner.rootDir, "apps");
        File dar = new File(appdir, app.name + ".dar");
        File exploded = new File(appdir,app.name);

        return new Redeployer(app,dar,exploded);
    }

    private final File dar;
    private final File exploded;
    private final WorkflowApplication app;

    public Redeployer(WorkflowApplication app, File archive, File exploded) {
        super(archive,exploded.exists()?exploded.lastModified():-1);

        this.app = app;
        this.dar = archive;
        this.exploded = exploded;
    }

    protected void onUpdated() {
        app.stop();

        try {
            explode();
            app.start();
        } catch (IOException e) {
            logger.log(Level.SEVERE,"Unable to re-deploy the updated dar file "+dar,e);
            // leave the engine stopped,
            // so that if the user updates the file again, it will restart the engine
        }
    }

    private void explode() throws IOException {
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
    }
}
