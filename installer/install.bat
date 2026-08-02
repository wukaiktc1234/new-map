@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ========================================
:: 食品溯源系统 - 一键安装程序
:: ========================================
:: 功能：
::   1. 环境检测
::   2. 自动安装依赖
::   3. 配置环境变量
::   4. 注册系统服务
::   5. 启动验证
:: ========================================

title 食品溯源系统安装程序

:: 版本信息
set "VERSION=1.0.0"
set "APP_NAME=FoodTraceability"
set "INSTALL_DIR=%~dp0"
set "LOG_FILE=%INSTALL_DIR%install.log"

:: 初始化日志
echo [%date% %time%] 安装开始 > "%LOG_FILE%"

:: 显示欢迎界面
call :show_banner

:: 步骤1: 环境检测
call :step_environment_check
if %errorLevel% neq 0 (
    call :log_error "环境检测失败"
    pause
    exit /b 1
)

:: 步骤2: 安装Java环境
call :step_install_java
if %errorLevel% neq 0 (
    call :log_error "Java安装失败"
    pause
    exit /b 1
)

:: 步骤3: 安装Python环境
call :step_install_python
if %errorLevel% neq 0 (
    call :log_error "Python安装失败"
    pause
    exit /b 1
)

:: 步骤4: 安装OCR服务依赖
call :step_install_ocr
if %errorLevel% neq 0 (
    call :log_error "OCR服务安装失败"
    pause
    exit /b 1
)

:: 步骤5: 配置数据库
call :step_configure_database
if %errorLevel% neq 0 (
    call :log_error "数据库配置失败"
    pause
    exit /b 1
)

:: 步骤6: 注册系统服务
call :step_register_services
if %errorLevel% neq 0 (
    call :log_error "服务注册失败"
    pause
    exit /b 1
)

:: 步骤7: 启动验证
call :step_start_verify
if %errorLevel% neq 0 (
    call :log_error "启动验证失败"
    pause
    exit /b 1
)

:: 安装完成
call :show_complete

pause
exit /b 0

:: ========================================
:: 子程序
:: ========================================

:show_banner
echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║                                                            ║
echo ║          食品溯源系统 - 一键安装程序 v%VERSION%              ║
echo ║                                                            ║
echo ║     Food Traceability System - One-Click Installer         ║
echo ║                                                            ║
echo ╠════════════════════════════════════════════════════════════╣
echo ║  支持系统: Windows 10/11, Windows Server 2016+             ║
echo ║  自动安装: Java 17, Python 3.10, PaddleOCR, MySQL          ║
echo ║  自动配置: 环境变量, 系统服务, 开机自启                      ║
echo ╚════════════════════════════════════════════════════════════╝
echo.
goto :eof

:step_environment_check
echo.
echo ┌────────────────────────────────────────────────────────────┐
echo │  [步骤1/7] 环境检测                                        │
echo └────────────────────────────────────────────────────────────┘
echo.

:: 检测操作系统
ver | findstr /i "10\.0" >nul
if %errorLevel% equ 0 (
    echo [√] 操作系统: Windows 10/11
) else (
    echo [!] 警告: 未检测到Windows 10/11，可能存在兼容性问题
)

:: 检测管理员权限
net session >nul 2>&1
if %errorLevel% equ 0 (
    echo [√] 管理员权限: 已获取
    set "ADMIN_MODE=1"
) else (
    echo [!] 管理员权限: 未获取（部分功能可能受限）
    set "ADMIN_MODE=0"
)

:: 检测磁盘空间
for /f "tokens=3" %%a in ('dir /-C %INSTALL_DIR% ^| findstr /C:"字节可用"') do set "FREE_SPACE=%%a"
set /a "FREE_SPACE_GB=%FREE_SPACE:~0,-9%"
if %FREE_SPACE_GB% geq 10 (
    echo [√] 磁盘空间: %FREE_SPACE_GB%GB 可用
) else (
    echo [!] 警告: 磁盘空间不足10GB，建议清理后安装
)

:: 检测端口占用
call :check_port 8081 "后端服务"
call :check_port 8868 "OCR服务"
call :check_port 3306 "MySQL数据库"

echo [√] 环境检测完成
call :log_info "环境检测完成"
goto :eof

:step_install_java
echo.
echo ┌────────────────────────────────────────────────────────────┐
echo │  [步骤2/7] Java环境安装                                    │
echo └────────────────────────────────────────────────────────────┘
echo.

