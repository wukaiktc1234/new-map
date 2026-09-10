# Phase 8 - Purchasable 能力分析

> **核心问题**: PURCHASABLE 应该属于 Item 的能力还是 Procurement Profile?
> **状态**: ANALYSIS
> **关联文档**: concept-taxonomy.md, procurement-model.md, type-vs-role-analysis.md, business-role-model.md

---

## 1. 问题域

### 1.1 核心问题

1. Supplier, Purchase Unit, MOQ, Lead Time, Purchase Specification, Price, Tax — 这些属于 Identity / Role / Capability / Profile 中的哪一层?
2. PURCHASABLE 是 Item 的固有能力还是可配置的 Procurement Profile?
3. 采购域和库存域是否共享同一个 Purchasable 概念?

### 1.2 采购属性清单

| 属性 | 说明 | 示例 |
|------|------|------|
| **Supplier** | 供应商 | 可口可乐经销商 |
| **Purchase Unit** | 采购单位 | 箱、件、kg |
| **MOQ** | 最小起订量 | 10箱 |
| **Lead Time** | 交货周期 | 3天 |
| **Purchase Specification** | 采购规格 | 330ml×24罐/箱 |
| **Purchase Price** | 采购价格 | 48元/箱 |
| **Tax** | 税率 | 13%增值税 |
| **Barcode** | 条形码 | 6901028075831 |
| **Origin** | 产地 | 广东省广州市 |

---

## 2. 当前现实 (CURRENT REALITY)

### 2.1 采购相关表结构

```
suppliers (供应商主数据):
├── supplier_id (PK)
├── supplier_name
├── contact_info
└── ...

material_archives (物料主数据):
├── material_id (PK)
├── material_code, material_name
├── unit (采购单位)
├── spec (采购规格)
├── reference_price (参考价格)
├── supplier_id (主供应商)
├── barcode
├── origin
├── shelf_life
└── storage_condition

purchase_orders (采购订单):
├── order_id (PK)
├── supplier_id → suppliers
├── warehouse_id → 收货仓库
└── order_status

purchase_order_items (采购订单明细):
├── item_id (PK)
├── material_id → material_archives
├── quantity, unit_price, amount
├── received_quantity
└── planned_store_id / planned_warehouse_id

purchase_stockin_items (采购入库明细):
├── stockin_item_id (PK)
├── material_id → material_archives
├── actual_quantity, unit_price
├── batch_no, production_date, expiry_date
└── location_id
```

### 2.2 关键事实

| 事实 | 证据 | 影响 |
|------|------|------|
| 采购对象是 material_id | `purchase_order_items.material_id` | Material 是采购的 Canonical Identity |
| 供应商信息在 material_archives | `material_archives.supplier_id` | 每个物料有一个主供应商 |
| 参考价格在 material_archives | `material_archives.reference_price` | 采购价格是物料的属性 |
| 采购单位在 material_archives | `material_archives.unit` | 采购单位是物料的属性 |
| 无 MOQ / Lead Time 字段 | material_archives 中未找到 | 缺失关键采购属性 |
| 无 Tax 字段 | material_archives 中未找到 | 税率未在物料层管理 |
| 采购订单有 unit_price | `purchase_order_items.unit_price` | 实际采购价格在订单中确定 |

### 2.3 当前系统的隐含假设

当前系统将采购属性（supplier_id, reference_price, unit, spec）直接嵌入 material_archives 表中。这意味着：
- 每个物料只有一个主供应商
- 每个物料只有一个参考价格
- 采购属性是物料的固有属性，不可按门店/场景变化

---

## 3. Purchasable 的本质是什么？

### 3.1 候选模型分析

#### 模型 A: Purchasable 作为 IDENTITY 的一部分

```
Material ← IDENTITY { supplier, price, unit, spec } → "采购身份"
```

**支持证据**:
- 当前系统将 supplier_id, reference_price, unit 直接放在 material_archives 中
- 这些属性似乎定义了"这个物料的采购身份"

**反对证据**:
- 同一物料可以从不同供应商采购（多供应商场景）
- 同一物料可以有不同采购价格（批量折扣）
- 采购单位可能和库存单位不同（采购"箱"，库存"瓶"）
- Identity 应该是稳定的、唯一的；但采购属性是可变的

