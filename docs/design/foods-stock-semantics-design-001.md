# P1-FOODS-STOCK-SEMANTICS-001 — foods.stock 语义重建：POS 三态可售（阶段 2：设计）

- **日期**：2026-09-26
- **阶段**：阶段 2（设计，只出方案）— 未改代码 / 未改 DB / 未建实施卡
- **设计输入**：诊断报告 `docs/quality/foods-stock-semantics-diagnosis-001.md`（D1–D8 + §8 登记）
- **证据规范**：结论均附 file:line；依赖生产数据的结论标 PROVISIONAL
- **PG-005 阶段 2 纪律**：本文件只出方案；"紧张"阈值等未擅自裁定，给推荐值 + 理由，标"待 Owner 拍板"

---

## 0. Owner 已裁定（设计输入，原样载入）

| # | 裁定 |
|---|------|
| O1 | POS 端菜品卡片应让收银员能预判"能不能点" |
| O2 | 不显示精确份数（共享原料无法精确计算） |
| O3 | 采用"三态"方案：**充足 / 紧张 / 售罄** |
| O4 | 后端强校验：下单时基于原料库存拒绝不足订单 |
| O5 | 后厨 86 管理（P1-MANUAL-86-001）作为**人工覆盖层**叠加（独立卡） |

---

## 1. 范围

**入范围**：
1. 三态判定算法（配方派生 / 无配方回退 / 套餐口径）
2. StockForecast 引擎接线（派生计算批量化为菜单路径）
3. 判定触发时机 + 实时推送接线（`/topic/menu/update` 生产者目前休眠，见 §13-1）
4. 过渡方案（B 实施前的临时规则）与替换点
5. 后端强校验（下单预检 + 错误文案 + 并发边界）
6. 与 86 卡、C 卡的接口契约
7. `FoodVO` / `ComboVO` 新字段定义

**出范围**（归其他卡）：
- 86 覆盖表 / 后厨 UI / 权限（P1-MANUAL-86-001）
- Order.vue / CustomerOrder.vue 菜单数据源切换（P1-POS-MENU-UNIFICATION-001，本卡只定义它们将继承的字段契约）
- `syncLegacyFood` 废弃 / 双表去留（C 卡 Q4-P1~P6）
- 双 BOM 体系合并（G4：dish_inventory 死路径处置）——本卡只**消费** dish_recipes，不统一
- foods.stock 变动台账（G7）
- 套餐独立库存对象（§8-7 套餐口径本卡只给派生方案）

---

## 2. 事实基线（设计用证据）

| 事实 | 证据 |
|------|------|
| 派生引擎已实现：单菜 `forecast(dishId, days)`，逐原料 `floor(可用库存 / 每份用量)` 取 MIN = maxServings | `StockForecastServiceImpl.java:100-138`（`maxActual`/`minActual`）；原料可用 = `store_inventory.current_stock`（`StockForecastMapper:74-77`） |
| 引擎每份用量自校准（近 N 天实际出库 ÷ 售出份数，数据不足回退理论 `required_quantity`） | `StockForecastServiceImpl:85-93` |
| 引擎消费方 = 仅管理端"检查库存"按钮（单菜、按需） | `FoodManagement.vue:673-698`（按钮 :948）→ `BomCheckController:167-173`；frontend-pos 全文 0 消费（诊断 D5） |
| 引擎 store 上下文 = 当前登录用户门店（`SecurityUtils.getCurrentUserStoreId`）；storeId=null 时 `getAvailableStock` 跨门店求和 | `StockForecastServiceImpl:53-54`；`StockForecastMapper:76` |
| POS 菜单现状：`FoodVO.stock` 原样透传（`FoodServiceImpl:721`），前端 `stock===0` 售罄、`<10` 低库存、999 兜底 | `useMenu.ts:41,63`；`MenuSection.vue:37,50,181`；`posApi.ts:160` |
| 菜单刷新通道：WS `/topic/menu/update` → `loadMenu(true)`；备用 30 分钟轮询 | `Home.vue:509,517,524`；但**推送方法 `pushMenuUpdate()` 全库无调用者**（§13-1） |
| 下单预检点：canonical 路径 Step 3（L213-275，含 foods.stock 预检 L262-275）；legacy 路径无预检、直接扣（L757-772） | `PosOrderCreateServiceImpl.java:213-275,757-772` |
| 扣减原子性：`deductStock` = `UPDATE ... SET stock=stock-? WHERE stock >= ?`（0 行=不足） | `FoodNewMapper.java:54`（诊断 D3） |
| 原料真实扣减点 = **出餐时**（非下单）：`deductMaterialsForServe`（`store_inventory`），失败有审计上下文 | `OrderNewServiceImpl.java:685`；`TrayServiceImpl.java:322`；`MaterialDeductionContext.java` |
| 套餐无独立 foods 库存：组件在 `combo_ingredients`，出餐扣料路径消费 | `PosOrderCreateServiceImpl.java:264` 注释（诊断 §8-7） |
| `sys_settings` 配置表可用（global + scope 两级读取） | `SysSettingMapper.java:20-38` |
| 活库（2026-09-26）：8 菜全部有配方；原料 3（生菜）存于门店 1/2（177.1 / 40.0）；菜 8 原料 36（50.0，门店 1）——**每个配方原料均有 store_inventory 行**（LEFT JOIN 无 null）；菜 8 `foods.stock=0` 但派生 S=50 | 本设计只读查询 `dish_recipes LEFT JOIN store_inventory` |

