# Production Read-Only Evidence Gate — 001

> **任务编号**: PRODUCTION-EVIDENCE-GATE-001
> **任务类型**: READ-ONLY · PRODUCTION EVIDENCE COLLECTION · NO SEMANTIC DECISION
> **执行日期**: 2026-09-11 01:38 ~ 01:52 (UTC+08:00)
> **数据库**: PostgreSQL 18.3 `food_traceability` @ `localhost:5432`
> **权限声明**: 全部为只读 `SELECT`；**未执行任何 UPDATE / DELETE / INSERT / DDL / Migration**；未修改代码、API、前端、Decision
> **PD-CANONICAL-001**: 维持 `RECOMMENDED / NOT CONFIRMED`（未修改）
> **GATE-0 VERDICT**: `BLOCKED`

---

## 0. 结论速览

| 项 | 结果 |
|---|---|
| **GATE-0** | **`BLOCKED`** |
| 根因 | **生产环境不可访问**——不存在独立的生产数据库实例、无生产凭证、无生产主机配置 |
| 已执行 | BE-01 ~ BE-12 全部 12 项查询**已执行完毕** |
| 已执行查询的环境 | **LIVE_LOCAL_INSTANCE**（本机 PostgreSQL，包含测试数据，尚在变动中） |
| 是否构成 `PRODUCTION_EVIDENCE` | **否**（依 brief §20：不得根据开发库结果推断生产） |
| 12 项中有可分级的本地发现 | 10 项 |
| 需生产环境才能定论 | 12 项全部 |

**关键区分（对应成功标准 §22）**

| 分类 | 本任务可得结论 |
|---|---|
| **Production Reality** | **无法判定** —— 无生产环境可查 |
| **Development Pollution（已被证实）** | 13 行软删测试物料确实存在于本机实例；生产是否存在**未知** |
| **仍然未知** | BE-01 ~ BE-12 的生产侧取值（全部） |

---

## 1. Production Environment

### 1.1 生产环境可达性判定（本次 Gate 的决定性调查）

| 检查项 | 结果 | 证据 |
|---|---|---|
| `application-prod.yml` 数据源 | `jdbc:postgresql://${PG_HOST:localhost}:${PG_PORT:5432}/${PG_DB_NAME:food_traceability}` | `backend/src/main/resources/application-prod.yml:46` |
| 生产必需环境变量 | **全部未设置**：`PG_HOST` / `PG_PORT` / `PG_DB_NAME` / `PG_USERNAME` / `PG_PASSWORD` / `APP_ENCRYPTION_KEY` / `CONFIG_ENCRYPT_KEY` / `AES_SECRET_KEY` / `JWT_SECRET` 均不存在 | `Get-ChildItem Env:` 实测 |
| 本机 PostgreSQL 实例 | **仅 1 个**：`postgresql-x64-18`（Running） | `Get-Service` 实测 |
| 可达数据库 | **仅 2 个**：`food_traceability`（44 MB）、`postgres`（8 MB） | `SELECT datname FROM pg_database` |
| `food_traceability_test` | **不存在**（`application-test.yml:14` 指向它，连接失败） | 实测报错 `数据库 "food_traceability_test" 不存在` |
| 监听端口 | 5432–5435 无 `Get-NetTCPConnection` 监听记录（psql 仍可经 localhost 连接，见 §1.3） | 实测 |
| 独立生产主机 | **文档中不存在任何生产主机名/IP 配置** | 全仓 `application*.yml` 检索 |

### 1.2 文档层的环境口径（HISTORICAL_EVIDENCE）

| 文档 | 陈述 | 与实测的关系 |
|---|---|---|
| `docs/quality/production-deployment-report.md:8,17` | 生产环境为 `localhost:5432`，profile=pg，端口 8081 | 与「本机实例即生产」一致 |
| `production-known-limitations.md:496` | 「`SalesOrder.java` 映射的表在**生产库不存在**（**活体库**实际订单表为 `orders`）」 | 使用「**活体库**」而非独立生产库 |
| `docs/architecture/03-review/production-data-snapshot-001.md:6` | 「**Agent 不得在开发环境伪造『生产结果』**；实际结果由 DBA/运维在**生产 DB** 执行后回填」 | 明确区分开发环境与生产 DB |
| 同上 `:30-34, 85-94` | 5 个生产扫描脚本**全部为「⏸ 待执行」** | 项目自身认定生产扫描尚未进行 |

**口径冲突**：部署报告将本机 `localhost` 称为生产；生产数据快照则要求由 DBA/运维在「生产 DB」执行且尚未执行。**两种口径无法同时成立。**

### 1.3 本次查询环境的实际状态（LIVE_LOCAL_INSTANCE）

- 服务器时间（实测）：`2026-09-11 01:38:45.714305+08`
- `flyway_schema_history`：**205 条**，全部 `success=t`；首条 `2026-07-23 12:57:26`，末条 `2026-09-09 14:17:24`（rank 205 = `20260909.003`）
- 表数量（实测）：**305** 张（`pg_class relkind='r'`, public）
- 连接活动：仅本次审查的 `psql` 会话，无应用进程连接
- **测试指纹（决定性）**：`stores` 表仅 2 行，名为 **`测试门店A` / `测试门店B`**；`users` 含 `admin`/`store_manager`/`purchaser`/`finance`/`chef` 测试账号

**判定**：该实例承载**开发/测试数据**（测试门店、测试账号、E2E 物料），**不满足 brief §20 对 Production Evidence 的要求**。

### 1.4 环境归类（每条证据必须标注，不得混用）

| 等级 | 本次适用范围 |
|---|---|
| `PRODUCTION_EVIDENCE` | **无** —— 无可达生产环境 |
| `LIVE_LOCAL_INSTANCE_EVIDENCE` | BE-01 ~ BE-12 的全部查询结果（本机实例，含测试数据） |
| `CODE_EVIDENCE` | 迁移脚本、实体、Controller、日志文件 |
| `HISTORICAL_EVIDENCE` | `docs/quality/*`、`production-*.md`、`03-review/*` 既有文档 |
| `SYNTHETIC_COUNTEREXAMPLE` | **无** —— 本任务未构造任何合成案例 |

---

## 2. BE-01 — `inventory.product_id` / `material_id`

**Finding**：本机实例中 `material_id` 填充 **100%**，`product_id` 填充 **9.09%**（仅 1 行）；二者同时存在的仅 **1 行**，且该行两列指向**语义无关的不同物品**；`material_id <> product_id` 的行数为 **0**——因为唯一同时填充的行恰好两列取值相同（均为 1），但**所指物品不同**。`product` 表仅 1 行且为测试数据。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

