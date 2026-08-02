# H5表单服务器防火墙配置脚本
# 以管理员身份运行此脚本
# 右键点击 → "以管理员身份运行"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  🛡️  配置防火墙规则（允许H5表单访问）" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 检查管理员权限
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "❌ 错误：需要管理员权限！" -ForegroundColor Red
    Write-Host "   请右键点击此脚本，选择'以管理员身份运行'`n" -ForegroundColor Yellow
    Read-Host "按回车键退出"
    exit 1
}

Write-Host "✅ 管理员权限确认通过`n" -ForegroundColor Green

# 添加3003端口规则（H5表单服务器）
Write-Host "📋 正在添加防火墙规则..." -ForegroundColor Yellow

try {
    # 删除已存在的规则（如果有的话）
    netsh advfirewall firewall delete rule name="H5 Form Server 3003" 2>$null | Out-Null

    # 添加新规则 - 允许入站连接
    netsh advfirewall firewall add rule `
        name="H5 Form Server 3003" `
        dir=in `
        action=allow `
        protocol=TCP `
        localport=3003 `
        profile=private `
        description="允许内网设备访问H5应聘表单（安全隔离端口）" `
        | Out-Null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 成功：端口 3003 已允许访问" -ForegroundColor Green
    } else {
        throw "添加规则失败"
    }

    # 验证规则是否生效
    $rule = netsh advfirewall firewall show rule name="H5 Form Server 3003" 2>$null
    if ($rule -match "3003") {
        Write-Host "✅ 防火墙规则验证通过`n" -ForegroundColor Green
    }
}
catch {
    Write-Host "❌ 添加防火墙规则失败：$_" -ForegroundColor Red
    Read-Host "`n按回车键退出"
    exit 1
}

# 显示网络信息
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  📱 配置完成！现在可以测试了" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 获取本机IP
$ipAddress = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -notmatch "Loopback" -and !$_.IsLoopback }).IPAddress | Select-Object -First 1

if ($ipAddress) {
    Write-Host "🌐 您的内网IP地址：" -ForegroundColor White
    Write-Host "   $ipAddress`n" -ForegroundColor Yellow

    Write-Host "📱 手机访问地址：" -ForegroundColor White
    Write-Host "   http://$ipAddress`:3003/recruit/apply.html?job=岗位ID`n" -ForegroundColor Green
}

Write-Host "🔧 测试步骤：" -ForegroundColor White
Write-Host "   1. 确保手机与电脑在同一WiFi下" -ForegroundColor Gray
Write-Host "   2. 打开手机浏览器，访问上面的地址" -ForegroundColor Gray
Write-Host "   3. 或扫描管理端生成的二维码`n" -ForegroundColor Gray

Write-Host "⚠️  注意事项：" -ForegroundColor White
Write-Host "   - 此规则仅对'专用网络'生效（家庭/工作网络）" -ForegroundColor Gray
Write-Host "   - 如果在公共网络（咖啡厅等），需额外配置`n" -ForegroundColor Gray

Read-Host "按回车键退出"
