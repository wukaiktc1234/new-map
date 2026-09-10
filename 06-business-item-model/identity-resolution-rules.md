# Identity Resolution Rules

> **文档类型**: Canonical Identity 粒度规则  
> **生成日期**: 2026-09-09  
> **状态**: ANALYSIS  
> **阶段**: Phase 2 - 身份解析规则定义  
> **核心问题**: 两个记录什么时候是同一个业务对象？两个"可乐"什么时候是不同对象？

---

## 一、问题定义

### 1.1 核心挑战

在餐饮ERP系统中，同一个物理实体可能在多个业务域中以不同形式存在：

```
可口可乐 330ml 罐
  ├── Food:    "可乐" (POS销售)
  ├── Material: "可口可乐" (采购/库存)
  ├── Product:  "COLA001" (供应商目录)
  ├── Inventory: 批次 20260901 (仓库管理)
  └── Recipe:   "可乐" (可乐鸡翅的原料)
```

**关键问题**: 这些记录是否代表同一个业务对象？如果是，如何建立统一标识？

### 1.2 分析目标

1. 定义 Canonical Identity 的粒度规则
2. 确定 Identity 应基于哪些属性
3. 区分"同一个东西"与"同一个东西的不同包装/规格"
4. 建立跨域身份判断标准

---

## 二、Identity 属性评估

### 2.1 候选属性矩阵

| 属性 | 类型 | 稳定性 | 唯一性 | 跨域可用性 | 评估结论 |
|------|------|--------|--------|------------|----------|
| **Brand** (品牌) | 高 | 高 | 低 | 高 | ✅ 强标识，但不充分 |
| **Name** (名称) | 高 | 中 | 低 | 高 | ⚠️ 弱标识，命名不规范 |
| **Specification** (规格) | 中 | 高 | 中 | 高 | ✅ 关键区分属性 |
| **Package** (包装) | 低 | 中 | 中 | 中 | ⚠️ 包装可变，不作为核心标识 |
| **Size** (尺寸) | 中 | 高 | 中 | 中 | ⚠️ 与 Specification 重叠 |
| **Base Unit** (基础单位) | 高 | 高 | 低 | 高 | ✅ 稳定但不区分对象 |
| **Barcode** (条形码) | 高 | 高 | 高 | 中 | ⚠️ 理想标识，但数据不完整 |
| **SKU** (库存单位) | 高 | 高 | 高 | 中 | ⚠️ 理想标识，但不是所有域都有 |
| **Manufacturer** (制造商) | 高 | 高 | 低 | 低 | ⚠️ 只在采购域有 |
| **Product Code** (产品编码) | 高 | 高 | 中 | 中 | ⚠️ 各域编码不统一 |

### 2.2 属性评估详解

#### Brand (品牌) - 强标识 ✅

**作用**: 品牌是区分"可口可乐"和"百事可乐"的关键属性

**局限**: 品牌不能区分同一品牌下的不同产品

**示例**:
- 可口可乐 330ml 罐 → Brand: 可口可乐
- 可口可乐 500ml 瓶 → Brand: 可口可乐
- 可口可乐糖浆 → Brand: 可口可乐

**结论**: Brand 是必要属性，但不充分

#### Name (名称) - 弱标识 ⚠️

**作用**: 名称是业务人员最直观的识别方式

**局限**: 命名不规范，不同系统/人员可能使用不同名称

**示例**:
- Food: "可乐", "可口可乐", "Coca-Cola"
- Material: "可口可乐330ml", "可口可乐罐装"
- Product: "COLA-330", "可口可乐330ml罐"

**结论**: Name 不作为核心标识，但可作为辅助验证

#### Specification (规格) - 关键区分 ✅

**作用**: 规格是区分同一品牌下不同产品的关键属性

**局限**: 规格命名可能不规范（如"330ml" vs "330毫升"）

**示例**:
- 可口可乐 330ml 罐 → Spec: "330ml"
- 可口可乐 500ml 瓶 → Spec: "500ml"
- 可口可乐糖浆 → Spec: "5L桶装"

**结论**: Specification 是核心区分属性

#### Barcode (条形码) - 理想标识 ⚠️

