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
                ? "\n[DANG BUFF] +20% suc danh toan server!"
                : "\nGop du 999 ly -> buff 20% suc danh 60 phut!";

        createOtherMenu(pl, ConstNpc.BASE_MENU,
                "QUAY NUOC MIA - SU KIEN HE\n"
                + "Su kien he dang dien ra soi dong.\n\n"
                + "Chien binh " + pl.name + ", hay tham gia!\n"
                + "Tien trinh: " + (eventCount % 999) + "/999 ly" + buffStatus
                + "\nDiem su kien cua ban: " + pl.event.getEventPoint(),
                "Giai nhiet", "Tra hang", "Thong tin SK");
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
                    "Can 30 Nuoc da + 30 Khuc mia + 100tr vang de giai nhiet.");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hanh trang day, hay don cho trong!");
            return;
        }

        Item reward;
        String rewardName;
        if (Util.isTrue(15, 100)) {
            reward = ItemService.gI().createNewItem((short) 1616); // phần thưởng hiếm
            rewardName = "Nuoc Mia Dac Biet";
        } else if (Util.isTrue(30, 100)) {
            reward = ItemService.gI().createNewItem((short) 1615); // phần thưởng khá
            rewardName = "Nuoc Mia Thom";
        } else {
            reward = ItemService.gI().createNewItem((short) 1614); // phần thưởng cơ bản
            rewardName = "Nuoc Mia Thuong";
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

        Service.gI().sendThongBao(player, "Ban da nhan: " + rewardName
                + "\n+2 diem su kien (tong: " + player.event.getEventPoint() + ")");
    }

    /**
     * Mở menu trả hàng
     */
    private void openMenuTraHang(Player player) {
        String buffInfo;
        if (isServerBuffActive && System.currentTimeMillis() < buffEndTime) {
            long remaining = (buffEndTime - System.currentTimeMillis()) / 60000;
            buffInfo = "[DANG BUFF] +20% suc danh con " + remaining + " phut!";
        } else {
            buffInfo = "Khi du 999 ly, buff +20% suc danh 60 phut cho toan server.";
        }

        String text = "TRA HANG - GOP LY NUOC MIA\n\n"
                + "- Diem su kien cua ban: " + player.event.getEventPoint() + "\n"
                + "- Tong so ly toan server: " + (eventCount % 999) + "/999\n"
                + "- Tong ly da gop: " + eventCount + "\n\n"
                + buffInfo;

        createOtherMenu(player, ConstNpc.MENU_OPEN_SUKIEN_BINH, text,
                "Tra 1 ly", "Tra 10 ly", "Tra 99 ly");
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
            Service.gI().sendThongBao(player, "So luong khong hop le.");
            return;
        }

        Item lyNuocMia = InventoryService.gI().finditemLyNuocMia(player, soLuong);
        if (lyNuocMia == null || lyNuocMia.quantity < soLuong) {
            Service.gI().sendThongBao(player, "Ban can co " + soLuong + " ly nuoc mia.");
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
                "Ban da tra " + soLuong + " ly nuoc mia.\nDiem su kien hien tai: "
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
                "[SU KIEN HE] Du 999 ly nuoc mia! Toan server duoc buff +20% suc danh trong 60 phut!");

        // Áp dụng buff cho tất cả player online
        try {
            for (Player pl : Client.gI().getPlayers()) {
                if (pl != null && !pl.isDie() && !pl.isBoss) {
                    // Tăng 20% damage
                    long bonusDame = (long) (pl.nPoint.dameg * 0.2);
                    pl.nPoint.dameg += bonusDame;
                    Service.gI().point(pl);
                    Service.gI().sendThongBao(pl,
                            "Ban duoc buff +20% suc danh (" + bonusDame + " dame) trong 60 phut!");
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
                "[SU KIEN HE] Buff +20% suc danh da het han! Tiep tuc gop ly nuoc mia de buff lai!");

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
                ? "KHUYEN MAI NAP x2 dang hoat dong!\nMoi lan nap ATM/Bank deu duoc x2 ngoc!"
                : "Khuyen mai nap hien chua bat.";

        String info = "=== SU KIEN HE ===\n\n"
                + "1. GIAI NHIET: 30 Nuoc Da + 30 Khuc Mia + 100tr vang\n"
                + "   -> Nhan Nuoc Mia ngau nhien + 2 diem SK\n\n"
                + "2. TRA HANG: Gop Nuoc Mia cho server\n"
                + "   -> +1 diem SK/ly, du 999 ly -> buff 20% SD 60p\n\n"
                + "3. DOI HOP QUA: Tai NPC Quy Lao\n"
                + "   -> 10 Vo Oc + 10 Vo So + 10 Con Cua + 10 Sao Bien\n"
                + "   + 5 Da Ngu Sac + 50tr vang (50% thanh cong)\n\n"
                + "4. BOSS MAT TROI: Xuat hien ngau nhien tai cac lang\n"
                + "   -> Drop Co Mat Troi, Thoi Vang, Da Ngu Sac\n\n"
                + "5. " + napInfo + "\n\n"
                + "Diem su kien: " + player.event.getEventPoint() + "\n"
                + "Diem su kien (tong hop): " + player.event.getDiemSuKien();

        Service.gI().sendThongBao(player, info);
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
