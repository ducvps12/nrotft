package nro.services;

import item.Item;
import item.Item.ItemOption;
import java.sql.*;
import jdbc.DBConnecter;
import nro.player.Player;
import player.badges.BadgesData;
import jdbc.daos.PlayerDAO;
import utils.Logger;
import utils.Util;
import nro.services.ItemTimeService;

/**
 * Hệ thống GÓI VIP TUẦN & GÓI ĐỆ TỬ NGÀY
 * Tại NPC Lý Tiểu Nương
 */
public class VipPackageService {

    private static VipPackageService instance;

    public static VipPackageService gI() {
        if (instance == null) {
            instance = new VipPackageService();
        }
        return instance;
    }

    // ===================== GIÁ GÓI VIP TUẦN =====================
    public static final int VIP1_PRICE = 100_000;
    public static final int VIP2_PRICE = 240_000;      // Gốc 300K, sale 20%
    public static final int VIP3_PRICE = 800_000;     // Gốc 1000K, sale 20%

    public static final int VIP2_PRICE_ORIGINAL = 300_000;
    public static final int VIP3_PRICE_ORIGINAL = 1_000_000;

    // Thời hạn gói VIP = 7 ngày
    public static final long VIP_DURATION_MS = 7L * 24 * 60 * 60 * 1000;

    // ===================== GIÁ GÓI ĐỆ TỬ NGÀY =====================
    public static final int DETU1_PRICE = 50_000;
    public static final int DETU2_PRICE = 100_000;
    public static final int DETU3_PRICE = 250_000;
    public static final int DETU4_PRICE = 500_000;

    // Thời hạn gói đệ tử = 1 ngày
    public static final long DETU_DURATION_MS = 24L * 60 * 60 * 1000;

    // ===================== FLASH SALE =====================
    private static volatile boolean flashSaleActive = false;
    private static volatile int flashSalePercent = 30;

    public static void setFlashSale(boolean active, int percent) {
        flashSaleActive = active;
        flashSalePercent = percent;
    }

    public static boolean isFlashSaleActive() {
        return flashSaleActive;
    }

    public static int getFlashSalePercent() {
        return flashSalePercent;
    }

    public int getFinalPrice(int basePrice) {
        if (flashSaleActive) {
            return basePrice - (basePrice * flashSalePercent / 100);
        }
        return basePrice;
    }

    // ===================== ITEM IDS =====================
    private static final short THOI_VANG = 457;
    private static final short DA_BAO_VE = 987;
    private static final short PHIEU_GIAM_GIA = 459;
    private static final short NGOC_RONG_1_SAO = 14; // ID 14 = Ngọc Rồng 1 sao (ID 17 = 4 sao!)
    private static final short CAPSULE_VANG = 956;
    // Bình TNSM: 441-447
    private static final int TNSM_MIN = 441;
    private static final int TNSM_MAX = 447;

