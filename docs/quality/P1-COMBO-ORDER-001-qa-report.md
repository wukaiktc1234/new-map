# P1-COMBO-ORDER-001 — 独立 QA 验收报告

> **结论：PASS_WITH_LIMITATION**（验收 6 项全过；限制 L-01 UI 浏览器目检未做、L-02 生产证据 PROVISIONAL，均不阻断进回归）
> **角色**: 独立 QA（与开发隔离；本报告为唯一可写产物，未改任何业务代码/实施报告，未 commit）
> **日期**: 2026-09-24
> **验收对象**: `docs/architecture/03-review/p1-combo-order-001-implementation-record-001.md` §0–§5 验收 6 项 + §11 DS 抽检 READY_FOR_QA
> **Scope**: 报告 §1 范围 5 项（不重审 Scope）
> **代码基线**: commit `aec5c45`（15 文件）+ DS 报告 commit `40fc776`；工作区 HEAD=`40fc776`（master）
> **环境**: local — DB `food_traceability`（psql 18，`-U postgres`）；后端 `127.0.0.1:8081` context-path=`/api`
> **Production Evidence**: `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`（生产不可访问，与前序卡同口径）
> **验收方式**: 抽检复核为主（对照 FOODID 范式 `docs/quality/P1-POS-FOODID-MAP-001-qa-report.md`）；DB 独立断言 + KDS live + 代码抽检 + 单测重跑 + 前端代码目检；**不重跑全量单测套件、不重跑完整 HTTP E2E 链**

---

## 0. 结论摘要

### 验收 6 项（对照实施报告 §5 / DS §11.1）

| # | 验收项 | QA 结论 | 一手证据摘要 |
|---|--------|---------|--------------|
| 1 | 套餐下单成功（`product_type=2` + `combo_id`） | **PASS** | DB 独立复现：`T20260924004` 行 `product_type=2, combo_id=1, food_id=NULL` |
| 2 | KDS 卡片套餐名 + 明细 `components[]` | **PASS（API live）** | `GET /api/v1/kitchen/orders/recent/full` HTTP 200；套餐行 `productType=2 comboId=1 components=[{foodId:1,quantity:1,name:生菜串}]`；其余单品行均无误展开 |
| 3 | 出餐扣料 PASS | **PASS** | DB：`KO1790260472189` `material_consumed=1 status=served`；库存 log id=62 生菜 `8.500→8.400` |
| 4 | 单品零回归（H-06 复跑） | **PASS** | DB：`T20260924003` `product_type=1 food_id=1 combo_id 空`；`KO…187` `material_consumed=1`；log id=61 `8.600→8.500`；单测重跑 **37/37 EXIT=0** |
| 5 | 前端入口恢复 | **PASS（代码级）+ LIMITATION** | `useMenu:43` / `Order.vue:45,258,337` / `MenuSection:168` 恢复标记齐全；KDS 三端 `components` 渲染齐全；**浏览器目检未做 → L-01** |
| 6 | 无新静默点 / 无 Scope 外改动 | **PASS** | `material_consumption` 2026-09-24 新增 FAILED=0、`failure_type` 非空行仅 2 条均为 09-23 既有；`food_id IS NULL AND product_type=1`=0；commit `aec5c45` **恰 15 文件**与 §6 逐文件一致 |

### 抽检项汇总

| # | 抽检项 | 结果 |
|---|--------|------|
| 1 | DB 断言独立复现（≥3 项：order_items 双单 / kitchen_order / store_inventory_log / 单品零回归 / 静默点） | **PASS 5/5** |
| 2 | KDS live `recent/full` 套餐行 `components[]` + 单品行不误展开 | **PASS** |
| 3 | 代码抽检（commit 15 文件、NewMapper、无 Scope 外、三端点展开、前端入口） | **PASS** |
| 4 | 单测证据核验（4 套件重跑，记录 EXIT） | **PASS 37/37，EXIT=0** |
| 5 | UI 浏览器目检 | **LIMITATION**（无法开浏览器，不 FAIL，与 FOODID 口径一致 → L-01） |
| 6 | 生产证据 | **PROVISIONAL_PENDING_PRODUCTION_EVIDENCE**（L-02） |

