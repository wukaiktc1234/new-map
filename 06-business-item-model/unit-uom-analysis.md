# Unit / UOM（计量单位）分析

> **文档类型**: 单位/UOM 架构分析  
> **生成日期**: 2026-09-09  
> **状态**: ANALYSIS  
> **范围**: 计量单位管理、单位转换、跨域单位一致性  
> **关联决策**: DEC-002 (Unit 对象归属), PADR-002 (门店经营范围)

---

## 一、现状事实（代码审计）

### 1.1 inventory_unit 表（独立单位表，已存在）

| 字段 | 类型 | 语义 |
|------|------|------|
| id | BIGINT(PK,AUTO) | 单位ID |
| name | VARCHAR | 单位名称（如"千克"、"升"） |
| code | VARCHAR | 单位编码（如"kg"、"L"） |
| type | VARCHAR | 单位类型：weight / volume / count |
| description | VARCHAR | 描述 |
| status | BOOLEAN | 状态（1启用/0禁用） |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

**状态**: 已存在但**未被引用**。无任何 Java Entity、Service、Controller 引用此表。
**结论**: 这是一个孤立的 Foundation 表，尚未接入业务链路。

---

### 1.2 各业务表中的 unit 字段

| 业务表 | 字段 | 类型 | 默认值 | 语义 | 引用 inventory_unit? |
|--------|------|------|--------|------|---------------------|
| `material_archives` | unit | VARCHAR(50) | 无 | 物料采购/库存单位 | **否** |
| `foods` | unit | VARCHAR(20) | '份' | 菜品销售单位 | **否** |
| `dish_recipe` / `dish_recipe_new` | unit | VARCHAR(20) | 无 | 配方用量单位 | **否** |
| `inventory` | unit | VARCHAR(20) | 隐含 | 仓库库存单位 | **否** |
| `store_inventory` | unit | VARCHAR(32) | 隐含 | 门店库存单位 | **否** |
| `purchase_order_items` | unit | VARCHAR | 隐含 | 采购单位（隐含在 material.unit 中） | **否** |
| `order_items` | unit | VARCHAR | 隐含 | 销售单位（隐含在 food.unit 中） | **否** |
| `combo_ingredient` / `combo_ingredient_new` | unit | VARCHAR | 无 | 套餐配料单位 | **否** |
| `dish_inventory` | unit | VARCHAR | 无 | 菜品-库存关联单位 | **否** |
| `combo_inventory` | unit | VARCHAR | 无 | 套餐-库存关联单位 | **否** |
| `asset_master` | unit | VARCHAR | 无 | 资产计量单位 | **否** |
| `electronic_invoice_item` | unit | VARCHAR | 无 | 发票行单位 | **否** |

**关键发现**: `inventory_unit` 表虽然存在，但所有业务表中的 unit 字段都是**自由文本 VARCHAR**，未引用 `inventory_unit` 表。

---

### 1.3 单位使用示例（同一物料的多单位场景）

以"可口可乐"为例：

| 业务场景 | 单位 | 字段来源 | 说明 |
|----------|------|----------|------|
| 采购 | 箱 | purchase_order_items (隐含 material.unit) | 10 箱 × 24瓶/箱 |
| 仓库存储 | 瓶 | inventory.unit | 240 瓶 |
| 门店库存 | 瓶 | store_inventory.unit | 12 瓶 |
| 配方用量 | ml | dish_recipe.unit | 330ml/份 |
| 销售 | 杯 | foods.unit | 1 杯（330ml） |

**同一物理实体 "可乐" 在不同业务场景中使用了 4 种不同的单位，且系统无法自动转换。**

---

### 1.4 现有单位值分布

从代码和枚举中提取的典型单位值：

| 类别 | 单位值 | 使用场景 |
|------|--------|----------|
| **重量** | 斤、kg、g、吨 | 物料采购、库存 |
| **体积** | L、ml、升、毫升 | 配方用量、饮品 |
| **数量** | 个、份、瓶、箱、袋、盒、包 | 菜品销售、包装材料 |
| **长度** | m、cm | 少见 |

**问题**: 同一类别内的单位（如"斤"和"kg"）没有换算关系。

---

## 二、核心问题分析

### 2.1 Unit 应该是共享 Foundation，还是对象内部配置？

#### 选项 A：共享 Foundation（独立 units 表，所有业务表引用）

