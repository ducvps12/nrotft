package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import java.util.List;

public class CheTaoTrangBiThanhQuang {

    private static final int REQUIRED_ITEMS = 5;

    public static void showInfoCombine(Player player) {
        List<Item> items = player.combine.itemsCombine;

        if (items.size() != REQUIRED_ITEMS) {
            Service.gI().sendDialogMessage(player, "Cần 1 món Thiên Sứ làm gốc và 4 món Thiên Sứ làm nguyên liệu.");
            return;
        }

        Item itemGoc = items.get(0);
        int trangBiDichId = getTrangBiDichId(itemGoc.template.id);
        if (trangBiDichId == -1) {
            Service.gI().sendDialogMessage(player, "Món đầu tiên phải là trang bị Thiên Sứ!");
            return;
        }

        for (int i = 1; i < items.size(); i++) {
            if (getTrangBiDichId(items.get(i).template.id) == -1) {
                Service.gI().sendDialogMessage(player, "Vật phẩm thứ " + (i + 1) + " không phải đồ Thiên Sứ!");
                return;
            }
        }

        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.MENU_START_COMBINE,
                ConstFont.BOLD_BLUE + "Chế Tạo Trang Bị Thanh Quang?\n"
                + ConstFont.BOLD_GREEN + "Gốc: " + itemGoc.template.name + "\n"
                + "Phí: 4 món Thiên Sứ phôi",
                "Đồng ý",
                "Hủy");
    }

    public static void cheTao(Player player) {
        List<Item> items = player.combine.itemsCombine;

        if (items.size() != REQUIRED_ITEMS) return;

        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendServerMessage(player, "Hành trang không đủ chỗ trống");
            return;
        }

        Item itemGoc = items.get(0);
        int trangBiDichId = getTrangBiDichId(itemGoc.template.id);
        if (trangBiDichId == -1) return;

        Item trangBiMoi = ItemService.gI().createNewItem((short) trangBiDichId);
        
        setupOptions(trangBiMoi);

        for (int i = 0; i < REQUIRED_ITEMS; i++) {
            Item it = items.get(i);
            InventoryService.gI().subQuantityItemsBag(player, it, 1);
        }

        InventoryService.gI().addItemBag(player, trangBiMoi);
        InventoryService.gI().sendItemBag(player);

        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendServerMessage(player, "Chế tạo thành công " + trangBiMoi.template.name);
        
        player.combine.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }

    private static void setupOptions(Item item) {

        switch (item.template.id) {
            case 2025: // Áo Thanh Quang
                item.itemOptions.add(new Item.ItemOption(47, 3000)); // Giáp +2000
                break;
            case 2026: // Quần Thanh Quang
                item.itemOptions.add(new Item.ItemOption(22, 300)); // HP +50.000
                break;
            case 2028: // Găng Thanh Quang
                item.itemOptions.add(new Item.ItemOption(0, 30000));  // Sức đánh +5000
                break;
            case 2027: // Giày Thanh Quang
                item.itemOptions.add(new Item.ItemOption(23, 300)); // KI +50.000
                break;
            case 2029: // Nhẫn Thanh Quang
                item.itemOptions.add(new Item.ItemOption(14, 30));   // Chí mạng +15%
                break;
        }
        item.itemOptions.add(new Item.ItemOption(21, 200)); // Yêu cầu 100 tỷ sức mạnh
        item.itemOptions.add(new Item.ItemOption(30, 0));   // Không thể giao dịch
    }

    private static int getTrangBiDichId(int nguyenLieuId) {
        switch (nguyenLieuId) {
            case 1048: case 1049: case 1050: return 2025;
            case 1051: case 1052: case 1053: return 2026;
            case 1054: case 1055: case 1056: return 2028;
            case 1057: case 1058: case 1059: return 2027;
            case 1060: case 1061: case 1062: return 2029;
            default: return -1;
        }
    }
}