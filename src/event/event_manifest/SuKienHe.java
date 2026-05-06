package event.event_manifest;

/*
 * 🏖️ Sự Kiện Hè — Summer Event
 * ════════════════════════════════════
 * Hoạt động chính:
 * 1. Thu thập Vỏ Ốc (695), Vỏ Sò (696), Con Cua (697), Sao Biển (698) tại map 161-163
 * 2. Làm Nước Mía tại NPC Quầy Nước Mía (ID: 83) — craft + trả hàng + buff toàn server
 * 3. Đổi Hộp Quà Sự Kiện tại NPC Quy Lão Kame — 50% thành công
 * 4. Boss Mặt Trời Mùa Hè — spawn mỗi 2h tại các làng, drop Cờ Mặt Trời
 * 5. Khuyến mãi x2 nạp ATM/Bank
 * 6. Đua Top điểm sự kiện
 *
 * Kích hoạt: EventPanel → Sự Kiện Hè (ID = 16)
 */

import boss.BossID;
import consts.ConstNpc;
import event.Event;
import nro.services.MapService;
import nro.models.npc.NpcFactory;

public class SuKienHe extends Event {

    @Override
    public void npc() {
        // NPC Quầy Nước Mía tại các làng chính
        createNpc(0, ConstNpc.QUAY_NUOC_MIA, 350, 432);    // Làng Aru (Trái Đất)
        createNpc(7, ConstNpc.QUAY_NUOC_MIA, 350, 432);    // Làng Mori (Namek)
        createNpc(14, ConstNpc.QUAY_NUOC_MIA, 350, 408);   // Làng Kakarot (Xayda)
    }

    @Override
    public void createNpc(int mapId, int npcId, int x, int y) {
        MapService.gI().getMapById(mapId).npcs.add(NpcFactory.createNPC(mapId, 0, x, y, npcId));
    }

    @Override
    public void boss() {
        // Boss Mặt Trời Mùa Hè — spawn 3 con tại 3 hành tinh
        createBoss(BossID.Virut, 3);
    }
}
