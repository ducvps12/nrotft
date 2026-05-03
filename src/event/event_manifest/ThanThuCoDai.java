package event.event_manifest;

/*
 * 🐘 Sự kiện Thần Thú Cổ Đại
 * - Map: 176 (Cung Trăng), 178 (Vùng Đất Huyền Thoại)
 * - 3 Thần Thú: Voi Chín Ngà (82), Gà Chín Cửa (83), Ngựa Chín Hồng Mao (84)
 * - Spawn ở 3 khu khác nhau trên map, mỗi con drop "Linh Phù" tương ứng
 * - Ghép 3 Linh Phù (mỗi loại x1) → Triệu hồi "Thần Long Cổ Đại" (boss ẩn)
 * - Boss ẩn drop: BTC3 hoàn chỉnh (ID 1810), Capsule VIP, NR 5+ sao
 * - Mob Piano (85) làm mini-boss gác cổng Cung Trăng
 */

import boss.BossID;
import event.Event;

public class ThanThuCoDai extends Event {

    @Override
    public void npc() {
        // NPC dẫn đường tại Thánh Địa Kaio (map 50) → Cung Trăng
        createNpc(50, 63, 350, 288);   // NPC hướng dẫn
        // NPC đổi Linh Phù tại Cung Trăng (map 176)
        createNpc(176, 63, 200, 336);
    }

    @Override
    public void boss() {
        // 3 Thần Thú đã có sẵn BossID từ Hùng Vương event
        createBoss(BossID.VOI_CHIN_NGA, 2);
        createBoss(BossID.GA_CHIN_CUA, 2);
        createBoss(BossID.NGUA_CHIN_HONG_MAO, 2);

        // Boss ẩn — Thần Long Cổ Đại (trigger bằng ghép Linh Phù)
        // Không spawn tự động, chỉ spawn khi player dùng item
    }
}
