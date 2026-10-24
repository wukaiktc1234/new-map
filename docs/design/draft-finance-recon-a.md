# 财务对账设计（A 部分）：第三方流水 CSV 导入 + 资金日记账 / 银行存款余额调节表

> **维护人**：架构总控（设计轨）· 执行：ts-hr-asset
> **创建日期**：2026-10-24
> **性质**：**只设计，不开发**（本文不含代码、不含建表语句、不改配置；表结构仅为"建议"，落地需走 Flyway 且经 Owner 裁决）
> **文档族**：财务对账设计三段式 —— **A（本文）**：CSV 导入 + 日记账 + 调节表；B/C（另文档）：勾稽匹配规则、期末结账与凭证联动
> **依据**：现状取自活库 `information_schema` 与代码只读核对（2026-10-24：`bank_accounts` 32 行 / `fund_flows` 576 行 / 近 30 天 `orders` 617 单）

---

## 0. 背景、目标、非目标

### 0.1 背景（为什么现在要补这一层）

当前 `fund_flows` 记录的是**业务发生**（卖出去了、收到钱了 → 记一笔资金流水），但它回答不了财务真正要回答的问题：

> **钱，究竟有没有真的到账？到账了多少？与账面差在哪？**

具体缺口：

| # | 缺口 | 现状证据 |
|---|---|---|
| 1 | **没有"银行侧事实"** | 全库无任何第三方/银行流水落地表（`information_schema` 仅 `inventory_transactions_legacy` 命中 `transaction`，与资金无关） |
| 2 | **账面余额只有单侧口径** | `bank_accounts.balance`（分）由业务写入；`fund_flows.balance_after`（分）为流水链路余额；两者都**没有**与银行对账单核对过 |
| 3 | **无线下/渠道差异的确认环节** | 微信/支付宝/银行的手续费、退款、跨日到账，目前在 `fund_flows` 里没有对应的事实来源 |
| 4 | **缺可审计的导入留痕** | 无 `import_batches` 级别的"谁在什么时候导了哪份文件、成功/失败多少行"记录 |

已在位的防线（本文所有验收都要求**不破坏**它们）：

- `scripts/qa/consistency_check.py`（5 类一致性巡检）；
- `scripts/qa/ui_e2e_payment.py`（三端支付口径 UI 断言）；
- `scripts/qa/schema_guard.py`（表结构守卫）；
- 已落地约束：**支付编码单轨**（0未记录/1现金/2微信/3支付宝/4银行卡/5余额）、`fund_flows.order_id` 关联、`bank_accounts` 门店隔离、余额原子更新、`create_user_id` 审计、流水状态默认已完成。

### 0.2 目标（A 部分）

1. **能导入**：支持 微信商户账单 / 支付宝账单 / 银行流水 三类 CSV，**元→分**无损换算，行级错误可读、可重导；
2. **能幂等**：同一份文件导入两次，第二次全部判重，**不产生任何重复事实**；
3. **能对账**：按 `account_id` + `business_date` 出**资金日记账**（期初/收入/支出/期末）；
4. **能出调节表**：企业账面 vs 银行对账单，四类未达账项可挂账，**调节后余额必须相等**（否则拒绝出具）；
5. **能追溯**：每次导入/每次出具调节表都有批次号 + 操作人 + 时间 + 行级结果。

### 0.3 非目标（A 部分明确不做）

- ❌ 不做自动凭证生成（与 B/C 段的结账/凭证联动解耦）；
- ❌ 不做银行直连（银企直连/OpenAPI 拉流水）；
- ❌ 不做多币种折算（`currency` 字段保留，本期只承诺 CNY）；
- ❌ 不做规则引擎自动匹配（人工勾稽优先，见 D15）；
- ❌ 不改动 `fund_flows` 现有语义与写入口（导入侧只**新增事实**，不回写业务流水）。

### 0.4 术语表

| 术语 | 定义（本文口径） |
|---|---|
| **业务流水** | `fund_flows` 中的一行：由业务动作（收款/付款/退款/入账）产生的**账面**资金变动 |
| **银行流水** / 第三方流水 | 渠道（微信/支付宝/银行）导出的**客观到账事实**，本文落地为 `bank_transactions` |
| **勾稽** / 匹配 | 把一条银行流水与一条业务流水建立 1:1 / 1:N / N:1 关系的过程（B/C 段详述规则） |
| **差异** | 勾稽不成功或金额不等时产生的偏移量（分）；差异必须被解释（未达账项 / 手续费 / 差错） |
| **未达账项** | 已在企业账、未在银行账（或反之）的合法时间性差异；调节表的四大类来源 |
| **调节表** | 银行存款余额调节表：账面余额 ± 未达账项 = 对账单余额 ± 未达账项 |
| **缴款单** | 门店现金交存银行的凭证；本期只要求"能被关联"，不要求生成（见 D14） |
| **批次（batch）** | 一次导入动作的容器，含文件指纹、操作人、行级统计 |
| **行（row）** | 导入文件中的一条数据（含它最终的状态与错误原因） |

### 0.5 角色与权限（总表，A 部分）

| 角色 | 在本部分的职责 | 可见范围 |
|---|---|---|
| **出纳** | 上传流水、处理行级错误、维护未达账项、出具调节表草稿 | 本人被授权账户（`bank_accounts.scope=STORE` 且 `store_id` ∈ 本人归属） |
| **财务主管** | 审核调节表、裁决差异、关闭期间 | 全部账户（含 `scope=GLOBAL`） |
| **店长** | 只读本店日记账与差异汇总 | 本店账户 |
| **审计** | 只读全量批次、调节表历史与操作日志（不可改） | 全量（只读） |

