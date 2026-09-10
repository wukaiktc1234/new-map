> **DOCUMENT STATUS:** ACTIVE_REFERENCE
> **SOURCE TASK:** BUSINESS-ITEM-MODEL-001

# 业务项模型分析总结

## 核心发现

### 1. 系统现状
当前系统存在三个独立的"物"概念：
- **Food（菜品）**：可销售的成品
- **Product（产品）**：已废弃的遗留概念
- **Material（物料/商品档案）**：可采购、可存储的原料

### 2. 库存管理
- 库存管理的是 `material_id`，不是 `food_id` 或 `product_id`
- Recipe（`dish_recipe`）连接 food 和 material/ingredient

### 3. 同一业务对象的多角色
同一个"可乐"在不同业务上下文中承担不同角色：
- 作为销售商品（Food）
- 作为采购对象（Material）
- 作为库存对象（Material/Stock Item）
- 作为菜品原料（Ingredient）

## 核心问题回答

### Q1: "可乐"在这个系统中到底应该是什么？
**答案**：一个统一的业务对象，在不同业务上下文中承担不同 Role
- **Canonical Identity**：统一的业务对象
- **Role**：在不同业务上下文中承担不同角色

### Q2: 同一个业务对象能否同时 Sellable、Purchasable、Stockable、Ingredient？
**答案**：是的，这是餐饮业务的核心特征

### Q3: 这些 Role 是否应该共享 Canonical Identity？
**答案**：是的，应该共享统一身份

### Q4: Product、Food、Material 的关系到底是什么？
- **Product**：已废弃的遗留概念
- **Food**：可销售的成品（菜品/食品）
- **Material**：可采购、可存储的原料/物料
- **关系**：两者通过 Recipe/BOM 建立关系，不是继承

### Q5: Inventory 管理的是什么？
**答案**：Material（物料/商品档案），通过 `material_id` 关联

### Q6: POS 销售的统一对象是什么？
**答案**：Food（菜品/食品）

### Q7: 采购的统一对象是什么？
**答案**：Material（物料/商品档案）

### Q8: Recipe 消耗的统一对象是什么？
**答案**：Material（作为 Ingredient）

### Q9: 不同 Role 的数据应该如何分层？
- **Identity 层**：name, code, description
- **Commercial 层**：selling price, POS visibility
- **Procurement 层**：supplier, purchase unit, lead time
- **Inventory 层**：stock unit, location, reorder point
- **Recipe 层**：quantity, yield, consumption unit

### Q10: Store Scope 是否可以改变 Role？
**答案**：是的，同一物料在不同门店可以有不同 Role

### Q11: Unit/UOM 在这个模型中处于什么位置？
**答案**：共享 Foundation，支持多种 UOM 角色

### Q12: 当前表应该分别如何映射？
- `foods` → Food Commercial Definition
- `material_archives` → Material Canonical Identity
- `inventory` → Material Stock Role
- `dish_recipe` → Food-Material Recipe Relationship

## 推荐模型

**Model C: Hybrid Domain Model（混合领域模型）**

> **标记为：RECOMMENDATION，NOT CONFIRMED**

### 模型特点
1. 统一的业务对象身份（Canonical Identity）
2. 多角色支持（Role-based）
3. 分层数据结构
4. 支持门店级别角色差异

## 需要产品决策的事项

1. **Product 是否存在**
2. **Food 是否是 Product 子类型**
3. **Material 是否独立**
4. **Item 是否需要建立**
5. **Inventory 是否管理 Item**
6. **Customer/Member 关系**
7. **Warehouse 最终模型**

## 参考映射

| 现有表 | 推荐映射 | 说明 |
|--------|----------|------|
| `foods` | Food Commercial Definition | 可销售的成品定义 |
| `material_archives` | Material Canonical Identity | 物料统一身份 |
| `inventory` | Material Stock Role | 库存角色 |
| `dish_recipe` | Food-Material Recipe Relationship | 菜品-原料关系 |

---

**状态**：只读分析任务，不修改任何代码  
**日期**：2026-09-09
