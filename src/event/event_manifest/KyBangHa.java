package event.event_manifest;

/*
 * ❄️ Sự kiện Kỷ Băng Hà
 * - Map: 195 (Cánh đồng tuyết 2), 196 (Rừng tuyết 2), 197 (Hang băng 2)
 * - Mob: Frostbite (105), Snowy Tangerine (106), Deinonychus (107), Snake (108)
 * - Mob mạnh hơn bản gốc 5x, drop "Tinh Thể Băng" (item event)
 * - Gom 99 Tinh Thể Băng → đổi "Vũ Khí Băng" (cải trang đặc biệt)
 * - Boss Ice Shenron spawn ở Hang Băng 2 (map 197) mỗi 4h
 * - Mob ở đây drop Mảnh vỡ BTC3 (1855) rate 10% — nguồn farm mới
 */

import boss.BossID;
import event.Event;

public class KyBangHa extends Event {

    @Override
    public void npc() {
        // NPC Dẫn Đường tại Cánh đồng tuyết (map 105) → Cánh đồng tuyết 2
        createNpc(105, 64, 300, 288);  // NPC portal
        // NPC Đổi Thưởng tại Cánh đồng tuyết 2 (map 195)
        createNpc(195, 64, 200, 336);
    }

    @Override
    public void boss() {
        // Boss Ice Shenron — Hang băng 2 (map 197), respawn 4h
        createBoss(BossID.ICE_SHENRON_BOSS, 1);
    }
}
