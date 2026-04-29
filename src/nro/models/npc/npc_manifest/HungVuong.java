package nro.models.npc.npc_manifest;

import consts.ConstItem;
import consts.ConstNpc;
import event.EventManager;
import item.Item;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.PlayerService;
import nro.services.Service;

/**
 * NPC Hùng Vương - Sự kiện Giỗ Tổ Hùng Vương
 * Xuất hiện tại Đảo Kame (map 5)
 * Chức năng: Quy đổi vật phẩm sự kiện, xem BXH, chúc phúc
 */
public class HungVuong extends Npc {

    // ========== MENU INDEX ==========
    private static final int MENU_MAIN = 4001;
    private static final int MENU_QUY_DOI = 4002;
    private static final int MENU_DOI_LINH_THU = 4005;
    private static final int MENU_TOP_SON_TINH_THUY_TINH = 4007;
    private static final int MENU_TOP_DUA_HAU = 4008;
    private static final int MENU_CONFIRM_DOI_LE_VAT = 4009;
    private static final int MENU_CONFIRM_DOI_DUA_HAU = 4010;
    private static final int MENU_CONFIRM_DOI_LINH_THU = 4011;
    private static final int MENU_DOI_CAI_TRANG = 4012;
    private static final int MENU_CONFIRM_DOI_CT_SON_TINH = 4013;
    private static final int MENU_CONFIRM_DOI_CT_THUY_TINH = 4014;
    private static final int MENU_CONFIRM_DOI_CT_MI_NUONG = 4015;
    private static final int MENU_CHUC_PHUC = 4006;
    private static final int MENU_CONFIRM_DOI_MANH_DB = 4016;
    private static final int MENU_CONFIRM_DOI_MANH_CT = 4017;

    public HungVuong(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) return;

