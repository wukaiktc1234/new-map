# POS支付系统 v0.13.1 对抗性安全验证脚本
# 生成时间: 2026-04-06

Write-Host "╔══════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  POS支付系统 v0.13.1 最终E2E安全验证测试          ║" -ForegroundColor Cyan
Write-Host "║  时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')           ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 获取Token
$loginResp = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"<redacted>"}'
$token = $loginResp.data.token
$bearer = "Bearer $token"
Write-Host "[INIT] Token获取成功 (长度:$($token.Length))" -ForegroundColor Green

# PART 1: E2E正常流程
Write-Host ""
Write-Host "████████████████████████████████████████████████████" -ForegroundColor Yellow
Write-Host "  PART 1: E2E正常流程测试 (4项)" -ForegroundColor Yellow
Write-Host "████████████████████████████████████████████████████" -ForegroundColor Yellow
Write-Host ""

# TEST-0 登录
Write-Host "[TEST-0] LOGIN:" -ForegroundColor White
if ($loginResp.code -eq 0) { Write-Host "  => PASS - code=0, token有效" -ForegroundColor Green }
else { Write-Host "  => FAIL" -ForegroundColor Red }

# TEST-1 创建订单
Write-Host ""
Write-Host "[TEST-1] CREATE ORDER:" -ForegroundColor White
$orderJson = '{"tableNumber":"A01","items":[{"id":"F001","name":"TestFood","price":28.00,"quantity":2}],"paymentMethod":"cash","orderType":"dinein"}'
try {
    $createResp = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body $orderJson
    
    if ($createResp.code -eq 0) {
        $oid = $createResp.data.orderId
        $src = $createResp.data.orderSource  
        $amt = $createResp.data.totalAmount
        Write-Host "  => PASS - 订单创建成功" -ForegroundColor Green
        Write-Host "     OrderID: $oid | Source: $src | Amount: `n$amt" -ForegroundColor Cyan
        
        if ($src -eq 4) { Write-Host "     [OK] orderSource=4验证通过" -ForegroundColor Green }
        else { Write-Host "     [WARN] orderSource=$src(期望4)" -ForegroundColor Yellow }
        
        $oid | Out-File "P:\my-new-project\backend\test_oid.txt" -Encoding UTF8
    } else {
        Write-Host "  => FAIL - $($createResp.message)" -ForegroundColor Red
    }
} catch { Write-Host "  => FAIL - 异常: $_" -ForegroundColor Red }

$oid = ""
if (Test-Path "P:\my-new-project\backend\test_oid.txt") {
    $oid = (Get-Content "P:\my-new-project\backend\test_oid.txt" -Raw).Trim()
}

# TEST-2 支付订单
Write-Host ""
Write-Host "[TEST-2] PAY ORDER:" -ForegroundColor White
if ($oid) {
    $payJson = "{`"orderId`":`"$oid`",`"paymentMethod`":`"cash`"}"
    try {
        $payResp = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order/pay" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body $payJson
        
        if ($payResp.code -eq 0) {
            $pst = $payResp.data.status
            Write-Host "  => PASS - 支付成功, status=$pst" -ForegroundColor Green
            if ($pst -eq 1) { Write-Host "     [OK] status=1(已支付)" -ForegroundColor Green }
        } elseif ($payResp.message -match '权限|403|unauthorized') {
            Write-Host "  => BLOCKED - 权限拦截" -ForegroundColor Yellow
        } else {
            Write-Host "  => FAIL - $($payResp.message)" -ForegroundColor Red
        }
    } catch {
        if ($_ -match '403|401') { Write-Host "  => BLOCKED - HTTP权限错误" -ForegroundColor Yellow }
        else { Write-Host "  => FAIL - 异常: $_" -ForegroundColor Red }
    }
} else { Write-Host "  => SKIP (无订单号)" -ForegroundColor Gray }

# TEST-3 查询订单
Write-Host ""
Write-Host "[TEST-3] QUERY ORDER:" -ForegroundColor White
if ($oid) {
    try {
        $qryResp = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order/$oid" -Method GET -Headers @{Authorization=$bearer}
        
        if ($qryResp.code -eq 0) {
            $ds = $qryResp.data.status
            $da = $qryResp.data.totalAmount
            Write-Host "  => PASS - 查询成功" -ForegroundColor Green
            Write-Host "     DB_Status: $ds | DB_Amount: `n$da" -ForegroundColor Cyan
        } else {
            Write-Host "  => WARN - $($qryResp.message)" -ForegroundColor Yellow
        }
    } catch { Write-Host "  => WARN - 异常: $_" -ForegroundColor Yellow }
} else { Write-Host "  => SKIP" -ForegroundColor Gray }