**结论**: ❌ Purchasable 不是 IDENTITY — 采购属性是可变的，不是身份标识

#### 模型 B: Purchasable 作为 TYPE

```
Material ← TYPE(Purchasable) → "这个东西是可采购的"
```

**支持证据**:
- concept-taxonomy.md 将 Purchasable 分类为 CAPABILITY + ROLE
- 某些物料确实"天生可采购"（如原料），某些"天生不可采购"（如自制菜品）

**反对证据**:
- 同一物料在不同门店可能有不同的采购策略
- 宫保鸡丁在门店 A 可能需要采购半成品（Purchasable），在门店 B 是自制（Not Purchasable）
- Type 是固有的、不可变的；但 Purchasable 是可配置的

**结论**: ❌ Purchasable 不是 TYPE

#### 模型 C: Purchasable 作为 CAPABILITY

```
Material ← CAPABILITY(Purchasable) → "我具备被采购的能力"
```

**支持证据**:
- 采购能力是全局声明：可乐能被采购，宫保鸡丁不能
- 能力是布尔值：能/不能
- concept-taxonomy.md 分类为 CAPABILITY

**反对证据**:
- 纯 Capability 无法承载采购属性（supplier, price, unit, MOQ）
- 需要额外的配置层来存储采购参数

**结论**: ⚠️ Purchasable 的核心是 CAPABILITY，但需要 Profile 来承载配置数据

#### 模型 D: Purchasable 作为 ROLE

```
Material ← ROLE(Purchasable) → "在采购场景中扮演什么角色"
```

**支持证据**:
- business-role-model.md 将 PURCHASABLE 定义为 Business Role
- type-vs-role-analysis.md 确认 Role 可以按场景配置
- Role 可以携带场景特定数据（supplier, price, unit）

**反对证据**:
- Role 描述"做什么"，但 Purchasable 更像"能被采购"
- 如果是 Role，那么需要在每个采购场景中"激活"这个 Role

**结论**: ⚠️ 接近但不精确 — Purchasable 首先是 Capability，Role 是其配置载体

#### 模型 E: Purchasable 作为 PROFILE

```
Material ← PROFILE(ProcurementProfile) → "采购配置信息"
```

**支持证据**:
- Supplier, Price, Unit, MOQ, Lead Time 等确实是 Profile 数据
- 这些数据描述了"怎么采购"，不是"是什么"
- Profile 可以按场景/门店独立配置

**反对证据**:
- Profile 描述"是什么样子"，不声明"能做什么"
- 需要先声明 Purchasable Capability，然后才有 Procurement Profile

**结论**: ✅ Procurement Profile 是 Purchasable 能力的配置数据层

### 3.2 综合结论

**Purchasable = CAPABILITY（能力声明）+ PROFILE（Procurement Profile）**

