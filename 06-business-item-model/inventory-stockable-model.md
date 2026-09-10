# 库存可存储模型分析 (Inventory Stockable Model)

## 1. 问题域

本分析回答三个核心问题：
1. `inventory` 表到底管理什么？Material？Product？Stock Item？
2. Stockable 应该是 Item 的属性、Role 还是 Capability？
3. 库存管理的核心抽象应该是什么概念？

---

## 2. 现有系统数据流追踪

### 2.1 表结构依赖图

```
material_archives (商品档案 - 主数据)
  │
  ├──► inventory (仓库库存 - 按 warehouse_id + material_id)
  │       ├── inventory_id (PK)
  │       ├── material_id (FK → material_archives)
  │       ├── warehouse_id
  │       ├── current_stock, locked_quantity
  │       ├── unit_cost, total_cost
  │       ├── batch_no, production_date, expiry_date
  │       └── min_safe_qty, max_stock_qty
  │
  ├──► store_inventory (门店库存 - 按 store_id + material_id)
  │       ├── id (PK)
  │       ├── store_id
  │       ├── material_id (FK → material_archives)
  │       ├── current_stock, unit_cost, total_cost
  │       └── safety_stock, max_stock
  │
  └──► inventory_transactions (库存流水)
          ├── transaction_id (PK)
          ├── material_id (冗余，便于查询)
          ├── inventory_id (FK → inventory)
          ├── warehouse_id
          ├── quantity_change, before_qty, after_qty
          └── unit_cost, total_cost
```

### 2.2 关键发现

| 发现 | 证据 | 影响 |
|------|------|------|
| 库存管理的粒度是 `material_id` | `inventory.material_id`, `store_inventory.material_id` | 库存项 = material 的某位置实例 |
| 仓库库存和门店库存完全独立 | `inventory` 用 `warehouse_id`，`store_inventory` 用 `store_id`，两表无外键关系 | 存在双维度库存模型 |
| 采购入库通过 `material_id` 关联 | `purchase_stockin_items.material_id`, `purchase_order_items.material_id` | 采购和库存共享同一 material 概念 |
| `inventory_log` 仍使用 `product_id` | V1.0.0.100 遗留字段，注释为"产品/物料ID" | 历史遗留，语义混乱 |
| `Inventory` Java 实体有兼容性 getter | `getProductId()` 映射到 `materialId` | 代码层承认了 product_id → material_id 的历史演变 |

### 2.3 从 V1.0.0.100 到当前的演进路径

```
V1.0.0.100:
  inventory (product_id, warehouse_id, store_id)  ← 单表管理所有库存
  ↓
V20260704_004:
  store_inventory (store_id, material_id)  ← 门店库存独立建表
  ↓
V20260723_005:
  inventory_transactions (material_id, inventory_id, warehouse_id)  ← 流水表重建
  ↓
V20260816_001:
  inventory 补齐 material_name, specification, location_id, locked_quantity 等 11 列
  ← 物料口径对齐
```

**结论**：系统从 product-centric 演进为 material-centric，但演进过程中产生了遗留字段和命名不一致。

---

## 3. Inventory 管理的到底是什么？

### 3.1 候选概念分析

| 候选概念 | 支持证据 | 反对证据 | 结论 |
|----------|----------|----------|------|
| **Product** | `inventory_log.product_id`；`getProductId()` 兼容方法 | 当前表结构已无 `product_id` 列；采购用 `material_id`；门店库存也用 `material_id` | ❌ 已被历史淘汰 |
| **Food** | `foods` 表是门店销售的菜品 | `inventory.material_id ≠ food_id`；原料和成品都入库但 food 只是成品 | ❌ 粒度不匹配 |
| **Material** | 所有活跃表都使用 `material_id`；采购订单用 `material_id`；门店库存用 `material_id` | `material_archives` 叫"商品档案"而非"物料档案"；注释存在 "物料" 和 "商品" 混用 | ⚠️ 最接近但语义需澄清 |
| **Stock Item** | 库存表是 (material, location) 的实例化 | 不存在独立的 StockItem 实体；库存直接用 material_id + warehouse_id | ⚠️ 概念正确但未建模 |
| **Canonical Item** | 跨所有场景的唯一标识 | 系统中无此实体；Material 已承担此角色 | ⚠️ 理想模型但未实现 |

### 3.2 结论：Material 是当前系统的 Canonical Identity

**证据链**：

```
采购阶段:  purchase_order_items.material_id → material_archives.material_id
                    ↓
入库阶段:  purchase_stockin_items.material_id → material_archives.material_id
                    ↓
仓库库存:  inventory.material_id → material_archives.material_id
                    ↓
门店库存:  store_inventory.material_id → material_archives.material_id
                    ↓
库存流水:  inventory_transactions.material_id → material_archives.material_id
                    ↓
领料消耗:  material_consumption.material_id → material_archives.material_id
```

