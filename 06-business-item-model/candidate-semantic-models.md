# 候选语义模型比较

## 1. 分析框架

### 1.1 决策标准权重

| 标准 | 权重 | 说明 |
|------|------|------|
| **Business Correctness** | 40% | 业务表达的准确性和完整性 |
| **Long-term Model Integrity** | 30% | 长期模型的完整性和可维护性 |
| **Cross-Domain Consistency** | 20% | 跨域数据一致性保证 |
| **Migration Cost** | 10% | 迁移成本和风险（权重最低） |

### 1.2 评估维度

| 维度 | 说明 |
|------|------|
| 业务表达能力 | 能否准确表达餐饮业务中的所有场景 |
| 业务语义清晰度 | 模型是否清晰反映业务概念 |
| 跨域一致性 | 是否能保证跨域数据的一致性 |
| Identity 一致性 | 是否能保证同一业务对象的唯一身份 |
| 数据重复风险 | 是否存在数据冗余和不一致风险 |
| 扩展能力 | 是否支持业务增长和新场景 |
| 门店差异能力 | 是否支持多门店差异化配置 |
| Costing 能力 | 是否支持准确的成本计算 |
| Finance Integration | 是否能与财务系统良好集成 |

---

## 2. Model A — Independent Entities（独立实体模型）

### 2.1 架构描述

```
Product (独立实体)
  ├── id
  ├── name
  ├── type: food | material | ...
  ├── sku
  ├── category
  └── 属性...

Food (独立实体)
  ├── id
  ├── product_id (引用)
  ├── price
  ├── menu_config
  └── 销售属性...

Material (独立实体)
  ├── id
  ├── product_id (引用)
  ├── cost
  ├── supplier_info
  └── 采购属性...

Inventory (独立实体)
  ├── id
  ├── material_id (引用)
  ├── quantity
  ├── warehouse_id
  └── 库存属性...
```

### 2.2 业务场景验证

#### 场景1：可乐的多角色管理

**业务需求**：可乐作为销售商品、采购物料、库存项、菜品原料

**Model A 实现**：
```sql
-- 可乐作为销售商品
INSERT INTO food (id, product_id, price, menu_config)
VALUES (1, 101, 3.00, '{"visibility": "menu"}');

-- 可乐作为采购物料
INSERT INTO material (id, product_id, cost, supplier_id)
VALUES (1, 101, 2.50, 501);

-- 可乐作为库存项
INSERT INTO inventory (id, material_id, quantity, warehouse_id)
VALUES (1, 1, 100, 1001);

-- 可乐作为菜品原料
INSERT INTO dish_recipe (dish_id, ingredient_id, quantity)
VALUES (201, 1, 0.2);
```

**问题**：
- 需要维护四个独立的记录
- 需要同步更新多个表
- 数据一致性难以保证

#### 场景2：门店差异化配置

**业务需求**：同一商品在不同门店有不同价格

**Model A 实现**：
```sql
-- 门店A的价格
INSERT INTO food (id, product_id, price, store_id)
VALUES (1, 101, 3.00, 1001);

-- 门店B的价格
INSERT INTO food (id, product_id, price, store_id)
VALUES (2, 101, 3.50, 1002);
```

**问题**：
- 同一商品在不同门店需要创建不同的food记录
- 无法统一管理商品信息
- 扩展性差

### 2.3 评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 业务表达能力 | ⭐⭐⭐ | 能表达所有当前业务 |
| 业务语义清晰度 | ⭐⭐⭐ | 每个实体职责明确 |
| 跨域一致性 | ⭐⭐ | 需要外键同步，冗余数据 |
| Identity 一致性 | ⭐ | 同一商品在不同域可能有不同记录 |
| 数据重复风险 | ⭐⭐ | 名称、属性等可能重复存储 |
| 扩展能力 | ⭐⭐ | 新增业务角色需要新建表 |
| 门店差异能力 | ⭐⭐⭐ | 每个门店可独立配置 |
| Costing 能力 | ⭐⭐ | 需要跨实体计算 |
| Finance Integration | ⭐⭐ | 需要映射逻辑 |

