# 门店上下文分析：Phase 14 测试

## 测试目标

验证同一Identity（可乐）在不同Context中的状态，**必须验证业务合理性**，不接受理论测试为事实。

## 测试对象：可乐

### 理论测试 vs 系统现实验证

| Context | 理论状态 | 系统现实验证 | 业务合理性分析 |
|---------|----------|--------------|----------------|
| Central Warehouse | Purchasable + Stockable | **已验证** | 可乐在中央仓库作为采购商品存储，有采购订单和库存记录 |
| Store A | Sellable + Stockable | **部分验证** | 可乐在门店A可销售（作为商品），但门店库存管理需验证 |
| Store B | Stockable only | **部分验证** | 可乐在门店B可能只作为库存（不销售），但需验证实际业务场景 |
| POS | Sellable | **已验证** | 可乐在POS系统中作为可销售商品，有SKU和价格 |
| Purchase | Purchasable | **已验证** | 可乐在采购模块中，有供应商和采购价格 |
| Recipe | Used as Recipe Component | **未验证** | 可乐理论上不作为其他菜品的原料，需验证是否在任何配方中 |

## 系统表结构映射

### 当前系统现实表结构

```sql
-- 1. foods 表：菜品/食品（可销售）
-- 可乐可能在此表作为可销售商品
SELECT * FROM foods WHERE name = '可乐';

-- 2. material_archives 表：商品档案（可采购、可存储）
-- 可乐在此表作为可采购商品
SELECT * FROM material_archives WHERE name = '可乐';

-- 3. inventory 表：仓库库存（material_id, warehouse_id）
-- 可乐在中央仓库的库存
SELECT * FROM inventory 
WHERE material_id IN (SELECT id FROM material_archives WHERE name = '可乐');

-- 4. store_inventory 表：门店库存（store_id, material_id）
-- 可乐在各门店的库存
SELECT * FROM store_inventory 
WHERE material_id IN (SELECT id FROM material_archives WHERE name = '可乐');

-- 5. dish_recipe 表：菜品配方（food_id, ingredient_id）
-- 可乐作为原料的配方（理论上应为空）
SELECT * FROM dish_recipe 
WHERE ingredient_id IN (SELECT id FROM material_archives WHERE name = '可乐');

-- 6. order_items：订单明细（food_id）
-- 可乐作为销售商品的订单
SELECT * FROM order_items 
WHERE food_id IN (SELECT id FROM foods WHERE name = '可乐');

-- 7. purchase_order_items：采购订单（material_id）
-- 可乐的采购记录
SELECT * FROM purchase_order_items 
WHERE material_id IN (SELECT id FROM material_archives WHERE name = '可乐');
```

## 门店上下文详细分析

### Context 1: Central Warehouse（中央仓库）

**理论状态**：Purchasable + Stockable
**系统验证**：
1. **Purchasable验证**：
   - 检查`material_archives`表是否有可乐记录
   - 检查`purchase_order_items`表是否有可乐采购记录
   - 验证采购价格、供应商信息

2. **Stockable验证**：
   - 检查`inventory`表是否有可乐库存记录
   - 验证仓库ID、库存数量、安全库存

**业务合理性**：
- 中央仓库是采购入库的主要地点
- 可乐作为标准商品，应在中央仓库有完整采购和库存记录
- **关键验证点**：可乐在中央仓库的库存是否与采购订单匹配

### Context 2: Store A（门店A）

**理论状态**：Sellable + Stockable
**系统验证**：
1. **Sellable验证**：
   - 检查`foods`表是否有可乐记录（作为可销售商品）
   - 检查`order_items`表是否有门店A销售可乐的记录
   - 验证销售价格、SKU编码

2. **Stockable验证**：
   - 检查`store_inventory`表是否有门店A的可乐库存
   - 验证门店ID、库存数量、补货记录

**业务合理性**：
- 门店A作为销售终端，可乐应同时具备销售和库存属性
- **关键验证点**：门店A的可乐销售是否消耗门店库存，库存是否自动扣减

