# Business Item Canonical — Independent Review

> **任务**: ARCHITECTURE / BUSINESS SEMANTICS MASTER BRIEF — FOR INDEPENDENT SENIOR ARCHITECT REVIEW
> **角色**: INDEPENDENT PRINCIPAL ARCHITECT + DOMAIN MODEL AUDITOR + EVIDENCE CHALLENGER + DECISION QUALITY GATE
> **审查日期**: 2026-09-10
> **代码改动**: 无。所有数据库操作为只读查询。
> **PD-CANONICAL-001 状态**: 保持 `RECOMMENDED / NOT CONFIRMED`（未修改任何 Decision 文件）
>
> **最终 Verdict**: `MAJOR_REVISION_REQUIRED`
>
> **对最终问题的回答（详见 §7）**：**是后者。** 项目**过早地把 Product Definition / Commercial Unit / Inventory Holding 压缩成了一个 Identity** —— 而且这个压缩**不是文档层面的，是数据库层面的**，文档只是没有察觉并把压缩当成了合法业务模型。

---

## 0. 本次审查的方法声明（与上一轮的区别）

本次审查**不复用上一轮独立审查的结论**。所有关键事实重新从三个来源独立取得：

| 来源 | 具体手段 | 用途 |
|---|---|---|
| **运行库实测** | `psql` 只读连接 `food_traceability`，读 `information_schema` / `pg_constraint` / `pg_indexes` / `pg_class` 与实际数据行 | 事实判定 |
| **代码与 DDL** | Flyway 迁移链、Hibernate/MyBatis 实体与 Mapper、Controller、日志文件 | 事实判定 |
| **全仓文本扫描** | 8,140 个文本文件（Java/XML/SQL/TS/Vue/YAML/MD），逐对象分类命中 | 幽灵对象审计 |

**本审查主动推翻了上一轮的两条结论**（见 §1.1 的 CONF-04 与 CONFLICT-01）。审查者自身结论同样受此标准约束。

---

## 1. Evidence Challenges

### 1.1 对上一轮 8 项 Finding 的独立再验证

| # | 上一轮 Finding | 本次独立结论 | 依据 |
|---|---|---|---|
| 1 | Identity 定义存在 Intrinsic vs Physical Entity 本体不一致 | **VALID** | 决策文本内在矛盾，无需外部证据；`PD-CANONICAL-001-Decision.yaml` DS-002 用「内在标识」，DS-003/Invariant 2 用「同一物理实体」 |
| 2 | 可能是三层而非一层 | **VALID（且比原结论更强）** | 见 §2：三层不仅可能存在，而且**已被数据库物理实现**，且**实现方式是错的** |
| 3 | `inventory` 双口径 | **VALID 且升级为 CRITICAL** | 实测两列指向**语义无关的不同物品**（`inventory_id=4`：`material_id=1`=娃娃菜，`product_id=1`=测试物料A） |
| 4 | `inventory.current_stock` 可能是 integer；`store_inventory` 可能是 numeric(15,3) | **VALID（确认）** | 实测 `inventory.current_stock = integer(32,0)`；`store_inventory.current_stock = numeric(15,3)` |
| 5 | `inventory` 可能存在重复（同 material_id + warehouse_id） | **❌ INVALID — 本审查推翻了自己的上一轮结论** | 见 §1.2 CONF-04 |
| 6 | `expiry_date` 无实际填充但预警逻辑在用 | **VALID（确认）** | 实测 `expiry_date` 填充 **0/11**；`production_date` 亦 0/11 |
| 7 | `purchase_request_item.food_id` 实际保存 material ID，转换失败静默写 0 | **VALID（代码确认）** | `PurchaseRequestServiceImpl.java:417-426`；种子数据 `food_id=1 → 娃娃菜`，而 `foods.food_id=1` 是「生菜串」 |
| 8 | 引用了若干不存在的对象与文档 | **VALID 且已分类** | 见 §1.3 幽灵对象台账：**完全不存在 4 个 / 半存在 2 个 / 概念不存在 3 个**；另有 1 个被本审查**更正为存在**（`in_transit`） |

### 1.2 证据重分类（Evidence Reclassification）

对上一轮审查中所有 `VERIFIED` / `CONFIRMED` 级判定逐条重分类。**禁止为维持旧文档一致性而保留 VERIFIED。**

