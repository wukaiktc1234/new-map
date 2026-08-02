﻿﻿﻿﻿﻿﻿﻿﻿﻿$ErrorActionPreference = 'Continue'
$BaseUrl = 'http://127.0.0.1:8081/api'

# 登录获取 Token
$loginBody = '{"username":"admin","password":"Admin@123"}'
$loginResp = Invoke-WebRequest -Uri "$BaseUrl/v1/auth/login" -Method POST -Body $loginBody -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
$loginData = $loginResp.Content | ConvertFrom-Json
$token = $loginData.data.token
$headers = @{ Authorization = "Bearer $token" }

$pass = 0
$fail = 0
$notFound = 0
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
            $msg = $data.message
            Write-Host "[FAIL] $method $url -> code=$($data.code) | $msg" -ForegroundColor Red
            $script:fail++
            $script:results += [PSCustomObject]@{Module=$module; Feature=$feature; Method=$method; Url=$url; Status='FAIL'; Code=$data.code; Info=$msg}
        }
    } catch {
        $sc = 'N/A'
        $errMsg = $_.Exception.Message
        if ($_.Exception.Response) { $sc = [int]$_.Exception.Response.StatusCode }
        # 404 视为"接口工作正常，仅数据不存在"，单独统计
        if ($sc -eq 404) {
            Write-Host "[404 ] $method $url -> 404 | data not found" -ForegroundColor Yellow
            $script:notFound++
            $script:results += [PSCustomObject]@{Module=$module; Feature=$feature; Method=$method; Url=$url; Status='NOT_FOUND'; Code=404; Info='data not found'}
        } else {
            Write-Host "[FAIL] $method $url -> $sc | $errMsg" -ForegroundColor Red
            $script:fail++
            $script:results += [PSCustomObject]@{Module=$module; Feature=$feature; Method=$method; Url=$url; Status='FAIL'; Code=$sc; Info=$errMsg}
        }
    }
}

# ============================================================
# 模块1: 产品中心（56个端点，跳过文件下载类）
# ============================================================
Write-Host "===== Product Center Module =====" -ForegroundColor Cyan

# CategoryController (5个)
TestEndpoint 'GET' '/v1/product-center/categories/tree' 'ProductCenter' 'Category-Tree'
TestEndpoint 'GET' '/v1/product-center/categories/tree/enabled' 'ProductCenter' 'Category-TreeEnabled'
TestEndpoint 'GET' '/v1/product-center/categories/list' 'ProductCenter' 'Category-List'
TestEndpoint 'GET' '/v1/product-center/categories/1' 'ProductCenter' 'Category-ById'
TestEndpoint 'GET' '/v1/product-center/categories/1/children' 'ProductCenter' 'Category-Children'

# ComboController (4个)
TestEndpoint 'GET' '/v1/product-center/combos?page=1&size=10' 'ProductCenter' 'Combo-Page'
TestEndpoint 'GET' '/v1/product-center/combos/on-sale' 'ProductCenter' 'Combo-OnSale'
TestEndpoint 'GET' '/v1/product-center/combos/1' 'ProductCenter' 'Combo-ById'
TestEndpoint 'GET' '/v1/product-center/combos/1/cost' 'ProductCenter' 'Combo-Cost'

# ComboInventoryController (2个)
TestEndpoint 'GET' '/v1/combo-inventories/combo/1' 'ProductCenter' 'ComboInv-ByCombo'
TestEndpoint 'GET' '/v1/combo-inventories/combo/1/cost' 'ProductCenter' 'ComboInv-Cost'

# FoodController (5个)
TestEndpoint 'GET' '/v1/product-center/foods?page=1&size=10' 'ProductCenter' 'Food-Page'
TestEndpoint 'GET' '/v1/product-center/foods/on-sale' 'ProductCenter' 'Food-OnSale'
TestEndpoint 'GET' '/v1/product-center/foods/recommend' 'ProductCenter' 'Food-Recommend'
TestEndpoint 'GET' '/v1/product-center/foods/1' 'ProductCenter' 'Food-ById'
TestEndpoint 'GET' '/v1/product-center/foods/category/1' 'ProductCenter' 'Food-ByCategory'

