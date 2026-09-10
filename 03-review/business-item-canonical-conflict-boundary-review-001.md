# Business Item Canonical — Conflict Boundary Review 001

> **任务性质**: 矛盾边界收口审查（CLOSURE REVIEW）
> **身份**: INDEPENDENT REVIEWER —— 非工程执行者，非架构拍板者
> **日期**: 2026-09-11
> **代码/schema/迁移改动**: 无
> **本地文档路径**: `docs/architecture/`（全部阅读与产出均在本仓库本地完成）
>
> **本文件不做的事**：不提出新架构、不设计数据库、不设计 API、不修改代码、不创建新 Option、不把 Recommendation 写成 Decision。
>
> **本文件做的事**：把已发现的冲突逐项归入 **A / B / C / D** 四类，并对每个 A 类问题做**反证**。

---

## 0. 分类方法与判定规则

| 类别 | 判定规则（必须同时满足） |
|---|---|
| **A — 负责人决策** | ① 工程无法替负责人决定；② 一旦选错会改变业务世界模型（而非仅实现）；③ 存在多个在业务上可辩护的选项 |
| **B — 可直接工程修复** | ① 有可复现的本地/代码证据；② 修复不依赖任何未决业务语义；③ 修复不改动业务含义 |
| **C — 必须先取生产证据** | ① 本地证据不足；② 贸然决定会把环境差异误判为系统事实；③ 证据可从生产库只读取回 |
| **D — 当前判断本身可能错误** | ① 问题当前无法进入决策（前提不明）；② 也无法进入工程（方向不明）；③ 需继续验证 |

**判定纪律**：
- 不得因为「已有 Recommendation」而视为「已有 Decision」。
- 不得把本地证据重新命名为生产证据。
- 一条冲突可**同时**含 A 与 B 成分（如「业务目标待决 + 旁证缺陷可修」），此时**分别登记**，不得合并成一个结论。

---

## 1. 特别检查：Product / Food / Material

### 1.1 当前已经知道什么（事实层，可复现）

| 事实 | 证据等级 | 位置 |
|---|---|---|
| `foods`、`material_archives`、`product` 是三个**互不引用**的主数据空间 | `LIVE_LOCAL_INSTANCE_EVIDENCE` | 三表之间无任何 FK；`pg_constraint` 实测 |
| `material_archives` 18 列中**无 `brand`、无 `product_family`、无 `item_type`** | `LIVE_LOCAL_INSTANCE_EVIDENCE` | `information_schema.columns` 实测 |
| `foods` 21 列中同样无 brand / family | 同上 | 同上 |
| 全库唯一指向 `product` 的物理 FK 是 `inventory.product_id → product.product_id` | `LIVE_LOCAL_INSTANCE_EVIDENCE` | `pg_constraint` 实测 |
| `inventory.material_id` **无任何 FK** | 同上 | 同上 |
| `inventory_id=4` 一行内 `material_id=1`→娃娃菜、`product_id=1`→测试物料A | 同上 | 逐行 JOIN 实测 |
| `product` 表仅 1 行，且为测试数据（`测试物料A`） | 同上 | 实测 |
| `product` 表**存在 5 处活跃调用点**，其中 `PurchaseOrderServiceImpl.java:638` 以 `dto.getMaterialId()` 去查 `product` | `CODE_EVIDENCE` | 源码实测 |
| `purchase_request_item.food_id`（VARCHAR 32）**92% 实为 material ID** | `LIVE_LOCAL_INSTANCE_EVIDENCE` | 46/50 匹配 `material_archives` |

### 1.2 哪些只是假设

| 假设 | 状态 | 为什么仍是假设 |
|---|---|---|
| 「Product 是 Commercial Concept / 商业抽象」 | **未验证** | 由一个测试数据行与一个定价键推断；`product` 表在生产是否有真实数据未知 |
| 「Material 是 Canonical/Current Identity」 | **未验证** | 只在本地库成立（material_id 填充 11/11）；生产填充率未知 |
| 「Food 属于 Commercial Unit 层」 | **未验证** | 由上一条假设推导而来 |
| 「三层模型是正确解」 | **未验证** | Commercial Unit 层在工程中**完全不存在**，属待建而非既有 |
| 「`product` 可直接宣布 dead」 | **已否证** | 5 处活跃调用点存在（本地代码） |

### 1.3 必须由负责人决定 / 可由工程修复 / 不可逆风险

| 问题 | 归类 | 理由 |
|---|---|---|
| 三个主数据空间是否需要统一、以谁为准 | **A** | 见 §2 A1 |
| `product` 概念在业务上是「商业抽象」还是「历史残留」 | **A**（并入 A1） | 决定 5 处调用点的处置方向 |
| `product` 表 5 处调用点的**引号/映射是否写错**（如以 materialId 查 product） | **B** | 见 §2 B7，属可独立修复的缺陷 |
| 现在贸然统一 | **禁止** | 见 §2 A1 反证 §2.A1.4 |

### 1.4 如果现在贸然统一，会造成什么不可逆风险

