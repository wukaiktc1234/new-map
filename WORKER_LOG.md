# WORKER_LOG.md — ai-night-chore-1 夜间苦力执行日志

> 执行模式：Team Lead（lead）统筹 + 队友分线；**所有 git 写操作（add/commit/push 的 add/commit）只由 Lead 单点执行**，避免并发污染暂存区。
> 分支：`ai-night-chore-1`（基于 `ai-night-patrol-1` @ f907804）。
> 停止条件：系统时间到达 07:00，或遇到无法解决的编译/类型阻塞 → 见 §6 交接。

## 0. 硬约束（每条任务执行前自检）

1. **禁止修改后端核心业务逻辑**：状态机（OrderLifecycleService）、Controller 核心分支、权限链、订单主流程 —— 只读、只测、只报告，不改。
2. **禁止执行带写入的 SQL**；不碰 `flyway_schema_history`；不新增手写 schema 脚本。
3. **不碰 Owner WIP**：编辑任一文件前先 `git status --porcelain -- <file>`，**输出非空即跳过**（该文件已有 Owner 未提交改动，暂存会把 WIP 一起入库）。工作区基线：448 M + 1 D + 90 ??。
4. **精确 git add**：只用 `git add <具体文件>`，禁 `-A` / `.` / 目录通配；commit 前 `git diff --cached --numstat` 复核只含本批文件。
5. 只在本分支提交；push 仅由 Owner 显式指令触发（本轮结束时按"Token 耗尽交接"条款 push 本分支）。

## 1. 进度看板

| 循环 | 内容 | 状态 | 证据 |
|------|------|------|------|
| B | 后端 ServiceImpl 纯 Mockito 单测 | 进行中 | **23 个类 / 263 个用例全绿**，每类一提交 |
| C | 前端 TypeScript 报错清洗 | **本轮收口**（仅 clean 文件） | **145 → 93（−52，0 新增）**，Lead 独立复跑确认 93；基线 62 文件里 52 个带 Owner WIP 不可碰 |
| A | API 契约静态比对报告 | **本轮收口** | `docs/night-patrol/api-contract-diff.md`（232 行）：2057 端点 / 944 调用点 / 配对 898（95%），发现 58 条（高危 14 / 中低 16 / 类型精度 10 / 待确认 8） |
| D | 死代码 / 废弃注释清理 | 进行中 | 已删 2 个零引用 util 类（148 行）；另发现 3 个零引用枚举，**保留待裁**（见 §5） |

## 2. 已完成任务（按提交序）

