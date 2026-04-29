package nro.services;

/*
 * IntrinsicService - Hệ thống Nội Tại
 * Refactored: Pricing, UI, Quality Tiers
 */

import consts.ConstNpc;
import intrinsic.Intrinsic;
import nro.player.Player;
import nro.server.Manager;
import network.Message;
import utils.Util;
import java.util.List;

public class IntrinsicService {

    private static IntrinsicService I;

    // Bảng giá vàng leo thang (đơn vị: triệu)
    // Lần 1: 5Tr, 2: 10Tr, 3: 20Tr, 4: 40Tr, 5: 80Tr, 6: 120Tr, 7: 160Tr, 8: 200Tr
    private static final int[] COST_OPEN = { 5, 10, 20, 40, 80, 120, 160, 200 };

    // Giá ngọc mở VIP (reset bộ đếm vàng + roll tỉ lệ vàng cao)
    private static final int GEM_COST_VIP = 20;

    // Yêu cầu sức mạnh tối thiểu
    private static final long MIN_POWER = 5_000_000_000L; // 5 tỷ SM

    public static IntrinsicService gI() {
        if (IntrinsicService.I == null) {
            IntrinsicService.I = new IntrinsicService();
        }
        return IntrinsicService.I;
    }

    public List<Intrinsic> getIntrinsics(byte playerGender) {
        switch (playerGender) {
            case 0:
                return Manager.INTRINSIC_TD;
            case 1:
                return Manager.INTRINSIC_NM;
            default:
                return Manager.INTRINSIC_XD;
        }
    }

    public Intrinsic getIntrinsicById(int id) {
        for (Intrinsic intrinsic : Manager.INTRINSICS) {
            if (intrinsic.id == id) {
                return new Intrinsic(intrinsic);
            }
        }
        return null;
    }

    // ======================== QUALITY TIER SYSTEM ========================
    // Dựa trên param1 so với range [paramFrom1..paramTo1]
    // Thường: 0-40% range | Tốt: 40-70% | Xuất sắc: 70-90% | Vàng: 90-100%
    private String getQualityName(Intrinsic intrinsic) {
        if (intrinsic.id == 0) return "";
        int range = intrinsic.paramTo1 - intrinsic.paramFrom1;
        if (range <= 0) return "Thường";
        int pos = intrinsic.param1 - intrinsic.paramFrom1;
        double percent = (double) pos / range * 100;
        if (percent >= 90) return "§Vàng§";
        if (percent >= 70) return "§Xuất sắc§";
        if (percent >= 40) return "§Tốt§";
        return "Thường";
    }

    private String getQualityColor(Intrinsic intrinsic) {
        if (intrinsic.id == 0) return "";
        int range = intrinsic.paramTo1 - intrinsic.paramFrom1;
        if (range <= 0) return "";
        int pos = intrinsic.param1 - intrinsic.paramFrom1;
        double percent = (double) pos / range * 100;
        if (percent >= 90) return " ★★★";
        if (percent >= 70) return " ★★";
        if (percent >= 40) return " ★";
        return "";
    }

    // ======================== UI METHODS ========================

    public void sendInfoIntrinsic(Player player) {
        Message msg;
        try {
            msg = new Message(112);
            msg.writer().writeByte(0);
            msg.writer().writeShort(player.playerIntrinsic.intrinsic.icon);
            msg.writer().writeUTF(player.playerIntrinsic.intrinsic.getName());
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void showAllIntrinsic(Player player) {
        List<Intrinsic> listIntrinsic = getIntrinsics(player.gender);
        Message msg;
        try {
            msg = new Message(112);
            msg.writer().writeByte(1);
            msg.writer().writeByte(1); // count tab
            msg.writer().writeUTF("Nội tại");
            msg.writer().writeByte(listIntrinsic.size() - 1);
            for (int i = 1; i < listIntrinsic.size(); i++) {
                msg.writer().writeShort(listIntrinsic.get(i).icon);
                msg.writer().writeUTF(listIntrinsic.get(i).getDescription());
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    // ======================== SET THẦN LINH / HỦY DIỆT MENUS ========================

    public void settltd(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.SET_TLTD, -1,
                "chọn lẹ đi để tau đi chơi với ny", "Set\nThiên Xin Hăn", "Set\nGenki", "Set\nKamejoko", "Từ chối");
    }

    public void settlnm(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.SET_TLNM, -1,
                "chọn lẹ đi để tau đi chơi với ny", "Set\nPicolo", "Set\nỐc Tiêu", "Set\nPikkoro Daimao", "Từ chối");
    }

    public void settlxd(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.SET_TLXD, -1,
                "chọn lẹ đi để tau đi chơi với ny", "Set\nKakarot", "Set\nCadic", "Set\nNappa", "Từ chối");
    }

    public void sethdtd(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.SET_HDTD, -1,
                "chọn lẹ đi để tau đi chơi với ny", "Set\nTien Xin Han", "Set\nGenki", "Set\nKamejoko", "Từ chối");
    }

    public void sethdnm(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.SET_HDNM, -1,
                "chọn lẹ đi để tau đi chơi với ny", "Set\nPicolo", "Set\nỐc Tiêu", "Set\nPikkoro Daimao", "Từ chối");
    }

