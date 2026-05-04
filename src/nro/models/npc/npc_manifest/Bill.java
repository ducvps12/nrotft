package nro.models.npc.npc_manifest;

/**
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import consts.ConstNpc;
import item.Item;
import java.util.ArrayList;
import java.util.List;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.TaskService;
import services.func.ChangeMapService;
import shop.ShopService;

public class Bill extends Npc {

    public Bill(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            TaskService.gI().checkDoneTaskTalkNpc(player, this);
            if (mapId == 154) {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "...",
                        "Về\nthánh địa\nKaio", "Từ chối");
            } else {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Chưa tới giờ thi đấu, xem hướng dẫn để biết thêm chi tiết",
                        "Nói\nchuyện", "Hướng\ndẫn\nthêm", "Đổi thức ăn\nlấy phiếu ăn", "Đổi phiếu ăn\nlấy quà",
                        "Shop\nPhiếu\nGiảm Giá", "Từ chối");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 48 -> {
                    switch (player.iDMark.getIndexMenu()) {
                        case ConstNpc.BASE_MENU -> {
                            switch (select) {
                                case 0 -> {
                                    if (InventoryService.gI().canOpenBillShop(player)) {
                                        createOtherMenu(player, 2,
                                                "Đói bụng quá...ngươi mang cho ta 99 phần đồ ăn\nta sẽ cho một món đồ Hủy Diệt.\nNếu tâm trạng ta vui ngươi có thể nhận trang bị tăng đến 15%",
                                                "OK", "Từ chối");
                                    } else {
                                        createOtherMenu(player, 2,
                                                "Ngươi trang bị đủ bộ 5 món trang bị Thần\nvà mang 99 phần đồ ăn tới đây...\nrồi ta nói chuyện tiếp.",
                                                "OK");
                                    }
                                }
                                case 1 -> {
                                    NpcService.gI().createTutorial(player, tempId, this.avartar,
                                            ConstNpc.HUONG_DAN_BILL);
                                }
                                case 2 -> {
                                    createOtherMenu(player, 10, "Ngươi có muốn đổi thức ăn thành phiếu ăn nhanh không?",
                                            "Đồng ý",
                                            "Từ chối");
                                    player.iDMark.setIndexMenu(10);
                                }
                                case 3 -> {
                                    ShopService.gI().opendShop(player, "SHOP_SU_KIEN_VL", true);
                                }
                                case 4 -> {
                                    // Shop Phiếu Giảm Giá — dùng phiếu giảm giá (item 459) để đổi đồ
                                    Item phieu = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
                                    int soPhieu = (phieu != null) ? phieu.quantity : 0;
                                    if (soPhieu > 0) {
                                        Service.gI().sendThongBao(player, "Ban co " + soPhieu + " Phieu Giam Gia. Mua hang o shop nay se tru 1 phieu/mon!");
                                    } else {
                                        Service.gI().sendThongBao(player, "Ban chua co Phieu Giam Gia! Nhan tu goi VIP, Boss Odo, hoac su kien.");
                                    }
                                    ShopService.gI().opendShop(player, "SHOP_PHIEU_GIAM_GIA", false);
                                }
                            }
                        }
                        case 2 -> {
                            if (select == 0 && InventoryService.gI().canOpenBillShop(player)) {
                                ShopService.gI().opendShop(player, "BILL", false);
                                break;
                            }
                        }
                        case 10 -> {
                            if (select == 0) {
                                doiNhanhItem(player);
                            }
                        }

                    }
                }
                case 154 -> {
                    if (select == 0) {
                        ChangeMapService.gI().changeMap(player, 50, -1, 318, 336);
                        break;
                    }
                }
            }
        }
    }

    private void doiNhanhItem(Player player) {
        // Thức ăn thường (663-667): bánh pudding, xúc xích, kem dâu, mì ly, sushi
        // Thức ăn cho Thần (1947-1951): drop từ các nguồn khác
        int[] itemIds = { 663, 664, 665, 666, 667, 1947, 1948, 1949, 1950, 1951 };
        int total1946 = 0;

        for (int id : itemIds) {
            int count = 0;
            List<Item> itemsToSubtract = new ArrayList<>();

            for (Item item : player.inventory.itemsBag) {
                if (item != null && item.template != null && item.template.id == id) {
                    count += item.quantity;
                    itemsToSubtract.add(item);
                }
            }

            int soLanDoi = count / 99;
            int soLuongTru = soLanDoi * 99;

            if (soLanDoi > 0) {
                total1946 += soLanDoi;

                int remaining = soLuongTru;
                for (Item item : itemsToSubtract) {
                    if (remaining <= 0) {
                        break;
                    }

                    int toSub = Math.min(remaining, item.quantity);
                    InventoryService.gI().subQuantityItemsBag(player, item, toSub);
                    remaining -= toSub;
                }
            }
        }

        if (total1946 > 0) {
            Item item1946 = ItemService.gI().createNewItem((short) 1946);
            item1946.quantity = total1946;
            InventoryService.gI().addItemBag(player, item1946);

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã đổi thành công " + total1946 + " x Phiếu ăn.");
        } else {
            Service.gI().sendThongBao(player, "Bạn không có đủ 99 phần của bất kỳ vật phẩm nào để đổi.");
        }
    }
}