| # | 提交 | 内容 | 验证 |
|---|------|------|------|
| B-1 | `4196f9a` | `DailySettlementServiceImplTest`（28 用例） | `mvn -o test -Dtest=DailySettlementServiceImplTest` 28/0/0 BUILD SUCCESS |
| B-2 | `6aaa54b` | `InventorySettingServiceImplTest`（11 用例） | 11/0/0 |
| B-3 | `9a743cc` | `InventoryWarningRuleServiceImplTest`（9 用例） | 9/0/0 |
| B-4 | `4d68bf4` | `InventoryCheckServiceImplTest`（13 用例） | 13/0/0 |
| B-5 | `b31ac75` | `DiningTableNewServiceImplTest`（12 用例） | 12/0/0 |
| B-6 | `42dbf37` | `StockForecastServiceImplTest`（10 用例） | 10/0/0 |
| B-7 | `e80c3a0` | `InventoryLocationServiceImplTest`（13 用例） | 13/0/0 |
| D-1 | `2c7e3e9` | 删除零引用 `util/PasswordGenerator`、`util/LabelRenderConfig` | `mvn -o compile` BUILD SUCCESS |
| B-8 | `eec8b6b` | `TableReservationNewServiceImplTest`（14 用例） | 14/0/0 |
| B-9 | `6882092` | `MaterialConsumptionServiceImplTest`（12 用例） | 12/0/0；`mvn -o test-compile` 全量通过 |
| C-1 | `d4d7ff0` | 前端 21 文件类型清洗（共享 types 放宽 + 缺失 import + `types/capacitor.d.ts` + 无效图标/spread no-op） | `npx vue-tsc --noEmit` → **93** error TS（基线 145） |
| LOG | `0c9eb24` | `WORKER_LOG.md` 建档 | — |
| A-1 | `a00c02a` | `docs/night-patrol/api-contract-diff.md`（队友 `api-contract-audit` 产出，Lead 抽查复核） | 抽查 3 条高危均成立：会员冻结/拉黑路由缺失、HR 四类配置仅 GET、日结状态数字/字符串映射错位 |
| B-10 | `ee1f5e9` | `InventoryLossServiceImplTest`（12 用例：审批状态映射、报损扣减 LOSS 源类型与跳过规则） | 12/0/0 |
| B-11 | 本次 | `ComboInventoryServiceImplTest`（9 用例：新增闸门、单位回退、布尔删除） | 9/0/0 |
| B-12 | `b888298` | `SelfPurchaseServiceImplTest`（8 用例：编号/初始态、入库追溯码+QUICK_STOCKIN、报销/取消闸门） | 8/0/0 |
| B-13 | 本次 | `InventorySummaryServiceImplTest`（2 用例：分页委托透传） | 2/0/0 |
| B-14 | 本次 | `MaterialConsumptionAuditServiceImplTest`（6 用例：FAILED 审计行映射、2000 字截断、空清单落 NULL、写入异常吞掉） | 6/0/0 |
| B-15 | `4d2cbbb` | `InventoryDeductionServiceImplTest`（14 用例：模式A 可用量回退链/扣完置 used、模式B 配方折算+JSON 回写、批次跨码消耗、内存记录） | 14/0/0 |
| B-16 | `84362a7` | `PricingServiceImplTest`（13 用例：FOOD/COMBO 调价、套餐优惠重算、批量 100 上限、历史筛选、变动率/毛利、limit 钳制） | 13/0/0 |
| B-17 | `0e1c9b5` | `InventoryOutboundServiceImplTest`（12 用例：OB 单号/总数量、草稿态闸门、审批人取安全上下文、执行出库扣减与金额、ID 解析容错） | 12/0/0 |
| B-18 | `d645ea2` | `StoreInventoryLogServiceImplTest`（10 用例：四道写路径校验、movement 字段映射、type→movement_type、备注合并截断、门店越权收敛） | 10/0/0 |
| B-19 | `37c18d3` | `AlertServiceImplTest`（11 用例：创建/查询、确认与解决迁移、删除布尔、阈值判定矩阵） | 11/0/0 |
| B-20 | `3e47d78` | `CaptchaServiceImplTest`（10 用例：生成/缓存、5 次锁定、大小写不敏感、失败计数递增、脏计数容错） | 10/0/0 |
| B-21 | `469750d` | `InventoryAdjustServiceImplTest`（15 用例：创建/查询、审批历史三步、草稿闸门、执行增减与类型映射；特征化"表头 vs 明细金额口径不一致"） | 15/0/0 |
| B-22 | `827500b` | `NotificationPreferenceServiceImplTest`（10 用例：upsert 双分支、唯一键冲突回退/重抛、默认开启语义、16 条默认偏好、批量） | 10/0/0 |
| B-23 | `16d498c` | `ReportConfigServiceImplTest`（9 用例：缓存命中/副本/隔离、upsert 双分支、保存后失效、默认 5 键） | 9/0/0 |
| A-2 | 本次 | `docs/night-patrol/code-quality-performance-report.md`（4 条 P0 + 4 条 P1 + 6 条 P2 实测缺陷与修法；含"如何继续补测试"的可执行脚本） | 只读汇总，无代码改动 |

