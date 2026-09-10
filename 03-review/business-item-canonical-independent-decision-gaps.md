# Business Item Canonical — Independent Decision Gaps

> **任务**: ARCHITECTURE / BUSINESS SEMANTICS MASTER BRIEF — INDEPENDENT REVIEW
> **配套文件**: `business-item-canonical-independent-review.md`（完整论证）、`business-item-canonical-independent-conflict-registry.yaml`（冲突与幽灵对象台账）
> **本文件用途**: 登记**必须由决策者裁决**的问题。本文件**不做裁决**。
> **PD-CANONICAL-001 状态**: 维持 `RECOMMENDED / NOT CONFIRMED`
> **结论**: `MAJOR_REVISION_REQUIRED`

---

## 0. 阅读顺序（本文件的依赖结构）

本次审查发现的问题**不能并行裁决**——存在严格的前置关系：

```
[第 0 层] 证据修复 —— 生产库只读扫描（BE-01 ~ BE-12）
      ↓  （无此层，下面全部是猜测）
[第 1 层] 命名空间冲突裁决（BIC-001）—— 谁是物品真相
      ↓
[第 2 层] 主数据去重裁决（BIC-008）—— 同一物被建两条记录怎么办
      ↓
[第 3 层] Identity 是否分层（BIC-002 / BIC-005 / BIC-010）
      ↓
[第 4 层] 模型选择（A / B / C / D）与 Scope 修订
      ↓
[第 5 层] 架构与工程落地
```

**关键判断**：**第 1 层（BIC-001）必须在模型选择之前裁决**。理由：`inventory_id=4` 一行同时持有 `material_id`（娃娃菜）与 `product_id`（测试物料A）两个语义无关的外键。在这种冲突存在的情况下，任何 Identity 模型（含 Option B 与 Option D）都无法自证其解释力。

---

## 1. 第 0 层：证据修复（Evidence Gaps）—— 无 Owner 决策，但必须先做

> 不做这一层，下述所有决策都是基于开发库 11 行数据外推。
> 依据：`03-review/production-data-snapshot-001.md:6` 明确「Agent 不得在开发环境伪造生产结果」，其 5 个生产扫描**全部仍为「⏸ 待执行」**。

| ID | 待验证事实 | 阻断的决策 | 技术归属 | 复现命令 |
|---|---|---|---|---|
| **BE-01** | 生产库 `inventory` 中 `product_id` 与 `material_id` 的填充率与冲突数 | DG-01 | DBA/运维 | `SELECT count(*) FILTER (WHERE product_id IS NOT NULL) prod, count(*) FILTER (WHERE material_id IS NOT NULL) mat, count(*) FILTER (WHERE material_id IS NOT NULL AND product_id IS NOT NULL AND material_id<>product_id) conflict FROM inventory;` |
| **BE-02** | 生产库是否存在同 `(material_id, warehouse_id[, batch_no])` 重复 | DG-03, DG-09 | DBA/运维 | `SELECT material_id,warehouse_id,batch_no,count(*) FROM inventory GROUP BY 1,2,3 HAVING count(*)>1;` |
| **BE-03** | 生产库 `inventory` 与 `store_inventory` 对同一 material 的数量差异 | DG-05 | DBA/运维 | review 文档 §8 Q4 |
| **BE-04** | 生产库 `inventory.store_id` / `location_id` 填充率（开发库均为 0/11） | DG-05, DG-12 | DBA/运维 | `SELECT count(store_id), count(location_id), count(*) FROM inventory;` |
| **BE-05** | 生产库 `inventory.expiry_date` / `batch_no` 填充率（开发库 0/11 与 4/11） | DG-06 | DBA/运维 | `SELECT count(*), count(batch_no), count(expiry_date) FROM inventory;` |
| **BE-06** | 生产库 `inventory_transactions` 的孤儿 `inventory_id` 比例 | DG-09 | DBA/运维 | review 文档 §8 Q5 |
| **BE-07** | 生产库 `material_archives` 的重复命名规模 | DG-02 | DBA/运维 | `SELECT material_name, count(*) FROM material_archives GROUP BY 1 HAVING count(*)>1 ORDER BY 2 DESC;` |
| **BE-08** | 生产库 `purchase_request_item.food_id` 的非法值比例 | DG-01 | DBA/运维 | `SELECT count(*) FROM purchase_request_item pri WHERE NOT EXISTS (SELECT 1 FROM material_archives ma WHERE ma.material_id::text = pri.food_id);` |
| **BE-09** | 是否存在调用 `/v1/inventory/unit` 或 `/v1/inventory/category` 的真实客户端 | DG-13 | 运维/网关日志 | 网关访问日志检索 |
| **BE-10** | 生产库金额列的实际口径分布 | DG-14 | DBA/运维 | `SELECT count(*) FILTER (WHERE unit_cost>100000) a, count(*) FILTER (WHERE cost_price>1000) b FROM inventory;` |
| **BE-11** | `dish_recipes` 在缺唯一约束下的重复规模 | DG-10 | DBA/运维 | `SELECT food_id, material_id, count(*) FROM dish_recipes WHERE deleted=0 GROUP BY 1,2 HAVING count(*)>1;` |
| **BE-12** | 是否存在以 `version=0` 过滤有效行的视图/快照/客户端代码 | DG-11 | 工程/前端 | 检索视图定义与 Flutter 查询 |

