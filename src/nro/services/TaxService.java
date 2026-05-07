package nro.services;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║        HỆ THỐNG THUẾ VŨ TRỤ MTDGame — TaxService           ║
 * ║  Dựa theo Luật thuế TNCN & thuế TNDN Việt Nam 2024         ║
 * ║  Áp dụng biểu thuế lũy tiến từng phần                      ║
 * ║  Kỳ thuế: 1 ngày — hạn nộp 12h trưa hoặc 18h30            ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * BIỂU THUẾ LŨY TIẾN (quy đổi ra vàng):
 *   Bậc 1:  0 – 10M vàng        →  5%
 *   Bậc 2:  10M – 100M          → 10%
 *   Bậc 3:  100M – 1B           → 15%
 *   Bậc 4:  1B – 10B            → 20%
 *   Bậc 5:  10B – 100B          → 25%
 *   Bậc 6:  100B – 1T           → 30%
 *   Bậc 7:  trên 1T             → 35%
 *
 * QUY ĐỔI TÀI SẢN → VÀNG:
 *   1 Thỏi Vàng     = 50.000.000 vàng (50M)
 *   1 Ngọc Xanh     = 10.000 vàng
 *   1 Hồng Ngọc     = 50.000 vàng
 *   1 Xu Ngọc Rồng  = 1.000.000 vàng (1M)
 *
 * DEBUFF KHI TRỐN THUẾ (escalating):
 *   Kỳ 1 trốn: -10% sức đánh
 *   Kỳ 2 trốn: -25% sức đánh, -10% HP
 *   Kỳ 3 trốn: -40% sức đánh, -25% HP, không nhặt đồ
 *   Kỳ 4+ trốn: -50% sức đánh, -40% HP, không nhặt đồ, truy thu x2
 *
 * GIẢM THUẾ / MIỄN THUẾ:
 *   - Tài sản dưới 1M vàng: miễn thuế
 *   - Thành viên bang hội: giảm 5%
 *   - VIP active: giảm 10%
 *   - Level < 30: miễn thuế (newbie protection)
 *   - Account mới < 7 ngày: miễn thuế 100%
 *   - Dòng tiền kỳ này < 500M quy vàng: miễn thuế
 *
 * DÒNG TIỀN (Cash Flow):
 *   Tổng thu nhập (vàng kiếm được, thỏi vàng nhận,
 *   ngọc xanh/hồng ngọc/xu NRO nhận) trong 1 kỳ thuế.
 *   Nếu dòng tiền < ngưỡng → người chơi nhỏ lẻ, miễn thuế.
 */

