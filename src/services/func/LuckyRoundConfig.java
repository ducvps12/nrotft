package services.func;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cấu hình vòng quay may mắn - cho phép admin tùy chỉnh tỉ lệ và vật phẩm từ panel.
 * Dữ liệu lưu tại data/config/lucky_round.json
 */
public class LuckyRoundConfig {

    private static final String CONFIG_PATH = "data/config/lucky_round.json";
    private static LuckyRoundConfig instance;

    // Danh sách tier VIP (Thỏi Vàng)
    private final CopyOnWriteArrayList<RewardTier> vipTiers = new CopyOnWriteArrayList<>();
    // Danh sách tier Thường (Vàng/Ngọc)
    private final CopyOnWriteArrayList<RewardTier> normalTiers = new CopyOnWriteArrayList<>();

    // Giá quay
    private volatile int priceGold = 25_000_000;
    private volatile int priceGem = 4;
    private volatile int priceTicket = 1;
    private volatile int ticketItemId = 457;

    // Vàng mặc định (khi không trúng tier nào)
    private volatile int defaultGoldMin = 5000;
    private volatile int defaultGoldMax = 50000;

    public static LuckyRoundConfig gI() {
        if (instance == null) {
            instance = new LuckyRoundConfig();
            instance.load();
        }
        return instance;
    }

    // ============ DATA CLASSES ============

    public static class RewardTier {
        public String name;           // Tên tier (hiển thị)
        public int ratioNumerator;    // Tử số tỉ lệ (VD: 1)
        public int ratioDenominator;  // Mẫu số tỉ lệ (VD: 10000)
        public boolean enabled;       // Bật/tắt tier
        public boolean announce;      // Thông báo server khi trúng
        public String announcePrefix; // Prefix thông báo: [JACKPOT], [SSR]...
        public List<RewardItem> items = new ArrayList<>();

        public RewardTier() {}

        public RewardTier(String name, int num, int den, boolean enabled, boolean announce, String prefix) {
            this.name = name;
            this.ratioNumerator = num;
            this.ratioDenominator = den;
            this.enabled = enabled;
            this.announce = announce;
            this.announcePrefix = prefix;
        }

        public double getPercentage() {
            if (ratioDenominator == 0) return 0;
            return (double) ratioNumerator / ratioDenominator * 100;
        }

        public String getRatioDisplay() {
            return ratioNumerator + "/" + ratioDenominator;
        }
    }

    public static class RewardItem {
        public int itemId;         // Template ID
        public String itemName;    // Tên hiển thị
        public int quantityMin;    // Số lượng tối thiểu
        public int quantityMax;    // Số lượng tối đa
        public int weight;         // Trọng số (trong cùng tier, random theo weight)
        public List<ItemOptionDef> options = new ArrayList<>(); // Chỉ số

        public RewardItem() {}

        public RewardItem(int id, String name, int qMin, int qMax, int weight) {
            this.itemId = id;
            this.itemName = name;
            this.quantityMin = qMin;
            this.quantityMax = qMax;
            this.weight = weight;
        }
    }

    public static class ItemOptionDef {
        public int optionId;   // ID option (50=SĐ%, 77=HP%, 103=KI%...)
        public int valueMin;   // Giá trị min
        public int valueMax;   // Giá trị max

        public ItemOptionDef() {}

        public ItemOptionDef(int id, int min, int max) {
            this.optionId = id;
            this.valueMin = min;
            this.valueMax = max;
        }
    }

    // ============ GETTERS ============

    public List<RewardTier> getVipTiers() { return vipTiers; }
    public List<RewardTier> getNormalTiers() { return normalTiers; }
    public int getPriceGold() { return priceGold; }
    public int getPriceGem() { return priceGem; }
    public int getPriceTicket() { return priceTicket; }
    public int getTicketItemId() { return ticketItemId; }
    public int getDefaultGoldMin() { return defaultGoldMin; }
    public int getDefaultGoldMax() { return defaultGoldMax; }

    // ============ SETTERS ============

    public void setPriceGold(int v) { this.priceGold = v; }
    public void setPriceGem(int v) { this.priceGem = v; }
    public void setPriceTicket(int v) { this.priceTicket = v; }
    public void setTicketItemId(int v) { this.ticketItemId = v; }
    public void setDefaultGoldMin(int v) { this.defaultGoldMin = v; }
    public void setDefaultGoldMax(int v) { this.defaultGoldMax = v; }

    // ============ LOAD / SAVE ============

