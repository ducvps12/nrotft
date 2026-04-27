package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import network.inetwork.ISession;

public class MessageSendCollect implements network.handler.IMessageSendCollect {

    private int curR;
    private int curW;

    public Message readMessage(ISession session, DataInputStream dis) throws Exception {
        int size;
        byte cmd = dis.readByte();
        if (session.sentKey()) {
            cmd = readKey(session, cmd);
        }

        if (session.sentKey()) {
            byte b1 = dis.readByte();
            byte b2 = dis.readByte();
            size = (readKey(session, b1) & 0xFF) << 8 | readKey(session, b2) & 0xFF;
        } else {
            size = dis.readUnsignedShort();
        }

        byte[] data = new byte[size];
        int len = 0;
        int byteRead = 0;
        while (len != -1 && byteRead < size) {
            len = dis.read(data, byteRead, size - byteRead);
            if (len > 0) {
                byteRead += len;
            }
        }

        if (session.sentKey()) {
            for (int i = 0; i < data.length; i++) {
                data[i] = readKey(session, data[i]);
            }
        }
        return new Message(cmd, data);
    }

    public byte readKey(ISession session, byte b) {
        byte i = (byte) (session.getKey()[curR++] & 0xFF ^ b & 0xFF);
        if (curR >= session.getKey().length) {
            curR %= session.getKey().length;
        }
        return i;
    }

    public void doSendMessage(ISession session, DataOutputStream dos, Message msg) throws Exception {
        byte[] data = msg.getData();
        if (session.sentKey()) {
            dos.writeByte(writeKey(session, msg.command));
        } else {
            dos.writeByte(msg.command);
        }

        if (data != null) {
            int size = data.length;
            if (msg.command == -32 || msg.command == -66 || msg.command == -74
                    || msg.command == 11 || msg.command == -67 || msg.command == -87
                    || msg.command == 66) {
                dos.writeByte(writeKey(session, (byte) size) - 128);
                dos.writeByte(writeKey(session, (byte) (size >> 8)) - 128);
                dos.writeByte(writeKey(session, (byte) (size >> 16)) - 128);
            } else if (session.sentKey()) {
                dos.writeByte(writeKey(session, (byte) (size >> 8)));
                dos.writeByte(writeKey(session, (byte) (size & 0xFF)));
            } else {
                dos.writeShort(size);
            }

            if (session.sentKey()) {
                for (int i = 0; i < data.length; i++) {
                    data[i] = writeKey(session, data[i]);
                }
            }
            dos.write(data);
        } else {
            dos.writeShort(0);
        }
        dos.flush();
        msg.cleanup();
    }

    public byte writeKey(ISession session, byte b) {
        byte i = (byte) (session.getKey()[curW++] & 0xFF ^ b & 0xFF);
        if (curW >= session.getKey().length) {
            curW %= session.getKey().length;
        }
        return i;
    }

    @Override
    public final network.io.Message readMessage(network.session.ISession session, DataInputStream dis) throws Exception {
        return readMessage((ISession) session, dis);
    }

    @Override
    public final byte readKey(network.session.ISession session, byte b) {
        return readKey((ISession) session, b);
    }

    @Override
    public final void doSendMessage(network.session.ISession session, DataOutputStream dos, network.io.Message msg) throws Exception {
        doSendMessage((ISession) session, dos, (Message) msg);
    }

    @Override
    public final byte writeKey(network.session.ISession session, byte b) {
        return writeKey((ISession) session, b);
    }
}
