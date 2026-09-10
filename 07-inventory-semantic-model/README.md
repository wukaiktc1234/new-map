# Inventory Semantic Model

## 项目名称

**Inventory, Stockable Item, Location, Quantity, UOM & Multi-Role Business Model Analysis**

## 分析目标

回答：**库存到底是什么？**

并建立：
- Canonical Business Identity → Stockable Capability → Inventory Location → Stock Quantity → UOM / Conversion → Inventory Movement
- Purchase → Receive → Stock → Transfer → Sale / Consumption → Adjustment

之间的统一业务语义。

## 核心原则

1. **Current DB Model ≠ Target Inventory Model**
2. **不得因为存在 Store Inventory + Warehouse Inventory，就建立两套库存 Truth**
3. **聚合库存不能成为第二 Truth Source**
4. **不要把库存简单等同于"商品数量"**
5. **长期业务正确性优先于当前最小改动**
6. **Recommendation ≠ Decision. Current Reality ≠ Target Semantic Model.**

## 分析阶段

| Phase | 内容 | 文件 |
|-------|------|------|
| 1 | Inventory Truth 分析 | inventory-truth-analysis.md |
| 2 | Stockable Object 分析 | stockable-object-analysis.md |
| 3 | Stockable Capability 分析 | stockable-capability-analysis.md |
| 4 | Inventory Location 模型 | inventory-location-model.md |
| 5 | 门店 vs 仓储分析 | store-vs-warehouse-analysis.md |
| 6 | Inventory Quantity 模型 | inventory-quantity-model.md |
| 7 | UOM 模型 | inventory-uom-model.md |
| 7-ext | UOM Conversion 模型 | uom-conversion-model.md |
| 8 | Canonical Stock Quantity | (包含在 inventory-quantity-model.md) |
| 9 | 部分消耗分析 | partial-consumption-analysis.md |
| 10 | Inventory Movement 模型 | inventory-movement-model.md |
| 11 | 销售/消耗库存流 | sale-consumption-inventory-flow.md |
| 12 | 采购/收货库存流 | purchase-receipt-inventory-flow.md |
| 13 | 调拨库存流 | transfer-inventory-flow.md |
| 14 | Inventory State 模型 | inventory-state-model.md |
| 15 | Batch/Lot/Expiry 分析 | batch-lot-expiry-analysis.md |
| 16 | 拆包分析 | open-package-analysis.md |
| 17 | Costing 交互 | inventory-costing-interaction.md |
| 18+20 | Inventory Identity 分析 | inventory-identity-analysis.md |
| 19 | 跨域库存流 | cross-domain-inventory-flow.md |
| 21 | 当前→目标语义映射 | current-to-target-inventory-semantic-mapping.md |
| 22 | 候选模型比较 | candidate-inventory-models.md |
| 22-ext | 模型压力点 | inventory-model-stress-points.md |
| 22-final | 语义推荐 | inventory-semantic-recommendation.md |
| 23 | 反例验证 | counterexample-inventory-validation.md |

## 文件清单

| # | 文件 | 大小 | Phase |
|---|------|------|-------|
| 1 | `README.md` | - | 目录说明 |
| 2 | `inventory-truth-analysis.md` | ~15KB | 1 |
| 3 | `stockable-object-analysis.md` | ~27KB | 2 |
| 4 | `stockable-capability-analysis.md` | ~22KB | 3 |
| 5 | `inventory-location-model.md` | ~15KB | 4 |
| 6 | `store-vs-warehouse-analysis.md` | ~15KB | 5 |
| 7 | `inventory-quantity-model.md` | ~18KB | 6 |
| 8 | `inventory-uom-model.md` | ~28KB | 7 |
| 9 | `uom-conversion-model.md` | ~21KB | 7-ext |
| 10 | `partial-consumption-analysis.md` | ~15KB | 9 |
| 11 | `inventory-movement-model.md` | ~18KB | 10 |
| 12 | `sale-consumption-inventory-flow.md` | ~15KB | 11 |
| 13 | `purchase-receipt-inventory-flow.md` | ~15KB | 12 |
| 14 | `transfer-inventory-flow.md` | ~15KB | 13 |
| 15 | `inventory-state-model.md` | ~12KB | 14 |
| 16 | `batch-lot-expiry-analysis.md` | ~12KB | 15 |
| 17 | `open-package-analysis.md` | ~12KB | 16 |
| 18 | `inventory-costing-interaction.md` | ~15KB | 17 |
| 19 | `inventory-identity-analysis.md` | ~25KB | 18+20 |
| 20 | `cross-domain-inventory-flow.md` | ~15KB | 19 |
| 21 | `current-to-target-inventory-semantic-mapping.md` | ~20KB | 21 |
| 22 | `candidate-inventory-models.md` | ~25KB | 22 |
| 23 | `inventory-model-stress-points.md` | ~24KB | 22-ext |
| 24 | `inventory-semantic-recommendation.md` | ~24KB | 22-final |
| 25 | `counterexample-inventory-validation.md` | ~24KB | 23 |

## 核心结论摘要

### Inventory Truth

- **Stock Ledger 是 Truth Source**，Balance 是 Derived View
- Inventory Fact = Material × Location × Quantity
- 聚合库存是 Aggregated View，不是 Truth Source

### Stockable Object

- Inventory 管理的是 **Material**（原料/物料），不是 Food（成品菜品）
- Stockable Item = Material + Location

### Stockable Capability

- STOCKABLE 是 Material 的 **Business Role**（角色）
- 作用域推荐 **LOCATION-SCOPED**（位置级）

### Location Model

- Inventory Fact = Material × Location × Quantity
- Store、Warehouse、Kitchen、Transit 都属于 Inventory Location

### Quantity Model

- 推荐 **Base UOM Quantity** 存储库存真相
- 库存以 Base Quantity 保存，展示时按需转换

### Movement Model

- 16+ 种标准 Movement 类型
- 四个语义层次：Business Event → Inventory Movement → State Change → Derived Result

### 推荐模型

**Model D: Item + Location + Stock Ledger**（加权得分最高）

### DEC-006 衔接

Inventory 结论影响：
- Canonical Item 定义
- Product / Food / Material 边界
- Role / Capability 分层
- UOM 模型
- Location 模型
- Recipe 关系

## 下一步行动

1. **Product Owner 审阅** 本调和结果
2. **Architecture Owner 审阅** 推荐模型
3. **合并 DEC-006 + Inventory 决策** 进入 Decision Workshop
4. **进入工程准备** 基于确认的语义模型

## 状态

**RECOMMENDATION, NOT CONFIRMED**

等待 Product Owner / Architecture Owner Review

---

**日期**：2026-09-10
**作者**：AI 架构总控
**任务**：INVENTORY-SEMANTIC-MODEL-001
