package nro.models.npc.npc_manifest;

/**
 *  NPC Jaco - Hỗ trợ di chuyển hành tinh + Sự kiện Thần Thú Cổ Đại
 */

import consts.ConstNpc;
import event.EventManager;
import item.Item;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Util;

public class Jaco extends Npc {

    public Jaco(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 24 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Go Ten, Calich va Monaka dang gap chuyen o hanh tinh\nPotaufeu\nHay den do ngay",
                            "Den\nPotaufeu", "Tu choi");
                case 139 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Tau Vu Tru cua ta co the dua cau den hanh tinh khac chi trong 3 giay.\nCau muon di dau?",
                            "Den\nTrai Dat", "Den\nNamec", "Den\nXayda", "Tu choi");
                case 176 -> {
                    // Sự kiện Thần Thú Cổ Đại - NPC ở Cung Trăng
                    if (EventManager.THAN_THU_CO_DAI) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Ta la Jaco - Canh sat Ngan ha!\nSu kien Than Thu Co Dai dang dien ra!\n"
                                + "Giet mob tai day de thu thap 3 Linh Phu.\nGom du 3 loai mang den ta de trieu hoi Boss an!",
                                "Trieu hoi\nThan Long", "Di den\nVung Dat\nHuyen Thoai",
                                "Huong dan\nsu kien", "Dong");
                    } else {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Ta la Jaco - Canh sat Ngan ha!\nHien tai su kien Than Thu chua bat dau.\nHay cho thong bao tu He Thong!",
                                "Di den\nVung Dat\nHuyen Thoai", "Dong");
                    }
                }
                case 178 -> {
                    // NPC ở Vùng Đất Huyền Thoại - quay về Cung Trăng
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Day la Vung Dat Huyen Thoai!\nNoi sinh song cua Than Thu Co Dai.\nCau muon lam gi?",
                            "Quay ve\nCung Trang", "Dong");
                }
                default -> {
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (this.mapId) {
                    case 24 -> {
                        if (select == 0) {
                            ChangeMapService.gI().goToPotaufeu(player);
                        }
                    }
                    case 139 -> {
                        switch (select) {
                            case 0 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 24, -1, -1);
                            case 1 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 25, -1, -1);
                            case 2 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 26, -1, -1);
                        }
                    }
                    case 176 -> {
                        if (EventManager.THAN_THU_CO_DAI) {
                            switch (select) {
                                case 0 -> {
                                    // Triệu hồi Thần Long Cổ Đại - cần 3 Linh Phù
                                    tryToSummonThanLong(player);
                                }
                                case 1 -> {
                                    // Đi đến Vùng Đất Huyền Thoại (map 178)
                                    ChangeMapService.gI().changeMapBySpaceShip(player, 178, -1, -1);
                                }
                                case 2 -> {
                                    // Hướng dẫn sự kiện
                                    Service.gI().sendThongBao(player,
                                        "=== THAN THU CO DAI ===\n"
                                        + "1. Den Cung Trang (176) hoac Vung Dat Huyen Thoai (178)\n"
                                        + "2. Giet mob de nhan Linh Phu\n"
                                        + "   - Linh Phu Voi (663): 8% tai Cung Trang\n"
                                        + "   - Linh Phu Ga (664): 8% tai Vung Dat HT\n"
                                        + "   - Linh Phu Ngua (665): 5% ca 2 map\n"
                                        + "3. Gom du 3 loai Linh Phu (moi loai x1)\n"
                                        + "4. Mang 3 Linh Phu den NPC Jaco\n"
                                        + "   -> Trieu hoi Boss an: Than Long Co Dai\n"
                                        + "5. Boss an drop item cuc hiem!");
                                }
                            }
                        } else {
                            // Không có event - chỉ có option đi map 178
                            if (select == 0) {
                                ChangeMapService.gI().changeMapBySpaceShip(player, 178, -1, -1);
                            }
                        }
                    }
                    case 178 -> {
                        if (select == 0) {
                            // Quay về Cung Trăng
                            ChangeMapService.gI().changeMapBySpaceShip(player, 176, -1, -1);
                        }
                    }
                }
            }
        }
    }

    /**
     * Kiểm tra và triệu hồi Thần Long Cổ Đại khi đủ 3 Linh Phù
     */
    private void tryToSummonThanLong(Player player) {
        // Tìm item trong bag
        Item itemVoi = null, itemGa = null, itemNgua = null;
        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            Item item = player.inventory.itemsBag.get(i);
            if (item != null) {
                if (item.template.id == 663 && itemVoi == null) itemVoi = item;
                if (item.template.id == 664 && itemGa == null) itemGa = item;
                if (item.template.id == 665 && itemNgua == null) itemNgua = item;
            }
        }

        if (itemVoi == null || itemGa == null || itemNgua == null) {
            Service.gI().sendThongBao(player,
                "Ban chua du 3 Linh Phu!\n"
                + "Can: Linh Phu Voi (663), Linh Phu Ga (664), Linh Phu Ngua (665)\n"
                + "Moi loai 1 cai. Hay giet mob de thu thap!");
            return;
        }

        // Trừ Linh Phù
        nro.services.InventoryService.gI().subQuantityItemsBag(player, itemVoi, 1);
        nro.services.InventoryService.gI().subQuantityItemsBag(player, itemGa, 1);
        nro.services.InventoryService.gI().subQuantityItemsBag(player, itemNgua, 1);

        // Spawn boss Thần Long Cổ Đại tại zone hiện tại
        try {
            boss.Boss bossObj = boss.BossManager.gI().createBoss(boss.BossID.THAN_LONG_CO_DAI);
            if (bossObj != null && player.zone != null) {
                bossObj.joinMapByZone(player.zone);
                Service.gI().sendThongBao(player, "Than Long Co Dai da xuat hien!\nHa guc no de nhan phan thuong!");
                // Thông báo cho cả zone
                nro.services.ChatGlobalService.gI().autoChatGlobal(player,
                    "[ He Thong ] " + player.name + " vua trieu hoi Than Long Co Dai tai Cung Trang!");
            } else {
                Service.gI().sendThongBao(player, "Khong the trieu hoi Boss luc nay. Thu lai sau!");
            }
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Loi khi trieu hoi Boss. Thu lai sau!");
        }
    }
}
