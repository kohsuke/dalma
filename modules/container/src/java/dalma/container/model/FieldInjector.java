package dalma.container.model;

import java.lang.reflect.Field;

/**
 * {@link Injector} that sets a field.
 * @author Kohsuke Kawaguchi
 */
final class FieldInjector<T,V> implements Injector<T,V> {
    private final Field f;

    public FieldInjector(Field f) {
        this.f = f;
    }

    public String getName() {
        return f.getName();
    }

    public void set(T target, V value) throws InjectionException {
        try {
            f.set(target,value);
        } catch (IllegalAccessException e) {
            throw new InjectionException(e);
        }
    }
}
