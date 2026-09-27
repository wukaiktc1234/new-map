# M3-M4 库存链考古矩阵（inventory-chain-archaeology-matrix-001）

- **日期**：2026-09-27 · 方法：只读静态考古（方法/SQL 语句粒度）· 性质：M3-M4 实施前置参考
- **基线**：location-organization-separation-design-001/-002.md + `m3m4-preflight/implementation-constitution-001.md`
- **配套**：行为快照 `m3m4-preflight/inventory-chain-snapshot-20260927-001.json`（合并后 diff 基线）

---

## 0. 总览：四账表 + 两流水 + 一单据的现状拓扑

| 表 | 实体 | Mapper | 关键维度列 | 乐观锁 |
|---|---|---|---|---|
| T1 store_inventory | `entity/StoreInventory.java`（@TableName:16） | `StoreInventoryMapper`（空 BaseMapper，无 XML） | store_id **String**:28 | @Version:103 ✅ |
| T2 inventory | `entity/Inventory.java`（@TableName:17） | `InventoryMapper` + XML | warehouse_id Long:70、location_id:76（仅列存在，无业务使用）、store_id **transient**:173 | @Version:163 ✅ |
| T3 store_inventory_log | `entity/StoreInventoryLog.java`（@TableName:12） | `StoreInventoryLogMapper` + XML | store_id String | 无（纯追加） |
| T4a inventory_transactions | `entity/InventoryTransaction.java`（@TableName:15） | `InventoryTransactionMapper`（无 XML） | warehouse_id:44 | 无（纯追加） |
| T4b inventory_log | `entity/InventoryLog.java`（@TableName:15） | `InventoryLogMapper` + XML | warehouse_id | 无（纯追加） |
| T5 material_consumption | `entity/MaterialConsumption.java`（@TableName:13） | `MaterialConsumptionMapper`（@Select 注解，XML 空壳） | store_id **Long**:77 | 无 |
| T6 inventory_transfers | `entity/InventoryTransfer.java`（@TableName:16） | `InventoryTransferMapper` + XML | from/to_warehouse_id | 无 |

**ID 类型污染实录**：`StoreInventory.storeId`=String、`MaterialConsumption.storeId`=Long、`Inventory.storeId`=transient Long、`InventoryTransfer` 用 warehouseId 冒充 storeId（`InventoryTransferServiceImpl.java:226-245` 注释自认）。

---

## 1. 写入路径全量矩阵

### T1 store_inventory（mapper 级 3 点 + 业务调用 12 点）

