@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ========================================
:: 食品溯源系统 - 卸载程序
:: ========================================

title 食品溯源系统卸载程序

set "INSTALL_DIR=%~dp0.."
set "LOG_FILE=%INSTALL_DIR%\uninstall.log"

echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║                                                            ║
echo ║          食品溯源系统 - 卸载程序                            ║
echo ║                                                            ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

:: 确认卸载
set /p "CONFIRM=确定要卸载食品溯源系统吗？(Y/N): "
if /i "%CONFIRM%" neq "Y" (
    echo 已取消卸载
    pause
    exit /b 0
)

echo.
echo [*] 开始卸载...

:: 停止服务
echo [*] 停止运行中的服务...
taskkill /f /im java.exe 2>nul
taskkill /f /im python.exe 2>nul
echo [√] 服务已停止

:: 删除系统服务
echo [*] 删除系统服务...
sc delete "FoodTraceability-Backend" 2>nul
sc delete "FoodTraceability-OCR" 2>nul
schtasks /delete /tn "FoodTraceability-Backend" /f 2>nul
schtasks /delete /tn "FoodTraceability-OCR" /f 2>nul
echo [√] 系统服务已删除

:: 删除虚拟环境
echo [*] 删除Python虚拟环境...
if exist "%INSTALL_DIR%\ocr-service\venv" (
    rmdir /s /q "%INSTALL_DIR%\ocr-service\venv"
    echo [√] 虚拟环境已删除
)

:: 删除日志文件
echo [*] 删除日志文件...
del /q "%INSTALL_DIR%\*.log" 2>nul
del /q "%INSTALL_DIR%\ocr-service\*.log" 2>nul
del /q "%INSTALL_DIR%\backend\*.log" 2>nul
echo [√] 日志文件已删除

:: 询问是否删除Python环境
echo.
set /p "DEL_PYTHON=是否删除Python虚拟环境？(Y/N): "
if /i "%DEL_PYTHON%"=="Y" (
    :: 删除conda环境
    conda env remove -n paddleocr -y 2>nul
    echo [√] Python环境已删除
)

:: 询问是否删除MySQL数据
echo.
set /p "DEL_DATA=是否删除数据库数据？(Y/N): "
if /i "%DEL_DATA%"=="Y" (
    echo [!] 请手动删除MySQL数据库或H2数据文件
)

:: 删除启动脚本
echo [*] 删除启动脚本...
del /q "%INSTALL_DIR%\start.bat" 2>nul
del /q "%INSTALL_DIR%\stop.bat" 2>nul
echo [√] 启动脚本已删除

echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║                                                            ║
echo ║                  卸载完成！                                ║
echo ║                                                            ║
echo ╠════════════════════════════════════════════════════════════╣
echo ║                                                            ║
echo ║  以下组件未被删除（可能被其他程序使用）：                    ║
echo ║    - Java运行环境                                          ║
echo ║    - Python运行环境                                        ║
echo ║    - MySQL数据库                                           ║
echo ║                                                            ║
echo ║  如需完全卸载，请手动删除这些组件。                          ║
echo ║                                                            ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

pause
exit /b 0
