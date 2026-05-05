package services.func;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import utils.Functions;
import java.util.HashMap;
import java.util.Map;
import item.Item;
import consts.ConstNpc;
import consts.ConstPlayer;
import consts.ConstTaskBadges;
import item.Item.ItemOption;
import map.Zone;
import nro.services.NpcService;
import nro.player.Inventory;
import nro.player.Player;
import nro.services.Service;
import utils.Util;
import network.Message;
import nro.services.ItemService;
import nro.services.PlayerService;
import nro.services.InventoryService;
import utils.Logger;
import java.util.List;
import nro.server.Maintenance;
import task.Badges.BadgesTaskService;

public class SummonDragon {

    public static final byte WISHED = 0;
    public static final byte TIME_UP = 1;

    public static final byte DRAGON_SHENRON = 0;
    public static final byte DRAGON_PORUNGA = 1;

    public static final short NGOC_RONG_1_SAO = 14;
    public static final short NGOC_RONG_2_SAO = 15;
    public static final short NGOC_RONG_3_SAO = 16;
    public static final short NGOC_RONG_4_SAO = 17;
    public static final short NGOC_RONG_5_SAO = 18;
    public static final short NGOC_RONG_6_SAO = 19;
    public static final short NGOC_RONG_7_SAO = 20;

    // Ngọc Rồng Siêu Cấp (ép từ 7 viên NRO 1 sao, tỉ lệ 50/50)
    public static final short NGOC_RONG_SC_1_SAO = 2980;
    public static final short NGOC_RONG_SC_2_SAO = 2981;
    public static final short NGOC_RONG_SC_3_SAO = 2982;
    public static final short NGOC_RONG_SC_4_SAO = 2983;
    public static final short NGOC_RONG_SC_5_SAO = 2984;
    public static final short NGOC_RONG_SC_6_SAO = 2985;
    public static final short NGOC_RONG_SC_7_SAO = 2986;

    public static final String SUMMON_SHENRON_TUTORIAL = "Có 3 cách gọi rồng thần. Gọi từ ngọc 1 sao, gọi từ ngọc 2 sao, hoặc gọi từ ngọc 3 sao\n"
            + "Các ngọc 4 sao đến 7 sao không thể gọi rồng thần được\n"
            + "Để gọi rồng 1 sao cần ngọc từ 1 sao đến 7 sao\n"
            + "Để gọi rồng 2 sao cần ngọc từ 2 sao đến 7 sao\n"
            + "Để gọi rồng 3 sao cần ngọc từ 3 sao đến 7sao\n"
            + "Điều ước rồng 3 sao: 200 ngọc xanh, hoặc 2 triệu sức mạnh, hoặc 20 triệu vàng\n"
            + "Điều ước rồng 2 sao: 2.000 ngọc xanh, hoặc 20 triệu sức mạnh, hoặc 200 triệu vàng\n"
            + "Điều ước rồng 1 sao: 100K ngọc xanh + 100 thỏi vàng, cải trang VIP VĨNH VIỄN + thay chiêu đệ tử, 2 tỷ SM+TN...\n"
            + "Lưu ý: Ngọc trong điều ước là NGỌC XANH, không phải Hồng Ngọc nạp.\n"
            + "Ngọc rồng sẽ mất ngay khi gọi rồng dù bạn có ước hay không\n"
            + "Quá 5 phút nếu không ước rồng thần sẽ bay mất";
    public static final String SHENRON_SAY = "Ta sẽ ban cho người 1 điều ước, ngươi có 5 phút, hãy suy nghĩ thật kỹ trước khi quyết định";

    public static final String[] SHENRON_1_STAR_WISHES_1 = new String[] { "Giàu có\n+100K Ngọc\n+100 Thỏi Vàng",
            "Găng tay\nđang mang\nlên 1 cấp", "Chí mạng\nGốc +2%",
            "Thay\nChiêu 2-3\nĐệ tử", "Điều ước\nkhác" };
    public static final String[] SHENRON_1_STAR_WISHES_2 = new String[] { "Đẹp Trai\nCải trang VIP\nVĩnh viễn",
            "+2 Tỷ\nSức mạnh\nvà Tiềm\nnăng", "Găng tay đệ\nđang mang\nlên 1 cấp",
            "Điều ước\nkhác" };
    public static final String[] SHENRON_1_STAR_WISHES_3 = new String[] { "3 Trứng\nrồng nhí", "Điều ước\nkhác" };
    public static final String[] SHENRON_2_STARS_WHISHES = new String[] { "Giàu có\n+2K\nNgọc xanh",
            "+20 Tr\nSức mạnh\nvà tiềm năng", "Giàu có\n+200 Tr\nVàng" };
    public static final String[] SHENRON_3_STARS_WHISHES = new String[] { "Giàu có\n+200\nNgọc xanh",
            "+2 Tr\nSức mạnh\nvà tiềm năng", "Giàu có\n+20 Tr\nVàng" };

