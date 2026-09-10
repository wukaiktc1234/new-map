# 采购模型分析 (Procurement Model)

## 1. 问题域

本分析回答三个核心问题：
1. 采购的是 Product、Material 还是 Purchasable Item？
2. 采购对象和库存对象是否应该是同一个概念？
3. 采购流程中各实体的关系和职责边界是什么？

---

## 2. 现有系统数据流追踪

### 2.1 表结构依赖图

```
suppliers (供应商主数据)
  │
  └──► purchase_request (采购申请单 - 门店发起)
  │       ├── request_id
  │       ├── store_id / warehouse_id
  │       └── status: 0待提交 1待审批 2已审批 3已驳回 4已完成
  │
  └──► purchase_plan (采购计划)
  │       ├── plan_id
  │       └── material_id, planned_quantity
  │
  └──► purchase_orders (采购订单 - 主表)
  │       ├── order_id
  │       ├── supplier_id → suppliers
  │       ├── warehouse_id → 收货仓库
  │       └── order_status: 0草稿 1待审核 2已审核 3部分入库 4已完成 5已取消
  │       │
  │       └──► purchase_order_items (采购订单明细)
  │               ├── item_id
  │               ├── material_id → material_archives
  │               ├── quantity, unit_price, amount
  │               ├── received_quantity (已收货数量)
  │               └── planned_store_id / planned_warehouse_id (计划收货方)
  │
  └──► purchase_stockins (采购入库单 - 主表)
  │       ├── stockin_id
  │       ├── order_id → purchase_orders
  │       ├── warehouse_id → 入库仓库
  │       └── stockin_type: 1正常入库 2退货入库 3赠品入库
  │       │
  │       └──► purchase_stockin_items (采购入库明细)
  │               ├── stockin_item_id
  │               ├── material_id → material_archives
  │               ├── actual_quantity, unit_price
  │               ├── batch_no, production_date, expiry_date
  │               └── location_id → inventory_locations
  │
  └──► purchase_return (采购退货)
  │
  └──► purchase_settlements (采购结算)
          ├── settlement_id
          ├── order_id → purchase_orders
          └── supplier_id → suppliers
```

### 2.2 关键发现

| 发现 | 证据 | 影响 |
|------|------|------|
| 采购订单明细使用 `material_id` | `purchase_order_items.material_id` | 采购对象 = material |
| 采购入库明细也使用 `material_id` | `purchase_stockin_items.material_id` | 入库对象 = material |
| `material_archives` 是采购的基础主数据 | 建表注释："采购物料/商品的基础信息维护" | Material 是采购和库存的共享概念 |
| 采购订单支持"计划收货方" | `planned_store_id` / `planned_warehouse_id` | 采购可以直送门店或入仓库 |
| 入库单关联采购订单 | `purchase_stockins.order_id → purchase_orders.order_id` | 入库是对订单的履约 |

### 2.3 采购流程状态机

```
purchase_request (采购申请)
  │ 审批通过
  ▼
purchase_plan (采购计划) ── 可选 ──► purchase_orders (采购订单)
                                        │ 创建
                                        ▼
                                   purchase_order_items (订单明细)
                                        │ 供应商确认
                                        ▼
                                   purchase_stockins (入库单)
                                        │ 收货确认
                                        ▼
                                   purchase_stockin_items (入库明细)
                                        │ 入库完成
                                        ▼
                                   inventory (仓库库存 +)
                                   store_inventory (门店库存 +)
                                   inventory_transactions (流水记录)
```

---

## 3. 采购的是什么？

### 3.1 候选概念分析

| 候选概念 | 支持证据 | 反对证据 | 结论 |
|----------|----------|----------|------|
| **Product** | 历史遗留字段 `product_id` 在 inventory_log 中 | 采购订单明细已用 `material_id`；material_archives 是采购主数据 | ❌ 已被淘汰 |
| **Material** | 所有活跃采购表都使用 `material_id`；material_archives 注释为"采购物料" | material_archives 同时也被叫作"商品档案" | ✅ 当前实际使用的概念 |
| **Purchasable Item** | 理论上更精确的命名 | 系统中无此实体；Material 已承担此职责 | ⚠️ 理想模型但未实现 |
| **Food** | foods 是门店销售的菜品 | 采购原料（如土豆、鸡翅）不是 foods | ❌ 粒度不匹配 |

### 3.2 结论：采购对象 = Material

**证据链**：

```
采购申请:  purchase_request → 关联 material_id
    ↓
采购计划:  purchase_plan → material_id, planned_quantity
    ↓
采购订单:  purchase_order_items.material_id → material_archives.material_id
    ↓
采购入库:  purchase_stockin_items.material_id → material_archives.material_id
    ↓
库存更新:  inventory.material_id = purchase_stockin_items.material_id
```

`material_id` 贯穿采购全流程。采购的对象就是 `material_archives` 中定义的 Material。

---

## 4. 采购对象和库存对象是同一个概念吗？

### 4.1 直接对比

