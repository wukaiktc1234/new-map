# Product / Material / Inventory 关系模型分析 (Phase 19)

> **核心问题**：Product、Material、Inventory 三者的关系应该是什么？独立实体？Canonical Item 的角色？还是 Profile？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

本分析必须回答：
1. Product、Material、Inventory 三者的关系应该是什么？
2. 至少比较三个模型：独立实体、角色模型、Profile 模型
3. 从业务表达、扩展性和跨域一致性比较
4. 用真实业务案例验证

**关键约束**：不得为了迁就现有数据库表结构定义最终业务模型。

---

## 2. 候选模型定义

### Model A: Product, Material, Inventory 完全独立

**核心思想**：Product、Material、Inventory 是三个独立的业务实体，各自有独立的标识和属性，通过外键或桥接表关联。

### Model B: Canonical Item → Product Role, Material Role, Stock Role

**核心思想**：引入 Canonical Item 作为统一身份，Product、Material、Stock 是 Canonical Item 在不同业务场景中的角色。

### Model C: Canonical Item → Commercial Profile, Operational Profile, Inventory Profile, Procurement Profile, Recipe Relationship

**核心思想**：引入 Canonical Item 作为统一身份，Product、Material、Inventory 等概念被分解为多个 Profile（商业定义、运营定义、库存定义、采购定义），每个 Profile 描述 Canonical Item 在特定业务域中的配置。

---

## 3. 当前系统现实分析

### 3.1 表结构依赖图

```
┌─────────────────────────────────────────────────────────────────┐
│                        当前系统架构                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  foods (菜品/食品 - 可销售)                                      │
│    ├── food_id (PK)                                             │
│    ├── food_code, food_name                                     │
│    ├── food_price, cost_price                                   │
│    └── food_category                                            │
│                                                                 │
│  product (产品/物料基础信息 - 已废弃)                              │
│    ├── product_id (PK)                                          │
│    ├── name, code                                               │
│    ├── unit, price, cost_price                                  │
│    └── supplier_id                                              │
│                                                                 │
│  material_archives (商品档案 - 可采购、可存储)                      │
│    ├── material_id (PK)                                         │
│    ├── material_code, material_name                             │
│    ├── unit, spec, reference_price                              │
│    └── supplier_id                                              │
│                                                                 │
│  inventory (库存管理)                                            │
│    ├── id (PK)                                                  │
│    ├── product_id (FK → material_archives)                      │
│    ├── warehouse_id                                             │
│    ├── current_stock, safety_stock                              │
│    └── cost_price, stock_value                                  │
│                                                                 │
│  store_inventory (门店库存)                                      │
│    ├── store_id, material_id (PK)                               │
│    ├── current_stock, unit_cost, total_cost                     │
│    └── safety_stock, max_stock                                  │
│                                                                 │
│  dish_recipe (菜品配方/BOM)                                      │
│    ├── dish_id (FK → foods)                                     │
│    ├── ingredient_id (FK → material_archives)                   │
│    └── quantity, unit                                           │
│                                                                 │
│  purchase_order_items (采购订单明细)                               │
│    ├── item_id (PK)                                             │
│    ├── order_id (FK → purchase_orders)                          │
│    ├── material_id (FK → material_archives)                     │
│    ├── quantity, unit_price                                     │
│    └── planned_store_id, planned_warehouse_id                   │
│                                                                 │
│  order_items / sales_order_items (销售订单明细)                   │
│    ├── food_id (FK → foods)                                     │
│    ├── quantity, price                                          │
│    └── ...                                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 身份碎片化现状

| 业务对象 | foods | material_archives | inventory | dish_recipe | purchase_order_items |
|----------|-------|-------------------|-----------|-------------|---------------------|
| 可乐 | food_id=F001 | material_id=M001 | product_id=M001(遗留) | ingredient_id=M001 | material_id=M001 |
| 鸡翅 | food_id=F002 | material_id=M002 | product_id=M002(遗留) | ingredient_id=M002 | material_id=M002 |
| 大米 | food_id=F003 | material_id=M003 | product_id=M003(遗留) | ingredient_id=M003 | material_id=M003 |
| 宫保鸡丁 | food_id=F010 | 通常不出现 | 通常不出现 | dish_id=F010 | 通常不出现 |

**关键发现**：
- 同一个"可乐"在 foods 和 material_archives 中有两个独立的记录
- inventory 使用 product_id（遗留字段）引用 material_archives
- dish_recipe 通过 ingredient_id 桥接 foods 和 material_archives
- purchase_order_items 通过 material_id 引用 material_archives

### 3.3 跨域查询的复杂性

**场景**：查询"可乐"在所有业务域中的完整信息。

**当前系统**：
```sql
SELECT
    f.food_id, f.food_name, f.food_price,
    m.material_id, m.material_name, m.reference_price,
    i.current_stock, i.cost_price,
    p.unit_price, p.quantity