---

## 3. Q1 — 三态判定算法

### 3.1 核心量：可用份数 S（per dish）

**有配方菜品**（dish_recipes ≥ 1 行）：

```
对配方中每个原料 m:
    perServing_m = required_quantity(m)          （菜单路径用理论用量，见 §3.5 决策 D-3）
    available_m  = SUM(store_inventory.current_stock) WHERE material_id=m AND store_id=<POS门店> AND deleted=0
    servings_m   = floor(available_m / perServing_m)      （perServing_m=0 时该项不计）
S_dish = MIN(servings_m)                                  （最短板原则——与引擎 :111-112 同一策略）
```

- **最短板原则**（问题 1c）：**是**。复用引擎既有 MIN 策略（`StockForecastServiceImpl:111-138`），共享原料天然走同一公式（问题 1d：判定用同一策略——所有菜品从**同一个原料池**按各自 perServing 独立取 MIN，不做分配/预留；显示端只出三态不出现数字，"两菜共享最后一份生菜"在显示上同呈"充足"，冲突在下单强校验处消解，见 §7）。
- **原料无 store_inventory 行**：`available_m = 0` → 该菜 S=0 → 售罄（**严格规则**，决策 D-2 待拍板）。活库已核：现网 8 菜全部原料有行 → 零冲击；风险 = 未来录入原料未建库存行即整菜售罄（R3）。

**无配方菜品**（dish_recipes 0 行，G8）：

```
S_dish = foods.stock      （人工倒计数，Count Down List——行业第二模式，诊断 D7）
```

- 计数器的既有维护链路（下单扣 / 超时回补 / 退款回补，诊断 D3）**保持不变**——它是无配方菜的可售真相，不再是"全菜单的可售真相"。

**套餐**（§8-7）：

```
S_combo = MIN over 组件 c of floor(S_c / quantity_c)      （combo_ingredients.quantity）
```

- 组件 S_c 按上述规则各自解析（递归深度 1，combo_ingredients 指向 foods 行，无嵌套套餐）。组件售罄 → 套餐售罄。
- 决策 D-4（待拍板）：推荐按此派生；备选 = 套餐恒"充足"（简化，但收银员失去预判力，违背 O1）。

### 3.2 三态映射

```
S = 0        →  SOLD_OUT  售罄   （status=0）
0 < S < N    →  TIGHT     紧张   （status=1）
S ≥ N        →  SUFFICIENT 充足  （status=2）
```

**N（紧张阈值）**：
- **推荐 N = 10**（**待 Owner 拍板**，未擅自定死）：
  1. 与前端既有低库存阈值 `stock < 10`（`useMenu.ts:63`、`MenuSection.vue:181`）对齐 → 视觉行为连续，收银员无学习成本；
  2. 与 `FoodVO.minStock` 默认语义（"最低库存预警"，`FoodVO.java:53-54` example=10）同量纲；
  3. 活库 PROVISIONAL：现网 S 值量级 = 400~1771（生菜类）/ 50（菜 8），N=10 下"紧张"在补货滞后时可见、不噪声。
- **可配**：落 `sys_settings`（键 `pos.dish.stock.tightThreshold`，scope_type global + 门店覆盖，读法沿用 `SysSettingMapper:26-32`）。配置行由实施阶段 V-迁移 seed（默认 10）；**不建 UI 编辑入口**（无此需求，Owner 改库即可，登记）。

### 3.3 判定优先级（为 86 预留）

