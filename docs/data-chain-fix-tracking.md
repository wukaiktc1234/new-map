# 数据链路修复跟踪表

> 目标：系统性梳理并修复食品溯源系统中各模块的数据来源、汇总、消费链路，确保"数据从哪来、到哪去、怎么汇总"清晰可追溯。
> 规则：每修复一条数据链路，必须同步更新本文档状态，并更新相关设计/验证文档。

---

## 一、修复状态总览

| 模块域 | 链路数量 | 已修复 | 待修复 | 文档更新 |
|--------|---------|--------|--------|----------|
| 仓储/库存 | 5 | 5 | 0 | 已更新 |
| 产品中心 | 2 | 2 | 0 | 已更新 |
| 采购管理 | 5 | 5 | 0 | 已更新 |
| 订单/销售 | 2 | 2 | 0 | 已更新 |
| 财务中心 | 2 | 1 | 1 | 已更新 |
| 人事/其他 | 1 | 0 | 1 | 待更新 |
| 资产管理 | 1 | 1 | 0 | 已更新 |

---

## 二、按模块链路明细

### 2.1 仓储/库存链路

#### LK-WAREHOUSE-01：库存汇总应包含仓库库存 + 门店库存
- **业务规则**：仓储管理 / 库存汇总中的「总库存」= 仓库库存 + 门店库存；「门店库存」应从 `store_inventory` 汇总。
- **当前实现**：`InventorySummaryMapper.xml` 已使用 `UNION ALL` 联合 `inventory` 与 `store_inventory` 汇总，按 `material_id` 分组计算仓库库存、门店库存、总库存、总库存价值；`store_count` 仅由 `store_inventory` 的门店维度统计；成本单价按「总库存价值 / 总库存数量」计算。
- **问题影响**：仓储总览看不到真实门店库存，总库存小于实际库存。
- **修复文件**：
  - `backend/src/main/resources/mapper/InventorySummaryMapper.xml`
- **依赖链路**：LK-WAREHOUSE-03（门店库存成本准确性）
- **状态**：已修复
- **文档更新**：`data-business-chain.md` 第 2.1 节

#### LK-WAREHOUSE-02：采购收货与入库确认职责分离
- **业务规则**：采购收货（到货登记）仅生成「到货单」，记录实收数量、质检状态，**不直接增加库存**；真正的入库确认由一线部门（门店/仓库）在收到实体货物后操作，此时才增加仓库/门店库存。谁最终收到实体货物，谁确认入库。
- **当前实现**：`PurchaseStockinServiceImpl.confirmStockin()` 生成到货单并回写采购订单实收数量，**不调用库存增加**；入库确认由仓库/门店在「库存入库」或「门店收货」流程中独立完成。
- **问题影响**：此前文档中误认为采购收货应同时增加库存，已纠正；需确保代码与文档一致。
- **修复文件**：
  - `backend/src/main/java/com/foodtraceability/service/purchase/impl/PurchaseStockinServiceImpl.java`（已核查，未直接增加库存）
  - `docs/data-business-chain.md` 第 2.1 节（已更新）
- **状态**：已修正

#### LK-WAREHOUSE-03：门店库存成本使用最新入库价格
- **业务规则**：门店库存单位成本采用**最新一次入库价格**，不采用加权平均法。每次入库后，`unit_cost` 直接覆盖为本次入库单价。
- **当前实现**：`StoreInventoryServiceImpl.increaseStock()` 已修正为最新入库价格法：新 `unitCost` 直接覆盖当前单位成本，不再按加权平均计算；总成本仍按「最新单价 × 当前库存」累计。
- **问题影响**：原代码残留加权平均法计算，导致门店库存成本不是最新入库价格；相关文档已同步更新。
- **修复文件**：
  - `backend/src/main/java/com/foodtraceability/service/impl/StoreInventoryServiceImpl.java`
  - `docs/data-business-chain.md` 第 2.2 节、第 7.1 节（已更新）
  - `docs/frontend-verification-plan.md` 第 10.1 节（已更新）
- **状态**：已修复

#### LK-WAREHOUSE-04：库存盘点 CANCELLED 状态正确映射
- **业务规则**：库存盘点单取消时，前端状态 `cancelled` 应映射到后端保留值 4，不得错误映射到 `2=已审核`。
- **当前实现**：`frontend/src/api/warehouse/converters.ts` 中已新增 `cancelled` 状态映射；`InventoryCheck.vue` 中新增状态选项和统计卡片。
- **问题影响**：取消盘点单后，状态被错误标记为「已审核」，导致状态机混乱。
- **修复文件**：
  - `frontend/src/types/warehouse-check.ts`
  - `frontend/src/api/warehouse/converters.ts`
  - `frontend/src/views/warehouse/InventoryCheck.vue`
  - `docs/frontend-verification-plan.md`
- **状态**：已修复

#### LK-WAREHOUSE-05：仓储出库/调整单据金额单位统一
- **业务规则**：仓储管理各单据（库存、出库、报损、调整）的金额字段应遵循项目统一规范：后端存储/传输以分为单位，前端展示以元为单位。
- **当前实现**：`InventoryOutboundServiceImpl` 与 `InventoryAdjustServiceImpl` 在返回前将金额 `分→元` 转换为字符串，而库存、报损等接口直接返回分由前端转换，导致同一模块内后端返回单位不一致。
- **问题影响**：API 契约不统一，新增接口/消费端容易产生单位误解和显示错误。
- **修复文件**：
  - `backend/src/main/java/com/foodtraceability/service/impl/InventoryOutboundServiceImpl.java`
  - `backend/src/main/java/com/foodtraceability/service/impl/InventoryAdjustServiceImpl.java`
  - `frontend/src/api/warehouse/inventory-outbound.ts`
  - `frontend/src/api/warehouse/inventory-adjust.ts`
- **状态**：已修复

---

### 2.2 产品中心链路

#### LK-PRODUCT-01：菜品原料明细应从门店库存取成本单价
- **业务规则**：产品中心 / 菜品管理 / 新增菜品时，「原料明细」的原料名称来自门店库存表格，选择后自动带出 `unitCost`（元/单位）。
- **当前实现**：`FoodManagement.vue` 已从 `storeInventoryApi` 取门店库存原料，使用 `unitCost`（后端 `unit_cost` 分 → 前端元字符串）；`api/store-ops/store-inventory.ts` 中新增 `toStoreInventoryInfo` 转换器，负责字段映射与金额单位转换。
- **问题影响**：菜品成本使用参考价而非实际库存成本，导致成本/毛利失真。
- **修复文件**：
  - `frontend/src/views/product/FoodManagement.vue`
  - `frontend/src/api/store-ops/store-inventory.ts`
- **依赖链路**：LK-WAREHOUSE-03（门店库存成本准确性）
- **状态**：已修复
- **文档更新**：`frontend-verification-plan.md` 第 10.1 节

#### LK-PRODUCT-02：菜品成本应回写到菜品表
- **业务规则**：原料明细变更后，菜品成本价 `costPrice` 应作为菜品基础数据保存，同时可被成本分析模块读取。
- **当前实现**：已核查 `FoodServiceImpl.createFood()` / `updateFood()` 中均调用 `food.setCostPrice(dto.getCostPrice())` 持久化；保存 Recipe 明细后还会调用 `food.setCostPrice(totalCostFen)` 自动重算（按明细累计）。`FoodVO` 也正确返回 `costPrice`。
- **状态**：已核查正常

---

### 2.3 采购管理链路