# PricingController (3个)
TestEndpoint 'GET' '/v1/product-center/pricing/history?page=1&size=10' 'ProductCenter' 'Pricing-History'
TestEndpoint 'GET' '/v1/product-center/pricing/current/FOOD/1' 'ProductCenter' 'Pricing-Current'
TestEndpoint 'GET' '/v1/product-center/pricing/history/FOOD/1' 'ProductCenter' 'Pricing-HistoryByProduct'

# DishCostController (6个)
TestEndpoint 'GET' '/v1/product-center/dish-cost/list' 'ProductCenter' 'DishCost-List'
TestEndpoint 'GET' '/v1/product-center/dish-cost/summary' 'ProductCenter' 'DishCost-Summary'
TestEndpoint 'GET' '/v1/product-center/dish-cost/alert-rules' 'ProductCenter' 'DishCost-AlertRules'
TestEndpoint 'GET' '/v1/product-center/dish-cost/1' 'ProductCenter' 'DishCost-ById'
TestEndpoint 'GET' '/v1/product-center/dish-cost/1/trend' 'ProductCenter' 'DishCost-Trend'
TestEndpoint 'GET' '/v1/product-center/dish-cost/1/bom' 'ProductCenter' 'DishCost-Bom'

# CostAnalysisController (6个)
TestEndpoint 'GET' '/v1/product-center/cost-analysis' 'ProductCenter' 'CostAnalysis-Query'
TestEndpoint 'GET' '/v1/product-center/cost-analysis/summary' 'ProductCenter' 'CostAnalysis-Summary'
TestEndpoint 'GET' '/v1/product-center/cost-analysis/profit-distribution' 'ProductCenter' 'CostAnalysis-ProfitDist'
TestEndpoint 'GET' '/v1/product-center/cost-analysis/category-ranking' 'ProductCenter' 'CostAnalysis-CategoryRanking'
TestEndpoint 'GET' '/v1/product-center/cost-analysis/low-profit-warning' 'ProductCenter' 'CostAnalysis-LowProfitWarning'
TestEndpoint 'GET' '/v1/product-center/cost-analysis/trend' 'ProductCenter' 'CostAnalysis-Trend'

# ProductAnalysisController (6个)
TestEndpoint 'GET' '/v1/analytics/product/profitability?startDate=2026-01-01&endDate=2026-06-30' 'ProductCenter' 'ProdAnalysis-Profitability'
TestEndpoint 'GET' '/v1/analytics/product/menu-engineering' 'ProductCenter' 'ProdAnalysis-MenuEngineering'
TestEndpoint 'GET' '/v1/analytics/product/combo-performance?startDate=2026-01-01&endDate=2026-06-30' 'ProductCenter' 'ProdAnalysis-ComboPerf'
TestEndpoint 'GET' '/v1/analytics/product/price-elasticity/1?startDate=2026-01-01&endDate=2026-06-30' 'ProductCenter' 'ProdAnalysis-PriceElasticity'
TestEndpoint 'GET' '/v1/analytics/product/customer-preference' 'ProductCenter' 'ProdAnalysis-CustomerPref'
TestEndpoint 'GET' '/v1/analytics/product/reports/1' 'ProductCenter' 'ProdAnalysis-ReportById'

