# D-IG Inventory Semantic Reconstruction 001

# A1 Local Engineering Evidence Report

> **状态**: ACTIVE_REFERENCE — LOCAL ENGINEERING EVIDENCE（本地实地探查证据报告）

> **Attack**: A1 — Inventory Balance Grain / Locator Uniqueness

> **Target**: `03-review/d-ig-inventory-semantic-reconstruction-001.md`

> **Responds to**: `03-review/d-ig-inventory-semantic-reconstruction-a1-grain-review-001.md`（INCONCLUSIVE_PENDING_LOCAL_EXECUTION → 本报告即其要求的 local execution）

> **Governance baseline**: `5923e7e585e718cf9ff1bfbec0380369ec85be32`

> **Production Evidence**: BLOCKED（未使用任何生产证据；本文全部 DB 证据为本地实例）

> **Identity Decision**: NOT_MADE · **Schema Authorization**: NO · **Migration Authorization**: NO

---

## 1. Evidence Baseline

```text
governance_repo      = wukaiktc1234/new-map
governance_baseline  = 5923e7e585e718cf9ff1bfbec0380369ec85be32
governance_HEAD_at_report = 05f1eba817167673179bda3be00d5653688c8a7b (main)
engineering_worktree = 本地真实工程 worktree（与治理仓 origin 同名但历史分叉，见 §2）
```

## 2. Engineering Git State

```text
git rev-parse HEAD          = 6b54411d4833f4e832b347567ecfddf7e72949a1
git branch --show-current   = master
git status --short          = 811 个已修改/未跟踪条目
WORKTREE_DIRTY              = YES
```

WORKTREE_DIRTY 修改面分布（顶层目录计数）：backend、frontend、employee-frontend、miniprogram、docs、配置等。**未做任何清理 / stash / commit**（依指令）。本报告全部 [CODE] 证据取自该 dirty worktree 的当前工作区内容；证据可由后续审计者在同一 commit + 同一份未提交内容下复核。

## 3. Database / Migration Baseline

```text
database type            = PostgreSQL 18.3 (x86_64-windows)
schema identifier        = food_traceability（public，本机实例）
snapshot timestamp       = 2026-09-13 16:16:21 +08（SELECT now()）
migration head           = 20260909.003 — finance record add order code（flyway_schema_history, success=true 最大 installed_rank）
DB_SNAPSHOT_IDENTITY     = 本地实例快照（非生产；凭据不记录）
PROD                     = BLOCKED（生产不可达）
```

inventory 相关 migration 基线（[MIGRATION]，文件均位于 `backend/src/main/resources/db/migration/`）：

| 文件 | 与 inventory 的关系 |
|---|---|
| V1.0.0.100__init_postgresql.sql:390-446 | 建表（仅 product_id/product_name；无 material_id；product_id 仅索引） |
| V20260627_001__create_inventory_adjust_outbound_tables.sql | 调整/出库单表 |
| V20260629_004/005（material_archives/categories） | Material referent 主数据 |
| **V20260629_015__create_warehouses_table.sql** | **warehouses 主数据表（新事实：见 §7.3）** |
| V20260717_012:316-329 | 条件创建 fk_inv_product_id |
| V20260717_030:390 / V20260731_008:13 | material_trace_code.material_id BIGINT→回退 VARCHAR(64) |
| V20260730_001:46-62 | **追加 inventory.material_id**（无 FK/无唯一约束）+ status/version |

## 4. Search Scope

| 搜索维 | 关键词 / 范围 | 结果位置 |
|---|---|---|
| Inventory ID | `Inventory::get…`（lambda 枚举、穷举）、`getById`、`updateById`、`deleteById` | §5/§6/§16 |
| Material | `getByMaterialAndWarehouse`、`Inventory::getMaterialId`（49 处引用点全枚举）、`material_id` | §6/§7 |
| Product | `Inventory::getProductId`（**全仓 2 处**）、`getProductId|setProductId`、XML `product_id` | §6/§15 |
| Warehouse | `Inventory::getWarehouseId`、`getByMaterialAndWarehouse`、warehouses 表 | §6/§7 |
| Store | `Inventory::getStoreId`（**全仓 1 处**）、`StoreInventory::get*`（11 处）、`getByStoreAndMaterial` | §6/§8 |
| Mapper/XML | `resources/mapper/*.xml` 全量扫 `inventory`；`InventoryMapper.xml` 全文 | §6/§16 |
| 注解 SQL | `@Select/@Update/@Insert/@Delete` 含 inventory | §16（0 命中直写） |
| Native | `JdbcTemplate/nativeQuery/createNativeQuery`（config 包 6 类） | §14 |
| Scheduler/Event | `@Scheduled`（8 处）、`RabbitListener/KafkaListener/@EventListener/@Async` | §14 |
| DTO | InventoryDeductDTO/IncreaseDTO/DecreaseDTO/LockDTO 字段全录 | §6.3 |

