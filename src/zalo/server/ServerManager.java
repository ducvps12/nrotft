
package zalo.server;

public class ServerManager {

    public static void main(String[] args) {
        try {
            Bot bot = new Bot();
            bot.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down bot...");
                bot.stop();
            }));
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
