package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import item.Item;
import jdbc.daos.PlayerDAO;
import models.ClanBoss.ClanBoss;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.player.Inventory;
import nro.server.Manager;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.PlayerService;
import nro.services.Service;
import services.func.ChangeMapService;
import services.func.Input;
import utils.TimeUtil;
import utils.Util;
import shop.ShopService;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Santa extends Npc {

    private static final int MENU_GUILD = 1001;
    private static final int MENU_UP_CLAN = 1002;
    private static final int MENU_DOI_VND = 1003;
    private static final int MENU_SHOPEE = 1004;
    private static final int MENU_SHOPEE_CONFIRM = 1005;
    private static final int MENU_HUONG_DAN = 1006;
    private static final int MENU_GOLD_MARKET = 1007;
    private static final int MENU_MUA_THOI_VANG = 1008;
    private static final int MENU_MUA_CONFIRM = 1009;

    // Shopee Affiliate config
    private static final String SHOPEE_LINK = "https://s.shopee.vn/1BIoU3HenV";
    private static final int SHOPEE_REWARD_VND = 10_000; // 10k VND
    private static final long SHOPEE_COOLDOWN = 48 * 60 * 60 * 1000L; // 48h
    private static final ConcurrentHashMap<Integer, Long> lastShopeeClick = new ConcurrentHashMap<>();

    // ====== HỆ THỐNG GIÁ VÀNG THỊ TRƯỜNG ======
    private static final long GIA_BAN_GOC = 50_000_000L;   // Bán 1 thỏi = 50tr
    private static final long GIA_MUA_GOC = 65_000_000L;   // Mua 1 thỏi = 65tr
    private static final double DAO_DONG_MAX = 0.05;        // ±5% dao động
    private static long lastPriceUpdate = 0;
    private static long currentBuyPrice = GIA_MUA_GOC;
    private static long currentSellPrice = GIA_BAN_GOC;
    private static final long PRICE_UPDATE_INTERVAL = 30 * 60 * 1000L; // 30 phút cập nhật giá

    // Lưu giá cũ để hiển thị xu hướng
    private static long previousBuyPrice = GIA_MUA_GOC;
    private static long previousSellPrice = GIA_BAN_GOC;

    public Santa(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    // ====== CẬP NHẬT GIÁ VÀNG ======
    private static synchronized void updateGoldPrice() {
        long now = System.currentTimeMillis();
        if (now - lastPriceUpdate < PRICE_UPDATE_INTERVAL) return;

        previousBuyPrice = currentBuyPrice;
        previousSellPrice = currentSellPrice;

        // Dao động ngẫu nhiên ±5%
        double buyFluctuation = 1.0 + (Util.nextInt(-50, 50) / 1000.0);
        double sellFluctuation = 1.0 + (Util.nextInt(-50, 50) / 1000.0);

        currentBuyPrice = (long) (GIA_MUA_GOC * buyFluctuation);
        currentSellPrice = (long) (GIA_BAN_GOC * sellFluctuation);

        // Giá mua luôn > giá bán (chênh lệch ít nhất 10tr)
        if (currentBuyPrice <= currentSellPrice + 10_000_000L) {
            currentBuyPrice = currentSellPrice + 10_000_000L;
        }

        lastPriceUpdate = now;
        System.out.println("[GOLD_MARKET] Giá mua: " + Util.numberToMoney(currentBuyPrice)
                + " | Giá bán: " + Util.numberToMoney(currentSellPrice));
    }

    private static String getTrend(long current, long previous) {
        if (current > previous) return "|1|▲ TĂNG";
        if (current < previous) return "|8|▼ GIẢM";
        return "|2|► ỔN ĐỊNH";
    }

    private static String getTimeToNextUpdate() {
        long remaining = PRICE_UPDATE_INTERVAL - (System.currentTimeMillis() - lastPriceUpdate);
        if (remaining <= 0) return "Đang cập nhật...";
        long mins = remaining / 60000;
        long secs = (remaining % 60000) / 1000;
        return mins + " phút " + secs + " giây";
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) return;

        updateGoldPrice();

        Item thoiVang = InventoryService.gI().findItemBag(player, 457);
        int slTV = (thoiVang != null) ? thoiVang.quantity : 0;

        ArrayList<String> menu = new ArrayList<>();
        menu.add("Cửa hàng");
        menu.add("Hướng dẫn\nchi tiết");
        menu.add("Thị trường\nVàng");
        menu.add("Shop xu");
        menu.add("Đổi VNĐ");
        menu.add("Ủng hộ\nAdmin\n+10k VNĐ");

        String[] menus = menu.toArray(String[]::new);
        player.iDMark.menuSelect = menus;

        this.createOtherMenu(
                player,
                ConstNpc.BASE_MENU,
                "|7|━━━ SANTA ━━━\n\n"
                        + "|1|Xin chào " + player.name + "!\n"
                        + "|2|★ Thỏi vàng: |8|" + slTV + "\n"
                        + "|2|★ VNĐ: |8|" + Util.numberToMoney(player.getSession() != null ? player.getSession().cash : 0) + "\n\n"
                        + "|8|Cửa hàng → Mua sắm vật phẩm\n"
                        + "|8|Hướng dẫn → Tác dụng chi tiết\n"
                        + "|8|Thị trường → Mua/Bán thỏi vàng",
                menus);
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        if (player.iDMark.isBaseMenu()) {
            handleBaseMenu(player, select);
            return;
        }

        switch (player.iDMark.getIndexMenu()) {
            case MENU_HUONG_DAN -> handleHuongDan(player, select);
            case MENU_GOLD_MARKET -> handleGoldMarket(player, select);
            case MENU_MUA_THOI_VANG -> handleMuaThoiVangConfirm(player, select);
            case MENU_DOI_VND -> handleDoiVndSelect(player, select);
            case MENU_UP_CLAN -> { if (select == 0) upgradeClanLevel(player); }
            case MENU_GUILD -> handleGuild(player, select);
            case MENU_SHOPEE -> { if (select == 0) openMenuShopeeConfirm(player); }
            case MENU_SHOPEE_CONFIRM -> { if (select == 0) claimShopeeReward(player); }
        }
    }

    // ====== XỬ LÝ MENU GỐC ======
    private void handleBaseMenu(Player player, int select) {
        if (player == null || player.iDMark == null || player.iDMark.menuSelect == null) return;
        if (select < 0 || select >= player.iDMark.menuSelect.length) return;

        String chosen = player.iDMark.menuSelect[select];

        if (chosen.equals("Cửa hàng")) {
            ShopService.gI().opendShop(player, "SANTA", false);
            return;
        }
        if (chosen.contains("Hướng dẫn")) {
            openMenuHuongDan(player);
            return;
        }
        if (chosen.contains("Thị trường")) {
            openMenuGoldMarket(player);
            return;
        }
        if (chosen.equals("Shop xu")) {
            ShopService.gI().opendShop(player, "LUNAR_NEW_YEAR", false);
            return;
        }
        if (chosen.contains("Đổi VNĐ")) {
            openMenuDoiVnd(player);
            return;
        }
        if (chosen.contains("Ủng hộ")) {
            openMenuShopee(player);
            return;
        }
    }

    // ====== HƯỚNG DẪN CHI TIẾT ======
    private void openMenuHuongDan(Player player) {
        this.createOtherMenu(player, MENU_HUONG_DAN,
                "|7|━━ HƯỚNG DẪN CHI TIẾT ━━\n\n"
                        + "|1|1. Cửa hàng:|8| Mua vật phẩm đặc biệt,\n"
                        + "cải trang, pet bằng Thỏi vàng/Ngọc\n\n"
                        + "|1|2. Thị trường Vàng:|8| Theo dõi giá\n"
                        + "thỏi vàng biến động, mua/bán tối ưu\n\n"
                        + "|1|3. Shop Xu:|8| Đổi Xu NRO lấy vật phẩm\n\n"
                        + "|1|4. Đổi VNĐ:|8| Quy đổi Xu NRO → VNĐ\n\n"
                        + "|1|5. Ủng hộ Admin:|8| +10k VNĐ/48h",
                "Cách kiếm\nThỏi vàng",
                "Thị trường\nlà gì?",
                "Đóng");
    }

    private void handleHuongDan(Player player, int select) {
        switch (select) {
            case 0 -> {
                Service.gI().sendThongBao(player,
                        "★ CÁCH KIẾM THỎI VÀNG ★\n\n"
                                + "1. Điểm danh → Bonus ngày 3 & 7\n"
                                + "2. Đổi Hộp Quà SK → 20% drop\n"
                                + "3. Ước nguyện Rồng Thần\n"
                                + "4. Mở Rương ĐHVT 23\n"
                                + "5. Mua bằng VNĐ (20k = 10 thỏi)\n"
                                + "6. Mua bằng vàng game tại Thị Trường\n\n"
                                + "→ Bán thỏi vàng: kéo vào shop bán");
            }
            case 1 -> {
                updateGoldPrice();
                Service.gI().sendThongBao(player,
                        "★ THỊ TRƯỜNG VÀNG LÀ GÌ? ★\n\n"
                                + "Giống thực tế, giá vàng biến động!\n"
                                + "→ Giá MUA luôn CAO hơn giá BÁN\n"
                                + "→ Dao động ±5% mỗi 30 phút\n\n"
                                + "Ví dụ hiện tại:\n"
                                + "• Bán 1 thỏi = " + Util.numberToMoney(currentSellPrice) + "\n"
                                + "• Mua 1 thỏi = " + Util.numberToMoney(currentBuyPrice) + "\n\n"
                                + "→ Mua thấp bán cao = KIẾM LỜI!");
            }
        }
    }

    // ====== THỊ TRƯỜNG VÀNG ======
    private void openMenuGoldMarket(Player player) {
        updateGoldPrice();

        Item thoiVang = InventoryService.gI().findItemBag(player, 457);
        int slTV = (thoiVang != null) ? thoiVang.quantity : 0;

        String buyTrend = getTrend(currentBuyPrice, previousBuyPrice);
        String sellTrend = getTrend(currentSellPrice, previousSellPrice);

        this.createOtherMenu(player, MENU_GOLD_MARKET,
                "|7|━━ THỊ TRƯỜNG VÀNG ━━\n\n"
                        + "|1|Giá MUA: |8|" + Util.numberToMoney(currentBuyPrice) + "/thỏi " + buyTrend + "\n"
                        + "|1|Giá BÁN: |8|" + Util.numberToMoney(currentSellPrice) + "/thỏi " + sellTrend + "\n"
                        + "|2|Chênh lệch: " + Util.numberToMoney(currentBuyPrice - currentSellPrice) + "\n\n"
                        + "|8|Thỏi vàng hiện có: " + slTV + "\n"
                        + "|8|Vàng: " + Util.numberToMoney(player.inventory.gold) + "\n"
                        + "|8|Cập nhật sau: " + getTimeToNextUpdate(),
                "Mua\nThỏi vàng",
                "Bán\nThỏi vàng",
                "Danh hiệu",
                "Phụ kiện",
                "Đóng");
    }

    private void handleGoldMarket(Player player, int select) {
        switch (select) {
            case 0 -> openMenuMuaThoiVang(player);
            case 1 -> Input.gI().createFormBanSLL(player);
            case 2 -> ShopService.gI().opendShop(player, "SANTA_DANH_HIEU", false);
            case 3 -> ShopService.gI().opendShop(player, "SANTA_PHUKIEN", false);
        }
    }

    // ====== MUA THỎI VÀNG BẰNG VÀNG GAME ======
    private void openMenuMuaThoiVang(Player player) {
        updateGoldPrice();

        long gold = player.inventory.gold;
        int maxCanBuy = (int) Math.min(gold / currentBuyPrice, 999);

        this.createOtherMenu(player, MENU_MUA_THOI_VANG,
                "|7|━━ MUA THỎI VÀNG ━━\n\n"
                        + "|1|Giá hiện tại: |8|" + Util.numberToMoney(currentBuyPrice) + "/thỏi\n"
                        + "|8|Vàng hiện có: " + Util.numberToMoney(gold) + "\n"
                        + "|8|Mua tối đa: " + maxCanBuy + " thỏi\n\n"
                        + "|2|Chọn số lượng muốn mua:",
                "Mua 1",
                "Mua 5",
                "Mua 10",
                "Mua 50",
                "Đóng");
    }

    private void handleMuaThoiVangConfirm(Player player, int select) {
        int[] quantities = {1, 5, 10, 50};
        if (select < 0 || select >= quantities.length) return;

        int qty = quantities[select];
        executeMuaThoiVang(player, qty);
    }

    private void executeMuaThoiVang(Player player, int qty) {
        updateGoldPrice();
        long totalCost = currentBuyPrice * qty;

        if (player.inventory.gold < totalCost) {
            Service.gI().sendThongBao(player,
                    "Không đủ vàng!\nCần: " + Util.numberToMoney(totalCost)
                            + "\nHiện có: " + Util.numberToMoney(player.inventory.gold));
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đầy, hãy dọn chỗ trống!");
            return;
        }

        // Trừ vàng
        player.inventory.gold -= totalCost;

        // Thêm thỏi vàng
        Item thoiVang = ItemService.gI().createNewItem((short) 457, qty);
        InventoryService.gI().addItemBag(player, thoiVang);
        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().sendMoney(player);

        Service.gI().sendThongBao(player,
                "★ Mua thành công " + qty + " Thỏi vàng!\n"
                        + "Chi phí: " + Util.numberToMoney(totalCost) + "\n"
                        + "Giá: " + Util.numberToMoney(currentBuyPrice) + "/thỏi");

        System.out.println("[GOLD_MARKET] " + player.name + " MUA " + qty
                + " thỏi vàng, giá " + Util.numberToMoney(currentBuyPrice) + "/thỏi");
    }

    // ====== ĐỔI VNĐ ======
    private void openMenuDoiVnd(Player player) {
        createOtherMenu(player, MENU_DOI_VND,
                "Đổi Xu NRO → VNĐ\n"
                        + "Hiện có: " + Util.numberToMoney(player.getSession() != null ? player.getSession().cash : 0) + " VNĐ\n"
                        + "Không giới hạn lượt đổi!",
                "100 xu\n1k VNĐ",
                "500 xu\n5k VNĐ",
                "1000 xu\n10k VNĐ");
    }

    private void handleDoiVndSelect(Player player, int select) {
        switch (select) {
            case 0 -> doiVnd(player, 100, 1_000);
            case 1 -> doiVnd(player, 500, 5_000);
            case 2 -> doiVnd(player, 1000, 10_000);
        }
    }

    private void handleGuild(Player player, int select) {
        switch (select) {
            case 0 -> ShopService.gI().opendShop(player, "SANTA_GUILD", false);
            case 1 -> openMenuUpClan(player);
            case 2 -> goToClanBossMap(player);
        }
    }

    private void openMenuUpClan(Player player) {
        createOtherMenu(player, MENU_UP_CLAN,
                "Nâng cấp bang hội\n"
                        + "Cấp hiện tại: " + player.clan.level + "\n"
                        + "Giá: 10.000 VNĐ / 1 cấp\n"
                        + "Mỗi cấp +5% HP, KI, SD",
                "Nâng cấp", "Đóng");
    }

    private void upgradeClanLevel(Player player) {
        if (player.clan == null) return;
        if (player.clan.level >= 5) {
            Service.gI().sendThongBao(player, "Bang hội đã đạt cấp tối đa (5)\nKhông thể nâng cấp thêm");
            return;
        }
        if (!PlayerDAO.subcash(player, 10000, "NANG_CAP_BANG", "ClanLv:" + player.clan.level)) {
            Service.gI().sendThongBao(player, "Bạn không đủ 10.000 VNĐ");
            return;
        }
        player.clan.level++;
        Manager.updateClanLevel(player.clan);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Nâng cấp bang hội thành công\nBang hiện tại cấp " + player.clan.level);
    }

    private void goToClanBossMap(Player player) {
        if (!TimeUtil.isClanBossOpen()) {
            Service.gI().sendThongBao(player,
                    "Map săn Boss Bang Hội chỉ mở trong khung giờ "
                            + ClanBoss.HOUR_OPEN + "h - " + ClanBoss.HOUR_CLOSE + "h");
            return;
        }
        ChangeMapService.gI().changeMapBySpaceShip(player, 175, -1, -1);
    }

    private void doiVnd(Player player, int xuCan, int vndNhan) {
        var itemXu = InventoryService.gI().findItemBag(player, 1705);
        if (itemXu == null || itemXu.quantity < xuCan) {
            Service.gI().sendThongBao(player, "Không đủ xu (cần " + xuCan + " Xu NRO)");
            return;
        }
        itemXu.quantity -= xuCan;
        if (itemXu.quantity <= 0) {
            InventoryService.gI().removeItemBag(player, itemXu);
        }
        if (!PlayerDAO.addCashNoDanap(player.getSession().userId, vndNhan, "DOI_XU_VND",
                "Xu:" + xuCan + " VND:" + vndNhan)) {
            Service.gI().sendThongBao(player, "Lỗi cộng VNĐ");
            return;
        }
        player.getSession().cash += vndNhan;
        player.getSession().vnd += vndNhan;
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Đổi thành công " + xuCan + " xu → " + Util.numberToMoney(vndNhan) + " VNĐ");
    }

    // ====== SHOPEE AFFILIATE ======
    private void openMenuShopee(Player player) {
        Long lastClick = lastShopeeClick.get((int) player.id);
        if (lastClick != null && (System.currentTimeMillis() - lastClick) < SHOPEE_COOLDOWN) {
            long remaining = SHOPEE_COOLDOWN - (System.currentTimeMillis() - lastClick);
            long hours = remaining / (60 * 60 * 1000);
            long mins = (remaining % (60 * 60 * 1000)) / (60 * 1000);
            Service.gI().LinkService(player, 10684,
                    "Bạn đã nhận thưởng rồi!\nCòn " + hours + "h" + mins + "p nữa mới nhận tiếp được.\n\nHãy nhấn Mở Shopee để mua sắm ủng hộ Admin nhé!",
                    SHOPEE_LINK, "Mở Shopee");
            return;
        }
        if (!PlayerDAO.addCashNoDanap(player.getSession().userId, SHOPEE_REWARD_VND,
                "SHOPEE_AFFILIATE", "Reward:" + SHOPEE_REWARD_VND)) {
            Service.gI().sendThongBao(player, "Lỗi hệ thống, thử lại sau!");
            return;
        }
        player.getSession().cash += SHOPEE_REWARD_VND;
        player.getSession().vnd += SHOPEE_REWARD_VND;
        lastShopeeClick.put((int) player.id, System.currentTimeMillis());
        Service.gI().sendMoney(player);
        Service.gI().LinkService(player, 10684,
                "CẢM ƠN BẠN ĐÃ ỦNG HỘ!\n"
                        + "Nhận thưởng: +" + Util.numberToMoney(SHOPEE_REWARD_VND) + " VNĐ\n\n"
                        + "Hệ thống sẽ tự động chuyển hướng,\n"
                        + "nhấn Mở Shopee để bắt đầu mua sắm ủng hộ Admin!",
                SHOPEE_LINK, "Mở Shopee");
        System.out.println("[SHOPEE] " + player.name + " claimed affiliate reward " + SHOPEE_REWARD_VND + " VND");
    }

    private void openMenuShopeeConfirm(Player player) {
        // Không dùng nữa, đã gộp vào openMenuShopee
    }

    private void claimShopeeReward(Player player) {
        // Không dùng nữa, đã gộp vào openMenuShopee
    }

    // ====== PUBLIC: Lấy giá bán hiện tại cho hệ thống bán thỏi vàng ======
    public static long getCurrentSellPrice() {
        updateGoldPrice();
        return currentSellPrice;
    }

    public static long getCurrentBuyPrice() {
        updateGoldPrice();
        return currentBuyPrice;
    }
}