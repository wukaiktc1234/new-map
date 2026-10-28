# 第十二轮：深色外壳 / Toast 工业化 / 弹窗自适应 / 日期面板深色 / 备用金复核

> 五项反馈逐条处理。验证：十套 E2E 共 **184 项断言全绿**（1440×900 与 1280×720，各含 100% 与 110% 缩放）。

---

## 1. 深色"白边 + 四角没倒圆"

**核查结果**（用真实浏览器量了 4 种组合：1440×900 / 1280×720 × 100% / 110% 缩放）：

| 检查项 | 1440 100% | 1440 110% | 1280 100% | 1280 110% |
|---|---|---|---|---|
| 根容器 rect | 0,0,1440,900 | 0,0,1440,900 | 0,0,1280,720 | 0,0,1280,720 |
| html 背景 | #1e1e1f | #1e1e1f | #1e1e1f | #1e1e1f |
| body 背景 | #1e1e1f | #1e1e1f | #1e1e1f | #1e1e1f |

**页面内部没有白边**（根容器正好铺满视口，html/body 已是深色）。

同时补了两条防御规则，避免任何场景下露出白底：
- `html, body { background: var(--pos-bg-primary) }`（原有）+ 关键补充：滚动态锁死 `overflow:hidden`；
- 新增全局 `::selection` 品牌色（原来深色下选中金额是刺眼的系统蓝，见备用金弹窗截图）。

⚠️ 如果你截图里那圈白边是**浏览器/外框窗口**自身的边（不在页面 DOM 内），前端无法覆盖——那需要壳层（Electron/WebView）设置 `backgroundColor` 或窗口圆角。判断方法：把浏览器切全屏（F11），若白边消失即为外壳问题。

同时修掉了同一批的 **顶栏被裁**：1280 或 110% 缩放下可用宽度只有 1164px，顶栏内容原本需要 1370px → 右侧账号区被切掉。现在按断点降级：≤1360 只留图标（title 提示），≤1120 再收起低频入口（桌台/视觉识别/券码）。四种组合实测 `scrollWidth == clientWidth`。

## 2. Toast（图二）工业化重做

旧版沿用 Element Plus 的浅色半透明底，深色下会**透出下面的按钮文字**（"挂单/取单/叫号"叠在提示里）。

新版：
```css
.el-message {
  background: #fff / #2C2C2E;        /* 实底，不透明 */
  border: 1px solid #3A3A3E;
  border-left: 3px solid 状态色;      /* success/warning/error 一眼可辨 */
  box-shadow: 0 12px 32px rgba(0,0,0,.55);
  top: 76px;                          /* 固定落在 64px 顶栏下方 */
  z-index: 3200;
}
```
实测：背景 `rgb(44,44,46)`、不透明度 1、位置 y=76（顶栏高 64），不再压任何按钮。

## 3. 各窗口宽度自适应（图三 / 图四）

旧版每个弹窗写死 `width="1120px"/"660px"`，小窗口或 110% 下右侧被裁 → 只能横向拖。

现在统一改成 **min(设计宽度, 94vw)**，并在 ≤1400 收窄内边距：

| 弹窗 | 旧 | 新 |
|---|---|---|
| 订单查询 | 1120px | min(1120px, 94vw) |
| 取单 | 660px | min(660px, 94vw) |
| 挂单 | 560px | min(560px, 92vw) |
| 订单详情 | 650px | min(680px, 94vw) |
| 库存查询 | 700px | min(700px, 94vw) |
| 结账确认 | 520px | min(520px, 92vw) |
| 开班/备用金 | 560px | min(560px, 92vw) |

实测 1280 窗口：订单查询宽 1120（≤94vw）、表格 `scrollWidth 1038 == clientWidth 1038`（无横向截断）。

另外修了**短屏被顶出**的老问题：`.cart-list` 补 `min-height:0`（flex 子项原来按内容撑开），并按视口高度降档支付按钮（52→46→42px）。实测 1280×720@110%：支付按钮底边 688 < 720，完整可见。

## 4. 日期选择器深色（图五）

面板 teleport 到 body，不受弹窗深色规则覆盖，整块保持白底。补齐：
- `.el-picker__popper / .el-picker-panel / .el-date-range-picker` → `#2C2C2E`
- 表头/单元格文字 → 浅色；上一月/下一月 → 灰；今天 → 品牌橙
- 选中区间 → 品牌橙半透明；起止日 → 实心品牌橙
- popper 箭头同步深色（否则箭头是白色小三角）

实测：面板 `rgb(44,44,46)`，可选日期文字 `rgb(229,229,229)`。

## 5. 备用金（开班）弹窗复核

用 1280×720 + 深色实测：
- 宽度 560 → 落在 94vw 内（x=360，右侧不越界）；
- 深色底 `rgb(44,44,46)`，标题/说明/快捷键提示对比度正常；
- 顺手修了「金额被系统蓝选中」的刺眼问题（新增全局 `::selection`）；
- 备用金业务口径未动：默认取上一班 openingCash → 无历史则 0.00，回车即开班。

---

## 验证汇总

| 套件 | 项数 |
|---|---|
| pos-round12-verify.cjs（新增） | 25 |
| pos-retrieve-verify.cjs | 30 |
| pos-order-query-scroll.cjs | 14 |
| pos-round8-verify.cjs | 15 |
| pos-visual-verify.cjs | 16 |
| pos-hold-dialog-verify.cjs | 16 |
| pos-round2-verify.cjs | 42 |
| pos-ui-verify.cjs | 20 |
| pos-negative-cash.cjs | 5 |
| pos-payment-double-enter.cjs | 1 组 |

截图：round12-home-1280-large-dark.png、round12-toast-dark.png、round12-datepicker-dark.png、round12-order-query-1280-dark.png、round12-startshift-dark.png。
