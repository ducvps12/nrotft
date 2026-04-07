/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import event.EventManager;
import item.Item;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;

/**
 *
 * @author Administrator
 */
public class Noibanh extends Npc {

    public Noibanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        // Menu nấu bánh khi TRUNG_THU BẬT
        if (EventManager.TRUNG_THU) {
            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU); // set index menu
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào, mình là nồi bánh, bạn muốn nấu bánh gì?",
                    "Bánh Trung Thu Gà Quay",
                    "Bánh Trung Thu Gà Quay Hảo Hạng",
                    "Bánh Trung Thu Hạt Sen",
                    "Từ chối");
            return;
        }

        // Menu nấu bánh khi LUNNAR_NEW_YEAR BẬT
        if (EventManager.LUNNAR_NEW_YEAR) {
            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào " + player.name + "\nTôi là nồi nấu bánh\nTôi có thể giúp gì cho bạn?",
                    "Tự nấu bánh", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (this.mapId != 0 && this.mapId != 5 && this.mapId != 7 && this.mapId != 14) {
            return;
        }

        // Menu cơ bản
        if (player.iDMark.isBaseMenu()) {
            if (EventManager.TRUNG_THU) {
                switch (select) {
                    case 0: // Bánh Trung Thu Gà Quay
                        showBanhtrunthuGaQuayMenu(player);
                        break;
                    case 1: // Bánh Trung Thu Gà Quay Hảo Hạng
                        showBanhtrunthuGaQuayHaoHanMenu(player); // Nếu có menu Hảo Hạng
                        break;
                    case 2: // Bánh Trung Thu Hạt Sen
                        showBanhtrunthuHatSenMenu(player); // Nếu có menu Hạt Sen
                        break;
                }
            } else if (EventManager.LUNNAR_NEW_YEAR) {
                switch (select) {
                    // Menu Tết
                    case 0:
                        createOtherMenu(player, 1,
                                "Hãy tìm đủ nguyên liệu và chọn loại bánh muốn nấu",
                                "Nấu Bánh Dầy", "Nấu Bánh Chưng", "Từ chối");
                        break;
                }
            }
            return;
        }

        // Menu nấu bánh dầy/chưng
        if (player.iDMark.getIndexMenu() == 1) {
            switch (select) {
                case 0:
                    showBanhDayMenu(player);
                    break;
                case 1:
                    showBanhChungMenu(player);
                    break;
            }
            return;
        }

        // Menu nấu bánh LUNNAR_NEW_YEAR
        if (EventManager.LUNNAR_NEW_YEAR && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_TET) {
            cookBanhDayEvent(player);
        }
        if (EventManager.LUNNAR_NEW_YEAR && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_CHUNG) {
            cookBanhChungEvent(player);
        }
        if (EventManager.TRUNG_THU && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY) {
            cookBanhTrungthugaquayEvent(player);
        }
        if (EventManager.TRUNG_THU && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY_HAO_HAN) {
            cookBanhTrungthugaquayHaoHanEvent(player);
        }
        if (EventManager.TRUNG_THU && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_TRUNG_THU_HAT_SEN) {
            cookBanhTrungthuHatSenEvent(player);
        }
    }

    // Hiển thị menu nấu bánh trung thu gà quay
    private void showBanhtrunthuGaQuayMenu(Player player) {
        Item botmi = InventoryService.gI().findItemBag(player, 888);
        Item dauxanh = InventoryService.gI().findItemBag(player, 889);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        Item gaquyanguyencon = InventoryService.gI().findItemBag(player, 887);

        if (botmi != null && botmi.quantity >= 99
                && dauxanh != null && dauxanh.quantity >= 5
                && trungvitmuoi != null && trungvitmuoi.quantity >= 2
                && gaquyanguyencon != null && gaquyanguyencon.quantity >= 1
                && player.inventory.gold >= 1_000_000) {

            createOtherMenu(player, ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY,
                    "|2|Bánh trung thu Gà Quay\n"
                    + "|1|Bột mì " + botmi.quantity + "/99\n"
                    + "đậu xanh " + dauxanh.quantity + "/5\n"
                    + "Trứng vịt muối " + trungvitmuoi.quantity + "/2\n"
                    + "Gà Quay nguyên con " + gaquyanguyencon.quantity + "/1\n"
                    + "Giá vàng: 20.000.000",
                    "Đồng ý", "Từ chối");
        } else {
            String NpcSay = "|2|Bánh trung thu Gà Quay\n";
            NpcSay += botmi == null ? "|7|Bột mì 0/99\n" : "|1|Bột mì " + botmi.quantity + "/99\n";
            NpcSay += dauxanh == null ? "|7|Đậu xanh 0/5\n" : "|1|Đậu xanh " + dauxanh.quantity + "/5\n";
            NpcSay += trungvitmuoi == null ? "|7|Trứng vịt muối 0/2\n" : "|1|Trứng vịt muối " + trungvitmuoi.quantity + "/2\n";
            NpcSay += gaquyanguyencon == null ? "|7|Gà Quay nguyên con 0/1\n" : "|1|Gà Quay nguyên con " + gaquyanguyencon.quantity + "/1\n";
            NpcSay += player.inventory.gold < 20_000_000 ? "|7|Còn thiếu vàng" : "|1|Giá vàng: 20.000.000\n";

            createOtherMenu(player, ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY_2, NpcSay, "Từ chối");
        }
    }

    // Hiển thị menu nấu bánh trung thu gà quay
    private void showBanhtrunthuGaQuayHaoHanMenu(Player player) {
        Item botmi = InventoryService.gI().findItemBag(player, 888);
        Item dauxanh = InventoryService.gI().findItemBag(player, 889);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        Item gaquyanguyencon = InventoryService.gI().findItemBag(player, 887);

        if (botmi != null && botmi.quantity >= 99
                && dauxanh != null && dauxanh.quantity >= 5
                && trungvitmuoi != null && trungvitmuoi.quantity >= 2
                && gaquyanguyencon != null && gaquyanguyencon.quantity >= 1
                && player.inventory.ruby >= 1_000) {

            createOtherMenu(player, ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY_HAO_HAN,
                    "|2|Bánh trung thu Gà Quay Hảo Hạng\n"
                    + "30% Cơ hội nhận thêm bánh trung thu thập cẩm\n"
                    + "|1|Bột mì " + botmi.quantity + "/99\n"
                    + "đậu xanh " + dauxanh.quantity + "/5\n"
                    + "Trứng vịt muối " + trungvitmuoi.quantity + "/2\n"
                    + "Gà Quay nguyên con " + gaquyanguyencon.quantity + "/1\n"
                    + "Giá ngọc: 1.000",
                    "Đồng ý", "Từ chối");
        } else {
            String NpcSay = "|2|Bánh trung thu Gà Quay Hảo Hạn\n";
            NpcSay += botmi == null ? "|7|Bột mì 0/99\n" : "|1|Bột mì " + botmi.quantity + "/99\n";
            NpcSay += dauxanh == null ? "|7|Đậu xanh 0/5\n" : "|1|Đậu xanh " + dauxanh.quantity + "/5\n";
            NpcSay += trungvitmuoi == null ? "|7|Trứng vịt muối 0/2\n" : "|1|Trứng vịt muối " + trungvitmuoi.quantity + "/2\n";
            NpcSay += gaquyanguyencon == null ? "|7|Gà Quay nguyên con 0/1\n" : "|1|Gà Quay nguyên con " + gaquyanguyencon.quantity + "/1\n";
            NpcSay += player.inventory.ruby < 1_000 ? "|7|Còn thiếu ngọc" : "|1|Giá ngọc: 1.000\n";

            createOtherMenu(player, ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY_HAO_HAN_2, NpcSay, "Từ chối");
        }
    }

    // Hiển thị menu nấu bánh trung thu hạt sen
    private void showBanhtrunthuHatSenMenu(Player player) {
        Item botmi = InventoryService.gI().findItemBag(player, 888);
        Item dauxanh = InventoryService.gI().findItemBag(player, 889);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        Item hatsen = InventoryService.gI().findItemBag(player, 1312);

        if (botmi != null && botmi.quantity >= 99
                && dauxanh != null && dauxanh.quantity >= 5
                && trungvitmuoi != null && trungvitmuoi.quantity >= 2
                && hatsen != null && hatsen.quantity >= 1
                && player.inventory.ruby >= 1_000) {

            createOtherMenu(player, ConstNpc.MENU_BANH_TRUNG_THU_HAT_SEN,
                    "|2|Bánh trung thu Hạt Sen\n"
                    + "|1|Bột mì " + botmi.quantity + "/99\n"
                    + "đậu xanh " + dauxanh.quantity + "/5\n"
                    + "Trứng vịt muối " + trungvitmuoi.quantity + "/2\n"
                    + "Hạt sen " + hatsen.quantity + "/1\n"
                    + "Giá ngọc: 1.000",
                    "Đồng ý", "Từ chối");
        } else {
            String NpcSay = "|2|Bánh trung thu Hạt Sen\n";
            NpcSay += botmi == null ? "|7|Bột mì 0/99\n" : "|1|Bột mì " + botmi.quantity + "/99\n";
            NpcSay += dauxanh == null ? "|7|Đậu xanh 0/5\n" : "|1|Đậu xanh " + dauxanh.quantity + "/5\n";
            NpcSay += trungvitmuoi == null ? "|7|Trứng vịt muối 0/2\n" : "|1|Trứng vịt muối " + trungvitmuoi.quantity + "/2\n";
            NpcSay += hatsen == null ? "|7|Hạt sen 0/1\n" : "|1|Hạt sen " + hatsen.quantity + "/1\n";
            NpcSay += player.inventory.ruby < 1_000 ? "|7|Còn thiếu ngọc" : "|1|Giá ngọc: 1.000\n";

            createOtherMenu(player, ConstNpc.MENU_BANH_TRUNG_THU_HAT_SEN_2, NpcSay, "Từ chối");
        }
    }

