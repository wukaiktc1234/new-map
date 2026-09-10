# Identity / Type / Role / Capability 概念分层分析

> Phase 3-5: 餐饮ERP系统概念边界厘清
> 
> 核心问题："一个东西是什么"与"这个东西在某个业务上下文中做什么"之间的边界

---

## 当前系统现实

### 数据表结构

| 表 | 职责 | 关键字段 |
|---|---|---|
| `foods` | 菜品/食品（可销售） | `id`, `name`, `price`, `category_id` |
| `material_archives` | 商品档案（可采购、可存储） | `id`, `name`, `unit`, `category_id` |
| `inventory` | 库存管理 | `material_id`, `quantity`, `warehouse_id` |
| `dish_recipe` | 菜品配方/BOM | `food_id`, `material_id`, `quantity` |
| `order_items` | 订单明细（销售的对象） | `food_id`, `quantity`, `price` |
| `purchase_order_items` | 采购订单明细（采购的对象） | `material_id`, `quantity`, `price` |

### 现实问题

- `foods` 和 `material_archives` 是**两张独立的表**，没有外键关联
- 同一个商品（如可乐）可能需要在两个表中各维护一次
- `order_items` 只关联 `food_id`，`purchase_order_items` 只关联 `material_id`
- 无法表达"可乐既是可销售商品，又是可采购原料"

---

## Phase 3 - Type 分析

### Type 到底回答什么问题？

**Type 回答的问题是：** "这个对象在分类体系中属于哪一类？"

Type 是**静态分类**，描述对象的本质属性，通常不随业务上下文变化。

### 业务案例验证

| 对象 | Type 分析 | 是否合理？ |
|---|---|---|
| 可乐 | `Type = Beverage` | ✅ 合理 |
| 可乐 | `Type = Packaged Goods` | ⚠️ 冲突 |
| 鸡翅 | `Type = Poultry` | ✅ 合理 |
| 大米 | `Type = Grain` | ✅ 合理 |
| 宫保鸡丁 | `Type = Dish` | ✅ 合理 |
| 矿泉水 | `Type = Beverage` | ✅ 合理 |
| 薯片 | `Type = Snack` | ✅ 合理 |

### 核心问题解答

**Q1: 一个对象是否只能有一个 Type？**

理论上，Type 应该是**单一分类**（Single Classification）。如果可乐同时是 `Beverage` 和 `Packaged Goods`，说明分类体系本身存在问题——这两个 Type 可能不是互斥的分类维度，而是**不同的分类轴**。

解决方案：
- **方案 A：扁平化分类** - 定义明确的分类层级，每个对象只属于一个叶子节点
- **方案 B：多维度分类** - 允许多个分类轴（如：商品类型、包装形式、存储条件）

**Q2: 如果允许 Type = Beverage 且 Type = Packaged Goods？**

这说明 Type 本身需要进一步分类。在餐饮ERP中，建议采用**两级分类**：

```
Level 1: 商品大类（Beverage, Food, Ingredient, Packaging...）
Level 2: 商品小类（Soft Drink, Grain, Meat, Oil...）
```

### Type 的边界

| 属性 | Type 是 | Type 不是 |
|---|---|---|
| 本质 | 静态分类 | 动态状态 |
| 变化频率 | 几乎不变 | 可频繁变化 |
| 业务上下文 | 无关 | 强相关 |
| 示例 | 可乐是饮料 | 可乐是可销售的 |

---

## Phase 4 - Role 分析

### Role 是不是"业务上下文中的行为身份"？

**是的。** Role 回答的问题是："在某个特定业务场景中，这个对象扮演什么角色？"

Role 是**动态身份**，随业务上下文变化。

### 业务案例验证

| 对象 | Role 分析 | 业务上下文 |
|---|---|---|
| 可乐 | `Role = Sellable` | 销售场景 |
| 可乐 | `Role = Purchasable` | 采购场景 |
| 可乐 | `Role = Stockable` | 库存场景 |
| 可乐 | `Role = RecipeComponent` | 配方场景 |
| 鸡翅 | `Role = Purchasable` | 采购场景 |
| 鸡翅 | `Role = Stockable` | 库存场景 |
| 鸡翅 | `Role = RecipeComponent` | 配方场景 |
| 鸡翅 | `Role = Sellable` | ❌ 通常不直接销售 |
| 宫保鸡丁 | `Role = Sellable` | 销售场景 |
| 宫保鸡丁 | `Role = RecipeComponent` | ❌ 宫保鸡丁本身不是原料 |
| 薯片 | `Role = Sellable` | 销售场景 |
| 薯片 | `Role = Purchasable` | 采购场景 |
| 薯片 | `Role = Stockable` | 库存场景 |

### 核心问题解答

**Q1: 一个对象是否能同时拥有多个 Role？**

**是的。** 这正是 Role 的核心价值。可乐可以同时是：
- `Sellable`（可销售）
- `Purchasable`（可采购）
- `Stockable`（可存储）
- `RecipeComponent`（可作为配方成分）

