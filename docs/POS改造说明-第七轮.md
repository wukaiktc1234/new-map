# 第七轮：品牌色回调 + 挂单弹窗长列表重构

> 反馈：① 上一轮把品牌橙压得太灰（#c9622a）看着发闷；② 挂单弹窗在菜品多时（图二）列表被切断、页脚被顶出屏幕。
> 验证：`npm run build` 通过；六套 E2E 共 **100 项断言全绿**。

---

## 一、品牌色：从"发灰"回到"稳而不闷"

| 版本 | 色值 | 问题 |
|---|---|---|
| 初版 | `#ea580c` | 饱和度过高，收银台大面积暖橙容易视觉疲劳 |
| 上一轮 | `#c9622a` | 压过头，按钮和金额显脏、显旧 |
| **本轮** | **`#e3651c`** | hsl(22, 78%, 50%)：保留食欲感，又不刺眼 |

同步调整：
- 浅色：`--pos-primary #e3651c` / `-dark #c25515` / `-light #ef7f3a`；软底 `#fdf1e7` / hover `#fbe2d1`
- 深色：`#ef7f3a` / `#e3651c` / `#f59a5f`；软底 `#3b2717`
- Element Plus 令牌（`--el-color-primary` 及 light-3/5/7/8/9、dark-2）同步，开关/单选/链接不再和按钮"两个橙"
- **全仓 23 个文件**里的旧硬编码（`#ea580c`、`#f97316`、`#c2410c` 及 `rgba(234,88,12,*)`、`rgba(249,115,22,*)`）统一替换为同一套值，消除"新旧橙并存"的花斑

---

## 二、挂单弹窗：订单多时的布局重构

### 2.1 旧版为什么在"多单"时崩

```
.el-dialog（高度随内容长）
├─ header
├─ body（padding:0）
│   ├─ input
│   └─ .hd-preview（固定 padding）
│        └─ .hd-preview-list（max-height: 200px，自己滚）
└─ footer（被内容顶出视口）
```

三个问题：**双层滚动**（弹窗主体 70vh + 列表 200px）、**列表被生硬切断**（没有渐隐提示）、**页脚被顶出屏幕**（弹窗总高不受控）。

### 2.2 新版结构

```
.el-dialog（max-height:86vh; margin-top:7vh; display:flex; flex-direction:column）
├─ header（不收缩）
├─ body（flex:1; min-height:0; overflow:hidden）   ← 不再滚动
│   └─ .hd-body（flex column）
│        ├─ 备注输入
│        └─ .hd-preview（flex:1; min-height:0）
│             ├─ 概览栏：当前订单 · "12 项 · 12 件"（常驻）
│             ├─ .hd-preview-list（唯一滚动区，6px 细滚动条）
│             ├─ .hd-list-mask（底部 24px 渐隐，提示"还有内容"）
│             └─ 合计行（常驻在列表下方，不随滚动消失）
└─ footer（flex:0 0 auto，按钮永远可见）
```

关键实现点（踩坑记录）：**结构性布局规则必须写在全局 `dialog.css`**——
`el-dialog` 默认 `append-to-body`，scoped `:deep()` 命中不到它内部（这一轮又一次验证：写在组件里的
`.hold-dialog :deep(.el-dialog__body){display:flex}` 完全没生效，移到全局后立刻正常）。

### 2.3 验证

用 12 个菜品模拟"订单很多"：

```
H1 弹窗高度不超出视口（封顶 86vh）   → y=63 h=774（63+774=837 < 900）
H2 确认挂单按钮在视口内可点         → y=760 h=44
H3 合计常驻可见                     → true
H4 弹窗主体隐藏溢出（只有一个滚动区）→ overflowY=hidden, display=flex
H5 清单高度被限制在弹窗内           → clientHeight=385，scrollHeight=496（内部滚动生效）
H6 列表底部渐隐遮罩存在             → true
H7 件数概览正确                     → "12 项 · 12 件"
H8 品牌色（浅 #e3651c / 深 #ef7f3a） → 命中
```

浅色/深色两套都跑了同一组断言（16 项）。

---

## 三、回归

| 套件 | 项数 | 结果 |
|---|---|---|
| `pos-visual-verify.cjs` | 16 | 全绿 |
| `pos-hold-dialog-verify.cjs`（新增） | 16 | 全绿 |
| `pos-round2-verify.cjs` | 42 | 全绿 |
| `pos-ui-verify.cjs` | 20 | 全绿 |
| `pos-negative-cash.cjs` | 5 | 全绿 |
| `pos-payment-double-enter.cjs` | 1 组 | PASS |

截图：`round7-hold-many-light.png`、`round7-hold-many-dark.png`、`round6-light-home.png`（新品牌色）。

## 四、变更文件

| 文件 | 改动 |
|---|---|
| `styles/pos/variables.css` | 品牌色回调到 `#e3651c`，软底/渐变同步 |
| `styles/pos/base.css` | Element Plus 主色令牌同步 |
| `styles/pos/components/dialog.css` | **挂单弹窗全局布局**（封顶/上移/flex 三段） |
| `components/pos/dialogs/HoldOrderDialog.vue` | 整窗重构：概览栏、唯一滚动区、渐隐、合计常驻 |
| 其余 20 个文件 | 旧硬编码品牌色统一替换为新令牌值 |

## 五、遗留

- 取单弹窗（卡片列表）结构类似但高度较小，多单时同样会滚动，未做同等重构；如需要我可以按同一套模式改造；
- 品牌色若还想更"暖"（偏黄）或更"红"，只需改 `--pos-primary` 一处与 `--el-color-primary` 一处。
