package dalma.container.model;

import dalma.Resource;
import dalma.Engine;

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
    public final Class<? extends T> clazz;

    /**
     * List of resoures that constitute a model.
     */
    public final List<Part> parts;

    /**
     * Represents one resource.
     */
    public final class Part<V> {
        public final String name;
        public final Class<V> type;
        public final Injector<T,V> injector;
        public final Converter<? super V> converter;

        public final String description;
        public final boolean optional;

        private Part(Injector<T,V> injector, Resource a) throws IllegalResourceException {
            this.injector = injector;
            this.name = injector.getName();
            this.type = injector.getType();

            this.description = a.description();
            this.optional = a.optional();

            converter = Converter.get(type);
            if(converter==null)
                throw new IllegalResourceException(type+" is not supported as a resource type");
        }

        private void inject(Engine engine, T target, Properties prop) throws ParseException, InjectionException {
            String token = prop.getProperty(name);
            if(token==null && !optional)
                throw new InjectionException("resource \""+name+"\" must be configured");
            Object value = converter.load(engine, name, token);
            if(!type.isInstance(value))
                throw new InjectionException("resource \""+name+"\" wants "+type.getName()+" but found "+(value==null?"null":value.getClass().getName())+" in configuration");
            injector.set(target,type.cast(value));
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean checkConfiguration(Properties props) {
            return optional || props.getProperty(name)!=null;
        }
    }

    /**
     * Builds a {@link Model} from the given class.
     *
     * @throws IllegalResourceException
     *      if there's incorrect use of {@link Resource}.
     */
    public Model( Class<? extends T> clazz ) throws IllegalResourceException {
        this.clazz = clazz;

        List<Part> parts = new ArrayList<Part>();

        for( Field f : clazz.getFields() ) {
            Resource a = f.getAnnotation(Resource.class);
            if(a !=null) {
                parts.add(new Part(new FieldInjector(f),a));
            }
        }
        for( Method m : clazz.getMethods() ) {
            Resource a = m.getAnnotation(Resource.class);
            if(a !=null) {
                parts.add(new Part(new MethodInjector(m),a));
            }
        }
        // TODO: check for non-public methods that have @Resource and report an error

        this.parts = Collections.unmodifiableList(parts);
    }

    public void inject(Engine engine, T target, Properties prop ) throws InjectionException, ParseException {
        for (Part<?> res : parts)
            res.inject(engine,target,prop);
    }

    /**
     * Checks if the given properties have enough configuration
     * for all mandatory parameters.
     */
    public boolean checkConfiguration(Properties props) {
        for (Part<?> res : parts)
            if(!res.checkConfiguration(props))
                return false;
        return true;
    }

    public List<Part> getParts() {
        return parts;
    }
}
