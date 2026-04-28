package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import event.EventManager;
import nro.models.npc.Npc;
import nro.player.Player;
import services.func.TopService;

/**
 * NPC bảng danh vọng đặt tại khu vực luyện tập để tân thủ dễ thấy các mục tiêu phấn đấu.
 */
public class BangDanhVong extends Npc {

    public BangDanhVong(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        createOtherMenu(player, ConstNpc.BASE_MENU,
                "|7|=== BANG TIN SU KIEN & DANH VONG ===\n\n"
                        + "|5|Dua Top Nap\n"
                        + "|2|Bang tin nay dat o lang de xem nhanh su kien, boss, giftcode.\n\n"
                        + "|1|Nen doc: Tin moi > Su kien > Boss",
                "Tin moi\nsu kien", "Huong dan\nsu kien", "Boss &\nthoi gian", "Top\nSuc manh", "Top\nNap", "Cam nang\ntan thu", "Ti le\nqua roi", "Dong");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player) || !player.iDMark.isBaseMenu()) {
            return;
        }
        switch (select) {
            case 0 -> showEventNews(player);
            case 1 -> showEventGuide(player);
            case 2 -> showBossOverview(player);
            case 3 -> TopService.gI().showListTopPower(player);
            case 4 -> TopService.gI().showListTopVnd(player);
            case 5 -> showGameOverview(player);
            case 6 -> showRewardRate(player);
            default -> {
            }
        }
    }

    private void showEventNews(Player player) {
        createOtherMenu(player, ConstNpc.IGNORE_MENU,
                "|7|=== TIN MOI & SU KIEN ===\n\n"
                        + "|5|Dang mo: |1|" + getActiveEventLine() + "\n\n"
                        + "|2|Thong bao:\n"
                        + "- Giftcode chi dung khi Admin bat Active\n"
                        + "- Reload Shop chi nap lai DB\n"
                        + "- Bang boss ben phai: xem va bam [Den]\n"
                        + "- Super Broly: danh Broly thuong den nguong\n\n"
                        + "|1|Sap toi: Su kien He 15/05",
                "Da hieu");
    }

    private void showEventGuide(Player player) {
        createOtherMenu(player, ConstNpc.IGNORE_MENU,
                "|7|=== HUONG DAN SU KIEN ===\n\n"
                        + "|2|1. Nhan thong tin:\n"
                        + "- Mo bang tin o lang moi ngay\n"
                        + "- Theo doi boss o khung phai\n\n"
                        + "|2|2. Farm su kien:\n"
                        + "- Uu tien nhiem vu, mo map, nang dau than\n"
                        + "- Doc mo ta NPC doi thuong truoc khi dung\n\n"
                        + "|2|3. Giftcode:\n"
                        + "- Code chua Active se bao chua kich hoat\n"
                        + "- Admin can Reload Giftcode de cap nhat",
                "Da hieu");
    }

    private String getActiveEventLine() {
        StringBuilder sb = new StringBuilder();
        if (EventManager.HUNG_VUONG) sb.append("Giỗ Tổ Hùng Vương, ");
        if (EventManager.TRUNG_THU) sb.append("Trung Thu, ");
        if (EventManager.HALLOWEEN) sb.append("Halloween, ");
        if (EventManager.CHRISTMAS) sb.append("Giáng Sinh, ");
        if (EventManager.LUNNAR_NEW_YEAR) sb.append("Tết Nguyên Đán, ");
        if (EventManager.INTERNATIONAL_WOMANS_DAY) sb.append("8/3, ");
        if (EventManager.TOP_UP) sb.append("Đua Top Nạp, ");
        if (EventManager.EVENT_POKEMON) sb.append("Pokemon, ");
        if (EventManager.TEACHERS_DAY) sb.append("20/11, ");
        if (EventManager.PHO_ANH_HAI) sb.append("Phở Anh Hai, ");
        if (sb.length() == 0) {
            return "Chưa có sự kiện mùa đang mở; hoạt động mặc định vẫn hoạt động.";
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private void showGameOverview(Player player) {
        createOtherMenu(player, ConstNpc.IGNORE_MENU,
                "|7|=== TONG QUAN CO CHE GAME ===\n\n"
                        + "|2|1. Lo trinh chinh:\n"
                        + "- Lam nhiem vu mo map\n"
                        + "- Qua khu luyen tap tang nen tang\n"
                        + "- Farm map SKH kiem Set Kich Hoat\n\n"
                        + "|2|2. Trang bi quan trong:\n"
                        + "- SKH: moc dau game\n"
                        + "- Do sao: phan nang cao\n\n"
                        + "|2|3. Tai nguyen:\n"
                        + "- Vang: mini game, giao dich\n"
                        + "- Ngoc: shop, su kien\n\n"
                        + "|5|Lo trinh tan thu:\n"
                        + "|1|Nhiem vu > Dau than > SKH > Boss",
                "Da hieu");
    }

    private void showBossOverview(Player player) {
        createOtherMenu(player, ConstNpc.IGNORE_MENU,
                "|7|━━━ BOSS & THỜI GIAN XUẤT HIỆN ━━━\n"
                        + "|1|Boss thường đang có nhiều nhánh chính:\n"
                        + "- Kuku / Mập đầu đinh / Rambo: mỗi loại 5 con.\n"
                        + "- Black Goku: nhiều bản thể săn ở các thành phố.\n"
                        + "- Golden Frieza: 5 con, độ khó cao hơn boss thường.\n"
                        + "- Broly thường: số lượng lớn, bị đánh sẽ tăng chỉ số dần. Khi đạt ngưỡng biến thân/rời map mới sinh Super Broly.\n"
                        + "- Cumber, Baby, Chiller, Cooler, Android, Cell, Bojack, Fide...\n"
                        + "- Mini boss: Sói Hẹc Quyn, Ô Đỏ, Xinbato... mỗi loại 5 con.\n\n"
                        + "|1|Lưu ý Super Broly:\n"
                        + "- Không nên hiện như boss tự spawn ngay từ đầu.\n"
                        + "- Luồng đúng: đánh Broly thường → Broly tăng HP/dame → đạt ngưỡng biến thân → Super Broly xuất hiện tại vị trí đó.\n\n"
                        + "|1|Thời gian:\n"
                        + "- Khung thông báo bên phải hiển thị boss, map và thời gian còn lại.\n"
                        + "- Nhiều boss hồi theo chu kỳ vài phút sau khi chết.\n"
                        + "- Boss sự kiện/bang hội/map đặc biệt có thể chỉ mở theo khung giờ riêng.\n\n"
                        + "|2|Mẹo săn: đọc thông báo boss → bấm [Đến] → chọn boss vừa sức, tránh tranh boss quá mạnh khi còn yếu.",
                "Đã hiểu");
    }

    private void showRewardRate(Player player) {
        createOtherMenu(player, ConstNpc.IGNORE_MENU,
                "|7|━━━ TỈ LỆ QUÀ & FARM ĐỒ ━━━\n"
                        + "|1|Map SKH:\n"
                        + "- Đồ kích hoạt: khoảng 1/5000 quái khi không dùng Cỏ bốn lá.\n"
                        + "- Có Cỏ bốn lá: khoảng 1/3500 quái.\n"
                        + "- Một số nhánh hiếm riêng có thể khó hơn, ví dụ Xayda khoảng 1/7000.\n\n"
                        + "|1|Đồ sao / vật phẩm phụ:\n"
                        + "- Có tỉ lệ rơi riêng, thấp hơn vật phẩm thường.\n"
                        + "- Chỉ số sao và option phụ phụ thuộc may mắn khi rơi/mở.\n\n"
                        + "|1|Boss:\n"
                        + "- Boss không phải lúc nào cũng rơi đồ hiếm.\n"
                        + "- Quà thường gồm vàng, vật phẩm sự kiện, mảnh, capsule hoặc đồ theo từng loại boss.\n"
                        + "- Boss càng khó/quý thì cạnh tranh càng cao, nên đi theo nhóm hoặc săn khung giờ vắng.\n\n"
                        + "|1|Rada / Ngọc rồng / vật phẩm đặc biệt:\n"
                        + "- Đây là nhóm vật phẩm giá trị, tỉ lệ và chỉ số có thể được admin cân bằng theo mùa.\n"
                        + "- Nếu thấy nguồn cung quá nhiều hoặc quá ít, hãy báo admin để chỉnh kinh tế.\n\n"
                        + "|2|Lưu ý: tỉ lệ là trung bình dài hạn, có thể 1000 quái đã ra hoặc 10000 quái chưa ra do may rủi.",
                "Đã hiểu");
    }
}
