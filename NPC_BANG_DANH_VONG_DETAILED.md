# 🎯 LUỒNG NPC BẢNG DANH VỌNG - CHI TIẾT HOÀN CHỈNH

## 📍 VỊ TRÍ & SPAWN

### **Vị trí trong game:**
- **Map:** 5 (Rừng Bamboo - Khu vực luyện tập)
- **Tọa độ:** x = 350, y = 336 (tính toán từ physics)
- **Gần:** GohanUltra (x = 420)
- **Mục đích:** Tân thủ dễ thấy khi vào map luyện tập

### **Luồng Spawn:**
```
Manager.java (dòng 265-273)
    ↓
initMap() → Duyệt tất cả map
    ↓
if (map.mapId == 5) {
    ↓
    NpcFactory.createNPC(mapId, 1, x, y, ConstNpc.BANG_DANH_VONG)
    ↓
    new BangDanhVong(5, 1, 350, 336, 106, avatar)
    ↓
    map.npcs.add(npc)
}
```

---

## 🎮 LUỒNG TƯƠNG TÁC TRONG GAME

### **1. CLICK NPC**
```
Player click NPC Bảng Danh Vọng
    ↓
Npc.openBaseMenu(player) được gọi
    ↓
BangDanhVong.openBaseMenu(player) override
    ↓
canOpenNpc(player) kiểm tra:
    - Player có online không?
    - Player có trong map không?
    - Khoảng cách có đủ không?
    ↓
createOtherMenu() hiển thị menu chính
```

### **2. MENU CHÍNH**
```
╔════════════════════════════════════════╗
║  === BẢNG TIN SỰ KIỆN & DANH VỌNG ===  ║
║                                        ║
║  Đua Top Nạp                          ║
║  Bảng tin này đặt ở làng để xem       ║
║  nhanh sự kiện, boss, giftcode.       ║
║                                        ║
║  Nên đọc: Tin mới > Sự kiện > Boss    ║
╠════════════════════════════════════════╣
║ [Tin mới] [Hướng dẫn] [Boss & thời]   ║
║ [Top Sức] [Top Nạp] [Cẩm nang] [Tỉ lệ]║
║ [Đóng]                                 ║
╚════════════════════════════════════════╝
```

### **3. MENU OPTIONS (8 NÚT)**

| Nút | Case | Hàm gọi | Chức năng |
|-----|------|---------|----------|
| 0 | Tin mới/sự kiện | `showEventNews()` | Hiển thị sự kiện đang mở |
| 1 | Hướng dẫn sự kiện | `showEventGuide()` | Hướng dẫn farm sự kiện |
| 2 | Boss & thời gian | `showBossOverview()` | Danh sách boss + thời gian spawn |
| 3 | Top Sức mạnh | `TopService.showListTopPower()` | Top 10 người chơi mạnh nhất |
| 4 | Top Nạp | `TopService.showListTopVnd()` | Top 10 người nạp nhiều nhất |
| 5 | Cẩm nang tân thủ | `showGameOverview()` | Hướng dẫn chơi game |
| 6 | Tỉ lệ quà/farm | `showRewardRate()` | Tỉ lệ drop đồ |
| 7 | Đóng | (default) | Đóng menu |

---

## 📋 CHI TIẾT CÁC MENU CON

### **Menu 0: Tin mới & Sự kiện**
```java
showEventNews(player)
    ↓
Kiểm tra EventManager:
    - HUNG_VUONG (Giỗ Tổ Hùng Vương)
    - TRUNG_THU (Trung Thu)
    - HALLOWEEN
    - CHRISTMAS
    - LUNNAR_NEW_YEAR (Tết)
    - INTERNATIONAL_WOMANS_DAY (8/3)
    - TOP_UP (Đua Top Nạp)
    - EVENT_POKEMON
    - TEACHERS_DAY (20/11)
    - PHO_ANH_HAI
    ↓
Hiển thị sự kiện đang mở
```

### **Menu 1: Hướng dẫn sự kiện**
```
1. Nhận thông tin:
   - Mở bảng tin ở làng mỗi ngày
   - Theo dõi boss ở khung phải

2. Farm sự kiện:
   - Ưu tiên nhiệm vụ, mở map
   - Nâng đầu thần

3. Giftcode:
   - Code chưa Active sẽ báo chưa kích hoạt
   - Admin cần Reload Giftcode để cập nhật
```

### **Menu 2: Boss & Thời gian**
```
Boss thường:
- Kuku / Mập đầu đinh / Rambo: 5 con mỗi loại
- Black Goku: Nhiều bản thể ở các thành phố
- Golden Frieza: 5 con, độ khó cao
- Broly thường: Tăng chỉ số dần, đạt ngưỡng → Super Broly

Super Broly:
- Không spawn ngay từ đầu
- Luồng đúng: Đánh Broly thường → Tăng HP/dame → Super Broly xuất hiện

Thời gian:
- Khung thông báo bên phải hiển thị boss, map, thời gian
- Nhiều boss hồi theo chu kỳ vài phút
```

### **Menu 3: Top Sức mạnh**
```
Gọi TopService.gI().showListTopPower(player)
    ↓
Lấy top 10 người chơi có sức mạnh cao nhất
    ↓
Hiển thị danh sách với:
    - Rank
    - Tên nhân vật
    - Sức mạnh
    - Cấp độ
```

### **Menu 4: Top Nạp**
```
Gọi TopService.gI().showListTopVnd(player)
    ↓
Lấy top 10 người chơi nạp VND nhiều nhất
    ↓
Hiển thị danh sách với:
    - Rank
    - Tên nhân vật
    - Tổng VND nạp
    - Ngày nạp
```