## 5. Inventory Write Paths（Inventory Write Path Inventory）

| ID | Path（file:line） | Entry Point | Locator | Fields Written | Active? | Business Context |
|----|---|---|---|---|---|---|
| W1 | `service/impl/InventoryServiceImpl.java:199-270` increaseInventory | PurchaseStockinServiceImpl.confirmStockin:329→:734；InventoryController /increase；MaterialArchive ensure | `(materialId, warehouseId)`（getByMaterialAndWarehouse + 乐观锁重查） | material_id/warehouse_id/location_id/current_stock/batch_no/unit_cost/total_cost/status/version + 台账 | **ACTIVE** | 采购入库/手工入库/建档初始化 |
| W2 | `InventoryServiceImpl.java:278+` decreaseInventory | Loss:155 / Outbound:299 / Adjust:360 / VoidStockin:786 / PurchaseReturn:455 / TraceCode:150 | `(materialId, warehouseId)` | current_stock/total_cost/status/version + 台账 | **ACTIVE** | 全部出库类 |
| W3 | `InventoryServiceImpl.java:80-147` lockInventory/unlock/deduct | InventoryController /deduct 等 | **inventory.id**（`getById(dto.getInventoryId())`:84,152） | locked_quantity/current_stock | **ACTIVE** | 锁定/指定行扣减 |
| W4 | `service/impl/SalesOrderServiceImpl.java:257-281` deductInventoryForOrder | completeOrder:143 | 尝试 `(Inventory::getProductId, Inventory::getStoreId)` | current_stock | **DEAD**（§9 三重静态证明） | 销售完成出账 |
| W5 | `service/LossOutboundService.java:122-142`（approve:105 调用） | LossOutboundController approve | 尝试 `(Inventory::getProductId, warehouseId)`；insert 兜底经别名 | current_stock | **ACTIVE entry；locator 运行期必然失败**（§15-B） | 报损审核出库 |
| W6 | `service/OtherInboundService.java:94-120` updateInventory + insert:120 | 其他入库 create | `(materialId, warehouseId)`（:96-98，注释"修复：使用getMaterialId替代getProductId"） | current_stock | **ACTIVE** | 其他入库 |
| W7 | `service/impl/InventoryTransferServiceImpl.java:292-323` | 调拨 | `(materialId, warehouseId)`（:295-296） | current_stock | **ACTIVE** | 调拨 |
| W8 | `service/purchase/impl/MaterialArchiveServiceImpl.java:364-395,405-460` ensure/sync/status | 建档/编辑 | `(materialId[, warehouseId])` → updateById(id) | 零余额行/material_name/code/category/status | **ACTIVE** | 主数据→库存同步 |
| W9 | `service/impl/MaterialTraceCodeServiceImpl.java:134-146,417-428` | QuickStockInController /execute、扫码 | **无持久化**（build-and-log：`new Inventory()` 后无任何 save/insert，仅 log.info + store_inventory_log） | — | **DEAD-CODE**（构建后弃置） | 追溯扫码"库存" |
| W10 | `service/impl/HardwareDeviceServiceImpl.java:60-90` | 硬件扫码核销 | **inventory.id**（InventoryCode→`getById(inventoryId)`） | current_stock（−1） | **ACTIVE** | 设备扫码出库 |
| W11 | `config/InventoryWarningScheduler.java:43（cron 0 0 * * * ?）,84-164` | 调度 | selectList(阈值+material/warehouse/expiry) → `updateById(id):164` | status（=3 冻结等） | **ACTIVE**（每时） | 预警状态 |
| W12 | `service/impl/StoreInventoryServiceImpl.java:89-121+` increase/decrease/getByStoreAndMaterial | stockin:727 / transfer:226+ / arrival:391 / return:460 / OrderNew:636,756,762 / Controller | `(storeId, materialId)` | current_stock/unit_cost | **ACTIVE** | 门店余额 |
| W13 | `service/impl/MaterialConsumptionServiceImpl.java:125-136` deductInventory | **无任何创建调用方**（全仓 0 处 `new MaterialConsumption()` 之外的调用者） | 无（仅置 inventoryDeducted=1 flag，不动余额） | 无 | **DEAD** | 消耗扣减 |
| W14 | `config/ExpiryCheckScheduler.java` | 调度 | 读 inventory（expiry）→ 只写 warning_rule/record 表 | 不写 inventory | **ACTIVE（只读 inventory）** | 效期 |
| W15 | `service/impl/TraceCodeServiceImpl.java:144-152` | 追溯码出库 | `(materialId ← traceCode.getProductId()→target_id, warehouseId)` | current_stock（−1，经 W2） | **ACTIVE** | 追溯出库 |