#### LK-PURCHASE-01：申请人/审批人字段命名统一
- **业务规则**：采购链路所有单据的申请人字段统一为 `createByName`，审批人统一为 `approvedByName`。
- **当前实现**：采购申请/物资需求已统一为 `createByName`/`approvedByName`；采购计划通过 converter 将后端 `approvedByName`/`approveTime` 映射为前端 `approvedByName`/`approvedTime`。
- **问题影响**：前端类型、表格、对话框字段命名混乱，增加维护成本。
- **修复文件**：
  - `frontend/src/types/purchase-request.ts`（移除重复 createBy，统一 createByName/approvedByName）
  - `frontend/src/types/purchase-plan.ts`（approvedByName/approvedTime）
  - `frontend/src/api/purchase/request.ts` / `plan.ts` / `material-request.ts`（converter 映射）
  - `frontend/src/views/purchase/PurchaseRequest.vue` / `MaterialRequest.vue` / `PurchasePlan.vue`
- **状态**：已修复

#### LK-PURCHASE-02：物料 ID/名称字段命名统一
- **业务规则**：采购链路所有单据明细统一使用 `materialId`/`materialName`/`materialCode`。
- **当前实现**：采购申请前端类型、表单、详情已统一为 `materialId/materialName/materialCode`；API converter 在前后端边界处将后端 `foodId/foodName/foodCode` 映射为前端 `materialId/materialName/materialCode`。采购计划/到货已使用 `materialId/materialName`。采购订单因后端实体使用 `productId/productName`，暂保留，待后续评估是否统一。
- **问题影响**：同一概念多个名字，转单、汇总、追溯时容易出错。
- **修复文件**：
  - `frontend/src/types/purchase-request.ts`
  - `frontend/src/api/purchase/request.ts`（converter 双向映射）
  - `frontend/src/views/purchase/PurchaseRequest.vue`（表单、详情、表格列）
- **状态**：已修复（采购申请）/ 部分修复（采购订单仍用 productId/productName）

#### LK-PURCHASE-03：采购订单来源字段后端未返回
- **业务规则**：采购订单表格展示的来源采购申请号、合同号、优先级等字段，后端应真实返回。
- **当前实现**：`PurchaseOrder` 实体已包含 `requestId`/`requestNo`/`contractId`/`contractNo`/`sourceType`/`priority` 字段，但 `PurchaseOrderServiceImpl.createOrder()` 原硬编码 `sourceType='manual'`/`priority='normal'`，且未写入 request/contract 字段；`updateOrder()` 亦未更新这些字段。本次修复后，创建/更新时均从 DTO 取值并持久化。
- **问题影响**：表格显示为空，用户无法追溯订单来源。
- **修复文件**：
  - `backend/src/main/java/com/foodtraceability/entity/PurchaseOrder.java`（字段已存在）
  - `backend/src/main/java/com/foodtraceability/service/impl/PurchaseOrderServiceImpl.java`
  - `backend/src/main/java/com/foodtraceability/dto/PurchaseOrderCreateDTO.java`
  - `backend/src/main/java/com/foodtraceability/dto/PurchaseOrderUpdateDTO.java`
- **状态**：已修复

#### LK-PURCHASE-04：采购计划状态流转
- **业务规则**：采购计划 approved → executing 需有「开始执行」入口；executing → completed 由采购订单全部入库触发。
- **当前实现**：
  - 前端已添加开始执行按钮和后端接口（executePlan：approved → executing）
  - **新增 plan_id 字段链路**：`purchase_orders` 表新增 `plan_id` 列关联采购计划（Flyway 迁移 V20260731_004）；`PurchaseOrder` 实体/`PurchaseOrderCreateDTO`/`PurchaseOrderUpdateDTO` 均已添加 `planId` 字段；`PurchaseOrderServiceImpl.createOrder()/updateOrder()` 持久化 `planId`
  - **executing → completed 自动回写**：`PurchasePlanServiceImpl.markPlanCompletedIfNeeded(planId)` 检查计划关联的所有采购订单是否都已完成（order_status=4），若是则更新计划状态为已完成(4)；由 `PurchaseStockinServiceImpl.confirmStockin()` 在 `updateOrderStockStatus` 后调用（异常隔离，失败不影响主入库事务）
- **问题影响**：状态机已完全闭环（draft → pending → approved → executing → completed）
- **修复文件**：
  - `backend/src/main/resources/db/migration/V20260731_004__add_plan_id_to_purchase_orders.sql`
  - `backend/src/main/java/com/foodtraceability/entity/PurchaseOrder.java`
  - `backend/src/main/java/com/foodtraceability/dto/PurchaseOrderCreateDTO.java`、`PurchaseOrderUpdateDTO.java`
  - `backend/src/main/java/com/foodtraceability/service/impl/PurchaseOrderServiceImpl.java`
  - `backend/src/main/java/com/foodtraceability/service/purchase/PurchasePlanService.java`、`impl/PurchasePlanServiceImpl.java`
  - `backend/src/main/java/com/foodtraceability/service/impl/PurchaseStockinServiceImpl.java`
  - `frontend/src/types/purchase-order.ts`、`frontend/src/api/purchase/order.ts`
- **状态**：已修复

#### LK-PURCHASE-05：采购收货数量回写采购订单
- **业务规则**：采购收货确认后，实收数量应回写采购订单明细的 `receivedQuantity`。
- **当前实现**：`PurchaseStockinServiceImpl.createStockin()` 在生成到货单时即将实收数量累加到 `purchase_order_items.received_quantity`，并校验累计到货数量不超过订单数量；`confirmStockin()` 仅负责质检通过后增加仓库/门店库存。`received_quantity` 语义为「已到货数量」，与「采购收货→到货单」职责匹配。
- **状态**：已核查（当前实现符合到货确认即回写已收货数量的设计）

---

### 2.4 订单/销售链路

#### LK-ORDER-01：订单完成应扣减物理库存
- **业务规则**：核心订单完成时，应根据 BOM 配方扣减 `inventory` 和 `store_inventory` 的库存，并生成库存变动流水。
- **当前实现**：经核查，`OrderNewServiceImpl.completeOrder()` (L421-L447) 已调用 `deductInventoryAndCalculateCost(order)` (L520-L603) 实现库存扣减：
  - 按 BOM 配方（`dish_recipe`）展开原料：单品直接展开，套餐先展开 `combo_ingredient` 再展开配方
  - 按 `materialId` 汇总后调用 `storeInventoryService.decreaseStock(storeId, materialId, qty)` 扣减**门店库存**
  - 跳过已退款菜品（`kitchenStatus=4`）
  - 退款流程 `restoreInventoryForRefund()` (L667) 已配套实现库存回补
  - **库存汇总设计**：仓库库存 + 门店库存通过 `InventorySummaryMapper.xml` 的 `UNION ALL` 实现，门店库存扣减会反馈到总库存数据
- **问题影响**：无（已完整实现）
- **修复文件**：无需修改，现有实现符合「数据先到门店库存，门店库存通过 UNION ALL 反馈到仓库总库存」的设计意图
- **状态**：已修复（经核查确认现有实现完整）

#### LK-ORDER-02：销售成本应实时结转
- **业务规则**：库存扣减时按当前单位成本计算销售成本，生成 `CostRecord`。
- **当前实现**：经核查，成本结转链路已完整实现：
  - `OrderNewServiceImpl.completeOrder()` 调用 `persistOrderCost()` (L455-L474) 写入 `cost_record` 表
  - `costType=1`（食材成本），`period=yyyy-MM`，`amount=totalCost`（分）
  - `StoreInventoryServiceImpl.decreaseStock()` (L207) 返回出库成本 = `unitCost × qty`（分），作为成本结转的数据来源
- **问题影响**：无（已完整实现）
- **修复文件**：无需修改
- **状态**：已修复（经核查确认现有实现完整）

---

### 2.5 财务中心链路

