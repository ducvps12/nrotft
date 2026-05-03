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

    @Override
    public void showListBoss(nro.player.Player player) {
        player.iDMark.setMenuType(4); // 4 = Pokemon boss list
        super.showListBoss(player);
        player.iDMark.setMenuType(4); // re-set after super call
    }

}
