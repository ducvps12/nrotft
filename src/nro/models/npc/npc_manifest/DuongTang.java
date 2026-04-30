package nro.models.npc.npc_manifest;

/**
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import consts.ConstNpc;
import item.Item;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import services.func.ChangeMapService;
import shop.ShopService;
import utils.Util;

public class DuongTang extends Npc {

    // ===== Menu IDs =====
    private static final int MENU_ESCORT_INFO = 501;
    private static final int MENU_ESCORT_CONFIRM = 502;
    private static final int MENU_REWARD = 503;

    // ===== Escort destination maps =====
    private static final int[][] ESCORT_DESTINATIONS = {
        // {mapId, x, y, điểm thưởng, minPower}
        {5, 300, 384, 5, 0},       // Thành phố Tây đô
        {7, 400, 336, 10, 50000},   // Đảo Kame
        {19, 200, 336, 15, 200000}, // Thung lũng Namek
        {24, 350, 384, 20, 500000}, // Đồng cỏ Xayda
        {123, 50, 384, 30, 1000000} // Ngũ hành sơn
    };

    private static final String[] ESCORT_NAMES = {
        "Thành phố Tây đô",
        "Đảo Kame",
        "Thung lũng Namek",
        "Đồng cỏ Xayda",
        "Ngũ hành sơn"
    };

    // ===== Reward tiers =====
    private static final int[][] REWARD_TIERS = {
        // {điểm cần, itemId, quantity, label}
        // Tier 1: 50 điểm
        // Tier 2: 150 điểm
        // Tier 3: 300 điểm
        // Tier 4: 500 điểm
    };

    public DuongTang(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (mapId) {
                case 0 -> {
                    int diemCongDuc = player.event.getEventPointNHS();
                    createOtherMenu(player, ConstNpc.BASE_MENU,
                            "|7|━━━ SƯ PHỤ ĐƯỜNG TĂNG ━━━\n"
                            + "|1|A mi phò phò, thí chủ hãy giúp giải cứu\n"
                            + "|1|đồ đệ của bần tăng đang bị phong ấn\n"
                            + "|1|tại Ngũ Hành Sơn.\n\n"
                            + "|2|Điểm công đức: " + diemCongDuc + "\n"
                            + "|7|━━━━━━━━━━━━━━━━━━",
                            "Đi\nNgũ Hành Sơn",
                            "Nhiệm Vụ\nHộ Tống",
                            "Nhận\nThưởng",
                            "Hướng\nDẫn");
                }
                case 123 -> {
                    createOtherMenu(player, ConstNpc.BASE_MENU,
                            "|7|━━━ NGŨ HÀNH SƠN ━━━\n"
                            + "|1|Ra khỏi ngôi làng này sẽ gặp\n"
                            + "|1|ngọn núi Ngũ Hành Sơn.\n"
                            + "|2|Đánh quái ở đây tích điểm NHS!\n"
                            + "|7|━━━━━━━━━━━━━━━━━━",
                            "Về\nLàng Aru", "Đóng");
                }
                case 122 -> {
                    createOtherMenu(player, ConstNpc.BASE_MENU,
                            "|7|━━━ ĐỔI ĐIỂM NHS ━━━\n"
                            + "|2|Điểm NHS của con: " + player.event.getEventPointNHS() + "\n"
                            + "|1|Giờ muốn đổi gì thì đổi!\n"
                            + "|7|━━━━━━━━━━━━━━━━━━",
                            "Shop\nĐổi Điểm", "Đóng");
                }
                default ->
                    super.openBaseMenu(player);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {

                // ===== BASE MENU (map 0) =====
                case ConstNpc.BASE_MENU -> {
                    switch (mapId) {
                        case 0 -> handleMap0BaseMenu(player, select);
                        case 123 -> {
                            if (select == 0) {
                                ChangeMapService.gI().changeMapNonSpaceship(player, 0, Util.nextInt(700, 800), 432);
                            }
                        }
                        case 122 -> {
                            if (select == 0) {
                                ShopService.gI().opendShop(player, "SHOP_NHS", false);
                            }
                        }
                    }
                }

                // ===== ESCORT INFO =====
                case MENU_ESCORT_INFO -> handleEscortSelect(player, select);

                // ===== ESCORT CONFIRM =====
                case MENU_ESCORT_CONFIRM -> {
                    if (select == 0) {
                        startEscortMission(player);
                    }
                }

                // ===== REWARD MENU =====
                case MENU_REWARD -> handleRewardSelect(player, select);
            }
        }
    }

    // ====================================================================
    // MAP 0 BASE MENU
    // ====================================================================
    private void handleMap0BaseMenu(Player player, int select) {
        switch (select) {
            case 0 -> {
                // Đi Ngũ Hành Sơn
                ChangeMapService.gI().changeMapNonSpaceship(player, 123, 50, 384);
            }
            case 1 -> {
                // Nhiệm Vụ Hộ Tống - hiển thị danh sách tuyến hộ tống
                showEscortMenu(player);
            }
            case 2 -> {
                // Nhận Thưởng
                showRewardMenu(player);
            }
            case 3 -> {
                // Hướng Dẫn
                showGuide(player);
            }
        }
    }

    // ====================================================================
    // NHIỆM VỤ HỘ TỐNG
    // ====================================================================
    private void showEscortMenu(Player player) {
        int diemHienTai = player.event.getEventPointNHS();

        createOtherMenu(player, MENU_ESCORT_INFO,
                "|7|━━━ NHIỆM VỤ HỘ TỐNG ━━━\n"
                + "|8|Hộ tống Đường Tăng đi thỉnh chân kinh!\n\n"
                + "|1|Chọn tuyến đường hộ tống:\n"
                + "|1|1. Tây Đô (dễ) — +5 điểm\n"
                + "|1|2. Đảo Kame — +10 điểm\n"
                + "|1|3. Thung lũng Namek — +15 điểm\n"
                + "|1|4. Đồng cỏ Xayda — +20 điểm\n"
                + "|2|5. Ngũ Hành Sơn (khó) — +30 điểm\n\n"
                + "|2|Điểm hiện tại: " + diemHienTai + "\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                "Tây Đô\n+5 điểm",
                "Đảo Kame\n+10 điểm",
                "Namek\n+15 điểm",
                "Xayda\n+20 điểm");
    }

    private void handleEscortSelect(Player player, int select) {
        if (select < 0 || select > 3) return;

        int destIndex = select;
        int[] dest = ESCORT_DESTINATIONS[destIndex];
        int diemThuong = dest[3];
        int minPower = dest[4];
        String destName = ESCORT_NAMES[destIndex];

        if (minPower > 0 && player.nPoint.power < minPower) {
            Service.gI().sendThongBaoOK(player,
                    "Tuyến " + destName + " yêu cầu sức mạnh tối thiểu "
                    + Util.numberToMoney(minPower) + "!\n"
                    + "Sức mạnh hiện tại: " + Util.numberToMoney(player.nPoint.power));
            return;
        }

        // Lưu thông tin tuyến đường để xác nhận
        player.iDMark.setMenuType(destIndex);

        createOtherMenu(player, MENU_ESCORT_CONFIRM,
                "|7|━━━ XÁC NHẬN HỘ TỐNG ━━━\n"
                + "|8|Hộ tống Đường Tăng đến: " + destName + "\n\n"
                + "|1|Phần thưởng: +" + diemThuong + " điểm công đức\n"
                + (minPower > 0 ? "|1|Yêu cầu SM: " + Util.numberToMoney(minPower) + "\n" : "")
                + "|2|Bạn sẽ được dịch chuyển đến " + destName + "\n"
                + "|2|và nhận điểm khi hoàn thành.\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                "Hộ Tống\nNgay!", "Quay Lại");
    }

    private void startEscortMission(Player player) {
        int destIndex = player.iDMark.getMenuType();
        if (destIndex < 0 || destIndex >= ESCORT_DESTINATIONS.length) return;

        // Cooldown 60s chống spam
        long now = System.currentTimeMillis();
        long lastEscort = player.event.lastEscortTime;
        if (now - lastEscort < 60_000) {
            long remain = (60_000 - (now - lastEscort)) / 1000;
            Service.gI().sendThongBao(player, "Vui long cho " + remain + "s de ho tong tiep!");
            return;
        }

        int[] dest = ESCORT_DESTINATIONS[destIndex];
        int targetMap = dest[0];
        int targetX = dest[1];
        int targetY = dest[2];
        int diemThuong = dest[3];
        String destName = ESCORT_NAMES[destIndex];

        // Lưu cooldown
        player.event.lastEscortTime = now;

        // Thêm điểm công đức
        player.event.addEventPointNHS(diemThuong);

        // Chuyển map
        ChangeMapService.gI().changeMapNonSpaceship(player, targetMap, targetX, targetY);

        // Thông báo
        Service.gI().sendThongBao(player,
                "Ho tong den " + destName + " thanh cong!\n"
                + "+" + diemThuong + " diem cong duc.\n"
                + "Tong diem: " + player.event.getEventPointNHS()
                + "\nCho 60s de ho tong tiep.");
    }

    // ====================================================================
    // NHẬN THƯỞNG
    // ====================================================================
    private void showRewardMenu(Player player) {
        int diem = player.event.getEventPointNHS();

        createOtherMenu(player, MENU_REWARD,
                "|7|━━━ NHẬN THƯỞNG CÔNG ĐỨC ━━━\n"
                + "|2|Điểm công đức hiện tại: " + diem + "\n\n"
                + "|8|Chọn phần thưởng:\n"
                + (diem >= 50 ? "|2|" : "|1|") + "1. Hộp Đậu Thần (50 điểm)\n"
                + "   → x10 Đậu Thần + 5M Vàng\n"
                + (diem >= 150 ? "|2|" : "|1|") + "2. Rương Chiến Binh (150 điểm)\n"
                + "   → x3 Thỏi Vàng + 20 Đá Bảo Vệ\n"
                + (diem >= 300 ? "|2|" : "|1|") + "3. Rương Huyền Thoại (300 điểm)\n"
                + "   → x10 Thỏi Vàng + 5000 Ngọc Xanh\n"
                + (diem >= 500 ? "|2|" : "|1|") + "4. Rương Thánh Tăng (500 điểm)\n"
                + "   → x50 Thỏi Vàng + x5 Mảnh Khí Oozaru\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                "Hộp\nĐậu Thần\n50 điểm",
                "Rương\nChiến Binh\n150 điểm",
                "Rương\nHuyền Thoại\n300 điểm",
                "Rương\nThánh Tăng\n500 điểm");
    }

    private void handleRewardSelect(Player player, int select) {
        int diem = player.event.getEventPointNHS();
        int diemCan;

        switch (select) {
            case 0 -> {
                // Hộp Đậu Thần - 50 điểm
                diemCan = 50;
                if (diem < diemCan) {
                    notifyNotEnoughPoints(player, diem, diemCan);
                    return;
                }
                if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                    Service.gI().sendThongBao(player, "Hành trang đã đầy!");
                    return;
                }
                player.event.subEventPointNHS(diemCan);
                Item dauThan = ItemService.gI().createNewItem((short) 12, 10);
                InventoryService.gI().addItemBag(player, dauThan);
                player.inventory.gold += 5_000_000;
                Service.gI().sendMoney(player);
                InventoryService.gI().sendItemBag(player);
                notifyRewardSuccess(player, "Hộp Đậu Thần", diemCan);
            }
            case 1 -> {
                // Rương Chiến Binh - 150 điểm
                diemCan = 150;
                if (diem < diemCan) {
                    notifyNotEnoughPoints(player, diem, diemCan);
                    return;
                }
                if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                    Service.gI().sendThongBao(player, "Cần ít nhất 2 ô trống hành trang!");
                    return;
                }
                player.event.subEventPointNHS(diemCan);
                Item thoiVang = ItemService.gI().createNewItem((short) 457, 3);
                Item daBaoVe = ItemService.gI().createNewItem((short) 987, 20);
                InventoryService.gI().addItemBag(player, thoiVang);
                InventoryService.gI().addItemBag(player, daBaoVe);
                InventoryService.gI().sendItemBag(player);
                notifyRewardSuccess(player, "Rương Chiến Binh", diemCan);
            }
            case 2 -> {
                // Rương Huyền Thoại - 300 điểm
                diemCan = 300;
                if (diem < diemCan) {
                    notifyNotEnoughPoints(player, diem, diemCan);
                    return;
                }
                if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                    Service.gI().sendThongBao(player, "Hành trang đã đầy!");
                    return;
                }
                player.event.subEventPointNHS(diemCan);
                Item thoiVang2 = ItemService.gI().createNewItem((short) 457, 10);
                InventoryService.gI().addItemBag(player, thoiVang2);
                player.inventory.gem += 5000;
                Service.gI().sendMoney(player);
                InventoryService.gI().sendItemBag(player);
                notifyRewardSuccess(player, "Rương Huyền Thoại", diemCan);
            }
            case 3 -> {
                // Rương Thánh Tăng - 500 điểm
                diemCan = 500;
                if (diem < diemCan) {
                    notifyNotEnoughPoints(player, diem, diemCan);
                    return;
                }
                if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                    Service.gI().sendThongBao(player, "Cần ít nhất 2 ô trống hành trang!");
                    return;
                }
                player.event.subEventPointNHS(diemCan);
                Item thoiVang3 = ItemService.gI().createNewItem((short) 457, 50);
                Item manhKhi = ItemService.gI().createNewItem((short) 1901, 5);
                InventoryService.gI().addItemBag(player, thoiVang3);
                InventoryService.gI().addItemBag(player, manhKhi);
                InventoryService.gI().sendItemBag(player);
                notifyRewardSuccess(player, "Rương Thánh Tăng", diemCan);
            }
        }
    }

    // ====================================================================
    // HƯỚNG DẪN
    // ====================================================================
    private void showGuide(Player player) {
        npcChat(player,
                "|7|HUONG DAN DUONG TANG\n\n"
                + "|8|Ho tong su phu qua cac tuyen:\n"
                + "|1|Tay Do +5, Dao Kame +10\n"
                + "|1|Namek +15, Xayda +20\n"
                + "|2|Ngu Hanh Son +30 (kho)\n\n"
                + "|8|Cach kiem diem nhanh:\n"
                + "|2|Ho tong lien tuc (cd 60s)\n"
                + "|2|Danh quai Ngu Hanh Son +1/con\n\n"
                + "|8|Nhan Thuong tai day:\n"
                + "|1|50d = Dau Than + 5M Vang\n"
                + "|1|150d = 3 Thoi Vang + Da BV\n"
                + "|1|300d = 10 Thoi Vang + 5k Ngoc\n"
                + "|1|500d = 50 Thoi Vang + Manh Oozaru\n\n"
                + "|8|Shop doi diem tai Ngu Hanh Son:\n"
                + "|1|Ngoc Rong 7s=30d, 6s=80d\n"
                + "|1|5s=200d, 4s=500d, 3s=1000d");
    }

    // ====================================================================
    // HELPER METHODS
    // ====================================================================
    private void notifyNotEnoughPoints(Player player, int current, int required) {
        Service.gI().sendThongBaoOK(player,
                "Chưa đủ điểm công đức!\n"
                + "Cần: " + required + " điểm\n"
                + "Hiện có: " + current + " điểm\n"
                + "Còn thiếu: " + (required - current) + " điểm\n\n"
                + "Hãy hộ tống thêm hoặc đánh quái tại\n"
                + "Ngũ Hành Sơn để tích điểm!");
    }

    private void notifyRewardSuccess(Player player, String rewardName, int diemTru) {
        Service.gI().sendThongBao(player,
                "Nhận " + rewardName + " thành công!\n"
                + "Đã trừ " + diemTru + " điểm công đức.\n"
                + "Điểm còn lại: " + player.event.getEventPointNHS());
    }
}