**门禁建议**：BE-01 ~ BE-12 **全部完成后**才可进入第 1 层决策。若 BE-01 显示生产库存在 `material_id <> product_id` 的行，则 BIC-001 从「开发库孤例」升级为「生产级数据污染」，需立即启动数据修复而非模型讨论。

---

## 2. 第 1 层：命名空间冲突裁决（最优先）

### DG-01 —— `inventory.product_id` 与 `material_id` 谁是物品真相

| 字段 | 值 |
|---|---|
| **层级** | **PRODUCT DECISION** |
| **Owner** | Product Owner |
| **优先级** | **P0 · CRITICAL** |
| **阻断** | 所有 Identity 模型选择（A/B/C/D）、AD-IDENTITY-001、AD-INVENTORY-002、DS-001/002/004/005/015 |
| **关联冲突** | BIC-001、BIC-014(CONF-01, CONF-07) |
| **前置** | BE-01 |

**问题**：`inventory` 行同时持有 `product_id`（有物理外键 → `product`）与 `material_id`（无外键 → `material_archives`），实测在 `inventory_id=4` 上分别指向「测试物料A」与「娃娃菜」——**两个语义无关的物品**。业务上应以哪一列为物品真相？

**选项**：

- **Option A — `material_id` 为唯一真相，`product_id` 废弃**
  - 依据：所有 ORM 写路径都写 `material_id`（实测填充 11/11 vs 1/11）；`Inventory.java:382-388` 已把 `getProductId()` 实现为 `materialId` 的别名。
  - 代价：需删除 `fk_inv_product_id` 与 11 个产品口径列（当前被官方记录为「保留不删」）。
  - 风险：`product` 表仍有 5 处活跃调用 + 每日 `@Scheduled` 全表扫描。

- **Option B — `product_id` 为物品真相，`material_id` 为目标态**
  - 依据：全库唯一物理外键指向 `product`。
  - 代价：与 11/11 的实际写入相悖，等于宣布现行写入路径错误。
  - 风险：`product` 表仅 1 行测试数据，无法承载 35 个物料。

- **Option C — 两者为不同层，各自保留（需先定义层）**
  - 依据：本次审查的 L1/L2/L3 假设。
  - 代价：**必须先完成 DG-03（是否分层）**，形成循环依赖；且本审查已实测两列**不构成层级关系**（它们指向不同物品，不是同一物品的不同粒度）。

- **Option D — 中断 `product_id` 写入，冻结该列并标记为待清理**
  - 依据：最小风险；不立即删除结构。
  - 代价：双口径继续存在，需在应用层禁止写入。

**审查者观察（非推荐）**：Option C 在本次实测下**不成立**——层级模型要求两列描述同一物品的不同粒度，而实测是不同物品。Option A 与 Option B 的取舍**取决于 `product` 表在生产库是否仍有真实数据**（BE-01），因此**本项无法在 BE-01 完成前裁决**。

**需要 Product Owner 提供的信息**：
1. `product` 表在业务上代表什么？（当前仅有 1 行「测试物料A」，且它是**测试数据**）
2. 是否存在「同一物品既需要产品档案又需要物料档案」的真实业务场景？
3. 打包盒/电磁炉/洗剂这类非食材，业务上算「产品」还是「物料」？

---

### DG-02 —— 主数据重复定义的去重规则

| 字段 | 值 |
|---|---|
| **层级** | **PRODUCT DECISION** |
| **Owner** | Product Owner |
| **优先级** | **P0 · BLOCKER** |
| **阻断** | 分层建模（L1 definition_key 的建立）、迁移 |
| **关联冲突** | BIC-008 |
| **前置** | BE-07 |

