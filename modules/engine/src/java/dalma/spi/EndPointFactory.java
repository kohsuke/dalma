package dalma.spi;

import dalma.EndPoint;

import java.text.ParseException;

/**
 * Creates {@link EndPoint} from a connection string.
 *
 * @author Kohsuke Kawaguchi
 */
public interface EndPointFactory {
    EndPoint create(String endPointName, String endpointURL) throws ParseException;
}