FROM foods f
LEFT JOIN material_archives m ON f.food_code = m.material_code  -- 无外键，靠编码匹配
LEFT JOIN inventory i ON m.material_id = i.product_id           -- 遗留字段
LEFT JOIN purchase_order_items p ON m.material_id = p.material_id
WHERE f.food_name LIKE '%可乐%';
```

**问题**：
- foods 和 material_archives 之间无外键，靠编码匹配（不可靠）
- inventory 使用 product_id 而非 material_id（遗留问题）
- 查询复杂，性能差

---

## 4. Model A: Product, Material, Inventory 完全独立

### 4.1 架构描述

```
Product (独立实体)
  ├── id: product_id
  ├── name: "可口可乐"
  ├── code: "COLA001"
  ├── type: food | material | ...
  └── attributes...

Food (独立实体)
  ├── id: food_id
  ├── product_id: FK → Product
  ├── price: 3.00
  └── 销售属性...

Material (独立实体)
  ├── id: material_id
  ├── product_id: FK → Product
  ├── cost: 2.50
  └── 采购属性...

Stock Item (独立实体)
  ├── id: stock_item_id
  ├── material_id: FK → Material
  ├── quantity: 200
  └── 库存属性...
```

### 4.2 评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 业务表达能力 | ⭐⭐⭐ | 能表达所有当前业务 |
| 业务语义清晰度 | ⭐⭐⭐ | 每个实体职责明确 |
| 跨域一致性 | ⭐⭐ | 需要外键同步，冗余数据 |
| Identity 一致性 | ⭐ | 同一商品在不同域可能有不同记录 |
| 数据重复风险 | ⭐⭐ | 名称、属性等可能重复存储 |
| 扩展能力 | ⭐⭐ | 新增业务角色需要新建表 |
| 门店差异能力 | ⭐⭐⭐ | 每个门店可独立配置 |
| 采购能力 | ⭐⭐⭐ | Material 专为采购设计 |
| 库存能力 | ⭐⭐⭐ | Stock Item 专为库存设计 |
| POS 能力 | ⭐⭐⭐ | Food 专为销售设计 |
| Recipe 能力 | ⭐⭐⭐ | 通过关系表桥接 |
| Costing 能力 | ⭐⭐ | 需要跨实体计算 |
| Finance Integration | ⭐⭐ | 需要映射逻辑 |
| 历史数据兼容 | ⭐⭐⭐ | 最接近当前结构 |
| 迁移复杂度 | ⭐⭐⭐ | 变更最小 |
| 长期维护成本 | ⭐⭐ | 同步逻辑维护成本 |

### 4.3 业务案例验证

**案例一：可乐**

```
Product: id=P001, name="可口可乐", code="COLA001"
  │
  ├── Food: food_id=F001, product_id=P001, price=3.00
  │     └── sales_order_items: food_id=F001
  │
  ├── Material: material_id=M001, product_id=P001, cost=2.50
  │     ├── purchase_order_items: material_id=M001
  │     └── inventory: material_id=M001, quantity=200
  │
  └── Stock Item: stock_item_id=S001, material_id=M001, quantity=200
        └── inventory_transactions: stock_item_id=S001
