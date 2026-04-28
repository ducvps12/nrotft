# HƯỚNG DẪN CHẠY SCRIPT POWERSHELL PHÁT HIỆN GIAN LẬN

## 📋 Yêu cầu

1. **PowerShell 5.0+** (Windows 10/11 có sẵn)
2. **MySQL Connector for .NET** (cần cài đặt)
3. **Quyền Admin** (để chạy script)

## 🔧 Cài đặt MySQL Connector

### Cách 1: Cài qua NuGet (Dễ nhất)

```powershell
# Mở PowerShell as Administrator
# Chạy lệnh sau:

Install-Package MySql.Data -ProviderName NuGet -Force

# Hoặc nếu chưa có NuGet:
Install-PackageProvider -Name NuGet -Force
Install-Package MySql.Data -ProviderName NuGet -Force
```

### Cách 2: Download thủ công

1. Tải từ: https://dev.mysql.com/downloads/connector/net/
2. Cài đặt file .msi
3. Thêm vào PATH hoặc copy DLL vào thư mục script

### Cách 3: Dùng Chocolatey

```powershell
choco install mysql-connector-net
```

## 🚀 Chạy Script

### Bước 1: Mở PowerShell as Administrator

```powershell
# Nhấn Win + X, chọn "Windows PowerShell (Admin)"
# Hoặc tìm PowerShell trong Start Menu, right-click > Run as administrator
```

### Bước 2: Cho phép chạy script

```powershell
# Chạy lệnh này một lần:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Bước 3: Chạy script

```powershell
# Điều hướng đến thư mục project
cd C:\Users\Admin\Desktop\tft\nrotft

# Chạy script (sẽ hỏi xác nhận trước khi ban)
.\fraud_detection_auto.ps1

# Hoặc chạy tự động ban (không hỏi)
.\fraud_detection_auto.ps1 -AutoBan

# Hoặc không kiểm tra bùa x2
.\fraud_detection_auto.ps1 -CheckBuaX2:$false
```

## 📊 Output của Script

Script sẽ hiển thị:

```
╔════════════════════════════════════════════════════════╗
║   HỆ THỐNG PHÁT HIỆN VÀ BAN GIAN LẬN TỰ ĐỘNG          ║
║   NRO TFT - 2026-04-28 16:35:00                        ║
╚════════════════════════════════════════════════════════╝

=== KIỂM TRA KẾT NỐI DATABASE ===
✓ Kết nối thành công!
  Host: 103.157.204.182
  Database: nrotft

=== KIỂM TRA NHANH TÀI KHOẢN NGHI NGỜ ===
  • Danap = 0 nhưng có giao dịch: 5
  • Cùng IP (>2 tài khoản): 3
  • Giao dịch bất thường 24h: 2
  • Cash/VND bất thường: 1

=== PHÁT HIỆN TÀI KHOẢN GIAN LẬN ===
Đang phát hiện... (có thể mất vài phút)
  ✓ Đã xử lý 5 bước
  ✓ Đã xử lý 10 bước
✓ Phát hiện hoàn tất!

=== KIỂM TRA BÙA X2 ĐỆ ===
Danh sách người chơi với bùa:
  • player1 (user1): CÓ BÙA X2
  • player2 (user2): KHÔNG CÓ BÙA

=== DANH SÁCH TÀI KHOẢN NGHI NGỜ ===
  Tier 0 (Gốc): 5 tài khoản
  Tier 1 (Cùng IP): 12 tài khoản
  Tier 2 (Giao dịch): 20 tài khoản
  Tier 3 (Cảnh báo): 8 tài khoản

Tổng cộng: 45 tài khoản nghi ngờ

=== XÁC NHẬN THỰC THI BAN ===
⚠️  CẢNH BÁO: Bạn sắp ban các tài khoản gian lận!
Hành động này KHÔNG THỂ HOÀN TÁC!

Bạn có chắc chắn muốn tiếp tục? (yes/no): yes

=== THỰC THI BAN ===
Đang ban tài khoản... (có thể mất vài phút)
  ✓ Đã xử lý 5 bước
  ✓ Đã xử lý 10 bước
