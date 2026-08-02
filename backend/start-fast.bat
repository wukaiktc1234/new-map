@echo off
chcp 65001 >nul
echo ==========================================
echo 启动后端服务 (端口 8081)
echo ==========================================

cd /d "%~dp0"

set JAVA_HOME=P:\my-new-project\JDK21
set PATH=%JAVA_HOME%\bin;%PATH%

echo 检查Java环境...
java -version

echo.
echo 正在启动Spring Boot应用...
echo 使用simple配置文件，H2数据库
echo.

set CLASSPATH=target/classes
for %%j in (lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%j

java -cp "target/classes;lib/*" com.foodtraceability.FoodTraceabilityApplication --spring.profiles.active=simple

echo.
echo 服务已停止
pause