**权限点命名沿用现有风格**（活库已存在：`finance:bank:view` / `finance:bank:query` / `finance:bank:create` / `finance:bank:update` / `finance:bank:delete` / `finance:bank:manage` / `finance:bank:reconcile` / `finance:record:*` / `finance:audit-log:view`）。

| 新增权限点（建议） | 用途 | 谁能授 |
|---|---|---|
| `finance:bank:import` | 上传/重导/提交批次 | 财务主管 |
| `finance:recon:view` | 查看日记账、调节表、差异 | 出纳/店长/审计 |
| `finance:recon:prepare` | 出具/修改调节表草稿、维护未达账项 | 出纳 |
| `finance:recon:approve` | 审核通过/关闭期间 | 财务主管 |

> **待定 D13**：是否新增上述 4 个权限码（需 Owner 裁决；若不新增，则复用 `finance:bank:reconcile` + `finance:bank:manage`，但审计粒度会变粗）。

---

## 1. 第三方流水导入（CSV）

### 1.1 三类来源与典型列

| 来源 `source_type` | 渠道标识 | 典型列（列名以各渠道导出为准，导入器做**别名映射**） |
|---|---|---|
| `WECHAT`（微信商户账单） | 微信支付 | 交易时间、公众账号ID、商户号、特约商户号、设备号、**微信订单号**、**商户订单号**、用户标识、交易类型、交易状态、付款银行、货币种类、应结订单金额、代金券金额、**微信退款单号**、商户退款单号、退款金额、充值券退款金额、**退款类型**、退款状态、商品名称、商户数据包、**手续费**、费率、订单金额、申请退款金额、费率备注 |
| `ALIPAY`（支付宝账单） | 支付宝 | 交易号 / **支付宝交易号**、**商家订单号**、业务类型、商品名称、创建时间、完成时间、门店编号、门店名称、操作员、终端号、对方账户、**订单金额（元）**、商家实收（元）、支付宝红包（元）、集分宝（元）、支付宝优惠（元）、商家优惠（元）、券核销金额（元）、券名称、商家红包消费金额（元）、卡消费金额（元）、**退款批次号/退款金额**、**服务费（元）**、分润（元）、备注 |
| `BANK`（银行流水） | 银行 | 交易日期/时间、**交易流水号（银行唯一）**、借方金额、贷方金额、**发生额**、余额、对方户名、**对方账号**、摘要/用途、**交易类型**（转入/转出/手续费/利息）、附言 |

**统一内部语义**（与现状字段对齐）：

| 内部语义 | 来源映射要点 |
|---|---|
| `txn_time` | 微信「交易时间」→ 支付宝「完成时间」（为空退回「创建时间」）→ 银行「交易日期+时间」 |
| `external_txn_id` | 微信 = 微信订单号；支付宝 = 支付宝交易号；银行 = 交易流水号（**跨来源不互认**，见 D2） |
| `merchant_order_no` | 微信「商户订单号」/ 支付宝「商家订单号」/ 银行「摘要中的订单号」（不一定存在） |
| `amount_fen` | 见 §1.3 单位换算；**收入为正、支出为负**（内部统一符号，见 D6） |
| `fee_fen` | 微信「手续费」/ 支付宝「服务费」/ 银行若单列则取「手续费」 |
| `direction` | `CREDIT`（进账）/ `DEBIT`（出账）/ `REFUND`（退款，独立方向，不并入 DEBIT） |
| `status_raw` | 微信「交易状态」/ 支付宝「交易状态」/ 银行「交易类型」原样保留 |
| `remark` | 微信「商品名称」/ 支付宝「备注」/ 银行「摘要」 |

> **待定 D1**：三类格式是否本轮全覆盖；银行流水因"每家银行列名不同"，是否要求首版采用**列名映射模板**（配置化）而不是硬编码（需 Owner 裁决）。
> **待定 D2**：跨来源统一键 `external_txn_id` 的取值口径（是否允许"渠道前缀 + 原值"，如 `WX:4200...`；以及银行流水号缺失时是否退化为组合键）（需 Owner 裁决）。
> **待定 D7**：本期是否只支持 `currency=CNY`（非 CNY 行按"格式错误+原因"整批拒绝）（需 Owner 裁决）。

### 1.2 导入流程状态机

```
[选择文件] → UPLOADED（落对象存储/临时目录 + 计算 sha256）
      ↓ 解析预检（编码、表头、列名映射、金额格式、时区、行数上限）
  PARSED_OK ────────────────────────────────→ PARSED_FAILED（整批拒绝，给可读原因）
      ↓ 幂等去重（批次内去重 + 与库内去重）
  DEDUPED
      ↓ 落库（bank_transactions 插入 + import_rows 状态回写，行级事务或分批事务）
  COMMITTED（成功/重复/错误 三类计数）
      ↓（差异需人工处理时）
  PARTIAL / MANUAL（存在需人工确认的行：金额为 0、方向不明、账号对不上账户）
```

**关键规则**：

