/*
 * MoKhoaItem.java — Mở khóa giao dịch cho item bị khóa GD
 * Nguyên liệu: 1 Đá Hoàng Kim (ID 1723) + 1 item bị khóa giao dịch
 * Chi phí: 2,000 Hồng Ngọc
 * Tỉ lệ thành công: 30%
 *   - Thành công: Xóa option 30 (khóa GD)
 *   - Thất bại: Mất Đá Hoàng Kim, item vẫn giữ nguyên (vẫn bị khóa)
 *
 * QUAN TRỌNG:
 *   - Chỉ xử lý TỪNG ITEM MỘT (quantity = 1)
 *   - Không cho phép mở khóa Thỏi Vàng (ID 457) — tránh loạn kinh tế
 *   - Item PHẢI có option 30 thực sự mới được mở khóa
 */
package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import java.util.Arrays;
import java.util.List;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import utils.Util;

public class MoKhoaItem {

    private static final int DA_HOANG_KIM_ID = 1723;
    private static final int COST_RUBY = 2000;
    private static final int RATE_SUCCESS = 30; // 30%

    // Danh sách item KHÔNG ĐƯỢC MỞ KHÓA (tránh loạn kinh tế)
    private static final List<Integer> BLACKLIST_ITEM_IDS = Arrays.asList(
            457  // Thỏi Vàng — nếu mở khóa hàng loạt sẽ phá kinh tế
    );

    /**
     * Kiểm tra item có nằm trong blacklist không
     */
    private static boolean isBlacklisted(Item item) {
        return BLACKLIST_ITEM_IDS.contains((int) item.template.id);
    }