```
Purchasable 能力模型:
┌─────────────────────────────────────────────────────────┐
│                    Material (Canonical Item)              │
│                                                         │
│  capability声明:                                         │
│    purchasable: boolean  ← "我具备被采购的能力"            │
│                                                         │
│  procurement profile (采购配置):                          │
│    ├── supplier_id: 供应商                               │
│    ├── purchase_unit: 采购单位                            │
│    ├── moq: 最小起订量                                   │
│    ├── lead_time: 交货周期                               │
│    ├── purchase_spec: 采购规格                            │
│    ├── reference_price: 参考价格                          │
│    ├── tax_rate: 税率                                    │
│    └── barcode: 条形码                                   │
│                                                         │
│  实例数据 (Purchase Order Instance):                      │
│    ├── purchase_order_items.material_id                  │
│    ├── purchase_order_items.unit_price                   │
│    └── purchase_order_items.quantity                     │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 采购属性的分层分析

### 4.1 属性归属矩阵

| 属性 | 当前位置 | 应归属层次 | 说明 |
|------|----------|-----------|------|
| **Supplier** | material_archives.supplier_id | PROFILE | 供应商是采购配置，不是身份 |
| **Purchase Unit** | material_archives.unit | PROFILE | 采购单位是采购配置 |
| **MOQ** | 不存在 | PROFILE | 最小起订量是采购配置 |
| **Lead Time** | 不存在 | PROFILE | 交货周期是采购配置 |
| **Purchase Spec** | material_archives.spec | PROFILE | 采购规格是采购配置 |
| **Purchase Price** | material_archives.reference_price | PROFILE | 参考价格是采购配置 |
| **Tax Rate** | 不存在 | PROFILE | 税率是采购配置 |
| **Barcode** | material_archives.barcode | IDENTITY | 条形码是物料标识 |
| **Origin** | material_archives.origin | IDENTITY | 产地是物料标识 |

### 4.2 关键区分: 什么属于 IDENTITY，什么属于 PROFILE?

**判断标准**:

| 标准 | IDENTITY | PROFILE |
|------|----------|---------|
| **稳定性** | 高（创建后很少变） | 中（业务变化时调整） |
| **唯一性** | 高（用于标识） | 低（同一物品可有多个配置） |
| **跨域一致性** | 高（所有场景共享） | 低（不同场景不同配置） |
| **业务语义** | "这是什么" | "怎么用" |

**应用到采购属性**:

| 属性 | 稳定性 | 唯一性 | 跨域一致性 | 归属 |
|------|--------|--------|-----------|------|
| Supplier | 低（可换供应商） | 低（多供应商） | 低 | PROFILE |
| Purchase Unit | 中（可能调整） | 低（可能有多个单位） | 中 | PROFILE |
| MOQ | 低（供应商可调整） | 低 | 低 | PROFILE |
| Lead Time | 低 | 低 | 低 | PROFILE |
| Reference Price | 低（市场波动） | 低 | 低 | PROFILE |
| Tax Rate | 中 | 低（不同物料不同税率） | 中 | PROFILE |
| Barcode | 高 | 高 | 高 | IDENTITY |
| Origin | 高 | 中 | 高 | IDENTITY |

---

## 5. 多供应商场景分析

### 5.1 问题: 同一物料可以从多个供应商采购

**场景**: 可乐可以从供应商 A 采购，也可以从供应商 B 采购。

**当前系统的处理**:
- `material_archives.supplier_id` 只能存储一个主供应商
- 无法记录"可乐可以从供应商 A 或 B 采购"
- `purchase_order_items` 中有 `supplier_id`（通过 purchase_orders），但这是一次性指定

**问题**: 如果供应商 A 缺货，需要切换到供应商 B，当前系统如何处理？

### 5.2 目标模型: 一个物料可以有多个 Procurement Profile

```
material_archives (Canonical Item):
  material_id=M001, material_name="可乐"

material_capability:
  material_id=M001, capability_type=PURCHASABLE, enabled=true

procurement_profile:
  ├── profile_id=P001, material_id=M001, supplier_id=S001
  │   reference_price=4800(分/箱), moq=10, lead_time=3天
  │
  └── profile_id=P002, material_id=M001, supplier_id=S002
      reference_price=5000(分/箱), moq=5, lead_time=2天
```

### 5.3 验证: 多供应商场景

| 场景 | 供应商 | 价格 | MOQ | Lead Time |
|------|--------|------|-----|-----------|
| 常规采购 | 供应商 A (P001) | 48元/箱 | 10箱 | 3天 |
| 紧急采购 | 供应商 B (P002) | 50元/箱 | 5箱 | 2天 |
| 批量采购 | 供应商 A (P001) | 45元/箱 | 50箱 | 5天 |

**结论**: 多供应商场景需要多个 Procurement Profile。

---

## 6. 采购单位 vs 库存单位 vs 销售单位

### 6.1 三单位模型

```
同一物料的三种单位:

可乐 330ml:
├── 采购单位: 箱 (1箱 = 24罐)
├── 库存单位: 瓶 (单罐管理)
└── 销售单位: 瓶 (单瓶销售)

大米 10kg:
├── 采购单位: 袋 (1袋 = 10kg)
├── 库存单位: kg (散装管理)
└── 销售单位: 份 (按菜品用量)

