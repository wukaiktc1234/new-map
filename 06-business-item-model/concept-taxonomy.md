# Concept Taxonomy - 餐饮ERP系统概念分类 (Phase 1)

## 分析目标

对餐饮ERP系统的16个核心概念进行多维度分类，建立清晰的概念分类体系。

## 分析原则

- **CURRENT REALITY**: 当前数据库表结构和代码实现
- **TARGET BUSINESS SEMANTICS**: 目标业务语义模型
- **TARGET TECHNICAL MODEL**: 目标技术实现模型

## 分类维度说明

| 维度 | 说明 | 示例 |
|------|------|------|
| ENTITY | 系统中的独立实体对象 | 客户、订单、商品 |
| IDENTITY | 用于唯一标识的属性或机制 | 条形码、SKU编码 |
| TYPE | 实体的具体类型或子类 | 软饮料、主食 |
| CLASSIFICATION | 分类体系中的分类标签 | 食品类、饮料类 |
| ROLE | 实体在业务流程中的角色 | 原料、成品、半成品 |
| CAPABILITY | 实体具备的能力或功能 | 可销售、可采购、可库存 |
| RELATIONSHIP | 实体间的关系定义 | 配方关系、供应关系 |
| PROFILE | 实体的属性集合或特征描述 | 价格信息、库存信息 |
| SCOPE | 实体的适用范围或边界 | 门店范围、仓库范围 |
| STATE | 实体的状态或生命周期 | 在库、已售、过期 |
| ATTRIBUTE | 实体的可度量属性 | 数量、重量、价格 |
| DERIVED | 从其他属性计算得出的值 | 库存总值、成本价 |

---

## 1. Product (产品)

```yaml
concept: Product
category: 
  - ENTITY
  - CLASSIFICATION
definition: >
  产品是指系统中可管理的所有物品的基础抽象概念，
  是Material在不同业务场景下的业务化表达。
  Product强调"可被交易和管理的物品"这一核心属性。
why: >
  - Product是ENTITY，因为它代表系统中独立存在的可管理对象
  - Product是CLASSIFICATION，因为它对物品进行了业务分类（区分非商品类物品）
  - Product不等同于Material，Material是更底层的物理属性描述
alternatives:
  - 纯粹的ENTITY：仅作为实体存在，不包含分类含义
  - 纯粹的TYPE：仅作为类型标签
evidence: |
  CURRENT REALITY:
  - product表标记为LEGACY，实际已被material_archives替代
  - purchase_order_items表同时引用product_id和material_id
  - 系统中存在product表但使用率低
  
  TARGET BUSINESS SEMANTICS:
  - 可乐：Product（可销售的饮料产品）
  - 鸡翅：Product（可销售的食品）
  - 大米：Product（可采购的原材料）
  - 酱油：Product（可采购的调味品）
  - 矿泉水：Product（可销售的饮料）
  - 薯片：Product（可销售的零食）
  - 餐盒：Product（可采购的耗材）
  - 宫保鸡丁：Product（可销售的菜品）
  - 可乐鸡翅：Product（可销售的菜品）
confidence: high
```

---

## 2. Food (食品/菜品)

```yaml
concept: Food
category:
  - ENTITY
  - TYPE
definition: >
  Food是指可直接销售给顾客的餐饮产品，包括成品菜品、饮品等。
  Food是Product的一个具体类型，强调"可直接食用/饮用"的属性。
why: >
  - Food是ENTITY，因为foods表是系统中的核心实体表
  - Food是TYPE，因为它是Product的一个具体类型（区别于原材料）
  - Food的核心特征：可直接销售、有销售价格、需要配方或直接供应
alternatives:
  - 纯粹的ENTITY：忽略其作为类型的特征
  - 纯粹的TYPE：忽略其作为独立实体的存在
  - CLASSIFICATION：不准确，因为Food不是分类标签而是实体类型
evidence: |
  CURRENT REALITY:
  - foods表：food_code, food_name, food_price, cost_price, food_category, stock
  - Java Entity额外字段：shelf_life_days, production_address, nutrition_info, 
    storage_conditions, price, weight, quality_grade
  - order_items表引用food_id
  
  TARGET BUSINESS SEMANTICS:
  - 宫保鸡丁：Food（可直接销售的菜品）
  - 可乐鸡翅：Food（可直接销售的菜品）
  - 可乐：Food（可直接销售的饮料）
  - 矿泉水：Food（可直接销售的饮料）
  - 大米：NOT Food（是原材料，不是直接销售的食品）
  - 酱油：NOT Food（是调味品，不是直接销售的食品）
  - 薯片：Food（可直接销售的零食）
confidence: high
```

---

## 3. Material (物料/商品档案)

