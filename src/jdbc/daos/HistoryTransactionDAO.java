package jdbc.daos;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/irufas657
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import jdbc.DBConnecter;
import item.Item;
import nro.player.Player;
import utils.TimeUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class HistoryTransactionDAO {

    public static void insert(Player pl1, Player pl2,
            int goldP1, int goldP2, List<Item> itemP1, List<Item> itemP2,
            List<Item> bag1Before, List<Item> bag2Before,
            List<Item> bag1After,
            List<Item> bag2After,
            long gold1Before, long gold2Before, long gold1After, long gold2After) {

        String player1 = pl1.name + " (" + pl1.id + ")";
        String player2 = pl2.name + " (" + pl2.id + ")";
        String itemPlayer1 = "Gold: " + goldP1 + ", ";
        String itemPlayer2 = "Gold: " + goldP2 + ", ";
        List<Item> doGD1 = new ArrayList<>();
        List<Item> doGD2 = new ArrayList<>();
        for (Item item : itemP1) {
            if (item.isNotNullItem() && doGD1.stream().noneMatch(item1 -> item1.template.id == item.template.id)) {
                doGD1.add(item);
            } else if (item.isNotNullItem()) {
                doGD1.stream().filter(item1 -> item1.template.id == item.template.id).findFirst()
                        .get().quantityGD += item.quantityGD;
            }
        }
        for (Item item : itemP2) {
            if (item.isNotNullItem() && doGD2.stream().noneMatch(item1 -> item1.template.id == item.template.id)) {
                doGD2.add(item);
            } else if (item.isNotNullItem()) {
                doGD2.stream().filter(item1 -> item1.template.id == item.template.id).findFirst()
                        .get().quantityGD += item.quantityGD;
            }
        }

        for (Item item : doGD1) {
            if (item.isNotNullItem()) {
                itemPlayer1 += item.template.name + " (x" + item.quantityGD + "),";
            }
        }
        for (Item item : doGD2) {
            if (item.isNotNullItem()) {
                itemPlayer2 += item.template.name + " (x" + item.quantityGD + "),";
            }
        }
        String beforeTran1 = "";
        String beforeTran2 = "";
        for (Item item : bag1Before) {
            if (item.isNotNullItem()) {
                beforeTran1 += item.template.name + " (x" + item.quantity + "),";
            }
        }
        for (Item item : bag2Before) {
            if (item.isNotNullItem()) {
                beforeTran2 += item.template.name + " (x" + item.quantity + "),";
            }
        }
        String afterTran1 = "";
        String afterTran2 = "";
        for (Item item : bag1After) {
            if (item.isNotNullItem()) {
                afterTran1 += item.template.name + " (x" + item.quantity + "),";
            }
        }
        for (Item item : bag2After) {
            if (item.isNotNullItem()) {
                afterTran2 += item.template.name + " (x" + item.quantity + "),";
            }
        }

        // Console log chi tiết giao dịch
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("📦 GIAO DỊCH: " + player1 + " ⇄ " + player2);
        System.out.println("  ├─ " + pl1.name + " gửi: " + itemPlayer1);
        System.out.println("  ├─ " + pl2.name + " gửi: " + itemPlayer2);
        System.out.println("  ├─ Vàng " + pl1.name + ": " + gold1Before + " → " + gold1After
                + " (Δ " + (gold1After - gold1Before) + ")");
        System.out.println("  └─ Vàng " + pl2.name + ": " + gold2Before + " → " + gold2After
                + " (Δ " + (gold2After - gold2Before) + ")");
        System.out.println("═══════════════════════════════════════════════════════");

        try {
            DBConnecter.executeUpdate(
                    "INSERT INTO history_transaction (player_1, player_2, item_player_1, item_player_2, "
                    + "bag_1_before_tran, bag_2_before_tran, bag_1_after_tran, bag_2_after_tran, "
                    + "gold_1_before, gold_2_before, gold_1_after, gold_2_after, time_tran) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    player1, player2, itemPlayer1, itemPlayer2, beforeTran1, beforeTran2, afterTran1, afterTran2,
                    gold1Before, gold2Before, gold1After, gold2After,
                    new Timestamp(System.currentTimeMillis()));
        } catch (Exception ex) {
            System.out.println("❌ Lỗi ghi log giao dịch: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Xoá lịch sử giao dịch cũ hơn 30 ngày (trước là 3 ngày - quá ngắn)
     */
    public static void deleteHistory() {
        PreparedStatement ps = null;
        try (Connection con = DBConnecter.getConnectionServer();) {
            ps = con.prepareStatement("DELETE FROM history_transaction WHERE time_tran < DATE_SUB(NOW(), INTERVAL 30 DAY)");
            ps.executeUpdate();
        } catch (Exception e) {
        } finally {
            try {
                ps.close();
            } catch (SQLException ex) {
            }
        }
    }

}
