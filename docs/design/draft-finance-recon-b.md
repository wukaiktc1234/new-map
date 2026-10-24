# DRAFT-FIN-RECON-B：财务自动勾稽对账 + 现金交款单闭环 设计草案

> **维护人**：数据/财务轨（fund-500）
> **创建日期**：2026-10-24
> **性质**：**设计草案（DRAFT）** —— 只含设计，不含代码、不含 DDL、不改配置
> **范围**：§2 自动勾稽/对账、§3 现金交款单闭环
> **不在本文件**：报表/预算/税、应付应收的既有流程（另有设计）

---

## 0. 阅读约定

1. **字段名严格用现状表**：`bank_accounts` / `fund_flows` / `order_payment_records` / `orders` / `pos_shifts` / `finance_records` 的列名一律照抄现状，不自造同义列。
2. **新增结构一律标「草案表」**：`bank_transactions`、`recon_runs`、`recon_items`、`recon_differences`、`deposit_slips` 在活库中**均不存在**（2026-10-24 实测 `to_regclass` = none），本文给出的是字段级设计，不是已建表。
3. **待定统一编号 TD-xx**（集中在 §4），正文出现处写「待定：xxx（TD-xx）」。**不确定就标待定，不臆造业务规则。**
4. 验收标准写成**可测断言**（能写成 SQL / 脚本判断），编号 `V2-x` / `V3-x`；边缘案例编号 `E2-x` / `E3-x`。
5. 与既有防线的衔接：`scripts/qa/consistency_check.py`（A1–A5 + A6 信息性）、`scripts/qa/ui_e2e_payment.py`、`scripts/qa/schema_guard.py`。本设计**不删除**任何既有断言，只新增。

### 0.1 已核实的现状约束（设计必须遵守）

| 约束 | 现状 | 对本设计的影响 |
|---|---|---|
| 支付方式编码单轨 | `order_payment_records.payment_method`：0未记录 / 1现金 / 2微信 / 3支付宝 / 4银行卡 / 5余额 | 对账的「现金」判定只认 1，**0 不得当现金** |
| 资金流水挂订单 | `fund_flows.order_id` 已落地（varchar） | 内部侧匹配首选 order_id，不靠备注字符串 |
| 门店隔离 | `bank_accounts.scope`(GLOBAL/STORE) + `store_id` | 对账批次必须按账户 × 门店切片 |
| 余额原子更新 | `bank_accounts.balance`（分）+ `version` 乐观锁 | 冲正/补账必须走同一套余额更新，不许直接 SET |
| 审计 | `fund_flows.create_user_id` / `created_by` | 补账用 `create_user_id=0` + `created_by='system'/'system-backfill-*` |
| 流水状态 | `fund_flows.status` 1待处理/2已完成/3已取消，默认已完成 | 对账只纳入 `status=2`，取消态不参与匹配 |
| 现金班次 | `pos_shifts`：`closing_cash` / `total_amount` / `status` / `employee_*`（19 行存量） | 交款单的「应交金额」来源 |

---

# §2 自动勾稽 / 对账

## 2.1 目标与非目标

**目标**
1. 把「系统记的账」与「银行实际到账」按日勾稽，差异**自动分池**、人工**认领核销**，全过程留痕。
2. 对账结果可复算、可重跑、**幂等**（同日多跑不产生重复差异）。
3. 差异处理只允许「冲正 + 新增反向流水」，**不物理删除、不改历史余额**。

**非目标（本期不做）**
- 不做银行 API 直连（本期只支持导入式 `bank_transactions`，TD-01）。
- 不做多币种（`bank_accounts.currency` 现状恒 CNY，TD-02）。
- 不做自动开票/自动记账（凭证侧由 `AutoVoucher` 体系负责）。

## 2.2 数据两侧定义

### 2.2.1 内部侧（系统账）

**A1 资金流水（收入侧）**：`fund_flows`
```
筛选：deleted=0 AND status=2 AND flow_direction=1
标识：flow_id（PK）、flow_no（FFyyyyMMddNNNN）
金额：amount（分）
时间：business_date（date）+ create_time（timestamp）
归属：account_id（→ bank_accounts，决定门店/币种）、order_id（可空）
分类：flow_category（1=销售收款 … 5=内部转账 6=其他 7=采购付款退回）
```

**A2 支付记录（渠道侧）**：`order_payment_records`
```
筛选：deleted=0
标识：payment_id、order_id、transaction_no（渠道流水号，可空）
金额：payment_amount（分）
方法：payment_method（0未记录/1现金/2微信/3支付宝/4银行卡/5余额）
时间：payment_time
```

**两侧关系**：一笔订单可能有多条支付记录（混合支付），对应**一条或多条** SALE 流水；对账时以「订单 + 支付方式」为聚合粒度（2.3 L2）。

### 2.2.2 外部侧（银行账）—— 草案表 `bank_transactions`

| 字段 | 类型（分/时间口径） | 说明 |
|---|---|---|
| id | bigint PK | 自增 |
| import_batch_id | bigint | 导入批次（同批可整批回滚） |
| account_id | bigint | 关联 `bank_accounts.account_id`（**账户级对账的锚点**） |
| external_txn_id | varchar(64) | 银行流水号/回单号（精确匹配首选键） |
| direction | smallint | 1 收入 / 2 支出（与 `fund_flows.flow_direction` 同口径） |
| amount | bigint | **分**（导入时必须换算，禁止元混入） |
| txn_time | timestamp | 银行记账时间 |
| counterparty_name | varchar(100) | 对方户名（模糊匹配用） |
| counterparty_account | varchar(64) | 对方账号 |
| summary | varchar(255) | 摘要/用途 |
| balance_snapshot | bigint | 银行回单余额（分，可空，用于连续性校验） |
| raw_hash | varchar(64) | 原始行哈希（**导入级去重**：同 hash 不重复导入） |
| create_time / deleted | — | 与项目其余表一致 |