台账（inventory_transactions）唯一写入点 = W1/W2 内 `recordTransaction`（全仓 `InventoryTransaction` 引用仅 entity/mapper/InventoryServiceImpl 三处）。inventory 无 deleteById（软删仅实体字段，无调用方）。

## 6. Inventory Locator Paths

### 6.1 全量 lambda 字段引用枚举（`Inventory::get*` 49 处 / `StoreInventory::get*` 11 处，逐文件归类）

- `Inventory::getMaterialId` ×15、`getWarehouseId` ×5、`getStoreId` ×1（仅 W4）、`getProductId` ×2（仅 W4/W5）、`getQuantity/getStatus/getExpiryDate/getMinSafeQty/getSafetyStock/getDeleted/getMaxStockQty/getMaterialName/getCreateTime`（读/阈值条件）
- **所有余额写入/定位包装器的字段组合仅有三种**：`(materialId, warehouseId)`、`(materialId)`（主数据同步，W8）、`(productId→列缺失, storeId→列缺失)`（仅死路径 W4/W5）；加上 id 直查（W3/W10/W11）。

### 6.2 关键 locator 判答（§六 十问逐条）

1. 定位哪个 balance：inventory（中央）或 store_inventory（门店）行；2. 字段：见上；3. 是否以 inventory.id 为最终 identity：W3/W10/W11 是（行已存在，等价于 grain 产出物）；4. `(materialId, warehouseId)`：W1/W2/W6/W7/W14/W15 是；5. `(storeId, materialId)`：W12 是（且有唯一索引 uk_store_inventory_store_material，pg_indexes 实证）；6. Product 为 locator：**无任何 active 路径**（仅死路径尝试）；7. text-only locator：仅读路径（selectInventoryPage 的 materialName/batch_no LIKE；StoreForecast 读 store_inventory_log）——**只读，不改余额**；8. 其他复合 locator：无；9. active：已标注；10. 影响余额：W1-W3/W5-W8/W10-W12/W15 影响；读路径不影响。

### 6.3 DTO 面

`InventoryIncreaseDTO{materialId, warehouseId, locationId, quantity, unitCost, transactionType, batchNo}`；`InventoryDecreaseDTO{materialId, warehouseId, …}` → 粒度键；`InventoryDeductDTO{inventoryId,…}`、`InventoryLockDTO{inventoryId,…}` → 行 ID。

### 6.4 Mapper/XML

`InventoryMapper.xml` 仅两条 select：`selectInventoryPage`（deleted=0 + 可选 warehouseId/materialId + **materialName LIKE / batchNo LIKE**，只读列表）与 `selectByMaterialAndWarehouse`（`WHERE deleted=0 AND material_id=#{materialId} AND warehouse_id=#{warehouseId} LIMIT 1`）。**XML 中不存在任何对 inventory 的 UPDATE/INSERT/DELETE**（全 10 个 XML 扫描，§16）。

## 7. Central / Warehouse Grain Evidence

### 7.1 代码层

- 粒度键执行点：`getByMaterialAndWarehouse`（InventoryMapper.xml:51-56 + InventoryServiceImpl 调用/乐观锁重查）；W1/W2/W6/W7/W14/W15 全部一致。
- `batch_no` 为末次写入属性（W1: `current.setBatchNo(dto.getBatchNo())`），**不参与粒度**—— reaffirmed。
- 中央侧无 `(material_id,warehouse_id)` 唯一约束：pg_constraint 实测（仅 PK/NOT NULL/2×CHECK/fk_inv_product_id）[DB-SCHEMA]。

### 7.2 数据层 [LIVE-REPRO]（快照 2026-09-13 16:16+08）