```
state(dish) = 86覆盖(在售86表, 86 卡建) ? SOLD_OUT
             : (有配方 ? 派生S : 计数器S) 按 §3.2 映射
```

- B 卡只实现后两个分支 + 把优先级结构写进 resolver（86 分支留接口、不建表——O5）。

### 3.4 与 foods.stock 计数器的最终关系（G1 处置）

| 菜型 | 状态真相 | foods.stock 角色 |
|------|----------|------------------|
| 有配方 | 派生 S（原料库存） | **状态判定不再消费**；计数扣减/回补链路保留（legacy 表同步 + A5 验收依赖），降为运维观测值 |
| 无配方 | foods.stock 计数器 | 状态真相（0 售罄 / <N 紧张 / ≥N 充足） |

- 显示端（O2）：**两型都不出现数字**；有配方菜显示三态标签，无配方菜同样三态（数字只进管理端）。
- 行为变化点（需 Owner 知晓，R1）：菜 8 现 `foods.stock=0`（POS 售罄）但原料够 50 份 → B 完成后**恢复可点**（派生覆盖计数器）。这正是 D2/G3 陷阱的根治方向（UI 新建菜 stock=0 不再即售罄，有配方时）。

---

## 4. Q2 — StockForecast 引擎接线方案

### 4.1 为什么之前没接（诊断 D5 的归因 + 代码复核）

1. **定位是管理端按需诊断**（方案 D+/E，`StockForecastServiceImpl:49` 单菜入参；`BomCheckController:167-173` 管理端按钮触发），设计时 POS 可售真相 = foods.stock（PADR-003 裁决，诊断 D4）——接 POS 会推翻该裁决，故停在"看板"；
2. **单菜成本结构不适合菜单**：每菜 × 每原料 4 条 SQL，其中 2 条是 7–90 天窗口聚合（`getActualConsumed`/`getSoldServings`，`StockForecastMapper:29-46`，order_items × orders × dish_recipes 三表 JOIN）——N 菜 × M 原料实时跑 = 菜单请求阻塞风险；
3. **无推送生产者**：`pushMenuUpdate()` 全库 0 调用者（§13-1），菜单刷新只靠 WS 事件 + 30 分钟兜底（`Home.vue:508-511`），派生值若接了也没通道推变更；
4. **store 上下文耦合登录用户**（`:53`）——POS 多会话场景未设计。

### 4.2 接线方案（推荐：抽批量派生服务，菜单路径只用理论用量）

**新增 `DishStockStateService`（B 卡核心新件）**：

```
resolveStates(List<Long> foodIds, String storeId): Map<Long, DishStockState>
    // 1 条批量 SQL：dish_recipes × store_inventory（JOIN，deleted=0，store_id=指定）
    //   每 (food_id, material_id): available = SUM(current_stock), perServing = required_quantity
    // Java 侧：按 food 分组 floor 取 MIN → S；无配方 food 回退 foods.stock
    // 映射 §3.2 → {status, name}
```

- **菜单路径不碰自校准**（`getActualConsumed`/`getSoldServings` 不进菜单路径）：窗口聚合只留在管理端 `forecast()` 原样（决策 D-3 待拍板，推荐=是）。理由：
  - 理论用量确定性、无历史数据依赖（新店直接可用）；
  - 自校准已知失真（§8-6 `getSoldServings` cast 少计分母 → 每份用量高估、S 低估）——留在管理端只影响"可做份数看板"精度，不影响 POS 判定；
  - 菜单路径 SQL 数 = 1（JOIN），与菜品/原料规模解耦（问题 3 性能评估见 §5）。
- **引擎映射**（问题 2c）：`forecast().maxServings`（管理端，自校准口径）与 `DishStockStateService.S`（菜单路径，理论口径）**同源公式、不同精度档**；三态映射函数（§3.2）为两者唯一出口——管理端看板显示数字 + 状态，POS 只显示状态。
- **复用方式**：`DishStockStateServiceImpl` 独立批量查询（新 `DishStockStateMapper`，1 条 @Select）；`StockForecastServiceImpl` **不改**（管理端行为零变化，禁顺手修 §8-6）。

### 4.3 接线前置（为什么现在能接）

