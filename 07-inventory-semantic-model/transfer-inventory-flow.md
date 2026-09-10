# Phase 13: Transfer 库存流语义模型

## 核心问题

1. Transit 是否需要成为 Inventory Location？
2. 调拨过程中库存属于谁？
3. Transfer Out 与 Transfer In 是否一个事务？
4. 跨门店 / 跨仓库是否一样？

## 业务流程定义

### 1. 调拨流程阶段

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Warehouse A │    │   Transit    │    │  Warehouse B │    │   Store B    │
│  (调出仓)    │───>│  (在途)      │───>│  (调入仓)    │───>│  (门店)      │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
     │                   │                   │                   │
     │  1. Transfer Out  │  2. In Transit   │  3. Transfer In   │  4. Store Sync
     │  (减少库存)       │  (持有库存)      │  (增加库存)       │  (同步库存)
     │                   │                   │                   │
     │  库存: -N         │  库存: +N (临时)  │  库存: +N         │  库存: +N
```

### 2. 语义定义

| 阶段 | 业务事件 | 库存影响 | 归属方 |
|------|----------|----------|--------|
| **Transfer Out** | 调出确认 | 源仓库 -N | 源仓库 |
| **Transit** | 在途运输 | Transit Location +N | 调出方(运输中) |
| **Transfer In** | 调入确认 | 目标仓库 +N | 目标仓库 |
| **Store Sync** | 门店同步 | 门店库存 +N | 门店 |

## 核心结论

### Q1: Transit 是否需要成为 Inventory Location？

**是的，Transit 应该成为临时的 Inventory Location。**

理由：
1. **库存归属明确**: 在途货物需要明确归属方（通常是调出方）
2. **库存盘点准确**: 避免在途货物被遗漏或重复计算
3. **异常处理**: 运输途中的损坏、丢失需要有对应记录

```
Transit Location 结构:
  - location_type: "transit"
  - transfer_id: 关联调拨单ID
  - material_id: 物料ID
  - quantity: 在途数量
  - owner_id: 归属方ID (调出方)
  - departure_time: 发货时间
  - expected_arrival: 预计到达时间
```

### Q2: 调拨过程中库存属于谁？

**归属权随阶段转移：**

| 阶段 | 归属方 | 风险承担方 |
|------|--------|------------|
| Transfer Out (发货前) | 源仓库 | 源仓库 |
| Transit (在途中) | **调出方** (运输中) | 调出方/物流公司 |
| Transfer In (到货后) | 目标仓库 | 目标仓库 |
| Store Sync (门店入库) | 门店 | 门店 |

**关键点：**
- 在途期间，库存仍属于调出方
- 运输损失由调出方承担（或按合同约定）
- 目标仓库确认收货后，归属权转移

### Q3: Transfer Out 与 Transfer In 是否一个事务？

**不是，应该是两个独立事务。**

原因：
1. **时间跨度**: 调拨可能跨天/跨周
2. **地理分离**: 源仓库和目标仓库可能在不同地点
3. **异常处理**: 需要支持部分到货、损坏等场景

```
事务 1: Transfer Out (源仓库)
  BEGIN
    UPDATE inventory SET quantity = quantity - N
    WHERE material_id = ? AND warehouse_id = source_warehouse_id;
    
    INSERT INTO inventory_transactions (transaction_type=3, ...);
    
    INSERT INTO inventory_in_transit (...);
  COMMIT

事务 2: Transfer In (目标仓库)
  BEGIN
    UPDATE inventory SET quantity = quantity + N
    WHERE material_id = ? AND warehouse_id = target_warehouse_id;
    
    INSERT INTO inventory_transactions (transaction_type=4, ...);
    
    DELETE FROM inventory_in_transit WHERE transfer_id = ?;
  COMMIT
```

### Q4: 跨门店 / 跨仓库是否一样？

**语义相同，但实现有差异：**

| 场景 | 源 | 目标 | 复杂度 |
|------|-----|------|--------|
| 仓库→仓库 | warehouse | warehouse | 低 |
| 仓库→门店 | warehouse | store | 中 (需同步 store_inventory) |
| 门店→门店 | store | store | 高 (双向同步) |

**当前系统实现：**

```java
// InventoryTransferServiceImpl.java:219-227
// 1. 更新中央仓 inventory 表
updateInventory(productId, fromWarehouseId, quantity.negate()); // 调出
updateInventory(productId, toWarehouseId, quantity);            // 调入

// 2. 同步门店库存 store_inventory 表
syncStoreInventory(inventoryTransfer, quantity);
```

**发现的问题：**

1. **没有 Transit 状态**: 当前实现在同一事务中完成调出和调入，没有在途状态
2. **门店同步不完整**: 只同步了目标门店，没有处理源门店的库存扣减
3. **缺少在途库存表**: 没有 `inventory_in_transit` 表跟踪在途货物

## 当前系统实现

### 数据表结构

```sql
-- 调拨单 (记录调拨意向)
inventory_transfers:
  - transfer_id: 调拨单ID
  - transfer_code: 调拨单号 (TR + 日期 + 序号)
  - from_warehouse_id: 源仓库ID
  - to_warehouse_id: 目标仓库ID
  - product_id: 产品ID
  - transfer_quantity: 调拨数量
  - transfer_status: 状态 (0草稿/1待审批/2已审批/3已完成/4已取消)
  - transfer_date: 调拨日期
  - receive_date: 接收日期

-- 库存流水 (记录库存变动)
inventory_transactions:
  - transaction_type: 3 (调拨出)
  - transaction_type: 4 (调拨入)