```sql
-- BE01.1
SELECT count(*) total,
       count(product_id) product_id_filled,
       round(100.0*count(product_id)/count(*),2) product_id_pct,
       count(material_id) material_id_filled,
       round(100.0*count(material_id)/count(*),2) material_id_pct,
       count(*) FILTER (WHERE product_id IS NOT NULL AND material_id IS NOT NULL) both_filled,
       count(*) FILTER (WHERE product_id IS NOT NULL AND material_id IS NOT NULL AND material_id <> product_id) mismatched
FROM inventory WHERE deleted = 0;
```

**Result**

| 指标 | 值 |
|---|---|
| total_rows | **11** |
| product_id_filled | **1**（9.09%） |
| material_id_filled | **11**（100.00%） |
| both_filled | **1**（9.09%） |
| material_id <> product_id | **0**（0.00%） |
| product 表行数 | **1** |
| product 表测试类行 | **1**（`name = 测试物料A`，正则 `test|e2e|ui|测试` 命中） |

**Sample rows（身份解析）**

| inventory_id | material_id | material_side | product_id | product_side | verdict |
|---|---|---|---|---|---|
| 4 | 1 | 娃娃菜 | 1 | 测试物料A | **DIFFERENT ITEM** |
| 5 | 1 | 娃娃菜 | NULL | NULL | product_id NULL |
| 6 | 999999 | NULL | NULL | NULL | product_id NULL |
| 13 | 11 | 蟹棒 | NULL | NULL | product_id NULL |
| 14 | 10 | 虾滑 | NULL | NULL | product_id NULL |
| 30 | 21 | 不锈钢汤桶 | NULL | NULL | product_id NULL |
| 31 | 26 | NULL | NULL | NULL | product_id NULL |
| 32 | 1 | 娃娃菜 | NULL | NULL | product_id NULL |
| 33 | 6 | 撒尿牛丸 | NULL | NULL | product_id NULL |
| 34 | 35 | 二硫化硒洗剂 | NULL | NULL | product_id NULL |
| 35 | 22 | NULL | NULL | NULL | product_id NULL |

**FK / Constraints（实测 `pg_constraint`）**

```
inventory_pkey                  PRIMARY KEY (inventory_id)
chk_inventory_stock_nonneg      CHECK (current_stock >= 0 AND safety_stock >= 0)
chk_inventory_deleted_domain    CHECK (deleted = ANY (ARRAY[0,1]))
fk_inv_product_id               FOREIGN KEY (product_id) REFERENCES product(product_id)
inventory_inventory_id_not_null NOT NULL inventory_id
inventory_version_not_null      NOT NULL version
```

**关键结论**：`product_id` 有物理外键；`material_id` **无任何外键**。

**Confidence**：HIGH（本地）／**生产侧 UNVERIFIED**
**Production / Development**：**Development（LIVE_LOCAL_INSTANCE）**
**Impact**：若生产同样存在 `both_filled > 0` 且两列所指物品不同，则「统一 Canonical Identity」在数据层不成立；本机实例的样本量为 1 行，**不足以外推生产**。

---

## 3. BE-02 — 重复规模

**Finding**：本机实例中 `(material_id, warehouse_id)` 与 `(material_id, warehouse_id, batch_no)` **均无重复组**；`inventory` 上唯一存在的唯一索引是主键 `inventory_pkey(inventory_id)`，**不存在任何业务唯一约束**。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

```sql
SELECT count(*) dup_groups, COALESCE(sum(n-1),0) excess_rows
FROM (SELECT material_id, warehouse_id, count(*) n FROM inventory WHERE deleted=0
      GROUP BY 1,2 HAVING count(*)>1) d;
```

**Result**

| 组合 | dup_groups | excess_rows |
|---|---|---|
| `(material_id, warehouse_id)` | **0** | **0** |
| `(material_id, warehouse_id, batch_no)` | **0** | **0** |

**唯一索引清单**

```
inventory_pkey  CREATE UNIQUE INDEX inventory_pkey ON public.inventory USING btree (inventory_id)
```

**关键结论**：**当前无重复数据可展示**。但**缺少业务唯一约束**这一结构性事实成立（对照 `store_inventory` 有 `uk_store_inventory_store_material`，`inventory` 没有对应约束）。

**Confidence**：HIGH（本地「无重复」与「无唯一键」两个事实均直接实测）
**Production / Development**：**Development**
**Impact**：重复风险是**结构性**而非**已发生**；不得表述为「生产存在重复」。生产侧需重跑同一查询。

---

## 4. BE-03 — 仓库数量 vs 门店数量

**Finding**：同一 `material_id` 在两张余额表中**数量不一致**；仓库独有 2、门店独有 7、两者共有 7，其中共有中 **4 个数量不一致**。`store_inventory` 另含一个 `material_id` 在 `material_archives` 中**不存在**。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

```sql
SELECT COALESCE(w.material_id, s.material_id) material_id,
       w.wh_qty, w.wh_unit, s.st_qty, s.st_unit,
       CASE WHEN w.material_id IS NULL THEN 'STORE_ONLY'
            WHEN s.material_id IS NULL THEN 'WAREHOUSE_ONLY' ELSE 'BOTH' END presence,
       (w.wh_qty - s.st_qty) diff
FROM (SELECT material_id, sum(current_stock) wh_qty, min(unit) wh_unit
      FROM inventory WHERE deleted=0 GROUP BY material_id) w
FULL OUTER JOIN (SELECT material_id, sum(current_stock) st_qty, min(unit) st_unit
      FROM store_inventory WHERE deleted=0 GROUP BY material_id) s
  ON w.material_id = s.material_id ORDER BY 1;
```

**Result**

| material_id | 仓库 | 门店 | presence | diff |
|---|---|---|---|---|
| 1 | 101 斤 | 71.000 斤 | BOTH | **+30.000** |
| 2 | — | 70.000 斤 | STORE_ONLY | — |
| 3 | — | 50.000 斤 | STORE_ONLY | — |
| 4 | — | 20.000 斤 | STORE_ONLY | — |
| 5 | — | 30.000 斤 | STORE_ONLY | — |
| 6 | 2 | 2.000 袋 | BOTH | 0.000 |
| 10 | 40 斤 | 70.000 斤 | BOTH | **−30.000** |
| 11 | 30 斤 | 30.000 斤 | BOTH | 0.000 |
| 12 | — | 20.000 包 | STORE_ONLY | — |
| 13 | — | 20.000 斤 | STORE_ONLY | — |
| 20 | — | 1.000 台 | STORE_ONLY | — |
| 21 | 0 个 | 2.000 个 | BOTH | **−2.000** |
| 22 | 150 | — | WAREHOUSE_ONLY | — |
| 26 | 100 | 100.000 jin | BOTH | 0.000 |
| 35 | 0 瓶 | 20.000 瓶 | BOTH | **−20.000** |
| 999999 | 70 | — | WAREHOUSE_ONLY | — |