**作用**: 条形码是全球唯一标识，理论上是最佳标识

**局限**: 数据不完整，不是所有记录都有条形码

**示例**:
- 可口可乐 330ml 罐 → Barcode: 6920459950180
- 可口可乐 500ml 瓶 → Barcode: 6920459950197

**结论**: Barcode 是理想标识，但数据完整性不足以作为唯一依赖

#### SKU (库存单位) - 理想标识 ⚠️

**作用**: SKU 是供应链中的标准标识，唯一性强

**局限**: 不是所有业务域都有 SKU（如 Recipe 域）

**示例**:
- 可口可乐 330ml 罐 → SKU: COLA-330-CAN
- 可口可乐 500ml 瓶 → SKU: COLA-500-BTL

**结论**: SKU 是供应链域的理想标识，但跨域可用性有限

---

## 三、Identity 构成规则

### 3.1 Canonical Identity 定义

**Canonical Identity = Brand + ProductFamily + Specification**

| 组成部分 | 定义 | 示例 |
|----------|------|------|
| **Brand** | 制造商/品牌名称 | 可口可乐, 百事可乐, 农夫山泉 |
| **ProductFamily** | 产品系列/家族 | 可乐系列, 矿泉水系列, 果汁系列 |
| **Specification** | 具体规格/包装 | 330ml罐, 500ml瓶, 24罐箱装 |

### 3.2 Identity 构成示例

| 业务对象 | Brand | ProductFamily | Specification | Canonical Identity |
|----------|-------|---------------|---------------|-------------------|
| 可口可乐 330ml 罐 | 可口可乐 | 可乐 | 330ml罐 | `COLA_COCACOLA_330ML_CAN` |
| 可口可乐 500ml 瓶 | 可口可乐 | 可乐 | 500ml瓶 | `COLA_COCACOLA_500ML_BTL` |
| 可口可乐 330ml 罐 × 24箱 | 可口可乐 | 可乐 | 330ml罐×24箱 | `COLA_COCACOLA_330ML_CAN_24BOX` |
| 可口可乐糖浆 | 可口可乐 | 可乐糖浆 | 5L桶 | `SYRUP_COCACOLA_5L_BKT` |
| 可乐原液 | 通用 | 可乐原液 | 20L桶 | `BASE_COLA_20L_BKT` |

### 3.3 Identity 构成规则

#### 规则 1: Brand 是必要条件

两个记录要被认为是同一业务对象，**Brand 必须相同**。

```
IF Brand_A ≠ Brand_B THEN
    DIFFERENT_IDENTITY
END IF
```

**例外**: "通用"品牌或无品牌产品

#### 规则 2: ProductFamily 是核心区分

同一 Brand 下，**ProductFamily 不同则 Identity 不同**。

```
IF Brand_A = Brand_B AND ProductFamily_A ≠ ProductFamily_B THEN
    DIFFERENT_IDENTITY
END IF
```

**示例**:
- 可口可乐 **可乐** 330ml → ProductFamily: 可乐
- 可口可乐 **雪碧** 330ml → ProductFamily: 雪碧
- 结论: DIFFERENT_IDENTITY

#### 规则 3: Specification 是精细区分

同一 Brand + ProductFamily 下，**Specification 不同则 Identity 不同**。

```
IF Brand_A = Brand_B AND ProductFamily_A = ProductFamily_B AND Specification_A ≠ Specification_B THEN
    DIFFERENT_IDENTITY
END IF
```

**示例**:
- 可口可乐 可乐 **330ml罐** → Specification: 330ml罐
- 可口可乐 可乐 **500ml瓶** → Specification: 500ml瓶
- 结论: DIFFERENT_IDENTITY

---

## 四、测试案例分析

### 案例 1: 可口可乐 330ml 罐

**属性分析**:

| 属性 | Value |
|------|-------|
| Brand | 可口可乐 |
| ProductFamily | 可乐 |
| Specification | 330ml罐 |
| Barcode | 6920459950180 |

**跨域表示**:

