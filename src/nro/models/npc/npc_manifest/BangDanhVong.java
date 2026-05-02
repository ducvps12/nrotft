package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import event.EventManager;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.NpcService;
import services.func.TopService;

/**
 * NPC Hướng Dẫn Viên (Bảng Danh Vọng) - đặt tại Sân Vườn (map 131-133).
 * Khi người chơi mới ra khỏi nhà sang sân vườn sẽ thấy NPC này ngay.
 * Cung cấp tutorial chi tiết toàn bộ cơ chế game cho tân thủ.
 */
public class BangDanhVong extends Npc {

    // Menu index constants
    private static final int MENU_TUTORIAL_MAIN = 88001;
    private static final int MENU_QUEST_GUIDE = 88002;
    private static final int MENU_EQUIP_GUIDE = 88003;
    private static final int MENU_BEAN_GUIDE = 88004;
    private static final int MENU_BOSS_GUIDE = 88005;
    private static final int MENU_CLAN_GUIDE = 88006;
    private static final int MENU_ECONOMY_GUIDE = 88007;
    private static final int MENU_MAP_GUIDE = 88008;
    private static final int MENU_PVP_GUIDE = 88009;
    private static final int MENU_FLOW_GUIDE = 88010;
    private static final int MENU_TOP_MAIN = 88011;
    private static final int MENU_EVENT_INFO = 88012;
    private static final int MENU_MABU_GUIDE = 88013;

    public BangDanhVong(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        createOtherMenu(player, ConstNpc.BASE_MENU,
                "|7|━━ HƯỚNG DẪN VIÊN ━━\n\n"
                        + "|5|Chào mừng " + player.name + " đến với thế giới Dragon Ball!\n\n"
                        + "|2|Ta là người hướng dẫn, sẽ giúp con hiểu\n"
                        + "toàn bộ cơ chế game từ A đến Z.\n\n"
                        + "|1|Hãy chọn mục con muốn tìm hiểu:",
                "Lộ Trình\nTân Thủ",
                "Hướng Dẫn\nChi Tiết",
                "Sự Kiện\n& Tin Mới",
                "Xếp Hạng\n& Top",
                "Đóng");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        switch (player.iDMark.getIndexMenu()) {
            case ConstNpc.BASE_MENU -> handleBaseMenu(player, select);
            case MENU_TUTORIAL_MAIN -> handleTutorialMain(player, select);
            case MENU_FLOW_GUIDE -> handleFlowGuide(player, select);
            case MENU_QUEST_GUIDE -> handleBackToTutorial(player, select);
            case MENU_EQUIP_GUIDE -> handleBackToTutorial(player, select);
            case MENU_BEAN_GUIDE -> handleBackToTutorial(player, select);
            case MENU_BOSS_GUIDE -> handleBackToTutorial(player, select);
            case MENU_CLAN_GUIDE -> handleBackToTutorial(player, select);
            case MENU_ECONOMY_GUIDE -> handleBackToTutorial(player, select);
            case MENU_MAP_GUIDE -> handleBackToTutorial(player, select);
            case MENU_PVP_GUIDE -> handleBackToTutorial(player, select);
            case MENU_MABU_GUIDE -> handleBackToTutorial(player, select);
            case MENU_TOP_MAIN -> handleTopMenu(player, select);
            case MENU_EVENT_INFO -> handleBackToBase(player, select);
            default -> {}
        }
    }

    // ========== MENU GỐC ==========
    private void handleBaseMenu(Player player, int select) {
        switch (select) {
            case 0 -> showFlowGuide(player);
            case 1 -> showTutorialMain(player);
            case 2 -> showEventInfo(player);
            case 3 -> showTopMenu(player);
        }
    }

