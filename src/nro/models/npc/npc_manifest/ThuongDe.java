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
                    switch (player.levelLuyenTap) {
                        case 2 ->
                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                    "Pôpô là đệ tử của ta, luyện tập với Pôpô con sẽ có thêm nhiều kinh nghiệm\nđánh bại được Pôpô ta sẽ dạy võ công cho con",
                                    player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động" : "Đăng ký\ntập\ntự động",
                                    "Tập luyện\nvới\nMr.PôPô", "Thách đấu\nMr.PôPô", "Tháp\nPôPô", "Đến\nKaio", "Quay ngọc\nMay mắn",
                                    "Quà\nThần Điện");
                        case 3 ->
                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                    "Từ nay con sẽ là đệ tử của ta. Ta sẽ truyền cho con tất cả tuyệt kĩ",
                                    player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động" : "Đăng ký\ntập\ntự động",
                                    "Tập luyện\nvới\nThượng Đế", "Thách đấu\nThượng Đế", "Tháp\nPôPô", "Đến\nKaio",
                                    "Quay ngọc\nMay mắn", "Quà\nThần Điện");
                        default ->
                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                    "Con đã mạnh hơn ta, ta sẽ chỉ đường cho con đến Kaio\nđể gặp thần Vũ Trụ Phương Bắc\nNgài là thần cai quản vũ trụ này, hãy theo ngài ấy học võ công.",
                                    player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động" : "Đăng ký\ntập\ntự động",
                                    "Tập luyện\nvới\nMr.PôPô", "Tập luyện\nvới\nThượng Đế", "Tháp\nPôPô", "Đến\nKaio",
                                    "Quay ngọc\nMay mắn", "Quà\nThần Điện");
                    }
                }
                case 141 ->
                    this.createOtherMenu(player, 0,
                            "Hãy nắm lấy tay ta mau!", "về\nthần điện");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (mapId) {
                case 45 -> {
                    if (player.iDMark.isBaseMenu()) {
                        switch (select) {
                            case 0 -> {
                                if (player.clan != null && player.clan.ConDuongRanDoc != null && player.joinCDRD
                                        && player.clan.ConDuongRanDoc.allMobsDead && !player.talkToThuongDe) {
                                    player.talkToThuongDe = true;
                                    return;
                                }
                                if (player.dangKyTapTuDong) {
                                    player.dangKyTapTuDong = false;
                                    NpcService.gI().createTutorial(player, tempId, avartar,
                                            "Con đã hủy thành công đăng ký tập tự động\ntừ giờ con muốn tập Offline hãy tự đến đây trước");
                                    return;
                                }
                                this.createOtherMenu(player, 2001,
                                        "Đăng ký để mỗi khi Offline quá 30 phút, con sẽ được tự động luyện tập với tốc độ 1280 sức mạnh mỗi phút",
                                        "Hướng\ndẫn\nthêm", "Đồng ý\n1 ngọc\nmỗi lần", "Không\nđồng ý");
                            }
                            case 1 -> {
                                switch (player.levelLuyenTap) {
                                    case 3 ->
                                        this.createOtherMenu(player, 2002,
                                                "Con có chắc muốn tập luyện ?\nTập luyện với ta sẽ tăng 160 sức mạnh mỗi phút",
                                                "Đồng ý\nluyện tập", "Không\nđồng ý");
                                    default ->
                                        this.createOtherMenu(player, 2002,
                                                "Con có chắc muốn tập luyện ?\nTập luyện với Mr.PôPô sẽ tăng 80 sức mạnh mỗi phút",
                                                "Đồng ý\nluyện tập", "Không\nđồng ý");
                                }
                            }
                            case 2 -> {
                                switch (player.levelLuyenTap) {
                                    case 2 ->
                                        this.createOtherMenu(player, 2003,
                                                "Con có chắc muốn thách đấu ?\nNếu thắng Mr.PôPô sẽ được tập với ta, tăng 160 sức mạnh mỗi phút",
                                                "Đồng ý\ngiao đấu", "Không\nđồng ý");
                                    case 3 ->
                                        this.createOtherMenu(player, 2003,
                                                "Con có chắc muốn thách đấu ?\nNếu thắng được ta, con sẽ được học võ với người mạnh hơn ta để tăng đến 320 sức mạnh mỗi phút",
                                                "Đồng ý\ngiao đấu", "Không\nđồng ý");
                                    default ->
                                        this.createOtherMenu(player, 2003,
                                                "Con có chắc muốn tập luyện ?\nTập luyện với ta sẽ tăng 160 sức mạnh mỗi phút",
                                                "Đồng ý\nluyện tập", "Không\nđồng ý");
                                }
                            }
                            case 3 ->
                                PopoTowerService.gI().openMenu(player, this);
                            case 4 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 48, -1, 354);
                            case 5 ->
                                this.createOtherMenu(player, ConstNpc.MENU_CHOOSE_LUCKY_ROUND,
                                        "Con muốn làm gì nào?", "Quay bằng\nthỏi vàng",
                                        "Vòng quay\nđặc biệt",
                                        "Rương phụ\n("
                                                + (player.inventory.itemsBoxCrackBall.size()
                                                        - InventoryService.gI().getCountEmptyListItem(
                                                                player.inventory.itemsBoxCrackBall))
                                                + " món)",
                                        "Xóa hết\ntrong rương", "Đóng");
                            case 6 ->
                                showGiftMainMenu(player);
                        }
                    } else if (player.iDMark.getIndexMenu() == 2001) {
                        switch (select) {
                            case 0 ->
                                NpcService.gI().createTutorial(player, tempId, avartar, ConstNpc.TAP_TU_DONG);
                            case 1 -> {
                                player.mapIdDangTapTuDong = mapId;
                                player.dangKyTapTuDong = true;
                                NpcService.gI().createTutorial(player, tempId, avartar,
                                        "Từ giờ, quá 30 phút Offline con sẽ được tự động luyện tập");
                            }
                        }

                    } else if (player.iDMark.getIndexMenu() == 2002) {
                        switch (player.levelLuyenTap) {
                            case 3 ->
                                TrainingService.gI().callBoss(player, BossID.THUONG_DE, false);
                            default ->
                                TrainingService.gI().callBoss(player, BossID.MRPOPO, false);
                        }
                    } else if (player.iDMark.getIndexMenu() == 2003) {
                        switch (player.levelLuyenTap) {
                            case 2 ->
                                TrainingService.gI().callBoss(player, BossID.MRPOPO, true);
                            case 3 ->
                                TrainingService.gI().callBoss(player, BossID.THUONG_DE, true);
                            default ->
                                TrainingService.gI().callBoss(player, BossID.THUONG_DE, false);
                        }
                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_POPO_TOWER) {
                        PopoTowerService.gI().handleMenu(player, this, select);
                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_CHOOSE_LUCKY_ROUND) {
                        switch (select) {
                            case 0 ->
                                LuckyRound.gI().openCrackBallUI(player, LuckyRound.USING_TICKET);
                            case 1 ->
                                LuckyRound.gI().openCrackBallUI(player, LuckyRound.USING_GEM);
                            case 2 ->
                                ShopService.gI().opendShop(player, "ITEMS_LUCKY_ROUND", true);
                            case 3 ->
                                NpcService.gI().createMenuConMeo(player,
                                        ConstNpc.CONFIRM_REMOVE_ALL_ITEM_LUCKY_ROUND, this.avartar,
                                        "Con có chắc muốn xóa hết vật phẩm trong rương phụ? Sau khi xóa "
                                                + "sẽ không thể khôi phục!",
                                        "Đồng ý", "Hủy bỏ");
                        }
                    } else if (player.iDMark.getIndexMenu() == 3100) {
                        // QUÀ THẦN ĐIỆN - MENU CHÍNH
                        switch (select) {
                            case 0 -> showGiftCostumes(player);
                            case 1 -> showGiftItems(player);
                            case 2 -> showTempleGuide(player);
                            case 3 -> { } // Đóng
                        }
                    } else if (player.iDMark.getIndexMenu() == 3101) {
                        // Xem cải trang chi tiết
                        if (select == 0) showGiftMainMenu(player);
                    } else if (player.iDMark.getIndexMenu() == 3102) {
                        // Xem vật phẩm chi tiết
                        if (select == 0) showGiftMainMenu(player);
                    } else if (player.iDMark.getIndexMenu() == 3103) {
                        // Hướng dẫn chi tiết
                        if (select == 0) showGiftMainMenu(player);
                    }
                }
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

    // ==========================================
    // QUÀ THẦN ĐIỆN - CẢI TRANG & PHẦN THƯỢNG
    // ==========================================
    private void showGiftMainMenu(Player player) {
        this.createOtherMenu(player, 3100,
                "|7|══ QUÀ THẦN ĐIỆN ══\n\n"
                        + "|5|Chào " + player.name + "!\n"
                        + "|1|Thần Điện là nơi linh thiêng, ban tặng\n"
                        + "các phần thưởng đặc biệt cho chiến binh.\n\n"
                        + "|5|🎯 Cách nhận quà:\n"
                        + "|1|• Quay Ngọc May Mắn (thỏi vàng/ngọc)\n"
                        + "• Tháp PôPô (clear tầng)\n"
                        + "• Luyện tập với Thượng Đế\n"
                        + "• Destron Gas (tại Mr.PoPo)",
                "Cải Trang\nHấp Dẫn", "Vật Phẩm\nĐặc Biệt", "Hướng Dẫn\nChi Tiết", "Đóng");
    }

    private void showGiftCostumes(Player player) {
        this.createOtherMenu(player, 3101,
                "|7|══ CẢI TRANG HẤP DẪN ══\n\n"
                        + "|5|🌟 Nguồn: Quay Ngọc May Mắn\n"
                        + "|1|• CT Goku SSJ4 - SĐ+30%, HP+30%, KI+30%\n"
                        + "• CT Cađíc SSJ Blue - SĐ+30%, HP+30%, KI+50%\n"
                        + "• CT Gogeta - SĐ+25%, HP+25%, KI+25%\n"
                        + "• CT Broly SSJ God - SĐ+30%, HP+30%\n\n"
                        + "|5|💎 Nguồn: Shop Santa (Ngọc)\n"
                        + "|1|• CT Chi Chi - SĐ+25%, HP+25%, KI+50%\n"
                        + "• CT Bunma Rider - SĐ+25%, HP+25%, KI+50%\n"
                        + "• CT Android 21 Evil - SĐ+10%, HP+10%, KI+10%\n\n"
                        + "|5|🌟 Nguồn: Điều ước Rồng Thần 1 Sao\n"
                        + "|1|• Cải trang VIP VĨNH VIỄN\n"
                        + "   SĐ+23%, HP+20%, KI+20%, Giáp+15%, CM+10%\n"
                        + "   + Thay chiêu 2-3 đệ tử",
                "Quay lại");
    }

    private void showGiftItems(Player player) {
        this.createOtherMenu(player, 3102,
                "|7|══ VẬT PHẨM ĐẶC BIỆT ══\n\n"
                        + "|5|💫 Từ Quay Ngọc May Mắn:\n"
                        + "|1|• Thỏi vàng (bán được vàng)\n"
                        + "• Capsule thời trang 5-7 ngày\n"
                        + "• Sách tiến hóa Lv1-5\n"
                        + "• Đá xanh lam nâng cấp\n\n"
                        + "|5|✨ Từ Tháp PôPô:\n"
                        + "|1|• Vàng + Ngọc theo tầng\n"
                        + "• Trang bị mạnh (tầng cao)\n"
                        + "• Đồ hiếm (tầng 50+)\n\n"
                        + "|5|👑 Từ Rồng Thần 1 Sao:\n"
                        + "|1|• 100K ngọc xanh + 100 thỏi vàng\n"
                        + "• +2 Tỷ sức mạnh & tiềm năng\n"
                        + "• Găng tay lên cấp / Chí mạng +2%",
                "Quay lại");
    }

    private void showTempleGuide(Player player) {
        this.createOtherMenu(player, 3103,
                "|7|══ HƯỚNG DẪN THẦN ĐIỆN ══\n\n"
                        + "|5|BƯỚC 1: Luyện tập\n"
                        + "|1|• Đánh Mr.PoPo → thắng = học Thượng Đế\n"
                        + "• Thắng Thượng Đế → mở Kaio\n"
                        + "• Đăng ký tập tự động = offline lên SM\n\n"
                        + "|5|BƯỚC 2: Tháp PôPô\n"
                        + "|1|• Clear tầng = nhận quà giá trị\n"
                        + "• Càng lên cao càng nhiều quà\n\n"
                        + "|5|BƯỚC 3: Quay Ngọc May Mắn\n"
                        + "|1|• Dùng thỏi vàng hoặc ngọc để quay\n"
                        + "• Trúng cải trang, vật phẩm hiếm\n\n"
                        + "|5|BƯỚC 4: Destron Gas (Mr.PoPo)\n"
                        + "|1|• Cần bang hội để tham gia\n"
                        + "• Đánh boss = điểm xếp hạng bang",
                "Quay lại");
    }
}
