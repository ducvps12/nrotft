package nro.server.maintenance;

import item.Item;
import item.Item.ItemOption;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;

public class QuaToriBot {

    public static void Qua_1(Player player) {
        if (player == null) return;

        if (InventoryService.gI().getCountEmptyBag(player) < 7) {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống");
            return;
        }

        Item ThoiVang = ItemService.gI().createNewItem((short) 457, 5000);
        Item PhieuGiamGia = ItemService.gI().createNewItem((short) 459, 10);
        Item DaBaoVe = ItemService.gI().createNewItem((short) 987, 20);
        Item CaiTrang = ItemService.gI().createNewItem((short) 883, 1);
        Item Pet = ItemService.gI().createNewItem((short) 1654, 1);
        Item Item933 = ItemService.gI().createNewItem((short) 933, 9999);
        Item ManhKhiOozaru = ItemService.gI().createNewItem((short) 1901, 10);

        ThoiVang.itemOptions.add(new ItemOption(100, 1));
        ThoiVang.itemOptions.add(new ItemOption(30, 1));

        PhieuGiamGia.itemOptions.add(new ItemOption(73, 1));
        DaBaoVe.itemOptions.add(new ItemOption(73, 1));

        CaiTrang.itemOptions.add(new ItemOption(50, 30));
        CaiTrang.itemOptions.add(new ItemOption(103, 30));
        CaiTrang.itemOptions.add(new ItemOption(77, 30));
        CaiTrang.itemOptions.add(new ItemOption(14, 10));
        CaiTrang.itemOptions.add(new ItemOption(93, 15));
        CaiTrang.itemOptions.add(new ItemOption(5, 35));

        Pet.itemOptions.add(new ItemOption(50, 15));
        Pet.itemOptions.add(new ItemOption(77, 15));
        Pet.itemOptions.add(new ItemOption(103, 15));
        Pet.itemOptions.add(new ItemOption(101, 50));

        ManhKhiOozaru.itemOptions.add(new ItemOption(73, 1));

        InventoryService.gI().addItemBag(player, ThoiVang);
        InventoryService.gI().addItemBag(player, PhieuGiamGia);
        InventoryService.gI().addItemBag(player, DaBaoVe);
        InventoryService.gI().addItemBag(player, CaiTrang);
        InventoryService.gI().addItemBag(player, Pet);
        InventoryService.gI().addItemBag(player, Item933);
        InventoryService.gI().sendItemBag(player);
    }

    public static void Qua_2(Player player) {
        if (player == null) return;

        if (InventoryService.gI().getCountEmptyBag(player) < 6) {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống");
            return;
        }

        Item ThoiVang = ItemService.gI().createNewItem((short) 457, 10000);
        Item PhieuGiamGia = ItemService.gI().createNewItem((short) 459, 10);
        Item DaBaoVe = ItemService.gI().createNewItem((short) 987, 150);
        Item CaiTrang = ItemService.gI().createNewItem((short) 883, 1);
        Item PhuKien = ItemService.gI().createNewItem((short) 1452, 1);
        Item TheTieuDoiTruongVang = ItemService.gI().createNewItem((short) 1204, 40);

        ThoiVang.itemOptions.add(new ItemOption(100, 1));
        ThoiVang.itemOptions.add(new ItemOption(30, 1));

        PhieuGiamGia.itemOptions.add(new ItemOption(73, 1));

        DaBaoVe.itemOptions.add(new ItemOption(73, 1));

        CaiTrang.itemOptions.add(new ItemOption(50, 30));
        CaiTrang.itemOptions.add(new ItemOption(103, 35));
        CaiTrang.itemOptions.add(new ItemOption(77, 35));
        CaiTrang.itemOptions.add(new ItemOption(14, 10));
        CaiTrang.itemOptions.add(new ItemOption(5, 50));

        PhuKien.itemOptions.add(new ItemOption(50, 15));
        PhuKien.itemOptions.add(new ItemOption(77, 15));
        PhuKien.itemOptions.add(new ItemOption(103, 15));
        PhuKien.itemOptions.add(new ItemOption(5, 20));
        PhuKien.itemOptions.add(new ItemOption(30,1));

        TheTieuDoiTruongVang.itemOptions.add(new ItemOption(73, 1));

        InventoryService.gI().addItemBag(player, ThoiVang);
        InventoryService.gI().addItemBag(player, PhieuGiamGia);
        InventoryService.gI().addItemBag(player, DaBaoVe);
        InventoryService.gI().addItemBag(player, CaiTrang);
        InventoryService.gI().addItemBag(player, PhuKien);
        InventoryService.gI().addItemBag(player, TheTieuDoiTruongVang);
        InventoryService.gI().sendItemBag(player);
    }