1. **两段式**：`UPLOADED → PARSED_OK` 只做预检与预览（返回前 50 行 + 行级错误），**不落 `bank_transactions`**；由使用者确认后才 `COMMITTED`。
   > **待定 D5**：是否需要这段人工确认（一步到位 vs 两段式）；以及 `COMMITTED` 后是否允许**撤销批次**（软删 `bank_transactions` 并留痕）（需 Owner 裁决）。
2. **批次不可变**：`COMMITTED` 的批次不允许"再塞行"；重导必须产生新批次，靠幂等键判重（保留两次批次的对照关系）。
3. **失败不静默**：任何整批失败必须落 `import_batches.status=FAILED` + `error_summary`，不允许只在界面弹窗。
4. **行级失败不阻断**：单行格式错误 → 该行 `import_rows.status=ERROR`，其余行照常入库；批次最终状态 `PARTIAL`。

### 1.3 编码、金额、表头、负数/退款

| 处理项 | 规则 | 拒绝条件（→ 行级 ERROR） |
|---|---|---|
| **字符编码** | 优先 `UTF-8`（含 BOM）→ 若解码失败回退 `GB18030`（银行常见）；判定结果写入 `import_batches.encoding_used` | 两种都失败 → 整批 `PARSED_FAILED` |
| **金额单位** | 微信/支付宝 CSV 多为**元**（两位小数）→ 一律转**分**：`Decimal(元) × 100` 后 **必须为整数**，禁止 `double`；银行流水若明确「金额（分）」列则原样使用并标记 `unit_hint=FEN` | 出现 3 位以上小数、千分位逗号、全角数字 → ERROR（给原文） |
| **符号约定** | 内部统一：进账正、出账负；退款单独 `direction=REFUND` 且 `amount_fen` 保持**正数**（方向在字段上，避免符号与方向双重表达） | 无法判定方向 → ERROR |
| **表头容错** | 表头行可出现在前 1–5 行；列名做别名映射（如「交易时间 / 交易日期 / 时间」→ `txn_time`）；多余列忽略；缺列仅对**必需列**报错 | 缺 `txn_time` / `amount` / `external_txn_id`（或来源特例）→ 整批 FAILED |
| **负数/退款行** | 微信/支付宝的退款行金额为负或单独退款列 → 统一转 `direction=REFUND`，并**关联原单**（若有订单号） | 退款行无任何关联键 → 仍入库但标 `needs_review=true` |
| **时区** | 一律按 `Asia/Shanghai` 解释无时区时间；带时区（如 ISO8601）按原时区换算到本地 | 未来时间（> 当前 + 1 天）→ ERROR（防脏数据） |
| **行数上限** | 同步 ≤ 50,000 行；超过给"需异步"提示 | — |

> **待定 D8**：CSV 时间是否允许含时区（还是强制"无时区=本地"），以及跨零点交易的 `business_date` 归属（按 `txn_time` 自然日 vs 渠道结算日）（需 Owner 裁决）。
> **待定 D16**：大文件（> 50k 行）是否本期就要支持异步导入任务（否则一律要求拆分文件）（需 Owner 裁决）。

### 1.4 幂等去重策略

**建议主键（唯一索引，按 D3 裁决）**：

| 方案 | 键 | 优点 | 缺点 |
|---|---|---|---|
| **A（推荐）** | `UNIQUE(account_id, source_type, external_txn_id)` | 精确、可解释（渠道内唯一号） | 银行流水号缺失时不可用 |
| B（兜底） | `UNIQUE(account_id, txn_time, amount_fen, counterparty_account?)` | 覆盖无流水号来源 | 同金额同秒的多笔真实交易会被误判重复 |
| C（推荐组合） | A 为主键；A 不可用（无 `external_txn_id`）时用 B 并打 `dedupe_basis='COMPOSITE'` | 兼顾 | 需在行上留痕去重依据 |

**批次内去重**：同一文件内出现重复键 → 保留第一条，后续行 `import_rows.status=DUPLICATE`（`duplicate_of_row_no` 指回第一条）。

**与库内去重**：命中已存在 `bank_transactions` → `import_rows.status=DUPLICATE`，`matched_transaction_id` 指向既有行。

> **待定 D3**：主键方案取 A / B / C 哪一个（需 Owner 裁决；建议 C）。

### 1.5 表结构建议（**建议稿**，落地需 Flyway + Owner 批准；本文不建表）

#### 1.5.1 `import_batches`（批次）

| 字段 | 类型（建议） | 必填 | 说明 |
|---|---|---|---|
| `batch_id` | BIGINT PK | 是 | 批次号 |
| `batch_no` | VARCHAR(32) | 是 | 业务号（建议 `IByyyyMMddNNNN`，与 `FFyyyyMMddNNNN` 同风格），唯一 |
| `source_type` | VARCHAR(16) | 是 | `WECHAT` / `ALIPAY` / `BANK` |
| `account_id` | BIGINT | 是 | 目标账户（`bank_accounts.account_id`） |
| `file_name` | VARCHAR(255) | 是 | 原始文件名 |
| `file_sha256` | VARCHAR(64) | 是 | 文件指纹（同文件重复上传可直接提示） |
| `encoding_used` | VARCHAR(16) | 否 | `UTF-8` / `GB18030` |
| `period_start` / `period_end` | DATE | 否 | 文件覆盖的日期范围（解析后回填） |
| `total_rows` | INTEGER | 是 | 解析出的数据行数 |
| `ok_rows` / `dup_rows` / `error_rows` | INTEGER | 是 | 三类计数（`ok+dup+error = total`） |
| `status` | VARCHAR(16) | 是 | `UPLOADED` / `PARSED_OK` / `PARSED_FAILED` / `COMMITTED` / `PARTIAL` / `FAILED` |
| `error_summary` | TEXT | 否 | 整批失败的摘要 |
| `create_user_id` / `created_by` | BIGINT / VARCHAR | 是 | 审计（与现状一致：`create_user_id` 取登录用户） |
| `create_time` / `update_time` / `deleted` / `version` | — | — | 与全库约定一致 |

