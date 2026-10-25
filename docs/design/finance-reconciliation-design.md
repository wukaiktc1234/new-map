# 财务对账体系设计（定稿合并稿）：第三方流水导入 · 自动勾稽 · 现金交款单 · 日记账与余额调节表

| 项 | 内容 |
|---|---|
| 文档名 | `docs/design/finance-reconciliation-design.md` |
| 版本 | **v1.3-draft**（v1.3：调节表**不调账**升级为硬规则一、审核四眼由「可配」改为**硬门禁 V8 并排除出纳**、新增 AC-44/AC-45；v1.1：§5 与 §4 调节表对齐 / TD-22 提升为 PC-7 / 新增 AC-42、AC-43、PC-8；**v1.2：修正调节表公式方向（A/B↔C/D 曾写反）、§4.3 示例数字与符号、V5–V7 口径统一、`bank_balance` 取值字段名；补 TD-12 退化键 NEEDS_REVIEW、TD-23 单向修复**） |
| 日期 | 2026-10-24（时区 Asia/Shanghai） |
| 作者 | 数据/财务轨（fund-500）+ ts-hr-asset（A 部分），由架构总控合并 |
| 合并自 | `docs/design/draft-finance-recon-a.md`（CSV 导入 / 日记账 / 调节表）+ `docs/design/draft-finance-recon-b.md`（自动勾稽 / 交款单） |
| 状态 | **待 Owner 审阅**（§9 尚有未裁决点） |
| 性质 | **设计稿**：不含代码、不含 DDL、不改配置；所有"草案表"在活库中**均不存在** |

> **结论：本文档是单一可交付设计稿，替代 A/B 两份草稿；实现前必须先由 Owner 对 §9 的剩余待裁决点逐条拍板。**

---

## 0. 背景与现状基线

### 0.1 为什么需要这一层

`fund_flows` 记录的是**业务发生**（卖出了、收到钱了 → 记一笔），回答不了财务真正要问的问题：

> **钱，究竟有没有真的到账？到账多少？与账面差在哪、差多少、谁认领、怎么处置？**

| # | 缺口 | 现状证据（只读核对，2026-10-24） |
|---|---|---|
| 1 | 没有「银行侧事实」 | 全库无任何资金侧导入/流水事实表（`information_schema` 仅命中 `inventory_transactions_legacy`，与资金无关） |
| 2 | 余额只有单侧口径 | `bank_accounts.balance`（分）与 `fund_flows.balance_after`（分）都**从未与银行对账单核对** |
| 3 | 渠道差异无确认环节 | 手续费、退款、T+1 汇总到账，在账面没有对应事实来源 |
| 4 | 导入无审计留痕 | 无「谁在何时导了哪份文件、成功/重复/失败多少行」的批次记录 |
| 5 | 现金交款无闭环 | `pos_shifts.closing_cash` 只是收银员自报，缺「出纳收妥 → 存行 → 复核」链路 |

### 0.2 现状基线（设计必须遵守的既有约束）

| 约束 | 现状 | 对本设计的影响 |
|---|---|---|
| 支付方式编码单轨 | `order_payment_records.payment_method`：0未记录 / 1现金 / 2微信 / 3支付宝 / 4银行卡 / 5余额 | 现金判定**只认 1**；0 不得当现金 |
| 资金流水挂订单 | `fund_flows.order_id`（varchar）已落地 | 内部侧匹配首选 order_id，不靠备注字符串 |
| 门店隔离 | `bank_accounts.scope`(GLOBAL/STORE) + `store_id` | 对账/导入/缴款单一律按账户 × 门店切片；**禁止**「无归属→默认门店」兜底 |
| 余额原子更新 | `bank_accounts.balance`（分）+ `version` 乐观锁 | 冲正/补账/内部转账必须走同一套原子更新，**禁止直接 SET** |
| 审计 | `fund_flows.create_user_id` / `created_by` | 系统补记用 `create_user_id=0` + `created_by=system/backfill-*` |
| 流水状态 | `fund_flows.status` 1待处理 / 2已完成 / 3已取消，默认已完成 | 勾稽只纳入 `status=2` |
| 现金班次 | `pos_shifts`：`closing_cash` / `total_amount` / `status` / `employee_*`（19 行存量） | 缴款单「应交」的参考源（**非权威**，见 §3.2） |
| 数据体量 | `bank_accounts` 32 行 / `fund_flows` 576 行 / 近 30 天 `orders` 617 单 | 验收基线数量级 |
| 既有防线 | `scripts/qa/consistency_check.py`（A1–A5 + A6 信息性）、`scripts/qa/ui_e2e_payment.py`、`scripts/qa/schema_guard.py` | 本设计**不删除**任何既有断言，只新增（§8） |

### 0.3 术语表（全文统一，不再混用同义词）

| 术语 | 定义 |
|---|---|
| **业务流水** | `fund_flows` 的一行：业务动作产生的**账面**资金变动 |
| **银行流水** | 渠道（微信/支付宝/银行）导出的**客观到账事实**，落地为 `bank_transactions`（§5.3） |
| **SALE 流水** | `fund_flows` 中 `flow_direction=1 AND flow_category=1` 的销售收款流水 |
| **现金户 / 存款户** | `bank_accounts.account_type=2`（现金）/ `=1`（银行） |
| **勾稽** | 把一条银行流水与一条业务流水建立 1:1 / 1:N / N:1 关系的过程（全文统一用「勾稽」，不用"匹配对账"混称） |
| **勾稽率** | `matched_amount / internal_amount`，**只计 AUTO_MATCHED + MANUAL_MATCHED**（MATCHED 不计，裁决 7） |
| **差异** | 勾稽不成功或金额不等产生的偏移量（分），必须被解释（差异类型见 §2.4） |
| **未达账项** | 已在企业账未在银行账（或反之）的合法时间性差异，调节表四大类来源 |
| **调节表** | 银行存款余额调节表：账面余额 ± 未达账项 = 对账单余额 ± 未达账项 |
| **缴款单** | 现金从收银员 → 出纳 → 银行的实物流转单据（`deposit_slips`） |
| **短款 / 长款** | 实交 < 应交 / 实交 > 应交 |
| **批次** | 一次导入（`import_batches`）或一次对账跑批（`recon_runs`）的容器 |

### 0.4 目标 / 非目标

**目标**

| # | 目标 |
|---|---|
| G1 | 能导入微信/支付宝/银行三类 CSV，**元→分**无损换算，行级错误可读可重导 |
| G2 | 同一文件重复导入**零重复事实**（幂等），批次全程留痕 |
| G3 | 按「账户 × 业务日」自动勾稽，差异**自动分池**、人工认领核销，结果可复算、可重跑、幂等 |
| G4 | 现金交款单闭环：应交可重算、差额留痕、存行生成两笔内部转账 |
| G5 | 按账户出资金日记账，出具银行存款余额调节表，且**调节后两侧必须相等**才能审核 |

**非目标（本期明确不做）**

| # | 不做 | 说明 |
|---|---|---|
| N1 | 银企直连 / 银行邮件解析 | 本阶段**只做 CSV 导入**（裁决 1） |
| N2 | 多币种 | 现状 `currency` 恒 CNY；非 CNY 行整批拒绝（TD-19） |
| N3 | 自动开票 / 自动记账 | 凭证由 AutoVoucher 体系负责（TD-35） |
| N4 | 规则引擎式自动匹配 | 本期人工勾稽优先；自动只做 L1/L2 精确与唯一命中 |
| N5 | 报表 / 预算 / 税 / 应收应付 | 另有设计，不在本文 |

---

## 1. 第三方流水导入（CSV）

> 外部事实表的**权威字段定义见 §5.3**，本节只讲来源、流程、规则与接口，不重复列字段。

### 1.1 三类来源与典型列

| 来源 `source_type` | 渠道 | 典型列（列名以各渠道导出为准，导入器做**别名映射**，见附录 A） |
|---|---|---|
| `WECHAT` | 微信支付 | 交易时间、商户号、**微信订单号**、**商户订单号**、交易类型、交易状态、付款银行、**应结订单金额**、**手续费**、费率、订单金额、退款单号、退款金额、退款类型、退款状态、商品名称 |
| `ALIPAY` | 支付宝 | **支付宝交易号**、**商家订单号**、业务类型、商品名称、创建时间、完成时间、门店编号、门店名称、对方账户、**订单金额（元）**、商家实收（元）、**服务费（元）**、退款批次号、退款金额、备注 |
| `BANK` | 银行 | 交易日期/时间、**交易流水号**、借方金额、贷方金额、**发生额**、余额、对方户名、**对方账号**、摘要/用途、交易类型（转入/转出/手续费/利息） |

**内部语义映射**（与现状字段对齐）

| 内部语义 | 取值要点 |
|---|---|
| `txn_time`（Asia/Shanghai） | 微信「交易时间」→ 支付宝「完成时间」（空则「创建时间」）→ 银行「交易日期+时间」 |
| `external_txn_id` | 微信订单号 / 支付宝交易号 / 银行交易流水号（跨来源不互认，TD-13） |
| `merchant_order_no` | 微信「商户订单号」/ 支付宝「商家订单号」/ 银行摘要中的订单号 |
| `amount`（分） | 见 §1.3；**收入/支出与渠道方向一致，不混符号** |
| `fee`（分） | 微信「手续费」/ 支付宝「服务费」；银行单列则取 |
| `direction`（1收入/2支出） | 与 `fund_flows.flow_direction` 同口径；退款单独见 §1.3 |
| `summary` | 微信「商品名称」/ 支付宝「备注」/ 银行「摘要」 |

> **结论：外部流水表统一命名 `bank_transactions`，本阶段只做 CSV 导入；银企直连与邮件解析为非目标（已裁决 1）。**

### 1.2 导入流程状态机

```
[选择文件] → UPLOADED（落临时区 + 计算 sha256）
      ↓ 解析预检（编码 / 表头映射 / 金额格式 / 时区 / 行数上限）
  PARSED_OK ────────────────────────→ PARSED_FAILED（整批拒绝，可读原因）
      ↓ 幂等去重（批次内 + 与库内）
  DEDUPED
      ↓ 落库（bank_transactions 插入 + import_rows 状态回写）
  COMMITTED（成功 / 重复 / 错误 三类计数）
      ↓ 存在需人工确认行时
  PARTIAL（不再落新行，仅标记待人工）
```

**硬规则**

1. **两段式**：`UPLOADED → PARSED_OK` 只预检与预览（返回前 50 行 + 行级错误），**不写 `bank_transactions`**；确认后才 `COMMITTED`（TD-15）。
2. **批次不可变**：`COMMITTED` 的批次不允许再追加行；重导必须产生新批次，靠幂等键判重，两批次保留对照。
3. **失败不静默**：整批失败必须落 `status=PARSED_FAILED` + `error_summary`，不允许只在界面弹窗。
4. **行级失败不阻断**：单行错误 → `import_rows.status=ERROR`，其余照常入库，批次终态 `PARTIAL`。

### 1.3 编码、金额、表头、负数与退款

| 处理项 | 规则 | 拒绝条件（→ 行级 ERROR） |
|---|---|---|
| 字符编码 | 优先 UTF-8（含 BOM）；失败回退 GB18030（银行常见）；结果写 `import_batches.encoding_used` | 两种都失败 → 整批 FAILED |
| 金额单位（元→分） | 元值 × 100 后**必须为整数**，禁用 `double`；银行若给「金额（分）」列则原样使用并标 `unit_hint=FEN` | 3 位以上小数、千分位逗号、全角数字 → ERROR |
| 符号与方向 | 内部统一：进账 `direction=1`、出账 `direction=2`、退款标记 `REFUND`；`amount` 恒正数（方向独立表达） | 无法判定方向 → ERROR |
| 表头容错 | 表头可出现在前 1–5 行；列名走别名映射；多余列忽略；仅**必需列**缺失报错 | 缺 `txn_time` / `amount` / `external_txn_id` → 整批 FAILED |
| 负数 / 退款行 | 退款金额为负或单列 → 统一 `direction=REFUND`；能关联原单则填 `merchant_order_no` | 退款无任何关联键 → 入库但 `needs_review=true` |
| 时区 | 无时区时间按 Asia/Shanghai 解释；带时区按原时区换算后落库 | 未来时间（> 当前+1 天）→ ERROR |
| 行数上限 | 同步 ≤ 50,000 行（TD-17 决定是否异步） | 超出 → 提示拆分 |

**金额单位裁决（已定）**：`fund_flows.amount`、`bank_accounts.balance`、`order_payment_records.payment_amount`、`bank_transactions.amount`、所有 `*_amount` 新增字段一律**分**；`pos_shifts.closing_cash` 为**元**（numeric(10,2)，实测 `0.00` / `total_amount 3008.80`），跨边界 **×100 转分**，禁止反向把分当元。

### 1.4 幂等去重

| 方案 | 唯一键 | 评价 |
|---|---|---|
| A | `(account_id, source_type, external_txn_id)` | 精确、可解释；银行流水号缺失时不可用 |
| B | `(account_id, txn_time, amount, counterparty_account)` | 覆盖无流水号来源；同额同秒多笔真实交易会误判 |
| **C（建议默认）** | A 为主；无 `external_txn_id` 时退 B，并在行上标 `dedupe_basis=COMPOSITE` | 兼顾精度与覆盖（TD-12） |

**批次内去重**：同键重复 → 保留首条，后续行按下方「按 dedupe_basis 分流」处理，并写 `duplicate_of_row_no`。
**与库内去重**：命中既有 `bank_transactions` → 按下方「按 dedupe_basis 分流」处理，并写 `matched_transaction_id`。

**按 dedupe_basis 分流（TD-12 附带裁决，误判成本不对称）**

| 命中场景 | 处理 | 理由 |
|---|---|---|
| **精确键**（`dedupe_basis=EXTERNAL_ID`，即命中 `external_txn_id`） | `import_rows.status=DUPLICATE`，**静默跳过**，不落 `bank_transactions` | 渠道唯一号相同 = 同一笔，误判概率极低 |
| **退化键**（`dedupe_basis=COMPOSITE`，即仅「时间+金额+对方」相同） | `import_rows.status=NEEDS_REVIEW`，**落 `bank_transactions` 但置 `needs_review=1` 且 `match_status=EXCEPTION`**，不静默判重 | 组合键置信度天然低：同额同秒也可能是两笔真实交易；吞掉真实交易的代价 > 多一次人工确认 |
| 后续到达同笔且带 `external_txn_id` | 允许把原 COMPOSITE 行「升级」关联（写 `upgraded_from`，非阻塞项） | 事后可纠偏 |