| 维度 | 采购对象 | 库存对象 | 是否相同？ |
|------|----------|----------|-----------|
| 标识 | `purchase_order_items.material_id` | `inventory.material_id` | ✅ 相同 |
| 主数据源 | `material_archives` | `material_archives` | ✅ 同一张表 |
| 名称 | material_name | material_name (冗余) | ✅ 一致 |
| 单位 | unit | unit | ✅ 一致 |
| 规格 | specification | specification | ✅ 一致 |

### 4.2 语义差异

虽然物理上是同一个 `material_id`，但在不同业务场景中，Material 承担了不同角色：

| 场景 | 角色 | 所需数据 | 消费者 |
|------|------|----------|--------|
| 采购 | **Purchasable** | material_id, supplier_id, reference_price, unit, spec | purchase_order_items, purchase_stockin_items |
| 入库 | **Stockable** | material_id, warehouse_id, batch_no, unit_cost | inventory, inventory_transactions |
| 门店库存 | **Store-Stockable** | material_id, store_id, current_stock | store_inventory |
| 领料消耗 | **Consumable** | material_id, quantity | material_consumption |
| 销售 | **Sellable** | food_id（不是 material_id） | sales_order_items |

### 4.3 关键区别：采购和销售的对象不同

```
采购维度 (Material-centric):
  purchase_order_items.material_id ──► material_archives
  "我从供应商买的是什么？" → 土豆5kg装

销售维度 (Food-centric):
  sales_order_items.food_id ──► foods
  "我卖给顾客的是什么？" → 宫保鸡丁

转换关系 (Recipe/BOM):
  foods (成品) ──recipe──► materials (原料)
  "宫保鸡丁需要：鸡胸肉300g、花生50g、辣椒20g"
```

**结论**：采购对象和库存对象是**同一个概念**（Material），但采购对象和销售对象**不是同一个概念**（Material vs Food）。Recipe/BOM 是它们之间的桥梁。

---

## 5. 采购模型的核心实体关系

### 5.1 实体职责矩阵

| 实体 | 职责 | 核心字段 | 状态机 |
|------|------|----------|--------|
| `material_archives` | 物料主数据（Canonical Identity） | material_id, material_code, material_name, unit, spec, reference_price, supplier_id | 1启用 / 0停用 |
| `suppliers` | 供应商主数据 | supplier_id, supplier_name, contact_info | — |
| `purchase_request` | 采购申请（门店发起） | store_id, material_id, quantity | 0待提交→1待审批→2已审批/3已驳回→4已完成 |
| `purchase_plan` | 采购计划（汇总） | plan_id, material_id, planned_quantity | — |
| `purchase_orders` | 采购订单（合同） | order_id, supplier_id, warehouse_id, total_amount | 0草稿→1待审核→2已审核→3部分入库→4已完成/5已取消 |
| `purchase_order_items` | 订单明细（行项） | material_id, quantity, unit_price, received_quantity | — |
| `purchase_stockins` | 入库单（收货确认） | stockin_id, order_id, warehouse_id | 0待入库→1已入库→2部分入库 |
| `purchase_stockin_items` | 入库明细（行项） | material_id, actual_quantity, batch_no | — |
| `purchase_return` | 退货单 | return_id, order_id | — |
| `purchase_settlements` | 结算单（财务） | settlement_id, order_id, total_amount, paid_amount | 0待结算→...→3已完成 |

### 5.2 采购与库存的衔接点

```
采购入库完成时：
  1. purchase_stockin_items.material_id → 确定入库物料
  2. purchase_stockin_items.actual_quantity → 入库数量
  3. purchase_stockin_items.unit_price → 入库成本
  4. purchase_stockins.warehouse_id → 确定入库仓库
  5. purchase_stockin_items.location_id → 确定入库库位

库存更新逻辑：
  inventory(material_id, warehouse_id).current_stock += actual_quantity
  inventory(material_id, warehouse_id).unit_cost = unit_price (或加权平均)
  inventory_transactions 插入一条 transaction_type=1 (采购入库) 的记录
```

---

## 6. 采购模型中的角色分析

### 6.1 Purchasable Role（可采购角色）

基于 `business-role-model.md` 的定义，Purchasable 角色包含：

**Purchasable 角色的配置数据**：

| 数据项 | 当前存储位置 | 说明 |
|--------|-------------|------|
| 物料名称 | material_archives.material_name | — |
| 物料编码 | material_archives.material_code | 业务唯一编码 |
| 采购单位 | material_archives.unit | 斤/袋/箱 |
| 规格型号 | material_archives.spec | 10kg/箱 |
| 参考价 | material_archives.reference_price | 分为单位 |
| 主供应商 | material_archives.supplier_id | 关联 suppliers |
| 分类 | material_archives.category_id | 关联 material_categories |
| 条码 | material_archives.barcode | 扫码用 |
| 产地 | material_archives.origin | — |
| 保质期 | material_archives.shelfLife | 字段已定义但未在采购流程使用 |

### 6.2 采购角色与库存角色的关系

