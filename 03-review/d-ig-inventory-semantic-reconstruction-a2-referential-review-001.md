# D-IG — A2 Referential Integrity / Identifier Semantics Review 001

> **状态**: ACTIVE_REFERENCE — REFERENTIAL / VALUE-DOMAIN REVIEW
> **前置**: A1 = PASS_WITH_REPAIRS（独立复核 4048e21）· UNRESOLVED_ATTACK = NO
> **Governance baseline**: `5923e7e585e718cf9ff1bfbec0380369ec85be32`
> **Production Evidence**: BLOCKED
> **Identity Decision**: NOT_MADE · **Schema/Migration Authorization**: NO · **Owner Decision Request 001 未被修改**

---

## 1. Evidence Baseline

```text
engineering worktree = 6b54411 + dirty（811：464 真实改动 + 347 untracked + 1 删除；构成已单独立案）
DB snapshot          = 2026-09-13（PostgreSQL 18.3 / food_traceability / flyway head 20260909.003）
governance docs      = P 盘镜像 @4048e21（本次复核全部离线可读）
A1 结论（不再重攻）  = locator 收敛于 (materialId, warehouseId) / (storeId, materialId) / inventory.id
```

A2 问题与 A1 正交：**这些 ID 的值到底来自哪个值域、指向哪个对象、是否存在 referential/value-domain inconsistency。**

---

## 2. Attack Package

| Attack | Question | PASS 标准 | FAIL 标准 |
|---|---|---|---|
| R1 | inventory.product_id 与 material_id 是否同值域 | 无异域证据，或证明 product_id 系历史兼容且值域始终等价 | active path 中二者承载不同 referent 且有业务影响 |
| R2 | food_id 与 material_id 是否同 referent | 库存语义上无 active referential conflict | Food 值→inventory.material_id 且域错、致错误库存 |
| R3 | TraceCode.target_id 的 referent | 进入 material_id 前已证明是 Material ID 或有 discriminator | Food/Other 域值未转换直接作 material_id 且影响真实库存 |
| R4 | LossOutbound.product_id 的值来源 | active writer 实际传入 Material ID 且有来源证据 | Product ID 未转换进入 material_id 并能修改库存 |
| R5 | SalesOrderDetail.food_id 的 referent | Food/Material/历史命名的归属可判定 | —（同 R2 接口） |

---

## 3. A2-R1 — Inventory 双列值域一致性

### ENGINEERING FACT

1. `inventory.product_id BIGINT` 带**全表唯一 FK** `fk_inv_product_id → product(product_id)`；`material_id BIGINT` **无 FK、无唯一约束**（pg_constraint 实测）[DB-SCHEMA]。
2. `product` 表仅 1 行（`测试物料A`，测试数据）；`material_archives` 35 行（13 软删）——**两个值域是不同的物理表空间** [LIVE-REPRO]。
3. 双列并存行 `inventory_id=4`：`material_id=1`→娃娃菜，`product_id=1`→测试物料A——**同一数值在两个值域指向不同对象**（CONF-07 复现）[LIVE-REPRO]。
4. **product_id 列无活跃写入方**（A1 §15：A 类出现=0），也无活跃读取方（全库无 SELECT/ORM 读取该列；唯一 getProductId 为 materialId 别名）。material_id 11/11 填充。
5. 迁移史：表以 product_id 形态诞生（V1.0.0.100），material_id 后置追加（V20260730_001，无 FK）[MIGRATION]。

### SEMANTIC FINDING

- **value-domain mismatch（已证实）**：两列的值域不同源（product 表 vs material_archives），且 inventory_id=4 证明同值异指——**R1 的 PASS 两条件均不成立**。
- **但 FAIL 条件亦不满足**：product_id 列无 active writer/reader，"active business path 中承载不同 referent 并有业务影响"无法对该列本身成立；其业务影响目前是**潜在的**（取决于 Owner Q1/Q3 与生产侧未知状态）。

### OWNER QUESTION

沿用已登记的 Owner Decision Request 001 **Q1/Q3**（inventory 业务对象范围；另一列解释）——本攻击为其补充了 FK 不对称与同值异指两项血证，不新增、不修改问题。

```text
A2-R1 = INCONCLUSIVE（PASS 被排除；FAIL 缺 active 实证；UNRESOLVED_ATTACK 对 product_id 列的业务意义 = YES）
```

