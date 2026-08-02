# V1 Smoke Test Runner
# Date: 2026-07-04

$ErrorActionPreference = "Continue"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$BASE_URL = "http://localhost:8081/api"
$TestResults = [System.Collections.ArrayList]::new()
$GlobalToken = $null
$GlobalRefreshToken = $null

function Add-Result {
    param($Scenario, $Step, $Status, $Detail, $ResponseBody)
    $result = [PSCustomObject]@{
        Scenario = $Scenario
        Step     = $Step
        Status   = $Status
        Detail   = $Detail
        Time     = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
        Response = if ($ResponseBody -and $ResponseBody.Length -gt 500) { $ResponseBody.Substring(0, 500) + "..." } else { $ResponseBody }
    }
    [void]$script:TestResults.Add($result)
    $icon = if ($Status -eq "PASS") { "[PASS]" } elseif ($Status -eq "FAIL") { "[FAIL]" } else { "[SKIP]" }
    $color = if ($Status -eq "PASS") { "Green" } elseif ($Status -eq "FAIL") { "Red" } else { "Yellow" }
    Write-Host "$icon $Scenario - $Step : $Detail" -ForegroundColor $color
}

function Invoke-Api {
    param($Method, $Path, $Body = $null, $Token = $null)
    $headers = @{ "Content-Type" = "application/json; charset=utf-8" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    try {
        if ($null -ne $Body) {
            $response = Invoke-WebRequest -Uri "$BASE_URL$Path" -Method $Method -Headers $headers -Body $Body -UseBasicParsing -TimeoutSec 15
        } else {
            $response = Invoke-WebRequest -Uri "$BASE_URL$Path" -Method $Method -Headers $headers -UseBasicParsing -TimeoutSec 15
        }
        # Re-decode response body as UTF-8 to handle Chinese chars correctly
        $bodyStr = $response.Content
        try {
            if ($response.RawContentStream) {
                $rawBytes = $response.RawContentStream.ToArray()
                $bodyStr = [System.Text.Encoding]::UTF8.GetString($rawBytes)
            }
        } catch {}
        return @{ Status = $response.StatusCode; Body = $bodyStr }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            $body = ""
            try {
                $reader = New-Object System.IO.StreamReader($resp.GetResponseStream(), [System.Text.Encoding]::UTF8)
                $body = $reader.ReadToEnd()
            } catch {}
            return @{ Status = [int]$resp.StatusCode; Body = $body; Error = $_.Exception.Message }
        }
        return @{ Status = 0; Body = ""; Error = $_.Exception.Message }
    }
}

function Get-Field {
    param($JsonStr, $FieldName)
    if (-not $JsonStr) { return $null }
    try {
        $obj = $JsonStr | ConvertFrom-Json -ErrorAction Stop
        $data = $obj.data
        if ($null -ne $data) {
            if ($data.PSObject.Properties.Name -contains $FieldName) { return $data.$FieldName }
            if ($data.records) {
                $rec0 = $data.records[0]
                if ($null -ne $rec0 -and $rec0.PSObject.Properties.Name -contains $FieldName) { return $rec0.$FieldName }
            }
            if ($data -is [System.Array]) {
                $rec0 = $data[0]
                if ($null -ne $rec0 -and $rec0.PSObject.Properties.Name -contains $FieldName) { return $rec0.$FieldName }
            }
        }
        if ($obj.PSObject.Properties.Name -contains $FieldName) { return $obj.$FieldName }
        return $null
    } catch { return $null }
}

function Get-DataValue {
    # Used when data is a scalar value (e.g. refresh returns token string)
    param($JsonStr)
    if (-not $JsonStr) { return $null }
    try {
        $obj = $JsonStr | ConvertFrom-Json -ErrorAction Stop
        if ($obj.data -is [string]) { return $obj.data }
        if ($null -ne $obj.data) {
            $strVal = "$($obj.data)"
            if ($strVal -and $strVal.Length -gt 0) { return $strVal }
        }
        return $null
    } catch {
        if ($JsonStr -match '"data"\s*:\s*"([^"]+)"') {
            return $Matches[1]
        }
        return $null
    }
}

