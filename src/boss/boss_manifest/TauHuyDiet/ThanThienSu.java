package boss.boss_manifest.TauHuyDiet;

/**
 *
 * @author Administrator
 */

import boss.Boss;
import boss.BossID;
import boss.BossesData;
import item.Item;
import map.ItemMap;
import utils.Util;

import java.util.Random;
import nro.player.Player;
import nro.services.Service;

public class ThanThienSu extends Boss {

    private long st;
    
    public ThanThienSu() throws Exception {
        super(BossID.THAN_THIEN_SU, BossesData.THAN_THIEN_SU);
    }
    
    @Override
    public void reward(Player plKill) {
        if (Util.isTrue(10, 100)) {
            ItemMap it = new ItemMap(this.zone, 650, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it.options.add(new Item.ItemOption(47, new Random().nextInt(1001) + 1800)); // áo từ 1800-2800 giáp
//            it.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it2 = new ItemMap(this.zone, 652, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it2.options.add(new Item.ItemOption(47, new Random().nextInt(1001) + 1800)); // áo từ 1800-2800 giáp
//            it2.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it2.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it2.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it3 = new ItemMap(this.zone, 654, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it3.options.add(new Item.ItemOption(47, new Random().nextInt(1001) + 1800)); // áo từ 1800-2800 giáp
//            it3.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it3.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it3.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it4 = new ItemMap(this.zone, 651, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it4.options.add(new Item.ItemOption(22, new Random().nextInt(16) + 85)); // hp 85-100k
//            it4.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it4.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it4.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it5 = new ItemMap(this.zone, 653, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it5.options.add(new Item.ItemOption(22, new Random().nextInt(16) + 85)); // hp 85-100k
//            it5.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it5.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it5.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it6 = new ItemMap(this.zone, 655, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it6.options.add(new Item.ItemOption(22, new Random().nextInt(16) + 85)); // hp 85-100k
//            it6.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it6.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it6.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it7 = new ItemMap(this.zone, 657, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it7.options.add(new Item.ItemOption(0, new Random().nextInt(150) + 8500)); // 8500-10000
//            it7.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it7.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it7.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it8 = new ItemMap(this.zone, 659, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it8.options.add(new Item.ItemOption(0, new Random().nextInt(150) + 8500)); // 8500-10000
//            it8.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it8.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it8.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it9 = new ItemMap(this.zone, 661, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it9.options.add(new Item.ItemOption(0, new Random().nextInt(150) + 8500)); // 8500-10000
//            it9.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it9.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it9.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it10 = new ItemMap(this.zone, 658, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it10.options.add(new Item.ItemOption(23, new Random().nextInt(11) + 80)); // ki 80-90k
//            it10.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it10.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it10.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it11 = new ItemMap(this.zone, 660, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it11.options.add(new Item.ItemOption(23, new Random().nextInt(11) + 80)); // ki 80-90k
//            it11.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it11.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it11.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it12 = new ItemMap(this.zone, 662, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it12.options.add(new Item.ItemOption(23, new Random().nextInt(11) + 80)); // ki 80-90k
//            it12.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it12.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it12.options.add(new Item.ItemOption(30, 1));// ko the gd

            ItemMap it13 = new ItemMap(this.zone, 656, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            it13.options.add(new Item.ItemOption(14, new Random().nextInt(3) + 17)); // áo từ 1800-2800 giáp
//            it13.options.add(new Item.ItemOption(86, 1));// Ký gửi vàng
            it13.options.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
            it13.options.add(new Item.ItemOption(30, 1));// ko the gd

            int randomdo = Util.nextInt(1, 14);
            if (randomdo == 1) {
                Service.gI().dropItemMap(this.zone, it);
            }
            if (randomdo == 2) {
                Service.gI().dropItemMap(this.zone, it2);
            }
            if (randomdo == 3) {
                Service.gI().dropItemMap(this.zone, it3);
            }
            if (randomdo == 4) {
                Service.gI().dropItemMap(this.zone, it4);
            }
            if (randomdo == 5) {
                Service.gI().dropItemMap(this.zone, it5);
            }
            if (randomdo == 6) {
                Service.gI().dropItemMap(this.zone, it6);
            }
            if (randomdo == 7) {
                Service.gI().dropItemMap(this.zone, it7);
            }
            if (randomdo == 8) {
                Service.gI().dropItemMap(this.zone, it8);
            }
            if (randomdo == 9) {
                Service.gI().dropItemMap(this.zone, it9);
            }
            if (randomdo == 10) {
                Service.gI().dropItemMap(this.zone, it10);
            }
            if (randomdo == 11) {
                Service.gI().dropItemMap(this.zone, it11);
            }
            if (randomdo == 12) {
                Service.gI().dropItemMap(this.zone, it12);
            }
            if (randomdo == 13) {
                Service.gI().dropItemMap(this.zone, it13);
            }
        }
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
        }
//        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
//            if (boss.id == BossID.THAN_HUY_DIET && !boss.isDie()) {
//                super.active();
//                break;
//            }
//        }
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }
}

