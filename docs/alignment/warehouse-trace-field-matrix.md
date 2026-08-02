# 仓储/溯源链路字段四维对照表

> 范围：仓储管理（仓库、库存、库位、入库、出库、调拨、盘点、报损、调整、预警、门店库存、智能补货、报表/统计/日志）与溯源管理（追溯码、原料追溯码、食品追溯码、检验记录、质量记录、临期预警、召回、供应商追溯、标签模板）
> 用途：为后续代码改造提供字段命名、类型、状态、金额/ID 转换依据
> 生成日期：2026-07-30

---

## 1. 单据/对象主档字段对照

### 1.1 仓储单据主档

| 业务语义 | 库存调拨 `InventoryTransferInfo` | 库存盘点 `InventoryCheckInfo` | 库存出库 `InventoryOutboundInfo` | 库存报损 `InventoryLossInfo` | 库存调整 `InventoryAdjustInfo` |
|---------|--------------------------------|-----------------------------|--------------------------------|-----------------------------|-------------------------------|
| 单据ID | `transferId` (string) | `checkId` (string) | `outboundId` (string) | `lossId` (string) | `adjustId` (string) |
| 单据编号 | `transferCode` | `checkCode` | `outboundCode` | `lossCode` | `adjustCode` |
| 仓库ID | `fromWarehouseId` / `toWarehouseId` | `warehouseId` | `warehouseId` | `warehouseId` | `warehouseId` |
| 仓库名称 | `fromWarehouseName` / `toWarehouseName` | `warehouseName` | `warehouseName` | `warehouseName` | `warehouseName` |
| 物料ID | `materialId` | —（items 中） | items 中 `materialId` | items 中 `materialId` | items 中 `materialId` |
| 物料名称 | `materialName` | —（items 中） | items 中 `materialName` | items 中 `materialName` | items 中 `materialName` |
| 总数量 | `quantity` | — | `totalQuantity` | —（items 汇总） | `totalAdjustQuantity` |
| 总金额（元） | — | — | `totalAmount` (string) | `totalAmount` (string) | `totalAdjustAmount` (string) |
| 单据状态 | `status`: `TransferStatus` | `checkStatus`: `CheckStatus` | `status`: `OutboundStatus` | `status`: `LossStatus` | `status`: `AdjustStatus` |
| 申请人ID/名称 | `applyUserId` / `applyUserName` | `createUserId` / `createUserName` | `applyUserId` / `applyUserName` | `applyUserId` / `applyUserName` | `applyUserId` / `applyUserName` |
| 审批人ID/名称 | `approveUserId` / `approveUserName` | `approveUserId` / `approveUserName` | `approveUserId` / `approveUserName` | `approveUserId` / `approveUserName` | `approveUserId` / `approveUserName` |
| 申请时间 | `applyTime` | `checkDate` | `applyTime` / `outboundDate` | `applyTime` | `applyTime` |
| 审批时间 | `approveTime` | `approveTime` | `approveTime` | `approveTime` | `approveTime` |
| 完成/执行时间 | `executeTime` | `completeDate` | `completeTime` | `processTime` | `completeTime` |
| 备注 | `remark` | `remark` | `remark` | `remark` | `remark` |
| 创建/更新时间 | `createTime` / `updateTime` | `createTime` / `updateTime` | `createTime` / `updateTime` | `createTime` / `updateTime` | `createTime` / `updateTime` |

### 1.2 溯源对象主档

| 业务语义 | 追溯码 `TraceCodeVO` | 原料追溯码 `MaterialTraceCode` | 食品追溯码 `FoodTraceCode` | 质量记录 `QualityRecord` | 检验记录 `InspectionRecord` | 临期预警 `ExpiryAlert` |
|---------|---------------------|-------------------------------|---------------------------|-------------------------|----------------------------|-----------------------|
| 主键ID | `id` (number) | `id` (number) | `id` (number) | `qualityRecordId` (number) | `inspectionId` (number) | `alertId` (number) |
| 追溯码 | `traceCode` (string) | `traceCode` / `traceCodeId` (string) | `traceCode` / `traceCodeId` (string) | `traceCode` (string) | `traceCode` (string) | `traceCode` (string) |
| 产品/物料ID | `productId` (number) | `productId` (number) | `dishId` (string) | `materialId` (string) | `materialId` (string) | `materialId` (string) |
| 产品/物料名称 | `productName` | `productName` | `dishName` | `materialName` | `materialName` | `materialName` |
| 批次号 | `batchNumber` | `batchNumber` | — | `batchNo` | `batchNo` | `batchNo` |
| 供应商ID | `supplierId` (number) | `supplierId` (number) | — | — | `supplierId` (number) | `supplierId` (number) |
| 供应商名称 | `supplierName` | `supplierName` | — | — | `supplierName` | `supplierName` |
| 状态 | `status` (string) `[待修复：应为 TraceCodeStatus]` | `status`: `MaterialTraceCodeStatus` | `status`: `FoodTraceCodeStatus` / `makeStatus`: `FoodMakeStatus` | `abnormalLevel` / `handlingStatus` | `inspectionResult` / `inspectionType` | `alertLevel` / `handlingStatus` |
| 仓库ID | `warehouseId` (number) | `warehouseId` (number) | `storeId` (number) | — | — | — |
| 仓库/门店名称 | `warehouseName` | `warehouseName` / `storeName` | `storeName` | — | — | — |
| 生产日期 | — | `productionDate` | — | — | — | `productionDate` |
| 过期日期 | — | `expiryDate` | — | — | — | `expiryDate` |
| 单价/金额 | — | `unitPrice` / `totalPrice` (分) | `materialCost` / `laborCost` / `totalCost` (分) | — | — | — |
| 创建/更新时间 | `createdAt` / `updatedAt` | `createTime` / `updateTime` | `createTime` / `updateTime` | `createTime` / `updateTime` | `createTime` / `updateTime` | `createTime` / `updateTime` |

