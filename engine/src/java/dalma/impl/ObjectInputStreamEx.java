package dalma.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * {@link ObjectInputStream} that uses a specific class loader.
 *
 * @author Kohsuke Kawaguchi
 */
final class ObjectInputStreamEx extends ObjectInputStream {

    private final ClassLoader classLoader;

    public ObjectInputStreamEx(InputStream in, ClassLoader classLoader) throws IOException {
        super(in);
        assert classLoader!=null;
        this.classLoader = classLoader;
    }

    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();
        return classLoader.loadClass(name);
    }
}
