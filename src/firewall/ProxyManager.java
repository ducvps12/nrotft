/*
 * ProxyManager - Quản lý multi-port TCP Proxy cho Anti-DDoS
 * Hỗ trợ: Start/Stop individual proxy, Auto-start từ config, Multi-port batch operations
 */
package firewall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.table.DefaultTableModel;
import utils.Logger;

public class ProxyManager {
    private static ProxyManager instance;
    private final Map<Integer, TCPProxy> activeProxies;

    private ProxyManager() {
        this.activeProxies = new ConcurrentHashMap<>();
    }

    public static synchronized ProxyManager getInstance() {
        if (instance == null) {
            instance = new ProxyManager();
        }
        return instance;
    }

    /**
     * Start proxy cho 1 port
     */
    public boolean startProxy(String targetIp, int targetPort, int listenPort, DefaultTableModel tableModel) {
        if (activeProxies.containsKey(listenPort)) {
            Logger.error("Port " + listenPort + " đã được sử dụng.\n");
            return false;
        }

        TCPProxy proxy = new TCPProxy(targetIp, targetPort, listenPort);
        activeProxies.put(listenPort, proxy);
        proxy.start();
        if (tableModel != null) {
            tableModel.addRow(new Object[]{targetIp, targetPort, listenPort, "Running"});
        }
        return true;
    }

    /**
     * Start proxy (không cần table model)
     */
    public boolean startProxy(String targetIp, int targetPort, int listenPort) {
        return startProxy(targetIp, targetPort, listenPort, null);
    }

    /**
     * Stop proxy theo listen port
     */
    public boolean stopProxy(int listenPort, DefaultTableModel tableModel, int viewRow) {
        TCPProxy proxy = activeProxies.remove(listenPort);
        if (proxy != null) {
            proxy.stop();
            if (tableModel != null && viewRow >= 0 && viewRow < tableModel.getRowCount()) {
                tableModel.removeRow(viewRow);
            }
            return true;
        }
        return false;
    }

    /**
     * Stop proxy (không cần table model)
     */
    public boolean stopProxy(int listenPort) {
        TCPProxy proxy = activeProxies.remove(listenPort);
        if (proxy != null) {
            proxy.stop();
            return true;
        }
        return false;
    }

    /**
     * Kiểm tra port đang chạy
     */
    public boolean isRunning(int listenPort) {
        TCPProxy proxy = activeProxies.get(listenPort);
        return proxy != null && proxy.isRunning();
    }

    /**
     * Lấy TCPProxy instance theo listen port
     */
    public TCPProxy getProxy(int listenPort) {
        return activeProxies.get(listenPort);
    }

    /**
     * Lấy tất cả proxy đang chạy
     */
    public Map<Integer, TCPProxy> getActiveProxies() {
        return activeProxies;
    }

    /**
     * Đếm proxy đang chạy
     */
    public int getActiveCount() {
        return activeProxies.size();
    }

    /**
     * Tổng connections qua tất cả proxy
     */
    public long getTotalConnections() {
        return activeProxies.values().stream()
                .mapToLong(TCPProxy::getTotalConnections).sum();
    }

    /**
     * Tổng blocked connections qua tất cả proxy
     */
    public long getTotalBlocked() {
        return activeProxies.values().stream()
                .mapToLong(TCPProxy::getBlockedConnections).sum();
    }

    /**
     * Tổng active connections qua tất cả proxy
     */
    public int getTotalActiveConnections() {
        return activeProxies.values().stream()
                .mapToInt(TCPProxy::getActiveConnections).sum();
    }

    /**
     * Auto-start tất cả protectedPorts từ config
     */
    public int autoStartFromConfig() {
        FirewallConfig config = FirewallConfig.getInstance();
        if (!config.autoStartProtection || config.protectedPorts.isEmpty()) {
            return 0;
        }

        int started = 0;
        for (FirewallConfig.ProtectedPort pp : config.protectedPorts) {
            if (pp.enabled && !activeProxies.containsKey(pp.listenPort)) {
                try {
                    boolean ok = startProxy(pp.targetIP, pp.targetPort, pp.listenPort);
                    if (ok) {
                        started++;
                        Logger.log("✅ Auto-started protection: " + pp.label + " :" + pp.listenPort + " → " + pp.targetIP + ":" + pp.targetPort);
                    }
                } catch (Exception e) {
                    Logger.error("❌ Failed to auto-start " + pp.label + ": " + e.getMessage());
                }
            }
        }

        if (started > 0) {
            TelegramAlert.getInstance().alertServerStatus(
                "🛡 Anti-DDoS Auto-Start\n" +
                "Đã tự động bảo vệ " + started + " port(s)\n" +
                "Firewall: " + (config.firewallEnabled ? "ON" : "OFF") + "\n" +
                "Rate Limit: " + (config.rateLimitEnabled ? "ON" : "OFF")
            );
        }

        return started;
    }

    /**
     * Stop tất cả proxy
     */
    public void stopAll() {
        activeProxies.values().forEach(TCPProxy::stop);
        activeProxies.clear();
    }
}