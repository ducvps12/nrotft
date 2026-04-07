package power;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * Box ZALO:https://zalo.me/g/hfaysi616
 * sdt zalo: 0372875491
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
}
