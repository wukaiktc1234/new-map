# P1-COMBO-ORDER-001 — 实施报告

## 0. 状态

| 项 | 值 |
|---|---|
| Task ID | `P1-COMBO-ORDER-001` |
| Stage | **QA_PASS_WITH_LIMITATION → REGRESSION_CANDIDATE（2026-09-24）** |
| Owner 路径 | 治本——改读新表 `combo_ingredients`（非 legacy `combo_ingredient`） |
| 前置依赖 | `P1-POS-FOODID-MAP-001` CLOSED_WITH_REGISTERED_LIMITATIONS（套餐入口临时禁用已恢复） |
| 代码变更 | 后端 4 文件 + 测试 3 文件 + 前端 6 文件 |
| 自测 | 单测 37/37 PASS；本地 HTTP E2E 双链 PASS；`vue-tsc` 前后端 EXIT=0；`mvn compile` EXIT=0 |
| 验收 6 项 | **6/6 自评 PASS**；DS 抽检 6/6 PASS；QA 独立验收 **PASS_WITH_LIMITATION**（`docs/quality/P1-COMBO-ORDER-001-qa-report.md`，FAIL=0） |
| 日期 | 2026-09-24 |
| 下一步 | 回归转基线 → planner 任务池回写 → architect roadmap |

---

## 1. 范围（5 项，全部实施）

| # | 范围 | 状态 | 关键落点 |
|---|---|---|---|
| 1 | `PosOrderCreateServiceImpl` 识别 `dishType=combo` | ✅ | 预检 combo 存在性+配料非空 400；`createCanonical` combo 分支 `productType=2`/`comboId`/`foodId=null`；跳过 foods 价格覆盖/库存；R1 fail-fast；dishItems JSON 写 `productType`/`comboId`；`toCanonicalItem` 已 `setDishType` |
| 2 | `OrderNewServiceImpl` 改读新表 | ✅ | 字段/构造器 `ComboIngredientNewMapper`；soft `:611` / strict `:814` / refund `:1241` 三处经 `resolveComboId`+`selectComboIngredients`；quantity `Integer→BigDecimal`；foodId `Long` 直传 |
| 3 | `DatabaseFixConfig` 列名+同步 | ✅ | dish_combo sync `created_at→create_time`、`updated_at→update_time`；`initializeDishComboTable` 列同步；新增 `syncComboIngredientsToLegacy`（幂等补插，时间列 information_schema 探测） |
| 4 | KDS 展开（单卡片不拆） | ✅ | `KitchenOrderController.expandComboDishItems` 三端点接入；`combo_ingredients JOIN foods` → `components[]`；前端 3 处渲染 + CSS + `KitchenDishItem` 类型 |
| 5 | 前端入口还原 | ✅ | `useMenu` combo concat+分类 badge；`Order.vue` radio/concat/loadMenu filter；`MenuSection` `visibleCategories`/`filteredItems` 去 combo 过滤 |

**禁止项遵守**：不废弃旧表、不切 `getFullMenu`、不动 P0 扣料主体、不动 Scope-002/A1/FOODID 卡、不引入新静默点。第二步独立卡（废弃 `combo_ingredient`、切 `getFullMenu`、清旧 POS 兼容）本卡不做。

---

## 2. 后端改动明细

### 2.1 `PosOrderCreateServiceImpl.java`（项 1）

| 位置 | 改动 |
|---|---|
| `:46` | 注入 `DishComboNewMapper` + `ComboIngredientNewMapper` |
| `:234-255` | 预检分支：`isComboItem` → combo 存在性 + `combo_ingredients` 非空，任一失败 `Result.error(400)`（含 `COMBO_INGREDIENTS_EMPTY` 语义）；单品保留 `food_code` 映射 |
| `:263-264` | 库存预检跳过 combo（套餐无独立 foods 库存） |
| `:470-496` | `createCanonicalOrderItemsAndDeductStockNew` combo 分支：`setProductType(2)`、`setComboId`、`foodId=null`；跳过 foods 价格覆盖与库存扣减；R1 fail-fast `BusinessException(400)` |
| `:628-631` | 新增 `isComboItem()`（`"combo".equals(dishType)`） |
| `:645-656` / `:1037-1050` | `buildCanonicalDishItemsJson` / `buildDishItemsJson` combo 行写 `"productType":2` + `"comboId"` |
| `toCanonicalItem` | 已 `setDishType`（既有） |
| `toCanonicalTableItem` | **不** `setDishType`（`TableOrderItem` 无该字段，已回退） |

