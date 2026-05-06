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
        int port = Manager.apiPort;
        boolean started = false;

        // Thử bind port, nếu bận thì tăng port
        while (!started) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                started = true;
            } catch (BindException e) {
                System.out.println("⚠️ Port " + port + " đang bận, thử port " + (port + 1) + "...");
                port++;
                if (port > Manager.apiPort + 10) {
                    System.out.println("❌ Không tìm được port trống cho HTTP Server!");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            server.createContext("/", new SimpleHttpHandler());
            server.setExecutor(Executors.newFixedThreadPool(Math.max(2, Manager.workerGroup)));
            server.start();
            System.out.println("Dashboard: http://localhost:" + port + "/admin");

            // Giữ thread sống cho đến khi bị interrupt
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            // Thread bị interrupt — thoát bình thường
        } catch (Exception e) {
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
