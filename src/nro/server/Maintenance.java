package nro.server;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import utils.Functions;
import nro.services.Service;
import utils.Logger;

public class Maintenance extends Thread {

    public static boolean isRunning = false;

    private static Maintenance i;

    private int time;

    private Maintenance() {

    }

    public static Maintenance gI() {
        if (i == null) {
            i = new Maintenance();
        }
        return i;
    }

    public void start(int min) {
        if (!isRunning) {
            isRunning = true;
            this.time = min;
            this.start();
        }
    }

    public void startNew(int min) {
        if (!isRunning) {
            isRunning = true;
            this.time = min;
            Thread.startVirtualThread(() -> Maintenance.gI().run());
        }
    }

    public void startImmediately() {
        if (!isRunning) {
            isRunning = true;
            Logger.log(Logger.YELLOW, "BEGIN MAINTENANCE (IMMEDIATE)\n");
            saveAllDataAndShutdown();
        }
    }

    @Override
    public void run() {
        while (this.time > 0) {
            if (this.time == 60) {
                Service.gI().sendThongBaoAllPlayer(
                        "Hệ thống sẽ bảo trì sau 1 phút nữa hãy thoát game ngay để tránh mất mát vật phẩm.");
                try {
                    Functions.sleep(1000);
                } catch (Exception e) {
                }
                this.time--;
            } else if (time < 60) {
                Service.gI().sendThongBaoAllPlayer("Hệ thống sẽ bảo trì sau " + time + " giây nữa");
                try {
                    Functions.sleep(1000);
                } catch (Exception e) {
                }
                this.time--;
            } else {
                int hour = this.time / 3600;
                int min = (this.time - hour * 3600) / 60;
                int sec = this.time % 60;

                String hourStr = (hour > 0) ? hour + " giờ " : "";
                String minStr = (min > 0) ? min + " phút " : "";
                String secStr = (sec > 0) ? sec + " giây " : "";

                Service.gI().sendThongBaoAllPlayer("Hệ thống sẽ bảo trì sau " + hourStr + minStr + secStr
                        + "nữa");
                Logger.log(Logger.YELLOW, "Hệ thống sẽ bảo trì sau " + hourStr + minStr + secStr
                        + "nữa\n");
                if (sec == 0 && this.time > 60) {
                    sec = 60;
                } else if (sec == 0) {
                    sec = 1;
                }
                this.time -= sec;
                try {
                    Functions.sleep(sec * 1000);
                } catch (Exception e) {
                }
            }
        }

        // === THỜI GIAN ĐÃ HẾT — BẮT ĐẦU TẮT SERVER ===
        saveAllDataAndShutdown();
    }

    /**
     * Lưu toàn bộ dữ liệu và tắt server (KHÔNG restart)
     */
    private void saveAllDataAndShutdown() {
        Logger.log(Logger.YELLOW, "═══════════════════════════════════\n");
        Logger.log(Logger.YELLOW, "  BẢO TRÌ: Đang lưu dữ liệu...\n");
        Logger.log(Logger.YELLOW, "═══════════════════════════════════\n");

        // Thông báo cuối cùng cho player
        try {
            Service.gI().sendThongBaoAllPlayer(
                    "Server đang bảo trì. Dữ liệu đang được lưu. Vui lòng đợi thông báo mở lại!");
        } catch (Exception e) {
            // Player đã disconnect, bỏ qua
        }

        // Lưu tất cả player data qua AutoSaveManager
        try {
            Logger.log(Logger.YELLOW, "MAINTENANCE: Đang AutoSave tất cả người chơi...\n");
            AutoSaveManager.getInstance().saveAllNow();
            Logger.log(Logger.GREEN, "MAINTENANCE: Đã AutoSave xong tất cả người chơi.\n");
        } catch (Exception e) {
            Logger.error("MAINTENANCE: Lỗi AutoSave - " + e.getMessage() + "\n");
        }

        // Chờ 2 giây để đảm bảo data flush xong
        try {
            Functions.sleep(2000);
        } catch (Exception e) {
        }

        // Gọi ServerManager.close() để shutdown hoàn toàn
        Logger.log(Logger.YELLOW, "MAINTENANCE: Bắt đầu shutdown server...\n");
        ServerManager.gI().close();
    }
}
