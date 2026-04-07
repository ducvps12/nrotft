package models.ClanBattle;

import clan.Clan;
import java.util.List;
import map.Zone;
import nro.player.Player;
import nro.services.Service;
import services.func.ChangeMapService;
import nro.server.Client;
import nro.server.Maintenance;
import utils.Functions;
import utils.Util;

public class ClanBattle implements Runnable {
    private Clan clan1;
    private Clan clan2;
    private Zone zone;
    private int timeDown = 60;
    private boolean isCompeting;

    public ClanBattle(Clan c1, Clan c2) {
        this.clan1 = c1;
        this.clan2 = c2;
        this.isCompeting = true;
        init();
    }

    // THÊM PHƯƠNG THỨC NÀY ĐỂ HẾT LỖI
    public Zone getZone() {
        return this.zone;
    }

    private void init() {
        // Lấy Map 153 Khu 0 (Map Đại Hội Võ Thuật)
        this.zone = ChangeMapService.gI().getZoneJoinByMapIdAndZoneId(null, 145, 0);
        if (this.zone == null) {
            return;
        }

        summonClan(clan1, 1); // Cờ Xanh
        summonClan(clan2, 2); // Cờ Đỏ

        Thread.ofVirtual().name("ClanBattle-" + clan1.name).start(this);
    }

    private void summonClan(Clan clan, int flag) {
        for (Player p : Client.gI().getPlayers()) {
            if (p != null && p.clan != null && p.clan.id == clan.id) {
                p.cFlag = (byte) flag;
                Service.gI().changeFlag(p, flag);
                ChangeMapService.gI().changeMap(p, zone, Util.nextInt(200, 600), 300);
            }
        }
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && isCompeting) {
            try {
                update();
                Functions.sleep(1000);
            } catch (Exception e) {
                isCompeting = false;
            }
        }
    }

    private void update() {
        if (!isCompeting) return;
        
        timeDown--;
        List<Player> players = zone.getPlayers();
        
        int count1 = (int) players.stream()
                .filter(p -> p != null && p.clan != null && p.clan.id == clan1.id && !p.isDie())
                .count();
        int count2 = (int) players.stream()
                .filter(p -> p != null && p.clan != null && p.clan.id == clan2.id && !p.isDie())
                .count();

        if (count1 == 0 && count2 > 0) {
            finish(clan2, "Bang " + clan1.name + " đã tử trận hết! Bang " + clan2.name + " chiến thắng!");
            return;
        }
        if (count2 == 0 && count1 > 0) {
            finish(clan1, "Bang " + clan2.name + " đã tử trận hết! Bang " + clan1.name + " chiến thắng!");
            return;
        }
        if (count1 == 0 && count2 == 0) {
            finish(null, "Cả hai bang đã cùng tử trận! Kết quả Hòa.");
            return;
        }

        if (timeDown <= 0) {
            if (count1 > count2) finish(clan1, "Hết giờ! Bang " + clan1.name + " thắng quân số.");
            else if (count2 > count1) finish(clan2, "Hết giờ! Bang " + clan2.name + " thắng quân số.");
            else finish(null, "Kết quả Hòa!");
        }
    }

    private void finish(Clan winner, String reason) {
        if (!isCompeting) return;
        this.isCompeting = false;
        
        List<Player> players = zone.getPlayers();
        for (int i = players.size() - 1; i >= 0; i--) {
            Player p = players.get(i);
            if (p != null) {
                Service.gI().sendThongBao(p, reason);
                p.cFlag = 0;
                Service.gI().changeFlag(p, 0);
                if (p.isDie()) {
                    Service.gI().hsChar(p, p.nPoint.hpMax, p.nPoint.mpMax);
                }
                ChangeMapService.gI().changeMapInYard(p, p.gender + 21, -1, 300);
            }
        }
        // Xóa trận đấu khỏi Manager khi kết thúc
        ClanBattleManager.gI().removeBattle(this);
    }
}