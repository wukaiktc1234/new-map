# P1-POS-MENU-UNIFICATION-001 — 实施报告（阶段 3a）

## 0. 状态

| 项 | 值 |
|---|---|
| Task ID | `P1-POS-MENU-UNIFICATION-001` |
| Stage | **阶段 3a 完成，停点等 Owner 安排 3b 观察窗口**（按卡 §3a 完成后停下） |
| 日期 | 2026-09-27 |
| 前置 | 阶段 1 诊断 `docs/quality/pos-menu-inventory-category-diagnostics-001.md`；阶段 2 设计 `docs/design/pos-menu-unification-design-001.md` |
| Owner 裁定 | ① 方案 A（前端直连 3 个 product-center 接口）② stock 三态本轮**不显示具体数字** ③ 401 独立建卡 ④ Q4-P1 留 3d |
| 代码变更 | 前端 3 文件（1 新 composable + 2 视图数据源切换）；**后端 0 改动**（方案 A） |
| R1 门禁 | **6/6 PASS**（§3），切流放行 |
| 构建 | `vue-tsc && vite build` EXIT=0（零新增类型错误） |
| 灰度 | 按页切换（无 feature flag，build 级）：CustomerOrder 先切 → 观察 1-3 营业日 → Order.vue（Owner 裁定 §8-5） |
| 下一步 | 3b 观察窗口（A1-A10 + A9 访问日志归零）→ 3c legacy 下线（410→删）→ 3d |

---

## 1. 改动明细

### 1.1 `frontend-pos/src/composables/usePosMenu.ts`（新增）

| 位置 | 内容 |
|---|---|
| `:25` | `activeCategory` ref（默认 `'all'`） |
| `:39-42` | `loadMenu(silent)` = `Promise.all([posApi.listFoodsOnSale(), posApi.listCombosOnSale(), posApi.listEnabledCategories()])` —— 与 Home.vue `useMenu.ts` **同三接口同 converter**，但**独立实现**（不 import useMenu，保 Home 零 diff，A8） |
| `:48` | 有套餐时注入 `combo` 虚拟分类（`sortOrder: 999`），与 Order.vue 既有 UI 一致 |
| `:56-59` | 切换后 `activeCategory` 失效（分类被删）→ 回退 `'all'` |
| 返回 | `{ loading, loadError, categories, dishes, combos, activeCategory, loadMenu }` |

### 1.2 `frontend-pos/src/views/Order.vue`（4 处）

| 位置 | 改动 |
|---|---|
| `:196` | import 改 `usePosMenu`（原 `posApi` 类型 import 删除） |
| `:212-220` | 菜单状态（`loading/categories/dishes/combos/activeCategory/loadMenu`）全部改由 composable 提供；本地 `loadMenu` 函数删除 |
| `:254-257` | **R1 关键修复**：`filteredItems` 单品 `id: d.dishId` → **`id: d.dishCode`**（带 R1 注释：canonical 路径按 food_code 建映射/扣减） |
| `:269` | 套餐 `id: c.comboId`（既有，未动） |
| `:342-343` | `onMounted` → `await loadMenu(); updateIndicator()` |

### 1.3 `frontend-pos/src/views/CustomerOrder.vue`（4 处）

| 位置 | 改动 |
|---|---|
| import | 新增 `usePosMenu`；`request`（`'../api/request'`）保留——tables/orders 等 3 处仍用 |
| 状态块 | 本地 `activeCategory` ref 保留（本页既有行为）；composable 解构为 `menuCategories/menuDishes/loadMenuData` |
| `categories` computed | 首项 `{categoryId:'all', categoryName:'推荐'}`（既有 UI）+ `menuCategories`；**排除 `combo` 虚拟分类**（本页只展示单品、无套餐区块——避免侧边栏出现空分类，切换前本页无"套餐"入口） |
| `dishes` computed | 产品中心 `Dish` → 本页本地形状：`id: d.dishCode`（R1 身份键）、`name/price/description/image/categoryId`、`dishType:'single'`；`salesCount/likeCount` 不传 → UI 显示 `'--'`（与切换前一致，模板 `?? '--'`） |
| `loadMenu` | 改 `await loadMenuData()`（本地 `loading` ref 保留，finally 置 false） |