**问题**：`material_archives` 35 行中存在重复定义（`Test Tomato` ×7、`E2E Potato` ×2），另有 5 行明显的测试污染（`测试采购物料`、`E2E Material 160258715`、`UI测试物料B`、`Test Tomato 154027/154237`）。在建立任何 definition 层之前，必须先定义「同一物」的判定与合并规则。

**选项**：

- **Option A — 先清理测试数据，不合并业务重复**（范围最小）
- **Option B — 建立去重规则 + 保留合并审计**（推荐路径需 Owner 确认）
- **Option C — 保留全部历史行，用 `status` 标记废弃**（最低风险，但 definition_key 仍不唯一）

**需要 Product Owner 提供的信息**：
1. 生产库是否存在真实业务上的同物多档（而不仅是测试数据）？
2. 合并历史记录时，历史单据的引用应指向哪一条（原记录 / 合并目标 / 新记录）？

---

## 3. 第 2 层：Identity 是否分层

### DG-03 —— Identity 是否分层（单层 / 三层 / 按类型）

| 字段 | 值 |
|---|---|
| **层级** | **PRODUCT DECISION** |
| **Owner** | Product Owner |
| **优先级** | **P0 · CRITICAL** |
| **阻断** | PD-CANONICAL-001 全部 18 项、AD-IDENTITY-001、AD-INVENTORY-002 |
| **关联冲突** | BIC-002、BIC-005、BIC-010、BIC-014(CONF-12) |
| **前置** | DG-01、DG-02 |

**问题**：Identity 应实现为单层（Option B）、三层（Option D：Definition → Commercial Unit → Inventory Holding）、还是按类型独立（Option A）？

**本次实测提供的新事实（供裁决参考）**：

| 层 | 是否已物理存在 | 载体 |
|---|---|---|
| Product Definition | **是，但是三份**（`foods` / `material_archives` / `product`，彼此零引用） | 3 张表 |
| Commercial Unit | **否**（价格/规格/状态被内嵌进各定义表） | 0 张表 |
| Inventory Holding | **是，但是两份且不一致**（对同一物料给出不同数量） | 2 张表 |

**关键裁决要素**：
1. 「以箱采购、以罐核算」是否为真实业务要求？（决定 Commercial Unit 层是否必须存在）
2. 仓库持仓与门店持仓是同一持仓的两种视图，还是两个独立持仓？（决定 I3 是 1 层还是 2 层）
3. 实物资产（电磁炉/汤桶）与食材是否共用同一物品身份与同一库存表？（实测：是，且无任何标记）

**选项**：

- **Option A — 单层统一（Option B 的实质）**：概念最少，但须增生 ≥4 个额外身份维度才能解释现有 DB（包装/单位/批次/商业类型），且无法解释 `inventory_id=4`。
- **Option B — 三层（Option D）**：描述力最强，但 **Commercial Unit 层在工程上完全不存在，需新建**；且**未回答命名空间冲突的裁决方式**。
- **Option C — 按类型独立（Option A 的实质）**：与现状最一致，但现状已被证明产生可观测数据错误（`parseLong` 静默 0L）。
- **Option D — 先不分层，先修复数据与命名空间**：把模型选择推迟到 BE-01~BE-12 与 DG-01/DG-02 完成之后。

**审查者观察（非推荐）**：本审查**不推荐任何选项**。四者在 brief §4 的七个评估维度上**各有未通过的维度**（详见 review 文档 §3.5）。**Option D（分层）在本次实测下有两项未通过**：(a) 依赖不存在的 Commercial Unit 层；(b) 未回答命名空间冲突。**Option B（单层）有一项决定性问题**：无法解释 `inventory_id=4`。

**需要 Product Owner 提供的信息**：
1. 连锁化后是否要求「同一物品在不同门店以不同单位交易」？
2. 是否接受「物品身份需要跨三层映射」带来的运维复杂度？

---

### DG-04 —— Commercial Unit 是否必须成为独立概念

| 字段 | 值 |
|---|---|
| **层级** | **PRODUCT DECISION** |
| **Owner** | Product Owner |
| **优先级** | **P1 · BLOCKER（若采纳分层）** |
| **阻断** | DG-03 的 Option B、DE-SKU-001、DS-011/012/013/014 |
| **关联冲突** | BIC-005、BIC-012 |

**问题**：系统是否需要「商业单位」作为独立于物品定义的身份层？

