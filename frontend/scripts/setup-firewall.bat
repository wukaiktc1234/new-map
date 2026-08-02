@echo off
chcp 65001 >nul
title H5表单服务器 - 防火墙配置

echo ========================================
echo   🛡️  配置防火墙规则（允许手机访问）
echo ========================================
echo.

:: 检查管理员权限
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ❌ 错误：需要管理员权限！
    echo.
    echo 请右键点击此文件，选择"以管理员身份运行"
    echo.
    pause
    exit /b 1
)

echo ✅ 管理员权限确认通过
echo.
echo 📋 正在添加防火墙规则...
echo.

:: 删除旧规则（如果存在）
netsh advfirewall firewall delete rule name="H5 Form Server 3003" >nul 2>&1

:: 添加新规则 - 允许3003端口入站连接
netsh advfirewall firewall add rule ^
    name="H5 Form Server 3003" ^
    dir=in ^
    action=allow ^
    protocol=TCP ^
    localport=3003 ^
    profile=private ^
    description="允许内网设备访问H5应聘表单"

if %errorLevel% equ 0 (
    echo ✅ 成功：端口 3003 已允许访问
) else (
    echo ❌ 失败：添加防火墙规则出错
    pause
    exit /b 1
)

echo.
echo ========================================
echo   ✅ 配置完成！
echo ========================================
echo.
echo 📱 手机访问地址：
echo    http://192.168.0.106:3003/recruit/apply.html?job=岗位ID
echo.
echo 🔧 测试步骤：
echo    1. 确保手机与电脑在同一WiFi下
echo    2. 打开手机浏览器，访问上面的地址
echo    3. 或扫描管理端生成的二维码
echo.
echo ⚠️  注意：此规则仅对'专用网络'生效
echo.
pause
