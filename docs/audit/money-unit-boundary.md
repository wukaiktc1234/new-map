# 金额单位边界清单（元 numeric ↔ 分 bigint）

> 生成日期：2026-10-28 ｜ 依据：`information_schema.columns` 实测 + 代码 grep
> 背景：`pos_shifts.opening_cash` 曾因「元/分」混用出过事故；`orders` 是全系统金额源头。

## 0. 一句话结论（风险最高的一条）

> **`orders` 表内部就混用两种单位**：`order_amount / discount_amount / actual_amount / refund_amount` 是 **numeric(元)**，
> 而 `total_amount / final_amount / paid_amount / coupon_amount / points_amount` 是 **bigint(分)**。
> 同表两套单位、且都是「金额」语义 —— 任何一处漏换算都会**静默算错**，且不会报错。

## 1. 元（numeric）金额列

| 表 | 列 |
|----|-----|
| `orders` | `order_amount`, `discount_amount`, `actual_amount`, `refund_amount` |
| `pos_shifts` | `opening_cash`, `closing_cash`, `total_amount` |
| `budget` / `budgets` | `budget_amount`, `used_amount`, `remaining_amount` |
| `cost_record` / `cost_records` | `amount`, `unit_price` |
| `food` / `product` | `cost_price`, `food_price`/`price` |
| `dish_combo` | `price` |
| `coupon` / `promotion` | `min_order_amount`, `min_amount` |
| `electronic_*`（发票/票据族） | `tax_amount`, `total_amount`, `amount_without_tax` 等 |
| `finance_invoice` / `invoice` | `amount`, `tax_amount`, `total_amount` |
| `purchase_*` | `contract_amount`, `total_amount`, `unit_price`, `total_price` |
| `reimbursement_requests` / `health_certificate_expense` / `tax_record` | `total_amount` / `amount` / `paid_amount` 等 |
| `orders_legacy` / `inventory_legacy` / `order_items_legacy` | 同上（**legacy，不修**） |

## 2. 分（bigint）金额列（新标准）

| 表 | 列 |
|----|-----|
| `deposit_slips` | `expected_amount`, `actual_amount`, `difference_amount`, `cash_counted_fen`, `difference_net_fen`, `refund_component_fen`, `reserve_fen` |
| `fund_flows` | `amount`, `balance_after` |
| `daily_settlements` | `refund_amount`, `difference_amount`, `total_discount_amount`, `cancelled_amount` |
| `finance_period_account_balance` | `closing_fen`, `expense_fen`, … |
| `orders` | `total_amount`, `final_amount`, `paid_amount`, `coupon_amount`, `points_amount` |
| `dish_combos`、`finance_invoices`、`bank_transactions`、`asset_flow_records` | 各自金额列 |

## 3. 换算发生在哪里（唯一实现）

```text
com.foodtraceability.service.finance.recon.importing.MoneyYuanToFen   ← 唯一实现（类注释已写：禁止自行 *100 / /100）
  39: toFen(BigDecimal yuan)     元 → 分（小数超 2 位抛异常，不静默四舍五入）
  另有 fenToYuan / 字符串解析（含千分位、全角数字拒绝）
调用点：58 处（DecisionBoardServiceImpl 7 ｜ SalesAnalysisServiceImpl 7 ｜ LiveMonitorServiceImpl 6 ｜
        FoodServiceImpl 6 ｜ PosOrderCreateServiceImpl 6 ｜ OperationsDashboardDataServiceImpl 5 …）
```

## 4. 跨表拼接/汇总的混用点（重点）

| 位置 | 混用情况 | 后果 |
|------|----------|------|
| `DashboardServiceImpl:157-168`（今日销售额） | `SUM(orders.total_amount)` —— **分**，前端按 `formatCurrency(fen)` 展示 | 单位一致 ✔ |
| `POS 下单`（`PosOrderCreateServiceImpl`） | 请求里 `price` 是**元**，落库 `order_amount`(元) + `total_amount`(分) 两列都要写 | 漏写或错单位 → 一列对一列错，对账时才发现 |
| `缴款单净额`（`DepositSlipServiceImpl`） | `expected/actual` 用**分**；若从 `orders.order_amount`(元) 或 `pos_shifts.total_amount`(元) 直接取 → **静默 100 倍误差** | 净额、差异全部错，且不报错 |
| `daily_settlements`（报表） | 分；若汇总自 `orders.order_amount`(元) 需 toFen | 同上 |
| `pos_shifts.opening_cash / closing_cash` | **元**；与 `deposit_slips.reserve_fen`(分) 相接处必须换算 | 备用金/现金盘点差 100 倍 |

**已发生过的事故**：`pos_shifts.opening_cash` 单位混淆（元被当分用），见当天 lesson。

## 5. 新表标准（2026-10-28 起）

```text
金额一律 bigint 存「分」，列名后缀 _fen（如 total_fen / amount_fen）；
禁止在新表使用 numeric 存金额；
元↔分换算只允许调用 MoneyYuanToFen；
跨单位取值必须在注释里写明「此处从元转分」/「此处已是分」。
```

## 6. 建议（待排期）

1. `orders` 的 4 个 numeric 列**不要贸然改成 bigint**（历史账 + 全链路读写 115 处），先加一列 `*_fen` 并双写，等对账一致再切；
2. 所有「从 orders/pos_shifts 取金额进缴款单/报表」的位置，**逐处核对是否调用了 toFen**；
3. 给 `MoneyYuanToFen` 加一条单测：元值 0.01 / 0.1 / 1234.56 → 分 1 / 10 / 123456（防回归）。
