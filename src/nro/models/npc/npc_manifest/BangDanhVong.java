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
                "|7|━━━ BẢNG TIN SỰ KIỆN & DANH VỌNG NROTFT ━━━\n"
                        + "|1|" + getActiveEventLine() + "\n"
                        + "|2|Bảng tin này đặt ở làng/khu luyện tập để người chơi xem nhanh sự kiện, boss, giftcode và hướng phát triển mà không phải đọc popup dài khi đăng nhập.\n"
                        + "|1|Nên đọc Tin mới → Sự kiện → Boss trước khi bắt đầu farm.",
                "Tin mới\nsự kiện", "Hướng dẫn\nsự kiện", "Boss &\nthời gian", "Top\nSức mạnh", "Top\nNạp", "Cẩm nang\ntân thủ", "Tỉ lệ\nquà rơi", "Đóng");
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
                "|7|━━━ TIN MỚI & TRẠNG THÁI SỰ KIỆN ━━━\n"
                        + "|1|Đang mở: " + getActiveEventLine() + "\n\n"
                        + "|2|Thông báo nhanh:\n"
                        + "- Giftcode chỉ dùng được khi Admin bật trạng thái Active trong panel.\n"
                        + "- Reload Shop chỉ nạp lại dữ liệu shop từ DB, không reload code/NPC.\n"
                        + "- Bảng boss bên phải dùng để xem boss đang/chuẩn bị xuất hiện và bấm [Đến].\n"
                        + "- Super Broly không phải boss tự nhiên: phải đánh Broly thường tới ngưỡng biến thân rồi mới kéo ra.\n\n"
                        + "|1|Sắp tới:\n"
                        + "- Sự kiện Hè được lên lịch mở từ 15/05, không nên bật sớm để tránh lệch vật phẩm/sự kiện.\n"
                        + "- Khi có thay đổi mới, Admin chỉ cần cập nhật bảng tin thay vì ép người chơi đọc popup dài lúc login.",
                "Đã hiểu");
    }

    private void showEventGuide(Player player) {
        createOtherMenu(player, ConstNpc.IGNORE_MENU,
                "|7|━━━ HƯỚNG DẪN SỰ KIỆN & HOẠT ĐỘNG ━━━\n"
                        + "|1|1. Nhận thông tin:\n"
                        + "- Mở bảng tin ở làng/khu luyện tập mỗi ngày để xem sự kiện đang chạy.\n"
                        + "- Theo dõi boss ở khung phải để chọn mục tiêu phù hợp sức mạnh.\n\n"
                        + "|1|2. Farm sự kiện:\n"
                        + "- Ưu tiên làm nhiệm vụ chính, mở map, nâng đậu thần rồi mới farm lâu dài.\n"
                        + "- Khi có vật phẩm sự kiện, đọc mô tả NPC đổi thưởng trước khi dùng để tránh phí nguyên liệu.\n\n"
                        + "|1|3. Giftcode / quà:\n"
                        + "- Code chưa Active sẽ báo chưa kích hoạt, không phải lỗi nhân vật.\n"
                        + "- Nếu code vừa được bật, Admin cần Reload Giftcode để cache đọc lại DB.\n\n"
                        + "|2|Gợi ý tối ưu: bảng tin này thay cho popup đăng nhập dài; popup chỉ nên để 3-5 dòng quan trọng nhất.",
                "Đã hiểu");
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
                "|7|━━━ TỔNG QUAN CƠ CHẾ GAME ━━━\n"
                        + "|1|1. Lộ trình chính:\n"
                        + "- Làm nhiệm vụ mở map, tăng sức mạnh và mở giới hạn.\n"
                        + "- Qua khu luyện tập để tăng nền tảng sức mạnh ban đầu.\n"
                        + "- Farm map SKH để kiếm Set Kích Hoạt, đồ sao và vật phẩm nâng tiến độ.\n\n"
                        + "|1|2. Trang bị quan trọng:\n"
                        + "- SKH là mốc đầu game: giúp mạnh nhanh và định hình lối chơi từng hành tinh.\n"
                        + "- Đồ sao / phụ kiện / cải trang là phần nâng cao, nên giữ đồ ngon để giao dịch hoặc dùng lâu dài.\n\n"
                        + "|1|3. Tài nguyên:\n"
                        + "- Vàng dùng cho cược mini game, giao dịch và nhiều hoạt động cơ bản.\n"
                        + "- Hồng ngọc/ngọc/ruby dùng cho shop, sự kiện hoặc tính năng đặc biệt tùy thời điểm.\n"
                        + "- Nạp ATM cần đúng ID nhân vật để hệ thống đối soát nhanh.\n\n"
                        + "|2|Gợi ý: tân thủ nên ưu tiên nhiệm vụ → đậu thần → SKH → boss vừa sức → bang hội.",
                "Đã hiểu");
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