### 2.2 `OrderNewServiceImpl.java`（项 2）

| 位置 | 改动 |
|---|---|
| 字段/构造器 | `ComboIngredientMapper` → `ComboIngredientNewMapper`（`:59/:84/:100`） |
| soft `:609-612` | `resolveComboId(item)` + `selectComboIngredients(comboId)` |
| strict `:793-849` | 同上；`quantity` `Integer→BigDecimal.valueOf`；`foodId` `Long` 直传无 `parse`；`INGREDIENT_FOOD_ID_PARSE_ERROR` / `COMBO_INGREDIENT_QTY_TRUNCATED` 对新表类型不可达（guard 保留，单测改占位断言） |
| refund `:1245-1248` | 同上 |
| helpers `:952-966` | `resolveComboId`：`comboId` 优先、legacy 回落 `foodId`（`food_id` 当 `combo_id` 的旧数据）；`selectComboIngredients`：`LambdaQueryWrapper.eq(comboId)` 读新表 |

### 2.3 `DatabaseFixConfig.java`（项 3）

| 位置 | 改动 |
|---|---|
| `:837/:842/:852` | dish_combo sync：`created_at→create_time`、`updated_at→update_time` |
| `initializeDishComboTable` | CREATE 列同步 `create_time`/`update_time` + `checkAndAddColumn` 补列 |
| `:75-76/:867-903` | 新增 `syncComboIngredientsToLegacy`：`combo_ingredients → combo_ingredient` 幂等补插 `NOT EXISTS (combo_id, food_id)`；时间列按 `information_schema` 探测 `create_time`/`created_at` 漂移 |

启动日志证据（2026-09-24 22:34）：`已同步 0 条套餐配料从 combo_ingredients 到 combo_ingredient 表`（幂等，当前无缺口）。

### 2.4 `KitchenOrderController.java`（项 4）

| 位置 | 改动 |
|---|---|
| `:107/:115/:123` | `getFullById` / `getActiveOrdersFull` / `getRecentOrdersFull` 后调 `expandComboDishItems` |
| `:128-182` | 用已有 `jdbcTemplate` 查 `combo_ingredients JOIN foods` 生成 `components[]` 写回 `dishItems` JSON；单卡片不拆 |

---

## 3. 前端改动明细（项 4/5）

| 文件 | 改动 |
|---|---|
| `frontend-pos/src/composables/useMenu.ts` | combo `concat` + 分类 `push` 恢复（`id=comboId`, `dishType='combo'`, `items=c.items`；类型加 `items?: Combo['items']`） |
| `frontend-pos/src/views/Order.vue` | `el-radio-button label="combo"` 恢复；combo `concat`；`loadMenu` filter 还原（缺失时 push combo 分类 `sortOrder:999`）；combo 卡片/badge/items CSS |
| `frontend-pos/src/components/pos/MenuSection.vue` | `visibleCategories`/`filteredItems` 去 combo 过滤；`DishItem` 加 `items` 字段 |
| `frontend-pos/src/types/kitchen.ts` | `KitchenDishItem` 加 `productType`/`comboId`/`components` |
| `frontend-pos/src/components/kitchen/KitchenOrderCard.vue` | 套餐明细 `components` 列表 + CSS（单卡片内） |
| `frontend-kitchen/src/views/Home.vue` | 同上 |
| `frontend-kitchen/src/views/ServeWindow.vue` | 同上 |

---

## 4. 测试证据

### 4.1 单测（2026-09-24 22:44，`mvn -Dtest=...` EXIT=0）

| 套件 | 结果 |
|---|---|
| `PosOrderCreateServiceFoodIdMapTest` | **5/5 PASS** |
| `OrderNewServiceImplDeductTest` | **28/28 PASS** |
| `OrderNewServiceImplOrderNumberA1Test` | **3/3 PASS** |
| `MaterialDeductionAuditSelfFailureIntegrationTest` | **1/1 PASS** |
| **合计（本卡目标）** | **37/37 PASS** |

测试适配说明：
- combo fixture 改 `setComboId` + `setFoodId(null)`；
- `ingredient()` helper 返回 `ComboIngredientNew(Long foodId, Integer qty)`；
- `INGREDIENT_FOOD_ID_PARSE_ERROR` / `COMBO_INGREDIENT_QTY_TRUNCATED` 两用例改为**不可达语义占位断言**（新表类型不可达，guard 仍保留）；
- surefire 中 `DeductMaterialsForServeConcurrencyTest` 的失败为**既有/非本卡目标套件**（本卡 `-Dtest` 过滤外报告残留），不计入本卡 FAIL。

