@echo off
setlocal enabledelayedexpansion
REM ============================================================
REM  Food Traceability System - Service Launcher
REM
REM  Encoding: Pure ASCII to avoid OEM/UTF-8 mojibake
REM  Key fix: All "call mvn" use <nul to prevent stdin consumption
REM           which caused set /p to fail and launcher to "flash exit"
REM ============================================================
title Food Traceability - Service Launcher
chcp 437 >nul 2>&1

echo.
echo ============================================
echo    Food Traceability System Launcher
echo ============================================
echo.

REM ===== UTF-8 encoding for Java/Maven output =====
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
set "MAVEN_OPTS=-Dfile.encoding=UTF-8"

REM ===== Project root path (with trailing backslash) =====
set "ROOT=%~dp0"

REM ===== Pre-check: Prerequisites =====
call :check_prerequisites
if errorlevel 1 (
    echo.
    echo [FATAL] Prerequisites check failed. Cannot continue.
    pause
    exit /b 1
)

REM ===== Pre-check: PostgreSQL =====
call :check_postgresql

REM ===== Pre-check: Port usage =====
echo.
echo --- Port Status ---
call :check_port 8081 "Backend   "
call :check_port 3001 "POS       "
call :check_port 3002 "Management"
call :check_port 3003 "Kitchen   "
call :check_port 3004 "Employee  "

echo.
echo   Services:
echo     [1] Management   frontend     Port 3002
echo     [2] Kitchen      kitchen      Port 3003
echo     [3] POS          pos          Port 3001
echo     [4] Employee     employee     Port 3004
echo     [5] All Frontend
echo     [6] Backend      backend      Port 8081
echo     [7] Backend Clean Compile     Port 8081
echo     [8] Start All Backend+Frontend
echo     [9] Stop Backend
echo     [P] Start PostgreSQL
echo.
echo     [0] Exit
echo.

REM Key fix: ensure stdin is clean before set /p
REM Previous "call mvn --version" (without <nul) consumed stdin,
REM causing set /p to immediately return empty and launcher to exit.
set "choice="
set /p "choice=Select service 0-9 or P: "

if /i "!choice!"=="1" goto start_frontend
if /i "!choice!"=="2" goto start_kitchen
if /i "!choice!"=="3" goto start_pos
if /i "!choice!"=="4" goto start_employee
if /i "!choice!"=="5" goto start_all_frontend
if /i "!choice!"=="6" goto start_backend
if /i "!choice!"=="7" goto start_backend_clean
if /i "!choice!"=="8" goto start_all
if /i "!choice!"=="9" goto stop_backend
if /i "!choice!"=="P" goto start_postgresql
if /i "!choice!"=="0" goto end

echo.
echo [ERROR] Invalid selection: "!choice!"
pause
exit /b 1

REM ============================================================
REM Start Management frontend Port 3002
REM ============================================================
:start_frontend
echo.
echo --- Starting Management Port 3002 ---
netstat -ano | findstr /R ":3002[^0-9]" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo   [WARNING] Port 3002 Management is already in use!
    echo   Service may already be running. Skipping startup.
    goto show_result
)
call :ensure_npm_install "%ROOT%frontend"
if errorlevel 1 goto show_result
start "frontend" cmd /k "chcp 65001 >nul && title Management Frontend 3002 && cd /d %ROOT%frontend && npm run dev"
echo Done: http://localhost:3002/
goto show_result

REM ============================================================
REM Start Kitchen frontend Port 3003
REM ============================================================
:start_kitchen
echo.
echo --- Starting Kitchen Port 3003 ---
call :check_port_occupied 3003 "Kitchen"
if errorlevel 1 goto show_result
call :ensure_npm_install "%ROOT%frontend-kitchen"
if errorlevel 1 goto show_result
start "Kitchen" cmd /k "chcp 65001 >nul && title Kitchen Frontend 3003 && cd /d %ROOT%frontend-kitchen && npm run dev"
echo Done: http://localhost:3003/
goto show_result

