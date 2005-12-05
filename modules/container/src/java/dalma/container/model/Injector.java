package dalma.container.model;

/**
 * Injects a value into an object.
 *
 * @author Kohsuke Kawaguchi
 */
interface Injector<T,V> {
    String getName();
    void set(T target, V value) throws InjectionException;
}
