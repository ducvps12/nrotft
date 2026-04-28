package models.ClanBoss;

import map.Zone;
import nro.player.Player;
import nro.server.Maintenance;
import nro.services.MapService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Functions;
import utils.TimeUtil;
import utils.Util;

public class ClanBoss implements Runnable {

    public static final byte HOUR_OPEN = 21;
    public static final byte MIN_OPEN = 0;
    public static final byte SECOND_OPEN = 0;

    public static final byte HOUR_CLOSE = 23;
    public static final byte MIN_CLOSE = 0;
    public static final byte SECOND_CLOSE = 0;

    private final Zone zone;

    public ClanBoss(Zone zone) {
        this.zone = zone;
        start();
    }

    private void start() {
        Thread.ofVirtual()
                .name("ClanBoss-Zone-" + zone.zoneId)
                .start(this);
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long start = System.currentTimeMillis();
                update();
                long sleep = 1000 - (System.currentTimeMillis() - start);
                if (sleep > 0) {
                    Functions.sleep(sleep);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void update() {
        for (int i = zone.getPlayers().size() - 1; i >= 0; i--) {
            try {
                updatePlayer(zone.getPlayers().get(i));
            } catch (Exception ignored) {
            }
        }
    }

    private void updatePlayer(Player player) {
        if (player == null
                || player.zone == null
                || !MapService.gI().isMapClanBoss(player.zone.map.mapId)) {
            return;
        }

        if (!TimeUtil.isClanBossOpen()) {
            kick(player);
        }
    }

    private void kick(Player player) {
        if (player.cFlag > 0) {
            Service.gI().changeFlag(player, Util.nextInt(1, 7));
        }

        Service.gI().sendThongBao(
                player,
                "Map săn Boss Bang Hội đã kết thúc");

        ChangeMapService.gI().changeMapBySpaceShip(
                player,
                ChangeMapService.getSpaceStationMapId(player),
                -1,
                250);
    }
}
