package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

/**
 * Chức năng Cường hóa Chiến Hồn
 * Item chính: 2031
 * Nguyên liệu: 2032
 * Phí: 10,000 Ruby
 */
public class CuongHoaChienHon {

    private static final int RUBY_COST = 10_000;
    private static final int ITEM_CHIEN_HON_ID = 2031;
    private static final int ITEM_NGUYEN_LIEU_ID = 2032;

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() < 2) {
            Service.gI().sendThongBao(player, "Cần Chiến Hồn (ID 2031) và Đá Chiến Hồn (ID 2032)");
            return;
        }

        Item chienHon = player.combine.itemsCombine.get(0);
        Item material = getMaterial(player);

        if (chienHon == null || chienHon.template.id != ITEM_CHIEN_HON_ID) {
            Service.gI().sendThongBao(player, "Vật phẩm chính phải là Chiến Hồn");
            return;
        }

        if (material == null) {
            Service.gI().sendThongBao(player, "Thiếu nguyên liệu Đá Chiến Hồn (ID 2032)");
            return;
        }

        if (player.inventory.ruby < RUBY_COST) {
            Service.gI().sendThongBao(player, "Không đủ Hồng Ngọc, cần " + Util.numberToMoney(RUBY_COST));
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(
            player,
            ConstNpc.MENU_START_COMBINE,
            "|2|CƯỜNG HÓA CHIẾN HỒN\n"
          + "|7|Vật phẩm: " + chienHon.template.name + "\n"
          + "|1|Chỉ số tăng: +1-5 SĐ, +1-50 HP, +1-50 KI\n"
          + "|7|Nguyên liệu: 1 " + material.template.name + "\n"
          + "|1|Phí nâng cấp: " + Util.numberToMoney(RUBY_COST) + " Hồng Ngọc",
            "Nâng cấp",
            "Từ chối"
        );
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() < 2) return;

        Item chienHon = player.combine.itemsCombine.get(0);
        Item material = getMaterial(player);

        if (chienHon == null || chienHon.template.id != ITEM_CHIEN_HON_ID || material == null) {
            return;
        }

        if (player.inventory.ruby < RUBY_COST) {
            Service.gI().sendThongBao(player, "Không đủ Hồng Ngọc");
            return;
        }

        // --- Thực hiện cường hóa ---
        player.inventory.ruby -= RUBY_COST;
        InventoryService.gI().subQuantityItemsBag(player, material, 1);

        // Random tăng chỉ số (Sức đánh, HP, hoặc KI)
        int random = Util.nextInt(0, 2);
        switch (random) {
            case 0:
                addOrUpdateOption(chienHon, 0, Util.nextInt(1,5)); // SĐ
                break;
            case 1:
                addOrUpdateOption(chienHon, 6, Util.nextInt(1,50)); // HP
                break;
            case 2:
                addOrUpdateOption(chienHon, 7, Util.nextInt(1,50)); // KI
                break;
        }

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player); // Cập nhật lại Ruby/Vàng hiển thị
        CombineService.gI().reOpenItemCombine(player);

        Service.gI().sendThongBao(player, "Cường hóa Chiến Hồn thành công!");
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