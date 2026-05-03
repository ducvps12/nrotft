package event.event_manifest;

/*
 * 🔥 Sự kiện Địa Ngục Đảo Lộn
 * - Map: 174 (Địa Ngục), 179 (Địa Ngục 2), 180 (Địa Ngục 3)
 * - Mob nguyên tố: Quỷ đỏ (88), Quỷ xanh (89), Quỷ xanh lá (90), Quỷ vàng (91)
 * - Boss: Janemba (96) spawn mỗi 2h tại Địa Ngục 3
 * - Mob drop "Hồn Quỷ" → gom 100 cái đổi Capsule CT VIP, 500 → Nhẫn Thời Không
 * - MEZ (97) + GOZ (98) là mini-boss canh cổng Địa Ngục 2→3
 */

import boss.BossID;
import consts.ConstNpc;
import event.Event;

public class DiaNgucDaoLon extends Event {

    @Override
    public void npc() {
        // NPC Dẫn Đường Địa Ngục tại Thần Điện (map 45)
        // Dùng NPC DauThan (ID có sẵn) để teleport vào map 174
        createNpc(45, ConstNpc.DAU_THAN, 200, 288);
        // NPC Đổi Thưởng tại Địa Ngục (map 174)
        createNpc(174, ConstNpc.DAU_THAN, 300, 336);
    }

    @Override
    public void boss() {
        // Boss Janemba — spawn tại Địa Ngục 3 (map 180), respawn mỗi 2h
        createBoss(BossID.JANEMBA_BOSS, 1);
    }
}
