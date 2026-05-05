package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import event.EventManager;
import item.Item;
import item.Item.ItemOption;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.PlayerService;
import nro.services.Service;
import shop.ShopService;
import utils.Util;

public class PhoAnhHai extends Npc {

    // Menu IDs
    private static final int MENU_SO_CAU_VANG = 111;
    private static final int MENU_HOT_CAU_VANG = 222;
    private static final int MENU_GIOI_THIEU = 333;
    private static final int MENU_BANG_THUONG = 444;

    public PhoAnhHai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
        if (EventManager.PHO_ANH_HAI) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Chào " + player.name + "!\n"
                    + "Phở Anh Hai xin kính chào quý khách!\n"
                    + "Ghé quán thưởng thức phở nóng\n"
                    + "và nhận quà hấp dẫn nhé!",
                    "Cửa hàng",
                    "Sờ cậu vàng",
                    "Hốt cậu vàng",
                    "Giới thiệu",
                    "Bảng thưởng",
                    "Đóng");
        } else {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Quán đang đóng cửa...\n"
                    + "Hẹn gặp lại khi sự kiện mở nhé!",
                    "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        // ===== MENU CHÍNH =====
        if (player.iDMark.isBaseMenu()) {
            if (!EventManager.PHO_ANH_HAI) return;
            switch (select) {
                case 0: // Cửa hàng
                    ShopService.gI().opendShop(player, "DA_BAC_PHO", false);
                    break;
                case 1: // Sờ cậu vàng
                    createOtherMenu(player, MENU_SO_CAU_VANG,
                            "Sờ Cậu Vàng\n\n"
                            + "Chi phí: 100.000.000 vàng\n"
                            + "Tỷ lệ trúng: 15%\n\n"
                            + "Phần thưởng khi trúng:\n"
                            + "x1 Gói Quà Đặc Biệt\n"
                            + "(Mở ra: Vàng, Hồng Ngọc,\n"
                            + "Thỏi Vàng, Đá Xanh...)\n\n"
                            + "Bạn có muốn thử vận may?",
                            "Sờ ngay!", "Để sau");
                    break;
                case 2: // Hốt cậu vàng
                    createOtherMenu(player, MENU_HOT_CAU_VANG,
                            "Hốt Cậu Vàng\n\n"
                            + "Cần 1 Thống Long trong hành trang\n"
                            + "(Chi Chi có bán)\n\n"
                            + "Dùng Thống Long để hốt cậu vàng\n"
                            + "nhận phần thưởng bí ẩn!\n\n"
                            + "Vào hành trang → chọn Thống Long\n"
                            + "→ Sử dụng để hốt!",
                            "Đã hiểu", "Quay lại");
                    break;
                case 3: // Giới thiệu
                    createOtherMenu(player, MENU_GIOI_THIEU,
                            "SỰ KIỆN PHỞ ANH HAI\n\n"
                            + "Phở Anh Hai - Đặc sản Đan Phượng\n"
                            + "Hà Nội nổi tiếng khắp vũ trụ!\n\n"
                            + "Cách chơi:\n"
                            + "1. Mua đồ tại Cửa Hàng\n"
                            + "2. Sờ Cậu Vàng (100M vàng)\n"
                            + "   → 15% trúng 1 Gói Quà!\n"
                            + "3. Hốt Cậu Vàng bằng Thống Long\n"
                            + "4. Mở Gói Quà Đặc Biệt\n"
                            + "   → nhận phần thưởng!\n\n"
                            + "Nhanh tay kẻo hết event!",
                            "Xem Bảng Thưởng", "Đóng");
                    break;
                case 4: // Bảng thưởng
                    showBangThuong(player);
                    break;
            }
            return;
        }

        // ===== MENU SỜ CẬU VÀNG =====
        if (player.iDMark.getIndexMenu() == MENU_SO_CAU_VANG) {
            if (select == 0) {
                soCauVang(player);
            }
            return;
        }

        // ===== MENU HỐT CẬU VÀNG =====
        if (player.iDMark.getIndexMenu() == MENU_HOT_CAU_VANG) {
            if (select == 1) { // Quay lại
                openBaseMenu(player);
            }
            return;
        }

        // ===== MENU GIỚI THIỆU =====
        if (player.iDMark.getIndexMenu() == MENU_GIOI_THIEU) {
            if (select == 0) { // Xem bảng thưởng
                showBangThuong(player);
            }
            return;
        }

        // ===== MENU BẢNG THƯỞNG =====
        if (player.iDMark.getIndexMenu() == MENU_BANG_THUONG) {
            if (select == 0) { // Quay lại
                openBaseMenu(player);
            }
            return;
        }
    }

    // ===== SỜ CẬU VÀNG (NERFED) =====
    private void soCauVang(Player player) {
        long cost = 100_000_000;

        if (player.inventory.gold < cost) {
            Service.gI().sendThongBao(player, "Bạn cần 100.000.000 vàng!\nHiện có: "
                + String.format("%,d", player.inventory.gold) + " vàng");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 2) {
            Service.gI().sendThongBao(player, "Cần ít nhất 2 ô trống trong hành trang!");
            return;
        }

        // Trừ vàng
        player.inventory.gold -= cost;
        Service.gI().sendMoney(player);

        // 15% trúng (giảm từ 30%)
        int rand = Util.nextInt(1, 100);

        if (rand <= 15) {
            // TRÚNG! Tặng 1 Gói Quà Đặc Biệt (giảm từ 3)
            Item goiQua = ItemService.gI().createNewItem((short) 1184);
            goiQua.itemOptions.add(new ItemOption(93, 30)); // HSD 30 ngày
            goiQua.itemOptions.add(new ItemOption(30, 0));  // Khóa
            InventoryService.gI().addItemBag(player, goiQua);
            InventoryService.gI().sendItemBag(player);

            Service.gI().sendThongBao(player,
                    "CẬU VÀNG THÍCH BẠN!\n\n"
                    + "Nhận được x1 Gói Quà Đặc Biệt!\n\n"
                    + "Mở gói quà để nhận phần thưởng!\n\n"
                    + "Chúc mừng bạn!");
            // Bỏ bonus Thỏi Vàng
        } else if (rand <= 30) {
            // 15%: An ủi - tặng vàng lại 1 phần
            long refund = Util.nextInt(5_000_000, 10_000_000);
            player.inventory.addGoldSafe(refund);
            Service.gI().sendMoney(player);
            Service.gI().sendThongBao(player,
                    "Cậu Vàng ngủ gật...\n"
                    + "Nhưng rơi ra " + String.format("%,d", refund) + " vàng!\n"
                    + "Thử lại lần nữa nhé!");
        } else {
            // 70%: Trượt
            Service.gI().sendThongBao(player,
                    "Cậu Vàng không thèm nhìn bạn...\n\n"
                    + "Chúc may mắn lần sau!");
        }
    }

    // ===== BẢNG THƯỞNG =====
    private void showBangThuong(Player player) {
        createOtherMenu(player, MENU_BANG_THUONG,
                "BẢNG THƯỞNG PHỞ ANH HAI\n\n"
                + "Gói Quà Đặc Biệt khi mở:\n"
                + "35% Vàng 100K-500K\n"
                + "25% Hồng Ngọc 30-100\n"
                + "15% Thỏi Vàng 1 cái\n"
                + "12% Đá Xanh Lam\n"
                + "8%  Cải Trang (7 ngày)\n"
                + "3%  Ngọc Rồng\n"
                + "2%  Đá Quý\n"
                + "+15% bonus Thống Long\n\n"
                + "Sờ Cậu Vàng (100M vàng):\n"
                + "15% x1 Gói Quà\n"
                + "15% hoàn vàng 5-10M",
                "Quay lại", "Đóng");
    }
}