| 域 | 记录 | 标识符 |
|----|------|--------|
| Food | "可乐" | food_code=COLA001 |
| Material | "可口可乐330ml" | material_code=MAT_COLA001 |
| Product | "COLA-330" | product_id=1 |
| Inventory | 批次20260901 | batch_no=20260901 |
| Recipe | "可乐" | ingredient_id=1 |

**Canonical Identity**: `COLA_COCACOLA_330ML_CAN`

---

### 案例 2: 可口可乐 500ml 瓶

**属性分析**:

| 属性 | Value |
|------|-------|
| Brand | 可口可乐 |
| ProductFamily | 可乐 |
| Specification | 500ml瓶 |
| Barcode | 6920459950197 |

**跨域表示**:

| 域 | 记录 | 标识符 |
|----|------|--------|
| Food | "可乐500ml" | food_code=COLA002 |
| Material | "可口可乐500ml" | material_code=MAT_COLA002 |
| Product | "COLA-500" | product_id=2 |
| Inventory | 批次20260901 | batch_no=20260901B |
| Recipe | (不常用) | - |

**Canonical Identity**: `COLA_COCACOLA_500ML_BTL`

---

### 案例 3: 可口可乐 330ml 罐 × 24箱

**属性分析**:

| 属性 | Value |
|------|-------|
| Brand | 可口可乐 |
| ProductFamily | 可乐 |
| Specification | 330ml罐×24箱 |
| Barcode | 6920459950180 (单罐) / 6920459950203 (箱) |

**跨域表示**:

| 域 | 记录 | 标识符 |
|----|------|--------|
| Food | (不直接销售) | - |
| Material | "可口可乐330ml×24" | material_code=MAT_COLA003 |
| Product | "COLA-330-CASE" | product_id=3 |
| Inventory | 批次20260901 | batch_no=20260901C |
| Recipe | (不作为原料) | - |

**Canonical Identity**: `COLA_COCACOLA_330ML_CAN_24BOX`

**关键决策**: 箱装是否是独立 Identity？

**分析**:
- 从物理形态: 箱装 = 24罐的组合包装
- 从库存管理: 箱装和罐装是独立的库存单位
- 从采购角度: 箱装和罐装是不同的采购单位
- 从销售角度: 箱装通常不直接销售给顾客

**结论**: 箱装是独立的 Canonical Identity，因为它在库存和采购域有独立的管理需求。

---

### 案例 4: 可口可乐糖浆

**属性分析**:

| 属性 | Value |
|------|-------|
| Brand | 可口可乐 |
| ProductFamily | 可乐糖浆 |
| Specification | 5L桶装 |
| Barcode | 6920459950302 |

**跨域表示**:

| 域 | 记录 | 标识符 |
|----|------|--------|
| Food | (不直接销售) | - |
| Material | "可口可乐糖浆" | material_code=MAT_SYRUP001 |
| Product | "SYRUP-COLA" | product_id=10 |
| Inventory | 批次20260901 | batch_no=20260901S |
| Recipe | "可乐糖浆" | ingredient_id=10 |

**Canonical Identity**: `SYRUP_COCACOLA_5L_BKT`

**关键决策**: 糖浆与罐装可乐是同一 Identity 吗？

**分析**:
- 从 Brand: 相同（可口可乐）
- 从 ProductFamily: **不同**（可乐 vs 可乐糖浆）
- 从用途: 罐装是即饮产品，糖浆是原料
- 从物理形态: 罐装是成品饮料，糖浆是浓缩液

**结论**: 糖浆与罐装可乐是 **DIFFERENT_IDENTITY**，因为 ProductFamily 不同。

---

### 案例 5: 可乐原液

**属性分析**:

| 属性 | Value |
|------|-------|
| Brand | 通用 (无品牌) |
| ProductFamily | 可乐原液 |
| Specification | 20L桶装 |
| Barcode | (可能无条码) |

**跨域表示**:

| 域 | 记录 | 标识符 |
|----|------|--------|
| Food | (不直接销售) | - |
| Material | "可乐原液" | material_code=MAT_BASE001 |
| Product | "BASE-COLA" | product_id=20 |
| Inventory | 批次20260901 | batch_no=20260901X |
| Recipe | "可乐原液" | ingredient_id=20 |