---

## 2. R1 六步验证（设计 §7.3，全过方可切流）

**结论：1-5 全 PASS → 门禁放行。**

| 步 | 方法 | 结果 |
|---|---|---|
| 1 前端 payload 静态审计 | 身份键链路：Order `filteredItems` 单品 `id`（原 `d.dishId`，**已修**为 `d.dishCode`）/ 套餐 `id: c.comboId`（既有）→ `goToPayment` 经 query 传 Payment.vue → `usePayment` `posApi.createOrder` 透传 `item.id`（Payment.vue/usePayment.ts 未改）；CustomerOrder `addToCart` → `buildOrderPayload` 透传 `item.id`（`dishes` computed 已用 `dishCode`） | ✅ 两页 payload 身份键 = `dishCode`（单品）/ `comboId`（套餐），源自 `foodVOToDish` `vo.foodCode` |
| 2 后端消费静态审计 | `PosOrderCreateServiceImpl` canonical 路径：预检 L236-260（combo 走 combo 分支校验存在性+配料非空；单品 L257 须 `foodCodeToIdMap` 命中，按 `food_code IN (...)` 构建）；扣减 L553-563 `foodCodeForDeduct = dbFood.getFoodCode()` → `FoodMapper.deductStock` L72 / `FoodNewMapper.deductStock` L54 均 `WHERE food_code = #{foodCode}`；**不消费 dishId** | ✅ 扣减键 = food_code；payload `id` 必须是 foodCode 字符串（单品）/ 数字 comboId（套餐） |
| 3 e2e 正例（活体） | `POST /api/v1/pos/orders/order` `{items:[{id:'FD202608010001', name:'生菜串', price:0.80, quantity:2, dishType:'single'}], totalAmount:1.60, orderType:'dinein'}` → 200 | ✅ `order_items.food_id`=**1（数字，非字符串/非他菜）**；`foods.stock` 73→71（−2）；legacy `food.stock` 73→71（−2，双写一致）；`payload.id == foods.food_code`。复跑（新 JAR，qty=1）T20260927005 同断言全过 |
| 4 e2e 交叉例（活体） | 另一分类菜品 `FD260926006`（category 3，其余菜均 category 745）：先 `UPDATE foods/food SET stock=5`，`POST /api/v1/pos/orders/order/table` `{tableId:1, tableNumber:'T1', items:[{id:'FD260926006', quantity:1}]}` → 200 | ✅ `order_items.food_id`=**8**（正确菜，非分类 id 3）；两表 stock 5→4（−1）；测试后基线恢复 0/0。复跑（新 JAR）T20260927006 同断言过 |
| 5 套餐路径（活体） | `POST /api/v1/pos/orders/order` `{items:[{id:'1', name:'生菜套餐', price:13.50, quantity:1, dishType:'combo'}]}` → 200 T20260927004 | ✅ `order_items`：`food_id=NULL, product_type=2, combo_id=1, unit_price=1350`；KDS `kitchen_order.dish_items` JSON 含 `productType:2, comboId:1`；创建期 `foods.stock` 不变（组件扣减在**出餐扣料路径** `OrderNewServiceImpl` 软/硬/退款三处展开 `combo_ingredients`→food_id 扣减，与 P1-COMBO-ORDER-001 REG-ORDER-012 基线一致）；组件命中 `combo_ingredients(combo_id=1)→food_id=1` 正确 |
| 6 门禁 | 1-5 全过 | ✅ 切流放行（回滚面 = 前端 build） |

### 2.1 ENV 发现：本地 JAR 过期（初跑 R1-5 假阴性）

