> **DOCUMENT STATUS:** SUPERSEDED
> **SUPERSEDED BY:** 03-review/business-item-canonical-semantic-reassessment.md
> **SOURCE TASK:** INVENTORY-SEMANTIC-MODEL-001

# Inventory Semantic Recommendation (Phase 22 Final)

> **核心问题**：基于所有库存域分析，推荐的 Inventory Semantic Model 是什么？
> **状态**：RECOMMENDATION, NOT CONFIRMED
> **约束**：必须区分 RECOMMENDATION 和 CONFIRMED DECISION；必须使用真实业务案例验证；必须证明为什么正确、在哪里不正确
> **依赖**：Phase 21 Model Stress Points + Phase 22 Extension, DEC-006-REFINED, BUSINESS-ITEM-MODEL-002

---

## 一、核心推荐

### 1.1 推荐模型：Canonical Item + Unified Location + Ledger-First Inventory

**推荐模型编号**：Model C-Hybrid（加权得分 4.65/5.0，来源：candidate-semantic-models.md）

**核心结构**：

```
Layer 1: Canonical Identity (身份层)
  └── canonical_item
        "这是什么东西？" → 可口可乐330ml

Layer 2: Business Role (角色层)
  └── business_role
        "它能做什么？" → SELLABLE, PURCHASABLE, STOCKABLE, INGREDIENT

Layer 3: Inventory Profile (配置层)
  └── stockable_profile
        "它如何被库存管理？" → 安全库存、保质期、存储条件

Layer 4: Inventory Instance (实例层)
  └── inventory_instance
        "它现在在哪里？有多少？" → 仓库A有200罐

Layer 5: Inventory Ledger (事件层)
  └── inventory_ledger
        "库存发生了什么变动？" → 采购入库+100罐
```

### 1.2 推荐的数据模型

```sql
-- 1. 统一身份（Canonical Identity）
CREATE TABLE canonical_item (
    id                  BIGINT PRIMARY KEY,
    item_code           VARCHAR(50) NOT NULL UNIQUE,
    item_name           VARCHAR(200) NOT NULL,
    brand               VARCHAR(100),
    category_id         BIGINT,
    base_uom_code       VARCHAR(20) NOT NULL,  -- 基础单位（克/个）
    status              VARCHAR(20) DEFAULT 'ACTIVE',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 身份映射表（解决 Identity Stress）
CREATE TABLE item_identity_map (
    canonical_item_id   BIGINT PRIMARY KEY,
    material_id         BIGINT,                  -- 当前系统的 material_archives.material_id
    food_id             BIGINT,                  -- 销售域的 foods.food_id
    product_id          BIGINT,                  -- 遗留的 product.product_id
    FOREIGN KEY (canonical_item_id) REFERENCES canonical_item(id),
    FOREIGN KEY (material_id) REFERENCES material_archives(material_id),
    FOREIGN KEY (food_id) REFERENCES foods(food_id),
    UNIQUE (material_id),
    UNIQUE (food_id)
);

-- 3. 统一位置模型（解决 Role Conflict + Scope Mismatch）
CREATE TABLE location (
    id                  BIGINT PRIMARY KEY,
    location_code       VARCHAR(50) NOT NULL UNIQUE,
    location_name       VARCHAR(100) NOT NULL,
    location_type       VARCHAR(20) NOT NULL,    -- 'WAREHOUSE' | 'STORE'
    parent_id           BIGINT,                  -- 可选：层级关系
    status              VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 4. 库存角色配置（解决 Scope Mismatch）
CREATE TABLE stockable_profile (
    id                  BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,
    location_id         BIGINT,                  -- 可选：NULL=全局默认，非NULL=门店覆盖
    safety_stock        DECIMAL(12,2) DEFAULT 0,
    max_stock           DECIMAL(12,2) DEFAULT 0,
    reorder_point       DECIMAL(12,2) DEFAULT 0,
    shelf_life_days     INT,
    storage_condition   VARCHAR(50),
    cost_method         VARCHAR(20) DEFAULT 'WEIGHTED_AVG',  -- 成本核算方法
    status              VARCHAR(20) DEFAULT 'ACTIVE',
    FOREIGN KEY (canonical_item_id) REFERENCES canonical_item(id),
    FOREIGN KEY (location_id) REFERENCES location(id),
    UNIQUE (canonical_item_id, location_id)
);

-- 5. 统一库存实例（解决 Role Conflict + Data Duplication）
CREATE TABLE inventory_instance (
    id                  BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,
    location_id         BIGINT NOT NULL,
    batch_no            VARCHAR(50),
    production_date     DATE,
    expiry_date         DATE,
    current_stock       DECIMAL(12,2) NOT NULL DEFAULT 0,
    locked_quantity     DECIMAL(12,2) DEFAULT 0,
    in_transit_quantity DECIMAL(12,2) DEFAULT 0,  -- 在途数量
    unit_cost           DECIMAL(12,4) DEFAULT 0,
    total_cost          DECIMAL(14,4) DEFAULT 0,
    version             INT DEFAULT 1,
    status              VARCHAR(20) DEFAULT 'ACTIVE',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (canonical_item_id) REFERENCES canonical_item(id),
    FOREIGN KEY (location_id) REFERENCES location(id),
    UNIQUE (canonical_item_id, location_id, batch_no)
);

-- 6. 统一库存流水（解决 Relationship Complexity + Type Ambiguity）
CREATE TABLE inventory_ledger (
    id                  BIGINT PRIMARY KEY,
    ledger_no           VARCHAR(50) NOT NULL UNIQUE,
    canonical_item_id   BIGINT NOT NULL,
    source_location_id  BIGINT,                  -- 来源位置（出库时）
    target_location_id  BIGINT NOT NULL,          -- 目标位置（入库时）
    location_type       VARCHAR(20) NOT NULL,     -- 'WAREHOUSE' | 'STORE'
    transaction_type    VARCHAR(30) NOT NULL,     -- 'PURCHASE_IN', 'SALE_OUT', 'TRANSFER_OUT', 'TRANSFER_IN', 'ADJUSTMENT', 'WASTE'
    quantity_change     DECIMAL(12,2) NOT NULL,   -- 正数=入库，负数=出库
    unit_cost           DECIMAL(12,4),
    total_cost          DECIMAL(14,4),
    before_qty          DECIMAL(12,2),            -- 变动前数量
    after_qty           DECIMAL(12,2),            -- 变动后数量
    reference_type      VARCHAR(50),              -- 关联单据类型
    reference_id        BIGINT,                   -- 关联单据ID
    batch_no            VARCHAR(50),
    operator_id         BIGINT,
    remark              VARCHAR(500),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (canonical_item_id) REFERENCES canonical_item(id),
    FOREIGN KEY (source_location_id) REFERENCES location(id),
    FOREIGN KEY (target_location_id) REFERENCES location(id)
);
```