```yaml
concept: Material
category:
  - ENTITY
  - PROFILE
definition: >
  Material是指系统中所有物品的基础属性描述，包括物理属性、规格、单位等。
  Material是底层的数据模型，为Product、Food等提供基础属性支撑。
why: >
  - Material是ENTITY，因为material_archives表是独立的实体表
  - Material是PROFILE，因为它定义了物品的属性集合（代码、名称、规格、单位等）
  - Material不等同于Product，它更强调物理属性而非业务属性
alternatives:
  - 纯粹的ENTITY：忽略其作为属性集合的特征
  - 纯粹的TYPE：不准确，Material不是类型而是属性描述
  - CLASSIFICATION：不准确，Material不是分类标签
evidence: |
  CURRENT REALITY:
  - material_archives表：material_id, material_code, material_name, category_id, 
    unit, spec, reference_price, supplier_id, barcode, origin, shelf_life, 
    storage_condition
  - inventory表引用material_id
  - store_inventory表引用material_id
  - purchase_order_items表引用material_id
  
  TARGET BUSINESS SEMANTICS:
  - 可乐：Material（有具体规格、单位、保质期等属性）
  - 鸡翅：Material（有具体规格、单位、保质期等属性）
  - 大米：Material（有具体规格、单位、保质期等属性）
  - 酱油：Material（有具体规格、单位、保质期等属性）
  - 矿泉水：Material（有具体规格、单位、保质期等属性）
  - 薯片：Material（有具体规格、单位、保质期等属性）
  - 餐盒：Material（有具体规格、单位等属性）
  - 宫保鸡丁：Material（有具体规格、单位等属性）
  - 可乐鸡翅：Material（有具体规格、单位等属性）
confidence: high
```

---

## 4. Beverage (饮料)

```yaml
concept: Beverage
category:
  - TYPE
  - CLASSIFICATION
definition: >
  Beverage是指可饮用的液体类产品，是Food的一个子类型。
  强调"液态、可直接饮用"的物理和消费特征。
why: >
  - Beverage是TYPE，因为它是Food的一个具体类型
  - Beverage是CLASSIFICATION，因为它对产品进行了品类分类
  - Beverage的核心特征：液态、可直接饮用、通常有包装规格
alternatives:
  - 纯粹的TYPE：忽略其作为分类标签的特征
  - 纯粹的CLASSIFICATION：忽略其作为类型子类的特征
  - ENTITY：不准确，Beverage不是独立实体而是类型
evidence: |
  CURRENT REALITY:
  - foods表中food_category字段可包含饮料类别
  - 没有独立的beverage表，通过category区分
  
  TARGET BUSINESS SEMANTICS:
  - 可乐：Beverage（液态、可直接饮用）
  - 矿泉水：Beverage（液态、可直接饮用）
  - 鸡翅：NOT Beverage（固态食品）
  - 大米：NOT Beverage（固态原材料）
  - 酱油：NOT Beverage（液态调味品，不可直接饮用）
  - 薯片：NOT Beverage（固态零食）
  - 宫保鸡丁：NOT Beverage（固态菜品）
confidence: high
```

---

## 5. Ingredient (原料/配料)

```yaml
concept: Ingredient
category:
  - ROLE
  - RELATIONSHIP
definition: >
  Ingredient是指用于制作菜品的原材料，强调在生产/制作过程中的角色。
  Ingredient是Material在"作为原料被消耗"这一业务场景下的角色定义。
why: >
  - Ingredient是ROLE，因为它是Material在配方关系中扮演的角色
  - Ingredient是RELATIONSHIP，因为它定义了"被配方引用"的关系
  - Ingredient不等同于Material，同一Material可以是Ingredient也可以是Sellable
alternatives:
  - 纯粹的ROLE：忽略其作为关系定义的特征
  - 纯粹的RELATIONSHIP：忽略其作为角色的特征
  - TYPE：不准确，Ingredient不是类型而是角色
  - ENTITY：不准确，Ingredient不是独立实体
evidence: |
  CURRENT REALITY:
  - dish_recipe表：dish_id, ingredient_id, ingredient_name, quantity, unit, 
    estimated_cost, actual_cost
  - ingredient_id引用material_id或product_id
  
  TARGET BUSINESS SEMANTICS:
  - 大米：Ingredient（在宫保鸡丁中作为原料）
  - 酱油：Ingredient（在宫保鸡丁中作为调味品）
  - 可乐：NOT Ingredient（直接销售，不作为原料）
  - 矿泉水：NOT Ingredient（直接销售，不作为原料）
  - 鸡翅：Ingredient（在可乐鸡翅中作为原料）
  - 可乐鸡翅中的可乐：Ingredient（作为调味/配料）
confidence: high
```

---

## 6. Sellable (可销售)

```yaml
concept: Sellable
category:
  - CAPABILITY
  - ROLE
definition: >
  Sellable是指具备销售能力的物品属性，表示该物品可以直接销售给顾客。
  Sellable是Product的一个能力维度，定义了"可被销售"这一业务能力。
why: >
  - Sellable是CAPABILITY，因为它描述了物品"能做什么"的能力
  - Sellable是ROLE，因为它定义了物品在销售流程中的角色
  - Sellable不等同于Product，Product可以是不可直接销售的原材料
alternatives:
  - 纯粹的CAPABILITY：忽略其作为角色的特征
  - 纯粹的ROLE：忽略其作为能力的特征
  - TYPE：不准确，Sellable不是类型而是能力
  - ATTRIBUTE：不准确，Sellable不是简单的属性值
evidence: |
  CURRENT REALITY:
  - foods表有food_price字段，表示可销售
  - order_items表引用food_id，表示可被销售
  - material_archives表有reference_price，但不一定直接销售
  
  TARGET BUSINESS SEMANTICS:
  - 可乐：Sellable（可直接销售给顾客）
  - 矿泉水：Sellable（可直接销售给顾客）
  - 薯片：Sellable（可直接销售给顾客）
  - 宫保鸡丁：Sellable（可直接销售给顾客）
  - 可乐鸡翅：Sellable（可直接销售给顾客）
  - 大米：NOT Sellable（作为原材料，不直接销售）
  - 酱油：NOT Sellable（作为调味品，不直接销售）
  - 餐盒：NOT Sellable（作为耗材，不直接销售）
confidence: high
```

