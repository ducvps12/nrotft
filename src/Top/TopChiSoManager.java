package Top;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jdbc.DBConnecter;
import lombok.Getter;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import item.Item;
import nro.player.Player;
import nro.services.ItemService;

public class TopChiSoManager {

    @Getter
    private List<Player> list = new ArrayList<>();
    private static final TopChiSoManager INSTANCE = new TopChiSoManager();

    public static TopChiSoManager getInstance() {
        return INSTANCE;
    }

    public void loadTopHP() {
        load("cur_hp");
    }

    public void loadTopKI() {
        load("cur_ki");
    }

    public void loadTopDame() {
        load("cur_sd");
    }

    private void load(String column) {
        list.clear();
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT * FROM player ORDER BY " + column + " DESC LIMIT 100");
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Player player = processPlayerResultSet(rs);
                player.nPoint.hpMax = rs.getLong("cur_hp");
                player.nPoint.mpMax = rs.getLong("cur_ki");
                player.nPoint.dame = rs.getLong("cur_sd");
                list.add(player);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Player processPlayerResultSet(ResultSet rs) throws SQLException {
        Player player = new Player();
        player.id = rs.getInt("id");
        player.name = rs.getString("name");
        player.head = rs.getShort("head");
        player.gender = rs.getByte("gender");
        player.firstTimeLogin = rs.getTimestamp("firstTimeLogin");

        extractDataPoint(rs.getString("data_point"), player);
        extractItemsBody(rs.getString("items_body"), player);

        return player;
    }

    private void extractDataPoint(String dataPoint, Player player) {
        JSONArray dataArray = (JSONArray) JSONValue.parse(dataPoint);
        player.nPoint.power = Long.parseLong(dataArray.get(1).toString());
    }

    private void extractItemsBody(String itemsBody, Player player) {
        JSONArray dataArray = (JSONArray) JSONValue.parse(itemsBody);

        for (Object obj : dataArray) {
            JSONArray data = (JSONArray) JSONValue.parse(obj.toString());
            short tempId = Short.parseShort(String.valueOf(data.get(0)));

            Item item;
            if (tempId != -1) {
                item = ItemService.gI().createNewItem(tempId,
                        Integer.parseInt(String.valueOf(data.get(1))));
            } else {
                item = ItemService.gI().createItemNull();
            }

            player.inventory.itemsBody.add(item);
        }
    }
}
