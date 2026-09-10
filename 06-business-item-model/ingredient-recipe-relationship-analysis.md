# Phase 6 - Ingredient/Recipe Component 语义分析

## 1. 引言

本文档对餐饮ERP系统中的 `Ingredient` 概念进行深度语义分析，核心问题：**"Ingredient" 究竟是对象的属性、角色还是业务关系中的参与方身份？**

## 2. 当前现实 (CURRENT REALITY)

### 2.1 数据库现状

系统中存在三套相关表结构（新旧并存）：

```
旧表 dish_recipe (V1.0.0.100, PostgreSQL):
├── id (PK)
├── recipe_id (配方编号, UNIQUE)
├── dish_id (菜品ID)
├── dish_name (菜品名称)
├── material_id (物料ID)         ← 字段名是 material_id
├── material_name (物料名称)     ← 字段名是 material_name
├── quantity (用量)
├── unit (单位)
├── is_required (是否必需)
└── version (乐观锁)

新表 dish_recipes (V6.0.0):
├── recipe_id (PK, AUTO)
├── food_id (菜品ID)
├── material_id (原料ID)         ← 直接使用 material_id
├── material_name (原料名称)
├── specification (规格)
├── required_quantity (标准用量)
├── unit (单位)
├── loss_rate (损耗率%)
├── unit_cost (原料单价, 分)
└── subtotal_cost (该项成本, 分)

已废弃表 ingredient (DEPRECATED):
├── ingredient_id (主键)
├── ingredient_code (配料编码)
├── ingredient_name (配料名称)
├── category_id (分类)
├── unit (单位)
├── cost_price (成本价)
├── supplier_id (供应商)
├── stock_quantity (库存)
└── ... (完整物料属性)
```

### 2.2 命名混乱的证据

| 位置 | 使用的命名 | 实际指向 |
|------|-----------|---------|
| V1 dish_recipe 表 | `material_id`, `material_name` | material_archives |
| V6 dish_recipes 表 | `material_id`, `material_name` | material_archives |
| DishRecipe 实体（旧） | `ingredientId`, `ingredientName` | 实际映射 `ingredient_id` 列 |
| DishRecipeNew 实体（新） | `materialId`, `materialName` | 实际映射 `material_id` 列 |
| RecipeItemDTO | `materialId`, `materialName` | material_archives |
| InventoryDeductionServiceImpl | `getIngredientId()`, `getIngredientName()` | 读取 dish_recipe 表 |
| StockForecastMapper | `JOIN dish_recipes r ON r.material_id` | 直接用 material_id |

**关键发现**：旧 DishRecipe 实体的字段名是 `ingredientId`/`ingredientName`，但数据库列名是 `ingredient_id`/`ingredient_name`（V1旧表），而新表和新实体已经统一使用 `material_id`/`material_name`。

### 2.3 已废弃的 ingredient 表

旧的 `ingredient` 表（DEPRECATED）定义了一个**独立的配料实体**：

```sql
-- 已废弃的 ingredient 表
ingredient: ingredient_id, ingredient_code, ingredient_name, category_id, unit, cost_price, supplier_id, stock_quantity...

-- 已废弃的 dish_ingredient 关联表
dish_ingredient: dish_id, ingredient_id, quantity, unit, cost_price, total_cost
```

这个表与 `material_archives` 有**大量字段重叠**（分类、单位、成本价、供应商、库存）。系统最终选择用 `material_archives` 统一管理，废弃了独立的 `ingredient` 表。

## 3. 业务语义分析 (TARGET BUSINESS SEMANTICS)

### 3.1 候选语义模型

#### 模型 A: Item → INGREDIENT Role（Ingredient 是 Material 的角色）

**定义**：Material 在特定上下文中扮演 "Ingredient" 角色，就像一个人可以扮演 "厨师"、"顾客" 等角色。

**支持证据**：
- 业务语言中常说 "鸡翅是可乐鸡翅的原料"
- 同一个 Material 可以参与多个 Recipe

