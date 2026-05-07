package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import models.Combine.CombineService;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import utils.Util;

/**
 * Hiến tế trang bị tại Champa
 * Tốn: 10 Thỏi Vàng
 *
 * === TỈ LỆ KHÔNG CÓ HỘP SKH THẦN LINH ===
 * - 40% Thành công: Tăng 10-20% chỉ số gốc (SD, HP, KI, Giáp...)
 * - 30% Hỏng: Giảm 10-20% chỉ số gốc (chỉ số không xuống dưới 1)
 * - 30% Gãy: MẤT TRẮNG trang bị (item bị xóa khỏi hành trang)
 *
 * === TỈ LỆ CÓ 1 HỘP SKH THẦN LINH (ID 1703) ===
 * - 50% Thành công: Tăng 3-5% chỉ số gốc
 * - 50% Hỏng: Giảm 3-5% chỉ số gốc
 * -  0% Gãy: KHÔNG BAO GIỜ mất trang bị
 *
 * === TỈ LỆ CÓ 10 HỘP SKH THẦN LINH ===
 * - 80% Thành công: Tăng 3-5% chỉ số gốc
 * - 20% Hỏng: Giảm 3-5% chỉ số gốc
 * -  0% Gãy: KHÔNG BAO GIỜ mất trang bị
 *
 * GIỚI HẠN CHỈ SỐ TỐI ĐA:
 * - Sức đánh %: tối đa 15%
 * - HP %: tối đa 30%
 * - KI %: tối đa 30%
 *
 * Lưu ý: Hộp SKH Thần Linh sẽ bị tiêu hao sau khi hiến tế (dù thành công hay thất bại)
 */
public class ChampaHienTe {

    private static final int COST_THOI_VANG = 10;
    private static final int ITEM_THOI_VANG = 457;
    private static final int ITEM_HOP_SKH = 1703;

    // Số lượng hộp SKH để đạt tỉ lệ cao (80/20)
    private static final int HOP_SKH_CAO = 10;