- inventory 活跃 11 行：material_id 11/11（100%）；**不存在任何 material_id 为 NULL 的行**（与 W9 从未持久化一致）；product_id 1/11；双列并存 1（inventory_id=4 异指，FACT）。
- batch_no 4/11、expiry 0/11、store_id 0/11。
- 悬空：material_id=999999（1 行）；2 行引用软删物料。
- warehouse referent 现可解析：**warehouses 表存在（10 行），10/11 活跃行 warehouse_id 可解析**（1 行不可解析）。

### 7.3 新事实：warehouse 主数据已存在（对既有记录的修正）

`V20260629_015__create_warehouses_table.sql` 已建 `warehouses`（10 行），`OtherInboundService/LossOutboundService` 均 `warehouseMapper.selectById` 校验。**DEC-001 时代"仓库无独立表/引用目标不明"的表述被本地现实 SUPERSEDE**——中央 grain 的 warehouse 腿不再是悬空引用。此项不改变 grain 定义，仅强化其 referential soundness。

## 8. Store Grain Evidence

- 定位：`getByStoreAndMaterial(storeId, materialId)`（StoreInventoryServiceImpl:82-83 wrapper eq）唯一键路径；W12 全部调用方一致。
- DB：唯一**索引**（非约束）`uk_store_inventory_store_material ON (store_id, material_id)`（pg_indexes 实测）；PG 主键 id。
- 分布 [LIVE-REPRO]：store_only 7 / warehouse_only 2 / both 7 / divergent 4（与 BE-03 一致）。
- 模糊因素（不改变 locator grain，记录为数据质量矛盾）：采购入库/调拨同步门店时**以 warehouseId 充当 storeId**（PurchaseStockinServiceImpl:713-715、InventoryTransferServiceImpl:226,245 注释成文约定）——store_inventory_log.store_id=584 的成因。

## 9. SalesOrder Trace（重点攻击项）

完整调用链（[CODE]，file:line 全录）：

```text
SalesOrderController
  → SalesOrderServiceImpl.completeOrder(:137-146)
    → salesOrderMapper.selectById(id)            /* SalesOrder @TableName("orders") */
    → if (!"preparing".equals(order.getStatus())) return false; /* :141-142 */
    → salesOrderMapper.updateById(order)          /* status 为 @TableField(exist=false)，不参与 SQL */
    → deductInventoryForOrder(id)                 /* :143 */
      → salesOrderMapper.selectOrderDetailList   /* SalesOrderMapper.xml:83-85: SELECT * FROM order_items WHERE order_id= */
      → for detail: inventoryQuery
          .eq(Inventory::getProductId, detail.getProductId()) /* :267 */
          .eq(Inventory::getStoreId, storeId)                 /* :268 */
      → inventoryMapper.selectOne → updateById   /* 从未到达 */
```

**三重独立死因（全部静态可证）**：

1. **入口门不可达**：`SalesOrder.status` 为 `@TableField(exist = false)`（SalesOrder.java:64-66，注释"B-1 BLOCKED…exist=false 不参与 SQL"）→ selectById 后 getStatus() 恒为 null → `"preparing".equals(null)` = false → **completeOrder 恒 return false，:143 永不执行**。

2. **lambda 列无法解析**：`Inventory` 实体**不存在 productId 字段**（getProductId 仅是 materialId 别名，Inventory.java:382-384）；`storeId` 为 `transient` 字段，而 MyBatis-Plus 3.5.5 官方源码 `ReflectionKit.java:149-151` 明证 **static 与 transient 修饰字段被过滤出实体字段列表**（列映射来源）→ 两属性均不在列缓存 → wrapper 列解析必然失败（MP 3.5.5 行为；确切异常面未运行验证，见 §21-R2）。

3. **定位值域错误**：即使前两条不成立，`SalesOrderDetail.productId` 映射 `order_items.food_id`（SalesOrderDetail.java:33-35，注释"T-7：productId → order_items.food_id"），本地 6/6 为 NULL——定位值为 NULL/food 值域，非 inventory.material_id 值域。

旁证：`DatabaseFixConfig.java:623-626` 对 `purchase_stockin.inventory_code` 的 `'ICnull-'` 前缀修复，证明历史上曾有 NULL productId 值流入编码生成链。

**A1 判定**：W4 = DEAD（不可达 + 解析失败 + 值域错位），**不构成 active materially-different locator**。`A1 review F-A1-02 的 UNRESOLVED 就此闭合`。

## 10. TraceCode Trace

