package npc.specialnpc;

/*
 *
 *
 *  Box ZALO:
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import consts.ConstNpc;
import item.Item;
import nro.models.npc.NpcManager;
import nro.player.Player;
import nro.services.Service;
import utils.Util;
import network.Message;
import nro.services.ItemService;
import nro.services.PlayerService;
import nro.services.InventoryService;
import utils.Logger;

public class MagicTree {

    public static final byte MAX_LEVEL = 10;

    public static final short[] PEA_TEMP = { 13, 60, 61, 62, 63, 64, 65, 352, 523, 595 };
    public static final int[] PEA_PARAM = { 100, 500, 2, 4, 8, 16, 32, 64, 128, 256 };

    private static final int[][][] POS_PEAS = {
            { { 19, 22 }, { -1, 16 }, { 3, 10 }, { 19, 8 }, { 9, 0 } },
            { { -1, 27 }, { 22, 35 }, { 15, 24 }, { 0, 17 }, { -1, 7 }, { 26, 5 }, { 5, 0 } },
            { { 25, 41 }, { -1, 40 }, { 25, 34 }, { 3, 32 }, { 25, 23 }, { 10, 19 }, { 2, 12 }, { 17, 10 }, { 4, 5 } },
            { { 3, 44 }, { 21, 49 }, { 25, 39 }, { 4, 30 }, { 29, 25 }, { 0, 18 }, { 21, 15 }, { 14, 39 }, { 18, 25 },
                    { 4, 7 }, { 15, 0 } },
            { { 21, 58 }, { 0, 56 }, { 18, 48 }, { 10, 0 }, { 25, 38 }, { 0, 26 }, { 14, 28 }, { 25, 16 }, { 1, 14 },
                    { 22, 7 }, { 10, 14 }, { 28, 23 }, { 15, 16 } },
            { { 25, 63 }, { 0, 66 }, { 21, 52 }, { 3, 55 }, { 14, 60 }, { 3, 45 }, { 22, 43 }, { 10, 35 }, { 22, 28 },
                    { 3, 28 }, { 18, 17 }, { 3, 14 }, { 17, 6 }, { 11, 22 }, { 6, 1 } },
            { { 32, 86 }, { 5, 77 }, { 25, 77 }, { 8, 89 }, { 29, 68 }, { 4, 63 }, { 18, 61 }, { 33, 53 }, { 8, 48 },
                    { 26, 39 }, { 11, 36 }, { 33, 23 }, { 18, 25 }, { 4, 20 }, { 26, 12 }, { 12, 7 }, { 19, 0 } },
            { { 32, 86 }, { 5, 77 }, { 25, 77 }, { 8, 89 }, { 29, 68 }, { 4, 63 }, { 18, 61 }, { 33, 53 }, { 8, 48 },
                    { 26, 39 }, { 11, 36 }, { 33, 23 }, { 18, 25 }, { 4, 20 }, { 26, 12 }, { 12, 7 }, { 19, 0 },
                    { 19, 0 }, { 19, 0 } },
            { { 32, 86 }, { 5, 77 }, { 25, 77 }, { 8, 89 }, { 29, 68 }, { 4, 63 }, { 18, 61 }, { 33, 53 }, { 8, 48 },
                    { 26, 39 }, { 11, 36 }, { 33, 23 }, { 18, 25 }, { 4, 20 }, { 26, 12 }, { 12, 7 }, { 19, 0 },
                    { 19, 0 }, { 19, 0 }, { 19, 0 }, { 19, 0 } },
            { { 32, 86 }, { 5, 77 }, { 25, 77 }, { 8, 89 }, { 29, 68 }, { 4, 63 }, { 18, 61 }, { 33, 53 }, { 8, 48 },
                    { 26, 39 }, { 11, 36 }, { 33, 23 }, { 18, 25 }, { 4, 20 }, { 26, 12 }, { 12, 7 }, { 19, 0 },
                    { 19, 0 }, { 19, 0 }, { 19, 0 }, { 19, 0 }, { 19, 0 }, { 19, 0 } } };

    // Cân bằng theo tỉ giá hiện tại: 1 thỏi vàng ~= 10k VND, 1 thỏi vàng bán ra ~= 500tr vàng.
    // Mục tiêu: nâng từng cấp đậu thần phải có cảm giác tích lũy, cấp cao là thành tựu dài hạn.
    // days - hours - mins - goldUnit (<= lv3: k vàng, > lv3: triệu vàng)
    private static final short[][] PEA_UPGRADE = {
            { 0, 0, 30, 250 }, { 0, 3, 0, 1000 }, { 1, 0, 0, 5000 }, { 3, 12, 0, 25 },
            { 7, 0, 0, 60 }, { 14, 0, 0, 125 }, { 24, 0, 0, 250 }, { 35, 0, 0, 500 },
            { 50, 0, 0, 1000 }, { 0, 0, 0, 0 }
    };

    private static final int ITEM_XU_NRO = 1705;

    private static final int[] FAST_RESPAWN_GEM = { 6, 9, 12, 16, 22, 30, 40, 55, 75, 100 };
    private static final int[] FAST_UPGRADE_GEM = { 15, 35, 75, 140, 240, 380, 560, 800, 1100, 0 };
    private static final int[] FERTILIZE_XU_NRO = { 3, 5, 8, 12, 18, 25, 35, 50, 70, 0 };
    private static final int[] FERTILIZE_MINUTE_REDUCE = { 10, 20, 45, 90, 180, 360, 720, 1080, 1440, 0 };

    // icon magic tree [gender][level]
    private static final short[][] ID_MAGIC_TREE = {
            { 84, 85, 86, 87, 88, 89, 90, 90, 90, 90 },
            { 371, 372, 373, 374, 375, 376, 377, 377, 377, 377 },
            { 378, 379, 380, 381, 382, 383, 384, 384, 384, 384 },
            { 378, 379, 380, 381, 382, 383, 384, 384, 384, 384 } }; // Majin reuse Xayda
    private static final short[][] POS_MAGIC_TREE = { { 348, 336 }, { 372, 336 }, { 348, 336 }, { 348, 336 } }; // +Majin

    // private static final int[] UPGRADE_GEM = {20, 50, 120, 300, 800, 1500, 3000,
    // 6000, 7500, 10000};
    // private static final int[] HARVEST_GEM = {1, 2, 5, 7, 9, 12, 15, 20, 25, 30};
    //
    // //lv 2 1 // min 1 ngọc
    // //lv 3 10
    // //lv 4 50 - 75
    // //lv 5 749
    // //lv 6 750 - 1500
    // //lv 7 2500
    // //lv 8 5500
    // //lv 9 7500
    // //lv 10 10000 - 57 ngọc
    // lv 1 ko bán
    // lv 2 10 ngọc
    // lv 3 10 ngọc
    // lv 4 10 ngọc
    // lv 5 10 ngọc
    // lv 6 10 ngọc
    // lv 7 10 ngọc
    // lv 8 10 ngọc
    // lv 9 15 ngọc
    // lv 10 20 ngọc

    private boolean loadedMagicTreeToPlayer;
    private Player player;

    public byte level;
    public int currPeas;
    public boolean isUpgrade;
    public long lastTimeHarvest;
    public long lastTimeUpgrade;

    public MagicTree(Player player, byte level, byte currPeas, long lastTimeHarvest, boolean isUpgrade,
            long lastTimeUpgrade) {
        this.player = player;
        this.level = level;
        this.currPeas = currPeas;
        if (this.currPeas > this.getMaxPea()) {
            this.currPeas = this.getMaxPea();
        }
        this.isUpgrade = isUpgrade;
        this.lastTimeHarvest = lastTimeHarvest;
        this.lastTimeUpgrade = lastTimeUpgrade;
    }

    public void update() {
        if (!isUpgrade) {
            if (this.currPeas < this.getMaxPea()) {
                int timeThrow = (int) ((System.currentTimeMillis() - lastTimeHarvest) / 1000);
                int numPeaRelease = timeThrow / getSecondPerPea();
                if (numPeaRelease > 0) {
                    this.currPeas += numPeaRelease;
                    if (this.currPeas >= this.getMaxPea()) {
                        this.currPeas = this.getMaxPea();
                        this.lastTimeHarvest = System.currentTimeMillis();
                    } else {
                        this.lastTimeHarvest += (numPeaRelease * getSecondPerPea()) * 1000;
                    }
                }
            }
        } else {
            if (Util.canDoWithTime(lastTimeUpgrade, getTimeUpgrade())) {
                if (this.level < MAX_LEVEL) {
                    this.level++;
                }
                this.isUpgrade = false;
            }
        }
    }

    public void loadMagicTree() {
        Message msg;
        try {
            msg = new Message(-34);
            msg.writer().writeByte(0);

            msg.writer().writeShort(ID_MAGIC_TREE[player.gender][level - 1]);

            msg.writer().writeUTF("Đậu thần cấp " + level);
            msg.writer().writeShort(POS_MAGIC_TREE[player.gender][0]);
            msg.writer().writeShort(POS_MAGIC_TREE[player.gender][1]);
            msg.writer().writeByte(level);
            msg.writer().writeShort(this.currPeas);
            msg.writer().writeShort(getMaxPea());
            msg.writer().writeUTF(this.isUpgrade ? "Đang nâng cấp" : "Đang kết hạt");
            msg.writer().writeInt(this.isUpgrade ? getSecondUpgrade() : getSecondPea()); // seconds
            msg.writer().writeByte(POS_PEAS[this.level - 1].length); // pos pea
            for (int i = 0; i < POS_PEAS[this.level - 1].length; i++) {
                msg.writer().writeByte(POS_PEAS[this.level - 1][i][0]);
                msg.writer().writeByte(POS_PEAS[this.level - 1][i][1]);
            }
            msg.writer().writeBoolean(this.isUpgrade);
            player.sendMessage(msg);
            msg.cleanup();
            if (!loadedMagicTreeToPlayer) {
                loadedMagicTreeToPlayer = true;
            }
        } catch (Exception e) {
            Logger.logException(MagicTree.class, e);
        }
    }

    public void openMenuTree() {
        Message msg;
        try {
            msg = new Message(-34);
            msg.writer().writeByte(1);
            if (!isUpgrade) {
                msg.writer().writeUTF("Thu\nhoạch");
                if (this.level < MAX_LEVEL) {
                    msg.writer().writeUTF(getTextMenuUpgrade());
                }
                if (this.currPeas < this.getMaxPea()) {
                    msg.writer().writeUTF("Kết hạt\nnhanh\n" + getFastRespawnGemRequire() + " ngọc");
                    msg.writer().writeUTF("Hướng\ndẫn");
                    this.player.iDMark.setIndexMenu(ConstNpc.MAGIC_TREE_NON_UPGRADE_LEFT_PEA);
                } else {
                    msg.writer().writeUTF("Hướng\ndẫn");
                    this.player.iDMark.setIndexMenu(ConstNpc.MAGIC_TREE_NON_UPGRADE_FULL_PEA);
                }
            } else {
                msg.writer().writeUTF("Nâng cấp\nnhanh\n" + getFastUpgradeGemRequire() + "\nngọc");
                msg.writer().writeUTF("Bón Xu\n" + getFertilizeXuRequire() + " Xu\ngiảm "
                        + getFertilizeMinuteReduce() + "'");
                msg.writer().writeUTF("Hướng\ndẫn");
                msg.writer().writeUTF("Hủy\nnâng\ncấp\n"
                        + formatGold(getGoldUpgradeRequire() / 2));
                this.player.iDMark.setIndexMenu(ConstNpc.MAGIC_TREE_UPGRADE);
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(MagicTree.class, e);
        }
    }

    public void harvestPea() {
        if (this.currPeas > 0) {
            byte currPeasTemp = (byte) this.currPeas;
            this.currPeas = (byte) this.addPeaHarvest(this.level, this.currPeas);
            if (this.currPeas == currPeasTemp) {
                return;
            }
            this.lastTimeHarvest = System.currentTimeMillis();
            InventoryService.gI().sendItemBag(player);
            Message msg;
            try {
                msg = new Message(-34);
                msg.writer().writeByte(2);
                msg.writer().writeShort(this.currPeas);
                msg.writer().writeInt(getSecondPea());
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Logger.logException(MagicTree.class, e);
            }
        }
    }

    public void showConfirmUpgradeMagicTree() {
        NpcManager.getByIdAndMap(ConstNpc.DAU_THAN, this.player.zone.map.mapId).createOtherMenu(player,
                ConstNpc.MAGIC_TREE_CONFIRM_UPGRADE, "Bạn có chắc chắn nâng cấp cây đậu?", "OK", "Từ chối");
    }

    public void showConfirmUnuppgradeMagicTree() {
        NpcManager.getByIdAndMap(ConstNpc.DAU_THAN, this.player.zone.map.mapId).createOtherMenu(player,
                ConstNpc.MAGIC_TREE_CONFIRM_UNUPGRADE, "Bạn có chắc chắn hủy nâng cấp cây đậu?", "OK", "Từ chối");
    }

    public void upgradeMagicTree() {
        int goldRequire = getGoldUpgradeRequire();
        if (this.player.inventory.gold < goldRequire) {
            Service.gI().sendThongBao(player, "Bạn không đủ vàng để nâng cấp, còn thiếu "
                    + (goldRequire - this.player.inventory.gold) + " vàng nữa");
        } else {
            this.player.inventory.gold -= goldRequire;
            PlayerService.gI().sendInfoHpMpMoney(this.player);
            this.isUpgrade = true;
            this.lastTimeUpgrade = System.currentTimeMillis();
            this.loadMagicTree();
        }
    }

    public void unupgradeMagicTree() {
        int goldReturn = getGoldUpgradeRequire() / 2;
        this.player.inventory.gold += goldReturn;
        PlayerService.gI().sendInfoHpMpMoney(this.player);
        this.isUpgrade = false;
        this.loadMagicTree();
    }

    public void fastRespawnPea() {
        int gemRequire = getFastRespawnGemRequire();
        if (this.player.inventory.gem < gemRequire) {
            Service.gI().sendThongBao(player, "Bạn không đủ ngọc để kết hạt nhanh, còn thiếu "
                    + (gemRequire - this.player.inventory.gem) + " ngọc nữa");
            return;
        }
        this.player.inventory.gem -= gemRequire;
        Service.gI().sendMoney(player);
        this.currPeas = this.getMaxPea();
        this.loadMagicTree();
    }

    public void fastUpgradeMagicTree() {
        int gemRequire = getFastUpgradeGemRequire();
        if (this.player.inventory.gem < gemRequire) {
            Service.gI().sendThongBao(player, "Bạn không đủ ngọc để nâng cấp nhanh, còn thiếu "
                    + (gemRequire - this.player.inventory.gem) + " ngọc nữa");
            return;
        }
        this.player.inventory.gem -= gemRequire;
        Service.gI().sendMoney(player);
        if (this.level < MAX_LEVEL) {
            this.level++;
        }
        this.isUpgrade = false;
        this.loadMagicTree();
    }

    public void showMagicTreeGuide() {
        NpcManager.getByIdAndMap(ConstNpc.DAU_THAN, this.player.zone.map.mapId).createOtherMenu(player,
                ConstNpc.BASE_MENU,
                "|7|— CÂY ĐẬU —\n"
                        + "|1|Cấp: " + level + "/" + MAX_LEVEL + "   Đậu: " + currPeas + "/" + getMaxPea() + "\n\n"
                        + "|1|Thu hoạch\n"
                        + "Nhận đậu để hồi HP/KI.\n\n"
                        + "|1|Nâng cấp\n"
                        + "Tốn vàng + thời gian.\n"
                        + "Cấp cao chứa nhiều đậu hơn.\n\n"
                        + "|1|Tăng tốc\n"
                        + "Ngọc: xong ngay. Xu NRO: giảm giờ.\n\n"
                        + "|1|Hủy nâng: hoàn 50% vàng.",
                "Đã hiểu");
    }

    public void showFertilizeXuGuide() {
        showMagicTreeGuide();
    }

    public void fertilizeByXuNro() {
        if (!this.isUpgrade) {
            Service.gI().sendThongBao(player, "Chỉ có thể bón Xu khi cây đậu đang nâng cấp");
            return;
        }
        int xuRequire = getFertilizeXuRequire();
        int minuteReduce = getFertilizeMinuteReduce();
        if (xuRequire <= 0 || minuteReduce <= 0) {
            Service.gI().sendThongBao(player, "Cấp hiện tại không thể bón Xu NRO");
            return;
        }
        Item xuNro = InventoryService.gI().findItemBag(player, ITEM_XU_NRO);
        if (xuNro == null || xuNro.quantity < xuRequire) {
            Service.gI().sendThongBao(player, "Bạn không đủ Xu NRO để bón, cần " + xuRequire + " Xu NRO");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, xuNro, xuRequire);
        InventoryService.gI().sendItemBag(player);
        this.lastTimeUpgrade -= minuteReduce * 60_000L;
        if (Util.canDoWithTime(lastTimeUpgrade, getTimeUpgrade())) {
            if (this.level < MAX_LEVEL) {
                this.level++;
            }
            this.isUpgrade = false;
            Service.gI().sendThongBao(player, "Bón Xu thành công, cây Đậu thần đã nâng xong!");
        } else {
            Service.gI().sendThongBao(player, "Bón " + xuRequire + " Xu NRO thành công, giảm "
                    + minuteReduce + " phút nâng cấp");
        }
        this.loadMagicTree();
    }

    private byte getMaxPea() {
        return (byte) ((this.level - 1) * 2 + 5);
    }

    private short getSecondPerPea() {
        return (short) (this.level * 60);
    }

    private int getSecondPea() {
        short secondPerPea = (short) getSecondPerPea();
        long timePeaRelease = lastTimeHarvest + secondPerPea * 1000;
        int secondLeft = (int) ((timePeaRelease - System.currentTimeMillis()) / 1000);
        return secondLeft < 0 ? 0 : secondLeft;
    }

    private int getSecondUpgrade() {
        return (int) ((lastTimeUpgrade + getTimeUpgrade() - System.currentTimeMillis()) / 1000);
    }

    private String getTextMenuUpgrade() {
        String text = "Nâng cấp\n";
        short d = PEA_UPGRADE[this.level - 1][0];
        short h = PEA_UPGRADE[this.level - 1][1];
        short m = PEA_UPGRADE[this.level - 1][2];
        short gold = PEA_UPGRADE[this.level - 1][3];
        if (d != 0) {
            text += d + "d";
        }
        if (h != 0) {
            text += h + "h";
        }
        if (m != 0) {
            text += m + "'";
        }
        text += "\n" + formatGold(getGoldUpgradeRequire()) + "\nvàng";
        return text;
    }

    private int getGoldUpgradeRequire() {
        short gold = PEA_UPGRADE[this.level - 1][3];
        return gold * (this.level <= 3 ? 1000 : 1000000);
    }

    private int getFastRespawnGemRequire() {
        return FAST_RESPAWN_GEM[Math.min(this.level - 1, FAST_RESPAWN_GEM.length - 1)];
    }

    private int getFastUpgradeGemRequire() {
        return FAST_UPGRADE_GEM[Math.min(this.level - 1, FAST_UPGRADE_GEM.length - 1)];
    }

    private int getFertilizeXuRequire() {
        return FERTILIZE_XU_NRO[Math.min(this.level - 1, FERTILIZE_XU_NRO.length - 1)];
    }

    private int getFertilizeMinuteReduce() {
        return FERTILIZE_MINUTE_REDUCE[Math.min(this.level - 1, FERTILIZE_MINUTE_REDUCE.length - 1)];
    }

    private String formatGold(int gold) {
        if (gold >= 1000000) {
            return (gold / 1000000) + " Tr";
        }
        return (gold / 1000) + " k";
    }

    private long getTimeUpgrade() {
        short d = PEA_UPGRADE[this.level - 1][0];
        short h = PEA_UPGRADE[this.level - 1][1];
        short m = PEA_UPGRADE[this.level - 1][2];
        return d * 24 * 60 * 60 * 1000L + h * 60 * 60 * 1000L + m * 60 * 1000L;
    }

    public void dispose() {
        this.player = null;
    }

    public int addPeaHarvest(byte level, int quantity) {
        Item pea = ItemService.gI().createNewItem(MagicTree.PEA_TEMP[level - 1], quantity);
        pea.itemOptions.add(new Item.ItemOption(level - 1 > 1 ? 2 : 48, MagicTree.PEA_PARAM[level - 1]));
        InventoryService.gI().addItemBag(player, pea);
        if (pea.quantity > 0) {
            InventoryService.gI().addItemBox(player, pea);
        }
        if (pea.quantity < quantity) {
            Service.gI().sendThongBao(player,
                    "Bạn vừa thu hoạch được " + (quantity - pea.quantity) + " hạt " + pea.template.name);
        }
        return pea.quantity;
    }

}