REM ============================================================
REM Start POS frontend Port 3001
REM ============================================================
:start_pos
echo.
echo --- Starting POS Port 3001 ---
call :check_port_occupied 3001 "POS"
if errorlevel 1 goto show_result
call :ensure_npm_install "%ROOT%frontend-pos"
if errorlevel 1 goto show_result
start "POS" cmd /k "chcp 65001 >nul && title POS Frontend 3001 && cd /d %ROOT%frontend-pos && npm run dev"
echo Done: http://localhost:3001/
goto show_result

REM ============================================================
REM Start Employee frontend Port 3004
REM ============================================================
:start_employee
echo.
echo --- Starting Employee Port 3004 ---
call :check_port_occupied 3004 "Employee"
if errorlevel 1 goto show_result
call :ensure_npm_install "%ROOT%employee-frontend"
if errorlevel 1 goto show_result
start "Employee" cmd /k "chcp 65001 >nul && title Employee Frontend 3004 && cd /d %ROOT%employee-frontend && npm run dev"
echo Done: http://localhost:3004/
goto show_result

REM ============================================================
REM Start all frontend services
REM ============================================================
:start_all_frontend
echo.
echo --- Starting All Frontend Services ---
call :check_port_occupied 3002 "Management"
call :check_port_occupied 3003 "Kitchen"
call :check_port_occupied 3001 "POS"
call :check_port_occupied 3004 "Employee"
call :ensure_npm_install "%ROOT%frontend"
start "Mgmt-Admin" cmd /k "chcp 65001 >nul && title Management Frontend 3002 && cd /d %ROOT%frontend && npm run dev"
timeout /t 2 >nul
call :ensure_npm_install "%ROOT%frontend-kitchen"
start "Kitchen" cmd /k "chcp 65001 >nul && title Kitchen Frontend 3003 && cd /d %ROOT%frontend-kitchen && npm run dev"
timeout /t 2 >nul
call :ensure_npm_install "%ROOT%frontend-pos"
start "POS" cmd /k "chcp 65001 >nul && title POS Frontend 3001 && cd /d %ROOT%frontend-pos && npm run dev"
timeout /t 2 >nul
call :ensure_npm_install "%ROOT%employee-frontend"
start "Employee" cmd /k "chcp 65001 >nul && title Employee Frontend 3004 && cd /d %ROOT%employee-frontend && npm run dev"
echo All frontend services started.
goto show_result

REM ============================================================
REM Start Backend Port 8081 using pg profile
REM ============================================================
:start_backend
echo.
echo --- Starting Backend Port 8081 ---
call :check_port_occupied 8081 "Backend"
if errorlevel 1 goto show_result
call :check_postgresql_required
if errorlevel 1 goto show_result
call :check_mvnw
if errorlevel 1 goto show_result
echo Setting up JDK21 environment...
echo Please wait about 30s for startup...
start "Backend" cmd /k "chcp 65001 >nul && title Backend Spring Boot 8081 && set JAVA_HOME=%ROOT%JDK21 && set PATH=%ROOT%JDK21\bin;%ROOT%maven-mvnd-1.0.3-windows-amd64\mvn\bin;%%PATH%% && set PG_HOST=localhost && set PG_PORT=5432 && set PG_DB_NAME=food_traceability && set PG_USERNAME=postgres && set PG_PASSWORD=123456 && set APP_ENCRYPTION_KEY=dev-only-not-for-production-use-32chars-min && set CONFIG_ENCRYPT_KEY=dev-only-not-for-production-use-32chars-min && set AES_SECRET_KEY=dev-only-not-for-production-use-32chars-min && set JASYPT_ENCRYPTOR_PASSWORD=demoJasyptPassword123456789012345678901234567890 && set JWT_SECRET=dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security && set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 && set MAVEN_OPTS=-Dfile.encoding=UTF-8 && cd /d %ROOT%backend && call "%MVN_CMD%" spring-boot:run -Dspring-boot.run.profiles=pg -DskipTests"
echo Done: http://localhost:8081/api
goto show_result