---

## 7. Purchasable (可采购)

```yaml
concept: Purchasable
category:
  - CAPABILITY
  - ROLE
definition: >
  Purchasable是指具备采购能力的物品属性，表示该物品可以从供应商处采购。
  Purchasable是Material的一个能力维度，定义了"可被采购"这一业务能力。
why: >
  - Purchasable是CAPABILITY，因为它描述了物品"能被采购"的能力
  - Purchasable是ROLE，因为它定义了物品在采购流程中的角色
  - Purchasable不等同于Sellable，可采购的物品不一定可直接销售
alternatives:
  - 纯粹的CAPABILITY：忽略其作为角色的特征
  - 纯粹的ROLE：忽略其作为能力的特征
  - TYPE：不准确，Purchasable不是类型而是能力
  - ATTRIBUTE：不准确，Purchasable不是简单的属性值
evidence: |
  CURRENT REALITY:
  - purchase_order_items表引用product_id和material_id
  - material_archives表有supplier_id字段
  - supplier_id表示可以从该供应商采购
  
  TARGET BUSINESS SEMANTICS:
  - 大米：Purchasable（可以从供应商采购）
  - 酱油：Purchasable（可以从供应商采购）
  - 餐盒：Purchasable（可以从供应商采购）
  - 可乐：Purchasable（可以从供应商采购）
  - 矿泉水：Purchasable（可以从供应商采购）
  - 薯片：Purchasable（可以从供应商采购）
  - 宫保鸡丁：NOT Purchasable（自制菜品，不从外部采购）
  - 可乐鸡翅：NOT Purchasable（自制菜品，不从外部采购）
confidence: high
```

---

## 8. Stockable (可库存)

```yaml
concept: Stockable
category:
  - CAPABILITY
  - SCOPE
definition: >
  Stockable是指具备库存能力的物品属性，表示该物品可以在仓库中存储和管理。
  Stockable是Material的一个能力维度，定义了"可被库存"这一业务能力。
why: >
  - Stockable是CAPABILITY，因为它描述了物品"能被库存"的能力
  - Stockable是SCOPE，因为它定义了物品在库存管理中的适用范围
  - Stockable不等同于Purchasable，可库存的物品不一定可采购
alternatives:
  - 纯粹的CAPABILITY：忽略其作为范围的特征
  - 纯粹的SCOPE：忽略其作为能力的特征
  - TYPE：不准确，Stockable不是类型而是能力
  - ATTRIBUTE：不准确，Stockable不是简单的属性值
evidence: |
  CURRENT REALITY:
  - inventory表：material_id, warehouse_id, current_stock, unit_cost, total_cost, batch_no
  - store_inventory表：store_id, material_id, current_stock, unit
  - foods表有stock字段
  
  TARGET BUSINESS SEMANTICS:
  - 大米：Stockable（可以在仓库中存储）
  - 酱油：Stockable（可以在仓库中存储）
  - 餐盒：Stockable（可以在仓库中存储）
  - 可乐：Stockable（可以在仓库中存储）
  - 矿泉水：Stockable（可以在仓库中存储）
  - 薯片：Stockable（可以在仓库中存储）
  - 宫保鸡丁：NOT Stockable（现做现卖，不库存）
  - 可乐鸡翅：NOT Stockable（现做现卖，不库存）
confidence: high
```

---

## 9. Consumable (可消耗)

```yaml
concept: Consumable
category:
  - CAPABILITY
  - STATE
definition: >
  Consumable是指具备消耗能力的物品属性，表示该物品可以在生产过程中被消耗。
  Consumable是Material在"作为原料被消耗"这一业务场景下的能力定义。
why: >
  - Consumable是CAPABILITY，因为它描述了物品"能被消耗"的能力
  - Consumable是STATE，因为它涉及物品从"有库存"到"被消耗"的状态变化
  - Consumable不等同于Ingredient，Ingredient是角色，Consumable是能力
alternatives:
  - 纯粹的CAPABILITY：忽略其作为状态的特征
  - 纯粹的STATE：忽略其作为能力的特征
  - TYPE：不准确，Consumable不是类型而是能力
  - ROLE：不准确，Consumable是能力而非角色
evidence: |
  CURRENT REALITY:
  - dish_recipe表定义了菜品配方，原料会被消耗
  - inventory表的current_stock会随着消耗而减少
  
  TARGET BUSINESS SEMANTICS:
  - 大米：Consumable（在制作宫保鸡丁时被消耗）
  - 酱油：Consumable（在制作宫保鸡丁时被消耗）
  - 可乐：Consumable（在制作可乐鸡翅时被消耗）
  - 鸡翅：Consumable（在制作可乐鸡翅时被消耗）
  - 矿泉水：NOT Consumable（直接销售，不作为原料消耗）
  - 薯片：NOT Consumable（直接销售，不作为原料消耗）
  - 餐盒：Consumable（在包装时被消耗）
confidence: high
```

