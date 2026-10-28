# 第五轮修正说明（深色订单详情 / 交班口径再校正 / 视觉细节）

> 反馈来源：门店业务审阅（图一 订单详情深色；图二 交班页公式文案与 −75 归属、滚动条、明细行过小）。
> 验证：`npm run build` 通过；四套 E2E 共 **68 项断言全绿**（42 主套件 + 20 回归 + 5 负现金 + 1 支付回归）。

---

## 一、图一：订单详情深色仍然是"白表"

### 根因

`OrderDetailDialog` 用的是 `<el-descriptions border>` 与原生 `<table>`：

- `el-descriptions` 的单元格底色来自 Element Plus 令牌 `--el-fill-color-blank`（纯白），
  深色主题只改了 POS 自己的 `--pos-*` 变量，**没有覆盖 Element 的填充色**；
- `el-table`（订单查询列表、库存查询）同理，默认白底 + 白边框。

### 修复（全局，不再逐组件打补丁）

在 `styles/pos/base.css` 的深色块里补齐 Element Plus 表格/描述列表令牌：

```css
[data-theme='dark'] .el-descriptions__body,
[data-theme='dark'] .el-descriptions__cell,
[data-theme='dark'] .el-descriptions__content { background: var(--pos-bg-secondary); border-color: var(--pos-border-color); }
[data-theme='dark'] .el-descriptions__label { background: var(--pos-bg-tertiary); color: var(--pos-text-secondary); }
[data-theme='dark'] .el-table th.el-table__cell { background: var(--pos-bg-tertiary); border-color: var(--pos-border-color); }
[data-theme='dark'] .el-table td.el-table__cell { background: var(--pos-bg-secondary); border-color: var(--pos-border-color); }
```

### 顺带修掉一个生产级文字 Bug

截图里「支付状态」显示的是裸数字 `2` —— 因为两个对话框各写了一份**只认英文文本**的状态映射
（`pending/paid/...`），而后端 `order_status` 的规范码是数字（0待确认 1已确认 2已完成 3已取消 5全额退款）。

新增 `utils/orderStatus.ts` 作为唯一实现（数字码 + 文本同义词 + 未知码兜底为「未知状态（X）」），
`OrderQueryDialog` / `OrderDetailDialog` 统一调用。现在显示为「已完成」并带语义色。

---

## 二、交班页：去掉"AI 味"公式 + 负现金的正确归属

### 2.1 删除公式行

按要求移除整行：

```
钱箱理论应有 = 备用金(¥1000.00) + 现金收入(¥0.00) − 现金退款(¥75.00) = ¥925.00
```

改为一行中性提示：**「备用金 ¥1000.00 留存钱箱」**（不带运算、不做推导）。
备用金仍然可见（收银员要确认钱箱底钱），但不再像一份"系统自证"的报表。

### 2.2 −75 的归属：为什么不能让它吃掉备用金

原口径 `应上交 = 现金收入 − 现金退款` 在"退款大于收款"时会出现负数。
如果直接把它当上交额，等于让收银员**从钱箱（备用金）里掏出 75 元去上交**，
那么钱箱从 1000 变 925，下一班开盘就少了 75，**且这笔亏空会被一代代传下去**。

改为两条独立语义：

| 字段 | 公式 | 含义 |
|---|---|---|
| 应上交现金 | `max(0, 现金收入 − 现金退款)` | 手里有现金才上交；净流出时记 **¥0.00**，不出现负数 |
| 备用金待补足 | `max(0, 现金退款 − 现金收入)` | 本班次从钱箱垫出去的钱，**必须由店长补回**，否则下一班零钱不够 |

净流出时界面直接给出处置指引（截图 `round5-shift-negative.png`）：

> ⚠ 本班次现金净流出 **¥75.00**：退款大于收款，已从备用金垫付。
> 请店长补足备用金 **¥75.00**，否则下一班零钱不足。

