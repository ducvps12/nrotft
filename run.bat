@echo off
title NROTFT Server

:: =====================================================================
:: CAU HINH RAM - Tu dong doc tu ram_config.txt (do Panel tao)
:: Neu khong co file, dung gia tri mac dinh
:: =====================================================================
set MIN_RAM=1G
set MAX_RAM=4G

setlocal enabledelayedexpansion
:: Doc RAM tu file config (neu ton tai)
if exist ram_config.txt (
    set /a linenum=0
    for /f "usebackq tokens=1 delims=" %%a in ("ram_config.txt") do (
        set /a linenum+=1
        if !linenum! equ 1 set MIN_RAM=%%a
        if !linenum! equ 2 set MAX_RAM=%%a
    )
)

:: =====================================================================
:: TOI UU HOA GOM RAC (GARBAGE COLLECTION) TREN JAVA 21
:: =====================================================================
set JAVA_OPTS=-Xms%MIN_RAM% -Xmx%MAX_RAM% -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heapdump.hprof

echo =========================================================
echo Dang khoi dong NROTFT Server...
echo RAM Khoi dong:    %MIN_RAM%
echo RAM Toi da:       %MAX_RAM%
echo =========================================================

"C:\Program Files\Java\jdk-21\bin\java.exe" %JAVA_OPTS% -cp "dist\NROTFT.jar;lib\*" nro.server.ServerManager
pause