---

## 10. Recipe Component (配方组件)

```yaml
concept: Recipe Component
category:
  - RELATIONSHIP
  - ROLE
definition: >
  Recipe Component是指菜品配方中的组成部分，定义了原料与菜品之间的配方关系。
  Recipe Component是Ingredient在"作为配方组成部分"这一关系中的具体表达。
why: >
  - Recipe Component是RELATIONSHIP，因为它定义了"菜品-原料"之间的关系
  - Recipe Component是ROLE，因为它定义了原料在配方中的角色
  - Recipe Component不等同于Ingredient，它是Ingredient在配方关系中的具体化
alternatives:
  - 纯粹的RELATIONSHIP：忽略其作为角色的特征
  - 纯粹的ROLE：忽略其作为关系的特征
  - TYPE：不准确，Recipe Component不是类型而是关系
  - ENTITY：不准确，Recipe Component不是独立实体
evidence: |
  CURRENT REALITY:
  - dish_recipe表：dish_id, ingredient_id, ingredient_name, quantity, unit, 
    estimated_cost, actual_cost
  - 这是典型的配方关系表
  
  TARGET BUSINESS SEMANTICS:
  - 大米在宫保鸡丁中：Recipe Component（作为米饭配料）
  - 酱油在宫保鸡丁中：Recipe Component（作为调味品）
  - 可乐在可乐鸡翅中：Recipe Component（作为调味/配料）
  - 鸡翅在可乐鸡翅中：Recipe Component（作为主料）
  - 可乐作为独立商品：NOT Recipe Component（直接销售，不是配方组件）
confidence: high
```

---

## 11. Stock Item (库存项)

```yaml
concept: Stock Item
category:
  - ENTITY
  - PROFILE
definition: >
  Stock Item是指库存管理系统中的具体库存记录，包含库存数量、成本、批次等信息。
  Stock Item是Material在库存管理中的具体化，强调"可被库存管理"的属性。
why: >
  - Stock Item是ENTITY，因为inventory表是独立的实体表
  - Stock Item是PROFILE，因为它定义了库存的属性集合（数量、成本、批次等）
  - Stock Item不等同于Material，它是Material在库存管理中的具体化
alternatives:
  - 纯粹的ENTITY：忽略其作为属性集合的特征
  - 纯粹的PROFILE：忽略其作为实体的特征
  - TYPE：不准确，Stock Item不是类型而是实体
  - CAPABILITY：不准确，Stock Item是实体而非能力
evidence: |
  CURRENT REALITY:
  - inventory表：material_id, warehouse_id, current_stock, unit_cost, total_cost, batch_no
  - store_inventory表：store_id, material_id, current_stock, unit
  - 这是典型的库存记录表
  
  TARGET BUSINESS SEMANTICS:
  - 大米在仓库A的库存：Stock Item（有具体库存记录）
  - 酱油在仓库B的库存：Stock Item（有具体库存记录）
  - 可乐在门店的库存：Stock Item（有具体库存记录）
  - 宫保鸡丁：NOT Stock Item（现做现卖，不作为库存项管理）
confidence: high
```

---

## 12. SKU (库存保有单位)

```yaml
concept: SKU
category:
  - IDENTITY
  - ATTRIBUTE
definition: >
  SKU是指库存保有单位（Stock Keeping Unit），是用于唯一标识库存物品的编码。
  SKU是Material的一个标识属性，强调"唯一标识"的功能。
why: >
  - SKU是IDENTITY，因为它用于唯一标识库存物品
  - SKU是ATTRIBUTE，因为它是Material的一个可度量属性
  - SKU不等同于Barcode，SKU是内部编码，Barcode是外部条码
alternatives:
  - 纯粹的IDENTITY：忽略其作为属性的特征
  - 纯粹的ATTRIBUTE：忽略其作为标识的特征
  - TYPE：不准确，SKU不是类型而是标识
  - ENTITY：不准确，SKU不是独立实体
evidence: |
  CURRENT REALITY:
  - material_archives表有material_code字段，可能是SKU
  - 没有独立的SKU字段，但material_code可能作为SKU使用
  
  TARGET BUSINESS SEMANTICS:
  - 可乐（330ml罐装）：SKU = "BEV-COLA-330ML"（唯一标识）
  - 可乐（500ml瓶装）：SKU = "BEV-COLA-500ML"（唯一标识）
  - 鸡翅（冷冻500g）：SKU = "FOD-WING-500G"（唯一标识）
  - 大米（10kg装）：SKU = "MAT-RICE-10KG"（唯一标识）
  - 酱油（500ml瓶装）：SKU = "MAT-SOY-500ML"（唯一标识）
confidence: high
```

---

