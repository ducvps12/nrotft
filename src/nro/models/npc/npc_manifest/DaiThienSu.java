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
    private static final int MENU_TOP_NV = 1118;

    private static final long WEEKLY_COOLDOWN = 7L * 24 * 60 * 60 * 1000; // 7 ngày

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
    private static final int MENU_HD_MABU = 3011;

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

    private String getWeeklyCooldownText(long lastClaim) {
        if (lastClaim <= 0) return "|5|[Chưa nhận lần nào]";
        long remaining = WEEKLY_COOLDOWN - (System.currentTimeMillis() - lastClaim);
        if (remaining <= 0) return "|5|[Có thể nhận thưởng]";
        long hours = remaining / (60 * 60 * 1000);
        long days = hours / 24;
        hours = hours % 24;
        return "|1|[Còn " + days + "d " + hours + "h]";
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player))
            return;

        switch (player.iDMark.getIndexMenu()) {

            case ConstMenu.MENU_SHOW -> {
                if (select == 0) showTopSMMenu(player);
                if (select == 1) showTopNapMenu(player);
                if (select == 2) showTopNVMenu(player);
                if (select == 3) showTopSuKienMenu(player);
                if (select == 4) showThongBao(player);
                if (select == 5) showHuongDan(player);
            }

            case 1115 -> handleTopSMOptions(player, select);
            case 1116 -> handleTopNapOptions(player, select);
            case MENU_TOP_NV -> handleTopNVOptions(player, select);
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
            case MENU_HD_MABU -> { if (select == 0) showHuongDan(player); }
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
                "====== THÔNG BÁO ======\n\n"
                        + ">> Sự kiện: " + events + "\n\n"
                        + "------------------------------\n"
                        + "* TNSM: " + tnsm + "\n"
                        + "* Drop: " + drop + "\n"
                        + "------------------------------\n\n"
                        + ">> Cập nhật mới:\n"
                        + "  - Shop Uron giá mới\n"
                        + "  - Đậu Thần nâng cấp\n"
                        + "  - NPC Hướng Dẫn tại làng\n"
                        + "  - Cân bằng kinh tế Xu NRO\n\n"
                        + ">> Tân thủ:\n"
                        + "  - Tặng 2 tỉ vàng + 100k ngọc\n"
                        + "  - Farm SKH tại map Fide\n"
                        + "  - Tháp PoPo nhận Xu NRO",
                "Quay lại");
    }

    private void handleThongBao(Player player, int select) {
        if (select == 0) openBaseMenu(player);
    }

    private String getActiveEventLine() {
        StringBuilder sb = new StringBuilder();
        if (EventManager.HUNG_VUONG) sb.append("Giỗ Tổ Hùng Vương, ");
        if (EventManager.TRUNG_THU) sb.append("Trung Thu, ");
        if (EventManager.HALLOWEEN) sb.append("Halloween, ");
        if (EventManager.CHRISTMAS) sb.append("Giáng Sinh, ");
        if (EventManager.LUNNAR_NEW_YEAR) sb.append("Tết Nguyên Đán, ");
        if (EventManager.INTERNATIONAL_WOMANS_DAY) sb.append("Ngày 8/3, ");
        if (EventManager.TOP_UP) sb.append("Đua Top Nạp, ");
        if (EventManager.EVENT_POKEMON) sb.append("Pokemon, ");
        if (EventManager.TEACHERS_DAY) sb.append("Ngày 20/11, ");
        if (EventManager.PHO_ANH_HAI) sb.append("Phố Anh Hai, ");
        if (sb.length() == 0) return "Không có sự kiện đặc biệt.";
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private String getTNSMStatus() {
        // Kiểm tra các event có buff TNSM
        if (EventManager.HUNG_VUONG || EventManager.CHRISTMAS || EventManager.LUNNAR_NEW_YEAR) {
            return "x2 (Sự kiện đặc biệt!)";
        }
        return "x1 (Bình thường)";
    }

    private String getDropRateStatus() {
        if (EventManager.HUNG_VUONG || EventManager.CHRISTMAS) {
            return "x1.5 (Sự kiện tăng drop!)";
        }
        return "x1 (Bình thường)";
    }

    // ==========================================
    // HƯỚNG DẪN GAME (tích hợp từ BangDanhVong)
    // ==========================================
    private void showHuongDan(Player player) {
        createOtherMenu(player, MENU_HUONG_DAN,
                "=== HƯỚNG DẪN GAME ===\n\n"
                        + "Chào " + player.name + "!\n"
                        + "Chọn mục muốn tìm hiểu:",
                "Lộ Trình\nTân Thủ",
                "Trang Bị\n& SKH",
                "Đậu Thần\n& Hồi Phục",
                "Boss &\nSăn Đồ",
                "Đệ Tử\n& Kilis",
                "Map MaBu\n& Kilis",
                "Bang Hội\n& Phó Bản",
                "PVP &\nKinh Tế");
    }

    private void handleHuongDan(Player player, int select) {
        switch (select) {
            case 0 -> showFlowGuide(player);
            case 1 -> showEquipGuide(player);
            case 2 -> showBeanGuide(player);
            case 3 -> showBossGuide(player);
            case 4 -> showKilisGuide(player);
            case 5 -> showMabuGuide(player);
            case 6 -> showClanGuide(player);
            case 7 -> showPvpEconomyGuide(player);
        }
    }

    private void showFlowGuide(Player player) {
        createOtherMenu(player, MENU_HD_FLOW,
                "=== LỘ TRÌNH TÂN THỦ ===\n\n"
                        + "B1: Nhiệm vụ chính\n"
                        + "  NPC ông nội -> nhận NV\n"
                        + "  Hoàn thành NV = mở map mới\n\n"
                        + "B2: Nâng sức mạnh\n"
                        + "  Thu hoạch đậu thần mỗi ngày\n"
                        + "  Dùng tiềm năng tăng HP/KI/SD\n\n"
                        + "B3: Trang bị SKH\n"
                        + "  Farm quái map Fide (NV 20+)\n"
                        + "  Kích hoạt set SKH = SM x10\n\n"
                        + "B4: Boss & Endgame\n"
                        + "  Săn boss nhận đồ hiếm\n"
                        + "  Bang hội, phó bản, PVP",
                "Quay lại");
    }

    private void showEquipGuide(Player player) {
        createOtherMenu(player, MENU_HD_TRANG_BI,
                "=== TRANG BỊ & SKH ===\n\n"
                        + "Loại trang bị:\n"
                        + "  Đồ thường: shop NPC\n"
                        + "  Đồ SKH: farm quái map Fide\n"
                        + "  Đồ Sao: nâng cấp từ SKH\n\n"
                        + "Cách farm SKH:\n"
                        + "  Map Fide: trại lính, vực chết\n"
                        + "  Tỉ lệ 1/5000 (Cỏ 4 lá 1/3500)\n"
                        + "  Đủ 4 món = Kích Hoạt\n\n"
                        + "Nâng cấp:\n"
                        + "  Bà Hạt Mít: ép đồ, nâng cấp\n"
                        + "  Lò Sơn + Thiên Sứ + 2 SKH = VIP",
                "Quay lại");
    }

    private void showBeanGuide(Player player) {
        createOtherMenu(player, MENU_HD_BEAN,
                "=== ĐẬU THẦN & HỒI PHỤC ===\n\n"
                        + "Đậu thần:\n"
                        + "  Cây đậu ở NHÀ (cạnh ông nội)\n"
                        + "  Thu hoạch -> hồi HP/KI\n"
                        + "  Nâng cấp cây = nhiều đậu hơn\n\n"
                        + "Nâng cấp đậu thần:\n"
                        + "  Nơi cây Đậu Thần -> Nâng cấp\n"
                        + "  Cấp cao = HP/KI/SD bonus lớn\n"
                        + "  Chi phí: vàng + ngọc (tăng dần)\n\n"
                        + "Hồi phục khác:\n"
                        + "  Đùi gà: nhặt từ quái\n"
                        + "  Bùa hỗ trợ: mua Bà Hạt Mít",
                "Quay lại");
    }

    private void showBossGuide(Player player) {
        createOtherMenu(player, MENU_HD_BOSS,
                "=== BOSS & SĂN ĐỒ ===\n\n"
                        + "Boss thường:\n"
                        + "  Số 1-4, Black Goku, Frieza\n"
                        + "  Tiểu Đội Trưởng (Fide)\n\n"
                        + "Boss sự kiện:\n"
                        + "  Thủy Tinh, Sơn Tinh\n"
                        + "  Xên Bọ Hung, MaBu\n\n"
                        + "Boss Rồng Nhí:\n"
                        + "  Drop Bình hút năng lượng\n"
                        + "  Dùng để đổi đệ tử Kilis\n\n"
                        + "Cách săn:\n"
                        + "  Xem khung PHẢI -> bấm [Đến]\n"
                        + "  Đi nhóm/bang hiệu quả hơn",
                "Quay lại");
    }

    private void showKilisGuide(Player player) {
        createOtherMenu(player, MENU_HD_KILIS,
                "=== ĐỆ TỬ & KILIS ===\n\n"
                        + "Kiếm Bình hút năng lượng:\n"
                        + "  Boss Rồng Nhí 1-7 sao (1-3 cái)\n"
                        + "  Boss Hirudegarn (1 cái)\n"
                        + "  NV Quỷ Lão Kame (10-20 cái)\n\n"
                        + "Farm chỉ số Kilis:\n"
                        + "  Đánh quái tại MAP CADIC\n"
                        + "  Tỉ lệ: 1/333 (có buff: 10/333)\n"
                        + "  Buff tại NPC Osin (map 187)\n"
                        + "  100 hồng ngọc = 10 phút buff\n\n"
                        + "Tiến hóa đệ tử:\n"
                        + "  3000 Kilis + đệ Mabu 40 tỉ SM\n"
                        + "  = Đệ 3K (Black Goku/Cell/Berus)\n"
                        + "  6000 Kilis + đệ 3K 100 tỉ SM\n"
                        + "  = Tuyệt Thế Đệ Tử",
                "Quay lại");
    }

    private void showClanGuide(Player player) {
        createOtherMenu(player, MENU_HD_CLAN,
                "=== BANG HỘI & PHÓ BẢN ===\n\n"
                        + "Tạo/gia nhập bang:\n"
                        + "  NPC Bò Mộng: tạo hoặc gia nhập\n\n"
                        + "Phó bản:\n"
                        + "  Trại Độc Nhãn: 2+ người\n"
                        + "  Bản Đồ Kho Báu: khám phá\n"
                        + "  Con Đường Rắn Độc: thử thách\n\n"
                        + "Hoạt động bang:\n"
                        + "  NV bang: cày quái cùng nhau\n"
                        + "  Gọi Rồng Thần Namếc\n"
                        + "  Tranh Ngọc Sao Đen: 20h-21h",
                "Quay lại");
    }

    private void showPvpEconomyGuide(Player player) {
        createOtherMenu(player, MENU_HD_PVP,
                "=== PVP & KINH TẾ ===\n\n"
                        + "PVP:\n"
                        + "  Chạm người chơi -> Thách đấu\n"
                        + "  Đại Hội Võ Thuật: giải ngọc\n"
                        + "  Tranh Ngọc Sao Đen: 20h-21h\n\n"
                        + "Tiền tệ:\n"
                        + "  Vàng: đánh quái, bán đồ\n"
                        + "  Ngọc xanh: free, sự kiện\n"
                        + "  Hồng Ngọc: nạp VNĐ đổi\n\n"
                        + "Cách nạp:\n"
                        + "  NHÀ -> NPC ông nội -> Nạp Tiền\n"
                        + "  QR/ATM -> tự cộng VNĐ\n\n"
                        + "Shop:\n"
                        + "  Uron: sách kỹ năng\n"
                        + "  Santa: trang phục, capsule",
                "Quay lại");
    }

    // ==========================================
    // HƯỚNG DẪN MAP MABU & KILIS
    // ==========================================
    private void showMabuGuide(Player player) {
        createOtherMenu(player, MENU_HD_MABU,
                "=== MAP MABU & KILIS ===\n\n"
                        + "Cách vào Map MaBu:\n"
                        + "  NPC Ôsin tại Đại Hội Võ Thuật\n"
                        + "  Mở từ 12h hàng ngày\n"
                        + "  Bấm OK để vào map\n\n"
                        + "Cơ chế phong ấn:\n"
                        + "  Mọi sức mạnh bị phong ấn\n"
                        + "  Tích 10 điểm TL = xuống tầng\n"
                        + "  Hạ quái/người chơi = tích điểm\n\n"
                        + "Hệ thống tầng:\n"
                        + "  T1: Drabura → T2: BuiBui\n"
                        + "  T3: BuiBui 2 → T4: YaCon\n"
                        + "  T5: Drabura 2 → T6: MaBu\n\n"
                        + "Phe phái:\n"
                        + "  Kaiô (Ôsin) vs Babiday\n"
                        + "  Bị thôi miên → đổi phe ngẫu nhiên\n\n"
                        + "Farm Kilis (Map 187):\n"
                        + "  Cần Bình hút năng lượng\n"
                        + "  100 hồng ngọc = 10p buff x2\n"
                        + "  Kilis dùng tiến hóa đệ tử\n\n"
                        + "MaBu 14H (map 127-128):\n"
                        + "  Phù hộ: 10 ngọc = +1tr HP/KI\n"
                        + "  Boss: MaBu, Super Bu",
                "Quay lại");
    }

    // ==========================================
    // TOP XẾP HẠNG (giữ nguyên logic cũ)
    // ==========================================
    private void showTopSMMenu(Player player) {
        String cd = getWeeklyCooldownText(player.lastClaimTopSM);
        createOtherMenu(player, 1115,
                "|8|BẢNG XẾP HẠNG SỨC MẠNH\n" + cd
                + "\n|1|Nhận thưởng: 1 lần / tuần",
                "Xem Top", "Phần Thưởng\nTop", "Phần Thưởng\nMốc", "Nhận Thưởng\nMốc", "Nhận Thưởng\nTop Tuần");
    }

    private void showTopNapMenu(Player player) {
        String cd = getWeeklyCooldownText(player.lastClaimTopNap);
        createOtherMenu(player, 1116,
                "|8|BẢNG XẾP HẠNG NẠP TIỀN\n"
                + cd + "\n"
                + "|1|Nhận thưởng: 1 lần / tuần\n"
                + "|5|Reset vào 00:00 Thứ 2 hàng tuần\n\n"
                + "|7|GHI CHÚ:\n"
                + "|5|• Top 1-3: CT VIP + Thỏi Vàng + SKH\n"
                + "|5|• Top 4-5: Thỏi Vàng + SKH\n"
                + "|5|• Top 6-10: Thỏi Vàng\n"
                + "|5|• Càng nạp nhiều, thưởng càng VIP!",
                "Xem Top", "Phần Thưởng\nTop", "Nhận Thưởng\nTop Tuần");
    }

    private void showTopNVMenu(Player player) {
        String cd = getWeeklyCooldownText(player.lastClaimTopNV);
        createOtherMenu(player, MENU_TOP_NV,
                "|8|BẢNG XẾP HẠNG NHIỆM VỤ\n" + cd
                + "\n|1|Nhận thưởng: 1 lần / tuần",
                "Xem Top", "Phần Thưởng\nTop", "Nhận Thưởng\nTop Tuần");
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
        if (select == 4)
            handleNhanThuongTopWeekly(player, "moc_suc_manh_top", "SM");
    }

    private void handleTopNVOptions(Player player, int select) {
        if (select == 0)
            TopService.showListTop(player, 0);
        if (select == 1)
            showRewardList(player, "moc_nhiem_vu_top");
        if (select == 2)
            handleNhanThuongTopWeekly(player, "moc_nhiem_vu_top", "NV");
    }

    private void handleTopNapOptions(Player player, int select) {
        if (select == 0) {
            TopService.gI().showListTopVnd(player);
        }
        if (select == 1) {
            showTopNapRewardInfo(player);
        }
        if (select == 2) {
            handleNhanThuongTopNap(player);
        }
    }

    /**
     * Hiển thị bảng phần thưởng Top Nạp chi tiết
     */
    private void showTopNapRewardInfo(Player player) {
        String info = "|8|=== PHẦN THƯỞNG TOP NẠP ===\n\n"
            + "|1|TOP 1:\n"
            + "  • x50 Thỏi Vàng\n"
            + "  • x1 Hộp SKH Thần Linh\n"
            + "  • x1 CT Himmel\n"
            + "  • x500 Hồng Ngọc\n\n"
            + "|1|TOP 2:\n"
            + "  • x40 Thỏi Vàng\n"
            + "  • x1 Hộp SKH Thần Linh\n"
            + "  • x1 CT Himmel\n"
            + "  • x300 Hồng Ngọc\n\n"
            + "|1|TOP 3:\n"
            + "  • x30 Thỏi Vàng\n"
            + "  • x1 Hộp SKH Thần Linh\n"
            + "  • x200 Hồng Ngọc\n\n"
            + "|1|TOP 4-5:\n"
            + "  • x25 Thỏi Vàng\n"
            + "  • x1 Hộp SKH Thần Linh\n"
            + "  • x100 Hồng Ngọc\n\n"
            + "|1|TOP 6-10:\n"
            + "  • x15 Thỏi Vàng\n"
            + "  • x50 Hồng Ngọc\n\n"
            + "|7|GHI CHÚ:\n"
            + "|5|• Reset vào 00:00 thứ 2 hàng tuần\n"
            + "|5|• Nhận thưởng 1 lần/tuần\n"
            + "|5|• Phần thưởng khóa giao dịch\n"
            + "|5|• Nạp càng nhiều, top càng cao!";
        Service.gI().sendThongBaoFromAdmin(player, info);
    }

    /**
     * Nhận thưởng Top Nạp hàng tuần
     */
    private void handleNhanThuongTopNap(Player player) {
        // Kiểm tra cooldown 7 ngày
        if (player.lastClaimTopNap > 0 && (System.currentTimeMillis() - player.lastClaimTopNap) < WEEKLY_COOLDOWN) {
            long remaining = WEEKLY_COOLDOWN - (System.currentTimeMillis() - player.lastClaimTopNap);
            long hours = remaining / (60 * 60 * 1000);
            long days = hours / 24;
            hours = hours % 24;
            Service.gI().sendThongBao(player, "Ban da nhan thuong Top Nap tuan nay roi!\nCon " + days + " ngay " + hours + " gio nua.");
            return;
        }

        int rank = getPlayerNapRank(player);
        if (rank <= 0 || rank > 10) {
            Service.gI().sendThongBao(player, "Ban khong nam trong Top 10 Nap!\nNap them de leo hang nha!");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 3) {
            Service.gI().sendThongBao(player, "Can it nhat 3 o trong hanh trang!");
            return;
        }

        int thoiVang = 0, ruby = 0;
        boolean hasSKH = false, hasCT = false;

        if (rank == 1) {
            thoiVang = 50; ruby = 500; hasSKH = true; hasCT = true;
        } else if (rank == 2) {
            thoiVang = 40; ruby = 300; hasSKH = true; hasCT = true;
        } else if (rank == 3) {
            thoiVang = 30; ruby = 200; hasSKH = true;
        } else if (rank <= 5) {
            thoiVang = 25; ruby = 100; hasSKH = true;
        } else {
            thoiVang = 15; ruby = 50;
        }

        Item tv = ItemService.gI().createNewItem((short) 457, thoiVang);
        tv.itemOptions.add(new Item.ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, tv);

        player.inventory.ruby += ruby;

        if (hasSKH) {
            Item skh = ItemService.gI().createNewItem((short) 1857, 1);
            skh.itemOptions.add(new Item.ItemOption(30, 1));
            InventoryService.gI().addItemBag(player, skh);
        }

        if (hasCT) {
            Item ct = ItemService.gI().createNewItem((short) 1879, 1);
            ct.itemOptions.add(new Item.ItemOption(30, 1));
            InventoryService.gI().addItemBag(player, ct);
        }

        player.lastClaimTopNap = System.currentTimeMillis();

        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().sendMoney(player);

        StringBuilder sb = new StringBuilder("Phan thuong Top " + rank + " Nap:\n");
        sb.append("• x").append(thoiVang).append(" Thoi Vang (khoa)\n");
        sb.append("• +").append(ruby).append(" Hong Ngoc\n");
        if (hasSKH) sb.append("• x1 Hop SKH Than Linh\n");
        if (hasCT) sb.append("• x1 CT Himmel\n");
        Service.gI().sendThongBaoFromAdmin(player, sb.toString());
    }

    /**
     * Tìm rank player trong Top Nạp (theo cash)
     */
    private int getPlayerNapRank(Player player) {
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(ConstSQL.TOP_NAP)) {
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next() && rank <= 10) {
                    if (rs.getString("name").equals(player.name)) return rank;
                    rank++;
                }
            }
        } catch (SQLException e) { logError(e); }
        return -1;
    }

    // ==========================================
    // NHẬN THƯỞNG TOP TUẦN (SM / NV)
    // ==========================================
    private void handleNhanThuongTopWeekly(Player player, String table, String type) {
        // Kiểm tra cooldown 7 ngày
        long lastClaim = type.equals("SM") ? player.lastClaimTopSM : player.lastClaimTopNV;
        if (lastClaim > 0 && (System.currentTimeMillis() - lastClaim) < WEEKLY_COOLDOWN) {
            long remaining = WEEKLY_COOLDOWN - (System.currentTimeMillis() - lastClaim);
            long hours = remaining / (60 * 60 * 1000);
            long days = hours / 24;
            hours = hours % 24;
            Service.gI().sendThongBao(player, "Bạn đã nhận thưởng Top " + type + " tuần này rồi!\nCòn " + days + " ngày " + hours + " giờ nữa.");
            return;
        }

        // Tìm rank player
        int rank = type.equals("SM") ? getPlayerPowerRank(player) : getPlayerTaskRank(player);
        if (rank <= 0 || rank > 10) {
            Service.gI().sendThongBao(player, "Bạn không nằm trong Top 10 " + type + "!");
            return;
        }

        // Phát thưởng từ DB
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement("SELECT detail FROM " + table + " WHERE id = ?")) {
            ps.setInt(1, rank);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String detail = rs.getString("detail");
                    JSONArray arr = (JSONArray) JSONValue.parse(detail);
                    if (arr == null || arr.isEmpty()) {
                        Service.gI().sendThongBao(player, "Top " + rank + " chưa có phần thưởng!");
                        return;
                    }
                    if (InventoryService.gI().getCountEmptyBag(player) < arr.size()) {
                        Service.gI().sendThongBao(player, "Hành trang không đủ chỗ!\nCần " + arr.size() + " ô trống.");
                        return;
                    }
                    StringBuilder sb = new StringBuilder("Phần thưởng Top " + rank + " " + type + ":\n");
                    for (Object o : arr) {
                        JSONObject obj = (JSONObject) JSONValue.parse(o.toString());
                        int tempId = Integer.parseInt(String.valueOf(obj.get("temp_id")));
                        int quantity = Integer.parseInt(String.valueOf(obj.get("quantity")));
                        Item item = ItemService.gI().createNewItem((short) tempId, quantity);
                        InventoryService.gI().addItemBag(player, item);
                        sb.append("- x").append(quantity).append(" ")
                          .append(ItemService.gI().getTemplate(tempId).name).append("\n");
                    }
                    // Cập nhật timestamp
                    if (type.equals("SM")) {
                        player.lastClaimTopSM = System.currentTimeMillis();
                    } else {
                        player.lastClaimTopNV = System.currentTimeMillis();
                    }
                    InventoryService.gI().sendItemBag(player);
                    PlayerService.gI().sendInfoHpMpMoney(player);
                    Service.gI().sendThongBaoFromAdmin(player, sb.toString());
                } else {
                    Service.gI().sendThongBao(player, "Không tìm thấy phần thưởng cho Top " + rank + "!");
                }
            }
        } catch (SQLException e) {
            logError(e);
            Service.gI().sendThongBao(player, "Lỗi hệ thống, thử lại sau!");
        }
    }

    private int getPlayerPowerRank(Player player) {
        Top.TopPowerManager.getInstance().load();
        java.util.List<Player> list = Top.TopPowerManager.getInstance().getList();
        list.sort((p1, p2) -> Long.compare(p2.nPoint.power, p1.nPoint.power));
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            if (list.get(i).name.equals(player.name)) return i + 1;
        }
        return -1;
    }

    private int getPlayerTaskRank(Player player) {
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(consts.ConstSQL.TOP_NV)) {
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next() && rank <= 10) {
                    if (rs.getString("name").equals(player.name)) return rank;
                    rank++;
                }
            }
        } catch (SQLException e) { logError(e); }
        return -1;
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
