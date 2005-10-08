package dalma.endpoints.jms.impl;

import dalma.spi.EndPointFactory;
import dalma.EndPoint;

import java.text.ParseException;

/**
 * {@link EndPointFactory} for the JMS endpoint.
 *
 * @author Kohsuke Kawaguchi
 */
public class JMSEndPointFactory implements EndPointFactory {
    public EndPoint create(String endPointName, String connectionString) throws ParseException {
        throw new UnsupportedOperationException();
    }
}
