@echo off
setlocal enabledelayedexpansion
REM ============================================================
REM  Backend Service Robust Launcher
REM  Food Traceability System
REM
REM  Usage:
REM    start-backend-robust.bat            Normal start
REM    start-backend-robust.bat clean      Clean compile + start
REM    start-backend-robust.bat norestart  No auto-restart on crash
REM
REM  Encoding: Pure ASCII + CRLF to avoid OEM/UTF-8 mojibake
REM ============================================================
title Backend Service - Robust Launcher
chcp 437 >nul 2>&1

echo ==========================================
echo   Backend Service Robust Launcher
echo   Food Traceability System
echo ==========================================
echo.

REM ===== Parse arguments =====
set "DO_CLEAN=0"
set "ALLOW_RESTART=1"
if /i "%~1"=="clean" set "DO_CLEAN=1"
if /i "%~1"=="norestart" set "ALLOW_RESTART=0"
if /i "%~2"=="clean" set "DO_CLEAN=1"
if /i "%~2"=="norestart" set "ALLOW_RESTART=0"

REM ===== Path config (script is in backend/, parent is project root) =====
set "BACKEND_DIR=%~dp0"
if "%BACKEND_DIR:~-1%"=="\" set "BACKEND_DIR=%BACKEND_DIR:~0,-1%"
set "PROJECT_ROOT=%BACKEND_DIR%\.."
pushd "%PROJECT_ROOT%" >nul 2>&1
set "PROJECT_ROOT=%CD%"
popd >nul 2>&1

set "JAVA_HOME=%PROJECT_ROOT%\JDK21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM ===== Maven path (prefer project-local maven-mvnd, then tools\apache-maven) =====
set "MVN_CMD=%PROJECT_ROOT%\maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd"
if not exist "%MVN_CMD%" set "MVN_CMD=%PROJECT_ROOT%\tools\apache-maven-3.9.11\bin\mvn.cmd"
if not exist "%MVN_CMD%" goto error_exit
set "PATH=%PROJECT_ROOT%\tools\apache-maven-3.9.11\bin;%PROJECT_ROOT%\maven-mvnd-1.0.3-windows-amd64\mvn\bin;%PATH%"

REM ===== UTF-8 encoding for Java/Maven output =====
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
set "MAVEN_OPTS=-Dfile.encoding=UTF-8"

REM ===== Critical environment variables (must be set before mvn) =====
set "PG_HOST=localhost"
set "PG_PORT=5432"
set "PG_DB_NAME=food_traceability"
set "PG_USERNAME=postgres"
set "PG_PASSWORD=123456"
set "APP_ENCRYPTION_KEY=dev-only-not-for-production-use-32chars-min"
set "CONFIG_ENCRYPT_KEY=dev-only-not-for-production-use-32chars-min"
set "AES_SECRET_KEY=dev-only-not-for-production-use-32chars-min"
set "JASYPT_ENCRYPTOR_PASSWORD=demoJasyptPassword123456789012345678901234567890"
set "JWT_SECRET=dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"

REM ===== Validate dev-only encryption key length =====
REM These keys are intentionally weak defaults for local dev only.
REM If they have been overridden externally, warn when too short.
call :check_key_length "APP_ENCRYPTION_KEY" 32
call :check_key_length "CONFIG_ENCRYPT_KEY" 32
call :check_key_length "AES_SECRET_KEY" 32
call :check_key_length "JASYPT_ENCRYPTOR_PASSWORD" 16
call :check_key_length "JWT_SECRET" 64

REM ===== Log config =====
set "LOG_DIR=%BACKEND_DIR%\logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
set "STARTUP_LOG=%LOG_DIR%\startup-robust.log"
set "APP_LOG=%LOG_DIR%\food-traceability.log"
set "ERROR_LOG=%LOG_DIR%\food-traceability-error.log"

REM ===== Launch params =====
set "SERVER_PORT=8081"
set "PG_PORT_CHECK=5432"
set "MAX_RESTART=3"
set "RESTART_COUNT=0"

REM ===== Portable PostgreSQL path =====
set "PG_DIR=%PROJECT_ROOT%\PostgreSQL\18"
set "PG_BIN=%PG_DIR%\bin"
set "PG_DATA=%PG_DIR%\data"

echo   Project Root:  %PROJECT_ROOT%
echo   Backend Dir:   %BACKEND_DIR%
echo   JAVA_HOME:     %JAVA_HOME%
echo   MVN_CMD:       %MVN_CMD%
echo   Server Port:   %SERVER_PORT%
echo   Portable PG:   %PG_DIR%
echo   Startup Log:   %STARTUP_LOG%
echo   Max Restart:   %MAX_RESTART%
echo   Do Clean:      %DO_CLEAN%
echo   Allow Restart: %ALLOW_RESTART%
echo.