```

**问题**：
- Product、Food、Material、Stock Item 四个独立实体，名称、编码等信息重复存储
- 跨域查询需要多次 JOIN
- 数据一致性难以保证

**案例二：宫保鸡丁**

```
Product: id=P010, name="宫保鸡丁", code="KPC001"
  │
  └── Food: food_id=F010, product_id=P010, price=28.00
        └── sales_order_items: food_id=F010
        └── dish_recipe: dish_id=F010, ingredient_id=M002 (鸡翅)
```

**问题**：宫保鸡丁通常不作为 Material 和 Stock Item，但 Product 实体仍然存在。

### 4.4 优缺点总结

**优点**：
- 最接近当前系统结构，迁移成本最低
- 每个实体职责明确
- 历史数据兼容性好

**缺点**：
- 身份碎片化：同一商品在不同域有不同记录
- 数据冗余：名称、编码等信息重复存储
- 跨域查询复杂：需要多次 JOIN
- 扩展性差：新增业务角色需要新建表

---

## 5. Model B: Canonical Item → Product Role, Material Role, Stock Role

### 5.1 架构描述

```
Canonical Item (统一身份)
  ├── id: canonical_item_id
  ├── name: "可口可乐330ml"
  ├── code: "SKU_COLA_330ML"
  ├── brand: "可口可乐"
  ├── specification: "330ml/罐"
  └── core_attributes...

Business Role (业务角色)
  ├── id: role_id
  ├── canonical_item_id: FK → Canonical Item
  ├── role_type: SELLABLE | PURCHASABLE | STOCKABLE | INGREDIENT | ...
  ├── domain_config: JSON (角色特定配置)
  ├── store_id: string? (门店级别角色)
  └── status: ACTIVE | INACTIVE
```

### 5.2 角色特定配置

| Role | 域 | 配置字段 |
|------|-----|----------|
| SELLABLE | POS | price, menu_config, display_info |
| PURCHASABLE | Procurement | supplier_id, purchase_unit, lead_time |
| STOCKABLE | Inventory | warehouse_id, safety_stock, max_stock |
| INGREDIENT | Recipe | recipe_id, quantity, yield |

### 5.3 评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 业务表达能力 | ⭐⭐⭐⭐ | 灵活的角色定义 |
| 业务语义清晰度 | ⭐⭐⭐⭐ | 统一身份 + 角色分离 |
| 跨域一致性 | ⭐⭐⭐⭐ | 天然一致，同一身份 |
| Identity 一致性 | ⭐⭐⭐⭐⭐ | 核心优势 |
| 数据重复风险 | ⭐⭐⭐⭐ | 最小化重复 |
| 扩展能力 | ⭐⭐⭐⭐⭐ | 新增角色只需新记录 |
| 门店差异能力 | ⭐⭐⭐ | 角色可按门店区分 |
| 采购能力 | ⭐⭐⭐⭐ | material 角色 |
| 库存能力 | ⭐⭐⭐⭐ | stock_item 角色 |
| POS 能力 | ⭐⭐⭐⭐ | food 角色 |
| Recipe 能力 | ⭐⭐⭐⭐ | 通过角色关联 |
| Costing 能力 | ⭐⭐⭐⭐ | 统一计算基础 |
| Finance Integration | ⭐⭐⭐⭐ | 清晰的映射 |
| 历史数据兼容 | ⭐⭐ | 需要转换逻辑 |
| 迁移复杂度 | ⭐ | 需要重构 |
| 长期维护成本 | ⭐⭐⭐⭐ | 统一维护 |

### 5.4 业务案例验证

**案例一：可乐**

```
Canonical Item: id=C001, name="可口可乐330ml", code="SKU_COLA_330ML"
  │
  ├── SELLABLE Role
  │     ├── role_id=R001
  │     ├── canonical_item_id=C001
  │     ├── role_type=SELLABLE
  │     └── config: {price: 3.00, menu_config: {...}}
  │
  ├── PURCHASABLE Role
  │     ├── role_id=R002
  │     ├── canonical_item_id=C001
  │     ├── role_type=PURCHASABLE
  │     └── config: {supplier_id: S001, purchase_unit: "罐", reference_price: 2.50}
  │
  ├── STOCKABLE Role
  │     ├── role_id=R003
  │     ├── canonical_item_id=C001
  │     ├── role_type=STOCKABLE
  │     └── config: {warehouse_id: W01, safety_stock: 50, max_stock: 500}
  │
  └── INGREDIENT Role
        ├── role_id=R004
        ├── canonical_item_id=C001
        ├── role_type=INGREDIENT
        └── config: {recipe_id: R001, quantity: 0.2}