**反对证据**：
- Role 通常是**身份标识**（如：供应商身份、顾客身份），而不是**参与方式描述**
- "Ingredient" 不改变 Material 的身份——鸡翅在仓库里是鸡翅，在配方里还是鸡翅
- Role 通常有独立的生命周期管理（角色激活/停用），但 "Ingredient" 没有
- 已废弃的 ingredient 表证明：系统曾经尝试将 Ingredient 作为独立实体，但最终失败
- 如果 Ingredient 是 Role，那么 `dish_recipe` 表应该是 "MaterialRoleAssignment" 而不是 "DishRecipe"

**结论：模型 A 不成立。Ingredient 不是 Material 的 Role。**

#### 模型 B: Recipe → RecipeComponent → Item（Ingredient 是 Recipe 的组成部分）

**定义**：Recipe 有多个 RecipeComponent，每个 RecipeComponent 描述一个 Material 在该 Recipe 中的参与方式（用量、单位、成本等）。

**支持证据**：
- `dish_recipes` 表的每条记录就是一个 RecipeComponent
- RecipeComponent 有独立属性：`required_quantity`, `unit`, `loss_rate`, `unit_cost`, `subtotal_cost`
- 同一个 Material 可以出现在不同 Recipe 的不同 RecipeComponent 中（不同用量、不同成本）
- RecipeComponent 有自己的业务逻辑：成本计算 = quantity × unit_cost × (1 + loss_rate)
- 代码中 `DishRecipeMapper.selectList(dishId)` 返回的是 RecipeComponent 列表

**反对证据**：
- 当前 RecipeComponent 没有独立主键（用自增 ID，不是业务编号）
- RecipeComponent 没有独立状态管理
- RecipeComponent 的 CRUD 依附于 Recipe

**结论：模型 B 最准确。Ingredient 本质上是 RecipeComponent——Recipe 的组成部分。**

#### 模型 C: Recipe → Material Requirement（Ingredient 是 Material 需求）

**定义**：Recipe 描述了制作一道菜需要哪些 Material，以及每种 Material 的需求量。

**支持证据**：
- `required_quantity` 字段名直接表达 "需求量"
- 成本核算逻辑：菜品成本 = Σ(Material 用量 × Material 单价)
- `deductByRecipe` 方法读取 Recipe 计算扣减量

**反对证据**：
- "需求" 是**单向**的（Recipe → Material），但实际关系是**双向**的（Material 也引用 Recipe）
- RecipeComponent 不仅描述 "需要什么"，还描述 "怎么用"（损耗率、允许偏差）
- "需求" 通常不包含成本属性，但 RecipeComponent 有 `unit_cost`, `subtotal_cost`
- 如果只是 "需求"，不需要 `allowVariance`（允许偏差）这样的生产控制属性

**结论：模型 C 部分成立，但过于简化。RecipeComponent 超出了 "需求" 的语义范围。**

### 3.2 深度分析：为什么 Ingredient 不是 Role？

#### 3.2.1 Role 的本质特征

Role（角色）在业务建模中的本质特征：

1. **身份绑定**：Role 绑定到特定身份（供应商身份、顾客身份）
2. **生命周期**：Role 有激活/停用状态
3. **权限边界**：Role 定义了能做什么、不能做什么
4. **不可替代性**：Role 一旦建立，不能随意替换（供应商不能突然变成顾客）

#### 3.2.2 Ingredient 不符合 Role 的特征

| Role 特征 | Ingredient 是否符合 | 原因 |
|-----------|-------------------|------|
| 身份绑定 | ❌ 不符合 | 鸡翅在配方里还是鸡翅，身份没变 |
| 生命周期 | ❌ 不符合 | 没有 "启用鸡翅作为原料" 的操作 |
| 权限边界 | ❌ 不符合 | Ingredient 不定义权限 |
| 不可替代性 | ❌ 不符合 | 鸡翅可以替换为鸭翅（虽然味道不同） |

#### 3.2.3 Ingredient 是什么？

**Ingredient 是 RecipeComponent 的业务名称**——它是 Recipe 与 Material 之间的关系描述，而不是 Material 的身份标签。

