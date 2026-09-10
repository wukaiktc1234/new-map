# Phase 9 - Sellable 能力分析

> **核心问题**: "可销售"究竟是什么?
> **状态**: ANALYSIS
> **关联文档**: concept-taxonomy.md, sales-pos-model.md, type-vs-role-analysis.md, business-role-model.md, store-scope-analysis.md

---

## 1. 问题域

### 1.1 核心问题

1. Food, Beverage, Packaged Goods, Retail, Service 是否都能成为 Sellable?
2. Price, Menu, POS Visibility, Tax, Store Scope, Promotion, Sales Unit 究竟属于哪里?
3. Sellable 是 Item 的固有能力还是可配置的角色?

### 1.2 分析原则

- **严格区分**: Type, Role, Capability, Relationship — 不得混淆
- **测试真实案例**: Food, Beverage, Packaged Goods, Retail, Service
- **三层分析**: CURRENT REALITY / TARGET BUSINESS SEMANTICS / TARGET TECHNICAL MODEL

---

## 2. 当前现实 (CURRENT REALITY)

### 2.1 销售相关表结构

```
foods (菜品/食品):
├── food_id (PK)
├── food_code, food_name
├── food_price (售价)
├── cost_price (成本价)
├── food_category
├── stock (可售量/库存)
├── status (1在售 2停售 3售罄)
├── shelf_life_days, production_address
├── nutrition_info, storage_conditions
└── price, weight, quality_grade

order_items (订单明细):
├── item_id (PK)
├── order_id (FK)
├── product_type (1单品 2套餐)
├── food_id (单品ID)
├── combo_id (套餐ID)
├── product_name
├── unit_price (单价)
├── quantity (数量)
├── amount (小计金额)
└── kitchen_status

orders (订单主表):
├── order_id (PK)
├── order_type (1堂食 2外卖 3自提 4打包)
├── order_source (1收银台 2小程序 3第三方平台)
├── store_id
├── total_amount, final_amount
└── payment_status
```

### 2.2 关键事实

| 事实 | 证据 | 影响 |
|------|------|------|
| 销售对象是 food_id | `order_items.food_id` | Food 是销售的 Canonical Identity |
| Food 有价格属性 | `foods.food_price`, `foods.cost_price` | 价格是 Food 的属性 |
| Food 有库存属性 | `foods.stock` | 可售量是 Food 的属性 |
| Food 有状态 | `foods.status` (1在售 2停售 3售罄) | 状态控制销售能力 |
| 无门店级价格配置 | 不存在门店级价格表 | 所有门店共享同一价格 |
| 无门店级菜单配置 | 不存在门店级菜单表 | 所有门店共享同一菜单 |
| 无促销配置 | 不存在促销表 | 无促销管理能力 |
| 无服务类菜品 | foods 表结构偏向实体食品 | 服务类菜品未建模 |

### 2.3 当前系统的隐含假设

当前系统将销售属性（food_price, cost_price, stock, status）直接嵌入 foods 表中。这意味着：
- 每个菜品只有一个价格
- 每个菜品在所有门店价格相同
- 没有门店级的菜单/可见性配置
- 没有促销管理能力

---

## 3. Sellable 的本质是什么？

### 3.1 候选模型分析

#### 模型 A: Sellable 作为 TYPE

```
Food ← TYPE(Sellable) → "这个东西是可销售的"
```

**支持证据**:
- foods 表是销售的核心
- 所有菜品都是"可销售的"
- concept-taxonomy.md 将 Sellable 分类为 CAPABILITY + ROLE

**反对证据**:
- 如果 Sellable 是 Type，那么同一菜品在不同门店不能有不同的销售状态
- 一个菜品在门店 A 可以是 Sellable，在门店 B 可以不是（不卖）
- Type 是固有的；但 Sellable 是可配置的

**结论**: ❌ Sellable 不是 Type

#### 模型 B: Sellable 作为 CAPABILITY

```
Food ← CAPABILITY(Sellable) → "我具备被销售的能力"
```

**支持证据**:
- 采购的原料不能直接销售（需加工），自制菜品可以销售
- 能力是全局声明：宫保鸡丁能被销售，大米不能
- concept-taxonomy.md 分类为 CAPABILITY

