# P1-FOODS-STOCK-SEMANTICS-001 — foods.stock 语义诊断（阶段 1，只读）

> 日期：2026-09-26 · 方法：代码 file:line 实证 + 活库快照（food_traceability，2026-09-26 18:5x）+ 既有审计文档引用
> 阶段 1 纪律：只读，不改代码/DB/不建卡/不顺手修；"应该怎么修"留待阶段 2

## D1 — foods.stock 当前语义

| 证据 | 内容 |
|------|------|
| DDL `V6.0.0__create_order_product_tables.sql:21` | `stock INTEGER DEFAULT 0, -- 库存数量（可选，用于限量菜品）` —— 字段自述语义 = **可选限量计数器**，非库存对象 |
| 实体/VO | `FoodNew.stock`（Integer）→ `FoodServiceImpl.java:721` `vo.setStock(food.getStock())` 原样透传至 `FoodVO.stock`（POS on-sale 返回） |
| 既有审计 `business-item-canonical-conflict-boundary-review-001.md:250` | "列注释为『库存数量（可选，用于限量菜品）』——**语义是限量而非库存**；**无任何台账记录 foods.stock 变动**"；A7 待决："foods.stock 是否计入『库存』范畴（限量标记 vs 库存）" |
| 活库快照（2026-09-26） | foods 8 行 stock = 73/99/49/20/36/40/29/0；legacy `food` 表 8 行 stock **逐行相同**（双写维持） |

**D1 结论**：当前语义 = **人工菜品份数计数器（Count Down List）**。由"下单扣 / 退款回滚"双写维持（D3），与原料库存无派生关系，无动态计算。与 Owner 目标语义（可售份数 = 配方 + 原料库存动态算出）的差距见 D6。

## D2 — 创建链路：初始值从哪来

| 环节 | 证据 | 结论 |
|------|------|------|
| 后端 create | `FoodServiceImpl.java:100` `food.setStock(dto.getStock())`；`FoodCreateDTO.java:43-44` `private Integer stock`（无 @NotNull） | **用户输入可选**，无后端默认赋值 |
| null 时的落库 | 全 profile `field-strategy: NOT_EMPTY`（application.yml:72 等 5 处）→ stock=null 时列不入 INSERT → **DB 默认 0**（foods DDL） | UI 创建 → foods.stock = **0** |
| legacy 双写 | `FoodServiceImpl.java:155` `legacy.setStock(food.getStock() != null ? food.getStock() : 0)` | 显式写 0，**覆盖 legacy DDL 默认 999**（`V1.0.0.100:368` `stock INT DEFAULT 999`） |
| 前端表单 | `FoodManagement.vue:978-1176` 编辑/新增表单字段 = 名称/编码/图片/原料明细/分类/状态/售价/成本/**最低库存(minStock, :1167)**/描述 —— **无"库存数量"输入框**（全文 stock 出现处均为列展示 :303/:921-922、预警统计 :153、CSS） | UI 创建永远 stock=null → **0** |

**D2 结论**：
1. 初始值来源 = 接口入参（仅 API/SQL 可设）；UI 表单无入口。
2. 经 UI 新建的菜品 foods.stock = 0 且 legacy stock = 0 → POS 主菜单按 `stock===0` 前端拦截（`product-architecture-map-v2.md §13.8-Q8`：MenuSection.vue 售罄拦截不读 status=2）→ **新建菜品在 POS 主菜单即呈售罄**（代码链路推论；活库 `FD260926006 测试菜品销售A stock=0 status=1` 与该推论一致）。
3. 双表 DDL 默认值语义分裂：foods=0（无货）vs food=999（"不限"惯例，与 `posApi.ts` 999 兜底同源）——新菜品走显式 0 后分裂被掩盖，但默认值定义本身不一致（登记项 §8-3）。

## D3 — 更新链路：谁改 foods.stock

**扣减点 = 订单创建时（非 KDS 出餐时）**，两表双写：

| 链路 | 证据 | 行为 |
|------|------|------|
| POS 下单（主路径） | `PosOrderCreateServiceImpl.java:551-566` | `foodMapper.deductStock`（legacy，失败 400"库存不足"，含 ensureLegacyFoodRow 自愈）+ `foodNewMapper.deductStock`（foods，失败**仅 warn 不回滚** :564-566） |
| POS 下单（canonical 路径） | `PosOrderCreateServiceImpl.java:757-772` | 同型双写，foods 失败 warn（:769-772"两表数据不一致，记录日志但不再回滚"） |
| 超时取消回滚 | `OrderTimeoutTask.java:124` | `foodNewMapper.addStock(foodCode, quantity)` |
| 退款回滚 | `PosOrderPaymentServiceImpl.java:463,513` | `foodNewMapper.addStock(...)` |
| KDS 出餐 | `TrayServiceImpl.java:285-342`（scanServe：ready→served→idle）+ `:322` → `OrderNewServiceImpl.deductMaterialsForServe:685` | 出餐 = **状态流转 + 原料扣料（store_inventory）**；`KitchenOrderServiceImpl.serve:164-175`（后厨单出餐端点 `KitchenOrderController:322-341`）亦仅状态+WebSocket |

