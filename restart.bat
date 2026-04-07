@echo off
REM — Số giây chờ JVM cũ tắt
set WAIT_SEC=60

REM — Chuyển vào thư mục chứa script
cd /d "%~dp0"

REM — Chờ im lặng
timeout /t %WAIT_SEC% /nobreak > nul

REM — Bật run.bat (mở cửa sổ mới), rồi thoát luôn
start "" "%~dp0run.bat"
exit
