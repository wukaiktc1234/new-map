# Phase 21: Model Stress Points Analysis

> **核心问题**：当前数据模型在哪些地方被迫"打补丁"？这些补丁是孤立问题还是系统性模型缺陷的症状？
> **状态**：ANALYSIS, NOT CONFIRMED
> **约束**：使用真实业务案例验证每个压力点，证明为什么正确、在哪里不正确

---

## 一、压力点识别方法论

### 1.1 什么是 MODEL_STRESS_POINT

当业务需求与数据模型之间存在张力时，开发人员被迫在模型上"打补丁"——添加特殊字段、编写特殊判断逻辑、建立非自然的桥接关系。这些补丁不是孤立的 Bug，而是**模型缺陷的外在症状**。

### 1.2 六种压力点类型

| 类型 | 定义 | 信号 |
|------|------|------|
| **Identity Stress** | 同一对象在不同域有不同身份 | 需要维护两份数据，或需要编码匹配 |
| **Role Conflict** | 同一对象需要同时扮演多个冲突角色 | 同一记录必须同时满足多种互斥的业务逻辑 |
| **Scope Mismatch** | 全局配置与门店差异冲突 | 需要在全局表上叠加门店级别的覆盖逻辑 |
| **Relationship Complexity** | 跨域关系无法自然表达 | 需要隐式关联（编码匹配、名称匹配）而非显式外键 |
| **Data Duplication** | 同一数据需要在多处维护 | 修改一处时需要同步修改其他地方 |
| **Type Ambiguity** | 对象类型不明确 | 同一表承载多种语义完全不同的实体 |

### 1.3 分析结构

每个压力点按以下结构分析：
1. **压力描述**：模型在哪个地方被迫打补丁
2. **补丁代码**：当前系统中为了绕过这个问题而写的具体代码/SQL
3. **业务案例**：用真实场景证明这个压力点确实存在
4. **根因分析**：为什么模型在这里不正确
5. **影响评估**：这个问题影响了多少业务流程
6. **修复方向**：正确的模型应该是什么

---

## 二、Identity Stress: foods 与 material_archives 的身份分裂

### 2.1 压力描述

同一个物理商品（如"可口可乐 330ml"）在系统中必须同时存在于 `foods` 和 `material_archives` 两张独立的表中，且两张表之间**没有外键关联**。

```
可口可乐 330ml
├── foods 表:          food_id=F001, food_name="可乐", food_price=3.00
└── material_archives: material_id=M001, material_name="可口可乐330ml", reference_price=2.50
```

**补丁代码**：

```sql
-- 跨域查询时被迫使用编码匹配（不可靠）
SELECT f.food_id, f.food_name, m.material_id, m.reference_price
FROM foods f
LEFT JOIN material_archives m ON f.food_code = m.material_code  -- 隐式关联，无外键约束
WHERE f.food_name LIKE '%可乐%';

-- 或者更糟：使用名称模糊匹配
SELECT * FROM foods f
JOIN material_archives m ON f.food_name LIKE CONCAT('%', m.material_name, '%');
```

### 2.2 业务案例验证

**案例：可乐鸡翅的完整生命周期**

| 阶段 | 使用的表 | 使用的ID | 问题 |
|------|----------|----------|------|
| 采购入库 | purchase_order_items → material_archives | material_id=M001 | ✅ 正常 |
| 仓库存储 | inventory | product_id=M001 (遗留) | ⚠️ 字段名误导 |
| 门店调拨 | store_inventory | material_id=M001 | ✅ 正常 |
| 菜品配方 | dish_recipe | ingredient_id=M001 | ✅ 正常 |
| POS销售 | order_items | food_id=F001 | ⚠️ 与 M001 无关联 |
| 成本核算 | ??? | ??? | ❌ 无法自动关联 |

**关键问题**：当门店售出一份"可乐鸡翅"时：
1. `order_items` 记录 `food_id=F001`（鸡翅作为菜品）
2. `dish_recipe` 定义 `ingredient_id=M001`（可乐作为原料）
3. 但系统**无法自动**从 `food_id=F001` 关联到 `material_id=M001`
4. 库存扣减需要**手动编写映射逻辑**

### 2.3 根因分析

**为什么 foods 和 material_archives 必须分离？**

从模型角度看，它们不应该分离。一个"可乐"同时具有：
- **Commercial 属性**：售价、菜单配置、POS 显示（foods 的职责）
- **Operational 属性**：采购价、供应商、保质期（material_archives 的职责）

当前模型的错误在于：**将"商业定义"和"操作定义"建模为两个独立实体**，而不是同一个 Canonical Item 的两个 Profile。