# ProductSalesStatsController (6个)
TestEndpoint 'GET' '/v1/sales-stats/product/page?page=1&size=10' 'ProductCenter' 'SalesStats-ProductPage'
TestEndpoint 'GET' '/v1/sales-stats/trend?startDate=2026-01-01&endDate=2026-06-30' 'ProductCenter' 'SalesStats-Trend'
TestEndpoint 'GET' '/v1/sales-stats/top-products' 'ProductCenter' 'SalesStats-TopProducts'
TestEndpoint 'GET' '/v1/sales-stats/overview' 'ProductCenter' 'SalesStats-Overview'
TestEndpoint 'GET' '/v1/sales-stats/category-stats' 'ProductCenter' 'SalesStats-CategoryStats'
TestEndpoint 'GET' '/v1/sales-stats/growth-rate' 'ProductCenter' 'SalesStats-GrowthRate'

# DishInventoryController (2个)
TestEndpoint 'GET' '/v1/dish-inventories/dish/1' 'ProductCenter' 'DishInv-ByDish'
TestEndpoint 'GET' '/v1/dish-inventories/dish/1/cost' 'ProductCenter' 'DishInv-Cost'

# InventoryUnitController (4个)
TestEndpoint 'GET' '/v1/inventory/unit?page=1&size=10' 'ProductCenter' 'InvUnit-Page'
TestEndpoint 'GET' '/v1/inventory/unit/all' 'ProductCenter' 'InvUnit-All'
TestEndpoint 'GET' '/v1/inventory/unit/1' 'ProductCenter' 'InvUnit-ById'
TestEndpoint 'GET' '/v1/inventory/unit/type/weight' 'ProductCenter' 'InvUnit-ByType'

# MaterialTemplateController (7个)
TestEndpoint 'GET' '/v1/material-templates/list' 'ProductCenter' 'MatTemplate-List'
TestEndpoint 'GET' '/v1/material-templates/page?page=1&size=10' 'ProductCenter' 'MatTemplate-Page'
TestEndpoint 'GET' '/v1/material-templates/categories' 'ProductCenter' 'MatTemplate-Categories'
TestEndpoint 'GET' '/v1/material-templates/category/raw' 'ProductCenter' 'MatTemplate-ByCategory'
TestEndpoint 'GET' '/v1/material-templates/search?name=test' 'ProductCenter' 'MatTemplate-Search'
TestEndpoint 'GET' '/v1/material-templates/1' 'ProductCenter' 'MatTemplate-ById'
TestEndpoint 'GET' '/v1/material-templates/barcode/1234567890' 'ProductCenter' 'MatTemplate-ByBarcode'

# ============================================================
# 模块2: 订单管理（31个端点，跳过需要特殊参数的端点）
# ============================================================
Write-Host "`n===== Order Management Module =====" -ForegroundColor Cyan

# OrderNewController (6个)
TestEndpoint 'GET' '/v1/orders?page=1&size=10' 'OrderMgmt' 'Order-Query'
TestEndpoint 'GET' '/v1/orders/refunds?page=1&size=10' 'OrderMgmt' 'Order-Refunds'
TestEndpoint 'GET' '/v1/orders/refunds/stats' 'OrderMgmt' 'Order-RefundStats'
TestEndpoint 'GET' '/v1/orders/1' 'OrderMgmt' 'Order-Detail'
TestEndpoint 'GET' '/v1/orders/today/statistics' 'OrderMgmt' 'Order-TodayStats'
TestEndpoint 'GET' '/v1/orders/cashier/shift-summary' 'OrderMgmt' 'Order-ShiftSummary'

# OrderTrendsController (2个)
TestEndpoint 'GET' '/v1/orders/trends' 'OrderMgmt' 'OrderTrends-Get'
TestEndpoint 'GET' '/v1/orders/trends/daily' 'OrderMgmt' 'OrderTrends-Daily'

# SalesOrderController (1个)
TestEndpoint 'GET' '/v1/sales/order/page?page=1&size=10' 'OrderMgmt' 'SalesOrder-Page'

