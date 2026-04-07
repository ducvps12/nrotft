package models.Combine.manifest;

import consts.ConstItem;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;

/**
 * Phân rã đồ Thần Linh (tất cả hành tinh) thành Đá Ngũ Sắc
 * Sử dụng Tab Combine để người chơi tự kéo-thả item muốn phân rã
 */
public class PhanRaDoThanLinh {

    /**
     * Hiển thị thông tin phân rã khi người chơi đặt item vào tab combine
     */
    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.isEmpty()) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hãy đặt trang bị Thần Linh vào để phân rã!", "Đóng");
            return;
        }

        // Validate tất cả item phải là đồ Thần Linh
        int soLuong = 0;
        StringBuilder danhSach = new StringBuilder();
        for (Item item : player.combine.itemsCombine) {
            if (item == null || !item.isNotNullItem() || !item.isDTL()) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Chỉ được đặt trang bị Thần Linh!\nVui lòng bỏ vật phẩm không hợp lệ ra.", "Đóng");
                return;
            }
            soLuong++;
            danhSach.append("\n|2|").append(item.template.name);
        }

        player.combine.goldCombine = 0;
        player.combine.ratioCombine = 100;

        String npcSay = "|1|Phân rã đồ Thần Linh\n"
                + "|7|Đã chọn " + soLuong + " trang bị:" + danhSach
                + "\n\n|1|Mỗi trang bị nhận được 1 Đá Ngũ Sắc"
                + "\n|7|Tổng nhận: " + soLuong + " Đá Ngũ Sắc";

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                "Phân rã\n" + soLuong + " trang bị", "Từ chối");
    }

    /**
     * Thực hiện phân rã đồ Thần Linh đã chọn
     */
    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.isEmpty()) {
            Service.gI().sendThongBao(player, "Không có trang bị nào để phân rã!");
            return;
        }

        // Validate lại lần nữa
        int soLuong = 0;
        for (Item item : player.combine.itemsCombine) {
            if (item == null || !item.isNotNullItem() || !item.isDTL()) {
                Service.gI().sendThongBao(player, "Chỉ được phân rã trang bị Thần Linh!");
                return;
            }
            soLuong++;
        }

        // Xóa tất cả đồ Thần Linh đã chọn khỏi hành trang
        for (Item item : player.combine.itemsCombine) {
            InventoryService.gI().removeItemBag(player, item);
        }

        // Thêm Đá Ngũ Sắc (ID 674) - mỗi đồ thần linh = 1 viên
        Item daNguSac = InventoryService.gI().findItemBag(player, ConstItem.DA_NGU_SAC);
        if (daNguSac != null) {
            // Đã có Đá Ngũ Sắc trong túi, cộng thêm số lượng
            daNguSac.quantity += soLuong;
        } else {
            // Tạo mới Đá Ngũ Sắc
            Item newDaNguSac = ItemService.gI().createNewItem((short) ConstItem.DA_NGU_SAC);
            newDaNguSac.quantity = soLuong;
            InventoryService.gI().addItemBag(player, newDaNguSac);
        }

        // Hiệu ứng thành công
        CombineService.gI().sendEffectSuccessCombine(player);

        // Thông báo kết quả
        Service.gI().sendThongBao(player,
                "Phân rã thành công " + soLuong + " trang bị Thần Linh!\n"
                + "Bạn nhận được " + soLuong + " Đá Ngũ Sắc.");
        Service.gI().chat(player, "Phân rã " + soLuong + " đồ Thần Linh thành công!");

        // Cập nhật hành trang
        InventoryService.gI().sendItemBag(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}