**反对证据**:
- 纯 Capability 无法承载价格、菜单、可见性等配置数据
- 需要额外的配置层来存储销售参数

**结论**: ⚠️ Sellable 的核心是 CAPABILITY，但需要 Role/Profile 来承载配置数据

#### 模型 C: Sellable 作为 ROLE

```
Food ← ROLE(Sellable) → "在销售场景中扮演什么角色"
```

**支持证据**:
- business-role-model.md 将 SELLABLE 定义为 Business Role
- 同一菜品在不同门店可以有不同的销售配置（价格、菜单位置）
- Role 可以携带场景特定数据

**反对证据**:
- Role 描述"做什么"，但 Sellable 更像"能被销售"
- 如果是 Role，那么需要在每个销售场景中"激活"这个 Role

**结论**: ⚠️ 接近但不精确 — Sellable 首先是 Capability，Role 是其配置载体

#### 模型 D: Sellable 作为 PROFILE

```
Food ← PROFILE(SalesProfile) → "销售配置信息"
```

**支持证据**:
- Price, Menu, POS Visibility, Promotion 等确实是 Profile 数据
- 这些数据描述了"怎么销售"，不是"是什么"

**反对证据**:
- Profile 描述"是什么样子"，不声明"能做什么"
- 需要先声明 Sellable Capability，然后才有 Sales Profile

**结论**: ✅ Sales Profile 是 Sellable 能力的配置数据层

### 3.2 综合结论

**Sellable = CAPABILITY（能力声明）+ ROLE（场景角色）+ PROFILE（Sales Profile）**

```
Sellable 能力模型:
┌─────────────────────────────────────────────────────────┐
│                    Food (Canonical Item)                  │
│                                                         │
│  capability声明:                                         │
│    sellable: boolean  ← "我具备被销售的能力"              │
│                                                         │
│  sales profile (销售配置):                                │
│    ├── Global Scope:                                    │
│    │   food_price: 基础价格                              │
│    │   food_category: 分类                              │
│    │   status: 在售/停售                                 │
│    │                                                    │
│    ├── Store Scope:                                     │
│    │   sell_price: 门店级价格                            │
│    │   is_visible: POS 可见性                            │
│    │   menu_position: 菜单位置                          │
│    │   promotion: 促销配置                              │
│    │   sales_unit: 销售单位                             │
│    │                                                    │
│    └── Tax Config:                                      │
│        tax_rate: 税率                                   │
│        tax_included: 是否含税                           │
│                                                         │
│  实例数据 (Sales Instance):                              │
│    ├── order_items.food_id                              │
│    ├── order_items.unit_price                           │
│    └── order_items.quantity                             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 跨品类 Sellable 测试

### 4.1 测试矩阵

| 品类 | 示例 | Sellable? | 说明 |
|------|------|-----------|------|
| **Food (食品)** | 宫保鸡丁 | ✅ 是 | 自制菜品，可直接销售 |
| **Beverage (饮料)** | 可口可乐 | ✅ 是 | 采购饮料，可直接销售 |
| **Packaged Goods (预包装)** | 薯片 | ✅ 是 | 采购零食，可直接销售 |
| **Retail (零售)** | 餐盒 | ❌ 否 | 耗材，不直接销售给顾客 |
| **Service (服务)** | 配送服务 | ✅ 是 | 无形服务，可销售 |

### 4.2 详细分析

#### Food (食品) — 宫保鸡丁

```
Capability: sellable = true
Profile:
  food_price = 3800(分)
  food_category = '热菜'
  status = 1(在售)
  tax_rate = 6%(餐饮服务)
  sales_unit = '份'

Instance:
  order_items: food_id=F001, unit_price=3800, quantity=2
```

**结论**: Food 是典型的 Sellable。

#### Beverage (饮料) — 可口可乐

```
Capability: sellable = true
Profile:
  food_price = 300(分)
  food_category = '饮料'
  status = 1(在售)
  tax_rate = 13%(商品销售)
  sales_unit = '瓶'

Instance:
  order_items: food_id=F002, unit_price=300, quantity=1
```

**结论**: Beverage 是典型的 Sellable。

#### Packaged Goods (预包装) — 薯片

```
Capability: sellable = true
Profile:
  food_price = 800(分)
  food_category = '零食'
  status = 1(在售)
  tax_rate = 13%(商品销售)
  sales_unit = '袋'