> 待定：`bank_transactions` 的表名与来源（手工 CSV / 银企直连 / 银行邮件解析）由 Owner 裁决（TD-01）；若选择复用既有 `bank_payment_records`（已存在同名 Controller），字段需重新对齐（TD-03）。

## 2.3 匹配策略分级

匹配在**同一账户 + 同一方向（收入）+ 同一业务日窗口**内进行，粒度优先级由高到低：

### L1 精确匹配（AUTO_MATCHED，置信度 1.00）
```
A2.transaction_no = B.external_txn_id
且 A1.amount = B.amount
且 同一 account_id
```
命中即绑定，不参与后续层级。

### L2 组合匹配（AUTO_MATCHED，置信度 0.90）
```
|A1.amount - B.amount| = 0
AND B.txn_time BETWEEN A1.business_date 00:00 - N 分钟 AND A1.business_date 23:59 + N 分钟   （默认 N=120，TD-04）
AND A1.account_id = B.account_id
```
**候选唯一**才自动绑定；候选 >1（同一账户同一天多笔同额）→ 降级为 `MATCHED`（**待人工确认**，不自动核销），并生成 `AMBIGUOUS` 差异类型。

### L3 模糊匹配（只给建议，永不自动核销 → `MATCHED` 候选）
```
|A1.amount - B.amount| <= max(1 分, 0.5% * A1.amount)           -- 容忍渠道手续费差，TD-05
AND B.txn_time BETWEEN A1.business_date - 1 天 AND A1.business_date + 3 天
AND similarity(A1.counterparty_name, B.counterparty_name) >= 0.6  -- 拼音/去空格/去"有限公司"后比较
```
结果写 `recon_items.candidate_refs`，状态 `UNMATCHED` + `suggested_external_id`，由人确认后才变 `MANUAL_MATCHED`。

### 一对一 / 一对多 / 多对一

| 形态 | 场景 | 处理规则 |
|---|---|---|
| 1:1 | 单笔订单单渠道支付 ↔ 一笔银行到账 | L1/L2 直接绑定 |
| 1:N（内部 1 → 外部多） | 一次收款被打款方拆成多笔到账（少见） | 允许「合并匹配」：`SUM(B.amount) = A1.amount` 且笔数 ≤ 3、窗口 ≤ 3 天；合并匹配**只允许人工确认**，自动匹配禁止 |
| N:1（内部多 → 外部 1） | 平台 T+1 汇总入账（微信/支付宝合并打款） | 允许「汇总匹配」：`SUM(A1.amount) = B.amount`，且所有 A1 同 account_id、同 business_date 区间；命中后 `recon_items` 生成 N 条 item 指向同一 external |
| N:M | 平台日汇总 + 手续费差额 | 禁止自动匹配，直接落 `EXCEPTION`（渠道手续费差），走人工 |

> 关键裁决：**自动匹配只允许 1:1**。任何 1:N / N:1 / N:M 都必须人工确认（TD-06 可改）。

## 2.4 匹配结果状态机

**recon_items.subject_status**（内部/外部主体的匹配状态）

```
UNMATCHED ──L1命中──▶ AUTO_MATCHED ──人工复核(可选)──▶ MANUAL_MATCHED
    │                     │
    │                     └──复核不通过──▶ UNMATCHED（解绑，回到池子）
    ├──L2唯一命中──▶ AUTO_MATCHED
    ├──L2多候选──▶ MATCHED(待确认) ──确认──▶ MANUAL_MATCHED
    ├──L3建议命中──▶ UNMATCHED(suggested_external_id 非空)
    └──人工认定不可对账──▶ EXCEPTION
```

**五态定义**

| 状态 | 含义 | 是否计入「已勾稽金额」 |
|---|---|---|
| UNMATCHED | 未匹配（可能带建议） | 否 |
| MATCHED | 已找到候选但**待人工确认**（多候选/模糊命中） | 否（**避免虚增勾稽率**） |
| AUTO_MATCHED | 系统自动确认 | 是 |
| MANUAL_MATCHED | 人工确认（含 1:N/N:1 合并） | 是 |
| EXCEPTION | 显式排除（如内部测试流水、已冲正） | 否，但计入「已处理」 |

> 规则：`MATCHED` 不计入勾稽完成率是刻意设计——只有「系统确信」或「人确认过」才算。

## 2.5 差异类型枚举（`recon_differences.difference_type`）

| code | 名称 | 判定 | 典型动作 |
|---|---|---|---|
| AMOUNT_DIFF | 金额差 | 已关联但仍 `A.amount <> B.amount` | 核销差额 / 冲正差额 |
| TIME_DIFF | 时间差 | 金额一致但 `txn_time` 超出窗口（跨日到账） | 允许「跨日挂账」，不改业务日 |
| ONE_SIDED_INTERNAL | 单边账（只有内部） | A 有记录、B 无对应（已过窗口） | 挂待查；超 T+N 天升级短款 |
| ONE_SIDED_EXTERNAL | 单边账（只有外部） | B 有到账、A 无对应 | 疑似未入账/他人打款，人工认领 |
| DUPLICATE | 重复入账 | 同一 `order_id`+同金额出现 >1 条 SALE 流水，或同 `external_txn_id` 重复导入 | 冲正多余那条（反向流水） |
| CHANNEL_FEE | 渠道手续费差 | `B.amount = A.amount - fee`（fee>0） | 差额记财务费用，**不放宽金额匹配** |
| AMBIGUOUS | 多候选 | L2 命中 >1 | 人工二选一 |

> 差异类型是**枚举（code）**，不是自由文本；中文名放字典表或前端映射，不进 DB 主字段。

## 2.6 差异处理流程