function Get-Array0Field {
    param($JsonStr, $FieldName)
    try {
        $obj = $JsonStr | ConvertFrom-Json
        $data = $obj.data
        if ($data -is [System.Array] -and $data.Count -gt 0) {
            if ($data[0].PSObject.Properties.Name -contains $FieldName) { return $data[0].$FieldName }
        }
        return $null
    } catch { return $null }
}

Write-Host "============================================================="
Write-Host "V1 Smoke Test Started - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host "Backend: $BASE_URL"
Write-Host "============================================================="
Write-Host ""

# ============================================
# Scenario 1: Auth and Token Refresh
# ============================================
Write-Host ">>> Scenario 1: Auth and Token Refresh" -ForegroundColor Cyan

$resp = Invoke-Api -Method Post -Path "/v1/auth/login" -Body '{"username":"admin","password":"Admin@123"}'
if ($resp.Status -eq 200 -and (Get-Field $resp.Body "token")) {
    $GlobalToken = Get-Field $resp.Body "token"
    $GlobalRefreshToken = Get-Field $resp.Body "refreshToken"
    Add-Result "1-Auth" "1.1 Login" "PASS" "Token acquired"
} else {
    Add-Result "1-Auth" "1.1 Login" "FAIL" "Login failed status=$($resp.Status)"
}

$resp = Invoke-Api -Method Get -Path "/v1/users" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "1-Auth" "1.2 Token Verify" "PASS" "Protected endpoint accessible"
} else {
    Add-Result "1-Auth" "1.2 Token Verify" "FAIL" "Token invalid status=$($resp.Status)"
}

# /v1/auth/refresh requires refresh token in Authorization header (not body)
$resp = Invoke-Api -Method Post -Path "/v1/auth/refresh" -Token $GlobalRefreshToken
$newToken = Get-Field $resp.Body "token"
if (-not $newToken) { $newToken = Get-DataValue $resp.Body }
if ($resp.Status -eq 200 -and $newToken) {
    Add-Result "1-Auth" "1.3 RefreshToken" "PASS" "Token refreshed"
    $GlobalToken = $newToken
} else {
    Add-Result "1-Auth" "1.3 RefreshToken" "FAIL" "Refresh failed status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Post -Path "/v1/auth/logout" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "1-Auth" "1.4 Logout" "PASS" "Logout success"
} else {
    Add-Result "1-Auth" "1.4 Logout" "FAIL" "Logout failed status=$($resp.Status)"
}

$resp = Invoke-Api -Method Post -Path "/v1/auth/login" -Body '{"username":"admin","password":"Admin@123"}'
if ($resp.Status -eq 200) {
    $GlobalToken = Get-Field $resp.Body "token"
    Write-Host "  Re-login for subsequent tests"
}

# ============================================
# Scenario 2: Purchase Stockin Full Flow
# ============================================
Write-Host ""
Write-Host ">>> Scenario 2: Purchase Stockin Full Flow" -ForegroundColor Cyan

$resp = Invoke-Api -Method Get -Path "/v1/suppliers?page=1%26pageSize=1" -Token $GlobalToken
if ($resp.Status -eq 200) {
    $supplierId = Get-Field $resp.Body "supplierId"
    if (-not $supplierId) { $supplierId = Get-Field $resp.Body "id" }
    if (-not $supplierId) { $supplierId = 1 }
    Add-Result "2-Purchase" "2.1 Query Suppliers" "PASS" "supplierId=$supplierId"
} else {
    $supplierId = 1
    Add-Result "2-Purchase" "2.1 Query Suppliers" "FAIL" "status=$($resp.Status)"
}

$resp = Invoke-Api -Method Get -Path "/v1/product-center/foods?page=1%26pageSize=1" -Token $GlobalToken
if ($resp.Status -eq 200) {
    $materialId = Get-Field $resp.Body "id"
    if (-not $materialId) { $materialId = Get-Field $resp.Body "foodId" }
    if (-not $materialId) { $materialId = 1 }
    Add-Result "2-Purchase" "2.2 Query Materials" "PASS" "materialId=$materialId"
} else {
    $materialId = 1
    Add-Result "2-Purchase" "2.2 Query Materials" "FAIL" "status=$($resp.Status)"
}