```
┌──────────────────────────────────────────────────────────┐
│                    units (Foundation)                     │
│  id | code | name | category | baseUnitId | convRate     │
│  1  | kg   | 千克 | WEIGHT   | 1          | 1.0          │
│  2  | g    | 克   | WEIGHT   | 1          | 0.001        │
│  3  | jin  | 斤   | WEIGHT   | 1          | 0.5          │
│  4  | L    | 升   | VOLUME   | 4          | 1.0          │
│  5  | ml   | 毫升 | VOLUME   | 4          | 0.001        │
│  6  | piece| 个   | COUNT    | NULL       | NULL         │
│  7  | box  | 箱   | COUNT    | 6          | 24           │
│  8  | bottle| 瓶  | COUNT    | 6          | 1            │
└──────────────────────────────────────────────────────────┘
     │
     ├── material_archives.unit_id → units.id
     ├── foods.unit_id → units.id
     ├── dish_recipe.unit_id → units.id
     ├── store_inventory.unit_id → units.id
     └── inventory.unit_id → units.id
```

**优点**:
- 单位定义唯一，消除"同物不同单位"的歧义
- 支持自动换算（baseUnitId + conversionRate）
- Foundation 层统一管理，符合系统架构原则

**缺点**:
- 需要修改所有业务表，迁移成本高
- 查询需要 JOIN units 表
- 部分场景的单位换算可能不精确（如"1箱可乐=24瓶"，但不同品牌箱规不同）

#### 选项 B：对象内部配置（各业务表保留 VARCHAR，inventory_unit 仅作为参考）

```
material_archives.unit = '箱'   (VARCHAR 自由文本)
foods.unit = '瓶'              (VARCHAR 自由文本)
inventory_unit 表: 仅用于 UI 下拉选择和校验
```

**优点**:
- 改动最小，保持现状
- 查询无需 JOIN

**缺点**:
- 单位定义分散，无法保证一致性
- 无法支持自动换算
- "箱"和"box"可能是同一单位的不同写法

#### 选项 C：混合模式（inventory_unit 作为字典 + 业务表保留 VARCHAR）

```
inventory_unit 表: 单位字典（UI 选择 + 校验）
业务表.unit: VARCHAR（存储 unit code，非 unit id）
应用层: 读取时通过 code 关联 inventory_unit 做换算
```

**优点**:
- 兼顾灵活性和一致性
- 业务表无需改结构（VARCHAR → VARCHAR(code)）
- 换算逻辑在应用层，可按需启用

**缺点**:
- 仍依赖字符串匹配（code 一致性）
- 换算逻辑需要应用层实现

---

### 2.2 Base Unit、Purchase UOM、Stock UOM、Recipe UOM、Sales UOM 的关系

#### 同一物料的多单位模型

```
                    ┌─────────────────────┐
                    │   Canonical Item    │
                    │   (可口可乐 330ml)   │
                    └─────────┬───────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
    ┌─────┴─────┐      ┌─────┴─────┐      ┌─────┴─────┐
    │ Purchase  │      │  Stock    │      │  Sales    │
    │ UOM: 箱   │      │  UOM: 瓶  │      │  UOM: 杯  │
    │ 24瓶/箱   │      │           │      │ 330ml/杯  │
    └───────────┘      └───────────┘      └───────────┘
          │                   │                   │
    purchase_order_items  store_inventory     order_items
    unit: '箱'           unit: '瓶'          unit: '杯'
```

#### 五种 UOM 角色定义

| UOM 角色 | 定义 | 存储位置 | 是否需要独立管理 |
|----------|------|----------|-----------------|
| **Base UOM** | 物理基础单位（如"瓶"） | inventory_unit (作为参考) | 是 — 换算基准 |
| **Purchase UOM** | 采购订单中的单位（如"箱"） | material_archives.unit 或 purchase_order_items.unit | 是 — 采购量 ≠ 库存量 |
| **Stock UOM** | 库存管理中的单位（如"瓶"） | inventory.unit / store_inventory.unit | 是 — 库存计量基准 |
| **Recipe UOM** | 配方用量单位（如"ml"） | dish_recipe.unit | 是 — 精确配料 |
| **Sales UOM** | 销售给客户的单位（如"杯"） | foods.unit | 是 — POS 收银 |

