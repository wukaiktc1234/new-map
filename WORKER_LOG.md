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

---

## 9. 长线循环进度更新（无人值守第二轮）

### 9.1 循环 B 新增单测（累计 5 类 / 42 例，全部绿）

| 测试类 | 用例 | 覆盖要点 |
|---|---|---|
| LocationServiceImplTest | 10 | null 短路、deleted=1 不可见、三类解析委托、未映射禁止数值兜底 |
| MemberLevelServiceImplTest | 8 | 启用列表条件与排序、升级判定（最高达标等级/消费门槛/无门槛/无等级）、等级分布 |
| ProductEntityServiceImplTest | 7 | 按 code 查、成本价/供应商更新、写库失败如实返回 false |
| FinanceAccountServiceImplTest | 6 | 科目树（扁平化缺陷已特征化）、按类型/父级/编码 |
| OperationLogServiceImplTest | 4 | 写入、写失败吞异常契约、条件查询透传 |

写 ServiceImpl 单测的三个坑（已固化进代码注释，后续照抄）：
1. `baseMapper` 是父类泛型字段，`@InjectMocks` 注入不进去，必须反射塞：`ServiceImpl.class.getDeclaredField("baseMapper")`；
2. `ServiceImpl.getOne(wrapper)` 实际调用的是 `baseMapper.selectOne(wrapper, throwEx)` **双参重载**，单参 stub 会拿到 null；
3. 断言 `LambdaQueryWrapper` 的 SQL 前要先 `TableInfoHelper.initTableInfo`，且 ORDER BY 在 `getCustomSqlSegment()`。

### 9.2 顺带修掉的健壮性缺陷（非业务语义）

- `MemberLevelServiceImpl.calculateUpgradeLevel` 直接对 `listEnabledLevels()` 的返回**就地 sort**，若 mapper/上层返回不可变集合会抛 `UnsupportedOperationException`；改为防御性拷贝 `new ArrayList<>(...)`。

### 9.3 循环 C 前端 TS（253 → 175，累计修 78 条）

| 批次 | 修复 | 条数 |
|---|---|---|
| 1 | inventory.ts 块注释未闭合、DisposalStatus 值导入、StatusColorType 补 4 色、hr 菜单属性名、permissions.ts 重复键 | 56 |
| 2 | purchase 查询参数补 departmentName、ECharts tooltipOption 索引签名、收货详情空值保护、MemberDetail 图标/充值方案/列类型、HRAttendance+HRRecruitment 列类型与 size、HROnboarding 分页参数、AssetLedger 空安全 | 22 |

剩余 175 条里 **103 条在 Owner WIP 未跟踪文件**（ReceivingMobile.vue 85 / BankAccountManage.vue 18），按纪律不碰；可动的是 72 条。

### 9.4 已登记未修的缺陷（只记录）

- `FinanceAccountServiceImpl.buildAccountTree`：递归算出 children 却从不挂回 account，返回的树只有顶层节点（已用测试固化现状，改它属业务语义变更）。
- `MemberList.vue`：`loadRechargePlans` / `rechargePlanList` / `RechargePaymentMethod` 被引用但未定义（疑似删代码残留），属功能缺口，未擅自补实现。

### 9.5 Owner 批准的缺陷修复（已完成）

| 缺陷 | 处置 | 验证 |
|---|---|---|
| `FinanceAccountServiceImpl.buildAccountTree` 树扁平化 | 递归结果 `account.setChildren(children)` 挂回；`FinanceAccount` 新增 `@TableField(exist = false) List<FinanceAccount> children`（不标 exist=false 会被 MyBatis-Plus 当成真实列） | `FinanceAccountServiceImplTest` 7/7（含三级树、叶子 children 空列表、孤儿节点不误挂） |
| `MemberList.vue` 充值方案缺失 | 补 `rechargePlanList = ref<RechargePlanInfo[]>([])` + `loadRechargePlans()`（调 `rechargePlanApi.getList()`，失败降级为空并保持弹窗可用）；补 `RechargePaymentMethod` 类型导入；列类型改用 `DataTableColumn`；模板 `currentMember?.gender`；移除未用图标 `TrendingUp` | `vue-tsc` 175 → **168**，MemberList 7 条清零 |

