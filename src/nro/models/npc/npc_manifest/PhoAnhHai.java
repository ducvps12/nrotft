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
                            + "Chi phí: 50.000.000 vàng\n"
                            + "Tỷ lệ trúng: 30%\n\n"
                            + "Phần thưởng khi trúng:\n"
                            + "x3 Gói Quà Đặc Biệt\n"
                            + "(Mở ra: Thỏi Vàng, Cải Trang,\n"
                            + "Hồng Ngọc, Ngọc Rồng...)\n\n"
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
                            + "2. Sờ Cậu Vàng (50M vàng)\n"
                            + "   → 30% trúng 3 Gói Quà!\n"
                            + "3. Hốt Cậu Vàng bằng Thống Long\n"
                            + "4. Mở Gói Quà Đặc Biệt\n"
                            + "   → nhận phần thưởng CỰC PHẨM!\n\n"
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

    // ===== SỜ CẬU VÀNG =====
    private void soCauVang(Player player) {
        long cost = 50_000_000;

        if (player.inventory.gold < cost) {
            Service.gI().sendThongBao(player, "Bạn cần 50.000.000 vàng!\nHiện có: "
                + String.format("%,d", player.inventory.gold) + " vàng");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 3) {
            Service.gI().sendThongBao(player, "Cần ít nhất 3 ô trống trong hành trang!");
            return;
        }

        // Trừ vàng
        player.inventory.gold -= cost;
        Service.gI().sendMoney(player);

        // 30% trúng
        int rand = Util.nextInt(1, 100);

        if (rand <= 30) {
            // TRÚNG! Tặng 3 Gói Quà Đặc Biệt
            for (int i = 0; i < 3; i++) {
                Item goiQua = ItemService.gI().createNewItem((short) 1184);
                goiQua.itemOptions.add(new ItemOption(93, 30)); // HSD 30 ngày
                goiQua.itemOptions.add(new ItemOption(30, 0));  // Khóa
                InventoryService.gI().addItemBag(player, goiQua);
            }
            InventoryService.gI().sendItemBag(player);

            Service.gI().sendThongBao(player,
                    "CẬU VÀNG THÍCH BẠN!\n\n"
                    + "Nhận được x3 Gói Quà Đặc Biệt!\n\n"
                    + "Mở gói quà để nhận:\n"
                    + "Thỏi Vàng, Cải Trang VIP,\n"
                    + "Hồng Ngọc, Ngọc Rồng...\n\n"
                    + "Chúc mừng bạn!");

            // Bonus: 10% chance thêm Thỏi Vàng trực tiếp
            if (Util.isTrue(10, 100) && InventoryService.gI().getCountEmptyBag(player) > 0) {
                Item thoiVang = ItemService.gI().createNewItem((short) 457);
                thoiVang.itemOptions.add(new ItemOption(30, 0));
                InventoryService.gI().addItemBag(player, thoiVang);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "BONUS! Cậu Vàng tặng thêm 1 Thỏi Vàng!");
                nro.server.ServerNotify.gI().notify(
                    player.name + " sờ Cậu Vàng trúng JACKPOT Thỏi Vàng!");
            }
        } else if (rand <= 50) {
            // 20%: An ủi - tặng vàng lại 1 phần
            long refund = Util.nextInt(5_000_000, 20_000_000);
            player.inventory.gold += refund;
            Service.gI().sendMoney(player);
            Service.gI().sendThongBao(player,
                    "Cậu Vàng ngủ gật...\n"
                    + "Nhưng rơi ra " + String.format("%,d", refund) + " vàng!\n"
                    + "Thử lại lần nữa nhé!");
        } else {
            // 50%: Trượt
            Service.gI().sendThongBao(player,
                    "Cậu Vàng không thèm nhìn bạn...\n\n"
                    + "Chúc may mắn lần sau!\n"
                    + "Tip: Thử sờ vào lúc đêm khuya\n"
                    + "tỷ lệ cao hơn đấy (đùa thôi)");
        }
    }

    // ===== BẢNG THƯỞNG =====
    private void showBangThuong(Player player) {
        createOtherMenu(player, MENU_BANG_THUONG,
                "BẢNG THƯỞNG PHỞ ANH HAI\n\n"
                + "Gói Quà Đặc Biệt khi mở:\n"
                + "25% Vàng 200K-1M\n"
                + "20% Hồng Ngọc 100-300\n"
                + "15% Thỏi Vàng 1-2 cái\n"
                + "15% Đá Xanh Lam\n"
                + "13% Cải Trang VIP (30 ngày)\n"
                + "8%  Ngọc Rồng\n"
                + "4%  Capsule CT VIP (JACKPOT)\n"
                + "+40% bonus Thống Long!\n\n"
                + "Sờ Cậu Vàng (50M vàng):\n"
                + "30% x3 Gói Quà + bonus!\n"
                + "20% hoàn vàng 5-20M",
                "Quay lại", "Đóng");
    }
}
