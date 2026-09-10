# Product / Food / Material 业务边界分析

> **文档类型**: 业务对象边界分析  
> **生成日期**: 2026-09-09  
> **状态**: ANALYSIS  
> **范围**: Product、Food、Material 三表关系、边界定义与架构定位

---

## 一、现状事实（代码审计）

### 1.1 三张表的字段清单

#### food 表（旧表，LEGACY）

| 字段 | 类型 | 语义 |
|------|------|------|
| food_code | VARCHAR(PK) | 菜品编码 |
| food_name | VARCHAR | 菜品名称 |
| food_category | VARCHAR | 菜品分类 |
| food_price | DECIMAL | 销售价 |
| cost_price | DECIMAL | 成本价 |
| food_desc | VARCHAR | 描述 |
| food_image | VARCHAR | 图片 |
| food_status | VARCHAR | 状态 |
| batch_number | VARCHAR | 批次号 |
| trace_code | VARCHAR | 追溯码 |
| manufacturer | VARCHAR | 生产商 |
| production_date | TIMESTAMP | 生产日期 |
| expiration_date | TIMESTAMP | 过期日期 |
| stock | INTEGER | 库存 |
| price | DECIMAL | 价格（冗余） |
| weight | DECIMAL | 重量 |
| quality_grade | VARCHAR | 质量等级 |
| shelf_life_days | INTEGER | 保质期天数 |
| production_address | VARCHAR | 产地 |
| nutrition_info | VARCHAR | 营养信息 |
| storage_conditions | VARCHAR | 存储条件 |
| rfid_tag | VARCHAR | RFID 标签 |
| device_id | VARCHAR | 设备ID |
| sensor_data | VARCHAR | 传感器数据 |

**状态**: `business-object-map.md:23` 标记为 `LEGACY`，已废弃。

---

#### product 表（遗留产品表，LEGACY）

| 字段 | 类型 | 语义 |
|------|------|------|
| product_id | BIGINT(PK,AUTO) | 产品ID |
| code | VARCHAR | 产品编码 |
| name | VARCHAR | 产品名称 |
| category_id | BIGINT | 分类ID |
| category_name | VARCHAR | 分类名称 |
| unit | VARCHAR | 计量单位 |
| price | DECIMAL | 销售价 |
| cost_price | DECIMAL | 成本价 |
| barcode | VARCHAR | 条形码 |
| specification | VARCHAR | 规格 |
| origin | VARCHAR | 产地 |
| supplier_id | BIGINT | 供应商ID |
| supplier_name | VARCHAR | 供应商名称 |
| status | BOOLEAN | 状态 |

**状态**: `business-object-map.md:24` 标记为 `LEGACY`，已废弃。  
**Entity 注释**: `Product.java:12` 注释为"产品表实体类（物料/原料）"。

---

#### material_archives 表（当前真相源）

| 字段 | 类型 | 语义 |
|------|------|------|
| material_id | BIGINT(PK,AUTO) | 物料ID |
| material_code | VARCHAR | 物料编码 |
| material_name | VARCHAR | 物料名称 |
| category_id | BIGINT | 分类ID（关联 material_categories） |
| unit | VARCHAR | 单位 |
| spec | VARCHAR | 规格型号 |
| reference_price | BIGINT | 参考价（分） |
| barcode | VARCHAR | 条码 |
| origin | VARCHAR | 产地 |
| shelf_life | VARCHAR | 保质期 |
| storage_condition | VARCHAR | 存储条件 |
| department_id | BIGINT | 使用部门ID |
| supplier_id | BIGINT | 主供应商ID |
| status | INTEGER | 状态：1启用 0停用 |
| remark | VARCHAR | 备注 |

**状态**: VERIFIED，当前采购/库存/追溯的真相源。

---

### 1.2 三张表的关联关系（从代码证据推导）