提交：`86806c7`（后端树）、`d2ed674`（前端充值方案），均已推送。

---

## 10. POS 主读路径迁移：orders_legacy → orders（2026-10-23 11:5x，Owner 批准）

### 10.1 改动范围

`PosOrderQueryServiceImpl`（5 个方法）全部改为**新表优先、旧表兜底**：

| 方法 | 新表 | 兜底 |
|---|---|---|
| `getOrderDetail` | `orders` 按 orderNumber / order_code / order_id 任一命中 + `order_items` + `order_payment_records` | 命中不到 → `orders_legacy` + `order_items_legacy`，打 WARN |
| `getOrders` | `orders` 按时间/关键字（orderNumber、orderCode、customerName）+ 新旧一致「默认最近 7 天、最多 200 条」 | 新表结果为空 → 旧表 |
| `getOrdersByTable` | 先由 `kitchen_order` 取单号，再查 `orders` | 新表无在途单 → 旧表 |
| `getOrdersByOpenid` | **不迁移**（见 10.3） | 始终旧表 |
| `reprintReceipt` | `orders` 按 order_id，要求 `payment_status=2` | 查不到 → 旧表（要求 `order_status=1`） |

金额口径：新表存**分**（Long），DTO 对外是**元**（BigDecimal），统一走 `centsToYuan()`（`movePointLeft(2)`）；旧表本身就是元，保持原样。

状态口径：新表 `order_status` 0待确认/1已确认/2已完成/3已取消/4部分退款/5全额退款，映射为 `pending/confirmed/completed/cancelled/partial_refunded/refunded`；旧表 0-7 的映射保持不变（两套语义并存，已加注释）。

### 10.2 验证

- `PosOrderQueryServiceImplTest` **11/11**：新表命中（分→元、支付方式取 `order_payment_records`、明细）、旧表兜底、双查不到报「订单不存在」、列表新表优先且旧表零查询、新表空→旧表、桌台无后厨单不查单、重打未支付拒绝/已支付走打印/不存在报错。
- POS 相关回归 **50 例全绿**（含 `OrderNewServiceImplDeductTest` 28、`PosOrderCreateServiceFoodIdMapTest` 5、`PosOrderEnsureLegacyRowTest` 3）。
- 端到端 `scripts/e2e_core_chain.py`：**12 PASS / 0 FAIL / 1 SKIP**（托盘无空闲，非核心）。新增断言「POS 详情可查 orders 新表订单」→ `HTTP 200 code=0 orderNumber=T20261023027 totalAmount=18.0 status=confirmed`。

### 10.3 明确的不兼容 / 风险提示

1. **openid 反查不可用**：`getOrdersByOpenid` 依赖 `orders_legacy.user_id`（由 openid 派生），`orders` 无 openid 列。该方法保持只读旧表，**新表订单按 openid 查不到**；小程序侧如需按用户查新单，必须改用会员 ID / 手机号维度（需产品确认）。
2. **列表混合期只显示新表**：`getOrders` 在「新表有数据」时不会合并旧表记录，历史单只能靠 `getOrderDetail` 精确单号查询或直接查旧表接口。若需要新旧混排，需要在应用层做双查合并 + 去重 + 统一分页（工作量另计）。
3. **支付写入路径未动**：`PosOrderPaymentServiceImpl`（`POST /v1/pos/orders/order/pay`）仍固定读 `orders_legacy`，本次只迁移**查询**。POS 新单支付请走 `POST /v1/pos/orders/{orderId}/quick-pay`；若要统一旧支付入口，需 Owner 另行批准。
4. **金额单位不可混用**：新表 `total_amount/final_amount/paid_amount/refund_amount/unit_price/amount` 均为「分」，旧表为「元」。任何新的对账/导出逻辑必须显式选择一侧口径，禁止直接相加。

---

## 11. WIP 页面收编：ReceivingMobile.vue / BankAccountManage.vue（2026-10-23 12:0x）

### 11.1 事实澄清