---

## 二、为什么推荐

### 2.1 解决 7 个压力点

| 压力点 | 当前问题 | 推荐模型如何解决 |
|--------|---------|-----------------|
| **Identity Stress** | food_id ↔ material_id 无映射 | `item_identity_map` 显式映射 |
| **Role Conflict** | inventory 与 store_inventory 结构不同 | `inventory_instance` 统一表结构 |
| **Scope Mismatch** | 安全库存无法按门店差异化 | `stockable_profile` 支持 location_id 覆盖 |
| **Relationship Complexity** | 门店库存变动无流水 | `inventory_ledger` 统一流水，含 location_type |
| **Data Duplication** | 字段名不一致，成本不一致 | 统一字段命名，统一成本模型 |
| **Type Ambiguity** | transaction_type 语义过载 | `transaction_type` 使用明确的业务语义枚举 |
| **在途缺失** | 调拨无中间状态 | `in_transit_quantity` 字段 + TRANSFER 状态 |

### 2.2 真实业务案例验证

#### 案例 1：可乐的完整生命周期

```
Canonical Item: 可口可乐330ml (C001)

身份映射：
  item_identity_map: canonical_item_id=C001, material_id=M001, food_id=F001

角色激活：
  business_role: C001 + SELLABLE (foods)
  business_role: C001 + PURCHASABLE (采购)
  business_role: C001 + STOCKABLE (库存)
  business_role: C001 + INGREDIENT (可乐鸡翅)

库存实例：
  inventory_instance: C001 + location=W01, current_stock=200, batch=B001
  inventory_instance: C001 + location=S01, current_stock=20, batch=B002

库存流水：
  inventory_ledger: C001 + target=W01, type=PURCHASE_IN, qty=+200
  inventory_ledger: C001 + source=W01 + target=S01, type=TRANSFER_OUT, qty=-100
  inventory_ledger: C001 + target=S01, type=TRANSFER_IN, qty=+100
  inventory_ledger: C001 + source=S01, type=SALE_OUT, qty=-1

验证：
  ✅ 从 food_id=F001 可以通过 item_identity_map 找到 canonical_item_id=C001
  ✅ 从 C001 可以查到所有位置的库存
  ✅ 所有库存变动都有完整的流水记录
  ✅ 调拨操作有 in_transit 状态
```

