# Phase 11: Sale vs Consumption 库存流语义模型

## 核心问题

**POS Sell 1 bottle 与 Kitchen Consume 100ml 是否修改同一个 Inventory Truth？**

## 语义定义

### 1. 业务事件类型

| 事件 | 触发源 | 语义 | 库存影响 |
|------|--------|------|----------|
| **Sale** | POS 系统 | 成品售出（按最小销售单位） | 减少成品库存（1 bottle） |
| **Consumption** | Kitchen 系统 | 原料被使用（按配方用量） | 减少原料库存（100ml） |

### 2. 库存分层模型

```
┌─────────────────────────────────────────────────────┐
│                  Inventory Truth                     │
├─────────────────────────────────────────────────────┤
│  Level 1: Raw Material (原料层)                      │
│  - material_id: M001 (食用油 5L桶)                   │
│  - unit: ml                                          │
│  - quantity: 10000ml                                 │
│  - 减少方式: Kitchen Consumption (按recipe扣减)       │
├─────────────────────────────────────────────────────┤
│  Level 2: Finished Product (成品层)                  │
│  - product_id: P001 (瓶装饮料)                       │
│  - unit: bottle                                      │
│  - quantity: 50 bottles                              │
│  - 减少方式: POS Sale (按销售单位扣减)                │
└─────────────────────────────────────────────────────┘
```

### 3. 核心结论

**两者修改的不是同一个 Inventory 层级，而是不同的 Inventory Truth：**

- **POS Sell**: 减少 **成品库存** (Level 2: `product_id` + `warehouse_id`)
- **Kitchen Consume**: 减少 **原料库存** (Level 1: `material_id` + `warehouse_id`)

**但它们通过 BOM/Recipe 关联：**

```
POS Sale (1 bottle)
    │
    ├─→ 成品库存 -1 bottle
    │
    └─→ [触发] Kitchen Consume 100ml
              │
              └─→ 原料库存 -100ml
```

## 业务流程图

```
┌──────────┐    ┌──────────────┐    ┌────────────────┐
│   POS    │    │   Kitchen    │    │   Inventory    │
│  System  │    │   System     │    │    System      │
└────┬─────┘    └──────┬───────┘    └───────┬────────┘
     │                 │                     │
     │  1. Sell Event  │                     │
     │────────────────>│                     │
     │                 │                     │
     │  2. Order Created│                    │
     │───────────────────────────────────── >│
     │                 │                     │
     │                 │  3. Recipe Lookup   │
     │                 │────────────────────>│
     │                 │                     │
     │                 │  4. Consume Event   │
     │                 │────────────────────>│
     │                 │                     │
     │  5. Stock Updated│   6. Stock Updated │
     │<─────────────────────────────────────│
     │                 │<────────────────────│
```

## 当前系统实现

### 代码位置

| 功能 | 文件 | 行号 |
|------|------|------|
| Recipe 扣减入口 | `InventoryDeductionController.java:33` | `@PostMapping("/recipe")` |
| 追溯码扣减 | `TraceCodeServiceImpl.java:147` | `decreaseDTO.setTransactionType(1)` (销售出库) |
| 库存扣减服务 | `InventoryServiceImpl.java:180` | `recordTransaction(inventory, ...)` |

### Transaction Type 映射

| transaction_type | 语义 | 触发场景 |
|------------------|------|----------|
| 1 | 采购入库 | Purchase Arrival |
| 2 | 销售出库 | POS Sale (成品) |
| 3 | 调拨出 | Transfer Out |
| 4 | 调拨入 | Transfer In |
| 5 | 盘点盈 | Inventory Check Gain |
| 6 | 盘点亏 | Inventory Check Loss |
| 7 | 报损 | Loss/Write-off |
| 8 | 退货 | Return |

### 实际代码逻辑

```java
// TraceCodeServiceImpl.java:142-150
// 追溯码出库时扣减库存
InventoryDecreaseDTO decreaseDTO = new InventoryDecreaseDTO();
decreaseDTO.setMaterialId(traceCode.getProductId());  // 注意：这里用的是 productId
decreaseDTO.setWarehouseId(traceCode.getWarehouseId());
decreaseDTO.setQuantity(BigDecimal.ONE);
decreaseDTO.setTransactionType(1); // 销售出库 (注释写的是销售出库，但 type=1 实际是采购入库)
decreaseDTO.setReferenceType("trace_code");
inventoryService.decreaseInventory(decreaseDTO);
```

**发现的问题：**

1. `transactionType=1` 注释写的是"销售出库"，但根据DDL定义 `1:采购入库`，存在不一致
2. `materialId` 字段实际上存储的是 `productId`，字段命名与实际用途不匹配

## 语义模型规范

### 正确定义

```
POS Sale:
  - source: POS
  - event: sale
  - inventory_level: finished_product
  - field: product_id + warehouse_id
  - quantity_unit: 最小销售单位 (bottle/box/portion)
  - transaction_type: 2 (销售出库)

Kitchen Consumption:
  - source: Kitchen
  - event: consumption
  - inventory_level: raw_material
  - field: material_id + warehouse_id
  - quantity_unit: 原料单位 (ml/g/kg)
  - transaction_type: 2 (销售出库) 或 新增 type: consumption
```

### 部分消耗场景

```
场景: 1瓶酱油(500ml) 被3道菜分别使用

菜A: 50ml → Kitchen Consume 50ml → 原料库存 -50ml
菜B: 30ml → Kitchen Consume 30ml → 原料库存 -30ml
菜C: 20ml → Kitchen Consume 20ml → 原料库存 -20ml

总消耗: 100ml (原料层)
但POS可能卖出了3份菜 (成品层)
```

### 单位转换

```
1 bottle = 500ml
POS Sell 1 bottle → 成品库存 -1 bottle
Kitchen Consume 100ml → 原料库存 -100ml

转换关系:
  sale_quantity × conversion_factor = consumption_quantity
  1 bottle × 500ml/bottle = 500ml (理论最大消耗)
```

## 建议

1. **分离成品与原料库存表**，避免字段语义混淆
2. **统一 transaction_type 定义**，修正代码注释与DDL不一致
3. **建立 Recipe → Inventory 的标准扣减流程**，确保 Kitchen 消耗正确反映到原料库存
4. **考虑增加 consumption 类型**，区分销售出库与厨房消耗
