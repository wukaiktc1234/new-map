@echo off
chcp 65001 >nul
echo ==========================================
echo 生产环境停止脚本 - Food Traceability System
echo ==========================================

echo.
echo 检查应用是否在运行...

rem 检查端口占用情况
netstat -ano | findstr :8083 >nul
if %errorlevel% neq 0 (
    echo 应用未在运行
    pause
    exit /b 0
)

echo 应用正在运行，查找进程ID...

for /f "tokens=5" %%i in ('netstat -ano ^| findstr :8083') do (
    set PID=%%i
)

echo 找到进程ID：%PID%

echo.
echo 正在终止进程...
taskkill /f /pid %PID%
if %errorlevel% equ 0 (
    echo 应用已成功停止
) else (
    echo 停止应用时发生错误
    pause
    exit /b 1
)

echo.
echo 验证应用是否已停止...
netstat -ano | findstr :8083 >nul
if %errorlevel% neq 0 (
    echo 验证：应用已成功停止
) else (
    echo 验证：应用仍在运行，请手动检查
    pause
    exit /b 1
)

echo.
echo ==========================================
echo 停止脚本执行完成
echo ==========================================
pause
