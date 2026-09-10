# Canonical Identity Analysis

## 1. Analysis Framework

### 1.1 Core Concepts

**Business Object (BO)**
An entity that participates in business processes. Examples: 可乐, 鸡翅, 宫保鸡丁.

**Business Role (BR)**
A function that a BO performs in a specific business context. Examples: 可销售的商品, 可采购的对象, 可存储的库存项.

**Business Identity (BI)**
The intrinsic identity of a BO that persists across different business contexts. This is the "what it is" question.

**Canonical Identity (CI)**
A system-level unique identifier for a BO that maintains consistency across all business contexts. This is the "how we track it" question.

### 1.2 Identity vs Role Distinction

**Identity (What it is)**
- Intrinsic properties that define the object
- Persistent across business contexts
- Examples: 物理实体, 化学组成, 基础属性

**Role (What it does)**
- Context-dependent functions
- Changes based on business scenario
- Examples: 在销售场景中作为商品, 在采购场景中作为物料, 在库存管理中作为库存项

### 1.3 Analysis Dimensions

| Dimension | Question | Example |
|-----------|----------|---------|
| Physical | 这个东西是什么？ | 碳酸饮料液体，装在铝罐里 |
| Functional | 这个东西能做什么？ | 可销售、可存储、可作为原料 |
| Contextual | 这个东西在什么场景中出现？ | 门店销售、仓库存储、厨房配料 |
| Systemic | 系统如何表示这个东西？ | food表、product表、material表 |

## 2. Business Object Tracking

### 2.1 Object: 可乐 (Cola)

**Physical Identity**
- Type: 碳酸饮料
- Composition: 水、糖、二氧化碳、咖啡因、磷酸
- Packaging: 铝罐/塑料瓶
- Properties: 液体，可饮用

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售商品 | 门店销售 | foods表 (as beverage) |
| 可采购物料 | 供应商采购 | product表 (as product) |
| 可存储库存 | 仓库管理 | inventory表 (as material_id) |
| 可作为原料 | 鸡翅等菜品配料 | dish_recipe表 (as ingredient_id) |

**System Realization**
- foods表: food_code='COLA001', food_name='可乐', food_price=3.00
- product表: product_id=1, name='可口可乐', code='COLA001', price=2.50
- material_archives表: material_id=1, material_code='MAT_COLA001', reference_price=2.50
- inventory表: inventory_id=1, material_id=1, current_stock=100

**Key Insight**: 同一物理实体"可乐"在系统中存在四个独立的表示，可能没有建立统一标识。

### 2.2 Object: 鸡翅 (Chicken Wing)

**Physical Identity**
- Type: 禽肉部位
- Composition: 鸡翅中段
- Properties: 固体，可烹饪

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售菜品 | 成品销售 | foods表 (as menu item) |
| 可采购原料 | 采购管理 | product表 (as raw material) |
| 可存储库存 | 冷藏管理 | inventory表 (as material_id) |
| 可作为原料 | 鸡翅菜品配料 | dish_recipe表 (as ingredient) |

**System Realization**
- foods表: food_code='WING001', food_name='鸡翅', food_price=8.00
- product表: product_id=2, name='鸡翅中', code='WING001', price=6.00
- material_archives表: material_id=2, material_code='MAT_WING001', reference_price=6.00
- inventory表: inventory_id=2, material_id=2, current_stock=50
- dish_recipe表: dish_id=3, ingredient_id=2, quantity=0.2

**Key Insight**: 鸡翅既是成品销售，也是其他菜品（如可乐鸡翅）的原料。

### 2.3 Object: 大米 (Rice)

**Physical Identity**
- Type: 谷物
- Composition: 稻米
- Properties: 固体，可烹饪

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售商品 | 袋装米销售 | foods表 (as packaged rice) |
| 可采购原料 | 批量采购 | product表 (as bulk material) |
| 可存储库存 | 仓库管理 | inventory表 (as material_id) |
| 可作为原料 | 米饭等菜品配料 | dish_recipe表 (as ingredient) |