1. **唯一可用的对应关系不存在。** `product_id` 与 `material_id` 之间没有映射表、没有 FK、没有代码桥接；唯一存在的是一行测试数据，而该行**恰好两列取值相同却指向不同物品**。以此为依据做数据合并，会把错误的对应关系**写入历史**。
2. **`product` 表在生产是否有真实数据未知。** 若生产存在真实 `product` 行（含 `product_id` 被 `inventory.product_id` 外键引用），直接废弃该列会**破坏一个真实外键**，且历史行的物品归属将无法还原。
3. **`purchase_request_item.food_id` 已发生跨命名空间写入。** 统一动作若以「food_id 就是 food」为前提，会把已经写错的 material ID 进一步固化。
4. **合并是数据层不可逆操作**：`material_archives` 与 `product` 的合并会改变所有引用这两张表的历史单据语义，而引用链横跨采购、库存、追溯、食谱。

---

## 2. 逐项分类

> 编号 A1–A7 / B1–B10 / C1–C8 / D1–D6。每条给出：现状事实 → 归类 → 依据。

### A 类 —— 必须由业务/架构负责人决策

#### A1 — Product Definition / Commercial Unit / Inventory Holding **是否分层**

- **现状事实**：`material_id` 填充 11/11、`product_id` 填充 1/11；无映射表；Commercial Unit 层不存在（无 SKU 列、无包装层级、无任一物料含多包装）；两张余额表互不约束。
- **归类**：**A**
- **为什么不是 B**：修复动作需要先知道「同一物品的多个商业单位应以哪个为库存计量基准」——这是业务世界模型问题。
- **为什么不是 C**：生产证据能**降低**不确定性，但不能**替代**决策（即使生产数据干净，仍需负责人选择单层或分层）。
- **为什么不是 D**：问题本身表述清楚，不存在「问题错了」的情形。

#### A2 — `inventory` 的 `product_id` / `material_id` **命名空间冲突的处置方向**

- **现状事实**：两列并存；`material_id` 由 ORM 写（11/11），`product_id` 近乎空转（1/11）；FK 在 `product_id` 上；`Inventory.java:382-388` 把 `getProductId()` 实现为 `materialId` 别名。
- **归类**：**A**（方向）+ **C1**（生产证据）—— 两者不可互相替代
- **为什么是 A 而非纯 C**：「保留 `product_id` 作为物品真相」与「以 `material_id` 为物品真相」是**两种不同的业务主张**，生产数据不能替负责人选。

#### A3 — 仓库持仓与门店持仓是**同一持仓的两个视图**还是**两个独立持仓**

- **现状事实**：仓库 9 个物料、门店 14 个、仅 7 个共有；共有中 4 个数量不一致（娃娃菜 101 vs 71、虾滑 40 vs 70、汤桶 0 vs 2、洗剂 0 vs 20）；两表无同步机制。
- **归类**：**A**（业务语义）+ **C2**（生产规模）
- **为什么是 A**：「两处库存是否应当始终对账」是业务规则，不是技术规则；差异既可能是缺陷，也可能是业务本意。

#### A4 — 批次 / 效期是否构成**持仓身份维度**

- **现状事实**：`inventory.batch_no` 4/11、`expiry_date` 0/11；`store_inventory` 无这些列；追溯码空间另持完整批次与效期；效期预警逻辑读的正是空的那一套。
- **归类**：**A**（是否作为身份维度）+ **C3**（生产填充率）
- **为什么是 A**：涉及食品安全召回责任与库存粒度，属业务/合规主张。

#### A5 — `orders_legacy` / 旧 POS 链路在业务上**是否仍在服役**

- **现状事实**：`OrderMapper.java` 含 **18 处** `orders_legacy` 引用、**11 个** legacy 统计方法；全部**无调用方**；本地表 **0 行**；`Order.java:14` 仍 `@TableName("orders_legacy")`。
- **归类**：**A**（业务是否退役）+ **C7**（生产是否有行）
- **为什么是 A**：是否还有旧 POS 端在写入该表，是业务部署事实，工程无法从本地判定。

#### A6 — `dish_recipe`（单数）与 `dish_recipes`（复数）的**业务归属**

- **现状事实**：两张表并存；`dish_recipe` 0 行且**缺 8 个被实体映射的列**；`dish_recipes` 活跃 1 行（4 行软删）；键空间不同（`dish_id` VARCHAR vs `food_id` BIGINT）。
- **归类**：**A**（应保留哪一套 / `dish_id`↔`food_id` 如何对应）—— 注意：**旁证缺陷可独立修复**，见 B3

#### A7 — `foods.stock` 是**第几个库存真相**

- **现状事实**：`foods.stock = 99`；`inventory`（11 行）与 `store_inventory`（16 行）是另两处；全库**至少 3 处持有数量**，彼此无同步。
- **归类**：**A**
- **为什么必须由负责人决定**：成品库存是否应计入「库存」范畴、以及它与门店/仓库持仓的关系，是业务定义问题。

---

### B 类 —— 已有充分事实，可直接工程修复

> 以下各项**均不改变业务含义**，且在本地/代码层可复现。B 类**不含**任何需要「哪个表是 truth」判断的动作。