```
[1] 对账跑批 ──▶ 生成 recon_items（全量主体）+ recon_differences（仅差异）
        │
[2] 差异池（UI：待处理差异）
        │  出纳/会计「认领」──▶ recon_differences.assignee_id + status=ASSIGNED
        │
[3] 处理动作（三选一）
        ├─ 核销 WRITE_OFF：认定差异可接受（如手续费），写 recon_differences.resolution=WRITE_OFF
        │                  + 关联生成一条费用/其他支出流水（可选，TD-07）
        ├─ 冲正 REVERSE：生成**反向流水**（fund_flows.flow_direction 反向、flow_category 同类、
        │                 remark 带 [reconReverseOf=<flow_no>]），原流水置 status=3（已取消）
        │                 —— **绝不 DELETE**，绝不改历史 amount
        └─ 忽略 EXEMPT：认定无需处理（内部测试单等），必须填 reason 且审计
        │
[4] 关闭 ──▶ recon_differences.status=CLOSED + resolution_by/resolution_time
        │
[5] 审计留痕 ──▶ finance_audit_logs（既有表）：who / when / 前后值 / 依据单据号
```

**硬规则**
- 冲正流水必须**新生成**，`balance_after` 由余额原子更新计算，不得手填。
- 已 `CLOSED` 的差异不可再改，只能新建一条「重开」记录（`reopen_of=<difference_id>`），保证审计链单向。
- 任何处理动作都要能追溯到 `recon_run_id`。

## 2.7 对账批次 `recon_runs` 与幂等

**批次键**：`run_key = 'RECON-' || account_id || '-' || business_date`（唯一索引）

**幂等规则**
| 场景 | 行为 |
|---|---|
| 同日同账户首次跑 | 创建 run（status=RUNNING）→ 生成 items/差异 → status=FINALIZED |
| 同日同账户再跑（已 FINALIZED） | **不新建** run；返回既有 run + `"reused": true`；不重复生成差异（HTTP 200，不报错） |
| 已 FINALIZED 但要求重算 | 必须显式 `?force=true` 且需 `finance:recon:rebuild` 权限 → 新建 `rebuild_of=<run_id>` 的 run（**旧 run 保留**，不覆盖） |
| 跑批中途失败 | run 保持 RUNNING，超过 30 分钟自动置 FAILED（定时任务），重跑生成新的 run；**部分写入的 items 按 run_id 级联清理**（仅未认领的） |
| 差异去重 | `recon_differences` 唯一键 `(run_id, difference_type, internal_ref, external_ref)`；同 run 内重复插入被吞 |

> 补充：**跨日到账**不新建 run，而是在原 `business_date` 的 run 里以 `TIME_DIFF` 记录，避免「一天两个 run」导致对账率漂移（TD-08）。

## 2.8 关键表结构（草案，字段级）

### 2.8.1 `recon_runs`
| 字段 | 类型 | 约束/说明 |
|---|---|---|
| run_id | bigint PK | 自增 |
| run_key | varchar(64) | UNIQUE，`RECON-<account_id>-<yyyyMMdd>` |
| account_id | bigint | → `bank_accounts.account_id`；NOT NULL |
| store_id | bigint | 冗余自 bank_accounts（STORE 账户才有值，GLOBAL 为 null） |
| business_date | date | 对账业务日 |
| status | varchar(16) | RUNNING / FINALIZED / FAILED |
| internal_count / internal_amount | int / bigint | 内部侧条目数 / 合计（分） |
| external_count / external_amount | int / bigint | 外部侧条目数 / 合计（分） |
| matched_count / matched_amount | int / bigint | 已勾稽（AUTO+MANUAL）数/额 |
| difference_count | int | 差异条数 |
| rebuild_of | bigint | 指向被重算的 run_id（可空） |
| started_at / finished_at | timestamp | |
| operator_id | bigint | 触发人；系统定时为 0 |
| create_time / update_time | timestamp | |

### 2.8.2 `recon_items`
| 字段 | 类型 | 说明 |
|---|---|---|
| item_id | bigint PK | |
| run_id | bigint | → recon_runs，NOT NULL |
| side | varchar(8) | INTERNAL / EXTERNAL |
| internal_ref | varchar(64) | INTERNAL 侧填 `flow_no`（SALE 流水号）；EXTERNAL 侧空 |
| external_ref | varchar(64) | EXTERNAL 侧填 `external_txn_id` 或 `bank_transactions.id`；INTERNAL 侧空 |
| payment_methods | varchar(16) | 内部侧：该订单涉及的支付方式集合（如 `1,2`，来自 order_payment_records） |
| amount | bigint | 分 |
| txn_time | timestamp | |
| counterparty_name | varchar(100) | |
| subject_status | varchar(16) | UNMATCHED / MATCHED / AUTO_MATCHED / MANUAL_MATCHED / EXCEPTION |
| match_level | varchar(8) | L1 / L2 / L3 / MANUAL / null |
| match_group | varchar(64) | 合并匹配分组键（1:N / N:1 时相同），null=1:1 |
| suggested_external_ref | varchar(64) | L3 建议 |
| confidence | numeric(4,3) | 0~1 |
| create_time / update_time | timestamp | |

> UNIQUE `(run_id, side, COALESCE(internal_ref,''), COALESCE(external_ref,''))` —— 防同一主体重复落 item。

### 2.8.3 `recon_differences`
| 字段 | 类型 | 说明 |
|---|---|---|
| difference_id | bigint PK | |
| run_id | bigint | NOT NULL |
| difference_type | varchar(24) | AMOUNT_DIFF / TIME_DIFF / ONE_SIDED_INTERNAL / ONE_SIDED_EXTERNAL / DUPLICATE / CHANNEL_FEE / AMBIGUOUS |
| internal_ref / external_ref | varchar(64) | 至少一个非空 |
| amount_internal / amount_external | bigint | 分 |
| diff_amount | bigint | `amount_external - amount_internal`（分） |
| status | varchar(16) | OPEN / ASSIGNED / RESOLVING / CLOSED / EXEMPT |
| resolution | varchar(16) | WRITE_OFF / REVERSE / EXEMPT |
| resolution_remark | varchar(500) | 必填（CLOSED 时） |
| reverse_flow_no | varchar(32) | 冲正产生的反向流水号（→ fund_flows.flow_no） |
| assignee_id / assigned_at | bigint / timestamp | 认领人 |
| resolution_by / resolution_time | bigint / timestamp | |
| reopen_of | bigint | 重开来源 |
| create_time / update_time | timestamp | |

