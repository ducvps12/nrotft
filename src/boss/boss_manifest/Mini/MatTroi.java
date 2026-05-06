package boss.boss_manifest.Mini;

import boss.Boss;
import boss.BossData;
import boss.BossID;
import boss.BossStatus;
import static boss.BossType.MATTROI;
import consts.ConstPlayer;
import consts.ConstTaskBadges;
import item.Item.ItemOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import map.ItemMap;
import nro.player.Player;
import nro.server.Client;
import nro.services.ItemTimeService;
import nro.services.Service;
import nro.services.SkillService;
import services.func.ChangeMapService;
import skill.Skill;
import task.Badges.BadgesTaskService;
import utils.Util;

/**
 * Boss Mặt Trời Mùa Hè
 * ═══════════════════════════
 * - HP: 500, mỗi hit chỉ trừ 1 HP → cần 500 hit để hạ
 * - Spawn tại map ngẫu nhiên (các làng), respawn mỗi 10 phút
 * - Hiệu ứng Bỏng Nhiệt: gây sát thương diện rộng cho player gần
 * - Drop: Cờ Mặt Trời Mùa Hè (1562), Đá Ngũ Sắc (674), Thỏi Vàng (457)
 * - Tồn tại 15 phút trên map trước khi biến mất
 */
public class MatTroi extends Boss {

    private final Map<Long, Long> globalEffectTimers = new ConcurrentHashMap<>();
    private long st;

    public MatTroi() throws Exception {
        super(MATTROI, BossID.Virut, new BossData(
                "Mat Troi Mua He " + Util.nextInt(1, 49),
                ConstPlayer.TRAI_DAT,
                new short[]{1501, 1502, 1503, -1, -1, -1},
                10,
                new long[]{500},
                new int[]{5, 7, 0, 14},
                new int[][]{{Skill.DRAGON, 7, 1000}},
                new String[]{
                    "Nong qua roi!",
                    "Tan chay nao!"
                },
                new String[]{
                    "Oi, ta bi ha roi...",
                    "Mat troi se quay lai!"
                },
                new String[]{},
                600));
    }

    @Override
    public void die(Player plKill) {
        this.reward(plKill);
        this.changeStatus(BossStatus.DIE);
        Service.gI().sendThongBaoAllPlayer(
                plKill.name + " da ha guc Mat Troi Mua He! Nhan thuong dac biet!");
    }

    private void applyEffect(Player player) {
        long effectEndTime = System.currentTimeMillis() + 30000;
        globalEffectTimers.put(player.id, effectEndTime);
        ItemTimeService.gI().sendItemTime(player, 12953, 60);
        this.chat(player.name + " bi bong nhiet!");
    }

    private void checkGlobalEffects() {
        long currentTime = System.currentTimeMillis();

        globalEffectTimers.forEach((playerId, effectEndTime) -> {
            if (currentTime >= effectEndTime) {
                Player player = Client.gI().getPlayer(playerId);
                if (player != null) {
                    if (!player.isDie()) {
                        if (Util.isTrue(60, 100)) {
                            player.injured(null, player.nPoint.hp, true, false);
                        }
                    }
                }
                globalEffectTimers.remove(playerId);
            }
        });
    }

    private void updateOdo() {
        try {
            if (Util.isTrue(30, 100)) {
                List<Player> playersMap = this.zone.getNotBosses();
                for (Player pl : playersMap) {
                    if (pl != null && pl.nPoint != null && !this.equals(pl) && !pl.isBoss && !pl.isDie()
                            && Util.getDistance(this, pl) <= 200) {
                        applyEffect(pl);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 3000) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = this.getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }

                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));

                if (Util.getDistance(this, pl) <= 40) {
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                    if (!globalEffectTimers.containsKey(pl.id)
                            || System.currentTimeMillis() >= globalEffectTimers.get(pl.id)) {
                        this.updateOdo();
                    }
                } else {
                    this.moveToPlayer(pl);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void reward(Player plKill) {
        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.KOL, 1);
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

        // +5 điểm sự kiện cho người hạ boss
        plKill.event.addDiemSuKien(5);

        // 50% — Drop Cờ Mặt Trời Mùa Hè (1562) — chỉ số tốt
        if (Util.isTrue(50, 100)) {
            ItemMap itemMap = new ItemMap(this.zone, 1562, 1, x, y, plKill.id);
            itemMap.options.add(new ItemOption(50, Util.nextInt(7, 12)));
            itemMap.options.add(new ItemOption(77, Util.nextInt(7, 12)));
            itemMap.options.add(new ItemOption(103, Util.nextInt(7, 12)));
            itemMap.options.add(new ItemOption(30, 0));
            itemMap.options.add(new ItemOption(93, Util.nextInt(3, 7)));
            Service.gI().dropItemMap(this.zone, itemMap);
        }

        // 30% — Drop Thỏi Vàng (457) x1-5
        if (Util.isTrue(30, 100)) {
            int qty = Util.nextInt(1, 5);
            ItemMap goldBar = new ItemMap(this.zone, 457, qty, x + Util.nextInt(-10, 10), y, plKill.id);
            Service.gI().dropItemMap(this.zone, goldBar);
        }

        // 20% — Drop Đá Ngũ Sắc (674) x3-10
        if (Util.isTrue(20, 100)) {
            int qty = Util.nextInt(3, 10);
            ItemMap daNguSac = new ItemMap(this.zone, 674, qty, x + Util.nextInt(-10, 10), y, plKill.id);
            Service.gI().dropItemMap(this.zone, daNguSac);
        }

        // 10% — Drop Nước Đá (1613) x10-30
        if (Util.isTrue(10, 100)) {
            int qty = Util.nextInt(10, 30);
            ItemMap nuocDa = new ItemMap(this.zone, 1613, qty, x + Util.nextInt(-10, 10), y, plKill.id);
            Service.gI().dropItemMap(this.zone, nuocDa);
        }

        // 10% — Drop Khúc Mía (1612) x10-30
        if (Util.isTrue(10, 100)) {
            int qty = Util.nextInt(10, 30);
            ItemMap khucMia = new ItemMap(this.zone, 1612, qty, x + Util.nextInt(-10, 10), y, plKill.id);
            Service.gI().dropItemMap(this.zone, khucMia);
        }
    }

    @Override
    public void joinMap() {
        this.name = "Mat Troi Mua He " + Util.nextInt(1, 49);
        this.nPoint.hpMax = 500;
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dameg = 1;
        this.joinMap2();
        st = System.currentTimeMillis();
    }

    public void joinMap2() {
        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }
        if (this.zone != null) {
            try {
                int zoneid = 0;
                this.zone = this.zone.map.zones.get(zoneid);
                ChangeMapService.gI().changeMap(this, this.zone, -1, -1);

                this.changeStatus(BossStatus.CHAT_S);
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
        // 15 phút tồn tại trên map
        if (Util.canDoWithTime(st, 900000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
            this.checkGlobalEffects();
        }
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            int actualDamage = 1;
            this.nPoint.subHP(actualDamage);

            if (this.nPoint.hp <= 0) {
                this.die(plAtt);
            }

            return actualDamage;
        } else {
            return 0;
        }
    }
}
