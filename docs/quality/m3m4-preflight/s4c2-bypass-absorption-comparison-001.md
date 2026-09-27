# S4c-2 旁路收编对照表（bypass-absorption-comparison-001）

- **日期**：2026-09-28 · 性质：S4c-2 开工前置，**Owner 过目后才动工**
- **差异分类口径**：被迫改 = 新表下原逻辑必然失效；现状迁移 = 行为原样保留（宪法 §四.6）；顺手修 = 顺带修复缺陷（需 Owner 知情，可否决）

| # | 旁路 | 现状 file:line | 现状逻辑 | 收编后逻辑 | 差异分类 | 处理 |
|---|---|---|---|---|---|---|
| 1 | **SalesOrderServiceImpl.deductInventoryForOrder** | `:257-281`（eq `Inventory::getProductId`+`getStoreId` transient 污染列 :270-271；直接 `inventoryMapper.updateById`） | 按订单扣仓库账；**不足时静默钳 0**（:278-280）；**不写任何流水**；乐观锁无重试（:281） | 改调 `inventoryService.decreaseStockAtLocation(locationId, materialId, qty, "OUT", sourceRef)`，locationId 经 `order.getStoreId()` → `resolveLocationIdByStoreId` 解析 | **被迫改**（product_id/store_id 污染列在新表不存在；规则 4）+ ⚠️ **含 2 个未批复行为点** | 分两段：(a) 污染列与直写 = 被迫改，必须收编；(b) **静默钳 0 与"不写流水"是否保留** = 待 Owner 表态（建议追加到确认单二）。未批复前此文件**不动** |
| 2 | **LossOutboundService.updateInventory** | `:122-151`（eq `Inventory::getProductId` 污染列 :127-128；INSERT/UPDATE 直写；无重试） | 报损出库直接改仓库账 | 改经 `inventoryService.increase/decreaseInventory`（既有服务入口，携带重试与流水），查询键 product_id→material_id | **被迫改**（污染列新表不存在） | 收编进服务入口 = 自然获得乐观锁重试与 T4a 流水（行为增强：报损开始留痕——属矩阵 §6-3 强一致问题的反向，建议在确认单二 Q1 批复时一并表态）。若 Owner 要求严格现状，需加"不写流水"开关——不推荐 |
| 3 | **OtherInboundService.updateInventory** | `:96-126`（eq materialId+warehouseId 直写；无重试；`:123` `intValue()` **精度截断**） | 其他入库直改仓库账 | 改经 `inventoryService.increaseInventory` | **被迫改 + 顺手修**：精度截断是缺陷（0.5 斤会记成 0），收编后自然修复（数量精度 NUMERIC(14,4)） | 收编；截断修复作为行为变化登记（影响：其他入库数量从 int 变 decimal——建议接受） |
| 4 | **HardwareDeviceServiceImpl** | `:60 getById`、`:81 inventoryService.updateById` | 硬件侧低频库存更新，已走服务但无重试 | **不收编**：统一表上 `getById`+`updateById`（@Version）语义不变 | **现状迁移** | 仅确认列兼容（新表含 version/status），无代码改动（除非 S8 编译暴露） |
| 5 | **config/InventoryWarningScheduler** | `:84/:102/:128/:148-164`（低/高/临期/过期四扫描；`:164` 过期写 status=3 直 updateById 无重试） | 调度器按 min_safe_qty 等阈值扫描 T2 并回写状态 | **不收编直写**：四扫描 SQL 改键（current_stock→quantity 等）后语义不变；status=3 回写保留直写（批量打标场景，重试价值低） | **现状迁移**（阈值列跟随统一表；min_safe_qty/safety_stock 双列并存裁定已登记） | S7 中改扫描 SQL 的列名/表名；status 回写保留；登记"调度器直写无重试"为已知限制 |

## 特别标注（Owner 三问的预答，待表态）

| 问题 | 现状 | 收编默认（现状迁移） | 建议 |
|---|---|---|---|
| 乐观锁旁路是否加重试 | #1 无重试、#2/#3 无重试、#4/#5 无重试 | #2/#3 收编进服务**自然获得重试**（行为增强，登记）；#1 待 Q；#4/#5 保持无重试 | #2/#3 接受增强；#1 由 Q（静默钳 0）一并定 |
| 静默钳 0 是否保留 | #1 保留（不足钳 0、不抛不记流水） | 未批复 → 不动 #1 文件 | **建议追加确认单二 Q5**："SalesOrder 扣减不足时：A 保留静默钳 0 / B 改抛 INVENTORY_INSUFFICIENT（订单链感知缺货）" |
| int 截断如何处理 | #3 `intValue()` 截断小数 | 收编后自然修复为 decimal | 接受修复（登记行为变化） |

## 附：第 5 处同族冒充（新增发现，未动）

`PurchaseArrivalServiceImpl:554-555`：`dto.setWarehouseId/setStoreId(String.valueOf(arrival.getWarehouseId()))` —— 到货确认给收货确认链的 DTO 双维度仍用 warehouseId 冒充，且 ReceiptConfirmation 走 `arrival.getStoreId()`（receiverType=STORE 分支）时用的是**另一字段**。属 R-04"双维度挑错用"根源的一部分，建议列入 M5（单据表 location_id 切换）一并处理，本批不动。

---

**停点**：本表交 Owner 过目。批复前 S4c-2 不动工；#1 文件在静默钳 0 问题表态前保持现状。


---

## 执行结果（2026-09-28，Owner 批复后）

| # | 批复/结果 |
|---|---|
| 1 | SalesOrder：**Q5=B 已执行**——扣减不足改抛 INVENTORY_INSUFFICIENT（订单链感知缺货），静默钳 0 与"不写流水"废止；另：明细 productId 为 varchar（T-7），非数值时报 PARAM_ERROR（禁止静默跳过）；未分配门店/未映射位置显式拒绝。SalesOrderServiceImplTest 7/7 绿（该测试未覆盖扣减路径，Q5=B 的行为变化由 S8 测试重锚补用例） |
| 2 | LossOutbound：已收编——经 decrease/increaseStockAtLocation，污染列 product_id 查询与直写旁路移除，自然获得乐观锁重试与流水（行为增强已登记） |
| 3 | OtherInbound：已收编——intValue() 精度截断随直写旁路移除而修复（数量精度 NUMERIC(14,4)） |
| 4 | HardwareDevice：未动（现状迁移，统一表列兼容） |
| 5 | WarningScheduler：未动（S7 中改扫描 SQL 列名） |
| 附 | 第 5 处冒充（PurchaseArrival:554-555）随 M5（Owner 指令） |
| 验证 | 编译 BUILD SUCCESS；四类测试 29/29 绿 |
