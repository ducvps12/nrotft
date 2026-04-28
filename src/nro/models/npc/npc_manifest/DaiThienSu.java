package nro.models.npc.npc_manifest;

import consts.ConstMenu;
import consts.ConstSQL;
import event.EventManager;
import item.Item;
import nro.models.npc.Npc;
import nro.player.ArchivementSucManh;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.PlayerService;
import nro.services.Service;
import jdbc.DBConnecter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import services.func.TopService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DaiThienSu extends Npc {

    private static final int MENU_MOC_THUONG = 2220;
    private static final int MENU_TOP_CHI_SO = 2221;

    private static final int MENU_MOC_KILL = 2300;
    private static final int MENU_MOC_NGOC = 2301;
    private static final int MENU_MOC_VANG = 2302;
    private static final int MENU_MOC_VND = 2303;

    private static final int MENU_TOP_SU_KIEN = 1117;

    // --- MENU MỚI: Thông báo & Hướng dẫn ---
    private static final int MENU_THONG_BAO = 3001;
    private static final int MENU_HUONG_DAN = 3002;
    private static final int MENU_HD_TRANG_BI = 3003;
    private static final int MENU_HD_BOSS = 3004;
    private static final int MENU_HD_BEAN = 3005;
    private static final int MENU_HD_CLAN = 3006;
    private static final int MENU_HD_PVP = 3007;
    private static final int MENU_HD_ECONOMY = 3008;
    private static final int MENU_HD_FLOW = 3009;
    private static final int MENU_HD_KILIS = 3010;

    private static final int[] MOC_5 = { 500, 800, 1500, 3000, 5000 };

    public DaiThienSu(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        String info = "|8|ĐẠI THIÊN SỨ\n"
                + "|7|TRUNG TÂM VŨ TRỤ\n"
                + "|5|Xếp hạng • Thông báo • Hướng dẫn";

        createOtherMenu(player, ConstMenu.MENU_SHOW, info,
                "Top\nSức mạnh",
                "Top\nĐại gia",
                "Top\nNhiệm vụ",
                "Top\nSự Kiện",
                "Thông Báo\n& Sự Kiện",
                "Hướng Dẫn\nGame");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player))
            return;

        switch (player.iDMark.getIndexMenu()) {

            case ConstMenu.MENU_SHOW -> {
                if (select == 0) showTopSMMenu(player);
                if (select == 1) showTopNapMenu(player);
                if (select == 2) TopService.showListTop(player, 0);
                if (select == 3) showTopSuKienMenu(player);
                if (select == 4) showThongBao(player);
                if (select == 5) showHuongDan(player);
            }

            case 1115 -> handleTopSMOptions(player, select);
            case 1116 -> handleTopNapOptions(player, select);
            case MENU_TOP_SU_KIEN -> handleTopSuKienOptions(player, select);
            case MENU_THONG_BAO -> handleThongBao(player, select);
            case MENU_HUONG_DAN -> handleHuongDan(player, select);
            case MENU_HD_FLOW -> { if (select == 0) showHuongDan(player); }
            case MENU_HD_TRANG_BI -> { if (select == 0) showHuongDan(player); }
            case MENU_HD_BOSS -> { if (select == 0) showHuongDan(player); }
            case MENU_HD_BEAN -> { if (select == 0) showHuongDan(player); }
            case MENU_HD_CLAN -> { if (select == 0) showHuongDan(player); }
            case MENU_HD_PVP -> { if (select == 0) showHuongDan(player); }
            case MENU_HD_ECONOMY -> { if (select == 0) showHuongDan(player); }
            case MENU_HD_KILIS -> { if (select == 0) showHuongDan(player); }
        }
    }

    // ==========================================
    // THÔNG BÁO & SỰ KIỆN
    // ==========================================
    private void showThongBao(Player player) {
        String events = getActiveEventLine();
        String tnsm = getTNSMStatus();
        String drop = getDropRateStatus();

        createOtherMenu(player, MENU_THONG_BAO,
                "====== THONG BAO ======\n\n"
                        + ">> Su kien: " + events + "\n\n"
                        + "------------------------------\n"
                        + "* TNSM: " + tnsm + "\n"
                        + "* Drop: " + drop + "\n"
                        + "------------------------------\n\n"
                        + ">> Cap nhat moi:\n"
                        + "  - Shop Uron gia moi\n"
                        + "  - Dau Than nang cap\n"
                        + "  - NPC Huong Dan tai lang\n"
                        + "  - Can bang kinh te Xu NRO\n\n"
                        + ">> Tan thu:\n"
                        + "  - Tang 2 ti vang + 100k ngoc\n"
                        + "  - Farm SKH tai map Fide\n"
                        + "  - Thap PoPo nhan Xu NRO",
                "Quay lai");
    }

    private void handleThongBao(Player player, int select) {
        if (select == 0) openBaseMenu(player);
    }

    private String getActiveEventLine() {
        StringBuilder sb = new StringBuilder();
        if (EventManager.HUNG_VUONG) sb.append("Gio To Hung Vuong, ");
        if (EventManager.TRUNG_THU) sb.append("Trung Thu, ");
        if (EventManager.HALLOWEEN) sb.append("Halloween, ");
        if (EventManager.CHRISTMAS) sb.append("Giang Sinh, ");
        if (EventManager.LUNNAR_NEW_YEAR) sb.append("Tet Nguyen Dan, ");
        if (EventManager.INTERNATIONAL_WOMANS_DAY) sb.append("Ngay 8/3, ");
        if (EventManager.TOP_UP) sb.append("Dua Top Nap, ");
        if (EventManager.EVENT_POKEMON) sb.append("Pokemon, ");
        if (EventManager.TEACHERS_DAY) sb.append("Ngay 20/11, ");
        if (EventManager.PHO_ANH_HAI) sb.append("Pho Anh Hai, ");
        if (sb.length() == 0) return "Khong co su kien dac biet.";
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private String getTNSMStatus() {
        // Kiểm tra các event có buff TNSM
        if (EventManager.HUNG_VUONG || EventManager.CHRISTMAS || EventManager.LUNNAR_NEW_YEAR) {
            return "x2 (Su kien dac biet!)";
        }
        return "x1 (Binh thuong)";
    }

    private String getDropRateStatus() {
        if (EventManager.HUNG_VUONG || EventManager.CHRISTMAS) {
            return "x1.5 (Su kien tang drop!)";
        }
        return "x1 (Binh thuong)";
    }

    // ==========================================
    // HƯỚNG DẪN GAME (tích hợp từ BangDanhVong)
    // ==========================================
    private void showHuongDan(Player player) {
        createOtherMenu(player, MENU_HUONG_DAN,
                "=== HUONG DAN GAME ===\n\n"
                        + "Chao " + player.name + "!\n"
                        + "Chon muc muon tim hieu:",
                "Lo Trinh\nTan Thu",
                "Trang Bi\n& SKH",
                "Dau Than\n& Hoi Phuc",
                "Boss &\nSan Do",
                "De Tu\n& Kilis",
                "Bang Hoi\n& Pho Ban",
                "PVP &\nKinh Te",
                "Quay lai");
    }

    private void handleHuongDan(Player player, int select) {
        switch (select) {
            case 0 -> showFlowGuide(player);
            case 1 -> showEquipGuide(player);
            case 2 -> showBeanGuide(player);
            case 3 -> showBossGuide(player);
            case 4 -> showKilisGuide(player);
            case 5 -> showClanGuide(player);
            case 6 -> showPvpEconomyGuide(player);
            case 7 -> openBaseMenu(player);
        }
    }

    private void showFlowGuide(Player player) {
        createOtherMenu(player, MENU_HD_FLOW,
                "=== LO TRINH TAN THU ===\n\n"
                        + "B1: Nhiem vu chinh\n"
                        + "  NPC ong noi -> nhan NV\n"
                        + "  Hoan thanh NV = mo map moi\n\n"
                        + "B2: Nang suc manh\n"
                        + "  Thu hoach dau than moi ngay\n"
                        + "  Dung tiem nang tang HP/KI/SD\n\n"
                        + "B3: Trang bi SKH\n"
                        + "  Farm quai map Fide (NV 20+)\n"
                        + "  Kich hoat set SKH = SM x10\n\n"
                        + "B4: Boss & Endgame\n"
                        + "  San boss nhan do hiem\n"
                        + "  Bang hoi, pho ban, PVP",
                "Quay lai");
    }

    private void showEquipGuide(Player player) {
        createOtherMenu(player, MENU_HD_TRANG_BI,
                "=== TRANG BI & SKH ===\n\n"
                        + "Loai trang bi:\n"
                        + "  Do thuong: shop NPC\n"
                        + "  Do SKH: farm quai map Fide\n"
                        + "  Do Sao: nang cap tu SKH\n\n"
                        + "Cach farm SKH:\n"
                        + "  Map Fide: trai linh, vuc chet\n"
                        + "  Ti le 1/5000 (Co 4 la 1/3500)\n"
                        + "  Du 4 mon = Kich Hoat\n\n"
                        + "Nang cap:\n"
                        + "  Ba Hat Mit: ep do, nang cap\n"
                        + "  Lo Son + Thien Su + 2 SKH = VIP",
                "Quay lai");
    }

    private void showBeanGuide(Player player) {
        createOtherMenu(player, MENU_HD_BEAN,
                "=== DAU THAN & HOI PHUC ===\n\n"
                        + "Dau than:\n"
                        + "  Cay dau o NHA (canh ong noi)\n"
                        + "  Thu hoach -> hoi HP/KI\n"
                        + "  Nang cap cay = nhieu dau hon\n\n"
                        + "Nang cap dau than:\n"
                        + "  Noi cay Dau Than -> Nang cap\n"
                        + "  Cap cao = HP/KI/SD bonus lon\n"
                        + "  Chi phi: vang + ngoc (tang dan)\n\n"
                        + "Hoi phuc khac:\n"
                        + "  Dui ga: nhat tu quai\n"
                        + "  Bua ho tro: mua Ba Hat Mit",
                "Quay lai");
    }

    private void showBossGuide(Player player) {
        createOtherMenu(player, MENU_HD_BOSS,
                "=== BOSS & SAN DO ===\n\n"
                        + "Boss thuong:\n"
                        + "  So 1-4, Black Goku, Frieza\n"
                        + "  Tieu Doi Truong (Fide)\n\n"
                        + "Boss su kien:\n"
                        + "  Thuy Tinh, Son Tinh\n"
                        + "  Xen Bo Hung, MaBu\n\n"
                        + "Boss Rong Nhi:\n"
                        + "  Drop Binh hut nang luong\n"
                        + "  Dung de doi de tu Kilis\n\n"
                        + "Cach san:\n"
                        + "  Xem khung PHAI -> bam [Den]\n"
                        + "  Di nhom/bang hieu qua hon",
                "Quay lai");
    }

    private void showKilisGuide(Player player) {
        createOtherMenu(player, MENU_HD_KILIS,
                "=== DE TU & KILIS ===\n\n"
                        + "Kiem Binh hut nang luong:\n"
                        + "  Boss Rong Nhi 1-7 sao (1-3 cai)\n"
                        + "  Boss Hirudegarn (1 cai)\n"
                        + "  NV Quy Lao Kame (10-20 cai)\n\n"
                        + "Farm chi so Kilis:\n"
                        + "  Danh quai tai MAP CADIC\n"
                        + "  Ti le: 1/333 (co buff: 10/333)\n"
                        + "  Buff tai NPC Osin (map 187)\n"
                        + "  100 hong ngoc = 10 phut buff\n\n"
                        + "Tien hoa de tu:\n"
                        + "  3000 Kilis + de Mabu 40 ti SM\n"
                        + "  = De 3K (Black Goku/Cell/Berus)\n"
                        + "  6000 Kilis + de 3K 100 ti SM\n"
                        + "  = Tuyet The De Tu",
                "Quay lai");
    }

    private void showClanGuide(Player player) {
        createOtherMenu(player, MENU_HD_CLAN,
                "=== BANG HOI & PHO BAN ===\n\n"
                        + "Tao/gia nhap bang:\n"
                        + "  NPC Bo Mong: tao hoac gia nhap\n\n"
                        + "Pho ban:\n"
                        + "  Trai Doc Nhan: 2+ nguoi\n"
                        + "  Ban Do Kho Bau: kham pha\n"
                        + "  Con Duong Ran Doc: thu thach\n\n"
                        + "Hoat dong bang:\n"
                        + "  NV bang: cay quai cung nhau\n"
                        + "  Goi Rong Than Namec\n"
                        + "  Tranh Ngoc Sao Den: 20h-21h",
                "Quay lai");
    }

    private void showPvpEconomyGuide(Player player) {
        createOtherMenu(player, MENU_HD_PVP,
                "=== PVP & KINH TE ===\n\n"
                        + "PVP:\n"
                        + "  Cham nguoi choi -> Thach dau\n"
                        + "  Dai Hoi Vo Thuat: giai ngoc\n"
                        + "  Tranh Ngoc Sao Den: 20h-21h\n\n"
                        + "Tien te:\n"
                        + "  Vang: danh quai, ban do\n"
                        + "  Ngoc xanh: free, su kien\n"
                        + "  Hong Ngoc: nap VND doi\n\n"
                        + "Cach nap:\n"
                        + "  NHA -> NPC ong noi -> Nap Tien\n"
                        + "  QR/ATM -> tu cong VND\n\n"
                        + "Shop:\n"
                        + "  Uron: sach ky nang\n"
                        + "  Santa: trang phuc, capsule",
                "Quay lai");
    }

    // ==========================================
    // TOP XẾP HẠNG (giữ nguyên logic cũ)
    // ==========================================
    private void showTopSMMenu(Player player) {
        createOtherMenu(player, 1115,
                "|8|BẢNG XẾP HẠNG SỨC MẠNH",
                "Xem Top", "Phần Thưởng Top", "Phần Thưởng Mốc", "Nhận Thưởng");
    }

    private void showTopNapMenu(Player player) {
        createOtherMenu(player, 1116,
                "|8|BẢNG XẾP HẠNG NẠP TIỀN",
                "Xem Top", "Phần Thưởng");
    }

    private void showTopSuKienMenu(Player player) {
        String status = EventManager.EVENT_RANKING_REWARD ? "|5|[ĐANG PHÁT THƯỞNG]" : "|7|[ĐANG DIỄN RA]";
        createOtherMenu(player, MENU_TOP_SU_KIEN,
                "|8|BẢNG XẾP HẠNG SỰ KIỆN\n" + status
                + "\nĐiểm của bạn: " + player.event.getDiemSuKien(),
                "Xem Top", "Phần Thưởng", "Nhận Thưởng");
    }

    private void handleTopSMOptions(Player player, int select) {
        if (select == 0)
            TopService.gI().showListTopPower(player);
        if (select == 1)
            showRewardList(player, "moc_suc_manh_top");
        if (select == 2)
            showRewardList(player, "moc_suc_manh");
        if (select == 3 && player.getSession().actived)
            ArchivementSucManh.gI().getAchievement(player);
    }

    private void handleTopNapOptions(Player player, int select) {
        if (select == 0)
            TopService.gI().showListTopVnd(player);
        if (select == 1)
            showRewardList(player, "moc_nap_top");
    }

    private void handleTopSuKienOptions(Player player, int select) {
        if (select == 0) {
            TopService.gI().showListTopSuKien(player);
        }
        if (select == 1) {
            showRewardList(player, "moc_su_kien_top");
        }
        if (select == 2) {
            handleNhanThuongSuKien(player);
        }
    }

    private void handleNhanThuongSuKien(Player player) {
        if (!EventManager.EVENT_RANKING_REWARD) {
            Service.gI().sendThongBao(player, "Chưa đến thời gian phát thưởng!\nBảng xếp hạng đang diễn ra.");
            return;
        }

        int rank = getPlayerEventRank(player);
        if (rank <= 0 || rank > 10) {
            Service.gI().sendThongBao(player, "Bạn không nằm trong Top 10 sự kiện!\nĐiểm hiện tại: " + player.event.getDiemSuKien());
            return;
        }

        if (player.event.isDaNhanThuongSuKien()) {
            Service.gI().sendThongBao(player, "Bạn đã nhận thưởng sự kiện rồi!");
            return;
        }

        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement("SELECT detail FROM moc_su_kien_top WHERE id = ?")) {

            ps.setInt(1, rank);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String detail = rs.getString("detail");
                    JSONArray arr = (JSONArray) JSONValue.parse(detail);

                    if (arr == null || arr.isEmpty()) {
                        Service.gI().sendThongBao(player, "Top " + rank + " chưa có phần thưởng nào được cấu hình!");
                        return;
                    }

                    if (InventoryService.gI().getCountEmptyBag(player) < arr.size()) {
                        Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!\nCần ít nhất " + arr.size() + " ô trống.");
                        return;
                    }

                    StringBuilder sb = new StringBuilder("Phần thưởng Top " + rank + " Sự Kiện:\n");
                    for (Object o : arr) {
                        JSONObject obj = (JSONObject) JSONValue.parse(o.toString());
                        int tempId = Integer.parseInt(String.valueOf(obj.get("temp_id")));
                        int quantity = Integer.parseInt(String.valueOf(obj.get("quantity")));

                        Item item = ItemService.gI().createNewItem((short) tempId, quantity);
                        InventoryService.gI().addItemBag(player, item);
                        sb.append("- x").append(quantity).append(" ")
                          .append(ItemService.gI().getTemplate(tempId).name).append("\n");
                    }

                    player.event.setDaNhanThuongSuKien(true);
                    InventoryService.gI().sendItemBag(player);
                    PlayerService.gI().sendInfoHpMpMoney(player);
                    Service.gI().sendThongBaoFromAdmin(player, sb.toString());
                }
            }
        } catch (SQLException e) {
            logError(e);
            Service.gI().sendThongBao(player, "Lỗi hệ thống, vui lòng thử lại!");
        }
    }

    private int getPlayerEventRank(Player player) {
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(ConstSQL.TOP_SU_KIEN)) {

            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (name.equals(player.name)) {
                        return rank;
                    }
                    rank++;
                }
            }
        } catch (SQLException e) {
            logError(e);
        }
        return -1;
    }

    private void showRewardList(Player player, String table) {
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM " + table);
                ResultSet rs = ps.executeQuery()) {

            StringBuilder sb = new StringBuilder("PHẦN THƯỞNG\n");

            while (rs.next()) {
                sb.append("\n-----------------\n");
                appendItemList(sb, rs.getString("detail"));
            }

            Service.gI().sendThongBaoFromAdmin(player, sb.toString());

        } catch (SQLException e) {
            logError(e);
        }
    }

    private void appendItemList(StringBuilder sb, String json) {
        JSONArray arr = (JSONArray) JSONValue.parse(json);
        int i = 1;

        for (Object o : arr) {
            JSONObject obj = (JSONObject) JSONValue.parse(o.toString());
            int tempId = Integer.parseInt(String.valueOf(obj.get("temp_id")));
            int quantity = Integer.parseInt(String.valueOf(obj.get("quantity")));
            sb.append(i++).append(". x").append(quantity)
                    .append(" ").append(ItemService.gI().getTemplate(tempId).name).append("\n");
        }
    }

    private void logError(Exception e) {
        Logger.getLogger(DaiThienSu.class.getName()).log(Level.SEVERE, null, e);
    }
}