## 13. Barcode (条形码)

```yaml
concept: Barcode
category:
  - IDENTITY
  - ATTRIBUTE
definition: >
  Barcode是指物品的条形码，是用于扫描识别的外部标识。
  Barcode是Material的一个标识属性，强调"可扫描识别"的功能。
why: >
  - Barcode是IDENTITY，因为它用于唯一标识物品（通过扫描）
  - Barcode是ATTRIBUTE，因为它是Material的一个可度量属性
  - Barcode不等同于SKU，Barcode是外部条码，SKU是内部编码
alternatives:
  - 纯粹的IDENTITY：忽略其作为属性的特征
  - 纯粹的ATTRIBUTE：忽略其作为标识的特征
  - TYPE：不准确，Barcode不是类型而是标识
  - ENTITY：不准确，Barcode不是独立实体
evidence: |
  CURRENT REALITY:
  - material_archives表有barcode字段
  - 这是典型的条形码存储字段
  
  TARGET BUSINESS SEMANTICS:
  - 可乐（330ml罐装）：Barcode = "6901028075831"（条形码）
  - 可乐（500ml瓶装）：Barcode = "6901028075848"（条形码）
  - 鸡翅（冷冻500g）：Barcode = "6928804011537"（条形码）
  - 大米（10kg装）：Barcode = "6902083886540"（条形码）
  - 酱油（500ml瓶装）：Barcode = "6902265510018"（条形码）
confidence: high
```

---

## 14. Unit (单位)

```yaml
concept: Unit
category:
  - ENTITY
  - ATTRIBUTE
definition: >
  Unit是指物品的计量单位，是用于度量物品数量的标准单位。
  Unit是Material的一个属性，强调"可度量"的功能。
why: >
  - Unit是ENTITY，因为inventory_unit表是独立的实体表
  - Unit是ATTRIBUTE，因为它是Material的一个可度量属性
  - Unit不等同于Quantity，Unit是单位标准，Quantity是具体数量
alternatives:
  - 纯粹的ENTITY：忽略其作为属性的特征
  - 纯粹的ATTRIBUTE：忽略其作为实体的特征
  - TYPE：不准确，Unit不是类型而是单位
  - IDENTITY：不准确，Unit不是标识而是度量标准
evidence: |
  CURRENT REALITY:
  - inventory_unit表：id, name, code, type: weight/volume/count
  - material_archives表有unit字段
  - dish_recipe表有unit字段
  - order_items表有unit字段
  
  TARGET BUSINESS SEMANTICS:
  - 可乐（330ml罐装）：Unit = "罐"（count类型）
  - 可乐（500ml瓶装）：Unit = "瓶"（count类型）
  - 鸡翅（冷冻500g）：Unit = "kg"（weight类型）
  - 大米（10kg装）：Unit = "kg"（weight类型）
  - 酱油（500ml瓶装）：Unit = "瓶"（count类型）
  - 矿泉水（550ml瓶装）：Unit = "瓶"（count类型）
  - 薯片（104g袋装）：Unit = "袋"（count类型）
  - 餐盒（100个装）：Unit = "个"（count类型）
  - 宫保鸡丁：Unit = "份"（count类型）
  - 可乐鸡翅：Unit = "份"（count类型）
confidence: high
```

---

## 15. Warehouse (仓库)

```yaml
concept: Warehouse
category:
  - ENTITY
  - SCOPE
definition: >
  Warehouse是指物品存储的物理场所，是库存管理的范围边界。
  Warehouse是库存管理的一个范围维度，定义了"库存在哪里"的问题。
why: >
  - Warehouse是ENTITY，因为warehouse表是独立的实体表
  - Warehouse是SCOPE，因为它定义了库存管理的范围边界
  - Warehouse不等同于Store，Warehouse是存储场所，Store是销售场所
alternatives:
  - 纯粹的ENTITY：忽略其作为范围的特征
  - 纯粹的SCOPE：忽略其作为实体的特征
  - TYPE：不准确，Warehouse不是类型而是场所
  - ATTRIBUTE：不准确，Warehouse不是属性而是实体
evidence: |
  CURRENT REALITY:
  - inventory表有warehouse_id字段
  - 这是典型的仓库关联字段
  
  TARGET BUSINESS SEMANTICS:
  - 大米在仓库A：Warehouse = "中央仓库"（存储场所）
  - 酱油在仓库B：Warehouse = "调味品仓库"（存储场所）
  - 可乐在门店：Warehouse = "门店仓库"（存储场所）
  - 餐盒在仓库：Warehouse = "耗材仓库"（存储场所）
  - 宫保鸡丁：NOT Warehouse（现做现卖，不存储）
confidence: high
```

---

## 16. Price (价格)

