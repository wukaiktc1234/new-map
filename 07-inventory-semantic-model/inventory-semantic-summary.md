> **DOCUMENT STATUS:** ACTIVE_REFERENCE
> **SOURCE TASK:** INVENTORY-SEMANTIC-MODEL-001

# Inventory Semantic Model 调和总结

## 任务完成情况

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 1 | Inventory Truth 分析 | ✅ |
| Phase 2 | Stockable Object 分析 | ✅ |
| Phase 3 | Stockable Capability 分析 | ✅ |
| Phase 4 | Inventory Location 模型 | ✅ |
| Phase 5 | 门店 vs 仓储分析 | ✅ |
| Phase 6 | Inventory Quantity 模型 | ✅ |
| Phase 7 | UOM 模型 + Conversion | ✅ |
| Phase 8 | Canonical Stock Quantity | ✅ |
| Phase 9 | 部分消耗分析 | ✅ |
| Phase 10 | Inventory Movement 模型 | ✅ |
| Phase 11 | 销售/消耗库存流 | ✅ |
| Phase 12 | 采购/收货库存流 | ✅ |
| Phase 13 | 调拨库存流 | ✅ |
| Phase 14 | Inventory State 模型 | ✅ |
| Phase 15 | Batch/Lot/Expiry 分析 | ✅ |
| Phase 16 | 拆包分析 | ✅ |
| Phase 17 | Costing 交互 | ✅ |
| Phase 18+20 | Inventory Identity 分析 | ✅ |
| Phase 19 | 跨域库存流 | ✅ |
| Phase 21 | 当前→目标语义映射 | ✅ |
| Phase 22 | 候选模型比较 + 压力点 + 推荐 | ✅ |
| Phase 23 | 反例验证 | ✅ |
| Registry | 概念注册表 | ✅ |
| Summary | 调和总结 | ✅ |
| README | 目录说明 | ✅ |

**25 个文件全部完成。**

---

## 核心发现总结

### 1. Inventory Truth

**结论**：Stock Ledger 是 Truth Source，Balance 是 Derived View。

- Inventory Fact = Material × Location × Quantity
- 聚合库存（如"总库存 = 仓库 + 门店"）是 Aggregated View，不是 Truth Source
- 当前系统存在两个独立 Truth 问题（inventory vs store_inventory）

### 2. Stockable Object

**结论**：Inventory 管理的是 **Material**（原料/物料），不是 Food（成品菜品）。

- 宫保鸡丁、可乐鸡翅等现做菜品不入库，它们的原料才入库
- Stockable Item = Material + Location

### 3. Stockable Capability

**结论**：STOCKABLE 是 Material 的 **Business Role**（角色），不是 Type、Capability、Profile 或 Contextual Property。

- 作用域推荐 **LOCATION-SCOPED**（位置级）
- 可乐在门店A可存储、门店B不可存储的场景是合理的

### 4. Location Model

**结论**：统一 Inventory Fact 模型 + Location 抽象层。

- Store、Warehouse、Kitchen、Transit 都属于 Inventory Location
- Inventory Fact = Material × Location × Quantity
- "总库存160"只是计算结果，不应存储

### 5. Quantity Model

**结论**：推荐 **Base UOM Quantity** 存储库存真相。

- 库存以 Base Quantity 保存，展示时按需转换
- Available / Reserved / On-hand / In-transit 是 Quantity 的不同维度

### 6. UOM Model

**结论**：Unit 必须成为独立 Foundation。

- 五层单位体系：Base → Purchase → Stock → Recipe → Sales
- 当前系统5张表各自存储 unit 字段且无转换关系
- 必须建立 Conversion Table

### 7. Movement Model

**结论**：16+ 种标准 Movement 类型，四个语义层次。

- Business Event → Inventory Movement → State Change → Derived Result
- 当前系统 transaction_type（1-8）需要扩展

### 8. 推荐模型

**Model D: Item + Location + Stock Ledger**（加权得分最高）

- Inventory Fact = Item × Location × Quantity
- Stock Ledger 是 Truth Source
- Balance 是 Derived View
- Movement 是 Ledger entries

---

## 概念分层模型

```
L1: Canonical Identity    "这是什么东西？"
L2: Type / Classification "它属于什么类别？"
L3: Capability            "它能做什么？"
L4: Role                  "它在某场景中做什么？"
L5: Profile               "它在某场景中的配置是什么？"
L6: Location              "它在哪里？"
L7: Quantity              "它有多少？"
L8: UOM                   "用什么单位？"
L9: Movement              "为什么增加/减少？"
L10: Cost                 "成本是多少？"
```

---

## 当前实现 → 目标语义映射

| 当前表 | 目标语义 | 说明 |
|--------|----------|------|
| inventory | Inventory Fact (仓库维度) | 需要合并到统一模型 |
| store_inventory | Inventory Fact (门店维度) | 需要合并到统一模型 |
| inventory_transactions | Stock Ledger (Movement) | 需要扩展类型 |
| material_archives | Stockable Item (Material) | Canonical Identity |
| foods | Sellable Definition | 商业定义 |
| product | Deprecated | 已废弃 |
| dish_recipe | Recipe Relationship | 跨域桥接 |
| order_items | Sale Event | 触发 Movement |
| purchase_order_items | Purchase Event | 触发 Movement |
| inventory_unit | UOM Foundation | 独立基础表 |

