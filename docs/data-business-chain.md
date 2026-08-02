# 数据业务链条梳理

## 概述

本文档梳理食品溯源系统中**采购 → 库存 → 成本 → 销售**的数据流动关系，确保库存和成本数据真正流动起来。

---

## 一、核心问题诊断

### 1.1 两套库存体系并存

| 维度 | 仓库库存（Inventory） | 门店库存（StoreInventory） |
|------|---------------------|--------------------------|
| 表名 | `inventory` | `store_inventory` |
| 维度 | 仓库 + 物料 + 批次 | 门店 + 物料 |
| 成本字段 | `unit_cost` (分), `total_cost` (分) | `unit_cost` (分), `total_cost` (分) |
| 锁定数量 | `locked_quantity` | 无 |
| 变动流水 | `inventory_transaction` | `store_inventory_log` |
| 核心 Service | `InventoryService` | `StoreInventoryService` |

**说明**：门店库存已具备 `unit_cost` / `total_cost` 字段，单位成本采用**最新一次入库价格**。

### 1.2 两套订单系统并存

| 系统 | 核心订单（OrderNew） | 销售订单（SalesOrder） |
|------|---------------------|---------------------|
| 表名 | `order_new` | `sales_order` |
| 库存扣减 | **无直接扣减** | 直接扣减 inventory 表 |
| 成本计算 | 事件携带成本数据 | 不计算成本 |
| 追溯联动 | 4种扣减模式（操作追溯码） | 生成 InventoryCode |

**问题**：核心订单系统完成时不扣减物理库存，导致库存数据与实际销售脱节。

### 1.3 成本计算断裂点

1. **销售出库不重算成本**：OrderNew 完成时不更新库存成本
2. **成本数据来源不统一**：菜品原料明细应从门店库存 `unit_cost` 读取，而非商品档案 `referencePrice`

---

## 二、完整数据业务链条设计

### 2.1 采购入库 → 库存增加 → 成本记录

```
┌─────────────────────────────────────────────────────────────┐
│                    采购入库业务流程                            │
└─────────────────────────────────────────────────────────────┘

采购订单 (PurchaseOrder)
       │
       ▼
采购到货单 (PurchaseStockin)  -- 状态：待收货
       │
       ▼  到货登记：记录实收数量、质检状态
   ┌───────────────────────────────────────────┐
   │  1. 生成到货单，不直接增加库存               │
   │  2. 回写采购订单明细实收数量                  │
   │  3. 更新质检状态                              │
   └───────────────────────────────────────────┘
       │
       ▼  入库确认：由一线收货部门实际收货后操作
   ┌───────────────────────────────────────────┐
   │  1. 状态校验（已到货 + 质检通过）             │
   │  2. 更新入库单状态为已入库                    │
   │  3. 增加库存（仓库库存 + 门店库存）           │
   │     ├── InventoryService.increaseInventory()│
   │     │   ├── 更新 unit_cost / total_cost     │
   │     │   └── 写入 inventory_transaction      │
   │     └── StoreInventoryService.increaseStock()│
   │         └── 使用本次入库单价覆盖 unit_cost  │
   │  4. 创建应付账款                             │
   │  5. 发送 MQ 消息                             │
   │  6. 发布 PurchaseStockInEvent               │
   │     ├── 生成原料追溯码                       │
   │     └── 记录采购成本 (CostRecord)           │
   └───────────────────────────────────────────┘
```

### 2.2 库存变动 → 成本重算

```
┌─────────────────────────────────────────────────────────────┐
│                    库存变动成本规则                            │
└─────────────────────────────────────────────────────────────┘

入库时（最新入库价格法）：
  新单位成本 = 本次入库单价（直接覆盖）
  新库存金额 = 新单位成本 × 新库存数量
  新库存数量 = 原库存数量 + 入库数量

出库时：
  出库成本 = 出库数量 × 当前单位成本
  剩余库存金额 = 原库存金额 - 出库成本
  剩余库存数量 = 原库存数量 - 出库数量
  单位成本保持不变

当前实现：
  total_cost = unit_cost × quantity
  unit_cost 取最近一次入库单价
```

### 2.3 订单销售 → 库存扣减 → 成本结转

