package network.inetwork;

public interface ISessionAcceptHandler {

    void sessionInit(ISession session);

    void sessionDisconnect(ISession session);
}
