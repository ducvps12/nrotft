package nro.models.npc.npc_manifest;

/**
 * NPC King Furry Guide — Chuyên gia Tut & Trick
 * Đứng tại Siêu thị Huyền Bí (map 173)
 * Cung cấp mẹo kiếm Thỏi Vàng, Xu NRO, nuôi Đệ, cường hóa trang bị, farm boss
 * + Chợ Thông Tin: bán thông tin mật (vị trí boss) giá 10K vàng, update mỗi giờ
 */
import boss.Boss;
import boss.BossManager;
import boss.BossStatus;
import consts.ConstNpc;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.MapService;
import nro.services.Service;
import nro.services.TaskService;
import utils.Util;

public class KingFurryGuide extends Npc {

    // ===== CHỢ THÔNG TIN: cache vị trí boss, update mỗi giờ =====
    private static String cachedBossInfo = null;
    private static long lastBossInfoUpdate = 0;
    private static final long BOSS_INFO_INTERVAL = 3600_000L; // 1 giờ = 3600 giây
    private static final int BOSS_INFO_COST = 10_000;         // 10K vàng

    public KingFurryGuide(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            long sm = player.nPoint.power;
            String smStr = Util.mumberToLouis(sm);

            String info = "|7|━━━ KING FURRY ━━━\n"
                    + "|1|★ Chuyên gia Tut & Trick\n"
                    + "|7|━━━━━━━━━━━━━━━━━━\n"
                    + "|2|SM hiện tại: " + smStr + "\n"
                    + "\n"
                    + "|8|Ta sẽ chỉ cho ngươi cách\n"
                    + "|8|kiếm tài nguyên NHANH nhất!\n"
                    + "|8|Chọn chủ đề bên dưới:";

            // Trang 1: chỉ 4 button + Trang sau (không bị tràn)
            createOtherMenu(player, ConstNpc.MENU_KFG_MAIN, info,
                    "Thỏi Vàng\n★ HOT",
                    "Xu NRO\n& VNĐ",
                    "Đệ Tử\nPro",
                    "Trang sau\n▶");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.MENU_KFG_MAIN -> handleMain(player, select);
                case ConstNpc.MENU_KFG_MAIN_2 -> handleMain2(player, select);
                case ConstNpc.MENU_KFG_THOI_VANG -> { if (select == 0) openBaseMenu(player); }
                case ConstNpc.MENU_KFG_XU_NRO -> handleXuNroNav(player, select);
                case ConstNpc.MENU_KFG_XU_NRO_2 -> { if (select == 0) showXuNro(player); }
                case ConstNpc.MENU_KFG_DE_TU -> handleDeTuNav(player, select);
                case ConstNpc.MENU_KFG_DE_TU_2 -> { if (select == 0) showDeTu(player); }
                case ConstNpc.MENU_KFG_TRANG_BI -> { if (select == 0) showMainPage2(player); }
                case ConstNpc.MENU_KFG_BF_SECRET -> { if (select == 0) showMainPage2(player); }
                case ConstNpc.MENU_KFG_CHO_TT -> { if (select == 0) showMainPage2(player); }
                case ConstNpc.MENU_KFG_CHO_TT_CONFIRM -> handleChoTTConfirm(player, select);
            }
        }
    }

    // ========== TRANG 1: Thỏi Vàng, Xu NRO, Đệ Tử, Trang sau ==========
    private void handleMain(Player player, int select) {
        switch (select) {
            case 0 -> showThoiVang(player);
            case 1 -> showXuNro(player);
            case 2 -> showDeTu(player);
            case 3 -> showMainPage2(player);
            // Đóng = không có case
        }
    }

    // ========== TRANG 2: Trang Bị, Boss Farm, Chợ TT, Quay lại ==========
    private void showMainPage2(Player player) {
        String info = "|7|━━━ KING FURRY ━━━\n"
                + "|1|★ Chuyên gia Tut & Trick\n"
                + "|7|━━━━━━━━━━━━━━━━━━\n"
                + "\n"
                + "|8|Tiếp tục chọn chủ đề:\n"
                + "|2|★ Chợ Thông Tin: Mua tin mật\n"
                + "|2|  về vị trí Boss chỉ 10K vàng!";

        createOtherMenu(player, ConstNpc.MENU_KFG_MAIN_2, info,
                "Trang Bị\n& Ép Sao",
                "Boss\nFarm",
                "Chợ TT\n★ MỚI",
                "◀ Quay\nLại");
    }

    private void handleMain2(Player player, int select) {
        switch (select) {
            case 0 -> showTrangBi(player);
            case 1 -> showBossFarm(player);
            case 2 -> showChoThongTin(player);
            case 3 -> openBaseMenu(player);
        }
    }

    // ============ MẸO KIẾM THỎI VÀNG ============
    private void showThoiVang(Player player) {
        String tip = "|7|━━ MẸO KIẾM THỎI VÀNG ━━\n"
                + "\n"
                + "|1|1. Rồng Thần 1 Sao\n"
                + "|8|  Ước \"Giàu có\" = 100 Thỏi Vàng\n"
                + "|8|  + 100K ngọc xanh!\n"
                + "\n"
                + "|1|2. Gói VIP Tuần (Lý Tiểu Nương)\n"
                + "|8|  VIP 1: 100 TV | VIP 2: 500 TV\n"
                + "|8|  VIP 3: 1000 TV | mua 1 lần/tuần\n"
                + "\n"
                + "|1|3. Boss Rương Sưu Tập\n"
                + "|8|  Drop Thỏi Vàng ngẫu nhiên\n"
                + "|8|  Xuất hiện mỗi 10-15 phút\n"
                + "\n"
                + "|1|4. NV Quỷ Lão Kame\n"
                + "|8|  Hoàn thành NV = nhận TV + Ngọc\n"
                + "|8|  Reset mỗi ngày!\n"
                + "\n"
                + "|2|★ BÍ KÍP: Ưu tiên Rồng Thần 1 Sao\n"
                + "|2|  để lấy Thỏi Vàng + Ngọc combo!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_KFG_THOI_VANG, tip,
                "Quay Lại");
    }

    // ============ MẸO KIẾM XU NRO & VNĐ — TRANG 1 ============
    private void showXuNro(Player player) {
        String tip = "|7|━━ MẸO KIẾM XU NRO (1/2) ━━\n"
                + "\n"
                + "|1|1. Tháp PoPo (MrPoPo)\n"
                + "|8|  Tầng cao = nhiều Xu + Ngọc\n"
                + "|8|  Mỗi ngày reset, farm sớm!\n"
                + "\n"
                + "|1|2. Đổi Đá Ngũ Sắc → Xu (Osin)\n"
                + "|8|  1000 ĐNS = 1 Xu NRO\n"
                + "|8|  Farm ĐNS: đánh quái + Boss\n"
                + "\n"
                + "|1|3. Boss Training\n"
                + "|8|  Đánh Boss Luyện Tập = Xu NRO\n"
                + "|8|  Giới hạn 100 Xu/ngày\n"
                + "\n"
                + "|2|★ Nhấn \"Trang sau\" để xem tiếp!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_KFG_XU_NRO, tip,
                "Quay Lại",
                "Trang sau\n▶");
    }

    private void handleXuNroNav(Player player, int select) {
        switch (select) {
            case 0 -> openBaseMenu(player);
            case 1 -> showXuNroPage2(player);
        }
    }

    // ============ MẸO KIẾM XU NRO — TRANG 2 ============
    private void showXuNroPage2(Player player) {
        String tip = "|7|━━ MẸO KIẾM XU NRO (2/2) ━━\n"
                + "\n"
                + "|1|4. Đại Hội Võ Thuật\n"
                + "|8|  Round 4: 5 Xu | Round 8: 15 Xu\n"
                + "|8|  Vô địch: 50 Xu JACKPOT!\n"
                + "\n"
                + "|1|5. Đổi Xu → VNĐ (NPC Santa)\n"
                + "|8|  Quy đổi Xu NRO thành số dư\n"
                + "\n"
                + "|2|★ BÍ KÍP: Farm Tháp PoPo mỗi ngày\n"
                + "|2|  + Boss Training = ổn định nhất!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_KFG_XU_NRO_2, tip,
                "◀ Trang\ntrước");
    }

    // ============ MẸO NUÔI ĐỆ TỬ — TRANG 1 ============
    private void showDeTu(Player player) {
        String tip = "|7|━━ MẸO ĐỆ TỬ PRO (1/2) ━━\n"
                + "\n"
                + "|1|1. Chọn đệ phù hợp\n"
                + "|8|  Newbie: Mabu (rẻ, 50K)\n"
                + "|8|  Mid: Cell/Berus (đồ 3 hệ)\n"
                + "|8|  Pro: B.Goku (Fusion #1!)\n"
                + "\n"
                + "|1|2. VIP Đệ Tử = x2~x7 TNSM\n"
                + "|8|  Mua VIP Đệ tại Lý Tiểu Nương\n"
                + "|8|  Đệ mạnh GẤP BỘI so với thường\n"
                + "\n"
                + "|2|★ Nhấn \"Trang sau\" để xem tiếp!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_KFG_DE_TU, tip,
                "Quay Lại",
                "Trang sau\n▶");
    }

    private void handleDeTuNav(Player player, int select) {
        switch (select) {
            case 0 -> openBaseMenu(player);
            case 1 -> showDeTuPage2(player);
        }
    }

    // ============ MẸO NUÔI ĐỆ TỬ — TRANG 2 ============
    private void showDeTuPage2(Player player) {
        String tip = "|7|━━ MẸO ĐỆ TỬ PRO (2/2) ━━\n"
                + "\n"
                + "|1|3. Lộ trình Tuyệt Thế (FREE)\n"
                + "|8|  Farm Kilis: Map Cadic (187)\n"
                + "|8|  Tỉ lệ 1/333 (buff Osin: 10/333)\n"
                + "|8|  3K Kilis → Đệ 3K → 6K = T.Thế\n"
                + "\n"
                + "|1|4. Rồng Thần Đệ Tử\n"
                + "|8|  Ghép 7 NR Băng → Túi NR Băng\n"
                + "|8|  + NR Siêu Cấp = Gọi RT Đệ Tử\n"
                + "\n"
                + "|2|★ BÍ KÍP: Bật VIP Đệ rồi đánh\n"
                + "|2|  quái Cadic = farm nhanh nhất!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_KFG_DE_TU_2, tip,
                "◀ Trang\ntrước");
    }

    // ============ MẸO CƯỜNG HÓA TRANG BỊ ============
    private void showTrangBi(Player player) {
        String tip = "|7|━━ TRANG BỊ & ÉP SAO ━━\n"
                + "\n"
                + "|1|1. Set SKH: Mặc đủ 4 món\n"
                + "|8|  Áo+Quần+Găng+Giày cùng cấp\n"
                + "|8|  → Bonus chỉ số cực mạnh!\n"
                + "\n"
                + "|1|2. Ép Sao: Dùng Đá BV từ 3+\n"
                + "|8|  Sao 1-2: thoải mái | 5+: Đá BV!\n"
                + "\n"
                + "|1|3. Găng: RT 1 sao = +2 cấp\n"
                + "|8|  Max +3 (SĐ +20%/cấp)\n"
                + "\n"
                + "|1|4. Chân Mệnh: NPC Gohan\n"
                + "|8|  Cần ĐNS + vàng nâng item\n"
                + "\n"
                + "|2|★ Kích Set trước, ép sao sau!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_KFG_TRANG_BI, tip,
                "Quay Lại");
    }

    // ============ BÍ KÍP FARM BOSS ============
    private void showBossFarm(Player player) {
        String tip = "|7|━━ BÍ KÍP BOSS FARM ━━\n"
                + "\n"
                + "|1|1. Rồng Nhí 1-7 sao\n"
                + "|8|  Drop Bình Hút NL + ĐNS\n"
                + "|8|  Map: Bamboo, làng | farm dễ\n"
                + "\n"
                + "|1|2. Quỷ Lão/Jacky (Bón Hành)\n"
                + "|8|  Drop NR Băng (50%)\n"
                + "|8|  7 NR Băng → Túi NR Băng\n"
                + "\n"
                + "|1|3. Rương Sưu Tập\n"
                + "|8|  Drop Thỏi Vàng, đồ hiếm\n"
                + "\n"
                + "|1|4. Super Broly\n"
                + "|8|  Boss mạnh nhất, cần team!\n"
                + "\n"
                + "|2|★ Xem Thông Báo Boss (góc phải)\n"
                + "|2|  để biết vị trí Boss!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_KFG_BF_SECRET, tip,
                "Quay Lại");
    }

    // ============ CHỢ THÔNG TIN — Phiên chợ bán info boss 10K vàng ============
    private void showChoThongTin(Player player) {
        // Tính thời gian đến lần update tiếp theo
        long now = System.currentTimeMillis();
        long nextUpdate = lastBossInfoUpdate + BOSS_INFO_INTERVAL;
        long remain = Math.max(0, nextUpdate - now);
        int minutesLeft = (int) (remain / 60_000);

        String intro = "|7|━━ CHỢ THÔNG TIN MẬT ━━\n"
                + "\n"
                + "|1|★ Phiên chợ Tình Báo ★\n"
                + "|8|Ta có nguồn tin mật về vị trí\n"
                + "|8|các Boss đang hoạt động!\n"
                + "\n"
                + "|2|Giá: 10.000 vàng / 1 lần mua\n"
                + "|8|Thông tin cập nhật mỗi 1 giờ\n"
                + "\n"
                + "|8|Phiên chợ tiếp theo: "
                + (minutesLeft > 0 ? minutesLeft + " phút nữa" : "Đang mở!") + "\n"
                + "\n"
                + "|2|Muốn mua tin không?";

        createOtherMenu(player, ConstNpc.MENU_KFG_CHO_TT_CONFIRM, intro,
                "Mua Tin\n10K Vàng",
                "Quay Lại");
    }

    private void handleChoTTConfirm(Player player, int select) {
        switch (select) {
            case 0 -> buyBossInfo(player);
            case 1 -> showMainPage2(player);
        }
    }

    private void buyBossInfo(Player player) {
        // Kiểm tra đủ vàng
        if (player.inventory.gold < BOSS_INFO_COST) {
            Service.gI().sendThongBao(player,
                    "Không đủ vàng! Cần " + Util.numberToMoney(BOSS_INFO_COST) + " vàng.");
            return;
        }

        // Trừ vàng
        player.inventory.gold -= BOSS_INFO_COST;
        Service.gI().sendMoney(player);

        // Build hoặc dùng cache
        String info = getBossInfoCached();

        // Hiển thị
        createOtherMenu(player, ConstNpc.MENU_KFG_CHO_TT, info,
                "Quay Lại");
    }

    /**
     * Lấy thông tin boss có cache, update mỗi giờ
     */
    private static synchronized String getBossInfoCached() {
        long now = System.currentTimeMillis();
        if (cachedBossInfo == null || (now - lastBossInfoUpdate) >= BOSS_INFO_INTERVAL) {
            cachedBossInfo = buildBossInfo();
            lastBossInfoUpdate = now;
        }
        return cachedBossInfo;
    }

    /**
     * Build text thông tin boss đang hoạt động
     */
    private static String buildBossInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("|7|━━ TIN MẬT BOSS ━━\n");
        sb.append("|2|★ Cập nhật lúc: ");

        // Format thời gian hiện tại
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        sb.append(sdf.format(new java.util.Date()));
        sb.append("\n\n");

        int count = 0;
        int maxShow = 5; // Giới hạn 5 boss để không tràn UI

        try {
            java.util.List<Boss> bosses = BossManager.gI().getBosses();
            java.util.Set<String> shown = new java.util.HashSet<>();

            for (Boss boss : bosses) {
                if (count >= maxShow) break;
                if (boss == null || boss.bossStatus == null) continue;

                // Chỉ hiển thị boss chính (không hiện boss con)
                try {
                    java.lang.reflect.Field parentField = Boss.class.getDeclaredField("parentBoss");
                    parentField.setAccessible(true);
                    Object parent = parentField.get(boss);
                    if (parent != null) continue;
                } catch (Exception ignored) {}

                // Bỏ qua boss ở map đặc biệt (dùng zone hiện tại)
                if (boss.zone != null && boss.zone.map != null) {
                    int mapId = boss.zone.map.mapId;
                    if (MapService.gI().isMapBossFinal(mapId)
                            || MapService.gI().isMapHuyDiet(mapId)
                            || MapService.gI().isMapYardart(mapId)
                            || MapService.gI().isMapMaBu(mapId)
                            || MapService.gI().isMapBlackBallWar(mapId)) {
                        continue;
                    }
                }

                String bossName = boss.name != null ? boss.name : "Boss #" + boss.id;

                // Tránh trùng tên (nhiều instance cùng boss)
                if (shown.contains(bossName)) continue;

                // Chỉ show boss đang sống (bỏ REST để tiết kiệm text)
                if (boss.zone != null && boss.zone.map != null && !boss.isDie()) {
                    sb.append("|1|• ").append(bossName).append("\n");
                    sb.append("|8|  ").append(boss.zone.map.mapName)
                            .append(" - K").append(boss.zone.zoneId).append("\n");
                    shown.add(bossName);
                    count++;
                }
            }
        } catch (Exception e) {
            sb.append("|8|Lỗi khi thu thập tin!\n");
        }

        if (count == 0) {
            sb.append("|8|Hiện chưa có boss nào!\n");
        }

        sb.append("\n|7|━━━━━━━━━━━━━━━━━━");
        return sb.toString();
    }
}
