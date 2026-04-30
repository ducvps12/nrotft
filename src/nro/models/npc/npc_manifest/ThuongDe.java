package nro.models.npc.npc_manifest;

/**
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import boss.BossID;
import consts.ConstNpc;
import models.Training.TrainingService;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.NpcService;
import nro.services.Service;
import services.func.ChangeMapService;
import services.func.LuckyRound;
import models.PopoTower.PopoTowerService;
import shop.ShopService;

public class ThuongDe extends Npc {

    // Menu IDs
    private static final int MENU_DANG_KY_TAP = 2001;
    private static final int MENU_LUYEN_TAP = 2002;
    private static final int MENU_THACH_DAU = 2003;
    private static final int MENU_GIFT_MAIN = 3100;
    private static final int MENU_GIFT_COSTUMES = 3101;
    private static final int MENU_GIFT_ITEMS = 3102;
    private static final int MENU_GUIDE = 3103;
    private static final int MENU_GUIDE_LUYEN = 3110;
    private static final int MENU_GUIDE_THAP = 3111;
    private static final int MENU_GUIDE_QUAY = 3112;
    private static final int MENU_GUIDE_DESTRON = 3113;
    private static final int MENU_CHOOSE_LUCKY_ROUND_COUNT = 3114;

    public ThuongDe(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (mapId) {
                case 45 -> {
                    if (player.clan != null && player.clan.ConDuongRanDoc != null && player.joinCDRD
                            && player.clan.ConDuongRanDoc.allMobsDead && !player.talkToThuongDe) {
                        Service.gI().sendThongBao(player, "Hãy xuống gặp thần mèo Karin");
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Hãy xuống gặp thần mèo Karin", "OK");
                        return;
                    }
                    showMainMenu(player);
                }
                case 141 ->
                    this.createOtherMenu(player, 0,
                            "Hãy nắm lấy tay ta mau!", "về\nthần điện");
            }
        }
    }

    // ==========================================
    // MENU CHÍNH - Thượng Đế
    // ==========================================
    private void showMainMenu(Player player) {
        String greeting;
        switch (player.levelLuyenTap) {
            case 2 -> greeting = "PôPô là đệ tử của ta.\n"
                    + "Luyện tập với PôPô con sẽ có\nthêm nhiều kinh nghiệm.\n"
                    + "Đánh bại PôPô ta sẽ dạy\nvõ công cho con.";
            case 3 -> greeting = "Từ nay con sẽ là đệ tử\ncủa ta.\n"
                    + "Ta sẽ truyền cho con\ntất cả tuyệt kĩ.";
            default -> greeting = "Con đã mạnh hơn ta.\n"
                    + "Ta sẽ chỉ đường cho con\nđến Kaio.\n"
                    + "Hãy theo Thần Vũ Trụ\nhọc võ công.";
        }

        this.createOtherMenu(player, ConstNpc.BASE_MENU, greeting,
                player.dangKyTapTuDong ? "Hủy\ntập t.động" : "Đăng ký\ntập t.động",
                "Luyện tập", "Thách đấu",
                "Tháp PôPô", "Đến Kaio",
                "Vòng quay\nMay Mắn", "Quà\nThần Điện");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (mapId) {
                case 45 -> handleMap45(player, select);
                case 141 -> {
                    switch (select) {
                        case 0 -> {
                            if (player.clan == null || player.clan.ConDuongRanDoc == null
                                    || !player.clan.ConDuongRanDoc.allMobsDead) {
                                Service.gI().sendThongBao(player, "Chưa hạ hết đối thủ");
                                return;
                            }
                            ChangeMapService.gI().changeMapYardrat(player,
                                    ChangeMapService.gI().getMapCanJoin(player, 45), 295, 408);
                            Service.gI().sendThongBao(player, "Hãy xuống gặp thần mèo Karin");
                        }
                    }
                }
            }
        }
    }

    private void handleMap45(Player player, int select) {
        if (player.iDMark.isBaseMenu()) {
            handleBaseMenu(player, select);
        } else {
            handleSubMenus(player, select);
        }
    }

    // ==========================================
    // XỬ LÝ BASE MENU
    // ==========================================
    private void handleBaseMenu(Player player, int select) {
        switch (select) {
            case 0 -> { // Đăng ký/Hủy tập tự động
                if (player.clan != null && player.clan.ConDuongRanDoc != null && player.joinCDRD
                        && player.clan.ConDuongRanDoc.allMobsDead && !player.talkToThuongDe) {
                    player.talkToThuongDe = true;
                    return;
                }
                if (player.dangKyTapTuDong) {
                    player.dangKyTapTuDong = false;
                    NpcService.gI().createTutorial(player, tempId, avartar,
                            "Con đã hủy đăng ký\ntập tự động thành công.");
                    return;
                }
                this.createOtherMenu(player, MENU_DANG_KY_TAP,
                        "Khi Offline quá 30 phút,\ncon sẽ tự động luyện tập.\n\n"
                                + "Tốc độ: 1280 SM/phút\n"
                                + "Phí: 1 ngọc mỗi lần",
                        "Hướng dẫn", "Đồng ý", "Từ chối");
            }
            case 1 -> { // Luyện tập
                String info;
                switch (player.levelLuyenTap) {
                    case 3 -> info = "Tập luyện với Thượng Đế\ntăng 160 SM mỗi phút.";
                    default -> info = "Tập luyện với Mr.PôPô\ntăng 80 SM mỗi phút.";
                }
                this.createOtherMenu(player, MENU_LUYEN_TAP, info, "Đồng ý", "Từ chối");
            }
            case 2 -> { // Thách đấu
                String info;
                switch (player.levelLuyenTap) {
                    case 2 -> info = "Thắng Mr.PôPô sẽ được\ntập với ta (+160 SM/phút).";
                    case 3 -> info = "Thắng ta sẽ được học\nvới người mạnh hơn\n(+320 SM/phút).";
                    default -> info = "Tập luyện với Thượng Đế\ntăng 160 SM mỗi phút.";
                }
                this.createOtherMenu(player, MENU_THACH_DAU, info, "Đồng ý", "Từ chối");
            }
            case 3 -> // Tháp PôPô
                PopoTowerService.gI().openMenu(player, this);
            case 4 -> // Đến Kaio
                ChangeMapService.gI().changeMapBySpaceShip(player, 48, -1, 354);
            case 5 -> // Vòng quay May Mắn
                showLuckyRoundMenu(player);
            case 6 -> // Quà Thần Điện
                showGiftMainMenu(player);
        }
    }

    // ==========================================
    // XỬ LÝ SUB MENUS
    // ==========================================
    private void handleSubMenus(Player player, int select) {
        int menu = player.iDMark.getIndexMenu();

        // Đăng ký tập tự động
        if (menu == MENU_DANG_KY_TAP) {
            switch (select) {
                case 0 -> NpcService.gI().createTutorial(player, tempId, avartar, ConstNpc.TAP_TU_DONG);
                case 1 -> {
                    player.mapIdDangTapTuDong = mapId;
                    player.dangKyTapTuDong = true;
                    NpcService.gI().createTutorial(player, tempId, avartar,
                            "Đăng ký thành công!\nOffline 30 phút sẽ tự\nđộng luyện tập.");
                }
            }
        }
        // Luyện tập
        else if (menu == MENU_LUYEN_TAP) {
            if (select == 0) {
                switch (player.levelLuyenTap) {
                    case 3 -> TrainingService.gI().callBoss(player, BossID.THUONG_DE, false);
                    default -> TrainingService.gI().callBoss(player, BossID.MRPOPO, false);
                }
            }
        }
        // Thách đấu
        else if (menu == MENU_THACH_DAU) {
            if (select == 0) {
                switch (player.levelLuyenTap) {
                    case 2 -> TrainingService.gI().callBoss(player, BossID.MRPOPO, true);
                    case 3 -> TrainingService.gI().callBoss(player, BossID.THUONG_DE, true);
                    default -> TrainingService.gI().callBoss(player, BossID.THUONG_DE, false);
                }
            }
        }
        // Tháp PôPô
        else if (menu == ConstNpc.MENU_POPO_TOWER) {
            PopoTowerService.gI().handleMenu(player, this, select);
        }
        // Vòng quay May Mắn - menu chọn loại
        else if (menu == ConstNpc.MENU_CHOOSE_LUCKY_ROUND) {
            switch (select) {
                case 0 -> { // Quay bằng Vàng (gold)
                    player.iDMark.setTypeLuckyRound(LuckyRound.USING_GOLD);
                    showLuckyRoundCountMenu(player, "Vàng", "25tr vàng/lượt");
                }
                case 1 -> { // Quay bằng Ngọc (gem)
                    player.iDMark.setTypeLuckyRound(LuckyRound.USING_GEM);
                    showLuckyRoundCountMenu(player, "Ngọc", "4 ngọc/lượt");
                }
                case 2 -> { // Quay bằng Thỏi Vàng (ticket)
                    player.iDMark.setTypeLuckyRound(LuckyRound.USING_TICKET);
                    showLuckyRoundCountMenu(player, "Thỏi Vàng", "1 thỏi/lượt");
                }
                case 3 -> // Rương phụ
                    ShopService.gI().opendShop(player, "ITEMS_LUCKY_ROUND", true);
                case 4 -> // Xóa rương
                    NpcService.gI().createMenuConMeo(player,
                            ConstNpc.CONFIRM_REMOVE_ALL_ITEM_LUCKY_ROUND, this.avartar,
                            "Xóa hết vật phẩm trong\nrương phụ?\n\nKhông thể khôi phục!",
                            "Đồng ý", "Hủy bỏ");
            }
        }
        // Vòng quay May Mắn - Chọn số lượng quay
        else if (menu == MENU_CHOOSE_LUCKY_ROUND_COUNT) {
            byte type = player.iDMark.getTypeLuckyRound();
            switch (select) {
                case 0 -> LuckyRound.gI().openCrackBallUI(player, type);
                case 1 -> fastPlayLuckyRound(player, type, (byte) 10);
                case 2 -> fastPlayLuckyRound(player, type, (byte) 50);
                case 3 -> fastPlayLuckyRound(player, type, (byte) 100);
            }
        }
        // Quà Thần Điện - menu chính
        else if (menu == MENU_GIFT_MAIN) {
            switch (select) {
                case 0 -> showGiftCostumes(player);
                case 1 -> showGiftItems(player);
                case 2 -> showGuideMenu(player);
            }
        }
        // Quà - cải trang
        else if (menu == MENU_GIFT_COSTUMES) {
            if (select == 0) showGiftMainMenu(player);
        }
        // Quà - vật phẩm
        else if (menu == MENU_GIFT_ITEMS) {
            if (select == 0) showGiftMainMenu(player);
        }
        // Hướng dẫn - menu chính
        else if (menu == MENU_GUIDE) {
            switch (select) {
                case 0 -> showGuideLuyen(player);
                case 1 -> showGuideThap(player);
                case 2 -> showGuideQuay(player);
                case 3 -> showGuideDestron(player);
                case 4 -> showGiftMainMenu(player);
            }
        }
        // Hướng dẫn chi tiết - quay lại
        else if (menu == MENU_GUIDE_LUYEN || menu == MENU_GUIDE_THAP
                || menu == MENU_GUIDE_QUAY || menu == MENU_GUIDE_DESTRON) {
            if (select == 0) showGuideMenu(player);
        }
    }

    // ==========================================
    // VÒNG QUAY MAY MẮN
    // ==========================================
    private void showLuckyRoundMenu(Player player) {
        int itemCount = player.inventory.itemsBoxCrackBall.size()
                - InventoryService.gI().getCountEmptyListItem(player.inventory.itemsBoxCrackBall);
        this.createOtherMenu(player, ConstNpc.MENU_CHOOSE_LUCKY_ROUND,
                "Chọn loại vòng quay:\n\n"
                        + "Quay Vàng: 25tr vàng/lượt\n"
                        + "Quay Ngọc: 4 ngọc/lượt\n"
                        + "Quay Thỏi Vàng: 1 thỏi/lượt\n\n"
                        + "Rương phụ: " + itemCount + "/100 món",
                "Quay bằng\nVàng", "Quay bằng\nNgọc",
                "Quay bằng\nThỏi Vàng",
                "Rương phụ\n(" + itemCount + " món)",
                "Xóa hết\ntrong rương", "Đóng");
    }

    private void showLuckyRoundCountMenu(Player player, String name, String price) {
        this.createOtherMenu(player, MENU_CHOOSE_LUCKY_ROUND_COUNT,
                "Chọn số lượng Quay bằng " + name + ":\nGiá: " + price + "\n"
                        + "Lưu ý: Rương phụ chứa tối đa 100 món.",
                "Quay tự\nchọn (UI)", "Quay nhanh\nx10", "Quay nhanh\nx50", "Quay nhanh\nx100", "Đóng");
    }

    private void fastPlayLuckyRound(Player player, byte type, byte count) {
        int emptyCount = 100 - player.inventory.itemsBoxCrackBall.size();
        if (count > emptyCount) {
            Service.gI().sendThongBao(player, "Rương phụ không đủ chỗ trống! (Chỉ còn " + emptyCount + " ô trống)");
            return;
        }

        switch (type) {
            case LuckyRound.USING_GOLD -> LuckyRound.gI().openBallByGold(player, count);
            case LuckyRound.USING_GEM -> LuckyRound.gI().openBallByGem(player, count);
            case LuckyRound.USING_TICKET -> LuckyRound.gI().openBallByTicket(player, count);
        }
        Service.gI().sendThongBao(player, "Đã quay xong " + count + " lượt. Mở Rương phụ để xem quà!");
    }

    // ==========================================
    // QUÀ THẦN ĐIỆN
    // ==========================================
    private void showGiftMainMenu(Player player) {
        this.createOtherMenu(player, MENU_GIFT_MAIN,
                "Chao " + player.name + "!\n\n"
                        + "Than Dien ban tang phan\n"
                        + "thuong dac biet cho chien binh.\n\n"
                        + "Cach nhan qua:\n"
                        + "- Vong quay May Man\n"
                        + "- Thap PoPo (clear tang)\n"
                        + "- Luyen tap voi Thuong De\n"
                        + "- Destron Gas (tai Mr.PoPo)",
                "Cai Trang", "Vat Pham", "Huong Dan", "Dong");
    }

    private void showGiftCostumes(Player player) {
        this.createOtherMenu(player, MENU_GIFT_COSTUMES,
                "CAI TRANG HAP DAN\n\n"
                        + "Vong Quay Thoi Vang (VIP):\n"
                        + "- CT SSR: SD+35-50% HP+35-50%\n"
                        + "  KI+35-50% (Cuc hiem!)\n"
                        + "- CT VIP: SD+20-40% HP+20-40%\n\n"
                        + "Vong Quay Vang/Ngoc:\n"
                        + "- CT Tot: SD+20-30% HP+20-30%\n"
                        + "- CT Thuong: SD+10-25% HP+10-25%\n\n"
                        + "Shop Xu (Santa):\n"
                        + "- CT Co ban: SD+10-18%\n"
                        + "  HP+10-18% KI+10-18%",
                "Quay lai");
    }

    private void showGiftItems(Player player) {
        this.createOtherMenu(player, MENU_GIFT_ITEMS,
                "VAT PHAM DAC BIET\n\n"
                        + "Tu Vong Quay May Man:\n"
                        + "- Thoi vang (ban duoc vang)\n"
                        + "- Capsule thoi trang 5-7 ngay\n"
                        + "- Sach tien hoa Lv1-5\n"
                        + "- Da xanh lam nang cap\n\n"
                        + "Tu Thap PoPo:\n"
                        + "- Vang + Ngoc theo tang\n"
                        + "- Trang bi manh (tang cao)\n"
                        + "- Do hiem (tang 50+)\n\n"
                        + "Tu Rong Than 1 Sao:\n"
                        + "- 100K ngoc + 100 thoi vang\n"
                        + "- +2 Ty SM va tiem nang\n"
                        + "- Gang tay len cap, CM+2%",
                "Quay lai");
    }

    // ==========================================
    // HƯỚNG DẪN CHI TIẾT
    // ==========================================
    private void showGuideMenu(Player player) {
        this.createOtherMenu(player, MENU_GUIDE,
                "HUONG DAN THAN DIEN\n\n"
                        + "Chon muc ban muon xem:\n\n"
                        + "1. Luyen tap va thang cap\n"
                        + "2. Thap PoPo va phan thuong\n"
                        + "3. Vong quay May Man\n"
                        + "4. Destron Gas (bang hoi)",
                "Luyen tap", "Thap PoPo",
                "Vong quay", "Destron Gas",
                "Quay lai");
    }

    private void showGuideLuyen(Player player) {
        this.createOtherMenu(player, MENU_GUIDE_LUYEN,
                "LUYEN TAP VA THANG CAP\n\n"
                        + "Buoc 1: Tap voi Mr.PoPo\n"
                        + "- Tang 80 SM/phut\n"
                        + "- Thang PoPo = mo Thuong De\n\n"
                        + "Buoc 2: Tap voi Thuong De\n"
                        + "- Tang 160 SM/phut\n"
                        + "- Thang = mo Kaio (+320)\n\n"
                        + "Buoc 3: Tap tu dong\n"
                        + "- Offline 30 phut = tu tap\n"
                        + "- Toc do 1280 SM/phut\n"
                        + "- Phi 1 ngoc/lan dang ky",
                "Quay lai");
    }

    private void showGuideThap(Player player) {
        this.createOtherMenu(player, MENU_GUIDE_THAP,
                "THAP POPO VA PHAN THUONG\n\n"
                        + "Cach choi:\n"
                        + "- Clear tang = nhan thuong\n"
                        + "- Cang cao cang nhieu qua\n"
                        + "- Reset hang ngay\n\n"
                        + "Phan thuong:\n"
                        + "- Tang 1-20: Vang + EXP\n"
                        + "- Tang 20-50: Ngoc + Do tot\n"
                        + "- Tang 50+: Do hiem, CT VIP",
                "Quay lai");
    }

    private void showGuideQuay(Player player) {
        this.createOtherMenu(player, MENU_GUIDE_QUAY,
                "VONG QUAY MAY MAN\n\n"
                        + "3 loai vong quay:\n\n"
                        + "1. Quay bang Vang\n"
                        + "- Gia: 25 trieu vang/luot\n"
                        + "- Thuong: do thuong + vang\n\n"
                        + "2. Quay bang Ngoc\n"
                        + "- Gia: 4 ngoc/luot\n"
                        + "- Thuong: cai trang, do VIP\n\n"
                        + "3. Quay bang Thoi Vang\n"
                        + "- Gia: 1 thoi vang/luot\n"
                        + "- Thuong: cai trang hiem",
                "Quay lai");
    }

    private void showGuideDestron(Player player) {
        this.createOtherMenu(player, MENU_GUIDE_DESTRON,
                "DESTRON GAS (BANG HOI)\n\n"
                        + "Yeu cau:\n"
                        + "- Can co bang hoi\n"
                        + "- Noi chuyen Mr.PoPo\n\n"
                        + "Cach choi:\n"
                        + "- Ca bang vao danh boss\n"
                        + "- Diem = xep hang bang\n\n"
                        + "Phan thuong:\n"
                        + "- Diem danh vong bang hoi\n"
                        + "- Vang + Ngoc + Do hiem\n"
                        + "- Top bang = thuong lon",
                "Quay lai");
    }
}