**System Realization**
- foods表: food_code='RICE001', food_name='大米', food_price=5.00/kg
- product表: product_id=3, name='东北大米', code='RICE001', price=4.00/kg
- material_archives表: material_id=3, material_code='MAT_RICE001', reference_price=4.00/kg
- inventory表: inventory_id=3, material_id=3, current_stock=200

**Key Insight**: 大米作为主食原料，库存管理非常重要。

### 2.4 Object: 酱油 (Soy Sauce)

**Physical Identity**
- Type: 调味品
- Composition: 大豆、小麦、盐水
- Properties: 液体，可调味

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售商品 | 瓶装酱油销售 | foods表 (as bottled sauce) |
| 可采购原料 | 供应商采购 | product表 (as condiment) |
| 可存储库存 | 仓库管理 | inventory表 (as material_id) |
| 可作为原料 | 菜品调味配料 | dish_recipe表 (as seasoning) |

**System Realization**
- foods表: food_code='SOY001', food_name='酱油', food_price=8.00/瓶
- product表: product_id=4, name='生抽酱油', code='SOY001', price=6.50/瓶
- material_archives表: material_id=4, material_code='MAT_SOY001', reference_price=6.50/瓶
- inventory表: inventory_id=4, material_id=4, current_stock=30

**Key Insight**: 酱油作为调味品，用量小但消耗快，需要精确的库存管理。

### 2.5 Object: 矿泉水 (Mineral Water)

**Physical Identity**
- Type: 饮用水
- Composition: 天然矿泉水
- Properties: 液体，可饮用

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售商品 | 瓶装水销售 | foods表 (as bottled water) |
| 可采购原料 | 批量采购 | product表 (as beverage) |
| 可存储库存 | 仓库管理 | inventory表 (as material_id) |
| 可作为原料 | 几乎不作为原料 | dish_recipe表 (rarely) |

**System Realization**
- foods表: food_code='WATER001', food_name='矿泉水', food_price=2.00
- product表: product_id=5, name='农夫山泉', code='WATER001', price=1.50
- material_archives表: material_id=5, material_code='MAT_WATER001', reference_price=1.50
- inventory表: inventory_id=5, material_id=5, current_stock=200

**Key Insight**: 矿泉水主要作为独立商品销售，很少作为菜品原料。

### 2.6 Object: 薯片 (Potato Chips)

**Physical Identity**
- Type: 休闲零食
- Composition: 土豆、油、盐
- Properties: 固体，可食用

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售商品 | 零食销售 | foods表 (as snack) |
| 可采购原料 | 供应商采购 | product表 (as snack product) |
| 可存储库存 | 仓库管理 | inventory表 (as material_id) |
| 可作为原料 | 几乎不作为原料 | dish_recipe表 (rarely) |

**System Realization**
- foods表: food_code='CHIPS001', food_name='薯片', food_price=6.00
- product表: product_id=6, name='乐事薯片', code='CHIPS001', price=5.00
- material_archives表: material_id=6, material_code='MAT_CHIPS001', reference_price=5.00
- inventory表: inventory_id=6, material_id=6, current_stock=80

**Key Insight**: 薯片作为标准包装商品，库存管理相对简单。

### 2.7 Object: 餐盒 (Meal Box)

**Physical Identity**
- Type: 包装容器
- Composition: 塑料/纸质材料
- Properties: 固体，可盛装食物

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售商品 | 外卖销售（很少直接销售） | foods表 (rarely) |
| 可采购原料 | 供应商采购 | product表 (as packaging) |
| 可存储库存 | 仓库管理 | inventory表 (as material_id) |
| 可作为原料 | 菜品包装 | dish_recipe表 (as packaging material) |

