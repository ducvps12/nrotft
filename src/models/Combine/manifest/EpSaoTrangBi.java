package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;

public class EpSaoTrangBi {

    public static int getGem(int star) {
        return switch (star) {
            case 7 -> 200;
            case 8 -> 300;
            default -> 10;
        };
    }

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần 1 trang bị có lỗ sao pha lê và 1 loại ngọc để ép vào.");
            return;
        }
        Item trangBi = null;
        Item daPhaLe = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.canPhaLeHoa()) {
                trangBi = item;
            } else if (item.isDaPhaLeEpSao()) {
                daPhaLe = item;
            }
        }
        if (trangBi == null || !trangBi.isNotNullItem() || daPhaLe == null || !daPhaLe.isNotNullItem()) {
            Service.gI().sendDialogMessage(player, "Cần 1 trang bị có lỗ sao pha lê và 1 loại ngọc để ép vào.");
            return;
        }
        
        int star = trangBi.getOptionParam(102); // Số sao đã ép
        int starEmpty = trangBi.getOptionParam(107); // Tổng số lỗ xanh

        // ĐÃ BỎ CHẶN: Không còn kiểm tra star < 7 hay star >= 7 đối với loại đá
        
        if (star >= starEmpty) {
            Service.gI().sendDialogMessage(player, "Trang bị đã đầy sao.");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append(trangBi.template.name).append("\n");
        text.append(ConstFont.BOLD_DARK).append(trangBi.getOptionInfo(daPhaLe)).append("\n");
        text.append(player.inventory.getGemAndRuby() < getGem(star) ? ConstFont.BOLD_RED : ConstFont.BOLD_BLUE)
            .append("Cần ").append(getGem(star)).append(" ngọc");

        if (player.inventory.getGemAndRuby() < getGem(star)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu\n" + (getGem(star) - player.inventory.getGemAndRuby()) + " ngọc");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(),
                "Nâng cấp\n" + getGem(star) + " ngọc", "Từ chối");
    }

    public static void epSaoTrangBi(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            return;
        }
        Item trangBi = null;
        Item daPhaLe = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.canPhaLeHoa()) {
                trangBi = item;
            } else if (item.isDaPhaLeEpSao()) {
                daPhaLe = item;
            }
        }
        if (trangBi == null || !trangBi.isNotNullItem() || daPhaLe == null || !daPhaLe.isNotNullItem()) {
            return;
        }
        
        int star = trangBi.getOptionParam(102);
        int starEmpty = trangBi.getOptionParam(107);

        // ĐÃ SỬA: Bỏ các điều kiện liên quan đến isDaPhaLeMoi() và isDaPhaLeCu()
        if (star >= starEmpty || player.inventory.getGemAndRuby() < getGem(star)) {
            return;
        }

        trangBi.addOptionParam(102, 1);

        // Logic nạm: Nếu là lỗ thứ 8 trở đi (star >= 7) thì add mới Option
        // Nếu là lỗ 1-7 thì cộng dồn vào Option có sẵn (addOptionParam)
        if (star >= 7) {
            if (star == 7) {
                trangBi.itemOptions.add(new Item.ItemOption(218, 0)); // Option phân tách các sao cấp cao
            }
            trangBi.itemOptions.add(new Item.ItemOption(
                    daPhaLe.getOptionDaPhaLe().optionTemplate.id,
                    daPhaLe.getOptionDaPhaLe().param));
        } else {
            trangBi.addOptionParam(
                    daPhaLe.getOptionDaPhaLe().optionTemplate.id,
                    daPhaLe.getOptionDaPhaLe().param);
        }

        player.inventory.subGemAndRuby(getGem(star));
        InventoryService.gI().subQuantityItemsBag(player, daPhaLe, 1);
        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}