# 🎁 HƯỚNG DẪN GIFTCODE & NẠP TIỀN

## 📋 GIFTCODE HIỆN CÓ

| ID | Code | Số lượng còn | Quà tặng |
|----|------|--------------|----------|
| 35 | **mtv** | 0 (hết) | 🎁 1 Rương (-4), 10 Thỏi vàng (457), 100 Ruby (861), 1 Trang bị đặc biệt (1454), 1 Pet (568), 50 Cỏ 4 lá (1705) |
| 36 | **item** | 30 | 🎁 99x Đá nâng cấp (381-384), 99x Mảnh ghép (395) |
| 37 | **dnc** | 30 | 🎁 99999x Đá nâng cấp cao cấp (220-224) |
| 39 | **free** | 30 | 🎁 15,000 Thỏi vàng (457) |
| 40 | **dbv** | 30 | 🎁 1,000,000,000 Vàng (987) |

---

## 🔍 LUỒNG GIFTCODE

### **1. Người chơi nhập code**
```
Input.java (dòng 444) → GiftCodeService.gI().giftCode(player, text[0])
```

### **2. Kiểm tra code**
```java
// Kiểm tra trong database
SELECT * FROM giftcode WHERE code = ?

// Kiểm tra điều kiện:
- Code có tồn tại không?
- count_left > 0 (còn lượt)?
- expired > NOW() (chưa hết hạn)?
- Player đã dùng code này chưa?
```

### **3. Trao quà**
```java
// Parse JSON detail từ database
detail = [{
    "temp_id": 457,      // ID vật phẩm
    "quantity": 10,      // Số lượng
    "options": [{        // Thuộc tính
        "id": 30,
        "param": 0
    }]
}]

// Thêm vào túi đồ player
player.inventory.addItem(item)

// Giảm count_left
UPDATE giftcode SET count_left = count_left - 1 WHERE id = ?
```

---

## 💰 LUỒNG NẠP TIỀN

### **1. Webhook nhận thông báo**
```
RechargeHttp.java (dòng 107) → processTopup()
```

### **2. Xử lý nạp tiền**
```java
// Tính tiền nhận được (có hệ số sự kiện)
int soTienCong = amount * HE_SO_SU_KIEN;

// Update database (FIX: Không dùng JOIN)
UPDATE account 
SET cash = cash + ?, 
    vnd = vnd + ?, 
    danap = danap + ?
WHERE id = (SELECT account_id FROM player WHERE id = ?)

// Ghi log
INSERT INTO recharge_log(trans_id, amount, description, status, created_at) 
VALUES (?, ?, ?, 1, NOW())
```

### **3. Thông báo cho player (nếu online)**
```java
Player pl = Client.gI().getPlayer(playerId);
if (pl != null && pl.getSession() != null) {
    // Sync session với database
    pl.getSession().cash += soTienCong;
    pl.getSession().vnd += soTienCong;
    pl.getSession().danap += amount;
    
    // Gửi thông báo
    Service.gI().sendThongBao(pl, 
        "Bạn đã nạp " + amount + " VNĐ (nhận " + soTienCong + " VNĐ, X" + HE_SO_SU_KIEN + ")"
    );
    
    // Refresh UI
    Service.gI().sendMoney(pl);
}
```

---

## 🎯 KIỂM TRA CODE CÓ HIỆU LỰC

### **Cách 1: Qua Database**
```sql
SELECT 
    code,
    count_left,
    expired,
    CASE 
        WHEN count_left <= 0 THEN '❌ Hết lượt'
        WHEN expired < NOW() THEN '❌ Hết hạn'
        ELSE '✅ Còn hiệu lực'
    END as status
FROM giftcode;
```

### **Cách 2: Qua Panel Admin**
1. Mở Server Control Panel
2. Vào **Quản Lý Giftcode**
3. Xem danh sách code và trạng thái

### **Cách 3: Test trong game**
1. Login vào game
2. Nhập code
3. Xem thông báo:
   - ✅ "Nhận quà thành công" → Code hợp lệ
   - ❌ "Code không tồn tại" → Code sai
   - ❌ "Code đã hết lượt" → count_left = 0
   - ❌ "Code đã hết hạn" → expired < NOW()
   - ❌ "Bạn đã dùng code này" → Đã nhận rồi

---

## 🛠️ QUẢN LÝ GIFTCODE

### **Tạo code mới**
```sql
INSERT INTO giftcode (code, count_left, detail, datecreate, expired, type) 
VALUES (
    'NEWCODE2026',
    100,
    '[{"temp_id":457,"quantity":1000,"options":[{"id":30,"param":0}]}]',
    NOW(),
    '2026-12-31 23:59:59',
    0
);
```

### **Reload code trong game**
1. Mở Dashboard Panel
2. Click **Reload Data** → **GiftCode**
3. Hoặc restart server

---

## 📊 THEO DÕI NẠP TIỀN

### **Xem lịch sử nạp**
```sql
SELECT 
    r.id,
    r.trans_id,
    r.account_id,
    a.username,
    r.amount,
    r.description,
    r.status,
    r.created_at
FROM recharge_log r
LEFT JOIN account a ON r.account_id = a.id
ORDER BY r.created_at DESC
LIMIT 20;
```

### **Kiểm tra tài khoản**
```sql
SELECT 
    username,
    tongnap,    -- Tổng đã nạp
    danap,      -- Tracking nạp
    cash,       -- Tiền hiện có
    vnd         -- VND hiện có
FROM account
WHERE username = 'Ninh';
```

---

## 🚨 LƯU Ý QUAN TRỌNG

1. **Giftcode "mtv" đã hết** (count_left = 0)
2. **Code "dbv" cho 1 tỷ vàng** → Cẩn thận lạm phát!
3. **Hệ số sự kiện** (HE_SO_SU_KIEN) ảnh hưởng số tiền nhận được
4. **Luôn kiểm tra danap** sau khi nạp để đảm bảo tracking đúng

---

## 🎯 NPC BẢNG DANH VỌNG

**Vị trí:** Map 5 (Rừng Bamboo) - Tọa độ x=350
**Chức năng:**
- Xem Top Sức mạnh
- Xem Top Nạp
- Xem thông tin sự kiện
- Hướng dẫn tân thủ

**Đã fix:** Chuyển từ map nhà (21-23) sang map luyện tập (5) để tân thủ dễ thấy!