**Canonical Identity**: `BASE_COLA_20L_BKT`

**关键决策**: 可乐原液与可口可乐糖浆是同一 Identity 吗？

**分析**:
- 从 Brand: **不同**（通用 vs 可口可乐）
- 从 ProductFamily: 不同（可乐原液 vs 可乐糖浆）
- 从用途: 都是制作可乐的原料
- 从物理形态: 都是浓缩液

**结论**: 可乐原液与可口可乐糖浆是 **DIFFERENT_IDENTITY**，因为 Brand 不同。

---

## 五、身份分类规则

### 5.1 分类定义

| 分类 | 定义 | 判断条件 |
|------|------|----------|
| **SAME_IDENTITY** | 完全相同的业务对象 | Brand + ProductFamily + Specification 完全匹配 |
| **DIFFERENT_IDENTITY** | 不同的业务对象 | Brand 或 ProductFamily 不同 |
| **SAME_BASE_ITEM_DIFFERENT_PACKAGING** | 同一基础物品的不同包装 | Brand + ProductFamily 相同，Specification 仅包装不同 |
| **SAME_ITEM_DIFFERENT_COMMERCIAL_OFFERING** | 同一物品的不同商业报价 | Brand + ProductFamily 相同，但销售渠道/价格不同 |
| **UNKNOWN** | 无法确定 | 信息不足，无法判断 |

### 5.2 分类判断流程

```
START
  │
  ├─→ 检查 Brand 是否相同？
  │     ├─ NO → DIFFERENT_IDENTITY
  │     └─ YES ↓
  │
  ├─→ 检查 ProductFamily 是否相同？
  │     ├─ NO → DIFFERENT_IDENTITY
  │     └─ YES ↓
  │
  ├─→ 检查 Specification 是否相同？
  │     ├─ YES → SAME_IDENTITY
  │     └─ NO ↓
  │
  ├─→ 检查差异是否仅为包装？
  │     ├─ YES → SAME_BASE_ITEM_DIFFERENT_PACKAGING
  │     └─ NO ↓
  │
  ├─→ 检查差异是否为商业报价？
  │     ├─ YES → SAME_ITEM_DIFFERENT_COMMERCIAL_OFFERING
  │     └─ NO → DIFFERENT_IDENTITY
  │
END
```

### 5.3 分类判断示例

#### 示例 1: 可口可乐 330ml 罐 vs 可口可乐 330ml 罐

```
Brand: 可口可乐 = 可口可乐 ✓
ProductFamily: 可乐 = 可乐 ✓
Specification: 330ml罐 = 330ml罐 ✓
→ SAME_IDENTITY
```

#### 示例 2: 可口可乐 330ml 罐 vs 可口可乐 500ml 瓶

```
Brand: 可口可乐 = 可口可乐 ✓
ProductFamily: 可乐 = 可乐 ✓
Specification: 330ml罐 ≠ 500ml瓶 ✗
差异: 规格不同（容量+包装形态）
→ DIFFERENT_IDENTITY
```

#### 示例 3: 可口可乐 330ml 罐 vs 可口可乐 330ml 罐 × 24箱

```
Brand: 可口可乐 = 可口可乐 ✓
ProductFamily: 可乐 = 可乐 ✓
Specification: 330ml罐 ≠ 330ml罐×24箱 ✗
差异: 仅包装数量不同
→ SAME_BASE_ITEM_DIFFERENT_PACKAGING
```

#### 示例 4: 可口可乐 330ml 罐 (堂食) vs 可口可乐 330ml 罐 (外卖)

```
Brand: 可口可乐 = 可口可乐 ✓
ProductFamily: 可乐 = 可乐 ✓
Specification: 330ml罐 = 330ml罐 ✓
差异: 销售渠道不同，价格可能不同
→ SAME_ITEM_DIFFERENT_COMMERCIAL_OFFERING
```

#### 示例 5: 可口可乐 330ml 罐 vs 百事可乐 330ml 罐

```
Brand: 可口可乐 ≠ 百事可乐 ✗
→ DIFFERENT_IDENTITY
```

#### 示例 6: 可口可乐糖浆 vs 可乐原液