// Hiển thị menu nấu bánh dầy
    private void showBanhDayMenu(Player player) {
        Item comNep = InventoryService.gI().findItemBag(player, 1214);
        Item botGao = InventoryService.gI().findItemBag(player, 1547);
        Item muoiTieu = InventoryService.gI().findItemBag(player, 1545);
        Item chaLua = InventoryService.gI().findItemBag(player, 1544);

        if (comNep != null && comNep.quantity >= 99
                && botGao != null && botGao.quantity >= 5
                && muoiTieu != null && muoiTieu.quantity >= 2
                && chaLua != null && chaLua.quantity >= 1
                && player.inventory.gold >= 1_000_000) {

            createOtherMenu(player, ConstNpc.MENU_BANH_TET,
                    "|2|Bạn muốn nấu bánh dầy?\n"
                    + "|1|Cơm nếp " + comNep.quantity + "/99\n"
                    + "Bột gạo " + botGao.quantity + "/5\n"
                    + "Muối tiêu " + muoiTieu.quantity + "/2\n"
                    + "Chả lụa " + chaLua.quantity + "/1\n"
                    + "Giá vàng: 1.000.000",
                    "Đồng ý", "Từ chối");
        } else {
            String NpcSay = "|2|Bạn muốn nấu bánh dầy\n";
            NpcSay += comNep == null ? "|7|Cơm nếp 0/99\n" : "|1|Cơm nếp " + comNep.quantity + "/99\n";
            NpcSay += botGao == null ? "|7|Bột gạo 0/5\n" : "|1|Bột gạo " + botGao.quantity + "/5\n";
            NpcSay += muoiTieu == null ? "|7|Muối tiêu 0/2\n" : "|1|Muối tiêu " + muoiTieu.quantity + "/2\n";
            NpcSay += chaLua == null ? "|7|Chả lụa 0/1\n" : "|1|Chả lụa " + chaLua.quantity + "/1\n";
            NpcSay += player.inventory.gold < 1_000_000 ? "|7|Còn thiếu vàng" : "|1|Giá vàng: 1.000.000\n";

            createOtherMenu(player, ConstNpc.MENU_BANH_TET_2, NpcSay, "Từ chối");
        }
    }

