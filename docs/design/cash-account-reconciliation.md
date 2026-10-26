# 现金户对账模型（NEW-19 设计稿 v2）

> 状态：**待评审**（v2 按 Owner 三处修正重写）｜ NEW-19 ｜ 关联：A32 的 `account_type=1` 过渡条件、A19 配对例外、`deposit_slips` 短款机制
> v2 变更：① 撤回全部「宽容窗」，改为**交班边界人工确认**；② 新增 **§2 字段盘点**（逐字段贴代码位置）；③ 「跨班次退款」是**伪概念**，配对一律以 **POS 端 `refund_time`** 为准。

## 1. 现状与问题

| 事实 | 影响 |
|---|---|
| 现金户的「对账单」实际是**缴款单 + 现金盘点** | 没有银行流水可配，退款流水只能靠缴款单闭环 |
| 现金退款流水（`flow_category=2`, `direction=2`）在现金户 statement 落 **B** | 无机制与缴款单关联 → **永久挂账**，只能靠 A32 的 `account_type=1` 屏蔽 |
| `deposit_slips.expected_amount` 由**现金支付记录重算**（不含退款扣减，见 §2） | 班次内有现金退款时，实存必然偏低 → 退款被混进「短款」 |

## 2. 字段盘点（**逐字段贴代码位置**，不凭记忆）

### 2.1 `deposit_slips`

| 字段 | 当前语义（代码位置） | 是否在用 | 与本设计关系 |
|---|---|---|---|
| `expected_amount` | **应交额 = 班次窗口内现金支付合计**，由 `order_payment_records`（`payment_method=1`）重算为权威；`closing_cash` 只展示不参与（`DepositSlipServiceImpl:202` TD-03 注释；`:217` METRIC 打点带 `expected`） | **在用（权威）** | **部分能用**：作为「现金毛收款」复用；**不含退款扣减**（退款写 `order_refund_records`，不在 `order_payment_records` 内）→ 不能直接当「应存净额」 |
| `actual_amount` | **实收/实存金额**，DRAFT 时为 0（`:204`），收款时写入（`:339` 回显 `actualAmount`） | **在用** | **能用上**：净值公式的减数 |
| `difference_amount` | **`actual_amount − expected_amount`**（`:205` AC-21 口径注释、`:318` 同口径注释、`:207` DRAFT 阶段 = `0 − expected`） | **在用（报表/审计）** | **部分能用**：旧口径是「毛差」，**含退款影响**；保留兼容，新增 `difference_net_fen` 表达「剔除退款后的真实长短款」 |
| `slip_no` / `store_id` / `shift_id` / `business_date` / `status` / `cashier_*` | 单号、门店、班次、业务日期（= 班次 `end_time` 日期，`:197-199` TD-36）、状态机、收银员 | 在用 | **能用上**：配对维度「门店 + 班次」直接复用 `store_id` + `shift_id` |
| （新增）`refund_component_fen` | 班次窗口内**现金退款流水合计** | 新增 | **本设计核心字段** |
| （新增）`difference_net_fen` | `actual − (expected − refund_component)` | 新增 | 净值口径 |

### 2.2 `pos_shifts`

| 字段 | 当前语义（代码位置） | 是否在用 | 与本设计关系 |
|---|---|---|---|
| `closing_cash` | **仅展示用**，短款绝不回写该字段（`DepositSlipServiceImpl:68-69`「短款不改 `closing_cash`：应交额以 `order_payment_records` 重算为权威，`closing_cash` 只作展示」；`:333` 红线 1/5 同述） | 在用（展示） | **用不上**：不参与配对、不参与恒等式；**不废弃**，但本设计不得写入它 |
| `start_time` / `end_time` | 班次起止时间 | 在用 | **能用上**：配对窗口唯一来源（P4-1 已要求起止完整） |
| `store_id` / `shift_id` / `employee_id` | 门店、班次号、收银员 | 在用 | **能用上** |
| `total_amount` | 班次营业额（含非现金支付） | 在用（统计） | **用不上**：含微信/支付宝，与现金配对无关；**不参与**任何配对或恒等式 |

