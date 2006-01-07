package dalma.container.model;

import dalma.EndPoint;
import dalma.Engine;

import java.text.ParseException;
import java.io.File;

/**
 * Converts a persisted configuration value into an object of the appropriate type.
 *
 * TODO: consider adding pluggability.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Converter<T> {

    abstract Class<T> getType();

    abstract T load(Engine engine, String propertyName, String value) throws ParseException;
    //String save(T value);

    /**
     * Finds a converter that handles the given type.
     *
     * @return
     *      null if no suitable converter was found.
     */
    public static <V> Converter<? super V> get(Class<V> type) {
        for (Converter conv : ALL) {
            if(conv.getType().isAssignableFrom(type))
                return conv;
        }
        return null;
    }

    /**
     * All converters.
     */
    public static final Converter[] ALL = {
        new Converter<String>() {
            public Class<String> getType() {
                return String.class;
            }

            public String load(Engine engine, String propertyName, String value) {
                return value;
            }

            public String save(String value) {
                return value;
            }
        },

        new Converter<File>() {
            public Class<File> getType() {
                return File.class;
            }

            public File load(Engine engine, String propertyName, String value) {
                return new File(value);
            }

            public String save(String value) {
                return value;
            }
        },

        new Converter<Boolean>() {
            public Class<Boolean> getType() {
                return Boolean.class;
            }

            public Boolean load(Engine engine, String propertyName, String value) {
                return Boolean.valueOf(value);
            }

            public String save(Boolean value) {
                if (value == null)
                    value = false;
                return Boolean.toString(value);
            }
        },

        new Converter<Integer>() {
            public Class<Integer> getType() {
                return Integer.class;
            }

            public Integer load(Engine engine, String propertyName, String value) throws ParseException {
                if (value == null) return 0;
                try {
                    return Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    // not a string
                    throw new ParseException(value,-1);
                }
            }

            public String save(Integer value) {
                if (value == null) return null;
                return Integer.toString(value);
            }
        },

        new Converter<EndPoint>() {
            public Class<EndPoint> getType() {
                return EndPoint.class;
            }

            public EndPoint load(Engine engine, String propertyName, String value) throws ParseException {
                return engine.addEndPoint(propertyName,value);
            }
        }
    };
}