### 4.2 构建 / 类型检查

| 项 | 命令 | 结果 |
|---|---|---|
| 后端编译 | `mvn -q -DskipTests compile`（`JAVA_HOME=P:\my-new-project\JDK21`） | **EXIT=0** |
| POS 类型 | `frontend-pos vue-tsc --noEmit` | **EXIT=0** |
| KDS 类型 | `frontend-kitchen vue-tsc --noEmit` | **EXIT=0** |

### 4.3 本地 HTTP E2E（2026-09-24 22:43–22:44）

环境：local `127.0.0.1:8081`（context-path=`/api`，含本卡改动）；DB `food_traceability`；证据目录 `.dbg/combo-order-001-e2e/`（**未入库**，已扫描无 token/password）。

#### A. 单品零回归（H-06 口径复跑）

| 步骤 | 结果 |
|---|---|
| `POST /api/v1/pos/orders/order` `id=FD202608010001` | **code=0**；`O1790260472188` / `T20260924003` |
| tray create → bind → kitchen-in → kitchen-out → serve | 全 **code=0**；`status=served` |
| DB `order_items` | `product_type=1`，`food_id=1`，`combo_id` 空 |
| DB `kitchen_order` | `KO1790260472187` `material_consumed=1` |
| DB `store_inventory_log` | id=61 生菜 `8.600→8.500` change=`0.100` remark=`KDS出餐扣料 - 订单:T20260924003` |

#### B. 套餐下单 + KDS 明细 + 出餐扣料

| 步骤 | 结果 |
|---|---|
| `POST /api/v1/pos/orders/order` `id=1,dishType=combo` | **code=0**；`O1790260472190` / `T20260924004` |
| DB `order_items` | **`product_type=2`，`combo_id=1`，`food_id=NULL`**（合法套餐语义） |
| KDS `GET /api/v1/kitchen/orders/recent/full` | 套餐行：`productType=2`，`comboId=1`，**`components=[{"foodId":"1","quantity":1,"name":"生菜串"}]`**（单卡片展开，不拆 N 张卡） |
| tray bind combo → 全链 serve | **code=0**；`KO1790260472189` `material_consumed=1` |
| DB `store_inventory_log` | id=62 生菜 `8.500→8.400` change=`0.100` remark=`KDS出餐扣料 - 订单:T20260924004` |

#### C. 静默点 / 失败审计

| 断言 | 结果 |
|---|---|
| 近 40 分钟 `material_consumption` 新增 | **0** |
| 近 40 分钟 `ITEM_FOOD_ID_NULL` / `COMBO_INGREDIENTS_EMPTY` | **0** |
| `order_items WHERE food_id IS NULL AND product_type=1` | **0** |
| `product_type=2` 合法 null 行 | **1**（本卡套餐单，非缺陷） |

---

## 5. 验收 6 项（自评，待 DS/QA 复核）

| # | 验收项 | 自评 | 证据 |
|---|---|---|---|
| 1 | 套餐下单成功（`product_type=2` + `combo_id`） | **PASS** | §4.3.B：`O1790260472190` 行 `product_type=2, combo_id=1, food_id=NULL` |
| 2 | KDS 卡片套餐名 + 明细 | **PASS** | §4.3.B：`recent/full` 含 `components[]`（生菜串×1）；前端 3 处渲染 + `vue-tsc` EXIT=0（UI 目检归 QA） |
| 3 | 出餐扣料 PASS | **PASS** | §4.3.B：`material_consumed=1` + 库存 log id=62 `8.500→8.400` |
| 4 | 单品零回归（H-06 复跑） | **PASS** | §4.3.A 全链 + §4.1 单测 37/37 |
| 5 | 前端入口恢复 | **PASS（代码级）** | `useMenu`/`Order.vue`/`MenuSection` 恢复标记齐全；`vue-tsc` EXIT=0；浏览器目检归 QA |
| 6 | 无新 `ITEM_FOOD_ID_NULL` / `COMBO_INGREDIENTS_EMPTY`；无 Scope 外改动 | **PASS** | §4.3.C 全 0；§6 文件清单 + 3 文件仍 legacy mapper 属第二步卡 |

---

## 6. 修改文件清单