**加权得分**：3.0 × 0.4 + 3.0 × 0.3 + 2.0 × 0.2 + 3.0 × 0.1 = **2.8**

---

## 3. Model B — Canonical Item + Business Roles（统一身份+业务角色模型）

### 3.1 架构描述

```
Canonical Item (统一身份)
  ├── id
  ├── name
  ├── sku
  ├── category
  ├── brand
  └── 核心属性...

Business Role (业务角色)
  ├── id
  ├── canonical_item_id
  ├── role_type: food | material | stock_item | recipe_ingredient
  ├── store_id (可选，支持门店差异化)
  ├── domain_config (JSON/扩展表)
  └── 领域特定属性...
```

### 3.2 业务场景验证

#### 场景1：可乐的多角色管理

**业务需求**：可乐作为销售商品、采购物料、库存项、菜品原料

**Model B 实现**：
```sql
-- 可乐的统一身份
INSERT INTO canonical_item (id, name, sku, category, brand)
VALUES (1, '可口可乐', 'COLA001', '饮料', '可口可乐');

-- 可乐作为销售商品
INSERT INTO business_role (id, canonical_item_id, role_type, domain_config)
VALUES (1, 1, 'food', '{"price": 3.00, "menu_config": {...}}');

-- 可乐作为采购物料
INSERT INTO business_role (id, canonical_item_id, role_type, domain_config)
VALUES (2, 1, 'material', '{"cost": 2.50, "supplier_id": 501}');

-- 可乐作为库存项
INSERT INTO business_role (id, canonical_item_id, role_type, domain_config)
VALUES (3, 1, 'stock_item', '{"quantity": 100, "warehouse_id": 1001}');

-- 可乐作为菜品原料
INSERT INTO business_role (id, canonical_item_id, role_type, domain_config)
VALUES (4, 1, 'recipe_ingredient', '{"quantity": 0.2, "unit": "罐"}');
```

**优势**：
- 统一身份，消除数据冗余
- 角色可以灵活添加和移除
- 跨域查询简单

#### 场景2：门店差异化配置

**业务需求**：同一商品在不同门店有不同价格

**Model B 实现**：
```sql
-- 可乐的统一身份
INSERT INTO canonical_item (id, name, sku, category, brand)
VALUES (1, '可口可乐', 'COLA001', '饮料', '可口可乐');

-- 门店A的销售角色
INSERT INTO business_role (id, canonical_item_id, role_type, store_id, domain_config)
VALUES (1, 1, 'food', 1001, '{"price": 3.00}');

-- 门店B的销售角色
INSERT INTO business_role (id, canonical_item_id, role_type, store_id, domain_config)
VALUES (2, 1, 'food', 1002, '{"price": 3.50}');
```

**优势**：
- 统一管理商品信息
- 门店差异化通过角色实现
- 扩展性强

### 3.3 评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 业务表达能力 | ⭐⭐⭐⭐ | 灵活的角色定义 |
| 业务语义清晰度 | ⭐⭐⭐⭐ | 统一身份 + 角色分离 |
| 跨域一致性 | ⭐⭐⭐⭐ | 天然一致，同一身份 |
| Identity 一致性 | ⭐⭐⭐⭐⭐ | 核心优势 |
| 数据重复风险 | ⭐⭐⭐⭐ | 最小化重复 |
| 扩展能力 | ⭐⭐⭐⭐⭐ | 新增角色只需新记录 |
| 门店差异能力 | ⭐⭐⭐ | 角色可按门店区分 |
| Costing 能力 | ⭐⭐⭐⭐ | 统一计算基础 |
| Finance Integration | ⭐⭐⭐⭐ | 清晰的映射 |