```
┌─────────────────────────────────────────────────────────────┐
│                    销售出库业务流程（目标）                     │
└─────────────────────────────────────────────────────────────┘

顾客下单 (OrderNew.createOrder)
       │
       ▼
  BomCheckService.preOrderCheck()  -- 预检查库存（仅检查，不扣减）
       │
       ▼
  OrderMaterialRequirementService  -- 生成原料需求清单（pending）
       │
       ▼  厨房出餐（扫码 / 确认）
  InventoryDeductionService.deductByXXX()
       │
       ├── 模式A：按追溯码扣减 → MaterialTraceCode.availableQty--
       ├── 模式B：按BOM配方扣减 → FoodTraceCode JSON 记录
       └── ⚠️ 不扣减物理库存！
       │
       ▼  订单完成 (completeOrder)
   ┌───────────────────────────────────────────┐
   │  当前：仅更新状态 + 解锁桌台                  │
   │  缺失：                                     │
   │  1️⃣ 扣减物理库存 (inventory / store_inventory)│
   │  2️⃣ 结转销售成本                              │
   │  3️⃣ 更新库存单位成本                           │
   │  4️⃣ 生成库存变动流水                           │
   └───────────────────────────────────────────┘
       │
       ▼
  OrderCompletedEvent
       ├── 记录财务收入
       └── 记录订单成本（由发布方传入，非实时计算）
```

---

## 三、关键实体与字段映射

### 3.1 采购入库相关

| 实体 | 表名 | 关键字段 | 说明 |
|------|------|---------|------|
| PurchaseOrder | `purchase_order` | order_id, total_amount, status | 采购订单主表 |
| PurchaseOrderItem | `purchase_order_item` | item_id, material_id, quantity, unit_price | 采购订单明细 |
| PurchaseStockin | `purchase_stockin` | stockin_id, order_id, warehouse_id, status | 入库单主表 |
| PurchaseStockinItem | `purchase_stockin_item` | item_id, stockin_id, material_id, quantity, unit_price | 入库单明细 |

### 3.2 库存相关

| 实体 | 表名 | 关键字段 | 说明 |
|------|------|---------|------|
| Inventory | `inventory` | inventory_id, warehouse_id, material_id, quantity, unit_cost, total_cost, locked_quantity | 仓库库存（有成本） |
| InventoryTransaction | `inventory_transaction` | txn_id, inventory_id, type, quantity, unit_cost, total_cost | 库存变动流水 |
| StoreInventory | `store_inventory` | id, store_id, material_id, current_stock, safety_stock, max_stock | 门店库存（**缺成本字段**） |
| StoreInventoryLog | `store_inventory_log` | id, store_id, material_id, change_type, quantity, before_stock, after_stock | 门店库存日志 |

### 3.3 销售订单相关

| 实体 | 表名 | 关键字段 | 说明 |
|------|------|---------|------|
| OrderNew | `order_new` | order_id, store_id, total_amount, order_status | 核心订单主表 |
| OrderItemNew | `order_item_new` | item_id, order_id, food_id, quantity, unit_price, subtotal | 订单明细 |
| SalesOrder | `sales_order` | order_id, store_id, total_amount, status | 销售订单（旧版） |
| SalesOrderDetail | `sales_order_detail` | detail_id, order_id, product_id, quantity, unit_price | 销售订单明细 |

### 3.4 成本相关

| 实体 | 表名 | 关键字段 | 说明 |
|------|------|---------|------|
| CostRecord | `cost_record` | record_id, cost_type, amount, biz_id, biz_type | 成本记录（财务维度） |
| ProductPricingHistory | `product_pricing_history` | id, product_id, cost_price, sale_price | 价格历史 |
| FoodNew | `food_new` | food_id, cost_price, sale_price | 菜品表（含成本价） |

---

## 四、需要修复/重构的关键问题

### 4.1 高优先级

1. **门店库存增加成本字段**
   - 为 `store_inventory` 表添加 `unit_cost` (BIGINT, 分) 和 `total_cost` (BIGINT, 分) 字段
   - 同步修改 StoreInventory 实体类
   - 在 increaseStock / decreaseStock 方法中同步更新成本
   - 入库时 `unit_cost` 使用本次入库单价直接覆盖（最新入库价格法）

2. **订单完成时扣减物理库存**
   - 在 OrderNewServiceImpl.completeOrder() 中添加库存扣减逻辑
   - 根据 BOM 配方计算需要扣减的原料数量
   - 调用 InventoryService.decreaseInventory() 扣减仓库库存
   - 调用 StoreInventoryService.decreaseStock() 扣减门店库存
   - 生成库存变动流水记录

3. **销售成本结转**
   - 出库时按当前单位成本计算出库成本
   - 生成 CostRecord（成本类型：食材成本）
   - 更新库存金额

### 4.2 中优先级

4. **明确最新入库价格成本规则**
   - 入库时 `unit_cost` 直接覆盖为本次入库单价
   - 在文档、代码注释、成本分析报表中统一说明此规则