```yaml
concept: Price
category:
  - ATTRIBUTE
  - DERIVED
definition: >
  Price是指物品的价值度量，是用于交易的价值表示。
  Price是Product的一个属性，强调"可交易"的功能。
why: >
  - Price是ATTRIBUTE，因为它是Product的一个可度量属性
  - Price是DERIVED，因为价格可能从成本、利润率等计算得出
  - Price不等同于Cost，Price是销售价格，Cost是成本价格
alternatives:
  - 纯粹的ATTRIBUTE：忽略其作为派生值的特征
  - 纯粹的DERIVED：忽略其作为属性的特征
  - TYPE：不准确，Price不是类型而是属性
  - ENTITY：不准确，Price不是独立实体
evidence: |
  CURRENT REALITY:
  - foods表有food_price字段
  - material_archives表有reference_price字段
  - order_items表有unit_price字段
  - purchase_order_items表有unit_price字段
  - inventory表有unit_cost字段
  
  TARGET BUSINESS SEMANTICS:
  - 可乐（330ml罐装）：Price = 3.00元（销售价格）
  - 可乐（500ml瓶装）：Price = 5.00元（销售价格）
  - 鸡翅（冷冻500g）：Price = 25.00元（采购价格）
  - 大米（10kg装）：Price = 80.00元（采购价格）
  - 酱油（500ml瓶装）：Price = 15.00元（采购价格）
  - 矿泉水（550ml瓶装）：Price = 2.00元（销售价格）
  - 薯片（104g袋装）：Price = 8.00元（销售价格）
  - 餐盒（100个装）：Price = 50.00元（采购价格）
  - 宫保鸡丁：Price = 38.00元（销售价格）
  - 可乐鸡翅：Price = 42.00元（销售价格）
confidence: high
```

---

## 概念关系矩阵

| 概念 | ENTITY | IDENTITY | TYPE | CLASSIFICATION | ROLE | CAPABILITY | RELATIONSHIP | PROFILE | CONTEXT | SCOPE | STATE | ATTRIBUTE | DERIVED |
|------|--------|----------|------|----------------|------|------------|--------------|---------|---------|-------|-------|-----------|---------|
| Product | ✓ | | | ✓ | | | | | | | | | |
| Food | ✓ | | ✓ | | | | | | | | | | |
| Material | ✓ | | | | | | | ✓ | | | | | |
| Beverage | | | ✓ | ✓ | | | | | | | | | |
| Ingredient | | | | | ✓ | | ✓ | | | | | | |
| Sellable | | | | | ✓ | ✓ | | | | | | | |
| Purchasable | | | | | ✓ | ✓ | | | | | | | |
| Stockable | | | | | | ✓ | | | | ✓ | | | |
| Consumable | | | | | | ✓ | | | | | ✓ | | |
| Recipe Component | | | | | ✓ | | ✓ | | | | | | |
| Stock Item | ✓ | | | | | | | ✓ | | | | | |
| SKU | | ✓ | | | | | | | | | | ✓ | |
| Barcode | | ✓ | | | | | | | | | | ✓ | |
| Unit | ✓ | | | | | | | | | | | ✓ | |
| Warehouse | ✓ | | | | | | | | | ✓ | | | |
| Price | | | | | | | | | | | | ✓ | ✓ |

---

## 业务案例验证

### 案例1：可乐（可口可乐330ml罐装）

| 概念 | 是否适用 | 说明 |
|------|----------|------|
| Product | ✓ | 可销售的产品 |
| Food | ✓ | 可直接饮用的饮料 |
| Material | ✓ | 有具体规格、单位、保质期等属性 |
| Beverage | ✓ | 液态、可直接饮用 |
| Ingredient | ✗ | 不作为原料使用 |
| Sellable | ✓ | 可直接销售给顾客 |
| Purchasable | ✓ | 可从供应商采购 |
| Stockable | ✓ | 可在仓库中存储 |
| Consumable | ✗ | 不作为原料消耗 |
| Recipe Component | ✗ | 不是配方组件 |
| Stock Item | ✓ | 有具体库存记录 |
| SKU | ✓ | 有唯一标识编码 |
| Barcode | ✓ | 有条形码 |
| Unit | ✓ | 有计量单位（罐） |
| Warehouse | ✓ | 有存储场所 |
| Price | ✓ | 有销售价格 |

### 案例2：鸡翅（冷冻鸡翅500g）

| 概念 | 是否适用 | 说明 |
|------|----------|------|
| Product | ✓ | 可销售/可采购的产品 |
| Food | ✓ | 可直接食用的食品 |
| Material | ✓ | 有具体规格、单位、保质期等属性 |
| Beverage | ✗ | 不是液态饮料 |
| Ingredient | ✓ | 可作为菜品原料 |
| Sellable | ✓ | 可直接销售给顾客 |
| Purchasable | ✓ | 可从供应商采购 |
| Stockable | ✓ | 可在仓库中存储 |
| Consumable | ✓ | 在制作可乐鸡翅时被消耗 |
| Recipe Component | ✓ | 在可乐鸡翅中作为主料 |
| Stock Item | ✓ | 有具体库存记录 |
| SKU | ✓ | 有唯一标识编码 |
| Barcode | ✓ | 有条形码 |
| Unit | ✓ | 有计量单位（kg） |
| Warehouse | ✓ | 有存储场所 |
| Price | ✓ | 有采购/销售价格 |

### 案例3：大米（东北大米10kg装）