### 2.3 盘点结论（**直接影响恒等式写法**）

`expected_amount` **不含退款扣减**（退款在 `order_refund_records`，不在 `order_payment_records`）→ 因此：

- **不能**写 `expected_amount − refund_component` 当成「系统现值」，因为 `expected_amount` 语义是「毛收款」，不是「净应存」；
- 正确做法：**新增字段** `refund_component_fen`（净额组件）+ `difference_net_fen`（净差），**旧字段一个不改**；
- 恒等式见 §4。

## 3. 配对规则（**不使用任何宽容窗**）

### 3.1 配对键：POS 端 `refund_time` vs 班次 `[start_time, end_time]`

**配对一律以 POS 端退款发生时刻为准**，与缴款单生成时间、提交时间、审核时间**完全无关**。

> 「跨班次退款」是**伪概念**：一笔退款只有一个发生时刻，它必然落在某一个班次窗口内或落在交班边界上。系统不做「±N 分钟/±1 天」的模糊匹配，**不猜归属**。

| 情况 | 归属 | 自动化 |
|---|---|---|
| `refund_time` 落在班次 A 的 `[start_time, end_time]` 内（且距两端 > N 分钟） | **归班次 A** | 自动 |
| `refund_time` 落在班次 B 的窗口内（B ≠ A） | **归班次 B** | 自动 |
| `refund_time` 落在**交班边界**：距任一班次的 `start_time` 或 `end_time` ≤ **N 分钟**（N 可配，默认 **10**） | **不自动归属** → 标记 `PENDING_MANUAL` | **人工确认** |

**缴款单生成时点不影响配对**：缴款单生成时按 `refund_time` 匹配该班次窗口内的退款流水计算 `refund_component`；退款流水先到、缴款单未生成时，退款流水挂在 B 类未清，等缴款单生成再算（由 A36 判超期）。

### 3.2 交班边界：人工确认（系统只呈现，不决定）

1. **标记**：`refund_time` 落在边界带（交班前后各 N 分钟）的现金退款，写入 `reconciliation_items.reason` 的 `[shiftBoundary]` 标记，状态保持未清；
2. **呈现**：缴款单详情页新增「**待确认归属的退款**」区块，列出：退款单号、金额、`refund_time`、候选班次（上一班次 / 本班次 / 下一班次，含各自 `[start,end]`）；
3. **人工选择**：收银员/财务选择「归上一班次 / 归本班次 / 归下一班次」→ 写 `finance_audit_logs`（`action=REFUND_SHIFT_ATTRIBUTION_CONFIRMED`，`operation_data` 记 `refund_record_id / chosen_shift_id / candidates / operator / reason`）；
4. **确认后**：按所选班次重算该缴款单的 `refund_component_fen` 与 `difference_net_fen`，并 `markCleared(reason="[slipPair=<slip_no>][manual=<shift_id>]")`；
5. **未确认前**：该退款**不算任何班次的 `refund_component`**（避免两边都算导致双计），缴款单的 `difference_net_fen` 标注 `pending_manual_count>0`，页面提示「有 N 笔退款待确认归属」。

**N 的来源**：配置项 `finance.cash.shift-boundary-minutes`（默认 10），不写死、不入库硬编码。

## 4. 恒等式（基于 §2 的实际字段定义）

```sql
-- 每张缴款单（仅现金户）：
refund_component_fen = Σ 该班次窗口内「已归属本班次」的现金退款流水
                       （排除 [shiftBoundary] 待确认的，排除人工判给其它班次的）

difference_net_fen   = actual_amount - (expected_amount - refund_component_fen)
                       -- expected_amount 是「毛现金收款」（§2.1），故减去退款组件即得「应存净额」

-- 与旧字段的关系（可互验，不改旧字段）：
difference_amount = actual_amount - expected_amount                    -- 旧口径，含退款影响
difference_net_fen - difference_amount = refund_component_fen          -- 两者差值恰为退款组件
```

