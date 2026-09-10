# Inventory Costing Interaction (Phase 17)

## 核心问题：Inventory Quantity + UOM 转换如何影响成本计算？

> 库存数量和单位转换如何影响成本计算？

---

## 1. 成本类型定义与语义

| 成本类型 | 定义 | 触发条件 | 数据来源 | 语义 |
|----------|------|----------|----------|------|
| **Purchase Cost** | 采购入库成本 | 采购入库 | purchase_stockin_items.unit_cost | 入库时的采购单价 |
| **Stock Cost** | 库存持有成本 | 库存快照 | inventory.unit_cost, store_inventory.unit_cost | 当前库存的平均成本 |
| **Consumption Cost** | 生产消耗成本 | Recipe消耗 | dish_recipe.estimated_cost, actual_cost | 按配方计算的物料消耗成本 |
| **Sale Cost** | 销售成本 | POS销售 | order_items相关 | 销售商品的成本 |
| **Waste Cost** | 损耗成本 | 报废/过期 | inventory_transactions(type=5/6) | 损耗物料的成本 |
| **Transfer Cost** | 调拨成本 | 仓库间调拨 | inventory_transactions(type=3/4) | 调拨物料的成本 |
| **Adjustment Cost** | 调整成本 | 库存盘点 | inventory_transactions(type=7) | 盘点调整的成本 |

---

## 2. 当前系统现实分析

### 2.1 成本相关数据字段

```
┌─────────────────────────────────────────────────────────────┐
│                    当前成本数据模型                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │    inventory     │      │ store_inventory  │            │
│  │  (仓库库存)      │      │  (门店库存)      │            │
│  ├──────────────────┤      ├──────────────────┤            │
│  │ unit_cost        │      │ unit_cost        │            │
│  │ total_cost       │      │ total_cost       │            │
│  └──────────────────┘      └──────────────────┘            │
│           │                         │                       │
│           ▼                         ▼                       │
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │ purchase_stockin │      │  dish_recipe     │            │
│  │    _items        │      │  (配方成本)      │            │
│  ├──────────────────┤      ├──────────────────┤            │
│  │ unit_cost        │      │ estimated_cost   │            │
│  │ total_cost       │      │ actual_cost      │            │
│  └──────────────────┘      └──────────────────┘            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 成本计算的关键问题

1. **成本粒度问题**：
   - inventory.unit_cost 是仓库级成本
   - store_inventory.unit_cost 是门店级成本
   - 两者可能不一致

2. **UOM转换成本问题**：
   - 采购单位：箱（100瓶）
   - 库存单位：瓶
   - 消耗单位：毫升
   - 成本如何在不同UOM间转换？

3. **成本时机问题**：
   - 采购成本在入库时确定
   - 库存成本可能随时间变化（加权平均）
   - 消耗成本在发生时计算

---

## 3. 成本流转语义分析

### 3.1 可乐采购示例

```
采购：100瓶 × 1.50元/瓶 = 150元
      ↓
库存：100瓶，unit_cost = 1.50元/瓶
      ↓
销售：1瓶，sale_cost = 1.50元
      ↓
Recipe消耗：100ml（1瓶 = 500ml）
      ↓
消耗成本：100ml × (1.50元 / 500ml) = 0.30元
```

### 3.2 成本转换链

```
采购成本 (Purchase Cost)
    ↓ 入库时记录
库存成本 (Stock Cost)
    ↓ UOM转换
消耗成本 (Consumption Cost)
    ↓ 配方计算
菜品成本 (Dish Cost)
    ↓ 销售时结转
销售成本 (Sale Cost)
```

### 3.3 关键转换公式

```
单位成本转换：
new_unit_cost = old_unit_cost × (old_uom_factor / new_uom_factor)

例如：
采购UOM：箱 (100瓶)
库存UOM：瓶
消耗UOM：毫升

采购成本：150元/箱
库存成本：150元/箱 ÷ 100瓶/箱 = 1.50元/瓶
消耗成本：1.50元/瓶 ÷ 500ml/瓶 = 0.003元/ml
```

---

## 4. 成本语义规则

### 4.1 成本一致性规则

1. **入库成本锁定规则**：
   - 采购入库后，unit_cost 不变
   - 总成本 = 数量 × 单位成本

2. **成本守恒规则**：
   - 调拨不改变总成本，只改变位置
   - 损耗成本 = 库存数量 × unit_cost
   - 盘点调整调整总成本

3. **UOM转换成本规则**：
   - 成本随UOM转换而按比例转换
   - 转换过程不产生成本差异

### 4.2 成本计算优先级

```
1. 采购入库成本 → 最权威的成本来源
2. 库存加权平均成本 → 库存持有成本
3. 配方标准成本 → 计划成本
4. 配方实际成本 → 实际消耗成本
```

---

## 5. 成本分析矩阵

### 5.1 成本类型与业务事件映射

| 业务事件 | 成本类型 | 数量变化 | 成本变化 | 数据来源 |
|----------|----------|----------|----------|----------|
| 采购入库 | Purchase Cost | +入库数量 | +采购成本 | purchase_stockin_items |
| 销售出库 | Sale Cost | -销售数量 | -销售成本 | order_items |
| 配方消耗 | Consumption Cost | -消耗数量 | -消耗成本 | material_consumption |
| 报废损耗 | Waste Cost | -损耗数量 | -损耗成本 | inventory_transactions(type=5/6) |
| 仓库调拨 | Transfer Cost | ±调拨数量 | 不变 | inventory_transactions(type=3/4) |
| 门店调拨 | Transfer Cost | ±调拨数量 | 不变 | inventory_transactions(type=3/4) |
| 盘点调整 | Adjustment Cost | ±调整数量 | ±调整成本 | inventory_transactions(type=7) |

### 5.2 成本计算场景

| 场景 | 计算公式 | 数据需求 | 复杂度 |
|------|----------|----------|--------|
| 采购入库成本 | cost = quantity × unit_cost | purchase_stockin_items | 低 |
| 库存持有成本 | cost = current_stock × unit_cost | inventory/unit_cost | 低 |
| 配方消耗成本 | cost = Σ(ingredient_quantity × ingredient_unit_cost) | dish_recipe | 中 |
| 销售成本 | cost = sale_quantity × unit_cost | order_items | 中 |
| 损耗成本 | cost = waste_quantity × unit_cost | inventory_transactions | 低 |
| 调拨成本 | cost = transfer_quantity × unit_cost | inventory_transactions | 低 |
| 盘点成本 | cost = adjustment_quantity × unit_cost | inventory_transactions | 低 |

---

## 6. 成本转换语义

### 6.1 UOM转换对成本的影响

```
场景：可乐从仓库调拨到门店