    public void sethdxd(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.SET_HDXD, -1,
                "chọn lẹ đi để tau đi chơi với ny", "Set\nKakarot", "Set\nCadic", "Set\nNappa", "Từ chối");
    }

    public void sattd(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.menutd, -1,
                "Chọn đi cậu", "Set\nKamejoko", "Set\nThên xin hăng", "Set\nSet Krillin", "Từ chối");
    }

    public void satnm(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.menunm, -1,
                "Chọn đi cậu", "Set\nLiên hoàn", "Set\nPicolo", "Set\nPikkoro Daimao", "Từ chối");
    }

    public void setxd(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.menuxd, -1,
                "Chọn đi cậu", "Set\nKakarot", "Set\nCađíc", "Set\nNappa", "Từ chối");
    }

    // ======================== MAIN MENU ========================

    public void showMenu(Player player) {
        // Hiển thị thông tin nội tại hiện tại
        Intrinsic current = player.playerIntrinsic.intrinsic;
        String currentInfo;
        if (current != null && current.id != 0) {
            String quality = getQualityName(current);
            String stars = getQualityColor(current);
            String shortName = current.getName();
            // Cắt bớt phần [x đến y] nếu quá dài
            int bracket = shortName.indexOf(" [");
            if (bracket > 0) shortName = shortName.substring(0, bracket);
            currentInfo = "Nội tại hiện tại: " + shortName
                    + "\nChất lượng: " + quality + stars
                    + "\n\nGiá mở tiếp: " + getCurrentGoldCost(player) + " Tr vàng"
                    + "\nMở VIP: " + GEM_COST_VIP + " ngọc (roll chất lượng cao + reset giá)";
        } else {
            currentInfo = "Bạn chưa kích hoạt Nội Tại!"
                    + "\nNội tại là kỹ năng bị động\nhỗ trợ đặc biệt cho nhân vật."
                    + "\n\nGiá mở: " + getCurrentGoldCost(player) + " Tr vàng"
                    + "\nYêu cầu SM tối thiểu " + Util.numberToMoney(MIN_POWER);
        }

        NpcService.gI().createMenuConMeo(player, ConstNpc.INTRINSIC, -1,
                currentInfo,
                "Xem\ntất cả\nNội Tại", "Mở\nNội Tại\n(" + getCurrentGoldCost(player) + "Tr)",
                "Mở VIP\n(" + GEM_COST_VIP + " ngọc)", "Từ chối");
    }

    private int getCurrentGoldCost(Player player) {
        int idx = Math.min(player.playerIntrinsic.countOpen, COST_OPEN.length - 1);
        return COST_OPEN[idx];
    }

    public void showConfirmOpen(Player player) {
        int cost = getCurrentGoldCost(player);
        String rollInfo = "Mở Nội Tại với giá " + cost + " Tr vàng?"
                + "\n\nChất lượng roll: Ngẫu nhiên"
                + "\n(Thường → Tốt → Xuất sắc → Vàng)"
                + "\nLần mở thứ: " + (player.playerIntrinsic.countOpen + 1) + "/" + COST_OPEN.length;
        NpcService.gI().createMenuConMeo(player, ConstNpc.CONFIRM_OPEN_INTRINSIC, -1,
                rollInfo,
                "Mở\nNội Tại", "Từ chối");
    }

    public void showConfirmOpenVip(Player player) {
        String vipInfo = "Mở Nội Tại VIP với " + GEM_COST_VIP + " ngọc?"
                + "\n\nƯu đãi VIP:"
                + "\n• Roll chất lượng cao (Tốt trở lên)"
                + "\n• Tái lập giá vàng về " + COST_OPEN[0] + " Tr"
                + "\n• Tỉ lệ roll Vàng: 15%";
        NpcService.gI().createMenuConMeo(player, ConstNpc.CONFIRM_OPEN_INTRINSIC_VIP, -1,
                vipInfo,
                "Mở\nNội VIP", "Từ chối");
    }

    // ======================== CHANGE INTRINSIC LOGIC ========================

    private void changeIntrinsic(Player player, boolean isVip) {
        List<Intrinsic> listIntrinsic = getIntrinsics(player.gender);
        if (listIntrinsic.size() <= 1) {
            Service.gI().sendThongBao(player, "Không có nội tại nào để mở!");
            return;
        }

        // Random nội tại (skip ID 0)
        player.playerIntrinsic.intrinsic = new Intrinsic(listIntrinsic.get(Util.nextInt(1, listIntrinsic.size() - 1)));

        // Roll param dựa trên VIP hay không
        if (isVip) {
            // VIP: roll từ 40% range trở lên, 15% chance Vàng
            int range = player.playerIntrinsic.intrinsic.paramTo1 - player.playerIntrinsic.intrinsic.paramFrom1;
            int minRoll = player.playerIntrinsic.intrinsic.paramFrom1 + (int)(range * 0.4); // Tốt trở lên

            if (Util.isTrue(15, 100)) {
                // 15% chance: Vàng (90-100%)
                int goldMin = player.playerIntrinsic.intrinsic.paramFrom1 + (int)(range * 0.9);
                player.playerIntrinsic.intrinsic.param1 = (short) Util.nextInt(goldMin, player.playerIntrinsic.intrinsic.paramTo1);
            } else if (Util.isTrue(30, 100)) {
                // 30% chance: Xuất sắc (70-90%)
                int excMin = player.playerIntrinsic.intrinsic.paramFrom1 + (int)(range * 0.7);
                int excMax = player.playerIntrinsic.intrinsic.paramFrom1 + (int)(range * 0.9);
                player.playerIntrinsic.intrinsic.param1 = (short) Util.nextInt(excMin, excMax);
            } else {
                // 55% chance: Tốt (40-70%)
                int goodMax = player.playerIntrinsic.intrinsic.paramFrom1 + (int)(range * 0.7);
                player.playerIntrinsic.intrinsic.param1 = (short) Util.nextInt(minRoll, goodMax);
            }
        } else {
            // Thường: full random
            player.playerIntrinsic.intrinsic.param1 = (short) Util.nextInt(
                    player.playerIntrinsic.intrinsic.paramFrom1,
                    player.playerIntrinsic.intrinsic.paramTo1);
        }

        player.playerIntrinsic.intrinsic.param2 = (short) Util.nextInt(
                player.playerIntrinsic.intrinsic.paramFrom2,
                player.playerIntrinsic.intrinsic.paramTo2);

        // Hiển thị kết quả
        String quality = getQualityName(player.playerIntrinsic.intrinsic);
        String stars = getQualityColor(player.playerIntrinsic.intrinsic);
        String resultName = player.playerIntrinsic.intrinsic.getName();
        int bracket = resultName.indexOf(" [");
        if (bracket > 0) resultName = resultName.substring(0, bracket);

        Service.gI().sendThongBao(player, "Bạn nhận được Nội tại:\n" + resultName
                + "\nChất lượng: " + quality + stars);
        sendInfoIntrinsic(player);
    }

    // ======================== OPEN (GOLD) ========================

    public void open(Player player) {
        if (player.nPoint.power < MIN_POWER) {
            Service.gI().sendThongBao(player, "Yêu cầu sức mạnh tối thiểu "
                    + Util.numberToMoney(MIN_POWER));
            return;
        }

        int costIndex = Math.min(player.playerIntrinsic.countOpen, COST_OPEN.length - 1);
        long goldRequire = (long) COST_OPEN[costIndex] * 1_000_000L;

        if (player.inventory.gold < goldRequire) {
            Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu "
                    + Util.numberToMoney(goldRequire - player.inventory.gold) + " vàng nữa");
            return;
        }

        player.inventory.gold -= goldRequire;
        PlayerService.gI().sendInfoHpMpMoney(player);
        changeIntrinsic(player, false);

        if (player.playerIntrinsic.countOpen < COST_OPEN.length - 1) {
            player.playerIntrinsic.countOpen++;
        }
    }

    // ======================== OPEN VIP (GEM) ========================

    public void openVip(Player player) {
        if (player.nPoint.power < MIN_POWER) {
            Service.gI().sendThongBao(player, "Yêu cầu sức mạnh tối thiểu "
                    + Util.numberToMoney(MIN_POWER));
            return;
        }

        if (player.inventory.gem < GEM_COST_VIP) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc, còn thiếu "
                    + (GEM_COST_VIP - player.inventory.gem) + " ngọc nữa");
            return;
        }

        player.inventory.gem -= GEM_COST_VIP;
        PlayerService.gI().sendInfoHpMpMoney(player);
        changeIntrinsic(player, true);
        player.playerIntrinsic.countOpen = 0; // Reset bộ đếm vàng
    }

}
