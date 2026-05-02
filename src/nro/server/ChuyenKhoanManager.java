package nro.server;

import com.google.gson.Gson;
import consts.Cmd;
import nro.server.Client;
import nro.services.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jdbc.DBConnecter;
import jdbc.daos.PlayerDAO;
import models.JsonResponse;
import models.Transaction;
import models.TransactionHistory;
import network.Message;
import nro.player.Player;
import utils.TimeUtil;
import utils.Util;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChuyenKhoanManager {

    private static final String ACB_HISTORY_API = "https://api.sieuthicode.net/historyapiacb/ec4f8aeb9d87bc0ffa48f709365313d1";
    // Pattern BẮT BUỘC phải có prefix: "chuyen tien", "naptien", "mtdgame", "id" + số player ID
    // QUAN TRỌNG: KHÔNG dùng "NAP" đơn lẻ vì match quá rộng (NAPAS, nội dung bank tự sinh)
    private static final Pattern WEBSITE_TRANSFER_PATTERN = Pattern.compile(
        "(?i)(?:CHUYEN\\s*TIEN|NAP\\s*TIEN|NAPTIEN|MTDGAME|\\bID\\b)\\s*[:#-]?\\s*(\\d{1,12})");
    // Pattern để match username (chữ) sau "chuyen tien" - ví dụ: "chuyen tien admin", "chuyen tien nhimoon"
    private static final Pattern USERNAME_TRANSFER_PATTERN = Pattern.compile(
        "(?i)(?:CHUYEN\\s*TIEN|NAP\\s*TIEN|NAPTIEN|MTDGAME)\\s+([a-zA-Z][a-zA-Z0-9_]{2,30})");
    // ID admin - KHÔNG BAO GIỜ cộng tiền tự động cho admin từ bank pattern matching
    private static final long ADMIN_PLAYER_ID = 2;

    public static String buildTransferDescription(Player player) {
        String username = getAccountUsername(player != null && player.getSession() != null ? player.getSession().userId : -1);
        if (username == null || username.isBlank()) {
            username = player != null && player.name != null ? player.name : String.valueOf(player != null ? player.id : 0);
        }
        return "chuyen tien " + username.trim();
    }

    public static String buildVietQrUrl(Transaction transaction) {
        if (transaction == null) {
            return "";
        }
        String addInfo = URLEncoder.encode(transaction.description == null ? "" : transaction.description, StandardCharsets.UTF_8);
        return "https://img.vietqr.io/image/ACB-24488671-compact2.png?amount="
                + Math.round(transaction.amount)
                + "&addInfo=" + addInfo;
    }

    private static String getAccountUsername(int accountId) {
        if (accountId <= 0) {
            return null;
        }
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement("SELECT username FROM account WHERE id = ? LIMIT 1")) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void InsertTransaction(long playerId, long amount, String description) {
        InsertTransactionAndGetId(playerId, amount, description);
    }

    public static long InsertTransactionAndGetId(long playerId, long amount, String description) {
        String sql = """
        INSERT INTO transaction_banking (player_id, amount, description, status, is_recieve, last_time_check, created_date)
        VALUES (?, ?, ?, 0, 0, NULL, NOW());
    """;

        try (Connection con = DBConnecter.getConnectionServer(); 
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, playerId);
            ps.setLong(2, amount);
            ps.setString(3, description);

            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void ShowTransaction(Player player) {
        List<Transaction> list = GetTransaction((int) player.id);
        Message msg = new Message(Cmd.TOP);
        try {
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Lịch sử giao dịch");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Transaction top = list.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(Math.toIntExact(top.id));
                msg.writer().writeShort(player.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(player.getBody());
                msg.writer().writeShort(player.getLeg());
                msg.writer().writeUTF(player.name);

                msg.writer().writeUTF(Util.formatCurrency(top.amount));

                msg.writer().writeUTF("Nội dung: " + top.description + "\n"
                        + "Trạng thái giao dịch: " + (top.status ? "Đã thanh toán" : "Chờ thanh toán") + "\n"
                        + "Trạng thái nhận quà " + (top.isReceive ? "Đã nhận quà" : "Chờ nhận quà") + "\n"
                        + "Ngày giao dịch " + (Util.formatLocalDateTime(top.createdDate)) + "\n");
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Transaction> GetTransactionAuto() {
        List<Transaction> result = new ArrayList<>();

        String sql = """
        SELECT * FROM transaction_banking
        WHERE status = FALSE
        AND created_date >= NOW() - INTERVAL 60 MINUTE
        ORDER BY created_date DESC;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.id = rs.getLong("id");
                transaction.playerId = rs.getLong("player_id");
                transaction.amount = rs.getLong("amount");
                transaction.description = rs.getString("description");
                transaction.status = rs.getBoolean("status");
                transaction.isReceive = rs.getBoolean("is_recieve");

                Timestamp lastCheck = rs.getTimestamp("last_time_check");
                if (lastCheck != null) {
                    transaction.lastTimeCheck = lastCheck.toLocalDateTime();
                }

                transaction.createdDate = rs.getTimestamp("created_date").toLocalDateTime();

                result.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static List<Transaction> GetTransactionDoneAuto() {
        List<Transaction> result = new ArrayList<>();

        String sql = """
        SELECT * FROM transaction_banking
        WHERE status = TRUE
        AND is_recieve = FALSE
        AND created_date >= NOW() - INTERVAL 60 MINUTE
        ORDER BY created_date DESC;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.id = rs.getLong("id");
                transaction.playerId = rs.getLong("player_id");
                transaction.amount = rs.getLong("amount");
                transaction.description = rs.getString("description");
                transaction.status = rs.getBoolean("status");
                transaction.isReceive = rs.getBoolean("is_recieve");

                Timestamp lastCheck = rs.getTimestamp("last_time_check");
                if (lastCheck != null) {
                    transaction.lastTimeCheck = lastCheck.toLocalDateTime();
                }

                transaction.createdDate = rs.getTimestamp("created_date").toLocalDateTime();

                result.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static List<Transaction> GetTransaction(int player_id) {
        List<Transaction> result = new ArrayList<>();

        String sql = """
        SELECT * FROM transaction_banking
        WHERE player_id = ?
        ORDER BY created_date DESC;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, player_id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction();

                    transaction.id = rs.getLong("id");
                    transaction.playerId = rs.getLong("player_id");
                    transaction.amount = rs.getLong("amount");
                    transaction.description = rs.getString("description");
                    transaction.status = rs.getBoolean("status");
                    transaction.isReceive = rs.getBoolean("is_recieve");

                    Timestamp lastCheck = rs.getTimestamp("last_time_check");
                    if (lastCheck != null) {
                        transaction.lastTimeCheck = lastCheck.toLocalDateTime();
                    }

                    transaction.createdDate = rs.getTimestamp("created_date").toLocalDateTime();

                    result.add(transaction);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Transaction GetTransactionById(long player_id, int id) {
        Transaction result = null;

        String sql = """
        SELECT * FROM transaction_banking
        WHERE id = ? AND player_id = ?
        ORDER BY created_date DESC
        LIMIT 1;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setLong(2, player_id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result = new Transaction();

                    result.id = rs.getLong("id");
                    result.playerId = rs.getLong("player_id");
                    result.amount = rs.getLong("amount");
                    result.description = rs.getString("description");
                    result.status = rs.getBoolean("status");
                    result.isReceive = rs.getBoolean("is_recieve");

                    Timestamp lastCheck = rs.getTimestamp("last_time_check");
                    if (lastCheck != null) {
                        result.lastTimeCheck = lastCheck.toLocalDateTime();
                    }

                    result.createdDate = rs.getTimestamp("created_date").toLocalDateTime();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Transaction GetTransactionLast(long player_id) {
        Transaction result = null;

        String sql = """
        SELECT * FROM transaction_banking
        WHERE player_id = ?
        ORDER BY id DESC
        LIMIT 1;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, player_id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result = new Transaction();

                    result.id = rs.getLong("id");
                    result.playerId = rs.getLong("player_id");
                    result.amount = rs.getLong("amount");
                    result.description = rs.getString("description");
                    result.status = rs.getBoolean("status");
                    result.isReceive = rs.getBoolean("is_recieve");

                    Timestamp lastCheck = rs.getTimestamp("last_time_check");
                    if (lastCheck != null) {
                        result.lastTimeCheck = lastCheck.toLocalDateTime();
                    }

                    result.createdDate = rs.getTimestamp("created_date").toLocalDateTime();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static LocalDateTime GetLastimeCreateTransaction(Player player) {
        LocalDateTime result = null;

        String sql = """
        SELECT created_date 
        FROM transaction_banking 
        WHERE player_id = ? 
        ORDER BY created_date DESC 
        LIMIT 1
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, player.id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp time = rs.getTimestamp("created_date");
                    if (time != null) {
                        result = time.toLocalDateTime();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static LocalDateTime GetLastimeCheckTransaction(Player player) {
        LocalDateTime result = null;

        String sql = """
        SELECT MAX(last_time_check) AS last_time_check 
        FROM transaction_banking 
        WHERE player_id = ?
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, player.id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp time = rs.getTimestamp("last_time_check");
                    if (time != null) {
                        result = time.toLocalDateTime();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void UpdateLastTimeCheck(long playerId, int transactionId) {
        String sql = "UPDATE transaction_banking SET last_time_check = NOW() WHERE id = ? AND player_id = ?";

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, transactionId);
            ps.setLong(2, playerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void UpdateGift(long playerId, long transactionId) {
        String sql = "UPDATE transaction_banking SET status = 1, is_recieve = 1 WHERE id = ? AND player_id = ?";

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, transactionId);
            ps.setLong(2, playerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void UpdateDoneNap(long playerId, long transactionId) {
        String sql = "UPDATE transaction_banking SET status = 1 WHERE id = ? AND player_id = ?";

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, transactionId);
            ps.setLong(2, playerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void UpdateDoneRecieve(long playerId, long transactionId) {
        String sql = "UPDATE transaction_banking SET is_recieve = 1 WHERE id = ? AND player_id = ?";

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, transactionId);
            ps.setLong(2, playerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void HandleTransactionAuto() {
        List<Transaction> transactions = GetTransactionAuto();

        if (!transactions.isEmpty()) {
            String history = GetTransactionOnline(ACB_HISTORY_API);
            JsonResponse response = parseApiResponse(history);

            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                for (TransactionHistory transactionHistory : response.getData()) {
                    for (Transaction transaction : transactions) {
                        if (Double.parseDouble(transactionHistory.getCreditAmount()) == transaction.amount && Util.containsSubstring(transactionHistory.getDescription(), transaction.description)) {
                            UpdateDoneNap(transaction.playerId, transaction.id);
                            break;
                        }
                    }
                }
            }
        }
    }

    public static void HandleTransaction(Player player, int transactionId) {
        boolean canCheck = false;
        LocalDateTime lastTimeCheck = GetLastimeCheckTransaction(player);
        Transaction transaction = null;
        long timeDifference = 0;

        if (lastTimeCheck == null) {
            canCheck = true;
            return;
        } else {
            LocalDateTime now = LocalDateTime.now();
            timeDifference = TimeUtil.calculateTimeDifferenceInSeconds(lastTimeCheck, now);

            if (timeDifference > 10) {
                canCheck = true;
            }

            transaction = GetTransactionById(player.id, transactionId);

            if (transaction != null && (transaction.status || transaction.isReceive)) {
                Service.gI().sendThongBao(player, "Bạn thanh toán giao dịch này và đã nhận được ruby!");
                return;
            }
        }

        if (transaction == null) {
            return;
        }

        if (canCheck) {
            transaction = GetTransactionById(player.id, transactionId);
            String history = GetTransactionOnline(ACB_HISTORY_API);

            JsonResponse response = parseApiResponse(history);

            UpdateLastTimeCheck(player.id, transactionId);

            if (response != null && response.getData() != null && response.getData().size() > 0) {
                for (TransactionHistory transactionHistory : response.getData()) {
                    if (Double.parseDouble(transactionHistory.getCreditAmount()) == transaction.amount && Util.containsSubstring(transactionHistory.getDescription(), transaction.description)) {
                        double coin = transaction.amount;

                        // Tính toán bonus
                        int bonus = calculateBonus((int) coin);

                        // ATM nạp vào số dư VNĐ trong game theo tỉ lệ 1:1 + bonus.
                        int totalAmount = (int) coin + bonus;

                        // Cập nhật tài khoản người chơi
                        PlayerDAO.addCash(player, totalAmount, "BANK_ATM", "TransactionID:" + transactionId + " Amount:" + transaction.amount + " Bonus:" + bonus);
                        // addCash đã cập nhật cash/vnd/danap trong DB, chỉ sync lại session online.
                        player.getSession().cash += totalAmount;
                        player.getSession().danap += totalAmount;

                        // In ra thông tin giao dịch
                        System.out.println("ADD COIN: " + totalAmount + " + Bonus: " + bonus);

                        // Gửi thông báo Telegram về giao dịch nạp tiền
                        try {
                            NotificationService.gI().notifyRecharge(
                                player.name != null ? player.name : "ID:" + player.id,
                                (long) transaction.amount, "ATM Bank (Manual)");
                        } catch (Exception ignored) {}

                        // Thông báo cho người chơi về số tiền nhận được (bao gồm cả bonus)
                        Service.gI().sendThongBao(player, "Bạn nhận được " + Util.formatCurrency(totalAmount) + " VNĐ vào số dư");

                        // Cập nhật các phần thưởng khác nếu có
                        UpdateGift(player.id, (long) transactionId);
                        return;
                    }
                }
            }
            Service.gI().sendThongBao(player, "Tài khoản của admin chưa nhận được tiền hoặc bạn chuyển khoản sai nội dung!");
        } else {
            Service.gI().sendThongBao(player, "Bạn cần đợi " + (10 - timeDifference) + " giây nữa để được check giao dịch");
        }
    }

    public static void HandleTransactionAddMoneyAuto() {
        List<Transaction> transactions = GetTransactionDoneAuto();

        if (!transactions.isEmpty()) {
            for (Transaction transaction : transactions) {
                creditTransactionToPlayer(transaction, "ATM Bank (Auto)", "BANK_AUTO");
            }
        }
    }

    public static String HandleWebsiteAtmCheckByAdmin(Player admin) {
        if (admin == null || !admin.isAdmin()) {
            return "Bạn không có quyền sử dụng chức năng này.";
        }

        String history = GetTransactionOnline(ACB_HISTORY_API);
        JsonResponse response = parseApiResponse(history);
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return "Không lấy được lịch sử ATM hoặc lịch sử đang rỗng.";
        }

        int checked = 0;
        int matched = 0;
        int credited = 0;
        int skipped = 0;
        int alreadyDone = 0;
        long totalCreditedAmount = 0;
        StringBuilder detail = new StringBuilder();
        StringBuilder warning = new StringBuilder();
        Set<String> processedInBatch = new HashSet<>();
        Map<Long, Integer> newBillCountByPlayer = new HashMap<>();
        Map<Long, Long> newAmountByPlayer = new HashMap<>();

        for (TransactionHistory bankTx : response.getData()) {
            checked++;
            String description = bankTx.getDescription();
            long playerId = extractWebsitePlayerId(description);
            long amount = parseAmount(bankTx.getCreditAmount());
            if (playerId <= 0 || amount <= 0) {
                skipped++;
                continue;
            }

            // AN TOÀN: KHÔNG BAO GIỜ cộng tiền tự động cho admin
            if (playerId == ADMIN_PLAYER_ID) {
                System.out.println("[ATM-SKIP] Bỏ qua admin ID " + playerId + " | Nội dung: " + description + " | Số tiền: " + amount);
                skipped++;
                continue;
            }

            matched++;

            // Dedup trong cùng 1 lần check (cùng player + amount + nội dung bank)
            String uniqueKey = playerId + "|" + amount + "|" + normalizeDescription(description);
            if (!processedInBatch.add(uniqueKey)) {
                skipped++;
                continue;
            }

            // Tìm hoặc tạo giao dịch - CÓ KIỂM TRA TRÙNG LẶP
            Transaction tx = findOrCreateWebsiteTransaction(playerId, amount, description);
            if (tx == null) {
                // Transaction đã được xử lý trước đó → skip, không tạo mới
                alreadyDone++;
                continue;
            }
            if (tx.isReceive) {
                alreadyDone++;
                continue;
            }

            // Đếm bill MỚI (chưa xử lý) cho mỗi player
            newBillCountByPlayer.merge(playerId, 1, Integer::sum);
            newAmountByPlayer.merge(playerId, amount, Long::sum);

            UpdateDoneNap(tx.playerId, tx.id);
            if (creditTransactionToPlayer(tx, "ATM Bank (Admin Panel)", "BANK_ATM_ADMIN")) {
                credited++;
                totalCreditedAmount += (long) tx.amount;
                if (detail.length() < 900) {
                    detail.append("\n+ ").append(getPlayerDisplayName(tx.playerId))
                            .append(" | Bill: ").append(Util.formatCurrency((long) tx.amount)).append(" VNĐ")
                            .append(" | Cộng: ").append(Util.formatCurrency((long) tx.amount + calculateBonus((int) tx.amount))).append(" VNĐ game");
                }
            } else {
                skipped++;
            }
        }

        // Chỉ cảnh báo các player có >= 2 bill MỚI (chưa xử lý)
        for (Map.Entry<Long, Integer> entry : newBillCountByPlayer.entrySet()) {
            if (entry.getValue() >= 2) {
                long playerId = entry.getKey();
                long total = newAmountByPlayer.getOrDefault(playerId, 0L);
                warning.append("\n! ").append(getPlayerDisplayName(playerId))
                        .append(" có ").append(entry.getValue()).append(" bill MỚI")
                        .append(" | Tổng: ").append(Util.formatCurrency(total)).append(" VNĐ");
            }
        }

        return "Đã check ATM.\n"
                + "Lịch sử đọc: " + checked + "\n"
                + "Nhận diện được player: " + matched + "\n"
                + "Đã cộng tiền: " + credited + " bill\n"
                + "Tổng đã cộng: " + Util.formatCurrency(totalCreditedAmount) + " VNĐ\n"
                + "Đã xử lý trước: " + alreadyDone + "\n"
                + "Bỏ qua: " + skipped
                + (warning.length() > 0 ? "\n\nCẢNH BÁO BILL MỚI LẶP:" + warning : "")
                + (detail.length() > 0 ? "\n\nCHI TIẾT ĐÃ CỘNG:" + detail : "")
                + (credited == 0 && alreadyDone > 0 ? "\n\nTất cả giao dịch đã được xử lý trước đó." : "")
                + (credited == 0 && alreadyDone == 0 && matched > 0 ? "\n\nGợi ý: bill khớp nhưng không cộng được. Kiểm tra player có tồn tại trong DB không." : "");
    }

    /**
     * Tìm hoặc tạo transaction cho giao dịch từ website/bank API.
     * QUAN TRỌNG: Kiểm tra giao dịch ĐÃ XỬ LÝ trước khi tạo mới để tránh cộng tiền trùng.
     */
    private static Transaction findOrCreateWebsiteTransaction(long playerId, long amount, String description) {
        // 1. Kiểm tra xem giao dịch này đã được xử lý chưa (is_recieve = 1)
        //    Nếu đã xử lý → trả null, KHÔNG tạo mới (tránh cộng tiền 2 lần)
        if (isTransactionAlreadyProcessed(playerId, amount)) {
            return null;
        }

        // 2. Tìm giao dịch đang chờ (is_recieve = 0)
        Transaction existed = findWebsiteTransaction(playerId, amount);
        if (existed != null) {
            return existed;
        }

        // 3. Tạo giao dịch mới
        InsertTransaction(playerId, amount, normalizeDescription(description));
        return findWebsiteTransaction(playerId, amount);
    }

    /**
     * Kiểm tra giao dịch đã được cộng tiền chưa (is_recieve = 1).
     * Dùng để tránh tạo transaction trùng lặp khi admin check thủ công nhiều lần.
     */
    private static boolean isTransactionAlreadyProcessed(long playerId, long amount) {
        String sql = """
        SELECT id FROM transaction_banking
        WHERE player_id = ? AND amount = ?
        AND is_recieve = 1
        AND created_date >= NOW() - INTERVAL 24 HOUR
        ORDER BY id DESC
        LIMIT 1;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            ps.setLong(2, amount);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true = đã xử lý rồi
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static Transaction findWebsiteTransaction(long playerId, long amount) {
        String sql = """
        SELECT * FROM transaction_banking
        WHERE player_id = ? AND amount = ?
        AND is_recieve = 0
        AND created_date >= NOW() - INTERVAL 7 DAY
        ORDER BY id DESC
        LIMIT 1;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            ps.setLong(2, amount);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Transaction transaction = new Transaction();
                    transaction.id = rs.getLong("id");
                    transaction.playerId = rs.getLong("player_id");
                    transaction.amount = rs.getLong("amount");
                    transaction.description = rs.getString("description");
                    transaction.status = rs.getBoolean("status");
                    transaction.isReceive = rs.getBoolean("is_recieve");
                    Timestamp lastCheck = rs.getTimestamp("last_time_check");
                    if (lastCheck != null) {
                        transaction.lastTimeCheck = lastCheck.toLocalDateTime();
                    }
                    transaction.createdDate = rs.getTimestamp("created_date").toLocalDateTime();
                    return transaction;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean creditTransactionToPlayer(Transaction transaction, String notifySource, String auditSource) {
        Player player = Client.gI().getPlayer(transaction.getPlayerId());
        if (player != null) {
            addMoneyToPlayer(player, transaction.amount, transaction.id, notifySource, auditSource);
            UpdateDoneRecieve(player.id, transaction.id);
            return true;
        }
        Integer accountId = getAccountIdByPlayerId(transaction.playerId);
        if (accountId == null) {
            return false;
        }
        int bonus = calculateBonus((int) transaction.amount);
        int totalAmount = (int) transaction.amount + bonus;
        if (PlayerDAO.addcash(accountId, totalAmount, auditSource,
                "TransactionID:" + transaction.id + " PlayerID:" + transaction.playerId + " Amount:" + transaction.amount + " Bonus:" + bonus)) {
            try {
                NotificationService.gI().notifyRecharge("ID:" + transaction.playerId, (long) transaction.amount, notifySource + " Offline");
            } catch (Exception ignored) {
            }
            UpdateDoneRecieve(transaction.playerId, transaction.id);
            return true;
        }
        return false;
    }

    private static void addMoneyToPlayer(Player player, double amount, long transactionId, String notifySource, String auditSource) {
        int bonus = calculateBonus((int) amount);
        int totalAmount = (int) amount + bonus;

        PlayerDAO.addCash(player, totalAmount, auditSource, "TransactionID:" + transactionId + " Amount:" + amount + " Bonus:" + bonus);

        player.getSession().cash += totalAmount;
        player.getSession().danap += totalAmount;

        System.out.println("ADD COIN: " + totalAmount + " + Bonus: " + bonus);
        try {
            NotificationService.gI().notifyRecharge(player.name != null ? player.name : "ID:" + player.id, (long) amount, notifySource);
        } catch (Exception ignored) {
        }
        Service.gI().sendThongBao(player, "Bạn nhận được " + Util.formatCurrency(totalAmount) + " VNĐ vào số dư");
    }

    private static Integer getAccountIdByPlayerId(long playerId) {
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement("SELECT account_id FROM player WHERE id = ? LIMIT 1")) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("account_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getPlayerDisplayName(long playerId) {
        Player online = Client.gI().getPlayer(playerId);
        if (online != null && online.name != null && !online.name.isBlank()) {
            return online.name + " (ID " + playerId + ")";
        }
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement("SELECT name FROM player WHERE id = ? LIMIT 1")) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    if (name != null && !name.isBlank()) {
                        return name + " (ID " + playerId + ")";
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "UNKNOWN (ID " + playerId + ")";
    }

    private static long extractWebsitePlayerId(String description) {
        if (description == null) {
            return -1;
        }
        
        // Loại bỏ các pattern ngân hàng tự sinh để tránh match nhầm
        // Ví dụ: "NAPAS", mã giao dịch ngân hàng chứa số dài, etc.
        String upperDesc = description.toUpperCase();
        
        // QUAN TRỌNG: Bỏ qua nội dung bank KHÔNG chứa keyword game hợp lệ
        boolean hasGameKeyword = upperDesc.contains("CHUYEN TIEN") 
                || upperDesc.contains("CHUYENTIEN")
                || upperDesc.contains("NAP TIEN")
                || upperDesc.contains("NAPTIEN")
                || upperDesc.contains("MTDGAME")
                || (upperDesc.contains(" ID ") || upperDesc.startsWith("ID ") || upperDesc.startsWith("ID:"));
        if (!hasGameKeyword) {
            return -1; // Không có keyword game → skip hoàn toàn
        }
        
        // 1. Thử match player ID bằng số trước
        Matcher matcher = WEBSITE_TRANSFER_PATTERN.matcher(description);
        long lastValidId = -1;
        while (matcher.find()) {
            long candidateId = Long.parseLong(matcher.group(1));
            // Bỏ qua số quá lớn (có thể là mã giao dịch ngân hàng, số tài khoản)
            if (candidateId > 0 && candidateId < 100000 && candidateId != ADMIN_PLAYER_ID && playerExists(candidateId)) {
                lastValidId = candidateId;
            }
        }
        if (lastValidId > 0) {
            System.out.println("[ATM-MATCH] Matched player ID " + lastValidId + " from: " + description);
            return lastValidId;
        }
        
        // 2. Fallback: thử match username (chữ) từ nội dung "chuyen tien <username>"
        Matcher usernameMatcher = USERNAME_TRANSFER_PATTERN.matcher(description);
        while (usernameMatcher.find()) {
            String candidateUsername = usernameMatcher.group(1).trim();
            // Bỏ qua các từ khóa ngân hàng phổ biến để tránh match nhầm
            if (candidateUsername.equalsIgnoreCase("GD")
                    || candidateUsername.equalsIgnoreCase("MBVCB")
                    || candidateUsername.equalsIgnoreCase("FT")
                    || candidateUsername.equalsIgnoreCase("admin")
                    || candidateUsername.length() <= 2) {
                continue;
            }
            long playerIdFromUsername = getPlayerIdByAccountUsername(candidateUsername);
            if (playerIdFromUsername > 0 && playerIdFromUsername != ADMIN_PLAYER_ID) {
                System.out.println("[ATM-MATCH] Matched username '" + candidateUsername + "' → player ID " + playerIdFromUsername + " from: " + description);
                return playerIdFromUsername;
            }
            // Thử tìm bằng player name nếu không tìm thấy bằng account username
            playerIdFromUsername = getPlayerIdByPlayerName(candidateUsername);
            if (playerIdFromUsername > 0 && playerIdFromUsername != ADMIN_PLAYER_ID) {
                System.out.println("[ATM-MATCH] Matched player name '" + candidateUsername + "' → player ID " + playerIdFromUsername + " from: " + description);
                return playerIdFromUsername;
            }
        }
        
        return -1;
    }

    /**
     * Tìm player ID từ account username.
     * Luồng: account.username → account.id → player.account_id → player.id
     */
    private static long getPlayerIdByAccountUsername(String username) {
        if (username == null || username.isBlank()) {
            return -1;
        }
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement(
                    "SELECT p.id FROM player p JOIN account a ON p.account_id = a.id WHERE LOWER(a.username) = LOWER(?) ORDER BY p.id ASC LIMIT 1")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Tìm player ID từ player name (tên nhân vật).
     */
    private static long getPlayerIdByPlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return -1;
        }
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement(
                    "SELECT id FROM player WHERE LOWER(name) = LOWER(?) LIMIT 1")) {
            ps.setString(1, playerName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static boolean isSameAmount(TransactionHistory transactionHistory, double amount) {
        return parseAmount(transactionHistory.getCreditAmount()) == amount;
    }

    private static long parseAmount(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Math.round(Double.parseDouble(value.replace(",", "").trim()));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private static boolean playerExists(long playerId) {
        if (Client.gI().getPlayer(playerId) != null) {
            return true;
        }
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement("SELECT id FROM player WHERE id = ? LIMIT 1")) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

// Hàm tính toán phần thưởng dựa trên số tiền nạp
    private static int calculateBonus(int amount) {
        int bonus = 0;
        // Xử lý bonus theo tỷ lệ phần trăm cho các mức nạp tiền
        if (amount >= 20000 && amount < 50000) {
            bonus = (int) (amount * 0.02); // Nạp 20k đến dưới 50k, nhận thêm 2%
        } else if (amount >= 50000 && amount < 100000) {
            bonus = (int) (amount * 0.05); // Nạp 50k đến dưới 100k, nhận thêm 5%
        } else if (amount >= 100000 && amount < 200000) {
            bonus = (int) (amount * 0.07); // Nạp 100k đến dưới 200k, nhận thêm 7%
        } else if (amount >= 200000 && amount < 500000) {
            bonus = (int) (amount * 0.10); // Nạp 200k đến dưới 500k, nhận thêm 10%
        } else if (amount >= 500000 && amount < 1000000) {
            bonus = (int) (amount * 0.15); // Nạp 500k đến dưới 1 triệu, nhận thêm 15%
        } else if (amount >= 1000000 && amount < 2000000) {
            bonus = (int) (amount * 0.20); // Nạp 1 triệu đến dưới 2 triệu, nhận thêm 20%
        } else if (amount >= 2000000 && amount < 5000000) {
            bonus = (int) (amount * 0.25); // Nạp 2 triệu đến dưới 5 triệu, nhận thêm 25%
        } else if (amount >= 5000000 && amount < 10000000) {
            bonus = (int) (amount * 0.30); // Nạp 5 triệu đến dưới 10 triệu, nhận thêm 30%
        } else if (amount >= 10000000) {
            bonus = (int) (amount * 0.35); // Nạp từ 10 triệu trở lên, nhận thêm 35%
        }
        return bonus;
    }

    private static String GetTransactionOnline(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static JsonResponse parseApiResponse(String apiResponse) {
        Gson gson = new Gson();
        return gson.fromJson(apiResponse, JsonResponse.class);
    }
}
