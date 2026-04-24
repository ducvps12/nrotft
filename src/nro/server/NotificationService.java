package nro.server;

import nro.server.ui.SettingsPanel;
import utils.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Service trung tâm gửi Telegram/Email notification.
 * Tự động kiểm tra config trước khi gửi.
 * 
 * Sử dụng: NotificationService.gI().notifyServerStart();
 */
public class NotificationService {

    private static NotificationService instance;

    public static NotificationService gI() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    private Properties loadConfig() {
        return SettingsPanel.getNotificationConfig();
    }

    private boolean isTelegramEnabled(Properties props) {
        return "true".equals(props.getProperty("telegram.enabled"));
    }

    private boolean isEventEnabled(Properties props, String eventKey) {
        return "true".equals(props.getProperty(eventKey, "false"));
    }

    // =========================================================================
    // SERVER START / STOP
    // =========================================================================
    
    /**
     * Gửi thông báo khi server khởi động xong
     */
    public void notifyServerStart() {
        Properties props = loadConfig();
        if (!isTelegramEnabled(props) || !isEventEnabled(props, "telegram.on_start")) return;

        String serverName = "Unknown";
        try {
            Properties cfgProps = new Properties();
            try (java.io.FileReader fr = new java.io.FileReader("data/config/config.properties")) {
                cfgProps.load(fr);
            }
            serverName = cfgProps.getProperty("server.name", "Unknown");
        } catch (Exception ignored) {}

        String msg = "🟢 *SERVER STARTED*\n"
                + "━━━━━━━━━━━━━━━\n"
                + "🖥 Server: " + serverName + "\n"
                + "⏰ Thời gian: " + timestamp() + "\n"
                + "✅ Trạng thái: Online & Ready";
        SettingsPanel.sendTelegramNotification(msg);
        Logger.log(Logger.GREEN, "[Telegram] Đã gửi thông báo Server Start.\n");
    }

    /**
     * Gửi thông báo khi server tắt / bảo trì
     */
    public void notifyServerStop(String reason) {
        Properties props = loadConfig();
        if (!isTelegramEnabled(props) || !isEventEnabled(props, "telegram.on_start")) return;

        String msg = "🔴 *SERVER STOPPED*\n"
                + "━━━━━━━━━━━━━━━\n"
                + "📝 Lý do: " + (reason != null ? reason : "Manual shutdown") + "\n"
                + "⏰ Thời gian: " + timestamp();
        SettingsPanel.sendTelegramNotification(msg);
    }

    // =========================================================================
    // PLAYER LOGIN
    // =========================================================================
    
    /**
     * Gửi thông báo khi player login
     */
    public void notifyPlayerLogin(String playerName, int userId, String ip) {
        Properties props = loadConfig();
        if (!isTelegramEnabled(props) || !isEventEnabled(props, "telegram.on_login")) return;

        String msg = "👤 *PLAYER LOGIN*\n"
                + "━━━━━━━━━━━━━━━\n"
                + "🎮 Tên: " + playerName + "\n"
                + "🆔 UserID: " + userId + "\n"
                + "📍 IP: `" + ip + "`\n"
                + "⏰ " + timestamp();
        SettingsPanel.sendTelegramNotification(msg);
    }

    // =========================================================================
    // NẠP THẺ / RECHARGE
    // =========================================================================
    
    /**
     * Gửi thông báo khi có nạp thẻ/ATM
     */
    public void notifyRecharge(String playerName, long amount, String method) {
        Properties props = loadConfig();
        if (!isTelegramEnabled(props) || !isEventEnabled(props, "telegram.on_recharge")) return;

        String msg = "💰 *NẠP TIỀN*\n"
                + "━━━━━━━━━━━━━━━\n"
                + "🎮 Player: " + playerName + "\n"
                + "💵 Số tiền: " + String.format("%,d", amount) + " VNĐ\n"
                + "🏦 Phương thức: " + method + "\n"
                + "⏰ " + timestamp();
        SettingsPanel.sendTelegramNotification(msg);
    }

    // =========================================================================
    // LỖI NGHIÊM TRỌNG / CRITICAL ERROR
    // =========================================================================
    
    /**
     * Gửi thông báo lỗi nghiêm trọng
     */
    public void notifyCriticalError(String errorMessage) {
        Properties props = loadConfig();
        if (!isTelegramEnabled(props) || !isEventEnabled(props, "telegram.on_error")) return;

        String msg = "🚨 *CRITICAL ERROR*\n"
                + "━━━━━━━━━━━━━━━\n"
                + "❌ " + errorMessage + "\n"
                + "⏰ " + timestamp();
        SettingsPanel.sendTelegramNotification(msg);
    }

    /**
     * Gửi thông báo lỗi nghiêm trọng kèm exception
     */
    public void notifyCriticalError(String context, Exception e) {
        notifyCriticalError(context + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
    }
}
