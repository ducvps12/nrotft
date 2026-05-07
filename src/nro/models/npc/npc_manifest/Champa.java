package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import consts.ConstMenu;
import item.Item;
import models.Combine.CombineService;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC Champa — Thu Gom Rác / Đổi Vật Phẩm
 * Đứng tại Siêu thị Huyền Bí (map 173)
 * Chức năng:
 * 1. Bán đồ rác (mở panel UI combine):
 *    - Đồ rác thường (ngọc rồng, đá, thức ăn, TB lỗi) → Vàng + Ngọc Xanh
 *    - Cải trang lỗi (không chỉ số / chỉ số thấp) → Thỏi Vàng (khóa)
 * 2. Bán nguyên liệu event thừa → Ngọc Xanh + Hồng Ngọc
 * 3. Đổi 100 Đá Bảo Vệ → 2 Đá Ngũ Sắc (80% thành công)
 * 4. Gom 7 mảnh Rồng thần Namếc → random Ngọc Rồng Namếc (1-7 sao)
 */
public class Champa extends Npc {

    // ===================== MENU INDEX =====================
    private static final int MENU_MAIN = ConstNpc.MENU_CHAMPA_MAIN;
    private static final int MENU_CONFIRM_BAN_NL = ConstNpc.MENU_CHAMPA_CONFIRM_ALL;
    private static final int MENU_DOI_DBV_CONFIRM = ConstNpc.MENU_CHAMPA_DOI_DBV_CONFIRM;
    private static final int MENU_GOM_MANH = ConstNpc.MENU_CHAMPA_GOM_MANH;
    private static final int MENU_GOM_CONFIRM = ConstNpc.MENU_CHAMPA_GOM_CONFIRM;
    private static final int MENU_SHOP_HIEN_TE = ConstNpc.MENU_CHAMPA_SHOP_HIEN_TE;
    private static final int MENU_SHOP_HT_CONFIRM = ConstNpc.MENU_CHAMPA_SHOP_HT_CONFIRM;

    // ===================== SHOP HIẾN TẾ CONFIG =====================
    private static final int COST_DA_BAO_VE_HT = 10;
    private static final int COST_DA_NGU_SAC_HT = 10;
    private static final int COST_THOI_VANG_HT = 100;
    private static final int COST_GEM_HT = 100_000;      // 100k ngọc xanh
    private static final int COST_RUBY_HT = 30_000;      // 30k hồng ngọc
    private static final int COST_VND_HT = 100_000;       // 100k VNĐ
    private static final int COST_COUPON_HT = 1000;       // 1000 xu ngọc rồng
    private static final int BASE_RATE_HT = 50;           // 50% base
    private static final int RATE_PER_HOP_SKH = 5;        // +5% mỗi hộp
    private static final int MAX_RATE_HT = 95;            // Max 95%
    private static final int ITEM_HOP_SKH = 1703;

    // ===================== ITEM IDS =====================
    // Nguyên liệu event thừa
    private static final int[] ITEMS_EVENT_THUA = {
        1623, // Lá trà tươi
        1624, // Que tre
        1625, // Nia tre
        1626, // Mảnh giấy
        1629, // Bao bì thiệp
        1630, // Keo dán
    };

    // Đá bảo vệ
    private static final int DA_BAO_VE = 987;
    // Đá Ngũ Sắc
    private static final int DA_NGU_SAC = 674;
    // Thỏi vàng
    private static final int THOI_VANG = 457;
    // Mảnh Rồng thần Namếc
    private static final int MANH_RONG_NAMEC = 1204;

