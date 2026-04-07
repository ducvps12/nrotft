package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

public class NangCapSaoPhaLe {

    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Cần 1 ô trống trong hành trang.");
            Service.gI().hideWaitDialog(player);
            return;
        }
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần 1 đá Hematite và 1 Sao Pha Lê cấp 1");
            return;
        }
        Item hematite = null;
        Item saoPhaLeC1 = null;

        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == 1423 || item.template.id == 1441) {
                hematite = item;
            } else if (item.isDaPhaLeC1()) {
                saoPhaLeC1 = item;
            }
        }

        if (hematite == null || saoPhaLeC1 == null) {
            Service.gI().sendDialogMessage(player, "Cần 1 đá Hematite và 1 Sao Pha Lê cấp 1");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Nâng cấp Sao Pha lê lên cấp 2\n");
        text.append(ConstFont.BOLD_GREEN).append("Cần 1 Hematite\n");
        text.append(ConstFont.BOLD_GREEN).append("Cần 1 ").append(saoPhaLeC1.template.name).append("\n");
        text.append(ConstFont.BOLD_GREEN).append("Tỉ lệ thành công: 50%\n");
        text.append(player.inventory.gold >= 100_000_000 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 100 Tr vàng\n");
        text.append(player.inventory.getGemAndRuby() >= 50 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 50 ngọc");

        if (player.inventory.getGemAndRuby() < 50) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + Util.numberToMoney(50 - player.inventory.getGemAndRuby()) + " ngọc");
            return;
        }
        if (player.inventory.gold < 100_000_000) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + Util.numberToMoney(100_000_000 - player.inventory.gold) + " vàng");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(), "Làm phép", "Từ chối");
    }

    public static void nangCapSaoPhaLe(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            return;
        }
        if (player.combine.itemsCombine.size() != 2) {
            return;
        }
        Item hematite = null;
        Item saoPhaLeC1 = null;

        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == 1423 || item.template.id == 1441) {
                hematite = item;
            } else if (item.isDaPhaLeC1()) {
                saoPhaLeC1 = item;
            }
        }

        if (hematite == null || saoPhaLeC1 == null) {
            return;
        }
        if (player.inventory.getGemAndRuby() < 50 || player.inventory.gold < 100_000_000) {
            return;
        }

        CombineService.gI().baHatMit.npcChat(player, "Bư cô lô, ba cô la, bư ra bư zô...");

        if (Util.isTrue(50, 100)) {
            // Tạo item mới dựa trên item cũ
            Item saoPhaLeC2 = ItemService.gI().createNewItem((short) (saoPhaLeC1.template.id + 975));
            saoPhaLeC2.quantity = 1;
            
            // Xử lý Options
            if (saoPhaLeC1.template.id == 444) {
                for (Item.ItemOption option : saoPhaLeC1.itemOptions) {
                    int newParam = option.param + 2;
                    saoPhaLeC2.itemOptions.add(new Item.ItemOption(option.optionTemplate.id, newParam));
                }

            } else {
                // Các ID khác: Giữ nguyên option cũ và cộng thêm 5 đơn vị vào param
                for (Item.ItemOption option : saoPhaLeC1.itemOptions) {
                    int newParam = option.param + 5;
                    saoPhaLeC2.itemOptions.add(new Item.ItemOption(option.optionTemplate.id, newParam));
                }
            }

            CombineService.gI().sendEffectCombineItem(player, (byte) 7, (short) saoPhaLeC2.template.iconID, (short) -1);
            InventoryService.gI().addItemBag(player, saoPhaLeC2);

            Util.setTimeout(() -> {
                Service.gI().sendServerMessage(player, "Bạn nhận được " + saoPhaLeC2.template.name);
                CombineService.gI().baHatMit.npcChat(player, "Chúc mừng con nhé");
            }, 2000);
        } else {
            CombineService.gI().sendEffectCombineItem(player, (byte) 8, (short) -1, (short) -1);
            Util.setTimeout(() -> {
                CombineService.gI().baHatMit.npcChat(player, "Chúc con may mắn lần sau, đừng buồn con nhé");
            }, 2000);
        }

        // Trừ phí và nguyên liệu
        player.inventory.subGemAndRuby(50);
        player.inventory.gold -= 100_000_000;
        InventoryService.gI().subQuantityItemsBag(player, hematite, 1);
        InventoryService.gI().subQuantityItemsBag(player, saoPhaLeC1, 1);
        
        // Cập nhật giao diện
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}