    public void load() {
        File file = new File(CONFIG_PATH);
        if (file.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
                Gson gson = new Gson();
                ConfigData data = gson.fromJson(reader, ConfigData.class);
                if (data != null) {
                    vipTiers.clear();
                    normalTiers.clear();
                    if (data.vipTiers != null) vipTiers.addAll(data.vipTiers);
                    if (data.normalTiers != null) normalTiers.addAll(data.normalTiers);
                    if (data.priceGold > 0) priceGold = data.priceGold;
                    if (data.priceGem > 0) priceGem = data.priceGem;
                    if (data.priceTicket > 0) priceTicket = data.priceTicket;
                    if (data.ticketItemId > 0) ticketItemId = data.ticketItemId;
                    if (data.defaultGoldMin > 0) defaultGoldMin = data.defaultGoldMin;
                    if (data.defaultGoldMax > 0) defaultGoldMax = data.defaultGoldMax;
                    System.out.println("[LuckyRoundConfig] Loaded " + vipTiers.size() + " VIP tiers, " + normalTiers.size() + " normal tiers");
                    return;
                }
            } catch (Exception e) {
                System.err.println("[LuckyRoundConfig] Error loading: " + e.getMessage());
            }
        }
        // Nếu không có file, tạo defaults
        initDefaults();
        save();
    }

    public void save() {
        try {
            File file = new File(CONFIG_PATH);
            file.getParentFile().mkdirs();
            ConfigData data = new ConfigData();
            data.vipTiers = new ArrayList<>(vipTiers);
            data.normalTiers = new ArrayList<>(normalTiers);
            data.priceGold = priceGold;
            data.priceGem = priceGem;
            data.priceTicket = priceTicket;
            data.ticketItemId = ticketItemId;
            data.defaultGoldMin = defaultGoldMin;
            data.defaultGoldMax = defaultGoldMax;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                gson.toJson(data, writer);
            }
            System.out.println("[LuckyRoundConfig] Saved to " + CONFIG_PATH);
        } catch (Exception e) {
            System.err.println("[LuckyRoundConfig] Error saving: " + e.getMessage());
        }
    }

    private static class ConfigData {
        List<RewardTier> vipTiers;
        List<RewardTier> normalTiers;
        int priceGold;
        int priceGem;
        int priceTicket;
        int ticketItemId;
        int defaultGoldMin;
        int defaultGoldMax;
    }

    // ============ DEFAULTS (từ code cũ) ============

    private void initDefaults() {
        vipTiers.clear();
        normalTiers.clear();

        // === VIP TIERS ===

        // GOD: Goku Blue
        RewardTier god = new RewardTier("🌟 GOD - Goku Blue", 1, 10000, true, true, "JACKPOT");
        RewardItem gokuBlue = new RewardItem(1858, "Goku Blue", 1, 1, 100);
        gokuBlue.options.add(new ItemOptionDef(5, 50, 60));
        gokuBlue.options.add(new ItemOptionDef(50, 50, 60));
        gokuBlue.options.add(new ItemOptionDef(77, 50, 60));
        gokuBlue.options.add(new ItemOptionDef(103, 50, 60));
        gokuBlue.options.add(new ItemOptionDef(99, 50, 60));
        gokuBlue.options.add(new ItemOptionDef(14, 15, 20));
        gokuBlue.options.add(new ItemOptionDef(125, 15, 20));
        god.items.add(gokuBlue);
        vipTiers.add(god);

        // SSR: Hào quang God
        RewardTier ssr = new RewardTier("✨ SSR - Hào quang God", 1, 5000, true, true, "SSR");
        RewardItem haoQuang = new RewardItem(2005, "Hào quang God", 1, 1, 100);
        haoQuang.options.add(new ItemOptionDef(50, 25, 35));
        haoQuang.options.add(new ItemOptionDef(77, 25, 35));
        haoQuang.options.add(new ItemOptionDef(103, 25, 35));
        haoQuang.options.add(new ItemOptionDef(14, 10, 18));
        haoQuang.options.add(new ItemOptionDef(94, 20, 30));
        ssr.items.add(haoQuang);
        vipTiers.add(ssr);

        // LEGENDARY: Thú cưỡi / Pet
        RewardTier legendary = new RewardTier("🔥 LEGENDARY - Thú cưỡi/Pet", 1, 2000, true, true, "LEGENDARY");
        RewardItem thuCuoi = new RewardItem(1904, "Thú cưỡi rồng SC", 1, 1, 50);
        thuCuoi.options.add(new ItemOptionDef(50, 20, 30));
        thuCuoi.options.add(new ItemOptionDef(77, 20, 30));
        thuCuoi.options.add(new ItemOptionDef(103, 20, 30));
        thuCuoi.options.add(new ItemOptionDef(14, 5, 12));
        legendary.items.add(thuCuoi);
        RewardItem petPo = new RewardItem(1564, "Pet Po", 1, 1, 50);
        petPo.options.add(new ItemOptionDef(50, 20, 30));
        petPo.options.add(new ItemOptionDef(77, 20, 30));
        petPo.options.add(new ItemOptionDef(103, 20, 30));
        petPo.options.add(new ItemOptionDef(14, 5, 12));
        legendary.items.add(petPo);
        vipTiers.add(legendary);

        // EPIC: Set Thần Linh (đã nerf 1/3000)
        RewardTier epic = new RewardTier("🎉 EPIC - Set Thần Linh", 1, 3000, true, true, "EPIC");
        int[] epicIds = { 555, 556, 562, 563, 557, 558, 564, 565, 559, 560, 566, 567, 561, 921 };
        String[] epicNames = {
            "Áo TĐ", "Quần TĐ", "Găng TĐ", "Giày TĐ",
            "Áo Namếc", "Quần Namếc", "Găng Namếc", "Giày Namếc",
            "Áo Xayda", "Quần Xayda", "Găng Xayda", "Giày Xayda",
            "Nhẫn Thần Linh", "Bông tai Porata +2"
        };
        for (int i = 0; i < epicIds.length; i++) {
            epic.items.add(new RewardItem(epicIds[i], epicNames[i], 1, 1, 10));
        }
        vipTiers.add(epic);

        // RARE: Chân Mệnh / Sách TK2 / Giáp
        RewardTier rare = new RewardTier("💎 RARE - Chân Mệnh/Sách TK2", 1, 100, true, true, "RARE");
        RewardItem chanMenh = new RewardItem(1893, "Chân Mệnh c9", 1, 1, 33);
        chanMenh.options.add(new ItemOptionDef(50, 8, 15));
        chanMenh.options.add(new ItemOptionDef(77, 8, 15));
        chanMenh.options.add(new ItemOptionDef(103, 8, 15));
        rare.items.add(chanMenh);
        rare.items.add(new RewardItem(1278, "Sách tuyệt kỹ 2", 1, 1, 33));
        rare.items.add(new RewardItem(1751, "Giáp tập luyện c4", 1, 1, 34));
        vipTiers.add(rare);

        // VIP CT
        RewardTier vipCt = new RewardTier("👑 VIP CT - Cải trang 20-40%", 1, 50, true, true, "VIP");
        int[] ctIds = { 467, 468, 469, 470, 471, 741, 745, 800, 801, 803, 804, 999, 1000, 1001 };
        for (int id : ctIds) {
            RewardItem ct = new RewardItem(id, "CT #" + id, 1, 1, 10);
            ct.options.add(new ItemOptionDef(77, 20, 40));
            ct.options.add(new ItemOptionDef(50, 20, 40));
            vipCt.items.add(ct);
        }
        vipTiers.add(vipCt);

        // UNCOMMON: Hộp SKH / Mảnh BT
        RewardTier uncommon = new RewardTier("🎁 UNCOMMON - Hộp SKH/Mảnh BT", 1, 300, true, false, "");
        uncommon.items.add(new RewardItem(1703, "Hộp SKH Thần Linh", 1, 1, 50));
        uncommon.items.add(new RewardItem(1855, "Mảnh vỡ BT c3", 1, 3, 50));
        vipTiers.add(uncommon);

        // COMMON: Capsule/Sách/Đá
        RewardTier common = new RewardTier("📦 COMMON - Capsule/Sách/Đá", 1, 5, true, false, "");
        common.items.add(new RewardItem(956, "Capsule", 1, 5, 25));
        common.items.add(new RewardItem(220, "Sách kỹ năng", 1, 5, 25));
        common.items.add(new RewardItem(585, "Đá xanh lam", 1, 5, 25));
        common.items.add(new RewardItem(18, "Capsule TT", 1, 3, 25));
        vipTiers.add(common);

        // === NORMAL TIERS ===

        // CT
        RewardTier normalCt = new RewardTier("👕 Cải Trang Thường", 1, 2, true, false, "");
        int[] normalCtIds = { 467, 468, 469, 470, 471, 741, 745, 800, 801, 803, 804, 1000 };
        for (int id : normalCtIds) {
            RewardItem ct = new RewardItem(id, "CT #" + id, 1, 1, 10);
            ct.options.add(new ItemOptionDef(77, 10, 25));
            ct.options.add(new ItemOptionDef(50, 10, 25));
            normalCt.items.add(ct);
        }
        normalTiers.add(normalCt);

        // Đá
        RewardTier normalStone = new RewardTier("💎 Đá xanh lam", 1, 20, true, false, "");
        normalStone.items.add(new RewardItem(585, "Đá xanh lam", 1, 5, 100));
        normalTiers.add(normalStone);

        // Capsule TT
        RewardTier normalCap = new RewardTier("📦 Capsule thời trang", 1, 100, true, false, "");
        normalCap.items.add(new RewardItem(18, "Capsule TT 1", 1, 5, 33));
        normalCap.items.add(new RewardItem(19, "Capsule TT 2", 1, 5, 33));
        normalCap.items.add(new RewardItem(20, "Capsule TT 3", 1, 5, 34));
        normalTiers.add(normalCap);

        // Sách
        RewardTier normalBook = new RewardTier("📚 Sách kỹ năng", 1, 30, true, false, "");
        for (int id = 220; id <= 224; id++) {
            normalBook.items.add(new RewardItem(id, "Sách #" + id, 1, 5, 20));
        }
        normalTiers.add(normalBook);

        // Nguyên liệu
        RewardTier normalMat = new RewardTier("🧪 Nguyên liệu", 1, 100, true, false, "");
        for (int id = 828; id <= 842; id++) {
            normalMat.items.add(new RewardItem(id, "NL #" + id, 1, 5, 7));
        }
        normalTiers.add(normalMat);
    }

    // ============ RELOAD ============

    public void reload() {
        load();
    }
}
