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

## 已知遗留（登记，不修）

| 项 | 说明 |
|----|------|
| `orders_legacy` / `inventory_legacy` / `order_items_legacy` 的 numeric 金额列 | legacy 空表，不修（见 `dead-code-deletion-list.md`「实体指向空表」） |
| 实体与表列不匹配 142 条（59 表不存在 + 83 空表列不匹配） | 见 `entity-column-audit-2026-10-28.md`，冻结待立项 |