    // Điều ước Rồng Thần Siêu Cấp (mạnh hơn rồng 1 sao ~3-5 lần)
    public static final String[] SHENRON_SC_WISHES = new String[] {
            "Giàu có\n+200K Ngọc\n+200 Thỏi Vàng",
            "+10 Tỷ\nSức mạnh\nvà Tiềm năng",
            "Chí mạng\nGốc +3%",
            "Găng tay\nđang mang\nlên 2 cấp" };
    // --------------------------------------------------------------------------
    private static SummonDragon instance;
    private final Map pl_dragonStar;
    private long lastTimeShenronAppeared;
    private long lastTimeShenronWait;
    private final int timeResummonShenron = 600000;
    private boolean isShenronAppear;
    private final int timeShenronWait = 300000;

    private final Thread update;
    private boolean active;

    public boolean isPlayerDisconnect;
    public Player playerSummonShenron;
    private int playerSummonShenronId;
    private Zone mapShenronAppear;
    private byte shenronStar;
    private int menuShenron;
    private byte select;

    private SummonDragon() {
        this.pl_dragonStar = new HashMap<>();
        this.update = Thread.ofVirtual().unstarted(() -> {
            while (active && !Maintenance.isRunning) {
                try {
                    if (isShenronAppear) {
                        if (isPlayerDisconnect) {
                            List<Player> players = mapShenronAppear.getPlayers();
                            for (Player plMap : players) {
                                if (plMap.isPl() && plMap.id == playerSummonShenronId) {
                                    playerSummonShenron = plMap;
                                    reSummonShenron();
                                    isPlayerDisconnect = false;
                                    break;
                                }
                            }
                        }

                        if (Util.canDoWithTime(lastTimeShenronWait, timeShenronWait)) {
                            shenronLeave(playerSummonShenron, TIME_UP);
                        }
                    }

                    Functions.sleep(1000);
                } catch (Exception e) {
                    Logger.logException(SummonDragon.class, e);
                }
            }
        });

        this.active();
    }

    private void active() {
        if (!active) {
            active = true;
            if (!update.isAlive()) {
                update.start();
            }
        }
    }

    public static SummonDragon gI() {
        if (instance == null) {
            instance = new SummonDragon();
        }
        return instance;
    }

    public void openMenuSummonShenron(Player pl, byte dragonBallStar) {
        this.pl_dragonStar.put(pl, dragonBallStar);
        NpcService.gI().createMenuConMeo(pl, ConstNpc.SUMMON_SHENRON, -1, "Bạn muốn gọi rồng thần ?",
                "Hướng\ndẫn thêm\n(mới)", "Gọi\nRồng Thần\n" + dragonBallStar + " Sao");
    }

    public void summonShenron(Player pl) {
        if (pl.zone.map.mapId == 0 || pl.zone.map.mapId == 7 || pl.zone.map.mapId == 14) {
            if (checkShenronBall(pl)) {
                if (isShenronAppear) {
                    Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    return;
                }

                if (Util.canDoWithTime(lastTimeShenronAppeared, timeResummonShenron)) {
                    // gọi rồng
                    playerSummonShenron = pl;
                    playerSummonShenronId = (int) pl.id;
                    mapShenronAppear = pl.zone;
                    byte dragonStar = (byte) pl_dragonStar.get(playerSummonShenron);
                    int begin = NGOC_RONG_1_SAO;
                    switch (dragonStar) {
                        case 2:
                            begin = NGOC_RONG_2_SAO;
                            break;
                        case 3:
                            begin = NGOC_RONG_3_SAO;
                            break;
                    }
                    for (int i = begin; i <= NGOC_RONG_7_SAO; i++) {
                        try {
                            InventoryService.gI().subQuantityItemsBag(pl, InventoryService.gI().findItemBag(pl, i), 1);
                        } catch (Exception ex) {
                        }
                    }
                    InventoryService.gI().sendItemBag(pl);
                    sendNotifyShenronAppear();
                    activeShenron(pl, true, SummonDragon.DRAGON_SHENRON);
                    sendWhishesShenron(pl);
                } else {
                    int timeLeft = (int) ((timeResummonShenron - (System.currentTimeMillis() - lastTimeShenronAppeared))
                            / 1000);
                    Service.gI().sendThongBao(pl, "Vui lòng đợi "
                            + (timeLeft < 7200 ? timeLeft + " giây" : timeLeft / 60 + " phút") + " nữa");
                }
            }
        } else {
            Service.gI().sendThongBao(pl, "Chỉ được gọi rồng thần ở ngôi làng trước nhà");
        }
    }

    private void reSummonShenron() {
        activeShenron(playerSummonShenron, true, SummonDragon.DRAGON_SHENRON);
        sendWhishesShenron(playerSummonShenron);
    }

