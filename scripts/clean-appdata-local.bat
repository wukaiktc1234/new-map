@echo off
chcp 65001 >nul
echo ==========================================
echo AppData\Local 文件夹清理脚本
echo ==========================================
echo.

setlocal enabledelayedexpansion

echo 正在分析 C:\Users\Liberty\AppData\Local 文件夹...
echo.

set TOTAL_SIZE=0

echo [1] 分析常见缓存文件夹...
echo.

if exist "C:\Users\Liberty\AppData\Local\Temp" (
    for /f "tokens=3" %%a in ('dir "C:\Users\Liberty\AppData\Local\Temp" /s /-c 2^>nul ^| find "个字节"') do set TEMP_SIZE=%%a
    set /a TOTAL_SIZE=!TOTAL_SIZE!+!TEMP_SIZE!
    echo    Temp: !TEMP_SIZE! 字节
) else (
    echo    Temp: 不存在
)

if exist "C:\Users\Liberty\AppData\Local\cache" (
    for /f "tokens=3" %%a in ('dir "C:\Users\Liberty\AppData\Local\cache" /s /-c 2^>nul ^| find "个字节"') do set CACHE_SIZE=%%a
    set /a TOTAL_SIZE=!TOTAL_SIZE!+!CACHE_SIZE!
    echo    cache: !CACHE_SIZE! 字节
) else (
    echo    cache: 不存在
)

if exist "C:\Users\Liberty\AppData\Local\npm-cache" (
    for /f "tokens=3" %%a in ('dir "C:\Users\Liberty\AppData\Local\npm-cache" /s /-c 2^>nul ^| find "个字节"') do set NPM_SIZE=%%a
    set /a TOTAL_SIZE=!TOTAL_SIZE!+!NPM_SIZE!
    echo    npm-cache: !NPM_SIZE! 字节
) else (
    echo    npm-cache: 不存在
)

if exist "C:\Users\Liberty\AppData\Local\pip" (
    for /f "tokens=3" %%a in ('dir "C:\Users\Liberty\AppData\Local\pip" /s /-c 2^>nul ^| find "个字节"') do set PIP_SIZE=%%a
    set /a TOTAL_SIZE=!TOTAL_SIZE!+!PIP_SIZE!
    echo    pip: !PIP_SIZE! 字节
) else (
    echo    pip: 不存在
)

if exist "C:\Users\Liberty\AppData\Local\Package Cache" (
    for /f "tokens=3" %%a in ('dir "C:\Users\Liberty\AppData\Local\Package Cache" /s /-c 2^>nul ^| find "个字节"') do set PACKAGE_SIZE=%%a
    set /a TOTAL_SIZE=!TOTAL_SIZE!+!PACKAGE_SIZE!
    echo    Package Cache: !PACKAGE_SIZE! 字节
) else (
    echo    Package Cache: 不存在
)

if exist "C:\Users\Liberty\AppData\Local\Microsoft\Windows\INetCache" (
    for /f "tokens=3" %%a in ('dir "C:\Users\Liberty\AppData\Local\Microsoft\Windows\INetCache" /s /-c 2^>nul ^| find "个字节"') do set INET_SIZE=%%a
    set /a TOTAL_SIZE=!TOTAL_SIZE!+!INET_SIZE!
    echo    INetCache: !INET_SIZE! 字节
) else (
    echo    INetCache: 不存在
)

if exist "C:\Users\Liberty\AppData\Local\Google\Chrome\User Data\Default\Cache" (
    for /f "tokens=3" %%a in ('dir "C:\Users\Liberty\AppData\Local\Google\Chrome\User Data\Default\Cache" /s /-c 2^>nul ^| find "个字节"') do set CHROME_SIZE=%%a
    set /a TOTAL_SIZE=!TOTAL_SIZE!+!CHROME_SIZE!
    echo    Chrome Cache: !CHROME_SIZE! 字节
) else (
    echo    Chrome Cache: 不存在
)

set /a TOTAL_MB=!TOTAL_SIZE!/1048576
set /a TOTAL_GB=!TOTAL_MB!/1024

echo.
echo ==========================================
echo 分析结果
echo ==========================================
echo.
echo 常见缓存文件夹总大小: !TOTAL_SIZE! 字节
echo                    约等于: !TOTAL_MB! MB
echo                    约等于: !TOTAL_GB! GB
echo.

echo ==========================================
echo 文件夹说明
echo ==========================================
echo.
echo [1] Temp - 临时文件
echo     作用: 存储临时文件
echo     建议: 可以安全删除
echo.
echo [2] cache - 应用缓存
echo     作用: 各种应用的缓存文件
echo     建议: 可以安全删除
echo.
echo [3] npm-cache - NPM包缓存
echo     作用: Node.js包管理器缓存
echo     建议: 可以安全删除（需要时自动重新下载）
echo.
echo [4] pip - Python包缓存
echo     作用: Python包管理器缓存
echo     建议: 可以安全删除（需要时自动重新下载）
echo.
echo [5] Package Cache - Windows包缓存
echo     作用: Windows安装包缓存
echo     建议: 可以安全删除
echo.
echo [6] INetCache - IE浏览器缓存
echo     作用: Internet Explorer缓存
echo     建议: 可以安全删除
echo.
echo [7] Chrome Cache - Chrome浏览器缓存
echo     作用: Chrome浏览器缓存
echo     建议: 可以安全删除（会自动重新生成）
echo.