### 2.4 影响评估

| 受影响的业务流程 | 影响程度 | 具体表现 |
|-----------------|----------|----------|
| 成本核算 | 🔴 严重 | 无法自动从销售追溯到原料成本 |
| 库存扣减 | 🔴 严重 | 需要手动维护 foods↔material_archives 映射 |
| 报表统计 | 🟡 中等 | 跨域报表需要复杂的 JOIN 逻辑 |
| 数据维护 | 🟡 中等 | 新增商品需要在两张表中各维护一次 |
| 门店差异 | 🟡 中等 | 不同门店可能使用不同的 food_id/material_id 映射 |

### 2.5 修复方向

**正确的模型**：引入 Canonical Item + Profile

```
Canonical Item: 可口可乐330ml (C001)
├── Commercial Profile:    price=3.00, menu_config={...}
├── Procurement Profile:   supplier=S001, reference_price=2.50
├── Inventory Profile:     safety_stock=50, shelf_life=365
└── Recipe Profile:        used_in=[可乐鸡翅]
```

**当前系统 vs 目标模型**：

| 当前表 | 目标映射 | 关键变化 |
|--------|----------|----------|
| foods | Canonical Item + Commercial Profile | 合并 |
| material_archives | Canonical Item + Procurement Profile | 合并 |
| inventory | Canonical Item + Inventory Profile | 统一位置模型 |
| dish_recipe | Canonical Item + Recipe Profile | 统一引用 |

---

## 三、Role Conflict: 可乐必须同时扮演四种角色

### 3.1 压力描述

同一个对象（可乐）必须同时承担 SELLABLE、PURCHASABLE、STOCKABLE、INGREDIENT 四种角色，但系统中**没有显式的 Role 模型**，导致角色信息分散在多张表中。

```
可口可乐 330ml 的角色矩阵：
├── SELLABLE:     foods.food_id=F001           → 售价=3.00
├── PURCHASABLE:  purchase_order_items.material_id=M001 → 采购价=2.50
├── STOCKABLE:    inventory.product_id=M001    → 库存=200罐
├── STORE-STOCK:  store_inventory.material_id=M001 → 门店库存=20罐
└── INGREDIENT:   dish_recipe.ingredient_id=M001 → 用于可乐鸡翅
```

**补丁代码**：

```java
// 当判断一个商品是否"可销售"时，需要检查 foods 表
public boolean isSellable(Long materialId) {
    // 通过编码匹配查找对应的 food_id
    String foodCode = getFoodCodeByMaterialId(materialId);
    return foodCode != null && !foodCode.isEmpty();
}

// 当判断一个商品是否"可采购"时，需要检查 material_archives 表
public boolean isPurchasable(Long foodId) {
    // 通过编码匹配查找对应的 material_id
    String materialCode = getMaterialCodeByFoodId(foodId);
    return materialCode != null && !materialCode.isEmpty();
}

// 当判断一个商品是否"可库存"时，需要检查 inventory 表
public boolean isStockable(Long materialId) {
    return inventoryMapper.existsByMaterialId(materialId);
}
```

### 3.2 业务案例验证

**案例：宫保鸡丁 vs 可乐的角色差异**

| 角色 | 宫保鸡丁 | 可乐 | 矿泉水 | 鸡胸肉 |
|------|---------|------|--------|--------|
| SELLABLE | ✅ food_id=F010 | ✅ food_id=F001 | ✅ food_id=F002 | ❌ 通常不销售 |
| PURCHASABLE | ❌ 不采购成品 | ✅ material_id=M001 | ✅ material_id=M002 | ✅ material_id=M010 |
| STOCKABLE | ❌ 不存储成品 | ✅ inventory(M001) | ✅ inventory(M002) | ✅ inventory(M010) |
| INGREDIENT | ❌ 通常不作为原料 | ✅ 可乐鸡翅 | ❌ 几乎不用 | ✅ 宫保鸡丁等 |

**关键问题**：
1. 宫保鸡丁只有 SELLABLE 角色，但 foods 表仍然存在它的记录
2. 鸡胸肉有 PURCHASABLE、STOCKABLE、INGREDIENT 角色，但没有 SELLABLE 角色
3. 系统**无法统一查询**一个商品的所有角色
4. 角色的**激活/停用**没有统一机制

### 3.3 根因分析

当前模型将"角色"隐式地编码在表结构中：
- `foods` 表 = SELLABLE 角色的载体
- `material_archives` 表 = PURCHASABLE 角色的载体
- `inventory` 表 = STOCKABLE 角色的载体
- `dish_recipe` 表 = INGREDIENT 角色的载体

