package intrinsic;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/hfaysi616
 *  sdt zalo: 0372875491
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import nro.services.IntrinsicService;

public class IntrinsicPlayer {

    public byte countOpen;

    public Intrinsic intrinsic;

    public IntrinsicPlayer() {
        this.intrinsic = IntrinsicService.gI().getIntrinsicById(0);
    }

    public void dispose() {
        this.intrinsic = null;
    }
}
