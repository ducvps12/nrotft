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

    // Menu phụ cho hướng dẫn SKH chi tiết (tránh tràn text)
    private static final int MENU_SKH_GUIDE = 77001;

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
                    "|7|━━━ GOHAN ULTRA ━━━\n"
                            + "|2|Shop cao cấp & Hướng dẫn SKH\n\n"
                            + "|5|Nơi rơi Set Kích Hoạt:\n"
                            + "|6|Trái Đất:|0| Map 1, 2, 3\n"
                            + "|6|Namếc:|0| Map 8, 9, 11\n"
                            + "|6|Xayda:|0| Map 15, 16, 17\n\n"
                            + "|8|Tỉ lệ: 1/5000 (có Cỏ bốn lá: 1/3500)",
                    "Shop\nUltra", "Hướng dẫn\nSKH", "Set\nTrái Đất", "Set\nNamếc", "Set\nXayda", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        switch (player.iDMark.getIndexMenu()) {
            case ConstNpc.BASE_MENU -> handleBaseMenu(player, select);
            case MENU_SKH_GUIDE -> handleSkhGuide(player, select);
            default -> {}
        }
    }

    private void handleBaseMenu(Player player, int select) {
        switch (select) {
            case 0 -> ShopService.gI().opendShop(player, "GOHAN_ULTRA", false);
            case 1 -> showSkhGuide(player);
            case 2 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                    "|7|━━━ SET TRÁI ĐẤT ━━━\n"
                            + "|1|Dòng: Songoku, Kaioken, Tên Xin Hăng.\n\n"
                            + "|2|Đặc điểm:\n"
                            + "- Dễ tiếp cận, hợp tân thủ.\n"
                            + "- Mạnh sát thương, dồn đòn.\n\n"
                            + "|5|Mẹo: mới chơi chọn Trái Đất\n"
                            + "là dễ làm quen nhất.",
                    "Shop\nUltra", "Hướng dẫn\nSKH", "Set\nTrái Đất", "Set\nNamếc", "Set\nXayda", "Đóng");
            case 3 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                    "|7|━━━ SET NAMẾC ━━━\n"
                            + "|1|Dòng: Piccolo, Ốc Tiêu, Pikkoro Daimao.\n\n"
                            + "|2|Đặc điểm:\n"
                            + "- Trâu, sống dai, hồi phục tốt.\n"
                            + "- Hợp farm lâu, ít chết.\n\n"
                            + "|5|Mẹo: hay bị quái/boss vả nằm\n"
                            + "đất thì gom set Namếc.",
                    "Shop\nUltra", "Hướng dẫn\nSKH", "Set\nTrái Đất", "Set\nNamếc", "Set\nXayda", "Đóng");
            case 4 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                    "|7|━━━ SET XAYDA ━━━\n"
                            + "|1|Dòng: Cadic, Nappa, Kakarot/Galick.\n\n"
                            + "|2|Đặc điểm:\n"
                            + "- Sát thương bùng nổ, đã tay.\n"
                            + "- Hợp giao tranh, săn boss.\n\n"
                            + "|8|Lưu ý: nhánh hiếm tỉ lệ 1/7000.\n"
                            + "Rơi được thì vui, chưa rơi đừng\n"
                            + "chửi Gohan nha.",
                    "Shop\nUltra", "Hướng dẫn\nSKH", "Set\nTrái Đất", "Set\nNamếc", "Set\nXayda", "Đóng");
            default -> {}
        }
    }

    /**
     * Hiển thị hướng dẫn SKH ngắn gọn, vừa vặn khung dialog.
     */
    private void showSkhGuide(Player player) {
        createOtherMenu(player, MENU_SKH_GUIDE,
                "|7|━━━ CẨM NANG SKH ━━━\n\n"
                        + "|1|SKH là đồ có dòng kích hoạt theo\n"
                        + "hành tinh. Mặc đúng hệ, gom đủ bộ\n"
                        + "sẽ mở hiệu ứng mạnh.\n\n"
                        + "|5|Cách làm:\n"
                        + "|0|1. Nhặt đồ đúng hành tinh.\n"
                        + "2. Đọc dòng kích hoạt trên đồ.\n"
                        + "3. Gom cùng dòng mặc thành bộ.\n"
                        + "4. Đồ lệch hệ bán hoặc giữ NL.\n\n"
                        + "|6|Tỉ lệ rơi:\n"
                        + "|0|- Thường: 1/5000 quái\n"
                        + "- Cỏ bốn lá: 1/3500 quái\n"
                        + "- Nhánh hiếm XD: 1/7000 quái\n\n"
                        + "|8|SKH là đồ hiếm, kiên nhẫn nhé!",
                "Quay lại");
    }

    private void handleSkhGuide(Player player, int select) {
        if (select == 0) {
            openBaseMenu(player);
        }
    }
}

