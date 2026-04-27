package network;

import java.io.IOException;
import java.net.Socket;

public class Session extends network.session.Session implements network.inetwork.ISession {

    public Session(String host, int port) throws IOException {
        super(host, port);
    }

    public Session(Socket socket) {
        super(socket);
    }
}
