package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import item.Item;
import jdbc.daos.PlayerDAO;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.server.ServerManager;
import nro.server.maintenance.QuaToriBot;
import nro.services.InventoryService;
import nro.services.NpcService;
import nro.services.Service;
import utils.Util;

public class ToriBot extends Npc {

    private static final int MENU_MAIN = 0;
    private static final int MENU_NAP = 1000;
    private static final int MENU_VIP = 100;
    private static final int MENU_GUIDE = 101;

    private static final int[] MOC_NAP_K = {
            10, 30, 50, 80, 120, 170, 220,
            300, 350, 440, 540, 600
    };

    public ToriBot(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player) || player.getSession() == null)
            return;

        String vipStatus = switch (player.vip) {
            case 1 -> "|2|⭐ Premium 1";
            case 2 -> "|2|⭐⭐ Premium 2";
            case 3 -> "|2|⭐⭐⭐ Premium 3";
            case 4 -> "|2|👑 Premium 4 MAX";
            default -> "|1|Chưa kích hoạt Premium";
        };

        createOtherMenu(player, MENU_MAIN,
                "|7|━━━ TORI BOT ━━━\n"
                + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                + vipStatus + "\n"
                + "|8|Nạp Premium để nhận quà VĨNH VIỄN!\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                "Xem Gói\nPremium",
                "Hướng Dẫn\nPremium",
                "Trạng Thái\nVIP");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player) || player.getSession() == null)
            return;

        switch (player.iDMark.getIndexMenu()) {

            case MENU_MAIN -> {
                switch (select) {
                    case 0 -> openMenuVip(player);
                    case 1 -> showPremiumGuide(player);
                    case 2 -> showVipStatus(player);
                }
            }

            case MENU_NAP -> handleNap(player, select);

            case MENU_VIP -> {
                switch (select) {
                    case 0 -> showPremiumDetail(player, 1);
                    case 1 -> showPremiumDetail(player, 2);
                    case 2 -> showPremiumDetail(player, 3);
                    case 3 -> showPremiumDetail(player, 4);
                }
            }

            case MENU_GUIDE -> {
                if (select == 0) openMenuVip(player);
            }

            case 223 -> buyVip(player, 1, 300_000, 7, () -> QuaToriBot.Qua_1(player));
            case 224 -> buyVip(player, 2, 700_000, 8, () -> QuaToriBot.Qua_2(player));
            case 225 -> buyVip(player, 3, 1_500_000, 8, () -> QuaToriBot.Qua_3(player));
            case 226 -> buyVip(player, 4, 2_000_000, 9, () -> QuaToriBot.Qua_4(player));
        }
    }

    // ===================== MENU CHỌN GÓI VIP =====================
    private void openMenuVip(Player player) {
        boolean p1 = PlayerDAO.checkPremium(player, 1);
        boolean p2 = PlayerDAO.checkPremium(player, 2);
        boolean p3 = PlayerDAO.checkPremium(player, 3);
        boolean p4 = PlayerDAO.checkPremium(player, 4);

        createOtherMenu(player, MENU_VIP,
                "|7|━━━ GÓI PREMIUM ━━━\n"
                + "|1|Chọn gói để xem chi tiết:\n"
                + (p1 ? "|2|✓ " : "|1|★ ") + "Premium 1 — 300K VNĐ\n"
                + (p2 ? "|2|✓ " : "|1|★ ") + "Premium 2 — 700K VNĐ\n"
                + (p3 ? "|2|✓ " : "|1|★ ") + "Premium 3 — 1.5M VNĐ\n"
                + (p4 ? "|2|✓ " : "|1|★ ") + "Premium 4 — 2M VNĐ\n"
                + "|8|Tất cả quà đều VĨNH VIỄN!\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                (p1 ? "✓ Premium 1\nĐã Mua" : "Premium 1\n300K"),
                (p2 ? "✓ Premium 2\nĐã Mua" : "Premium 2\n700K"),
                (p3 ? "✓ Premium 3\nĐã Mua" : "Premium 3\n1.5M"),
                (p4 ? "✓ Premium 4\nĐã Mua" : "Premium 4\n2M"));
    }

    // ===================== CHI TIẾT TỪNG GÓI =====================
    private void showPremiumDetail(Player player, int tier) {
        boolean owned = PlayerDAO.checkPremium(player, tier);
        String title, items, price;
        int menuId;

        switch (tier) {
            case 1 -> {
                menuId = 223;
                price = "300.000 VNĐ";
                title = "⭐ PREMIUM 1";
                items = "|8|━━ PHẦN THƯỞNG (VĨNH VIỄN) ━━\n"
                        + "|1|• 5.000 Thỏi Vàng\n"
                        + "|1|• 10 Phiếu Giảm Giá 80%\n"
                        + "|1|• 20 Đá Bảo Vệ\n"
                        + "|2|• Cải Trang Black Gohan VĨNH VIỄN\n"
                        + "|2|• Pet Chó 3 Đầu Địa Ngục VĨNH VIỄN\n"
                        + "|1|• 9.999 Mảnh Vỡ Bông Tai\n"
                        + "|1|• 10 Mảnh Khí Oozaru\n";
            }
            case 2 -> {
                menuId = 224;
                price = "700.000 VNĐ";
                title = "⭐⭐ PREMIUM 2";
                items = "|8|━━ PHẦN THƯỞNG (VĨNH VIỄN) ━━\n"
                        + "|1|• 10.000 Thỏi Vàng\n"
                        + "|1|• 10 Phiếu Giảm Giá\n"
                        + "|1|• 150 Đá Bảo Vệ\n"
                        + "|2|• Cải Trang Black Gohan VĨNH VIỄN\n"
                        + "|2|• Pet Ông Già Noel VĨNH VIỄN\n"
                        + "|1|• 40 Mảnh Thẻ Tiểu Đội Trưởng Vàng\n";
            }
            case 3 -> {
                menuId = 225;
                price = "1.500.000 VNĐ";
                title = "⭐⭐⭐ PREMIUM 3";
                items = "|8|━━ PHẦN THƯỞNG (VĨNH VIỄN) ━━\n"
                        + "|1|• 20.000 Thỏi Vàng\n"
                        + "|1|• 10 Phiếu Giảm Giá\n"
                        + "|1|• 500 Đá Bảo Vệ\n"
                        + "|2|• Cải Trang Broly Hắc Vương VIP\n"
                        + "|2|• Phụ Kiện Tuần Lộc VIP\n"
                        + "|1|• 30 Hộp Sao Pha Lê VIP\n"
                        + "|1|• 5.000 Mảnh Vỡ Bông Tai Cấp 3\n";
            }
            default -> {
                menuId = 226;
                price = "2.000.000 VNĐ";
                title = "👑 PREMIUM 4 — ĐẲNG CẤP TỐI THƯỢNG";
                items = "|8|━━ PHẦN THƯỞNG (VĨNH VIỄN) ━━\n"
                        + "|1|• 30.000 Thỏi Vàng\n"
                        + "|1|• 500 Đá Bảo Vệ\n"
                        + "|2|• Cải Trang Pan VIP VĨNH VIỄN\n"
                        + "|2|• Ván Bay Rồng Thiêng VIP VĨNH VIỄN\n"
                        + "|1|• 40 Mảnh Rồng Thần Namek\n"
                        + "|1|• 20 Mảnh Thẻ Đội Trưởng Vàng\n"
                        + "|1|• 10.000 Mảnh Vỡ Bông Tai Cấp 3\n";
            }
        }

        String status = owned ? "|2|✓ BẠN ĐÃ SỞ HỮU GÓI NÀY" : "|1|Giá: " + price;

        if (owned) {
            createOtherMenu(player, menuId,
                    "|7|━━━ " + title + " ━━━\n"
                    + status + "\n"
                    + items
                    + "|7|━━━━━━━━━━━━━━━━━━",
                    "Đóng");
        } else {
            createOtherMenu(player, menuId,
                    "|7|━━━ " + title + " ━━━\n"
                    + status + "\n"
                    + items
                    + "|7|━━━━━━━━━━━━━━━━━━",
                    "Mua Ngay\n" + price, "Đóng");
        }
    }

    // ===================== HƯỚNG DẪN PREMIUM =====================
    private void showPremiumGuide(Player player) {
        createOtherMenu(player, MENU_GUIDE,
                "|7|━━ HƯỚNG DẪN PREMIUM ━━\n"
                + "|8|★ Premium là gì?\n"
                + "|1|Gói ưu đãi đặc biệt, quà VĨNH VIỄN!\n\n"
                + "|8|★ Có gì đặc biệt?\n"
                + "|2|• Cải trang VIP stats cực mạnh\n"
                + "|2|• Pet độc quyền Premium\n"
                + "|2|• Mảnh ghép hiếm + Đá Bảo Vệ\n\n"
                + "|8|★ Cách mua?\n"
                + "|1|1. Nạp VNĐ qua Ông Gohan (ATM/QR)\n"
                + "|1|2. Quay lại ToriBot → Chọn gói\n"
                + "|1|3. Xác nhận mua → Nhận quà ngay!\n\n"
                + "|8|★ Lưu ý:\n"
                + "|1|Mỗi gói mua 1 lần, không cần gói thấp\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                "Xem Gói\nPremium", "Đóng");
    }

    // ===================== TRẠNG THÁI VIP =====================
    private void showVipStatus(Player player) {
        String status;
        if (player.vip == 0) {
            status = "|1|Bạn chưa mua gói Premium nào.\n"
                    + "|8|Hãy mua Premium để nhận quà VĨNH VIỄN\n"
                    + "|8|và trải nghiệm đẳng cấp chiến binh!";
        } else {
            status = "|2|✓ Premium hiện tại: " + player.vip + "\n"
                    + "|8|Cảm ơn bạn đã ủng hộ server!\n"
                    + "|1|Bạn có thể nâng cấp lên gói cao hơn\n"
                    + "|1|để nhận thêm nhiều phần thưởng giá trị.";
        }

        boolean p1 = PlayerDAO.checkPremium(player, 1);
        boolean p2 = PlayerDAO.checkPremium(player, 2);
        boolean p3 = PlayerDAO.checkPremium(player, 3);
        boolean p4 = PlayerDAO.checkPremium(player, 4);

        createOtherMenu(player, 3422,
                "|7|━━━ TRẠNG THÁI PREMIUM ━━━\n"
                + status + "\n\n"
                + (p1 ? "|2|✓ " : "|1|✗ ") + "Premium 1\n"
                + (p2 ? "|2|✓ " : "|1|✗ ") + "Premium 2\n"
                + (p3 ? "|2|✓ " : "|1|✗ ") + "Premium 3\n"
                + (p4 ? "|2|✓ " : "|1|✗ ") + "Premium 4\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                "Đóng");
    }

    private void handleNap(Player player, int select) {
        if (select < 0 || select >= MOC_NAP_K.length)
            return;

        int mocK = MOC_NAP_K[select];
        int mocTien = mocK * 1000;
        int tv = (select + 1) * 5;

        if (player.getSession().danap < mocTien) {
            Service.gI().sendThongBaoOK(player, "Bạn chưa đủ mốc " + mocK + "K");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBaoOK(player, "Cần 1 ô trống hành trang");
            return;
        }

        PlayerDAO.subDaNap(player, mocTien);

        Item item = new Item((short) 457);
        item.quantity = tv;

        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
    }

    private void buyVip(Player player, int vip, int cost, int bag, Runnable reward) {

        if (PlayerDAO.checkPremium(player, vip)) {
            npcChat(player, "Bạn đã mua Premium " + vip + " rồi!");
            return;
        }

        if (player.getSession().cash < cost) {
            Service.gI().sendThongBaoOK(player,
                    "Số dư VNĐ chưa đủ.\nCần: " + Util.mumberToLouis(cost) + " VNĐ"
                    + "\nHiện có: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ"
                    + "\nNạp thêm tại Ông Gohan!");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < bag) {
            npcChat(player, "Cần " + bag + " ô trống hành trang để nhận quà!");
            return;
        }

        // trừ tiền trước
        PlayerDAO.subcash(player, cost, "MUA_VIP", "VIP:" + vip + " Cost:" + cost);

        // lưu đã mua
        PlayerDAO.setPremium(player, vip);

        player.vip = (byte) vip;
        player.timevip = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000;

        // nhận quà
        reward.run();

        Service.gI().sendThongBao(player,
                "🎉 Kích hoạt Premium " + vip + " thành công!\n"
                + "Quà đã được gửi vào hành trang.\n"
                + "Cảm ơn bạn đã ủng hộ server!");
    }
}