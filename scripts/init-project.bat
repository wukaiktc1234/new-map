@echo off
echo ========================================
echo   My New Project - 初始化工具
echo ========================================
echo.

:: 检查前置条件
echo [1/6] 检查环境和前置条件...

node --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未检测到 Node.js，请先安装 Node.js 18+ 
    echo    下载地址: https://nodejs.org/
    pause
    exit /b 1
)

java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未检测到 Java，请先安装 Java 17+ 
    echo    下载地址: https://adoptium.net/
    pause
    exit /b 1
)

mvn --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未检测到 Maven，请先安装 Maven 3.9+ 
    echo    下载地址: https://maven.apache.org/
    pause
    exit /b 1
)

docker --version >nul 2>&1
if errorlevel 1 (
    echo ⚠️  警告: 未检测到 Docker，某些功能可能无法使用
    echo    下载地址: https://www.docker.com/
    echo.
)

echo ✅ 环境检查通过!
echo.

:: 选择前端框架
echo [2/6] 选择前端技术栈...
echo 请选择要使用的框架:
echo 1. Vue.js 3 + TypeScript + Vite
echo 2. React 18 + TypeScript + Vite
choice /C 12 /N /M "请输入选项 (1-2): "

if errorlevel 2 set "FRONTEND_FRAMEWORK=react"
if errorlevel 1 set "FRONTEND_FRAMEWORK=vue"

echo 已选择: %FRONTEND_FRAMEWORK%
echo.

:: 初始化后端
echo [3/6] 初始化后端项目...
cd backend

echo 正在创建 Spring Boot 项目...
call mvn archetype:generate -DgroupId=com.mynewproject ^
  -DartifactId=my-new-project-backend ^
  -DarchetypeArtifactId=maven-archetype-spring-boot ^
  -DinteractiveMode=false ^
  -DarchetypeVersion=3.2.0

if errorlevel 1 (
    echo ⚠️  警告: Spring Boot 项目创建失败，请手动初始化
    echo 建议: 使用 Spring Initializr (https://start.spring.io/) 创建项目
)
cd ..

:: 初始化前端
echo [4/6] 初始化前端项目...
cd frontend

if "%FRONTEND_FRAMEWORK%"=="vue" (
    echo 正在创建 Vue.js 项目...
    call npm create vue@latest . -- --typescript --router --pinia --eslint --prettier
) else (
    echo 正在创建 React 项目...
    call npm create vite@latest . --template react-ts
)

if errorlevel 1 (
    echo ⚠️  警告: 前端项目创建失败，请手动初始化
    echo 建议: 检查网络连接后重试
)
cd ..

:: 设置配置文件
echo [5/6] 设置配置文件...
if exist "config\.env.example" (
    copy "config\.env.example" "config\.env" >nul
    echo ✅ 配置文件已创建: config\.env
) else (
    echo ⚠️  警告: 配置文件模板不存在
)

:: 安装根依赖
echo [6/6] 安装项目依赖...
echo 正在安装 concurrently...
call npm install concurrently --save-dev

echo.
echo ========================================
echo   ✅ 项目初始化完成！
echo ========================================
echo.
echo 📋 后续步骤:
echo 1. 编辑 config\.env 文件，配置您的数据库和其他设置
echo 2. 根据需要修改后端配置 (backend\src\main\resources\application.yml)
echo 3. 根据需要修改前端配置 (frontend\vite.config.ts)
echo 4. 运行 scripts\start-dev.bat 启动开发环境
echo.
echo 🚀 开发命令:
echo - npm run dev          # 启动前后端开发服务器
echo - npm run build        # 构建生产版本
echo - npm run test         # 运行测试
echo.
echo 📚 文档:
echo - 查看 docs\ 目录获取更多文档
echo - 访问 http://localhost:3000 查看前端
echo - 访问 http://localhost:8080 查看后端API
echo.
echo Happy coding! 🎉
echo ========================================
pause