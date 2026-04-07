package nro.models.npc.npc_manifest;

import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.Service;
import consts.ConstNpc;
import models.ClanBattle.ClanBattleManager;

public class BunmaBunny extends Npc {

    public BunmaBunny(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (this.mapId == 5) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                "|0|Đại Hội Bang Chiến\n|7|Chỉ Bang chủ mới có quyền đăng ký thi đấu!",
                "Đăng ký", "Danh sách", "Đóng");
        } else {
            super.openBaseMenu(player);
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (player.iDMark.isBaseMenu() && this.mapId == 5) {
            switch (select) {
                case 0 -> { // Đăng ký
                    if (player.clan == null || player.clan.role != 0) {
                        Service.gI().sendThongBao(player, "Chỉ Bang chủ mới có thể đăng ký!");
                        return;
                    }
                    ClanBattleManager.gI().addWaitList(player.clan);
                    Service.gI().sendThongBao(player, "Đăng ký thành công bang " + player.clan.name);
                }
                case 1 -> { // Danh sách
                    StringBuilder sb = new StringBuilder("|0|BANG ĐANG CHỜ:\n");
                    var list = ClanBattleManager.gI().getWaitList();
                    if (list.isEmpty()) sb.append("Trống");
                    for (int i = 0; i < list.size(); i++) {
                        sb.append(i + 1).append(". ").append(list.get(i).name).append("\n");
                    }
                    Service.gI().sendThongBaoOK(player, sb.toString());
                }
            }
        }
    }
}