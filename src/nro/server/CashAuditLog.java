package nro.server;

import jdbc.DBConnecter;
import nro.player.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Hệ thống truy vết mọi thay đổi VND/Cash của người chơi.
 * Ghi log vào bảng cash_audit_log và console.
 * 
 * Các loại nguồn (source):
 * - BANK_ATM: Nạp qua chuyển khoản ngân hàng (ATM/QR)
 * - BANK_AUTO: Hệ thống tự động xác nhận giao dịch bank
 * - ADMIN_BUFF: Admin cộng tiền thủ công
 * - ADMIN_PANEL: Admin cộng qua Server Panel UI
 * - ADMIN_HTTP: Admin cộng qua HTTP API
 * - DOI_THOI_VANG: Đổi VND sang thỏi vàng
 * - DOI_NGOC_XANH: Đổi VND sang ngọc xanh
 * - DOI_NGOC_HONG: Đổi VND sang hồng ngọc
 * - MUA_DE: Mua đệ tử
 * - DOI_SKILL: Đổi skill đệ
 * - MO_THANH_VIEN: Mở thành viên
 * - XOA_DE: Xóa đệ tử
 * - SANTA_QUAY: Quay Santa/sự kiện
 * - TORI_BOT: Mua từ ToriBot
 * - RECHARGE_HTTP: Nạp qua RechargeHttp (cổng cũ)
 * - UNKNOWN: Không xác định nguồn
 */
public class CashAuditLog {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Ghi log thay đổi VND
     * @param playerId ID người chơi (account_id)
     * @param playerName Tên nhân vật
     * @param amount Số tiền thay đổi (dương = cộng, âm = trừ)
     * @param balanceBefore Số dư trước
     * @param balanceAfter Số dư sau
     * @param source Nguồn giao dịch (BANK_ATM, ADMIN_BUFF, DOI_THOI_VANG, ...)
     * @param detail Chi tiết bổ sung
     */
    public static void log(int playerId, String playerName, long amount, long balanceBefore, long balanceAfter, String source, String detail) {
        String time = LocalDateTime.now().format(FORMATTER);

        // Log ra console với màu
        String direction = amount > 0 ? "+" : "";
        System.out.println("[CASH_AUDIT] " + time
                + " | Player: " + playerName + " (ID:" + playerId + ")"
                + " | " + direction + amount + " VND"
                + " | Before: " + balanceBefore + " → After: " + balanceAfter
                + " | Source: " + source
                + " | Detail: " + detail);

        // Ghi vào DB (async để không block game)
        Thread.startVirtualThread(() -> {
            try {
                insertToDb(playerId, playerName, amount, balanceBefore, balanceAfter, source, detail);
            } catch (Exception e) {
                System.err.println("[CASH_AUDIT] DB Error: " + e.getMessage());
            }
        });
    }

    /**
     * Log khi cộng tiền
     */
    public static void logAdd(Player player, int amount, String source, String detail) {
        if (player == null || player.getSession() == null) return;
        long before = player.getSession().cash;
        long after = before + amount;
        log(player.getSession().userId, player.name, amount, before, after, source, detail);
    }

    /**
     * Log khi cộng tiền (dùng accountId khi không có Player object)
     */
    public static void logAdd(int accountId, String playerName, int amount, String source, String detail) {
        log(accountId, playerName != null ? playerName : "unknown", amount, -1, -1, source, detail);
    }

    /**
     * Log khi trừ tiền
     */
    public static void logSub(Player player, int amount, String source, String detail) {
        if (player == null || player.getSession() == null) return;
        long before = player.getSession().cash;
        long after = before - amount;
        log(player.getSession().userId, player.name, -amount, before, after, source, detail);
    }

    private static void insertToDb(int playerId, String playerName, long amount, long balanceBefore, long balanceAfter, String source, String detail) {
        String sql = """
            INSERT INTO cash_audit_log (account_id, player_name, amount, balance_before, balance_after, source, detail, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
        """;

        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setString(2, playerName);
            ps.setLong(3, amount);
            ps.setLong(4, balanceBefore);
            ps.setLong(5, balanceAfter);
            ps.setString(6, source);
            ps.setString(7, detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Bảng chưa tồn tại thì tự tạo
            if (e.getMessage().contains("doesn't exist")) {
                createTable();
                // Retry
                try (Connection con = DBConnecter.getConnectionServer();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, playerId);
                    ps.setString(2, playerName);
                    ps.setLong(3, amount);
                    ps.setLong(4, balanceBefore);
                    ps.setLong(5, balanceAfter);
                    ps.setString(6, source);
                    ps.setString(7, detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    System.err.println("[CASH_AUDIT] Retry failed: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Tự động tạo bảng nếu chưa có
     */
    public static void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS cash_audit_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                account_id INT NOT NULL,
                player_name VARCHAR(50),
                amount BIGINT NOT NULL COMMENT 'Số tiền thay đổi (+ cộng, - trừ)',
                balance_before BIGINT DEFAULT -1 COMMENT 'Số dư trước giao dịch',
                balance_after BIGINT DEFAULT -1 COMMENT 'Số dư sau giao dịch',
                source VARCHAR(50) NOT NULL COMMENT 'Nguồn: BANK_ATM, ADMIN_BUFF, DOI_THOI_VANG...',
                detail VARCHAR(500) COMMENT 'Chi tiết bổ sung',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_account (account_id),
                INDEX idx_source (source),
                INDEX idx_created (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Truy vết mọi thay đổi VND/Cash'
        """;

        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
            System.out.println("[CASH_AUDIT] Table cash_audit_log created successfully!");
        } catch (SQLException e) {
            System.err.println("[CASH_AUDIT] Create table error: " + e.getMessage());
        }
    }
}