| 前置 | 状态 |
|------|------|
| 派生公式实现 | ✅ 已有（:100-138），抽为批量版即可 |
| 原料库存台账（store_inventory + log） | ✅ 出餐扣料已收口（P0-KDS-DEDUCT，诊断 D4） |
| POS 菜单数据源 | Home.vue 已走 `foods/on-sale`（`useMenu.ts:79`）；Order/CustomerOrder 切同一接口 = C 卡（本卡只加字段） |
| 推送通道 | ⚠️ 通道在（`/topic/menu/update`）、生产者休眠 → B 卡接线（§5.3） |
| 门店上下文 | ⚠️ 需定（§5.2） |

---

## 5. Q3 — 判定触发时机

### 5.1 三层时机（分工，非二选一）

| 层 | 时机 | 权威 | 用途 |
|----|------|------|------|
| **显示层** | `GET /v1/product-center/foods/on-sale` 请求时**实时批量计算**（无缓存） | 参考 | POS 卡片三态 |
| **校验层** | 下单预检时**实时计算**（同 service，只对订单内菜品） | **权威（O4）** | 拒绝不足订单 |
| **扣减层** | 出餐时 `deductMaterialsForServe`（既有，不动） | 权威（原料账本） | 真实消耗 |

- 为什么显示层实时而不缓存：活库规模（8 菜/1 原料，§2）下 1 条 JOIN 查询 <5ms 级；菜单请求频率 = 页面挂载 + WS 事件 + 30min 兜底，非高频。**规模增长预案**（登记，不实施）：>200 菜时加 60s TTL 内存缓存 + 事件失效。
- 显示值与下单值之间无共享状态 → 无缓存一致性问题；最坏情形 = 菜单显示"充足"而两单并发后原料不够（§7.3 并发窗口）。

### 5.2 门店上下文

- `resolveStates` 入参 `storeId` 显式化（不进 = null = 跨店求和，仅管理端兼容用）。
- POS 菜单/校验取 **当前会话用户门店**（`SecurityUtils.getCurrentUserStoreId`，与引擎 :53 同口径）；canonical 下单路径已有 store 解析（`PosOrderCreateServiceImpl:309-320`，默认 STORE_A）——预检复用同值。
- ⚠️ 活库 PROVISIONAL：原料 3 同时存在于门店 1 与 2（177.1 / 40.0）→ 若 POS 会话落在门店 2，生菜菜 S 从 1771 降到 100。**实施阶段第一步验证**：POS 会话用户的 store 字段值 + `store_inventory` 门店分布（R2）。

### 5.3 变更推送（接线休眠生产者）

- `pushMenuUpdate()`（`OrderWebSocketController:115-122`）现有 0 调用者。B 卡接线点（最小集）：
  1. `deductMaterialsForServe` 成功扣减后（原料账本变化 → 派生 S 可能变）；
  2. 出餐扣减**失败**后（MaterialDeductionContext 审计收尾后）——失败也意味着账面与预期偏离，POS 需刷新；
  3. （86 卡接线：86 开关后——本卡只留调用点注释位）
- 效果：出餐扣料 → 全 POS 终端 `loadMenu(true)`（`Home.vue:509`）→ 三态 ≤1 次刷新更新（对齐 C 卡 A3 验收口径）。
- 不引入新 topic（复用既有 `/topic/menu/update`）。

---

## 6. Q4 — 过渡方案（B 实施前的临时规则）

### 6.1 临时规则（Owner 给定，原样确认）

```
foods.stock == 0    → 售罄
foods.stock == 999  → 充足
其他（1..998）       → 充足
```

- **等价于现状**：前端 `stock===0` 售罄、`<10` 仅低库存样式不拦截（`MenuSection.vue:37,181`）、999 兜底（`posApi.ts:160`）——即临时规则 = "什么都不改，维持当前 POS 行为"。设计确认：**过渡期零改动**（B 实施前无前端/后端动作），仅作为 C 卡切换期与 B 上线前的既定口径写入两卡文档。

### 6.2 B 完成后是否完全替换

**是，且是原子替换（无半态窗口）**：

| 菜型 | 过渡规则 | B 后规则 | 替换点 |
|------|----------|----------|--------|
| 有配方 | foods.stock 三值规则 | 派生 S 三态 | B 上线瞬间（on-sale 响应加 `stockStatus`，前端改读该字段，不再读 stock 数字） |
| 无配方 | 同上 | foods.stock → 三态映射（0 售罄 / <N 紧张 / ≥N 充足） | 同上——注意临时规则的"1..998 全充足"被**细化**为"<N 紧张"（新增状态档，行为变紧） |
| 套餐 | 999 占位（`useMenu.ts:52`） | 组件派生（§3.1） | 同上（若 D-4 拍板"恒充足"则套餐不变） |

