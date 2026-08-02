# UI Acceptance API Test - Simulates frontend API calls
# Tests 4 target modules: Dashboard, Product Center, Certificate, Personal Center

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$BASE_URL = "http://localhost:8081/api"
$TestResults = [System.Collections.ArrayList]::new()

function Add-Result {
    param($Module, $Step, $Status, $Detail, $ResponseBody)
    $result = [PSCustomObject]@{
        Module = $Module
        Step   = $Step
        Status = $Status
        Detail = $Detail
        Time   = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
        Response = if ($ResponseBody -and $ResponseBody.Length -gt 500) { $ResponseBody.Substring(0, 500) + "..." } else { $ResponseBody }
    }
    [void]$script:TestResults.Add($result)
    $icon = if ($Status -eq "PASS") { "[PASS]" } elseif ($Status -eq "FAIL") { "[FAIL]" } else { "[SKIP]" }
    $color = if ($Status -eq "PASS") { "Green" } elseif ($Status -eq "FAIL") { "Red" } else { "Yellow" }
    Write-Host "$icon $Module - $Step : $Detail" -ForegroundColor $color
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
        }
        if ($obj.PSObject.Properties.Name -contains $FieldName) { return $obj.$FieldName }
        return $null
    } catch { return $null }
}

# Check business code: code=0 means success, non-zero means business error
# Note: HTTP 200 but code=500 is a hidden business error, must check both
function Get-BusinessCode {
    param($JsonStr)
    if (-not $JsonStr) { return $null }
    try {
        $obj = $JsonStr | ConvertFrom-Json -ErrorAction Stop
        return $obj.code
    } catch { return $null }
}

# Comprehensive success check: HTTP 200 AND business code=0
function Test-ApiSuccess {
    param($Response)
    if ($null -eq $Response -or $Response.Status -ne 200) { return $false }
    $code = Get-BusinessCode $Response.Body
    return ($code -eq 0)
}

Write-Host "============================================================="
Write-Host "UI Acceptance API Test Started - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host "Backend: $BASE_URL"
Write-Host "============================================================="
Write-Host ""

# ============================================
# Login first
# ============================================
$resp = Invoke-Api -Method Post -Path "/v1/auth/login" -Body '{"username":"admin","password":"Admin@123"}'
$token = Get-Field $resp.Body "token"
if (-not $token) {
    Write-Host "Login failed, cannot continue" -ForegroundColor Red
    exit 1
}
Write-Host "Login OK, token acquired" -ForegroundColor Green
Write-Host ""

# ============================================
# Module 1: Dashboard
# ============================================
Write-Host ">>> Module 1: Dashboard" -ForegroundColor Cyan