import nro.player.Player;
import nro.player.Inventory;
import utils.Util;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaxService {

    private static TaxService instance;

    public static TaxService gI() {
        if (instance == null) {
            instance = new TaxService();
        }
        return instance;
    }

    // ===== QUY ĐỔI TÀI SẢN =====
    public static final long THOI_VANG_TO_GOLD = 50_000_000L;    // 1 thỏi vàng = 50M gold
    public static final long NGOC_XANH_TO_GOLD = 10_000L;        // 1 ngọc xanh = 10K gold
    public static final long HONG_NGOC_TO_GOLD = 50_000L;        // 1 hồng ngọc = 50K gold
    public static final long XU_NRO_TO_GOLD = 1_000_000L;        // 1 xu NRO = 1M gold

    // ===== BIỂU THUẾ LŨY TIẾN =====
    private static final long[] TAX_BRACKETS = {
            10_000_000L,          // 10M
            100_000_000L,         // 100M
            1_000_000_000L,       // 1B
            10_000_000_000L,      // 10B
            100_000_000_000L,     // 100B
            1_000_000_000_000L,   // 1T
            Long.MAX_VALUE        // trên 1T
    };

    private static final double[] TAX_RATES = {
            0.05,  // 5%
            0.10,  // 10%
            0.15,  // 15%
            0.20,  // 20%
            0.25,  // 25%
            0.30,  // 30%
            0.35   // 35%
    };

    // ===== NGƯỠNG MIỄN THUẾ =====
    public static final long TAX_EXEMPT_THRESHOLD = 1_000_000L;   // 1M gold
    public static final int NEWBIE_LEVEL_EXEMPT = 30;              // Level < 30 miễn thuế
    public static final long ACCOUNT_AGE_EXEMPT_MS = 7L * 24 * 60 * 60 * 1000; // 7 ngày
    public static final long CASH_FLOW_EXEMPT_THRESHOLD = 500_000_000L; // Dòng tiền < 500M → miễn

    // ===== GIẢM THUẾ =====
    public static final double CLAN_DISCOUNT = 0.05;              // Có bang: giảm 5%
    public static final double VIP_DISCOUNT = 0.10;               // VIP: giảm 10%

    // ===== DEBUFF LEVELS =====
    public static final int MAX_EVASION_LEVEL = 4;

    // ===== DATA: playerID → TaxRecord =====
    private final Map<Long, TaxRecord> taxRecords = new ConcurrentHashMap<>();

    // ===== TAX RECORD =====
    public static class TaxRecord {
        public long playerId;
        public long lastTaxTime;          // Timestamp lần đóng thuế gần nhất
        public long totalTaxPaid;         // Tổng thuế đã đóng (quy vàng)
        public long currentTaxOwed;       // Thuế phải đóng kỳ này
        public long backTaxOwed;          // Thuế truy thu (nợ cũ)
        public long penaltyOwed;          // Tiền phạt trốn thuế
        public int evasionLevel;          // Cấp trốn thuế (0 = không trốn)
        public int consecutiveEvasions;   // Số kỳ liên tiếp trốn thuế
        public long lastAssessmentTime;   // Timestamp lần tính thuế gần nhất
        public boolean hasDebuff;         // Đang bị debuff không
        public long totalWealth;          // Tổng tài sản quy vàng (cache)

        // Debuff effects
        public double damePenalty;        // % giảm sức đánh
        public double hpPenalty;          // % giảm HP
        public boolean blockPickItem;     // Chặn nhặt đồ
        public boolean backTaxMultiplier; // Truy thu x2

        // ===== DÒNG TIỀN (Cash Flow) theo kỳ =====
        public long flowGoldIn;           // Vàng kiếm được trong kỳ
        public long flowThoiVangIn;       // Số thỏi vàng nhận trong kỳ
        public long flowGemIn;            // Ngọc xanh nhận trong kỳ
        public long flowRubyIn;           // Hồng ngọc nhận trong kỳ
        public long flowXuNroIn;          // Xu NRO nhận trong kỳ
        public long flowGoldOut;          // Vàng chi ra trong kỳ
        public long flowPeriodStart;      // Timestamp đầu kỳ

        public TaxRecord(long playerId) {
            this.playerId = playerId;
            this.lastTaxTime = System.currentTimeMillis();
            this.lastAssessmentTime = System.currentTimeMillis();
            this.flowPeriodStart = System.currentTimeMillis();
        }

        public long getTotalDebt() {
            return currentTaxOwed + backTaxOwed + penaltyOwed;
        }

        /** Tổng dòng tiền VÀO quy ra vàng */
        public long getTotalCashFlowIn() {
            return flowGoldIn
                    + flowThoiVangIn * THOI_VANG_TO_GOLD
                    + flowGemIn * NGOC_XANH_TO_GOLD
                    + flowRubyIn * HONG_NGOC_TO_GOLD
                    + flowXuNroIn * XU_NRO_TO_GOLD;
        }

        /** Reset dòng tiền cho kỳ mới */
        public void resetCashFlow() {
            flowGoldIn = 0;
            flowThoiVangIn = 0;
            flowGemIn = 0;
            flowRubyIn = 0;
            flowXuNroIn = 0;
            flowGoldOut = 0;
            flowPeriodStart = System.currentTimeMillis();
        }
    }

    // ===== LẤY / TẠO TAX RECORD =====
    public TaxRecord getOrCreateRecord(Player player) {
        return taxRecords.computeIfAbsent(player.id, TaxRecord::new);
    }

    public TaxRecord getRecord(long playerId) {
        return taxRecords.get(playerId);
    }

    // ===== TÍNH TỔNG TÀI SẢN =====
    public long calculateTotalWealth(Player player) {
        if (player == null || player.inventory == null) return 0;

        long totalGold = player.inventory.gold;

        // Đếm Thỏi Vàng trong túi
        int thoiVangCount = countItemInBag(player, 457); // ID thỏi vàng
        totalGold += (long) thoiVangCount * THOI_VANG_TO_GOLD;

        // Ngọc Xanh (gem)
        totalGold += (long) player.inventory.gem * NGOC_XANH_TO_GOLD;

        // Hồng Ngọc (ruby)
        totalGold += (long) player.inventory.ruby * HONG_NGOC_TO_GOLD;

        // Xu Ngọc Rồng (coupon / event currency)
        totalGold += (long) player.inventory.coupon * XU_NRO_TO_GOLD;

        return totalGold;
    }

    // ===== ĐẾM ITEM TRONG TÚI =====
    private int countItemInBag(Player player, int itemTemplateId) {
        int count = 0;
        if (player.inventory.itemsBag != null) {
            for (item.Item it : player.inventory.itemsBag) {
                if (it != null && it.isNotNullItem() && it.template.id == itemTemplateId) {
                    count += it.quantity;
                }
            }
        }
        return count;
    }

    // ===== TÍNH THUẾ LŨY TIẾN =====
    public long calculateProgressiveTax(long totalWealth) {
        if (totalWealth <= TAX_EXEMPT_THRESHOLD) return 0;

        long taxableAmount = totalWealth;
        long totalTax = 0;
        long previousBracket = 0;

        for (int i = 0; i < TAX_BRACKETS.length; i++) {
            long bracketLimit = TAX_BRACKETS[i];
            if (taxableAmount <= previousBracket) break;

            long taxableInBracket = Math.min(taxableAmount, bracketLimit) - previousBracket;
            if (taxableInBracket > 0) {
                totalTax += (long) (taxableInBracket * TAX_RATES[i]);
            }
            previousBracket = bracketLimit;
        }

        return totalTax;
    }

    // ===== KIỂM TRA ACCOUNT MỚI < 7 NGÀY =====
    public boolean isNewAccount(Player player) {
        if (player.firstTimeLogin == null) return false;
        long age = System.currentTimeMillis() - player.firstTimeLogin.getTime();
        return age < ACCOUNT_AGE_EXEMPT_MS;
    }

    /** Số ngày còn lại được miễn thuế (account mới) */
    public int getExemptDaysRemaining(Player player) {
        if (player.firstTimeLogin == null) return 0;
        long age = System.currentTimeMillis() - player.firstTimeLogin.getTime();
        long remaining = ACCOUNT_AGE_EXEMPT_MS - age;
        return remaining > 0 ? (int) (remaining / (24 * 60 * 60 * 1000)) + 1 : 0;
    }

    // ===== GHI NHẬN DÒNG TIỀN =====
    /** Gọi mỗi khi player NHẬN vàng (farm, bán đồ, thưởng...) */
    public void recordGoldIncome(Player player, long amount) {
        if (amount <= 0) return;
        TaxRecord r = getOrCreateRecord(player);
        r.flowGoldIn += amount;
    }

    /** Gọi mỗi khi player NHẬN thỏi vàng */
    public void recordThoiVangIncome(Player player, int quantity) {
        if (quantity <= 0) return;
        TaxRecord r = getOrCreateRecord(player);
        r.flowThoiVangIn += quantity;
    }

    /** Gọi mỗi khi player NHẬN ngọc xanh */
    public void recordGemIncome(Player player, int amount) {
        if (amount <= 0) return;
        TaxRecord r = getOrCreateRecord(player);
        r.flowGemIn += amount;
    }

    /** Gọi mỗi khi player NHẬN hồng ngọc */
    public void recordRubyIncome(Player player, int amount) {
        if (amount <= 0) return;
        TaxRecord r = getOrCreateRecord(player);
        r.flowRubyIn += amount;
    }

    /** Gọi mỗi khi player NHẬN xu NRO */
    public void recordXuNroIncome(Player player, int amount) {
        if (amount <= 0) return;
        TaxRecord r = getOrCreateRecord(player);
        r.flowXuNroIn += amount;
    }

    /** Gọi mỗi khi player CHI vàng (mua đồ, nâng cấp...) */
    public void recordGoldExpense(Player player, long amount) {
        if (amount <= 0) return;
        TaxRecord r = getOrCreateRecord(player);
        r.flowGoldOut += amount;
    }

    // ===== KIỂM TRA LÝ DO MIỄN THUẾ =====
    public String getExemptReason(Player player) {
        if (player.level < NEWBIE_LEVEL_EXEMPT)
            return "Level < " + NEWBIE_LEVEL_EXEMPT + " (newbie)";
        if (isNewAccount(player))
            return "Account mới < 7 ngày (còn " + getExemptDaysRemaining(player) + " ngày)";
        long totalWealth = calculateTotalWealth(player);
        if (totalWealth <= TAX_EXEMPT_THRESHOLD)
            return "Tài sản < " + Util.numberToMoney(TAX_EXEMPT_THRESHOLD) + " vàng";
        TaxRecord r = getOrCreateRecord(player);
        if (r.getTotalCashFlowIn() < CASH_FLOW_EXEMPT_THRESHOLD)
            return "Dòng tiền kỳ này < " + Util.numberToMoney(CASH_FLOW_EXEMPT_THRESHOLD) + " vàng";
        return null; // Không được miễn
    }

    // ===== TÍNH THUẾ CHO PLAYER (CÓ GIẢM TRỪ) =====
    public long assessTax(Player player) {
        if (player == null) return 0;

        // Miễn thuế newbie (level)
        if (player.level < NEWBIE_LEVEL_EXEMPT) return 0;

        // Miễn thuế account mới < 7 ngày
        if (isNewAccount(player)) return 0;

        long totalWealth = calculateTotalWealth(player);

        // Miễn thuế tài sản nhỏ
        if (totalWealth <= TAX_EXEMPT_THRESHOLD) return 0;

        // Miễn thuế dòng tiền nhỏ (người chơi nhỏ lẻ, không gây lạm phát)
        TaxRecord record = getOrCreateRecord(player);
        if (record.getTotalCashFlowIn() < CASH_FLOW_EXEMPT_THRESHOLD) return 0;

        long baseTax = calculateProgressiveTax(totalWealth);

        // Áp dụng giảm trừ
        double discountRate = 0;
        if (player.clan != null) {
            discountRate += CLAN_DISCOUNT;
        }
        if (player.vip > 0 && player.timevip > System.currentTimeMillis()) {
            discountRate += VIP_DISCOUNT;
        }

        long finalTax = (long) (baseTax * (1.0 - discountRate));

        // Thuế tính trên tổng tài sản / ngày (1 kỳ = 1 ngày)
        // Không chia nhỏ — đóng 1 lần/ngày trước hạn

        // Update record
        record.totalWealth = totalWealth;
        record.currentTaxOwed = finalTax;
        record.lastAssessmentTime = System.currentTimeMillis();

        return finalTax;
    }

    // ===== DEADLINE TRONG NGÀY =====
    /** Kiểm tra đã qua hạn nộp thuế chưa (18:30) */
    public boolean isPastDeadline() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        return (hour > 18) || (hour == 18 && minute >= 30);
    }

    /** Lấy text hạn nộp thuế */
    public String getDeadlineText() {
        return "12:00 trưa hoặc 18:30 tối";
    }

    /** Kiểm tra player đã đóng thuế hôm nay chưa */
    public boolean hasPaidToday(Player player) {
        TaxRecord record = getOrCreateRecord(player);
        if (record.lastTaxTime <= 0) return false;

        Calendar lastPaid = Calendar.getInstance();
        lastPaid.setTimeInMillis(record.lastTaxTime);

        Calendar today = Calendar.getInstance();
        return lastPaid.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && lastPaid.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    /** Lấy trạng thái thuế hôm nay */
    public String getTodayStatus(Player player) {
        if (getExemptReason(player) != null) return "MIỄN THUẾ";
        if (hasPaidToday(player)) return "✅ Đã đóng";
        if (isPastDeadline()) return "⚠ QUÁ HẠN!";
        return "⏳ Chưa đóng (hạn " + getDeadlineText() + ")";
    }

    // ===== ĐÓNG THUẾ =====
    public PayTaxResult payTax(Player player) {
        TaxRecord record = getOrCreateRecord(player);
        long totalDebt = record.getTotalDebt();

        if (totalDebt <= 0) {
            return new PayTaxResult(false, 0, "Bạn không có thuế phải đóng!");
        }

        // Kiểm tra đủ vàng
        if (player.inventory.gold < totalDebt) {
            return new PayTaxResult(false, totalDebt,
                    "Không đủ vàng! Cần " + Util.numberToMoney(totalDebt) + " vàng để đóng thuế.");
        }

        // Trừ vàng
        player.inventory.gold -= totalDebt;

        // Cập nhật record
        record.totalTaxPaid += totalDebt;
        record.currentTaxOwed = 0;
        record.backTaxOwed = 0;
        record.penaltyOwed = 0;
        record.consecutiveEvasions = 0;
        record.evasionLevel = 0;
        record.lastTaxTime = System.currentTimeMillis();

        // Xóa debuff
        removeDebuff(player, record);

        return new PayTaxResult(true, totalDebt,
                "Đóng thuế thành công! Đã nộp " + Util.numberToMoney(totalDebt) + " vàng.");
    }

    // ===== ĐÓNG THUẾ TỪNG PHẦN =====
    public PayTaxResult payPartialTax(Player player, long amount) {
        TaxRecord record = getOrCreateRecord(player);
        long totalDebt = record.getTotalDebt();

        if (totalDebt <= 0) {
            return new PayTaxResult(false, 0, "Bạn không có thuế phải đóng!");
        }

        if (player.inventory.gold < amount) {
            return new PayTaxResult(false, amount,
                    "Không đủ vàng! Bạn chỉ có " + Util.numberToMoney(player.inventory.gold) + " vàng.");
        }

        // Trừ vàng
        player.inventory.gold -= amount;
        record.totalTaxPaid += amount;

        // Trừ theo thứ tự: phạt → truy thu → thuế hiện tại
        long remaining = amount;
        if (record.penaltyOwed > 0) {
            long payPenalty = Math.min(remaining, record.penaltyOwed);
            record.penaltyOwed -= payPenalty;
            remaining -= payPenalty;
        }
        if (remaining > 0 && record.backTaxOwed > 0) {
            long payBack = Math.min(remaining, record.backTaxOwed);
            record.backTaxOwed -= payBack;
            remaining -= payBack;
        }
        if (remaining > 0 && record.currentTaxOwed > 0) {
            long payCurrent = Math.min(remaining, record.currentTaxOwed);
            record.currentTaxOwed -= payCurrent;
            remaining -= payCurrent;
        }

        // Nếu hết nợ → xóa debuff
        if (record.getTotalDebt() <= 0) {
            record.consecutiveEvasions = 0;
            record.evasionLevel = 0;
            record.lastTaxTime = System.currentTimeMillis();
            removeDebuff(player, record);
        }

        return new PayTaxResult(true, amount,
                "Đã đóng " + Util.numberToMoney(amount) + " vàng.\n"
                        + "Còn nợ: " + Util.numberToMoney(record.getTotalDebt()) + " vàng.");
    }

    // ===== XỬ LÝ TRỐN THUẾ (gọi mỗi kỳ thuế) =====
    public void processEvasion(Player player) {
        TaxRecord record = getOrCreateRecord(player);

        if (record.currentTaxOwed <= 0) return;

        // Tăng cấp trốn thuế
        record.consecutiveEvasions++;
        record.evasionLevel = Math.min(record.consecutiveEvasions, MAX_EVASION_LEVEL);

        // Chuyển thuế hiện tại → nợ cũ + phạt
        long evadedTax = record.currentTaxOwed;
        record.backTaxOwed += evadedTax;

        // Tính phạt theo cấp trốn
        double penaltyRate;
        switch (record.evasionLevel) {
            case 1 -> penaltyRate = 0.10;  // 10% phạt
            case 2 -> penaltyRate = 0.25;  // 25% phạt
            case 3 -> penaltyRate = 0.50;  // 50% phạt
            default -> penaltyRate = 1.00; // 100% phạt (x2 truy thu)
        }
        record.penaltyOwed += (long) (evadedTax * penaltyRate);
        record.currentTaxOwed = 0;

        // Áp dụng debuff
        applyDebuff(player, record);
    }

    // ===== ÁP DỤNG DEBUFF =====
    private void applyDebuff(Player player, TaxRecord record) {
        record.hasDebuff = true;

        switch (record.evasionLevel) {
            case 1 -> {
                record.damePenalty = 0.10;
                record.hpPenalty = 0;
                record.blockPickItem = false;
                record.backTaxMultiplier = false;
            }
            case 2 -> {
                record.damePenalty = 0.25;
                record.hpPenalty = 0.10;
                record.blockPickItem = false;
                record.backTaxMultiplier = false;
            }
            case 3 -> {
                record.damePenalty = 0.40;
                record.hpPenalty = 0.25;
                record.blockPickItem = true;
                record.backTaxMultiplier = false;
            }
            default -> { // 4+
                record.damePenalty = 0.50;
                record.hpPenalty = 0.40;
                record.blockPickItem = true;
                record.backTaxMultiplier = true;
            }
        }

        // Thông báo cho player
        if (player.isPl()) {
            String warning = "⚠ CẢNH BÁO THUẾ ⚠\n"
                    + "Bạn đã trốn thuế " + record.consecutiveEvasions + " kỳ liên tiếp!\n"
                    + "Debuff: Sức đánh -" + (int) (record.damePenalty * 100) + "%"
                    + (record.hpPenalty > 0 ? ", HP -" + (int) (record.hpPenalty * 100) + "%" : "")
                    + (record.blockPickItem ? ", CHẶN nhặt đồ" : "")
                    + (record.backTaxMultiplier ? ", TRUY THU x2" : "")
                    + "\nTổng nợ thuế: " + Util.numberToMoney(record.getTotalDebt()) + " vàng"
                    + "\nHãy đến NPC Thu Thuế để đóng thuế ngay!";
            Service.gI().sendThongBao(player, warning);
        }
    }

    // ===== XÓA DEBUFF =====
    private void removeDebuff(Player player, TaxRecord record) {
        record.hasDebuff = false;
        record.damePenalty = 0;
        record.hpPenalty = 0;
        record.blockPickItem = false;
        record.backTaxMultiplier = false;

        if (player.isPl()) {
            Service.gI().sendThongBao(player,
                    "✅ Bạn đã đóng đủ thuế! Mọi debuff đã được gỡ bỏ.\n"
                            + "Chúc chiến binh thuận lợi chiến đấu!");
        }
    }

    // ===== KIỂM TRA DEBUFF BLOCK PICK ITEM =====
    public boolean isBlockedFromPicking(Player player) {
        TaxRecord record = taxRecords.get(player.id);
        return record != null && record.blockPickItem;
    }

    // ===== LẤY DAME PENALTY =====
    public double getDamePenalty(Player player) {
        TaxRecord record = taxRecords.get(player.id);
        return record != null ? record.damePenalty : 0;
    }

    // ===== LẤY HP PENALTY =====
    public double getHpPenalty(Player player) {
        TaxRecord record = taxRecords.get(player.id);
        return record != null ? record.hpPenalty : 0;
    }

    // ===== FORMAT BIỂU THUẾ CHO HIỂN THỊ =====
    public String formatTaxBrackets() {
        StringBuilder sb = new StringBuilder();
        sb.append("|7|━━ BIỂU THUẾ LŨY TIẾN ━━\n\n");
        String[] labels = {
                "0 – 10M", "10M – 100M", "100M – 1B",
                "1B – 10B", "10B – 100B", "100B – 1T", "Trên 1T"
        };
        String[] rates = {"5%", "10%", "15%", "20%", "25%", "30%", "35%"};
        for (int i = 0; i < labels.length; i++) {
            sb.append("|1|Bậc ").append(i + 1).append(": ").append(labels[i]).append("\n");
            sb.append("|8|  → Thuế suất: ").append(rates[i]).append("\n");
        }
        sb.append("\n|7|━━━━━━━━━━━━━━━━━━");
        return sb.toString();
    }

    // ===== FORMAT TÌNH HÌNH THUẾ PLAYER =====
    public String formatPlayerTaxInfo(Player player) {
        TaxRecord record = getOrCreateRecord(player);
        long totalWealth = calculateTotalWealth(player);
        long currentTax = assessTax(player);

        StringBuilder sb = new StringBuilder();
        sb.append("|7|━━ SỔ THUẾ CÁ NHÂN ━━\n\n");

        // Tổng tài sản
        sb.append("|1|Tổng tài sản: ").append(Util.numberToMoney(totalWealth)).append(" vàng\n");
        sb.append("|8|  Vàng: ").append(Util.numberToMoney(player.inventory.gold)).append("\n");
        sb.append("|8|  Thỏi Vàng: ").append(countItemInBag(player, 457)).append(" cái\n");
        sb.append("|8|  Ngọc Xanh: ").append(Util.numberToMoney(player.inventory.gem)).append("\n");
        sb.append("|8|  Hồng Ngọc: ").append(Util.numberToMoney(player.inventory.ruby)).append("\n\n");

        // Thuế phải đóng
        sb.append("|1|Thuế kỳ này: ").append(Util.numberToMoney(currentTax)).append(" vàng\n");

        // Giảm trừ
        if (player.clan != null || (player.vip > 0 && player.timevip > System.currentTimeMillis())) {
            sb.append("|2|★ Giảm trừ: ");
            if (player.clan != null) sb.append("Bang hội -5% ");
            if (player.vip > 0) sb.append("VIP -10%");
            sb.append("\n");
        }

        // Nợ thuế
        long totalDebt = record.getTotalDebt();
        if (totalDebt > 0) {
            sb.append("\n|4|⚠ NỢ THUẾ: ").append(Util.numberToMoney(totalDebt)).append(" vàng\n");
            if (record.backTaxOwed > 0) {
                sb.append("|8|  Truy thu: ").append(Util.numberToMoney(record.backTaxOwed)).append("\n");
            }
            if (record.penaltyOwed > 0) {
                sb.append("|8|  Tiền phạt: ").append(Util.numberToMoney(record.penaltyOwed)).append("\n");
            }
            if (record.consecutiveEvasions > 0) {
                sb.append("|4|  ⚠ Trốn thuế: ").append(record.consecutiveEvasions).append(" kỳ liên tiếp\n");
            }
        } else {
            sb.append("\n|2|✅ Bạn đã đóng đủ thuế!");
        }

        sb.append("\n\n|8|Tổng thuế đã đóng: ").append(Util.numberToMoney(record.totalTaxPaid)).append("\n");
        sb.append("|7|━━━━━━━━━━━━━━━━━━");

        return sb.toString();
    }

    // ===== HOÀN THUẾ =====
    public RefundResult processRefund(Player player) {
        TaxRecord record = getOrCreateRecord(player);

        // Điều kiện hoàn thuế: đã đóng >= 3 kỳ liên tiếp, không có nợ
        if (record.getTotalDebt() > 0) {
            return new RefundResult(false, 0,
                    "Bạn đang có nợ thuế! Hãy đóng đủ thuế trước khi xin hoàn thuế.");
        }

        if (record.totalTaxPaid <= 0) {
            return new RefundResult(false, 0,
                    "Bạn chưa đóng thuế nào! Không có gì để hoàn.");
        }

        // Hoàn 5% tổng thuế đã đóng (chính sách khuyến khích đóng thuế đúng hạn)
        long refundAmount = (long) (record.totalTaxPaid * 0.05);
        if (refundAmount <= 0) {
            return new RefundResult(false, 0,
                    "Số thuế đã đóng quá ít để được hoàn thuế.");
        }

        // Giới hạn hoàn tối đa 10B / lần
        refundAmount = Math.min(refundAmount, 10_000_000_000L);

        // Cộng vàng
        player.inventory.addGoldSafe(refundAmount);

        // Reset tổng thuế đã đóng (sau hoàn)
        record.totalTaxPaid = 0;

        return new RefundResult(true, refundAmount,
                "Hoàn thuế thành công!\n"
                        + "Số tiền hoàn: " + Util.numberToMoney(refundAmount) + " vàng\n"
                        + "(5% tổng thuế đã đóng, tối đa 10B)");
    }

    // ===== RESULT CLASSES =====
    public static class PayTaxResult {
        public final boolean success;
        public final long amount;
        public final String message;

        public PayTaxResult(boolean success, long amount, String message) {
            this.success = success;
            this.amount = amount;
            this.message = message;
        }
    }

    public static class RefundResult {
        public final boolean success;
        public final long amount;
        public final String message;

        public RefundResult(boolean success, long amount, String message) {
            this.success = success;
            this.amount = amount;
            this.message = message;
        }
    }
}