**加权得分**：4.0 × 0.4 + 4.5 × 0.3 + 4.0 × 0.2 + 2.0 × 0.1 = **4.0**

---

## 4. Model C — Hybrid Domain Model（混合领域模型）

### 4.1 架构描述

```
Canonical Identity (统一身份)
  ├── id
  ├── name
  ├── global_sku
  ├── category
  ├── brand
  └── 核心属性...

Commercial Definition (商业定义)
  ├── canonical_id
  ├── saleable: boolean
  ├── purchasable: boolean
  ├── storable: boolean
  ├── sell_price
  ├── cost_price
  └── 业务开关...

Operational Definition (运营定义)
  ├── canonical_id
  ├── domain: pos | procurement | inventory | recipe | costing
  ├── store_id (可选)
  ├── config (JSON/扩展)
  └── 领域特定配置...

Role Views (角色视图/聚合)
  ├── Food View (销售视图)
  │   ├── price
  │   ├── menu_config
  │   └── POS 属性...
  ├── Material View (采购/库存视图)
  │   ├── cost
  │   ├── supplier_info
  │   └── 库存属性...
  ├── Recipe View (配方视图)
  │   ├── ingredients
  │   ├── process
  │   └── BOM 属性...
  └── Finance View (财务视图)
      ├── gl_account
      └── 财务属性...
```

### 4.2 业务场景验证

#### 场景1：可乐的多角色管理

**业务需求**：可乐作为销售商品、采购物料、库存项、菜品原料

**Model C 实现**：
```sql
-- 可乐的统一身份
INSERT INTO canonical_identity (id, name, global_sku, category, brand)
VALUES (1, '可口可乐', 'COLA001', '饮料', '可口可乐');

-- 可乐的商业定义
INSERT INTO commercial_definition (canonical_id, saleable, purchasable, storable, sell_price, cost_price)
VALUES (1, true, true, true, 3.00, 2.50);

-- 可乐的销售运营定义
INSERT INTO operational_definition (canonical_id, domain, store_id, config)
VALUES (1, 'pos', NULL, '{"menu_config": {...}}');

-- 可乐的采购运营定义
INSERT INTO operational_definition (canonical_id, domain, store_id, config)
VALUES (1, 'procurement', NULL, '{"supplier_id": 501, "lead_time": 7}');

-- 可乐的库存运营定义
INSERT INTO operational_definition (canonical_id, domain, store_id, config)
VALUES (1, 'inventory', NULL, '{"warehouse_id": 1001, "reorder_point": 50}');

-- 可乐作为菜品原料的配方定义
INSERT INTO operational_definition (canonical_id, domain, store_id, config)
VALUES (1, 'recipe', NULL, '{"quantity": 0.2, "unit": "罐"}');
```

**优势**：
- 清晰的分层设计
- 统一身份保证一致性
- 灵活的运营配置

#### 场景2：门店差异化配置

**业务需求**：同一商品在不同门店有不同价格

**Model C 实现**：
```sql
-- 可乐的统一身份
INSERT INTO canonical_identity (id, name, global_sku, category, brand)
VALUES (1, '可口可乐', 'COLA001', '饮料', '可口可乐');

-- 可乐的全局商业定义
INSERT INTO commercial_definition (canonical_id, saleable, purchasable, storable, sell_price, cost_price)
VALUES (1, true, true, true, 3.00, 2.50);

-- 门店A的销售运营定义
INSERT INTO operational_definition (canonical_id, domain, store_id, config)
VALUES (1, 'pos', 1001, '{"price": 3.00, "menu_config": {...}}');

-- 门店B的销售运营定义
INSERT INTO operational_definition (canonical_id, domain, store_id, config)
VALUES (1, 'pos', 1002, '{"price": 3.50, "menu_config": {...}}');
```

**优势**：
- 全局定义统一管理
- 门店差异化通过运营定义实现
- 最灵活的扩展能力