| ID | 原判定 | 重分类 | 理由与可复现方法 |
|---|---|---|---|
| CONF-01 | `inventory` 以 `material_id` 管理库存 — VERIFIED | **CONFLICT** | 实测两列并存且填充 1/11（product）vs 11/11（material）；两列指向**不同物品**。复现：`SELECT i.material_id, ma.material_name, i.product_id, p.name FROM inventory i LEFT JOIN material_archives ma ON ma.material_id=i.material_id LEFT JOIN product p ON p.product_id=i.product_id WHERE i.inventory_id=4;` |
| CONF-02 | `store_inventory` 以 `store_id + material_id` 管理门店库存 — VERIFIED | **PARTIAL** | 列与唯一键成立（`uk_store_inventory_store_material`），但 `store_id` **VARCHAR** 而 `inventory.store_id` 是 **BIGINT**，且实测门店键由仓库键填充（`store_inventory_log` 中同时出现 store_id=1 与 584（仓库ID））。复现：`\d store_inventory` 与 `SELECT DISTINCT store_id FROM store_inventory_log;` |
| CONF-03 | `dish_recipe` 连接 food 和 material — VERIFIED | **INVALID** | 真实表 `dish_recipe` 列为 `dish_id` + `material_id`（无 `food_id`）；`food_id + material_id` 属于**另一张表** `dish_recipes`。且 `dish_recipe` 实测 **0 行**，`dish_recipes` 5 行中 4 行 `deleted=1`。复现：`\d dish_recipe` / `SELECT * FROM dish_recipes;` |
| CONF-04 | `inventory` 存在同 (material_id, warehouse_id) 重复行 | **❌ INVALID（自我推翻）** | 上一轮把 `inventory_id=4` 与 `5` 判为重复。实测 `inventory_id=4 → warehouse_id=1`，`inventory_id=5 → warehouse_id=2`，**warehouse 不同，不是重复**。分组统计返回空集。复现：`SELECT material_id, warehouse_id, count(*) FROM inventory WHERE deleted=0 GROUP BY 1,2 HAVING count(*)>1;` |
| CONF-05 | `product` 表不是死表 — CONFIRMED | **PARTIAL** | 表**非死**（存在 1 行；`ProductMapper` 被 5 处活跃代码调用），但 `ProductEntityService` 无注入点；且 `product` 表只有 **1 行测试数据**（`测试物料A`）。「非死」成立，「活跃业务主数据」不成立。 |
| CONF-06 | 无完整 Ledger — CONFIRMED | **CONFIRMED（升级为 CRITICAL）** | 实测 `inventory_transactions` 引用了 **6 个不存在的 `inventory_id`**（9,15,16,24,28 等），且**零外键约束**，`transaction_type` 有 2 行 **NULL**。见 §2.4。 |
| CONF-07 | `product_id` 与 `material_id` 取值「恰好一致」— 文档断言 | **INVALID** | 实测反例：`product_id=1`→测试物料A，`material_id=1`→娃娃菜。**取值一致论被实测否证**。 |
| CONF-08 | 金额以分为准（LOCK-A003）— PARTIAL | **PARTIAL（维持）** | `inventory.unit_cost/total_cost` 为 `bigint`（分）；但 `inventory.cost_price/stock_value` 为 `numeric(12,2)`，同一表内两种金额口径并存。 |
| CONF-09 | 多 UOM / Conversion 是已识别业务需求 | **VALID 且加强** | 实测 `inventory.current_stock` 不可为小数（integer + `CHECK current_stock >= 0`），`store_inventory.current_stock` 可为 3 位小数；**同一物料在两个空间可用不同精度表达**，是无换算能力造成的直接后果。 |
| CONF-10 | Transit 是 Inventory State | **INVALID** | 原结论既无证据、论证前提亦错误；且**存在被遗漏的反证**：`purchase_arrivals.shipment_status` 是**持久化的** `in_transit` 状态列（见 §1.3 GH-08）。 |
| CONF-11 | `inventory_unit` 表已存在但未被引用 | **INVALID（半存在）** | 全仓 0 个 DDL；但**曾真实存在**——2026-06-30 日志记录了成功的 `SELECT ... FROM inventory_unit`（见 §1.3 GH-05）。正确表述是「表在当前 schema 中不存在，其 API 端点已失效」。 |
| CONF-12 | `INGREDIENT = Recipe ↔ Business Item Relationship`（RECONCILED） | **INVALID** | 4 套并行配方表示，且其中 2 张表实测为 **0 行**；真实在用的是第 3 张（`dish_recipes`）。 |
| CONF-13 | `CONSUMABLE = Capability` | **UNVERIFIED** | 系统无任何 stockable/consumable 标记列（见 §1.3 GH-07）；三种消耗场景只实现一种。 |
| CONF-14 | 06 语料 registry 16/16 CONFIRMED | **CONFLICT** | 其命名对象大面积不存在；且其采用的产品层级模型被**同语料** `boundary` 文件明确否决。 |

### 1.3 Ghost Object Audit（全量扫描结果）

**扫描范围**：8,140 个文本文件（`.java/.xml/.sql/.ts/.tsx/.vue/.js/.yml/.yaml/.json/.properties/.md/.html`），排除 `node_modules/.git/target/venvs/SDK` 等。

| Ghost 对象 | DB | DDL/Migration | Java Entity | Mapper/代码 | Frontend | 文档 | **裁定** |
|---|---|---|---|---|---|---|---|
| `stock_item` | ✗ | ✗ | ✗ | ✗ | ✗ | 10 | **完全不存在**（纯文档构造） |
| `inventory_item` | ✗ | ✗ | ✗ | ✗ | ✗ | 6 | **完全不存在** |
| `inventory_ledger` | ✗ | ✗ | ✗ | ✗ | ✗ | 8 | **完全不存在** |
| `inventory_balance` | ✗ | ✗ | ✗ | ✗ | ✗ | 5 | **完全不存在** |
| `inventory_unit` (GH-05) | ✗ | **✗** | **✓** | ✓（Mapper 全套） | ✗ | 17 | **半存在**：Entity+Mapper+Service+Controller 全套存在，**DB 表不存在，DDL 从未存在**；但 2026-06-30 日志证明**曾真实可用** |
| `inventory_category` (GH-06) | ✗ | **✗** | **✓** | ✓ | ✗ | 4 | **半存在**（同上） |
| `location.transit` | ✗ | ✗ | ✗ | ✗ | ✗ | — | **不存在**（无 `location` 表；`inventory_transfers` 无在途状态） |
| `is_food` (GH-07) | ✗ | ✗ | ✗ | ✗ | ✗ | 5 | **完全不存在** |
| `in_transit` (GH-08) | ✓（列） | **✓** | ✓ | ✓ | ✓（9 处） | 15 | **更正为存在**：`purchase_arrivals.shipment_status` 为持久化 VARCHAR 列，取值含 `in_transit`，含 `logistics_no`/`logistics_company` |
| `unit_conversion` / `sku_code` / `is_stockable` / `product_batches` | ✗ | ✗ | ✗ | ✗ | ✗ | 少量 | **完全不存在**（`sku/uom/conversion` 类列全库 0 个） |

**关键补充证据（GH-05/GH-06 的运行时缺陷）**

```
backend/logs/food-traceability.2026-06-30.0.json
  c.f.m.I.selectList_mpCount:  Preparing: SELECT COUNT(*) AS total FROM inventory_unit
  c.f.m.InventoryUnitMapper.selectList: Preparing: SELECT id,name,code,type,description,status,created_at,updated_at FROM inventory_unit
```

- 当前库：`to_regclass('public.inventory_unit')` → **NULL**。
- `InventoryUnitController` 挂载 `/v1/inventory/unit`，暴露 `POST`、`GET /{id}`、`GET`、`GET /all`、`GET /type/{type}` 共 6 个端点。
- `InventoryCategoryController` 同构，挂载 `/v1/inventory/category`，6 个端点。
- 两套端点的**前端调用方数量 = 0**（`frontend/src`、`frontend-pos/src`、`frontend-kitchen/src` 全无引用）。

