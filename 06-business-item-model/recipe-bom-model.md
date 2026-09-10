# 配方/BOM模型分析

## 1. 当前系统现实

### 1.1 核心表结构

```
dish_recipe (菜品配方/BOM)
├── id (PK)
├── recipe_id (配方编号)
├── dish_id (菜品ID)        ← 关联 foods.food_id
├── dish_name (菜品名称)
├── material_id (物料ID)    ← 关联物料系统
├── material_name (物料名称)
├── quantity (用量)
├── unit (单位)
├── is_required (是否必需)
├── estimated_cost (预估成本)  ← V6.0.0 新增
├── actual_cost (实际成本)     ← V6.0.0 新增
├── allow_variance (允许偏差)  ← V6.0.0 新增
└── loss_rate (损耗率)         ← V6.0.0 新增
```

### 1.2 关键发现

1. **Recipe 连接 Food 和 Material**：dish_id → foods, material_id → 物料系统
2. **Recipe 是多对多关系**：一个 Food 有多个 Material，一个 Material 可用于多个 Food
3. **Recipe 有成本属性**：estimated_cost, actual_cost, loss_rate

## 2. Recipe 是否独立业务对象？

### 2.1 当前实现分析

**答案：Recipe 当前不是独立业务对象，而是 Food 的附属属性**

原因：
1. Recipe 没有独立的生命周期管理
2. Recipe 没有独立的状态字段
3. Recipe 的主键是自增 ID，不是业务编号
4. Recipe 的 CRUD 是通过 Food 管理页面进行的

### 2.2 与独立业务对象的对比

| 特征 | 独立业务对象 | 当前 Recipe |
|------|-------------|-------------|
| 独立主键 | ✅ 业务编号 | ❌ 自增 ID |
| 独立状态 | ✅ active/inactive | ❌ 无状态 |
| 独立 CRUD | ✅ 独立管理页面 | ❌ 依附于 Food |
| 独立版本 | ✅ 版本控制 | ❌ 无版本 |
| 独立门店差异 | ✅ 门店级配置 | ❌ 全局统一 |

### 2.3 结论

**Recipe 当前是 Food 的"配方属性"，不是独立业务对象**

但这不一定是坏事：
- 简化了模型
- 减少了管理复杂度
- 适合当前业务规模

## 3. Recipe 是否支持版本？

### 3.1 当前实现分析

**答案：不支持版本**

证据：
1. dish_recipe 表没有 version 字段（虽然有 version 列，但那是 ORM 的乐观锁字段）
2. 修改配方是直接更新，没有历史记录
3. 没有"生效时间"、"失效时间"等版本控制字段

### 3.2 版本需求分析

**是否需要版本？**

| 场景 | 需要版本？ | 原因 |
|------|-----------|------|
| 日常配方调整 | ❌ 不需要 | 直接修改即可 |
| 成本核算追溯 | ⚠️ 可能需要 | 需要知道历史配方 |
| 食品安全审计 | ⚠️ 可能需要 | 需要追溯配料变化 |
| 季节性配方变化 | ⚠️ 可能需要 | 不同季节不同配方 |

### 3.3 建议

**短期：不支持版本**（当前实现足够）
**长期：如果需要审计追溯，可以考虑添加版本**

## 4. Recipe 是否门店差异？

### 4.1 当前实现分析

**答案：不支持门店差异**

证据：
1. dish_recipe 表没有 store_id 字段
2. 所有门店共享同一套配方
3. 修改配方会影响所有门店

### 4.2 门店差异需求分析

**是否需要门店差异？**

| 场景 | 需要门店差异？ | 原因 |
|------|---------------|------|
| 统一标准化运营 | ❌ 不需要 | 所有门店保持一致 |
| 区域口味差异 | ⚠️ 可能需要 | 不同地区口味不同 |
| 供应链差异 | ⚠️ 可能需要 | 不同门店用不同供应商 |
| 成本差异 | ⚠️ 可能需要 | 不同门店成本不同 |

### 4.3 建议

**当前：不支持门店差异**（简化管理）
**扩展方案：如果需要，可以添加 store_recipe 表实现门店级配方覆盖**

```
dish_recipe (全局默认配方)
    │
    │ store_id (门店ID)
    │
    ▼
store_recipe (门店级配方覆盖)
├── store_id
├── food_id
├── material_id
├── quantity (门店级用量)
├── cost_price (门店级成本)
└── ...
```

## 5. Recipe 是否支持替代材料？

### 5.1 当前实现分析

**答案：不支持替代材料**

证据：
1. dish_recipe 表没有 alternative_material_id 字段
2. 没有替代材料的配置机制
3. 库存扣减只按固定配方执行

### 5.2 替代材料需求分析

**是否需要替代材料？**

| 场景 | 需要替代材料？ | 原因 |
|------|---------------|------|
| 原料缺货 | ⚠️ 可能需要 | 临时替换 |
| 成本优化 | ⚠️ 可能需要 | 用更便宜的替代品 |
| 口味调整 | ⚠️ 可能需要 | 提供不同选择 |

### 5.3 建议

