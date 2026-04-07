package boss.boss_manifest.Cooler;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/hfaysi616
 *  sdt zalo: 0372875491
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import boss.Boss;
import boss.BossID;
import boss.BossesData;
import consts.ConstTaskBadges;
import item.Item;
import map.ItemMap;
import nro.player.Player;
import nro.services.EffectSkillService;
import nro.services.Service;
import utils.Util;

import java.util.Random;
import nro.server.Manager;
import nro.services.TaskService;
import task.Badges.BadgesTaskService;

public class Cooler extends Boss {

    private long st;

    public Cooler() throws Exception {
        super(BossID.COOLER, BossesData.COOLER, BossesData.COOLER_2);
    }

    @Override
    public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);

        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.TRUM_SAN_BOSS, 1);

        // --- Rơi thêm đồ từ Panel ---
        String customItems = Manager.BOSS_REWARD_PANEL.get((int) this.id);
        if (customItems != null && !customItems.isEmpty()) {
            String[] entries = customItems.split(","); // Tách các phần tử
            for (String entry : entries) {
                try {
                    String[] parts = entry.trim().split("-");
                    int itemId = Integer.parseInt(parts[0]);
                    int quantity = 1; // Mặc định là 1 nếu không nhập số lượng

                    if (parts.length > 1) {
                        quantity = Integer.parseInt(parts[1]);
                    }

                    if (Util.isTrue(30, 100)) {
                        ItemMap it = new ItemMap(this.zone, itemId, quantity, this.location.x + Util.nextInt(-10, 10),
                                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
                        Service.gI().dropItemMap(this.zone, it);
                    }
                } catch (Exception e) {
                    // Tránh lỗi khi nhập sai định dạng trong Panel (ví dụ nhập chữ hoặc thiếu dấu
                    // -)
                    System.err.println("Lỗi cấu hình vật phẩm Boss ID " + this.id + ": " + entry);
                }
            }
        }
        int[] itemDos = new int[] { 233, 237, 241, 245, 249, 253, 257, 261, 265, 269, 273, 277, 281 };
        int[] itemtime = new int[] { 381, 382, 383, 384, 385 };
        int randomDo = new Random().nextInt(itemDos.length);
        int randomitem = new Random().nextInt(itemtime.length);
        ItemMap it = new ItemMap(this.zone, 702, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                this.location.y - 24), plKill.id);
        it.options.add(new Item.ItemOption(93, 30));
        Service.gI().dropItemMap(this.zone, it);
        if (Util.isTrue(20, 100)) {
            if (Util.isTrue(1, 5)) {
                Service.gI().dropItemMap(this.zone,
                        Util.RaitiDoc12(zone, 281, 1, this.location.x, this.location.y, plKill.id));
                return;
            }
            Service.gI().dropItemMap(this.zone,
                    Util.RaitiDoc12(zone, itemDos[randomDo], 1, this.location.x, this.location.y, plKill.id));
        } else if (Util.isTrue(20, 100)) {
            Service.gI().dropItemMap(this.zone, new ItemMap(zone, itemtime[randomitem], 1, this.location.x,
                    zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id));
        }
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (piercing) {
                damage /= 100;
            }
            if (Util.isTrue(200, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            if (effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            if (damage > 10_000_000) {
                damage = Util.nextInt(9_000_000, 10_000_000);
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
        st = System.currentTimeMillis();
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
