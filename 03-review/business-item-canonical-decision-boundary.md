# Business Item Canonical Decision Boundary

## 一、Decision Boundary 概述

本文档将餐饮 ERP 系统中 Business Item 相关的决策重新划分为 **Product Decisions**、**Architecture Decisions** 和 **Data / Engineering Decisions** 三个独立边界。每个决策拥有明确的状态标识：`REASSESSED` / `PARTIAL` / `OPEN`。

核心原则：
- 不得继续将所有决策全部塞入 DEC-006
- 必须明确区分 Product / Architecture / Engineering Decisions
- 保持决策之间的依赖关系可见

---

## 二、Product Decisions（产品决策）

### PD-CANONICAL-001: Canonical Business Item 定义

- **问题：** Business Item 是否应该拥有独立 Canonical Identity？
- **选项分析：**
  - Option A: 每个 Business Item 类型（Food / Material / Ingredient）独立定义 Identity，无统一 Canonical 层
  - Option B: 引入统一 Canonical Business Item Identity 层，所有子类型共享 UUID + canonical_name + domain_tags
- **推荐：** Option B — 统一 Canonical Identity 层
- **阻塞影响：** 阻塞 PD-CANONICAL-002 ~ PD-CANONICAL-006 的所有后续 Product 语义决策
- **状态：** `REASSESSED`

---

### PD-CANONICAL-002: Product 语义

- **问题：** Product 是 Umbrella Concept / Commercial Definition / Legacy Concept？
- **选项分析：**
  - Option A: Product 作为 Umbrella Concept — 包含所有可售实体，不区分 Food / Non-food
  - Option B: Product 作为 Commercial Definition — 仅包含具有商业定价属性的可售项
  - Option C: Product 作为 Legacy Concept — 仅保留历史兼容，不新增语义
- **推荐：** Option B — Product 定义为 Commercial Definition（含 SKU、unit_price、tax_category、status）
- **阻塞影响：** 阻塞 DE-SKU-001（SKU 生成规则）、阻塞 Inventory Object 设计
- **状态：** `OPEN`

---

### PD-CANONICAL-003: Food 语义

- **问题：** Food 是 Entity / Type / Commercial Profile？
- **选项分析：**
  - Option A: Food 作为独立 Entity — 拥有完整 Identity + 属性集（recipe、shelf_life、storage_req）
  - Option B: Food 作为 Type Tag — 仅标记 Business Item 的食品属性
  - Option C: Food 作为 Commercial Profile — 扩展 Product 的商业属性（含 recipe、costing）
- **推荐：** Option A — Food 作为独立 Entity
- **阻塞影响：** 阻塞 PD-CANONICAL-006（Consumable 定义）、阻塞 Recipe 建模
- **状态：** `OPEN`

---

### PD-CANONICAL-004: Material 语义

- **问题：** Material 是 Identity / Type / Procurement Profile？
- **选项分析：**
  - Option A: Material 作为 Identity — 独立 Identity + 采购属性（supplier、lead_time、MOQ）
  - Option B: Material 作为 Type Tag — 仅标记 Business Item 的物料属性
  - Option C: Material 作为 Procurement Profile — 扩展 Business Item 的采购维度
- **推荐：** Option A — Material 作为独立 Identity（含采购属性、供应商关联）
- **阻塞影响：** 阻塞 PD-CANONICAL-006（Consumable 定义）、阻塞 Procurement 模块设计
- **状态：** `OPEN`

---

### PD-CANONICAL-005: Ingredient 定义

- **问题：** Ingredient 是 Item Role / Recipe Relationship？
- **选项分析：**
  - Option A: Ingredient 作为 Item Role — Business Item 在 Recipe 上下文中的角色标识
  - Option B: Ingredient 作为 Recipe Relationship — 独立实体，关联 Business Item 与 Recipe
- **推荐：** Option B — Ingredient 作为 Recipe Relationship（含 quantity、uom、sequence、substitution_group）
- **阻塞影响：** 阻塞 PD-CANONICAL-006（Consumable 定义）
- **状态：** `OPEN`

---

### PD-CANONICAL-006: Consumable 定义

- **问题：** Consumable 是 Recipe Role / Supply Role / Operational Role？
- **选项分析：**
  - Option A: Consumable 作为 Recipe Role — 仅在 Recipe 层面定义消耗
  - Option B: Consumable 作为 Supply Role — 关联 Supply Chain 的消耗属性
  - Option C: Consumable 作为 Operational Role — 统一 Recipe + Supply + Operational 消耗维度
- **推荐：** Option C — Consumable 作为 Operational Role
- **阻塞影响：** 阻塞 Inventory Truth 设计（AD-INVENTORY-001）
- **状态：** `OPEN`

---

## 三、Architecture Decisions（架构决策）

### AD-IDENTITY-001: Identity Storage

