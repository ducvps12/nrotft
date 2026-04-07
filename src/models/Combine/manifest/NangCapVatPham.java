package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstItem;
import consts.ConstNpc;
import consts.ConstTaskBadges;
import item.Item;
import java.time.LocalDate;
import models.Combine.CombineService;
import models.Combine.CombineUtil;
import nro.player.Player;
import nro.server.ServerNotify;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import task.Badges.BadgesTaskService;
import utils.Util;

public class NangCapVatPham {

    // Chi phí vàng cố định cho tất cả các cấp
    private static final int GOLD_COST = 50_000_000;
    // Số Đá Ngũ Sắc cần cho mỗi lần đập
    private static final int DA_NGU_SAC_REQUIRED = 20;

    private static int getGold(Item item) {
        return GOLD_COST;
    }

    private static int getDa(Item item) {
        int levelItem = item.getOptionParam(72);
        int levelOption = item.template.level;
        int level = levelItem + levelOption;
        return level + 1;
    }

    private static int getRatio(int level) {
        return switch (level) {
            case 0 ->
                80;
            case 1 ->
                50;
            case 2 ->
                20;
            case 3 ->
                10;
            case 4 ->
                7;
            case 5 ->
                5;
            case 6 ->
                3;
            case 7 ->
                5;  // +7 → +8
            case 8 ->
                4;  // +8 → +9
            case 9 ->
                3;  // +9 → +10
            case 10 ->
                2;  // +10 → +11
            case 11 ->
                1;  // +11 → +12
            default ->
                0;
        };
    }

    /**
     * Trả về % bonus SD/HP/KI khi lên cấp đó
     * +1 đến +10: mỗi cấp +1%
     * +11: +2%
     * +12: +3%
     */
    private static int getBonusIncrement(int newLevel) {
        if (newLevel >= 1 && newLevel <= 10) {
            return 1;
        } else if (newLevel == 11) {
            return 2;
        } else if (newLevel == 12) {
            return 3;
        }
        return 0;
    }

