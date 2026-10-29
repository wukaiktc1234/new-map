# 交班面板：后端字段 → 前端显示对照（给 pos-ux，2026-10-29）

> 起因：前端实测显示「本班次收款 = **-174.00**」与「异常单 **38 笔** vs pendingExceptionCount=**0**」。
> 两者**都不是后端字段算错**，而是前端把「净额」当「收入」、把「订单明细异常数」当「待处理异常数」。

## 一、字段对照表（`GET /v1/pos/shift/stats`）

| 后端字段 | 单位 | 语义 | 前端应如何显示（禁止做什么） |
|----------|------|------|------------------------------|
| `cashIn` | 元 | **现金收入（只加不减）**，来自 `payment_breakdown.cash` | 直接显示。**禁止**在这里减退款 |
| `cashRefund` | 元 | **现金退款**（`payment_method=1` 的退款合计） | 单独显示为「退款已扣」。**禁止**并入 cashIn |
| `handoverCash` | 元 | `cashIn − cashRefund`（**应上交现金/净现金**） | 显示为「净现金」或「应上交」。**禁止**再减一次退款 |
| `drawerExpected` | 元 | `openingCash + handoverCash`（**钱箱应有**，= 交班落库的 `closing_cash`） | 显示为「钱箱应有」。**禁止**自行加备用金（已含） |
| `openingCash` | 元 | 开班备用金（只读） | 显示为「备用金」，「修改」按钮走 `POST /opening-cash`（当前 501） |
| `netRevenue` | 元 | **净营业额**（营收 − 退款，含非现金） | 显示为「净营业额」。**禁止**与 `handoverCash` 混用（前者全渠道，后者只现金） |
| `pendingExceptionCount` | 笔 | **待处理异常单数**（本班次 + 未支付 + 无 exception_ops） | **交班按钮的唯一开关**，徽标只认这个字段 |
| `exceptionOrderTotal` | 笔 | **历史全部异常单数**（含已处理、含 TTL 已过期） | 仅作参考展示。**禁止**用它控制交班按钮 |

### 其它接口的金额字段（同样按「单位」区分，别混）

| 接口 / 字段 | 单位 | 说明与前端约束 |
|-------------|------|----------------|
| `GET /v1/pos/shift/exceptions` → `amount` | **分** | 异常单金额（2200 = ¥22.00）。展示前必须 `fenToYuan(amount)`；回写 `POST /v1/pos/exceptions/{id}/resolve` 时 `amountFen/cashReceivedFen` 保持**分**。 |
| `POST .../resolve` 请求体 | **分** | `amountFen` / `cashReceivedFen`。禁止把「元」直接发出去（2200 分错发成 220000 分 → 后端 422「金额不符」）。 |
| `POST .../resolve` / `.../void` 幂等键 | — | `idempotencyKey` **必须放在请求体**（后端 `body.get("idempotencyKey")`），Header 同时带上亦可。 |

**回归测试**：`frontend-pos/tests/money-units.test.mjs`（`npm run test:units`）锁死 0 / 0.01 / 0.1 / 22 / 1234.56 / 999999.99 / 负数 / null / NaN 的元⇄分换算，
并含两条反例断言：`fenToYuan(2200) === 22`、`yuanToFen(22) === 2200`。

## 二、三条硬规则（请前端遵守）

```text
1. 前端禁止自行相减求净额 —— 凡「净」的概念后端已给字段（handoverCash / netRevenue）。
   反例（当前 bug）：ShiftHandover.vue:721-730 paymentRows 用 paid − refunded 求「本班次收款」，
                     退款大于收款时显示 -174.00。
2. 前端禁止用「订单明细里 statusKey==='anomaly' 的条数」冒充 pendingExceptionCount。
   反例（当前 bug）：ShiftHandover.vue:460-468 兜底用 anomalyOrders.length → 显示 38 笔，
                     而后端 pendingExceptionCount=0（两个口径：明细异常 vs 待处理异常单）。
3. 交班按钮只看 pendingExceptionCount；为 0 才可交班（后端另有 409 兜底）。
```

## 三、后端如何保证 stats 与交班同源

`getShiftStatsForPanel()` 内部调用 **endShift 用的同一个** `countShiftOrders()`，钱箱公式也是同一段代码；
实测六个数字逐一相等（netRevenue / cashIn / cashRefund / openingCash / drawerExpected / pendingExceptionCount）。

## 四、⚠ 同一响应里「两种单位并存」（2026-10-29 实测确认，务必让前端知道）

### 4.1 `GET /v1/pos/shift/stats`（全部金额 = **元**）

```text
cashIn / cashRefund / handoverCash / drawerExpected / openingCash / netRevenue  → **元**（BigDecimal，2 位小数）
理由：stats 复用的是 endShift 的 countShiftOrders 结果，PO 展示口径一直是元。
实测：{"cashIn":22.00,"cashRefund":0.00,"handoverCash":22.00,"drawerExpected":122.00,"netRevenue":22.00}
```

### 4.2 `GET /v1/pos/shift/exceptions`（`amount` = **分**）

```text
amount 取自 SQL：COALESCE(o.final_amount, o.total_amount, 0)
  pos_auth_impl SQL 位置：PosAuthServiceImpl 第 1011 / 1015 行（pending / processed 两个分支同源）
orders.final_amount / total_amount 都是 **bigint 分**（见 Key-Tables-Cheat-Sheet 的 orders 54 列清单）

实测（夹具 O1793282946516，单价 22.00 元）：
  DB:  final_amount=2200  total_amount=2200  actual_amount=0.00  order_amount=0.00
  API: {"orderId":"O1793282946516","amount":**2200**,...}   ← 2200 分 = 22.00 元
```

### 4.3 前端必须遵守

```text
1. stats 的金额字段 → 直接显示（已经是元），**不要再 /100**；
2. exceptions 的 amount → 显示前 **/100**（分转元）；回写补收款时**保持分**
   （POST resolve 的 amountFen / cashReceivedFen 都是**分**）；
3. 反例（本轮前端实测 bug）：把 amount=2200 当元显示成 ¥2200.00，补收款发 220000 分 → 后端 422「金额不符」。
```

**为什么会出现两种单位**：`orders` 表历史遗留 —— `order_amount/actual_amount/discount_amount/refund_amount` 是 numeric（元），
而 `total_amount/final_amount/paid_amount/coupon_amount/points_amount/delivery_fee/packaging_fee` 是 bigint（分）。
新代码统一用**分**，但面向收银员的统计口径一直是**元**，所以 stats 保持元、明细保持分。
