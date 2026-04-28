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

public class NangCapKichHoatVip {

    private static final int[][] THAN_LINH_ITEMS = {
            { 555, 556, 562, 563, 561 },
            { 557, 558, 564, 565, 561 },
            { 559, 560, 566, 567, 561 },
            { 559, 560, 566, 567, 561 }  // Majin reuse Xayda
    };
    private static final int[][] HUY_DIET_ITEMS = {
            { 650, 651, 657, 658, 656 }, // gender 0
            { 652, 653, 659, 660, 656 }, // gender 1
            { 654, 655, 661, 662, 656 }, // gender 2
            { 654, 655, 661, 662, 656 }  // gender 3 Majin reuse Xayda
    };

    // check item thần linh
    public static boolean isThanLinh(Item item) {
        if (item == null || item.template == null) {
            return false;
        }
        for (int[] arr : THAN_LINH_ITEMS) {
            for (int id : arr) {
                if (item.template.id == id) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isVeHuyDiet(int id) {
        return id >= 2035 && id <= 2039;
    }

    public static boolean isDoHuyDiet(Item item) {
        if (item == null || item.template == null) {
            return false;
        }
        return item.template.id >= 650 && item.template.id <= 662;
    }

    // check có option kích hoạt
    public static boolean isKichHoat(Item item) {

        if (item == null) {
            return false;
        }

        for (ItemOption op : item.itemOptions) {

            int id = op.optionTemplate.id;

            if (id >= 127 && id <= 144) {
                return true;
            }

            if (id >= 250 && id <= 254) {
                return true;
            }
        }

        return false;
    }

    // check mặc đủ 5 món thần linh
    public static boolean isMacDuSet(Player player) {

        int count = 0;

        for (Item item : player.inventory.itemsBody) {

            if (isThanLinh(item)) {
                count++;
            }
        }

        return count >= 5;
    }

    public static void showInfoCombine(Player player) {

        if (player.combine.itemsCombine.size() != 3) {
            Service.gI().sendThongBaoOK(player,
                    "Cần:\n1 Thần Linh\n1 Vé Hủy Diệt\n1 Trang bị Hủy Diệt");
            return;
        }

        Item base = null;
        Item ve = null;
        Item hd = null;

        for (Item item : player.combine.itemsCombine) {

            if (isThanLinh(item)) {
                base = item;
            } else if (item != null && item.template != null && isVeHuyDiet(item.template.id)) {
                ve = item;
            } else if (isDoHuyDiet(item)) {
                hd = item;
            }
        }

        if (base == null || ve == null || hd == null) {
            Service.gI().sendThongBaoOK(player, "Sai nguyên liệu");
            return;
        }

        if (!isMacDuSet(player)) {
            Service.gI().sendThongBaoOK(player, "Phải mặc đủ 5 món Thần Linh");
            return;
        }

        int type = base.template.type;

        if (ve.template.id != 2035 + type) {
            Service.gI().sendThongBaoOK(player, "Vé không đúng loại trang bị");
            return;
        }

        player.combine.goldCombine = 500_000_000;

        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.MENU_START_COMBINE,
                "Nâng cấp Set Hủy Diệt\n50% giữ Thần Linh\n10% lên Hủy Diệt\n40% fail",
                "Nâng cấp\n500M vàng",
                "Từ chối");
    }

    public static void startCombine(Player player) {

        if (player.combine.itemsCombine.size() != 3) {
            return;
        }

        int gold = player.combine.goldCombine;

        if (player.inventory.gold < gold) {
            Service.gI().sendThongBao(player, "Không đủ vàng");
            return;
        }

        Item base = null;
        Item ve = null;
        Item hd = null;

        for (Item item : player.combine.itemsCombine) {

            if (isThanLinh(item)) {
                base = item;
            } else if (item != null && item.template != null && isVeHuyDiet(item.template.id)) {
                ve = item;
            } else if (isDoHuyDiet(item)) {
                hd = item;
            }
        }

        if (base == null || ve == null || hd == null) {
            return;
        }

        int gender = base.template.gender;
        int type = base.template.type;

        player.inventory.gold -= gold;

        // trừ vé + hd
        InventoryService.gI().subQuantityItemsBag(player, ve, 1);
        InventoryService.gI().subQuantityItemsBag(player, hd, 1);

        player.combine.itemsCombine.clear();

        int rate = Util.nextInt(100);

        // ===== 50% giữ Thần Linh =====
        if (rate < 50) {

            // tạo lại item thần linh chuẩn từ hàm của bạn
            Item newBase = ItemService.gI().otptl((short) base.template.id, 1);

            // random kích hoạt
            int[] ops = randOptionItemKichHoat(gender);
            newBase.itemOptions.add(new ItemOption(ops[0], 0));
            newBase.itemOptions.add(new ItemOption(ops[1], 0));

            // xóa base cũ và add lại item mới
            InventoryService.gI().subQuantityItemsBag(player, base, 1);
            InventoryService.gI().addItemBag(player, newBase);

            CombineService.gI().sendEffectSuccessCombine(player);
        }

        // ===== 10% lên Hủy Diệt =====
        else if (rate < 60) {

            int gIdx = Math.min(gender, 3);
            int id = HUY_DIET_ITEMS[gIdx][type];
            Item newItem = ItemService.gI().otphd((short) id, 1);

            // random kích hoạt mới
            int[] ops = randOptionItemKichHoat(gender);
            newItem.itemOptions.add(new ItemOption(ops[0], 0));
            newItem.itemOptions.add(new ItemOption(ops[1], 0));

            InventoryService.gI().addItemBag(player, newItem);

            CombineService.gI().sendEffectSuccessCombine(player);
        }

        // ===== 40% fail =====
        else {
            CombineService.gI().sendEffectFailCombine(player);
        }

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    public static int[] randOptionItemKichHoat(int gender) {

        int[][] options = {
                { 128, 140 }, { 127, 139 }, { 129, 141 },
                { 130, 142 }, { 131, 143 }, { 132, 144 },
                { 134, 137 }, { 135, 138 }, { 133, 136 }
        };

        int index = Util.nextInt(3);

        if (gender == 0) {
            return options[index];
        }

        if (gender == 1) {
            return options[index + 3];
        }

        return options[index + 6];
    }

    public static void copyOptionKichHoat(Item from, Item to) {

        for (ItemOption op : from.itemOptions) {

            int id = op.optionTemplate.id;

            if ((id >= 127 && id <= 144) || (id >= 250 && id <= 254)) {

                to.itemOptions.add(new ItemOption(id, op.param));
            }
        }
    }
}