> UNIQUE `(run_id, difference_type, internal_ref, external_ref)`；索引 `(status, assignee_id)` 支撑差异池列表。

## 2.9 状态迁移图（文字版）

**run**：`RUNNING ──跑完──▶ FINALIZED`；`RUNNING ──超时/异常──▶ FAILED`；`FINALIZED ──force 重算──▶ (新 run) RUNNING`（旧 run 不动）。

**item**：见 2.4 五态迁移；禁止 `EXCEPTION → AUTO_MATCHED`（只能人工 `MANUAL_MATCHED`）。

**difference**：`OPEN ──认领──▶ ASSIGNED ──处理中──▶ RESOLVING ──成功──▶ CLOSED`；`ASSIGNED/RESOLVING ──豁免──▶ EXEMPT`；`CLOSED ──重开──▶ (新记录) OPEN`（旧记录保留 `reopen_of` 反向引用）。

## 2.10 接口草案（REST，前缀 `/v1/finance`）

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | POST | `/v1/finance/recon-runs` | 触发跑批：body `{accountId, businessDate, force?}` → 201/200（幂等返回 `reused:true`） |
| 2 | GET | `/v1/finance/recon-runs` | 列表：`?businessDate=&accountId=&status=&page=&size=` |
| 3 | GET | `/v1/finance/recon-runs/{runId}` | 详情（含汇总 + 差异计数 + 勾稽率） |
| 4 | GET | `/v1/finance/recon-items` | item 列表：`?runId=&subjectStatus=&side=&keyword=` |
| 5 | GET | `/v1/finance/recon-differences` | 差异池：`?status=&type=&runId=&assignee=` |
| 6 | POST | `/v1/finance/recon-differences/{id}/claim` | 认领（body 空）→ ASSIGNED |
| 7 | POST | `/v1/finance/recon-differences/{id}/resolve` | 处理：`{resolution: WRITE_OFF\|REVERSE\|EXEMPT, remark, reverseFlowNo?}` |
| 8 | POST | `/v1/finance/recon-items/{id}/match` | 人工匹配：`{externalRef, groupKey?}`（支持 1:N/N:1） |
| 9 | POST | `/v1/finance/recon-items/{id}/unmatch` | 解绑（仅 MANUAL_MATCHED → UNMATCHED） |
| 10 | POST | `/v1/finance/bank-transactions/import` | 导入外部流水（multipart CSV）→ 返回 batch_id + 去重统计（TD-01） |
| 11 | GET | `/v1/finance/recon-runs/{runId}/export` | 导出对账明细（xlsx） |

**响应统一**：`{code:0, message, data, timestamp}`（沿用项目 Result 结构）；分页沿用 MyBatis-Plus `records/total/current/size`。

## 2.11 页面线框

### 2.11.1 对账工作台 `/finance/recon`
```
┌────────────────────────────────────────────────────────────────────────────┐
│ 财务对账                                    [导入银行流水] [发起对账]      │
├────────────────────────────────────────────────────────────────────────────┤
│ 业务日 [2026-10-24 ▾]  账户 [现金户/工行 ****1234 ▾]  状态 [全部 ▾]  [查询]│
├────────────────────────────────────────────────────────────────────────────┤
│ 内部 617 笔 / 7,823.30 元    外部 615 笔 / 7,801.30 元    勾稽率 99.6%      │
│ 差异 3 条：金额差 1 · 单边(内) 1 · 手续费 1                                 │
├──────────────┬──────────────┬──────────────┬──────────────┬───────────────┤
│ 类型         │ 笔数         │ 金额差       │ 状态         │ 操作          │
├──────────────┼──────────────┼──────────────┼──────────────┼───────────────┤
│ 金额差       │ 1            │ -0.30        │ 待认领       │ [认领][详情]  │
│ 单边账(内)   │ 1            │ 22.00        │ 处理中(@张三)│ [冲正][豁免]  │
│ 渠道手续费差 │ 1            │ -1.50        │ 待认领       │ [认领][详情]  │
└──────────────┴──────────────┴──────────────┴──────────────┴───────────────┘
```

### 2.11.2 差异详情抽屉
```
┌───────────────────────────────────────────────┐
│ 差异 #1024  类型：金额差          状态：待认领 │
├───────────────────────────────────────────────┤
│ 内部：FF202610240001  22.00 元  10:12:03      │
│ 外部：TXN-88213       21.70 元  10:12:41      │
│ 金额差：-0.30 元（-30 分）                     │
│ 建议：渠道手续费（相似度 0.92）                │
├───────────────────────────────────────────────┤
│ 处理方式： ( )核销  ( )冲正  ( )豁免           │
│ 说明：[必填                              ]     │
│ 关联流水号：[FF_______________]（冲正时必填）  │
│                        [取消]  [提交处理]      │
└───────────────────────────────────────────────┘
```

## 2.12 权限点

| 权限 | 用途 |
|---|---|
| `finance:recon:view` | 查看对账工作台/明细/差异池 |
| `finance:recon:run` | 发起对账跑批 |
| `finance:recon:rebuild` | force 重算（生成新 run） |
| `finance:recon:claim` | 认领差异 |
| `finance:recon:resolve` | 核销/冲正/豁免 |
| `finance:recon:match` | 人工匹配/解绑 |
| `finance:recon:import` | 导入银行流水 |
| `finance:recon:export` | 导出对账明细 |

> 沿用项目 `finance:<domain>:<action>` 命名；数据权限：`scope=STORE` 账户仅本店可见（复用 `bank_accounts.store_id`）。

## 2.13 验收标准（可测断言）