`material_id` 是贯穿全生命周期的唯一标识。`material_archives` 表承担了 Canonical Item 的职责。

### 3.3 Inventory 的语义重新定义

**Inventory = Material 在特定 Location 的数量实例**

用公式表示：

```
inventory(material_id, warehouse_id) → {
    current_stock,        -- 可用数量
    locked_quantity,      -- 锁定数量
    unit_cost, total_cost, -- 成本信息
    batch_no,             -- 批次追踪
    min_safe_qty, max_stock_qty  -- 预警阈值
}
```

**Store Inventory = Material 在特定 Store 的数量实例**

```
store_inventory(material_id, store_id) → {
    current_stock,
    unit_cost, total_cost,
    safety_stock, max_stock
}
```

---

## 4. Stockable 是属性、Role 还是 Capability？

### 4.1 三种建模范式对比

#### 范式 A：Stockable 作为属性（Attribute）

```typescript
// Material 是主体，stockable 是其属性之一
interface Material {
  id: MaterialId;
  name: string;
  unit: string;
  stockable: boolean;       // 是否可存储
  purchasable: boolean;     // 是否可采购
  sellable: boolean;        // 是否可销售
  consumable: boolean;      // 是否可消耗
}
```

- **优点**：简单，查询快
- **缺点**：布尔属性无法表达存储的具体能力（如保质期要求、温区要求）
- **适用场景**：快速原型，简单业务

#### 范式 B：Stockable 作为 Role（角色）

```typescript
// Material 是主体，Stockable 是其扮演的角色之一
interface Material { id: MaterialId; ... }

interface StockableRole {
  materialId: MaterialId;
  warehouseId: WarehouseId;
  safetyStock: number;
  maxStock: number;
  shelfLife?: number;    // 保质期（天）
  storageTemp?: string;  // 存储温度
}

// 库存表 = StockableRole 的实例化
interface InventoryRecord {
  stockableRole: StockableRole;
  currentStock: number;
  batchNo: string;
  ...
}
```

- **优点**：角色携带业务语义；一个 Material 可以在不同仓库扮演不同 StockableRole
- **缺点**：查询链变长；需要 Role 表
- **适用场景**：多仓库、多存储条件的中型系统

#### 范式 C：Stockable 作为 Capability（能力声明）

```typescript
// Material 声明自己具备 stockable 能力，能力由具体模块实现
interface Material {
  id: MaterialId;
  capabilities: Capability[];
}

type Capability =
  | { type: 'stockable'; config: StockableConfig }
  | { type: 'purchasable'; config: PurchasableConfig }
  | { type: 'sellable'; config: SellableConfig }
  | { type: 'consumable'; config: ConsumableConfig };
```

- **优点**：高度灵活；能力可动态添加
- **缺点**：过度抽象；当前系统无需此复杂度
- **适用场景**：大型平台化系统

### 4.2 推荐：Stockable 作为 Role（角色）

**理由**：

1. **系统复杂度匹配**：当前系统是中型餐饮 ERP，Role 范式的复杂度恰到好处
2. **已有建模基础**：`business-role-model.md` 已定义了 SELLABLE / PURCHASABLE / STOCKABLE / INGREDIENT 四种角色
3. **库存双维度**：`inventory` 和 `store_inventory` 实质上是 StockableRole 在不同位置的实例
4. **与采购模型衔接**：StockableRole 的上游是 PURCHASABLE，下游是 CONSUMABLE

### 4.3 Stockable Role 的数据模型

```
material_archives          (Canonical Item / 主数据)
       │
       ├── material_categories  (分类信息)
       │
       └── stockable_config     (可存储能力配置 - 新增)
            │
            ├── inventory       (仓库维度实例)
            └── store_inventory (门店维度实例)
```

**推荐的 StockableRole 配置字段**（当前分散在多表中）：

| 字段 | 当前位置 | 语义 |
|------|----------|------|
| min_safe_qty | inventory | 仓库安全库存 |
| max_stock_qty | inventory | 仓库最大库存 |
| safety_stock | store_inventory | 门店安全库存 |
| max_stock | store_inventory | 门店最大库存 |
| shelf_life | material_archives (有字段但未用) | 保质期 |
| storage_condition | material_archives (有字段但未用) | 存储条件 |

---

## 5. 库存管理的核心抽象模型

### 5.1 推荐模型：三层架构