- 替换原子性：后端字段 + Home.vue 读取同批上线；上线前前端读 stock 数字（过渡规则），上线后读 stockStatus（派生）——**无"两规则并存"状态**。
- Order.vue / CustomerOrder.vue 在 C 卡切换前仍走 legacy 接口（无 stock 字段，`PosDishDTO` 无 stock）→ 不受影响；C 切换后自动继承 `stockStatus`（§8）。
- 遗留语义清理（登记，不在本卡实施）：legacy 999 默认（`V1.0.0.100:368`）与 `posApi.ts:160` 999 兜底在 B+C 全量后失去显示意义，随 C 卡废弃阶段处置（C 卡 R5）。

---

## 7. Q5 — 后端强校验设计（O4）

### 7.1 校验位置（两条下单路径统一）

**单一新增校验方法**（`DishStockStateService.checkOrderSufficiency(items, storeId)`），插入点：

| 路径 | 插入点 | 说明 |
|------|--------|------|
| canonical（主路径，POS 现用） | **Step 3 预检块内**（`PosOrderCreateServiceImpl:262-275` 既有 foods.stock 预检处）——**统一替换**该预检为 S_effective 检查 | 预检点既有先例：B2 映射 miss 400 拒绝就在此块（`:233-260`） |
| legacy 路径 | 扣减循环（`:757-772`）**之前**新增同款预检（该路径现无预检，直接扣） | 两条路径共享同一 service 方法，语义一致 |

**统一判定式**（取代 canonical 路径 L270-274 的裸 `stock < quantity`）：

```
对订单项 item（非套餐）:
    S_eff = 有配方 ? resolveStates(derived).get(foodId).servings
                : foods.stock        （计数器，与 :270 同源）
    if (S_eff < item.quantity) → 拒绝
```

- 套餐项：组件派生 S_combo < quantity → 拒绝（D-4 拍板"恒充足"则套餐跳过）。
- 与既有 B2 预检（映射 miss）、套餐 combo_id/配料预检（:240-256）互不干扰，顺序：B2 → 本卡 → 金额计算。

### 7.2 拒绝错误文案

| 情形 | 文案（400） |
|------|-------------|
| 有配方菜不足 | `菜品【{foodName}】原料不足，当前仅可制作 {S} 份，订单需要 {quantity} 份（瓶颈原料：{bottleneckMaterialName}）` |
| 无配方菜不足 | 沿用既有 `菜品【{foodName}】库存不足，当前库存: {stock}`（`:272` 文案保留） |
| 套餐不足 | `套餐【{comboName}】原料不足（组件：{componentName}），当前仅可制作 {S} 份` |

- 瓶颈原料名从 resolveStates 返回的 MIN 项取（引擎 :113-116 同逻辑）。
- 返回结构复用 `Result.error(400, msg)`（B2 先例 `:245`），不新增错误码体系。

### 7.3 并发保护（问题 5c）

**结论：预检（读）+ 出餐扣减（既有原子权威）；不做下单时原子预留。**

- 现状权威链：`deductStock` 原子 CAS（`FoodNewMapper:54`，计数器菜）；`deductMaterialsForServe`（原料账本，出餐时，P0-KDS-DEDUCT 收口）。
- 本卡预检为**读检查**：两单并发时两单都可过预检（S=1 时），第二单被接受，出餐时原料扣减由既有失败审计（`MaterialDeductionContext`）记录——**残留窗口 = 秒级双单抢最后一份**（R4）。
- 为什么不做下单时原子预留（设计排除理由）：原料真实消耗发生在**出餐**（诊断 D3 已确认扣料点），下单时原子预留 = 扣料前移 → 与出餐扣料**双扣**，需重写回滚链（取消/退款/超时三条）——超出 O4 语义（"下单时**拒绝**不足订单"），破坏 P0-KDS-DEDUCT 已收口链路。
- 计数器菜（无配方）并发安全**不变**：既有 `deductStock` CAS 仍是权威（不足 → 0 行 → 400，`:561` fail-fast 保留）。
- 残留接受度 = **待 Owner 拍板（决策 D-5）**：推荐接受（窗口小 + 出餐审计兜底 + 显示层秒级刷新）。

### 7.4 与显示层的一致性

