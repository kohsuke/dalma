package dalma.endpoints.jms.impl;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.Serializable;

/**
 * {@link Serializable} {@link TextMessage}.
 *
 * @author Kohsuke Kawaguchi
 */
public class TextMessageImpl extends MessageImpl<TextMessage> implements TextMessage {
    private String text;

    public TextMessageImpl() {
    }

    public TextMessageImpl wrap(TextMessage s) throws JMSException {
        super.wrap(s);
        this.text = s.getText();
        return this;
    }

    public void writeTo(TextMessage d) throws JMSException {
        super.writeTo(d);
        d.setText(text);
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
