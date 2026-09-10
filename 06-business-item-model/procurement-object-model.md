# Procurement 对象模型分析 (Phase 18)

> **核心问题**："采购对象"到底是什么概念？Supplier 能否供应同一 Item 的不同 Pack/Unit/Specification？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

本分析必须回答：
1. "采购对象"到底是什么概念？Material？Product？Purchasable Item？Procurement Line Item？
2. 一个 Supplier 能不能供应同一 Item 的不同 Pack / Unit / Specification？
3. 采购对象和库存对象是否应该是同一个概念？
4. Purchasable 是 Item 的属性、Role 还是 Capability？

**关键约束**：不得为了迁就现有数据库表结构定义最终业务模型。

---

## 2. 候选概念定义

| 候选概念 | 定义 | 典型代表 | 系统中的位置 |
|----------|------|----------|-------------|
| **Purchase Material** | 采购流程中的物料，以 material_id 为标识 | 土豆5kg装、可乐330ml×24罐 | `purchase_order_items.material_id` |
| **Purchase Product** | 采购流程中的商品（遗留概念） | 可口可乐 | `product`（已废弃） |
| **Purchasable Item** | 可以从供应商采购的标准化项 | 标准化的采购项 | 无（理想模型） |
| **Procurement Line Item** | 采购订单中的具体行项 | PO-001 第1行：可乐200罐 | `purchase_order_items` |

---

## 3. 业务案例深度分析

### 3.1 场景一：同一 Item 的不同包装规格

**场景**：一家餐厅从供应商采购"可乐"，但有不同的包装规格：

| 供应商 | 产品 | 包装规格 | 采购单位 | 采购单价 |
|--------|------|----------|----------|----------|
| 供应商A | 可口可乐 | 330ml×24罐/箱 | 箱 | ¥48.00 |
| 供应商A | 可口可乐 | 330ml×12罐/半箱 | 半箱 | ¥25.00 |
| 供应商B | 可口可乐 | 500ml×24瓶/箱 | 箱 | ¥68.00 |
| 供应商B | 可口可乐 | 1.25L×6瓶/箱 | 箱 | ¥42.00 |

**问题**：这些是同一个 "Item" 还是不同的 "Item"？

**分析**：
- **物理实体**：都是"可口可乐"碳酸饮料
- **商业实体**：不同的 SKU（330ml罐装 vs 500ml瓶装 vs 1.25L瓶装）
- **采购实体**：不同的采购项（不同供应商、不同包装、不同价格）

**结论**：采购对象需要区分**物理实体**和**商业包装**。

### 3.2 场景二：同一供应商的不同规格

**场景**：供应商A 同时供应"鸡翅"的不同部位：

| 供应商 | 产品 | 规格 | 采购单位 | 采购单价 |
|--------|------|------|----------|----------|
| 供应商A | 鸡翅中 | 冷冻500g/袋 | 袋 | ¥15.00 |
| 供应商A | 鸡翅中 | 冷冻1kg/袋 | 袋 | ¥28.00 |
| 供应商A | 鸡翅尖 | 冷冻500g/袋 | 袋 | ¥8.00 |
| 供应商A | 鸡全翅 | 冷冻1kg/袋 | 袋 | ¥22.00 |

**问题**：这些是同一个 "Item" 还是不同的 "Item"？

**分析**：
- **物理实体**：鸡翅的不同部位，物理上不同
- **商业实体**：不同的 SKU（鸡翅中 vs 鸡翅尖 vs 鸡全翅）
- **采购实体**：不同的采购项（不同部位、不同规格）

**结论**：采购对象需要区分**物理实体**和**商业 SKU**。

### 3.3 场景三：同一 Item 的不同用途

**场景**：餐厅采购"大米"，有两种用途：

| 用途 | 产品 | 规格 | 采购单位 | 采购单价 | 消耗场景 |
|------|------|------|----------|----------|----------|
| 厨房用米 | 东北大米 | 50kg/袋 | 袋 | ¥120.00 | 蒸米饭 |
| 赠品米 | 小包装米 | 1kg/袋 | 袋 | ¥5.00 | 赠送给客户 |

**问题**：这些是同一个 "Item" 还是不同的 "Item"？

