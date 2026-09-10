# Canonical Business Item Semantic Reassessment

## 一、任务概述

- **任务目标**：重新评估 Business Item / Product / Food / Material / Inventory 语义
- **状态**：REASSESSED / PARTIAL / OPEN
- **范围**：餐饮ERP系统中所有涉及业务实体定义的领域——POS、Procurement、Inventory、Recipe、Reporting
- **原则**：当前现实 ≠ 目标模型；Recommendation ≠ Decision；Current Implementation Identity ≠ Target Canonical Identity

---

## 二、Business Invariants（业务不变量）

### Invariant 1: 多角色共存

**同一个业务对象可以同时：Sell + Purchase + Stock + Consume + Become Recipe Component**

| Capability | 说明 |
|---|---|
| Sellable | 可以在 POS / Sales Channel 被销售 |
| Purchasable | 可以在 Procurement 模块被采购 |
| Stockable | 可以在 Inventory 中以库存数量追踪 |
| Consumable | 可以在 Recipe / Kitchen 消耗 |
| Recipe Component | 可以作为某道菜的原料/配料 |

**验证案例：Coca-Cola 330ml**

| 检查项 | 值 |
|---|---|
| Sellable | ✅ 可以在 POS 直接销售 |
| Purchasable | ✅ 可以从供应商采购 |
| Stockable | ✅ 存入仓库、有库存数量 |
| Consumable | ✅ 可被某菜谱使用（如套餐组合） |
| Recipe Component | ✅ 部分菜品以可乐为配方成分 |

**结论**：任一 Business Item 都可能同时拥有上述所有 Capability。Canonical Identity 必须独立于任何单一 Capability。

---

### Invariant 2: Identity 稳定性

**Business Identity 不应因为使用场景变化而变化。**

**验证：Coca-Cola 在各模块中必须仍能识别为同一个业务对象**

| 模块 | 场景 | Identity 是否一致 |
|---|---|---|
| POS | 扫码销售 ¥3 | ✅ |
| Procurement | 供应商报价 ¥2/罐 | ✅ |
| Inventory | 入库 100 罐、出库 5 罐 | ✅ |
| Recipe | 可乐鸡翅配料 | ✅ |
| Reporting | 按 SKU 统计销量 | ✅ |

**结论**：Coca-Cola 330ml 是 **一个** Canonical Identity，它在不同 Context 中以不同 Representation / Profile 出现，但 Identity 本身不变。

---

### Invariant 3: 概念分离

**Role / Capability / Profile / Context 不得互相混淆。必须分别定义。**

| 概念 | 明确归属 | 禁止混淆 |
|---|---|---|
| Identity | `Canonical Business Item` | ≠ Role |
| Role | 某个 Capability 在特定 Context 下的表现形式 | ≠ Identity |
| Capability | Identity 能参与的业务活动 | ≠ Role |
| Profile | Identity 在特定 Context 的配置属性集 | ≠ Capability |
| Context | 使用场景（POS / Procurement / Inventory / Recipe） | ≠ Profile |

**结论**：每个概念必须有独立的 ontology ownership。禁止出现 "Capability + Role" 或 "Role + Profile" 这种混用定义。

---

## 三、Canonical Identity 重新评估

### 3.1 Business Item 是否应该拥有独立 Canonical Identity？

#### Evidence 分析

| 模块 | 当前实现 | 问题 |
|---|---|---|
| POS | `product` table | 同一商品在不同 POS channel 有重复记录 |
| Procurement | `material` table | 同一可乐可能被建为两个 material |
| Inventory | `stock_item` / `inventory_item` | Identity 来源不统一 |
| Recipe | `ingredient` table | 引用 material 或 product，缺乏统一 Identity |

#### Counterexample

如果没有独立 Canonical Identity：
- Coca-Cola 在 POS 是 `product_id=101`，在 Procurement 是 `material_id=205`，在 Inventory 是 `stock_id=310`
- 跨模块统计（如：可乐的采购成本 vs 销售收入）需要复杂的多表 JOIN 且依赖人工匹配
- 新增模块（如：loyalty program）无法直接引用同一个业务对象

#### 结论

**Business Item 应该拥有独立 Canonical Identity。**

