# 跨域影响分析

## 域定义

### POS 域（销售）
- **核心实体**：Food（菜品/食品）
- **关键属性**：price, menu_config, display_info
- **关键外键**：order_detail.food_id
- **业务流程**：菜品点单 -> 订单生成 -> 支付

### 采购域（Procurement）
- **核心实体**：Material（商品档案）
- **关键属性**：cost, supplier_info, lead_time
- **关键外键**：purchase_order.material_id
- **业务流程**：采购申请 -> 采购订单 -> 入库

### 库存域（Inventory）
- **核心实体**：Stock Item（库存项）
- **关键属性**：quantity, location, batch
- **关键外键**：inventory.material_id
- **业务流程**：入库 -> 库存管理 -> 出库

### 配方域（Recipe/BOM）
- **核心实体**：Dish Recipe（菜品配方）
- **关键属性**：ingredients, quantity, process
- **关键外键**：dish_recipe.food_id, dish_recipe.material_id
- **业务流程**：菜品 -> 配方定义 -> 物料消耗

---

## 跨域影响分析

### 1. POS -> Recipe -> Inventory 影响链

**场景**：菜品销售触发库存消耗

```
POS 订单
  |
order_detail (food_id)
  |
dish_recipe (food_id -> material_id)
  |
inventory (material_id)
  |
库存扣减
```

**影响**：
- POS 的 `food_id` 必须能在 Recipe 域找到对应 `material_id`
- Recipe 变更（配方修改）直接影响库存计算
- **当前风险**：如果 Recipe 数据不完整，POS 销售无法触发库存更新

### 2. 采购 -> Inventory 影响链

**场景**：采购入库更新库存

```
采购订单
  |
purchase_order (material_id)
  |
inventory (material_id)
  |
库存增加
```

**影响**：
- 采购域和库存域使用相同 `material_id`，一致性较好
- 但 `material_id` 的定义（`material_archives`）可能与 `food` 不一致
- **当前风险**：`product` 表废弃后，`material_archives` 成为唯一基础数据源

### 3. Recipe 跨域桥接影响

**场景**：菜品配方定义

```
Food (POS 域)
  |
dish_recipe
  |
Material (Procurement/Inventory 域)
```

**影响**：
- Recipe 是 POS 和 Procurement/Inventory 的唯一桥接点
- Recipe 的准确性直接决定成本计算和库存管理的准确性
- **当前风险**：Recipe 数据质量直接影响下游所有域

---

## 域间数据一致性分析

### 一致性矩阵

| 从 \ 到 | POS | Procurement | Inventory | Recipe | Finance |
|---------|-----|-------------|-----------|--------|---------|
| POS | - | 需映射 | 需映射 | 直接关联 | 需映射 |
| Procurement | 需映射 | - | 直接关联 | 需映射 | 需映射 |
| Inventory | 需映射 | 直接关联 | - | 需映射 | 需映射 |
| Recipe | 直接关联 | 需映射 | 需映射 | - | 需映射 |
| Finance | 需映射 | 需映射 | 需映射 | 需映射 | - |

### 一致性风险

| 风险 | 严重程度 | 说明 |
|------|----------|------|
| food_id 与 material_id 映射丢失 | 高 | Recipe 数据不完整导致 |
| 名称不一致 | 中 | 同一商品在不同域名称不同 |
| 价格/成本不同步 | 中 | POS 价格和采购成本独立维护 |
| 新增商品未同步 | 中 | 新增菜品未添加对应物料 |
| 删除商品未清理 | 中 | 删除菜品未清理关联数据 |

---

## 门店差异影响

### 门店差异化需求

| 差异点 | POS | Procurement | Inventory | Recipe |
|--------|-----|-------------|-----------|--------|
| 价格差异 | 需要 | - | - | - |
| 供应商差异 | - | 需要 | - | - |
| 库存位置 | - | - | 需要 | - |
| 配方差异 | - | - | - | 需要 |
| 菜品可用性 | 需要 | - | - | - |

### 门店差异化实现

| 模型 | POS 差异 | 采购差异 | 库存差异 | 配方差异 |
|------|----------|----------|----------|----------|
| Model A | 独立记录 | 独立记录 | 独立记录 | 独立记录 |
| Model B | 角色属性 | 角色属性 | 角色属性 | 角色属性 |
| Model C | Operational Config | Operational Config | Operational Config | Operational Config |

---

## 成本计算影响

### 成本计算链

```
采购成本 (Procurement)
  |
库存成本 (Inventory)
  |
配方成本 (Recipe)
  |
菜品成本 (POS)
  |
销售毛利 (Finance)
```

### 成本计算依赖

| 依赖 | 影响 | 风险 |
|------|------|------|
| 采购价格 -> 库存成本 | 采购价格变动影响库存估值 | 价格同步延迟 |
| 库存成本 -> 配方成本 | 库存成本影响菜品成本 | 成本计算不准确 |
| 配方成本 -> 菜品成本 | 配方变动影响菜品成本 | 配方数据不完整 |
| 菜品成本 -> 销售毛利 | 菜品成本影响利润计算 | 成本核算错误 |

---

## 迁移影响评估

### 数据迁移范围

| 域 | 源表 | 目标表 | 迁移复杂度 |
|----|------|--------|------------|
| POS | foods | canonical_items + food_roles | 低 |
| Procurement | material_archives | canonical_items + material_roles | 中 |
| Inventory | inventory | canonical_items + inventory_roles | 中 |
| Recipe | dish_recipe | recipe_links (cross-role) | 高 |
| Order | order_detail | order_detail (外键更新) | 低 |
| Purchase | purchase_order | purchase_order (外键更新) | 低 |

### 迁移风险

| 风险 | 严重程度 | 缓解措施 |
|------|----------|----------|
| 数据丢失 | 高 | 全量备份 + 增量同步 |
| 外键断裂 | 高 | 分阶段迁移 + 数据校验 |
| 业务中断 | 中 | 灰度发布 + 回滚计划 |
| 性能下降 | 中 | 索引优化 + 读写分离 |