**问题**：角色与表结构**硬耦合**，导致：
1. 新增角色需要新建表
2. 同一对象的多角色信息分散在多处
3. 无法统一管理角色的生命周期

### 3.4 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 商品上架 | 🔴 严重 | 需要同时维护 foods 和 material_archives |
| 角色切换 | 🔴 严重 | 无法统一控制角色的激活/停用 |
| 跨域查询 | 🟡 中等 | 查询"可乐的所有角色"需要多次查询 |
| 新业务扩展 | 🟡 中等 | 新增角色（如 RETURNABLE）需要新建表 |

### 3.5 修复方向

**正确的模型**：显式 Role 实体

```sql
CREATE TABLE business_role (
    id                  BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,
    role_type           VARCHAR(50) NOT NULL,  -- SELLABLE, PURCHASABLE, STOCKABLE, INGREDIENT
    domain_config       JSONB,                 -- 角色特定配置
    store_id            BIGINT,                -- 可选：门店级别角色
    status              VARCHAR(20) DEFAULT 'ACTIVE',
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP
);
```

---

## 四、Scope Mismatch: 全局配置与门店差异的冲突

### 4.1 压力描述

系统需要同时支持"集团标准"和"门店定制"两种模式，但当前模型中**没有门店级别的配置覆盖机制**。

```
理想场景：
集团标准: 可乐售价=3.00
门店A:    可乐售价=3.50 (高端商圈)
门店B:    可乐售价=2.80 (学校附近)

当前现实：
foods 表只有 一个 food_price，无法支持门店差异
```

**补丁代码**：

```sql
-- 方法1：在 foods 表中添加 store_id（但 foods 表设计时没有考虑门店）
ALTER TABLE foods ADD COLUMN store_id BIGINT;
-- 问题：每个门店需要一条独立的 foods 记录，数据爆炸

-- 方法2：创建单独的价格表
CREATE TABLE store_food_price (
    store_id BIGINT,
    food_id BIGINT,
    price DECIMAL(10,2)
);
-- 问题：又一个补丁表，与 foods.price 的关系不明确

-- 方法3：使用配置表覆盖
CREATE TABLE store_config_override (
    store_id BIGINT,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    config_key VARCHAR(100),
    config_value TEXT
);
-- 问题：过于通用，失去了类型安全性
```

### 4.2 业务案例验证

**案例：连锁餐厅的菜品管理**

| 配置项 | 集团标准 | 门店A | 门店B | 冲突程度 |
|--------|---------|-------|-------|---------|
| 菜品售价 | 3.00 | 3.50 | 2.80 | 🔴 高 |
| 菜品配方 | 标准版 | 微辣版 | 标准版 | 🔴 高 |
| 库存安全值 | 50 | 80 | 30 | 🟡 中 |
| 供应商 | 供应商A | 供应商A | 供应商B | 🟡 中 |
| 上架状态 | 上架 | 上架 | 下架 | 🔴 高 |

**关键问题**：
1. 一个"可乐"在门店A和门店B可能有不同的价格，但 foods 表只支持一个 price
2. 一个"麻婆豆腐"在四川门店需要更辣，但 dish_recipe 只有一个标准配方
3. 门店下架某个菜品时，需要在 foods 表中标记，但这是全局操作还是门店操作？

### 4.3 根因分析

当前模型是**全局单例模型**：每个商品/菜品在系统中只有一个记录，不支持门店级别的差异化配置。

**补丁模式**：通过创建多个记录来模拟门店差异

```
foods 表：
├── food_id=F001, food_name="可乐", store_id=NULL     → 集团标准
├── food_id=F002, food_name="可乐", store_id=S01      → 门店A专用
└── food_id=F003, food_name="可乐", store_id=S02      → 门店B专用

问题：
1. F001、F002、F003 代表同一个菜品，但有三个不同的 food_id
2. 修改"可乐"的基本信息时，需要同步修改三条记录
3. 统计"可乐"的总销量时，需要聚合三条记录
```

### 4.4 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 菜品定价 | 🔴 严重 | 无法统一管理菜品价格 |
| 配方管理 | 🔴 严重 | 无法支持区域口味差异 |
| 库存管理 | 🟡 中等 | 安全库存需要门店级别配置 |
| 报表统计 | 🔴 严重 | 需要区分集团数据和门店数据 |
| 数据维护 | 🔴 严重 | 修改菜品信息需要同步多条记录 |

### 4.5 修复方向