```
┌──────────────────────────────────────────────────────────────┐
│                    语义对比                                    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Role 模型（错误）:                                           │
│  Material ──(has)──▶ Ingredient Role                         │
│  鸡翅 ──(扮演)──▶ 原料角色                                    │
│                                                              │
│  Relationship 模型（正确）:                                    │
│  Recipe ──(has)──▶ RecipeComponent ──(uses)──▶ Material      │
│  可乐鸡翅配方 ──(包含)──▶ 配方项 ──(使用)──▶ 鸡翅              │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 3.3 多维度分析

#### 维度 1: 作为 RecipeComponent（配方组成部分）

**场景**：厨师定义菜品配方时，需要指定每种原料的用量。

**证据**：
- `dish_recipes` 表的每条记录定义一种原料的用量
- `required_quantity` + `unit` 定义标准用量
- `loss_rate` 定义损耗率
- `unit_cost` + `subtotal_cost` 定义成本

**结论**：Ingredient 在配方维度是 RecipeComponent。

#### 维度 2: 作为 MaterialRequirement（物料需求）

**场景**：库存扣减时，需要根据 Recipe 计算需要扣减多少 Material。

**证据**：
- `InventoryDeductionServiceImpl.deductByRecipe()` 读取 Recipe 计算扣减量
- `StockForecastMapper` JOIN `dish_recipes` 计算物料需求

**结论**：Ingredient 在库存维度是 MaterialRequirement。

#### 维度 3: 作为 CostComponent（成本组成部分）

**场景**：财务核算时，需要计算每道菜的原料成本。

**证据**：
- `subtotal_cost` = `required_quantity` × `unit_cost` × (1 + `loss_rate`)
- `material_consumption.total_cost` 记录实际消耗成本
- Recipe 的成本 = Σ(RecipeComponent.subtotal_cost)

**结论**：Ingredient 在财务维度是 CostComponent。

#### 维度 4: 作为 ConsumptionRecord（消耗记录）

**场景**：后厨制作菜品时，记录实际消耗的原料。

**证据**：
- `material_consumption` 表记录每次消耗：`material_id`, `consume_quantity`, `unit_cost`, `total_cost`
- 关联到 `kitchen_order_id`（后厨订单）和 `dish_id`（菜品）

**结论**：Ingredient 在生产维度是 ConsumptionRecord。

### 3.4 维度关系图

```
Ingredient 多维度模型：
┌──────────────────────────────────────────────────────────────┐
│                    Ingredient 本质                            │
│                                                              │
│  ┌─────────────────┐    ┌─────────────────┐                  │
│  │ RecipeComponent │    │ Material        │                  │
│  │ (配方组成部分)    │    │ Requirement     │                  │
│  │                 │    │ (物料需求)       │                  │
│  │ - required_qty  │    │ - 扣减量         │                  │
│  │ - unit          │    │ - 扣减方式       │                  │
│  │ - loss_rate     │    │ - 库存关联       │                  │
│  └─────────────────┘    └─────────────────┘                  │
│           │                     │                            │
│           │                     │                            │
│           ▼                     ▼                            │
│  ┌─────────────────┐    ┌─────────────────┐                  │
│  │ CostComponent   │    │ Consumption     │                  │
│  │ (成本组成部分)    │    │ Record          │                  │
│  │                 │    │ (消耗记录)       │                  │
│  │ - unit_cost     │    │ - consume_qty   │                  │
│  │ - subtotal_cost │    │ - unit_cost     │                  │
│  │ - 成本核算       │    │ - total_cost    │                  │
│  └─────────────────┘    └─────────────────┘                  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## 4. 真实案例验证

### 案例 1: 鸡翅的多 Recipe 参与

**场景**：鸡翅可以用于制作可乐鸡翅和香辣鸡翅。

**数据流分析**：

```
Material: 鸡翅 (material_id=100)
    │
    ├── Recipe A: 可乐鸡翅 (food_id=1)
    │   └── RecipeComponent: {material_id=100, quantity=500g, unit="g", loss_rate=5%, unit_cost=80分}
    │
    └── Recipe B: 香辣鸡翅 (food_id=2)
        └── RecipeComponent: {material_id=100, quantity=400g, unit="g", loss_rate=3%, unit_cost=80分}
```

**验证**：
- 鸡翅作为 Material，身份不变（还是鸡翅）
- 在 Recipe A 中，鸡翅的用量是 500g，损耗率 5%
- 在 Recipe B 中，鸡翅的用量是 400g，损耗率 3%
- **"Ingredient" 描述的是鸡翅在不同 Recipe 中的参与方式，而不是鸡翅的身份**

