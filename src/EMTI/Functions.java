package EMTI;

import nro.player.Player;

public final class Functions {

    private Functions() {
    }

    public static boolean isSpam(Player player, String text) {
        return utils.Functions.isSpam(player, text);
    }

    public static int maxInt(long n) {
        return utils.Functions.maxInt(n);
    }

    public static String generateRandomCharacters(int quantity) {
        return utils.Functions.generateRandomCharacters(quantity);
    }

    public static void sleep(long millis) {
        utils.Functions.sleep(millis);
    }
}