---

## 2. 关键实体字段对照

### 2.1 仓库/库位/库存

| 业务语义 | 仓库 `WarehouseInfo` | 库位 `InventoryLocationInfo` | 库存 `InventoryInfo` | 门店库存 `StoreInventoryInfo` | 库存日志 `InventoryLogInfo` |
|---------|---------------------|-----------------------------|---------------------|------------------------------|---------------------------|
| 主键ID | `warehouseId` (string) | `locationId` (string) | `inventoryId` (string) | `inventoryId` (string) | `id` (string) |
| 编码 | `warehouseCode` | `locationCode` | — | — | — |
| 名称 | `warehouseName` | `locationName` | — | — | — |
| 类型 | `warehouseType`: `main/cold/freeze/normal` | `locationType`: `shelf/floor/cold_storage/freezer` | `inventoryType`: `raw_material/semi_finished/finished/packaging` | — | `operationType`: `purchase_in/sale_out/...` |
| 状态 | `status`: `active/inactive` | `status`: `active/inactive` | `status`: `normal/warning/expired/frozen` | — | — |
| 容量/数量 | `capacity` / `usedCapacity` | `maxCapacity` / `currentQuantity` | `quantity` / `lockedQuantity` / `availableQuantity` | `quantity` / `lockedQuantity` / `availableQuantity` | `beforeStock` / `afterStock` / `changeAmount` |
| 物料ID | — | — | `materialId` | `materialId` | `productId` `[待修复：应为 materialId]` |
| 物料名称 | — | — | `materialName` | `materialName` | `productName` `[待修复：应为 materialName]` |
| 单价/金额 | — | — | `unitCost` / `totalCost` (元字符串) | `unitCost` / `totalCost` (元字符串) | — |
| 仓库ID | — | `warehouseId` | `warehouseId` / `locationId` | `storeId` | `warehouseId` |
| 负责人/操作人 | `managerId` / `managerName` | — | — | — | `operatorId` / `operatorName` |

### 2.2 预警/统计/补货

| 业务语义 | 库存预警记录 `InventoryWarningRecordInfo` | 库存统计 `InventoryStatsOverview` | 分析报表 `InventoryAnalysisReport` | 智能补货 `SuggestionRecord` |
|---------|----------------------------------------|----------------------------------|-----------------------------------|---------------------------|
| 主键ID | `warningId` (string) | — | `reportId` (string) | `suggestionNo` (string) |
| 预警类型 | `warningType`: `low_stock/high_stock/expiring_soon/expired` | — | — | — |
| 物料ID/名称 | `materialId` / `materialName` | — | — | —（items 中） |
| 当前库存/阈值 | `currentStock` / `threshold` | `totalItems` / `totalQuantity` | — | — |
| 状态 | `status` (number: 0/1) `[待修复：与其他模块语义化字符串不一致]` | — | — | `status`: `pending/accepted/modified/rejected/converted` |
| 处理人/时间 | `handler` / `handlerName` / `handleTime` | — | — | — |
| 金额 | — | `totalValue` (元字符串) | `totalValue` (元字符串) | `totalAmount` (number 元) |

---

## 3. 字段不一致点汇总

### 3.1 ID 类型不统一

- `[待修复]` 仓储模块统一使用 **string** 类型 ID（`warehouseId`、`materialId`、`inventoryId` 等），溯源模块核心对象使用 **number** 类型 ID（`TraceCodeVO.id`、`MaterialTraceCode.id`、`FoodTraceCode.id`）。
- `[待修复]` 溯源模块内部也存在混用：`productId` 为 number，但 `dishId` 为 string；`traceCodeId` 为 string，但 `id` 为 number。
- `[待修复]` 库存日志 `InventoryLogInfo` 使用 `productId` / `productName`，与库存模块 `materialId` / `materialName` 语义不一致。

