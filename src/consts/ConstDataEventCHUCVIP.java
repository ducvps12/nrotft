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
public class ConstDataEventCHUCVIP {

    public static ConstDataEventCHUCVIP gI;

    public static ConstDataEventCHUCVIP gI() {
        if (gI == null) {
            gI = new ConstDataEventCHUCVIP();
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

    public static short MONTH_OPEN = 7;
    public static short DATE_OPEN = 13;
    public static short HOUR_OPEN = 12;
    public static short MIN_OPEN = 0;

    public static short MONTH_END = 8;
    public static short DATE_END = 13;
    public static short HOUR_END = 12;
    public static short MIN_END = 0;

    public static boolean isActiveEvent() {
        if (!initsukien) {
            initsukien = true;
            startEvent = Calendar.getInstance();

            // Set event start date and time
            startEvent.set(ConstDataEventSM.YEAR_EVENT, MONTH_OPEN - 1, DATE_OPEN, HOUR_OPEN, MIN_OPEN);
            System.out.println("Start Event: VIP Greeting Card TOP - " + startEvent.getTime());

            endEvents = Calendar.getInstance();
// Set event end date and time
            endEvents.set(ConstDataEventSM.YEAR_EVENT, MONTH_END - 1, DATE_END, HOUR_END, MIN_END);
            System.out.println("End Event: VIP Greeting Card TOP - " + endEvents.getTime());
        }

        Calendar currentTime = Calendar.getInstance();
        if (System.currentTimeMillis() >= startEvent.getTimeInMillis() && System.currentTimeMillis() <= endEvents.getTimeInMillis()) {
            if (isTraoQua && System.currentTimeMillis() + 60000 >= endEvents.getTimeInMillis()) {
                String sql = "SELECT  player.id as plid, player.name as name, account.hopqua2010 FROM account, player WHERE account.id = player.account_id AND account.create_time > '2025-" + MONTH_OPEN + "-" + DATE_OPEN + " " + HOUR_OPEN + ":" + MIN_OPEN + ":00' AND account.hopqua2010 >= 100000 ORDER BY account.hopqua2010 DESC LIMIT 10;";
                List<Integer> AccIdPlayer = new ArrayList<>();
                NDVResultSet rs = null;
                try {
                    rs = DBConnecter.executeQuery(sql);
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        AccIdPlayer.add(id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < AccIdPlayer.size(); i++) {
                    Player player = NDVSqlFetcher.loadPlayerByID(AccIdPlayer.get(i));
                    TraoQuaSuKien(player, i + 1);
                    Logger.error("Đã trao quà nạp top " + (i + 1) + " cho: " + player.name + "\n");
                    try {
                        //Thread.sleep(100);
                        Functions.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                isTraoQua = false;
            }
            return true;
        } else {

            return false;
        }
    }

    public static boolean isRunningSK = isActiveEvent();

    public static void TraoQuaSuKien(Player pl, int rank) {
        Item item = null;
        JSONArray dataArray;
        JSONObject dataObject;
        try ( Connection con2 = DBConnecter.getConnectionServer();  PreparedStatement ps = con2.prepareStatement("SELECT detail FROM moc_hopqua2010 WHERE id = ?")) {
            // Sử dụng rank (thứ hạng) để truy vấn phần thưởng tương ứng
            ps.setInt(1, rank);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                        int tempid = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                        int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                        item = ItemService.gI().createNewItem((short) tempid);
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
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
