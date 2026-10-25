# P3 实现清单 · 影响面 · 验收映射（v0.2 修订版，**未开始写业务代码**）

> 状态：**v0.2（2026-10-25，按 Owner 审查意见修订）**。本文只做计划。
> v0.1 → v0.2 的 4 处设计修正、3 项裁决落地、2 处补全，见 §9 变更记录。

## 0. 全局约定（P3 全程）

| 项 | 约定 |
|---|---|
| 写 `fund_flows` / `bank_accounts.balance` | **只能**经 `AtomicBalanceUpdater#addBalanceGuarded`（乐观锁 + 下限守卫）；禁止裸 SET；禁止先 select 再 update |
| 新断言 | 只增不删，编号接 **A23** 起；观测型一律 INFO 不进 TOTAL；判定型 `>0` 即红 |
| 白名单/基线 | **不引入**。若某断言在干净数据上必然红，先报告设计问题 |
| 权限 | 复用既有码（`finance:bank:view/import/recon/reconcile`、`finance:deposit-slip:*`）；**新增码先报告** |
| 前端 | Vue 3 + `views/finance` 既有习惯；路由 `meta.domain='finance'` + `roles`；**每项必须跑 Playwright E2E** |
| 数据 | 验收前 `python scripts/qa/seed_p3_fixture.py --reset`；**不手工造数** |
| 结构变更 | 本计划原定「零 DDL」，但 **A25 需要烧号账载体（见 §5）→ 需 Owner 单独放行** |

## 1. 第一项：财务对账工作台（前端）

**缺口**：R1–R10 已可用，财务只能用接口/SQL —— 「算对」还没变成「在用」。

### 1.1 实现清单
| # | 内容 | 文件 |
|---|---|---|
| 1.1 | API 层封装（跑批/差异/勾稽/摘要） | `frontend/src/api/finance-recon.ts`（新增） |
| 1.2 | 类型定义（与后端 VO 字段一一对应） | `frontend/src/types/finance-recon.ts`（新增） |
| 1.3 | 三区工作台页 | `frontend/src/views/finance/recon/ReconWorkbench.vue`（新增） |
| 1.4 | 摘要卡（笔数 / 金额 / 差异条数 / 差异金额 / 收敛提示） | `.../components/ReconSummaryCard.vue`（新增） |
| 1.5 | 队列表（状态标签、筛选、**默认 7 天窗**、服务端分页） | `.../components/ReconQueueTable.vue`（新增） |
| 1.6 | 详情面板（内部/外部并排 + 差异类型 + 建议动作 + 审计轨迹） | `.../components/ReconDetailPanel.vue`（新增） |
| 1.7 | 处置对话框（claim→resolve，`reason` 必填，WRITE_OFF/EXEMPT/REVERSE） | `.../components/ReconResolveDialog.vue`（新增） |
| 1.8 | **R8 service 层加可选 `from` / `to`**（只改查询条件，**不改接口签名**；DTO 增加 `from`/`to`，`ReconDifferenceMapper.xml` 增加 `AND create_time BETWEEN`） | `ReconDifferenceQueryDTO.java`、`ReconDifferenceServiceImpl`、`ReconDifferenceMapper.xml`（**小改既有**） |
| 1.9 | 路由注册 `/finance/recon` + 侧边栏入口 | `frontend/src/router/index.ts`（精确追加一行） |

> 1.8 说明：这是**对既有 R8 的小改动，不是新增接口**。原因：若纯前端过滤 7 天窗，等于把全量差异拉到浏览器再丢弃 —— 与「服务端分页」自相矛盾。

### 1.2 复用的后端接口
R1 `POST /recon-runs`、R2 `GET /recon-runs`、R3 `GET /recon-runs/{id}`、R5 `GET /recon-items`、R6/R7 `POST /recon-items/{id}/match|unmatch`、**R8 `GET /recon-differences`（+`from`/`to`）**、R9 `claim`、R10 `resolve`。