**实测现状**：**完全不存在**。价格（`foods.sale_price` / `dish_combos.combo_price` / `product.price` / `material_archives.reference_price`）、规格（`spec` / `specification` 文本列）、状态（`status`）全部内嵌进各自定义表。全库**没有任何一个物料拥有多个包装层级的记录**。

**本次实测的反例（BIC-005）**：`圆形打包盒(500ml)` 与 `长方形打包盒(750ml)` 的形状与容量被压进同一个 `spec` 文本列——**单列 spec 无法区分「形状」与「容量」两种语义**。

**需要 Product Owner 提供的信息**：
1. 业务上是否存在「同一物品、不同包装同时销售」的场景？（如整箱与单罐）
2. 门店零售（若未来出现）是否要求与餐饮不同的单位体系？

---

### DG-05 —— 仓库持仓与门店持仓是否为同一持仓

| 字段 | 值 |
|---|---|
| **层级** | **PRODUCT DECISION** |
| **Owner** | Product Owner |
| **优先级** | **P0 · BLOCKER** |
| **阻断** | AD-INVENTORY-002、AD-LOCATION-001、DS-017、L2-005 |
| **关联冲突** | BIC-002、BIC-007、BIC-014(CONF-02) |
| **前置** | BE-03、BE-04 |

**问题**：`inventory`（仓库）与 `store_inventory`（门店）对同一物料给出不同数量（实测 4 例差异：娃娃菜 101 vs 71、虾滑 40 vs 70、汤桶 0 vs 2、洗剂 0 vs 20），且两表互不约束。业务上它们是同一持仓的两个视图，还是两个独立持仓？

**选项**：

- **Option A — 同一持仓的两个视图**：必须收敛为一张表或建立同步机制；当前 4 例差异即数据错误。
- **Option B — 两个独立持仓**：必须明确「总库存 = 仓 + 店」的语义与对账规则；当前差异属正常。
- **Option C — 门店持仓是仓库持仓的下级（库位）**：需先定义 Location 层级。

**需要 Product Owner 提供的信息**：
1. 门店库存与仓库库存是否应始终可对账？差异容忍度是多少？
2. 调拨在途期间，货物归属哪个持仓？

---

### DG-06 —— 批次/效期是否构成持仓身份维度

| 字段 | 值 |
|---|---|
| **层级** | **PRODUCT DECISION** |
| **Owner** | Product Owner |
| **优先级** | **P0 · BLOCKER** |
| **阻断** | DS-016、AD-INVENTORY-002、召回能力 |
| **关联冲突** | BIC-010 |
| **前置** | BE-05 |

**问题**：批次与效期是否构成库存持仓的身份维度（即 `(物料, 位置, 批次)` 是否为持仓主键）？

**实测现状（两套事实，其中一套空转）**：
- `inventory.batch_no`：4/11 填充；`expiry_date` 与 `production_date`：**0/11 填充**
- `store_inventory`：**无任何批次/效期列**
- 追溯码空间（`material_trace_code` / `food_trace_codes` / `expiry_alert_record`）：持有完整批次与效期
- `InventoryServiceImpl.java:356-359` 的效期预警读取的是**空的那一套**

**需要 Product Owner 提供的信息**：
1. 食品安全召回是否必须以批次定位到具体库存行？
2. 门店库存是否也需要批次追溯？

---

## 4. 第 3 层：架构决策

| ID | 问题 | 层级 | Owner | 优先级 | 关联冲突 | 前置 |
|---|---|---|---|---|---|---|
| **DG-07** | Product Definition 空间的收敛方式（`foods`/`material_archives`/`product` 三选一或建映射表） | ARCHITECTURE | Architecture Owner | P0 | BIC-001 | DG-01, DG-02 |
| **DG-08** | Commercial Unit 的存储形态（新表 / JSONB / 复用 `product` 表） | ARCHITECTURE | Architecture Owner | P1 | BIC-005 | DG-04 |
| **DG-09** | 库存台账与持仓表的结构收敛：外键、不可变性、7 个孤儿 `inventory_id` 的清理 | ARCHITECTURE | Architecture Owner | P0 | BIC-003 | BE-06 |
| **DG-10** | 两张余额表的收敛与精度统一（`integer` → `numeric`） | ARCHITECTURE | Architecture Owner | P0 | BIC-002, BIC-012 | DG-05 |
| **DG-11** | `store_id` 的类型与来源统一（`VARCHAR` vs `BIGINT`；仓库键冒充门店键） | ARCHITECTURE | Architecture Owner | P0 | BIC-007 | BE-04 |
| **DG-12** | Location 模型：是否存在统一 Location 抽象，Transit 归属 | ARCHITECTURE | Architecture Owner | P1 | BIC-007 | DG-05 |
| **DG-13** | 失效 API 端点处置（`/v1/inventory/unit`、`/v1/inventory/category`：建表或下线） | ENGINEERING | 工程团队 | P1 | BIC-011 | BE-09 |
| **DG-14** | `dish_recipes` 唯一键与 BOM 去重 | ENGINEERING | 工程团队 | P0 | BIC-009 | BE-11 |
| **DG-15** | `version=0` 双重语义（乐观锁 + 删除墓碑）的消除 | ENGINEERING | 工程团队 | P1 | BIC-013 | BE-12 |
| **DG-16** | 金额口径统一（`unit_cost` 分 vs `cost_price` 元） | ENGINEERING | 工程团队 | P1 | BIC-012 | BE-10 |