REM ============================================================
REM Start Backend with Clean Compile Port 8081
REM ============================================================
:start_backend_clean
echo.
echo --- Starting Backend with Clean Compile Port 8081 ---
call :check_port_occupied 8081 "Backend"
if errorlevel 1 goto show_result
call :check_postgresql_required
if errorlevel 1 goto show_result
call :check_mvnw
if errorlevel 1 goto show_result
echo Setting up JDK21 environment...
echo Running mvn clean compile first... this may take 1-2 minutes...
start "Backend" cmd /k "chcp 65001 >nul && title Backend Spring Boot 8081 && set JAVA_HOME=%ROOT%JDK21 && set PATH=%ROOT%JDK21\bin;%ROOT%maven-mvnd-1.0.3-windows-amd64\mvn\bin;%%PATH%% && set PG_HOST=localhost && set PG_PORT=5432 && set PG_DB_NAME=food_traceability && set PG_USERNAME=postgres && set PG_PASSWORD=123456 && set APP_ENCRYPTION_KEY=dev-only-not-for-production-use-32chars-min && set CONFIG_ENCRYPT_KEY=dev-only-not-for-production-use-32chars-min && set AES_SECRET_KEY=dev-only-not-for-production-use-32chars-min && set JASYPT_ENCRYPTOR_PASSWORD=demoJasyptPassword123456789012345678901234567890 && set JWT_SECRET=dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security && set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 && set MAVEN_OPTS=-Dfile.encoding=UTF-8 && cd /d %ROOT%backend && call "%MVN_CMD%" clean spring-boot:run -Dspring-boot.run.profiles=pg -DskipTests"
echo Done: http://localhost:8081/api
echo Note: Clean compile takes longer. Please be patient.
goto show_result

REM ============================================================
REM Start all services Backend + Frontend
REM ============================================================
:start_all
echo.
echo === Starting Full System ===
echo.
call :check_postgresql_required
if errorlevel 1 goto show_result

echo [1/6] Starting Backend...
call :check_port_occupied 8081 "Backend"
call :check_mvnw
if errorlevel 1 goto show_result
start "Backend" cmd /k "chcp 65001 >nul && title Backend Spring Boot 8081 && set JAVA_HOME=%ROOT%JDK21 && set PATH=%ROOT%JDK21\bin;%ROOT%maven-mvnd-1.0.3-windows-amd64\mvn\bin;%%PATH%% && set PG_HOST=localhost && set PG_PORT=5432 && set PG_DB_NAME=food_traceability && set PG_USERNAME=postgres && set PG_PASSWORD=123456 && set APP_ENCRYPTION_KEY=dev-only-not-for-production-use-32chars-min && set CONFIG_ENCRYPT_KEY=dev-only-not-for-production-use-32chars-min && set AES_SECRET_KEY=dev-only-not-for-production-use-32chars-min && set JASYPT_ENCRYPTOR_PASSWORD=demoJasyptPassword123456789012345678901234567890 && set JWT_SECRET=dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security && set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 && set MAVEN_OPTS=-Dfile.encoding=UTF-8 && cd /d %ROOT%backend && call "%MVN_CMD%" spring-boot:run -Dspring-boot.run.profiles=pg -DskipTests"
echo Waiting for backend to start...
timeout /t 30 >nul
call :check_backend_health
echo [2/6] Starting Management...
call :ensure_npm_install "%ROOT%frontend"
start "Mgmt-Admin" cmd /k "chcp 65001 >nul && title Management Frontend 3002 && cd /d %ROOT%frontend && npm run dev"
timeout /t 2 >nul
echo [3/6] Starting Kitchen...
call :ensure_npm_install "%ROOT%frontend-kitchen"
start "Kitchen" cmd /k "chcp 65001 >nul && title Kitchen Frontend 3003 && cd /d %ROOT%frontend-kitchen && npm run dev"
timeout /t 2 >nul
echo [4/6] Starting POS...
call :ensure_npm_install "%ROOT%frontend-pos"
start "POS" cmd /k "chcp 65001 >nul && title POS Frontend 3001 && cd /d %ROOT%frontend-pos && npm run dev"
timeout /t 2 >nul
echo [5/6] Starting Employee...
call :ensure_npm_install "%ROOT%employee-frontend"
start "Employee" cmd /k "chcp 65001 >nul && title Employee Frontend 3004 && cd /d %ROOT%employee-frontend && npm run dev"
echo.
echo [6/6] All services started!
goto show_result

REM ============================================================
REM Stop Backend Port 8081
REM ============================================================
:stop_backend
echo.
echo --- Stopping Backend Port 8081 ---
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /R ":8081[^0-9]" ^| findstr "LISTENING"') do (
    echo Killing PID: %%a
    taskkill /f /pid %%a >nul 2>&1
)
echo Backend stopped.
goto show_result