#### 1.5.2 `import_rows`（行级明细与回执）

| 字段 | 类型（建议） | 必填 | 说明 |
|---|---|---|---|
| `row_id` | BIGINT PK | 是 | 行ID |
| `batch_id` | BIGINT | 是 | 外键 → `import_batches` |
| `row_no` | INTEGER | 是 | 文件内行号（从 1 开始，含表头偏移后的**文件行号**，便于对照） |
| `raw_json` | JSONB | 否 | 原始行（脱敏后可存；见 D19） |
| `status` | VARCHAR(16) | 是 | `OK` / `DUPLICATE` / `ERROR` / `NEEDS_REVIEW` |
| `error_code` / `error_message` | VARCHAR(32) / VARCHAR(255) | 否 | 可读原因（如 `AMOUNT_FORMAT` / 「金额含千分位」） |
| `duplicate_of_row_no` | INTEGER | 否 | 批次内重复指向 |
| `matched_transaction_id` | BIGINT | 否 | 与库内既有 `bank_transactions` 重复时指向 |
| `transaction_id` | BIGINT | 否 | 成功入库后的 `bank_transactions.transaction_id` |

#### 1.5.3 `bank_transactions`（银行/第三方流水事实表）

| 字段 | 类型（建议） | 必填 | 说明 |
|---|---|---|---|
| `transaction_id` | BIGINT PK | 是 | 主键 |
| `account_id` | BIGINT | 是 | 归属账户（`bank_accounts.account_id`） |
| `source_type` | VARCHAR(16) | 是 | 同批次 |
| `external_txn_id` | VARCHAR(64) | 否 | 渠道唯一号（见 D2） |
| `merchant_order_no` | VARCHAR(64) | 否 | 商户单号（可关联 `orders.order_number` / `order_code`） |
| `txn_time` | TIMESTAMP | 是 | 交易时间 |
| `business_date` | DATE | 是 | 业务日期（口径见 D8） |
| `direction` | VARCHAR(8) | 是 | `CREDIT` / `DEBIT` / `REFUND` |
| `amount_fen` | BIGINT | 是 | 金额（分，正数） |
| `fee_fen` | BIGINT | 否 | 手续费（分）；无则 NULL |
| `balance_after_fen` | BIGINT | 否 | 银行给的"交易后余额"（银行流水常有，微信/支付宝通常无） |
| `counterparty_name` / `counterparty_account` | VARCHAR | 否 | 对方户名/账号 |
| `status_raw` | VARCHAR(64) | 否 | 渠道原始状态 |
| `settle_status` | VARCHAR(16) | 否 | 归一后的结算状态（`SETTLED` / `REFUNDED` / `FAILED` / `UNKNOWN`） |
| `dedupe_basis` | VARCHAR(16) | 是 | `EXTERNAL_ID` / `COMPOSITE`（见 §1.4） |
| `match_status` | VARCHAR(16) | 是 | `UNMATCHED` / `MATCHED` / `IGNORED`（勾稽结果，B 段写） |
| `matched_flow_id` | BIGINT | 否 | 勾稽到的 `fund_flows.flow_id`（B 段写） |
| `needs_review` | BOOLEAN | 是 | 需人工确认（退款无关联键等） |
| `batch_id` | BIGINT | 是 | 来源批次 |
| `remark` | VARCHAR(255) | 否 | 摘要/备注原样 |
| `create_user_id` / `create_time` / `update_time` / `deleted` / `version` | — | — | 同上 |

> **待定 D4**：`bank_transactions` 是否**生成** `fund_flows`（即"到账即记账"）——建议**不生成**，保持"事实层 / 账面层"分离（需 Owner 裁决）。
> **待定 D6**：手续费是否**单独成行**（`fee_fen` + 一条 `flow_category=手续费` 的差异行），还是作为 `bank_transactions.fee_fen` 参与净额计算（需 Owner 裁决）。
> **待定 D19**：`raw_json` 是否保留原行（含账号、对方账号等敏感信息）——还是只保留脱敏副本（需 Owner 裁决；建议脱敏 + 原文仅保留哈希与必要字段）。

### 1.6 接口草案（HTTP）

| # | 方法 & 路径 | 用途 | 请求（关键字段） | 响应（关键字段） |
|---|---|---|---|---|
| 1 | `POST /v1/finance/imports` | 上传 + 预检（不落事实表） | `multipart/form-data`：`file`、`sourceType`(WECHAT|ALIPAY|BANK)、`accountId` | `batchId`、`batchNo`、`status`、`totalRows`、`previewRows[≤50]`、`rowErrors[≤50]` |
| 2 | `GET /v1/finance/imports/{batchId}` | 批次详情 + 统计 | — | 批次字段 + `okRows/dupRows/errorRows` + `errorSummary` |
| 3 | `GET /v1/finance/imports/{batchId}/rows` | 行级回执分页 | `status`、`page`、`size`（≤200） | `records[]`（`rowNo/status/errorCode/errorMessage/rawPreview`）、`total` |
| 4 | `POST /v1/finance/imports/{batchId}/commit` | 确认落库 | — | `status=COMMITTED|PARTIAL`、三类计数 |
| 5 | `POST /v1/finance/imports/{batchId}/cancel` | 撤销未提交批次（D5） | `reason` | `status=CANCELLED` |
| 6 | `GET /v1/finance/bank-transactions` | 银行流水查询 | `accountId`、`from`、`to`、`sourceType`、`matchStatus`、`needsReview`、`page`、`size` | `records[]`、`total` |

