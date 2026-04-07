package nro.models.npc.npc_manifest;

import consts.ConstDataEventCHUCVIP;
import consts.ConstDataEventNAP;
import consts.ConstDataEventTRANGSUCVIP;
import consts.ConstDataEventthangmuoi;
import consts.ConstNpc;
import event.EventManager;
import item.Item;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.server.Manager;
import nro.services.InventoryService;
import nro.services.Service;
import services.func.TopService;
import shop.ShopService;

public class ChiChi extends Npc {

    private static final int VO_SEN = 1664;
    private static final int TUI_DUNG = 1665;

    public ChiChi(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        // Menu nấu bánh khi EVENT_POKEMON BẬT
        if (EventManager.EVENT_POKEMON) {
            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Bạn muốn làm gì?",
                    "Đổi thưởng",
                    "Top Bóng Master",
                    "Top tháng 9 VIP",
                    "Top tháng 9",
                    "Cửa hàng",
                    "Đóng");
            return;
        }

        // Menu nấu bánh khi HALLOWEEN BẬT
        if (EventManager.HALLOWEEN) {
            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Bạn muốn hỏi chi ?",
                    "Top\nTúi Mù\nHalloween",
                    "Top\nHộp Kẹo\nMa Quỷ",
                    "Top\nThiệp Ma\nHalloween",
                    "Cửa hàng",
                    "Đóng");
            return;
        }

        // Menu nấu bánh khi LUNNAR_NEW_YEAR
