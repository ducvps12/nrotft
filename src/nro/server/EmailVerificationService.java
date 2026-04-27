package nro.server;

import jdbc.DBConnecter;
import nro.player.Player;
import nro.services.Service;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;

/**
 * Xác minh email bằng OTP ngay trong game.
 * Không mở HTTP endpoint/link active để tránh lộ IP script và giảm rủi ro DDoS web/game.
 */
public class EmailVerificationService {

    private static final long OTP_TTL_MS = 10 * 60 * 1000L;
    private static final long RESEND_COOLDOWN_MS = 60 * 1000L;
    private static final Random RANDOM = new Random();

    public static boolean isVerified(Player player) {
        if (player == null || player.getSession() == null) return false;
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement("SELECT email_verified FROM account WHERE id = ?")) {
            ps.setInt(1, player.getSession().userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("email_verified") == 1;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static void requestOtp(Player player, String email) {
        if (player == null || player.getSession() == null) return;
        email = email == null ? "" : email.trim().toLowerCase();
        if (!email.matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$".toLowerCase())) {
            Service.gI().sendThongBao(player, "Email không hợp lệ.");
            return;
        }
        try (Connection con = DBConnecter.getConnectionServer()) {
            ensureColumns(con);
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT email_verified, email_verify_last_sent FROM account WHERE id = ?")) {
                ps.setInt(1, player.getSession().userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getInt("email_verified") == 1) {
                            Service.gI().sendThongBao(player, "Tài khoản đã xác minh email rồi.");
                            return;
                        }
                        long last = rs.getLong("email_verify_last_sent");
                        long wait = RESEND_COOLDOWN_MS - (System.currentTimeMillis() - last);
                        if (wait > 0 && !player.isAdmin()) {
                            Service.gI().sendThongBao(player, "Vui lòng đợi " + (wait / 1000) + " giây để gửi lại OTP.");
                            return;
                        }
                    }
                }
            }

            String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
            long now = System.currentTimeMillis();
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE account SET email = ?, email_verify_code = ?, email_verify_expire = ?, email_verify_last_sent = ? WHERE id = ?")) {
                ps.setString(1, email);
                ps.setString(2, otp);
                ps.setLong(3, now + OTP_TTL_MS);
                ps.setLong(4, now);
                ps.setInt(5, player.getSession().userId);
                ps.executeUpdate();
            }
            sendOtpEmail(email, player.name, otp);
            Service.gI().sendThongBao(player, "Đã gửi OTP đến email. Vào Hòm Thư -> Nhập OTP để xác nhận.");
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Không gửi được OTP: " + e.getMessage());
        }
    }

    public static void verifyOtp(Player player, String otp) {
        if (player == null || player.getSession() == null) return;
        otp = otp == null ? "" : otp.trim();
        try (Connection con = DBConnecter.getConnectionServer()) {
            ensureColumns(con);
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT email_verify_code, email_verify_expire FROM account WHERE id = ?")) {
                ps.setInt(1, player.getSession().userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;
                    String code = rs.getString("email_verify_code");
                    long expire = rs.getLong("email_verify_expire");
                    if (code == null || !code.equals(otp)) {
                        Service.gI().sendThongBao(player, "OTP không đúng.");
                        return;
                    }
                    if (System.currentTimeMillis() > expire) {
                        Service.gI().sendThongBao(player, "OTP đã hết hạn, hãy gửi lại mã mới.");
                        return;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE account SET email_verified = 1, email_verify_code = NULL, email_verify_expire = 0 WHERE id = ?")) {
                ps.setInt(1, player.getSession().userId);
                ps.executeUpdate();
            }
            Service.gI().sendThongBao(player, "Xác minh email thành công!");
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Lỗi xác minh OTP: " + e.getMessage());
        }
    }

    private static void sendOtpEmail(String to, String playerName, String otp) throws Exception {
        Properties cfg = new Properties();
        try (FileReader fr = new FileReader("data/config/notification_config.properties")) {
            cfg.load(fr);
        }
        if (!Boolean.parseBoolean(cfg.getProperty("smtp.enabled", "false"))) {
            throw new IllegalStateException("SMTP chưa bật trong notification_config.properties");
        }
        String host = cfg.getProperty("smtp.host", "smtp.gmail.com").trim();
        int port = Integer.parseInt(cfg.getProperty("smtp.port", "587").trim());
        String user = cfg.getProperty("smtp.email", "").trim();
        String pass = cfg.getProperty("smtp.password", "").trim();
        String subject = "Ma OTP xac minh tai khoan NRO";
        String body = "Xin chao " + playerName + ",\n\nMa OTP xac minh email cua ban la: " + otp
                + "\nMa co hieu luc 10 phut. Khong chia se ma nay cho bat ky ai.\n";
        sendSmtp(host, port, user, pass, to, subject, body);
    }

    private static void sendSmtp(String host, int port, String user, String pass, String to, String subject, String body) throws Exception {
        Socket plainSocket = new Socket(host, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(plainSocket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(plainSocket.getOutputStream(), StandardCharsets.UTF_8));
        read(in); cmd(in, out, "EHLO localhost");
        cmd(in, out, "STARTTLS");
        SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        Socket socket = sslFactory.createSocket(plainSocket, host, port, true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        cmd(in, out, "EHLO localhost");
        cmd(in, out, "AUTH LOGIN");
        cmd(in, out, Base64.getEncoder().encodeToString(user.getBytes(StandardCharsets.UTF_8)));
        cmd(in, out, Base64.getEncoder().encodeToString(pass.getBytes(StandardCharsets.UTF_8)));
        cmd(in, out, "MAIL FROM:<" + user + ">");
        cmd(in, out, "RCPT TO:<" + to + ">");
        cmd(in, out, "DATA");
        out.write("From: " + user + "\r\nTo: " + to + "\r\nSubject: " + subject + "\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n" + body + "\r\n.\r\n");
        out.flush(); read(in);
        cmd(in, out, "QUIT");
        socket.close();
    }

    private static void cmd(BufferedReader in, BufferedWriter out, String cmd) throws IOException {
        out.write(cmd + "\r\n"); out.flush(); read(in);
    }

    private static void read(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() < 4 || line.charAt(3) != '-') break;
        }
    }

    private static void ensureColumns(Connection con) {
        try {
            con.createStatement().executeUpdate("ALTER TABLE account ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0");
        } catch (Exception ignored) {}
        try {
            con.createStatement().executeUpdate("ALTER TABLE account ADD COLUMN email_verify_code VARCHAR(10) DEFAULT NULL");
        } catch (Exception ignored) {}
        try {
            con.createStatement().executeUpdate("ALTER TABLE account ADD COLUMN email_verify_expire BIGINT NOT NULL DEFAULT 0");
        } catch (Exception ignored) {}
        try {
            con.createStatement().executeUpdate("ALTER TABLE account ADD COLUMN email_verify_last_sent BIGINT NOT NULL DEFAULT 0");
        } catch (Exception ignored) {}
    }
}