- **Status**: REASSESSED
- **Recommendation**: 建立 `canonical_business_item` 作为 Identity 层，各模块通过 Reference 引用
- **当前现实**: 各模块自行维护 Identity，无统一 Canonical Identity
- **目标模型**: Canonical Identity + Context-specific Profile

---

### 3.2 Material 是否只是当前 Implementation Identity？

#### Evidence 分析

| 检查项 | 说明 |
|---|---|
| 当前 Material 的含义 | 在现有实现中，Material 通常 = "可采购的物料" |
| Material 与 Identity 的关系 | Material 是 Identity 的一个子集，不是 Identity 本身 |
| Material 的局限性 | 不 Sellable 的物品（如餐盒）仍被归类为 Material，但 Sellable 的物品（如瓶装水）同时是 Product |

#### Counterexample

- 酱油：是 Material（可采购），也是 Recipe Component（配料），也可以 Sellable（整瓶销售）——但当前实现中，它在 `material` 表有一个记录，在 `product` 表可能有另一个记录
- 宫保鸡丁：是 Product（可销售），但不是 Material（不采购）——当前实现中它只存在于 `product` 表

#### 结论

**Material 是当前 Implementation Identity，不是 Target Canonical Identity。**

- **Status**: REASSESSED
- **Recommendation**: Material 应降级为 Profile（Procurement Profile），不再承担 Identity 职责
- **当前现实**: Material ≈ Identity + Procurement Capability 混合体
- **目标模型**: Material = Canonical Business Item + Procurement Profile

---

### 3.3 Food 的语义定位

#### Evidence 分析

Food 在不同语境中含义不同：

| 语境 | Food 的含义 | 角色 |
|---|---|---|
| POS 菜单 | 一道可销售的菜品 | Sellable Entity |
| Recipe 管理 | 菜品的配方组合 | Recipe Output |
| Kitchen 生产 | 厨房制作的产出物 | Production Output |
| Inventory | 部分 Food 可库存（预制品），部分不可（现做现卖） | 取决于库存策略 |

#### Counterexample

- 宫保鸡丁：Food，Sellable，Recipe Output，Not Purchasable，Not Stockable
- 瓶装可乐：Food（广义上是食物/饮品），但不是 Recipe Output，是 Purchasable + Stockable 的商品

#### 结论

**Food 不是 Identity。Food 是 Domain Entity（领域实体），更具体地说是 Sellable + Recipe Output 的 Business Item 的领域分类。**

- **Status**: REASSESSED
- **Food 语义定位**: Domain Entity（领域实体）—— 表示"可以被食用/消费的物品"的领域分类
- **Food ≠ Identity**: Food 是一种业务分类，不是 Identity 层概念
- **Food ≠ Profile**: Food 不只是某个 Context 的配置
- **Food ≠ Type**: Food 跨越多个 Type（菜品、饮品、原料）

---

### 3.4 Material 的语义定位

#### Evidence 分析

| 检查项 | 说明 |
|---|---|
| Material 作为 Identity? | 不是——Material 缺乏跨模块 Identity 稳定性 |
| Material 作为 Type? | 不是——Material 涵盖范围太广（从原材料到成品） |
| Material 作为 Procurement Profile? | 最接近——Material 的核心语义是"可被采购的物品" |
| Material 作为 Inventory Profile? | 部分相关——Material 通常也是可库存的 |
| Material 作为 Domain Entity? | 是——Material 是"可被采购和存储的物料"的领域分类 |

#### Counterexample

- 宫保鸡丁：不是 Material（不采购），但是 Food / Domain Entity
- 餐盒：是 Material（采购），但不是 Food

#### 结论

**Material 是 Domain Entity（领域实体）+ Procurement Profile（采购配置）。**

- **Status**: REASSESSED
- **Material 语义定位**: Domain Entity（领域实体）—— 表示"可被采购和存储的物料"的领域分类
- **Material 的 Profile 层**: Procurement Profile + Inventory Profile
- **Material ≠ Identity**: Material 不应承担 Identity 职责

---

### 3.5 Product 的语义定位

#### Evidence 分析

| 检查项 | 说明 |
|---|---|
| Product 作为 Legacy Concept? | 是——当前实现中 `product` table 承载过多语义 |
| Product 作为 Commercial Definition? | 最接近——Product 的核心语义是"可被商业交易的物品" |
| Product 作为 Sellable Product? | 部分——Product 通常是 Sellable 的，但不是唯一的 Sellable 类型 |
| Product 作为 Canonical Business Item? | 不是——Product 是 Business Item 在商业语境中的表现 |
| Product 作为 Umbrella Concept? | 是——Product 涵盖 Food / Beverage / Packaged Goods / Retail Goods |

