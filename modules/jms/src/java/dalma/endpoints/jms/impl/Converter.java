package dalma.endpoints.jms.impl;

import javax.jms.MessageFormatException;
import javax.jms.JMSException;
import java.util.Map;
import java.util.HashMap;

/**
 * Converts the type of a value according to the following table in JMS:
 *
 * <pre>
 * |        | boolean byte short char int long float double String byte[]
 * |----------------------------------------------------------------------
 * |boolean |    X                                            X
 * |byte    |          X     X         X   X                  X
 * |short   |                X         X   X                  X
 * |char    |                     X                           X
 * |int     |                          X   X                  X
 * |long    |                              X                  X
 * |float   |                                    X     X      X
 * |double  |                                          X      X
 * |String  |    X     X     X         X   X     X     X      X
 * |byte[]  |                                                        X
 * |----------------------------------------------------------------------
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Converter<T> {
    private final Class<T> sourceType;

    private Converter(Class<T> sourceType) {
        this.sourceType = sourceType;
        convs.put(sourceType,this); // register
    }

//
// conversion methods
//
    boolean asBoolean(T source) throws JMSException {
        throw error("boolean");
    }

    byte asByte(T source) throws JMSException {
        throw error("byte");
    }
    short asShort(T source) throws JMSException {
        throw error("short");
    }
    char asChar(T source) throws JMSException {
        throw error("char");
    }
    int asInt(T source) throws JMSException {
        throw error("int");
    }
    long asLong(T source) throws JMSException {
        throw error("long");
    }
    float asFloat(T source) throws JMSException {
        throw error("float");
    }
    double asDouble(T source) throws JMSException {
        throw error("double");
    }
    String asString(T source) throws JMSException {
//        throw error("string");
        return source.toString();
    }
    byte[] asByteArray(T source) throws JMSException {
        throw error("byte[]");
    }


    // table
    private static Map<Class,Converter> convs = new HashMap<Class, Converter>();

    /**
     * Gets the converter for the given class.
     */
    public static <T> Converter<T> get(Class<T> type) {
        return (Converter<T>)convs.get(type);
    }

    public static Converter get(Object o) {
        if(o==null) return NULL;
        return convs.get(o.getClass());
    }





//
// converters
//
    static final Converter<Boolean> BOOLEAN = new Converter<Boolean>(Boolean.class) {
        boolean asBoolean(Boolean source) {
            return source;
        }
    };

    static final Converter<Byte> BYTE = new Converter<Byte>(Byte.class) {
        byte asByte(Byte source) {
            return source;
        }

        short asShort(Byte source) {
            return source;
        }

        int asInt(Byte source) {
            return source;
        }

        long asLong(Byte source) {
            return source;
        }
    };

    static final Converter<Short> SHORT = new Converter<Short>(Short.class) {
        short asShort(Short source) {
            return source;
        }

        int asInt(Short source) {
            return source;
        }

        long asLong(Short source) {
            return source;
        }
    };

    static final Converter<Character> CHARACTER = new Converter<Character>(Character.class) {
        char asChar(Character source) {
            return source;
        }
    };

    static final Converter<Integer> INTEGER = new Converter<Integer>(Integer.class) {
        int asInt(Integer source) {
            return source;
        }

        long asLong(Integer source) {
            return source;
        }
    };

    static final Converter<Long> LONG = new Converter<Long>(Long.class) {
        long asLong(Long source) {
            return source;
        }
    };

    static final Converter<Float> FLOAT = new Converter<Float>(Float.class) {
        float asFloat(Float source) {
            return source;
        }

        double asDouble(Float source) {
            return source;
        }
    };

    static final Converter<Double> DOUBLE = new Converter<Double>(Double.class) {
        double asDouble(Double source) {
            return source;
        }
    };

    static final Converter<String> STRING = new Converter<String>(String.class) {
        boolean asBoolean(String source) {
            return Boolean.parseBoolean(source);
        }

        byte asByte(String source) {
            return Byte.parseByte(source);
        }

        short asShort(String source) {
            return Short.parseShort(source);
        }

        int asInt(String source) {
            return Integer.parseInt(source);
        }

        long asLong(String source) {
            return Long.parseLong(source);
        }

        float asFloat(String source) {
            return Float.parseFloat(source);
        }

        double asDouble(String source) {
            return Double.parseDouble(source);
        }
    };

    static final Converter<byte[]> BYTE_ARRAY = new Converter<byte[]>(byte[].class) {
        String asString(byte[] source) throws JMSException {
            throw error("byte[]");
        }

        byte[] asByteArray(byte[] source) {
            return source;
        }
    };

    static final Converter<?> NULL = new Converter<Object>(Object.class) {
        boolean asBoolean(Object source) {
            return false;
        }

        byte asByte(Object source) {
            return 0;
        }

        short asShort(Object source) {
            return 0;
        }

        char asChar(Object source) {
            return 0;
        }

        int asInt(Object source) {
            return 0;
        }

        long asLong(Object source) {
            return 0;
        }

        float asFloat(Object source) {
            return 0;
        }

        double asDouble(Object source) {
            return 0;
        }

        String asString(Object source) {
            return null;
        }

        byte[] asByteArray(Object source) {
            return null;
        }
    };

//
// impl
//
    protected final JMSException error(String name) throws MessageFormatException {
        throw new MessageFormatException(sourceType.getName()+" cannot be converted to "+name);
    }
}