**统一约定**：返回沿用 `Result<T>`（`code/message/data/timestamp/success`）；金额字段一律**分**（`*_fen` 后缀或沿用现有 `amount` 语义并在文档标注）；时间 `yyyy-MM-dd HH:mm:ss`。

### 1.7 页面线框描述（"资金管理 → 流水导入"）

```
┌─ 流水导入 ────────────────────────────────────────────────────────────┐
│ [账户 ▼](必填, 仅显示当前用户有权限的账户)  [来源 ▼ 微信/支付宝/银行]  │
│ ┌─────────── 拖拽或点击上传 CSV ───────────┐   [下载模板]            │
│ │  file.csv   (≤10MB / ≤50k 行)            │                          │
│ └──────────────────────────────────────────┘                          │
│ 预检结果：共 1,204 行 │ 可导入 1,180 │ 重复 20 │ 错误 4   [查看错误]  │
│ ┌ 行级预览（前 50 行）───────────────────────────────────────────────┐ │
│ │ 行号 │ 交易时间 │ 单号 │ 方向 │ 金额(元) │ 状态 │ 原因            │ │
│ │ 3    │ 2026-10-23 09:12 │ WX… │ 进账 │ 128.00 │ ERROR│ 金额含千分位 │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│                                    [取消]  [确认导入(1,180 行)]        │
└────────────────────────────────────────────────────────────────────────┘
```

- 列表页（批次历史）：`批次号 / 账户 / 来源 / 文件名 / 时间 / 总数 / 成功 / 重复 / 错误 / 状态 / 操作(详情|重导)`；
- 详情页：批次头 + 三类计数卡 + 行级表（按状态筛选）+ 「重导错误行」入口（导出错误行 CSV，修好后作为新批次导入）；
- 空态/失败态文案要能指路（例如"该文件为 GBK 且缺少『金额』列 → 请用模板核对列名"）。

### 1.8 权限点（本节的落点）

| 动作 | 权限点 | 附加约束 |
|---|---|---|
| 上传/预检/提交/取消批次 | `finance:bank:import`（或复用 `finance:bank:manage`，见 D13） | 只能对自己有权账户操作（`bank_accounts.scope=STORE` 且 `store_id` 匹配，或 `scope=GLOBAL` 且具备主管权限） |
| 查看批次与行级回执 | `finance:recon:view`（或 `finance:bank:view`） | 同账户可见范围 |
| 查询银行流水 | `finance:bank:query` | 同账户可见范围 |

> **待定 D14**：`scope=GLOBAL`（总部账户）的导入权限归属（出纳能不能导总部账户？还是仅财务主管）；以及"缴款单"关联是否本期做（需 Owner 裁决）。

### 1.9 验收标准（可测断言）

| # | 断言 | 判定方式 |
|---|---|---|
| A1-1 | 三类样例文件（各 1 份，含中文表头、BOM、GB18030 各一份）能解析，`total_rows` 与人工计数一致 | 接口返回 + 人工核对 |
| A1-2 | 金额换算无损：`选 100 行，assert 库里 amount_fen == round(元 × 100)`，错误数 = 0 | SQL 断言 |
| A1-3 | **幂等**：同一文件连续导入 2 次，第 2 次 `dup_rows == total_rows` 且 `bank_transactions` 行数不变 | SQL 计数对比 |
| A1-4 | 行级错误可读：构造 5 行错误（千分位、3 位小数、未来时间、缺列、缺方向），`error_rows=5` 且每条 `error_code` 非空 | 接口断言 |
| A1-5 | 整批失败不留垃圾：缺必需列的文件 → `status=PARSED_FAILED`，`bank_transactions` 增量 = 0 | SQL 断言 |
| A1-6 | 审计完整：任一 `bank_transactions` 行都能回溯到 `batch_id` 与 `create_user_id` | SQL 断言（无孤儿） |
| A1-7 | 不破坏既有防线：`consistency_check.py` 5 类仍全 PASS | 脚本退出码 0 |

### 1.10 §1 本轮不实现

- 银行直连 / OpenAPI 拉流水；
- 列名映射的**可视化模板编辑器**（首版按 D1 裁决，走固定映射或配置文件）；
- 异步导入任务队列（> 50k 行先要求拆分，见 D16）；
- 错误行"一键修正后重导"的自动化（只提供错误行 CSV 导出）。

---

## 4. 资金日记账 与 银行存款余额调节表

### 4.1 资金日记账（按账户 × 日期）

**口径**：一条日记账 = 某 `account_id` + 某 `business_date` 的聚合行。

