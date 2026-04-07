package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

public class CuongHoaLinhVuc {

    private static final long GOLD_COST = 500_000_000L; // 500 triệu vàng
    private static final int ITEM_LINH_VUC_ID = 2033;
    private static final int ITEM_DA_NANG_CAP_ID = 1998;

    public static void showInfoCombine(Player player) {
        // 1. Kiểm tra số lượng đồ đặt vào
        if (player.combine.itemsCombine.size() < 2) {
            Service.gI().sendThongBao(player, "Cần Linh Vực và Đá nâng cấp (ID 1998)");
            return;
        }

        Item linhVuc = player.combine.itemsCombine.get(0);

        // 2. Kiểm tra vật phẩm chính (Linh Vực 1893)
        if (linhVuc == null || linhVuc.template.id != ITEM_LINH_VUC_ID) {
            Service.gI().sendThongBao(player, "Ô đầu tiên phải là Linh Vực (ID: 1893)");
            return;
        }

        // 3. Kiểm tra nguyên liệu (Đá nâng cấp 1998)
        Item material = getMaterial(player);
        if (material == null) {
            Service.gI().sendThongBao(player, "Thiếu Đá nâng cấp (ID: 1998)");
            return;
        }

        // 4. Kiểm tra vàng
        if (player.inventory.gold < GOLD_COST) {
            Service.gI().sendThongBao(player, "Bạn không đủ vàng, cần " + Util.numberToMoney(GOLD_COST));
            return;
        }

        // Giao diện xác nhận
        CombineService.gI().baHatMit.createOtherMenu(
            player,
            ConstNpc.MENU_START_COMBINE,
            "|2|CƯỜNG HÓA LINH VỰC\n"
          + "|7|Vật phẩm: " + linhVuc.template.name + "\n"
          + "|1|Chỉ số tăng: +1-5 SĐ, +1-50 HP, +1-50 KI\n"
          + "|7|Nguyên liệu: 1 Đá nâng cấp (1998)\n"
          + "|1|Phí cường hóa: " + Util.numberToMoney(GOLD_COST) + " vàng",
            "Cường hóa",
            "Từ chối"
        );
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() < 2) return;

        Item linhVuc = player.combine.itemsCombine.get(0);
        Item material = getMaterial(player);

        if (linhVuc == null || linhVuc.template.id != ITEM_LINH_VUC_ID || material == null) {
            return;
        }

        if (player.inventory.gold < GOLD_COST) {
            Service.gI().sendThongBao(player, "Không đủ vàng");
            return;
        }

        // --- Thực thi cường hóa ---
        player.inventory.gold -= GOLD_COST;
        InventoryService.gI().subQuantityItemsBag(player, material, 1);

        int random = Util.nextInt(0, 2);
        switch (random) {
            case 0:
                addOrUpdateOption(linhVuc, 0, Util.nextInt(1,5)); // SĐ
                break;
            case 1:
                addOrUpdateOption(linhVuc, 6, Util.nextInt(1,50)); // HP
                break;
            case 2:
                addOrUpdateOption(linhVuc, 7, Util.nextInt(1,50)); // KI
                break;
        }

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);

        Service.gI().sendThongBao(player, "Cường hóa Linh Vực thành công!");
    }

    // Hàm tìm đúng vật phẩm ID 1998 trong các ô nguyên liệu
    private static Item getMaterial(Player player) {
        for (int i = 1; i < player.combine.itemsCombine.size(); i++) {
            Item it = player.combine.itemsCombine.get(i);
            if (it != null && it.template.id == ITEM_DA_NANG_CAP_ID) {
                return it;
            }
        }
        return null;
    }

    // Hàm cập nhật hoặc thêm mới option
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