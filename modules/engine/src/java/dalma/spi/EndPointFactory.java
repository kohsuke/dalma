package dalma.spi;

import dalma.EndPoint;

import java.text.ParseException;

/**
 * Creates {@link EndPoint} from a connection string.
 *
 * @author Kohsuke Kawaguchi
 */
public interface EndPointFactory {
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
    EndPoint create(String endPointName, String endpointURL) throws ParseException;
}