| 列 | 计算 | 说明 |
|---|---|---|
| 期初余额 | 该账户在 `business_date` 之前最后一笔流水的 `balance_after`；无历史则取账户初始余额 | 期初 = 上日期末（必须闭合） |
| 收入合计 | `SUM(amount) WHERE flow_direction=1 AND status=2` | 单位分 |
| 支出合计 | `SUM(amount) WHERE flow_direction=2 AND status=2` | 单位分 |
| 当日净额 | 收入合计 − 支出合计 | — |
| 期末余额 | 期初 + 当日净额 | **必须等于**该日最后一笔 `balance_after`（校验点 V1） |
| 笔数 | 有效流水行数（`status=2`） | 待处理(`status=1`)/已取消(`status=3`)不计入金额，单独计数 |

**日结 / 月结**：

- **日结**：逐日聚合；输出"账户 × 日"明细 + 日合计；
- **月结**：月内各日汇总 + 月初/月末余额；月末余额 = 次月 1 日期初（跨月闭合校验 V2）；
- **封账后不可变**：期间关闭（B/C 段）后，日记账只读；补录必须走"反结账"审批（本文只留接口位）。

> **待定 D12**：调节期间是自然月还是自定义起止（以及是否允许"跨月未结账"）（需 Owner 裁决）。
> **待定 D9**：日记账粒度是否为 `account × day`（是否需要 `× currency` / `× store`）（需 Owner 裁决）。

### 4.2 与 `fund_flows.balance_after` 的关系（谁是真值）

| 层 | 载体 | 语义 | 是否真值 |
|---|---|---|---|
| **账面层** | `fund_flows.balance_after`（每笔流水后的余额快照，分） | 业务流水链路的账面余额演进 | **账面真值（企业账）**：只要流水链完整，它就是"企业认为银行里应该有多少钱" |
| **账户层** | `bank_accounts.balance`（分） | 账户当前余额（写入方为账户服务，已做原子更新） | **账面当前值**，应与"最后一笔 `balance_after`"一致（校验点 V3） |
| **事实层** | `bank_transactions.balance_after_fen`（银行给的交易后余额，分） | 银行侧客观余额 | **银行真值（对账单）** |

**校验点（必须都可断言）**：

| 编号 | 校验 | 不等时的含义 |
|---|---|---|
| V1 | 日记账期末 == 当日最后一笔 `fund_flows.balance_after` | 流水链断链/并发写入丢失 |
| V2 | 月末余额 == 次月 1 日期初 | 期间切分开错日 |
| V3 | `bank_accounts.balance` == 该账户最新一笔 `fund_flows.balance_after` | 账户余额与流水脱钩（历史漂移） |
| V4 | `fund_flows.balance_after` 与 `bank_transactions.balance_after_fen`（同一天最后一笔）之差 == 当日未达账项净额 | **这正是调节表要解释的差** |

> **待定 D10**：`balance_after` 若历史上已漂移（`bank_accounts.balance` 与流水不一致），本部分是否先做**一次基线校准**（以某日银行对账单为锚点重建），还是只在调节表中"解释差异"（需 Owner 裁决）。
> **待定 D11（真值口径）**：`balance_after` 与 `bank_accounts.balance` 谁是唯一真值（建议：流水快照为准，账户余额为派生缓存）（需 Owner 裁决）。

### 4.3 银行存款余额调节表

**标准结构**：

```
调节表（期间 2026-10-01 ~ 2026-10-31，账户 招商银行 ****8821，币种 CNY）
──────────────────────────────────────────────────────────────
  A. 企业账面余额（来自 fund_flows 期末 / bank_accounts.balance）      ¥ 12,345.67
  B. 加：银行已收，企业未收（企业未达）                                ¥    500.00
  C. 减：银行已付，企业未付（企业未达）                                ¥    120.00
  D. 加：企业已收，银行未收（银行未达）                                ¥    300.00
  E. 减：企业已付，银行未付（银行未达）                                ¥     80.00
──────────────────────────────────────────────────────────────
  企业调节后余额 = A + B − C                                          ¥ 12,725.67
  对账单余额（bank_transactions 期末 / 银行对账单录入）               ¥ 12,545.67
  银行调节后余额 = 对账单余额 − D + E                                 ¥ 12,325.67   ← 校验：必须相等
──────────────────────────────────────────────────────────────
  差异（必须为 0）：                                                     ¥      0.00
```

> **口径说明**：调节表两侧各自加减**对方未达**，最终两侧必须相等。上面示例的 D/E 方向为"企业已记、银行未记"，故在银行侧做反向调整。**任一侧算错方向都会导致差异不为 0**，因此校验是硬门禁。

**未达账项四类**（挂账载体见 D17）：

| 类 | 名称 | 判定线索（自动建议） | 方向 |
|---|---|---|---|
| 1 | **企业已收、银行未收** | `fund_flows` 有进账、同期 `bank_transactions` 无对应 | 银行侧加 |
| 2 | **企业已付、银行未付** | `fund_flows` 有出账、同期 `bank_transactions` 无对应 | 银行侧减 |
| 3 | **银行已收、企业未收** | `bank_transactions` 有进账、`fund_flows` 无对应（常见：利息、代收） | 企业侧加 |
| 4 | **银行已付、企业未付** | `bank_transactions` 有出账、`fund_flows` 无对应（常见：手续费、代扣） | 企业侧减 |

> **待定 D15**：本期是否**只做人工勾稽**（不做规则引擎自动匹配）；若做，匹配规则（金额+时间窗+订单号）优先级如何定义（需 Owner 裁决；建议本期人工）。

**硬校验**：