**汇总**：`store_only = 7` · `warehouse_only = 2` · `both = 7` · `both_divergent = 4`

**Confidence**：HIGH（本地）
**Production / Development**：**Development**
**Impact**：「Inventory Holding 是一层」在两份实现上不成立。**差异数值本身是本地数据，不可外推生产**；但「两表无同步机制」是结构事实。

---

## 5. BE-04 — Location 列填充率与仓库 ID 冒用

**Finding**：`inventory.store_id` 与 `inventory.location_id` **填充率均为 0%**（11 行全 NULL）。`store_inventory.store_id` 出现 2 个不同值（1 与 2），均对应 `stores` 中的**测试门店**。`store_inventory_log.store_id` 出现 `1` 与 **`584`**；**`584` 既不是任何 `stores.store_id`，也不是任何 `warehouses.warehouse_id`**（stores=1,2；warehouses=1..10）——它是**孤儿值**。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

```sql
-- BE04.1
SELECT count(*) total, count(store_id) inv_store_id_filled,
       count(location_id) inv_location_id_filled, count(warehouse_id) inv_warehouse_id_filled
FROM inventory WHERE deleted=0;
```

**Result — BE-04.1**

| 列 | 填充 | 百分比 | distinct |
|---|---|---|---|
| `inventory.store_id` | **0 / 11** | **0.00%** | 0 |
| `inventory.location_id` | **0 / 11** | **0.00%** | 0 |
| `inventory.warehouse_id` | **10 / 11** | 90.91% | 2 |

**Result — BE-04.2 `store_inventory.store_id` 值分布**

| store_id | rows | 对应 stores 表 |
|---|---|---|
| 1 | 14 | 测试门店A |
| 2 | 2 | 测试门店B |

**Result — BE-04.3 `store_inventory_log.store_id` 值分布**

| store_id | rows | 是否存在于 stores |
|---|---|---|
| 1 | 14 | ✅ |
| **584** | **2** | ❌ **不存在** |

**Result — BE-04.4 仓库 ID 冒用检验**

| store_id_in_log | 匹配到的 warehouse_id |
|---|---|
| 1 | 1（⚠ `warehouses` 中确有 warehouse_id=1） |
| 584 | **NULL（无匹配）** |

**Result — BE-04.5 类型对照**

| 表 | 列 | 类型 |
|---|---|---|
| `inventory` | store_id | **bigint** |
| `orders` | store_id | bigint |
| `stores` | store_id | bigint |
| `store_inventory` | store_id | **character varying** |
| `store_inventory_log` | store_id | **character varying** |

**关键结论**：
1. `inventory.store_id` / `location_id` 在本实例**完全空转**（0%）。
2. **仓库 ID 冒用无法证实也无法否证**：`584` 是孤儿值，不匹配任何门店或仓库；其来源不可由数据库本身确定。**代码注释**（`InventoryTransferServiceImpl.java:226`：「warehouseId 作为 storeId」）提供了**代码级**支持，但**缺少生产数据级**证据。
3. `store_id` 在三张表间类型不一致（BIGINT vs VARCHAR），无法建立外键。

**Confidence**：MEDIUM（`584` 的成因未定）
**Production / Development**：**Development**
**Impact**：若生产同样为 VARCHAR 且含非门店 ID，则门店维度不可靠。

---

## 6. BE-05 — batch_no / expiry_date / production_date

**Finding**：`batch_no` 填充 **36.36%**（4/11，4 个唯一值，覆盖 4 个物料）；`expiry_date` 与 `production_date` **填充率均为 0%**。`store_inventory` **完全没有**批次/效期列。真正的批次与效期数据位于**追溯码空间**（`material_trace_code` 18 行，batch 13，expiry 8）。`food_trace_codes` **0 行**。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

```sql
SELECT count(*) total, count(batch_no) batch_filled,
       round(100.0*count(batch_no)/count(*),2) batch_pct,
       count(expiry_date) expiry_filled, count(production_date) proddate_filled
FROM inventory WHERE deleted=0;
```

**Result — BE-05.1**

| 指标 | 值 |
|---|---|
| total | 11 |
| `batch_no` 填充 | **4（36.36%）** |
| `batch_no` 唯一值 | **4** |
| 物料覆盖（distinct material_id） | **9** |
| `expiry_date` 填充 | **0（0.00%）** |
| `expiry_date` 唯一值 | **0** |
| `production_date` 填充 | **0** |
| `production_date` 唯一值 | **0** |

**Result — BE-05.2 批次行明细**

| inventory_id | material_id | material_name | batch_no | production_date | expiry_date |
|---|---|---|---|---|---|
| 4 | 1 | 娃娃菜 | BATCH-WH-001 | NULL | NULL |
| 13 | 11 | 蟹棒 | B20260727001 | NULL | NULL |
| 14 | 10 | 虾滑 | B20260727002 | NULL | NULL |
| 35 | 22 | NULL | SMOKE-WH-UPDATE-20260816 | NULL | NULL |

**Result — BE-05.3** `store_inventory` 批次/效期列：**查询返回空集**（无此类列）

**Result — BE-05.4 追溯码空间的批次事实**

| 表 | rows | batch_filled | expiry_filled |
|---|---|---|---|
| `material_trace_code` | **18** | **13** | **8** |
| `food_trace_codes` | **0** | 0 | 0 |

**关键结论**：**批次与效期存在两套事实**——库存侧 `expiry_date` 全空（效期预警逻辑读此列 ⇒ 本地恒为空集）；追溯码侧持有真实批次与效期。

**Confidence**：HIGH（本地）
**Production / Development**：**Development**
**Impact**：效期驱动逻辑在生产是否空转，取决于生产侧 `expiry_date` 填充率——**必须生产实测**。

---

## 7. BE-06 — `inventory_transactions` 完整性

**Finding**：27 行台账中 **17 行（62.96%）的 `inventory_id` 为孤儿**（指向不存在的持仓行）；**3 行持仓无任何流水**；该表**零外键**；**2 行 `transaction_type` 为 NULL**；同一 `inventory_id` 的多行流水 `before_qty` 链**大体连续但有 2 处起始值非 0**。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

```sql
SELECT count(*) total_ledger_rows, count(inventory_id) with_inventory_id,
       count(*) FILTER (WHERE inventory_id IS NOT NULL AND NOT EXISTS
         (SELECT 1 FROM inventory i WHERE i.inventory_id = t.inventory_id)) dangling,
       count(*) FILTER (WHERE transaction_type IS NULL) null_txn_type
FROM inventory_transactions t;
```