> **结论：精确键重复 = DUPLICATE（跳过）；退化键重复 = NEEDS_REVIEW（落库+人工确认），绝不静默吞单。**
**文件级去重**：`file_sha256` 相同直接提示「该文件已导过（批次 xxx）」，不产生新批次（TD-15 允许覆盖）。

### 1.5 接口草案（导入模块）

| # | 方法 & 路径 | 用途 | 请求（关键） | 响应（关键） |
|---|---|---|---|---|
| 1 | `POST /v1/finance/bank-transactions/import` | 上传 + 预检（不落事实表） | multipart：`file`、`sourceType`(WECHAT\|ALIPAY\|BANK)、`accountId` | `batchId`、`batchNo`、`status`、`totalRows`、`previewRows[≤50]`、`rowErrors[≤50]` |
| 2 | `GET /v1/finance/imports/{batchId}` | 批次详情 + 统计 | — | 批次字段 + `okRows/dupRows/errorRows` + `errorSummary` |
| 3 | `GET /v1/finance/imports/{batchId}/rows` | 行级回执分页 | `status`、`page`、`size`(≤200) | `records[]`（`rowNo/status/errorCode/errorMessage/rawPreview`）、`total` |
| 4 | `POST /v1/finance/imports/{batchId}/commit` | 确认落库 | — | `status=COMMITTED\|PARTIAL` + 三类计数 |
| 5 | `POST /v1/finance/imports/{batchId}/cancel` | 撤销未提交批次 | `reason` | `status=CANCELLED` |
| 6 | `GET /v1/finance/bank-transactions` | 银行流水查询 | `accountId`、`from`、`to`、`sourceType`、`matchStatus`、`needsReview`、分页 | `records[]`、`total` |

### 1.6 页面线框（资金管理 → 流水导入）

```
┌─ 流水导入 ────────────────────────────────────────────────────────────┐
│ [账户 ▼](必填, 仅列出有权限账户)   [来源 ▼ 微信/支付宝/银行]           │
│ ┌────────── 拖拽或点击上传 CSV ──────────┐   [下载模板]              │
│ │  file.csv   (≤10MB / ≤50k 行)           │                            │
│ └─────────────────────────────────────────┘                            │
│ 预检：共 1,204 行 │ 可导入 1,180 │ 重复 20 │ 错误 4        [查看错误]  │
│ ┌ 行级预览（前 50 行）───────────────────────────────────────────────┐ │
│ │ 行号 │ 交易时间 │ 单号 │ 方向 │ 金额(元) │ 状态  │ 原因            │ │
│ │ 3    │ 10-23 09:12│ WX… │ 收入 │ 128.00   │ ERROR │ 金额含千分位    │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│                                     [取消]  [确认导入(1,180 行)]       │
└────────────────────────────────────────────────────────────────────────┘
```

列表页：批次号 / 账户 / 来源 / 文件名 / 时间 / 总数 / 成功 / 重复 / 错误 / 状态 / 操作（详情、重导）。
详情页：批次头 + 三类计数卡 + 行级表（按状态筛选）+「导出错误行 CSV」。

### 1.7 本轮不实现（导入）

- 银行直连 / 邮件解析（裁决 1，非目标）；
- 列名映射的可视化模板编辑器（首版用固定映射 / 配置文件，TD-16）；
- 异步导入任务队列（> 50k 行先拆分，TD-17）；
- 错误行「一键修正后重导」自动化（只提供错误行导出）。

---

## 2. 自动勾稽与差异处理

### 2.1 两侧数据定义

| 侧 | 来源 | 纳入条件 | 标识 | 金额（分） | 时间 |
|---|---|---|---|---|---|
| **内部侧 A1**（系统账） | `fund_flows`（SALE 流水） | `deleted=0 AND status=2 AND flow_direction=1` | `flow_id` / `flow_no`（FFyyyyMMddNNNN） | `amount` | `business_date` + `create_time`（Asia/Shanghai） |
| **内部侧 A2**（渠道细节） | `order_payment_records` | `deleted=0` | `payment_id` / `transaction_no`（渠道流水号，可空） | `payment_amount` | `payment_time` |
| **外部侧 B**（银行账） | `bank_transactions`（§5.3） | `deleted=0` | `transaction_id` / `external_txn_id` | `amount` | `txn_time` |

**两侧关系**：一笔订单可有**多条**支付记录（混合支付），对应一条或多条 SALE 流水；勾稽以「订单 + 支付方式」为聚合粒度（§2.3 L2 与附录 B）。

### 2.2 匹配分级（窗口与阈值）

**结论：自动勾稽只允许 1:1（已裁决 5）；1:N / N:1 / N:M 一律人工。**

| 级别 | 条件（同 `account_id` 为前提） | 结果 | 置信度 |
|---|---|---|---|
| **L1 精确** | `order_payment_records.transaction_no = bank_transactions.external_txn_id` 且金额相等 | `AUTO_MATCHED`，`match_level=L1` | 1.00 |
| **L2 唯一命中** | 金额相等 且 `\|txn_time − 业务日\| ≤ 时间窗`（**默认 ±10 分钟**，`app.finance.recon.time-window-minutes=10`）且候选**唯一** | `AUTO_MATCHED`，`match_level=L2` | 0.90 |
| **L2 多候选** | 同上但候选 >1（同账户同日同额多笔） | `MATCHED`（待人工），差异类型 `AMBIGUOUS` | — |
| **L3 建议** | 金额差 ≤ `max(1 分, 0.5% × 内部金额)`（手续费容忍，TD-27）且时间窗放宽至 −1 天 ~ +3 天且对方户名相似度 ≥ 0.6 | `UNMATCHED` + `suggested_external_ref`（**永不自动核销**） | ≤0.6 |

**手续费口径（已裁决 6）**：渠道手续费是**独立字段** `bank_transactions.fee`，**不并入交易金额**；判金额差时**先扣手续费再比**：`内部金额 == 外部金额 + fee` 即视为平账（差额记 `CHANNEL_FEE` 差异，仍需人工核销）。

**多对多形态处理**

| 形态 | 场景 | 处理 |
|---|---|---|
| 1:1 | 单笔订单单渠道支付 ↔ 一笔到账 | L1/L2 自动 |
| 1:N | 一次收款被拆多笔到账（少见） | 人工「合并匹配」：`SUM(外部) = 内部` 且笔数 ≤ 3、窗口 ≤ 3 天 |
| N:1 | 平台 T+1 汇总打款 | 人工「汇总匹配」：`SUM(内部) = 外部`，同账户同业务日区间；生成 N 条 item 指向同一外部 |
| N:M | 日汇总 + 手续费差 | **禁止自动**，落 `EXCEPTION` 走人工 |

### 2.3 主体状态机（`recon_items.subject_status`）

```
UNMATCHED ──L1/L2唯一──▶ AUTO_MATCHED ──人工复核(可选)──▶ MANUAL_MATCHED
    │                        └──复核不通过──▶ UNMATCHED（解绑回池）
    ├──L2 多候选──▶ MATCHED(待确认) ──人工确认──▶ MANUAL_MATCHED
    ├──L3 建议────▶ UNMATCHED(suggested_external_ref 非空)
    └──认定不可对账──▶ EXCEPTION
```

| 状态 | 含义 | 计入勾稽率 | 计入「已处理」 |
|---|---|---|---|
| `UNMATCHED` | 未匹配（可能带建议） | 否 | 否 |
| `MATCHED` | 有候选但**待人工确认** | **否（已裁决 7，避免虚增勾稽率）** | 否 |
| `AUTO_MATCHED` | 系统自动确认 | 是 | 是 |
| `MANUAL_MATCHED` | 人工确认（含 1:N/N:1 合并） | 是 | 是 |
| `EXCEPTION` | 显式排除（测试流水、已冲正等） | 否 | 是 |

**禁止**：`EXCEPTION → AUTO_MATCHED`（只能人工改 `MANUAL_MATCHED`）。

### 2.4 差异类型枚举（`recon_differences.difference_type`）

| code | 名称 | 判定 | 典型动作 |
|---|---|---|---|
| `AMOUNT_DIFF` | 金额差 | 已关联但 `内部金额 <> 外部金额 + fee` | 核销差额 / 冲正差额 |
| `TIME_DIFF` | 时间差 | 金额一致但 `txn_time` 超窗（跨日到账） | 挂账，**不改业务日** |
| `ONE_SIDED_INTERNAL` | 单边账（只有内部） | A 有、B 无（已过窗） | 挂待查；超 T+N 天升级短款 |
| `ONE_SIDED_EXTERNAL` | 单边账（只有外部） | B 有、A 无 | 疑似未入账/他人打款，人工认领 |
| `DUPLICATE` | 重复入账 | 同 `order_id` 同金额多条 SALE，或同 `external_txn_id` 重复导入 | 冲正多余那条 |
| `CHANNEL_FEE` | 渠道手续费差 | `外部金额 = 内部金额 − fee`（fee>0） | 差额记财务费用，**不放宽金额匹配** |
| `AMBIGUOUS` | 多候选 | L2 命中 >1 | 人工二选一 |

> 差异类型是**枚举 code**，中文名放前端字典，不进 DB 主字段。

> **跨日时间差窗口径（BL-7）**：同账户 + 金额完全相等 + |Δt| ≤ `app.finance.recon.cross-day-window-minutes`（默认 1440 分钟 = 24h）且不满足常规 10 分钟窗 → 两侧 `MATCHED` + `TIME_DIFF`（差 0）；超窗 → `ONE_SIDED_*`；窗内多笔同额 → `AMBIGUOUS`。

### 2.5 差异处理流程与冲正规则

```
[1] 跑批 ─▶ recon_items（全量主体）+ recon_differences（仅差异）
[2] 差异池 ──认领──▶ status=ASSIGNED（assignee_id）
[3] 处理三选一：核销 WRITE_OFF / 冲正 REVERSE / 豁免 EXEMPT
[4] 关闭 status=CLOSED（resolution_by + resolution_time + remark 必填）
[5] 审计 finance_audit_logs（who / when / 前后值 / 依据单号）
```

**硬规则（已裁决 8、10）**

1. **冲正只新增反向流水**：`fund_flows` 新增一条方向相反、金额相等、`remark` 带 `[reconReverseOf=<原 flow_no>]` 的流水；**原流水置 `status=3`（已取消）**；
2. **禁止物理删除、禁止改历史金额**；反向流水 `balance_after` 由余额原子更新计算，**不得手填**；
3. **已 CLOSED 的差异不可原地修改**，只能通过 `reopen_of=<difference_id>` **新建**一条重开记录，审计链单向可追；
4. 任何处理动作必须能追溯到 `run_id`；
5. `resolve` 不带 `remark` → 400；核销差额是否生成费用流水见 TD-28。

### 2.6 跑批（`recon_runs`）与幂等

**批次键**：`run_key = RECON-<account_id>-<yyyyMMdd>`（UNIQUE）。

| 场景 | 行为 |
|---|---|
| 同日同账户首次跑 | 建 run（RUNNING）→ 生成 items/差异 → FINALIZED |
| 同日同账户再跑（已 FINALIZED） | **不新建**，返回既有 run + `reused:true`（HTTP 200，不报错） |
| 已 FINALIZED 要求重算 | 必须 `?force=true` + 权限 `finance:bank:recon` → 新建 `rebuild_of=<run_id>` 的 run，**旧 run 保留** |
| 跑批中途失败 | run 保持 RUNNING；超 30 分钟由定时任务置 FAILED；重跑生成新 run，未认领的 items 按 run_id 级联清理 |
| 差异去重 | UNIQUE `(run_id, difference_type, internal_ref, external_ref)`，同 run 内重复插入被吞 |
| 跨日到账 | **不新建 run**，在原业务日 run 里记 `TIME_DIFF`（TD-29） |
| 跑批期间新增流水 | 按 `create_time <= run.started_at` 快照，不追加本 run；次日 run 覆盖 |

### 2.7 接口草案（勾稽模块）

| # | 方法 & 路径 | 用途 | 关键参数 |
|---|---|---|---|
| 1 | `POST /v1/finance/recon-runs` | 触发跑批 | `{accountId, businessDate, force?}` → 幂等返回 `reused:true` |
| 2 | `GET /v1/finance/recon-runs` | 批次列表 | `businessDate / accountId / status / page / size` |
| 3 | `GET /v1/finance/recon-runs/{runId}` | 批次详情（含勾稽率） | — |
| 4 | `GET /v1/finance/recon-items` | 主体列表 | `runId / subjectStatus / side / keyword` |
| 5 | `GET /v1/finance/recon-differences` | 差异池 | `status / type / runId / assignee` |
| 6 | `POST /v1/finance/recon-differences/{id}/claim` | 认领 | — |
| 7 | `POST /v1/finance/recon-differences/{id}/resolve` | 处理 | `{resolution: WRITE_OFF\|REVERSE\|EXEMPT, remark(必填), reverseFlowNo?}` |
| 8 | `POST /v1/finance/recon-items/{id}/match` | 人工勾稽（支持 1:N/N:1） | `{externalRef, groupKey?}` |
| 9 | `POST /v1/finance/recon-items/{id}/unmatch` | 解绑（仅 MANUAL_MATCHED → UNMATCHED） | — |
| 10 | `GET /v1/finance/recon-runs/{runId}/export` | 导出明细 | xlsx |

统一响应 `{code,message,data,timestamp}`；分页 `records/total/current/size`。

### 2.8 页面线框（对账工作台 `/finance/recon`）

```
┌────────────────────────────────────────────────────────────────────────────┐
│ 财务对账                                    [导入银行流水] [发起对账]      │
├────────────────────────────────────────────────────────────────────────────┤
│ 业务日 [2026-10-24 ▾]   账户 [现金户/工行 ****1234 ▾]   状态 [全部 ▾] [查询]│
├────────────────────────────────────────────────────────────────────────────┤
│ 内部 617 笔 / 7,823.30 元    外部 615 笔 / 7,801.30 元    勾稽率 99.6%      │
│ 差异 3 条：金额差 1 · 单边(内) 1 · 渠道手续费差 1                            │
├──────────────┬────────┬─────────┬───────────────┬─────────────────────────┤
│ 类型         │ 笔数   │ 金额差  │ 状态          │ 操作                    │
│ 金额差       │ 1      │ -0.30   │ 待认领        │ [认领][详情]            │
│ 单边账(内)   │ 1      │ 22.00   │ 处理中(@张三) │ [冲正][豁免]            │
│ 渠道手续费差 │ 1      │ -1.50   │ 待认领        │ [认领][详情]            │
└──────────────┴────────┴─────────┴───────────────┴─────────────────────────┘
```

