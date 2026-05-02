package models.DestronGas;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import EMTI.Functions;
import boss.Boss;
import boss.boss_manifest.DestronGas.DrLychee;
import clan.Clan;
import item.Item;
import map.Zone;
import mob.Mob;
import nro.player.Player;
import nro.server.ServerNotify;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.ItemTimeService;
import nro.services.MapService;
import nro.services.PlayerService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import nro.server.Maintenance;
import nro.services.ClanService;
import nro.services.ItemMapService;
import utils.TimeUtil;

@Data
public class DestronGas implements Runnable {

    public static final long POWER_CAN_GO_TO_KHI_GAS_HUY_DIET = 2000000000;
    public static final int AVAILABLE = 50;
    public static final int TIME_KHI_GAS_HUY_DIET = 1800000;
    // bang hội đủ số người mới đc mở
    public static final int N_PLAYER_CLAN = 0;

    public int id;
    public byte level;
    public final List<Zone> zones;

    public Clan clan;
    public boolean isOpened;
    private long lastTimeOpen;
    private long lastTimeUpdateMessage;
    private boolean kickoutkghd;
    private long timeKickOutKGHD;
    public List<Boss> bosses = new ArrayList<>();
    private boolean callBoss;
    public boolean hatchiyatchDead;

    public DestronGas(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && isOpened) {
            try {
                long startTime = System.currentTimeMillis();
                update();
                long elapsedTime = System.currentTimeMillis() - startTime;
                long sleepTime = 150 - elapsedTime;
                if (sleepTime > 0) {
                    Functions.sleep(sleepTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        if (isOpened) {
            if (Util.canDoWithTime(lastTimeOpen, TIME_KHI_GAS_HUY_DIET)
                    || (kickoutkghd && Util.canDoWithTime(timeKickOutKGHD, 60000))) {
                finish();
                dispose();
                for (Zone zone : zones) {
                    for (Player pl : zone.getPlayers()) {
                        sendThanhTichKhiGas(pl);
                    }
                }
            }

            boolean allCharactersDead = true;
            for (Zone zone : zones) {
                for (Mob mob : zone.mobs) {
                    if (!mob.isDie()) {
                        allCharactersDead = false;
                        break;
                    }
                }
            }

            if (allCharactersDead && !callBoss) {
                try {
                    long bossDamage = (10000 * level);
                    long bossMaxHealth = (15000000 * level);

                    bossDamage = Util.maxIntValue((bossDamage * 1.5));
                    bossMaxHealth = Util.maxIntValue((bossMaxHealth * 1.5));
                    bosses.add(new DrLychee(
                            getMapById(148),
                            clan,
                            level,
                            bossDamage,
                            bossMaxHealth));
                    callBoss = true;
                } catch (Exception exception) {
                }
            }

            if (!kickoutkghd && (hatchiyatchDead || Util.canDoWithTime(lastTimeOpen, TIME_KHI_GAS_HUY_DIET - 60000))) {
                kickoutkghd = true;
                timeKickOutKGHD = System.currentTimeMillis();
                for (Zone zone : zones) {
                    List<Player> players = zone.getPlayers();
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Nơi này sắp nổ tung mau chạy đi");
                    }

                }
            }
            if (kickoutkghd && Util.canDoWithTime(lastTimeUpdateMessage, 10000)) {
                lastTimeUpdateMessage = System.currentTimeMillis();
                for (Zone zone : zones) {
                    List<Player> players = zone.getPlayers();
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl,
                                "Về làng Aru sau " + TimeUtil.getTimeLeft(timeKickOutKGHD, 60) + " nữa");
                    }
                }
            }

        }
    }

    public void sendThanhTichKhiGas(Player pl) {
        long timeDoneKhiGas;
        timeDoneKhiGas = System.currentTimeMillis() - pl.clan.lastTimeOpenKhiGasHuyDiet;
        int levelDoneKG;
        levelDoneKG = pl.clan.KhiGasHuyDiet.level;
        if (levelDoneKG > pl.clan.levelDoneKhiGas) {
            pl.clan.levelDoneKhiGas = levelDoneKG;
            pl.clan.thoiGianHoanThanhKhiGas = timeDoneKhiGas;
            // System.out.println("levelDoneKG: " + levelDoneKG);
            // System.out.println("timeDoneKhiGas: " + timeDoneKhiGas);
        } else if (levelDoneKG == pl.clan.levelDoneKhiGas) {
            if (timeDoneKhiGas < pl.clan.thoiGianHoanThanhKhiGas) {
                pl.clan.thoiGianHoanThanhKhiGas = timeDoneKhiGas;
            }
        }
        pl.clan.updatethanhTichKG(pl.clan.id);
        pl.clan.updatethanhTichKGForLeader();
        pl.clan.updateThongTinLeader2(pl.clan.id);
    }

