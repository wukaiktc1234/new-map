# DEC-006-REFINED 升级决策包

## 1. 决策升级说明

### 1.1 为什么需要升级 DEC-006

**原 DEC-006 范围**：
- Product / Food / Material 谁废弃谁保留
- 继承模型 vs 组合模型

**升级后的 DEC-006-REFINED 范围**：
- Canonical Business Item / Product / Food / Material / Stock / Recipe 的目标业务语义边界
- 跨域数据所有权和一致性保证
- 业务角色的精确定义和边界

### 1.2 升级原因

1. **原 DEC-006 推荐方案需要修正**：继承模型缺乏数据支撑
2. **业务语义边界需要精确定义**：当前边界模糊导致数据不一致
3. **跨域一致性需要保证**：不同域使用不同标识符导致查询复杂
4. **扩展性需要考虑**：非餐饮场景（如零售）需要支持

---

## 2. 决策包结构

### 2.1 DEC-006-REFINED 决策包

| 字段 | 值 |
|------|-----|
| **决策ID** | DEC-006-REFINED |
| **决策标题** | Canonical Business Item / Product / Food / Material / Stock / Recipe 的目标业务语义边界 |
| **状态** | 🟡 OPEN |
| **优先级** | P0 |
| **负责人** | Product Owner + Architecture Owner |
| **截止日期** | 2026-10-15 |
| **阻塞影响** | 影响多态数据建模、跨域一致性、事件驱动架构 |

---

## 3. 需要 Product Owner 决定的事项

### 3.1 商品概念边界定义

**问题**：Product、Food、Material 的业务边界是什么？

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | Product 作为通用商品，Food/Material 作为子类型 | 保持现有逻辑 | 概念模糊，边界不清 |
| B | Food 专注可销售，Material 专注可采购/可存储 | 概念清晰 | 需要重新梳理数据归属 |
| C | 引入 Canonical Item 作为统一身份 | 身份一致性最优 | 迁移成本高 |

**推荐**：B — Food 专注可销售，Material 专注可采购/可存储

**理由**：
1. 概念清晰：Food 专注可售菜品管理，Material 专注原材料采购管理
2. 事件驱动友好：每个实体可独立定义事件，符合 LOCK-001 架构约束
3. 扩展性：非餐饮场景可按需扩展新的商品类型（如 Retail Product）

### 3.2 库存管理对象定义

**问题**：Inventory 管理的核心对象是什么？

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 管理 Food（菜品库存） | 与销售直接关联 | 无法支持原材料库存 |
| B | 管理 Material（物料库存） | 与采购直接关联 | 与销售需要转换 |
| C | 管理 Canonical Item（统一库存） | 统一管理 | 需要重构 |

**推荐**：B — 管理 Material（物料库存）

**理由**：
1. 当前系统已经使用 material_id 管理库存
2. 库存管理与采购直接关联
3. 销售时通过 Recipe/BOM 关联到 Material

### 3.3 Recipe/BOM 管理对象定义

**问题**：Recipe/BOM 消耗的核心对象是什么？

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 消耗 Food（菜品作为原料） | 概念简单 | 无法支持原材料作为原料 |
| B | 消耗 Material（原材料作为原料） | 与库存直接关联 | 与销售需要转换 |
| C | 消耗 Canonical Item（统一原料） | 统一管理 | 需要重构 |

**推荐**：B — 消耗 Material（原材料作为原料）

**理由**：
1. 当前 dish_recipe 表已经使用 ingredient_id 指向 material_archives
2. 库存管理基于 Material
3. 成本计算基于 Material

### 3.4 门店差异化配置

**问题**：是否需要支持门店级别的差异化配置？

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 不支持门店差异化 | 简单 | 无法满足多门店需求 |
| B | 支持价格差异化 | 满足基本需求 | 配置复杂 |
| C | 支持全维度差异化 | 最灵活 | 设计复杂 |

**推荐**：B — 支持价格差异化

**理由**：
1. 当前业务主要需要价格差异化
2. 其他维度的差异化可以通过其他方式实现
3. 平衡灵活性和复杂度

---

## 4. 需要 Architecture Owner 决定的事项

### 4.1 Canonical Identity 设计

