> **DOCUMENT STATUS:** SUPERSEDED
> **SUPERSEDED BY:** 03-review/business-item-canonical-semantic-reassessment.md
> **SOURCE TASK:** BUSINESS-ITEM-MODEL-002

# 候选模型比较

## 当前系统现实总结

| 表名 | 业务含义 | 关键外键 | 备注 |
|------|----------|----------|------|
| foods | 菜品/食品（可销售） | - | 主要销售实体 |
| product | 产品/物料基础信息 | - | 已废弃 |
| material_archives | 商品档案（可采购、可存储） | - | 采购和库存基础 |
| inventory | 库存管理 | material_id | 关联商品档案 |
| dish_recipe | 菜品配方/BOM | food_id → food, material_id → material | 菜品与物料关系 |
| order_detail | 订单明细 | food_id | 关联菜品 |
| purchase_order | 采购订单 | material_id | 关联商品档案 |

**核心发现**：
- 销售域使用 `food_id`
- 采购域使用 `material_id`
- 库存域使用 `material_id`
- 配方域桥接 `food_id ↔ material_id`
- `product` 表已废弃，但可能仍有残留数据

---

## Model A — Independent Entities（独立实体模型）

### 架构描述

```
Product (独立)
  ├── id
  ├── name
  ├── type: food | material | ...
  └── 属性...

Food (独立)
  ├── id
  ├── product_id (引用)
  ├── price
  └── 销售属性...

Material (独立)
  ├── id
  ├── product_id (引用)
  ├── cost
  └── 采购属性...

Stock Item (独立)
  ├── id
  ├── material_id (引用)
  ├── quantity
  └── 库存属性...
```

### 评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 业务表达能力 | ⭐⭐⭐ | 能表达所有当前业务 |
| 业务语义清晰度 | ⭐⭐⭐ | 每个实体职责明确 |
| 跨域一致性 | ⭐⭐ | 需要外键同步，冗余数据 |
| Identity 一致性 | ⭐ | 同一商品在不同域可能有不同记录 |
| 数据重复风险 | ⭐⭐ | 名称、属性等可能重复存储 |
| 扩展能力 | ⭐⭐ | 新增业务角色需要新建表 |
| 门店差异能力 | ⭐⭐⭐ | 每个门店可独立配置 |
| 采购能力 | ⭐⭐⭐ | Material 专为采购设计 |
| 库存能力 | ⭐⭐⭐ | Stock Item 专为库存设计 |
| POS 能力 | ⭐⭐⭐ | Food 专为销售设计 |
| Recipe 能力 | ⭐⭐⭐ | 通过关系表桥接 |
| Costing 能力 | ⭐⭐ | 需要跨实体计算 |
| Finance Integration | ⭐⭐ | 需要映射逻辑 |
| 历史数据兼容 | ⭐⭐⭐ | 最接近当前结构 |
| 迁移复杂度 | ⭐⭐⭐ | 变更最小 |
| 长期维护成本 | ⭐⭐ | 同步逻辑维护成本 |

---

## Model B — Canonical Item + Business Roles（统一身份+业务角色模型）

### 架构描述

```
Canonical Item (统一身份)
  ├── id
  ├── name
  ├── sku
  └── 属性...

Business Role (业务角色)
  ├── id
  ├── canonical_item_id
  ├── role_type: food | material | stock_item | ...
  ├── domain_specific_attributes (JSON/扩展表)
  └── ...
```

### 评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 业务表达能力 | ⭐⭐⭐⭐ | 灵活的角色定义 |
| 业务语义清晰度 | ⭐⭐⭐⭐ | 统一身份 + 角色分离 |
| 跨域一致性 | ⭐⭐⭐⭐ | 天然一致，同一身份 |
| Identity 一致性 | ⭐⭐⭐⭐⭐ | 核心优势 |
| 数据重复风险 | ⭐⭐⭐⭐ | 最小化重复 |
| 扜展能力 | ⭐⭐⭐⭐⭐ | 新增角色只需新记录 |
| 门店差异能力 | ⭐⭐⭐ | 角色可按门店区分 |
| 采购能力 | ⭐⭐⭐⭐ | material 角色 |
| 库存能力 | ⭐⭐⭐⭐ | stock_item 角色 |
| POS 能力 | ⭐⭐⭐⭐ | food 角色 |
| Recipe 能力 | ⭐⭐⭐⭐ | 通过角色关联 |
| Costing 能力 | ⭐⭐⭐⭐ | 统一计算基础 |
| Finance Integration | ⭐⭐⭐⭐ | 清晰的映射 |
| 历史数据兼容 | ⭐⭐ | 需要转换逻辑 |
| 迁移复杂度 | ⭐ | 需要重构 |
| 长期维护成本 | ⭐⭐⭐⭐ | 统一维护 |

---

## Model C — Hybrid Domain Model（混合领域模型）

### 架构描述

```
Canonical Identity (统一身份)
  ├── id
  ├── name
  ├── global_sku
  └── 核心属性...

Commercial Definition (商业定义)
  ├── canonical_id
  ├── saleable: boolean
  ├── purchasable: boolean
  ├── storable: boolean
  └── 业务开关...

Operational Definition (运营定义)
  ├── canonical_id
  ├── domain: pos | procurement | inventory | recipe | costing
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

### 评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 业务表达能力 | ⭐⭐⭐⭐⭐ | 最全面 |
| 业务语义清晰度 | ⭐⭐⭐⭐ | 清晰的分层 |
| 跨域一致性 | ⭐⭐⭐⭐ | 统一身份保证 |
| Identity 一致性 | ⭐⭐⭐⭐⭐ | 核心设计目标 |
| 数据重复风险 | ⭐⭐⭐⭐⭐ | 最小化 |
| 扩展能力 | ⭐⭐⭐⭐⭐ | 最灵活 |
| 门店差异能力 | ⭐⭐⭐⭐⭐ | Operational 定义支持 |
| 采购能力 | ⭐⭐⭐⭐ | Material View |
| 库存能力 | ⭐⭐⭐⭐ | Material View |
| POS 能力 | ⭐⭐⭐⭐ | Food View |
| Recipe 能力 | ⭐⭐⭐⭐ | Recipe View |
| Costing 能力 | ⭐⭐⭐⭐⭐ | 跨域计算最优 |
| Finance Integration | ⭐⭐⭐⭐⭐ | Finance View 专门支持 |
| 历史数据兼容 | ⭐⭐⭐ | 需要适配器 |
| 迁移复杂度 | ⭐⭐ | 中等复杂 |
| 长期维护成本 | ⭐⭐⭐⭐ | 设计复杂但维护清晰 |

---

## 综合对比

| 维度 | Model A | Model B | Model C |
|------|---------|---------|---------|
| 业务表达能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 业务语义清晰度 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 跨域一致性 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Identity 一致性 | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 数据重复风险 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 扩展能力 | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 门店差异能力 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 采购能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 库存能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| POS 能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Recipe 能力 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Costing 能力 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Finance Integration | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 历史数据兼容 | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| 迁移复杂度 | ⭐⭐⭐ | ⭐ | ⭐⭐ |
| 长期维护成本 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **总分** | **44** | **60** | **70** |
