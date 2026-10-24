# PC-8：确保本地 PostgreSQL 18 在需要时可用（服务被设为 Manual 且无管理员权限时的兜底）
# 用法：
#   pwsh -File scripts/qa/start-postgres.ps1           # 服务未起则拉起，已起则跳过
#   pwsh -File scripts/qa/start-postgres.ps1 -Check    # 只检查，不启动
# 背景：postgresql-x64-18 的服务启动类型是 Manual，Set-Service 需要管理员权限（本机被拒）；
#       夜间巡航 / CI / E2E 前请先执行本脚本，或在计划任务里挂 ONLOGON。

param(
  [switch]$Check,
  [string]$ServiceName = 'postgresql-x64-18',
  [string]$PgData = 'P:\my-new-project\PostgreSQL\18\data',
  [string]$PgCtl  = 'P:\my-new-project\PostgreSQL\18\bin\pg_ctl.exe',
  [int]$Port = 5432
)

function Test-PgUp {
  try {
    $c = New-Object System.Net.Sockets.TcpClient
    $c.Connect('127.0.0.1', $Port)
    $c.Close()
    return $true
  } catch { return $false }
}

if (Test-PgUp) { Write-Host "[PC-8] PostgreSQL 已在监听 $Port，跳过"; exit 0 }
if ($Check) { Write-Host "[PC-8] PostgreSQL 未监听 $Port（-Check 模式不启动）"; exit 1 }

$svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($svc) {
  Write-Host "[PC-8] 服务 $ServiceName 状态=$($svc.Status) StartType=$($svc.StartType)"
  if ($svc.Status -ne 'Running') {
    try { Start-Service -Name $ServiceName -ErrorAction Stop; Write-Host "[PC-8] Start-Service 成功" }
    catch { Write-Host "[PC-8] Start-Service 被拒（$($_.Exception.Message)），改用 pg_ctl" }
  }
}

if (-not (Test-PgUp)) {
  # pg_ctl 必须脱离当前进程启动，否则父进程退出会把库一起带走
  Start-Process -FilePath $PgCtl -ArgumentList '-D', $PgData, '-l', (Join-Path $env:TEMP 'pg-start.log'), 'start' -WindowStyle Hidden
  for ($i = 0; $i -lt 20; $i++) { Start-Sleep -Milliseconds 750; if (Test-PgUp) { break } }
}

if (Test-PgUp) { Write-Host "[PC-8] PostgreSQL 已就绪（127.0.0.1:$Port）"; exit 0 }
Write-Host "[PC-8] 启动失败：请检查 $PgData 的 postmaster.pid 是否残留，或改用管理员启动服务"
exit 1