**裁定**：这不是「文档想象了一张表」，而是**一对已上线但已失效的 API 端点**：契约存在、实现存在、表不存在。任何第三方集成方调用 `/v1/inventory/unit` 都会得到 500。此事实同时推翻了「`inventory_unit` 是孤立 Foundation 表」的表述——**它不是 Foundation，它是遗留残骸**。

---

## 2. Ontology Findings

> **强制三层测试与身份测试的实测结果。本节所有断言均可由 §8 的复现命令重现。**

### 2.1 观察到的真实对象图谱

| 空间 | 载体 | 主键 | 实测行数 | 与其它空间的关系 |
|---|---|---|---|---|
| 可售成品 | `foods` | `food_id` BIGINT | **1**（生菜串） | 与 material 无任何 FK |
| 可售套餐 | `dish_combos` | `combo_id` BIGINT | **1**（生菜套餐） | 经 `combo_ingredients.food_id` 指向 foods |
| 物料定义 | `material_archives` | `material_id` BIGINT | **35** | **本 DB 的事实锚点** |
| 遗留产品 | `product` | `product_id` BIGINT | **1**（测试物料A） | **全库唯一物理 FK 指向它** |
| 仓库持仓 | `inventory` | `inventory_id` BIGINT | **11** | `product_id`(有FK) 与 `material_id`(无FK) 并列 |
| 门店持仓 | `store_inventory` | `id` BIGINT | **16** | 仅 `material_id`（NOT NULL + 唯一键） |
| 追溯/批次 | `material_trace_code` 等 | 追溯码 | 活跃 | 批次与效期在此空间 |

### 2.2 三层测试的实测结论（不是推演）

**测试对象取自本数据库真实数据**（因下列对象在本系统**不存在**：Coca-Cola、鸡翅、宫保鸡丁、大米、食用油、餐盒、清洁剂、配送费、包装饮料——实测 0 命中）。

| 层 | 定义 | 本 DB 实测载体 | 是否已被物理实现 |
|---|---|---|---|
| **L1 Product Definition** | 这是什么 | `material_archives`(35) / `foods`(1) / `product`(1) —— **三个互不引用的定义空间** | 实现为**三个并列空间**，非一层 |
| **L2 Commercial Unit** | 以什么单位交易 | `foods`(sale_price+specification+stock) / `dish_combos`(combo_price) / `product`(price) | **无独立层**；被折叠进各定义表 |
| **L3 Inventory Holding** | 在哪、持有什么、多少 | `inventory`(11) / `store_inventory`(16) | **实现为两张互不约束的余额表** |

**强制测试结果表**

| 测试对象 | Business Identity | Commercial Unit | Inventory Holding | UOM/Packaging/Batch |
|---|---|---|---|---|
| 生菜（material_id=3） | L1：`material_archives#3` | 无独立载体（仅 `unit='斤'`） | 仓库：无行；门店：50.000 斤 | unit=斤；batch 无 |
| 生菜串（food_id=1） | L1：`foods#1`（**与生菜无关联**） | 载体=`foods`（sale_price=80, unit='份'） | 无（`foods.stock=99` 是**第二个余额**） | 规格列空 |
| 生菜套餐（combo_id=1） | L1：`dish_combos#1` | 载体=`dish_combos`（combo_price=1350） | 无 | `combo_ingredients` 指向 food_id=1 |
| 娃娃菜（material_id=1） | L1：`material_archives#1` | 无 | 仓库 101（wh=1:70 + wh=2:30）；门店 41.000 | 仓库行 4 带 batch=BATCH-WH-001 |
| 圆形打包盒 / 长方形打包盒（17/18） | **两个不同 Identity**（按现行 DS-005） | 均为 `unit=套` | 无持仓行 | spec=500ml vs 750ml（**形状差异被写成规格**） |
| 商用电磁炉（20） | L1：`material_archives#20` | 无 | 门店 1.000 台；仓库无 | unit=台；spec=3500W |
| 二硫化硒洗剂（35） | L1：`material_archives#35` | 无 | 仓库 0；门店 20.000 瓶 | unit=瓶 |
| Warehouse batch | — | — | `inventory` 行自带单值 `batch_no`（4/11 填充） | 批次挂在**余额行**上 |
| Store stock | — | — | `store_inventory`（**无 batch / expiry / location 列**） | — |
| Open bottle | **不存在** | — | — | 全库无 `packaging_state`/`open_quantity` 类列 |

### 2.3 本体层逐项裁定

| 层 | 裁定 | 硬证据 |
|---|---|---|
| **Product Definition** | **存在，但是三份**。`foods` / `material_archives` / `product` 三者之间**没有任何映射表、没有任何 FK、没有任何代码桥接**（唯一的桥是 `Long.parseLong` 与 `String.valueOf`） | §2.1；`PurchaseRequestServiceImpl.java:417-426` |
| **Commercial Unit** | **不存在为独立概念**。商业属性（价格/规格/状态）被直接内嵌到各定义表中：`foods.sale_price`、`dish_combos.combo_price`、`product.price`、`material_archives.reference_price`。计价键是 `(product_type, product_id)`，`product_type ∈ {FOOD, COMBO}` | `PricingServiceImpl.java:152-158`；`product_pricing_history` DDL |
| **Inventory Holding** | **存在，但是两份且不一致**。见 §2.4 | §2.4 |
| **Location** | **概念混乱**。`warehouse_id` 是唯一真实位置维；`store_id` 在 `inventory` 中是 BIGINT 且**恒为 NULL**（实测 0/11 填充），在 `store_inventory` 中是 VARCHAR 且**被仓库键填充**（实测 store_id 出现 1 与 584）；`location_id` 实测 0/11 填充且无 `location` 主表 | §1.2 CONF-02；实测 |
| **UOM** | **无 Foundation、无换算**。全库 `unit` 为自由文本 VARCHAR（宽度 20/32/50 不一致，可空）；`~/^(sku|uom)|conversion\|is_food\|stockable/` 列扫描**仅命中 `orders.packaging_fee`** | 实测 §8-Q6 |
| **Packaging** | **无独立概念**。包装与形状被写成 `spec` 文本（`500ml`/`750ml`/`85g`/`10kg/box`/`含勺`） | §2.2 |
| **SKU / Barcode** | **无 SKU**（0 列）。`barcode` 仅存在于 `material_archives`（单值、**无唯一约束**）、`product`、`material_template`；`foods` **无条码列** | 实测 |
| **Batch / Lot / Expiry** | **两套事实且其中一套空转**。`inventory.batch_no` 4/11 填充、`expiry_date` **0/11**；追溯码空间另有一套完整批次+效期；而效期预警逻辑读的是**空的那一套** | §1.2 CONF-06 |
| **Capability** | **无实现**。无 `is_stockable` / `is_food` / capability 类列或表 | GH-07 |
| **Role** | **无实现**。Role 仅存在于文档与 `role_type` 提案中 | GH 全表 |
| **Profile** | **无实现**。所有 Profile 性属性内嵌于定义表与余额行 | §2.2 |

