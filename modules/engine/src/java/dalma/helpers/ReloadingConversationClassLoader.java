package dalma.helpers;

import org.apache.commons.javaflow.bytecode.transformation.ResourceTransformer;
import org.apache.commons.javaflow.bytecode.transformation.bcel.BcelClassTransformer;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.IOException;

/**
 * {@link ClassLoader} that loads the same classes as its parent
 * {@code ClassLoader} but with necessary bytecode enhancement for
 * running conversations.
 *
 * <p>
 * This {@code ClassLoader} is useful when all the application classes
 * are available in a single {@code ClassLoader} (which is typically
 * the application classloader), but yet you still want to selectively
 * instrument classes at runtime.
 *
 * @author Kohsuke Kawaguchi
 */
public class ReloadingConversationClassLoader extends ClassLoader {
    private ResourceTransformer transformer = new BcelClassTransformer();

    private final String prefix;

    /**
     * Creates a new instance.
     *
     * @param parent
     *      parent class loader. Can be null, in which case it delegates
     *      to the application class loader.
     * @param prefix
     *      prefix of the classes that will be instrumented by this class loader.
     *      for example, if this parameter is "org.acme.foo.", then classes like
     *      "org.acme.foo.Abc" or "org.acme.foo.bar.Zot" will be instrumented,
     *      but not "org.acme.Joe" or "org.acme.foobar.Zot".
     */
    public ReloadingConversationClassLoader(ClassLoader parent, String prefix) {
        super(parent);
        this.prefix = prefix;
        if(prefix==null)
            throw new IllegalArgumentException();
    }

    private boolean shouldBeRewritten(String s) {
        return s.startsWith(prefix);
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
