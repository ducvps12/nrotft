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
                case 4 -> { // Xóa rương - Làm mới về 0
                    int count = player.inventory.itemsBoxCrackBall.size()
                            - InventoryService.gI().getCountEmptyListItem(player.inventory.itemsBoxCrackBall);
                    NpcService.gI().createMenuConMeo(player,
                            ConstNpc.CONFIRM_REMOVE_ALL_ITEM_LUCKY_ROUND, this.avartar,
                            "|8|⚠ XÁC NHẬN LÀM MỚI RƯƠNG\n\n"
                                    + "|1|Số vật phẩm sẽ bị xóa: " + count + "\n\n"
                                    + "|2|Toàn bộ vật phẩm trong rương\n"
                                    + "|2|sẽ bị hủy VĨNH VIỄN!\n\n"
                                    + "|8|Không thể khôi phục!",
                            "Xóa hết\n(" + count + " món)", "Hủy bỏ");
                }
            }
        }
        // Vòng quay May Mắn - Chọn số lượng quay
        else if (menu == MENU_CHOOSE_LUCKY_ROUND_COUNT) {
            byte type = player.iDMark.getTypeLuckyRound();
            switch (select) {
                case 0 -> LuckyRound.gI().openCrackBallUI(player, type);
                case 1 -> fastPlayLuckyRound(player, type, 10);
                case 2 -> fastPlayLuckyRound(player, type, 50);
                case 3 -> fastPlayLuckyRound(player, type, 100);
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
                "|7|━━━ VÒNG QUAY MAY MẮN ━━━\n"
                        + "|1|Quay Vàng: 25 triệu vàng/lượt\n"
                        + "|1|Quay Ngọc: 4 ngọc/lượt\n"
                        + "|2|Quay Thỏi Vàng (VIP): 1 thỏi/lượt\n\n"
                        + "|8|★ PHẦN THƯỞNG THỎI VÀNG:\n"
                        + "|2|» GOD: Goku Blue +60% ALL\n"
                        + "|2|» SSR: Hào quang God\n"
                        + "|2|» Thú cưỡi Siêu Cấp / Pet Po\n"
                        + "|2|» Set Thần Linh, Porata +2\n"
                        + "|2|» Chân Mệnh c9, Sách TK2\n"
                        + "|2|» CT VIP 35-50% chỉ số\n\n"
                        + "|8|Rương phụ: " + itemCount + "/1000 món\n"
                        + "|7|━━━━━━━━━━━━━━━━━━",
                "Quay bằng\nVàng", "Quay bằng\nNgọc",
                "Quay bằng\nThỏi Vàng",
                "Rương phụ\n(" + itemCount + " món)",
                "Xóa hết\ntrong rương", "Đóng");
    }

    private void showLuckyRoundCountMenu(Player player, String name, String price) {
        String vipPreview = name.equals("Thỏi Vàng")
                ? "|8|\n★ JACKPOT: Goku Blue GOD\n"
                        + "|8|★ Thú cưỡi rồng Siêu Cấp\n"
                        + "|8|★ Full set Thần Linh\n"
                        + "|8|★ CT VIP chỉ số 35-50%\n"
                : "";
        this.createOtherMenu(player, MENU_CHOOSE_LUCKY_ROUND_COUNT,
                "|7|━━━ QUAY BẰNG " + name.toUpperCase() + " ━━━\n"
                        + "|1|Giá: " + price + "\n"
                        + vipPreview
                        + "|8|Rương phụ chứa tối đa 1000 món.\n"
                        + "|7|━━━━━━━━━━━━━━━━━━",
                "Quay tự\nchọn (UI)", "Quay nhanh\nx10", "Quay nhanh\nx50", "Quay nhanh\nx100", "Đóng");
    }

    private void fastPlayLuckyRound(Player player, byte type, int count) {
        int emptyCount = 1000 - player.inventory.itemsBoxCrackBall.size();
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
                "|7|━━━ QUÀ THẦN ĐIỆN ━━━\n"
                        + "|1|Chào " + player.name + "!\n\n"
                        + "|8|Thần Điện ban tặng phần\n"
                        + "|8|thưởng đặc biệt cho chiến binh.\n\n"
                        + "|2|Cách nhận quà:\n"
                        + "|1|• Vòng Quay May Mắn\n"
                        + "|1|• Tháp PôPô (clear tầng)\n"
                        + "|1|• Luyện tập với Thượng Đế\n"
                        + "|1|• Destron Gas (tại Mr.PôPô)\n"
                        + "|7|━━━━━━━━━━━━━━━━━━",
                "Cải Trang", "Vật Phẩm", "Hướng Dẫn", "Đóng");
    }

    private void showGiftCostumes(Player player) {
        this.createOtherMenu(player, MENU_GIFT_COSTUMES,
                "|7|━━ CẢI TRANG HẤP DẪN ━━\n\n"
                        + "|2|▶ Quay Thỏi Vàng (VIP):\n"
                        + "|8|• GOD: Goku Blue +60% ALL (1/10K)\n"
                        + "|8|• SSR: Hào quang God (1/5K)\n"
                        + "|8|• CT VIP: SĐ+20-40% HP+20-40%\n"
                        + "|8|  Tỉ lệ ra CT: ~2%\n\n"
                        + "|2|▶ Quay Vàng / Ngọc:\n"
                        + "|8|• CT Tốt: SĐ+20-30% HP+20-30%\n"
                        + "|8|  Tỉ lệ: 1/20 (5%)\n"
                        + "|8|• CT Thường: SĐ+10-25% HP+10-25%\n"
                        + "|8|  Tỉ lệ: 50%\n\n"
                        + "|2|▶ Shop Xu (Santa):\n"
                        + "|8|• CT Cơ bản: SĐ+10-18%\n"
                        + "|8|  HP+10-18% KI+10-18%",
                "Quay lại");
    }

    private void showGiftItems(Player player) {
        this.createOtherMenu(player, MENU_GIFT_ITEMS,
                "|7|━━ VẬT PHẨM ĐẶC BIỆT ━━\n\n"
                        + "|2|▶ Từ Vòng Quay Thỏi Vàng:\n"
                        + "|8|• Goku Blue GOD +60% ALL\n"
                        + "|8|• Thú cưỡi rồng Siêu Cấp\n"
                        + "|8|• Pet Po siêu đáng yêu\n"
                        + "|8|• Set Thần Linh (5 món)\n"
                        + "|8|• Bông tai Porata +2\n\n"
                        + "|2|▶ Từ Tháp PôPô (50 tầng):\n"
                        + "|8|• T10+: Capsule, Đá nâng cấp\n"
                        + "|8|• T20+: Thỏi vàng, Hộp SKH\n"
                        + "|8|• T30+: Mảnh BT, Sách TK2\n"
                        + "|8|• T40+: Pet Po, Thú cưỡi\n"
                        + "|8|• T50: JACKPOT cực khủng!\n\n"
                        + "|2|▶ Từ Destron Gas (Mr.PôPô):\n"
                        + "|8|• Boss drop Cải trang mạnh\n"
                        + "|8|• Lv60+: SKH, Sách kỹ năng\n"
                        + "|8|• Lv100: Pet Po, Thú cưỡi",
                "Quay lại");
    }

    // ==========================================
    // HƯỚNG DẪN CHI TIẾT
    // ==========================================
    private void showGuideMenu(Player player) {
        this.createOtherMenu(player, MENU_GUIDE,
                "|7|━━ HƯỚNG DẪN THẦN ĐIỆN ━━\n\n"
                        + "|8|Chọn mục bạn muốn xem:\n\n"
                        + "|1|1. Luyện tập và thăng cấp\n"
                        + "|1|2. Tháp PôPô và phần thưởng\n"
                        + "|1|3. Vòng Quay May Mắn\n"
                        + "|1|4. Destron Gas (bang hội)",
                "Luyện Tập", "Tháp PôPô",
                "Vòng Quay", "Destron Gas",
                "Quay lại");
    }

    private void showGuideLuyen(Player player) {
        this.createOtherMenu(player, MENU_GUIDE_LUYEN,
                "|7|━━ LUYỆN TẬP VÀ THĂNG CẤP ━━\n\n"
                        + "|2|▶ Bước 1: Tập với Mr.PôPô\n"
                        + "|8|• Tăng 80 SM/phút\n"
                        + "|8|• Thắng PôPô = mở Thượng Đế\n\n"
                        + "|2|▶ Bước 2: Tập với Thượng Đế\n"
                        + "|8|• Tăng 160 SM/phút\n"
                        + "|8|• Thắng = mở Kaio (+320)\n\n"
                        + "|2|▶ Bước 3: Tập tự động\n"
                        + "|8|• Offline 30 phút = tự tập\n"
                        + "|8|• Tốc độ 1280 SM/phút\n"
                        + "|8|• Phí 1 ngọc/lần đăng ký",
                "Quay lại");
    }

    private void showGuideThap(Player player) {
        this.createOtherMenu(player, MENU_GUIDE_THAP,
                "|7|━━ THÁP PÔPÔ (50 TẦNG) ━━\n\n"
                        + "|2|▶ Cách chơi:\n"
                        + "|8|• 8 lượt miễn phí/ngày\n"
                        + "|8|• Solo 1v1, 3 phút/trận\n"
                        + "|8|• Tầng càng cao PôPô càng mạnh\n\n"
                        + "|2|▶ Phần thưởng mỗi tầng:\n"
                        + "|8|• Xu NRO + Vàng + Ngọc\n"
                        + "|8|• T10+: Capsule, Đá nâng cấp\n"
                        + "|8|• T20+: Thỏi vàng, Hộp SKH\n"
                        + "|8|• T30+: Mảnh BT, Sách TK2\n"
                        + "|8|• T40+: Pet Po (0.5%)\n\n"
                        + "|2|▶ Mốc đặc biệt (5→50):\n"
                        + "|8|• T50: 500 Xu + 500 ngọc\n"
                        + "|8|  + 50 TV + Pet Po + Sách TK2",
                "Quay lại");
    }

    private void showGuideQuay(Player player) {
        this.createOtherMenu(player, MENU_GUIDE_QUAY,
                "|7|━━ VÒNG QUAY MAY MẮN ━━\n\n"
                        + "|2|▶ 1. Quay Vàng/Ngọc:\n"
                        + "|8|• 50% CT SĐ+10-25%\n"
                        + "|8|• 5% CT Tốt SĐ+20-30%\n"
                        + "|8|• Còn lại: Vàng/Sách/Đá\n\n"
                        + "|2|▶ 2. Quay Thỏi Vàng (VIP):\n"
                        + "|8|• 1/10K: Goku Blue GOD +60%\n"
                        + "|8|• 1/5K: Hào quang God SSR\n"
                        + "|8|• 1/2K: Thú cưỡi/Pet huyền thoại\n"
                        + "|8|• 1/500: Set Thần Linh, Porata\n"
                        + "|8|• 1/100: Chân Mệnh, Sách TK2\n"
                        + "|8|• 2% CT VIP 20-50% chỉ số\n"
                        + "|8|• 5% Hộp SKH, Mảnh bông tai\n"
                        + "|8|• 20% Capsule/Sách/Đá\n"
                        + "|8|• Còn lại: Vàng thưởng",
                "Quay lại");
    }

    private void showGuideDestron(Player player) {
        this.createOtherMenu(player, MENU_GUIDE_DESTRON,
                "|7|━━ DESTRON GAS (BANG HỘI) ━━\n\n"
                        + "|2|▶ Yêu cầu:\n"
                        + "|8|• Có bang hội, bang chủ mở\n"
                        + "|8|• Nói chuyện Mr.PôPô\n"
                        + "|8|• Tối đa 3 lượt/ngày/bang\n\n"
                        + "|2|▶ Cách chơi:\n"
                        + "|8|• Chọn cấp 1-110, đánh quái\n"
                        + "|8|• Hạ quái → Boss DrLychee\n"
                        + "|8|• Hạ tiếp → Boss Hatchiyack\n"
                        + "|8|• Thời gian: 30 phút\n\n"
                        + "|2|▶ Phần thưởng hoàn thành:\n"
                        + "|8|• Vàng + Ngọc + Xu NRO\n"
                        + "|8|• Clear Boss = x2 thưởng\n"
                        + "|8|• Lv20+: Thỏi vàng\n"
                        + "|8|• Lv60+: SKH, Sách kỹ năng\n"
                        + "|8|• Lv100: 0.5% Pet Po!",
                "Quay lại");
    }
}
