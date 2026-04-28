package nro.recharge;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.regex.*;
import jdbc.DBConnecter;
import nro.player.Player;

import nro.server.Client;
import nro.services.Service;

public class RechargeHttp {

    private static final int PORT = 8080; 
    private static final String WEBHOOK_KEY = "BARCOLLxENZEEFXNRO"; // Key webhook

    //  Hệ số sự kiện (2.0 = X2, 3.0 = X3, 4.0 = X4, 1.0 = bình thường)
    private static final double HE_SO_SU_KIEN = 1.0;  

    public static void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/sepay", new SepayHandler());
        server.start();
        System.out.println("Sepay Webhook started and listening on http://0.0.0.0:" + PORT + "/sepay");
    }

    static class SepayHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) {
            try {
                String method = ex.getRequestMethod();

                if (method.equalsIgnoreCase("GET")) {
                    send(ex, 200, "Webhook Sepay đang hoạt động – vui lòng dùng POST");
                    return;
                }

                if (!method.equalsIgnoreCase("POST")) {
                    send(ex, 405, "Method Not Allowed");
                    return;
                }

                String auth = ex.getRequestHeaders().getFirst("Authorization");
                if (auth == null || !auth.equals("Apikey " + WEBHOOK_KEY)) {
                    System.out.println("❌ Sai API Key: " + auth);
                    send(ex, 401, "Unauthorized");
                    return;
                }

                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println(" Raw body nhận: " + body);

                JsonObject json = new JsonParser().parse(body).getAsJsonObject();
                System.out.println("JSON parse: " + json.toString());

                int amount = 0;
                if (json.has("transferAmount") && !json.get("transferAmount").isJsonNull()) {
                    amount = json.get("transferAmount").getAsInt();
                } else if (json.has("amount_in") && !json.get("amount_in").isJsonNull()) {
                    amount = json.get("amount_in").getAsInt();
                } else if (json.has("amount") && !json.get("amount").isJsonNull()) {
                    amount = json.get("amount").getAsInt();
                }

                String description = json.has("transaction_content")
                        ? json.get("transaction_content").getAsString()
                        : (json.has("content") ? json.get("content").getAsString()
                        : (json.has("description") ? json.get("description").getAsString() : ""));

                String transId = json.has("transaction_id")
                        ? json.get("transaction_id").getAsString()
                        : (json.has("referenceCode") ? json.get("referenceCode").getAsString()
                        : (json.has("id") ? json.get("id").getAsString() : "unknown"));
                Matcher m = Pattern.compile("(?i)(?:CHUYEN\\s*TIEN|NAP)\\s*\\+?\\s*(\\d+)").matcher(description);

                long playerId = -1;
                if (m.find()) {
                    playerId = Long.parseLong(m.group(1));
                    System.out.println("Tìm thấy playerId = " + playerId + " trong nội dung: " + description);
                } else {
                    System.out.println("️ Không tìm thấy playerId trong nội dung: " + description);
                    saveLog(transId, amount, description, false);
                    send(ex, 200, "Logged but no player id");
                    return;
                }
                System.out.println("➡️ playerId=" + playerId + ", amount=" + amount + ", desc=" + description);


                processTopup(playerId, amount, "sepay", transId, description);
                send(ex, 200, "OK");

            } catch (Exception e) {
                e.printStackTrace();
                try { send(ex, 500, "Server error"); } catch (Exception ignored) {}
            }
        }
    }
    private static void processTopup(long playerId, int amount, String provider, String transId, String desc) {
        int soTienCong = (int)(amount * HE_SO_SU_KIEN);

        try (Connection con = DBConnecter.getConnectionServer()) {
            // FIX: Update account trực tiếp bằng player ID, không dùng JOIN
            // Đảm bảo update luôn thành công dù player online hay offline
            PreparedStatement ps = con.prepareStatement(
                "UPDATE account SET cash = cash + ?, vnd = vnd + ?, danap = danap + ? "
                        + "WHERE id = (SELECT account_id FROM player WHERE id = ?)"
            );
            ps.setInt(1, soTienCong);
            ps.setInt(2, soTienCong);
            ps.setInt(3, amount);  // FIX: Cập nhật danap với amount gốc, không phải soTienCong
            ps.setLong(4, playerId);
            int updated = ps.executeUpdate();
            ps.close();

            if (updated > 0) {
                System.out.println("✓ Nạp thành công: playerId=" + playerId + ", +" + soTienCong + " (gốc " + amount + ")");
                saveLog(transId, amount, desc, true);
            } else {
                System.out.println("✗ Không tìm thấy player id=" + playerId);
                saveLog(transId, amount, desc, false);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Gửi thông báo cho player online
        Player pl = Client.gI().getPlayer(playerId);
        if (pl != null && pl.getSession() != null) {
            try {
                nro.server.CashAuditLog.logAdd(pl, soTienCong, "RECHARGE_HTTP", "Sepay TransID:" + transId + " Amount:" + amount + " HeSo:" + HE_SO_SU_KIEN);
                pl.getSession().cash += soTienCong;
                pl.getSession().vnd += soTienCong;
                pl.getSession().danap += amount;  // FIX: Sync với database
                pl.danap += amount;
            } catch (Exception ignored) {}
            Service.gI().sendThongBao(pl,
                "Bạn đã nạp " + amount + " VNĐ (nhận " + soTienCong + " VNĐ, X" + HE_SO_SU_KIEN + ")");
            Service.gI().sendMoney(pl);
        }
    }

    private static void saveLog(String transId, int amount, String desc, boolean success) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO recharge_log(trans_id, amount, description, success, time) VALUES (?,?,?,?,NOW())"
            );
            ps.setString(1, transId);
            ps.setInt(2, amount);
            ps.setString(3, desc);
            ps.setInt(4, success ? 1 : 0);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void send(HttpExchange ex, int code, String msg) throws Exception {
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }
}