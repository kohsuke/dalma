package dalma.endpoints.irc;

import dalma.spi.EndPointFactory;
import dalma.EndPoint;

import java.text.ParseException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Kohsuke Kawaguchi
 */
public class IRCEndPointFactory implements EndPointFactory {
    // irc://nick@server:port/
    public EndPoint create(String endPointName, String endpointURL) throws ParseException {
        try {
            URI uri = new URI(endpointURL);

            int port = uri.getPort();
            if(port==-1)    port=6667;

            if(uri.getUserInfo()==null)
                throw new ParseException("irc:// URL needs to have \"<username>@\" portion",0);

            return new IRCEndPoint(endPointName, uri.getHost(), port, uri.getUserInfo());
        } catch (URISyntaxException e) {
            ParseException pe = new ParseException(e.getMessage(), 0);
            pe.initCause(e);
            throw pe;
        }
    }
}