```
Brand: 可口可乐 ≠ 通用 ✗
→ DIFFERENT_IDENTITY
```

---

## 六、跨域身份判断

### 6.1 域定义

| 域 | 说明 | 典型表 |
|----|------|--------|
| **Food** | 可销售的成品/菜品 | foods, order_items |
| **Material** | 可采购、可存储的原料 | material_archives, inventory |
| **Product** | 供应商目录中的产品 | product |
| **Inventory** | 库存管理 | inventory, store_inventory |
| **Purchase** | 采购管理 | purchase_order_items |
| **Recipe** | 菜品配方/BOM | dish_recipe |

### 6.2 跨域身份判断矩阵

| 域 A | 域 B | 判断依据 | 典型场景 |
|------|------|----------|----------|
| Food ↔ Material | 基于 Brand + ProductFamily + Specification | 可乐(POS) ↔ 可口可乐(库存) |
| Food ↔ Recipe | 基于 ProductFamily | 可乐(销售) ↔ 可乐(原料) |
| Material ↔ Inventory | 基于 material_id 外键 | 可口可乐(物料) ↔ 批次(库存) |
| Material ↔ Purchase | 基于 material_id 外键 | 可口可乐(物料) ↔ 采购单 |
| Purchase ↔ Product | 基于 Product Code | 采购单 ↔ 供应商目录 |

### 6.3 跨域身份映射示例

**场景**: 可口可乐 330ml 罐在各域中的映射

```
┌─────────────────────────────────────────────────────────────────┐
│                    Canonical Identity                            │
│                 COLA_COCACOLA_330ML_CAN                         │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
    ┌─────▼─────┐       ┌─────▼─────┐       ┌─────▼─────┐
    │   Food    │       │ Material  │       │  Recipe   │
    │  "可乐"   │       │"可口可乐" │       │  "可乐"   │
    │ food_code │       │mat_code   │       │ingred_id  │
    │=COLA001   │       |=MAT_COLA  │       |=1         │
    └─────┬─────┘       └─────┬─────┘       └─────┬─────┘
          │                   │                   │
    ┌─────▼─────┐       ┌─────▼─────┐       ┌─────▼─────┐
    │  Order    │       │ Inventory │       │   Dish    │
    │ Items     │       │  批次     │       │  Recipe   │
    │ food_id=1 │       │batch=2026 │       │ dish_id=2 │
    └───────────┘       └───────────┘       └───────────┘
```

### 6.4 跨域身份判断规则

#### 规则 1: Food ↔ Material 映射

**判断条件**:
```
Food.brand = Material.brand AND
Food.productFamily = Material.productFamily AND
Food.specification = Material.specification
```

**示例**:
- Food: "可乐" (food_code=COLA001) 
- Material: "可口可乐330ml" (material_code=MAT_COLA001)
- 判断: Brand=可口可乐, ProductFamily=可乐, Specification=330ml罐 → SAME_IDENTITY

#### 规则 2: Material ↔ Inventory 映射

**判断条件**:
```
Inventory.material_id = Material.material_id
```

**示例**:
- Material: "可口可乐330ml" (material_id=1)
- Inventory: 批次20260901 (material_id=1, batch_no=20260901)
- 判断: 通过 material_id 外键关联 → SAME_IDENTITY

#### 规则 3: Recipe ↔ Material 映射

**判断条件**:
```
Recipe.ingredient_id = Material.material_id
```

**示例**:
- Recipe: "可乐" (dish_id=2, ingredient_id=1)
- Material: "可口可乐330ml" (material_id=1)
- 判断: 通过 ingredient_id 外键关联 → SAME_IDENTITY

---

## 七、特殊情况处理

### 7.1 无品牌产品

**场景**: 散装大米、通用面粉等无品牌产品

**处理规则**:
```
IF Brand = NULL OR Brand = '通用' THEN
    Identity = ProductFamily + Specification
END IF
```

**示例**:
- 散装大米 (无品牌, ProductFamily=大米, Specification=东北产)
- 通用面粉 (无品牌, ProductFamily=面粉, Specification=高筋)
- 判断: DIFFERENT_IDENTITY (ProductFamily 不同)

