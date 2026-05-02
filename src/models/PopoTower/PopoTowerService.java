package models.PopoTower;

import boss.BossID;
import item.Item;
import jdbc.DBConnecter;
import map.Zone;
import models.Training.TrainingService;
import nro.player.Player;
import nro.server.ServerNotify;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.MapService;
import nro.services.PlayerService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Util;

public class PopoTowerService {

    private static final int MAX_FLOOR = 50;
    private static final int FREE_TICKET_PER_DAY = 8;
    private static final int MAP_SOLO = 49;
    private static final int X_START = 295;
    private static final int Y_START = 408;

    private static PopoTowerService instance;

    public static PopoTowerService gI() {
        if (instance == null) {
            instance = new PopoTowerService();
        }
        return instance;
    }

    public void openMenu(Player player, nro.models.npc.Npc npc) {
        resetDaily(player);
        int remaining = Math.max(0, FREE_TICKET_PER_DAY - player.popoTowerTodayCount);
        String tierInfo = getTierName(player.popoTowerFloor);
        npc.createOtherMenu(player, consts.ConstNpc.MENU_POPO_TOWER,
                "|7|━━ THÁP HUẤN LUYỆN PÔPÔ ━━\n\n"
                        + "|1|Tầng đã chinh phục: |8|" + player.popoTowerFloor + " / " + MAX_FLOOR + "\n"
                        + "|1|Cấp bậc: |2|" + tierInfo + "\n"
                        + "|1|Lượt hôm nay: |8|" + remaining + " / " + FREE_TICKET_PER_DAY + "\n\n"
                        + "|2|★ Phần thưởng mỗi tầng:\n"
                        + "|8|• Xu NRO + Vàng + Ngọc\n"
                        + "|8|• Tầng 10+: Capsule, Đá nâng cấp\n"
                        + "|8|• Tầng 20+: Hộp SKH, Thỏi vàng\n"
                        + "|8|• Tầng 30+: Mảnh Bông tai, Sách TK\n"
                        + "|8|• Tầng 40+: Set Thần Linh, CT VIP\n\n"
                        + "|7|━━━━━━━━━━━━━━━━━━━",
                "Vào\nthử thách", "Phần\nthưởng", "Hướng\ndẫn", "Top\nPôPô", "Đóng");
    }

    public void handleMenu(Player player, nro.models.npc.Npc npc, int select) {
        switch (select) {
            case 0 -> startChallenge(player);
            case 1 -> showReward(player, npc);
            case 2 -> showGuide(player, npc);
            case 3 -> showTop(player, npc);
        }
    }

    public void startChallenge(Player player) {
        resetDaily(player);
        if (player.popoTowerFloor >= MAX_FLOOR) {
            Service.gI().sendThongBao(player, "Bạn đã chinh phục tối đa Tháp PôPô! Bậc Huyền Thoại!");
            return;
        }
        if (player.popoTowerTodayCount >= FREE_TICKET_PER_DAY) {
            Service.gI().sendThongBao(player, "Hôm nay đã hết " + FREE_TICKET_PER_DAY + " lượt. Quay lại ngày mai!");
            return;
        }
        Zone zone = MapService.gI().getMapCanJoin(player, MAP_SOLO, 0);
        if (zone == null) {
            Service.gI().sendThongBao(player, "Phòng huấn luyện đang quá tải, hãy thử lại sau");
            return;
        }
        player.popoTowerTodayCount++;
        player.isPopoTowerChallenge = true;
        player.popoTowerChallengeFloor = player.popoTowerFloor + 1;
        player.popoTowerStartTime = System.currentTimeMillis();
        ChangeMapService.gI().changeMap(player, zone, X_START, Y_START);
        TrainingService.gI().callBoss(player, BossID.MRPOPO, false);
        Service.gI().sendThongBao(player, "★ Tháp PôPô tầng " + player.popoTowerChallengeFloor
                + " [" + getTierName(player.popoTowerChallengeFloor) + "] - Hạ PôPô trong 3 phút!");
    }

