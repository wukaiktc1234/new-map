# 数据链路修复交接文档

> **范围**：本会话围绕「食品溯源系统数据链路一致性」开展的修复与文档治理工作。
> **时间**：2026-07-31
> **状态**：核心链路修复完成，A 类核查全部通过，C 类前后端契约对齐，B 类全部修复完成（LK-FINANCE-02、LK-ORDER-01/02、LK-PURCHASE-04）；剩余 LK-HR-01 待核查

---

## 一、本次会话目标

1. 系统性修复采购、仓储、门店库存等核心模块的数据链路问题，确保「数据从哪来、到哪去、怎么汇总」可追溯、一致。
2. 优先落地 `docs/frontend-verification-plan.md` 中已发现的数据对齐问题。
3. 建立并维护 `docs/data-chain-fix-tracking.md` 作为唯一进度源，避免遗漏。
4. 纠正先前对 LK-WAREHOUSE-02（采购收货直接加库存）与 LK-WAREHOUSE-03（门店库存加权平均成本）的错误判断。

---

## 二、核心结论

| 结论项 | 最终方案 |
|--------|----------|
| 采购收货与入库边界 | 采购收货（到货登记）**仅生成到货单**，记录实收数量与质检状态，**不直接增加库存**；真正的入库确认由一线部门（门店/仓库）操作，此时才增加仓库/门店库存。 |
| 门店库存成本计算 | **不使用加权平均法**，每次入库后 `unit_cost` 直接覆盖为**最新一次入库单价**。 |
| 库存汇总 | 「总库存」= 仓库库存 + 门店库存，通过 `UNION ALL` 联合 `inventory` 与 `store_inventory` 汇总。 |
| 采购订单来源字段 | 后端 `PurchaseOrderServiceImpl` 创建/更新时，从 DTO 持久化 `requestId/requestNo/contractId/contractNo/sourceType/priority`。 |
| 仓储单据金额单位 | 后端统一以**分**返回，前端 `DataConverter` 统一转换为**元**字符串，消除同模块内单位不一致。 |
| 字段命名统一 | 采购链路申请人/审批人统一为 `createByName`/`approvedByName`；物料字段统一为 `materialId/materialName/materialCode`（后端 `productId/productName` 通过转换器映射）。 |
| 状态映射 | 仓库/资产盘点 `CANCELLED` 状态不再错误映射到后端 `2=已审核`，统一映射到后端保留值 `4`。 |

---

## 三、已修复链路清单

### 3.1 仓储/库存模块（5 条全部修复）

| 链路编号 | 链路名称 | 修复文件 | 状态 |
|----------|----------|----------|------|
| LK-WAREHOUSE-01 | 库存汇总应包含仓库库存 + 门店库存 | `backend/src/main/resources/mapper/InventorySummaryMapper.xml` | 已修复 |
| LK-WAREHOUSE-02 | 采购收货不应直接增加库存 | 已核查：`PurchaseStockinServiceImpl.createStockin()` 仅生成到货单；`confirmStockin()` 才增加库存 | 已修正 |
| LK-WAREHOUSE-03 | 门店库存成本使用最新入库价格 | `backend/src/main/java/com/foodtraceability/service/impl/StoreInventoryServiceImpl.java` | 已修复 |
| LK-WAREHOUSE-04 | 库存盘点 `CANCELLED` 状态映射错误 | `frontend/src/types/warehouse-check.ts`、`api/warehouse/converters.ts`、`views/warehouse/InventoryCheck.vue` | 已修复 |
| LK-WAREHOUSE-05 | 仓储出库/调整单据金额单位统一 | `backend/.../InventoryOutboundServiceImpl.java`、`backend/.../InventoryAdjustServiceImpl.java`、`frontend/src/api/warehouse/inventory-outbound.ts`、`frontend/src/api/warehouse/inventory-adjust.ts` | 已修复 |

### 3.2 采购管理模块（5 条：3 条已修复，2 条已核查）