### 4.3 评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 业务表达能力 | ⭐⭐⭐⭐⭐ | 最全面 |
| 业务语义清晰度 | ⭐⭐⭐⭐ | 清晰的分层 |
| 跨域一致性 | ⭐⭐⭐⭐ | 统一身份保证 |
| Identity 一致性 | ⭐⭐⭐⭐⭐ | 核心设计目标 |
| 数据重复风险 | ⭐⭐⭐⭐⭐ | 最小化 |
| 扩展能力 | ⭐⭐⭐⭐⭐ | 最灵活 |
| 门店差异能力 | ⭐⭐⭐⭐⭐ | Operational 定义支持 |
| Costing 能力 | ⭐⭐⭐⭐⭐ | 跨域计算最优 |
| Finance Integration | ⭐⭐⭐⭐⭐ | Finance View 专门支持 |

**加权得分**：5.0 × 0.4 + 5.0 × 0.3 + 4.5 × 0.2 + 2.5 × 0.1 = **4.65**

---

## 5. 综合对比

### 5.1 评分对比

| 维度 | Model A | Model B | Model C |
|------|---------|---------|---------|
| 业务表达能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 业务语义清晰度 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 跨域一致性 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Identity 一致性 | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 数据重复风险 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 扩展能力 | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 门店差异能力 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Costing 能力 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Finance Integration | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

### 5.2 加权得分

| 模型 | Business Correctness (40%) | Long-term Model Integrity (30%) | Cross-Domain Consistency (20%) | Migration Cost (10%) | **总分** |
|------|---------------------------|--------------------------------|------------------------------|---------------------|----------|
| Model A | 3.0 | 3.0 | 2.0 | 3.0 | **2.8** |
| Model B | 4.0 | 4.5 | 4.0 | 2.0 | **4.0** |
| Model C | 5.0 | 5.0 | 4.5 | 2.5 | **4.65** |

### 5.3 权衡分析

#### Model A 的权衡

**接受的权衡**：
- 迁移复杂度低（接近当前结构）
- 实现简单，团队学习成本低

**拒绝的权衡**：
- 无法解决跨域一致性问题
- 长期维护成本高
- 不支持业务增长

#### Model B 的权衡

**接受的权衡**：
- 身份一致性最优
- 扩展性强
- 跨域查询简单

**拒绝的权衡**：
- 迁移成本高（需要重构）
- 可能过度设计

#### Model C 的权衡

**接受的权衡**：
- 迁移复杂度中等
- 设计复杂度较高

**拒绝的权衡**：
- Model A 的简单性无法解决核心问题
- Model B 的极致统一可能增加不必要的复杂度

---

## 6. 推荐结论

### 6.1 推荐模型

**推荐 Model C — Hybrid Domain Model（混合领域模型）**

### 6.2 推荐理由

1. **Business Correctness（40%权重）**：Model C 在业务表达能力上得分最高，能够全面覆盖餐饮业务的所有场景
2. **Long-term Model Integrity（30%权重）**：Model C 的分层设计保证了长期模型的完整性和可维护性
3. **Cross-Domain Consistency（20%权重）**：Model C 通过 Canonical Identity 保证了跨域数据一致性
4. **Migration Cost（10%权重）**：虽然迁移成本较高，但权重最低，且长期收益远大于短期成本

### 6.3 实施建议

1. **Phase 1: 设计验证（2-4周）**
   - 定义 Canonical Identity 的数据模型
   - 定义各域 View 的数据模型
   - 创建原型验证关键场景

2. **Phase 2: 基础设施（4-6周）**
   - 实现 Canonical Identity 表
   - 实现各域 View 表
   - 实现数据访问层

3. **Phase 3: 数据迁移（4-8周）**
   - 制定迁移计划
   - 实现迁移脚本
   - 灰度迁移验证

4. **Phase 4: 业务迁移（8-12周）**
   - 逐域迁移
   - 业务验证
   - 全量切换

---

**状态**：只读分析任务，不修改任何代码  
**日期**：2026-09-09