**短期：不支持替代材料**（通过库存预警解决缺货问题）
**长期：如果需要，可以添加 recipe_alternative 表**

```
recipe_alternative (配方替代材料)
├── recipe_id (原配方ID)
├── original_material_id (原物料ID)
├── alternative_material_id (替代物料ID)
├── priority (优先级)
├── quantity_ratio (用量比例)
├── cost_ratio (成本比例)
└── effective_date / expiry_date (有效期)
```

## 6. Recipe 与成本核算

### 6.1 Recipe 的成本属性

```
dish_recipe 成本字段：
├── estimated_cost (预估成本)
├── actual_cost (实际成本)
├── loss_rate (损耗率 %)
└── allow_variance (允许偏差 %)
```

### 6.2 成本计算逻辑

```
Food 成本 = Σ (Material 用量 × Material 单价) × (1 + 损耗率)

示例：
红烧肉 Recipe：
├── 猪肉 500g × ¥20/kg = ¥10
├── 酱油 50ml × ¥0.1/ml = ¥5
├── 糖 20g × ¥0.05/g = ¥1
└── 损耗率 5%

总成本 = (10 + 5 + 1) × 1.05 = ¥16.8
```

### 6.3 实际成本 vs 预估成本

```
实际成本 = Σ (实际消耗量 × 实际采购价)

差异原因：
├── 实际用量 vs 标准用量
├── 实际采购价 vs 标准成本价
├── 实际损耗 vs 预估损耗
└── 批次差异
```

## 7. Recipe 数据模型设计

### 7.1 当前模型（简化版）

```typescript
interface DishRecipe {
  id: number;
  recipeId: string;      // 配方编号
  dishId: number;        // 菜品ID
  dishName: string;      // 菜品名称
  materialId: number;    // 物料ID
  materialName: string;  // 物料名称
  quantity: number;      // 用量
  unit: string;          // 单位
  isRequired: boolean;   // 是否必需
}
```

### 7.2 增强模型（支持成本核算）

```typescript
interface DishRecipeEnhanced {
  // 基础信息
  id: number;
  recipeId: string;
  dishId: number;
  materialId: number;
  
  // 用量信息
  quantity: number;
  unit: string;
  isRequired: boolean;
  
  // 成本信息
  estimatedCost: number;    // 预估成本（分）
  actualCost: number;       // 实际成本（分）
  lossRate: number;         // 损耗率（%）
  allowVariance: number;    // 允许偏差（%）
  
  // 供应链信息
  supplierId?: number;      // 供应商ID
  batchNo?: string;         // 批次号
  shelfLifeDays?: number;   // 保质期（天）
}
```

### 7.3 扩展模型（支持门店差异）

```typescript
interface DishRecipeWithStore {
  // 基础配方
  baseRecipe: DishRecipe;
  
  // 门店覆盖
  storeOverrides?: {
    storeId: number;
    quantity?: number;      // 门店级用量
    costPrice?: number;     // 门店级成本
    supplierId?: number;    // 门店级供应商
    effectiveDate: Date;    // 生效日期
    expiryDate?: Date;      // 失效日期
  }[];
}
```

## 8. Recipe 管理最佳实践

### 8.1 配方创建流程

```
1. 创建 Food
   └── foods 表插入记录
   
2. 定义 Recipe
   └── dish_recipe 表插入多条记录
   └── 每条记录对应一个 Material
   
3. 设置成本
   └── 填写 estimated_cost
   └── 设置 loss_rate
   
4. 测试验证
   └── 模拟库存扣减
   └── 验证成本计算
   
5. 启用配方
   └── Food.status = 1 (在售)
```

### 8.2 配方修改流程

```
1. 评估影响
   └── 检查当前库存
   └── 检查进行中的订单
   
2. 修改 Recipe
   └── 更新 dish_recipe 记录
   
3. 重新计算成本
   └── 更新 Food.cost_price
   
4. 通知相关门店
   └── 如果有门店差异
   
5. 审计记录
   └── 记录修改历史（如果需要）
```

### 8.3 配方删除流程

```
1. 检查依赖
   └── 是否有进行中的订单
   └── 是否有库存
   
2. 软删除
   └── dish_recipe.deleted = 1
   
3. 更新 Food 状态
   └── 如果是唯一配方，Food.status = 2 (停售)
```

## 9. 总结

| 问题 | 答案 |
|------|------|
| Recipe 是否独立业务对象？ | **否**，当前是 Food 的附属属性 |
| Recipe 是否支持版本？ | **否**，修改直接覆盖 |
| Recipe 是否门店差异？ | **否**，全局统一配方 |
| Recipe 是否支持替代材料？ | **否**，按固定配方执行 |
| Recipe 的核心作用？ | **连接 Food 和 Material**，支撑成本核算和库存扣减 |

### 设计建议

1. **保持当前简化模型**：适合当前业务规模
2. **预留扩展点**：成本字段、损耗率等已经为未来扩展做好准备
3. **按需添加功能**：版本、门店差异、替代材料等功能，等业务需要时再添加

---

*分析时间：2026-09-09*
*基于系统当前实现*