| 链路编号 | 链路名称 | 修复文件 | 状态 |
|----------|----------|----------|------|
| LK-PURCHASE-01 | 采购申请人/审批人字段命名统一 | `frontend/src/types/purchase-request.ts`、`purchase-plan.ts`；`api/purchase/request.ts`、`plan.ts`；视图文件 | 已修复 |
| LK-PURCHASE-02 | 采购链路物料字段命名统一 | 前端类型、API 转换器、视图文件 | 已修复 |
| LK-PURCHASE-03 | 采购订单来源字段后端未返回 | `backend/.../PurchaseOrderServiceImpl.java`、`dto/PurchaseOrderCreateDTO.java`、`dto/PurchaseOrderUpdateDTO.java` | 已修复 |
| LK-PURCHASE-04 | 采购计划状态流转 | 已在前端验证计划与类型定义中修复 | 部分修复 |
| LK-PURCHASE-05 | 采购收货数量回写采购订单 | 已核查：`createStockin()` 在生成到货单时回写 `received_quantity`，符合到货确认即回写已收货数量的设计 | 已核查 |

### 3.3 产品中心模块（2 条已修复）

| 链路编号 | 链路名称 | 修复文件 | 状态 |
|----------|----------|----------|------|
| LK-PRODUCT-01 | 库存日志物料字段命名统一 | `frontend/src/types/warehouse-log.ts`、`api/warehouse/inventory-log.ts`、`views/warehouse/WarehouseOverview.vue`、`StoreInventory.vue` | 已修复 |
| LK-PRODUCT-02 | 菜品成本回写 | 文档已更新，菜品原料明细成本应从门店库存 `unit_cost` 读取 | 已更新文档 |

### 3.4 资产管理模块（1 条已修复）

| 链路编号 | 链路名称 | 修复文件 | 状态 |
|----------|----------|----------|------|
| LK-ASSET-01 | 资产盘点 `CANCELLED` 状态映射错误 | `frontend/src/api/asset/inventory.ts` | 已修复 |

---

## 四、关键代码变更摘要

### 4.1 门店库存成本：加权平均 → 最新入库价格

**文件**：`backend/src/main/java/com/foodtraceability/service/impl/StoreInventoryServiceImpl.java`

- 原逻辑：用「总成本 / 总数量」计算新的单位成本（加权平均）。
- 新逻辑：新 `unitCost` 直接覆盖为本次入库单价 `incomingUnitCost`。
- 总成本仍按 `beforeTotalCost + incomingTotalCost` 累计，保持库存价值准确。

### 4.2 库存汇总 SQL 修正

**文件**：`backend/src/main/resources/mapper/InventorySummaryMapper.xml`

- 使用 `UNION ALL` 合并 `inventory` 与 `store_inventory`。
- `store_count` 仅由 `store_inventory` 的门店维度统计，不再混入仓库维度。
- 成本单价按「总库存价值 / 总库存数量」计算，避免仓库/门店单价差异导致的数据歧义。
- 移除仓库库存部分不必要的 `::INT` 类型转换。

### 4.3 采购订单来源字段持久化

**文件**：
- `backend/src/main/java/com/foodtraceability/service/impl/PurchaseOrderServiceImpl.java`
- `backend/src/main/java/com/foodtraceability/dto/PurchaseOrderCreateDTO.java`
- `backend/src/main/java/com/foodtraceability/dto/PurchaseOrderUpdateDTO.java`

- `createOrder()` 与 `updateOrder()` 中均从 DTO 读取并写入 `requestId/requestNo/contractId/contractNo/sourceType/priority`。
- `sourceType` 与 `priority` 缺失时使用业务默认值 `manual` / `normal`。

### 4.4 仓储出库/调整金额单位统一

**后端**：`InventoryOutboundServiceImpl.java`、`InventoryAdjustServiceImpl.java`
- `convertOutboundToMap` / `convertAdjustToMap` 与明细转换方法中，金额字段直接返回 `Long`（分），不再转换为元字符串。