### 单测覆盖要点（供白天 Agent 接手）
- 日结：查询权限过滤现状（`currentUser==null` 会丢弃全部筛选条件 → 特征化测试已固化）、元→分换算、确认状态机、日结完成事件发布与异常隔离、批量生成"全失败必须抛错"。
- 库存：设置项类型推断（boolean/integer/json/string）、预警规则分页 `enabled→is_enabled` 布尔映射、盘点单草稿态闸门与差异数量/金额换算、库位使用率除零边界、库位分页仓库名批量回填。
- 桌台：编码唯一性（`excludeTableId` 排除自身）、按状态统计（未知状态只计总数）、分页参数透传。
- 预测：瓶颈原料取最小可做份数、数据不足回退理论用量、窗口 1..90 钳制、差异分析按 `|variance|` 降序 + topN 截断 + 脏行跳过。

## 3. 进行中

- B-10：继续挑选下一个无单测 ServiceImpl（全仓 **324 个** ServiceImpl 无对应测试类，池子远未掏空；已交付 9 类 / 122 用例）。
- C-2（待 Owner 放行）：剩余 93 条全部根因在 Owner 脏文件，**本轮一律未动**。队友已给出两条"一改消多条"的批量线索：
  1. `DataTable.vue:22` 把 `align?: 'left'|'center'|'right'` 放宽为 `string` → 一次消 10 条（7 份脏 vue 的本地 ColumnDef 都只差这一个字段）；
  2. 9 条是脏 vue 引用了不存在的 element-plus 图标（Points/Coupon/TrendingUp/BankCard/Refund/DatePicker/Transfer/Copy），对应关系：Coin/Ticket/TrendCharts/CreditCard/RefreshLeft/Calendar/Sort/DocumentCopy。
- A 已收口，报告见 `docs/night-patrol/api-contract-diff.md`；Top 高危：日结金额二次换算（小 100 倍）、日结状态恒「待日结」、会员风控/HR 配置/优惠券/薪资批次/POS 会员/排班 6 簇写操作打空（404/405）。

## 3.1 单测避坑（本轮实测，后续写测试直接用）

1. **`ServiceImpl.getOne(wrapper)` 在 Mock 上必须双打桩**：`selectOne(any())` **和** `selectOne(any(), anyBoolean())` 都要 stub，否则 getOne 恒返回 null（单参方法在 Mock 上不转发）。
2. **`removeById` / `save` / `updateById` 走 baseMapper**：继承 `ServiceImpl` 的类要用 `ReflectionTestUtils.setField(service, "baseMapper", mapper)` 补齐。
3. **Lambda 条件断言**：先 `initTableInfo(assistant, Entity.class)`（否则 "can not find lambda cache"）；取参数用 `wrapper.getParamNameValuePairs()` + 先调 `getSqlSegment()`；**ORDER BY 只出现在 `getCustomSqlSegment()` 里**，`getSqlSegment()` 不含排序。
4. **MP `orderBy(condition, isAsc, column)` 第二参是 isAsc**，传 isDesc 会反向（已在 `PricingServiceImpl` 实测到）。

## 4. 待办队列（按优先级）

1. B：继续补 ServiceImpl Mockito 测试（每类一提交，`mvn -o test -Dtest=<类名>` 绿了才提交）。
2. C：继续压 TS 报错（仅 clean 文件 + clean 共享类型间接消错）。
3. D：继续扫零引用 util / 废弃常量，安全删除后过编译。
4. 池子掏空后：输出《代码质量与性能优化建议报告》到 `docs/night-patrol/`。

## 5. 遗留观察（只报不改，属核心业务或需 Owner 裁决）

