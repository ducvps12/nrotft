package event.event_manifest;

/**
 *
 *  Box ZALO:https://zalo.me/g/hfaysi616
 *  sdt zalo: 0372875491
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import boss.BossID;
import event.Event;

public class Default extends Event {

    @Override
    public void boss() {
        createBoss(BossID.SUPER_BROLY, 5);
    }

}
