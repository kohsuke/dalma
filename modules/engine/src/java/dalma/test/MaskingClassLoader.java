package dalma.test;

/**
 * {@link ClassLoader} that masks <tt>test.*</tt> classes visible
 * in the parent class loader.
 *
 * @author Kohsuke Kawaguchi
 */
public class MaskingClassLoader extends ClassLoader {
    public MaskingClassLoader(ClassLoader parent) {
        super(parent);
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(name.startsWith("test."))
            throw new ClassNotFoundException(name+" is masked");
        return super.loadClass(name,resolve);
    }
}
