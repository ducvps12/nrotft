package boss.boss_manifest.Training;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import boss.BossID;
import boss.BossStatus;
import boss.BossesData;
import static boss.BossType.PHOBAN;
import nro.player.Player;
import services.func.ChangeMapService;
import utils.Util;

public class MrPoPo extends TrainingBoss {

    private long lastTimeBay;
    private long lastTimeBay2;

    public MrPoPo(Player player) throws Exception {
        super(PHOBAN, BossID.MRPOPO, BossesData.MRPOPO);
        this.playerAtt = player;
        if (player.isPopoTowerChallenge) {
            int rate = models.PopoTower.PopoTowerService.gI().getBossPowerRate(player);
            this.nPoint.hpMax = Math.max(this.nPoint.hpMax, player.nPoint.hpMax * rate / 80);
            this.nPoint.hpg = this.nPoint.hpMax;
            this.nPoint.hp = this.nPoint.hpMax;
            this.nPoint.mpg = Math.max(this.nPoint.mpg, player.nPoint.mpMax * rate / 100);
            this.nPoint.mp = this.nPoint.mpg;
            this.nPoint.dameg = Math.max(this.nPoint.dameg, player.nPoint.dame * rate / 120);
            this.name = "Mr.PôPô Tầng " + player.popoTowerChallengeFloor;
        }
    }

    @Override
    public void joinMap() {
        if (playerAtt.zone != null) {
            this.zone = playerAtt.zone;
            ChangeMapService.gI().changeMap(this, this.zone, 295, 408);
            this.changeStatus(BossStatus.CHAT_S);
        }
    }

    @Override
    public void afk() {
        if (Util.canDoWithTime(lastTimeAFK, 15000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
    }

    @Override
    public boolean chatS() {
        if (Util.canDoWithTime(lastTimeChatS, timeChatS)) {
            if (this.doneChatS) {
                return true;
            }
            String textChat = this.data[this.currentLevel].getTextS()[playerAtt.isThachDau ? 1 : 0];
            int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
            textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
            if (!this.chat(prefix, textChat)) {
                return false;
            }
            this.moveToPlayer(playerAtt);
            this.lastTimeChatS = System.currentTimeMillis();
            this.timeChatS = 100;
            doneChatS = true;
        }
        return false;
    }

    @Override
    public void bayLungTung() {
        if (Util.canDoWithTime(lastTimeBay, 3000)) {
            goToXY(playerAtt.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 80)),
                    this.location.y + Util.getOne(-100, 10), false);
            lastTimeBay = System.currentTimeMillis();
        }
        if (Util.canDoWithTime(lastTimeBay2, 4000)) {
            goToXY(playerAtt.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 80)),
                    this.location.y + Util.getOne(-100, 10), false);
            lastTimeBay2 = System.currentTimeMillis();
        }
    }

}