REM ============================================================
REM Start PostgreSQL (portable install)
REM ============================================================
:start_postgresql
echo.
echo --- Starting PostgreSQL (portable) ---
set "PG_DIR=%ROOT%PostgreSQL\18"
set "PG_BIN=%PG_DIR%\bin"
set "PG_DATA=%PG_DIR%\data"
if not exist "%PG_BIN%\pg_ctl.exe" (
    echo   [ERROR] PostgreSQL not found at %PG_DIR%
    pause
    exit /b 1
)
echo   Starting PostgreSQL from: %PG_BIN%
"%PG_BIN%\pg_ctl" start -D "%PG_DATA%" -l "%ROOT%logs\postgresql.log" -w -t 10
if errorlevel 1 (
    echo   [FAILED] Could not start PostgreSQL!
    echo   Check logs: %ROOT%logs\postgresql.log
    echo   Try manual start from command prompt ^(as admin^):
    echo     set PGDATA=%PG_DATA%
    echo     "%PG_BIN%\pg_ctl" start -D "%PG_DATA%"
) else (
    echo   [OK] PostgreSQL started successfully on port 5432
)
pause
goto show_result

REM ============================================================
REM Show startup result
REM ============================================================
:show_result
echo.
echo -------------------------------------------
echo   Service URLs:
echo -------------------------------------------
if "!choice!"=="1" echo   Management: http://localhost:3002/
if "!choice!"=="2" echo   Kitchen:    http://localhost:3003/
if "!choice!"=="3" echo   POS:        http://localhost:3001/
if "!choice!"=="4" echo   Employee:   http://localhost:3004/
if "!choice!"=="5" echo   Management: http://localhost:3002/  Kitchen: http://localhost:3003/  POS: http://localhost:3001/  Employee: http://localhost:3004/
if "!choice!"=="6" echo   Backend:    http://localhost:8081/api
if "!choice!"=="7" echo   Backend:    http://localhost:8081/api
if "!choice!"=="8" echo   Backend: http://localhost:8081/api  Management: http://localhost:3002/  Kitchen: http://localhost:3003/  POS: http://localhost:3001/  Employee: http://localhost:3004/
if "!choice!"=="9" echo   Backend stopped.
if /i "!choice!"=="P" echo   PostgreSQL started on port 5432
echo -------------------------------------------
echo.
echo Each service runs in its own window.
echo Close a window to stop that service.
echo.
echo Login: admin / Admin@123
echo.
echo Notes:
echo   - PostgreSQL is portable at %ROOT%PostgreSQL\18
echo   - Backend profile: pg ^(PostgreSQL, no Redis^)
echo   - Frontend needs: npm install ^(first time only^)
echo.
pause
exit /b 0

REM ============================================================
REM Check prerequisites (Maven, Node.js, npm)
REM Returns: errorlevel=0 on success, errorlevel=1 on failure
REM ============================================================
:check_prerequisites
echo --- Prerequisites ---

REM Prefer project-local maven-mvnd if available
set "MVN_CMD="
if exist "%ROOT%maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd" (
    set "MVN_CMD=%ROOT%maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd"
    set "PATH=%ROOT%maven-mvnd-1.0.3-windows-amd64\mvn\bin;%PATH%"
    echo   [OK] Using project-local maven-mvnd
    goto :prereq_mvn_ok
)

REM Prefer project-local apache-maven (tools\apache-maven-3.9.11)
if exist "%ROOT%tools\apache-maven-3.9.11\bin\mvn.cmd" (
    set "MVN_CMD=%ROOT%tools\apache-maven-3.9.11\bin\mvn.cmd"
    set "PATH=%ROOT%tools\apache-maven-3.9.11\bin;%PATH%"
    echo   [OK] Using project-local apache-maven
    goto :prereq_mvn_ok
)