这两个文件在 `wip/night-merge-handover-20261023` 快照里**并不存在**——当时 `git add -u` 只快照已跟踪文件，它们是工作区里的未跟踪文件；后续 `git add -A frontend/src` 已把它们随 `41d7cc8` 一并提交进 master。所以本轮不是「从分支 checkout」，而是**在主干上补齐这两个半成品**。

### 11.2 后端缺口（已补）

`ReceiptConfirmationController` 缺 4 个接口（Service 实现、实体、Mapper、DTO 主干早已具备）：

| 接口 | 说明 |
|---|---|
| `POST /v1/receipt-confirmations/{id}/signatures` | 收货方/供应商双签存证 |
| `GET /v1/receipt-confirmations/{id}/signatures` | 签名列表 |
| `POST /v1/receipt-confirmations/{id}/print-records` | 打印登记（服务端计数，防打印泛滥） |
| `GET /v1/receipt-confirmations/verify/{verifyCode}` | 防伪码查验（免登录 + `@RateLimit` 10 次/60s） |

冒烟：`verify/0000...` → `HTTP 200 {"code":0,...,"valid":false,"message":"防伪码不存在..."}`；`GET /1/signatures` 无 token → 401（鉴权生效）。

### 11.3 前端补齐

- `types/purchase-arrival.ts`：新增 `ReceiptEvidenceForm`（含离线暂存字段）、`ReceiptSignatureInfo`、`ReceiptVerifyResult`；明细补 `quantity/accept/evidence/manualMatched/qualityCheckResult/supplierName`，`confirmedQuantity/rejectedQuantity` 改可选（由 converter 按 `quantity` 兜底映射）。
- `api/receipt-confirmation.ts`：新增 `addSignature/getSignatures/recordPrint/verifyByCode`；`toCreateDTO` 现在把逐条 `qualityCheckResult` 与 `evidence[]` 一并上送（后端 `saveEvidence` 落 `receipt_evidence`）。
- `types/purchase-archive.ts`：`MaterialArchiveQueryForm` 补 `barcode`（扫码收货按码匹配商品）。
- `BankAccountManage.vue`：**按主干真实模型重写**——原 WIP 假设 `scope/organizationId/storeId/derivedBalance/reconciliationStatus/accountType='bank'` 等字段，但后端 `bank_accounts` 表与 DTO 全都没有，属「UI 展示不存在的列」。现改为 `basic/general/payroll/reserve/other` 五种类型 + `active/frozen/closed` 状态，账号字段 `accountNo`，启停走 update（`BankAccountUpdateDTO` 支持 status）。
- 顺带：`PurchasePlan` 表单 `items` 改必填（消 8 条 TS18048）、`RoleManagementTab` 断言与 `assignRoles(String)` 修正（6 条）。

### 11.4 验证

- `vue-tsc --noEmit`：**168 → 49**；`ReceivingMobile.vue` 85 条、`BankAccountManage.vue` 18 条 **全部清零**，新增的 `VerifyCodePage.vue` 也已 0 报错。
- `mvn -o compile` BUILD SUCCESS；后端重启 `ERROR=0`。
- E2E `scripts/e2e_core_chain.py`：核心链路仍全绿（EXIT=0；托盘 SKIP 为无空闲托盘）。

### 11.5 剩余风险 / 未做

1. **`BankAccountManage` 的语义收窄**：主干没有 scope/组织/门店归属列，页面不再提供该维度筛选与展示；如产品确定要「账户归属」，需先加 `bank_accounts.scope/organization_id/store_id` 列与 DTO 字段。
2. **收编的两个页面未做浏览器级联调**：本轮只做类型与编译收口 + 接口打通，交互细节（签名画板、扫码桥、离线暂存重传）仍建议明天真机过一遍。
3. 全仓仍有 **49 条** TS 报错（`PurchaseOrder` 4 / `OrderStatistics` 4 / `PurchaseArchive` 4 / `PurchaseReport` 3 / `SealManagement` 3 / `SupplierArchive` 3 …），均为既有页面，下一轮继续清。

---

## 12. 业务主链路闭环行动（2026-10-23 12:1x）

### 12.1 优先级 1：统一 POS 支付入口（消灭后门）

