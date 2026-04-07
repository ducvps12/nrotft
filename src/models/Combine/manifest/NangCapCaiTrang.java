package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;

import utils.Util;

public class NangCapCaiTrang {

    private static final long COST = 500_000_000L;

    public static void showInfoCombine(Player player) {

        if (player.combine.itemsCombine.size() < 2) {
            CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.IGNORE_MENU,
                "Cần ít nhất 2 cải trang:\n"
              + "• Ô đầu: cải trang cần nâng cấp\n"
              + "• Ô sau: cải trang làm nguyên liệu",
                "Đóng"
            );
            return;
        }

        Item baseItem = player.combine.itemsCombine.get(0);

        if (baseItem == null || !(baseItem.template.type == 5
                || baseItem.template.type == 32
                || baseItem.template.type == 23
                || baseItem.template.type == 24
                || baseItem.template.type == 11
                || baseItem.template.type == 27
                || baseItem.template.type == 21
                || baseItem.template.type == 35
                || baseItem.template.type == 36
                || baseItem.template.type == 98
                || baseItem.template.type == 72)) {
            CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.IGNORE_MENU,
                "Hãy đặt cải trang cần nâng cấp vào ô đầu tiên",
                "Đóng"
            );
            return;
        }

        boolean hasMaterial = false;
        for (int i = 1; i < player.combine.itemsCombine.size(); i++) {
            Item it = player.combine.itemsCombine.get(i);
            if (it != null && it.template.type == 50) {
                hasMaterial = true;
                break;
            }
        }

        if (!hasMaterial) {
            CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.IGNORE_MENU,
                "Cần ít nhất 1 cải trang làm nguyên liệu",
                "Đóng"
            );
            return;
        }

        if (player.inventory.gold < COST) {
            CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.IGNORE_MENU,
                "Không đủ vàng (cần "
              + Util.numberToMoney(COST) + ")",
                "Đóng"
            );
            return;
        }

        int currentSD = 0;
        for (Item.ItemOption op : baseItem.itemOptions) {
            if (op.optionTemplate.id == 0) {
                currentSD = op.param;
                break;
            }
        }

        CombineService.gI().baHatMit.createOtherMenu(
            player,
            ConstNpc.MENU_START_COMBINE,
            "|2|NÂNG CẤP CẢI TRANG\n"
          + "|7|Cải trang: " + baseItem.template.name + "\n"
          + "|1|SĐ hiện tại: " + currentSD + "\n"
          + "|1|Sau khi nâng: +" + (currentSD + 10) + " SĐ\n"
          + "|7|Tiêu hao: 1 cải trang nguyên liệu\n"
          + "|1|Phí: " + Util.numberToMoney(COST) + " vàng",
            "Nâng cấp",
            "Từ chối"
        );
    }

    public static void startCombine(Player player) {

        if (player.combine.itemsCombine.size() < 2) {
            Service.gI().sendThongBao(player,
                "Cần ít nhất 1 cải trang làm nguyên liệu");
            return;
        }

        Item baseItem = player.combine.itemsCombine.get(0);

        if (baseItem == null || !(baseItem.template.type == 5
                || baseItem.template.type == 32
                || baseItem.template.type == 23
                || baseItem.template.type == 24
                || baseItem.template.type == 11
                || baseItem.template.type == 27
                || baseItem.template.type == 21
                || baseItem.template.type == 35
                || baseItem.template.type == 36
                || baseItem.template.type == 98
                || baseItem.template.type == 72)) {
            Service.gI().sendThongBao(player,
                "Hãy đặt cải trang cần nâng cấp vào ô đầu tiên");
            return;
        }

        Item material = null;
        for (int i = 1; i < player.combine.itemsCombine.size(); i++) {
            Item it = player.combine.itemsCombine.get(i);
            if (it != null && it.template.type == 50) {
                material = it;
                break;
            }
        }

        if (material == null) {
            Service.gI().sendThongBao(player,
                "Cần cải trang làm nguyên liệu");
            return;
        }

        if (player.inventory.gold < COST) {
            Service.gI().sendThongBao(player,
                "Không đủ vàng để nâng cấp");
            return;
        }

        player.inventory.gold -= COST;

        boolean found = false;
        for (Item.ItemOption op : baseItem.itemOptions) {
            if (op.optionTemplate.id == 0) {
                op.param += 10;
                found = true;
                break;
            }
        }

        if (!found) {
            baseItem.itemOptions.add(
                new Item.ItemOption(
                    ItemService.gI().getItemOptionTemplate(0),
                    10
                )
            );
        }

        InventoryService.gI().subQuantityItemsBag(player, material, 1);

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);

        Service.gI().sendThongBao(player,
            "Nâng cấp cải trang thành công +10 SĐ");
    }
}