| FAIL 项 | — | **无** → 无需生成 `-R{n}` |
|--------|---|---------------------------|
| **是否可进回归** | | **可进回归**（无阻断项；生产放行仍待生产证据 + Release Gate） |

---

## 1. 抽检 1 — DB 断言独立复现（5/5）

> 全部由 QA 独立执行 psql（`$env:PGPASSWORD` + 显式 `-U postgres`），未采信开发自测输出。

### 1.1 order_items：套餐单 + 单品单（一次 JOIN 查询）

**命令**：

```sql
SELECT oi.item_id, oi.order_id, o.order_number, oi.product_type, oi.combo_id, oi.food_id,
       oi.product_name, oi.quantity
FROM order_items oi
JOIN orders o ON oi.order_id = o.order_id
WHERE o.order_number IN ('T20260924003','T20260924004')
ORDER BY o.order_number;
```

**输出**：

```text
             item_id              |    order_id     | order_number | product_type | combo_id | food_id | product_name | quantity
----------------------------------+-----------------+--------------+--------------+----------+---------+--------------+----------
 d41fb4e5a703dca385f9e8bad8c2861c | O1790260472188  | T20260924003 |            1 |          | 1       | 生菜串       |        1
 d732d20cd3bb59f67027bbc217cf7358 | O1790260472190  | T20260924004 |            2 | 1        |         | 生菜套餐     |        1
(2 行)
```

**断言表**：

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| 套餐行 product_type | =2 | **2** | ✅ |
| 套餐行 combo_id | =1 | **1** | ✅ |
| 套餐行 food_id | IS NULL | **NULL**（空） | ✅ |
| 单品行 product_type / food_id | 1 / 1 | **1 / 1** | ✅ |
| 单品行 combo_id | 空 | **空** | ✅ |
| 订单关联 | O…188 / O…190 | **O1790260472188 / O1790260472190** | ✅ |

**与报告 §4.3.A/B、§5-1、DS §11.1-1 对照**：一致。

### 1.2 kitchen_order：两单 `material_consumed`

**命令**：

```sql
SELECT kitchen_order_id, order_id, order_number, status, material_consumed, material_consume_time
FROM kitchen_order
WHERE kitchen_order_id IN ('KO1790260472187','KO1790260472189');
```

**输出**：

```text
 kitchen_order_id |    order_id    | order_number | status | material_consumed |   material_consume_time
 KO1790260472189  | O1790260472190 | T20260924004 | served |                 1 | 2026-09-24 22:43:58.93802
 KO1790260472187  | O1790260472188 | T20260924003 | served |                 1 | 2026-09-24 22:43:21.987431
```

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| 套餐 KO `material_consumed` | =1 | **1** | ✅ |
| 单品 KO `material_consumed` | =1 | **1** | ✅ |
| 两单 status | served | **served / served** | ✅ |

**与报告 §4.3.A/B、DS §11.1-3/-4 对照**：一致。

### 1.3 store_inventory_log：id=61 / 62

**命令**：

```sql
SELECT id, product_name, before_stock, after_stock, change_quantity, remark, create_time
FROM store_inventory_log WHERE id IN (61,62);
```

**输出**：

```text
 id | product_name | before_stock | after_stock | change_quantity |             remark              |        create_time
 61 | 生菜         |        8.600 |       8.500 |           0.100 | KDS出餐扣料 - 订单:T20260924003 | 2026-09-24 22:43:21.98467
 62 | 生菜         |        8.500 |       8.400 |           0.100 | KDS出餐扣料 - 订单:T20260924004 | 2026-09-24 22:43:58.936902
```

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| 单品扣料 log | 8.600→8.500，change=0.100，remark 含 T20260924003 | **完全一致** | ✅ |
| 套餐扣料 log | 8.500→8.400，change=0.100，remark 含 T20260924004 | **完全一致** | ✅ |
| 库存链连续性 | 61 after = 62 before | **8.500 = 8.500** | ✅ |