- 预检与菜单用同一 `DishStockStateService` → 同源公式、同门店上下文；菜单显示"售罄"⇒ 预检必拒（一致方向）；菜单显示"充足"⇒ 预检可能拒（并发/延迟，§7.3 窗口）。无"菜单售罄但可下单"反向不一致（86 覆盖层上线后该方向由 86 卡保证：86 状态进同一 resolver，显示与预检同读）。

---

## 8. Q7 — 与 C 卡（POS 菜单统一）的接口

### 8.1 字段契约（C 卡切换时自动继承）

| 载体 | 新增字段 | 类型/值 |
|------|----------|---------|
| `FoodVO` | `stockStatus` | `Integer`：0=售罄 / 1=紧张 / 2=充足（@Schema 注明三态） |
| `FoodVO` | `stockStatusName` | `String`："售罄"/"紧张"/"充足" |
| `ComboVO` | `stockStatus` / `stockStatusName` | 同上（D-4 拍板后定；恒充足则固定 2） |
| `food.stock` | **不改**（数字保留，管理端 + legacy 路径消费；POS 三态页面不再显示数字） |
| 前端 `Dish`（`posApi.ts`） | `stockStatus: number` + `stockStatusName: string` | `foodVOToDish`（`posApi.ts:147-162`）透传 |

- **不删 `stock` 字段**：legacy 过渡期（C 切换前 Order/CustomerOrder 读 stock）与管理端仍需要；删除属 C 卡废弃阶段。

### 8.2 先后顺序

```
B 后端字段 + Home.vue 三态渲染（本卡）  ──先──▶  C 卡 Order.vue/CustomerOrder.vue 切 on-sale 接口（自动继承 stockStatus）
```

- 理由：B 先上时 Home 已三态、Order/CustomerOrder 仍 legacy 无 stock 字段（不显示库存，与 C 卡 R5"切换期不显示"一致）→ 无三页打架；C 切换后三页同源同三态。
- C 卡**拍板项 2**（"切换后是否显示 stock"，C 设计 §8）由本卡解决：**不显示数字，显示三态**——C 卡文档回填该裁定即可（本卡不代改 C 文档，登记）。
- C 卡验收 **A5**（下单 → foods.stock −N 且 food.stock −N）**不受影响**：计数链路不动（§3.4）。
- C 卡 R5（999 外泄）随 B 字段上线 + C 切换自然关闭（POS 三页均不再显示数字）。
- 并行安全：B 与 C 文件零重叠（B = FoodVO/FoodServiceImpl/MenuSection/useMenu/posApi 类型段 + 后端校验；C = Order.vue/CustomerOrder.vue loadMenu 段）——同文件不同段（`useMenu.ts`/`posApi.ts`），C 卡实施时以本卡字段为准。

---

## 9. Q6 — 与 86 卡（P1-MANUAL-86-001）的接口

> 86 卡诊断（`docs/quality/manual-86-prerequisites-diagnosis-001.md`）现为空骨架（§0–§9 待填），接口从 B 侧单向定义。

### 9.1 契约

| 项 | B 卡提供 | 86 卡消费 |
|----|----------|-----------|
| 状态 resolver | `DishStockStateService.resolveStates(foodIds, storeId)`——**86 覆盖是 resolver 内优先级最高分支**（§3.3），86 卡只需在 resolver 入参/内部读取 86 表（B 留 `Set<Long> manual86Ids` 入参，不建表） | 86 开关 → 调 resolver（带 manual86Ids）或直接依赖 resolver 已内建读取 |
| 状态枚举 | `stockStatus` 0/1/2 + 名称（§8.1） | 86 生效 = 强制 0；解除 = 恢复派生值 |
| 推送 | `pushMenuUpdate()` 接线点预留（§5.3-3） | 86 开关后调用同一推送 → POS ≤1 次刷新翻转 |

### 9.2 时序（问题 6）

- **86 覆盖后**：resolver 短路 → `stockStatus=0`（显示"售罄"）；**强校验同拒**（预检走同一 resolver → 400 文案"菜品【X】已被后厨估清"——文案 86 卡定，B 预留 resolver 返回的 `source` 字段：`derived` / `manual86`）；派生值仍后台计算（供 86 解除时即时恢复，无需重算等待）。
- **86 恢复后**：resolver 下次调用即返回派生三态——**生效时机 = 推送触发的下一次菜单刷新（秒级）或下一次下单预检（即时）**；无缓存残留（§5.1 无缓存）。
- **依赖方向**：86 卡以"B 设计完成"为前置（86 卡 Q1 待填项的答案方向：resolver 先行则 86 可并行开发，接线在 B 实施后）——本卡给出"B 实施完成后 86 接线"的最低依赖，86 卡可先做 UI/表设计。

