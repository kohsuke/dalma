package dalma.endpoints.jms.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * Readable/writable memory buffer.
 *
 * @author Kohsuke Kawaguchi
 */
final class Buffer extends ByteArrayOutputStream {
    public Buffer() {
    }

    public Buffer(byte[] data) {
        super(0);
        super.buf = data;
    }

    public InputStream newInputStream() {
        return new ByteArrayInputStream(buf,0,count);
    }

    public byte[] getBuffer() {
        return buf;
    }
    public int size() {
        return count;
    }
}