#### LK-FINANCE-01：采购入库应生成应付账款
- **业务规则**：采购收货确认入库后，应根据采购订单金额生成应付账款记录。
- **当前实现**：已核查 `PurchaseStockinServiceImpl.confirmStockin()` 调用 `createPayableForStockin(stockin)`，通过 `payableService.createForStockin()` 创建应付账款（含幂等性保证）；`voidStockin()` 中调用 `payableService.voidPayableByStockinId()` 逻辑删除关联应付。
- **状态**：已核查正常

#### LK-FINANCE-02：付款状态应回写采购订单
- **业务规则**：财务付款后，采购订单的 `paymentStatus` 和 `paidAmount` 应同步更新。
- **当前实现**：`PaymentServiceImpl.registerPayment()` 与 `voidPayment()` 在更新应付账款后，调用 `syncPurchaseOrderPaymentStatus(payable.getPurchaseOrderId())` 重算采购订单的 `paidAmount`（按订单下所有有效应付账款汇总）并更新 `paymentStatus`（0未付/1部分/2已付）。
- **问题影响**：原实现仅更新应付账款，未回写采购订单，导致前端订单列表付款状态长期停留在"未付"。
- **修复文件**：
  - `backend/src/main/java/com/foodtraceability/service/finance/impl/PaymentServiceImpl.java`（注入 PurchaseOrderMapper，新增 syncPurchaseOrderPaymentStatus 方法，在 registerPayment/voidPayment 中调用）
- **状态**：已修复

---

### 2.6 人事/其他链路

#### LK-HR-01：学习记录页面数据来源
- **业务规则**：学习记录页面应从培训模块或员工学习记录接口获取数据。
- **当前实现**：`KnowledgeStudyRecords.vue` 已接入 `trainingApi.getStudyRecords()`。
- **状态**：已确认正常

---

### 2.7 资产管理链路

#### LK-ASSET-01：资产盘点 CANCELLED 状态正确映射
- **业务规则**：资产盘点单取消时，前端状态 `CANCELLED` 应映射到后端保留值 4，不得错误映射到 `2=已审核`。
- **当前实现**：`frontend/src/api/asset/inventory.ts` 中 `FrontendStatusMap` 已将 `CANCELLED` 从 2 改为 4；`cancel` 方法注释说明后端兼容方案。
- **问题影响**：取消资产盘点单后，状态被错误标记为「已审核」。
- **修复文件**：
  - `frontend/src/api/asset/inventory.ts`
  - `docs/frontend-verification-plan.md`
- **状态**：已修复

---

## 三、修复执行记录

| 日期 | 修复链路 | 修改文件 | 文档更新 | 验证结果 |
|------|---------|----------|----------|----------|
| 2026-07-31 | LK-PURCHASE-04（部分） | 后端 PurchasePlanController/Service 新增 execute 接口 | frontend-repair-record.md | 前端构建通过，后端未编译 |
| 2026-07-31 | LK-WAREHOUSE-02/03、LK-PURCHASE-01/02 | `data-chain-fix-tracking.md`、`data-business-chain.md`、`frontend-verification-plan.md`、`types/purchase-request.ts`、`api/purchase/request.ts`/`order.ts`、`views/purchase/PurchaseRequest.vue` | 同步更新本文档及前端验证计划 | `npm run build` 通过（exit code 0） |
| 2026-07-31 | LK-WAREHOUSE-04、LK-ASSET-01 | `types/warehouse-check.ts`、`api/warehouse/converters.ts`、`views/warehouse/InventoryCheck.vue`、`api/asset/inventory.ts` | 同步更新 `frontend-verification-plan.md` | `npm run build` 通过（exit code 0） |
| 2026-07-31 | LK-PRODUCT-01 | `frontend/src/api/store-ops/store-inventory.ts` | 同步更新 `frontend-verification-plan.md` | `npm run build` 通过（exit code 0） |
| 2026-07-31 | LK-PURCHASE-03 | `backend/src/main/java/com/foodtraceability/service/impl/PurchaseOrderServiceImpl.java`、`backend/src/main/java/com/foodtraceability/dto/PurchaseOrderCreateDTO.java`、`backend/src/main/java/com/foodtraceability/dto/PurchaseOrderUpdateDTO.java` | 同步更新 `data-chain-fix-tracking.md`、`frontend-verification-plan.md` | `mvn compile -q` 通过，`npm run build` 通过（exit code 0） |
| 2026-07-31 | LK-WAREHOUSE-05 | `backend/src/main/java/com/foodtraceability/service/impl/InventoryOutboundServiceImpl.java`、`backend/src/main/java/com/foodtraceability/service/impl/InventoryAdjustServiceImpl.java`、`frontend/src/api/warehouse/inventory-outbound.ts`、`frontend/src/api/warehouse/inventory-adjust.ts` | 同步更新 `data-chain-fix-tracking.md`、`frontend-verification-plan.md` | `mvn compile -q` 通过，`npm run build` 通过（exit code 0） |
| 2026-07-31 | LK-WAREHOUSE-01 / LK-WAREHOUSE-03 | `backend/src/main/resources/mapper/InventorySummaryMapper.xml`、`backend/src/main/java/com/foodtraceability/service/impl/StoreInventoryServiceImpl.java` | 同步更新 `data-chain-fix-tracking.md`、`frontend-verification-plan.md` | `mvn compile -q` 通过，`npm run build` 通过（exit code 0） |
| 2026-07-31 | LK-PURCHASE-05 | `backend/src/main/java/com/foodtraceability/service/impl/PurchaseStockinServiceImpl.java` | 同步更新 `data-chain-fix-tracking.md` | 已核查，当前实现符合到货确认回写已收货数量设计 |
| 2026-07-31 | LK-PRODUCT-02 / LK-FINANCE-01（核查） | 无代码改动 | 同步更新 `data-chain-fix-tracking.md` | 已核查正常：FoodServiceImpl 已保存 costPrice；PurchaseStockinServiceImpl.confirmStockin 已创建应付 |
| 2026-07-31 | LK-FINANCE-02（修复） | `backend/src/main/java/com/foodtraceability/service/finance/impl/PaymentServiceImpl.java` | 同步更新 `data-chain-fix-tracking.md` | `mvn compile -q` 通过，`npm run build` 通过（exit code 0） |
| 2026-07-31 | 采购订单前后端契约对齐（C 类） | `frontend/src/api/purchase/order.ts`、`frontend/src/types/purchase-order.ts` | 同步更新 `data-chain-fix-tracking.md` | `npm run build` 通过（exit code 0） |
| 2026-07-31 | LK-PURCHASE-04（完整修复）+ LK-ORDER-01/02（核查确认） | `backend/src/main/resources/db/migration/V20260731_004__add_plan_id_to_purchase_orders.sql`、`backend/src/main/java/com/foodtraceability/entity/PurchaseOrder.java`、`dto/PurchaseOrderCreateDTO.java`、`dto/PurchaseOrderUpdateDTO.java`、`service/impl/PurchaseOrderServiceImpl.java`、`service/purchase/PurchasePlanService.java`、`service/purchase/impl/PurchasePlanServiceImpl.java`、`service/impl/PurchaseStockinServiceImpl.java`、`frontend/src/types/purchase-order.ts`、`frontend/src/api/purchase/order.ts` | 同步更新 `data-chain-fix-tracking.md`、`data-chain-fix-handover.md` | `mvn compile -q` 通过，`npm run build` 通过（exit code 0） |
| 2026-07-31 | SR-1~SR-8（单店无单直收 + 财务域 + 追溯码 + 模式开关） | `db/migration/V20260731_006__direct_receipt_and_single_store_finance.sql`、`dto/ReceiptConfirmationCreateDTO.java`、`entity/ReceiptConfirmation.java`、`service/impl/ReceiptConfirmationServiceImpl.java`、`event/ReceiptConfirmationCompletedEvent.java`（新增）、`event/listener/ReceiptConfirmationEventListener.java`（新增）、`event/EventPublisher.java`、`frontend/src/types/purchase-arrival.ts`、`api/receipt-confirmation.ts`、`components/business/receipt-confirmation/DirectReceiptFormDialog.vue`（新增）、`composables/useReceiptPage.ts`、`views/store-ops/StoreReceiving.vue`、`stores/permission.ts` | 同步更新 `data-chain-action-plan.md` 第九章 | `mvn compile -q` 通过，`npm run build` 通过（exit code 0） |
| 2026-07-31 | **移除无单直收（D1 回退）** + 保留 SR-7/SR-9/T1-1 | 后端：`ReceiptConfirmationServiceImpl`（去 direct 分支/`createDirectPayable`/常量）、`dto/ReceiptConfirmationCreateDTO.java`（回退原状+恢复 @NotNull）；前端：删除 `DirectReceiptFormDialog.vue`、`StoreReceiving.vue`/`useReceiptPage.ts` 去直收、`receipt-confirmation.ts`/`types/purchase-arrival.ts` 回退。保留：到货流程追溯码（SR-7）、角色化采购/财务（SR-9）、追溯码列类型（T1-1） | 同步更新 `data-chain-action-plan.md` | `mvn compile -q` 通过，`npm run build` 通过；实跑验证无 arrivalId 请求被拒（"到货单ID不能为空"） |

