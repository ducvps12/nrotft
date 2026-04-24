package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import item.Item;
import jdbc.daos.PlayerDAO;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.server.ServerManager;
import nro.server.maintenance.QuaToriBot;
import nro.services.InventoryService;
import nro.services.Service;

public class ToriBot extends Npc {

    private static final int MENU_MAIN = 0;
    private static final int MENU_NAP = 1000;
    private static final int MENU_VIP = 100;

    private static final int[] MOC_NAP_K = {
            10, 30, 50, 80, 120, 170, 220,
            300, 350, 440, 540, 600
    };

    public ToriBot(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
public void openBaseMenu(Player player) {
    if (!canOpenNpc(player) || player.getSession() == null)
        return;

    createOtherMenu(player, MENU_MAIN,
            "|0|TORI BOT"
            + "\nSố dư: " + player.getSession().cash + " VND",
            "Mua premium");
}

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player) || player.getSession() == null)
            return;

        switch (player.iDMark.getIndexMenu()) {

            case MENU_MAIN -> {
                if (select == 0)
                    openMenuVip(player);
            }

            case MENU_NAP -> handleNap(player, select);

            case MENU_VIP -> {
                switch (select) {

                    case 4 -> createOtherMenu(player, 3422,
                            "|0|VIP STATUS"
                                    + (player.vip == 1 ? "\nStatus VIP : VIP 1"
                                    : player.vip == 2 ? "\nStatus VIP : VIP 2"
                                    : player.vip == 3 ? "\nStatus VIP : VIP 3"
                                    : player.vip == 4 ? "\nStatus VIP : VIP 4" : "")
                                    + "\nCảm Ơn Đã Ủng Hộ Ngọc Rồng Online",
                            "Đóng");

                    case 0 -> createOtherMenu(player, 223,
                            "|0|Nâng Cấp VIP 1: 300.000 Điểm Mùa\n"
                                    + "- 500 Thỏi Vàng\n"
                                    + "- 10 Phiếu Giảm Giá 80%\n"
                                    + "- 20 Đá Bảo Vệ\n"
                                    + "- Cải Trang Black Gohan 15 Ngày\n"
                                    + "- Pet Chó 3 Đầu Địa Ngục vĩnh viễn\n"
                                    + "- 9999 Mảnh Vỡ Bông Tai\n"
                                    + "- 10 Mảnh Khí Oozaru\n",
                            "300.000 VND", "Đóng");

                    case 1 -> createOtherMenu(player, 224,
                            "|0|Nâng Cấp VIP 2: 700.000 Điểm Mùa\n"
                                    + "- 1500 Thỏi Vàng\n"
                                    + "- 10 Phiếu Giảm Giá\n"
                                    + "- 150 Đá Bảo Vệ\n"
                                    + "- Cải Trang Black Gohan Vĩnh Viễn\n"
                                    + "- 1 Pet Ông Già Noel Vĩnh Viễn\n"
                                    + "- 20 Mảnh Rồng Thần Namek\n",
                            "700.000 VND", "Đóng");

                    case 2 -> createOtherMenu(player, 225,
                            "|0|Nâng Cấp VIP 3: 1.500.000 Điểm Mùa\n"
                                    + "- 10000 Thỏi Vàng\n"
                                    + "- 10 Phiếu Giảm Giá\n"
                                    + "- 500 Đá Bảo Vệ\n"
                                    + "- 1 Cải Trang Broly Hắc Vương VIP\n"
                                    + "- 1 Pet Tuần Lộc VIP\n"
                                    + "- 230 Mảnh Khí Oozaru\n"
                                    + "- 30 Hộp Sao Pha Lê VIP\n"
                                    + "- 5000 Mảnh Vỡ Bông Tai Cấp 3\n",
                            "1.500.000 VND", "Đóng");

                    case 3 -> createOtherMenu(player, 226,
                            "|0|Nâng Cấp VIP 4: 2.000.000\n"
                                    + "- 15.000 Thỏi Vàng\n"
                                    + "- 500 Đá Bảo Vệ\n"
                                    + "- 1 Cải Trang Pan VIP\n"
                                    + "- 1 Ván Bay Rồng Thiêng VIP\n"
                                    + "- 45 Mảnh Rồng Thần Namek\n"
                                    + "- 20 Mảnh Thẻ Đội Trưởng Vàng\n"
                                    + "- 10.000 Mảnh Vỡ Bông Tai Cấp 3\n",
                            "2.000.000 VND", "Đóng");
                }
            }

            case 223 -> buyVip(player, 1, 300_000, 7, () -> QuaToriBot.Qua_1(player));
            case 224 -> buyVip(player, 2, 700_000, 8, () -> QuaToriBot.Qua_2(player));
            case 225 -> buyVip(player, 3, 1_500_000, 8, () -> QuaToriBot.Qua_3(player));
            case 226 -> buyVip(player, 4, 2_000_000, 9, () -> QuaToriBot.Qua_4(player));
        }
    }

    private void openMenuVip(Player player) {
        createOtherMenu(player, MENU_VIP,
                "Mua gì đê",
                "Premium 1", "Premium 2", "Premium 3", "Premium 4");
    }

    private void handleNap(Player player, int select) {
        if (select < 0 || select >= MOC_NAP_K.length)
            return;

        int mocK = MOC_NAP_K[select];
        int mocTien = mocK * 1000;
        int tv = (select + 1) * 5;

        if (player.getSession().danap < mocTien) {
            Service.gI().sendThongBaoOK(player, "Bạn chưa đủ mốc " + mocK + "K");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBaoOK(player, "Cần 1 ô trống hành trang");
            return;
        }

        PlayerDAO.subDaNap(player, mocTien);

        Item item = new Item((short) 457);
        item.quantity = tv;

        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
    }

   private void buyVip(Player player, int vip, int cost, int bag, Runnable reward) {

    if (PlayerDAO.checkPremium(player, vip)) {
        npcChat(player, "Bạn đã mua Premium " + vip + " rồi");
        return;
    }

    if (player.getSession().cash < cost) {
        Service.gI().sendThongBaoOK(player,
                "Điểm tích lũy chưa đủ.\nTruy Cập: " + ServerManager.LINK);
        return;
    }

    if (InventoryService.gI().getCountEmptyBag(player) < bag) {
        npcChat(player, "Cần " + bag + " ô trống hành trang");
        return;
    }

    // trừ tiền trước
    PlayerDAO.subcash(player, cost, "MUA_VIP", "VIP:" + vip + " Cost:" + cost);

    // lưu đã mua
    PlayerDAO.setPremium(player, vip);

    player.vip = (byte) vip;
    player.timevip = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000;

    // nhận quà
    reward.run();

    npcChat(player, "Kích hoạt thành công Premium " + vip);
}
}