**Q2: Role 是否可以随时间变化？**

**是的。** 这是 Role 与 Type 的关键区别：
- 今天不销售的商品，明天可能上架销售
- 某个原料暂时不采购，但库存还有
- 新菜品加入菜单，旧菜品下架

**Q3: Role 是否可以按门店不同？**

**是的。** 这是餐饮连锁的关键场景：
- 门店 A 的菜单上有可乐，门店 B 没有
- 某个原料只在中央厨房采购，不在门店采购
- 某个菜品只在特定门店销售

**Q4: Role 是否可以按仓库不同？**

**是的。** 这是库存管理的关键场景：
- 仓库 A 存储可乐，仓库 B 不存储
- 某个原料只在中央仓库存储
- 冷冻仓库存储肉类，常温仓库存储干货

**Q5: Role 是否可以独立启用/停用？**

**是的。** 这是 Role 与 Capability 的关键区别：
- Role 可以有时间范围（有效期）
- Role 可以有条件触发（库存低于阈值时启用）
- Role 可以有组织范围（只在特定门店生效）

### Role 的边界

| 属性 | Role 是 | Role 不是 |
|---|---|---|
| 本质 | 动态身份 | 静态分类 |
| 变化频率 | 可频繁变化 | 几乎不变 |
| 业务上下文 | 强相关 | 无关 |
| 示例 | 可乐是可销售的 | 可乐是饮料 |

---

## Phase 5 - Capability 分析

### SELLABLE / STOCKABLE / PURCHASABLE 到底应该是 Role，还是 Capability？

这是最核心的问题。需要比较三种模型：

### 模型比较

#### 模型 A: ROLE_MODEL

```typescript
enum Role {
  SELLABLE = "SELLABLE",
  STOCKABLE = "STOCKABLE",
  PURCHASABLE = "PURCHASABLE",
  RECIPE_COMPONENT = "RECIPE_COMPONENT",
}

interface BusinessItem {
  id: string;
  name: string;
  roles: Role[];
}
```

**优点：**
- 简单直接
- 符合现有系统结构

**缺点：**
- Role 与 Capability 混为一谈
- 无法表达能力的细粒度差异
- 无法表达能力的条件限制

#### 模型 B: CAPABILITY_MODEL

```typescript
enum Capability {
  CAN_SELL = "CAN_SELL",
  CAN_PURCHASE = "CAN_PURCHASE",
  CAN_STOCK = "CAN_STOCK",
  CAN_BE_RECIPE_COMPONENT = "CAN_BE_RECIPE_COMPONENT",
}

interface BusinessItem {
  id: string;
  name: string;
  capabilities: Capability[];
}
```

**优点：**
- 清晰表达对象的能力
- 符合领域驱动设计原则

**缺点：**
- 过于抽象
- 无法表达业务上下文
- 无法表达时间、组织、仓库维度

#### 模型 C: HYBRID_MODEL（推荐）

```typescript
interface BusinessItem {
  id: string;
  name: string;
  type: ItemType; // 静态分类
  capabilities: Capability[]; // 核心能力
  roles: Role[]; // 业务上下文中的角色
}

enum ItemType {
  BEVERAGE = "BEVERAGE",
  FOOD = "FOOD",
  INGREDIENT = "INGREDIENT",
  PACKAGING = "PACKAGING",
}

enum Capability {
  CAN_SELL = "CAN_SELL",
  CAN_PURCHASE = "CAN_PURCHASE",
  CAN_STOCK = "CAN_STOCK",
  CAN_BE_RECIPE_COMPONENT = "CAN_BE_RECIPE_COMPONENT",
}

interface Role {
  capability: Capability;
  context: RoleContext; // 业务上下文
  enabled: boolean; // 是否启用
  startTime?: Date; // 开始时间
  endTime?: Date; // 结束时间
}

interface RoleContext {
  storeId?: string; // 门店
  warehouseId?: string; // 仓库
  supplierId?: string; // 供应商
}
```

**优点：**
- 清晰区分 Type、Role、Capability
- 支持业务上下文
- 支持时间、组织、仓库维度
- 符合餐饮连锁的复杂场景

**缺点：**
- 复杂度较高
- 需要更多的表和关联

### 业务案例验证（HYBRID_MODEL）

#### 可乐

```typescript
{
  id: "cola-001",
  name: "可口可乐 330ml",
  type: ItemType.BEVERAGE,
  capabilities: [
    Capability.CAN_SELL,
    Capability.CAN_PURCHASE,
    Capability.CAN_STOCK,
    Capability.CAN_BE_RECIPE_COMPONENT,
  ],
  roles: [
    {
      capability: Capability.CAN_SELL,
      context: { storeId: "store-001" },
      enabled: true,
    },
    {
      capability: Capability.CAN_PURCHASE,
      context: { warehouseId: "warehouse-001" },
      enabled: true,
    },
    {
      capability: Capability.CAN_STOCK,
      context: { warehouseId: "warehouse-001" },
      enabled: true,
    },
    {
      capability: Capability.CAN_BE_RECIPE_COMPONENT,
      context: {}, // 全局生效
      enabled: true,
    },
  ],
}
```

