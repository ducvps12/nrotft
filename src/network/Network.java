package network;

import network.inetwork.ISessionAcceptHandler;
import network.server.IServerClose;
import network.server.Server_firewall;

public class Network {

    private static final Network INSTANCE = new Network();
    private final Server_firewall server = Server_firewall.gI();

    public static Network gI() {
        return INSTANCE;
    }

    public Network init() {
        server.init();
        return this;
    }

    public Network start(int port) throws Exception {
        server.start(port);
        return this;
    }

    public Network setAcceptHandler(ISessionAcceptHandler handler) {
        server.setAcceptHandler(new network.server.ISessionAcceptHandler() {
            @Override
            public void sessionInit(network.session.ISession session) {
                handler.sessionInit((network.inetwork.ISession) session);
            }

            @Override
            public void sessionDisconnect(network.session.ISession session) {
                handler.sessionDisconnect((network.inetwork.ISession) session);
            }
        });
        return this;
    }

    public Network close() {
        server.close();
        return this;
    }

    public Network dispose() {
        server.dispose();
        return this;
    }

    public Network randomKey(boolean randomKey) {
        server.randomKey(randomKey);
        return this;
    }

    public Network setDoSomeThingWhenClose(IServerClose serverClose) {
        server.setDoSomeThingWhenClose(serverClose);
        return this;
    }

    public Network setTypeSessioClone(Class clazz) throws Exception {
        server.setTypeSessioClone(clazz);
        return this;
    }

    public boolean isRandomKey() {
        return server.isRandomKey();
    }

    public void stopConnect() {
        server.stopConnect();
    }
}