---

## 4. A2-R2 — Food → Material

### ENGINEERING FACT

1. **唯一活跃的 Food→Material 转换路径**：`OrderNewServiceImpl` 销售出库/退款回补 → `addFoodMaterialDeductions(foodId,…)` → `getDishRecipes(foodId)`（dish_recipes 按 food_id 查询）→ 取 `recipe.getMaterialId()`（null 跳过）→ 以 **dish_recipes.material_id（Material 域值）** 汇总扣减 `store_inventory`（decreaseStock/getByStoreAndMaterial/increaseStock，(storeId, materialId) 粒度）[CODE: OrderNewServiceImpl.java:670-706, 626-645, 752-770]。**无任何分支将 food_id 本身放入 materialId 槽。**
2. `PosOrderCreateServiceImpl` 对 `Inventory|inventoryMapper|materialId` **零引用**——POS 链仅扣 food/foods.stock [CODE]。
3. `KitchenScanServiceImpl` 不触碰库存 [CODE]。
4. **反向漂移（field-name↔value-domain）**：`purchase_request_item.food_id` 实际存 **Material 值**（46/50，A2 §4.5 Owner 已确认跨命名空间漂移）——字段名是 food，值域是 material。
5. `SalesOrderDetail.productId → order_items.food_id`（T-7 对齐）→ 其扣减路径已被 A1 证明不可达（三重死因）。

### SEMANTIC FINDING

- 转换路径本身 referent 正确（recipe 介导、Material 域值）。
- **但经 R3 交叉（见 §5/§8）：菜品域（type 1）追溯码出库可把 Food 域 target_id 送入 material_id 扣减路径**——Food→inventory.material_id 的 active referential conflict **由 R3 的结构缺陷构成**，R2 依跨攻击规则回查后不能再记 PASS。
- `purchase_request_item.food_id` 的同名异域（Food 名 ↔ Material 值）是采购侧的 drift 实证，库存侧影响经 W1 传导（入库明细 materialId 来自订单链，46/50 可解析）。

### OWNER QUESTION（候选，待治理层裁定是否提交）

- 无新问题；采购侧 food_id 命名漂移已在 A2 §4.5 登记，维持。

```text
A2-R2 = FAIL（经 R3 交叉依赖成立：Food 域值可经菜品追溯码出库进入 material_id 扣减；本地 food_trace_codes 0 行、生产未知——实例化状态同 R3）
```

---

## 5. A2-R3 — TraceCode.target_id（多态字段）

### ENGINEERING FACT

1. `TraceCode` 实体 → `@TableName("food_trace_codes")`；`target_type Integer` + `target_id Long` **多态设计** [CODE: TraceCode.java:14,34-41]。
2. **trace_type 语义映射**：`1=菜品追溯码、2=原料批次追溯码、3=物流追溯码、4=检验报告码`（FoodTraceabilityServiceImpl.getTraceTypeName:769-776）——**菜品域与原料域共表混存是设计事实**。
3. 创建路径两条：`FoodTraceabilityServiceImpl.generateTraceCode(:81-85)` 直接取 **客户端 DTO 的 targetType/targetId（无约束）**；`TraceCodeServiceImpl.create(:59-63)` 经别名 `setProductId(productId)→targetId`。
4. **出库路径无 discriminator**：`TraceCodeServiceImpl.outbound(:121-152)` 仅校验 status ∈ {CREATED, INBOUND} → `decreaseDTO.setMaterialId(traceCode.getProductId()→targetId)` → W2 粒度扣减——**全程不读 target_type/trace_type**。
5. 入口活跃：`TraceCodeController`（`/{traceCode}/scan`、`PUT /{id}/status` 等）。
6. 数据：food_trace_codes 本地 **0 行**；生产未知 [LIVE-REPRO]。

### SEMANTIC FINDING

- **cross-domain referent defect（结构性）**：同一张表按设计容纳菜品/原料/物流/检验四类 target，而出库把 target_id **无条件**当作 material_id 使用。任何 type=1（菜品）追溯码出库 → Food 域 target_id 直接扣减 material 库存——**不依赖脏数据，取决于码的类型即可成立**。
- provenance gap：target_id 的值域由客户端运行时输入决定，出库前无任何 discriminator/conversion。

### OWNER QUESTION（候选）

