package dalma.endpoints.jms.impl;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;

/**
 * {@link Serializable} {@link BytesMessage}.
 *
 * @author Kohsuke Kawaguchi
 * @ThirdParty this class contains code released under ASL.
 */
public class BytesMessageImpl extends MessageImpl implements BytesMessage {
    private DataOutputStream dataOut;
    private Buffer buffer;
    private DataInputStream dataIn;
    private transient long bodyLength = 0;

    public BytesMessageImpl() {
    }

    public BytesMessageImpl(BytesMessage s) throws JMSException {
        super(s);
        long len = s.getBodyLength();
        if(len>0) {
            byte[] data = new byte[(int)len];
            s.readBytes(data);
            buffer = new Buffer(data);
        }
    }

    public void clearBody() throws JMSException {
        this.dataOut = null;
        this.dataIn = null;
        this.buffer = null;
    }

    public long getBodyLength() throws JMSException {
        preRead();
        return bodyLength;
    }

    public boolean readBoolean() throws JMSException {
        preRead();
        try {
            return this.dataIn.readBoolean();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public byte readByte() throws JMSException {
        preRead();
        try {
            return this.dataIn.readByte();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public int readUnsignedByte() throws JMSException {
        preRead();
        try {
            return this.dataIn.readUnsignedByte();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public short readShort() throws JMSException {
        preRead();
        try {
            return this.dataIn.readShort();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public int readUnsignedShort() throws JMSException {
        preRead();
        try {
            return this.dataIn.readUnsignedShort();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public char readChar() throws JMSException {
        preRead();
        try {
            return this.dataIn.readChar();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public int readInt() throws JMSException {
        preRead();
        try {
            return this.dataIn.readInt();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public long readLong() throws JMSException {
        preRead();
        try {
            return this.dataIn.readLong();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public float readFloat() throws JMSException {
        preRead();
        try {
            return this.dataIn.readFloat();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public double readDouble() throws JMSException {
        preRead();
        try {
            return this.dataIn.readDouble();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public String readUTF() throws JMSException {
        preRead();
        try {
            return this.dataIn.readUTF();
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public int readBytes(byte[] value) throws JMSException {
        return readBytes(value, value.length);
    }

    public int readBytes(byte[] value, int length) throws JMSException {
        preRead();
        try {
            int n = 0;
            while (n < length) {
                int count = this.dataIn.read(value, n, length - n);
                if (count < 0) {
                    break;
                }
                n += count;
            }
            if (n == 0 && length > 0) {
                n = -1;
            }
            return n;
        }
        catch (EOFException eof) {
            JMSException jmsEx = new MessageEOFException(eof.getMessage());
            jmsEx.setLinkedException(eof);
            throw jmsEx;
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Format error occured" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeBoolean(boolean value) throws JMSException {
        preWrite();
        try {
            this.dataOut.writeBoolean(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeByte(byte value) throws JMSException {
        preWrite();
        try {
            this.dataOut.writeByte(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeShort(short value) throws JMSException {
        preWrite();
        try {
            this.dataOut.writeShort(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeChar(char value) throws JMSException {
        preWrite();
        try {
            this.dataOut.writeChar(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeInt(int value) throws JMSException {
        preWrite();
        try {
            this.dataOut.writeInt(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeLong(long value) throws JMSException {
        preWrite();
        try {
            this.dataOut.writeLong(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeFloat(float value) throws JMSException {
        preWrite();
        try {
            this.dataOut.writeFloat(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeDouble(double value) throws JMSException {
        preWrite();
        try {
            this.dataOut.writeDouble(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeUTF(String value) throws JMSException {
        preWrite();
        try {
            this.dataOut.writeUTF(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeBytes(byte[] value) throws JMSException {
        preWrite();
        try {
            this.dataOut.write(value);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeBytes(byte[] value, int offset, int length) throws JMSException {
        preWrite();
        try {
            this.dataOut.write(value, offset, length);
        }
        catch (IOException ioe) {
            JMSException jmsEx = new JMSException("Could not write data:" + ioe.getMessage());
            jmsEx.setLinkedException(ioe);
            throw jmsEx;
        }
    }

    public void writeObject(Object value) throws JMSException {
        if (value == null) {
            throw new NullPointerException();
        }
        preWrite();
        if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
        }
        else if (value instanceof Character) {
            writeChar((Character) value);
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
        else if (value instanceof Double) {
            writeDouble((Double) value);
        }
        else if (value instanceof Long) {
            writeLong((Long) value);
        }
        else if (value instanceof Float) {
            writeFloat((Float) value);
        }
        else if (value instanceof String) {
            writeUTF(value.toString());
        }
        else if (value instanceof byte[]) {
            writeBytes((byte[]) value);
        }
        else {
            throw new MessageFormatException("Cannot write non-primitive type:" + value.getClass());
        }
    }

    public void reset() throws JMSException {
//        super.readOnlyMessage = true;
        if (this.dataOut != null) {
            try {
                this.dataOut.flush();
                dataOut.close();
            }
            catch (IOException ioe) {
                JMSException jmsEx = new JMSException("reset failed: " + ioe.getMessage());
                jmsEx.setLinkedException(ioe);
                throw jmsEx;
            }
        }
        this.dataIn = null;
        this.dataOut = null;
    }

    private void preWrite() {
//        if (super.readOnlyMessage) {
//            throw new MessageNotWriteableException("This message is in read-only mode");
//        }
        if (this.dataOut == null) {
            this.buffer = new Buffer();
            this.dataOut = new DataOutputStream(this.buffer);
        }
    }

    private void preRead() {
//        if (!super.readOnlyMessage) {
//            throw new MessageNotReadableException("This message is in write-only mode");
//        }
        if (this.dataIn == null) {
            this.dataIn = new DataInputStream(buffer.newInputStream());
        }
    }

    private static final long serialVersionUID = 1L;
}