```

**优点**：
- 同一身份，无碎片化
- 角色可独立添加/移除
- 跨域查询简单

**问题**：
- 角色配置使用 JSON，查询性能可能受影响
- 需要维护角色类型枚举

**案例二：宫保鸡丁**

```
Canonical Item: id=C010, name="宫保鸡丁", code="SKU_KPC_001"
  │
  ├── SELLABLE Role
  │     ├── role_id=R010
  │     ├── canonical_item_id=C010
  │     ├── role_type=SELLABLE
  │     └── config: {price: 28.00, menu_config: {...}}
  │
  └── (无 PURCHASABLE, STOCKABLE, INGREDIENT Role)
```

**优点**：宫保鸡丁只有 SELLABLE Role，不需要其他角色。

**案例三：同一物料的不同包装规格**

```
Canonical Item: id=C001, name="可口可乐330ml", code="SKU_COLA_330ML"
  │
  ├── PURCHASABLE Role 1
  │     ├── role_id=R002
  │     ├── canonical_item_id=C001
  │     ├── role_type=PURCHASABLE
  │     └── config: {supplier_id: S001, purchase_unit: "箱", purchase_spec: "330ml×24罐/箱", reference_price: 4800}
  │
  ├── PURCHASABLE Role 2
  │     ├── role_id=R005
  │     ├── canonical_item_id=C001
  │     ├── role_type=PURCHASABLE
  │     └── config: {supplier_id: S001, purchase_unit: "半箱", purchase_spec: "330ml×12罐/半箱", reference_price: 2500}
  │
  └── PURCHASABLE Role 3
        ├── role_id=R006
        ├── canonical_item_id=C001
        ├── role_type=PURCHASABLE
        └── config: {supplier_id: S002, purchase_unit: "箱", purchase_spec: "500ml×24瓶/箱", reference_price: 6800}
```

**优点**：支持同一物料的不同包装规格。

### 5.5 优缺点总结

**优点**：
- 身份一致性：同一商品只有一个 Canonical Item
- 数据最小化：名称、编码等信息只存储一次
- 灵活扩展：新增角色只需新记录
- 门店差异：角色可按门店区分

**缺点**：
- 迁移复杂度高：需要重构所有业务流程
- 角色配置使用 JSON：查询性能可能受影响
- 需要维护角色类型枚举

---

## 6. Model C: Canonical Item → Commercial Profile, Operational Profile, Inventory Profile, Procurement Profile, Recipe Relationship

### 6.1 架构描述

```
Canonical Item (统一身份)
  ├── id: canonical_item_id
  ├── name: "可口可乐330ml"
  ├── code: "SKU_COLA_330ML"
  └── core_attributes...

Commercial Definition (商业定义)
  ├── canonical_id: FK → Canonical Item
  ├── saleable: boolean
  ├── purchasable: boolean
  ├── storable: boolean
  └── 业务开关...

Operational Definition (运营定义)
  ├── canonical_id: FK → Canonical Item
  ├── domain: pos | procurement | inventory | recipe | costing
  ├── config (JSON/扩展)
  └── 领域特定配置...

Profile Views (配置视图/聚合)
  ├── Commercial View (商业视图)
  │   ├── price, POS visibility, menu_config
  │   └── ...
  ├── Procurement View (采购视图)
  │   ├── supplier, purchase_unit, lead_time
  │   └── ...
  ├── Inventory View (库存视图)
  │   ├── warehouse, safety_stock, max_stock
  │   └── ...
  ├── Recipe View (配方视图)
  │   ├── ingredients, process, BOM
  │   └── ...
  └── Finance View (财务视图)
      ├── gl_account, cost_center
      └── ...