- `target/food-traceability-1.0.0.jar` 构建于 **2026-09-17**，而 `PosOrderCreateServiceImpl.java` 修改于 **2026-09-26**（P1-COMBO-ORDER-001 改动未打包）→ 初跑 R1-5 对旧 JAR：套餐走了**单品分支**（`product_type=1`、`food_id=NULL` warn-then-insert、DB 价格覆盖 80 分、`foods.stock` 71→70 误扣）。
- 处置：`mvn package -DskipTests`（EXIT=0）重打 JAR（2026-09-27 01:28），R1-3/4/5 全部对**同一新 JAR** 复跑 PASS（T20260927004-006）。
- 建议登记 **ENV-3**（`production-known-limitations.md`）：本地 e2e 前核对 `jar LastWriteTime` 晚于被验源码；不 rewrite 历史。

### 2.2 测试单（dev 数据，未清理）

| 订单 | 路径 | JAR | 用途 |
|---|---|---|---|
| T20260927001 | /order 单品 ×2 | 旧 | R1-3 初跑 |
| T20260927002 | /order/table 交叉 | 旧 | R1-4 初跑 |
| T20260927003 | /order 套餐 | 旧 | R1-5 初跑（假阴性来源） |
| T20260927004 | /order 套餐 | 新 | R1-5 复跑 PASS |
| T20260927005 | /order 单品 ×1 | 新 | R1-3 复跑 PASS |
| T20260927006 | /order/table 交叉 | 新 | R1-4 复跑 PASS |

数据变化：`foods`/`food` food 1 stock 73→69（e2e 真实扣减路径，不手工回滚）；food 8 已恢复 0/0。

---

## 3. gap 表逐项核销（设计 §7.2，无"待定"）

**端点级**：

| 项 | 处置 | 核实 |
|---|---|---|
| 请求 1→3 并行 | `usePosMenu` `Promise.all` 三接口 | ✅ 与 Home 同模式（已生产运行） |
| 认证对等 | 三接口 JWT 同 `anyRequest`（§2.4） | ✅ CustomerOrder 免登录 401 = 既有风险 R2 → **独立卡**（Owner 裁定③，本卡不管） |
| 数据源归一 | `foods` on-sale + `food_categories` enabled | ✅ 两页不再读 legacy `food`/`food_category` |
| 分类形状 code→数字 id 字符串 | radio `:label`/`activeCategory` 均为字符串比较，兼容 | ✅ "其他"兜底消失（R3，新路径以 `food_categories` 真相为准，无 code 查表兜底） |
| 套餐 `peopleCount` 缺失（R4） | **两页模板均不消费 `peopleCount`**（grep 无引用） | ✅ 无影响 |
| 套餐 `items[].foodId` 语义（R9） | Order.vue `:key` 展示用，无下游（不入 payload） | ✅ 无影响（payload 套餐只传 `id=comboId`） |

**字段级（菜品）**：

| 字段 | 处置 | 核实 |
|---|---|---|
| `dishId` 语义翻转（R1） | 两页 payload `id` 改用 `dishCode`（Order `:257` / CustomerOrder `dishes` computed） | ✅ e2e 正例+交叉例 PASS |
| `categoryId` 名字→数字串 | radio 字符串比较兼容；`activeCategory` 失效回退 `'all'`（composable `:56-59`） | ✅ |
| `price` 分→元 | `fenToYuanNumber`（converter 既有） | ✅ e2e 响应 `totalAmount` 0.80/12.00/13.50 元正确（A10） |
| `stock` 新增数据 | **UI 不显示**（两页模板本就无 stock 渲染；Owner 裁定② 三态数字等 B 卡） | ✅ |
| `description`/`imageUrl` | 同源直出 | ✅ |
| `salesCount`/`likeCount` | 不传 → `'--'`（切换前 legacy 亦无此数据，UI 一致） | ✅ |

---

## 4. 禁止项遵守