**分析**：
- **物理实体**：都是大米，但品质和用途不同
- **商业实体**：不同的 SKU（50kg装 vs 1kg装）
- **采购实体**：不同的采购项（不同规格、不同价格）
- **消耗场景**：一个作为原料（厨房蒸饭），一个作为赠品（客户赠送）

**结论**：采购对象需要区分**物理实体**和**消耗场景**。

### 3.4 场景四：预包装商品的采购

**场景**：餐厅采购"薯片"作为商品销售：

| 供应商 | 产品 | 规格 | 采购单位 | 采购单价 | 销售价格 |
|--------|------|------|----------|----------|----------|
| 供应商C | 乐事薯片 | 104g/袋 | 袋 | ¥3.50 | ¥6.00 |
| 供应商C | 乐事薯片 | 75g/袋 | 袋 | ¥2.80 | ¥5.00 |
| 供应商D | 品客薯片 | 110g/罐 | 罐 | ¥8.00 | ¥12.00 |

**问题**：薯片作为预包装商品，采购对象是什么？

**分析**：
- **物理实体**：预包装薯片，有独立的 SKU
- **商业实体**：不同的品牌和规格
- **采购实体**：不同的采购项
- **销售实体**：直接作为商品销售（`foods` 表）

**结论**：预包装商品的采购对象 = Material + SELLABLE Role。

---

## 4. 当前系统现实分析

### 4.1 表结构依赖图

```
suppliers (供应商主数据)
  │
  └──► purchase_request (采购申请单 - 门店发起)
  │       ├── request_id
  │       ├── store_id / warehouse_id
  │       └── status: 0待提交 1待审批 2已审批 3已驳回 4已完成
  │
  └──► purchase_plan (采购计划)
  │       ├── plan_id
  │       └── material_id, planned_quantity
  │
  └──► purchase_orders (采购订单 - 主表)
  │       ├── order_id
  │       ├── supplier_id → suppliers
  │       ├── warehouse_id → 收货仓库
  │       └── order_status: 0草稿 1待审核 2已审核 3部分入库 4已完成 5已取消
  │       │
  │       └──► purchase_order_items (采购订单明细)
  │               ├── item_id
  │               ├── material_id → material_archives
  │               ├── quantity, unit_price, amount
  │               ├── received_quantity (已收货数量)
  │               └── planned_store_id / planned_warehouse_id (计划收货方)
  │
  └──► purchase_stockins (采购入库单 - 主表)
  │       ├── stockin_id
  │       ├── order_id → purchase_orders
  │       ├── warehouse_id → 入库仓库
  │       └── stockin_type: 1正常入库 2退货入库 3赠品入库
  │       │
  │       └──► purchase_stockin_items (采购入库明细)
  │               ├── stockin_item_id
  │               ├── material_id → material_archives
  │               ├── actual_quantity, unit_price
  │               ├── batch_no, production_date, expiry_date
  │               └── location_id → inventory_locations
  │
  └──► purchase_return (采购退货)
  │
  └──► purchase_settlements (采购结算)
          ├── settlement_id
          ├── order_id → purchase_orders
          └── supplier_id → suppliers
```

### 4.2 关键发现

| 发现 | 证据 | 影响 |
|------|------|------|
| 采购订单明细使用 `material_id` | `purchase_order_items.material_id` | 采购对象 = material |
| 采购入库明细也使用 `material_id` | `purchase_stockin_items.material_id` | 入库对象 = material |
| `material_archives` 是采购的基础主数据 | 建表注释："采购物料/商品的基础信息维护" | Material 是采购和库存的共享概念 |
| 采购订单支持"计划收货方" | `planned_store_id` / `planned_warehouse_id` | 采购可以直送门店或入仓库 |
| 入库单关联采购订单 | `purchase_stockins.order_id → purchase_orders.order_id` | 入库是对订单的履约 |

### 4.3 purchase_order_items 的当前结构

