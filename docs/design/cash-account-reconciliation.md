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
---

## 11. v3 修正（2026-10-26，审计补齐后）

### 11.1 配对时间字段：改为 `fund_flows.refund_time`（原 §3.1/§3.2 修正）

v2 说「以 POS 端 `refund_time` 为准」，但 `fund_flows` 当时只有 `business_date`（**天**）与 `create_time`（时刻，非业务时间），`order_refund_records.refund_time` **未写入流水** → 配对键无字段可落。**v3 定案：给流水加列**。

```sql
-- 迁移 V20261026_005__fund_flows_refund_time.sql
ALTER TABLE fund_flows ADD COLUMN IF NOT EXISTS refund_time timestamptz;
COMMENT ON COLUMN fund_flows.refund_time IS 
'退款发生时刻（来自 order_refund_records.refund_time，POS 端时间）；仅 flow_category=2 写入，NEW-19 配对键'
CREATE INDEX IF NOT EXISTS idx_fund_flows_refund_time 
    ON fund_flows (account_id, refund_time) WHERE flow_category = 2 AND deleted = 0;
-- 回填：
UPDATE fund_flows f SET refund_time = r.refund_time
  FROM order_refund_records r
 WHERE f.refund_record_id = r.refund_id AND f.refund_time IS NULL AND f.flow_category = 2;
```

写入侧：`PosRefundFlowRecorder` 写流水时 `setRefundTime(record.getRefundTime())`（**不是** `now()`）；`refund_time` 为空时**拒绝写入**（`BusinessException(400)`「退款发生时刻缺失」），不允许回落到 `now()`。

**配对口径（替换 v2 §3.1）**：一律用 `fund_flows.refund_time`（= POS 端退款时刻）与班次 `[start_time, end_time]` 比较；`business_date` 只用于报表分日，`create_time` 只用于审计，**两者都不参与归属判定**。

### 11.2 A32 / A36 互斥（原 §6 补充）

- `A32`：**只查银行户**（`account_type = 1`），判「无等额银行退款且不在未清明细」的孤儿；
- `A36`：**只查现金户**（`account_type = 2`），判「超期未被缴款单闭环」；
- 两者**列集合互斥**（按 `account_type` 分流），任何一条流水只可能被其中一条判；
- **撤 `account_type=1` 时**：A32 恢复全账户口径的同时，必须把 A36 改为「现金户 + 未被缴款单闭环」并**在 A32 中排除现金户**（等价于把 `account_type` 分流写进两条 SQL，而不是让两条都覆盖全部账户），否则同一笔现金退款会被两条同时报出。

### 11.3 断言「分母」字段（回应「无数据即 PASS」）

每条判定断言必须输出**分母**（被检查行数）；**分母 = 0 时输出 `[INFO]`（`skipped_no_data`），不得输出 `[PASS]`**，避免「没数据」被当成「都对」。

| 编号 | 判据 | 分母定义 | 分母=0 时 |
|---|---|---|---|
| A30 | 有退款额但无退款流水 | `count(orders WHERE refund_amount>0 AND deleted=0)` | `[INFO] A30 skipped_no_data denom=0` |
| A32 | 银行户孤儿退款 | `count(现金退款流水 account_type=1)` | 同上 |
| A33 | 逐单退款对拍 | 同上（`refund_amount>0` 订单数） | 同上 |
| A36 | 现金户超期未闭环 | `count(现金退款流水 account_type=2)` | 同上 |
| A37 | 落不进班次窗口 | 同上 | 同上 |
| A39 | 缴款单恒等式 | `count(现金户且非 DRAFT 的 deposit_slips)` | 同上 |

> 实现要求：判定行格式统一为 `[PASS]/[FAIL]/[INFO skipped_no_data]`，且**分母列随行输出**（如 `denom=12`），便于审计回溯。

## 12. v3 补充（2026-10-26 13:40，5 点确认后）

### 12.1 人工确认归属的权限码：**新建，不复用**

- 不复用 `finance:deposit-slip:submit`（提交缴款单）/`receive`（收款确认）：这两个是**资金交接**语义，人工归属是**账务裁决**，混用会让「谁能改账」无法审计；
- 新建 **`finance:deposit-slip:confirm-attribution`**（另加 `finance:deposit-slip:confirm-attribution:view` 如需只读），绑定角色：财务主管 + 店长（收银员**不可**自行终审）；
- **因此 P2-8 对该端点是阻塞项**（需建码 + 绑角色 + `@PreAuthorize`），已列 §12.5。

### 12.2 三个隐含假设的处置