仓库库存：100瓶，unit_cost = 1.50元/瓶，total_cost = 150元
         ↓ 调拨50瓶
门店库存：50瓶，unit_cost = 1.50元/瓶，total_cost = 75元

关键点：
1. 调拨不改变单位成本
2. 调拨不改变总成本（150元 = 75元 + 75元）
3. 调拨只改变成本的位置分布
```

### 6.2 Recipe消耗的成本转换

```
场景：制作可乐鸡翅消耗可乐

Recipe要求：可乐 200ml
库存单位：瓶 (500ml)
成本转换：
- 消耗成本 = 200ml × (1.50元 / 500ml) = 0.60元
- 剩余库存：300ml（0.6瓶）

关键点：
1. 消耗成本按UOM转换比例计算
2. 剩余库存成本按剩余数量计算
3. 成本守恒：0.60元 + 0.90元 = 1.50元
```

---

## 7. 成本语义规则总结

### 7.1 核心原则

1. **成本来源唯一性**：
   - 采购成本是初始成本来源
   - 其他成本类型都是采购成本的转化或分配

2. **成本守恒原则**：
   - 总成本在流转过程中保持不变
   - 成本只是在不同位置、不同形态间转移

3. **UOM转换原则**：
   - 成本随UOM转换按比例转换
   - 转换过程不产生成本差异

### 7.2 成本计算规则

1. **入库成本规则**：
   - 采购入库：unit_cost = purchase_price
   - 调拨入库：unit_cost = 调出方unit_cost
   - 盘盈入库：unit_cost = 当前库存unit_cost

2. **出库成本规则**：
   - 销售出库：cost = sale_quantity × unit_cost
   - 配方消耗：cost = Σ(consumption_quantity × ingredient_unit_cost)
   - 报废损耗：cost = waste_quantity × unit_cost
   - 调拨出库：cost = transfer_quantity × unit_cost

3. **库存成本规则**：
   - 加权平均成本 = Σ(total_cost) / Σ(quantity)
   - 成本调整：盘点差异调整总成本

---

## 8. 当前系统问题分析

### 8.1 成本数据不一致问题

1. **仓库成本 vs 门店成本**：
   - inventory.unit_cost 可能与 store_inventory.unit_cost 不一致
   - 原因：调拨时成本转换逻辑不统一

2. **采购成本 vs 库存成本**：
   - purchase_stockin_items.unit_cost 与 inventory.unit_cost 可能不一致
   - 原因：多次采购的加权平均计算

3. **标准成本 vs 实际成本**：
   - dish_recipe.estimated_cost 与 actual_cost 可能不一致
   - 原因：采购价格波动、消耗效率差异

### 8.2 成本计算时机问题

1. **入库时机**：
   - 采购入库时立即计算成本
   - 但成本可能需要后续调整（如采购折扣）

2. **消耗时机**：
   - 配方消耗时计算成本
   - 但实际消耗可能与标准消耗有差异

3. **销售时机**：
   - 销售时结转成本
   - 但成本可能需要后续调整（如退货）

---

## 9. 成本语义模型建议

### 9.1 成本数据模型

```
成本事件表 (cost_events)
├── event_id
├── event_type (purchase/sale/consumption/waste/transfer/adjustment)
├── material_id
├── uom_id
├── quantity
├── unit_cost
├── total_cost
├── source_type (inventory/store_inventory/purchase_stockin_items)
├── source_id
├── created_at
└── business_date
```

### 9.2 成本转换规则

```
成本转换规则表 (cost_conversion_rules)
├── rule_id
├── source_uom_id
├── target_uom_id
├── conversion_factor
├── cost_adjustment_factor (默认=1)
├── effective_date
└── expiry_date
```

---

## 10. 下一步行动

### 10.1 待解决问题

1. **成本一致性**：如何确保不同位置的库存成本一致？
2. **成本时效性**：如何处理采购价格波动？
3. **成本准确性**：如何确保消耗成本计算准确？
4. **成本追溯性**：如何追溯成本的来源和去向？

### 10.2 语义模型设计方向

1. **统一成本事件模型**：所有成本变动都记录为成本事件
2. **成本转换规则**：明确UOM转换时的成本转换规则
3. **成本追溯链**：建立成本从采购到消耗的完整追溯链
4. **成本一致性检查**：定期检查成本数据的一致性