- OQ-A：菜品追溯码（type 1）的出库，业务上是否允许影响 material 库存？target_type 是否必须 = 原料批次才允许出库扣减？（待治理层决定是否并入 Owner Decision Request）

```text
A2-R3 = FAIL（结构性跨域 + 活跃入口 + 能影响真实库存；本地实例 0、生产未知——已如实标注）
```

---

## 6. A2-R4 — LossOutbound.product_id（及其他同模式）值域来源

### ENGINEERING FACT

1. **输入值来源已钉死**：`LossOutboundService.create(:60-64)` 用 `productMapper.selectById(dto.getProductId())` 校验，不存在即抛"商品不存在"→ **product_id 的值域 = product 表（Product 域）**，且 lossOutbound 的金额计算用 `product.getCostPrice()` [CODE]。
2. `approve → updateInventory(productId,…)` → wrapper `eq(Inventory::getProductId=别名→material_id, productId)`——**Product 域校验值直接作为 material_id 定位键**；该 wrapper 运行期必抛（A1-R1），故此路径**当前不可执行**。
3. **同模式存在可执行实例：OtherInboundService**（任务 §A2-R4 明示纳入范围）：`create(:58)` 同样以 product 表校验 → `updateInventory(:95-125)` 用**已映射的** `getMaterialId` 定位、无则 `inventoryMapper.insert` 新建行（material_id = **product 域值**）→ **POST /v1/other-inbound 单事务内真实修改余额** [CODE]。入口活跃（OtherInboundController，权限 other-inbound:create）。
4. 数值碰撞后果具体化：product#1（测试物料A）与 material#1（娃娃菜）**同时存在** → 其他入库选中"测试物料A"会给 `material_id=1`（娃娃菜）的余额加库存——错误归因 today 可发生（若该功能被使用）。
5. 数据：other_inbound **0 行**；999999 悬空行**不是** other_inbound 产物（已探测）[LIVE-REPRO]。

### SEMANTIC FINDING

- **provenance mismatch（已证实、且有可执行实例）**：同一数值被要求"在 product 表校验通过 + 在 material 域作为余额键"——只有数值碰撞（如 1）能同时满足，碰撞即错账。
- LossOutbound = 同模式潜在实例（不可执行）；OtherInbound = 可执行实例（未发生本地数据）。

### OWNER QUESTION（候选）

- OQ-B：其他入库/报损的业务对象域定义是什么？product 校验 + material 落库二者只能取一（业务上这批单据到底入的是谁的货）。

```text
A2-R4 = FAIL（Product 域值未转换进入 material_id 的模式已被活跃代码证实——OtherInbound 为可执行实例；LossOutbound 为潜在实例；本地数据实例 0、生产未知）
```

---

## 7. A2-R5 — SalesOrderDetail.food_id referent

### ENGINEERING FACT

1. `SalesOrderDetail.productId` → `@TableField("food_id")`（order_items 列，VARCHAR；T-7 注释"productId → order_items.food_id，单品ID"）[CODE: SalesOrderDetail.java:33-35]；运行期列映射实测（A1-R1 探针 [4]：解析为 food_id）。
2. 写入来源：POS 链以 food_code → `foods.food_id` 解析后写入；解析失败可存 NULL（C001-R2）；本地样本 6/6 NULL [LIVE-REPRO]。
3. 该字段的库存扣减消费方（SalesOrder 扣减路径）已被 A1 证明不可达 → 无库存效果。

### SEMANTIC FINDING

- referent 判定：**Food 域（foods.food_id，代码意图层面）**——非 Material、非 legacy alias；但存储层 NULL 全量使数据级 referent 无法展示（凭据：代码 + POS 解析链）。
- 与 R2 的关系：order_items.food_id（Food 域）与 purchase_request_item.food_id（Material 值域）**同名异域**——跨表同名不同义是本项目标识符语义的核心风险形态。

### OWNER QUESTION

- 无新问题：food_id referent 归属属 C-001/FM 轨道（A1 已登记），A2 不越界。

```text
A2-R5 = PASS（ referent = Food 域（代码级）；无 active 库存影响；数据级 NULL 为已登记缺陷）
```

---

## 8. Cross-Attack Dependencies（依任务 §四执行）

