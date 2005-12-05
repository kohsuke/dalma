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
    private final List<Part> resources = new ArrayList<Part>();

    class Part<V> {
        String name;
        Class<V> type;
        Injector<T,V> injector;
        Converter<V> converter;

        void inject(T target, Properties prop) throws ParseException, InjectionException {
            String token = prop.getProperty(name);
            V value = converter.load(name, token);
            injector.set(target,value);
        }
    }

    Model( Class<T> clazz ) {
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
    }

    private <V> void addPart(Injector<T,V> injector) {
        Part<V> part = new Part<V>();
        // TODO
    }

    void inject(T target, Properties prop ) throws InjectionException, ParseException {
        for (Part<?> res : resources)
            res.inject(target,prop);
    }
}
