package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/hfaysi616
 *  sdt zalo: 0372875491
 * Chuyên chỉnh sữa mua bán source nro,...
 */

public class YardartManager extends BossManager {

    private static YardartManager instance;

    public static YardartManager gI() {
        if (instance == null) {
            instance = new YardartManager();
        }
        return instance;
    }

}
