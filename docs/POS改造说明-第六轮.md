# 第六轮：全局视觉重构（深色补丁 / 缩放溢出 / 质感统一）

> 验证：`npm run build` 通过；五套 E2E 共 **84 项断言全绿**（16 视觉 + 42 主 + 20 回归 + 5 负现金 + 1 支付）。

---

## 一、两个致命 Bug

### 1.1 深色模式"白补丁"（购物车 +/− 与删除）

**根因**：`CartSection.vue` 里写死了浅色值：
```css
.decrease-btn { background-color: #fef2f2; }   /* 近白粉 */
.increase-btn { background-color: #fff7ed; }   /* 近白橙 */
.pc-delete-btn { padding: 4px; border-radius: 4px; }  /* 没有 border/background 重置 → 浏览器默认白底方块 */
```

**修复**：引入"软底"语义令牌，随主题切换；删除按钮重置原生外观为圆形幽灵按钮。

| 令牌 | 浅色 | 深色 |
|---|---|---|
| `--pos-primary-soft` | `#fbf1ea` | `#33261d` |
| `--pos-danger-soft` | `#fbf0f0` | `#332426` |
| 深色下 +/− 按钮 | — | 底 `#3a2c21`/`#3a2a2c`，图标 `#e8935f`/`#e08a8a` |

结果（实测计算样式）：`+` = `rgb(58,44,33)`、`−` = `rgb(58,42,44)`、删除按钮 = `rgba(0,0,0,0)` + `border: none`。

### 1.2 缩放 110% 溢出

**根因**：`zoom` 会把元素的**布局盒子**一起放大，`zoom:1.1 + height:100vh` 的实际视觉高度是 110vh → 页面出现双滚动条。
**且**：`width: calc(100% / 1.1)` 是错的——百分比在 zoom 坐标系内**已经折算过一次**，再除一次会变成双重缩小（实测 `offsetWidth` 只有 1190 而视口 1440，右侧留 130px 黑边）。

**正确写法**：
```css
html[data-pos-font-scale='large'] { --pos-zoom: 1.1; }

html[data-pos-font-scale='large'] .fastfood-pos {
  zoom: var(--pos-zoom);
  width: 100%;                               /* 百分比自动折算 */
  height: calc(100vh / var(--pos-zoom));     /* vh 不折算，必须手动除 */
}
```

同时去掉了布局里的硬宽度：购物车由 `width:420px; flex-shrink:0` 改为
`flex: 0 1 clamp(340px, 30%, 460px); min-width: 300px`；菜品网格由 `repeat(7, 1fr)` 改为
`repeat(auto-fill, minmax(104px, 1fr))`。

实测（1440×900）：`scrollWidth=1440`、`scrollHeight=900`，容器 rect 恰好 `1440×900`，网格从 8 列自适应到 7 列。

---

## 二、全局质感

| 维度 | 改前 | 改后 |
|---|---|---|
| **品牌色** | `#ea580c`（高饱和亮橙） | `#c9622a`（低饱和暖橙）+ Element Plus 主色令牌同步，开关/单选/按钮不再"荧光橙" |
| **深色底** | `#1c1917` → `#1e1e20` | **`#1E1E1F`**；卡片 **`#2C2C2E`**；抬升 `#353538`；文字 **`#E5E5E5`** |
| **圆角** | 4/6/8/10/12/16/20 混用 | 统一 8 / 12 / 16 三档（按钮 8、卡片与弹窗 12、大面板 16） |
| **间距** | 各处 4/6/10/12/14 随手写 | 8 的倍数令牌（8 / 16 / 24 / 32 / 40），弹窗内边距 24、底部 16 |
| **字体** | 金额用系统字体 | `--pos-font-number`（DIN Alternate → Bahnschrift → Roboto）+ `tabular-nums`；应收 30px、菜品价 15px、购物车行价 14px，拉开层级 |
| **按钮** | 顶栏 8 个按钮各配蓝/青/绿/紫/黄描边 | **中性描边 + 单一主行动色**：仅「叫号」填充品牌色，其余 hover 才染主色 |
| **装饰噪音** | 每张菜品卡都挂「库存99」绿色角标 | 仅在 `stock < 10` 时显示「仅剩 N / 售罄」，改为软底预警样式 |
| **热区** | 顶栏 40px | 44px（触控标准），图标按钮 44×44 |

