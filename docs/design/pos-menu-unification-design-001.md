# P1-POS-MENU-UNIFICATION-001 — POS 菜单统一到新表（阶段 2：设计）

- **日期**：2026-09-26
- **阶段**：阶段 2（设计，只出方案）— 未改代码 / 未改 DB；**未擅自裁定切换顺序/灰度比例**，方案项均标"待 Owner 拍板"
- **Owner 已裁定方向**：砍掉 legacy 菜单入口（Order.vue / CustomerOrder.vue 的 `/v1/pos/api/menu` 路径）；全部统一到 `foods` / `food_categories`；与 A2 决策一致（不做竞争性 namespace）
- **设计输入**：`docs/quality/pos-menu-inventory-category-diagnostics-001.md`（双轨三页 + 3 处分类断点）；F6 双写现状（`FoodServiceImpl.syncLegacyFood` L133-179）
- **证据规范**：结论均附 file:line；依赖生产数据的结论标 PROVISIONAL

---

## 1. 范围

**入范围**：
1. `frontend-pos/src/views/Order.vue`（/order，收银下单页）菜单数据源切换
2. `frontend-pos/src/views/CustomerOrder.vue`（/customer/order，顾客扫码点餐页，免登录）菜单数据源切换
3. legacy 分类表 `food_category` 的逻辑停用裁定
4. legacy 菜单接口 `/v1/pos/api/*` 的残留调用方核查 + 下线节奏
5. `syncLegacyFood` 废弃前置条件与节奏（废弃动作本身留待后续实施卡）

**出范围**：
- Home.vue（主 POS 屏）——已在用新表三接口（`useMenu.ts` L71-82），零改动
- 订单创建/支付/退款路径的 legacy 双扣（属"废弃 syncLegacyFood"前置，单独卡）
- legacy 表物理 DROP（属废弃阶段，单独迁移卡）
- `foods.stock` 语义重建（P1-FOODS-STOCK-SEMANTICS-001 并行卡）

## 2. 残留调用方核查（本卡完成，证据基线）

### 2.1 `/v1/pos/api/*` 接口族

| 接口 | 前端调用方 | 后端调用方 |
|---|---|---|
| `GET /v1/pos/api/menu` | **2 处**：`Order.vue` L336（`posApi.getFullMenu`）、`CustomerOrder.vue` L342（裸 request） | `PosApiController.getFullMenu` L65-66 → `PosApiServiceImpl.getFullMenu` L175-181 |
| `GET /v1/pos/api/categories` | **0 处**（仅 `posApi.ts` L399-400 定义） | 同链 |
| `GET /v1/pos/api/dishes` | **0 处**（仅 L403-404 定义） | 同链 |
| `GET /v1/pos/api/dishes/category/{id}` | **0 处**（仅 L407-408 定义） | 同链 |
| `GET /v1/pos/api/combos` | **0 处**（仅 L411-412 定义） | 同链 |

- 管理端 `frontend/`（*.vue）对 `/v1/pos/api` **0 调用**。
- `PosApiService` 后端唯一调用方 = `PosApiController`（无其他 service 注入）。
- 即：**整条 legacy 菜单接口族只有 2 个活调用方，其余 4 个接口已是死接口**。

### 2.2 legacy `food` 表读方（`syncLegacyFood` 废弃的依赖全景）

| # | 读方 | 位置 | 依赖性质 |
|---|---|---|---|
| 1 | `PosApiServiceImpl`（菜单） | L76 / L101 | **本卡切换对象** |
| 2 | `PosOrderCreateServiceImpl`（下单硬扣） | L554-558 / L758-762（`foodMapper.deductStock` 400 fail-fast）+ L1085-1109（`ensureLegacyFoodRow` 自愈补行，**stock 按 foods 当前值复制** L1103） | 订单路径 |
| 3 | `PosOrderPaymentServiceImpl`（退款回滚） | L502 `foodMapper.addStock` | 订单路径 |
| 4 | `FoodServiceImpl`（双写源） | L140-174 `syncLegacyFood`（try-catch 吞异常仅 warn L176-178） | 产品中心 CRUD 触发 |
| 5 | `PreMakeServiceImpl`（提前制作） | L58 `foodMapper.selectById` → 取 code/name/price 生成追溯码；**无自愈** | 非订单路径，依赖 legacy 行存在 |
| 6 | `ProductSalesStatsServiceImpl`（销售统计） | L54 / L92 / L120（列表/TopN/分类聚合，当前多为 0 值占位实现） | 管理端统计 |
| 7 | `KitchenScanServiceImpl` | L35 注入（用法未核实） | **登记** |
| 8 | `OrderMaterialRequirementServiceImpl` | L21 注入（用法未核实） | **登记** |

