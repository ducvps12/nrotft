package boss.boss_manifest.The23rdMartialArtCongress;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/hfaysi616
 *  sdt zalo: 0372875491
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import boss.BossID;
import boss.BossesData;
import static boss.BossType.PHOBAN;
import nro.player.Player;

public class ChaPa extends The23rdMartialArtCongress {

    public ChaPa(Player player) throws Exception {
        super(PHOBAN, BossID.CHA_PA, BossesData.CHA_PA);
        this.playerAtt = player;
    }
}
