# 🚀 HƯỚNG DẪN BAN NGAY TÀI KHOẢN BUG DANAP = 0

## ⚡ Cách nhanh nhất (3 bước)

### Bước 1: Mở MySQL Workbench hoặc HeidiSQL

**Thông tin kết nối:**
- Host: `103.157.204.182`
- User: `root`
- Password: `Nro@2026!`
- Port: `3306`
- Database: `nrotft`

### Bước 2: Mở file script

```
File: c:\Users\Admin\Desktop\tft\nrotft\sql\ban_bug_danap_now.sql
```

Hoặc copy-paste toàn bộ nội dung vào MySQL Workbench

### Bước 3: Chạy script

1. **Lần 1 - Xem danh sách:**
   - Chọn từ dòng 1 đến dòng 60 (phần BƯỚC 1-5)
   - Nhấn `Ctrl+Enter` hoặc click "Execute"
   - Xem kết quả danh sách tài khoản sẽ bị ban

2. **Lần 2 - Thực thi ban:**
   - Chọn từ dòng 65 đến hết (phần BƯỚC 6)
   - Bỏ comment: Xóa `/*` ở dòng 65 và `*/` ở dòng 110
   - Nhấn `Ctrl+Enter` để chạy
   - Xem kết quả ban

## 📊 Output sẽ hiển thị

### Lần 1 - Xem danh sách:
```
=== DANH SÁCH TÀI KHOẢN SẼ BỊ BAN ===
total_accounts: 15
total_recharge_amount: 5000000

account_id | username | ip_address | cash | vnd | danap | total_recharge | reason
-----------|----------|------------|------|-----|-------|----------------|--------
123        | hacker1  | 1.2.3.4    | 500k | 100 | 0     | 5000000        | Danap = 0 nhưng có lịch sử nạp thẻ
124        | hacker2  | 1.2.3.4    | 200k | 50  | 0     | 2000000        | Cùng IP với tài khoản bug
...
```

### Lần 2 - Thực thi ban:
```
=== KẾT QUẢ BAN ===
total_banned: 15
banned_time: 2026-04-28 16:45:00

=== CHI TIẾT ===
id  | username | ip_address | ban_reason | ban_time
----|----------|------------|------------|------------------
123 | hacker1  | 1.2.3.4    | [AUTO-BAN-BUG] Danap = 0... | 2026-04-28 16:45:00
124 | hacker2  | 1.2.3.4    | [AUTO-BAN-BUG] Cùng IP... | 2026-04-28 16:45:00
...
```

## ⚠️ Lưu ý quan trọng

1. **Luôn xem danh sách trước khi ban**
   - Chạy lần 1 để xem ai sẽ bị ban
   - Kiểm tra kỹ tier 1 (cùng IP)

2. **Backup database trước**
   ```sql
   -- Chạy trong MySQL Workbench:
   -- Hoặc dùng mysqldump từ command line
   ```

3. **Restart server sau khi ban**
   ```
   cd C:\Users\Admin\Desktop\tft\nrotft
   .\restart.bat
   ```

## 🔍 Nếu muốn kiểm tra chi tiết hơn

### Xem tài khoản danap = 0 có giao dịch nạp
```sql
SELECT 
    a.id,
    a.username,
    a.danap,
    COUNT(t.id) as total_transactions,
    SUM(t.amount) as total_amount
FROM account a
LEFT JOIN transaction_history t ON t.account_id = a.id AND t.type = 'RECHARGE'
WHERE a.danap = 0 
  AND a.ban = 0
GROUP BY a.id
HAVING COUNT(t.id) > 0
ORDER BY total_amount DESC;
```

### Xem tài khoản cùng IP
```sql
SELECT 
    a.ip_address,
    COUNT(*) as total_accounts,
    GROUP_CONCAT(a.username SEPARATOR ', ') as usernames
FROM account a
WHERE a.ip_address IS NOT NULL 
  AND a.ip_address != '' 
  AND a.ip_address != '127.0.0.1'
  AND a.ban = 0
GROUP BY a.ip_address
HAVING COUNT(*) > 1
ORDER BY COUNT(*) DESC;
```

## 📋 Checklist trước khi ban

- [ ] Đã mở MySQL Workbench/HeidiSQL
- [ ] Đã kết nối database thành công
- [ ] Đã chạy lần 1 để xem danh sách
- [ ] Đã xem xét kỹ danh sách
- [ ] Đã backup database (nếu cần)
- [ ] Đã bỏ comment phần BƯỚC 6
- [ ] Đã chạy lần 2 để thực thi ban
- [ ] Đã restart server

## 🆘 Troubleshooting

### Lỗi: "Cannot connect to database"
- Kiểm tra host, user, password
- Kiểm tra firewall
- Kiểm tra MySQL service đang chạy

### Lỗi: "Access denied"
- Kiểm tra password: `Nro@2026!`
- Kiểm tra user: `root`

### Lỗi: "Unknown database"
- Kiểm tra database name: `nrotft`

### Script chạy chậm
- Bình thường, database lớn có thể mất 1-2 phút
- Kiểm tra kết nối mạng

## 📞 Hỗ trợ

Nếu có vấn đề:
1. Kiểm tra log: `ban_log` table
2. Rollback nếu cần:
   ```sql
   UPDATE account SET ban = 0, ban_reason = NULL 
   WHERE ban_time >= 'YYYY-MM-DD HH:MM:SS';
   ```

## ✅ Sau khi ban xong

1. **Restart server**
   ```
   cd C:\Users\Admin\Desktop\tft\nrotft
   .\restart.bat
   ```

2. **Thông báo cho người chơi**
   - Tạo thông báo trong game
   - Giải thích lý do ban

3. **Theo dõi**
   - Kiểm tra xem có tài khoản mới từ cùng IP không
   - Theo dõi phản hồi từ cộng đồng

---

**Bạn đã sẵn sàng! Chạy script ngay bây giờ! 🚀**