**System Realization**
- foods表: 通常不作为菜品销售
- product表: product_id=7, name='外卖餐盒', code='BOX001', price=0.50/个
- material_archives表: material_id=7, material_code='MAT_BOX001', reference_price=0.50/个
- inventory表: inventory_id=7, material_id=7, current_stock=500

**Key Insight**: 餐盒作为包装材料，是成本的一部分，但很少直接销售。

### 2.8 Object: 宫保鸡丁 (Kung Pao Chicken)

**Physical Identity**
- Type: 成品菜品
- Composition: 鸡肉、花生、辣椒、花椒等
- Properties: 可直接食用的成品

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售菜品 | 菜单销售 | foods表 (as menu item) |
| 可采购原料 | 几乎不作为采购对象 | product表 (rarely) |
| 可存储库存 | 通常不存储成品 | inventory表 (rarely) |
| 可作为原料 | 几乎不作为其他菜品原料 | dish_recipe表 (rarely) |

**System Realization**
- foods表: food_code='KPC001', food_name='宫保鸡丁', food_price=28.00
- product表: 通常不作为采购对象
- material_archives表: 通常不作为物料
- inventory表: 通常不存储成品
- dish_recipe表: dish_id=1, ingredient_id=2, quantity=0.3 (作为被配料的对象)

**Key Insight**: 宫保鸡丁作为成品菜品，主要是销售对象，几乎不参与其他业务角色。

### 2.9 Object: 可乐鸡翅 (Cola Chicken Wings)

**Physical Identity**
- Type: 成品菜品
- Composition: 鸡翅、可乐、酱油等
- Properties: 可直接食用的成品

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售菜品 | 菜单销售 | foods表 (as menu item) |
| 可采购原料 | 几乎不作为采购对象 | product表 (rarely) |
| 可存储库存 | 通常不存储成品 | inventory表 (rarely) |
| 可作为原料 | 几乎不作为其他菜品原料 | dish_recipe表 (rarely) |

**System Realization**
- foods表: food_code='CCW001', food_name='可乐鸡翅', food_price=32.00
- dish_recipe表: dish_id=2, ingredient_id=2, quantity=0.3 (鸡翅), ingredient_id=1, quantity=0.2 (可乐)

**Key Insight**: 可乐鸡翅作为复合菜品，其配方定义了它与原材料的关系。

### 2.10 Object: 咖啡 (Coffee)

**Physical Identity**
- Type: 饮品
- Composition: 咖啡豆、水、牛奶等
- Properties: 液体，可饮用

**Business Roles**
| Role | Business Context | System Representation |
|------|------------------|------------------------|
| 可销售商品 | 现磨咖啡销售 | foods表 (as brewed coffee) |
| 可采购原料 | 咖啡豆采购 | product表 (as coffee beans) |
| 可存储库存 | 咖啡豆库存 | inventory表 (as material_id) |
| 可作为原料 | 咖啡饮品配料 | dish_recipe表 (as ingredient) |

**System Realization**
- foods表: food_code='COFFEE001', food_name='美式咖啡', food_price=15.00
- product表: product_id=8, name='咖啡豆', code='COFFEE001', price=100.00/kg
- material_archives表: material_id=8, material_code='MAT_COFFEE001', reference_price=100.00/kg
- inventory表: inventory_id=8, material_id=8, current_stock=10
- dish_recipe表: dish_id=4, ingredient_id=8, quantity=0.01 (咖啡豆用量)

**Key Insight**: 咖啡的复杂性在于成品（美式咖啡）和原料（咖啡豆）的区别。

## 3. Identity vs Role Distinction

### 3.1 Identity Properties

**Physical Identity**
- 物理形态: 固体、液体、粉末
- 化学组成: 成分、配方
- 包装规格: 容器、包装
- 保质期: 有效期

**Business Identity**
- 品牌: 可口可乐、农夫山泉
- 供应商: 供应商信息
- 采购价格: 成本价
- 销售价格: 售价

**System Identity**
- 唯一标识: UUID、自增ID
- 编码规则: 商品编码、物料编码
- 分类体系: 品类、类别

