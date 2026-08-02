# Cross-module smoke test
$baseUrl = "http://localhost:8081/api"

# 1. Login
$loginBody = '{"username":"admin","password":"Admin@123"}'
$loginResp = Invoke-RestMethod -Uri "$baseUrl/v1/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$token = $loginResp.data.token
$headers = @{ "Authorization" = "Bearer $token" }
Write-Host "=== [1] Login OK ==="

# 2. Create seal
Write-Host ">>> [2] POST /v1/seals"
$sealBody = '{"sealName":"CrossSeal","sealType":"contract","keeper":"admin","authorizedUsers":["8"],"authorizedScenes":["purchase_contract","electronic_contract"],"remark":"cross"}'
$sealId = $null
try {
    $resp = Invoke-RestMethod -Uri "$baseUrl/v1/seals" -Method Post -Headers $headers -ContentType "application/json" -Body $sealBody
    $sealId = $resp.data.sealId
    Write-Host "OK sealId=$sealId"
} catch {
    Write-Host "FAIL"
}

# 3. Create employee
Write-Host ">>> [3] POST /v1/employees"
$empBody = '{"name":"CrossEmp","gender":"female","hireDate":"2026-07-01 00:00:00","status":"active"}'
try {
    $resp = Invoke-RestMethod -Uri "$baseUrl/v1/employees" -Method Post -Headers $headers -ContentType "application/json" -Body $empBody
    Write-Host "OK empId=$($resp.data.id) code=$($resp.data.employeeCode)"
} catch {
    Write-Host "FAIL"
}

# 4. Create device
Write-Host ">>> [4] POST /v1/devices"
$deviceBody = '{"deviceCode":"DEV-CROSS-002","deviceName":"CrossPrinter","deviceType":1,"connectionType":3,"location":"Store1","remark":"cross"}'
try {
    $resp = Invoke-RestMethod -Uri "$baseUrl/v1/devices" -Method Post -Headers $headers -ContentType "application/json" -Body $deviceBody
    Write-Host "OK deviceId=$($resp.data.deviceId)"
} catch {
    Write-Host "FAIL"
}

# 5. Create supplier
Write-Host ">>> [5] POST /v1/suppliers"
$supplierBody = '{"supplierName":"CrossSupplier","contactPerson":"LiSi","phone":"13900139002","address":"CrossAddr","category":"food","remark":"cross"}'
try {
    $resp = Invoke-RestMethod -Uri "$baseUrl/v1/suppliers" -Method Post -Headers $headers -ContentType "application/json" -Body $supplierBody
    Write-Host "OK supplierId=$($resp.data.supplierId)"
} catch {
    Write-Host "FAIL"
}

# 6. Cross-module: record seal usage
if ($null -ne $sealId) {
    Write-Host ">>> [6] POST /v1/seals/usage-logs"
    $usageBody = '{"sealId":"' + $sealId + '","businessType":"purchase_contract","businessId":"TEST-001","businessNo":"PO001","operator":"admin","remark":"cross"}'
    try {
        $resp = Invoke-RestMethod -Uri "$baseUrl/v1/seals/usage-logs" -Method Post -Headers $headers -ContentType "application/json" -Body $usageBody
        Write-Host "OK logId=$($resp.data.logId)"
    } catch {
        Write-Host "FAIL"
    }

    Write-Host ">>> [7] GET /v1/seals/usage-logs"
    try {
        $resp = Invoke-RestMethod -Uri "$baseUrl/v1/seals/usage-logs?sealId=$sealId&page=1&size=5" -Method Get -Headers $headers
        Write-Host "OK total=$($resp.data.total)"
    } catch {
        Write-Host "FAIL"
    }
}

# 8. Permission center 4 modes
Write-Host ">>> [8] Permission Center"
$resp = Invoke-RestMethod -Uri "$baseUrl/v1/permission-templates/system" -Method Get -Headers $headers
Write-Host "OK templates count=$($resp.data.Count)"
foreach ($t in $resp.data) { Write-Host "   - $($t.code): $($t.name)" }

foreach ($code in @("centralized-single","standard-chain","large-chain","custom")) {
    $resp = Invoke-RestMethod -Uri "$baseUrl/v1/permission-templates/$code" -Method Get -Headers $headers
    Write-Host "OK [$code] -> $($resp.data.name)"
}

# 9. Summary
Write-Host ">>> [9] Cross-module summary"
$sealsList = Invoke-RestMethod -Uri "$baseUrl/v1/seals/page?page=1&size=10" -Method Get -Headers $headers
$empList = Invoke-RestMethod -Uri "$baseUrl/v1/employees/page?page=1&size=10" -Method Get -Headers $headers
$devicesList = Invoke-RestMethod -Uri "$baseUrl/v1/devices/page?page=1&size=10" -Method Get -Headers $headers
Write-Host "OK seals=$($sealsList.data.total) employees=$($empList.data.total) devices=$($devicesList.data.total)"

Write-Host "=== Done ==="
