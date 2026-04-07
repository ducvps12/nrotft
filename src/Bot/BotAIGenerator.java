package Bot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Bộ sinh hội thoại thật bằng AI DeepSeek với fallback thông minh
 *
 * @author hoquo
 */
public class BotAIGenerator {

    // 🔹 Trạng thái API - tự động phát hiện lỗi balance
    private static boolean apiEnabled = true;
    private static int errorCount = 0;
    private static final int MAX_ERRORS_BEFORE_DISABLE = 3;

    // 🔹 API key - ƯU TIÊN LẤY TỪ ENVIRONMENT VARIABLE
    private static final String DEEPSEEK_API_KEY = System.getenv().getOrDefault("DEEPSEEK_API_KEY", "sk-afe839c428494a55bd9d90150d717a95");

    // 🔹 Fallback messages được mở rộng - HOÀN TOÀN ĐỦ DÙNG
    private static final String[] FALLBACKS = {
        // ==================== CHAT THÔNG THƯỜNG ====================
        "Ae chat vui phết nhể :))",
        "Ad fix boss cold đi ad ơi!",
        "Lag nhẹ thôi chứ vui mà!",
        "Có ai onl không z?",
        "Boss xên chưa ai ăn à?",
        "Ae đông vl luôn =))",
        "Server đông quá chat lag rồi!",
        "Mới đi farm về à?",
        "Có clan nào tuyển thành viên không?",
        "Up level nhanh thế nhờ!",
        "Đi boss không ae?",
        "Cày cuối tuần vui phết :3",
        "Ai chung team farm không?",
        "Pha combat vừa xong mệt ghê",
        "Item mới kiếm đẹp không?",
        "Rank mùa này khó leo thật",
        "Event mới có gì hot không?",
        "Build đồ thế nào cho mạnh?",
        "Có tips gì hay không chia sẻ đi",
        "Game này nghiện thật đấy :v",
        // ==================== BOSS & RAID ====================
        "Boss Cold nào mạnh quá ae ơi!",
        "Team mình đánh boss nào tiếp?",
        "Cần healer đánh boss không?",
        "Boss rare xuất hiện chưa?",
        "Tank đâu rồi, vào nhận boss đi",
        "Dps tập trung đánh boss nhanh",
        "Boss sắp chết rồi, cố lên!",
        "Ai có pet tank không?",
        "Boss drop đồ ngon không?",
        "Một mình solo boss được không?",
        "Boss nào dễ farm nhất?",
        "Team 5 người đủ đánh boss max?",
        "Boss mới update có khó không?",
        "Chiến boss lúc mấy giờ?",
        "Boss world nào đang spawn?",
        "Cần item gì để đánh boss?",
        "Boss lv bao nhiêu thì khó?",
        "Share kinh nghiệm đánh boss đi",
        "Boss nào drop nhiều exp nhất?",
        "Mới bị boss one shot mất xp :(",
        "Boss ultimate khó nhằn thật",
        "Tụ tập đánh boss event nào!",
        "Boss phụ có đáng đánh không?",
        "Boss hidden tìm thế nào?",
        "Đang tìm team đánh boss",
        "Boss sắp hết giờ rồi nhanh!",
        "Boss cần strategy gì?",
        "Boss mùa này có gì mới?",
        "Boss nào dễ kiếm item?",
        "Chuyên farm boss có giàu không?",
        // ==================== CLAN & GUILD ====================
        "Clan mình có event gì không?",
        "Có clan nào active không?",
        "Vào clan cần yêu cầu gì?",
        "Clan war khi nào diễn ra?",
        "Donate cho clan lợi gì?",
        "Clan rank mấy rồi mọi người?",
        "Có clan nào tuyển member mới?",
        "Clan boss có khó không?",
        "Share buff clan đi nào",
        "Clan quest làm chung không?",
        "Clan level bao nhiêu thì mạnh?",
        "Kick inactive member đi clan ơi",
        "Clan event cuối tuần gì vui?",
        "Có nên đổi clan không?",
        "Clan war thắng được gì?",
        "Clan donation mỗi ngày bao nhiêu?",
        "Clan skill nào useful nhất?",
        "Clan market có gì hay?",
        "Clan tournament sắp tới chưa?",
        "Clan rank server mấy?",
        "Clan cần gì để phát triển?",
        "Clan war strategy thế nào?",
        "Clan có discord không?",
        "Clan meeting khi nào?",
        "Clan support nhau thế nào?",
        "Clan level up nhanh không?",
        "Clan item exclusive có gì?",
        "Clan battle thế nào là hay?",
        "Clan war mùa này có gì hot?",
        "Clan event nào sắp tới?",
        // ==================== FARM & GRIND ====================
        "Farm chỗ nào exp cao?",
        "Đi farm chung không?",
        "Farm bao lâu thì lên level?",
        "Map nào farm ít cạnh tranh?",
        "Farm item hay exp tốt hơn?",
        "Auto farm có hiệu quả không?",
        "Farm party có lợi gì?",
        "Farm boss hay mob tốt hơn?",
        "Farm event có đáng không?",
        "Farm gold ở đâu nhanh?",
        "Farm material để craft đồ",
        "Farm pet food thế nào?",
        "Farm dungeon có worth không?",
        "Farm rare item mất bao lâu?",
        "Farm 24/7 có bị ban không?",
        "Farm spot nào ít người?",
        "Farm với pet có nhanh hơn?",
        "Farm quest hay grind tốt?",
        "Farm achievement có giá trị?",
        "Farm reputation để làm gì?",
        "Farm mount item ở đâu?",
        "Farm pvp point thế nào?",
        "Farm clan point làm gì?",
        "Farm daily quest có nên?",
        "Farm weekly boss được gì?",
        "Farm event limited item?",
        "Farm gem để enhance đồ",
        "Farm potion material?",
        "Farm skill book ở đâu?",
        "Farm title rare thế nào?",
        // ==================== PVP & ARENA ====================
        "Pvp rank mấy rồi mọi người?",
        "Arena nào đang hot?",
        "Pvp build nào meta?",
        "Chiến pvp không ae?",
        "Pvp reward có gì hay?",
        "Pvp season khi nào reset?",
        "Pvp rank nào khó leo?",
        "Pvp strategy thế nào?",
        "Pvp class nào mạnh?",
        "Pvp item cần gì?",
        "Pvp team composition?",
        "Pvp counter build?",
        "Pvp tournament có gì?",
        "Pvp daily quest?",
        "Pvp point để làm gì?",
        "Pvp rank reward?",
        "Pvp matchmaking?",
        "Pvp ban pick?",
        "Pvp meta shift?",
        "Pvp training?",
        "Pvp guide?",
        "Pvp combo?",
        "Pvp ultimate?",
        "Pvp dodge?",
        "Pvp heal?",
        "Pvp tank?",
        "Pvp dps?",
        "Pvp support?",
        "Pvp cc?",
        "Pvp one shot?",
        // ==================== ITEM & EQUIPMENT ====================
        "Item nào mạnh nhất game?",
        "Craft đồ hay drop tốt hơn?",
        "Enhance đồ thế nào?",
        "Socket gem vào đâu?",
        "Refine đồ có risk?",
        "Set item nào best?",
        "Rare item kiếm đâu?",
        "Legendary item khi nào?",
        "Item level cap?",
        "Trade item an toàn?",
        "Auction item giá?",
        "Marketplace mua bán?",
        "Item durability?",
        "Repair đồ đắt?",
        "Disassemble đồ?",
        "Craft material?",
        "Enchant scroll?",
        "Upgrade stone?",
        "Protection scroll?",
        "Lucky powder?",
        "Item appearance?",
        "Fashion item?",
        "Mount item?",
        "Pet equipment?",
        "Wing item?",
        "Title item?",
        "Aura item?",
        "Effect item?",
        "Limited item?",
        "Event item?",
        // ==================== SKILL & BUILD ====================
        "Build nào meta?",
        "Skill combo?",
        "Talent tree?",
        "Skill rotation?",
        "Ultimate skill?",
        "Passive skill?",
        "Active skill?",
        "Skill level?",
        "Skill book?",
        "Skill point?",
        "Reset skill?",
        "Hybrid build?",
        "Pve build?",
        "Pvp build?",
        "Tank build?",
        "Dps build?",
        "Support build?",
        "Healer build?",
        "Crowd control?",
        "Buff skill?",
        "Debuff skill?",
        "Aoe skill?",
        "Single target?",
        "Dot skill?",
        "Burst skill?",
        "Utility skill?",
        "Mobility skill?",
        "Defensive skill?",
        "Offensive skill?",
        "Skill cooldown?",
        // ==================== PET & MOUNT ====================
        "Pet nào mạnh?",
        "Mount nào nhanh?",
        "Pet skill?",
        "Mount speed?",
        "Pet evolution?",
        "Mount upgrade?",
        "Pet food?",
        "Mount item?",
        "Pet level?",
        "Mount level?",
        "Pet battle?",
        "Mount race?",
        "Pet rarity?",
        "Mount appearance?",
        "Pet buff?",
        "Mount skill?",
        "Pet collection?",
        "Mount collection?",
        "Pet fusion?",
        "Mount fusion?",
        "Pet arena?",
        "Mount tournament?",
        "Pet quest?",
        "Mount quest?",
        "Pet market?",
        "Mount market?",
        "Pet event?",
        "Mount event?",
        "Pet limited?",
        "Mount limited?",
        // ==================== EVENT & ACTIVITY ====================
        "Event nào đang diễn ra?",
        "Event reward có gì?",
        "Event limited item?",
        "Event boss?",
        "Event quest?",
        "Event pvp?",
        "Event clan?",
        "Event daily?",
        "Event weekly?",
        "Event monthly?",
        "Event seasonal?",
        "Event anniversary?",
        "Event holiday?",
        "Event special?",
        "Event login?",
        "Event attendance?",
        "Event lottery?",
        "Event roulette?",
        "Event collection?",
        "Event race?",
        "Event tournament?",
        "Event challenge?",
        "Event mission?",
        "Event achievement?",
        "Event title?",
        "Event mount?",
        "Event pet?",
        "Event fashion?",
        "Event gem?",
        "Event enhancement?",
        // ==================== CRAFT & GATHERING ====================
        "Craft gì profitable?",
        "Gathering chỗ nào?",
        "Craft material?",
        "Gathering tool?",
        "Craft recipe?",
        "Gathering spot?",
        "Craft level?",
        "Gathering level?",
        "Craft profession?",
        "Gathering profession?",
        "Craft order?",
        "Gathering node?",
        "Craft market?",
        "Gathering route?",
        "Craft quest?",
        "Gathering quest?",
        "Craft event?",
        "Gathering event?",
        "Craft rare?",
        "Gathering rare?",
        "Craft legendary?",
        "Gathering legendary?",
        "Craft daily?",
        "Gathering daily?",
        "Craft weekly?",
        "Gathering weekly?",
        "Craft achievement?",
        "Gathering achievement?",
        "Craft title?",
        "Gathering title?",
        // ==================== MARKET & ECONOMY ====================
        "Item nào đắt giá?",
        "Market trend?",
        "Gold farm?",
        "Diamond mua?",
        "Trade safe?",
        "Auction bid?",
        "Price check?",
        "Market flip?",
        "Investment item?",
        "Limited sale?",
        "Event market?",
        "Clan market?",
        "Personal shop?",
        "Global market?",
        "Price history?",
        "Supply demand?",
        "Rare item price?",
        "Craft profit?",
        "Gather profit?",
        "Farm profit?",
        "Boss drop value?",
        "Pet value?",
        "Mount value?",
        "Fashion value?",
        "Gem value?",
        "Enhance value?",
        "Material value?",
        "Recipe value?",
        "Skill book value?",
        "Title value?",
        // ==================== QUEST & STORY ====================
        "Main quest đến đâu?",
        "Side quest nào hay?",
        "Daily quest?",
        "Weekly quest?",
        "Achievement quest?",
        "Hidden quest?",
        "Story quest?",
        "Event quest?",
        "Clan quest?",
        "Pvp quest?",
        "Pve quest?",
        "Craft quest?",
        "Gather quest?",
        "Explore quest?",
        "Combat quest?",
        "Collection quest?",
        "Escort quest?",
        "Defend quest?",
        "Attack quest?",
        "Boss quest?",
        "Dungeon quest?",
        "Raid quest?",
        "Time quest?",
        "Chain quest?",
        "Repeat quest?",
        "Limited quest?",
        "Season quest?",
        "Legendary quest?",
        "Ultimate quest?",
        "Secret quest?",
        // ==================== DUNGEON & RAID ====================
        "Dungeon nào khó?",
        "Raid nào hay?",
        "Dungeon reward?",
        "Raid reward?",
        "Dungeon strategy?",
        "Raid strategy?",
        "Dungeon party?",
        "Raid team?",
        "Dungeon boss?",
        "Raid boss?",
        "Dungeon timer?",
        "Raid timer?",
        "Dungeon reset?",
        "Raid reset?",
        "Dungeon difficulty?",
        "Raid difficulty?",
        "Dungeon exclusive?",
        "Raid exclusive?",
        "Dungeon achievement?",
        "Raid achievement?",
        "Dungeon title?",
        "Raid title?",
        "Dungeon mount?",
        "Raid mount?",
        "Dungeon pet?",
        "Raid pet?",
        "Dungeon fashion?",
        "Raid fashion?",
        "Dungeon gem?",
        "Raid gem?",
        // ==================== SOCIAL & INTERACTION ====================
        "Kết bạn không?",
        "Team up không?",
        "Chat vui quá!",
        "Server đông vui!",
        "Giúp đỡ nhau!",
        "Share kinh nghiệm!",
        "Tìm mentor!",
        "Tìm disciple!",
        "Clan chat!",
        "World chat!",
        "Private chat!",
        "Group chat!",
        "Friend list!",
        "Block list!",
        "Report player!",
        "Compliment player!",
        "Trade player!",
        "Duel player!",
        "Marry system!",
        "Friend system!",
        "Social point!",
        "Reputation!",
        "Honor point!",
        "Social event!",
        "Party event!",
        "Group event!",
        "Community event!",
        "Social achievement!",
        "Social title!",
        "Social reward!"
    };

// 🔹 Context-based responses - phản hồi theo ngữ cảnh
    private static final String[][] CONTEXT_RESPONSES = {
        // {"keyword", "response1", "response2", ...}
        {"boss", "Chuẩn bị đi boss không ae?", "Boss nào mạnh nhất nhỉ?", "Team mấy người đánh boss là vừa?", "Boss rare drop đồ ngon lắm!", "Cần tank healer dps đủ team!"},
        {"clan", "Clan nào mạnh nhất server?", "Có clan nào tuyển không?", "Vào clan có lợi gì nhỉ?", "Clan war sắp diễn ra!", "Donate cho clan nhận buff nhé!"},
        {"farm", "Đi farm chung không?", "Chỗ nào farm exp nhanh?", "Farm bao lâu thì lên level?", "Farm spot ít người qua đây!", "Auto farm có hiệu quả không?"},
        {"item", "Item hiếm kiếm thế nào?", "Có trade item không?", "Item nào mạnh nhất game?", "Enhance đồ may rủi lắm!", "Craft đồ tốn nguyên liệu ghê!"},
        {"pvp", "Pvp vui không mọi người?", "Rank pvp bao nhiêu rồi?", "Chiến thuật pvp nào hay?", "Arena đang hot lắm!", "Pvp season sắp reset!"},
        {"event", "Event nào sắp tới?", "Event có gì hay không?", "Tham gia event có gì không?", "Event limited item đẹp lắm!", "Nhớ tham gia event hàng ngày!"},
        {"lag", "Server lag quá!", "Mạng hôm nay chậm thế?", "Ad fix lag đi chứ!", "Delay skill khó chịu ghê!", "Mạng lag đánh boss nguy hiểm!"},
        {"cày", "Cày cả ngày mệt ghê!", "Cày bao lâu thì max level?", "Cày solo hay team tốt hơn?", "Cày event có worth không?", "Cày night có bị ban không?"},
        {"pet", "Pet nào mạnh nhất?", "Nuôi pet tốn kém không?", "Pet rare kiếm đâu ra?", "Pet evolution cần gì?", "Pet skill nào useful?"},
        {"mount", "Mount nào nhanh nhất?", "Mount bay đẹp không?", "Mount limited có đáng mua?", "Mount speed bao nhiêu?", "Mount upgrade thế nào?"},
        {"quest", "Quest nào khó nhất?", "Làm quest được gì?", "Daily quest có nên làm?", "Achievement quest nhiều không?", "Hidden quest ở đâu?"},
        {"dungeon", "Dungeon nào khó?", "Vào dungeon cần gì?", "Dungeon reward có gì?", "Dungeon team thế nào?", "Dungeon strategy ra sao?"},
        {"craft", "Craft gì lời nhất?", "Craft level lên nhanh không?", "Craft material kiếm đâu?", "Craft rare item được không?", "Craft profession nào hot?"},
        {"market", "Item nào đang hot?", "Gold kiếm thế nào nhanh?", "Market trend thế nào?", "Đầu tư item gì bây giờ?", "Trade an toàn không?"},
        {"skill", "Skill nào op nhất?", "Skill combo thế nào?", "Reset skill có đắt không?", "Skill build nào meta?", "Ultimate skill mạnh không?"}
    };