```

### 6.2 Profile 定义

| Profile | 职责 | 核心数据 | 消费者 |
|---------|------|----------|--------|
| **Commercial Profile** | 描述"能否销售" | saleable, price, POS config | POS, 销售订单 |
| **Procurement Profile** | 描述"能否采购" | supplier, purchase_unit, lead_time | 采购订单, 入库单 |
| **Inventory Profile** | 描述"能否存储" | warehouse, safety_stock, max_stock | 库存管理, 盘点 |
| **Recipe Profile** | 描述"能否作为原料" | ingredients, quantity, yield | 配方管理, 成本核算 |
| **Finance Profile** | 描述"财务属性" | gl_account, cost_center | 财务系统, 报表 |

### 6.3 评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 业务表达能力 | ⭐⭐⭐⭐⭐ | 最全面 |
| 业务语义清晰度 | ⭐⭐⭐⭐ | 清晰的分层 |
| 跨域一致性 | ⭐⭐⭐⭐ | 统一身份保证 |
| Identity 一致性 | ⭐⭐⭐⭐⭐ | 核心设计目标 |
| 数据重复风险 | ⭐⭐⭐⭐⭐ | 最小化 |
| 扩展能力 | ⭐⭐⭐⭐⭐ | 最灵活 |
| 门店差异能力 | ⭐⭐⭐⭐⭐ | Operational 定义支持 |
| 采购能力 | ⭐⭐⭐⭐ | Procurement Profile |
| 库存能力 | ⭐⭐⭐⭐ | Inventory Profile |
| POS 能力 | ⭐⭐⭐⭐ | Commercial Profile |
| Recipe 能力 | ⭐⭐⭐⭐ | Recipe Profile |
| Costing 能力 | ⭐⭐⭐⭐⭐ | 跨域计算最优 |
| Finance Integration | ⭐⭐⭐⭐⭐ | Finance Profile 专门支持 |
| 历史数据兼容 | ⭐⭐⭐ | 需要适配器 |
| 迁移复杂度 | ⭐⭐ | 中等复杂 |
| 长期维护成本 | ⭐⭐⭐⭐ | 设计复杂但维护清晰 |

### 6.4 业务案例验证

**案例一：可乐**

```
Canonical Item: id=C001, name="可口可乐330ml", code="SKU_COLA_330ML"
  │
  ├── Commercial Profile
  │     ├── canonical_id=C001
  │     ├── saleable=true
  │     ├── price=3.00
  │     └── POS config: {display_name: "可乐", category: "饮料"}
  │
  ├── Procurement Profile
  │     ├── canonical_id=C001
  │     ├── purchasable=true
  │     ├── supplier_id=S001
  │     ├── purchase_unit="罐"
  │     ├── reference_price=2.50
  │     └── lead_time_days=3
  │
  ├── Inventory Profile
  │     ├── canonical_id=C001
  │     ├── storable=true
  │     ├── warehouse_id=W01
  │     ├── safety_stock=50
  │     ├── max_stock=500
  │     └── shelf_life_days=365
  │
  ├── Recipe Profile (可选)
  │     ├── canonical_id=C001
  │     ├── is_ingredient=true
  │     └── used_in: [可乐鸡翅]
  │
  └── Finance Profile
        ├── canonical_id=C001
        ├── gl_account="1401" (库存商品)
        └── cost_center="CC001"
```

**优点**：
- 每个 Profile 职责明确
- 跨域查询简单
- 支持门店级别差异化

**案例二：宫保鸡丁**

```
Canonical Item: id=C010, name="宫保鸡丁", code="SKU_KPC_001"
  │
  ├── Commercial Profile
  │     ├── canonical_id=C010
  │     ├── saleable=true
  │     ├── price=28.00
  │     └── POS config: {display_name: "宫保鸡丁", category: "热菜"}
  │
  ├── Procurement Profile
  │     ├── canonical_id=C010
  │     ├── purchasable=false (不采购成品)
  │     └── ...
  │
  ├── Inventory Profile
  │     ├── canonical_id=C010
  │     ├── storable=false (不存储成品)
  │     └── ...
  │
  └── Recipe Profile
        ├── canonical_id=C010
        ├── is_ingredient=false (不作为其他菜品原料)
        ├── ingredients: [
        │   {canonical_id: C002 (鸡胸肉), quantity: 0.3kg},
        │   {canonical_id: C003 (花生), quantity: 0.05kg},
        │   {canonical_id: C004 (辣椒), quantity: 0.02kg}
        │ ]
        └── ...