---

## 10. 改动文件清单（阶段 3 预估，本卡不实施）

**后端**：

| # | 文件 | 改动 |
|---|------|------|
| 1 | `service/DishStockStateService.java` | **新增**（接口：resolveStates / checkOrderSufficiency） |
| 2 | `service/impl/DishStockStateServiceImpl.java` | **新增**（批量派生 + 三态映射 + 瓶颈原料 + 阈值读取） |
| 3 | `mapper/DishStockStateMapper.java` | **新增**（1 条批量 JOIN @Select：dish_recipes × store_inventory） |
| 4 | `dto/product/FoodVO.java` | + `stockStatus` / `stockStatusName` |
| 5 | `dto/product/ComboVO.java` | + `stockStatus` / `stockStatusName`（D-4 后定） |
| 6 | `service/impl/FoodServiceImpl.java` | `listOnSale`（:721 附近）填充三态（批量一次调用，无 N+1） |
| 7 | `service/impl/ComboServiceImpl.java`（文件名阶段 3 核实） | on-sale 填充套餐三态 |
| 8 | `service/impl/PosOrderCreateServiceImpl.java` | canonical Step 3 预检统一替换（:262-275）+ legacy 路径预检新增（:757 前）+ 文案 |
| 9 | `service/impl/OrderNewServiceImpl.java` | `deductMaterialsForServe`（:685）成功/失败收尾后调 `pushMenuUpdate` |
| 10 | `db/migration/V...__seed_pos_stock_settings.sql` | `sys_settings` seed：`pos.dish.stock.tightThreshold`=10（global） |

**前端（仅 Home 侧，Order/CustomerOrder 归 C 卡）**：

| # | 文件 | 改动 |
|---|------|------|
| 11 | `frontend-pos/src/api/posApi.ts` | `FoodVO`/`Dish` 类型 + `foodVOToDish` 透传 `stockStatus/stockStatusName` |
| 12 | `frontend-pos/src/composables/useMenu.ts` | 三态接入；`lowStockItems`（:62-64）改语义为"紧张集合"（stockStatus=1） |
| 13 | `frontend-pos/src/components/pos/MenuSection.vue` | 售罄 overlay（`item.stock===0` → `stockStatus===0`）；badge 改三态标签**去数字**（:50）；低库存样式（:181）对齐 |

**测试 + 记录**：

| # | 文件 | 说明 |
|---|------|------|
| 14 | `backend/src/test/java/.../DishStockStateServiceTest.java` | 单测：最短板 MIN / 无配方回退 / 阈值边界（S=N-1 vs N）/ 套餐组件派生 / 无库存行=0 |
| 15 | `backend/src/test/java/com/foodtraceability/integration/DishStockStateIntegrationTest.java` | 集成：on-sale 返回 stockStatus 精确值；下单预检 400 拒绝（不足）+ 200 通过（充足）；并发窗口登记为已知残留（不断言） |
| 16 | `docs/architecture/03-review/p1-foods-stock-semantics-001-implementation-record-001.md` | 实施记录 |

**禁止项（阶段 3 边界）**：不改 `StockForecastServiceImpl`/`StockForecastMapper`（§8-6 失真不顺手修）；不改 `deductMaterialsForServe` 扣减逻辑（只加推送调用）；不改 legacy 双扣/自愈（归 C 卡 P1）；不建 86 表；不动 `dish_inventory` 死路径。

---