**Result — BE-06.1**

| 指标 | 值 |
|---|---|
| 台账总行数 | **27** |
| 带 inventory_id | **27** |
| **孤儿 inventory_id** | **17（62.96%）** |
| NULL transaction_type | **2** |

**Result — BE-06.2 孤儿样本（17 行，节选）**

| transaction_id | inventory_id | material_id | reference_type |
|---|---|---|---|
| 3 | 9 | 22 | adjust |
| 4 | 9 | 22 | purchase_stockin |
| 5 | 12 | 22 | purchase_stockin |
| 6 | 12 | 22 | purchase_stockin |
| 9 | 15 | 23 | purchase_stockin |
| 10–18 | 16 | 24 | purchase_stockin（9 行） |
| 19 | 24 | 31 | purchase_stockin |
| 20 | 26 | 32 | purchase_stockin |
| 21 | 28 | 33 | purchase_stockin |

**Result — BE-06.3 无流水的持仓行（3 行）**

| inventory_id | material_id | material_name | current_stock |
|---|---|---|---|
| 5 | 1 | 娃娃菜 | 30 |
| 30 | 21 | 不锈钢汤桶 | 0 |
| 34 | 35 | 二硫化硒洗剂 | 0 |

**Result — BE-06.4 外键**：查询返回**空集** —— `inventory_transactions` 与 `store_inventory_log` **均无任何外键约束**

**Result — BE-06.5 before/after 连续性**

| inventory_id | 行数 | 链 |
|---|---|---|
| 9 | 2 | `0.00->70.00` , `70.00->170.00` |
| 12 | 2 | `20.00->35.00` , `35.00->50.00` ⚠ 起始非 0 |
| 16 | 9 | `0.00->10.00` … `80.00->90.00`（连续） |
| 31 | 2 | `0.00->50.00` , `50.00->100.00`（连续） |
| 35 | 2 | `0.00->100.00` , `100.00->150.00`（连续） |

**Result — BE-06.6 NULL 枚举行**

| transaction_id | transaction_type | inventory_id | material_id | reference_type |
|---|---|---|---|---|
| 2 | NULL | 6 | 999999 | adjust |
| 3 | NULL | 9 | 22 | adjust |

**关键结论**：台账**无法重建余额**（62.96% 孤儿 + 零外键）。但需注意：余额表当前值（如 material 22 = 150）与台账链 `50.00->170.00` 的终值 170 **不一致**，说明二者已脱钩。

**Confidence**：HIGH（本地）
**Production / Development**：**Development**
**Impact**：若生产同样存在孤儿，则 Ledger-first 不可行；**生产侧比例必须实测**。

---

## 8. BE-07 — `material_archives` 重复与测试污染

**Finding**：**活跃行中不存在重复 `material_name`，不存在重复 `barcode`**；重复仅体现在 `spec` 文本（`冷冻`×5、`新鲜`×4，属正常枚举值而非重复）。**测试污染确实存在，共 13 行**，但**全部已被软删（`deleted=1`）**——因此活跃行（22 行）中污染率为 **0%**，而全表（35 行）中为 **37.14%**。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

```sql
SELECT count(*) FROM material_archives WHERE deleted=0;            -- 22
SELECT material_name, count(*) FROM material_archives
 WHERE deleted=0 GROUP BY 1 HAVING count(*)>1;                      -- 空集
```

**Result — BE-07.1 / 07.2 / 07.3**

| 检查 | 结果 |
|---|---|
| 活跃总行数 | **22** |
| 重复 `material_name` | **0 组**（空集） |
| 重复 `barcode` | **0 组**（空集；且活跃行 barcode 全部为 NULL/空） |
| 重复 `spec` | `冷冻`=5、`新鲜`=4（**规格枚举值，非重复物料**） |

**Result — BE-07.4 / 07.5 测试污染**

| 口径 | 命中 | 总数 | 百分比 |
|---|---|---|---|
| **活跃行**（`deleted=0`） | **0** | 22 | **0.00%** |
| **全表**（含软删） | **13** | 35 | **37.14%** |

**被软删的 13 行（`deleted=1`）**

| material_id | material_name | deleted |
|---|---|---|
| 22 | Test Cucumber | 1 |
| 23 | E2E Potato | 1 |
| 24 | E2E Potato | 1 |
| 25 | 测试采购物料 | 1 |
| 26 | Test Tomato | 1 |
| 27 | Test Tomato | 1 |
| 28 | Test Tomato | 1 |
| 29 | Test Tomato | 1 |
| 30 | Test Tomato | 1 |
| 31 | Test Tomato 154027 | 1 |
| 32 | Test Tomato 154237 | 1 |
| 33 | E2E Material 160258715 | 1 |
| 34 | UI测试物料B | 1 |

**⚠ 连带发现（重要）**：部分被软删的 `material_id` **仍被活跃业务数据引用**：

- `inventory` 行 31 → `material_id = 26`（Test Tomato，已软删）
- `inventory` 行 35 → `material_id = 22`（Test Cucumber，已软删）
- `store_inventory` 含 `material_id = 20/21`（活跃）及若干引用软删物料的可能

即：**软删主数据未级联处理引用**，形成「悬挂引用」。

**Confidence**：HIGH（本地）
**Production / Development**：**Development**
**Impact**：污染**在活跃数据中为 0**，此前「7 行 Test Tomato 存在于主数据中」的说法需修正为「存在于全表、均已软删」。**不得删除，仅统计**（依 brief §10）。

---

## 9. BE-08 — `purchase_request_item.food_id`

**Finding**：`food_id` 列为 **VARCHAR(32)**。50 行中 **49 行有值**，其中 **46 行（93.88%）解析后匹配 `material_archives.material_id`**，**仅 6 行匹配 `foods.food_id`**（且这些行同时也匹配 material）。**3 行无法解析为数字**，**1 行可解析但无任何匹配**。结论：**该列实际保存 material ID**。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

```sql
SELECT CASE WHEN food_id ~ '^[0-9]+$' AND EXISTS (SELECT 1 FROM material_archives ma
                WHERE ma.material_id::text = pri.food_id) THEN 'MATERIAL_ID'
            WHEN food_id ~ '^[0-9]+$' AND EXISTS (SELECT 1 FROM foods f
                WHERE f.food_id::text = pri.food_id) THEN 'FOOD_ID'
            WHEN food_id ~ '^[0-9]+$' THEN 'NUMERIC_BUT_NO_MATCH'
            ELSE 'UNPARSEABLE' END verdict, count(*)
FROM purchase_request_item pri GROUP BY 1 ORDER BY 2 DESC;
```