| 模块 | file:line | 方法 | 操作 | store_id 用法 | 乐观锁 | M3-M4 改造要点 |
|---|---|---|---|---|---|---|
| 库存核心 | service/impl/StoreInventoryServiceImpl.java:121 | increaseStock 新建分支 | INSERT | 写入值（String） | —（INSERT） | 参数 String→Long(locationId)；唯一键 `(location_id, material_id)` |
| 库存核心 | StoreInventoryServiceImpl.java:165 | increaseStock 已有分支 | UPDATE | eq :82 getByStoreAndMaterial | ✅ + 3 次重试(:133) | 键改 location_id；重试保留 |
| 库存核心 | StoreInventoryServiceImpl.java:229 | decreaseStock | UPDATE | 同上 | ✅ + 重试(:200) | NOT_FOUND 口径见 §6-1 |
| 门店库存手调 | controller/StoreInventoryController.java:126,136 | adjustStoreInventory(:108) | 调 increase/decrease | 取自存量行 | 继承 ✅ | 后续支持 location 维度 |
| 采购到货 | service/impl/PurchaseArrivalServiceImpl.java:391 | increaseInventoryForArrival(:362) | increaseStock | `String.valueOf(warehouseId)`，**兜底仓库"1"**(:366-368) | 继承 ✅ | ⚠️ R-04 污染源，改 location_id 后删兜底 |
| 调拨 | InventoryTransferServiceImpl.java:265 | syncStoreInventory(:251) | decreaseStock | `String.valueOf(fromWarehouseId)` | 继承 ✅ | 同仓校验(:208)语义重审 |
| 调拨 | InventoryTransferServiceImpl.java:274 | syncStoreInventory | increaseStock（unitCost=null→0 成本） | `String.valueOf(toWarehouseId)` | 继承 ✅ | 成本口径见 §6-6 |
| 销售出库 | OrderNewServiceImpl.java:644 | deductInventoryAndCalculateCost(:583) | decreaseStock | `order.getStoreId()` | 继承 ✅ | 订单 store_id 属 B 类——过 location_id_map（规则 4） |
| KDS 出餐扣料 | OrderNewServiceImpl.java:901 | doDeductMaterialsForServe(:707) | decreaseStock | `order.getStoreId()` | 继承 ✅ | 幂等占位在 kitchen_order.material_consumed |
| 退款回补 | OrderNewServiceImpl.java:1131 | restoreInventoryForRefund(:1089) | increaseStock（先读 :1125 拿单位成本） | 同上 | 继承 ✅ | 回补口径与扣减对称 |
| 采购退货 | PurchaseReturnServiceImpl.java:460 | decreaseInventoryForReturn(:440) | decreaseStock | `String.valueOf(getWarehouseId())` | 继承 ✅ | warehouseId 冒充，改 location_id |
| 收货确认 | ReceiptConfirmationServiceImpl.java:696 | increaseInventory(:688) | increaseStock | `arrival.getStoreId()`（String） | 继承 ✅ | receiverType 双路由是最大分支改造点 |
| 采购入库 | PurchaseStockinServiceImpl.java:740 | increaseInventoryForStockin(:711) | increaseStock | `String.valueOf(warehouseId)`(:716) | 继承 ✅ | DF-001：同步失败回滚主事务(:749) 语义必须保留 |
| 采购入库作废 | PurchaseStockinServiceImpl.java:791 | decreaseInventoryForVoidStockin(:771) | decreaseStock | 同上 | 继承 ✅ | |

补漏：无其他 T1 写入方——`MaterialTraceCodeServiceImpl.updateStoreInventory(:119)/updateInventoryAfterConfirm(:400)` 的 Inventory 对象**从未落库**（死代码）；QuickStockIn 不触碰 T1。

### T2 inventory 写入点

| file:line | 方法 | 乐观锁 | 改造要点 |
|---|---|---|---|
| InventoryServiceImpl.java:224 | increaseInventory 新建分支（saveOrUpdate） | — | 合并主表落点；batch_no 不进唯一键 |
| InventoryServiceImpl.java:102 / :134 | lockInventory / unlockInventory | ✅+重试 | 锁定语义保留；负数钳 0(:128-130) |
| InventoryServiceImpl.java:177 | deductInventory | ✅+重试 | 成功才写 T4a 流水(:180) |
| InventoryServiceImpl.java:258 / :303 | increase/decrease 已有分支 | ✅+重试 | **仅按 (material_id, warehouse_id) 定位**——合并键改造核心 |
| ⚠️ InventoryTransferServiceImpl.java:312,321 | updateInventory(:292) | ❌ 直写 updateById 无重试 | 绕过服务；改统一入口 |
| ⚠️ LossOutboundService.java:141,151 | updateInventory(:122) | ❌ | eq **Inventory::getProductId（transient 污染列）** |
| ⚠️ OtherInboundService.java:120,126 | updateInventory(:96) | ❌ | 绕过服务；int 精度截断(:123) |
| PurchaseStockinServiceImpl.java:734 | 经 InventoryService | 继承 ✅ | 与 T1 双写(:740)，合并后删除 |
| PurchaseArrivalServiceImpl.java:385 | 经 InventoryService | 继承 ✅ | 失败仅 warn 不回滚(:383)——与门店侧不对称（§6-8） |
| ReceiptConfirmationServiceImpl.java:716 | 经 InventoryService | 继承 ✅ | receiverType=warehouse 分支 |
| PurchaseReturnServiceImpl.java:455 / PurchaseStockinServiceImpl.java:786 | 经 InventoryService | 继承 ✅ | |
| InventoryAdjustServiceImpl.java:349,360 / InventoryOutboundServiceImpl.java:299 / InventoryLossServiceImpl.java:155 / SelfPurchaseServiceImpl.java:111 / PurchaseStockinScanServiceImpl.java:414 / TraceCodeServiceImpl.java:150,189 | 经 InventoryService | 继承 ✅ | |
| MaterialArchiveServiceImpl.java:375 | ensureInventoryForMaterial（读+0 库存建档） | — | |
| ⚠️ SalesOrderServiceImpl.java:281 | deductInventoryForOrder(:257) **直接 inventoryMapper.updateById** | ⚠️ 无重试；不足时**静默钳 0**(:278)，不写流水 | 双重污染（绕服务+transient 列 :270-271）；重写或下线 |
| ⚠️ HardwareDeviceServiceImpl.java:81 | inventoryService.updateById | ⚠️ 无重试 | 低频，收编 |
| ⚠️ config/InventoryWarningScheduler.java:164 | checkExpiredWarning 写 T2 status=3 | ⚠️ 无重试 | 调度器写账表（§7） |