#### 案例 2：门店调拨流程

```
场景：仓库W01向门店S01调拨100罐可乐

步骤 1：创建调拨单
  transfer_order: source=W01, target=S01, material=C001, qty=100

步骤 2：仓库出库
  inventory_ledger: canonical_item_id=C001, source=W01, type=TRANSFER_OUT, qty=-100
  inventory_instance: C001 + W01, current_stock: 200→100
  inventory_instance: C001 + W01, in_transit_quantity: 0→100

步骤 3：在途
  inventory_instance: C001 + W01, in_transit_quantity=100
  → 库存仍在仓库名下，但标记为"在途"

步骤 4：门店入库
  inventory_ledger: canonical_item_id=C001, target=S01, type=TRANSFER_IN, qty=+100
  inventory_instance: C001 + W01, in_transit_quantity: 100→0
  inventory_instance: C001 + S01, current_stock: 20→120

验证：
  ✅ 调拨过程有 in_transit 状态
  ✅ 调拨两端都有流水记录
  ✅ 成本从仓库传递到门店
  ✅ 事务一致性：出库和入库在同一事务中
```

#### 案例 3：门店安全库存差异化

```
场景：可乐在不同门店有不同的安全库存

stockable_profile:
  C001 + location=NULL:    safety_stock=50  (集团默认)
  C001 + location=S01:     safety_stock=80  (门店A覆盖)
  C001 + location=S02:     safety_stock=30  (门店B覆盖)

查询逻辑：
  1. 先查 location=S01 的覆盖配置
  2. 如果没有，回退到 location=NULL 的默认配置

验证：
  ✅ 支持门店级别差异化配置
  ✅ 有继承机制：未覆盖的门店使用默认值
  ✅ 修改集团标准时，只修改 location=NULL 的记录
```

#### 案例 4：可乐鸡翅的成本追溯

```
场景：POS售出1份可乐鸡翅，需要追溯原料成本

步骤 1：POS销售
  order_items: food_id=F009 (可乐鸡翅), price=32.00

步骤 2：配方消耗
  dish_recipe: dish_id=F009, ingredient_id=M001 (可乐), quantity=0.2L
  dish_recipe: dish_id=F009, ingredient_id=M002 (鸡翅), quantity=0.5kg

步骤 3：库存扣减
  inventory_ledger: canonical_item_id=C001 (可乐), source=S01, type=RECIPE_CONSUMPTION, qty=-0.2L
  inventory_ledger: canonical_item_id=C002 (鸡翅), source=S01, type=RECIPE_CONSUMPTION, qty=-0.5kg

步骤 4：成本追溯
  可乐鸡翅成本 = 可乐消耗成本 + 鸡翅消耗成本
  可乐消耗成本 = 0.2L × (C001 在 S01 的 unit_cost)
  鸡翅消耗成本 = 0.5kg × (C002 在 S01 的 unit_cost)

验证：
  ✅ 从 food_id=F009 可以通过 item_identity_map 找到 canonical_item_id
  ✅ 从 canonical_item_id 可以追溯到所有原料的库存变动
  ✅ 成本追溯链完整
```

---

## 三、权衡分析

### 3.1 推荐模型 vs 其他方案

| 维度 | Model A (独立实体) | Model B (Role) | **Model C (推荐)** |
|------|-------------------|----------------|-------------------|
| 业务表达能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 业务语义清晰度 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 跨域一致性 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Identity 一致性 | ⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 数据重复风险 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 扩展能力 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 门店差异能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Costing 能力 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Finance Integration | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **加权得分** | **2.8** | **4.0** | **4.65** |

### 3.2 选择理由

1. **Business Correctness (40%)**：Model C 能最准确地表达餐饮业务中的所有场景
2. **Long-term Model Integrity (30%)**：Model C 有清晰的分层架构，长期可维护性最好
3. **Cross-Domain Consistency (20%)**：Model C 通过 item_identity_map 保证跨域一致性
4. **Migration Cost (10%)**：虽然迁移成本较高，但权重最低，长期业务正确性更重要

### 3.3 已知不适用场景

| 场景 | 为什么不适用 | 替代方案 |
|------|-------------|---------|
| 极简单的进销存系统 | Model C 过于复杂 | Model A |
| 单门店系统 | 不需要位置模型和 Scope 覆盖 | Model B |
| 非餐饮零售 | 需要 Retail Product 角色扩展 | Model C + 扩展角色 |