**正确的模型**：Profile + Scope 覆盖

```
Canonical Item: 可口可乐330ml
├── Global Commercial Profile:    price=3.00 (集团标准)
├── Store A Commercial Profile:   price=3.50 (覆盖)
├── Store B Commercial Profile:   price=2.80 (覆盖)
└── Store B Commercial Profile:   status=INACTIVE (下架)
```

---

## 五、Relationship Complexity: foods 与 material_archives 的隐式关联

### 5.1 压力描述

foods 和 material_archives 之间**没有外键关联**，系统被迫使用编码匹配或名称匹配来建立隐式关联。

```
当前关联方式：
foods.food_code ←──?──→ material_archives.material_code
   (编码匹配，无约束)

问题：
1. 编码可能不一致（COLA001 vs MAT_COLA001）
2. 编码可能重复（不同门店使用不同编码）
3. 编码可能被修改
4. 无外键约束保证数据完整性
```

**补丁代码**：

```sql
-- 方案1：创建桥接表
CREATE TABLE food_material_mapping (
    food_id BIGINT,
    material_id BIGINT,
    mapping_type VARCHAR(20),  -- 'exact', 'approximate', 'substitute'
    confidence DECIMAL(3,2)    -- 匹配置信度
);
-- 问题：需要人工维护映射关系

-- 方案2：使用编码匹配
SELECT f.*, m.*
FROM foods f
JOIN material_archives m ON f.food_code = m.material_code;
-- 问题：编码可能不一致

-- 方案3：使用名称模糊匹配
SELECT f.*, m.*
FROM foods f
JOIN material_archives m ON f.food_name LIKE CONCAT('%', m.material_name, '%');
-- 问题：性能差，匹配不准确
```

### 5.2 业务案例验证

**案例：可乐在不同系统中的编码**

| 系统 | 编码 | 名称 | 问题 |
|------|------|------|------|
| POS系统 | COLA001 | 可乐 | food_code |
| 采购系统 | MAT_COLA001 | 可口可乐330ml | material_code |
| 供应商目录 | COCA-COLA-330 | Coca-Cola 330ml | product_code |
| 库存系统 | M001 | 可口可乐 | material_id |

**关键问题**：
1. 四个系统使用四种不同的编码，无法自动关联
2. 名称也不一致："可乐" vs "可口可乐330ml" vs "Coca-Cola 330ml"
3. 跨系统报表需要手动编写映射逻辑

### 5.3 根因分析

foods 和 material_archives 是**两个独立的主数据域**，它们之间没有设计时的关联关系。这种设计假设：
- foods 只用于销售
- material_archives 只用于采购和库存
- 两者不需要关联

**现实**：同一个商品（可乐）既需要销售（foods），又需要采购和库存（material_archives），所以**必须关联**。

### 5.4 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 成本核算 | 🔴 严重 | 无法从销售自动追溯到原料成本 |
| 库存扣减 | 🔴 严重 | 销售后无法自动扣减原料库存 |
| 数据一致性 | 🔴 严重 | 两表数据可能不一致 |
| 报表统计 | 🟡 中等 | 跨域报表需要复杂映射逻辑 |

### 5.5 修复方向

**正确的模型**：统一 Canonical Item

foods 和 material_archives 不应该存在关联关系，因为它们**应该是同一个实体的两个 Profile**。

```
当前：foods ←──?──→ material_archives (隐式关联)
目标：canonical_item → commercial_profile + procurement_profile (显式关联)
```

---

## 六、Data Duplication: 同一数据在多处维护

### 6.1 压力描述

同一个商品的基本信息（名称、单位、规格）需要在多张表中重复存储，导致数据不一致。

```
可口可乐 330ml 的数据分布：
├── foods 表:
│   ├── food_name = "可乐"
│   ├── unit = "瓶"
│   └── spec = NULL
├── material_archives 表:
│   ├── material_name = "可口可乐330ml"
│   ├── unit = "罐"          ← 与 foods 不一致！
│   └── spec = "330ml"
├── inventory 表:
│   ├── unit = "瓶"          ← 与 material_archives 不一致！
│   └── ...
└── store_inventory 表:
    ├── unit = "罐"          ← 又不一致！
    └── ...
```

**补丁代码**：

```sql
-- 数据同步任务（定期修复不一致）
UPDATE inventory i
JOIN material_archives m ON i.product_id = m.material_id
SET i.unit = m.unit
WHERE i.unit != m.unit;

-- 更糟：手动修复不一致数据
-- "可乐在 inventory 中是'瓶'，在 store_inventory 中是'罐'，需要统一为'罐'"
UPDATE store_inventory SET unit = '罐' WHERE material_id = 1;
UPDATE inventory SET unit = '罐' WHERE product_id = 1;
```