REM Test Maven via direct execution (more reliable than 'where')
REM KEY FIX: <nul prevents mvn.cmd from consuming the main stdin
REM Without this, set /p later in the script would fail silently
call mvn --version <nul >nul 2>&1
if not errorlevel 1 (
    set "MVN_CMD=mvn"
    goto :prereq_mvn_ok
)
echo   [WARNING] Maven not in PATH, trying project JDK21...
set "JAVA_HOME=%ROOT%JDK21"
set "PATH=%ROOT%JDK21\bin;%PATH%"
call mvn --version <nul >nul 2>&1
if not errorlevel 1 (
    set "MVN_CMD=mvn"
    goto :prereq_mvn_ok
)
echo   [ERROR] Maven ^(mvn^) is not available!
echo   Install Maven from https://maven.apache.org/download.cgi
echo   Or restore the bundled maven at:
echo     %ROOT%tools\apache-maven-3.9.11
echo   Or project-local maven-mvnd at:
echo     %ROOT%maven-mvnd-1.0.3-windows-amd64
exit /b 1

:prereq_mvn_ok
if not defined MVN_CMD set "MVN_CMD=mvn"
REM KEY FIX: <nul here too, to keep stdin clean for set /p later
for /f "tokens=*" %%a in ('call mvn --version ^<nul 2^>nul ^| findstr "Apache Maven"') do set "MVN_VER=%%a"
echo   [OK] Maven found: %MVN_VER%

REM Prefer project-local Node.js (tools\nodejs)
set "PATH=%ROOT%tools\nodejs;%PATH%"
node --version <nul >nul 2>&1
if not errorlevel 1 goto :prereq_node_ok
echo   [ERROR] Node.js not detected!
echo   Install Node.js from https://nodejs.org/ ^(v18+^)
echo   Or restore the bundled Node.js at %ROOT%tools\nodejs
exit /b 1

:prereq_node_ok
for /f "tokens=*" %%a in ('node --version') do set "NODE_VER=%%a"
echo   [OK] Node.js found: %NODE_VER%

REM KEY FIX: <nul prevents npm.cmd from consuming stdin
call npm --version <nul >nul 2>&1
if not errorlevel 1 goto :prereq_npm_ok
echo   [ERROR] npm not detected!
echo   npm should be bundled with Node.js
exit /b 1

:prereq_npm_ok
echo   [OK] npm found
exit /b 0

REM ============================================================
REM Check PostgreSQL (portable install at project\PostgreSQL\18)
REM Display-only check, does not block startup
REM ============================================================
:check_postgresql
echo --- PostgreSQL Status ---
set "PG_DIR=%ROOT%PostgreSQL\18"
set "PG_BIN=%PG_DIR%\bin"
set "PG_DATA=%PG_DIR%\data"

if not exist "%PG_BIN%\pg_ctl.exe" (
    echo   [WARNING] Portable PostgreSQL not found at %PG_DIR%
    echo   Backend will fail to start. Check PostgreSQL installation.
    exit /b 0
)
"%PG_BIN%\pg_isready" -h localhost -p 5432 >nul 2>&1
if errorlevel 1 (
    echo   [WARNING] PostgreSQL is NOT running!
    echo   Start it manually: "%PG_BIN%\pg_ctl" start -D "%PG_DATA%"
    echo   Or use option [P] to start PostgreSQL from this launcher.
) else (
    echo   [OK] PostgreSQL is running on port 5432
)
exit /b 0

REM ============================================================
REM Check PostgreSQL (required before backend start)
REM Returns: errorlevel=0 on success, errorlevel=1 on failure
REM ============================================================
:check_postgresql_required
if not exist "%PG_BIN%\pg_ctl.exe" (
    echo   [ERROR] Portable PostgreSQL not found at %PG_DIR%
    echo   Please check PostgreSQL installation.
    exit /b 1
)
"%PG_BIN%\pg_isready" -h localhost -p 5432 >nul 2>&1
if not errorlevel 1 goto :pgreq_running
echo   [WARNING] PostgreSQL is not running.
echo   Attempting to start portable PostgreSQL...
set "PGDATA=%PG_DATA%"
"%PG_BIN%\pg_ctl" start -D "%PG_DATA%" -l "%ROOT%logs\postgresql.log" -w -t 10
if not errorlevel 1 goto :pgreq_started
echo   [FAILED] Could not start PostgreSQL!
echo   Check logs: %ROOT%logs\postgresql.log
echo   Try manual start: "%PG_BIN%\pg_ctl" start -D "%PG_DATA%"
exit /b 1

