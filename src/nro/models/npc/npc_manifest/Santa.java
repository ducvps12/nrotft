package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import jdbc.daos.PlayerDAO;
import models.ClanBoss.ClanBoss;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.server.Manager;
import nro.services.InventoryService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.TimeUtil;
import utils.Util;
import shop.ShopService;

public class Santa extends Npc {

    private static final int MENU_GUILD = 1001;
    private static final int MENU_UP_CLAN = 1002;
    private static final int MENU_DOI_VND = 1003;

    public Santa(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        createOtherMenu(
                player,
                ConstNpc.BASE_MENU,
                "Xin chào, ta có một số vật phẩm đặc biệt dành cho ngươi",
                "Cửa hàng",
                "Danh hiệu",
                "Cửa hàng\nPhụ kiện",
                "Shop xu",
                "Đổi VNĐ");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.iDMark.isBaseMenu()) {
            switch (select) {
                case 0 ->
                    ShopService.gI().opendShop(player, "SANTA", false);
                case 1 ->
                    ShopService.gI().opendShop(player, "SANTA_DANH_HIEU", false);
                case 2 ->
                    ShopService.gI().opendShop(player, "SANTA_PHUKIEN", false);
                case 3 ->
                    ShopService.gI().opendShop(player, "LUNAR_NEW_YEAR", false);
                case 4 ->
                    openMenuDoiVnd(player);
            }
            return;
        }

        if (player.iDMark.getIndexMenu() == MENU_GUILD) {
            switch (select) {
                case 0 ->
                    ShopService.gI().opendShop(player, "SANTA_GUILD", false);
                case 1 ->
                    openMenuUpClan(player);
                case 2 ->
                    goToClanBossMap(player);
            }
            return;
        }

        if (player.iDMark.getIndexMenu() == MENU_DOI_VND) {
            switch (select) {
                case 0 -> doiVnd(player, 100, 1_000);   // 100 xu = 1k VNĐ
                case 1 -> doiVnd(player, 500, 5_000);   // 500 xu = 5k VNĐ
                case 2 -> doiVnd(player, 1000, 10_000);  // 1000 xu = 10k VNĐ
            }
            return;
        }

        if (player.iDMark.getIndexMenu() == MENU_UP_CLAN) {
            if (select == 0) {
                upgradeClanLevel(player);
            }
        }
    }

    private void openMenuUpClan(Player player) {
        createOtherMenu(
                player,
                MENU_UP_CLAN,
                "Nâng cấp bang hội\n"
                        + "Cấp hiện tại: " + player.clan.level + "\n"
                        + "Giá: 10.000 VNĐ / 1 cấp\n"
                        + "Mỗi cấp +5% HP, KI, SD",
                "Nâng cấp",
                "Đóng");
    }

    private void openMenuDoiVnd(Player player) {
        createOtherMenu(
                player,
                MENU_DOI_VND,
                "Đổi Xu NRO → VNĐ\n"
                        + "Hiện có: " + Util.numberToMoney(player.getSession().cash) + " VNĐ\n"
                        + "Không giới hạn lượt đổi!",
                "100 xu\n1k VNĐ",
                "500 xu\n5k VNĐ",
                "1000 xu\n10k VNĐ");
    }

    private void upgradeClanLevel(Player player) {
        if (player.clan == null) {
            return;
        }

        if (player.clan.level >= 5) {
            Service.gI().sendThongBao(player,
                    "Bang hội đã đạt cấp tối đa (5)\nKhông thể nâng cấp thêm");
            return;
        }

        // dùng cash thay vì vnd
        if (!PlayerDAO.subcash(player, 10000, "NANG_CAP_BANG", "ClanLv:" + player.clan.level)) {
            Service.gI().sendThongBao(player, "Bạn không đủ 10.000 VNĐ");
            return;
        }

        player.clan.level++;

        Manager.updateClanLevel(player.clan);

        Service.gI().sendMoney(player);

        Service.gI().sendThongBao(
                player,
                "Nâng cấp bang hội thành công\nBang hiện tại cấp " + player.clan.level);
    }

    private void goToClanBossMap(Player player) {
        if (!TimeUtil.isClanBossOpen()) {
            Service.gI().sendThongBao(player,
                    "Map săn Boss Bang Hội chỉ mở trong khung giờ "
                            + ClanBoss.HOUR_OPEN + "h - " + ClanBoss.HOUR_CLOSE + "h");
            return;
        }

        ChangeMapService.gI().changeMapBySpaceShip(player, 175, -1, -1);
    }

    private void doiVnd(Player player, int xuCan, int vndNhan) {
        var itemXu = InventoryService.gI().findItemBag(player, 1705);

        if (itemXu == null || itemXu.quantity < xuCan) {
            Service.gI().sendThongBao(player, "Không đủ xu (cần " + xuCan + " Xu NRO)");
            return;
        }

        // trừ xu
        itemXu.quantity -= xuCan;
        if (itemXu.quantity <= 0) {
            InventoryService.gI().removeItemBag(player, itemXu);
        }

        // cộng cash DB (KHÔNG cộng danap)
        if (!PlayerDAO.addCashNoDanap(player.getSession().userId, vndNhan, "DOI_XU_VND",
                "Xu:" + xuCan + " VND:" + vndNhan)) {
            Service.gI().sendThongBao(player, "Lỗi cộng VNĐ");
            return;
        }

        // cộng vào player đang online
        player.getSession().cash += vndNhan;
        player.getSession().vnd += vndNhan;

        // update lại túi đồ
        InventoryService.gI().sendItemBag(player);

        // gửi lại tiền
        Service.gI().sendMoney(player);

        Service.gI().sendThongBao(player,
                "Đổi thành công " + xuCan + " xu → " + Util.numberToMoney(vndNhan) + " VNĐ");
    }
}