### 2.3 legacy `food_category` 表

- 读方：仅 `PosApiServiceImpl` L60 / L80 / L105（全部随 legacy 菜单接口退役）。
- 写方：**0 个活写方**——`CategoryOperationLogService.logCreate/logUpdate/logDelete`（唯一接 `FoodCategory` 实体的写日志通道）全代码库 **0 调用者**（grep `.logCreate|.logUpdate|.logDelete` = no match）。
- 活库：0 行（诊断 3 实测）。
- **结论：`food_category` 表已是"只读且无人写"的死表**，唯一读方随本卡切换而消失。

### 2.4 认证对等性

- `SecurityConfig` L123：`/v1/pos/**` → `authenticated()`（仅 `/v1/pos/auth/**` 公开）。
- `/v1/product-center/**` 未列 permitAll → 落 `anyRequest().authenticated()`（L170）。
- **新旧菜单接口认证要求完全对等**（都要 JWT）。`CustomerOrder.vue` 是免登录页（router WHITE_LIST L92）却调需 JWT 的接口 → 401 风险是**既有状态**（菜单加载 catch 吞错 → 空菜单），切换不新增也不修复该风险。PROVISIONAL：若部署环境 `app.security.enabled=false`（L35-36/L82-83 全公开），则无此问题。

## 3. 切换方案

### 3.1 目标数据源

Order.vue / CustomerOrder.vue → **与 Home.vue 完全相同的三个产品中心接口**：

| 接口 | 返回 | 现有转换器（已存在、已在 Home 生产使用） |
|---|---|---|
| `GET /v1/product-center/foods/on-sale` | `FoodVO[]`（含 stock/categoryName/salePrice 分） | `posApi.ts` `foodVOToDish` L147-162 |
| `GET /v1/product-center/combos/on-sale` | `ComboVO[]` | `comboVOToCombo` L164+ |
| `GET /v1/product-center/categories/tree/enabled` | `CategoryVO[]`（树） | `useMenu.ts` L587+ 已有拉取模式 |

### 3.2 实现选项（**待 Owner 拍板**）

**方案 A（推荐）：前端直连现有三接口，后端零改动**
- 改动面：`Order.vue` loadMenu（L333-350）+ `CustomerOrder.vue` loadMenu（L340-366）改为调用三接口 + 复用既有转换器；可选抽取共享 composable（`usePosMenu`）。
- 优点：零新后端代码、接口已被 Home.vue 生产验证、与 A2"单一真相源"最大化一致、回滚纯前端。
- 缺点：两页各 3 个请求（可并行，Home.vue 已同样模式）；前端 shape 适配工作量集中在此。

**方案 B：后端新增统一菜单端点（读新表，返回 `Menu{categories,dishes,combos}` 原形状）**
- 前端只换 URL，diff 最小。
- 缺点：新增后端端点 + 维护面（与三接口重复取数）；虽命名空间仍属 pos（非 A2 意义的竞争性 namespace），但多一条并行数据路径。

**推荐 A**：本卡的核心收益是"消灭第二真相源"，A 让接口与数据源同时归一，无新增维护面。

### 3.3 数据形状适配（方案 A，阶段 3 实施清单）