Instance:
  order_items: food_id=F003, unit_price=800, quantity=1
```

**结论**: Packaged Goods 是典型的 Sellable。

#### Retail (零售/耗材) — 餐盒

```
Capability: sellable = false
Profile:
  (无销售配置)

Instance:
  (无销售记录)
```

**结论**: 餐盒不是 Sellable — 它是耗材，不直接销售给顾客。

#### Service (服务) — 配送服务

```
Capability: sellable = true
Profile:
  food_price = 500(分)  ← 配送费
  food_category = '服务'
  status = 1(在售)
  tax_rate = 6%(服务费)
  sales_unit = '次'

Instance:
  order_items: food_id=F010, unit_price=500, quantity=1
```

**结论**: Service 可以成为 Sellable。需要扩展 foods 表以支持服务类菜品。

### 4.3 测试结论

| 品类 | Sellable? | 需要的配置 |
|------|-----------|-----------|
| Food | ✅ 是 | 价格、分类、状态、税率、单位 |
| Beverage | ✅ 是 | 价格、分类、状态、税率、单位 |
| Packaged Goods | ✅ 是 | 价格、分类、状态、税率、单位 |
| Retail (耗材) | ❌ 否 | 无需销售配置 |
| Service | ✅ 是 | 价格、分类、状态、税率、单位 |

---

## 5. 销售属性的分层分析

### 5.1 属性归属矩阵

| 属性 | 当前位置 | 应归属层次 | 说明 |
|------|----------|-----------|------|
| **Price (基础价格)** | foods.food_price | PROFILE (Global) | 全局基础价格 |
| **Price (门店价格)** | 不存在 | PROFILE (Store) | 门店级价格覆盖 |
| **Menu (菜单位置)** | 不存在 | PROFILE (Store) | 门店级菜单配置 |
| **POS Visibility** | foods.status | PROFILE (Store) | 门店级可见性 |
| **Tax (税率)** | 不存在 | PROFILE (Global/Store) | 税率配置 |
| **Store Scope** | 不存在 | CAPABILITY Config | 门店级销售能力 |
| **Promotion** | 不存在 | PROFILE (Store/Time) | 促销配置 |
| **Sales Unit** | 不存在 | PROFILE (Global/Store) | 销售单位 |

### 5.2 关键区分: 什么属于 Capability，什么属于 Profile?

**Capability（能力声明）**= "我能不能被销售"
- 全局声明
- 布尔值（能/不能）
- 变更频率低
- 示例: 宫保鸡丁 sellable=true

**Profile（销售配置）**= "怎么销售"
- 可以按 Global / Store / Time 配置
- 包含多个参数（价格、菜单、可见性、促销等）
- 变更频率中
- 示例: 宫保鸡丁在门店A: price=3800, visible=true

### 5.3 与 IDENTITY 的区别

| 属于 IDENTITY | 属于 Profile |
|---------------|-------------|
| food_code (菜品编码) | food_price (价格) |
| food_name (菜品名称) | food_category (分类) |
| 基础描述信息 | status (在售/停售) |
| | tax_rate (税率) |
| | sales_unit (销售单位) |

---

## 6. 门店级销售配置分析

### 6.1 问题: 同一菜品在不同门店是否可以有不同的销售配置?

**场景**: 宫保鸡丁在门店 A 卖 38 元，在门店 B 卖 42 元（因为房租不同）。

**当前系统的处理**:
- `foods.food_price` 只能存储一个价格
- 所有门店共享同一价格
- 无法支持"千店千价"

### 6.2 目标模型: Store-scoped Sales Profile

```
foods (Canonical Item):
  food_id=F001, food_name="宫保鸡丁"

food_capability:
  food_id=F001, capability_type='SELLABLE', enabled=true

food_sales_profile:
  ├── global_profile:
  │   food_price = 3800(分)
  │   food_category = '热菜'
  │   tax_rate = 6%
  │   sales_unit = '份'
  │
  ├── store_profile (门店A):
  │   sell_price = 3800(分)  ← 使用全局价格
  │   is_visible = true
  │   menu_position = 1
  │
  └── store_profile (门店B):
      sell_price = 4200(分)  ← 门店级价格覆盖
      is_visible = true
      menu_position = 3
