@echo off
echo Starting Spring Boot Application...
cd /d "p:\my-new-project\backend"
set JAVA_HOME=P:\my-new-project\JDK21
set PATH=%JAVA_HOME%\bin;%PATH%
"P:\my-new-project\tools\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=pg
pause