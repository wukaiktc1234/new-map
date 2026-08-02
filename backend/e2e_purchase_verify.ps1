# Purchase E2E verification script (ASCII-safe)
$baseUrl = "http://localhost:8081/api"

function Invoke-Api($method, $path, $body, $token) {
    $h = @{ "Content-Type" = "application/json; charset=utf-8" }
    if ($token) { $h["Authorization"] = "Bearer $token" }
    $uri = "$baseUrl$path"
    Write-Host "`n>>> $method $uri"
    if ($body) { Write-Host "BODY: $body" }
    try {
        if ($method -eq "GET" -or $method -eq "DELETE") {
            $resp = Invoke-RestMethod -Uri $uri -Method $method -Headers $h
        } else {
            # 强制按 UTF-8 发送请求体，避免中文 category 乱码
            $payload = if ($body) { $body } else { "" }
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
            $resp = Invoke-RestMethod -Uri $uri -Method $method -Headers $h -Body $bytes
        }
        $json = $resp | ConvertTo-Json -Depth 5 -Compress
        Write-Host "RESP: $json"
        return $resp
    } catch {
        Write-Host "ERROR: $($_.Exception.Message)"
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errBody = $reader.ReadToEnd()
            Write-Host "ERR BODY: $errBody"
        }
        throw
    }
}

# 1. Login
$loginBody = '{"username":"admin","password":"Admin@123"}'
$loginResp = Invoke-Api "POST" "/v1/auth/login" $loginBody
$token = $loginResp.data.token
Write-Host "`nTOKEN: $token"

# 2. Create supplier (category built from Unicode code points to avoid source encoding issues)
$categoryRaw = [char]0x539f + [char]0x6750 + [char]0x6599
$supplierBody = (@{
    supplierName = "E2E Supplier"
    contactPerson = "John"
    phone = "13800138001"
    address = "E2E Address"
    category = $categoryRaw
} | ConvertTo-Json -Compress)
$supplierResp = Invoke-Api "POST" "/v1/suppliers" $supplierBody $token
$supplierId = $supplierResp.data.supplierId

# 3. Create warehouse
$whCode = "WH-E2E-" + (Get-Date -Format "HHmmss")
$warehouseBody = "{`"warehouseCode`":`"$whCode`",`"warehouseName`":`"E2E Warehouse`",`"warehouseType`":1,`"address`":`"E2E WH Address`",`"phone`":`"13800138002`"}"
$warehouseResp = Invoke-Api "POST" "/v1/warehouses" $warehouseBody $token
$warehouseId = $warehouseResp.data.warehouseId

# 4. Create material archive
$materialBody = '{"materialName":"E2E Potato","unit":"jin","referencePrice":500}'
$materialResp = Invoke-Api "POST" "/v1/purchase/archives" $materialBody $token
$materialId = $materialResp.data.materialId

# 5. Create purchase request
$expectedDate = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
$requestBody = @{
    title = "E2E Purchase Request"
    requestType = "routine"
    priority = "normal"
    expectedDate = $expectedDate
    description = "E2E flow verification"
    items = @(@{
        foodId = "$materialId"
        foodName = "E2E Potato"
        quantity = 10
        unit = "jin"
        estimatedPrice = 5.00
        plannedReceiverType = "WAREHOUSE"
        plannedWarehouseId = [long]$warehouseId
    })
} | ConvertTo-Json -Depth 5 -Compress
$requestResp = Invoke-Api "POST" "/v1/purchase/requests" $requestBody $token
$requestId = $requestResp.data.requestId

# 6. Submit request
Invoke-Api "POST" "/v1/purchase/requests/$requestId/submit" $null $token | Out-Null

# 7. Approve request
$approveBody = '{"status":"approved","remark":"ok"}'
Invoke-Api "POST" "/v1/purchase/requests/$requestId/approve" $approveBody $token | Out-Null

# 8. Generate order (returns request DTO, not order id)
Invoke-Api "POST" "/v1/purchase/requests/$requestId/generate-order" $null $token | Out-Null

# 8.1 Query latest draft order to obtain orderId
$orderListResp = Invoke-Api "GET" "/v1/purchase/orders?current=1&size=1&orderStatus=0" $null $token
$orderId = $orderListResp.data.records[0].orderId
Write-Host "LATEST ORDER ID: $orderId"

# 9. Submit order for approval
Invoke-Api "PUT" "/v1/purchase/orders/$orderId/submit" $null $token | Out-Null

# 10. Approve order
Invoke-Api "PUT" "/v1/purchase/orders/$orderId/approve?approvalRemark=ok" $null $token | Out-Null

# 11. Confirm order
Invoke-Api "PUT" "/v1/purchase/orders/$orderId/confirm" $null $token | Out-Null

# 12. Get order detail
$orderDetailResp = Invoke-Api "GET" "/v1/purchase/orders/$orderId" $null $token
$orderItemId = $orderDetailResp.data.items[0].itemId

# 13. Create stockin
$stockinDate = (Get-Date).ToString("yyyy-MM-dd")
$stockinBody = @{
    orderId = [long]$orderId
    warehouseId = [long]$warehouseId
    stockinDate = $stockinDate
    items = @(@{
        orderItemId = [long]$orderItemId
        materialId = [long]$materialId
        materialName = "E2E Potato"
        actualQuantity = 10
        unit = "jin"
        unitPrice = 500
    })
} | ConvertTo-Json -Depth 5 -Compress
$stockinResp = Invoke-Api "POST" "/v1/purchase/stockins" $stockinBody $token
$stockinId = $stockinResp.data.stockinId

# 14. Quality check passed
$qcBody = '{"qualityCheckResult":1}'
Invoke-Api "PUT" "/v1/purchase/stockins/$stockinId/quality-check" $qcBody $token | Out-Null

# 15. Confirm stockin
Invoke-Api "PUT" "/v1/purchase/stockins/$stockinId/confirm" $null $token | Out-Null

# 16. Query inventory
$inventoryResp = Invoke-Api "GET" "/v1/inventory?materialId=$materialId&warehouseId=$warehouseId" $null $token

Write-Host "`n==================== SUMMARY ===================="
Write-Host "supplierId: $supplierId"
Write-Host "warehouseId: $warehouseId"
Write-Host "materialId: $materialId"
Write-Host "requestId: $requestId"
Write-Host "orderId: $orderId"
Write-Host "stockinId: $stockinId"
Write-Host "inventory quantity: $($inventoryResp.data.records[0].quantity)"
Write-Host "================================================="