---

## 5. PD-CANONICAL-001 的处置建议

### 5.1 建议动作（按顺序）

| # | 动作 | 依据 |
|---|---|---|
| 1 | **撤回** `decision-record.md:337` 的 `READY_FOR_PRODUCT_OWNER_CONFIRMATION` 声明 | 违反 `05-governance/governance-baseline-summary.md:87` 的「禁止强行 Ready」规则 |
| 2 | 把 18 项 Scope 的 `status: RESOLVED` **全部回退** 为 `RECOMMENDED` / `OPEN` | 同文件内矩阵规则仍标 `CANDIDATE (NOT CONFIRMED)` |
| 3 | 重新分类 `evidence_status: VERIFIED` | 本审查已重分类 14 条：`INVALID 5` / `CONFLICT 2` / `PARTIAL 3` / `UNVERIFIED 1` |
| 4 | **新增** 一个 Scope Item 覆盖套餐（`dish_combos`） | 实测套餐是独立主数据表 + 独立计价类型，18 项**无一条覆盖** |
| 5 | **新增** 一个 Scope Item 覆盖命名空间冲突（BIC-001） | 该冲突未被任何现有 Scope 覆盖 |
| 6 | 补充 DS-012 的编号污染修正 | 同一 ID 在 YAML 与 markdown 中指向不同属性（`Pack Size` vs `UOM`） |
| 7 | 修复 §Scope 统计 | YAML 称 `5 IDENTITY_BOUNDARY + 12 NOT = 17`，实际表体为 `4 + 12 = 16` |

### 5.2 明确禁止

1. **禁止**在 DG-01 与 DG-02 裁决前确认任何 Identity 模型（含 Option B 与 Option D）。
2. **禁止**把本审查的任何结论直接写入 `04-decision-history/PD-CANONICAL-001*` 并改为 `CONFIRMED`。
3. **禁止**以「保持项目进度」为由把 `MAJOR_REVISION_REQUIRED` 降级。
4. **禁止**引用 `stock_item` / `inventory_item` / `inventory_ledger` / `inventory_balance` / `is_food` / `unit_conversion` / `sku_code` / `is_stockable` / `product_batches` 作为现行结构（全部实测不存在）。
5. **禁止**在任何任务输入中引用 `inventory_unit` / `inventory_category` **作为现存表**（表不存在，仅端点存在）。

---

## 6. 门禁（Gate）

```
GATE-0  生产库只读扫描 BE-01..BE-12 完成          -> 否则不得进入 GATE-1
GATE-1  DG-01（命名空间冲突）裁决                  -> 否则不得进入 GATE-2
GATE-2  DG-02（主数据去重）裁决                    -> 否则不得进入 GATE-3
GATE-3  DG-03 + DG-05 + DG-06（分层/持仓/批次）裁决 -> 否则不得进入 GATE-4
GATE-4  DG-04（Commercial Unit）裁决               -> 否则不得进入 GATE-5
GATE-5  模型选择 + PD-CANONICAL-001 Scope 修订     -> 否则不得进入工程化
```

**当前状态**：位于 **GATE-0 之前**。所有 12 项生产扫描仍为「⏸ 待执行」。

---

## 7. 声明

1. **本文件不做裁决**。所有选项均为供决策者选择的输入。
2. **本审查未推荐任何 Option**。A/B/C/D 在 brief §4 的七个维度上各有未通过项。
3. **未修改任何 Decision 文件**；`PD-CANONICAL-001` 维持 `RECOMMENDED / NOT CONFIRMED`。
4. **本审查推翻了自己上一轮的一条结论**（重复行不存在）并更正了一条（`in_transit` 存在）——审查者结论受与被审文档相同的证据标准约束。