**A. `refund_component > expected_amount` 时怎么办**
→ **不拒绝生成缴款单**：现金退款可能来自**上一班次遗留现金**（本班次无对应收款），拒绝生成会卡住交班。做法：
- 照常生成缴款单，`refund_component_fen` 如实写入；
- `difference_net_fen` 允许为负（表示「应存净额为负」，即本班次净流出）；
- **页面标红 + 文案**：「本班次退款额（X 分）超过应存现金（Y 分），差额来自上一班次遗留现金或异常，请核对」；
- **A39 不因此判红**（恒等式仍成立）；新增 **A40（判定）**：`refund_component_fen > expected_amount` 的缴款单数 → 期望 0（超出即需人工解释，**不是静默**）。

**B. 同一退款能否归属两个班次**
→ **只能选一个**（互斥）。`refund_record_id` 在 `deposit_slips` 的归属是**单值**，实现上：
- 归属记录写 `refund_shift_attribution(refund_record_id UNIQUE, shift_id, slip_id, operator, confirmed_at, reason)`；`UNIQUE(refund_record_id)` 保证**一个退款只归一个班次**；
- 若要拆分（少见：跨班次的合并退款），**不支持**，需人工拆成两笔退款单后再归属 —— 避免同一笔钱被两个班次各算一次。

**C. 人工确认的幂等与锁**
| 问题 | 处置 |
|---|---|
| 确认后能否改 | **可改，但留痕**：再次确认覆盖 `refund_shift_attribution`，并追加审计 `REFUND_SHIFT_ATTRIBUTION_CHANGED`（记录 old/new shift_id、operator、reason）；不计「重复确认」为错误 |
| 确认期间能否生成缴款单 | **能生成**，但该退款**不计入任何班次**的 `refund_component`，缴款单标 `pending_manual_count > 0` + 页面提示；确认后**重算**该缴款单（不需要重新生成） |
| 未确认期间缴款单是锁定还是用临时值 | **不锁定、不用临时值**：`refund_component_fen` 只统计「已确认/自动归属」的退款，待确认的不计入（宁少不多，避免虚假平衡）；`A38` INFO 显示待确认笔数 |
| 并发 | 确认操作用 `refund_shift_attribution` 唯一键 + 乐观锁（`version`）；两个操作员同时确认同一退款 → 后者 409「该退款已被确认，请刷新」 |

### 12.3 断言分母与验收标准（实测）

- 当前分母：**A30 = 33**（`orders.refund_amount>0` 且未删）、**A33 = 33**（同源）、**A32 = 36**（`flow_category=2` 且 `refund_record_id` 非空）；
- **两条分母均 > 0** → 引入「分母=0 输出 INFO」后，**当前 PASS/INFO 计数不变**（仍 22 判定项 PASS、13 INFO），验收标准**保持**「22 判定项全 PASS」；
- 该机制的价值在**未来**：若数据被清空，断言会输出 `[INFO] skipped_no_data` 而非 `[PASS]`，避免「没数据 = 都对」的假绿。

### 12.4 权限对表复核（用 information_schema 自查后）

- 真实结构：`role_permissions(id, role_id varchar, permission_id **varchar**, deleted smallint)`；`permissions.permission_id` 为 **bigint**，业务键是 `permission_id_str`；
- **原先查询返回空的原因**：`role_permissions.permission_id` 是 **varchar**，与 `permissions.permission_id`（bigint）类型不匹配 → 按 `permission_id_str` 重查；
- 重查结果：**绑定 1719 条**、覆盖 **430** 个权限码、**孤儿绑定 2 条**（`role_permissions.permission_id` 在 `permissions` 里不存在 → 需清理，记 P2）；
- `finance:%` 码中**没有任何一个码是 0 角色**（`HAVING count=0` 返回 0 行）→ **既有 finance 权限码绑定完整**；
- **结论**：P2-8 对**既有**码已澄清；对**新建**码（`confirm-attribution`）仍是待办（见 12.5）。

### 12.5 阻塞项（进实现前）

1. **新建权限码 `finance:deposit-slip:confirm-attribution`**（+ 绑定角色 + `@PreAuthorize` + 权限矩阵文档）；
2. **清理 2 条孤儿 `role_permissions` 绑定**（`permission_id` 不存在）；
3. 迁移清单追加：`deposit_slips.refund_component_fen`、`deposit_slips.difference_net_fen`、`fund_flows.refund_time`、新表 `refund_shift_attribution`。

### 12.6 `order_refund_records.refund_time` 的写入与语义（**待补证**）

- 写入位置：退款入口（`PosOrderPaymentServiceImpl#refundNewOrder` / `OrderNewServiceImpl#approveRefund`）在插入 `order_refund_records` 时写入；
- **语义判定未完成**：需确认它是「POS 端退款操作时刻」还是「退款单创建时刻」（两者在异步审批场景会差数分钟到数天）；
- **NULL 率未取到**（本轮 SQL 未返回，疑该表无 `refund_time` 列或列名不同）→ **列为阻塞**：进实现前必须贴出 `\d order_refund_records` 与写入行号。