```

**优点**：
- 宫保鸡丁的 Procurement Profile 和 Inventory Profile 为 false
- Recipe Profile 定义了它的原料组成
- 语义清晰

### 6.5 优缺点总结

**优点**：
- 最全面的业务表达能力
- 清晰的 Profile 分层
- 最好的跨域一致性
- 最灵活的扩展能力
- 最好的门店差异支持

**缺点**：
- 设计复杂度高
- 需要维护多个 Profile 表
- 迁移复杂度中等

---

## 7. 综合对比

### 7.1 评分矩阵

| 维度 | Model A | Model B | Model C |
|------|---------|---------|---------|
| 业务表达能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 业务语义清晰度 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 跨域一致性 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Identity 一致性 | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 数据重复风险 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 扩展能力 | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 门店差异能力 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 采购能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 库存能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| POS 能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Recipe 能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Costing 能力 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Finance Integration | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 历史数据兼容 | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| 迁移复杂度 | ⭐⭐⭐ | ⭐ | ⭐⭐ |
| 长期维护成本 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **总分** | **44** | **60** | **70** |

### 7.2 业务场景对比

#### 场景一：可乐的完整生命周期

| 阶段 | Model A | Model B | Model C |
|------|---------|---------|---------|
| 采购入库 | material_id=M001 → inventory | canonical_item_id=C001 + PURCHASABLE Role → STOCKABLE Role | canonical_item_id=C001 + Procurement Profile → Inventory Profile |
| 门店销售 | food_id=F001 → order_items | canonical_item_id=C001 + SELLABLE Role → order_items | canonical_item_id=C001 + Commercial Profile → order_items |
| 库存管理 | inventory.product_id=M001 | canonical_item_id=C001 + STOCKABLE Role | canonical_item_id=C001 + Inventory Profile |
| 配方消耗 | dish_recipe.ingredient_id=M001 | canonical_item_id=C001 + INGREDIENT Role | canonical_item_id=C001 + Recipe Profile |

#### 场景二：宫保鸡丁的配方管理

| 阶段 | Model A | Model B | Model C |
|------|---------|---------|---------|
| 菜品定义 | food_id=F010 | canonical_item_id=C010 + SELLABLE Role | canonical_item_id=C010 + Commercial Profile |
| 配方定义 | dish_recipe.dish_id=F010 | canonical_item_id=C010 + RECIPE Relationship | canonical_item_id=C010 + Recipe Profile |
| 原料采购 | material_id=M002 (鸡翅) | canonical_item_id=C002 + PURCHASABLE Role | canonical_item_id=C002 + Procurement Profile |
| 原料库存 | inventory.material_id=M002 | canonical_item_id=C002 + STOCKABLE Role | canonical_item_id=C002 + Inventory Profile |

#### 场景三：多供应商多包装规格

| 阶段 | Model A | Model B | Model C |
|------|---------|---------|---------|
| 供应商A箱装 | material_id=M001 + supplier_id=S001 | canonical_item_id=C001 + PURCHASABLE Role 1 | canonical_item_id=C001 + Procurement Profile 1 |
| 供应商A半箱装 | material_id=M001 + supplier_id=S001 | canonical_item_id=C001 + PURCHASABLE Role 2 | canonical_item_id=C001 + Procurement Profile 2 |
| 供应商B箱装 | material_id=M001 + supplier_id=S002 | canonical_item_id=C001 + PURCHASABLE Role 3 | canonical_item_id=C001 + Procurement Profile 3 |

---

## 8. 推荐模型

### 8.1 推荐：Model C (Hybrid Domain Model)

**理由**：

1. **业务表达能力最强**：Profile 分层清晰，每个域有独立的配置
2. **扩展性最好**：新增域只需新增 Profile，不修改核心模型
3. **门店差异支持最好**：Operational Definition 支持门店级别配置
4. **跨域一致性最好**：统一身份保证数据一致性
5. **成本核算最优**：跨域计算有统一基础

### 8.2 与当前系统的映射

| 当前表 | 目标映射 | 说明 |
|--------|----------|------|
| `foods` | Canonical Item + Commercial Profile | 食品=统一身份+商业定义 |
| `material_archives` | Canonical Item + Procurement Profile | 物料=统一身份+采购定义 |
| `inventory` | Canonical Item + Inventory Profile | 库存=统一身份+库存定义 |
| `store_inventory` | Canonical Item + Inventory Profile (门店) | 门店库存=统一身份+门店库存定义 |
| `dish_recipe` | Canonical Item + Recipe Profile | 配方=统一身份+配方定义 |
| `purchase_order_items` | Canonical Item + Procurement Profile | 采购=统一身份+采购定义 |
| `order_items` | Canonical Item + Commercial Profile | 销售=统一身份+商业定义 |

### 8.3 实现路径

**Phase 1: 引入 Canonical Item**
- 创建 `canonical_item` 表
- 将 `material_archives` 迁移为 `canonical_item`
- 将 `foods` 迁移为 `canonical_item`

**Phase 2: 引入 Profile**
- 创建 `commercial_profile` 表
- 创建 `procurement_profile` 表
- 创建 `inventory_profile` 表
- 创建 `recipe_profile` 表

**Phase 3: 迁移现有数据**
- 将 `foods` 数据迁移到 `canonical_item` + `commercial_profile`
- 将 `material_archives` 数据迁移到 `canonical_item` + `procurement_profile`
- 将 `inventory` 数据迁移到 `canonical_item` + `inventory_profile`

**Phase 4: 更新业务逻辑**
- 更新所有业务流程，使用 `canonical_item_id`
- 更新所有查询，使用 Profile 聚合

---

## 9. 三维度分离

### 9.1 CURRENT REALITY（当前现实）

| 维度 | 现状 | 问题 |
|------|------|------|
| 身份模型 | Product / Material / Inventory 独立 | 身份碎片化 |
| 数据冗余 | 名称、编码等重复存储 | 数据不一致 |
| 跨域查询 | 多次 JOIN，性能差 | 查询复杂 |
| 扩展能力 | 新增域需要新建表 | 扩展性差 |

### 9.2 TARGET BUSINESS SEMANTICS（目标业务语义）

| 维度 | 目标 | 理由 |
|------|------|------|
| 身份模型 | **Canonical Item + Profile** | 统一身份 + 分层配置 |
| 数据冗余 | **最小化** | 名称、编码只存储一次 |
| 跨域查询 | **简单 JOIN** | 统一身份保证一致性 |
| 扩展能力 | **Profile 扩展** | 新增域只需新增 Profile |

### 9.3 TARGET TECHNICAL MODEL（目标技术模型）

```sql
-- 统一身份
CREATE TABLE canonical_item (
    canonical_item_id   BIGINT PRIMARY KEY,
    item_code           VARCHAR(50) NOT NULL UNIQUE,
    item_name           VARCHAR(200) NOT NULL,
    brand               VARCHAR(100),
    specification       VARCHAR(200),
    unit                VARCHAR(50),
    barcode             VARCHAR(100),
    category_id         BIGINT,
    status              INTEGER DEFAULT 1,
    ...
);