    // ========== LỘ TRÌNH TÂN THỦ ==========
    private void showFlowGuide(Player player) {
        createOtherMenu(player, MENU_FLOW_GUIDE,
                "|7|━━ LỘ TRÌNH TÂN THỦ ━━\n\n"
                        + "|5|BƯỚC 1: Làm nhiệm vụ chính\n"
                        + "|1|- Nói chuyện NPC ông nội (Gohan/Moori/Paragus)\n"
                        + "- Làm theo hướng dẫn, mở dần các map\n"
                        + "- Thu thập đùi gà, đánh mộc nhân...\n\n"
                        + "|5|BƯỚC 2: Nâng sức mạnh\n"
                        + "|1|- Thu hoạch đậu thần mỗi ngày\n"
                        + "- Dùng tiềm năng tăng HP/KI/Sức đánh\n"
                        + "- Mua trang bị từ Bunma/Dende/Appule\n\n"
                        + "|5|BƯỚC 3: Trang bị SKH\n"
                        + "|1|- Farm quái ở map SKH (map Fide)\n"
                        + "- Kích hoạt set SKH = sức mạnh x10\n\n"
                        + "|5|BƯỚC 4: Săn Boss & Endgame\n"
                        + "|1|- Săn boss nhận đồ hiếm\n"
                        + "- Tham gia bang hội, phó bản\n"
                        + "- PVP, đại hội võ thuật",
                "Xem thêm\nChi tiết", "Quay lại");
    }

    private void handleFlowGuide(Player player, int select) {
        switch (select) {
            case 0 -> showTutorialMain(player);
            case 1 -> openBaseMenu(player);
        }
    }

    // Quay lại menu tutorial từ sub-guide
    private void handleBackToTutorial(Player player, int select) {
        if (select == 0) {
            showTutorialMain(player);
        }
    }

    // Quay lại menu gốc
    private void handleBackToBase(Player player, int select) {
        if (select == 0) {
            openBaseMenu(player);
        }
    }

    // ========== HƯỚNG DẪN CHI TIẾT ==========
    private void showTutorialMain(Player player) {
        createOtherMenu(player, MENU_TUTORIAL_MAIN,
                "|7|━━ HƯỚNG DẪN CHI TIẾT ━━\n\n"
                        + "|2|Chọn chủ đề muốn tìm hiểu:",
                "Nhiệm Vụ\n& Mở Map",
                "Trang Bị\n& SKH",
                "Đậu Thần\n& Hồi Phục",
                "Boss &\nSăn Đồ",
                "Bang Hội\n& Phó Bản",
                "Kinh Tế\n& Nạp",
                "Map Đặc\nBiệt",
                "PVP &\nMini Game",
                "Map MaBu\n& Kilis");
    }

    private void handleTutorialMain(Player player, int select) {
        switch (select) {
            case 0 -> showQuestGuide(player);
            case 1 -> showEquipGuide(player);
            case 2 -> showBeanGuide(player);
            case 3 -> showBossGuide(player);
            case 4 -> showClanGuide(player);
            case 5 -> showEconomyGuide(player);
            case 6 -> showMapGuide(player);
            case 7 -> showPvpGuide(player);
            case 8 -> showMabuGuide(player);
        }
    }

    // ========== 1. NHIỆM VỤ ==========
    private void showQuestGuide(Player player) {
        createOtherMenu(player, MENU_QUEST_GUIDE,
                "|7|━━ NHIỆM VỤ & MỞ MAP ━━\n\n"
                        + "|5|Nhiệm vụ chính:\n"
                        + "|1|- Bắt đầu tại Làng → về Nhà → ra Sân Vườn (đây)\n"
                        + "- Nói chuyện NPC ông nội để nhận nhiệm vụ\n"
                        + "- Hoàn thành từng bước: đánh quái, nhặt đồ, nói NPC\n"
                        + "- Mỗi chuỗi NV mở thêm map mới\n\n"
                        + "|5|Map mở theo tiến trình:\n"
                        + "|1|- NV 1-3: Làng → Đồi → Thung lũng\n"
                        + "- NV 4-6: Trạm tàu vũ trụ\n"
                        + "- NV 7-12: Rừng, đảo, khu vực mạnh hơn\n"
                        + "- NV 13+: Map Fide, map boss, liên hành tinh\n"
                        + "- NV 23+: Map Tương Lai (Android)\n"
                        + "- NV 29+: Map Cold (hành tinh băng)\n\n"
                        + "|5|Mẹo:\n"
                        + "|1|- Xem mũi tên chỉ đường trên màn hình\n"
                        + "- Dùng capsule bay nhanh đến map đã mở\n"
                        + "- Hỗ trợ NV khó: nói chuyện NPC ông nội",
                "Quay lại");
    }