#### 关键问题：是否需要分别管理？

**结论：是，但需要分层实现。**

| 层次 | 管理方式 | 理由 |
|------|----------|------|
| **Foundation 层** | inventory_unit 表定义所有可用单位 | 统一字典 |
| **Object 层** | 每个业务表保留自己的 unit 字段 | 不同角色需要不同单位 |
| **Conversion 层** | 新增 unit_conversion 表或应用层换算 | 支持跨角色单位转换 |

---

### 2.3 Conversion（单位转换）应该如何实现？

#### 选项 1：静态转换表（推荐短期方案）

```sql
CREATE TABLE unit_conversion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_unit VARCHAR(20) NOT NULL,     -- 源单位 code
    to_unit VARCHAR(20) NOT NULL,       -- 目标单位 code
    conversion_rate DECIMAL(10,6) NOT NULL,  -- 转换率: 1 from = rate to
    unit_category VARCHAR(20) NOT NULL, -- WEIGHT/VOLUME/COUNT
    status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(from_unit, to_unit)
);

-- 示例数据
INSERT INTO unit_conversion VALUES
(1, '箱', '瓶', 24, 'COUNT', 1, NOW()),
(2, '瓶', 'ml', 330, 'VOLUME', 1, NOW()),
(3, '斤', 'kg', 0.5, 'WEIGHT', 1, NOW()),
(4, 'kg', 'g', 1000, 'WEIGHT', 1, NOW()),
(5, 'L', 'ml', 1000, 'VOLUME', 1, NOW());
```

**优点**:
- 简单直接，易于维护
- 支持单向和双向转换
- 可审计转换历史

**缺点**:
- 需要人工维护转换率
- 跨类别转换（如体积↔重量）需要密度参数

#### 选项 2：Base Unit + Conversion Rate（推荐中期方案）

```sql
-- 在 units 表中内嵌转换关系
ALTER TABLE inventory_unit ADD COLUMN base_unit_id BIGINT;
ALTER TABLE inventory_unit ADD COLUMN conversion_rate DECIMAL(10,6);

-- 1箱 → 24瓶（base_unit_id = 瓶的 id, conversion_rate = 24）
-- 1瓶 → 330ml（base_unit_id = ml 的 id, conversion_rate = 330）
-- 1斤 → 0.5kg（base_unit_id = kg 的 id, conversion_rate = 0.5）
```

**优点**:
- 转换关系内嵌，查询高效
- 支持链式转换（箱→瓶→ml）

**缺点**:
- 单向转换（需反向查询）
- 跨类别仍需额外处理

#### 选项 3：应用层换算（推荐短期 + 最小改动）

```java
public class UnitConverter {
    // 转换率硬编码或从配置读取
    private static final Map<String, Map<String, Double>> CONVERSIONS = Map.of(
        "箱→瓶", Map.of("箱", 1.0, "瓶", 24.0),
        "斤→kg", Map.of("斤", 1.0, "kg", 0.5),
        "L→ml", Map.of("L", 1.0, "ml", 1000.0)
    );
    
    public static double convert(double value, String from, String to) {
        // 查找转换率并计算
    }
}
```

**优点**:
- 零数据库改动
- 快速实现

**缺点**:
- 硬编码，不可维护
- 新增单位需改代码

---

### 2.4 推荐的 Conversion 架构

```
┌─────────────────────────────────────────────────────────┐
│                    单位转换架构                           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐    ┌──────────────┐    ┌────────────┐ │
│  │inventory_   │    │unit_         │    │业务表      │ │
│  │unit (字典)  │◄───│conversion    │◄───│.unit (VARCHAR)│
│  │             │    │(转换关系)    │    │            │ │
│  │ code: 'kg'  │    │ from: '箱'   │    │ material:  │ │
│  │ code: '瓶'  │    │ to: '瓶'     │    │   '箱'     │ │
│  │ code: '箱'  │    │ rate: 24     │    │ inventory: │ │
│  └─────────────┘    └──────────────┘    │   '瓶'     │ │
│                                         └────────────┘ │
│                                                         │
│  应用层换算流程:                                         │
│  1. 读取业务表.unit → 得到 unit code                      │
│  2. 查询 unit_conversion → 得到转换率                    │
│  3. 执行转换 → 得到目标单位数量                           │
└─────────────────────────────────────────────────────────┘
```

