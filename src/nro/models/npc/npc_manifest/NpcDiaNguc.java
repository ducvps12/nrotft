package nro.models.npc.npc_manifest;

import consts.ConstMap;
import consts.ConstNpc;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.Service;
import services.func.ChangeMapService;

/**
 * NPC Đổi Thưởng Địa Ngục - Đổi Hồn Quỷ lấy phần thưởng tại map 174.
 * Luồng vào Địa Ngục đã có sẵn qua Bà Hạt Mít ở Đảo Kamè (map 5).
 */
public class NpcDiaNguc extends Npc {

    public NpcDiaNguc(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            // NPC chỉ ở map 174 (Địa Ngục) → đổi thưởng
            this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Ngươi đã thu thập được bao nhiêu Hồn Quỷ rồi?\n"
                    + "Hãy mang đến đây để ta đổi cho ngươi phần thưởng xứng đáng.",
                    "Đổi\\nthưởng", "Quay lại\\nĐảo Kamè");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (select == 0) {
                // Đổi thưởng - TODO: implement exchange logic
                Service.gI().sendThongBao(player, "Tính năng đang phát triển!");
            } else if (select == 1) {
                // Quay về Đảo Kamè (map 5)
                ChangeMapService.gI().changeMap(player, 5, -1, 340, 408);
            }
        }
    }
}
