package models.BlackBallWar;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import utils.Functions;
import jdbc.daos.NDVSqlFetcher;
import jdbc.daos.PlayerDAO;
import nro.player.Player;
import nro.services.MapService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.TimeUtil;
import utils.Util;

import lombok.Data;
import map.Zone;
import nro.server.Client;
import nro.server.Maintenance;

@Data
public class BlackBallWar implements Runnable {

    public static final int TIME_CAN_PICK_BLACK_BALL_AFTER_DROP = 5000;

    public static final byte X3 = 3;
    public static final byte X5 = 5;
    public static final byte X7 = 7;

    public static final int COST_X3 = 100000000;
    public static final int COST_X5 = 300000000;
    public static final int COST_X7 = 500000000;

    public static final byte HOUR_OPEN = 20;
    public static final byte MIN_OPEN = 0;
    public static final byte SECOND_OPEN = 0;

    public static final byte HOUR_CAN_PICK_DB = 20;
    public static final byte MIN_CAN_PICK_DB = 30;
    public static final byte SECOND_CAN_PICK_DB = 0;

    public static final byte HOUR_CLOSE = 21;
    public static final byte MIN_CLOSE = 0;
    public static final byte SECOND_CLOSE = 0;

    public static final int AVAILABLE = 4; // khu blackball
    private static final int TIME_WIN = 30000;

    public Zone zone;

    public BlackBallWar(Zone zone) {
        this.zone = zone;
        start();
    }

    private void start() {
        Thread.ofVirtual()
                .name("Update Black Ball War Map " + zone.map.mapName + " Zone " + zone.zoneId)
                .start(this);
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long startTime = System.currentTimeMillis();
                update();
                long elapsedTime = System.currentTimeMillis() - startTime;
                long sleepTime = 1000 - elapsedTime;
                if (sleepTime > 0) {
                    Functions.sleep(sleepTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        if (!TimeUtil.isBlackBallWarOpen()) {
            zone.finishBlackBallWar = false;
        }
        for (int i = zone.getNumOfPlayers() - 1; i >= 0; i--) {
            try {
                updatePlayer(zone.getPlayers().get(i));
            } catch (Exception e) {
            }
        }
    }

    public void updatePlayer(Player player) {
        if (player.zone == null || !MapService.gI().isMapBlackBallWar(player.zone.map.mapId)) {
            return;
        }
        if (!TimeUtil.isBlackBallWarOpen()) {
            kickOutOfMap(player);
            return;
        }

        if (player.iDMark.isHoldBlackBall()) {
            if (Util.canDoWithTime(player.iDMark.getLastTimeHoldBlackBall(), TIME_WIN)) {
                win(player);
            } else if (Util.canDoWithTime(player.iDMark.getLastTimeNotifyTimeHoldBlackBall(), 10000)) {
                Service.gI().sendThongBao(player, "Cố giữ ngọc thêm "
                        + TimeUtil.getSecondLeft(player.iDMark.getLastTimeHoldBlackBall(), TIME_WIN / 1000)
                        + " giây nữa sẽ thắng");
                player.iDMark.setLastTimeNotifyTimeHoldBlackBall(System.currentTimeMillis());
            }
        }
    }

    private void win(Player player) {
        if (player == null || player.zone == null) {
            return;
        }

        player.zone.finishBlackBallWar = true;

        int star = player.iDMark.getTempIdBlackBallHold() - 371;

        if (star < 1 || star > 7) {
            return;
        }

        // ===== NGƯỜI WIN (ONLINE) =====
        player.rewardBlackBall.reward((byte) star);
        PlayerDAO.updatePlayer(player);

        Service.gI().sendThongBao(player,
                "Chúc mừng bạn đã dành được Ngọc rồng " + star + " sao đen cho bang");

        // ===== CLAN MEMBERS =====
        if (player.clan != null && player.clan.members != null) {

            player.clan.members.forEach(m -> {

                // Bỏ qua chính người vừa win (đã xử lý phía trên)
                if (m.id == player.id) {
                    return;
                }

                Player online = Client.gI().getPlayer(m.id);

                if (online != null) {
                    // ===== ONLINE =====
                    online.rewardBlackBall.reward((byte) star);
                    PlayerDAO.updatePlayer(online);
                } else {
                    // ===== OFFLINE =====
                    PlayerDAO.updateBlackBallReward(m.id, star);
                }
            });
        }

        kickAllPlayersOutOfMap(player.zone);
    }

    private void kickOutOfMap(Player player) {
        if (player.cFlag == 8) {
            Service.gI().changeFlag(player, Util.nextInt(1, 7));
        }

        Service.gI().sendThongBao(player, "Trò chơi tìm ngọc hôm nay đã kết thúc, hẹn gặp lại vào 20h ngày mai");

        ChangeMapService.gI().changeMapBySpaceShip(player, ChangeMapService.getSpaceStationMapId(player), -1, 250);
    }

    private void kickAllPlayersOutOfMap(Zone zone) {
        for (int i = zone.getPlayers().size() - 1; i >= 0; i--) {
            if (i < zone.getPlayers().size()) {
                Player pl = zone.getPlayers().get(i);
                kickOutOfMap(pl);
            }
        }
    }

}
