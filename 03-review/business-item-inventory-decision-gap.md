# Business Item × Inventory Decision Gap

> **版本**: 1.0
> **生成日期**: 2026-09-10
> **状态**: OPEN
> **任务**: BUSINESS-ITEM-INVENTORY-INTEGRATION-001

---

## 一、Decision Gap 概述

本文件登记所有需要 Product / Architecture Decision 的问题。这些问题无法通过分析收敛，必须由决策者正式确认。

---

## 二、需要 Product Owner 决策

### DG-001: Product 是否保留

| 字段 | 值 |
|------|-----|
| **Gap ID** | DG-001 |
| **来源** | Conflict A, DEC-006 |
| **优先级** | P0 |
| **截止日期** | 2026-10-15 |
| **阻塞影响** | 阻塞 Canonical Item 定义 |

**问题**：Product 是否作为 Commercial Definition 保留，还是彻底废弃？

**选项**：
- **Option A**：废弃 Product，保留 Food + Material（DEC-006 推荐）
- **Option B**：保留 Product 作为 Commercial Definition 层
- **Option C**：引入 Canonical Item 替代 Product

**推荐**：Option A — 废弃 Product，保留 Food + Material

**理由**：
1. 当前 product 表无活跃业务角色
2. Food 和 Material 已覆盖所有业务场景
3. 迁移成本最低

**需要的信息**：
- 未来是否有集团化多品牌需求？
- 是否需要 Commercial Definition 层支撑非餐饮业务？

---

### DG-002: Material 是否作为 Canonical Identity

| 字段 | 值 |
|------|-----|
| **Gap ID** | DG-002 |
| **来源** | Conflict C |
| **优先级** | P0 |
| **截止日期** | 2026-10-15 |
| **阻塞影响** | 阻塞 Inventory 模型设计 |

**问题**：Material 是否作为 Canonical Identity 保留，还是引入 Canonical Item 替代？

**选项**：
- **Option A**：Material 作为 Canonical Identity（保持现状）
- **Option B**：引入 Canonical Item 作为更高层抽象
- **Option C**：Material 作为 Type，Canonical Item 作为 Identity

**推荐**：Option A — Material 作为 Canonical Identity

**理由**：
1. LOCK-A002 已锁定 material_archives 为物料真相源
2. material_id 贯穿采购→库存→配方全生命周期
3. 引入 Canonical Item 迁移成本过高

**需要的信息**：
- 是否需要支持非 Material 的库存对象（如 Service、Digital）？
- 是否需要跨品牌统一 Identity？

---

### DG-003: Inventory 管理什么对象

| 字段 | 值 |
|------|-----|
| **Gap ID** | DG-003 |
| **来源** | 07-inventory-semantic-model |
| **优先级** | P0 |
| **截止日期** | 2026-10-15 |
| **阻塞影响** | 阻塞 Inventory 数据模型 |

**问题**：Inventory 管理的是 Material、Product、Stock Item、Canonical Item 还是 Stockable Profile？

**选项**：
- **Option A**：Inventory → Material（当前实现）
- **Option B**：Inventory → Canonical Item
- **Option C**：Inventory → Stockable Profile
- **Option D**：Inventory → Item × Location（推荐）

**推荐**：Option D — Inventory → Item × Location

**理由**：
1. 当前 inventory 表以 material_id + warehouse_id 为核心
2. store_inventory 以 store_id + material_id 为核心
3. 统一为 Item × Location × Quantity 最符合业务语义

**需要的信息**：
- 门店库存是否需要独立管理？
- 调拨过程中库存归属权如何定义？

---

### DG-004: Store Scope 如何影响 Role

| 字段 | 值 |
|------|-----|
| **Gap ID** | DG-004 |
| **来源** | 06-business-item-model/store-scope-analysis.md |
| **优先级** | P1 |
| **截止日期** | 2026-10-15 |
| **阻塞影响** | 阻塞门店级配置设计 |

**问题**：同一业务对象在不同门店是否可以拥有不同 Role？