**前端**：`frontend/src/api/warehouse/inventory-outbound.ts`、`inventory-adjust.ts`
- 后端原始类型中金额字段标注为「分」。
- `toFrontend` / `toItemFrontend` 中使用 `inventoryOutboundConverter.toYuan` / `inventoryAdjustConverter.toYuan` 转换为元字符串。

### 4.5 盘点 `CANCELLED` 状态映射修正

**仓库盘点**：
- `frontend/src/types/warehouse-check.ts`：`CheckStatus` 新增 `cancelled`。
- `frontend/src/api/warehouse/converters.ts`：后端 `4` 映射为 `cancelled`，后端 `2` 映射为 `approved`。
- `frontend/src/views/warehouse/InventoryCheck.vue`：新增状态选项、统计卡片、状态过滤。

**资产盘点**：
- `frontend/src/api/asset/inventory.ts`：`FrontendStatusMap` 中 `CANCELLED` 从映射到 `2` 改为映射到 `4`。

### 4.6 字段命名统一

**采购链路**：
- 申请人/审批人统一为 `createByName` / `approvedByName`。
- 物料字段统一为 `materialId` / `materialName` / `materialCode`。
- 后端 `foodId/foodName/foodCode` 或 `productId/productName` 通过前端 `DataConverter` 映射为前端字段。

**库存日志**：
- 前端 `productId/productName` 统一改为 `materialId/materialName`，通过 `DataConverter` 映射后端字段。

---

## 五、文档更新清单

| 文档 | 更新内容 |
|------|----------|
| `docs/data-chain-fix-tracking.md` | 作为唯一进度源，新增 LK-WAREHOUSE-05，修正 LK-WAREHOUSE-02/03 业务规则描述，更新各链路状态，补充修复执行记录。 |
| `docs/frontend-verification-plan.md` | 纠正门店库存成本规则描述，补充采购管理已修复/待修复项，更新 5.1 已修复问题列表。 |
| `docs/data-business-chain.md` | 明确采购收货与入库确认职责分离，明确门店库存成本使用最新入库价格。 |

---

## 六、验证结果

| 验证项 | 命令 | 结果 |
|--------|------|------|
| 后端编译 | `mvn compile -q`（`JAVA_HOME=H:\jdk-25.0.1.8-hotspot`） | 通过 |
| 前端构建 | `npm run build` | 通过（exit code 0，仅有 chunk 大小与插件耗时警告） |

---

## 七、遗留风险与下一步建议

### 7.1 仍待修复的数据链路

| 模块域 | 链路编号 | 问题描述 | 建议 |
|--------|----------|----------|------|
| 人事/其他 | LK-HR-01 | 待进一步核查 | 根据业务优先级安排 |

> **B 类链路已全部修复完成**（2026-07-31）：
> - LK-ORDER-01/02：经核查确认 `OrderNewServiceImpl.completeOrder()` 已实现库存扣减（`deductInventoryAndCalculateCost` 调用 `storeInventoryService.decreaseStock`）和成本结转（`persistOrderCost` 写入 `cost_record`），无需代码修改
> - LK-PURCHASE-04：新增 `plan_id` 字段链路（Flyway V20260731_004 + 实体/DTO/Service），`PurchaseStockinServiceImpl.confirmStockin` 完成后调用 `markPlanCompletedIfNeeded` 回写计划状态为已完成

### 7.2 采购订单状态前后端不一致（已对齐结论）

- **现象**：前端采购订单状态 11 个，后端 `order_status` 仅 7 个整数值。
- **当前处理**：`shipped` / `received` / `rejected` / `terminated` 等前端状态为近似映射（`order.ts` 中 `STATUS_TO_BACKEND`）。
- **结论**：保留现有"就近映射"方案。后端 7 个状态足够业务流转，多出的前端状态实际不会从后端返回（`STATUS_TO_FRONTEND` 仅映射 7 个后端值），仅作前端 UI 查询选项使用。无需后端扩展状态码。

