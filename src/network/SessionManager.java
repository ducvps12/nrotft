package network;

import java.util.List;
import network.inetwork.ISession;
import network.server.SessionManager_network;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    public static SessionManager gI() {
        return INSTANCE;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<ISession> getSessions() {
        return (List) SessionManager_network.gI().getSessions();
    }

    public ISession findByID(long id) throws Exception {
        return (ISession) SessionManager_network.gI().findByID(id);
    }

    public int getNumSession() {
        return SessionManager_network.gI().getNumSession();
    }
}