# Frontend dashboard/index.vue uses static data, but backend has /v1/dashboard/overview/{type}
# Test dashboard overview endpoint (type=1 for general overview)
$resp = Invoke-Api -Method Get -Path "/v1/dashboard/overview/1" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "1-Dashboard" "1.1 Overview Type=1" "PASS" "Dashboard overview accessible"
} else {
    Add-Result "1-Dashboard" "1.1 Overview Type=1" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# Test today-sales
$resp = Invoke-Api -Method Get -Path "/v1/dashboard/today-sales" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "1-Dashboard" "1.2 Today Sales" "PASS" "Today sales accessible"
} else {
    Add-Result "1-Dashboard" "1.2 Today Sales" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# Test inventory-alerts
$resp = Invoke-Api -Method Get -Path "/v1/dashboard/inventory-alerts" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "1-Dashboard" "1.3 Inventory Alerts" "PASS" "Inventory alerts accessible"
} else {
    Add-Result "1-Dashboard" "1.3 Inventory Alerts" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# Test finance-summary
$resp = Invoke-Api -Method Get -Path "/v1/dashboard/finance-summary" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "1-Dashboard" "1.4 Finance Summary" "PASS" "Finance summary accessible"
} else {
    Add-Result "1-Dashboard" "1.4 Finance Summary" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# ============================================
# Module 2: Product Center - Food List
# ============================================
Write-Host ""
Write-Host ">>> Module 2: Product Center - Food" -ForegroundColor Cyan

# Test food list
$resp = Invoke-Api -Method Get -Path "/v1/product-center/foods?page=1&pageSize=10" -Token $token
if (Test-ApiSuccess $resp) {
    $total = Get-Field $resp.Body "total"
    Add-Result "2-ProductFood" "2.1 Food List" "PASS" "total=$total"
} else {
    Add-Result "2-ProductFood" "2.1 Food List" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# Test food categories (for new dialog dropdown) - correct endpoint is /list (flat list)
$resp = Invoke-Api -Method Get -Path "/v1/product-center/categories/list" -Token $token
if (Test-ApiSuccess $resp) {
    $catCount = (Get-Field $resp.Body "records").Count
    if (-not $catCount) {
        # /list returns a List directly under data, not paginated
        try { $catCount = (($resp.Body | ConvertFrom-Json).data | Measure-Object).Count } catch { $catCount = "?" }
    }
    Add-Result "2-ProductFood" "2.2 Categories (for new dialog)" "PASS" "categories count=$catCount"
} else {
    Add-Result "2-ProductFood" "2.2 Categories (for new dialog)" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# Test on-sale foods
$resp = Invoke-Api -Method Get -Path "/v1/product-center/foods/on-sale" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "2-ProductFood" "2.3 On-Sale Foods" "PASS" "On-sale accessible"
} else {
    Add-Result "2-ProductFood" "2.3 On-Sale Foods" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# ============================================
# Module 3: Certificate Management
# ============================================
Write-Host ""
Write-Host ">>> Module 3: Certificate Management" -ForegroundColor Cyan

# Test certificate list
$resp = Invoke-Api -Method Get -Path "/v1/store/health-certificate/list?page=1&size=10" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "3-Certificate" "3.1 Certificate List" "PASS" "List accessible"
} else {
    Add-Result "3-Certificate" "3.1 Certificate List" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# Test with status filter
$resp = Invoke-Api -Method Get -Path "/v1/store/health-certificate/list?page=1&size=10&status=valid" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "3-Certificate" "3.2 List with status=valid" "PASS" "Filter accessible"
} else {
    Add-Result "3-Certificate" "3.2 List with status=valid" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# Test expense list
$resp = Invoke-Api -Method Get -Path "/v1/store/health-certificate/expense/all?page=1&size=10" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "3-Certificate" "3.3 Expense List" "PASS" "Expense list accessible"
} else {
    Add-Result "3-Certificate" "3.3 Expense List" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# ============================================
# Module 4: Personal Center
# ============================================
Write-Host ""
Write-Host ">>> Module 4: Personal Center" -ForegroundColor Cyan

# Test /v1/auth/me
$resp = Invoke-Api -Method Get -Path "/v1/auth/me" -Token $token
if (Test-ApiSuccess $resp) {
    $username = Get-Field $resp.Body "username"
    $fullName = Get-Field $resp.Body "fullName"
    if (-not $fullName) { $fullName = Get-Field $resp.Body "name" }
    Add-Result "4-Personal" "4.1 Get Current User (/me)" "PASS" "username=$username, name=$fullName"
} else {
    Add-Result "4-Personal" "4.1 Get Current User (/me)" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# Test /v1/auth/user-info
$resp = Invoke-Api -Method Get -Path "/v1/auth/user-info" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "4-Personal" "4.2 Get User Info (/user-info)" "PASS" "User info accessible"
} else {
    Add-Result "4-Personal" "4.2 Get User Info (/user-info)" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# Test menus
$resp = Invoke-Api -Method Get -Path "/v1/auth/menus" -Token $token
if (Test-ApiSuccess $resp) {
    Add-Result "4-Personal" "4.3 Get Menus" "PASS" "Menus accessible"
} else {
    Add-Result "4-Personal" "4.3 Get Menus" "FAIL" "status=$($resp.Status) body=$($resp.Body)"
}

# ============================================
# Summary
# ============================================
Write-Host ""
Write-Host "============================================================="
Write-Host "Test Results Summary"
Write-Host "============================================================="
$pass = ($TestResults | Where-Object { $_.Status -eq "PASS" }).Count
$fail = ($TestResults | Where-Object { $_.Status -eq "FAIL" }).Count
$skip = ($TestResults | Where-Object { $_.Status -eq "SKIP" }).Count
$total = $TestResults.Count
Write-Host "Total: $total"
Write-Host "Pass:  $pass"
Write-Host "Fail:  $fail"
Write-Host "Skip:  $skip"
Write-Host ""

if ($fail -gt 0) {
    Write-Host "Failed Items:" -ForegroundColor Red
    $TestResults | Where-Object { $_.Status -eq "FAIL" } | ForEach-Object {
        Write-Host "  $($_.Module) - $($_.Step)" -ForegroundColor Red
        Write-Host "    $($_.Detail)" -ForegroundColor Gray
        if ($_.Response) { Write-Host "    Response: $($_.Response)" -ForegroundColor DarkGray }
    }
}

# Export results
$TestResults | Export-Csv -Path "p:\my-new-project\docs\ui-acceptance-results.csv" -NoTypeInformation -Encoding UTF8
$TestResults | ConvertTo-Json -Depth 5 | Out-File -FilePath "p:\my-new-project\docs\ui-acceptance-results.json" -Encoding UTF8
Write-Host ""
Write-Host "Results exported to: p:\my-new-project\docs\ui-acceptance-results.csv"
Write-Host "Results exported to: p:\my-new-project\docs\ui-acceptance-results.json"