✓ Ban hoàn tất!

=== THỐNG KÊ KẾT QUẢ ===
Tổng tài khoản đã ban: 45
Ban trong 1 giờ qua: 45

⚠️  Hãy RESTART SERVER để kick người chơi đang online!

✓ Hoàn tất!
```

## 🎯 Các tùy chọn chạy

### 1. Chạy bình thường (hỏi xác nhận)
```powershell
.\fraud_detection_auto.ps1
```

### 2. Chạy tự động ban (không hỏi)
```powershell
.\fraud_detection_auto.ps1 -AutoBan
```

### 3. Không kiểm tra bùa x2
```powershell
.\fraud_detection_auto.ps1 -CheckBuaX2:$false
```

### 4. Kết hợp cả hai
```powershell
.\fraud_detection_auto.ps1 -AutoBan -CheckBuaX2:$false
```

## 🔍 Kiểm tra bùa x2 đệ tử

Nếu bạn muốn kiểm tra chi tiết bùa x2 đệ tử:

```powershell
# Mở MySQL Workbench hoặc HeidiSQL
# Chạy script: sql/check_bua_x2_de.sql

# Hoặc từ PowerShell:
mysql -h 103.157.204.182 -u root -p"Nro@2026!" nrotft < sql/check_bua_x2_de.sql
```

## ⚠️ Lưu ý quan trọng

1. **Luôn xem xét danh sách trước khi ban**
   - Có thể có false positive
   - Kiểm tra kỹ tier 1 (cùng IP)

2. **Backup database trước**
   ```powershell
   mysqldump -h 103.157.204.182 -u root -p"Nro@2026!" nrotft > backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql
   ```

3. **Restart server sau khi ban**
   ```powershell
   cd C:\Users\Admin\Desktop\tft\nrotft
   .\restart.bat
   ```

4. **Kiểm tra log sau khi ban**
   ```sql
   SELECT * FROM fraud_detection_log ORDER BY created_at DESC LIMIT 50;
   ```

## 🐛 Troubleshooting

### Lỗi: "The term 'mysql' is not recognized"
- MySQL chưa được thêm vào PATH
- Cài MySQL Server hoặc MySQL Connector

### Lỗi: "Cannot connect to database"
- Kiểm tra host, user, password
- Kiểm tra firewall
- Kiểm tra MySQL service đang chạy

### Lỗi: "MySql.Data not found"
- Cài MySQL Connector: `Install-Package MySql.Data -ProviderName NuGet -Force`

### Script chạy chậm
- Bình thường, database lớn có thể mất 5-10 phút
- Kiểm tra kết nối mạng

## 📅 Chạy định kỳ

### Tạo Scheduled Task (Windows)

```powershell
# Tạo trigger chạy mỗi ngày lúc 3h sáng
$trigger = New-ScheduledTaskTrigger -Daily -At 3am
$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File C:\Users\Admin\Desktop\tft\nrotft\fraud_detection_auto.ps1 -AutoBan"
Register-ScheduledTask -TaskName "NRO_Fraud_Detection" -Trigger $trigger -Action $action -RunLevel Highest
```

### Xem scheduled task
```powershell
Get-ScheduledTask -TaskName "NRO_Fraud_Detection"
```

### Xóa scheduled task
```powershell
Unregister-ScheduledTask -TaskName "NRO_Fraud_Detection" -Confirm:$false
```

## 📞 Hỗ trợ

Nếu có vấn đề:
1. Kiểm tra log: `fraud_detection_log` table
2. Kiểm tra bảng tạm: `temp_suspicious_accounts`
3. Rollback nếu cần:
   ```sql
   UPDATE account SET ban = 0, ban_reason = NULL 
   WHERE ban_time >= 'YYYY-MM-DD HH:MM:SS';
   ```

## ✅ Checklist trước khi chạy

- [ ] Đã backup database
- [ ] Đã test script trên database test
- [ ] Đã cài MySQL Connector
- [ ] Đã cho phép chạy script (Set-ExecutionPolicy)
- [ ] Đã chuẩn bị restart server
- [ ] Đã chuẩn bị thông báo cho người chơi