- **W9（MaterialTraceCodeServiceImpl:134-146/417-428）**：构建 Inventory 对象（productName/storeId/inventoryType 均为 transient，quantity/unit 有映射）后**无任何持久化调用**（该文件 `inventoryService` 全部引用仅 :36/:39/:46/:134/:417；134/417 两处均为 build→log→弃置），随后仅写 `store_inventory_log`（product_id 列存物料值，16/16 [LIVE-REPRO]）。**此前 Reconstruction §10-C8"trace 扫码创建无 referent 库存行"被本证据推翻：从未创建。** 候选"Unknown/无 referent 行"与 Owner 问题 N1 的前提消失（修正建议见 §19）。
- **W15（TraceCodeServiceImpl:144-152）**：追溯出库经 `(materialId ← traceCode.getProductId(), warehouseId)` 走 W2——locator 字段符合 grain；**值 provenance**：`TraceCode.setProductId(Long) → this.targetId`（TraceCode.java:389-390，trace 域 target_id 值）——referent 完整性问题，**非 A1 grain FAIL**（与 F-A1-04 判定一致，本报告以完整调用链将其闭环为"field-conforming / value-provenance caveat"）。

## 11. Purchase / Inbound Trace

```text
PurchaseStockinController → PurchaseStockinServiceImpl.confirmStockin(:298)
  → increaseInventoryForStockin(:711)
    → InventoryIncreaseDTO{materialId=item.materialId, warehouseId, locationId, quantity, unitCost, batchNo, referenceType="purchase_stockin"}
    → InventoryServiceImpl.increaseInventory  /* (materialId, warehouseId) 粒度 ✓ */
    → storeInventoryService.increaseStock(storeIdForSync=warehouseId, materialId, …)  /* 门店同步；warehouseId 充当 storeId（:713-715 注释）；失败回滚（DF-001） */
```

Locator 完全符合中央/门店双 grain；作废回滚（:430/:771-786）同样走 decreaseInventory（grain ✓）。

## 12. Loss / Outbound Trace

- LossOutboundController（active entry）→ create：insert loss_outbound（referent=product 表校验）→ **approve(:105)**：`updateInventory(productId, warehouseId, −qty)` → wrapper `.eq(Inventory::getProductId, …)`:124 → **列缓存必然未命中（§15-B）→ 运行期在定位前失败** → loss 单据无法变为 approved（本地 loss_outbound 0 行、inventory_losses 0 行 [LIVE-REPRO]，无反例）。insert 兜底分支同样经别名写 material_id 值域（且同样不可达，因为在 selectOne 已抛出）。
- InventoryLossServiceImpl:155 / InventoryOutboundServiceImpl:299：materialId+warehouseId 粒度 ✓（这两条才是"报损/出库"的活跃实现）。

## 13. Consumption Trace

`MaterialConsumptionService.deductInventory(consumptionId)` = 仅 `consumption.setInventoryDeducted(1)` + updateById（MaterialConsumptionServiceImpl:125-136），**不触碰任何 inventory 行**；且全仓**不存在** MaterialConsumption 记录的创建调用方（0 处）；实体按 product_id 风格映射而表无 product_id 列（schema 错位）；本地 0 行。**如果**未来某路径真实扣减，也只能经由 W2 粒度键或 W3 行 ID——当前不存在其他 locator。F-A1 / A2 交叉依赖保留。

## 14. Scheduler / Event / Batch / Sync Trace

| 项 | 写 inventory？ | 明细 |
|---|---|---|
| InventoryWarningScheduler（cron 每时） | **是（status）** | selectList（quantity/safetyStock/material/warehouse/expiry 阈值）→ `updateById(id):164`（status=3 等）——id 等价 |
| ExpiryCheckScheduler（cron 每日 2 点） | 否（只读 inventory） | 写 expiry_warning_rule/record |
| AuditLog / Churn / Coupon / FileCleanup 等 | 否 | 与 inventory 无关 |
| ReceiptConfirmationEventListener（ApplicationEvent，JVM 内） | 否 | 仅生成 material_trace_code（materialId 入参；一臂可写 null，[R-DOC] C002-R3） |
| RabbitMQ/Kafka listener | **全仓 0** | 无 MQ 旁路 |
| JdbcTemplate 配置类（DatabaseFix/DataFix/KitchenOrderMigration/TraceCodeSystem 等） | 否（对 inventory 而言） | 仅 schema 初始化/校验与 `purchase_stockin.inventory_code` 修复（:623） |
| 数据同步 | 无 MQ/远端同步路径 | — |