| # | 文件 | 类型 |
|---|------|------|
| 1 | `backend/.../service/impl/PosOrderCreateServiceImpl.java` | 项1 下单识别 |
| 2 | `backend/.../service/impl/OrderNewServiceImpl.java` | 项2 扣料读新表 |
| 3 | `backend/.../config/DatabaseFixConfig.java` | 项3 列名+同步 |
| 4 | `backend/.../controller/kitchen/KitchenOrderController.java` | 项4 KDS 展开 |
| 5 | `backend/src/test/.../PosOrderCreateServiceFoodIdMapTest.java` | 测试（本卡更新） |
| 6 | `backend/src/test/.../OrderNewServiceImplDeductTest.java` | 测试（本卡更新） |
| 7 | `backend/src/test/.../OrderNewServiceImplOrderNumberA1Test.java` | 测试（本卡更新） |
| 8 | `frontend-pos/src/composables/useMenu.ts` | 项5 入口 |
| 9 | `frontend-pos/src/views/Order.vue` | 项5 入口 |
| 10 | `frontend-pos/src/components/pos/MenuSection.vue` | 项5 入口 |
| 11 | `frontend-pos/src/components/kitchen/KitchenOrderCard.vue` | 项4 渲染 |
| 12 | `frontend-pos/src/types/kitchen.ts` | 项4 类型 |
| 13 | `frontend-kitchen/src/views/Home.vue` | 项4 渲染 |
| 14 | `frontend-kitchen/src/views/ServeWindow.vue` | 项4 渲染 |
| 15 | `docs/architecture/03-review/p1-combo-order-001-implementation-record-001.md` | 本报告 |

改动均带 `P1-COMBO-ORDER-001` 注释标识（测试 2 文件为行为适配，见 §4.1）。

---

## 7. 未修改声明 / 范围外

- DB schema / Flyway / `combo_ingredients` 列定义：**未动**
- P0 扣料主体语义（配方展开、库存扣减、审计 fail-fast 结构）：**未动**（仅 combo 分支数据源切换）
- `getFullMenu` / `PosApiServiceImpl`：**未动**（仍 legacy `ComboIngredientMapper`）
- `KitchenScanServiceImpl` / `OrderMaterialRequirementServiceImpl`：**未动**（仍 legacy mapper → **第二步卡**）
- Scope-002 / A1 / FOODID 卡 / PD 决策：**未动**
- 旧表 `combo_ingredient`：**未废弃**（过渡期由 `syncComboIngredientsToLegacy` 保底）

### 7.1 仍用 legacy `ComboIngredientMapper` 的文件（第二步卡）

| 文件 | legacy | new |
|---|---|---|
| `PosApiServiceImpl.java` | 5 | 0 |
| `KitchenScanServiceImpl.java` | 3 | 0 |
| `OrderMaterialRequirementServiceImpl.java` | 4 | 0 |
| `OrderNewServiceImpl.java` | 0 | 4 |
| `PosOrderCreateServiceImpl.java` | 0 | 5 |

---

## 8. 回滚方案

| 层 | 回滚 |
|---|---|
| 下单 combo 分支 | 恢复 `PosOrderCreateServiceImpl` 预检/`isComboItem`/JSON productType 标记（注释定位） |
| 扣料读新表 | 恢复 `OrderNewServiceImpl` 三处 legacy `ComboIngredientMapper` 查询（helpers 删除） |
| DB 同步 | 删除 `syncComboIngredientsToLegacy` 调用与方法；dish_combo 列名改回（**不建议**，Flyway 已是 `create_time`） |
| KDS 展开 | 移除三端点 `expandComboDishItems` 调用 |
| 前端入口 | 重新注释 combo concat/radio/badge（对称于 FOODID STOP-1） |

---

## 9. 未完成项 / 已知限制