#### Counterexample

- 酱油：在 `product` 表中可能是 Sellable Product，在 `material` 表中是 Procurement Material——同一个 Canonical Identity 有两种表现
- 餐盒：在某些实现中是 Material（采购），不是 Product——但如果直接卖给顾客呢？

#### 结论

**Product 是 Commercial Concept（商业概念），不是 Identity。Product 是 Canonical Business Item 在 Sellable Context 中的商业表现。**

- **Status**: REASSESSED
- **Product 语义定位**: Commercial Concept（商业概念）—— 表示"可以被商业交易的物品"
- **Product ≠ Identity**: Product 不应承担 Identity 职责
- **Product 是 Umbrella**: Product 涵盖 Food / Beverage / Packaged Goods / Retail Goods
- **当前现实**: `product` table = Identity + Commercial Profile + 部分 Inventory Profile 混合体
- **目标模型**: Product = Canonical Business Item + Commercial Profile + Sales Profile

---

## 四、Product 重新评估

### 4.1 Product 作为 Umbrella / Commercial Concept

#### 验证：Product 是否可以表示 Food / Beverage / Packaged Goods / Retail Goods

| Product Type | 举例 | 验证 |
|---|---|---|
| Food | 宫保鸡丁、红烧肉 | ✅ 可销售 |
| Beverage | 可乐、雪碧、啤酒 | ✅ 可销售 |
| Packaged Goods | 袋装薯片、罐头 | ✅ 可销售 |
| Retail Goods | 餐具、纪念品 | ✅ 可销售 |

#### 验证：某些 Product Sellable + Purchasable + Stockable，另一些 Sellable only

| 模式 | 举例 | 验证 |
|---|---|---|
| Sellable + Purchasable + Stockable | 瓶装可乐（采购→库存→销售） | ✅ |
| Sellable + Purchasable + Stockable + Consumable | 酱油（采购→库存→可销售→可消耗于菜品） | ✅ |
| Sellable only（现做现卖） | 宫保鸡丁（Recipe 产出→直接销售） | ✅ |

#### 验证：Purchasable + Stockable + Consumable 的场景

| 模式 | 举例 | 验证 |
|---|---|---|
| Purchasable + Stockable + Consumable（不 Sellable） | 餐盒（采购→库存→厨房消耗） | ✅ |
| Purchasable + Stockable + Consumable + Sellable | 酱油（采购→库存→可消耗→可销售） | ✅ |

**结论**：Product 作为 Umbrella / Commercial Concept 成立。但 Product 不是 Identity。

---

### 4.2 Legacy product table ≠ Product business concept

| 维度 | Legacy `product` table | Product business concept |
|---|---|---|
| 定义 | 当前数据库中的产品表 | 商业语境中可交易物品的概念 |
| Identity | 包含 Identity 字段（id, code, name） | 不承担 Identity 职责 |
| Profile | 混合了 Commercial + Sales + Inventory Profile | 只包含 Commercial + Sales Profile |
| 可扩展性 | 受限于固定 schema | 可通过 Profile 扩展到任意 Context |

**必须区分**：

- **Legacy `product` table** = Identity + Commercial Profile + Sales Profile + 部分 Inventory Profile 的混合体
- **Product business concept** = Canonical Business Item 在 Sellable Context 中的商业表现

**Status**: REASSESSED

---

## 五、Food / Material 重新评估

### Case A: Coca-Cola 330ml

| 维度 | 值 |
|---|---|
| Business Identity | 一个 Canonical Identity（Coca-Cola 330ml） |
| Commercial/Sales Profile | Sellable at POS, price ¥3, category Beverage |
| Procurement Profile | Purchasable from Supplier A, cost ¥2/can |
| Inventory Profile | Stockable, min stock 50, max stock 500, current 120 |
| Recipe Component Relationship | 可作为套餐组合中的 Beverage 配件 |
| Domain Entity | Food（Beverage）+ Material（可采购物料） |

**结论**：一个 Canonical Identity，拥有多个 Profile，在不同 Context 中表现不同。

