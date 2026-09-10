# Type vs Role 分析

> **核心问题**：Type（类型）和 Role（角色）是否应该分离？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

### 1.1 什么是 Type？

Type 是业务对象的**固有分类**，描述"这个东西是什么"。

- Food（菜品）：可销售的成品
- Material（物料）：可采购、可存储的原料
- Product（产品）：已废弃的遗留概念

### 1.2 什么是 Role？

Role 是业务对象在**特定业务上下文中的行为**，描述"这个东西在这个场景中做什么"。

- SELLABLE：在 POS 场景中可销售
- PURCHASABLE：在采购场景中可采购
- STOCKABLE：在库存场景中可存储
- INGREDIENT：在 Recipe 中作为原料

### 1.3 核心问题

- Type 和 Role 是否应该分离？
- 同一个业务对象能否同时拥有多个 Type？
- Type 和 Role 的关系是什么？

---

## 2. 当前系统现实

### 2.1 Type 的当前状态

| Type | 对应表 | 状态 | 说明 |
|------|--------|------|------|
| Food | foods | 活跃 | 可销售的成品 |
| Material | material_archives | 活跃 | 可采购、可存储的原料 |
| Product | product | 废弃 | 已被 material_archives 替代 |

### 2.2 Role 的当前状态

| Role | 对应表/字段 | 状态 | 说明 |
|------|------------|------|------|
| SELLABLE | foods.price, order_items.food_id | 活跃 | 可销售 |
| PURCHASABLE | purchase_order_items.material_id | 活跃 | 可采购 |
| STOCKABLE | inventory.material_id | 活跃 | 可存储 |
| INGREDIENT | dish_recipe.ingredient_id | 活跃 | 原料 |

### 2.3 关键发现

1. **Type 和 Role 当前是耦合的**
   - Food Type = SELLABLE Role（几乎等价）
   - Material Type = PURCHASABLE + STOCKABLE Role（几乎等价）

2. **但存在反例**
   - 一个 Material 可以是 PURCHASABLE 但不是 STOCKABLE（直送门店）
   - 一个 Food 可以是 SELLABLE 但不是 INGREDIENT（成品菜不作为原料）
   - 一个 Material 可以是 INGREDIENT 但不是 SELLABLE（原材料不直接销售）

3. **Type 和 Role 的边界模糊**
   - Food 是 Type 还是 Role？
   - Material 是 Type 还是 Role？

---

## 3. Type vs Role 的区分标准

### 3.1 判断标准

| 标准 | Type | Role |
|------|------|------|
| **生命周期** | 与业务对象同生共灭 | 可独立添加/移除 |
| **唯一性** | 一个对象只有一个 Type | 一个对象可有多个 Role |
| **数据位置** | 主数据表 | 配置表/关联表 |
| **变更频率** | 低（创建时确定） | 高（业务变化时调整） |
| **业务语义** | "是什么" | "做什么" |

### 3.2 应用到当前系统

| 业务对象 | Type? | Role? | 理由 |
|----------|-------|-------|------|
| Food | ❌ Type | ✅ Role | 同一"可乐"在不同门店可以是 SELLABLE 或不 SELLABLE |
| Material | ❌ Type | ✅ Role | 同一"可乐"在不同门店可以是 PURCHASABLE 或不 PURCHASABLE |
| SELLABLE | - | ✅ Role | 可独立添加/移除 |
| PURCHASABLE | - | ✅ Role | 可独立添加/移除 |
| STOCKABLE | - | ✅ Role | 可独立添加/移除 |
| INGREDIENT | - | ✅ Role | 可独立添加/移除 |

### 3.3 结论

**Type 和 Role 应该分离**。当前系统的 Food 和 Material 实际上是 Role 的组合，而不是独立的 Type。

---

## 4. 目标模型

### 4.1 Canonical Identity + Role 模型

```
Canonical Identity (统一身份)
  ├── id: string (UUID)
  ├── name: string
  ├── code: string (SKU)
  ├── description: string
  └── core_attributes: JSON

Business Role (业务角色)
  ├── id: string (UUID)
  ├── canonical_item_id: string (FK → Canonical Identity)
  ├── role_type: enum (SELLABLE, PURCHASABLE, STOCKABLE, INGREDIENT, ...)
  ├── domain_config: JSON (角色特定配置)
  ├── store_id: string? (门店级别角色)
  ├── status: enum (ACTIVE, INACTIVE)
  └── created_at, updated_at
```

### 4.2 角色特定配置

| Role | 域 | 配置字段 |
|------|-----|----------|
| SELLABLE | POS | price, menu_config, display_info |
| PURCHASABLE | Procurement | supplier_id, purchase_unit, lead_time |
| STOCKABLE | Inventory | warehouse_id, safety_stock, max_stock |
| INGREDIENT | Recipe | recipe_id, quantity, yield |

### 4.3 与当前系统的映射

| 当前表 | 目标映射 | 说明 |
|--------|----------|------|
| foods | Canonical Identity + SELLABLE Role | 食品=统一身份+可销售角色 |
| material_archives | Canonical Identity + PURCHASABLE Role | 物料=统一身份+可采购角色 |
| inventory | STOCKABLE Role 实例 | 库存=可存储角色的实例 |
| dish_recipe | INGREDIENT Role 关联 | 配方=原料角色的关联 |
| order_items | SELLABLE Role 引用 | 订单=可销售角色的引用 |
| purchase_order_items | PURCHASABLE Role 引用 | 采购=可采购角色的引用 |

