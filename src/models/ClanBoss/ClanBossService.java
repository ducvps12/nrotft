package models.ClanBoss;

import map.Zone;
import nro.player.Player;
import nro.services.Service;
import utils.Util;

import java.util.*;

public class ClanBossService {

    private static ClanBossService instance;

    public static ClanBossService gI() {
        if (instance == null) {
            instance = new ClanBossService();
        }
        return instance;
    }

    private final Map<Zone, ClanBoss> clanBossMaps = new HashMap<>();

    private static final int[] FLAGS = { 1, 2, 3, 4, 5, 6, 7, 8 };

    public void addMapClanBoss(Zone zone) {
        if (zone == null || clanBossMaps.containsKey(zone)) {
            return;
        }
        clanBossMaps.put(zone, new ClanBoss(zone));
    }

    public void joinMapClanBoss(Player player) {
        if (player == null || player.zone == null || player.isBoss) {
            return;
        }

        List<Player> players = player.zone.getPlayers();

        if (player.clan != null) {
            for (Player pl : players) {
                if (pl != player
                        && pl.clan != null
                        && pl.clan.id == player.clan.id) {
                    Service.gI().changeFlag(player, pl.cFlag);
                    return;
                }
            }
        }

        Set<Integer> used = new HashSet<>();
        for (Player pl : players) {
            if (pl.cFlag > 0) {
                used.add((int) pl.cFlag);
            }
        }

        for (int f : FLAGS) {
            if (!used.contains(f)) {
                Service.gI().changeFlag(player, f);
                return;
            }
        }

        Service.gI().changeFlag(player, FLAGS[Util.nextInt(0, FLAGS.length - 1)]);
    }
}