// Hiển thị menu nấu bánh chưng
    private void showBanhChungMenu(Player player) {
        Item comNep = InventoryService.gI().findItemBag(player, 1214);
        Item dauXanh = InventoryService.gI().findItemBag(player, 1548);
        Item thitTuoi = InventoryService.gI().findItemBag(player, 1549);

        if (comNep != null && comNep.quantity >= 99
                && dauXanh != null && dauXanh.quantity >= 2
                && thitTuoi != null && thitTuoi.quantity >= 2
                && player.inventory.gold >= 5_000_000) {

            createOtherMenu(player, ConstNpc.MENU_BANH_CHUNG,
                    "|2|Bạn muốn nấu bánh chưng?\n"
                    + "|1|Cơm nếp " + comNep.quantity + "/99\n"
                    + "Đậu xanh " + dauXanh.quantity + "/2\n"
                    + "Thịt tươi " + thitTuoi.quantity + "/2\n"
                    + "Giá vàng: 5.000.000",
                    "Đồng ý", "Từ chối");
        } else {
            String NpcSay = "|2|Bạn muốn nấu bánh chưng\n";
            NpcSay += comNep == null ? "|7|Cơm nếp 0/99\n" : "|1|Cơm nếp " + comNep.quantity + "/99\n";
            NpcSay += dauXanh == null ? "|7|Đậu xanh 0/2\n" : "|1|Đậu xanh " + dauXanh.quantity + "/2\n";
            NpcSay += thitTuoi == null ? "|7|Thịt tươi 0/2\n" : "|1|Thịt tươi " + thitTuoi.quantity + "/2\n";
            NpcSay += player.inventory.gold < 5_000_000 ? "|7|Còn thiếu vàng" : "|1|Giá vàng: 5.000.000\n";

            createOtherMenu(player, ConstNpc.MENU_BANH_CHUNG_2, NpcSay, "Từ chối");
        }
    }

    private void cookBanhTrungthugaquayEvent(Player player) {
        if (player.isCookingBanhTrungThuGaQuay || player.isCookingBanhTrungThuGaQuayHaoHan || player.isCookingBanhTrungThuHatSen) {
            this.npcChat(player, "Bạn đang nấu bánh Trung Thu Gà Quay rồi mà!");
            return;
        }

        Item botmi = InventoryService.gI().findItemBag(player, 888);
        Item dauxanh = InventoryService.gI().findItemBag(player, 889);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        Item gauqyanguyencon = InventoryService.gI().findItemBag(player, 887);
        int vang = 20_000_000;

        if (botmi == null || botmi.quantity < 99
                || dauxanh == null || dauxanh.quantity < 5
                || trungvitmuoi == null || trungvitmuoi.quantity < 2
                || gauqyanguyencon == null || gauqyanguyencon.quantity < 1
                || player.inventory.gold < vang) {
            this.npcChat(player, "Bạn không đủ nguyên liệu hoặc vàng để nấu Trung Thu Gà Quay!");
            return;
        }

        Item banhtrungthugaquay = ItemService.gI().createNewItem((short) 890);
        player.isCookingBanhTrungThuGaQuay = true;

        this.npcChat(player, "Bắt đầu nấu Trung Thu Gà Quay...\n|7|Vui lòng chờ trong giây lát!");

        Thread.startVirtualThread(() -> {
            try {
                int timeWait = 60;
                while (timeWait > 0) {
                    this.npcChat(player, "Đang nấu Trung Thu Gà Quay\n|7|Thời gian còn lại: " + timeWait + " giây.");
                    java.util.concurrent.TimeUnit.SECONDS.sleep(1);
                    timeWait--;
                }

                InventoryService.gI().subQuantityItemsBag(player, botmi, 99);
                InventoryService.gI().subQuantityItemsBag(player, dauxanh, 5);
                InventoryService.gI().subQuantityItemsBag(player, trungvitmuoi, 2);
                InventoryService.gI().subQuantityItemsBag(player, gauqyanguyencon, 1);

                player.inventory.gold -= vang;
                Service.gI().sendMoney(player);

                InventoryService.gI().addItemBag(player, banhtrungthugaquay);
                InventoryService.gI().sendItemBag(player);

                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Đã nấu xong Trung Thu Gà Quay!\n|7|Bạn đã nhận được " + banhtrungthugaquay.template.name,
                        "Nhận Ngay");
            } catch (InterruptedException ignored) {
            } finally {
                player.isCookingBanhTrungThuGaQuay = false;
            }
        });
    }

    private void cookBanhTrungthugaquayHaoHanEvent(Player player) {
        if (player.isCookingBanhTrungThuGaQuay || player.isCookingBanhTrungThuGaQuayHaoHan || player.isCookingBanhTrungThuHatSen) {
            this.npcChat(player, "Bạn đang nấu bánh Trung Thu Gà Quay Hảo Hạn rồi mà!");
            return;
        }

        Item botmi = InventoryService.gI().findItemBag(player, 888);
        Item dauxanh = InventoryService.gI().findItemBag(player, 889);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        Item gauqyanguyencon = InventoryService.gI().findItemBag(player, 887);
        int ngoc = 1_000;

        if (botmi == null || botmi.quantity < 99
                || dauxanh == null || dauxanh.quantity < 5
                || trungvitmuoi == null || trungvitmuoi.quantity < 2
                || gauqyanguyencon == null || gauqyanguyencon.quantity < 1
                || player.inventory.ruby < ngoc) {
            this.npcChat(player, "Bạn không đủ nguyên liệu hoặc ngọc để nấu Trung Thu Gà Quay Hảo Hạn!");
            return;
        }

        Item banhtrungthugaquay = ItemService.gI().createNewItem((short) 890);
        player.isCookingBanhTrungThuGaQuayHaoHan = true;

        this.npcChat(player, "Bắt đầu nấu Trung Thu Gà Quay Hảo Hạn...\n|7|Vui lòng chờ trong giây lát!");

        Thread.startVirtualThread(() -> {
            try {
                int timeWait = 60;
                while (timeWait > 0) {
                    this.npcChat(player, "Đang nấu Trung Thu Gà Quay Hảo Hạn\n|7|Thời gian còn lại: " + timeWait + " giây.");
                    java.util.concurrent.TimeUnit.SECONDS.sleep(1);
                    timeWait--;
                }

                // Trừ nguyên liệu và ngọc
                InventoryService.gI().subQuantityItemsBag(player, botmi, 99);
                InventoryService.gI().subQuantityItemsBag(player, dauxanh, 5);
                InventoryService.gI().subQuantityItemsBag(player, trungvitmuoi, 2);
                InventoryService.gI().subQuantityItemsBag(player, gauqyanguyencon, 1);
                player.inventory.ruby -= ngoc;
                Service.gI().sendMoney(player);

                // Thêm bánh chính
                InventoryService.gI().addItemBag(player, banhtrungthugaquay);

                // 30% cơ hội nhận thêm bánh Trung Thu Thập Cẩm (ID 891)
                if (Math.random() <= 0.3) {
                    Item banhThapCam = ItemService.gI().createNewItem((short) 891);
                    InventoryService.gI().addItemBag(player, banhThapCam);
                    this.npcChat(player, "Bạn may mắn nhận thêm 1 bánh Trung Thu Thập Cẩm!");
                }

                InventoryService.gI().sendItemBag(player);

                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Đã nấu xong Trung Thu Gà Quay Hảo Hạn!\n|7|Bạn đã nhận được " + banhtrungthugaquay.template.name,
                        "Nhận Ngay");
            } catch (InterruptedException ignored) {
            } finally {
                player.isCookingBanhTrungThuGaQuayHaoHan = false;
            }
        });
    }

    private void cookBanhTrungthuHatSenEvent(Player player) {
        if (player.isCookingBanhTrungThuGaQuay || player.isCookingBanhTrungThuGaQuayHaoHan || player.isCookingBanhTrungThuHatSen) {
            this.npcChat(player, "Bạn đang nấu bánh Trung Thu Hạt Sen rồi mà!");
            return;
        }

        Item botmi = InventoryService.gI().findItemBag(player, 888);
        Item dauxanh = InventoryService.gI().findItemBag(player, 889);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        Item hatsen = InventoryService.gI().findItemBag(player, 1312);
        int ngoc = 1_000;

        if (botmi == null || botmi.quantity < 99
                || dauxanh == null || dauxanh.quantity < 5
                || trungvitmuoi == null || trungvitmuoi.quantity < 2
                || hatsen == null || hatsen.quantity < 1
                || player.inventory.ruby < ngoc) {
            this.npcChat(player, "Bạn không đủ nguyên liệu hoặc vàng để nấu Trung Thu Hạt Sen!");
            return;
        }

        Item banhtrungthugaquay = ItemService.gI().createNewItem((short) 1313);
        player.isCookingBanhTrungThuHatSen = true;

        this.npcChat(player, "Bắt đầu nấu Trung Thu Hạt Sen...\n|7|Vui lòng chờ trong giây lát!");

        Thread.startVirtualThread(() -> {
            try {
                int timeWait = 60;
                while (timeWait > 0) {
                    this.npcChat(player, "Đang nấu Trung Thu Hạt Sen\n|7|Thời gian còn lại: " + timeWait + " giây.");
                    java.util.concurrent.TimeUnit.SECONDS.sleep(1);
                    timeWait--;
                }

                InventoryService.gI().subQuantityItemsBag(player, botmi, 99);
                InventoryService.gI().subQuantityItemsBag(player, dauxanh, 5);
                InventoryService.gI().subQuantityItemsBag(player, trungvitmuoi, 2);
                InventoryService.gI().subQuantityItemsBag(player, hatsen, 1);

                player.inventory.ruby -= ngoc;
                Service.gI().sendMoney(player);

                InventoryService.gI().addItemBag(player, banhtrungthugaquay);
                InventoryService.gI().sendItemBag(player);

                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Đã nấu xong Trung Thu Hạt Sen!\n|7|Bạn đã nhận được " + banhtrungthugaquay.template.name,
                        "Nhận Ngay");
            } catch (InterruptedException ignored) {
            } finally {
                player.isCookingBanhTrungThuHatSen = false;
            }
        });
    }