| 字段 | legacy（`PosDishDTO`） | 新（`foodVOToDish` 输出） | 适配动作 |
|---|---|---|---|
| `dishId` | `foodCode`（字符串码） | `String(foodId)`（数字串） | **R1 关键风险**：核查下单 payload 用的是 `dishCode` 还是 `dishId`（`useMenu.ts` L32 注释：后端按 food_code 查询扣减 → 应走 `dishCode`，`foodVOToDish` L150 已正确填充）。阶段 3 第一步验证 |
| `dishCode` | `foodCode` | `foodCode` | ✓ 一致（订单路径身份键，不变） |
| `categoryId` | 分类 **code**（字符串） | `String(categoryId)`（数字串） | Order.vue `el-radio :label="cat.categoryId"`（L31）+ `activeCategory` 比较均按字符串 → 兼容；`'combo'` 虚拟分类注入（L341-343）逻辑保留 |
| `categoryName` | categoryMap 按 code 查，查不到兜底"其他"（`PosApiServiceImpl` L89） | 直接来自 `FoodVO.categoryName` | 兜底逻辑随 legacy 消失（新路径 categoryName 后端已拼好） |
| `price` | 元（BigDecimal） | 分 → `fenToYuanNumber` | ✓ 转换器已处理 |
| `stock` | **无字段** | 有（999 兜底 L160） | 两页新增 stock 数据；**切换期建议 UI 不显示**（避免 999 语义外泄，R5）——待 Owner 拍板 |
| `items`（套餐） | `PosComboItemDTO`（foodId=foodCode 字符串） | `ComboItem`（foodId 数字串） | Order.vue L90 `:key="i.foodId"` 仅展示用 → 兼容（R9 登记核实） |
| `peopleCount` | 固定 2（`PosApiServiceImpl` L142） | `ComboVO` 无此字段 | Order.vue 模板未见使用（R4 登记，阶段 3 核实） |
| CustomerOrder 本地 `Dish` 类型 | `id/name/price/image/categoryId/dishType`（L347-358 手工映射） | 同上适配 | loadMenu 映射段改写 |

## 4. 八问逐项

**Q1 切换顺序**：**先动前端，后端不动**（方案 A 下三接口已存在且生产在用）。若 Owner 选方案 B，则后端先行。

**Q2 灰度方案（设计推荐 + Owner 确认，见 §8-5）**：
- POS 客户端 = 每门店部署的 SPA（无原生壳、无客户端版本残留 → 新 build 覆盖旧 build）。
- 本地单门店（8 菜品/1 门店）→ "单门店灰度"本地近似全量；真实灰度价值只在多门店生产环境（R10）。
- **设计推荐 = 按页切换**：CustomerOrder（公开扫码页、流量低、无下单资金路径耦合）先切 → 观察 1-3 个营业日 → Order.vue（收银主路径）；不做 feature flag（备选：多门店时 `VITE_POS_MENU_SOURCE=unified|legacy` build-time 逐店发版）。
- 观察指标：菜单加载成功率（500/401 计数）、产品中心新建菜品 ≤1 次刷新在三页可见（A3）、下单成功率、`/v1/pos/api/menu` 命中数（切换后应归零，A9）、价格展示与 `foods.salePrice` 一致（A10）。

**Q3 双写窗口期**：**两套接口并存**（product-center 三接口本就在生产；legacy `/v1/pos/api/menu` 切流后不下线，留观察窗口）。数据面 `syncLegacyFood` 持续运行（订单路径仍双扣）→ 期间 `food`/`foods` 双表一致性由现有双写维持，**窗口期两套接口都可用**，非单点切换。窗口结束点 = legacy 接口下线（Q6）。

