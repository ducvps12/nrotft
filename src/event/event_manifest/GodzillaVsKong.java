package event.event_manifest;

/*
 * 🦎 Sự kiện Godzilla vs Kong
 * - Map: 175 (Hành Tinh Vampa)
 * - World Boss: Godzilla (mob 92) spawn 12h trưa, Kong (mob 93) spawn 20h tối
 * - Cả 2 chết cùng ngày → spawn MechaGodzilla (super boss) HP 20 tỉ
 * - Drop: Cải trang đặc biệt, Ngọc Rồng 5-7 sao, Capsule VIP
 * - Mob thường ở Vampa drop "Mảnh Titan" → gom 50 cái đổi quà
 */

import boss.BossID;
import event.Event;

public class GodzillaVsKong extends Event {

    @Override
    public void boss() {
        // Godzilla World Boss — Hành Tinh Vampa
        createBoss(BossID.GODZILLA_BOSS, 1);
        // Kong World Boss — Hành Tinh Vampa
        createBoss(BossID.KONG_BOSS, 1);
    }
}