---

## 三、跨域单位问题深度分析

### 3.1 采购→库存 单位转换

```
采购收货流程:
  purchase_order_items.unit = '箱'  (采购量: 10箱)
      ↓ 收货确认
  库存入库: inventory.unit = '瓶'  (入库量: 10×24=240瓶)
  
  问题: 当前系统无自动换算，收货时需手动输入转换后的数量
```

**建议**: 在收货确认界面增加"采购单位→库存单位"的自动换算。

### 3.2 库存→配方 单位转换

```
配方管理流程:
  material_archives.unit = '箱'  (物料档案单位)
      ↓
  dish_recipe.unit = 'ml'  (配方用量: 330ml/份)
  
  问题: 配方中 "330ml" 需要知道 1瓶=330ml，1箱=24瓶
```

**建议**: 配方管理时，自动从 unit_conversion 表获取换算率。

### 3.3 库存→销售 单位转换

```
销售扣减流程:
  foods.unit = '杯'  (销售单位)
      ↓ BOM 扣减
  store_inventory.unit = '瓶'  (库存单位)
  
  问题: 1杯可乐 = 1瓶？还是 1杯 = 330ml ≈ 1瓶？
```

**建议**: 在 foods 表增加 `stock_unit_conversion_rate` 字段，记录"1销售单位 = N库存单位"。

### 3.4 仓库库存→门店库存 单位转换

```
调拨流程:
  inventory.unit = '箱'  (仓库以箱为单位)
      ↓ 调拨到门店
  store_inventory.unit = '瓶'  (门店以瓶为单位)
  
  问题: 调拨时需要自动换算
```

**建议**: 调拨单支持"源单位→目标单位"的自动换算。

---

## 四、决策建议

### 4.1 短期方案（0-3 个月）

| 动作 | 说明 | 优先级 |
|------|------|--------|
| 激活 inventory_unit 表 | 创建 Entity/Service，允许 UI 选择单位 | P1 |
| 创建 unit_conversion 表 | 定义常用单位换算关系 | P1 |
| 统一 unit 字段语义 | 确保所有业务表.unit 存储的是 unit.code（不是自由文本） | P1 |
| 应用层换算服务 | 提供 UnitConverter 工具类 | P2 |

### 4.2 中期方案（3-6 个月）

| 动作 | 说明 | 优先级 |
|------|------|--------|
| 业务表.unit → unit_id (FK) | VARCHAR 改为引用 inventory_unit.id | P2 |
| 采购收货自动换算 | 收货确认时自动转换采购单位→库存单位 | P2 |
| 配方管理换算 | 配方编辑时自动显示换算后的用量 | P2 |
| 门店调拨换算 | 调拨单支持源/目标单位自动转换 | P3 |

### 4.3 长期方案（6-12 个月）

| 动作 | 说明 |
|------|------|
| 五种 UOM 角色分离 | material 级别定义 Purchase/Stock/Recipe/Sales UOM |
| 跨类别换算支持 | 体积↔重量（需密度参数） |
| 门店级单位配置 | 不同门店可使用不同单位（如大杯/中杯/小杯） |

---

## 五、与 DEC-002 的关系

DEC-002 (Unit Decision) 当前推荐方案为 **A_enum（保持枚举）**，短期风险最低。

本分析建议：
- **短期**: 采纳 DEC-002 的 A_enum 方案，但激活 inventory_unit 表作为字典
- **中期**: 演进到 DEC-002 的 B_independent_table 方案
- **条件**: 当出现"采购箱→库存瓶"的换算需求时，立即升级

---

## 六、总结

| 问题 | 结论 |
|------|------|
| Unit 应该是共享 Foundation 还是对象内部配置？ | **短期：混合模式（inventory_unit 作为字典 + 业务表保留 VARCHAR）；中期：共享 Foundation（units 表 + FK 引用）** |
| Base/Purchase/Stock/Recipe/Sales UOM 是否需要分别管理？ | **是。每个业务场景有自己的单位，通过 unit_conversion 表建立换算关系** |
| Conversion 应该如何实现？ | **短期：静态转换表 + 应用层换算；中期：Base Unit + Conversion Rate 内嵌在 units 表中** |
| inventory_unit 表是否应该被激活？ | **是。它已经是 Foundation，只需接入业务链路** |

---

*文档结束*