**Q4 `syncLegacyFood` 废弃节奏**：**切换阶段不废弃**（它仍护订单路径 + PreMake + 统计的 legacy 行存在性）。废弃前置条件（全部满足才可删，动作属后续实施卡）：
- **P1** 订单路径不再以 legacy 硬扣为准（切到 foods 硬扣，**或** Owner 接受"自愈补行 + 双扣"为唯一同步机制——二选一，需单独卡裁定；注意 L1103 自愈 stock 按 foods 当前值复制，新建菜品首单后两表即对齐，此后订单/退款双扣维持同步，产品中心改名/改价不再同步 legacy——若 legacy 只余扣减用途则无害，需裁定）
- **P2** `PreMakeServiceImpl` L58 迁到 `foods`（或补自愈）——当前唯一"依赖 legacy 行存在且无自愈"的非订单读方
- **P3** `ProductSalesStatsServiceImpl` L54/92/120 迁到 `foods`（或裁定接受 legacy 快照口径）
- **P4** 核实 #7/#8 注入用法（KitchenScan / OrderMaterialRequirement）
- **P5** legacy 接口已下线 + 前端 100% 切新（本卡完成）
- **P6** 一次性对账：`food` vs `foods` 行数 / stock 一致性（当前 8/8 行逐行相等，基线已知）
- 满足 P1-P6 后删 `syncLegacyFood`（`FoodServiceImpl` L116-123 调用点 + L133-179 方法体），`food` 表进入"冻结只读"，DROP 表 = 再单独迁移卡。

**Q5 分类统一**：`food_categories` = 唯一真相源（自切换日起三页同源）。`food_category`（legacy）**直接逻辑停用**（0 行/0 活写方/唯一读方随本卡消失，见 §2.3）；**本卡不 DROP 表**（无 DB 改动原则），物理删除留废弃阶段迁移卡。`food.food_category` 列（存名字，诊断 3 断点 2）在 `syncLegacyFood` 窗口期继续被写（F6），随 P1-P6 一起废弃。

**Q6 legacy 接口下线节奏（保留时长 = 设计推荐 1 个发布周期，Owner 确认，见 §8-6）**：
- 切流后保留 `/v1/pos/api/*`（5 个接口）至少 1-2 个发布周期。
- 残留调用方验证三件套：① 代码库 grep（本核查 = 2 处，§2.1）② 观察窗口内访问日志 `/v1/pos/api/*` 命中 = 0（A9）③ POS SPA 发版覆盖确认（顾客扫码页浏览器缓存风险低，PROVISIONAL）。
- 下线方式：先发版 410 Gone（保留一个周期暴露残留调用）→ 下周期删 `PosApiController` 菜单端点 + `PosApiServiceImpl` + DTO（若 `getAllCombos` L125-171 无其他调用方可同删；`PosApiService` 其余方法先 grep 确认）。

**Q7 回滚方案**：
- 切换阶段 = **纯前端改动 + 零 DB 迁移 + 零后端改动**（方案 A）→ 回滚 = 重新部署上一版本前端 build（或 feature flag 翻回 legacy）。
- 数据无需回滚：窗口期双写维持两表一致，legacy 侧数据始终有效。
- 不可逆动作（DROP 表 / 删 legacy 接口 / 删 syncLegacyFood）全部推迟到废弃阶段独立执行，切换期不碰 → **回滚面 = 一个前端 build**。

**Q8 验收基准（切换后必须通过的断言）**：
- **A1** Order.vue 菜单列表 == `/v1/product-center/foods/on-sale` 返回（名称/价格/分类逐条一致）；分类页签 == `food_categories` enabled 集合
- **A2** CustomerOrder.vue 同 A1
- **A3** 产品中心新建菜品 → Home / Order / CustomerOrder 三页 ≤1 次刷新全部可见（F6 症状回归）
- **A4** 产品中心新建分类 → Order / CustomerOrder 页签出现（修复诊断 3 三断点：0 行死表 / code≠name / 双表不同步）
- **A5** 自 Order.vue / CustomerOrder.vue 下单 → `foods.stock` −N **且** `food.stock` −N（双写窗口内两表一致）
- **A6** 退款 → 两表回补一致
- **A7** 切换后无新增 500；CustomerOrder 401 水平不劣化（认证对等，§2.4）
- **A8** Home.vue 行为零 diff（回归）
- **A9** 观察窗口内 `/v1/pos/api/menu` 访问日志命中 = 0
- **A10** 两页价格展示与 `foods.salePrice`（分→元）一致

## 5. 改动文件清单（阶段 3 预估）

