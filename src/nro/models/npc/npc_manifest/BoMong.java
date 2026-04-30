package nro.models.npc.npc_manifest;

/**
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import consts.ConstNpc;
import consts.ConstTask;
import consts.ConstTaskBadges;
import item.Item;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jdbc.daos.PlayerDAO;
import models.Achievement.AchievementService;
import models.Transaction;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.server.ChuyenKhoanManager;
import nro.server.ServerManager;
import nro.services.*;
import services.func.Input;
import shop.ShopService;
import task.Badges.BadgesTaskService;
import utils.TimeUtil;
import utils.Util;

public class BoMong extends Npc {

    // Bảng nạp vàng (VNĐ -> Thỏi vàng)
    private static final int[][] NAP_VANG = {
        {10000, 100}, {20000, 200}, {50000, 500}, {100000, 1000},
        {200000, 2000}, {500000, 5000}, {1000000, 10000}
    };

    // Bảng nạp ngọc (VNĐ -> Hồng ngọc)
    private static final int[][] NAP_NGOC = {
        {20000, 2000}, {50000, 5000}, {100000, 10000}, {500000, 50000},
        {1000000, 100000}, {2000000, 200000}, {5000000, 500000}
    };

    public BoMong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (this.mapId == 47 || this.mapId == 84) {
                    String cash = Util.mumberToLouis(player.getSession().cash);
                    String goldBar = Util.mumberToLouis(player.getSession().goldBar);

                    String info = "|7|━━━ BÒ MỘNG - TRUNG TÂM ━━━\n"
                            + "|1|Số dư: " + cash + " VNĐ\n"
                            + "|8|Thỏi vàng (giữ hộ): " + goldBar + "\n"
                            + "|2|Chào chiến binh! Ta có thể giúp gì cho ngươi?\n"
                            + "|7|━━━━━━━━━━━━━━━━━━";

                    this.createOtherMenu(player, ConstNpc.BASE_MENU, info,
                            "Nạp Tiền\n& Đổi VNĐ",
                            "Nhiệm vụ\nhàng ngày",
                            "Nhiệm vụ\nthành tích",
                            "Danh\nHiệu",
                            "GiftCode");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 47 || this.mapId == 84) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.BASE_MENU ->
                        handleBaseMenu(player, select);
                    case ConstNpc.MENU_BO_MONG_NAP_TIEN ->
                        handleNapTienMenu(player, select);
                    case ConstNpc.MENU_BO_MONG_CHUYEN_KHOAN ->
                        handleChuyenKhoan(player, select);
                    case ConstNpc.MENU_BO_MONG_CHUYEN_KHOAN_QR ->
                        handleChuyenKhoanQR(player, select);
                    case ConstNpc.MENU_BO_MONG_NAP_VANG ->
                        handleNapVang(player, select);
                    case ConstNpc.MENU_BO_MONG_NAP_NGOC ->
                        handleNapNgoc(player, select);
                    case ConstNpc.MENU_BO_MONG_DANH_HIEU ->
                        handleDanhHieuMenu(player, select);
                    case ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK -> {
                        switch (select) {
                            case 0, 1, 2, 3, 4 ->
                                TaskService.gI().changeSideTask(player, (byte) select);
                        }
                    }
                    case ConstNpc.MENU_OPTION_PAY_SIDE_TASK -> {
                        switch (select) {
                            case 0 ->
                                TaskService.gI().paySideTask(player);
                            case 1 ->
                                TaskService.gI().removeSideTask(player);
                        }
                    }
                    case ConstNpc.MENU_BO_MONG -> {
                        switch (select) {
                            case 0 -> showHuongDanNap(player);
                            case 1 -> Input.gI().createFormGiftCode(player);
                        }
                    }
                }
            }
        }
    }

    // ===================== XỬ LÝ MENU GỐC =====================
    private void handleBaseMenu(Player player, int select) {
        switch (select) {
            case 0 -> // Nạp Tiền & Đổi VNĐ
                showNapTienMenu(player);
            case 1 -> { // Nhiệm vụ hàng ngày
                if (player.playerTask.sideTask.template != null) {
                    String npcSay = "|7|━━━ NHIỆM VỤ HÀNG NGÀY ━━━\n"
                            + "|1|Nhiệm vụ: " + player.playerTask.sideTask.getName() + "\n"
                            + "|8|Cấp độ: " + player.playerTask.sideTask.getLevel() + "\n"
                            + "|2|Tiến độ: " + player.playerTask.sideTask.count + "/"
                            + player.playerTask.sideTask.maxCount + " ("
                            + player.playerTask.sideTask.getPercentProcess()
                            + "%)\n"
                            + "|1|Nhiệm vụ còn lại: "
                            + player.playerTask.sideTask.leftTask + "/" + ConstTask.MAX_SIDE_TASK + "\n"
                            + "|7|━━━━━━━━━━━━━━━━━━";
                    this.createOtherMenu(player, ConstNpc.MENU_OPTION_PAY_SIDE_TASK,
                            npcSay, "Trả nhiệm\nvụ", "Hủy nhiệm\nvụ");
                } else {
                    this.createOtherMenu(player, ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK,
                            "|7|━━━ CHỌN CẤP ĐỘ NHIỆM VỤ ━━━\n"
                            + "|2|Cấp cao hơn = Phần thưởng hấp dẫn hơn!\n"
                            + "|8|Dễ: 5 Thỏi Vàng\n"
                            + "|1|Bình thường: 10 Thỏi Vàng + EXP\n"
                            + "|6|Khó: 20 Thỏi Vàng + Đá Bảo Vệ\n"
                            + "|3|Siêu khó: 30 Thỏi Vàng + Bí Kíp\n"
                            + "|5|Địa ngục: 50 Thỏi Vàng + Danh Hiệu!\n"
                            + "|7|━━━━━━━━━━━━━━━━━━",
                            "Dễ", "Bình thường", "Khó", "Siêu khó", "Địa ngục", "Từ chối");
                }
            }
            case 2 -> // Nhiệm vụ thành tích
                AchievementService.gI().openAchievementUI(player);
            case 3 -> // Danh Hiệu
                showDanhHieuMenu(player);
            case 4 -> // GiftCode
                Input.gI().createFormGiftCode(player);
        }
    }

    // ===================== DANH HIỆU =====================
    private void showDanhHieuMenu(Player player) {
        int totalBadges = player.dataBadges != null ? player.dataBadges.size() : 0;
        this.createOtherMenu(player, ConstNpc.MENU_BO_MONG_DANH_HIEU,
                "|7|━━━ DANH HIỆU ━━━\n"
                + "|1|Số danh hiệu đang sở hữu: " + totalBadges + "\n"
                + "|8|Hoàn thành nhiệm vụ thành tích, cày boss,\n"
                + "|8|hoặc nạp tiền để mở khóa Danh Hiệu!\n"
                + "|2|Mỗi Danh Hiệu tăng chỉ số đặc biệt!\n"
                + "|7|━━━━━━━━━━━━━━━━━━",
                "Shop Danh\nHiệu",
                "Danh Hiệu\nĐang Có",
                "Đóng");
    }

    private void handleDanhHieuMenu(Player player, int select) {
        switch (select) {
            case 0, 1 -> ShopService.gI().opendShop(player, "SANTA_DANH_HIEU", false);
        }
    }

    // ===================== NẠP TIỀN & ĐỔI VNĐ =====================
    private void showNapTienMenu(Player player) {
        String cash = Util.mumberToLouis(player.getSession().cash);
        String goldBar = Util.mumberToLouis(player.getSession().goldBar);

        String msg = "|7|━━━ NẠP TIỀN & ĐỔI VNĐ ━━━\n"
                + "|1|Số dư: " + cash + " VNĐ\n"
                + "|8|Thỏi vàng (giữ hộ): " + goldBar + "\n"
                + "|2|Nạp qua ATM/QR - Tự động cộng tiền!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        this.createOtherMenu(player, ConstNpc.MENU_BO_MONG_NAP_TIEN, msg,
                "Nạp VNĐ\n(ATM/QR)",
                "Đổi VNĐ\n→ Thỏi Vàng",
                "Đổi VNĐ\n→ Hồng Ngọc",
                "Nhận\nThỏi Vàng",
                "Hướng Dẫn\nNạp",
                "Đóng");
    }

    private void handleNapTienMenu(Player player, int select) {
        switch (select) {
            case 0 -> // Nạp VNĐ (ATM/QR)
                this.createOtherMenu(player, ConstNpc.MENU_BO_MONG_CHUYEN_KHOAN,
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
                    menu.add(Util.mumberToLouis(option[0]) + "\n" + Util.mumberToLouis(option[1]) + " Thỏi");
                }
                this.createOtherMenu(player, ConstNpc.MENU_BO_MONG_NAP_VANG,
                        "|7|━━━ ĐỔI VNĐ → THỎI VÀNG ━━━\n"
                        + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                        + "|8|Chọn mệnh giá muốn đổi:\n"
                        + "|7|━━━━━━━━━━━━━━━━━━",
                        menu.toArray(new String[0]));
            }
            case 2 -> { // Đổi VNĐ → Hồng Ngọc
                List<String> menu = new ArrayList<>();
                for (int[] option : NAP_NGOC) {
                    menu.add(Util.mumberToLouis(option[0]) + "\n" + Util.mumberToLouis(option[1]) + " Ngọc");
                }
                this.createOtherMenu(player, ConstNpc.MENU_BO_MONG_NAP_NGOC,
                        "|7|━━━ ĐỔI VNĐ → HỒNG NGỌC ━━━\n"
                        + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                        + "|8|Chọn mệnh giá muốn đổi:\n"
                        + "|7|━━━━━━━━━━━━━━━━━━",
                        menu.toArray(new String[0]));
            }
            case 3 -> // Nhận Thỏi Vàng
                receiveGoldBar(player);
            case 4 -> // Hướng Dẫn Nạp
                showHuongDanNap(player);
        }
    }

    // ===================== CHUYỂN KHOẢN ATM =====================
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

    private void handleChuyenKhoanQR(Player player, int select) {
        switch (select) {
            case 0 -> {
                Transaction transaction = null;

                if (player.getSession() != null && player.getSession().lastTransactionId > 0) {
                    transaction = ChuyenKhoanManager.GetTransactionById(player.id, (int) player.getSession().lastTransactionId);
                }

                if (transaction == null) {
                    transaction = ChuyenKhoanManager.GetTransactionLast(player.id);
                    if (transaction != null && (transaction.status || transaction.isReceive)) {
                        transaction = null;
                    }
                }

                if (transaction == null) {
                    Service.gI().sendThongBao(player, "Vui lòng bấm 'Tạo Giao Dịch' và nhập số tiền trước khi quét QR!");
                    return;
                }

                if (transaction.status || transaction.isReceive) {
                    Service.gI().sendThongBao(player, "Giao dịch này đã được thanh toán! Hãy tạo giao dịch mới.");
                    return;
                }

                if (player.getSession() != null) {
                    player.getSession().lastTransactionId = transaction.id;
                }

                String description = transaction.description;
                if (description == null || description.isBlank()) {
                    description = ChuyenKhoanManager.buildTransferDescription(player);
                }

                Service.gI().LinkService(player, 10684,
                        "Quét QR để chuyển khoản ATM\n"
                        + "Số tiền: " + Util.formatCurrency((long) transaction.amount) + " VNĐ\n"
                        + "Nội dung: " + description + "\n"
                        + "Lưu ý: chuyển đúng số tiền và đúng nội dung để tự cộng.",
                        ChuyenKhoanManager.buildVietQrUrl(transaction), "Quét QR");
            }
            case 1 -> { } // Đóng
        }
    }

    // ===================== ĐỔI VNĐ → THỎI VÀNG =====================
    private void handleNapVang(Player player, int select) {
        if (select < 0 || select >= NAP_VANG.length) return;
        if (player.getSession().cash < NAP_VANG[select][0]) {
            Service.gI().sendThongBao(player, "Không đủ số dư VNĐ");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang");
            return;
        }
        Item item = ItemService.gI().createNewItem((short) 457, NAP_VANG[select][1]);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
        PlayerDAO.subcash(player, NAP_VANG[select][0], "DOI_THOI_VANG_BM", "Menh:" + NAP_VANG[select][0] + " SL:" + NAP_VANG[select][1]);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.DAI_GIA_MOI_NHU, NAP_VANG[select][0]);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.EM_XINH_EM_DEP, NAP_VANG[select][0]);
        Service.gI().sendThongBao(player, "Bạn nhận được " + Util.mumberToLouis(NAP_VANG[select][1]) + " Thỏi vàng");
    }

    // ===================== ĐỔI VNĐ → HỒNG NGỌC =====================
    private void handleNapNgoc(Player player, int select) {
        if (select < 0 || select >= NAP_NGOC.length) return;
        if (player.getSession().cash < NAP_NGOC[select][0]) {
            Service.gI().sendThongBao(player, "Không đủ số dư VNĐ");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang");
            return;
        }
        Item item = ItemService.gI().createNewItem((short) 861, NAP_NGOC[select][1]);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
        PlayerDAO.subcash(player, NAP_NGOC[select][0], "DOI_HONG_NGOC_BM", "Menh:" + NAP_NGOC[select][0] + " SL:" + NAP_NGOC[select][1]);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.BI_MOC_SACH_TUI, NAP_NGOC[select][0]);
        Service.gI().sendThongBao(player, "Bạn nhận được " + Util.mumberToLouis(NAP_NGOC[select][1]) + " Hồng Ngọc");
    }

    // ===================== NHẬN THỎI VÀNG GIỮ HỘ =====================
    private void receiveGoldBar(Player player) {
        if (player.getSession().goldBar <= 0) {
            Service.gI().sendThongBao(player, "Bạn không có Thỏi Vàng nào đang được giữ hộ.");
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

    // ===================== HƯỚNG DẪN NẠP =====================
    private void showHuongDanNap(Player player) {
        Service.gI().sendThongBaoFromAdmin(player,
                "━━━ HƯỚNG DẪN NẠP TIỀN ━━━\n\n"
                + "▸ BƯỚC 1: Bấm [Nạp VNĐ (ATM/QR)]\n"
                + "▸ BƯỚC 2: Bấm [Tạo Giao Dịch]\n"
                + "▸ BƯỚC 3: Nhập số tiền muốn nạp\n"
                + "▸ BƯỚC 4: Quét mã QR hoặc chuyển khoản\n"
                + "▸ BƯỚC 5: Đợi 1-3 phút để hệ thống xử lý\n\n"
                + "━━━ BẢNG QUY ĐỔI VNĐ → THỎI VÀNG ━━━\n"
                + "10.000đ → 1 Thỏi Vàng\n"
                + "20.000đ → 2 Thỏi Vàng\n"
                + "50.000đ → 5 Thỏi Vàng\n"
                + "100.000đ → 10 Thỏi Vàng\n"
                + "200.000đ → 20 Thỏi Vàng\n"
                + "500.000đ → 50 Thỏi Vàng\n"
                + "1.000.000đ → 100 Thỏi Vàng\n\n"
                + "━━━ BẢNG QUY ĐỔI VNĐ → HỒNG NGỌC ━━━\n"
                + "20.000đ → 2.000 Hồng Ngọc\n"
                + "50.000đ → 5.000 Hồng Ngọc\n"
                + "100.000đ → 10.000 Hồng Ngọc\n"
                + "500.000đ → 50.000 Hồng Ngọc\n"
                + "1.000.000đ → 100.000 Hồng Ngọc\n\n"
                + "━━━ BONUS KHI NẠP ━━━\n"
                + "20k → +2% giá trị nạp\n"
                + "50k → +5% giá trị nạp\n"
                + "100k → +7% giá trị nạp\n"
                + "200k → +10% giá trị nạp\n"
                + "500k → +15% giá trị nạp\n"
                + "1 triệu → +20% giá trị nạp\n\n"
                + "━━━━━━━━━━━━━━━━━━\n"
                + "Có vấn đề? Liên hệ Admin!");
    }
}