## 11. 风险清单

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| R1 | **语义切换**：有配方菜不再被 foods.stock 门禁 → 菜 8（stock=0，原料 50 份）B 后恢复可点；收银员感知"以前售罄的菜能点了" | 中 | Owner 拍板 D-2 时明确知晓此例；实施记录写行为对照表 |
| R2 | **门店上下文误配**：原料双门店分布（生菜 store 1/2，177.1/40.0）；POS 会话 store 解析错误 → 三态/校验用错原料池 | 中 | 实施阶段第一步验证 POS 会话 store + store_inventory 门店分布；resolver 入参显式 storeId |
| R3 | **无库存行原料 → 整菜售罄**（严格规则）：现网零冲击（已核），未来录原料忘建库存行即隐性售罄 | 中 | D-2 拍板项明示此副作用；管理端低库存看板已有（FoodManagement:153）可发现 |
| R4 | **并发窗口**：秒级双单过预检、出餐时原料不足（依赖既有失败审计） | 低 | D-5 拍板接受；显示层秒级推送刷新收敛 |
| R5 | legacy 999 / 数字 stock 在过渡期仍在 Order.vue 上游（C 切换前）：两页无 stock 字段 → 无显示冲突，但"临时规则"实际只在 Home.vue 生效 | 低 | §6.1 已澄清；C 切换后归一 |
| R6 | `useMenu.ts`/`posApi.ts` 与 C 卡同文件不同段 → 并行 WIP 重叠风险 | 中 | B 先 C 后（§8.2）；C 卡实施时 PG-001 门禁对 `useMenu.ts` 做 WIP 重叠核对 |
| R7 | 引擎 `getSoldServings` cast 失真（§8-6）若被误用到菜单路径 → S 低估误售罄 | 低 | §4.2 菜单路径只用理论用量，结构性排除；登记不修 |
| R8 | `pushMenuUpdate` 广播全 topic：出餐扣料高频时 POS 全量刷新（8 菜规模无压力） | 低 | 规模预案（§5.1）登记；必要时 86 卡后加节流 |
| R9 | 套餐派生依赖组件 foods 行存在（combo_ingredients → foods id 悬空） | 低 | 组件缺失 → 该组件 S=0 → 套餐售罄（保守方向）；阶段 3 断言 |
| R10 | `sys_settings` seed 与既有设置管理页面冲突（管理端若已有 settings CRUD 可能覆盖） | 低 | 阶段 3 核实 settings 管理页是否暴露该 key；不暴露则 Owner 改库 |

---

## 12. Owner 拍板/确认清单

**拍板项（5 项，裁决后进阶段 3）**：

- **D-1 紧张阈值 N**：**推荐 10**（§3.2 三条理由）；`sys_settings` 可配（门店级覆盖）。**未擅自定死**——Owner 可改值或要求不可配。
- **D-2 无 store_inventory 行原料规则**：**推荐严格（available=0 → 售罄）**（§3.1）；副作用见 R1/R3（含菜 8 恢复可点的语义切换确认）。
- **D-3 菜单路径每份用量口径**：**推荐理论用量（required_quantity）**，自校准留管理端（§4.2）；备选 = 菜单也用自校准（接受窗口聚合进菜单请求 + §8-6 失真风险）。
- **D-4 套餐三态**：**推荐组件派生**（§3.1）；备选 = 恒"充足"。
- **D-5 并发残留**：**推荐接受**"预检 + 出餐权威扣减"（§7.3），不做下单时原子预留。

**确认项（2 项，设计已给结论，Owner 确认即可）**：

- **C-1 过渡规则 = 现状零改动**（§6.1）：B 实施前不动任何前端/后端。
- **C-2 与 C 卡顺序 = B 字段先、C 切换后**（§8.2）；C 卡拍板项 2（是否显示 stock）按"不显示数字、显示三态"回填。

---

## 13. 登记清单（本设计发现、不顺手修）

1. `pushMenuUpdate()` 全库 0 调用者（`OrderWebSocketController:115`）——推送通道在、生产者休眠；本卡接线后此登记关闭。
2. 活库菜 8（`FD260926006` 系）`foods.stock=0` 但派生 S=50 → B 后状态翻转的活体例证（R1 载体）。
3. 原料 3（生菜）双门店库存行（store 1: 177.1 / store 2: 40.0）→ 门店上下文敏感性活体证据（R2 载体）。
4. 引擎 `getSoldServings` cast 失真（§8-6 沿袭）——菜单路径结构性隔离（§4.2），修复另裁。
5. legacy 路径（`PosOrderCreateServiceImpl:757-772`）**无预检**直接扣——本卡补预检后此缺口关闭。
6. `PosComboDTO`/`ComboVO` 均无 stockStatus 字段且套餐无 foods 库存对象（诊断 §8-7）——D-4 拍板前套餐三态无数据源。
7. `sys_settings` 无 POS 域设置 seed 先例（本卡 V-迁移为首个）——R10。
8. 86 卡诊断骨架未填（`manual-86-prerequisites-diagnosis-001.md` §0–§9 待填）——本卡 §9 从 B 侧给出接口定义，86 卡回填时引用。

---

## 交接条件

- [x] 设计文档落盘（本文件）
- [ ] Owner 审过 D-1~D-5 + C-1~C-2
- [ ] 方可进阶段 3（实施）——按 §10 改动文件清单建实施卡
