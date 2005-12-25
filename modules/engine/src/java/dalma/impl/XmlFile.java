package dalma.impl;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.XppReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * Represents an XML data file that Hudson uses as a data file.
 *
 * @author Kohsuke Kawaguchi
 */
public final class XmlFile {
    private final XStream xs;
    private final File file;

    public XmlFile(File file) {
        this(new XStream(),file);
    }

    public XmlFile(XStream xs, File file) {
        this.xs = xs;
        this.file = file;
    }

    /**
     * Loads the contents of this file into a new object.
     */
    public Object read(ClassLoader cl) throws IOException {
        xs.setClassLoader(cl);
        return read();
    }

    /**
     * Loads the contents of this file into a new object.
     */
    public Object read() throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try {
            return xs.fromXML(r);
        } catch(StreamException e) {
            throw new IOException2(e);
        } finally {
            r.close();
        }
    }

    /**
     * Loads the contents of this file into an existing object.
     */
    public void unmarshal( Object o ) throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
        try {
            xs.unmarshal(new XppReader(r),o);
        } catch (StreamException e) {
            throw new IOException2(e);
        } finally {
            r.close();
        }
    }

    public void write( Object o ) throws IOException {
        AtomicFileWriter w = new AtomicFileWriter(file);
        try {
            w.write("<?xml version='1.0' encoding='UTF-8'?>\n");
            xs.toXML(o,w);
            w.commit();
        } catch(StreamException e) {
            throw new IOException2(e);
        } finally {
            w.close();
        }
    }

    public boolean exists() {
        return file.exists();
    }


    /**
     * Implements the atomic write operation in which
     * either the original file is left intact, or the file is completely rewritten.
     */
    private static final class AtomicFileWriter extends Writer {

        private final Writer core;
        private final File tmpFile;
        private final File destFile;

        public AtomicFileWriter(File f) throws IOException {
            try {
                tmpFile = File.createTempFile("atomic",null,f.getParentFile());
            } catch (IOException e) {
                throw new IOException2("Failed to create a temp dir in "+f.getParentFile(),e);
            }
            destFile = f;
            core = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile),"UTF-8"));
        }

        public void write(int c) throws IOException {
            core.write(c);
        }

        public void write(String str, int off, int len) throws IOException {
            core.write(str,off,len);
        }

        public void write(char cbuf[], int off, int len) throws IOException {
            core.write(cbuf,off,len);
        }

        public void flush() throws IOException {
            core.flush();
        }

        public void close() throws IOException {
            core.close();
        }

        public void commit() throws IOException {
            close();
            if(destFile.exists() && !destFile.delete())
                throw new IOException("Unable to delete "+destFile);
            tmpFile.renameTo(destFile);
        }
    }

    private static final class IOException2 extends IOException  {
        private final Exception cause;

        public IOException2(String msg, Exception cause) {
            super(msg);
            this.cause = cause;
        }

        public IOException2(Exception cause) {
            this(cause.getMessage(),cause);
        }

        public Throwable getCause() {
            return cause;
        }
    }

}