- **V5**：调节后两侧余额必须相等；不等 → 调节表只能存为 `DRAFT`，**禁止** `APPROVED`；
- **V6**：每一条未达账项必须有"挂账人 + 原因 + 期间"，且金额合计与所归类别的列合计一致；
- **V7**：跨期间不得重复挂同一笔（同一 `fund_flows.flow_id` / `bank_transactions.transaction_id` 在同一期间只出现一次）。

> **待定 D17**：未达账项的挂账载体——新建 `reconciliation_items` 表 vs 复用 `fund_flows.remark` 打标 vs 只在 `bank_transactions.match_status` 上标记（需 Owner 裁决；建议新表，便于跨期间追溯与审计）。
> **待定 D18**：调节表是否需要"暂收暂付/在途"等第五、第六类（部分行业要求），本期是否只做四类（需 Owner 裁决）。

### 4.4 接口草案（HTTP）

| # | 方法 & 路径 | 用途 | 请求 | 响应 |
|---|---|---|---|---|
| 1 | `GET /v1/finance/ledger/daily` | 资金日记账（按账户×日） | `accountId`(必填)、`from`、`to`、`page`、`size` | `records[]`（`businessDate/openingFen/incomeFen/expenseFen/netFen/closingFen/flowCount`）、`summary` |
| 2 | `GET /v1/finance/ledger/summary` | 月结汇总（期初/月末/合计） | `accountId`、`month`(yyyy-MM) | `openingFen/monthIncomeFen/monthExpenseFen/closingFen` |
| 3 | `POST /v1/finance/recon/statements` | 生成（重算）调节表草稿 | `accountId`、`periodStart`、`periodEnd`、`bankStatementBalanceFen`（对账单余额，手工或来自 `bank_transactions` 期末） | `statementId`、两侧调节后余额、`diffFen`、`isBalanced` |
| 4 | `GET /v1/finance/recon/statements` | 调节表列表 | `accountId`、`period`、`status`、分页 | `records[]` |
| 5 | `GET /v1/finance/recon/statements/{id}` | 调节表详情（含四类未达） | — | 表头 + `unmatchedItems{type1[],type2[],type3[],type4[]}` + `isBalanced` |
| 6 | `POST /v1/finance/recon/statements/{id}/submit` | 提交审核（DRAFT→SUBMITTED） | `remark` | `status` |
| 7 | `POST /v1/finance/recon/statements/{id}/approve` | 审核通过（SUBMITTED→APPROVED） | `remark` | `status`（`isBalanced=false` 时返回业务错误） |
| 8 | `GET /v1/finance/recon/unmatched` | 未匹配池（两侧） | `accountId`、`from`、`to`、`side`（`BOOK`/`BANK`） | `records[]`（含"建议归类"） |

### 4.5 页面线框描述（"资金管理 → 日记账 / 余额调节表"）

**日记账页**

```
┌─ 资金日记账 ─────────────────────────────────────────────────────────┐
│ [账户 ▼] [期间 2026-10-01 ~ 10-31] [日结|月结 ●]        [导出 CSV]  │
│ 期初 ¥12,000.00 │ 收入 ¥3,200.00 │ 支出 ¥2,854.33 │ 期末 ¥12,345.67 │
│ ┌ 日期 │ 期初 │ 收入 │ 支出 │ 净额 │ 期末 │ 笔数 │ 校验 ┐           │
│ │ 10-30│ 9,800│  600 │ 1,200│ -600 │ 9,200│  12  │ ✔ V1    │           │
│ │ 10-31│ 9,200│ 3,145│  0.00 │3,145 │12,345│  31  │ ✔ V1    │           │
│ └──────────────────────────────────────────────────────────────┘     │
│ 提示条：V3 账户余额与流水一致 ✔                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**调节表页**

```
┌─ 银行存款余额调节表（草稿）──────────────────────────────────────────┐
│ 账户：招商银行 ****8821   期间：2026-10-01 ~ 10-31   [重算] [提交]    │
│ 企业账面余额 ¥12,345.67   ↔   对账单余额 ¥12,545.67                  │
│ ┌ 未达账项 ─────────────────────────────────────────────────────────┐ │
│ │ 类别            │ 笔数 │ 金额 │ 操作                            │ │
│ │ ① 企业已收银行未收│  2   │ +300 │ [查看] [移除]                   │ │
│ │ ② 企业已付银行未付│  1   │  -80 │ [查看]                          │ │
│ │ ③ 银行已收企业未收│  1   │ +500 │ [查看] [新增挂账]               │ │
│ │ ④ 银行已付企业未付│  1   │ -120 │ [查看]                          │ │
│ └───────────────────────────────────────────────────────────────────┘ │
│ 企业调节后 ¥12,725.67   银行调节后 ¥12,725.67   差异 ¥0.00  ✔平衡     │
│ 未平衡时：仅可保存草稿，[提交] 置灰并提示"差异 ¥X 未解释"             │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.6 权限点

| 动作 | 权限点 | 附加约束 |
|---|---|---|
| 查看日记账 / 调节表 / 未匹配池 | `finance:recon:view`（或 `finance:bank:view`） | 账户可见范围（店长限本店） |
| 生成/重算调节表草稿、维护未达账项 | `finance:recon:prepare`（或 `finance:bank:reconcile`） | 仅本账户 + 未关闭期间 |
| 提交/审核 | `finance:recon:approve` | 财务主管；提交人与审核人**不得同一人**（四眼原则） |
| 导出 | `finance:recon:view` + `finance:audit-log:view`（导出需留痕） | — |