| ID | 缺陷 | 事实 | 允许的工程动作（不含语义变更） |
|---|---|---|---|
| **B1** | `inventory.current_stock` 精度与 `store_inventory` 不一致 | 实测 `integer` vs `numeric(15,3)`；`chk_inventory_stock_nonneg` 亦为整数比较 | 将 `inventory.current_stock` / `safety_stock` 对齐为与门店一致的精度；**不改变任何数值** |
| **B2** | `inventory.material_id` 缺外键 | `pg_constraint` 实测无 FK；`store_inventory.material_id` 亦无 | 对无孤儿的数据加 FK；若有孤儿先登记不清洗 |
| **B3** | `dish_recipe` 代码查询**不存在列** | `DishRecipeMapper` 查 `ingredient_id`/`sort_order`/`estimated_cost`；实体映射 21 列，实际表仅 13 列 | 移除/停用这条**必然失败**的代码路径（或标注不可用）；**不做**表间语义迁移（属 A6） |
| **B4** | `inventory_transactions.inventory_id` 悬挂 | 17/27（62.96%）指向不存在的持仓；零 FK | 登记悬挂清单；加 FK 前先处理孤儿；**不删除历史流水** |
| **B5** | `inventory_transactions.transaction_type` 为 NULL | 2 行（adjust 类型） | 补枚举值或显式允许 NULL 并加注释；**不猜测业务含义** |
| **B6** | `store_inventory_log.store_id` 含孤儿值 `584` | 既不匹配 `stores`(1,2) 也不匹配 `warehouses`(1..10) | 登记数据缺陷；加类型/来源校验；**不改写历史值** |
| **B7** | `product` 残留调用点的**映射可疑** | `PurchaseOrderServiceImpl.java:638` 以 `getMaterialId()` 查 `product`；4 处其他调用 | 加断言/日志暴露不匹配；**不删除**调用点（退役属 A5/A1 决策） |
| **B8** | `purchase_request_item.food_id` **无校验** | 3 行不可解析、1 行无匹配；服务层 `parseLong` 失败静默写 `0L` | 加输入校验与显式失败（**拒绝静默降级**）；**不改列名/不改语义** |
| **B9** | `inventory_unit` / `inventory_category` 端点**必然 500** | 表不存在（`to_regclass` NULL）；实体+Mapper+Controller 全套存在；前端调用方 0 | 下线端点或建表；**业务语义存疑**（见 C5），故仅允许「使之不再暴露」 |
| **B10** | `inventory` 缺 `(material_id, warehouse_id)` 唯一约束 | 有 PK 无业务唯一键；实测当前 0 重复 | 加防重复约束（**只在确认两余额表关系前仅防重复、不建同步**）；若属 A3 范围则先挂起 |

**B 类的统一限制**：B 类动作**不得**顺手做以下事 —— 改名、合并表、删除 legacy 数据、把某表宣布为 truth、补齐业务含义。

---

### C 类 —— 必须先取得生产证据

> 生产访问现状：**不可达**（无独立实例/凭证/主机配置；唯一可查实例承载测试数据）。以下为 Gate-0 `BLOCKED` 解除后须回填的项。

| ID | 必须取得的生产证据 | 回填后影响 |
|---|---|---|
| **C1** | 生产 `inventory` 中 `product_id` / `material_id` 的填充率与**不一致行数**（`material_id <> product_id` 或两列分别 JOIN 到不同名称） | A2 的方向选择 |
| **C2** | 生产 `inventory` 与 `store_inventory` 的同物料差异规模 | A3 定性（差异是缺陷还是常态） |
| **C3** | 生产 `inventory.expiry_date` / `batch_no` 填充率 | A4 的直接依据 |
| **C4** | 生产 `material_archives` 重复命名规模（同物多名/多名一物） | A1 的迁移范围 |
| **C5** | 生产是否存在调用 `/v1/inventory/unit`、`/v1/inventory/category` 的**真实客户端** | B9 的处置方式（下线 vs 建设） |
| **C6** | 生产 `purchase_request_item.food_id` 的不可解析比例 | A2 的破损规模 |
| **C7** | 生产 `orders_legacy` 行数与写入活动 | A5 的退役判定 |
| **C8** | 生产是否存在按 `version = 0` 过滤的视图或客户端查询 | 先前「version 双语义」风险是否真实 |

**C 类纪律**：C 类证据**只能降低不确定性，不能替代 A 类决策**。不得以「生产数据看起来干净」为由跳过 A 类。

---

### D 类 —— 当前判断本身可能错误，需继续验证

