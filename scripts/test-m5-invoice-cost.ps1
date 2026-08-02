# M5 invoice & cost API verification script
$base = 'http://localhost:8081/api'
$ErrorActionPreference = 'Stop'

function Get-Token($username, $password) {
    $body = @{ username = $username; password = $password } | ConvertTo-Json -Compress
    $resp = Invoke-RestMethod -Uri "$base/v1/auth/login" -Method POST -ContentType 'application/json' -Body $body
    if ($resp.code -ne 0) { throw "login failed $username : $($resp.message)" }
    return $resp.data.token
}

function Invoke-JsonApi($method, $path, $token, $body = $null) {
    $headers = @{ Authorization = "Bearer $token" }
    $uri = "$base$path"
    if ($body) {
        $json = $body | ConvertTo-Json -Compress
        return Invoke-RestMethod -Uri $uri -Method $method -Headers $headers -ContentType 'application/json' -Body $json
    }
    return Invoke-RestMethod -Uri $uri -Method $method -Headers $headers
}

# 1. finance manager login
$tokenC = Get-Token 'emp-c' 'Test@123456'
Write-Host "[1] emp-c login OK, token length $($tokenC.Length)"

# 2. OCR status
$ocrStatus = Invoke-JsonApi 'GET' '/v1/finance/invoices/ocr/status' $tokenC
Write-Host "[2] OCR status available=$($ocrStatus.data.available) engine=$($ocrStatus.data.engineName)"

# 3. OCR simulate (upload dummy file via curl)
$dummyFile = 'P:\my-new-project\scripts\dummy-invoice.txt'
"dummy invoice content" | Set-Content -Path $dummyFile -Encoding UTF8 -Force
$ocrResp = curl.exe -s -X POST "$base/v1/finance/invoices/ocr" -H "Authorization: Bearer $tokenC" -F "file=@$dummyFile" | ConvertFrom-Json
Write-Host "[3] OCR simulate code=$($ocrResp.code) invoiceNo=$($ocrResp.data.invoiceNo) totalAmount=$($ocrResp.data.totalAmount)"

# 4. create invoice
$invoiceNo = 'INV' + (Get-Date -Format 'yyyyMMddHHmmss')
$createResp = Invoke-JsonApi 'POST' '/v1/finance/invoices' $tokenC @{
    invoiceCode = '044002100211'
    invoiceNo = $invoiceNo
    invoiceType = 1
    invoiceCategory = 1
    buyerName = 'Test Company A'
    buyerTaxNo = '91110000123456789X'
    sellerName = 'Fresh Veggie Supplier Ltd'
    sellerTaxNo = '91110000987654321Y'
    totalAmount = 10000
    taxAmount = 1300
    totalAmountWithTax = 11300
    invoiceDate = '2026-07-20'
    receiveDate = '2026-07-21'
}
if ($createResp.code -ne 0) { throw "create invoice failed: $($createResp.message)" }
$invoiceId = $createResp.data.invoiceId
Write-Host "[4] create invoice OK id=$invoiceId no=$invoiceNo status=$($createResp.data.invoiceStatus)"

# 5. issue invoice
$issueResp = Invoke-JsonApi 'POST' "/v1/finance/invoices/$invoiceId/issue" $tokenC
Write-Host "[5] issue invoice status=$($issueResp.data.invoiceStatus)"

# 6. create cost record (food material)
$costResp1 = Invoke-JsonApi 'POST' '/v1/finance/costs' $tokenC @{
    costType = 1
    period = '2026-07'
    costCenterId = 1
    amount = 50000
    quantity = 100
    unitPrice = 500
    calculationMethod = 1
    remark = 'test food material cost'
}
if ($costResp1.code -ne 0) { throw "create cost failed: $($costResp1.message)" }
$costId1 = $costResp1.data.costId
Write-Host "[6] create cost1 OK id=$costId1 no=$($costResp1.data.costNo) amount=$($costResp1.data.amount)"

# 7. create cost record (packaging)
$costResp2 = Invoke-JsonApi 'POST' '/v1/finance/costs' $tokenC @{
    costType = 6
    period = '2026-07'
    costCenterId = 1
    amount = 3000
    calculationMethod = 1
    remark = 'test packaging cost'
}
$costId2 = $costResp2.data.costId
Write-Host "[7] create cost2 OK id=$costId2 no=$($costResp2.data.costNo) amount=$($costResp2.data.amount)"

# 8. cost summary
$summaryResp = Invoke-JsonApi 'GET' '/v1/finance/costs/summary?period=2026-07' $tokenC
Write-Host "[8] cost summary 2026-07:"
$summaryResp.data | Format-List

# 9. admin query inventory
$tokenAdmin = Get-Token 'admin' 'Admin@123'
$invList = Invoke-JsonApi 'GET' '/v1/inventory?page=1&size=5' $tokenAdmin
Write-Host "[9] inventory list (top 5):"
$invList.data.records | Select-Object inventoryId, materialId, warehouseId, batchNo, quantity, unitCost, totalCost | Format-Table

# 10. validate existing inventory cost
$records = $invList.data.records
if ($records -and $records.Count -gt 0) {
    $first = $records[0]
    $expected = [math]::Round($first.unitCost * $first.quantity)
    $actual = $first.totalCost
    Write-Host "[10] existing inventory cost check id=$($first.inventoryId) unitCost=$($first.unitCost) qty=$($first.quantity) expected=$expected actual=$actual match=$(($expected -eq $actual))"
} else {
    Write-Host "[10] no inventory records"
}

# 11. create test inventory increase and validate cost
$incResp = Invoke-JsonApi 'POST' '/v1/inventory/increase' $tokenC @{
    materialId = 22
    warehouseId = 2
    quantity = 15
    unitCost = 800
    transactionType = 1
    batchNo = 'BATCH-M5-20260724'
    referenceNo = 'ST-M5-001'
    referenceType = 'purchase_stockin'
    remark = 'M5 cost verification stockin'
}
Write-Host "[11] test stockin code=$($incResp.code) message=$($incResp.message)"

# 12. re-query test inventory
$newInv = Invoke-JsonApi 'GET' '/v1/inventory?page=1&size=20&warehouseId=2&materialId=22' $tokenAdmin
$testRec = $newInv.data.records | Where-Object { $_.batchNo -eq 'BATCH-M5-20260724' }
if ($testRec) {
    $expected2 = [math]::Round($testRec.unitCost * $testRec.quantity)
    $actual2 = $testRec.totalCost
    Write-Host "[12] test inventory cost check id=$($testRec.inventoryId) batch=$($testRec.batchNo) unitCost=$($testRec.unitCost) qty=$($testRec.quantity) expected=$expected2 actual=$actual2 match=$(($expected2 -eq $actual2))"
}

Write-Host "`nverification done."
