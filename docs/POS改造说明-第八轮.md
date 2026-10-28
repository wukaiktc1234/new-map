# 第八轮：取单同款重构 + 浅色层次 + 订单查询修复 + 实收不足校验

> 反馈：① 取单窗口按挂单模式改；② 浅色下各区块边界不清；③ 订单查询表格被截断 / 日期选择器点不动 / 查询按钮太深；④ 实收现金不足被静默补足仍满额结算；⑤ 分类胶囊要全圆角。
> 验证：`npm run build` 通过；七套 E2E 共 **119 项断言全绿**。

---

## 一、取单窗口（RetrieveOrderDialog）同款重构

与挂单弹窗共用一套"封顶 + 单滚动区"规则（全局 `dialog.css` 的 `.retrieve-dialog` 段）：

```
el-dialog  max-height:86vh / margin-top:7vh / flex column
├─ header   不收缩
├─ body     flex:1 / min-height:0 / overflow:hidden
│   ├─ 概览栏「共 12 张挂单 · 合计 ¥570.00」常驻
│   └─ .rd-list  唯一滚动区（6px 细滚动条 + 底部渐隐）
└─ footer   取消 / 清空全部 常驻可见
```

附带两处质感修正：**清空全部**从"高饱和红渐变大按钮"改成**描边危险色**（危险但低频，不该和主操作抢视觉）；卡片圆角/间距统一到 8 的倍数。
实测：弹窗 `y=63 h=774`（不超视口）、页脚 `y=756` 常驻、列表 `scrollHeight 1890 / clientHeight 496`（唯一滚动区）。

---

## 二、浅色模式层次（图一）

之前"白底 + 浅灰卡片"，边界几乎看不见。引入**画布层**：

| 层 | 浅色 | 深色 |
|---|---|---|
| 画布 `--pos-bg-canvas` | `#f2f3f5` | `#181819` |
| 卡片 | `#ffffff` + 1px 边框 + 阴影 | `#2c2c2e` + 阴影 |

- `.main-content` 铺画布色；
- 购物车面板：白底 + `1px var(--pos-border-light)` + `--pos-shadow-md`；
- 菜品卡：白底 + 1px 边框 + `--pos-shadow-sm`，hover 才染主色描边；
- 分类胶囊：`border-radius: 9999px`（两端全圆角）+ `:active` 轻微缩放。

实测：画布 `rgb(242,243,245)` vs 卡片 `rgb(255,255,255)`，卡片 `border-top-width: 1px`。

---

## 三、订单查询（图二）

| 问题 | 根因 | 修复 |
|---|---|---|
| 日期选择器点不动 | `<el-date-picker readonly>` —— 输入框被设成只读，日历面板永远不弹 | 去掉 `readonly` 与 `:teleported="false"`，输入框可点可输 |
| 表格被水平截断 | 8 列固定宽度合计 ≈ 890px + 弹窗 980px 内边距 + `fixed="right"` 强制出现横向滚动 | 弹窗加宽到 1120、列宽改 `min-width`、**去掉固定列**、订单号/支付方式加 `show-overflow-tooltip` |
| 查询按钮颜色太深 | 用了 `type="primary"` 且被旧主色影响 | 显式走 `var(--pos-primary)`（`rgb(227,101,28)`），hover 走 `primary-dark`，最小高度 40px |
| 表格纵向撑破弹窗 | 弹窗高度随内容 | `el-table max-height="440"`，表头使用 `--pos-bg-tertiary` |

实测：表格 `scrollWidth 1038 = clientWidth 1038`（无横向截断）；点击日期框 → `.el-picker-panel` 可见；查询按钮 `rgb(227,101,28)`。

---

## 四、实收现金不足（图三 / 图四）—— P0 修复

**根因**：`<el-input-number :min="totalAmount">`。用户输 65（应收 67），组件在 blur / Enter 时按 `min` 自动夹回 67，
于是"客人少付 2 元"被静默抹平，仍按满额结算。

**修复**：
1. `:min="0"`，不再自动夹取；
2. 新增 `insufficientCash = 现金支付 && 实收 < 应收`：
   - 不显示找零行，改为红色「实收不足 还差 ¥2.00」；
   - 「确认支付」按钮 **disabled**（并补了全局 `.is-disabled{opacity:.45}`，避免"看着能点"）；
3. `handleConfirm` 二次拦截：`ElMessage.warning('实收现金不足，还差 ¥X')` 后 return，回车也绕不过去；
4. 补足金额后按钮自动恢复可用。

实测：输入 65 → 失焦后仍为 `65.00`；`[data-test=cash-short]` 显示「还差 ¥2.00」；确认按钮 disabled；按 Enter 后弹窗仍开着且 `createOrder=0`；改为 70 后按钮恢复。

---

## 五、验证

| 套件 | 项数 | 结果 |
|---|---|---|
| `pos-round8-verify.cjs`（新增） | 15 | 全绿（层次/胶囊/取单/查询/实收不足） |
| `pos-visual-verify.cjs` | 16 | 全绿 |
| `pos-hold-dialog-verify.cjs` | 16 | 全绿 |
| `pos-round2-verify.cjs` | 42 | 全绿 |
| `pos-ui-verify.cjs` | 20 | 全绿 |
| `pos-negative-cash.cjs` | 5 | 全绿 |
| `pos-payment-double-enter.cjs` | 1 组 | PASS |

截图：`round6-light-home.png`（层次）、`round8-retrieve-many.png`（取单 12 张）、`round8-order-query.png`（订单查询）、`round8-cash-short.png`（实收不足）。

## 六、变更文件

| 文件 | 改动 |
|---|---|
| `styles/pos/variables.css` | 新增 `--pos-bg-canvas`（浅 `#f2f3f5` / 深 `#181819`） |
| `styles/pos/components/dialog.css` | 取单弹窗全局布局；禁用态按钮可见化 |
| `views/Home.vue` | 主内容区铺画布色 |
| `components/pos/CartSection.vue` | 白卡 + 边框 + 面板级阴影 |
| `components/pos/MenuSection.vue` | 菜品卡白底描边 + hover 主色；分类胶囊全圆角 |
| `components/pos/dialogs/RetrieveOrderDialog.vue` | 结构重构 + 概览栏 + 唯一滚动区 + 清空按钮降饱和 |
| `components/pos/dialogs/OrderQueryDialog.vue` | 日期可点、表格不截断、查询按钮品牌色、弹窗加宽 |
| `components/pos/dialogs/PaymentConfirmDialog.vue` | 实收不足不再自动补足，改为差额提示 + 禁用 + 二次拦截 |

## 七、遗留

- 订单查询的日期范围仍是"选完即查"还是"点查询才查"，取决于使用习惯，目前保持点按钮查询；
- 若希望「实收不足」也允许成交（例如抹零场景），需要门店确认规则再加"抹零"入口，目前按"不允许少收"处理。
