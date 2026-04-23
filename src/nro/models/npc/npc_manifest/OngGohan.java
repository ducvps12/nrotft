package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import consts.ConstTask;
import consts.ConstTaskBadges;
import item.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdbc.DBConnecter;
import jdbc.NDVResultSet;
import jdbc.daos.PlayerDAO;
import models.Transaction;
import nro.models.npc.Npc;
import nro.player.Archivement;
import nro.player.ArchivementSanBoss;
import nro.player.Player;
import nro.server.ChuyenKhoanManager;
import nro.server.ServerManager;
import nro.services.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import services.func.Input;
import shop.ShopService;
import task.Badges.BadgesTaskService;
import utils.TimeUtil;
import utils.Util;

public class OngGohan extends Npc {

    public OngGohan(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    // Chi phí quy đổi
    private static final int COST_NAP_VANG = 1;
    private static final int COST_NAP_NGOC = 1;

    // Bảng nạp vàng (VNĐ -> Thỏi vàng)
    private static final int[][] NAP_VANG = {
        {10000, 30}, {20000, 65}, {50000, 180}, {100000, 360},
        {200000, 750}, {500000, 1500}, {1000000, 3100}
    };

    // Bảng nạp ngọc (VNĐ -> Hồng ngọc)
    private static final int[][] NAP_NGOC = {
        {20000, 20000}, {50000, 50000}, {100000, 100000}, {500000, 550000},
        {1000000, 1300000}, {2000000, 3500000}, {5000000, 10000000}
    };

    // ===================== INDEX MENU MỚI =====================
    // Menu gốc: BASE_MENU (hiển thị tổng quan + 8 nút chức năng chính)
    // Menu con cấp 1:
    private static final int MENU_NAP_TIEN = ConstNpc.NAP_TIEN;       // 503
    private static final int MENU_QUA_MIEN_PHI = 991;
    private static final int MENU_DIEM_DANH = 111;
    private static final int MENU_QUA_THANH_TICH = ConstNpc.MOC_QUA_TANG; // 507
    private static final int MENU_MOC_NAP = 1115;
    private static final int MENU_MOC_BOSS = 1116;
    private static final int MENU_XOA_DE = 12456;
    private static final int MENU_THANH_VIEN = 782;
    private static final int MENU_HOM_THU = ConstNpc.MAIL_BOX;
    private static final int MENU_KHAC = 992;

    // ===================== MENU GỐC - TỐI ƯU =====================
    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player) && !TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            String cash = Util.mumberToLouis(player.getSession().cash);
            String goldBar = Util.mumberToLouis(player.getSession().goldBar);
            boolean isActive = player.getSession().actived;
            int mailCount = player.inventory.itemsMailBox.size()
                    - InventoryService.gI().getCountEmptyListItem(player.inventory.itemsMailBox);

            // Header hiển thị thông tin ngay tại menu gốc
            String info = "|7|━━━ THÔNG TIN TÀI KHOẢN ━━━\n"
                    + "|1|Số dư: " + cash + " VNĐ\n"
                    + "|8|Thỏi vàng (giữ hộ): " + goldBar + "\n"
                    + "|2|Thành viên: " + (isActive ? "✓ Đã kích hoạt" : "✗ Chưa kích hoạt") + "\n"
                    + "|3|Hòm thư: " + mailCount + " món\n"
                    + "|7|━━━━━━━━━━━━━━━━━━━━━━━━\n"
                    + "Chọn chức năng bên dưới:";