**与报告 §4.3.A/B、DS §11.1-3/-4 对照**：一致。

### 1.4 单品零回归 / 合法套餐 null

**命令**：

```sql
SELECT COUNT(*) FROM order_items WHERE food_id IS NULL AND product_type=1;
SELECT COUNT(*) FROM order_items WHERE product_type=2;
```

**输出**：`bad_null=0`；`combo_rows=1`

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| 非法 null（单品型 null food_id） | 0 | **0** | ✅ |
| 合法套餐行 | 1（仅本卡套餐单） | **1** | ✅ |

**与报告 §4.3.C、DS §11.1-6 对照**：一致。

### 1.5 静默点：无新增失败审计

**命令**：

```sql
SELECT COUNT(*) FROM material_consumption WHERE status='FAILED' AND create_time >= '2026-09-24 00:00';
SELECT COUNT(*) FROM material_consumption WHERE status='FAILED';
SELECT consumption_id, failure_type, create_time FROM material_consumption
WHERE failure_type IS NOT NULL ORDER BY create_time DESC LIMIT 5;
```

**输出**：

```text
new_failed = 0
all_failed = 2
MCF17901561648931052 | ITEM_FOOD_ID_NULL | 2026-09-23 17:36:04
MCF17901539792614236 | ITEM_FOOD_ID_NULL | 2026-09-23 16:59:39
```

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| 2026-09-24 新增 FAILED | 0 | **0** | ✅ |
| `COMBO_INGREDIENTS_EMPTY` 失败行 | 0 新增 | **0**（`failure_type` 全表仅 2 条 ITEM_FOOD_ID_NULL，均为 09-23 既有） | ✅ |
| `ITEM_FOOD_ID_NULL` 新增 | 0 | **0**（09-23 两条为 FOODID 卡既有登记） | ✅ |

**与报告 §4.3.C、DS §11.1-6 对照**：一致。口径与 FOODID 报告 V2 附注相同：`material_consumption` 只记 FAILED 行，"无新增"证明未新增失败；成功以 §1.2/§1.3 的 `material_consumed=1` + 库存 log 为准。

**抽检 1 结论：PASS 5/5**

---

## 2. 抽检 2 — KDS live `components[]`

**命令**：

```powershell
Invoke-WebRequest -Uri 'http://127.0.0.1:8081/api/v1/kitchen/orders/recent/full' -UseBasicParsing
```

**输出**：`HTTP 200`，body 35027 bytes。QA 解析 `dishItems` 逐行核验（UTF-8 读取）：

**套餐行（`T20260924004`）**：

```json
{
    "name": "生菜套餐",
    "quantity": 1,
    "price": 13.5,
    "type": "combo",
    "productType": 2,
    "comboId": 1,
    "components": [
        { "foodId": "1", "quantity": 1, "name": "生菜串" }
    ]
}
```

**断言表**：

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| HTTP | 200 | **200** | ✅ |
| 套餐行 productType / comboId | 2 / 1 | **2 / 1** | ✅ |
| 套餐名 | 生菜套餐 | **生菜套餐** | ✅ |
| `components[]` | `[{foodId:1, qty:1, name:生菜串}]` | **逐字段一致** | ✅ |
| 其余 24 行（单品历史单）无 components | 不误展开 | **hasComponents=False × 24**（含 `T20260924003`） | ✅ |
| 单卡片不拆 | 单一 dish 对象携带 components | **是**（无 N 张拆卡） | ✅ |

**components 与源数据交叉核验**（QA 独立查 `combo_ingredients`）：

```sql
SELECT * FROM combo_ingredients WHERE combo_id=1;
-- ingredient_id=1: food_id=1 qty=50 deleted=1（已删）
-- ingredient_id=2: food_id=1 qty=1  deleted=0（生效）
```

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| components 数量 = 生效配料行（deleted=0） | qty=1 | **quantity=1**，未把 deleted=1 行带出 | ✅ |

**与报告 §4.3.B、§5-2、DS §11.1-2 对照**：一致。