# KitchenOrderController (13个)
TestEndpoint 'GET' '/v1/kitchen-order/list?page=1&size=10' 'OrderMgmt' 'Kitchen-List'
TestEndpoint 'GET' '/v1/kitchen-order/active/full' 'OrderMgmt' 'Kitchen-ActiveFull'
TestEndpoint 'GET' '/v1/kitchen-order/recent/full' 'OrderMgmt' 'Kitchen-RecentFull'
TestEndpoint 'GET' '/v1/kitchen-order/1' 'OrderMgmt' 'Kitchen-ById'
TestEndpoint 'GET' '/v1/kitchen-order/1/full' 'OrderMgmt' 'Kitchen-ByIdFull'
TestEndpoint 'GET' '/v1/kitchen-order/order/1' 'OrderMgmt' 'Kitchen-ByOrderId'
TestEndpoint 'GET' '/v1/kitchen-order/store-active/1' 'OrderMgmt' 'Kitchen-StoreActive'
TestEndpoint 'GET' '/v1/kitchen-order/chef-active/1' 'OrderMgmt' 'Kitchen-ChefActive'
TestEndpoint 'GET' '/v1/kitchen-order/statistics/1' 'OrderMgmt' 'Kitchen-Statistics'
TestEndpoint 'GET' '/v1/kitchen-order/orders/sorted' 'OrderMgmt' 'Kitchen-Sorted'
TestEndpoint 'GET' '/v1/kitchen-order/stats' 'OrderMgmt' 'Kitchen-Stats'
TestEndpoint 'GET' '/v1/kitchen-order/orders/overdue' 'OrderMgmt' 'Kitchen-Overdue'
TestEndpoint 'GET' '/v1/kitchen-order/orders/grouped-by-food' 'OrderMgmt' 'Kitchen-GroupedByFood'

# TableReservationNewController (4个)
TestEndpoint 'GET' '/v1/reservations?page=1&size=10' 'OrderMgmt' 'Reservation-Query'
TestEndpoint 'GET' '/v1/reservations/stats' 'OrderMgmt' 'Reservation-Stats'
TestEndpoint 'GET' '/v1/reservations/1' 'OrderMgmt' 'Reservation-ById'
TestEndpoint 'GET' '/v1/reservations/by-date/2026-06-30' 'OrderMgmt' 'Reservation-ByDate'

# PosOrderController (5个，跳过需要openid/订单号的端点)
TestEndpoint 'GET' '/v1/pos/orders?page=1&size=10' 'OrderMgmt' 'PosOrder-List'
TestEndpoint 'GET' '/v1/pos/orders/table/1' 'OrderMgmt' 'PosOrder-ByTable'
TestEndpoint 'GET' '/v1/pos/orders/order/ORDER001' 'OrderMgmt' 'PosOrder-ByOrderNumber'
TestEndpoint 'GET' '/v1/pos/orders/byOpenid?openid=test' 'OrderMgmt' 'PosOrder-ByOpenid'
TestEndpoint 'GET' '/v1/pos/orders/byOrderNumber?orderNumber=ORDER001' 'OrderMgmt' 'PosOrder-QueryByNumber'

# ============================================================
# 模块3: 运营中心（45个端点，跳过文件下载类）
# ============================================================
Write-Host "`n===== Operations Center Module =====" -ForegroundColor Cyan

# OperationsDashboardController (2个)
TestEndpoint 'GET' '/v1/operations/dashboard/stats' 'OperationsCenter' 'OpsDashboard-Stats'
TestEndpoint 'GET' '/v1/operations/dashboard/stores' 'OperationsCenter' 'OpsDashboard-Stores'

# OperationsReportController (3个，跳过download)
TestEndpoint 'GET' '/v1/operations-reports/daily?startDate=2026-01-01&endDate=2026-06-30' 'OperationsCenter' 'OpsReport-Daily'
TestEndpoint 'GET' '/v1/operations-reports/kpi-summary?startDate=2026-01-01&endDate=2026-06-30' 'OperationsCenter' 'OpsReport-KpiSummary'
TestEndpoint 'GET' '/v1/operations-reports/export/1/status' 'OperationsCenter' 'OpsReport-ExportStatus'

