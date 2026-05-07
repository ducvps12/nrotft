package nro.models.npc.npc_manifest;

/**
 * NPC Hoàn Thuế — Musashi (Cải trang ID 1877)
 * ═══════════════════════════════════════════════
 * Vị trí: Sân sau siêu thị (map 48)
 * Avatar: Cải trang Musashi
 * 
 * Chức năng:
 *   1. Hoàn thuế 5% tổng đã đóng (nếu đóng đủ, không nợ)
 *   2. Xem lịch sử đóng thuế
 *   3. Tư vấn giảm thuế (vào bang, mua VIP)
 *   4. Kiểm tra trạng thái debuff
 *   5. Cấp giấy chứng nhận "Công dân gương mẫu" (nếu đóng đủ)
 */

import consts.ConstNpc;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.TaxService;
import nro.services.TaxService.RefundResult;
import nro.services.TaxService.TaxRecord;
import nro.services.TaskService;
import utils.Util;

public class HoanThue extends Npc {

    public HoanThue(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            TaxRecord record = TaxService.gI().getOrCreateRecord(player);

            // Kiểm tra miễn thuế
            String exemptReason = TaxService.gI().getExemptReason(player);
            if (exemptReason != null) {
                createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_MAIN,
                        "|7|━━━ HOÀN THUẾ VŨ TRỤ ━━━\n"
                                + "|1|★ Chuyên Viên Musashi ★\n"
                                + "|7|━━━━━━━━━━━━━━━━━━\n\n"
                                + "|8|Chào chiến binh " + player.name + "!\n"
                                + "|8|Ngươi được MIỄN THUẾ vì:\n"
                                + "|2|  " + exemptReason + "\n\n"
                                + "|8|Chưa cần lo về thuế đâu!\n"
                                + "|8|Hãy yên tâm chiến đấu!",
                        "Tư Vấn\nGiảm Thuế");
                return;
            }

            // Status tóm tắt
            String statusLine;
            if (record.getTotalDebt() > 0) {
                statusLine = "|4|⚠ Đang nợ thuế: " + Util.numberToMoney(record.getTotalDebt());
            } else if (record.totalTaxPaid > 0) {
                statusLine = "|2|✅ Đã đóng: " + Util.numberToMoney(record.totalTaxPaid) + " vàng";
            } else {
                statusLine = "|8|Chưa có lịch sử đóng thuế";
            }

            createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_MAIN,
                    "|7|━━━ HOÀN THUẾ VŨ TRỤ ━━━\n"
                            + "|1|★ Chuyên Viên Musashi ★\n"
                            + "|7|━━━━━━━━━━━━━━━━━━\n\n"
                            + "|8|Chào " + player.name + "! Ta phụ trách\n"
                            + "|8|hoàn thuế và tư vấn chính sách.\n\n"
                            + statusLine,
                    "Hoàn Thuế\n💎 5%",
                    "Tư Vấn\nGiảm Thuế",
                    "Kiểm Tra\nDebuff",
                    "Trang sau\n▶");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.MENU_HOAN_THUE_MAIN -> handleMain(player, select);
                case ConstNpc.MENU_HOAN_THUE_MAIN_2 -> handleMain2(player, select);
                case ConstNpc.MENU_HOAN_THUE_HOAN -> handleHoanThue(player, select);
                case ConstNpc.MENU_HOAN_THUE_CONFIRM -> handleConfirmHoan(player, select);
                case ConstNpc.MENU_HOAN_THUE_TU_VAN -> { if (select == 0) openBaseMenu(player); }
                case ConstNpc.MENU_HOAN_THUE_DEBUFF -> { if (select == 0) openBaseMenu(player); }
                case ConstNpc.MENU_HOAN_THUE_LICH_SU -> { if (select == 0) openBaseMenu(player); }
                case ConstNpc.MENU_HOAN_THUE_CHUNG_NHAN -> { if (select == 0) openBaseMenu(player); }
            }
        }
    }

    // ========== MENU CHÍNH TRANG 1 ==========
    private void handleMain(Player player, int select) {
        String exemptReason = TaxService.gI().getExemptReason(player);
        if (exemptReason != null) {
            if (select == 0) showTuVan(player);
            return;
        }

        switch (select) {
            case 0 -> showHoanThue(player);
            case 1 -> showTuVan(player);
            case 2 -> showDebuff(player);
            case 3 -> showMainPage2(player);
        }
    }

    // ========== MENU CHÍNH TRANG 2 ==========
    private void showMainPage2(Player player) {
        createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_MAIN_2,
                "|7|━━━ HOÀN THUẾ VŨ TRỤ ━━━\n"
                        + "|1|★ Chuyên Viên Musashi ★\n"
                        + "|7|━━━━━━━━━━━━━━━━━━\n\n"
                        + "|8|Các dịch vụ bổ sung:",
                "Lịch Sử\nĐóng Thuế",
                "Chứng Nhận\n★ Gương Mẫu",
                "◀ Quay\nLại");
    }

    private void handleMain2(Player player, int select) {
        switch (select) {
            case 0 -> showLichSu(player);
            case 1 -> showChungNhan(player);
            case 2 -> openBaseMenu(player);
        }
    }

    // ========== HOÀN THUẾ ==========
    private void showHoanThue(Player player) {
        TaxRecord record = TaxService.gI().getOrCreateRecord(player);

        // Kiểm tra nợ
        if (record.getTotalDebt() > 0) {
            createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_HOAN,
                    "|7|━━ HOÀN THUẾ ━━\n\n"
                            + "|4|⚠ Bạn đang có nợ thuế!\n"
                            + "|4|Nợ: " + Util.numberToMoney(record.getTotalDebt()) + " vàng\n\n"
                            + "|8|Hãy đóng đủ thuế tại NPC\n"
                            + "|8|Thu Thuế (Kochiro) trước\n"
                            + "|8|rồi quay lại hoàn thuế!",
                    "Quay Lại");
            return;
        }

        if (record.totalTaxPaid <= 0) {
            createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_HOAN,
                    "|7|━━ HOÀN THUẾ ━━\n\n"
                            + "|8|Bạn chưa đóng thuế nào!\n"
                            + "|8|Không có gì để hoàn.\n\n"
                            + "|8|Hãy thực hiện nghĩa vụ thuế\n"
                            + "|8|trước đã nhé!",
                    "Quay Lại");
            return;
        }

        long refundAmount = Math.min((long) (record.totalTaxPaid * 0.05), 10_000_000_000L);

        createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_CONFIRM,
                "|7|━━ HOÀN THUẾ 5% ━━\n\n"
                        + "|1|Tổng thuế đã đóng:\n"
                        + "|2|  " + Util.numberToMoney(record.totalTaxPaid) + " vàng\n\n"
                        + "|1|Số tiền được hoàn (5%):\n"
                        + "|2|  " + Util.numberToMoney(refundAmount) + " vàng\n\n"
                        + "|8|(Tối đa 10B vàng / lần hoàn)\n\n"
                        + "|8|Bạn muốn nhận hoàn thuế?",
                "Nhận Hoàn\nThuế 💎",
                "Để Sau");

    }

    private void handleHoanThue(Player player, int select) {
        if (select == 0) openBaseMenu(player);
    }

    private void handleConfirmHoan(Player player, int select) {
        if (select == 0) {
            RefundResult result = TaxService.gI().processRefund(player);
            Service.gI().sendThongBao(player, result.message);
            if (result.success) {
                PlayerService.gI().sendInfoHpMpMoney(player);
                // Thông báo server
                Service.gI().sendServerMessage(player,
                        "💎 " + player.name + " đã nhận hoàn thuế "
                                + Util.numberToMoney(result.amount) + " vàng! Công dân gương mẫu!");
            }
        }
        openBaseMenu(player);
    }

    // ========== TƯ VẤN GIẢM THUẾ ==========
    private void showTuVan(Player player) {
        TaxRecord record = TaxService.gI().getOrCreateRecord(player);
        long totalWealth = TaxService.gI().calculateTotalWealth(player);
        long baseTax = TaxService.gI().calculateProgressiveTax(totalWealth) / 4;

        // Tính giảm trừ hiện tại
        boolean hasClan = player.clan != null;
        boolean hasVip = player.vip > 0 && player.timevip > System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        sb.append("|7|━━ TƯ VẤN GIẢM THUẾ ━━\n\n");
        sb.append("|1|Thuế cơ bản mỗi kỳ:\n");
        sb.append("|8|  ").append(Util.numberToMoney(baseTax)).append(" vàng\n\n");

        sb.append("|1|Giảm trừ hiện tại:\n");
        if (hasClan) {
            sb.append("|2|  ✅ Bang hội: -5% (đang có)\n");
        } else {
            sb.append("|4|  ❌ Bang hội: -5% (chưa có)\n");
            sb.append("|8|     → Gia nhập bang để giảm!\n");
        }
        if (hasVip) {
            sb.append("|2|  ✅ VIP: -10% (đang kích hoạt)\n");
        } else {
            sb.append("|4|  ❌ VIP: -10% (chưa kích hoạt)\n");
            sb.append("|8|     → Mua VIP tại Lý Tiểu Nương!\n");
        }

        // Tính thuế sau giảm trừ
        double discount = (hasClan ? 0.05 : 0) + (hasVip ? 0.10 : 0);
        long finalTax = (long) (baseTax * (1.0 - discount));
        long saved = baseTax - finalTax;

        sb.append("\n|1|Thuế thực đóng mỗi kỳ:\n");
        sb.append("|2|  ").append(Util.numberToMoney(finalTax)).append(" vàng\n");
        if (saved > 0) {
            sb.append("|2|  Tiết kiệm: ").append(Util.numberToMoney(saved)).append("\n");
        }

        sb.append("\n|2|★ MẸO: Vào bang + VIP = giảm 15%!\n");
        sb.append("|7|━━━━━━━━━━━━━━━━━━");

        createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_TU_VAN, sb.toString(),
                "Quay Lại");
    }

    // ========== KIỂM TRA DEBUFF ==========
    private void showDebuff(Player player) {
        TaxRecord record = TaxService.gI().getOrCreateRecord(player);

        StringBuilder sb = new StringBuilder();
        sb.append("|7|━━ TRẠNG THÁI DEBUFF ━━\n\n");

        if (!record.hasDebuff) {
            sb.append("|2|✅ Không có debuff nào!\n\n");
            sb.append("|8|Bạn đang đóng thuế đầy đủ.\n");
            sb.append("|8|Tiếp tục phát huy nhé!\n");
        } else {
            sb.append("|4|⚠ ĐANG BỊ DEBUFF THUẾ!\n\n");
            sb.append("|1|Cấp trốn thuế: ").append(record.evasionLevel).append("\n");
            sb.append("|1|Số kỳ liên tiếp: ").append(record.consecutiveEvasions).append("\n\n");

            sb.append("|4|Hiệu ứng phạt:\n");
            if (record.damePenalty > 0) {
                sb.append("|4|  ⬇ Sức đánh: -").append((int) (record.damePenalty * 100)).append("%\n");
            }
            if (record.hpPenalty > 0) {
                sb.append("|4|  ⬇ HP: -").append((int) (record.hpPenalty * 100)).append("%\n");
            }
            if (record.blockPickItem) {
                sb.append("|4|  🚫 Chặn nhặt đồ rơi\n");
            }
            if (record.backTaxMultiplier) {
                sb.append("|4|  💀 Truy thu gấp đôi!\n");
            }

            sb.append("\n|2|★ Đóng đủ thuế tại NPC Kochiro\n");
            sb.append("|2|  để gỡ bỏ mọi debuff!");
        }

        sb.append("\n|7|━━━━━━━━━━━━━━━━━━");

        createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_DEBUFF, sb.toString(),
                "Quay Lại");
    }

    // ========== LỊCH SỬ ĐÓNG THUẾ ==========
    private void showLichSu(Player player) {
        TaxRecord record = TaxService.gI().getOrCreateRecord(player);
        long totalWealth = TaxService.gI().calculateTotalWealth(player);

        StringBuilder sb = new StringBuilder();
        sb.append("|7|━━ LỊCH SỬ THUẾ ━━\n\n");

        sb.append("|1|Tổng tài sản hiện tại:\n");
        sb.append("|8|  ").append(Util.numberToMoney(totalWealth)).append(" vàng\n\n");

        sb.append("|1|Tổng thuế đã đóng:\n");
        sb.append("|2|  ").append(Util.numberToMoney(record.totalTaxPaid)).append(" vàng\n\n");

        if (record.lastTaxTime > 0) {
            long timeSince = System.currentTimeMillis() - record.lastTaxTime;
            long hours = timeSince / 3600000;
            sb.append("|8|Lần đóng gần nhất: ").append(hours).append("h trước\n\n");
        }

        sb.append("|1|Trạng thái:\n");
        if (record.getTotalDebt() > 0) {
            sb.append("|4|  ⚠ Đang nợ: ").append(Util.numberToMoney(record.getTotalDebt())).append("\n");
        } else {
            sb.append("|2|  ✅ Không nợ thuế\n");
        }

        if (record.consecutiveEvasions > 0) {
            sb.append("|4|  Trốn thuế: ").append(record.consecutiveEvasions).append(" kỳ\n");
        }

        sb.append("\n|7|━━━━━━━━━━━━━━━━━━");

        createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_LICH_SU, sb.toString(),
                "Quay Lại");
    }

    // ========== CHỨNG NHẬN CÔNG DÂN GƯƠNG MẪU ==========
    private void showChungNhan(Player player) {
        TaxRecord record = TaxService.gI().getOrCreateRecord(player);

        StringBuilder sb = new StringBuilder();
        sb.append("|7|━━ CHỨNG NHẬN THUẾ ━━\n\n");

        if (record.getTotalDebt() > 0) {
            sb.append("|4|❌ Bạn đang có nợ thuế!\n");
            sb.append("|4|Không đủ điều kiện cấp\n");
            sb.append("|4|chứng nhận công dân gương mẫu.\n\n");
            sb.append("|8|Hãy đóng đủ thuế trước nhé!");
        } else if (record.totalTaxPaid < 1_000_000_000L) {
            sb.append("|8|Bạn cần đóng tổng cộng\n");
            sb.append("|8|ít nhất 1B vàng tiền thuế\n");
            sb.append("|8|để nhận chứng nhận.\n\n");
            sb.append("|8|Đã đóng: ").append(Util.numberToMoney(record.totalTaxPaid)).append("\n");
            sb.append("|8|Còn thiếu: ").append(Util.numberToMoney(1_000_000_000L - record.totalTaxPaid)).append("\n");
        } else {
            sb.append("|2|╔═══════════════════════╗\n");
            sb.append("|2|║                       ║\n");
            sb.append("|2|║   ★ CÔNG DÂN MẪU ★   ║\n");
            sb.append("|2|║                       ║\n");
            sb.append("|2|║  ").append(player.name);
            // Pad name
            int padLen = 21 - player.name.length();
            sb.append(" ".repeat(Math.max(0, padLen)));
            sb.append("║\n");
            sb.append("|2|║                       ║\n");
            sb.append("|2|║  Đã đóng đầy đủ thuế ║\n");
            sb.append("|2|║  cho Vũ Trụ MTDGame!  ║\n");
            sb.append("|2|║                       ║\n");
            sb.append("|2|╚═══════════════════════╝\n\n");
            sb.append("|1|Tổng thuế: ").append(Util.numberToMoney(record.totalTaxPaid)).append("\n");
            sb.append("|8|Được hoàn thuế 5% + Ưu tiên\n");
            sb.append("|8|sự kiện đặc biệt!");
        }

        sb.append("\n|7|━━━━━━━━━━━━━━━━━━");

        createOtherMenu(player, ConstNpc.MENU_HOAN_THUE_CHUNG_NHAN, sb.toString(),
                "Quay Lại");
    }
}