    public static void Qua_3(Player player) {
        if (player == null) return;

        if (InventoryService.gI().getCountEmptyBag(player) < 8) {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống");
            return;
        }

        Item ThoiVang = ItemService.gI().createNewItem((short) 457, 20000);
        Item PhieuGiamGia = ItemService.gI().createNewItem((short) 459, 10);
        Item DaBaoVe = ItemService.gI().createNewItem((short) 987, 500);
        Item CaiTrang = ItemService.gI().createNewItem((short) 1941, 1);
        Item PhuKien = ItemService.gI().createNewItem((short) 1912, 1);
        Item Item1453 = ItemService.gI().createNewItem((short) 1453, 30);
        Item ManhVoBongTaiCap3 = ItemService.gI().createNewItem((short) 1855, 5000);

        ThoiVang.itemOptions.add(new ItemOption(100, 1));
        ThoiVang.itemOptions.add(new ItemOption(30, 1));

        PhieuGiamGia.itemOptions.add(new ItemOption(73, 1));

        DaBaoVe.itemOptions.add(new ItemOption(73, 1));

        CaiTrang.itemOptions.add(new ItemOption(50, 40));
        CaiTrang.itemOptions.add(new ItemOption(103, 50));
        CaiTrang.itemOptions.add(new ItemOption(77, 50));
        CaiTrang.itemOptions.add(new ItemOption(14, 25));
        CaiTrang.itemOptions.add(new ItemOption(204, 30));
        CaiTrang.itemOptions.add(new ItemOption(5, 20));
        CaiTrang.itemOptions.add(new ItemOption(30, 1));

        PhuKien.itemOptions.add(new ItemOption(50, 30));
        PhuKien.itemOptions.add(new ItemOption(77, 30));
        PhuKien.itemOptions.add(new ItemOption(103, 30));
        PhuKien.itemOptions.add(new ItemOption(5, 35));
        PhuKien.itemOptions.add(new ItemOption(30, 1));

        ManhVoBongTaiCap3.itemOptions.add(new ItemOption(73, 1));

        InventoryService.gI().addItemBag(player, ThoiVang);
        InventoryService.gI().addItemBag(player, PhieuGiamGia);
        InventoryService.gI().addItemBag(player, DaBaoVe);
        InventoryService.gI().addItemBag(player, CaiTrang);
        InventoryService.gI().addItemBag(player, PhuKien);
        InventoryService.gI().addItemBag(player, Item1453);
        InventoryService.gI().addItemBag(player, ManhVoBongTaiCap3);
        InventoryService.gI().sendItemBag(player);
    }

    public static void Qua_4(Player player) {
        if (player == null) return;

        if (InventoryService.gI().getCountEmptyBag(player) < 7) {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống");
            return;
        }

        Item ThoiVang = ItemService.gI().createNewItem((short) 457, 30000);
        Item DaBaoVe = ItemService.gI().createNewItem((short) 987, 500);
        Item CaiTrang = ItemService.gI().createNewItem((short) 1961, 1);
        Item TheTieuDoiTruongVang = ItemService.gI().createNewItem((short) 956, 20);
        Item TheRongThanNamek = ItemService.gI().createNewItem((short) 1204, 40);
        Item ManhVoBongTai = ItemService.gI().createNewItem((short) 1855, 10000);
        Item VanBayRongThiengVIP = ItemService.gI().createNewItem((short) 1902, 1);

        ThoiVang.itemOptions.add(new ItemOption(100, 1));
        ThoiVang.itemOptions.add(new ItemOption(30, 1));
        
        DaBaoVe.itemOptions.add(new ItemOption(73, 1));

        CaiTrang.itemOptions.add(new ItemOption(50, 45));
        CaiTrang.itemOptions.add(new ItemOption(103, 50));
        CaiTrang.itemOptions.add(new ItemOption(77, 50));
        CaiTrang.itemOptions.add(new ItemOption(14, 30));
        CaiTrang.itemOptions.add(new ItemOption(106, 1));
        CaiTrang.itemOptions.add(new ItemOption(204, 50));
        CaiTrang.itemOptions.add(new ItemOption(5, 20));
        CaiTrang.itemOptions.add(new ItemOption(30, 1));

        TheTieuDoiTruongVang.itemOptions.add(new ItemOption(73, 1));

        TheRongThanNamek.itemOptions.add(new ItemOption(73, 1));

        ManhVoBongTai.itemOptions.add(new ItemOption(73, 1));


        VanBayRongThiengVIP.itemOptions.add(new ItemOption(50, 25));
        VanBayRongThiengVIP.itemOptions.add(new ItemOption(103, 25));
        VanBayRongThiengVIP.itemOptions.add(new ItemOption(77, 25));
        VanBayRongThiengVIP.itemOptions.add(new ItemOption(5, 20));
        VanBayRongThiengVIP.itemOptions.add(new ItemOption(30, 1));
        

        InventoryService.gI().addItemBag(player, ThoiVang);
        InventoryService.gI().addItemBag(player, DaBaoVe);
        InventoryService.gI().addItemBag(player, CaiTrang);
        InventoryService.gI().addItemBag(player, TheTieuDoiTruongVang);
        InventoryService.gI().addItemBag(player, TheRongThanNamek);
        InventoryService.gI().addItemBag(player, ManhVoBongTai);
        InventoryService.gI().addItemBag(player, VanBayRongThiengVIP);

        InventoryService.gI().sendItemBag(player);
    }
}