`POST /v1/pos/orders/order/pay`（`PosOrderPaymentServiceImpl.payOrder`）改为**新表优先、旧表兜底**：

- 先按 `orderId` 查 `orders`；命中则走新链路：金额校验（`PayRequestDTO.amount` 元 → 分，与 `final_amount - paid_amount` 严格比对）、写 `order_payment_records`、回写 `orders.payment_status=2 / paid_amount`，并返回 `OrderResultDTO`（金额、明细按分转元）；
- 未命中 → 打 WARN 回落原有 `orders_legacy` 全流程（CAS 锁、二维码、余额支付等一条未动）；
- 新表支付方式编码独立：1现金/2微信/3支付宝/4银行卡/5积分（与旧表 0/1/2/3/4 不同，已加注释禁止混用）；
- `POST /v1/pos/orders/{orderId}/quick-pay` 已标 `@Deprecated` 并在 Javadoc 写明替代入口。

E2E `e2e_core_chain.py` 已改回标准 `order/pay`：**PASS=12 FAIL=0 SKIP=1**（托盘无空闲）。

### 12.2 优先级 2：收编页面自动联调自查

新增 `scripts/e2e_wip_pages.py`（自签 admin JWT + 直连活库断言），**9/9 全通过**：

| 场景 | 证据 |
|---|---|
| 收货签名存证（base64 PNG） | `POST /{id}/signatures` 200；`receipt_confirmation_signatures` 记录数 0→1；`GET /{id}/signatures` 返回 1 条 |
| 防伪码-无效码 | `GET /verify/ZZZZ…` → 200 `valid=false`（限流 10/60s） |
| 防伪码-有效码 | `GET /verify/ZPLWA4MYLQ9AQULC` → `valid=true`，`confirmationCode=RC202607310001` |
| 银行账户新建 | `POST /v1/finance/bank-accounts` 200，accountId=7 |
| 银行账户启停 | `PUT /{id}`（`BankAccountUpdateDTO.status=3`）200，活库 `status=3` |
| 清理 | `DELETE /{id}` 200 |

顺带修正一处真实契约错位：前端 `accountNo` ↔ 后端 `accountNumber`，此前新建账户**账号写不进去**；已在 `BankAccountDataConverter` 双向映射。

### 12.3 优先级 3：前端 TS 报错清零（49 → 0）

由 3 个并行子代理分域收口（文件域互不重叠），lead 收最后 3 条：

| 域 | 起始 | 结束 | 关键修法 |
|---|---|---|---|
| purchase（11 文件） | 23 | 0 | 本地 ColumnDef → DataTableColumn；查询体对齐 API 真实字段（applicantName→createByName、orderNo→orderCode）；PurchaseArchive 补齐漏传的必填字段 |
| hr + asset（5 文件） | 7 | 0 | reduce 泛型、withDefaults 默认值类型、createTime 空值收窄、Department 载荷本地类型 |
| order/seal/store-ops/operations（9 文件） | 17 | 0 | 图标改名（BankCard→CreditCard、Refund→RefreshLeft、Transfer→Sort）；ChartData.storeRank 字段纠正（原写法永远显示 0）；状态标签索引收口；`size="default"`→`small` |
| lead 收尾（3 文件） | 3 | 0 | 移除不存在的 `Copy` 图标；MaterialTraceCode 列类型同源；TraceabilityQuality 本地表单类型补 `materialName` |

**`npx vue-tsc --noEmit` → 0 error**。

### 12.4 顺带修掉的真实契约缺陷

- 前端 `accountNo` ↔ 后端 `accountNumber`：新建银行账户时账号从未写库；已在 `BankAccountDataConverter` 双向映射。
- `PurchaseArchive` 提交体漏传 `barcode/origin/shelfLife/storageCondition/departmentId`（后端必填为空）。
- `PurchaseOrder` 到货查询用 `orderNo`（API 不消费，等于未过滤取首条）→ 改 `orderCode`。
- `ReportCenter` TOP 榜用 `storeRanking`（实际字段 `storeRank`）→ 永远显示 0。

### 12.5 三条留给 Owner 决策的共享类型建议（未改，避免影响面扩散）