### 3.2 Role Properties

**销售角色**
- 销售价格
- 销售单位
- 促销策略
- 目标客户

**采购角色**
- 采购价格
- 采购批量
- 供应商
- 采购周期

**库存角色**
- 库存单位
- 安全库存
- 最高库存
- 库存位置

**生产角色**
- 用量标准
- 损耗率
- 生产工序
- 质量标准

### 3.3 Identity-Role Mapping

| Identity | Sales Role | Procurement Role | Inventory Role | Production Role |
|----------|------------|------------------|----------------|-----------------|
| 可乐 | 3.00元/罐 | 2.50元/罐 | 罐为单位 | 可作为原料 |
| 鸡翅 | 8.00元/份 | 6.00元/kg | kg为单位 | 0.2kg/份 |
| 大米 | 5.00元/kg | 4.00元/kg | kg为单位 | 0.1kg/份 |
| 酱油 | 8.00元/瓶 | 6.50元/瓶 | 瓶为单位 | 10ml/份 |
| 矿泉水 | 2.00元/瓶 | 1.50元/瓶 | 瓶为单位 | 很少作为原料 |
| 薯片 | 6.00元/袋 | 5.00元/袋 | 袋为单位 | 很少作为原料 |
| 餐盒 | 0.50元/个 | 0.50元/个 | 个为单位 | 1个/份 |
| 宫保鸡丁 | 28.00元/份 | 不采购 | 不存储 | 菜品本身 |
| 可乐鸡翅 | 32.00元/份 | 不采购 | 不存储 | 菜品本身 |
| 咖啡 | 15.00元/杯 | 100.00元/kg | kg为单位 | 10g/杯 |

## 4. Current System Reality Analysis

### 4.1 System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Business Layer                        │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │
│  │ Sales   │  │ Purchase│  │Inventory│  │Production│   │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘   │
└─────────────────────────────────────────────────────────┘
                           │
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                            │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │
│  │  foods  │  │ product │  │material │  │inventory│   │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 4.2 Current Table Structures

**foods table (菜品/食品)**
```sql
CREATE TABLE foods (
    food_id INT PRIMARY KEY,
    food_code VARCHAR(50) UNIQUE,
    food_name VARCHAR(100),
    food_price DECIMAL(10,2),
    cost_price DECIMAL(10,2),
    food_category VARCHAR(50),
    stock INT DEFAULT 0
);
```

**product table (产品/物料基础信息)**
```sql
CREATE TABLE product (
    product_id INT PRIMARY KEY,
    name VARCHAR(100),
    code VARCHAR(50) UNIQUE,
    category_id INT,
    unit VARCHAR(20),
    price DECIMAL(10,2),
    cost_price DECIMAL(10,2),
    barcode VARCHAR(100),
    specification VARCHAR(200),
    supplier_id INT
);
```

**material_archives table (商品档案)**
```sql
CREATE TABLE material_archives (
    material_id INT PRIMARY KEY,
    material_code VARCHAR(50) UNIQUE,
    material_name VARCHAR(100),
    category_id INT,
    unit VARCHAR(20),
    spec VARCHAR(200),
    reference_price DECIMAL(10,2),
    supplier_id INT
);
```

**inventory table (库存)**
```sql
CREATE TABLE inventory (
    inventory_id INT PRIMARY KEY,
    material_id INT,
    warehouse_id INT,
    current_stock DECIMAL(10,2),
    unit_cost DECIMAL(10,2),
    total_cost DECIMAL(10,2),
    batch_no VARCHAR(50),
    FOREIGN KEY (material_id) REFERENCES material_archives(material_id)
);
```

**store_inventory table (门店库存)**
```sql
CREATE TABLE store_inventory (
    store_id INT,
    material_id INT,
    current_stock DECIMAL(10,2),
    unit VARCHAR(20),
    PRIMARY KEY (store_id, material_id)
);
```

