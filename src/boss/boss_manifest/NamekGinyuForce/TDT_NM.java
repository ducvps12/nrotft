package boss.boss_manifest.NamekGinyuForce;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import boss.BossesData;
import map.ItemMap;
import nro.player.Player;
import nro.services.Service;
import utils.Util;

public class TDT_NM extends Boss {

    private long st;

    public TDT_NM() throws Exception {
        super(BossID.TIEU_DOI_TRUONG_NM, false, true, BossesData.TIEU_DOI_TRUONG_NM);
    }

    @Override
    public void moveTo(int x, int y) {
        if (this.currentLevel == 1) {
            return;
        }
        super.moveTo(x, y);
    }

    @Override
    public void reward(Player plKill) {
        if (this.currentLevel == 1) {
            return;
        }
        // Drop hồng ngọc (ruby, item 861) 1-3 viên
        for (int i = 0; i < Util.nextInt(1, 3); i++) {
            Service.gI().dropItemMap(this.zone,
                    new ItemMap(zone, 861, Util.nextInt(1, 2), this.location.x + i * Util.nextInt(-30, 30),
                            this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id));
        }
        // Drop thỏi vàng (item 457) 2-5 thỏi
        for (int i = 0; i < Util.nextInt(1, 2); i++) {
            Service.gI().dropItemMap(this.zone,
                    new ItemMap(zone, 457, Util.nextInt(2, 5), this.location.x + i * Util.nextInt(-40, 40),
                            this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id));
        }
    }

    @Override
    protected void notifyJoinMap() {
        if (this.currentLevel == 1) {
            return;
        }
        super.notifyJoinMap();
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void doneChatS() {
        this.changeStatus(BossStatus.AFK);
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }
}
