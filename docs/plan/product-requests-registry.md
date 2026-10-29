# 产品需求登记册（只登记，不实现）

生成：2026-10-29 ｜ 状态词：`BLOCKED_PRODUCT_RULE`（等产品/财务定口径，后端不动手）

## P-01　钱箱实盘（counted_cash / over_short）

**状态**：`BLOCKED_PRODUCT_RULE`（未实现）

```text
现状：pos_shifts.closing_cash = opening_cash + 现金毛收入 − 现金退款（系统账面钱箱）
缺口：没有「实盘」概念 —— 收银员实际点出来多少钱、与账面差多少，系统不记。
```

**建议字段（未建）**：
| 字段 | 类型 | 含义 |
|------|------|------|
| `pos_shifts.counted_cash` | numeric(14,2) | 收银员实盘现金（元） |
| `pos_shifts.over_short` | numeric(14,2) | 长短款 = **实盘 − 账面**（正=长款，负=短款） |

**口径（必须与产品确认后再建）**：
```text
账面 = opening_cash + 现金毛收入(payment_method=1) − 现金退款(payment_method=1)
      ← 与当前 closing_cash 完全一致（2026-10-29 已定稿，见 tech-debt / handover_remark）
长短款 = counted_cash − 账面
要求：closing_cash 保持「系统账面」，counted_cash 存「实盘」，两者不合并；
      长短款落库而不是临时算，便于追责与日结核对。
```

## P-02　分级退款（限额内自动退 / 超限审批）

**状态**：`BLOCKED_PRODUCT_RULE`（未实现）

```text
现状：退款权限已收紧 —— ROLE_CASHIER 不含 order:refund / order:refund:initiate / refund:manage
      （V20261029_002），退款接口要求店长（@PreAuthorize 店长/管理员）。
缺口：没有「限额内收银员可自行退」的规则，所有退款都压到店长，高峰期堵。
```

**口径（待产品定阈值）**：
```text
退款金额 ≤ 阈值（建议 50 元）→ 收银员可直接退（需要给 ROLE_CASHIER 回补一个「限额退款」专用码，
                                  不是恢复 order:refund —— 恢复等于把刚修掉的越权开回来）
退款金额 > 阈值 → 走店长审批；审批人 ≠ 发起人（不得自批）
所有限额退款必须落审计（操作人、金额、订单号、时间）
```

**为什么不能简单恢复 `order:refund`**：那会让收银员退任意金额 —— 与 P-01 的钱箱短款风险叠加。