    private static final Random rand = new Random();

    /**
     * Phản hồi thông minh - tự động fallback khi API lỗi
     */
    public static String generate(String lastMessage) {
        // 🔹 Nếu API đã bị disable hoặc có quá nhiều lỗi, dùng fallback ngay
        if (!apiEnabled || errorCount >= MAX_ERRORS_BEFORE_DISABLE) {
            return getSmartResponse(lastMessage);
        }

        // 🔹 Thử gọi API (chỉ khi còn enabled)
        try {
            String apiResponse = callDeepSeekAPI(lastMessage);
            if (apiResponse != null) {
                errorCount = 0; // Reset error count khi thành công
                return apiResponse;
            }
        } catch (Exception e) {
            handleAPIError(e);
        }

        // 🔹 Fallback thông minh
        return getSmartResponse(lastMessage);
    }

    /**
     * Gọi API DeepSeek với xử lý lỗi balance
     */
    private static String callDeepSeekAPI(String message) throws Exception {
        if (!apiEnabled) {
            return null;
        }

        // 🔹 Xử lý input
        String processedInput = processInput(message);

        // 🔹 Tạo request
        JSONObject body = new JSONObject();
        body.put("model", "deepseek-chat");
        body.put("max_tokens", 50);
        body.put("temperature", 0.8);

        JSONArray messages = new JSONArray();

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "Bạn là game thủ Ngọc Rồng Online. Trả lời ngắn gọn, vui vẻ, dùng tiếng lóng game. Tối đa 2 câu.");
        messages.add(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", processedInput);
        messages.add(userMsg);

        body.put("messages", messages);

        // 🔹 Gửi request
        URL url = new URL("https://api.deepseek.com/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Authorization", "Bearer " + DEEPSEEK_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toJSONString().getBytes("UTF-8"));
        }

