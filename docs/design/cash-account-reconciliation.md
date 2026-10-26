# 现金户对账模型（NEW-19 设计稿）

> 状态：**待评审**（Owner 审阅通过后才写实现）｜ 编号 NEW-19 ｜ 关联：A32 的 `account_type=1` 过渡条件、A19 的配对例外、`deposit_slips` 短款机制
> 前置事实：P4-5 已把「退款流水（`flow_category=2`）」纳入勾稽并与银行侧退款 1:1 配对；但**现金退款没有银行侧对应**（钱从抽屉出），因此现金户的退款流水会永久停在 **B（企业已付、银行未付）**，`A32` 只好用 `account_type=1` 把它排除。本设计要给出**现金户真正的闭环路径**，然后移除该过渡条件。

## 1. 现状与问题

| 事实 | 影响 |
|---|---|
| 现金户（`bank_accounts.account_type=2`）的收款是 POS 现金销售，支出主要是**存行**（缴款单）与**现金退款** | 现金户的「银行对账单」实际是**缴款单 + 现金盘点** |
| `deposit_slips.expected_amount` = 班次窗口内**现金销售额**（`sumCashPayments`） | 若班次内有现金退款，`expected` **没有**扣掉退款 → 实存必然偏低 → `difference_amount` 把这些退款混进「短款」 |
| 现金退款流水（category=2, direction=2）在现金户 statement 里落 **B** | 没有任何机制把它与缴款单关联 → **永久挂账**，只能靠 A32 的 `account_type=1` 屏蔽 |
| A19 已支持 `[refundPair=…]` 的同额对例外 | 现金 B 项若与缴款单配对，需要同类「配对 token」机制，不能新造一套 |

## 2. 设计结论（先给答案，再给依据）

**核心判断：现金退款不能与 `difference_amount` 按金额配对。** `difference_amount` 是「应存 − 实存」的**聚合差**，同一张缴款单的差额可能同时包含：少存、错存、找零错误、现金退款、被挪款 —— 按金额相等去「碰配」= **假核销**（把 A 笔的差额记到 B 笔退款上）。

**因此配对粒度改为「缴款单 ↔ 班次窗口内的现金退款流水集合」**，依据是两者共享**同一业务维度（门店 + 班次时间窗）**，而不是同一金额。

### 2.1 对四个必答问题的回答

**a) 现金退款的 B 类怎么和缴款单的 `difference_amount` 配对？**

不配对 `difference_amount`。配对链是：

```
现金退款流水 (category=2, direction=2, store, business_date, create_time)
        │  按「门店 + 落入该缴款单的班次窗口」匹配
        ▼
deposit_slips (store_id, shift_id → pos_shifts.start_time/end_time)
```

`difference_amount` 只用于**校验这条链是否自洽**（见 2.2 的恒等式），不作为配对键。

**b) `difference_amount` 是总额差，怎么拆到单笔现金退款？**

**不拆。** 单笔粒度由「退款流水」自己保证（每笔一条 item，金额各自独立）；缴款单侧只承担「班次窗口内的退款总额」这一聚合值，用于校验：

```
expected_amount  = Σ 班次窗口内现金收款             -- 现有口径不变
refund_component = Σ 班次窗口内现金退款（新增字段/现算）
expected_net     = expected_amount - refund_component  -- 「应存净额」
difference_net   = actual_amount - expected_net        -- 「净短款」= 真实长短款（不含退款）
```

→ 一笔现金退款只会体现在 `refund_component` 里一次，而且与单笔流水一一对应；`difference_amount`（旧口径）继续保留以兼容历史与既有短款审计，**新增 `difference_net_fen`** 表达「剔除退款后的真实短款」。

**c) 配对成功后，B 类转 CLOSED 还是标记「已由缴款单闭环」？**

**标记「已由缴款单闭环」，不删不转 CLOSED。** 做法与 P4-5 一致：

- `reconciliation_items.cleared_flag = true`；
- `reason` 追加 `[slipPair=<slip_no>]`（沿用 A19 的 token 机制，配对形态不再计入「同额两侧并存」）；
- 该 item 从 `ambiguousItemCount`、四类未达合计、A19 假平衡判据中**全部退出**（复用 `cleared_flag=false` 那一套口径，**不新造真源**）。

**d) 配对不成功（缴款单还没生成）的 B 类怎么处理？**

分三段处理，**不静默**：
| 场景 | 处理 |
|---|---|
| 退款当天、缴款单尚未生成（正常） | 保持 B 未清，`A32-DIST` 归入 `cash-B（待缴款单）`，不算孤儿 |
| 超过 `CURRENT_DATE - 1` 仍无缴款单（**异常**） | 计入 **A36（判定）** → 期望 0；文案「存在 N 笔现金退款未由任何缴款单闭环」 |
| 缴款单已存在但窗口不覆盖该退款（配置/班次数据错） | 计入 **A37（判定）**：退款落不进任何窗口 → 期望 0（提示班次时间窗缺失，与 P4-1 的「班次缺少起止时间」同类） |

### 2.2 关键恒等式（可断言、可解释）

```sql
-- 对每张缴款单：净短款必须等于「应存净额 − 实存」
actual_amount - (expected_amount - refund_component) = difference_net_fen
-- 且 refund_component 必须等于该班次窗口内现金退款流水合计
refund_component = (SELECT COALESCE(SUM(amount),0) FROM fund_flows
                     WHERE flow_category=2 AND flow_direction=2 AND deleted=0 AND status=2
                       AND account_id=<现金户> AND create_time BETWEEN shift.start_time AND shift.end_time)
```

## 3. 落地要素