| 编号 | 断言（可直接写成 SQL / 脚本） |
|---|---|
| V2-1 | 同日同账户跑两次 `POST /recon-runs`：`SELECT count(*) FROM recon_runs WHERE run_key=...` = **1**，且第二次响应 `reused=true` |
| V2-2 | 同一 run 内差异无重复：`SELECT count(*) FROM (SELECT difference_type,internal_ref,external_ref,count(*) c FROM recon_differences WHERE run_id=? GROUP BY 1,2,3 HAVING c>1) t` = 0 |
| V2-3 | L1 精确匹配：构造 `transaction_no='TXN-X'` 与 `external_txn_id='TXN-X'`，断言 item `subject_status='AUTO_MATCHED'` 且 `match_level='L1'` |
| V2-4 | 多候选不自动确认：同账户同日两笔等额内部流水 + 一笔外部 → 两条 item 均为 `MATCHED`（不是 AUTO_MATCHED），difference_type 含 `AMBIGUOUS` |
| V2-5 | 自动匹配只允许 1:1：构造 N:1 场景（两笔内部 → 一笔外部），断言**不会**出现两条 item 均 `AUTO_MATCHED` 指向同一 external_ref |
| V2-6 | 冲正不删数据：对 `FF202610240001` 冲正后，`SELECT count(*) FROM fund_flows WHERE flow_no='FF202610240001'` 仍 =1（status=3），且新增一条 `remark LIKE '%[reconReverseOf=FF202610240001]%'` 的反向流水，方向相反、金额相等 |
| V2-7 | 余额一致：冲正前后 `bank_accounts.balance` 变化量 = 反向流水 amount（分），且 `balance_after` = 冲正后余额 |
| V2-8 | 勾稽率口径：`matched_amount` 只累加 AUTO_MATCHED + MANUAL_MATCHED，`MATCHED` 不计入（构造一条 MATCHED 后断言率不变） |
| V2-9 | 差异必须填说明：`POST /recon-differences/{id}/resolve` 不带 remark → 400；带 remark → 200 且 status=CLOSED |
| V2-10 | 审计可追：任一 resolve 后，`finance_audit_logs` 出现对应记录（含 run_id、difference_id、前后状态） |
| V2-11 | 金额单位：`recon_runs.external_amount` 必须等于 `SUM(bank_transactions.amount)`（分），与导入 CSV 的元值 ×100 一致（差 100 倍即 FAIL） |
| V2-12 | 只读防线不受影响：`python scripts/qa/consistency_check.py` 仍 5 PASS（A1–A5），A6 仍为 INFO |

## 2.14 边缘案例清单（E2）

1. **E2-1 跨日到账**：23:58 的销售、次日 00:03 银行入账 → 记为 `TIME_DIFF`，不新建 run，不重复计差异。
2. **E2-2 同一笔重复导入**：同 `raw_hash` 二次导入 → `bank_transactions` 增量为 0，跑批差异不增加。
3. **E2-3 平台汇总打款 + 手续费**：微信 T+1 打款 = 当日多笔之和 - 手续费 → N:1 人工「汇总匹配」+ `CHANNEL_FEE` 差额。
4. **E2-4 混合支付订单**：`order_payment_records` 有 1 现金 + 3 支付宝 → 内部侧聚合为一条 item（`payment_methods='1,3'`），金额为两条之和；外部可能两笔到账 → 1:N 人工合并。
5. **E2-5 payment_method=0（未记录）**：不得当作现金参与现金对账；对账 item 标 `payment_methods='0'` 并在 UI 提示「渠道未知」，仅允许与「无法识别来源的到账」人工匹配。
6. **E2-6 订单多次支付（部分支付）**：`orders.payment_status=1` 的中间态不参与当日对账，只对 `payment_status=2` 后的支付记录对账（TD-09）。
7. **E2-7 退款冲抵**：退款（`flow_category=7` 或反向）是否计入外部支出侧对账 → 待定（TD-10）；当前设计**仅对收入侧**对账。
8. **E2-8 账户停用/换卡**：`bank_accounts.status<>1` 期间的外部流水仍需认领（历史到账），但禁止发起新 run（TD-11）。
9. **E2-9 跑批期间新增流水**：跑批开始后新落地的 SALE 流水不属于本 run（按 `create_time <= run.started_at` 快照），避免无限追加；次日 run 会覆盖。
10. **E2-10 金额精度**：导入 CSV 有「12.345 元」→ 拒绝整批导入并报行号（禁止四舍五入到分）。
11. **E2-11 同一 external_txn_id 打到两个账户**（银行侧重复回单）→ `DUPLICATE`，人工裁决归属账户。
12. **E2-12 时区**：`business_date` 一律按服务器本地日（Asia/Shanghai）；银行回单时间若带时区必须换算后再比窗口（TD-12）。

---

# §3 现金交款单闭环

## 3.1 起点与边界

**起点**：`pos_shifts` 出纳交班 —— 班次 `status='ended'` 且 `closing_cash` 已录入（`pos_shifts.closing_cash`，单位：元 or 分？**现状为元（numeric 语义）**，落地时必须与 `fund_flows.amount`（分）显式换算 —— 见 TD-13）。

**边界**：本闭环只处理**现金**（`order_payment_records.payment_method=1`）与「现金户 → 银行户」的实物流转；微信/支付宝/银行卡走平台清算，不在本单范围。

## 3.2 草案表 `deposit_slips`（现金交款单）

| 字段 | 类型 | 说明 |
|---|---|---|
| slip_id | bigint PK | |
| slip_no | varchar(32) | UNIQUE，`DS<yyyyMMdd><NNNN>` |
| store_id | bigint | 门店 |
| shift_id | bigint | → `pos_shifts.shift_id`；UNIQUE（一个班次一张缴款单，TD-14） |
| cashier_employee_id / cashier_name | bigint / varchar(50) | 交款人（班次收银员，来自 `pos_shifts.employee_id/employee_name`） |
| collector_user_id / collector_name | bigint / varchar(50) | 出纳（实收人） |
| expected_amount | bigint | **应交（分）**= 班结现金收入（3.3 口径） |
| actual_amount | bigint | **实交（分）**= 出纳点钞金额 |
| difference_amount | bigint | `actual - expected`（分），>0 长款，<0 短款 |
| cash_account_id | bigint | 现金户 `bank_accounts.account_id`（`account_type=2`，scope=STORE） |
| bank_account_id | bigint | 存行目标账户（`account_type=1`，同一 store_id） |
| deposit_bank_txn_id | bigint | 关联 `bank_transactions.id`（存行后回填） |
| deposit_ref_no | varchar(64) | 银行进账单号 |
| status | varchar(16) | DRAFT/SUBMITTED/RECEIVED/DEPOSITED/VERIFIED/CLOSED/REJECTED/REVERSED |
| submitted_at / received_at / deposited_at / verified_at / closed_at | timestamp | 各阶段时间戳 |
| remark | varchar(500) | |
| create_user_id / created_by / version / deleted / create_time / update_time | — | 与项目其余表一致 |