echo ==========================================
echo 清理建议
echo ==========================================
echo.
echo 推荐清理顺序:
echo 1. Temp (临时文件） - 立即可删除
echo 2. cache (应用缓存） - 立即可删除
echo 3. npm-cache (NPM缓存） - 立即可删除
echo 4. pip (Python缓存） - 立即可删除
echo 5. Package Cache (Windows缓存） - 立即可删除
echo 6. INetCache (IE缓存） - 立即可删除
echo 7. Chrome Cache (Chrome缓存） - 立即可删除
echo.

echo ==========================================
echo 游戏相关文件夹（谨慎删除）
echo ==========================================
echo.
echo 以下文件夹是游戏相关，请谨慎删除:
echo - Steam
echo - Epic Games
echo - Battle.net
echo - Blizzard Entertainment
echo - Origin
echo - GOG.com
echo - Rockstar Games
echo - Ubisoft (可能在不同位置）
echo.
echo 建议: 只删除不玩的游戏文件夹
echo.

echo ==========================================
echo 开发工具文件夹（谨慎删除）
echo ==========================================
echo.
echo 以下文件夹是开发工具相关:
echo - Docker
echo - npm-cache
echo - pip
echo - flutter_webview_windows
echo - ms-playwright
echo - UniCompactView
echo.
echo 建议: 保留正在使用的工具
echo.

echo ==========================================
echo 是否开始清理？
echo ==========================================
echo.

set /p CLEAN_TEMP="是否清理 Temp 文件夹？(y/n): "
if /i "%CLEAN_TEMP%" == "y" (
    echo 正在清理 Temp 文件夹...
    rd /s /q "C:\Users\Liberty\AppData\Local\Temp" 2>nul
    if exist "C:\Users\Liberty\AppData\Local\Temp" (
        echo [失败] Temp 文件夹清理失败
    ) else (
        echo [成功] Temp 文件夹已清理
    )
)

set /p CLEAN_CACHE="是否清理 cache 文件夹？(y/n): "
if /i "%CLEAN_CACHE%" == "y" (
    echo 正在清理 cache 文件夹...
    rd /s /q "C:\Users\Liberty\AppData\Local\cache" 2>nul
    if exist "C:\Users\Liberty\AppData\Local\cache" (
        echo [失败] cache 文件夹清理失败
    ) else (
        echo [成功] cache 文件夹已清理
    )
)

set /p CLEAN_NPM="是否清理 npm-cache 文件夹？(y/n): "
if /i "%CLEAN_NPM%" == "y" (
    echo 正在清理 npm-cache 文件夹...
    rd /s /q "C:\Users\Liberty\AppData\Local\npm-cache" 2>nul
    if exist "C:\Users\Liberty\AppData\Local\npm-cache" (
        echo [失败] npm-cache 文件夹清理失败
    ) else (
        echo [成功] npm-cache 文件夹已清理
    )
)

set /p CLEAN_PIP="是否清理 pip 文件夹？(y/n): "
if /i "%CLEAN_PIP%" == "y" (
    echo 正在清理 pip 文件夹...
    rd /s /q "C:\Users\Liberty\AppData\Local\pip" 2>nul
    if exist "C:\Users\Liberty\AppData\Local\pip" (
        echo [失败] pip 文件夹清理失败
    ) else (
        echo [成功] pip 文件夹已清理
    )
)

set /p CLEAN_PACKAGE="是否清理 Package Cache 文件夹？(y/n): "
if /i "%CLEAN_PACKAGE%" == "y" (
    echo 正在清理 Package Cache 文件夹...
    rd /s /q "C:\Users\Liberty\AppData\Local\Package Cache" 2>nul
    if exist "C:\Users\Liberty\AppData\Local\Package Cache" (
        echo [失败] Package Cache 文件夹清理失败
    ) else (
        echo [成功] Package Cache 文件夹已清理
    )
)

set /p CLEAN_INET="是否清理 INetCache 文件夹？(y/n): "
if /i "%CLEAN_INET%" == "y" (
    echo 正在清理 INetCache 文件夹...
    rd /s /q "C:\Users\Liberty\AppData\Local\Microsoft\Windows\INetCache" 2>nul
    if exist "C:\Users\Liberty\AppData\Local\Microsoft\Windows\INetCache" (
        echo [失败] INetCache 文件夹清理失败
    ) else (
        echo [成功] INetCache 文件夹已清理
    )
)

set /p CLEAN_CHROME="是否清理 Chrome Cache 文件夹？(y/n): "
if /i "%CLEAN_CHROME%" == "y" (
    echo 正在清理 Chrome Cache 文件夹...
    rd /s /q "C:\Users\Liberty\AppData\Local\Google\Chrome\User Data\Default\Cache" 2>nul
    if exist "C:\Users\Liberty\AppData\Local\Google\Chrome\User Data\Default\Cache" (
        echo [失败] Chrome Cache 文件夹清理失败
    ) else (
        echo [成功] Chrome Cache 文件夹已清理
    )
)

echo.
echo ==========================================
echo 清理完成！
echo ==========================================
echo.
echo 提示:
echo - 清理后建议重启电脑
echo - 某些应用首次启动时会重新生成缓存
echo - 游戏和开发工具文件夹请谨慎删除
echo.
pause
