# HƯỚNG DẪN SỬ DỤNG HỆ THỐNG PHÁT HIỆN VÀ BAN GIAN LẬN

## 📋 Tổng quan

Hệ thống này giúp phát hiện và ban tài khoản gian lận theo mô hình affiliate 3 tầng:
- **Tier 0**: Tài khoản gốc (gian lận chính)
- **Tier 1**: Tài khoản cùng IP
- **Tier 2**: Tài khoản có giao dịch trực tiếp
- **Tier 3**: Tài khoản có giao dịch gián tiếp (chỉ cảnh báo)

## 🚀 Cách sử dụng

### Bước 1: Phát hiện tài khoản nghi ngờ

```bash
mysql -u root -p nrotftchuan < sql/detect_and_ban_fraud_accounts.sql > fraud_report.txt
```

Hoặc trong MySQL Workbench:
1. Mở file `detect_and_ban_fraud_accounts.sql`
2. Chạy toàn bộ script
3. Xem kết quả ở tab "Result Grid"

### Bước 2: Xem xét danh sách

Script sẽ hiển thị:
```
=== THỐNG KÊ TỔNG QUAN ===
total_suspicious: 45
tier_0_root: 5
tier_1_same_ip: 12
tier_2_transaction: 20
tier_3_indirect: 8
```

Và danh sách chi tiết:
```
tier | account_id | username | ip_address | reason
-----|------------|----------|------------|--------
0    | 123        | hacker1  | 1.2.3.4    | Danap = 0 nhưng có lịch sử nạp thẻ
1    | 124        | hacker2  | 1.2.3.4    | Cùng IP với tài khoản gian lận: hacker1
...
```

### Bước 3: Xác nhận và thực thi ban

**⚠️ QUAN TRỌNG: Xem xét kỹ danh sách trước khi ban!**

```bash
mysql -u root -p nrotftchuan < sql/execute_ban_fraud_accounts.sql
```

### Bước 4: Restart server

Sau khi ban, cần restart server để kick người chơi đang online:
```bash
cd c:\Users\Admin\Desktop\tft\nrotft
.\restart.bat
```

## 🔍 Các tiêu chí phát hiện

### Tier 0 (Gốc - Gian lận chính)

1. **Danap = 0 nhưng có lịch sử nạp thẻ**
   - Có thể do bug hoặc hack database

2. **Giao dịch chuyển tiền bất thường**
   - Hơn 10 lần chuyển vàng/ngọc trong 24h

3. **Cash/VND bất thường**
   - Cash/VND âm
   - Cash/VND > 1M nhưng danap = 0

4. **Tạo nhiều nhân vật**
   - Hơn 5 nhân vật trong 1 giờ

### Tier 1 (Cùng IP)

- Tất cả tài khoản cùng IP với Tier 0
- Loại trừ: localhost (127.0.0.1)

### Tier 2 (Giao dịch trực tiếp)

- Nhận hoặc gửi vàng/ngọc cho Tier 0 hoặc Tier 1
- Trong vòng 7 ngày gần đây

### Tier 3 (Giao dịch gián tiếp)

- Có giao dịch với Tier 2
- Trong vòng 3 ngày gần đây
- **Chỉ cảnh báo, không ban ngay**

## 📊 Xem log

```sql
-- Xem tất cả log phát hiện gian lận
SELECT * FROM fraud_detection_log 
ORDER BY created_at DESC 
LIMIT 100;

-- Xem theo action
SELECT action, COUNT(*) as total
FROM fraud_detection_log
GROUP BY action;

-- Xem theo ngày
SELECT DATE(created_at) as date, COUNT(*) as total
FROM fraud_detection_log
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

## 🛠️ Tùy chỉnh

### Điều chỉnh ngưỡng phát hiện

Sửa trong file `detect_and_ban_fraud_accounts.sql`:

```sql
-- Thay đổi số lần giao dịch bất thường
HAVING COUNT(*) > 10;  -- Đổi thành 5, 15, 20...

-- Thay đổi khoảng thời gian
AND t.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)  -- Đổi thành 3, 14, 30...

-- Thay đổi ngưỡng cash
OR (a.cash > 1000000 AND a.danap = 0)  -- Đổi thành 500000, 2000000...
```

### Loại trừ tài khoản admin

Thêm vào mỗi query:
```sql
AND a.is_admin = 0
AND a.vip < 5  -- Loại trừ VIP cao
```

### Chỉ ban một số tier

Trong file `execute_ban_fraud_accounts.sql`, comment các tier không muốn ban:
```sql
-- 3.1: Ban TIER 0
UPDATE account a...  -- Giữ lại

-- 3.2: Ban TIER 1
-- UPDATE account a...  -- Comment để không ban tier 1

-- 3.3: Ban TIER 2
-- UPDATE account a...  -- Comment để không ban tier 2
```

## 🔄 Chạy định kỳ

### Tạo cron job (Linux)

```bash
# Chạy mỗi ngày lúc 3h sáng
0 3 * * * cd /path/to/nrotft && mysql -u root -pPASSWORD nrotftchuan < sql/detect_and_ban_fraud_accounts.sql > /tmp/fraud_$(date +\%Y\%m\%d).txt
```

### Tạo scheduled task (Windows)

1. Mở Task Scheduler
2. Create Basic Task
3. Trigger: Daily, 3:00 AM
4. Action: Start a program
   - Program: `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`
   - Arguments: `-u root -pPASSWORD nrotftchuan < C:\path\to\sql\detect_and_ban_fraud_accounts.sql`

## ⚠️ Lưu ý quan trọng

1. **Luôn xem xét danh sách trước khi ban**
   - Có thể có false positive (người chơi thật bị nhầm)
   - Kiểm tra kỹ tier 1 (cùng IP) vì có thể là gia đình/quán net

2. **Backup database trước khi ban**
   ```bash
   mysqldump -u root -p nrotftchuan > backup_before_ban_$(date +%Y%m%d).sql
   ```

3. **Thông báo cho người chơi**
   - Tạo thông báo trong game về việc ban gian lận
   - Cung cấp cách appeal nếu bị nhầm

4. **Theo dõi sau khi ban**
   - Kiểm tra xem có tài khoản mới tạo từ cùng IP không
   - Theo dõi phản hồi từ cộng đồng

## 📞 Hỗ trợ

Nếu có vấn đề:
1. Kiểm tra log: `fraud_detection_log`
2. Kiểm tra bảng tạm: `temp_suspicious_accounts`
3. Rollback nếu cần:
   ```sql
   UPDATE account SET ban = 0, ban_reason = NULL 
   WHERE ban_time >= 'YYYY-MM-DD HH:MM:SS';
   ```

## 📈 Thống kê hiệu quả

```sql
-- Tổng số tài khoản đã ban
SELECT COUNT(*) as total_banned
FROM account
WHERE ban = 1;

-- Ban theo thời gian
SELECT 
    DATE(ban_time) as date,
    COUNT(*) as total
FROM account
WHERE ban = 1
GROUP BY DATE(ban_time)
ORDER BY date DESC;

-- Ban theo lý do
SELECT 
    SUBSTRING_INDEX(ban_reason, ']', 1) as ban_type,
    COUNT(*) as total
FROM account
WHERE ban = 1
GROUP BY ban_type;
```

## ✅ Checklist trước khi chạy

- [ ] Đã backup database
- [ ] Đã test script trên database test
- [ ] Đã xem xét danh sách nghi ngờ
- [ ] Đã thông báo cho team
- [ ] Đã chuẩn bị restart server
- [ ] Đã chuẩn bị thông báo cho người chơi
