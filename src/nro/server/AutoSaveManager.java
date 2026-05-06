package nro.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jdbc.daos.PlayerDAO;
import network.SessionManager;

public class AutoSaveManager {

    private static AutoSaveManager instance = null;

    private ScheduledExecutorService scheduler;

    private AutoSaveManager() {
    }

    public static synchronized AutoSaveManager getInstance() {
        if (instance == null) {
            instance = new AutoSaveManager();
        }
        return instance;
    }

    public void startAutoSave() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
        scheduler.scheduleAtFixedRate(() -> {
            try {
                handleAutoSave();
            } catch (Exception ignored) {
            }
        }, 60, 90, TimeUnit.SECONDS);
    }

    public void stopAutoSave() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public void handleAutoSave() {
        Client.gI().getPlayers().forEach(player -> {
            PlayerDAO.updatePlayer(player);
        });
    }

    /**
     * Lưu tất cả player ngay lập tức (đồng bộ) — dùng khi bảo trì/shutdown
     */
    public void saveAllNow() {
        try {
            var players = Client.gI().getPlayers();
            int count = 0;
            for (var player : players) {
                if (player != null) {
                    try {
                        PlayerDAO.updatePlayer(player);
                        count++;
                    } catch (Exception e) {
                        System.err.println("[AutoSave] Lỗi save player " + player.name + ": " + e.getMessage());
                    }
                }
            }
            System.out.println("[AutoSave] Đã lưu " + count + "/" + players.size() + " người chơi.");
        } catch (Exception e) {
            System.err.println("[AutoSave] Lỗi saveAllNow: " + e.getMessage());
        }
    }

    public void dispose() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
    }
}
