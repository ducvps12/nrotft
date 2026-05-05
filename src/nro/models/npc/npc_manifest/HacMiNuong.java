package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import models.Template.ItemTemplate;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.server.Manager;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.Service;
import utils.Util;

import java.util.ArrayList;
import java.util.List;


/**
 * NPC Hắc Mị Nương — Hiến tế Cải trang
 * Map 173 (Siêu thị Huyền Bí)
 * 
 * Chức năng: Đổi giao diện cải trang (giữ nguyên chỉ số).
 * - Player chọn cải trang muốn đổi từ túi
 * - Chọn phương thức thanh toán (ngọc xanh, vàng, thỏi vàng, hồng ngọc)
 * - Hệ thống random 1 cải trang khác, copy chỉ số cũ sang
 * - Mất cải trang cũ + phí
 */
public class HacMiNuong extends Npc {

    // Chi phí đổi cải trang — phải trả TẤT CẢ cùng lúc
    private static final long GIA_VANG = 100_000_000L; // 100 triệu vàng
    private static final int GIA_NGOC_XANH = 100_000;  // 100k ngọc xanh (gem)
    private static final int GIA_THOI_VANG = 100;      // 100 thỏi vàng (item 457)
    private static final int GIA_HONG_NGOC = 20_000;   // 20k hồng ngọc (ruby)

    // Danh sách cải trang phổ biến (type=5) — sẽ được build từ ITEM_TEMPLATES lúc runtime
    private static List<Short> COSTUME_IDS = null;

    // Không cần lưu payment type nữa — luôn trả tất cả

    public HacMiNuong(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    /**
     * Build danh sách cải trang từ ITEM_TEMPLATES (lazy init)
     */
    private static synchronized List<Short> getCostumeIds() {
        if (COSTUME_IDS == null) {
            COSTUME_IDS = new ArrayList<>();
            for (ItemTemplate it : Manager.ITEM_TEMPLATES) {
                if (it.type == 5 && it.id > 0) {
                    COSTUME_IDS.add(it.id);
                }
            }
        }
        return COSTUME_IDS;
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            // Tìm cải trang trong túi player
            List<Item> costumes = findCostumesInBag(player);
            if (costumes.isEmpty()) {
                Service.gI().sendThongBao(player,
                        "Bạn không có cải trang nào trong hành trang!");
                return;
            }

            // Build nội dung menu
            StringBuilder sb = new StringBuilder();
            sb.append("— HẮC MỊ NƯƠNG —\n");
            sb.append("Hiến tế Cải trang\n");
            sb.append("━━━━━━━━━━━━━━━━\n");
            sb.append("Ta sẽ giúp ngươi ĐỔI GIAO DIỆN\n");
            sb.append("cải trang mà GIỮ NGUYÊN chỉ số!\n\n");
            sb.append("Chi phí đổi (trả TẤT CẢ):\n");
            sb.append("• " + Util.mumberToLouis(GIA_VANG) + " Vàng\n");
            sb.append("• " + Util.numberToMoney(GIA_NGOC_XANH) + " Ngọc Xanh\n");
            sb.append("• " + GIA_THOI_VANG + " Thỏi Vàng\n");
            sb.append("• " + Util.numberToMoney(GIA_HONG_NGOC) + " Hồng Ngọc\n\n");
            sb.append("Tìm thấy " + costumes.size() + " cải trang.\n");
            sb.append("Chọn cải trang muốn đổi:");

            // Build buttons = tên cải trang (tối đa 5)
            int maxShow = Math.min(costumes.size(), 5);
            String[] buttons = new String[maxShow + 1];
            for (int i = 0; i < maxShow; i++) {
                Item ct = costumes.get(i);
                buttons[i] = ct.template.name;
            }
            buttons[maxShow] = "Đóng";

            this.createOtherMenu(player, ConstNpc.MENU_HMN_CHON_CT,
                    sb.toString(), buttons, this);
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        switch (player.iDMark.getIndexMenu()) {
            case ConstNpc.MENU_HMN_CHON_CT -> {
                // Player chọn cải trang
                List<Item> costumes = findCostumesInBag(player);
                int maxShow = Math.min(costumes.size(), 5);
                if (select < 0 || select >= maxShow) {
                    return; // Đóng
                }
                Item chosen = costumes.get(select);
                // Lưu index vào iDMark để dùng sau
                player.iDMark.setOtt(InventoryService.gI().getIndexItemBag(player, chosen));

                // Hiện menu xác nhận trực tiếp (trả TẤT CẢ cùng lúc)
                StringBuilder sb = new StringBuilder();
                sb.append("Đổi giao diện: " + chosen.template.name + "\n");
                sb.append("━━━━━━━━━━━━━━━━\n");
                sb.append("Chỉ số hiện tại:\n");
                sb.append(chosen.getOptionInfo() + "\n\n");
                sb.append("Chi phí (trả TẤT CẢ):\n");
                sb.append("• " + Util.mumberToLouis(GIA_VANG) + " Vàng\n");
                sb.append("• " + Util.numberToMoney(GIA_NGOC_XANH) + " Ngọc Xanh\n");
                sb.append("• " + GIA_THOI_VANG + " Thỏi Vàng\n");
                sb.append("• " + Util.numberToMoney(GIA_HONG_NGOC) + " Hồng Ngọc\n");

                this.createOtherMenu(player, ConstNpc.MENU_HMN_CONFIRM,
                        sb.toString(), new String[]{"Đồng ý", "Hủy"}, this);
            }
            case ConstNpc.MENU_HMN_CONFIRM -> {
                if (select != 0) {
                    openBaseMenu(player);
                    return;
                }
                performSacrifice(player);
            }
        }
    }

    /**
     * Thực hiện hiến tế cải trang
     */
    private void performSacrifice(Player player) {
        int bagIndex = player.iDMark.getOtt();

        if (bagIndex < 0 || bagIndex >= player.inventory.itemsBag.size()) {
            Service.gI().sendThongBao(player, "Lỗi! Vui lòng thử lại.");
            return;
        }

        Item oldCostume = player.inventory.itemsBag.get(bagIndex);
        if (!oldCostume.isNotNullItem() || oldCostume.template.type != 5) {
            Service.gI().sendThongBao(player, "Cải trang không hợp lệ!");
            return;
        }

        // Check hành trang trống
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang!");
            return;
        }

        // Kiểm tra & trừ phí (trả TẤT CẢ cùng lúc)
        if (!chargePaymentAll(player)) {
            return; // Lỗi đã gửi thông báo
        }

        // Lưu options cũ
        List<ItemOption> oldOptions = new ArrayList<>();
        for (ItemOption io : oldCostume.itemOptions) {
            oldOptions.add(new ItemOption(io));
        }
        short oldId = oldCostume.template.id;
        String oldName = oldCostume.template.name;

        // Random cải trang mới (khác cái cũ)
        List<Short> costumePool = getCostumeIds();
        if (costumePool.size() < 2) {
            Service.gI().sendThongBao(player, "Không đủ cải trang trong hệ thống!");
            return;
        }

        short newId;
        int maxAttempts = 100;
        do {
            newId = costumePool.get(Util.nextInt(0, costumePool.size() - 1));
            maxAttempts--;
        } while (newId == oldId && maxAttempts > 0);

        // Tạo item mới với options cũ
        Item newCostume = ItemService.gI().createNewItem(newId);
        if (newCostume == null || !newCostume.isNotNullItem()) {
            Service.gI().sendThongBao(player, "Lỗi tạo cải trang mới!");
            return;
        }

        // Xóa options mặc định, copy options cũ sang
        newCostume.itemOptions.clear();
        newCostume.itemOptions.addAll(oldOptions);

        // Xóa cải trang cũ
        InventoryService.gI().subQuantityItemsBag(player, oldCostume, 1);
        // Thêm cải trang mới
        InventoryService.gI().addItemBag(player, newCostume);
        InventoryService.gI().sendItemBag(player);
        Service.gI().Send_Caitrang(player);

        // Thông báo thành công
        Service.gI().sendThongBao(player,
                "✨ HIẾN TẾ THÀNH CÔNG!\n"
                + "━━━━━━━━━━━━━━━━\n"
                + "Cũ: " + oldName + "\n"
                + "Mới: " + newCostume.template.name + "\n\n"
                + "Chỉ số đã được giữ nguyên!");
    }