**切换阶段（本卡实施范围）**：
| 文件 | 改动 |
|---|---|
| `frontend-pos/src/views/Order.vue` | loadMenu L333-350 → 三接口 + 转换器；L341-343 combo 虚拟分类逻辑保留；item 映射段（L280-304）核对 `dishCode` 透传 |
| `frontend-pos/src/views/CustomerOrder.vue` | loadMenu L340-366 → 同上；本地 Dish 映射段 L347-358 改写 |
| `frontend-pos/src/api/posApi.ts` | 补 on-sale 三接口调用方法（若 Home 走 useMenu 内联则抽到 posApi 复用）；`getFullMenu/getCategories/...` 窗口期保留 |
| （可选）`frontend-pos/src/composables/` | `usePosMenu` 共享（避免两页复制拉取逻辑） |
| 后端 | **无**（方案 A） |

**废弃阶段（后续卡，列出供 P1-P6 规划）**：
`PosOrderCreateServiceImpl`（硬扣切换）/ `PosOrderPaymentServiceImpl` L502 / `FoodServiceImpl`（syncLegacyFood）/ `PreMakeServiceImpl` L58 / `ProductSalesStatsServiceImpl` L54-129 / `PosApiServiceImpl` + `PosApiService` + `PosApiController` + `PosDishDTO`/`PosCategoryDTO`/`PosComboDTO`/`PosComboItemDTO`/`PosMenuDTO` / （V-migration：DROP `food_category`，`food` 表去留另裁）

## 6. 风险清单

| # | 风险 | 等级 | 缓解 |
|---|---|---|---|
| R1 | `dishId` 语义翻转（foodCode → String(foodId)）：若下单 payload 误用 `dishId` 会下错单 | 高 | 阶段 3 第一步：验证订单 payload 身份键 = `dishCode`（`useMenu.ts` L32 注释支持此假设）+ e2e 下单断言 A5 |
| R2 | CustomerOrder 免登录页 × JWT（SecurityConfig L123）：顾客页菜单可能当前就是空（401 被 catch 吞）；切换不新增也不修复 | 中（PROVISIONAL） | 切换前实测顾客页当前菜单状态并记录基线；若为空，向 Owner 报告"切换 ≠ 修复"，修复另裁（如 `/v1/pos/api/menu` 公开化或顾客 token 机制） |
| R3 | 分类 id 形状变化（code 字符串 → 数字串）：`activeCategory`/radio label 全按字符串比较 → 兼容，但"其他"兜底语义消失 | 低 | A1/A4 覆盖 |
| R4 | `peopleCount` legacy 固定 2 vs 新路径无此字段：模板未见使用 | 低 | 阶段 3 grep 核实，使用则补默认值 |
| R5 | 新路径 stock 999 兜底（`foodVOToDish` L160）：两页若显示 stock，"无限"语义外泄给用户 | 中 | 切换期 UI 不显示 stock（待 Owner 拍板）；根治归 P1-FOODS-STOCK-SEMANTICS-001 |
| R6 | 窗口期三页数据源不一致（Home 实时新表 vs 切换前 Order/CustomerOrder 旧表） | 低 | 已登记（诊断 3 现状即如此），切换收敛 |
| R7 | `syncLegacyFood` 吞异常仅 warn（L176-178）：窗口期 legacy 表漂移无告警 | 低 | 已知观察项 1；P6 对账兜底 |
| R8 | 部署环境 `app.security.enabled=false` 时认证对等假设变化 | 低（PROVISIONAL） | 实施前核实目标环境配置 |
| R9 | 套餐 `items.foodId` 字符串形状变化（foodCode → 数字串）：`:key` 仅展示用 | 低 | 阶段 3 核实无下游消费 |
| R10 | 本地单门店 → "单门店灰度"退化为全量；真实灰度只存在于多门店生产 | 信息 | 若生产亦单门店，灰度按"按页 + 时间窗"执行（Q2 候选 1） |

## 7. 执行模型（阶段 3）

### 7.1 实施阶段划分（供阶段 3 建卡参考，本卡不建卡）

