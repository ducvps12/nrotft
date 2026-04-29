package event.event_manifest;

/*
 * Sự kiện Giỗ Tổ Hùng Vương
 * - Spawn Boss Sơn Tinh + Thuỷ Tinh liên tục tại 3 hành tinh
 * - Spawn Boss Voi Chín Ngà, Gà Chín Cựa, Ngựa Chín Hồng Mao
 * - Rồng Nhí cho tìm ngọc rồng
 */

import boss.BossID;
import event.Event;

public class HungVuong extends Event {

    @Override
    public void boss() {
        // Boss chính: Sơn Tinh vs Thuỷ Tinh - liên tục 3 hành tinh
        createBoss(BossID.THUY_TINH, 10);

        // Boss đặc biệt: Voi, Gà, Ngựa - drop lễ vật
        createBoss(BossID.VOI_CHIN_NGA, 5);
        createBoss(BossID.GA_CHIN_CUA, 5);
        createBoss(BossID.NGUA_CHIN_HONG_MAO, 5);

        // Rồng Nhí
        this.createBoss(BossID.RONG_1_SAO, 5);
        this.createBoss(BossID.RONG_2_SAO, 5);
        this.createBoss(BossID.RONG_3_SAO, 5);
        this.createBoss(BossID.RONG_4_SAO, 5);
        this.createBoss(BossID.RONG_5_SAO, 5);
        this.createBoss(BossID.RONG_6_SAO, 5);
        this.createBoss(BossID.RONG_7_SAO, 5);
    }
}
