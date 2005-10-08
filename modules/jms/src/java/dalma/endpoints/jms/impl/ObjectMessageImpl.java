package dalma.endpoints.jms.impl;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.io.Serializable;

/**
 * {@link Serializable} {@link ObjectMessage}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ObjectMessageImpl extends MessageImpl<ObjectMessage> implements ObjectMessage {
    private Serializable object;

    public ObjectMessageImpl() {
    }

    public ObjectMessageImpl wrap(ObjectMessage s) throws JMSException {
        super.wrap(s);
        object = s.getObject();
        return this;
    }

    public void writeTo(ObjectMessage d) throws JMSException {
        super.writeTo(d);
        d.setObject(object);
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
