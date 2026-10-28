# 技术债：Schema 层（2026-10-28）

## P0-1　全库时间列为 `timestamp`（无时区），依赖单时区假设

**现状**
```text
500+ 张既有表的时间列几乎全部为 `timestamp without time zone`
Java 实体统一用 `LocalDateTime`（无时区）
DB TimeZone = Asia/Shanghai，JVM 同区，中国无夏令时
```

**2026-10-28 裁定（Lead）**：新表**沿用 `timestamp without time zone`**（方案 C），不采用 Owner 标准的 `timestamptz`。

理由（三条，按权重排序）：
1. **一致性与风险对称**：全库同类型 → 任何日期比较/统计/报表 SQL 只有一种写法；改 `timestamptz` 会出现「两种时间类型并存」，长期成本更高；
2. **避免制造前端 bug**：若走方案 A（`timestamptz` + `OffsetDateTime`），JSON 序列化会变成带 `+08:00` 后缀，可能打破前端日期解析 —— **为满足标准而制造前端缺陷，不值得**；
3. **当前部署下不可触发**：单时区（Asia/Shanghai，无夏令时），方案 B 的静默偏移风险在现网不可触发。

**迁移前提（触发器）**：一旦发生以下任一情况，必须**全库**迁移到 `timestamptz` 并同步实体类型 + 前端序列化改造：
```text
· 多时区部署（跨区门店/海外）
· 引入夏令时地区
· 与外部系统做带时区的时间对齐（如跨境支付对账）
```
**记录编号**：此为「偏离 Owner 新表标准」的书面记录，Lead 已报 Owner；若 Owner 坚持 `timestamptz`，按方案 A 整体改造（含前端序列化）。

## P1-1　`orders` 表内部混用「元/分」两种金额单位（**最高风险**）

```text
numeric(元)：order_amount / discount_amount / actual_amount / refund_amount
bigint(分) ：total_amount / final_amount / paid_amount / coupon_amount / points_amount
```
同表两套单位、都是「金额」语义 → **任何一处漏换算都静默算错且不报错**。
`orders` 是全系统金额源头（今日销售、缴款单净额、报表汇总都从它来）。

### 最危险的 5 条跨表混用点（按「静默错 100 倍」的可能性排序）

| # | 位置 | 混用 | 后果 | 是否报错 |
|---|------|------|------|----------|
| 1 | 缴款单净额（`DepositSlipServiceImpl`：`expected/actual/refund_component` 均为**分**） | 若从 `orders.order_amount`(元) 或 `pos_shifts.total_amount`(元) 直接取值 | **净额偏小 100 倍**，与现金盘点差异一起放大 | **不报错** |
| 2 | `pos_shifts.opening_cash/closing_cash`（**元**）与 `deposit_slips.reserve_fen`（**分**）相接 | 备用金/交接现金 | 备用金差 100 倍，天天空账 | **不报错** |
| 3 | `daily_settlements.*_amount`（**分**）汇总自订单 | 若取 `orders.actual_amount`(元) | 日结金额整体偏小 100 倍 | **不报错** |
| 4 | 仪表盘今日销售 `SUM(orders.total_amount)`（**分**） | 若有人换成 `order_amount`(元) 求 sum | 今日销售额 1/100 | **不报错** |
| 5 | POS 下单写库：请求 `price` 是**元**，需同时写 `order_amount`(元) 与 `total_amount`(分) | 只写一列或错单位 | 同一单两列不一致，对账才发现 | **不报错** |

### 处置：先加 `*_fen` 双写，**不贸然改 numeric 列**（Lead 已批准）

**但双写必须带校验，否则会变成「两个都错」**：

```sql
-- 定时比对（建议每小时一次，异常即告警/落表）
SELECT order_id, order_amount, actual_amount, total_amount, final_amount
  FROM orders
 WHERE deleted = 0
   AND total_amount IS DISTINCT FROM (order_amount * 100)::bigint   -- 分 vs 元×100
 LIMIT 100;
```