餐盒:
├── 采购单位: 箱 (1箱 = 100个)
├── 库存单位: 个 (单个管理)
└── 销售单位: N/A (不直接销售)
```

### 6.2 单位转换

| 物料 | 采购单位 | 转换系数 | 库存单位 |
|------|----------|----------|----------|
| 可乐 | 箱 | 1箱 = 24瓶 | 瓶 |
| 大米 | 袋 | 1袋 = 10kg | kg |
| 酱油 | 箱 | 1箱 = 12瓶 | 瓶 |

### 6.3 单位归属分析

| 单位类型 | 归属层次 | 说明 |
|----------|----------|------|
| 采购单位 | PROFILE | 采购配置，可能随供应商变化 |
| 库存单位 | IDENTITY / CAPABILITY Config | 库存管理的基础单位 |
| 销售单位 | ROLE (SELLABLE) Config | 销售配置 |

---

## 7. 真实案例验证

### 案例 A: 可乐的完整 Purchasable 生命周期

```
1. 创建物料
   material_archives: 可乐, material_id=M001
   capability: purchasable = true

2. 配置 Procurement Profile
   procurement_profile: supplier_id=S001, reference_price=4800(分/箱)
   procurement_profile: supplier_id=S002, reference_price=5000(分/箱)
   purchase_unit = '箱', moq = 10, lead_time = 3天

3. 创建采购订单
   purchase_orders: supplier_id=S001, warehouse_id=W01
   purchase_order_items: material_id=M001, quantity=20, unit_price=4800

4. 采购入库
   purchase_stockin_items: material_id=M001, actual_quantity=480(20箱×24瓶)
   inventory: material_id=M001, warehouse_id=W01, current_stock += 480

5. 成本流转
   purchase_order_items.unit_price → inventory.unit_cost
```

### 案例 B: 大米的多供应商采购

```
1. 创建物料
   material_archives: 大米, material_id=M003
   capability: purchasable = true

2. 配置多个 Procurement Profile
   procurement_profile: supplier_id=S003 (东北供应商)
     reference_price=8000(分/袋), purchase_unit='袋', moq=20
   procurement_profile: supplier_id=S004 (泰国供应商)
     reference_price=10000(分/袋), purchase_unit='袋', moq=10

3. 采购决策
   常规采购 → 供应商 S003 (价格低)
   紧急采购 → 供应商 S004 (交货快)
```

### 案例 C: 宫保鸡丁的 Purchasable 分析

```
1. 创建菜品
   foods: 宫保鸡丁, food_id=F001

2. Capability 声明
   purchasable = false ← 自制菜品，不从外部采购

3. 虽然宫保鸡丁不采购，但其原料需要采购
   鸡翅: purchasable = true
   大米: purchasable = true
   酱油: purchasable = true

4. 成本流转
   原料采购价格 → 原料库存成本 → 菜品配方成本
```

---

## 8. 目标技术模型 (TARGET TECHNICAL MODEL)

### 8.1 Purchasable 能力模型

```sql
-- 采购能力声明
CREATE TABLE material_capability (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    capability_type VARCHAR(50) NOT NULL,  -- PURCHASABLE
    enabled BOOLEAN DEFAULT TRUE,
    config JSON,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(material_id, capability_type)
);

-- 采购配置 (Procurement Profile)
CREATE TABLE procurement_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    purchase_unit VARCHAR(20),           -- 采购单位: 箱/件/kg
    moq INT,                             -- 最小起订量
    lead_time_days INT,                  -- 交货周期(天)
    purchase_spec VARCHAR(100),          -- 采购规格: 330ml×24罐/箱
    reference_price BIGINT,              -- 参考价格(分)
    tax_rate DECIMAL(5,2),              -- 税率(%)
    is_primary BOOLEAN DEFAULT FALSE,    -- 是否主供应商
    status INT DEFAULT 1,               -- 状态
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(material_id, supplier_id)
);
```

### 8.2 与现有表的关系

```
material_archives (Canonical Item)
    │
    ├── material_capability (能力声明)
    │   material_id + capability_type = 'PURCHASABLE'
    │   enabled = true/false
    │
    ├── procurement_profile (采购配置)
    │   material_id + supplier_id
    │   purchase_unit, moq, lead_time, reference_price, tax_rate
    │
    ├── purchase_order_items (采购订单)
    │   material_id, quantity, unit_price
    │   从 procurement_profile 获取默认配置
    │
    └── purchase_stockin_items (采购入库)
        material_id, actual_quantity, unit_price
        入库后更新 inventory.unit_cost
