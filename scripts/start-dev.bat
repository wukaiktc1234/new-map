@echo off
echo ========================================
echo   My New Project - 开发环境启动器
echo ========================================
echo.

:: 检查环境变量文件
if not exist "config\.env" (
    echo ⚠️  警告: 未找到 config\.env 文件
    echo 正在创建默认配置文件...
    copy "config\.env.example" "config\.env" >nul
    echo 请根据需要编辑 config\.env 文件
    echo.
)

:: 检查Docker环境
echo [1/4] 检查Docker环境...
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)
echo ✅ Docker 环境正常
echo.

:: 启动基础服务
echo [2/4] 启动基础服务 (MySQL, Redis)...
docker-compose up -d mysql redis
if errorlevel 1 (
    echo ❌ 基础服务启动失败
    pause
    exit /b 1
)
echo ✅ 基础服务已启动
echo.

:: 等待服务就绪
echo [3/4] 等待服务就绪...
echo 等待数据库启动...
timeout /t 15 /nobreak >nul

:: 检查服务状态
echo 检查服务状态...
docker-compose ps mysql redis | findstr "Up" >nul
if errorlevel 1 (
    echo ❌ 服务未正常启动，请检查日志
    docker-compose logs mysql redis
    pause
    exit /b 1
)
echo ✅ 服务状态正常
echo.

:: 启动应用服务
echo [4/4] 启动应用服务...
echo.

:: 启动后端服务
echo 🚀 启动后端服务...
cd backend
if exist "pom.xml" (
    start "后端服务 - Spring Boot" cmd /k "title Spring Boot Backend && set JAVA_HOME=P:\my-new-project\JDK21 && set PATH=%JAVA_HOME%\bin;%PATH% && call P:\my-new-project\tools\apache-maven-3.9.11\bin\mvn.cmd spring-boot:run"
    echo ✅ 后端服务已启动 (端口: 8080)
) else (
    echo ⚠️  后端项目未初始化，请先运行 scripts\init-project.bat
)
cd ..

:: 等待后端启动
timeout /t 5 /nobreak >nul

:: 启动前端服务
echo 🚀 启动前端服务...
cd frontend
if exist "package.json" (
    start "前端服务 - %FRONTEND_FRAMEWORK%" cmd /k "title Frontend Dev Server && set PATH=P:\my-new-project\tools\nodejs;%PATH% && npm run dev"
    echo ✅ 前端服务已启动 (端口: 3000)
) else (
    echo ⚠️  前端项目未初始化，请先运行 scripts\init-project.bat
)
cd ..

echo.
echo ========================================
echo   🎉 开发环境启动完成！
echo ========================================
echo.
echo 🌐 应用访问地址:
echo - 前端应用: http://localhost:3000
echo - 后端API:  http://localhost:8080
echo - API文档:  http://localhost:8080/swagger-ui.html
echo.
echo 🗄️  服务访问:
echo - MySQL: localhost:3306
echo - Redis: localhost:6379
echo.
echo 📋 管理命令:
echo - 查看日志: docker-compose logs -f
echo - 停止服务: docker-compose down
echo - 重启服务: docker-compose restart
echo.
echo 💡 提示: 可以最小化此窗口，但不要关闭
echo ========================================
pause