| 概念 | 是否适用 | 说明 |
|------|----------|------|
| Product | ✓ | 可采购的产品 |
| Food | ✗ | 不直接销售，是原材料 |
| Material | ✓ | 有具体规格、单位、保质期等属性 |
| Beverage | ✗ | 不是液态饮料 |
| Ingredient | ✓ | 可作为菜品原料 |
| Sellable | ✗ | 不直接销售给顾客 |
| Purchasable | ✓ | 可从供应商采购 |
| Stockable | ✓ | 可在仓库中存储 |
| Consumable | ✓ | 在制作米饭时被消耗 |
| Recipe Component | ✓ | 在宫保鸡丁中作为配料 |
| Stock Item | ✓ | 有具体库存记录 |
| SKU | ✓ | 有唯一标识编码 |
| Barcode | ✓ | 有条形码 |
| Unit | ✓ | 有计量单位（kg） |
| Warehouse | ✓ | 有存储场所 |
| Price | ✓ | 有采购价格 |

### 案例4：酱油（海天酱油500ml瓶装）

| 概念 | 是否适用 | 说明 |
|------|----------|------|
| Product | ✓ | 可采购的产品 |
| Food | ✗ | 不直接销售，是调味品 |
| Material | ✓ | 有具体规格、单位、保质期等属性 |
| Beverage | ✗ | 不可直接饮用 |
| Ingredient | ✓ | 可作为菜品原料 |
| Sellable | ✗ | 不直接销售给顾客 |
| Purchasable | ✓ | 可从供应商采购 |
| Stockable | ✓ | 可在仓库中存储 |
| Consumable | ✓ | 在制作宫保鸡丁时被消耗 |
| Recipe Component | ✓ | 在宫保鸡丁中作为调味品 |
| Stock Item | ✓ | 有具体库存记录 |
| SKU | ✓ | 有唯一标识编码 |
| Barcode | ✓ | 有条形码 |
| Unit | ✓ | 有计量单位（瓶） |
| Warehouse | ✓ | 有存储场所 |
| Price | ✓ | 有采购价格 |

### 案例5：矿泉水（农夫山泉550ml瓶装）

| 概念 | 是否适用 | 说明 |
|------|----------|------|
| Product | ✓ | 可销售的产品 |
| Food | ✓ | 可直接饮用的饮料 |
| Material | ✓ | 有具体规格、单位、保质期等属性 |
| Beverage | ✓ | 液态、可直接饮用 |
| Ingredient | ✗ | 不作为原料使用 |
| Sellable | ✓ | 可直接销售给顾客 |
| Purchasable | ✓ | 可从供应商采购 |
| Stockable | ✓ | 可在仓库中存储 |
| Consumable | ✗ | 不作为原料消耗 |
| Recipe Component | ✗ | 不是配方组件 |
| Stock Item | ✓ | 有具体库存记录 |
| SKU | ✓ | 有唯一标识编码 |
| Barcode | ✓ | 有条形码 |
| Unit | ✓ | 有计量单位（瓶） |
| Warehouse | ✓ | 有存储场所 |
| Price | ✓ | 有销售价格 |

### 案例6：薯片（乐事薯片104g袋装）

| 概念 | 是否适用 | 说明 |
|------|----------|------|
| Product | ✓ | 可销售的产品 |
| Food | ✓ | 可直接食用的零食 |
| Material | ✓ | 有具体规格、单位、保质期等属性 |
| Beverage | ✗ | 不是液态饮料 |
| Ingredient | ✗ | 不作为原料使用 |
| Sellable | ✓ | 可直接销售给顾客 |
| Purchasable | ✓ | 可从供应商采购 |
| Stockable | ✓ | 可在仓库中存储 |
| Consumable | ✗ | 不作为原料消耗 |
| Recipe Component | ✗ | 不是配方组件 |
| Stock Item | ✓ | 有具体库存记录 |
| SKU | ✓ | 有唯一标识编码 |
| Barcode | ✓ | 有条形码 |
| Unit | ✓ | 有计量单位（袋） |
| Warehouse | ✓ | 有存储场所 |
| Price | ✓ | 有销售价格 |

### 案例7：餐盒（一次性餐盒100个装）

| 概念 | 是否适用 | 说明 |
|------|----------|------|
| Product | ✓ | 可采购的产品 |
| Food | ✗ | 不是食品 |
| Material | ✓ | 有具体规格、单位等属性 |
| Beverage | ✗ | 不是液态饮料 |
| Ingredient | ✗ | 不作为菜品原料 |
| Sellable | ✗ | 不直接销售给顾客 |
| Purchasable | ✓ | 可从供应商采购 |
| Stockable | ✓ | 可在仓库中存储 |
| Consumable | ✓ | 在包装时被消耗 |
| Recipe Component | ✗ | 不是配方组件 |
| Stock Item | ✓ | 有具体库存记录 |
| SKU | ✓ | 有唯一标识编码 |
| Barcode | ✓ | 有条形码 |
| Unit | ✓ | 有计量单位（个） |
| Warehouse | ✓ | 有存储场所 |
| Price | ✓ | 有采购价格 |

### 案例8：宫保鸡丁（自制菜品）