**结论**：Ingredient 是 RecipeComponent，不是 Material 的 Role。

### 案例 2: 可乐的多维度使用

**场景**：可乐可以用于制作可乐鸡翅，也可以直接销售。

**数据流分析**：

```
Material: 可乐 (material_id=200)
    │
    ├── 作为 RecipeComponent (Ingredient 维度):
    │   └── Recipe A: 可乐鸡翅
    │       └── RecipeComponent: {material_id=200, quantity=200ml, unit="ml"}
    │
    ├── 作为可销售商品 (Product 维度):
    │   └── Product: 可口可乐 330ml
    │       └── 直接销售给顾客
    │
    └── 作为库存物品 (Inventory 维度):
        └── Inventory: {material_id=200, quantity=100瓶, shelf_life="12个月"}
```

**验证**：
- 可乐在不同场景中扮演不同角色，但 Material 身份不变
- 作为 Ingredient 时，关注的是 "在 Recipe 中的用量"
- 作为 Product 时，关注的是 "售价和销售"
- 作为 Inventory 时，关注的是 "库存和保质期"

**结论**：Ingredient 是可乐在 Recipe 上下文中的参与方式，不是可乐的身份标签。

### 案例 3: 大米的不同配方用途

**场景**：大米可以用于制作米饭、粥、米粉。

**数据流分析**：

```
Material: 大米 (material_id=300)
    │
    ├── Recipe A: 米饭
    │   └── RecipeComponent: {quantity=200g, loss_rate=0%, unit_cost=30分}
    │
    ├── Recipe B: 白粥
    │   └── RecipeComponent: {quantity=100g, loss_rate=0%, unit_cost=15分}
    │
    └── Recipe C: 米粉
        └── RecipeComponent: {quantity=150g, loss_rate=10%, unit_cost=22分}
```

**验证**：
- 同一袋大米，用于不同菜品时用量不同
- 米饭用 200g，粥用 100g，米粉用 150g
- 损耗率也不同（米粉有 10% 损耗）
- **"Ingredient" 描述的是大米在每个 Recipe 中的精确参与参数**

**结论**：Ingredient 的核心是 RecipeComponent 的属性（quantity, unit, loss_rate, cost）。

### 案例 4: 库存扣减的实际流程

**场景**：顾客点了 2 份可乐鸡翅，厨房需要扣减库存。

**代码流程分析**（基于 `InventoryDeductionServiceImpl.deductByRecipe()`）：

```java
// 1. 根据 dishId 查询所有 RecipeComponent
List<DishRecipe> recipes = dishRecipeMapper.selectList(
    new LambdaQueryWrapper<DishRecipe>().eq(DishRecipe::getDishId, dishId)
);

// 2. 遍历每个 RecipeComponent，计算扣减量
for (DishRecipe recipe : recipes) {
    BigDecimal requiredQty = recipe.getQuantity().multiply(new BigDecimal(quantity));
    String materialName = recipe.getIngredientName();
    // 记录扣减...
}
```

**验证**：
- 代码读取的是 `DishRecipe`（RecipeComponent），不是 `Material` 的 Role
- `recipe.getIngredientName()` 实际返回的是 `material_name`（物料名称）
- 扣减量 = `required_quantity` × 点菜数量
- **"Ingredient" 在代码层面就是 RecipeComponent 的字段名**

**结论**：Ingredient 在代码层面是 RecipeComponent 的属性，不是独立的业务概念。

## 5. 目标技术模型 (TARGET TECHNICAL MODEL)

### 5.1 推荐模型：Ingredient = RecipeComponent

**理由**：
1. **语义准确**：Ingredient 描述的是 "Material 在 Recipe 中的参与方式"
2. **避免概念混淆**：不再将 Ingredient 视为 Material 的 Role
3. **符合代码现实**：当前代码已经是这个模型
4. **支持未来扩展**：RecipeComponent 可以扩展更多属性

### 5.2 技术模型设计

