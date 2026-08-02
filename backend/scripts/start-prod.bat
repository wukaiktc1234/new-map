@echo off
chcp 65001 >nul
echo ==========================================
echo 生产环境启动脚本 - Food Traceability System
echo ==========================================

echo 当前目录: %~dp0
cd /d "%~dp0\.."
echo 切换到项目根目录: %cd%

echo.
echo 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Java环境未找到，请确保已安装Java 21或更高版本
    pause
    exit /b 1
)

echo.
echo 检查环境变量配置...
if not exist "%cd%\.env" (
    echo 警告：未找到.env文件，将使用默认配置
    echo 请确保已配置必要的环境变量，如数据库连接信息
)

echo.
echo 查找最新的JAR包...
for /f "delims=" %%i in ('dir /b /o-d target\food-traceability-*.jar ^| findstr /v "sources" ^| findstr /v "javadoc"') do (
    set JAR_FILE=%%i
    goto :found
)

:found
if not defined JAR_FILE (
    echo 错误：未找到JAR包，请先运行构建脚本
    pause
    exit /b 1
)

echo 找到JAR包：%JAR_FILE%
set FULL_JAR_PATH=%cd%\target\%JAR_FILE%

echo.
echo 检查端口占用情况（8083）...
netstat -ano | findstr :8083 >nul
if %errorlevel% equ 0 (
    echo 警告：端口8083已被占用
    echo 正在尝试终止占用端口的进程...
    for /f "tokens=5" %%i in ('netstat -ano ^| findstr :8083') do (
        taskkill /f /pid %%i >nul 2>&1
        if %errorlevel% equ 0 (
            echo 已终止进程：%%i
        )
    )
)

echo.
echo 正在启动生产环境应用...
echo 启动命令：java -jar %FULL_JAR_PATH% --spring.profiles.active=prod

rem 创建logs目录
if not exist "%cd%\logs" mkdir "%cd%\logs"

rem 启动应用并记录日志
start "Food Traceability System" /B java -jar %FULL_JAR_PATH% --spring.profiles.active=prod > "%cd%\logs\backend-prod.log" 2>&1

echo.
echo 应用已启动，日志文件：%cd%\logs\backend-prod.log

echo.
echo 正在检查应用启动状态...
set /a counter=0
:check_start
set /a counter+=1
if %counter% gtr 30 (
    echo 应用启动超时，请检查日志文件
    goto :end
)

rem 检查日志中是否有启动成功的迹象
type "%cd%\logs\backend-prod.log" | findstr "Started FoodTraceabilityApplication" >nul
if %errorlevel% equ 0 (
    echo 应用启动成功！
    goto :end
)

type "%cd%\logs\backend-prod.log" | findstr "Error" >nul
if %errorlevel% equ 0 (
    echo 应用启动时发生错误，请检查日志文件
    goto :end
)

echo 等待应用启动... %counter%/30
timeout /t 2 /nobreak >nul
goto :check_start

:end
echo.
echo ==========================================
echo 启动脚本执行完成
echo 您可以使用以下命令查看日志：
echo tail -f %cd%\logs\backend-prod.log
echo ==========================================
pause