```
┌─────────────────────────────────────────────────────────────────┐
│                        业务关系全景图                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐        DishRecipe / DishRecipeNew             │
│  │   foods      │◄────── (food_id → material_id) ──────┐       │
│  │  (菜品/食品)  │       菜品配方 / BOM                   │       │
│  │              │                                        │       │
│  │ PK: food_code│                                        │       │
│  │ 价格: food_price                                      │       │
│  │ 库存: stock  │                                        │       │
│  │ 分类: food_category                                   │       │
│  └──────┬───────┘                                        │       │
│         │                                                 │       │
│         │ order_items.food_id                             │       │
│         │ order_items_new.food_id                         │       │
│         ▼                                                 │       │
│  ┌──────────────┐                                   ┌─────┴──────┐
│  │  订单/销售    │                                   │material_   │
│  │  (POS 收银)   │                                   │archives    │
│  │              │                                   │(物料档案)   │
│  │ 面向客户销售  │                                   │            │
│  └──────────────┘                                   │ PK:        │
│                                                     │ material_id│
│                                                     │ 价格:      │
│  ┌──────────────┐                                   │ reference_ │
│  │  库存         │◄──────── material_id ────────────│ price      │
│  │  (inventory)  │                                   │ 分类:      │
│  │              │                                   │ category_id│
│  │ 物料库存管理  │                                   │ 供应商:    │
│  │ 成本核算基础  │                                   │ supplier_id│
│  └──────────────┘                                   └─────┬──────┘
│                                                           │
│  ┌──────────────┐                                   ┌─────┴──────┐
│  │  采购订单     │◄──── material_id ─────────────────│            │
│  │(purchase_    │                                   └────────────┘
│  │ order_items) │
│  │              │
│  │ 采购物料/原料 │
│  └──────────────┘

  product 表: LEGACY，已废弃，不在当前业务链路中。
```

---

### 1.3 关键代码证据

| 关联点 | 代码位置 | 证据 |
|--------|----------|------|
| 库存→物料 | `Inventory.java:29` | `@TableField("material_id") private Long materialId;` |
| 库存兼容→产品 | `Inventory.java:382-388` | `getProductId()` 返回 `materialId`（兼容性映射） |
| 旧订单→菜品 | `OrderItem.java:31` | `@TableField("food_id") private String foodId;` |
| 新订单→菜品 | `OrderItemNew.java:36` | `@TableField("food_id") private Long foodId;` |
| 采购订单→物料 | `PurchaseOrderItem.java:44` | `@TableField("material_id") private Long materialId;` |
| 菜品配方→菜品+物料 | `DishRecipeNew.java:22-29` | `food_id` + `material_id` |
| 旧配方→菜品+配料 | `DishRecipe.java:24-33` | `dish_id` + `ingredient_id` |
| 订单原料需求 | `OrderMaterialRequirement.java:29-44` | `dish_id` + `material_id` |

---

## 二、边界问题分析

### 2.1 Product 是什么？—— Entity / Type / Role / Commercial Definition

| 维度 | 分析 |
|------|------|
| **Entity?** | product 表有独立主键 product_id、独立字段集，表面上是 Entity |
| **Type?** | 不是类型枚举，不是分类体系 |
| **Role?** | 不是某个对象在特定场景下的角色 |
| **Commercial Definition?** | 不是商业定义（如 SKU、价格策略） |

**结论**: Product 是一个**被废弃的遗留 Entity**。

`Product.java:12` 注释明确写道："产品表实体类（物料/原料）"——这个注释本身就暴露了 Product 的真实身份：它本意是想做 Material 的上层概念，但最终 Material（material_archives）独立发展成为采购/库存/追溯的真相源，而 Product 被标记为 LEGACY。

**证据链**:
1. `business-object-map.md:24` → `product (遗留产品表) | LEGACY`
2. `legacy-map.md:14` → `product | 遗留产品表 | 已废弃`
3. `Inventory.java:382-388` → `getProductId()` 实际返回 `materialId`（兼容性别名）
4. 当前所有采购链路使用 `material_id`，不使用 `product_id`