---

### Case B: 宫保鸡丁

| 维度 | 值 |
|---|---|
| Business Identity | 一个 Canonical Identity（宫保鸡丁） |
| Sellable | ✅ 在 POS 可销售，price ¥38 |
| Recipe Output | ✅ 由 Recipe 产出（鸡胸肉 + 花生 + 酱油 + ...） |
| Purchasable | ❌ 不从外部采购 |
| Stockable | ❌ 通常现做现卖，不库存 |
| Domain Entity | Food（菜品） |

**结论**：一个 Canonical Identity，是 Sellable + Recipe Output，但不是 Material。

---

### Case C: 鸡翅

| 维度 | 值 |
|---|---|
| Business Identity | 一个 Canonical Identity（鸡翅） |
| Purchasable | ✅ 从供应商采购 |
| Stockable | ✅ 冷库存储，有库存数量 |
| Recipe Component | ✅ 是可乐鸡翅、烤鸡翅等 Recipe 的原料 |
| Sellable | ⚠️ 可能——某些餐厅直接销售烤鸡翅作为单品 |
| Domain Entity | Food + Material（既是食材也是可售商品） |

**结论**：一个 Canonical Identity，根据业务场景可能同时是 Material + Recipe Component + Sellable。

---

### Case D: 餐盒

| 维度 | 值 |
|---|---|
| Business Identity | 一个 Canonical Identity（餐盒） |
| Purchasable | ✅ 从供应商采购 |
| Stockable | ✅ 仓库存储 |
| Consumable | ✅ 厨房/打包时消耗 |
| Sellable | ❌ 通常不单独销售（除非作为商品） |
| Domain Entity | Material（物料） |

**结论**：一个 Canonical Identity，是 Purchasable + Stockable + Consumable，但不是 Sellable、不是 Food。

---

### Case E: 服务 / Digital / Non-stock item

| 维度 | 值 |
|---|---|
| Business Identity | 一个 Canonical Identity（如：外卖配送费） |
| Sellable | ✅ 可以在 POS 作为收费项 |
| Stockable | ❌ 不能库存 |
| Purchasable | ❌ 不从外部采购 |
| Domain Entity | Service（服务类） |

**验证：Inventory Object 不能自动等于全部 Business Item**

| Business Item Type | Stockable? | Inventory Object? |
|---|---|---|
| Coca-Cola 330ml | ✅ | ✅ |
| 宫保鸡丁 | ❌ | ❌ |
| 餐盒 | ✅ | ✅ |
| 外卖配送费 | ❌ | ❌ |
| 清洁剂 | ✅ | ✅ |

**结论**：不是所有 Business Item 都是 Inventory Object。Inventory Object 只是 Business Item 的一个子集。

---

## 六、Type / Role / Capability / Profile / Relationship / Context / Scope 重新定义

### 6.1 Type

| 维度 | 内容 |
|---|---|
| **概念** | 对 Business Item 进行领域分类的标签 |
| **定义** | Type 表示 Business Item 在业务领域中的分类归属 |
| **判断规则** | 如果一个 Business Item 属于某个业务领域（Food / Beverage / Material / Service），它就拥有该 Type |
| **正例** | Coca-Cola = Type: Beverage; 宫保鸡丁 = Type: Food; 餐盒 = Type: Material |
| **反例** | ❌ Type 不表示"能做什么"（那是 Capability）；❌ Type 不表示"怎么用"（那是 Profile） |
| **Owner** | Canonical Business Item |
| **Scope** | 全局（跨所有 Context） |

---

### 6.2 Role

| 维度 | 内容 |
|---|---|
| **概念** | Business Item 在特定 Context 中被赋予的业务角色 |
| **定义** | Role = Identity + Context + 行为约束 |
| **判断规则** | 如果一个 Business Item 在某个 Context 中承担特定业务职责（如：POS 中的"商品"、Recipe 中的"原料"），它就扮演该 Role |
| **正例** | Coca-Cola 在 POS 中扮演 "Sales Item" Role；在 Recipe 中扮演 "Ingredient" Role |
| **反例** | ❌ Role ≠ Identity（Role 可以变化，Identity 不变）；❌ Role ≠ Capability（Role 包含 Context 约束） |
| **Owner** | Canonical Business Item + Context |
| **Scope** | Context-specific（POS / Procurement / Inventory / Recipe） |

