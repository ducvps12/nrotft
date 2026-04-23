package event.event_manifest;

/**
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import boss.BossID;
import consts.ConstNpc;
import event.Event;

public class Halloween extends Event {

    @Override
    public void npc() {
        createNpc(5, 82, 231, 288);
    }

    @Override
    public void boss() {
        createBoss(BossID.BIMA, 10);
        createBoss(BossID.MATROI, 10);
        createBoss(BossID.BOXUONG, 10);
        createBoss(BossID.DOI, 10);
    }
}