---

### 2.2 Food 与 Material 是什么关系？

**不是继承关系，而是两个独立业务对象，通过 Recipe/BOM 建立生产关系。**

| 判据 | Food | Material |
|------|------|----------|
| **业务角色** | 可销售的成品/菜品 | 不可直接销售的原料/物料 |
| **库存归属** | foods.stock（菜品层可售量，下单即扣） | inventory.material_id（物料层实物，出餐时按 BOM 扣） |
| **价格语义** | food_price = 售价（面向客户） | reference_price = 参考价/采购价（面向供应商） |
| **成本核算** | cost_price = 菜品成本（= Σ 原料用量 × 原料单价） | reference_price / 最新入库价 = 物料成本 |
| **采购对象** | 不是 | 是（purchase_order_items.material_id） |
| **销售对象** | 是（order_items.food_id） | 不是 |
| **追溯链** | food_code → batch_number → trace_code | material_code → material_trace_code |
| **分类体系** | food_category（字符串） | material_categories（独立分类表） |

**关系模型**:

```
Material (原料) ──Recipe/BOM──► Food (菜品)
   │                              │
   │ 采购入库                      │ 销售出库
   │ 实物库存                      │ 可售库存
   │ 成本基础                      │ 收入来源
   ▼                              ▼
Purchase ──────────────► Order/Sales
```

---

### 2.3 Product 是否是统一销售商品的上层概念？

**否。Product 不是上层概念，也不是当前系统中的活跃角色。**

分析:
1. 当前销售链路（POS）直接读 `foods` 表，不经过 `product`
2. 当前采购链路直接读 `material_archives` 表，不经过 `product`
3. 库存管理直接关联 `material_id`，`getProductId()` 是兼容性别名
4. Product 表字段集与 Material 高度重叠（code, name, category_id, unit, price, cost_price, barcode, specification, origin, supplier_id, supplier_name, status），说明 Product 本意就是 Material 的早期形态

**如果需要一个"统一销售商品"的上层概念**，应该考虑:
- 菜品（Food）= 可销售的成品（POS 收银对象）
- 套餐（DishComboNew）= 多个菜品的组合销售单元
- 物料（Material）= 不可直接销售的原料（采购/库存对象）

Product 在这个体系中没有独立的业务位置。

---

### 2.4 Food 与 Material 能否合并？

**不能，也不应该。**

| 合并风险 | 说明 |
|----------|------|
| **语义混淆** | 菜品（可销售、有售价）和物料（不可销售、有采购价）是完全不同的业务概念 |
| **库存模型冲突** | 菜品库存 = 下单即扣/取消回补（可售量）；物料库存 = 出餐按 BOM 扣（实物量）。两种库存模型不能混在同一张表 |
| **成本核算混乱** | 菜品成本 = Σ(原料用量 × 原料单价) 是派生值；物料成本 = 最新入库价/加权平均。合并后无法区分 |
| **追溯链断裂** | 食品追溯需要区分"成品追溯"和"原料追溯"，合并后追溯链无法建立 |
| **PD-030 已决策** | 产品负责人已确认"菜品层可售量 = 下单即扣、物料层实物 = 出餐按 BOM 扣"的双层扣减模型 |

---

## 三、建议的领域模型（理想状态）