```sql
CREATE TABLE purchase_order_items (
    item_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id            BIGINT NOT NULL,              -- 关联采购订单
    material_id         BIGINT NOT NULL,              -- 物料ID（关联物料主数据）
    material_name       VARCHAR(200),                 -- 物料名称（冗余）
    specification       VARCHAR(100),                 -- 规格
    unit                VARCHAR(20),                  -- 单位
    quantity            DECIMAL(12,3) NOT NULL,       -- 采购数量
    unit_price          BIGINT NOT NULL,               -- 单价（分）
    amount              BIGINT DEFAULT 0,              -- 金额（分）
    tax_rate            DECIMAL(5,4),                 -- 税率
    received_quantity   DECIMAL(12,3) DEFAULT 0,      -- 已收货数量
    planned_receiver_type VARCHAR(20),                -- 计划收货方类型
    planned_store_id    VARCHAR(50),                  -- 计划收货门店ID
    planned_warehouse_id BIGINT,                      -- 计划收货仓库ID
    ...
);
```

**关键发现**：
- `material_id` 是采购对象的唯一标识
- `specification`、`unit`、`unit_price` 是采购行项的属性，不是 material 的属性
- `planned_receiver_type` + `planned_store_id` / `planned_warehouse_id` 支持直送门店

### 4.4 material_archives 的当前结构

```sql
CREATE TABLE material_archives (
    material_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    material_code   VARCHAR(50)   NOT NULL,           -- 商品编码（业务唯一）
    material_name   VARCHAR(200)  NOT NULL,           -- 商品名称
    category_id     BIGINT,                           -- 分类ID
    unit            VARCHAR(50)   NOT NULL,           -- 单位
    spec            VARCHAR(200),                     -- 规格型号
    reference_price BIGINT        NOT NULL DEFAULT 0, -- 参考价（分）
    supplier_id     BIGINT,                           -- 主供应商ID
    ...
);
```

**关键发现**：
- `material_archives` 同时存储了**物料主数据**和**采购属性**（reference_price, supplier_id）
- `specification`（规格）是物料的固有属性，但采购订单中也有 `specification` 字段
- `reference_price` 是参考价，但采购订单中有 `unit_price`（实际采购价）

---

## 5. 候选概念深度比较

### 5.1 Purchase Material

**定义**：采购流程中的物料，以 `material_id` 为标识，携带采购特定的属性。

**支持证据**：
- 所有活跃采购表都使用 `material_id`
- `material_archives` 是采购的基础主数据
- 采购流程：`purchase_order_items.material_id` → `material_archives.material_id`

**反对证据**：
- `material_archives` 同时也被叫作"商品档案"，语义含糊
- Purchase Material 无法区分"采购对象"和"库存对象"的差异
- Purchase Material 无法表达"同一物料的不同包装规格"

**结论**：Purchase Material 是当前系统的采购对象，但语义需要澄清。

### 5.2 Purchase Product

**定义**：采购流程中的商品（遗留概念）。

**支持证据**：
- `inventory.product_id` 仍使用 product_id
- `Inventory.java` 中 `getProductId()` 返回 `materialId`

**反对证据**：
- `product` 表已被标记为废弃
- 当前所有活跃采购表都使用 `material_id`
- Product 无法区分"可销售"和"可采购"

**结论**：Purchase Product 已被历史淘汰，不应作为采购对象。

### 5.3 Purchasable Item

**定义**：可以从供应商采购的标准化项，包含物料信息和采购配置。

**支持证据**：
- 理论上更精确的命名
- Purchasable Item 可以区分"物料"和"采购配置"
- Purchasable Item 可以支持同一物料的不同包装规格

**反对证据**：
- 系统中无此实体
- Material 已承担此职责
- 引入 Purchasable Item 需要重构采购流程

**结论**：Purchasable Item 是目标模型的理想形态，但当前未实现。

### 5.4 Procurement Line Item

**定义**：采购订单中的具体行项，包含物料信息、数量、价格、收货方等。

**支持证据**：
- `purchase_order_items` 实际上就是 Procurement Line Item
- Procurement Line Item 携带了采购特定的属性（quantity, unit_price, planned_receiver）
- Procurement Line Item 可以支持同一物料的不同包装规格

**反对证据**：
- Procurement Line Item 是采购订单的一部分，不是独立的采购对象
- Procurement Line Item 的生命周期与采购订单绑定

**结论**：Procurement Line Item 是采购订单的行项，不是独立的采购对象。

---

## 6. "采购对象"的语义分析

### 6.1 采购对象的双重含义

**含义一：采购什么东西？（What）**
- 物理实体：可口可乐碳酸饮料
- 商业 SKU：可口可乐330ml×24罐/箱
- 系统标识：material_id