```
Layer 1: Canonical Identity (标识层)
  └── material_archives (material_id)
        "这是什么？" → 一袋5kg装的东北大米

Layer 2: Stockable Role (能力层)
  └── stockable_config (material_id, capability_type)
        "它能被存储吗？" → 能，需干燥环境，保质期180天

Layer 3: Inventory Instance (实例层)
  ├── inventory (material_id, warehouse_id)  → 仓库中的具体库存
  └── store_inventory (material_id, store_id) → 门店中的具体库存
        "它现在在哪里？有多少？" → 仓库A有200袋，门店B有10袋
```

### 5.2 与业务流程的映射

```
                    material_archives
                         │
    ┌────────────────────┼────────────────────┐
    ▼                    ▼                    ▼
 [采购场景]          [库存场景]           [销售场景]
 PURCHASABLE        STOCKABLE            SELLABLE
    │                    │                    │
 purchase_order    inventory/            foods/
 _items             store_inventory      sales_order
    │                    │                    │
    └──────── material_id ────────────────────┘
              (Canonical Identity)
```

### 5.3 库存流水的语义

`inventory_transactions.transaction_type` 编码的业务语义：

| type | 操作 | 来源 | 目标 | material_id 变化 |
|------|------|------|------|-----------------|
| 1 | 采购入库 | 采购订单 | 仓库库存 | + |
| 2 | 销售出库 | 仓库库存/门店库存 | 门店销售 | - |
| 3 | 调拨出 | 源仓库/门店 | - | - |
| 4 | 调拨入 | - | 目标仓库/门店 | + |
| 5 | 盘点盈 | - | 库存 | + |
| 6 | 盘点亏 | 库存 | - | - |
| 7 | 报损 | 库存 | 报损记录 | - |
| 8 | 退货 | 客户退回 | 库存 | + |

**关键洞察**：流水表同时记录了 `inventory_id` 和 `material_id`。`inventory_id` 是外键关联到具体库存实例，`material_id` 是冗余字段便于跨仓库聚合查询。这说明库存流水的主语是 **"material 在某个 location 的数量变动"**。

---

## 6. 遗留问题与技术债

### 6.1 命名不一致

| 问题 | 位置 | 建议 |
|------|------|------|
| `material_archives` 叫"商品档案"而非"物料档案" | 表注释 | 统一为"物料主数据"或接受"商品档案"作为采购视角的名称 |
| `inventory_log.product_id` 仍使用 product_id | V1.0.0.100 遗留 | 应迁移为 material_id |
| `Inventory.java` 中 `getProductId()` 返回 `materialId` | 兼容性方法 | 应标记 @Deprecated |
| `store_inventory` 缺少 `unit_cost` / `total_cost` 的初始建表字段 | V20260704_004 vs V20260705_001 | 已通过后续迁移补齐 |

### 6.2 双维度库存的同步问题

- `inventory` 和 `store_inventory` 是独立表，无事务一致性保证
- 门店收货确认时需要同时更新两个表
- 缺少库存同步机制（如：门店申请 → 仓库调拨 → 双表联动）

### 6.3 成本核算的割裂

- `inventory.unit_cost` / `inventory.total_cost` 与 `store_inventory.unit_cost` / `store_inventory.total_cost` 独立维护
- 采购入库单有 `unit_price`，入库后更新 `inventory.unit_cost`，但门店调拨时成本如何传递未定义

---

## 7. 推荐改进方向

### 7.1 短期（保持现有结构）

1. **统一 material_id 口径**：确保所有新增表和字段使用 `material_id`，不使用 `product_id`
2. **清理 Inventory.java 的兼容性方法**：标记 `getProductId()` 为 @Deprecated
3. **修复 inventory_log.product_id**：在后续迁移中添加 `material_id` 列并回填数据

### 7.2 中期（引入 Stockable Role 配置）

1. **创建 `material_stockable_config` 表**：将分散在 inventory / store_inventory 的预警阈值、存储条件统一管理
2. **标准化库存预警**：基于 material_stockable_config 统一预警规则

### 7.3 长期（统一库存模型）

1. **考虑合并 inventory 和 store_inventory**：引入 `location_type` (WAREHOUSE/STORE) 字段，统一为一张表
2. **引入 StockItem 概念**：(material_id, location_id, batch_no) 作为库存记录的唯一标识

---

## 8. 总结

| 问题 | 结论 |
|------|------|
| Inventory 管理的是什么？ | **Material**（物料/商品），以 material_id 为 canonical identity |
| Stockable 是什么？ | 是 Material 的一个 **Business Role**（角色），表示"可以被存储" |
| 库存的核心抽象是什么？ | **Material + Location → Inventory Instance**（物料 + 位置 → 库存实例） |
| 仓库库存和门店库存的关系？ | 同一 Material 在不同 Location 类型的 StockableRole 实例化 |