    public void openKhiGasHuyDiet(Player plOpen, Clan clan, byte level) {
        try {
            this.level = level;
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = clan;
            this.clan.lastTimeOpenKhiGasHuyDiet = this.lastTimeOpen;
            this.clan.playerOpenKhiGasHuyDiet = plOpen;
            this.clan.KhiGasHuyDiet = this;
            this.callBoss = false;
            this.isOpened = true;
            this.init();
            sendTextKhiGasHuyDiet();
        } catch (Exception e) {
            plOpen.clan.lastTimeOpenKhiGasHuyDiet = 0;
            this.dispose();
        }
    }

    private void init() {
        // Hồi sinh quái
        for (Zone zone : this.zones) {
            List<Mob> mobs = zone.mobs;
            for (int i = 0; i < mobs.size(); i++) {
                Mob mob = mobs.get(i);
                if (((i == 5 || i == 10) && zone.map.mapId == 149) || ((i == 5 || i == 10 || i == 15)
                        && zone.map.mapId == 147)
                        || ((i == 5 || i == 10 || i == 15 || i == 20 || i == 25)
                                && zone.map.mapId == 152)
                        || (i == 5 && zone.map.mapId == 151)
                        || ((i == 5 || i == 10) && zone.map.mapId == 148)) {
                    mob.lvMob = 1;
                    mob.point.dame = Util.maxIntValue(level * 31 * 5 * mob.tempId * 10);
                    mob.point.maxHp = Util.maxIntValue(level * 3107 * 5 * mob.tempId * 10);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                } else {
                    mob.lvMob = mob.tempId == 76 ? 1 : 0;
                    mob.point.dame = Util.maxIntValue(level * 31 * 5 * mob.tempId);
                    mob.point.maxHp = Util.maxIntValue(level * 3107 * 5 * mob.tempId);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                }
            }
        }
        new Thread(this, "Khí Gas Hủy Diệt: " + this.clan.name).start();
    }

    // kết thúc khí gas hủy diệt
    public void finish() {
        boolean bossesCleared = hatchiyatchDead;
        for (Zone zone : zones) {
            for (int i = zone.getPlayers().size() - 1; i >= 0; i--) {
                if (i < zone.getPlayers().size()) {
                    Player pl = zone.getPlayers().get(i);
                    // Trao phần thưởng hoàn thành trước khi kick
                    rewardCompletion(pl, bossesCleared);
                    kickOutOfKGHD(pl);
                    pl.playerTask.kolTask.addCount();
                    pl.destronGas70CompletionCount++;
                }
            }
        }
        // Thông báo toàn server cho level cao
        if (bossesCleared && level >= 80 && clan != null) {
            ServerNotify.gI().notify("★ Bang " + clan.name + " đã chinh phục Destron Gas cấp " + level + "!");
        }
    }