**含义二：怎么采购？（How）**
- 供应商：供应商A
- 包装规格：330ml×24罐/箱
- 采购单位：箱
- 采购价格：¥48.00/箱
- 收货方：仓库/门店

### 6.2 采购对象 = Material + Purchasable Role Config

```
Material (Canonical Identity)
  ├── material_id: M001
  ├── material_name: "可口可乐330ml"
  └── specification: "330ml/罐"

Purchasable Role Config (采购角色配置)
  ├── config_id: PC001
  ├── material_id: M001
  ├── supplier_id: S001
  ├── purchase_unit: "箱"
  ├── purchase_spec: "330ml×24罐/箱"
  ├── reference_price: 4800 (分)
  ├── min_order_qty: 5
  └── lead_time_days: 3
```

### 6.3 一个 Supplier 能否供应同一 Item 的不同 Pack/Unit/Specification？

**答案：是的。**

**证据**：
- 供应商A 可乐330ml×24罐/箱 ¥48.00
- 供应商A 可乐330ml×12罐/半箱 ¥25.00
- 供应商A 可乐500ml×24瓶/箱 ¥68.00

**建模方式**：

```
Material: 可口可乐 (M001)
  │
  ├── Purchasable Config 1: 供应商A, 箱装330ml×24罐
  │     ├── supplier_id: S001
  │     ├── purchase_unit: "箱"
  │     ├── purchase_spec: "330ml×24罐/箱"
  │     ├── reference_price: 4800
  │     └── min_order_qty: 5
  │
  ├── Purchasable Config 2: 供应商A, 半箱330ml×12罐
  │     ├── supplier_id: S001
  │     ├── purchase_unit: "半箱"
  │     ├── purchase_spec: "330ml×12罐/半箱"
  │     ├── reference_price: 2500
  │     └── min_order_qty: 10
  │
  └── Purchasable Config 3: 供应商B, 箱装500ml×24瓶
        ├── supplier_id: S002
        ├── purchase_unit: "箱"
        ├── purchase_spec: "500ml×24瓶/箱"
        ├── reference_price: 6800
        └── min_order_qty: 3
```

---

## 7. 采购模型的核心实体关系

### 7.1 实体职责矩阵

| 实体 | 职责 | 核心字段 | 状态机 |
|------|------|----------|--------|
| `material_archives` | 物料主数据（Canonical Identity） | material_id, material_code, material_name, unit, spec, reference_price, supplier_id | 1启用 / 0停用 |
| `suppliers` | 供应商主数据 | supplier_id, supplier_name, contact_info | — |
| `purchase_request` | 采购申请（门店发起） | store_id, material_id, quantity | 0待提交→1待审批→2已审批/3已驳回→4已完成 |
| `purchase_plan` | 采购计划（汇总） | plan_id, material_id, planned_quantity | — |
| `purchase_orders` | 采购订单（合同） | order_id, supplier_id, warehouse_id, total_amount | 0草稿→1待审核→2已审核→3部分入库→4已完成/5已取消 |
| `purchase_order_items` | 订单明细（行项） | material_id, quantity, unit_price, received_quantity | — |
| `purchase_stockins` | 入库单（收货确认） | stockin_id, order_id, warehouse_id | 0待入库→1已入库→2部分入库 |
| `purchase_stockin_items` | 入库明细（行项） | material_id, actual_quantity, batch_no | — |
| `purchase_return` | 退货单 | return_id, order_id | — |
| `purchase_settlements` | 结算单（财务） | settlement_id, order_id, total_amount, paid_amount | 0待结算→...→3已完成 |

### 7.2 采购与库存的衔接点

```
采购入库完成时：
  1. purchase_stockin_items.material_id → 确定入库物料
  2. purchase_stockin_items.actual_quantity → 入库数量
  3. purchase_stockin_items.unit_price → 入库成本
  4. purchase_stockins.warehouse_id → 确定入库仓库
  5. purchase_stockin_items.location_id → 确定入库库位

库存更新逻辑：
  inventory(material_id, warehouse_id).current_stock += actual_quantity
  inventory(material_id, warehouse_id).cost_price = unit_price (或加权平均)
  inventory_transactions 插入一条 transaction_type=1 (采购入库) 的记录
```

### 7.3 采购流程状态机