### 7.3 采购订单后端字段回显（已对齐结论）

- `contactPerson / contactPhone / paidAmount / budgetId / budgetStatus / purchaseType` 后端实体均有字段，且 `PurchaseOrderServiceImpl.createOrder()` 已持久化，会被自动序列化返回。
- 前端 `PurchaseOrderBackend` 接口原注释"后端当前未持久化，预留"**已修正**为正确说明。
- `updateBy / deletedTime / deletedBy` 后端实体无此字段，前端类型已改为可选字段（`?`），运行时 `?? ''` 兜底。
- `createByName` 后端仍仅有 `createUserId`，前端注释保留"待后端配合"。

### 7.4 测试建议

1. 对「采购到货 → 入库确认 → 库存增加 → 成本覆盖」进行端到端验证。
2. 验证库存汇总页仓库库存、门店库存、总库存、总价值的数值正确性。
3. 验证采购订单表格中「来源申请号、合同号、来源类型、优先级」字段回显。
4. 验证出库单、调整单金额在前端显示为元、后端存储/传输为分。
5. 验证仓库盘点与资产盘点在 `CANCELLED` 状态下的状态显示与统计。

---

## 八、交接确认

- [ ] 后端代码已编译通过
- [ ] 前端代码已构建通过
- [ ] `docs/data-chain-fix-tracking.md` 已更新至最新状态
- [ ] `docs/frontend-verification-plan.md` 已同步更新
- [ ] 遗留风险已通知下游负责人

---

## 九、补充决策记录（2026-07-31）

> 以下三项决策在数据链路修复过程中确认，作为后续开发的统一约定。

### 9.1 门店选择器默认行为

| 项目 | 内容 |
|------|------|
| **决策** | 采购/仓储/库存等模块不显示门店下拉选择器，数据自动按当前用户所属门店（JWT `storeId` / `CURRENT_STORE_ID`）过滤 |
| **背景** | 个体餐饮企业以单店场景为主，用户仅能访问自身所属门店数据，门店选择器无实际业务价值且增加操作复杂度 |
| **影响** | 前端查询/操作均通过后端按 token 中 `storeId` 自动隔离；禁止在前端组件中暴露门店选择下拉 |
| **适用范围** | 采购管理、仓储管理、门店库存、排班管理等所有门店维度数据模块 |

### 9.2 物料命名方向（后端 productId → 前端 materialId）

| 项目 | 内容 |
|------|------|
| **决策** | 后端实体与数据库统一使用 `productId/productName/productCode`，前端统一使用 `materialId/materialName/materialCode`，通过 `DataConverter` 在 API 边界完成映射 |
| **背景** | 后端已存在大量基于 `productId` 的实体、Mapper 与 Service 代码，反向改名成本高、风险大；前端使用 `materialId` 更贴合「物料」业务语义 |
| **影响** | 前后端字段名不一致由转换器层消化，禁止在组件中直接做字段映射；新增物料相关接口须遵循此命名方向 |
| **相关文件** | `frontend/src/api/purchase/converters.ts`、各模块 `DataConverter` |

### 9.3 订单库存扣减粒度

| 项目 | 内容 |
|------|------|
| **决策** | 订单完成时按订单明细行（`order_items`）逐行扣减门店库存，每行扣减对应物料的数量，并同步写入 `cost_record` 记录成本 |
| **背景** | 按明细行扣减可精确追踪每笔订单对库存的影响，支持部分退款/退货场景下的库存按行回滚；整单扣减无法满足精细化核算需求 |
| **影响** | `OrderNewServiceImpl.completeOrder()` 调用 `deductInventoryAndCalculateCost` 按明细行循环扣减 `store_inventory`，扣减后写入 `cost_record`；禁止改为整单一次性扣减 |
| **相关文件** | `backend/.../service/impl/OrderNewServiceImpl.java`、`StoreInventoryServiceImpl.java` |