//        if (EventManager.TRUNG_THU) {
//            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
//            createOtherMenu(player, ConstNpc.BASE_MENU,
//                    "Xin chào " + player.name + "\nTôi là nồi nấu bánh\nTôi có thể giúp gì cho bạn?",
//                    "Top\nHộp quà\nTrung Thu\nVIP", "Top\nLồng đèn\ntreo", "Đóng");
//        }
        if (EventManager.TRUNG_THU) {
            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Bạn muốn hỏi chi?",
                    "Top\nLồng đèn\ntreo", "Top\nCapsule\nTrang sức VIP", "Top\nThiệp chúc\nVIP", "Top\nHộp quà\n20/10", "Cửa hàng", "Đóng");
        }

        // Menu nấu bánh khi LUNNAR_NEW_YEAR
        if (EventManager.TEACHERS_DAY) {
            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Bạn muốn hỏi chi?",
                    "Top\nHộp trà hoa\ncúc", "Top\nHộp Kẹo Ma\nQuỷ", "Cửa hàng", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (this.mapId != 5) {
            return;
        }

        if (player.iDMark.isBaseMenu()) {
//            if (EventManager.TRUNG_THU) {
//                switch (select) {
//                    case 0 ->
//                        openMenuTop(player, ConstNpc.MENU_HOP_QUA_TRUNG_THU_VIP);
//                    case 1 ->
//                        openMenuTop(player, ConstNpc.MENU_LONG_DEN_TREO);
//                }
//            }
            if (EventManager.TRUNG_THU) {
                switch (select) {
                    case 4 ->
                        ShopService.gI().opendShop(player, "EVENT_NHA_GIAO", false);
                    case 3 ->
                        openMenuTop(player, ConstNpc.MENU_HOP_QUA_2010);
                    case 2 ->
                        openMenuTop(player, ConstNpc.MENU_THIEP_CHUC_VIP);
                    case 1 ->
                        openMenuTop(player, ConstNpc.MENU_CAPSULE_VIP);
                    case 0 ->
                        openMenuTop(player, ConstNpc.MENU_LONG_DEN_TREO);
                }
            } else if (EventManager.EVENT_POKEMON) {
                switch (select) {
                    case 0 ->
                        openMenuDoiThuong(player);
                    case 1 ->
                        openMenuTop(player, ConstNpc.MENU_TOP_BONG_MASTER);
                    case 2 ->
                        openMenuTop(player, ConstNpc.MENU_TOP_THANG_9_VIP);
                    case 3 ->
                        openMenuTop(player, ConstNpc.MENU_TOP_THANG_9);
                    case 4 ->
                        ShopService.gI().opendShop(player, "CHI_CHI_POKE", false);
                }
            } else if (EventManager.HALLOWEEN) {
                switch (select) {
                    case 0 ->
                        openMenuTop(player, ConstNpc.MENU_TOP_HALLOWEEN_MASTER);
                    case 1 ->
                        openMenuTop(player, ConstNpc.MENU_TOP_HOP_KEO_HALLOWEEN);
                    case 2 ->
                        openMenuTop(player, ConstNpc.MENU_TOP_THIEP_HALLOWEEN);
                    case 3 ->
                        ShopService.gI().opendShop(player, "CHI_CHI_HALLOWEEN", false);
                }
            } else if (EventManager.TEACHERS_DAY) {
                switch (select) {
                    case 0 ->
                        openMenuTop(player, ConstNpc.MENU_TRA_HOA_CUC);
                    case 1 ->
                        openMenuTop(player, ConstNpc.MENU_MA_QUY);
                    case 2 ->
                        ShopService.gI().opendShop(player, "EVENT_NHA_GIAO", false);
                }
            }
            return;
        } else {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.MENU_TRA_HOA_CUC ->
                    xuLyTop(player, select, "hoptrahoacuc");
                case ConstNpc.MENU_MA_QUY ->
                    xuLyTop(player, select, "hopkeomaquy");
                case ConstNpc.MENU_DOI_THUONG ->
                    thucHienDoiQua(player, select);
                case ConstNpc.MENU_TOP_BONG_MASTER ->
                    xuLyTop(player, select, "bongmaster");
                case ConstNpc.MENU_TOP_THANG_9_VIP ->
                    xuLyTop(player, select, "thang9vip");
                case ConstNpc.MENU_TOP_THANG_9 ->
                    xuLyTop(player, select, "thang9");
                case ConstNpc.MENU_HOP_QUA_TRUNG_THU_VIP ->
                    xuLyTop(player, select, "hopquatrungthuvip");
                case ConstNpc.MENU_LONG_DEN_TREO ->
                    xuLyTop(player, select, "longdentreo");
                case ConstNpc.MENU_CAPSULE_VIP ->
                    xuLyTop(player, select, "capsuvip");
                case ConstNpc.MENU_THIEP_CHUC_VIP ->
                    xuLyTop(player, select, "thiepchucvip");
                case ConstNpc.MENU_HOP_QUA_2010 ->
                    xuLyTop(player, select, "hopqua2010");
                case ConstNpc.MENU_TOP_HALLOWEEN_MASTER ->
                    xuLyTop(player, select, "halloween_master");
                case ConstNpc.MENU_TOP_HOP_KEO_HALLOWEEN ->
                    xuLyTop(player, select, "keo_halloween");
                case ConstNpc.MENU_TOP_THIEP_HALLOWEEN ->
                    xuLyTop(player, select, "thiep_halloween");
            }
        }
    }

    // ================== ĐỔI THƯỞNG ==================
    private void openMenuDoiThuong(Player player) {
        String text = """
                Chọn phần thưởng muốn đổi:
                
                - 99 Vỏ sên = Quà thường
                - 99 Vỏ sên + 2 Túi đựng = Quà VIP
                """;

        createOtherMenu(player, ConstNpc.MENU_DOI_THUONG, text,
                "Đổi quà thường", "Đổi quà VIP", "Từ chối");
    }

    private void thucHienDoiQua(Player player, int select) {
        Item voSen = InventoryService.gI().findItemBag(player, VO_SEN);

        if (select == 0) { // Quà thường
            if (voSen == null || voSen.quantity < 99) {
                Service.gI().sendThongBao(player, "Cần 99 Vỏ sên để đổi quà thường.");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(player, voSen, 99);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được Quà Thường!");
        }

        if (select == 1) { // Quà VIP
            Item tuiDung = InventoryService.gI().findItemBag(player, TUI_DUNG);
            if (voSen == null || voSen.quantity < 99 || tuiDung == null || tuiDung.quantity < 2) {
                Service.gI().sendThongBao(player, "Cần 99 Vỏ sên và 2 Túi đựng để đổi quà VIP.");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(player, voSen, 99);
            InventoryService.gI().subQuantityItemsBag(player, tuiDung, 2);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được Quà VIP!");
        }
    }

    // ================== TOP ==================
    private void openMenuTop(Player player, int type) {
        String texttrahoacuc
                = "Sự kiện đua Top Hộp trà hoa cúc nhận quà khủng\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeSuKientrahoacuc() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeSuKientrahoacucNhanGiai() + "\n"
                + "Đến gặp Chi Chi để nhận giải nhé\n"
                + "Chi tiết xem tại diễn đàn, fanpage.";
        String texthopmaquy
                = "Sự kiện đua Top Hộp trà hoa cúc nhận quà khủng\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeSuKienmaquy() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeSuKienmaquyNhanGiai() + "\n"
                + "Đến gặp Chi Chi để nhận giải nhé\n"
                + "Chi tiết xem tại diễn đàn, fanpage.";
        String textpoke = """
                Sự kiện đua TOP - phần thưởng hấp dẫn!
                
                Thời gian: Đã kết thúc
                Hạn nhận thưởng: Đã kết thúc
                
                Liên hệ Chi-Chi để nhận thưởng.
                Chi tiết xem trên diễn đàn/Fanpage.
                """;
        String texttrungthu
                = "Sự kiện đua Top Hộp quà Trung Thu VIP nhận quà khủng\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeSuKienTrungThuVip() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeSuKienTrungThuVipNhanGiai() + "\n"
                + "Đến gặp Chi Chi để nhận giải nhé\n"
                + "Chi tiết xem tại diễn đàn, fanpage.";
        String textlongdentreo
                = "Sự kiện đua Top Lồng đèn treo nhận quà khủng\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeSuKienlongdentreo() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeSuKienlongdentreoNhanGiai() + "\n"
                + "Đến gặp Chi Chi để nhận giải nhé\n"
                + "Chi tiết xem tại diễn đàn, fanpage.";
        String textcapsuvip
                = "Sự kiện đua Top Capsule Trang sức VIP nhận quà khủng\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeSuKiencapsuvip() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeSuKiencapsuvipNhanGiai() + "\n"
                + "Đến gặp Chi Chi để nhận giải nhé\n"
                + "Chi tiết xem tại diễn đàn, fanpage.";
        String textthiepchucvip
                = "Sự kiện đua Top Thiệp chúc VIP nhận quà khủng\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeSuKienthiepchucvip() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeSuKienthiepvipNhanGiai() + "\n"
                + "Đến gặp Chi Chi để nhận giải nhé\n"
                + "Chi tiết xem tại diễn đàn, fanpage.";
        String texthop2010
                = "Sự kiện đua Top Hộp quà 20/10 nhận quà khủng\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeSuKien2010() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeSuKien2010NhanGiai() + "\n"
                + "Đến gặp Chi Chi để nhận giải nhé\n"
                + "Chi tiết xem tại diễn đàn, fanpage.";
        
        String texttuimu
                = "Sự kiện đua Top Túi Mù Halloween với phần thưởng cực khủng!\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeTuiMuHalloween() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeTuiMuHalloweenNhanGiai() + "\n"
                + "Hãy đến gặp Chi Chi để nhận thưởng nhé!\n"
                + "Chi tiết xem tại fanpage & diễn đàn.";

        String textkeomaquy
                = "Sự kiện đua Top Hộp Kẹo Ma Quỷ với phần thưởng cực khủng!\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeKeoMaQuy() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeKeoMaQuyNhanGiai() + "\n"
                + "Đến gặp Chi Chi để nhận thưởng nhé!\n"
                + "Chi tiết xem tại fanpage & diễn đàn.";

        String textthiephalloween
                = "Sự kiện đua Top Thiệp Ma Halloween gửi yêu thương và nhận quà!\n"
                + "Kết thúc và trao giải sau: " + Manager.demTimeThiepHalloween() + "\n"
                + "Hạn chót nhận giải: " + Manager.demTimeThiepHalloweenNhanGiai() + "\n"
                + "Đến gặp Chi Chi để nhận thưởng nhé!\n"
                + "Chi tiết xem tại fanpage & diễn đàn.";

        switch (type) {
            case ConstNpc.MENU_TOP_HALLOWEEN_MASTER ->
                createOtherMenu(player, type, textpoke, "Top 100\nTúi Mù\nHalloween", "Xem điểm", "Đóng");
            case ConstNpc.MENU_TOP_HOP_KEO_HALLOWEEN ->
                createOtherMenu(player, type, texttuimu, "Top 100\nHộp Kẹo Ma\nQuỷ", "Xem điểm", "Đóng");
            case ConstNpc.MENU_TOP_THIEP_HALLOWEEN ->
                createOtherMenu(player, type, textkeomaquy, "Top 100\n Thiệp Ma\nHalloween", "Xem điểm", "Đóng");
            case ConstNpc.MENU_TOP_BONG_MASTER ->
                createOtherMenu(player, type, textthiephalloween, "Top 100 Bóng Master", "Xem điểm", "Đóng");
            case ConstNpc.MENU_TRA_HOA_CUC ->
                createOtherMenu(player, type, texttrahoacuc, "Top 100 Hộp trà hoa cúc", "Xem điểm", "Đóng");
            case ConstNpc.MENU_MA_QUY ->
                createOtherMenu(player, type, texthopmaquy, "Top 100 Hộp kẹo ma quỷ", "Xem điểm", "Đóng");
            case ConstNpc.MENU_TOP_THANG_9_VIP ->
                createOtherMenu(player, type, textpoke, "Top 100 Tháng 9 VIP", "Xem điểm", "Đóng");
            case ConstNpc.MENU_TOP_THANG_9 ->
                createOtherMenu(player, type, textpoke, "Top 100 Tháng 9", "Xem điểm", "Đóng");
            case ConstNpc.MENU_HOP_QUA_TRUNG_THU_VIP ->
                createOtherMenu(player, type, texttrungthu, "Top 100\nHộp quà\nTrung Thu\nVIP", "Xem điểm", "Đóng");
            case ConstNpc.MENU_LONG_DEN_TREO ->
                createOtherMenu(player, type, textlongdentreo, "Top 100\nLồng đèn\ntreo", "Xem điểm", "Đóng");
            case ConstNpc.MENU_CAPSULE_VIP -> {
                if (!ConstDataEventTRANGSUCVIP.isRunningSK) {
                    Service.gI().sendThongBao(player, "Sự kiện Top Capsule Trang sức VIP đã kết thúc.");
                    return;
                }
                createOtherMenu(player, type, textcapsuvip, "Top\nCapsule\nTrang sức VIP", "Xem điểm", "Đóng");
            }
            case ConstNpc.MENU_THIEP_CHUC_VIP -> {
                if (!ConstDataEventCHUCVIP.isRunningSK) {
                    Service.gI().sendThongBao(player, "Sự kiện Top hiệp chúc VIP đã kết thúc.");
                    return;
                }
                createOtherMenu(player, type, textthiepchucvip, "Top\nThiệp chúc\nVIP", "Xem điểm", "Đóng");
            }
            case ConstNpc.MENU_HOP_QUA_2010 -> {
                if (!ConstDataEventthangmuoi.isRunningSK) {
                    Service.gI().sendThongBao(player, "Sự kiện Top Hộp quà 20/10 đã kết thúc.");
                    return;
                }
                createOtherMenu(player, type, texthop2010, "Top\nHộp quà\n20/10", "Xem điểm", "Đóng");
            }
        }
    }

    private void xuLyTop(Player player, int select, String type) {
        if (select == 0) {
            switch (type) {
                case "capsuvip" ->
                    TopService.gI().showListTopcapsuvip(player);
                case "thiepchucvip" ->
                    TopService.gI().showListTopthiepchucvip(player);
                case "hopqua2010" ->
                    TopService.gI().showListTophopqua2010(player);
                case "bongmaster" ->
                    TopService.gI().showListTopbongmaster(player);
                case "thang9vip" ->
                    TopService.gI().showListTophopquathang9vip(player);
                case "thang9" ->
                    TopService.gI().showListTophopquathang9(player);
                case "hopquatrungthuvip" ->
                    TopService.gI().showListTophopquatrungthuvip(player);
                case "longdentreo" ->
                    TopService.gI().showListToplongdentreo(player);
                case "hoptrahoacuc" ->
                    TopService.gI().showListTophoptrahoacuc(player);
                case "hopkeomaquy" ->
                    TopService.gI().showListTophopkeomaquy(player);
                case "halloween_master" ->
                    TopService.gI().showListTopthiep_halloween(player);
                case "keo_halloween" ->
                    TopService.gI().showListTopkeo_halloween(player);
                case "thiep_halloween" ->
                    TopService.gI().showListTopthiep_halloween(player);
            }
        }

        if (select == 1) {
            int diem = 0;
            String loai = "";
            switch (type) {
                case "halloween_master" -> {
                    diem = player.getSession().halloween_master;
                    loai = "Halloween Túi Mù";
                }
                case "keo_halloween" -> {
                    diem = player.getSession().keo_halloween;
                    loai = "Hộp kẹo ma quỷ";
                }
                case "thiep_halloween" -> {
                    diem = player.getSession().thiep_halloween;
                    loai = "Thiệp Ma Halloween";
                }
                case "hoptrahoacuc" -> {
                    diem = player.getSession().hoptrahoacuc;
                    loai = "Hộp trà hoa cúc";
                }
                case "hopkeomaquy" -> {
                    diem = player.getSession().hopkeomaquy;
                    loai = "Hộp kẹo ma quỷ";
                }
                case "bongmaster" -> {
                    diem = player.getSession().bongmaster;
                    loai = "Bóng Master";
                }
                case "thang9vip" -> {
                    diem = player.getSession().hopquathang9vip;
                    loai = "Hộp quà Tháng 9 VIP";
                }
                case "thang9" -> {
                    diem = player.getSession().hopquathang9;
                    loai = "Hộp quà Tháng 9";
                }
                case "hopquatrungthuvip" -> {
                    diem = player.getSession().hopquatrungthuvip;
                    loai = "Hộp quà Trung Thu VIP";
                }
                case "longdentreo" -> {
                    diem = player.getSession().longdentreo;
                    loai = "Lồng đèn treo";
                }
                case "capsuvip" -> {
                    diem = player.getSession().capsuvip;
                    loai = "Capsule Trang sức VIP";
                }
                case "thiepchucvip" -> {
                    diem = player.getSession().thiepchucvip;
                    loai = "Thiệp chúc VIP";
                }
                case "hopqua2010" -> {
                    diem = player.getSession().hopqua2010;
                    loai = "Hộp quà 20/10";
                }
            }

            if (diem >= 10) {
                Service.gI().sendThongBaoOK(player, "Bạn đang có " + diem + " điểm " + loai + ".");
            } else {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Chưa đủ điểm vào TOP (cần ít nhất 10 điểm).", "Đóng");
            }
        }
    }
}