---

## 四、已知风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 数据迁移复杂度高 | 业务中断 | 高 | 分阶段迁移，保留回滚能力 |
| 性能下降 | 用户体验 | 中 | 添加索引，优化查询，分区表 |
| 业务逻辑复杂度增加 | 开发成本 | 中 | 渐进式实现，避免过度设计 |
| 身份映射错误 | 数据不一致 | 低 | 完整的数据验证和备份策略 |
| 成本模型变更 | 财务影响 | 中 | 保持成本计算逻辑不变，只统一数据结构 |
| 门店流水数据量大 | 存储压力 | 中 | 分区表，定期归档 |

---

## 五、开放问题

### 5.1 尚未确定的业务规则

| # | 问题 | 影响范围 | 需要谁决定 |
|---|------|---------|-----------|
| 1 | 成本核算方法：加权平均法 vs 先进先出法 vs 移动加权平均 | 成本模型 | Product Owner |
| 2 | 调拨成本传递规则：按仓库成本还是按市场价？ | 成本模型 | Product Owner |
| 3 | 门店销售扣减库存的时机：下单时扣减还是出品时扣减？ | 库存准确性 | Product Owner |
| 4 | 在途库存是否计入可用库存？ | 业务规则 | Product Owner |
| 5 | 负库存是否允许？ | 库存规则 | Product Owner |
| 6 | 批次管理的粒度：按采购批次还是按保质期？ | 库存管理 | Product Owner |

### 5.2 尚未确定的技术决策

| # | 问题 | 影响范围 | 需要谁决定 |
|---|------|---------|-----------|
| 1 | inventory_ledger 的分区策略：按时间还是按 location？ | 性能 | Architecture Owner |
| 2 | item_identity_map 是否需要异步同步？ | 架构 | Architecture Owner |
| 3 | 库存流水是否需要事件驱动架构（EDA）？ | 架构 | Architecture Owner |
| 4 | inventory_instance 的乐观锁策略：version 字段还是 CAS？ | 并发 | Architecture Owner |

---

## 六、需要 Product Owner 决定的事项

### 6.1 业务规则决策

| # | 决策项 | 选项 | 推荐 | 截止日期 |
|---|--------|------|------|---------|
| 1 | **成本核算方法** | A: 加权平均法 / B: 先进先出法 / C: 移动加权平均 | A（加权平均法） | 2026-10-15 |
| 2 | **调拨成本传递** | A: 按仓库成本 / B: 按市场价 / C: 按协议价 | A（按仓库成本） | 2026-10-15 |
| 3 | **库存扣减时机** | A: 下单时扣减 / B: 出品时扣减 / C: 完成时扣减 | B（出品时扣减） | 2026-10-15 |
| 4 | **在途库存语义** | A: 计入可用库存 / B: 不计入可用库存 / C: 可配置 | B（不计入） | 2026-10-15 |
| 5 | **负库存控制** | A: 允许 / B: 不允许 / C: 可配置 | B（不允许） | 2026-10-15 |
| 6 | **批次管理粒度** | A: 按采购批次 / B: 按保质期 / C: 不管理批次 | A（按采购批次） | 2026-10-15 |

### 6.2 业务范围决策

| # | 决策项 | 选项 | 推荐 | 截止日期 |
|---|--------|------|------|---------|
| 7 | **是否支持门店差异化配方** | A: 支持 / B: 不支持 | B（不支持，保持集团标准） | 2026-10-15 |
| 8 | **是否支持替代材料** | A: 支持 / B: 不支持 | B（不支持，简化模型） | 2026-10-15 |
| 9 | **是否需要库存预警** | A: 需要 / B: 不需要 | A（需要） | 2026-10-15 |

---

## 七、需要 Architecture Owner 决定的事项

### 7.1 技术架构决策

| # | 决策项 | 选项 | 推荐 | 截止日期 |
|---|--------|------|------|---------|
| 1 | **Canonical Item 存储位置** | A: 新建 canonical_item 表 / B: 复用 material_archives | A（新建） | 2026-10-15 |
| 2 | **item_identity_map 同步机制** | A: 同步写入 / B: 异步事件 / C: 定时同步 | A（同步写入） | 2026-10-15 |
| 3 | **inventory_ledger 分区策略** | A: 按时间分区 / B: 按 location 分区 / C: 不分区 | A（按时间分区） | 2026-10-15 |
| 4 | **inventory_instance 并发控制** | A: 乐观锁(version) / B: 悲观锁 / C: CAS | A（乐观锁） | 2026-10-15 |
| 5 | **事件驱动架构（EDA）** | A: 采用 EDA / B: 不采用 / C: 渐进式引入 | C（渐进式引入） | 2026-10-15 |
| 6 | **数据迁移策略** | A: 一次性迁移 / B: 双写过渡 / C: 渐进式迁移 | B（双写过渡） | 2026-10-15 |

