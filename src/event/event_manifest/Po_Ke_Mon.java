package event.event_manifest;

/*
 * Sự kiện Pokémon 30/4 - 1/5
 * - 3 Boss Pokémon: Pikachu (Kakaro), Charmander (Aru), Squirtle (Mori)
 * - HP=5000, mỗi hit trừ 1 HP, không gây dame lên người chơi
 * - Drop 2-4 trứng Pokémon khi chết, respawn 30 phút
 * - Item 695-698 rơi từ mob 10% tại map TD/XD/NM
 * - Đổi vật phẩm tại NPC Quy Lão Kame
 */

import boss.BossID;
import event.Event;

public class Po_Ke_Mon extends Event {

    @Override
    public void boss() {
        // NPC ChiChi tại Đảo Kame (map 5)
        createNpc(5, 82, 231, 288);

        // 3 Boss Pokémon mới - spawn tại 3 làng (1 con mỗi loại)
        createBoss(BossID.PIKACHU_BOSS);    // Làng Kakaro (map 2)
        createBoss(BossID.CHARMANDER_BOSS); // Làng Aru (map 9)
        createBoss(BossID.SQUIRTLE_BOSS);   // Làng Mori (map 16)
    }
}