## 15. ProductId Compatibility Analysis

对全部 `productId` 出现点按 A（真写 inventory.product_id）/ B（别名→materialId）/ C（DTO/销售域字段）/ D（真参与 inventory SQL locator）四分类：

| 出现点 | 分类 | 证据 |
|---|---|---|
| Inventory.getProductId()/setProductId() | **B** | Inventory.java:382-388（别名方法，无对应字段） |
| SalesOrderServiceImpl:267 | D-attempted → **失败**（属性不在列缓存） | §9 |
| LossOutboundService.java:124 | D-attempted → **失败**（同上） | §12 |
| SalesOrderDetail.productId | **C** | @TableField("food_id")（order_items 列），SalesOrderDetail.java:33-35 |
| OtherInboundService dto.getProductId | **C→B** | product 表校验（:58）→ setMaterialId 落库（:108-110）；locator 用 getMaterialId（:96）✓ |
| LossOutbound 实体.productId | C（loss_outbound 表自有列，非 inventory） | loss_outboundMapper |
| TraceCode.setProductId() | **B（trace 域）** | TraceCode.java:389-390 → targetId（target_id 列） |
| StockForecastMapper:30 `WHERE product_id=#{materialId}` | 读别名（store_inventory_log 表，非 inventory） | [CODE] |
| InventorySummaryMapper.xml:23,42 `material_id AS product_id` | 读别名 | [CODE] |
| inventory.product_id 物理列 | **A 类出现次数 = 0**（当前代码无任何写入点；仅 V1.0.0.100 时代/手工数据残留 1 行） | [CODE]+[DB-SCHEMA] |

**结论：不存在 A 类活跃路径；D 类两处均静态证明失败。**

## 16. Negative Search

| 搜索 | 命中 | 判定 |
|---|---|---|
| XML 中 `UPDATE/INSERT/DELETE FROM inventory`（直写主表） | **0** | 无 XML 写路径 |
| `@Select/@Update…inventory` 注解 SQL 直写主表 | **0**（InventoryMapper.xml 2 条只读；StockForecastMapper 读 store_inventory_log） | 无 |
| `inventoryMapper.deleteById` | **0** | 无物理删除路径 |
| `RabbitListener/KafkaListener` | **0** | 无 MQ 旁路 |
| `Inventory::getProductId` | **2**（SalesOrder:267、LossOutbound:124）→ 均死 | §9/§12 |
| `Inventory::getStoreId` | **1**（SalesOrder:268）→ 死 | §9 |
| `sales_order%` 表 | **0**（实体对齐 orders/order_items） | §9 |
| migration 中 `INSERT INTO inventory/product` 种子 | **0** | 数据非种子 |
| JdbcTemplate 配置类写 inventory 余额 | **0**（仅 purchase_stockin 编码修复与建表校验） | §14 |
| `new Inventory()` | 6 文件，全部归类（W1/W5/W6/W7/W8/W9），其中 W9 不持久化 | §5 |
| `inventoryService.save/saveOrUpdate/updateById` 外部调用方 | MaterialArchive×3、HardwareDevice×1，均已归类 | §5 |
| 其他 `FROM inventory` 读（summary/expiry/forecast/dashboard） | 均只读 | §6.4 |

## 17. Evidence Matrix