    // ============ PHẦN THƯỞNG HOÀN THÀNH DESTRON GAS ============
    private void rewardCompletion(Player pl, boolean bossesCleared) {
        try {
            // Phần thưởng cơ bản: Vàng + Ngọc + Xu NRO (scale theo level)
            long goldReward = (long) level * 2_000_000L;
            int gemReward = Math.max(1, level / 5);
            int xuReward = Math.max(2, level / 3);

            // Bonus x2 nếu đã hạ hết boss
            if (bossesCleared) {
                goldReward *= 2;
                gemReward *= 2;
                xuReward = (int) (xuReward * 1.5);
            }

            // Trao Vàng
            pl.inventory.gold += goldReward;

            // Trao Ngọc
            pl.inventory.gem += gemReward;

            // Trao Xu NRO
            Item xuNro = ItemService.gI().createNewItem((short) 1705, xuReward);
            InventoryService.gI().addItemBag(pl, xuNro);

            // ---- PHẦN THƯỞNG ITEM THEO MỐC LEVEL ----
            StringBuilder bonus = new StringBuilder();

            // Level 20+: Cơ hội nhận Thỏi vàng (457)
            if (level >= 20) {
                int thoiVangChance = Math.min(30, 10 + level / 5);
                if (Util.nextInt(100) < thoiVangChance) {
                    int slTV = Util.nextInt(1, Math.max(1, level / 20));
                    Item tv = ItemService.gI().createNewItem((short) 457, slTV);
                    InventoryService.gI().addItemBag(pl, tv);
                    bonus.append(", ").append(slTV).append(" Thỏi vàng");
                }
            }

            // Level 40+: Cơ hội nhận Capsule dây chuyền (192)
            if (level >= 40) {
                if (Util.nextInt(100) < 20) {
                    Item capsule = ItemService.gI().createNewItem((short) 192, Util.nextInt(1, 2));
                    InventoryService.gI().addItemBag(pl, capsule);
                    bonus.append(", Capsule dây chuyền");
                }
            }

            // Level 60+: Cơ hội nhận Sách kỹ năng (215) + Hộp SKH (860)
            if (level >= 60) {
                if (Util.nextInt(100) < 12) {
                    Item skn = ItemService.gI().createNewItem((short) 215, 1);
                    InventoryService.gI().addItemBag(pl, skn);
                    bonus.append(", Sách kỹ năng");
                }
                if (Util.nextInt(100) < 8) {
                    Item hop = ItemService.gI().createNewItem((short) 860, 1);
                    InventoryService.gI().addItemBag(pl, hop);
                    bonus.append(", Hộp SKH");
                }
            }

            // Level 80+: Cơ hội nhận Mảnh bông tai (441) + Sách TK2 (456)
            if (level >= 80) {
                if (Util.nextInt(100) < 8) {
                    Item mbt = ItemService.gI().createNewItem((short) 441, 1);
                    InventoryService.gI().addItemBag(pl, mbt);
                    bonus.append(", Mảnh bông tai Porata");
                }
                if (Util.nextInt(100) < 3) {
                    Item stk = ItemService.gI().createNewItem((short) 456, 1);
                    InventoryService.gI().addItemBag(pl, stk);
                    bonus.append(", Sách TK2");
                }
            }

            // Level 100+: Cơ hội nhận item cực hiếm
            if (level >= 100 && bossesCleared) {
                int superRare = Util.nextInt(1000);
                if (superRare < 5) { // 0.5% Pet Po
                    Item pet = ItemService.gI().createNewItem((short) 1667, 1);
                    InventoryService.gI().addItemBag(pl, pet);
                    bonus.append(", ★Pet Po★");
                    ServerNotify.gI().notify("★ " + pl.name + " nhận được Pet Po từ Destron Gas cấp " + level + "!");
                } else if (superRare < 15) { // 1% Capsule thú cưỡi
                    Item thuCuoi = ItemService.gI().createNewItem((short) 193, 1);
                    InventoryService.gI().addItemBag(pl, thuCuoi);
                    bonus.append(", ★Capsule thú cưỡi★");
                }
            }

            InventoryService.gI().sendItemBag(pl);
            PlayerService.gI().sendInfoHpMpMoney(pl);

            String msg = "★ Hoàn thành Destron Gas Lv." + level
                    + (bossesCleared ? " (Clear Boss!)" : "") + "\n"
                    + "Nhận: " + xuReward + " Xu NRO, " + gemReward + " ngọc, "
                    + Util.numberToMoney(goldReward) + " vàng"
                    + bonus;
            Service.gI().sendThongBao(pl, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void kickOutOfKGHD(Player player) {
        if (MapService.gI().isMapKhiGasHuyDiet(player.zone.map.mapId)) {
            ChangeMapService.gI().changeMapBySpaceShip(player, 0, -1, -1);
            sendThanhTichKhiGas(player);
        }
    }

    public Zone getMapById(int mapId) {
        for (Zone zone : this.zones) {
            if (zone.map.mapId == mapId) {
                return zone;
            }
        }
        return null;
    }

    private void sendTextKhiGasHuyDiet() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextKhiGasHuyDiet(pl);
        }
    }

    private void removeTextKhiGasHuyDiet() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextKhiGasHuyDiet(pl);
        }
    }

    public void dispose() {
        for (Zone zone : zones) {
            for (int i = zone.items.size() - 1; i >= 0; i--) {
                if (i < zone.items.size()) {
                    ItemMapService.gI().removeItemMap(zone.items.get(i));
                }
            }
        }
        for (Boss boss : bosses) {
            if (!boss.isDie()) {
                boss.leaveMap();
            }
        }
        this.removeTextKhiGasHuyDiet();
        this.bosses.clear();
        this.isOpened = false;
        this.clan.KhiGasHuyDiet = null;
        this.clan = null;
        this.kickoutkghd = false;
        this.hatchiyatchDead = false;
    }
}
