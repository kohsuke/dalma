package dalma.container.model;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Kohsuke Kawaguchi
 */
final class MethodInjector<T,V> implements Injector<T,V> {
    private final Method m;

    public MethodInjector(Method m) {
        this.m = m;
    }

    public String getName() {
        String name = m.getName();
        // proper lower casing
        if(name.startsWith("set") && name.length()>3) {
            return Character.toLowerCase(name.charAt(3))+name.substring(4);
        } else {
            return name;
        }
    }

    public void set(T target, V value) throws InjectionException {
        try {
            m.invoke(target,value);
        } catch (IllegalAccessException e) {
            throw new InjectionException(e);
        } catch (InvocationTargetException e) {
            throw new InjectionException(e);
        }
    }
}