| ID | Source | Entry Point | Locator | Operation | Active Status | Business Relevant | Materially Different | Evidence | Result |
|---|---|---|---|---|---|---|---|---|---|
| W1 | [CODE]+[LIVE-REPRO] | confirmStockin:329 / /increase | (materialId, warehouseId) | increase/upsert+ledger | ACTIVE | YES | NO | InventoryServiceImpl.java:199-270; InventoryMapper.xml:51-56 | CONFORM |
| W2 | [CODE] | 6 个出库入口 | (materialId, warehouseId) | decrease+ledger | ACTIVE | YES | NO | InventoryServiceImpl.java:278+ | CONFORM |
| W3 | [CODE] | /deduct、lock | inventory.id（DTO） | lock/deduct | ACTIVE | YES | NO（行 ID=grain 产出物，等价） | InventoryServiceImpl.java:80-147; DTO 字段 | EQUIVALENT |
| W4 | [CODE] | completeOrder:143 | (productId?, storeId?) | decrease | **DEAD**（门不可达+列缺失+值域错） | YES（设计意图） | attempted-but-inert | SalesOrderServiceImpl.java:137-281; SalesOrder.java:64-66; SalesOrderDetail.java:33-35; ReflectionKit.java:149-151 | NOT_A_DIFFERENT_ACTIVE_LOCATOR |
| W5 | [CODE]+[LIVE-REPRO] | approve:105 | (productId?, warehouseId) | decrease/insert | ACTIVE entry / **locator 解析必然失败** | YES | attempted-but-inert | LossOutboundService.java:105-142; loss_outbound 0 行 | NOT_A_DIFFERENT_ACTIVE_LOCATOR |
| W6 | [CODE] | 其他入库 | (materialId, warehouseId) | upsert | ACTIVE | YES | NO（locator ✓；值 provenance=product 域，属 referent 层） | OtherInboundService.java:94-120 | CONFORM |
| W7 | [CODE] | 调拨 | (materialId, warehouseId) | ± | ACTIVE | YES | NO | InventoryTransferServiceImpl.java:292-323 | CONFORM |
| W8 | [CODE] | 建档/编辑 | (materialId[, warehouseId])→id | 零行/元数据 | ACTIVE | YES | NO | MaterialArchiveServiceImpl.java:364-460 | CONFORM |
| W9 | [CODE] | QuickStockIn/execute | 无 | build-only（未持久化） | **DEAD-CODE** | NO（无效果） | NO | MaterialTraceCodeServiceImpl.java:134-146,417-428 | NO_PERSISTENCE |
| W10 | [CODE] | 硬件扫码 | inventory.id（InventoryCode→id） | −1 | ACTIVE | YES | NO（等价） | HardwareDeviceServiceImpl.java:60-90 | EQUIVALENT |
| W11 | [CODE] | cron 0 0 * * * ? | 阈值筛选→id | status 写 | ACTIVE | YES | NO（元数据/状态） | InventoryWarningScheduler.java:43,84-164 | EQUIVALENT |
| W12 | [CODE]+[DB-SCHEMA] | 入库/调拨/退货/订单/Controller | (storeId, materialId) | ± | ACTIVE | YES | NO（唯一索引佐证） | StoreInventoryServiceImpl.java:82-121; pg_indexes | CONFORM |
| W13 | [CODE]+[LIVE-REPRO] | 无调用方 | 无（flag-only） | — | **DEAD** | NO | NO | MaterialConsumptionServiceImpl.java:125-136 | NO_EFFECT |
| W14 | [CODE] | 盘点/调整 | materialId(,warehouseId) | ± | ACTIVE | YES | NO | InventoryAdjustServiceImpl.java:331-360 | CONFORM |
| W15 | [CODE] | 追溯出库 | (materialId←target_id 值, warehouseId) | −1（经 W2） | ACTIVE | YES | NO（字段符合；值 provenance 登记为 referent 层问题） | TraceCodeServiceImpl.java:144-152; TraceCode.java:389-390 | CONFORM (caveat) |
| R-组 | [CODE] | 分页/汇总/预警/效期/预测 | 含 text LIKE / 别名列 | **只读** | ACTIVE | YES | NO（不改余额） | InventoryMapper.xml:28-49; InventorySummaryMapper.xml; StockForecastMapper.java:30 | READ_ONLY |

## 18. A1 Result

**A1_RESULT = PASS**

判定依据（对照 a1-grain-review-001 §6 三测试）：

- **PASS 测试**：在已声明证据范围（本工程 baseline 6b54411 + dirty worktree + DB 快照 2026-09-13）内，已完成 source-level 穷举定位面（§4/§16）：全部 inventory 余额写入/定位路径 = 中央 `(materialId, warehouseId)`（W1/W2/W6/W7/W14/W15）、门店 `(storeId, materialId)`（W12）、行 ID 等价访问（W3/W10/W11）与只读路径（R-组）。唯二尝试不同 locator 的路径（W4/W5）被完整调用链静态证明**不能到达任何余额行**；W13/W9 无任何余额效果。**未发现 active + business-relevant + materially different 的 locator。**
- **FAIL 测试**：不满足——无一条被证明 active 且实质不同的路径（W4 三重死因、W5 定位前失败、W9/W13 无效果）。
- **F-A1-02（SalesOrder）闭合**；**F-A1-03（覆盖面）由本报告满足**；F-A1-04 维持（TraceCode 不构成 FAIL，且本报告进一步证明 W9 从未持久化）。