**dish_recipe table (菜品配方/BOM)**
```sql
CREATE TABLE dish_recipe (
    dish_id INT,
    ingredient_id INT,
    quantity DECIMAL(10,2),
    unit VARCHAR(20),
    estimated_cost DECIMAL(10,2),
    actual_cost DECIMAL(10,2),
    PRIMARY KEY (dish_id, ingredient_id)
);
```

**dish_inventory table (菜品库存关联)**
```sql
CREATE TABLE dish_inventory (
    dish_id INT,
    inventory_id INT,
    quantity DECIMAL(10,2),
    unit VARCHAR(20),
    PRIMARY KEY (dish_id, inventory_id)
);
```

**combo_inventory table (套餐库存关联)**
```sql
CREATE TABLE combo_inventory (
    combo_id INT,
    inventory_id INT,
    quantity DECIMAL(10,2),
    unit VARCHAR(20),
    PRIMARY KEY (combo_id, inventory_id)
);
```

### 4.3 Data Flow Analysis

```
采购流程:
product → material_archives → inventory → store_inventory
  │           │                  │              │
  └───────────┴──────────────────┴──────────────┘
            (同一个"物"在不同表中的表示)

销售流程:
foods → dish_recipe → inventory → material_archives
  │        │              │              │
  └────────┴──────────────┴──────────────┘
        (菜品通过配方关联到库存)

库存管理:
inventory → material_archives
    │              │
    └──────────────┘
      (库存基于物料档案)
```

### 4.4 Key Issues Identified

**Issue 1: Identity Fragmentation**
同一个"可乐"在系统中存在四个独立的表示：
- foods.food_code='COLA001'
- product.code='COLA001'
- material_archives.material_code='MAT_COLA001'
- inventory.material_id=1

**Issue 2: Cross-Reference Gaps**
- foods表与product表之间没有外键关联
- product表与material_archives表之间没有外键关联
- dish_recipe表中的ingredient_id可能指向product也可能指向material_archives

**Issue 3: Role Confusion**
- foods表既表示"可销售的菜品"又表示"可存储的商品"
- product表既表示"可采购的产品"又表示"可作为原料的物料"
- material_archives表既表示"可存储的库存项"又表示"可作为原料的物料"

**Issue 4: Inventory Management Inconsistency**
- inventory表管理的是material_id，不是food_id或product_id
- store_inventory表管理的是material_id，不是food_id
- 这导致销售时需要从foods转换到material_archives

### 4.5 Example: "可乐" in Current System

**采购流程**
1. 采购员在product表中找到"可乐"（product_id=1, name='可口可乐', code='COLA001'）
2. 采购单记录product_id=1
3. 入库时，系统需要将product_id=1转换为material_id=1
4. inventory表更新material_id=1的库存

**销售流程**
1. 收银员在foods表中找到"可乐"（food_id=1, food_code='COLA001', food_price=3.00）
2. 销售单记录food_id=1
3. 出库时，系统需要将food_id=1转换为material_id=1
4. inventory表更新material_id=1的库存

**问题**
- 采购和销售使用不同的标识符（product_id vs food_id）
- 库存管理使用第三种标识符（material_id）
- 转换逻辑复杂，容易出错

## 5. Target Model Candidates

### 5.1 Candidate Model 1: Unified Canonical Identity

**核心思想**: 每个物理实体只有一个Canonical Identity，不同业务角色通过该Identity关联。