**Result — BE-08.1 / 08.3 判定分布**

| verdict | 行数 | 占比 |
|---|---|---|
| **MATERIAL_ID** | **46** | **92.00%** |
| **UNPARSEABLE** | **3** | **6.00%** |
| **NUMERIC_BUT_NO_MATCH** | **1** | **2.00%** |
| FOOD_ID（仅匹配 foods） | **0** | 0.00% |

| 指标 | 值 |
|---|---|
| 总行数 | **50** |
| food_id 填充 | **49** |
| 可解析为数字 | **47** |
| 不可解析 | **3** |
| 匹配 material_archives | **46** |
| 匹配 foods | **6**（与 material 匹配重叠） |

**Result — BE-08.2 样本（节选）**

| item_id | food_id | food_name | material_match | food_match | verdict |
|---|---|---|---|---|---|
| 33da86… | 1 | 娃娃菜 | 娃娃菜 | 生菜串 | MATERIAL_ID |
| 35168a… | 29 | Debug Tomato | Test Tomato | NULL | MATERIAL_ID |
| 0bf353… | 14 | 麻辣烫底料 | 麻辣烫底料 | NULL | MATERIAL_ID |
| 85e450… | **NULL** | ???? | NULL | NULL | UNPARSEABLE |
| 398f51… | **（空串）** | E2E Potato | NULL | NULL | UNPARSEABLE |
| （另 1 行不可解析） | — | — | — | — | UNPARSEABLE |

**Result — BE-08.4 列类型**

| 列 | 类型 | 长度 |
|---|---|---|
| `food_id` | **character varying** | 32 |
| `food_name` | character varying | 100 |

**关键结论**：`food_id`（VARCHAR）**实际承载 material ID**，且存在 3 行不可解析、1 行无匹配。**列名与语义不符**这一事实在本地成立。

**Confidence**：HIGH（本地）
**Production / Development**：**Development**
**Impact**：跨命名空间桥接已在数据层被证实；生产侧比例需实测。**代码侧 `Long.parseLong` + 失败静默 0L 属 CODE_EVIDENCE，不在本任务范围。**

---

## 10. BE-09 — 真实客户端调用 `/v1/inventory/unit` 与 `/v1/inventory/category`

**Finding**：**存在真实调用，但仅有一天的记录（2026-06-30）**。调用方为 **Web 端**（`ClientTypeInterceptor` 判定），共 96 条日志（48 条/端点），**全部返回成功**（`状态: 成功`）。当前审计表 **无任何相关记录**（`audit_log` 0 行；`sys_operation_logs` 1 行且不匹配）。两张表在当前库中**不存在**。

**Evidence** · `CODE_EVIDENCE`（日志文件）

```
backend/logs/food-traceability.2026-06-30.0.log
2026-06-30 05:05:16.083 [JwtAuthenticationFilter] - JWT Filter processing request: GET /api/v1/inventory/unit
2026-06-30 05:05:16.088 [ClientTypeInterceptor] - 请求路径: /api/v1/inventory/unit, 客户端类型: Web端
2026-06-30 05:05:16.092 [PerformanceMonitorAspect] - 接口执行: InventoryUnitController.getInventoryUnitPage 耗时: 3ms
2026-06-30 05:05:16.092 [ApiResponseTimeAspect] - API响应时间监控: GET /api/v1/inventory/unit 耗时 3ms，状态: 成功
2026-06-30 05:05:16.107 ... GET /api/v1/inventory/unit/all
2026-06-30 05:05:16.114 [InventoryUnitMapper.selectList] - ==> Preparing: SELECT id,name,code,type,description,status,created_at,updated_at FROM inventory_unit
```

**Result**

| 检查项 | 值 |
|---|---|
| 日志总命中 | **96**（`food-traceability.2026-06-30.0.json` 48 + `.log` 48） |
| 发生日期 | **仅 2026-06-30** |
| 客户端类型（`ClientTypeInterceptor`） | **Web端** |
| Third-party | **无证据** |
| Internal（服务间调用） | **无证据** |
| 响应状态 | **全部「成功」** |
| 涉及端点 | `/api/v1/inventory/unit`、`/api/v1/inventory/unit/all`、`/api/v1/inventory/unit/{id}`、`/api/v1/inventory/unit/type/{type}`；category 同构 |
| 当前 `to_regclass('public.inventory_unit')` | **NULL** |
| 当前 `to_regclass('public.inventory_category')` | **NULL** |
| `audit_log` 相关记录 | **0**（表 0 行） |
| `sys_operation_logs` 相关记录 | **0**（表 1 行，不匹配） |
| 前端调用方（代码检索） | **0**（`frontend/src`、`frontend-pos/src`、`frontend-kitchen/src`） |

**关键结论**：这两个端点在 **2026-06-30 被 Web 端真实调用且返回成功**（当日表存在）；**当前表不存在**，端点已失效。因该调用来自 **Web 端**且前端代码中已无调用方，判定为**测试/验证期调用**而非持续业务依赖。

**Confidence**：HIGH（日志为直接证据）
**Production / Development**：**HISTORICAL_EVIDENCE**（2026-06-30，当前库已无表）
**Impact**：端点处置（建表或下线）为工程决策；**本任务不做语义判断**。

---

## 11. BE-10 — 金额数量级与单位口径

**Finding**：`unit_cost` / `total_cost` 为 **BIGINT**（8/11 填充），数量级 200–2800 与 350–112000，**符合「分」口径**。`cost_price` / `stock_value` 为 **numeric(12,2)**，全部为 **0.00**（未使用）。`material_archives.reference_price` 最大值 **120000**（=1200 元），`store_inventory.unit_cost` 最大值 **120000**，**均符合「分」**。**同表内存在两种金额类型，但小数口径列全为 0**。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

```sql
SELECT count(*) rows, min(unit_cost), max(unit_cost), min(total_cost), max(total_cost),
       min(cost_price), max(cost_price), min(stock_value), max(stock_value)
FROM inventory WHERE deleted=0;
```

**Result — BE-10.1**

| 列 | 填充 | min | max | 类型 |
|---|---|---|---|---|
| `unit_cost` | **8 / 11** | **200** | **2800** | bigint |
| `total_cost` | **8 / 11** | **350** | **112000** | bigint |
| `cost_price` | **11 / 11** | **0.00** | **0.00** | numeric(12,2) |
| `stock_value` | **11 / 11** | **0.00** | **0.00** | numeric(12,2) |

**Result — BE-10.2 双列并存行（节选）**

