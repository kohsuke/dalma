package dalma.container;

import org.apache.bcel.util.Repository;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.classfile.JavaClass;

/**
 * {@link Repository} that delegates to another.
 * @author Kohsuke Kawaguchi
 */
public class DelegatingRepository implements Repository {
    private Repository r;

    public void setRepository(Repository r) {
        this.r = r;
    }

    public void storeClass(JavaClass clazz) {
        r.storeClass(clazz);
    }

    public void removeClass(JavaClass clazz) {
        r.removeClass(clazz);
    }

    public JavaClass findClass(String className) {
        return r.findClass(className);
    }

    public JavaClass loadClass(String className) throws ClassNotFoundException {
        return r.loadClass(className);
    }

    public JavaClass loadClass(Class clazz) throws ClassNotFoundException {
        return r.loadClass(clazz);
    }

    public void clear() {
        r.clear();
    }

    public ClassPath getClassPath() {
        return r.getClassPath();
    }
}