> 注意：`expected_amount` 本身**不改语义**（仍是毛收款）；「应存净额」只出现在 `difference_net_fen` 的算式里。若 Owner 希望把 `expected_amount` 改成净额，会破坏历史报表口径 —— 本设计**不改**。

## 5. 配对结果与状态

- 配对成功：`reconciliation_items.cleared_flag = true`，`reason` 追加 `[slipPair=<slip_no>]`（人工确认的再加 `[manual=<shift_id>]`）；
- 退出所有未清口径：四类未达合计、`ambiguousItemCount`、A19 假平衡判据（复用 `cleared_flag=false` 单一真源）；
- **不删、不转 CLOSED**，保留可回溯。

## 6. A32 的 `account_type=1` 条件如何移除

1. **并行观察**：本设计落地后，A32 保留 `account_type=1`，同时新增 A36/A37 独立判现金户；两者都为 0 连续一周才进第 2 步；
2. **移除**：删掉 `AND COALESCE(ba.account_type,1)=1`，恢复 A32 全账户口径，同时删除 `A32-DIST` 的 `cash-B` 桶。

## 7. 断言清单

| 编号 | 类型 | 判据 | 期望 |
|---|---|---|---|
| **A36** | 判定 | 现金退款（category=2, direction=2, 现金户）`business_date < CURRENT_DATE-1` 且未 `cleared_flag=true` 且无 `[slipPair]` | 0 |
| **A37** | 判定 | 现金退款**落不进任何班次窗口**（`pos_shifts` 缺 `start_time/end_time` 或窗口不覆盖） | 0 |
| **A38** | INFO | 现金退款闭环率 + **待人工确认笔数**（`[shiftBoundary]` 未处置） | 展示 |
| **A39** | 判定 | 缴款单恒等式：`difference_net_fen = actual − (expected − refund_component)` 且 `refund_component` = 窗口内已归属退款流水合计 | 0 |
| **A19** | 修改 | 配对例外扩展：`[refundPair=…]` ∪ `[slipPair=…]` | 两形态均 0 |
| **A32** | 修改 | 第 6 步后移除 `account_type=1` | 0 |

## 8. 风险与反假核销

| 风险 | 对策 |
|---|---|
| 假核销（B 项错判给某张缴款单） | 配对维度 = 门店 + 班次窗口（**不含金额**）；边界不猜、人工确认并留审计 |
| 待确认退款被两边同时计入 | **未确认前不算任何班次**（`refund_component` 不含它），页面提示待确认数量 |
| 与旧 `difference_amount` 报表冲突 | 旧字段不改；差值恒等于 `refund_component`，可互验 |
| 班次时间窗缺失 | A37 专门报出；P4-1 已有「班次起止必须完整」门禁 |

## 9. 待 Owner 评审的决策点（v2 已收敛 2 个）

1. **边界带宽度 N**：默认 10 分钟、走配置 `finance.cash.shift-boundary-minutes` —— 确认默认值与配置键名；
2. **人工确认的角色**：收银员可自选，还是必须财务审批？（涉及权限矩阵，建议：收银员提交 → 财务确认）
3. `expected_amount` 保持「毛收款」语义不变（本设计推荐）—— 确认。

## 10. 工作量（v2）

| 项 | 人日 |
|---|---|
| 迁移（`refund_component_fen` / `difference_net_fen`）+ 缴款单计算与回填 | 1.5 |
| 现金户配对 + 边界标记 + 缴款单详情页「待确认退款」区块 + 人工归属接口/审计 | 2.5（含前端 1.0） |
| A36/A37/A38/A39 + A19 例外扩展 | 1.0 |
| E2E（`e2e_cash_recon.py`：窗口内自动归属 / 边界待确认 / 人工选择后闭环 / A36/A37=0） | 1.5 |
| **合计** | **6.5 人日** |