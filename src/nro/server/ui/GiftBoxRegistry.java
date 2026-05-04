package nro.server.ui;

import java.util.*;

/**
 * Registry chứa thông tin chi tiết về hộp quà và vật phẩm có thể mở.
 * Dùng cho Admin Panel hiển thị drop table, tỉ lệ, chỉ số.
 */
public class GiftBoxRegistry {

    public static class RewardEntry {
        public String group;       // Nhóm phần thưởng
        public int[] itemIds;      // Danh sách item ID có thể nhận
        public String itemNames;   // Tên hiển thị
        public int minQty, maxQty; // Số lượng min-max
        public double rate;        // Tỉ lệ % rơi
        public String options;     // Chỉ số bonus (nếu có)

        public RewardEntry(String group, int[] itemIds, String itemNames,
                           int minQty, int maxQty, double rate, String options) {
            this.group = group;
            this.itemIds = itemIds;
            this.itemNames = itemNames;
            this.minQty = minQty;
            this.maxQty = maxQty;
            this.rate = rate;
            this.options = options;
        }
    }

    public static class GiftBoxInfo {
        public int itemId;
        public String name;
        public String description;
        public List<RewardEntry> rewards;

        public GiftBoxInfo(int itemId, String name, String desc) {
            this.itemId = itemId;
            this.name = name;
            this.description = desc;
            this.rewards = new ArrayList<>();
        }

        public GiftBoxInfo add(String group, int[] ids, String names,
                               int minQ, int maxQ, double rate, String opts) {
            rewards.add(new RewardEntry(group, ids, names, minQ, maxQ, rate, opts));
            return this;
        }
    }

    private static final Map<Integer, GiftBoxInfo> REGISTRY = new LinkedHashMap<>();