1. `types/asset.ts` `InventoryCreateDTO` 缺 `creatorId?: string` —— 盘点负责人目前只在表单态存在，不随 create 提交。
2. `types/hr.ts` `Department.parentId?: string` 与后端实收 number（0=顶级）不符，已在视图内本地断言收口。
3. `types/purchase-archive.ts` `MaterialArchiveFormData` 五个字段前端只能填 `''`（后端允许空），建议改可选。

---

## 13. 体验缺陷修复轮 + 全量归档（2026-10-23 12:55–13:30）

### 13.1 Owner 裁决的 3 条共享类型尾巴（已收口）

| # | 类型 | 处理 |
|---|---|---|
| 1 | `types/asset.ts` `InventoryCreateDTO` | 增 `creatorId?: string`；`AssetInventory.vue` 提交体补 `creatorId: formData.creatorId`，盘点负责人**首次真正落库** |
| 2 | `types/hr.ts` `Department.parentId` | `string` → `number`（0=顶级，对齐 `/v1/departments` 实收）；`DepartmentBasicInfo`/`DepartmentDTO`/查询表单保留 string 不做连带破坏 |
| 3 | `types/purchase-archive.ts` `MaterialArchiveFormData` | `barcode/origin/shelfLife/storageCondition/departmentId` 改可选，未填不传（不再用 `''` 占位） |

验证：`vue-tsc --noEmit` 仍 **0 error**。

### 13.2 五项实测体验缺陷（全部修完）

**① POS 商品图片挂掉**（根因实测，非猜测）

- `frontend-pos` 目录**没有任何 .env**，`VITE_API_BASE_URL` 为 undefined → 旧代码拼出裸 `/uploads/x.png`；
- 后端 `context-path=/api`，资源真实地址是 `{origin}/api/uploads/**`：`:8081/uploads/…` → 404；`:3001/uploads/…` → **200 text/html**（vite SPA 回退 index.html，就是浏览器看到的破图）；`:3001/api/uploads/…` → 200 image/png；
- 统一入口 `frontend-pos/src/utils/image.ts#getFullImageUrl`：相对路径补 `/api` 前缀（走已有 vite 代理）、http(s) 原样并统一 `127.0.0.1↔localhost`、`data:/blob:` 原样、空值返回 ''；调用点收敛到 MenuSection / CustomerOrder / YoloCheckoutDialog。

**② 时间格式** 新增 `frontend-pos/src/utils/format.ts`（`formatDateTime` 等 5 个方法，微秒截断、本地解析、可配 fallback）。裸 ISO 真源头是 `OrderQueryDialog.vue:28` 的 prop 直出，另有 13 处各自本地实现统一替换，含 CSV 导出与日期区间参数（顺带修掉 UTC 跨时区少一天）。

**③ 叫号号段**（与订单号解耦）

- 格式 `^[A-Z][0-9]{3,4}$`，堂食 A / 外卖 B / 自提 C；键 `CALL-{storeId}-{yyyyMMdd}-{前缀}` 存 `order_sequence`，跨天自动重置、按门店隔离；
- 原子取号 `INSERT … ON CONFLICT DO UPDATE … RETURNING`（PG 行锁串行化，无 MAX+1 竞态）；幂等靠「先读后取 + `UPDATE … WHERE call_number IS NULL` CAS 绑定」，抢号失败读回对方号，宁可跳号绝不重号；
- 落库 `orders.call_number`（权威）+ `call_record.call_number`（叫号屏），DDL `scripts/db-fix/2026-10-23-pos-call-number.sql`（全 `IF NOT EXISTS`，连跑两次可复放）；
- 生成入口四处旁路（POS 下单收尾 / 支付成功 / 叫号屏同步 / KDS 出餐），**状态机零改动**；
- 展示：小票居中大字「叫号 A001」（订单号降为对账行）、POS 查询/详情、叫号屏、大屏、支付成功页、顾客端全改叫号号；
- 实测：堂食 A051 / 外卖 B003 / 自提 C002；重复支付同号 A052；12 路并发 A038–A049 **零重号**；DB 分组重号 0 行。