### 7.2 同一产品的不同渠道报价

**场景**: 同一瓶可乐在堂食卖 3 元，外卖卖 5 元

**处理规则**:
```
IF Brand + ProductFamily + Specification 相同 AND
   仅价格/渠道不同 THEN
    SAME_ITEM_DIFFERENT_COMMERCIAL_OFFERING
END IF
```

**结论**: 这是同一个 Canonical Identity，价格差异是商业报价差异，不是 Identity 差异。

### 7.3 促销包装

**场景**: 可口可乐 330ml 罐 × 6 (促销装)

**处理规则**:
```
IF Specification 差异仅为包装数量 THEN
    SAME_BASE_ITEM_DIFFERENT_PACKAGING
END IF
```

**示例**:
- 可口可乐 330ml 罐 × 6 (促销装)
- 可口可乐 330ml 罐 × 24 (箱装)
- 判断: SAME_BASE_ITEM_DIFFERENT_PACKAGING

### 7.4 跨国/跨地区版本

**场景**: 中国版可口可乐 vs 美国版可口可乐

**处理规则**:
```
IF Brand + ProductFamily + Specification 相同 AND
   产地/版本不同 THEN
    SAME_BASE_ITEM_DIFFERENT_PACKAGING
END IF
```

**示例**:
- 可口可乐 330ml 罐 (中国版)
- 可口可乐 330ml 罐 (美国版)
- 判断: SAME_BASE_ITEM_DIFFERENT_PACKAGING

### 7.5 临期产品

**场景**: 临期可乐 vs 正常可乐

**处理规则**:
```
IF Brand + ProductFamily + Specification 相同 AND
   仅保质期/批次不同 THEN
    SAME_IDENTITY
END IF
```

**结论**: 临期产品是同一个 Canonical Identity，批次差异是库存管理属性，不是 Identity 差异。

---

## 八、当前系统映射

### 8.1 现有表结构与 Identity 的关系

#### foods 表

```sql
foods
├── food_code     → Canonical Identity 的简化表示
├── food_name     → 可用于辅助验证
├── food_price    → 商业报价属性（Role 属性）
└── food_category → ProductFamily 的近似表示
```

**问题**: food_code 不能唯一标识 Canonical Identity，因为同一产品可能有多个 food_code（不同门店/渠道）

#### material_archives 表

```sql
material_archives
├── material_code   → Canonical Identity 的简化表示
├── material_name   → 可用于辅助验证
├── barcode         → 理想的 Identity 标识（如果完整）
├── specification   → Specification 的近似表示
└── unit            → Base Unit
```

**问题**: material_code 不能唯一标识 Canonical Identity，因为同一产品可能有多个 material_code（不同供应商）

#### inventory 表

```sql
inventory
├── material_id    → 外键，关联到 material_archives
├── batch_no       → 批次标识（不是 Identity）
├── production_date → 批次属性（不是 Identity）
└── expiry_date    → 批次属性（不是 Identity）
```

**结论**: inventory 表管理的是批次，不是 Canonical Identity

#### purchase_order_items 表

```sql
purchase_order_items
├── material_id    → 外键，关联到 material_archives
├── product_id     → 外键，关联到 product（已废弃）
└── quantity       → 采购数量
```

**结论**: purchase_order_items 通过 material_id 间接关联到 Canonical Identity

#### dish_recipe 表

```sql
dish_recipe
├── dish_id        → 外键，关联到 foods
├── ingredient_id  → 外键，关联到 material_archives
└── quantity       → 用量
```

**结论**: dish_recipe 通过 ingredient_id 间接关联到 Canonical Identity

### 8.2 现有系统的 Identity 映射挑战

| 挑战 | 说明 | 影响 |
|------|------|------|
| **编码不统一** | food_code ≠ material_code ≠ product.code | 无法直接关联 |
| **属性不完整** | 不同表的 Brand/Specification 信息不一致 | 无法准确判断 |
| **外键缺失** | foods ↔ product ↔ material_archives 无直接关联 | 需要复杂的映射逻辑 |
| **角色混淆** | 同一表承担多种角色 | Identity 与 Role 混淆 |

---

## 九、Identity Resolution 实现建议

