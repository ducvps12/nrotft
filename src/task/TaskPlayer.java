package task;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */
public class TaskPlayer {

    public TaskMain taskMain;

    public SideTask sideTask;

    public ClanTask clanTask;

    public KolTask kolTask;

    public TaskPlayer() {
        this.sideTask = new SideTask();
        this.clanTask = new ClanTask();
        this.kolTask = new KolTask();
    }

    public void dispose() {
        this.taskMain = null;
        this.sideTask = null;
        this.clanTask = null;
        if (this.kolTask != null) {
            this.kolTask.dispose();
            this.kolTask = null;
        }
    }
}