差异详情抽屉：内部（流水号/金额/时间）↔ 外部（流水号/金额/时间）+ 差额（分与元双显示）+ 建议成因 + 处理方式单选（核销/冲正/豁免）+ 说明（必填）+ 冲正必填原流水号 + 「提交处理」。

### 2.9 边缘案例（E2）

| 编号 | 场景 | 处理 |
|---|---|---|
| E2-1 | 23:58 销售、次日 00:03 到账 | 记 `TIME_DIFF`，不新建 run |
| E2-2 | 同一笔重复导入 | `raw_hash`/幂等键命中 → `bank_transactions` 增量 0 |
| E2-3 | 平台汇总打款 + 手续费 | N:1 人工汇总 + `CHANNEL_FEE` |
| E2-4 | 混合支付订单 | 内部聚合为一条 item（`payment_methods=1,3`），外部多笔走 1:N 人工 |
| E2-5 | `payment_method=0`（未记录） | **不得当现金**；UI 提示「渠道未知」，仅允许人工勾稽 |
| E2-6 | 部分支付（`payment_status=1`） | 不参与当日勾稽（TD-30） |
| E2-7 | 退款冲抵 | 本期仅对收入侧勾稽；退款纳入范围见 TD-31 |
| E2-8 | 账户停用/换卡 | 历史到账仍可认领；是否允许新 run 见 TD-32 |
| E2-9 | 跑批期间新增流水 | 按 `started_at` 快照，不追加（§2.6） |
| E2-10 | 金额精度（12.345 元） | 拒绝整批并报行号，禁止四舍五入 |
| E2-11 | 同 `external_txn_id` 打到两个账户 | `DUPLICATE`，人工裁决归属 |
| E2-12 | 时区 | 一律 Asia/Shanghai；带时区先换算（TD-12 已裁决） |

---

## 3. 现金交款单闭环

### 3.1 起点与边界

**起点**：`pos_shifts` 交班完成（`status=ended` 且 `closing_cash` 已录入，单位**元**）。
**边界**：只处理**现金**（`order_payment_records.payment_method=1`）与「现金户 → 银行户」的实物流转；微信/支付宝/银行卡走平台清算，不在本闭环。

### 3.2 应交金额口径（已裁决 3）

**结论：应交金额以 `order_payment_records` 中该班次现金支付合计**重算**为权威；`pos_shifts.closing_cash` 仅作参考，二者不一致记为差异并留痕，禁止直接改 `closing_cash`。**

```
expected_amount（分）
  = SUM(order_payment_records.payment_amount)
    WHERE payment_method = 1                 -- 只认现金
      AND order_id ∈ 该班次订单             -- pos_shifts.start_time <= orders.payment_time <= pos_shifts.end_time
      AND orders.payment_status = 2 AND orders.deleted = 0
  − 班内现金退款金额（退款口径见 TD-31）
```

| 对比项 | 权威性 | 用途 |
|---|---|---|
| `expected_amount`（重算） | **权威** | 判定短款/长款 |
| `pos_shifts.closing_cash`（元） | 仅参考 | 与 `actual_amount` 并列展示；差 3.30 元即标红 |

### 3.3 流程与状态机

```
[交班 ended] ─(系统)生成──▶ DRAFT（expected_amount 已算）
      │  收银员核对 → submit
      ▼
  SUBMITTED ──出纳点钞录 actual_amount──▶ RECEIVED ──存行登记──▶ DEPOSITED ──财务复核──▶ VERIFIED ──归档──▶ CLOSED
      │                                       │
      └──reject──▶ REJECTED ──resubmit──▶ SUBMITTED（新记录，旧记录保留）
                                              └──reverse──▶ REVERSED（整单冲正：生成反向流水，不删单）
```

**禁止**：`RECEIVED` 之后回退 `DRAFT`；任何状态都不允许修改 `expected_amount`（要改只能作废重开）。

### 3.4 与 `fund_flows` 的关系（已裁决 9，三段式只记一次收入）

| 阶段 | 是否生成 `fund_flows` | 口径 |
|---|---|---|
| ① 支付时（**已实现**） | 生成 **1 条 SALE 收入**（现金户） | `flow_direction=1`、`flow_category=1`、`order_id` 关联、`amount`=支付金额（分） |
| ② 缴款单 `RECEIVED`（出纳收妥） | **不生成任何流水** | 现金只在收银员→出纳之间转移，现金户余额未变；生成即重复计收入 |
| ③ 存行 `DEPOSITED` | 生成 **2 条内部转账** | 现金户 `flow_direction=2`、银行户 `flow_direction=1`，两者 `flow_category=5`（内部转账），金额均 = `actual_amount`；幂等键写在 `remark`：`[depositSlip=<slip_no>]`；两账户余额各自原子更新 |

**例外补记**：若某班次 SALE 现金流水缺失（历史欠账/上游失败），在 `RECEIVED` 时补记一条 SALE 收入（`flow_category=1`，幂等键 `[posShift=<shift_id>][cashBackfill]`，`create_user_id=0`），**UI 必须标注「系统补记」**；③ 仍照常生成内部转账，不重复计收入。

### 3.5 差额处理（短款/长款）

| 情形 | 定义 | 处理 | 禁止 |
|---|---|---|---|
| 短款 | `actual_amount < expected_amount`（差额为负） | 差额挂「待查损溢」；出纳+收银员双签；追责方式见 TD-34；单据可继续 `RECEIVED` | 不得改 `pos_shifts.closing_cash` |
| 长款 | `actual_amount > expected_amount`（差额为正） | 差额挂「待查溢余」，**不确认收入**；核实后退回或转营业外（需审批） | 不得直接计入当日营业外收入 |
| 少交/多交（已 RECEIVED 后发现） | — | 走 `reverse` + 重开新单；原单保留 | 不得就地改 `actual_amount` |
| 平账 | 差额 = 0 | 正常推进 | — |

> 差额永远记在 `deposit_slips.difference_amount`（分）；来源数据只读。

### 3.6 接口草案（缴款单模块，11 个）

| # | 方法 & 路径 | 说明 |
|---|---|---|
| 1 | `POST /v1/finance/deposit-slips/generate` | 按 `shiftId` 生成（幂等：已存在返回既有单 + `reused:true`） |
| 2 | `GET /v1/finance/deposit-slips` | 列表 `storeId/status/dateFrom/dateTo/cashierId` |
| 3 | `GET /v1/finance/deposit-slips/{id}` | 详情（差额、审批链、关联流水） |
| 4 | `POST /v1/finance/deposit-slips/{id}/submit` | DRAFT → SUBMITTED |
| 5 | `POST /v1/finance/deposit-slips/{id}/receive` | `{actualAmount(分), dueRemark, shortsReason?}` → RECEIVED |
| 6 | `POST /v1/finance/deposit-slips/{id}/deposit` | `{depositRefNo, bankTxnId, bankAccountId, depositDate}` → DEPOSITED（生成 2 条内部转账） |
| 7 | `POST /v1/finance/deposit-slips/{id}/verify` | 复核 → VERIFIED |
| 8 | `POST /v1/finance/deposit-slips/{id}/close` | 归档 → CLOSED |
| 9 | `POST /v1/finance/deposit-slips/{id}/reject` | `{reason}` → REJECTED |
| 10 | `POST /v1/finance/deposit-slips/{id}/reverse` | `{reason, reverseFlows:true/false}` → REVERSED |
| 11 | `GET /v1/finance/deposit-slips/{id}/export` | 导出单张（PDF/打印视图） |

### 3.7 页面线框（现金交款单）

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 现金交款单  DS202610240007                        状态：RECEIVED             │
├──────────────────────────────────────────────────────────────────────────────┤
│ 门店：测试门店A   班次：2026-10-24 早班(08:00-16:00)   收银员：员工B          │
│ 应交：7,823.30 元    实交：7,820.00 元    差额：-3.30 元（短款） ⚠           │
├──────────────────────────────────────────────────────────────────────────────┤
│ 明细：现金收入 617 笔 ｜ 现金退款 0 笔 ｜ 班结自报 closing_cash 7,820.00 元   │
├──────────────────────────────────────────────────────────────────────────────┤
│ 关联：现金户(account_id=5)   存行账户[工行 ****1234 ▾]   进账单号[________]   │
│      悬空 bank_transactions：[关联]                                          │
├──────────────────────────────────────────────────────────────────────────────┤
│ 历史：[16:05 生成]→[16:20 提交]→[16:40 出纳收妥(差额-3.30)]                  │
│ 说明：短款 3.30 元，已双签确认，挂待查损溢                                    │
│                                   [退回] [存行] [复核通过] [导出]             │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 3.8 边缘案例（E3）

| 编号 | 场景 | 处理 |
|---|---|---|
| E3-1 | 一个班次跨天（22:00 开班、次日 02:00 交班） | `expected_amount` 按**班次时间窗**统计；单据 `business_date` 取交班日（TD-36） |
| E3-2 | 班内现金退款 | 从应交中扣减；非现金退款不扣减 |
| E3-3 | 混合支付订单 | 只计现金部分；`payment_method=0` 不计（宁少不多） |
| E3-4 | 支付记录缺失（历史单） | 该单不计入应交，页面告警「N 笔支付记录缺失，可能低估应交」（与 `consistency_check` A2 同源） |
| E3-5 | `closing_cash` 未填 | 是否允许生成缴款单见 TD-37（建议：生成但禁止 submit） |
| E3-6 | 重复交班 | 同一终端同一时间窗两条 ended 班次 → 仅一条可生成缴款单，另一条标异常人工合并 |
| E3-7 | 出纳与收银员同一人 | 允许但需二次复核（内控，TD-38） |
| E3-8 | 存行金额 ≠ 实交 | 关联的 `bank_transactions.amount` 与 `actual_amount` 不一致 → 拒绝存行，先走差额处理 |
| E3-9 | 跨店/跨账户误选 | `bank_account_id.store_id` 必须等于单据 `store_id`，否则 400 |
| E3-10 | 作废后重开 | 原单 `REVERSED`，新单 `reopen_of` 指向原单；不得复用 `slip_no` |
| E3-11 | 月末结账拦截 | 已结账期间禁止 `deposit/verify`（TD-39） |
| E3-12 | 现金户余额不足 | `balance < actual_amount` → 拒绝并提示上游有未入账现金 |

---

## 4. 资金日记账与银行存款余额调节表

### 4.1 日记账口径（账户 × 业务日）

| 列 | 计算（全部为**分**） |
|---|---|
| 期初余额 | 该账户在 `business_date` 之前最后一笔流水的 `balance_after`；无历史取账户初始余额 |
| 收入合计 | `SUM(amount) WHERE flow_direction=1 AND status=2` |
| 支出合计 | `SUM(amount) WHERE flow_direction=2 AND status=2` |
| 当日净额 | 收入 − 支出 |
| 期末余额 | 期初 + 当日净额；**必须等于**该日最后一笔 `balance_after`（V1） |
| 笔数 | `status=2` 的行数；`status=1`（待处理）/`3`（已取消）单独计数、不计金额 |

**日结 / 月结**：日结逐日聚合（粒度见 TD-20，期间口径见 TD-21）；月结 = 月内各日汇总 + 月初/月末余额，**月末余额 = 次月 1 日期初**（V2）；期间封账后只读，补录须走反结账（另设计）。

### 4.2 三层余额与「谁是真值」（口径见 TD-23）

| 层 | 载体 | 语义 | 真值地位 |
|---|---|---|---|
| 账面层 | `fund_flows.balance_after`（分） | 每笔流水后的账面余额 | **企业账真值**（链路完整时） |
| 账户层 | `bank_accounts.balance`（分）+ `version` | 账户当前余额（原子更新维护） | 派生缓存，应与最后一笔 `balance_after` 一致（V3） |
| 事实层 | `bank_transactions.balance_snapshot`（分，可空） | 银行回单给的交易后余额 | **银行账真值**（对账单侧） |

| 校验 | 断言 | 不等含义 |
|---|---|---|
| V1 | 日记账期末 == 当日最后一笔 `balance_after` | 流水链断裂/并发丢失 |
| V2 | 月末余额 == 次月 1 日期初 | 期间切分开错日 |
| V3 | `bank_accounts.balance` == 最新一笔 `balance_after` | 账户余额与流水脱钩（历史漂移，TD-22） |

> **V3 豁免规则（白名单式，唯一口径；2026-10-24 收窄）**：仅下列两类行豁免 V3 比对 ——
> ① `flow_no LIKE 'FFH%'`（历史补账流水，前缀即标记）；② `created_by LIKE 'system/backfill-%'`（补账脚本审计名）。
> 豁免理由：补账行 `balance_after` **按设计写 0**（补账不回写余额，避免篡改上线后真实余额）。
> **反例（不得豁免）**：普通流水 `balance_after = 0` 仍参与 V3 比对；若它成为某账户最新一笔流水，V3 必须报红（真实不一致），不准放宽规则、不准改数据。
> V3 取「最新一笔**非豁免**流水」的 `balance_after` 与 `bank_accounts.balance` 比较；若账户全部流水均为豁免行，判 **N/A** 并单列。
> 落地：`scripts/qa/pc7_balance_baseline.sql`（第 3/4 段分别输出豁免行数与普通 0 值清单）+ `consistency_check.py` 的 **A7**（参与判定）/ **A8**（INFO）。
>
> **V3 修复方向（单向，TD-23 附带裁决）**：不一致时**只允许用最新 `balance_after` 回填 `bank_accounts.balance`（覆盖缓存）并写校准审计**；**禁止反向修改 `fund_flows.balance_after`**（这是「不改历史金额」红线的延伸，历史快照一旦被改写，审计链即失效）。校准执行记录：改前/改后值、账户、依据流水号、操作人、时间。
| V4 | `balance_after` 与当日最后 `balance_snapshot` 之差 == 当日未达账项净额 | 调节表要解释的差 |

### 4.3 调节表结构与四类未达账项

```
银行存款余额调节表（账户 招商银行 ****8821，期间 2026-10-01 ~ 10-31，币种 CNY）
──────────────────────────────────────────────────────────────
  ① 企业账面余额（book_balance）                        ¥ 12,345.67
  ② 加：银行已收、企业未收        （C 类，企业侧加）    ¥    500.00
  ③ 减：银行已付、企业未付        （D 类，企业侧减）    ¥    120.00
  ④ 加：企业已收、银行未收        （A 类，银行侧加）    ¥    300.00
  ⑤ 减：企业已付、银行未付        （B 类，银行侧减）    ¥     80.00
──────────────────────────────────────────────────────────────
  企业调节后余额 = ① + ② − ③ = book_balance + C − D     ¥ 12,725.67
  对账单余额（bank_balance）                            ¥ 12,505.67
  银行调节后余额 = 对账单 + ④ − ⑤ = bank_balance + A − B ¥ 12,725.67  ← 必须相等
  （12,505.67 + 300.00 − 80.00 = 12,725.67；与企业侧 12,345.67 + 500.00 − 120.00 恒等）
  差异                                                  ¥      0.00
──────────────────────────────────────────────────────────────
```

