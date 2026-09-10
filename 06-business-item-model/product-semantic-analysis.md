# Phase 10 - Product 语义分析

## 1. 引言

本文档对餐饮ERP系统中的 `Product` 概念进行深度语义分析，拒绝接受 "Product = 已废弃" 这一简单结论，从业务语义角度重新研究其本质。

## 2. 当前现实 (CURRENT REALITY)

### 2.1 数据库现状

```
product 表（已标记为 LEGACY）:
- product_id (主键)
- name (名称)
- code (编码)
- category_id (分类)
- unit (单位)
- price (售价)
- cost_price (成本价)
- barcode (条形码)
- specification (规格)
- origin (产地)
- supplier_id (供应商ID)
- supplier_name (供应商名称)
```

### 2.2 与其他表的关系

- `order_items` 表引用 `food_id`，不直接引用 `product_id`
- `purchase_order_items` 表同时引用 `product_id` 和 `material_id`
- `inventory` 表引用 `material_id`，不直接引用 `product_id`
- `dish_recipe` 表引用 `ingredient_id`（可能是 material_id）

### 2.3 现有使用情况

Product 表被标记为 LEGACY，但：
- 采购订单明细表仍在引用 `product_id`
- 供应商信息同时存在于 `product` 和 `material_archives` 中
- 产品分类 `category_id` 在多个地方使用

## 3. 业务语义分析 (TARGET BUSINESS SEMANTICS)

### 3.1 候选语义模型

#### 模型 A: Product = Entity (实体)

**定义**：Product 是系统中所有可识别物品的统称，是基础实体层。

**支持证据**：
- Product 包含基础属性（名称、编码、分类）
- 多个业务领域都需要"物品"这个基础概念
- 采购、库存、销售都需要一个统一的物品标识

**反对证据**：
- 当前 product 表缺少库存相关字段（current_stock, warehouse_id）
- 采购订单需要同时引用 product_id 和 material_id，说明两者有区别
- 食品有特殊的保质期、营养信息等属性，Product 表没有

#### 模型 B: Product = Commercial Definition (商业定义)

**定义**：Product 是商业上可交易的物品定义，关注商业属性（价格、供应商、条码）。

**支持证据**：
- Product 表包含 price, cost_price, barcode, supplier 等商业属性
- 采购订单需要 product 的商业信息
- 销售需要知道产品的售价和成本

**反对证据**：
- 商业定义应该能直接用于销售，但 order_items 引用的是 food_id
- 库存管理需要更详细的仓储信息，Product 表没有
- 不同业务场景（堂食、外卖、采购）需要不同的商业视图

#### 模型 C: Product = Sellable Role (可销售角色)

**定义**：Product 是一个实体在销售场景中扮演的角色。

**支持证据**：
- 同一个实体在不同场景可能扮演不同角色（作为菜品销售、作为原料采购）
- 销售角色需要价格、条码等商业属性
- 符合"一个实体，多种角色"的设计模式

**反对证据**：
- 当前系统没有明确的"实体-角色"分离机制
- Product 表直接存储了商业属性，而不是引用实体
- 需要更复杂的角色管理机制

#### 模型 D: Product = Offering (产品报价)

**定义**：Product 是面向客户的可购买产品报价，是销售目录中的一个条目。

**支持证据**：
- Product 包含完整的销售信息（名称、价格、规格、条码）
- 可以直接生成销售目录
- 符合传统 ERP 中"产品"的概念

**反对证据**：
- 当前系统中，销售目录实际上是 foods 表（菜品）
- Product 表被标记为 LEGACY，说明业务已经不再使用
- 采购场景需要 Product，但销售场景不需要

#### 模型 E: Product = Deprecated (已废弃)

**定义**：Product 是历史遗留概念，已被 foods 和 material_archives 替代。

**支持证据**：
- product 表被标记为 LEGACY
- 销售使用 foods，库存使用 material_archives
- 业务流程已经绕过了 product 表

**反对证据**：
- 采购订单明细表仍在引用 product_id
- 供应商信息需要统一管理
- 缺少一个统一的"可采购物品"概念

### 3.2 语义模型对比

| 模型 | 适用场景 | 业务价值 | 实现复杂度 | 未来扩展性 |
|------|----------|----------|------------|------------|
| Entity | 基础数据管理 | 低 | 中 | 高 |
| Commercial Definition | 采购、成本核算 | 中 | 低 | 中 |
| Sellable Role | 多场景物品管理 | 高 | 高 | 高 |
| Offering | 销售目录管理 | 中 | 低 | 低 |
| Deprecated | 无 | 无 | 无 | 无 |

## 4. 真实案例验证

### 案例 1: 可乐鸡翅的原料采购

**场景**：采购可乐、鸡翅、酱油、糖等原料

**当前流程**：
1. 采购员创建采购订单
2. 选择供应商（来自 product 或 material_archives）
3. 选择采购物品（product_id 或 material_id）
4. 记录采购价格和数量