```

### 8.3 查询模式

```sql
-- 查询物料的采购配置
SELECT pp.*, s.supplier_name
FROM procurement_profile pp
JOIN suppliers s ON pp.supplier_id = s.supplier_id
WHERE pp.material_id = M001
  AND pp.status = 1;

-- 查询物料的主供应商采购配置
SELECT pp.*, s.supplier_name
FROM procurement_profile pp
JOIN suppliers s ON pp.supplier_id = s.supplier_id
WHERE pp.material_id = M001
  AND pp.is_primary = TRUE
  AND pp.status = 1;

-- 创建采购订单时获取默认配置
SELECT pp.purchase_unit, pp.reference_price, pp.moq, pp.lead_time_days
FROM procurement_profile pp
WHERE pp.material_id = M001
  AND pp.supplier_id = S001
  AND pp.status = 1;
```

---

## 9. 与现有决策的关系

### 9.1 与 procurement-model.md 的关系

procurement-model.md 已经确认:
- 采购对象是 Material（material_id）
- Purchasable 是 Material 的 Business Role
- 采购和库存共享 material_id

本分析进一步细化:
- Purchasable = Capability + Profile
- Profile 包含 supplier, price, unit, MOQ 等配置

### 9.2 与 type-vs-role-analysis.md 的关系

type-vs-role-analysis.md 确认:
- Type 和 Role 应该分离
- Role 可以按场景配置

本分析确认:
- Purchasable 是 Role 的一种（采购场景的 Role）
- 但更精确地说，Purchasable 是 Capability，Profile 是其配置载体

### 9.3 与 store-scope-analysis.md 的关系

store-scope-analysis.md 提出了 item_store_config 表:
- 门店级采购配置: is_purchasable
- 门店级采购价格: purchase_price

本分析与之一致:
- material_capability 中的 purchasable = 全局能力声明
- item_store_config 中的 is_purchasable = 门店级能力覆盖
- procurement_profile = 采购配置详情（可能有多个供应商）

---

## 10. 结论

### 10.1 回答核心问题

| 问题 | 结论 |
|------|------|
| PURCHASABLE 应该属于 Item 的能力还是 Procurement Profile? | **两者都需要。Capability 声明能力，Profile 承载配置。** |
| Supplier 属于哪一层? | **PROFILE** — 供应商是采购配置，不是身份标识 |
| Purchase Unit 属于哪一层? | **PROFILE** — 采购单位是采购配置 |
| MOQ / Lead Time 属于哪一层? | **PROFILE** — 这些是采购配置参数 |
| Price 属于哪一层? | **PROFILE** — 采购价格是采购配置 |
| Tax 属于哪一层? | **PROFILE** — 税率是采购配置 |

### 10.2 分层总结

| 层次 | 概念 | 说明 | 示例 |
|------|------|------|------|
| **Capability** | purchasable=true/false | "我能不能被采购" | 可乐: true, 宫保鸡丁: false |
| **Profile** | procurement_profile | "怎么采购" | 供应商A, 48元/箱, MOQ=10 |
| **Instance** | purchase_order_items | "实际采购记录" | 本次采购20箱, 4800分/箱 |

### 10.3 为什么不能把 Purchasable 混为其他概念

1. **不是 Identity**: 采购属性（supplier, price）是可变的，不是身份标识
2. **不是 Type**: 同一物料在不同场景可以有不同的采购策略
3. **不是纯 Role**: Role 描述"做什么"，Purchasable 首先声明"能做什么"
4. **不是 State**: State 是临时的，Purchasable 是持久的能力声明

---

**分析完成时间**: 2026-09-09
**分析结论**: Purchasable = CAPABILITY（全局能力声明）+ PROFILE（Procurement Profile 配置）
**推荐行动**: 新增 procurement_profile 表，将 supplier_id, reference_price, unit 等从 material_archives 迁移到 procurement_profile
