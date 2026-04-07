package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import utils.Util;

public class CuongHoaSachTuyetKy {

    private static final int COST_RUBY = 1000;

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 1) {
            Service.gI().sendDialogMessage(player, "Cần đặt Sách Tuyệt Kỹ vào ấn nâng cấp.");
            return;
        }

        Item item = player.combine.itemsCombine.get(0);
        if (!isSachTuyetKy(item)) {
            Service.gI().sendDialogMessage(player, "Chỉ có thể cường hóa Sách Tuyệt Kỹ.");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Cường hóa Sách Tuyệt Kỹ\n\n");
        text.append("Ngẫu nhiên nhận:\n");
        text.append("+1-5 Sức đánh\n");
        text.append("+1-50 HP\n");
        text.append("+1-50 KI\n\n");
        text.append(player.inventory.ruby >= COST_RUBY
                ? ConstFont.BOLD_BLUE
                : ConstFont.BOLD_RED)
                .append("Cần 1000 Hồng Ngọc");

        // ❌ Thiếu ruby → IGNORE_MENU
        if (player.inventory.ruby < COST_RUBY) {
            CombineService.gI().baHatMit.createOtherMenu(
                    player,
                    ConstNpc.IGNORE_MENU,
                    text.toString(),
                    "Còn thiếu\n" + (COST_RUBY - player.inventory.ruby) + " Hồng Ngọc");
            return;
        }

        // ✅ Đủ điều kiện → MENU_START_COMBINE
        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.MENU_START_COMBINE,
                text.toString(),
                "Cường hóa\n1000 Hồng Ngọc",
                "Từ chối");
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() != 1) {
            return;
        }

        Item item = player.combine.itemsCombine.get(0);
        if (!isSachTuyetKy(item) || player.inventory.ruby < COST_RUBY) {
            return;
        }

        player.inventory.ruby -= COST_RUBY;
        applyRandomOption(item);

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    private static void applyRandomOption(Item item) {
        int rd = Util.nextInt(3);
        switch (rd) {
            case 0 -> addOrIncreaseOption(item, 0, Util.nextInt(1, 5)); // SD
            case 1 -> addOrIncreaseOption(item, 6, Util.nextInt(1, 50)); // HP
            case 2 -> addOrIncreaseOption(item, 7, Util.nextInt(1, 50)); // KI
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

    private static boolean isSachTuyetKy(Item item) {
        if (item == null)
            return false;
        int id = item.template.id;
        return id == 1278 || id == 1279 || id == 1280;
    }
}
