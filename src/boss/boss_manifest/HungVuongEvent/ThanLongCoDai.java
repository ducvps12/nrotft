package boss.boss_manifest.HungVuongEvent;

import boss.*;
import static boss.BossType.HUNGVUONG_EVENT;
import consts.ConstPlayer;
import item.Item;
import java.util.ArrayList;
import java.util.List;
import map.ItemMap;
import nro.player.Player;
import nro.services.EffectSkillService;
import nro.services.Service;
import nro.services.SkillService;
import services.func.ChangeMapService;
import utils.Util;

/**
 * Boss ẩn Thần Long Cổ Đại - Sự kiện Thần Thú Cổ Đại
 * Được triệu hồi khi player ghép đủ 3 Linh Phù (Voi, Gà, Ngựa)
 * Drop: Item hiếm (Capsule dây chuyền, Đá Xanh Lam, Mảnh BTC3, Thỏi vàng)
 */
public class ThanLongCoDai extends Boss {

    private boolean isReward;
    private long lastTimeReward;

    public ThanLongCoDai() throws Exception {
        super(HUNGVUONG_EVENT, BossID.THAN_LONG_CO_DAI, false, false, BossesData.THAN_LONG_CO_DAI);
    }

    @Override
    public void reward(Player plKill) {
        this.playerReward = plKill;
        this.changeStatus(BossStatus.AFK);
    }

    @Override
    public void afk() {
        if (playerReward != null && playerReward.isPl() && !isReward && this.zone != null) {
            int x = this.location.x;
            int y = this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24);

            // Drop rewards
            // 1. Thỏi vàng x3-5
            Service.gI().dropItemMap(this.zone,
                new ItemMap(this.zone, 457, Util.nextInt(3, 5), x, y, playerReward.id));

            // 2. Mảnh vỡ BTC3 (1855) x5-10
            Service.gI().dropItemMap(this.zone,
                new ItemMap(this.zone, 1855, Util.nextInt(5, 10), x + 10, y, playerReward.id));

            // 3. Đá Xanh Lam (935) x1-2
            Service.gI().dropItemMap(this.zone,
                new ItemMap(this.zone, 935, Util.nextInt(1, 2), x - 10, y, playerReward.id));

            // 4. Capsule dây chuyền (192) — 30%
            if (Util.isTrue(30, 100)) {
                Service.gI().dropItemMap(this.zone,
                    new ItemMap(this.zone, 192, 1, x + 20, y, playerReward.id));
            }

            // 5. Sách TK (456) — 10%
            if (Util.isTrue(10, 100)) {
                Service.gI().dropItemMap(this.zone,
                    new ItemMap(this.zone, 456, 1, x - 20, y, playerReward.id));
            }

            // 6. Vàng 500k-1M
            Service.gI().dropItemMap(this.zone,
                new ItemMap(this.zone, 190, Util.nextInt(500000, 1000000), x, y, playerReward.id));

            // +5 điểm BXH
            for (int i = 0; i < 5; i++) {
                pointBoss(playerReward);
            }

            isReward = true;
            lastTimeReward = System.currentTimeMillis();
            this.chat("Cac nguoi... that loi hai...");
        }
        if (Util.canDoWithTime(lastTimeReward, 3000)) {
            this.leaveMap();
        }
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xi hut!");
                return 0;
            }
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 1;
            }
            if (!piercing && damage > 5000000) {
                damage = Util.nextInt(3000000, 5000000);
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return damage;
        }
        return 0;
    }

    @Override
    public void joinMap() {
        super.joinMap();
        this.chat("Ta la Than Long Co Dai! Ai dam day ta thuc giac!");
    }

    @Override
    public void active() {
        this.attack();
    }

    @Override
    public Player getPlayerAttack() {
        List<Player> plNotVoHinh = new ArrayList<>();
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
                    int moveD = Util.nextInt(50, 100);
                    move(this.location.x + (dir == 1 ? moveD : -moveD), pl.location.y);
                } else {
                    if (pl.isPl()) {
                        this.nPoint.dame = pl.nPoint.hpMax / 15; // Mạnh hơn boss thường
                    } else {
                        this.nPoint.dame = 50000;
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