**数据模型**
```sql
-- Canonical Item (唯一标识)
CREATE TABLE canonical_item (
    item_id INT PRIMARY KEY,
    item_code VARCHAR(50) UNIQUE NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_type ENUM('food', 'material', 'packaging', 'ingredient') NOT NULL,
    brand VARCHAR(100),
    specification VARCHAR(200),
    unit VARCHAR(20) NOT NULL,
    barcode VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Sales Role (销售角色)
CREATE TABLE sales_role (
    item_id INT PRIMARY KEY,
    sales_price DECIMAL(10,2) NOT NULL,
    sales_unit VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (item_id) REFERENCES canonical_item(item_id)
);

-- Procurement Role (采购角色)
CREATE TABLE procurement_role (
    item_id INT PRIMARY KEY,
    procurement_price DECIMAL(10,2) NOT NULL,
    supplier_id INT,
    lead_time_days INT,
    minimum_order_qty DECIMAL(10,2),
    FOREIGN KEY (item_id) REFERENCES canonical_item(item_id)
);

-- Inventory Role (库存角色)
CREATE TABLE inventory_role (
    item_id INT PRIMARY KEY,
    warehouse_id INT,
    current_stock DECIMAL(10,2) DEFAULT 0,
    unit_cost DECIMAL(10,2),
    safety_stock DECIMAL(10,2),
    maximum_stock DECIMAL(10,2),
    FOREIGN KEY (item_id) REFERENCES canonical_item(item_id)
);

-- Production Role (生产角色)
CREATE TABLE production_role (
    item_id INT PRIMARY KEY,
    is_ingredient BOOLEAN DEFAULT FALSE,
    is_finished_product BOOLEAN DEFAULT FALSE,
    standard_cost DECIMAL(10,2),
    FOREIGN KEY (item_id) REFERENCES canonical_item(item_id)
);
```

**优点**
- 消除身份碎片化
- 每个物理实体只有一个权威表示
- 角色可以灵活添加和移除

**缺点**
- 需要重构现有系统
- 迁移复杂
- 可能过度设计

### 5.2 Candidate Model 2: Bridge Pattern

**核心思想**: 保持现有表结构，通过桥接表建立跨表关联。

**数据模型**
```sql
-- Item Bridge (桥接表)
CREATE TABLE item_bridge (
    bridge_id INT PRIMARY KEY,
    food_id INT,
    product_id INT,
    material_id INT,
    canonical_code VARCHAR(50) UNIQUE NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (food_id) REFERENCES foods(food_id),
    FOREIGN KEY (product_id) REFERENCES product(product_id),
    FOREIGN KEY (material_id) REFERENCES material_archives(material_id)
);

-- 添加索引
CREATE INDEX idx_bridge_food ON item_bridge(food_id);
CREATE INDEX idx_bridge_product ON item_bridge(product_id);
CREATE INDEX idx_bridge_material ON item_bridge(material_id);
```

**优点**
- 最小化现有系统改动
- 渐进式迁移
- 保持向后兼容

**缺点**
- 仍然存在多个表示
- 查询复杂
- 数据一致性难以保证

### 5.3 Candidate Model 3: Canonical Identity + Role Tables

**核心思想**: 引入Canonical Identity表，但保留现有的Role表结构。

**数据模型**
```sql
-- Canonical Item (唯一标识)
CREATE TABLE canonical_item (
    item_id INT PRIMARY KEY,
    item_code VARCHAR(50) UNIQUE NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_type ENUM('food', 'material', 'packaging', 'ingredient') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Food Role (食品角色) - 保留foods表结构
CREATE TABLE food_role (
    item_id INT PRIMARY KEY,
    food_code VARCHAR(50),
    food_price DECIMAL(10,2),
    cost_price DECIMAL(10,2),
    food_category VARCHAR(50),
    stock INT DEFAULT 0,
    FOREIGN KEY (item_id) REFERENCES canonical_item(item_id)
);

-- Product Role (产品角色) - 保留product表结构
CREATE TABLE product_role (
    item_id INT PRIMARY KEY,
    product_code VARCHAR(50),
    category_id INT,
    unit VARCHAR(20),
    price DECIMAL(10,2),
    cost_price DECIMAL(10,2),
    barcode VARCHAR(100),
    specification VARCHAR(200),
    supplier_id INT,
    FOREIGN KEY (item_id) REFERENCES canonical_item(item_id)
);

-- Material Role (物料角色) - 保留material_archives表结构
CREATE TABLE material_role (
    item_id INT PRIMARY KEY,
    material_code VARCHAR(50),
    category_id INT,
    unit VARCHAR(20),
    spec VARCHAR(200),
    reference_price DECIMAL(10,2),
    supplier_id INT,
    FOREIGN KEY (item_id) REFERENCES canonical_item(item_id)
);

-- Inventory Role (库存角色) - 保留inventory表结构
CREATE TABLE inventory_role (
    inventory_id INT PRIMARY KEY,
    item_id INT,
    warehouse_id INT,
    current_stock DECIMAL(10,2),
    unit_cost DECIMAL(10,2),
    total_cost DECIMAL(10,2),
    batch_no VARCHAR(50),
    FOREIGN KEY (item_id) REFERENCES canonical_item(item_id)
);
```

