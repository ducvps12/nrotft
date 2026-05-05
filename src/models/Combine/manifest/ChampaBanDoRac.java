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
 * Champa — Panel bán đồ rác
 * Mở panel combine để player kéo-thả item vào, thấy giá trị quy đổi
 * rồi mới xác nhận bán → nhận Thỏi Vàng (khóa)
 */
public class ChampaBanDoRac {

    // Thỏi vàng
    private static final int THOI_VANG = 457;

    /**
     * Hiển thị thông tin khi player đặt item vào panel combine
     */
    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.isEmpty()) {
            Service.gI().sendThongBao(player,
                    "Hãy đặt đồ rác vào để ta định giá!\n"
                    + "Chấp nhận: Trang bị cấp 1-12, thức ăn, mảnh vỡ cũ");
            return;
        }

        // Tính giá trị quy đổi cho từng item
        int tongThoi = 0;
        int tongMon = 0;
        StringBuilder danhSach = new StringBuilder();

        for (Item item : player.combine.itemsCombine) {
            if (item == null || !item.isNotNullItem()) continue;

            if (!isJunkItem(item)) {
                Service.gI().sendThongBao(player,
                        item.template.name + " không phải đồ rác!\n"
                        + "Vui lòng bỏ ra và chỉ đặt đồ hợp lệ.");
                return;
            }

            int gia = getJunkPrice(item);
            int tien = gia * item.quantity;
            tongThoi += tien;
            tongMon += item.quantity;
            danhSach.append("\n|2|").append(item.template.name)
                    .append(" x").append(item.quantity)
                    .append(" = ").append(tien).append(" TV");
        }

        player.combine.goldCombine = 0;
        player.combine.ratioCombine = 100;

        String npcSay = "|1|=== ĐỊNH GIÁ ĐỒ RÁC ===\n"
                + "|7|Đã chọn " + tongMon + " món:" + danhSach
                + "\n\n|1|━━━━━━━━━━━━━━━━"
                + "\n|7|Tổng nhận: |2|" + tongThoi + " Thỏi Vàng (khóa)";

        // Dùng NPC Champa để hiện menu
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                "Bán " + tongMon + " món\n→ " + tongThoi + " TV", "Không bán");
    }

    /**
     * Thực hiện bán đồ rác đã chọn
     */
    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.isEmpty()) {
            Service.gI().sendThongBao(player, "Không có đồ rác nào để bán!");
            return;
        }

        // Validate + tính tổng
        int tongThoi = 0;
        int tongMon = 0;
        for (Item item : player.combine.itemsCombine) {
            if (item == null || !item.isNotNullItem() || !isJunkItem(item)) {
                Service.gI().sendThongBao(player, "Có vật phẩm không hợp lệ! Vui lòng thử lại.");
                return;
            }
            tongThoi += getJunkPrice(item) * item.quantity;
            tongMon += item.quantity;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đầy! Cần 1 ô trống.");
            return;
        }

        // Xóa đồ rác đã chọn
        for (Item item : player.combine.itemsCombine) {
            InventoryService.gI().subQuantityItemsBag(player, item, item.quantity);
        }

        // Thêm Thỏi Vàng (khóa)
        if (tongThoi > 0) {
            Item thoi = ItemService.gI().createNewItem((short) THOI_VANG, tongThoi);
            thoi.itemOptions.add(new Item.ItemOption(30, 1)); // Khóa giao dịch
            InventoryService.gI().addItemBag(player, thoi);
        }

        // Hiệu ứng thành công
        CombineService.gI().sendEffectSuccessCombine(player);

        Service.gI().sendThongBao(player,
                "Bán thành công " + tongMon + " món!\n"
                + "Nhận " + tongThoi + " Thỏi Vàng (khóa)");
        Service.gI().chat(player, "Bán " + tongMon + " đồ rác cho Champa!");

        InventoryService.gI().sendItemBag(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    // ===================== HELPER METHODS =====================
    public static boolean isJunkItem(Item item) {
        if (item == null || item.template == null) return false;
        int type = item.template.type;

        // Trang bị (áo, quần, găng, giày, nhẫn) hoặc Cải trang (type 5)
        // Nếu không có bất kỳ option chỉ số nào thì đây là vật phẩm bị lỗi -> Rác
        if (type >= 0 && type <= 5) {
            if (item.itemOptions == null || item.itemOptions.isEmpty()) {
                return true;
            }
        }
        
        return false;
    }

    private static int getJunkPrice(Item item) {
        if (item == null || item.template == null) return 0;
        
        // Trả về 1 Thỏi Vàng cho mỗi vật phẩm lỗi (có thể điều chỉnh nếu cần)
        return 1;
    }
}
