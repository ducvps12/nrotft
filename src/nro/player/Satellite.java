package nro.player;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import utils.Util;

public class Satellite {

    public boolean isHP;
    public boolean isMP;
    public boolean isIntelligent;
    public boolean isDefend;
    public long lastHPTime;
    public long lastMPTime;
    public long lastIntelligentTime;
    public long lastDefendTime;

    public void update() {
        if (isHP && Util.canDoWithTime(lastHPTime, 3000)) {
            isHP = false;
        }
        if (isMP && Util.canDoWithTime(lastMPTime, 3000)) {
            isMP = false;
        }
        if (isIntelligent && Util.canDoWithTime(lastIntelligentTime, 3000)) {
            isIntelligent = false;
        }
        if (isDefend && Util.canDoWithTime(lastDefendTime, 3000)) {
            isDefend = false;
        }
    }
}