### 2.4 Inventory Holding 的真实本体（本节为本次审查最重要的新发现）

**(a) 两张余额表对同一物料给出不同数量，且互不约束**

| material_id | 仓库 `inventory` | 门店 `store_inventory` | 是否一致 |
|---|---|---|---|
| 1 娃娃菜 | **101** 斤（wh=1:70 + wh=2:30） | **71.000** 斤 | ✗ |
| 6 撒尿牛丸 | 2 | 2.000 袋 | 值同（单位不同） |
| 10 虾滑 | 40 | 70.000 斤 | ✗ |
| 11 蟹棒 | 30 | 30.000 斤 | ≈ |
| 21 不锈钢汤桶 | **0** 个 | **2.000** 个 | ✗ |
| 26 Test Tomato | 100 jin | 100.000 jin | ≈ |
| 35 二硫化硒洗剂 | **0** 瓶 | **20.000** 瓶 | ✗ |

**(b) 两个空间大面积不相交**：仓库持有 9 个物料，门店持有 14 个，**仅 7 个共有**；门店独有的 7 个物料（菠菜/生菜/土豆片/金针菇/方便面/红薯粉/商用电磁炉）**在仓库表中完全没有行**。

**(c) 台账无法重建余额**（直接否证 Ledger-first）

- `inventory_transactions` 引用的 `inventory_id` = `{4,6,9,12,13,14,15,16,24,26,28,31,32,33,35}`
- `inventory` 实际存在的 `inventory_id` = `{4,5,6,13,14,30,31,32,33,34,35}`
- **引用不存在的 inventory_id：9, 12, 15, 16, 24, 26, 28**（7 个）
- **存在但无流水的 inventory_id：5, 30, 34**（3 个）
- 同一 `inventory_id=12` 被 2 行流水引用且 `before_qty` 不衔接（20→35，然后 35→50）
- **`inventory_transactions` 无任何外键约束**（`pg_constraint` 对该表返回空）
- 2 行 `transaction_type IS NULL`（均为 `adjust`）

**(d) `store_inventory_log` 的身份口径错位**

- 16 行日志全部把**物料 ID 写入 `product_id` 列**（如 `product_id=10` → 名称为「虾滑」，而 `material_archives#10` = 虾滑）
- `store_id` 列同时出现 `1` 与 `584`（584 是 `purchase_arrival` 的仓库 ID）
- `store_inventory_log` **同样无任何外键**

**裁定**：Inventory Holding 在本系统中**不是「一个持仓层」**，而是**两个各自为政、对同一物料给出不同数字、且都无法从台账重建的余额空间**。任何以单一 I3 层为前提的模型（含 Option D）都**尚未被工程现实支持**——但这是**实现缺陷**，不是模型缺陷（见 §3）。

---

## 3. Identity Models

### 3.1 Option A — Type-specific Identity
**定义**：每个类型（Food / Material / Ingredient）独立定义 Identity，无统一层。

**实测支持**：这是**当前工程现实最接近的模型**。系统实际就是 `foods` / `material_archives` / `product` 三个独立空间，且**它们之间确实没有任何引用**。

**实测反对**：
- `purchase_request_item.food_id`（VARCHAR）实际存**物料 ID**，转单时 `Long.parseLong` → 失败静默写 `0L`；
- `OrderTimeoutTask.java:121-124` 把数值 `food_id` 当 `food_code` 使用；
- `OtherInboundService.java:58,108` 从 `product` 读、往 `inventory.material_id` 写。

**裁定**：A 描述现状**准确**，但现状**已被证明会产生可观测的数据错误**。A 不是可推荐的**目标**，但「A 完全不成立」的说法也**不成立**——它是有效的**现状描述**。

### 3.2 Option B — Unified Canonical Business Identity
**定义**：引入统一 Canonical Business Identity 作为业务语义层。

**支持**：跨模块串联的需求真实存在（错误注释 §3.1 所列）。

**反对（实测）**：
1. 若要解释本 DB，B 必须**额外引入**：`product_type` 判别式（已存在）、包装/集合数量语义（不存在）、库存单位语义（不存在）、批次层（半存在）、位置层（半存在）。**即 B 的原始单层定义必须增生至少 4 个额外身份维度才能解释现有数据** —— 这正是 brief §17 所问的情形，答案是**是，B 的原始定义过于简单**。
2. **B 无法解释本 DB 中最重要的一行**：`inventory_id=4` 同时携带 `material_id=1`(娃娃菜) 与 `product_id=1`(测试物料A)。在 B 之下这两个引用**应当**指向同一个 Canonical Identity；实测它们指向**语义无关的两个物品**。B 没有机制发现或防止这种冲突——因为 B 假定该层已存在。
3. B 不解决 §2.4 的任何一条实测缺陷。

**裁定**：**NOT_CONFIRMABLE_AS_STATED**（与上一轮一致，但理由已换成实测证据而非文本矛盾）。

### 3.3 Option C — Hybrid
**定义**：部分类型共享 Canonical Identity，部分保持独立。

**实测支持**：本系统的**真实分层**正是混合的——`foods`/`dish_combos`（销售侧，已用 `product_type` 判别式）具备商业层特征；`material_archives` 同时承担定义与采购配置；`store_inventory` 是纯持仓。

**实测反对**：C 的边界条件仍未定义，因此在工程上不可判定。

**裁定**：**UNDERSPECIFIED**（既非成立亦非不成立；C 缺乏可判定的边界定义）。

### 3.4 Option D — Layered Identity（Definition → Commercial Unit → Inventory Holding）

