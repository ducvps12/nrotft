@echo off
REM =====================================================
REM SCRIPT KIỂM TRA VÀ BAN TÀI KHOẢN BUG
REM Chạy thủ công trong MySQL Workbench/HeidiSQL
REM =====================================================

echo.
echo ╔════════════════════════════════════════════════════════╗
echo ║   HƯỚNG DẪN BAN TÀI KHOẢN BUG DANAP = 0               ║
echo ╚════════════════════════════════════════════════════════╝
echo.
echo [!] Lỗi: IP của bạn (116.98.201.250) không được phép kết nối database
echo.
echo Database chỉ cho phép kết nối từ IP được whitelist.
echo.
echo ═══════════════════════════════════════════════════════
echo CÁCH 1: Dùng MySQL Workbench/HeidiSQL (KHUYẾN NGHỊ)
echo ═══════════════════════════════════════════════════════
echo.
echo 1. Mở MySQL Workbench hoặc HeidiSQL
echo 2. Kết nối database:
echo    - Host: 103.157.204.182
echo    - User: root
echo    - Password: Nro@2026!
echo    - Port: 3306
echo    - Database: nrotft
echo.
echo 3. Mở file: sql\ban_bug_danap_now.sql
echo.
echo 4. Chạy lần 1 (xem danh sách):
echo    - Chọn từ dòng 1 đến dòng 60
echo    - Nhấn Execute (F9)
echo    - Xem danh sách tài khoản sẽ bị ban
echo.
echo 5. Chạy lần 2 (thực thi ban):
echo    - Bỏ comment: Xóa /* ở dòng 65 và */ ở dòng 110
echo    - Chọn từ dòng 65 đến hết
echo    - Nhấn Execute (F9)
echo.
echo 6. Restart server:
echo    - Chạy: restart.bat
echo.
echo ═══════════════════════════════════════════════════════
echo CÁCH 2: Thêm IP vào whitelist
echo ═══════════════════════════════════════════════════════
echo.
echo Liên hệ admin database để thêm IP: 116.98.201.250
echo vào whitelist của MySQL server
echo.
echo ═══════════════════════════════════════════════════════
echo CÁCH 3: Kết nối qua SSH Tunnel
echo ═══════════════════════════════════════════════════════
echo.
echo Nếu có SSH access đến server 103.157.204.182:
echo ssh -L 3306:localhost:3306 root@103.157.204.182
echo.
echo Sau đó kết nối MySQL qua localhost:3306
echo.
pause