$resp = Invoke-Api -Method Get -Path "/v1/warehouses?page=1%26pageSize=1" -Token $GlobalToken
if ($resp.Status -eq 200) {
    $warehouseId = Get-Field $resp.Body "warehouseId"
    if (-not $warehouseId) { $warehouseId = Get-Field $resp.Body "id" }
    if (-not $warehouseId) { $warehouseId = 1 }
    Add-Result "2-Purchase" "2.3 Query Warehouses" "PASS" "warehouseId=$warehouseId"
} else {
    $warehouseId = 1
    Add-Result "2-Purchase" "2.3 Query Warehouses" "FAIL" "status=$($resp.Status)"
}

$orderBody = '{"supplierId":' + $supplierId + ',"warehouseId":' + $warehouseId + ',"items":[{"materialId":' + $materialId + ',"materialName":"Smoke Test Material","unit":"kg","quantity":100,"unitPrice":10}]}'
$resp = Invoke-Api -Method Post -Path "/v1/purchase/orders" -Body $orderBody -Token $GlobalToken
if ($resp.Status -eq 200 -or $resp.Status -eq 201) {
    $orderId = Get-Field $resp.Body "orderId"
    if (-not $orderId) { $orderId = Get-Field $resp.Body "id" }
    Add-Result "2-Purchase" "2.4 Create Order" "PASS" "orderId=$orderId"
} else {
    $orderId = $null
    Add-Result "2-Purchase" "2.4 Create Order" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

if ($orderId) {
    $resp = Invoke-Api -Method Put -Path "/v1/purchase/orders/$orderId/approve" -Token $GlobalToken
    if ($resp.Status -eq 200) {
        Add-Result "2-Purchase" "2.5 Approve Order" "PASS" "Order approved"
    } else {
        Add-Result "2-Purchase" "2.5 Approve Order" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
    }
}

if ($orderId) {
    # 查询订单详情获取orderItemId
    $resp = Invoke-Api -Method Get -Path "/v1/purchase/orders/$orderId" -Token $GlobalToken
    $orderItemId = $null
    if ($resp.Status -eq 200) {
        try {
            $orderObj = $resp.Body | ConvertFrom-Json -ErrorAction Stop
            if ($orderObj.data.items -and $orderObj.data.items.Count -gt 0) {
                $orderItemId = $orderObj.data.items[0].orderItemId
                if (-not $orderItemId) { $orderItemId = $orderObj.data.items[0].id }
            }
        } catch {}
    }
    if (-not $orderItemId) { $orderItemId = 1 }

    # 注意：stockin item 字段名为 actualQuantity（不是quantity），且需orderItemId + stockinDate
    $stockinDate = (Get-Date -Format "yyyy-MM-dd")
    $stockinBody = '{"orderId":' + $orderId + ',"warehouseId":' + $warehouseId + ',"stockinDate":"' + $stockinDate + '","items":[{"orderItemId":' + $orderItemId + ',"materialId":' + $materialId + ',"materialName":"Smoke Test Material","unit":"kg","actualQuantity":100,"unitPrice":10}]}'
    $resp = Invoke-Api -Method Post -Path "/v1/purchase/stockins" -Body $stockinBody -Token $GlobalToken
    if ($resp.Status -eq 200 -or $resp.Status -eq 201) {
        $stockinId = Get-Field $resp.Body "stockinId"
        if (-not $stockinId) { $stockinId = Get-Field $resp.Body "id" }
        Add-Result "2-Purchase" "2.6 Create Stockin" "PASS" "stockinId=$stockinId"
    } else {
        $stockinId = $null
        Add-Result "2-Purchase" "2.6 Create Stockin" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
    }
}

if ($stockinId) {
    $resp = Invoke-Api -Method Put -Path "/v1/purchase/stockins/$stockinId/quality-check" -Body '{"qualityCheckResult":1,"qualityRemark":"OK"}' -Token $GlobalToken
    if ($resp.Status -eq 200) {
        Add-Result "2-Purchase" "2.7 Quality Check" "PASS" "QC passed"
    } else {
        Add-Result "2-Purchase" "2.7 Quality Check" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
    }
}

if ($stockinId) {
    $resp = Invoke-Api -Method Put -Path "/v1/purchase/stockins/$stockinId/confirm" -Token $GlobalToken
    if ($resp.Status -eq 200) {
        Add-Result "2-Purchase" "2.8 Confirm Stockin" "PASS" "Stockin confirmed"
    } else {
        Add-Result "2-Purchase" "2.8 Confirm Stockin" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
    }
}

$resp = Invoke-Api -Method Get -Path "/v1/inventory?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "2-Purchase" "2.9 Query Inventory" "PASS" "Inventory query success"
} else {
    Add-Result "2-Purchase" "2.9 Query Inventory" "FAIL" "status=$($resp.Status)"
}