    /**
     * Trừ phí thanh toán — trả TẤT CẢ cùng lúc
     */
    private boolean chargePaymentAll(Player player) {
        // Kiểm tra đủ tất cả trước khi trừ
        StringBuilder thieu = new StringBuilder();
        boolean du = true;

        if (player.inventory.gold < GIA_VANG) {
            thieu.append("• Vàng: cần " + Util.mumberToLouis(GIA_VANG)
                    + ", có " + Util.mumberToLouis(player.inventory.gold) + "\n");
            du = false;
        }
        if (player.inventory.gem < GIA_NGOC_XANH) {
            thieu.append("• Ngọc Xanh: cần " + Util.numberToMoney(GIA_NGOC_XANH)
                    + ", có " + Util.numberToMoney(player.inventory.gem) + "\n");
            du = false;
        }
        Item thoiVang = InventoryService.gI().findItemBagByTemp(player, 457);
        int slTV = thoiVang != null ? thoiVang.quantity : 0;
        if (slTV < GIA_THOI_VANG) {
            thieu.append("• Thỏi Vàng: cần " + GIA_THOI_VANG
                    + ", có " + slTV + "\n");
            du = false;
        }
        if (player.inventory.ruby < GIA_HONG_NGOC) {
            thieu.append("• Hồng Ngọc: cần " + Util.numberToMoney(GIA_HONG_NGOC)
                    + ", có " + Util.numberToMoney(player.inventory.ruby) + "\n");
            du = false;
        }

        if (!du) {
            Service.gI().sendThongBao(player,
                    "Không đủ tài nguyên!\n━━━━━━━━━━━━━━━━\n" + thieu);
            return false;
        }

        // Trừ tất cả
        player.inventory.gold -= GIA_VANG;
        player.inventory.gem -= GIA_NGOC_XANH;
        player.inventory.ruby -= GIA_HONG_NGOC;
        InventoryService.gI().subQuantityItemsBag(player, thoiVang, GIA_THOI_VANG);
        Service.gI().sendMoney(player);
        return true;
    }

    /**
     * Tìm tất cả cải trang (type=5) trong hành trang player
     */
    private List<Item> findCostumesInBag(Player player) {
        List<Item> result = new ArrayList<>();
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.type == 5) {
                result.add(item);
            }
        }
        return result;
    }
}
