package dalma.container.model;

/**
 * Injects a value as a property into an object.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Injector<T,V> {
    /**
     * Name of the property that this {@link Injector} represents.
     */
    public abstract String getName();

    /**
     * Gets the type of the property.
     */
    public final Class<V> getType() {
        Class t = _getType();
        if(t==int.class)
            t = Integer.class;
        return t;
    }

    protected abstract Class<V> _getType();

    /**
     * Injects a value.
     */
    public abstract void set(T target, V value) throws InjectionException;
}
