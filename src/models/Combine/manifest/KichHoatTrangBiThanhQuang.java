package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import utils.Util;

public class KichHoatTrangBiThanhQuang {

    private static final int COST_HONG_NGOC = 1000;

    private static final int[] THANH_QUANG_OPTIONS = {
            129, 141, 127, 139, 128, 140,
            251, 254, 132, 144, 130, 142,
            135, 138, 133, 136, 134, 137
    };

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 1) {
            Service.gI().sendDialogMessage(player, "Cần 1 trang bị Thanh Quang để kích hoạt.");
            return;
        }

        Item item = player.combine.itemsCombine.get(0);
        if (item == null || item.template.id < 2025 || item.template.id > 2029) {
            Service.gI().sendDialogMessage(player, "Trang bị không hợp lệ.");
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.MENU_START_COMBINE,
                ConstFont.BOLD_BLUE + "Kích Hoạt Trang Bị Thanh Quang?\nTốn 1000 Hồng Ngọc mỗi lần",
                "Nâng cấp",
                "Hủy");
    }

    public static void kichHoat(Player player) {
        if (player.combine.itemsCombine.size() != 1) {
            return;
        }

        Item item = player.combine.itemsCombine.get(0);
        if (item == null || item.template.id < 2025 || item.template.id > 2029) {
            Service.gI().sendServerMessage(player, "Không thể thực hiện");
            return;
        }

        if (player.inventory.ruby < COST_HONG_NGOC) {
            Service.gI().sendServerMessage(player, "Bạn không đủ Hồng Ngọc");
            return;
        }

        player.inventory.ruby -= COST_HONG_NGOC;
        Service.gI().sendMoney(player);

        int[] pair = getRandomOptionPair(player.gender);
        if (pair == null) {
            Service.gI().sendServerMessage(player, "Không thể kích hoạt");
            return;
        }

        removeOldThanhQuangOptions(item);

        item.itemOptions.add(new Item.ItemOption(pair[0], 0));
        item.itemOptions.add(new Item.ItemOption(pair[1], 0));

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBag(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    private static void removeOldThanhQuangOptions(Item item) {
        for (int i = item.itemOptions.size() - 1; i >= 0; i--) {
            int id = item.itemOptions.get(i).optionTemplate.id;
            for (int tq : THANH_QUANG_OPTIONS) {
                if (id == tq) {
                    item.itemOptions.remove(i);
                    break;
                }
            }
        }
    }

    private static int[] getRandomOptionPair(int gender) {
        int rd = Util.nextInt(3);

        if (gender == 0) {
            if (rd == 0)
                return new int[] { 129, 141 };
            if (rd == 1)
                return new int[] { 127, 139 };
            return new int[] { 128, 140 };
        }

        if (gender == 1) {
            if (rd == 0)
                return new int[] { 251, 254 };
            if (rd == 1)
                return new int[] { 132, 144 };
            return new int[] { 130, 142 };
        }

        if (gender == 2) {
            if (rd == 0)
                return new int[] { 135, 138 };
            if (rd == 1)
                return new int[] { 133, 136 };
            return new int[] { 134, 137 };
        }

        return null;
    }
}
