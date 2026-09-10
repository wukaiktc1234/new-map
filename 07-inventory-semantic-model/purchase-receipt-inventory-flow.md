# Phase 12: Purchase / Receiving 库存流语义模型

## 核心问题

**Inventory 在哪个业务节点正式增加？**
- Purchase Order
- Receipt
- Receiving Confirmation
- Warehouse Putaway

## 业务事件定义

### 1. 采购流程阶段

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Purchase   │    │   Receipt    │    │  Receiving   │    │  Warehouse   │
│    Order     │───>│  (到货)      │───>│ Confirmation │───>│   Putaway    │
│              │    │              │    │  (确认收货)   │    │  (入库上架)   │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
     │                   │                   │                   │
     │  1. 创建采购单     │  2. 供应商送货    │  3. 验收确认       │  4. 上架入位
     │  (承诺,未入库)     │  (在途,未入库)    │  (待入库)         │  (正式入库)
     │                   │                   │                   │
     │  库存: +0         │  库存: +0         │  库存: +0         │  库存: +N
     │  (仅记录意向)      │  (仅记录在途)     │  (仅记录待处理)    │  (正式增加)
```

### 2. 语义定义

| 阶段 | 业务事件 | 库存影响 | 财务影响 |
|------|----------|----------|----------|
| **Purchase Order** | 创建采购订单 | 无变化 (仅承诺) | 无 |
| **Receipt** | 供应商送货到达 | 无变化 (在途库存) | 无 |
| **Receiving Confirmation** | 验收确认 | 无变化 (待入库) | 无 |
| **Warehouse Putaway** | 入库上架 | **正式增加** | 确认应付账款 |

## 核心结论

**Inventory 在 Warehouse Putaway (入库上架) 节点正式增加。**

### 理由

1. **法律/财务角度**: 货物验收合格后才算正式入库，产生应付账款
2. **实物角度**: 货物必须实际入库上架后才算可用库存
3. **风险角度**: 在途/待入库状态的货物尚未完全可控

## 当前系统实现

### 数据表结构

```sql
-- 采购订单明细 (承诺, 未入库)
purchase_order_items:
  - order_id: 采购订单ID
  - material_id: 物料ID
  - quantity: 采购数量 (承诺)
  - unit_price: 单价
  - received_quantity: 已收货数量 (跟踪执行进度)

-- 采购入库单明细 (正式入库)
purchase_stockin_items:
  - stockin_id: 入库单ID
  - order_item_id: 关联采购订单明细ID
  - material_id: 物料ID
  - actual_quantity: 实际入库数量
  - unit_cost: 单位成本
  - batch_no: 批次号
  - production_date: 生产日期
  - expiry_date: 过期日期
  - location_id: 库位ID

-- 库存流水 (正式入库记录)
inventory_transactions:
  - transaction_type: 1 (采购入库)
  - material_id: 物料ID
  - warehouse_id: 仓库ID
  - quantity_change: 变动数量
  - before_qty: 变动前数量
  - after_qty: 变动后数量
  - unit_cost: 单位成本
  - reference_no: 关联单据号
```

### 代码逻辑

```java
// PurchaseArrivalServiceImpl.java:376-385
// 到货确认时增加库存 (实际上是在 Receiving Confirmation 阶段)
InventoryIncreaseDTO increaseDTO = new InventoryIncreaseDTO();
increaseDTO.setMaterialId(item.getMaterialId());
increaseDTO.setWarehouseId(arrival.getWarehouseId());
increaseDTO.setQuantity(actualQty);
increaseDTO.setUnitCost(item.getUnitPrice());
increaseDTO.setTransactionType(1); // 采购入库
increaseDTO.setReferenceNo(arrival.getArrivalCode());
increaseDTO.setReferenceType("purchase_arrival");
inventoryService.increaseInventory(increaseDTO);
```

```java
// PurchaseStockinServiceImpl.java:729
// 入库确认时增加库存
increaseDTO.setTransactionType(1); // 采购入库
```

```java
// ReceiptConfirmationServiceImpl.java:711
// 收货确认时增加库存
increaseDTO.setTransactionType(1);
```

### 发现的问题

1. **多处触发库存增加**: 
   - `PurchaseArrivalServiceImpl` (到货确认)
   - `PurchaseStockinServiceImpl` (入库确认)
   - `ReceiptConfirmationServiceImpl` (收货确认)
   - 可能导致重复增加

2. **阶段定义模糊**:
   - 当前系统在 "Receiving Confirmation" 阶段就增加了库存
   - 但根据语义模型，应该在 "Warehouse Putaway" 阶段才正式入库

3. **门店库存同步**:
   ```java
   // PurchaseArrivalServiceImpl.java:389-404
   if (storeIdForSync != null) {
       storeInventoryService.increaseStock(
               storeIdForSync,
               item.getMaterialId(),
               item.getMaterialName(),
               actualQty,
               item.getUnit(),
               item.getUnitPrice() != null ? item.getUnitPrice() : null,
               1,
               "采购入库 - 到货单:" + arrival.getArrivalCode());
   }
   ```
   - 到货确认时同步了门店库存
   - 但门店库存应该在 Putaway 完成后才正式可用

## 语义模型规范

### 正确的触发时机

```
阶段 1: Purchase Order
  - 事件: order_created
  - 库存影响: 无
  - 记录: purchase_order_items (承诺数量)
  - 状态: planned

阶段 2: Receipt (供应商送货)
  - 事件: goods_arrived
  - 库存影响: 无
  - 记录: purchase_arrivals (在途数量)
  - 状态: in_transit

阶段 3: Receiving Confirmation (验收确认)
  - 事件: quality_checked
  - 库存影响: 无
  - 记录: purchase_stockin_items (待入库数量)
  - 状态: pending_putaway

阶段 4: Warehouse Putaway (入库上架)
  - 事件: putaway_completed
  - 库存影响: +N (正式增加)
  - 记录: inventory_transactions (transaction_type=1)
  - 状态: available
  - 财务: 确认应付账款
```

### 部分入库场景

```
采购订单: 100箱啤酒
到货: 80箱 (供应商缺货)
验收: 75箱 (5箱损坏)
入库: 75箱

流程:
  PO: 承诺 100箱
  Receipt: 到货 80箱 (在途)
  Confirmation: 75箱合格 (待入库)
  Putaway: 75箱入库 (正式增加)

库存变化:
  purchase_order_items.received_quantity = 75
  inventory_transactions.quantity_change = +75
  inventory.current_stock += 75
```

### 单位转换场景

```
采购: 10桶食用油 × 5L/桶
入库时发现: 实际是 5000ml/桶

需要:
  1. 验证单位转换关系: 1桶 = 5L = 5000ml
  2. 按实际单位入库
  3. 记录转换关系用于成本计算
```

## 建议

1. **统一入库触发点**: 建议在 Warehouse Putaway 阶段统一触发库存增加
2. **增加状态跟踪**: 在 purchase_order_items 增加 `fulfillment_status` 字段
3. **分离在途库存**: 考虑增加 `inventory_in_transit` 表跟踪在途货物
4. **修正重复入库**: 检查并修正当前系统中可能的重复入库问题
5. **增加质量检验**: 在 Confirmation 阶段记录质量检验结果