```
purchase_request (采购申请)
  │ 审批通过
  ▼
purchase_plan (采购计划) ── 可选 ──► purchase_orders (采购订单)
                                        │ 创建
                                        ▼
                                   purchase_order_items (订单明细)
                                        │ 供应商确认
                                        ▼
                                   purchase_stockins (入库单)
                                        │ 收货确认
                                        ▼
                                   purchase_stockin_items (入库明细)
                                        │ 入库完成
                                        ▼
                                   inventory (仓库库存 +)
                                   store_inventory (门店库存 +)
                                   inventory_transactions (流水记录)
```

---

## 8. 采购模型中的角色分析

### 8.1 Purchasable Role（可采购角色）

基于 `business-role-model.md` 的定义，Purchasable 角色包含：

**Purchasable 角色的配置数据**：

| 数据项 | 当前存储位置 | 说明 |
|--------|-------------|------|
| 物料名称 | material_archives.material_name | — |
| 物料编码 | material_archives.material_code | 业务唯一编码 |
| 采购单位 | material_archives.unit | 斤/袋/箱 |
| 规格型号 | material_archives.spec | 10kg/箱 |
| 参考价 | material_archives.reference_price | 分为单位 |
| 主供应商 | material_archives.supplier_id | 关联 suppliers |
| 分类 | material_archives.category_id | 关联 material_categories |
| 条码 | material_archives.barcode | 扫码用 |
| 产地 | material_archives.origin | — |
| 保质期 | material_archives.shelfLife | 字段已定义但未在采购流程使用 |

### 8.2 采购角色与库存角色的关系

```
material_archives
  │
  ├── [Purchasable Role]
  │     数据: reference_price, supplier_id, unit, spec
  │     消费者: purchase_order_items, purchase_stockin_items
  │     上游: 无（独立角色）
  │     下游: Stockable
  │
  └── [Stockable Role]
        数据: min_safe_qty, max_stock_qty (仓库); safety_stock, max_stock (门店)
        消费者: inventory, store_inventory
        上游: Purchasable
        下游: Consumable (领料) / Sellable (销售)
```

**关键洞察**：Purchasable 和 Stockable 是同一个 Material 在不同业务场景中的角色。它们共享 material_id，但各自携带不同的业务数据。

### 8.3 Purchasable Role 的配置问题

**当前问题**：
- `material_archives.reference_price` 是参考价，但采购订单中有 `unit_price`（实际采购价）
- `material_archives.supplier_id` 是主供应商，但同一物料可以从不同供应商采购
- `material_archives.unit` 是物料单位，但采购订单中可以有不同的采购单位

**目标模型**：

```
Material: 可口可乐 (M001)
  │
  ├── Purchasable Role Config (采购角色配置)
  │     ├── reference_price: 参考价
  │     ├── primary_supplier_id: 主供应商
  │     ├── purchase_unit: 采购单位
  │     ├── min_order_qty: 最小起订量
  │     └── lead_time_days: 交货周期
  │
  └── Supplier-Specific Config (供应商特定配置)
        ├── supplier_id: S001
        ├── purchase_price: 实际采购价
        ├── purchase_unit: 箱
        ├── purchase_spec: 330ml×24罐/箱
        └── min_order_qty: 5
```

---

## 9. 采购对象的多维度设计

### 9.1 支持"直送门店"的采购模式

`purchase_order_items` 中的 `planned_store_id` 和 `planned_warehouse_id` 字段表明系统支持两种采购收货模式：

| 模式 | 字段 | 流程 |
|------|------|------|
| **仓库收货** | `planned_warehouse_id` | 采购→仓库收货→入库→可能需要门店调拨 |
| **直送门店** | `planned_store_id` | 采购→直接到门店→门店收货确认 |

### 9.2 采购与门店库存的关系

```
模式A: 仓库采购
  purchase_orders → purchase_stockins → inventory (仓库)
       └──→ inventory_transactions (type=1)
       └──→ 后续: inventory_transfers → store_inventory (门店)

模式B: 直送门店
  purchase_orders → purchase_stockins → store_inventory (门店)
       └──→ store_inventory_log
```

### 9.3 成本流转路径

```
采购单价 (unit_price in purchase_order_items)
    ↓ 入库
入库成本 (unit_price in purchase_stockin_items)
    ↓ 更新
库存成本 (cost_price in inventory)
    ↓ 调拨
门店成本 (unit_cost in store_inventory)
```