    // ===================== INIT TABLE =====================
    public static void initTable() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            String sql = "CREATE TABLE IF NOT EXISTS history_vip_purchase ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "account_id INT NOT NULL, "
                    + "player_id INT NOT NULL, "
                    + "package_type VARCHAR(20) NOT NULL, "
                    + "tier INT NOT NULL, "
                    + "price INT NOT NULL, "
                    + "purchased_at DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + "expires_at DATETIME NOT NULL, "
                    + "INDEX idx_account (account_id), "
                    + "INDEX idx_player (player_id)"
                    + ")";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            Logger.error("VipPackageService: Lỗi tạo bảng history_vip_purchase: " + e.getMessage());
        }
    }

    // ===================== KIỂM TRA THỜI HẠN =====================
    /**
     * Kiểm tra player đã mua gói VIP tuần chưa (còn hiệu lực)
     */
    public boolean hasActiveVipPackage(Player player) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            String sql = "SELECT COUNT(*) FROM history_vip_purchase "
                    + "WHERE account_id = ? AND package_type = 'VIP_TUAN' AND expires_at > NOW()";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, player.getSession().userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            // Table chưa tồn tại hoặc lỗi → cho phép mua
        }
        return false;
    }

    /**
     * Kiểm tra player đã mua gói đệ tử hôm nay chưa
     */
    public boolean hasActivePetPackage(Player player) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            String sql = "SELECT COUNT(*) FROM history_vip_purchase "
                    + "WHERE account_id = ? AND package_type = 'DE_TU_NGAY' AND DATE(purchased_at) = CURDATE()";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, player.getSession().userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            // Cho phép mua nếu lỗi
        }
        return false;
    }

    /**
     * Lấy thời gian hết hạn gói VIP hiện tại (nếu có)
     */
    public String getVipExpireInfo(Player player) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            String sql = "SELECT expires_at FROM history_vip_purchase "
                    + "WHERE account_id = ? AND package_type = 'VIP_TUAN' AND expires_at > NOW() "
                    + "ORDER BY expires_at DESC LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, player.getSession().userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getTimestamp("expires_at").toString();
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return null;
    }

    // ===================== GHI LỊCH SỬ MUA =====================
    private void recordPurchase(Player player, String packageType, int tier, int price, long durationMs) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            String sql = "INSERT INTO history_vip_purchase (account_id, player_id, package_type, tier, price, purchased_at, expires_at) "
                    + "VALUES (?, ?, ?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? SECOND))";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, player.getSession().userId);
                ps.setLong(2, player.id);
                ps.setString(3, packageType);
                ps.setInt(4, tier);
                ps.setInt(5, price);
                ps.setLong(6, durationMs / 1000);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            Logger.error("VipPackageService: Lỗi ghi lịch sử mua: " + e.getMessage());
        }
    }

    // ===================== MUA GÓI VIP TUẦN =====================
    public boolean purchaseVipPackage(Player player, int tier) {
        if (hasActiveVipPackage(player)) {
            String expire = getVipExpireInfo(player);
            Service.gI().sendThongBao(player,
                    "Bạn đã mua gói VIP tuần rồi!\nHết hạn: " + (expire != null ? expire : "đang tính..."));
            return false;
        }

        int price = getVipPrice(tier);
        int requiredSlots = tier == 3 ? 6 : 5;

        if (player.getSession().cash < price) {
            Service.gI().sendThongBao(player,
                    "Không đủ VNĐ! Cần " + Util.mumberToLouis(price) + " VNĐ\nSố dư: "
                            + Util.mumberToLouis(player.getSession().cash) + " VNĐ");
            return false;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < requiredSlots) {
            Service.gI().sendThongBao(player,
                    "Cần ít nhất " + requiredSlots + " ô trống trong hành trang!");
            return false;
        }

        // Trừ tiền
        PlayerDAO.subcash(player, price, "VIP_TUAN_" + tier,
                "Mua gói VIP Tuần " + tier + " giá " + price);

        // Trao quà
        giveVipRewards(player, tier);

        // Ghi lịch sử
        recordPurchase(player, "VIP_TUAN", tier, price, VIP_DURATION_MS);

        Service.gI().sendThongBao(player,
                "🎉 Mua GÓI VIP " + tier + " thành công!\nHiệu lực 7 ngày.\nKiểm tra hành trang nhé!");
        return true;
    }

    // ===================== MUA GÓI ĐỆ TỬ NGÀY =====================
    public boolean purchasePetPackage(Player player, int tier) {
        if (hasActivePetPackage(player)) {
            Service.gI().sendThongBao(player, "Hôm nay bạn đã mua gói Đệ Tử rồi!\nQuay lại ngày mai nhé.");
            return false;
        }

        int price = getPetPrice(tier);
        int requiredSlots = tier >= 3 ? 4 : 3;

        if (player.getSession().cash < price) {
            Service.gI().sendThongBao(player,
                    "Không đủ VNĐ! Cần " + Util.mumberToLouis(price) + " VNĐ");
            return false;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < requiredSlots) {
            Service.gI().sendThongBao(player,
                    "Cần ít nhất " + requiredSlots + " ô trống trong hành trang!");
            return false;
        }

        // Trừ tiền
        PlayerDAO.subcash(player, price, "DE_TU_" + tier,
                "Mua gói Đệ Tử " + tier + " giá " + price);

        // Trao quà items
        givePetRewards(player, tier);

        // Trao đệ tử (nếu chưa có hoặc upgrade)
        givePetCreation(player, tier);

        // Ghi lịch sử
        recordPurchase(player, "DE_TU_NGAY", tier, price, DETU_DURATION_MS);

        Service.gI().sendThongBao(player,
                "🎉 Mua GÓI ĐỆ TỬ " + tier + " thành công!\nKiểm tra hành trang và đệ tử nhé!");
        return true;
    }

    // ===================== TRAO QUÀ VIP =====================
    private void giveVipRewards(Player player, int tier) {
        switch (tier) {
            case 1 -> giveVip1Rewards(player);
            case 2 -> giveVip2Rewards(player);
            case 3 -> giveVip3Rewards(player);
        }
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
    }

    /**
     * VIP 1 (40K): 100 Thỏi Vàng khóa, 3 Bình TNSM, 10 Đá Bảo Vệ, 3 Phiếu Giảm Giá
     */
    private void giveVip1Rewards(Player player) {
        // Thỏi Vàng khóa
        Item thoiVang = ItemService.gI().createNewItem(THOI_VANG, 500);
        thoiVang.itemOptions.add(new ItemOption(30, 1)); // khóa
        InventoryService.gI().addItemBag(player, thoiVang);

        // Bình TNSM random
        Item tnsm = ItemService.gI().createNewItem((short) Util.nextInt(TNSM_MIN, TNSM_MAX), 3);
        tnsm.itemOptions.add(new ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, tnsm);

        // Đá Bảo Vệ
        Item daBV = ItemService.gI().createNewItem(DA_BAO_VE, 10);
        daBV.itemOptions.add(new ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, daBV);

        // Phiếu Giảm Giá
        Item phieu = ItemService.gI().createNewItem(PHIEU_GIAM_GIA, 3);
        InventoryService.gI().addItemBag(player, phieu);
    }

    /**
     * VIP 2 (80K): 500 Thỏi Vàng khóa, 5 Bình TNSM, 30 Đá Bảo Vệ, 5 Phiếu GG, Danh hiệu 7 ngày
     */
    private void giveVip2Rewards(Player player) {
        Item thoiVang = ItemService.gI().createNewItem(THOI_VANG, 1000);
        thoiVang.itemOptions.add(new ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, thoiVang);

        Item tnsm = ItemService.gI().createNewItem((short) Util.nextInt(TNSM_MIN, TNSM_MAX), 5);
        tnsm.itemOptions.add(new ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, tnsm);

        Item daBV = ItemService.gI().createNewItem(DA_BAO_VE, 30);
        daBV.itemOptions.add(new ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, daBV);

        Item phieu = ItemService.gI().createNewItem(PHIEU_GIAM_GIA, 5);
        InventoryService.gI().addItemBag(player, phieu);

        // Danh hiệu VIP 7 ngày (id=1 cho VIP badge)
        new BadgesData(player, 1, 7);
    }

    /**
     * VIP 3 (400K): 2000 TV khóa, 10 TNSM, 100 ĐBV, 10 PGG, 5 NR 1 sao, Capsule Vàng, DH 7 ngày
     */
    private void giveVip3Rewards(Player player) {
        Item thoiVang = ItemService.gI().createNewItem(THOI_VANG, 5000);
        thoiVang.itemOptions.add(new ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, thoiVang);

        Item tnsm = ItemService.gI().createNewItem((short) Util.nextInt(TNSM_MIN, TNSM_MAX), 10);
        tnsm.itemOptions.add(new ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, tnsm);

        Item daBV = ItemService.gI().createNewItem(DA_BAO_VE, 100);
        daBV.itemOptions.add(new ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, daBV);

        Item phieu = ItemService.gI().createNewItem(PHIEU_GIAM_GIA, 10);
        InventoryService.gI().addItemBag(player, phieu);

        // Ngọc Rồng 1 sao khóa
        Item ngocRong = ItemService.gI().createNewItem(NGOC_RONG_1_SAO, 5);
        ngocRong.itemOptions.add(new ItemOption(30, 1));
        InventoryService.gI().addItemBag(player, ngocRong);

        // Danh hiệu VIP 7 ngày
        new BadgesData(player, 1, 7);
    }

    // ===================== TRAO ĐỆ TỬ =====================
    private void givePetRewards(Player player, int tier) {
        switch (tier) {
            case 1 -> {
                Item tv = ItemService.gI().createNewItem(THOI_VANG, 200);
                tv.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, tv);

                Item tnsm = ItemService.gI().createNewItem((short) Util.nextInt(TNSM_MIN, TNSM_MAX), 3);
                tnsm.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, tnsm);
            }
            case 2 -> {
                Item tv = ItemService.gI().createNewItem(THOI_VANG, 500);
                tv.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, tv);

                Item tnsm = ItemService.gI().createNewItem((short) Util.nextInt(TNSM_MIN, TNSM_MAX), 5);
                tnsm.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, tnsm);

                Item daBV = ItemService.gI().createNewItem(DA_BAO_VE, 10);
                daBV.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, daBV);
            }
            case 3 -> {
                Item tv = ItemService.gI().createNewItem(THOI_VANG, 1000);
                tv.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, tv);

                Item tnsm = ItemService.gI().createNewItem((short) Util.nextInt(TNSM_MIN, TNSM_MAX), 10);
                tnsm.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, tnsm);

                Item daBV = ItemService.gI().createNewItem(DA_BAO_VE, 20);
                daBV.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, daBV);
            }
            case 4 -> {
                Item tv = ItemService.gI().createNewItem(THOI_VANG, 2000);
                tv.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, tv);

                Item tnsm = ItemService.gI().createNewItem((short) Util.nextInt(TNSM_MIN, TNSM_MAX), 15);
                tnsm.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, tnsm);

                Item daBV = ItemService.gI().createNewItem(DA_BAO_VE, 30);
                daBV.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(player, daBV);

                Item phieu = ItemService.gI().createNewItem(PHIEU_GIAM_GIA, 5);
                InventoryService.gI().addItemBag(player, phieu);
            }
        }
        InventoryService.gI().sendItemBag(player);
    }

    private void givePetCreation(Player player, int tier) {
        switch (tier) {
            case 1 -> { // Đệ Mabu
                if (player.pet == null) {
                    PetService.gI().createMabuPet(player);
                } else {
                    PetService.gI().changeMabuPet(player);
                }
            }
            case 2 -> { // Đệ Black Goku
                if (player.pet == null) {
                    PetService.gI().createBlackGokuPet(player, player.gender);
                } else {
                    PetService.gI().changeBlackGokuPet(player);
                }
            }
            case 3 -> { // Đệ Cell
                if (player.pet == null) {
                    PetService.gI().createCellPet(player, player.gender);
                } else {
                    PetService.gI().changeCellPet(player);
                }
            }
            case 4 -> { // Đệ Berus
                if (player.pet == null) {
                    PetService.gI().createBerusPet(player, player.gender);
                } else {
                    PetService.gI().changeBerusPet(player);
                }
            }
        }
    }

    // ===================== HELPER =====================
    public int getVipPrice(int tier) {
        int base = switch (tier) {
            case 1 -> VIP1_PRICE;
            case 2 -> VIP2_PRICE;
            case 3 -> VIP3_PRICE;
            default -> 0;
        };
        return getFinalPrice(base);
    }

    public int getPetPrice(int tier) {
        int base = switch (tier) {
            case 1 -> DETU1_PRICE;
            case 2 -> DETU2_PRICE;
            case 3 -> DETU3_PRICE;
            case 4 -> DETU4_PRICE;
            default -> 0;
        };
        return getFinalPrice(base);
    }

    public String getVipDescription(int tier) {
        return switch (tier) {
            case 1 -> "|7|━━━ GÓI VIP 1 ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getVipPrice(1)) + " VNĐ\n"
                    + "|8|• 500 Thỏi Vàng (khóa)\n"
                    + "|8|• 3 Bình TNSM (khóa)\n"
                    + "|8|• 10 Đá Bảo Vệ (khóa)\n"
                    + "|8|• 3 Phiếu Giảm Giá\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 2 -> "|7|━━━ GÓI VIP 2 ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getVipPrice(2)) + " VNĐ"
                    + (VIP2_PRICE < VIP2_PRICE_ORIGINAL ? " (Gốc " + Util.mumberToLouis(VIP2_PRICE_ORIGINAL) + ")" : "") + "\n"
                    + "|8|• 1000 Thỏi Vàng (khóa)\n"
                    + "|8|• 5 Bình TNSM (khóa)\n"
                    + "|8|• 30 Đá Bảo Vệ (khóa)\n"
                    + "|8|• 5 Phiếu Giảm Giá\n"
                    + "|2|• Danh Hiệu VIP (7 ngày)\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 3 -> "|7|━━━ GÓI VIP 3 ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getVipPrice(3)) + " VNĐ"
                    + (VIP3_PRICE < VIP3_PRICE_ORIGINAL ? " (Gốc " + Util.mumberToLouis(VIP3_PRICE_ORIGINAL) + ")" : "") + "\n"
                    + "|8|• 5000 Thỏi Vàng (khóa)\n"
                    + "|8|• 10 Bình TNSM (khóa)\n"
                    + "|8|• 100 Đá Bảo Vệ (khóa)\n"
                    + "|8|• 10 Phiếu Giảm Giá\n"
                    + "|8|• 5 Ngọc Rồng 1 Sao (khóa)\n"
                    + "|2|• Danh Hiệu VIP (7 ngày)\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            default -> "";
        };
    }

    public String getPetDescription(int tier) {
        return switch (tier) {
            case 1 -> "|7|━━━ GÓI ĐỆ MABU ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getPetPrice(1)) + " VNĐ\n"
                    + "|2|• Đệ Tử Mabu (VĨNH VIỄN)\n"
                    + "|8|• 200 Thỏi Vàng (khóa)\n"
                    + "|8|• 3 Bình TNSM (khóa)\n"
                    + "|3|SM khởi điểm 1.5M, chỉ số cao!\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 2 -> "|7|━━━ GÓI ĐỆ B.GOKU ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getPetPrice(2)) + " VNĐ\n"
                    + "|2|• Đệ Black Goku (VĨNH VIỄN)\n"
                    + "|8|• 500 Thỏi Vàng (khóa)\n"
                    + "|8|• 5 Bình TNSM (khóa)\n"
                    + "|8|• 10 Đá Bảo Vệ (khóa)\n"
                    + "|3|SM khởi điểm 1.5M, 9 ô trang bị!\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 3 -> "|7|━━━ GÓI ĐỆ CELL ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getPetPrice(3)) + " VNĐ\n"
                    + "|2|• Đệ Tử Cell (VĨNH VIỄN)\n"
                    + "|8|• 1000 Thỏi Vàng (khóa)\n"
                    + "|8|• 10 Bình TNSM (khóa)\n"
                    + "|8|• 20 Đá Bảo Vệ (khóa)\n"
                    + "|3|SM 1.5M, 9 ô trang bị, chỉ số cao!\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 4 -> "|7|━━━ GÓI ĐỆ BERUS ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getPetPrice(4)) + " VNĐ\n"
                    + "|2|• Đệ Thần Hủy Diệt Berus (VĨNH VIỄN)\n"
                    + "|8|• 2000 Thỏi Vàng (khóa)\n"
                    + "|8|• 15 Bình TNSM (khóa)\n"
                    + "|8|• 30 Đá Bảo Vệ (khóa)\n"
                    + "|8|• 5 Phiếu Giảm Giá\n"
                    + "|3|SM 1.5M, 9 ô, Thần Hủy Diệt!\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            default -> "";
        };
    }

    // ===================== GIÁ GÓI VIP ĐỆ TỬ =====================
    public static final int VIP_PET1_PRICE = 100_000;   // x2 TNSM + phân bổ HP/DAME 24h
    public static final int VIP_PET2_PRICE = 250_000;   // x3 TNSM + phân bổ HP/DAME 24h + buff dame đệ
    public static final int VIP_PET3_PRICE = 500_000;  // x5 TNSM + phân bổ HP/DAME 24h + buff dame + crit
    public static final int VIP_PET4_PRICE = 1_000_000;  // VIP CAO THỦ: x7 TNSM + admin custom chỉ số đệ

    // Thời hạn gói VIP Đệ = 24 giờ
    public static final long VIP_PET_DURATION_MS = 24L * 60 * 60 * 1000;

    // Chế độ phân bổ chỉ số VIP Đệ
    // 0 = mặc định (random cân bằng), 1 = HP/DAME focus
    public static final byte PET_DIST_DEFAULT = 0;
    public static final byte PET_DIST_HP_DAME = 1;

    // ===================== KIỂM TRA GÓI VIP ĐỆ =====================
    /**
     * Kiểm tra player đã mua gói VIP Đệ chưa (còn hiệu lực)
     */
    public boolean hasActiveVipPetPackage(Player player) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            String sql = "SELECT tier FROM history_vip_purchase "
                    + "WHERE account_id = ? AND package_type = 'VIP_PET' AND expires_at > NOW() "
                    + "ORDER BY tier DESC LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, player.getSession().userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // Cho phép mua nếu lỗi
        }
        return false;
    }

    /**
     * Lấy tier VIP Đệ hiện tại (0 nếu không có)
     */
    public int getActiveVipPetTier(Player player) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            String sql = "SELECT tier FROM history_vip_purchase "
                    + "WHERE account_id = ? AND package_type = 'VIP_PET' AND expires_at > NOW() "
                    + "ORDER BY tier DESC LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, player.getSession().userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getInt("tier");
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return 0;
    }

    /**
     * Lấy thời gian hết hạn VIP Đệ hiện tại
     */
    public String getVipPetExpireInfo(Player player) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            String sql = "SELECT expires_at FROM history_vip_purchase "
                    + "WHERE account_id = ? AND package_type = 'VIP_PET' AND expires_at > NOW() "
                    + "ORDER BY expires_at DESC LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, player.getSession().userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getTimestamp("expires_at").toString();
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return null;
    }

    // ===================== MUA GÓI VIP ĐỆ TỬ =====================
    public boolean purchaseVipPetPackage(Player player, int tier) {
        if (player.pet == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có đệ tử!\nHãy mua Gói Đệ Tử trước.");
            return false;
        }

        // Cho phép nâng cấp tier cao hơn nếu đang có gói thấp hơn
        int currentTier = getActiveVipPetTier(player);
        if (currentTier >= tier) {
            String expire = getVipPetExpireInfo(player);
            Service.gI().sendThongBao(player,
                    "Đang có gói VIP Đệ " + currentTier + " rồi!"
                    + "\nHết hạn: " + (expire != null ? expire : "đang tính...")
                    + (currentTier < 3 ? "\nBạn có thể nâng cấp lên gói cao hơn." : ""));
            return false;
        }

        int price = getVipPetPrice(tier);

        if (player.getSession().cash < price) {
            Service.gI().sendThongBao(player,
                    "Không đủ VNĐ! Cần " + Util.mumberToLouis(price) + " VNĐ\nSố dư: "
                            + Util.mumberToLouis(player.getSession().cash) + " VNĐ");
            return false;
        }

        // Trừ tiền
        PlayerDAO.subcash(player, price, "VIP_PET_" + tier,
                "Mua gói VIP Đệ Tử " + tier + " giá " + price);

        // Kích hoạt buff
        activateVipPetBuff(player, tier);

        // Ghi lịch sử
        recordPurchase(player, "VIP_PET", tier, price, VIP_PET_DURATION_MS);

        String tierName = switch (tier) {
            case 1 -> "BẠC (x2 TNSM)";
            case 2 -> "VÀNG (x3 TNSM)";
            case 3 -> "KIM CƯƠNG (x5 TNSM)";
            case 4 -> "CAO THỦ (x7 TNSM)";
            default -> "";
        };

        Service.gI().sendThongBao(player,
                "🎉 Mua GÓI VIP ĐỆ " + tierName + " thành công!"
                + "\nHiệu lực 24 giờ."
                + "\n✅ TNSM đệ x" + getVipPetTnsmMultiplier(tier)
                + "\n✅ Phân bổ ưu tiên HP + DAME"
                + (tier >= 2 ? "\n✅ Buff dame đệ x2" : "")
                + (tier >= 3 ? "\n✅ Buff crit đệ +5" : "")
                + (tier >= 4 ? "\n✅ Admin custom chỉ số đệ\n✅ Đệ không tự cộng Giáp/Chí Mạng" : ""));
        return true;
    }

    /**
     * Kích hoạt buff VIP Đệ
     */
    private void activateVipPetBuff(Player player, int tier) {
        // 1. Buff TNSM cho đệ qua hệ thống charms.tdDeTu (24h)
        int minutesLeft = (int) (VIP_PET_DURATION_MS / 60000);
        player.charms.addTimeCharms(522, minutesLeft); // 522 = bùa đệ tử

        // 2. Set chế độ phân bổ HP/DAME
        player.petVipDistMode = PET_DIST_HP_DAME;
        player.petVipTier = (byte) tier;

        // 3. Buff dame + crit trực tiếp cho đệ (theo tier)
        if (player.pet != null) {
            if (tier >= 2) {
                // Buff dame: thêm 500 dame gốc cho đệ
                player.pet.damageBonus += 500;
            }
            if (tier >= 3) {
                // Buff crit: thêm 5 crit gốc cho đệ
                player.pet.nPoint.critg += 5;
            }
            if (tier >= 4) {
                // VIP CAO THỦ: bật chế độ admin custom chỉ số
                player.petVipCaoThuMode = 1;
                // Block đệ tự cộng DEF/CRIT cho đến khi admin cho phép
                player.petCaoThuAllowDef = false;
                player.petCaoThuAllowCrit = false;
            }
            // Tính lại chỉ số
            player.pet.nPoint.calPoint();
            Service.gI().point(player.pet);
            Service.gI().sendChiSoPetGoc(player);
            Service.gI().showInfoPet(player);
        }
    }

    /**
     * Lấy hệ số nhân TNSM theo tier VIP Đệ
     */
    public static int getVipPetTnsmMultiplier(int tier) {
        return switch (tier) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 7;
            default -> 1;
        };
    }

    /**
     * Khôi phục trạng thái VIP Đệ từ DB khi player login hoặc khi cần.
     * Fix bug: petVipDistMode/petVipTier chỉ lưu trong memory, mất khi relog.
     */
    public void restoreVipPetState(Player player) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            String sql = "SELECT tier FROM history_vip_purchase "
                    + "WHERE account_id = ? AND package_type = 'VIP_PET' AND expires_at > NOW() "
                    + "ORDER BY tier DESC LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, player.getSession().userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int tier = rs.getInt("tier");
                    player.petVipDistMode = PET_DIST_HP_DAME;
                    player.petVipTier = (byte) tier;
                    if (tier >= 4) {
                        player.petVipCaoThuMode = 1;
                        // Giữ nguyên trạng thái allow DEF/CRIT từ admin
                    }
                } else {
                    // Hết hạn hoặc chưa mua → reset về mặc định
                    player.petVipDistMode = PET_DIST_DEFAULT;
                    player.petVipTier = 0;
                    player.petVipCaoThuMode = 0;
                }
            }
        } catch (SQLException e) {
            // Nếu lỗi DB, giữ nguyên giá trị hiện tại
            Logger.error("VipPackageService: Lỗi restore VIP Pet state: " + e.getMessage());
        }
    }

    // ===================== HELPER VIP ĐỆ =====================
    public int getVipPetPrice(int tier) {
        int base = switch (tier) {
            case 1 -> VIP_PET1_PRICE;
            case 2 -> VIP_PET2_PRICE;
            case 3 -> VIP_PET3_PRICE;
            case 4 -> VIP_PET4_PRICE;
            default -> 0;
        };
        return getFinalPrice(base);
    }

    public String getVipPetDescription(int tier) {
        return switch (tier) {
            case 1 -> "|7|━━━ VIP ĐỆ BẠC ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getVipPetPrice(1)) + " VNĐ\n"
                    + "|3|Thời hạn: 24 giờ\n"
                    + "|8|• TNSM đệ tử x2\n"
                    + "|8|• Phân bổ ưu tiên HP + DAME\n"
                    + "|8|  (HP 50%, DAME 50% - CHI HP + DAME)\n"
                    + "|8|• Bùa Đệ Tử 24h (dame đệ x2, TNSM đệ x2)\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 2 -> "|7|━━━ VIP ĐỆ VÀNG ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getVipPetPrice(2)) + " VNĐ\n"
                    + "|3|Thời hạn: 24 giờ\n"
                    + "|8|• TNSM đệ tử x3\n"
                    + "|8|• Phân bổ ưu tiên HP + DAME\n"
                    + "|8|  (HP 50%, DAME 50% - CHI HP + DAME)\n"
                    + "|8|• Bùa Đệ Tử 24h (dame đệ x2, TNSM đệ x2)\n"
                    + "|2|• +500 dame gốc cho đệ\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 3 -> "|7|━━━ VIP ĐỆ KIM CƯƠNG ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getVipPetPrice(3)) + " VNĐ\n"
                    + "|3|Thời hạn: 24 giờ\n"
                    + "|8|• TNSM đệ tử x5\n"
                    + "|8|• Phân bổ ưu tiên HP + DAME\n"
                    + "|8|  (HP 50%, DAME 50% - CHI HP + DAME)\n"
                    + "|8|• Bùa Đệ Tử 24h (dame đệ x2, TNSM đệ x2)\n"
                    + "|2|• +500 dame gốc cho đệ\n"
                    + "|2|• +5 crit gốc cho đệ\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 4 -> "|7|━━━ VIP ĐỆ CAO THỦ ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getVipPetPrice(4)) + " VNĐ\n"
                    + "|3|Thời hạn: 24 giờ\n"
                    + "|8|• TNSM đệ tử x7\n"
                    + "|8|• Phân bổ ưu tiên HP + DAME\n"
                    + "|8|  (HP 50%, DAME 50% - CHI HP + DAME)\n"
                    + "|8|• Bùa Đệ Tử 24h (dame đệ x2, TNSM đệ x2)\n"
                    + "|2|• +500 dame gốc cho đệ\n"
                    + "|2|• +5 crit gốc cho đệ\n"
                    + "|1|• ⭐ Admin custom chỉ số đệ tử\n"
                    + "|1|• ⭐ Đệ KHÔNG tự cộng Giáp + Chí Mạng\n"
                    + "|8|  (chờ Admin cho phép mới được cộng)\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            default -> "";
        };
    }

    // ===================== PHIẾU GIẢM GIÁ CHO VIP ĐỆ =====================
    /**
     * Tính giá VIP Đệ sau khi áp dụng phiếu giảm giá.
     * PGG VIP (721) = giảm 70%, PGG thường (459) = giảm 30%.
     * Ưu tiên PGG VIP nếu có cả hai.
     */
    public int getVipPetPriceWithCoupon(Player player, int tier) {
        int basePrice = getVipPetPrice(tier);
        if (hasVIPDiscountCoupon(player)) {
            return basePrice - (basePrice * 70 / 100); // Giảm 70%
        }
        if (hasDiscountCoupon(player)) {
            return basePrice - (basePrice * 30 / 100); // Giảm 30%
        }
        return basePrice;
    }

    /**
     * Kiểm tra player có PGG thường (459) - giảm 30%
     */
    public boolean hasDiscountCoupon(Player player) {
        return player.itemTime != null
            && player.itemTime.isUsePhieuGiamGia
            && !player.itemTime.usedPhieuGiamGia;
    }

    /**
     * Kiểm tra player có PGG VIP (721) - giảm 70%
     */
    public boolean hasVIPDiscountCoupon(Player player) {
        return player.itemTime != null
            && player.itemTime.isUsePhieuGiamGiaVIP
            && !player.itemTime.usedPhieuGiamGiaVIP;
    }

    /**
     * Sử dụng phiếu giảm giá — Ưu tiên PGG VIP (70%) trước, rồi PGG thường (30%)
     */
    public void useDiscountCoupon(Player player) {
        if (player.itemTime != null && player.itemTime.isUsePhieuGiamGiaVIP) {
            player.itemTime.usedPhieuGiamGiaVIP = true;
            ItemTimeService.gI().removeItemTime(player, 721);
            Service.gI().sendThongBao(player, "Đã sử dụng Phiếu Giảm Giá VIP 70%!\nPhiếu hết hiệu lực.");
        } else if (player.itemTime != null && player.itemTime.isUsePhieuGiamGia) {
            player.itemTime.usedPhieuGiamGia = true;
            ItemTimeService.gI().removeItemTime(player, 459);
            Service.gI().sendThongBao(player, "Đã sử dụng Phiếu Giảm Giá 30%!\nPhiếu hết hiệu lực.");
        }
    }

    /**
     * Mua VIP Đệ có sử dụng phiếu giảm giá 30%.
     * Tự động trừ phiếu nếu có.
     */
    public boolean purchaseVipPetWithCoupon(Player player, int tier) {
        if (player.pet == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có đệ tử!\nHãy mua Gói Đệ Tử trước.");
            return false;
        }

        int currentTier = getActiveVipPetTier(player);
        if (currentTier >= tier) {
            String expire = getVipPetExpireInfo(player);
            Service.gI().sendThongBao(player,
                    "Đang có gói VIP Đệ " + currentTier + " rồi!"
                    + "\nHết hạn: " + (expire != null ? expire : "đang tính...")
                    + (currentTier < 4 ? "\nBạn có thể nâng cấp lên gói cao hơn." : ""));
            return false;
        }

        boolean hasCouponVIP = hasVIPDiscountCoupon(player);
        boolean hasCoupon = hasCouponVIP || hasDiscountCoupon(player);
        int price = hasCoupon ? getVipPetPriceWithCoupon(player, tier) : getVipPetPrice(tier);

        if (player.getSession().cash < price) {
            Service.gI().sendThongBao(player,
                    "Không đủ VNĐ! Cần " + Util.mumberToLouis(price) + " VNĐ\nSố dư: "
                            + Util.mumberToLouis(player.getSession().cash) + " VNĐ");
            return false;
        }

        // Trừ phiếu giảm giá nếu có
        if (hasCoupon) {
            useDiscountCoupon(player);
        }

        // Trừ tiền
        jdbc.daos.PlayerDAO.subcash(player, price, "VIP_PET_" + tier,
                "Mua gói VIP Đệ Tử " + tier + " giá " + price + (hasCoupon ? " (giảm 30%)" : ""));

        // Kích hoạt buff
        activateVipPetBuff(player, tier);

        // Ghi lịch sử
        recordPurchase(player, "VIP_PET", tier, price, VIP_PET_DURATION_MS);

        String tierName = switch (tier) {
            case 1 -> "BẠC (x2 TNSM)";
            case 2 -> "VÀNG (x3 TNSM)";
            case 3 -> "KIM CƯƠNG (x5 TNSM)";
            case 4 -> "CAO THỦ (x7 TNSM)";
            default -> "";
        };

        Service.gI().sendThongBao(player,
                "🎉 Mua GÓI VIP ĐỆ " + tierName + " thành công!"
                + (hasCoupon ? "\n|1|🎫 Đã sử dụng Phiếu Giảm Giá" + (hasCouponVIP ? " VIP 70%" : " 30%") + "!" : "")
                + "\nGiá: " + Util.mumberToLouis(price) + " VNĐ"
                + "\nHiệu lực 24 giờ."
                + "\n✅ TNSM đệ x" + getVipPetTnsmMultiplier(tier)
                + (tier >= 4 ? "\n✅ Admin custom chỉ số đệ" : ""));
        return true;
    }

    // ===================== ADMIN CUSTOM CHỈ SỐ ĐỆ (VIP CAO THỦ) =====================
    /**
     * Admin set chỉ số bonus cho đệ tử (VIP CAO THỦ).
     * Đệ sẽ nhận thêm các chỉ số này khi tính toán.
     */
    public static void adminSetPetCaoThuStats(Player player, int hp, int mp, int dame, int def, int crit) {
        player.petCaoThuBonusHp = hp;
        player.petCaoThuBonusMp = mp;
        player.petCaoThuBonusDame = dame;
        player.petCaoThuBonusDef = def;
        player.petCaoThuBonusCrit = crit;

        // Apply vào đệ tử ngay
        if (player.pet != null) {
            player.pet.nPoint.calPoint();
            Service.gI().point(player.pet);
            Service.gI().sendChiSoPetGoc(player);
            Service.gI().showInfoPet(player);
        }

        Service.gI().sendThongBao(player,
                "⭐ Admin đã cập nhật chỉ số đệ tử!\n"
                + "HP+" + hp + ", MP+" + mp + ", DAME+" + dame
                + "\nDEF+" + def + ", CRIT+" + crit);
    }

    /**
     * Admin cho phép/cấm đệ tự cộng DEF
     */
    public static void adminTogglePetDef(Player player, boolean allow) {
        player.petCaoThuAllowDef = allow;
        Service.gI().sendThongBao(player,
                allow ? "✅ Đệ tử đã được phép tự cộng Giáp" : "❌ Đệ tử KHÔNG được tự cộng Giáp");
    }

    /**
     * Admin cho phép/cấm đệ tự cộng CRIT
     */
    public static void adminTogglePetCrit(Player player, boolean allow) {
        player.petCaoThuAllowCrit = allow;
        Service.gI().sendThongBao(player,
                allow ? "✅ Đệ tử đã được phép tự cộng Chí Mạng" : "❌ Đệ tử KHÔNG được tự cộng Chí Mạng");
    }
}