## 3.3 生成与流程

### 3.3.1 应交金额口径
```
expected_amount(分)
  = SUM(order_payment_records.payment_amount)
    WHERE payment_method = 1（现金）
      AND order_id ∈ 该班次的订单（pos_shifts.start_time <= orders.payment_time <= pos_shifts.end_time）
      AND orders.payment_status = 2 AND orders.deleted = 0
  - 班内现金退款金额（若有；退款口径见 TD-10）
```
> `pos_shifts.closing_cash` 只作为**对账参考**（收银员自报），**不作为**应交金额的权威源；二者不一致时以支付记录重算值为应交，差额进 `difference_amount` 并在 UI 标红（TD-15）。

### 3.3.2 主流程
```
[交班] pos_shifts.status='ended'
   │  （系统）生成 deposit_slips：status=DRAFT，expected_amount=3.3.1 口径
   ▼
[DRAFT] 收银员核对 → 提交（附小票/说明）──▶ SUBMITTED
   ▼
[SUBMITTED] 出纳点钞：录入 actual_amount
   ├─ 不符（差额≠0）→ 可 REJECTED（退回收银员重核）或继续（差额挂账，见 3.4）
   └─ 相符 → RECEIVED（出纳确认收妥，生成「现金户」凭证/流水关系见 3.5）
   ▼
[RECEIVED] 存行：录入 deposit_ref_no（银行进账单号）+ 关联 bank_transactions.id
   ▼
[DEPOSITED] 财务复核（金额、单据、账户一致性）
   ▼
[VERIFIED] 归档（月度结账前必须全部 CLOSED）
   ▼
[CLOSED]
```

### 3.3.3 状态机
```
DRAFT ──submit──▶ SUBMITTED ──receive──▶ RECEIVED ──deposit──▶ DEPOSITED ──verify──▶ VERIFIED ──close──▶ CLOSED
  │                   │                    │                     │                    │
  │                   └──reject──▶ REJECTED ──resubmit──▶ SUBMITTED（新版本记录，旧记录保留）
  │                                        │                     │
  └──void（未提交前可删/作废）──────────────┴──reverse──▶ REVERSED（整单冲正：生成反向流水，不删单）
```
**禁止**：`RECEIVED` 之后回退到 `DRAFT`；任何状态都不允许修改 `expected_amount`（要改只能作废重开）。

## 3.4 差额处理规则

| 情形 | 定义 | 处理 | 禁止 |
|---|---|---|---|
| 短款（现金少） | `actual < expected`，`difference_amount < 0` | 差额挂「待查损溢」；出纳与收银员双签确认；按门店制度追责（生成应收 or 工资扣款，TD-16）；缴款单可继续 `RECEIVED` | 不得直接改 `pos_shifts.closing_cash` |
| 长款（现金多） | `actual > expected`，`difference_amount > 0` | 差额挂「待查溢余」，**不确认收入**；核实后退回或转营业外（需财务审批） | 不得直接计入当日营业外收入 |
| 少交（实交少于应交且已 RECEIVED） | 单据状态为 RECEIVED 但后续发现 | 走 `REVERSED` + 重开新单；原单保留 | 不得就地改 actual_amount |
| 多交 | 同上反向 | 同上 | 同上 |
| 平账 | `difference_amount = 0` | 正常推进 | — |

> 差额永远记在 `deposit_slips.difference_amount`，不改来源数据；`pos_shifts.closing_cash` **只读**。

## 3.5 与 `fund_flows` 的关系（明确结论）

**现状事实**：POS 现金支付成功后，系统已在支付链路生成 SALE 收入流水
（`fund_flows`：`flow_direction=1`、`flow_category=1`、`account_id`=该店现金户、`order_id`=订单、`amount`=支付金额分、`status=2`）。

**因此本设计的结论是「三段式，只记一次收入」**：

| 阶段 | 是否生成 `fund_flows` | 口径 |
|---|---|---|
| ① 支付时（已实现） | **生成 1 条 SALE 收入**（现金户） | `flow_category=1`，`order_id` 关联 |
| ② 缴款单 RECEIVED（出纳收妥） | **不生成任何流水** | 现金只是从收银员手里到出纳手里，账户余额未变（都在现金户），若生成即为重复计收入 |
| ③ 存行 DEPOSITED | **生成 2 条内部转账**：现金户 `flow_direction=2, flow_category=5(内部转账)`，银行户 `flow_direction=1, flow_category=5`，金额都 = `actual_amount` | 幂等键：remark 带 `[depositSlip=<slip_no>]`；两账户余额各自原子更新 |

**例外补记**：若某班次的 SALE 现金流水缺失（历史欠账 / 上游失败），在 **RECEIVED** 时补记一条 SALE 收入（`flow_category=1`，幂等键 `[posShift=<shift_id>][cashBackfill]`，`create_user_id=0`），**且必须在 UI 标注「系统补记」**；此时 ③ 仍照常生成内部转账，不重复计收入。

> 待定：内部转账是否生成**凭证**（voucher）由 AutoVoucher 体系决定（TD-17）。