---

### 6.3 Capability

| 维度 | 内容 |
|---|---|
| **概念** | Business Item 能参与的业务活动类型 |
| **定义** | Capability 表示 Business Item 支持哪些业务操作 |
| **判断规则** | 如果一个 Business Item 能执行某操作（Sell / Purchase / Stock / Consume），它就拥有该 Capability |
| **正例** | Coca-Cola = Capability: Sellable + Purchasable + Stockable + Consumable |
| **反例** | ❌ Capability ≠ Role（Capability 不包含 Context）；❌ Capability ≠ Profile（Capability 不包含配置属性） |
| **Owner** | Canonical Business Item |
| **Scope** | 全局（跨所有 Context） |

---

### 6.4 Profile

| 维度 | 内容 |
|---|---|
| **概念** | Business Item 在特定 Context 下的配置属性集 |
| **定义** | Profile = Identity + Context + Configuration Attributes |
| **判断规则** | 如果一个 Business Item 在某个 Context 中需要特定配置（如：POS 中的价格、Procurement 中的供应商信息），它就拥有该 Profile |
| **正例** | Coca-Cola 的 POS Profile: price=¥3, category=Beverage; Procurement Profile: supplier=Supplier A, cost=¥2 |
| **反例** | ❌ Profile ≠ Capability（Profile 包含配置属性，Capability 不包含）；❌ Profile ≠ Role（Profile 是静态配置，Role 是动态行为） |
| **Owner** | Canonical Business Item + Context |
| **Scope** | Context-specific（POS / Procurement / Inventory / Recipe） |

---

### 6.5 Relationship

| 维度 | 内容 |
|---|---|
| **概念** | 两个或多个 Business Item 之间的业务关联 |
| **定义** | Relationship = Identity A + Identity B + Relationship Type + Context |
| **判断规则** | 如果两个 Business Item 之间存在业务关联（如：Recipe 关联、替代品关系、组合关系），它们就拥有 Relationship |
| **正例** | 宫保鸡丁 ↔ 鸡翅: Recipe Component Relationship; Coca-Cola ↔ Pepsi: Substitute Relationship |
| **反例** | ❌ Relationship ≠ Role（Relationship 连接两个 Identity，Role 描述单个 Identity 的行为）；❌ Relationship ≠ Profile |
| **Owner** | 两个或多个 Canonical Business Items |
| **Scope** | Context-specific（Recipe / Sales / Procurement） |

---

### 6.6 Context

| 维度 | 内容 |
|---|---|
| **概念** | Business Item 被使用的业务场景 |
| **定义** | Context = 业务模块 + 使用目的 + 规则集 |
| **判断规则** | 如果 Business Item 在某个业务模块中被使用，该模块就是它的一个 Context |
| **正例** | POS Context; Procurement Context; Inventory Context; Recipe Context; Reporting Context |
| **反例** | ❌ Context ≠ Profile（Context 是场景，Profile 是该场景下的配置）；❌ Context ≠ Scope |
| **Owner** | System（系统定义） |
| **Scope** | 系统级（跨所有 Business Items） |

---

### 6.7 Scope

| 维度 | 内容 |
|---|---|
| **概念** | 概念的适用范围和作用边界 |
| **定义** | Scope = 概念作用的业务边界 |
| **判断规则** | 如果一个概念只在特定范围内有效，该范围就是它的 Scope |
| **正例** | Type: Global Scope; Role: Context-specific Scope; Profile: Context-specific Scope |
| **反例** | ❌ Scope ≠ Context（Scope 是边界，Context 是场景） |
| **Owner** | System（系统定义） |
| **Scope** | 系统级 |

---

## 七、Ingredient 重新评估

### 保留候选：Ingredient = Recipe ↔ Item Relationship

| 维度 | 内容 |
|---|---|
| **概念** | Ingredient 是 Recipe 与 Business Item 之间的 Relationship |
| **定义** | Ingredient = Recipe Identity + Business Item Identity + Quantity + UOM + Preparation Notes |
| **判断规则** | 如果一个 Business Item 在某个 Recipe 中被使用，它在该 Recipe 中就扮演 Ingredient Role |
| **Owner** | Recipe + Business Item |
| **Scope** | Recipe-specific |