| ID | 当前判断 | 为什么可能错 | 需要什么才能收敛 |
|---|---|---|---|
| **D1** | 「`product` 是 Commercial Concept / 商业抽象」 | 该结论建立在 1 行测试数据 + 一个定价键上；`product` 在生产可能是空表或另一含义 | 生产 `product` 行数与引用情况（C1/C7 同批） |
| **D2** | 「Material 是 Canonical Identity」 | 只在本地库成立；且 `material_id` 在 5 张表中类型/宽度不一（VARCHAR(50)/VARCHAR(32)/BIGINT），同一值是否指同一物未验证 | 生产跨表 `material_id` 一致性抽样 |
| **D3** | 「`foods` 是 Dish/Sales Food Truth」 | 本地活跃 `foods` 仅 1 行（生菜串）；`food` 遗留表仍被读取（`FoodMapper` 有调用点，`OrderTimeoutTask.java:121-124` 以数值 `food_id` 当 `food_code` 用） | 生产 `foods` 与 `food` 行数、以及是否仍双写 |
| **D4** | 「`orders` 是 canonical order truth」 | 本地成立（10 行，`aggregateByPaymentMethod` 等日结路径确读 `orders`），但 `OrderServiceImpl` **仍使用映射到 `orders_legacy` 的 `Order` 实体**；生产是否有 legacy 写入未知 | 生产双表行数 + 日结聚合口径核对 |
| **D5** | 「三层模型（Definition/Commercial Unit/Holding）是可能的正确解」 | Commercial Unit 层**在工程中不存在**；「三层」是描述性假设，缺少任一现实实现佐证 | 需先确认业务是否要求多商业单位（属 A1 的输入，非 C） |
| **D6** | 「`inventory_unit` 曾真实存在」 | 仅由 2026-06-30 一天日志支持；该日志可能来自已废弃的测试环境 | 生产 `to_regclass` + 生产日志检索 |

---

## 3. 反证（仅对 A 类，遵循指定五问格式）

> 本节**不使用**「最佳实践认为 / 行业通常 / 推荐采用」等表述。以下所有「支持/反对」均为**证据陈述**，非推荐。

### 3.A1 反证 —— 是否采用分层身份

| 问 | 答 |
|---|---|
| **若采用分层 X（Definition + Commercial Unit + Inventory Holding）：会破坏什么？** | ① 需新建一个当前**完全不存在**的层（无 SKU 列、无包装层级、无任一物料含多包装），因此要改写所有库存写入路径以带商业单位；② 现有两张余额表只有单一数量标量（`current_stock` + 自由文本 `unit`），分层后每个数量都需重新归属到某一层，历史数值的归属需逐个判定；③ `foods.stock`、`inventory`、`store_inventory` 三处既有数量必须在层内重新定位，否则会出现第四套口径。 |
| **若不采用 X：会产生什么？** | ① 跨模块仍无法可靠串接（现状：`purchase_request_item.food_id` 实存 material ID，靠 `parseLong` 桥接，失败静默写 `0L`）；② 「以箱采购、以罐核算」在单一层下仍不可表达（无换算、`unit` 为自由文本）；③ 分层所解决的「同一物品多商业单位」问题将继续只能靠多建物料行规避——而本库中已出现同物多行的实例（`Test Tomato` 7 行、`E2E Potato` 2 行、`圆形/长方形打包盒` 2 行）。 |
| **当前有哪些证据支持 X？** | ① `inventory` 行同时携带 `unit` 与 `batch_no`，说明数量须依附单位与批次才有意义；② 销售侧已用判别式多态 `(product_type ∈ {FOOD, COMBO}, product_id)`，即**商业单位层已被实现以另一种形式承认**；③ `store_inventory` 与 `inventory` 键空间不同（`store_id+material_id` vs `material_id+warehouse_id`），说明「持仓」至少有二态。 |
| **当前有哪些证据反对 X？** | ① Commercial Unit 层**零实现**：全库无 `sku`、无 `uom`、无换算列；② **没有任何一个物料拥有多个包装层级**（brief 的「单罐 vs 整箱」在本库无实例）；③ `material_archives` 只有单值 `spec`/`unit`/`barcode`，无法在单一物品下表达两个可分单位；④ 本地数据规模极小（11/16/1/35 行），不足以证明分层的必要性。 |
| **缺什么证据才能降低不确定性？** | ① 生产是否存在「同一物料、多个包装/交易单位」的真实行（**属 A 类输入，不可由 C 替代**）；② 生产 `material_id` 与 `product_id` 是否出现语义分歧行（C1）；③ 业务上是否存在「以箱采购、以罐核算」的实际作业（需求面事实，非数据面）。 |

### 3.A2 反证 —— 命名空间冲突的处置方向

| 问 | 答 |
|---|---|
| **若采用方案 X（以 `material_id` 为唯一真相、废弃 `product_id`）：会破坏什么？** | ① 破坏 `fk_inv_product_id` —— 它是**全库唯一**指向 `product` 的物理外键，删除将失去该完整性约束；② `product` 表 5 处活跃调用点将失去目标（其中 `PurchaseOrderServiceImpl.java:638` 以 materialId 查 product，删除后该逻辑必须重写而非删除）；③ 若生产 `product` 有真实行且被引用，历史库存行的物品归属将不可还原。 |
| **若不采用 X：会产生什么？** | ① 双口径持续存在，`inventory_id=4` 这类「一行两物」无法被系统发现；② 完整性约束继续指向一个近乎空转的列，而实际写入列无约束保护；③ 任何后续身份模型都必须先解释这一冲突。 |
| **当前有哪些证据支持 X？** | ① `material_id` 填充 11/11、`product_id` 1/11；② `Inventory.java:382-388` 已把 `getProductId()` 实现为 `materialId` 的别名（代码层已单方面认定等价）；③ 无任何代码路径写入 `product_id`。 |
| **当前有哪些证据反对 X？** | ① 唯一物理外键在 `product_id` 上（声明式完整性指向相反方向）；② `product` 仍被 5 处调用；③ 本地唯一同时填充的行**两列所指物品不同**，说明二者**不是同一命名空间**，因此「以谁为真相」不是二选一，而可能是「二者本就不该出现在同一行」。 |
| **缺什么证据才能降低不确定性？** | 生产 `inventory` 两列填充率与不一致行数（C1）；生产 `product` 行数与引用数（C7 同批）；是否存在写入 `product_id` 的客户端或作业。 |

