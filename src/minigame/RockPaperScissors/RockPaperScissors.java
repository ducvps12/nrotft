
/*
 * minhluong
 */
package minigame.RockPaperScissors;

import consts.ConstFont;
import consts.ConstMiniGame;
import nro.models.npc.Npc;
import nro.models.npc.npc_manifest.LyTieuNuong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nro.player.Player;
import nro.services.ItemTimeService;
import nro.services.Service;
import utils.Util;

public class RockPaperScissors {

    public static final byte KEO = 0;
    public static final byte BUA = 1;
    public static final byte BAO = 2;
    private static final Logger log = LoggerFactory.getLogger(RockPaperScissors.class);

    public static long timePlay = 15; // có 15 giây để chơi

    public static final long COST_0 = 500_000;      // 500k vàng
    public static final long COST_1 = 2_000_000;     // 2M vàng
    public static final long COST_2 = 5_000_000;     // 5M vàng
    public static final long COST_3 = 10_000_000;    // 10M vàng
    public static final long COST_4 = 25_000_000;    // 25M vàng
    public static final long COST_5 = 50_000_000;    // 50M vàng

    public static void confirmMenu(Npc npc, Player player, int select) { // xử lý chọn menu => chuyển qua một menu mới
        long tiendatcuoc = switch (select) {
            case 0 -> COST_0;
            case 1 -> COST_1;
            case 2 -> COST_2;
            case 3 -> COST_3;
            case 4 -> COST_4;
            case 5 -> COST_5;
            default -> COST_0;
        };
        String money = Util.numberFormatLouis(tiendatcuoc);
        player.iDMark.setMoneyKeoBuaBao((int) tiendatcuoc);
        player.iDMark.setTimePlayKeoBuaBao(System.currentTimeMillis() + (timePlay * 1000));
        ItemTimeService.gI().sendTextTimeKeoBuaBao(player, (int) timePlay);
        npc.createOtherMenu(player, ConstMiniGame.MENU_PLAY_KEO_BUA_BAO,
                ConstFont.BOLD_GREEN + "Mức vàng cược: " + money + "\n"
                        + ConstFont.BOLD_DARK + "Hãy chọn Kéo, Búa hoặc Bao\n"
                        + ConstFont.BOLD_RED + "Thời gian " + timePlay + " giây bắt đầu",
                "Kéo", "Búa", "Bao", "Đổi\nmức cược", "Nghỉ chơi");
    }

    public static void confirmPlay(Npc npc, Player player, int select) {
        switch (select) {
            case 0, 1, 2:
                if (player.inventory.gold < player.iDMark.getMoneyKeoBuaBao()) {
                    long soVangConThieu = player.iDMark.getMoneyKeoBuaBao() - player.inventory.gold;
                    Service.gI().sendThongBao(player,
                            "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(soVangConThieu) + " vàng nữa");
                    return;
                }
                player.iDMark.setKeoBuaBaoPlayer((byte) select);
                player.iDMark.setKeoBuaBaoServer((byte) Util.nextInt(0, 2));
                if (RockPaperScissorsService.checkWinLose(player) == 1) { // xử lý thắng
                    RockPaperScissorsService.winKeoBuaBao(npc, player);
                } else if (RockPaperScissorsService.checkWinLose(player) == 2) { // xử lý thua
                    RockPaperScissorsService.loseKeoBuaBao(npc, player);
                } else { // xử lý hoà
                    RockPaperScissorsService.hoaKeoBuaBao(npc, player);
                }
                break;
            case 3:
                npc.createOtherMenu(player, ConstMiniGame.MENU_KEO_BUA_BAO,
                        "Hãy chọn mức cược.",
                        "500K vàng",
                        "2 Tr vàng",
                        "5 Tr vàng",
                        "10 Tr vàng",
                        "25 Tr vàng",
                        "50 Tr vàng");
                break;
            default:
                break;
        }
    }
}
