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
        }
    }

    // ==========================================
    // THÔNG BÁO & SỰ KIỆN
    // ==========================================
    private void showThongBao(Player player) {
        createOtherMenu(player, MENU_THONG_BAO,
                "|7|━━ THÔNG BÁO VŨ TRỤ ━━\n\n"
                        + "|5|📢 Sự kiện đang diễn ra:\n"
                        + "|1|" + getActiveEventLine() + "\n\n"
                        + "|5|⚡ Trạng thái Server:\n"
                        + "|1|• TNSM: " + getTNSMStatus() + "\n"
                        + "• Drop Rate: " + getDropRateStatus() + "\n"
                        + "• EXP Quái: Bình thường\n\n"
                        + "|5|📋 Cập nhật mới nhất:\n"
                        + "|1|• Rebalance Shop Uron (giá mới)\n"
                        + "• Boss Giỗ Tổ Hùng Vương\n"
                        + "• Hệ thống Đậu Thần nâng cấp\n"
                        + "• NPC Hướng Dẫn Viên tại làng\n\n"
                        + "|5|💰 Tân thủ:\n"
                        + "|1|• Tặng 2 tỉ vàng + 100k ngọc\n"
                        + "• Farm SKH tại map Fide\n"
                        + "• Nâng đậu thần sớm = mạnh nhanh",
                "Quay lại");
    }

    private void handleThongBao(Player player, int select) {
        if (select == 0) openBaseMenu(player);
    }

    private String getActiveEventLine() {
        StringBuilder sb = new StringBuilder();
        if (EventManager.HUNG_VUONG) sb.append("🏯 Giỗ Tổ Hùng Vương, ");
        if (EventManager.TRUNG_THU) sb.append("🥮 Trung Thu, ");
        if (EventManager.HALLOWEEN) sb.append("🎃 Halloween, ");
        if (EventManager.CHRISTMAS) sb.append("🎄 Giáng Sinh, ");
        if (EventManager.LUNNAR_NEW_YEAR) sb.append("🧧 Tết Nguyên Đán, ");
        if (EventManager.INTERNATIONAL_WOMANS_DAY) sb.append("💐 8/3, ");
        if (EventManager.TOP_UP) sb.append("🏆 Đua Top Nạp, ");
        if (EventManager.EVENT_POKEMON) sb.append("⚡ Pokemon, ");
        if (EventManager.TEACHERS_DAY) sb.append("📚 20/11, ");
        if (EventManager.PHO_ANH_HAI) sb.append("🍜 Phở Anh Hai, ");
        if (sb.length() == 0) return "Hiện chưa có sự kiện đặc biệt.";
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private String getTNSMStatus() {
        // Kiểm tra các event có buff TNSM
        if (EventManager.HUNG_VUONG || EventManager.CHRISTMAS || EventManager.LUNNAR_NEW_YEAR) {
            return "|7|x2 (Sự kiện đặc biệt!)";
        }
        return "x1 (Bình thường)";
    }

    private String getDropRateStatus() {
        if (EventManager.HUNG_VUONG || EventManager.CHRISTMAS) {
            return "|7|x1.5 (Sự kiện tăng drop!)";
        }
        return "x1 (Bình thường)";
    }

    // ==========================================
    // HƯỚNG DẪN GAME (tích hợp từ BangDanhVong)
    // ==========================================
    private void showHuongDan(Player player) {
        createOtherMenu(player, MENU_HUONG_DAN,
                "|7|━━ HƯỚNG DẪN GAME ━━\n\n"
                        + "|2|Chào " + player.name + "!\n"
                        + "Chọn mục muốn tìm hiểu:",
                "Lộ Trình\nTân Thủ",
                "Trang Bị\n& SKH",
                "Đậu Thần\n& Hồi Phục",
                "Boss &\nSăn Đồ",
                "Bang Hội\n& Phó Bản",
                "PVP &\nMini Game",
                "Kinh Tế\n& Nạp",
                "Quay lại");
    }

    private void handleHuongDan(Player player, int select) {
        switch (select) {
            case 0 -> showFlowGuide(player);
            case 1 -> showEquipGuide(player);
            case 2 -> showBeanGuide(player);
            case 3 -> showBossGuide(player);
            case 4 -> showClanGuide(player);
            case 5 -> showPvpGuide(player);
            case 6 -> showEconomyGuide(player);
            case 7 -> openBaseMenu(player);
        }
    }

    private void showFlowGuide(Player player) {
        createOtherMenu(player, MENU_HD_FLOW,
                "|7|━━ LỘ TRÌNH TÂN THỦ ━━\n\n"
                        + "|5|BƯỚC 1: Làm nhiệm vụ chính\n"
                        + "|1|- Nói NPC ông nội → nhận NV\n"
                        + "- Hoàn thành NV = mở map mới\n\n"
                        + "|5|BƯỚC 2: Nâng sức mạnh\n"
                        + "|1|- Thu hoạch đậu thần mỗi ngày\n"
                        + "- Dùng tiềm năng tăng HP/KI/SĐ\n"
                        + "- Mua sách skill từ Uron\n\n"
                        + "|5|BƯỚC 3: Trang bị SKH\n"
                        + "|1|- Farm quái map Fide (NV 20+)\n"
                        + "- Kích hoạt set SKH = sức mạnh x10\n\n"
                        + "|5|BƯỚC 4: Săn Boss & Endgame\n"
                        + "|1|- Săn boss nhận đồ hiếm\n"
                        + "- Bang hội, phó bản, PVP",
                "Quay lại");
    }

    private void showEquipGuide(Player player) {
        createOtherMenu(player, MENU_HD_TRANG_BI,
                "|7|━━ TRANG BỊ & SKH ━━\n\n"
                        + "|5|Các loại trang bị:\n"
                        + "|1|- Đồ thường: shop Bunma/Dende/Appule\n"
                        + "- Đồ SKH: farm từ quái map Fide\n"
                        + "- Đồ Sao: nâng cấp từ SKH\n"
                        + "- Đồ Thiên Sứ: cao cấp nhất\n\n"
                        + "|5|Cách farm SKH:\n"
                        + "|1|- Map Fide: trại lính, vực chết...\n"
                        + "- Tỉ lệ ~1/5000 (có Cỏ 4 lá ~1/3500)\n"
                        + "- Đủ 4 món cùng loại = Kích Hoạt\n\n"
                        + "|5|Nâng cấp:\n"
                        + "|1|- Bà Hạt Mít: ép đồ, nâng cấp\n"
                        + "- Lọ Sơn + Thiên Sứ + 2 SKH = VIP",
                "Quay lại");
    }

    private void showBeanGuide(Player player) {
        createOtherMenu(player, MENU_HD_BEAN,
                "|7|━━ ĐẬU THẦN & HỒI PHỤC ━━\n\n"
                        + "|5|Đậu thần:\n"
                        + "|1|- Cây đậu ở NHÀ (cạnh ông nội)\n"
                        + "- Thu hoạch → hồi HP/KI chiến đấu\n"
                        + "- Nâng cấp cây = nhiều đậu hơn\n\n"
                        + "|5|Nâng cấp đậu thần:\n"
                        + "|1|- Nói cây Đậu Thần → Nâng cấp\n"
                        + "- Cấp cao = HP/KI/SĐ bonus lớn\n"
                        + "- Chi phí: vàng + ngọc (tăng dần)\n\n"
                        + "|5|Hồi phục khác:\n"
                        + "|1|- Đùi gà: nhặt từ quái\n"
                        + "- Bùa hỗ trợ: mua Bà Hạt Mít",
                "Quay lại");
    }

    private void showBossGuide(Player player) {
        createOtherMenu(player, MENU_HD_BOSS,
                "|7|━━ BOSS & SĂN ĐỒ ━━\n\n"
                        + "|5|Boss thường:\n"
                        + "|1|- Kuku, Mập Đầu Đinh, Rambo\n"
                        + "- Số 1-4, Tiểu Đội Trưởng (Fide)\n"
                        + "- Black Goku, Golden Frieza\n\n"
                        + "|5|Boss sự kiện:\n"
                        + "|1|- Thủy Tinh, Sơn Tinh (Hùng Vương)\n"
                        + "- Xên Bọ Hung, MaBu, Android\n\n"
                        + "|5|Cách săn:\n"
                        + "|1|- Xem khung PHẢI → bấm [Đến]\n"
                        + "- Boss hồi sinh vài phút\n"
                        + "- Đi nhóm/bang hiệu quả hơn",
                "Quay lại");
    }

    private void showClanGuide(Player player) {
        createOtherMenu(player, MENU_HD_CLAN,
                "|7|━━ BANG HỘI & PHÓ BẢN ━━\n\n"
                        + "|5|Tạo/gia nhập bang:\n"
                        + "|1|- NPC Bò Mộng: tạo hoặc gia nhập\n\n"
                        + "|5|Phó bản:\n"
                        + "|1|- Trại Độc Nhãn: 2+ người\n"
                        + "- Bản Đồ Kho Báu: khám phá\n"
                        + "- Con Đường Rắn Độc: thử thách\n"
                        + "- Mỗi bản 30 phút\n\n"
                        + "|5|Hoạt động bang:\n"
                        + "|1|- NV bang: cày quái cùng nhau\n"
                        + "- Gọi Rồng Thần Namếc\n"
                        + "- Tranh Ngọc Sao Đen: 20h-21h",
                "Quay lại");
    }

    private void showPvpGuide(Player player) {
        createOtherMenu(player, MENU_HD_PVP,
                "|7|━━ PVP & MINI GAME ━━\n\n"
                        + "|5|PVP:\n"
                        + "|1|- Chạm người chơi → Thách đấu\n"
                        + "- Cả 2 đồng ý mới bắt đầu\n\n"
                        + "|5|Đại Hội Võ Thuật:\n"
                        + "|1|- Nhi đồng → Siêu cấp → Ngoại hạng\n"
                        + "- Giải: ngọc, đá nâng cấp\n\n"
                        + "|5|Tranh Ngọc Sao Đen:\n"
                        + "|1|- 20h-21h, PvP bang hội\n"
                        + "- Giữ ngọc 5 phút = thắng\n\n"
                        + "|5|Oẳn Tù Tì:\n"
                        + "|1|- Chạm người → cược 5tr vàng",
                "Quay lại");
    }

    private void showEconomyGuide(Player player) {
        createOtherMenu(player, MENU_HD_ECONOMY,
                "|7|━━ KINH TẾ & NẠP ━━\n\n"
                        + "|5|Tiền tệ:\n"
                        + "|1|- Vàng: đánh quái, bán đồ\n"
                        + "- Ngọc xanh: free, sự kiện\n"
                        + "- Hồng Ngọc: nạp VNĐ đổi\n\n"
                        + "|5|Cách nạp:\n"
                        + "|1|- NHÀ → NPC ông nội → Nạp Tiền\n"
                        + "- QR/ATM → tự cộng VNĐ\n"
                        + "- Đổi → Thỏi vàng hoặc Hồng Ngọc\n\n"
                        + "|5|Shop:\n"
                        + "|1|- Bunma/Dende/Appule: trang bị\n"
                        + "- Uron: sách kỹ năng\n"
                        + "- Santa: trang phục, capsule",
                "Quay lại");
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