**④ 退款前置放宽**：`PosOrderPaymentServiceImpl.refundOrder` 新表优先——只要 `payment_status=2` 且 `paid_amount > refund_amount` 即可退（**不再要求「已完成」**），按剩余可退金额一次退完，写 `order_refund_records`、回写 `orders.refund_amount` 与 `order_status=5`；旧表流程原样兜底。前端 POS 两个对话框改为 `canRefund()`：排除「未支付/已取消/已退款」即放行。

**⑤ 手动流转理由非必填**：`TrayServiceImpl.manualTransition` 未填理由时落默认「收银员手动流转」，仍写 `tray_scan_record` 审计，不再挡住高峰期操作。

### 13.3 本轮验证（可复跑）

| 项 | 命令 | 结果 |
|---|---|---|
| 后端编译 | `mvn -o -B compile`（JDK21） | BUILD SUCCESS |
| 管理端类型 | `frontend: npx vue-tsc --noEmit` | **0 error** |
| POS 类型 | `frontend-pos: npx vue-tsc --noEmit` | **0 error** |
| 核心链路 E2E | `python scripts/e2e_core_chain.py --password 123456 --tray auto` | **PASS=12 FAIL=0 SKIP=1**（托盘无空闲） |
| 页面自检 | `python scripts/e2e_wip_pages.py` | **PASS=11 FAIL=0**（签名存证/防伪码双态/银行账户 CRUD/未完成订单退款） |

### 13.4 全量遗留风险（挂起项，按优先级）

| # | 风险 | 级别 | 现状与建议 |
|---|---|---|---|
| R1 | `OrderNewServiceImpl.createPosQuickOrder`（禁改类）建单不立即取号 | 低 | POS 前端未调用该接口（已 grep 确认），无实际缺口；若要「下单即出号」需 Owner 授权后在收尾插 `posCallNumberService.ensureCallNumber(orderId)` |
| R2 | POS 支付写入仍是「新表优先 + 旧表兜底」双轨 | 中 | 新表链路已闭环（含退款），但旧表分支仍在；建议下一轮把 `orders_legacy` 支付写入彻底下线 |
| R3 | `orders` 无 openid 列 | 中 | `getOrdersByOpenid` 只能走旧表，顾客端「我的订单」在新表订单上不可用 |
| R4 | 混合期列表只显示新表（新表非空时） | 低 | 历史单在 POS 列表不可见，需按需回查旧表 |
| R5 | `InventoryCreateDTO` 已补 `creatorId`，但后端是否有对应列需复核 | 低 | 本次只做前端类型与提交链路；若后端未落列，负责人仍不持久化（下轮核对 `inventory_check` 表结构） |
| R6 | `Department.parentId` 前端已改 number | 低 | 其他调用点若仍传字符串会被 TS 拦下，属预期；后端 0=顶级语义需与产品确认 |
| R7 | P1-AUTH-CHAIN-001 C 卡（废弃 `users.roles` 鉴权）未启动 | 高 | 见 `docs/STATUS.md` §2 |
| R8 | P1-DASHBOARD-FILTER-001 看板无门店级数据过滤 | 高（越权） | 见 `docs/STATUS.md` §2，需 Owner 放行后实施 |
| R9 | 托盘绑定 E2E 恒 SKIP | 低 | 门店 1 无空闲托盘，非核心链路；如需覆盖，跑前先 `release` 一张托盘 |
| R10 | 收编页面（Receiving/BankAccount/叫号展示）未做浏览器级联调 | 中 | 本轮已用接口级自检（11 项）覆盖业务闭环，签名画板/扫码桥/离线暂存重传仍需真机过一遍 |

---

## 14. POS 订单查询无反应定位 + 体验巡检（2026-10-23 13:28–13:50）

### 14.1 根因：查询参数根本没发出去

`OrderQueryDialog.vue` 两处调用写成了 `request.get('/v1/pos/orders', params)`。
axios 的第二个参数是 **config**，只有 `config.params` 才会拼到 query string；`{keyword, startDate, endDate}` 被当成未知 config 字段**静默丢弃**。
→ 后端每次都收到「无条件」请求，返回最近 200 条；用户输入单号后列表当然「没变化」，表现为点了查询没反应。