### 7.2 性能与扩展性决策

| # | 决策项 | 选项 | 推荐 | 截止日期 |
|---|--------|------|------|---------|
| 7 | **inventory_ledger 归档策略** | A: 按月归档 / B: 按季归档 / C: 不归档 | A（按月归档） | 2026-10-15 |
| 8 | **库存查询缓存策略** | A: Redis 缓存 / B: 本地缓存 / C: 不缓存 | A（Redis 缓存） | 2026-10-15 |
| 9 | **门店流水数据量预估** | 需要评估 | - | 2026-10-15 |

---

## 八、与 DEC-006 的依赖关系

### 8.1 DEC-006-REFINED 概述

DEC-006-REFINED 是关于 **"Canonical Business Item / Product / Food / Material / Stock / Recipe 的目标业务语义边界"** 的决策。

**DEC-006-REFINED 的核心决策**：
1. **Product 废弃**，保留 Food + Material
2. **Inventory 管理 Material**，不管理 Food
3. **Recipe 消耗 Material**，不消耗 Food
4. **引入 Canonical Item** 作为统一身份

### 8.2 本推荐与 DEC-006 的依赖

| DEC-006 决策 | 本推荐的影响 | 依赖关系 |
|-------------|-------------|---------|
| 废弃 Product，保留 Food + Material | 本推荐使用 Canonical Item + item_identity_map 替代 Product | 强依赖 |
| Inventory 管理 Material | 本推荐使用 Canonical Item 替代 Material 作为库存粒度 | 强依赖 |
| Recipe 消耗 Material | 本推荐使用 Canonical Item 作为 Recipe 的 ingredient 引用 | 强依赖 |
| 引入 Canonical Item | 本推荐的 canonical_item 表是 DEC-006 的直接实现 | 强依赖 |

### 8.3 依赖风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| DEC-006 未确认 | 本推荐无法最终确认 | 设置 Deadline 2026-10-15 |
| DEC-006 方案变更 | 本推荐需要调整 | 保持推荐模型的灵活性 |
| DEC-006 实施延迟 | 本推荐实施受阻 | 渐进式实施，先建立映射表 |

---

## 九、与 BUSINESS-ITEM-MODEL-002 的衔接

### 9.1 BUSINESS-ITEM-MODEL-002 概述

BUSINESS-ITEM-MODEL-002 是 **"Business Item Model Reconciliation"** 任务，解决：
> "一个东西是什么"与"这个东西在某个业务上下文中做什么"之间的边界。

**BUSINESS-ITEM-MODEL-002 的核心结论**：
1. **概念分层模型**：L1 Canonical Identity → L2 Type → L3 Capability → L4 Role → L5 Profile → L6 Relationship
2. **Identity Resolution Rule**：Canonical Identity = Brand + ProductFamily + Specification
3. **候选模型结论**：推荐 Model C Hybrid Domain Model（加权得分 4.65/5.0）

### 9.2 本推荐与 BUSINESS-ITEM-MODEL-002 的衔接

| BUSINESS-ITEM-MODEL-002 结论 | 本推荐的实现 | 衔接方式 |
|-----------------------------|-------------|---------|
| L1 Canonical Identity | `canonical_item` 表 | 直接实现 |
| L3 Capability | `business_role` 表 | 直接实现 |
| L4 Role | `stockable_profile` 表 | 直接实现 |
| L5 Profile | `inventory_instance` 表 | 直接实现 |
| L6 Relationship | `item_identity_map` 表 | 直接实现 |
| Model C Hybrid | 本推荐就是 Model C 的库存域实现 | 直接衔接 |

### 9.3 衔接验证

