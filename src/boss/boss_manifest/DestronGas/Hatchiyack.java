package boss.boss_manifest.DestronGas;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import consts.ConstPlayer;
import boss.*;
import static boss.BossType.PHOBANKGHD;
import clan.Clan;
import item.Item;
import map.ItemMap;
import map.Zone;
import nro.player.Player;
import nro.services.EffectSkillService;
import skill.Skill;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Util;

public class Hatchiyack extends Boss {

    private final int level;
    private Clan clan;

    private static final int[][] FULL_DEMON = new int[][] { { Skill.DEMON, 1 }, { Skill.DEMON, 2 }, { Skill.DEMON, 3 },
            { Skill.DEMON, 4 }, { Skill.DEMON, 5 }, { Skill.DEMON, 6 }, { Skill.DEMON, 7 } };

    public Hatchiyack(Zone zone, Clan clan, int level, long dame, long hp) throws Exception {
        super(PHOBANKGHD, BossID.HATCHIYACK, new BossData(
                " ",
                ConstPlayer.TRAI_DAT,
                new short[] { 639, 640, 641, -1, -1, -1 },
                (dame),
                new long[] { hp },
                new int[] { 148 },
                (int[][]) Util.addArray(FULL_DEMON),
                new String[] { "|-1|Các ngươi dám hạ sư phụ ta",
                        "|-1|Ta sẽ tiêu diệt hết các ngươi" },
                new String[] { "|-1|Đại bác báo thù...",
                        "|-1|Heyyyyyyyy Yaaaaa" },
                new String[] { "|-1|Các ngươi khó mà rời khỏi nơi đây" },
                60));
        this.zone = zone;
        this.level = level;
        this.clan = clan;
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.level + 10, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            if (plAtt != null && plAtt.idNRNM != -1) {
                return 1;
            }

            damage = this.nPoint.subDameInjureWithDeff(damage + Util.nextInt(-200 * this.level, 0));

            damage -= damage / 100 * (this.level / 5);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }

            this.nPoint.subHP(damage);

            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }

            return damage;
        } else {
            return 0;
        }
    }

    @Override
    public void reward(Player plKill) {
        // Drop Cải trang cho tất cả player
        dropCt(0);
        for (int i = 0; i < this.zone.getNumOfPlayers(); i++) {
            int x = (i + 1) * 50;
            dropCt(x);
            dropCt(-x);
        }
        
        // Boss cuối - Thưởng lớn cho tất cả player trong zone
        for (Player pl : this.zone.getPlayers()) {
            try {
                // Vàng thưởng x2 so với DrLychee
                long goldBonus = (long) level * 3_000_000L;
                pl.inventory.addGoldSafe(goldBonus);
                
                // Ngọc thưởng 
                int gemBonus = Math.max(2, level / 5);
                pl.inventory.gem += gemBonus;
                
                // Xu NRO
                int xuBonus = Math.max(3, level / 8);
                Item xuNro = nro.services.ItemService.gI().createNewItem((short) 1705, xuBonus);
                nro.services.InventoryService.gI().addItemBag(pl, xuNro);
                
                StringBuilder extra = new StringBuilder();
                
                // Thỏi vàng (level 40+, 25% chance)
                if (level >= 40 && Util.nextInt(100) < 25) {
                    int slTV = Util.nextInt(1, Math.max(1, level / 25));
                    Item thoiVang = nro.services.ItemService.gI().createNewItem((short) 457, slTV);
                    nro.services.InventoryService.gI().addItemBag(pl, thoiVang);
                    extra.append(", ").append(slTV).append(" Thỏi vàng");
                }
                
                // Hộp SKH (level 60+, 10% chance)
                if (level >= 60 && Util.nextInt(100) < 10) {
                    Item hop = nro.services.ItemService.gI().createNewItem((short) 860, 1);
                    nro.services.InventoryService.gI().addItemBag(pl, hop);
                    extra.append(", Hộp SKH");
                }
                
                // Mảnh bông tai (level 80+, 5% chance)
                if (level >= 80 && Util.nextInt(100) < 5) {
                    Item mbt = nro.services.ItemService.gI().createNewItem((short) 441, 1);
                    nro.services.InventoryService.gI().addItemBag(pl, mbt);
                    extra.append(", Mảnh BT Porata");
                }
                
                // Sách TK2 (level 100+, 2% chance)
                if (level >= 100 && Util.nextInt(100) < 2) {
                    Item stk = nro.services.ItemService.gI().createNewItem((short) 456, 1);
                    nro.services.InventoryService.gI().addItemBag(pl, stk);
                    extra.append(", ★Sách TK2★");
                    nro.server.ServerNotify.gI().notify("★ " + pl.name + " nhận Sách TK2 từ Hatchiyack Lv." + level + "!");
                }
                
                nro.services.InventoryService.gI().sendItemBag(pl);
                nro.services.PlayerService.gI().sendInfoHpMpMoney(pl);
                Service.gI().sendThongBao(pl, "★ Hạ Hatchiyack! Nhận " + xuBonus + " Xu NRO, "
                        + gemBonus + " ngọc, " + Util.numberToMoney(goldBonus) + " vàng" + extra);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Thông báo server
        if (level >= 90 && plKill != null) {
            nro.server.ServerNotify.gI().notify("★ " + plKill.name + " và đồng đội hạ Hatchiyack Lv." + level + "!");
        }
    }

    private void dropCt(int x) {
        try {
            ItemMap it = new ItemMap(zone, 729, 1, this.location.x + x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), -1);
            it.options.clear();
            int ParamMax = (int) 11 + (level / 4) - (level > 55 ? Util.nextInt(level / 10) : 0);
            if (ParamMax < 3) {
                ParamMax = 3;
            }
            int ParamMin = ParamMax - 3;
            if (ParamMin < 3) {
                ParamMin = 3;
            }
            int ParamMaxSDCM = ParamMax < 41 ? ParamMax : 40;
            int ParamMinSDCM = ParamMaxSDCM - 3;
            if (ParamMinSDCM < 3) {
                ParamMinSDCM = 3;
            }
            int hsd = Util.nextInt(ParamMin, ParamMax);
            it.options.add(new Item.ItemOption(50, Util.nextInt(ParamMin, ParamMax)));
            it.options.add(new Item.ItemOption(77, Util.nextInt(ParamMin, ParamMax)));
            it.options.add(new Item.ItemOption(103, Util.nextInt(ParamMin, ParamMax)));
            it.options.add(new Item.ItemOption(5, Util.nextInt(ParamMin, ParamMax)));
            it.options.add(new Item.ItemOption(93, hsd > 21 ? 21 : hsd));
            it.options.add(new Item.ItemOption(30, 0));
            Service.gI().dropItemMap(this.zone, it);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void joinMap() {
        ChangeMapService.gI().changeMap(this, this.zone, 480, 295);
        this.moveTo(480, 480);
        this.changeStatus(BossStatus.CHAT_S);
    }

    @Override
    public void die(Player plKill) {
        if (plKill != null) {
            reward(plKill);
        }
        this.changeStatus(BossStatus.DIE);
    }

    @Override
    public void leaveMap() {
        if (clan != null && clan.KhiGasHuyDiet != null) {
            clan.KhiGasHuyDiet.hatchiyatchDead = true;
        }
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        GasDestroyManager.gI().removeBoss(this);
        this.dispose();
    }
}
