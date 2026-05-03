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

        // ====== DROP ITEMS CHO TẤT CẢ PLAYER ĐÁNH ======
        if (this.zone == null) return;

        java.util.List<Player> players = this.zone.getPlayers();
        for (Player pl : players) {
            if (pl == null || pl.isDie() || pl.id == -1000000) continue;

            java.util.List<ItemMap> drops = new java.util.ArrayList<>();
            int x = this.location.x;
            int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

            // === 1. Vàng game (100% cho tất cả) ===
            int gold = utils.Util.nextInt(5_000_000, 20_000_000);
            drops.add(new ItemMap(this.zone, 190, gold, x + utils.Util.nextInt(-50, 50), y, pl.id));

            // === 2. Thỏi vàng (30%) ===
            if (utils.Util.isTrue(30, 100)) {
                int qty = utils.Util.nextInt(1, 3);
                drops.add(new ItemMap(this.zone, 457, qty, x + utils.Util.nextInt(-40, 40), y, pl.id));
            }

            // === 3. Ngọc Rồng 1-4 sao (20%) ===
            if (utils.Util.isTrue(20, 100)) {
                int nrId = 14 + utils.Util.nextInt(0, 3); // ID 14-17 (NR1-4 sao)
                drops.add(new ItemMap(this.zone, nrId, 1, x + utils.Util.nextInt(-30, 30), y, pl.id));
            }

            // === 4. Capsule cải trang VIP (5%) ===
            if (utils.Util.isTrue(5, 100)) {
                drops.add(new ItemMap(this.zone, 2006, 1, x + utils.Util.nextInt(-20, 20), y, pl.id));
            }

            // === 5. Sách phân tách level 3-5 (15%) ===
            if (utils.Util.isTrue(15, 100)) {
                int[] sachIds = {1826, 1827, 1828}; // Sách phân tách 3/4/5
                int sachId = sachIds[utils.Util.nextInt(0, sachIds.length - 1)];
                drops.add(new ItemMap(this.zone, sachId, 1, x + utils.Util.nextInt(-35, 35), y, pl.id));
            }

            // === 6. Hồng Ngọc (40%) ===
            if (utils.Util.isTrue(40, 100)) {
                int hnQty = utils.Util.nextInt(500, 2000);
                drops.add(new ItemMap(this.zone, 861, hnQty, x + utils.Util.nextInt(-45, 45), y, pl.id));
            }

            // === 7. Nhẫn Thời Không (2%) ===
            if (utils.Util.isTrue(2, 100)) {
                drops.add(new ItemMap(this.zone, 992, 1, x + utils.Util.nextInt(-25, 25), y, pl.id));
            }

            // === 8. Mảnh vỡ bông tai cấp 3 (25%) ===
            if (utils.Util.isTrue(25, 100)) {
                int btc3Qty = utils.Util.nextInt(50, 200);
                drops.add(new ItemMap(this.zone, 1855, btc3Qty, x + utils.Util.nextInt(-30, 30), y, pl.id));
            }

            // Drop tất cả items
            for (ItemMap im : drops) {
                if (im.itemTemplate != null) {
                    nro.services.Service.gI().dropItemMap(this.zone, im);
                }
            }
        }

        // Thông báo server
        nro.server.ServerNotify.gI().notify("Đại Tướng Broly đã bị " + plKill.name + " hạ gục! Phần thưởng đã rơi!");
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
    }
}