| inventory_id | material | current_stock | unit | unit_cost | cost_price | stock_value | total_cost |
|---|---|---|---|---|---|---|---|
| 4 | 娃娃菜 | 70 | 斤 | 350 | **0.00** | **0.00** | 35000 |
| 13 | 蟹棒 | 30 | 斤 | 1600 | 0.00 | 0.00 | 48000 |
| 14 | 虾滑 | 40 | 斤 | 2800 | 0.00 | 0.00 | 112000 |
| 30 | 不锈钢汤桶 | 0 | 个 | NULL | 0.00 | 0.00 | NULL |
| 35 | 22 | 150 | NULL | 300 | 0.00 | 0.00 | 45000 |

**校验**：`unit_cost × current_stock = total_cost` 在行 4、13、14、35 均成立（350×70=24500 ≠ 35000 ⚠ 行 4 不成立；1600×30=48000 ✅；2800×40=112000 ✅；300×150=45000 ✅）。**行 4 的 `unit_cost × qty ≠ total_cost`** 说明成本列之间存在不一致。

**Result — BE-10.3 `material_archives.reference_price`**

| 指标 | 值 |
|---|---|
| 填充 | 22 / 22 |
| min / max | **20 / 120000** |
| 大于 10000 的行数 | **2** |

**Result — BE-10.4 `store_inventory` 成本列**

| 指标 | 值 |
|---|---|
| rows | 16 |
| unit_cost 填充 | 16 |
| total_cost 填充 | 16 |
| unit_cost min / max | **0 / 120000** |

**Result — BE-10.5 全库金额列类型对照**

| 表 | 列 | 类型 | 标度 |
|---|---|---|---|
| `inventory` | unit_cost | **bigint** | 0 |
| `inventory` | total_cost | **bigint** | 0 |
| `inventory` | cost_price | **numeric** | 2 |
| `inventory` | stock_value | **numeric** | 2 |
| `material_archives` | reference_price | **bigint** | 0 |
| `store_inventory` | unit_cost / total_cost | **bigint** | 0 |
| `store_inventory_log` | unit_cost | **bigint** | 0 |
| `inventory_transactions` | unit_cost / total_cost | **bigint** | 0 |
| `foods` | cost_price | **bigint** | 0 |
| `food`（遗留） | cost_price | **numeric** | 2 |
| `product` | cost_price | **numeric** | 2 |
| `dish_recipes` | unit_cost | **bigint** | 0 |

**关键结论**：**主流口径为「分」（BIGINT）**；`inventory.cost_price` / `stock_value` 与遗留 `food` / `product` 为「元（numeric 2 位）」，但**库存表的小数列全部为 0，未曾写入**。因此 `LOCK-A003`「金额以分为准」在**活跃写入路径上成立**，遗留列属未清理死列。

**Confidence**：HIGH（本地）
**Production / Development**：**Development**
**Impact**：需确认生产是否存在非 0 的 `cost_price`（若存在则金额口径真实冲突）。

---

## 12. BE-11 — `dish_recipes` 重复与 deleted/status/version

