package dalma;

import dalma.spi.EndPointFactory;

import java.io.Serializable;
import java.io.IOException;
import java.util.Properties;
import java.util.Enumeration;
import java.util.WeakHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.text.ParseException;

/**
 * Represents a gate through which {@link Conversation}s communicate with
 * outer world.
 *
 * TODO: One engine may have more than one instances of the same endPoint
 * (such as using multiple POP3 ports, etc.) How do we configure this?
 * Perhaps by using Spring?
 *
 * TODO: allow JMX to monitor the endPoint status
 *
 * TODO: endPoint setting needs to be persistable. How do we do this?
 * ports tend to have other native resources.
 *
 * TODO: check if the endPoint is really necessary at the engine level.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class EndPoint implements Serializable {
    private final String name;

    protected EndPoint(String name) {
        this.name = name;
        if(name==null)
            throw new IllegalArgumentException();
    }

    /**
     * Gets the unique name that identifies this {@link EndPoint} within an {@link Engine}.
     *
     * @return
     *      always non-null valid object.
     */
    public String getName() {
        return name;
    }

    /**
     * Creates a new {@link EndPoint} from a connection string.
     *
     * @param endPointName
     *      this value will be returned from {@link EndPoint#getName()}.
     *      Must not be null.
     * @param endpointURL
     *      A connection string. Must not be null.
     *
     * @return always non-null valid object
     * @throws ParseException
     *      if there was an error in the connection string.
     */
    public static EndPoint create( String endPointName, String endpointURL ) throws ParseException {
        return Loader.get().createEndPoint(endPointName,endpointURL);
    }

    /**
     * The same as {@link #create(String, String)} except
     * that it uses the given {@link ClassLoader} to locate {@link EndPoint} implementations.
     */
    public static EndPoint create( String endPointName, String endpointURL, ClassLoader cl ) throws ParseException {
        return Loader.get(cl).createEndPoint(endPointName,endpointURL);
    }


    private static final class Loader {
        private static final Logger logger = Logger.getLogger(Loader.class.getName());
        private static final Map<ClassLoader,Loader> loaders
            = Collections.synchronizedMap(new WeakHashMap<ClassLoader, Loader>());

        private final ClassLoader cl;
        private final Properties endPointFactories = new Properties();

        static Loader get() {
            return get(inferDefaultClassLoader());
        }
        static Loader get(ClassLoader cl) {
            Loader loader = loaders.get(cl);
            if(loader==null) {
                loaders.put(cl,loader=new Loader(cl));
            }
            return loader;
        }

        private static ClassLoader inferDefaultClassLoader() {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if(cl==null)        cl = Loader.class.getClassLoader();
            if(cl==null)        cl = ClassLoader.getSystemClassLoader();
            return cl;
        }

        public Loader(ClassLoader cl) {
            this.cl = cl;

            try {
                Enumeration<URL> resources = cl.getResources("META-INF/services/dalma.spi.EndPointFactory");
                while(resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    try {
                        endPointFactories.load(url.openStream());
                    } catch (IOException e) {
                        logger.log(Level.WARNING,"Unable to access "+url,e);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING,"failed to load endpoint factory list",e);
            }
        }

        public synchronized EndPoint createEndPoint(String name, String endpointURL) throws ParseException {
            int idx = endpointURL.indexOf(':');
            if(idx<0)
                throw new ParseException("no scheme in "+endpointURL,-1);
            String scheme = endpointURL.substring(0,idx);

            EndPointFactory epf;
            Object value = endPointFactories.get(scheme);
            if(value==null)
                throw new ParseException("unrecognized scheme "+scheme,0);
            if(value instanceof String) {
                try {
                    Class clazz = cl.loadClass((String)value);
                    Object o = clazz.newInstance();
                    if(!(o instanceof EndPointFactory)) {
                        logger.warning(clazz+" is not an EndPointFactory");
                    }
                    epf = (EndPointFactory)o;
                    endPointFactories.put(scheme,epf);
                } catch (ClassNotFoundException e) {
                    throw new NoClassDefFoundError(e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                } catch (InstantiationException e) {
                    throw new InstantiationError(e.getMessage());
                }
            } else {
                epf = (EndPointFactory)value;
            }

            return epf.create(name, endpointURL);
        }
    }


    private static final long serialVersionUID = 1L;
}