### T3 store_inventory_log 写入点

| file:line | 说明 |
|---|---|
| StoreInventoryServiceImpl.java:273（writeLog:257） | 唯一"扣减/增加伴随流水"入口；**catch-all 吞异常**(:274)——流水丢失不回滚 |
| MaterialTraceCodeServiceImpl.java:159 / :442 | **只写流水不写库存**（假流水，§6-5） |
| StoreInventoryLogServiceImpl.java:23-26 | 底层 INSERT |

### T4a inventory_transactions 写入点（仅 1 处）

InventoryServiceImpl.java:410（recordTransaction:393）；**T4b inventory_log 无业务写入点**，仅 InventoryLogController.java:42 手工 CRUD 入口。

### T5 material_consumption 写入点

- MaterialConsumptionServiceImpl.java:91 record(:46)——**consumption_id "查最大+1" 非原子，并发撞 UNIQUE 风险**；unitCost 硬编码 0(:74-76)；唯一活跃调用方 KitchenOrderServiceImpl.java:196
- MaterialConsumptionAuditServiceImpl.java:74 recordDeductionFailure（REQUIRES_NEW 审计行）
- deductInventory(:125) / batchDeductInventory / calculateOrderCost / calculateStoreCost / getUndeductedRecords —— **5 个死方法零调用**

### T6 inventory_transfers 写入点

InventoryTransferServiceImpl：create:113 / update:125 / approve:185 / execute:233 / delete:136；单号 generateTransferCode:150-153 用 count+1——**并发重号风险**。

---

## 2. 读取路径全量矩阵

### XML SELECT

| XML | 语句:行 | 改造要点 |
|---|---|---|
| InventoryMapper.xml selectInventoryPage:28 | T2 | warehouse_id 条件(:35)→location_id |
| InventoryMapper.xml selectByMaterialAndWarehouse:51 | T2 | **合并后新唯一键定位点** |
| InventorySummaryMapper.xml selectSummaryPage:6 | T2+T1 **UNION ALL** | 合并后**整段重写**为单表 GROUP BY（:49 COUNT(DISTINCT si.store_id)） |
| StoreInventoryLogMapper.xml selectLogPage:6 | T3 | store_id 条件:10 |
| InventoryLogMapper.xml selectInventoryLogPage:4 / selectConsumptionStats:27 | T4b | 合并后对齐 T4a transaction_type 语义 |
| InventoryTransferMapper.xml selectInventoryTransferPage:71 | T6 | from/to_warehouse_id 条件 |
| DishInventoryMapper.xml selectByDishId:16 | **JOIN inventory**（:25 引用不存在的 `i.cost_price`——污染点） | JOIN 键改 location 维度 |

