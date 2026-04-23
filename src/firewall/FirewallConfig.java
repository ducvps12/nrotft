package firewall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import utils.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cấu hình Firewall - lưu/đọc từ file JSON
 * Cho phép tùy chỉnh từ panel hoặc file config
 */
public class FirewallConfig {

    private static final String CONFIG_FILE = "firewall_config.json";
    private static FirewallConfig instance;

    // ===== CẤU HÌNH CHUNG =====
    public boolean firewallEnabled = true;
    public boolean geoBlockEnabled = false;        // Block quốc tế (chỉ cho VN)
    public boolean rateLimitEnabled = true;         // Rate limiting per IP
    public boolean telegramAlertEnabled = false;    // Cảnh báo Telegram
    public boolean autoBlockEnabled = true;         // Tự động block IP vi phạm
    public boolean lockdownMode = false;            // Chặn tất cả kết nối mới

    // ===== RATE LIMIT =====
    public int maxConnectionsPerSecond = 10;        // Max kết nối/giây per IP
    public int maxConcurrentPerIP = 50;             // Max kết nối đồng thời per IP
    public int burstSize = 20;                      // Burst cho phép
    public int blockDurationMinutes = 30;           // Thời gian block (phút)

    // ===== DDoS DETECTION =====
    public int ddosThresholdPPS = 300;              // Packets per second threshold
    public int ddosThresholdConnPerSec = 50;        // Connections per second threshold (toàn server)

    // ===== TELEGRAM =====
    public String telegramBotToken = "";
    public String telegramChatId = "";              // Chat ID nhận thông báo

    // ===== PORTS =====
    public int gamePort = 14445;
    public int proxyListenPort = 24445;             // Port proxy public

    // ===== ANTI-DDoS MULTI-PORT PROTECTION =====
    public boolean autoStartProtection = false;     // Tự động bảo vệ khi server start
    public List<ProtectedPort> protectedPorts = new ArrayList<>();  // Danh sách port được bảo vệ

    // ===== IP LISTS =====
    public Set<String> whitelistIPs = new HashSet<>();   // IP luôn được phép
    public Set<String> blacklistIPs = new HashSet<>();   // IP luôn bị chặn
    public Set<String> blockedIPs = new HashSet<>();     // IP đang bị block (runtime)

    /**
     * Cấu hình 1 port được bảo vệ Anti-DDoS
     */
    public static class ProtectedPort {
        public String label;          // Tên hiển thị: "Web HTTP", "Game Server"...
        public String targetIP;       // IP đích (VPS)
        public int targetPort;        // Port đích thực tế
        public int listenPort;        // Port proxy listen (public)
        public boolean enabled;       // Có bật hay không

        public ProtectedPort() {}

        public ProtectedPort(String label, String targetIP, int targetPort, int listenPort, boolean enabled) {
            this.label = label;
            this.targetIP = targetIP;
            this.targetPort = targetPort;
            this.listenPort = listenPort;
            this.enabled = enabled;
        }

        @Override
        public String toString() {
            return label + " [" + targetIP + ":" + targetPort + " ← :" + listenPort + "]";
        }
    }

    private FirewallConfig() {
        // Default whitelist
        whitelistIPs.add("127.0.0.1");
        whitelistIPs.add("::1");
    }

    public static synchronized FirewallConfig getInstance() {
        if (instance == null) {
            instance = new FirewallConfig();
            instance.load();
        }
        return instance;
    }

    public void save() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(Paths.get(CONFIG_FILE), gson.toJson(this));
        } catch (Exception e) {
            Logger.error("Lỗi lưu firewall config: " + e.getMessage());
        }
    }

    public void load() {
        try {
            File f = new File(CONFIG_FILE);
            if (f.exists()) {
                String json = Files.readString(f.toPath());
                FirewallConfig loaded = new Gson().fromJson(json, FirewallConfig.class);
                if (loaded != null) {
                    this.firewallEnabled = loaded.firewallEnabled;
                    this.geoBlockEnabled = loaded.geoBlockEnabled;
                    this.rateLimitEnabled = loaded.rateLimitEnabled;
                    this.telegramAlertEnabled = loaded.telegramAlertEnabled;
                    this.autoBlockEnabled = loaded.autoBlockEnabled;
                    this.lockdownMode = loaded.lockdownMode;
                    this.maxConnectionsPerSecond = loaded.maxConnectionsPerSecond;
                    this.maxConcurrentPerIP = loaded.maxConcurrentPerIP;
                    this.burstSize = loaded.burstSize;
                    this.blockDurationMinutes = loaded.blockDurationMinutes;
                    this.ddosThresholdPPS = loaded.ddosThresholdPPS;
                    this.ddosThresholdConnPerSec = loaded.ddosThresholdConnPerSec;
                    this.telegramBotToken = loaded.telegramBotToken;
                    this.telegramChatId = loaded.telegramChatId;
                    this.gamePort = loaded.gamePort;
                    this.proxyListenPort = loaded.proxyListenPort;
                    this.autoStartProtection = loaded.autoStartProtection;
                    if (loaded.protectedPorts != null) this.protectedPorts = new ArrayList<>(loaded.protectedPorts);
                    if (loaded.whitelistIPs != null) this.whitelistIPs = loaded.whitelistIPs;
                    if (loaded.blacklistIPs != null) this.blacklistIPs = loaded.blacklistIPs;
                    if (loaded.blockedIPs != null) this.blockedIPs = loaded.blockedIPs;
                    Logger.log("Firewall config loaded successfully.");
                }
            } else {
                save(); // Tạo file config mặc định
                Logger.log("Firewall config created with defaults.");
            }
        } catch (Exception e) {
            Logger.error("Lỗi load firewall config: " + e.getMessage());
        }
    }
}
