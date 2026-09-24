# P1-COMBO-LEGACY-CLEANUP-001 — QA 独立验收报告

- **日期**：2026-09-25
- **验收对象**：commit `05d4404`（切表代码 4 文件）/ `58329c8`+`11e05d4`（实施记录）；DS 抽检 `docs/quality/P1-COMBO-LEGACY-CLEANUP-001-sampling-review.md`（5/5 PASS）
- **验收方式**：**活体验证为主**——重启本地后端至当前构建（旧实例 2026-09-24 22:34 启动不含本卡代码，PID 2668 已停），真实 HTTP 调用 + psql DB 断言独立复现
- **环境**：本地 dev（` Started FoodTraceabilityApplication in 26.105s`，03:21:17）；PostgreSQL 18 @ localhost/food_traceability；JDK Temurin 25

## 1. 活体验证结果

### 活体 1 — POS 拉菜单（套餐数据源 = dish_combos 新表）

| 断言 | 结果 | 一手证据 |
|------|------|----------|
| 套餐列表非空（来自 dish_combos） | **PASS** | `GET /api/v1/pos/api/combos` code=0，`combos=[{comboId:"1", comboCode:"CB202608010001", comboName:"生菜套餐"}]`（该行仅存在于 dish_combos 新表：combo_id=1, combo_price=1350） |
| 价格分→元转换 | **PASS** | `price=13.5`（DB combo_price=1350 分 → 13.50 元）；食材 `price=0.8`（foods.sale_price=80 分） |
| 食材/成分反查正确 | **PASS** | `items=[{foodId:"FD202608010001", quantity:1, foodName:"生菜串", price:0.8}]`——combo_ingredients.food_id(Long)→foods 反查→food_code 正确回填 |
| 完整聚合 `/api/v1/pos/api/menu` | **500（既有缺陷，非本卡）** | 见 §3 观察项 OBS-1——`getCategories` 查 `food_category` 报「字段 id 不存在」，该方法/表均不在本卡 4 文件与数据链内 |

### 活体 2 — POS 下单套餐 + DB 断言

| 断言 | 结果 | 一手证据 |
|------|------|----------|
| 下单成功 | **PASS** | `POST /api/v1/pos/orders/order`（id=1, dishType=combo, price=13.5）→ **code=0**，`O1790277670326` / `T20260925001` |
| `order_items.product_type=2` | **PASS** | psql：`item_id=9086…da83, product_type=2, combo_id=1, food_id=NULL(空), product_name=生菜套餐, unit_price=1350` |
| `combo_id` 非空 + `food_id=NULL` | **PASS** | 同上行——合法套餐语义三字段全部正确 |

### 活体 3 — KDS 拉单 components 展开

| 断言 | 结果 | 一手证据 |
|------|------|----------|
| 套餐 item 含 components 数组 | **PASS** | `GET /api/v1/kitchen/orders/recent/full` code=0；`T20260925001` 行 `dishItems` 内嵌 JSON：`productType=2, comboId=1, components=[…]` |
| components 内容正确 | **PASS** | `components=[{"foodId":"1","quantity":1,"name":"生菜串"}]`——foodId/quantity/name 三字段与 combo_ingredients 新表数据一致，单卡片展开不拆分 |

### 活体 4 — KDS 出餐扣料（tray scan-serve 链）

| 断言 | 结果 | 一手证据 |
|------|------|----------|
| 链路全通 | **PASS** | tray `create(QATRAY01)` → `bind-order(id=108)` → `scan-kitchen-in` → `scan-kitchen-out` → `scan-serve` 全 code=0 |
| `kitchen_order.material_consumed=1` | **PASS** | psql：`KO1790277670325, status=served, material_consumed=1` |
| `store_inventory_log` 扣减记录 | **PASS** | `id=65, product_name=生菜, before=8.400, after=8.300, change=0.100, remark=KDS出餐扣料 - 订单:T20260925001` |

## 2. REG-ORDER-012（新增回归，正式基线 125 → 126）

- **测试编号**：REG-ORDER-012
- **模块**：订单-POS 套餐菜单数据源切换（legacy dish_combo/combo_ingredient → dish_combos/combo_ingredients）
- **关联任务**：P1-COMBO-LEGACY-CLEANUP-001（本报告，2026-09-25）
- **覆盖链**：菜单拉取（/combos）→ 套餐显示（价格/成分）→ 下单（product_type=2）→ KDS components[] → scan-serve 扣料
- **测试步骤**：即本报告 §1 活体 1→4 原样复跑（含 psql 三断言）
- **预期**：combos 非空且价格/成分正确；下单 code=0 + order_items 三字段；KDS components[] 正确；material_consumed=1 + 库存 log
- **当前状态**：**PASS（2026-09-25，首跑即本报告 §1 证据）**
- **登记**：`production-regression-test.md` Batch P1-COMBO-LEGACY-CLEANUP-001 节，正式基线 **125 → 126**（只增不减）

## 3. 观察项（不修复、不销号）

| # | 项 | 说明 |
|---|-----|------|
| OBS-1 | `/api/v1/pos/api/menu` 聚合 500 | 根因：`FoodCategory` 实体 @TableId 期望 `id`，本地表 `food_category` 主键为 `category_id` → `getCategories` BadSqlGrammar。**非本卡引入**：getCategories 及 food_category 均不在本卡 4 文件/数据链内（HEAD 既有缺陷，建议独立登记修复卡）；组合断言经 `/combos` 端点完成 |
| OBS-2 | `Long.valueOf(comboId)` 依赖未来调用方（L-02 同族） | `generateRequirementsForCombo` 当前无生产调用方；组合 ID 空间须为 dish_combos.combo_id——与实施记录 L-02 一致，维持观察 |
| OBS-3 | 生产证据 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE` | 与 KL-069/076/079 同族；仅阻断生产放行 |
| OBS-4 | UI 浏览器目检未做 | 与 L-01 家族口径一致（API live 已覆盖可自动化面） |

## 4. 验收结论

| 项 | 结果 |
|----|------|
| 活体 4 项 | **4/4 PASS**（菜单套餐部分 / 下单+DB / KDS components / scan-serve 扣料） |
| DB 断言 | **3/3 PASS**（order_items 三字段 / material_consumed / 库存 log id=65） |
| FAIL 项 | **无** → 无 `-R{n}` |
| 限制 | OBS-1（既有聚合菜单缺陷，非本卡）/ OBS-3（生产 PROVISIONAL）/ OBS-4（UI 目检）——逐条不阻断本地放行 |
| **QA 结论** | **PASS_WITH_LIMITATION**——切表后功能真实工作（活体证据链完整）；可进回归（REG-ORDER-012 已随验收首跑转正） |