**当前问题**：
- 仓库入库时的 `unit_price` 与门店入库时的 `unit_price` 如何保持一致？
- 如果有中间调拨，调拨成本如何计算？
- 系统中未见明确的库存成本核算规则（如加权平均法、先进先出法）

---

## 10. 业务案例验证

### 10.1 案例一：可乐的采购

**当前系统**：
- `material_archives`: material_id=M001, material_name="可口可乐330ml", reference_price=250(分), supplier_id=S001
- `purchase_order_items`: material_id=M001, quantity=200, unit_price=250(分), planned_warehouse_id=W01

**目标模型**：
- `canonical_item`: id=C001, name="可口可乐330ml"
- `purchasable_config`: canonical_item_id=C001, supplier_id=S001, purchase_unit="罐", reference_price=250
- `procurement_line_item`: canonical_item_id=C001, supplier_id=S001, quantity=200, unit_price=250, receiver=W01

### 10.2 案例二：同一物料的不同包装规格

**当前系统**（问题）：
- `material_archives`: material_id=M001, material_name="可口可乐330ml", unit="罐"
- 无法表达"330ml×24罐/箱"和"330ml×12罐/半箱"的区别

**目标模型**：
- `canonical_item`: id=C001, name="可口可乐330ml"
- `purchasable_config_1`: canonical_item_id=C001, supplier_id=S001, purchase_unit="箱", purchase_spec="330ml×24罐/箱", reference_price=4800
- `purchasable_config_2`: canonical_item_id=C001, supplier_id=S001, purchase_unit="半箱", purchase_spec="330ml×12罐/半箱", reference_price=2500

### 10.3 案例三：不同供应商的同一物料

**当前系统**（问题）：
- `material_archives`: material_id=M001, supplier_id=S001（主供应商）
- 无法表达"从供应商B采购"的场景

**目标模型**：
- `canonical_item`: id=C001, name="可口可乐330ml"
- `purchasable_config_1`: canonical_item_id=C001, supplier_id=S001, purchase_unit="箱", reference_price=4800
- `purchasable_config_2`: canonical_item_id=C001, supplier_id=S002, purchase_unit="箱", purchase_spec="500ml×24瓶/箱", reference_price=6800

---

## 11. 三维度分离

### 11.1 CURRENT REALITY（当前现实）

| 维度 | 现状 | 问题 |
|------|------|------|
| 采购对象 | `material_id` | 遗留字段和命名不一致 |
| 供应商关系 | `material_archives.supplier_id` | 一个物料只有一个主供应商 |
| 包装规格 | `material_archives.spec` | 无法区分不同包装规格 |
| 采购价格 | `purchase_order_items.unit_price` | 与 `reference_price` 不统一 |

### 11.2 TARGET BUSINESS SEMANTICS（目标业务语义）

| 维度 | 目标 | 理由 |
|------|------|------|
| 采购对象 | **Material + Purchasable Role** | 采购对象是 Material 的采购角色 |
| 供应商关系 | **多对多** | 同一物料可以从不同供应商采购 |
| 包装规格 | **Purchasable Config** | 不同包装规格是不同的采购配置 |
| 采购价格 | **Supplier-Specific Price** | 不同供应商可以有不同的采购价格 |

### 11.3 TARGET TECHNICAL MODEL（目标技术模型）