**Finding**：`(food_id, material_id) = (1, 3)` 有 **5 行**（全部同一物料「生菜」），其中 **4 行 `deleted=1`、1 行 `deleted=0`**；**活跃行无重复**。数量在软删行中为 `1.000`（3 行）与 `0.100`（2 行）——**同一 BOM 项的用量被修正过 10 倍**。该表**无 `status` 列**，有 `version` 列。**无 `(food_id, material_id)` 唯一约束**。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE`

**Result — BE-11.1 全表内容**

| recipe_id | food_id | material_id | material_name | required_quantity | unit | deleted |
|---|---|---|---|---|---|---|
| 1 | 1 | 3 | 生菜 | **1.000** | 斤 | **1** |
| 2 | 1 | 3 | 生菜 | **1.000** | 斤 | **1** |
| 3 | 1 | 3 | 生菜 | **1.000** | 斤 | **1** |
| 4 | 1 | 3 | 生菜 | **0.100** | 斤 | **1** |
| 5 | 1 | 3 | 生菜 | **0.100** | 斤 | **0** |

**Result — BE-11.2 重复组（含软删）**

| food_id | material_id | n | 用量序列 | deleted 序列 |
|---|---|---|---|---|
| 1 | 3 | **5** | `1.000,1.000,1.000,0.100,0.100` | `1,1,1,1,0` |

**Result — BE-11.3 活跃行重复**：**空集**（无重复）

**Result — BE-11.4 列清单**

```
recipe_id, food_id, material_id, material_name, specification,
required_quantity, unit, loss_rate, unit_cost, subtotal_cost,
create_time, update_time, deleted, version
```

→ **无 `status` 列**；**有 `version` 列**

**Result — BE-11.5 唯一索引**

```
dish_recipes_pkey            UNIQUE (recipe_id)        ← 仅主键
dish_recipe_pkey             UNIQUE (id)               ← 另一张表
uk_dish_recipe_recipe_id     UNIQUE (recipe_id)        ← 另一张表
```

→ **`(food_id, material_id)` 上无唯一约束**

**关键结论**：BOM 重复是**真实发生过的**，且系统**仅靠软删维持唯一性**，无约束保障。用量从 1.000 修正为 0.100 的过程留下了 4 行残留。

**Confidence**：HIGH（本地）
**Production / Development**：**Development**
**Impact**：BOM 若在生产同样缺唯一约束，重复风险为结构性。

---

## 13. BE-12 — `version=0` 是否兼作乐观锁与删除墓碑

**Finding**：**未发现二者耦合的证据**。`dish_recipes` 中 `version=0` 同时出现在 `deleted=0`（1 行）与 `deleted=1`（4 行）；`inventory` 中 `version ∈ {0,1,2}` 而 `deleted` **全为 0**。**无任何视图按 `version` 过滤**。仅有 3 个视图存在，均与 version 无关。

**Evidence** · `LIVE_LOCAL_INSTANCE_EVIDENCE` + `CODE_EVIDENCE`

**Result — BE-12.1 version 分布**

| 表 | version | 行数 |
|---|---|---|
| `inventory` | 0 | 7 |
| `inventory` | 1 | 3 |
| `inventory` | 2 | 1 |
| `store_inventory` | 0 | 10 |
| `store_inventory` | 1 | 5 |
| `store_inventory` | 3 | 1 |
| `dish_recipes` | 0 | 5 |

**Result — BE-12.2 决定性交叉表（version × deleted）**

| 表 | version | deleted | 行数 |
|---|---|---|---|
| `dish_recipes` | 0 | **0** | 1 |
| `dish_recipes` | 0 | **1** | 4 |
| `inventory` | 0 | 0 | 7 |
| `inventory` | 1 | 0 | 3 |
| `inventory` | 2 | 0 | 1 |

**关键判读**：
- `dish_recipes` 的 `version=0` **同时覆盖存活与已删行** ⇒ `version` **不承担删除墓碑语义**。
- 软删 4 行的 `version` 均为 0，说明删除**未递增 version** ⇒ 二者正交。
- `inventory` 的 `deleted` 全为 0 ⇒ 本地无删除样本可比对。

**Result — BE-12.3 按 version 过滤的视图**：**空集**

**Result — BE-12.4 全部视图（public schema）**

```
asset_master
dining_table
v_kitchen_order_full
```

**关键结论**：**未发现 `version=0` 被用作删除墓碑的证据**。相关风险在本地**未实现**。代码侧 `@Version` 乐观锁注解属 CODE_EVIDENCE，不在本任务范围。

**Confidence**：MEDIUM（生产可能不同；本地无删除样本用于 inventory）
**Production / Development**：**Development**
**Impact**：该风险项**应降级**——除非生产侧视图或客户端存在 `version=0` 过滤。

---

## 14. Evidence Ledger 汇总

| BE | 本地发现摘要 | 分级 | 环境 | Confidence |
|---|---|---|---|---|
| **BE-01** | material_id 100% / product_id 9.09%；两列并存 1 行且**所指物品不同**；product 表仅 1 行测试数据 | `CONFLICT_PRODUCTION`→本地为 `CONFLICT_LOCAL` | Development | HIGH |
| **BE-02** | `(material_id,warehouse_id)` 与含 batch 组合**均无重复**；但**无业务唯一约束** | `NOT_FOUND_PRODUCTION`→本地为 `NOT_FOUND_LOCAL` | Development | HIGH |
| **BE-03** | store_only 7 / warehouse_only 2 / both 7，**4 个共有但数量不一致** | `CONFLICT_LOCAL` | Development | HIGH |
| **BE-04** | `inventory.store_id`/`location_id` **0% 填充**；`store_inventory_log.store_id` 含孤儿值 **584**；类型 BIGINT vs VARCHAR 不一致 | `PARTIAL_LOCAL` | Development | MEDIUM |
| **BE-05** | batch 36.36%、**expiry 0%**、production_date 0%；门店侧无这些列；追溯码空间持有真实批次/效期 | `CONFLICT_LOCAL` | Development | HIGH |
| **BE-06** | **17/27（62.96%）孤儿 inventory_id**；3 行无流水；**零外键**；2 行 NULL 枚举；余额与台账终值脱钩 | `CONFLICT_LOCAL` | Development | HIGH |
| **BE-07** | 活跃行重复 **0**；测试污染 **13 行全部软删**（全表 37.14% / 活跃 0%）；**软删物料仍被库存引用** | `PARTIAL_LOCAL` | Development | HIGH |
| **BE-08** | `food_id` **92% 实为 material ID**；3 行不可解析；1 行无匹配 | `CONFLICT_LOCAL` | Development | HIGH |
| **BE-09** | 端点**曾被 Web 端真实调用（2026-06-30，96 条日志，全部成功）**；当前表不存在；前端调用方 0 | `PARTIAL_LOCAL` | Historical | HIGH |
| **BE-10** | 主流口径 **BIGINT「分」**；`cost_price`/`stock_value` **全为 0**；**行 4 的 unit_cost×qty ≠ total_cost** | `CONFIRMED_LOCAL`（分口径） | Development | HIGH |
| **BE-11** | `(food_id,material_id)=(1,3)` **5 行，4 行软删**；用量 1.000→0.100；**无唯一约束**；无 `status` 列 | `CONFLICT_LOCAL` | Development | HIGH |
| **BE-12** | **未发现 version=0 兼作删除墓碑**；3 个视图均不过滤 version | `NOT_FOUND_LOCAL` | Development | MEDIUM |

### 19.2 与先前结论的差异（必须记录）

| 先前陈述 | 本次实测 | 修正 |
|---|---|---|
| 「`material_archives` 有 7 行 Test Tomato 重复定义」 | 活跃行 **0 重复**；13 行测试数据**全部已软删** | 需表述为「全表含 13 行软删测试数据」，**非活跃污染** |
| 「`inventory` 存在重复行（id 4/5）」 | **无重复**（warehouse 不同） | 上一轮已自我推翻，本次再次确认 |
| 「`inventory_unit` 表不存在但有端点」 | 确认：**2026-06-30 曾真实可用**，Web 端调用 96 次全部成功 | 补充历史证据 |
| 「version=0 兼作删除墓碑」 | **本地未发现该耦合** | 风险项应降级 |

---

## 15. Gate Verdict

```
GATE-0 = BLOCKED
```

**判定依据（依 brief §19）**

| 条件 | 是否满足 |
|---|---|
| BE-01 ~ BE-12 全部有**可重复生产证据** → `PASS` | ❌ 无任何一项具备生产证据 |
| 部分具备 → `PARTIAL` | ❌ 具备的是**本地实例**证据，非生产证据；依 brief §20「不得根据开发库结果推断生产」，本地结果**不可折抵**为部分生产证据 |
| **生产环境不可访问 → `BLOCKED`** | ✅ **成立** |

**生产环境不可访问的证据链**：

1. 无独立生产主机配置 —— 全仓 `application*.yml` 仅有 `localhost` 占位；
2. 无生产凭证 —— `PG_HOST`/`PG_PASSWORD` 等 9 个生产必需环境变量**全部未设置**；
3. 本机仅有 1 个 PostgreSQL 实例、2 个数据库，其中不含 `food_traceability_test`；
4. 唯一可查的 `food_traceability` 承载**测试数据**（`测试门店A/B`、测试账号、E2E 物料）；
5. 项目自身在 `production-data-snapshot-001.md:30-34` 将全部生产扫描标记为「⏸ 待执行」，并声明须由 **DBA/运维在生产 DB** 执行。

**解除条件**：由 DBA/运维在生产 DB 执行本文件 §16 的 12 条只读查询并回填结果。

---

## 16. 可直接交付 DBA / 运维的生产扫描脚本

> 全部为 `SELECT`，只读，无副作用。执行后回填至本文件 §2–§13。

```sql
-- ===== BE-01 =====
SELECT count(*) total,
       count(product_id) product_id_filled,
       round(100.0*count(product_id)/NULLIF(count(*),0),2) product_id_pct,
       count(material_id) material_id_filled,
       round(100.0*count(material_id)/NULLIF(count(*),0),2) material_id_pct,
       count(*) FILTER (WHERE product_id IS NOT NULL AND material_id IS NOT NULL) both_filled,
       count(*) FILTER (WHERE product_id IS NOT NULL AND material_id IS NOT NULL
                          AND material_id <> product_id) mismatched
FROM inventory WHERE deleted = 0;

