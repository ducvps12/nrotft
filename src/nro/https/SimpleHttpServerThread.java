package nro.https;

import com.sun.net.httpserver.HttpServer;
import nro.server.Manager;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class SimpleHttpServerThread extends Thread {

    private HttpServer server;

    @Override
    public void run() {
        try {
            server = HttpServer.create(new InetSocketAddress(Manager.apiPort), 0);
            server.createContext("/", new SimpleHttpHandler());
            server.setExecutor(Executors.newFixedThreadPool(Math.max(2, Manager.workerGroup)));
            server.start();
            System.out.println("Dashboard: http://localhost:" + Manager.apiPort + "/admin");

            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
            }
            int port = Manager.apiPort;
            boolean started = false;
            while (!started) {
                try {
                    server = HttpServer.create(new InetSocketAddress(port), 0);
                    started = true;
                } catch (BindException e) {
                    System.out.println("⚠️ Port " + port + " đang bận, thử port khác...");
                    port++;
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 HTTP Server stopped");
        }
    }
}