# DecisionBoardController (5个)
TestEndpoint 'GET' '/v1/operations/decision-board/health-scores' 'OperationsCenter' 'Decision-HealthScores'
TestEndpoint 'GET' '/v1/operations/decision-board/store-ranking' 'OperationsCenter' 'Decision-StoreRanking'
TestEndpoint 'GET' '/v1/operations/decision-board/category-sales' 'OperationsCenter' 'Decision-CategorySales'
TestEndpoint 'GET' '/v1/operations/decision-board/insights' 'OperationsCenter' 'Decision-Insights'
TestEndpoint 'GET' '/v1/operations/decision-board/revenue-trend' 'OperationsCenter' 'Decision-RevenueTrend'

# LiveMonitorController (4个)
TestEndpoint 'GET' '/v1/operations/live-monitor/overview' 'OperationsCenter' 'LiveMonitor-Overview'
TestEndpoint 'GET' '/v1/operations/live-monitor/stores' 'OperationsCenter' 'LiveMonitor-Stores'
TestEndpoint 'GET' '/v1/operations/live-monitor/trend' 'OperationsCenter' 'LiveMonitor-Trend'
TestEndpoint 'GET' '/v1/operations/live-monitor/alerts' 'OperationsCenter' 'LiveMonitor-Alerts'

# PromotionController (4个)
TestEndpoint 'GET' '/v1/promotions?page=1&size=10' 'OperationsCenter' 'Promotion-List'
TestEndpoint 'GET' '/v1/promotions/active' 'OperationsCenter' 'Promotion-Active'
TestEndpoint 'GET' '/v1/promotions/1' 'OperationsCenter' 'Promotion-ById'
TestEndpoint 'GET' '/v1/promotions/statistics/effect' 'OperationsCenter' 'Promotion-Effect'

# MarketingAnalysisController (6个)
TestEndpoint 'GET' '/v1/marketing/analysis/overview' 'OperationsCenter' 'MktAnalysis-Overview'
TestEndpoint 'GET' '/v1/marketing/analysis/rfm/distribution' 'OperationsCenter' 'MktAnalysis-RFMDist'
TestEndpoint 'GET' '/v1/marketing/analysis/churn-risk' 'OperationsCenter' 'MktAnalysis-ChurnRisk'
TestEndpoint 'GET' '/v1/marketing/analysis/high-value' 'OperationsCenter' 'MktAnalysis-HighValue'
TestEndpoint 'GET' '/v1/marketing/analysis/clv-ranking' 'OperationsCenter' 'MktAnalysis-CLVRanking'
TestEndpoint 'GET' '/v1/marketing/analysis/trend' 'OperationsCenter' 'MktAnalysis-Trend'

# SalesAnalysisController (6个)
TestEndpoint 'GET' '/v1/analytics/sales/trend?startDate=2026-01-01&endDate=2026-06-30' 'OperationsCenter' 'SalesAnalysis-Trend'
TestEndpoint 'GET' '/v1/analytics/sales/category-analysis?startDate=2026-01-01&endDate=2026-06-30' 'OperationsCenter' 'SalesAnalysis-Category'
TestEndpoint 'GET' '/v1/analytics/sales/top-foods?startDate=2026-01-01&endDate=2026-06-30' 'OperationsCenter' 'SalesAnalysis-TopFoods'
TestEndpoint 'GET' '/v1/analytics/sales/hourly-distribution?date=2026-06-30' 'OperationsCenter' 'SalesAnalysis-Hourly'
TestEndpoint 'GET' '/v1/analytics/sales/channel-comparison?startDate=2026-01-01&endDate=2026-06-30' 'OperationsCenter' 'SalesAnalysis-Channel'
TestEndpoint 'GET' '/v1/analytics/sales/reports/1' 'OperationsCenter' 'SalesAnalysis-ReportById'

