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
                    "|7|━━━ GOHAN ULTRA SHOP ━━━\n"
                            + "|2|Cải trang - Danh hiệu - Hỗ trợ cao cấp\n"
                            + "|1|Thanh toán bằng hồng ngọc, vật phẩm mạnh có hạn dùng và khóa giao dịch.\n\n"
                            + "|5|━━━ NƠI RƠI SET KÍCH HOẠT ━━━\n"
                            + "|6|Trái Đất:|0| Map 1, 2, 3\n"
                            + "|6|Namếc:|0| Map 8, 9, 11\n"
                            + "|6|Xayda:|0| Map 15, 16, 17\n\n"
                            + "|8|Tỉ lệ rơi:\n"
                            + "- Không dùng Cỏ bốn lá: 1/5000 quái\n"
                            + "- Có Cỏ bốn lá: 1/3500 quái\n"
                            + "- Xayda có thêm nhánh hiếm: 1/7000 quái",
                    "Shop\nUltra", "Hướng dẫn\nSKH", "Set\nTrái Đất", "Set\nNamếc", "Set\nXayda", "Đóng");
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
                case 1 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|7|━━━ CẨM NANG SET KÍCH HOẠT ━━━\n"
                                + "|1|SKH là đồ có dòng kích hoạt theo hành tinh. Khi mặc đúng hệ và gom đủ bộ, nhân vật sẽ mở thêm hiệu ứng mạnh hơn đồ thường.\n\n"
                                + "|5|Nơi farm cho tân thủ:\n"
                                + "- Trái Đất: map 1, 2, 3.\n"
                                + "- Namếc: map 8, 9, 11.\n"
                                + "- Xayda: map 15, 16, 17.\n\n"
                                + "|6|Tỉ lệ rơi hiện tại:\n"
                                + "- Farm thường: khoảng 1/5000 quái.\n"
                                + "- Dùng Cỏ bốn lá: khoảng 1/3500 quái.\n"
                                + "- Một số nhánh Xayda hiếm: khoảng 1/7000 quái.\n\n"
                                + "|2|Cách hiểu đơn giản:\n"
                                + "1. Nhặt đồ đúng hành tinh của mình trước.\n"
                                + "2. Đọc tên dòng kích hoạt trên món đồ.\n"
                                + "3. Gom các món cùng dòng để mặc thành bộ.\n"
                                + "4. Đồ lệch hệ thì nên bán, trao đổi hoặc giữ làm nguyên liệu.\n\n"
                                + "|8|Lưu ý: SKH là đồ hiếm. Đừng thấy người khác rơi nhanh mà tưởng dễ, có người đỏ vài phút, có người cày cả buổi mới ra.",
                        "Shop\nUltra", "Hướng dẫn\nSKH", "Set\nTrái Đất", "Set\nNamếc", "Set\nXayda", "Đóng");
                case 2 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|7|━━━ SET KÍCH HOẠT TRÁI ĐẤT ━━━\n"
                                + "|1|Dành cho người chơi hệ Trái Đất. Các dòng thường gặp gồm: Songoku, Kaioken, Tên Xin Hăng.\n\n"
                                + "|2|Lối chơi chính:\n"
                                + "- Dễ tiếp cận, hợp tân thủ.\n"
                                + "- Mạnh ở sát thương và dồn đòn.\n"
                                + "- Phù hợp farm quái, đi boss nhẹ và giao tranh cơ bản.\n\n"
                                + "|6|Khi dùng set này:\n"
                                + "Hãy ưu tiên mặc các món cùng dòng kích hoạt. Mặc lẫn nhiều dòng khác nhau thường không phát huy đủ sức mạnh.\n\n"
                                + "|5|Mẹo: nếu bạn mới chơi và chưa biết chọn gì, Trái Đất là nhánh dễ làm quen nhất.",
                        "Shop\nUltra", "Hướng dẫn\nSKH", "Set\nTrái Đất", "Set\nNamếc", "Set\nXayda", "Đóng");
                case 3 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|7|━━━ SET KÍCH HOẠT NAMẾC ━━━\n"
                                + "|1|Dành cho người chơi hệ Namếc. Các dòng thường gặp gồm: Piccolo, Ốc Tiêu, Pikkoro Daimao.\n\n"
                                + "|2|Lối chơi chính:\n"
                                + "- Trâu hơn, sống dai hơn.\n"
                                + "- Thiên về hồi phục, chống chịu và hỗ trợ.\n"
                                + "- Hợp người thích farm lâu, ít chết, đánh chắc chắn.\n\n"
                                + "|6|Khi dùng set này:\n"
                                + "Đừng chỉ nhìn dame. Namếc mạnh ở độ ổn định, đặc biệt khi đi map khó hoặc boss cần sống lâu.\n\n"
                                + "|5|Mẹo: nếu hay bị quái/boss vả nằm đất, hãy cân nhắc gom set Namếc chuẩn hệ.",
                        "Shop\nUltra", "Hướng dẫn\nSKH", "Set\nTrái Đất", "Set\nNamếc", "Set\nXayda", "Đóng");
                case 4 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|7|━━━ SET KÍCH HOẠT XAYDA ━━━\n"
                                + "|1|Dành cho người chơi hệ Xayda. Các dòng thường gặp gồm: Cadic, Nappa, Kakarot/Galick.\n\n"
                                + "|2|Lối chơi chính:\n"
                                + "- Sát thương bùng nổ, đánh rất đã tay.\n"
                                + "- Hợp người thích giao tranh, săn boss và đua sức mạnh.\n"
                                + "- Có nhiều món đẹp nên thị trường thường săn nhiều.\n\n"
                                + "|6|Khi dùng set này:\n"
                                + "Xayda có sức mạnh cao nhưng cần đúng bộ mới ngon. Một món lẻ chưa nói lên gì, hãy gom đủ rồi hãy nâng cấp mạnh.\n\n"
                                + "|8|Nhắc nhẹ: tỉ lệ nhánh hiếm đã được giảm để tránh loạn chợ. Rơi được thì vui, chưa rơi thì đừng chửi Gohan nha.",
                        "Shop\nUltra", "Hướng dẫn\nSKH", "Set\nTrái Đất", "Set\nNamếc", "Set\nXayda", "Đóng");
                default -> {
                }
            }
        }
    }
}