| 触发 | 重跑 | 结果 |
|---|---|---|
| R3 发现 target_type 多态（type 1 菜品 与 type 2 原料同表） | **R2 回查**：Food 值→material_id 是否有活跃路径 | **R2 由 PASS 改判 FAIL**（菜品追溯码出库路径）；rerun 已记录 |
| R4 发现 product 域值进入 material_id 的模式 | **R1 回查**：该模式是否也写 product_id 列 | 否（写的是 material_id 列）——R1 维持 INCONCLUSIVE |
| R1 双列异指 | **R4 回查**：LossOutbound 是否重复该模式 | 是（product 校验→material_id 定位），已并入 R4 |
| R5 food_id 归属 Food 域 | **R2 回查**：order_items.food_id 是否流入库存 | 仅经死路径（SalesOrder），无活跃流入 |

---

## 9. Evidence Matrix

| Attack | Question | Evidence | Value Domain | Active Writer | Referent | Result |
|--------|----------|----------|--------------|---------------|----------|--------|
| A2-R1 | product_id vs material_id | FK→product；同值异指行；无活跃读写方 | **不同**（product 表域 vs material_archives 域） | product_id 列：无；material_id 列：6 条写入路径 | 双重：product（FK 实证）/ material（数据实证）；业务 referent 未决 | **INCONCLUSIVE** |
| A2-R2 | food_id vs material_id | OrderNew 转换经 dish_recipes.material_id；purchase_request_item.food_id=Material 值 46/50；菜品追溯码出库路径 | 转换路径=Material ✓；追溯路径=Food 值入侵 | OrderNew（✓ 域正确）；TraceCode 出库（✗ 经 R3） | dish_recipes.material_id→material | **FAIL（经 R3 交叉）** |
| A2-R3 | target_id referent | 多态设计（type 1 菜品/2 原料同表）+ 出库无 discriminator + 客户端无约束输入 | 运行时决定（Food/Material/其他均可） | TraceCodeController（活跃） | **不可证明**（依赖运行时输入） | **FAIL** |
| A2-R4 | Loss product_id provenance | product 表校验（"商品不存在"）→ material_id 定位/插入 | **Product 域**（校验钉死） | OtherInbound（活跃、可执行）；LossOutbound（潜在、必抛） | product 表（1 行测试数据） | **FAIL** |
| A2-R5 | Sales food_id referent | T-7 映射（运行期实证 food_id 列）+ POS food_code 解析链；样本 6/6 NULL | VARCHAR（food_id 文本化） | POS 下单（写入 order_items，非库存） | **Food 域（代码级）** | **PASS** |

---

## 10. Negative Search

| 搜索 | 结果 |
|---|---|
| inventory.product_id 的活跃写入方（全代码 + XML + 配置类） | **0**（A1 §15 + 本轮复核一致） |
| inventory.product_id 的活跃读取方 | **0**（唯一 getProductId 为别名；无原生 SQL 读该列） |
| order_items.food_id → inventory.material_id 的活跃流入 | **0**（唯一候选路径 SalesOrder 扣减已证死） |
| food/foods 值进入 material 库存的其他路径 | **0**（PosOrderCreate 零引用；KitchenScan 不碰库存） |
| food_trace_codes 本地数据 | **0 行**（缺陷模式无本地实例） |
| other_inbound 本地数据 | **0 行**（同上） |
| loss_outbound 本地数据 | **0 行** |
| store_inventory.material_id 悬空 | **0**（门店侧值域干净；悬空 999999 仅在中央 inventory） |
| 999999 的来源表（other_inbound 等） | **0 命中**（来源不明，维持 UNRESOLVED） |

**注意**：以上"0"均为"未发现"，不等于"证明不存在"（生产侧全部未知）。

---

## 11. Engineering Facts（汇总）

1. inventory 双列分别指向两张物理表（product / material_archives），FK 仅在 product 侧；同值异指行实测存在。
2. inventory.product_id 列当前无活跃读写方；material_id 有 6 条写入路径 + 全部出库路径。
3. Food→Material 唯一活跃转换 = OrderNew 经 dish_recipes.material_id（值域正确）。
4. food_trace_codes 为多态追溯表（菜品/原料/物流/检验四类同表），出库不校验类型，target_id 直接作 material_id。
5. OtherInbound/LossOutbound 均以 product 表校验输入值、以 material_id 落库/定位；前者可执行、后者必抛。
6. SalesOrderDetail.productId→order_items.food_id（Food 域，VARCHAR），扣减链死。
7. purchase_request_item.food_id 存 Material 值（46/50，4 异常 UNRESOLVED_REFERENT）。
8. 三个 `productId` 符号三个值域：Inventory 别名→material_id；TraceCode 别名→target_id（多态）；SalesOrderDetail→food_id（Food 域）。

