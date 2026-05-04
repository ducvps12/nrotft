package nro.models.npc.npc_manifest;

import consts.ConstMap;
import consts.ConstNpc;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.Service;
import services.func.ChangeMapService;

/**
 * NPC Dẫn Đường Địa Ngục - Teleport người chơi vào map Địa Ngục (174).
 * Hiển thị trên Thần Điện (map 45) khi event Địa Ngục được bật.
 */
public class NpcDiaNguc extends Npc {

    public NpcDiaNguc(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 45) {
                // Ở Thần Điện → hỏi có muốn vào Địa Ngục không
                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Ta là người canh giữ cổng Địa Ngục.\n"
                        + "Bên trong đó đầy rẫy quỷ dữ và linh hồn tà ác.\n"
                        + "Ngươi có muốn bước vào hay không?",
                        "Vào\\nĐịa Ngục", "Từ chối");
            } else if (this.mapId == 174) {
                // Ở Địa Ngục → hỏi có muốn đổi thưởng không
                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Ngươi đã thu thập được bao nhiêu Hồn Quỷ rồi?\n"
                        + "Hãy mang đến đây để ta đổi cho ngươi phần thưởng xứng đáng.",
                        "Đổi\\nthưởng", "Quay lại\\nThần Điện");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 45) {
                // Ở Thần Điện
                if (select == 0) {
                    // Teleport vào Địa Ngục
                    ChangeMapService.gI().changeMap(player, 174, -1, 100, 336);
                    Service.gI().sendThongBao(player, "Bạn đã bước vào Địa Ngục!");
                }
            } else if (this.mapId == 174) {
                if (select == 0) {
                    // Đổi thưởng - TODO: implement exchange logic
                    Service.gI().sendThongBao(player, "Tính năng đang phát triển!");
                } else if (select == 1) {
                    // Quay về Thần Điện
                    ChangeMapService.gI().changeMap(player, ConstMap.THAN_DIEN, -1, 340, 408);
                }
            }
        }
    }
}