    // ========== 2. TRANG BỊ ==========
    private void showEquipGuide(Player player) {
        createOtherMenu(player, MENU_EQUIP_GUIDE,
                "|7|━━ TRANG BỊ & SKH ━━\n\n"
                        + "|5|Các loại trang bị:\n"
                        + "|1|- Đồ thường: mua từ shop Bunma/Dende/Appule\n"
                        + "- Đồ Kích Hoạt (SKH): farm từ quái map Fide\n"
                        + "- Đồ Sao: nâng cấp từ đồ SKH\n"
                        + "- Đồ Thiên Sứ: cao cấp nhất\n\n"
                        + "|5|Cách farm SKH:\n"
                        + "|1|- Đến map Fide (NV 20+): trại lính, vực chết...\n"
                        + "- Đánh quái rơi mảnh trang bị\n"
                        + "- Tỉ lệ drop ~1/5000 quái (có Cỏ bốn lá ~1/3500)\n"
                        + "- Thu đủ 4 món (áo, quần, găng, giày) = Kích Hoạt\n\n"
                        + "|5|Kích Hoạt Set:\n"
                        + "|1|- Đủ 4 món cùng loại → bonus sức mạnh cực lớn\n"
                        + "- Nâng cấp SKH VIP cần: Lọ Sơn + Thiên Sứ + 2 SKH\n"
                        + "- Nói chuyện Bà Hạt Mít để ép đồ, nâng cấp",
                "Quay lại");
    }

    // ========== 3. ĐẬU THẦN ==========
    private void showBeanGuide(Player player) {
        createOtherMenu(player, MENU_BEAN_GUIDE,
                "|7|━━ ĐẬU THẦN & HỒI PHỤC ━━\n\n"
                        + "|5|Đậu thần là gì?\n"
                        + "|1|- Cây đậu thần ở NHÀ (bên cạnh ông nội)\n"
                        + "- Thu hoạch đậu → hồi HP/KI khi chiến đấu\n"
                        + "- Nhấn nút trái tim (góc phải dưới) để dùng\n\n"
                        + "|5|Nâng cấp đậu thần:\n"
                        + "|1|- Nói chuyện cây Đậu Thần → Nâng cấp\n"
                        + "- Cấp càng cao → càng nhiều đậu mỗi lần thu\n"
                        + "- Chi phí nâng: vàng + ngọc (tăng dần)\n\n"
                        + "|5|Các loại hồi phục khác:\n"
                        + "|1|- Đùi gà: nhặt từ quái\n"
                        + "- Bánh: mua từ shop Santa\n"
                        + "- Bùa hỗ trợ: mua từ Bà Hạt Mít\n"
                        + "- Siêu Thần Thủy: hồi full HP (map đặc biệt)",
                "Quay lại");
    }

    // ========== 4. BOSS ==========
    private void showBossGuide(Player player) {
        createOtherMenu(player, MENU_BOSS_GUIDE,
                "|7|━━ BOSS & SĂN ĐỒ ━━\n\n"
                        + "|5|Boss thường:\n"
                        + "|1|- Kuku, Mập Đầu Đinh, Rambo: boss đầu game\n"
                        + "- Số 1-4, Tiểu Đội Trưởng: boss Fide\n"
                        + "- Black Goku, Golden Frieza: boss mạnh\n"
                        + "- Broly → Super Broly: đánh đến ngưỡng biến thân\n\n"
                        + "|5|Boss đặc biệt:\n"
                        + "|1|- Xên Bọ Hung: 3 dạng biến thân\n"
                        + "- MaBu: map phong ấn sức mạnh\n"
                        + "- Android: map Tương Lai\n"
                        + "- Cold: hành tinh băng (-50% chỉ số)\n\n"
                        + "|5|Cách săn:\n"
                        + "|1|- Xem khung thông báo boss bên PHẢI màn hình\n"
                        + "- Bấm [Đến] để bay tới map boss\n"
                        + "- Boss hồi sinh theo chu kỳ vài phút\n"
                        + "- Đi nhóm/bang để săn boss mạnh hiệu quả hơn",
                "Quay lại");
    }