### 验证：同一个 Item 在不同 Recipe 中的角色

| Item | Recipe A (可乐鸡翅) | Recipe B (套餐组合) | Recipe C (宫保鸡丁) |
|---|---|---|---|
| Coca-Cola | Ingredient (配料) | Ingredient (饮料配件) | ❌ 不是 Ingredient |
| 鸡翅 | Ingredient (主料) | ❌ 不是 Ingredient | ❌ 不是 Ingredient |
| 酱油 | Ingredient (调味) | ❌ 不是 Ingredient | Ingredient (调味) |

**结论**：Ingredient 是 Relationship，不是 Identity。同一个 Item 在不同 Recipe 中可以是/不是 Ingredient。

---

## 八、Consumable 重新评估

### 验证：Recipe Consumption vs Supply Consumption vs Operational Consumption

| Consumption Type | 定义 | 举例 |
|---|---|---|
| **Recipe Consumption** | 在制作菜品时消耗的原料 | 酱油 → 宫保鸡丁 Recipe 消耗 |
| **Supply Consumption** | 在日常运营中消耗的供应品 | 餐盒 → 打包时消耗 |
| **Operational Consumption** | 在设备维护/清洁中消耗的物品 | 清洁剂 → 厨房清洁消耗 |

### 验证：不同物品的 Consumption 模式

| 物品 | Recipe Consumption | Supply Consumption | Operational Consumption | Sellable |
|---|---|---|---|---|
| 酱油 | ✅ | ❌ | ❌ | ✅ (整瓶销售) |
| 餐盒 | ❌ | ✅ | ❌ | ❌ |
| 清洁剂 | ❌ | ❌ | ✅ | ❌ |
| 鸡翅 | ✅ | ❌ | ❌ | ✅ (烤鸡翅) |
| 食用油 | ✅ | ❌ | ❌ | ✅ (整瓶销售) |

**结论**：Consumable 是 Capability，不是 Identity。不同物品根据业务场景拥有不同的 Consumption 模式。

---

## 九、Inventory Object 重新评估

### 区分：Business Item / Stockable Item / Inventory Fact / Inventory Instance / Inventory Ledger / Balance / Snapshot / Aggregation

| 概念 | 定义 | 举例 |
|---|---|---|
| **Business Item** | 跨模块的 Canonical Identity | Coca-Cola 330ml |
| **Stockable Item** | 拥有 Stockable Capability 的 Business Item | Coca-Cola 330ml (可库存) |
| **Inventory Fact** | 某个时刻发生的库存变动事件 | "2026-09-10 入库 100 罐" |
| **Inventory Instance** | 某个 Storage Location 中的 Stockable Item 实例 | "A 仓库 Coca-Cola 330ml" |
| **Inventory Ledger** | 记录所有 Inventory Fact 的流水账 | 入库/出库/盘点记录 |
| **Balance** | 某个 Inventory Instance 当前的库存数量 | "A 仓库 Coca-Cola 330ml: 120 罐" |
| **Snapshot** | 某个时间点的库存快照 | "2026-09-10 23:59:59 全部仓库 Coca-Cola 余额" |
| **Aggregation** | 跨多个 Inventory Instance 的库存汇总 | "所有仓库 Coca-Cola 总库存: 500 罐" |

### 重新回答：Inventory 到底管理什么

Inventory 管理的是 **Stockable Item 在 Storage Location 中的数量变化**。

- **管理对象**: Stockable Item（Business Item 的子集）
- **管理维度**: Location（存储位置）+ Time（时间）+ Quantity（数量）
- **核心数据**: Inventory Fact (事件) → Balance (状态) → Snapshot (快照) → Aggregation (汇总)

**Status**: REASSESSED

---

## 十、Location 重新评估

### 检查：Store / Warehouse / Kitchen / Transit 是 Location Type？Location？Inventory State？Ownership State？

| Location | Location Type | Location 实体 | Inventory State | Ownership State |
|---|---|---|---|---|
| **Store** | Retail | ✅ 是 Location（物理位置） | 部分（Store 也有库存） | 部分（Store 拥有库存） |
| **Warehouse** | Storage | ✅ 是 Location（物理位置） | ✅ 是 Inventory State（库存存储位置） | 部分（Warehouse 拥有库存） |
| **Kitchen** | Production | ✅ 是 Location（物理位置） | ❌ 不是 Inventory State（Kitchen 是消耗位置） | ❌ 不是 Ownership State |
| **Transit** | Virtual | ⚠️ 不是物理 Location | ✅ 是 Inventory State（在途库存） | ⚠️ 取决于所有权转移时点 |