**对卡内"已确认"条目的勘误**：卡文称"KDS 出餐时 foods.stock 会 -1"。代码证据：foods.stock 扣减发生在**下单时**（每份 -quantity，两表），出餐时扣的是**原料库存**（store_inventory，扣减量 = `required_quantity × 份数`，`docs/business-logic/02-pos-order.md:45`）。活体验证佐证：`business-chain-verification-20260925.md:58`"foods.stock 100 → 99（菜品份数扣减）"发生在下单链路。

**原子性**：`FoodNewMapper.java:54` `UPDATE foods SET stock = stock - #{quantity} ... AND stock >= #{quantity}`（防负数，0 行=不足）；`:66` addStock 上限 99999。

## D4 — 与 store_inventory（原料库存）的关系

**完全独立，两套库存体系不联动**（引用已裁决结论，非本卡新发现）：

- `cross-domain-conflict-summary.md:15` 冲突点 #1 裁决（订单域，2026-08-18）："**POS 可售真相 = 菜品层 foods.stock**（当前事实成立）；『售罄从物料库存派生』被推翻；物料库存 = 完成时扣减的另一维度"；PADR-003 售罄与库存分离已决策。
- `product-domain-review-inventory.md:85`："POS 可售/售罄由菜品层 foods.stock 决定；真实物料库存（store_inventory）只在订单完成时被扣减——**两个库存体系不联动**"。
- 菜品双表（food/foods）之间的一致性靠 **POS 双写 + 启动同步**（`DatabaseFixConfig`），与原料库存无关。
- 原料扣料唯一入口 = 出餐扣料 `deductMaterialsForServe`（P0-KDS-DEDUCT 已收口，`p0-kds-deduct-closure-001.md`）。

## D5 — 配方（dish_recipes）与"反推可售份数"

**一份菜需要多少原料**：`dish_recipes`（food_id + material_id + required_quantity + unit）。活库 8 行（每菜 1 条原料；样例：生菜 0.1~0.4 斤/份）。

**系统里有两套 BOM 体系（未统一）**：

| BOM 表 | 消费方 | 对照的库存 | 活库状态 |
|--------|--------|-----------|---------|
| `dish_recipes` | 出餐扣料 `deductMaterialsForServe`；方案 D+/E `StockForecastService` | **store_inventory** | 8 行（在用） |
| `dish_inventory` | `BomCheckService`（checkDish/preOrderCheck/batchCheck） | **inventory 表**（非 store_inventory） | **0 行** → `BomCheckServiceImpl:116-120` 无 BOM 记录"视为可制作" → BOM 预检实为死路径 |

**"根据配方 + 原料库存反推可售份数"逻辑 —— 存在，已实现，但孤立**（既非"从未实现"也非"被删"）：

- 实现：`StockForecastServiceImpl.java:50-168`（方案 D+/E，2026-07-31 上线，`data-chain-action-plan.md:234`）。算法：对配方每原料 = `store_inventory.current_stock`（`StockForecastMapper:74-77`）÷ **实际每份用量**（自校准 = 近 N 天实际出库 ÷ 售出份数 `:80-93`，数据不足回退理论用量）→ **MIN(各原料可做份数) = maxServings（可做份数）**；另有理论份数、售罄天数（÷日均销量）、瓶颈原料、建议补货量、损耗差异（方案 E）。
- 触发方式：**按需**——产品中心"检查库存"按钮（`FoodManagement.vue:677-698`）→ `GET /v1/product-center/bom-check/{dishId}/forecast`（`BomCheckController:167-173`）。
- 消费端：**POS 前端零消费**（frontend-pos 全文无 bom-check/forecast/preOrderCheck）→ 结果**不写回 foods.stock、不驱动 POS 菜单、不拦截下单**（BomCheck 预检同样无 POS 调用，且其 BOM 表为空）。

## D6 — 现状 vs 目标语义差距

**目标（Owner 已确认）**：菜品库存 = **菜品可售份数**（配方 + 原料库存**动态算出**）。

