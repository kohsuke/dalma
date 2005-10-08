package dalma.endpoints.jms.impl;

import javax.jms.ObjectMessage;
import javax.jms.Message;
import javax.jms.JMSException;
import java.io.Serializable;

/**
 * {@link Serializable} {@link ObjectMessage}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ObjectMessageImpl extends MessageImpl implements ObjectMessage {
    private Serializable object;

    public ObjectMessageImpl() {
    }

    public ObjectMessageImpl(ObjectMessage s) throws JMSException {
        super(s);
        object = s.getObject();
    }

    public void clearBody() throws JMSException {
        object = null;
    }

    public void setObject(Serializable object) throws JMSException {
        this.object = object;
    }

    public Serializable getObject() throws JMSException {
        return object;
    }

    private static final long serialVersionUID = 1L;
}