### 特别检查：Transit 是 Virtual Location 还是 Inventory State？

| 检查项 | Transit = Virtual Location? | Transit = Inventory State? |
|---|---|---|
| Evidence | Transit 不是物理位置，无法存储物品 | Transit 表示物品正在运输中，有明确的数量 |
| Counterexample | 如果 Transit 是 Location，那么"在途"应该有地址——但没有 | 如果 Transit 只是 State，那么"在途"应该有时间戳和数量——有 |
| 结论 | Transit **不是** Virtual Location | Transit **是** Inventory State |

**结论**：

- Location = 物理或逻辑位置（Store / Warehouse / Kitchen）
- Transit = Inventory State（在途状态），不是 Location
- **Status**: REASSESSED

---

## 十一、UOM 重新评估

### 分析：Unit / UOM / Base UOM / Context UOM / Conversion / Packaging / Partial Consumption

| 概念 | 定义 | 举例 |
|---|---|---|
| **Unit** | 物品的基本计量单位 | 罐、瓶、箱、千克 |
| **UOM (Unit of Measure)** | 计量单位的标准定义 | piece, kg, L, box |
| **Base UOM** | 物品的最小可分割计量单位 | Coca-Cola: piece (单罐) |
| **Context UOM** | 在特定 Context 中使用的计量单位 | POS: piece; Procurement: box (12罐/箱); Inventory: piece |
| **Conversion** | 不同 UOM 之间的换算关系 | 1 box = 12 piece |
| **Packaging** | 物品的包装层级 | Coca-Cola: piece (单罐) → pack (6罐) → box (12罐) → pallet (120罐) |
| **Partial Consumption** | 部分消耗时的计量方式 | 酱油: 1 bottle → 每次 Recipe 消耗 15ml |

### 回答：为什么需要多个 UOM？

| 原因 | 说明 | 举例 |
|---|---|---|
| **业务场景不同** | 不同 Context 需要不同计量方式 | POS: piece; Procurement: box; Recipe: ml |
| **包装层级不同** | 物品可能有多种包装规格 | Coca-Cola: piece / pack / box / pallet |
| **部分消耗** | 某些物品不是整件消耗 | 酱油: 整瓶采购，按 ml 消耗 |
| **成本核算** | 不同 UOM 影响成本计算 | 采购价按 box 计算，消耗成本按 ml 计算 |

**结论**：多个 UOM 是业务需求的必然结果。Canonical Identity 必须支持多 UOM，且 UOM 是 Context-specific 的。

**Status**: REASSESSED

---

## 十二、Ledger / Balance 重新评估

### 保持概念分离

| 概念 | 定义 | 特性 |
|---|---|---|
| **Ledger** | 记录所有库存变动事件的流水账 | 只增不改、可审计、时间序列 |
| **Balance** | 某个 Inventory Instance 当前的库存数量 | 可变、实时、由 Ledger 计算得出 |

### 概念分离原则

| 原则 | 说明 |
|---|---|
| Ledger ≠ Balance | Ledger 是事件记录，Balance 是状态结果 |
| Ledger 只增不改 | 已记录的 Ledger 不可修改（审计需求） |
| Balance 可重算 | Balance 可以从 Ledger 重新计算得出 |
| Balance = f(Ledger) | Balance 是 Ledger 的聚合函数结果 |

### 输出 Semantic Candidate + Architecture Decision Boundary

| 概念 | Semantic Candidate | Architecture Decision Boundary |
|---|---|---|
| **Ledger** | Inventory Fact Collection（库存事件集合） | 存储策略（分区、归档）、查询性能 |
| **Balance** | Inventory Snapshot（库存快照） | 实时计算 vs 缓存、并发控制 |
| **Ledger → Balance** | Aggregation Function（聚合函数） | 增量更新 vs 全量重算 |

**Status**: REASSESSED

---

## 十三、15个核心问题回答

### Q1: Business Item 是否需要 Canonical Identity？

**答**：是。所有业务实体应共享一个 Canonical Identity 层，各模块通过 Reference 引用。

