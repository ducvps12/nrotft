package models.ClanBattle;

import clan.Clan;
import java.util.ArrayList;
import java.util.List;
import nro.server.Maintenance;
import map.Zone;
import utils.Functions;

public class ClanBattleManager implements Runnable {
    private static ClanBattleManager instance;
    private final List<Clan> waitList = new ArrayList<>();
    // Danh sách các trận đấu đang diễn ra
    private final List<ClanBattle> listBattles = new ArrayList<>();

    public static ClanBattleManager gI() {
        if (instance == null) {
            instance = new ClanBattleManager();
        }
        return instance;
    }

    public void addWaitList(Clan clan) {
        if (!waitList.contains(clan)) {
            waitList.add(clan);
        }
    }

    // Thêm trận đấu vào danh sách quản lý
    public void addBattle(ClanBattle battle) {
        synchronized (listBattles) {
            this.listBattles.add(battle);
        }
    }

    // Xóa trận đấu khi kết thúc (Hàm bạn đang thiếu)
    public void removeBattle(ClanBattle battle) {
        synchronized (listBattles) {
            this.listBattles.remove(battle);
        }
    }

    // Tìm trận đấu theo Zone (Dùng để check thắng thua khi player die)
    public ClanBattle getBattleByZone(Zone zone) {
        synchronized (listBattles) {
            for (ClanBattle battle : listBattles) {
                if (battle.getZone().equals(zone)) {
                    return battle;
                }
            }
        }
        return null;
    }

    public List<Clan> getWaitList() {
        return waitList;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            long st = System.currentTimeMillis();
            try {
                if (waitList.size() >= 2) {
                    Clan c1 = waitList.remove(0);
                    Clan c2 = waitList.remove(0);
                    
                    // Khởi tạo trận đấu
                    ClanBattle battle = new ClanBattle(c1, c2);
                    // Đưa vào danh sách quản lý
                    addBattle(battle);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Nghỉ 1 giây rồi quét tiếp
            Functions.sleep(Math.max(100, 1000 - (System.currentTimeMillis() - st)));
        }
    }
}