**问题**：Canonical Identity 的唯一键如何定义？

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 使用 UUID | 全局唯一 | 不可读，无法排序 |
| B | 使用自增 ID + 业务编码 | 可读，可排序 | 需要维护编码规则 |
| C | 使用组合键（门店+编码） | 支持门店隔离 | 复杂度高 |

**推荐**：B — 使用自增 ID + 业务编码

**理由**：
1. 可读性好，便于调试和排查问题
2. 可排序，便于分页查询
3. 业务编码可以包含业务含义

### 4.2 Operational Config 存储设计

**问题**：Operational Config 使用 JSON 还是扩展表？

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 使用 JSON | 灵活，扩展性强 | 查询性能差，无法建立索引 |
| B | 使用扩展表 | 查询性能好 | 需要维护表结构 |
| C | 混合方案 | 平衡灵活性和性能 | 设计复杂 |

**推荐**：C — 混合方案

**理由**：
1. 高频查询字段使用扩展表，保证性能
2. 低频配置字段使用 JSON，保证灵活性
3. 平衡性能和扩展性

### 4.3 跨域查询优化

**问题**：跨域查询如何优化性能？

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 使用视图 | 简单，无需重构 | 性能差 |
| B | 使用物化视图 | 性能好 | 需要同步 |
| C | 使用缓存 | 性能最好 | 一致性问题 |

**推荐**：B — 使用物化视图

**理由**：
1. 查询性能好
2. 可以定期同步，保证一致性
3. 支持复杂查询

### 4.4 数据迁移策略

**问题**：历史数据如何迁移？

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 一次性迁移 | 简单 | 风险高 |
| B | 分阶段迁移 | 风险低 | 复杂度高 |
| C | 双写期迁移 | 最安全 | 成本最高 |

**推荐**：B — 分阶段迁移

**理由**：
1. 风险可控
2. 可以逐步验证
3. 成本适中

### 4.5 迁移期间数据一致性

**问题**：如何保证迁移期间数据一致性？

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 最终一致性 | 简单 | 可能出现不一致 |
| B | 强一致性 | 保证一致 | 性能差 |
| C | 补偿机制 | 平衡 | 实现复杂 |

**推荐**：A — 最终一致性

**理由**：
1. 餐饮业务对实时一致性要求不高
2. 可以通过补偿机制修复不一致
3. 性能好，用户体验好

---

## 5. 决策依赖关系

### 5.1 上游依赖

```
LOCK-001 (事件驱动架构)
    │
    ├── DEC-006-REFINED (商品语义边界)
    │       │
    │       ├── DEC-004 (Customer/Member 边界)
    │       │
    │       └── DEC-001 (Warehouse 管理模式)
    │
    └── LOCK-005 (门店管理)
            │
            └── DEC-006-REFINED (商品语义边界)
```

### 5.2 下游影响

```
DEC-006-REFINED (商品语义边界)
    │
    ├── WP-002 (商品域实施)
    │       │
    │       └── BL-001 (商品 Identity 统一)
    │
    ├── SR-001 (统一 Identity)
    │
    ├── SR-002 (跨域数据一致性)
    │
    └── SR-005 (事件驱动集成)
```

### 5.3 依赖矩阵

| 依赖方 | 被依赖方 | 依赖类型 | 说明 |
|--------|----------|----------|------|
| DEC-006-REFINED | LOCK-001 | 强依赖 | 事件驱动架构约束 |
| DEC-006-REFINED | LOCK-005 | 强依赖 | 门店管理约束 |
| DEC-004 | DEC-006-REFINED | 强依赖 | 需要联合确认商品与客户关系 |
| DEC-001 | DEC-006-REFINED | 弱依赖 | 仓库管理影响库存设计 |
| WP-002 | DEC-006-REFINED | 强依赖 | 需要 DEC-006 确认后实施 |
| BL-001 | DEC-006-REFINED | 强依赖 | 需要 DEC-006 确认后实施 |

---

## 6. 决策时间线建议

### 6.1 Phase 1: 决策准备（2026-09-09 - 2026-09-20）