| 概念 | 是否适用 | 说明 |
|------|----------|------|
| Product | ✓ | 可销售的产品 |
| Food | ✓ | 可直接食用的菜品 |
| Material | ✓ | 有具体规格、单位等属性 |
| Beverage | ✗ | 不是液态饮料 |
| Ingredient | ✗ | 不作为原料使用 |
| Sellable | ✓ | 可直接销售给顾客 |
| Purchasable | ✗ | 不从外部采购 |
| Stockable | ✗ | 现做现卖，不库存 |
| Consumable | ✗ | 不作为原料消耗 |
| Recipe Component | ✗ | 不是配方组件 |
| Stock Item | ✗ | 不作为库存项管理 |
| SKU | ✓ | 有唯一标识编码 |
| Barcode | ✗ | 无条形码（自制菜品） |
| Unit | ✓ | 有计量单位（份） |
| Warehouse | ✗ | 无存储场所 |
| Price | ✓ | 有销售价格 |

### 案例9：可乐鸡翅（自制菜品）

| 概念 | 是否适用 | 说明 |
|------|----------|------|
| Product | ✓ | 可销售的产品 |
| Food | ✓ | 可直接食用的菜品 |
| Material | ✓ | 有具体规格、单位等属性 |
| Beverage | ✗ | 不是液态饮料 |
| Ingredient | ✗ | 不作为原料使用 |
| Sellable | ✓ | 可直接销售给顾客 |
| Purchasable | ✗ | 不从外部采购 |
| Stockable | ✗ | 现做现卖，不库存 |
| Consumable | ✗ | 不作为原料消耗 |
| Recipe Component | ✗ | 不是配方组件 |
| Stock Item | ✗ | 不作为库存项管理 |
| SKU | ✓ | 有唯一标识编码 |
| Barcode | ✗ | 无条形码（自制菜品） |
| Unit | ✓ | 有计量单位（份） |
| Warehouse | ✗ | 无存储场所 |
| Price | ✓ | 有销售价格 |

---

## 分类总结

### 按 ENTITY 维度

| 实体 | 说明 |
|------|------|
| Product | 产品基础实体 |
| Food | 食品/菜品实体 |
| Material | 物料基础实体 |
| Stock Item | 库存记录实体 |
| Unit | 计量单位实体 |
| Warehouse | 仓库实体 |

### 按 IDENTITY 维度

| 标识 | 说明 |
|------|------|
| SKU | 内部库存标识 |
| Barcode | 外部条码标识 |

### 按 TYPE 维度

| 类型 | 说明 |
|------|------|
| Food | 食品/菜品类型 |
| Beverage | 饮料类型 |

### 按 CLASSIFICATION 维度

| 分类 | 说明 |
|------|------|
| Product | 产品分类 |
| Beverage | 饮料分类 |

### 按 ROLE 维度

| 角色 | 说明 |
|------|------|
| Ingredient | 原料角色 |
| Sellable | 可销售角色 |
| Purchasable | 可采购角色 |
| Recipe Component | 配方组件角色 |

### 按 CAPABILITY 维度

| 能力 | 说明 |
|------|------|
| Sellable | 可销售能力 |
| Purchasable | 可采购能力 |
| Stockable | 可库存能力 |
| Consumable | 可消耗能力 |

### 按 RELATIONSHIP 维度

| 关系 | 说明 |
|------|------|
| Ingredient | 原料关系 |
| Recipe Component | 配方关系 |

### 按 PROFILE 维度

| 配置 | 说明 |
|------|------|
| Material | 物料属性配置 |
| Stock Item | 库存属性配置 |

### 按 SCOPE 维度

| 范围 | 说明 |
|------|------|
| Stockable | 库存范围 |
| Warehouse | 仓库范围 |

### 按 STATE 维度

| 状态 | 说明 |
|------|------|
| Consumable | 消耗状态 |

### 按 ATTRIBUTE 维度

| 属性 | 说明 |
|------|------|
| SKU | 标识属性 |
| Barcode | 条码属性 |
| Unit | 单位属性 |
| Price | 价格属性 |

### 按 DERIVED 维度

| 派生值 | 说明 |
|--------|------|
| Price | 从成本/利润率计算 |

---

## 关键发现

1. **Product vs Material**: 
   - Product是业务层面的产品概念
   - Material是物理层面的物料概念
   - 两者不是等同关系，而是不同层面的抽象

2. **Food vs Material**:
   - Food是Product的一个类型（可直接销售的食品）
   - Material是底层的属性描述
   - 同一物品可以同时是Food和Material

3. **Role vs Capability**:
   - Role定义"是什么"（如Ingredient）
   - Capability定义"能做什么"（如Sellable）
   - 同一物品可以有多个Role和Capability

4. **Identity vs Attribute**:
   - Identity用于唯一标识（如SKU、Barcode）
   - Attribute用于描述属性（如Unit、Price）
   - Identity和Attribute是不同的维度

---

## 下一步行动

1. **Phase 2**: 基于分类结果，建立概念间的关系模型
2. **Phase 3**: 设计目标技术实现模型
3. **Phase 4**: 与当前实现进行差异分析

---

## 文档信息

- **创建日期**: 2026-09-09
- **版本**: 1.0
- **状态**: Phase 1 完成
- **作者**: AI Assistant
