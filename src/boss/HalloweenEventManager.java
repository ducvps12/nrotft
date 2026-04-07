package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/hfaysi616
 *  sdt zalo: 0372875491
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