- **问题：** Canonical Identity 如何存储？
- **选项分析：**
  - Option A: 每个子类型独立 Identity 表（food_identity、material_identity 等）
  - Option B: 统一 business_item 表 + 子类型扩展表
  - Option C: 统一 business_item 表 + JSONB 扩展字段
- **推荐：** Option B — 统一 business_item 表 + 子类型扩展表（兼顾查询性能与扩展性）
- **阻塞影响：** 阻塞 DE-PK-001（Primary Key 设计）
- **状态：** `OPEN`

---

### AD-LOCATION-001: Location Model

- **问题：** Store / Warehouse / Kitchen / Transit 是否统一 Location？
- **选项分析：**
  - Option A: 统一 Location 表 + location_type 区分（Store / Warehouse / Kitchen / Transit）
  - Option B: 分离独立实体表（Store、Warehouse、Kitchen 各自独立）
  - Option C: 统一 Location 表 + 多态关联（polymorphic association）
- **推荐：** Option A — 统一 Location 表 + location_type 枚举
- **阻塞影响：** 阻塞 Inventory Object 设计（AD-INVENTORY-002）
- **状态：** `REASSESSED`

---

### AD-UOM-001: UOM Foundation

- **问题：** UOM 是否成为独立 Foundation？
- **选项分析：**
  - Option A: UOM 作为独立 Foundation — 独立 unit_of_measure 表 + conversion_factor
  - Option B: UOM 作为 Business Item 属性 — 内嵌于 Business Item 表
  - Option C: UOM 作为全局枚举 — 预定义枚举 + 自定义扩展
- **推荐：** Option A — UOM 作为独立 Foundation
- **阻塞影响：** 阻塞 Inventory Quantity 计算、阻塞 Costing 模块
- **状态：** `REASSESSED`

---

### AD-INVENTORY-001: Inventory Truth

- **问题：** Ledger-First 还是 Balance-First？
- **选项分析：**
  - Option A: Ledger-First — 所有库存变动通过 Ledger 事件驱动，Balance 为衍生视图
  - Option B: Balance-First — Balance 为主要存储，Ledger 为审计日志
  - Option C: 混合模式 — Balance 为热数据 + Ledger 为冷数据归档
- **推荐：** Option A — Ledger-First（保证数据一致性与可审计性）
- **阻塞影响：** 阻塞 DE-STORAGE-001（存储方案）
- **状态：** `OPEN`

---

### AD-INVENTORY-002: Inventory Object

- **问题：** Inventory 管理 Business Item / Stockable Profile / Item × Location？
- **选项分析：**
  - Option A: Inventory 管理 Business Item — 以 Business Item 粒度管理库存
  - Option B: Inventory 管理 Stockable Profile — 以 Stockable Profile 粒度管理库存
  - Option C: Inventory 管理 Item × Location — 以 Business Item × Location 粒度管理库存
- **推荐：** Option C — Item × Location 粒度（支持多仓库、多门店库存管理）
- **阻塞影响：** 阻塞 Ledger 表设计、阻塞 DE-STORAGE-001
- **状态：** `OPEN`

---

## 四、Data / Engineering Decisions（数据/工程决策）

### DE-PK-001: Primary Key 设计

- **问题：** Primary Key 采用 UUID / Auto-Increment / Composite？
- **选项分析：**
  - Option A: UUID v7 — 全局唯一 + 时间有序
  - Option B: Auto-Increment BigInt — 自增整型
  - Option C: Composite Key — 多字段组合
- **推荐：** Option A — UUID v7
- **阻塞影响：** 影响所有表结构设计
- **状态：** `REASSESSED`

---

### DE-SKU-001: SKU 生成规则

- **问题：** SKU 自动生成规则如何定义？
- **选项分析：**
  - Option A: 前缀 + 序列号（如 FOOD-000001）
  - Option B: 分类编码 + 序列号（如 FD-A-001）
  - Option C: 全局序列号（无分类前缀）
- **推荐：** Option A — 前缀 + 序列号
- **阻塞影响：** 阻塞 Product 表 SKU 字段设计
- **状态：** `OPEN`

---

### DE-MIGRATION-001: 数据迁移策略

- **问题：** 现有数据迁移采用何种策略？
- **选项分析：**
  - Option A: 全量重建 — 清空后重新导入
  - Option B: 增量迁移 — 按时间段/业务线增量迁移
  - Option C: 双写过渡 — 新旧系统并行写入
- **推荐：** Option B — 增量迁移
- **阻塞影响：** 阻塞上线时间线
- **状态：** `OPEN`

---

### DE-STORAGE-001: JSON / Table / Materialized View

- **问题：** Inventory Ledger 数据采用何种存储方案？
- **选项分析：**
  - Option A: 纯关系表存储 — 所有字段独立列
  - Option B: JSON 扩展字段 — 核心字段独立列 + 扩展字段 JSONB
  - Option C: Materialized View — 预计算聚合视图
