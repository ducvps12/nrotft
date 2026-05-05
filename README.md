# 🐉 NROTFT — Ngọc Rồng Online Server

> Private server **Chú Bé Rồng Online** (NRO) — Java backend với hệ thống NPC, Shop, Pet, Boss, Mini Game và Admin Panel đầy đủ.

---

## 📋 Mục Lục

- [Tính Năng](#-tính-năng)
- [Kiến Trúc](#-kiến-trúc)
- [Cài Đặt](#-cài-đặt)
- [Cấu Hình](#-cấu-hình)
- [Hệ Thống Game](#-hệ-thống-game)
- [Admin Panel](#-admin-panel)
- [Changelog](#-changelog-gần-nhất)

---

## ⚡ Tính Năng

### 🎮 Gameplay
- **Hệ thống Đệ Tử** — Mabu, Cell, Berus, B.Goku, Tuyệt Thế (farm Kilis)
- **VIP Đệ Tử** — Buff x2~x7 TNSM, phân bổ HP/DAME, cấu hình Giáp/Chí Mạng
- **Fusion Porata** — Cấp 1-4, bonus stats theo loại đệ
- **Boss System** — Custom drop, reward panel, nhiều biến thể boss
- **Ngọc Rồng Băng** — Hệ thống 1-7 sao, ước nguyện, ghép mảnh

### 🏪 Shop & NPC
- **Shop Osin** — Cải trang, đá nâng cấp, công thức (đá ngũ sắc)
- **Bà Hạt Mít** — Ép đồ, Mở Khóa GD, Gia Hạn Vật Phẩm, Tẩy Trang Bị
- **Bill** — Đổi thức ăn lấy trang bị Hủy Diệt/Thần, phiếu ăn
- **Champa** — Bán đồ rác, hiến tế trang bị
- **Lý Tiểu Nương** — Gói VIP Tuần, Gói Đệ Tử, VIP Đệ Tử, Mini Games
- **Santa** — Shop bang hội, shop hỗ trợ

### 🎰 Mini Games
- Kéo Búa Bao (nhiều mức cược)
- Số May Mắn (Vàng/Ngọc)
- Chọn Ai Đây (Vàng/Ngọc/Ruby)

### 🔧 Hệ Thống Khác
- **Đá Hoàng Kim** — Mở khóa GD (30%), gia hạn HSD
- **Giao dịch** — Hỗ trợ số lượng lớn (>9999)
- **Capsule VIP** — Random cải trang full stats vĩnh viễn
- **Firewall** — Chống DDoS, giới hạn IP
- **Radar/Card** — Hệ thống thẻ bài sưu tập

---

## 🏗 Kiến Trúc

```
nrotft/
├── src/
│   ├── nro/                    # Core server
│   │   ├── models/npc/         # NPC logic (Bill, BaHatMit, LyTieuNuong...)
│   │   ├── server/             # Server manager, Admin UI panels
│   │   ├── services/           # Game services (Inventory, Item, NPC, VIP...)
│   │   └── https/              # HTTP API endpoints
│   ├── models/
│   │   └── Combine/            # Ép đồ system (BaHatMit combine tabs)
│   ├── boss/                   # Boss spawn, AI, rewards
│   ├── shop/                   # Shop system (ShopService, ShopDAO)
│   ├── jdbc/                   # Database layer (MySQL)
│   ├── item/                   # Item templates & options
│   ├── mob/                    # Mob definitions
│   ├── map/                    # Map definitions
│   ├── minigame/               # Mini games
│   ├── event/                  # Event system
│   ├── skill/                  # Skill system
│   ├── clan/                   # Bang hội
│   └── consts/                 # Constants
├── data/
│   ├── config/                 # Server config (config.properties)
│   └── map/                    # Map data files
├── sql/                        # Database schemas & patches
├── settings/                   # Additional settings
├── lib/                        # Dependencies (JAR files)
└── dist/                       # Build output
```

---

## 🚀 Cài Đặt

### Yêu Cầu
- **Java** 17+ (hỗ trợ switch expressions, text blocks)
- **MySQL** 5.7+ / MariaDB 10+
- **XAMPP** (khuyến nghị cho dev local)

### Bước 1: Import Database
```bash
mysql -u root -p nrotft < sql/nrotft.sql
```

### Bước 2: Cấu Hình
Sửa `data/config/config.properties`:
```properties
database.host=127.0.0.1
database.port=3306
database.name=nrotft
database.user=root
database.pass=your_password
server.port_real=14445
server.ip_host=127.0.0.1
```

### Bước 3: Build & Run
```bash
# Build
ant -f build.xml

# Hoặc chạy trực tiếp
run.bat
```

---

## ⚙ Cấu Hình

| Key | Mô tả | Mặc định |
|-----|--------|----------|
| `server.port_real` | Port game server | `14445` |
| `server.maxplayer` | Số player tối đa | `10000` |
| `server.expserver` | Hệ số EXP | `1` |
| `game.droprate` | Hệ số drop | `1` |
| `game.goldrate` | Hệ số vàng | `1` |
| `game.bossrespawn` | Thời gian respawn boss (giây) | `600` |
| `game.maxlevel` | Level tối đa | `150` |

---

## 🎮 Hệ Thống Game

### 🐾 Hệ Thống Đệ Tử

| Loại | Ô TB | Fusion Bonus | Nguồn |
|------|------|-------------|-------|
| Mabu | 7 | HP 20% KI 35% SD 25% | Gói 50K VNĐ |
| Cell | 9 | HP 25% KI 30% SD 30% | Gói 100K VNĐ |
| Berus | 9 | HP 30% KI 35% SD 35% | Gói 250K VNĐ |
| B.Goku | 9 | HP 35% KI 40% SD 40% | Gói 500K VNĐ |
| **Tuyệt Thế** | **10** | **+20% + cộng thẳng** | **Chỉ cày FREE** |

### 🏆 Lộ Trình Tuyệt Thế Đệ Tử

```
❶ Kiếm Bình Hút Năng Lượng
   └─ Boss Rồng Nhí / Hirudegarn / NV Quỷ Lão Kame

❷ Farm Kilis (Map Cadic)
   └─ Tỉ lệ 1/333 • Buff Osin x10 (100 HN = 10 phút)

❸ Đệ 3K Kilis
   └─ 3,000 Kilis + Mabu 40 tỷ SM → Random B.Goku/Cell/Berus

❹ Tuyệt Thế Đệ Tử
   └─ 6,000 Kilis + Đệ 3K 100 tỷ SM → Tuyệt Thế!
```

### 💎 Đá Hoàng Kim (Item 1723)

| Chức năng | Chi phí | Tỉ lệ |
|-----------|---------|--------|
| Mở khóa giao dịch | 2,000 Ruby + 1 Đá HK | 30% thành công |
| Gia hạn HSD | 1,000 Ruby + 1 Đá HK | 50% +3~7 ngày / 50% +1 ngày |

---

## 🖥 Admin Panel

Server bao gồm Admin Panel GUI (Swing) với các tính năng:
- **Dashboard** — Reload shop, NPC, boss data real-time
- **Players Panel** — Xem/sửa thông tin player, đổi pet type
- **Boss Editor** — Cấu hình drop, reward cho từng boss
- **Account Panel** — Quản lý tài khoản, tìm kiếm theo power

---

## 📝 Changelog Gần Nhất

### 2026-05-06
- ✅ **Shop Osin**: Bỏ CT Gogeta SSJ Blue & Vegito SSJ Blue, giảm giá Gohan Beast & Vegito God → 8,500 đá ngũ sắc
- ✅ **UI Hướng Dẫn Đệ Tử**: Redesign hoàn toàn với bảng so sánh, boxed layout, emoji markers
- ✅ **Bà Hạt Mít**: Tích hợp Mở Khóa GD, Gia Hạn Vật Phẩm, Tẩy Trang Bị
- ✅ **Giao dịch**: Nâng giới hạn số lượng giao dịch (hỗ trợ >9999)
- ✅ **Hiến tế**: Fix logic hiến tế trang bị tại Champa
- ✅ **MoKhoaItem**: Chuẩn hóa tỉ lệ 30%, chi phí 2,000 Ruby
- ✅ **GiaHanVatPham**: Fix bug reject item có option phụ

---

## 📄 License

Private project. All rights reserved.
