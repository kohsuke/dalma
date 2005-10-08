package dalma.endpoints.jms.impl;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.Serializable;

/**
 * {@link Serializable} {@link TextMessage}.
 *
 * @author Kohsuke Kawaguchi
 */
public class TextMessageImpl extends MessageImpl implements TextMessage {
    private String text;

    public TextMessageImpl() {
    }

    public TextMessageImpl(TextMessage s) throws JMSException {
        super(s);
        this.text = s.getText();
    }

    public void clearBody() throws JMSException {
        text = null;
    }

    public void setText(String string) throws JMSException {
        this.text = string;
    }

    public String getText() throws JMSException {
        return text;
    }

    private static final long serialVersionUID = 1L;
}