5. **统一两套订单系统**
   - 评估 OrderNew 和 SalesOrder 的合并方案
   - 统一库存扣减入口

6. **库存成本与追溯码联动**
   - 追溯码扣减时同步扣减物理库存
   - 确保追溯数据与库存数据一致

### 4.3 低优先级

7. **成本分析报表完善**
   - 门店级别的毛利分析
   - 库存周转率分析
   - 成本趋势分析

---

## 五、相关代码位置索引

### Service 层

| Service | 路径 | 核心方法 |
|---------|------|---------|
| PurchaseStockinService | `service/PurchaseStockinService.java` | confirmStockin() |
| InventoryService | `service/InventoryService.java` | increaseInventory(), decreaseInventory() |
| StoreInventoryService | `service/StoreInventoryService.java` | increaseStock(), decreaseStock() |
| OrderNewService | `service/OrderNewService.java` | createOrder(), completeOrder() |
| InventoryDeductionService | `service/InventoryDeductionService.java` | deductByTraceCode(), deductByRecipe() |
| BomCheckService | `service/BomCheckService.java` | checkDish(), preOrderCheck() |
| CostRecordService | `service/finance/CostRecordService.java` | createCostRecord() |
| DishCostService | `service/DishCostService.java` | calculateDishCost() |

### 事件监听

| 监听器 | 路径 | 监听事件 |
|--------|------|---------|
| PurchaseStockInEventListener | `event/listener/PurchaseStockInEventListener.java` | PurchaseStockInEvent |
| OrderCompletedEventListener | `event/listener/OrderCompletedEventListener.java` | OrderCompletedEvent |

### 数据库迁移

| 迁移脚本 | 说明 |
|---------|------|
| V20260629_009__create_purchase_stockin_tables.sql | 采购入库表 |
| V20260629_015__create_warehouses_table.sql | 仓库表 |
| V20260704_004__create_store_inventory_table.sql | 门店库存表 |
| V20260629_020__create_inventory_logs_table.sql | 库存日志表 |

---

## 六、前端页面对应关系

| 业务环节 | 前端页面 | 路径 |
|---------|---------|------|
| 采购入库 | 采购收货 | `views/purchase/PurchaseStockin.vue` |
| 仓库库存 | 仓储总览 | `views/warehouse/WarehouseOverview.vue` |
| 门店库存 | 门店库存 | `views/warehouse/StoreInventory.vue` |
| 销售订单 | 订单查询 | `views/order/OrderQuery.vue` |
| 成本分析 | 决策看板 | `views/decision/DecisionBoard.vue` |

---

## 七、实施进展记录（2026-07-07）

### 7.1 已完成

| 序号 | 任务 | 状态 | 说明 |
|------|------|------|------|
| 1 | 为门店库存增加成本字段 | ✅ 完成 | 为 `store_inventory` 和 `store_inventory_log` 表添加 `unit_cost`、`total_cost` 字段 |
| 2 | 门店库存实体类更新 | ✅ 完成 | `StoreInventory` 实体类添加成本字段及 getter/setter |
| 3 | StoreInventoryService 接口更新 | ✅ 完成 | `increaseStock` 增加 unitCost 参数，`decreaseStock` 返回出库成本 |
| 4 | 加权平均成本计算法 | ✅ 完成 | `increaseStock` 实现加权平均，`decreaseStock` 按比例减少总成本 |
| 5 | 采购入库同步成本 | ✅ 完成 | `PurchaseStockinServiceImpl` 同步传入单位成本到门店库存 |
| 6 | 库存调整接口支持成本 | ✅ 完成 | `StoreInventoryController.adjust` 支持传入 unitCost 参数 |
| 7 | 订单完成按BOM扣减库存（单品） | ✅ 完成 | `OrderNewServiceImpl.completeOrder` 实现单品类型的库存扣减和成本结转 |
| 8 | 套餐类型订单的库存扣减 | ✅ 完成 | 展开套餐内所有菜品，按各菜品BOM配方汇总扣减原料 |
| 9 | 订单退款时的库存回补 | ✅ 完成 | 全额退款审批通过时，按当前单位成本回补门店库存 |

### 7.2 实施详情

**门店库存成本计算规则（最新入库价格法）：**

```
入库时：
  newTotalQty = oldQty + inQty
  newUnitCost = inUnitCost（直接覆盖为本次入库单价）
  newTotalCost = newUnitCost × newTotalQty

出库时：
  outCost = outQty × currentUnitCost
  newTotalQty = oldQty - outQty
  newTotalCost = oldTotalCost - outCost
```

**订单完成库存扣减流程：**