-- 商业定义
CREATE TABLE commercial_profile (
    profile_id          BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,
    saleable            BOOLEAN DEFAULT TRUE,
    selling_price       BIGINT,               -- 销售价（分）
    menu_config         JSONB,                -- 菜单配置
    pos_config          JSONB,                -- POS配置
    ...
);

-- 采购定义
CREATE TABLE procurement_profile (
    profile_id          BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,
    purchasable         BOOLEAN DEFAULT TRUE,
    supplier_id         BIGINT,
    purchase_unit       VARCHAR(50),
    purchase_spec       VARCHAR(200),
    reference_price     BIGINT,               -- 参考价（分）
    min_order_qty       DECIMAL(12,3),
    lead_time_days      INTEGER,
    ...
);

-- 库存定义
CREATE TABLE inventory_profile (
    profile_id          BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,
    storable            BOOLEAN DEFAULT TRUE,
    location_id         BIGINT,
    safety_stock        INTEGER DEFAULT 0,
    max_stock           INTEGER DEFAULT 0,
    shelf_life_days     INTEGER,
    storage_temp        VARCHAR(50),
    ...
);

-- 配方定义
CREATE TABLE recipe_profile (
    profile_id          BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,
    is_ingredient       BOOLEAN DEFAULT FALSE,
    recipe_config       JSONB,                -- 配方配置
    ...
);

