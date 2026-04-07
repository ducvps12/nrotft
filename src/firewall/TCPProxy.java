package firewall;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import utils.Logger;

/**
 * TCP Proxy nâng cao - tích hợp:
 * - Rate Limiting (chống DDoS L4)
 * - Geo-blocking (chỉ VN)
 * - IP Blacklist/Whitelist
 * - Connection statistics
 * - Telegram alerts
 */
public class TCPProxy implements Runnable {
    private final String targetIp;
    private final int targetPort;
    private final int listenPort;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final Thread thread;

    // Thống kê
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong blockedConnections = new AtomicLong(0);
    private final AtomicLong geoBlockedConnections = new AtomicLong(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public TCPProxy(String targetIp, int targetPort, int listenPort) {
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.listenPort = listenPort;
        this.thread = Thread.ofVirtual().name("TCPProxy-" + listenPort).unstarted(this);
    }

    public void start() {
        if (thread != null && !thread.isAlive()) {
            thread.start();
        }
    }

    public void stop() {
        this.running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Logger.error("Lỗi khi đóng proxy trên cổng " + listenPort + ": " + e.getMessage());
        }
        thread.interrupt();
    }

    @Override
    public void run() {
        FirewallConfig config = FirewallConfig.getInstance();

        try {
            serverSocket = new ServerSocket(listenPort);
            Logger.log("🛡 Firewall Proxy ON: :" + listenPort + " → " + targetIp + ":" + targetPort);
            Logger.log("   Rate Limit: " + (config.rateLimitEnabled ? config.maxConnectionsPerSecond + " conn/s" : "OFF"));
            Logger.log("   Geo-Block VN: " + (config.geoBlockEnabled ? "ON" : "OFF"));

            // Load GeoIP database
            if (config.geoBlockEnabled) {
                GeoIPFilter.getInstance().load();
            }

            // Gửi thông báo Telegram
            TelegramAlert.getInstance().alertServerStatus(
                "✅ Firewall Proxy khởi động\n" +
                "📡 Port: " + listenPort + " → " + targetIp + ":" + targetPort + "\n" +
                "🔒 Rate Limit: " + (config.rateLimitEnabled ? "ON (" + config.maxConnectionsPerSecond + "/s)" : "OFF") + "\n" +
                "🌍 Geo-Block: " + (config.geoBlockEnabled ? "ON (VN Only)" : "OFF")
            );

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    totalConnections.incrementAndGet();

                    String clientIP = ((InetSocketAddress) clientSocket.getRemoteSocketAddress())
                            .getAddress().getHostAddress();

                    // ===== KIỂM TRA BẢO MẬT =====
                    String rejectReason = checkConnection(clientIP);

                    if (rejectReason != null) {
                        // Reject connection
                        blockedConnections.incrementAndGet();
                        clientSocket.close();
                        continue;
                    }

                    // ===== CHO PHÉP KẾT NỐI =====
                    activeConnections.incrementAndGet();
                    Thread.ofVirtual()
                            .name("ProxyTask-" + listenPort + "-" + clientIP)
                            .start(new ProxyTask(clientSocket, clientIP));

                } catch (IOException e) {
                    if (running) {
                        Logger.error("Accept error on port " + listenPort + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            Logger.error("Không thể khởi động proxy trên cổng " + listenPort);
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try { serverSocket.close(); } catch (IOException ignored) {}
            }
            TelegramAlert.getInstance().alertServerStatus(
                "🔴 Firewall Proxy dừng\nPort: " + listenPort
            );
        }
    }

    /**
     * Kiểm tra kết nối có hợp lệ không
     * @return null nếu OK, hoặc lý do reject
     */
    private String checkConnection(String ip) {
        FirewallConfig config = FirewallConfig.getInstance();

        // 1. Lockdown mode
        if (config.lockdownMode) {
            if (!config.whitelistIPs.contains(ip)) {
                return "Lockdown mode active";
            }
        }

        // 2. Firewall disabled
        if (!config.firewallEnabled) return null;

        // 3. Whitelist - luôn cho phép
        if (config.whitelistIPs.contains(ip)) return null;

        // 4. Blacklist - luôn chặn
        if (config.blacklistIPs.contains(ip) || config.blockedIPs.contains(ip)) {
            return "Blacklisted";
        }

        // 5. Geo-blocking
        if (config.geoBlockEnabled) {
            if (!GeoIPFilter.getInstance().isVietnamIP(ip)) {
                geoBlockedConnections.incrementAndGet();
                // Chỉ alert cho mỗi IP 1 lần (tránh spam)
                TelegramAlert.getInstance().alertGeoBlocked(ip, "Non-VN");
                return "Geo-blocked (non-VN)";
            }
        }

        // 6. Rate limiting
        if (config.rateLimitEnabled) {
            if (!RateLimiter.getInstance().allowConnection(ip)) {
                return "Rate limited";
            }
        }

        // 7. Rate limiter temporary ban
        if (RateLimiter.getInstance().isBanned(ip)) {
            return "Temporarily banned";
        }

        return null; // OK
    }

    // ===== THỐNG KÊ =====
    public long getTotalConnections() { return totalConnections.get(); }
    public long getBlockedConnections() { return blockedConnections.get(); }
    public long getGeoBlockedConnections() { return geoBlockedConnections.get(); }
    public int getActiveConnections() { return activeConnections.get(); }
    public int getListenPort() { return listenPort; }
    public String getTargetIp() { return targetIp; }
    public int getTargetPort() { return targetPort; }
    public boolean isRunning() { return running; }

    // ===== PROXY TASK =====

    private class ProxyTask implements Runnable {
        private final Socket clientSocket;
        private final String clientIP;

        public ProxyTask(Socket clientSocket, String clientIP) {
            this.clientSocket = clientSocket;
            this.clientIP = clientIP;
        }

        @Override
        public void run() {
            try (Socket targetSocket = new Socket(targetIp, targetPort)) {
                // Set timeouts
                clientSocket.setSoTimeout(300000); // 5 min timeout
                targetSocket.setSoTimeout(300000);

                Thread t1 = Thread.ofVirtual().name("C2S").start(() -> forwardStream(clientSocket, targetSocket));
                Thread t2 = Thread.ofVirtual().name("S2C").start(() -> forwardStream(targetSocket, clientSocket));

                t1.join();
                t2.join();

            } catch (IOException | InterruptedException e) {
                // Connection ended
            } finally {
                try { clientSocket.close(); } catch (IOException ignored) {}
                activeConnections.decrementAndGet();
                RateLimiter.getInstance().releaseConnection(clientIP);
            }
        }

        private void forwardStream(Socket input, Socket output) {
            try {
                InputStream in = input.getInputStream();
                OutputStream out = output.getOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    out.flush();
                }
            } catch (IOException e) {
                // Stream ended
            } finally {
                try { input.close(); output.close(); } catch (IOException ignored) {}
            }
        }
    }
}