### 4.7 验收标准（可测断言）

| # | 断言 | 判定方式 |
|---|---|---|
| A4-1 | 日记账闭合：任取 30 天，逐日 `期初=前日期末`，且 `期末=期初+收入−支出`，不满足行数 = 0 | SQL 断言（V1/V2） |
| A4-2 | 账户余额一致：`bank_accounts.balance == 最新 fund_flows.balance_after`，不一致账户数 = 0（或全部在"已知漂移清单"中） | SQL 断言（V3） |
| A4-3 | 调节表硬门禁：构造 `diff≠0` 的调节表 → `approve` 必须失败（业务错误码非 0） | 接口断言 |
| A4-4 | 四类合计 == 列合计；同类跨期间不重复挂账（V7） | SQL 断言 |
| A4-5 | 平衡时可审核：`diff=0` → `APPROVED`，且留痕（提交人/审核人/时间不同人） | 接口 + SQL |
| A4-6 | 只读可见范围：店长查非本店账户 → 403；审计只读 → 写接口 403 | 接口断言 |
| A4-7 | 不破坏既有防线：`consistency_check.py` 5 类仍全 PASS；`ui_e2e_payment.py` 7/7 仍 PASS | 脚本退出码 0 |

### 4.8 §4 本轮不实现

- 期末结账/反结账审批流（B/C 段）；
- 调节表导出 PDF/打印模板（首版只导 CSV/在线查看）；
- 未达账项的自动清账（跨期自动匹配上一个期间的挂账）；
- 多币种调节。

---

## 5. 待裁决点清单（共 **19** 项）

| ID | 待裁决内容 | 建议 | 影响面 |
|---|---|---|---|
| D1 | 三类 CSV 是否本轮全覆盖；银行流水列名是否走**映射模板（配置化）** | 全覆盖；映射模板 | §1.1 |
| D2 | `external_txn_id` 是否带渠道前缀、缺失时是否退化组合键 | 带前缀 + 退化 | §1.1/§1.4 |
| D3 | 幂等唯一键取 A（external_id）/ B（时间+金额+对方）/ C（组合，推荐） | C | §1.4/表唯一索引 |
| D4 | `bank_transactions` 是否生成 `fund_flows` | 否（事实层/账面层分离） | §1.5.3 |
| D5 | 是否两段式（预检→确认）+ 已提交批次能否撤销 | 两段式 + 可撤销留痕 | §1.2 |
| D6 | 手续费单独成行 vs 净额参与差异 | 单独成行 | §1.3/§4.3 |
| D7 | 是否本期只支持 CNY | 是（非 CNY 整批拒绝） | §1.1 |
| D8 | CSV 时间是否允许带时区；跨零点 `business_date` 归属 | 无时区=本地；按 `txn_time` 自然日 | §1.3 |
| D9 | 日记账粒度 `account × day` 是否够（是否需 currency/store） | 够 | §4.1 |
| D10 | 历史余额漂移是否先做基线校准 | 先校准（以对账单为锚） | §4.2 |
| D11 | `balance_after`（流水快照）vs `bank_accounts.balance` 谁是唯一真值 | 流水快照为准，账户余额为派生 | §4.2 |
| D12 | 调节期间：自然月 vs 自定义起止 | 自然月（首版） | §4.1/§4.4 |
| D13 | 是否新增 4 个权限码（`finance:bank:import` / `finance:recon:view/prepare/approve`） | 新增 | §0.5/§1.8/§4.6 |
| D14 | `scope=GLOBAL` 总部账户的导入/调节权限归属；缴款单关联是否本期做 | 仅财务主管；缴款单下期 | §1.8 |
| D15 | 是否需要规则引擎自动匹配（本期是否纯人工勾稽） | 本期人工 | §0.3 |
| D16 | > 50k 行是否本期支持异步导入 | 不支持，先拆分 | §1.3 |
| D17 | 未达账项挂账载体（新建表 / `fund_flows.remark` / `match_status`） | 新建 `reconciliation_items` | §4.3 |
| D18 | 调节表是否只做四类未达（是否要"暂收暂付/在途"） | 只做四类 | §4.3 |
| D19 | `import_rows.raw_json` 是否保留原始行（含账号等敏感信息）或只留脱敏副本 | 脱敏副本 + 原文哈希 | §1.5.2 |

---

## 6. 附：与现有系统的兼容性声明（不破坏清单）

| 现状 | 本文处理 |
|---|---|
| 支付编码单轨 0/1/2/3/4/5 | 导入侧**不新增编码**；`bank_transactions` 用 `direction` 独立表达进出，不做编码复用 |
| `fund_flows.order_id` 关联 | 导入侧通过 `merchant_order_no` 反查订单，**只做建议匹配**，不写 `fund_flows`（写入在 B 段） |
| `bank_accounts` 门店隔离 | 导入与查询一律带账户可见范围校验；**禁止**"无归属→默认门店"式兜底 |
| 余额原子更新 | 本文只读余额，不写余额；若 D10 校准落库，必须复用现有原子更新通道 |
| `create_user_id` 审计 / 流水状态默认已完成 | 新表同样要求 `create_user_id` + `created_by`；导入不改变 `fund_flows.status` 默认值 |
| `scripts/qa/*` 三道防线 | 新增导入/调节能力后，两道 e2e 与 5 类巡检必须仍全绿（见 A1-7 / A4-7） |
