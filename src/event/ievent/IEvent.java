package event.ievent;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

public interface IEvent {

    void init();

    void npc();

    void createNpc(int mapId, int npcId, int x, int y);

    void boss();

    void createBoss(int bossId, int... total);

    void itemMap();

    void itemBoss();
}
