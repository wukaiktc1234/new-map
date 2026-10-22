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
| B | 后端 ServiceImpl 纯 Mockito 单测 | 进行中 | **15 个类 / 173 个用例全绿**，每类一提交 |
| C | 前端 TypeScript 报错清洗 | **本轮收口**（仅 clean 文件） | **145 → 93（−52，0 新增）**，Lead 独立复跑确认 93；基线 62 文件里 52 个带 Owner WIP 不可碰 |
| A | API 契约静态比对报告 | **本轮收口** | `docs/night-patrol/api-contract-diff.md`（232 行）：2057 端点 / 944 调用点 / 配对 898（95%），发现 58 条（高危 14 / 中低 16 / 类型精度 10 / 待确认 8） |
| D | 死代码 / 废弃注释清理 | 进行中 | 已删 2 个零引用 util 类（148 行），`mvn -o compile` 绿 |

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
| B-15 | 本次 | `InventoryDeductionServiceImplTest`（14 用例：模式A 可用量回退链/扣完置 used、模式B 配方折算+JSON 回写、批次跨码消耗、内存记录） | 14/0/0 |

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

## 4. 待办队列（按优先级）

1. B：继续补 ServiceImpl Mockito 测试（每类一提交，`mvn -o test -Dtest=<类名>` 绿了才提交）。
2. C：继续压 TS 报错（仅 clean 文件 + clean 共享类型间接消错）。
3. D：继续扫零引用 util / 废弃常量，安全删除后过编译。
4. 池子掏空后：输出《代码质量与性能优化建议报告》到 `docs/night-patrol/`。

## 5. 遗留观察（只报不改，属核心业务或需 Owner 裁决）

- **契约层（A 报告，需 Owner 决策 2 条）**：① `GET /v1/roles/list` 前端传 `status=1/0`、后端要 `active/inactive`（`RoleController.java:150-155`）→ 筛选静默失效；② `JacksonConfig.java:51-54` 全局 Long→String 可能让 MyBatis-Plus `Page.total/size/current` 变字符串，与前端 number 类型冲突 —— 需实抓一次响应体才能定级（Q1）。

- `DailySettlementServiceImpl.buildQueryWrapper`：`currentUser == null` 时提前 return，**日期/状态/门店筛选全部失效**（已用特征化测试固化现状）。
- `DiningTableNewServiceImpl.update`：`updateById` 后直接 `selectById(...).getStoreId()`，回查为空会 NPE。
- `InventoryWarningRuleServiceImpl.getWarningRulePage`：`category` 参数被静默忽略（代码注释自认）。
- 前端 52 个报错文件带 Owner WIP，本轮**一律未动**，其 TS 报错需 Owner WIP 落定后再修。

## 6. 交接说明

- 分支提交列表：`git log --oneline ai-night-patrol-1..ai-night-chore-1`
- 未提交在途改动：`git status --porcelain` 中除 Owner WIP 外仅含队友正在编辑的前端 clean 文件。
- push：`git push origin ai-night-chore-1`（独立夜间分支，不影响 master；本轮结束时已按交接条款推送）。