### Q2: Material 是否应该被重新定义？

**答**：是。Material 应从 Identity 降级为 Domain Entity + Procurement Profile。

### Q3: Product 是否应该被重新定义？

**答**：是。Product 应从 Identity 降级为 Commercial Concept + Sales Profile。

### Q4: Food 是否需要独立的 Identity？

**答**：不需要。Food 是 Domain Entity（领域分类），不是 Identity。

### Q5: Inventory Object 是否等于 Business Item？

**答**：不是。Inventory Object 是 Business Item 的子集（只有 Stockable 的才是）。

### Q6: Transit 是 Location 还是 Inventory State？

**答**：Inventory State。Transit 不是物理位置，是库存状态。

### Q7: Ingredient 是 Identity 还是 Relationship？

**答**：Relationship。Ingredient = Recipe ↔ Business Item Relationship。

### Q8: Consumable 是 Identity 还是 Capability？

**答**：Capability。Consumable 表示"可以被消耗"的业务能力。

### Q9: 为什么需要多个 UOM？

**答**：业务场景不同（POS/Procurement/Recipe）、包装层级不同、部分消耗需求。

### Q10: Ledger 和 Balance 的关系是什么？

**答**：Balance = f(Ledger)。Ledger 是事件记录，Balance 是状态结果。

### Q11: Type / Role / Capability / Profile 如何区分？

**答**：Type=领域分类, Role=Context行为, Capability=业务能力, Profile=Context配置。各有独立 ontology ownership。

### Q12: 当前实现与目标模型的差距有多大？

**答**：差距较大。当前实现中 Identity / Profile / Capability 混合在各模块的表中，缺乏统一的 Canonical Identity 层。

### Q13: 是否需要 Migration Plan？

**答**：是。需要从当前 Implementation Identity 迁移到 Target Canonical Identity。

### Q14: 哪些 Decision 是 OPEN 的？

**答**：Canonical Identity 的具体 schema 设计、Profile 的存储策略、跨模块 Reference 的实现方式。

### Q15: 是否可以分阶段实施？

**答**：是。建议分阶段：先建立 Canonical Identity → 再迁移各模块 → 最后优化 Profile。

---

## 十四、最终 Verdict

```
SEMANTIC-REASSESSMENT = PASS_WITH_OPEN_DECISIONS
```

### 状态总结

| 概念 | 状态 | 说明 |
|---|---|---|
| Canonical Business Item | REASSESSED | 需要独立 Identity 层 |
| Material | REASSESSED | 从 Identity 降级为 Domain Entity + Procurement Profile |
| Product | REASSESSED | 从 Identity 降级为 Commercial Concept + Sales Profile |
| Food | REASSESSED | 确认为 Domain Entity，不是 Identity |
| Inventory Object | REASSESSED | 确认为 Business Item 的子集（Stockable） |
| Ingredient | REASSESSED | 确认为 Relationship，不是 Identity |
| Consumable | REASSESSED | 确认为 Capability，不是 Identity |
| Transit | REASSESSED | 确认为 Inventory State，不是 Location |
| UOM | REASSESSED | 确认需要多 UOM 支持（Context-specific） |
| Ledger / Balance | REASSESSED | 确认概念分离（事件 vs 状态） |
| Type / Role / Capability / Profile | REASSESSED | 确认独立 ontology ownership |

### OPEN Decisions

| Decision | 说明 |
|---|---|
| Canonical Identity Schema | 具体的数据库 schema 设计待定 |
| Profile Storage Strategy | Profile 存储方式（独立表 vs JSON 字段）待定 |
| Cross-module Reference | 跨模块 Reference 的实现方式（FK vs UUID）待定 |
| Migration Plan | 从当前实现迁移到目标模型的具体计划待定 |
| Legacy Data Cleanup | 历史数据清理策略待定 |

### 关键原则重申

- **Status**: REASSESSED / PARTIAL / OPEN（禁止输出 CONFIRMED / LOCKED）
- **当前现实 ≠ 目标模型**：当前实现是历史演进的结果，目标模型是理想状态
- **Recommendation ≠ Decision**：本文档提供的是分析和建议，不是最终决策
- **Current Implementation Identity ≠ Target Canonical Identity**：当前各模块的 Identity 实现不是最终的 Canonical Identity