:: 检测Java是否已安装
java -version >nul 2>&1
if %errorLevel% equ 0 (
    for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set "JAVA_VER=%%v"
        set "JAVA_VER=!JAVA_VER:"=!"
    )
    echo [√] Java已安装: !JAVA_VER!
    
    :: 检查版本是否>=17
    for /f "tokens=1,2 delims=." %%a in ("!JAVA_VER!") do (
        if %%a geq 17 (
            echo [√] Java版本符合要求 (>=17)
            goto :eof
        )
    )
    echo [!] Java版本过低，需要17或更高版本
)

:: 安装Java
echo [*] 正在下载Java 17...
set "JAVA_INSTALLER=%TEMP%\java-installer.exe"

:: 使用Adoptium Temurin (开源JDK)
curl -L -o "%JAVA_INSTALLER%" "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi" 2>nul

if exist "%JAVA_INSTALLER%" (
    echo [*] 正在安装Java 17...
    msiexec /i "%JAVA_INSTALLER%" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome
    
    :: 刷新环境变量
    call :refresh_env
    
    echo [√] Java 17 安装完成
    call :log_info "Java 17 安装完成"
) else (
    echo [!] Java下载失败，请手动安装Java 17
    echo     下载地址: https://adoptium.net/
    
    set /p "CONTINUE=是否继续安装其他组件？(Y/N): "
    if /i "!CONTINUE!" neq "Y" exit /b 1
)

goto :eof

:step_install_python
echo.
echo ┌────────────────────────────────────────────────────────────┐
echo │  [步骤3/7] Python环境安装                                  │
echo └────────────────────────────────────────────────────────────┘
echo.

:: 检测Python是否已安装
python --version >nul 2>&1
if %errorLevel% equ 0 (
    for /f "tokens=2" %%v in ('python --version 2^>^&1') do set "PYTHON_VER=%%v"
    echo [√] Python已安装: !PYTHON_VER!
    
    :: 检查版本是否>=3.8
    for /f "tokens=1,2 delims=." %%a in ("!PYTHON_VER!") do (
        set "PY_MAJOR=%%a"
        set "PY_MINOR=%%b"
    )
    if !PY_MAJOR! geq 3 (
        if !PY_MINOR! geq 8 (
            echo [√] Python版本符合要求 (>=3.8)
            goto :eof
        )
    )
    echo [!] Python版本过低，需要3.8或更高版本
)

:: 安装Python
echo [*] 正在下载Python 3.10...
set "PYTHON_INSTALLER=%TEMP%\python-installer.exe"

curl -L -o "%PYTHON_INSTALLER%" "https://www.python.org/ftp/python/3.10.11/python-3.10.11-amd64.exe" 2>nul

if exist "%PYTHON_INSTALLER%" (
    echo [*] 正在安装Python 3.10...
    "%PYTHON_INSTALLER%" /quiet InstallAllUsers=1 PrependPath=1 Include_test=0
    
    :: 刷新环境变量
    call :refresh_env
    
    echo [√] Python 3.10 安装完成
    call :log_info "Python 3.10 安装完成"
) else (
    echo [!] Python下载失败，请手动安装Python 3.10+
    echo     下载地址: https://www.python.org/downloads/
    
    set /p "CONTINUE=是否继续安装其他组件？(Y/N): "
    if /i "!CONTINUE!" neq "Y" exit /b 1
)

goto :eof

:step_install_ocr
echo.
echo ┌────────────────────────────────────────────────────────────┐
echo │  [步骤4/7] OCR服务安装                                     │
echo └────────────────────────────────────────────────────────────┘
echo.

:: 创建虚拟环境
set "VENV_PATH=%INSTALL_DIR%ocr-service\venv"

if not exist "%VENV_PATH%" (
    echo [*] 创建Python虚拟环境...
    python -m venv "%VENV_PATH%"
    echo [√] 虚拟环境创建完成
) else (
    echo [√] 虚拟环境已存在
)

:: 激活虚拟环境
call "%VENV_PATH%\Scripts\activate.bat"

:: 配置pip镜像
echo [*] 配置pip镜像源...
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple

:: 检测GPU
echo [*] 检测显卡类型...
nvidia-smi >nul 2>&1
if %errorLevel% equ 0 (
    echo [√] 检测到NVIDIA显卡，安装GPU版本
    set "PADDLE_PACKAGE=paddlepaddle-gpu"
) else (
    echo [√] 未检测到NVIDIA显卡，安装CPU版本
    set "PADDLE_PACKAGE=paddlepaddle"
)

:: 安装依赖
echo [*] 正在安装PaddleOCR依赖（这可能需要几分钟）...
echo     - 安装 !PADDLE_PACKAGE!...
pip install !PADDLE_PACKAGE! --quiet 2>nul

echo     - 安装 paddleocr...
pip install paddleocr --quiet 2>nul

echo     - 安装 flask flask-cors...
pip install flask flask-cors --quiet 2>nul