校验规则（三条，缺一不可）：
1. **等值校验**：`total_fen`(分) 必须等于 `order_amount`(元)×100（整数）；
2. **不允许多 100 倍以外的容差**：出现 1 分以内的四舍五入差异要显式记录（不是静默通过）；
3. **双写开关可回滚**：任一时刻关闭双写后，系统仍需能跑（即读路径不能依赖新列，直到切读完成）。

### P1-1 实施进度（2026-10-28）

| 项 | 状态 | 说明 |
|----|------|------|
| `*_fen` 列已建 | ✅ 已建（E 批 2/3） | `receivable`(3 列) / `requisition_requests`(2) / `travel_requests`(4) / `self_purchase`(1) / `self_purchase_item`(2) |
| **双写实现** | ❌ **尚未实现** | 当前 `*_fen` 是**空影子列**：没有任何 Service/Mapper 写它，读路径也全部读旧 `numeric(元)` 列 |
| 双写所在层（计划） | Service 层 + 同一事务 | 不放 Mapper：Mapper 层拿不到「元值来源」也做不了事务边界；**必须与前一行 insert/update 同一事务**，任一失败整体回滚 |
| 读路径切换 | 未开始 | 切换前需：① 双写稳定运行 ≥1 个结算周期；② 校验 SQL 差异为 0；③ 再切读 |

**风险提示（必须写清楚，避免「以为已经双写」）**：
```text
现在是「加了列但没写」——比「没加列」更容易误导：
  · 看 schema 会以为已经有分单位数据；实际全为 NULL；
  · 若有人直接 SELECT *_fen 做对账，会得到全 NULL → 静默无数据。
任何人要在 *_fen 上做统计前，先确认双写已上线（见上表「双写实现」）。
```

## 已知遗留（登记，不修）

| 项 | 说明 |
|----|------|
| `orders_legacy` / `inventory_legacy` / `order_items_legacy` 的 numeric 金额列 | legacy 空表，不修（见 `dead-code-deletion-list.md`「实体指向空表」） |
| 实体与表列不匹配 142 条（59 表不存在 + 83 空表列不匹配） | 见 `entity-column-audit-2026-10-28.md`，冻结待立项 |

---

# 补记（2026-10-28 ts-ops）：体检实测的索引债 + 金额单位边界清单

## D. 索引缺失（`table_health_check.py` 实测）

```
$ python scripts/qa/table_health_check.py --tables deposit_slips,fund_flows,pos_shifts,orders
deposit_slips 4/5 ✗  外键列无索引：cashier_employee_id,collector_user_id,cash_account_id,bank_account_id,deposit_bank_txn_id,create_user_id
fund_flows    4/5 ✗  外键列无索引：voucher_id,create_user_id
pos_shifts    4/5 ✗  金额列非 bigint：total_amount(numeric)
orders        3/5 ✗  外键列无索引：user_id,transaction_id,merchant_id,deliveryman_id,cashier_user_id,create_user_id
                     金额列非 bigint：order_amount/discount_amount/actual_amount/refund_amount(numeric)
结果：0/4 通过 → EXIT=2（**这是老账，故只登记、不进 gate**）
```

| 编号 | 对象 | 影响 | 规模 | 建议时机 |
|---|---|---|---|---|
| D-1 | `orders.user_id / transaction_id / merchant_id / deliveryman_id / cashier_user_id / create_user_id` | 我的订单、骑手配送、收银对账等**高频查询全表扫描**；十万级后是性能悬崖 | 6 列，核心表行数增长最快 | 下次改订单查询/报表前补 `CREATE INDEX CONCURRENTLY` |
| D-2 | `fund_flows.voucher_id / create_user_id` | 按凭证/操作人反查流水扫表；`fund_flows` 是**增长最快的表** | 2 列，表最大 | **优先级实际高于 D-3** |
| D-3 | `deposit_slips.cashier_employee_id / collector_user_id / cash_account_id / bank_account_id / deposit_bank_txn_id / create_user_id` | 缴款单按收银员/账户反查扫表（资金核对最常用 `cash_account_id`） | 6 列，体量中等 | 与 D-1 同批 |
| D-4 | `pos_shifts.total_amount` 是 `numeric`（元） | 班次汇总（元）与缴款单应交额（分）不同单位，每次比对都要换算 | 1 列 + 汇总计算 | **与 P1-1 同批处理，不许单独动**（否则出现第三种口径） |

