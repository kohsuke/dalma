package dalma.container.model;

import dalma.Resource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Collections;
import java.text.ParseException;

/**
 * Represents a resource injection model of a class.
 *
 * <p>
 * A {@link Model} consists of zero or more {@link Part}s,
 * which each represents one resource in a model.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Model<T> {

    /**
     * This object represents the resource injection model for this class.
     */
    public final Class<T> clazz;

    public final List<Part> parts;

    /**
     * Represents one resource.
     */
    public final class Part<V> {
        public final String name;
        public final Class<V> type;
        public final Injector<T,V> injector;
        public final Converter<? super V> converter;

        private Part(Injector<T,V> injector) throws IllegalResourceException {
            this.injector = injector;
            this.name = injector.getName();
            this.type = injector.getType();

            converter = Converter.get(type);
            if(converter==null)
                throw new IllegalResourceException(type+" is not supported as a resource type");
        }

        private void inject(T target, Properties prop) throws ParseException, InjectionException {
            String token = prop.getProperty(name);
            Object value = converter.load(name, token);
            if(!type.isInstance(value))
                throw new InjectionException("resource \""+name+"\" wants "+type.getName()+" but found "+value.getClass().getName()+" in configuration");
            injector.set(target,type.cast(value));
        }
    }

    /**
     * Builds a {@link Model} from the given class.
     *
     * @throws IllegalResourceException
     *      if there's incorrect use of {@link Resource}.
     */
    public Model( Class<T> clazz ) throws IllegalResourceException {
        this.clazz = clazz;

        List<Part> parts = new ArrayList<Part>();

        for( Field f : clazz.getFields() ) {
            if(f.getAnnotation(Resource.class)!=null) {
                parts.add(new Part(new FieldInjector(f)));
            }
        }
        for( Method m : clazz.getMethods() ) {
            if(m.getAnnotation(Resource.class)!=null) {
                parts.add(new Part(new MethodInjector(m)));
            }
        }
        // TODO: check for non-public methods that have @Resource and report an error

        this.parts = Collections.unmodifiableList(parts);
    }

    public void inject(T target, Properties prop ) throws InjectionException, ParseException {
        for (Part<?> res : parts)
            res.inject(target,prop);
    }
}