        int status = conn.getResponseCode();

        // 🔹 PHÁT HIỆN LỖI BALANCE VÀ TỰ ĐỘNG DISABLE API
        if (status == 402) {
            System.out.println("⚠️ PHÁT HIỆN HẾT TIỀN API - TỰ ĐỘNG CHUYỂN SANG CHẾ ĐỘ FALLBACK");
            apiEnabled = false;
            return null;
        }

        if (status != 200) {
            System.out.println("⚠️ DeepSeek API lỗi: " + status);
            return null;
        }

        // 🔹 Đọc response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(response.toString());
            JSONArray choices = (JSONArray) obj.get("choices");
            JSONObject choice = (JSONObject) choices.get(0);
            JSONObject messageObj = (JSONObject) choice.get("message");
            String content = (String) messageObj.get("content");

            return content.replaceAll("[\n\r]+", " ").trim();
        }
    }

    /**
     * Xử lý lỗi API và đếm số lần lỗi
     */
    private static void handleAPIError(Exception e) {
        errorCount++;
        System.err.println("❌ Lỗi API (" + errorCount + "/" + MAX_ERRORS_BEFORE_DISABLE + "): " + e.getMessage());

        if (errorCount >= MAX_ERRORS_BEFORE_DISABLE) {
            apiEnabled = false;
            System.out.println("🚫 TẠM DỪNG API DO QUÁ NHIỀU LỖI - CHUYỂN SANG FALLBACK MODE");
        }
    }

    /**
     * Phản hồi thông minh dựa trên ngữ cảnh
     */
    private static String getSmartResponse(String lastMessage) {
        if (lastMessage == null || lastMessage.trim().isEmpty()) {
            return FALLBACKS[rand.nextInt(FALLBACKS.length)];
        }

        String lowerMessage = lastMessage.toLowerCase();

        // 🔹 Tìm response theo context
        for (String[] context : CONTEXT_RESPONSES) {
            String keyword = context[0];
            if (lowerMessage.contains(keyword)) {
                String[] responses = new String[context.length - 1];
                System.arraycopy(context, 1, responses, 0, context.length - 1);
                return responses[rand.nextInt(responses.length)];
            }
        }

        // 🔹 Fallback ngẫu nhiên thông minh
        return FALLBACKS[rand.nextInt(FALLBACKS.length)];
    }

    /**
     * Xử lý input
     */
    private static String processInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Chào anh em, chat vui vẻ nhé!";
        }
        return input.trim();
    }

    /**
     * Lấy fallback ngẫu nhiên
     */
    public static String random() {
        return FALLBACKS[rand.nextInt(FALLBACKS.length)];
    }

    /**
     * Kiểm tra trạng thái API
     */
    public static boolean isAPIEnabled() {
        return apiEnabled;
    }

    /**
     * Reset trạng thái API (dùng khi nạp thêm tiền)
     */
    public static void resetAPIStatus() {
        apiEnabled = true;
        errorCount = 0;
        System.out.println("✅ ĐÃ RESET TRẠNG THÁI API - CÓ THỂ THỬ LẠI");
    }

    /**
     * Get số lượng fallback messages
     */
    public static int getFallbackCount() {
        return FALLBACKS.length;
    }

    /**
     * Get số lượng context responses
     */
    public static int getContextResponseCount() {
        return CONTEXT_RESPONSES.length;
    }
}