---

## 四、待确认事项

1. **产品中心是否需要门店选择器**：`FoodManagement.vue` 是产品中心全局页面，如果原料来源改为门店库存，是否需要增加门店选择？还是默认使用当前用户所属门店？
2. **物料命名统一方向**：采购链路统一使用 `materialId/materialName` 还是 `productId/productName`？
3. **订单库存扣减粒度**：订单完成时扣减仓库库存、门店库存，还是两者都扣？扣减比例如何确定？

---

## 2026-08-01 批 2：UI 明细链路修复

### 1. 采购申请/订单 500 根因（storeId 脏数据 + 防御）
- 现象：申请单"生成订单"500；订单"从申请生成"批量转单 500（PR20260729007/006）。
- 根因：purchase_request.store_id（varchar）全部为 '624'，而 stores_new 仅存在 1/2 → 转单时 purchase_orders.store_id（bigint）FK 违反 k_purchase_orders_store_id。624 为历史遗留脏数据（非当前用户数据）。
- 修复：① 数据：UPDATE purchase_request SET store_id='1' WHERE store_id='624'；② 防御：PurchaseRequestServiceImpl.resolveValidStoreId()（注入 StoreNewMapper，create 与 generateOrder 均校验，无效回落默认门店 1 并 warn）。
- 验证：PR20260729006/007 转单成功。

### 2. 商品档案分类过滤（工具栏）
- 根因：控制器未绑定 categoryId（前端曾转 categoryName 传、后端忽略）。
- 修复：控制器补 @RequestParam Long categoryId；前端直传 categoryId。
- 验证：UI 选"蔬菜类"→ 5 条。

### 3. 明细下拉数据源统一（菜品管理/采购申请/采购计划）
- 根因：菜品管理明细从门店库存拉（仅 6 项），应来自商品档案（21 项）。
- 修复：FoodManagement.loadIngredientOptions 改档案为主 + 库存 unit_cost 匹配最新价；采购申请/计划 loadMaterialOptions 同样加最新价映射（latestPriceMap）。
- 回填规则：名称/规格/单位从档案直填；单价取门店库存最新成本价（无则 0 手动填）。
- 验证：菜品管理新增对话框下拉 21 项（原 6）。

### 4. 套餐管理（DishComboDialog）
- 保存后编辑空白：openEdit 未调 comboApi.getById（注释写了但代码没做）→ 补调 getById 加载明细。
- 明细缺单价/小计：后端 ComboIngredientVO 已有 unitPrice/subtotal，前端 converter ingredientToFrontend 丢弃 → 补映射（types + converters）。
- 表格加"单价(元)/小计(元)"列；列宽固定 resizable=false；对话框 720→900px。
- 验证：编辑套餐明细 1 行正常显示，列头含 单价/小计。

### 5. 定价历史记录（DishPricing）
- 根因：后端返回 oldSalePrice/newSalePrice，前端 PricingBackend 用 oldPrice/newPrice → undefined → 显示错乱。
- 修复：types 与 converters 字段对齐。
- 验证：历史显示 菜品 0.50→0.80 (+60%)、套餐 12.00→13.50 (+12.5%)。

### 6. 采购计划：生成计划 + 导出（从占位到可用）
- 新增 POST /v1/purchase/plans/generate-from-stock：扫描 store_inventory（safety_stock>0 且 current< safety）→ 建议量 = max_stock-current（无则 safety*2-current，至少1）→ 草稿计划+明细；单价取 unit_cost。
- 新增 GET /v1/purchase/plans/export：POI 导出 Excel（复用列表筛选）。
- 前端 purchasePlanApi.generateFromStock / exportExcel + 页面按钮接真实 API。
- 验证：娃娃菜 safety25/stock20 → 生成 PL 明细 qty=30 单价350分；导出 200 xlsx。

### 7. 明细表格/对话框体验统一
- 采购申请：对话框 960→1100px、明细表格 resizable=false、备注说明改为 form-section 对齐。
- 采购计划：对话框 960→1100px、明细表格 resizable=false。
- 菜品管理：对话框 800→1000px、明细表格 resizable=false。
- 套餐：见上。

### 8. 分页体验
- 页大小选择持久化（useCrudTable localStorage 'fts-table-page-size'，handleSizeChange 写入；商品档案 @size-change 改绑 handleSizeChange）。
- 验证：50/页 → 刷新后仍 50。

---

## 2026-08-01 批 3：采购业务链衔接修复

### 1. 业务链回顾（用户确认的方案）
- 采购申请 → 生成订单 → **跳转订单列表定位新订单**（?requestNo=申请单号）
- 采购计划（审批/执行）→ **生成订单**（业务链缺口补齐）
- 从申请/计划生成的订单保持**草稿**（orderStatus=0，由用户在订单页确认/到货登记）

### 2. 申请→订单跳转
- PurchaseRequest.vue handleGenerateOrder：成功后 outer.push('/purchase/orders?requestNo=')
- PurchaseOrder.vue：queryForm 增加 requestNo；onMounted 处理 ?requestNo= 定位（过滤+高亮+推送最近记录）
- 关键坑：路由为 **/purchase/orders（复数）**，单数 /purchase/order 无匹配（白屏）
- order.ts getList 参数映射缺 requestNo → 补齐（否则请求不带参数）

### 3. 计划→订单（新增）
- 后端 PurchasePlanServiceImpl.generateOrder(planId)：仅 approved/executing 计划可生成；幂等（已生成过返回既有订单）；订单草稿、storeId=1（集中式单店）、requestNo=planNo、sourceType=purchase_plan、明细带材料/数量/单价（estimatedPrice）
- POST /v1/purchase/plans/{id}/generate-order
- 前端 PurchasePlan.vue：操作列"生成订单"按钮（approved/executing 显示）+ 跳转订单列表定位；plan.ts 加 generateOrder

### 4. 订单查询 requestNo 过滤
- PurchaseOrderQueryDTO 加 requestNo；getPurchaseOrderPage eq 过滤
- 验证：?requestNo=PR20260729003 → 仅 1 行（PO20260801006）

### 5. 端到端验证
- 申请 PR20260729003 → 生成订单 → 跳转 /purchase/orders?requestNo=PR20260729003 → 过滤 1 行 ✓
- 计划 PL20260801001（创建→提交→审批）→ 生成订单 PO202608010004（幂等验证：重复调用返回同单）✓
- 订单状态 = 草稿(0)、requestNo/sourceType 追溯字段正确 ✓

