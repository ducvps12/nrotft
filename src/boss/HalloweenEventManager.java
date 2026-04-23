package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

public class HalloweenEventManager extends BossManager {

    private static HalloweenEventManager instance;

    public static HalloweenEventManager gI() {
        if (instance == null) {
            instance = new HalloweenEventManager();
        }
        return instance;
    }
}