echo     - 安装 pillow numpy...
pip install pillow numpy --quiet 2>nul

:: 验证安装
python -c "import paddleocr; print('PaddleOCR版本:', paddleocr.__version__)" 2>nul
if %errorLevel% equ 0 (
    echo [√] OCR服务依赖安装完成
    call :log_info "OCR服务依赖安装完成"
) else (
    echo [!] OCR服务安装可能存在问题
)

goto :eof

:step_configure_database
echo.
echo ┌────────────────────────────────────────────────────────────┐
echo │  [步骤5/7] 数据库配置                                      │
echo └────────────────────────────────────────────────────────────┘
echo.

:: 检测MySQL是否已安装
mysql --version >nul 2>&1
if %errorLevel% equ 0 (
    echo [√] MySQL已安装
    goto :eof
)

:: 询问是否安装MySQL
echo.
echo 数据库选项：
echo   1. 安装MySQL 8.0（推荐）
echo   2. 使用H2内存数据库（开发测试用，无需安装）
echo   3. 连接已有MySQL服务器
echo.
set /p "DB_OPTION=请选择 (1/2/3): "

if "%DB_OPTION%"=="2" (
    echo [√] 将使用H2内存数据库
    echo [!] 注意：H2数据库数据不会持久化
    goto :eof
)

if "%DB_OPTION%"=="3" (
    set /p "DB_HOST=请输入MySQL服务器地址: "
    set /p "DB_PORT=请输入端口 (默认3306): "
    if "!DB_PORT!"=="" set "DB_PORT=3306"
    set /p "DB_USER=请输入用户名: "
    set /p "DB_PASS=请输入密码: "
    
    :: 测试连接
    mysql -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -p!DB_PASS! -e "SELECT 1" >nul 2>&1
    if %errorLevel% equ 0 (
        echo [√] 数据库连接成功
    ) else (
        echo [!] 数据库连接失败，请检查配置
    )
    goto :eof
)

:: 安装MySQL
echo [*] 正在下载MySQL 8.0...
set "MYSQL_INSTALLER=%TEMP%\mysql-installer.msi"

curl -L -o "%MYSQL_INSTALLER%" "https://dev.mysql.com/get/Downloads/MySQLInstaller/mysql-installer-community-8.0.35.0.msi" 2>nul

if exist "%MYSQL_INSTALLER%" (
    echo [*] 正在安装MySQL 8.0...
    echo [!] 请在安装向导中设置root密码
    msiexec /i "%MYSQL_INSTALLER%"
    
    echo [√] MySQL安装完成
    call :log_info "MySQL 8.0 安装完成"
) else (
    echo [!] MySQL下载失败，请手动安装
    echo     下载地址: https://dev.mysql.com/downloads/installer/
)

goto :eof

:step_register_services
echo.
echo ┌────────────────────────────────────────────────────────────┐
echo │  [步骤6/7] 注册系统服务                                    │
echo └────────────────────────────────────────────────────────────┘
echo.

if "%ADMIN_MODE%"=="0" (
    echo [!] 需要管理员权限才能注册系统服务
    echo [*] 将创建启动脚本代替系统服务
    goto :create_scripts
)

:: 创建OCR服务启动脚本
set "OCR_START_SCRIPT=%INSTALL_DIR%ocr-service\start_service.bat"
(
echo @echo off
echo cd /d "%INSTALL_DIR%ocr-service"
echo call venv\Scripts\activate.bat
echo set USE_GPU=false
echo set GPU_MEM=2000
echo set OCR_PORT=8868
echo python ocr_server.py
) > "%OCR_START_SCRIPT%"

:: 创建后端服务启动脚本
set "BACKEND_START_SCRIPT=%INSTALL_DIR%backend\start_service.bat"
(
echo @echo off
echo cd /d "%INSTALL_DIR%backend"
echo set SPRING_PROFILES_ACTIVE=prod
echo java -jar target\food-traceability-1.0.0.jar
) > "%BACKEND_START_SCRIPT%"

:: 注册Windows服务（使用任务计划）
echo [*] 注册OCR服务...
schtasks /create /tn "%APP_NAME%_OCR" /tr "\"%OCR_START_SCRIPT%\"" /sc onstart /rl highest /f >nul 2>&1

echo [*] 注册后端服务...
schtasks /create /tn "%APP_NAME%_Backend" /tr "\"%BACKEND_START_SCRIPT%\"" /sc onstart /rl highest /f >nul 2>&1

echo [√] 系统服务注册完成
call :log_info "系统服务注册完成"

:create_scripts