- **库存调整单金额口径不一致（B-21 实测，待修）**：`InventoryAdjustServiceImpl.createAdjust:112-114` 表头 `totalAdjustAmount` 直接把入参（元）累加，而明细 `adjustAmount` 已 ×100 存分；`updateAdjust:219-226` 又改成按明细分值累加（可为负）。同一字段"创建按元、更新按分"，且可为负 → 报表对账必错。
- **定价排序方向反转（B-16 实测，待修）**：`PricingServiceImpl.queryHistory:138-139` 用 `orderBy(true, isDesc, ...)`，而 MP 第二个参数是 **isAsc** → `sortOrder=desc` 实际升序、`asc` 实际降序。已用特征化测试固化，未改实现（属 ServiceImpl 行为，留白天 Agent）。
- **契约层（A 报告，需 Owner 决策 2 条）**：① `GET /v1/roles/list` 前端传 `status=1/0`、后端要 `active/inactive`（`RoleController.java:150-155`）→ 筛选静默失效；② `JacksonConfig.java:51-54` 全局 Long→String 可能让 MyBatis-Plus `Page.total/size/current` 变字符串，与前端 number 类型冲突 —— 需实抓一次响应体才能定级（Q1）。

- `DailySettlementServiceImpl.buildQueryWrapper`：`currentUser == null` 时提前 return，**日期/状态/门店筛选全部失效**（已用特征化测试固化现状）。
- `DiningTableNewServiceImpl.update`：`updateById` 后直接 `selectById(...).getStoreId()`，回查为空会 NPE。
- `InventoryWarningRuleServiceImpl.getWarningRulePage`：`category` 参数被静默忽略（代码注释自认）。
- 前端 52 个报错文件带 Owner WIP，本轮**一律未动**，其 TS 报错需 Owner WIP 落定后再修。

## 5.1 D 循环：零引用枚举扫描结论（保留待裁）

`backend/src/main/java/com/foodtraceability/enums/` 下 3 个枚举在全仓 Java/XML 中**零引用**：
`HealthCertificateStatusEnum` / `HealthCertificateExpenseStatusEnum` / `HealthCertificateApprovalStatusEnum`（共 168 行）。
**未删除**，理由：这三个枚举编码的是「健康证状态」业务词典，前端 `types/healthCertificate.ts` 仍在用同义状态值；删除会把未来接回后端的词典一并清掉，属产品决策而非纯清理。建议 Owner 裁决：要么接入实体字段，要么连同前端类型一起清。

## 6. 交接说明（2026-10-23 06:25 收口）

### 6.1 分支与提交
- 分支：`ai-night-chore-1`（基于 `ai-night-patrol-1` @ f907804），已推送 origin，ahead=0。
- 本轮共 **40 个提交**：23 个测试类、1 个前端类型修复、1 个死代码删除、2 份报告、其余为日志/文档。
- 查看：`git log --oneline f907804..ai-night-chore-1`

### 6.2 终检证据
- 23 个新测试类一次性全跑：`mvn -o test -Dtest="<23 个类名>"` → **Tests run: 263, Failures: 0, Errors: 0, BUILD SUCCESS**（2026-10-23 06:24）。
- 全量测试编译：`mvn -o test-compile` → BUILD SUCCESS。
- 前端：`npx vue-tsc --noEmit` → **93 条**（基线 145，−52，0 新增）。
- 生产代码改动：**0 行**（仅新增测试 + 删除 2 个零引用类 + 前端类型层改动）。

### 6.3 给白天 Agent 的 3 条优先修复（都有测试兜底，改完跑对应测试即可）
1. **P0 定价排序反转**：`PricingServiceImpl:139` 的 `orderBy(true, isDesc, ...)` 改传 `!isDesc`；同步把 `PricingServiceImplTest#queryHistory_*` 的两条特征化断言改回正常方向。
2. **P0 调整单金额口径**：`InventoryAdjustServiceImpl` 创建(112-114)与更新(219-226)统一为"分"；改完更新 `InventoryAdjustServiceImplTest` 里三条特征化断言。
3. **P1 扣料静默缺口**：`InventoryDeductionServiceImpl` ①批次不足仅 warn（199-201）②按配方不写库存（78-92）；前者改抛异常/返回缺口，后者接 `InventoryService`，测试已固定在对应用例。

