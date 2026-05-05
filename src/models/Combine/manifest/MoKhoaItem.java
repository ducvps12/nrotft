/*
 * MoKhoaItem.java — Mở khóa giao dịch cho item bị khóa GD
 * Nguyên liệu: 1 Đá Hoàng Kim (ID 1723) + 1 item bị khóa giao dịch
 * Chi phí: 2,000 Hồng Ngọc
 * Tỉ lệ thành công: 30%
 *   - Thành công: Xóa option 30 (khóa GD), thêm option 73 (đã mở khóa)
 *   - Thất bại: Mất Đá Hoàng Kim, item vẫn giữ nguyên (vẫn bị khóa)
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

    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hành trang cần ít nhất 1 chỗ trống", "Đóng");
            return;
        }

        if (player.combine.itemsCombine.size() != 2) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần đặt 1 Đá Hoàng Kim + 1 item bị khóa giao dịch", "Đóng");
            return;
        }

        Item daHoangKim = null;
        Item itemKhoaGD = null;
        for (Item item_ : player.combine.itemsCombine) {
            if (item_.template.id == DA_HOANG_KIM_ID) {
                daHoangKim = item_;
            } else if (item_.isTrangBiKhoaGd()) {
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
                    "Cần có Item bị khóa giao dịch\n(bông tai, item sự kiện, thỏi vàng...)", "Đóng");
            return;
        }

        // Hiển thị thông tin item
        String npcSay = "|1|=== MỞ KHÓA GIAO DỊCH ===\n"
                + "|2|Item: " + itemKhoaGD.template.name + "\n|0|";
        for (Item.ItemOption io : itemKhoaGD.itemOptions) {
            if (io.optionTemplate.id != 72) {
                npcSay += io.getOptionString() + "\n";
            }
        }
        npcSay += "\n|1|Sau khi mở khóa:\n"
                + "|2|Item sẽ trở thành item giao dịch được\n"
                + "\n|7|Tỉ lệ thành công: |6|" + RATE_SUCCESS + "%\n"
                + "|7|Thất bại: mất Đá Hoàng Kim, item vẫn còn\n"
                + "|1|Chi phí: |2|" + Util.numberToMoney(COST_RUBY) + " Hồng Ngọc";

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
            } else if (item_.isNotNullItem() && item_.isTrangBiKhoaGd()) {
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
            // Thành công: Xóa option 30 (khóa GD), thêm option 73 (đã mở khóa)
            CombineService.gI().sendEffectSuccessCombine(player);

            ItemOption optionKhoa = null;
            for (ItemOption itopt : trangBiKhoaGD.itemOptions) {
                if (itopt.optionTemplate.id == 30) {
                    optionKhoa = itopt;
                    break;
                }
            }
            if (optionKhoa != null) {
                trangBiKhoaGD.itemOptions.remove(optionKhoa);
                trangBiKhoaGD.itemOptions.add(new Item.ItemOption(73, 0));
            }

            Service.gI().sendThongBao(player, "|2|MỞ KHÓA THÀNH CÔNG!\n" + trangBiKhoaGD.template.name + " đã có thể giao dịch được!");
        } else {
            // Thất bại: Mất Đá Hoàng Kim, item vẫn giữ nguyên
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player, "|7|MỞ KHÓA THẤT BẠI!\nĐá Hoàng Kim đã bị tiêu hao.\nItem vẫn giữ nguyên trạng thái khóa.");
        }

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        player.combine.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }

}