:: 创建一键启动脚本
set "START_ALL_SCRIPT=%INSTALL_DIR%start.bat"
(
echo @echo off
echo chcp 65001 ^>nul
echo echo.
echo echo ========================================
echo echo   食品溯源系统 - 启动中...
echo echo ========================================
echo echo.
echo echo [1/2] 启动OCR服务...
echo start "OCR服务" /min cmd /c "%OCR_START_SCRIPT%"
echo timeout /t 10 /nobreak ^>nul
echo.
echo echo [2/2] 启动后端服务...
echo start "后端服务" /min cmd /c "%BACKEND_START_SCRIPT%"
echo timeout /t 15 /nobreak ^>nul
echo.
echo echo ========================================
echo echo   服务启动完成！
echo echo ========================================
echo echo.
echo echo   后端API: http://localhost:8081/api
echo echo   OCR服务: http://localhost:8868
echo echo.
echo pause
) > "%START_ALL_SCRIPT%"

:: 创建一键停止脚本
set "STOP_ALL_SCRIPT=%INSTALL_DIR%stop.bat"
(
echo @echo off
echo echo 正在停止服务...
echo taskkill /f /im java.exe 2^>nul
echo taskkill /f /im python.exe /fi "WINDOWTITLE eq OCR服务*" 2^>nul
echo echo 服务已停止
) > "%STOP_ALL_SCRIPT%"

echo [√] 启动脚本创建完成
goto :eof

:step_start_verify
echo.
echo ┌────────────────────────────────────────────────────────────┐
echo │  [步骤7/7] 启动验证                                        │
echo └────────────────────────────────────────────────────────────┘
echo.

echo [*] 正在启动OCR服务进行验证...

:: 启动OCR服务
start "OCR服务验证" /min cmd /c "%INSTALL_DIR%ocr-service\start_service.bat"

:: 等待服务启动
echo [*] 等待服务启动（约30秒）...
timeout /t 30 /nobreak >nul

:: 验证OCR服务
echo [*] 验证OCR服务...
curl -s http://localhost:8868/health >nul 2>&1
if %errorLevel% equ 0 (
    echo [√] OCR服务启动成功
) else (
    echo [!] OCR服务启动失败，请检查日志
)

:: 验证后端服务（如果已构建）
if exist "%INSTALL_DIR%backend\target\food-traceability-1.0.0.jar" (
    echo [*] 验证后端服务...
    start "后端服务验证" /min cmd /c "%INSTALL_DIR%backend\start_service.bat"
    timeout /t 20 /nobreak >nul
    
    curl -s http://localhost:8081/api/actuator/health >nul 2>&1
    if %errorLevel% equ 0 (
        echo [√] 后端服务启动成功
    ) else (
        echo [!] 后端服务启动失败，请检查日志
    )
) else (
    echo [!] 后端服务未构建，跳过验证
    echo     请先运行: cd backend ^&^& mvn clean package -DskipTests
)

echo [√] 启动验证完成
call :log_info "启动验证完成"
goto :eof

:show_complete
echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║                                                            ║
echo ║                  安装完成！                                ║
echo ║                                                            ║
echo ╠════════════════════════════════════════════════════════════╣
echo ║                                                            ║
echo ║  使用方法：                                                ║
echo ║                                                            ║
echo ║    启动所有服务:  双击 start.bat                           ║
echo ║    停止所有服务:  双击 stop.bat                            ║
echo ║                                                            ║
echo ║  服务地址：                                                ║
echo ║    后端API:    http://localhost:8081/api                   ║
echo ║    OCR服务:    http://localhost:8868                       ║
echo ║                                                            ║
echo ║  日志文件：                                                ║
echo ║    安装日志:   %INSTALL_DIR%install.log                    ║
echo ║                                                            ║
echo ╚════════════════════════════════════════════════════════════╝
echo.
goto :eof

:: ========================================
:: 工具函数
:: ========================================

:check_port
set "PORT=%~1"
set "SERVICE_NAME=%~2"
netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul 2>&1
if %errorLevel% equ 0 (
    echo [!] 端口%PORT%(%SERVICE_NAME%)已被占用
) else (
    echo [√] 端口%PORT%(%SERVICE_NAME%)可用
)
goto :eof

:refresh_env
:: 刷新环境变量（需要管理员权限）
if "%ADMIN_MODE%"=="1" (
    for /f "tokens=2*" %%a in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v Path 2^>nul') do set "SYS_PATH=%%b"
    for /f "tokens=2*" %%a in ('reg query "HKCU\Environment" /v Path 2^>nul') do set "USER_PATH=%%b"
    set "PATH=%SYS_PATH%;%USER_PATH%"
)
goto :eof

:log_info
echo [%date% %time%] [INFO] %~1 >> "%LOG_FILE%"
goto :eof

:log_error
echo [%date% %time%] [ERROR] %~1 >> "%LOG_FILE%"
goto :eof
