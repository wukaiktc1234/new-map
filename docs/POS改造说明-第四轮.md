# 第四轮改造说明（交班财务口径 + 全局质感）

> 范围：`frontend-pos` 展示层与交互层。本轮**没有改后端**（上一轮已落库的现金字段沿用）。
> 验证：`npm run build` 通过；三套 E2E 共 **59 项断言全绿**（38 新套件 + 20 回归 + 1 支付回归）。

---

## 一、交班日结：财务口径重塑

### 1.1 旧口径错在哪

旧版把「备用金」算进「应交现金总额」：

```
应交现金 = 备用金 + 现金收入 − 现金退款      ← 错
```

备用金是开班时放进钱箱的零钱，**交班时原封不动留在钱箱**，不属于要上交的钱。
把两者相加会让收银员多交一笔自己没拿过的钱（门店最常见的"短款"来源）。

### 1.2 新口径

| 指标 | 公式 | 用途 | 视觉层级 |
|---|---|---|---|
| **应上交现金** | 现金收入 − 现金退款 | 收银员唯一要交出去的钱 | 主角：54px 数字 + 暖橙渐变卡 |
| **钱箱理论应有** | 备用金 + 现金收入 − 现金退款 | 收银员自己数钱核对 | 小字（12.5px，虚线分隔） |
| 净营业额 | Σ已支付 − Σ已退款 | 给店长看 | 次级指标卡 |
| 非现金收入 | 微信 / 支付宝 / 银行卡（+会员余额） | **已直连入账，无需上交** | 独立三卡模块 + 绿色徽标 |

测试数据下的结果（备用金 500 / 现金收入 170 / 现金退款 20 / 微信 30）：

```
应上交现金   ¥150.00        （170 − 20）
钱箱理论应有 ¥650.00        （500 + 170 − 20）
微信 ¥30.00 · 支付宝 ¥0.00 · 银行卡 ¥0.00   → 已直连入账，无需上交
```

### 1.3 数据清洗（沿用上一轮，仍是唯一口径）

- 计入：`已确认/已完成/完成`（有支付流水且金额 > 0）、`全额退款/已退`（作负项）；
- 剔除并进异常抽屉：`待付`、`已取消`、`退款中`、**未记录支付流水**、**金额 ≤ 0**；
- 后端 `/shift/summary` 的脏汇总（测试里 mock 成 99 笔 / ¥9999）**不参与展示**，只用于班次状态与开始时间。

---

## 二、全局 UI 质感升级

### 2.1 Emoji 全部替换为 SVG（Element Plus Icons）

| 位置 | 旧 | 新 |
|---|---|---|
| 交班页头部 | 📋 | `Files` |
| 明细入口 | 📋 | `Tickets` |
| 异常红条 | ⚠ | `WarningFilled` |
| 导出 / 打印 | 📥 / 🖨️ | `Download` / `Printer` |
| 钱箱核对 | 无 | `Coin` |
| 撤销条 | 🗑️ | `Delete` |
| 库存查询 | ⚠️ / 🎉 | `WarningFilled` / `CircleCheckFilled` |
| 支付弹窗扫码区 | 📱 | `Iphone` |
| 桌台空态 / 叫号空态 | 🪑 / 🔔 / ✨ | `Grid` / `BellFilled` / `Star` |
| 登录页特性 | ⚡ / 🔗 / 📊 | `Lightning` / `Link` / `DataLine` |
| 后厨批量操作 | 🍳 / ✅ / 🔄 | `Dish` / `Select` / `Refresh` |
| 后厨快捷键面板 | ⌨️ / 💡 | `Operation` / `InfoFilled` |
| 顾客端状态文案 | ⏳📋👨‍🍳✅🍽️❌ | 纯文本（"制作中"/"已完成"…） |

E2E 断言：交班弹窗与深色抽屉的 innerText 用 Unicode 区间扫描 **0 命中**，同时 `.el-icon svg` 计数 ≥ 6（实测 13/15 个）。

### 2.2 深色模式重做（去"黑客后台"感）

| 项目 | 旧 | 新 |
|---|---|---|
| 主背景 | `#1c1917`（近纯黑，暖偏红） | **`#1E1E20`** 中性深灰 |
| 卡片 / 抬升层 | `#292524` / `#3f3b37` | `#26262A` / `#2E2E33` |
| 文字 | 纯白 `#f5f5f4` | 柔和灰白 `#ECECEE`（次要 `#B4B4BB`，弱化 `#8A8A92`） |
| 边框 | 白描边（表格白框） | 极低对比度 `#35353B` / `#2A2A2F`，**分区靠阴影** |
| 抽屉 / 弹窗 / 消息框 | 部分仍为 Element 默认白底 | 统一 `--pos-bg-secondary`，标题/正文/输入框全部跟随 |
| 主角卡 | 米白渐变（深色下刺眼） | 深棕渐变 `#2A2118 → #33251A`，橙色数字 `#FB923C` |

