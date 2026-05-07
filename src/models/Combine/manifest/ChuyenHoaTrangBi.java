package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import models.Combine.CombineUtil;
import nro.player.Player;
import nro.server.ServerNotify;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

/**
 * Chuyển hóa trang bị: Chuyển cấp (+level) và sao pha lê từ trang bị gốc
 * sang trang bị mới cao cấp hơn 1 bậc.
 *
 * - Yêu cầu: 2 item trong tab combine:
 *   + Trang bị GỐC (đã nâng cấp từ +4 trở lên)
 *   + Trang bị MỚI (chưa nâng cấp, cùng type, level cao hơn 1 bậc)
 *
 * - Dùng vàng: tốn 500.000.000 vàng
 * - Dùng ngọc: tốn 500 ngọc
 * - Tỉ lệ: 100% (chuyển hóa luôn thành công)
 */
public class ChuyenHoaTrangBi {

    private static final long GOLD_COST = 500_000_000L;
    private static final int NGOC_COST = 500;
    private static final int MIN_LEVEL_REQUIRED = 4; // Trang bị gốc phải từ +4 trở lên

    /**
     * Hiển thị thông tin khi player bỏ item vào tab combine
     */
    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player,
                    "Cần chọn 1 trang bị gốc (đã nâng cấp +4 trở lên) và 1 trang bị mới cùng loại (chưa nâng cấp)");
            return;
        }

        Item trangBiGoc = null;
        Item trangBiMoi = null;

        for (Item item : player.combine.itemsCombine) {
            if (item.template == null || item.template.type >= 5) {
                continue;
            }
            int level = item.getOptionParam(72);
            if (level >= MIN_LEVEL_REQUIRED) {
                trangBiGoc = item;
            } else if (level == 0) {
                trangBiMoi = item;
            }
        }

        // Kiểm tra trang bị gốc
        if (trangBiGoc == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Trang bị gốc phải có cấp từ +4 trở lên", "Đóng");
            return;
        }

        // Kiểm tra trang bị mới
        if (trangBiMoi == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Trang bị mới phải chưa nâng cấp (cấp 0)", "Đóng");
            return;
        }

        // Kiểm tra cùng type (áo với áo, quần với quần,...)
        if (trangBiGoc.template.type != trangBiMoi.template.type) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hai trang bị phải cùng loại (Áo-Áo, Quần-Quần,...)", "Đóng");
            return;
        }

        // Kiểm tra trang bị mới phải cao cấp hơn 1 bậc
        if (trangBiMoi.template.level <= trangBiGoc.template.level) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Trang bị mới phải cao cấp hơn trang bị gốc 1 bậc", "Đóng");
            return;
        }

        // Lấy thông tin level gốc
        int levelGoc = trangBiGoc.getOptionParam(72);

        // Xây dựng text hiển thị
        boolean dungVang = player.combine.typeCombine == CombineService.CHUYEN_HOA_TRANG_BI_DUNG_VANG;
        StringBuilder text = new StringBuilder();

        // Trang bị gốc
        text.append(ConstFont.BOLD_BLUE).append("Trang bị gốc: ").append(trangBiGoc.template.name)
                .append(" [+").append(levelGoc).append("]\n");
        text.append(ConstFont.BOLD_DARK).append(trangBiGoc.getOptionInfo()).append("\n");

        // Trang bị mới
        text.append(ConstFont.BOLD_BLUE).append("Trang bị mới: ").append(trangBiMoi.template.name).append("\n");

        // Kết quả dự kiến
        text.append(ConstFont.BOLD_GREEN).append("Sau chuyển hóa: ").append(trangBiMoi.template.name)
                .append(" [+").append(levelGoc).append("]\n");
        text.append(ConstFont.BOLD_GREEN).append(trangBiMoi.getOptionInfoChuyenHoa(trangBiGoc, levelGoc)).append("\n");

        // Chi phí
        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: 100%\n");
        if (dungVang) {
            text.append(player.inventory.gold >= GOLD_COST ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                    .append("Chi phí: ").append(Util.numberToMoney(GOLD_COST)).append(" vàng\n");
        } else {
            int ngoc = player.inventory.getGemAndRuby();
            text.append(ngoc >= NGOC_COST ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                    .append("Chi phí: ").append(Util.numberToMoney(NGOC_COST)).append(" ngọc\n");
        }
        text.append(ConstFont.BOLD_RED).append("Lưu ý: Trang bị gốc sẽ bị mất sau chuyển hóa!");

        // Kiểm tra đủ chi phí
        if (dungVang) {
            if (player.inventory.gold < GOLD_COST) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        text.toString(),
                        "Còn thiếu\n" + Util.numberToMoney(GOLD_COST - player.inventory.gold) + " vàng");
                return;
            }
        } else {
            int ngoc = player.inventory.getGemAndRuby();
            if (ngoc < NGOC_COST) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        text.toString(),
                        "Còn thiếu\n" + Util.numberToMoney(NGOC_COST - ngoc) + " ngọc");
                return;
            }
        }

        // Hiển thị menu xác nhận
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                text.toString(), "Chuyển hóa", "Đóng");
    }

    /**
     * Thực hiện chuyển hóa trang bị
     */
    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            return;
        }

        Item trangBiGoc = null;
        Item trangBiMoi = null;

        for (Item item : player.combine.itemsCombine) {
            if (item.template == null || item.template.type >= 5) {
                continue;
            }
            int level = item.getOptionParam(72);
            if (level >= MIN_LEVEL_REQUIRED) {
                trangBiGoc = item;
            } else if (level == 0) {
                trangBiMoi = item;
            }
        }

        if (trangBiGoc == null || trangBiMoi == null) {
            return;
        }

        // Validate lại
        if (trangBiGoc.template.type != trangBiMoi.template.type) {
            return;
        }
        if (trangBiMoi.template.level <= trangBiGoc.template.level) {
            return;
        }

        boolean dungVang = player.combine.typeCombine == CombineService.CHUYEN_HOA_TRANG_BI_DUNG_VANG;
        int levelGoc = trangBiGoc.getOptionParam(72);

        // Kiểm tra và trừ chi phí
        if (dungVang) {
            if (player.inventory.gold < GOLD_COST) {
                return;
            }
            player.inventory.gold -= GOLD_COST;
        } else {
            int ngoc = player.inventory.getGemAndRuby();
            if (ngoc < NGOC_COST) {
                return;
            }
            player.inventory.subGemAndRuby(NGOC_COST);
        }

        // Thực hiện chuyển hóa: copy thuộc tính từ gốc sang mới
        int percentGoc = trangBiGoc.getPercentOption();

        // Chuyển các option có thể nâng cấp từ gốc sang mới
        for (Item.ItemOption ioMoi : trangBiMoi.itemOptions) {
            if (ioMoi.isOptionCanUpgrade()) {
                ioMoi.param = CombineUtil.pointUp(ioMoi.param * percentGoc / 100, levelGoc);
            }
        }

        // Copy các option không thể nâng cấp từ gốc sang mới (trừ level, sao pha lê internal)
        for (Item.ItemOption ioGoc : trangBiGoc.itemOptions) {
            if (!ioGoc.isOptionCanUpgrade()
                    && ioGoc.optionTemplate.id != 72  // cấp
                    && ioGoc.optionTemplate.id != 73  // pha lê
                    && ioGoc.optionTemplate.id != 102 // internal
                    && ioGoc.optionTemplate.id != 107  // internal
                    && ioGoc.optionTemplate.id != 218) { // internal
                trangBiMoi.itemOptions.add(new Item.ItemOption(ioGoc));
            }
        }

        // Set cấp cho trang bị mới = cấp trang bị gốc
        trangBiMoi.addOptionParam(72, levelGoc);

        // Copy sao pha lê nếu có
        int saoPhaLe = trangBiGoc.getOptionParam(73);
        if (saoPhaLe > 0) {
            trangBiMoi.addOptionParam(73, saoPhaLe);
        }

        // Xóa trang bị gốc
        InventoryService.gI().removeItemBag(player, trangBiGoc);

        // Gửi hiệu ứng thành công
        CombineService.gI().sendEffectSuccessCombine(player);

        // Thông báo
        Service.gI().sendThongBao(player, "Chuyển hóa thành công! " + trangBiMoi.template.name + " [+" + levelGoc + "]");
        ServerNotify.gI().notify(player.name + " vừa chuyển hóa thành công " + trangBiMoi.template.name + " [+" + levelGoc + "]");

        // Cập nhật client
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}
