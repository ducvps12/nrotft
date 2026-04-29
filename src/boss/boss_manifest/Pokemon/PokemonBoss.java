package boss.boss_manifest.Pokemon;

import boss.Boss;
import boss.BossData;
import boss.BossesData;
import boss.BossID;
import boss.CatchpokemonEventManager;
import static boss.BossType.CATH_POKEMON;
import event.EventManager;
import item.Item;
import map.ItemMap;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

/**
 * Boss Pokémon Event 30/4 - 1/5
 * - HP = 5000, mỗi lần bị đánh trừ 1 HP
 * - Không gây sát thương lên người chơi (dame=0, tắt phản sát thương)
 * - Spawn tại Làng Kakaro (map 2), Làng Aru (map 9), Làng Mori (map 16)
 * - Respawn 30 phút sau khi chết
 * - Drop 2-4 trứng Pokémon (Thường/Ultra/Master) khi chết
 */
public class PokemonBoss extends Boss {

    private long st;

    public PokemonBoss(int bossId, BossData data) throws Exception {
        super(CATH_POKEMON, bossId, false, true, data);
    }

    /**
     * Override: mỗi hit chỉ trừ 1 HP bất kể sức mạnh
     */
    @Override
    public long injured(Player plAtt, long damage, boolean isCrit, boolean isMobAttack) {
        if (this.isDie()) return 0;
        // Mỗi lần đánh chỉ trừ 1 HP
        long actualDamage = 1;
        this.nPoint.subHP(actualDamage);

        if (this.isDie()) {
            this.setDie(plAtt);
            die(plAtt);
        }
        return actualDamage;
    }

    /**
     * Override: không di chuyển (đứng yên tại chỗ)
     */
    @Override
    public void moveTo(int x, int y) {
        // Pokémon boss đứng yên
    }

    /**
     * Override: không tấn công người chơi (tắt hoàn toàn cơ chế gây dame)
     */
    @Override
    public void attack() {
        // Không tấn công - tránh phản sát thương
    }

    /**
     * Override: drop 2-4 trứng Pokémon khi chết
     * - Trứng Thường (1873): 60% chance
     * - Trứng Ultra (1874): 30% chance 
     * - Trứng Master (1875): 10% chance
     */
    @Override
    public void reward(Player plKill) {
        int numEggs = Util.nextInt(2, 4);
        for (int i = 0; i < numEggs; i++) {
            int eggId;
            int rand = Util.nextInt(100);
            if (rand < 10) {
                eggId = 1875; // Trứng Master - 10%
            } else if (rand < 40) {
                eggId = 1874; // Trứng Ultra - 30%
            } else {
                eggId = 1873; // Trứng Thường - 60%
            }
            Service.gI().dropItemMap(this.zone,
                    new ItemMap(zone, eggId, 1,
                            this.location.x + (i * Util.nextInt(-30, 30)),
                            this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                            plKill.id));
        }
    }

    @Override
    protected void notifyJoinMap() {
        super.notifyJoinMap();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 1800000)) { // 30 phút
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    // ===================== Factory methods =====================
    public static PokemonBoss createPikachu() throws Exception {
        return new PokemonBoss(BossID.PIKACHU_BOSS, BossesData.PIKACHU_BOSS);
    }

    public static PokemonBoss createCharmander() throws Exception {
        return new PokemonBoss(BossID.CHARMANDER_BOSS, BossesData.CHARMANDER_BOSS);
    }

    public static PokemonBoss createSquirtle() throws Exception {
        return new PokemonBoss(BossID.SQUIRTLE_BOSS, BossesData.SQUIRTLE_BOSS);
    }
}