## 12. Semantic Findings（汇总）

- **SF-1（R1）**：双列 value-domain mismatch（已证实）+ product_id 列业务意义未决（INCONCLUSIVE）。
- **SF-2（R4）**：provenance gap——product 校验值未转换进入 material 余额键；数值碰撞（1↔1）即错账；活跃实例存在（OtherInbound）。
- **SF-3（R3）**：polymorphic referent 无 discriminator——多类 target 同表、出库无条件作 material_id；结构性跨域。
- **SF-4（R2/R5）**：同名异域——`food_id` 在 order_items=Food、在 purchase_request_item=Material 值；`productId` 符号横跨三个值域。
- **SF-5（R2 正面）**：Food→Material 的唯一健康转换是 recipe 介导（dish_recipes.material_id），值域正确。

## 13. Owner Questions（候选清单——未提交、未修改 Owner Decision Request 001）

| 编号 | 问题 | 来源 |
|---|---|---|
| OQ-A（新，候选） | 菜品追溯码（type 1）出库是否允许影响 material 库存？出库扣减是否必须限定 target_type=原料批次？ | R3 |
| OQ-B（新，候选） | 其他入库/报损单据的业务对象域：product 校验与 material 落库必须取一，业务上入的是谁的货？ | R4 |
| 沿用 | Owner Decision Request 001 Q1/Q2/Q3（inventory 业务对象范围）——A2 为其补充 FK 不对称与同值异指血证，问题本身不变 | R1 |

## 14. Governance Repairs

1. `a1-local-evidence-001` §17 W15 的"value provenance caveat"→ 经本轮**升级为已证实缺陷**（A2-R3 FAIL）；不修改 A1 文件，以本报告为准。
2. Reconstruction referential matrix 增补：`food_trace_codes 多态 target_id`、`OrderNew recipe 介导转换（正面锚点）`、`OtherInbound product→material 落库`。
3. Reconstruction §10-C8/候选"Unknown 无 referent 行"维持 SUPERSEDED（A1 已改）；本轮无新增翻案。
4. 历史记录一律不修改。

## 15. Final A2 Verdict

```text
A2_RESULT = FAIL
UNRESOLVED_ATTACK = YES
```

**FAIL 组成**：

- **A2-R3 = FAIL**：多态追溯表（菜品/原料同表设计）+ 出库无 discriminator + target_id 无条件作 material_id + 活跃入口——结构性跨域缺陷，能影响真实库存。
- **A2-R4 = FAIL**：Product 域校验值未转换进入 material_id——OtherInbound 为**可执行实例**（活跃入口、映射定位器、单事务真实写余额），LossOutbound 为潜在实例。
- **A2-R2 = FAIL**（经 R3 交叉依赖）：Food 域值存在进入 material_id 的活跃路径。

**INCONCLUSIVE 组成**：

- **A2-R1 = INCONCLUSIVE**：双列值域不同已证实（PASS 排除），但 product_id 列无活跃读写方，业务影响无法在当前证据下判定（依赖 Owner Q1/Q3 + 生产侧）。
- R3/R4 的本地数据实例均为 0、生产未知——缺陷的**代码级存在**已证，**生产级发生**未知。

**PASS 被排除的原因**：R1 INCONCLUSIVE（不得整体 PASS）+ R3/R4 两项已证 active referential defect。

**残余不确定性**：生产侧全部分布未知（PROD BLOCKED）；food_trace_codes/other_inbound/loss_outbound 的生产实例化状态未知；999999 来源未明。

---

## 最终声明

> **A2 不决定 Inventory 的最终业务身份。**
>
> A2 只提供 referential/value-domain engineering evidence（本报告 §11–§13）。
>
> 最终业务身份仍由 Owner Decision（Q1/Q2/Q3 及治理层对 OQ-A/OQ-B 的处置）决定。

本轮停止。不自行进入 A3、Owner Decision、Schema 或 Migration。
