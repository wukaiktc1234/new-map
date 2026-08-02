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
echo 请稍候，启动可能需要1-2分钟...
echo.

call "P:\my-new-project\tools\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=pg -DskipTests

echo.
echo 服务已停止
pause