```
Ingredient 核心模型：
┌──────────────────────────────────────────────────────────────┐
│                    RecipeComponent (Ingredient 本质)          │
│                                                              │
│  关联属性：                                                   │
│  - recipe_id (配方ID)                                        │
│  - food_id (菜品ID)                                          │
│  - material_id (物料ID)                                      │
│                                                              │
│  用量属性：                                                   │
│  - required_quantity (标准用量)                               │
│  - unit (单位)                                               │
│  - specification (规格)                                      │
│                                                              │
│  质量属性：                                                   │
│  - loss_rate (损耗率%)                                       │
│  - allow_variance (允许偏差%)                                │
│                                                              │
│  成本属性：                                                   │
│  - unit_cost (原料单价, 分)                                   │
│  - subtotal_cost (该项成本, 分)                               │
│                                                              │
│  管理属性：                                                   │
│  - is_required (是否必需)                                     │
│  - sort_order (排序)                                         │
│  - remark (备注)                                             │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 5.3 与其他概念的关系

```
Ingredient (RecipeComponent) 关系图：

Material (物料) ← 1:N → RecipeComponent (Ingredient)
Recipe (配方) ← 1:N → RecipeComponent (Ingredient)
Food (菜品) ← 1:N → Recipe (通过 food_id)
RecipeComponent → MaterialConsumption (实际消耗记录)
RecipeComponent → InventoryDeduction (库存扣减依据)
```

### 5.4 建议的命名统一

当前系统存在命名混乱，建议统一：

| 当前命名 | 建议命名 | 原因 |
|---------|---------|------|
| `dish_recipe.ingredient_id` | `dish_recipe.material_id` | 与新表和实际语义一致 |
| `dish_recipe.ingredient_name` | `dish_recipe.material_name` | 与新表和实际语义一致 |
| `DishRecipe.ingredientId` | `DishRecipe.materialId` | 与 DishRecipeNew 一致 |
| `DishRecipe.ingredientName` | `DishRecipe.materialName` | 与 DishRecipeNew 一致 |
| "Ingredient"（业务语言） | "RecipeComponent" 或 "原料配方项" | 避免与已废弃概念混淆 |

## 6. 结论

### 6.1 核心问题回答

> **"Ingredient" 究竟是对象的属性、角色还是业务关系中的参与方身份？**

**答案：Ingredient 既不是属性，也不是角色，而是 RecipeComponent 的业务名称——Recipe 与 Material 之间的关系描述。**

| 候选模型 | 判断 | 原因 |
|---------|------|------|
| Material 的属性 | ❌ 不成立 | Ingredient 不在 Material 表中定义 |
| Material 的 Role | ❌ 不成立 | Ingredient 不绑定身份、无生命周期、无权限边界 |
| Recipe 的 RecipeComponent | ✅ 成立 | Ingredient 描述 Material 在 Recipe 中的参与方式 |

### 6.2 为什么不是 Role？

1. **Role 是身份标签，Ingredient 是参与方式**
   - 鸡翅在配方里还是鸡翅，身份没变
   - 变化的是 "鸡翅在可乐鸡翅中用 500g，在香辣鸡翅中用 400g"

2. **Role 有生命周期，Ingredient 没有**
   - 没有 "启用鸡翅作为原料" 的操作
   - 有 "创建 RecipeComponent" 的操作

3. **Role 绑定权限，Ingredient 不绑定**
   - 供应商角色有采购权限
   - Ingredient 不定义任何权限

4. **历史证据**
   - 已废弃的 `ingredient` 表证明：系统曾经尝试将 Ingredient 作为独立实体
   - 最终选择用 `material_archives` 统一管理，证明 Ingredient 不是独立实体

### 6.3 未来演进方向

**短期（当前必需）**：
- 统一命名：将旧表的 `ingredient_id` 重命名为 `material_id`
- 完善 RecipeComponent 属性：补充 `specification`, `allow_variance` 等字段

**中期（按需添加）**：
- Recipe 版本控制：如果需要审计追溯，添加 `version` 字段
- 门店差异：如果需要门店级配方覆盖，添加 `store_recipe` 表

**长期（未来能力）**：
- 替代材料：如果需要支持原料替换，添加 `recipe_alternative` 表
- 配方模板：如果需要批量管理配方，添加 `recipe_template` 表

---

*分析时间：2026-09-09*  
*基于系统当前实现（V1.0.0.100 ~ V6.0.0）*  
*分析结论：Ingredient = RecipeComponent（Recipe 的组成部分）*