## 3.6 接口草案

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | POST | `/v1/finance/deposit-slips/generate` | 按 `shiftId` 生成（幂等：已存在则返回既有单 + `reused:true`） |
| 2 | GET | `/v1/finance/deposit-slips` | 列表：`?storeId=&status=&dateFrom=&dateTo=&cashierId=` |
| 3 | GET | `/v1/finance/deposit-slips/{id}` | 详情（含差额、审批链、关联流水） |
| 4 | POST | `/v1/finance/deposit-slips/{id}/submit` | DRAFT → SUBMITTED |
| 5 | POST | `/v1/finance/deposit-slips/{id}/receive` | `{actualAmount(分), dueRemark, shortsReason?}` → RECEIVED（差额落 difference_amount） |
| 6 | POST | `/v1/finance/deposit-slips/{id}/deposit` | `{depositRefNo, bankTxnId, bankAccountId, depositDate}` → DEPOSITED（生成 2 条内部转账流水） |
| 7 | POST | `/v1/finance/deposit-slips/{id}/verify` | 财务复核 → VERIFIED |
| 8 | POST | `/v1/finance/deposit-slips/{id}/close` | 归档 → CLOSED |
| 9 | POST | `/v1/finance/deposit-slips/{id}/reject` | `{reason}` → REJECTED |
| 10 | POST | `/v1/finance/deposit-slips/{id}/reverse` | `{reason, reverseFlows:true/false}` → REVERSED（生成反向流水） |
| 11 | GET | `/v1/finance/deposit-slips/{id}/export` | 导出单张 PDF/打印视图 |

