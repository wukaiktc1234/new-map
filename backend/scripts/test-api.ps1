$ErrorActionPreference = 'Continue'
$BaseUrl = 'http://127.0.0.1:8081/api'

# 登录获取 Token
$loginBody = '{"username":"admin","password":"Admin@123"}'
$loginResp = Invoke-WebRequest -Uri "$BaseUrl/v1/auth/login" -Method POST -Body $loginBody -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
$loginData = $loginResp.Content | ConvertFrom-Json
$token = $loginData.data.token
$headers = @{ Authorization = "Bearer $token" }

$pass = 0
$fail = 0
$results = @()

function TestEndpoint($method, $url, $module, $feature) {
    $fullUrl = "$BaseUrl$url"
    try {
        $resp = Invoke-WebRequest -Uri $fullUrl -Method $method -Headers $headers -UseBasicParsing -TimeoutSec 15
        $data = $resp.Content | ConvertFrom-Json
        if ($data.code -eq 0) {
            $info = ""
            if ($data.data.records) { $info = "records=$($data.data.records.Count) total=$($data.data.total)" }
            elseif ($data.data -is [array]) { $info = "array=$($data.data.Count)" }
            else { $info = "OK" }
            Write-Host "[PASS] $method $url -> code=0 | $info" -ForegroundColor Green
            $script:pass++
            $script:results += [PSCustomObject]@{Module=$module; Feature=$feature; Method=$method; Url=$url; Status='PASS'; Code=0; Info=$info}
        } else {
            Write-Host "[FAIL] $method $url -> code=$($data.code) | $($data.message)" -ForegroundColor Red
            $script:fail++
            $script:results += [PSCustomObject]@{Module=$module; Feature=$feature; Method=$method; Url=$url; Status='FAIL'; Code=$data.code; Info=$data.message}
        }
    } catch {
        $sc = 'N/A'
        if ($_.Exception.Response) { $sc = [int]$_.Exception.Response.StatusCode }
        Write-Host "[FAIL] $method $url -> $sc | $($_.Exception.Message)" -ForegroundColor Red
        $script:fail++
        $script:results += [PSCustomObject]@{Module=$module; Feature=$feature; Method=$method; Url=$url; Status='FAIL'; Code=$sc; Info=$_.Exception.Message}
    }
}

Write-Host "===== Purchase Module =====" -ForegroundColor Cyan
TestEndpoint 'GET' '/v1/purchase/requests/page?page=1&size=10' 'Purchase' 'Request-Page'
TestEndpoint 'GET' '/v1/purchase/requests/statistics' 'Purchase' 'Request-Stats'
TestEndpoint 'GET' '/v1/purchase/requests/status/pending?page=1&size=10' 'Purchase' 'Request-Pending'
TestEndpoint 'GET' '/v1/purchase/orders?page=1&size=10' 'Purchase' 'Order-Page'
TestEndpoint 'GET' '/v1/purchase/plans?page=1&size=10' 'Purchase' 'Plan-Page'
TestEndpoint 'GET' '/v1/purchase/contracts/page?page=1&size=10' 'Purchase' 'Contract-Page'
TestEndpoint 'GET' '/v1/purchase/electronic-contracts/page?page=1&size=10' 'Purchase' 'EContract-Page'
TestEndpoint 'GET' '/v1/purchase/stockins?page=1&size=10' 'Purchase' 'Stockin-Page'
TestEndpoint 'GET' '/v1/purchase/settlements?page=1&size=10' 'Purchase' 'Settlement-Page'
TestEndpoint 'GET' '/v1/suppliers/page?page=1&size=10' 'Purchase' 'Supplier-Page'
TestEndpoint 'GET' '/v1/suppliers/list' 'Purchase' 'Supplier-Active'
TestEndpoint 'GET' '/v1/suppliers/statistics' 'Purchase' 'Supplier-Stats'
TestEndpoint 'GET' '/v1/purchase/archives?page=1&size=10' 'Purchase' 'Archive-Page'
TestEndpoint 'GET' '/v1/material-categories/page?page=1&size=10' 'Purchase' 'Category-Page'
TestEndpoint 'GET' '/v1/material-categories' 'Purchase' 'Category-Active'
TestEndpoint 'GET' '/v1/material-categories/all' 'Purchase' 'Category-All'
TestEndpoint 'GET' '/v1/purchase/material-requests?page=1&size=10' 'Purchase' 'MaterialRequest-Page'
TestEndpoint 'GET' '/v1/purchase/material-requests/statistics' 'Purchase' 'MaterialRequest-Stats'
TestEndpoint 'GET' '/v1/purchase/reports/summary' 'Purchase' 'Report-Summary'
TestEndpoint 'GET' '/v1/purchase/reports/monthly-trend?startDate=2026-01-01&endDate=2026-06-30' 'Purchase' 'Report-MonthlyTrend'
TestEndpoint 'GET' '/v1/purchase/reports/supplier' 'Purchase' 'Report-Supplier'
TestEndpoint 'GET' '/v1/purchase/reports/category' 'Purchase' 'Report-Category'
TestEndpoint 'GET' '/v1/purchase/analysis/trend?startDate=2026-01-01&endDate=2026-06-30' 'Purchase' 'Analysis-Trend'
TestEndpoint 'GET' '/v1/purchase/analysis/supplier' 'Purchase' 'Analysis-Supplier'
TestEndpoint 'GET' '/v1/purchase/analysis/category' 'Purchase' 'Analysis-Category'
TestEndpoint 'GET' '/v1/purchase/analysis/summary' 'Purchase' 'Analysis-Summary'

