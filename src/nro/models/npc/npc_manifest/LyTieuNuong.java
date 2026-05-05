package nro.models.npc.npc_manifest;

/**
 * NPC Lý Tiểu Nương - Mini Games + Gói VIP Tuần + Gói Đệ Tử Ngày
 */
import consts.ConstMiniGame;
import consts.ConstNpc;
import minigame.DecisionMaker.DecisionMaker;
import minigame.DecisionMaker.DecisionMakerGem;
import minigame.DecisionMaker.DecisionMakerGold;
import minigame.DecisionMaker.DecisionMakerRuby;
import minigame.LuckyNumber.LuckyNumber;
import minigame.LuckyNumber.LuckyNumberService;
import minigame.RockPaperScissors.RockPaperScissors;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.TaskService;
import nro.services.VipPackageService;
import services.func.Input;
import utils.Util;

public class LyTieuNuong extends Npc {

    public LyTieuNuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            String flashSale = VipPackageService.isFlashSaleActive()
                    ? "\n|2|⚡ FLASH SALE -" + VipPackageService.getFlashSalePercent() + "% ĐANG DIỄN RA!"
                    : "";

            String info = "|7|━━━ LÝ TIỂU NƯƠNG ━━━\n"
                    + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                    + "|8|Sale 20% gói VIP 2 & VIP 3!"
                    + flashSale + "\n"
                    + "|7|━━━━━━━━━━━━━━━━━━\n"
                    + "|2|Gói VIP Tuần: Nhận items buff mỗi tuần\n"
                    + "|2|Gói Đệ Tử: Nhận đệ VĨNH VIỄN + items\n"
                    + "|1|VIP Đệ Tử: x2~x5 TNSM + phân bổ HP/DAME\n"
                    + "|2|Mini Games: Kéo Búa Bao, Số May Mắn,...\n"
                    + "Chọn dịch vụ bên dưới:";