    // Giới hạn chỉ số tối đa từ hiến tế
    private static final int MAX_SUC_DANH_PERCENT = 15;  // Giới hạn sức đánh % (option 50)
    private static final int MAX_HP_PERCENT = 30;         // Giới hạn HP % (option 22, 77)
    private static final int MAX_KI_PERCENT = 30;         // Giới hạn KI % (option 23, 103)

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.isEmpty()) {
            Service.gI().sendThongBao(player, "Hãy đặt trang bị vào để hiến tế!");
            return;
        }
        if (player.combine.itemsCombine.size() > 2) {
            Service.gI().sendThongBao(player, "Chỉ có thể đặt tối đa 2 vật phẩm (1 Trang bị + 1 Hộp SKH Thần Linh)!");
            return;
        }

        Item equip = null;
        Item hopSkh = null;

        for (Item item : player.combine.itemsCombine) {
            if (isValidItem(item)) equip = item;
            else if (item.template.id == ITEM_HOP_SKH) hopSkh = item;
        }

        if (equip == null) {
            Service.gI().sendThongBao(player, "Không tìm thấy trang bị hợp lệ để hiến tế!");
            return;
        }
        if (player.combine.itemsCombine.size() == 2 && hopSkh == null) {
            Service.gI().sendThongBao(player, "Vật phẩm hỗ trợ chỉ có thể là Hộp SKH Thần Linh!");
            return;
        }

        // Tìm thỏi vàng trong hành trang để hiển thị số lượng
        Item thoiVang = InventoryService.gI().findItemBag(player, ITEM_THOI_VANG);
        int slTV = thoiVang != null ? thoiVang.quantity : 0;

        String npcSay = "|1|=== HIẾN TẾ TRANG BỊ ===\n"
                + "|7|Ngươi muốn hiến tế " + equip.template.name + "?\n\n";

        if (hopSkh != null) {
            int soHop = hopSkh.quantity;
            if (soHop >= HOP_SKH_CAO) {
                // 10+ Hộp SKH: tỉ lệ 80/20, không mất đồ
                npcSay += "|1|Hỗ trợ từ " + soHop + " Hộp SKH Thần Linh:\n"
                        + "|2|• 80% Thành công (Tăng 3-5% chỉ số)\n"
                        + "|7|• 20% Hỏng (Giảm 3-5% chỉ số)\n"
                        + "|2|• Tỉ lệ Gãy: 0% (Không mất trang bị)\n"
                        + "|6|• Tiêu hao: " + HOP_SKH_CAO + " Hộp SKH\n";
            } else {
                // 1-9 Hộp SKH: tỉ lệ 50/50, không mất đồ
                npcSay += "|1|Hỗ trợ từ " + soHop + " Hộp SKH Thần Linh:\n"
                        + "|7|• 50% Thành công (Tăng 3-5% chỉ số)\n"
                        + "|7|• 50% Hỏng (Giảm 3-5% chỉ số)\n"
                        + "|2|• Tỉ lệ Gãy: 0% (Không mất trang bị)\n"
                        + "|6|• Tiêu hao: 1 Hộp SKH\n";
            }
        } else {
            // Không có Hộp SKH: rủi ro cao nhưng thưởng lớn hơn
            npcSay += "|1|Tỉ lệ rủi ro (Không hỗ trợ):\n"
                    + "|2|• 40% Thành công (Tăng 10-20% chỉ số)\n"
                    + "|7|• 30% Hỏng (Giảm 10-20% chỉ số)\n"
                    + "|6|• 30% Gãy (MẤT TRẮNG TRANG BỊ)\n"
                    + "\n|1|Mẹo: Thêm Hộp SKH Thần Linh để loại bỏ tỉ lệ gãy!\n";
        }

        npcSay += "\n|7|Giới hạn chỉ số tối đa:\n"
                + "|1|• Sức đánh: " + MAX_SUC_DANH_PERCENT + "%\n"
                + "|1|• HP/KI: " + MAX_HP_PERCENT + "%\n";

        npcSay += "\n|1|Phí hiến tế: |2|" + COST_THOI_VANG + " Thỏi Vàng\n"
                + "|1|Hiện có: |" + (slTV >= COST_THOI_VANG ? "2|" : "6|") + slTV + " TV";

        // Dùng NPC đang chọn (Champa) thay vì baHatMit để menu popup đúng NPC
        Npc npcChose = player.iDMark.getNpcChose();
        if (npcChose != null) {
            npcChose.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Hiến tế\n(" + COST_THOI_VANG + " TV)", "Không");
        } else {
            // Fallback nếu không tìm được NPC đang chọn
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Hiến tế\n(" + COST_THOI_VANG + " TV)", "Không");
        }
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.isEmpty() || player.combine.itemsCombine.size() > 2) {
            return;
        }

        Item equip = null;
        Item hopSkh = null;

        for (Item item : player.combine.itemsCombine) {
            if (isValidItem(item)) equip = item;
            else if (item.template.id == ITEM_HOP_SKH) hopSkh = item;
        }

        if (equip == null) {
            Service.gI().sendThongBao(player, "Không tìm thấy trang bị hợp lệ để hiến tế!");
            return;
        }
        if (player.combine.itemsCombine.size() == 2 && hopSkh == null) {
            Service.gI().sendThongBao(player, "Vật phẩm hỗ trợ không hợp lệ!");
            return;
        }

        Item thoiVang = InventoryService.gI().findItemBag(player, ITEM_THOI_VANG);
        if (thoiVang == null || thoiVang.quantity < COST_THOI_VANG) {
            Service.gI().sendThongBao(player, "Không đủ Thỏi Vàng để hiến tế!");
            return;
        }

        // Trừ phí
        InventoryService.gI().subQuantityItemsBag(player, thoiVang, COST_THOI_VANG);

        int rand = Util.nextInt(1, 100);

        if (hopSkh != null) {
            int soHop = hopSkh.quantity;

            if (soHop >= HOP_SKH_CAO) {
                // CÓ 10+ HỘP SKH THẦN LINH (80% Thành công, 20% Hỏng, 0% Gãy)
                InventoryService.gI().subQuantityItemsBag(player, hopSkh, HOP_SKH_CAO);

                int percent = Util.nextInt(3, 5);
                if (rand <= 80) {
                    modifyItemOptions(equip, percent, true);
                    CombineService.gI().sendEffectSuccessCombine(player);
                    Service.gI().sendThongBao(player, "|2|HIẾN TẾ THÀNH CÔNG!\nTrang bị được cường hóa thêm " + percent + "% sức mạnh!\n(Dùng 10 Hộp SKH)");
                } else {
                    modifyItemOptions(equip, percent, false);
                    CombineService.gI().sendEffectFailCombine(player);
                    Service.gI().sendThongBao(player, "|7|HIẾN TẾ THẤT BẠI!\nTrang bị bị suy yếu đi " + percent + "% sức mạnh!\n(Dùng 10 Hộp SKH)");
                }
            } else {
                // CÓ 1-9 HỘP SKH THẦN LINH (50% Thành công, 50% Hỏng, 0% Gãy)
                InventoryService.gI().subQuantityItemsBag(player, hopSkh, 1);

                int percent = Util.nextInt(3, 5);
                if (rand <= 50) {
                    modifyItemOptions(equip, percent, true);
                    CombineService.gI().sendEffectSuccessCombine(player);
                    Service.gI().sendThongBao(player, "|2|HIẾN TẾ THÀNH CÔNG!\nTrang bị được cường hóa thêm " + percent + "% sức mạnh!");
                } else {
                    modifyItemOptions(equip, percent, false);
                    CombineService.gI().sendEffectFailCombine(player);
                    Service.gI().sendThongBao(player, "|7|HIẾN TẾ THẤT BẠI!\nTrang bị bị suy yếu đi " + percent + "% sức mạnh!");
                }
            }
        } else {
            // KHÔNG CÓ HỘP (40% Thành công, 30% Hỏng, 30% Gãy)
            if (rand <= 40) {
                int percent = Util.nextInt(10, 20);
                modifyItemOptions(equip, percent, true);
                CombineService.gI().sendEffectSuccessCombine(player);
                Service.gI().sendThongBao(player, "|2|HIẾN TẾ THÀNH CÔNG!\nTrang bị được cường hóa thêm " + percent + "% sức mạnh!");
            } else if (rand <= 70) {
                int percent = Util.nextInt(10, 20);
                modifyItemOptions(equip, percent, false);
                CombineService.gI().sendEffectFailCombine(player);
                Service.gI().sendThongBao(player, "|7|HIẾN TẾ THẤT BẠI!\nTrang bị bị suy yếu đi " + percent + "% sức mạnh!");
            } else {
                CombineService.gI().sendEffectFailCombine(player);
                Service.gI().sendThongBao(player, "|6|HIẾN TẾ THẤT BẠI!\nTrang bị của bạn đã tan vỡ thành cát bụi!");
                InventoryService.gI().subQuantityItemsBag(player, equip, 1);
                player.combine.itemsCombine.remove(equip);
            }
        }

        InventoryService.gI().sendItemBag(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    // Kiểm tra xem item có phải là trang bị có thể hiến tế không
    private static boolean isValidItem(Item item) {
        if (item == null || item.template == null) return false;
        int type = item.template.type;
        // Chấp nhận Áo, Quần, Găng, Giày, Radar (0-4), Cải trang (5)
        if (type >= 0 && type <= 5) {
            // Trang bị phải có option chỉ số
            if (item.itemOptions != null && !item.itemOptions.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Chỉnh sửa chỉ số của trang bị (có giới hạn tối đa)
    private static void modifyItemOptions(Item item, int percent, boolean isIncrease) {
        if (item.itemOptions == null) return;
        
        for (ItemOption opt : item.itemOptions) {
            int opId = opt.optionTemplate.id;
            // Chỉ tác động vào các option tăng chỉ số sức mạnh cơ bản
            if (isBasicStatOption(opId)) {
                int delta = (opt.param * percent) / 100;
                if (delta < 1) delta = 1; // Tối thiểu thay đổi 1
                
                if (isIncrease) {
                    opt.param += delta;
                    // === GIỚI HẠN TỐI ĐA ===
                    if (opId == 50 && opt.param > MAX_SUC_DANH_PERCENT) {
                        opt.param = MAX_SUC_DANH_PERCENT; // Max Sức đánh %
                    }
                    if (opId == 22 && opt.param > MAX_HP_PERCENT) {
                        opt.param = MAX_HP_PERCENT; // Max HP %
                    }
                    if (opId == 77 && opt.param > MAX_HP_PERCENT) {
                        opt.param = MAX_HP_PERCENT; // Max HP %
                    }
                    if (opId == 23 && opt.param > MAX_KI_PERCENT) {
                        opt.param = MAX_KI_PERCENT; // Max KI %
                    }
                    if (opId == 103 && opt.param > MAX_KI_PERCENT) {
                        opt.param = MAX_KI_PERCENT; // Max KI %
                    }
                } else {
                    opt.param -= delta;
                    if (opt.param < 1) opt.param = 1; // Không cho âm
                }
            }
        }
    }

    private static boolean isBasicStatOption(int id) {
        // 47: Giáp, 6: HP, 7: KI, 0: Sức đánh, 14: Chí mạng, 22: HP %, 23: KI %, 50: Sức đánh %
        // 77: HP/KI %
        // Các option liên quan tới sức mạnh
        return id == 47 || id == 6 || id == 7 || id == 0 || id == 14 
            || id == 22 || id == 23 || id == 50 || id == 77 || id == 103 || id == 97;
    }
}