> **四类未达账项与字段字母的固定映射（全文唯一口径，§5.10.1 同名）**：
> **A** = 企业已收、银行未收（银行侧加）；**B** = 企业已付、银行未付（银行侧减）；
> **C** = 银行已收、企业未收（企业侧加）；**D** = 银行已付、企业未付（企业侧减）。
> 恒等式：`book_balance + C − D = bank_balance + A − B`（即上例两条调节后余额必须相等，V5）。
| 类 | 名称 | 自动建议线索 | 调整方向 |
|---|---|---|---|
| 1 | 企业已收、银行未收 | `fund_flows` 有进账、同期 `bank_transactions` 无对应 | 银行侧加 |
| 2 | 企业已付、银行未付 | `fund_flows` 有出账、同期无对应 | 银行侧减 |
| 3 | 银行已收、企业未收 | `bank_transactions` 有进账、`fund_flows` 无对应（利息/代收） | 企业侧加 |
| 4 | 银行已付、企业未付 | `bank_transactions` 有出账、无对应（手续费/代扣） | 企业侧减 |

**硬校验**（未达账项载体见 TD-24，类别范围见 TD-25）

| 编号 | 规则 |
|---|---|
| V5 | 调节后两侧余额必须相等；不等 → 只能存 `DRAFT`，**禁止 `SUBMIT` 与 `APPROVED`**（`is_balanced=false` 时的硬门禁） |
| V6 | 每条未达账项必须有「挂账人 + 原因 + 期间」，且按 `category` 的金额合计与主表四列 `uncleared_*` 一致（落地为 AC-43） |
| V7 | 同一笔（`flow_id` / `transaction_id`）在同一期间只出现一次，不得跨期重复挂账 |
| **V8** | **审核人硬约束（四眼，不可配置关闭）**：`approved_by ≠ submitted_by`，且 `approved_by` 对应用户**不得持有 `ROLE_TELLER`（出纳）**；违反 → 拒绝 `APPROVED`（业务错误，不是前端提示） |

> **硬规则一：调节表不调账（与「冲正只增不删」同级，不得违反）**
> 调节表的**生成 / 重算 / 提交 / 审核**动作 **不产生、也不修改任何 `fund_flows` 流水**，不改 `bank_accounts.balance`。
> 未达账项只在 `reconciliation_items` 中**挂账披露**（A/B 类引用已有 `flow_id`、C/D 类引用已有 `bank_transactions.transaction_id`，仅做两侧事实核对，**不创造新的账面事实**）。
> 调节表**不是原始凭证**，不得作为调整账面记录的记账依据；未达账项待实际结算凭证到达后，按正常入账流程另行发起（走 §2 的差异核销 / 冲正通道，且冲正仍只增反向流水）。

> **硬规则二：出纳编表必须由他人复核（规范例外口径）**
> 《会计基础工作规范》未把「编制银行存款余额调节表」列入出纳的禁止性列举，但审计/内控口径要求：出纳**一般不得**同时从事对账单获取与调节表编制；**确需出纳办理的，必须由其他人员定期审核监督**。
> 因此系统**保留**出纳的 `finance:bank:recon`（允许编制草稿与提交），但 `APPROVED` 必须由**非本人、且非出纳角色**的人员完成（= V8 硬门禁）。

### 4.4 接口草案（日记账 / 调节表）

| # | 方法 & 路径 | 用途 | 关键参数 |
|---|---|---|---|
| 1 | `GET /v1/finance/ledger/daily` | 日记账（账户×日） | `accountId`(必填)、`from`、`to`、分页 |
| 2 | `GET /v1/finance/ledger/summary` | 月结汇总 | `accountId`、`month`(yyyy-MM) |
| 3 | `POST /v1/finance/recon/statements` | 生成/重算调节表草稿 | `{accountId, periodStart, periodEnd, bankStatementBalanceFen}` |
| 4 | `GET /v1/finance/recon/statements` | 调节表列表 | `accountId / period / status` |
| 5 | `GET /v1/finance/recon/statements/{id}` | 详情（四类未达 + `isBalanced`） | — |
| 6 | `POST /v1/finance/recon/statements/{id}/submit` | DRAFT → SUBMITTED | `{remark}` |
| 7 | `POST /v1/finance/recon/statements/{id}/approve` | SUBMITTED → APPROVED | `{remark}`；`isBalanced=false` → 业务错误 |
| 8 | `GET /v1/finance/recon/unmatched` | 未匹配池（两侧） | `accountId、from、to、side(BOOK\|BANK)` |

### 4.5 页面线框