REM ===== Step 1: Check Java =====
echo [1/5] Checking Java environment...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo   [ERROR] JDK21 not found: %JAVA_HOME%\bin\java.exe
    echo   Please ensure JDK21 directory exists in project root
    goto error_exit
)
echo   [OK] JDK21 ready
"%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr "version"
echo.

REM ===== Step 2: Check Maven =====
echo [2/5] Checking Maven...
if not exist "%MVN_CMD%" (
    echo   [ERROR] Maven not found: %MVN_CMD%
    echo   Please restore the project-local maven at:
    echo     %PROJECT_ROOT%\tools\apache-maven-3.9.11
    goto error_exit
)
echo   [OK] Maven found: %MVN_CMD%
REM Use <nul to prevent mvn.cmd from consuming parent stdin
call "%MVN_CMD%" --version <nul 2>&1 | findstr "Apache Maven"
echo.

REM ===== Step 3: Check PostgreSQL =====
echo [3/5] Checking PostgreSQL...

set "PG_STARTED_BY_SCRIPT=0"

if exist "%PG_BIN%\pg_ctl.exe" (
    echo   Portable PostgreSQL detected: %PG_DIR%
    "%PG_BIN%\pg_isready" -h localhost -p %PG_PORT_CHECK% >nul 2>&1
    if errorlevel 1 (
        echo   [WARNING] PostgreSQL not running, starting portable PostgreSQL...
        if not exist "%PG_DATA%" (
            echo   [ERROR] PG data directory not found: %PG_DATA%
            echo   Please initialize PostgreSQL first
            goto error_exit
        )
        if not exist "%PROJECT_ROOT%\logs" mkdir "%PROJECT_ROOT%\logs"
        "%PG_BIN%\pg_ctl" start -D "%PG_DATA%" -l "%PROJECT_ROOT%\logs\postgresql.log" -w -t 10
        if errorlevel 1 (
            echo   [ERROR] Could not start portable PostgreSQL
            echo   Check log: %PROJECT_ROOT%\logs\postgresql.log
            goto error_exit
        )
        set "PG_STARTED_BY_SCRIPT=1"
        echo   [OK] PostgreSQL started, waiting 3 seconds...
        timeout /t 3 >nul
    ) else (
        echo   [OK] PostgreSQL is running on port %PG_PORT_CHECK%
    )
) else (
    echo   [ERROR] Portable PostgreSQL not found: %PG_DIR%
    echo   Please restore the portable PostgreSQL at: %PROJECT_ROOT%\PostgreSQL\18
    echo   [NOTE] project-local toolchain only, Windows PG service is NOT used
    goto error_exit
)

netstat -ano | findstr ":%PG_PORT_CHECK% " | findstr "LISTENING" >nul
if errorlevel 1 (
    echo   [ERROR] PostgreSQL port %PG_PORT_CHECK% not listening
    echo   Please check PostgreSQL configuration
    goto error_exit
)
echo   [OK] PostgreSQL port %PG_PORT_CHECK% is listening
echo.

REM ===== Step 4: Check port conflict =====
echo [4/5] Checking port %SERVER_PORT% conflict...
netstat -ano | findstr ":%SERVER_PORT% " | findstr "LISTENING" >nul
if errorlevel 1 (
    echo   [OK] Port %SERVER_PORT% is available
) else (
    echo   [WARNING] Port %SERVER_PORT% is already in use!
    echo   Finding and killing the process...
    set "CONFLICT_PID="
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%SERVER_PORT% " ^| findstr "LISTENING"') do (
        echo   Process PID: %%a
        set "CONFLICT_PID=%%a"
    )
    if "!CONFLICT_PID!"=="" (
        echo   [ERROR] Could not determine the process using port %SERVER_PORT%
        goto error_exit
    )
    taskkill /f /pid !CONFLICT_PID! >nul 2>&1
    if errorlevel 1 (
        echo   [ERROR] Could not kill process !CONFLICT_PID!
        echo   Please kill manually: taskkill /f /pid !CONFLICT_PID!
        goto error_exit
    )
    echo   [OK] Process killed
    timeout /t 2 >nul
)
echo.

REM ===== Step 5: Build target =====
echo [5/5] Preparing build target...
if "%DO_CLEAN%"=="1" (
    set "MVN_TARGET=clean spring-boot:run"
    echo   Will run: mvn clean spring-boot:run
    echo   Note: Clean compile may take 1-2 minutes
) else (
    set "MVN_TARGET=spring-boot:run"
    echo   Will run: mvn spring-boot:run
)
echo.