### 1.3 影响面
| 维度 | 内容 |
|---|---|
| 表 | **无 DDL、无新表**；R8 仅增加 WHERE 条件 |
| 接口 | **0 个新增**；R8 增加 2 个可选查询参数（向后兼容：不传即原行为） |
| 写路径 | 前端不直接写库；`resolve` 写 `recon_differences` + `finance_audit_logs`，后端事务保证 |
| 权限 | 读 `finance:bank:view`；写（跑批/认领/核销/勾稽）`finance:bank:recon`；**不新增码** |
| 事务边界 | R1 单事务；R9/R10 各自事务；前端「一次点击一次调用」 |
| 风险 | ① 差异池量大（真实可达上千）→ 服务端分页 + from/to 收窄；② **「无声更改」**：`reason` 必填、详情面板常驻 `resolution_by/time/remark` + 审计时间线 |

### 1.4 验收映射
| AC | 断言 | 方法 |
|---|---|---|
| **AC-46a**（新） | 摘要 `differenceCount` == `GET /recon-runs/{id}.differenceCount` | E2E 接口对拍 |
| **AC-46b**（新） | 摘要 `differenceAmount` == `SELECT SUM(diff_amount) FROM recon_differences WHERE run_id=?`（注意：不是 `difference_count` 的等价物，**必须单独算**） | E2E + SQL 对拍 |
| AC-47（新） | 「差异池 → 认领 → 核销 → 状态变化 → 审计出现」完整可走 | E2E |
| AC-48（新） | 未填 `reason` → 前端阻塞（**不发请求**，用网络监听断言 0 次调用） | E2E |
| AC-7/AC-13（既有） | 幂等与 claim→resolve 语义不回归 | 既有脚本 |

### 1.5 E2E 前置条件（**补全**）
- **当前种子数据 `recon_runs = 0`、`recon_differences = 0`**（已实测）→ 工作台没有可操作对象。
- 因此：`scripts/qa/e2e_recon_workbench.py` **第一步先调 R1 跑一次批**（固定账户 + 固定期间，用例内自建），断言 `run_id` 与差异条数 > 0，再进入 UI 断言；**不依赖 seed 预置差异**。
- 同时在 seed 脚本加 `--with-recon` 可选开关（跑一次 R1，供手工验收用），默认不跑（E2E 自己跑，避免双重跑批数据叠加）。

### 1.6 「较上次是否收敛」的**定义**（补全）
> **「上次」= 同账户、同期间范围内，`create_time` 早于当前 run 的最近一条 `recon_runs`（不限 status）**；比较指标为 `|SUM(diff_amount)|`。
> 字段名已按活库核实：`recon_runs` 用的时间列是 **`create_time`**（不是 `created_at`；同表另有 `started_at` / `finished_at` / `update_time`）。
- 无历史 run → 摘要卡显示「首次跑批（无可比基线）」，**不显示假趋势**；
- E2E 断言：连续跑两次 R1（第二次前先把某条差异 resolve 掉），断言第二次摘要卡的 `differenceAmount` 小于第一次且趋势文案为「差异缩小」；
- **不使用前端 localStorage**（会与后端真实状态不一致，且 E2E 不可复现）；不引入「结账周期」概念（P3 无结账模块）。

## 2. 第二项：门店对账视图 + 平台佣金/手续费可见

**缺口**：门店只能被动接收；手续费差异淹没在差异池里。

### 2.1 实现清单
| # | 内容 | 位置 |
|---|---|---|
| 2.1 | 门店当日视图接口 `GET /v1/finance/store-recon/daily?storeId&date`（只读） | 新增 `StoreReconController` |
| 2.2 | **手续费/佣金汇总接口** `GET /v1/finance/recon/channel-fee-summary?runId&from&to`（只读聚合，**按 `source_type` 分组**） | 新增 `ChannelFeeSummaryController` |
| 2.3 | 门店视图页（店长本店 / 财务可切店） | `views/finance/recon/StoreReconView.vue`（新增） |
| 2.4 | 工作台内渠道手续费卡（按渠道：笔数 / 手续费合计 / 与内部记录差额） | `.../components/ChannelFeePanel.vue`（新增） |

**手续费汇总的实现口径（修正点 4）**：
```sql
-- recon_differences.external_ref ↔ bank_transactions.external_txn_id
SELECT bt.source_type,
       COUNT(*)                        AS diff_count,
       COALESCE(SUM(bt.fee), 0)        AS fee_total_fen,
       COALESCE(SUM(rd.diff_amount), 0) AS diff_amount_fen
  FROM recon_differences rd
  JOIN bank_transactions bt ON bt.external_txn_id = rd.external_ref
 WHERE rd.type = 'CHANNEL_FEE' /* + runId / 时间窗过滤 */
 GROUP BY bt.source_type;
```