### 9.1 Identity Resolution 流程

```
输入: 两个待比较的记录 (Record A, Record B)
输出: Identity 分类结果

步骤:
1. 提取属性
   ├── Brand_A, Brand_B
   ├── ProductFamily_A, ProductFamily_B
   └── Specification_A, Specification_B

2. 标准化属性
   ├── Brand 标准化 (去除空格、统一大小写)
   ├── ProductFamily 标准化 (统一命名)
   └── Specification 标准化 (统一格式)

3. 比较属性
   ├── IF Brand_A ≠ Brand_B → DIFFERENT_IDENTITY
   ├── IF ProductFamily_A ≠ ProductFamily_B → DIFFERENT_IDENTITY
   ├── IF Specification_A = Specification_B → SAME_IDENTITY
   └── IF Specification_A ≠ Specification_B → 深度分析

4. 深度分析
   ├── IF 差异仅为包装 → SAME_BASE_ITEM_DIFFERENT_PACKAGING
   ├── IF 差异为商业报价 → SAME_ITEM_DIFFERENT_COMMERCIAL_OFFERING
   └── ELSE → DIFFERENT_IDENTITY

5. 输出结果
   └── 返回 Identity 分类
```

### 9.2 属性标准化规则

#### Brand 标准化

```
输入: "可口可乐", "COCA-COLA", "coca cola", "可口可乐公司"
输出: "可口可乐"
```

**规则**:
1. 去除首尾空格
2. 统一为中文名称
3. 去除公司后缀（如"公司"、"集团"）

#### ProductFamily 标准化

```
输入: "可乐", "可口可乐可乐", "Cola"
输出: "可乐"
```

**规则**:
1. 去除品牌前缀
2. 统一为标准名称
3. 建立 ProductFamily 字典

#### Specification 标准化

```
输入: "330ml", "330毫升", "330ml罐", "330ML"
输出: "330ml"
```

**规则**:
1. 统一单位格式（ml ↔ 毫升）
2. 去除包装描述（如"罐"、"瓶"）
3. 保留数值和单位

### 9.3 Identity Resolution 算法

```python
def resolve_identity(record_a: dict, record_b: dict) -> str:
    """
    解析两个记录的 Identity 关系
    
    Args:
        record_a: 包含 brand, product_family, specification 的字典
        record_b: 包含 brand, product_family, specification 的字典
    
    Returns:
        Identity 分类结果
    """
    # 1. 标准化属性
    brand_a = standardize_brand(record_a['brand'])
    brand_b = standardize_brand(record_b['brand'])
    
    family_a = standardize_family(record_a['product_family'])
    family_b = standardize_family(record_b['product_family'])
    
    spec_a = standardize_spec(record_a['specification'])
    spec_b = standardize_spec(record_b['specification'])
    
    # 2. 比较 Brand
    if brand_a != brand_b:
        return 'DIFFERENT_IDENTITY'
    
    # 3. 比较 ProductFamily
    if family_a != family_b:
        return 'DIFFERENT_IDENTITY'
    
    # 4. 比较 Specification
    if spec_a == spec_b:
        return 'SAME_IDENTITY'
    
    # 5. 深度分析 Specification 差异
    if is_packaging_only_diff(spec_a, spec_b):
        return 'SAME_BASE_ITEM_DIFFERENT_PACKAGING'
    
    if is_commercial_offering_diff(record_a, record_b):
        return 'SAME_ITEM_DIFFERENT_COMMERCIAL_OFFERING'
    
    return 'DIFFERENT_IDENTITY'
```

---

## 十、验证与测试

### 10.1 测试用例