    // ========== 5. BANG HỘI ==========
    private void showClanGuide(Player player) {
        createOtherMenu(player, MENU_CLAN_GUIDE,
                "|7|━━ BANG HỘI & PHÓ BẢN ━━\n\n"
                        + "|5|Tạo/gia nhập bang:\n"
                        + "|1|- Đến NPC Bò Mộng để tạo bang hoặc gia nhập\n"
                        + "- Bang cần tối thiểu vài thành viên\n"
                        + "- Bang chủ quản lý, phong phó chủ\n\n"
                        + "|5|Phó bản bang hội:\n"
                        + "|1|- Trại Độc Nhãn: 2+ người, boss giữ ngọc rồng\n"
                        + "- Bản Đồ Kho Báu: khám phá hang động\n"
                        + "- Khí Gas Hủy Diệt: 4 địa điểm đặc biệt\n"
                        + "- Con Đường Rắn Độc: thử thách khó\n"
                        + "- Mỗi phó bản có 30 phút, hết giờ bị đưa về nhà\n\n"
                        + "|5|Hoạt động bang:\n"
                        + "|1|- Nhiệm vụ bang: cày quái cùng thành viên\n"
                        + "- Gọi Rồng Thần Namếc: cần 7 ngọc rồng Namếc\n"
                        + "- Tranh Ngọc Rồng Sao Đen: PvP bang 20h-21h\n"
                        + "- Boss Bang Hội: đánh boss riêng cho bang",
                "Quay lại");
    }

    // ========== 6. KINH TẾ ==========
    private void showEconomyGuide(Player player) {
        createOtherMenu(player, MENU_ECONOMY_GUIDE,
                "|7|━━ KINH TẾ & NẠP ━━\n\n"
                        + "|5|Các loại tiền tệ:\n"
                        + "|1|- Vàng: đánh quái, bán đồ, mini game\n"
                        + "- Ngọc xanh: nhận free, sự kiện\n"
                        + "- Hồng Ngọc: nạp VNĐ đổi\n"
                        + "- Thỏi vàng: nạp VNĐ đổi, rất quý\n\n"
                        + "|5|Cách nạp:\n"
                        + "|1|- Vào NHÀ → nói NPC ông nội → Nạp Tiền\n"
                        + "- Chuyển khoản ATM/QR → tự cộng VNĐ\n"
                        + "- Đổi VNĐ → Thỏi vàng hoặc Hồng Ngọc\n"
                        + "- Bonus nạp: mệnh giá càng cao, bonus càng lớn\n\n"
                        + "|5|Shop:\n"
                        + "|1|- Bunma/Dende/Appule: trang bị cơ bản\n"
                        + "- Santa: trang phục, capsule\n"
                        + "- Uron: sách kỹ năng\n"
                        + "- Bà Hạt Mít: bùa, ép đồ, nâng cấp\n"
                        + "- Cửa hàng Ký Gửi: mua bán giữa người chơi",
                "Quay lại");
    }

    // ========== 7. MAP ĐẶC BIỆT ==========
    private void showMapGuide(Player player) {
        createOtherMenu(player, MENU_MAP_GUIDE,
                "|7|━━ MAP ĐẶC BIỆT ━━\n\n"
                        + "|5|Khu vực tập luyện:\n"
                        + "|1|- Thần Mèo → Mr.PoPo → Thượng Đế...\n"
                        + "- Tập offline: sức mạnh tăng khi logout\n\n"
                        + "|5|Map Tương Lai (NV 23+):\n"
                        + "|1|- Gặp Calick/Bunma Tương Lai\n"
                        + "- Đánh Android sát thủ\n\n"
                        + "|5|Hành tinh Cold (NV 29+):\n"
                        + "|1|- HP và sức đánh giảm 50%!\n"
                        + "- Boss Cold cực mạnh\n\n"
                        + "|5|Map MaBu:\n"
                        + "|1|- Phong ấn sức mạnh, tích điểm TL\n"
                        + "- Hạ đối thủ/boss để xuống tầng\n\n"
                        + "|5|Ngũ Hành Sơn (map 122-124):\n"
                        + "|1|- Cần dịch chuyển tức thời (Yardrat)\n"
                        + "- Khu vực endgame",
                "Quay lại");
    }