// Nấu bánh dầy (Trung Thu / Tết / bình thường)
    private void cookBanhDayEvent(Player player) {
        if (player.isCookingBanhDay || player.isCookingBanhChung) {
            this.npcChat(player, "Bạn đang nấu bánh khác rồi mà!");
            return;
        }

        Item comNep = InventoryService.gI().findItemBag(player, 1214);
        Item botGao = InventoryService.gI().findItemBag(player, 1547);
        Item muoiTieu = InventoryService.gI().findItemBag(player, 1545);
        Item chaLua = InventoryService.gI().findItemBag(player, 1544);
        int vang = 1_000_000;

        if (comNep == null || comNep.quantity < 99
                || botGao == null || botGao.quantity < 5
                || muoiTieu == null || muoiTieu.quantity < 2
                || chaLua == null || chaLua.quantity < 1
                || player.inventory.gold < vang) {
            this.npcChat(player, "Bạn không đủ nguyên liệu hoặc vàng để nấu bánh dầy!");
            return;
        }

        Item banhDay = ItemService.gI().createNewItem((short) 1542);
        player.isCookingBanhDay = true;

        this.npcChat(player, "Bắt đầu nấu bánh dầy...\n|7|Vui lòng chờ trong giây lát!");

        Thread.startVirtualThread(() -> {
            try {
                int timeWait = 60;
                while (timeWait > 0) {
                    this.npcChat(player, "Đang nấu bánh dầy\n|7|Thời gian còn lại: " + timeWait + " giây.");
                    java.util.concurrent.TimeUnit.SECONDS.sleep(1);
                    timeWait--;
                }

                InventoryService.gI().subQuantityItemsBag(player, comNep, 99);
                InventoryService.gI().subQuantityItemsBag(player, botGao, 5);
                InventoryService.gI().subQuantityItemsBag(player, muoiTieu, 2);
                InventoryService.gI().subQuantityItemsBag(player, chaLua, 1);

                player.inventory.gold -= vang;
                Service.gI().sendMoney(player);

                InventoryService.gI().addItemBag(player, banhDay);
                InventoryService.gI().sendItemBag(player);

                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Đã nấu xong bánh dầy!\n|7|Bạn đã nhận được " + banhDay.template.name,
                        "Nhận Ngay");
            } catch (InterruptedException ignored) {
            } finally {
                player.isCookingBanhDay = false;
            }
        });
    }