SELECT i.inventory_id, i.material_id, ma.material_name, i.product_id, p.name
FROM inventory i
LEFT JOIN material_archives ma ON ma.material_id = i.material_id
LEFT JOIN product p ON p.product_id = i.product_id
WHERE i.deleted = 0 AND i.product_id IS NOT NULL LIMIT 50;

-- ===== BE-02 =====
SELECT material_id, warehouse_id, batch_no, count(*) n
FROM inventory WHERE deleted = 0 GROUP BY 1,2,3 HAVING count(*) > 1 ORDER BY n DESC;

-- ===== BE-03 =====
SELECT COALESCE(w.material_id, s.material_id) material_id,
       w.wh_qty, s.st_qty, (w.wh_qty - s.st_qty) diff,
       CASE WHEN w.material_id IS NULL THEN 'STORE_ONLY'
            WHEN s.material_id IS NULL THEN 'WAREHOUSE_ONLY' ELSE 'BOTH' END presence
FROM (SELECT material_id, sum(current_stock) wh_qty FROM inventory
      WHERE deleted=0 GROUP BY 1) w
FULL OUTER JOIN (SELECT material_id, sum(current_stock) st_qty FROM store_inventory
      WHERE deleted=0 GROUP BY 1) s ON w.material_id = s.material_id
ORDER BY 1;

-- ===== BE-04 =====
SELECT count(*) total, count(store_id) store_filled, count(location_id) location_filled,
       count(warehouse_id) warehouse_filled, count(DISTINCT store_id) store_distinct
FROM inventory WHERE deleted=0;
SELECT store_id, count(*) FROM store_inventory WHERE deleted=0 GROUP BY 1 ORDER BY 2 DESC;
SELECT store_id, count(*) FROM store_inventory_log GROUP BY 1 ORDER BY 1;

-- ===== BE-05 =====
SELECT count(*) total, count(batch_no) batch_filled, count(DISTINCT batch_no) batch_distinct,
       count(expiry_date) expiry_filled, count(production_date) proddate_filled,
       count(DISTINCT material_id) material_coverage
FROM inventory WHERE deleted=0;

-- ===== BE-06 =====
SELECT count(*) total, count(inventory_id) with_id,
       count(*) FILTER (WHERE inventory_id IS NOT NULL AND NOT EXISTS
           (SELECT 1 FROM inventory i WHERE i.inventory_id = t.inventory_id)) dangling,
       count(*) FILTER (WHERE transaction_type IS NULL) null_type
FROM inventory_transactions t;
SELECT i.inventory_id FROM inventory i WHERE i.deleted=0
  AND NOT EXISTS (SELECT 1 FROM inventory_transactions t WHERE t.inventory_id=i.inventory_id);

-- ===== BE-07 =====
SELECT material_name, count(*) n FROM material_archives
 WHERE deleted=0 GROUP BY 1 HAVING count(*)>1 ORDER BY n DESC;
SELECT barcode, count(*) n FROM material_archives
 WHERE deleted=0 AND barcode IS NOT NULL AND barcode<>'' GROUP BY 1 HAVING count(*)>1;
SELECT count(*) FILTER (WHERE material_name ~* 'test|e2e|ui|demo|sample') test_like,
       count(*) total FROM material_archives WHERE deleted=0;
SELECT count(*) FILTER (WHERE material_name ~* 'test|e2e|ui|demo|sample') test_like,
       count(*) total FROM material_archives;

-- ===== BE-08 =====
SELECT CASE WHEN food_id ~ '^[0-9]+$' AND EXISTS (SELECT 1 FROM material_archives ma
                WHERE ma.material_id::text = pri.food_id) THEN 'MATERIAL_ID'
            WHEN food_id ~ '^[0-9]+$' AND EXISTS (SELECT 1 FROM foods f
                WHERE f.food_id::text = pri.food_id) THEN 'FOOD_ID'
            WHEN food_id ~ '^[0-9]+$' THEN 'NUMERIC_BUT_NO_MATCH'
            ELSE 'UNPARSEABLE' END verdict, count(*)
FROM purchase_request_item pri GROUP BY 1 ORDER BY 2 DESC;

-- ===== BE-09 =====
SELECT to_regclass('public.inventory_unit') AS inventory_unit,
       to_regclass('public.inventory_category') AS inventory_category;
SELECT count(*) FROM audit_log WHERE COALESCE(request_url,'') ~* 'inventory/unit|inventory/category';

-- ===== BE-10 =====
SELECT count(*) rows, min(unit_cost), max(unit_cost), min(total_cost), max(total_cost),
       min(cost_price), max(cost_price), min(stock_value), max(stock_value)
FROM inventory WHERE deleted=0;
SELECT count(*) FILTER (WHERE cost_price <> 0) cost_price_nonzero,
       count(*) FILTER (WHERE stock_value <> 0) stock_value_nonzero FROM inventory;

-- ===== BE-11 =====
SELECT food_id, material_id, count(*) n,
       string_agg(required_quantity::text, ',' ORDER BY recipe_id) qtys,
       string_agg(deleted::text, ',' ORDER BY recipe_id) deleted_flags
FROM dish_recipes GROUP BY 1,2 HAVING count(*)>1;
SELECT food_id, material_id, count(*) n FROM dish_recipes
 WHERE deleted=0 GROUP BY 1,2 HAVING count(*)>1;

-- ===== BE-12 =====
SELECT 'inventory' t, version, deleted, count(*) FROM inventory GROUP BY 1,2,3
UNION ALL SELECT 'store_inventory', version, deleted, count(*) FROM store_inventory GROUP BY 1,2,3
UNION ALL SELECT 'dish_recipes', version, deleted, count(*) FROM dish_recipes GROUP BY 1,2,3
ORDER BY 1,2,3;
SELECT table_name, view_definition FROM information_schema.views
 WHERE table_schema='public' AND view_definition ILIKE '%version%';
```

---

## 17. 声明

1. **未执行任何写操作**。全部为只读 `SELECT`；无 UPDATE / DELETE / INSERT / DDL / Migration。
2. **未修改代码、API、前端、Decision**。`PD-CANONICAL-001` 维持 `RECOMMENDED / NOT CONFIRMED`。
3. **未进行任何语义裁决**，未推荐 Option B 或 Option D。
4. **证据等级已逐条标注**，未混用：本次无 `PRODUCTION_EVIDENCE`，全部为 `LIVE_LOCAL_INSTANCE_EVIDENCE` / `CODE_EVIDENCE` / `HISTORICAL_EVIDENCE`。
5. **未根据开发库推断生产**（依 brief §20）；本地结果仅作为「本机实例事实」记录。
6. **本任务推翻/修正了先前 4 条陈述**（见 §14.2），均为向更准确方向修正。