| 阶段 | 内容 | 独立回滚面 |
|---|---|---|
| 3a 切换 | 前端两页切三接口（+ 可选 flag）；**第一动作 = §7.3 R1 验证** | 前端 build |
| 3b 观察窗口 | 1 个发布周期，跑 A1-A10 + 访问日志归零（时长 = §8-5 设计推荐，待 Owner 确认） | — |
| 3c 接口下线 | legacy `/v1/pos/api/*` 410 → 删代码 | 后端发版 |
| 3d 废弃（另卡） | P1-P6 → 删 `syncLegacyFood` + DROP `food_category`（+ `food` 表裁定） | 后端发版 + 迁移 |

### 7.2 接口字段 gap 对比表（legacy vs 新，3a 切换与 A1/A2/A10 验收参照）

**端点级**：

| 项 | legacy（Order/CustomerOrder 现状） | 新（Home 现状 = 切换目标） | gap / 处置 |
|---|---|---|---|
| 端点 | `GET /v1/pos/api/menu`（单请求） | `GET /v1/product-center/foods/on-sale` + `/combos/on-sale` + `/categories/tree/enabled`（3 并行请求） | 请求数 1→3（并行；Home.vue 已生产运行此模式） |
| 认证 | JWT（`SecurityConfig` L123） | JWT（L170 落 anyRequest） | 对等（§2.4）；CustomerOrder 免登录页 401 为既有风险（R2） |
| 数据源 | legacy `food` 表（`food_status ∈ {'1','active'}`）+ legacy `food_category`（0 行） | `foods`（on-sale 口径）+ `food_categories`（enabled） | 真相源归一（本卡目标） |
| 分类形状 | `PosCategoryDTO{categoryId = categoryCode（字符串 code）, categoryName, sortOrder}` | `CategoryVO{categoryId: number, categoryName, parentId, children[], sortOrder, foodCount}` | id 语义 = **code 字符串 → 数字 id 字符串**（`String()` 后均为字符串，`el-radio :label`/`activeCategory` 比较兼容；§3.3） |
| 套餐形状 | `PosComboDTO{price: 元, peopleCount = 硬编码 2（L142）, items[].foodId = foodCode 字符串}` | `ComboVO → Combo{price: 元, 无 peopleCount, items[].foodId = 数字串}` | peopleCount 缺失（R4）；items.foodId 字符串语义（R9）——均 3a 核实 |

**字段级（菜品）**：

| 字段 | legacy `PosDishDTO` 值 | 新 `foodVOToDish` 输出 | gap / 动作 |
|---|---|---|---|
| `dishId` | `foodCode`（码） | `String(foodId)`（数字串） | **R1（最高风险）**：订单 payload 身份键须 = `dishCode`；验证 = §7.3 |
| `dishCode` | `foodCode` | `vo.foodCode`（L150） | ✓ 一致（订单身份键，不变） |
| `categoryId` | 分类**名字**（L88 直存 `food.food_category`） | `String(categoryId)` 数字串 | radio 字符串比较兼容；"其他"兜底语义消失（R3） |
| `categoryName` | code 表查 + "其他"兜底（L89） | 后端拼好直出 | ✓ |
| `price` | 元（BigDecimal） | 分 → `fenToYuanNumber` | ✓ 转换已处理（A10 验收） |
| `stock` | **无字段** | 真实值（999 兜底 L160） | 新增数据；切换期 UI 建议不显示（R5，§8-3 待拍板） |
| `description`/`imageUrl` | ✓ 同 | ✓ 同 | — |
| 套餐 `items[].foodId` | `foodCode` 字符串 | 数字串 | 展示用 `:key`（R9，3a 核实无下游） |
| 套餐 `peopleCount` | 硬编码 2 | 无字段 | 3a 核实模板是否消费（R4） |

### 7.3 R1 缓解：具体验证步骤（3a 第一动作，全过方可切流发布）

