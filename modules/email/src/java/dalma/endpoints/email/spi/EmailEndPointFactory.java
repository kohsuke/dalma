package dalma.endpoints.email.spi;

import dalma.EndPoint;
import dalma.endpoints.email.EmailEndPoint;
import dalma.endpoints.email.Listener;
import dalma.endpoints.email.MailDirListener;
import dalma.endpoints.email.POP3Listener;
import dalma.endpoints.email.TCPListener;
import dalma.spi.EndPointFactory;
import dalma.spi.UrlQueryParser;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.Properties;

/**
 * {@link EndPointFactory} for the e-mail port.
 *
 * @author Kohsuke Kawaguchi
 */
public class EmailEndPointFactory implements EndPointFactory {
    public EndPoint create(String endPointName, String endpointURL) throws ParseException {
        // split into the SMTP part and listener part
        int idx = endpointURL.indexOf('!');
        if(idx<0)
            throw new ParseException("the smtp protocol string needs to contain '!'",-1);

        try {
            URI smtp = new URI(endpointURL.substring(0,idx));
            String listener = endpointURL.substring(idx + 1);

            Listener listenerObject;

            if(listener.startsWith("pop3://"))
                listenerObject = createPop3Listener(listener,idx+1);
            else
            if(listener.startsWith("imap4://"))
                listenerObject = createImap4Listener(listener,idx+1);
            else
            if(listener.startsWith("maildir://"))
                listenerObject = createMailDirListener(listener,idx+1);
            else
            if(listener.startsWith("tcp://"))
                listenerObject = createTcpListener(listener,idx+1);
            else
                throw new ParseException("Unsupported scheme: "+listener,idx+1);

            if(smtp.getUserInfo()==null)
                throw new ParseException("user name is missing",-1);
            UrlQueryParser smtpQuery = new UrlQueryParser(smtp);

            Properties props = new Properties(System.getProperties());
            smtpQuery.addTo(props);

            // translate known query parameters
            if(smtpQuery.getValue("host")!=null)
                props.put("mail.smtp.host",smtpQuery.getValue("host"));

            return new EmailEndPoint(endPointName,
                new InternetAddress(
                    smtp.getUserInfo()+'@'+smtp.getHost(),
                    smtpQuery.getValue("personal")),
                listenerObject,
                Session.getInstance(props) );

        } catch (URISyntaxException e) {
            throw new ParseException(e.getMessage(),e.getIndex());
        } catch (UnsupportedEncodingException e) {
            throw new ParseException("Unsupported encoding: "+e.getMessage(),-1);
        }
    }

    private Listener createImap4Listener(String listener, int startIndex) throws URISyntaxException, ParseException {
        try {
            URI uri = new URI(listener);
            UrlQueryParser query = new UrlQueryParser(uri);

            String userInfo = uri.getUserInfo();
            if(userInfo==null)
                throw new ParseException("imap4 needs a user name",startIndex);
            int idx = userInfo.indexOf(':');
            if(idx<0)
                throw new ParseException("imap4 needs a password",startIndex);

            return new POP3Listener(
                uri.getHost(),
                userInfo.substring(0,idx),
                userInfo.substring(idx+1),
                query.getValue("interval",3000)
            );
        } catch (URISyntaxException e) {
            throw new URISyntaxException(e.getInput(),e.getReason(),e.getIndex()+startIndex);
        }
    }

    private Listener createPop3Listener(String listener, int startIndex) throws URISyntaxException, ParseException {
        try {
            URI uri = new URI(listener);
            UrlQueryParser query = new UrlQueryParser(uri);

            String userInfo = uri.getUserInfo();
            if(userInfo==null)
                throw new ParseException("pop3 needs a user name",startIndex);
            int idx = userInfo.indexOf(':');
            if(idx<0)
                throw new ParseException("pop3 needs a password",startIndex);

            return new POP3Listener(
                uri.getHost(),
                userInfo.substring(0,idx),
                userInfo.substring(idx+1),
                query.getValue("interval",3000)
            );
        } catch (URISyntaxException e) {
            throw new URISyntaxException(e.getInput(),e.getReason(),e.getIndex()+startIndex);
        }
    }

    private Listener createMailDirListener(String listener, int startIndex) throws ParseException {
        listener = listener.substring("maildir://".length());
        int questionMark = listener.indexOf('?');
        // parse the query parameter
        UrlQueryParser query = new UrlQueryParser(questionMark<0 ? null : listener.substring(questionMark+1));

        // extract the file name portion
        String filePath = questionMark<0 ? listener : listener.substring(0,questionMark);

        File dir = new File(filePath);
        if(!dir.isDirectory())
            throw new ParseException("no such directory exists: "+filePath,-1);

        int interval = query.getValue("interval",3000);

        return new MailDirListener(dir,interval);
    }

    private Listener createTcpListener(String listener, int startIndex) throws URISyntaxException, ParseException {
        try {
            URI uri = new URI(listener);
            if(uri.getPort()==-1)
                throw new ParseException("tcp protocol requres a port number",-1);
            return new TCPListener(new InetSocketAddress(uri.getHost(),uri.getPort()));
        } catch (URISyntaxException e) {
            throw new URISyntaxException(e.getInput(),e.getReason(),e.getIndex()+startIndex);
        }
    }
}
