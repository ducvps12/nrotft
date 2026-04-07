package Bot;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import nro.services.ChatGlobalService;
import utils.Logger;

/**
 * Bot chat thế giới (BOT TG)
 * - Tích hợp AI: Bot tự sinh nội dung hội thoại ngẫu nhiên.
 * - Mỗi bot chat 1 câu mỗi lượt, cách nhau đúng 1 phút.
 * - Các bot nói chuyện qua lại như người chơi thật.
 *
 * @author hoquo
 */
public class BotChatTG implements Runnable {

    private final Bot bot;
    private volatile boolean running = true;

    // Biến toàn cục kiểm soát nhịp chat
    private static volatile long lastChatTime = 0;
    private static final Object CHAT_LOCK = new Object();

    public BotChatTG(Bot bot) {
        this.bot = bot;
        startChatThread();
    }

    private void startChatThread() {
        Thread.startVirtualThread(this);
    }

    @Override
    public void run() {
        while (running && bot != null) {
            try {
                synchronized (CHAT_LOCK) {
                    long now = System.currentTimeMillis();
                    long waitTime = 60_000 - (now - lastChatTime);
                    if (waitTime > 0) {
                        TimeUnit.MILLISECONDS.sleep(waitTime);
                    }

                    lastChatTime = System.currentTimeMillis();
                }

                // 🧠 Sinh câu chat kiểu AI
                String msg = BotAIGenerator.generate("");
                // Gửi chat toàn server
                ChatGlobalService.gI().autoChatGlobal(null, "[" + bot.name + "] " + msg);
                // Mỗi bot đợi thêm 1 phút
                TimeUnit.MILLISECONDS.sleep(60_000);

            } catch (Exception e) {
                Logger.logException(BotChatTG.class, e, "Lỗi BotChatTG");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void stop() {
        running = false;
    }

    // 🟡 Chế độ toàn server (AI chat luân phiên)
    private static volatile boolean globalRunning = false;

    public static void start() {
        if (globalRunning) {
            return;
        }
        globalRunning = true;

        Thread.startVirtualThread(() -> {

            while (globalRunning) {
                try {
                    synchronized (CHAT_LOCK) {
                        long now = System.currentTimeMillis();
                        long waitTime = 60_000 - (now - lastChatTime);
                        if (waitTime > 0) {
                            TimeUnit.MILLISECONDS.sleep(waitTime);
                        }
                        lastChatTime = System.currentTimeMillis();
                    }

                    String msg = BotAIGenerator.generate("");
                    ChatGlobalService.gI().autoChatGlobal(null, "[BOT TG] " + msg);

                } catch (Exception e) {
                    Logger.logException(BotChatTG.class, e, "Lỗi BOT TG toàn server");
                }
            }
        });
    }
}