### 6.4 未完成（下一夜可继续）
- 仍有 **300+ `ServiceImpl` 无单测**（挑选脚本见质量报告 §5）。
- 前端剩余 **93 条** TS 报错根因全在 Owner WIP 脏文件，需 Owner 放行后才能修；两条批量线索（`DataTable.vue:22` 一改消 10 条；9 条图标名错误）见 §3。
- 3 个零引用健康证枚举（168 行）保留待 Owner 裁决，见 §5.1。

### 6.5 纪律复核
- 所有提交均为精确 `git add <文件>`，每次 commit 前 `git diff --cached --numstat` 复核只含本批文件。
- Owner WIP（448 M + 1 D + 90 ??）全程未暂存；测试目录下既有的 `PaymentServiceImplTest.java`（Owner WIP）未触碰。
- 未执行任何写入 SQL、未碰 `flyway_schema_history`、未改后端核心业务逻辑。

---

# 无人值守班次（2026-10-23 10:14 → ）· 白天 Agent

> 本段由长线值守 Agent 追加。上一段（06:25 夜间收口）保留不动。

## 1. 前置安全（已完成）

| 动作 | 结果 |
|---|---|
| git push origin master | 通过（GitHub 连接不稳，重试 3 次成功；此后每次提交都重试推送，ahead 恒为 0） |
| E2E 核心链路 | 12/12 全绿：POS 登录 → 下单 → quick-pay 支付 → 后厨接单 → 开始制作 → 完成出餐 → 扣料流水 |
| 托盘绑定 | 通过（KDS 设备凭证链路打通后） |

### 1.1 E2E 期间发现并修复的三个真问题

1. KDS 凭证被 JWT 过滤器打回 401（阻断 KDS/托盘整条链路）
   KdsTokenFilter 认证成功后，JwtAuthenticationFilter 仍把 kds_* 当 JWT 解析 → 401。
   修法：JwtAuthenticationFilter 遇到 kds_ 前缀直接放行（认证已由 KDS 过滤器完成）。
2. food_trace_code 表比实体少 11 个字段（出餐生成追溯码 500）
   实体已带 qr_code_url/order_number/production_station/produce_time/inventory_deduction/material_details/make_start_time/serve_method/remark/create_by/update_by，活库从未迁移。
   修法：scripts/db-fix/2026-10-23-food-trace-code-align.sql（可复放 ADD COLUMN IF NOT EXISTS），只加列，不改实体不改业务。
3. e2e_pos_kitchen.py 的支付路由走的是遗留表
   POST /v1/pos/orders/order/pay → PosOrderPaymentServiceImpl 查 orders_legacy（Order 实体 @TableName(orders_legacy)），新单在 orders → 必然「订单不存在」；GET /v1/pos/orders/order/{no} 同理。
   绕过：新增 scripts/e2e_core_chain.py，支付走 POST /v1/pos/orders/{orderId}/quick-pay，订单状态与扣料改用活库断言。未改原脚本。
   命令：python scripts/e2e_core_chain.py --password 123456 --tray auto

## 2. 主线纪律执行情况

- OrderLifecycleService / OrderNewServiceImpl 等状态机核心：零改动。
- WIP 业务：零合入（wip/night-merge-handover-20261023 分支原样保留，449 文件）。
- 每次独立提交后立即 push，origin/master 与本地全程同步（ahead=0）。

## 3. 循环 A：P1 缺陷（3 修 1 记录）

| 编号 | 位置 | 处置 | 验证 |
|---|---|---|---|
| P1-3 报损审批不落痕迹 | InventoryLossServiceImpl.approveInventoryLoss | 写 approveUserId/approveTime（原被注释），审批意见进审计日志 | InventoryLossServiceImplTest 12/12 |
| P1-4 自采入库硬编码主仓库 | SelfPurchaseServiceImpl.inboundSelfPurchase | 优先取 SelfPurchase.warehouseId/Name，缺失才回退并 warn；补多仓用例 | SelfPurchaseServiceImplTest 9/9 |
| P1-1 批次不足只告警 | InventoryDeductionServiceImpl.deductByBatchCode | 改为抛 BusinessException（带缺口数量）+ 事务回滚，消除假成功 | InventoryDeductionServiceImplTest 14/14 |
| P1-2 按配方扣料不写库存 | InventoryDeductionServiceImpl.deductByRecipe | 未修，只记录：见 §5 | — |