**场景**：
- Store A：可乐 = Sellable + Stockable
- Store B：可乐 = Stockable only（不销售）
- Central Warehouse：可乐 = Purchasable + Stockable

**选项**：
- **Option A**：Role 是 Global（所有门店相同）
- **Option B**：Role 是 Store-scoped（按门店配置）
- **Option C**：Role 是 Location-scoped（按位置配置）

**推荐**：Option C — Role 是 Location-scoped

**理由**：
1. 不同门店可以有不同的经营范围
2. 仓库和门店的库存管理需求不同
3. Location-scoped 最灵活

**需要的信息**：
- 门店经营范围是否需要审批流程？
- 是否需要门店级价格差异化？

---

## 三、需要 Architecture Owner 决策

### DG-005: UOM 是否成为独立 Foundation

| 字段 | 值 |
|------|-----|
| **Gap ID** | DG-005 |
| **来源** | 07-inventory-semantic-model/inventory-uom-model.md |
| **优先级** | P0 |
| **截止日期** | 2026-10-01 |
| **阻塞影响** | 阻塞 Inventory 数据模型 |

**问题**：Unit/UOM 是否必须成为独立 Foundation？

**当前现实**：5张表各自存储 unit 字段且无转换关系

**推荐**：是 — Unit 必须成为独立 Foundation

**理由**：
1. 采购"箱"→库存"瓶"→配方"ml"需要自动转换
2. inventory_unit 表已存在但未被引用
3. 统一 UOM 支持跨域数据一致性

---

### DG-006: Location 是否统一抽象

| 字段 | 值 |
|------|-----|
| **Gap ID** | DG-006 |
| **来源** | 07-inventory-semantic-model/inventory-location-model.md |
| **优先级** | P0 |
| **截止日期** | 2026-10-01 |
| **阻塞影响** | 阻塞 Inventory 数据模型 |

**问题**：Store / Warehouse / Kitchen / Transit 是否属于统一 Location 模型？

**当前现实**：inventory（仓库）和 store_inventory（门店）是独立表

**推荐**：是 — 统一 Location 抽象

**理由**：
1. 调拨操作需要统一的 Location 概念
2. Transit 应成为临时 Location
3. "总库存"只是 Aggregated View

---

### DG-007: Ledger-First 还是 Balance-First

| 字段 | 值 |
|------|-----|
| **Gap ID** | DG-007 |
| **来源** | 07-inventory-semantic-model/inventory-truth-analysis.md |
| **优先级** | P0 |
| **截止日期** | 2026-10-01 |
| **阻塞影响** | 阻塞 Inventory 架构 |

**问题**：Inventory Truth 是基于 Stock Ledger 还是 Balance？

**推荐**：Ledger-First

**理由**：
1. Stock Ledger 是不可变的，支持审计
2. Balance 可从 Ledger 重建
3. 当前 inventory.current_stock 是 Derived View

---

## 四、Decision Gap 依赖关系

```
DG-001 (Product)
    ↓
DG-002 (Material as Canonical Identity)
    ↓
DG-003 (Inventory Object)
    ↓
DG-004 (Store Scope)
    ↓
DG-005 (UOM Foundation)
DG-006 (Location Model)
DG-007 (Ledger-First)
```

---

## 五、决策时间线建议

| 阶段 | 时间 | 决策项 | 负责人 |
|------|------|--------|--------|
| Phase 1 | 2026-09-30 | DG-001, DG-002 | Product Owner |
| Phase 2 | 2026-10-01 | DG-005, DG-006, DG-007 | Architecture Owner |
| Phase 3 | 2026-10-15 | DG-003, DG-004 | Product Owner |

---

## 六、成功标准

所有 Decision Gap 解决后，必须能够清楚回答：

1. Business Item 是什么？
2. Product 是什么？
3. Food 是什么？
4. Material 是什么？
5. Inventory 管理什么？
6. Inventory Fact 是什么？
7. Location 是什么？
8. Quantity 是什么？
9. UOM 是什么？
10. Ledger 是什么？

---

**文档状态**: OPEN
**下一步**: 等待 Product Owner / Architecture Owner 正式确认
