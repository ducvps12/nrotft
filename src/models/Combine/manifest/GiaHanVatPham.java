/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import utils.Util;

/**
 * Gia hạn vật phẩm có hạn sử dụng (HSD)
 * Nguyên liệu: 1 Đá Hoàng Kim (ID 1723) + 1 trang bị có option 93 (HSD)
 * Chi phí: 1,000 Hồng Ngọc
 * Kết quả: 100% thành công
 *   - 50% cơ hội: +3 đến +7 ngày
 *   - 50% cơ hội: +1 ngày
 */
public class GiaHanVatPham {

    private static final int DA_HOANG_KIM_ID = 1723;
    private static final int COST_RUBY = 1000;
    private static final int OPTION_HSD = 93;

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần đặt 1 Đá Hoàng Kim + 1 trang bị có hạn sử dụng", "Đóng");
            return;
        }

        Item daHoangKim = null;
        Item itemGiaHan = null;
        for (Item item_ : player.combine.itemsCombine) {
            if (item_.template.id == DA_HOANG_KIM_ID) {
                daHoangKim = item_;
            } else if (item_.isTrangBiHSD()) {
                itemGiaHan = item_;
            }
        }

        if (daHoangKim == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần có Đá Hoàng Kim để gia hạn", "Đóng");
            return;
        }
        if (itemGiaHan == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần có trang bị có hạn sử dụng (HSD)", "Đóng");
            return;
        }

        // Kiểm tra xem trang bị có option HSD (93) không
        boolean hasHSD = false;
        int currentHSD = 0;
        for (ItemOption itopt : itemGiaHan.itemOptions) {
            if (itopt.optionTemplate.id == OPTION_HSD) {
                hasHSD = true;
                currentHSD = itopt.param;
                break;
            }
        }

        if (!hasHSD) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Trang bị này không có hạn sử dụng!", "Đóng");
            return;
        }

        // Tạo nội dung thông tin
        String npcSay = "|1|=== GIA HẠN VẬT PHẨM ===\n"
                + "|7|Trang bị: " + itemGiaHan.template.name + "\n|2|";
        for (Item.ItemOption io : itemGiaHan.itemOptions) {
            npcSay += io.getOptionString() + "\n";
        }
        npcSay += "\n|1|Sau khi gia hạn:\n"
                + "|2|• 50% cơ hội: +3 đến +7 ngày\n"
                + "|7|• 50% cơ hội: +1 ngày\n"
                + "|1|Tỉ lệ thành công: |2|100%\n";

        if (player.inventory.ruby >= COST_RUBY) {
            npcSay += "|2|Chi phí: " + Util.numberToMoney(COST_RUBY) + " hồng ngọc";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Gia hạn\n" + Util.numberToMoney(COST_RUBY) + " HN", "Từ chối");
        } else {
            int thieu = COST_RUBY - player.inventory.ruby;
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    npcSay + "|6|Bạn còn thiếu " + Util.numberToMoney(thieu) + " Hồng Ngọc", "Đóng");
        }
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendThongBao(player, "Thiếu nguyên liệu");
            return;
        }

        // Tìm Đá Hoàng Kim và trang bị HSD
        Item daHoangKim = null;
        Item tbiHSD = null;
        for (Item item_ : player.combine.itemsCombine) {
            if (item_.isNotNullItem() && item_.template.id == DA_HOANG_KIM_ID) {
                daHoangKim = item_;
            } else if (item_.isNotNullItem() && item_.isTrangBiHSD()) {
                tbiHSD = item_;
            }
        }

        if (daHoangKim == null) {
            Service.gI().sendThongBao(player, "Thiếu Đá Hoàng Kim");
            return;
        }
        if (tbiHSD == null) {
            Service.gI().sendThongBao(player, "Thiếu trang bị có HSD");
            return;
        }

        // Kiểm tra option 93
        boolean hasHSD = false;
        for (ItemOption itopt : tbiHSD.itemOptions) {
            if (itopt.optionTemplate.id == OPTION_HSD) {
                hasHSD = true;
                break;
            }
        }
        if (!hasHSD) {
            Service.gI().sendThongBao(player, "Không phải trang bị có hạn sử dụng");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }

        // Kiểm tra ruby
        if (player.inventory.ruby < COST_RUBY) {
            Service.gI().sendThongBao(player, "Cần " + Util.numberToMoney(COST_RUBY) + " hồng ngọc để gia hạn");
            return;
        }

        // Trừ ruby
        player.inventory.ruby -= COST_RUBY;

        // Gia hạn: 50% +3~7 ngày, 50% +1 ngày
        int addDays;
        if (Util.isTrue(50, 100)) {
            addDays = Util.nextInt(3, 7);
        } else {
            addDays = 1;
        }

        for (ItemOption itopt : tbiHSD.itemOptions) {
            if (itopt.optionTemplate.id == OPTION_HSD) {
                itopt.param += addDays;
                break;
            }
        }

        // Trừ Đá Hoàng Kim
        InventoryService.gI().subQuantityItemsBag(player, daHoangKim, 1);

        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendThongBao(player, "|2|Gia hạn thành công!\nĐã thêm +" + addDays + " ngày cho " + tbiHSD.template.name);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        player.combine.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }

}