**优点**
- 清晰的Identity vs Role分离
- 保留现有业务逻辑
- 支持渐进式迁移

**缺点**
- 需要修改现有表结构
- 查询可能变慢
- 需要维护多个Role表

### 5.4 Candidate Model Evaluation

| Criteria | Model 1 | Model 2 | Model 3 |
|----------|---------|---------|---------|
| 身份一致性 | 高 | 中 | 高 |
| 实现复杂度 | 高 | 低 | 中 |
| 迁移成本 | 高 | 低 | 中 |
| 查询性能 | 高 | 中 | 中 |
| 扩展性 | 高 | 低 | 高 |
| 维护成本 | 中 | 高 | 中 |

**推荐**: Model 3 (Canonical Identity + Role Tables)
- 平衡了身份一致性和实现复杂度
- 支持渐进式迁移
- 保留现有业务逻辑

## 6. Conclusions and Recommendations

### 6.1 Key Findings

1. **Identity Fragmentation**: 当前系统中同一个物理实体存在多个独立表示，导致数据不一致和查询复杂。

2. **Role Confusion**: 现有表结构混淆了"Identity"和"Role"，导致概念不清。

3. **Cross-Reference Gaps**: 跨表关联缺失，需要复杂的转换逻辑。

4. **Inventory Management Inconsistency**: 库存管理基于material_id，而销售和采购使用不同的标识符。

### 6.2 Recommendations

**短期 (0-3个月)**
1. 建立item_bridge表，统一关联foods、product、material_archives
2. 修复现有数据中的不一致
3. 更新业务逻辑，使用canonical_code作为统一标识

**中期 (3-6个月)**
1. 引入canonical_item表，建立统一身份
2. 将现有表重构为Role表（food_role、product_role、material_role）
3. 更新所有业务流程，使用canonical_item_id

**长期 (6-12个月)**
1. 完全迁移到Canonical Identity模型
2. 优化查询性能
3. 建立数据治理流程，确保身份一致性

### 6.3 Implementation Roadmap

```
Phase 1: Bridge Pattern (0-3 months)
├── Create item_bridge table
├── Populate bridge data
├── Update business logic
└── Test and validate

Phase 2: Canonical Identity (3-6 months)
├── Create canonical_item table
├── Migrate existing data
├── Create role tables
└── Update all references

Phase 3: Full Migration (6-12 months)
├── Remove legacy tables
├── Optimize performance
├── Establish data governance
└── Document and train
```

### 6.4 Success Metrics

1. **Data Consistency**: 每个物理实体只有一个canonical_item记录
2. **Query Performance**: 查询时间减少50%
3. **Development Velocity**: 新功能开发时间减少30%
4. **Bug Reduction**: 数据相关bug减少70%

### 6.5 Risk Mitigation

1. **Data Migration Risk**: 使用增量迁移，保持数据完整性
2. **Performance Risk**: 建立索引，优化查询
3. **Business Logic Risk**: 充分测试，保持向后兼容
4. **Training Risk**: 提供文档和培训

---

**Document Version**: 1.0
**Last Updated**: 2026-09-09
**Author**: Business Analysis Team
**Status**: Draft