```text
A1_RESULT = PASS
UNRESOLVED_ATTACK = NO
LOCAL_CONCLUSION_STRENGTH = HIGH（source-level 穷举 + 同基线 DB 快照；运行期执行未演示，见 §19）
RECONSTRUCTION_CORE_CREDIBILITY = RESTORED_FOR_A1（grain 主张在 A1 维度成立；条件与重跑触发见 §19/§21）
```

## 19. Unresolved Attacks（最强反证与残余攻击——PASS 亦必须列出）

1. **MP 运行期行为的最后一块（最强反证）**：W4/W5“必然失败”的判定依赖“MP 3.5.5 列缓存缺失即抛错”的框架行为（字段缺席 + ReflectionKit.java:149-151 transient 过滤 = 源码实证；确切异常断言行未能二次抓取，未运行演示）。**若该假设被证伪**（例如运行期实际解析成功），W5 将立刻变为“以 product 域值定位 material_id 行”的 active 异指 locator → A1 须重开并很可能转 FAIL。这是 PASS 的单点假设，已列入 R2。
2. **FK 不对称未被任何规则解释**：全表唯一 FK 指向 product 表（fk_inv_product_id 已创建 [DB-SCHEMA]），material_id 无 FK——A1 只证明 locator 字段唯一，**不证明 product_id 列在业务上无意义**（该裁决仍属 Owner Q1/Q3；A1 与之正交）。
3. **值 provenance 缺陷清单**（不属于 grain，但影响 referent）：W6/W15/LossOutbound 兜底均把 product/trace 域值写入 material_id 值域；material_id=999999 悬空；2 行引用软删物料。
4. **warehouse-as-storeId 成文约定**：门店同步的 store 维度被仓库键填充（C5）——locator 字段符合 grain，但空间 referent 混淆，属 A3/数据质量轨道。
5. **生产不可达**：全部证据为本地实例；生产侧分布、product_id 真实填充率（BE-01 生产值）未知。

## 20. Cross-Attack Dependencies

- **对 A2（消费/台账）**：本报告证明 W13 死亡、台账唯一写入点=recordTransaction——A2 的“消耗经未识别 locator 更新库存”分支已被排除；A2 只需覆盖 W2 的六类 decrease 入参 provenance。
- **对 A3（storeId/inventoryType 持久化）**：ReflectionKit transient 过滤（源码实证）+ store_id 0/11、inventory_type 全 1（LIVE-REPRO）双证——若 A3 发现物理持久化旁路则按 a1-grain-review §9 重开 A1。
- **对 Owner Q1/Q3**：A1 = 字段级 locator 唯一性；**不得**被引用为“material_id 是业务真相”的证据（FK 不对称与别名链仍待 Owner 裁决）。
- **对 C-002**：E2/E4 inventory 段的 conditional 状态不变；本报告移除的是 A1 层不确定性，不是 business-truth 裁决。

## 21. Recommended Rerun Conditions

- **R1**：生产证据显示 `inventory.product_id` 存在活跃业务写入 → 立即重开 A1（并触发 candidate re-entry T2）。
- **R2**：MP 列缓存行为假设被证伪（例如观察到 LossOutbound approve 成功修改余额）→ 重开 W5（并重估 W4）。
- **R3**：新增/变更 inventory 相关 migration（唯一约束、新列、新表）或 PD-017/020 落地使 SalesOrder status 参与持久化 → 重跑受影响路径（W4 可能复活）。
- **R4**：出现新的 scheduler/event/MQ 写入方或新模块引用 Inventory → 扩充 §5 矩阵。
- **R5**：DB 快照口径漂移（store_id/product_id 填充率显著变化）→ 重跑 §7/§8 数据层证据。

## 附：对 Reconstruction 001 的修正建议（不改其文件，仅登记）

1. §10-C8/候选“Unknown/无 referent 库存行”降级：trace 扫码路径**从未持久化**任何 inventory 行（build-and-log），相关候选与 Owner 问题 N1 的前提消失。
2. §9“DEC-001 仓库无独立表”：被 V20260629_015（warehouses，10 行；10/11 可解析）SUPERSEDE。
3. §5 写路径清单增补：W3（deduct/lock by id）、W10（HardwareDevice 扫码 by InventoryCode→id，**无台账记录**——建议移交台账完整性轨道）、W11（scheduler status 写）。

---

*脱敏声明：本报告不含密码/token/连接串/客户数据/真实订单内容；所引“娃娃菜/测试物料A”等为本地实例既有测试物料名（已见于治理包既有记录），仅用于证据可复核性。未提交任何源码文件。*