| 日期 | 任务 | 负责人 | 交付物 |
|------|------|--------|--------|
| 2026-09-10 | 收集业务需求 | PO | 需求文档 |
| 2026-09-12 | 技术可行性分析 | Arch | 技术方案 |
| 2026-09-15 | 风险评估 | PO + Arch | 风险矩阵 |
| 2026-09-18 | 选项分析 | PO + Arch | 选项评估 |
| 2026-09-20 | 初步推荐 | PO + Arch | 推荐方案 |

### 6.2 Phase 2: 决策评审（2026-09-20 - 2026-09-30）

| 日期 | 任务 | 负责人 | 交付物 |
|------|------|--------|--------|
| 2026-09-22 | 评审会议准备 | PO + Arch | 评审材料 |
| 2026-09-25 | 评审会议 | PO + Arch + Stakeholders | 会议纪要 |
| 2026-09-28 | 修订方案 | PO + Arch | 修订方案 |
| 2026-09-30 | 最终确认 | PO | 决策记录 |

### 6.3 Phase 3: 决策执行（2026-10-01 - 2026-10-15）

| 日期 | 任务 | 负责人 | 交付物 |
|------|------|--------|--------|
| 2026-10-01 | 实施计划制定 | Arch | 实施计划 |
| 2026-10-05 | 原型开发 | Dev | 原型系统 |
| 2026-10-10 | 验证测试 | QA | 测试报告 |
| 2026-10-15 | 决策确认 | PO | CONFIRMED DECISION |

---

## 7. 真实业务案例验证

### 7.1 案例1：可乐的多角色管理

**业务场景**：
- 可乐作为销售商品（Food）
- 可乐作为采购物料（Material）
- 可乐作为库存项（Stock）
- 可乐作为菜品原料（Recipe Ingredient）

**DEC-006-REFINED 验证**：
```sql
-- Canonical Identity
INSERT INTO canonical_identity (id, name, global_sku, category, brand)
VALUES (1, '可口可乐', 'COLA001', '饮料', '可口可乐');

-- Commercial Definition
INSERT INTO commercial_definition (canonical_id, saleable, purchasable, storable, sell_price, cost_price)
VALUES (1, true, true, true, 3.00, 2.50);

-- POS Operational Definition
INSERT INTO operational_definition (canonical_id, domain, config)
VALUES (1, 'pos', '{"menu_config": {...}}');

-- Procurement Operational Definition
INSERT INTO operational_definition (canonical_id, domain, config)
VALUES (1, 'procurement', '{"supplier_id": 501, "lead_time": 7}');

-- Inventory Operational Definition
INSERT INTO operational_definition (canonical_id, domain, config)
VALUES (1, 'inventory', '{"warehouse_id": 1001, "reorder_point": 50}');

-- Recipe Operational Definition
INSERT INTO operational_definition (canonical_id, domain, config)
VALUES (1, 'recipe', '{"quantity": 0.2, "unit": "罐"}');
```

**验证结果**：
- ✅ 概念清晰：Food 专注可销售，Material 专注可采购/可存储
- ✅ 身份一致：统一 Canonical Identity
- ✅ 跨域一致：通过 Canonical Identity 保证
- ✅ 扩展性强：新增角色只需新增记录

### 7.2 案例2：鸡翅的多角色管理

**业务场景**：
- 鸡翅作为销售菜品（Food）
- 鸡翅作为采购原料（Material）
- 鸡翅作为库存项（Stock）
- 鸡翅作为其他菜品原料（Recipe Ingredient）

**DEC-006-REFINED 验证**：
```sql
-- Canonical Identity
INSERT INTO canonical_identity (id, name, global_sku, category, brand)
VALUES (2, '鸡翅中', 'WING001', '禽肉', '供应商A');

-- Commercial Definition
INSERT INTO commercial_definition (canonical_id, saleable, purchasable, storable, sell_price, cost_price)
VALUES (2, true, true, true, 8.00, 6.00);

-- POS Operational Definition
INSERT INTO operational_definition (canonical_id, domain, config)
VALUES (2, 'pos', '{"menu_config": {...}}');

-- Procurement Operational Definition
INSERT INTO operational_definition (canonical_id, domain, config)
VALUES (2, 'procurement', '{"supplier_id": 502, "lead_time": 3}');

-- Inventory Operational Definition
INSERT INTO operational_definition (canonical_id, domain, config)
VALUES (2, 'inventory', '{"warehouse_id": 1001, "reorder_point": 20}');

-- Recipe Operational Definition
INSERT INTO operational_definition (canonical_id, domain, config)
VALUES (2, 'recipe', '{"quantity": 0.2, "unit": "kg"}');
```

