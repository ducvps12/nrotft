package models.Combine.manifest;

import item.Item;
import consts.ConstNpc;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.RewardService;
import nro.services.Service;
import utils.Util;

public class NangCapKichHoat {

    private static final int[][] THAN_LINH_ITEMS = {
            { 555, 556, 562, 563, 561 },
            { 557, 558, 564, 565, 561 },
            { 559, 560, 566, 567, 561 },
            { 559, 560, 566, 567, 561 }  // Majin reuse Xayda
    };

    public static boolean isThanLinh(Item item) {
        for (int[] arr : THAN_LINH_ITEMS) {
            for (int id : arr) {
                if (item.template.id == id) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void showInfoCombine(Player player) {

        if (player.combine.itemsCombine.size() != 3) {
            Service.gI().sendThongBaoOK(player, "Cần 3 trang bị thần linh");
            return;
        }

        Item base = player.combine.itemsCombine.get(0);

        if (!isThanLinh(base)) {
            Service.gI().sendThongBaoOK(player, "Nguyên liệu phải là đồ thần linh");
            return;
        }

        int type = base.template.type;

        for (Item item : player.combine.itemsCombine) {
            if (!isThanLinh(item)) {
                Service.gI().sendThongBaoOK(player, "Nguyên liệu phải là 3 đồ thần linh");
                return;
            }
        }

        player.combine.goldCombine = 500_000_000;

        String npcSay = "Dùng 3 trang bị Thần Linh chế tạo Kích Hoạt\n"
                + "Tỉ lệ thành công: 50%\n"
                + "Thành công:\n"
                + "95% Kích Hoạt thường\n"
                + "5% Kích Hoạt Thần Linh";

        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.MENU_START_COMBINE,
                npcSay,
                "Chế tạo\n500M vàng",
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

        Item base = player.combine.itemsCombine.get(0);
        if (base == null || base.template == null) {
            return;
        }

        int gender = base.template.gender;
        int type = base.template.type;

        player.inventory.gold -= gold;

        // trừ nguyên liệu
        for (Item item : player.combine.itemsCombine) {
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
        }
        player.combine.itemsCombine.clear();

        // ===== 50% fail =====
        if (!Util.isTrue(50, 100)) {
            CombineService.gI().sendEffectFailCombine(player);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendMoney(player);
            return;
        }

        Item newItem;

        // ===== 5% ra Thần Linh =====
        if (Util.isTrue(100, 100)) {

            if (type == 4) {
                // NHẪN → giữ nguyên
                newItem = ItemService.gI().createNewItem((short) base.template.id);
            } else {
                int gIdx = Math.min(gender, 3);
                int id = THAN_LINH_ITEMS[gIdx][type];
                newItem = ItemService.gI().createNewItem((short) id);
            }

        } else {

            // ===== thường =====
            if (type == 4) {
                // NHẪN → giữ nguyên
                newItem = ItemService.gI().createNewItem((short) base.template.id);
            } else {

                int[][][] items = {
                        { { 0, 33 }, { 1, 41 }, { 2, 49 }, { 2, 49 } },
                        { { 6, 35 }, { 7, 43 }, { 8, 51 }, { 8, 51 } },
                        { { 27, 30 }, { 28, 47 }, { 29, 55 }, { 29, 55 } },
                        { { 21, 24 }, { 22, 46 }, { 23, 53 }, { 23, 53 } },
                        { { 12, 57 }, { 12, 57 }, { 12, 57 }, { 12, 57 } }
                };

                if (type < 0 || type >= items.length) {
                    Service.gI().sendThongBao(player, "Không hỗ trợ loại trang bị");
                    return;
                }

                int gIdx2 = Math.min(gender, 3);
                int id = items[type][gIdx2][0];
                newItem = ItemService.gI().createNewItem((short) id);
            }
        }

        if (type != 4) {
            RewardService.gI().initBaseOptionClothes(
                    newItem.template.id,
                    newItem.template.type,
                    newItem.itemOptions);
        }

        // random kích hoạt
        int[] options = randOptionItemKichHoat(gender);
        newItem.itemOptions.add(new Item.ItemOption(options[0], 0));
        newItem.itemOptions.add(new Item.ItemOption(options[1], 0));

        InventoryService.gI().addItemBag(player, newItem);

        CombineService.gI().sendEffectSuccessCombine(player);

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

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