-- 库存实例（统一）
CREATE TABLE inventory_instance (
    instance_id         BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,
    location_id         BIGINT NOT NULL,
    current_stock       INTEGER NOT NULL DEFAULT 0,
    locked_quantity     INTEGER DEFAULT 0,
    unit_cost           DECIMAL(12,2) DEFAULT 0,
    total_cost          DECIMAL(14,4) DEFAULT 0,
    batch_no            VARCHAR(50),
    production_date     DATE,
    expiry_date         DATE,
    ...
);
```

---

## 10. 遗留问题与技术债

### 10.1 当前系统的身份碎片化

| 问题 | 位置 | 影响 |
|------|------|------|
| foods 和 material_archives 无外键关联 | 跨表 | 身份碎片化 |
| inventory 使用 product_id 遗留字段 | inventory 表 | 语义混乱 |
- dish_recipe 的 ingredient_id 可能指向 product 也可能指向 material_archives | dish_recipe 表 | 身份不明确 |

### 10.2 数据一致性问题

| 问题 | 位置 | 影响 |
|------|------|------|
| 名称、编码重复存储 | foods, material_archives | 数据不一致 |
| 价格分散维护 | foods.price, material_archives.reference_price, inventory.cost_price | 成本核算困难 |
- 供应商信息分散 | material_archives.supplier_id, purchase_order_items | 供应商管理混乱 |

### 10.3 跨域查询问题

| 问题 | 位置 | 影响 |
|------|------|------|
| 查询"可乐"的完整信息需要多次 JOIN | 跨表 | 性能差 |
| 无法统一查询所有域的数据 | 跨表 | 报表困难 |
- 门店级别差异化难以实现 | 跨表 | 门店管理困难 |

---

## 11. 推荐改进方向

### 11.1 短期（保持现有结构）

1. **建立 item_bridge 表**：统一关联 foods、material_archives
2. **修复 inventory.product_id**：在后续迁移中添加 material_id 列并回填数据
3. **统一 material_id 口径**：确保所有新增表和字段使用 material_id

### 11.2 中期（引入 Profile）

1. **创建 canonical_item 表**：建立统一身份
2. **创建 commercial_profile 表**：迁移 foods 的商业属性
3. **创建 procurement_profile 表**：迁移 material_archives 的采购属性
4. **创建 inventory_profile 表**：迁移 inventory 的库存属性

### 11.3 长期（统一模型）

1. **完全迁移到 Canonical Item + Profile 模型**
2. **删除遗留表**：删除 foods、product、material_archives 等遗留表
3. **优化查询性能**：建立索引，优化跨域查询
4. **建立数据治理流程**：确保身份一致性

---

## 12. 总结

| 问题 | 结论 |
|------|------|
| Product、Material、Inventory 的关系应该是什么？ | **Canonical Item + Profile**，三者是统一身份在不同域的配置 |
| 推荐哪个模型？ | **Model C: Hybrid Domain Model**，业务表达最强，扩展性最好 |
| 如何验证？ | 用可乐、宫保鸡丁、多供应商多包装规格三个案例验证 |
| 迁移路径是什么？ | 短期建立桥接，中期引入 Profile，长期统一模型 |
| 最大的技术债是什么？ | 身份碎片化和数据冗余，需要通过 Canonical Item 解决 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-09
**作者**：AI 架构总控
