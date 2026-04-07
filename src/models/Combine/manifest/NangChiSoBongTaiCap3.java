package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import utils.Util;

public class NangChiSoBongTaiCap3 {

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 3) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 3, 198 mảnh hồn porata và 2 đá xanh lam.");
            return;
        }

        Item bongTai = null;
        Item manhHonBongTai = null;
        Item daXanhLam = null;

        for (Item item : player.combine.itemsCombine) {
            if (item.isNotNullItem()) {
                switch (item.template.id) {
                    case 1810 -> bongTai = item;
                    case 934 -> manhHonBongTai = item;
                    case 935 -> daXanhLam = item;
                }
            }
        }

        if (bongTai == null || manhHonBongTai == null || daXanhLam == null) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 3, 198 mảnh hồn porata và 2 đá xanh lam.");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Bông tai Porata cấp 3");
        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: 50%\n");
        text.append(manhHonBongTai.quantity >= 198 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                .append("Cần 198 Mảnh hồn bông tai\n");
        text.append(daXanhLam.quantity >= 2 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                .append("Cần 2 Đá xanh lam\n");
        text.append(player.inventory.getGemAndRuby() >= 500 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                .append("Cần 500 ngọc\n");

        if (player.inventory.getGemAndRuby() < 500) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu\n" + Util.numberToMoney(500 - player.inventory.getGemAndRuby()) + " ngọc");
            return;
        }

        if (daXanhLam.quantity < 2) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu\nĐá xanh lam");
            return;
        }

        if (manhHonBongTai.quantity < 198) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu\n" + (198 - manhHonBongTai.quantity) + " Mảnh hồn bông tai");
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                text.toString(), "Nâng cấp\n500 ngọc", "Từ chối");
    }

    // type = 1 -> nâng option 1
    // type = 2 -> nâng option 2
    public static void nangChiSoBongTai(Player player, int type) {

        if (player.combine.itemsCombine.size() != 3) {
            return;
        }

        Item bongTai = null;
        Item manhHonBongTai = null;
        Item daXanhLam = null;

        for (Item item : player.combine.itemsCombine) {
            if (item.isNotNullItem()) {
                switch (item.template.id) {
                    case 1810 -> bongTai = item;
                    case 934 -> manhHonBongTai = item;
                    case 935 -> daXanhLam = item;
                }
            }
        }

        if (bongTai == null || manhHonBongTai == null || daXanhLam == null
                || player.inventory.getGemAndRuby() < 500
                || daXanhLam.quantity < 2
                || manhHonBongTai.quantity < 198) {
            return;
        }

        if (Util.isTrue(50, 100)) {

            if (type == 1) {
           nangChiSo1(bongTai);
}

            if (type == 2) {
           nangChiSo2(bongTai);
}

            CombineService.gI().sendEffectSuccessCombine(player);

        } else {
            CombineService.gI().sendEffectFailCombine(player);
        }

        InventoryService.gI().subQuantityItemsBag(player, manhHonBongTai, 198);
        InventoryService.gI().subQuantityItemsBag(player, daXanhLam, 2);

        InventoryService.gI().sendItemBag(player);
        CombineService.gI().reOpenItemCombine(player);
    }

  private static void nangChiSo1(Item bongTai) { // day la random chi so btc3

    int[] options = {77,103,50,108,94,14,80,81,175,5};

    int option = options[Util.nextInt(options.length)];
    int param = Util.nextInt(1, 16); // random 1 -> 15

    if (bongTai.itemOptions.size() > 0) {
        bongTai.itemOptions.set(0, new Item.ItemOption(option, param));
    } else {
        bongTai.itemOptions.add(new Item.ItemOption(option, param));
    }
}
    private static void nangChiSo2(Item bongTai) {

    int[] options = {77,103,50,108,94,14,80,81,175,5};

    int option = options[Util.nextInt(options.length)];
    int param = Util.nextInt(1, 16); // random 1 -> 15

    if (bongTai.itemOptions.size() > 1) {
        bongTai.itemOptions.set(1, new Item.ItemOption(option, param));
    } else {
        bongTai.itemOptions.add(new Item.ItemOption(option, param));
    }
  }
}