Write-Host "`n===== Warehouse Module =====" -ForegroundColor Cyan
TestEndpoint 'GET' '/v1/warehouses?page=1&size=10' 'Warehouse' 'Warehouse-Page'
TestEndpoint 'GET' '/v1/warehouses/active' 'Warehouse' 'Warehouse-Active'
TestEndpoint 'GET' '/v1/inventory?page=1&size=10' 'Warehouse' 'Inventory-Page'
TestEndpoint 'GET' '/v1/inventory/low-stock' 'Warehouse' 'Inventory-LowStock'
TestEndpoint 'GET' '/v1/inventory-checks' 'Warehouse' 'Check-List'
TestEndpoint 'GET' '/v1/inventory/transfers/page?page=1&size=10' 'Warehouse' 'Transfer-Page'
TestEndpoint 'GET' '/v1/inventory/warnings/page?page=1&size=10' 'Warehouse' 'Warning-Page'
TestEndpoint 'GET' '/v1/inventory/warnings/purchase-suggestions' 'Warehouse' 'Warning-Suggestions'
TestEndpoint 'GET' '/v1/inventory/warning-rules/page?page=1&size=10' 'Warehouse' 'WarningRule-Page'
TestEndpoint 'GET' '/v1/inventory/losses/page?page=1&size=10' 'Warehouse' 'Loss-Page'
TestEndpoint 'GET' '/v1/inventory/outbounds/page?page=1&size=10' 'Warehouse' 'Outbound-Page'
TestEndpoint 'GET' '/v1/inventory/adjustments/page?page=1&size=10' 'Warehouse' 'Adjust-Page'
TestEndpoint 'GET' '/v1/inventory/stats/overview' 'Warehouse' 'Stats-Overview'
TestEndpoint 'GET' '/v1/inventory/stats/trend?startDate=2026-01-01&endDate=2026-06-30' 'Warehouse' 'Stats-Trend'
TestEndpoint 'GET' '/v1/inventory/stats/category' 'Warehouse' 'Stats-Category'
TestEndpoint 'GET' '/v1/inventory/stats/warning-trend' 'Warehouse' 'Stats-WarningTrend'
TestEndpoint 'GET' '/v1/analytics/inventory/overview' 'Warehouse' 'Analysis-Overview'
TestEndpoint 'GET' '/v1/analytics/inventory/turnover-analysis' 'Warehouse' 'Analysis-Turnover'
TestEndpoint 'GET' '/v1/analytics/inventory/abc-classification' 'Warehouse' 'Analysis-ABC'
TestEndpoint 'GET' '/v1/analytics/inventory/waste-analysis' 'Warehouse' 'Analysis-Waste'
TestEndpoint 'GET' '/v1/analytics/inventory/stockout-risk' 'Warehouse' 'Analysis-Stockout'
TestEndpoint 'GET' '/v1/analytics/inventory/warehouse-value' 'Warehouse' 'Analysis-WarehouseValue'
TestEndpoint 'GET' '/v1/inventory-locations?page=1&size=10' 'Warehouse' 'Location-Page'
TestEndpoint 'GET' '/v1/inventory/logs/page?page=1&size=10' 'Warehouse' 'Log-Page'

Write-Host "`n===== Summary =====" -ForegroundColor Cyan
Write-Host "Total: $($pass + $fail) | PASS: $pass | FAIL: $fail" -ForegroundColor White
$results | Export-Csv -Path 'P:\my-new-project\backend\scripts\api-test-results.csv' -NoTypeInformation -Encoding UTF8
Write-Host "Results exported to api-test-results.csv" -ForegroundColor Cyan
if ($fail -gt 0) {
    Write-Host "`n===== Failed Endpoints =====" -ForegroundColor Red
    $results | Where-Object { $_.Status -eq 'FAIL' } | Format-Table Module, Feature, Method, Url, Code, Info -AutoSize
}