## 4. 循环 B：补单测

| 类 | 用例数 | 覆盖点 |
|---|---|---|
| LocationServiceImplTest | 10 | null 短路、deleted=1 不可见、按门店/仓库/位置反查委托与参数、未映射禁止数值兜底、活跃列表透传 |

剩余无测试 ServiceImpl：302 个（挑选脚本见 docs/night-patrol/code-quality-performance-report.md §5）。

## 5. 未做/待裁决（明日请 Owner 过一眼）

1. P1-2 按配方扣料不写库存 —— 未改：deductByRecipe 目前只落 MaterialUsageRecord + 内存 DTO + 追溯码 JSON，真正的库存扣减在 OrderNewServiceImpl 出餐链路里做。若在此处再加一次 decreaseInventory，会与主链路重复扣减。属口径决策，需 Owner 明确该入口是否还对外使用；先记录不改。
2. POS 读取接口仍绑遗留表：GET /v1/pos/orders/order/{orderNumber}、POST /v1/pos/orders/order/pay、PosOrderQueryServiceImpl 全部查 orders_legacy，与 orders 新单不通。修它等于改订单主读路径（状态机邻域），本轮不动。
3. InventoryLoss 缺 approve_remark 列：审批意见暂进日志；补列需 Owner 点头后再加 DDL。
4. positions 种子数据与活库口径漂移：DEPT_MANAGER / SUPER_ADMIN 在活库不存在，启动时职位-角色映射会跳过（已降为 WARN，不再 ERROR）。要修需对齐种子数据与活库职位编码。

## 6. 循环 C：前端 TS 报错（进行中）

- 起点 253 条（Owner 记忆的 93 条应为旧口径），本轮降至 197 条，修了 56 条：
  - src/api/asset/inventory.ts 块注释未闭合（38 条，一行 */）
  - AssetDisposal.vue DisposalStatus 用 import type 导入却当值用（10 条）
  - StatusTag.vue StatusColorType 缺 primary/orange/danger/default（4 条，CSS 变量早已存在）
  - modules/hr/menu.ts permissions → permission（1 条）
  - utils/permissions.ts 对象字面量重复键（3 条）
- 余下大头：ReceivingMobile.vue(85) / BankAccountManage.vue(18) 等 Owner WIP 未跟踪文件，按规矩不碰。

## 7. 明日建议顺序

1. 先跑 python scripts/e2e_core_chain.py --password 123456 --tray auto 确认链路仍绿（后端需用 JDK21 启动，见 §8）。
2. 继续循环 B（挑 PurchaseOrderServiceImpl / FinanceRecordServiceImpl 这类有真实分支的）。
3. 循环 C 继续清已跟踪文件的 TS 报错，WIP 文件留给 Owner。
4. 若要解锁 POS 新单读取/支付，需 Owner 决定是否把 PosOrderQueryServiceImpl 从 orders_legacy 迁到 orders。

## 8. 环境备忘（重要）

- JAVA_HOME 系统变量指向不存在的 H:\fuwu\jdk-17.0.17+10；可用 JDK 在 P:\my-new-project\JDK21（Temurin 21.0.9，与 pom 的 Java 21 匹配）。
  启动：$env:JAVA_HOME='P:\my-new-project\JDK21'; mvn -o spring-boot:run -Dspring-boot.run.profiles=pg
- 本地 Postgres：PostgreSQL\18\bin\pg_ctl -D PostgreSQL\18\data start|stop（SCM Start-Service 无权限）。
- scripts/e2e_pos_kitchen.py 的支付步骤仍会失败（路由遗留问题，见 §1.1-3）；核心链路以 e2e_core_chain.py 为准。