## 3.7 页面线框

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 现金交款单  DS202610240007                   状态：RECEIVED                  │
├──────────────────────────────────────────────────────────────────────────────┤
│ 门店：测试门店A    班次：2026-10-24 早班(08:00-16:00)   收银员：员工B          │
│ 应交：7,823.30 元    实交：7,820.00 元    差额：-3.30 元（短款）  ⚠            │
├──────────────────────────────────────────────────────────────────────────────┤
│ 明细：现金收入 617 笔 ｜ 现金退款 0 笔 ｜ 班结自报 closing_cash 7,820.00 元    │
├──────────────────────────────────────────────────────────────────────────────┤
│ 关联：现金户(account_id=5)   存行账户[工行 ****1234 ▾]   进账单号[________]   │
│      悬空 bank_transactions：[关联]                                           │
├──────────────────────────────────────────────────────────────────────────────┤
│ 历史：[2026-10-24 16:05 生成]→[16:20 提交]→[16:40 出纳收妥(差额-3.30)]         │
├──────────────────────────────────────────────────────────────────────────────┤
│ 说明：短款 3.30 元，已双签确认，挂待查损溢                                     │
│                                   [退回] [存行] [复核通过] [导出]              │
└──────────────────────────────────────────────────────────────────────────────┘
```

## 3.8 权限点

| 权限 | 用途 |
|---|---|
| `finance:deposit-slip:view` | 查看缴款单（含门店数据权限） |
| `finance:deposit-slip:create` | 生成（或由交班自动生成） |
| `finance:deposit-slip:submit` | 收银员提交 |
| `finance:deposit-slip:receive` | 出纳收妥/录实交 |
| `finance:deposit-slip:deposit` | 存行登记（含关联银行流水） |
| `finance:deposit-slip:verify` | 财务复核 |
| `finance:deposit-slip:close` | 归档 |
| `finance:deposit-slip:reverse` | 冲正（高权限，建议仅财务主管） |

## 3.9 验收标准（可测断言）

| 编号 | 断言 |
|---|---|
| V3-1 | 班次 `ended` 后 `POST /deposit-slips/generate` 生成 1 张 DRAFT；重复调用返回同一 `slip_id`（`reused=true`），`SELECT count(*) FROM deposit_slips WHERE shift_id=?` = 1 |
| V3-2 | `expected_amount` 可复算：`SUM(order_payment_records.payment_amount WHERE payment_method=1 AND 订单∈班次时间窗 AND payment_status=2)` 与该单字段一致（分毫不差） |
| V3-3 | `difference_amount = actual_amount - expected_amount`，三种情形（0/正/负）各断言一次 |
| V3-4 | 状态机不可跳步：DRAFT 直接调 `deposit` → 400；SUBMITTED 直接 `verify` → 400 |
| V3-5 | RECEIVED 不生成流水：`SELECT count(*) FROM fund_flows WHERE remark LIKE '%[depositSlip=DS...]%'` 在 RECEIVED 后仍 = 0 |
| V3-6 | DEPOSITED 生成恰好 2 条内部转账（一出一入、金额相同 = actual_amount），且不新增 SALE 收入流水：`SELECT count(*) FROM fund_flows WHERE remark LIKE '%[depositSlip=<no>]%' AND flow_category=1` = 0 |
| V3-7 | 重复 `deposit` 调用幂等：再调一次 → 400/409 且流水仍为 2 条（不重复入账） |
| V3-8 | 余额一致：存行后现金户余额减少 `actual_amount`、存款户增加 `actual_amount`；两账户 `balance_after` 与各自 `balance` 相等 |
| V3-9 | 短款不改源数据：`SELECT closing_cash FROM pos_shifts WHERE shift_id=?` 在 RECEIVED 前后不变 |
| V3-10 | 冲正：`reverse` 后原单 status=REVERSED（记录仍在），新增反向流水，余额回滚到冲正前 |
| V3-11 | 审计：每一步状态变化都能在 `finance_audit_logs` 找到（含 operator、时间、前后状态） |
| V3-12 | 与对账联动：存行后该笔 `bank_transactions` 可被 §2 的 L1 精确命中（`deposit_ref_no` ↔ `external_txn_id`） |

## 3.10 边缘案例清单（E3）

1. **E3-1 一个班次跨天**（22:00 开班、次日 02:00 交班）：`expected_amount` 按**班次时间窗**而非自然日统计，缴款单 `business_date` 取交班日（TD-18）。
2. **E3-2 班内现金退款**：退款金额必须从应交中扣减；若退款走的不是现金（如退回微信），不扣减。
3. **E3-3 混合支付订单**：订单现金部分 10 元 + 支付宝 8 元 → 只计 10 元进应交；`payment_method=0` 的金额不计入现金（宁少不多）。
4. **E3-4 支付记录缺失（历史单）**：某单 `paid_amount>0` 但无 `order_payment_records` → 该单不计入应交，并在缴款单页面给出「N 笔支付记录缺失，可能低估应交」告警（与 `consistency_check` A2 同源）。
5. **E3-5 closing_cash 未填**：交班强制要求填 `closing_cash`，为空则**不允许**生成缴款单（或生成但标「待补」并禁止 submit，TD-19）。
6. **E3-6 重复交班**：同一 terminal 同一时间窗出现两条 ended 班次 → 只允许一条生成缴款单，另一条标异常并人工合并。
7. **E3-7 出纳与收银员同一人**：允许但需要「双人复核」权限二次确认（内控要求，TD-20）。
8. **E3-8 存行金额 ≠ 实交**（银行实际到账少/多）：`deposit_ref_no` 关联的 `bank_transactions.amount` 与 `actual_amount` 不一致 → 存行被拒，必须先走差额处理。
9. **E3-9 跨店/跨账户误选**：`bank_account_id.store_id` 必须等于缴款单 `store_id`，否则 400（禁止把 A 店现金存进 B 店账户）。
10. **E3-10 缴款单作废后重开**：原单 REVERSED、新单 `reopen_of` 指向原单；不允许复用 `slip_no`。
11. **E3-11 月末结账拦截**：会计期间已结账（`accounting_periods` 状态=已结账）的月份，禁止再 `deposit/verify`（TD-21）。
12. **E3-12 现金户余额不足**：存行时现金户 `balance < actual_amount` → 拒绝并提示（说明上游有未入账现金或历史缺口）。

---

# §4 待裁决点汇总（TD）

| 编号 | 待定问题 | 建议默认值 | 影响 |
|---|---|---|---|
| TD-01 | `bank_transactions` 表名与数据来源（CSV 导入 / 银企直连 / 邮件解析） | 新建 `bank_transactions` + CSV 导入 | §2 全部外部侧 |
| TD-02 | 是否多币种（`currency` 恒 CNY 的前提是否长期成立） | 本期仅 CNY | 匹配/汇总口径 |
| TD-03 | 是否复用既有 `bank_payment_records`（已有同名 Controller） | 不复用，避免语义混淆 | 表设计 |
| TD-04 | L2 时间窗 N（默认 120 分钟） | 120 分钟 | 自动匹配率 |
| TD-05 | 手续费容忍阈值（默认 0.5%，上限？） | 0.5% 且 ≤ 5 元 | 差额认定 |
| TD-06 | 是否允许自动 1:N / N:1 | 不允许（只 1:1 自动） | 勾稽率与风险 |
| TD-07 | 核销差额是否生成费用/其他支出流水 | 生成（`flow_category=6`） | 账实一致 |
| TD-08 | 跨日到账是否新建 run | 不新建（挂 TIME_DIFF） | 对账率口径 |
| TD-09 | `payment_status=1`（部分支付）是否参与当日对账 | 不参与 | 单边账噪音 |
| TD-10 | 退款（收入侧反向）是否纳入对账范围 | 本期不纳入收入侧，单列支出侧（后续） | 范围 |
| TD-11 | 停用账户是否允许发起新 run | 允许（需手工确认），历史仍可对 | 运维 |
| TD-12 | 银行回单时区换算规则 | 一律换算到 Asia/Shanghai | 时间窗 |
| TD-13 | `pos_shifts.closing_cash` 单位（元 vs 分）与换算点 | 视为「元」，入库前 ×100 转分 | 金额精度 |
| TD-14 | 一个班次是否只允许一张缴款单 | 是（UNIQUE shift_id） | 单据模型 |
| TD-15 | 应交金额以「支付记录重算」还是「closing_cash」为权威 | 重算为准，closing_cash 仅参考 | 短款判定 |
| TD-16 | 短款追责方式（应收 / 工资扣款 / 手工核销） | 先挂待查损溢，财务线下决定 | 与 HR 联动 |
| TD-17 | 内部转账是否生成会计凭证 | 由 AutoVoucher 规则决定，本期手工 | 记账 |
| TD-18 | 跨天班次的 `business_date` 归属 | 取交班日 | 报表归属 |
| TD-19 | `closing_cash` 为空是否允许生成缴款单 | 生成但禁止 submit | 流程阻塞 |
| TD-20 | 收银员=出纳时是否强制二次复核 | 强制 | 内控 |
| TD-21 | 已结账期间是否硬拦截存行/复核 | 硬拦截 | 结账完整性 |

> **待裁决点数量：21（TD-01 ~ TD-21）**。其中 TD-01/13/15 是**开工前置**（不定就无法写代码），其余可在实现中默认后置调整。

---

# §5 与既有防线的衔接（建议新增断言，不替换）

| 断言 | 归属 | 说明 |
|---|---|---|
| recon 同日 run 唯一 | 新 `consistency_check` A7（建议） | `SELECT count(*) FROM (SELECT run_key,count(*) c FROM recon_runs GROUP BY 1 HAVING c>1) t` = 0 |
| 缴款单与班次一对一 | 新 A8 | `SELECT count(*) FROM (SELECT shift_id,count(*) c FROM deposit_slips WHERE deleted=0 GROUP BY 1 HAVING c>1) t` = 0 |
| 存行流水成对 | 新 A9 | 每张 DEPOSITED 缴款单对应恰好 2 条 `[depositSlip=...]` 内部转账 |
| 现金户余额非负 | 新 A10 | `SELECT count(*) FROM bank_accounts WHERE account_type=2 AND balance<0` = 0 |
| 现有 A1–A5 | `consistency_check.py` | **保持不变**（本设计不改既有口径） |

---

## 附：术语
- **SALE 流水**：`fund_flows` 中 `flow_direction=1 AND flow_category=1` 的销售收款流水。
- **现金户 / 存款户**：`bank_accounts` 中 `account_type=2`（现金）/ `account_type=1`（银行）。
- **勾稽率**：`matched_amount / internal_amount`（只计 AUTO_MATCHED + MANUAL_MATCHED）。
- **短款 / 长款**：实交 < 应交 / 实交 > 应交。