**为什么单独立账**：把老账塞进 gate → gate 从第一天就红 → 被人关掉 → 再也拦不住新问题。
所以：新表由 `table_health_check.py`（`preflight --full` 自动跑 `new-tables.txt`）阻断；老账只在这里按时机清偿。

## M. 金额单位边界清单（哪些是分、哪些是元、换算在哪）

### M.1 权威结论

| 单位 | 存法 | 典型对象 |
|---|---|---|
| **分**（新标准） | `bigint`（多带 `_fen` 或处资金域） | `fund_flows.amount`、`deposit_slips.expected_amount/actual_amount/refund_component_fen/difference_net_fen/reserve_fen`、`bank_accounts.balance`、`recon_*`、`finance_records.amount`、`daily_settlements.*_amount` |
| **元**（历史遗留） | `numeric` | `orders.order_amount/discount_amount/actual_amount/refund_amount`、`pos_shifts.total_amount`、`budgets/budget.*`、`electronic_*`、`finance_invoice.*`、`food.cost_price`、`dish_combo.price` |
| **元**（外部输入） | `numeric` / DTO `BigDecimal` | `pos_shifts.opening_cash`（开班备用金）—— **2026-10-28 已因此出过一次问题** |

规模（实测）：`bigint` 金额列 **176** 个；`numeric` 金额列 **30+** 个。

### M.2 唯一换算实现与全部调用点

```java
// backend/.../service/finance/recon/importing/MoneyYuanToFen.java
MoneyYuanToFen.Result r = MoneyYuanToFen.toFen("123.45");  // → 12345 分（千分位/负号/精度校验都在这）
```
**禁止裸 `× 100`。** 全仓 11 处引用（含注释），真正的换算点：

| 换算点 | 位置 | 方向 |
|---|---|---|
| 开班备用金 → `reserve_fen` | `DepositSlipServiceImpl.java:239-246` | 元 → 分 |
| 银行流水 CSV `amount` | `CsvImportParser.java:287` | 元 → 分 |
| 银行流水 CSV `fee` | `CsvImportParser.java:293` | 元 → 分 |
| 银行流水导入入口 | `BankImportServiceImpl.java:46`（复用 `CsvImportParser`） | 元 → 分 |
| 对账单创建 | `StatementCreateRequest.java:9`（契约：金额**一律分**，调用方自行换算） | 由调用方换算 |

### M.3 四个必须确认的边界风险点

| 风险点 | 为什么危险 | 怎么确认 |
|---|---|---|
| `pos_shifts.opening_cash`（元）→ `deposit_slips.reserve_fen`（分） | 已出过事故 | `AC-58-29` 断言 `reserve_fen == opening_cash × 100` |
| `orders.actual_amount`（元） vs `fund_flows.amount`（分） | 直接比较差 100 倍 | 任何「订单 vs 流水」断言必须显式换算 |
| `pos_shifts.total_amount`（元） vs 缴款单 `expected_amount`（分） | 班次汇总与应交额不同单位 | 对账 SQL 全部在分域计算（A39 即如此） |
| 前端 `data-*-fen` 钩子 | 钩子名带 `fen`，后端某些接口返回元 | 页面实测比对（`e2e_store_recon.py` AC-49d 就是这么抓出假红的） |
| 12 个 `*_fen` 影子列 | 建了列但双写未实现，恒 NULL | `python scripts/qa/fen_shadow_check.py`（期望全 NULL；非空 → 立刻确认双写已上线） |

