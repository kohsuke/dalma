package dalma.container.model;

import dalma.EndPoint;

import java.text.ParseException;

/**
 * Converts a persisted configuration value into an object of the appropriate type.
 *
 * @author Kohsuke Kawaguchi
 */
interface Converter<T> {

    Class<T> getType();

    T load(String propertyName, String value) throws ParseException;
    //String save(T value);

    public static final Converter<String> STRING = new Converter<String>() {
        public Class<String> getType() {
            return String.class;
        }

        public String load(String propertyName, String value) {
            return value;
        }

        public String save(String value) {
            return value;
        }
    };

    public static final Converter<Boolean> BOOLEAN = new Converter<Boolean>() {
        public Class<Boolean> getType() {
            return Boolean.class;
        }

        public Boolean load(String propertyName, String value) {
            return Boolean.valueOf(value);
        }

        public String save(Boolean value) {
            if(value==null)
                value = false;
            return Boolean.toString(value);
        }
    };

    public static final Converter<Integer> INTEGER = new Converter<Integer>() {
        public Class<Integer> getType() {
            return Integer.class;
        }

        public Integer load(String propertyName, String value) {
            if(value==null) return 0;
            return Integer.valueOf(value);
        }

        public String save(Integer value) {
            if(value==null) return null;
            return Integer.toString(value);
        }
    };

    public static final Converter<EndPoint> ENDPOINT = new Converter<EndPoint>() {
        public Class<EndPoint> getType() {
            return EndPoint.class;
        }

        public EndPoint load(String propertyName, String value) throws ParseException {
            return EndPoint.create(propertyName,value);
        }
    };
}