```
completeOrder(orderId)
  │
  ├─ 校验订单状态
  ├─ 判断是否有关联门店（storeId）
  │   └─ 有门店 → deductInventoryAndCalculateCost(order)
  │       ├─ 获取订单明细
  │       ├─ 遍历每个明细（跳过已退款）
  │       │   └─ 单品类型 → 查找菜品配方(BOM)
  │       │       └─ 按原料汇总扣减数量
  │       │           实际用量 = 标准用量 × 数量 × (1 + 损耗率)
  │       └─ 执行库存扣减
  │           └─ 调用 StoreInventoryService.decreaseStock()
  │               返回出库成本，累加得到订单总成本
  ├─ 更新订单状态为"已完成"
  └─ 解锁桌台
```

### 7.3 待完成

- [x] 套餐类型（productType=2）的库存扣减支持 ✅ 2026-07-07
- [x] 订单退款时的库存回补 ✅ 2026-07-07（全额退款）
- [ ] 部分退款的库存回补（需增加退款明细表）
- [ ] 菜品成本自动从库存成本更新
- [ ] 成本记录（CostRecord）与订单关联
- [ ] 端到端测试验证

---

## 八、不同经营模式下的数据流向差异

### 8.1 三种经营模式

| 模式 | 编码 | 规模 | 核心特征 |
|------|------|------|---------|
| 集中式单店 | `centralized-single` | 单店 | 一个门店，一个仓库，流程简单 |
| 标准连锁 | `standard-chain` | 中型连锁 | 多门店，中央仓+配送，区域化管理 |
| 大型连锁 | `large-chain` | 大型连锁 | 多区域，多级仓配，复杂供应链 |

### 8.2 数据流向对比

```
【集中式单店】
采购入库 → 仓库库存 → 门店库存（直接调拨）→ 销售扣减
         ↓          ↓
      成本核算    成本核算（最新入库价格）

特点：
- 仓库库存和门店库存都存在，但调拨简单
- 采购入库确认时可同时增加仓库和门店库存（收货登记不增加库存）
- 订单完成直接扣减门店库存

【标准连锁】
采购入库 → 中央仓库存 → 门店库存（配送调拨）→ 销售扣减
         ↓                ↓
      成本核算          成本核算
         ↑
      库存调拨单（移库）

特点：
- 中央仓统一采购，然后配送到各门店
- 库存调拨是核心流程之一
- 成本在不同层级独立核算

【大型连锁】
采购入库 → 区域仓 → 城市仓 → 门店库存 → 销售扣减
         ↓        ↓        ↓
      成本核算  成本核算  成本核算
         ↑        ↑
      供应链调度  配送管理

特点：
- 多级仓储体系（区域仓-城市仓-门店）
- 复杂的供应链调度和配送
- 成本核算更加复杂（调拨成本、运输成本）
```

### 8.3 当前实现适配情况

| 功能 | 集中式单店 | 标准连锁 | 大型连锁 |
|------|-----------|---------|---------|
| 门店库存成本核算 | ✅ 支持 | ✅ 支持 | ✅ 支持 |
| 订单完成扣减门店库存 | ✅ 支持 | ✅ 支持 | ✅ 支持 |
| 采购入库增加库存 | ✅ 支持 | ⚠️ 需适配 | ⚠️ 需适配 |
| 库存调拨 | ⚠️ 基础功能 | ⚠️ 需完善 | ❌ 不支持 |
| 多级仓储 | ❌ 不需要 | ⚠️ 部分支持 | ❌ 不支持 |
| 套餐库存扣减 | ✅ 支持 | ✅ 支持 | ✅ 支持 |
| 退款库存回补 | ✅ 支持 | ✅ 支持 | ✅ 支持 |

### 8.4 后续优化建议

1. **标准连锁模式优化**：
   - 完善库存调拨流程（调拨单、在途库存）
   - 采购入库默认入中央仓，通过配送到门店
   - 增加配送管理模块

2. **大型连锁模式扩展**：
   - 多级仓储架构（区域仓-城市仓-门店）
   - 供应链调度和优化
   - 跨区域库存调拨
   - 集中采购+分级配送

---

## 九、下一步行动计划

1. **第一步**：为门店库存增加成本字段（数据库 + 实体 + Service） ✅
2. **第二步**：在采购入库时同步写入门店库存成本 ✅
3. **第三步**：订单完成时按 BOM 配方扣减物理库存 ✅（单品+套餐）
4. **第四步**：实现加权平均成本计算法 ✅
5. **第五步**：退款库存回补 ✅（全额退款）
6. **第六步**：验证端到端数据流动（采购→库存→销售→成本）