```
┌─────────────────────────────────────────────────────────────────┐
│                    理想领域模型                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐     DishRecipe      ┌──────────────┐          │
│  │ Dish (菜品)  │◄──── (BOM) ────────│Material (物料)│          │
│  │             │                     │              │          │
│  │ • dishId    │                     │ • materialId │          │
│  │ • name      │                     │ • name       │          │
│  │ • category  │                     │ • category   │          │
│  │ • salePrice │                     │ • purchasePrice│        │
│  │ • costPrice │ (派生=Σ原料成本)     │ • supplier   │          │
│  │ • stock     │ (可售量，下单即扣)    │ • unit       │          │
│  │ • status    │                     │ • spec       │          │
│  └──────┬──────┘                     └──────┬───────┘          │
│         │                                    │                  │
│         │ OrderItem                         │ Inventory         │
│         ▼                                    ▼                  │
│  ┌─────────────┐                     ┌──────────────┐          │
│  │Order (订单)  │                     │Inventory (库存)│         │
│  │             │                     │              │          │
│  │ • orderId   │                     │ • materialId │          │
│  │ • items[]   │                     │ • quantity   │          │
│  │ • total     │                     │ • batchNo    │          │
│  │ • status    │                     │ • unitCost   │          │
│  └─────────────┘                     └──────────────┘          │
│                                                                 │
│  ┌─────────────┐                     ┌──────────────┐          │
│  │Combo (套餐)  │──contains──►Dish   │Purchase (采购)│          │
│  │             │                     │              │          │
│  │ • comboId   │                     │ • materialId │          │
│  │ • price     │                     │ • quantity   │          │
│  │ • items[]   │                     │ • unitPrice  │          │
│  └─────────────┘                     └──────────────┘          │
│                                                                 │
│  Product 表: 废弃，不纳入领域模型。                              │
│  food 表(旧): 废弃，已由 foods 表替代。                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、遗留表处置建议

| 表 | 当前状态 | 处置建议 | 优先级 |
|----|----------|----------|--------|
| `product` | LEGACY，已废弃 | 维持废弃状态，不新增引用；如仍有代码引用，迁移到 material_archives | P2 |
| `food`（旧表） | LEGACY，已废弃 | 维持废弃状态；POS 扫码仍读旧表的需迁移到 foods | P1 |
| `foods` | VERIFIED，当前真相源 | 作为 Dish/菜品 的真相源继续使用 | 持续 |
| `material_archives` | VERIFIED，当前真相源 | 作为 Material/物料 的真相源继续使用 | 持续 |

---

## 五、已知问题与决策依赖

### 5.1 已决策

| 决策 | 来源 | 结论 |
|------|------|------|
| PD-022 千店千面 | 产品负责人 2026-08-18 | 集团统一 + 门店差异化为方向；集团发布 ≠ 门店销售 |
| PD-023 PADR-001 | 产品负责人 2026-08-18 | 全产品形态顶层决策确认 |
| PD-030 库存扣减分层 | 产品负责人 2026-08-31 | 菜品层可售量=下单即扣，物料层实物=出餐按BOM扣 |

### 5.2 待决策

| 问题 | 影响 | 建议 |
|------|------|------|
| Food 与 Material 是否需要明确的"可售/不可售"类型标记 | 未来门店差异化经营范围 | 在 foods 表增加 `is_sellable` 或复用 `food_status` 明确标记 |
| dish_recipe.ingredient_id 实际指向 material_id 还是 product_id | 配方准确性 | 审计 ingredient_id 的实际数据来源，确认指向 material_archives.material_id |
| foods.stock 与 inventory 的双库存同步机制 | 库存一致性 | 按 PD-030 决策实现双层扣减模型 |

---

## 六、总结

| 问题 | 结论 |
|------|------|
| Product 是否是统一销售商品的上层概念？ | **否**。Product 是遗留废弃表，当前系统中无活跃业务角色。 |
| Food 与 Material 是不是继承关系？ | **否**。两者是独立业务对象：Food=可销售成品，Material=不可销售原料。 |
| Food 与 Material 是两个独立业务对象，通过 Recipe/BOM 建立关系？ | **是**。DishRecipe / DishRecipeNew 是连接两者的桥梁。 |
| Product 是 Entity、Type、Role 还是 Commercial Definition？ | **已废弃的遗留 Entity**。本意是 Material 的早期形态，现已被 material_archives 替代。 |

---

*文档结束*
