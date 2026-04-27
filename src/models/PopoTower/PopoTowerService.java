package models.PopoTower;

import boss.BossID;
import item.Item;
import jdbc.DBConnecter;
import map.Zone;
import models.Training.TrainingService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.MapService;
import nro.services.PlayerService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Util;

public class PopoTowerService {

    private static final int MAX_FLOOR = 20;
    private static final int FREE_TICKET_PER_DAY = 5;
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
        npc.createOtherMenu(player, consts.ConstNpc.MENU_POPO_TOWER,
                "Tháp Huấn Luyện PôPô\n"
                        + "Tầng đã chinh phục: " + player.popoTowerFloor + " / " + MAX_FLOOR + "\n"
                        + "Lượt hôm nay: " + Math.max(0, FREE_TICKET_PER_DAY - player.popoTowerTodayCount) + " / " + FREE_TICKET_PER_DAY + "\n"
                        + "Vượt từng tầng sẽ nhận Xu NRO, vàng và mở mốc thưởng đặc biệt.",
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
            Service.gI().sendThongBao(player, "Bạn đã chinh phục tối đa Tháp PôPô");
            return;
        }
        if (player.popoTowerTodayCount >= FREE_TICKET_PER_DAY) {
            Service.gI().sendThongBao(player, "Hôm nay đã hết lượt thách đấu PôPô");
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
        Service.gI().sendThongBao(player, "Bắt đầu Tháp PôPô tầng " + player.popoTowerChallengeFloor
                + ". Hãy đánh bại PôPô trong 3 phút!");
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
        int xu = 10 + floor * 5;
        long gold = floor * 2_000_000L;
        Item xuNro = ItemService.gI().createNewItem((short) 1705, xu);
        InventoryService.gI().addItemBag(player, xuNro);
        InventoryService.gI().sendItemBag(player);
        player.inventory.gold += gold;
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().sendThongBao(player, "Vượt tầng " + floor + " Tháp PôPô! Nhận " + xu
                + " Xu NRO và " + Util.numberToMoney(gold) + " vàng.");
        if (floor == 5 || floor == 10 || floor == 15 || floor == 20) {
            Service.gI().sendThongBaoAllPlayer("Chúc mừng " + player.name + " đã chinh phục tầng "
                    + floor + " Tháp Huấn Luyện PôPô!");
            rewardMilestone(player, floor);
        }
        saveProgress(player);
        clearChallenge(player);
    }

    public void lose(Player player) {
        if (player != null && player.isPopoTowerChallenge) {
            Service.gI().sendThongBao(player, "Thử thách PôPô thất bại, hãy mạnh hơn rồi quay lại!");
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

    private void rewardMilestone(Player player, int floor) {
        int bonusXu = switch (floor) {
            case 5 -> 50;
            case 10 -> 120;
            case 15 -> 250;
            case 20 -> 500;
            default -> 0;
        };
        if (bonusXu > 0) {
            InventoryService.gI().addItemBag(player, ItemService.gI().createNewItem((short) 1705, bonusXu));
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Thưởng mốc tầng " + floor + ": thêm " + bonusXu + " Xu NRO");
        }
    }

    private void showReward(Player player, nro.models.npc.Npc npc) {
        npc.createOtherMenu(player, consts.ConstNpc.BASE_MENU,
                "Phần thưởng Tháp PôPô:\n"
                        + "Mỗi tầng: Xu NRO + vàng theo tầng.\n"
                        + "Mốc 5/10/15/20: thưởng Xu NRO lớn và thông báo toàn server.\n"
                        + "Xu NRO có thể dùng để Bón Xu cây Đậu thần, đổi shop sự kiện hoặc tích lũy.",
                "Đã hiểu");
    }

    private void showGuide(Player player, nro.models.npc.Npc npc) {
        npc.createOtherMenu(player, consts.ConstNpc.BASE_MENU,
                "Luật Tháp Huấn Luyện PôPô:\n"
                        + "1) Mỗi ngày có 5 lượt miễn phí.\n"
                        + "2) Mỗi lần vào sẽ solo 1-1 với PôPô tại phòng riêng.\n"
                        + "3) Có 3 phút để hạ PôPô. Thắng mở tầng tiếp theo.\n"
                        + "4) Tầng càng cao PôPô càng mạnh, phần thưởng càng nhiều.",
                "Đã hiểu");
    }

    private void showTop(Player player, nro.models.npc.Npc npc) {
        StringBuilder sb = new StringBuilder("Top Tháp PôPô\n");
        jdbc.NDVResultSet rs = null;
        try {
            rs = DBConnecter.executeQuery("SELECT name, data_luyentap FROM player ORDER BY CAST(JSON_EXTRACT(data_luyentap, '$[13]') AS UNSIGNED) DESC, CAST(JSON_EXTRACT(data_luyentap, '$[14]') AS UNSIGNED) ASC LIMIT 10");
            int i = 1;
            while (rs.next()) {
                org.json.simple.JSONArray arr = (org.json.simple.JSONArray) org.json.simple.JSONValue.parse(rs.getString("data_luyentap"));
                int floor = arr != null && arr.size() > 13 ? Integer.parseInt(arr.get(13).toString()) : 0;
                long time = arr != null && arr.size() > 14 ? Long.parseLong(arr.get(14).toString()) : 0;
                sb.append(i++).append(". ").append(rs.getString("name")).append(" - tầng ").append(floor)
                        .append(" - ").append(time).append("s\n");
            }
        } catch (Exception e) {
            sb.append("Chưa có dữ liệu xếp hạng hoặc database chưa hỗ trợ JSON_EXTRACT.");
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
