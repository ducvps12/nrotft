package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/hfaysi616
 *  sdt zalo: 0372875491
 * Chuyên chỉnh sữa mua bán source nro,...
 */

public class CatchpokemonEventManager extends BossManager {

    private static CatchpokemonEventManager instance;

    public static CatchpokemonEventManager gI() {
        if (instance == null) {
            instance = new CatchpokemonEventManager();
        }
        return instance;
    }

}