**问题**：
- 采购的"可乐"和库存的"可乐"是同一个东西吗？
- 如果是，为什么需要两个 ID（product_id 和 material_id）？
- 如果不是，区别在哪里？

**验证**：
- 采购的"可乐"关注：供应商、采购价、最小起订量、到货时间
- 库存的"可乐"关注：库存数量、保质期、存储位置、批次管理
- 同一个物理实体，在采购和库存场景中需要不同的属性

### 案例 2: 菜品销售

**场景**：顾客点餐"可乐鸡翅"

**当前流程**：
1. 顾客在 POS 选择菜品（food_id）
2. 生成订单明细（order_items）
3. 扣减库存（通过 dish_recipe 扣减原料）

**问题**：
- 销售的是"可乐鸡翅"这道菜，不是原料
- 需要知道菜品的售价、份量、利润率
- 原料成本需要从 recipe 计算

**验证**：
- 销售场景需要"菜品"（food）概念，不是"产品"（product）
- 菜品是成品，原料是配料，两者在销售场景中不直接等同
- Product 概念在销售场景中不适用

### 案例 3: 集团化多门店管理

**场景**：集团有 10 家门店，需要统一管理菜品和原料

**当前流程**：
1. 每家门店独立管理自己的 foods 和 material_archives
2. 集团层面需要统一的菜品和原料目录
3. 不同门店可能有不同的供应商和采购价

**问题**：
- 集团层面需要一个"标准菜品"和"标准原料"的概念
- 门店层面需要"门店菜品"和"门店原料"的概念
- Product 能否承担"标准物品"的角色？

**验证**：
- 集团化需要分层：集团标准 → 门店定制
- Product 作为集团标准层，foods 和 material_archives 作为门店实现层
- 这符合"标准 + 定制"的集团化管理模式

## 5. 目标技术模型 (TARGET TECHNICAL MODEL)

### 5.1 推荐模型：Product = Commercial Definition (商业定义)

**理由**：
1. **业务价值最高**：Product 作为商业定义，可以统一管理采购、成本、供应商
2. **实现复杂度适中**：不需要复杂的角色管理机制
3. **未来扩展性好**：可以支持集团化标准化管理
4. **符合当前现实**：采购订单已经在使用 product_id

### 5.2 技术模型设计

```
Product (商业定义)
├── 基础信息
│   ├── product_id (唯一标识)
│   ├── name (标准名称)
│   ├── code (标准编码)
│   ├── category_id (分类)
│   └── specification (标准规格)
├── 商业信息
│   ├── reference_price (参考价格)
│   ├── cost_price (成本价)
│   ├── barcode (条形码)
│   └── unit (计量单位)
├── 供应信息
│   ├── supplier_id (默认供应商)
│   ├── supplier_name (供应商名称)
│   ├── lead_time (交货周期)
│   └── min_order_qty (最小起订量)
└── 状态信息
    ├── status (启用/停用)
    ├── effective_date (生效日期)
    └── expiry_date (失效日期)
```

### 5.3 与其他概念的关系

```
Product (商业定义) ← 1:N → Material Archive (库存档案)
Product (商业定义) ← 1:N → Food (菜品)
Product (商业定义) ← 1:N → Inventory (库存记录)
```

**关系说明**：
- 一个 Product 可以对应多个 Material Archive（不同仓库、不同批次）
- 一个 Product 可以被多个 Food 使用（作为原料）
- 一个 Product 可以有多个 Inventory 记录（不同仓库、不同批次）

## 6. 结论

### 6.1 为什么 Product = Commercial Definition 是正确的

1. **业务语义准确**：Product 描述的是"可采购的商业物品"，关注商业属性
2. **解决实际问题**：统一了采购、成本、供应商管理
3. **支持集团化**：可以作为集团标准物品目录
4. **符合当前现实**：采购订单已经在使用 product_id

### 6.2 为什么不选择其他模型

1. **不是 Entity**：Entity 太底层，缺少商业属性
2. **不是 Sellable Role**：实现复杂度太高，当前业务不需要
3. **不是 Offering**：销售场景使用 foods，不需要 Product
4. **不是 Deprecated**：采购场景仍在使用，有实际业务价值

### 6.3 未来演进方向

1. **短期**：完善 Product 表，补充缺失字段
2. **中期**：建立 Product → Material Archive 的标准映射
3. **长期**：支持集团化标准物品管理，实现跨门店共享

### 6.4 对现有系统的影响

1. **采购模块**：继续使用 product_id，但需要完善 Product 数据
2. **库存模块**：保持使用 material_id，建立与 Product 的映射
3. **销售模块**：保持使用 food_id，但可以从 Product 获取成本信息
4. **财务模块**：可以从 Product 获取标准成本，用于成本核算

---

**分析完成时间**：2026年9月9日  
**分析结论**：Product = Commercial Definition (商业定义)  
**推荐行动**：完善 Product 表结构，建立与 Material Archive 的映射关系