```
material_archives
  │
  ├── [Purchasable Role]
  │     数据: reference_price, supplier_id, unit, spec
  │     消费者: purchase_order_items, purchase_stockin_items
  │     上游: 无（独立角色）
  │     下游: Stockable
  │
  └── [Stockable Role]
        数据: min_safe_qty, max_stock_qty (仓库); safety_stock, max_stock (门店)
        消费者: inventory, store_inventory
        上游: Purchasable
        下游: Consumable (领料) / Sellable (销售)
```

**关键洞察**：Purchasable 和 Stockable 是同一个 Material 在不同业务场景中的角色。它们共享 material_id，但各自携带不同的业务数据。

---

## 7. 采购流程中的多维度设计

### 7.1 支持"直送门店"的采购模式

`purchase_order_items` 中的 `planned_store_id` 和 `planned_warehouse_id` 字段表明系统支持两种采购收货模式：

| 模式 | 字段 | 流程 |
|------|------|------|
| **仓库收货** | `planned_warehouse_id` | 采购→仓库收货→入库→可能需要门店调拨 |
| **直送门店** | `planned_store_id` | 采购→直接到门店→门店收货确认 |

### 7.2 采购与门店库存的关系

```
模式A: 仓库采购
  purchase_orders → purchase_stockins → inventory (仓库)
       └──→ inventory_transactions (type=1)
       └──→ 后续: inventory_transfers → store_inventory (门店)

模式B: 直送门店
  purchase_orders → purchase_stockins → store_inventory (门店)
       └──→ store_inventory_log
```

### 7.3 成本流转路径

```
采购单价 (unit_price in purchase_order_items)
    ↓ 入库
入库成本 (unit_cost in purchase_stockin_items)
    ↓ 更新
库存成本 (unit_cost in inventory)
    ↓ 调拨
门店成本 (unit_cost in store_inventory)
```

**当前问题**：
- 仓库入库时的 `unit_cost` 与门店入库时的 `unit_cost` 如何保持一致？
- 如果有中间调拨，调拨成本如何计算？
- 系统中未见明确的库存成本核算规则（如加权平均法、先进先出法）

---

## 8. 遗留问题与技术债

### 8.1 命名不一致

| 问题 | 位置 | 建议 |
|------|------|------|
| `material_archives` 注释为"商品档案" | 表注释 | 统一为"物料主数据" |
| `purchase_stockin_items.unit_price` vs `inventory.unit_cost` | 列名不同 | 入库后 unit_price 应转化为 unit_cost |
| `purchase_stockin_items` 字段 `unit_price` 在表注释中叫"单价"，实体注释也叫"单价" | — | 语义清晰但与库存的 cost 概念有混淆 |

### 8.2 缺失的关联

| 缺失 | 影响 | 建议 |
|------|------|------|
| `purchase_order_items` 无 `unit_cost` 字段 | 无法直接从订单行获取成本价 | 可接受，因为 `unit_price` 即为成本价 |
| `purchase_stockin_items` 无 `store_id` | 直送门店场景无法在入库单上标记门店 | 应补充 store_id 字段 |
| `purchase_return_items` 表结构缺失 | 退货明细未找到独立建表 | 需确认是否在 purchase_return 主表中存储 |

### 8.3 采购与库存的成本一致性

- `purchase_stockin_items.unit_price` (分) 是采购价
- `inventory.unit_cost` (分) 是库存成本价
- 入库时应将 `unit_price` 更新为 `unit_cost`，但更新规则（先进先出？加权平均？）未定义
- `store_inventory.unit_cost` 来源不明确（是采购价？还是调拨价？）

---

## 9. 推荐改进方向

### 9.1 短期（保持现有结构）

1. **统一 material_id 口径**：确保所有新增采购表使用 `material_id`
2. **清理 inventory_log.product_id**：后续迁移中添加 material_id 列
3. **补充 purchase_stockin_items.store_id**：支持直送门店场景

### 9.2 中期（规范采购模型）

1. **创建 `material_purchasable_config` 表**：将 `reference_price`、`supplier_id`、最小起订量等 Purchasable 角色数据统一管理
2. **定义成本核算规则**：明确 unit_price → unit_cost 的转化逻辑
3. **完善退货明细表**：如果需要，创建 `purchase_return_items` 表

### 9.3 长期（统一 Item 模型）

1. **Material 作为 Canonical Item**：明确 material_archives 的"物料主数据"定位
2. **Role-based 架构**：Purchasable、Stockable、Consumable 作为 Material 的角色
3. **位置模型统一**：warehouse_id 和 store_id 统一为 location_id + location_type

---

## 10. 总结

| 问题 | 结论 |
|------|------|
| 采购的是什么？ | **Material**（物料），以 material_id 为 canonical identity |
| 采购对象和库存对象是否相同？ | **是**。采购和库存共享 material_id，是同一个 Canonical Item 在不同场景中的角色 |
| Purchasable 是什么？ | 是 Material 的一个 **Business Role**，表示"可以从供应商购买" |
| 采购和销售的对象相同吗？ | **否**。采购用 Material，销售用 Food，通过 Recipe/BOM 桥接 |
| 当前模型的最大问题？ | 成本流转规则不明确；直送门店场景缺少 store_id；命名不统一 |