$resp = Invoke-Api -Method Get -Path "/v1/store-inventory/list?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "2-Purchase" "2.10 Query Store Inventory" "PASS" "Store inventory query success"
} else {
    Add-Result "2-Purchase" "2.10 Query Store Inventory" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Get -Path "/v1/finance/payables?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "2-Purchase" "2.11 Query Payables" "PASS" "Payables query success"
} else {
    Add-Result "2-Purchase" "2.11 Query Payables" "FAIL" "status=$($resp.Status)"
}

# ============================================
# Scenario 3: Store Inventory Query and Management
# ============================================
Write-Host ""
Write-Host ">>> Scenario 3: Store Inventory Query and Management" -ForegroundColor Cyan

$resp = Invoke-Api -Method Get -Path "/v1/store-inventory/stores" -Token $GlobalToken
if ($resp.Status -eq 200) {
    $storeId = Get-Array0Field $resp.Body "storeId"
    if (-not $storeId) { $storeId = "1" }
    Add-Result "3-StoreInv" "3.1 Query Stores" "PASS" "storeId=$storeId"
} else {
    $storeId = "1"
    Add-Result "3-StoreInv" "3.1 Query Stores" "FAIL" "status=$($resp.Status)"
}

$resp = Invoke-Api -Method Get -Path "/v1/store-inventory/list?page=1%26pageSize=10" -Token $GlobalToken
if ($resp.Status -eq 200) {
    $invId = Get-Field $resp.Body "id"
    if (-not $invId) { $invId = 1 }
    Add-Result "3-StoreInv" "3.2 Query Store Inventory" "PASS" "inventoryId=$invId"
} else {
    $invId = 1
    Add-Result "3-StoreInv" "3.2 Query Store Inventory" "FAIL" "status=$($resp.Status)"
}

