package models.Achievement;

/**
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import consts.ConstAchievement;
import item.Item;
import jdbc.daos.PlayerDAO;
import mob.Mob;
import models.Template.AchievementTemplate;
import network.Message;
import nro.player.Player;
import nro.server.Manager;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import skill.Skill;
import utils.Util;

public class AchievementService {

    private static AchievementService instance;

    public static AchievementService gI() {
        if (instance == null) {
            instance = new AchievementService();
        }
        return instance;
    }

    public void openAchievementUI(Player player) {
        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(0);
            msg.writer().writeByte(Manager.ACHIEVEMENT_TEMPLATE.size());
            for (int i = 0; i < Manager.ACHIEVEMENT_TEMPLATE.size(); i++) {
                AchievementTemplate at = Manager.ACHIEVEMENT_TEMPLATE.get(i);
                msg.writer().writeUTF(at.info1); // info 1
                msg.writer().writeUTF(regex(player, at.info2) + " ("
                        + numberToString(player.achievement.getCompleted(i)) + "/" + numberToString(at.maxCount) + ")"); // info
                                                                                                                         // 2
                msg.writer().writeShort(at.money); // money
                msg.writer().writeBoolean(player.achievement.isFinish(i, at.maxCount));// isFinish
                msg.writer().writeBoolean(player.achievement.isRecieve(i)); // isRecieve
            }
            player.sendMessage(msg);
            player.typeRecvieArchiment = 0;
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void confirmAchievement(Player player, byte select) {
        if (player.achievement == null) {
            return;
        }

        // Kiểm tra index hợp lệ
        if (select < 0 || select >= Manager.ACHIEVEMENT_TEMPLATE.size()) {
            Service.gI().sendThongBao(player, "Thành tích không hợp lệ!");
            return;
        }

        AchievementTemplate at = Manager.ACHIEVEMENT_TEMPLATE.get(select);
        long completed = player.achievement.getCompleted(select);

        // Thông báo chi tiết nếu chưa đủ điều kiện
        if (completed < at.maxCount) {
            String rankName = getRankName(player);
            String progressPercent = String.format("%.1f", (completed * 100.0 / at.maxCount));

            Service.gI().sendThongBao(player,
                    "|7|━━━ CHƯA ĐỦ ĐIỀU KIỆN ━━━\n"
                    + "|1|Thành tích: " + at.info1 + "\n"
                    + "|2|Yêu cầu: " + regex(player, at.info2) + "\n"
                    + "|8|Tiến độ: " + numberToString(completed) + "/" + numberToString(at.maxCount)
                    + " (" + progressPercent + "%)\n"
                    + "|6|Rank hiện tại: " + rankName + "\n"
                    + "|1|Sức mạnh: " + Util.numberToMoney(player.nPoint.power) + "\n"
                    + "|7|━━━━━━━━━━━━━━━━━━\n"
                    + "|3|Cố lên chiến binh! Hoàn thành để nhận "
                    + at.money + " Hồng Ngọc!");
            return;
        }

        // Kiểm tra đã nhận rồi chưa
        if (player.achievement.isRecieve(select)) {
            Service.gI().sendThongBao(player,
                    "|7|━━━ ĐÃ NHẬN RỒI ━━━\n"
                    + "|1|Thành tích: " + at.info1 + "\n"
                    + "|8|Bạn đã nhận thưởng thành tích này trước đó.\n"
                    + "|7|━━━━━━━━━━━━━━━━━━");
            return;
        }

        // Đủ điều kiện, chưa nhận → phát thưởng
        int money = at.money;
        player.achievement.reward(select);
        player.inventory.ruby += money;
        Service.gI().sendMoney(player);

        // Cập nhật tiến trình Danh Hiệu khi nhận thưởng thành tích
        task.Badges.BadgesTaskService.updateCountBagesTask(player, consts.ConstTaskBadges.NONG_DAN_CHAM_CHI, 1);
        task.Badges.BadgesTaskService.updateDoneTask(player);

        String rankName = getRankName(player);
        Service.gI().sendThongBao(player,
                "|7|━━━ NHẬN THƯỞNG THÀNH CÔNG ━━━\n"
                + "|1|Thành tích: " + at.info1 + "\n"
                + "|8|Phần thưởng: +" + money + " Hồng Ngọc\n"
                + "|6|Rank hiện tại: " + rankName + "\n"
                + "|2|Tiến trình Danh Hiệu đã được cập nhật!\n"
                + "|7|━━━━━━━━━━━━━━━━━━");

        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(1);
            msg.writer().writeByte(select);
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Lấy tên rank dựa trên sức mạnh và chủng tộc
     */
    private String getRankName(Player player) {
        try {
            power.CaptionManager cm = power.CaptionManager.getInstance();
            int level = cm.getLevel(player);
            power.Caption cap = cm.findLevel(level);
            if (cap != null) {
                switch (player.gender) {
                    case 0: return cap.getEarth();
                    case 1: return cap.getNamek();
                    case 2: return cap.getSaiya();
                    default: return cap.getEarth();
                }
            }
        } catch (Exception e) {
            // fallback nếu CaptionManager chưa load
        }

        // Fallback nếu CaptionManager lỗi
        long power = player.nPoint.power;
        if (power >= 15_000_000L) return "Siêu cấp+";
        if (power >= 1_500_000L) return "Vệ binh hoàng gia";
        if (power >= 700_000L) return "Vệ binh";
        if (power >= 340_000L) return "Chiến binh cao cấp";
        if (power >= 170_000L) return "Chiến binh";
        if (power >= 90_000L) return "Tân binh";
        return "Tập sự";
    }

    public String numberToString(long num) {
        return num <= 10000 ? num + "" : Util.numberToMoney(num);
    }

    public String regex(Player player, String text) {
        int gen = player.gender;
        return text.replaceAll("%1", gen == 0 ? "Siêu nhân" : gen == 1 ? "Siêu Namếc" : "Siêu Xayda").replaceAll("%2",
                gen == 0 ? "Bunma" : gen == 1 ? "Dende" : "Appule");
    }

    public void checkDoneTask(Player player, int aId) {
        if (player.isPl() && player.achievement != null) {
            switch (aId) {
                case ConstAchievement.LAN_DAU_NAP_NGOC ->
                    player.achievement.doneNotAdd(aId, player.getSession().danap);
                default ->
                    player.achievement.done(aId, 1);
            }
        }
    }

    public void checkDoneTaskKillMob(Player player, Mob mob) {
        try {
            if (mob.lvMob > 0) {
                checkDoneTask(player, ConstAchievement.DANH_BAI_SIEU_QUAI);
            }
            if (mob.type == 4) {
                checkDoneTask(player, ConstAchievement.THO_SAN_THIEN_XA);
            }
            if (mob.tempId == 0) {
                checkDoneTask(player, ConstAchievement.TAP_LUYEN_BAI_BAN);
            }
        } catch (Exception e) {
        }
    }

    public void checkDoneTaskUseSkill(Player player) {
        if (player.isPl()) {
            switch (player.playerSkill.skillSelect.template.id) {
                case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC -> {
                    checkDoneTask(player, ConstAchievement.NOI_CONG_CAO_CUONG);
                }
                case Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN,
                        Skill.DICH_CHUYEN_TUC_THOI ->
                    {
                    }
                default ->
                    checkDoneTask(player, ConstAchievement.KY_NANG_THANH_THAO);
            }
        }
    }

    public void checkDoneTaskFly(Player player, int length) {
        if (player.isPl() && player.achievement != null) {
            length = Math.abs(length / 10);
            if (length < 10) {
                player.achievement.done(ConstAchievement.KHINH_CONG_THANH_THAO, length);
            }
        }
    }

}
