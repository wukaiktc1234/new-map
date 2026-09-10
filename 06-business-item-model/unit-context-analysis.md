# Phase 15: Unit/UOM 上下文分析

## 1. 业务背景

餐饮ERP系统中，单位（UOM）贯穿于采购、库存、生产、销售全链路。单位的管理质量直接影响：
- 成本核算准确性
- 库存管理可靠性
- 配方标准化程度

---

## 2. 单位分层模型

### 2.1 五层单位体系

```
┌─────────────────────────────────────────────────────────────┐
│                    Sales UOM（销售单位）                      │
│                    菜品出品、销售计价                          │
├─────────────────────────────────────────────────────────────┤
│                    Recipe UOM（配方单位）                      │
│                    标准菜谱配方计量                           │
├─────────────────────────────────────────────────────────────┤
│                    Stock UOM（库存单位）                       │
│                    仓库收发、库存盘点                         │
├─────────────────────────────────────────────────────────────┤
│                    Purchase UOM（采购单位）                    │
│                    供应商报价、采购下单                       │
├─────────────────────────────────────────────────────────────┤
│                    Base UOM（基础单位）                        │
│                    物理属性基准（1ml / 1g / 1个）              │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 五层定义

| 层级 | 名称 | 用途 | 粒度 |
|------|------|------|------|
| L1 | Base UOM | 物理属性基准单位 | 最细 |
| L2 | Purchase UOM | 采购报价、订单 | 粗 |
| L3 | Stock UOM | 库存收发、盘点 | 中 |
| L4 | Recipe UOM | 配方标准计量 | 细 |
| L5 | Sales UOM | 菜品出品、销售 | 灵活 |

---

## 3. 真实业务案例

### 案例1：啤酒采购

**业务场景**：采购啤酒用于门店销售

| 层级 | 单位 | 数量 | 说明 |
|------|------|------|------|
| Purchase | 箱 | 10箱 | 供应商按箱报价，$50/箱 |
| Stock | 瓶 | 240瓶 | 入库按瓶登记（1箱=24瓶） |
| Recipe | ml | 不适用 | 啤酒非配方原料 |
| Sales | 瓶 | 240瓶 | 门店按瓶销售，$8/瓶 |

**转换关系**：1箱 = 24瓶，1瓶 = 500ml

**单位分层归属**：转换发生在 Purchase ↔ Stock 层

---

### 案例2：食用油采购

**业务场景**：采购大豆油用于厨房烹饪

| 层级 | 单位 | 数量 | 说明 |
|------|------|------|------|
| Purchase | 桶 | 5桶 | 供应商报价，$80/桶 |
| Stock | L | 100L | 入库按升登记（1桶=20L） |
| Recipe | ml | 50ml | 配方标准：炒青菜用50ml油 |
| Sales | 份 | 不适用 | 油作为成本分摊到菜品 |

**转换关系**：1桶 = 20L，1L = 1000ml

**单位分层归属**：转换跨越 Purchase → Stock → Recipe 三层

---

### 案例3：面粉采购

**业务场景**：采购面粉用于面点制作

| 层级 | 单位 | 数量 | 说明 |
|------|------|------|------|
| Purchase | 袋 | 20袋 | 供应商报价，$45/袋 |
| Stock | kg | 500kg | 入库按公斤登记（1袋=25kg） |
| Recipe | g | 500g | 配方标准：馒头配方用500g面粉 |
| Sales | 份 | 不适用 | 面粉作为成本分摊到菜品 |

**转换关系**：1袋 = 25kg，1kg = 1000g

**单位分层归属**：转换跨越 Purchase → Stock → Recipe 三层

---

### 案例4：瓶装饮料

**业务场景**：采购瓶装可乐用于门店销售

| 层级 | 单位 | 数量 | 说明 |
|------|------|------|------|
| Purchase | 箱 | 20箱 | 供应商报价，$30/箱 |
| Stock | 瓶 | 480瓶 | 入库按瓶登记（1箱=24瓶） |
| Recipe | 不适用 | 不适用 | 饮料非配方原料 |
| Sales | 瓶 | 480瓶 | 门店按瓶销售，$5/瓶 |

**单位分层归属**：转换发生在 Purchase ↔ Stock 层

---

### 案例5：复合调料包

**业务场景**：采购调料包用于标准化出品

| 层级 | 单位 | 数量 | 说明 |
|------|------|------|------|
| Purchase | 箱 | 5箱 | 供应商报价，$120/箱 |
| Stock | 包 | 500包 | 入库按包登记（1箱=100包） |
| Recipe | 包 | 2包 | 配方标准：火锅底料用2包 |
| Sales | 份 | 不适用 | 调料包作为成本分摊到菜品 |

**单位分层归属**：转换发生在 Purchase ↔ Stock 层，Recipe 直接使用 Stock 单位

---

## 4. 转换关系分析

### 4.1 转换发生的位置

| 转换类型 | 转换位置 | 业务场景 | 频率 |
|----------|----------|----------|------|
| Purchase → Stock | 入库环节 | 采购收货时转换 | 高 |
| Stock → Recipe | 配方计算 | 标准菜谱配方 | 中 |
| Recipe → Sales | 成本核算 | 成本分摊到菜品 | 中 |

### 4.2 转换规则示例

```yaml
# 采购单位 → 库存单位转换
conversion_rules:
  - item: "啤酒"
    from: { unit: "箱", qty: 1 }
    to: { unit: "瓶", qty: 24 }
    
  - item: "食用油"
    from: { unit: "桶", qty: 1 }
    to: { unit: "L", qty: 20 }
    
  - item: "面粉"
    from: { unit: "袋", qty: 1 }
    to: { unit: "kg", qty: 25 }

