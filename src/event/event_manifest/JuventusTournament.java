package event.event_manifest;

/*
 * 🏟️ Sự kiện Juventus Tournament
 * - Map: 183 (Juventus) — 20 khu vực, 7 mob
 * - PVP Tournament hàng tuần, 16 người single elimination
 * - Đăng ký qua NPC Trọng Tài, phí 1 Thỏi vàng
 * - DB column "juventus" đã có sẵn, đếm số lần tham gia
 * - Phần thưởng: Top 1 = 50 Thỏi vàng + Danh hiệu
 *                Top 2 = 30 TV, Top 4 = 15 TV
 * - Mob ở map Juventus drop vàng + Hồng Ngọc rate cao
 */

import event.Event;

public class JuventusTournament extends Event {

    @Override
    public void npc() {
        // NPC Trọng Tài tại Đại Hội Võ Thuật (map 52) — dẫn vào Juventus
        // NPC GhiDanh tại map 183 (Juventus) — đăng ký giải đấu
        createNpc(52, 61, 400, 336);   // NPC dẫn đường ID 61
        createNpc(183, 61, 300, 288);  // NPC đăng ký tại Juventus
    }
}