---

## 5. 业务场景验证

### 5.1 场景一：可乐

**当前状态**：
- foods 表：可乐（food_id=F001, price=3.00）
- material_archives 表：可乐（material_id=M001）
- inventory 表：可乐（material_id=M001, warehouse_id=W01, current_stock=100）
- dish_recipe 表：可乐鸡翅（food_id=F002, ingredient_id=M001, quantity=0.5）

**目标状态**：
- Canonical Identity：可乐（id=C001, name="可口可乐330ml"）
- SELLABLE Role：可乐（canonical_item_id=C001, role_type=SELLABLE, price=3.00）
- PURCHASABLE Role：可乐（canonical_item_id=C001, role_type=PURCHASABLE, supplier_id=S001）
- STOCKABLE Role：可乐（canonical_item_id=C001, role_type=STOCKABLE, warehouse_id=W01）
- INGREDIENT Role：可乐（canonical_item_id=C001, role_type=INGREDIENT, recipe_id=R001）

### 5.2 场景二：鸡翅

**当前状态**：
- material_archives 表：鸡翅（material_id=M002）
- inventory 表：鸡翅（material_id=M002, warehouse_id=W01, current_stock=50）
- dish_recipe 表：可乐鸡翅（food_id=F002, ingredient_id=M002, quantity=0.3）

**目标状态**：
- Canonical Identity：鸡翅（id=C002, name="鸡翅中"）
- PURCHASABLE Role：鸡翅（canonical_item_id=C002, role_type=PURCHASABLE, supplier_id=S002）
- STOCKABLE Role：鸡翅（canonical_item_id=C002, role_type=STOCKABLE, warehouse_id=W01）
- INGREDIENT Role：鸡翅（canonical_item_id=C002, role_type=INGREDIENT, recipe_id=R001）

### 5.3 场景三：大米

**当前状态**：
- material_archives 表：大米（material_id=M003）
- inventory 表：大米（material_id=M003, warehouse_id=W01, current_stock=200）
- dish_recipe 表：米饭（food_id=F003, ingredient_id=M003, quantity=0.2）

**目标状态**：
- Canonical Identity：大米（id=C003, name="东北大米5kg"）
- PURCHASABLE Role：大米（canonical_item_id=C003, role_type=PURCHASABLE, supplier_id=S003）
- STOCKABLE Role：大米（canonical_item_id=C003, role_type=STOCKABLE, warehouse_id=W01）
- INGREDIENT Role：大米（canonical_item_id=C003, role_type=INGREDIENT, recipe_id=R002）

---

## 6. 优势分析

### 6.1 业务灵活性

- 同一业务对象可在不同门店拥有不同 Role
- 新增 Role 无需修改核心数据模型
- Role 可独立添加/移除，不影响其他 Role

### 6.2 数据一致性

- Canonical Identity 保证全局唯一
- Role 配置独立管理，避免数据冗余
- 跨域查询通过 Canonical Identity 关联

### 6.3 扩展能力

- 新增 Role 类型只需新增枚举值
- 新增域配置只需新增 JSON 结构
- 支持门店级别差异化

---

## 7. 风险分析

### 7.1 高风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 迁移期间数据不一致 | 业务中断 | 分阶段迁移 + 双写期 |
| Canonical Identity 设计不当 | 全局影响 | 充分评审 + 原型验证 |

### 7.2 中风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 团队学习曲线 | 开发效率短期下降 | 培训 + 文档 + 代码示例 |
| Role 配置复杂度 | 配置错误 | 配置校验 + UI 工具 |

### 7.3 低风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 性能下降 | 查询变慢 | 索引优化 + 读写分离 |
| 过度设计 | 资源浪费 | 最小可用原则 |

---

## 8. 开放问题

### 8.1 需要产品决策的事项

| 问题 | 影响范围 | 优先级 |
|------|----------|--------|
| 门店是否需要独立的价格体系？ | POS 域设计 | 高 |
| 配方是否需要按门店差异化？ | Recipe 域设计 | 高 |
| 采购是否需要按门店独立供应商？ | Procurement 域设计 | 中 |
| 库存是否需要按门店独立管理？ | Inventory 域设计 | 中 |

### 8.2 需要架构决策的事项

| 问题 | 影响范围 | 优先级 |
|------|----------|--------|
| Canonical Identity 的唯一键如何定义？ | 全局 | 高 |
| Role 配置使用 JSON 还是扩展表？ | 存储设计 | 高 |
| 跨域查询如何优化性能？ | 查询设计 | 中 |

---

## 9. 总结

| 问题 | 结论 |
|------|------|
| Type 和 Role 是否应该分离？ | **是**，应该分离 |
| 当前系统的 Food/Material 是 Type 还是 Role？ | **Role**，不是 Type |
| 目标模型是什么？ | **Canonical Identity + Role** |
| 推荐方案？ | **Model C: Hybrid Domain Model** |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-09
**作者**：AI 架构总控
