package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import utils.Util;

public class CuongHoaChanMenh {

    private static final int COST_RUBY = 1000;
    private static final int ID_CHAN_MENH = 1893;
    private static final int ID_SAO_THIENTU = 1905;
    private static final int QUANTITY_SAO = 1;

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 1) {
            Service.gI().sendDialogMessage(player, "Cần đặt Chân Mệnh vào ô nâng cấp.");
            return;
        }

        Item item = player.combine.itemsCombine.get(0);
        if (item.template.id != ID_CHAN_MENH) {
            Service.gI().sendDialogMessage(player, "Chỉ có thể cường hóa Chân Mệnh");
            return;
        }

        Item saoThienTu = InventoryService.gI().findItemBag(player, ID_SAO_THIENTU);
        int currentSao = (saoThienTu != null) ? saoThienTu.quantity : 0;
        boolean hasEnoughSao = currentSao >= QUANTITY_SAO;

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Cường hóa Chân Mệnh\n\n");
        text.append(ConstFont.BOLD_BLUE).append("Ngẫu nhiên nhận thêm:\n");
        text.append("+1-5 Sức đánh\n");
        text.append("+1-50 HP\n");
        text.append("+1-50 KI\n\n");
        
        text.append(player.inventory.ruby >= COST_RUBY ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
            .append("Cần ").append(Util.numberToMoney(COST_RUBY)).append(" Hồng Ngọc\n");
        
        text.append(hasEnoughSao ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
            .append("Cần ").append(QUANTITY_SAO).append(" Sao Thiên Tử (")
            .append(currentSao).append("/").append(QUANTITY_SAO).append(")");

        if (player.inventory.ruby < COST_RUBY || !hasEnoughSao) {
            CombineService.gI().baHatMit.createOtherMenu(
                    player,
                    ConstNpc.IGNORE_MENU,
                    text.toString(),
                    "Còn thiếu nguyên liệu");
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.MENU_START_COMBINE,
                text.toString(),
                "Cường hóa",
                "Từ chối");
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() != 1) {
            return;
        }

        Item item = player.combine.itemsCombine.get(0);
        Item saoThienTu = InventoryService.gI().findItemBag(player, ID_SAO_THIENTU);

        if (item.template.id != ID_CHAN_MENH || 
            player.inventory.ruby < COST_RUBY || 
            saoThienTu == null || 
            saoThienTu.quantity < QUANTITY_SAO) {
            Service.gI().sendThongBao(player, "Không đủ nguyên liệu hoặc Hồng Ngọc");
            return;
        }

        player.inventory.ruby -= COST_RUBY;
        InventoryService.gI().subQuantityItemsBag(player, saoThienTu, QUANTITY_SAO);
        
        applyRandomOption(item);

        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendMoney(player);
        InventoryService.gI().sendItemBag(player); 
        
        CombineService.gI().reOpenItemCombine(player);
        Service.gI().sendThongBao(player, "Nâng cấp Chân Mệnh thành công!");
    }

    private static void applyRandomOption(Item item) {
        int rd = Util.nextInt(3);
        int amount;

        switch (rd) {
            case 0 -> {
                amount = Util.nextInt(1, 5);
                addOrIncreaseOption(item, 0, amount); // 0 là Sức đánh
            }
            case 1 -> {
                amount = Util.nextInt(1, 50);
                addOrIncreaseOption(item, 6, amount); // 6 là HP
            }
            case 2 -> {
                amount = Util.nextInt(1, 50);
                addOrIncreaseOption(item, 7, amount); // 7 là KI
            }
        }
    }

    private static void addOrIncreaseOption(Item item, int optionId, int value) {
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == optionId) {
                io.param += value;
                return;
            }
        }

        item.itemOptions.add(new Item.ItemOption(optionId, value));
    }
}