| 测试ID | Record A | Record B | 预期结果 | 说明 |
|--------|----------|----------|----------|------|
| TC-001 | 可口可乐330ml罐 | 可口可乐330ml罐 | SAME_IDENTITY | 完全相同 |
| TC-002 | 可口可乐330ml罐 | 可口可乐500ml瓶 | DIFFERENT_IDENTITY | 规格不同 |
| TC-003 | 可口可乐330ml罐 | 可口可乐330ml罐×24箱 | SAME_BASE_ITEM_DIFFERENT_PACKAGING | 仅包装不同 |
| TC-004 | 可口可乐330ml罐(堂食) | 可口可乐330ml罐(外卖) | SAME_ITEM_DIFFERENT_COMMERCIAL_OFFERING | 渠道不同 |
| TC-005 | 可口可乐330ml罐 | 百事可乐330ml罐 | DIFFERENT_IDENTITY | 品牌不同 |
| TC-006 | 可口可乐糖浆 | 可口可乐330ml罐 | DIFFERENT_IDENTITY | ProductFamily不同 |
| TC-007 | 可口可乐糖浆 | 可乐原液 | DIFFERENT_IDENTITY | 品牌不同 |
| TC-008 | 散装大米(东北) | 散装大米(泰国) | DIFFERENT_IDENTITY | 产地不同 |
| TC-009 | 可口可乐330ml罐(临期) | 可口可乐330ml罐(正常) | SAME_IDENTITY | 仅批次不同 |
| TC-010 | 可口可乐330ml罐(中国版) | 可口可乐330ml罐(美国版) | SAME_BASE_ITEM_DIFFERENT_PACKAGING | 版本不同 |

### 10.2 验证矩阵

| 验证项 | 说明 | 通过标准 |
|--------|------|----------|
| **Brand 标准化** | 不同格式的 Brand 能否正确标准化 | 100% 正确标准化 |
| **ProductFamily 标准化** | 不同命名的 ProductFamily 能否正确归一 | 100% 正确归一 |
| **Specification 标准化** | 不同格式的 Specification 能否正确标准化 | 95%+ 正确标准化 |
| **分类准确性** | 分类结果是否符合业务预期 | 100% 符合预期 |
| **跨域一致性** | 同一产品在不同域的分类是否一致 | 100% 一致 |

---

## 十一、实施路线图

### 11.1 Phase 1: 属性标准化 (2周)

- [ ] 建立 Brand 字典表
- [ ] 建立 ProductFamily 字典表
- [ ] 建立 Specification 标准化规则
- [ ] 实现属性标准化函数
- [ ] 编写单元测试

### 11.2 Phase 2: Identity Resolution 引擎 (3周)

- [ ] 实现 Identity Resolution 算法
- [ ] 实现跨域身份映射
- [ ] 实现分类判断逻辑
- [ ] 编写集成测试
- [ ] 性能优化

### 11.3 Phase 3: 数据迁移与验证 (4周)

- [ ] 现有数据清洗与标准化
- [ ] 建立 Canonical Identity 主数据
- [ ] 数据迁移与映射
- [ ] 数据验证与修复
- [ ] 业务验收测试

### 11.4 Phase 4: 系统集成 (3周)

- [ ] 集成到采购流程
- [ ] 集成到库存流程
- [ ] 集成到销售流程
- [ ] 集成到配方流程
- [ ] 端到端测试

---

## 十二、结论

### 12.1 核心发现

1. **Identity 构成**: Canonical Identity = Brand + ProductFamily + Specification
2. **关键区分**: Specification 是区分同一品牌下不同产品的关键属性
3. **包装差异**: 仅包装数量不同属于 SAME_BASE_ITEM_DIFFERENT_PACKAGING
4. **商业报价**: 渠道/价格差异属于 SAME_ITEM_DIFFERENT_COMMERCIAL_OFFERING

### 12.2 实施建议

1. **优先建立字典**: Brand 和 ProductFamily 字典是 Identity Resolution 的基础
2. **标准化优先**: 属性标准化是准确判断的前提
3. **渐进式迁移**: 先建立映射，再逐步迁移
4. **业务参与**: 分类规则需要业务人员确认

### 12.3 风险提示

1. **数据质量**: 现有数据的 Brand/Specification 信息可能不完整
2. **命名规范**: 需要建立统一的命名规范
3. **业务理解**: 业务人员可能对分类规则有不同理解
4. **性能影响**: Identity Resolution 可能影响查询性能

---

**Document Version**: 1.0  
**Last Updated**: 2026-09-09  
**Author**: Business Analysis Team  
**Status**: Draft  
**Phase**: Phase 2 - Identity Resolution Rules
