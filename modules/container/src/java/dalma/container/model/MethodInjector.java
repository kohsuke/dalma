package dalma.container.model;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Kohsuke Kawaguchi
 */
final class MethodInjector<T,V> implements Injector<T,V> {
    private final Method m;

    public MethodInjector(Method m) throws IllegalResourceException {
        this.m = m;
        if(m.getParameterTypes().length==0)
            throw new IllegalResourceException(m.getName()+" has @Resource but takes no parameter");
        if(m.getParameterTypes().length>1)
            throw new IllegalResourceException(m.getName()+" has @Resource but takes more than one parameters");
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

    public Class<V> getType() {
        return (Class<V>) m.getParameterTypes()[0];
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