$resp = Invoke-Api -Method Get -Path "/v1/store-inventory/summary?page=1%26pageSize=10" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "3-StoreInv" "3.3 Query Summary" "PASS" "Summary query success"
} else {
    Add-Result "3-StoreInv" "3.3 Query Summary" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Get -Path "/v1/store-inventory/logs?page=1%26pageSize=10" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "3-StoreInv" "3.4 Query Logs" "PASS" "Logs query success"
} else {
    Add-Result "3-StoreInv" "3.4 Query Logs" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$adjustBody = "inventoryId=$invId&quantity=10&type=1&remark=smoke-test"
$headers = @{ "Authorization" = "Bearer $GlobalToken"; "Content-Type" = "application/x-www-form-urlencoded" }
try {
    $respAdjust = Invoke-WebRequest -Uri "$BASE_URL/v1/store-inventory/adjust" -Method Post -Headers $headers -Body $adjustBody -UseBasicParsing -TimeoutSec 15
    if ($respAdjust.StatusCode -eq 200) {
        Add-Result "3-StoreInv" "3.5 Adjust Stock" "PASS" "Stock increased"
    } else {
        Add-Result "3-StoreInv" "3.5 Adjust Stock" "FAIL" "status=$($respAdjust.StatusCode)"
    }
} catch {
    $resp = $_.Exception.Response
    $body = ""
    try {
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $body = $reader.ReadToEnd()
    } catch {}
    Add-Result "3-StoreInv" "3.5 Adjust Stock" "FAIL" "Error: $($_.Exception.Message) body=$body"
}

# ============================================
# Scenario 4: Schedule and Attendance Sync
# ============================================
Write-Host ""
Write-Host ">>> Scenario 4: Schedule and Attendance Sync" -ForegroundColor Cyan

$resp = Invoke-Api -Method Get -Path "/v1/schedule/plans?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    $planId = Get-Field $resp.Body "planId"
    if (-not $planId) { $planId = 1 }
    Add-Result "4-Schedule" "4.1 Query Plans" "PASS" "planId=$planId"
} else {
    $planId = 1
    Add-Result "4-Schedule" "4.1 Query Plans" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Get -Path "/v1/schedule/plans/$planId/entries" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "4-Schedule" "4.2 Query Entries" "PASS" "Entries query success"
} else {
    Add-Result "4-Schedule" "4.2 Query Entries" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Get -Path "/v1/attendance/page?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "4-Schedule" "4.3 Query Attendance" "PASS" "Attendance query success"
} else {
    Add-Result "4-Schedule" "4.3 Query Attendance" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Get -Path "/v1/schedule/plans/stats" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "4-Schedule" "4.4 Query Stats" "PASS" "Stats query success"
} else {
    Add-Result "4-Schedule" "4.4 Query Stats" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# ============================================
# Scenario 5: Recruitment Flow
# ============================================
Write-Host ""
Write-Host ">>> Scenario 5: Recruitment Flow" -ForegroundColor Cyan

$resp = Invoke-Api -Method Get -Path "/v1/recruitment-requirements?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "5-Recruit" "5.1 Query Requirements" "PASS" "Requirements query success"
} else {
    Add-Result "5-Recruit" "5.1 Query Requirements" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$reqBody = '{"positionName":"Smoke Test Position","requirementNum":1,"requirements":"smoke-test","storeId":"1","type":"hr","status":"open","approvalStatus":"pending"}'
$resp = Invoke-Api -Method Post -Path "/v1/recruitment-requirements" -Body $reqBody -Token $GlobalToken
if ($resp.Status -eq 200 -or $resp.Status -eq 201) {
    $reqId = Get-Field $resp.Body "id"
    if (-not $reqId) { $reqId = Get-Field $resp.Body "requirementId" }
    Add-Result "5-Recruit" "5.2 Create Requirement" "PASS" "requirementId=$reqId"
} else {
    $reqId = $null
    Add-Result "5-Recruit" "5.2 Create Requirement" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Get -Path "/v1/resumes?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "5-Recruit" "5.3 Query Resumes" "PASS" "Resumes query success"
} else {
    Add-Result "5-Recruit" "5.3 Query Resumes" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Get -Path "/v1/interviews?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "5-Recruit" "5.4 Query Interviews" "PASS" "Interviews query success"
} else {
    Add-Result "5-Recruit" "5.4 Query Interviews" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# ============================================
# Scenario 6: Food Traceability Scan
# ============================================
Write-Host ""
Write-Host ">>> Scenario 6: Food Traceability Scan" -ForegroundColor Cyan

$resp = Invoke-Api -Method Get -Path "/v1/trace-codes/page?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    $traceCode = Get-Field $resp.Body "traceCode"
    $traceId = Get-Field $resp.Body "id"
    Add-Result "6-Trace" "6.1 Query Trace Codes" "PASS" "traceCode=$traceCode"
} else {
    Add-Result "6-Trace" "6.1 Query Trace Codes" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

if ($traceCode) {
    $resp = Invoke-Api -Method Get -Path "/v1/trace-codes/$traceCode" -Token $GlobalToken
    if ($resp.Status -eq 200) {
        Add-Result "6-Trace" "6.2 Query by Code" "PASS" "Trace code query success"
    } else {
        Add-Result "6-Trace" "6.2 Query by Code" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
    }
}

if ($traceId) {
    $resp = Invoke-Api -Method Get -Path "/v1/trace-codes/$traceId/chain" -Token $GlobalToken
    if ($resp.Status -eq 200) {
        Add-Result "6-Trace" "6.3 Query Chain" "PASS" "Trace chain query success"
    } else {
        Add-Result "6-Trace" "6.3 Query Chain" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
    }
}

if ($traceCode) {
    $resp = Invoke-Api -Method Post -Path "/v1/trace-codes/$traceCode/scan" -Token $GlobalToken
    if ($resp.Status -eq 200) {
        Add-Result "6-Trace" "6.4 Scan Code" "PASS" "Scan success"
    } else {
        Add-Result "6-Trace" "6.4 Scan Code" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
    }
}

# ============================================
# Scenario 7: Permission Control
# ============================================
Write-Host ""
Write-Host ">>> Scenario 7: Permission Control" -ForegroundColor Cyan

$resp = Invoke-Api -Method Get -Path "/v1/users"
if ($resp.Status -eq 401 -or $resp.Status -eq 403) {
    Add-Result "7-Permission" "7.1 No Token Rejected" "PASS" "status=$($resp.Status)"
} else {
    Add-Result "7-Permission" "7.1 No Token Rejected" "FAIL" "Should reject status=$($resp.Status)"
}

$resp = Invoke-Api -Method Get -Path "/v1/users" -Token "invalid.token.here"
if ($resp.Status -eq 401 -or $resp.Status -eq 403) {
    Add-Result "7-Permission" "7.2 Invalid Token Rejected" "PASS" "status=$($resp.Status)"
} else {
    Add-Result "7-Permission" "7.2 Invalid Token Rejected" "FAIL" "status=$($resp.Status)"
}

$resp = Invoke-Api -Method Get -Path "/v1/users?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "7-Permission" "7.3 Admin Access Users" "PASS" "Admin permission verified"
} else {
    Add-Result "7-Permission" "7.3 Admin Access Users" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Get -Path "/v1/roles?page=1%26pageSize=5" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "7-Permission" "7.4 Query Roles" "PASS" "Roles query success"
} else {
    Add-Result "7-Permission" "7.4 Query Roles" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

$resp = Invoke-Api -Method Get -Path "/v1/permissions" -Token $GlobalToken
if ($resp.Status -eq 200) {
    Add-Result "7-Permission" "7.5 Query Permissions" "PASS" "Permissions query success"
} else {
    Add-Result "7-Permission" "7.5 Query Permissions" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# ============================================
# Summary
# ============================================
Write-Host ""
Write-Host "============================================================="
Write-Host "Test Results Summary"
Write-Host "============================================================="
$pass = @($TestResults | Where-Object { $_.Status -eq "PASS" }).Count
$fail = @($TestResults | Where-Object { $_.Status -eq "FAIL" }).Count
$skip = @($TestResults | Where-Object { $_.Status -eq "SKIP" }).Count
$total = @($TestResults).Count
Write-Host "Total: $total"
if ($total -gt 0) {
    $passRate = [math]::Round($pass/$total*100, 1)
    Write-Host "Pass: $pass ($passRate%)"
}
Write-Host "Fail: $fail"
Write-Host "Skip: $skip"
Write-Host ""

$exportPath = "p:\my-new-project\docs\smoke-test-results.csv"
$TestResults | Export-Csv -Path $exportPath -NoTypeInformation -Encoding UTF8
Write-Host "Results exported to: $exportPath"

$jsonPath = "p:\my-new-project\docs\smoke-test-results.json"
$TestResults | ConvertTo-Json -Depth 3 | Out-File -FilePath $jsonPath -Encoding UTF8
Write-Host "Results exported to: $jsonPath"

Write-Host ""
Write-Host "Failed Items:"
$failedItems = @($TestResults | Where-Object { $_.Status -eq "FAIL" })
if ($failedItems -and $failedItems.Count -gt 0) {
    foreach ($item in $failedItems) {
        Write-Host "  - $($item.Scenario) / $($item.Step): $($item.Detail)" -ForegroundColor Red
        if ($item.Response) { Write-Host "    Response: $($item.Response)" -ForegroundColor DarkGray }
    }
} else {
    Write-Host "  No failures"
}