REM ===== Start backend =====
echo ==========================================
echo   Backend starting...
echo   URL:    http://localhost:%SERVER_PORT%/api
echo   Health: http://localhost:%SERVER_PORT%/api/actuator/health
echo   Login:  admin / Admin@123
echo   Profile: pg
echo ==========================================
echo.

:restart_loop
set /a RESTART_COUNT+=1
echo [Start #%RESTART_COUNT%/%MAX_RESTART%] Time: %TIME%
echo ===== Start #%RESTART_COUNT% Time: %TIME% ===== >> "%STARTUP_LOG%"

cd /d "%BACKEND_DIR%"
echo   Console output: real-time display
echo   App log:   %APP_LOG%
echo   Error log: %ERROR_LOG%
echo.

REM Direct call to mvn, real-time output. <nul prevents stdin consumption.
REM Quote the profile property to avoid parsing errors in some environments.
call "%MVN_CMD%" %MVN_TARGET% "-Dspring-boot.run.profiles=pg" -DskipTests <nul

set "EXIT_CODE=%errorlevel%"
echo.
echo [INFO] Backend exited with code: %EXIT_CODE%
echo [INFO] Exit time: %TIME%
echo ===== Exit #%RESTART_COUNT% Code: %EXIT_CODE% Time: %TIME% ===== >> "%STARTUP_LOG%"

if %EXIT_CODE% equ 0 (
    echo [INFO] Service exited normally. No restart needed.
    goto normal_exit
)

if "%ALLOW_RESTART%"=="0" (
    echo [INFO] Auto-restart disabled. Exiting.
    goto error_exit
)

if %RESTART_COUNT% geq %MAX_RESTART% (
    echo [ERROR] Max restart count %MAX_RESTART% reached. Stopping.
    echo   Check log: %STARTUP_LOG%
    echo   Check error log: %ERROR_LOG%
    goto error_exit
)

echo [WARNING] Service crashed. Auto restart in 10 seconds...
echo   Restart: %RESTART_COUNT% / %MAX_RESTART%
echo   Press Ctrl+C to cancel restart...
timeout /t 10 >nul

netstat -ano | findstr ":%SERVER_PORT% " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [WARNING] Port %SERVER_PORT% still in use, releasing...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%SERVER_PORT% " ^| findstr "LISTENING"') do (
        taskkill /f /pid %%a >nul 2>&1
    )
    timeout /t 3 >nul
)

REM After first crash, don't clean again (faster restart)
set "MVN_TARGET=spring-boot:run"
goto restart_loop

REM ===== Subroutine: check encryption key length =====
:check_key_length
set "KEY_NAME=%~1"
set "MIN_LEN=%~2"
call set "KEY_VALUE=%%%KEY_NAME%%%"
set "KEY_LEN=0"
set "KEY_INDEX=0"
if not defined KEY_VALUE goto :check_key_length_done
:check_key_length_loop
set "CHAR=!KEY_VALUE:~%KEY_INDEX%,1!"
if "!CHAR!"=="" goto :check_key_length_done
set /a KEY_LEN+=1
set /a KEY_INDEX+=1
goto :check_key_length_loop
:check_key_length_done
if %KEY_LEN% lss %MIN_LEN% (
    echo   [WARNING] %KEY_NAME% length is %KEY_LEN%, minimum recommended is %MIN_LEN% for production.
    echo             Current value is a development default and is NOT safe for production.
)
exit /b 0

:normal_exit
echo.
echo ==========================================
echo   Backend service stopped normally
echo ==========================================
echo   Log: %STARTUP_LOG%
echo.
pause
exit /b 0

:error_exit
echo.
echo ==========================================
echo   Startup FAILED!
echo ==========================================
echo   Troubleshooting:
echo   1. Check PostgreSQL: "%PG_BIN%\pg_isready" -h localhost -p %PG_PORT_CHECK%
echo   2. Check port 8081: netstat -ano ^| findstr :8081
echo   3. View startup log: type "%STARTUP_LOG%"
echo   4. View app log:     type "%APP_LOG%"
echo   5. View error log:   type "%ERROR_LOG%"
echo   6. Try Clean Compile: start-backend-robust.bat clean
echo   7. Check DB: "%PG_BIN%\psql" -U postgres -h localhost -p 5432 -d food_traceability
echo.
echo   Environment variables:
echo     PG_PASSWORD=%PG_PASSWORD%
echo     PG_DB_NAME=%PG_DB_NAME%
echo     JAVA_HOME=%JAVA_HOME%
echo     MVN_CMD=%MVN_CMD%
echo     APP_ENCRYPTION_KEY length check: see warnings above
echo.
echo   Log: %STARTUP_LOG%
echo.
pause
exit /b 1