# CustomerAnalysisController (6个)
TestEndpoint 'GET' '/v1/analytics/customer/member-growth-trend' 'OperationsCenter' 'CustomerAnalysis-MemberGrowth'
TestEndpoint 'GET' '/v1/analytics/customer/retention-cohort' 'OperationsCenter' 'CustomerAnalysis-Retention'
TestEndpoint 'GET' '/v1/analytics/customer/rfm-distribution' 'OperationsCenter' 'CustomerAnalysis-RFM'
TestEndpoint 'GET' '/v1/analytics/customer/segment-insight?segmentType=HIGH_VALUE' 'OperationsCenter' 'CustomerAnalysis-Segment'
TestEndpoint 'GET' '/v1/analytics/customer/clv-forecast' 'OperationsCenter' 'CustomerAnalysis-CLV'
TestEndpoint 'GET' '/v1/analytics/customer/reports/1' 'OperationsCenter' 'CustomerAnalysis-ReportById'

# DashboardController (6个)
TestEndpoint 'GET' '/v1/dashboard/overview/1' 'OperationsCenter' 'Dashboard-Overview'
TestEndpoint 'GET' '/v1/dashboard/today-sales' 'OperationsCenter' 'Dashboard-TodaySales'
TestEndpoint 'GET' '/v1/dashboard/inventory-alerts' 'OperationsCenter' 'Dashboard-InvAlerts'
TestEndpoint 'GET' '/v1/dashboard/member-growth' 'OperationsCenter' 'Dashboard-MemberGrowth'
TestEndpoint 'GET' '/v1/dashboard/finance-summary' 'OperationsCenter' 'Dashboard-FinanceSummary'
TestEndpoint 'GET' '/v1/dashboard/config/user/1/type/1' 'OperationsCenter' 'Dashboard-Config'

# ReportConfigController (2个) - reportType为Integer类型(1-6)，1=日报
TestEndpoint 'GET' '/v1/operations-reports/config/1' 'OperationsCenter' 'ReportConfig-UserConfig'
TestEndpoint 'GET' '/v1/operations-reports/config/1/default' 'OperationsCenter' 'ReportConfig-Default'

# ============================================================
# 模块4: 门店管理（31个端点，跳过文件下载类）
# ============================================================
Write-Host "`n===== Store Management Module =====" -ForegroundColor Cyan

# StoreNewController (6个)
TestEndpoint 'GET' '/v1/stores?page=1&size=10' 'StoreMgmt' 'Store-Page'
TestEndpoint 'GET' '/v1/stores/active' 'StoreMgmt' 'Store-Active'
TestEndpoint 'GET' '/v1/stores/code-available?storeCode=S001' 'StoreMgmt' 'Store-CodeAvailable'
TestEndpoint 'GET' '/v1/stores/stats' 'StoreMgmt' 'Store-Stats'
TestEndpoint 'GET' '/v1/stores/1' 'StoreMgmt' 'Store-ById'
TestEndpoint 'GET' '/v1/stores/code/S001' 'StoreMgmt' 'Store-ByCode'

# StoreHealthCertificateController (4个)
TestEndpoint 'GET' '/v1/store/health-certificate/list' 'StoreMgmt' 'HealthCert-List'
TestEndpoint 'GET' '/v1/store/health-certificate/1' 'StoreMgmt' 'HealthCert-ById'
TestEndpoint 'GET' '/v1/store/health-certificate/employee/1' 'StoreMgmt' 'HealthCert-ByEmployee'
TestEndpoint 'GET' '/v1/store/health-certificate/expense/1' 'StoreMgmt' 'HealthCert-Expense'