    /**
     * Kiểm tra item có thực sự bị khóa GD không (có option 30)
     */
    private static boolean hasLockOption(Item item) {
        for (ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 30) {
                return true;
            }
        }
        return false;
    }

    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hành trang cần ít nhất 1 chỗ trống", "Đóng");
            return;
        }

        if (player.combine.itemsCombine.size() != 2) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần đặt 1 Đá Hoàng Kim + 1 item bị khóa giao dịch\n"
                    + "(CHỈ MỞ KHÓA TỪNG CÁI MỘT)", "Đóng");
            return;
        }

        Item daHoangKim = null;
        Item itemKhoaGD = null;
        for (Item item_ : player.combine.itemsCombine) {
            if (item_.template.id == DA_HOANG_KIM_ID) {
                daHoangKim = item_;
            } else {
                itemKhoaGD = item_;
            }
        }

        if (daHoangKim == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần có Đá Hoàng Kim", "Đóng");
            return;
        }
        if (itemKhoaGD == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần có Item bị khóa giao dịch", "Đóng");
            return;
        }

        // Kiểm tra blacklist
        if (isBlacklisted(itemKhoaGD)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "|7|Vật phẩm này KHÔNG THỂ mở khóa giao dịch!\n"
                    + "|8|" + itemKhoaGD.template.name + " nằm trong danh sách cấm.", "Đóng");
            return;
        }

        // Kiểm tra item thực sự có option 30 (khóa GD)
        if (!hasLockOption(itemKhoaGD)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "|7|Item này KHÔNG BỊ KHÓA giao dịch!\n"
                    + "|8|" + itemKhoaGD.template.name + " đã có thể giao dịch.", "Đóng");
            return;
        }

        // Chỉ cho phép quantity = 1
        if (itemKhoaGD.quantity > 1) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "|7|Chỉ mở khóa TỪNG CÁI MỘT!\n"
                    + "|8|Hãy tách ra 1 " + itemKhoaGD.template.name + " rồi thử lại.", "Đóng");
            return;
        }

        // Hiển thị thông tin item
        String npcSay = "|7|══════════════════\n"
                + "|1|    🔓 MỞ KHÓA GIAO DỊCH\n"
                + "|7|══════════════════\n"
                + "|2|Item: " + itemKhoaGD.template.name + "\n|0|";
        for (Item.ItemOption io : itemKhoaGD.itemOptions) {
            if (io.optionTemplate.id != 72) {
                npcSay += io.getOptionString() + "\n";
            }
        }
        npcSay += "\n|7|──────────────────\n"
                + "|2|Sau mở khóa: Item giao dịch được\n"
                + "|1|Tỉ lệ: " + RATE_SUCCESS + "% thành công\n"
                + "|8|Thất bại: mất Đá HK, item còn\n"
                + "|7|Chi phí: |1|" + Util.numberToMoney(COST_RUBY) + " Hồng Ngọc\n"
                + "|7|══════════════════";

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                npcSay, "Mở Khóa\n" + Util.numberToMoney(COST_RUBY) + " HN", "Từ chối");
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendThongBao(player, "Thiếu nguyên liệu");
            return;
        }

        // Tìm items
        Item daHoangKim = null;
        Item trangBiKhoaGD = null;
        for (Item item_ : player.combine.itemsCombine) {
            if (item_.isNotNullItem() && item_.template.id == DA_HOANG_KIM_ID) {
                daHoangKim = item_;
            } else if (item_.isNotNullItem()) {
                trangBiKhoaGD = item_;
            }
        }

        if (daHoangKim == null) {
            Service.gI().sendThongBao(player, "Cần Đá Hoàng Kim");
            return;
        }
        if (trangBiKhoaGD == null) {
            Service.gI().sendThongBao(player, "Cần Item bị khóa giao dịch");
            return;
        }

        // === BẢO VỆ: Blacklist ===
        if (isBlacklisted(trangBiKhoaGD)) {
            Service.gI().sendThongBao(player, "Vật phẩm " + trangBiKhoaGD.template.name + " KHÔNG THỂ mở khóa!");
            player.combine.itemsCombine.clear();
            CombineService.gI().reOpenItemCombine(player);
            return;
        }

        // === BẢO VỆ: Item phải thực sự có option 30 ===
        if (!hasLockOption(trangBiKhoaGD)) {
            Service.gI().sendThongBao(player, "Item này không bị khóa giao dịch!");
            player.combine.itemsCombine.clear();
            CombineService.gI().reOpenItemCombine(player);
            return;
        }

        // === BẢO VỆ: Chỉ cho phép quantity = 1 ===
        if (trangBiKhoaGD.quantity > 1) {
            Service.gI().sendThongBao(player, "Chỉ mở khóa TỪNG CÁI MỘT! Hãy tách ra 1 item.");
            player.combine.itemsCombine.clear();
            CombineService.gI().reOpenItemCombine(player);
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống");
            return;
        }

        // Kiểm tra ruby
        if (player.inventory.ruby < COST_RUBY) {
            Service.gI().sendThongBao(player, "Cần " + Util.numberToMoney(COST_RUBY) + " Hồng Ngọc để mở khóa");
            return;
        }

        // Trừ ruby
        player.inventory.ruby -= COST_RUBY;

        // Trừ Đá Hoàng Kim (luôn mất dù thành công hay thất bại)
        InventoryService.gI().subQuantityItemsBag(player, daHoangKim, 1);

        if (Util.isTrue(RATE_SUCCESS, 100)) {
            // ═══ THÀNH CÔNG ═══
            CombineService.gI().sendEffectSuccessCombine(player);

            // Xóa option 30 (khóa GD)
            ItemOption optionKhoa = null;
            for (ItemOption itopt : trangBiKhoaGD.itemOptions) {
                if (itopt.optionTemplate.id == 30) {
                    optionKhoa = itopt;
                    break;
                }
            }
            if (optionKhoa != null) {
                trangBiKhoaGD.itemOptions.remove(optionKhoa);
            }

            Service.gI().sendThongBao(player,
                    "|2|🔓 MỞ KHÓA THÀNH CÔNG!\n"
                    + trangBiKhoaGD.template.name + "\nđã có thể giao dịch!");
        } else {
            // ═══ THẤT BẠI ═══
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player,
                    "|7|❌ MỞ KHÓA THẤT BẠI!\n"
                    + "Đá Hoàng Kim đã bị tiêu hao.\n"
                    + "Item vẫn giữ nguyên trạng thái khóa.");
        }

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        player.combine.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }

}
