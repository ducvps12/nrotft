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
    public static final int VIP1_PRICE = 40_000;
    public static final int VIP2_PRICE = 80_000;      // Gốc 100K, sale 20%
    public static final int VIP3_PRICE = 400_000;     // Gốc 500K, sale 20%

    public static final int VIP2_PRICE_ORIGINAL = 100_000;
    public static final int VIP3_PRICE_ORIGINAL = 500_000;

    // Thời hạn gói VIP = 7 ngày
    public static final long VIP_DURATION_MS = 7L * 24 * 60 * 60 * 1000;

    // ===================== GIÁ GÓI ĐỆ TỬ NGÀY =====================
    public static final int DETU1_PRICE = 16_000;
    public static final int DETU2_PRICE = 40_000;
    public static final int DETU3_PRICE = 80_000;
    public static final int DETU4_PRICE = 160_000;

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
            case 1, 2 -> {
                if (player.pet == null) {
                    PetService.gI().createNormalPet(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn đã có đệ tử, items buff đã vào túi!");
                }
            }
            case 3 -> {
                if (player.pet == null) {
                    PetService.gI().createMabuPet(player);
                } else {
                    PetService.gI().changeMabuPet(player);
                }
            }
            case 4 -> {
                if (player.pet == null) {
                    PetService.gI().createBlackGokuPet(player, player.gender);
                } else {
                    PetService.gI().changeBlackGokuPet(player);
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
            case 1 -> "|7|━━━ GÓI ĐỆ TỬ 1 ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getPetPrice(1)) + " VNĐ\n"
                    + "|2|• Đệ Tử Thường (VĨNH VIỄN)\n"
                    + "|8|• 200 Thỏi Vàng (khóa)\n"
                    + "|8|• 3 Bình TNSM (khóa)\n"
                    + "|3|Đệ tử giữ mãi, không hết hạn!\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 2 -> "|7|━━━ GÓI ĐỆ TỬ 2 ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getPetPrice(2)) + " VNĐ\n"
                    + "|2|• Đệ Tử Thường (VĨNH VIỄN)\n"
                    + "|8|• 500 Thỏi Vàng (khóa)\n"
                    + "|8|• 5 Bình TNSM (khóa)\n"
                    + "|8|• 10 Đá Bảo Vệ (khóa)\n"
                    + "|3|Đệ tử giữ mãi, không hết hạn!\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 3 -> "|7|━━━ GÓI ĐỆ TỬ 3 ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getPetPrice(3)) + " VNĐ\n"
                    + "|2|• Đệ Tử Mabu - mạnh (VĨNH VIỄN)\n"
                    + "|8|• 1000 Thỏi Vàng (khóa)\n"
                    + "|8|• 10 Bình TNSM (khóa)\n"
                    + "|8|• 20 Đá Bảo Vệ (khóa)\n"
                    + "|3|Nếu đã có đệ → nâng cấp Mabu!\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            case 4 -> "|7|━━━ GÓI ĐỆ TỬ 4 ━━━\n"
                    + "|1|Giá: " + Util.mumberToLouis(getPetPrice(4)) + " VNĐ\n"
                    + "|2|• Đệ Black Goku - siêu mạnh (VĨNH VIỄN)\n"
                    + "|8|• 2000 Thỏi Vàng (khóa)\n"
                    + "|8|• 15 Bình TNSM (khóa)\n"
                    + "|8|• 30 Đá Bảo Vệ (khóa)\n"
                    + "|8|• 5 Phiếu Giảm Giá\n"
                    + "|3|Nếu đã có đệ → nâng cấp B.Goku!\n"
                    + "|7|━━━━━━━━━━━━━━━━━━";
            default -> "";
        };
    }
}
