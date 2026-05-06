package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

/**
 * Champa — Panel bán đồ rác
 * Mở panel combine để player kéo-thả item vào, thấy giá trị quy đổi
 * rồi mới xác nhận bán.
 *
 * PHÂN LOẠI:
 * 1. Đồ rác thường (ngọc rồng, đá nâng cấp, thức ăn) → Quy đổi về VÀNG + NGỌC XANH
 * 2. Cải trang lỗi (type 5 - không option hoặc chỉ số thấp) → 1 Thỏi Vàng (khóa) / món
 * 3. Trang bị lỗi (type 0-4 - không option) → Quy đổi về VÀNG theo cấp
 */
public class ChampaBanDoRac {

    // Thỏi vàng
    private static final int THOI_VANG = 457;

    // Ngọc Rồng 1-7 sao (template ID 14-20)
    private static final int NGOC_RONG_1_SAO = 14;
    private static final int NGOC_RONG_7_SAO = 20;

    // Ngưỡng chỉ số để cải trang được coi là "lỗi"
    // Tổng chỉ số < ngưỡng này → cải trang lỗi
    private static final int CAI_TRANG_CHI_SO_THAP = 10;

    /**
     * Hiển thị thông tin khi player đặt item vào panel combine
     */
    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.isEmpty()) {
            Service.gI().sendThongBao(player,
                    "Hãy đặt đồ rác vào để ta định giá!\n"
                    + "Chấp nhận: TB lỗi, cải trang lỗi, thức ăn,\n"
                    + "đá nâng cấp thường, ngọc rồng thường");
            return;
        }

        // Tính giá trị quy đổi cho từng item
        long tongVang = 0;
        int tongNgocXanh = 0;
        int tongThoi = 0;       // Thỏi vàng (chỉ cho cải trang lỗi)
        int tongMon = 0;
        StringBuilder danhSach = new StringBuilder();

        for (Item item : player.combine.itemsCombine) {
            if (item == null || !item.isNotNullItem()) continue;

            if (!isJunkItem(item)) {
                Service.gI().sendThongBao(player,
                        item.template.name + " không phải đồ rác!\n"
                        + "Vui lòng bỏ ra và chỉ đặt đồ hợp lệ.");
                return;
            }

            int qty = item.quantity;
            tongMon += qty;

            if (isBrokenCaiTrang(item)) {
                // Cải trang lỗi → 1 Thỏi Vàng / món
                int thoiItem = qty;
                tongThoi += thoiItem;
                danhSach.append("\n|3|").append(item.template.name)
                        .append(" x").append(qty)
                        .append(" = ").append(thoiItem).append(" TV");
            } else {
                // Đồ rác thường → Vàng + Ngọc Xanh
                long vangPerUnit = getGoldPrice(item);
                int ngocPerUnit = getGemPrice(item);
                long vangItem = vangPerUnit * qty;
                int ngocItem = ngocPerUnit * qty;
                tongVang += vangItem;
                tongNgocXanh += ngocItem;

                // Hiển thị
                StringBuilder line = new StringBuilder();
                line.append("\n|2|").append(item.template.name)
                    .append(" x").append(qty).append(" = ");
                if (vangItem > 0) {
                    line.append(Util.numberToMoney(vangItem)).append(" vàng");
                }
                if (ngocItem > 0) {
                    if (vangItem > 0) line.append(" + ");
                    line.append(Util.numberToMoney(ngocItem)).append(" ngọc");
                }
                danhSach.append(line);
            }
        }

        player.combine.goldCombine = 0;
        player.combine.ratioCombine = 100;

        // Build hiển thị tổng
        StringBuilder tongNhan = new StringBuilder();
        if (tongVang > 0) {
            tongNhan.append("|2|").append(Util.numberToMoney(tongVang)).append(" Vàng");
        }
        if (tongNgocXanh > 0) {
            if (tongVang > 0) tongNhan.append("\n");
            tongNhan.append("|6|+").append(Util.numberToMoney(tongNgocXanh)).append(" Ngọc Xanh");
        }
        if (tongThoi > 0) {
            if (tongVang > 0 || tongNgocXanh > 0) tongNhan.append("\n");
            tongNhan.append("|3|+").append(tongThoi).append(" Thỏi Vàng (khóa)");
        }

        String npcSay = "|1|=== ĐỊNH GIÁ ĐỒ RÁC ===\n"
                + "|7|Đã chọn " + tongMon + " món:" + danhSach
                + "\n\n|1|━━━━━━━━━━━━━━━━"
                + "\n|7|Tổng nhận:\n" + tongNhan;

        // Build nút bấm
        String btnBan = "Bán " + tongMon + " món";
        // Dùng NPC đang chọn (Champa) thay vì baHatMit để menu popup đúng NPC
        Npc npcChose = player.iDMark.getNpcChose();
        if (npcChose != null) {
            npcChose.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    btnBan, "Không bán");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    btnBan, "Không bán");
        }
    }

    /**
     * Thực hiện bán đồ rác đã chọn
     */
    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.isEmpty()) {
            Service.gI().sendThongBao(player, "Không có đồ rác nào để bán!");
            return;
        }

        // Validate + tính tổng
        long tongVang = 0;
        int tongNgocXanh = 0;
        int tongThoi = 0;
        int tongMon = 0;

        for (Item item : player.combine.itemsCombine) {
            if (item == null || !item.isNotNullItem() || !isJunkItem(item)) {
                Service.gI().sendThongBao(player, "Có vật phẩm không hợp lệ! Vui lòng thử lại.");
                return;
            }
            int qty = item.quantity;
            tongMon += qty;

            if (isBrokenCaiTrang(item)) {
                tongThoi += qty;
            } else {
                tongVang += getGoldPrice(item) * qty;
                tongNgocXanh += getGemPrice(item) * qty;
            }
        }

        // Kiểm tra ô trống nếu cần thỏi vàng
        if (tongThoi > 0 && InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đầy! Cần 1 ô trống để nhận Thỏi Vàng.");
            return;
        }

        // Xóa đồ rác đã chọn
        for (Item item : player.combine.itemsCombine) {
            InventoryService.gI().subQuantityItemsBag(player, item, item.quantity);
        }

        // Cộng Vàng
        if (tongVang > 0) {
            player.inventory.addGoldSafe(tongVang);
        }

        // Cộng Ngọc Xanh
        if (tongNgocXanh > 0) {
            player.inventory.gem += tongNgocXanh;
        }

        // Thêm Thỏi Vàng (khóa) nếu có cải trang lỗi
        if (tongThoi > 0) {
            Item thoi = ItemService.gI().createNewItem((short) THOI_VANG, tongThoi);
            thoi.itemOptions.add(new Item.ItemOption(30, 1)); // Khóa giao dịch
            InventoryService.gI().addItemBag(player, thoi);
        }

        // Hiệu ứng thành công
        CombineService.gI().sendEffectSuccessCombine(player);

        // Thông báo kết quả
        StringBuilder ketQua = new StringBuilder();
        ketQua.append("Bán thành công ").append(tongMon).append(" món!\n");
        if (tongVang > 0) {
            ketQua.append("Nhận +").append(Util.numberToMoney(tongVang)).append(" Vàng\n");
        }
        if (tongNgocXanh > 0) {
            ketQua.append("Nhận +").append(Util.numberToMoney(tongNgocXanh)).append(" Ngọc Xanh\n");
        }
        if (tongThoi > 0) {
            ketQua.append("Nhận +").append(tongThoi).append(" Thỏi Vàng (khóa)");
        }

        Service.gI().sendThongBao(player, ketQua.toString().trim());
        Service.gI().chat(player, "Bán " + tongMon + " đồ rác cho Champa!");

        Service.gI().sendMoney(player);
        InventoryService.gI().sendItemBag(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    // ===================== HELPER METHODS =====================

    /**
     * Kiểm tra item có phải đồ rác được thu mua không
     */
    public static boolean isJunkItem(Item item) {
        if (item == null || item.template == null) return false;
        int type = item.template.type;
        int id = item.template.id;

        // Trang bị lỗi (áo, quần, găng, giày, radar - type 0-4)
        // Không option → rác
        if (type >= 0 && type <= 4) {
            if (item.itemOptions == null || item.itemOptions.isEmpty()) {
                return true;
            }
        }

        // Cải trang lỗi (type 5):
        // Không option HOẶC chỉ số thấp → rác
        if (type == 5) {
            if (isBrokenCaiTrang(item)) {
                return true;
            }
        }

        // Thức ăn thường (type 12 = Thức ăn hồi máu/ki)
        if (type == 12) {
            return true;
        }

        // Đá nâng cấp thường (type 14 = Đá nâng cấp)
        // Loại trừ đá quý hiếm: Đá Bảo Vệ (987), Đá Ngũ Sắc (674)
        if (type == 14) {
            if (id != 987 && id != 674) {
                return true;
            }
        }

        // Ngọc Rồng thường 1-7 sao (ID 14-20)
        if (id >= NGOC_RONG_1_SAO && id <= NGOC_RONG_7_SAO) {
            return true;
        }

        return false;
    }

    /**
     * Kiểm tra cải trang có phải "lỗi" không
     * Lỗi = không có option HOẶC tổng chỉ số quá thấp
     */
    private static boolean isBrokenCaiTrang(Item item) {
        if (item == null || item.template == null) return false;
        if (item.template.type != 5) return false;

        // Không có option → lỗi
        if (item.itemOptions == null || item.itemOptions.isEmpty()) {
            return true;
        }

        // Tính tổng chỉ số (bỏ qua option khóa, hạn sử dụng, v.v.)
        int tongChiSo = 0;
        boolean hasRealOption = false;
        for (Item.ItemOption opt : item.itemOptions) {
            if (opt == null || opt.optionTemplate == null) continue;
            int opId = opt.optionTemplate.id;
            // Bỏ qua option không phải chỉ số: khóa GD(30), HSD(93), v.v.
            if (opId == 30 || opId == 93 || opId == 188) continue;
            tongChiSo += Math.abs(opt.param);
            hasRealOption = true;
        }

        // Không có option chỉ số thực → lỗi
        if (!hasRealOption) return true;

        // Tổng chỉ số quá thấp → lỗi
        return tongChiSo < CAI_TRANG_CHI_SO_THAP;
    }

    /**
     * Giá bán quy đổi ra VÀNG (gold) cho đồ rác thường
     * Cải trang lỗi KHÔNG dùng hàm này (dùng Thỏi Vàng)
     */
    private static long getGoldPrice(Item item) {
        if (item == null || item.template == null) return 0;
        int type = item.template.type;
        int level = item.template.level;
        int id = item.template.id;

        // Ngọc Rồng 1-7 sao → giá Vàng theo sao
        if (id >= NGOC_RONG_1_SAO && id <= NGOC_RONG_7_SAO) {
            int sao = id - NGOC_RONG_1_SAO + 1;
            return switch (sao) {
                case 7 -> 5_000_000L;  // 5 triệu vàng
                case 6 -> 3_000_000L;  // 3 triệu vàng
                case 5 -> 2_000_000L;  // 2 triệu vàng
                case 4 -> 1_000_000L;  // 1 triệu vàng
                case 3 -> 500_000L;    // 500k vàng
                case 2 -> 200_000L;    // 200k vàng
                default -> 100_000L;   // 100k vàng (1 sao)
            };
        }

        // Trang bị lỗi (type 0-4) → Vàng theo cấp
        if (type >= 0 && type <= 4) {
            if (level >= 12) return 10_000_000L;   // 10 triệu
            if (level >= 9) return 5_000_000L;     // 5 triệu
            if (level >= 5) return 2_000_000L;     // 2 triệu
            return 500_000L;                        // cấp 1-4: 500k
        }

        // Đá nâng cấp (type 14) → 1 triệu vàng/viên
        if (type == 14) {
            return 1_000_000L;
        }

        // Thức ăn (type 12) → 100k vàng/cái
        if (type == 12) {
            return 100_000L;
        }

        return 100_000L; // Mặc định
    }

    /**
     * Giá bán quy đổi ra NGỌC XANH (gem) cho đồ rác thường
     * Cải trang lỗi KHÔNG dùng hàm này
     */
    private static int getGemPrice(Item item) {
        if (item == null || item.template == null) return 0;
        int type = item.template.type;
        int id = item.template.id;

        // Ngọc Rồng 1-7 sao → Ngọc Xanh theo sao
        if (id >= NGOC_RONG_1_SAO && id <= NGOC_RONG_7_SAO) {
            int sao = id - NGOC_RONG_1_SAO + 1;
            return switch (sao) {
                case 7 -> 500;
                case 6 -> 300;
                case 5 -> 200;
                case 4 -> 100;
                case 3 -> 50;
                case 2 -> 20;
                default -> 10; // 1 sao
            };
        }

        // Đá nâng cấp → 100 ngọc/viên
        if (type == 14) {
            return 100;
        }

        // Trang bị lỗi → 50 ngọc/món
        if (type >= 0 && type <= 4) {
            return 50;
        }

        // Thức ăn → 0 ngọc
        return 0;
    }
}
