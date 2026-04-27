package network.inetwork;

import network.Message;

public interface IMessageHandler extends network.handler.IMessageHandler {

    void onMessage(ISession session, Message message);

    @Override
    default void onMessage(network.session.ISession session, network.io.Message message) throws Exception {
        onMessage((ISession) session, (Message) message);
    }
}