**必须诚实说明的定位**：Option D 是**上一轮审查提出的方案**，因此本次审查把它当作**被审对象**，而不是推荐。

**十项解释义务的逐条审计**（brief §16 强制）

| # | 义务 | 实测结果 | 判定 |
|---|---|---|---|
| 1 | 是否真正存在于工程现实 | **否**。L1 有三份真实载体；**L2 完全无载体**；L3 有两份载体但互不约束。D 的三层中**只有 L1 与 L3 在物理上存在，L2 不存在** | **部分失败** |
| 2 | 能否解释当前 DB | **部分**。D 能**描述**现状（这正是它的价值），但**不能解释**为何同一物料两个余额不同、为何 7 个门店物料无仓库行、为何台账引用不存在的 id | **部分成功** |
| 3 | 能否解释 POS | **能**。`product_type ∈ {FOOD, COMBO}` 恰好是 L2 的判别式，D 将其显式化 | **成功** |
| 4 | 能否解释 Procurement | **能**。采购链以 `material_id` 为键，D 归入 L1 | **成功** |
| 5 | 能否解释 Inventory | **不能**。D 规定「I3 数量必须能以 I2/I1 单位表达」，但**系统无换算**，且 `store_inventory`/`inventory` 精度不同（integer vs numeric(15,3)）。D 描述了一个**当前无法满足的约束** | **失败** |
| 6 | 能否解释 Recipe | **部分**。`dish_recipes`（5 行，4 行软删）与 `combo_ingredients`（2 行）可归入 L1↔L1 关系；但 `dish_recipe` 实测 0 行而代码仍在查它，D 不解释该矛盾 | **部分成功** |
| 7 | 能否解释 Pricing | **能**。`(product_type, product_id)` 是 L2 键，D 将其显式化 | **成功** |
| 8 | 能否解释 SKU | **不能**。系统无 SKU。D 把 SKU 列为 I2 边界属性，但**不存在可被解释的对象** | **N/A → 未验证** |
| 9 | 能否解释 Barcode | **部分**。`material_archives.barcode` 单值且无唯一约束；D 说「每包装层级一码」，系统不支持多层码 | **部分成功** |
| 10 | 能否解释 Batch | **不能**。批量事实分裂（`inventory.batch_no` 4/11 + 追溯码空间 + `store_inventory` 无批次列），且 `expiry_date` 空转。D 把批次放入 I3，但**未说明为何系统会有两套批次事实** | **失败** |

**D 的致命弱点（本次审查新发现，上一轮未指出）**

1. **L2 在工程上完全不存在**，D 要求把它建出来。这是一个**重大架构投资**，而 brief 明确要求「不要因为 Option D 是新方案就自动推荐」。
2. **D 未能诊断真正的故障点**。真正的故障不是「缺一层」，而是 **`inventory` 行同时携带两个不同命名空间的外键**（`inventory_id=4`）。D 假设 `product_id`↔`material_id` 的对应关系可用，**但该对应关系在本 DB 中已被实测否证**。**D 没有回答「如果两个命名空间对同一行给出不同物品，哪个是真相」**。
3. **D 不解释最严重的数据缺陷**：两张余额表不一致、7 个台账引用不存在的 id、门店键被仓库键填充。这些是**实现与治理缺陷**，加一层身份模型不会自动修复。
4. **D 的迁移成本被上一轮低估**：它要求为 35 个物料 + 1 个菜品 + 1 个套餐建立 definition_key，并统一两张余额表的口径与精度——而 `material_archives` 中存在 **7 行 Test Tomato、2 行 E2E Potato** 等重复定义，**去重决策先于分层决策**。

**裁定**：**NOT_CONFIRMED — 需要修订**。D 是**目前对现状描述力最强的假设**，但它 (a) 依赖一个不存在的层，(b) 未解决命名空间冲突的真问题，(c) 不能解释 3 项实测缺陷。**D 不得在 L2 的工程可行性与命名空间冲突的裁决方式明确之前被推荐。**

### 3.5 候选模型对比（brief §4 指定的七个维度）

评分口径：`强 / 中 / 弱 / 未验证`。**未验证 = 本审查无法用实测证据判定**。

| 维度 | A Type-specific | B Unified | C Hybrid | **D Layered** |
|---|---|---|---|---|
| **Correctness** | 中（描述现状准确，但现状已被证明出错） | 弱（须增生 ≥4 维度；无法解释 `inventory_id=4`） | 未验证（边界未定义） | **中**（描述力最强，但依赖不存在的 L2，且未解决命名空间冲突） |
| **Business Fit** | 弱（无法支撑集团化/多终端） | 中（方向对，层次不足） | 未验证 | **中强** |
| **Evidence Fit** | **强**（与 DB 完全一致） | 弱 | 弱 | 中（与 L1/L3 一致，与 L2 不一致） |
| **Future Scalability** | 弱 | 中 | 未验证 | **强** |
| **Cross-Domain Fit** | 弱（跨域桥靠 `parseLong`） | 中 | 未验证 | 中强 |
| **Migration Risk** | 低（不动） | 高（须合并三个主数据空间，含去重裁决） | 中 | **中高**（须新建 L2 + 统一两余额表 + 去重） |
| **Model Simplicity** | 中（简单但错误） | **强**（概念最少） | 弱 | 中（三层 + 显式映射） |

**结论**：**没有任何选项在全部七个维度上占优。** A 证据拟合最好但业务上不可接受；B 最简单但解释力最弱；D 描述力最强但依赖未实现的层且迁移风险中高。**这正是本次审查判定为 `MAJOR_REVISION_REQUIRED` 而非「推荐 D」的原因。**

---

## 4. Decision Boundary（brief §5 强制）