#### 鸡翅

```typescript
{
  id: "chicken-wing-001",
  name: "鸡翅中",
  type: ItemType.INGREDIENT,
  capabilities: [
    Capability.CAN_PURCHASE,
    Capability.CAN_STOCK,
    Capability.CAN_BE_RECIPE_COMPONENT,
    // 注意：没有 CAN_SELL
  ],
  roles: [
    {
      capability: Capability.CAN_PURCHASE,
      context: { warehouseId: "warehouse-001" },
      enabled: true,
    },
    {
      capability: Capability.CAN_STOCK,
      context: { warehouseId: "warehouse-001" },
      enabled: true,
    },
    {
      capability: Capability.CAN_BE_RECIPE_COMPONENT,
      context: {},
      enabled: true,
    },
  ],
}
```

#### 宫保鸡丁

```typescript
{
  id: "kung-pao-chicken-001",
  name: "宫保鸡丁",
  type: ItemType.FOOD,
  capabilities: [
    Capability.CAN_SELL,
    // 注意：没有 CAN_PURCHASE, CAN_STOCK, CAN_BE_RECIPE_COMPONENT
  ],
  roles: [
    {
      capability: Capability.CAN_SELL,
      context: { storeId: "store-001" },
      enabled: true,
    },
  ],
}
```

---

## 三种模型的对比总结

| 维度 | ROLE_MODEL | CAPABILITY_MODEL | HYBRID_MODEL |
|---|---|---|---|
| Type 分类 | ❌ 缺失 | ❌ 缺失 | ✅ 完整 |
| Role 支持 | ✅ 基本 | ❌ 缺失 | ✅ 完整 |
| Capability 支持 | ⚠️ 混淆 | ✅ 基本 | ✅ 完整 |
| 业务上下文 | ❌ 缺失 | ❌ 缺失 | ✅ 完整 |
| 时间维度 | ❌ 缺失 | ❌ 缺失 | ✅ 完整 |
| 组织维度 | ❌ 缺失 | ❌ 缺失 | ✅ 完整 |
| 复杂度 | 低 | 低 | 高 |

---

## 最终结论

### 推荐模型：HYBRID_MODEL

**理由：**
1. **Type 是静态分类**，描述对象的本质属性，不随业务上下文变化
2. **Capability 是核心能力**，描述对象能做什么，是 Role 的基础
3. **Role 是动态身份**，描述对象在特定业务上下文中扮演什么角色，支持时间、组织、仓库维度

### 关键原则

1. **不得把 Type、Role、Capability、Relationship 混成同一个枚举**
2. **必须严格区分 CURRENT REALITY / TARGET BUSINESS SEMANTICS / TARGET TECHNICAL MODEL**
3. **必须证明为什么正确、在哪里不正确**
4. **允许最终结论不是 Canonical Identity + Role**

### 当前系统的问题

| 问题 | 影响 | 解决方案 |
|---|---|---|
| `foods` 和 `material_archives` 分离 | 同一商品需维护两次 | 统一为 `business_items` 表 |
| `order_items` 只关联 `food_id` | 无法销售原料 | 扩展为 `business_item_id` |
| `purchase_order_items` 只关联 `material_id` | 无法采购成品 | 扩展为 `business_item_id` |
| 缺少 Role 维度 | 无法按门店/仓库区分角色 | 新增 `business_item_roles` 表 |

### 实施路径

1. **Phase 1: 数据模型重构** - 合并 `foods` 和 `material_archives` 为 `business_items`
2. **Phase 2: 引入 Type** - 定义商品分类体系
3. **Phase 3: 引入 Capability** - 定义核心能力枚举
4. **Phase 4: 引入 Role** - 定义业务上下文中的角色
5. **Phase 5: 迁移数据** - 将现有数据迁移到新模型

---

## 附录：概念定义

### Identity（身份）

**定义：** 一个对象的唯一标识，回答"这个东西是谁？"

**示例：**
- 可乐的 Identity 是 `cola-001`
- 鸡翅的 Identity 是 `chicken-wing-001`

### Type（类型）

**定义：** 一个对象的静态分类，回答"这个东西是什么？"

**示例：**
- 可乐的 Type 是 `BEVERAGE`
- 鸡翅的 Type 是 `INGREDIENT`

### Role（角色）

**定义：** 一个对象在特定业务上下文中的行为身份，回答"这个东西在某个场景中做什么？"

**示例：**
- 可乐在门店 A 的 Role 是 `Sellable`
- 鸡翅在仓库 B 的 Role 是 `Stockable`

### Capability（能力）

**定义：** 一个对象的核心能力，回答"这个东西能做什么？"

**示例：**
- 可乐的 Capability 包括 `CAN_SELL`, `CAN_PURCHASE`, `CAN_STOCK`
- 鸡翅的 Capability 包括 `CAN_PURCHASE`, `CAN_STOCK`, `CAN_BE_RECIPE_COMPONENT`