### 6.2 业务案例验证

**案例：大米的单位不一致**

| 表 | material_id | name | unit | 问题 |
|----|-------------|------|------|------|
| material_archives | M003 | 东北大米 | kg | ✅ 标准 |
| inventory | M003 | 东北大米 | 公斤 | ⚠️ 中文 vs 英文 |
| store_inventory | M003 | 东北大米 | KG | ⚠️ 大写 |
| dish_recipe | M003 | 东北大米 | g | ⚠️ 基础单位 |

**关键问题**：
1. 同一个"大米"在四张表中有四种单位表示
2. 无外键约束保证单位一致性
3. 单位转换逻辑散落在多处

### 6.3 根因分析

**根因1**：缺少统一的 Unit Foundation
- 每张表独立管理自己的单位字段
- 无全局的单位字典和转换规则

**根因2**：缺少统一的 Canonical Item
- 每张表独立存储商品名称、规格等信息
- 修改一处时无法自动同步其他地方

### 6.4 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 成本核算 | 🔴 严重 | 单位不一致导致成本计算错误 |
| 库存管理 | 🔴 严重 | 单位转换错误导致库存数量不准 |
| 配方计算 | 🔴 严重 | 单位不一致导致配方用量错误 |
| 报表统计 | 🟡 中等 | 需要处理多种单位表示 |

### 6.5 修复方向

**正确的模型**：Canonical Item + Unit Foundation

```
Canonical Item: 东北大米
├── 基础单位: G (克)
├── 库存单位: KG (公斤) → 1 KG = 1000 G
├── 采购单位: BAG (袋) → 1 BAG = 25 KG
└── 配方单位: G (克) → 与基础单位相同
```

---

## 七、Type Ambiguity: material_archives 的语义混乱

### 7.1 压力描述

`material_archives` 表被称为"商品档案"，但实际上同时承担了三种完全不同的语义：
1. **Product**：可销售的商品（如可乐）
2. **Material**：可采购的原料（如鸡翅）
3. **Inventory Item**：可存储的物品（如大米）

```
material_archives 表的实际用途：
├── 可乐: 既是 Product (可销售)，又是 Material (可采购)
├── 鸡翅: 主要是 Material (可采购)，不直接销售
├── 大米: 主要是 Material (可采购)，不直接销售
└── 宫保鸡丁: 通常不在此表中（成品不采购）
```

**补丁代码**：

```java
// 判断 material_archives 中的记录是"商品"还是"原料"
public String getMaterialType(Long materialId) {
    MaterialArchive ma = materialArchiveMapper.selectById(materialId);
    
    // 补丁逻辑：通过名称或分类判断类型
    if (ma.getMaterialName().contains("可乐") || 
        ma.getMaterialName().contains("矿泉水")) {
        return "PRODUCT";  // 可销售的商品
    } else if (ma.getCategoryName().equals("生鲜") || 
               ma.getCategoryName().equals("调料")) {
        return "MATERIAL";  // 可采购的原料
    } else {
        return "UNKNOWN";  // 类型不明确
    }
}
```

### 7.2 业务案例验证

**案例：可口可乐在 material_archives 中的尴尬地位**

| 字段 | 值 | 问题 |
|------|-----|------|
| material_name | "可口可乐330ml" | ✅ 作为 Material 合理 |
| category_id | 饮料 | ✅ 作为 Product 合理 |
| reference_price | 2.50 | ⚠️ 这是采购价还是成本价？ |
| supplier_id | S001 | ✅ 作为 Material 合理 |
| shelf_life | 365 | ✅ 作为 Product 合理 |

**关键问题**：
1. 可乐在 material_archives 中，但它的主要业务场景是**销售**（foods 表）
2. material_archives 中的 reference_price 是采购参考价，但可乐的**销售价**在 foods 表中
3. 系统无法自动判断一个 material 是"可销售的商品"还是"纯原料"

### 7.3 根因分析

material_archives 表在设计时试图**统一 Product 和 Material**，但实际上：
- Product 关注**销售属性**：售价、菜单配置、POS 显示
- Material 关注**操作属性**：采购价、供应商、保质期
- 两者有**重叠**：名称、规格、单位
- 但也有**差异**：销售价 vs 采购价，菜单配置 vs 供应商配置