```
BUSINESS-ITEM-MODEL-002 的概念分层 → 本推荐的数据模型映射：

L1: Canonical Identity "这是什么东西？"
    → canonical_item(id, item_code, item_name)

L2: Type / Classification "它属于什么类别？"
    → canonical_item.category_id → category 表

L3: Capability "它能做什么？"
    → business_role(canonical_item_id, role_type)
    → role_type: SELLABLE, PURCHASABLE, STOCKABLE, INGREDIENT

L4: Role "它在某场景中做什么？"
    → stockable_profile(canonical_item_id, location_id)
    → 安全库存、存储条件等配置

L5: Profile "它在某场景中的配置是什么？"
    → inventory_instance(canonical_item_id, location_id)
    → 当前库存数量、成本等运行时状态

L6: Relationship "它与其他东西是什么关系？"
    → item_identity_map(canonical_item_id, material_id, food_id)
    → 跨域身份映射
```

---

## 十、实施路径

### 10.1 分阶段实施计划

```
Phase 1: 建立 Canonical Item 基础 (4周)
├── 创建 canonical_item 表
├── 创建 item_identity_map 表
├── 迁移 material_archives → canonical_item
├── 回填 item_identity_map（预包装品的 food_id ↔ material_id）
├── 验证 POS 销售 → 库存扣减链路
└── 里程碑：身份映射可用

Phase 2: 统一位置模型 (3周)
├── 创建 location 表
├── 迁移 warehouse + store → location
├── 创建 stockable_profile 表
├── 迁移库存配置 → stockable_profile
└── 里程碑：位置模型可用

Phase 3: 统一库存实例 (3周)
├── 创建 inventory_instance 表
├── 迁移 inventory + store_inventory → inventory_instance
├── 创建 inventory_ledger 表
├── 迁移 inventory_transactions → inventory_ledger
├── 补充门店维度流水
└── 里程碑：统一库存模型可用

Phase 4: 验证与优化 (2周)
├── 全链路验证（采购→库存→销售→配方→成本）
├── 性能优化（索引、缓存、分区）
├── 数据一致性检查
└── 里程碑：生产就绪
```

### 10.2 里程碑与验收标准

| 里程碑 | 验收标准 | 截止日期 |
|--------|---------|---------|
| M1: 身份映射可用 | POS 销售可以通过 item_identity_map 找到 canonical_item_id | Phase 1 完成后 |
| M2: 位置模型可用 | 所有库存查询使用统一的 location 表 | Phase 2 完成后 |
| M3: 统一库存模型可用 | inventory + store_inventory 数据迁移到 inventory_instance | Phase 3 完成后 |
| M4: 生产就绪 | 全链路验证通过，性能达标 | Phase 4 完成后 |

---

## 十一、总结

### 11.1 核心推荐

**推荐 Model C-Hybrid**：Canonical Item + Unified Location + Ledger-First Inventory

**核心价值**：
1. 消除 Identity Stress（food_id ↔ material_id 映射）
2. 消除 Role Conflict（统一库存表结构）
3. 消除 Scope Mismatch（支持门店差异化配置）
4. 消除 Relationship Complexity（统一库存流水）
5. 消除 Data Duplication（统一字段命名）
6. 消除 Type Ambiguity（明确 transaction_type 语义）

### 11.2 关键原则重申

| # | 原则 | 说明 |
|---|------|------|
| 1 | **不得为了迁就现有数据库表结构定义最终业务模型** | 当前 inventory + store_inventory 是历史产物，不是目标架构 |
| 2 | **必须严格区分 RECOMMENDATION 和 CONFIRMED DECISION** | 本文件是 RECOMMENDATION，等待 Product Owner 和 Architecture Owner 确认 |
| 3 | **必须使用真实业务案例验证** | 本文件使用可乐、可乐鸡翅、门店调拨等真实案例验证 |
| 4 | **必须证明为什么正确、在哪里不正确** | 本文件在每个压力点和推荐中都提供了验证和局限性分析 |
| 5 | **长期业务正确性 > 当前最小迁移成本** | 推荐模型优先考虑业务正确性，迁移成本通过渐进式路径控制 |

### 11.3 下一步行动

| # | 任务 | 负责人 | 截止日期 |
|---|------|--------|---------|
| 1 | 确认 DEC-006-REFINED | Product Owner + Architecture Owner | 2026-10-15 |
| 2 | 确认本推荐模型 | Product Owner + Architecture Owner | 2026-10-15 |
| 3 | 开始 Phase 1 实施 | 开发团队 | DEC-006 确认后 |

---

**状态**：RECOMMENDATION, NOT CONFIRMED
**日期**：2026-09-10
**阶段**：Phase 22 Final - Inventory Semantic Recommendation
**依赖**：Phase 21 + Phase 22 Extension, DEC-006-REFINED, BUSINESS-ITEM-MODEL-002
**等待**：Product Owner / Architecture Owner Review