**微交互**：`.action-btn` / `.qty-btn` / `.pc-delete-btn` / `.dish-item` / `.el-button` 统一
`transition` + `:active { transform: scale(0.97~0.98) }`，hover 背景加深（不再靠"跳一下位置"）。

**Loading / 防连点**：菜单骨架屏（既有）、结账 `paying`、挂单 `isSubmitting`、导出 `exporting`、交班 `endingShift`、
**新增**：取单面板 `v-loading`、券码「验证」按钮 `loading` 态、`.el-button.is-loading { pointer-events: none }`。

---

## 三、验证矩阵

| 套件 | 项数 | 结果 |
|---|---|---|
| `pos-visual-verify.cjs`（新增） | 16 | 全绿 |
| `pos-round2-verify.cjs` | 42 | 全绿 |
| `pos-ui-verify.cjs` | 20 | 全绿 |
| `pos-negative-cash.cjs` | 5 | 全绿 |
| `pos-payment-double-enter.cjs` | 1 组 | PASS |

关键断言：

```
V1  加减按钮为圆形                     → 9999px
V2  品牌橙已降饱和                     → #c9622a
V4  深色下「+」按钮不再是白底          → rgb(58, 44, 33)
V5  深色下「−」按钮不再是白底          → rgb(58, 42, 44)
V6  删除按钮无白底、无方框边框         → {bg: rgba(0,0,0,0), border: none}
V7  深色底色为 #1E1E1F                 → rgb(30,30,31)
V8  深色卡片为 #2C2C2E                 → rgb(44,44,46)
V9  110% 缩放无横向溢出                → scrollW 1440 = innerW 1440
V10 110% 缩放无纵向溢出                → scrollH 900 = innerH 900
V11 110% 下容器正好铺满视口            → 1440×900
V12 110% 下网格自动减列而非溢出        → 8 列 → 7 列
V13 购物车为弹性宽度（不写死 420px）   → 421（clamp 生效）
V14 偏好设置行间距 32px / 垂直留白 16px → 32px / 16px
V15 顶栏除「叫号」外统一中性色         → rgb(100,116,139) / rgb(201,98,42)
V16 库存角标默认隐藏                   → badges=0
```

截图：`round6-light-home.png`、`round6-dark-home.png`、`round6-zoom110-dark.png`、
`round6-light-preferences.png`、`round6-dark-preferences.png`。

---

## 四、变更文件

| 文件 | 改动 |
|---|---|
| `styles/pos/variables.css` | 低饱和品牌色、深色三级底与软底色、圆角/间距/缩放令牌 |
| `styles/pos/base.css` | **缩放不溢出修复**、Element 主色与圆角统一、全局按钮微交互 |
| `styles/pos/components/dialog.css` | 弹窗圆角 12、内边距 24/16 |
| `components/pos/CartSection.vue` | 加减/删除按钮深色适配与圆形化、弹性宽度、金额字体 |
| `components/pos/MenuSection.vue` | 自适应网格、字号上调、软底标签、卡片微交互 |
| `components/pos/PosTopBar.vue` | 顶栏中性化、44px 热区、标题/日期不换行 |
| `components/pos/dialogs/PreferenceDialog.vue` | 行距 32 / 留白 16、右侧控件不压缩 |
| `components/pos/dialogs/RetrieveOrderDialog.vue` | 加载骨架 |
| `components/pos/dialogs/CouponDialog.vue` | 验证按钮 loading |

## 五、遗留

- 微信/支付宝按钮保留渠道语义色（已降饱和），未并入品牌橙——它们是"钱从哪来"的识别锚点；
- 金额字体依赖 DIN/Bahnschrift 是否存在，缺失时回退 Roboto/系统字体；
- 110% 缩放下列数会变化（8→7 列），属于自适应预期行为，不是 bug。