**抽检 2 结论：PASS**

---

## 3. 抽检 3 — 代码抽检

### 3.1 commit `aec5c45` 文件清单（恰 15 文件，与报告 §6 对照）

**命令**：`git show aec5c45 --name-only` → **files=15**，`git show aec5c45 --stat` → `15 files changed, 2493 insertions(+), 583 deletions(-)`

**实测清单 vs 报告 §6 逐项**：

| # | 报告 §6 文件 | commit 中存在 |
|---|---|---|
| 1 | `PosOrderCreateServiceImpl.java` | ✅ |
| 2 | `OrderNewServiceImpl.java` | ✅ |
| 3 | `DatabaseFixConfig.java` | ✅ |
| 4 | `KitchenOrderController.java` | ✅ |
| 5–7 | 3 个测试文件 | ✅ |
| 8–10 | `useMenu.ts` / `Order.vue` / `MenuSection.vue` | ✅ |
| 11–14 | `KitchenOrderCard.vue` / `kitchen.ts` / `Home.vue` / `ServeWindow.vue` | ✅ |
| 15 | 实施报告本身 | ✅ |

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| 文件数 | 15 | **15** | ✅ |
| Scope 外文件 | 0 | **0**（清单与 §6 完全一致，无多余文件） | ✅ |
| 后续 DS 报告 | 独立 commit | **`40fc776`**（仅追加 §11，未混入 aec5c45） | ✅ |

### 3.2 `OrderNewServiceImpl` 改用 `ComboIngredientNewMapper`

**证据（grep 实测）**：

```text
Line 59:  private final ComboIngredientNewMapper comboIngredientNewMapper;
Line 84:  ComboIngredientNewMapper comboIngredientNewMapper,
Line 609: // 套餐：…读 combo_ingredients（P1-COMBO-ORDER-001）
Line 793: // P1-COMBO-ORDER-001: combo_id 优先，legacy 回落 foodId
Line 963: // P1-COMBO-ORDER-001: 读新表 combo_ingredients（替代 combo_ingredient）
Line 1245: // 套餐（P1-COMBO-ORDER-001 读 combo_ingredients）
```

`git show aec5c45 -- OrderNewServiceImpl.java` diff 确认 `ComboIngredientMapper → ComboIngredientNewMapper` 替换（字段/构造器/helper 查询）。

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| OrderNewServiceImpl 无 legacy `ComboIngredientMapper` | 0 引用 | **0**（grep 仅命中 NewMapper） | ✅ |
| 三处调用点（soft/strict/refund）有 `P1-COMBO-ORDER-001` 标识 | 齐全 | **:609 / :793 / :1245 齐全** | ✅ |

**legacy 三文件仍读旧表（报告 §7.1 声明的第二步卡，非本卡缺陷）**：`PosApiServiceImpl` / `KitchenScanServiceImpl` / `OrderMaterialRequirementServiceImpl` 实测仍为 `ComboIngredientMapper` — **与报告声明一致，不计入 FAIL**。

### 3.3 combo 下单分支（`PosOrderCreateServiceImpl`）

```text
Line 240/263/471: isComboItem(item) 分支
Line 495: comboOrderItem.setProductType(2);
Line 496: comboOrderItem.setComboId(comboId);
Line 497: comboOrderItem.setFoodId(null);
Line 630: private boolean isComboItem(…
```

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| combo 分支 `productType=2`/`setComboId`/`setFoodId(null)` | 齐全 | **:495/:496/:497 齐全** | ✅ |
| 预检/库存跳过/JSON 标记均走 `isComboItem` | 4 处 | **:240/:263/:471/:654（+ :643/:1037 JSON 写点）** | ✅ |

### 3.4 KDS 三端点接入（`KitchenOrderController`）

```text
Line 107: expandComboDishItems(List.of(order));   // getFullById
Line 115: expandComboDishItems(orders);           // getActiveOrdersFull
Line 123: expandComboDishItems(orders);           // getRecentOrdersFull
Line 131: private void expandComboDishItems(…
```

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| 三端点接入 | 3 | **3 + 私有方法定义** | ✅ |