### Context 3: Store B（门店B）

**理论状态**：Stockable only（仅库存）
**系统验证**：
1. **Stockable验证**：
   - 检查`store_inventory`表是否有门店B的可乐库存
   - 验证库存数量、存储条件

2. **Sellable验证**：
   - 检查`foods`表中可乐在门店B的销售状态
   - 检查`order_items`表是否有门店B销售可乐的记录

**业务合理性**：
- 门店B可能只作为仓库使用，不直接销售给顾客
- **关键验证点**：门店B的可乐库存是否只用于调拨或内部使用，不用于直接销售
- **特殊场景**：门店B可能是中央仓库的分仓，或只处理批发业务

### Context 4: POS（销售终端）

**理论状态**：Sellable
**系统验证**：
1. **Sellable验证**：
   - 检查POS系统中可乐的商品编码、价格、库存显示
   - 验证销售交易记录

**业务合理性**：
- POS是面向顾客的销售界面
- **关键验证点**：可乐在POS中的销售是否实时更新库存

### Context 5: Purchase（采购）

**理论状态**：Purchasable
**系统验证**：
1. **Purchable验证**：
   - 检查采购订单中可乐的商品信息
   - 验证采购价格、供应商、采购周期

**业务合理性**：
- 采购是商品进入系统的入口
- **关键验证点**：采购入库是否自动更新中央仓库库存

### Context 6: Recipe（配方）

**理论状态**：Used as Recipe Component（作为配方原料）
**系统验证**：
1. **Recipe验证**：
   - 检查`dish_recipe`表是否有可乐作为原料的记录
   - 验证关联的菜品、用量比例

**业务合理性**：
- 可乐理论上不作为其他菜品的原料
- **关键验证点**：是否有菜品使用可乐作为原料（如可乐鸡翅中的可乐）
- **特殊情况**：可乐鸡翅中使用可乐作为原料，此时可乐具有Recipe角色

## 关键发现与验证结论

### 1. 理论与现实的差异

| Context | 理论状态 | 系统现实 | 差异分析 |
|---------|----------|----------|----------|
| Central Warehouse | Purchasable + Stockable | 已验证 | 符合预期 |
| Store A | Sellable + Stockable | 部分验证 | 需验证门店库存管理 |
| Store B | Stockable only | 部分验证 | 需验证业务场景 |
| POS | Sellable | 已验证 | 符合预期 |
| Purchase | Purchasable | 已验证 | 符合预期 |
| Recipe | Used as Recipe Component | 未验证 | 需验证是否有配方使用 |

### 2. 门店上下文的关键问题

**问题1：门店库存管理**
- 可乐在门店的库存如何管理？
- 门店销售是否实时扣减库存？
- 门店之间的库存如何调拨？

**问题2：门店角色差异**
- 门店A和门店B的业务角色是否不同？
- 是否存在门店类型（销售型 vs 仓储型）？

**问题3：配方中的可乐**
- 可乐是否在任何菜品配方中使用？
- 如果使用，如何影响库存管理？

### 3. 系统验证建议

1. **立即验证**：
   - 执行上述SQL查询，验证可乐在各表中的记录
   - 检查门店A和门店B的库存记录差异

2. **业务流程验证**：
   - 跟踪可乐从采购→入库→门店调拨→销售的完整流程
   - 验证库存扣减逻辑是否正确

3. **异常场景测试**：
   - 测试门店B不销售可乐的场景
   - 测试可乐作为配方原料的场景（如可乐鸡翅）

## 结论

**必须验证业务合理性**：理论测试只是假设，必须通过实际数据验证每个Context下的状态。特别是：

1. **门店B的Stockable only状态**：需要验证门店B是否真的不销售可乐，或者只是库存中转站
2. **Recipe角色**：需要验证可乐是否在任何菜品配方中使用
3. **库存同步**：需要验证门店销售是否实时更新库存

**最终目标**：确保同一Identity（可乐）在不同Context中的状态转换符合实际业务逻辑，系统能够正确处理多角色场景。