### 2.2 影响面
| 维度 | 内容 |
|---|---|
| 表 | **无 DDL**；只读 `deposit_slips`、`bank_transactions(fee, source_type)`、`recon_differences(CHANNEL_FEE)` |
| 接口 | **3 个只读**（门店日视图 1 + 手续费汇总 1 + 渠道明细 1） |
| join 依据 | `bank_transactions.external_txn_id` ↔ `recon_differences.external_ref`，按 `source_type` 分组 |
| 权限 | 门店视图复用 `finance:deposit-slip:view`；渠道汇总复用 `finance:bank:view`；**不新增码** |
| 数据范围 | 服务端按登录用户 `store_id` 强制过滤；**无 `store_id` 的用户直接 403**（不返回默认值/不返回空集） |
| 风险 | ① 越权是本期最大风险 → E2E 双账号对拍；② `external_ref` 可能为空（内部侧差异）→ JOIN 前必须 `rd.external_ref IS NOT NULL`，否则丢行 |

### 2.3 验收映射
| AC | 断言 |
|---|---|
| **AC-49**（新） | 店长 A 访问本店 → 200 且数据为 A 店；访问 B 店 → **403（越权，不是 404 资源不存在）**；E2E 断言状态码与提示文案分别匹配 |
| AC-50（新） | `channel-fee-summary` 的 `diff_count` / `fee_total` / `diff_amount` 逐字段等于对应 SQL 聚合 |

### 2.4 E2E 前置条件（**补全**）
- **当前种子的 2 张缴款单都在 `store_id = 1`**（已实测：slip 74/75 均为 store 1）→ 无法验证跨店隔离。
- **seed 脚本需改**：改为「**两个门店各一张缴款单**」（A 店平账、B 店短款），并同步给 B 店建一个店长测试账号或用现有 `qa_teller2` 绑定 B 店；`--reset` 后自动具备 P3-2 E2E 前置。
- 若 Owner 不希望改 seed 的默认行为，则加 `--two-stores` 开关，P3-2 E2E 固定带该开关执行（我会选「默认就两店」，因为更贴近真实连锁场景，改动即改 seed 一处）。

## 3. 第三项：调节表闭环收口（生成 → 提交 → 审核 → 导出）

### 3.1 实现清单
| # | 内容 | 位置 |
|---|---|---|
| 3.1 | 四类未达账项**下钻**（A/B/C/D 点击 → `reconciliation_items` 明细 + 引用侧原记录） | `StatementDetail.vue`（新增） |
| 3.2 | 四眼门禁 UI 可见（`submitted_by == 当前用户` 或 `ROLE_TELLER` → 审核按钮禁用 + 原因提示；**后端 409 仍为最终依据**） | `StatementDetail.vue` |
| 3.3 | 导出 CSV 按钮（复用既有 `export` 接口） + 可打印版 | `StatementPrintView.vue`（新增） |