### 3.5 前端入口恢复（代码级）

| 文件 | 实测证据 | 结果 |
|------|---------|------|
| `useMenu.ts` | `:43` 恢复注释；`:44-49` combo concat（`dishType:'combo'`）；`:91` combo 分类 push | ✅ |
| `Order.vue` | `:45` 恢复注释；`:46` `<el-radio-button label="combo">`；`:258-266` concat；`:337-342` badge push；`:56/:61/:89/:143` combo 卡片渲染；`:571+` CSS | ✅ |
| `MenuSection.vue` | `:168` 恢复注释；`:169` `visibleCategories = props.categories`（无 combo 过滤）；`:35/:42` combo badge | ✅ |
| `KitchenOrderCard.vue` | `:69` 套餐明细注释；`:71-75` `dish.components` 列表渲染；`:460` CSS | ✅ |
| `types/kitchen.ts` | `:28` `components` 字段注释（`productType`/`comboId` 同文件声明） | ✅ |
| `Home.vue` | `:84-86` components 展开；`:1289` CSS | ✅ |
| `ServeWindow.vue` | `:74-76` components 展开；`:790` CSS | ✅ |

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| POS 入口 3 文件恢复标记 | 齐全 | **齐全，无 combo 过滤残留** | ✅ |
| KDS 渲染 3 处 + 类型 | 齐全 | **KitchenOrderCard / Home / ServeWindow + kitchen.ts** | ✅ |

**抽检 3 结论：PASS**

---

## 4. 抽检 4 — 单测证据核验（重跑）

**命令**：

```powershell
$env:JAVA_HOME='P:\my-new-project\JDK21'
mvn -Dtest=PosOrderCreateServiceFoodIdMapTest,OrderNewServiceImplDeductTest,OrderNewServiceImplOrderNumberA1Test,MaterialDeductionAuditSelfFailureIntegrationTest -DfailIfNoTests=false test
```

**输出**：**EXIT=0**（QA 本次执行，2026-09-24 23:04 surefire 报告时间戳确认为本次生成）

| 套件 | surefire `Tests run` | 结果 | 报告时间戳 |
|---|---|---|---|
| `PosOrderCreateServiceFoodIdMapTest` | 5, Failures 0, Errors 0 | **5/5** | 23:04:18（本次） |
| `OrderNewServiceImplDeductTest` | 28, Failures 0, Errors 0 | **28/28** | 23:04:17（本次） |
| `OrderNewServiceImplOrderNumberA1Test` | 3, Failures 0, Errors 0 | **3/3** | 23:04:17（本次） |
| `MaterialDeductionAuditSelfFailureIntegrationTest` | 1, Failures 0, Errors 0 | **1/1** | 23:04:17（本次） |
| **合计** | **37** | **37/37 PASS** | **mvn EXIT=0** |

| 断言 | 期望 | 实测 | 结果 |
|------|------|------|------|
| 四套件合计 | 37/37 | **37/37** | ✅ |
| mvn EXIT | 0 | **0** | ✅ |

**范围外说明（不计入本卡）**：surefire 目录中 `DeductMaterialsForServeConcurrencyTest.txt`（2 Errors）时间戳为 **2026-09-21**，为本卡之前既有残留、不在本次 `-Dtest` 过滤内——与实施报告 §4.1 声明一致，**非本卡 FAIL**。

**与报告 §4.1、§5-4 对照**：一致（数量与套件构成完全吻合）。

**抽检 4 结论：PASS 37/37 EXIT=0**

---

## 5. 抽检 5 — UI 浏览器目检

| 项 | 状态 |
|----|------|
| POS 菜单套餐入口实拍 | **未执行**（QA 会话无浏览器能力） |
| KDS 卡片套餐明细实拍 | **未执行** |
| 处置 | 记 **LIMITATION L-01**，不 FAIL（与 FOODID 报告同口径）；代码级（§3.5）+ live API（§2）+ `vue-tsc`（报告 §4.2）已覆盖可自动化部分 |