    public void win(Player player) {
        if (!player.isPopoTowerChallenge) {
            return;
        }
        int floor = player.popoTowerChallengeFloor;
        long clearTime = Math.max(1, (System.currentTimeMillis() - player.popoTowerStartTime) / 1000);
        player.popoTowerFloor = Math.max(player.popoTowerFloor, floor);
        if (floor > player.popoTowerBestFloor || (floor == player.popoTowerBestFloor
                && (player.popoTowerBestTime <= 0 || clearTime < player.popoTowerBestTime))) {
            player.popoTowerBestFloor = floor;
            player.popoTowerBestTime = clearTime;
        }

        // ============ PHẦN THƯỞNG CƠ BẢN MỖI TẦNG ============
        int xu = 3 + floor * 2;
        long gold = floor * 3_000_000L;
        int gem = floor >= 5 ? (floor / 5) : 0; // bắt đầu nhận ngọc từ tầng 5

        // Thêm Xu NRO
        Item xuNro = ItemService.gI().createNewItem((short) 1705, xu);
        InventoryService.gI().addItemBag(player, xuNro);

        // Thêm vàng
        player.inventory.gold += gold;

        // Thêm ngọc (từ tầng 5+)
        if (gem > 0) {
            player.inventory.gem += gem;
        }

        // ============ PHẦN THƯỞNG ITEM THEO TẦNG ============
        StringBuilder bonusMsg = new StringBuilder();
        rewardFloorItems(player, floor, bonusMsg);

        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);

        String baseMsg = "★ Vượt tầng " + floor + " [" + getTierName(floor) + "] trong " + clearTime + "s!\n"
                + "Nhận: " + xu + " Xu NRO, " + Util.numberToMoney(gold) + " vàng"
                + (gem > 0 ? ", " + gem + " ngọc" : "");
        if (bonusMsg.length() > 0) {
            baseMsg += "\n" + bonusMsg;
        }
        Service.gI().sendThongBao(player, baseMsg);

        // ============ THƯỞNG MỐC ĐẶC BIỆT ============
        if (isMilestoneFloor(floor)) {
            rewardMilestone(player, floor);
            Service.gI().sendThongBaoAllPlayer("★ " + player.name + " chinh phục tầng "
                    + floor + " Tháp PôPô [" + getTierName(floor) + "]!");
        }