```
┌─ 资金日记账 ─────────────────────────────────────────────────────────┐
│ [账户 ▼] [期间 2026-10-01 ~ 10-31] [日结|●月结]          [导出 CSV] │
│ 期初 ¥12,000.00 │ 收入 ¥3,200.00 │ 支出 ¥2,854.33 │ 期末 ¥12,345.67 │
│ ┌ 日期  │ 期初 │ 收入 │ 支出 │ 净额 │ 期末  │ 笔数 │ 校验 ┐        │
│ │ 10-30 │ 9,800│  600 │1,200 │ -600 │ 9,200 │  12  │ ✔ V1 │        │
│ │ 10-31 │ 9,200│3,145 │ 0.00 │3,145 │12,345 │  31  │ ✔ V1 │        │
│ └──────────────────────────────────────────────────────────────┘    │
│ 提示条：V3 账户余额与流水一致 ✔                                      │
└──────────────────────────────────────────────────────────────────────┘

┌─ 银行存款余额调节表（草稿）──────────────────────────────────────────┐
│ 账户：招商银行 ****8821  期间：2026-10-01 ~ 10-31  [重算] [提交]      │
│ 企业账面 ¥12,345.67  ↔  对账单 ¥12,505.67                             │
│ ┌ 未达账项 ─────────────────────────────────────────────────────────┐│
│ │ ① 企业已收银行未收 │ 2 笔 │ +300 │ [查看][移除]                    ││
│ │ ② 企业已付银行未付 │ 1 笔 │  -80 │ [查看]                          ││
│ │ ③ 银行已收企业未收 │ 1 笔 │ +500 │ [查看][新增挂账]                ││
│ │ ④ 银行已付企业未付 │ 1 笔 │ -120 │ [查看]                          ││
│ └───────────────────────────────────────────────────────────────────┘│
│ 企业调节后 ¥12,725.67  银行调节后 ¥12,725.67  差异 ¥0.00 ✔平衡       │
│ 未平衡：仅可存草稿，[提交] 置灰并提示「差异 ¥X 未解释」              │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.6 本轮不实现（日记账/调节表）

- 期末结账/反结账审批流（另设计）；
- 调节表 PDF/打印模板（首版只在线查看 + CSV 导出）；
- 未达账项跨期自动清账；
- 多币种调节。

---

## 5. 数据模型总览（全部新表集中于此，字段级）

> **本章是唯一权威定义**：§1/§2/§3 只引用本章，不重复列字段。全部 **9 张表**（P0 的 §5.1–§5.7 共 7 张 + P1 的 §5.10 共 2 张）在活库中**均不存在**（2026-10-24 `to_regclass` 实测为空）；落地一律走 Flyway（只增不改，禁止手写 DDL 脚本）。

### 5.1 `import_batches`（导入批次）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `batch_id` | BIGINT PK | 是 | 批次ID |
| `batch_no` | VARCHAR(32) | 是 | 业务号 `IByyyyMMddNNNN`，UNIQUE |
| `source_type` | VARCHAR(16) | 是 | `WECHAT` / `ALIPAY` / `BANK`（是否复用既有 `bank_payment_records` 见 TD-26） |
| `account_id` | BIGINT | 是 | → `bank_accounts.account_id` |
| `file_name` | VARCHAR(255) | 是 | 原始文件名 |
| `file_sha256` | VARCHAR(64) | 是 | 文件指纹（重复上传提示） |
| `encoding_used` | VARCHAR(16) | 否 | `UTF-8` / `GB18030` |
| `period_start` / `period_end` | DATE | 否 | 文件覆盖日期范围（解析后回填） |
| `total_rows` / `ok_rows` / `dup_rows` / `error_rows` | INTEGER | 是 | `ok+dup+error = total` |
| `status` | VARCHAR(16) | 是 | `UPLOADED` / `PARSED_OK` / `PARSED_FAILED` / `COMMITTED` / `PARTIAL` / `FAILED` / `CANCELLED` |
| `error_summary` | TEXT | 否 | 整批失败摘要 |
| `create_user_id` / `created_by` | BIGINT / VARCHAR(64) | 是 | 审计（与现状一致） |
| `create_time` / `update_time` / `deleted` / `version` | — | 是 | 项目通用列 |

### 5.2 `import_rows`（行级明细与回执）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `row_id` | BIGINT PK | 是 | 行ID |
| `batch_id` | BIGINT | 是 | → `import_batches` |
| `row_no` | INTEGER | 是 | **文件内行号**（含偏移，便于对照原文） |
| `raw_json` | JSONB | 否 | 原始行（脱敏后可存，TD-18） |
| `status` | VARCHAR(16) | 是 | `OK` / `DUPLICATE` / `ERROR` / `NEEDS_REVIEW` |
| `error_code` / `error_message` | VARCHAR(32) / VARCHAR(255) | 否 | 可读原因（`AMOUNT_FORMAT` / `ENCODING` / `FUTURE_TIME` …） |
| `duplicate_of_row_no` | INTEGER | 否 | 批次内重复指向 |
| `matched_transaction_id` | BIGINT | 否 | 与库内既有流水重复时指向 `bank_transactions.transaction_id` |
| `transaction_id` | BIGINT | 否 | 成功入库后的 `bank_transactions.transaction_id` |

### 5.3 `bank_transactions`（外部流水事实表 —— 权威定义）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `transaction_id` | BIGINT PK | 是 | 主键（B 稿的 `bank_transactions.id` 统一改称 `transaction_id`，与 `account_id`/`flow_id` 命名一致） |
| `batch_id` | BIGINT | 是 | → `import_batches.batch_id`（整批可回滚） |
| `account_id` | BIGINT | 是 | → `bank_accounts.account_id`（账户级勾稽锚点） |
| `source_type` | VARCHAR(16) | 是 | `WECHAT` / `ALIPAY` / `BANK` |
| `external_txn_id` | VARCHAR(64) | 否 | 渠道唯一号 / 银行流水号（L1 精确匹配首选键，TD-13） |
| `merchant_order_no` | VARCHAR(64) | 否 | 商户单号（可反查 `orders.order_number` / `order_code`） |
| `direction` | SMALLINT | 是 | **1 收入 / 2 支出**（与 `fund_flows.flow_direction` 同口径）；退款另用 `is_refund` |
| `is_refund` | BOOLEAN | 是 | 是否退款行（退款不并入支出，TD-31） |
| `amount` | BIGINT | 是 | **分**，恒正数（方向由 `direction` 表达，禁止元混入） |
| `fee` | BIGINT | 否 | **分**，渠道手续费，**独立字段不并入 `amount`**（裁决 6） |
| `txn_time` | TIMESTAMP | 是 | 银行/渠道记账时间（Asia/Shanghai） |
| `business_date` | DATE | 是 | 业务日（按 `txn_time` 自然日，TD 见 §9） |
| `balance_snapshot` | BIGINT | 否 | 银行回单余额（分，可空，用于连续性校验 V4） |
| `counterparty_name` / `counterparty_account` | VARCHAR(100) / VARCHAR(64) | 否 | 对方户名 / 账号 |
| `summary` | VARCHAR(255) | 否 | 摘要/用途 |
| `status_raw` | VARCHAR(64) | 否 | 渠道原始状态串 |
| `settle_status` | VARCHAR(16) | 否 | 归一：`SETTLED` / `REFUNDED` / `FAILED` / `UNKNOWN` |
| `dedupe_basis` | VARCHAR(16) | 是 | `EXTERNAL_ID` / `COMPOSITE`（TD-12） |
| `raw_hash` | VARCHAR(64) | 是 | 原始行哈希（导入级去重） |
| `match_status` | VARCHAR(16) | 是 | `UNMATCHED` / `MATCHED` / `AUTO_MATCHED` / `MANUAL_MATCHED` / `EXCEPTION`（与 `recon_items.subject_status` 同枚举） |
| `matched_flow_id` | BIGINT | 否 | 勾稽到的 `fund_flows.flow_id` |
| `needs_review` | BOOLEAN | 是 | 需人工确认（如退款无关联键） |
| `create_user_id` / `create_time` / `update_time` / `deleted` / `version` | — | 是 | 项目通用列 |

**建议索引/约束**

| 对象 | 定义 | 用途 |
|---|---|---|
| UNIQUE | `(account_id, source_type, external_txn_id)` WHERE `external_txn_id IS NOT NULL` AND `deleted=0` | 主幂等键（方案 C，TD-12） |
| UNIQUE | `(account_id, txn_time, amount, counterparty_account)` WHERE `dedupe_basis=COMPOSITE` | 兜底幂等 |
| UNIQUE | `(raw_hash)` 同一批次内由 `import_rows` 把关，表级不做 | 避免跨来源合法同内容误伤 |
| INDEX | `(account_id, business_date)` / `(match_status)` / `(batch_id)` | 勾稽与列表 |

### 5.4 `recon_runs`（对账批次）

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| `run_id` | BIGINT PK | — |
| `run_key` | VARCHAR(64) | UNIQUE，`RECON-<account_id>-<yyyyMMdd>` |
| `account_id` | BIGINT | NOT NULL，→ `bank_accounts` |
| `store_id` | BIGINT | 冗余自 `bank_accounts.store_id`（GLOBAL 账户为空） |
| `business_date` | DATE | 对账业务日 |
| `status` | VARCHAR(16) | `RUNNING` / `FINALIZED` / `FAILED` |
| `internal_count` / `internal_amount` | INT / BIGINT | 内部侧条数 / 合计（分） |
| `external_count` / `external_amount` | INT / BIGINT | 外部侧条数 / 合计（分） |
| `matched_count` / `matched_amount` | INT / BIGINT | 已勾稽（仅 AUTO + MANUAL，裁决 7） |
| `difference_count` | INT | 差异条数 |
| `rebuild_of` | BIGINT | 被重算的 `run_id`（可空） |
| `started_at` / `finished_at` | TIMESTAMP | 跑批时间 |
| `operator_id` | BIGINT | 触发人；定时任务为 0 |
| `create_time` / `update_time` | TIMESTAMP | — |

### 5.5 `recon_items`（勾稽主体）

| 字段 | 类型 | 说明 |
|---|---|---|
| `item_id` | BIGINT PK | — |
| `run_id` | BIGINT | → `recon_runs`，NOT NULL |
| `side` | VARCHAR(8) | `INTERNAL` / `EXTERNAL` |
| `internal_ref` | VARCHAR(64) | 内部侧填 `flow_no`（SALE 流水号） |
| `external_ref` | VARCHAR(64) | 外部侧填 `external_txn_id` 或 `bank_transactions.transaction_id` |
| `payment_methods` | VARCHAR(16) | 内部侧支付方式集合（如 `1,2`） |
| `amount` | BIGINT | 分 |
| `txn_time` | TIMESTAMP | Asia/Shanghai |
| `counterparty_name` | VARCHAR(100) | 关键词相似度用 |
| `subject_status` | VARCHAR(16) | 五态（§2.3） |
| `matched_since` | TIMESTAMP | 进入 `MATCHED`（待人工确认）的时刻；**NULL 表示非 MATCHED**。供 AC-42 统计「超期未处理」（如需回填历史，可临时以 `update_time` 代替，但新数据必须写本列） |
| `match_level` | VARCHAR(8) | `L1` / `L2` / `L3` / `MANUAL` / NULL |
| `match_group` | VARCHAR(64) | 合并勾稽分组键（1:N/N:1 相同），NULL=1:1 |
| `suggested_external_ref` | VARCHAR(64) | L3 建议 |
| `confidence` | NUMERIC(4,3) | 0~1 |
| `create_time` / `update_time` | TIMESTAMP | — |

约束：UNIQUE `(run_id, side, COALESCE(internal_ref,「」), COALESCE(external_ref,「」))` 防同一主体重复落 item。

### 5.6 `recon_differences`（差异池）

| 字段 | 类型 | 说明 |
|---|---|---|
| `difference_id` | BIGINT PK | — |
| `run_id` | BIGINT | NOT NULL |
| `difference_type` | VARCHAR(24) | §2.4 七类枚举 |
| `internal_ref` / `external_ref` | VARCHAR(64) | 至少一个非空 |
| `amount_internal` / `amount_external` | BIGINT | 分 |
| `diff_amount` | BIGINT | `amount_external − amount_internal`（分） |
| `status` | VARCHAR(16) | `OPEN` / `ASSIGNED` / `RESOLVING` / `CLOSED` / `EXEMPT` / **`PENDING_P2_REV`**（REVERSE 决议的 P1 落地值；列宽 16 装不下 `PENDING_P2_REVERSE`(18)，权威标记取 `resolution='REVERSE'` + `reverse_flow_no IS NULL`；P2 若需完整名须先放宽列宽，属 §5.6 结构变更） |
| `resolution` | VARCHAR(16) | `WRITE_OFF` / `REVERSE` / `EXEMPT` |
| `resolution_remark` | VARCHAR(500) | CLOSED 时必填 |
| `reverse_flow_no` | VARCHAR(32) | 冲正产生的反向流水号 → `fund_flows.flow_no` |
| `assignee_id` / `assigned_at` | BIGINT / TIMESTAMP | 认领 |
| `resolution_by` / `resolution_time` | BIGINT / TIMESTAMP | 处理 |
| `reopen_of` | BIGINT | 重开来源（裁决 10） |
| `create_time` / `update_time` | TIMESTAMP | — |

约束：UNIQUE `(run_id, difference_type, internal_ref, external_ref)`；INDEX `(status, assignee_id)`。

### 5.7 `deposit_slips`（现金交款单）

| 字段 | 类型 | 说明 |
|---|---|---|
| `slip_id` | BIGINT PK | — |
| `slip_no` | VARCHAR(32) | UNIQUE，`DS<yyyyMMdd><NNNN>` |
| `store_id` | BIGINT | 门店 |
| `shift_id` | BIGINT | → `pos_shifts.shift_id`；**UNIQUE**（一班次一单，TD-33） |
| `cashier_employee_id` / `cashier_name` | BIGINT / VARCHAR(50) | 交款人（来自 `pos_shifts.employee_id/employee_name`） |
| `collector_user_id` / `collector_name` | BIGINT / VARCHAR(50) | 出纳（实收人） |
| `expected_amount` | BIGINT | **分**，应交（§3.2 重算权威） |
| `actual_amount` | BIGINT | **分**，实交（点钞） |
| `difference_amount` | BIGINT | **分**，`actual − expected`（>0 长款，<0 短款） |
| `cash_account_id` | BIGINT | 现金户（`account_type=2`，`scope=STORE`） |
| `bank_account_id` | BIGINT | 存行目标账户（`account_type=1`，同 `store_id`） |
| `deposit_bank_txn_id` | BIGINT | → `bank_transactions.transaction_id`（存行后回填） |
| `deposit_ref_no` | VARCHAR(64) | 银行进账单号 |
| `status` | VARCHAR(16) | `DRAFT` / `SUBMITTED` / `RECEIVED` / `DEPOSITED` / `VERIFIED` / `CLOSED` / `REJECTED` / `REVERSED` |
| `submitted_at` / `received_at` / `deposited_at` / `verified_at` / `closed_at` | TIMESTAMP | 各阶段时间 |
| `reopen_of` | BIGINT | 作废重开来源 |
| `remark` | VARCHAR(500) | 说明（差额原因必填） |
| `create_user_id` / `created_by` / `create_time` / `update_time` / `version` / `deleted` | — | 项目通用列 |

### 5.8 与现状表的引用关系

| 新表.字段 | → 现状表.字段 | 关系 | 备注 |
|---|---|---|---|
| `import_batches.account_id` | `bank_accounts.account_id` | N:1 | 导入必须指定账户 |
| `bank_transactions.account_id` | `bank_accounts.account_id` | N:1 | 勾稽锚点 |
| `bank_transactions.matched_flow_id` | `fund_flows.flow_id` | N:1（可空） | 勾稽结果回填 |
| `recon_runs.account_id` | `bank_accounts.account_id` | N:1 | 同账户同日唯一 run |
| `recon_items.internal_ref` | `fund_flows.flow_no` | 逻辑引用（字符串） | 不建外键，避免历史流水变更阻塞 |
| `recon_items.external_ref` | `bank_transactions.external_txn_id` | 逻辑引用 | 同上 |
| `recon_differences.reverse_flow_no` | `fund_flows.flow_no` | 逻辑引用 | 冲正流水 |
| `deposit_slips.shift_id` | `pos_shifts.shift_id` | 1:1 | 班次一单 |
| `deposit_slips.cash_account_id` / `bank_account_id` | `bank_accounts.account_id` | 2×N:1 | 现金户与存款户 |
| `deposit_slips.deposit_bank_txn_id` | `bank_transactions.transaction_id` | N:1（可空） | 存行后回填 |
| （重算口径）`expected_amount` | `order_payment_records.payment_method=1` | 聚合 | 见 §3.2 |

### 5.9 迁移与回滚策略

| 项 | 策略 |
|---|---|
| 变更通道 | 一律 Flyway（`V*.sql` 只增不改）；**禁止**手写 schema 脚本（项目 PG-003） |
| 顺序 | ① **P0 建 7 张新表**（§5.1–§5.7，纯新增，不改现状表结构）→ ② 建索引/唯一约束 → ③ 权限码入库 → ④ 前端菜单开关 → ⑤ **P1 按 TD-24 裁决追加 §5.10 的调节表两表**（若 TD-24 裁决为其他载体则跳过 ⑤） |
| 现状表影响 | **零结构变更**；`fund_flows` / `bank_accounts` 仅新增写入方（冲正、内部转账），字段不动 |
| 回滚 | 新表无外部依赖 → P0 阶段 `DROP` 7 张新表即可回到基线；若已执行 P1 的 §5.10，需连 `reconciliation_items` / `reconciliation_statements` 一并 `DROP`（数据一并消失）；**前提**：回滚前确认没有已 `DEPOSITED` 的缴款单与已 `CLOSED` 的差异（否则会丢失账务事实） |
| 数据回滚 | 若已生成冲正/内部转账流水：只能再冲正一次（反向），**不得删除历史 `fund_flows` 行** |
| 权限回滚 | 撤销新增权限码 + 从角色解除绑定（保留 `permission_assignment_log` 记录） |
| 灰度 | 建议先「只读」上线：导入 + 勾稽可跑，所有写动作（resolve/deposit）默认关闭，观察 1 个结账周期 |

---

### 5.10 调节表相关表（待 TD-24 裁决后定稿）

> **结论：本节是 §4 调节表功能的落地载体草案（二表），归属 P1，不在 P0 的 7 张表内；两表只承载「核对与披露」，**不承载任何调账动作**（见 §4.3 硬规则一）。若 TD-24 裁决为其他载体（如复用 `fund_flows.remark` / 仅用 `bank_transactions.match_status`），则**整节删除**，并同步改写 §4 的 S3–S9 接口与 V5–V7 校验点。**

#### 5.10.1 `reconciliation_statements`（银行存款余额调节表主表）

| 字段 | 类型 | 说明 |
|---|---|---|
| `statement_id` | BIGINT PK | — |
| `statement_no` | VARCHAR(32) | UNIQUE，`BS<yyyyMM><NNNN>`（如 `BS2026100001`） |
| `account_id` | BIGINT | → `bank_accounts.account_id`，NOT NULL（一个账户一张表） |
| `store_id` | BIGINT | 账户归属门店（`scope=GLOBAL` 的总部账户为 NULL） |
| `period_start` / `period_end` | DATE | 期间起止；首版按自然月（TD-21），`period_end` 为该月最后一日 |
| `book_balance` | BIGINT | **分**，期间末企业账面余额（取 `fund_flows.balance_after` 快照，见 TD-23） |
| `bank_balance` | BIGINT | **分**，期间末银行对账单余额（取 `bank_transactions.balance_snapshot` 最新一笔；字段名见 §5.3，**不是** `balance_after_fen`） |
| `uncleared_in_credit` | BIGINT | **分**，未达账项 A：企业已收、银行未收（合计） |
| `uncleared_in_debit` | BIGINT | **分**，未达账项 B：企业已付、银行未付（合计） |
| `uncleared_out_credit` | BIGINT | **分**，未达账项 C：银行已收、企业未收（合计） |
| `uncleared_out_debit` | BIGINT | **分**，未达账项 D：银行已付、企业未付（合计） |
| `adjusted_book_balance` | BIGINT | **分**，`book_balance + C − D`（企业侧：企业账面 **+ 银行已收企业未收(C) − 银行已付企业未付(D)**） |
| `adjusted_bank_balance` | BIGINT | **分**，`bank_balance + A − B`（银行侧：对账单余额 **+ 企业已收银行未收(A) − 企业已付银行未付(B)**） |
| `is_balanced` | BOOLEAN | 生成时计算：`adjusted_book_balance = adjusted_bank_balance`；**false 时只允许保存 DRAFT，禁止 SUBMIT 与 APPROVED**（§4.3 **V5** 硬门禁） |
| `status` | VARCHAR(16) | `DRAFT` / `SUBMITTED` / `APPROVED`（审核通过后锁定，改动需重开新表） |
| `submitted_by` / `submitted_at` | BIGINT / TIMESTAMP | 提交人（出纳）/ 时间 |
| `approved_by` / `approved_at` | BIGINT / TIMESTAMP | 审核人（财务主管）/ 时间。**硬门禁（§4.3 V8，不可配置关闭）**：`approved_by ≠ submitted_by`，且 `approved_by` 不得持有 `ROLE_TELLER`（出纳）；违反直接拒绝 `APPROVED` 并返回业务错误 |
| `remark` | VARCHAR(500) | 说明（`is_balanced=false` 时必填差异原因） |
| `create_user_id` / `created_by` / `create_time` / `update_time` / `version` / `deleted` | — | 项目通用列 |

约束：UNIQUE `(account_id, period_start, period_end)` —— 同账户同期间只允许一张有效调节表（重开走新记录 + 旧记录 `status` 保持 `APPROVED` 并存档）。

#### 5.10.2 `reconciliation_items`（未达账项明细）

| 字段 | 类型 | 说明 |
|---|---|---|
| `item_id` | BIGINT PK | — |
| `statement_id` | BIGINT | → `reconciliation_statements.statement_id`，NOT NULL |
| `category` | VARCHAR(4) | 四类之一：`A`(企业已收银行未收) / `B`(企业已付银行未付) / `C`(银行已收企业未收) / `D`(银行已付企业未付)（TD-25：首版只做四类） |
| `flow_id` | BIGINT | → `fund_flows.flow_id`（A/B 类填此项，内部侧） |
| `transaction_id` | BIGINT | → `bank_transactions.transaction_id`（C/D 类填此项，外部侧） |
| `amount` | BIGINT | **分**，该笔未达金额（正数；方向由 `category` 决定） |
| `business_date` | DATE | 该笔业务发生日（A/B 取 `fund_flows.business_date`；C/D 取 `bank_transactions.txn_time` 的日期） |
| `period_start` / `period_end` | DATE | 归属期间（冗余自 statement，便于跨期追溯与索引） |
| `owner_user_id` / `owner_name` | BIGINT / VARCHAR(50) | **挂账人**（认领该未达项的责任人；出纳或主管） |
| `reason` | VARCHAR(500) | **原因**（必填；如「月末在途」「银行已扣手续费未回单」） |
| `cleared_flag` | BOOLEAN | 是否已在下期核销（默认 false；下期调节表命中同一 `flow_id`/`transaction_id` 时置 true） |
| `cleared_statement_id` | BIGINT | 在下一期哪张调节表被核销（可空） |
| `create_time` / `update_time` / `deleted` | — | 项目通用列 |

约束：

- CHECK：`(category IN ('A','B') AND flow_id IS NOT NULL) OR (category IN ('C','D') AND transaction_id IS NOT NULL)` —— 不允许「既无内部引用也无外部引用」的悬空未达项；
- UNIQUE `(statement_id, category, COALESCE(flow_id,0), COALESCE(transaction_id,0))` —— 同一张表内同一笔不重复挂账；
- 汇总一致性（AC-43）：每张 statement 的四个 `uncleared_*_credit/debit` 合计 = 本表按 `category` 的 `SUM(amount)`（`deleted=0`）。

#### 5.10.3 与 §4 / §9 的联动

| 关联点 | 说明 |
|---|---|
| §4 S3–S9 接口 | 均以本节两表为落库载体（S3 生成草稿 = INSERT statements + items；S8 审核 = status → APPROVED）；若 TD-24 改口径，这些接口同步改写 |
| §4 V5–V8 校验 | 以 §4.3 定义为唯一口径（V8 = 审核人硬约束：≠提交人 且 ≠出纳）：**V5** = 调节后两侧相等（`is_balanced=false` 时不得 SUBMIT/APPROVED）；**V6** = 未达项必须有挂账人+原因+期间，且四类合计与 items 求和一致（AC-43）；**V7** = 同一笔（`flow_id`/`transaction_id`）同期间唯一 |
| §10.1 分期 | 本两表属 **P1**，P0 不建；P0 的 7 张表保持 §5.1–§5.7 不变 |
| 表数量口径 | 全案共 **9 张新表**：P0 建 7 张（§5.1–§5.7）+ P1 建 2 张（§5.10） |

## 6. 接口总览（按模块分组，共 36 个端点）

### 6.1 导入（6）

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| I1 | POST | `/v1/finance/bank-transactions/import` | 上传 + 预检（不落事实表） |
| I2 | GET | `/v1/finance/imports/{batchId}` | 批次详情 |
| I3 | GET | `/v1/finance/imports/{batchId}/rows` | 行级回执分页 |
| I4 | POST | `/v1/finance/imports/{batchId}/commit` | 确认落库 |
| I5 | POST | `/v1/finance/imports/{batchId}/cancel` | 撤销未提交批次 |
| I6 | GET | `/v1/finance/bank-transactions` | 银行流水查询 |

### 6.2 勾稽（10）

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| R1 | POST | `/v1/finance/recon-runs` | 触发跑批（幂等） |
| R2 | GET | `/v1/finance/recon-runs` | 批次列表 |
| R3 | GET | `/v1/finance/recon-runs/{runId}` | 批次详情 |
| R4 | GET | `/v1/finance/recon-runs/{runId}/export` | 导出明细 |
| R5 | GET | `/v1/finance/recon-items` | 主体列表 |
| R6 | POST | `/v1/finance/recon-items/{id}/match` | 人工勾稽 |
| R7 | POST | `/v1/finance/recon-items/{id}/unmatch` | 解绑 |
| R8 | GET | `/v1/finance/recon-differences` | 差异池 |
| R9 | POST | `/v1/finance/recon-differences/{id}/claim` | 认领 |
| R10 | POST | `/v1/finance/recon-differences/{id}/resolve` | 核销/冲正/豁免 |

### 6.3 现金交款单（11）

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| SL1 | POST | `/v1/finance/deposit-slips/generate` | 按班次生成（幂等） |
| SL2 | GET | `/v1/finance/deposit-slips` | 列表 |
| SL3 | GET | `/v1/finance/deposit-slips/{id}` | 详情 |
| SL4 | POST | `/v1/finance/deposit-slips/{id}/submit` | 提交 |
| SL5 | POST | `/v1/finance/deposit-slips/{id}/receive` | 出纳收妥 |
| SL6 | POST | `/v1/finance/deposit-slips/{id}/deposit` | 存行（生成 2 条内部转账） |
| SL7 | POST | `/v1/finance/deposit-slips/{id}/verify` | 复核 |
| SL8 | POST | `/v1/finance/deposit-slips/{id}/close` | 归档 |
| SL9 | POST | `/v1/finance/deposit-slips/{id}/reject` | 退回（权限 `finance:deposit-slip:reject`；出纳/财务总监） |
| SL10 | POST | `/v1/finance/deposit-slips/{id}/reverse` | 冲正 |
| SL11 | GET | `/v1/finance/deposit-slips/{id}/export` | 导出单张 |

### 6.4 日记账与调节表（9）

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| S1 | GET | `/v1/finance/ledger/daily` | 日记账（账户×日） |
| S2 | GET | `/v1/finance/ledger/summary` | 月结汇总 |
| S3 | POST | `/v1/finance/recon/statements` | 生成/重算调节表 |
| S4 | GET | `/v1/finance/recon/statements` | 调节表列表 |
| S5 | GET | `/v1/finance/recon/statements/{id}` | 详情（四类未达） |
| S6 | POST | `/v1/finance/recon/statements/{id}/submit` | 提交审核 |
| S7 | POST | `/v1/finance/recon/statements/{id}/approve` | 审核通过（硬门禁） |
| S8 | GET | `/v1/finance/recon/unmatched` | 未匹配池 |
| S9 | GET | `/v1/finance/recon/statements/{id}/export` | 调节表导出（CSV） |

**统一约定**：响应 `{code,message,data,timestamp}`；分页 `records/total/current/size`；金额一律**分**；时间格式 `yyyy-MM-dd HH:mm:ss`（Asia/Shanghai）。

---

## 7. 权限点与角色矩阵

> **已裁决 11**：新增权限码沿用现有前缀 `finance:`，**不新建命名空间**。

| 权限码 | 来源 | 用途 |
|---|---|---|
| `finance:bank:import` | **新增** | 上传 / 预检 / 提交 / 撤销导入批次 |
| `finance:bank:recon` | **新增** | 跑批 / force 重算 / 认领 / 核销 / 冲正 / 人工勾稽 / 导出明细 |
| `finance:deposit-slip:view` | **新增** | 查看缴款单 |
| `finance:deposit-slip:create` | **新增** | 生成缴款单（或由交班自动生成） |
| `finance:deposit-slip:submit` | **新增** | 收银员提交 |
| `finance:deposit-slip:receive` | **新增** | 出纳收妥 / 录实交 |
| `finance:deposit-slip:deposit` | **新增** | 存行登记（含关联银行流水） |
| `finance:deposit-slip:verify` | **新增** | 财务复核 |
| `finance:deposit-slip:close` | **新增** | 归档 |
| `finance:deposit-slip:reverse` | **新增** | 冲正（建议仅财务主管） |
| `finance:bank:view` / `finance:bank:query` | 既有 | 账户与流水只读 |
| `finance:bank:reconcile` | 既有 | 调节表出具/审核（复用，不另起新码） |
| `finance:audit-log:view` | 既有 | 审计日志（导出留痕校验用） |

### 7.1 角色矩阵

| 权限 | 出纳 | 财务主管 | 店长 | 审计 |
|---|---|---|---|---|
| `finance:bank:import` | ✅ | ✅ | — | — |
| `finance:bank:recon` | ✅（认领/勾稽） | ✅（含 force 重算/冲正） | — | — |
| `finance:bank:view` / `:query` | ✅ | ✅ | ✅（本店） | ✅（只读全量） |
| `finance:bank:reconcile` | ✅（出具草稿） | ✅（审核/封账） | — | — |
| `finance:deposit-slip:view` | ✅ | ✅ | ✅（本店） | ✅ |
| `finance:deposit-slip:create` | ✅ | ✅ | — | — |
| `finance:deposit-slip:submit` | — （收银员角色） | — | ✅（本店收银员） | — |
| `finance:deposit-slip:receive` | ✅ | ✅ | — | — |
| `finance:deposit-slip:deposit` | ✅ | ✅ | — | — |
| `finance:deposit-slip:verify` | — | ✅ | — | — |
| `finance:deposit-slip:close` | — | ✅ | — | — |
| `finance:deposit-slip:reverse` | — | ✅ | — | — |
| `finance:audit-log:view` | — | ✅ | — | ✅ |

**总部账户归属见 TD-40。数据可见范围（与权限正交）**：`bank_accounts.scope=STORE` 的账户仅本店可见可操作；`scope=GLOBAL` 仅财务主管/审计可见；**任何情况下禁止「无归属 → 默认门店」回退**。

**四眼原则（硬门禁，不可配置关闭）**：调节表 `submitted_by ≠ approved_by`，且**审核人不得为出纳（`ROLE_TELLER`）**——出纳可编制/提交（`finance:bank:recon` 保留），但审核必须由他人完成（§4.3 V8）；缴款单收妥人与复核人同样不得同一人（TD-38）。

**权限码（P3 前置已拍板，2026-10-25）**：新建 `finance:deposit-slip:reject`（`PERM_FINANCE_DEPOSIT_SLIP_REJECT`，permission_id=33501），绑 **`ROLE_TELLER` + `ROLE_FINANCE_DIRECTOR`**；SL9 `reject` 从 `finance:deposit-slip:close` **切回 `finance:deposit-slip:reject`**。绑定脚本 `scripts/db-fix/2026-10-25-deposit-slip-reject-permission.sql`（幂等，留痕 2 行）。矩阵中「退回」一列：出纳 ✓、财务总监 ✓，其余角色不得退回。

---

## 8. 验收标准与自动化防线（AC-1 – AC-43）

> 编号统一为 **AC-n**（合并 A 稿 A1-x / A4-x 与 B 稿 V2-x / V3-x，去重后单一编号）。
> **结论：其中 AC-1～AC-36 可作为实现期自测；AC-37 是「不许破坏既有防线」的硬门禁；AC-38～AC-41 与 AC-43 建议直接挂进 `scripts/qa/consistency_check.py`（新增 A7–A10、A12），与既有 A1–A6 并列；AC-42 为 INFO 信息性断言（A11），只输出超期条数供看板展示，不计入 TOTAL FAIL。**

| 编号 | 断言（可直接写成 SQL / 脚本） | 验证方式 | 章节 |
|---|---|---|---|
| AC-1 | 三类样例文件（微信/支付宝/银行，含 UTF-8 BOM 与 GB18030 各一份）解析成功，`import_batches.total_rows` 与人工计数一致 | 接口返回 + 人工核对 | §1 |
| AC-2 | 金额换算无损：抽 100 行断言 `bank_transactions.amount == round(元×100)`，不符行数 = 0 | SQL | §1 |
| AC-3 | 幂等：同一文件连续导入 2 次，第 2 次 `dup_rows == total_rows` 且 `bank_transactions` 行数不变 | SQL 计数对比 | §1 |
| AC-4 | 行级错误可读：构造 5 类错误（千分位 / 3 位小数 / 未来时间 / 缺必需列 / 无法判方向），`error_rows=5` 且每条 `error_code` 非空 | 接口断言 | §1 |
| AC-5 | 整批失败不留垃圾：缺必需列的文件 → `status=PARSED_FAILED`，`bank_transactions` 增量 = 0 | SQL | §1 |
| AC-6 | 审计完整：任取一条 `bank_transactions`，可回溯到 `batch_id` 与 `create_user_id`；孤儿行数 = 0 | SQL | §1 |
| AC-7 | 同日同账户跑两次 `POST /recon-runs`：`count(recon_runs WHERE run_key=…)` = 1，第二次响应 `reused=true` | SQL + 接口 | §2 |
| AC-8 | 同 run 内差异无重复：按 `(difference_type,internal_ref,external_ref)` 分组 `count>1` 的行数 = 0 | SQL | §2 |
| AC-9 | L1 精确命中：构造 `transaction_no='TXN-X'` 与 `external_txn_id='TXN-X'`，item 为 `AUTO_MATCHED` 且 `match_level='L1'` | 接口断言 | §2 |
| AC-10 | 多候选不自动确认：同账户同日两笔等额内部流水 + 一笔外部 → 两条 item 均为 `MATCHED`（非 AUTO），且存在 `AMBIGUOUS` 差异 | SQL + 接口 | §2 |
| AC-11 | 自动仅 1:1：构造 N:1 场景（两笔内部 → 一笔外部），断言不存在两条 item 同时 `AUTO_MATCHED` 指向同一 `external_ref` | SQL | §2 |
| AC-12 | 冲正不删数据：冲正后原 `flow_no` 行仍存在且 `status=3`，新增一条 `remark LIKE '%[reconReverseOf=<原flow_no>]%'` 的反向流水（方向相反、金额相等） | SQL | §2 |
| AC-13 | 冲正余额一致：`bank_accounts.balance` 变化量 = 反向流水 `amount`，且反向流水 `balance_after` = 冲正后余额 | SQL | §2 |
| AC-14 | 勾稽率口径：`matched_amount` 只累加 AUTO_MATCHED + MANUAL_MATCHED；构造一条 `MATCHED` 后比率不变 | SQL + 接口 | §2 |
| AC-15 | 差异必须填说明：`resolve` 不带 `remark` → 400；带 `remark` → 200 且 `status=CLOSED` | 接口断言 | §2 |
| AC-16 | 审计可追：任一 `resolve` 后 `finance_audit_logs` 存在对应记录（含 run_id、difference_id、前后状态） | SQL | §2 |
| AC-17 | 金额单位：`recon_runs.external_amount` == `SUM(bank_transactions.amount)`（分），与 CSV 元值 ×100 一致（差 100 倍即 FAIL） | SQL | §2 |
| AC-18 | 手续费口径：`fee` 独立字段不入 `amount`；构造 `内部金额 = 外部金额 + fee` 的场景 → **判平账、不生成差异**（P1 实现口径）；仅当差额**无法被 fee 完全解释**时才生成 `CHANNEL_FEE` 类型差异，金额 = 未被解释的差额。双向断言：fee 平账样本 `CHANNEL_FEE` 必须为 0；有未解释差额时必须 > 0 | SQL + 接口 | §2 |
| AC-19 | 缴款单生成幂等：班次 `ended` 后 `generate` 两次 → 同一 `slip_id` 且 `count(deposit_slips WHERE shift_id=…)=1` | SQL + 接口 | §3 |
| AC-20 | 应交可复算：`expected_amount` 与 `SUM(order_payment_records.payment_amount WHERE payment_method=1 AND 订单∈班次窗 AND payment_status=2)` 分毫不差 | SQL | §3 |
| AC-21 | 差额公式：`difference_amount = actual_amount − expected_amount`，0 / 正 / 负三种情形各断言一次 | SQL + 接口 | §3 |
| AC-22 | 状态机不可跳步：DRAFT 直接 `deposit` → 400；SUBMITTED 直接 `verify` → 400 | 接口断言 | §3 |
| AC-23 | `RECEIVED` 不生成流水：`count(fund_flows WHERE remark LIKE '%[depositSlip=…]%')` = 0 | SQL | §3 |
| AC-24 | `DEPOSITED` 恰好 2 条内部转账（一出一入、金额均 = `actual_amount`、`flow_category=5`），且无新增 SALE 收入 | SQL | §3 |
| AC-25 | `deposit` 重复调用幂等：再调 → 400/409，流水仍为 2 条 | 接口 + SQL | §3 |
| AC-26 | 存行余额一致：现金户减少 `actual_amount`、存款户增加 `actual_amount`，两账户 `balance_after` 与 `balance` 相等 | SQL | §3 |
| AC-27 | 短款不改源数据：`pos_shifts.closing_cash` 在 `RECEIVED` 前后不变 | SQL | §3 |
| AC-28 | 缴款单冲正：`reverse` 后原单 `status=REVERSED`（记录仍在）、新增反向流水、余额回滚 | SQL | §3 |
| AC-29 | 缴款单审计：每一步状态变化在 `finance_audit_logs` 可查（operator / 时间 / 前后状态） | SQL | §3 |
| AC-30 | 与勾稽联动：存行后该笔 `bank_transactions` 能被 L1 命中（`deposit_ref_no` ↔ `external_txn_id`） | 接口 + SQL | §3 |
| AC-31 | 日记账闭合：任取 30 天，逐日 `期初=前日期末` 且 `期末=期初+收入−支出`，不满足行数 = 0 | SQL（V1/V2） | §4 |
| AC-32 | 账户余额一致：`bank_accounts.balance == 最新**非豁免** fund_flows.balance_after`（豁免仅 `FFH%` 与 `created_by LIKE 'system/backfill-%'`；普通行 `balance_after=0` **不豁免**；全豁免账户判 N/A），不一致账户数 = 0 | SQL（V3 / `consistency_check` A7） | §4 |
| AC-33 | 调节表硬门禁：构造 `diff≠0` → `approve` 必须失败（业务错误码非 0） | 接口断言 | §4 |
| AC-34 | 未达账项一致：四类合计 == 列合计；同一笔跨期间不重复挂账（V6/V7） | SQL | §4 |
| AC-35 | 平衡可审核：`diff=0` → `APPROVED`，且提交人 ≠ 审核人（四眼） | 接口 + SQL | §4 |
| AC-36 | 只读可见范围：店长查非本店账户 → 403；审计调写接口 → 403 | 接口断言 | §7 |
| AC-37 | 不破坏既有防线：`consistency_check.py` 5 PASS + A6 INFO；`ui_e2e_payment.py` 7/7；`schema_guard.py` 通过 | 脚本退出码 | §8 |
| AC-38 | （建议新增 A7）同日同账户 run 唯一：分组 `run_key` 计 `count>1` 的行数 = 0 | 挂 `consistency_check.py` | §8 |
| AC-39 | （建议新增 A8）缴款单与班次一对一：分组 `shift_id` 计 `count>1` 的行数 = 0 | 挂 `consistency_check.py` | §8 |
| AC-40 | （建议新增 A9）存行流水成对：每张 `DEPOSITED` 缴款单对应恰好 2 条 `[depositSlip=…]` 内部转账 | 挂 `consistency_check.py` | §8 |
| AC-41 | （建议新增 A10）现金户余额非负：`count(bank_accounts WHERE account_type=2 AND balance<0)` = 0 | 挂 `consistency_check.py` | §8 |
| AC-42 | （建议新增 A11，**INFO 非阻断**）`MATCHED` 超期未处理：`count(recon_items WHERE subject_status='MATCHED' AND matched_since < now() - interval 'N days')`，N 默认 3、可配置 `app.finance.recon.matched-stale-days`；输出行数供巡检看板展示，**不计入 TOTAL FAIL** | 挂 `consistency_check.py`（INFO 段） | §2.3、§5.5 |
| AC-43 | （建议新增 A12）调节表汇总自洽：每张 `reconciliation_statements` 的 `uncleared_in_credit/in_debit/out_credit/out_debit` = 其 `reconciliation_items` 按 `category` 的 `SUM(amount)`（`deleted=0`），差值不为 0 的行数 = 0（TD-24 裁决为其他载体时本断言作废） | 挂 `consistency_check.py` | §4.3、§5.10 |
| AC-44 | （P1 新增）**审核人硬门禁 V8**：以出纳身份提交的调节表，用同一账号 approve → 必须被拒（业务错误码非 0）；用另一出纳账号 approve → 也必须被拒；用非出纳的其他审核人 approve → 通过。三条用例全部断言 | 接口 + SQL | §4.3 V8、§5.10.1 |
| AC-45 | （P1 新增）**调节表不调账**：`POST /statements`（生成）→ `submit` → `approve` 全链路前后，`fund_flows` 行数与 `bank_accounts.balance` 合计**零变化**；`reconciliation_items` 只引用既有 `flow_id`/`transaction_id`，不存在指向不存在记录的悬空引用 | SQL | §4.3 硬规则一 |

### 8.1 与现有 QA 脚本的挂载关系

| 脚本 | 现状 | 本设计新增/复用 |
|---|---|---|
| `scripts/qa/consistency_check.py` | A1–A5 断言 + A6 信息性 | **复用不改**；建议新增 A7–A10（AC-38～AC-41） |
| `scripts/qa/ui_e2e_payment.py` | 三端支付口径 7 断言 | **复用不改**（AC-37 要求仍 7/7） |
| `scripts/qa/schema_guard.py` | 表结构守卫 | **复用不改**；新表落地后自动纳入白名单比对 |
| `scripts/qa/r8_location_coverage.sql` / `.py` | 用户归属覆盖率只读排查 | 与本设计无关，但同属「数据体检」族，可并列执行 |

---

## 9. 待裁决点（TD-01 – TD-40，其中 11 条已裁决）

> **结论：共 40 条；其中 11 条（TD-01～TD-11）Owner 已裁决，直接照此实现；剩余 29 条待拍板，已给建议默认值，未拍板前实现受阻面见「影响面」列。**

### 9.1 已裁决（11 条，照此实现，不得再改口径）

| 编号 | 事项 | 裁决结论 | 影响面 |
|---|---|---|---|
| TD-01 | 外部流水表命名与数据来源 | **统一命名 `bank_transactions`；本阶段只做 CSV 导入；银企直连 / 邮件解析 = 非目标** | §1、§5.3 |
| TD-02 | `pos_shifts.closing_cash` 单位 | **元**（numeric(10,2)，实测 `0.00` / `total_amount 3008.80`）；跨边界 **×100 转分**；`fund_flows` / `bank_accounts.balance` / `order_payment_records` 一律**分** | §1.3、§3.2、§5 |
| TD-03 | 缴款单应交金额权威源 | 以 `order_payment_records` 该班次现金支付合计**重算为权威**；`closing_cash` 仅参考；不一致记差异并留痕；**不得直接改 `closing_cash`** | §3.2 |
| TD-04 | 勾稽时间窗 | 默认 **±10 分钟**，可配置 `app.finance.recon.time-window-minutes`（默认 10） | §2.2 L2 |
| TD-05 | 自动勾稽形态 | **只允许 1:1**；1:N / N:1 / N:M 一律人工 | §2.2 |
| TD-06 | 渠道手续费口径 | **独立字段 `bank_transactions.fee`，不并入交易金额**；判金额差时**先扣手续费再比** | §1.1、§2.2、§5.3 |
| TD-07 | 勾稽完成率口径 | **`MATCHED`（单边标记）不计入勾稽率**；只有 `AUTO_MATCHED` / `MANUAL_MATCHED` 计入 | §2.3、§5.4 |
| TD-08 | 冲正方式 | **只新增反向流水**、原流水置 `status=3`；**禁止物理删除、禁止改历史金额** | §2.5 |
| TD-09 | 缴款单与流水关系 | **三段式**：支付时记一次收入（已实现）→ `RECEIVED` **不生成流水** → `DEPOSITED` 生成**两条内部转账**（现金户出 / 银行户入，`flow_category=5`），幂等键写在 `remark` | §3.4 |
| TD-10 | 已关闭差异的重开 | 只能通过 `reopen_of` **新建**记录重开，**不可原地修改** | §2.5、§5.6 |
| TD-11 | 权限码命名 | 沿用现有前缀：`finance:bank:import`、`finance:bank:recon`、`finance:deposit-slip:*`；**不新建命名空间** | §7 |

### 9.2 待裁决（29 条：TD-12 – TD-40）

| 编号 | 待裁决问题 | 建议默认值 | 影响面 |
|---|---|---|---|
| TD-12 | 幂等唯一键取 A（external_id）/ B（时间+金额+对方）/ C（组合） | **C**（A 为主键，无号退化 B 并标 `dedupe_basis=COMPOSITE`）；**附带裁决：精确键命中=DUPLICATE 静默跳过；退化键命中=NEEDS_REVIEW 落库+人工确认，不得静默判重** | §1.4、§5.3 唯一索引、AC-3 |
| TD-13 | `external_txn_id` 是否带渠道前缀（如 `WX:`），银行流水号缺失时是否退化组合键 | 带前缀 + 允许退化 | §1.1、§1.4 |
| TD-14 | `bank_transactions` 是否生成 `fund_flows`（到账即记账） | **否**（事实层/账面层分离） | §5.3、§3.4 |
| TD-15 | 是否两段式（预检→确认）；已 `COMMITTED` 批次能否撤销 | 两段式 + 可撤销且留痕 | §1.2、§1.5 |
| TD-16 | 银行流水列名是否走**映射模板（配置化）** | 配置化模板（否则每家银行一套代码） | §1.1、附录 A |
| TD-17 | > 50k 行是否本期支持异步导入 | 不支持，先要求拆分文件 | §1.3 |
| TD-18 | `import_rows.raw_json` 是否保留原始行（含账号等敏感信息） | 只留脱敏副本 + 原文哈希 | §5.2 |
| TD-19 | 是否多币种（`currency` 恒 CNY 是否长期成立） | 本期仅 CNY；非 CNY 整批拒绝 | §0.4、§1.3 |
| TD-20 | 日记账粒度 `account × day` 是否够（是否需 currency / store 维度） | 够（store 由账户间接确定） | §4.1 |
| TD-21 | 调节期间：自然月 vs 自定义起止；是否允许跨月未结账 | 自然月（首版） | §4.1、§4.4 |
| TD-22 | 历史余额漂移（`balance` 与流水不一致）是否先做一次基线校准 | 先校准（以对账单为锚点）；**已提升为 PC-7（§10.3）**，不再是『可选优化』：未完成校准不得进 P1 | §4.2 V3、§10.3 PC-7 |
| TD-23 | `balance_after`（流水快照）vs `bank_accounts.balance` 谁是唯一真值 | 流水快照为准，账户余额为派生缓存；**附带裁决：V3 修复单向（快照→缓存），禁止反向改 `balance_after`** | §4.2、PC-7、AC-32 |
| TD-24 | 未达账项挂账载体（新建 `reconciliation_items` / `fund_flows.remark` / `match_status`） | 新建独立表（便于跨期追溯）；**必须连带确认：§5.10 两张调节表（`reconciliation_statements` + `reconciliation_items`）是否保留、归属 P0 还是 P1** | **§5 表数量（7 张 vs 9 张）、§10.1 P0/P1 分期、§4 S3–S9 落地、§5.10 去留、AC-43 是否生效** |
| TD-25 | 调节表是否只做四类（是否要「暂收暂付 / 在途」） | 只做四类 | §4.3 |
| TD-26 | 是否复用既有 `bank_payment_records`（已有同名 Controller） | 不复用，避免语义混淆 | §5.3 |
| TD-27 | L3 金额容忍阈值（手续费差上限） | 0.5% 且 ≤ 5 元 | §2.2 L3 |
| TD-28 | 核销差额是否生成费用/其他支出流水 | 生成（`flow_category=6`，幂等键含 `[writeOff=<difference_id>]`） | §2.5 |
| TD-29 | 跨日到账是否新建 run | 不新建（记 `TIME_DIFF`） | §2.6 |
| TD-30 | `payment_status=1`（部分支付）是否参与当日勾稽 | 不参与 | §2.9 E2-6 |
| TD-31 | 退款（收入侧反向）是否纳入勾稽范围 | 本期不纳入收入侧；单列支出侧（后续） | §2.9 E2-7 |
| TD-32 | 停用账户是否允许发起新 run | 允许（需手工确认），历史仍可对 | §2.9 E2-8 |
| TD-33 | 一个班次是否只允许一张缴款单 | 是（UNIQUE `shift_id`） | §5.7、§3 |
| TD-34 | 短款追责方式（应收 / 工资扣款 / 手工核销） | 先挂待查损溢，财务线下决定 | §3.5 |
| TD-35 | 内部转账是否生成会计凭证 | 由 AutoVoucher 规则决定，本期手工 | §3.4 |
| TD-36 | 跨天班次的 `business_date` 归属 | 取交班日 | §3.8 E3-1 |
| TD-37 | `closing_cash` 为空是否允许生成缴款单 | 生成但禁止 `submit` | §3.8 E3-5 |
| TD-38 | 收银员=出纳时是否强制二次复核 | **强制且不可配置关闭**（与 V8 同口径：审核人 ≠ 提交人 且 ≠ 出纳角色） | §4.3 V8、§7 四眼原则 |
| TD-39 | 已结账期间是否硬拦截 `deposit` / `verify` | 硬拦截 | §3.8 E3-11 |
| TD-40 | `scope=GLOBAL`（总部账户）导入/勾稽/调节权限归属；缴款单关联是否本期做 | 仅财务主管可操作总部账户；缴款单关联下期 | §7、§3 |

**开工前置（不定就无法写代码）**：TD-12（幂等键）、TD-14（是否生成流水）、TD-23（真值口径）、TD-24（未达账项载体）。其余可在实现中默认后置调整。

> 决策用汇总表见 **§9.3 拍板清单**（含建议默认值、驳回影响面、连带确认事项）。
### 9.3 拍板清单（Owner 用这一节决策）

> **结论：Owner 拍板下列 4 条 + 确认 §5.10 去留后，方可开 P0 代码；其余 25 条可在实现中按建议默认值先做，后置调整。**

#### A. 开工前置 4 条（不定无法写 P0 代码）

| 编号 | 问题 | 建议默认值 | 驳回后的影响面 | 连带确认事项 |
|---|---|---|---|---|
| **TD-12** | 外部流水幂等唯一键 | **组合式**：`external_txn_id` 为主键；缺号时退化为「交易时间 + 金额 + 对方名称」并标 `dedupe_basis=COMPOSITE`；**退化键命中重复 → `NEEDS_REVIEW`（落库+人工确认），精确键命中才 `DUPLICATE` 静默跳过** | 驳回为纯 A 方案 → 缺少流水号的银行文件整批被拒；驳回为纯 B 方案 → 同额同日多笔会被误判重复，真实交易被吞 | 需确认唯一索引形态（§5.3）与 AC-3 断言口径 |
| **TD-14** | `bank_transactions` 是否生成 `fund_flows`（到账即记账） | **否**：事实层（银行流水）与账面层（业务流水）分离，勾稽才有两侧可比 | 改为「是」→ 每笔到账都会多出一笔收入流水，与 SALE 流水重复计收入；勾稽退化为自证，§2 全部匹配规则需重写 | 需确认 §3.4 三段式（RECEIVED 不入账 / DEPOSITED 内部转账）是否仍成立 |
| **TD-23** | 余额唯一真值：`fund_flows.balance_after` 快照 vs `bank_accounts.balance` | **流水快照为准**，账户余额为派生缓存，每日巡检校验（§4.2 V3、AC-32）；**V3 修复单向：快照→缓存，禁止反向改 `balance_after`** | 改为「账户余额为准」→ 冲正/补账后必须重算全表，历史流水失去可审计快照；调节表 book_balance 取值口径需改写 | 连带 PC-7（基线校准）的口径定义 |
| **TD-24** | 未达账项挂账载体 | **新建独立表**：`reconciliation_statements` + `reconciliation_items`（§5.10 字段级草案） | 改为 remark / match_status 承载 → **§5.10 整节删除**，§4 的 S3–S9 与 V5–V7 全部改写，AC-43 作废，表数量从 9 回落 7 | **必须同时确认**：§5.10 两表是否保留、归属 **P1**（推荐）还是 **P0**；若不保留，需指定替代载体。**另：若保留，P1 开工前须复核本版（v1.2）已修正的四处——① §5.10.1 调节公式方向（A/B 与 C/D 曾写反）② §4.3 示例数字与符号（`+D−E`）③ V5–V7 编号口径（以 §4.3 为唯一权威）④ `bank_balance` 取值字段名（应为 `bank_transactions.balance_snapshot`，非 `balance_after_fen`）** |

#### B. 其余 25 条（可实现中默认后置调整）

> 以下编号保持 §9.2 原编号与建议默认值不变，此处仅汇总，不重复展开（详见 §9.2 表）：TD-13、TD-15、TD-16、TD-17、TD-18、TD-19、TD-20、TD-21、TD-25、TD-26、TD-27、TD-28、TD-29、TD-30、TD-31、TD-32、TD-33、TD-34、TD-35、TD-36、TD-37、TD-38、TD-39、TD-40 —— 共 **24 条**，加上已提升为 PC-7 的 **TD-22**，合计 **25 条**。

| 分组 | 编号 | 共同处理方式 |
|---|---|---|
| 导入侧细节 | TD-13、TD-15、TD-16、TD-17、TD-18、TD-19 | 按 §9.2 建议默认值实现，验收时回归确认 |
| 勾稽/差异细节 | TD-25、TD-26、TD-27、TD-28、TD-29、TD-30、TD-31、TD-32 | 同上；其中 TD-28（核销是否生成费用流水）在 P1 上线前需再确认一次 |
| 日记账/调节表 | TD-20、TD-21、TD-22（→PC-7） | TD-22 已升级为前置条件，其余按默认值 |
| 缴款单/内控 | TD-33、TD-34、TD-35、TD-36、TD-37、TD-38、TD-39、TD-40 | 同上；TD-38（收银员=出纳强制二次复核）建议保持 强制 |

**生效条件（硬门禁）**：Owner 拍板 **TD-12 / TD-14 / TD-23 / TD-24** 四条 + 确认 **§5.10 去留与归属期** 之后，方可开 P0 代码。


---

## 10. 实施分期、风险与前置条件

### 10.1 分期（P0 / P1 / P2）

| 期 | 范围 | 交付物 | 退出标准 |
|---|---|---|---|
| **P0（地基，必须先行）** | **只建 §5.1–§5.7 的 7 张新表**（调节表两表 §5.10 **不属 P0**，按 TD-24 裁决归 P1 或取消）；权限码入库；`bank_transactions` CSV 导入（微信/支付宝/银行各 1 份样例）；幂等与行级回执；只读接口（I1–I6） | 可导入、可回执、可查 | AC-1～AC-6、AC-37 全绿；无任何写账动作 |
| **P1（勾稽与日记账）** | 跑批（R1–R4）、主体/差异列表（R5、R8）、认领与处理（R9、R10）、人工勾稽（R6、R7）、日记账（S1、S2）、调节表草稿与审核（S3–S9，落地表为 §5.10 的 `reconciliation_statements` + `reconciliation_items`，随本阶段 Flyway 建表） | 能跑通「导入 → 勾稽 → 处理 → 调节表」闭环 | AC-7～AC-18、AC-31～AC-36 全绿；一个结账周期内勾稽率可解释 |
| **P2（现金缴款单 + 硬化）** | 缴款单全链路（SL1–SL11）；冲正/补记；新增 A7–A10 巡检；导出 | 现金「收银员→出纳→银行」闭环 + 账实一致 | AC-19～AC-30、AC-38～AC-41 全绿；连续 2 个班次实测平账/短款各 1 例 |

**结论：P0 是纯新增（不改现状表），风险最低，可先独立上线；P2 涉及生成 `fund_flows`，必须在 P0/P1 稳定后再动。表数量口径：全案 9 张 = P0 的 7 张（§5.1–§5.7）+ P1 的 2 张（§5.10，随 TD-24 裁决可能取消）。**

### 10.2 风险与缓解

| # | 风险 | 影响 | 缓解 |
|---|---|---|---|
| RK-1 | 金额单位混用（元/分） | 差 100 倍，账全线失真 | TD-02 已裁决；导入边界强制 `×100` 且断言 AC-2 / AC-17；代码评审单列检查项 |
| RK-2 | 幂等键设计不当 | 重复入账 / 误判重复吞掉真实交易 | TD-12 定稿前不实现 P1；`dedupe_basis` 留痕；AC-3 覆盖 |
| RK-3 | 自动勾稽误绑（同额同日多笔） | 差异被错杀，账实不符 | 只做 1:1（TD-05）；多候选强制 `MATCHED` 等人工（AC-10） |
| RK-4 | 冲正写坏余额 | 余额漂移不可逆 | 只增反向流水（TD-08）+ 原子更新通道 + AC-13 |
| RK-5 | 缴款单重复计收入 | 收入虚增 | 三段式（TD-09）：RECEIVED 不生成流水，DEPOSITED 仅内部转账；AC-23/AC-24 |
| RK-6 | 门店隔离被兜底逻辑破坏 | 跨店串数据（越权） | 明令禁止「无归属→默认门店」；AC-36 + R8 同款原则 |
| RK-7 | 历史数据脏（支付记录缺失、退款无关联） | 勾稽率长期偏低 | E3-4 告警 + 手工挂账；不追求首期 100% |
| RK-8 | 期间结账与对账互相阻塞 | 月末无法出表 | TD-39 定义拦截点；建议「结账前必须全部 CLOSED」作为流程约束 |

### 10.3 生效前必须满足的前置条件

| # | 前置条件 | 状态 |
|---|---|---|
| PC-1 | §9 剩余 29 条 TD 中**开工前置 4 条**（TD-12/14/23/24）已拍板 | 未满足 |
| PC-2 | `bank_accounts` 账户主数据齐备（`scope` / `store_id` / `account_type` 无空值） | 待核 |
| PC-3 | 现金户（`account_type=2`）与门店一一对应，且 `balance` 与流水一致（V3 全绿或已列入漂移清单） | 待核（TD-22） |
| PC-4 | 权限码入库 + 角色绑定，且 `permission_assignment_log` 留痕 | 待实现 |
| PC-5 | 三道既有防线（consistency / ui_e2e_payment / schema_guard）在基线全绿 | 已具备 |
| PC-6 | 至少 3 份真实样例文件（微信 / 支付宝 / 银行）可用于验收 | 待收集 |
| **PC-7** | **P0 开工前完成历史余额基线校准**：跑 `bank_accounts.balance` vs 最新**非补账** `fund_flows.balance_after` 对账（§4.2 V3 例外规则：`FFH%`/`[历史补账%` 行 `balance_after=0` 属设计，须排除；全补账账户判 N/A），输出**漂移账户清单**（账户/账面/快照/差额/建议处理）并按 **V3 单向修复**（快照 → 缓存，禁止反向改 `balance_after`）执行校准。**未校准不得进 P1**（否则 AC-32 第一天就红） | **未满足（TD-22 提升而来）** |
| **PC-8** | **PostgreSQL 服务改为自动启动或写入启动脚本**：避免夜间巡航 / CI / E2E 因服务 Stopped 直接失败（2026-10-24 已发生一次，需手动 `pg_ctl` 拉起） | **未满足** |

---

## 附录 A：CSV 列名映射样例

**A.1 微信商户账单 → `bank_transactions`**

| CSV 列（示例） | 目标字段 | 处理 |
|---|---|---|
| 交易时间 | `txn_time` | `yyyy-MM-dd HH:mm:ss`，Asia/Shanghai |
| 微信订单号 | `external_txn_id` | 加前缀 `WX:`（TD-13） |
| 商户订单号 | `merchant_order_no` | 原样 |
| 订单金额 | `amount` | 元 → 分（×100，必须整数） |
| 手续费 | `fee` | 元 → 分；独立字段 |
| 交易状态 | `status_raw` + `settle_status` | `SUCCESS→SETTLED`、`REFUND→REFUNDED`、`支付失败→FAILED`、其他 `UNKNOWN` |
| 退款金额 / 退款状态 | `is_refund` + `amount` | 退款行 `is_refund=true`，金额取退款金额 |
| 商品名称 | `summary` | 截断 255 |

**A.2 支付宝账单 → `bank_transactions`**

| CSV 列（示例） | 目标字段 | 处理 |
|---|---|---|
| 完成时间（空则创建时间） | `txn_time` | Asia/Shanghai |
| 支付宝交易号 | `external_txn_id` | 加前缀 `ALI:` |
| 商家订单号 | `merchant_order_no` | 原样 |
| 订单金额（元） | `amount` | ×100 → 分 |
| 服务费（元） | `fee` | ×100 → 分 |
| 对方账户 | `counterparty_account` | 脱敏后存（TD-18） |
| 备注 | `summary` | 截断 255 |

**A.3 银行流水 → `bank_transactions`**

| CSV 列（示例） | 目标字段 | 处理 |
|---|---|---|
| 交易日期 / 时间 | `txn_time` | 合并为 `yyyy-MM-dd HH:mm:ss`；无时间则补 `00:00:00` 并标 `needs_review` |
| 交易流水号 | `external_txn_id` | 加前缀 `BANK:` |
| 贷方金额 / 借方金额 | `amount` + `direction` | 贷方→1（收入）、借方→2（支出），均取正数（TD-16 映射模板） |
| 余额 | `balance_snapshot` | 元 → 分 |
| 对方账号 / 户名 | `counterparty_account` / `counterparty_name` | 账号脱敏 |
| 摘要 / 用途 | `summary` | 截断 255 |
| 交易类型 | `status_raw` | `手续费/利息` 也归入 `direction`，`settle_status=SETTLED` |

## 附录 B：状态机一览

| 对象 | 状态 | 迁移（允许） |
|---|---|---|
| `import_batches` | UPLOADED → PARSED_OK / PARSED_FAILED → COMMITTED / PARTIAL；UPLOADED→CANCELLED | 不可回退到 UPLOADED；COMMITTED 不可追加行 |
| `import_rows` | OK / DUPLICATE / ERROR / NEEDS_REVIEW | 终态，不流转 |
| `bank_transactions.match_status` | UNMATCHED → AUTO_MATCHED / MANUAL_MATCHED / EXCEPTION；MATCHED → MANUAL_MATCHED | 解绑仅 MANUAL_MATCHED → UNMATCHED |
| `recon_runs` | RUNNING → FINALIZED / FAILED；FINALIZED --force-->（新 run）RUNNING | 旧 run 只读 |
| `recon_differences` | OPEN → ASSIGNED → RESOLVING → CLOSED；ASSIGNED/RESOLVING → EXEMPT；CLOSED --reopen-->（新记录）OPEN | CLOSED 不可原地改（TD-10） |
| `deposit_slips` | DRAFT → SUBMITTED → RECEIVED → DEPOSITED → VERIFIED → CLOSED；SUBMITTED → REJECTED → SUBMITTED；任意已提交态 → REVERSED | **流水号/单据号必须统一走 `order_sequence` 号段表**（`GREATEST(号段, 当天MAX)+1 RETURNING` + MyBatis `flushCache=TRUE/useCache=false`）；禁止「查 MAX+1」、禁止多套发号器并行（P2 曾因此撞 `fund_flows_flow_no_key`）；不得回退到 DRAFT；`expected_amount` 只读 |
| 调节表（§4 / §5.10） | DRAFT → SUBMITTED → APPROVED | `is_balanced=false` 不得 SUBMITTED/APPROVED（**V5**）；落地表为 `reconciliation_statements` + `reconciliation_items`，**随 TD-24 裁决确定去留与归属期** |

## 附录 C：与现状表字段对照（易混点）

| 概念 | 现状字段 | 单位/口径 | 本设计对应 |
|---|---|---|---|
| 资金流水金额 | `fund_flows.amount` | **分** | `bank_transactions.amount`（**分**，同口径可直接比较） |
| 流水方向 | `fund_flows.flow_direction`（1收入/2支出） | — | `bank_transactions.direction`（同枚举） |
| 支付方式 | `order_payment_records.payment_method`（0–5） | — | 不支持扩展；现金只认 `1` |
| 支付金额 | `order_payment_records.payment_amount` | **分** | 缴款单 `expected_amount` 的求和源 |
| 班结现金 | `pos_shifts.closing_cash` | **元**（numeric(10,2)） | 仅参考；与 `actual_amount` 并列展示（**不可当权威**） |
| 班结营业额 | `pos_shifts.total_amount` | 元（实测 3008.80） | 不参与本设计计算 |
| 账户余额 | `bank_accounts.balance` | **分** | 派生缓存（V3 校验对象） |
| 流水后余额 | `fund_flows.balance_after` | **分** | 账面真值（V1 校验对象） |
| 银行回单余额 | ——（不存在） | — | `bank_transactions.balance_snapshot`（**分**） |
| 订单实付 | `orders.actual_amount` / `paid_amount` | 分 | 仅用于交叉核对，不直接参与勾稽 |
| 财务记录 | `finance_records`（record_type/category） | 元/分口径需逐字段核（既有债） | 本设计**不写** `finance_records` |

---

> **本文档为设计稿，未实现任何代码；实现前需 Owner 对 §9 剩余待裁决点逐条拍板。**

