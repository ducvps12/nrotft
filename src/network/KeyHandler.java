package network;

import network.inetwork.ISession;

public class KeyHandler implements network.handler.IKeySessionHandler {

    public void sendKey(ISession session) {
        Message msg = new Message(CommandMessage.REQUEST_KEY);
        try {
            byte[] keys = session.getKey();
            msg.writer().writeByte(keys.length);
            msg.writer().writeByte(keys[0]);
            for (int i = 1; i < keys.length; i++) {
                msg.writer().writeByte(keys[i] ^ keys[i - 1]);
            }
            session.doSendMessage(msg);
            msg.cleanup();
            session.setSentKey(true);
        } catch (Exception ignored) {
        }
    }

    public void setKey(ISession session, Message message) throws Exception {
        try {
            byte length = message.reader().readByte();
            byte[] keys = new byte[length];
            for (int i = 0; i < length; i++) {
                keys[i] = message.reader().readByte();
            }
            for (int i = 0; i < keys.length - 1; i++) {
                keys[i + 1] = (byte) (keys[i + 1] ^ keys[i]);
            }
            session.setKey(keys);
            session.setSentKey(true);
        } catch (Exception ignored) {
        }
    }

    @Override
    public final void sendKey(network.session.ISession session) {
        sendKey((ISession) session);
    }

    @Override
    public final void setKey(network.session.ISession session, network.io.Message message) throws Exception {
        setKey((ISession) session, (Message) message);
    }
}
