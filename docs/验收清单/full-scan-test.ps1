# Full page acceptance scan script
# Tests core GET API for each business page after admin login
# Output: pass/fail/skip status for each page

$ErrorActionPreference = 'Continue'
$baseUrl = 'http://localhost:8081/api'
$outputFile = 'p:\my-new-project\docs\验收清单\full-scan-results.txt'
$summaryFile = 'p:\my-new-project\docs\验收清单\full-scan-summary.md'

# ========== Login ==========
Write-Host "===== LOGIN =====" -ForegroundColor Cyan
$body = '{"username":"admin","password":"Admin@123"}'
try {
    $r = Invoke-WebRequest -Uri "$baseUrl/v1/auth/login" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing -TimeoutSec 10
    $json = $r.Content | ConvertFrom-Json
    if ($json.code -ne 0) { Write-Host "LOGIN FAILED: $($json.message)" -ForegroundColor Red; exit 1 }
    $token = $json.data.token
    Write-Host "LOGIN OK, token length: $($token.Length)" -ForegroundColor Green
} catch {
    Write-Host "LOGIN ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
$headers = @{ Authorization = "Bearer $token" }

# ========== Helper functions ==========
function Test-Api {
    param([string]$name, [string]$url, [string]$method = 'GET', [string]$reqBody = $null)
    $result = [PSCustomObject]@{
        Name = $name
        Url = $url
        Method = $method
        Status = 'SKIP'
        HttpCode = 0
        BizCode = $null
        Message = ''
        Error = ''
    }
    try {
        $params = @{
            Uri = "$baseUrl$url"
            Method = $method
            Headers = $headers
            UseBasicParsing = $true
            TimeoutSec = 8
        }
        if ($reqBody) { $params.Body = $reqBody; $params.ContentType = 'application/json' }
        $resp = Invoke-WebRequest @params
        $result.HttpCode = $resp.StatusCode
        try {
            $obj = $resp.Content | ConvertFrom-Json
            $result.BizCode = $obj.code
            $result.Message = $obj.message
            if ($obj.code -eq 0) { $result.Status = 'PASS' }
            else { $result.Status = 'BIZ_FAIL' }
        } catch {
            $result.Status = 'PARSE_FAIL'
            $result.Error = 'Response is not JSON'
        }
    } catch {
        $result.Status = 'ERR'
        if ($_.ErrorDetails.Message) {
            $result.Error = $_.ErrorDetails.Message.Substring(0, [Math]::Min(200, $_.ErrorDetails.Message.Length))
        } else {
            $result.Error = $_.Exception.Message.Substring(0, [Math]::Min(200, $_.Exception.Message.Length))
        }
    }
    return $result
}

# ========== Test cases (ordered by module) ==========
$tests = @()

# === Workspace ===
$tests += Test-Api 'workspace/home-dashboard' '/v1/auth/me'

# === Product Center ===
$tests += Test-Api 'product/food-list' '/v1/product-center/foods?page=1&pageSize=10'
$tests += Test-Api 'product/category-tree' '/v1/product-center/categories/tree'
$tests += Test-Api 'product/combo-list' '/v1/product-center/combos?page=1&pageSize=10'
$tests += Test-Api 'product/pricing-history' '/v1/product-center/pricing/history?page=1&pageSize=10'
$tests += Test-Api 'product/cost-summary' '/v1/product-center/cost-analysis/summary?startDate=2026-06-01&endDate=2026-06-30'

# === Order Management ===
$tests += Test-Api 'order/query-list' '/v1/orders?page=1&pageSize=10'
$tests += Test-Api 'order/statistics-trends' '/v1/orders/trends'
$tests += Test-Api 'order/refund-list' '/v1/orders/refunds?page=1&pageSize=10'
$tests += Test-Api 'order/reservation-list' '/v1/reservations?page=1&pageSize=10'

# === Operations Center ===
$tests += Test-Api 'operations/dashboard-stats' '/v1/operations/dashboard/stats'
$tests += Test-Api 'operations/live-monitor-overview' '/v1/operations/live-monitor/overview'
$tests += Test-Api 'operations/decision-health' '/v1/operations/decision-board/health-scores'
$tests += Test-Api 'operations/strategy-promotions' '/v1/promotions?page=1&pageSize=10'
$tests += Test-Api 'operations/alert-list' '/v1/alerts?page=1&pageSize=10'
$tests += Test-Api 'operations/reports-kpi' '/v1/operations-reports/kpi-summary?startDate=2026-06-01&endDate=2026-07-05'

# === Store Management ===
$tests += Test-Api 'store/archive-list' '/v1/stores?page=1&pageSize=10'
$tests += Test-Api 'store/pending-tasks' '/v1/store-management/tasks?page=1&pageSize=10'
$tests += Test-Api 'store/daily-settlements' '/v1/store-management/settlements?page=1&pageSize=10'
$tests += Test-Api 'store/certificate-list' '/v1/store/health-certificate/list?page=1&pageSize=10'
$tests += Test-Api 'store/recruitment-approvals' '/v1/store-management/recruitment/approvals?page=1&pageSize=10'
$tests += Test-Api 'store/table-list' '/v1/store-management/tables?page=1&pageSize=10'
$tests += Test-Api 'store/queue-records' '/v1/store-management/queue/records?page=1&pageSize=10'
$tests += Test-Api 'store/shift-plans' '/v1/schedule/plans?page=1&pageSize=10'
$tests += Test-Api 'store/material-request-list' '/v1/purchase/material-requests?page=1&pageSize=10'
$tests += Test-Api 'store/store-inventory-list' '/v1/store-inventory/list?page=1&pageSize=10'

# === Purchase Management ===
$tests += Test-Api 'purchase/orders-list' '/v1/purchase/orders?page=1&pageSize=10'
$tests += Test-Api 'purchase/archive-list' '/v1/purchase/archives?page=1&pageSize=10'
$tests += Test-Api 'purchase/material-category-page' '/v1/material-categories/page?page=1&pageSize=10'
$tests += Test-Api 'purchase/plan-list' '/v1/purchase/plans?page=1&pageSize=10'
$tests += Test-Api 'purchase/stockin-list' '/v1/purchase/stockins?page=1&pageSize=10'
$tests += Test-Api 'purchase/contract-page' '/v1/purchase/contracts/page?page=1&pageSize=10'
$tests += Test-Api 'purchase/electronic-contract-page' '/v1/purchase/electronic-contracts/page?page=1&pageSize=10'
$tests += Test-Api 'purchase/request-page' '/v1/purchase/requests/page?page=1&pageSize=10'
$tests += Test-Api 'purchase/settlement-list' '/v1/purchase/settlements?page=1&pageSize=10'
$tests += Test-Api 'purchase/report-summary' '/v1/purchase/reports/summary'
$tests += Test-Api 'purchase/analysis-summary' '/v1/purchase/analysis/summary'
$tests += Test-Api 'purchase/supplier-page' '/v1/suppliers/page?page=1&pageSize=10'
$tests += Test-Api 'purchase/material-request-page' '/v1/purchase/material-requests?page=1&pageSize=10'

# === Warehouse Management ===
$tests += Test-Api 'warehouse/inventory-list' '/v1/inventory?page=1&pageSize=10'
$tests += Test-Api 'warehouse/low-stock' '/v1/inventory/low-stock'
$tests += Test-Api 'warehouse/inventory-stats-overview' '/v1/inventory/stats/overview'
$tests += Test-Api 'warehouse/warnings-page' '/v1/inventory/warnings/page?page=1&pageSize=10'
$tests += Test-Api 'warehouse/inventory-checks' '/v1/inventory-checks?page=1&pageSize=10'
$tests += Test-Api 'warehouse/transfer-page' '/v1/inventory/transfers/page?page=1&pageSize=10'
$tests += Test-Api 'warehouse/outbound-page' '/v1/inventory/outbounds/page?page=1&pageSize=10'
$tests += Test-Api 'warehouse/adjustment-page' '/v1/inventory/adjustments/page?page=1&pageSize=10'
$tests += Test-Api 'warehouse/loss-page' '/v1/inventory/losses/page?page=1&pageSize=10'
$tests += Test-Api 'warehouse/location-list' '/v1/inventory-locations?page=1&pageSize=10'
$tests += Test-Api 'warehouse/analytics-overview' '/v1/analytics/inventory/overview'
$tests += Test-Api 'warehouse/active-warehouses' '/v1/warehouses/active'

# === Member Management ===
$tests += Test-Api 'member/overview' '/v1/members/stats/overview'
$tests += Test-Api 'member/list' '/v1/members?page=1&pageSize=10'
$tests += Test-Api 'member/level-list' '/v1/member-levels'
$tests += Test-Api 'member/recharge-plans' '/v1/recharge-plans'
$tests += Test-Api 'member/recharge-records' '/v1/recharge-records?page=1&pageSize=10'
$tests += Test-Api 'member/recharge-settings' '/v1/recharge-settings'
$tests += Test-Api 'member/refunds' '/v1/refunds?page=1&pageSize=10'

# === Finance Center ===
$tests += Test-Api 'finance/vouchers' '/v1/finance/vouchers?page=1&pageSize=10'
$tests += Test-Api 'finance/receivables' '/v1/finance/receivables?page=1&pageSize=10'
$tests += Test-Api 'finance/payables' '/v1/finance/payables?page=1&pageSize=10'
$tests += Test-Api 'finance/costs' '/v1/finance/costs?page=1&pageSize=10'
$tests += Test-Api 'finance/budgets' '/v1/finance/budgets?page=1&pageSize=10'
$tests += Test-Api 'finance/bank-accounts' '/v1/finance/bank-accounts?page=1&pageSize=10'
$tests += Test-Api 'finance/tax-rate-configs' '/v1/finance/tax-rate-configs'
$tests += Test-Api 'finance/invoices' '/v1/finance/invoices?page=1&pageSize=10'
$tests += Test-Api 'finance/invoice-reimbursements' '/v1/finance/invoice-reimbursements?page=1&pageSize=10'
$tests += Test-Api 'finance/approval-flow-configs' '/v1/finance/approval-flow-configs'
$tests += Test-Api 'finance/transfer-templates' '/v1/finance/transfer-templates'
$tests += Test-Api 'finance/accounting-periods' '/v1/finance/accounting-periods'
$tests += Test-Api 'finance/subjects' '/v1/finance/subjects'

# === Asset Management ===
$tests += Test-Api 'asset/overview' '/v1/asset/overview'
$tests += Test-Api 'asset/list' '/v1/asset/list?page=1&pageSize=10'
$tests += Test-Api 'asset/categories-page' '/v1/asset/categories/page?page=1&pageSize=10'
$tests += Test-Api 'asset/depreciation-page' '/v1/asset/depreciation/page?page=1&pageSize=10'
$tests += Test-Api 'asset/inventory-page' '/v1/asset/inventory/page?page=1&pageSize=10'
$tests += Test-Api 'asset/maintenance-page' '/v1/asset/maintenance/page?page=1&pageSize=10'
$tests += Test-Api 'asset/disposal-page' '/v1/asset/disposal/page?page=1&pageSize=10'

# === HR Management ===
$tests += Test-Api 'hr/employee-statistics' '/v1/employees/statistics'
$tests += Test-Api 'hr/organization-tree' '/v1/hr/organizations/tree'
$tests += Test-Api 'hr/attendance-page' '/v1/attendance/page?page=1&pageSize=10'
$tests += Test-Api 'hr/salary-batches' '/v1/salary/batches?page=1&pageSize=10'
$tests += Test-Api 'hr/recruitment-requirements' '/v1/recruitment-requirements?page=1&pageSize=10'
$tests += Test-Api 'hr/knowledge-base' '/v1/hr/knowledge-base?page=1&pageSize=10'
$tests += Test-Api 'hr/training' '/v1/hr/training?page=1&pageSize=10'
$tests += Test-Api 'hr/health-certificate-stats' '/v1/hr/health-certificate/statistics'
$tests += Test-Api 'hr/contract-list' '/v1/contracts/list?page=1&size=10'
$tests += Test-Api 'hr/contract-template-list' '/v1/hr/contract-template/list'
$tests += Test-Api 'hr/config-sick-pay' '/v1/hr/config/sick-pay'
$tests += Test-Api 'hr/approvals' '/v1/hr/approvals'
$tests += Test-Api 'hr/analytics-departments' '/v1/hr/analytics/departments'
$tests += Test-Api 'hr/invitation-codes' '/v1/hr/invitation-codes'
$tests += Test-Api 'hr/positions-page' '/v1/positions/page?page=1&pageSize=10'
$tests += Test-Api 'hr/position-levels' '/v1/position-levels'
$tests += Test-Api 'hr/onboarding-archive' '/v1/onboarding/archive/list'
$tests += Test-Api 'hr/departments-page' '/v1/departments/page?page=1&pageSize=10'

# === Traceability ===
$tests += Test-Api 'traceability/trace-codes-page' '/v1/trace-codes/page?page=1&pageSize=10'
$tests += Test-Api 'traceability/material-trace-list' '/v1/material-trace-code/list?page=1&pageSize=10'
$tests += Test-Api 'traceability/food-trace-list' '/v1/food-trace-code/list?page=1&pageSize=10'
$tests += Test-Api 'traceability/expiry-dashboard' '/v1/expiry-alerts/dashboard'
$tests += Test-Api 'traceability/quality-records' '/v1/quality/records?page=1&pageSize=10'
$tests += Test-Api 'traceability/label-template-list' '/v1/label-template/list'
$tests += Test-Api 'traceability/supplier-trace-stats' '/v1/supplier-trace/statistics'
$tests += Test-Api 'traceability/inspections' '/v1/inspections?page=1&pageSize=10'

# === Device Management ===
$tests += Test-Api 'device/page' '/v1/devices/page?page=1&pageSize=10'
$tests += Test-Api 'device/online' '/v1/devices/online'
$tests += Test-Api 'device-alerts/page' '/v1/device-alerts/page?page=1&pageSize=10'
$tests += Test-Api 'device-status-history' '/v1/device-status-history/last-7-days?deviceType=temperature'
$tests += Test-Api 'device-alerts-monitor-stats' '/v1/device-alerts/monitor/statistics'

# === Seal Management ===
$tests += Test-Api 'seal/page' '/v1/seals/page?page=1&pageSize=10'

# === System Management ===
$tests += Test-Api 'system/audit-logs' '/v1/audit-logs?page=1&pageSize=10'
$tests += Test-Api 'system/sys-config' '/v1/sys-config?page=1&pageSize=10'
$tests += Test-Api 'system/roles-list' '/v1/roles/list'
$tests += Test-Api 'system/users-list' '/v1/users/list?page=1&pageSize=10'
$tests += Test-Api 'system/permissions' '/v1/permissions'
$tests += Test-Api 'system/permission-templates' '/v1/permission-templates'
$tests += Test-Api 'system/user-permission-overrides' '/v1/user-permission-overrides?page=1&pageSize=10'
$tests += Test-Api 'system/ai-models' '/v1/ai-models'

# === Personal Center ===
$tests += Test-Api 'personal-center/me' '/v1/auth/me'

# ========== Output ==========
$pass = ($tests | Where-Object { $_.Status -eq 'PASS' }).Count
$fail = ($tests | Where-Object { $_.Status -ne 'PASS' }).Count
$total = $tests.Count

$line = "TOTAL: $total, PASS: $pass, FAIL: $fail"
Write-Host ""
Write-Host "===== SUMMARY =====" -ForegroundColor Yellow
Write-Host $line -ForegroundColor Cyan

# Save detail results
$tests | ForEach-Object {
    "$($_.Status)`t$($_.HttpCode)`t$($_.BizCode)`t$($_.Name)`t$($_.Url)`t$($_.Message)`t$($_.Error)"
} | Out-File -FilePath $outputFile -Encoding utf8 -Force
Write-Host "Detail saved to: $outputFile" -ForegroundColor Green

# Show failed tests
if ($fail -gt 0) {
    Write-Host ""
    Write-Host "===== FAILED TESTS =====" -ForegroundColor Red
    $tests | Where-Object { $_.Status -ne 'PASS' } | ForEach-Object {
        Write-Host "[$($_.Status)] $($_.Name) -> $($_.Url)" -ForegroundColor Yellow
        Write-Host "  HTTP=$($_.HttpCode) BIZ=$($_.BizCode) MSG=$($_.Message)" -ForegroundColor Gray
        if ($_.Error) { Write-Host "  ERR=$($_.Error)" -ForegroundColor Gray }
    }
}