```

### 6.3 查询模式

```sql
-- 查询门店 A 的菜单（含价格）
SELECT f.*, 
       COASP(ssp.sell_price, f.food_price) as effective_price
FROM foods f
JOIN food_capability fc ON f.food_id = fc.food_id
LEFT JOIN food_sales_profile ssp ON f.food_id = ssp.food_id 
  AND ssp.scope_type = 'STORE' 
  AND ssp.scope_id = 'STORE_A'
WHERE fc.capability_type = 'SELLABLE'
  AND fc.enabled = TRUE
  AND f.status = 1
ORDER BY ssp.menu_position, f.food_category;
```

---

## 7. 促销配置分析

### 7.1 促销场景

| 促销类型 | 说明 | 示例 |
|----------|------|------|
| **折扣** | 直接降价 | 宫保鸡丁 8 折 |
| **满减** | 满额减 | 满 100 减 20 |
| **买赠** | 买一送一 | 买可乐送薯片 |
| **限时特价** | 时间限制 | 午餐特价 11:00-13:00 |
| **会员价** | 会员专属 | 会员 9 折 |

### 7.2 促销配置归属

| 配置项 | 归属层次 | 说明 |
|--------|----------|------|
| 促销规则 | Profile (Time-scoped) | 有时间范围的配置 |
| 促销价格 | Profile (Derived) | 从基础价格 + 折扣计算 |
| 促销可见性 | Profile (Store-scoped) | 门店级促销配置 |

### 7.3 促销模型

```sql
-- 促销规则
CREATE TABLE promotion_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    promotion_name VARCHAR(100),
    promotion_type VARCHAR(50),          -- DISCOUNT, FULL_REDUCTION, BUY_GET, TIME_LIMITED
    scope_type VARCHAR(20),              -- GLOBAL, STORE
    scope_id VARCHAR(50),                -- store_id
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    discount_rate DECIMAL(5,2),          -- 折扣率
    reduction_amount BIGINT,             -- 减免金额(分)
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 促销商品关联
CREATE TABLE promotion_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    promotion_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    promotion_price BIGINT,              -- 促销价(分)
    buy_quantity INT,                    -- 买N
    get_quantity INT,                    -- 送N
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(promotion_id, food_id)
);
```

---

## 8. 真实案例验证

### 案例 A: 宫保鸡丁的完整 Sellable 生命周期

```
1. 创建菜品
   foods: 宫保鸡丁, food_id=F001
   capability: sellable = true

2. 配置 Global Sales Profile
   food_sales_profile: scope_type=GLOBAL
   food_price = 3800(分), tax_rate = 6%, sales_unit = '份'

3. 配置 Store Sales Profile
   food_sales_profile: scope_type=STORE, scope_id=STORE_A
   sell_price = 3800(分), is_visible = true, menu_position = 1

   food_sales_profile: scope_type=STORE, scope_id=STORE_B
   sell_price = 4200(分), is_visible = true, menu_position = 3

4. 配置促销
   promotion_rule: 午餐特价 11:00-13:00, discount=0.8
   promotion_item: food_id=F001, promotion_price = 3040(分)

5. 顾客点餐
   order_items: food_id=F001, unit_price=3040(促销价), quantity=2
   orders: total_amount = 6080(分)
```

### 案例 B: 可口可乐的多场景 Sellable

```
1. 创建菜品
   foods: 可口可乐, food_id=F002
   capability: sellable = true

2. 配置 Global Sales Profile
   food_price = 300(分), tax_rate = 13%, sales_unit = '瓶'

3. 配置 Store Sales Profile
   Store A (堂食): sell_price = 300(分), is_visible = true
   Store B (外卖): sell_price = 500(分), is_visible = true  ← 外卖加价

4. 顾客点餐
   堂食: order_items: food_id=F002, unit_price=300, quantity=1
   外卖: order_items: food_id=F002, unit_price=500, quantity=1
```

### 案例 C: 配送服务的 Sellable

```
1. 创建服务类菜品
   foods: 配送服务, food_id=F010, food_category='服务'
   capability: sellable = true