### 3.A3 反证 —— 两张余额表的关系

| 问 | 答 |
|---|---|
| **若采用 X（同一持仓的两个视图）：会破坏什么？** | ① 现有 4 例数量差异必须被判为数据错误并修正，修正动作会改变账面库存；② `store_inventory.store_id`（VARCHAR）与 `inventory.store_id`（BIGINT，实测 0 填充）必须先统一，否则无法对账；③ 本地门店独有的 7 个物料（菠菜/生菜/土豆片/金针菇/方便面/红薯粉/商用电磁炉）在仓库侧**没有行**，收敛后需为它们补建仓库持仓行——这是**新增业务事实**。 |
| **若不采用 X：会产生什么？** | ① 「总库存」只能继续以 `UNION ALL` 相加（`InventorySummaryMapper.xml` 现状），两个口径的差异不被发现；② 调拨的对账基准不明确。 |
| **当前有哪些证据支持 X？** | ① 同一动作（收货/调拨）**同时写两张表**（多处 dual-write 调用点）；② 两表都持有同一 `material_id` 的 `current_stock`。 |
| **当前有哪些证据反对 X？** | ① 门店独有的 7 个物料在仓库侧完全不存在，说明二者并非同一集合的两种视图；② `store_inventory.store_id` 实测含仓库 ID（`584`），键空间本身已混乱；③ 两表精度不同（integer vs numeric(15,3)），若为同一持仓不应有此差异。 |
| **缺什么证据才能降低不确定性？** | 生产两表差异规模（C2）；调拨单据在两表的记账口径；是否存在只入门店不入仓库的收货流程。 |

### 3.A4 反证 —— 批次/效期是否为持仓身份维度

| 问 | 答 |
|---|---|
| **若采用 X（批次构成持仓身份）：会破坏什么？** | ① `store_inventory` **无批次列**，须新增列并重定唯一键（当前 `(store_id, material_id)`），历史行需回填批次——本地 16 行中有意义的批次信息不存在；② `inventory` 现为「一行一批次」的单值覆盖式写入（实测 4 行有批次）；改为批次维度后同一 `(物料, 仓库)` 将出现多行，需处理既有重复语义；③ 召回口径改变会影响追溯与损耗记账。 |
| **若不采用 X：会产生什么？** | ① 效期预警继续读取实测填充率 **0%** 的列（本地恒为空集）；② 召回无法以库存行为凭据，只能依赖追溯码空间（该空间本地有 18 行、13 有批次、8 有效期）。 |
| **当前有哪些证据支持 X？** | ① `inventory` 行自带 `batch_no`/`production_date`/`expiry_date` 列，说明设计意图上批次属持仓行；② 追溯码空间持有完整批次与效期，说明业务确实需要批次粒度；③ `recall_record.batch_no` 为 NOT NULL，说明召回以批次为单位。 |
| **当前有哪些证据反对 X？** | ① 库存侧 `expiry_date` 填充 **0%**、`batch_no` 仅 36%，实际未按批次管理；② `inventory_transactions`（台账）**完全没有批次列**，若批次是持仓身份则台账无法追溯批次变动；③ 门店侧完全没有批次概念。 |
| **缺什么证据才能降低不确定性？** | 生产库存侧批次/效期填充率（C3）；生产是否存在按批次执行的召回或临期处置记录；门店是否需要批次追溯（业务要求，属 A）。 |

### 3.A5 反证 —— 旧 POS / `orders_legacy` 是否退役

| 问 | 答 |
|---|---|
| **若采用 X（宣布退役）：会破坏什么？** | ① `OrderServiceImpl` 仍以 `Order`（`@TableName("orders_legacy")`）做 CRUD，若该服务仍被调用，退役会使其写入失败；② `OrderMapper` 的 11 个 legacy 统计方法虽当前无调用方，但属于已发布接口面。 |
| **若不采用 X：会产生什么？** | ① 死代码与 18 处 legacy 引用持续存在，后续任何「订单 truth」讨论都需反复排除；② `Order` 实体与 `OrderNew` 实体并存，映射到不同表，构成持续的误用风险。 |
| **当前有哪些证据支持 X？** | ① 本地 `orders_legacy` **0 行**、`order_items_legacy` **0 行**；② `OrderMapper.xml` 中所有**在用**聚合（`aggregateByPaymentMethod`/`sumDiscountAmount`/`aggregateRefunds`/`aggregateCancelled`）均查 `orders`，`DailySettlementServiceImpl` 走这些方法；③ 11 个 legacy 方法实测**无调用方**。 |
| **当前有哪些证据反对 X？** | ① `OrderServiceImpl` 仍引用该实体；② `OrderMapper.java` 仍保留 18 处引用；③ 生产是否存在旧 POS 客户端写入该表**未知**。 |
| **缺什么证据才能降低不确定性？** | 生产 `orders_legacy` 行数与最近写入时间（C7）；是否有旧 POS 端仍在运行（部署事实）。 |

