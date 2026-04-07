package firewall;

import utils.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Telegram Alert Bot - Gửi cảnh báo bảo mật về Telegram
 * Hỗ trợ:
 * - Alert DDoS detection
 * - Alert IP bị block
 * - Alert server status
 * - Rate limit gửi message (tránh spam)
 */
public class TelegramAlert {

    private static final TelegramAlert INSTANCE = new TelegramAlert();
    private static final String API_BASE = "https://api.telegram.org/bot";
    private static final int MESSAGE_COOLDOWN_MS = 5000; // Min 5s giữa các message
    private static final int MAX_MESSAGES_PER_MINUTE = 10;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TelegramAlert");
        t.setDaemon(true);
        return t;
    });

    private final AtomicLong lastSendTime = new AtomicLong(0);
    private final AtomicInteger messageCountThisMinute = new AtomicInteger(0);
    private long minuteStart = System.currentTimeMillis();

    public static TelegramAlert getInstance() {
        return INSTANCE;
    }

    /**
     * Gửi alert (async, không block thread chính)
     * @param message Nội dung markdown
     */
    public void sendAlert(String message) {
        FirewallConfig config = FirewallConfig.getInstance();
        if (!config.telegramAlertEnabled) return;
        if (config.telegramBotToken.isEmpty() || config.telegramChatId.isEmpty()) return;

        // Rate limit gửi message
        long now = System.currentTimeMillis();
        if (now - lastSendTime.get() < MESSAGE_COOLDOWN_MS) return;
        if (now - minuteStart > 60000) {
            minuteStart = now;
            messageCountThisMinute.set(0);
        }
        if (messageCountThisMinute.get() >= MAX_MESSAGES_PER_MINUTE) return;

        lastSendTime.set(now);
        messageCountThisMinute.incrementAndGet();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String fullMessage = "🛡 *FIREWALL ALERT*\n"
            + "⏰ " + timestamp + "\n\n"
            + message;

        executor.submit(() -> {
            try {
                sendTelegramMessage(config.telegramBotToken, config.telegramChatId, fullMessage);
            } catch (Exception e) {
                Logger.error("Telegram alert error: " + e.getMessage());
            }
        });
    }

    /**
     * Gửi thông báo DDoS
     */
    public void alertDDoS(String ip, int pps, String detail) {
        sendAlert(
            "🚨 *PHÁT HIỆN TẤN CÔNG DDoS*\n" +
            "━━━━━━━━━━━━━━━\n" +
            "📍 IP: `" + ip + "`\n" +
            "📊 PPS: " + pps + "\n" +
            "📝 Chi tiết: " + detail + "\n" +
            "⚡ Hành động: Auto-blocked"
        );
    }

    /**
     * Gửi thông báo IP bị block
     */
    public void alertBlocked(String ip, String reason) {
        sendAlert(
            "🔴 *IP BỊ CHẶN*\n" +
            "━━━━━━━━━━━━━━━\n" +
            "📍 IP: `" + ip + "`\n" +
            "📝 Lý do: " + reason
        );
    }

    /**
     * Gửi thông báo server status
     */
    public void alertServerStatus(String status) {
        sendAlert(
            "📡 *SERVER STATUS*\n" +
            "━━━━━━━━━━━━━━━\n" +
            status
        );
    }

    /**
     * Gửi thông báo Geo-Block
     */
    public void alertGeoBlocked(String ip, String country) {
        sendAlert(
            "🌍 *GEO-BLOCK*\n" +
            "━━━━━━━━━━━━━━━\n" +
            "📍 IP: `" + ip + "`\n" +
            "🗺 Quốc gia: " + country + "\n" +
            "⚡ Hành động: Rejected (Only VN allowed)"
        );
    }

    /**
     * Test kết nối bot
     */
    public boolean testConnection() {
        FirewallConfig config = FirewallConfig.getInstance();
        if (config.telegramBotToken.isEmpty()) return false;

        try {
            String url = API_BASE + config.telegramBotToken + "/getMe";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy Chat ID từ last update (helper cho setup)
     */
    public String getLastChatId() {
        FirewallConfig config = FirewallConfig.getInstance();
        if (config.telegramBotToken.isEmpty()) return null;

        try {
            String url = API_BASE + config.telegramBotToken + "/getUpdates?limit=1&offset=-1";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            conn.disconnect();

            // Parse simple - tìm "chat":{"id":xxxx
            String body = response.toString();
            int idx = body.indexOf("\"chat\":{\"id\":");
            if (idx != -1) {
                int start = idx + 13;
                int end = body.indexOf(",", start);
                if (end == -1) end = body.indexOf("}", start);
                return body.substring(start, end);
            }
        } catch (Exception e) {
            Logger.error("Get chat ID error: " + e.getMessage());
        }
        return null;
    }

    // ===== PRIVATE =====

    private void sendTelegramMessage(String botToken, String chatId, String text) throws Exception {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String url = API_BASE + botToken + "/sendMessage?chat_id=" + chatId
                + "&text=" + encodedText + "&parse_mode=Markdown";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) error.append(line);
            reader.close();
            Logger.error("Telegram API error " + responseCode + ": " + error);
        }
        conn.disconnect();
    }
}