### 注解 @Select

StockForecastMapper.java:29 getActualConsumed（T3，消费方按"物料单行"假设）、:74 getAvailableStock（T1）、:82 getSafetyStock（T1）；MaterialConsumptionMapper.java:19-34（T5，sumCostByStoreAndTime:34 storeId Long）。

### Lambda 级读取（改键必查）

T1：StoreInventoryServiceImpl:76,84,289、PurchasePlanServiceImpl:611、OrderNewServiceImpl:1125；T2：InventoryServiceImpl:84,120,152,329,337-346,356、DashboardServiceImpl:130、InventoryStatsServiceImpl:45、InventoryAnalysisServiceImpl:47,215、InventoryWarningServiceImpl:88,157、ProcurementTraceServiceImpl:445、BomCheckServiceImpl:130、SalesOrderServiceImpl:270（污染列）、LossOutboundService:127（污染列）、OtherInboundService:103、InventoryTransferServiceImpl:299。

死语句：`InventoryTransactionMapper.selectTransactionPage`（:27，无 XML 绑定零调用，调用即抛 Invalid bound statement——合并时删除）。

## 3. Service 接口签名与调用方数量

### StoreInventoryService（外部调用方 7 文件）
- getStoreInventoryPage(Page, String storeId, Long materialId, String materialName)
- getByStoreAndMaterial(String storeId, Long materialId)
- increaseStock 6 参（0 调用）/ **8 参（6 文件 7 处）**
- decreaseStock 3 参（0）/ **5 参（5 文件 6 处）**
- getLowStockList（PurchasePlanServiceImpl 经 impl 直用 :611）

### InventoryService（外部调用方 16 文件——最多，合并风险最高）
- getInventoryPage / lockInventory / unlockInventory / deductInventory（Controller）
- **increaseInventory（7 文件）**：SelfPurchase:111、InventoryAdjust:349、PurchaseArrival:385、PurchaseStockin:734、PurchaseStockinScan:414、ReceiptConfirmation:716、TraceCode:189
- **decreaseInventory（6 文件）**：InventoryAdjust:360、InventoryOutbound:299、InventoryLoss:155、PurchaseStockin:786、PurchaseReturn:455、TraceCode:150
- getAvailableQuantity（5 处）/ getByMaterialAndWarehouse / getLowStockList（2）/ getExpiringSoonList（0）

### StoreInventoryLogService（3 文件）：createLog（3 处，含 2 处假流水）/ getLogPage
### InventoryLogService（2 控制器）：**updateInventoryLog / deleteInventoryLog 存在——流水可被改删（§6-9）**
### MaterialConsumptionService（活跃 1 文件）：record；5 个死方法
### InventoryTransferService（1 控制器 7 端点）

## 4. XML × 表交叉矩阵（191 个 XML 全量扫描）

仅 7 个 XML 触及 T1-T6：InventoryMapper（T2×2）、InventorySummaryMapper（T2+T1 UNION）、DishInventoryMapper（T2 JOIN）、StoreInventoryLogMapper（T3）、InventoryLogMapper（T4b×2）、InventoryTransferMapper（T6）。**其余 185 个零触及**。T5/T4a 无 XML（注解/BaseMapper）。

## 5. 测试覆盖

| 测试类 | 触及表 |
|---|---|
| InventoryLogConsumptionStatsIntegrationTest / InventoryLogFilterIntegrationTest | T4b |
| MaterialDeductionAuditIntegrationTest（手工 INSERT store_inventory 带 version :125）/ SelfFailureIntegrationTest | T1+T5 |
| OrderNewServiceImplDeductTest（14 用例 failureType 矩阵） | T1(mock)+T5 |
| PurchaseStockinServiceImplTest / PurchaseReturnServiceImplTest / SalesOrderServiceImplTest（mock 层） | T1/T2 |
| OrderNewServiceImplOrderNumberA1Test | 间接 |