**已实现（部分能力存在）**：
1. ✅ 反推计算引擎：StockForecast maxServings（D5）
2. ✅ 原料扣减链路：出餐扣料 store_inventory（P0-KDS-DEDUCT 收口）
3. ✅ 人工计数器 + 回滚闭环：下单扣/超时/退款回补（D3）
4. ✅ 售罄展示：POS 按 `stock===0` 前端拦截（不读 status=2）
5. ✅ 预警：minStock + 低库存统计（FoodManagement :153）+ POS 低库存（stock<10，useMenu）
6. ✅ 管理端决策看板：可做份数/售罄时间/补货建议/损耗差异（D+/E）

**缺失（差距清单）**：
| # | 差距 | 现状事实 |
|---|------|---------|
| G1 | **foods.stock 是人工计数器，不是派生值** | 原料补货不增菜品份数；原料盘亏/损耗不减菜品份数；两库存各自演化 |
| G2 | **无实时/动态回写** | 反推结果仅在管理端按需弹窗出现，不回写、不进 POS |
| G3 | **初始值无入口 + 默认 0 陷阱** | UI 表单无 stock 字段；新建菜品 = 0 = POS 即售罄（D2）；API 不设也是 0 |
| G4 | **双 BOM / 双原料库存体系未统一** | dish_recipes+store_inventory（在用）vs dish_inventory+inventory（死路径，D5） |
| G5 | **status=2 售罄死状态** | DB 定义售罄状态，C 端按 stock===0 判断，status=2 不被消费（T7，product-architecture-review :21） |
| G6 | **999 兜底语义混入计数器** | legacy 默认 999 + posApi 999 兜底（POS-DATA-004，client-audit-pos.md:21）——"不限"与"有限计数"同字段混用 |
| G7 | **foods.stock 变动无台账** | 无 log 表（对比 store_inventory_log 完备）；A7 悬置 |
| G8 | **无配方菜品无策略** | 反推逻辑对"无 dish_recipes 行"的菜品无定义（回退人工计数？无限？）——目标语义下必须回答 |

## D7 — 行业对标（引用卡文所载已调研结论）

| 行业标准模式 | 本系统现状 | 判定 |
|-------------|-----------|------|
| **Item Availability / Available Portions（可售份数，配方+原料动态算出）** | 计算引擎存在（StockForecast maxServings，D5），但定位于管理端按需看板，不承载 POS 可售 | **计算模式已具备，定位未到位**（G1/G2） |
| **无配方菜品 = 手动倒计数（Count Down List）** | foods.stock 当前正是此模式（D1/D3），且是唯一生效模式 | **计数模式已具备**（G3 入口缺失、G7 无台账） |

> 注：上述结论为卡文引用（Owner 提供之调研结论）；docs/ 中未检索到独立的行业调研文档（"Item Availability/可售份数/Count Down"无命中）——登记 §8-8。

## §8 — 阶段 1 登记清单（其他发现，不顺手修）

1. **status 枚举三方漂移**：DDL（1在售/2停售/3售罄，V6.0.0:26）vs 实体/DTO（1在售/0停售/2售罄，FoodCreateDTO:58）——`survey/指令6_...:38/400` 已录。
2. **dish_inventory BOM 体系空表**：BomCheckService 对照 inventory 表（非 store_inventory），preOrderCheck 死路径（D5）。
3. **双表默认值分裂**：foods.stock 默认 0 vs food.stock 默认 999（D2-3）；999 = "不限"惯例（POS-DATA-004 同源）。
4. **UI 新建菜品 = stock 0 = POS 即售罄**（代码链路推论 + 活库 FD260926006 佐证；D2-2）。
5. **foods 软扣/food 硬扣双轨**：foods 扣减失败仅 warn 不回滚（:564-566/:769-772），两表可静默不一致（d-ig-inventory-semantic-reconstruction-001.md:200 已录）。
6. **StockForecastMapper cast 失真**：`getSoldServings` 分母少计 → 每份用量高估、可做份数低估（p1-pos-foodid-map-001-scope-001.md:185 已录，D2 级）。
7. **套餐无独立 foods 库存**：`PosOrderCreateServiceImpl:264` 注释"组件扣减在出餐扣料路径（combo_ingredients）"——目标语义下套餐可售份数如何定义需阶段 2 回答。
8. **行业调研结论未落盘 docs**（D7 注）。

## 交接条件

- [x] 报告落盘（本文件）
- [ ] Owner 审过
- [ ] 方可进阶段 2（设计）——阶段 2 需回答的核心问题：可售份数是"派生值只读展示"还是"定期/事件回写 foods.stock"；无配方菜品（G8）与套餐（§8-7）的口径；双 BOM 体系（G4）是否合并。