    /**
     * Trả về tổng bonus % tích lũy tại cấp đó
     * +1: 1%, +2: 2%, ..., +10: 10%, +11: 12%, +12: 15%
     */
    private static int getTotalBonus(int level) {
        if (level <= 0) return 0;
        if (level <= 10) return level;
        if (level == 11) return 12;
        if (level == 12) return 15;
        return 15;
    }

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần 1 trang bị và đúng loại đá nâng cấp");
            return;
        }

        Item trangBi = null;
        Item daNangCap = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.template.type < 5) {
                trangBi = item;
            } else if (item.isDaNangCap()) {
                daNangCap = item;
            }
        }
        if (trangBi == null || daNangCap == null || !trangBi.canNangCapWithNDC(daNangCap)) {
            Service.gI().sendDialogMessage(player, "Cần 1 trang bị và đúng loại đá nâng cấp");
            return;
        }
        Item daBaoVe = InventoryService.gI().findItemBag(player, 987);
        Item daBaoVeKhoa = InventoryService.gI().findItemBag(player, 1143);
        Item daNguSac = InventoryService.gI().findItemBag(player, ConstItem.DA_NGU_SAC);
        int level = trangBi.getOptionParam(72);
        int gold = getGold(trangBi);
        int da = getDa(trangBi);
        if (level >= CombineService.MAX_LEVEL_ITEM) {
            Service.gI().sendDialogMessage(player, "Vật phẩm đã đạt cấp độ tối đa (+12), không thể nâng cấp nữa");
            return;
        }
        // Từ +3, +5, +7 trở đi và tất cả từ +8: cần đá bảo vệ
        boolean canUseDBV = level == 2 || level == 4 || level == 6 || level >= 7;

        int bonusIncrement = getBonusIncrement(level + 1);
        int currentBonus = getTotalBonus(level);
        int nextBonus = getTotalBonus(level + 1);

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Hiện tại ").append(trangBi.template.name);
        if (level > 0) {
            text.append(" [+").append(level).append("]\n");
        } else {
            text.append("\n");
        }
        text.append(ConstFont.BOLD_DARK).append(trangBi.getOptionInfoUpgrade()).append("\n");
        if (currentBonus > 0) {
            text.append(ConstFont.BOLD_DARK).append("Bonus: +").append(currentBonus).append("% SD, HP, KI\n");
        }
        text.append(ConstFont.BOLD_BLUE).append("Sau khi nâng cấp [+").append(level + 1).append("]\n");
        text.append(ConstFont.BOLD_GREEN).append(trangBi.getOptionInfoUpgradeFinal()).append("\n");
        text.append(ConstFont.BOLD_GREEN).append("Bonus: +").append(nextBonus).append("% SD, HP, KI\n");
        int displayRatio = player.isAdmin() ? 100 : getRatio(level);
        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: ").append(displayRatio).append("%\n");

        // Hiển thị yêu cầu đá nâng cấp
        text.append(daNangCap.quantity >= da ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần ").append(da)
                .append(" ").append(daNangCap.template.name).append("\n");

        // Hiển thị yêu cầu Đá Ngũ Sắc
        int daNguSacQty = daNguSac != null ? daNguSac.quantity : 0;
        text.append(daNguSacQty >= DA_NGU_SAC_REQUIRED ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                .append("Cần ").append(DA_NGU_SAC_REQUIRED).append(" Đá Ngũ Sắc\n");

        // Hiển thị yêu cầu vàng
        text.append(player.inventory.gold >= gold ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần ")
                .append(Util.numberToMoney(gold)).append(" vàng");

        if (canUseDBV) {
            text.append("\n");
            text.append(ConstFont.BOLD_BLUE).append("Nếu thất bại sẽ rớt xuống [+").append(level - 1).append("]\n");
            text.append(ConstFont.BOLD_RED).append("Nếu dùng đá bảo vệ sẽ không bị rớt cấp.");
        }

        // Kiểm tra thiếu đá nâng cấp
        if (daNangCap.quantity < da) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    text.toString(),
                    "Còn thiếu\n" + Util.numberToMoney(da - daNangCap.quantity) + " " + daNangCap.template.name);
            return;
        }

        // Kiểm tra thiếu Đá Ngũ Sắc
        if (daNguSacQty < DA_NGU_SAC_REQUIRED) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    text.toString(),
                    "Còn thiếu\n" + (DA_NGU_SAC_REQUIRED - daNguSacQty) + " Đá Ngũ Sắc");
            return;
        }

        // Kiểm tra thiếu vàng
        if (player.inventory.gold < gold) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    text.toString(), "Còn thiếu\n" + Util.numberToMoney(gold - player.inventory.gold) + " vàng");
            return;
        }

        if (canUseDBV && (daBaoVe != null || daBaoVeKhoa != null)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    text.toString(), "Nâng cấp", "Nâng cấp\ndùng đá\nbảo vệ", "Đóng");
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                text.toString(), "Nâng cấp", "Đóng");
    }

    public static void nangCapVatPham(Player player, boolean useDBV) {
        if (player.combine.itemsCombine.size() != 2) {
            return;
        }

        Item trangBi = null;
        Item daNangCap = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.template.type < 5) {
                trangBi = item;
            } else if (item.isDaNangCap()) {
                daNangCap = item;
            }
        }
        if (trangBi == null || daNangCap == null || !trangBi.canNangCapWithNDC(daNangCap)) {
            return;
        }
        Item daBaoVe = InventoryService.gI().findItemBag(player, 987);
        Item daBaoVeKhoa = InventoryService.gI().findItemBag(player, 1143);
        Item daNguSac = InventoryService.gI().findItemBag(player, ConstItem.DA_NGU_SAC);
        int level = trangBi.getOptionParam(72);
        int gold = getGold(trangBi);
        int da = getDa(trangBi);
        int daNguSacQty = daNguSac != null ? daNguSac.quantity : 0;

        boolean canUseDBV = level == 2 || level == 4 || level == 6 || level >= 7;
        if (daNangCap.quantity < da || player.inventory.gold < gold || level >= CombineService.MAX_LEVEL_ITEM) {
            return;
        }
        // Kiểm tra đủ Đá Ngũ Sắc
        if (daNguSacQty < DA_NGU_SAC_REQUIRED) {
            return;
        }
        if (canUseDBV && useDBV && daBaoVe == null && daBaoVeKhoa == null) {
            return;
        }

        if (player.isAdmin() || Util.isTrue(getRatio(level), 100)) {
            // === THÀNH CÔNG ===
            // Tăng chỉ số trang bị (logic gốc)
            for (Item.ItemOption io : trangBi.itemOptions) {
                if (io.isOptionCanUpgrade()) {
                    io.param += (io.param * 10 / 100);
                }
            }
            // Tăng cấp
            trangBi.addOptionParam(72, 1);
            int newLevel = level + 1;

            // Thêm bonus % SD, HP, KI
            int bonusIncrement = getBonusIncrement(newLevel);
            if (bonusIncrement > 0) {
                trangBi.addOptionParam(50, bonusIncrement);   // Sức đánh +#%
                trangBi.addOptionParam(77, bonusIncrement);   // HP +#%
                trangBi.addOptionParam(103, bonusIncrement);  // KI +#%
            }

            CombineService.gI().sendEffectSuccessCombine(player);
            if (level > 1) {
                Service.gI().chatJustForMe(player, player,
                        player.name + ": Vừa nâng cấp thành công " + trangBi.template.name + " lên +" + newLevel);
            }
            if (level > 5) {
                ServerNotify.gI().notify(
                        player.name + ": Vừa nâng cấp thành công " + trangBi.template.name + " lên +" + newLevel);
            }
            if (level == 7) {
                BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.THANH_DAP_DO_7, 1);
            }
        } else {
            // === THẤT BẠI ===
            if (canUseDBV) {
                if (!useDBV || (daBaoVe == null && daBaoVeKhoa == null)) {
                    // Không dùng đá bảo vệ → rớt 1 cấp
                    if (trangBi.template.type < 5) {
                        for (Item.ItemOption io : trangBi.itemOptions) {
                            if (io.isOptionCanUpgrade()) {
                                io.param -= (io.param * 11 / 100);
                            }
                        }
                    }
                    // Trừ bonus % SD, HP, KI của cấp hiện tại
                    int bonusLost = getBonusIncrement(level);
                    if (bonusLost > 0) {
                        trangBi.subOptionParam(50, bonusLost);
                        trangBi.subOptionParam(77, bonusLost);
                        trangBi.subOptionParam(103, bonusLost);
                    }
                    trangBi.subOptionParam(72, 1);
                    trangBi.addOptionParam(209, 1);
                }
            }
            CombineService.gI().sendEffectFailCombine(player);
        }

        // Trừ đá bảo vệ nếu dùng
        if (canUseDBV && useDBV) {
            if (daBaoVe != null) {
                InventoryService.gI().subQuantityItemsBag(player, daBaoVe, 1);
            } else {
                InventoryService.gI().subQuantityItemsBag(player, daBaoVeKhoa, 1);
            }
        }

        // Trừ chi phí
        player.inventory.gold -= gold;
        InventoryService.gI().subQuantityItemsBag(player, daNangCap, da);
        // Trừ Đá Ngũ Sắc
        InventoryService.gI().subQuantityItemsBag(player, daNguSac, DA_NGU_SAC_REQUIRED);

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

}