        if (!EventManager.HUNG_VUONG) {
            createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "|7|SỰ KIỆN GIỖ TỔ HÙNG VƯƠNG\n"
                    + "|1|Sự kiện chưa diễn ra!\n"
                    + "Hãy chờ đợi nhé chiến binh.",
                    "Đóng");
            return;
        }

        String info = "|7|⚜ HÙNG VƯƠNG ⚜\n"
                + "|5|Sự kiện Giỗ Tổ Hùng Vương\n"
                + "|0|Chào " + player.name + "!\n"
                + "Hãy mang lễ vật tới đây\n"
                + "để nhận quà đặc biệt!";

        createOtherMenu(player, MENU_MAIN, info,
                "Quy Đổi\nVật Phẩm",
                "Đổi Cải\nTrang VIP",
                "Đổi Linh\nThú Huyền",
                "Top\nDưa Hấu",
                "Top\nSơn Tinh\nThuỷ Tinh",
                "Chúc Phúc\n+50 Điểm");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        switch (player.iDMark.getIndexMenu()) {
            case MENU_MAIN -> handleMainMenu(player, select);
            case MENU_QUY_DOI -> handleQuyDoiMenu(player, select);
            case MENU_DOI_CAI_TRANG -> handleDoiCaiTrangMenu(player, select);
            case MENU_DOI_LINH_THU -> handleDoiLinhThuMenu(player, select);
            case MENU_CONFIRM_DOI_LE_VAT -> { if (select == 0) doDoiLeVat(player); }
            case MENU_CONFIRM_DOI_DUA_HAU -> { if (select == 0) doDoiDuaHau(player); }
            case MENU_CONFIRM_DOI_CT_SON_TINH -> { if (select == 0) doDoiCaiTrang(player, ConstItem.CAI_TRANG_SON_TINH, "Sơn Tinh", ConstItem.NGA_VOI, 50); }
            case MENU_CONFIRM_DOI_CT_THUY_TINH -> { if (select == 0) doDoiCaiTrang(player, ConstItem.CAI_TRANG_THUY_TINH, "Thuỷ Tinh", ConstItem.CUA_GA, 50); }
            case MENU_CONFIRM_DOI_CT_MI_NUONG -> { if (select == 0) doDoiCaiTrang(player, ConstItem.CAI_TRANG_MI_NUONG, "Mị Nương", ConstItem.HONG_MAO, 50); }
            case MENU_CONFIRM_DOI_MANH_DB -> { if (select == 0) doDoiManhDinhBa(player); }
            case MENU_CONFIRM_DOI_MANH_CT -> { if (select == 0) doDoiManhCungTen(player); }
            case MENU_CONFIRM_DOI_LINH_THU -> handleConfirmLinhThu(player, select);
            case MENU_CHUC_PHUC -> { if (select == 0) openBaseMenu(player); }
            case MENU_TOP_DUA_HAU -> { if (select == 0) openBaseMenu(player); }
            case MENU_TOP_SON_TINH_THUY_TINH -> { if (select == 0) openBaseMenu(player); }
        }
    }

    // ==========================================
    // MAIN MENU ROUTER
    // ==========================================
    private void handleMainMenu(Player player, int select) {
        switch (select) {
            case 0 -> showQuyDoiMenu(player);
            case 1 -> showDoiCaiTrangMenu(player);
            case 2 -> showDoiLinhThuMenu(player);
            case 3 -> showTopDuaHau(player);
            case 4 -> showTopSonTinhThuyTinh(player);
            case 5 -> doChucPhuc(player);
        }
    }

    // ==========================================
    // QUY ĐỔI VẬT PHẨM
    // ==========================================
    private void showQuyDoiMenu(Player player) {
        int ngaVoi = getItemQuantity(player, ConstItem.NGA_VOI);
        int cuaGa = getItemQuantity(player, ConstItem.CUA_GA);
        int hongMao = getItemQuantity(player, ConstItem.HONG_MAO);
        int duaHau = getItemQuantity(player, ConstItem.DUA_HAU);
        int manhDB = getItemQuantity(player, ConstItem.MANH_DINH_BA);
        int manhCT = getItemQuantity(player, ConstItem.MANH_CUNG_TEN);

        createOtherMenu(player, MENU_QUY_DOI,
                "|7|⚜ QUY ĐỔI VẬT PHẨM ⚜\n\n"
                + "|0|Vật phẩm đang có:\n"
                + "|5|• Ngà Voi: " + ngaVoi + "\n"
                + "|5|• Cựa Gà: " + cuaGa + "\n"
                + "|5|• Hồng Mao: " + hongMao + "\n"
                + "|5|• Dưa Hấu: " + duaHau + "\n"
                + "|5|• Mảnh Đinh Ba: " + manhDB + "\n"
                + "|5|• Mảnh Cung Tên: " + manhCT + "\n"
                + "|1|━━━━━━━━━━━━━━━━━━━━\n"
                + "|0|1. Mâm Lễ: 50 Ngà+50 Cựa+50 H.Mao\n"
                + "|0|2. 99 Dưa Hấu → +10 điểm\n"
                + "|0|3. 99 M.Đinh Ba → +15 điểm\n"
                + "|0|4. 99 M.Cung Tên → +15 điểm",
                "Đổi Mâm\nLễ Vật",
                "Đổi Dưa\nHấu",
                "Đổi Mảnh\nĐinh Ba",
                "Đổi Mảnh\nCung Tên",
                "Quay lại");
    }

    private void handleQuyDoiMenu(Player player, int select) {
        switch (select) {
            case 0 -> showConfirmDoiLeVat(player);
            case 1 -> showConfirmDoiDuaHau(player);
            case 2 -> showConfirmDoiManhDB(player);
            case 3 -> showConfirmDoiManhCT(player);
            case 4 -> openBaseMenu(player);
        }
    }

    private void showConfirmDoiLeVat(Player player) {
        createOtherMenu(player, MENU_CONFIRM_DOI_LE_VAT,
                "|7|⚜ ĐỔI MÂM LỄ VẬT ⚜\n\n"
                + "|1|Yêu cầu:\n"
                + "|0|• 50 Ngà Voi\n"
                + "|0|• 50 Cựa Gà\n"
                + "|0|• 50 Hồng Mao\n\n"
                + "|2|Nhận được:\n"
                + "|5|• 10x Thỏi Vàng\n"
                + "|5|• +20 điểm sự kiện\n\n"
                + "|7|Bạn có muốn đổi không?",
                "Đồng ý", "Từ chối");
    }

    private void doDoiLeVat(Player player) {
        Item ngaVoi = InventoryService.gI().findItemBag(player, ConstItem.NGA_VOI);
        Item cuaGa = InventoryService.gI().findItemBag(player, ConstItem.CUA_GA);
        Item hongMao = InventoryService.gI().findItemBag(player, ConstItem.HONG_MAO);

        if (ngaVoi == null || ngaVoi.quantity < 50
            || cuaGa == null || cuaGa.quantity < 50
            || hongMao == null || hongMao.quantity < 50) {
            Service.gI().sendThongBao(player, "Không đủ nguyên liệu! Cần 50 Ngà + 50 Cựa + 50 Hồng Mao");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
            return;
        }

        InventoryService.gI().subQuantityItemsBag(player, ngaVoi, 50);
        InventoryService.gI().subQuantityItemsBag(player, cuaGa, 50);
        InventoryService.gI().subQuantityItemsBag(player, hongMao, 50);

        // 10 Thỏi Vàng
        Item thoiVang = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, 10);
        InventoryService.gI().addItemBag(player, thoiVang);

        // +20 điểm sự kiện
        player.event.addDiemSuKien(20);

        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().sendThongBao(player, "Đổi thành công!\nNhận được 10 Thỏi Vàng + 20 điểm sự kiện");
    }

    private void showConfirmDoiDuaHau(Player player) {
        int duaHau = getItemQuantity(player, ConstItem.DUA_HAU);
        createOtherMenu(player, MENU_CONFIRM_DOI_DUA_HAU,
                "|7|⚜ ĐỔI DƯA HẤU ⚜\n\n"
                + "|0|Đang có: " + duaHau + " Dưa Hấu\n\n"
                + "|1|Yêu cầu: 99 Dưa Hấu\n"
                + "|2|Nhận được: +10 điểm BXH Dưa Hấu\n\n"
                + "|7|Bạn có muốn đổi không?",
                "Đồng ý", "Từ chối");
    }

    private void doDoiDuaHau(Player player) {
        Item duaHau = InventoryService.gI().findItemBag(player, ConstItem.DUA_HAU);
        if (duaHau == null || duaHau.quantity < 99) {
            Service.gI().sendThongBao(player, "Không đủ 99 Dưa Hấu!");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, duaHau, 99);
        player.event.addDiemSuKien(10);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Đổi thành công! +10 điểm BXH Sự Kiện");
    }

    private void showConfirmDoiManhDB(Player player) {
        int mdb = getItemQuantity(player, ConstItem.MANH_DINH_BA);
        createOtherMenu(player, MENU_CONFIRM_DOI_MANH_DB,
                "|7|⚜ ĐỔI MẢNH ĐINH BA ⚜\n\n"
                + "|0|Đang có: " + mdb + " Mảnh Đinh Ba\n\n"
                + "|1|Yêu cầu: 99 Mảnh Đinh Ba\n"
                + "|2|Nhận được: +15 điểm + 5 Thỏi Vàng\n\n"
                + "|7|Bạn có muốn đổi không?",
                "Đồng ý", "Từ chối");
    }

    private void doDoiManhDinhBa(Player player) {
        Item mdb = InventoryService.gI().findItemBag(player, ConstItem.MANH_DINH_BA);
        if (mdb == null || mdb.quantity < 99) {
            Service.gI().sendThongBao(player, "Không đủ 99 Mảnh Đinh Ba!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, mdb, 99);
        Item tv = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, 5);
        InventoryService.gI().addItemBag(player, tv);
        player.event.addDiemSuKien(15);
        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().sendThongBao(player, "Đổi thành công! +15 điểm + 5 Thỏi Vàng");
    }

    private void showConfirmDoiManhCT(Player player) {
        int mct = getItemQuantity(player, ConstItem.MANH_CUNG_TEN);
        createOtherMenu(player, MENU_CONFIRM_DOI_MANH_CT,
                "|7|⚜ ĐỔI MẢNH CUNG TÊN ⚜\n\n"
                + "|0|Đang có: " + mct + " Mảnh Cung Tên\n\n"
                + "|1|Yêu cầu: 99 Mảnh Cung Tên\n"
                + "|2|Nhận được: +15 điểm + 5 Thỏi Vàng\n\n"
                + "|7|Bạn có muốn đổi không?",
                "Đồng ý", "Từ chối");
    }

    private void doDoiManhCungTen(Player player) {
        Item mct = InventoryService.gI().findItemBag(player, ConstItem.MANH_CUNG_TEN);
        if (mct == null || mct.quantity < 99) {
            Service.gI().sendThongBao(player, "Không đủ 99 Mảnh Cung Tên!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, mct, 99);
        Item tv = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, 5);
        InventoryService.gI().addItemBag(player, tv);
        player.event.addDiemSuKien(15);
        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().sendThongBao(player, "Đổi thành công! +15 điểm + 5 Thỏi Vàng");
    }

    // ==========================================
    // ĐỔI CẢI TRANG
    // ==========================================
    private void showDoiCaiTrangMenu(Player player) {
        createOtherMenu(player, MENU_DOI_CAI_TRANG,
                "|7|⚜ ĐỔI CẢI TRANG VIP ⚜\n\n"
                + "|0|Cải trang sự kiện Hùng Vương\n"
                + "|0|mỗi loại cần 50 lễ vật tương ứng\n\n"
                + "|5|1. CT Sơn Tinh: 50 Ngà Voi\n"
                + "|5|2. CT Thuỷ Tinh: 50 Cựa Gà\n"
                + "|5|3. CT Mị Nương: 50 Hồng Mao",
                "Sơn Tinh",
                "Thuỷ Tinh",
                "Mị Nương",
                "Quay lại");
    }

    private void handleDoiCaiTrangMenu(Player player, int select) {
        switch (select) {
            case 0 -> showConfirmCaiTrang(player, MENU_CONFIRM_DOI_CT_SON_TINH,
                    "Sơn Tinh", ConstItem.NGA_VOI, "Ngà Voi", 50);
            case 1 -> showConfirmCaiTrang(player, MENU_CONFIRM_DOI_CT_THUY_TINH,
                    "Thuỷ Tinh", ConstItem.CUA_GA, "Cựa Gà", 50);
            case 2 -> showConfirmCaiTrang(player, MENU_CONFIRM_DOI_CT_MI_NUONG,
                    "Mị Nương", ConstItem.HONG_MAO, "Hồng Mao", 50);
            case 3 -> openBaseMenu(player);
        }
    }

    private void showConfirmCaiTrang(Player player, int menuId, String ctName, int matId, String matName, int qty) {
        int has = getItemQuantity(player, matId);
        createOtherMenu(player, menuId,
                "|7|⚜ ĐỔI CẢI TRANG " + ctName.toUpperCase() + " ⚜\n\n"
                + "|1|Yêu cầu: " + qty + " " + matName + "\n"
                + "|0|Đang có: " + has + " " + matName + "\n\n"
                + "|2|Nhận được:\n"
                + "|5|• Cải trang " + ctName + " (30 ngày)\n\n"
                + "|7|Bạn có muốn đổi không?",
                "Đồng ý", "Từ chối");
    }

    private void doDoiCaiTrang(Player player, int ctItemId, String ctName, int matId, int qty) {
        Item matItem = InventoryService.gI().findItemBag(player, matId);
        if (matItem == null || matItem.quantity < qty) {
            Service.gI().sendThongBao(player, "Không đủ nguyên liệu!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, matItem, qty);

        Item ct = ItemService.gI().createNewItem((short) ctItemId, 1);
        // Hạn 30 ngày
        ct.itemOptions.add(new Item.ItemOption(93, 30));
        InventoryService.gI().addItemBag(player, ct);

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Đổi thành công!\nNhận được Cải trang " + ctName + " (30 ngày)");
    }

    // ==========================================
    // ĐỔI LINH THÚ
    // ==========================================
    private void showDoiLinhThuMenu(Player player) {
        createOtherMenu(player, MENU_DOI_LINH_THU,
                "|7|⚜ ĐỔI LINH THÚ HUYỀN THOẠI ⚜\n\n"
                + "|0|Linh thú sự kiện Hùng Vương\n\n"
                + "|5|1. Pet Voi Chín Ngà:\n"
                + "|0|   99 Ngà + 20 Thỏi Vàng\n"
                + "|5|2. Pet Gà Chín Cựa:\n"
                + "|0|   99 Cựa + 20 Thỏi Vàng\n"
                + "|5|3. Pet Ngựa Chín Hồng Mao:\n"
                + "|0|   99 H.Mao + 20 Thỏi Vàng",
                "Voi\nChín Ngà",
                "Gà\nChín Cựa",
                "Ngựa\nHồng Mao",
                "Quay lại");
    }

    private void handleDoiLinhThuMenu(Player player, int select) {
        switch (select) {
            case 0 -> showConfirmLinhThu(player, 0, "Voi Chín Ngà", ConstItem.NGA_VOI, "Ngà Voi");
            case 1 -> showConfirmLinhThu(player, 1, "Gà Chín Cựa", ConstItem.CUA_GA, "Cựa Gà");
            case 2 -> showConfirmLinhThu(player, 2, "Ngựa Chín Hồng Mao", ConstItem.HONG_MAO, "Hồng Mao");
            case 3 -> openBaseMenu(player);
        }
    }

    private int pendingLinhThu = -1;

    private void showConfirmLinhThu(Player player, int type, String petName, int matId, String matName) {
        pendingLinhThu = type;
        int has = getItemQuantity(player, matId);
        int hasTv = getItemQuantity(player, ConstItem.THOI_VANG);
        createOtherMenu(player, MENU_CONFIRM_DOI_LINH_THU,
                "|7|⚜ ĐỔI PET " + petName.toUpperCase() + " ⚜\n\n"
                + "|1|Yêu cầu:\n"
                + "|0|• 99 " + matName + " (có: " + has + ")\n"
                + "|0|• 20 Thỏi Vàng (có: " + hasTv + ")\n\n"
                + "|2|Nhận được:\n"
                + "|5|• Pet " + petName + " (30 ngày)\n\n"
                + "|7|Bạn có muốn đổi không?",
                "Đồng ý", "Từ chối");
    }

    private void handleConfirmLinhThu(Player player, int select) {
        if (select != 0) return;

        int matId, petId;
        String petName;
        switch (pendingLinhThu) {
            case 0 -> { matId = ConstItem.NGA_VOI; petId = ConstItem.PET_VOI_CHIN_NGA; petName = "Voi Chín Ngà"; }
            case 1 -> { matId = ConstItem.CUA_GA; petId = ConstItem.PET_GA_CHIN_CUA; petName = "Gà Chín Cựa"; }
            case 2 -> { matId = ConstItem.HONG_MAO; petId = ConstItem.PET_NGUA_CHIN_HONG_MAO; petName = "Ngựa Chín Hồng Mao"; }
            default -> { return; }
        }

        Item matItem = InventoryService.gI().findItemBag(player, matId);
        Item thoiVang = InventoryService.gI().findItemBag(player, ConstItem.THOI_VANG);

        if (matItem == null || matItem.quantity < 99
            || thoiVang == null || thoiVang.quantity < 20) {
            Service.gI().sendThongBao(player, "Không đủ nguyên liệu!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
            return;
        }

        InventoryService.gI().subQuantityItemsBag(player, matItem, 99);
        InventoryService.gI().subQuantityItemsBag(player, thoiVang, 20);

        Item pet = ItemService.gI().createNewItem((short) petId, 1);
        pet.itemOptions.add(new Item.ItemOption(93, 30)); // 30 ngày
        InventoryService.gI().addItemBag(player, pet);

        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().sendThongBao(player, "Đổi thành công!\nNhận được Pet " + petName + " (30 ngày)");
        pendingLinhThu = -1;
    }

    // ==========================================
    // TOP DƯA HẤU (dùng điểm sự kiện)
    // ==========================================
    private void showTopDuaHau(Player player) {
        createOtherMenu(player, MENU_TOP_DUA_HAU,
                "|7|⚜ BXH DƯA HẤU ⚜\n\n"
                + "|0|Điểm sự kiện của bạn: " + player.event.getDiemSuKien() + "\n\n"
                + "|5|Cách tích điểm:\n"
                + "|0|• Đổi 99 Dưa Hấu = +10 điểm\n"
                + "|0|• Hạ Boss = +1 điểm\n\n"
                + "|1|Top sự kiện tại NPC Đại Thiên Sứ",
                "Quay lại");
    }

    // ==========================================
    // TOP SƠN TINH - THUỶ TINH
    // ==========================================
    private void showTopSonTinhThuyTinh(Player player) {
        createOtherMenu(player, MENU_TOP_SON_TINH_THUY_TINH,
                "|7|⚜ BXH SƠN TINH - THUỶ TINH ⚜\n\n"
                + "|0|Điểm Boss của bạn: " + player.event.getDiemSuKien() + "\n\n"
                + "|5|Cách tích điểm:\n"
                + "|0|• Tiêu diệt Sơn Tinh = +1 điểm\n"
                + "|0|• Tiêu diệt Thuỷ Tinh = +1 điểm\n"
                + "|0|• Tiêu diệt Voi/Gà/Ngựa = +1 điểm\n\n"
                + "|1|Bật cờ phe để hỗ trợ Boss\n"
                + "|1|Top tại NPC Đại Thiên Sứ",
                "Quay lại");
    }

    // ==========================================
    // CHÚC PHÚC x2 TNSM
    // ==========================================
    private void doChucPhuc(Player player) {
        Item thoiVang = InventoryService.gI().findItemBag(player, ConstItem.THOI_VANG);
        if (thoiVang == null || thoiVang.quantity < 5) {
            Service.gI().sendThongBao(player, "Cần 5 Thỏi Vàng để được Chúc Phúc (+50 điểm sự kiện)");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, thoiVang, 5);

        // +50 điểm sự kiện
        player.event.addDiemSuKien(50);
        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);

        createOtherMenu(player, MENU_CHUC_PHUC,
                "|7|⚜ CHÚC PHÚC HÙNG VƯƠNG ⚜\n\n"
                + "|2|Đã nhận +50 Điểm Sự Kiện!\n"
                + "|5|Chi phí: 5 Thỏi Vàng\n\n"
                + "|1|Chúc chiến binh " + player.name + "\n"
                + "chiến đấu dũng mãnh!",
                "Quay lại");
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================
    private int getItemQuantity(Player player, int itemId) {
        Item item = InventoryService.gI().findItemBag(player, itemId);
        return (item != null) ? item.quantity : 0;
    }
}