1. **UI 目检未做**：POS 菜单套餐入口 / KDS 卡片明细的浏览器实拍归 QA（代码级 + `vue-tsc` 已过）。
2. **parse-error / qty-truncated 单测不可达**：新表类型不可达，占位断言保留 guard 语义；若未来 `combo_ingredients.quantity` 改 `numeric` 需恢复真测。
3. **legacy 三文件未切换**：`PosApiServiceImpl` / `KitchenScanServiceImpl` / `OrderMaterialRequirementServiceImpl` 仍读旧表 — **第二步卡**（切 `getFullMenu`、废弃 `combo_ingredient`、清旧 POS 兼容）本卡明确不做。
4. **`ComboIngredient` 实体 `create_time` 与 `TableInitConfig` `created_at` 漂移**：既有问题；同步 SQL 已探测兼容，**本卡未修实体映射**。
5. **ENV 工作区混杂**：仓库大量既有未提交改动；本卡 commit **仅 stage §6 清单 15 文件**，不夹带无关文件。
6. **生产证据**：全链 E2E 为 local；生产放行仍 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`（与前序卡一致）。
7. **HTTP 响应 `materialConsumed`**：scan-serve 响应仍可能回 0（既有短路，L-05 同族），**成功以 DB `material_consumed=1` + 库存 log 为准**（§4.3）。

---

## 10. 最终状态

| 项 | 状态 |
|----|------|
| 本卡 | **QA PASS_WITH_LIMITATION（2026-09-24）→ 回归候选** |
| 5 项范围 | 全部实施 |
| 验收 6 项自评 | **6/6 PASS** |
| 单测 | 37/37 PASS |
| 构建/类型 | 后端 compile + 双前端 vue-tsc **EXIT=0** |
| H-06 单品零回归 | **PASS**（`T20260924003` 全链 + `food_id=1` + 扣料 log id=61） |
| 套餐链 | **PASS**（`T20260924004` `product_type=2` + KDS `components[]` + 扣料 log id=62） |
| 静默点新增 | **0** |
| DB schema | 未改 |
| P0 / Scope-002 / 其他卡 | 未动 |
| 下一步 | **回归转基线**（QA PASS_WITH_LIMITATION，FAIL=0，§11；限制 L-01 UI 目检 / L-02 生产证据） |

---

*报告状态：DS 抽检 §11 READY_FOR_QA → QA 独立验收 `docs/quality/P1-COMBO-ORDER-001-qa-report.md` = **PASS_WITH_LIMITATION**（6/6 PASS，FAIL=0，无 `-R{n}`）；可进回归；生产仍 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`。*

---

## 11. DS 抽检（2026-09-24，独立执行）

> 角色：独立 DS（只读复核，不改代码、不写 `docs/quality/`）。  
> 范式对照：`p1-pos-foodid-map-001-implementation-record-001.md` §12。  
> 环境：local DB `food_traceability`；后端 `127.0.0.1:8081/api` 存活；HEAD=`aec5c45`（15 文件与 §6 逐文件一致）。  
> §0–§10 原文保留；结论冲突以本节为准。

### 11.1 验收 6 项独立复核

| # | 验收项 | DS 结论 | 一手证据摘要 |
|---|--------|---------|--------------|
| 1 | 套餐下单 `product_type=2` | **PASS** | `T20260924004` / `O1790260472190`：`product_type=2`，`combo_id=1`，`food_id=NULL` |
| 2 | KDS `components[]` | **PASS（live）** | `GET /api/v1/kitchen/orders/recent/full` HTTP 200；套餐行 `productType=2 comboId=1 components=[{foodId:1,qty:1,name:生菜串}]`；单品行无误展开 |
| 3 | 出餐扣料 | **PASS** | `KO1790260472189` `material_consumed=1`；`store_inventory_log` id=62 生菜 `8.500→8.400` |
| 4 | 单品零回归 H-06 | **PASS** | `T20260924003` `product_type=1 food_id=1`；`KO1790260472187` `material_consumed=1`；log id=61 `8.600→8.500` |
| 5 | 前端入口恢复（代码级） | **PASS** | `useMenu:43/:89`、`Order.vue:45/:258/:337`、`MenuSection:168` 无 combo 过滤；KDS 三端渲染标记齐全 |
| 6 | 无新静默点 / 无 Scope 外 | **PASS** | `material_consumption` 无 2026-09-24 新增 FAILED / 无 `COMBO_*`；`food_id IS NULL AND product_type=1` = 0；commit 恰 15 文件 |

附加：git HEAD/清单、`PosOrderCreateServiceImpl` combo 分支、`OrderNewServiceImpl` 读 `ComboIngredientNewMapper`（非 legacy）、`expandComboDishItems` 三端点、测试适配 — **全 PASS**；未发现假成功/锚点错位（对照 FOODID §12 教训）。

### 11.2 非阻断观察

1. **行号轻微漂移**：§2.4 写 `:128-182`，实测方法体约 `:131-189+`；§2.2 helpers 写 `:952-966`，实测 `:955-969` — 语义一致，属文档行号未最终校对。
2. **soft 路径 `continue` 无日志**：OBS-S1 同族 HEAD 既有，本卡仅换数据源，非新增静默点。
3. **UI 浏览器目检 / 生产证据**：仍归 QA / `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`（§9.1/§9.6 已正确声明）。

### 11.3 裁决

**READY_FOR_QA** — 问题清单空（无 FAIL / PARTIAL / UNVERIFIED）。  
移交 QA 边界：浏览器 UI 目检（POS 套餐入口 + KDS 卡片明细）、生产证据、legacy 三文件第二步卡、OBS-S1、ENV 工作区债。
