package dalma.endpoints.jms.impl;

import dalma.spi.EndPointFactory;
import dalma.spi.UrlQueryParser;
import dalma.EndPoint;
import dalma.endpoints.jms.JMSEndPoint;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.JMSException;
import java.text.ParseException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.HashMap;

/**
 * {@link EndPointFactory} for the JMS endpoint.
 *
 * URL structure is:
 *
 * <pre>
 * jms://?factory=jndi://jms/QueueConnectionFactory&in=jndi://jms/request-queue&out=jndi://jms/response-topics
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
public class JMSEndPointFactory implements EndPointFactory {
    public EndPoint create(String endPointName, String endpointURL) throws ParseException {
        try {
            Context jndic = new InitialContext();

            UrlQueryParser qp = new UrlQueryParser(new URI(endpointURL));

            ConnectionFactory cf = getJndiValue(ConnectionFactory.class, jndic,qp,"factory");

            Destination in = getJndiValue(Destination.class, jndic,qp,"in");

            Destination out = null;
            if(qp.getValue("out")!=null)
                out = getJndiValue(Destination.class, jndic,qp,"out");

            String user = qp.getValue("username");
            String pass = qp.getValue("password");
            Connection con;
            if(user==null || pass==null)
                con = cf.createConnection();
            else
                con = cf.createConnection(user,pass);

            String modeStr = qp.getValue("mode","AUTO").toUpperCase();
            if(!MODE_TABLE.containsKey(modeStr))
                throw new ParseException("Unknown mode: "+modeStr,-1);
            int mode = MODE_TABLE.get(modeStr);

            return new JMSEndPoint(endPointName,
                con.createSession(qp.has("transacted"),mode),
                out,in);
        } catch (URISyntaxException e) {
            ParseException pe = new ParseException(e.getMessage(), e.getIndex());
            pe.initCause(e);
            throw pe;
        } catch (NamingException e) {
            ParseException pe = new ParseException(e.getMessage(),-1);
            pe.initCause(e);
            throw pe;
        } catch (JMSException e) {
            ParseException pe = new ParseException(e.getMessage(),-1);
            pe.initCause(e);
            throw pe;
        }
    }

    private <T> T getJndiValue(Class<T> type, Context context, UrlQueryParser qp, String paramName) throws ParseException, NamingException {
        String jndiPath = qp.getValue(paramName);
        if(jndiPath==null)
            throw new ParseException("JMS endpoint URL is missing the '"+paramName+"' parameter",-1);

        Object value = context.lookup(jndiPath);

        if(!type.isInstance(value))
            throw new ParseException("JNDI name "+jndiPath+" is an object of type "+value.getClass().getName()+
                "\n"+type.getName()+" expected",-1);

        return type.cast(value);
    }

    private static final Map<String,Integer> MODE_TABLE = new HashMap<String, Integer>();

    static {
        MODE_TABLE.put("AUTO_ACKNOWLEDGE", Session.AUTO_ACKNOWLEDGE);
        MODE_TABLE.put("CLIENT_ACKNOWLEDGE", Session.CLIENT_ACKNOWLEDGE);
        MODE_TABLE.put("DUPS_OK_ACKNOWLEDGE", Session.DUPS_OK_ACKNOWLEDGE);
        MODE_TABLE.put("AUTO", Session.AUTO_ACKNOWLEDGE);
        MODE_TABLE.put("CLIENT", Session.CLIENT_ACKNOWLEDGE);
        MODE_TABLE.put("DUPS_OK", Session.DUPS_OK_ACKNOWLEDGE);
    }
}