### 3.A6 反证 —— 两套 recipe 表的业务归属

| 问 | 答 |
|---|---|
| **若采用 X（废弃 `dish_recipe` 单数、统一到 `dish_recipes`）：会破坏什么？** | ① 键空间不同：`dish_recipe.dish_id` 是 **VARCHAR(50)**，`dish_recipes.food_id` 是 **BIGINT**，迁移需建立 `dish_id → food_id` 的对应，而该对应**无任何映射表**；② `DishRecipeMapper` 的 4 条查询与实体 21 列均基于不存在列，迁移等于重写；③ 依赖它的 3 个服务（库存扣减、厨房扫码、物料需求）行为会改变。 |
| **若不采用 X：会产生什么？** | ① 库存扣减的 `/v1/inventory-deduction/recipe` 路径持续**必然失败**（查询不存在列）；② 「配方真相」继续不可判定。 |
| **当前有哪些证据支持 X？** | ① `dish_recipe` **0 行**、`dish_recipes` 有 1 行活跃（4 行软删）；② `FoodServiceImpl`/`OrderNewServiceImpl`/`RecipeServiceImpl`/`StockForecastServiceImpl` 使用的都是 `dish_recipes`（复数）；③ `dish_recipe` 的实体与表结构**已不一致到无法运行**。 |
| **当前有哪些证据反对 X？** | ① 仍有 3 个服务注入 `DishRecipeMapper`；② 本地样本极小（1 行活跃配方），不足以代表生产用法。 |
| **缺什么证据才能降低不确定性？** | 生产两表行数；`dish_recipe.dish_id` 的取值是否可映射到 `foods.food_code`；是否存在仅由旧接口使用的配方。 |

### 3.A7 反证 —— `foods.stock` 是否为库存真相之一

| 问 | 答 |
|---|---|
| **若采用 X（`foods.stock` 计入库存真相）：会破坏什么？** | ① 需定义它与 `inventory`/`store_inventory` 的换算与同步规则，而三者单位体系不同（`foods.unit='份'`、`inventory.unit='斤'`）；② 成品库存进入库存口径会改变成本与损耗计算基准。 |
| **若不采用 X：会产生什么？** | ① 「成品是否有库存」在系统内无单一答案（`foods.stock=99` 仍是可见数字）；② `OrderTimeoutTask` 回补逻辑（对 `food` 与 `foods` 双表改 stock）继续存在。 |
| **当前有哪些证据支持 X？** | ① `foods.stock` 与 `foods.min_stock` 为真实列且有值；② 存在专门的库存回补代码路径。 |
| **当前有哪些证据反对 X？** | ① `foods` 列注释为「库存数量（可选，用于限量菜品）」——**语义是限量而非库存**；② 无任何台账记录 `foods.stock` 变动。 |
| **缺什么证据才能降低不确定性？** | `foods.stock` 在生产是否被业务实际使用（限量菜品 vs 库存管理）——属业务语义确认（A），生产数据可作旁证。 |

---

## 4. 明确工程问题（与业务架构分离）

> 依 brief §7 要求单独列出，**不与业务架构混在一起**。本节全部为**技术缺陷陈述**，其修复授权见 §5 B 类与文末 `ENGINEERING_ACTIONS_ALLOWED`。

| # | 问题 | 技术事实 | 是否阻断业务决策 | 备注 |
|---|---|---|---|---|
| E1 | `purchase_request_item.food_id` | 列 VARCHAR(32)；46/50（92%）实为 material ID；3 行不可解析、1 行无匹配；服务层 `parseLong` 失败静默写 `0L` | **否**（不阻断；但影响 A2 的破损规模） | 列名与语义不符属 A2 范围，**校验缺失属工程** |
| E2 | `dish_recipe` / `dish_recipes` | 两表并存；单数表缺 8 个被映射列；`DishRecipeMapper` 4 条查询必然失败；3 个服务依赖它 | **否**（旁证缺陷可修） | 表间语义归属属 A6 |
| E3 | precision mismatch | `inventory.current_stock` = `integer`；`store_inventory.current_stock` = `numeric(15,3)`；`chk_inventory_stock_nonneg` 亦为整数 | **否** | 纯类型对齐 |
| E4 | missing FK | `inventory.material_id` 无 FK；`store_inventory.material_id` 无 FK；`inventory_transactions` **零 FK**；`store_inventory_log` **零 FK** | **否** | 加约束前需处理孤儿 |
| E5 | dangling `inventory_transactions` | 17/27（62.96%）`inventory_id` 指向不存在持仓；3 行持仓无流水；2 行 `transaction_type` NULL | **否** | 不清洗历史 |
| E6 | broken `inventory_unit` / `inventory_category` endpoints | 两表 `to_regclass` = NULL；实体+Mapper+Service+Controller 全套存在（各 6 个端点）；前端调用方 0；2026-06-30 日志显示曾被 Web 端调用且成功 | **否** | 处置方式见 C5 |
| E7 | active `product` legacy residue | `ProductMapper` 5 处调用；`PurchaseOrderServiceImpl.java:638` 以 materialId 查 product；`product` 表 1 行测试数据 | **否**（但退役属 A1/A5） | 映射可疑属工程；删除属决策 |
| E8 | `store_inventory_log.store_id` 孤儿值 | 值 `584` 不匹配任何 `stores`(1,2) 或 `warehouses`(1..10) | **否** | 不改写历史 |
| E9 | `inventory` 缺业务唯一约束 | 有 PK，无 `(material_id, warehouse_id)` 唯一键；实测当前 0 重复 | **否** | 与 A3 相关，仅允许加防重复 |
| E10 | `orders_legacy` 死代码 | `OrderMapper` 18 处引用、11 个无调用方方法；`Order.java` 仍映射该表；本地 0 行 | **否** | 退役属 A5 |