| 决策 | 层级 | 归属 | 理由 |
|---|---|---|---|
| **命名空间冲突裁决**：`inventory.product_id` 与 `material_id` 何者为真、是否保留双列 | **PRODUCT DECISION** | Product Owner | 涉及「同一行出现两个不同物品时哪个是业务真相」——这是业务语义问题，不是技术问题 |
| **Identity 是否分层**（单层 / 三层 / 按类型） | **PRODUCT DECISION** | Product Owner | 决定「以箱采购以罐核算」是否为业务要求 |
| **批次/效期是否构成持仓身份** | **PRODUCT DECISION** | Product Owner | 涉及食品安全合规责任归属 |
| **Commercial Unit 是否为独立概念** | **PRODUCT DECISION** | Product Owner | 涉及渠道/定价/促销策略 |
| **仓库持仓与门店持仓是否为同一持仓的两种视图** | **PRODUCT DECISION** | Product Owner | 决定是否允许两者不一致（实测已不一致） |
| **`material_archives` 重复定义处置**（7 行 Test Tomato、2 行 E2E Potato、2 行打包盒） | **PRODUCT DECISION** | Product Owner | 去重即业务合并决策 |
| **Product Definition 空间的收敛方式**（`foods`/`material_archives`/`product` 三选一或建映射） | **ARCHITECTURE DECISION** | Architecture Owner | 存储与引用结构 |
| **L2 的存储形态**（若采纳 D） | **ARCHITECTURE DECISION** | Architecture Owner | 新表 vs JSON vs 复用 `product` |
| **两个余额表的收敛与精度统一** | **ARCHITECTURE DECISION** | Architecture Owner | 含 integer→numeric 迁移 |
| **台账的外键与不可变性** | **ARCHITECTURE DECISION** | Architecture Owner | 含清理 7 个孤儿 inventory_id |
| **`store_id` 类型与来源统一**（VARCHAR vs BIGINT；仓库键冒充门店键） | **ARCHITECTURE DECISION** | Architecture Officer | 涉及 `stores` 双表 |
| **失效 API 端点处置**（`/v1/inventory/unit`、`/v1/inventory/category`） | **ENGINEERING DECISION** | 工程团队 | 建表或下线 |
| **`dish_recipes` 唯一键与 BOM 去重** | **ENGINEERING DECISION** | 工程团队 | 加唯一键、清理软删行 |
| **快照视图的 `version=0 AND deleted=0` 双重过滤** | **ENGINEERING DECISION** | 工程团队 | 消除首更新即消失的语义重载 |
| **金额口径统一**（`unit_cost` 分 vs `cost_price` 元） | **ENGINEERING DECISION** | 工程团队 | 表内两种口径并存 |

---

## 5. Blocking Evidence（必须重新验证的事实清单）

> 以下事实**在解决前，任何 Identity 模型决策都缺乏充分事实基础**。每项给出复现方法。

| ID | 必须验证的事实 | 为什么阻断 | 复现命令 |
|---|---|---|---|
| **BE-01** | `inventory` 中 `product_id` 与 `material_id` 在**生产库**的对应关系（本 DB 已否证对应） | 决定命名空间冲突的裁决方式 | `SELECT count(*) FILTER (WHERE product_id IS NOT NULL), count(*) FILTER (WHERE material_id IS NOT NULL), count(*) FILTER (WHERE material_id IS NOT NULL AND product_id IS NOT NULL AND material_id<>product_id) FROM inventory;` |
| **BE-02** | 生产库 `inventory` 是否存在同 (material_id, warehouse_id[, batch_no]) 重复 | 决定是否需要唯一键与批次维度 | `SELECT material_id,warehouse_id,batch_no,count(*) FROM inventory GROUP BY 1,2,3 HAVING count(*)>1;` |
| **BE-03** | `inventory` 与 `store_inventory` 对同一 material 的差异是否在生产中同样存在 | 决定「两张余额表是否为同一持仓」 | §2.4(a) 的 FULL OUTER JOIN 查询 |
| **BE-04** | `inventory` 中 `store_id`/`location_id` 在生产库是否有填充（本 DB 全为 NULL） | 决定 Location 是否为持仓维度 | `SELECT count(store_id), count(location_id) FROM inventory;` |
| **BE-05** | 生产库 `expiry_date` 的填充率（本 DB 0/11） | 决定效期逻辑是否为死代码 | `SELECT count(*), count(expiry_date), count(batch_no) FROM inventory;` |
| **BE-06** | 生产库 `inventory_transactions` 的孤儿 `inventory_id` 比例 | 决定台账能否作为真相源 | §2.4(c) 的两个 id 集合对比查询 |
| **BE-07** | `material_archives` 在生产库的重复度（同物多名/多名一物） | 决定去重规模 | `SELECT material_name, count(*) FROM material_archives GROUP BY 1 HAVING count(*)>1;` |
| **BE-08** | 生产库 `purchase_request_item.food_id` 的非法值（无法 parseLong、或解析后不存在于 material_archives）比例 | 决定跨命名空间桥的实际破损率 | `SELECT count(*) FROM purchase_request_item pri WHERE NOT EXISTS (SELECT 1 FROM material_archives ma WHERE ma.material_id::text = pri.food_id);` |
| **BE-09** | 生产库是否存在调用 `/v1/inventory/unit` 或 `/v1/inventory/category` 的客户端 | 决定失效端点处置 | 网关访问日志 |
| **BE-10** | 生产库金额列的实际口径分布（`unit_cost` 分 vs `cost_price` 元） | 决定 `LOCK-A003` 是否真成立 | `SELECT count(*) FILTER (WHERE unit_cost > 100000), count(*) FILTER (WHERE cost_price > 1000) FROM inventory;` |
| **BE-11** | `dish_recipes` 唯一约束缺失下的重复规模 | 决定 BOM 是否需要去重决策 | `SELECT food_id, material_id, count(*) FROM dish_recipes WHERE deleted=0 GROUP BY 1,2 HAVING count(*)>1;` |
| **BE-12** | 快照/前端是否真的过滤 `version=0` | 决定「首更新即消失」是否已发生 | 查 Flutter/SQL 视图定义 |

**关键提示**：`docs/architecture/03-review/production-data-snapshot-001.md:6` 已明确写入「Agent 不得在开发环境伪造生产结果」，且其 5 个生产扫描脚本**全部仍为「⏸ 待执行」**。**因此 BE-01 ~ BE-12 全部处于未验证状态**，这是 `MAJOR_REVISION_REQUIRED` 的直接依据之一。

---

## 6. Counterexamples（brief §19 强制的 11 个对象）

**首先必须记录的事实**：brief 指定的 11 个对象中，**10 个在本系统不存在**。实测搜索（对 `material_archives` / `foods` / `dish_combos` 三个主数据表按名称匹配）：可乐 0、鸡翅 0、宫保鸡丁 0、大米 0、食用油 0、餐盒 0、清洁剂 0、Service Item 0、Packaged Beverage 0。仅「Combo Meal」存在（`dish_combos#1 生菜套餐`）。

