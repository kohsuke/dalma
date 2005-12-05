package dalma.container.model;

/**
 * Injects a value as a property into an object.
 *
 * @author Kohsuke Kawaguchi
 */
interface Injector<T,V> {
    /**
     * Name of the property that this {@link Injector} represents.
     */
    String getName();

    /**
     * Gets the type of the property.
     */
    Class<V> getType();

    /**
     * Injects a value.
     */
    void set(T target, V value) throws InjectionException;
}