    public Champa(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            int soNL = countMaterialItems(player);
            int soDBV = countItemById(player, DA_BAO_VE);
            int soManh = countItemById(player, MANH_RONG_NAMEC);

            String chatText = "Xin chào! Ta là Champa!\n"
                + "Ta thu mua đồ rác, đổi vật phẩm.\n"
                + "NL Event: " + soNL + " | ĐBV: " + soDBV + "\n"
                + "Mảnh Rồng Namếc: " + soManh + "/7";

            this.createOtherMenu(player, MENU_MAIN, chatText,
                "Bán đồ rác\n(Panel UI)",
                "Bán nguyên liệu",
                "Đổi Đá BV\n100→2 Ngũ Sắc",
                "Gom mảnh\nRồng Namếc",
                "Hiến tế\nTrang bị",
                "Shop Hiến Tế\n(Cải Trang VIP)");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case 2300 -> handleMainMenu(player, select);  // MENU_MAIN
                case 2305 -> handleConfirmBanNL(player, select);
                case 2306 -> handleConfirmDoiDBV(player, select);
                case 2307 -> handleGomManh(player, select);
                case 2308 -> handleGomConfirm(player, select);
                case 2309 -> handleShopHienTePreview(player, select);  // MENU_SHOP_HIEN_TE
                case 2310 -> handleShopHienTeConfirm(player, select);  // MENU_SHOP_HT_CONFIRM
                // Xử lý khi user nhấn nút trong popup "Hiến tế" hoặc "Bán đồ rác"
                case ConstNpc.MENU_START_COMBINE -> {
                    if (select == 0) {
                        CombineService.gI().startCombine(player);
                    }
                }
                default -> {}
            }
        }
    }

    private void handleMainMenu(Player player, int select) {
        switch (select) {
            case 0 -> {
                // Tự động tìm và điền đồ rác (đồ lỗi) vào panel
                player.combine.itemsCombine.clear();
                for (Item item : player.inventory.itemsBag) {
                    if (item != null && item.isNotNullItem() && models.Combine.manifest.ChampaBanDoRac.isJunkItem(item)) {
                        player.combine.itemsCombine.add(item);
                        // Giới hạn UI combine là 8 món
                        if (player.combine.itemsCombine.size() >= 8) {
                            break;
                        }
                    }
                }
                
                // Mở panel UI combine bán đồ rác
                CombineService.gI().openTabCombine(player, CombineService.CHAMPA_BAN_DO_RAC);
                
                // Nếu có đồ rác thì gửi lệnh cập nhật giao diện luôn để hiện lên slot
                if (!player.combine.itemsCombine.isEmpty()) {
                    CombineService.gI().reOpenItemCombine(player);
                    models.Combine.manifest.ChampaBanDoRac.showInfoCombine(player);
                } else {
                    Service.gI().sendThongBao(player, "Hành trang của bạn không có vật phẩm lỗi (rác) nào!");
                }
            }
            case 1 -> showPreviewBanNL(player);
            case 2 -> showDoiDBVMenu(player);
            case 3 -> showGomManhMenu(player);
            case 4 -> {
                // Mở panel UI combine hiến tế
                CombineService.gI().openTabCombine(player, CombineService.CHAMPA_HIEN_TE);
            }
            case 5 -> showShopHienTe(player);
        }
    }

    // =====================================================
    //  BÁN NGUYÊN LIỆU — XEM GIÁ TRƯỚC → XÁC NHẬN MỚI BÁN
    // =====================================================
    private void showPreviewBanNL(Player player) {
        List<String> danhSach = new ArrayList<>();
        int tongGem = 0;
        int tongRuby = 0;
        int tongMon = 0;

        for (Item item : player.inventory.itemsBag) {
            if (item != null && isMaterialItem(item)) {
                int qty = item.quantity;
                int gem = qty / 5;
                int ruby = qty / 10;
                tongGem += gem;
                tongRuby += ruby;
                tongMon += qty;
                danhSach.add(item.template.name + " x" + qty);
            }
        }

        if (tongMon == 0) {
            Service.gI().sendThongBao(player, "Không có nguyên liệu event nào!");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== NGUYÊN LIỆU ===\n");
        int maxShow = Math.min(danhSach.size(), 10);
        for (int i = 0; i < maxShow; i++) {
            sb.append(danhSach.get(i)).append("\n");
        }
        if (danhSach.size() > maxShow) {
            sb.append("... +").append(danhSach.size() - maxShow).append(" loại khác\n");
        }
        sb.append("\nTổng: ").append(tongMon).append(" NL\n");
        sb.append("Nhận: +").append(tongGem).append(" Ngọc, +").append(tongRuby).append(" Ruby");

        this.createOtherMenu(player, MENU_CONFIRM_BAN_NL, sb.toString(),
            "Bán hết", "Không bán");
    }

    private void handleConfirmBanNL(Player player, int select) {
        if (select != 0) return;

        int totalGem = 0;
        int totalRuby = 0;
        int totalSold = 0;

        for (int i = player.inventory.itemsBag.size() - 1; i >= 0; i--) {
            Item item = player.inventory.itemsBag.get(i);
            if (item != null && isMaterialItem(item)) {
                int qty = item.quantity;
                totalGem += qty / 5;
                totalRuby += qty / 10;
                totalSold += qty;
                InventoryService.gI().subQuantityItemsBag(player, item, qty);
            }
        }

        if (totalSold == 0) {
            Service.gI().sendThongBao(player, "Không còn nguyên liệu!");
            return;
        }

        if (totalGem > 0) player.inventory.gem += totalGem;
        if (totalRuby > 0) player.inventory.ruby += totalRuby;

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player,
            "Đã bán " + totalSold + " NL\nNhận +" + totalGem + " Ngọc, +" + totalRuby + " Ruby");
    }

    // ===================== ĐỔI 100 ĐÁ BẢO VỆ → 2 ĐÁ NGŨ SẮC (80%) =====================
    private void showDoiDBVMenu(Player player) {
        int soDBV = countItemById(player, DA_BAO_VE);
        int soLanDoi = soDBV / 100;

        if (soLanDoi <= 0) {
            Service.gI().sendThongBao(player,
                "Cần 100 Đá Bảo Vệ!\nCon chỉ có " + soDBV + " viên.");
            return;
        }

        this.createOtherMenu(player, MENU_DOI_DBV_CONFIRM,
            "Con có " + soDBV + " Đá Bảo Vệ\n"
            + "Đổi được " + soLanDoi + " lần\n"
            + "(100 ĐBV → 2 ĐNS, 80% thành công)\n"
            + "Thất bại mất ĐBV nhưng không nhận ĐNS",
            "Đổi 1 lần",
            "Đổi tất cả\n(" + soLanDoi + " lần)",
            "Không đổi");
    }

    private void handleConfirmDoiDBV(Player player, int select) {
        int soDBV = countItemById(player, DA_BAO_VE);
        int soLanDoi;

        switch (select) {
            case 0 -> soLanDoi = 1;
            case 1 -> soLanDoi = soDBV / 100;
            default -> { return; }
        }

        if (soLanDoi <= 0 || soDBV < soLanDoi * 100) {
            Service.gI().sendThongBao(player, "Không đủ Đá Bảo Vệ!");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đầy!");
            return;
        }

        // Trừ ĐBV
        Item dbvItem = InventoryService.gI().findItemBag(player, DA_BAO_VE);
        if (dbvItem != null) {
            InventoryService.gI().subQuantityItemsBag(player, dbvItem, soLanDoi * 100);
        }

        // 80% thành công mỗi lần → nhận 2 ĐNS
        int thanhCong = 0;
        int thatBai = 0;
        for (int i = 0; i < soLanDoi; i++) {
            if (Util.isTrue(80, 100)) {
                thanhCong++;
            } else {
                thatBai++;
            }
        }

        int tongDNS = thanhCong * 2;
        if (tongDNS > 0) {
            Item dns = ItemService.gI().createNewItem((short) DA_NGU_SAC, tongDNS);
            dns.itemOptions.add(new Item.ItemOption(30, 1)); // Khóa giao dịch
            InventoryService.gI().addItemBag(player, dns);
        }

        InventoryService.gI().sendItemBag(player);

        StringBuilder result = new StringBuilder();
        result.append("Kết quả đổi ĐBV:\n");
        result.append("━━━━━━━━━━━━━━━━\n");
        result.append("Đã dùng: ").append(soLanDoi * 100).append(" ĐBV\n");
        result.append("Thành công: ").append(thanhCong).append("/").append(soLanDoi).append(" lần\n");
        if (thatBai > 0) {
            result.append("Thất bại: ").append(thatBai).append(" lần\n");
        }
        result.append("Nhận: +").append(tongDNS).append(" Đá Ngũ Sắc");
        Service.gI().sendThongBao(player, result.toString());
    }

    // ===================== GOM 7 MẢNH RỒNG THẦN NAMẾC → NGỌC RỒNG NAMẾC =====================
    private void showGomManhMenu(Player player) {
        int soManh = countItemById(player, MANH_RONG_NAMEC);

        if (soManh < 7) {
            Service.gI().sendThongBao(player,
                "Cần 7 Mảnh Rồng thần Namếc!\n"
                + "Hiện có: " + soManh + "/7 mảnh\n\n"
                + "Mảnh Rồng rơi từ boss trên hành tinh Namếc.\n"
                + "Hãy cùng bang hội đi săn!");
            return;
        }

        int soLanGom = soManh / 7;

        this.createOtherMenu(player, MENU_GOM_MANH,
            "Gom mảnh Rồng thần Namếc\n"
            + "━━━━━━━━━━━━━━━━\n"
            + "Hiện có: " + soManh + " mảnh\n"
            + "Ghép được: " + soLanGom + " lần\n"
            + "(7 mảnh → 1 Ngọc Rồng Namếc random 1-7 sao)\n\n"
            + "Khi đủ 7 Ngọc Rồng Namếc (1-7 sao)\n"
            + "→ Về Làng Mori (map 7) gọi Rồng thần!",
            "Ghép 1 lần",
            "Ghép tất cả\n(" + soLanGom + " lần)",
            "Không ghép");
    }

    private void handleGomManh(Player player, int select) {
        int soManh = countItemById(player, MANH_RONG_NAMEC);
        int soLanGom;

        switch (select) {
            case 0 -> soLanGom = 1;
            case 1 -> soLanGom = soManh / 7;
            default -> { return; }
        }

        if (soLanGom <= 0 || soManh < soLanGom * 7) {
            Service.gI().sendThongBao(player, "Không đủ Mảnh Rồng thần Namếc!");
            return;
        }

        // Lưu số lần gom vào iDMark
        player.iDMark.setOtt(soLanGom);

        this.createOtherMenu(player, MENU_GOM_CONFIRM,
            "⚠️ XÁC NHẬN GHÉP MẢNH\n"
            + "━━━━━━━━━━━━━━━━\n"
            + "Dùng: " + (soLanGom * 7) + " Mảnh Rồng Namếc\n"
            + "Nhận: " + soLanGom + " Ngọc Rồng Namếc (random 1-7 sao)\n\n"
            + "⚠️ Mảnh sẽ bị MẤT sau khi ghép!",
            "Đồng ý", "Hủy");
    }

    private void handleGomConfirm(Player player, int select) {
        if (select != 0) return;

        int soLanGom = player.iDMark.getOtt();
        int soManh = countItemById(player, MANH_RONG_NAMEC);

        if (soLanGom <= 0 || soManh < soLanGom * 7) {
            Service.gI().sendThongBao(player, "Không đủ Mảnh Rồng thần Namếc!");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < soLanGom) {
            Service.gI().sendThongBao(player,
                "Cần ít nhất " + soLanGom + " ô trống hành trang!");
            return;
        }

        // Trừ mảnh
        Item manhItem = InventoryService.gI().findItemBag(player, MANH_RONG_NAMEC);
        if (manhItem != null) {
            InventoryService.gI().subQuantityItemsBag(player, manhItem, soLanGom * 7);
        }

        // Random ngọc rồng Namếc cho mỗi lần ghép
        StringBuilder ketQua = new StringBuilder();
        ketQua.append("✨ GHÉP MẢNH THÀNH CÔNG!\n");
        ketQua.append("━━━━━━━━━━━━━━━━\n");

        for (int i = 0; i < soLanGom; i++) {
            // Ngọc Rồng Namếc 1-7 sao (ID 353-359)
            int ngocSao = 353 + Util.nextInt(0, 6);
            int sao = ngocSao - 352;
            Item ngocRong = ItemService.gI().createNewItem((short) ngocSao);
            ngocRong.quantity = 1;
            InventoryService.gI().addItemBag(player, ngocRong);
            ketQua.append("• Ngọc Rồng Namếc ").append(sao).append(" sao\n");
        }

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, ketQua.toString());

        // Thông báo toàn server
        if (soLanGom > 0) {
            Service.gI().sendThongBaoAllPlayer(
                player.name + " vừa ghép " + soLanGom + " Ngọc Rồng Namếc từ mảnh tại Champa!");
        }
    }

    // ===================== SHOP HIẾN TẾ =====================
    private void showShopHienTe(Player player) {
        int soDBV = countItemById(player, DA_BAO_VE);
        int soDNS = countItemById(player, DA_NGU_SAC);
        int soTV = countItemById(player, THOI_VANG);
        int soHopSKH = countItemById(player, ITEM_HOP_SKH);
        int rate = Math.min(BASE_RATE_HT + soHopSKH * RATE_PER_HOP_SKH, MAX_RATE_HT);

        // Hàm check đủ/thiếu → màu xanh(2)/đỏ(6)
        String c1 = soDBV >= COST_DA_BAO_VE_HT ? "2" : "6";
        String c2 = soDNS >= COST_DA_NGU_SAC_HT ? "2" : "6";
        String c3 = soTV >= COST_THOI_VANG_HT ? "2" : "6";
        String c4 = player.inventory.coupon >= COST_COUPON_HT ? "2" : "6";
        String c5 = player.inventory.gem >= COST_GEM_HT ? "2" : "6";
        String c6 = player.inventory.ruby >= COST_RUBY_HT ? "2" : "6";
        String c7 = player.getSession().vnd >= COST_VND_HT ? "2" : "6";

        String info = "|1|★ HIẾN TẾ SIÊU CẤP ★\n"
            + "|2|Thưởng: CT VIP HP+120% KI+120%\n\n"
            + "|7|Chi phí:\n"
            + "|" + c1 + "|" + COST_DA_BAO_VE_HT + " ĐBV(" + soDBV + ") "
            + "|" + c2 + "|" + COST_DA_NGU_SAC_HT + " ĐNS(" + soDNS + ")\n"
            + "|" + c3 + "|" + COST_THOI_VANG_HT + " TV(" + soTV + ") "
            + "|" + c4 + "|1k Xu(" + player.inventory.coupon + ")\n"
            + "|" + c5 + "|100k Ngọc(" + Util.numberToMoney(player.inventory.gem) + ")\n"
            + "|" + c6 + "|30k Ruby(" + player.inventory.ruby + ") "
            + "|" + c7 + "|100k VNĐ(" + Util.numberToMoney(player.getSession().vnd) + ")\n\n"
            + "|1|Tỉ lệ: |2|" + rate + "% "
            + "|8|Hụt: mất NL!\n";
        if (soHopSKH > 0) {
            info += "|2|SKH Thần: " + soHopSKH + "(+" + (soHopSKH * RATE_PER_HOP_SKH) + "%)\n";
        } else {
            info += "|6|+Hộp SKH Thần = +5%/hộp\n";
        }

        this.createOtherMenu(player, MENU_SHOP_HIEN_TE, info,
            "Hiến tế!\n(" + rate + "%)", "Không");
    }

    private void handleShopHienTePreview(Player player, int select) {
        if (select != 0) return;

        // Kiểm tra tất cả nguyên liệu
        int soDBV = countItemById(player, DA_BAO_VE);
        int soDNS = countItemById(player, DA_NGU_SAC);
        int soTV = countItemById(player, THOI_VANG);
        int soHopSKH = countItemById(player, ITEM_HOP_SKH);

        if (soDBV < COST_DA_BAO_VE_HT) { Service.gI().sendThongBao(player, "Thiếu Đá Bảo Vệ! Cần " + COST_DA_BAO_VE_HT + ", có " + soDBV); return; }
        if (soDNS < COST_DA_NGU_SAC_HT) { Service.gI().sendThongBao(player, "Thiếu Đá Ngũ Sắc! Cần " + COST_DA_NGU_SAC_HT + ", có " + soDNS); return; }
        if (soTV < COST_THOI_VANG_HT) { Service.gI().sendThongBao(player, "Thiếu Thỏi Vàng! Cần " + COST_THOI_VANG_HT + ", có " + soTV); return; }
        if (player.inventory.coupon < COST_COUPON_HT) { Service.gI().sendThongBao(player, "Thiếu Xu NRO! Cần " + COST_COUPON_HT); return; }
        if (player.inventory.gem < COST_GEM_HT) { Service.gI().sendThongBao(player, "Thiếu Ngọc Xanh! Cần " + Util.numberToMoney(COST_GEM_HT)); return; }
        if (player.inventory.ruby < COST_RUBY_HT) { Service.gI().sendThongBao(player, "Thiếu Hồng Ngọc! Cần " + Util.numberToMoney(COST_RUBY_HT)); return; }
        if (player.getSession().vnd < COST_VND_HT) { Service.gI().sendThongBao(player, "Thiếu VNĐ! Cần " + Util.numberToMoney(COST_VND_HT)); return; }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) { Service.gI().sendThongBao(player, "Hành trang đầy!"); return; }

        int rate = Math.min(BASE_RATE_HT + soHopSKH * RATE_PER_HOP_SKH, MAX_RATE_HT);

        this.createOtherMenu(player, MENU_SHOP_HT_CONFIRM,
            "|6|⚠ XÁC NHẬN HIẾN TẾ SIÊU CẤP\n"
            + "|7|══════════════════\n"
            + "|1|Sẽ trừ TẤT CẢ nguyên liệu:\n"
            + "|2|" + COST_DA_BAO_VE_HT + " ĐBV + " + COST_DA_NGU_SAC_HT + " ĐNS + " + COST_THOI_VANG_HT + " TV\n"
            + "|2|" + Util.numberToMoney(COST_COUPON_HT) + " Xu + " + Util.numberToMoney(COST_GEM_HT) + " Ngọc\n"
            + "|2|" + Util.numberToMoney(COST_RUBY_HT) + " Ruby + " + Util.numberToMoney(COST_VND_HT) + " VNĐ\n\n"
            + "|1|Tỉ lệ: |2|" + rate + "% thành công\n"
            + "|6|Thất bại: MẤT TOÀN BỘ nguyên liệu!\n"
            + "|7|══════════════════",
            "HIẾN TẾ!", "Hủy");
    }

    private void handleShopHienTeConfirm(Player player, int select) {
        if (select != 0) return;

        // Re-check tất cả
        int soDBV = countItemById(player, DA_BAO_VE);
        int soDNS = countItemById(player, DA_NGU_SAC);
        int soTV = countItemById(player, THOI_VANG);
        int soHopSKH = countItemById(player, ITEM_HOP_SKH);

        if (soDBV < COST_DA_BAO_VE_HT || soDNS < COST_DA_NGU_SAC_HT || soTV < COST_THOI_VANG_HT
            || player.inventory.coupon < COST_COUPON_HT || player.inventory.gem < COST_GEM_HT
            || player.inventory.ruby < COST_RUBY_HT || player.getSession().vnd < COST_VND_HT) {
            Service.gI().sendThongBao(player, "Không đủ nguyên liệu!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đầy!");
            return;
        }

        // === TRỪ NGUYÊN LIỆU ===
        Item dbvItem = InventoryService.gI().findItemBag(player, DA_BAO_VE);
        if (dbvItem != null) InventoryService.gI().subQuantityItemsBag(player, dbvItem, COST_DA_BAO_VE_HT);
        Item dnsItem = InventoryService.gI().findItemBag(player, DA_NGU_SAC);
        if (dnsItem != null) InventoryService.gI().subQuantityItemsBag(player, dnsItem, COST_DA_NGU_SAC_HT);
        Item tvItem = InventoryService.gI().findItemBag(player, THOI_VANG);
        if (tvItem != null) InventoryService.gI().subQuantityItemsBag(player, tvItem, COST_THOI_VANG_HT);
        player.inventory.coupon -= COST_COUPON_HT;
        player.inventory.gem -= COST_GEM_HT;
        player.inventory.ruby -= COST_RUBY_HT;
        player.getSession().vnd -= COST_VND_HT;

        // === TÍNH TỈ LỆ ===
        int rate = Math.min(BASE_RATE_HT + soHopSKH * RATE_PER_HOP_SKH, MAX_RATE_HT);
        boolean success = Util.isTrue(rate, 100);

        if (success) {
            // === THÀNH CÔNG — Random cải trang VIP ===
            short[] vipCT = { 860, 1735, 884 }; // Mị Nương, Hắc Mị Nương, Hit
            String[] ctNames = { "Mị Nương", "Hắc Mị Nương", "Hit" };
            int idx = Util.nextInt(0, vipCT.length - 1);

            Item reward = ItemService.gI().createNewItem(vipCT[idx]);
            reward.itemOptions.clear();
            reward.itemOptions.add(new Item.ItemOption(77, 120));   // HP +120%
            reward.itemOptions.add(new Item.ItemOption(103, 120));  // KI +120%
            reward.itemOptions.add(new Item.ItemOption(50, 30));    // Sức đánh +30%
            reward.itemOptions.add(new Item.ItemOption(30, 0));     // Khóa GD

            InventoryService.gI().addItemBag(player, reward);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendMoney(player);

            Service.gI().sendThongBao(player,
                "|2|★★★ HIẾN TẾ THÀNH CÔNG! ★★★\n\n"
                + "|1|Nhận được: |2|CT " + ctNames[idx] + "\n"
                + "|2|HP +120% | KI +120%\n"
                + "|2|Sức đánh +30% | Vĩnh viễn\n\n"
                + "|1|Tỉ lệ đã dùng: " + rate + "%");

            // Thông báo toàn server
            nro.server.ServerNotify.gI().notify(
                "🔥 " + player.name + " vừa hiến tế thành công nhận CT " + ctNames[idx]
                + " (HP+120%, KI+120%)! Bùng nổ cả server!");
        } else {
            // === THẤT BẠI — Mất hết nguyên liệu ===
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendMoney(player);

            Service.gI().sendThongBao(player,
                "|6|✘ HIẾN TẾ THẤT BẠI!\n\n"
                + "|7|Nguyên liệu đã bị thiêu rụi...\n"
                + "|8|Toàn bộ nguyên liệu hiến tế đã mất!\n\n"
                + "|1|Tỉ lệ đã dùng: " + rate + "%\n"
                + "|7|Thêm Hộp SKH Thần để tăng tỉ lệ!");
        }
    }

    // ===================== HELPER METHODS =====================
    private boolean isMaterialItem(Item item) {
        if (item == null || item.template == null) return false;
        int id = item.template.id;

        for (int matId : ITEMS_EVENT_THUA) {
            if (id == matId) return true;
        }

        return false;
    }

    private int countItemById(Player player, int templateId) {
        int count = 0;
        for (Item item : player.inventory.itemsBag) {
            if (item != null && item.template != null && item.template.id == templateId) {
                count += item.quantity;
            }
        }
        return count;
    }

    private int countMaterialItems(Player player) {
        int count = 0;
        for (Item item : player.inventory.itemsBag) {
            if (item != null && isMaterialItem(item)) {
                count += item.quantity;
            }
        }
        return count;
    }
}