### 3.1 表结构（最小改动）

```sql
-- 迁移 V20261026_004__deposit_slip_refund_component.sql
ALTER TABLE deposit_slips ADD COLUMN IF NOT EXISTS refund_component_fen bigint NOT NULL DEFAULT 0;
ALTER TABLE deposit_slips ADD COLUMN IF NOT EXISTS difference_net_fen   bigint;  -- actual - (expected - refund_component)
COMMENT ON COLUMN deposit_slips.refund_component_fen IS 
'班次窗口内现金退款流水合计（分）；用于把退款从短款里剔除，NEW-19'
COMMENT ON COLUMN deposit_slips.difference_net_fen IS 
'净短款 = actual - (expected - refund_component)；旧 difference_amount 保留兼容'
```

**不新增对账表**：配对结果落在 `reconciliation_items` 的 `cleared_flag` + `reason`（与 P4-5 同一真源）。

### 3.2 生成/处置流程（伪代码）

```text
# 1) 缴款单生成时（SL1 / 存行前）
refund_component = SUM(fund_flows.amount WHERE 现金户 AND category=2 AND direction=2
                       AND create_time BETWEEN shift.start AND shift.end AND deleted=0 AND status=2)
difference_net   = actual - (expected - refund_component)

# 2) 每次生成/重算调节表时（现金户 statement）
for each 现金退款 B 项 r:
    slip = SELECT ... FROM deposit_slips d JOIN pos_shifts s ON d.shift_id=s.shift_id
            WHERE d.store_id = r.store AND r.create_time BETWEEN s.start_time AND s.end_time
              AND d.deleted = 0 ORDER BY d.slip_id DESC LIMIT 1
    if slip is not null:
        mark cleared(r, "[slipPair=" + slip.slip_no + "]")   # 复用 cleared_flag + reason token
    else:
        keep uncleared(B)  # 交给 A36/A37 判超期/窗口缺失
```

### 3.3 A32 的 `account_type=1` 条件如何移除

**分两步，不一次性撤**：
1. **本设计落地后先并行观察一周**：A32 保留 `account_type=1`，同时新增 A36/A37 独立判定现金户；两者都为 0 才进入第 2 步。
2. **移除条件**：删掉 `AND COALESCE(ba.account_type,1)=1`，把 A32 恢复为「全账户」口径；`A32-DIST` 的 `cash-B` 桶同时删除（此时现金 B 应由缴款单闭环，未闭环的由 A36 报出）。

### 3.4 断言清单（新增/修改）

| 编号 | 类型 | 判据 | 期望 |
|---|---|---|---|
| **A36** | 判定 | 现金退款流水（category=2, direction=2, 现金户）`business_date < CURRENT_DATE-1` 且**未** `cleared_flag=true` 且**无**窗口匹配的 `deposit_slips` | 0 |
| **A37** | 判定 | 现金退款流水存在但**落不进任何班次窗口**（`pos_shifts` 缺 start/end 或窗口不覆盖） | 0 |
| **A38** | INFO | 现金退款闭环率：`cleared([slipPair=…]) / 全部现金退款` | 展示 |
| **A19** | 修改 | 配对例外扩展：`[refundPair=…]` ∪ `[slipPair=…]` 两种 token 均视为「已识别配对形态」 | 两形态均 0 |
| **A32** | 修改 | 第 2 步后移除 `account_type=1` | 0 |
| **A39** | 判定 | 缴款单恒等式：`actual - (expected - refund_component) = difference_net_fen` 且 `refund_component` 等于窗口内退款流水合计 | 0 |

## 4. 风险与反假核销设计

| 风险 | 对策 |
|---|---|
| **假核销**（B 项被错误地判给一张缴款单） | 配对维度是「门店 + 班次时间窗」，**不是金额**；且一对多时按 `slip_id DESC` 只取最近一张并在 reason 里写明 slip_no，可回溯 |
| 窗口重叠（两班次时间窗交叉） | 生成缴款单时已要求 `pos_shifts.start_time/end_time` 完整（P4-1 已有该门禁）；A37 专门抓「落不进窗口」 |
| 与既有 `difference_amount` 短款审计冲突 | **不动旧字段**，新增 `difference_net_fen`；旧报表继续用 `difference_amount`，新判据用净值，两者差值恰为 `refund_component`（可互相验证） |
| 时间窗内退款属于「非当前班次」现金 | 退款流水的 `create_time` 是真实发生时刻；若跨班次（如 23:59 退、00:01 存）会落不进窗口 → A37 报出，人工决定是否放宽到「±1 天」 |

## 5. 待 Owner 评审确认（3 个决策点）

1. **配对维度**：确认「门店 + 班次窗口」可行（而不是按金额/按退款单号）？
2. **`expected_amount` 是否改为扣减退款**：本设计**不改旧字段语义**，而是新增 `refund_component_fen` + `difference_net_fen`；如果你希望 `expected` 直接扣减退款（更直观，但会影响历史报表口径），需明确。
3. **跨班次退款**（窗口边界）：默认不宽容（落不进 → A37 报出）；是否允许 ±1 天匹配？

## 6. 工作量估计

| 项 | 人日 |
|---|---|
| 迁移 + 缴款单 `refund_component`/`difference_net` 计算与回填 | 1.5 |
| 调节表现金户配对（`[slipPair=…]` + cleared_flag） | 1.5 |
| A36/A37/A38/A39 + A19 例外扩展 | 1.0 |
| E2E（`e2e_cash_recon.py`：退款→缴款单→配对→A36/A37=0；跨班次落不进→A37 报红） | 1.0 |
| **合计** | **5 人日** |