### **Menu 5: Cẩm nang tân thủ**
```
1. Lộ trình chính:
   - Làm nhiệm vụ mở map
   - Qua khu luyện tập tăng nên tảng
   - Farm map SKH kiếm Set Kích Hoạt

2. Trang bị quan trọng:
   - SKH: Mốc đầu game
   - Đồ sao: Phần nâng cao

3. Tài nguyên:
   - Vàng: Mini game, giao dịch
   - Ngọc: Shop, sự kiện

Lộ trình tân thủ:
Nhiệm vụ > Đấu thần > SKH > Boss
```

### **Menu 6: Tỉ lệ quà & farm**
```
Map SKH:
- Đồ kích hoạt: ~1/5000 quái (không Cỏ 4 lá)
- Có Cỏ 4 lá: ~1/3500 quái
- Xayda: ~1/7000 quái

Đồ sao / vật phẩm phụ:
- Tỉ lệ rơi riêng, thấp hơn vật phẩm thường
- Chỉ số sao phụ thuộc may mắn

Boss:
- Không phải lúc nào cũng rơi đồ hiếm
- Quà thường: Vàng, vật phẩm sự kiện, mảnh, capsule
- Boss khó → Cạnh tranh cao → Nên đi nhóm hoặc khung giờ vắng

Rada / Ngọc rồng:
- Nhóm vật phẩm giá trị
- Tỉ lệ có thể được admin cân bằng theo mùa
```

---

## 🔄 LUỒNG CONFIRMmenu (Xử lý khi chọn option)

```java
BangDanhVong.confirmMenu(player, select)
    ↓
Kiểm tra:
    - canOpenNpc(player)?
    - player.iDMark.isBaseMenu()?
    ↓
switch (select) {
    case 0 → showEventNews(player)
    case 1 → showEventGuide(player)
    case 2 → showBossOverview(player)
    case 3 → TopService.gI().showListTopPower(player)
    case 4 → TopService.gI().showListTopVnd(player)
    case 5 → showGameOverview(player)
    case 6 → showRewardRate(player)
    default → (không làm gì)
}
```

---

## 🗄️ DATABASE KẾT NỐI

### **Trạng thái:**
✅ **KẾT NỐI THÀNH CÔNG**

```
Database: nrotft
Host: 103.157.204.182
User: root
Status: Connected ✓
```

### **Bảng liên quan:**
```sql
-- Không cần bảng riêng cho NPC Danh Vọng
-- Dữ liệu lấy từ:
- account (tongnap, danap, cash, vnd)
- player (sức mạnh, cấp độ)
- event (EventManager - sự kiện đang mở)
- giftcode (danh sách code)
- boss (thông tin boss)
```

---

## 🚀 LUỒNG HOÀN CHỈNH TỪ START ĐẾN FINISH

```
1. SERVER START
   ↓
2. Manager.initMap()
   ↓
3. Duyệt MAP_TEMPLATES
   ↓
4. if (map.mapId == 5)
   ↓
5. NpcFactory.createNPC(..., BANG_DANH_VONG)
   ↓
6. new BangDanhVong(5, 1, 350, 336, 106, avatar)
   ↓
7. map.npcs.add(npc)
   ↓
8. NPC SPAWN THÀNH CÔNG ✓

---

9. PLAYER VÀO GAME
   ↓
10. Player vào map 5
    ↓
11. Thấy NPC Bảng Danh Vọng
    ↓
12. Click NPC
    ↓
13. openBaseMenu() → Hiển thị menu chính
    ↓
14. Player chọn option (0-6)
    ↓
15. confirmMenu() → Xử lý
    ↓
16. Hiển thị thông tin tương ứng
    ↓
17. Player đóng menu
    ↓
18. HOÀN THÀNH ✓
```

---

## ✅ KIỂM TRA HOÀN THÀNH

- ✅ NPC được tạo trong NpcFactory.java (case 217-218)
- ✅ NPC được spawn trong Manager.java (dòng 265-273)
- ✅ NPC có menu chính với 8 option
- ✅ Mỗi option có hàm xử lý riêng
- ✅ Database kết nối thành công
- ✅ TopService có sẵn (showListTopPower, showListTopVnd)
- ✅ EventManager có sẵn (kiểm tra sự kiện)

---

## 🎯 REBUILD & TEST

**Bước 1:** Clean & Build project
```
NetBeans → Build → Clean and Build
```

**Bước 2:** Restart server
```
.\restart.bat
```

**Bước 3:** Test trong game
```
1. Login vào game
2. Vào map 5 (Rừng Bamboo)
3. Tìm NPC Bảng Danh Vọng (x=350)
4. Click NPC
5. Chọn các option để test
6. Xem thông tin hiển thị đúng không
```

**Bước 4:** Kiểm tra log
```
Console sẽ in:
✓ Nạp thành công: playerId=..., +... (gốc ...)
✓ NPC spawn thành công
```

---

## 🚨 TROUBLESHOOT

| Vấn đề | Nguyên nhân | Giải pháp |
|--------|-----------|----------|
| NPC không hiển thị | Chưa rebuild | Clean & Build lại |
| Menu không hiển thị | NPC chưa spawn | Restart server |
| Option không hoạt động | TopService lỗi | Kiểm tra TopService.java |
| Database không kết nối | Mất kết nối | Kiểm tra config.properties |
| Sự kiện không hiển thị | EventManager = false | Bật sự kiện trong admin panel |