2. 配置 Global Sales Profile
   food_price = 500(分), tax_rate = 6%, sales_unit = '次'

3. 顾客下单
   order_items: food_id=F010, unit_price=500, quantity=1
```

### 案例 D: 餐盒的非 Sellable 验证

```
1. 创建物料
   material_archives: 一次性餐盒, material_id=M010
   capability: sellable = false

2. 验证
   foods 表中无餐盒记录
   order_items 表中无餐盒销售记录
   餐盒只在 inventory / store_inventory 中管理
```

---

## 9. 目标技术模型 (TARGET TECHNICAL MODEL)

### 9.1 Sellable 能力模型

```sql
-- 销售能力声明
CREATE TABLE food_capability (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    food_id BIGINT NOT NULL,
    capability_type VARCHAR(50) NOT NULL,  -- SELLABLE
    enabled BOOLEAN DEFAULT TRUE,
    config JSON,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(food_id, capability_type)
);

-- 销售配置 (Sales Profile)
CREATE TABLE food_sales_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    food_id BIGINT NOT NULL,
    scope_type VARCHAR(20) NOT NULL,       -- GLOBAL, STORE
    scope_id VARCHAR(50),                  -- store_id (STORE scope)
    sell_price BIGINT,                     -- 销售价格(分)
    food_category VARCHAR(50),             -- 菜品分类
    tax_rate DECIMAL(5,2),                -- 税率(%)
    sales_unit VARCHAR(20),               -- 销售单位: 份/瓶/袋
    is_visible BOOLEAN DEFAULT TRUE,       -- POS 可见性
    menu_position INT,                     -- 菜单位置
    description TEXT,                      -- 菜品描述
    image_url VARCHAR(255),               -- 菜品图片
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(food_id, scope_type, scope_id)
);
```

### 9.2 与现有表的关系

```
foods (Canonical Item)
    │
    ├── food_capability (能力声明)
    │   food_id + capability_type = 'SELLABLE'
    │   enabled = true/false
    │
    ├── food_sales_profile (销售配置)
    │   food_id + scope_type + scope_id
    │   sell_price, tax_rate, sales_unit, is_visible
    │
    ├── order_items (销售实例)
    │   food_id, unit_price, quantity
    │   从 food_sales_profile 获取有效价格
    │
    └── promotion_rule / promotion_item (促销配置)
        food_id + promotion_id
        promotion_price, time_range
```

### 9.3 查询模式

```sql
-- 查询门店 A 的有效菜单价格
SELECT f.food_id, f.food_name,
       COALESCE(ssp.sell_price, gsp.sell_price) as effective_price,
       COALESCE(ssp.tax_rate, gsp.tax_rate) as effective_tax_rate,
       COALESCE(ssp.sales_unit, gsp.sales_unit) as effective_sales_unit
FROM foods f
JOIN food_capability fc ON f.food_id = fc.food_id
JOIN food_sales_profile gsp ON f.food_id = gsp.food_id 
  AND gsp.scope_type = 'GLOBAL'
LEFT JOIN food_sales_profile ssp ON f.food_id = ssp.food_id 
  AND ssp.scope_type = 'STORE' 
  AND ssp.scope_id = 'STORE_A'
WHERE fc.capability_type = 'SELLABLE'
  AND fc.enabled = TRUE
  AND f.status = 1
  AND COALESCE(ssp.is_visible, TRUE) = TRUE;

-- 查询门店 A 的有效促销价格
SELECT f.food_id, f.food_name,
       COALESCE(pi.promotion_price, COALESCE(ssp.sell_price, gsp.sell_price)) as final_price
FROM foods f
JOIN food_capability fc ON f.food_id = fc.food_id
JOIN food_sales_profile gsp ON f.food_id = gsp.food_id AND gsp.scope_type = 'GLOBAL'
LEFT JOIN food_sales_profile ssp ON f.food_id = ssp.food_id 
  AND ssp.scope_type = 'STORE' AND ssp.scope_id = 'STORE_A'
LEFT JOIN promotion_item pi ON f.food_id = pi.food_id
LEFT JOIN promotion_rule pr ON pi.promotion_id = pr.id
  AND pr.scope_type IN ('GLOBAL', 'STORE')
  AND pr.scope_id IN (NULL, 'STORE_A')
  AND NOW() BETWEEN pr.start_time AND pr.end_time
