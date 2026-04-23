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

    private static final int[] MOC_5 = { 500, 800, 1500, 3000, 5000 };

    public DaiThienSu(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        String info = """
                |8|ĐẠI THIÊN SỨ
                |7|BẢNG XẾP HẠNG VŨ TRỤ
                |5|Giải thưởng hấp dẫn
                """;

        createOtherMenu(player, ConstMenu.MENU_SHOW, info,
                "Top\nSức mạnh",
                "Top\nĐại gia",
                "Top\nNhiệm vụ",
                "Top\nSự Kiện");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player))
            return;

        switch (player.iDMark.getIndexMenu()) {

            case ConstMenu.MENU_SHOW -> {
                if (select == 0)
                    showTopSMMenu(player);
                if (select == 1)
                    showTopNapMenu(player);
                if (select == 2)
                    TopService.showListTop(player, 0);
                if (select == 3)
                    showTopSuKienMenu(player);
                if (select == 4)
                    createOtherMenu(player, MENU_TOP_CHI_SO,
                            "|8|BẢNG XẾP HẠNG CHỈ SỐ",
                            "Top HP", "Top KI", "Top SĐ");
                if (select == 5)
                    createOtherMenu(player, MENU_MOC_THUONG,
                            "|8|MỐC THƯỞNG",
                            "Kill Quái",
                            "Tiêu Hồng Ngọc",
                            "Thỏi Vàng",
                            "Tiêu VND");
            }

            case 1115 -> handleTopSMOptions(player, select);
            case 1116 -> handleTopNapOptions(player, select);
            case MENU_TOP_SU_KIEN -> handleTopSuKienOptions(player, select);
        }
    }

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
            // Xem Top
            TopService.gI().showListTopSuKien(player);
        }
        if (select == 1) {
            // Phần Thưởng Top
            showRewardList(player, "moc_su_kien_top");
        }
        if (select == 2) {
            // Nhận Thưởng
            handleNhanThuongSuKien(player);
        }
    }

    private void handleNhanThuongSuKien(Player player) {
        if (!EventManager.EVENT_RANKING_REWARD) {
            Service.gI().sendThongBao(player, "Chưa đến thời gian phát thưởng!\nBảng xếp hạng đang diễn ra.");
            return;
        }

        // Tìm thứ hạng của player trong bảng xếp hạng sự kiện
        int rank = getPlayerEventRank(player);
        if (rank <= 0 || rank > 10) {
            Service.gI().sendThongBao(player, "Bạn không nằm trong Top 10 sự kiện!\nĐiểm hiện tại: " + player.event.getDiemSuKien());
            return;
        }

        // Kiểm tra đã nhận thưởng chưa
        if (player.event.isDaNhanThuongSuKien()) {
            Service.gI().sendThongBao(player, "Bạn đã nhận thưởng sự kiện rồi!");
            return;
        }

        // Lấy phần thưởng từ bảng moc_su_kien_top theo thứ hạng
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

                    // Đánh dấu đã nhận
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