### 7.4 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 商品分类 | 🔴 严重 | 无法自动区分"商品"和"原料" |
| 采购管理 | 🟡 中等 | 可乐作为"原料"采购，但实际是"商品" |
| 销售管理 | 🔴 严重 | 可乐在 foods 中，但 material_archives 中也有 |
| 报表统计 | 🟡 中等 | 需要手动过滤"商品"和"原料" |

### 7.5 修复方向

**正确的模型**：Canonical Item + Capability Flag

```
Canonical Item: 可口可乐330ml
├── is_sellable:     TRUE  (Commercial Profile)
├── is_purchasable:  TRUE  (Procurement Profile)
├── is_storable:     TRUE  (Inventory Profile)
└── is_ingredient:   TRUE  (Recipe Profile - 可乐鸡翅)

Canonical Item: 鸡胸肉
├── is_sellable:     FALSE
├── is_purchasable:  TRUE
├── is_storable:     TRUE
└── is_ingredient:   TRUE
```

---

## 八、Inventory 双表问题: inventory 与 store_inventory 的事务一致性

### 8.1 压力描述

`inventory`（仓库库存）和 `store_inventory`（门店库存）是**两张独立的表**，没有外键关系，也没有事务一致性保证。

```
当前架构：
inventory (仓库库存)
├── id, product_id (遗留), warehouse_id
├── current_stock, safety_stock
└── cost_price, stock_value

store_inventory (门店库存)
├── store_id, material_id
├── current_stock, unit_cost
└── safety_stock, max_stock

问题：两张表无事务一致性保证
```

**补丁代码**：

```java
// 门店收货时需要同时更新两张表
@Transactional
public void receiveStoreDelivery(Long materialId, Long storeId, int quantity) {
    // 更新仓库库存（减少）
    inventoryMapper.deductStock(materialId, warehouseId, quantity);
    
    // 更新门店库存（增加）
    storeInventoryMapper.addStock(storeId, materialId, quantity);
    
    // 问题：如果第二步失败，第一步已经提交
    // 没有跨表的事务一致性保证
}

// 更糟：使用分布式事务
@GlobalTransactional  // Seata 分布式事务
public void receiveStoreDelivery(Long materialId, Long storeId, int quantity) {
    // 复杂度高，性能差
}
```

### 8.2 业务案例验证

**案例：门店调拨流程**

```
流程：仓库 → 门店A 调拨 100 罐可乐

步骤1：仓库库存减少
inventory: material_id=M001, warehouse_id=W01, current_stock: 200 → 100

步骤2：门店库存增加
store_inventory: material_id=M001, store_id=S01, current_stock: 20 → 120

风险：
- 如果步骤2失败，步骤1已提交 → 库存数据不一致
- 如果步骤1和步骤2之间有其他操作 → 并发问题
- 如果系统崩溃 → 可能丢失部分更新
```

### 8.3 根因分析

**根因1**：仓库库存和门店库存使用**不同的表结构**
- `inventory` 使用 `product_id`（遗留字段）+ `warehouse_id`
- `store_inventory` 使用 `material_id` + `store_id`
- 两者无外键关系，无法保证引用完整性

**根因2**：缺少**统一的位置模型**
- 仓库和门店应该是同一种位置类型的不同实例
- 但当前模型将它们建模为两个独立的概念

### 8.4 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 门店调拨 | 🔴 严重 | 无事务一致性保证 |
| 库存盘点 | 🔴 严重 | 仓库和门店库存独立盘点 |
| 成本核算 | 🟡 中等 | 仓库和门店成本独立维护 |
| 报表统计 | 🟡 中等 | 需要合并两张表的数据 |

### 8.5 修复方向

**正确的模型**：统一 Inventory Instance

```sql
CREATE TABLE inventory_instance (
    instance_id         BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,
    location_id         BIGINT NOT NULL,        -- 统一位置模型
    location_type       VARCHAR(20) NOT NULL,    -- 'WAREHOUSE' | 'STORE'
    current_stock       INTEGER NOT NULL DEFAULT 0,
    locked_quantity     INTEGER DEFAULT 0,
    unit_cost           DECIMAL(12,2) DEFAULT 0,
    total_cost          DECIMAL(14,4) DEFAULT 0,
    batch_no            VARCHAR(50),
    production_date     DATE,
    expiry_date         DATE,
    UNIQUE (canonical_item_id, location_id, batch_no)
);
```

---

## 九、Recipe 模型的多重压力

### 9.1 压力描述

`dish_recipe` 表面临多重压力：
1. 不支持版本管理
2. 不支持门店差异
3. 不支持替代材料
4. ingredient_id 同时引用 foods 和 material_archives（语义不明确）