### 6. 其他
- 菜品管理明细下拉回退为门店库存数据源（用户澄清：菜品管理用门店库存是正确设计，采购模块才用商品档案）

---

## 2026-08-01 批 4：7.1 采购主闭环 UI 自检与断点修复（前端为测试重点）

> 方法：Playwright 驱动前端 3002 页面做真实操作（登录→页面→点击→断言），文本断言替代截图（模型不支持读图）。发现的问题按用户操作路径定位修复。

### 自检发现并修复
1. **订单审批走 mock API 不落库**（数据链缺陷）：PurchaseOrder.vue 的 ApprovalDialog 未传 approveHandler/rejectHandler → 审批仅提示不更新状态。修复：接 purchaseOrderApi.approve/reject 真实 API。验证：待审批→已审批（API 200）。
2. **到货单无质检/入库确认能力**（7.1 主断点）：前端 UI 用 /v1/purchase/arrivals 链路，后端只有 /v1/purchase/stockins 的质检/入库接口 → 到货后流程全断。修复：
   - 迁移 V20260731_012：purchase_arrivals 加 quality_check_result/quality_check_remark/confirm_time
   - 后端 PurchaseArrivalServiceImpl：qualityCheck（1通过/2失败，失败关闭）+ confirmArrival（增加库存[仓库+门店/unit_cost最新价覆盖/流水]→应付[AP+arrivalId幂等]→回写订单实收→同步生成原料追溯码→事件记录成本）
   - 控制器：PUT /{arrivalId}/quality-check、PUT /{arrivalId}/confirm
   - 前端 ArrivalDetailDialog：明细表格已存在但 data.items 曾为空（旧缓存）；补质检/确认入库操作区 + arrival.ts qualityCheck/confirmArrival API + 类型 qualityCheckResult/confirmTime + 父组件 refresh 接线
3. **到货明细实收数量恒 0**（断点）：前端无实收录入 → 确认入库无货可入。修复：后端兜底（实收=0 时按预计数量入库+回写），订单实收/到货明细 received 同步回写。
4. **追溯码生成失败**：事件模型单物料与多明细到货单不匹配（materialId null）。修复：确认入库时同步逐明细生成追溯码（MaterialTraceCodeGenerateDTO），事件仅保留成本记录。
5. **已确认到货单占用剩余可到货量**：calculateRemainingQuantity 未排除已确认（status=3）到货单 → 无法二次到货。修复：getExistingArrivalExpectedQuantity 排除已确认到货单。
6. **数据修正**：申请/订单明细/到货单/库存中的无效仓库 584 → 1（集中式单店默认仓/门店，warehouses 与 stores_new 均无 584，为 7-29 测试遗留）。

### 验证结果（全链路）
- UI：PR20260729002→生成订单→跳转定位→提交审批→审批（真实API）→确认下单→收货跳转→创建到货单 AR202608010001→详情质检→确认入库→列表"已收货"
- 数据：库存 +50（unit_cost=500 分覆盖）、应付 AP0000000011、追溯码 MTC202608010001960（qty 50）、订单实收回写 50、成本记录 250.00 元
- 回归：订单 52 二次到货 AR202608010003 确认后订单实收 50、库存累计 100

### 追加（批 4b）：新增采购申请/计划明细下拉空白（用户复验卡点）
- **根因**：明细表格内 el-select :teleported="false" → 下拉内联渲染被表格 overflow:hidden 裁剪（与 7-31 菜品管理同源问题，当时修了菜品/套餐/批量定价，**遗漏采购申请/计划页**）。小屏或滚动对话框后下拉空白。
- **修复**：PurchaseRequest.vue、PurchasePlan.vue 明细下拉改 :teleported="true" + :popper-options="{ strategy: 'fixed' }"（与项目 Dialog 内弹出组件定位规范一致）；PurchasePlan 选项标签简化为物料名称。
- **验证**：1024x700 小屏 + 对话框滚动后，明细下拉 21 项可见（不锈钢汤桶…）。

---

## 2026-08-01 批 5：采购业务方向调整（需求侧入口统一 + OA 化）

> 决策（用户拍板，基于成熟产品调研）：请购(需求侧)→计划(汇总)→订单(执行) 行业模型；采购申请收敛为需求单据，各部门提报。

### 业务方向（新模型）
`
需求层（各部门/门店）: 物资需求提报（= 采购申请，OA 化自动绑定申请人/部门）→ 提交
汇集层（采购部）:     采购申请页 审批 → 生成采购订单
执行层（采购部自用）:  采购计划（草稿直接建）→ 审批 → 生成订单
`

### 改动
1. **下线**采购模块"物资需求提报"：路由 /purchase/material-request 移除（router/index.ts），采购菜单移除该项（modules/purchase/menu.ts），页面 MaterialRequest.vue 不再可访问（API 保留避免破坏引用）
2. **改造**门店运营"物资需求"（/store-management/material-request → StoreMaterialRequest.vue 整体重写）：
   - 数据源 material_request → **采购申请（purchase_request API/类型/converter）**
   - 移除"转采购申请"（本身即采购申请）；保留 新建/详情/编辑/提交/批量提交/删除
   - 状态流：draft/pending/approved/rejected/completed；统计走 getStatistics
   - 明细下拉从商品档案（teleported=true 防裁剪，21 项）+ 最新单价回填
3. **OA 化**：创建表单移除部门选择，顶部提示"申请人/所属部门由系统自动带出，不可修改"；后端 PurchaseRequestServiceImpl.create 已有强制绑定（applicantId/Name、departmentId/Name、storeId 从 JWT 取，前端伪造无效）
4. **跨部门入口**：新增 modules/request-center/menu.ts 菜单组"物资需求"（visibleRoles：owner/admin/店长/组长/部门经理/普通员工）→ 指向提报页
5. 采购申请页（/purchase/request）保留：采购部审批 + 生成订单（此前已通）

### 验证（UI）
- 菜单：采购管理无"物资需求提报"；侧边栏有"物资需求"入口
- 提报页新增：OA 提示显示申请人/所属部门；明细下拉 21 项；POST 返回 requestNo=PR20260801003、applicantName=系统管理员（后端绑定）✓
- 列表显示：单号/标题/申请人/日期/状态(草稿)/金额/操作 ✓
- 备注：admin 用户无部门（departmentId null）为数据问题，真实用户配置部门后自动带出

---

## 2026-08-01 批 6：物资需求提报页精调 + 部门过滤 + 临时物料策略

1. **布局精调**：根因 .fts-dialog--lg 全局强制 960px 覆盖了 1100 设置 → 明细 8 列被挤压截断。修复：对话框改用自定 ts-dialog--request（1120px + max-width: calc(100vw-48px) 防小屏溢出），明细表格 min-width 860 防列挤压。验证：对话框宽 1120 ✓
2. **菜单合并**：store-management 菜单移除重复"物资需求"项，统一由跨部门菜单组"物资需求"（request-center）提供入口
3. **物资按部门过滤**：商品档案加 department_id（V20260731_013，NULL=通用物料）；后端查询支持 departmentId 过滤（部门匹配 OR 通用）；提报页下拉传当前用户部门；档案表单加"使用部门"下拉。验证：departmentId=2 返回全部通用物料 ✓
4. **临时物料策略**：关闭 allow-create（仅档案物料可选，杜绝错别字产生新物料名——主数据必引用原则）；placeholder 提示"缺失请先由采购部建档"

---

## 2026-08-01 批 7：提报页统一 + 名称/路径调整 + 订单数据断点修复

### 1. 新增需求对话框统一（对齐新建订单）
- 弃用自定义 fts-dialog--request(1120px)，改用标准 960px + fts-dialog--lg + label-width 110 + el-row 两列布局，与采购订单"新建订单"完全一致

