package boss.boss_manifest.Training;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import consts.ConstRatio;
import boss.Boss;
import boss.BossData;
import boss.OtherBossManager;
import boss.BossStatus;
import boss.BossType;
import boss.BossID;
import network.Message;
import consts.ConstPlayer;
import java.io.IOException;
import models.Training.TrainingService;
import nro.player.Player;
import nro.services.EffectSkillService;
import nro.services.MapService;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.SkillService;
import nro.services.ItemService;
import nro.services.InventoryService;
import item.Item;
import services.func.ChangeMapService;
import utils.Logger;
import utils.SkillUtil;
import utils.Util;

public abstract class TrainingBoss extends Boss {

    public Player playerAtt;
    protected long timeJoinMap;
    protected long lastTimeAFK;
    protected long lastTimeMove;
    public boolean doneChatS;
    public long lastTimeChat;
    protected boolean isPlayerDie;
    public long lastTimeBuff;

    public TrainingBoss(BossType ducvupro, int id, BossData data) throws Exception {
        super(ducvupro, id, data);
        this.bossStatus = BossStatus.RESPAWN;
    }

    @Override
    public void checkPlayerDie(Player pl) {
        if (pl.isDie()) {
            this.chat("Luyện tập tiếp đi");
            isPlayerDie = true;
            lastTimeAFK = System.currentTimeMillis();
            changeStatus(BossStatus.AFK);
            changeToTypeNonPK();
            Service.gI().sendPlayerVS(playerAtt, null, (byte) 0);
        }
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
            Service.gI().sendPVB(playerAtt, this, ConstPlayer.PK_PVP);
        }
        this.attack();
    }

    public void hutMau() {
    }

    public void tanHinh() {

    }

    public void bayLungTung() {

    }

    public void buffPea() {
        if (Util.canDoWithTime(lastTimeBuff, 30000)) {
            this.nPoint.hp += this.nPoint.hpMax / 5;
            this.nPoint.mp = this.nPoint.mpMax;
            Service.gI().sendInfoPlayerEatPea(this);
            lastTimeBuff = System.currentTimeMillis();
        }
    }

    protected void goToXY(int x, int y, boolean isTeleport) {
        if (!isTeleport) {
            byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
            byte move = (byte) Util.nextInt(50, 100);
            PlayerService.gI().playerMove(this, this.location.x + (dir == 1 ? move : -move), y);
        } else {
            ChangeMapService.gI().changeMapYardrat(this, this.zone, x, y);
        }
    }

    protected void goToXY(int x, int y) {
        byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
        byte move = (byte) Util.nextInt(50, 100);
        this.location.x = this.location.x + (dir == 1 ? move : -move);
        this.location.y = y;
        MapService.gI().sendPlayerMove(this);
    }

    @Override
    public void attack() {
        try {
            if (playerAtt.location != null && playerAtt != null && playerAtt.zone != null && this.zone != null
                    && this.zone.equals(playerAtt.zone)) {
                if (this.isDie()) {
                    return;
                }
                hutMau();
                tanHinh();
                bayLungTung();
                buffPea();
                this.playerSkill.skillSelect = this.playerSkill.skills
                        .get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, playerAtt) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(15, ConstRatio.PER100) && SkillUtil.isUseSkillChuong(this)) {
                        goToXY(playerAtt.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 80)),
                                Util.nextInt(10) % 2 == 0 ? playerAtt.location.y
                                        : playerAtt.location.y - Util.nextInt(0, 50),
                                false);
                    }
                    SkillService.gI().useSkill(this, playerAtt, null, -1, null);
                    checkPlayerDie(playerAtt);
                } else {
                    goToPlayer(playerAtt, false);
                }
            } else {
                this.leaveMap();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void goToPlayer(Player pl, boolean isTeleport) {
        goToXY(pl.location.x, pl.location.y, isTeleport);
    }

    @Override
    public void joinMap() {
        if (playerAtt.zone != null) {
            this.zone = playerAtt.zone;
            ChangeMapService.gI().changeMap(this, this.zone, 420, 408);
            this.changeStatus(BossStatus.CHAT_S);
        }
    }

    protected void immortalMp() {
        this.nPoint.mp = this.nPoint.mpg;
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(400, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            if (plAtt != null && plAtt.idNRNM != -1) {
                return 1;
            }

            damage = this.nPoint.subDameInjureWithDeff(damage);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }

            // if (damage > this.nPoint.hpMax / 10) {
            // damage = this.nPoint.hpMax / 10;
            // }
            this.nPoint.subHP(damage);

            if (this.nPoint.hp > 0 && this.nPoint.hp < this.nPoint.hpMax / 5) {
                if (Util.canDoWithTime(lastTimeChat, 2000)) {
                    String[] text = { "AAAAAAAAA", "ai da" };
                    this.chat(text[Util.nextInt(text.length)]);
                }
            }

            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }

            return damage;
        } else {
            return 0;
        }
    }

    protected void notifyPlayeKill(Player player) {

    }

    @Override
    public void die(Player plKill) {
        this.changeStatus(BossStatus.AFK);
        this.chatE();
        this.lastTimeAFK = System.currentTimeMillis();
        Service.gI().sendPlayerVS(playerAtt, null, (byte) 0);
        if (playerAtt.isPopoTowerChallenge && (int) this.id == BossID.MRPOPO) {
            models.PopoTower.PopoTowerService.gI().win(playerAtt);
            return;
        }
        if (playerAtt.isThachDau) {
            playerAtt.levelLuyenTap++;
            rewardTrainingChallenge(playerAtt);
        }
    }

    private void rewardTrainingChallenge(Player player) {
        int xuReward;
        long goldReward;
        String milestone;
        switch ((int) this.id) {
            case BossID.MRPOPO -> {
                xuReward = 10;
                goldReward = 10_000_000L;
                milestone = "Bạn đã vượt qua Mr.PôPô và mở khóa luyện tập với Thượng Đế!";
            }
            case BossID.THUONG_DE -> {
                xuReward = 15;
                goldReward = 20_000_000L;
                milestone = "Bạn đã vượt qua Thượng Đế, cảnh giới luyện tập mới đã mở ra!";
            }
            case BossID.THAN_VU_TRU -> {
                xuReward = 25;
                goldReward = 35_000_000L;
                milestone = "Bạn đã vượt qua Thần Vũ Trụ, sức mạnh tu luyện tăng mạnh!";
            }
            case BossID.TO_SU_KAIO -> {
                xuReward = 35;
                goldReward = 50_000_000L;
                milestone = "Bạn đã vượt qua Tổ Sư Kaio, bước vào hàng cao thủ luyện tập!";
            }
            case BossID.WHIS -> {
                xuReward = 50;
                goldReward = 100_000_000L;
                milestone = "Bạn đã vượt qua Whis, đạt đỉnh cao huấn luyện hiện tại!";
            }
            default -> {
                xuReward = 5;
                goldReward = 5_000_000L;
                milestone = "Bạn đã vượt qua thử thách luyện tập!";
            }
        }
        Item xuNro = ItemService.gI().createNewItem((short) 1705, xuReward);
        InventoryService.gI().addItemBag(player, xuNro);
        InventoryService.gI().sendItemBag(player);
        player.inventory.gold += goldReward;
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().sendThongBao(player, milestone + " Nhận " + xuReward + " Xu NRO và "
                + Util.numberToMoney(goldReward) + " vàng.");
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        Player npc = TrainingService.gI().getNonInteractiveNPC(zone, (int) this.id);
        if (npc != null) {
            this.nPoint.hp = this.nPoint.hpMax;
            Service.gI().Send_Info_NV(this);
            this.goToPlayer(npc, false);
        } else {
            Message msg;
            try {
                msg = new Message(-6);
                msg.writer().writeInt((int) this.id);
                playerAtt.sendMessage(msg);
                msg.cleanup();
                this.zone = null;
            } catch (IOException e) {
                Logger.logException(MapService.class, e);
            }
            TrainingService.gI().luyenTapEnd(playerAtt, (int) this.id);
        }

        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        OtherBossManager.gI().removeBoss(this);
        this.dispose();
    }
}