这样「谁欠谁」清楚了：收银员不用倒贴，店长知道要补多少钱，下一班开盘钱箱是完整的 1000。
交班确认弹窗、导出 CSV、打印小票三处文案同步改成「备用金待补足」，不再出现"钱箱理论应有"。

### 2.3 右侧滚动条

对话框主体改为隐藏滚动条（`scrollbar-width: none` + `::-webkit-scrollbar{width:0}`），
滚轮与触屏仍可滚动；抽屉内的列表框保留 6px 细滚动条（内容长时需要位置感）。
注意：这条规则必须写在**全局** `dialog.css` 里 —— scoped `:deep()` 对 teleport 到 body 的
`el-dialog` 根节点不生效（上一轮 `el-dialog__header` 隐藏也是靠全局规则才生效的）。

### 2.4 明细抽屉的订单行

从 5 列小字网格（12.5px）改为**卡片式两行**：

```
┌──────────────────────────────────────────┐
│ 09:30  #028001                    [已支付] │   ← 14px 时间 / 12.5px 单号 / 徽章
│ 现金                              ¥100.00 │   ← 13px 支付方式 / 19px 金额
└──────────────────────────────────────────┘
```

行高 ≥ 64px、卡片间距 10px、悬停上浮 1px；异常单用琥珀底 + 内阴影区分。
断言：`.sh-order-item` 高 ≥ 64px 且金额字号 ≥ 18px（A8b）。

---

## 三、验证

| 套件 | 项数 | 结果 |
|---|---|---|
| `pos-round2-verify.cjs`（主） | 42 | **全绿**，新增 A2d（公式已移除）/ A2d2（留存钱箱）/ A8b（卡片行尺寸）/ A15（深色订单列表非白底）/ A16（深色描述表非白框） |
| `pos-ui-verify.cjs`（回归） | 20 | 全绿 |
| `pos-negative-cash.cjs`（新增） | 5 | 全绿：应上交 ¥0.00、净流出提示、垫付+补足文案、备用金不显示为被扣减、公式已移除 |
| `pos-payment-double-enter.cjs` | 1 组 | PASS |

关键断言原文：

```
A2d  已移除「钱箱理论应有」公式表述        → 通过
A2d2 备用金以「留存钱箱」提示呈现           → 备用金 ¥500.00 留存钱箱
A8b  明细行已放大为卡片（高≥64px、金额≥18px）→ h=78 font=19
A15  深色下订单列表表格不再是白底           → rgb(38, 38, 42)
A16  深色下订单详情描述表不再是白框         → rgb(38, 38, 42) / rgb(38, 38, 42)
N1   现金净额为负时「应上交现金」显示 ¥0.00  → ¥0.00
N4   备用金仍完整显示 ¥1000.00              → ¥1000.00
```

截图：`round4-order-detail-dark.png`、`round4-order-list-dark.png`、`round5-shift-negative.png`、
`round2-shift-handover.png`（浅色主界面·无公式）、`round3-shift-detail-dark.png`（深色卡片式明细）。

---

## 四、变更文件

| 文件 | 改动 |
|---|---|
| `styles/pos/base.css` | 深色下 el-descriptions / el-table 令牌覆盖 |
| `styles/pos/components/dialog.css` | 交班弹窗隐藏滚动条、抽屉细滚动条 |
| `views/ShiftHandover.vue` | 删除公式行、备用金留存提示、负现金垫付/补足提示、明细卡片化、滚动条隐藏 |
| `utils/orderStatus.ts`（新增） | 订单/后厨状态码统一映射（数字码 + 文本） |
| `components/pos/dialogs/OrderDetailDialog.vue` | 改用统一状态映射（修裸数字 "2"） |
| `components/pos/dialogs/OrderQueryDialog.vue` | 同上 |

## 五、遗留

- 净营业额在"退款大于销售"时仍显示负数（会计口径正确，退款另有一张卡单独展示），
  如果门店更希望看到"本期净额不计退款"的口径，需要产品确认后再改；
- 金额字体依赖 DIN/Bahnschrift 是否存在，缺失时回退 Roboto/系统字体（真机需复核）。