    private void sendWhishesShenron(Player pl) {
        byte dragonStar;
        try {
            dragonStar = (byte) pl_dragonStar.get(pl);
            this.shenronStar = dragonStar;
        } catch (Exception e) {
            dragonStar = this.shenronStar;
        }
        switch (dragonStar) {
            case 1:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_1, SHENRON_SAY, SHENRON_1_STAR_WISHES_1);
                break;
            case 2:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_2, SHENRON_SAY, SHENRON_2_STARS_WHISHES);
                break;
            case 3:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_3, SHENRON_SAY, SHENRON_3_STARS_WHISHES);
                break;
        }
    }

    private void activeShenron(Player pl, boolean appear, byte type) {
        Message msg;
        try {
            msg = new Message(-83);
            msg.writer().writeByte(appear ? 0 : (byte) 1);
            if (appear) {
                msg.writer().writeShort(pl.zone.map.mapId);
                msg.writer().writeShort(pl.zone.map.bgId);
                msg.writer().writeByte(pl.zone.zoneId);
                msg.writer().writeInt((int) pl.id);
                msg.writer().writeUTF("BARCOLLxENZEEFXNRO");
                msg.writer().writeShort(pl.location.x);
                msg.writer().writeShort(pl.location.y);
                msg.writer().writeByte(type);
                lastTimeShenronWait = System.currentTimeMillis();
                isShenronAppear = true;
                pl.iDMark.setShenronType(-1);
            }
            Service.gI().sendMessAllPlayer(msg);
        } catch (Exception e) {
        }
    }

    private boolean checkShenronBall(Player pl) {
        byte dragonStar = (byte) this.pl_dragonStar.get(pl);
        if (dragonStar == 1) {
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_1_SAO)) {
                Service.gI().sendThongBao(pl, "Ban con thieu 1 vien ngoc rong 1 sao");
                return false;
            }
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_2_SAO)) {
                Service.gI().sendThongBao(pl, "Ban con thieu 1 vien ngoc rong 2 sao");
                return false;
            }
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_3_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 3 sao");
                return false;
            }
        } else if (dragonStar == 2) {
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_3_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 3 sao");
                return false;
            }
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_4_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 4 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_5_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 5 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_6_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 6 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_7_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 7 sao");
            return false;
        }
        return true;
    }

    private void sendNotifyShenronAppear() {
        Message msg = null;
        try {
            msg = new Message(-25);
            msg.writer().writeUTF(playerSummonShenron.name + " vừa gọi rồng thần tại "
                    + playerSummonShenron.zone.map.mapName + " khu vực " + playerSummonShenron.zone.zoneId);
            Service.gI().sendMessAllPlayerIgnoreMe(playerSummonShenron, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void confirmWish() {
        try {
        switch (this.menuShenron) {
            case ConstNpc.SHENRON_1_1:
                switch (this.select) {

                    case 0: // Giàu có: +100k ngọc xanh + 100 thỏi vàng
                        this.playerSummonShenron.inventory.gem += 100000;
                        // Thêm 100 thỏi vàng (item 457)
                        for (int i = 0; i < 10; i++) {
                            if (InventoryService.gI().getCountEmptyBag(playerSummonShenron) > 0) {
                                Item thoiVang = ItemService.gI().createNewItem((short) 457, 10);
                                InventoryService.gI().addItemBag(playerSummonShenron, thoiVang);
                            }
                        }
                        InventoryService.gI().sendItemBag(playerSummonShenron);
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        Service.gI().sendThongBao(playerSummonShenron,
                                "Bạn nhận được 100.000 Ngọc xanh + 100 Thỏi vàng!");
                        BadgesTaskService.updateCountBagesTask(playerSummonShenron, ConstTaskBadges.TRUM_UOC_RONG, 1);
                        break;
                    case 1: // găng tay đang đeo lên 2 cấp (giới hạn +3)
                        Item item = this.playerSummonShenron.inventory.itemsBody.get(2);
                        if (item.isNotNullItem()) {
                            // Tìm option 72 (cấp nâng rồng) trên găng
                            int level = 0;
                            ItemOption upgradeOption = null;
                            for (ItemOption io : item.itemOptions) {
                                if (io.optionTemplate.id == 72) {
                                    upgradeOption = io;
                                    level = io.param;
                                    break;
                                }
                            }
                            if (level >= 3) {
                                Service.gI().sendThongBao(playerSummonShenron,
                                        "Găng tay của ngươi đã đạt cấp +3, không thể nâng thêm bằng ước rồng");
                                reOpenShenronWishes(playerSummonShenron);
                                return;
                            }
                            // Nâng cấp: tăng 2 level option 72 (cap tại 3)
                            int addLevel = Math.min(2, 3 - level);
                            if (upgradeOption != null) {
                                upgradeOption.param += addLevel;
                            } else {
                                item.itemOptions.add(new ItemOption(72, addLevel));
                            }
                            // Tăng 20% sức đánh cho găng (tìm option sức đánh: 0, 23, hoặc 50)
                            boolean foundDameOption = false;
                            for (ItemOption io : item.itemOptions) {
                                if (io.optionTemplate.id == 0 || io.optionTemplate.id == 23 || io.optionTemplate.id == 50) {
                                    io.param += Math.max(1, io.param * 20 / 100);
                                    foundDameOption = true;
                                    break;
                                }
                            }
                            if (!foundDameOption) {
                                item.itemOptions.add(new ItemOption(0, 20));
                            }
                            InventoryService.gI().sendItemBody(playerSummonShenron);
                            Service.gI().sendThongBao(playerSummonShenron,
                                    "Găng tay đã được nâng lên +" + (level + addLevel) + "!");
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Ngươi hiện tại có đeo găng đâu");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        BadgesTaskService.updateCountBagesTask(playerSummonShenron, ConstTaskBadges.TRUM_UOC_RONG, 1);
                        break;
                    case 2: // chí mạng +2%
                        if (this.playerSummonShenron.nPoint.critg < 9) {
                            this.playerSummonShenron.nPoint.critg += 2;
                            Service.gI().point(playerSummonShenron);
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron,
                                    "Điều ước này đã quá sức với ta, ta sẽ cho ngươi chọn lại");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        BadgesTaskService.updateCountBagesTask(playerSummonShenron, ConstTaskBadges.TRUM_UOC_RONG, 1);
                        break;
                    case 3: // thay chiêu 2-3 đệ tử
                        if (playerSummonShenron.pet != null) {
                            if (playerSummonShenron.pet.playerSkill.skills.get(1).skillId != -1) {
                                playerSummonShenron.pet.openSkill2();
                                if (playerSummonShenron.pet.playerSkill.skills.get(2).skillId != -1) {
                                    playerSummonShenron.pet.openSkill3();
                                }
                            } else {
                                Service.gI().sendThongBao(playerSummonShenron,
                                        "Ít nhất đệ tử ngươi phải có chiêu 2 chứ!");
                                reOpenShenronWishes(playerSummonShenron);
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Ngươi làm gì có đệ tử?");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        BadgesTaskService.updateCountBagesTask(playerSummonShenron, ConstTaskBadges.TRUM_UOC_RONG, 1);
                        break;

                }
                break;

            case ConstNpc.SHENRON_1_2:
                switch (this.select) {
                    case 0: // Đẹp trai: Cải trang VIP VĨNH VIỄN + stats mạnh + thay chiêu đệ tử
                        if (InventoryService.gI().getCountEmptyBag(playerSummonShenron) > 0) {
                            byte gender = this.playerSummonShenron.gender;
                            Item avtVip = ItemService.gI().createNewItem((short) (gender == ConstPlayer.TRAI_DAT ? 227
                                    : gender == ConstPlayer.NAMEC ? 228 : 229));
                            // Stats VIP mạnh + VĨNH VIỄN (không có option 30 = không hết hạn)
                            avtVip.itemOptions.add(new ItemOption(50, 23)); // Sức đánh +23%
                            avtVip.itemOptions.add(new ItemOption(77, 20)); // HP +20%
                            avtVip.itemOptions.add(new ItemOption(103, 20)); // KI +20%
                            avtVip.itemOptions.add(new ItemOption(97, 15)); // Phản 15% sát thương
                            avtVip.itemOptions.add(new ItemOption(14, 10)); // Chí mạng +10%
                            InventoryService.gI().addItemBag(playerSummonShenron, avtVip);
                            InventoryService.gI().sendItemBag(playerSummonShenron);
                            // Thay chiêu 2-3 đệ tử luôn
                            if (playerSummonShenron.pet != null
                                    && playerSummonShenron.pet.playerSkill.skills.get(1).skillId != -1) {
                                playerSummonShenron.pet.openSkill2();
                                if (playerSummonShenron.pet.playerSkill.skills.get(2).skillId != -1) {
                                    playerSummonShenron.pet.openSkill3();
                                }
                            }
                            Service.gI().sendThongBao(playerSummonShenron,
                                    "Bạn nhận Cải trang VIP VĨNH VIỄN (SĐ+23%, HP+20%, KI+20%, Phản ST 15%, CM+10%)\n"
                                            + "+ Đã thay chiêu 2-3 đệ tử!");
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Hành trang đã đầy");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        BadgesTaskService.updateCountBagesTask(playerSummonShenron, ConstTaskBadges.TRUM_UOC_RONG, 1);
                        break;
                    case 1: // +2 tỷ sức mạnh và tiềm năng
                        Service.gI().addSMTN(this.playerSummonShenron, (byte) 2, 2000000000L, false);
                        Service.gI().sendThongBao(playerSummonShenron, "Bạn nhận được +2 Tỷ Sức mạnh & Tiềm năng!");
                        BadgesTaskService.updateCountBagesTask(playerSummonShenron, ConstTaskBadges.TRUM_UOC_RONG, 1);
                        break;
                    case 2: // găng tay đệ lên 1 cấp (giới hạn +3)
                        if (this.playerSummonShenron.pet != null) {
                            Item item = this.playerSummonShenron.pet.inventory.itemsBody.get(2);
                            if (item.isNotNullItem()) {
                                int level = 0;
                                for (ItemOption io : item.itemOptions) {
                                    if (io.optionTemplate.id == 72) {
                                        level = io.param;
                                        if (level < 3) {
                                            io.param++;
                                        }
                                        break;
                                    }
                                }
                                if (level < 3) {
                                    if (level == 0) {
                                        item.itemOptions.add(new ItemOption(72, 1));
                                    }
                                    for (ItemOption io : item.itemOptions) {
                                        if (io.optionTemplate.id == 0) {
                                            io.param += (io.param * 10 / 100);
                                            break;
                                        }
                                    }
                                    Service.gI().point(playerSummonShenron);
                                } else {
                                    Service.gI().sendThongBao(playerSummonShenron,
                                            "Găng tay của đệ ngươi đã đạt cấp +3, không thể nâng thêm bằng ước rồng");
                                    reOpenShenronWishes(playerSummonShenron);
                                    return;
                                }
                            } else {
                                Service.gI().sendThongBao(playerSummonShenron, "Đệ ngươi hiện tại có đeo găng đâu");
                                reOpenShenronWishes(playerSummonShenron);
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Ngươi đâu có đệ tử");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        BadgesTaskService.updateCountBagesTask(playerSummonShenron, ConstTaskBadges.TRUM_UOC_RONG, 1);
                        break;
                }
                break;
            case ConstNpc.SHENRON_1_3:
                switch (this.select) {
                    case 0:
                        Item trungRong = ItemService.gI().createNewItem((short) 1785);
                        InventoryService.gI().addItemBag(playerSummonShenron, trungRong);
                        InventoryService.gI().sendItemBag(playerSummonShenron);
                        break;
                }
                break;
            case ConstNpc.SHENRON_2:
                switch (this.select) {
                    case 0: // +2.000 ngọc xanh
                        this.playerSummonShenron.inventory.gem += 2000;
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        Service.gI().sendThongBao(playerSummonShenron,
                                "Bạn nhận được 2.000 Ngọc xanh từ điều ước Rồng Thần");
                        break;
                    case 1: // +20 tr smtn
                        Service.gI().addSMTN(this.playerSummonShenron, (byte) 2, 20000000, false);
                        break;
                    case 2: // +200 triệu vàng
                        long addGold2 = 200_000_000L;
                        if (this.playerSummonShenron.inventory.gold + addGold2 > Inventory.LIMIT_GOLD) {
                            this.playerSummonShenron.inventory.gold = Inventory.LIMIT_GOLD;
                        } else {
                            this.playerSummonShenron.inventory.addGoldSafe(addGold2);
                        }
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        break;
                }
                break;
            case ConstNpc.SHENRON_3:
                switch (this.select) {
                    case 0: // +200 ngọc xanh
                        this.playerSummonShenron.inventory.gem += 200;
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        Service.gI().sendThongBao(playerSummonShenron,
                                "Bạn nhận được 200 Ngọc xanh từ điều ước Rồng Thần");
                        break;
                    case 1: // +2 tr smtn
                        Service.gI().addSMTN(this.playerSummonShenron, (byte) 2, 2000000, false);
                        break;
                    case 2: // +20 triệu vàng
                        long addGold3 = 20_000_000L;
                        if (this.playerSummonShenron.inventory.gold + addGold3 > Inventory.LIMIT_GOLD) {
                            this.playerSummonShenron.inventory.gold = Inventory.LIMIT_GOLD;
                        } else {
                            this.playerSummonShenron.inventory.addGoldSafe(addGold3);
                        }
                        PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                        break;
                }
                break;
        }
        shenronLeave(this.playerSummonShenron, WISHED);
        } catch (Exception e) {
            Logger.logException(SummonDragon.class, e, "Lỗi ước rồng thần");
            // Nếu lỗi xảy ra giữa chừng, mở lại menu ước thay vì kẹt
            try {
                if (this.playerSummonShenron != null) {
                    Service.gI().sendThongBao(playerSummonShenron, "Có lỗi xảy ra, hãy chọn lại điều ước!");
                    reOpenShenronWishes(this.playerSummonShenron);
                }
            } catch (Exception ex) {
                Logger.logException(SummonDragon.class, ex, "Lỗi reopen shenron");
            }
        }
    }

    public void showConfirmShenron(Player pl, int menu, byte select) {
        this.menuShenron = menu;
        this.select = select;
        String wish = null;
        switch (menu) {
            case ConstNpc.SHENRON_1_1:
                wish = SHENRON_1_STAR_WISHES_1[select];
                break;
            case ConstNpc.SHENRON_1_2:
                wish = SHENRON_1_STAR_WISHES_2[select];
                break;
            case ConstNpc.SHENRON_1_3:
                wish = SHENRON_1_STAR_WISHES_3[select];
                break;
            case ConstNpc.SHENRON_2:
                wish = SHENRON_2_STARS_WHISHES[select];
                break;
            case ConstNpc.SHENRON_3:
                wish = SHENRON_3_STARS_WHISHES[select];
                break;
        }
        NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_CONFIRM, "Ngươi có chắc muốn ước?", wish, "Từ chối");
    }

    public void reOpenShenronWishes(Player pl) {
        switch (menuShenron) {
            case ConstNpc.SHENRON_1_1:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_1, SHENRON_SAY, SHENRON_1_STAR_WISHES_1);
                break;
            case ConstNpc.SHENRON_1_2:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_2, SHENRON_SAY, SHENRON_1_STAR_WISHES_2);
                break;
            case ConstNpc.SHENRON_1_3:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_3, SHENRON_SAY, SHENRON_1_STAR_WISHES_3);
                break;
            case ConstNpc.SHENRON_2:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_2, SHENRON_SAY, SHENRON_2_STARS_WHISHES);
                break;
            case ConstNpc.SHENRON_3:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_3, SHENRON_SAY, SHENRON_3_STARS_WHISHES);
                break;
            case ConstNpc.SHENRON_SIEU_CAP:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_SIEU_CAP,
                        "Hãy chọn lại điều ước của ngươi!", SHENRON_SC_WISHES);
                break;
        }
    }

    public void shenronLeave(Player pl, byte type) {
        if (type == WISHED) {
            NpcService.gI().createTutorial(pl, 0,
                    "Điều ước của ngươi đã trở thành sự thật\nHẹn gặp ngươi lần sau, ta đi ngủ đây, bái bai");
        } else {
            NpcService.gI().createMenuRongThieng(pl, ConstNpc.IGNORE_MENU,
                    "Ta buồn ngủ quá rồi\nHẹn gặp ngươi lần sau, ta đi đây, bái bai");
        }
        activeShenron(pl, false, SummonDragon.DRAGON_SHENRON);
        this.isShenronAppear = false;
        this.menuShenron = -1;
        this.select = -1;
        this.playerSummonShenron = null;
        this.playerSummonShenronId = -1;
        this.shenronStar = -1;
        this.mapShenronAppear = null;
        lastTimeShenronAppeared = System.currentTimeMillis();
    }
    // ======================== NGỌC RỒNG SIÊU CẤP ========================
    // Item 1015 = "Ngọc rồng Siêu Cấp" (1 viên duy nhất, dùng để gọi rồng)
    public static final short NGOC_RONG_SIEU_CAP = 1015;

    public void openMenuSummonShenronSieuCap(Player pl) {
        this.pl_dragonStar.put(pl, (byte) 99); // 99 = Siêu Cấp marker
        NpcService.gI().createMenuConMeo(pl, ConstNpc.SUMMON_SHENRON_SIEU_CAP, -1,
                "Bạn đang sở hữu Ngọc Rồng Siêu Cấp!\n"
                + "Triệu hồi Rồng Thần Siêu Cấp để nhận\n"
                + "điều ước mạnh gấp nhiều lần rồng thường.\n"
                + "Lưu ý: Viên ngọc sẽ bị tiêu hao khi gọi rồng.",
                "Triệu hồi\nRồng Thần\nSiêu Cấp", "Huỷ");
    }

    public void summonShenronSieuCap(Player pl) {
        if (pl.zone.map.mapId == 0 || pl.zone.map.mapId == 7 || pl.zone.map.mapId == 14) {
            // Kiểm tra có viên 1015 không
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_SIEU_CAP)) {
                Service.gI().sendThongBao(pl, "Bạn chưa có Ngọc Rồng Siêu Cấp!\n"
                        + "Ép 2 viên Ngọc Rồng 1 sao để nhận 1 viên (tỉ lệ 50/50).");
                return;
            }
            if (isShenronAppear) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện, Rồng Thần đang xuất hiện!");
                return;
            }
            if (Util.canDoWithTime(lastTimeShenronAppeared, timeResummonShenron)) {
                playerSummonShenron = pl;
                playerSummonShenronId = (int) pl.id;
                mapShenronAppear = pl.zone;
                // Thu 1 viên Siêu Cấp (item 1015)
                try {
                    InventoryService.gI().subQuantityItemsBag(pl,
                            InventoryService.gI().findItemBag(pl, NGOC_RONG_SIEU_CAP), 1);
                } catch (Exception ignored) {}
                InventoryService.gI().sendItemBag(pl);
                // Thông báo toàn server
                sendNotifyShenronSieuCapAppear();
                activeShenron(pl, true, SummonDragon.DRAGON_PORUNGA);
                // Hiện menu ước
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_SIEU_CAP,
                        "Ta là Rồng Thần Siêu Cấp!\n"
                        + "Điều ước của ngươi sẽ vượt xa giới hạn thông thường.\n"
                        + "Hãy lựa chọn thật kỹ, ngươi chỉ có 5 phút!",
                        SHENRON_SC_WISHES);
            } else {
                int timeLeft = (int) ((timeResummonShenron - (System.currentTimeMillis() - lastTimeShenronAppeared)) / 1000);
                Service.gI().sendThongBao(pl, "Vui lòng đợi "
                        + (timeLeft < 7200 ? timeLeft + " giây" : timeLeft / 60 + " phút") + " nữa");
            }
        } else {
            Service.gI().sendThongBao(pl, "Chỉ được gọi Rồng Thần Siêu Cấp ở ngôi làng trước nhà!");
        }
    }

    /**
     * Hien dialog xac nhan truoc khi ep NRO Sieu Cap.
     * Player phai bam "Dong y" truoc khi 2 vien NR 1 sao bi tieu hao.
     */
    public void combineNroToSieuCap(Player pl) {
        // Kiem tra du 2 vien NRO 1 sao truoc
        int count1Sao = InventoryService.gI().countItemBag(pl, NGOC_RONG_1_SAO);
        if (count1Sao < 2) {
            Service.gI().sendThongBao(pl,
                    "Can 2 vien Ngoc Rong 1 sao de ep!\n"
                    + "Hien co: " + count1Sao + "/2 vien.\n"
                    + "Ti le thanh cong 50%, that bai se MAT het 2 vien!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
            Service.gI().sendThongBao(pl, "Hanh trang da day, can it nhat 1 o trong!");
            return;
        }
        // Hien dialog xac nhan thay vi ep ngay
        NpcService.gI().createMenuConMeo(pl, ConstNpc.CONFIRM_GHEP_NRO_SIEU_CAP, -1,
                "BAN CO CHAC CHAN MUON EP?\n\n"
                + "- Tieu hao: 2 vien Ngoc Rong 1 Sao\n"
                + "- Ti le: 50% thanh cong\n"
                + "- That bai: MAT HET 2 vien!\n\n"
                + "Hien co: " + count1Sao + " vien NR 1 sao",
                "Dong y\nEp ngay", "Huy bo");
    }

    /**
     * Thuc hien ep NRO Sieu Cap sau khi player da xac nhan.
     */
    public void confirmCombineNroToSieuCap(Player pl) {
        // Kiem tra lai lan nua (phong truong hop lag)
        int count1Sao = InventoryService.gI().countItemBag(pl, NGOC_RONG_1_SAO);
        if (count1Sao < 2) {
            Service.gI().sendThongBao(pl, "Khong du 2 vien Ngoc Rong 1 sao!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(pl) < 1) {
            Service.gI().sendThongBao(pl, "Hanh trang da day!");
            return;
        }
        // Thu 2 vien NRO 1 sao
        for (int i = 0; i < 2; i++) {
            try {
                InventoryService.gI().subQuantityItemsBag(pl,
                        InventoryService.gI().findItemBag(pl, NGOC_RONG_1_SAO), 1);
            } catch (Exception ignored) {}
        }
        // Ti le 50/50
        boolean success = Util.nextInt(0, 1) == 1;
        if (success) {
            // Tao 1 vien Ngoc Rong Sieu Cap (item 1015)
            Item scBall = ItemService.gI().createNewItem(NGOC_RONG_SIEU_CAP);
            InventoryService.gI().addItemBag(pl, scBall);
            InventoryService.gI().sendItemBag(pl);
            Service.gI().sendThongBao(pl,
                    "Ep thanh cong! Nhan duoc Ngoc Rong Sieu Cap!\n"
                    + "Dung vien nay de trieu hoi Rong Than Sieu Cap!");
            // Thong bao toan server
            Message msg = null;
            try {
                msg = new Message(-25);
                msg.writer().writeUTF(pl.name + " vua ep thanh cong Ngoc Rong Sieu Cap!");
                Service.gI().sendMessAllPlayerIgnoreMe(pl, msg);
            } catch (Exception ignored) {
            } finally {
                if (msg != null) msg.cleanup();
            }
        } else {
            InventoryService.gI().sendItemBag(pl);
            Service.gI().sendThongBao(pl,
                    "Ep that bai! 2 vien Ngoc Rong 1 sao da bi tieu huy.\n"
                    + "Chuc ban may man lan sau!");
        }
    }

    private void sendNotifyShenronSieuCapAppear() {
        Message msg = null;
        try {
            msg = new Message(-25);
            msg.writer().writeUTF(playerSummonShenron.name
                    + " vừa triệu hồi RỒNG THẦN SIÊU CẤP tại "
                    + playerSummonShenron.zone.map.mapName
                    + " khu vực " + playerSummonShenron.zone.zoneId + "!");
            Service.gI().sendMessAllPlayerIgnoreMe(playerSummonShenron, msg);
        } catch (Exception ignored) {
        } finally {
            if (msg != null) msg.cleanup();
        }
    }

    public void showConfirmShenronSieuCap(Player pl, byte select) {
        this.menuShenron = ConstNpc.SHENRON_SIEU_CAP;
        this.select = select;
        String wish = SHENRON_SC_WISHES[select];
        NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_SIEU_CAP_CONFIRM,
                "Ngươi có chắc muốn ước điều này?", wish, "Từ chối");
    }

    public void confirmWishSieuCap() {
        try {
            switch (this.select) {
                case 0: // +200K ngọc + 200 thỏi vàng
                    this.playerSummonShenron.inventory.gem += 200000;
                    for (int i = 0; i < 20; i++) {
                        if (InventoryService.gI().getCountEmptyBag(playerSummonShenron) > 0) {
                            Item thoiVang = ItemService.gI().createNewItem((short) 457, 10);
                            InventoryService.gI().addItemBag(playerSummonShenron, thoiVang);
                        }
                    }
                    InventoryService.gI().sendItemBag(playerSummonShenron);
                    PlayerService.gI().sendInfoHpMpMoney(this.playerSummonShenron);
                    Service.gI().sendThongBao(playerSummonShenron,
                            "Bạn nhận được 200.000 Ngọc xanh + 200 Thỏi vàng!");
                    break;
                case 1: // +10 tỷ SM+TN (bỏ qua giới hạn sức mạnh)
                    this.playerSummonShenron.nPoint.powerUp(10_000_000_000L);
                    this.playerSummonShenron.nPoint.tiemNangUp(10_000_000_000L);
                    PlayerService.gI().sendTNSM(this.playerSummonShenron, (byte) 2, 10_000_000_000L);
                    Service.gI().point(this.playerSummonShenron);
                    Service.gI().sendThongBao(playerSummonShenron,
                            "Bạn nhận được +10 Tỷ Sức mạnh & Tiềm năng!");
                    break;
                case 2: // chí mạng +3%
                    if (this.playerSummonShenron.nPoint.critg < 15) {
                        int addCrit = Math.min(3, 15 - this.playerSummonShenron.nPoint.critg);
                        this.playerSummonShenron.nPoint.critg += addCrit;
                        Service.gI().point(playerSummonShenron);
                        Service.gI().sendThongBao(playerSummonShenron,
                                "Chí mạng gốc +" + addCrit + "%! Hiện tại: " + this.playerSummonShenron.nPoint.critg + "%");
                    } else {
                        Service.gI().sendThongBao(playerSummonShenron,
                                "Chí mạng đã đạt giới hạn, chọn ước khác!");
                        NpcService.gI().createMenuRongThieng(playerSummonShenron,
                                ConstNpc.SHENRON_SIEU_CAP,
                                "Hãy chọn điều ước khác!", SHENRON_SC_WISHES);
                        return;
                    }
                    break;
                case 3: // Găng tay lên +2 cấp (giới hạn +3)
                    Item glove = this.playerSummonShenron.inventory.itemsBody.get(2);
                    if (glove.isNotNullItem()) {
                        ItemOption upgradeOpt = null;
                        int currentLevel = 0;
                        for (ItemOption io : glove.itemOptions) {
                            if (io.optionTemplate.id == 72) {
                                upgradeOpt = io;
                                currentLevel = io.param;
                                break;
                            }
                        }
                        if (currentLevel >= 3) {
                            Service.gI().sendThongBao(playerSummonShenron,
                                    "Găng tay đã đạt cấp +3, chọn ước khác!");
                            NpcService.gI().createMenuRongThieng(playerSummonShenron,
                                    ConstNpc.SHENRON_SIEU_CAP,
                                    "Hãy chọn điều ước khác!", SHENRON_SC_WISHES);
                            return;
                        }
                        int addLevel = Math.min(2, 3 - currentLevel);
                        if (upgradeOpt != null) {
                            upgradeOpt.param += addLevel;
                        } else {
                            glove.itemOptions.add(new ItemOption(72, addLevel));
                        }
                        // Tăng 30% sức đánh cho găng
                        for (ItemOption io : glove.itemOptions) {
                            if (io.optionTemplate.id == 0 || io.optionTemplate.id == 23 || io.optionTemplate.id == 50) {
                                io.param += Math.max(1, io.param * 30 / 100);
                                break;
                            }
                        }
                        InventoryService.gI().sendItemBody(playerSummonShenron);
                        Service.gI().sendThongBao(playerSummonShenron,
                                "Găng tay đã được nâng lên +" + (currentLevel + addLevel) + " + 30% sức đánh!");
                    } else {
                        Service.gI().sendThongBao(playerSummonShenron, "Ngươi chưa đeo găng tay!");
                        NpcService.gI().createMenuRongThieng(playerSummonShenron,
                                ConstNpc.SHENRON_SIEU_CAP,
                                "Hãy chọn điều ước khác!", SHENRON_SC_WISHES);
                        return;
                    }
                    break;
            }
            shenronLeave(this.playerSummonShenron, WISHED);
        } catch (Exception e) {
            Logger.logException(SummonDragon.class, e, "Lỗi ước Rồng Thần Siêu Cấp");
            try {
                if (this.playerSummonShenron != null) {
                    Service.gI().sendThongBao(playerSummonShenron, "Có lỗi xảy ra, hãy chọn lại!");
                    NpcService.gI().createMenuRongThieng(playerSummonShenron,
                            ConstNpc.SHENRON_SIEU_CAP,
                            "Hãy chọn lại điều ước!", SHENRON_SC_WISHES);
                }
            } catch (Exception ignored) {}
        }
    }

}