    static {
        // === Hộp quà tháng 9 (ID 1695) ===
        reg(1695, "Hộp quà tháng 9", "Mở ra ngẫu nhiên: Công thức, nguyên liệu, cải trang, bùa, capsule, rương rồng thần")
            .add("Công thức / Đá nâng cấp", new int[]{1071,1072,1073,1074,1075,1076,1077,1078,1084,1085,1086},
                 "CT nâng cấp ngẫu nhiên", 1, 3, 25.0, "Không")
            .add("Nguyên liệu sự kiện", new int[]{1310,1311,1312,674,670},
                 "NL sự kiện ngẫu nhiên", 3, 10, 25.0, "Không")
            .add("Cải trang thường", new int[]{1693,1697,1698,1700},
                 "Cải trang ngẫu nhiên", 1, 1, 20.0,
                 "HP+5~12%, SD+5~12%, Giáp+5~12%, 80% thêm Crit+1~7%")
            .add("Bùa bình an / may mắn", new int[]{1691,1692},
                 "Bùa ngẫu nhiên", 1, 3, 15.0, "Không")
            .add("Capsule kích hoạt", new int[]{1559},
                 "Capsule kích hoạt", 1, 1, 10.0, "Không")
            .add("Rương rồng thần", new int[]{1898},
                 "Rương rồng thần", 1, 1, 5.0, "Không");

        // === Hộp quà tháng 9 VIP (ID 1696) ===
        reg(1696, "Hộp quà tháng 9 VIP", "Phiên bản VIP với tỉ lệ đồ hiếm cao hơn")
            .add("Cải trang VIP", new int[]{1693,1697,1698,1700},
                 "Cải trang ngẫu nhiên", 1, 1, 30.0,
                 "HP+8~15%, SD+8~15%, Giáp+8~15%, 90% thêm Crit+3~10%")
            .add("Rương rồng thần", new int[]{1898},
                 "Rương rồng thần", 1, 1, 15.0, "Không")
            .add("Capsule kích hoạt", new int[]{1559},
                 "Capsule kích hoạt", 1, 1, 15.0, "Không")
            .add("Công thức nâng cấp", new int[]{1071,1072,1073,1074,1075,1076,1077,1078},
                 "CT nâng cấp ngẫu nhiên", 2, 5, 20.0, "Không")
            .add("Nguyên liệu sự kiện", new int[]{1310,1311,1312,674,670},
                 "NL sự kiện ngẫu nhiên", 5, 15, 15.0, "Không")
            .add("Bùa may mắn", new int[]{1691,1692},
                 "Bùa ngẫu nhiên", 2, 5, 5.0, "Không");

        // === Hộp đồ Thần Linh (ID 1703) ===
        reg(1703, "Hộp đồ Thần Linh", "Chọn hành tinh → nhận set đồ Thần Linh tương ứng")
            .add("Set Trái Đất", new int[]{}, "Full set Thần Linh Trái Đất", 1, 1, 33.3, "Chỉ số Thần Linh")
            .add("Set Namec", new int[]{}, "Full set Thần Linh Namec", 1, 1, 33.3, "Chỉ số Thần Linh")
            .add("Set Xayda", new int[]{}, "Full set Thần Linh Xayda", 1, 1, 33.4, "Chỉ số Thần Linh");

        // === Hộp đồ Hủy Diệt (ID 1704) ===
        reg(1704, "Hộp đồ Hủy Diệt", "Chọn hành tinh → nhận set đồ Hủy Diệt tương ứng")
            .add("Set Trái Đất", new int[]{}, "Full set Hủy Diệt Trái Đất", 1, 1, 33.3, "Chỉ số Hủy Diệt")
            .add("Set Namec", new int[]{}, "Full set Hủy Diệt Namec", 1, 1, 33.3, "Chỉ số Hủy Diệt")
            .add("Set Xayda", new int[]{}, "Full set Hủy Diệt Xayda", 1, 1, 33.4, "Chỉ số Hủy Diệt");

        // === Hộp đồ Vải Thô (ID 1806) ===
        reg(1806, "Hộp đồ Vải Thô", "Chọn hành tinh → nhận set đồ Vải Thô tương ứng")
            .add("Set Trái Đất", new int[]{}, "Full set Vải Thô Trái Đất", 1, 1, 33.3, "Chỉ số Vải Thô")
            .add("Set Namec", new int[]{}, "Full set Vải Thô Namec", 1, 1, 33.3, "Chỉ số Vải Thô")
            .add("Set Xayda", new int[]{}, "Full set Vải Thô Xayda", 1, 1, 33.4, "Chỉ số Vải Thô");

        // === Rương Rồng Thần (ID 1898) ===
        reg(1898, "Rương Rồng Thần", "Mở ra ngẫu nhiên đồ Rồng Thần Namec")
            .add("Đồ Rồng Thần", new int[]{},
                 "Set Rồng Thần Namec ngẫu nhiên", 1, 1, 100.0, "Chỉ số Rồng Thần");

        // === Hộp quà Tân Thủ (ID 1938) ===
        reg(1938, "Hộp quà Tân Thủ", "Quà cho người mới bắt đầu")
            .add("Trang bị cơ bản", new int[]{},
                 "Set trang bị tân thủ", 1, 1, 100.0, "Chỉ số cơ bản cho lv1");

        // === Capsule Trang Sức VIP (ID 1964) ===
        reg(1964, "Capsule Trang Sức VIP", "Mở ra trang sức VIP ngẫu nhiên")
            .add("Trang sức VIP", new int[]{},
                 "Trang sức VIP ngẫu nhiên", 1, 1, 100.0, "Chỉ số trang sức cao cấp");

        // === Capsule Cải Trang VIP (ID 2006) ===
        reg(2006, "Capsule Cải Trang VIP", "Mở ra cải trang VIP ngẫu nhiên")
            .add("Cải trang VIP", new int[]{},
                 "Cải trang VIP ngẫu nhiên", 1, 1, 100.0, "Chỉ số cải trang VIP");

        // === Hộp 20/10 (ID 1957) ===
        reg(1957, "Hộp quà 20/10", "Hộp quà sự kiện 20/10")
            .add("Quà sự kiện", new int[]{},
                 "Phần thưởng sự kiện 20/10", 1, 1, 100.0, "Ngẫu nhiên");
    }

    private static GiftBoxInfo reg(int id, String name, String desc) {
        GiftBoxInfo info = new GiftBoxInfo(id, name, desc);
        REGISTRY.put(id, info);
        return info;
    }

    public static GiftBoxInfo getInfo(int itemId) {
        return REGISTRY.get(itemId);
    }

    public static boolean isGiftBox(int itemId) {
        return REGISTRY.containsKey(itemId);
    }

    public static Map<Integer, GiftBoxInfo> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