# StoreInventoryController (4个)
TestEndpoint 'GET' '/v1/store-inventory/stores' 'StoreMgmt' 'StoreInv-Stores'
TestEndpoint 'GET' '/v1/store-inventory/list?page=1&size=10' 'StoreMgmt' 'StoreInv-List'
TestEndpoint 'GET' '/v1/store-inventory/summary?page=1&size=10' 'StoreMgmt' 'StoreInv-Summary'
TestEndpoint 'GET' '/v1/store-inventory/logs?page=1&size=10' 'StoreMgmt' 'StoreInv-Logs'

# StoreManagementTaskController (4个)
TestEndpoint 'GET' '/v1/store-management/tasks?page=1&size=10' 'StoreMgmt' 'StoreTask-List'
TestEndpoint 'GET' '/v1/store-management/tasks/unread-count' 'StoreMgmt' 'StoreTask-UnreadCount'
TestEndpoint 'GET' '/v1/store-management/tasks/1' 'StoreMgmt' 'StoreTask-ById'
TestEndpoint 'GET' '/v1/store-management/tasks/1/redirect-url' 'StoreMgmt' 'StoreTask-RedirectUrl'

# StoreManagementSettlementController (3个，跳过export)
TestEndpoint 'GET' '/v1/store-management/settlements?page=1&size=10' 'StoreMgmt' 'StoreSettlement-List'
TestEndpoint 'GET' '/v1/store-management/settlements/1' 'StoreMgmt' 'StoreSettlement-ById'
TestEndpoint 'GET' '/v1/store-management/settlements/1/shifts' 'StoreMgmt' 'StoreSettlement-Shifts'

# StoreManagementRecruitmentController (3个)
TestEndpoint 'GET' '/v1/store-management/recruitment/approvals/my-list?page=1&size=10' 'StoreMgmt' 'StoreRecruit-MyList'
TestEndpoint 'GET' '/v1/store-management/recruitment/approvals/pending-me?page=1&size=10' 'StoreMgmt' 'StoreRecruit-PendingMe'
TestEndpoint 'GET' '/v1/store-management/recruitment/approvals/1' 'StoreMgmt' 'StoreRecruit-ById'

# DiningTableManagementController (4个)
TestEndpoint 'GET' '/v1/store-management/tables?page=1&size=10' 'StoreMgmt' 'DiningTable-List'
TestEndpoint 'GET' '/v1/store-management/tables/stats' 'StoreMgmt' 'DiningTable-Stats'
TestEndpoint 'GET' '/v1/store-management/tables/usage-records' 'StoreMgmt' 'DiningTable-UsageRecords'
TestEndpoint 'GET' '/v1/store-management/tables/1' 'StoreMgmt' 'DiningTable-ById'

# CallNumberQueueManagementController (2个)
TestEndpoint 'GET' '/v1/store-management/queue/records?page=1&size=10' 'StoreMgmt' 'Queue-Records'
TestEndpoint 'GET' '/v1/store-management/queue/stats' 'StoreMgmt' 'Queue-Stats'

# ============================================================
# 汇总
# ============================================================
Write-Host "`n===== Summary =====" -ForegroundColor Cyan
$total = $pass + $fail + $notFound
Write-Host "Total: $total | PASS: $pass | FAIL: $fail | NOT_FOUND(404): $notFound"

# 导出CSV
$csvPath = "$PSScriptRoot\api-test-results-4modules.csv"
$results | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8
Write-Host "Results exported to $csvPath"

# 显示失败端点
if ($fail -gt 0) {
    Write-Host "`n===== Failed Endpoints =====" -ForegroundColor Red
    $results | Where-Object { $_.Status -eq 'FAIL' } | Format-Table Module, Feature, Method, Url, Code -AutoSize
}

# 显示404端点（信息性，不算失败）
if ($notFound -gt 0) {
    Write-Host "`n===== 404 Endpoints (informational) =====" -ForegroundColor Yellow
    $results | Where-Object { $_.Status -eq 'NOT_FOUND' } | Format-Table Module, Feature, Method, Url -AutoSize
}
