package boss.boss_manifest.HungVuongEvent;

import boss.*;
import static boss.BossType.HUNGVUONG_EVENT;
import consts.ConstItem;
import consts.ConstPlayer;
import item.Item;
import java.util.ArrayList;
import java.util.List;
import map.ItemMap;
import nro.player.Player;
import nro.services.EffectSkillService;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.SkillService;
import services.func.ChangeMapService;
import utils.SkillUtil;
import utils.Util;

/**
 * Boss Voi Chín Ngà - Sự kiện Giỗ Tổ Hùng Vương
 * Drop: Ngà Voi (10-15) + điểm BXH
 */
public class VoiChinNga extends Boss {

    private long lastTimeMove;
    private int timeMove;
    private boolean isReward;
    private long lastTimeReward;

    public VoiChinNga() throws Exception {
        super(HUNGVUONG_EVENT, BossID.VOI_CHIN_NGA, true, false, BossesData.VOI_CHIN_NGA);
    }

    @Override
    public void reward(Player plKill) {
        this.playerReward = plKill;
        this.changeStatus(BossStatus.AFK);
    }

    @Override
    public void afk() {
        if (playerReward != null && playerReward.isPl() && !isReward && this.zone != null) {
            // Drop Ngà Voi 10-15
            ItemMap it = new ItemMap(this.zone, ConstItem.NGA_VOI, Util.nextInt(10, 15),
                    this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                    playerReward.id);
            Service.gI().dropItemMap(this.zone, it);

            // +1 điểm BXH
            pointBoss(playerReward);

            isReward = true;
            lastTimeReward = System.currentTimeMillis();
            this.chat("Ngà ta... gãy rồi sao...");
        }
        if (Util.canDoWithTime(lastTimeReward, 3000)) {
            this.leaveMap();
        }
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 1;
            }
            if (!piercing && damage > 2000000) {
                damage = Util.nextInt(1500000, 2000000);
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
    public void joinMap() {
        super.joinMap();
    }

    @Override
    public void active() {
        this.attack();
    }

    @Override
    public Player getPlayerAttack() {
        List<Player> plNotVoHinh = new ArrayList();
        for (Player pl : this.zone.getNotBosses()) {
            if (pl.effectSkin == null || !pl.effectSkin.isVoHinh) {
                plNotVoHinh.add(pl);
            }
        }
        if (!plNotVoHinh.isEmpty()) {
            return plNotVoHinh.get(Util.nextInt(0, plNotVoHinh.size() - 1));
        }
        return null;
    }

    @Override
    public void attack() {
        if (this.effectSkill.isCharging) return;
        if (Util.canDoWithTime(this.lastTimeAttack, 100)) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) return;
                this.playerSkill.skillSelect = this.playerSkill.skills
                        .get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                int dis = Util.getDistance(this, pl);
                if (dis > 450) {
                    move(pl.location.x - 24, pl.location.y);
                } else if (dis > 100) {
                    int dir = (this.location.x - pl.location.x < 0 ? 1 : -1);
                    int move = Util.nextInt(50, 100);
                    move(this.location.x + (dir == 1 ? move : -move), pl.location.y);
                } else {
                    if (pl.isPl()) {
                        this.nPoint.dame = pl.nPoint.hpMax / 30;
                    } else {
                        this.nPoint.dame = 10000;
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.isReward = false;
        this.playerReward = null;
        this.changeStatus(BossStatus.REST);
    }
}
