package nro.https;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.management.OperatingSystemMXBean;
import event.EventManager;
import item.Item;
import jdbc.DBConnecter;
import jdbc.daos.PlayerDAO;
import jdbc.daos.ShopDAO;
import models.GiftCode.GiftCodeManager;
import network.SessionManager;
import nro.player.Player;
import nro.server.*;
import nro.services.*;
import org.json.simple.JSONObject;
import boss.*;
import task.SubTaskMain;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * SimpleHttpHandler - NRO Admin Dashboard (HTTP)
 *
 * @Author: minhluong
 * @Zalo: 0376263452
 */
public class SimpleHttpHandler implements HttpHandler {

    private static Player selectedPlayer = null;
    private static final Instant serverStart = Instant.now();
    private final OperatingSystemMXBean osBean;

    public SimpleHttpHandler() {
        OperatingSystemMXBean bean = null;
        try {
            bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        } catch (Exception ignored) {
        }
        this.osBean = bean;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers",
                    "X-Requested-With, Content-Type, Content-Length");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (path.equals("/admin") || path.equals("/admin/")) {
                serveAdminPage(exchange);
                return;
            }

            Map<String, List<String>> params = parseQuery(uri.getRawQuery());
            JSONObject resp = handleApi(params);

            byte[] out = resp.toJSONString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }

        } catch (Exception e) {
            JSONObject err = msg("error", "Internal error: " + e.getMessage());
            byte[] out = err.toJSONString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(500, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        }
    }

    // ==================== GIAO DIỆN HTML ====================
    // ------------------ Giao diện web HTML hoàn chỉnh ------------------
    private void serveAdminPage(HttpExchange exchange) throws IOException {
        String html = """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>NRO Admin Dashboard</title>
                  <style>
                    body {
                      margin:0;
                      font-family:Segoe UI,Roboto,Arial;
                      background:#0b1221;
                      color:#e6eef8;
                    }
                    header {
                      background:#0e162b;
                      padding:12px 20px;
                      display:flex;
                      justify-content:space-between;
                      align-items:center;
                      box-shadow:0 2px 10px rgba(0,0,0,0.4);
                    }
                    header h2 { margin:0; color:#4fa3ff; }
                    main { padding:20px; }
                    .card {
                      background:#131c33;
                      border-radius:10px;
                      padding:15px;
                      margin-bottom:15px;
                      box-shadow:0 4px 14px rgba(0,0,0,0.3);
                    }
                    button {
                      background:#2f7bff;
                      border:none;
                      border-radius:8px;
                      padding:8px 14px;
                      color:#fff;
                      cursor:pointer;
                    }
                    button:hover { opacity:0.85; }
                    pre {
                      background:#091226;
                      color:#a0aec0;
                      padding:10px;
                      border-radius:8px;
                      overflow:auto;
                      max-height:200px;
                    }
                  </style>
                </head>
                <body>
                  <header>
                    <h2>NRO Server Dashboard</h2>
                    <div>
                      <span id="uptime">Uptime: --</span> |
                      <span id="online">Online: 0</span>
                    </div>
                  </header>

                  <main>
                    <div class="card">
                      <h3>Server Status</h3>
                      <div>CPU: <span id="cpu">--</span></div>
                      <div>RAM: <span id="ram">--</span></div>
                      <div>Giftcode: <span id="gift">--</span></div>
                      <div>Boss: <span id="boss">--</span></div>
                      <div>Event: <span id="event">--</span></div>
                    </div>

                    <div class="card">
                      <h3>Actions</h3>
                      <button onclick="call('ONLINE')">Cập nhật</button>
                      <button onclick="call('RESTART')">Khởi động lại</button>
                      <button onclick="call('MAINTENANCE')">Bảo trì</button>
                      <button onclick="call('RELOAD_GIFTCODE')">Reload Giftcode</button>
                      <button onclick="call('RELOAD_SHOP')">Reload Shop</button>
                    </div>

                    <div class="card">
                      <h3>Kết quả API</h3>
                      <pre id="log">Chưa có dữ liệu...</pre>
                    </div>
                  </main>

                  <script>
                    const key = 'abcdef'; // API key
                    async function call(type){
                      try {
                        const res = await fetch('/?key='+key+'&type='+type);
                        const data = await res.json();
                        document.getElementById('log').textContent = JSON.stringify(data, null, 2);
                        if(data.online !== undefined) document.getElementById('online').textContent = 'Online: ' + data.online;
                        if(data.cpu) document.getElementById('cpu').textContent = data.cpu;
                        if(data.ram) document.getElementById('ram').textContent = data.ram;
                        if(data.gift) document.getElementById('gift').textContent = data.gift;
                        if(data.event_active) document.getElementById('event').textContent = data.event_active;
                        if(data.boss_active) document.getElementById('boss').textContent = data.boss_active;
                        if(data.uptime) document.getElementById('uptime').textContent = 'Uptime: ' + data.uptime;
                      } catch(e){
                        document.getElementById('log').textContent = 'API Error: ' + e;
                      }
                    }
                    // Auto refresh every 5 seconds
                    setInterval(()=>call('ONLINE'),5000);
                    call('ONLINE');
                  </script>
                </body>
                </html>
                """;

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ==================== API CORE ====================
    private JSONObject handleApi(Map<String, List<String>> params) {
        try {
            String key = getParam(params, "key", "");
            String type = getParam(params, "type", "").toUpperCase();

            if (key.isEmpty()) {
                return msg("error", "Thiếu tham số key!");
            }
            if (!key.equals(Manager.apiKey)) {
                return msg("error", "Key không hợp lệ!");
            }
            if (type.isEmpty()) {
                return msg("error", "Thiếu tham số type!");
            }

            return switch (type) {
                case "ONLINE", "RELOAD_STATS" ->
                    onlineStatus();
                case "MAINTENANCE" ->
                    doMaintenance();
                case "RESTART" ->
                    doRestart();
                case "UPDATE_EXP" ->
                    updateExpRate(params);
                case "RELOAD_GIFTCODE" ->
                    reloadGiftcode();
                case "RELOAD_SHOP" ->
                    reloadShop();
                case "RELOAD_NPCS" ->
                    reloadNpcs();
                case "AUTOSAVE_TOGGLE" ->
                    toggleAutoSave();
                case "SET_PLAYER" ->
                    setPlayer(params);
                case "KICK" ->
                    kickSelected();
                case "BAN" ->
                    banSelected();
                case "ADD_ITEM" ->
                    addItemApi(params);
                case "ADD_GOLD" ->
                    addGoldApi(params);
                case "ADD_RUBY" ->
                    addRubyApi(params);
                case "BUFF_VND" ->
                    buffVnd(params);
                case "CHANGE_EVENT" ->
                    changeEvent(params);
                case "SEND_NOTI" ->
                    sendNoti(params);
                case "BOSS_LIST" ->
                    bossList();
                case "CALL_BOSS" ->
                    callBossApi(params);
                case "RESET_BOSSES" ->
                    resetAllBosses();
                case "BOOST_EXP" ->
                    boostExpApi(params);
                case "PLAYER_LIST" ->
                    playerList();
                case "ADMIN_BUFF_SELF" ->
                    adminBuffSelf(params);
                case "ADMIN_BUFF_TARGET" ->
                    adminBuffTarget(params);
                default ->
                    msg("error", "Lệnh không hợp lệ: " + type);
            };

        } catch (Exception e) {
            return msg("error", "Exception: " + e.getMessage());
        }
    }

    // ✅ Cập nhật tỉ lệ EXP server (updateExpRate)
    private JSONObject updateExpRate(Map<String, List<String>> params) {
        String v = getParam(params, "value", "0");
        try {
            // Chuyển giá trị nhập vào thành số thập phân
            double newRate = Double.parseDouble(v);

            if (newRate <= 0) {
                return msg("error", "Giá trị EXP phải lớn hơn 0!");
            }

            // Cập nhật vào biến toàn cục Manager
            Manager.RATE_EXP_SERVER = newRate;

            // Gửi thông báo toàn server
            String notify = "Admin đã thay đổi tỷ lệ EXP server thành x" + newRate;
            if (Client.gI() != null && Client.gI().getPlayers() != null) {
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null) {
                        Service.gI().sendThongBao(p, notify);
                    }
                }
            }

            // Ghi log console
            System.out.println("[ADMIN] EXP server cập nhật -> x" + newRate);

            // Trả về JSON kết quả
            return msg("success", "Đã cập nhật tỷ lệ EXP = x" + newRate);
        } catch (NumberFormatException e) {
            return msg("error", "Sai định dạng số: " + e.getMessage());
        } catch (Exception e) {
            return msg("error", "Lỗi cập nhật EXP: " + e.getMessage());
        }
    }

    // ========================== API CHỨC NĂNG ===========================
    // ✅ Thêm vật phẩm
    private JSONObject addItemApi(Map<String, List<String>> params) {
        if (selectedPlayer == null) {
            return msg("error", "Chưa chọn người chơi!");
        }
        try {
            short id = Short.parseShort(getParam(params, "item_id", "0"));
            int amount = Integer.parseInt(getParam(params, "amount", "1"));
            Item item = ItemService.gI().createNewItem(id);
            if (item == null) {
                return msg("error", "Item ID không hợp lệ: " + id);
            }
            item.quantity = amount;
            InventoryService.gI().addItemBag(selectedPlayer, item);
            InventoryService.gI().sendItemBag(selectedPlayer);
            Service.gI().sendThongBao(selectedPlayer, "Bạn nhận được " + amount + " vật phẩm ID " + id);
            return msg("success", "Đã thêm vật phẩm thành công!");
        } catch (Exception e) {
            return msg("error", "Lỗi khi thêm item: " + e.getMessage());
        }
    }

    // ✅ Thêm thỏi vàng (gold bar item ID 457)
    private JSONObject addGoldApi(Map<String, List<String>> params) {
        if (selectedPlayer == null) {
            return msg("error", "Chưa chọn người chơi!");
        }
        try {
            int amount = Integer.parseInt(getParam(params, "amount", "1"));
            Item item = ItemService.gI().createNewItem((short) 457);
            item.quantity = amount;
            InventoryService.gI().addItemBag(selectedPlayer, item);
            InventoryService.gI().sendItemBag(selectedPlayer);
            Service.gI().sendThongBao(selectedPlayer, "Bạn đã nhận " + amount + " Thỏi Vàng");
            return msg("success", "Đã thêm thỏi vàng thành công!");
        } catch (Exception e) {
            return msg("error", "Lỗi khi thêm vàng: " + e.getMessage());
        }
    }

    // ✅ Thêm ruby
    private JSONObject addRubyApi(Map<String, List<String>> params) {
        if (selectedPlayer == null) {
            return msg("error", "Chưa chọn người chơi!");
        }
        try {
            int amount = Integer.parseInt(getParam(params, "amount", "0"));
            if (amount <= 0) {
                return msg("error", "Số lượng ruby phải > 0");
            }
            selectedPlayer.inventory.ruby += amount;
            PlayerService.gI().sendInfoHpMpMoney(selectedPlayer);
            Service.gI().sendThongBao(selectedPlayer, "Bạn nhận được " + amount + " Hồng Ngọc");
            return msg("success", "Đã thêm ruby thành công!");
        } catch (Exception e) {
            return msg("error", "Lỗi thêm ruby: " + e.getMessage());
        }
    }

    // ✅ Buff VND vào tài khoản
    private JSONObject buffVnd(Map<String, List<String>> params) {
        if (selectedPlayer == null) {
            return msg("error", "Chưa chọn người chơi!");
        }
        try {
            int amount = Integer.parseInt(getParam(params, "amount", "0"));
            if (amount <= 0) {
                return msg("error", "Số tiền phải > 0");
            }
            int result = PlayerDAO.addVnd(selectedPlayer.name, amount);
            if (result == -1) {
                return msg("error", "Cộng VND thất bại (DB error)");
            }
            nro.server.CashAuditLog.logAdd(selectedPlayer, amount, "ADMIN_HTTP", "BuffVND via HTTP API");
            selectedPlayer.getSession().cash += amount;
            Service.gI().sendThongBao(selectedPlayer, "Tài khoản bạn đã được cộng thêm " + amount + " VND");
            return msg("success", "Đã cộng " + amount + " VND cho người chơi.");
        } catch (Exception e) {
            return msg("error", "Lỗi buff VND: " + e.getMessage());
        }
    }

    // ✅ Đổi sự kiện đang hoạt động
    private JSONObject changeEvent(Map<String, List<String>> params) {
        String idStr = getParam(params, "id", "0");
        try {
            int id = Integer.parseInt(idStr);

            // reset all event flags
            EventManager.TRUNG_THU = false;
            EventManager.LUNNAR_NEW_YEAR = false;
            EventManager.HALLOWEEN = false;
            EventManager.CHRISTMAS = false;
            EventManager.HUNG_VUONG = false;
            EventManager.INTERNATIONAL_WOMANS_DAY = false;
            EventManager.TOP_UP = false;

            String name;
            switch (id) {
                case 1:
                    EventManager.TRUNG_THU = true;
                    name = "Trung Thu";
                    break;
                case 2:
                    EventManager.TOP_UP = true;
                    name = "Mặc định";
                    break;
                case 3:
                    EventManager.LUNNAR_NEW_YEAR = true;
                    name = "Tết Nguyên Đán";
                    break;
                case 4:
                    EventManager.INTERNATIONAL_WOMANS_DAY = true;
                    name = "8/3";
                    break;
                case 5:
                    EventManager.HUNG_VUONG = true;
                    name = "Giỗ Tổ Hùng Vương";
                    break;
                case 6:
                    EventManager.CHRISTMAS = true;
                    name = "Giáng Sinh";
                    break;
                case 7:
                    EventManager.HALLOWEEN = true;
                    name = "Halloween";
                    break;
                default:
                    return msg("error", "ID sự kiện không hợp lệ!");
            }

            EventManager.gI().init();
            Service.gI().sendThongBaoAllPlayer("Sự kiện " + name + " đang diễn ra!");
            return msg("success", "Đã đổi sự kiện sang: " + name);
        } catch (Exception e) {
            return msg("error", "Lỗi đổi sự kiện: " + e.getMessage());
        }
    }

    // ✅ Gửi thông báo đến tất cả người chơi
    private JSONObject sendNoti(Map<String, List<String>> params) {
        String content = getParam(params, "content", "").trim();
        if (content.isEmpty()) {
            return msg("error", "Nội dung trống!");
        }
        try {
            Service.gI().sendBigMessAllPlayer(1139, "|7|THÔNG BÁO:\n" + content.replace(";", "\n"));
            return msg("success", "Đã gửi thông báo đến toàn server.");
        } catch (Exception e) {
            return msg("error", "Lỗi gửi thông báo: " + e.getMessage());
        }
    }

    // ✅ Danh sách boss (từ BossID)
    private JSONObject bossList() {
        JSONObject o = new JSONObject();
        try {
            List<JSONObject> list = new ArrayList<>();
            Field[] fields = BossID.class.getFields();
            for (Field f : fields) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                    int id = f.getInt(null);
                    String name = f.getName().replace("_", " ").toLowerCase();
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    JSONObject j = new JSONObject();
                    j.put("id", id);
                    j.put("name", name);
                    list.add(j);
                }
            }
            o.put("status", "success");
            o.put("bosses", list);
        } catch (Exception e) {
            o.put("status", "error");
            o.put("message", e.getMessage());
        }
        return o;
    }

    // ✅ Gọi boss theo ID
    private JSONObject callBossApi(Map<String, List<String>> params) {
        String idStr = getParam(params, "boss_id", "");
        try {
            int id = Integer.parseInt(idStr);
            if (BossManager.gI() == null) {
                return msg("error", "BossManager chưa sẵn sàng!");
            }
            Boss boss = BossManager.gI().createBoss(id);
            if (boss == null) {
                return msg("error", "Không thể tạo boss id=" + id);
            }
            boss.changeStatus(BossStatus.RESPAWN);
            return msg("success", "Đã triệu hồi boss ID " + id);
        } catch (Exception e) {
            return msg("error", "Lỗi gọi boss: " + e.getMessage());
        }
    }

    // ✅ Reset toàn bộ boss
    private JSONObject resetAllBosses() {
        try {
            if (BossManager.gI() != null) {
                BossManager.gI().resetAllBosses();
                return msg("success", "Đã reset toàn bộ boss thành công!");
            } else {
                return msg("error", "BossManager chưa sẵn sàng!");
            }
        } catch (Exception e) {
            return msg("error", "Lỗi reset boss: " + e.getMessage());
        }
    }

    // ✅ Tăng tạm thời EXP server
    private JSONObject boostExpApi(Map<String, List<String>> params) {
        String v = getParam(params, "value", "1");
        try {
            double mul = Double.parseDouble(v);
            Manager.RATE_EXP_SERVER = Manager.RATE_EXP_SERVER * mul;
            String notify = "Admin đã tăng EXP server x" + mul;
            if (Client.gI() != null) {
                for (Player p : Client.gI().getPlayers()) {
                    Service.gI().sendThongBao(p, notify);
                }
            }
            return msg("success", "EXP server tạm thời tăng x" + mul);
        } catch (Exception e) {
            return msg("error", "Lỗi boost EXP: " + e.getMessage());
        }
    }

    // ✅ Danh sách người chơi online
    private JSONObject playerList() {
        JSONObject o = new JSONObject();
        try {
            List<Player> players = Client.gI() != null ? Client.gI().getPlayers() : new ArrayList<>();
            List<JSONObject> arr = new ArrayList<>();
            for (Player p : players) {
                if (p == null) {
                    continue;
                }
                JSONObject j = new JSONObject();
                j.put("id", (int) p.id);
                j.put("name", p.name);
                try {
                    j.put("level", p.nPoint.levelBT);
                } catch (Exception ignored) {
                    j.put("level", "?");
                }
                arr.add(j);
            }
            o.put("status", "success");
            o.put("players", arr);
        } catch (Exception e) {
            o.put("status", "error");
            o.put("message", e.getMessage());
        }
        return o;
    }

    // ==================== API IMPLEMENTATION ====================
    private JSONObject onlineStatus() {
        JSONObject o = new JSONObject();
        try {
            List<Player> players = Client.gI() != null ? Client.gI().getPlayers() : Collections.emptyList();
            o.put("status", "success");
            o.put("online", players.size());

            // CPU + RAM
            if (osBean != null) {
                double cpu = Math.max(0, osBean.getProcessCpuLoad() * 100.0);
                long total = Runtime.getRuntime().totalMemory() / (1024L * 1024L);
                long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L);
                o.put("cpu", String.format("%.1f%%", cpu));
                o.put("ram", used + "/" + total + " MB");
            }

            Duration up = Duration.between(serverStart, Instant.now());
            o.put("uptime", String.format("%d giờ %02d phút", up.toHours(), up.toMinutesPart()));
            o.put("exp_rate", Manager.RATE_EXP_SERVER);
            o.put("event_active", getCurrentEvent());
            o.put("boss_active", getBossCount());

        } catch (Exception e) {
            return msg("error", "Lỗi thống kê: " + e.getMessage());
        }
        return o;
    }

    private JSONObject doMaintenance() {
        try {
            Thread.startVirtualThread(() -> Maintenance.gI().start(5));
            return msg("success", "Máy chủ sẽ bảo trì sau 5 giây!");
        } catch (Exception e) {
            return msg("error", e.getMessage());
        }
    }

    private JSONObject doRestart() {
        try {
            Thread.startVirtualThread(() -> new AutoMaintenance().execute());
            return msg("success", "Server sẽ khởi động lại!");
        } catch (Exception e) {
            return msg("error", e.getMessage());
        }
    }

    private JSONObject reloadGiftcode() {
        try {
            Thread.startVirtualThread(() -> {
                try {
                    if (GiftCodeManager.gI() != null) {
                        GiftCodeManager.gI().loadGiftCodeFromDB();
                    }
                } catch (Exception e) {
                    System.err.println("[AdminAPI] Lỗi reload giftcode: " + e.getMessage());
                }
            });
            return msg("success", "Giftcode reload đang chạy nền. Cache sẽ nạp lại từ DB.");
        } catch (Exception e) {
            return msg("error", e.getMessage());
        }
    }

    private JSONObject reloadShop() {
        try {
            Thread.startVirtualThread(() -> {
                try {
                    Manager.SHOPS = ShopDAO.getShops(DBConnecter.getConnectionServer());
                } catch (Exception e) {
                    System.err.println("[AdminAPI] Lỗi reload shop: " + e.getMessage());
                }
            });
            return msg("success", "Reload shop chạy nền.");
        } catch (Exception e) {
            return msg("error", e.getMessage());
        }
    }

    private JSONObject reloadNpcs() {
        try {
            Thread.startVirtualThread(() -> {
                try {
                    Manager.NPC_TEMPLATES.clear();
                } catch (Exception ignored) {
                }
            });
            return msg("success", "Reload NPCs chạy nền.");
        } catch (Exception e) {
            return msg("error", e.getMessage());
        }
    }

    // ✅ Bật / Tắt AutoSave an toàn (không cần isRunning())
    private JSONObject toggleAutoSave() {
        try {
            // Gọi instance của AutoSaveManager
            AutoSaveManager auto = AutoSaveManager.getInstance();

            // Thử dừng, nếu đang tắt thì sẽ ném lỗi -> bật lại
            try {
                auto.stopAutoSave();
                System.out.println("[ADMIN] AutoSave đã được TẮT.");
                return msg("success", "Đã tắt AutoSave.");
            } catch (Exception e1) {
                // Nếu stop lỗi => nghĩa là chưa chạy, nên bật lên
                try {
                    auto.startAutoSave();
                    System.out.println("[ADMIN] AutoSave đã được BẬT.");
                    return msg("success", "Đã bật AutoSave.");
                } catch (Exception e2) {
                    return msg("error", "Không thể bật AutoSave: " + e2.getMessage());
                }
            }
        } catch (Exception e) {
            return msg("error", "Lỗi AutoSave: " + e.getMessage());
        }
    }

    private JSONObject setPlayer(Map<String, List<String>> params) {
        String name = getParam(params, "player_name", "").trim();
        if (name.isEmpty()) {
            return msg("error", "Thiếu tên player!");
        }
        Player p = Client.gI() != null ? Client.gI().getPlayer(name) : null;
        if (p == null) {
            return msg("error", "Không tìm thấy người chơi!");
        }
        selectedPlayer = p;
        return msg("success", "Đã chọn: " + name);
    }

    private JSONObject kickSelected() {
        if (selectedPlayer == null) {
            return msg("error", "Chưa chọn người chơi!");
        }
        try {
            Client.gI().kickSession(selectedPlayer.getSession());
            Service.gI().sendThongBao(selectedPlayer, "Bạn bị kick bởi Admin!");
            selectedPlayer = null;
            return msg("success", "Đã kick người chơi.");
        } catch (Exception e) {
            return msg("error", e.getMessage());
        }
    }

    private JSONObject banSelected() {
        if (selectedPlayer == null) {
            return msg("error", "Chưa chọn người chơi!");
        }
        try {
            PlayerDAO.banAccount(selectedPlayer.getSession(), selectedPlayer);
            Service.gI().sendThongBao(selectedPlayer, "Tài khoản của bạn đã bị khóa!");
            selectedPlayer.getSession().disconnect();
            selectedPlayer = null;
            return msg("success", "Đã ban người chơi.");
        } catch (Exception e) {
            return msg("error", e.getMessage());
        }
    }

    private JSONObject adminBuffSelf(Map<String, List<String>> params) {
        String name = getParam(params, "player_name", "").trim();
        Player target = name.isEmpty() ? selectedPlayer : (Client.gI() != null ? Client.gI().getPlayer(name) : null);
        if (target == null) {
            return msg("error", "Chưa chọn admin/player online. Dùng SET_PLAYER hoặc truyền player_name.");
        }
        if (!target.isAdmin()) {
            return msg("error", "Chỉ cho phép buff tài khoản admin để test bug.");
        }
        return applyAdminBuff(target, params);
    }

    private JSONObject adminBuffTarget(Map<String, List<String>> params) {
        if (selectedPlayer == null) {
            return msg("error", "Chưa chọn người chơi!");
        }
        if (!selectedPlayer.isAdmin()) {
            return msg("error", "Chỉ cho phép buff tài khoản admin để test bug.");
        }
        return applyAdminBuff(selectedPlayer, params);
    }

    private JSONObject applyAdminBuff(Player target, Map<String, List<String>> params) {
        try {
            String mode = getParam(params, "mode", "starter").toLowerCase();
            switch (mode) {
                case "task" -> completeCurrentSubTask(target);
                case "nexttask" -> TaskService.gI().sendNextTaskMain(target);
                case "power" -> {
                    long amount = Long.parseLong(getParam(params, "amount", "1000000"));
                    addPowerAndPotential(target, amount, amount);
                }
                case "stat" -> buffBaseStats(target,
                        Long.parseLong(getParam(params, "hp", "1000")),
                        Long.parseLong(getParam(params, "mp", "1000")),
                        Long.parseLong(getParam(params, "dame", "100")));
                case "starter" -> {
                    addPowerAndPotential(target, 5_000_000L, 5_000_000L);
                    buffBaseStats(target, 10_000L, 10_000L, 1_000L);
                    addTestItems(target);
                }
                default -> {
                    return msg("error", "mode không hợp lệ. Dùng: starter, task, nexttask, power, stat");
                }
            }
            target.nPoint.calPoint();
            PlayerService.gI().sendInfoHpMpMoney(target);
            Service.gI().point(target);
            Service.gI().Send_Info_NV(target);
            PlayerDAO.updatePlayer(target);
            Service.gI().sendThongBao(target, "Admin buff test thành công: " + mode);
            return msg("success", "Đã buff test cho admin " + target.name + " mode=" + mode);
        } catch (Exception e) {
            return msg("error", "Lỗi admin buff: " + e.getMessage());
        }
    }

    private void completeCurrentSubTask(Player player) {
        if (player.playerTask == null || player.playerTask.taskMain == null) {
            return;
        }
        int index = player.playerTask.taskMain.index;
        if (index < 0 || index >= player.playerTask.taskMain.subTasks.size()) {
            return;
        }
        SubTaskMain subTask = player.playerTask.taskMain.subTasks.get(index);
        subTask.count = subTask.maxCount;
        TaskService.gI().sendUpdateCountSubTask(player);
        TaskService.gI().sendTaskMain(player);
    }

    private void addPowerAndPotential(Player player, long power, long potential) {
        player.nPoint.power = Math.max(0, player.nPoint.power + power);
        player.nPoint.tiemNang = Math.max(0, player.nPoint.tiemNang + potential);
        TaskService.gI().checkDoneTaskPower(player, player.nPoint.power);
    }

    private void buffBaseStats(Player player, long hp, long mp, long dame) {
        player.nPoint.hpg = Math.max(1, player.nPoint.hpg + hp);
        player.nPoint.mpg = Math.max(1, player.nPoint.mpg + mp);
        player.nPoint.dameg = Math.max(1, player.nPoint.dameg + dame);
        player.nPoint.hp = player.nPoint.hpMax;
        player.nPoint.mp = player.nPoint.mpMax;
    }

    private void addTestItems(Player player) {
        Item goldBar = ItemService.gI().createNewItem((short) 457, 100);
        Item gem = ItemService.gI().createNewItem((short) 77, 5000);
        InventoryService.gI().addItemBag(player, goldBar);
        InventoryService.gI().addItemBag(player, gem);
        player.inventory.ruby += 5000;
        InventoryService.gI().sendItemBag(player);
    }

    // ==================== UTILITIES ====================
    private String getCurrentEvent() {
        if (EventManager.TRUNG_THU) {
            return "Trung Thu";
        }
        if (EventManager.LUNNAR_NEW_YEAR) {
            return "Tết Nguyên Đán";
        }
        if (EventManager.HALLOWEEN) {
            return "Halloween";
        }
        if (EventManager.CHRISTMAS) {
            return "Giáng Sinh";
        }
        if (EventManager.HUNG_VUONG) {
            return "Giỗ Tổ";
        }
        if (EventManager.INTERNATIONAL_WOMANS_DAY) {
            return "8/3";
        }
        return "Không có";
    }

    private int getBossCount() {
        try {
            return BossManager.gI() != null ? BossManager.gI().getBosses().size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private JSONObject msg(String status, String message) {
        JSONObject o = new JSONObject();
        o.put("status", status);
        o.put("message", message);
        return o;
    }

    private String getParam(Map<String, List<String>> params, String key, String def) {
        try {
            List<String> v = params.get(key);
            return (v == null || v.isEmpty()) ? def : v.get(0);
        } catch (Exception e) {
            return def;
        }
    }

    private Map<String, List<String>> parseQuery(String query) {
        Map<String, List<String>> map = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return map;
        }
        for (String part : query.split("&")) {
            try {
                String[] kv = part.split("=", 2);
                String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                map.computeIfAbsent(k, x -> new ArrayList<>()).add(v);
            } catch (Exception ignored) {
            }
        }
        return map;
    }
}
