package nro.models.npc.npc_manifest;

/**
 * NPC Thu Thuế — Kochiro (Cải trang ID 1876)
 * ═══════════════════════════════════════════
 * Vị trí: Sân sau siêu thị (map 48)
 * Avatar: Cải trang Kochiro
 * 
 * Chức năng:
 *   1. Xem tình hình thuế cá nhân (Sổ Thuế)
 *   2. Đóng thuế toàn bộ / từng phần
 *   3. Xem biểu thuế lũy tiến
 *   4. Xem hướng dẫn hệ thống thuế
 *   5. Xem bảng phạt trốn thuế
 *
 * Kỳ thuế: 1 ngày — hạn nộp 12h trưa hoặc 18h30 tối
 */

import consts.ConstNpc;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.TaxService;
import nro.services.TaxService.PayTaxResult;
import nro.services.TaxService.TaxRecord;
import nro.services.TaskService;
import utils.Util;

public class ThuThue extends Npc {

    public ThuThue(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            TaxRecord record = TaxService.gI().getOrCreateRecord(player);
            long totalDebt = record.getTotalDebt();

            // Kiểm tra miễn thuế (newbie, account mới, tài sản nhỏ, dòng tiền thấp)
            String exemptReason = TaxService.gI().getExemptReason(player);
            if (exemptReason != null) {
                String extraInfo = "";
                if (TaxService.gI().isNewAccount(player)) {
                    int daysLeft = TaxService.gI().getExemptDaysRemaining(player);
                    extraInfo = "\n|2|★ Account mới: còn " + daysLeft + " ngày miễn thuế!";
                }
                createOtherMenu(player, ConstNpc.MENU_THU_THUE_MAIN,
                        "|7|━━━ SỞ THUẾ VŨ TRỤ ━━━\n"
                                + "|1|★ Thanh Tra Thuế Kochiro ★\n"
                                + "|7|━━━━━━━━━━━━━━━━━━\n\n"
                                + "|2|Chào chiến binh " + player.name + "!\n"
                                + "|8|Ngươi được MIỄN THUẾ vì:\n"
                                + "|2|  " + exemptReason + "\n"
                                + extraInfo + "\n\n"
                                + "|8|Hãy yên tâm chiến đấu!",
                        "Xem Biểu\nThuế",
                        "Hướng Dẫn\n★");
                return;
            }

            // Menu chính — hiển thị trạng thái thuế hôm nay
            String statusLine = TaxService.gI().getTodayStatus(player);
            String debtInfo = "";
            if (totalDebt > 0) {
                debtInfo = "\n|4|⚠ NỢ THUẾ: " + Util.numberToMoney(totalDebt) + " vàng";
                if (record.consecutiveEvasions > 0) {
                    debtInfo += "\n|4|  Trốn thuế: " + record.consecutiveEvasions + " ngày!";
                }
            }

            createOtherMenu(player, ConstNpc.MENU_THU_THUE_MAIN,
                    "|7|━━━ SỞ THUẾ VŨ TRỤ ━━━\n"
                            + "|1|★ Thanh Tra Thuế Kochiro ★\n"
                            + "|7|━━━━━━━━━━━━━━━━━━\n\n"
                            + "|8|Chào chiến binh " + player.name + "!\n"
                            + "|1|Hôm nay: " + statusLine + "\n"
                            + "|8|Hạn nộp: " + TaxService.gI().getDeadlineText()
                            + debtInfo,
                    "Sổ Thuế\n★ Xem",
                    "Đóng Thuế\n💰",
                    "Biểu Thuế\n& Hướng Dẫn",
                    "Bảng Phạt\n⚠");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.MENU_THU_THUE_MAIN -> handleMain(player, select);
                case ConstNpc.MENU_THU_THUE_SO_THUE -> { if (select == 0) openBaseMenu(player); }
                case ConstNpc.MENU_THU_THUE_DONG -> handleDongThue(player, select);
                case ConstNpc.MENU_THU_THUE_CONFIRM -> handleConfirmDong(player, select);
                case ConstNpc.MENU_THU_THUE_BIEU -> { if (select == 0) showHuongDan2(player); else if (select == 1) openBaseMenu(player); }
                case ConstNpc.MENU_THU_THUE_HUONG_DAN -> { if (select == 0) openBaseMenu(player); }
                case ConstNpc.MENU_THU_THUE_HUONG_DAN_2 -> { if (select == 0) openBaseMenu(player); }
                case ConstNpc.MENU_THU_THUE_PHAT -> { if (select == 0) openBaseMenu(player); }
            }
        }
    }

    // ========== MENU CHÍNH ==========
    private void handleMain(Player player, int select) {
        // Exempt player chỉ có 2 button
        String exemptReason = TaxService.gI().getExemptReason(player);
        if (exemptReason != null) {
            switch (select) {
                case 0 -> showBieuThue(player);
                case 1 -> showHuongDan(player);
            }
            return;
        }

        switch (select) {
            case 0 -> showSoThue(player);
            case 1 -> showDongThue(player);
            case 2 -> showBieuThue(player);
            case 3 -> showBangPhat(player);
        }
    }

    // ========== SỔ THUẾ CÁ NHÂN ==========
    private void showSoThue(Player player) {
        String info = TaxService.gI().formatPlayerTaxInfo(player);
        createOtherMenu(player, ConstNpc.MENU_THU_THUE_SO_THUE, info,
                "Quay Lại");
    }

    // ========== ĐÓNG THUẾ ==========
    private void showDongThue(Player player) {
        TaxRecord record = TaxService.gI().getOrCreateRecord(player);
        long currentTax = TaxService.gI().assessTax(player);
        long totalDebt = record.getTotalDebt();

        // Đã đóng hôm nay rồi
        if (TaxService.gI().hasPaidToday(player) && totalDebt <= 0) {
            createOtherMenu(player, ConstNpc.MENU_THU_THUE_DONG,
                    "|7|━━ ĐÓNG THUẾ ━━\n\n"
                            + "|2|✅ Hôm nay đã đóng thuế rồi!\n\n"
                            + "|8|Tiếp tục chiến đấu và kiếm\n"
                            + "|8|tài sản nha chiến binh!\n\n"
                            + "|8|Ngày mai nhớ đóng trước\n"
                            + "|8|" + TaxService.gI().getDeadlineText() + " nhé!",
                    "Quay Lại");
            return;
        }

        if (totalDebt <= 0 && currentTax <= 0) {
            createOtherMenu(player, ConstNpc.MENU_THU_THUE_DONG,
                    "|7|━━ ĐÓNG THUẾ ━━\n\n"
                            + "|2|✅ Bạn không có thuế phải đóng!\n\n"
                            + "|8|Tiếp tục chiến đấu và kiếm\n"
                            + "|8|tài sản nha chiến binh!",
                    "Quay Lại");
            return;
        }

        String info = "|7|━━ ĐÓNG THUẾ ━━\n\n"
                + "|1|Thuế hôm nay: " + Util.numberToMoney(currentTax) + "\n";

        if (record.backTaxOwed > 0) {
            info += "|4|Truy thu nợ: " + Util.numberToMoney(record.backTaxOwed) + "\n";
        }
        if (record.penaltyOwed > 0) {
            info += "|4|Tiền phạt: " + Util.numberToMoney(record.penaltyOwed) + "\n";
        }

        long totalOwed = totalDebt + currentTax;
        info += "\n|1|TỔNG PHẢI ĐÓNG: " + Util.numberToMoney(totalOwed) + " vàng\n"
                + "|8|Vàng hiện có: " + Util.numberToMoney(player.inventory.gold) + "\n"
                + "|8|Hạn nộp: " + TaxService.gI().getDeadlineText();

        createOtherMenu(player, ConstNpc.MENU_THU_THUE_DONG, info,
                "Đóng Toàn\nBộ 💰",
                "Đóng 50%\n★",
                "Quay Lại");
    }

    private void handleDongThue(Player player, int select) {
        TaxRecord record = TaxService.gI().getOrCreateRecord(player);
        long totalDebt = record.getTotalDebt();

        switch (select) {
            case 0 -> {
                // Đóng toàn bộ - xác nhận
                if (totalDebt <= 0) {
                    Service.gI().sendThongBao(player, "Không có thuế phải đóng!");
                    return;
                }
                createOtherMenu(player, ConstNpc.MENU_THU_THUE_CONFIRM,
                        "|7|━━ XÁC NHẬN ĐÓNG THUẾ ━━\n\n"
                                + "|1|Số tiền: " + Util.numberToMoney(totalDebt) + " vàng\n\n"
                                + "|8|Bạn chắc chắn muốn đóng\n"
                                + "|8|toàn bộ thuế không?",
                        "Xác Nhận\n✅",
                        "Hủy");
            }
            case 1 -> {
                // Đóng 50%
                if (totalDebt <= 0) {
                    Service.gI().sendThongBao(player, "Không có thuế phải đóng!");
                    return;
                }
                long halfDebt = totalDebt / 2;
                PayTaxResult result = TaxService.gI().payPartialTax(player, halfDebt);
                Service.gI().sendThongBao(player, result.message);
                PlayerService.gI().sendInfoHpMpMoney(player);
                openBaseMenu(player);
            }
            case 2 -> openBaseMenu(player);
        }
    }

    private void handleConfirmDong(Player player, int select) {
        if (select == 0) {
            PayTaxResult result = TaxService.gI().payTax(player);
            Service.gI().sendThongBao(player, result.message);
            PlayerService.gI().sendInfoHpMpMoney(player);
            if (result.success) {
                // Thông báo toàn server
                Service.gI().sendServerMessage(player,
                        "📢 " + player.name + " đã thực hiện nghĩa vụ thuế đầy đủ! Công dân gương mẫu!");
            }
        }
        openBaseMenu(player);
    }

    // ========== BIỂU THUẾ ==========
    private void showBieuThue(Player player) {
        String info = TaxService.gI().formatTaxBrackets();
        createOtherMenu(player, ConstNpc.MENU_THU_THUE_BIEU, info,
                "Hướng Dẫn\n★",
                "Quay Lại");
    }

    // ========== HƯỚNG DẪN THUẾ (TRANG 1) ==========
    private void showHuongDan(Player player) {
        String info = "|7|━━ HƯỚNG DẪN HỆ THỐNG THUẾ ━━\n\n"
                + "|1|1. Tại sao phải đóng thuế?\n"
                + "|8|  Để ổn định kinh tế vũ trụ,\n"
                + "|8|  chống lạm phát và giữ cân\n"
                + "|8|  bằng game cho mọi chiến binh.\n\n"
                + "|1|2. Kỳ thuế là bao lâu?\n"
                + "|8|  1 ngày = 1 kỳ thuế\n"
                + "|8|  Hạn nộp: 12h trưa hoặc\n"
                + "|8|  18h30 tối. Quá hạn = phạt!\n\n"
                + "|1|3. Ai được MIỄN THUẾ?\n"
                + "|8|  - Level < 30 (newbie)\n"
                + "|8|  - Account mới < 7 ngày\n"
                + "|8|  - Tài sản < 1M vàng\n"
                + "|8|  - Dòng tiền < 500M/ngày\n\n"
                + "|2|★ Trang sau: Giảm trừ & Quy đổi";

        createOtherMenu(player, ConstNpc.MENU_THU_THUE_HUONG_DAN, info,
                "Quay Lại");
    }

    // ========== HƯỚNG DẪN THUẾ (TRANG 2) ==========
    private void showHuongDan2(Player player) {
        String info = "|7|━━ QUY ĐỔI TÀI SẢN ━━\n\n"
                + "|1|Quy đổi ra vàng để tính thuế:\n"
                + "|8|  1 Thỏi Vàng  = 50M vàng\n"
                + "|8|  1 Ngọc Xanh  = 10K vàng\n"
                + "|8|  1 Hồng Ngọc  = 50K vàng\n"
                + "|8|  1 Xu NRO     = 1M vàng\n\n"
                + "|1|Giảm trừ thuế:\n"
                + "|2|  ★ Có bang hội: -5% thuế\n"
                + "|2|  ★ VIP active:  -10% thuế\n\n"
                + "|1|Hoàn thuế:\n"
                + "|8|  Đóng đủ thuế được hoàn 5%\n"
                + "|8|  tại NPC Hoàn Thuế (Musashi)\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_THU_THUE_HUONG_DAN_2, info,
                "Quay Lại");
    }

    // ========== BẢNG PHẠT TRỐN THUẾ ==========
    private void showBangPhat(Player player) {
        TaxRecord record = TaxService.gI().getOrCreateRecord(player);

        String currentStatus = "";
        if (record.consecutiveEvasions > 0) {
            currentStatus = "\n|4|⚠ BẠN ĐANG Ở CẤP " + record.evasionLevel + " TRỐN THUẾ!\n";
        }

        String info = "|7|━━ BẢNG PHẠT TRỐN THUẾ ━━\n"
                + currentStatus + "\n"
                + "|1|Ngày 1 trốn thuế:\n"
                + "|8|  Phạt 10% + Sức đánh -10%\n\n"
                + "|1|Ngày 2 liên tiếp:\n"
                + "|8|  Phạt 25% + SĐ -25%, HP -10%\n\n"
                + "|1|Ngày 3 liên tiếp:\n"
                + "|4|  Phạt 50% + SĐ -40%\n"
                + "|4|  HP -25% + CHẶN nhặt đồ\n\n"
                + "|1|Ngày 4+ liên tiếp:\n"
                + "|4|  Phạt 100% + SĐ -50%\n"
                + "|4|  HP -40% + CHẶN nhặt đồ\n"
                + "|4|  + TRUY THU GẤP ĐÔI!\n\n"
                + "|2|★ Đóng thuế đúng hạn = Xóa phạt!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_THU_THUE_PHAT, info,
                "Quay Lại");
    }
}