            createOtherMenu(player, ConstNpc.BASE_MENU, info,
                    "Nạp Tiền\n& Đổi VNĐ",
                    "Quà\nMiễn Phí",
                    "Hòm Thư\n(" + mailCount + " món)",
                    "Đổi\nMật Khẩu",
                    "GiftCode",
                    "Hỗ Trợ\n& Khác");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        switch (player.iDMark.getIndexMenu()) {
            case ConstNpc.BASE_MENU ->
                handleBaseMenu(player, select);
            case MENU_NAP_TIEN ->
                handleNapTienMenu(player, select);
            case MENU_QUA_MIEN_PHI ->
                handleFreeGiftMenu(player, select);
            case MENU_DIEM_DANH ->
                handleFreeItemMenu(player, select);
            case MENU_QUA_THANH_TICH ->
                handleNhanQuaMenu(player, select);
            case MENU_MOC_NAP ->
                handleMocNap(player, select);
            case MENU_MOC_BOSS ->
                handleMocBoss(player, select);
            case MENU_XOA_DE ->
                handleDeletePet(player, select);
            case MENU_THANH_VIEN ->
                handleMemberActivation(player, select);
            case MENU_HOM_THU ->
                handleMailBox(player, select);
            case MENU_KHAC ->
                handleOtherOptionsMenu(player, select);
            case ConstNpc.CHUYEN_KHOAN ->
                handleChuyenKhoan(player, select);
            case ConstNpc.CONTENT_CHUYEN_KHOAN ->
                handleChuyenKhoanqr(player, select);
            case ConstNpc.NAP_VANG ->
                handleNapVang(player, select);
            case ConstNpc.NAP_NGOC ->
                handleNapNgoc(player, select);
        }
    }

    // ===================== XỬ LÝ MENU GỐC =====================
    private void handleBaseMenu(Player player, int select) {
        switch (select) {
            case 0 -> // Nạp Tiền & Đổi VNĐ
                showNapTienMenu(player);
            case 1 -> // Quà Miễn Phí
                showFreeGiftMenu(player);
            case 2 -> // Hòm Thư
                openMailBoxDirect(player);
            case 3 -> // Đổi Mật Khẩu
                Input.gI().createFormChangePassword(player);
            case 4 -> // GiftCode
                Input.gI().createFormGiftCode(player);
            case 5 -> // Hỗ Trợ & Khác
                showOtherMenu(player);
        }
    }

    // ===================== QUÀ MIỄN PHÍ (GỘP 1 CẤP) =====================
    private void showFreeGiftMenu(Player player) {
        int diemDanh = player.getSession().diemdanh;
        String msg = "|7|━━━ QUÀ MIỄN PHÍ ━━━\n"
                + "|1|Điểm danh hiện tại: " + diemDanh + " ngày\n"
                + "|8|Ngọc xanh: " + Util.mumberToLouis(player.inventory.gem) + "\n"
                + "|7|━━━━━━━━━━━━━━━━━━";
        createOtherMenu(player, MENU_QUA_MIEN_PHI, msg,
                "Nhận\nNgọc Xanh",
                "Điểm Danh\nHàng Ngày",
                "Nhận Quà\nĐiểm Danh",
                "Quà\nThành Tích",
                "Nhận\nĐệ Tử",
                "Đóng");
    }

    private void handleFreeGiftMenu(Player player, int select) {
        switch (select) {
            case 0 -> // Nhận Ngọc Xanh trực tiếp
                giveFreeGem(player);
            case 1 -> // Điểm Danh trực tiếp
                thucHienDiemDanh(player);
            case 2 -> // Nhận Quà Điểm Danh → mở shop
                ShopService.gI().opendShop(player, "DIEM_DANH", false);
            case 3 -> // Quà Thành Tích
                showQuaTangMenu(player);
            case 4 -> // Nhận Đệ Tử
                giveFreePet(player);
        }
    }

    private void handleFreeItemMenu(Player player, int select) {
        switch (select) {
            case 0:
                ShopService.gI().opendShop(player, "DIEM_DANH", false);
                break;
            case 1:
                thucHienDiemDanh(player);
                break;
        }
    }
    
    private void thucHienDiemDanh(Player player) {
        try {
            String checkQuery = "SELECT COUNT(*) FROM history_items_diemdanh WHERE account_id = ? AND DATE(bought_date) = CURDATE()";
            NDVResultSet resultSet = DBConnecter.executeQuery(checkQuery, player.getSession().userId);

            if (resultSet.next() && resultSet.getInt(1) > 0) {
                Service.gI().sendThongBao(player, "Hôm nay bạn đã điểm danh rồi!");
                return;
            }
            player.getSession().diemdanh++;

            String insertQuery = "INSERT INTO history_items_diemdanh (account_id, item_temp_id, bought_date) VALUES (?, 0, NOW())";
            DBConnecter.executeUpdate(insertQuery, player.getSession().userId);

            Service.gI().sendThongBao(player, "Điểm danh thành công! Tổng điểm danh: " + player.getSession().diemdanh);

        } catch (Exception e) {
            e.printStackTrace();
            Service.gI().sendThongBao(player, "Lỗi điểm danh, vui lòng báo Admin!");
        }
    }

    // ===================== HỖ TRỢ & KHÁC (GỘP) =====================
    private void showOtherMenu(Player player) {
        if (player.getSession() == null) {
            return;
        }

        boolean isActive = player.getSession().actived;

        String msg = "|7|━━━ HỖ TRỢ & TIỆN ÍCH ━━━\n"
                + "|1|Quên Mã Bảo Vệ → nhập lại\n"
                + "|8|Xóa đệ cần 10K VNĐ\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        if (isActive) {
            createOtherMenu(player, MENU_KHAC, msg,
                    "Quên Mã\nBảo Vệ",
                    "Hỗ Trợ\nNhiệm Vụ",
                    "Xóa Đệ",
                    "Đóng");
        } else {
            createOtherMenu(player, MENU_KHAC, msg,
                    "Quên Mã\nBảo Vệ",
                    "Hỗ Trợ\nNhiệm Vụ",
                    "Xóa Đệ",
                    "Mở\nThành Viên",
                    "Đóng");
        }
    }

    private void handleOtherOptionsMenu(Player player, int select) {
        if (player.getSession() == null) {
            return;
        }

        if (player.getSession().actived) {
            switch (select) {
                case 0 -> // Quên Mã Bảo Vệ
                    Input.gI().createFormMBV(player);
                case 1 -> // Hỗ Trợ Nhiệm Vụ
                    supportTask(player);
                case 2 -> // Xóa Đệ
                    createOtherMenu(player, MENU_XOA_DE, "|0|Bạn muốn xóa đệ với giá 10K VND?", "Đồng ý", "Không");
            }
        } else {
            switch (select) {
                case 0 -> // Quên Mã Bảo Vệ
                    Input.gI().createFormMBV(player);
                case 1 -> // Hỗ Trợ Nhiệm Vụ
                    supportTask(player);
                case 2 -> // Xóa Đệ
                    createOtherMenu(player, MENU_XOA_DE, "|0|Bạn muốn xóa đệ với giá 10K VND?", "Đồng ý", "Không");
                case 3 -> // Mở Thành Viên
                    showMemberMenu(player);
            }
        }
    }

    // ===================== NẠP TIỀN (HIỂN THỊ ĐẦY ĐỦ) =====================
    private void showNapTienMenu(Player player) {
        String cash = Util.mumberToLouis(player.getSession().cash);
        String goldBar = Util.mumberToLouis(player.getSession().goldBar);

        String msg = "|7|━━━ NẠP TIỀN & ĐỔI VNĐ ━━━\n"
                + "|1|Số dư: " + cash + " VNĐ\n"
                + "|8|Thỏi vàng (giữ hộ): " + goldBar + "\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, MENU_NAP_TIEN, msg,
                "Nạp VNĐ\n(ATM/QR)",
                "Đổi VNĐ\n→ Thỏi Vàng",
                "Đổi VNĐ\n→ Hồng Ngọc",
                "Nhận\nThỏi Vàng",
                "Hướng Dẫn\nBONUS",
                "Đóng");
    }
    
    // ===================== MỐC QUÀ TẶNG =====================
    private void showQuaTangMenu(Player player) {
        String cash = Util.mumberToLouis(player.getSession().cash);
        String goldBar = Util.mumberToLouis(player.getSession().goldBar);

        String msg = "|7|━━━ QUÀ THÀNH TÍCH ━━━\n"
                + "|1|Số dư: " + cash + " VNĐ\n"
                + "|8|Thỏi vàng (giữ hộ): " + goldBar + "\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, MENU_QUA_THANH_TICH, msg,
                "Mốc\nĐã Nạp",
                "Mốc\nSăn Boss",
                "Đóng");
    }
    
    private void handleNhanQuaMenu(Player player, int select) {
        switch (select) {
            case 0 -> {
                this.createOtherMenu(player, MENU_MOC_NAP,
                        "|7|━━━ MỐC NẠP ━━━\n"
                        + "|1|Nạp đạt mốc nhận quà!\n"
                        + "|8|Reset vào thứ 2 hàng tuần\n"
                        + "|7|━━━━━━━━━━━━━━━━━━",
                        "Xem Quà\nMốc Nạp",
                        "Nhận Quà\nMốc Nạp",
                        "Đóng");
            }
            case 1 -> {
                this.createOtherMenu(player, MENU_MOC_BOSS,
                        "|7|━━━ MỐC SĂN BOSS ━━━\n"
                        + "|1|Săn boss đạt mốc nhận quà!\n"
                        + "|8|Reset vào cuối tháng\n"
                        + "|7|━━━━━━━━━━━━━━━━━━",
                        "Xem Quà\nMốc Boss",
                        "Nhận Quà\nMốc Boss",
                        "Đóng");
            }
        }
    }

    private void handleNapTienMenu(Player player, int select) {
        switch (select) {
            case 0 -> // Nạp VNĐ (ATM/QR)
                createOtherMenu(player, ConstNpc.CHUYEN_KHOAN,
                    "|7|━━━ NẠP COIN ATM ━━━\n"
                    + "|1|Server: " + ServerManager.NAME + "\n"
                    + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                    + "|8|Tỉ lệ: X1 GIÁ TRỊ NẠP\n"
                    + "|2|Chọn đúng mệnh giá. Sai = không cộng\n"
                    + "|7|Đợi 1-3p để hệ thống xử lý\n"
                    + "|2|Quá 5 phút chưa nhận → liên hệ Admin\n"
                    + "|7|━━━━━━━━━━━━━━━━━━",
                    "Tạo\nGiao Dịch",
                    "Lịch Sử\nGiao Dịch",
                    "Đóng");
            case 1 -> { // Đổi VNĐ → Thỏi Vàng
                List<String> menu = new ArrayList<>();
                for (int[] option : NAP_VANG) {
                    menu.add(Util.mumberToLouis(option[0]) + "\n" + Util.mumberToLouis(option[1] * COST_NAP_VANG) + " Thỏi");
                }
                createOtherMenu(player, ConstNpc.NAP_VANG,
                        "|7|━━━ ĐỔI VNĐ → THỎI VÀNG ━━━\n"
                        + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                        + "|8|Chọn mệnh giá muốn đổi:\n"
                        + "|7|━━━━━━━━━━━━━━━━━━",
                        menu.toArray(new String[0]));
            }
            case 2 -> { // Đổi VNĐ → Hồng Ngọc
                List<String> menu = new ArrayList<>();
                for (int[] option : NAP_NGOC) {
                    menu.add(Util.mumberToLouis(option[0]) + "\n" + Util.mumberToLouis(option[1] * COST_NAP_NGOC) + " Ngọc");
                }
                createOtherMenu(player, ConstNpc.NAP_NGOC,
                        "|7|━━━ ĐỔI VNĐ → HỒNG NGỌC ━━━\n"
                        + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                        + "|8|Chọn mệnh giá muốn đổi:\n"
                        + "|7|━━━━━━━━━━━━━━━━━━",
                        menu.toArray(new String[0]));
            }
            case 3 -> // Nhận Thỏi Vàng
                receiveGoldBar(player);
            case 4 -> // Hướng Dẫn BONUS
                NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.HUONG_DAN_NAP);
        }
    }

    private void handleMocNap(Player player, int select) {
        switch (select) {
            case 0 -> {
                JSONArray dataArray;
                JSONObject dataObject;
                PreparedStatement ps = null;
                ResultSet rs = null;
                StringBuilder sb = new StringBuilder();
                sb.append("Phần thưởng mốc nạp (reset vào thứ 2 hàng tuần):\n");

                try (Connection con2 = DBConnecter.getConnectionServer()) {
                    ps = con2.prepareStatement("SELECT * FROM moc_nap");
                    rs = ps.executeQuery();

                    while (rs.next()) {
                        dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                        int mốcNạp = Archivement.GIADOLACHIADOI[rs.getInt("id") - 1];
                        String formattedMoney = NumberFormat.getInstance(new Locale("vi", "VN")).format(mốcNạp) + " VND";

                        sb.append("\n------------------------------\n");
                        sb.append("Mốc nạp ").append(formattedMoney).append(":\n");

                        for (int i = 0; i < dataArray.size(); i++) {
                            dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                            int tempId = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                            int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                            JSONArray optionsArray = (JSONArray) dataObject.get("options");

                            sb.append("- ").append(quantity).append(" x ")
                                    .append(ItemService.gI().getTemplate(tempId).name).append("\n");

                            if (optionsArray != null) {
                                for (int j = 0; j < optionsArray.size(); j++) {
                                    JSONObject optionObject = (JSONObject) optionsArray.get(j);
                                    int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                                    int param = Integer.parseInt(String.valueOf(optionObject.get("param")));

                                    String optionTemplateName = ItemService.gI().getItemOptionTemplate(optionId).name;
                                    String formattedOption = optionTemplateName.replace("#", String.valueOf(param));
                                    sb.append("  + ").append(formattedOption).append("\n");
                                }
                            }
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(QuyLaoKame.class.getName()).log(Level.SEVERE, null, ex);
                }

                Service.gI().sendThongBaoFromAdmin(player, sb.toString());
            }

            case 1 -> {
                if (player.getSession().actived) {
                    Archivement.gI().getAchievement(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn cần mở thành viên để nhận thưởng.");
                }
            }
        }
    }
    
    private void handleMocBoss(Player player, int select) {
        switch (select) {
            case 0 -> {
                JSONArray dataArray;
                JSONObject dataObject;
                PreparedStatement ps = null;
                ResultSet rs = null;
                StringBuilder sb = new StringBuilder();
                sb.append("Phần thưởng mốc Săn Boss (reset vào cuối tháng):\n");

                try (Connection con2 = DBConnecter.getConnectionServer()) {
                    ps = con2.prepareStatement("SELECT * FROM moc_san_boss");
                    rs = ps.executeQuery();

                    while (rs.next()) {
                        dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                        int mocsanboss = ArchivementSanBoss.DIEMSANBOSS[rs.getInt("id") - 1];
                        String formattedMoney = NumberFormat.getInstance(new Locale("vi", "VN")).format(mocsanboss) + " Điểm";

                        sb.append("\n------------------------------\n");
                        sb.append("MỐC SĂN BOSS ").append(formattedMoney).append(":\n");

                        for (int i = 0; i < dataArray.size(); i++) {
                            dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                            int tempId = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                            int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                            JSONArray optionsArray = (JSONArray) dataObject.get("options");

                            sb.append("- ").append(quantity).append(" x ")
                                    .append(ItemService.gI().getTemplate(tempId).name).append("\n");

                            if (optionsArray != null) {
                                for (int j = 0; j < optionsArray.size(); j++) {
                                    JSONObject optionObject = (JSONObject) optionsArray.get(j);
                                    int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                                    int param = Integer.parseInt(String.valueOf(optionObject.get("param")));

                                    String optionTemplateName = ItemService.gI().getItemOptionTemplate(optionId).name;
                                    String formattedOption = optionTemplateName.replace("#", String.valueOf(param));
                                    sb.append("  + ").append(formattedOption).append("\n");
                                }
                            }
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(QuyLaoKame.class.getName()).log(Level.SEVERE, null, ex);
                }

                Service.gI().sendThongBaoFromAdmin(player, sb.toString());
            }

            case 1 -> {
                if (player.getSession().actived) {
                    ArchivementSanBoss.gI().getMocSanBoss(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn cần mở thành viên để nhận thưởng.");
                }
            }
        }
    }

    private void handleChuyenKhoan(Player player, int select) {
        switch (select) {
            case 0 -> {
                boolean canCreate = false;
                long timeDifference = 0;
                LocalDateTime lastTimeCreate = ChuyenKhoanManager.GetLastimeCreateTransaction(player);

                if (lastTimeCreate == null) {
                    canCreate = true;
                } else {
                    LocalDateTime now = LocalDateTime.now();
                    timeDifference = TimeUtil.calculateTimeDifferenceInSeconds(lastTimeCreate, now);

                    if (timeDifference > 10) {
                        canCreate = true;
                    }
                }

                if (player.isAdmin()) {
                    canCreate = true;
                }

                if (canCreate) {
                    Input.gI().createFormChuyenKhoan(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn cần đợi " + (10 - timeDifference) + " giây nữa để được tạo giao dịch mới");
                }
            }
            case 1 ->
                ChuyenKhoanManager.ShowTransaction(player);
        }
    }

    private void handleChuyenKhoanqr(Player player, int select) {
        switch (select) {
            case 0 -> {
                Transaction transaction = ChuyenKhoanManager.GetTransactionLast(player.id);
                Service.gI().LinkService(player, 10684,
                        "Ấn quét để được tự động chuyển sang trình duyệt webiste\n"
                        + "Nếu có thắc mắc gì liên hệ admin để được giải quyết\n"
                        + "Hãy đợi admin rep nhé<3", "https://img.vietqr.io/image/MB-0000331855198-compact2.png?amount=" + transaction.amount + "&addInfo=" + transaction.description, "Quét QR");
            }
            case 1 -> Service.gI().sendThongBao(player, "Chưa có giao dịch nào!");
        }
    }

    private void handleNapVang(Player player, int select) {
        if (player.getSession().cash < NAP_VANG[select][0]) {
            Service.gI().sendThongBao(player, "Không đủ số dư");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang");
            return;
        }

        Item item = ItemService.gI().createNewItem((short) 457, NAP_VANG[select][1] * COST_NAP_VANG);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);

        PlayerDAO.subcash(player, NAP_VANG[select][0]);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.DAI_GIA_MOI_NHU, NAP_VANG[select][0]);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.EM_XINH_EM_DEP, NAP_VANG[select][0]);
        Service.gI().sendThongBao(player, "Bạn nhận được " + Util.mumberToLouis(NAP_VANG[select][1]) + " Thỏi vàng");
    }

    private void handleNapNgoc(Player player, int select) {
        if (player.getSession().cash < NAP_NGOC[select][0]) {
            Service.gI().sendThongBao(player, "Không đủ số dư");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang");
            return;
        }

        Item item = ItemService.gI().createNewItem((short) 861, NAP_NGOC[select][1] * COST_NAP_NGOC);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);

        PlayerDAO.subcash(player, NAP_NGOC[select][0]);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.BI_MOC_SACH_TUI, NAP_NGOC[select][0]);
        Service.gI().sendThongBao(player, "Bạn nhận được " + Util.mumberToLouis(NAP_NGOC[select][1]) + " Hồng Ngọc");
    }

    private void receiveGoldBar(Player player) {
        if (player.getSession().goldBar <= 0) {
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang");
            return;
        }

        Item item = ItemService.gI().createNewItem((short) 457, player.getSession().goldBar);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);

        Service.gI().sendThongBao(player, "Bạn đã nhận " + player.getSession().goldBar + " Thỏi vàng");
        PlayerDAO.subGoldBar(player, player.getSession().goldBar);
    }

    // ===================== QUÀ MIỄN PHÍ =====================
    private void giveFreeGem(Player player) {
        if (player.inventory.gem >= 5000) {
            Service.gI().sendThongBao(player, "Tiêu bớt ngọc đi rồi nhận tiếp");
            return;
        }
        player.inventory.gem += 5000;
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Bạn nhận được 5.000 Ngọc xanh");
    }

    private void giveFreePet(Player player) {
        if (!player.getSession().actived) {
            Service.gI().sendThongBao(player, "Vui lòng mở thành viên trước");
            return;
        }
        if (player.pet != null) {
            npcChat(player, "Bú ít thôi con, còn đệ rồi!");
            return;
        }
        PetService.gI().createNormalPet(player);
        Service.gI().sendThongBao(player, "Bạn nhận được đệ tử");
    }

    // ===================== HỖ TRỢ NHIỆM VỤ =====================
    private void supportTask(Player player) {
        int id = TaskService.gI().getIdTask(player);
        if ((id >= ConstTask.TASK_9_0 && id < ConstTask.TASK_11_0)
                || (id >= ConstTask.TASK_18_0 && id < ConstTask.TASK_20_0)) {

            player.playerTask.taskMain.id = (id >= ConstTask.TASK_18_0) ? 19 : 10;
            player.playerTask.taskMain.index = 0;
            TaskService.gI().sendNextTaskMain(player);

            Service.gI().sendThongBao(player, "Bạn đã được hỗ trợ nhiệm vụ thành công");
        } else {
            Service.gI().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ tàu Pảy Pảy và nhiệm vụ DHVT, Trung úy trắng");
        }
    }

    // ===================== HÒM THƯ (TRỰC TIẾP) =====================
    private void openMailBoxDirect(Player player) {
        int mailCount = player.inventory.itemsMailBox.size()
                - InventoryService.gI().getCountEmptyListItem(player.inventory.itemsMailBox);

        createOtherMenu(player, MENU_HOM_THU,
                "|7|━━━ HÒM THƯ ━━━\n"
                + "|1|Bạn có " + mailCount + " vật phẩm trong hòm thư\n"
                + "|8|Quà từ sự kiện, đua top hoặc Admin\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                "Mở\nHòm Thư",
                "Xóa Hết\nHòm Thư",
                "Đóng");
    }

    private void handleMailBox(Player player, int select) {
        switch (select) {
            case 0 ->
                ShopService.gI().opendShop(player, "ITEMS_MAIL_BOX", true);
            case 1 ->
                NpcService.gI().createMenuConMeo(player,
                        ConstNpc.CONFIRM_REMOVE_ALL_ITEM_MAIL_BOX, this.avartar,
                        "|3|Bạn có chắc muốn xóa hết vật phẩm trong hòm thư?\n|7|Sau khi xóa sẽ không thể khôi phục!",
                        "Đồng ý", "Hủy bỏ");
        }
    }

    // ===================== THÀNH VIÊN =====================
    private void showMemberMenu(Player player) {
        if (player.getSession() == null) {
            return;
        }

        // Định dạng tiền tệ
        DecimalFormat df = new DecimalFormat("#,###");
        String formattedCash = df.format(player.getSession().cash);

        createOtherMenu(player, MENU_THANH_VIEN,
                "|7|━━━ MỞ THÀNH VIÊN ━━━\n"
                + "|2|Giá: 20.000 VNĐ\n"
                + "|1|Số dư: " + formattedCash + " VNĐ\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                "Mở\nThành Viên",
                "Đóng");
    }

    private void handleMemberActivation(Player player, int select) {
        if (select != 0 || player.getSession() == null) {
            return;
        }

        if (player.getSession().actived) {
            Service.gI().sendThongBao(player, "Bạn đã mở thành viên rồi");
        } else if (player.getSession().cash < 20000) {
            NpcService.gI().createBigMessage(player, avartar,
                    "Bạn chưa nạp đủ 20K.\nBạn có muốn nạp để mở thành viên không?",
                    (byte) 1, "NẠP", "https://dragonball.online/nap-atm.php");
        } else if (PlayerDAO.updateActive(player, 1)) {
            Service.gI().sendThongBao(player, "Mở thành viên thành công!");
        } else {
            Service.gI().sendThongBao(player,
                    "Có lỗi khi kích hoạt.\n"
                    + "Nếu đã bị trừ tiền mà chưa được kích hoạt, vui lòng liên hệ Admin.\n"
                    + "Hãy chụp lại thông báo này.");
        }
    }

    // ===================== XÓA ĐỆ =====================
    private void handleDeletePet(Player player, int select) {
        if (select != 0) {
            return;
        }

        if (player.getSession().cash < 10000) {
            Service.gI().sendThongBao(player, "Xóa đệ cần 10K VNĐ");
            return;
        }
        if (player.pet != null) {
            PetService.gI().deletePet(player);
        }
    }
}
