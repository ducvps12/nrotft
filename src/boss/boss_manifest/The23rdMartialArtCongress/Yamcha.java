package boss.boss_manifest.The23rdMartialArtCongress;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import boss.BossID;
import boss.BossesData;
import static boss.BossType.PHOBAN;
import nro.player.Player;

public class Yamcha extends The23rdMartialArtCongress {

    public Yamcha(Player player) throws Exception {
        super(PHOBAN, BossID.YAMCHA, BossesData.YAMCHA);
        this.playerAtt = player;
    }
}