        saveProgress(player);
        clearChallenge(player);
    }

    public void lose(Player player) {
        if (player != null && player.isPopoTowerChallenge) {
            Service.gI().sendThongBao(player, "Thử thách PôPô thất bại! Hãy mạnh hơn rồi quay lại!");
            clearChallenge(player);
        }
    }

    public void update(Player player) {
        if (player != null && player.isPopoTowerChallenge
                && Util.canDoWithTime(player.popoTowerStartTime, 180_000)) {
            lose(player);
        }
    }

    public int getBossPowerRate(Player player) {
        int floor = player != null && player.isPopoTowerChallenge ? player.popoTowerChallengeFloor : 1;
        return 100 + floor * 18;
    }

    // ============ PHẦN THƯỞNG ITEM NGẪU NHIÊN THEO TẦNG ============
    private void rewardFloorItems(Player player, int floor, StringBuilder bonusMsg) {
        // Tầng 10+: Cơ hội nhận Capsule dây chuyền (itemId 192) hoặc Đá nâng cấp (77)
        if (floor >= 10) {
            int chance = Util.nextInt(100);
            if (chance < 30) { // 30% capsule
                Item capsule = ItemService.gI().createNewItem((short) 192, 1);
                InventoryService.gI().addItemBag(player, capsule);
                bonusMsg.append("+ Capsule dây chuyền\n");
            }
            if (chance < 20) { // 20% đá nâng cấp
                Item gem = ItemService.gI().createNewItem((short) 77, Util.nextInt(1, 3));
                InventoryService.gI().addItemBag(player, gem);
                bonusMsg.append("+ ").append(gem.quantity).append(" Đá nâng cấp\n");
            }
        }

        // Tầng 15+: Cơ hội nhận Sách kỹ năng (215) 
        if (floor >= 15) {
            if (Util.nextInt(100) < 15) { // 15%
                Item sachKN = ItemService.gI().createNewItem((short) 215, 1);
                InventoryService.gI().addItemBag(player, sachKN);
                bonusMsg.append("+ Sách kỹ năng\n");
            }
        }

        // Tầng 20+: Cơ hội nhận Hộp SKH (860), Thỏi vàng (457)
        if (floor >= 20) {
            if (Util.nextInt(100) < 15) { // 15% Thỏi vàng
                int sl = Util.nextInt(1, 3);
                Item thoiVang = ItemService.gI().createNewItem((short) 457, sl);
                InventoryService.gI().addItemBag(player, thoiVang);
                bonusMsg.append("+ ").append(sl).append(" Thỏi vàng\n");
            }
            if (Util.nextInt(100) < 8) { // 8% Hộp SKH
                Item hopSKH = ItemService.gI().createNewItem((short) 860, 1);
                InventoryService.gI().addItemBag(player, hopSKH);
                bonusMsg.append("+ Hộp SKH\n");
            }
        }

        // Tầng 30+: Cơ hội nhận Mảnh bông tai (441), Sách TK (456)
        if (floor >= 30) {
            if (Util.nextInt(100) < 10) { // 10% Mảnh bông tai
                Item mbt = ItemService.gI().createNewItem((short) 441, 1);
                InventoryService.gI().addItemBag(player, mbt);
                bonusMsg.append("+ Mảnh bông tai Porata\n");
            }
            if (Util.nextInt(100) < 5) { // 5% Sách TK2
                Item sachTK2 = ItemService.gI().createNewItem((short) 456, 1);
                InventoryService.gI().addItemBag(player, sachTK2);
                bonusMsg.append("+ Sách Tiềm Năng 2\n");
            }
        }

        // Tầng 40+: Cơ hội nhận item cực hiếm
        if (floor >= 40) {
            int rare = Util.nextInt(1000);
            if (rare < 5) { // 0.5% Pet Po
                Item petPo = ItemService.gI().createNewItem((short) 1667, 1);
                InventoryService.gI().addItemBag(player, petPo);
                bonusMsg.append("|8|★★★ PET PO ★★★\n");
                ServerNotify.gI().notify("★ " + player.name + " nhận được Pet Po từ Tháp PôPô tầng " + floor + "!");
            } else if (rare < 20) { // 1.5% Capsule thú cưỡi
                Item thuCuoi = ItemService.gI().createNewItem((short) 193, 1);
                InventoryService.gI().addItemBag(player, thuCuoi);
                bonusMsg.append("|2|★ Capsule thú cưỡi\n");
            }
        }
    }

    // ============ PHẦN THƯỞNG MỐC ĐẶC BIỆT ============
    private boolean isMilestoneFloor(int floor) {
        return floor == 5 || floor == 10 || floor == 15 || floor == 20
                || floor == 25 || floor == 30 || floor == 35
                || floor == 40 || floor == 45 || floor == 50;
    }

    private void rewardMilestone(Player player, int floor) {
        int bonusXu = 0;
        int bonusGem = 0;
        long bonusGold = 0;
        String specialReward = "";

        switch (floor) {
            case 5 -> {
                bonusXu = 15;
                bonusGem = 5;
                bonusGold = 10_000_000L;
            }
            case 10 -> {
                bonusXu = 30;
                bonusGem = 15;
                bonusGold = 30_000_000L;
                // Thưởng 3 Capsule dây chuyền
                Item cap = ItemService.gI().createNewItem((short) 192, 3);
                InventoryService.gI().addItemBag(player, cap);
                specialReward = ", 3 Capsule dây chuyền";
            }
            case 15 -> {
                bonusXu = 50;
                bonusGem = 30;
                bonusGold = 50_000_000L;
                // Thưởng 2 Sách kỹ năng
                Item skn = ItemService.gI().createNewItem((short) 215, 2);
                InventoryService.gI().addItemBag(player, skn);
                specialReward = ", 2 Sách kỹ năng";
            }
            case 20 -> {
                bonusXu = 80;
                bonusGem = 50;
                bonusGold = 100_000_000L;
                // Thưởng 5 Thỏi vàng + 1 Hộp SKH
                Item tv = ItemService.gI().createNewItem((short) 457, 5);
                InventoryService.gI().addItemBag(player, tv);
                Item hop = ItemService.gI().createNewItem((short) 860, 1);
                InventoryService.gI().addItemBag(player, hop);
                specialReward = ", 5 Thỏi vàng, 1 Hộp SKH";
            }
            case 25 -> {
                bonusXu = 120;
                bonusGem = 80;
                bonusGold = 200_000_000L;
                // Thưởng 2 Mảnh bông tai
                Item mbt = ItemService.gI().createNewItem((short) 441, 2);
                InventoryService.gI().addItemBag(player, mbt);
                specialReward = ", 2 Mảnh bông tai Porata";
            }
            case 30 -> {
                bonusXu = 160;
                bonusGem = 120;
                bonusGold = 350_000_000L;
                // Thưởng 10 Thỏi vàng + 1 Sách TK2
                Item tv = ItemService.gI().createNewItem((short) 457, 10);
                InventoryService.gI().addItemBag(player, tv);
                Item stk = ItemService.gI().createNewItem((short) 456, 1);
                InventoryService.gI().addItemBag(player, stk);
                specialReward = ", 10 Thỏi vàng, 1 Sách TK2";
            }
            case 35 -> {
                bonusXu = 200;
                bonusGem = 160;
                bonusGold = 500_000_000L;
                // Thưởng 3 Hộp SKH
                Item hop = ItemService.gI().createNewItem((short) 860, 3);
                InventoryService.gI().addItemBag(player, hop);
                specialReward = ", 3 Hộp SKH";
            }
            case 40 -> {
                bonusXu = 250;
                bonusGem = 200;
                bonusGold = 750_000_000L;
                // Thưởng 20 Thỏi vàng + 5 Mảnh bông tai
                Item tv = ItemService.gI().createNewItem((short) 457, 20);
                InventoryService.gI().addItemBag(player, tv);
                Item mbt = ItemService.gI().createNewItem((short) 441, 5);
                InventoryService.gI().addItemBag(player, mbt);
                specialReward = ", 20 Thỏi vàng, 5 Mảnh bông tai";
            }
            case 45 -> {
                bonusXu = 300;
                bonusGem = 250;
                bonusGold = 1_000_000_000L;
                // Thưởng 5 Hộp SKH + 2 Sách TK2
                Item hop = ItemService.gI().createNewItem((short) 860, 5);
                InventoryService.gI().addItemBag(player, hop);
                Item stk = ItemService.gI().createNewItem((short) 456, 2);
                InventoryService.gI().addItemBag(player, stk);
                specialReward = ", 5 Hộp SKH, 2 Sách TK2";
            }
            case 50 -> {
                bonusXu = 500;
                bonusGem = 500;
                bonusGold = 2_000_000_000L;
                // JACKPOT: 50 Thỏi vàng + 10 Mảnh bông tai + 3 Sách TK2 + Pet Po
                Item tv = ItemService.gI().createNewItem((short) 457, 50);
                InventoryService.gI().addItemBag(player, tv);
                Item mbt = ItemService.gI().createNewItem((short) 441, 10);
                InventoryService.gI().addItemBag(player, mbt);
                Item stk = ItemService.gI().createNewItem((short) 456, 3);
                InventoryService.gI().addItemBag(player, stk);
                Item pet = ItemService.gI().createNewItem((short) 1667, 1);
                InventoryService.gI().addItemBag(player, pet);
                specialReward = ", 50 Thỏi vàng, 10 Mảnh BT, 3 Sách TK2, Pet Po";
                ServerNotify.gI().notify("★★★ " + player.name + " đã chinh phục ĐỈNH CAO Tháp PôPô tầng 50! ★★★");
            }
        }

        // Trao Xu NRO
        if (bonusXu > 0) {
            InventoryService.gI().addItemBag(player, ItemService.gI().createNewItem((short) 1705, bonusXu));
        }
        // Trao Ngọc
        if (bonusGem > 0) {
            player.inventory.gem += bonusGem;
        }
        // Trao Vàng
        if (bonusGold > 0) {
            player.inventory.gold += bonusGold;
        }

        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);

        Service.gI().sendThongBao(player,
                "★ THƯỞNG MỐC TẦNG " + floor + " ★\n"
                        + bonusXu + " Xu NRO, " + bonusGem + " ngọc, "
                        + Util.numberToMoney(bonusGold) + " vàng"
                        + specialReward);
    }

    // ============ CẤP BẬC THEO TẦNG ============
    private String getTierName(int floor) {
        if (floor >= 50) return "★ HUYỀN THOẠI ★";
        if (floor >= 40) return "Bậc Thầy";
        if (floor >= 30) return "Cao Thủ";
        if (floor >= 20) return "Tinh Anh";
        if (floor >= 10) return "Chiến Binh";
        if (floor >= 5) return "Tân Binh";
        return "Mới Bắt Đầu";
    }

    // ============ HIỂN THỊ BẢNG PHẦN THƯỞNG ============
    private void showReward(Player player, nro.models.npc.Npc npc) {
        npc.createOtherMenu(player, consts.ConstNpc.BASE_MENU,
                "|7|━━ PHẦN THƯỞNG THÁP PÔPÔ ━━\n\n"
                        + "|2|▶ Mỗi tầng:\n"
                        + "|8|• Xu NRO + Vàng + Ngọc\n"
                        + "|8|• Item ngẫu nhiên theo tầng\n\n"
                        + "|2|▶ Mốc đặc biệt (5→50):\n"
                        + "|8|• T5: 15 Xu + 5 ngọc + 10M vàng\n"
                        + "|8|• T10: 30 Xu + 15 ngọc + 3 Capsule\n"
                        + "|8|• T20: 80 Xu + 50 ngọc + 5 TV + SKH\n"
                        + "|8|• T30: 160 Xu + 120 ngọc + Sách TK2\n"
                        + "|8|• T40: 250 Xu + 200 ngọc + 20 TV\n"
                        + "|8|• T50: ★JACKPOT★ 500 Xu + 500 ngọc\n"
                        + "|8|  + 50 TV + 10 Mảnh BT + Pet Po\n\n"
                        + "|2|▶ Drop hiếm (T40+):\n"
                        + "|8|• 0.5% Pet Po, 1.5% Thú cưỡi",
                "Đã hiểu");
    }

    // ============ HƯỚNG DẪN ============
    private void showGuide(Player player, nro.models.npc.Npc npc) {
        npc.createOtherMenu(player, consts.ConstNpc.BASE_MENU,
                "|7|━━ HƯỚNG DẪN THÁP PÔPÔ ━━\n\n"
                        + "|2|▶ Cách chơi:\n"
                        + "|8|1) " + FREE_TICKET_PER_DAY + " lượt miễn phí mỗi ngày\n"
                        + "|8|2) Solo 1v1 với PôPô tại phòng riêng\n"
                        + "|8|3) Thắng trong 3 phút = mở tầng mới\n"
                        + "|8|4) Tầng càng cao PôPô càng mạnh\n\n"
                        + "|2|▶ Cấp bậc:\n"
                        + "|8|• T1-4: Mới Bắt Đầu\n"
                        + "|8|• T5-9: Tân Binh\n"
                        + "|8|• T10-19: Chiến Binh\n"
                        + "|8|• T20-29: Tinh Anh\n"
                        + "|8|• T30-39: Cao Thủ\n"
                        + "|8|• T40-49: Bậc Thầy\n"
                        + "|8|• T50: ★ HUYỀN THOẠI ★",
                "Đã hiểu");
    }

    private void showTop(Player player, nro.models.npc.Npc npc) {
        StringBuilder sb = new StringBuilder("|7|━━ TOP THÁP PÔPÔ ━━\n\n");
        jdbc.NDVResultSet rs = null;
        try {
            rs = DBConnecter.executeQuery("SELECT name, data_luyentap FROM player ORDER BY CAST(JSON_EXTRACT(data_luyentap, '$[13]') AS UNSIGNED) DESC, CAST(JSON_EXTRACT(data_luyentap, '$[14]') AS UNSIGNED) ASC LIMIT 10");
            int i = 1;
            while (rs.next()) {
                org.json.simple.JSONArray arr = (org.json.simple.JSONArray) org.json.simple.JSONValue.parse(rs.getString("data_luyentap"));
                int floor = arr != null && arr.size() > 13 ? Integer.parseInt(arr.get(13).toString()) : 0;
                long time = arr != null && arr.size() > 14 ? Long.parseLong(arr.get(14).toString()) : 0;
                String medal = switch (i) {
                    case 1 -> "|8|🥇 ";
                    case 2 -> "|2|🥈 ";
                    case 3 -> "|1|🥉 ";
                    default -> "|1|" + i + ". ";
                };
                sb.append(medal).append(rs.getString("name"))
                        .append(" - T").append(floor)
                        .append(" [").append(getTierName(floor)).append("]")
                        .append(" - ").append(time).append("s\n");
                i++;
            }
            if (i == 1) {
                sb.append("|1|Chưa có ai chinh phục. Bạn sẽ là người đầu tiên?\n");
            }
        } catch (Exception e) {
            sb.append("|1|Chưa có dữ liệu xếp hạng.\n");
        } finally {
            if (rs != null) {
                rs.dispose();
            }
        }
        npc.createOtherMenu(player, consts.ConstNpc.BASE_MENU, sb.toString(), "Đóng");
    }

    private void resetDaily(Player player) {
        if (player.popoTowerLastDay <= 0 || Util.isAfterMidnight(player.popoTowerLastDay)) {
            player.popoTowerTodayCount = 0;
            player.popoTowerLastDay = System.currentTimeMillis();
        }
    }

    private void clearChallenge(Player player) {
        player.isPopoTowerChallenge = false;
        player.popoTowerChallengeFloor = 0;
        player.popoTowerStartTime = 0;
    }

    private void saveProgress(Player player) {
        jdbc.daos.TraningDAO.updatePlayer(player);
    }
}