```
当前 dish_recipe 表结构：
CREATE TABLE dish_recipe (
    id BIGINT PRIMARY KEY,
    dish_id BIGINT,           -- 引用 foods.food_id
    ingredient_id BIGINT,     -- 引用 material_archives.material_id（还是 foods.food_id？）
    ingredient_name VARCHAR(100),  -- 自由文本，与 ingredient_id 冗余
    quantity DECIMAL(10,2),
    unit VARCHAR(20),
    ...
);

问题：
1. ingredient_id 指向哪个表？材料档案还是食品？
2. 修改配方时直接更新原记录，无法追溯历史
3. 所有门店使用相同配方，无法支持区域差异
```

**补丁代码**：

```java
// 判断 ingredient_id 指向哪个表
public MaterialArchive getIngredient(Long ingredientId) {
    // 补丁逻辑：先查 material_archives，再查 foods
    MaterialArchive ma = materialArchiveMapper.selectById(ingredientId);
    if (ma != null) {
        return ma;
    }
    
    // 如果 material_archives 中没有，尝试从 foods 中查找
    Food food = foodMapper.selectById(ingredientId);
    if (food != null) {
        // 将 foods 记录转换为 MaterialArchive 格式
        return convertFoodToMaterial(food);
    }
    
    return null;
}
```

### 9.2 业务案例验证

**案例：麻婆豆腐的区域差异**

| 门店区域 | 辣椒用量 | 花椒用量 | 口味差异 |
|---------|---------|---------|---------|
| 四川门店 | 15g | 10g | 重辣重麻 |
| 广东门店 | 5g | 2g | 微辣微麻 |
| 北京门店 | 10g | 5g | 中辣中麻 |

**当前模型**：dish_recipe 只有一个标准配方，无法支持区域差异

**补丁方式**：为每个区域创建不同的 dish_id

```
foods 表：
├── food_id=F010, food_name="麻婆豆腐(四川版)"
├── food_id=F011, food_name="麻婆豆腐(广东版)"
└── food_id=F012, food_name="麻婆豆腐(北京版)"

dish_recipe 表：
├── dish_id=F010, ingredient_id=M001, quantity=15  (四川版)
├── dish_id=F011, ingredient_id=M001, quantity=5   (广东版)
└── dish_id=F012, ingredient_id=M001, quantity=10  (北京版)

问题：同一个菜品在 foods 表中有三个记录，数据爆炸
```

### 9.3 根因分析

**根因1**：dish_recipe 缺少 **version** 和 **scope** 概念
- 无版本管理：修改配方时直接更新原记录
- 无门店范围：所有门店使用相同配方

**根因2**：ingredient_id 的**多态引用**问题
- ingredient_id 可能指向 material_archives（原料）
- 也可能指向 foods（成品菜品作为原料）
- 无外键约束保证引用完整性

### 9.4 修复方向

**正确的模型**：Recipe + Recipe Component + Scope

```sql
CREATE TABLE recipe (
    id BIGINT PRIMARY KEY,
    canonical_item_id BIGINT NOT NULL,  -- 菜品的统一身份
    version INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'active',
    effective_date DATE,
    ...
);

CREATE TABLE recipe_component (
    id BIGINT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    ingredient_item_id BIGINT NOT NULL,  -- 统一引用 canonical_item
    quantity DECIMAL(10,2),
    uom_code VARCHAR(20),
    ...
);

CREATE TABLE recipe_scope (
    id BIGINT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    scope_type VARCHAR(20),  -- 'GLOBAL' | 'REGION' | 'STORE'
    scope_id BIGINT,         -- region_id 或 store_id
    ...
);
```

---

## 十、压力点交叉分析

### 10.1 压力点关系图

```
                        ┌─────────────────────┐
                        │   Identity Stress    │
                        │ (foods ↔ material)   │
                        └──────────┬──────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
            ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
            │    Role      │ │ Relationship │ │     Type     │
            │   Conflict   │ │  Complexity  │ │  Ambiguity   │
            │ (多角色无模型)│ │ (隐式关联)   │ │ (语义混乱)   │
            └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
                   │                │                │
                   └────────────────┼────────────────┘
                                    ▼
                        ┌─────────────────────┐
                        │  Data Duplication    │
                        │ (同一数据多处维护)    │
                        └──────────┬──────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
            ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
            │   Scope      │ │  Inventory   │ │   Recipe     │
            │  Mismatch    │ │  双表问题     │ │  多重压力    │
            │ (全局vs门店)  │ │ (无事务一致)  │ │ (版本/范围)  │
            └──────────────┘ └──────────────┘ └──────────────┘
```