同时修掉一个**写法级事故**：scoped 样式里 `:global([data-theme='dark']) .sh-hero` 被 Vue 编译器把后半段选择器丢掉，
编译产物退化成了 `[data-theme=dark]{...}`（等于给 html 加样式）——改成普通后代选择器 `[data-theme='dark'] .sh-hero` 后正常生成
`[data-theme=dark] .sh-hero[data-v-xxx]`。E2E 增加了背景色断言兜底（A14）。

### 2.3 排版与组件质感

- **数字字体**：新增 `--pos-font-number`（DIN Alternate / DINPro / Bahnschrift / Roboto 回退链）+ `font-variant-numeric: tabular-nums`，金额不再因字宽跳动；
- **主数字放大**：应上交现金 54px/800，非现金卡 22px，指标卡 20px；
- **弱化边框**：卡片统一"无边框 + box-shadow 分层"，仅保留极轻的 1px 分隔线；
- **品牌色统一**：覆盖 Element Plus 主色令牌（`--el-color-primary: #ea580c`），按钮/开关/单选/链接不再出现默认蓝；弹窗与 MessageBox 按钮统一 8px 圆角；
- **信息密度**：交班上移到 `margin-top: 4vh`、卡片内边距收紧，实测「确认交班」按钮 `y=763 + h=42`，**无需滚动即可见**（新增断言 A13）。

---

## 三、验证矩阵

| 套件 | 项数 | 结果 |
|---|---|---|
| `test-review/pos-round2-verify.cjs` | 38 | **全绿**（含 A13 不滚动可见、A14 深色主色卡、A2h/A11 无 Emoji、A2e/A2f 非现金卡、A10 深色非白底） |
| `test-review/pos-ui-verify.cjs`（一轮回归） | 20 | **全绿** |
| `test-review/pos-payment-double-enter.cjs`（支付回归） | 1 组 | **PASS**：单次回车 → 1 确认框 / createOrder 1 / payOrder 1 |

关键断言原文：

```
A1  应上交现金 = 170-20 = ¥150.00（不含备用金）      → ¥150.00
A2d 钱箱理论应有 = 500+170-20 = ¥650.00             → ¥650.00
A2e 微信独立卡片 ¥30.00 且标注已直连入账             → 微信 ¥30.00 已直连入账
A2g 非现金模块标注「无需上交」                       → 已直连入账，无需上交
A2h 交班弹窗无 Emoji（已换 SVG）                     → clean（svg=13）
A13 确认交班按钮不滚动即可见                         → {"y":763,"h":42}
A14 深色下主角卡片为深棕                             → linear-gradient(150deg, rgb(42,33,24), …)
A10 深色下抽屉为深灰而非纯白                         → rgb(38,38,42) / body rgb(30,30,32)
C1/C2 撤销条、D3/D4 取单护栏、E1–E3 支付字段          → 全部通过
```

截图：`round2-shift-handover.png`（浅色主界面）、`round3-shift-handover-dark.png`（深色主界面）、
`round3-shift-detail-dark.png`（深色明细抽屉）、`round2-shift-detail-drawer.png`（浅色抽屉）。

---

## 四、变更文件

| 文件 | 改动 |
|---|---|
| `views/ShiftHandover.vue` | 财务口径重塑（应上交现金/钱箱核对/非现金三卡）、SVG 图标、阴影分层排版、深色适配 |
| `styles/pos/variables.css` | 深色令牌重做（#1E1E20 系）+ `--pos-font-number` |
| `styles/pos/base.css` | Element Plus 主色对齐品牌橙、深色抽屉/弹窗/消息框统一、圆角 8px |
| `styles/pos/components/dialog.css` | 交班弹窗上移/高度放宽；头部隐藏与按钮规范沿用 |
| `components/pos/CartUndoBar.vue` | 🗑️ → `Delete` 图标 |
| `components/pos/dialogs/StockQueryDialog.vue` | ⚠️/🎉 → `WarningFilled`/`CircleCheckFilled` |
| `components/pos/dialogs/PaymentConfirmDialog.vue` | 📱 → `Iphone` |
| `views/TableManagement.vue` / `views/CallNumber.vue` | 空态 Emoji → `Grid`/`BellFilled`/`Star` |
| `views/Login.vue` | 特性 Emoji → `Lightning`/`Link`/`DataLine` |
| `views/CustomerOrder.vue` | 状态文案去 Emoji |
| `components/pos/CustomerOrderDisplay.vue` | 促销位 Emoji → `Present`/`Iphone`/`Star` |
| `components/kitchen/BatchOperationPanel.vue` / `ShortcutHelp.vue` | 后厨端 Emoji → `Dish`/`Select`/`Refresh`/`Operation`/`InfoFilled` |

## 五、遗留

| 项 | 说明 |
|---|---|
| 唯一未闭环 P0 | **R-3 券折扣不随改单重算**（属业务规则，需产品先定义折扣/抹零规则；本轮未动） |
| 真机校验 | 深色主题与 DIN 字体在 1366×768 收银一体机上的实际观感需门店复核（字体回退到 Bahnschrift/Roboto） |
| 打印小票 | 仍为浏览器打印窗口的等宽排版（未接热敏打印 SDK），文案已同步新口径 |
