package boss.boss_manifest.DaiTuongBroly;

/*
 *
 *
 * @author NGOJC
 */
import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import boss.BossesData;
import map.ItemMap;
import nro.player.Player;
import nro.server.Manager;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

public class DaiTuongBroly extends Boss {

    private long st;

    public DaiTuongBroly() throws Exception {
        super(BossID.DAITUONGBROLY, true, true, BossesData.DAITUONGBROLY);
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void reward(Player plKill) {
        this.pointBoss(plKill);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (plAtt == null) {
            return 0;
        }
        if (!this.isDie()) {

            if (damage > 20_000_000) {
                damage = 20_000_000;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return damage;
        } else {
            return 0;
        }
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
        // if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
        // st = System.currentTimeMillis();
        // }
    }
}