---

## 5. 汇总统计

| 类别 | 数量 | 编号 |
|---|---|---|
| **A — 负责人决策** | **7** | A1–A7 |
| **B — 可直接工程修复** | **10** | B1–B10 |
| **C — 必须先取生产证据** | **8** | C1–C8 |
| **D — 当前判断可能错误** | **6** | D1–D6 |
| 工程问题单列（与业务架构分离） | **10** | E1–E10 |

**交叉关系**：A2↔C1、A3↔C2、A4↔C3、A5↔C7、A6（归属）↔B3（旁证缺陷）、A7↔（无 C）。**A 与 C 是并列关系，不是替代关系。**

---

## 6. 收口结论

### OWNER_DECISIONS_REQUIRED

> 仅列真正需要负责人拍板的问题，控制在最小数量。**以下每一项都无法由工程或生产数据替代。**

| ID | 决策问题 | 为什么工程无法替代 |
|---|---|---|
| **A1** | Product Definition / Commercial Unit / Inventory Holding **是否分层**；若分层，哪一层是库存计量基准 | Commercial Unit 层在工程中不存在；是否需要它取决于业务是否要求「同一物品多交易单位」，这是业务模型主张 |
| **A2** | `product_id` 与 `material_id` 冲突的**处置方向**（保留哪个为物品真相 / 是否二者本不应同行） | 「谁是物品真相」是业务主张；且实测两列所指物品不同，说明可能是「不该同行」而非「二选一」 |
| **A3** | 仓库持仓与门店持仓是**同一持仓的两视图**还是**两个独立持仓** | 决定两处差异是缺陷还是常态；决定对账规则 |
| **A4** | 批次 / 效期是否为**持仓身份维度** | 涉及食品安全召回责任与库存粒度 |
| **A5** | `orders_legacy` / 旧 POS 链路在业务上**是否仍在服役** | 部署与业务事实，工程无法从本地判定 |
| **A6** | `dish_recipe`（单数）与 `dish_recipes`（复数）的**业务归属**，以及 `dish_id`↔`food_id` 的对应 | 键空间不同且无映射表；对应关系是业务事实 |
| **A7** | `foods.stock` 是否计入「库存」范畴（限量标记 vs 库存） | 业务定义问题 |

**最小集合说明**：以上 7 项已不可再合并 —— 每一项的答案都会独立改变业务世界模型；合并会产生互相污染的结论。

---

### ENGINEERING_ACTIONS_ALLOWED

> 仅列无需等待业务决策即可执行的工程项。**全部不改变业务含义、不删除历史、不改名、不合并表。**

| ID | 允许动作 | 前置 |
|---|---|---|
| **B1** | 对齐 `inventory.current_stock` / `safety_stock` 精度至与 `store_inventory` 一致（`integer` → `numeric`），**不改变任何数值** | 无 |
| **B4** | 登记 `inventory_transactions.inventory_id` 悬挂清单（17 行），加 FK 前先暴露孤儿 | 无 |
| **B5** | 处理 2 行 `transaction_type IS NULL`：补枚举值或显式声明允许 NULL，**不猜测业务含义** | 无 |
| **B6** | 为 `store_inventory_log.store_id` 增加来源/类型校验，登记孤儿值 `584` | 无 |
| **B8** | 为 `purchase_request_item.food_id` 增加输入校验，**消除静默写 `0L`**，改为显式失败 | 无 |
| **B2** | 在无孤儿的前提下为 `inventory.material_id` / `store_inventory.material_id` 补 FK | 需先跑 B4 同类检查 |
| **B10** | 为 `inventory` 增加**仅防重复**的唯一约束（不建跨表同步） | 若归入 A3 范围则先挂起 |
| **B7** | 为 `product` 残留调用点增加断言/日志以暴露映射不匹配，**不删除调用点** | 无 |
| **B3** | 移除或停用**必然失败**的 `dish_recipe` 代码路径（或标注不可用），**不做表间语义迁移** | 不做迁移，迁移属 A6 |
| **B9** | 使 `/v1/inventory/unit`、`/v1/inventory/category` **不再暴露**（下线或遮蔽），**不建表** | 建表属业务语义（见 C5） |