# 库存单位 → 配方单位转换
recipe_conversions:
  - item: "食用油"
    from: { unit: "L", qty: 1 }
    to: { unit: "ml", qty: 1000 }
    
  - item: "面粉"
    from: { unit: "kg", qty: 1 }
    to: { unit: "g", qty: 1000 }
```

---

## 5. 当前系统现实

### 5.1 现有表结构

| 表名 | 字段 | 类型 | 问题 |
|------|------|------|------|
| inventory_unit | type | ENUM | 仅 weight/volume/count 三种类型 |
| material_archives | unit | VARCHAR(50) | 自由文本，无标准化 |
| dish_recipe | unit | VARCHAR(20) | 自由文本，无标准化 |
| inventory | unit | VARCHAR(20) | 自由文本，无标准化 |
| store_inventory | unit | VARCHAR(32) | 自由文本，无标准化 |

### 5.2 数据一致性问题

**问题1**：同一物料在不同表中单位不一致

```sql
-- material_archives 中记录为 "kg"
SELECT unit FROM material_archives WHERE id = 1;
-- 返回: "kg"

-- inventory 中记录为 "公斤"
SELECT unit FROM inventory WHERE material_id = 1;
-- 返回: "公斤"

-- store_inventory 中记录为 "KG"
SELECT unit FROM store_inventory WHERE material_id = 1;
-- 返回: "KG"
```

**问题2**：无转换关系定义

```sql
-- 无法回答：1箱啤酒 = ?瓶
-- 无法回答：1桶油 = ?升
```

**问题3**：配方单位与库存单位脱节

```sql
-- dish_recipe 中记录为 "ml"
SELECT unit FROM dish_recipe WHERE ingredient_id = 1;
-- 返回: "ml"

-- inventory 中记录为 "L"
SELECT unit FROM inventory WHERE material_id = 1;
-- 返回: "L"

-- 无转换关系：50ml油 = ?L（需要0.05L）
```

---

## 6. Unit 是否必须成为独立 Foundation？

### 6.1 分析框架

| 维度 | 独立 Unit Foundation | 隶属 Item/Profile | 隶属 Transaction |
|------|---------------------|-------------------|------------------|
| 复用性 | ✅ 跨层复用 | ⚠️ 跨层困难 | ❌ 仅单次 |
| 一致性 | ✅ 全局统一 | ⚠️ 可能不一致 | ❌ 无需一致 |
| 复杂度 | ⚠️ 需独立维护 | ✅ 降低复杂度 | ✅ 最简单 |
| 扩展性 | ✅ 支持未来 | ⚠️ 受限 | ❌ 不支持 |

### 6.2 业务验证

**验证1**：同一单位在多层使用

```
"瓶" 这个单位：
- Stock 层：库存以瓶计量
- Sales 层：销售以瓶计价
- Purchase 层：有时也以瓶采购（如单瓶采购）

结论：单位必须跨层复用
```

**验证2**：转换关系需要统一管理

```
"1箱 = 24瓶" 这个转换：
- 入库时需要转换
- 盘点时需要转换
- 成本核算时需要转换

结论：转换关系必须集中管理
```

**验证3**：单位类型需要标准化

```
当前 system 仅有 weight/volume/count：
- 需要支持 "箱"、"包"、"袋" 等复合单位
- 需要支持 "份"、"个" 等计量单位
- 需要支持 "ml"、"g" 等基础单位