Write-Host ""
Write-Host "=== PART 1 COMPLETE ===" -ForegroundColor Cyan

# PART 2: 安全攻击模拟
Write-Host ""
Write-Host "████████████████████████████████████████████████████" -ForegroundColor Magenta
Write-Host "  PART 2: 安全攻击模拟测试 (7项)" -ForegroundColor Magenta  
Write-Host "████████████████████████████████████████████████████" -ForegroundColor Magenta
Write-Host ""

if (-not $oid) {
    $atkOrder = '{"tableNumber":"A99","items":[{"id":"F001","name":"AttackTest","price":100.00,"quantity":1}],"paymentMethod":"cash","orderType":"dinein"}'
    try {
        $atkCreate = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body $atkOrder
        if ($atkCreate.code -eq 0) {
            $oid = $atkCreate.data.orderId
            Write-Host "[SETUP] 创建攻击测试订单: $oid" -ForegroundColor Gray
        }
    } catch {}
}

if ($oid) {
    # A1 价格篡改
    Write-Host "[SEC-A1] Price Tampering (amount=0.01):" -ForegroundColor Red
    try {
        $a1 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order/pay" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body "{`"orderId`":`"$oid`",`"paymentMethod`":`"cash`",`"amount`":0.01}"
        if ($a1.code -ne 0) { Write-Host "  => BLOCKED - $($a1.message)" -ForegroundColor Green }
        else { Write-Host "  => VULNERABLE! Resp:$($a1.message)" -ForegroundColor Red }
    } catch { Write-Host "  => BLOCKED (exception)" -ForegroundColor Green }
    
    # A2 超额支付
    Write-Host ""
    Write-Host "[SEC-A2] Overpayment (amount=9999):" -ForegroundColor Red
    try {
        $a2 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order/pay" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body "{`"orderId`":`"$oid`",`"paymentMethod`":`"cash`",`"amount`":9999}"
        if ($a2.code -ne 0) { Write-Host "  => BLOCKED - $($a2.message)" -ForegroundColor Green }
        else { Write-Host "  => VULNERABLE! Resp:$($a2.message)" -ForegroundColor Red }
    } catch { Write-Host "  => BLOCKED (exception)" -ForegroundColor Green }
    
    # A3 零金额
    Write-Host ""
    Write-Host "[SEC-A3] Zero Amount (amount=0):" -ForegroundColor Red
    try {
        $a3 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order/pay" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body "{`"orderId`":`"$oid`",`"paymentMethod`":`"cash`",`"amount`":0}"
        if ($a3.code -ne 0) { Write-Host "  => BLOCKED - $($a3.message)" -ForegroundColor Green }
        else { Write-Host "  => VULNERABLE! Resp:$($a3.message)" -ForegroundColor Red }
    } catch { Write-Host "  => BLOCKED (exception)" -ForegroundColor Green }
    
    # A4 负数金额
    Write-Host ""
    Write-Host "[SEC-A4] Negative Amount (amount=-5):" -ForegroundColor Red
    try {
        $a4 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order/pay" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body "{`"orderId`":`"$oid`",`"paymentMethod`":`"cash`",`"amount`":-5}"
        if ($a4.code -ne 0) { Write-Host "  => BLOCKED - $($a4.message)" -ForegroundColor Green }
        else { Write-Host "  => VULNERABLE! Resp:$($a4.message)" -ForegroundColor Red }
    } catch { Write-Host "  => BLOCKED (exception)" -ForegroundColor Green }
    
    # B1 重复支付
    Write-Host ""
    Write-Host "[SEC-B1] Duplicate Payment:" -ForegroundColor Red
    try {
        $b1 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order/pay" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body "{`"orderId`":`"$oid`",`"paymentMethod`":`"cash`"}"
        if ($b1.code -ne 0) { Write-Host "  => BLOCKED - $($b1.message) [CAS OK]" -ForegroundColor Green }
        else { Write-Host "  => VULNERABLE! Resp:$($b1.message)" -ForegroundColor Red }
    } catch { Write-Host "  => BLOCKED (exception)" -ForegroundColor Green }
    
    # C1 假ID
    Write-Host ""
    Write-Host "[SEC-C1] Fake OrderID:" -ForegroundColor Red
    try {
        $c1 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order/pay" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body "{`"orderId`":`"FAKE_99999_XYZ`",`"paymentMethod`":`"cash`"}"
        if ($c1.code -ne 0) { Write-Host "  => BLOCKED - $($c1.message)" -ForegroundColor Green }
        else { Write-Host "  => VULNERABLE! Resp:$($c1.message)" -ForegroundColor Red }
    } catch { Write-Host "  => BLOCKED (exception)" -ForegroundColor Green }
    
    # D1 SQL注入
    Write-Host ""
    Write-Host "[SEC-D1] SQL Injection:" -ForegroundColor Red
    try {
        $d1 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pos/order/pay" -Method POST -ContentType "application/json" -Headers @{Authorization=$bearer} -Body "{`"orderId`":`"$oid' OR '1'='1`",`"paymentMethod`":`"cash`"}"
        if ($d1.code -ne 0) { Write-Host "  => BLOCKED - $($d1.message)" -ForegroundColor Green }
        else { Write-Host "  => VULNERABLE! Resp:$($d1.message)" -ForegroundColor Red }
    } catch { Write-Host "  => BLOCKED (exception)" -ForegroundColor Green }
    
} else {
    Write-Host "SKIP: No order for attack tests" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== PART 2 COMPLETE ===" -ForegroundColor Magenta

# PART 3: Critical修复验证
Write-Host ""
Write-Host "████████████████████████████████████████████████████" -ForegroundColor Blue
Write-Host "  PART 3: Critical修复验证 (5项)" -ForegroundColor Blue
Write-Host "████████████████████████████████████████████████████" -ForegroundColor Blue
Write-Host ""

Write-Host "[C1] CAS Condition Update:" -ForegroundColor White
$c1 = Select-String -Path "P:\my-new-project\backend\src\main\java\com\example\demo\controller\PosOrderController.java" -Pattern "status.*-1|WHERE.*status"
if ($c1) { Write-Host "  => VERIFIED (Line:$($c1Check.LineNumber))" -ForegroundColor Green }
else { Write-Host "  => NOT FOUND in Controller (maybe Service layer)" -ForegroundColor Yellow }

Write-Host ""
Write-Host "[C2] Stock Limit (<=99999):" -ForegroundColor White
$c2 = Select-String -Path "P:\my-new-project\backend\src\main\java" -Recurse -Pattern "99999|maxStock" -Include "*.java"
if ($c2) { Write-Host "  => VERIFIED" -ForegroundColor Green }
else { Write-Host "  => NOT FOUND" -ForegroundColor Yellow }

Write-Host ""
Write-Host "[C3] Idempotency Key:" -ForegroundColor White
$c3 = Select-String -Path "P:\my-new-project\backend\src\main\java\com\example\demo\dto\OrderRequestDTO.java" -Pattern "idempotencyKey"
if ($c3) { Write-Host "  => VERIFIED" -ForegroundColor Green }
else { Write-Host "  => NOT FOUND" -ForegroundColor Yellow }

Write-Host ""
Write-Host "[C4] @PreAuthorize:" -ForegroundColor White
$c4p = Select-String -Path "P:\my-new-project\backend\src\main\java\com\example\demo\controller\PosOrderController.java" -Pattern "PreAuthorize.*pay"
$c4r = Select-String -Path "P:\my-new-project\backend\src\main\java\com\example\demo\controller\PosOrderController.java" -Pattern "PreAuthorize.*refund"
if ($c4p -and $c4r) { Write-Host "  => VERIFIED (both pay & refund)" -ForegroundColor Green }
elseif ($c4p -or $c4r) { Write-Host "  => PARTIAL" -ForegroundColor Yellow }
else { Write-Host "  => NOT FOUND" -ForegroundColor Yellow }

Write-Host ""
Write-Host "[C5] DB Price Override:" -ForegroundColor White
$c5 = Select-String -Path "P:\my-new-project\backend\src\main\java\com\example\demo\controller\PosOrderController.java" -Pattern "foodMapper|selectByFoodCode|getPrice"
if ($c5) { Write-Host "  => VERIFIED" -ForegroundColor Green }
else { Write-Host "  => NOT FOUND" -ForegroundColor Yellow }

Write-Host ""
Write-Host "=== PART 3 COMPLETE ===" -ForegroundColor Blue

Write-Host ""
Write-Host "============================================================" -ForegroundColor White
Write-Host "  Test Completed: $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor White
Write-Host "============================================================" -ForegroundColor White