            createOtherMenu(player, ConstNpc.MENU_LTN_MAIN, info,
                    "Gói VIP\nTuần",
                    "Gói Đệ Tử\n(Vĩnh Viễn)",
                    "VIP Đệ\nTử",
                    "Mini\nGames",
                    "Hướng Dẫn",
                    "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {

                // ================== MENU CHÍNH ==================
                case ConstNpc.MENU_LTN_MAIN -> handleMainMenu(player, select);

                // ================== GÓI VIP TUẦN ==================
                case ConstNpc.MENU_LTN_VIP -> handleVipMenu(player, select);
                case ConstNpc.MENU_LTN_VIP_CONFIRM_1 -> handleVipConfirm(player, select, 1);
                case ConstNpc.MENU_LTN_VIP_CONFIRM_2 -> handleVipConfirm(player, select, 2);
                case ConstNpc.MENU_LTN_VIP_CONFIRM_3 -> handleVipConfirm(player, select, 3);

                // ================== GÓI ĐỆ TỬ NGÀY ==================
                case ConstNpc.MENU_LTN_PET -> handlePetMenu(player, select);
                case ConstNpc.MENU_LTN_PET_CONFIRM_1 -> handlePetConfirm(player, select, 1);
                case ConstNpc.MENU_LTN_PET_CONFIRM_2 -> handlePetConfirm(player, select, 2);
                case ConstNpc.MENU_LTN_PET_CONFIRM_3 -> handlePetConfirm(player, select, 3);
                case ConstNpc.MENU_LTN_PET_CONFIRM_4 -> handlePetConfirm(player, select, 4);
                case ConstNpc.MENU_LTN_PET_GUIDE -> { if (select == 0) showPetMenu(player); }

                // ================== GÓI VIP ĐỆ TỬ ==================
                case ConstNpc.MENU_LTN_VIP_PET -> handleVipPetMenu(player, select);
                case ConstNpc.MENU_LTN_VIP_PET_CONFIRM_1 -> handleVipPetConfirm(player, select, 1);
                case ConstNpc.MENU_LTN_VIP_PET_CONFIRM_2 -> handleVipPetConfirm(player, select, 2);
                case ConstNpc.MENU_LTN_VIP_PET_CONFIRM_3 -> handleVipPetConfirm(player, select, 3);
                case ConstNpc.MENU_LTN_VIP_PET_CONFIRM_4 -> handleVipPetConfirm(player, select, 4);
                case ConstNpc.MENU_LTN_VIP_PET_CONFIG -> handleVipPetConfigMenu(player, select);

                // ================== MINI GAMES (GIỮ NGUYÊN) ==================
                case ConstMiniGame.MENU_CHINH -> handleMiniGameMain(player, select);
                case ConstMiniGame.MENU_KEO_BUA_BAO ->
                    RockPaperScissors.confirmMenu(this, player, select);
                case ConstMiniGame.MENU_PLAY_KEO_BUA_BAO -> {
                    if (player.iDMark.getTimePlayKeoBuaBao() - System.currentTimeMillis() > 0) {
                        RockPaperScissors.confirmPlay(this, player, select);
                    } else {
                        createOtherMenu(player, ConstMiniGame.MENU_KEO_BUA_BAO, "Hãy chọn mức cược.",
                                "500K vàng", "2 Tr vàng", "5 Tr vàng",
                                "10 Tr vàng", "25 Tr vàng", "50 Tr vàng");
                    }
                }
                case ConstMiniGame.MENU_CON_SO_MAY_MAN_VANG -> { /* để trống */ }
                case ConstMiniGame.MENU_CON_SO_MAY_MAN_NGOC -> { /* để trống */ }
                case ConstMiniGame.MENU_CHON_AI_DAY -> {
                    switch (select) {
                        case 0 -> DecisionMaker.gI().showTutorial(this, player);
                        case 1 -> DecisionMakerGold.showMenuSelect(this, player);
                        case 2 -> DecisionMakerRuby.showMenuSelect(this, player);
                        case 3 -> DecisionMakerGem.showMenuSelect(this, player);
                    }
                }
                case ConstMiniGame.MENU_LUCKY_NUMBER -> {
                    if (select == 0) {
                        LuckyNumber.showMenu(this, player, player.iDMark.isGemCSMM());
                    }
                }
                case ConstMiniGame.MENU_PLAY_LUCKY_NUMBER_GOLD, ConstMiniGame.MENU_PLAY_LUCKY_NUMBER_GEM -> {
                    switch (select) {
                        case 0 -> LuckyNumber.showMenu(this, player, player.iDMark.isGemCSMM());
                        case 1 -> Input.gI().createFormSelectOneNumberLuckyNumber(player, player.iDMark.isGemCSMM());
                        case 2 -> LuckyNumberService.addOneNumber(player, true);
                        case 3 -> LuckyNumberService.addOneNumber(player, false);
                        case 4 -> LuckyNumber.showMenuTutorials(this, player);
                    }
                }
                case ConstMiniGame.MENU_PLAY_DECISION_MAKER_GOLD -> {
                    switch (select) {
                        case 0 -> DecisionMakerGold.showMenuSelect(this, player);
                        case 1 -> DecisionMakerGold.selectPlay(this, player, true);
                        case 2 -> DecisionMakerGold.selectPlay(this, player, false);
                    }
                }
                case ConstMiniGame.MENU_PLAY_DECISION_MAKER_GEM -> {
                    switch (select) {
                        case 0 -> DecisionMakerGem.showMenuSelect(this, player);
                        case 1 -> DecisionMakerGem.selectPlay(this, player, true);
                        case 2 -> DecisionMakerGem.selectPlay(this, player, false);
                    }
                }
                case ConstMiniGame.MENU_PLAY_DECISION_MAKER_RUBY -> {
                    switch (select) {
                        case 0 -> DecisionMakerRuby.showMenuSelect(this, player);
                        case 1 -> DecisionMakerRuby.selectPlay(this, player, true);
                        case 2 -> DecisionMakerRuby.selectPlay(this, player, false);
                    }
                }
                case ConstMiniGame.MENU_WAIT_NEW_GAME -> {
                    if (select == 0) {
                        DecisionMaker.gI().showTutorial(this, player);
                    }
                }
            }
        }
    }

    // ===================== XỬ LÝ MENU CHÍNH =====================
    private void handleMainMenu(Player player, int select) {
        switch (select) {
            case 0 -> showVipMenu(player);
            case 1 -> showPetMenu(player);
            case 2 -> showVipPetMenu(player);
            case 3 -> showMiniGameMenu(player);
            case 4 -> showGuideMenu(player);
            // case 5 = Đóng
        }
    }

    // ===================== GÓI VIP TUẦN =====================
    private void showVipMenu(Player player) {
        VipPackageService vps = VipPackageService.gI();
        boolean hasActive = vps.hasActiveVipPackage(player);
        String expireInfo = hasActive ? vps.getVipExpireInfo(player) : null;

        String flashTag = VipPackageService.isFlashSaleActive()
                ? " ⚡-" + VipPackageService.getFlashSalePercent() + "%"
                : "";

        String info = "|7|━━━ GÓI VIP TUẦN ━━━\n"
                + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                + (hasActive
                        ? "|2|✓ Đang có gói VIP (hết: " + expireInfo + ")\n"
                        : "|8|Chưa có gói VIP nào\n")
                + "|8|Sale 20% cho VIP 2 & VIP 3" + flashTag + "\n"
                + "|3|Mua 1 lần nhận items buff dùng 7 ngày\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_LTN_VIP, info,
                "VIP 1\n" + Util.mumberToLouis(vps.getVipPrice(1)),
                "VIP 2\n" + Util.mumberToLouis(vps.getVipPrice(2)) + " ❌" + Util.mumberToLouis(VipPackageService.VIP2_PRICE_ORIGINAL),
                "VIP 3\n" + Util.mumberToLouis(vps.getVipPrice(3)) + " ❌" + Util.mumberToLouis(VipPackageService.VIP3_PRICE_ORIGINAL),
                "Quay Lại");
    }

    private void handleVipMenu(Player player, int select) {
        VipPackageService vps = VipPackageService.gI();
        switch (select) {
            case 0 -> createOtherMenu(player, ConstNpc.MENU_LTN_VIP_CONFIRM_1,
                    vps.getVipDescription(1), "Mua Ngay", "Quay Lại");
            case 1 -> createOtherMenu(player, ConstNpc.MENU_LTN_VIP_CONFIRM_2,
                    vps.getVipDescription(2), "Mua Ngay", "Quay Lại");
            case 2 -> createOtherMenu(player, ConstNpc.MENU_LTN_VIP_CONFIRM_3,
                    vps.getVipDescription(3), "Mua Ngay", "Quay Lại");
            case 3 -> openBaseMenu(player);
        }
    }

    private void handleVipConfirm(Player player, int select, int tier) {
        if (select == 0) {
            VipPackageService.gI().purchaseVipPackage(player, tier);
        } else {
            showVipMenu(player);
        }
    }

    // ===================== GÓI ĐỆ TỬ NGÀY =====================
    private void showPetMenu(Player player) {
        VipPackageService vps = VipPackageService.gI();
        boolean hasActive = vps.hasActivePetPackage(player);

        String info = "|7|━━━ GÓI ĐỆ TỬ (VĨNH VIỄN) ━━━\n"
                + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                + (hasActive
                        ? "|2|✓ Hôm nay đã mua gói Đệ Tử\n"
                        : "|8|Chưa mua gói Đệ Tử hôm nay\n")
                + "|3|Đệ tử nhận được là VĨNH VIỄN!\n"
                + "|8|Giới hạn: mua 1 gói/ngày\n"
                + "|8|Đệ tử + Items buff kèm theo\n"
                + "|1|★ Tuyệt Thế Đệ Tử: CHỈ CÀY FREE!\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_LTN_PET, info,
                "Đệ Mabu\n" + Util.mumberToLouis(vps.getPetPrice(1)),
                "Đệ Cell\n" + Util.mumberToLouis(vps.getPetPrice(2)),
                "Đệ Berus\n" + Util.mumberToLouis(vps.getPetPrice(3)),
                "Đệ B.Goku\n" + Util.mumberToLouis(vps.getPetPrice(4)),
                "Hướng Dẫn\nĐệ Tử",
                "Quay Lại");
    }

    private void handlePetMenu(Player player, int select) {
        VipPackageService vps = VipPackageService.gI();
        switch (select) {
            case 0 -> createOtherMenu(player, ConstNpc.MENU_LTN_PET_CONFIRM_1,
                    vps.getPetDescription(1), "Mua Ngay", "Quay Lại");
            case 1 -> createOtherMenu(player, ConstNpc.MENU_LTN_PET_CONFIRM_2,
                    vps.getPetDescription(2), "Mua Ngay", "Quay Lại");
            case 2 -> createOtherMenu(player, ConstNpc.MENU_LTN_PET_CONFIRM_3,
                    vps.getPetDescription(3), "Mua Ngay", "Quay Lại");
            case 3 -> createOtherMenu(player, ConstNpc.MENU_LTN_PET_CONFIRM_4,
                    vps.getPetDescription(4), "Mua Ngay", "Quay Lại");
            case 4 -> showPetGuide(player);
            case 5 -> openBaseMenu(player);
        }
    }

    private void handlePetConfirm(Player player, int select, int tier) {
        if (select == 0) {
            VipPackageService.gI().purchasePetPackage(player, tier);
        } else {
            showPetMenu(player);
        }
    }

    // ===================== GÓI VIP ĐỆ TỬ =====================
    private void showVipPetMenu(Player player) {
        VipPackageService vps = VipPackageService.gI();
        boolean hasActive = vps.hasActiveVipPetPackage(player);
        int currentTier = vps.getActiveVipPetTier(player);
        String expireInfo = hasActive ? vps.getVipPetExpireInfo(player) : null;
        boolean hasCouponVIP = vps.hasVIPDiscountCoupon(player);
        boolean hasCoupon = hasCouponVIP || vps.hasDiscountCoupon(player);

        String info = "|7|━━━ VIP ĐỆ TỬ ━━━\n"
                + "|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                + (hasActive
                        ? "|2|✓ Đang có VIP Đệ tier " + currentTier + " (hết: " + expireInfo + ")\n"
                        : "|8|Chưa có gói VIP Đệ nào\n")
                + (hasCoupon ? "|1|🎫 Có Phiếu Giảm Giá" + (hasCouponVIP ? " VIP 70%" : " 30%") + "!\n" : "")
                + "|3|Tăng tốc đệ tử, ưu tiên HP + DAME!\n"
                + "|8|Hiệu lực: 24 giờ, cho phép nâng tier\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        if (currentTier == 4) {
            createOtherMenu(player, ConstNpc.MENU_LTN_VIP_PET, info,
                    "VIP Bạc\nx2 TNSM\n" + Util.mumberToLouis(vps.getVipPetPriceWithCoupon(player, 1)),
                    "VIP Vàng\nx3 TNSM\n" + Util.mumberToLouis(vps.getVipPetPriceWithCoupon(player, 2)),
                    "VIP K.Cương\nx5 TNSM\n" + Util.mumberToLouis(vps.getVipPetPriceWithCoupon(player, 3)),
                    "VIP CAO THỦ\nx7 TNSM\n" + Util.mumberToLouis(vps.getVipPetPriceWithCoupon(player, 4)),
                    "Cấu Hình\nCAO THỦ",
                    "Quay Lại");
        } else {
            createOtherMenu(player, ConstNpc.MENU_LTN_VIP_PET, info,
                    "VIP Bạc\nx2 TNSM\n" + Util.mumberToLouis(vps.getVipPetPriceWithCoupon(player, 1)),
                    "VIP Vàng\nx3 TNSM\n" + Util.mumberToLouis(vps.getVipPetPriceWithCoupon(player, 2)),
                    "VIP K.Cương\nx5 TNSM\n" + Util.mumberToLouis(vps.getVipPetPriceWithCoupon(player, 3)),
                    "VIP CAO THỦ\nx7 TNSM\n" + Util.mumberToLouis(vps.getVipPetPriceWithCoupon(player, 4)),
                    "Quay Lại");
        }
    }

    private void handleVipPetMenu(Player player, int select) {
        VipPackageService vps = VipPackageService.gI();
        switch (select) {
            case 0 -> createOtherMenu(player, ConstNpc.MENU_LTN_VIP_PET_CONFIRM_1,
                    vps.getVipPetDescription(1), "Mua Ngay", "Quay Lại");
            case 1 -> createOtherMenu(player, ConstNpc.MENU_LTN_VIP_PET_CONFIRM_2,
                    vps.getVipPetDescription(2), "Mua Ngay", "Quay Lại");
            case 2 -> createOtherMenu(player, ConstNpc.MENU_LTN_VIP_PET_CONFIRM_3,
                    vps.getVipPetDescription(3), "Mua Ngay", "Quay Lại");
            case 3 -> createOtherMenu(player, ConstNpc.MENU_LTN_VIP_PET_CONFIRM_4,
                    vps.getVipPetDescription(4), "Mua Ngay", "Quay Lại");
            case 4 -> {
                if (vps.getActiveVipPetTier(player) == 4) {
                    showVipPetConfigMenu(player);
                } else {
                    openBaseMenu(player);
                }
            }
            case 5 -> openBaseMenu(player);
        }
    }

    private void handleVipPetConfirm(Player player, int select, int tier) {
        if (select == 0) {
            VipPackageService.gI().purchaseVipPetWithCoupon(player, tier);
        } else {
            showVipPetMenu(player);
        }
    }

    private void showVipPetConfigMenu(Player player) {
        String info = "|7|━━━ CẤU HÌNH VIP CAO THỦ ━━━\n"
                + "|1|Bạn có thể bật/tắt cộng Giáp và Chí mạng cho Đệ tử!\n"
                + "|2|Cộng Giáp: " + (player.petCaoThuAllowDef ? "|2|ĐANG BẬT" : "|7|ĐANG TẮT") + "\n"
                + "|2|Cộng Chí Mạng: " + (player.petCaoThuAllowCrit ? "|2|ĐANG BẬT" : "|7|ĐANG TẮT") + "\n"
                + "|7|━━━━━━━━━━━━━━━━━━";

        createOtherMenu(player, ConstNpc.MENU_LTN_VIP_PET_CONFIG, info,
                (player.petCaoThuAllowDef ? "Tắt" : "Bật") + " Giáp",
                (player.petCaoThuAllowCrit ? "Tắt" : "Bật") + " C.Mạng",
                "Quay Lại");
    }

    private void handleVipPetConfigMenu(Player player, int select) {
        switch (select) {
            case 0 -> {
                player.petCaoThuAllowDef = !player.petCaoThuAllowDef;
                showVipPetConfigMenu(player);
            }
            case 1 -> {
                player.petCaoThuAllowCrit = !player.petCaoThuAllowCrit;
                showVipPetConfigMenu(player);
            }
            case 2 -> showVipPetMenu(player);
        }
    }

    // ===================== MINI GAMES (GIỮ NGUYÊN) =====================
    private void showMiniGameMenu(Player player) {
        createOtherMenu(player, ConstMiniGame.MENU_CHINH, "Bạn muốn tham gia mini game nào?",
                "Kéo\nBúa\nBao", "Con số\nmay mắn\nvàng",
                "Con số\nmay mắn\nngọc xanh", "Chọn ai đây", "Đóng");
    }

    private void handleMiniGameMain(Player player, int select) {
        switch (select) {
            case 0 ->
                createOtherMenu(player, ConstMiniGame.MENU_KEO_BUA_BAO, "Hãy chọn mức cược.",
                        "500K vàng", "2 Tr vàng", "5 Tr vàng",
                        "10 Tr vàng", "25 Tr vàng", "50 Tr vàng");
            case 1 -> {
                LuckyNumber.showMenu(this, player, false);
                player.iDMark.setGemCSMM(false);
            }
            case 2 -> {
                LuckyNumber.showMenu(this, player, true);
                player.iDMark.setGemCSMM(true);
            }
            case 3 ->
                DecisionMaker.gI().showMenu(this, player);
        }
    }
    // ===================== HƯỚNG DẪN =====================
    private void showGuideMenu(Player player) {
        String guide = "|7|══════════════════\n"
                + "|1|    📋 HƯỚNG DẪN DỊCH VỤ\n"
                + "|7|══════════════════\n"
                + "\n"
                + "|2|❶ GÓI VIP TUẦN\n"
                + "|8|  ○ Mua 1 lần, buff 7 ngày\n"
                + "|8|  ○ Items: Thỏi vàng, TNSM, Đá BV\n"
                + "\n"
                + "|2|❷ GÓI ĐỆ TỬ (VĨNH VIỄN)\n"
                + "|8|  ○ Mua 1 gói/ngày\n"
                + "|8|  ○ Mabu 50K → B.Goku 500K\n"
                + "|8|  ○ Đệ + items buff kèm theo\n"
                + "\n"
                + "|1|❸ TUYỆT THẾ ĐỆ TỬ\n"
                + "|1|  ○ CHỈ CÀY FREE, không bán!\n"
                + "|8|  ○ Xem chi tiết: Menu Đệ Tử\n"
                + "|8|    → Hướng Dẫn Đệ Tử\n"
                + "\n"
                + "|2|❹ VIP ĐỆ TỬ (24h)\n"
                + "|8|  ○ x2 ~ x7 TNSM cho đệ\n"
                + "|8|  ○ Ưu tiên HP + DAME\n"
                + "|2|  ○ 🎫 PGG: -30% | PGG VIP: -70%\n"
                + "\n"
                + "|2|❺ MINI GAMES\n"
                + "|8|  ○ Kéo Búa Bao\n"
                + "|8|  ○ Số May Mắn (Vàng/Ngọc)\n"
                + "|8|  ○ Chọn Ai Đây\n"
                + "|7|══════════════════";

        npcChat(player, guide);
    }

    // ===================== HƯỚNG DẪN ĐỆ TỬ =====================
    private void showPetGuide(Player player) {
        // --- Thông tin đệ hiện tại ---
        String petInfo;
        if (player.pet != null) {
            String typeName = switch (player.pet.typePet) {
                case 1 -> "Mabu";
                case 2 -> "B.Goku";
                case 3 -> "Cell";
                case 4 -> "Berus";
                case 5 -> "Tuyệt Thế";
                default -> "???";
            };
            petInfo = "|2|🐉 Đệ: $" + player.pet.name
                    + " (" + typeName + ")\n"
                    + "|2|⚔ SM: " + Util.mumberToLouis(player.pet.nPoint.power) + "\n";
        } else {
            petInfo = "|8|❌ Chưa có đệ tử\n";
        }

        String guide = "|7|══════════════════\n"
                + "|1|    📖 HỆ THỐNG ĐỆ TỬ\n"
                + "|7|══════════════════\n"
                + petInfo
                + "\n"
                + "|7|┌─── BẢNG SO SÁNH ĐỆ TỬ ───┐\n"
                + "|8|│ Mabu    │ 7 ô │ Bonus thấp\n"
                + "|8|│ Cell    │ 9 ô │ Đồ 3 hệ\n"
                + "|8|│ Berus   │ 9 ô │ Đồ 3 hệ\n"
                + "|1|│ B.Goku  │ 9 ô │ Fusion #1\n"
                + "|1|│ T.Thế   │10 ô │ 5 skill sẵn\n"
                + "|7|└────────────────────┘\n"
                + "\n"
                + "|7|┌── FUSION (Porata cấp 4) ──┐\n"
                + "|8|│ Mabu  │ HP 20  KI 35  SD 25\n"
                + "|8|│ Cell  │ HP 25  KI 30  SD 30\n"
                + "|8|│ Berus │ HP 30  KI 35  SD 35\n"
                + "|1|│ B.Goku│ HP 35  KI 40  SD 40\n"
                + "|1|│ T.Thế │ +20% + cộng thẳng!\n"
                + "|7|└────────────────────┘\n"
                + "\n"
                + "|7|══════════════════\n"
                + "|1|    🏆 LỘ TRÌNH TUYỆT THẾ\n"
                + "|7|══════════════════\n"
                + "|1|⚠ KHÔNG MUA BẰNG TIỀN!\n"
                + "|1|⚠ CHỈ CÀY FREE!\n"
                + "\n"
                + "|2|❶ Kiếm Bình Hút Năng Lượng\n"
                + "|8|  ○ Boss Rồng Nhí: drop 1-3\n"
                + "|8|  ○ Boss Hirudegarn: drop 1\n"
                + "|8|  ○ NV Quỷ Lão Kame: 10-20\n"
                + "\n"
                + "|2|❷ Farm Kilis (Map Cadic)\n"
                + "|8|  ○ Tỉ lệ: 1/333 mỗi quái\n"
                + "|8|  ○ Buff Osin: 10/333 (x10)\n"
                + "|8|  ○ Giá buff: 100 HN = 10 phút\n"
                + "|8|  ○ Mỗi lần: +1 Kilis vào Bình\n"
                + "\n"
                + "|2|❸ Đệ 3K Kilis\n"
                + "|8|  ○ 3,000 Kilis + Mabu 40 tỷ SM\n"
                + "|8|  ○ Dùng Bình → Nhận 1 trong:\n"
                + "|8|    B.Goku / Cell / Berus\n"
                + "\n"
                + "|1|❹ TUYỆT THẾ ĐỆ TỬ\n"
                + "|1|  ○ 6,000 Kilis + Đệ 3K 100 tỷ\n"
                + "|1|  ○ Dùng Bình → Nhận Tuyệt Thế!\n"
                + "\n"
                + "|7|┌── ĐẶC QUYỀN TUYỆT THẾ ──┐\n"
                + "|2|│ HP/KI gốc: 900,000\n"
                + "|2|│ SĐ gốc: 40,000\n"
                + "|2|│ 10 ô trang bị (max game)\n"
                + "|2|│ 5 skill sẵn, không cần mở\n"
                + "|2|│ Mặc đồ cả 3 hệ\n"
                + "|1|│ ★ Fusion cộng THẲNG chỉ số!\n"
                + "|7|└────────────────────┘";

        createOtherMenu(player, ConstNpc.MENU_LTN_PET_GUIDE, guide,
                "Quay Lại");
    }
}