**验证结果**：
- ✅ 概念清晰：Food 专注可销售，Material 专注可采购/可存储
- ✅ 身份一致：统一 Canonical Identity
- ✅ 跨域一致：通过 Canonical Identity 保证
- ✅ 扩展性强：新增角色只需新增记录

### 7.3 案例3：宫保鸡丁的多角色管理

**业务场景**：
- 宫保鸡丁作为销售菜品（Food）
- 宫保鸡丁不作为采购对象（Not Material）
- 宫保鸡丁不作为库存项（Not Stock）
- 宫保鸡丁不作为其他菜品原料（Not Recipe Ingredient）

**DEC-006-REFINED 验证**：
```sql
-- Canonical Identity
INSERT INTO canonical_identity (id, name, global_sku, category, brand)
VALUES (3, '宫保鸡丁', 'KPC001', '菜品', '自有品牌');

-- Commercial Definition
INSERT INTO commercial_definition (canonical_id, saleable, purchasable, storable, sell_price, cost_price)
VALUES (3, true, false, false, 28.00, 15.00);

-- POS Operational Definition
INSERT INTO operational_definition (canonical_id, domain, config)
VALUES (3, 'pos', '{"menu_config": {...}}');
```

**验证结果**：
- ✅ 概念清晰：Food 专注可销售，不支持的业务角色不创建
- ✅ 身份一致：统一 Canonical Identity
- ✅ 跨域一致：通过 Canonical Identity 保证
- ✅ 扩展性强：未来需要时可以新增角色

---

## 8. 决策记录

### 8.1 RECOMMENDATION（推荐方案）

| 字段 | 值 |
|------|-----|
| **决策ID** | DEC-006-REFINED |
| **状态** | RECOMMENDATION |
| **推荐方案** | Option B — Food 专注可销售，Material 专注可采购/可存储 |
| **推荐日期** | 2026-09-09 |
| **推荐理由** | 概念清晰，事件驱动友好，扩展性强 |

### 8.2 CONFIRMED DECISION（确认决策）

**注意**：DEC-006-REFINED 尚未确认，需要 Product Owner 和 Architecture Owner 联合确认。

| 字段 | 值 |
|------|-----|
| **决策ID** | DEC-006-REFINED |
| **状态** | NOT CONFIRMED |
| **确认日期** | 待定 |
| **确认人** | Product Owner + Architecture Owner |

---

## 9. 风险评估

### 9.1 高风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 迁移期间数据不一致 | 业务中断 | 分阶段迁移 + 双写期 |
| Canonical Identity 设计不当 | 全局影响 | 充分评审 + 原型验证 |
| 业务需求变更 | 方案失效 | 灵活设计 + 渐进式实施 |

### 9.2 中风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 团队学习曲线 | 开发效率短期下降 | 培训 + 文档 + 代码示例 |
| Operational Config 复杂度 | 配置错误 | 配置校验 + UI 工具 |
| 性能下降 | 查询变慢 | 索引优化 + 读写分离 |

### 9.3 低风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 过度设计 | 资源浪费 | 最小可用原则 |
| 历史数据兼容性 | 数据丢失 | 保留历史数据 + 适配器 |

---

## 10. 成功标准

### 10.1 业务标准

1. **概念清晰度**：Food、Material、Stock、Recipe 的边界清晰定义
2. **数据一致性**：跨域数据一致性达到 99.9%
3. **业务覆盖度**：支持所有当前和预期的业务场景
4. **扩展性**：新增业务角色只需新增记录，无需重构

### 10.2 技术标准

1. **查询性能**：跨域查询时间减少 50%
2. **开发效率**：新功能开发时间减少 30%
3. **维护成本**：维护成本降低 40%
4. **测试覆盖率**：单元测试覆盖率 > 80%

---

**状态**：只读分析任务，不修改任何代码  
**日期**：2026-09-09  
**版本**：1.0