**缺口**：InventoryServiceImpl 乐观锁重试、InventoryTransferServiceImpl 双写、StoreInventoryServiceImpl 成本算法均无直接测试；SalesOrder 旁路扣减无测试。

## 6. 行为敏感点（最容易算错账的 8+1 处）

1. **"入建出抛"不对称**：T1/T2 increaseStock/Inventory 对不存在记录新建（:109-128 / :201-231），decrease 抛 NOT_FOUND（:204 / :281）——合并后必须保留，否则退款回补凭空建行
2. **乐观锁重试覆盖残缺**：5 处旁路无重试直写（Transfer/SalesOrder/LossOutbound/OtherInbound/WarningScheduler）；合并单表一次 UPDATE 反而消除双写窗口，但禁止遗漏旁路
3. **流水"尽力而为"语义**：writeLog 吞异常（:274）——合并后改强一致会使 12 个写入点的失败语义从"降级"变"回滚"，需逐个拍板
4. **transient 污染列查询**：SalesOrderServiceImpl:270-271、LossOutboundService:127-128、DishInventoryMapper.xml:25 `i.cost_price`——全清一级目标
5. **T3 假流水与账脱节**：MaterialTraceCodeServiceImpl:119-170/:400-450 只写流水不写库存；按 T3 聚合消耗（getActualConsumed）会算入从未发生的入库——清数据时删除或补账
6. **调拨成本清零**：syncStoreInventory:274-283 调入传 unitCost=null → 单位成本覆盖为 0（:149 最新入库价格法）——成本核算最高危点
7. **消费 ID 竞态**：getNextSequence "查最大+1" 非原子——改 DB sequence
8. **事务边界不对称**：PurchaseStockin 门店同步失败回滚（:749）vs PurchaseArrival 仓库侧失败仅 warn（:383）——同一业务两个入口一致性等级相反，合并后必须统一
9. **手改流水后门**：InventoryConsumptionController:100/117 允许 update/delete inventory_log——合并后应关闭

## 7. 调度器 / 异步监听

| 组件 | 触碰 | 口径 |
|---|---|---|
| InventoryWarningScheduler | T2 读写 | :84 低库存 / :102 高库存 / :128 临期 / :148 过期 + **写 status=3**(:164)；status 语义（1/2/4 由 updateInventoryStatus:375 维护）合并后跟表走 |
| PurchaseStockInEventListener | 间接 T1 | @Async + AFTER_COMMIT(:54-55, DF-002) → MaterialTraceCodeService.generateBatch → **T3 假流水**（§6-5） |
| MonthlyClosingScheduler | 无 | :70 getMonthlySalesCost 已注释——月结库存成本**空转** |
| ExpiryCheckScheduler / config/scheduler/* / task/* / config/job/* | 无 | 全量 grep 确认零触及 |
| ReceiptConfirmation/OrderCompleted/OrderRefund EventListener | 无 | 库存联动全在同步 Service 内 |

## 8. 增量结论（相对 -002 §4.1 A 类 32 文件）

1. A 类清单基本准确，**漏列 5 个绕过服务的直写文件**：SalesOrderServiceImpl、LossOutboundService、OtherInboundService、HardwareDeviceServiceImpl、config/InventoryWarningScheduler（其中两个用污染 transient 列）
2. 可顺带删除的死代码：InventoryTransactionMapper.selectTransactionPage、MaterialConsumptionService 5 个死方法
3. 唯一 SQL 级 T1/T2 聚合 = InventorySummaryMapper.xml 的 UNION ALL——合并后**必须重写**（非改列名）
4. warehouseId≡storeId 数值巧合实证 4 处 `String.valueOf(warehouseId)`：PurchaseStockin:716、PurchaseArrival:366、PurchaseReturn:443、InventoryTransfer:258-261——全部经 location_id_map 改写（规则 4）