---

## 模型压力点

| 压力点 | 类型 | 严重程度 | 根因 |
|--------|------|----------|------|
| food_id ↔ material_id 无映射 | Identity Stress | HIGH | 缺少 Canonical Item |
| inventory 与 store_inventory 结构分裂 | Role Conflict | HIGH | 缺少统一 Location Model |
| 安全库存无法按门店差异化 | Scope Mismatch | MEDIUM | 全局配置 vs 门店差异 |
| 门店库存变动无流水 | Relationship Complexity | HIGH | 缺少门店 Movement |
| 字段名不一致、成本不一致 | Data Duplication | MEDIUM | 缺少统一 UOM |
| transaction_type 语义过载 | Type Ambiguity | MEDIUM | 缺少 Movement Type 体系 |
| 调拨无中间状态 | State Mismatch | MEDIUM | 缺少 Transit Location |

**所有压力点指向同一根因：缺少统一的 Inventory Fact Model。**

---

## 需要 Product Owner 决定的事项

| # | 决策项 | 影响范围 | 优先级 |
|---|--------|----------|--------|
| 1 | 门店是否需要独立的安全库存阈值？ | Inventory 配置 | HIGH |
| 2 | 调拨过程中库存归属权如何定义？ | Inventory State | HIGH |
| 3 | 部分消耗（开瓶/拆包）是否核心需求？ | Inventory Model | MEDIUM |
| 4 | Batch/Lot/Expiry 是否核心需求？ | Inventory Context | MEDIUM |
| 5 | 门店是否可以不销售某些 Material？ | Stockable Scope | HIGH |
| 6 | Recipe 是否需要按门店差异化？ | Recipe Model | HIGH |

---

## 需要 Architecture Owner 决定的事项

| # | 决策项 | 影响范围 | 优先级 |
|---|--------|----------|--------|
| 1 | 统一 inventory 表还是保持双表？ | Data Model | HIGH |
| 2 | Base UOM 存储还是 Display UOM？ | Storage Design | HIGH |
| 3 | UOM Conversion 在应用层还是数据库层？ | Architecture | HIGH |
| 4 | Movement Type 枚举还是动态配置？ | Extensibility | MEDIUM |
| 5 | 门店 Movement 流水如何实现？ | Data Model | HIGH |
| 6 | Transit Location 如何建模？ | Data Model | MEDIUM |

---

## 与 DEC-006 的依赖关系

| Inventory 结论 | 影响 DEC-006 的方面 |
|----------------|---------------------|
| Inventory 管理 Material | Material 是库存对象，不是 Food |
| Stockable 是 Location-scoped Role | 同一 Item 在不同 Location 可以有不同 Stockable 配置 |
| UOM 是独立 Foundation | 需要统一的 UOM 模型 |
| Location 是统一抽象 | Store 和 Warehouse 都是 Location |
| Movement 是 Ledger | 需要统一的 Movement Type 体系 |

---

## 与 BUSINESS-ITEM-MODEL-002 的衔接

| Inventory 结论 | 影响 Business Item Model 的方面 |
|----------------|--------------------------------|
| Canonical Identity = Material | 支持 Model C (Hybrid Domain Model) |
| Stockable 是 Role | 支持 Role-based 模型 |
| Location 是统一抽象 | 支持 Store-scoped Role |
| UOM 是 Foundation | 支持 Unit 作为共享基础 |

---

## 19 项验收标准全部满足

| # | 验收条件 | 状态 |
|---|----------|------|
| 1 | Inventory Truth 已定义 | ✅ |
| 2 | Stockable Object 已分析 | ✅ |
| 3 | Location Model 已分析 | ✅ |
| 4 | Store / Warehouse 已区分 | ✅ |
| 5 | Quantity Truth 已定义 | ✅ |
| 6 | UOM 已定义 | ✅ |
| 7 | UOM Conversion 已分析 | ✅ |
| 8 | Purchase / Receive / Sale / Consumption / Transfer 已形成统一语义 | ✅ |
| 9 | Partial Consumption 已分析 | ✅ |
| 10 | Batch/Lot/Expiry 是否需要已判断 | ✅ |
| 11 | Inventory Cost interaction 已分析 | ✅ |
| 12 | Identity propagation 已分析 | ✅ |
| 13 | 多门店/多仓库已验证 | ✅ |
| 14 | 可乐/可乐鸡翅反例通过验证 | ✅ |
| 15 | 至少 4 个候选模型已比较 | ✅ |
| 16 | 当前实现 → 目标语义映射完成 | ✅ |
| 17 | Model Stress Points 已识别 | ✅ |
| 18 | Recommendation 与 Confirmed Decision 分离 | ✅ |
| 19 | 与 DEC-006 的依赖关系已明确 | ✅ |

---

## 状态

**RECOMMENDATION, NOT CONFIRMED**

等待 Product Owner / Architecture Owner Review

---

**日期**：2026-09-10
**作者**：AI 架构总控
**任务**：INVENTORY-SEMANTIC-MODEL-001
