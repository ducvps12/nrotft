package models.Combine.manifest;

import java.util.ArrayList;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

public class NangCapSachTuyetKy {

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ 1 và 10 Kìm bấm giấy.");
            return;
        }
        Item sachTuyetKy = null;
        Item kimBamGiay = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.isSachTuyetKy()) {
                sachTuyetKy = item;
            } else if (item.template.id == 1285) {
                kimBamGiay = item;
            }
        }
        if (sachTuyetKy == null || kimBamGiay == null) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ 1 và 10 Kìm bấm giấy.");
            return;
        }
        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Nâng cấp sách tuyệt kỹ\n");
        text.append(kimBamGiay.quantity >= 10 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                .append("Cần 10 Kìm bấm giấy\n");
        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: 50%\n");
        text.append(ConstFont.BOLD_BLUE).append("Nâng cấp thất bại sẽ mất 10 Kìm bấm giấy");
        if (kimBamGiay.quantity < 10) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu\n" + (10 - kimBamGiay.quantity) + " Kìm bấm giấy");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(),
                "Nâng cấp", "Từ chối");
    }

    public static void nangCapSachTuyetKy(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            return;
        }

        Item sachTuyetKy = null;
        Item kimBamGiay = null;

        for (Item item : player.combine.itemsCombine) {
            if (item.isSachTuyetKy()) {
                sachTuyetKy = item;
            } else if (item.template.id == 1285) {
                kimBamGiay = item;
            }
        }

        if (sachTuyetKy == null || kimBamGiay == null) {
            return;
        }

        if (kimBamGiay.quantity < 10) {
            Service.gI().sendThongBao(player, "Không đủ 10 Kìm bấm giấy");
            return;
        }

        boolean success = Util.isTrue(50, 100);

        if (success) {
            short newId = -1;

            switch (sachTuyetKy.template.id) {
                case 1044 -> newId = 1278;
                case 1211 -> newId = 1279;
                case 1212 -> newId = 1280;
            }

            if (newId != -1) {
                Item newItem = ItemService.gI().createNewItem(newId);

                if (newItem.itemOptions == null) {
                    newItem.itemOptions = new ArrayList<>();
                } else {
                    newItem.itemOptions.clear();
                }
                for (Item.ItemOption oldOpt : sachTuyetKy.itemOptions) {
                    if (oldOpt != null) {
                        int newParam = oldOpt.param + 5;

                        newItem.itemOptions.add(
                                new Item.ItemOption(oldOpt.optionTemplate.id, newParam));
                    }
                }

                InventoryService.gI().subQuantityItemsBag(player, sachTuyetKy, 1);
                InventoryService.gI().addItemBag(player, newItem);

                CombineService.gI().sendEffectSuccessCombine(player);
                Service.gI().sendThongBao(player, "Nâng cấp sách thành công");
            }
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player, "Nâng cấp thất bại!");
        }

        InventoryService.gI().subQuantityItemsBag(player, kimBamGiay, 10);
        InventoryService.gI().sendItemBag(player);
        CombineService.gI().reOpenItemCombine(player);
    }

}
