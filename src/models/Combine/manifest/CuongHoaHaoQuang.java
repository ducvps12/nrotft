package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

public class CuongHoaHaoQuang {

    private static final long GOLD_COST = 500_000_000L; // 500 triệu vàng
    private static final int ITEM_HAO_QUANG_ID = 2005; // Giữ ID 1893 nếu là cùng loại linh vực/hào quang, hoặc đổi theo ý bạn
    private static final int ITEM_NGUYEN_LIEU_ID = 2004; // Đá hào quang hoặc vật phẩm ID 2004

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() < 2) {
            Service.gI().sendThongBao(player, "Cần vật phẩm Hào Quang và Đá Hào Quang (ID 2004)");
            return;
        }

        Item haoQuang = player.combine.itemsCombine.get(0);
        Item material = getMaterial(player);

        if (haoQuang == null || haoQuang.template.id != ITEM_HAO_QUANG_ID) {
            Service.gI().sendThongBao(player, "Ô đầu tiên phải là vật phẩm Hào Quang phù hợp");
            return;
        }

        if (material == null) {
            Service.gI().sendThongBao(player, "Thiếu nguyên liệu Đá Hào Quang (ID: 2004)");
            return;
        }

        if (player.inventory.gold < GOLD_COST) {
            Service.gI().sendThongBao(player, "Không đủ vàng, cần " + Util.numberToMoney(GOLD_COST));
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(
            player,
            ConstNpc.MENU_START_COMBINE,
            "|2|CƯỜNG HÓA HÀO QUANG\n"
          + "|7|Vật phẩm: " + haoQuang.template.name + "\n"
          + "|1|Chỉ số tăng: +1-5 SĐ, +1-50 HP, +1-50 KI\n"
          + "|7|Nguyên liệu: 1 " + material.template.name + " (ID 2004)\n"
          + "|1|Phí nâng cấp: " + Util.numberToMoney(GOLD_COST) + " vàng",
            "Nâng cấp",
            "Từ chối"
        );
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() < 2) return;

        Item haoQuang = player.combine.itemsCombine.get(0);
        Item material = getMaterial(player);

        if (haoQuang == null || haoQuang.template.id != ITEM_HAO_QUANG_ID || material == null) {
            return;
        }

        if (player.inventory.gold < GOLD_COST) {
            Service.gI().sendThongBao(player, "Không đủ vàng");
            return;
        }

        // --- Thực hiện cường hóa ---
        player.inventory.gold -= GOLD_COST;
        InventoryService.gI().subQuantityItemsBag(player, material, 1);

        int random = Util.nextInt(0, 2);
        switch (random) {
            case 0:
                addOrUpdateOption(haoQuang, 0, Util.nextInt(1,5)); // SĐ
                break;
            case 1:
                addOrUpdateOption(haoQuang, 6, Util.nextInt(1,50)); // HP
                break;
            case 2:
                addOrUpdateOption(haoQuang, 7, Util.nextInt(1,50)); // KI
                break;
        }

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);

        Service.gI().sendThongBao(player, "Cường hóa Hào Quang thành công!");
    }

    private static Item getMaterial(Player player) {
        for (int i = 1; i < player.combine.itemsCombine.size(); i++) {
            Item it = player.combine.itemsCombine.get(i);
            if (it != null && it.template.id == ITEM_NGUYEN_LIEU_ID) {
                return it;
            }
        }
        return null;
    }

    private static void addOrUpdateOption(Item item, int optionId, int value) {
        boolean exists = false;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == optionId) {
                io.param += value;
                exists = true;
                break;
            }
        }
        if (!exists) {
            item.itemOptions.add(new Item.ItemOption(ItemService.gI().getItemOptionTemplate(optionId), value));
        }
    }
}