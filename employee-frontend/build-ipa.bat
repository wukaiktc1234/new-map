@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

:: ============================================================
::  build-ipa.bat — iOS IPA 打包辅助脚本（Windows 端）
::
::  功能：
::    1. 构建 Web 资源 (npm run build:app)
::    2. 同步到 iOS 目录 (npx cap sync ios)
::    3. 输出构建状态和下一步指引
::
::  使用方法：
::    双击运行 或 命令行: build-ipa.bat
::
::  注意：
::    本脚本完成的是"打包前准备工作"
::    最终的 .ipa 文件需要 macOS + Xcode 或 云构建服务
:: ============================================================

echo.
echo ════════════════════════════════════════════════
echo   员工门户 - iOS 打包工具
echo   Food Traceability Employee App Builder
echo ════════════════════════════════════════════════
echo.

:: ---- 检查 Node.js ----
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 Node.js，请先安装 Node.js
    echo        下载地址: https://nodejs.org/
    pause
    exit /b 1
)

echo [1/4] 检查环境...
node -v
npm -v
echo.

:: ---- 检查是否在项目根目录 ----
if not exist "package.json" (
    echo [ERROR] 请在 employee-frontend 目录下运行此脚本
    pause
    exit /b 1
)

if not exist "capacitor.config.ts" (
    echo [ERROR] 未找到 capacitor.config.ts，请确认 Capacitor 已安装
    pause
    exit /b 1
)

echo [2/4] 构建 Web 资源...
set "PATH=P:\my-new-project\tools\nodejs;%PATH%"
call npm run build:app
if %errorlevel% neq 0 (
    echo [ERROR] Web 构建失败，请检查错误信息
    pause
    exit /b 1
)
echo [OK] Web 构建成功
echo.

:: ---- 同步到 iOS ----
echo [3/4] 同步到 iOS 工程...
call npx cap sync ios
if %errorlevel% neq 0 (
    echo [ERROR] iOS 同步失败，请检查错误信息
    pause
    exit /b 1
)
echo [OK] iOS 同步成功
echo.

:: ---- 验证输出 ----
echo [4/4] 验证构建产物...
if exist "ios\App\App.xcodeproj" (
    echo [OK] Xcode 工程文件:     ios\App\App\xcodeproj
) else (
    echo [WARN] 未找到 Xcode 工程文件
)

if exist "ios\App\App\public\index.html" (
    echo [OK] Web 资源:           ios\App\App\public\ (已同步)
) else (
    echo [WARN] Web 资源未同步
)

if exist "ios\App\App\Info.plist" (
    echo [OK] 应用配置:           Info.plist (已配置)
) else (
    echo [WARN] Info.plist 缺失
)

echo.
echo ════════════════════════════════════════════════
echo   准备工作完成！
echo ══════════════════════════════════════════════
echo.
echo   下一步（选择一种方式生成 .ipa）：
echo.
echo   方式 1: Sideloadly（推荐 - 免费）
echo     1. 下载 Sideloadly: https://sideloadly.io/
echo     2. 连接 iPhone 到电脑
echo     3. Sideloadly 选择文件: ios\App\App.xcodeproj
echo     4. 输入免费 Apple ID
echo     5. 点击 Start → 自动安装到 iPhone
echo.
echo   方式 2: AltStore（免费）
echo     1. 下载 AltStore + AltServer: https://altstore.io/
echo     2. iPhone 安装 AltStore
echo     3. 电脑安装 AltServer，连接 WiFi
echo     4. AltStore 中导入: ios\App\App.xcodeproj
echo     5. 安装 → 完成
echo.
echo   方式 3: 云构建（无需 Mac - 免费）
echo     1. 注册 Codemagic: https://codemagic.io/
echo     2. Fork 项目到 GitHub
echo     3. 新建 Project → 选择仓库
echo     4. 使用 codemagic.yaml 配置自动构建
echo     5. 构建完成后下载 .ipa
echo.
echo   方式 4: macOS + Xcode（完整流程）
echo     1. 将整个项目拷贝到 Mac
echo     2. 运行: npx cap open ios
echo     3. Xcode → Product → Archive
echo     4. Distribute App → 导出 .ipa
echo.
echo ════════════════════════════════════════════════
echo.

pause