结论：单位类型需要扩展
```

### 6.3 结论

**Unit 必须成为独立 Foundation**

理由：
1. **跨层复用**：同一单位在 Purchase/Stock/Recipe/Sales 多层使用
2. **集中管理**：转换关系需要统一维护
3. **数据一致性**：避免不同表中单位表示不一致
4. **扩展性**：支持未来新增单位类型

---

## 7. 建议的 Unit Foundation 结构

### 7.1 核心表设计

```sql
-- 单位主表
CREATE TABLE uom_master (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,      -- 单位编码：BOTTLE, KG, ML
    name VARCHAR(50) NOT NULL,             -- 单位名称：瓶、公斤、毫升
    category ENUM('weight', 'volume', 'count', 'compound') NOT NULL,
    base_unit_id BIGINT,                   -- 基础单位引用
    conversion_factor DECIMAL(10,4),       -- 到基础单位的转换系数
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 单位转换规则表
CREATE TABLE uom_conversion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,               -- 物料ID
    from_uom VARCHAR(20) NOT NULL,         -- 源单位
    to_uom VARCHAR(20) NOT NULL,           -- 目标单位
    factor DECIMAL(10,4) NOT NULL,         -- 转换系数
    scene ENUM('purchase_stock', 'stock_recipe', 'recipe_sales') NOT NULL,
    effective_date DATE,                   -- 生效日期
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 物料单位关联表
CREATE TABLE item_uom (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    uom_type ENUM('purchase', 'stock', 'recipe', 'sales') NOT NULL,
    uom_code VARCHAR(20) NOT NULL,
    is_primary TINYINT DEFAULT 0,          -- 是否主单位
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 7.2 初始数据

```sql
-- 基础单位
INSERT INTO uom_master (code, name, category, base_unit_id, conversion_factor) VALUES
('G', '克', 'weight', NULL, 1),
('KG', '公斤', 'weight', 1, 1000),
('ML', '毫升', 'volume', NULL, 1),
('L', '升', 'volume', 4, 1000),
('EA', '个', 'count', NULL, 1);

-- 复合单位
INSERT INTO uom_master (code, name, category, base_unit_id, conversion_factor) VALUES
('BOTTLE', '瓶', 'compound', 4, 500),       -- 1瓶 = 500ml
('BOX', '箱', 'compound', 6, 24),           -- 1箱 = 24瓶
('BAG', '袋', 'compound', 2, 25),           -- 1袋 = 25kg
('BUCKET', '桶', 'compound', 5, 20);        -- 1桶 = 20L

-- 单位转换规则
INSERT INTO uom_conversion (item_id, from_uom, to_uom, factor, scene) VALUES
(1, 'BOX', 'BOTTLE', 24, 'purchase_stock'),
(1, 'BOTTLE', 'ML', 500, 'stock_recipe'),
(2, 'BUCKET', 'L', 20, 'purchase_stock'),
(2, 'L', 'ML', 1000, 'stock_recipe'),
(3, 'BAG', 'KG', 25, 'purchase_stock'),
(3, 'KG', 'G', 1000, 'stock_recipe');
```

---

## 8. 迁移策略

### 8.1 渐进式迁移

```mermaid
graph LR
    A[阶段1: 建立Unit Foundation] --> B[阶段2: 统一单位表示]
    B --> C[阶段3: 补充转换关系]
    C --> D[阶段4: 业务逻辑适配]
```

### 8.2 数据清洗规则

```yaml
data_cleaning_rules:
  # 标准化单位表示
  standardize_unit:
    - pattern: "公斤|KG|kg|千克"
      replace_with: "KG"
    - pattern: "克|g|G"
      replace_with: "G"
    - pattern: "升|L|l|公升"
      replace_with: "L"
    - pattern: "毫升|ml|ML"
      replace_with: "ML"
    
  # 补充缺失的转换关系
  add_conversion:
    - item: "啤酒"
      conversions:
        - from: "箱", to: "瓶", factor: 24
        - from: "瓶", to: "ML", factor: 500
```

---

## 9. 总结

### 9.1 核心结论

1. **Unit 必须独立**：跨层复用和转换管理要求 Unit 成为独立 Foundation
2. **转换关系集中管理**：避免业务逻辑分散
3. **渐进式迁移**：分阶段实施，降低风险

### 9.2 关键数据

| 维度 | 结论 |
|------|------|
| 单位分层 | 5层：Base → Purchase → Stock → Recipe → Sales |
| 转换位置 | 主要在 Purchase→Stock 和 Stock→Recipe |
| 独立性 | Unit 必须成为独立 Foundation |
| 迁移策略 | 4阶段渐进式迁移 |