```

### 代码逻辑

```java
// InventoryTransferServiceImpl.java:192-237
@Transactional(rollbackFor = Exception.class)
public InventoryTransfer executeInventoryTransfer(Long id) {
    // 1. 验证调拨单状态
    if (!InventoryTransferStatus.APPROVED.getCode().equals(inventoryTransfer.getTransferStatus())) {
        throw new RuntimeException("调拨单未审批，无法执行");
    }
    
    // 2. 校验同仓调拨
    if (fromWarehouseId != null && fromWarehouseId.equals(toWarehouseId)) {
        throw new RuntimeException("源仓库与目标仓库不能相同");
    }
    
    // 3. 执行库存调拨 (在同一事务中)
    updateInventory(productId, fromWarehouseId, quantity.negate()); // 调出
    updateInventory(productId, toWarehouseId, quantity);            // 调入
    
    // 4. 同步门店库存
    syncStoreInventory(inventoryTransfer, quantity);
    
    // 5. 更新调拨单状态
    inventoryTransfer.setStatus(InventoryTransferStatus.COMPLETED.getCode());
}
```

### 状态枚举

```java
// InventoryTransferStatus.java
public enum InventoryTransferStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消/已拒绝");
}
```

## 语义模型规范

### 正确的流程设计

```
阶段 1: 调拨申请 (Transfer Request)
  - 事件: transfer_requested
  - 库存影响: 无
  - 记录: inventory_transfers (状态: PENDING_APPROVAL)
  - 审批: 需要审批流程

阶段 2: 调拨审批 (Transfer Approved)
  - 事件: transfer_approved
  - 库存影响: 无
  - 记录: inventory_transfers (状态: APPROVED)
  - 准备: 源仓库准备发货

阶段 3: 调出确认 (Transfer Out Confirmed)
  - 事件: transfer_out_completed
  - 库存影响: 源仓库 -N
  - 记录: 
    - inventory_transactions (transaction_type=3, -N)
    - inventory_in_transit (+N, 归属调出方)
  - 状态: inventory_transfers (状态: IN_TRANSIT)

阶段 4: 在途运输 (In Transit)
  - 事件: goods_in_transit
  - 库存影响: 无变化
  - 记录: inventory_in_transit (跟踪位置、预计到达时间)
  - 风险: 调出方承担

阶段 5: 调入确认 (Transfer In Confirmed)
  - 事件: transfer_in_completed
  - 库存影响: 目标仓库 +N
  - 记录: 
    - inventory_transactions (transaction_type=4, +N)
    - inventory_in_transit (-N, 移除在途记录)
  - 状态: inventory_transfers (状态: COMPLETED)

阶段 6: 门店同步 (Store Sync) [可选]
  - 事件: store_inventory_synced
  - 库存影响: 门店库存 +N
  - 记录: store_inventory (+N)
```

### 部分调拨场景

```
调拨单: 100箱啤酒 (仓库A → 仓库B)
实际发货: 80箱 (仓库A库存不足)
在途: 80箱
到货: 75箱 (5箱运输损坏)
入库: 75箱

流程:
  Request: 100箱
  Out: 80箱 (仓库A -80, Transit +80)
  Transit: 80箱
  In: 75箱 (仓库B +75, Transit -80, 损耗5箱)
  
库存变化:
  仓库A: -80箱
  Transit: +80 → -80 (清零)
  仓库B: +75箱
  损耗: 5箱 (需要报损处理)
```

### 跨门店调拨场景

```
门店A → 门店B (同一仓库管理)

需要:
  1. 门店A库存扣减 (store_inventory)
  2. 在途记录 (inventory_in_transit)
  3. 门店B库存增加 (store_inventory)
  4. 同步中央仓库存 (inventory)

当前系统问题:
  - syncStoreInventory 只处理了目标门店
  - 没有处理源门店的库存扣减
  - 缺少门店间的在途跟踪
```

## 建议

### 1. 增加在途库存表

```sql
CREATE TABLE inventory_in_transit (
    transit_id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    from_location_id BIGINT NOT NULL,
    to_location_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,  -- 归属方
    status VARCHAR(20),  -- in_transit/delivered/damaged
    departure_time TIMESTAMP,
    expected_arrival TIMESTAMP,
    actual_arrival TIMESTAMP,
    create_time TIMESTAMP DEFAULT NOW(),
    deleted SMALLINT DEFAULT 0
);
```

### 2. 扩展调拨单状态

```java
public enum InventoryTransferStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    TRANSFERRED_OUT(3, "已调出"),
    IN_TRANSIT(4, "在途"),
    TRANSFERRED_IN(5, "已调入"),
    COMPLETED(6, "已完成"),
    CANCELLED(7, "已取消"),
    PARTIAL_RECEIVED(8, "部分收货");
}
```

### 3. 修正门店同步逻辑

```java
private void syncStoreInventory(InventoryTransfer transfer, BigDecimal quantity) {
    // 源门店库存扣减 (当前缺失)
    if (fromStoreId != null) {
        storeInventoryService.decreaseStock(fromStoreId, productId, quantity, 3, "库存调拨出库");
    }
    
    // 目标门店库存增加
    if (toStoreId != null) {
        storeInventoryService.increaseStock(toStoreId, productId, quantity, 3, "库存调拨入库");
    }
}
```

### 4. 增加损耗处理

调拨过程中的损耗需要单独处理：
- 运输损坏 → 报损 (transaction_type=7)
- 需要记录损耗原因、责任方
- 影响财务核算