// Nấu bánh chưng
    private void cookBanhChungEvent(Player player) {
        if (player.isCookingBanhChung || player.isCookingBanhDay) {
            this.npcChat(player, "Bạn đang nấu bánh khác rồi mà!");
            return;
        }

        Item comNep = InventoryService.gI().findItemBag(player, 1214);
        Item dauXanh = InventoryService.gI().findItemBag(player, 1548);
        Item thitTuoi = InventoryService.gI().findItemBag(player, 1549);
        Item banhChung = ItemService.gI().createNewItem((short) 1556);
        int vang = 5_000_000;

        if (comNep == null || comNep.quantity < 99
                || dauXanh == null || dauXanh.quantity < 2
                || thitTuoi == null || thitTuoi.quantity < 2
                || player.inventory.gold < vang) {
            this.npcChat(player, "Bạn không đủ nguyên liệu hoặc vàng để nấu bánh chưng!");
            return;
        }

        player.isCookingBanhChung = true;
        this.npcChat(player, "Bắt đầu nấu bánh chưng...\n|7|Vui lòng chờ trong giây lát!");

        Thread.startVirtualThread(() -> {
            try {
                int timeWait = 60;
                while (timeWait > 0) {
                    this.npcChat(player, "Đang nấu bánh chưng\n|7|Thời gian còn lại: " + timeWait + " giây.");
                    java.util.concurrent.TimeUnit.SECONDS.sleep(1);
                    timeWait--;
                }

                InventoryService.gI().subQuantityItemsBag(player, comNep, 99);
                InventoryService.gI().subQuantityItemsBag(player, dauXanh, 2);
                InventoryService.gI().subQuantityItemsBag(player, thitTuoi, 2);

                player.inventory.gold -= vang;
                Service.gI().sendMoney(player);

                InventoryService.gI().addItemBag(player, banhChung);
                InventoryService.gI().sendItemBag(player);

                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Đã nấu xong bánh chưng!\n|7|Bạn đã nhận được " + banhChung.template.name,
                        "Nhận Ngay");
            } catch (InterruptedException ignored) {
            } finally {
                player.isCookingBanhChung = false;
            }
        });
    }
}
