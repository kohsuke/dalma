package dalma.test;

import org.apache.commons.io.IOUtils;
import org.apache.commons.javaflow.bytecode.transformation.ResourceTransformer;
import org.apache.commons.javaflow.bytecode.transformation.bcel.BcelClassTransformer;

import java.io.IOException;
import java.io.InputStream;

public final class TestClassLoader extends ClassLoader {
    private ResourceTransformer transformer = new BcelClassTransformer();

    public TestClassLoader(ClassLoader parent) {
        super(parent);
    }

    private boolean shouldBeRewritten(String s) {
        return s.startsWith("test.");
    }

    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {

        Class c = findLoadedClass(name);

        if(c==null && shouldBeRewritten(name)) {
            InputStream is = super.getResourceAsStream(name.replace('.', '/') + ".class");
            if(is!=null) {
                try {
                    byte[] buf = IOUtils.toByteArray(is);
                    buf = transformer.transform(buf);
                    c = defineClass(name, buf, 0, buf.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException("failed to read the class file", e);
                }
            }
        }

        if(c==null) {
            // delegate
            final ClassLoader parent = getParent();
            if (parent != null)
                c = parent.loadClass(name);
            else
                throw new ClassNotFoundException(name);
        }

        if (resolve)
            resolveClass(c);
        return c;
    }
}
