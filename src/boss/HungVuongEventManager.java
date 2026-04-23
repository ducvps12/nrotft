package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

public class HungVuongEventManager extends BossManager {

    private static HungVuongEventManager instance;

    public static HungVuongEventManager gI() {
        if (instance == null) {
            instance = new HungVuongEventManager();
        }
        return instance;
    }

}
