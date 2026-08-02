@echo off
chcp 65001 >nul
echo ==========================================
echo 生产环境构建脚本 - Food Traceability System
echo ==========================================

echo 当前目录: %~dp0
cd /d "%~dp0\.."
echo 切换到项目根目录: %cd%

echo.
echo 检查Java环境...
java -version
if %errorlevel% neq 0 (
    echo Java环境未找到，请确保已安装Java 17或更高版本
    pause
    exit /b 1
)

echo.
echo 检查Maven环境...
mvn -version
if %errorlevel% neq 0 (
    echo Maven环境未找到，请确保已安装Maven 3.6.0或更高版本
    pause
    exit /b 1
)

echo.
echo 开始清理旧构建...
call mvn clean -DskipTests
if %errorlevel% neq 0 (
    echo 清理失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo 开始生产环境构建...
echo 正在编译和打包，这可能需要几分钟时间...
call mvn package -DskipTests -P production
if %errorlevel% neq 0 (
    echo 构建失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo ==========================================
echo 构建成功！
echo 生产环境JAR包已生成到: %cd%\target

dir /b target\*.jar | findstr /v "sources" | findstr /v "javadoc"

echo.
echo 构建脚本执行完成
echo ==========================================
pause