1. **前端 payload 静态审计**：读 `Order.vue` goToPayment（L322-331，`items` JSON 经 query 传 `/payment`）→ `Payment.vue` L662 / `usePayment.ts` L101 `posApi.createOrder(orderData)` 的构造路径。断言：`orderData.items[*]` 身份字段 = `foodCode`/`dishCode`（**非** `dishId`），且其值源自 `foodVOToDish` L150（`vo.foodCode`）
2. **后端消费静态审计**：读 `PosOrderCreateServiceImpl` canonical 路径——订单项携 `food_code`（字符串；`useMenu.ts` L32 注释："后端 PosOrderCreateServiceImpl.createOrder 通过 food_code 查询 food 表并扣减库存"），映射数字 id 落库（P1-POS-FOODID-MAP-001 B2 既有机制）；断言扣减键 = `food_code`（`FoodNewMapper.deductStock` L54 `WHERE food_code = ?`），不消费 `dishId`
3. **e2e 正例（活体）**：Order.vue 选已知菜品（如 `FD202608010001`，记切换前 `foods.stock` = S0）下 N 份 → 断言：① `order_items` 行 `food_id` = 该菜数字 id（非 foodCode 字符串、非其他菜）② `foods.stock` = S0−N ③ `food.stock` 同步 −N（双写窗口）④ KDS 收到正确菜名
4. **e2e 交叉例（活体）**：CustomerOrder.vue 对**另一分类**菜品下单 → 断言 `order_items.food_id` 正确（防分类 id 语义翻转串菜）
5. **套餐路径（活体）**：Order.vue 下含套餐单 → 断言组件扣料 `combo_ingredients` 命中正确 food_id（与 P1-COMBO-ORDER-001 REG-ORDER-012 基线 126 项一致）
6. **门禁**：1-5 任一不过 → 不切流（回滚面 = 前端 build，§7.1）

## 8. Owner 拍板/确认清单

**拍板项（4 项，Owner 裁决后进入阶段 3）**：

1. 方案 A（推荐，后端零改动）vs 方案 B（新增统一端点）
2. 切换后 Order/CustomerOrder 是否显示 stock（R5：建议不显示）
3. CustomerOrder 顾客页 401 基线核实结果（R2）——是否随本卡一并修复
4. Q4-P1 二选一：订单路径切 foods 硬扣 vs 接受自愈+双扣机制（属 3d 卡，但需现在定方向）

**确认项（2 项，设计已给推荐，Owner 确认即可）**：

5. 灰度粒度 + 观察窗口（**设计推荐**）：**按页切换**——CustomerOrder（公开扫码页、流量低、无下单资金路径耦合）先切 → 观察 **1-3 个营业日** → Order.vue；不做 feature flag（单门店 SPA，build 级切换已足够细；若 Owner 知生产为多门店，改为 `VITE_POS_MENU_SOURCE=unified|legacy` build-time 逐店发版）；3b 观察窗口 = **1 个发布周期**，准出 = A9 访问日志归零。（依据：R10——单门店假设下门店级灰度无意义）
6. legacy 接口下线保留时长（**设计推荐**）：**保留 1 个发布周期**（不取 2 周期——5 个 legacy 接口中 4 个已是死接口，唯一活路径即本卡切换的 2 处，残留概率低）；准出 = A9（观察窗口 `/v1/pos/api/*` 命中 = 0）+ SPA 发版覆盖确认；下线方式 = 410 Gone（1 周期）→ 删代码（下周期）

## 9. 阶段 1 登记清单（本卡发现、不顺手修）

1. `/v1/pos/api/categories`、`/dishes`、`/dishes/category/{id}`、`/combos` 4 个死接口（前端 0 调用）
2. `CategoryOperationLogService.logCreate/logUpdate/logDelete` 0 调用者 → legacy `food_category` 表 0 活写方
3. `KitchenScanServiceImpl` L35 / `OrderMaterialRequirementServiceImpl` L21 的 `FoodMapper` 注入用法未核实（P4 前置）
4. `CustomerOrder.vue` 免登录页调需 JWT 接口（SecurityConfig L123）——401 被 catch 吞，菜单可能长期为空（PROVISIONAL，R2）
5. `PosApiServiceImpl.getAllCombos` L142 `peopleCount` 硬编码 2（dish_combos 无该列）
6. `ProductSalesStatsServiceImpl` 统计为 0 值占位实现（读 legacy `food` 表，订单模块未完成注释 L91）
