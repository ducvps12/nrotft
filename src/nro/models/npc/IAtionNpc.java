package nro.models.npc;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/hfaysi616
 *  sdt zalo: 0372875491
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import nro.player.Player;

public interface IAtionNpc {

    void openBaseMenu(Player player);

    void confirmMenu(Player player, int select);

}
