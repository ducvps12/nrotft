package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import event.EventManager;
import item.Item;
import item.Item.ItemOption;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.server.Client;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import nro.services.NpcService;
import shop.*;
import nro.server.Manager;
import utils.Util;

/**
 * NPC Quầy Nước Mía — Sự Kiện Hè
 * ═══════════════════════════════════
 * Menu chính:
 * 1. Giải nhiệt — 30 Nước Đá + 30 Khúc Mía + 100tr vàng → random Nước Mía
 * 2. Trả hàng — Góp ly nước mía, tích điểm, buff toàn server khi đủ 999 ly
 * 3. Thông tin SK — Xem hướng dẫn sự kiện, điểm cá nhân, top
 */
public class QuayNuocMia extends Npc {

    // Tổng số ly trả hàng toàn server (persist trong file)
    private static int eventCount = 0;
    // Thời gian buff server kết thúc
    private static long buffEndTime = 0;
    // Flag buff đang active
    private static boolean isServerBuffActive = false;

    static {
        // Load eventCount từ file khi server khởi động
        loadEventCount();
    }

    public QuayNuocMia(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (!canOpenNpc(pl)) {
            return;
        }

        String buffStatus = isServerBuffActive && System.currentTimeMillis() < buffEndTime
                ? "\n[ĐANG BUFF] +20% sức đánh toàn server!"
                : "\nGóp đủ 999 ly -> buff 20% sức đánh 60 phút!";

        createOtherMenu(pl, ConstNpc.BASE_MENU,
                "QUẦY NƯỚC MÍA - SỰ KIỆN HÈ\n"
                + "Sự kiện hè đang diễn ra sôi động.\n\n"
                + "Chiến binh " + pl.name + ", hãy tham gia!\n"
                + "Tiến trình: " + (eventCount % 999) + "/999 ly" + buffStatus
                + "\nĐiểm sự kiện của bạn: " + pl.event.getEventPoint(),
                "Giải nhiệt", "Trả hàng", "Thông tin SK", "Cửa hàng");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.iDMark.isBaseMenu()) {
            switch (select) {
                case 0 ->
                    giaiNhiet(player);
                case 1 ->
                    openMenuTraHang(player);
                case 2 ->
                    showEventInfo(player);
                case 3 ->
                    ShopService.gI().opendShop(player, getShopQuayNuocMia().tagName, true);
            }
        } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPEN_SUKIEN_BINH) {
            traHangNuocMia(player, select);
        }
    }

    /**
     * Giải nhiệt - đổi nguyên liệu lấy phần thưởng
     */
    private void giaiNhiet(Player player) {
        Item nuocDa = InventoryService.gI().findItem(player.inventory.itemsBag, 1613); // Nước đá
        Item khucMia = InventoryService.gI().findItem(player.inventory.itemsBag, 1612); // Khúc mía

        if (nuocDa == null || khucMia == null
                || nuocDa.quantity < 30
                || khucMia.quantity < 30
                || player.inventory.gold < 100_000_000) {
            Service.gI().sendThongBao(player,
                    "Cần 30 Nước đá + 30 Khúc mía + 100tr vàng để giải nhiệt.");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đầy, hãy dọn chỗ trống!");
            return;
        }

        Item reward;
        String rewardName;
        if (Util.isTrue(15, 100)) {
            reward = ItemService.gI().createNewItem((short) 1616); // phần thưởng hiếm
            rewardName = "Nước Mía Đặc Biệt";
        } else if (Util.isTrue(30, 100)) {
            reward = ItemService.gI().createNewItem((short) 1615); // phần thưởng khá
            rewardName = "Nước Mía Thơm";
        } else {
            reward = ItemService.gI().createNewItem((short) 1614); // phần thưởng cơ bản
            rewardName = "Nước Mía Thường";
        }
        reward.itemOptions.add(new ItemOption(30, 0));

        // Trừ nguyên liệu
        InventoryService.gI().subQuantityItemsBag(player, nuocDa, 30);
        InventoryService.gI().subQuantityItemsBag(player, khucMia, 30);
        player.inventory.gold -= 100_000_000;

        // Nhận quà
        InventoryService.gI().addItemBag(player, reward);
        InventoryService.gI().sendItemBag(player);

        // +2 điểm sự kiện mỗi lần giải nhiệt
        player.event.setEventPoint(player.event.getEventPoint() + 2);

        Service.gI().sendThongBao(player, "Bạn đã nhận: " + rewardName
                + "\n+2 điểm sự kiện (tổng: " + player.event.getEventPoint() + ")");
    }

    /**
     * Mở menu trả hàng
     */
    private void openMenuTraHang(Player player) {
        String buffInfo;
        if (isServerBuffActive && System.currentTimeMillis() < buffEndTime) {
            long remaining = (buffEndTime - System.currentTimeMillis()) / 60000;
            buffInfo = "[ĐANG BUFF] +20% sức đánh còn " + remaining + " phút!";
        } else {
            buffInfo = "Khi đủ 999 ly, buff +20% sức đánh 60 phút cho toàn server.";
        }

        String text = "TRẢ HÀNG - GÓP LY NƯỚC MÍA\n\n"
                + "- Điểm sự kiện của bạn: " + player.event.getEventPoint() + "\n"
                + "- Tổng số ly toàn server: " + (eventCount % 999) + "/999\n"
                + "- Tổng ly đã góp: " + eventCount + "\n\n"
                + buffInfo;

        createOtherMenu(player, ConstNpc.MENU_OPEN_SUKIEN_BINH, text,
                "Trả 1 ly", "Trả 10 ly", "Trả 99 ly");
    }

    /**
     * Xử lý trả hàng
     */
    private void traHangNuocMia(Player player, int select) {
        int soLuong = switch (select) {
            case 0 ->
                1;
            case 1 ->
                10;
            case 2 ->
                99;
            default ->
                0;
        };

        if (soLuong <= 0) {
            Service.gI().sendThongBao(player, "Số lượng không hợp lệ.");
            return;
        }

        Item lyNuocMia = InventoryService.gI().finditemLyNuocMia(player, soLuong);
        if (lyNuocMia == null || lyNuocMia.quantity < soLuong) {
            Service.gI().sendThongBao(player, "Bạn cần có " + soLuong + " ly nước mía.");
            return;
        }

        // Trừ ly nước mía
        InventoryService.gI().subQuantityItemsBag(player, lyNuocMia, soLuong);
        InventoryService.gI().sendItemBag(player);

        // Cộng điểm sự kiện
        player.event.setEventPoint(player.event.getEventPoint() + soLuong);
        int oldCount = eventCount;
        eventCount += soLuong;

        // Lưu eventCount
        saveEventCount();

        Service.gI().sendThongBao(player,
                "Bạn đã trả " + soLuong + " ly nước mía.\nĐiểm sự kiện hiện tại: "
                + player.event.getEventPoint());

        // Kiểm tra buff toàn server — mỗi 999 ly
        int oldMilestone = oldCount / 999;
        int newMilestone = eventCount / 999;
        if (newMilestone > oldMilestone) {
            activateServerBuff();
        }
    }

    /**
     * Kích hoạt buff 20% sức đánh toàn server trong 60 phút
     */
    private static void activateServerBuff() {
        isServerBuffActive = true;
        buffEndTime = System.currentTimeMillis() + 60 * 60 * 1000; // 60 phút

        Service.gI().sendThongBaoAllPlayer(
                "[SỰ KIỆN HÈ] Đủ 999 ly nước mía! Toàn server được buff +20% sức đánh trong 60 phút!");

        // Áp dụng buff cho tất cả player online
        try {
            for (Player pl : Client.gI().getPlayers()) {
                if (pl != null && !pl.isDie() && !pl.isBoss) {
                    // Tăng 20% damage
                    long bonusDame = (long) (pl.nPoint.dameg * 0.2);
                    pl.nPoint.dameg += bonusDame;
                    Service.gI().point(pl);
                    Service.gI().sendThongBao(pl,
                            "Bạn được buff +20% sức đánh (" + bonusDame + " dame) trong 60 phút!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Lên lịch hủy buff sau 60 phút
        Thread.ofVirtual().name("summer-buff-timer").start(() -> {
            try {
                Thread.sleep(60 * 60 * 1000);
                deactivateServerBuff();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Hủy buff server
     */
    private static void deactivateServerBuff() {
        isServerBuffActive = false;
        buffEndTime = 0;

        Service.gI().sendThongBaoAllPlayer(
                "[SỰ KIỆN HÈ] Buff +20% sức đánh đã hết hạn! Tiếp tục góp ly nước mía để buff lại!");

        try {
            for (Player pl : Client.gI().getPlayers()) {
                if (pl != null && !pl.isDie() && !pl.isBoss) {
                    // Recalc lại chỉ số (sẽ tự reset về chuẩn)
                    pl.nPoint.initPowerLimit();
                    Service.gI().point(pl);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hiển thị thông tin sự kiện
     */
    private void showEventInfo(Player player) {
        String napInfo = EventManager.SUMMER_EVENT
                ? "KHUYẾN MÃI NẠP x2 đang hoạt động!\nMỗi lần nạp ATM/Bank đều được x2 ngọc!"
                : "Khuyến mãi nạp hiện chưa bật.";

        String info = "=== SỰ KIỆN HÈ ===\n\n"
                + "1. GIẢI NHIỆT: 30 Nước Đá + 30 Khúc Mía + 100tr vàng\n"
                + "   -> Nhận Nước Mía ngẫu nhiên + 2 điểm SK\n\n"
                + "2. TRẢ HÀNG: Góp Nước Mía cho server\n"
                + "   -> +1 điểm SK/ly, đủ 999 ly -> buff 20% SĐ 60p\n\n"
                + "3. ĐỔI HỘP QUÀ: Tại NPC Quy Lão\n"
                + "   -> 10 Vỏ Ốc + 10 Vỏ Sò + 10 Con Cua + 10 Sao Biển\n"
                + "   + 5 Đá Ngũ Sắc + 50tr vàng (50% thành công)\n\n"
                + "4. BOSS MẶT TRỜI: Xuất hiện ngẫu nhiên tại các làng\n"
                + "   -> Drop Cờ Mặt Trời, Thỏi Vàng, Đá Ngũ Sắc\n\n"
                + "5. " + napInfo + "\n\n"
                + "Điểm sự kiện: " + player.event.getEventPoint() + "\n"
                + "Điểm sự kiện (tổng hợp): " + player.event.getDiemSuKien();

        NpcService.gI().createTutorial(player, tempId, this.avartar, info);
    }

    private static Shop shopQuayNuocMia;

    public static Shop getShopQuayNuocMia() {
        if (shopQuayNuocMia == null) {
            shopQuayNuocMia = new Shop();
            shopQuayNuocMia.id = 999;
            shopQuayNuocMia.npcId = ConstNpc.QUAY_NUOC_MIA;
            shopQuayNuocMia.tagName = "SHOP_NUOC_MIA";
            shopQuayNuocMia.typeShop = 0; // NORMAL_SHOP
            
            TabShop tab = new TabShop();
            tab.id = 999;
            tab.name = "Cửa\nhàng";
            tab.shop = shopQuayNuocMia;
            
            int[] itemIds = {1614, 61, 665, 1609, 456, 1238, 1612, 1613};
            int[] costs = {100000, 50000, 500000, 2000000, 1500000, 1000000, 1000000, 1000000};
            
            for (int i = 0; i < itemIds.length; i++) {
                ItemShop is = new ItemShop();
                is.temp = ItemService.gI().getTemplate(itemIds[i]);
                is.cost = costs[i];
                is.typeSell = 0; // GOLD
                tab.itemShops.add(is);
            }

            shopQuayNuocMia.tabShops.add(tab);
            Manager.SHOPS.add(shopQuayNuocMia);
        }
        return shopQuayNuocMia;
    }

    // ========== PERSIST EVENT COUNT ==========

    private static void loadEventCount() {
        try {
            java.io.File f = new java.io.File("data/summer_event_count.txt");
            if (f.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(f.toPath())).trim();
                eventCount = Integer.parseInt(content);
                System.out.println("[QuayNuocMia] Loaded eventCount = " + eventCount);
            }
        } catch (Exception e) {
            System.out.println("[QuayNuocMia] Could not load eventCount: " + e.getMessage());
            eventCount = 0;
        }
    }

    private static void saveEventCount() {
        try {
            java.io.File f = new java.io.File("data/summer_event_count.txt");
            java.nio.file.Files.writeString(f.toPath(), String.valueOf(eventCount));
        } catch (Exception e) {
            System.out.println("[QuayNuocMia] Could not save eventCount: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra buff có đang active không (dùng cho các hệ thống khác)
     */
    public static boolean isBuffActive() {
        return isServerBuffActive && System.currentTimeMillis() < buffEndTime;
    }

    /**
     * Reset event count (dùng khi đổi sự kiện)
     */
    public static void resetEventCount() {
        eventCount = 0;
        isServerBuffActive = false;
        buffEndTime = 0;
        saveEventCount();
    }
}