**因此对这些对象的一切「验证」都是构造的，不构成证据。** 下表区分「真实存在的替代对象」与「必须以真实数据替换的对象」。

| # | brief 指定对象 | 本系统是否存在 | 替代的真实对象 | 独立裁定 |
|---|---|---|---|---|
| 1 | Coca-Cola | **否** | （无饮料类物料） | **UNVERIFIABLE** —— 需先导入真实数据 |
| 2 | Chicken Wings | **否** | 撒尿牛丸(6)/鱼丸(7)/肥牛卷(9) | **UNVERIFIABLE** |
| 3 | Kung Pao Chicken | **否** | 生菜串(1) 为唯一成品 | **UNVERIFIABLE** |
| 4 | Rice | **否** | 红薯粉(13) 为最近似干制主食 | **UNVERIFIABLE** |
| 5 | Cooking Oil | **否** | 辣椒油(16)/芝麻酱(15) | **UNVERIFIABLE** |
| 6 | Takeaway Box | **否**（"餐盒" 0 命中） | **圆形打包盒(17) / 长方形打包盒(18)** —— 真实存在 | **VERIFIED**：二者**形状不同 → 现行 DS-005 判为 Different Identity**；但封装形态差异在业务上**不改变物品定义**（同为 500–750ml 食品级打包容器）。**该真实数据对 PD-001 的 `Specification = IDENTITY_BOUNDARY` 构成直接反例** |
| 7 | Cleaning Chemical | **否** | **二硫化硒洗剂(35)** —— 真实存在的药用洗剂 | **VERIFIED**：该系统与食品、设备同表管理，**无任何 stockable / is_food / item_type 标记**（GH-07）。证明系统**无法区分食品与非食品** |
| 8 | Service Item | **否** | （无） | **UNVERIFIABLE** —— 且系统**无 service 标记**，`(product_type, product_id)` 取值域仅 `{FOOD, COMBO}` |
| 9 | Combo Meal | **是** | **生菜套餐(combo_id=1)** | **VERIFIED**：套餐是**独立主数据表 + 独立计价类型**，不是 `foods` 的一种。**PD-CANONICAL-001 的 18 项 Scope 无一条覆盖套餐** |
| 10 | Packaged Beverage | **否** | 方便面(12)（85g/包，预包装食品） | **部分可测**：其为普通 `material_archives` 行，**无独立包装层级模型** → 证明包装被折叠为 `spec` 文本 |
| 11 | Coca-Cola 330ml 单罐 vs ×24 整箱 | **否** | （无同物多包装行） | **UNVERIFIABLE** —— **本系统没有任何一个物料拥有多个包装层级的记录**，这本身就是 Option B/D 均未被工程现实支持的直接证据 |

**反例测试的核心结论**：

1. **对 DS-004/DS-005（Brand + Product Family + Specification）**：`material_archives` **18 列中无 brand、无 product_family**（全库仅 `asset_master`/`material_template` 有 brand）。**该规则的两个核心属性在物品主数据上不存在** → 规则在本项目**不可实施**。
2. **对 DS-005（Specification 是 Identity 边界）**：真实数据 `圆形打包盒(500ml)` vs `长方形打包盒(750ml)` 表明——**形状与容量混在同一个 `spec` 文本列里**。若形状是身份边界，则同一物品的容量升级会变身份；若形状不是边界，则系统当前的 `spec` 单列**无法表达这个区分**。**两条路都通向「单列 spec 不足以承担身份判定」**。
3. **对 Invariant 1（多能力共存）**：本系统**无任何 capability 标记**，因此「多能力共存」在实现上既非真亦非假，而是**未建模**。
4. **对 Invariant 3（层级不得压缩）**：**被真实数据直接违反**——`inventory_id=4` 一行同时持有两个命名空间的外键，且指向不同物品。

---

## 7. Final Answer to the Core Question

> **「我们之前到底是正确地建立了一个统一 Business Identity 模型，还是为了消除多个表之间的混乱，过早把 Product Definition、Commercial Unit 和 Inventory Holding 压缩成了一个 Identity？」**

### 回答：**是后者 —— 而且是比问题描述更严重的一种压缩。**

三条独立成立的证据：

**（一）压缩不是文档的构想，而是数据库的既成事实，且压缩方式错误。**

`inventory_id=4` 一行同时持有：

```
material_id = 1  →  material_archives#1 = 娃娃菜
product_id  = 1  →  product#1           = 测试物料A
```

两个列各自有 FK 约束（`fk_inv_product_id` 指向 `product`；`material_id` **无 FK**），指向**语义无关的两个物品**。这不是「同一物品的两种表示」，而是**两种不同物品被压进同一行**。任何「统一 Identity」假设在此行上直接失败。

**（二）压缩的动机确实是「消除多表混乱」，而不是「业务上确实同一」。**

- 系统有 `foods`、`material_archives`、`product` 三个定义主数据空间，**彼此零引用**；
- 唯一的跨空间连接是 `Long.parseLong(...)`（`PurchaseRequestServiceImpl.java:417-426`），失败时**静默写 `0L`**；
- `OrderTimeoutTask.java:121-124` 把数值 `food_id` 当 `food_code` 用；
- `inventory_log.product_id` 的注释直接写「产品/物料ID」——**两个不同概念被写进同一个列名**。

这些是**为绕过「多空间」而做的临时粘合**，不是业务上确认「它们本来就是一个东西」。

**（三）「统一」这个动作本身没有被工程现实支持。**

- 无统一 ID 列、无映射表、无跨空间 FK；
- 全库**没有任何一个物料拥有多个包装层级**（brief 的 Case 2 在本系统**无实例**）；
- Commercial Unit 层**完全不存在**（价格/规格/状态被内嵌进各定义表）；
- Inventory Holding 分裂为两张**对同一物料给出不同数量**的余额表。

### 但必须同时给出对现有文档的公平裁定

**压缩不是 `PD-CANONICAL-001` 发明的——它早于文档存在于数据库中。** 文档的真正错误不是「制造了压缩」，而是：

