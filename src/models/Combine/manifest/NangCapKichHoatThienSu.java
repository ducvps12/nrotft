package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

public class NangCapKichHoatThienSu {

    private static final int[][] THIEN_SU_ITEMS = {
            { 1048, 1051, 1054, 1057, 1060 },
            { 1049, 1052, 1055, 1058, 1061 },
            { 1050, 1053, 1056, 1059, 1062 }
    };

    public static boolean isDoHuyDiet(Item item) {
        return item != null && item.template != null
                && item.template.id >= 650 && item.template.id <= 662;
    }

    public static boolean isKichHoat(Item item) {
        if (item == null || item.itemOptions == null) {
            return false;
        }
        for (ItemOption op : item.itemOptions) {
            if (op == null || op.optionTemplate == null)
                continue;
            int id = op.optionTemplate.id;
            if ((id >= 127 && id <= 144) || (id >= 250 && id <= 254)) {
                return true;
            }
        }
        return false;
    }

    public static boolean macDuSetHuyDiet(Player player) {
        int count = 0;
        for (Item item : player.inventory.itemsBody) {
            if (isDoHuyDiet(item)) {
                count++;
            }
        }
        return count >= 5;
    }

    // ================== CHECK & HIỂN THỊ ==================
    public static void showInfoCombine(Player player) {

        if (player.combine.itemsCombine.size() != 3) {
            Service.gI().sendThongBaoOK(player, "Cần đúng 3 trang bị Hủy Diệt");
            return;
        }

        Item base = player.combine.itemsCombine.get(0);

        if (base == null || base.template == null) {
            Service.gI().sendThongBaoOK(player, "Item lỗi");
            return;
        }

        // chỉ cần 3 món là Hủy Diệt
        for (Item item : player.combine.itemsCombine) {
            if (!isDoHuyDiet(item)) {
                Service.gI().sendThongBaoOK(player, "Phải là 3 trang bị Hủy Diệt");
                return;
            }
        }

        if (!macDuSetHuyDiet(player)) {
            Service.gI().sendThongBaoOK(player, "Phải mặc đủ 5 món Hủy Diệt");
            return;
        }

        player.combine.goldCombine = 1_000_000_000;

        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.MENU_START_COMBINE,
                "Nâng cấp Thiên Sứ\n20% thành công",
                "Nâng cấp\n1 Tỷ vàng",
                "Từ chối");
    }

    // ================== THỰC HIỆN ==================
    public static void startCombine(Player player) {

        if (player.combine.itemsCombine.size() != 3) {
            return;
        }

        int gold = player.combine.goldCombine;

        if (player.inventory.gold < gold) {
            Service.gI().sendThongBao(player, "Không đủ vàng");
            return;
        }

        Item base = player.combine.itemsCombine.get(0);
        if (base == null || base.template == null) {
            return;
        }

        int gender = base.template.gender;
        int type = base.template.type;

        player.inventory.gold -= gold;

        // XÓA 3 MÓN
        for (Item item : player.combine.itemsCombine) {
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
        }

        player.combine.itemsCombine.clear();

        // 20%
        if (Util.isTrue(100, 100)) {

            int id = THIEN_SU_ITEMS[gender][type];
            Item newItem = ItemService.gI().otpts((short) id, 1);

            // RANDOM kích hoạt mới hoàn toàn
            int[] options = randOptionItemKichHoat(gender);
            newItem.itemOptions.add(new ItemOption(options[0], 0));
            newItem.itemOptions.add(new ItemOption(options[1], 0));

            InventoryService.gI().addItemBag(player, newItem);

            CombineService.gI().sendEffectSuccessCombine(player);

        } else {
            CombineService.gI().sendEffectFailCombine(player);
        }

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    // ================== RANDOM OPTION ==================
    public static int[] randOptionItemKichHoat(int gender) {

        int op1;
        int op2;

        switch (gender) {

            case 0 -> {
                if (Util.isTrue(70, 100)) {
                    op1 = 128;
                    op2 = 140;
                } else if (Util.isTrue(50, 100)) {
                    op1 = 127;
                    op2 = 139;
                } else if (Util.isTrue(40, 100)) {
                    op1 = 129;
                    op2 = 141;
                } else {
                    op1 = 250;
                    op2 = 253;
                }
            }

            case 1 -> {
                if (Util.isTrue(70, 100)) {
                    op1 = 130;
                    op2 = 142;
                } else if (Util.isTrue(50, 100)) {
                    op1 = 131;
                    op2 = 143;
                } else if (Util.isTrue(40, 100)) {
                    op1 = 251;
                    op2 = 254;
                } else {
                    op1 = 132;
                    op2 = 144;
                }
            }

            default -> {
                if (Util.isTrue(70, 100)) {
                    op1 = 134;
                    op2 = 137;
                } else if (Util.isTrue(50, 100)) {
                    op1 = 135;
                    op2 = 138;
                } else {
                    op1 = 133;
                    op2 = 136;
                }
            }
        }

        return new int[] { op1, op2 };
    }
}