### 3.2 金额单位与类型不统一

- `[待修复]` 仓储库存/出库/报损/调整等单据的金额字段为 **元字符串**（如 `totalAmount: string`），后端 API 中部分接口以分为单位，部分接口（如调整单、出库单）直接返回元字符串，转换逻辑分散在 `converters.ts` 与各 API 文件中。
- `[待修复]` 溯源模块原料追溯码/食品追溯码的成本字段为 **分（number）**，与仓储模块的元字符串不一致，转换集中在 `api/traceability/converters.ts`。
- `[待修复]` 智能补货 `SuggestionRecord.totalAmount` 为 number 元，与库存统计 `InventoryStatsOverview.totalValue` 元字符串不一致。

### 3.3 状态字段表达不统一

- `[待修复]` 仓储模块状态普遍使用**小写语义化字符串**（如 `pending`/`approved`/`completed`），后端映射为数字。
- `[待修复]` 溯源模块中质量/检验/临期预警使用**大写枚举字符串**（如 `PENDING`/`RESOLVED`、`QUALIFIED`/`UNQUALIFIED`），与仓储风格不一致。
- `[待修复]` 库存预警记录 `InventoryWarningRecordInfo.status` 使用 **number（0/1）**，与仓储其他模块的语义化字符串状态不一致。
- `[待修复]` `TraceCodeVO.status` 类型声明为 `string`，但已定义 `TraceCodeStatus = 'active' | 'recalled' | 'expired'`，未使用类型约束。

### 3.4 时间字段命名不统一

- `[待修复]` 仓储模块使用 `createTime` / `updateTime`，溯源模块部分老接口使用 `createdAt` / `updatedAt`（`TraceCodeVO`、`TraceCodeLog`）。
- `[待修复]` 出库单使用 `outboundDate`、`applyTime`、`completeTime` 多个时间字段；盘点单使用 `checkDate`、`completeDate`、`approveTime`，命名规律不一致。

### 3.5 申请人/创建人字段不统一

- `[待修复]` 库存调拨、出库、报损、调整使用 `applyUserId` / `applyUserName`，库存盘点使用 `createUserId` / `createUserName`。
- `[待修复]` 库存日志使用 `operatorId` / `operatorName`，与库存单据申请人字段语义不同。

### 3.6 明细结构不一致

- `[待修复]` 调拨单在 `InventoryTransferInfo` 顶层同时存在 `materialId`/`materialName`/`quantity`（单物料视角），又存在 `items: InventoryTransferItemInfo[]`（多物料明细），存在数据冗余与歧义。
- `[待修复]` 出库单、报损单、调整单的金额/成本在明细中维护，但盘点单明细 `InventoryCheckItemInfo` 的 `diffAmount` 为元字符串，与库存模块其他金额字段一致，但盘点主档无总金额字段。

### 3.7 溯源模块字段问题

- `[待修复]` `MaterialTraceCode` 与 `FoodTraceCode` 中 `materialTraceCodes` / `materialDetails` / `relatedFoodTraceCodes` 等字段为 JSON 字符串，未在前端类型中明确定义结构。
- `[待修复]` `QualityRecord.inspectionData` 与 `InspectionRecord.inspectionItems` 为 JSON 字符串，前端仅做透传，未做结构化解析。
- `[待修复]` `RecallRecord` 状态使用语义化字符串 `'pending' | 'processing' | 'completed' | 'cancelled'`，但 `AffectedTraceCode.status` 使用后端数字（1/2/3/4/5），同一模块状态表达不一致。

---

## 4. 推荐改造方向

1. **统一 ID 类型**：仓储/溯源模块内部建议统一为 string，避免 number/string 混用导致的大数精度与类型比较问题。
2. **统一金额表达**：API 层统一以分为单位，前端展示统一为元字符串，统一使用 `utils/money` 转换。
3. **统一状态风格**：建议仓储/溯源均采用小写语义化字符串，后端数字映射统一由 `converters.ts` 处理。
4. **统一时间字段命名**：新增/更新统一使用 `createTime` / `updateTime`，业务时间使用 `{action}Time` 或 `{action}Date` 并明确语义。
5. **统一物料字段命名**：全链路统一使用 `materialId` / `materialName` / `materialCode`，避免 `productId` / `foodId` / `productName` 等混用。
6. **清理冗余字段**：调拨单移除顶层单物料字段或明确其作为 `items[0]` 的快捷引用；盘点单补充主档总金额/总差异金额字段。