### 2. 名称与路径
- 页面标题/菜单统一为"物资需求提报"；路由移至 /workspace/material-request（domain: workspace，工作台域，所有员工可见）

### 3. 订单数据断点（用户复验发现）
- **编辑订单表头供应商空白**：根因 generateOrder(申请转单) 不指定供应商（supplierId=null）。修复：①后端 getOrderDetail 详情返回时从第一个明细物料档案的主供应商自动回填（supplierName/contactPerson/contactPhone）；②前端 edit() 打开时若 supplierId 为空，从明细物料主供应商自动填表头。验证：订单55 编辑打开显示"鲜蔬源农产品有限公司"+账期"30天"+结算方式"月结"
- **详情联系人/电话空**：订单未存这些字段。修复：后端详情回填（从供应商档案取王经理/13800138001）+ 前端取 detailSupplier 实时档案。验证 ✓
- **审批记录"没有"**：实为草稿订单未提交审批（时间线本就为空）。验证：订单55 提交审批+审批后 approval_audit_logs 有 submit/admin + approve/admin 两条 → 详情时间线 2 节点 ✓

---

## 2026-08-01 批 8：物资需求对话框统一 + 采购订单成熟方案改造

### 1. 新增需求对话框（对齐"新建订单"）
- "+添加明细"按钮移入 section-title 右侧（flex space-between，与新建订单一致，解决重叠）
- "总金额"改为"明细合计"并右对齐（amount-summary 样式对齐新建订单）

### 2. 采购订单全面改造（用户确认的成熟方案）
- **列表操作收敛**：只留 查看 + 当前状态主操作（draft→提交审批 / pending→审批 / ordered→收货）；编辑/删除/终止移入详情"更多"Tab
- **详情 Tab 分层**：摘要行（状态徽章+单号+金额+主操作）→ 流程步骤条 → Tab（基本信息[精简10项]/采购明细/审批记录/更多）
- **关联互动**：来源申请号→跳转采购申请页；供应商→跳转供应商档案；明细物料→跳转商品档案
- **删除独立"链路追溯"对话框**（PurchaseTraceDialog 引用移除，追溯能力由步骤条+跳转承担）
- 编辑保存成功后详情同步刷新（handleOrderFormSuccess）

### 验证
- 已下单订单列表操作 = [查看, 收货]（状态驱动正确）
- 详情 4 个 Tab 正常；来源申请/供应商为可点击链接；审批记录时间线 2 节点；更多 Tab 显示"终止订单"

---

## 2026-08-01 批 9：订单详情体验精修（用户反馈）

1. **删除"更多"Tab**：编辑/删除/终止按状态放回摘要行（draft→编辑+提交审批+删除；ordered→收货+终止），Tab 收敛为 3 个
2. **供应商标色不跳转**：纯文本+主题色（link-text），移除点击跳转
3. **来源申请 → 弹出详情**：新建 PurchaseRefDialog（关联单据详情弹窗：单号/标题/部门/申请人/状态/金额/明细表），点击来源申请号弹出；**步骤条"采购申请"节点显示"1 采购申请 PR20260801005"且点击同样弹出详情**（不再跳页面）
4. **商品名称去跳转**：恢复纯文本
5. **审批记录紧凑化**：新建 ApprovalRecordCompact（横向节点：动作+操作人+时间+意见，flex 排布），替代垂直时间线（省纵向空间）；订单详情不再用 ApprovalTimeline

### 验证
- Tab 收敛为 3 个；步骤条节点含单号；来源申请点击弹出关联详情（含明细表）；审批记录横排 2 节点；旧垂直时间线已移除

---

## 2026-08-01 批 10：订单详情体验精修（二）+ 收货链路断点修复

### 1. 采购明细加大
- 详情明细表格 size small→default，列宽加大，单元格 14px/10px 内边距（实测 14px）

### 2. 步骤条完善
- 未进行到的节点 is-disabled（opacity 0.45 + not-allowed），不可点击
- clickable 按流程位置驱动：申请(有单号)、到货(已下单/已收货)；入库/结算无可点单据保持不可点
- 到货节点点击 → 弹出到货单详情（PurchaseRefDialog 扩展 purchase_arrival 类型，按订单号查）

### 3. 收货断点修复（用户实测发现）
- **根因**：提报页新建的申请明细无收货地点 → 订单明细 planned=null → 到货单 warehouse_id=空 → 确认入库时门店库存被静默跳过（"入库了但不知道入哪"=实际未入库）
- **修复**：buildAndSaveArrival 兜底默认仓库 1；confirmArrival 门店同步 storeId 兜底 "1"
- **数据修正**：AR202608010004（wh=null）→ 1；娃娃菜 1 件补入库存（20→21）
- 说明：审批记录不含收货/入库动作（它是审批日志）；单据流转看步骤条（到货/入库节点状态）

### 4. 其他
- 摘要行按钮统一 default 尺寸（编辑/提交审批/删除、审批通过/驳回、确认下单、收货/终止）
- 审批记录空态：虚线卡片+提示文案（"提交审批后，审批流转将在此展示"）
- **操作列补"驳回"**：pending 行加"驳回"按钮（prompt 输入原因 → purchaseOrderApi.reject → 通知申请人），审批者发现问题可直接驳回

### 验证
- 明细字号 14px；步骤条 class 含 is-disabled（未到节点）；摘要按钮 default；到货节点点击弹到货详情（含单号）

---

## 2026-08-01 批 11：订单详情/审批记录/驳回 精修（三）

1. **详情对话框加宽**：960→1200px（fts-dialog--xl）
2. **审批记录表达优化**：
   - 顺序修复：后端 getApprovalLogs 改 orderByAsc（原倒序导致"通过在前提交在后"）
   - 信息字号加大：动作 14px、操作人/时间 13px（原 12px 太小）
   - 深色模式：审批卡片中性化（白 4% 底 + 白 12% 边框）
3. **深色模式全局优化**：el-table 表头文字 rgba(255,255,255,0.82)（原灰蓝）；el-dialog 标题主题色加粗
4. **步骤条到货节点复用原对话框**：改引 ArrivalDetailDialog（到货登记页同款组件，含基本信息/物流/明细/入库操作区），不再用自制简化弹窗
5. **新建/编辑订单对话框加宽**：960→1200px（fts-dialog--xl）
6. **驳回功能验证**：pending 行操作列 [查看/审批/驳回]；驳回弹窗输入原因 → 状态"已取消"（测试订单 55 状态由 DB 置 pending 验证，已驳回为测试数据）

### 验证
- 详情 1200px；审批记录顺序 提交→通过；到货节点点击弹"到货单详情"（原组件）；驳回全流程走通

---

## 2026-08-01 批 12：驳回闭环 + 深色背景 + 订单对话框修复

### 1. 驳回后闭环（修改→重新提交→审批→通过）
- 后端新增 STATUS_REJECTED=7（reject 置 7，与终止 cancel(5) 语义分离；updateOrder/submitOrder 校验放行 rejected）
- 前端映射 7→'rejected'（显示"已拒绝"）；操作列/详情摘要 rejected 显示 [编辑/重新提交/删除]
- 存量被驳回订单(order_status=5)修正为 7
- **验证**：驳回→[查看/编辑/重新提交/删除]→重新提交→待审核→审批→已审核 ✓

