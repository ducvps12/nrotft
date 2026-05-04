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
import java.util.concurrent.ConcurrentHashMap;

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

    // Chi phí đổi cải trang
    private static final int GIA_VANG = 500_000_000; // 500tr vàng
    private static final int GIA_NGOC_XANH = 50;     // 50 ngọc xanh (gem)
    private static final int GIA_THOI_VANG = 10;     // 10 thỏi vàng (item 457)
    private static final int GIA_HONG_NGOC = 30;     // 30 hồng ngọc (ruby)

    // Danh sách cải trang phổ biến (type=5) — sẽ được build từ ITEM_TEMPLATES lúc runtime
    private static List<Short> COSTUME_IDS = null;

    // Lưu tạm phương thức thanh toán theo player ID
    private static final ConcurrentHashMap<Long, Integer> PAYMENT_TYPE = new ConcurrentHashMap<>();

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
            sb.append("Chi phí mỗi lần đổi:\n");
            sb.append("• " + Util.mumberToLouis(GIA_VANG) + " Vàng\n");
            sb.append("• " + GIA_NGOC_XANH + " Ngọc Xanh\n");
            sb.append("• " + GIA_THOI_VANG + " Thỏi Vàng\n");
            sb.append("• " + GIA_HONG_NGOC + " Hồng Ngọc\n\n");
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

                // Hiện menu chọn phương thức thanh toán
                StringBuilder sb = new StringBuilder();
                sb.append("Đổi giao diện: " + chosen.template.name + "\n");
                sb.append("━━━━━━━━━━━━━━━━\n");
                sb.append("Chỉ số hiện tại:\n");
                sb.append(chosen.getOptionInfo() + "\n\n");
                sb.append("Chọn phương thức thanh toán:");

                this.createOtherMenu(player, ConstNpc.MENU_HMN_CHON_PHI,
                        sb.toString(), new String[]{
                                Util.mumberToLouis(GIA_VANG) + " Vàng",
                                GIA_NGOC_XANH + " Ngọc Xanh",
                                GIA_THOI_VANG + " Thỏi Vàng",
                                GIA_HONG_NGOC + " Hồng Ngọc",
                                "Quay lại"
                        }, this);
            }
            case ConstNpc.MENU_HMN_CHON_PHI -> {
                if (select < 0 || select >= 4) {
                    openBaseMenu(player);
                    return;
                }
                int bagIndex = player.iDMark.getOtt();
                if (bagIndex < 0 || bagIndex >= player.inventory.itemsBag.size()) {
                    Service.gI().sendThongBao(player, "Lỗi! Vui lòng thử lại.");
                    return;
                }
                Item chosenItem = player.inventory.itemsBag.get(bagIndex);
                if (!chosenItem.isNotNullItem() || chosenItem.template.type != 5) {
                    Service.gI().sendThongBao(player, "Cải trang không hợp lệ!");
                    return;
                }

                PAYMENT_TYPE.put(player.id, select);

                // Xác nhận
                String paymentName = switch (select) {
                    case 0 -> Util.mumberToLouis(GIA_VANG) + " Vàng";
                    case 1 -> GIA_NGOC_XANH + " Ngọc Xanh";
                    case 2 -> GIA_THOI_VANG + " Thỏi Vàng";
                    case 3 -> GIA_HONG_NGOC + " Hồng Ngọc";
                    default -> "";
                };

                this.createOtherMenu(player, ConstNpc.MENU_HMN_CONFIRM,
                        "⚠️ XÁC NHẬN HIẾN TẾ\n"
                        + "━━━━━━━━━━━━━━━━\n"
                        + "Cải trang: " + chosenItem.template.name + "\n"
                        + "Chi phí: " + paymentName + "\n\n"
                        + "Bạn sẽ nhận 1 cải trang NGẪU NHIÊN\n"
                        + "khác với chỉ số giữ nguyên!\n\n"
                        + "⚠️ Cải trang cũ sẽ bị MẤT!",
                        new String[]{"Đồng ý", "Hủy"}, this);
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
        int paymentType = PAYMENT_TYPE.getOrDefault(player.id, -1);
        PAYMENT_TYPE.remove(player.id);

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

        // Kiểm tra & trừ phí
        if (!chargePayment(player, paymentType)) {
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
     * Trừ phí thanh toán
     */
    private boolean chargePayment(Player player, int type) {
        switch (type) {
            case 0 -> { // Vàng
                if (player.inventory.gold < GIA_VANG) {
                    Service.gI().sendThongBao(player,
                            "Không đủ Vàng! Cần " + Util.mumberToLouis(GIA_VANG)
                            + "\nHiện có: " + Util.mumberToLouis(player.inventory.gold));
                    return false;
                }
                player.inventory.gold -= GIA_VANG;
                Service.gI().sendMoney(player);
            }
            case 1 -> { // Ngọc xanh (gem)
                if (player.inventory.gem < GIA_NGOC_XANH) {
                    Service.gI().sendThongBao(player,
                            "Không đủ Ngọc Xanh! Cần " + GIA_NGOC_XANH
                            + "\nHiện có: " + player.inventory.gem);
                    return false;
                }
                player.inventory.gem -= GIA_NGOC_XANH;
                Service.gI().sendMoney(player);
            }
            case 2 -> { // Thỏi vàng (item 457)
                Item thoiVang = InventoryService.gI().findItemBagByTemp(player, 457);
                if (thoiVang == null || thoiVang.quantity < GIA_THOI_VANG) {
                    Service.gI().sendThongBao(player,
                            "Không đủ Thỏi Vàng! Cần " + GIA_THOI_VANG
                            + "\nHiện có: " + (thoiVang != null ? thoiVang.quantity : 0));
                    return false;
                }
                InventoryService.gI().subQuantityItemsBag(player, thoiVang, GIA_THOI_VANG);
            }
            case 3 -> { // Hồng ngọc (ruby)
                if (player.inventory.ruby < GIA_HONG_NGOC) {
                    Service.gI().sendThongBao(player,
                            "Không đủ Hồng Ngọc! Cần " + GIA_HONG_NGOC
                            + "\nHiện có: " + player.inventory.ruby);
                    return false;
                }
                player.inventory.ruby -= GIA_HONG_NGOC;
                Service.gI().sendMoney(player);
            }
            default -> {
                return false;
            }
        }
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
