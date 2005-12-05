package dalma.container.model;

import dalma.Resource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.text.ParseException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Model<T> {
    private final List<Part> parts = new ArrayList<Part>();

    class Part<V> {
        final String name;
        final Class<V> type;
        final Injector<T,V> injector;
        final Converter<? super V> converter;

        public Part(Injector<T,V> injector) throws IllegalResourceException {
            this.injector = injector;
            this.name = injector.getName();
            this.type = injector.getType();

            converter = Converter.get(type);
            if(converter==null)
                throw new IllegalResourceException(type+" is not supported as a resource type");
        }

        void inject(T target, Properties prop) throws ParseException, InjectionException {
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
    Model( Class<T> clazz ) throws IllegalResourceException {
        for( Field f : clazz.getFields() ) {
            if(f.getAnnotation(Resource.class)!=null) {
                addPart(new FieldInjector(f));
            }
        }
        for( Method m : clazz.getMethods() ) {
            if(m.getAnnotation(Resource.class)!=null) {
                addPart(new MethodInjector(m));
            }
        }
        // TODO: check for non-public methods that have @Resource and report an error
    }

    private <V> void addPart(Injector<T,V> injector) throws IllegalResourceException {
        Part<V> part = new Part<V>(injector);
        parts.add(part);
    }

    void inject(T target, Properties prop ) throws InjectionException, ParseException {
        for (Part<?> res : parts)
            res.inject(target,prop);
    }
}