**抽检 5 结论：LIMITATION（L-01）**

---

## 6. 抽检 6 — 生产证据

| 项 | 状态 |
|----|------|
| 生产环境访问 | **不可用** |
| 结论口径 | **`PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`**（与 FOODID / A1 轮一致；仅阻断生产放行，不阻断 local 回归准入） |

---

## 限制 / L-x

| ID | 内容 | 是否阻断进回归 |
|----|------|----------------|
| L-01 | **UI 浏览器目检未做**（POS 套餐入口点击流 / KDS 卡片实拍）；代码级 + API live + 类型检查已过。归 QA 边界内未完成项 | **否**（建议回归阶段补浏览器断言） |
| L-02 | **生产证据缺失** → 全部结论 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE` | 否（阻断仅限生产放行 / Release Gate） |
| L-03 | **ENV 工作区混杂**：仓库存在大量既有未提交改动（`git status` 实测 backend 前端大量 `M` 文件）；但本卡 commit `aec5c45` 恰 15 文件未夹带——与报告 §9.5 一致，属既有工作区债 | 否（不属本卡引入） |
| L-04 | **报告行号漂移**：§2.4 `:128-182` vs 实测方法体 `:131+`；§2.2 helpers `:952-966` vs 实测 `:952-966` 注释起点一致但方法体略有偏移——语义一致，DS §11.2.1 已登记 | 否（文档行号未最终校对） |
| L-05 | **soft 路径 `continue` 无日志（OBS-S1 同族）**：HEAD 既有，本卡仅换数据源非新增静默点；DS §11.2.2 已登记 | 否 |
| L-06 | **legacy 三文件仍读旧表**（`PosApiServiceImpl` / `KitchenScanServiceImpl` / `OrderMaterialRequirementServiceImpl`）→ 报告 §7.1 明确划归**第二步卡**，本卡不做 | 否（Scope 内声明的后续工作） |
| L-07 | **HTTP 响应 `materialConsumed` 可能回 0**（既有短路 L-05 同族）：成功以 DB `material_consumed=1` + 库存 log 为准（报告 §9.7）；本次 DB 已独立证实 | 否 |
| OBS-P1 | **任务池状态未见 `P1-COMBO-ORDER-001` 独立条目**（`production-remediation-task-board.md` 仅在 FOODID 条目下游提及）。按角色边界 QA 不改任务池——**planner 请据本报告同步任务状态为 QA 结论** | 否（流程同步项，非代码缺陷） |

---

## 是否可进回归

| 项 | 状态 |
|----|------|
| **本 QA 结论** | **PASS_WITH_LIMITATION**（验收 6/6 PASS；限制 L-01 UI 目检、L-02 生产证据，另有 L-03–L-07/OBS-P1 非阻断） |
| FAIL 项 | **无** |
| 是否返回开发 `-R{n}` | **否** |
| **是否可进回归** | **可进回归** — 本卡无阻断项；regression 可按 REG 口径将 DB 断言（order_items product_type/combo_id、KO material_consumed、库存 log 61/62）、KDS `recent/full` components、四套件 37/37 转基线；**建议回归批次补 L-01 浏览器目检** |
| 生产放行 | 仍 **`PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`**，待生产证据 + Release Gate（FAIL>0 禁止放行规则不变） |
| Scope / 代码 | **未改**（QA 只写本报告；未 commit/push） |
| 下一步 | ① planner 同步任务池状态（OBS-P1）；② regression 转基线；③ architect 更新 roadmap |

---

*验收执行：qa（独立会话，与开发隔离）· 2026-09-24 · 只读验证：psql 独立断言 ×12 条 SQL + KDS live HTTP 1 次 + `git show/diff/status` ×5 + 代码 grep 抽检 ×8 文件 + `mvn -Dtest` 四套件重跑（EXIT=0，37/37）· 唯一写入：本报告 · 未采信开发自测为唯一证据（单测与 DB 均独立复跑/复现） · 未猜测业务正确性（套餐 null 语义依据任务卡 §1 范围 1 与实施报告 §2.1，非 QA 推定）*