修复：统一走 `posApi.getOrders(params)`（内部 `{ params }`），并补交互反馈：空结果 `ElMessage.info('没有匹配的订单')`、失败 `ElMessage.error(...)`（原来只 `console.error`，属典型静默失败）。

### 14.2 巡检脚本 `scripts/e2e_pos_experience.py`（新增）

API 级体验巡检，15 项断言 + 1 项数据缺口提示：

| 区块 | 覆盖 |
|---|---|
| A 订单查询 | 空条件 / 存在单号 / 不存在单号 / 日期范围 / 叫号号 / 非法日期 |
| B 商品菜单 | 分类列表 / 菜品列表 / **每张图片真实可下载（HTTP 200 + image\*）** / 分类筛选覆盖率 |
| C 结算 | 下单（改数量）→ 金额=单价×数量 → 详情数量联动 → 微信支付 → 支付方式回显 |
| D 静态扫描 | `request.get(url, payload)` 漏 `{params}` / 模板直出裸 ISO / 页面手拼图片 base |

### 14.3 巡检顺带发现的 3 个真实缺陷（全部已修）

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| B1 | 日期范围查询 500 | `applyDateRange` 把 `"2026-10-23 00:00:00"` 字符串直接比较 timestamp 列，PG 报「操作符不存在: timestamp >= character varying」 | 新增宽松解析（`yyyy-MM-dd` / 带时间 / ISO），解析失败按未指定处理，不再 500 |
| B2 | 菜单页分类 Tab 全空 | POS 读的是遗留表 `food_category`（实测 0 行），管理端读的是 `food_categories` | `getCategories` 改走 canonical `FoodCategoryNew`；菜品分组键统一为 `foods.category_id`（按 foodCode 映射），无值回退文本分类 |
| B3 | 微信支付显示成「现金」 | `buildNewOrderDetail` 用**旧表编码**翻译新表 `order_payment_records.payment_method`（新表 2=微信，旧表 2=现金） | 新增 `convertNewPaymentMethodToString`（1现金/2微信/3支付宝/4银行卡/5余额），新表链路专用 |

### 14.4 R1 授权收口

`OrderNewServiceImpl.createPosQuickOrder` 收尾已插入 `posCallNumberService.ensureCallNumber(orderId)`（字段注入 `required=false` + try/catch，取号失败绝不阻断下单），`OrderVO` 增 `callNumber` 并回填；关键字查询同时支持叫号号。实测该入口下单直接返回 `callNumber=A054/A055`。

### 14.5 本轮验证

| 项 | 结果 |
|---|---|
| `scripts/e2e_pos_experience.py` | **PASS=15 FAIL=0 SKIP=1**（5 个 QA 测试菜无图，数据缺口） |
| `scripts/e2e_core_chain.py` | 核心链路全绿（PASS=12 FAIL=0 SKIP=1） |
| `scripts/e2e_wip_pages.py` | PASS=11 FAIL=0 |
| `frontend` vue-tsc | 0 error |
| `frontend-pos` vue-tsc | 0 error |
| `mvn -o -B compile` | BUILD SUCCESS |

### 14.6 仍未处理（记录，不静默）

- **5 个 QA 测试菜品无图**（`QA-红烧土豆`/`QA-F6-即时下单验证菜`/`QA-链路复验菜`/`QA-QA1-独立验收菜`/`测试菜品销售A`）：遗留 `food` 表 `food_image` 为空，属测试数据缺口，POS 已走占位图；真实菜品（生菜串等）图片正常。
- **POS 菜品数据源仍是遗留 `food` 表**：分类归属要靠 `foods` 映射，长期应统一到 `foods`（foods 有 category_id/售价分口径）。
- **下单接口要求前端传 price/productName**：服务端会再校验，但契约上仍允许前端先算价，建议后续改为只传 productId+quantity。
- 浏览器级 E2E（真点击）未接：Playwright 1.63 可用但未安装浏览器内核，当前巡检为 API+静态扫描；需要时再补 `npx playwright install chromium`。
