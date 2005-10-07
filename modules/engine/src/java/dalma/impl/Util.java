package dalma.impl;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Util {
    /**
     * Deletes the contents of the given directory (but not the directory itself)
     * recursively.
     *
     * @throws IOException
     *      if the operation fails.
     */
    public static void deleteContentsRecursive(File file) throws IOException {
        File[] files = file.listFiles();
        if(files==null)     return; // non existent
        for (File child : files) {
            if (child.isDirectory())
                deleteContentsRecursive(child);
            if (!child.delete())
                throw new IOException("Unable to delete " + child.getPath());
        }
    }

    public static void deleteRecursive(File dir) throws IOException {
        if(!dir.exists())
            return;

        deleteContentsRecursive(dir);
        if(!dir.delete())
            throw new IOException("Unable to delete "+dir);
    }
}