**B 类红线**：不得顺手改名、合并表、删除 legacy 数据、宣布某表为 truth、补齐业务含义。

---

### PRODUCTION_EVIDENCE_REQUIRED

> 仅列必须从生产环境取得证据的问题。**生产当前不可访问，Gate-0 = BLOCKED。**

| ID | 需要的生产证据 | 关联决策 |
|---|---|---|
| **C1** | `inventory` 中 `product_id` / `material_id` 填充率与不一致行数 | A2 |
| **C2** | `inventory` 与 `store_inventory` 同物料差异规模 | A3 |
| **C3** | `inventory.expiry_date` / `batch_no` / `production_date` 填充率 | A4 |
| **C4** | `material_archives` 重复命名规模 | A1（迁移范围） |
| **C5** | 是否存在调用 `/v1/inventory/unit`、`/v1/inventory/category` 的真实客户端 | B9 处置方式 |
| **C6** | `purchase_request_item.food_id` 不可解析比例 | A2（破损规模） |
| **C7** | `orders_legacy` 行数与最近写入时间 | A5 |
| **C8** | 是否存在按 `version = 0` 过滤的视图/客户端 | 先前「version 双语义」风险真伪 |

**纪律**：C 类**只降低不确定性，不替代 A 类决策**；不得因生产数据「看起来干净」而跳过 A。

---

### FURTHER_VALIDATION_REQUIRED

> 尚不能进入决策，也不能进入工程的问题。

| ID | 判断 | 为什么现在不可用 | 收敛路径 |
|---|---|---|---|
| **D1** | `product` = Commercial Concept | 建立在 1 行测试数据 + 一个定价键上 | C1/C7 同批取生产行数与引用 |
| **D2** | Material = Canonical Identity | `material_id` 在 5 张表中宽度/类型不一（VARCHAR(50)/VARCHAR(32)/BIGINT），同一值是否指同一物未验证 | 生产跨表 `material_id` 一致性抽样 |
| **D3** | `foods` = Dish/Sales Food Truth | 本地活跃仅 1 行；`food` 遗留表仍有调用点（`OrderTimeoutTask.java:121-124` 以数值 `food_id` 当 `food_code`） | 生产 `foods`/`food` 行数与是否双写 |
| **D4** | `orders` = canonical order truth | 本地成立，但 `OrderServiceImpl` 仍用映射到 `orders_legacy` 的 `Order` 实体 | 生产双表行数 + 日结口径核对 |
| **D5** | 三层模型是可能的正确解 | Commercial Unit 层零实现；「三层」缺任一现实实现佐证 | 需先确认业务是否要求多商业单位（属 A1 输入） |
| **D6** | `inventory_unit` 曾真实存在 | 仅由 2026-06-30 单日日志支持，该日志可能来自已废弃测试环境 | 生产 `to_regclass` + 生产日志检索 |

---

### CURRENT_DECISION_STATUS

| 项 | 状态 |
|---|---|
| **PD-CANONICAL-001 是否仍然 NOT CONFIRMED** | **是。仍为 `RECOMMENDED / NOT CONFIRMED`。** 本审查未修改该 Decision 文件，未将其改为 CONFIRMED 或 LOCKED。 |
| **是否可以进入正式工程建模** | **否。** 理由：① A1–A7 七项负责人决策全部未决；② 生产证据 Gate-0 = `BLOCKED`，C1–C8 全部未取；③ D1–D6 六项当前判断不可信。在 A1（是否分层）与 A2（命名空间处置）裁决前，任何正式身份建模都会把未验证假设固化进 schema。 |
| **当前最小下一步** | **① 由 DBA/运维在生产库执行 `production-evidence-gate-001.md` §16 的 12 条只读查询并回填（解除 Gate-0）；② 同时由负责人就 A1 与 A2 作出方向性裁决。** 这两步互不依赖，可并行；其余 A3–A7 可在其后跟进。 |
| **允许并行执行的工程项** | B1、B3–B9（见 `ENGINEERING_ACTIONS_ALLOWED`），均不改变业务含义、不删除历史、不改名、不合并表。 |

---

## 7. 本次审查的立场声明

1. **未提出新架构**，未创建新的 Option A/B/C/D/E，未设计表、API 或迁移。
2. **未修改** schema、生产代码、legacy 数据；未执行任何迁移。
3. **未把本地证据重新命名为生产证据** —— 全部本地发现均标注 `LIVE_LOCAL_INSTANCE_EVIDENCE`；生产证据一栏为空（C1–C8 待取）。
4. **未以自己的推荐替代负责人决策** —— 全部 A 类问题仅给出反证，未给出结论性建议。
5. **未因已有 Recommendation 而假定 Decision 已存在** —— PD-CANONICAL-001 明确维持 `NOT CONFIRMED`。
6. **本审查同时修正了自身先前的过度结论**：先前将 `orders_legacy` 与 `dish_recipe` 笼统列为「活跃缺陷」；本次实测区分出其**代码虽在、调用方为零 / 表为空**的实际状态，并据此把它们分别归入 A5/A6（业务归属）与 B3/B10（可修缺陷）。