### 2. 深色模式背景（非文字）
- 根因：--fts-bg-hover(#252d3d)/--fts-bg-selected(#1a2a3a) 带蓝调
- 改为中性灰：bg-page #101014、bg-card #16161a、bg-hover #26262b、bg-selected #202024、bg-secondary #1b1b1f

### 3. 新建/编辑订单对话框修复
- **物料下拉"拉不出数据"根因**：选供应商后被"供应商物料过滤"过滤为空（业务逻辑正确但无提示）。修复：空态提示（"该供应商暂无供应的物料，可勾选允许跨供应商选料"）；下拉 teleported 修复裁剪；未选供应商时 21 项正常
- 表格加大：size small→default、单元格 padding 10px、列宽加大、内嵌组件去 small
- "添加明细"等按钮 size small→default

### 4. 操作列快捷功能恢复
- 列表操作列恢复按状态显示全部快捷按钮：查看/编辑/提交审批(或重新提交)/审批/驳回/收货/删除

---

## 2026-08-01 批 13：审批记录表格化 + 订单对话框重建 + 跨供应商强一致

### 1. 审批记录改为紧凑表格（解决无限横向扩展挤压）
- ApprovalRecordCompact 由横向节点改为 el-table（序号/操作/操作人/审批节点/时间/意见），记录多时纵向滚动，永不挤压变形

### 2. 新建/编辑订单对话框（编码损坏后完整重建，含全部功能与本次优化）
- 物料下拉：**选中后仅显示物料名称**（短名），选项富展示"名称[规格]·供应商"（利于重名区分）；空态提示
- 数量/单价：**去掉加减按钮**（:controls=false 纯输入），省空间
- "允许跨供应商选料"：**加边框样式**（checked 主题色边框）
- 表格加大、1200px、编辑供应商自动回填（保留）
- 功能保留：账期/结算方式展示、明细 CRUD、金额合计、create/edit/submit、校验

### 3. 跨供应商逻辑（用户确认的专业设计）
- **未开启跨供应商时**：切换供应商若与明细主供应商不一致 → **ElMessageBox.alert 警告对话框**（点击消失）→ **强制改回与明细一致的主供应商**（不再自动勾选跨供应商）
- 已开启跨供应商：允许混用 + warning 提示
- 验证：选不锈钢汤桶（主供应商=商用厨房设备）→ 切 E2E Supplier → 警告框 → 回退"商用厨房设备有限公司" ✓

---

## 2026-08-01 批 14：审批记录对齐 + 摘要行 + 跨供应商提示组件化

1. **审批记录表格**：与"采购明细"表格同规格（default 行高、padding 10px 8px、14px 字号）；**驳回徽章修复**（StatusTag 无 danger 类型→改用 error，红色正常显示）
2. **订单详情摘要行**：金额 22px 加粗（原 14px）、增加"下单日期"显示
3. **section-actions 对齐**：checkbox 与"添加明细"按钮垂直对齐（flex align-items center）；勾选跨供应商后显示说明"明细可混用多家供应商，应付按明细供应商分别核算"
4. **跨供应商提示组件化**：新建 CrossSupplierTipDialog（WarningFilled 图标 + 标题 + 说明 + 警示条样式），替代原生 ElMessageBox.alert，观感统一精致

### 跨供应商逻辑的专业结论（用户问"表头供应商是否要多家"）
**不需要**。行业标准（SAP/用友）：订单表头供应商=主供应商（结算/应付归属），明细可混用多家供应商，应付按明细供应商分别核算。跨供应商开关控制的是"明细选择范围"，不是表头多选。已通过勾选后的说明文案传达此语义。

---

## 2026-08-01 批 15：下单日期样式 + 申请详情弹窗复用原组件 + 跨供应商拆分方案评估

### 1. 下单日期表达
- 加粗 + 左侧分隔线 + 与金额间距（margin-left + border-left）

### 2. 申请详情弹窗 → 复用原页面对话框组件
- 新建 RequestDetailDialog.vue（从申请页提取详情展示：基本信息/明细/优先级/状态），申请页与订单详情共用
- 订单详情步骤条"采购申请"节点点击 → 弹出与申请页一致的详情对话框（不再用自制简化弹窗）
- 验证：订单 PO20260801011 步骤条"1 采购申请 PR20260801005"可点 → 弹"采购申请详情"（含单号+明细）✓

### 3. 跨供应商拆分方案评估（用户新设计）
- **评估：方案成立且行业有先例**（ERP 按供应商拆单：SAP PO splitting / 用友请购转多订单）
- 逻辑：明细多家供应商 → 保存时按供应商分组拆成多张采购订单（各自应付归属）→ 混合表单作为"合并/拆分/归类"工具
- 待实施（下轮）：后端 createOrder 按供应商分组拆分 + 前端"混合"标记与提示

### 附：重建说明
- PurchaseRequest.vue 在操作中损坏（行操作+编码），已完整重建（script+template 重建、style 保留原文件并补 </style>），功能与之前一致（列表/统计/创建/编辑/审批/生成订单跳转/批量提交）
- 构建通过；申请页列表/新增对话框/明细下拉验证正常

---

## 2026-08-01 批 18：徽章颜色根治 + 创建人 + 点击无反应修复 + 多供应商拆分

1. **状态徽章颜色（彻底根治，三层根因）**：
   - ① scoped 样式不匹配动态 class → 改 inline style 绑定主题变量
   - ② orderStatusMap 返回 primary/danger/default 但 StatusTag.statusConfig 无这些 key → 回退 info → statusConfig 补 primary/danger/default
   - ③ info 变量过素 → 改蓝灰（浅 #5a8fc7 / 深 #7aa2c4）；订单状态配色重设计（draft灰/pending黄/approved蓝/ordered绿/received蓝灰/completed绿/rejected红/cancelled灰）
   - 实测全部状态有颜色 ✓
2. **订单创建人（发起人）**：后端实体加 createByName（非表字段）；createOrder/generateOrder 写 createUserId+createByName（OA 化）；列表/详情批量填充（fillCreateByNames）；前端列表加"创建人"列+详情加创建人。实测列表/详情显示"系统管理员" ✓
3. **点击无反应修复**：拖拽指令的 click 抑制标志 moved 未复位（mouseup 丢失→后续点击全被抑制）→ click 抑制后立即复位 + mouseup 兜底延时复位。这正是"突然点击无反应，刷新后正常"的根因
4. **多供应商自动拆分（实施完成）**：后端 createOrder 按明细物料主供应商分组 → 每组一张订单（各自供应商/明细/金额）；Controller 返回 List；前端 create 适配数组+提示"已生成 N 张订单（已按供应商拆分）"；选择物料后表头供应商自动带入主供应商。实测：娃娃菜(供应商1)+撒尿牛丸(供应商2) → PO202608020003(鲜蔬源/娃娃菜/1050) + PO202608020004(商用厨房设备/撒尿牛丸/2400) ✓

---

## 2026-08-01 批 19：徽章配色去重 + 草稿背景 + 审批对话框对齐详情风格

1. **配色去重**：ordered（已下单）由绿改为**蓝灰**（info），与 completed（已完成）绿区分；received 保持绿（success）
2. **草稿徽章背景**：default 色系变量缺失（无背景/边框）→ tokens+dark 补 --fts-status-default-*（浅 #757575/#f5f5f5；深 #9e9e9e/#26262b）
3. **审批对话框对齐详情风格**：头部仿订单详情 status-header（primary 色带左边框 4px + 方形大字徽章"审" + 节点名大字 + 业务类型 + 单据编号）；信息区 el-descriptions；意见区；通过/驳回图标按钮。实测：色带 4px、大字标题、descriptions 均正常
4. 实测徽章：草稿灰(有背景)/待审核黄/已审核蓝/已下单蓝灰/已完成绿/未付款红 ✓

---

## 2026-08-01 批 20：审批身份单号 + ordered 橙色 + 明细列宽精调

1. **审批对话框身份信息**：新增 businessNo/businessTitle props；显示"采购单号：POxxx"（业务单号而非数字 ID，防止审批错单）；PurchaseOrder.vue 传入 orderNo；修复 businessNoLabel ref 未 .value 导致 [object Object]。实测"采购单号：PO202608010012" ✓
2. **ordered（已下单）改橙色**：新增 --fts-status-orange-*（浅 #e65100/#fdeee5；深 #ffab6e/#331f14）+ statusConfig orange；与已完成绿、待审核黄清晰区分（建立颜色记忆：灰草稿/黄待审/蓝已审/橙已下单/绿完成/红拒绝）
3. **明细列宽精调**：列宽重排（物料200/供应商130/规格140/单位100/数量120/单价150/金额120/备注140/删除60）；根因：el-table .cell 默认 padding 12px 挤压内嵌控件 → 覆盖 .cell padding 0 + 输入框 width:100%。实测规格输入框 77→101px、单位 37→61px（可完整显示 placeholder）

---

## 2026-08-01 批 21：驳回限制讨论 + 设计推广 + 申请生成订单对话框 + 编辑保存 bug

### 1. 驳回次数限制（业务讨论）
- 建议"软限制 + 修改校验"：①申请单加 reject_count，达 3 次后"重新提交"变灰（管理员可重置）；②驳回后再次提交必须修改内容（明细/标题与上次驳回版本一致则拦截）；③流程升级（驳回2次自动升上级）适合多级组织，单店后置

### 2. 采购订单对话框/下拉设计推广
- 采购计划：对话框 1100→1200（xl）、明细表格加大（cell padding/cell 内边距 0）、物料下拉富选项+选中短名、数量去加减按钮、列宽加大
- 采购退货：对话框 960→1100、明细表格加大、输入框 100%
- 商品档案/供应商档案/采购结算：对话框 960→1100（新增全局 fts-dialog--wide）
- 采购合同已 1200；电子合同/到货登记无独立对话框；分类 680 合理；报表/分析只读跳过

### 3. 从采购申请生成订单对话框
- 960→1200、提示条（选择已审批申请说明）、表格加大（申请编号/标题/部门/申请人/申请时间/金额/状态）、选中合计汇总

### 4. 采购申请编辑保存重复 bug（严重）
- **根因**：handleEdit 未记录编辑目标——保存时用 currentDetail?.requestId（可能是"查看"过的其他单或 null）→ 走错分支（覆盖他单或新增一条）→ "多了一组"
- 修复：独立 editRequestId ref（handleEdit 设置/保存判断/新增清空）
- 验证：编辑 PR20260801006 类操作不再重复

---

## 2026-08-02 批 22：预设权限模板未生效（上线 P0）

> 现象：上线后"预设权限模板"4 模式均未应用，所有菜单暴露。当前推进集中单店模式。

### 根因（三层）
1. **无默认激活模板**：`currentTemplate` 仅由用户手动点"应用模板"设置，且只存浏览器 localStorage → 新环境/新浏览器为空 → `getVisibleMenus()` 不应用任何模板 → 全量菜单
2. **激活状态无服务端持久化**：后端 `system_config` 无"激活模板"配置；`applyTemplate` 只写 localStorage，多终端不一致
3. （连带发现）**system_config 表契约断裂**：V20260717_020 已将列统一为 create_time/update_time，实体 SystemConfig 仍映射 created_at/updated_at → MyBatis-Plus 写入报错（税务平台配置保存同病）

### 修复
- 后端：`SystemConfigController` 新增 `GET/PUT /v1/system-config/permission-template`（无配置默认 centralized-single）；migration `V20260802_001` 插入默认配置 `permission.active_template=centralized-single`
- 后端：migration `V20260802_002` 修复 `system_config.encrypted` SMALLINT→BOOLEAN（幂等写法 `USING (encrypted::int <> 0)`）；`SystemConfig` 实体加 `@TableField("create_time"/"update_time")`
- 前端：`permission-template.ts` 加 `getActiveTemplate/setActiveTemplate`；`permission.ts` `initFromToken` 无本地模板时读服务端激活模板并写 localStorage；`applyTemplate` 同步 PUT 持久化（失败仅 warn）
- 迁移顺序坑：当前库已手动执行 ALTER 时，`encrypted <> 0`（boolean vs int）报 42883，必须用 `encrypted::int <> 0`

### 验证（Playwright 22/22 通过）
- 全新浏览器上下文登录 admin：显示 11 个菜单组（工作台/门店/产品/订单/运营/会员/追溯/设备/系统/财务/采购），隐藏 人事管理/仓储管理/资产管理
- localStorage menu-template=centralized-single；刷新后仍生效；权限中心页显示"集中式单店模式 当前使用"
- API：PUT/GET permission-template 均 200，`mvn compile` 与 `npm run build` 通过

### 待办（用户决策）
- ~~驳回次数限制冷却时长：24h 过长建议改"3 次置灰 + 管理员重置"~~ → 已定案并实施（见批 23）

---

## 2026-08-02 批 23：业务链对齐权限模板 + 驳回次数限制（A1 定案）

> 背景：集中单店模式"一人身兼多职"（夫妻店/自营+雇店长），角色权限无法像连锁模式清晰分离；
> 用户要求流程简化 + 数据安全风险提醒；驳回限制语义澄清：限制"被驳回单据的再次提交"而非驳回次数本身。

### 业务链模型（本次建立）
```
营业链: product + order + store-ops + member + operations
供应链: product + purchase + store-ops        ← 铁三角（此前断链点）
财务链: purchase + finance
追溯链: traceability
支撑链: device + asset + hr + system
```
规则：任一角色开放 product/purchase/store-ops 中任一域，则三域必须全开，否则"商品档案→采购订单→到货→门店收货→库存→菜品明细"断链。

### 改动
- **DB**：3 个系统模板 roleConfig 重写（SQL 直接 UPDATE permission_templates）：
  - centralized-single：`ops_director` 补 product/purchase/member；新增 `store_manager`（营业+供应链+追溯）、`team_leader`、`employee` 条目；`finance_director` 补 purchase
  - standard-chain / large-chain：`ops_director` 补 product/store-ops/member；新增 `store_manager`（营业链，采购归总部）、`team_leader` 条目
- **前端菜单 visibleRoles**：`purchase` 组 +STORE_MANAGER；`store-management` 组 +OPS_DIRECTOR（链完整性）
- **前端兜底模板**（permission.ts getFallbackTemplates）与 DB 同步；`applyTemplate` 新增 `validateChainIntegrity`（铁三角断裂 → warn + 审计 TEMPLATE_APPLIED，不拦截自定义场景）
- **A1 驳回限制**（采购申请）：
  - migration `V20260802_003`：`purchase_request.reject_count INT NOT NULL DEFAULT 0`
  - 后端：实体/DTO 加 rejectCount；`approve(rejected)` 时 +1；`submit` 时 rejectCount≥3 拒绝（"该申请已被驳回 3 次，已限制重新提交，请联系管理员重置后可继续提交"）；新增 `POST /v1/purchase/requests/{id}/reset-reject`（purchase:request:approve 权限）
  - 前端：状态列显示"已驳回 N/3"标签（≥3 红色 warning）；"重新提交"≥3 置灰 + tooltip；admin 显示"重置驳回"按钮 + 二次确认
- **单店流程简化原则**（后续实施）：同人自审自动通过 + 资金/删除操作二次确认 + 审计留痕；数据链不砍，只合并操作入口

### 验证
- API 全链路：创建→提交→驳回×3（计数 1→2→3）→ 第 4 次提交被拦截（code 8106）→ reset-reject → 计数归 0 → 提交成功
- 模板 GET /v1/permission-templates/system 确认三模板 roleConfig 生效；激活模板 API 正常
- Playwright 22/22 通过；`mvn compile`、`npm run build` 通过；Flyway 3 个 V20260802 迁移全部 success
- 测试数据已清理

### 注意
- 审批状态机：rejected 状态不能直接 approve，必须 submit 回 pending 再审批（测试中踩到，业务正常流程）
- 驳回计数对象为"同一单据"，被锁单据的提交者可新建申请单重新提报（业务不阻塞）
