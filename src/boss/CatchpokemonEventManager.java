package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
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