- **推荐：** Option A — 纯关系表存储（兼顾查询性能与数据完整性）
- **阻塞影响：** 影响数据库性能与扩展性
- **状态：** `OPEN`

---

## 五、Decision Dependency Graph

```
PD-CANONICAL-001 (Canonical Identity)
  ├── PD-CANONICAL-002 (Product 语义)
  │     ├── DE-SKU-001 (SKU 生成规则)
  │     └── AD-INVENTORY-002 (Inventory Object)
  ├── PD-CANONICAL-003 (Food 语义)
  │     └── PD-CANONICAL-006 (Consumable 定义)
  ├── PD-CANONICAL-004 (Material 语义)
  │     └── PD-CANONICAL-006 (Consumable 定义)
  └── PD-CANONICAL-005 (Ingredient 定义)
        └── PD-CANONICAL-006 (Consumable 定义)
              └── AD-INVENTORY-001 (Inventory Truth)
                    └── DE-STORAGE-001 (存储方案)

AD-IDENTITY-001 (Identity Storage)
  └── DE-PK-001 (Primary Key 设计)

AD-LOCATION-001 (Location Model)
  └── AD-INVENTORY-002 (Inventory Object)

AD-UOM-001 (UOM Foundation)
  └── AD-INVENTORY-001 (Inventory Truth)

DE-MIGRATION-001 (数据迁移策略)
  └── (依赖所有 Architecture Decisions 完成)
```

---

## 六、Decision Timeline

| 阶段 | 决策 ID | 决策名称 | 状态 | 预计完成 |
|------|---------|----------|------|----------|
| Phase 1 | PD-CANONICAL-001 | Canonical Identity 定义 | REASSESSED | Week 1 |
| Phase 1 | AD-IDENTITY-001 | Identity Storage | OPEN | Week 1 |
| Phase 1 | DE-PK-001 | Primary Key 设计 | REASSESSED | Week 1 |
| Phase 1 | AD-UOM-001 | UOM Foundation | REASSESSED | Week 1 |
| Phase 1 | AD-LOCATION-001 | Location Model | REASSESSED | Week 1 |
| Phase 2 | PD-CANONICAL-002 | Product 语义 | OPEN | Week 2 |
| Phase 2 | PD-CANONICAL-003 | Food 语义 | OPEN | Week 2 |
| Phase 2 | PD-CANONICAL-004 | Material 语义 | OPEN | Week 2 |
| Phase 2 | PD-CANONICAL-005 | Ingredient 定义 | OPEN | Week 2 |
| Phase 3 | PD-CANONICAL-006 | Consumable 定义 | OPEN | Week 3 |
| Phase 3 | AD-INVENTORY-001 | Inventory Truth | OPEN | Week 3 |
| Phase 3 | AD-INVENTORY-002 | Inventory Object | OPEN | Week 3 |
| Phase 4 | DE-SKU-001 | SKU 生成规则 | OPEN | Week 4 |
| Phase 4 | DE-STORAGE-001 | 存储方案 | OPEN | Week 4 |
| Phase 5 | DE-MIGRATION-001 | 数据迁移策略 | OPEN | Week 5 |

---

## 七、与现有 DEC-006 的关系

- **Existing DEC-006 / DEC-006-REFINED** = Historical / Existing Recommendation
  - 原 DEC-006 将 Business Item 的所有产品语义决策统一打包处理
  - 已识别该方式导致决策边界模糊、依赖关系不清晰

- **Reassessment** = Current Decision Input
  - 本文档（business-item-canonical-decision-boundary.md）为重新划分后的决策边界
  - 所有决策已拆分为独立的 PD / AD / DE 三个维度
  - 每个决策拥有独立的状态标识，便于追踪与依赖管理

**关键变更：**
1. DEC-006 中的 Business Item Identity 决策 → 迁移至 PD-CANONICAL-001
2. DEC-006 中的 Product/Food/Material 语义 → 迁移至 PD-CANONICAL-002 ~ PD-CANONICAL-005
3. DEC-006 中的 Architecture 决策 → 迁移至 AD-* 系列
4. DEC-006 中的 Engineering 决策 → 迁移至 DE-* 系列
5. 新增 Consumable 定义决策（PD-CANONICAL-006）
6. 新增 Inventory Truth 与 Inventory Object 决策（AD-INVENTORY-001 ~ 002）

**状态总览：**
- `REASSESSED`: PD-CANONICAL-001, AD-IDENTITY-001, AD-LOCATION-001, AD-UOM-001, DE-PK-001
- `PARTIAL`: （暂无）
- `OPEN`: PD-CANONICAL-002 ~ 006, AD-INVENTORY-001 ~ 002, DE-SKU-001, DE-MIGRATION-001, DE-STORAGE-001