    // ========== 8. PVP ==========
    private void showPvpGuide(Player player) {
        createOtherMenu(player, MENU_PVP_GUIDE,
                "|7|━━ PVP & MINI GAME ━━\n\n"
                        + "|5|PVP - Đấu tay đôi:\n"
                        + "|1|- Chạm vào người chơi khác → Thách đấu\n"
                        + "- Cả 2 bên phải đồng ý mới bắt đầu\n\n"
                        + "|5|Đại Hội Võ Thuật:\n"
                        + "|1|- Nhiều giải: Nhi đồng → Siêu cấp → Ngoại hạng\n"
                        + "- Lịch thi đấu theo giờ mỗi ngày\n"
                        + "- Giải thưởng: ngọc, đá nâng cấp\n\n"
                        + "|5|Đại Hội Võ Thuật Liên Vũ Trụ:\n"
                        + "|1|- NPC Bill tổ chức theo lịch tuần\n"
                        + "- Top 1: Rađa cấp 13 + Capsule Vàng\n\n"
                        + "|5|Tranh Ngọc Rồng Sao Đen:\n"
                        + "|1|- 20h-21h mỗi ngày, PvP bang hội\n"
                        + "- Giữ ngọc 5 phút liên tục = thắng\n"
                        + "- Phần thưởng buff cả bang 1 ngày\n\n"
                        + "|5|Oẳn Tù Tì:\n"
                        + "|1|- Chạm người chơi → chơi OTT cược 5tr vàng",
                "Quay lại");
    }

    // ========== 9. MAP MABU & KILIS ==========
    private void showMabuGuide(Player player) {
        createOtherMenu(player, MENU_MABU_GUIDE,
                "|7|━━ MAP MABU & KILIS ━━\n\n"
                        + "|5|Cách vào Map MaBu:\n"
                        + "|1|- NPC Ôsin tại Đại Hội Võ Thuật (map 52)\n"
                        + "- Mở từ 12h hàng ngày, bấm OK để vào\n"
                        + "- MaBu 14H: mở riêng cho bản nâng cao\n\n"
                        + "|5|Cơ chế phong ấn:\n"
                        + "|1|- Tất cả sức mạnh bị phong ấn như nhau\n"
                        + "- Tích 10 điểm Tích Lũy (TL) = xuống tầng\n"
                        + "- Hạ người chơi hoặc boss để tích điểm\n"
                        + "- Giải trừ phong ấn: 1 ngọc\n\n"
                        + "|5|Hệ thống 6 tầng:\n"
                        + "|1|- T1: Drabura → T2: BuiBui\n"
                        + "- T3: BuiBui 2 → T4: YaCon\n"
                        + "- T5: Drabura 2 → T6: MaBu (Boss cuối)\n\n"
                        + "|5|Phe phái (PvP nội bộ):\n"
                        + "|1|- Phe Kaiô (Ôsin) vs Phe Babiday\n"
                        + "- Babiday thôi miên (1%) đổi phe\n"
                        + "- Ôsin giải bùa (2%) về phe gốc\n\n"
                        + "|5|Farm Kilis (map 187):\n"
                        + "|1|- Cần item Bình hút năng lượng\n"
                        + "- 100 hồng ngọc = 10p buff hút x2\n"
                        + "- Kilis dùng tiến hóa đệ tử\n\n"
                        + "|5|MaBu 14H (map 127-128):\n"
                        + "|1|- Phù hộ: 10 ngọc = +1tr HP/KI/10k SĐ\n"
                        + "- Boss: MaBu + Super Bu",
                "Quay lại");
    }

    // ========== SỰ KIỆN ==========
    private void showEventInfo(Player player) {
        createOtherMenu(player, MENU_EVENT_INFO,
                "|7|━━ SỰ KIỆN & TIN MỚI ━━\n\n"
                        + "|5|Sự kiện đang mở:\n"
                        + "|1|" + getActiveEventLine() + "\n\n"
                        + "|5|Thông báo:\n"
                        + "|1|- Xem boss: khung thông báo bên PHẢI\n"
                        + "- Bấm [Đến] để bay tới map boss\n"
                        + "- GiftCode: vào Nhà → nói NPC ông nội\n"
                        + "- Nạp tiền: vào Nhà → NPC ông nội → Nạp Tiền\n\n"
                        + "|2|Mẹo: quay lại đây bất cứ lúc nào\n"
                        + "để xem hướng dẫn!",
                "Quay lại");
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
            return "Chưa có sự kiện mùa nào đang mở.";
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    // ========== XẾP HẠNG ==========
    private void showTopMenu(Player player) {
        createOtherMenu(player, MENU_TOP_MAIN,
                "|7|━━ XẾP HẠNG ━━\n\n"
                        + "|2|Xem bảng xếp hạng người chơi:",
                "Top\nSức Mạnh",
                "Top\nNạp",
                "Đóng");
    }

    private void handleTopMenu(Player player, int select) {
        switch (select) {
            case 0 -> TopService.showListTopPower(player);
            case 1 -> TopService.showListTopVnd(player);
        }
    }
}
