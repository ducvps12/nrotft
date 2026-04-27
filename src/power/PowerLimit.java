package power;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * Box ZALO:https://zalo.me/g/irufas657
 * sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

@Setter
@Getter
@AllArgsConstructor
@Builder
public class PowerLimit {

    private int id;
    private long power;
    private long hp;
    private long mp;
    private long damage;
    private int defense;
    private int critical;

    public int getId() {
        return id;
    }

    public long getPower() {
        return power;
    }

    public long getHp() {
        return hp;
    }

    public long getMp() {
        return mp;
    }

    public long getDamage() {
        return damage;
    }

    public int getDefense() {
        return defense;
    }

    public int getCritical() {
        return critical;
    }
}