### 10.2 根因追溯

| 压力点 | 表层症状 | 深层根因 | 修复优先级 |
|--------|---------|---------|-----------|
| Identity Stress | 编码匹配、名称匹配 | 缺少 Canonical Item | P0 |
| Role Conflict | 多表存储角色信息 | 缺少显式 Role 模型 | P0 |
| Scope Mismatch | 为每个门店创建独立记录 | 缺少 Profile + Scope 覆盖 | P1 |
| Relationship Complexity | 隐式外键、编码匹配 | 缺少统一的跨域关联 | P0 |
| Data Duplication | 单位不一致、名称不一致 | 缺少 Canonical Item + Unit Foundation | P0 |
| Type Ambiguity | 类型判断逻辑散落在业务代码中 | 缺少 Capability Flag | P1 |
| Inventory 双表 | 无事务一致性 | 缺少统一位置模型 | P1 |
| Recipe 多重压力 | 版本/范围/替代材料缺失 | 缺少 Recipe 版本模型 | P2 |

### 10.3 系统性结论

**核心发现**：所有压力点都指向同一个根因——**缺少 Canonical Item 作为统一身份层**。

```
当前模型：
foods ←──?──→ material_archives ←──→ inventory
   (独立表)      (独立表)         (独立表)
   (无关联)      (无关联)         (无关联)

目标模型：
Canonical Item
├── Commercial Profile (销售)
├── Procurement Profile (采购)
├── Inventory Profile (库存)
├── Recipe Profile (配方)
└── Finance Profile (财务)
   (统一身份)    (显式关联)    (事务一致)
```

---

## 十一、修复路径建议

### 11.1 修复优先级矩阵

| 优先级 | 压力点 | 修复策略 | 预期收益 |
|--------|--------|---------|---------|
| **P0** | Identity Stress | 引入 Canonical Item | 消除身份分裂 |
| **P0** | Relationship Complexity | 统一跨域关联 | 消除隐式关联 |
| **P0** | Data Duplication | Canonical Item + Unit Foundation | 消除数据冗余 |
| **P1** | Role Conflict | 显式 Role 模型 | 统一角色管理 |
| **P1** | Scope Mismatch | Profile + Scope 覆盖 | 支持门店差异 |
| **P1** | Type Ambiguity | Capability Flag | 明确类型语义 |
| **P1** | Inventory 双表 | 统一位置模型 | 事务一致性 |
| **P2** | Recipe 多重压力 | Recipe 版本模型 | 配方管理能力 |

### 11.2 渐进式修复路径

```
Phase 1: 建立 Canonical Item 基础 (4周)
├── 创建 canonical_item 表
├── 迁移 material_archives → canonical_item
├── 创建 item_bridge 表（过渡方案）
└── 验证基本关联

Phase 2: 引入 Profile 层 (4周)
├── 创建 commercial_profile 表
├── 创建 procurement_profile 表
├── 迁移 foods → canonical_item + commercial_profile
└── 迁移 material_archives → canonical_item + procurement_profile

Phase 3: 统一库存模型 (3周)
├── 创建 location 统一位置模型
├── 创建 inventory_instance 统一库存表
├── 迁移 inventory + store_inventory → inventory_instance
└── 验证事务一致性

Phase 4: 增强 Recipe 模型 (3周)
├── 创建 recipe + recipe_component 表
├── 添加版本管理
├── 添加门店范围支持
└── 添加替代材料支持
```

---

## 十二、总结

### 12.1 核心发现

1. **所有压力点都指向同一个根因**：缺少 Canonical Item 作为统一身份层
2. **当前模型的根本错误**：将"商业定义"和"操作定义"建模为两个独立实体
3. **补丁模式的共性**：都是通过创建更多表/记录来绕过模型缺陷

### 12.2 修复原则

1. **先统一身份，再分层配置**：Canonical Item 是所有修复的基础
2. **渐进式迁移**：先建立桥接，再逐步迁移，最后统一模型
3. **保持业务连续性**：修复过程中不能中断现有业务

### 12.3 风险提示

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 迁移复杂度高 | 业务中断 | 分阶段迁移，保留回滚能力 |
| 数据迁移失败 | 数据丢失 | 完整的数据验证和备份策略 |
| 性能下降 | 用户体验 | 添加索引，优化查询 |
| 业务逻辑复杂度增加 | 开发成本 | 渐进式实现，避免过度设计 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-10
**作者**：AI 架构总控
**阶段**：Phase 21 - Model Stress Points Analysis
