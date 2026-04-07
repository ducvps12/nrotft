package consts;

import utils.Functions;
import item.Item;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import nro.services.ItemService;
import nro.services.Service;
import jdbc.DBConnecter;
import jdbc.daos.NDVSqlFetcher;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import jdbc.NDVResultSet;
import nro.player.Player;
import utils.Logger;

// su kien 1/6
public class ConstDataEventNAP {

    public static ConstDataEventNAP gI;

    public static ConstDataEventNAP gI() {
        if (gI == null) {
            gI = new ConstDataEventNAP();
        }
        return gI;
    }

    public static boolean isEventActive() {
        return false;
    }

    public static boolean isTraoQua = true;
    public static Calendar startEvent;
    public static Calendar endEvents;
    public static boolean initsukien = false;

    public static short MONTH_OPEN;
    public static short DATE_OPEN;
    public static short HOUR_OPEN;
    public static short MIN_OPEN;
    public static short MONTH_END;
    public static short DATE_END;
    public static short HOUR_END;
    public static short MIN_END;

    public static boolean isActiveEvent() {
        if (!initsukien) {
            initsukien = true;
            startEvent = Calendar.getInstance();
            startEvent.set(ConstDataEventSM.YEAR_EVENT, MONTH_OPEN - 1, DATE_OPEN, HOUR_OPEN, MIN_OPEN);
            System.out.println("Star Event TOP CARD: " + startEvent.getTime());

            endEvents = Calendar.getInstance();
            endEvents.set(ConstDataEventSM.YEAR_EVENT, MONTH_END - 1, DATE_END, HOUR_END, MIN_END);
            System.out.println("End Event TOP CARD: " + endEvents.getTime());
        }

        Calendar currentTime = Calendar.getInstance();
        long currentMillis = System.currentTimeMillis();
        
        if (currentMillis >= startEvent.getTimeInMillis() && currentMillis <= endEvents.getTimeInMillis()) {
            if (isTraoQua && currentMillis + 60000 >= endEvents.getTimeInMillis()) {
                traoQuaHangLoat();
                isTraoQua = false;
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean isRunningSK = isActiveEvent();

    // Tối ưu: Load tất cả phần thưởng trước
    private static List<JSONArray> loadAllRewards() {
        List<JSONArray> rewards = new ArrayList<>();
        String sql = "SELECT detail FROM moc_nap_top ORDER BY id ASC";
        
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                JSONArray detail = (JSONArray) JSONValue.parse(rs.getString("detail"));
                rewards.add(detail);
            }
        } catch (SQLException e) {
            Logger.error("Lỗi load rewards TOP NAP: " + e.getMessage());
            e.printStackTrace();
        }
        return rewards;
    }

    // Tối ưu hóa: Trao quà hàng loạt với 1 kết nối duy nhất
    private static void traoQuaHangLoat() {
        String sql = "SELECT player.id as plid, player.name as name, account.danap " +
                    "FROM account, player WHERE account.id = player.account_id " +
                    "AND account.create_time > ? AND account.danap >= 100000 " +
                    "ORDER BY account.danap DESC LIMIT 10";
        
        List<Integer> accIdPlayers = new ArrayList<>();
        List<String> playerNames = new ArrayList<>();
        List<JSONArray> allRewards = loadAllRewards();

        // Lấy danh sách người chơi
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, String.format("2025-%02d-%02d %02d:%02d:00", 
                MONTH_OPEN, DATE_OPEN, HOUR_OPEN, MIN_OPEN));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("plid"); // Sửa từ "id" thành "plid"
                    String name = rs.getString("name");
                    accIdPlayers.add(id);
                    playerNames.add(name);
                }
            }
        } catch (Exception e) {
            Logger.error("Lỗi lấy danh sách người chơi TOP NAP: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Trao quà cho từng người chơi
        for (int i = 0; i < accIdPlayers.size(); i++) {
            if (i >= allRewards.size()) {
                Logger.error("Không đủ phần thưởng cho hạng " + (i + 1) + " TOP NAP");
                continue;
            }

            Player player = NDVSqlFetcher.loadPlayerByID(accIdPlayers.get(i));
            if (player != null) {
                traoQuaSuKien(player, allRewards.get(i));
                Logger.log("Đã trao quà nạp top " + (i + 1) + " cho: " + playerNames.get(i));
                
                try {
                    Functions.sleep(100); // Giảm tải
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Logger.error("Không thể load player ID: " + accIdPlayers.get(i) + " TOP NAP");
            }
        }
    }

    // Phiên bản tối ưu của TraoQuaSuKien
    public static void traoQuaSuKien(Player pl, JSONArray rewardDetail) {
        if (pl == null || rewardDetail == null) return;

        try {
            for (int i = 0; i < rewardDetail.size(); i++) {
                JSONObject dataObject = (JSONObject) JSONValue.parse(String.valueOf(rewardDetail.get(i)));
                
                int tempid = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                
                Item item = ItemService.gI().createNewItem((short) tempid);
                item.quantity = quantity;
                
                JSONArray optionsArray = (JSONArray) dataObject.get("options");
                for (int j = 0; j < optionsArray.size(); j++) {
                    JSONObject optionObject = (JSONObject) optionsArray.get(j);
                    int param = Integer.parseInt(String.valueOf(optionObject.get("param")));
                    int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                    item.itemOptions.add(new Item.ItemOption(optionId, param));
                }
                
                pl.inventory.itemsMailBox.add(item);
            }

            if (NDVSqlFetcher.updateMailBox(pl)) {
                Service.gI().sendThongBao(pl, "Bạn vừa nhận quà về mail thành công");
            } else {
                Logger.error("Lỗi update mailbox cho: " + pl.name + " TOP NAP");
            }
            
        } catch (Exception e) {
            Logger.error("Lỗi trao quà TOP NAP cho " + pl.name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}