| 禁止项 | 状态 |
|---|---|
| 不改后端接口 | ✅ 后端 0 改动（方案 A） |
| 不删 legacy 接口（留 3c） | ✅ `posApi.getFullMenu` 前端包装保留（调用方 0，死代码待 3c 删） |
| 不改 `syncLegacyFood` 双写（留 3d） | ✅ 未动；e2e 双写断言（R1-3/4）仍成立 |
| 不动 inventory/category/stock 相关代码 | ✅ 未动（backend diff = 0） |
| 不显示 stock 数字 | ✅ |
| 不扩 3b-3d | ✅ |
| 不顺手修其他 | ✅ `usePayment.ts`（金额/现金标签）等既有未提交改动**未入本 commit** |
| CustomerOrder `getCategoryCartCount` 既有 bug | 未修；切换后 `item.categoryId` 与 `cat.categoryId` 同为数字串 → badge 计数由"永不命中"变为正确命中（数据统一的附带行为，登记为观察项，非本卡 bug fix） |

**Home.vue 零 diff**：`git diff -- frontend-pos/src/views/Home.vue` = 空；`useMenu.ts` 亦零 diff（A8 ✅）。

---

## 5. 构建与类型检查

- `npm run build`（`vue-tsc && vite build`）**EXIT=0**，零新增类型错误；vite chunk size warning 为既有。
- 沙箱注记：vite 阶段 esbuild 子进程 `spawn EPERM`（workspace-write 边界），`danger-full-access` 一次升级重跑即过——与既有 ENV 记录一致，非代码问题。

---

## 6. 验收自评（3a 面）

| 验收项 | 状态 | 依据 |
|---|---|---|
| A1 Order.vue 菜单 == 新表（名/价/分类逐条） | **自评 PASS**（数据源侧） | 三页同三接口同 converter；页面渲染回归留 3b 观察窗口 |
| A2 CustomerOrder.vue 同 A1 | **自评 PASS**（数据源侧） | 同上；CustomerOrder 先切（灰度第 1 页） |
| A5 `order_items.food_id` 一致 + `payload.id == foods.food_code` + 双表 stock 同扣 | **PASS** | e2e R1-3/R1-4 活体断言 |
| A8 Home.vue 零 diff | **PASS** | git diff 空 |
| A9 `/v1/pos/api/menu` 访问日志归零 | **待 3b 观察窗口** | 静态残留调用方 = 0（全仓 grep 仅剩 `posApi.ts` L415-416 死定义 + CustomerOrder 注释） |
| A10 价格展示 = `foods.salePrice`（分→元） | **PASS** | converter `fenToYuanNumber` + e2e `totalAmount` 断言 |

---

## 7. 环境

- PG 18 本地（PID 13792, :5432, `food_traceability`）；后端新 JAR（job `pwsh-4`，:8081，profile pg）；登录 `admin/Admin@123`。
- 前端 dev 未启（build 级验证）；SPA 单门店，无 feature flag。

---

## 8. 残留 / 登记建议

| # | 项 | 去向 |
|---|---|---|
| 1 | **ENV-3**（建议）：本地 JAR 过期导致 e2e 假阴性（§2.1） | `production-known-limitations.md` |
| 2 | R2 CustomerOrder 免登录 401 | 独立卡（Owner 裁定③） |
| 3 | Q4-P1 | 3d（Owner 裁定④） |
| 4 | legacy 4 个死接口 + `posApi.getFullMenu` 前端死代码 | 3b 观察窗口 → 3c 下线（410 一周 → 删） |
| 5 | `syncLegacyFood` 双写废弃依赖全景 | 3d |
| 6 | CustomerOrder badge 计数由永不命中变正确命中 | 观察项（3b 窗口内确认无 UI 异常） |

---

## 9. Commit

单次：`feat(pos): unify Order/CustomerOrder menu source`

文件（4）：
- `frontend-pos/src/composables/usePosMenu.ts`（新增）
- `frontend-pos/src/views/Order.vue`
- `frontend-pos/src/views/CustomerOrder.vue`
- `docs/architecture/03-review/p1-pos-menu-unification-001-implementation-record-001.md`（新增）
