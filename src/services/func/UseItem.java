package services.func;

/*
 *
 *
 *  Box ZALO:
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import boss.Boss;
import boss.BossID;
import boss.BossManager;
import boss.boss_manifest.Mini.SoiHecQuyn;
import boss.boss_manifest.Mini.Xinbato;
import nro.services.RewardService;
import nro.services.Service;
import nro.services.TaskService;
import nro.services.InventoryService;
import nro.services.MapService;
import nro.services.ItemService;
import nro.services.SkillService;
import nro.services.ItemTimeService;
import nro.services.PlayerService;
import nro.services.PetService;
import nro.services.NgocRongNamecService;
import nro.services.NpcService;
import consts.ConstItem;
import models.Combine.CombineService;
import models.ShenronEvent.ShenronEventService;
import models.Card.Card;
import models.Card.RadarService;
import models.Card.RadarCard;
import consts.ConstMap;
import item.Item;
import consts.ConstNpc;
import consts.ConstPlayer;
import consts.ConstTaskBadges;
import data.RandomCollection;
import item.Item.ItemOption;
import item.ItemTime;
import map.Zone;
import nro.player.Inventory;
import nro.player.Player;
import skill.Skill;
import network.Message;
import utils.SkillUtil;
import utils.TimeUtil;
import utils.Util;
import nro.server.io.MySession;
import utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import jdbc.daos.PlayerDAO;
import map.ItemMap;
import nro.services.ItemMapService;
import task.Badges.BadgesTaskService;

public class UseItem {

    private static final int ITEM_BOX_TO_BODY_OR_BAG = 0;
    private static final int ITEM_BAG_TO_BOX = 1;
    private static final int ITEM_BODY_TO_BOX = 3;
    private static final int ITEM_BAG_TO_BODY = 4;
    private static final int ITEM_BODY_TO_BAG = 5;
    private static final int ITEM_BAG_TO_PET_BODY = 6;
    private static final int ITEM_BODY_PET_TO_BAG = 7;

    private static final byte DO_USE_ITEM = 0;
    private static final byte DO_THROW_ITEM = 1;
    private static final byte ACCEPT_THROW_ITEM = 2;
    private static final byte ACCEPT_USE_ITEM = 3;

    private static final int PEA_BONUS_DAILY_LIMIT = 80;
    private static final int PEA_BONUS_SD_LIMIT = 2000;
    private static final int PEA_BONUS_HP_LIMIT = 20000;
    private static final int PEA_BONUS_KI_LIMIT = 20000;

    private static UseItem instance;

    private int randClothes(int level) {
        return ConstItem.LIST_ITEM_CLOTHES[Util.nextInt(0, 2)][Util.nextInt(0, 4)][level - 1];
    }

    private UseItem() {

    }

    public static UseItem gI() {
        if (instance == null) {
            instance = new UseItem();
        }
        return instance;
    }

    public void getItem(MySession session, Message msg) {
        Player player = session.player;
        if (player == null) {
            return;
        }
        TransactionService.gI().cancelTrade(player);
        try {
            int type = msg.reader().readByte();
            int index = msg.reader().readByte();
            if (index == -1) {
                return;
            }
            switch (type) {
                case ITEM_BOX_TO_BODY_OR_BAG:
                    InventoryService.gI().itemBoxToBodyOrBag(player, index);
                    TaskService.gI().checkDoneTaskGetItemBox(player);
                    break;
                case ITEM_BAG_TO_BOX:
                    InventoryService.gI().itemBagToBox(player, index);
                    break;
                case ITEM_BODY_TO_BOX:
                    InventoryService.gI().itemBodyToBox(player, index);
                    break;
                case ITEM_BAG_TO_BODY:
                    InventoryService.gI().itemBagToBody(player, index);
                    break;
                case ITEM_BODY_TO_BAG:
                    InventoryService.gI().itemBodyToBag(player, index);
                    break;
                case ITEM_BAG_TO_PET_BODY:
                    InventoryService.gI().itemBagToPetBody(player, index);
                    break;
                case ITEM_BODY_PET_TO_BAG:
                    InventoryService.gI().itemPetBodyToBag(player, index);
                    break;
            }
            if (player.setClothes != null) {
                player.setClothes.setup();
            }
            if (player.pet != null) {
                player.pet.setClothes.setup();
            }
            player.setClanMember();
            Service.gI().sendFlagBag(player);
            Service.gI().point(player);
            Service.gI().sendSpeedPlayer(player, -1);
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);

        }
    }

    public Item finditem(Player player, int iditem) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == iditem) {
                return item;
            }
        }
        return null;
    }

    public void doItem(Player player, Message _msg) {
        TransactionService.gI().cancelTrade(player);
        Message msg = null;
        byte type;
        try {
            type = _msg.reader().readByte();
            int where = _msg.reader().readByte();
            int index = _msg.reader().readByte();
            switch (type) {
                case DO_USE_ITEM:
                    if (player != null && player.inventory != null) {
                        if (index != -1) {
                            if (index < 0) {
                                return;
                            }
                            Item item = player.inventory.itemsBag.get(index);
                            if (item.isNotNullItem()) {
                                if (item.template.type == 7) {
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc chắn học "
                                            + player.inventory.itemsBag.get(index).template.name + "?");
                                    player.sendMessage(msg);
                                } else if (item.template.id == 570) {
                                    if (!Util.isAfterMidnight(player.lastTimeRewardWoodChest)) {
                                        Service.gI().sendThongBao(player, "Hãy chờ đến ngày mai");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn mở\n"
                                            + player.inventory.itemsBag.get(index).template.name + " ?");
                                    player.sendMessage(msg);
                                } else if (item.template.type == 22) {
                                    if (player.zone.items.stream()
                                            .filter(it -> it != null && it.itemTemplate.type == 22).count() > 2) {
                                        Service.gI().sendThongBaoOK(player, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn dùng\n"
                                            + player.inventory.itemsBag.get(index).template.name + " ?");
                                    player.sendMessage(msg);
                                } else {
                                    UseItem.gI().useItem(player, item, index);
                                }
                            }
                        } else {
                            int iditem = _msg.reader().readShort();
                            Item item = finditem(player, iditem);
                            UseItem.gI().useItem(player, item, index);
                        }
                    }
                    break;
                case DO_THROW_ITEM:
                    if (!(player.zone.map.mapId == 21 || player.zone.map.mapId == 22 || player.zone.map.mapId == 23)) {
                        Item item = null;
                        if (index < 0) {
                            return;
                        }
                        if (where == 0) {
                            item = player.inventory.itemsBody.get(index);
                        } else {
                            item = player.inventory.itemsBag.get(index);
                        }

                        if (item.isNotNullItem() && item.template.id == 570) {
                            Service.gI().sendThongBao(player, "Không thể bỏ vật phẩm này.");
                            return;
                        }
                        if (!item.isNotNullItem()) {
                            return;
                        }
                        msg = new Message(-43);
                        msg.writer().writeByte(type);
                        msg.writer().writeByte(where);
                        msg.writer().writeByte(index);
                        msg.writer().writeUTF("Bạn chắc chắn muốn vứt " + item.template.name + "?");
                        player.sendMessage(msg);
                    } else {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                    }
                    break;
                case ACCEPT_THROW_ITEM:
                    InventoryService.gI().throwItem(player, where, index);
                    Service.gI().point(player);
                    InventoryService.gI().sendItemBag(player);
                    break;
                case ACCEPT_USE_ITEM:
                    UseItem.gI().useItem(player, player.inventory.itemsBag.get(index), index);
                    break;
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void useItem(Player pl, Item item, int indexBag) throws Exception {
        if (item != null && item.isNotNullItem()) {
            if (item.template.id == 570) {
                int time = (int) TimeUtil.diffDate(new Date(), new Date(item.createTime), TimeUtil.DAY);
                if (time == 0) {
                    Service.gI().sendThongBao(pl, "Hãy chờ đến ngày mai");
                } else {
                    openRuongGo(pl);
                }
                return;
            }
            if (item.template.strRequire <= pl.nPoint.power) {
                switch (item.template.type) {
                    case 21: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        PetService.Pet2(pl, pl.getHeadThuCung(), pl.getBodyThuCung(), pl.getLegThuCung());
                        // Service.gI().sendEffPlayer(pl);
                        Service.gI().point(pl);
                        break;
                    }
                    case 33: {// card
                        UseCard(pl, item);
                        break;
                    }
                    case 7: {// sách học, nâng skill
                        learnSkill(pl, item);
                        // HocSkill(pl, item);
                        break;
                    }
                    case 6: {// đậu thần
                        this.eatPea(pl);
                        break;
                    }
                    case 12: {// ngọc rồng các loại
                        controllerCallRongThan(pl, item);
                        break;
                    }
                    case 23: // thú cưỡi mới
                    case 24: // thú cưỡi cũ
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        break;
                    case 11: {// item bag
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendFlagBag(pl);
                        break;
                    }
                    case 36: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendEffPlayer(pl);
                        break;
                    }
                    case 72: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendPetFollow(pl, (short) (item.template.iconID - 1));
                        break;
                    }
                    case 98: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendEffPlayer(pl);
                        break;
                    }
                    case 99: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendEffPlayer(pl);
                        break;
                    }
                    default:
                        switch (item.template.id) {
                            case 992: // Nhan thoi khong
                                pl.type = 2;
                                pl.maxTime = 5;
                                Service.gI().Transport(pl);
                                break;
                            case 361:
                                pl.idGo = (short) Util.nextInt(0, 6);
                                NgocRongNamecService.gI().menuCheckTeleNamekBall(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBag(pl);
                                break;
                            case 1187: // Đổi đệ thành Jiren
                                if (pl.pet == null) {
                                    Service.gI().sendThongBao(pl, "Bạn chưa có đệ tử");
                                    return;
                                }

                                // ⭐ đổi đệ
                                PetService.gI().changeBlackGokuPet(pl);

                                // ⭐ trừ item sau khi đổi
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBag(pl);

                                // ⭐ thông báo
                                Service.gI().sendThongBao(pl, "Đệ tử của bạn đã tiến hóa thành Jiren!");
                                break;
                            case 211: // nho tím
                            case 212: // nho xanh
                                eatGrapes(pl, item);
                                break;
                            case 342:
                            case 343:
                            case 344:
                            case 345:
                                if (pl.zone.items.stream().filter(it -> it != null && it.itemTemplate.type == 22)
                                        .count() < 3) {
                                    Service.gI().dropSatellite(pl, item, pl.zone, pl.location.x, pl.location.y);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                } else {
                                    Service.gI().sendThongBaoOK(pl, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                }
                                break;
                            case 380: // cskb
                                openCSKB(pl, item);
                                break;
                            case 381: // cuồng nộ
                            case 382: // bổ huyết
                            case 383: // bổ khí
                            case 384: // giáp xên
                            case 385: // ẩn danh
                            case 379: // máy dò capsule
                            case 638: // commeson
                            case 2075: // rocket
                            case 2160: // Nồi cơm điện
                            case 579:
                            case 1045: // đuôi khỉ
                            case 663: // bánh pudding
                            case 664: // xúc xíc
                            case 665: // kem dâu
                            case 666: // mì ly
                            case 667: // sushi
                            case 1150:
                            case 1151:
                            case 1152:
                            case 1153:
                            case 1154:
                            case 1978:
                            case 1979:
                            case 1980:
                            case 465:
                            case 466:
                            case 472:
                            case 473:
                            case 1628:
                            case 764:
                            case 1731:
                            case 1727:
                            case 1728:
                            case 1729:
                            case 1730:
                            case 1635:
                                useItemTime(pl, item);
                                break;
                            case 1809:
                                ChangeMapService.gI().changeMap(pl, 186, -1, 100, 84);
                                break;
                            case 1540:
                                ChangeMapService.gI().changeMap(pl, 194, -1, 100, 84);
                                break;
                            case 1560:
                                if (InventoryService.gI().findItem(pl.inventory.itemsBag, 1561) != null) {
                                    UseItem.gI().openRuongNgocRong(pl, item);
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn không có chía khoá vàng!");
                                }
                                break;
                            case 460:
                                CucXuong(pl, item);
                                break;
                            case 456:
                                BinhNuoc(pl, item);
                                break;
                            case 1787:
                                Item trungRong = InventoryService.gI().findItemBag(pl, 1787);
                                if (trungRong != null && trungRong.quantity >= 99) {
                                    open1787(pl, item);
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn cần x99 Mảnh Trứng");
                                }
                                break;
                            case 1786:
                                open1786(pl, item);
                                break;
                            case 1788:
                                open1788(pl, item);
                                break;
                            case 1798:
                                open1798(pl, item);
                                break;
                            // case 1576:
                            // ItemService.gI().OpenItem1576(pl, item);
                            // break;
                            case 1805:
                                ItemService.gI().OpenItem1805(pl, item);
                                break;
                            case 962, 963:
                                ItemService.gI().OpenCapsuleCaiTrang(pl, item);
                                break;
                            case 627:
                                open627(pl, item);
                                break;
                            // case 1806:
                            // UseItem.gI().ItemSKH(pl, item);
                            // break;
                            case 1807:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 1808:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 880:
                            case 881:
                            case 882:
                                if (pl.itemTime.isEatMeal2) {
                                    Service.gI().sendThongBao(pl, "Chỉ được sử dụng 1 cái");
                                    break;
                                }
                                useItemTime(pl, item);
                                break;
                            case 899:
                            case 900:
                            case 902:
                            case 903:
                                if (pl.itemTime.isEatMeal3) {
                                    Service.gI().sendThongBao(pl, "Chỉ được sử dụng 1 cái");
                                    break;
                                }
                                useItemTime(pl, item);
                                break;
                            case 521: // tdlt
                                useTDLT(pl, item);
                                break;
                            case 454: // bông tai
                                usePorata(pl);
                                break;
                            case 921:
                                usePorata2(pl, item);
                                break;
                            case 1810:
                                usePorata3(pl, item);
                                break;
                            case 2034:
                                usePorata4(pl, item);
                                break;
                            case 193: // gói 10 viên capsule
                                openCapsuleUI(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            case 194: // capsule đặc biệt
                                openCapsuleUI(pl);
                                break;
                            case 401: // đổi đệ tử
                                changePet(pl, item);
                                break;
                            case 402: // sách nâng chiêu 1 đệ tử
                            case 403: // sách nâng chiêu 2 đệ tử
                            case 404: // sách nâng chiêu 3 đệ tử
                            case 759: // sách nâng chiêu 4 đệ tử
                                upSkillPet(pl, item);
                                break;
                            case 1264:
                                BossManager.gI().showListBoss(pl);
                                break;
                            case 726:
                                UseItem.gI().ItemManhGiay(pl, item);
                                break;
                            case 727:
                            case 728:
                                UseItem.gI().ItemSieuThanThuy(pl, item);
                                break;
                            case 648:
                                ItemService.gI().OpenItem648(pl, item);
                                break;
                            case 736:
                                ItemService.gI().OpenItem736(pl, item);
                                break;
                            case 987:
                                Service.gI().sendThongBao(pl, "Bảo vệ trang bị không bị rớt cấp"); // đá bảo vệ
                                break;
                            case 988: { // Túi nâng giới hạn vàng - thêm 1 tỷ vàng
                                long goldToAdd = 1_000_000_000L;
                                pl.inventory.gold += goldToAdd;
                                if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
                                    pl.inventory.gold = Inventory.LIMIT_GOLD;
                                }
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBag(pl);
                                Service.gI().sendMoney(pl);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được 1 Tỷ vàng! Tổng vàng hiện tại: " + Util.numberToMoney(pl.inventory.gold));
                                break;
                            }
                            case 1955:
                                Input.gI().createFormChangeNameByItem(pl);
                                break;
                            case 1623:
                                TaskService.gI().sendNextTaskMain(pl);
                                break;
                            case 1228:
                                NpcService.gI().createMenuConMeo(pl, ConstNpc.HOP_QUA_THAN_LINH, -1,
                                        "Chọn hành tinh của đồ thần linh muốn nhận.",
                                        "Trái đất", "Namek", "Xayda");
                                break;
                            case 1626: {
                                int[] listItem = { 856, 943, 942 };
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                Item phuKien = ItemService.gI().createNewItem((short) listItem[Util.nextInt(2)]);
                                if (phuKien.template.id == 856) {
                                    phuKien.itemOptions.add(new Item.ItemOption(50, 10));
                                    phuKien.itemOptions.add(new Item.ItemOption(77, 10));
                                    phuKien.itemOptions.add(new Item.ItemOption(103, 10));
                                } else if (phuKien.template.id == 943) {
                                    phuKien.itemOptions.add(new Item.ItemOption(50, 10));
                                } else if (phuKien.template.id == 942) {
                                    phuKien.itemOptions.add(new Item.ItemOption(77, 10));
                                    phuKien.itemOptions.add(new Item.ItemOption(103, 10));
                                }
                                if (Util.isTrue(95, 100)) {
                                    phuKien.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 5)));
                                }
                                InventoryService.gI().addItemBag(pl, phuKien);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBag(pl);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + phuKien.template.name);
                            }
                                break;
                            case 1629: {
                                Player player = pl;
                                if (player.pet != null) {
                                    if (player.pet.playerSkill.skills.get(2).skillId != -1) {
                                        player.pet.openSkill3();
                                    } else {
                                        Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 3 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                            }
                                break;
                            case 1630: {
                                Player player = pl;
                                if (player.pet != null) {
                                    if (player.pet.playerSkill.skills.get(3).skillId != -1) {
                                        player.pet.openSkill4();
                                    } else {
                                        Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 4 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                            }
                                break;
                            case 628: {
                                int ct = Util.nextInt(618, 626);
                                Item caiTrangHaiTac = ItemService.gI().createNewItem((short) ct);
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(93, 30));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(50, 15));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(77, 15));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(103, 15));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(149, 1));
                                InventoryService.gI().addItemBag(pl, caiTrangHaiTac);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl,
                                        "Bạn đã nhận được cải trang " + caiTrangHaiTac.template.name);
                            }
                                break;
                            case 1440: {
                                int ct = Util.nextInt(441, 447);
                                Item caiTrangHaiTac = ItemService.gI().createNewItem((short) ct);
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(93, 30));
                                InventoryService.gI().addItemBag(pl, caiTrangHaiTac);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + caiTrangHaiTac.template.name);
                            }
                                break;
                            case 1453: {
                                int[] listCT = { 1416, 1417, 1419, 1422 };
                                int ct = listCT[Util.nextInt(0, listCT.length)];
                                Item caiTrangHaiTac = ItemService.gI().createNewItem((short) ct);
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(93, 30));
                                InventoryService.gI().addItemBag(pl, caiTrangHaiTac);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + caiTrangHaiTac.template.name);
                            }
                                break;
                            case 1536:
                                hopQuaKichHoat(pl, item);

                                break;
                            case 1592:
                                UseItem.gI().Gokudayvip(pl, item);
                                break;
                            case 1377:
                                thiepchucvip(pl, item);
                                break;
                            case 1957:
                                hop2010(pl, item);
                                break;
                            case 1964:
                                CapsuleTrangSucVIP(pl, item);
                                break;
                            case 1758:
                                UseItem.gI().Cadicvip(pl, item);
                                break;
                            case 1898:
                                UseItem.gI().RuongRongThan(pl, item);
                                break;
                            case 1703:// set tl kh
                                UseItem.gI().Hopdothanlinh(pl, item);
                                break;
                            case 1806:
                                UseItem.gI().Hopdovaitho(pl, item);
                                break;
                            case 1704:// set hd kh
                                UseItem.gI().Hopdohuydiet(pl, item);
                                break;
                            case 1938:
                                hopQuaTanThu(pl, item);
                                break;
                            case 718:
                                if (!pl.getSession().actived) {
                                    Service.gI().sendThongBao(pl, "Vui lòng kích hoạt tài khoản để có thể sử dụng");
                                    return;
                                }
                                Input.gI().createFormTangRuby(pl);
                                break;
                            case 1939:
                                ItemService.gI().setTiemNang(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                break;
                            case 1570:
                                opkhoabac(pl, item);
                                break;
                            case 1561:
                                opkhoavang(pl, item);
                                break;
                            case 1982:
                                openfa(pl, item);
                                break;
                            case 554:
                                dotPhao(pl, item);
                                break;
                            case 1314:
                                banhtrungthucaocap(pl, item);
                                break;
                            case 1981:
                                openthonglong(pl, item);
                                break;
                            case 1315:
                                opHopBanhNho(pl, item);
                                break;
                            case 1316:
                                banhtrungthuchay(pl, item);
                                break;
                            case 1317:
                                banhtrungthu(pl, item);
                                break;
                            case 1822: // đổi đệ tử
                                changePetRamdom(pl, item);
                                break;
                            case 2030:
                                addDamageByItem(pl, item);
                                break;
                            case 1952: // rada ngọc rồng
                                UseItem.gI().RadaNgocRong(pl, item.template.id);
                                break;
                            case 1947:
                            case 1948:
                            case 1949:
                            case 1950:
                            case 1951:
                                ThucAnChoThan(pl, item);
                                break;
                            case 1655:
                                hopQuaKichHoat(pl, item);
                                break;
                            case 1954:
                                OpenHopThanlinh(pl, item.template.id);
                                break;
                            case 1575: // pháo bômg
                                UseItem.gI().PhaoBong(pl, item);
                                break;
                            case 1576: // pháo bômg
                                UseItem.gI().PhaoBongVip(pl, item);
                                break;
                        }
                        break;
                }
                TaskService.gI().checkDoneTaskUseItem(pl, item);
                InventoryService.gI().sendItemBag(pl);
            } else {
                Service.gI().sendThongBaoOK(pl, "Sức mạnh không đủ yêu cầu");
            }
        }
    }

    private void hopQuaKichHoat(Player player, Item item) {
        NpcService.gI().createMenuConMeo(player,
                ConstNpc.MENU_OPTION_USE_ITEM1655, -1, "Chọn hành tinh của cậu đi",
                "Set trái đất",
                "Set namec",
                "Set xayda",
                "Từ chổi");
    }

    public void OpenHopThanlinh(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[][] itemsByGenderAndType = {
                    { 555, 556, 562, 563, 561 },
                    { 557, 558, 564, 565, 561 },
                    { 559, 560, 566, 567, 561 }
            };

            List<Item> allPreInitializedItems = new ArrayList<>();

            for (int genderIndex = 0; genderIndex < itemsByGenderAndType.length; genderIndex++) {
                short aoId = (short) itemsByGenderAndType[genderIndex][0];
                short quanId = (short) itemsByGenderAndType[genderIndex][1];
                short gangId = (short) itemsByGenderAndType[genderIndex][2];
                short giayId = (short) itemsByGenderAndType[genderIndex][3];
                short nhanId = (short) itemsByGenderAndType[genderIndex][4];

                Item aotl = ItemService.gI().createNewItem(aoId);
                RewardService.gI().initChiSoItem(aotl);
                aotl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(aotl);

                Item wTl = ItemService.gI().createNewItem(quanId);
                RewardService.gI().initChiSoItem(wTl);
                wTl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(wTl);

                Item gTl = ItemService.gI().createNewItem(gangId);
                RewardService.gI().initChiSoItem(gTl);
                gTl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(gTl);

                Item jayTl = ItemService.gI().createNewItem(giayId);
                RewardService.gI().initChiSoItem(jayTl);
                jayTl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(jayTl);

                Item RdTl = ItemService.gI().createNewItem(nhanId);
                RewardService.gI().initChiSoItem(RdTl);
                RdTl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(RdTl);
            }

            Random random = new Random();
            Item chosenItem = allPreInitializedItems.get(random.nextInt(allPreInitializedItems.size()));
            InventoryService.gI().addItemBag(player, chosenItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn vừa nhận được 1 " + chosenItem.template.name + " Thần linh!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có ít nhất 1 ô trống hành trang");
        }
    }

    private void ThucAnChoThan(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            Item itemUsed = InventoryService.gI().findItemBag(player, item.template.id);

            if (itemUsed == null || itemUsed.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int id = itemUsed.template.id;
            if ((id == 1747 || id == 1816 || id == 1817 || id == 1818 || id == 1819 || id == 1820 || id == 1821)
                    && itemUsed.quantity >= 99) {

                InventoryService.gI().subQuantityItemsBag(player, itemUsed, 99);

                Item newItem = ItemService.gI().createNewItem((short) 1946);
                InventoryService.gI().addItemBag(player, newItem);

                Service.gI().sendThongBao(player, "Bạn đã nhận được 1 " + newItem.template.name + "!");
            } else {
                Service.gI().sendThongBao(player, "Số lượng vật phẩm không đủ hoặc không đúng loại!");
                return;
            }

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
        }
    }

    private int randomInRange(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    public void RadaNgocRong(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, 1952);

            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = { 1579, 1580, 1581, 1582, 1583, 1584, 1585 };
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            int dame = randomInRange(10, 17);
            int hp = randomInRange(10, 17);
            int ki = randomInRange(10, 17);
            int khongthegiaodich = randomInRange(0, 0);

            newItem.itemOptions.add(new ItemOption(50, dame));
            newItem.itemOptions.add(new ItemOption(77, hp));
            newItem.itemOptions.add(new ItemOption(103, ki));
            newItem.itemOptions.add(new ItemOption(30, khongthegiaodich));

            int randomOption = (int) (Math.random() * 100);
            newItem.itemOptions.add(new ItemOption(93, randomOption < 0.5 ? 0 : 15));

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBag(player);
        }
    }

    private void addDamageByItem(Player player, Item item) {
        short[] icon = new short[2];
        icon[0] = item.template.iconID;
        if (item.template.id != 2030) {
            Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ!");
            return;
        }
        if (item.quantity < 20) {
            Service.gI().sendThongBao(player, "Cần đủ 200 vật phẩm để thực hiện!");
            return;
        }
        String msg = "";
        int rd = Util.nextInt(0, 2); // Trả về 0, 1, hoặc 2
        switch (rd) {
            case 0: // Cộng Sức đánh
                player.nPoint.dameg += 10;
                msg = "Bạn đã được tăng 10 Sức đánh!";
                break;
            case 1: // Cộng HP
                player.nPoint.hpg += 100;
                msg = "Bạn đã được tăng 100 HP!";
                break;
            case 2: // Cộng KI
                player.nPoint.mpg += 100;
                msg = "Bạn đã được tăng 100 KI!";
                break;
        }
        InventoryService.gI().subQuantityItemsBag(player, item, 20);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, msg);
        player.nPoint.calPoint();
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().point(player);
        CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
    }

    private static final Random rand = new Random();

    private void changePetRamdom(Player player, Item item) {
        short[] icon = new short[2];
        icon[0] = item.template.iconID;

        if (item.template.id != 1822) {
            Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ!");
            return;
        }

        // ⭐ TUYỆT THẾ ĐỆ TỬ: 6000 Kilis + đệ 3k kilis (typePet 2/3/4) + power >= 100 tỷ
        if (item.hasOption(249, 6000)
                && player.pet != null
                && (player.pet.typePet == 2 || player.pet.typePet == 3 || player.pet.typePet == 4)
                && player.pet.nPoint.power >= 100_000_000_000L) {

            byte limitPower = player.pet.nPoint.limitPower;
            int gender = player.pet.gender;

            PetService.gI().deletePet(player);
            PetService.gI().createTuyetThePet(player, gender, limitPower);

            InventoryService.gI().removeItemBag(player, item);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã nhận được Tuyệt Thế Đệ Tử!");
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
            return;
        }

        // ⭐ ĐỆ 3K KILIS: Logic cũ
        if (!item.hasOption(249, 3000)) {
            Service.gI().sendThongBao(player, "Cần ít nhất 3000 sức mạnh Kilis để mở!");
            return;
        }

        if (player.pet == null || player.pet.typePet != 1 || player.pet.nPoint.power < 40_000_000_000L) {
            Service.gI().sendThongBao(player, "Cần có đệ Mabư đạt 40 tỷ sức mạnh để thực hiện!");
            return;
        }

        // ⭐ LƯU THÔNG TIN ĐỆ CŨ
        byte limitPower = player.pet.nPoint.limitPower;
        int gender = player.pet.gender;

        // ⭐ XÓA ĐỆ CŨ
        PetService.gI().deletePet(player);

        int[] petTypes = { 2, 3, 4 };
        int randomType = petTypes[rand.nextInt(petTypes.length)];

        switch (randomType) {
            case 2:
                PetService.gI().createBlackGokuPet(player, gender, limitPower);
                break;
            case 3:
                PetService.gI().createCellPet(player, gender, limitPower);
                break;
            case 4:
                PetService.gI().createBerusPet(player, gender, limitPower);
                break;
        }

        InventoryService.gI().removeItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận được đệ tử mới!");
        CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
    }

    private void banhtrungthucaocap(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            RandomCollection<Integer> rd = new RandomCollection<>();
            rd.add(15, 4);
            rd.add(15, 3);
            rd.add(15, 2);
            rd.add(30, 1);
            rd.add(10, 5);
            int color = rd.next();
            if (color == 2) {
                int[] vatpham = new int[] { 1944 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(20, 25)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(15, 17)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(15, 17)));
                Pet.itemOptions.add(new ItemOption(14, Util.nextInt(1, 7)));
                Pet.itemOptions.add(new ItemOption(5, Util.nextInt(15, 20)));
                Pet.itemOptions.add(new ItemOption(30, 0));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 3) {
                int[] vatpham = new int[] { 1765, 1766, 1767 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(17, 23)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(17, 20)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(17, 20)));
                Pet.itemOptions.add(new ItemOption(83, Util.nextInt(10, 20)));
                Pet.itemOptions.add(new ItemOption(5, Util.nextInt(10, 15)));
                Pet.itemOptions.add(new ItemOption(30, 0));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 2) {
                int[] vatpham = new int[] { 1700, 1943, 1945 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 6)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 1) {
                int[] vatpham = new int[] { 1901, 1204, 1066, 1067, 1068, 1069, 1070, 1173 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item vatphamzz = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, vatphamzz);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + vatphamzz.template.name);
            } else {
                int[] vatpham = new int[] { 1071, 1072, 1073, 1084, 1085, 1086, 1074, 1075, 1076, 1077, 1078, 1079,
                        1080, 1081, 1082, 1083, 1440 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                Item vatphamzz = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().addItemBag(pl, vatphamzz);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + vatphamzz.template.name);
            }
        }
    }

    private void openthonglong(Player pl, Item item) {
        if (pl.zone.map.mapId != 0 && pl.zone.map.mapId != 7 && pl.zone.map.mapId != 14) {
            Service.gI().sendThongBao(pl, "Không tìm thấy cậu vàng!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            RandomCollection<Integer> rd = new RandomCollection<>();
            rd.add(15, 4);
            rd.add(15, 3);
            rd.add(15, 2);
            rd.add(30, 1);
            rd.add(10, 5);
            int color = rd.next();
            if (color == 5) {
                int[] vatpham = new int[] { 1983 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(20, 25)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(15, 17)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(15, 17)));
                Pet.itemOptions.add(new ItemOption(14, Util.nextInt(1, 7)));
                Pet.itemOptions.add(new ItemOption(5, Util.nextInt(15, 20)));
                Pet.itemOptions.add(new ItemOption(30, 0));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 4) {
                int[] vatpham = new int[] { 1944 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(20, 17)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(15, 17)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(15, 17)));
                Pet.itemOptions.add(new ItemOption(14, Util.nextInt(1, 7)));
                Pet.itemOptions.add(new ItemOption(5, Util.nextInt(15, 20)));
                Pet.itemOptions.add(new ItemOption(30, 0));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 3) {
                int[] vatpham = new int[] { 1765, 1766, 1767 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(17, 23)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(17, 20)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(17, 20)));
                Pet.itemOptions.add(new ItemOption(83, Util.nextInt(10, 20)));
                Pet.itemOptions.add(new ItemOption(5, Util.nextInt(10, 15)));
                Pet.itemOptions.add(new ItemOption(30, 0));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 2) {
                int[] vatpham = new int[] { 1700, 1943, 1945 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 6)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 1) {
                int[] vatpham = new int[] { 1901, 1204, 1066, 1067, 1068, 1069, 1070, 1173 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item vatphamzz = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, vatphamzz);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + vatphamzz.template.name);
            } else {
                int[] vatpham = new int[] { 1071, 1072, 1073, 1084, 1085, 1086, 1074, 1075, 1076, 1077, 1078, 1079,
                        1080, 1081, 1082, 1083, 1440 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                Item vatphamzz = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().addItemBag(pl, vatphamzz);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + vatphamzz.template.name);
            }
        }
    }

    private void opHopBanhNho(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            if (Util.isTrue(25, 100)) {
                int[] ngocrong = new int[] { 17, 17, 1143, 17, 16, 17, 17, 17, 17 };
                int randomtrungpet = new Random().nextInt(ngocrong.length);
                Item pet = ItemService.gI().createNewItem((short) ngocrong[randomtrungpet]);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + pet.template.name);
            } else if (Util.isTrue(50, 100)) {
                short[] temp = { 381, 382, 383, 384 };
                byte index = (byte) Util.nextInt(0, temp.length - 1);
                Item it = ItemService.gI().createNewItem(temp[index]);
                it.quantity = 1;
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, it);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + it.template.name);
            } else {
                short[] temp = { 1204, 859, 956 };
                byte index = (byte) Util.nextInt(0, temp.length - 1);
                Item it = ItemService.gI().createNewItem(temp[index]);
                it.quantity = 1;
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, it);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + it.template.name);
            }
        } else {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
        }
    }

    private void banhtrungthu(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            RandomCollection<Integer> rd = new RandomCollection<>();
            rd.add(15, 4);
            rd.add(15, 3);
            rd.add(15, 2);
            rd.add(30, 1);
            rd.add(10, 5);
            int color = rd.next();
            if (color == 5) {
                int[] vatpham = new int[] { 730, 731, 732 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(165, 10));
                Pet.itemOptions.add(new ItemOption(50, 20));
                Pet.itemOptions.add(new ItemOption(103, 17));
                Pet.itemOptions.add(new ItemOption(77, 17));
                Pet.itemOptions.add(new ItemOption(1540, 0));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int ruby = Util.nextInt(500, 1000);
                pl.inventory.ruby += ruby;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + ruby + " Hồng Ngọc");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 4) {
                int[] vatpham = new int[] { 467, 468, 469, 470, 471 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int ruby = Util.nextInt(300, 500);
                pl.inventory.ruby += ruby;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + ruby + " Hồng Ngọc");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 3) {
                int[] vatpham = new int[] { 1765, 1766, 1767 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int ruby = Util.nextInt(200, 300);
                pl.inventory.ruby += ruby;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + ruby + " Hồng Ngọc");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 2) {
                int[] vatpham = new int[] { 1926, 1927 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 6)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int ruby = Util.nextInt(100, 200);
                pl.inventory.ruby += ruby;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + ruby + " Hồng Ngọc");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 1) {
                int[] vatpham = new int[] { 1921, 1922, 1923, 1924, 1925 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int ruby = Util.nextInt(50, 100);
                pl.inventory.ruby += ruby;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + ruby + " Hồng Ngọc");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else {
                int[] vatpham = new int[] { 987, 16, 1173 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                Item vatphamzz = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                int ruby = Util.nextInt(1, 50);
                pl.inventory.ruby += ruby;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + ruby + " Hồng Ngọc");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().addItemBag(pl, vatphamzz);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + vatphamzz.template.name);
            }
        }
    }

    private void banhtrungthuchay(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            RandomCollection<Integer> rd = new RandomCollection<>();
            rd.add(15, 4);
            rd.add(15, 3);
            rd.add(15, 2);
            rd.add(30, 1);
            rd.add(10, 5);
            int color = rd.next();
            if (color == 5) {
                int[] vatpham = new int[] { 730, 731, 732 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(165, 10));
                Pet.itemOptions.add(new ItemOption(50, 20));
                Pet.itemOptions.add(new ItemOption(103, 17));
                Pet.itemOptions.add(new ItemOption(77, 17));
                Pet.itemOptions.add(new ItemOption(1540, 0));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int gold = Util.nextInt(500_000_000, 1_000_000_000);
                pl.inventory.gold += gold;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + gold + " vàng");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            }
            if (color == 4) {
                int[] vatpham = new int[] { 467, 468, 469, 470, 471 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int gold = Util.nextInt(300_000_000, 500_000_000);
                pl.inventory.gold += gold;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + gold + " vàng");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 3) {
                int[] vatpham = new int[] { 1765, 1766, 1767 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int gold = Util.nextInt(100_000_000, 300_000_000);
                pl.inventory.gold += gold;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + gold + " vàng");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 2) {
                int[] vatpham = new int[] { 1926, 1927 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 6)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int gold = Util.nextInt(50_000_000, 100_000_000);
                pl.inventory.gold += gold;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + gold + " vàng");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else if (color == 1) {
                int[] vatpham = new int[] { 1921, 1922, 1923, 1924, 1925 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                Item Pet = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));

                if (Util.isTrue(90, 100)) {
                    Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                }
                int gold = Util.nextInt(10_000_000, 50_000_000);
                pl.inventory.gold += gold;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + gold + " vàng");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            } else {
                int[] vatpham = new int[] { 987, 16, 1173 };
                int randomvatpham = new Random().nextInt(vatpham.length);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                Item vatphamzz = ItemService.gI().createNewItem((short) vatpham[randomvatpham]);
                int gold = Util.nextInt(1_000_000, 10_000_000);
                pl.inventory.gold += gold;
                Service.gI().sendThongBao(pl, "Bạn nhận được " + gold + " vàng");
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().addItemBag(pl, vatphamzz);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + vatphamzz.template.name);
            }
        }
    }

    private void dotPhao(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            // pl.inventory.diemvulan++;
            // Service.gI().sendThongBao(pl, "Điểm bắn pháo của bạn là :" +
            // pl.inventory.diemvulan);
            int[] temp = { Util.nextInt(15, 16), 1281, 1282, 1283, 674 };

            byte index = (byte) Util.nextInt(0, temp.length - 1);

            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            Item it = ItemService.gI().createNewItem((short) temp[index]);
            if (temp[index] == 1278) {
                it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 8)));
                it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 8)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 8)));
                it.itemOptions.add(new ItemOption(30, 0));

                if (Util.isTrue(1, 30)) {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                } else {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 30)));
                }
            } else if (temp[index] == 1104 || temp[index] == 1105 || temp[index] == 1106) { // mèo mun
                it.itemOptions.add(new ItemOption(77, Util.nextInt(20, 30)));
                it.itemOptions.add(new ItemOption(103, Util.nextInt(20, 30)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(20, 30)));
                if (Util.isTrue(1, 50)) {
                    it.itemOptions.add(new ItemOption(74, 0));
                } else {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 30)));
                }
            } else if (temp[index] == 380) { // mèo mun
                it.quantity = 5;
            } else if (temp[index] >= 1185 && temp[index] <= 1186 || temp[index] == 1687) { // mèo mun
                setOptionItem(it, 5, 12);
            } else if (temp[index] >= 1202 && temp[index] <= 1203) { // mèo mun
                setOptionItem(it, 1, 8);
            } else if (temp[index] == 743) { // chổi bay
                it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 12)));
                it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 12)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 10)));
                it.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(10, 100)) {
                    it.itemOptions.add(new ItemOption(74, 0));
                } else {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 30)));
                }
            } else {
                it.itemOptions.add(new ItemOption(73, 0));
            }
            InventoryService.gI().addItemBag(pl, it);
            icon[1] = it.template.iconID;

            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
            activePhaoHoa(pl);
        } else {
            Service.gI().sendThongBao(pl, "Hành trang của bạn không đủ ô trống");
        }
    }

    public static void activePhaoHoa(Player pl) {
        for (int i = 0; i < 10; i++) {
            EffectMapService.gI().sendEffectMapToAllInMap(pl.zone, 64, 1, 1, pl.location.x - Util.nextInt(-50, 50),
                    pl.location.y, 1);

        }
    }

    private void setOptionItem(Item item, int min, int max) {
        int[] temp = { 50, 77, 103 };
        int index = Util.nextInt(0, temp.length - 1);
        int ops = temp[index];
        int param = 0;
        switch (ops) {
            case 50:
                param = Util.nextInt(min, max);
                break;
            case 77:
                param = Util.nextInt(min, max);
                break;
            case 103:
                param = Util.nextInt(min, max);
                break;
            case 101:
                param = Util.nextInt(min, max * 2);
                break;
            case 14:
                param = Util.nextInt(1, 10);
                break;
            case 94:
                param = Util.nextInt(5, 15);
                break;

        }
        if (Util.isTrue(95, 100)) {
            item.itemOptions.add(new ItemOption(93, Util.nextInt(1, 5)));
        }
        item.itemOptions.add(new ItemOption(ops, param));

    }

    private void opkhoabac(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item ruongkhobau = InventoryService.gI().findItemBag(pl, 1569);
            if (ruongkhobau != null) {
                RandomCollection<Integer> rd = new RandomCollection<>();
                rd.add(40, 5);
                rd.add(35, 4);
                rd.add(25, 3);
                int color = rd.next();
                if (color == 4) {
                    short[] temp = { 441, 442, 447, 381, 382, 383, 384 };
                    byte index = (byte) Util.nextInt(0, temp.length - 1);
                    Item it = ItemService.gI().createNewItem(temp[index]);
                    if (temp[index] == 441) {
                        it.itemOptions.add(new ItemOption(95, 5));
                        it.quantity = 2;
                    } else if (temp[index] == 442) {
                        it.itemOptions.add(new ItemOption(96, 5));
                        it.quantity = 2;
                    } else if (temp[index] == 447) {
                        it.itemOptions.add(new ItemOption(101, 5));
                        it.quantity = 2;
                    } else {
                        it.quantity = 1;
                    }
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, it);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + it.template.name);
                } else if (color == 3) {
                    int[] ngocrong = new int[] { 18, 17, 18 };
                    int randomtrungpet = new Random().nextInt(ngocrong.length);
                    Item pet = ItemService.gI().createNewItem((short) ngocrong[randomtrungpet]);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, pet);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + pet.template.name);
                } else {
                    int[] itdeolung = new int[] { 1554, 1555 };
                    int randomIMDEOLUNG = new Random().nextInt(itdeolung.length);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    Item Itdeolung = ItemService.gI().createNewItem((short) itdeolung[randomIMDEOLUNG]);
                    Itdeolung.itemOptions.add(new ItemOption(50, Util.nextInt(2, 8)));
                    Itdeolung.itemOptions.add(new ItemOption(77, Util.nextInt(3, 6)));
                    Itdeolung.itemOptions.add(new ItemOption(103, Util.nextInt(3, 6)));
                    if (Util.isTrue(95, 100)) {
                        Itdeolung.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                    }
                    InventoryService.gI().addItemBag(pl, Itdeolung);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Itdeolung.template.name);
                }
            }

        } else {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
        }
    }

    private void opkhoavang(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item ruongkhobau = InventoryService.gI().findItemBag(pl, 1569);
            if (ruongkhobau != null) {
                Item itemReward = null;
                RandomCollection<Integer> rd = new RandomCollection<>();

                rd.add(60, 4);
                rd.add(30, 5);

                rd.add(10, 3);
                rd.add(1, 2);
                rd.add(1, 1);
                int color = rd.next();
                if (color == 4) {
                    short[] temp = { 1150, 1151, 1152, 1153 };
                    byte index = (byte) Util.nextInt(0, temp.length - 1);
                    Item it = ItemService.gI().createNewItem(temp[index]);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, it);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + it.template.name);
                } else if (color == 2) {
                    int[] set2 = { 555, 556, 563, 557, 558, 565, 559, 567, 560 };
                    itemReward = ItemService.gI().createNewItem((short) set2[Util.nextInt(0, set2.length - 1)]);
                    RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type,
                            itemReward.itemOptions);
                    RewardService.gI().initStarOption(itemReward,
                            new RewardService.RatioStar[] { new RewardService.RatioStar((byte) 1, 1, 2),
                                    new RewardService.RatioStar((byte) 2, 1, 3),
                                    new RewardService.RatioStar((byte) 3, 1, 4),
                                    new RewardService.RatioStar((byte) 4, 1, 5), });
                    InventoryService.gI().addItemBag(pl, itemReward);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendMoney(pl);
                } else if (color == 3) {
                    int[] ngocrong = new int[] { 16, 17 };
                    int randomtrungpet = new Random().nextInt(ngocrong.length);
                    Item pet = ItemService.gI().createNewItem((short) ngocrong[randomtrungpet]);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, pet);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + pet.template.name);
                } else if (color == 1) {
                    int[] set1 = { 562, 564, 566, 561 };
                    itemReward = ItemService.gI().createNewItem((short) set1[Util.nextInt(0, set1.length - 1)]);
                    RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type,
                            itemReward.itemOptions);
                    RewardService.gI().initStarOption(itemReward,
                            new RewardService.RatioStar[] { new RewardService.RatioStar((byte) 1, 1, 2),
                                    new RewardService.RatioStar((byte) 2, 1, 3),
                                    new RewardService.RatioStar((byte) 3, 1, 4),
                                    new RewardService.RatioStar((byte) 4, 1, 5), });
                    InventoryService.gI().addItemBag(pl, itemReward);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendMoney(pl);
                } else if (color == 5) {
                    int[] itdeolung = new int[] { 1578, 1563, 1603 };
                    int randomIMDEOLUNG = new Random().nextInt(itdeolung.length);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    Item Itdeolung = ItemService.gI().createNewItem((short) itdeolung[randomIMDEOLUNG]);
                    Itdeolung.itemOptions.add(new ItemOption(50, Util.nextInt(5, 12)));
                    Itdeolung.itemOptions.add(new ItemOption(77, Util.nextInt(5, 12)));
                    Itdeolung.itemOptions.add(new ItemOption(103, Util.nextInt(5, 12)));
                    if (Util.isTrue(950, 1000)) {
                        Itdeolung.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                    }
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, Itdeolung);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Itdeolung.template.name);
                }
                // pl.inventory.diemvulan++;
                Service.gI().sendThongBao(pl, "Bạn nhận được 1 điểm kho báu");

            }

        } else {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
        }
    }

    private void openfa(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item ruongkhobau = InventoryService.gI().findItemBag(pl, 1569);
            if (ruongkhobau != null) {
                Item itemReward = null;
                RandomCollection<Integer> rd = new RandomCollection<>();

                rd.add(60, 4);
                rd.add(30, 5);

                rd.add(10, 3);
                rd.add(1, 2);
                rd.add(1, 1);
                int color = rd.next();
                if (color == 4) {
                    short[] temp = { 1150, 1151, 1152, 1153 };
                    byte index = (byte) Util.nextInt(0, temp.length - 1);
                    Item it = ItemService.gI().createNewItem(temp[index]);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, it);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + it.template.name);
                } else if (color == 2) {
                    int[] set2 = { 555, 556, 563, 557, 558, 565, 559, 567, 560 };
                    itemReward = ItemService.gI().createNewItem((short) set2[Util.nextInt(0, set2.length - 1)]);
                    RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type,
                            itemReward.itemOptions);
                    RewardService.gI().initStarOption(itemReward,
                            new RewardService.RatioStar[] { new RewardService.RatioStar((byte) 1, 1, 2),
                                    new RewardService.RatioStar((byte) 2, 1, 3),
                                    new RewardService.RatioStar((byte) 3, 1, 4),
                                    new RewardService.RatioStar((byte) 4, 1, 5), });
                    InventoryService.gI().addItemBag(pl, itemReward);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendMoney(pl);
                } else if (color == 3) {
                    int[] ngocrong = new int[] { 16, 17 };
                    int randomtrungpet = new Random().nextInt(ngocrong.length);
                    Item pet = ItemService.gI().createNewItem((short) ngocrong[randomtrungpet]);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, pet);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + pet.template.name);
                } else if (color == 1) {
                    int[] set1 = { 562, 564, 566, 561 };
                    itemReward = ItemService.gI().createNewItem((short) set1[Util.nextInt(0, set1.length - 1)]);
                    RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type,
                            itemReward.itemOptions);
                    RewardService.gI().initStarOption(itemReward,
                            new RewardService.RatioStar[] { new RewardService.RatioStar((byte) 1, 1, 2),
                                    new RewardService.RatioStar((byte) 2, 1, 3),
                                    new RewardService.RatioStar((byte) 3, 1, 4),
                                    new RewardService.RatioStar((byte) 4, 1, 5), });
                    InventoryService.gI().addItemBag(pl, itemReward);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendMoney(pl);
                } else if (color == 5) {
                    int[] itdeolung = new int[] { 1578, 1563, 1603 };
                    int randomIMDEOLUNG = new Random().nextInt(itdeolung.length);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    Item Itdeolung = ItemService.gI().createNewItem((short) itdeolung[randomIMDEOLUNG]);
                    Itdeolung.itemOptions.add(new ItemOption(50, Util.nextInt(5, 12)));
                    Itdeolung.itemOptions.add(new ItemOption(77, Util.nextInt(5, 12)));
                    Itdeolung.itemOptions.add(new ItemOption(103, Util.nextInt(5, 12)));
                    if (Util.isTrue(950, 1000)) {
                        Itdeolung.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                    }
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, Itdeolung);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Itdeolung.template.name);
                }
                // pl.inventory.diemvulan++;
                // Service.gI().sendThongBao(pl, "Bạn nhận được 1 điểm kho báu");

            }

        } else {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
        }
    }

    private void PhaoBong(Player pl, Item item) {
        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.XSMAX, 1);
        int[][] gold = { { 5000, 20000 } };
        short[] icon = new short[2];
        icon[0] = item.template.iconID;
        pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
        if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
            pl.inventory.gold = Inventory.LIMIT_GOLD;
        }
        Service.gI().LogicEffect(pl, 62, 1, -1, 1, 1, 15000);
        Service.gI().LogicEffect(pl, 63, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 64, 1, -1, 1, 1, 5000); // eff này live
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);

        /*
         * pl.point_sukien1 += 1;
         * if (!Manager.isTopSukien1Changed) {
         * Manager.isTopSukien1Changed = true;
         * }
         */
        Item removeItem = InventoryService.gI().findItemBag(pl, 1575);
        if (removeItem != null) {
            InventoryService.gI().subQuantityItemsBag(pl, removeItem, 1);
        }

        PlayerService.gI().sendInfoHpMpMoney(pl);
        InventoryService.gI().sendItemBag(pl);
    }

    private void PhaoBongVip(Player pl, Item item) {
        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.XSMAX, 1);
        int[][] gold = { { 500000, 2000000 } };
        short[] icon = new short[2];
        icon[0] = item.template.iconID;
        pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
        if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
            pl.inventory.gold = Inventory.LIMIT_GOLD;
        }
        Service.gI().LogicEffect(pl, 62, 1, -1, 1, 1, 15000);
        Service.gI().LogicEffect(pl, 63, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 64, 1, -1, 1, 1, 5000); // eff này live
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);

        /*
         * pl.point_sukien += 1;
         * if (!Manager.isTopSukienChanged) {
         * Manager.isTopSukienChanged = true;
         * }
         */
        Item removeItem = InventoryService.gI().findItemBag(pl, 1576);
        if (removeItem != null) {
            InventoryService.gI().subQuantityItemsBag(pl, removeItem, 1);
        }

        PlayerService.gI().sendInfoHpMpMoney(pl);
        InventoryService.gI().sendItemBag(pl);
    }

    public void hopQuaTanThu(Player pl, Item it) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 23) {
            int gender = pl.gender;
            // Majin (gender=3) reuse Xayda (gender=2) items for equipment
            int clothesGender = (gender >= 3) ? 2 : gender;
            int soluongitem = ConstItem.LIST_ITEM_CLOTHES[0][0].length;
            int[] id = { clothesGender, 6 + clothesGender, 21 + clothesGender, 27 + clothesGender, 12, 194, 441, 442, 443, 444, 445, 446, 447, 381,
                    382, 383, 384, 385, 16, 17, 18, 19, 20 };
            int[] soluong = { 1, 1, 1, 1, 1, 1, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999,
                    999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999 };
            int[] option = { 0, 0, 0, 0, 0, 73, 95, 96, 97, 98, 99, 100, 101, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
                    30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30 };
            int[] param = { 0, 0, 0, 0, 0, 0, 5, 5, 5, 3, 3, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    1, 1, 1, 1, 1, 1, 1, 1 };
            int arrLength = id.length - 1;
            // pl.inventory.gold += 50_000_000_000L;
            for (int i = 0; i < arrLength; i++) {
                if (i < 5) {
                    Item item = ItemService.gI().createNewItem((short) id[i]);
                    RewardService.gI().initBaseOptionClothes(item.template.id, item.template.type, item.itemOptions);
                    item.itemOptions.add(new ItemOption(107, 4));
                    InventoryService.gI().addItemBag(pl, item);
                } else {
                    Item item = ItemService.gI().createNewItem((short) id[i]);
                    item.quantity = soluong[i];
                    item.itemOptions.add(new ItemOption(option[i], param[i]));
                    InventoryService.gI().addItemBag(pl, item);
                }

            }
            for (int j = 0; j < 5; j++) {
                int id1 = soluongitem - 1;
                Item item = ItemService.gI()
                        .createNewItem((short) ConstItem.LIST_ITEM_CLOTHES[clothesGender][j][id1]);
                RewardService.gI().initBaseOptionClothes(item.template.id, item.template.type, item.itemOptions);
                item.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(pl, item);
            }

            InventoryService.gI().subQuantityItemsBag(pl, it, 1);
            // Service.gI().sendMoney(pl);
            InventoryService.gI().sendItemBag(pl);
            // Service.getInstance().sendThongBao(pl, "Bạn mở hộp quà tân thủ nhận được 50
            // tỷ vàng");
            Service.gI().sendThongBao(pl, "Chúc bạn chơi game vui vẻ");
        } else {
            Service.gI().sendThongBao(pl, "Cần tối thiểu 14 ô trống để nhận thưởng");
        }
    }

    public void openRuongGo(Player player) {
        // Tìm kiếm rương gỗ trong hành trang của người chơi
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE, 1);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE1, 1);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE2, 1);
        Item ruongGo = InventoryService.gI().findItemBag(player, 570);
        if (ruongGo != null) {
            int level = InventoryService.gI().getParam(player, 72, 570);

            // Tính toán số ô trống cần thiết cho phần thưởng
            int requiredSlots = calculateRequiredEmptySlots(level);

            // Kiểm tra số lượng ô trống trong hành trang người chơi
            if (InventoryService.gI().getCountEmptyBag(player) < requiredSlots) {
                Service.gI().sendThongBao(player,
                        "Cần ít nhất " + (requiredSlots - InventoryService.gI().getCountEmptyBag(player))
                                + " ô trống trong hành trang");
            } else {
                player.itemsWoodChest.clear(); // Xóa các item trong danh sách phần thưởng trước khi mở rương

                // Phần thưởng khi cấp độ = 0
                if (level == 0) {
                    InventoryService.gI().subQuantityItemsBag(player, ruongGo, 1);
                    InventoryService.gI().sendItemBag(player);

                    // Tạo item vàng (ID 190) cho phần thưởng
                    Item item = ItemService.gI().createNewItem((short) 190);
                    item.quantity = 1; // Số lượng vàng ở level 0 là 1
                    InventoryService.gI().addItemBag(player, item);
                    InventoryService.gI().sendItemBag(player);

                    Service.gI().sendThongBao(player, "reward");
                    return; // Thoát ra nếu cấp độ = 0
                }

                // Tính toán số lượng vàng thưởng dựa trên cấp độ
                int baseGoldAmount = 100 * level; // Tính số lượng vàng cơ bản
                int randomFactor = Util.nextInt(-15, 15); // Tạo một yếu tố ngẫu nhiên để biến động số lượng vàng
                int goldAmount = baseGoldAmount + (baseGoldAmount * randomFactor / 100);

                Item itemGold = ItemService.gI().createNewItem((short) 190);
                itemGold.quantity = goldAmount * 1000; // Số lượng vàng thưởng (đơn vị là vàng)
                player.itemsWoodChest.add(itemGold); // Thêm vàng vào phần thưởng
                // Kiểm tra nếu cấp độ > 9
                if (level >= 9) {
                    // Tính số lượng item ID 77, bắt đầu từ 100 và tăng 20 mỗi cấp
                    int quantity = 100 + (level - 9) * 20;

                    // Tạo item với ID 77
                    Item item77 = ItemService.gI().createNewItem((short) 77);
                    item77.quantity = quantity;

                    // Thêm item vào danh sách phần thưởng
                    player.itemsWoodChest.add(item77);
                }

                // Phần thưởng đồ tại rương
                int clothesCount = 1;
                if (level >= 5 && level <= 8) {
                    clothesCount = 2; // Nếu cấp độ từ 5 đến 8, thưởng 2 món đồ
                } else if (level >= 10 && level <= 12) {
                    clothesCount = 3; // Nếu cấp độ từ 10 đến 12, thưởng 3 món đồ
                }

                // Tạo đồ thưởng (clothes) và thêm vào phần thưởng
                for (int i = 0; i < clothesCount; i++) {
                    int randItemId = randClothes(level); // Lấy ID ngẫu nhiên của món đồ
                    Item rewardItem = ItemService.gI().createNewItem((short) randItemId);
                    List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) randItemId);
                    if (ops != null && !ops.isEmpty()) {
                        rewardItem.itemOptions.addAll(ops); // Thêm thuộc tính item
                    }
                    rewardItem.quantity = 1; // Số lượng món đồ là 1
                    player.itemsWoodChest.add(rewardItem); // Thêm món đồ vào phần thưởng
                }

                // Phần thưởng item ngẫu nhiên (từ rewardItems)
                int[] rewardItems = { 17, 18, 19, 20, 380, 381, 382, 383, 384, 385, 1229 };
                int rewardCount = 2; // Số lượng item mặc định

                // Thay đổi số lượng phần thưởng tùy theo cấp độ
                if (level >= 5 && level <= 8) {
                    rewardCount = 3; // Nếu cấp độ từ 5 đến 8, thưởng 3 item ngẫu nhiên
                } else if (level >= 10 && level <= 12) {
                    rewardCount = 4; // Nếu cấp độ từ 10 đến 12, thưởng 4 item ngẫu nhiên
                }

                // Thêm item ngẫu nhiên vào phần thưởng
                Set<Integer> selectedItems = new HashSet<>();
                while (selectedItems.size() < rewardCount) {
                    int randItemId = rewardItems[Util.nextInt(0, rewardItems.length - 1)];
                    if (!selectedItems.contains(randItemId)) {
                        selectedItems.add(randItemId);
                        Item rewardItem = ItemService.gI().createNewItem((short) randItemId);
                        rewardItem.quantity = Util.nextInt(1, level); // Số lượng item phụ thuộc vào cấp độ
                        player.itemsWoodChest.add(rewardItem); // Thêm item vào phần thưởng
                    }
                }

                // Phần thưởng sao pha lê (nâng cấp)
                int saoPhaLeCount = (level > 9) ? 2 : 1; // Nếu cấp độ > 9, thêm 2 sao phá lệ
                for (int i = 0; i < saoPhaLeCount; i++) {
                    int rand = Util.nextInt(0, 6);
                    Item level1 = ItemService.gI().createNewItem((short) (441 + rand));
                    level1.itemOptions.add(new Item.ItemOption(95 + rand, (rand == 3 || rand == 4) ? 3 : 5));
                    level1.quantity = Util.nextInt(1, 3); // Số lượng sao phá lệ
                    player.itemsWoodChest.add(level1); // Thêm sao phá lệ vào phần thưởng
                }

                // Phần thưởng đá nâng cấp
                int dncCount = (level > 9) ? 2 : 1; // Nếu cấp độ > 9, có 2 đá nâng cấp
                for (int i = 0; i < dncCount; i++) {
                    int rand = Util.nextInt(0, 4);
                    Item dnc = ItemService.gI().createNewItem((short) (220 + rand));
                    dnc.itemOptions.add(new Item.ItemOption(71 - rand, 0));
                    dnc.quantity = Util.nextInt(1, level * 2); // Số lượng đá nâng cấp phụ thuộc vào cấp độ
                    player.itemsWoodChest.add(dnc); // Thêm đá nâng cấp vào phần thưởng
                }

                // Trừ 1 rương gỗ
                InventoryService.gI().subQuantityItemsBag(player, ruongGo, 1);
                InventoryService.gI().sendItemBag(player);

                // Thêm các phần thưởng vào hành trang
                for (Item it : player.itemsWoodChest) {
                    InventoryService.gI().addItemBag(player, it);
                }
                InventoryService.gI().sendItemBag(player);

                // Cập nhật chỉ số rương gỗ
                player.indexWoodChest = player.itemsWoodChest.size() - 1;
                int i = player.indexWoodChest;
                if (i < 0) {
                    return;
                }
                Item itemWoodChest = player.itemsWoodChest.get(i);
                player.indexWoodChest--;
                String info = "|1|" + itemWoodChest.template.name;
                if (itemWoodChest.quantity > 1) {
                    info += " (x" + itemWoodChest.quantity + ")";
                }

                String info2 = "\n|2|";
                if (!itemWoodChest.itemOptions.isEmpty()) {
                    for (Item.ItemOption io : itemWoodChest.itemOptions) {
                        if (io.optionTemplate.id != 102 && io.optionTemplate.id != 73) {
                            info2 += io.getOptionString() + "\n";
                        }
                    }
                }
                info = (info2.length() > "\n|2|".length() ? (info + info2).trim() : info.trim()) + "\n|0|"
                        + itemWoodChest.template.description;
                NpcService.gI().createMenuConMeo(player, ConstNpc.RUONG_GO, -1, "Bạn nhận được\n"
                        + info.trim(), "OK" + (i > 0 ? " [" + i + "]" : ""));
            }
        }
    }

    public int calculateRequiredEmptySlots(int level) {
        // Khởi tạo số ô trống cần thiết
        int requiredSlots = 0;

        // Tính số lượng vàng
        int baseGoldAmount = 100 * level;
        int randomFactor = Util.nextInt(-15, 15);
        int goldAmount = baseGoldAmount + (baseGoldAmount * randomFactor / 100);

        // Vàng có ID 190, không tính vào số ô trống yêu cầu
        if (goldAmount > 0) {
            requiredSlots++;
        }

        // Tính phần thưởng quần áo
        int clothesCount = 1;
        if (level >= 5 && level <= 8) {
            clothesCount = 2;
        } else if (level >= 10 && level <= 12) {
            clothesCount = 3;
        }
        // Đếm số phần thưởng quần áo
        requiredSlots += clothesCount;

        // Tính phần thưởng item hỗ trợ
        int[] rewardItems = { 17, 18, 19, 20, 380, 381, 382, 383, 384, 385, 1229 };
        int rewardCount = 2;

        if (level >= 5 && level <= 8) {
            rewardCount = 3;
        } else if (level >= 10 && level <= 12) {
            rewardCount = 4;
        }
        // Đếm phần thưởng item hỗ trợ
        requiredSlots += rewardCount;

        // Tính sao pha lê (Số lượng 2 nếu level > 9)
        int saoPhaLeCount = (level > 9) ? 2 : 1;
        requiredSlots += saoPhaLeCount;

        // Tính đá nâng cấp (Số lượng 2 nếu level > 9)
        int dncCount = (level > 9) ? 2 : 1;
        requiredSlots += dncCount;

        // Trả về tổng số ô trống cần thiết
        return requiredSlots;
    }

    private void changePet(Player player, Item item) {
        if (player.pet != null) {
            int gender = player.pet.gender + 1;
            if (gender > 3) { // cycle through 0,1,2,3 (include Majin)
                gender = 0;
            }
            PetService.gI().changeNormalPet(player, gender);
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
        } else {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
    }

    private void eatGrapes(Player pl, Item item) {
        int percentCurrentStatima = pl.nPoint.stamina * 100 / pl.nPoint.maxStamina;
        if (percentCurrentStatima > 50) {
            Service.gI().sendThongBao(pl, "Thể lực vẫn còn trên 50%");
            return;
        } else if (item.template.id == 211) {
            pl.nPoint.stamina = pl.nPoint.maxStamina;
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 100%");
        } else if (item.template.id == 212) {
            pl.nPoint.stamina += (pl.nPoint.maxStamina * 20 / 100);
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 20%");
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBag(pl);
        PlayerService.gI().sendCurrentStamina(pl);
    }

    private void openCSKB(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] temp = { 76, 188, 189, 190, 381, 382, 383, 384, 385 };
            int[][] gold = { { 5000, 20000 } };
            byte index = (byte) Util.nextInt(0, temp.length - 1);
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            if (index <= 3) {
                pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
                    pl.inventory.gold = Inventory.LIMIT_GOLD;
                }
                PlayerService.gI().sendInfoHpMpMoney(pl);
                icon[1] = 930;
            } else {
                Item it = ItemService.gI().createNewItem(temp[index]);
                it.itemOptions.add(new ItemOption(73, 0));
                InventoryService.gI().addItemBag(pl, it);
                icon[1] = it.template.iconID;
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);

            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private boolean isUsingSameTypeBuff(Player pl, int type) {
        return switch (type) {
            case 382 ->
                pl.itemTime.isUseBoHuyet || pl.itemTime.isUseBoHuyet2;
            case 383 ->
                pl.itemTime.isUseBoKhi || pl.itemTime.isUseBoKhi2;
            case 384 ->
                pl.itemTime.isUseGiapXen || pl.itemTime.isUseGiapXen2;
            case 381 ->
                pl.itemTime.isUseCuongNo || pl.itemTime.isUseCuongNo2;
            case 385 ->
                pl.itemTime.isUseAnDanh || pl.itemTime.isUseAnDanh2;
            default ->
                false;
        };
    }

    private void useItemTime(Player pl, Item item) {

        switch (item.template.id) {

            case 1809 -> {
                pl.itemTime.lastTimevevang = System.currentTimeMillis();
                pl.itemTime.isUsevevang = true;
            }

            // ==== LỌ NƯỚC THÁNH (X2 - X15) ====
            case 1731 -> {
                if (pl.itemTime.isUseLoX5 || pl.itemTime.isUseLoX7 || pl.itemTime.isUseLoX10
                        || pl.itemTime.isUseLoX15) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX2 = System.currentTimeMillis();
                pl.itemTime.isUseLoX2 = true;
            }
            case 1727 -> {
                if (pl.itemTime.isUseLoX2 || pl.itemTime.isUseLoX7 || pl.itemTime.isUseLoX10
                        || pl.itemTime.isUseLoX15) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX5 = System.currentTimeMillis();
                pl.itemTime.isUseLoX5 = true;
            }
            case 1728 -> {
                if (pl.itemTime.isUseLoX5 || pl.itemTime.isUseLoX2 || pl.itemTime.isUseLoX10
                        || pl.itemTime.isUseLoX15) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX7 = System.currentTimeMillis();
                pl.itemTime.isUseLoX7 = true;
            }
            case 1729 -> {
                if (pl.itemTime.isUseLoX5 || pl.itemTime.isUseLoX7 || pl.itemTime.isUseLoX2 || pl.itemTime.isUseLoX15) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX10 = System.currentTimeMillis();
                pl.itemTime.isUseLoX10 = true;
            }
            case 1730 -> {
                if (pl.itemTime.isUseLoX5 || pl.itemTime.isUseLoX7 || pl.itemTime.isUseLoX10 || pl.itemTime.isUseLoX2) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX15 = System.currentTimeMillis();
                pl.itemTime.isUseLoX15 = true;
            }

            case 764 -> {
                pl.itemTime.lastTimeKhauTrang = System.currentTimeMillis();
                pl.itemTime.isUseKhauTrang = true;
                Service.gI().Send_Caitrang(pl);
            }

            case 1628 -> {
                pl.itemTime.lastTimeBuax2DeTu = System.currentTimeMillis();
                pl.itemTime.isUseBuax2DeTu = true;
            }

            // ===== BUFF THƯỜNG =====
            case 382 -> {
                if (isUsingSameTypeBuff(pl, 382)) {
                    Service.gI().sendThongBao(pl, "Bạn đã dùng Bổ Huyết hoặc Bổ Huyết VIP rồi!");
                    return;
                }
                pl.itemTime.lastTimeBoHuyet = System.currentTimeMillis();
                pl.itemTime.isUseBoHuyet = true;
                Service.gI().point(pl);
            }

            case 383 -> {
                if (isUsingSameTypeBuff(pl, 383)) {
                    Service.gI().sendThongBao(pl, "Bạn đã dùng Bổ Khí hoặc Bổ Khí VIP rồi!");
                    return;
                }
                pl.itemTime.lastTimeBoKhi = System.currentTimeMillis();
                pl.itemTime.isUseBoKhi = true;
                Service.gI().point(pl);
            }

            case 384 -> {
                // if (isUsingSameTypeBuff(pl, 384)) {
                // Service.gI().sendThongBao(pl, "Bạn đã dùng Giáp Xên hoặc Giáp Xên VIP rồi!");
                // return;
                // }
                pl.itemTime.lastTimeGiapXen = System.currentTimeMillis();
                pl.itemTime.isUseGiapXen = true;
                Service.gI().point(pl);
            }
            case 379 -> {
                if (isUsingSameTypeBuff(pl, 379))
                    return;
                pl.itemTime.lastTimeUseMayDo = System.currentTimeMillis();
                pl.itemTime.isUseMayDo = true;
            }
            case 381 -> {
                if (isUsingSameTypeBuff(pl, 381)) {
                    Service.gI().sendThongBao(pl, "Bạn đã dùng Cuồng Nộ hoặc Cuồng Nộ VIP rồi!");
                    return;
                }
                pl.itemTime.lastTimeCuongNo = System.currentTimeMillis();
                pl.itemTime.isUseCuongNo = true;
                Service.gI().point(pl);
            }

            case 385 -> {
                if (isUsingSameTypeBuff(pl, 385)) {
                    Service.gI().sendThongBao(pl, "Bạn đã dùng Ẩn Danh hoặc Ẩn Danh VIP rồi!");
                    return;
                }
                pl.itemTime.lastTimeAnDanh = System.currentTimeMillis();
                pl.itemTime.isUseAnDanh = true;
                Service.gI().point(pl);
            }

            // ===== BUFF VIP =====
            case 1151 -> {
                if (isUsingSameTypeBuff(pl, 382)) {
                    Service.gI().sendThongBao(pl, "Bạn đã dùng Bổ Huyết hoặc Bổ Huyết VIP rồi!");
                    return;
                }
                pl.itemTime.lastTimeBoHuyet2 = System.currentTimeMillis();
                pl.itemTime.isUseBoHuyet2 = true;
                Service.gI().point(pl);
            }

            case 1152 -> {
                if (isUsingSameTypeBuff(pl, 383)) {
                    Service.gI().sendThongBao(pl, "Bạn đã dùng Bổ Khí hoặc Bổ Khí VIP rồi!");
                    return;
                }
                pl.itemTime.lastTimeBoKhi2 = System.currentTimeMillis();
                pl.itemTime.isUseBoKhi2 = true;
                Service.gI().point(pl);
            }

            case 1153 -> {
                // if (isUsingSameTypeBuff(pl, 384)) {
                // Service.gI().sendThongBao(pl, "Bạn đã dùng Giáp Xên hoặc Giáp Xên VIP rồi!");
                // return;
                // }
                pl.itemTime.lastTimeGiapXen2 = System.currentTimeMillis();
                pl.itemTime.isUseGiapXen2 = true;
                Service.gI().point(pl);
            }

            case 1150 -> {
                if (isUsingSameTypeBuff(pl, 381)) {
                    Service.gI().sendThongBao(pl, "Bạn đã dùng Cuồng Nộ hoặc Cuồng Nộ VIP rồi!");
                    return;
                }
                pl.itemTime.lastTimeCuongNo2 = System.currentTimeMillis();
                pl.itemTime.isUseCuongNo2 = true;
                Service.gI().point(pl);
            }

            case 1154 -> {
                if (isUsingSameTypeBuff(pl, 385)) {
                    Service.gI().sendThongBao(pl, "Bạn đã dùng Ẩn Danh hoặc Ẩn Danh VIP rồi!");
                    return;
                }
                pl.itemTime.lastTimeAnDanh2 = System.currentTimeMillis();
                pl.itemTime.isUseAnDanh2 = true;
                Service.gI().point(pl);
            }

            // ===== PHỞ (generic buff - đọc options từ item) =====
            case 1980, 1979, 1978 -> {
                // Kiểm tra chỉ dùng được 1 loại phở
                if (pl.itemTime.hasActiveBuff(1980) || pl.itemTime.hasActiveBuff(1979) || pl.itemTime.hasActiveBuff(1978)) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng 1 loại phở!");
                    return;
                }
                if (!pl.itemTime.addBuff(item, ItemTime.TIME_EAT_MEAL3)) {
                    Service.gI().sendThongBao(pl, "Không thể sử dụng!");
                    return;
                }
            }

            // ===== TRUNG THU (generic buff - đọc options từ item) =====
            case 465, 466 -> {
                // Kiểm tra chỉ dùng được 1 loại bánh
                if (pl.itemTime.hasActiveBuff(465) || pl.itemTime.hasActiveBuff(466)) {
                    Service.gI().sendThongBao(pl, "Chỉ dùng được 1 loại!");
                    return;
                }
                if (!pl.itemTime.addBuff(item, ItemTime.TIME_30P)) {
                    Service.gI().sendThongBao(pl, "Không thể sử dụng!");
                    return;
                }
            }

            case 472, 473 -> {
                if (!pl.itemTime.addBuff(item, ItemTime.TIME_30P)) {
                    Service.gI().sendThongBao(pl, "Không thể sử dụng!");
                    return;
                }
            }

            case 638 -> {
                pl.itemTime.lastTimeUseCMS = System.currentTimeMillis();
                pl.itemTime.isUseCMS = true;
            }
            case 2160 -> {
                pl.itemTime.lastTimeUseNCD = System.currentTimeMillis();
                pl.itemTime.isUseNCD = true;
            }

            case 579, 1045 -> {
                pl.itemTime.lastTimeUseDK = System.currentTimeMillis();
                pl.itemTime.isUseDK = true;
            }

            // ==== BỮA ĂN ====
            case 663, 664, 665, 666, 667 -> {
                pl.itemTime.lastTimeEatMeal = System.currentTimeMillis();
                pl.itemTime.isEatMeal = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal);
                pl.itemTime.iconMeal = item.template.iconID;
            }

            case 880, 881, 882 -> {
                pl.itemTime.lastTimeEatMeal2 = System.currentTimeMillis();
                pl.itemTime.isEatMeal2 = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal2);
                pl.itemTime.iconMeal2 = item.template.iconID;
            }

            case 889, 900, 902, 903 -> {
                pl.itemTime.lastTimeEatMeal3 = System.currentTimeMillis();
                pl.itemTime.isEatMeal3 = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal3);
                pl.itemTime.iconMeal3 = item.template.iconID;
            }

            case 1109 -> {
                pl.itemTime.lastTimeUseMayDo2 = System.currentTimeMillis();
                pl.itemTime.isUseMayDo2 = true;
            }

            case 1635 -> {
                long now = System.currentTimeMillis();

                long remaining = 0;
                if (pl.itemTime.isCoBonLa) {
                    remaining = ItemTime.TIME_CO_BON_LA
                            - (now - pl.itemTime.lastTimeCoBonLa);

                    if (remaining < 0) {
                        remaining = 0;
                    }
                } else {
                    pl.itemTime.lastTimeCoBonLa = now;
                }
                long total = remaining + ItemTime.TIME_CO_BON_LA;
                pl.itemTime.lastTimeCoBonLa = now - (ItemTime.TIME_CO_BON_LA - total);
                pl.itemTime.isCoBonLa = true;
            }
        }

        Service.gI().point(pl);
        ItemTimeService.gI().sendAllItemTime(pl);
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBag(pl);
    }

    private void controllerCallRongThan(Player pl, Item item) {
        int tempId = item.template.id;
        if (tempId >= SummonDragon.NGOC_RONG_1_SAO && tempId <= SummonDragon.NGOC_RONG_7_SAO) {
            switch (tempId) {
                case SummonDragon.NGOC_RONG_1_SAO:
                case SummonDragon.NGOC_RONG_2_SAO:
                case SummonDragon.NGOC_RONG_3_SAO:
                    SummonDragon.gI().openMenuSummonShenron(pl, (byte) (tempId - 13));
                    break;
                default:
                    NpcService.gI().createMenuConMeo(pl, ConstNpc.TUTORIAL_SUMMON_DRAGON,
                            -1, "Bạn chỉ có thể gọi rồng từ ngọc 3 sao, 2 sao, 1 sao", "Hướng\ndẫn thêm\n(mới)", "OK");
                    break;
            }
        } else if (tempId >= ShenronEventService.NGOC_RONG_1_SAO && tempId <= ShenronEventService.NGOC_RONG_7_SAO) {
            ShenronEventService.gI().openMenuSummonShenron(pl, 0);
        }
    }

    private void learnSkill(Player pl, Item item) {
        Message msg;
        try {
            if (item.template.gender == pl.gender || item.template.gender == 3) {
                String[] subName = item.template.name.split("");
                byte level = Byte.parseByte(subName[subName.length - 1]);
                Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
                if (curSkill.point == 7) {
                    Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                } else {
                    if (curSkill.point == 0) {
                        if (level == 1) {// Hoc skill moi
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                    level);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 23);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else { // neu chua hoc ma hoc lv cao
                            Skill skillNeed = SkillUtil
                                    .createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            Service.gI().sendThongBao(pl,
                                    "Vui lòng học " + skillNeed.template.name + " cấp " + skillNeed.point + " trước!");
                        }
                    } else {
                        if (curSkill.point + 1 == level) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                    level);
                            pl.BoughtSkill.add((int) item.template.id);
                            // System.out.println(curSkill.template.name + " - " + curSkill.point);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 62);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else {
                            Service.gI().sendThongBao(pl, "Vui lòng học " + curSkill.template.name + " cấp "
                                    + (curSkill.point + 1) + " trước!");
                        }
                    }
                    InventoryService.gI().sendItemBag(pl);
                }
            } else {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        }
    }

    private void HocSkill(Player pl, Item item) {
        Message msg = null;
        try {
            // Kiểm tra giới tính
            if (item.template.gender != pl.gender && item.template.gender != 3) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
                return;
            }

            // Lấy cấp từ tên item (đuôi tên là số)
            char lastChar = item.template.name.charAt(item.template.name.length() - 1);
            byte desiredLevel = Byte.parseByte(Character.toString(lastChar));

            // Lấy skill hiện tại và kiểm tra null
            Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
            if (curSkill == null) {
                Service.gI().sendThongBao(pl,
                        "Không thể sử dụng “" + item.template.name + "” để học kỹ năng!");
                return;
            }

            // Kiểm tra đã đạt tối đa chưa
            if (curSkill.point == 7) {
                Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                return;
            }

            // Học lần đầu
            if (curSkill.point == 0) {
                if (desiredLevel == 1) {
                    curSkill = SkillUtil.createSkill(
                            SkillUtil.getTempSkillSkillByItemID(item.template.id), desiredLevel);
                    SkillUtil.setSkill(pl, curSkill);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    msg = Service.gI().messageSubCommand((byte) 23);
                    msg.writer().writeShort(curSkill.skillId);
                    pl.sendMessage(msg);
                } else {
                    Skill needed = SkillUtil.createSkill(
                            SkillUtil.getTempSkillSkillByItemID(item.template.id), desiredLevel);
                    Service.gI().sendThongBao(pl,
                            "Vui lòng học " + needed.template.name + " cấp " + needed.point + " trước!");
                }
            } // Nâng cấp
            else {
                if (curSkill.point + 1 == desiredLevel) {
                    curSkill = SkillUtil.createSkill(
                            SkillUtil.getTempSkillSkillByItemID(item.template.id), desiredLevel);
                    SkillUtil.setSkill(pl, curSkill);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    msg = Service.gI().messageSubCommand((byte) 62);
                    msg.writer().writeShort(curSkill.skillId);
                    pl.sendMessage(msg);
                } else {
                    Service.gI().sendThongBao(pl,
                            "Vui lòng học " + curSkill.template.name + " cấp " + (curSkill.point + 1) + " trước!");
                }
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        } finally {
            InventoryService.gI().sendItemBag(pl);
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void learnSkillSuperNew(Player pl, Item item) {
        Message msg;
        try {
            if (item.template.gender == pl.gender || item.template.gender == 3) {
                byte level = SkillUtil.getLevelSkillByItemID(item.template.id);
                Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
                if (curSkill.point == 6) {
                    Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                } else {
                    if (curSkill.point == 0) {
                        if (level == 1) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                    level);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 23);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                            SkillService.gI().learSkillSpecial(pl, (byte) 30);
                        } else {
                            Skill skillNeed = SkillUtil
                                    .createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            if (level > 1) {
                                Item itemNew = ItemService.gI().createNewItem((short) (item.template.id - 1));
                                String name = itemNew.template.name;
                                String desiredName = name.substring(5);
                                Service.gI().sendThongBao(pl, "Vui lòng học " + desiredName + " trước!");
                            } else {
                                Service.gI().sendThongBao(pl, "Vui lòng học " + skillNeed.template.name + " trước!");
                            }
                        }
                    } else {
                        if (curSkill.point + 1 == level) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                    level);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 62);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else {
                            if (level > 1) {
                                Item itemNew = ItemService.gI().createNewItem((short) (item.template.id - 1));
                                String name = itemNew.template.name;
                                String desiredName = name.substring(5);
                                Service.gI().sendThongBao(pl, "Vui lòng học " + desiredName + " trước!");
                            } else {
                                Service.gI().sendThongBao(pl, "Vui lòng học " + curSkill.template.name + " trước!");
                            }
                        }
                    }
                    InventoryService.gI().sendItemBag(pl);
                }
            } else {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void useTDLT(Player pl, Item item) {
        if (pl.itemTime.isUseTDLT) {
            ItemTimeService.gI().turnOffTDLT(pl, item);
        } else {
            ItemTimeService.gI().turnOnTDLT(pl, item);
        }
    }

    private void usevevang(Player pl, Item item) {
        if (pl.itemTime.isUsevevang) {
            ItemTimeService.gI().turnOffvevang(pl, item);
        } else {
            ItemTimeService.gI().turnOnvevang(pl, item);
        }
    }

    private void usePorataGogeta(Player pl) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                pl.pet.fusionGogeta(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void usePorata(Player pl) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                pl.pet.fusion(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void usePorata2(Player pl, Item item) {
        if (pl.pet == null || pl.fusion.typeFusion == 4 || pl.fusion.typeFusion == 6 || pl.fusion.typeFusion == 10
                || pl.fusion.typeFusion == 12) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                for (ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == 50) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    } else if (io.optionTemplate.id == 77) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    } else if (io.optionTemplate.id == 103) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    }
                }
                pl.pet.fusion2(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void usePorata3(Player pl, Item item) {
        if (pl.pet == null || pl.fusion.typeFusion == 4 || pl.fusion.typeFusion == 6 || pl.fusion.typeFusion == 8) {
            Service.gI().sendThongBao(pl, "Dạng hợp thể không phù hợp");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                for (ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == 50) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    } else if (io.optionTemplate.id == 77) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    } else if (io.optionTemplate.id == 103) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    }
                }
                pl.pet.fusion3(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void usePorata4(Player pl, Item item) {
        if (pl.pet == null || pl.fusion.typeFusion == 4 || pl.fusion.typeFusion == 6 || pl.fusion.typeFusion == 8) {
            Service.gI().sendThongBao(pl, "Dạng hợp thể không phù hợp");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                for (ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == 50) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    } else if (io.optionTemplate.id == 77) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    } else if (io.optionTemplate.id == 103) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    }
                }
                pl.pet.fusion4(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void BinhNuoc(Player pl, Item item) {
        List<Player> bosses = pl.zone.getBosses();
        boolean checkSoi = false;

        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.XINBATO_1 && !pl.isDie()) {
                    checkSoi = true;
                }
            }
        }

        if (!checkSoi) {
            Service.gI().sendThongBao(pl, "");
            return;
        }

        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.XINBATO_1) {
                    Boss xinbato = (Boss) bossPlayer;
                    if (xinbato != null) {
                        if (((Xinbato) xinbato).Check()) {
                            continue;
                        } else {
                            ((Xinbato) xinbato).NhatXuong1();
                            Service.gI().chat(xinbato, "Cảm ơn" + pl.name + " đã cho ta bình nước");
                        }

                        ItemMap itemMap = null;
                        int x = pl.location.x;
                        if (x < 0 || x >= pl.zone.map.mapWidth) {
                            return;
                        }
                        int y = pl.zone.map.yPhysicInTop(x, pl.location.y - 24);
                        itemMap = new ItemMap(pl.zone, 456, 99, x, y, pl.id);
                        itemMap.isPickedUp = true;
                        itemMap.createTime -= 23000;
                        if (itemMap != null) {
                            Service.gI().dropItemMap(pl.zone, itemMap);
                        }

                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                        InventoryService.gI().sendItemBag(pl);

                        if (Util.nextInt(4) < 3) { // 75% cơ hội
                            int rand = Util.nextInt(0, 6); // Random từ 0 đến 6
                            short idItem = (short) (rand + 441); // Item 441 + rand
                            Item it = ItemService.gI().createNewItem(idItem);
                            it.itemOptions.add(new Item.ItemOption(95 + rand, (rand == 3 || rand == 4) ? 3 : 5));

                            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                                InventoryService.gI().addItemBag(pl, it);
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                            } else {
                                Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                            }
                        } else {
                            short idItem = 459; // Item 459
                            Item it = ItemService.gI().createNewItem(idItem);
                            it.itemOptions.add(new Item.ItemOption(112, 80));
                            it.itemOptions.add(new Item.ItemOption(93, 90));
                            it.itemOptions.add(new Item.ItemOption(20, Util.nextInt(10000)));
                            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                                InventoryService.gI().addItemBag(pl, it);
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                            } else {
                                Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                            }
                        }

                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {
                            // Handle exception
                        }

                        ItemMapService.gI().removeItemMapAndSendClient(itemMap);
                        ((Xinbato) xinbato).leaveMapNew();
                    }

                }
            }

            InventoryService.gI().sendItemBag(pl);
        }

    }

    private void CucXuong(Player pl, Item item) {
        List<Player> bosses = pl.zone.getBosses();
        boolean checkSoi = false;

        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.SOI_HEC_QUYN_1 && !pl.isDie()) {
                    checkSoi = true;
                }
            }
        }

        if (!checkSoi) {
            Service.gI().sendThongBao(pl, "Không tìm thấy Sói hẹc quyn");
            return;
        }

        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.SOI_HEC_QUYN_1) {
                    Boss soihecQuyn = (Boss) bossPlayer;
                    if (soihecQuyn != null) {
                        if (((SoiHecQuyn) soihecQuyn).KiemTraNhatXuong()) {
                            Service.gI().sendThongBao(pl, "Sói đã no rồi");
                            continue;
                        } else {
                            ((SoiHecQuyn) soihecQuyn).NhatXuong();
                            Service.gI().chat(soihecQuyn, "Ê, Cục xương ngon quá");
                        }

                        ItemMap itemMap = null;
                        int x = pl.location.x;
                        if (x < 0 || x >= pl.zone.map.mapWidth) {
                            return;
                        }
                        int y = pl.zone.map.yPhysicInTop(x, pl.location.y - 24);
                        itemMap = new ItemMap(pl.zone, 460, 1, x, y, pl.id);
                        itemMap.isPickedUp = true;
                        itemMap.createTime -= 23000;
                        if (itemMap != null) {
                            Service.gI().dropItemMap(pl.zone, itemMap);
                        }

                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                        InventoryService.gI().sendItemBag(pl);

                        if (Util.nextInt(4) < 3) { // 75% cơ hội
                            int rand = Util.nextInt(0, 6); // Random từ 0 đến 6
                            short idItem = (short) (rand + 441); // Item 441 + rand
                            Item it = ItemService.gI().createNewItem(idItem);
                            it.itemOptions.add(new Item.ItemOption(95 + rand, (rand == 3 || rand == 4) ? 3 : 5));

                            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                                InventoryService.gI().addItemBag(pl, it);
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                            } else {
                                Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                            }
                        } else {
                            short idItem = 459; // Item 459
                            Item it = ItemService.gI().createNewItem(idItem);
                            it.itemOptions.add(new Item.ItemOption(112, 80));
                            it.itemOptions.add(new Item.ItemOption(93, 90));
                            it.itemOptions.add(new Item.ItemOption(20, Util.nextInt(10000)));
                            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                                InventoryService.gI().addItemBag(pl, it);
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                            } else {
                                Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                            }
                        }

                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {
                            // Handle exception
                        }

                        ItemMapService.gI().removeItemMapAndSendClient(itemMap);
                        ((SoiHecQuyn) soihecQuyn).leaveMapNew();
                    }

                }
            }

            InventoryService.gI().sendItemBag(pl);
        }

    }

    private void openCapsuleUI(Player pl) {
        pl.iDMark.setTypeChangeMap(ConstMap.CHANGE_CAPSULE);
        ChangeMapService.gI().openChangeMapTab(pl);
    }

    private static void open1798(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item1;
            if (Util.nextInt(100) < 50) {
                item1 = ItemService.gI().createNewItem((short) 1799);
                item1.itemOptions.add(new ItemOption(50, 18));
                item1.itemOptions.add(new ItemOption(77, 7));
                item1.itemOptions.add(new ItemOption(103, 7));
                item1.itemOptions.add(new ItemOption(5, 11));
                item1.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item1.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item1);
            } else if (Util.nextInt(100) < 50) {
                item1 = ItemService.gI().createNewItem((short) 1800);
                item1.itemOptions.add(new ItemOption(50, 18));
                item1.itemOptions.add(new ItemOption(5, 8));
                item1.itemOptions.add(new ItemOption(14, 5));
                item1.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item1.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item1);
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1788(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            if (Util.nextInt(100) < 20) {
                item2 = ItemService.gI().createNewItem((short) 1790);
                item2.itemOptions.add(new ItemOption(50, 20));
                item2.itemOptions.add(new ItemOption(77, 20));
                item2.itemOptions.add(new ItemOption(103, 20));
                item2.itemOptions.add(new ItemOption(108, 12));
                item2.itemOptions.add(new ItemOption(94, 12));
                item2.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item2);
            } else if (Util.nextInt(100) < 20) {
                item2 = ItemService.gI().createNewItem((short) 1791);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(103, 18));
                item2.itemOptions.add(new ItemOption(108, 10));
                item2.itemOptions.add(new ItemOption(94, 10));
                item2.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item2);
            } else if (Util.nextInt(100) < 20) {
                item2 = ItemService.gI().createNewItem((short) 1792);
                item2.itemOptions.add(new ItemOption(50, 17));
                item2.itemOptions.add(new ItemOption(77, 17));
                item2.itemOptions.add(new ItemOption(103, 17));
                item2.itemOptions.add(new ItemOption(108, 5));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item2);
            } else if (Util.nextInt(100) < 20) {
                item2 = ItemService.gI().createNewItem((short) 1793);
                item2.itemOptions.add(new ItemOption(50, 16));
                item2.itemOptions.add(new ItemOption(77, 16));
                item2.itemOptions.add(new ItemOption(103, 16));
                item2.itemOptions.add(new ItemOption(108, 5));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item2);
            } else if (Util.nextInt(100) < 20) {
                item2 = ItemService.gI().createNewItem((short) 1794);
                item2.itemOptions.add(new ItemOption(50, 15));
                item2.itemOptions.add(new ItemOption(77, 15));
                item2.itemOptions.add(new ItemOption(103, 15));
                item2.itemOptions.add(new ItemOption(108, 5));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item2);
            } else if (Util.nextInt(100) < 20) {
                item2 = ItemService.gI().createNewItem((short) 1795);
                item2.itemOptions.add(new ItemOption(50, 13));
                item2.itemOptions.add(new ItemOption(77, 13));
                item2.itemOptions.add(new ItemOption(103, 13));
                item2.itemOptions.add(new ItemOption(108, 5));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item2);
            } else if (Util.nextInt(100) < 20) {
                item2 = ItemService.gI().createNewItem((short) 1796);
                item2.itemOptions.add(new ItemOption(50, 12));
                item2.itemOptions.add(new ItemOption(77, 12));
                item2.itemOptions.add(new ItemOption(103, 12));
                item2.itemOptions.add(new ItemOption(108, 5));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item2);
            } else if (Util.nextInt(100) < 20) {
                item2 = ItemService.gI().createNewItem((short) 1797);
                item2.itemOptions.add(new ItemOption(50, 22));
                item2.itemOptions.add(new ItemOption(77, 22));
                item2.itemOptions.add(new ItemOption(103, 22));
                item2.itemOptions.add(new ItemOption(108, 10));
                item2.itemOptions.add(new ItemOption(94, 10));
                item2.itemOptions.add(new ItemOption(30, 0));
                if (Util.nextInt(100) < 99) {
                    item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(pl, item2);
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void open627(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 7) {
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            short baseAo = 1;
            short baseQuan = 7;
            short baseGang = 22;
            short baseGiay = 28;
            short baseCaiTrang = 1470;
            short genderAo = (pl.gender == 0) ? -1 : ((pl.gender == 2) ? 1 : (short) 0);
            short genderQuan = (pl.gender == 0) ? -1 : ((pl.gender == 2) ? 1 : (short) 0);
            short genderGang = (pl.gender == 0) ? -1 : ((pl.gender == 2) ? 1 : (short) 0);
            short genderGiay = (pl.gender == 0) ? -1 : ((pl.gender == 2) ? 1 : (short) 0);
            short genderCaiTrang = (pl.gender == 0) ? -1 : ((pl.gender == 2) ? 1 : (short) 0);
            Item ao = ItemService.gI().createNewItem((short) (baseAo + genderAo));
            Item quan = ItemService.gI().createNewItem((short) (baseQuan + genderQuan));
            Item gang = ItemService.gI().createNewItem((short) (baseGang + genderGang));
            Item giay = ItemService.gI().createNewItem((short) (baseGiay + genderGiay));
            Item nhan = ItemService.gI().createNewItem((short) 12);
            Item caiTrang = ItemService.gI().createNewItem((short) (baseCaiTrang + genderCaiTrang));
            ao.itemOptions.add(new ItemOption(47, 3));
            quan.itemOptions.add(new ItemOption(6, 30));
            gang.itemOptions.add(new ItemOption(0, 4));
            giay.itemOptions.add(new ItemOption(7, 10));
            nhan.itemOptions.add(new ItemOption(14, 1));
            ao.itemOptions.add(new ItemOption(107, 3));
            quan.itemOptions.add(new ItemOption(107, 3));
            gang.itemOptions.add(new ItemOption(107, 3));
            giay.itemOptions.add(new ItemOption(107, 3));
            nhan.itemOptions.add(new ItemOption(107, 3));
            caiTrang.itemOptions.add(new ItemOption(50, 25));
            caiTrang.itemOptions.add(new ItemOption(77, 25));
            caiTrang.itemOptions.add(new ItemOption(103, 25));
            caiTrang.itemOptions.add(new ItemOption(101, 100));
            caiTrang.itemOptions.add(new ItemOption(93, 2));
            InventoryService.gI().addItemBag(pl, ao);
            InventoryService.gI().addItemBag(pl, quan);
            InventoryService.gI().addItemBag(pl, gang);
            InventoryService.gI().addItemBag(pl, giay);
            InventoryService.gI().addItemBag(pl, nhan);
            InventoryService.gI().addItemBag(pl, caiTrang);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1786(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            int rand = Util.nextInt(1000); // sử dụng hệ số 1000 để tăng độ chính xác

            if (rand < 15) { // 0.5% - cực hiếm
                item2 = ItemService.gI().createNewItem((short) 1778);
                item2.itemOptions.add(new ItemOption(77, 22));
                item2.itemOptions.add(new ItemOption(50, 22));
                item2.itemOptions.add(new ItemOption(94, 8));
                item2.itemOptions.add(new ItemOption(5, 11));
                item2.itemOptions.add(new ItemOption(14, 8));
                item2.itemOptions.add(new ItemOption(106, 0));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 35) { // 1% - cực hiếm
                item2 = ItemService.gI().createNewItem((short) 1779);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(103, 18));
                item2.itemOptions.add(new ItemOption(108, 10));
                item2.itemOptions.add(new ItemOption(94, 10));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 65) { // 5% - hiếm hơn
                item2 = ItemService.gI().createNewItem((short) 1780);
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(108, 7));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 135) { // 7% - hiếm hơn
                item2 = ItemService.gI().createNewItem((short) 1781);
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(5, 7));
                item2.itemOptions.add(new ItemOption(14, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 385) { // 25% - tỉ lệ cao hơn
                item2 = ItemService.gI().createNewItem((short) 1782);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(94, 15));
                item2.itemOptions.add(new ItemOption(108, 7));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 685) { // 30% - tỉ lệ cao hơn
                item2 = ItemService.gI().createNewItem((short) 1783);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(5, 7));
                item2.itemOptions.add(new ItemOption(14, 5));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (rand < 985) { // 30% - tỉ lệ cao hơn
                item2 = ItemService.gI().createNewItem((short) 1784);
                item2.itemOptions.add(new ItemOption(77, 16));
                item2.itemOptions.add(new ItemOption(50, 16));
                item2.itemOptions.add(new ItemOption(103, 16));
                item2.itemOptions.add(new ItemOption(236, 20));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                Service.gI().sendThongBao(pl, "Bạn không nhận được vật phẩm nào.");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                return;
            }
            // Thêm option ngẫu nhiên
            if (Util.nextInt(100) < 99) {
                item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
            }

            InventoryService.gI().addItemBag(pl, item2);
            Service.gI().sendThongBao(pl, "Bạn nhận được " + item2.template.name);
            InventoryService.gI().sendItemBag(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1787(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            Item it = ItemService.gI().createNewItem((short) 1785);
            InventoryService.gI().addItemBag(pl, it);
            Service.gI().sendThongBao(pl, "Bạn Nhận Được Quả Trứng Rồng Nhí");
            icon[1] = 15127;
            InventoryService.gI().subQuantityItemsBag(pl, item, 99);
            InventoryService.gI().sendItemBag(pl);
            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void openRuongNgocRong(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            // Số ngẫu nhiên từ 0 đến 100 để quyết định tỷ lệ
            int random = Util.nextInt(0, 100);

            int itemwhis;

            // 85% xuất hiện item 20, 19, 18, 17
            if (random < 85) {
                int[] itemList = { 20, 19, 18, 17 }; // Các item có tỉ lệ xuất hiện 85%
                itemwhis = itemList[Util.nextInt(0, itemList.length - 1)];
            } // 10% xuất hiện item 16
            else if (random < 95) {
                itemwhis = 16; // Item 16 có tỉ lệ 10%
            } // 5% xuất hiện item 14 hoặc 15
            else {
                itemwhis = Util.nextInt(14, 15); // Item 14 hoặc 15 có tỉ lệ 5%
            }

            // Tạo vật phẩm mới từ ID đã chọn
            Item it = ItemService.gI().createNewItem((short) itemwhis);

            // Kiểm tra nếu người chơi có item 1561 (chìa khóa)
            Item item1561 = InventoryService.gI().findItem(pl.inventory.itemsBag, 1561);
            if (item1561 != null) {
                // Trừ vật phẩm và thêm vật phẩm mới vào túi đồ
                InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
                InventoryService.gI().subQuantityItemsBag(pl, item1561, 1); // Trừ 1 Item Box
                InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
                InventoryService.gI().sendItemBag(pl); // Gửi cập nhật túi đồ
                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Bạn không có chìa khoá vàng");
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void choseMapCapsule(Player pl, int index) {

        if (pl.idNRNM != -1) {
            Service.gI().sendThongBao(pl, "Không thể mang ngọc rồng này lên Phi thuyền");
            Service.gI().hideWaitDialog(pl);
            return;
        }

        int zoneId = -1;
        if (index > pl.mapCapsule.size() - 1 || index < 0) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            Service.gI().hideWaitDialog(pl);
            return;
        }
        Zone zoneChose = pl.mapCapsule.get(index);
        // Kiểm tra số lượng người trong khu

        if (zoneChose.getNumOfPlayers() > 25
                || MapService.gI().isMapDoanhTrai(zoneChose.map.mapId)
                || MapService.gI().isMapMaBu(zoneChose.map.mapId)
                || MapService.gI().isMapHuyDiet(zoneChose.map.mapId)) {
            Service.gI().sendThongBao(pl, "Hiện tại không thể vào được khu!");
            return;
        }
        if (index != 0 || zoneChose.map.mapId == 21
                || zoneChose.map.mapId == 22
                || zoneChose.map.mapId == 23) {
            pl.mapBeforeCapsule = pl.zone;
        } else {
            zoneId = pl.mapBeforeCapsule != null ? pl.mapBeforeCapsule.zoneId : -1;
            pl.mapBeforeCapsule = null;
        }
        pl.changeMapVIP = true;
        ChangeMapService.gI().changeMapBySpaceShip(pl, pl.mapCapsule.get(index).map.mapId, zoneId, -1);
    }

    public void eatPea(Player player) {
        if (!Util.canDoWithTime(player.lastTimeEatPea, 2000)) {
            return;
        }
        player.lastTimeEatPea = System.currentTimeMillis();

        // reset ngày
        long nowDay = System.currentTimeMillis() / 86400000;
        if (player.pea_last_day != nowDay) {
            player.pea_today_count = 0;
            player.pea_last_day = nowDay;
        }

        // Đã xóa giới hạn 100 đậu/ngày

        // tìm đậu
        Item pea = null;
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.type == 6) {
                pea = item;
                break;
            }
        }
        if (pea == null)
            return;

        /*
         * =========================
         * HỒI PHỤC GỐC (GIỮ NGUYÊN)
         * =========================
         */

        long hpKiHoiPhuc = 0;
        int lvPea = getPeaLevel(pea);

        for (ItemOption io : pea.itemOptions) {
            if (io.optionTemplate.id == 2) {
                hpKiHoiPhuc = io.param * 1000;
                break;
            }
            if (io.optionTemplate.id == 48) {
                hpKiHoiPhuc = io.param;
                break;
            }
        }

        // Hồi phục sẽ xử lý sau khi cập nhật bonus chỉ số để HP/KI max mới có hiệu lực ngay.

        // hồi cho pet
        if (player.pet != null && player.zone.equals(player.pet.zone) && !player.pet.isDie()) {
            int statima = 100 * lvPea;
            player.pet.nPoint.stamina += statima;
            if (player.pet.nPoint.stamina > player.pet.nPoint.maxStamina) {
                player.pet.nPoint.stamina = player.pet.nPoint.maxStamina;
            }

            player.pet.nPoint.setHp(Util.maxIntValue(player.pet.nPoint.hp + hpKiHoiPhuc));
            player.pet.nPoint.setMp(Util.maxIntValue(player.pet.nPoint.mp + hpKiHoiPhuc));
            Service.gI().sendInfoPlayerEatPea(player.pet);
            Service.gI().chatJustForMe(player, player.pet, "Cám ơn sư phụ");
        }

        /*
         * =========================
         * HỆ THỐNG CHỈ SỐ + LƯU DATA
         * =========================
         */

        player.pea_today_count++;
        player.pea_use_count++;

        boolean canReceiveBonus = player.pea_today_count <= PEA_BONUS_DAILY_LIMIT
                && (player.pea_bonus_sd < PEA_BONUS_SD_LIMIT
                        || player.pea_bonus_hp < PEA_BONUS_HP_LIMIT
                        || player.pea_bonus_ki < PEA_BONUS_KI_LIMIT);

        // trừ item trước khi cộng bonus để tránh spam/dup khi lỗi gửi packet
        InventoryService.gI().subQuantityItemsBag(player, pea, 1);
        InventoryService.gI().sendItemBag(player);

        // cộng chỉ số theo vòng SD -> HP -> KI, có cap để chống tool spam farm vô hạn
        String bonusText = null;
        if (canReceiveBonus) {
            if (player.pea_cycle == 0 && player.pea_bonus_sd < PEA_BONUS_SD_LIMIT) {
                int value = Math.min(Util.nextInt(1, 10), PEA_BONUS_SD_LIMIT - player.pea_bonus_sd);
                player.pea_bonus_sd += value;
                bonusText = "Sức đánh gốc +" + value;
            } else if (player.pea_cycle == 1 && player.pea_bonus_hp < PEA_BONUS_HP_LIMIT) {
                int value = Math.min(Util.nextInt(10, 100), PEA_BONUS_HP_LIMIT - player.pea_bonus_hp);
                player.pea_bonus_hp += value;
                bonusText = "HP gốc +" + value;
            } else if (player.pea_bonus_ki < PEA_BONUS_KI_LIMIT) {
                int value = Math.min(Util.nextInt(10, 100), PEA_BONUS_KI_LIMIT - player.pea_bonus_ki);
                player.pea_bonus_ki += value;
                bonusText = "KI gốc +" + value;
            }
        }

        if (bonusText != null) {
            player.pea_cycle++;
            if (player.pea_cycle > 2) {
                player.pea_cycle = 0;
            }
        }

        // mốc thưởng
        int[] milestone = { 100, 200, 300, 400, 500 };
        for (int m : milestone) {
            if (player.pea_use_count >= m && player.pea_milestone < m) {
                player.pea_milestone = m;
            }
        }

        // cập nhật lại chỉ số để pea_bonus_* ăn vào hpMax/mpMax/dame ngay lập tức
        Service.gI().point(player);
        PlayerService.gI().sendInfoHpMpMoney(player);

        // hồi phục sau khi recalculation để có hiệu lực theo max mới
        player.nPoint.setHp(Util.maxIntValue(player.nPoint.hp + hpKiHoiPhuc));
        player.nPoint.setMp(Util.maxIntValue(player.nPoint.mp + hpKiHoiPhuc));
        PlayerService.gI().sendInfoHpMp(player);
        Service.gI().sendInfoPlayerEatPea(player);
        if (bonusText != null) {
            Service.gI().sendThongBao(player, bonusText);
        } else if (player.pea_today_count == PEA_BONUS_DAILY_LIMIT + 1) {
            Service.gI().sendThongBao(player,
                    "Hôm nay bạn đã đạt giới hạn cộng chỉ số từ Đậu thần, vẫn có thể ăn để hồi HP/KI");
        }

        PlayerDAO.updatePlayer(player);
    }

    private int getPeaLevel(Item pea) {
        try {
            String name = pea.template.name;
            return Integer.parseInt(name.substring(name.lastIndexOf(' ') + 1));
        } catch (Exception e) {
            return 1;
        }
    }

    private void upSkillPet(Player pl, Item item) {
        if (pl.pet == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        try {
            switch (item.template.id) {
                case 402: // skill 1
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 0)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 403: // skill 2
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 1)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 404: // skill 3
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 2)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 759: // skill 4
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 3)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;

            }

        } catch (Exception e) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    private void ItemManhGiay(Player pl, Item item) {
        if (pl.winSTT && !Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            Service.gI().sendThongBao(pl, "Hãy gặp thần mèo Karin để sử dụng");
            return;
        } else if (pl.winSTT && Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            pl.winSTT = false;
            pl.callBossPocolo = false;
            pl.zoneSieuThanhThuy = null;
        }
        NpcService.gI().createMenuConMeo(pl, item.template.id, 564,
                "Đây chính là dấu hiệu riêng của...\nĐại Ma Vương Pôcôlô\nĐó là một tên quỷ dữ đội lốt người, một kẻ đại gian ác\ncó sức mạnh vô địch và lòng tham không đáy...\nĐối phó với hắn không phải dễ\nCon có chắc chắn muốn tìm hắn không?",
                "Đồng ý", "Từ chối");
    }

    private void ItemSieuThanThuy(Player pl, Item item) {
        long tnsm = 5_000_000;
        int n = 0;
        switch (item.template.id) {
            case 727:
                n = 2;
                break;
            case 728:
                n = 10;
                break;
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBag(pl);
        if (Util.isTrue(50, 100)) {
            Service.gI().sendThongBao(pl, "Bạn đã bị chết vì độc của thuốc tăng lực siêu thần thủy.");
            pl.setDie();
        } else {
            for (int i = 0; i < n; i++) {
                Service.gI().addSMTN(pl, (byte) 2, tnsm, true);
            }
        }
    }

    private void Cadicvip(Player pl, Item item) {
        try {

            List<Short> itemList = Arrays.asList((short) 1759, (short) 1760, (short) 1761, (short) 1762, (short) 1763,
                    (short) 1764);

            short selectedItemId = itemList.get(Util.nextInt(0, itemList.size()));

            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);

            switch (selectedItemId) {
                case 1759: // Item 1759
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 21)); // Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 21)); // HP
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 21)); // KI
                    selectedItem.itemOptions.add(new Item.ItemOption(210, 1)); // Tùy chọn khác
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(1, 14);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    break;

                case 1760: // Item 1760
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 22)); // Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 22)); // HP
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 22)); // KI
                    selectedItem.itemOptions.add(new Item.ItemOption(210, 2)); // Tùy chọn khác
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(1, 14);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    break;

                case 1761: // Item 1761
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 23)); // Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 23)); // HP
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 23)); // KI
                    selectedItem.itemOptions.add(new Item.ItemOption(210, 3)); // Tùy chọn khác
                    // Tạo ID 1743 giống 1744
                    Item.ItemOption optionFor1744 = new Item.ItemOption(93, 30); // Chỉ số 1743 = 1744
                    selectedItem.itemOptions.add(optionFor1744);
                    break;

                case 1762: // Item 1762
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 23)); // Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 23)); // HP
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 23)); // KI
                    selectedItem.itemOptions.add(new Item.ItemOption(210, 3)); // Tùy chọn khác
                    // Tạo ID 1743 giống 1744
                    Item.ItemOption optionFor1743 = new Item.ItemOption(93, 30); // Chỉ số 1743 = 1744
                    selectedItem.itemOptions.add(optionFor1743);
                    break;

                case 1763: // Item 1763
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 25)); // Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 25)); // HP
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 25)); // KI
                    selectedItem.itemOptions.add(new Item.ItemOption(210, 3));
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(1, 14);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    } // Tùy chọn khác
                    break;

                case 1764: // Item 1764
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 27)); // Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 27)); // HP
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 27)); // KI
                    selectedItem.itemOptions.add(new Item.ItemOption(210, 4)); // Tùy chọn khác
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(1, 14);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    break;

            }

            // Kiểm tra hành trang của người chơi có đủ chỗ không
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                // Giảm số lượng item cũ trong hành trang
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                // Thêm vật phẩm mới vào hành trang
                InventoryService.gI().addItemBag(pl, selectedItem);
                // Gửi thông báo cho người chơi
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                // Thông báo nếu hành trang đầy
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }
        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm Cadicvip: " + e.getMessage());
        }
    }

    private void RuongRongThan(Player pl, Item item) {
        try {

            List<Short> itemList = Arrays.asList((short) 1895, (short) 1902, (short) 1903, (short) 1904);

            short selectedItemId = itemList.get(Util.nextInt(0, itemList.size()));

            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);

            switch (selectedItemId) {
                case 1895: // Item rồng đen
                    selectedItem.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 15)));// Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(95, Util.nextInt(10, 15)));// hút hp
                    selectedItem.itemOptions.add(new Item.ItemOption(85, 0));// bay và phục hồi ki
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(15, 30);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    selectedItem.itemOptions.add(new Item.ItemOption(30, 0)); // không thể giao dịch
                    break;
                case 1902: // Item Thú cưỡi rồng Thiêng
                    selectedItem.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 15)));// Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(95, Util.nextInt(10, 15)));// hút hp
                    selectedItem.itemOptions.add(new Item.ItemOption(85, 0));// bay và phục hồi ki
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(15, 30);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    selectedItem.itemOptions.add(new Item.ItemOption(30, 0)); // không thể giao dịch
                    break;

                case 1903: // Item Thú cưỡi rồng Băng
                    selectedItem.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 15)));// Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(95, Util.nextInt(10, 15)));// hút hp
                    selectedItem.itemOptions.add(new Item.ItemOption(85, 0));// bay và phục hồi ki
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(15, 30);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    selectedItem.itemOptions.add(new Item.ItemOption(30, 0)); // không thể giao dịch
                    break;

                case 1904: // Item Thú cưỡi rồng Vô Cực
                    selectedItem.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 15)));// Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(95, Util.nextInt(10, 15)));// hút hp
                    selectedItem.itemOptions.add(new Item.ItemOption(85, 0));// bay và phục hồi ki
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(15, 30);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    selectedItem.itemOptions.add(new Item.ItemOption(30, 0)); // không thể giao dịch
                    break;

            }

            // Kiểm tra hành trang của người chơi có đủ chỗ không
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                // Giảm số lượng item cũ trong hành trang
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                // Thêm vật phẩm mới vào hành trang
                InventoryService.gI().addItemBag(pl, selectedItem);
                // Gửi thông báo cho người chơi
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                // Thông báo nếu hành trang đầy
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }
        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm Cadicvip: " + e.getMessage());
        }
    }

    private void Gokudayvip(Player pl, Item item) {
        try {

            List<Short> itemList = Arrays.asList((short) 1588, (short) 1589, (short) 1590, (short) 1593, (short) 955);

            short selectedItemId = itemList.get(Util.nextInt(0, itemList.size()));

            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);
            switch (selectedItemId) {
                case 1588: // Item 1589
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 21));
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 21));
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 21));
                    selectedItem.itemOptions.add(new Item.ItemOption(210, 1));
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(1, 14);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    break;
                case 1589: // Item 1589
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 22));
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 22));
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 22));
                    selectedItem.itemOptions.add(new Item.ItemOption(210, 1));
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(1, 14);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    break;
                case 1590: // Item 1590
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 27));
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 27));
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 27));
                    selectedItem.itemOptions.add(new Item.ItemOption(210, 4));
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(1, 14);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    break;
                case 1593: // Item 1593 ok
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 25));
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 25));
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 25));
                    selectedItem.itemOptions.add(new Item.ItemOption(210, Util.nextInt(3, 4)));
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(1, 14);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    break;
                case 1595: // Item 1595
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 23));
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 23));
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 23));
                    selectedItem.itemOptions.add(new Item.ItemOption(210, Util.nextInt(2, 3)));
                    if (Util.nextInt(0, 100) < 99) {
                        int randValue = Util.nextInt(1, 14);
                        selectedItem.itemOptions.add(new Item.ItemOption(93, randValue));
                    }
                    break;
            }
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }
        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm Gokudayvip: " + e.getMessage());
        }
    }

    private void hop2010(Player pl, Item item) {
        try {
            if (pl.thiepchucvip < 0) {
                pl.thiepchucvip = 0;
            }

            List<Short> itemListMain = Arrays.asList(
                    (short) 1961, (short) 1602, (short) 680, (short) 819,
                    (short) 914, (short) 977, (short) 1041, (short) 1042,
                    (short) 1208, (short) 1209, (short) 1210, (short) 1235,
                    (short) 1567, (short) 1476, (short) 1557, (short) 860, (short) 1772);

            List<Short> itemListBonus = new ArrayList<>();
            itemListBonus.add((short) 956);
            for (short i = 1074; i <= 1083; i++) {
                itemListBonus.add(i);
            }
            for (short i = 1150; i <= 1154; i++) {
                itemListBonus.add(i);
            }
            for (short i = 381; i <= 385; i++) {
                itemListBonus.add(i);
            }

            int roll = Util.nextInt(0, 100);
            boolean isMain = roll < 10;
            boolean isGoldReward = roll >= 10 && roll < 70;

            if (isGoldReward) {
                int goldAmount = Util.nextInt(100_000, 1_500_000);
                pl.inventory.gold += goldAmount;
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
                    pl.inventory.gold = Inventory.LIMIT_GOLD;
                }

                Service.gI().sendMoney(pl);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                Service.gI().sendThongBao(pl, "Bạn nhận được " + Util.numberFormat(goldAmount) + " vàng");
                if (goldAmount >= 1_000_000) {
                    Service.gI().sendThongBaoAllPlayer(
                            pl.name + " vừa mở Thiệp Chúc VIP nhận " + Util.numberFormat(goldAmount) + " vàng");
                }

                pl.hopqua2010 += 5;
                Service.gI().sendThongBao(pl, "Bạn được cộng 5 điểm Thiệp Chúc VIP (Tổng: " + pl.hopqua2010 + ")");
                return;
            }

            short selectedItemId = isMain
                    ? itemListMain.get(Util.nextInt(0, itemListMain.size()))
                    : itemListBonus.get(Util.nextInt(0, itemListBonus.size()));

            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);

            if (isMain) {
                int random93 = Util.nextInt(1, 14);
                int atk = Util.nextInt(20, 28);
                int hp = Util.nextInt(20, 26);
                int ki = Util.nextInt(22, 29);
                int crit = Util.nextInt(5, 9);

                if (selectedItemId == 1961) {
                    selectedItem.itemOptions.add(new Item.ItemOption(50, 24)); // Sức đánh
                    selectedItem.itemOptions.add(new Item.ItemOption(77, 23)); // HP
                    selectedItem.itemOptions.add(new Item.ItemOption(103, 29)); // KI
                    selectedItem.itemOptions.add(new Item.ItemOption(95, 100)); // May mắn
                    selectedItem.itemOptions.add(new Item.ItemOption(93, 15)); // Hạn 15 ngày
                } else {
                    selectedItem.itemOptions.add(new Item.ItemOption(50, atk)); // Sức đánh %
                    selectedItem.itemOptions.add(new Item.ItemOption(77, hp)); // HP %
                    selectedItem.itemOptions.add(new Item.ItemOption(103, ki)); // KI %
                    selectedItem.itemOptions.add(new Item.ItemOption(211, crit)); // Chí mạng %
                    selectedItem.itemOptions.add(new Item.ItemOption(93, 15)); // Hạn sử dụng
                }

                if (selectedItemId == 1961) {
                    Service.gI().sendThongBaoAllPlayer(
                            pl.name + " vừa nhận được cải trang hiếm " + selectedItem.template.name);
                }
            }

            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm");
            }

            pl.hopqua2010 += 5;
            Service.gI().sendThongBao(pl, "Bạn được cộng thêm 5 điểm Thiệp Chúc VIP (Tổng: " + pl.hopqua2010 + ")");

        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm thiepchucvip: " + e.getMessage());
        }
    }

    private void thiepchucvip(Player pl, Item item) {
        try {
            // Danh sách item CHÍNH (có chỉ số)
            List<Short> itemListMain = Arrays.asList(
                    (short) 1503, (short) 1504, (short) 1512,
                    (short) 1884, (short) 1960, (short) 1961);

            // Danh sách item PHỤ (không có chỉ số)
            List<Short> itemListBonus = new ArrayList<>();
            itemListBonus.add((short) 956);
            for (short i = 1074; i <= 1083; i++) {
                itemListBonus.add(i);
            }
            for (short i = 1150; i <= 1154; i++) {
                itemListBonus.add(i);
            }
            for (short i = 381; i <= 385; i++) {
                itemListBonus.add(i);
            }

            // Xác định phần thưởng
            int roll = Util.nextInt(0, 100);
            boolean isMain = roll < 10; // 10% ra item chính
            boolean isGoldReward = roll >= 10 && roll < 70; // 60% ra vàng
            boolean isBonus = roll >= 70; // 30% ra phụ

            // 🔹 Nếu là vàng
            if (isGoldReward) {
                int goldAmount = Util.nextInt(100_000, 1_500_000);
                pl.inventory.gold += goldAmount;
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
                    pl.inventory.gold = Inventory.LIMIT_GOLD;
                }

                Service.gI().sendMoney(pl);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                Service.gI().sendThongBao(pl, "Bạn nhận được " + Util.numberFormat(goldAmount) + " vàng!");

                if (goldAmount >= 1_000_000) {
                    Service.gI().sendThongBaoAllPlayer(
                            "" + pl.name + " vừa mở thiệp chúc VIP nhận " + Util.numberFormat(goldAmount) + " vàng!");
                }
                return;
            }

            // 🔹 Nếu là item (chính hoặc phụ)
            short selectedItemId = isMain
                    ? itemListMain.get(Util.nextInt(0, itemListMain.size()))
                    : itemListBonus.get(Util.nextInt(0, itemListBonus.size()));

            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);

            // Nếu là item CHÍNH -> thêm chỉ số đặc biệt
            if (isMain) {
                int random93 = Util.nextInt(1, 14);

                switch (selectedItemId) {
                    case 1503:
                    case 1504:
                        selectedItem.itemOptions.add(new Item.ItemOption(50, Util.nextInt(15, 21)));
                        selectedItem.itemOptions.add(new Item.ItemOption(77, Util.nextInt(15, 21)));
                        selectedItem.itemOptions.add(new Item.ItemOption(103, Util.nextInt(15, 21)));
                        selectedItem.itemOptions.add(new Item.ItemOption(210, 1));
                        break;

                    case 1512:
                        selectedItem.itemOptions.add(new Item.ItemOption(50, Util.nextInt(17, 23)));
                        selectedItem.itemOptions.add(new Item.ItemOption(77, Util.nextInt(17, 23)));
                        selectedItem.itemOptions.add(new Item.ItemOption(103, Util.nextInt(17, 23)));
                        selectedItem.itemOptions.add(new Item.ItemOption(210, 4));
                        break;

                    case 1884:
                        selectedItem.itemOptions.add(new Item.ItemOption(50, Util.nextInt(20, 24)));
                        selectedItem.itemOptions.add(new Item.ItemOption(77, Util.nextInt(20, 24)));
                        selectedItem.itemOptions.add(new Item.ItemOption(103, Util.nextInt(20, 24)));
                        selectedItem.itemOptions.add(new Item.ItemOption(210, Util.nextInt(3, 4)));
                        break;

                    // 🌸 Cải trang Chi Chi Áo Cưới Mới (ID 1960)
                    case 1960:
                        selectedItem.itemOptions.add(new Item.ItemOption(50, 24)); // Sức đánh +24%
                        selectedItem.itemOptions.add(new Item.ItemOption(77, 23)); // HP +23%
                        selectedItem.itemOptions.add(new Item.ItemOption(103, 29)); // KI +29%
                        selectedItem.itemOptions.add(new Item.ItemOption(226, 21)); // Cute +21%
                        selectedItem.itemOptions.add(new Item.ItemOption(14, 7)); // Chí mạng +7%
                        selectedItem.itemOptions.add(new Item.ItemOption(93, random93)); // Option phụ
                        selectedItem.itemOptions.add(new Item.ItemOption(93, 15)); // Hạn sử dụng 15 ngày (nếu hệ thống
                                                                                   // hỗ trợ)
                        break;

                    // 🌟 Cải trang Pan VIP (ID 1961)
                    case 1961:
                        selectedItem.itemOptions.add(new Item.ItemOption(95, 14)); // Biến 14% tấn công thành HP
                        selectedItem.itemOptions.add(new Item.ItemOption(236, 100)); // +100% May mắn
                        selectedItem.itemOptions.add(new Item.ItemOption(30, 0)); // Option đặc biệt
                        selectedItem.itemOptions.add(new Item.ItemOption(93, random93));
                        selectedItem.itemOptions.add(new Item.ItemOption(93, 15)); // Hạn 15 ngày
                        break;
                }

                // Thông báo vật phẩm hiếm
                if (selectedItemId == 1960 || selectedItemId == 1961) {
                    Service.gI().sendThongBaoAllPlayer(
                            "" + pl.name + " vừa nhận được cải trang cực hiếm " + selectedItem.template.name + "!");
                }
            }

            // Thêm item vào hành trang
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                pl.thiepchucvip += 5;
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }

        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm thiepchucvip: " + e.getMessage());
        }
    }

    private void CapsuleTrangSucVIP(Player pl, Item item) {
        try {
            List<Short> itemList = Arrays.asList(
                    (short) 1100, (short) 1700, (short) 1669,
                    (short) 1344, (short) 954, (short) 1587, (short) 1588,
                    (short) 1142, (short) 1197, (short) 1206, (short) 1519,
                    (short) 1520, (short) 1595, (short) 1962, (short) 1963);

            short selectedItemId = itemList.get(Util.nextInt(0, itemList.size()));
            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);

            // ========== RANDOM DÒNG SỨC MẠNH ==========
            int[] randomValues = getRandomPowerSet(); // random 3 dòng 5–11%

            selectedItem.itemOptions.add(new Item.ItemOption(50, randomValues[0]));
            selectedItem.itemOptions.add(new Item.ItemOption(77, randomValues[1]));
            selectedItem.itemOptions.add(new Item.ItemOption(103, randomValues[2]));

            // Dòng phụ ngẫu nhiên 210 (sức mạnh thêm)
            selectedItem.itemOptions.add(new Item.ItemOption(210, Util.nextInt(1, 4)));

            // Dòng may mắn 93 ngẫu nhiên
            if (Util.nextInt(0, 100) < 99) {
                selectedItem.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 14)));
            }

            // ========== TRAO VẬT PHẨM ==========
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }

        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm Trang Sức VIP: " + e.getMessage());
        }
    }

    /**
     * Random 3 dòng sức mạnh xen kẽ mạnh – yếu (giá trị 5–11%) Ví dụ: [11, 5,
     * 6], [6, 11, 5], [5, 7, 11]
     */
    private int[] getRandomPowerSet() {
        int strong = Util.nextInt(9, 11); // dòng mạnh
        int mid = Util.nextInt(7, 9); // dòng trung bình
        int weak = Util.nextInt(5, 7); // dòng yếu

        int[][] patterns = {
                { strong, weak, weak },
                { weak, strong, weak },
                { weak, weak, strong },
                { strong, mid, weak },
                { mid, strong, weak },
                { weak, mid, strong }
        };
        return patterns[Util.nextInt(0, patterns.length)];
    }

    private void Hopdothanlinh(Player pl, Item item) {// hop qua do thần linh
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của Bạn đi", "Set trái đất",
                "Set namec", "Set xayda", "Từ chổi");
    }

    private void Hopdovaitho(Player pl, Item item) {// hop qua do vãi thô
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của Bạn đi", "Set trái đất",
                "Set namec", "Set xayda", "Từ chổi");
    }

    private void Hopdohuydiet(Player pl, Item item) {// hop qua do huy diet
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của Bạn đi", "Set trái đất",
                "Set namec", "Set xayda", "Từ chổi");
    }

    public void UseCard(Player pl, Item item) {
        RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(c -> c.Id == item.template.id)
                .findFirst().orElse(null);
        if (radarTemplate == null) {
            return;
        }
        if (radarTemplate.Require != -1) {
            RadarCard radarRequireTemplate = RadarService.gI().RADAR_TEMPLATE.stream()
                    .filter(r -> r.Id == radarTemplate.Require).findFirst().orElse(null);
            if (radarRequireTemplate == null) {
                return;
            }
            Card cardRequire = pl.Cards.stream().filter(r -> r.Id == radarRequireTemplate.Id).findFirst().orElse(null);
            if (cardRequire == null || cardRequire.Level < radarTemplate.RequireLevel) {
                Service.gI().sendThongBao(pl, "Bạn cần sưu tầm " + radarRequireTemplate.Name + " ở cấp độ "
                        + radarTemplate.RequireLevel + " mới có thể sử dụng thẻ này");
                return;
            }
        }
        Card card = pl.Cards.stream().filter(r -> r.Id == item.template.id).findFirst().orElse(null);
        if (card == null) {
            Card newCard = new Card(item.template.id, (byte) 1, radarTemplate.Max, (byte) -1, radarTemplate.Options);
            if (pl.Cards.add(newCard)) {
                RadarService.gI().RadarSetAmount(pl, newCard.Id, newCard.Amount, newCard.MaxAmount);
                RadarService.gI().RadarSetLevel(pl, newCard.Id, newCard.Level);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().sendItemBag(pl);
            }
        } else {
            if (card.Level >= 2) {
                Service.gI().sendThongBao(pl, "Thẻ này đã đạt cấp tối đa");
                return;
            }
            card.Amount++;
            if (card.Amount >= card.MaxAmount) {
                card.Amount = 0;
                if (card.Level == -1) {
                    card.Level = 1;
                } else {
                    card.Level++;
                }
                Service.gI().point(pl);
            }
            RadarService.gI().RadarSetAmount(pl, card.Id, card.Amount, card.MaxAmount);
            RadarService.gI().RadarSetLevel(pl, card.Id, card.Level);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
        }
    }

    private void ItemSKH(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Hãy chọn 1 trong các trang bị", "Áo", "Quần",
                "Găng", "Giày", "Rađa");
    }
}