1. **把数据库层面的错误压缩误认为合法业务模型**，并据此写出一个二元 Identity 判定矩阵；
2. **把「需要一个身份锚点」（NEED）写成「该身份已存在」（EXISTENCE）**；
3. **在 `inventory_id=4` 这类反例可见的情况下**，仍宣称 `evidence_status: VERIFIED`。

因此：

- **Option B 并未被「证明错误」，而是被证明「其前提在工程现实中不成立」**——B 要求一个真实、无害、可信的统一层，而现状是一个**错误压缩**。在错误压缩之上加一个统一层，只会让冲突合法化。
- **Option D 也未被证明正确**：它**依赖一个不存在层（L2）**，且**未回答「两个命名空间冲突时以谁为准」**（§3.4）。

### 最终 Verdict

```
VERDICT = MAJOR_REVISION_REQUIRED
```

| 备选 | 是否成立 | 理由 |
|---|---|---|
| `PASS` | ❌ | 无一项 Critical Evidence 通过独立验证（BE-01~BE-12 全部未验证） |
| `PASS_WITH_OPEN_DECISIONS` | ❌ | 问题不是「有开放决策」，而是**已记录的 VERIFIED 中有 3 条被实测推翻、1 条为 CONFLICT、多条为 PARTIAL**；且存在命名空间冲突这一未被任何决策覆盖的新问题 |
| **`MAJOR_REVISION_REQUIRED`** | ✅ | 需要修订：证据链、Identity 分层假设、命名空间冲突裁决、以及 §4 中 6 项产品决策 + 6 项架构决策 + 4 项工程决策 |
| `BLOCKED` | ❌ | 并非完全不可推进——BE-01~BE-12 可通过一次生产库只读扫描解除；但**在生产扫描完成前不得确认任何 Identity 模型** |

**PD-CANONICAL-001 状态**：维持 `RECOMMENDED / NOT CONFIRMED`。本审查**未修改任何 Decision 文件**（§9 声明）。

---

## 8. 复现命令（任何 VERIFIED 断言均可独立复现）

```sql
-- Q1 双口径冲突（BE-01）
SELECT i.inventory_id, i.material_id, ma.material_name AS material_side,
       i.product_id, p.name AS product_side
FROM inventory i
LEFT JOIN material_archives ma ON ma.material_id = i.material_id
LEFT JOIN product p ON p.product_id = i.product_id
WHERE i.deleted = 0 ORDER BY i.inventory_id;

-- Q2 精度分裂（CONF-04 相关）
SELECT table_name, column_name, data_type, numeric_precision, numeric_scale
FROM information_schema.columns
WHERE table_name IN ('inventory','store_inventory') AND column_name LIKE '%stock%';

-- Q3 重复检测（CONF-04，返回空集 = 无重复）
SELECT material_id, warehouse_id, batch_no, count(*)
FROM inventory WHERE deleted = 0 GROUP BY 1,2,3 HAVING count(*) > 1;

-- Q4 两余额表差异（BE-03）
SELECT COALESCE(w.material_id, s.material_id) AS material_id,
       w.wh_stock, w.wh_unit, s.st_stock, s.st_unit
FROM (SELECT material_id, sum(current_stock) wh_stock, min(unit) wh_unit
      FROM inventory WHERE deleted=0 GROUP BY material_id) w
FULL OUTER JOIN (SELECT material_id, sum(current_stock) st_stock, min(unit) st_unit
      FROM store_inventory WHERE deleted=0 GROUP BY material_id) s
  ON w.material_id = s.material_id ORDER BY 1;

-- Q5 台账孤儿（BE-06）
SELECT 'ledger' k, string_agg(DISTINCT inventory_id::text, ',') FROM inventory_transactions WHERE inventory_id IS NOT NULL
UNION ALL SELECT 'existing', string_agg(inventory_id::text, ',') FROM inventory WHERE deleted=0;

-- Q6 幽灵对象（GH 全表）
SELECT n, to_regclass('public.'||n)::text FROM (VALUES
 ('stock_item'),('inventory_item'),('inventory_ledger'),('inventory_balance'),
 ('inventory_unit'),('inventory_category'),('location'),('ingredients'),
 ('dish_ingredient'),('unit_conversion'),('units'),('product_batches')) v(n);

-- Q7 SKU / UOM / conversion / stockable 列扫描
SELECT table_name, column_name FROM information_schema.columns
WHERE column_name ~* '^(sku|uom)|conversion|is_food|stockable|is_stock|pack_size|packaging';

-- Q8 brand / product_family 是否存在（DS-004 可实施性）
SELECT table_name, column_name FROM information_schema.columns
WHERE column_name ~* '^(brand|product_family|family|item_type|material_type)';
```

**代码侧复现**

| 断言 | 位置 |
|---|---|
| `parseLong` 静默 0L | `backend/src/main/java/com/foodtraceability/service/impl/PurchaseRequestServiceImpl.java:417-426` |
| `food_id` 当 `food_code` 用 | `backend/src/main/java/com/foodtraceability/task/OrderTimeoutTask.java:121-124` |
| `product` 读 → `material_id` 写 | `backend/src/main/java/com/foodtraceability/service/OtherInboundService.java:58,96-108` |
| `store_inventory_log` 写 `materialId` 进 `product_id` | `backend/src/main/java/com/foodtraceability/service/impl/StoreInventoryServiceImpl.java:264` |
| 仓库键冒充门店键 | `backend/src/main/java/com/foodtraceability/service/impl/InventoryTransferServiceImpl.java:226`（注释原文） |
| `inventory_unit` 曾可用 | `backend/logs/food-traceability.2026-06-30.0.json` |

---

## 9. 声明

1. **未修改任何 Decision 文件**。`04-decision-history/PD-CANONICAL-001-Decision.yaml` 与 `PD-CANONICAL-001-decision-record.md` 保持原状；状态维持 `RECOMMENDED / NOT CONFIRMED`。
2. **未修改任何代码**。所有数据库操作为只读 SELECT。
3. **本审查推翻了自己上一轮的一条结论**（CONF-04，重复行不存在），并更正了一条（`in_transit` 存在）。审查者结论受与被审文档相同的证据标准约束。
4. **未推荐任何 Option**。A/B/C/D 四者各有未通过的维度；本轮结论是**先裁决命名空间冲突与去重，再选模型**。
