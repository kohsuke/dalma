package dalma.endpoints.jms.impl;

import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import javax.jms.StreamMessage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * @ThirdParty this class contains code released under ASL.
 * @author Kohsuke Kawaguchi
 */
public class StreamMessageImpl extends MessageImpl implements StreamMessage {
    /**
     * A {@link StreamMessage} can be in two modes.
     * read-only/write-only
     */
    boolean readOnlyMode = false;

    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private Buffer bytesOut = new Buffer();
    private int bytesToRead = -1;

    public StreamMessageImpl() {
    }

    public StreamMessageImpl(StreamMessage s) throws JMSException {
        super(s);
        try {
            while(true) {
                writeObject(s.readObject());
            }
        } catch (MessageEOFException e) {
            // reached EOF
            reset();
        }
    }

    public void clearBody() throws JMSException {
        dataOut = null;
        dataIn = null;
        bytesOut = new Buffer();
        readOnlyMode = false;
    }

    /**
     * Reads a <code>boolean</code> from the stream message.
     *
     * @return the <code>boolean</code> value read
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     */

    public boolean readBoolean() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(10);
            int type = this.dataIn.read();
            if (type == BOOLEAN) {
                return this.dataIn.readBoolean();
            }
            if (type == STRING) {
                return Boolean.valueOf(this.dataIn.readUTF());
            }
            if (type == NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to boolean.");
            }
            else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a boolean type");
            }
        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Reads a <code>byte</code> value from the stream message.
     *
     * @return the next byte from the stream message as a 8-bit
     *         <code>byte</code>
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     */

    public byte readByte() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(10);
            int type = this.dataIn.read();
            if (type == BYTE) {
                return this.dataIn.readByte();
            }
            if (type == STRING) {
                return Byte.valueOf(this.dataIn.readUTF());
            }
            if (type == NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to byte.");
            }
            else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a byte type");
            }
        }
        catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed");
                jmsEx.setLinkedException(ioe);
            }
            throw mfe;

        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Reads a 16-bit integer from the stream message.
     *
     * @return a 16-bit integer from the stream message
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     */

    public short readShort() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(17);
            int type = this.dataIn.read();
            if (type == SHORT) {
                return this.dataIn.readShort();
            }
            if (type == BYTE) {
                return this.dataIn.readByte();
            }
            if (type == STRING) {
                return Short.valueOf(this.dataIn.readUTF());
            }
            if (type == NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to short.");
            }
            else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a short type");
            }
        }
        catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed");
                jmsEx.setLinkedException(ioe);
            }
            throw mfe;

        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }

    }


    /**
     * Reads a Unicode character value from the stream message.
     *
     * @return a Unicode character from the stream message
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     */

    public char readChar() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(17);
            int type = this.dataIn.read();
            if (type == CHAR) {
                return this.dataIn.readChar();
            }
            if (type == NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to char.");
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a char type");
            }
        }
        catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed");
                jmsEx.setLinkedException(ioe);
            }
            throw mfe;

        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Reads a 32-bit integer from the stream message.
     *
     * @return a 32-bit integer value from the stream message, interpreted
     *         as an <code>int</code>
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     */

    public int readInt() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(33);
            int type = this.dataIn.read();
            if (type == INT) {
                return this.dataIn.readInt();
            }
            if (type == SHORT) {
                return this.dataIn.readShort();
            }
            if (type == BYTE) {
                return this.dataIn.readByte();
            }
            if (type == STRING) {
                return Integer.valueOf(this.dataIn.readUTF());
            }
            if (type == NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to int.");
            }
            else {
                this.dataIn.reset();
                throw new MessageFormatException(" not an int type");
            }
        }
        catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed");
                jmsEx.setLinkedException(ioe);
            }
            throw mfe;

        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Reads a 64-bit integer from the stream message.
     *
     * @return a 64-bit integer value from the stream message, interpreted as
     *         a <code>long</code>
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     */

    public long readLong() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(65);
            int type = this.dataIn.read();
            if (type == LONG) {
                return this.dataIn.readLong();
            }
            if (type == INT) {
                return this.dataIn.readInt();
            }
            if (type == SHORT) {
                return this.dataIn.readShort();
            }
            if (type == BYTE) {
                return this.dataIn.readByte();
            }
            if (type == STRING) {
                return Long.valueOf(this.dataIn.readUTF());
            }
            if (type == NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to long.");
            }
            else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a long type");
            }
        }
        catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed");
                jmsEx.setLinkedException(ioe);
            }
            throw mfe;

        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Reads a <code>float</code> from the stream message.
     *
     * @return a <code>float</code> value from the stream message
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     */

    public float readFloat() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(33);
            int type = this.dataIn.read();
            if (type == FLOAT) {
                return this.dataIn.readFloat();
            }
            if (type == STRING) {
                return Float.valueOf(this.dataIn.readUTF());
            }
            if (type == NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to float.");
            }
            else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a float type");
            }
        }
        catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed");
                jmsEx.setLinkedException(ioe);
            }
            throw mfe;

        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Reads a <code>double</code> from the stream message.
     *
     * @return a <code>double</code> value from the stream message
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     */

    public double readDouble() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(65);
            int type = this.dataIn.read();
            if (type == DOUBLE) {
                return this.dataIn.readDouble();
            }
            if (type == FLOAT) {
                return this.dataIn.readFloat();
            }
            if (type == STRING) {
                return Double.valueOf(this.dataIn.readUTF());
            }
            if (type == NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to double.");
            }
            else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a double type");
            }
        }
        catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed");
                jmsEx.setLinkedException(ioe);
            }
            throw mfe;

        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Reads a <CODE>String</CODE> from the stream message.
     *
     * @return a Unicode string from the stream message
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     */

    public String readString() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(65);
            int type = this.dataIn.read();
            if (type == NULL) {
                return null;
            }
            if (type == STRING) {
                return this.dataIn.readUTF();
            }
            if (type == LONG) {
                return Long.toString(this.dataIn.readLong());
            }
            if (type == INT) {
                return Integer.toString(this.dataIn.readInt());
            }
            if (type == SHORT) {
                return Short.toString(this.dataIn.readShort());
            }
            if (type == BYTE) {
                return new Byte(this.dataIn.readByte()).toString();
            }
            if (type == FLOAT) {
                return Float.toString(this.dataIn.readFloat());
            }
            if (type == DOUBLE) {
                return Double.toString(this.dataIn.readDouble());
            }
            if (type == BOOLEAN) {
                return (this.dataIn.readBoolean() ? Boolean.TRUE : Boolean.FALSE).toString();
            }
            if (type == CHAR) {
                return new Character(this.dataIn.readChar()).toString();
            }
            else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a String type");
            }
        }
        catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed");
                jmsEx.setLinkedException(ioe);
            }
            throw mfe;

        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Reads a byte array field from the stream message into the
     * specified <CODE>byte[]</CODE> object (the read buffer).
     * <p/>
     * <P>To read the field value, <CODE>readBytes</CODE> should be
     * successively called
     * until it returns a value less than the length of the read buffer.
     * The value of the bytes in the buffer following the last byte
     * read is undefined.
     * <p/>
     * <P>If <CODE>readBytes</CODE> returns a value equal to the length of the
     * buffer, a subsequent <CODE>readBytes</CODE> call must be made. If there
     * are no more bytes to be read, this call returns -1.
     * <p/>
     * <P>If the byte array field value is null, <CODE>readBytes</CODE>
     * returns -1.
     * <p/>
     * <P>If the byte array field value is empty, <CODE>readBytes</CODE>
     * returns 0.
     * <p/>
     * <P>Once the first <CODE>readBytes</CODE> call on a <CODE>byte[]</CODE>
     * field value has been made,
     * the full value of the field must be read before it is valid to read
     * the next field. An attempt to read the next field before that has
     * been done will throw a <CODE>MessageFormatException</CODE>.
     * <p/>
     * <P>To read the byte field value into a new <CODE>byte[]</CODE> object,
     * use the <CODE>readObject</CODE> method.
     *
     * @param value the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if
     *         there is no more data because the end of the byte field has been
     *         reached
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     * @see #readObject()
     */

    public int readBytes(byte[] value) throws JMSException {
        initializeReading();
        try {
            if (value == null) {
                throw new NullPointerException();
            }
            if (bytesToRead == 0) {
                bytesToRead = -1;
                return -1;
            }
            else if (bytesToRead > 0) {
                if (value.length >= bytesToRead) {
                    bytesToRead = 0;
                    return dataIn.read(value, 0, bytesToRead);
                }
                else {
                    bytesToRead -= value.length;
                    return dataIn.read(value);
                }
            }
            else {
                if (this.dataIn.available() == 0) {
                    throw new MessageEOFException("reached end of data");
                }
                if (this.dataIn.available() < 1) {
                    throw new MessageFormatException("Not enough data left to read value");
                }
                this.dataIn.mark(value.length + 1);
                int type = this.dataIn.read();
                if (this.dataIn.available() < 1) {
                    return -1;
                }
                if (type != BYTES) {
                    throw new MessageFormatException("Not a byte array");
                }
                int len = this.dataIn.readInt();

                if (len >= value.length) {
                    bytesToRead = len - value.length;
                    return this.dataIn.read(value);
                }
                else {
                    bytesToRead = 0;
                    return this.dataIn.read(value, 0, len);
                }
            }
        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Reads an object from the stream message.
     * <p/>
     * <P>This method can be used to return, in objectified format,
     * an object in the Java programming language ("Java object") that has
     * been written to the stream with the equivalent
     * <CODE>writeObject</CODE> method call, or its equivalent primitive
     * <CODE>write<I>type</I></CODE> method.
     * <p/>
     * <P>Note that byte values are returned as <CODE>byte[]</CODE>, not
     * <CODE>Byte[]</CODE>.
     * <p/>
     * <P>An attempt to call <CODE>readObject</CODE> to read a byte field
     * value into a new <CODE>byte[]</CODE> object before the full value of the
     * byte field has been read will throw a
     * <CODE>MessageFormatException</CODE>.
     *
     * @return a Java object from the stream message, in objectified
     *         format (for example, if the object was written as an <CODE>int</CODE>,
     *         an <CODE>Integer</CODE> is returned)
     * @throws JMSException                if the JMS provider fails to read the message
     *                                     due to some internal error.
     * @throws MessageEOFException         if unexpected end of message stream has
     *                                     been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only
     *                                     mode.
     * @see #readBytes(byte[] value)
     */

    public Object readObject() throws JMSException {
        initializeReading();
        try {
            if (this.dataIn.available() == 0) {
                throw new MessageEOFException("reached end of data");
            }

            this.dataIn.mark(65);
            int type = this.dataIn.read();
            if (type == NULL) {
                return null;
            }
            if (type == STRING) {
                return this.dataIn.readUTF();
            }
            if (type == LONG) {
                return this.dataIn.readLong();
            }
            if (type == INT) {
                return this.dataIn.readInt();
            }
            if (type == SHORT) {
                return this.dataIn.readShort();
            }
            if (type == BYTE) {
                return this.dataIn.readByte();
            }
            if (type == FLOAT) {
                return this.dataIn.readFloat();
            }
            if (type == DOUBLE) {
                return this.dataIn.readDouble();
            }
            if (type == BOOLEAN) {
                return this.dataIn.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
            }
            if (type == CHAR) {
                return this.dataIn.readChar();
            }
            if (type == BYTES) {
                int len = this.dataIn.readInt();
                byte[] value = new byte[len];
                this.dataIn.readFully(value);
                return value;
            }
            else {
                this.dataIn.reset();
                throw new MessageFormatException("unknown type");
            }
        }
        catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed");
                jmsEx.setLinkedException(ioe);
            }
            throw mfe;

        }
        catch (EOFException e) {
            JMSException jmsEx = new MessageEOFException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
        catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }


    /**
     * Writes a <code>boolean</code> to the stream message.
     * The value <code>true</code> is written as the value
     * <code>(byte)1</code>; the value <code>false</code> is written as
     * the value <code>(byte)0</code>.
     *
     * @param value the <code>boolean</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeBoolean(boolean value) throws JMSException {
        initializeWriting();
        try {
            this.dataOut.write(BOOLEAN);
            this.dataOut.writeBoolean(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes a <code>byte</code> to the stream message.
     *
     * @param value the <code>byte</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeByte(byte value) throws JMSException {
        initializeWriting();
        try {
            this.dataOut.write(BYTE);
            this.dataOut.writeByte(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes a <code>short</code> to the stream message.
     *
     * @param value the <code>short</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeShort(short value) throws JMSException {
        initializeWriting();
        try {
            this.dataOut.write(SHORT);
            this.dataOut.writeShort(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes a <code>char</code> to the stream message.
     *
     * @param value the <code>char</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeChar(char value) throws JMSException {
        initializeWriting();
        try {
            this.dataOut.write(CHAR);
            this.dataOut.writeChar(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes an <code>int</code> to the stream message.
     *
     * @param value the <code>int</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeInt(int value) throws JMSException {
        initializeWriting();
        try {
            this.dataOut.write(INT);
            this.dataOut.writeInt(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes a <code>long</code> to the stream message.
     *
     * @param value the <code>long</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeLong(long value) throws JMSException {
        initializeWriting();
        try {
            this.dataOut.write(LONG);
            this.dataOut.writeLong(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes a <code>float</code> to the stream message.
     *
     * @param value the <code>float</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeFloat(float value) throws JMSException {
        initializeWriting();
        try {
            this.dataOut.write(FLOAT);
            this.dataOut.writeFloat(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes a <code>double</code> to the stream message.
     *
     * @param value the <code>double</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeDouble(double value) throws JMSException {
        initializeWriting();
        try {
            this.dataOut.write(DOUBLE);
            this.dataOut.writeDouble(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes a <code>String</code> to the stream message.
     *
     * @param value the <code>String</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeString(String value) throws JMSException {
        initializeWriting();
        try {
            if (value == null) {
                this.dataOut.write(NULL);
            }
            else {
                this.dataOut.write(STRING);
                this.dataOut.writeUTF(value);
            }
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes a byte array field to the stream message.
     * <p/>
     * <P>The byte array <code>value</code> is written to the message
     * as a byte array field. Consecutively written byte array fields are
     * treated as two distinct fields when the fields are read.
     *
     * @param value the byte array value to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeBytes(byte[] value) throws JMSException {
        writeBytes(value, 0, value.length);
    }


    /**
     * Writes a portion of a byte array as a byte array field to the stream
     * message.
     * <p/>
     * <P>The a portion of the byte array <code>value</code> is written to the
     * message as a byte array field. Consecutively written byte
     * array fields are treated as two distinct fields when the fields are
     * read.
     *
     * @param value  the byte array value to be written
     * @param offset the initial offset within the byte array
     * @param length the number of bytes to use
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeBytes(byte[] value, int offset, int length) throws JMSException {
        initializeWriting();
        try {
            this.dataOut.write(BYTES);
            this.dataOut.writeInt(length);
            this.dataOut.write(value, offset, length);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException(ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }


    /**
     * Writes an object to the stream message.
     * <p/>
     * <P>This method works only for the objectified primitive
     * object types (<code>Integer</code>, <code>Double</code>,
     * <code>Long</code>&nbsp;...), <code>String</code> objects, and byte
     * arrays.
     *
     * @param value the Java object to be written
     * @throws JMSException                 if the JMS provider fails to write the message
     *                                      due to some internal error.
     * @throws MessageFormatException       if the object is invalid.
     * @throws MessageNotWriteableException if the message is in read-only
     *                                      mode.
     */

    public void writeObject(Object value) throws JMSException {
        initializeWriting();
        if (value == null) {
            try {
                this.dataOut.write(NULL);
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException(ioe.getMessage());
                jmsEx.setLinkedException(ioe);
                throw jmsEx;
            }
        }
        else if (value instanceof String) {
            writeString(value.toString());
        }
        else if (value instanceof Character) {
            writeChar((Character) value);
        }
        else if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
        }
        else if (value instanceof Byte) {
            writeByte((Byte) value);
        }
        else if (value instanceof Short) {
            writeShort((Short) value);
        }
        else if (value instanceof Integer) {
            writeInt((Integer) value);
        }
        else if (value instanceof Float) {
            writeFloat((Float) value);
        }
        else if (value instanceof Double) {
            writeDouble((Double) value);
        }
        else if (value instanceof byte[]) {
            writeBytes((byte[]) value);
        }
    }


    /**
     * Puts the message body in read-only mode and repositions the stream of
     * bytes to the beginning.
     *
     * @throws JMSException if an internal error occurs
     */

    public void reset() throws JMSException {
        readOnlyMode = true;
        if (this.dataOut != null) {
            try {
                this.dataOut.flush();
                dataOut.close();
                this.dataOut = null;
            } catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed: " + ioe.getMessage());
                jmsEx.setLinkedException(ioe);
                throw jmsEx;
            }
        }
        this.dataIn = null;
    }

    private void initializeWriting() throws MessageNotWriteableException {
        if (readOnlyMode) {
            throw new MessageNotWriteableException("This message is in read-only mode");
        }
        if (this.dataOut == null) {
            this.bytesOut = new Buffer();
            this.dataOut = new DataOutputStream(this.bytesOut);
        }
        dataIn = null;
    }
    private void initializeReading() throws MessageNotReadableException {
        if (!readOnlyMode) {
            throw new MessageNotReadableException("This message is in write-only mode");
        }
        if (dataIn == null) {
            dataIn = new DataInputStream(bytesOut.newInputStream());
        }
        dataOut = null;
    }

    /**
     * message property types
     */
    private final static byte BYTES = 3;
    private final static byte STRING = 4;
    private final static byte BOOLEAN = 5;
    private final static byte CHAR = 6;
    private final static byte BYTE = 7;
    private final static byte SHORT = 8;
    private final static byte INT = 9;
    private final static byte LONG = 10;
    private final static byte FLOAT = 11;
    private final static byte DOUBLE = 12;
    private final static byte NULL = 13;

    private static final long serialVersionUID = 1L;
}