### 3.2 影响面
| 维度 | 内容 |
|---|---|
| 表 | 无 DDL；写 `reconciliation_statements` 沿用既有事务与 V5/V8/**A19 形态门禁** |
| 接口 | **0 新增**（复用 S3–S9 + export） |
| 权限 | 生成/提交 `finance:bank:recon`；审核 `finance:bank:reconcile`（仅 FD） |
| 风险 | 打印版数字**必须取接口原值，禁止前端重算**；门禁前置只是体验优化，不得替代后端校验 |

### 3.3 验收映射
| AC | 断言 |
|---|---|
| AC-51（新） | 四类下钻条数/金额 == `reconciliation_items` 按 `category` 聚合 |
| AC-52（新） | 自审：前端按钮禁用 **且** 后端 409（两层都验） |
| AC-43/44/45（既有） | 汇总自洽 / V8 四眼 / 不调账 不回归 |

## 4. 第四项：总部资金总览看板（只读聚合）

### 4.1 实现清单
| # | 内容 | 位置 |
|---|---|---|
| 4.1 | `GET /v1/finance/overview/daily`（三段聚合，只读） | 新增 `FinanceOverviewController` |
| 4.2 | 看板页（三卡 + 门店表格，异常标红） | `views/finance/recon/FundOverview.vue`（新增） |

### 4.2 可见范围（裁决 3 落地）
- **店长（`STORE_MANAGER`，`data_scope='store'`）：仅本店**三段数据；
- **财务总监 / 财务经理 / 审计（`data_scope='all'`）：全部门店**；
- 实现：服务端按登录用户 `data_scope` 自动加 `store_id` 过滤；**复用 `finance:bank:view`，不新增权限码**；无 `store_id` 且无全局范围的用户 → 403。

### 4.3 影响面
| 维度 | 内容 |
|---|---|
| 表 | 无 DDL；只读 `bank_accounts` / `fund_flows` / `recon_runs` / `reconciliation_statements` |
| 接口 | **1 个只读** |
| 性能 | `fund_flows(account_id, business_date)` 索引可用；默认「今天 + 近 7 天」窗口 |

### 4.4 验收映射
| AC | 断言 |
|---|---|
| AC-53（新） | 三段数字**逐字段**与 SQL 聚合相等（店长视角 = 本店子集） |
| **A23（INFO）** | 「未关闭调节表最长挂账天数」> 7 天时观测输出，不进 TOTAL |

## 5. 第五项：技术债收口

### 5.1 BL-9 时区偏移（无 DDL）
| 项 | 内容 |
|---|---|
| 改动 | `CsvImportParser` 支持 `+08:00` / `Z`，统一换算 Asia/Shanghai；失败仍 `TXN_TIME_INVALID` |
| 影响面 | 只动导入解析器；不改表、不改接口 |
| 验收 | AC-54（新）：带偏移样本导入后 `txn_time` = 期望的 Asia/Shanghai 时刻；既有 AC-2 不回归 |

### 5.2 BL-5 号段烧号（**A24 INFO + A25 判定，A25 需 DDL 放行**）
| 断言 | 口径 | 级别 |
|---|---|---|
| **A24** | 当天号段「已取值 − 已落库条数」的差值（`order_sequence` 当前值 vs `fund_flows` 当天行数） | **INFO（观测，不阻断）** |
| **A25** | **烧号账**中「已登记但未落库」的行数 | **判定（>0 即红，进 TOTAL）** |
**A25 的前置：需要一个烧号账载体**（当前不存在）：
```sql
-- 方案（推荐）：新建表，与取号同事务写入
CREATE TABLE fund_flow_no_burn_ledger (
  ledger_id     BIGSERIAL PRIMARY KEY,
  seq_key       VARCHAR(32) NOT NULL,       -- 如 FLOW20261025
  seq_value     BIGINT      NOT NULL,       -- 被消耗的号
  reason        VARCHAR(64) NOT NULL,       -- 业务废弃原因（取号后回滚/校验失败…）
  flow_id       BIGINT      NULL,          -- 若最终落库则回填
  created_by    VARCHAR(64) NULL,
  create_time   TIMESTAMP   NOT NULL DEFAULT now()
);
```
- **这是本计划唯一的 DDL 项** → **需 Owner 单独放行**；不放行则：A24 保持 INFO、**不加 A25**、历史洞不追溯（按你的裁决 2 的 fallback 分支执行）。
- **Owner 已放行（2026-10-25），执行约束如下（P3-5 落地时逐条对照）**：
  1. 建表 `fund_flow_no_burn_ledger`，字段至少含 **`flow_no` / `reason` / `created_by` / `create_time` / `archived_flag`**（推荐再加 `seq_key`、`seq_value`、`flow_id` 便于回溯）；
  2. **写入时机**：取号成功后、流水插入失败时（唯一键冲突 / 乐观锁耗尽 / 业务校验失败），在 **catch 块**写一条烧号账，`reason` 写明失败原因；**取号本身失败不写**（没消耗号，不算烧号）；
  3. **归档**：超过 **90 天**可由定时任务置 `archived_flag = true`，**不物理删除**；
  4. `schema_guard.py` 白名单加 `fund_flow_no_burn_ledger`（该白名单只用于「表存在性检查」，不是数据白名单/断言豁免）；
  5. **A25 判定 SQL**：`SELECT COUNT(*) FROM fund_flow_no_burn_ledger WHERE archived_flag = false;` → **>0 即红**（进 TOTAL）；
  6. A24 保持 **INFO**（只报当天号段取值 − 已落库条数，不阻断）。

## 6. 执行顺序与验收门

| 阶段 | 内容 | 完成门 |
|---|---|---|
| P3-1 | 工作台前端 + R8 from/to | AC-46a/46b/47/48 + E2E + consistency 全绿 |
| P3-2 | 门店视图 + 手续费汇总（3 只读接口）+ seed 两店改造 | AC-49/50 + E2E + 越权 403 对拍 |
| P3-3 | 调节表闭环 | AC-51/52 + E2E + 打印页字段对拍 |
| P3-4 | 资金总览 | AC-53 + A23 + E2E 逐字段对拍 |
| P3-5 | BL-9 / BL-5 | AC-54 + A24（+A25 视放行） |

每阶段收尾：`consistency_check.py` **TOTAL FAIL=0 / exit 0**、`ui_e2e_payment.py` 7/7 不破、`ahead=0`。

## 7. 不在 P3 范围
期末结账/反结账、退款侧勾稽（TD-31）、**BL-2 三处发号器收敛**（需单独放行）、恢复 `fixture-reverse`。

## 8. 汇总表（修订后）

| 阶段 | 表变更 | 新接口 | 新权限码 | 新 AC | 新断言 | E2E |
|---|---|---|---|---|---|---|
| P3-1 | 无 | **0 个（R8 加 `from`/`to` 查询参数）** | 无 | AC-46a/46b/47/48 | 无 | `e2e_recon_workbench.py` |
| P3-2 | 无 | **3 个只读**（门店日视图 / 手续费汇总 / 渠道明细） | 无 | AC-49/50 | 无 | `e2e_store_recon.py` |
| P3-3 | 无 | 0 | 无 | AC-51/52 | 无 | `e2e_statement_flow.py` |
| P3-4 | 无 | 1 个只读 | 无 | AC-53 | A23(INFO) | `e2e_fund_overview.py` |
| P3-5 | **A25 需烧号账表（待放行）** | 0 | 无 | AC-54 | A24(INFO) + A25(判定，待放行) | 导入 + 号段实测 |

## 9. v0.1 → v0.2 变更记录

| # | 审查意见 | 处理 |
|---|---|---|
| 1 | AC-46 拆条数与金额 | 拆为 **AC-46a**（count 对拍）/ **AC-46b**（`SUM(diff_amount)` 对拍），验收映射表已拆两行 |
| 2 | A24 改级 + 新增 A25 | **A24 = INFO**（只报差值不阻断）；**A25 = 判定（烧号账未落库行数 >0 即红）**；A25 需烧号账表 → **DDL 待放行**，不放行则不加 A25 |
| 3 | 7 天窗与「不新增接口」矛盾 | 1.8 新增「R8 service 加可选 `from`/`to`，不改签名」；总览表 P3-1「新接口」列改为「0 个（R8 加查询参数）」 |
| 4 | 手续费汇总需 join `source_type` | 新增 `GET /v1/finance/recon/channel-fee-summary`；总览表 P3-2 改「3 个只读」；§2.1 写明 join 口径 |
| 5 | 裁决：AC 编号 | AC-46～AC-54 将并入设计稿 §8（**待 P3-1 完成时一并提交**）；**AC-49 明确「越权 403，不是资源不存在」** |
| 6 | 裁决：看板可见范围 | 店长按 `data_scope='store'` 只看本店，财务/审计看全部；复用 `finance:bank:view`，**不新增权限码** |
| 7 | 补全：E2E 前置 | **实测 `recon_runs=0`/`recon_differences=0`** → P3-1 E2E 自己先跑 R1；seed 加 `--with-recon` 可选开关 |
| 8 | 补全：seed 跨店 | **实测两张缴款单都属 `store_id=1`** → seed 改为「两店各一张」（A 店平账 / B 店短款） |
| 9 | 补全：「上次」定义 | 定义为「同账户同期间、`created_at` 更早的最近一条 `recon_runs`」，比较 `|SUM(diff_amount)|`；无历史则显示「首次跑批」；**不用 localStorage、不引入结账周期** |