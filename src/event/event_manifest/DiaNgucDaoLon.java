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
        // NPC Đổi Thưởng tại Địa Ngục (map 174)
        // Luồng vào Địa Ngục đã có sẵn qua Bà Hạt Mít ở Đảo Kamè (map 5)
        createNpc(174, ConstNpc.NPC_DIA_NGUC, 300, 336);
    }

    @Override
    public void boss() {
        // Boss Janemba — spawn tại Địa Ngục 3 (map 180), respawn mỗi 2h
        createBoss(BossID.JANEMBA_BOSS, 1);
    }
}