WHERE fc.capability_type = 'SELLABLE'
  AND fc.enabled = TRUE
  AND f.status = 1;
```

---

## 10. 与现有决策的关系

### 10.1 与 sales-pos-model.md 的关系

sales-pos-model.md 已经确认:
- POS 销售的统一对象是 Food
- Food 与 Material 通过 Recipe 形成关系，不是继承
- 术语: Food（销售）、Material（库存）、Recipe（配方）

本分析进一步细化:
- Sellable = Capability + Profile
- Profile 包含 price, tax, unit, visibility 等配置

### 10.2 与 type-vs-role-analysis.md 的关系

type-vs-role-analysis.md 确认:
- Type 和 Role 应该分离
- Food 是 Role 组合，不是独立 Type

本分析确认:
- Sellable 是 Food 的 Role 之一（销售场景的 Role）
- 但更精确地说，Sellable 是 Capability，Profile 是其配置载体

### 10.3 与 store-scope-analysis.md 的关系

store-scope-analysis.md 提出了 item_store_config 表:
- 门店级销售配置: is_sellable
- 门店级销售价格: sell_price

本分析与之一致:
- food_capability 中的 sellable = 全局能力声明
- item_store_config 中的 is_sellable = 门店级能力覆盖
- food_sales_profile = 销售配置详情（价格、菜单、可见性等）

---

## 11. 结论

### 11.1 回答核心问题

| 问题 | 结论 |
|------|------|
| "可销售"究竟是什么? | **Capability（能力声明）+ Role（场景角色）+ Profile（Sales Profile）** |
| Food 是否都能成为 Sellable? | **是** — Food 是典型的 Sellable |
| Beverage 是否都能成为 Sellable? | **是** — Beverage 是典型的 Sellable |
| Packaged Goods 是否都能成为 Sellable? | **是** — Packaged Goods 是典型的 Sellable |
| Retail (耗材) 是否能成为 Sellable? | **否** — 餐盒等耗材不直接销售给顾客 |
| Service 是否能成为 Sellable? | **是** — 服务类菜品可以销售 |
| Price 属于哪里? | **PROFILE** — 价格是销售配置 |
| Menu 属于哪里? | **PROFILE** — 菜单是销售配置 |
| POS Visibility 属于哪里? | **PROFILE** — 可见性是销售配置 |
| Tax 属于哪里? | **PROFILE** — 税率是销售配置 |
| Store Scope 属于哪里? | **CAPABILITY Config** — 门店级销售能力 |
| Promotion 属于哪里? | **PROFILE (Time-scoped)** — 有时间范围的配置 |
| Sales Unit 属于哪里? | **PROFILE** — 销售单位是销售配置 |

### 11.2 分层总结

| 层次 | 概念 | 变更频率 | 示例 |
|------|------|----------|------|
| **Capability** | sellable=true/false | 极低（菜品创建时确定） | 宫保鸡丁能被销售 |
| **Profile (Global)** | food_price, tax_rate | 中（价格调整时） | 宫保鸡丁全局价 38 元 |
| **Profile (Store)** | sell_price, is_visible | 中（门店配置时） | 门店A: 38元, 门店B: 42元 |
| **Profile (Time)** | promotion | 高（促销活动时） | 午餐特价: 11:00-13:00 |
| **Instance** | order_items | 高（每次点餐变动） | 本次购买 2 份, 76 元 |

### 11.3 为什么不能把 Sellable 混为其他概念

1. **不是 Type**: Type 是固有的（宫保鸡丁永远是 Food），Sellable 是可配置的（可以按门店启用/禁用）
2. **不是纯 Role**: Role 描述"做什么"，Sellable 首先声明"能做什么"
3. **不是 State**: State 是临时的（售罄不意味着不可销售），Sellable 是持久的能力
4. **不是 Identity**: 价格、菜单、可见性是可变的配置，不是身份标识

---

**分析完成时间**: 2026-09-09
**分析结论**: Sellable = CAPABILITY（全局能力声明）+ ROLE（场景角色）+ PROFILE（Sales Profile 配置）
**推荐行动**: 新增 food_capability 表和 food_sales_profile 表，实现门店级销售配置和促销管理
