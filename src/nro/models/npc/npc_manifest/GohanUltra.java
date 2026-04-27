package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.TaskService;
import shop.ShopService;

/**
 * NPC GohanUltra.
 *
 * SQL npc_template id 75 is named GohanUltra, but this id was previously routed
 * to Cadic in NpcFactory. This class gives id 75 its own behavior so talking to
 * GohanUltra no longer falls through to an unrelated/empty NPC flow.
 */
public class GohanUltra extends Npc {

    public GohanUltra(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "|7|GohanUltra Shop\n|2|Cải trang - Danh hiệu - Hỗ trợ cao cấp\n|1|Thanh toán bằng hồng ngọc, vật phẩm mạnh có hạn dùng và khóa giao dịch.",
                    "Shop\nUltra", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (player.iDMark.isBaseMenu()) {
            switch (select) {
                case 0 -> ShopService.gI().opendShop(player, "GOHAN_ULTRA", false);
                default -> {
                }
            }
        }
    }
}