```sql
-- 采购角色配置
CREATE TABLE purchasable_config (
    config_id           BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,        -- 关联 canonical_item
    supplier_id         BIGINT NOT NULL,        -- 关联 suppliers
    purchase_unit       VARCHAR(50) NOT NULL,   -- 采购单位（箱/袋/瓶）
    purchase_spec       VARCHAR(200),           -- 采购规格（330ml×24罐/箱）
    reference_price     BIGINT NOT NULL,        -- 参考价（分）
    min_order_qty       DECIMAL(12,3),          -- 最小起订量
    lead_time_days      INTEGER,                -- 交货周期（天）
    is_primary          BOOLEAN DEFAULT FALSE,  -- 是否主供应商配置
    status              INTEGER DEFAULT 1,      -- 1启用 0停用
    UNIQUE (canonical_item_id, supplier_id, purchase_unit)
);

-- 采购订单（保持现有结构，但引用 canonical_item_id）
CREATE TABLE purchase_order (
    order_id            BIGINT PRIMARY KEY,
    order_no            VARCHAR(50) NOT NULL,
    supplier_id         BIGINT NOT NULL,
    warehouse_id        BIGINT,
    total_amount        DECIMAL(14,2),
    ...
);

-- 采购订单明细（引用 canonical_item_id 和 purchasable_config_id）
CREATE TABLE purchase_order_item (
    item_id             BIGINT PRIMARY KEY,
    order_id            BIGINT NOT NULL,
    canonical_item_id   BIGINT NOT NULL,        -- 关联 canonical_item
    purchasable_config_id BIGINT,               -- 关联 purchasable_config（可选）
    quantity            DECIMAL(12,3) NOT NULL,
    unit_price          BIGINT NOT NULL,        -- 实际采购价（分）
    amount              BIGINT,
    received_quantity   DECIMAL(12,3) DEFAULT 0,
    planned_receiver_type VARCHAR(20),
    planned_store_id    VARCHAR(50),
    planned_warehouse_id BIGINT,
    ...
);
```

---

## 12. 遗留问题与技术债

### 12.1 命名不一致

| 问题 | 位置 | 建议 |
|------|------|------|
| `material_archives` 注释为"商品档案" | 表注释 | 统一为"物料主数据" |
| `purchase_stockin_items.unit_price` vs `inventory.cost_price` | 列名不同 | 入库后 unit_price 应转化为 cost_price |
| `purchase_order_items.material_name` 是冗余字段 | 冗余存储 | 应通过 JOIN 获取 |

### 12.2 缺失的关联

| 缺失 | 影响 | 建议 |
|------|------|------|
| `purchase_order_items` 无 `store_id` 字段 | 直送门店场景无法在订单行上标记门店 | 已通过 `planned_store_id` 解决 |
| `purchase_stockin_items` 无 `store_id` | 直送门店场景无法在入库单上标记门店 | 应补充 store_id 字段 |
| `purchase_return_items` 表结构缺失 | 退货明细未找到独立建表 | 需确认是否在 purchase_return 主表中存储 |

### 12.3 采购与库存的成本一致性

- `purchase_stockin_items.unit_price` (分) 是采购价
- `inventory.cost_price` (分) 是库存成本价
- 入库时应将 `unit_price` 更新为 `cost_price`，但更新规则（先进先出？加权平均？）未定义
- `store_inventory.unit_cost` 来源不明确（是采购价？还是调拨价？）

---

## 13. 推荐改进方向

### 13.1 短期（保持现有结构）

1. **统一 material_id 口径**：确保所有新增采购表使用 `material_id`
2. **清理 inventory.product_id**：后续迁移中添加 material_id 列
3. **补充 purchase_stockin_items.store_id**：支持直送门店场景

### 13.2 中期（规范采购模型）

1. **创建 `purchasable_config` 表**：将 `reference_price`、`supplier_id`、最小起订量等 Purchasable 角色数据统一管理
2. **支持多供应商**：允许同一物料有多个 purchasable_config
3. **定义成本核算规则**：明确 unit_price → cost_price 的转化逻辑

### 13.3 长期（统一采购模型）

1. **引入 Canonical Item**：采购对象引用 canonical_item_id
2. **Purchasable Role**：采购配置作为 Canonical Item 的角色
3. **Supplier Relationship**：多对多的供应商关系管理

---

## 14. 总结

| 问题 | 结论 |
|------|------|
| 采购对象到底是什么？ | **Material + Purchasable Role**，以 material_id 为 canonical identity |
| 一个 Supplier 能否供应同一 Item 的不同 Pack/Unit/Specification？ | **是的**。通过 purchasable_config 支持多供应商、多包装规格 |
| 采购对象和库存对象是否相同？ | **是**。采购和库存共享 material_id，是同一个 Canonical Item 在不同场景中的角色 |
| Purchasable 是什么？ | 是 Material 的一个 **Business Role**，表示"可以从供应商购买" |
| 采购和销售的对象相同吗？ | **否**。采购用 Material，销售用 Food，通过 Recipe/BOM 桥接 |
| 当前模型的最大问题？ | 无法支持多供应商、多包装规格；成本流转规则不明确 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-09
**作者**：AI 架构总控
