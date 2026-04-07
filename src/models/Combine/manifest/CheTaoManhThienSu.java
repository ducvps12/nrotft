package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;

public class CheTaoManhThienSu {

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 1) {
            Service.gI().sendDialogMessage(player, "Cần 1 món Thiên Sứ để chế tạo mảnh.");
            return;
        }

        Item item = player.combine.itemsCombine.get(0);
        if (item == null || getManhId(item.template.id) == -1) {
            Service.gI().sendDialogMessage(player, "Vật phẩm không hợp lệ.");
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.MENU_START_COMBINE,
                ConstFont.BOLD_BLUE + "Chế tạo Mảnh Thiên Sứ?",
                "Đồng ý",
                "Từ chối");
    }

    public static void cheTao(Player player) {
        if (player.combine.itemsCombine.size() != 1) {
            return;
        }

        Item item = player.combine.itemsCombine.get(0);
        int manhId = getManhId(item.template.id);

        if (item == null || manhId == -1) {
            Service.gI().sendServerMessage(player, "Không thể thực hiện");
            return;
        }

        InventoryService.gI().subQuantityItemsBag(player, item, 1);

        Item manh = ItemService.gI().createNewItem((short) manhId);
        manh.quantity = 1;

        InventoryService.gI().addItemBag(player, manh);

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBag(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    private static int getManhId(int itemId) {
        // Áo thiên sứ
        if (itemId >= 1048 && itemId <= 1050) {
            return 1066;
        }
        // Quần thiên sứ
        if (itemId >= 1051 && itemId <= 1053) {
            return 1067;
        }
        // Găng thiên sứ
        if (itemId >= 1054 && itemId <= 1056) {
            return 1070;
        }
        // Giày thiên sứ
        if (itemId >= 1057 && itemId <= 1059) {
            return 1068;
        }
        // Nhẫn thiên sứ
        if (itemId >= 1060 && itemId <= 1062) {
            return 1069;
        }
        return -1;
    }
}