:pgreq_started
echo   [OK] PostgreSQL started successfully.
timeout /t 3 >nul
exit /b 0

:pgreq_running
echo   [OK] PostgreSQL is running.
exit /b 0

REM ============================================================
REM Check port status (display only)
REM Args: %1=port %2=name
REM ============================================================
:check_port
set "port=%~1"
set "name=%~2"
netstat -ano | findstr /R ":%port%[^0-9]" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo   %name% port %port%: FREE
) else (
    echo   %name% port %port%: IN USE
)
exit /b 0

REM ============================================================
REM Check if port is occupied before start
REM Args: %1=port %2=name
REM Returns: errorlevel=0 if port is FREE, errorlevel=1 if IN USE
REM ============================================================
:check_port_occupied
set "port=%~1"
set "name=%~2"
netstat -ano | findstr /R ":%port%[^0-9]" | findstr "LISTENING" >nul
if errorlevel 1 (
    REM Port is free - return success
    exit /b 0
) else (
    echo   [WARNING] Port %port% %name% is already in use!
    echo   Service may already be running. Skipping startup.
    exit /b 1
)
goto :eof

REM ============================================================
REM Check backend health endpoint
REM ============================================================
:check_backend_health
echo   Checking backend health...
set "health_ok=0"
for /l %%i in (1,1,10) do (
    if "!health_ok!"=="0" (
        timeout /t 3 >nul
        powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:8081/api/actuator/health' -UseBasicParsing -TimeoutSec 3; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
        if not errorlevel 1 (
            set "health_ok=1"
            echo   [OK] Backend is healthy!
        ) else (
            echo   [WAIT] Backend not ready... attempt %%i/10
        )
    )
)
if "!health_ok!"=="0" (
    echo   [WARNING] Backend health check failed. It may still be starting.
    echo   Please check the Backend window for errors.
)
exit /b 0

REM ============================================================
REM Check if npm install is needed, run if node_modules missing
REM Args: %1=frontend directory path
REM Returns: errorlevel=0 on success, errorlevel=1 on failure
REM ============================================================
:ensure_npm_install
set "FE_DIR=%~1"
if exist "%FE_DIR%\node_modules" exit /b 0
echo   [INFO] node_modules not found in %FE_DIR%
echo   Running npm install...
pushd "%FE_DIR%"
call npm install <nul
set "NPM_RC=!errorlevel!"
popd
if "!NPM_RC!"=="0" (
    echo   [OK] npm install completed.
    exit /b 0
) else (
    echo   [WARNING] npm install failed in %FE_DIR%
    echo   Check network connectivity and try again.
    exit /b 1
)
goto :eof

REM ============================================================
REM Check Maven command availability
REM Sets %MVN_CMD% to the available Maven command (full path preferred)
REM Also adds maven-mvnd to PATH so child cmd windows can find it
REM Returns: errorlevel=0 on success, errorlevel=1 on failure
REM ============================================================
:check_mvnw
if defined MVN_CMD exit /b 0

REM Prefer project-local maven-mvnd (more reliable than system PATH)
if exist "%ROOT%maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd" (
    set "MVN_CMD=%ROOT%maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd"
    set "PATH=%ROOT%maven-mvnd-1.0.3-windows-amd64\mvn\bin;%PATH%"
    echo   [OK] Using project-local maven-mvnd
    exit /b 0
)

REM Prefer project-local apache-maven (tools\apache-maven-3.9.11)
if exist "%ROOT%tools\apache-maven-3.9.11\bin\mvn.cmd" (
    set "MVN_CMD=%ROOT%tools\apache-maven-3.9.11\bin\mvn.cmd"
    set "PATH=%ROOT%tools\apache-maven-3.9.11\bin;%PATH%"
    echo   [OK] Using project-local apache-maven
    exit /b 0
)

echo   [ERROR] Maven not found in project!
echo   Restore the bundled maven at:
echo     %ROOT%tools\apache-maven-3.9.11
echo   Or project-local maven-mvnd at:
echo     %ROOT%maven-mvnd-1.0.3-windows-amd64
exit /b 1

:end
echo.
echo Exited.
exit /b 0
