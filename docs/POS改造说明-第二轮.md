# 第二轮修复说明（头像菜单 / 交班重构 / P0 防御）

> 基线：第一轮交付后的 `frontend-pos`（Vue 3 + TS + Element Plus）。
> 本轮机型范围：前端交互与展示层 + 一项**向后兼容的后端增量字段**（实收现金落库，见 §4）。
> 验证：`npm run build` 通过；Playwright 27 项新断言 + 第一轮 21 项回归断言全绿。

---

## 1. 头像菜单「点了没反应」——根因与修复

### 1.1 根因（不是没绑定事件，是弹窗被"困"在顶栏里）

Element Plus 的 `el-dialog` **`append-to-body` 默认是 `false`**（`element-plus/es/components/dialog/src/dialog.mjs:12`），
即弹窗 DOM 渲染在组件原位——本组件位于 `.pos-top-bar` 内，而该容器有：

```css
.pos-top-bar {
  backdrop-filter: blur(10px);   /* ← 关键 */
}
```

`backdrop-filter` 会给后代 `position: fixed` 元素建立**包含块（containing block）**。
弹窗的遮罩层与容器都用 `position: fixed; inset: 0` 定位，于是它们的"视口"变成了
**高约 64px 的顶栏**：

- 弹窗被压成顶栏高度、遮罩只覆盖顶栏 → 视觉上"点了一下，什么都没发生"；
- 但 `isVisible()` 仍返回 true（元素有尺寸、未被 `display:none`），
  所以第一轮的自动化测试**没能发现**这个问题——只断言了可见性，没断言位置与尺寸。

### 1.2 修复

| 改动 | 文件 |
|---|---|
| 修改密码 / 偏好设置弹窗显式 `append-to-body="true"`（传送到 body，脱离 backdrop-filter 的包含块） | `ChangePasswordDialog.vue`、`PreferenceDialog.vue` |
| 开班弹窗同样补上，避免将来被放进带 `filter/transform` 的容器 | `StartShiftDialog.vue` |
| 测试补齐**几何断言**：弹窗 `boundingBox` 必须 `width>300 && height>200 && y>=0` | `test-review/pos-round2-verify.cjs` B1/B3 |

修复后实测：修改密码 `480×394 @ y=115`，偏好设置 `520×479 @ y=115`。

### 1.3 接口不可用必须 Toast，不许静默

`ChangePasswordDialog` 的失败分支改为 `describePasswordError()` 统一收敛：

| 场景 | 表现的 UI |
|---|---|
| 404 / 405 / 501 / 502 / 503（端点不存在或未就绪） | 内联红条 + **Toast「接口暂不可用：修改密码服务未就绪，请联系管理员」** |
| 后端返回业务错误（如原密码错误） | 内联红条 + Toast 透出后端 message |
| 网络异常 / 未知错误 | 内联红条 + Toast「修改密码失败，请稍后重试」 |

验证：mock `PUT /v1/auth/password` → 404，实测捕获到
`接口暂不可用：修改密码服务未就绪，请联系管理员`（截图 `round2-password-unavailable.png`）。

### 1.4 深色模式 / 缩放必须真的生效并持久化

| 项 | 落点 | 持久化键 | 验证 |
|---|---|---|---|
| 深色模式 | `documentElement[data-theme=dark]`（`useTheme.toggleTheme`） | `pos-theme-dark` | 切换后 `.fastfood-pos` 计算背景 `rgb(28,25,23)`；刷新后仍为 dark |
| 界面缩放 | `documentElement[data-pos-font-scale=large]` → `html[data-pos-font-scale=large] .fastfood-pos { zoom: 1.08 }` | `pos-preferences.fontScale` | 刷新后仍为 large |
| 每日确认开关 | `usePosPreferences.enterConfirmCheckout` | `pos-preferences` | Home 的 Enter 处理读该开关 |

---

## 2. 交班日结重构（图三 → 新版）

### 2.1 旧版问题（实测复现）

- 营业额来自 `/shift/summary` 的后端口径，退款来自明细口径，两个来源**混算**；
- 明细区默认展开，几十条订单直接铺在弹窗里，其中包含 `待付`/`已取消`/无支付流水的脏数据；
- 没有任何"我该交多少钱"的结论数字，收银员要自己做减法。

### 2.2 新版结构（自上而下，只有一条主线）

| 区块 | 内容 | 设计意图 |
|---|---|---|
| ① 异常红条 | 「本班次有 N 笔异常单（待付 1 笔 · 未记录支付流水 1 笔 · 已取消 1 笔），已从营业额中剔除」+ **[去处理]** | 异常隔离：不混入统计，但必须可见、可点进去核实 |
| ② **应交现金总额** | `¥650.00`（54px/800 等宽数字）+ 公式条 `备用金 ¥500.00 + 现金收入 ¥170.00 − 现金退款 ¥20.00` | 收银员唯一必须在意的数字，一眼看清该交多少 |
| ③ 四个次要指标 | 净营业额 / 有效订单 / 客单价 / 非现金收入 | 交给店长看的，不抢主视觉 |
| ④ 折叠明细 | **[📋 调阅本班次所有订单明细（共 5 笔）]** → 右侧 Drawer | 主界面严禁铺列表 |
| ⑤ 操作区 | 交班备注 / 导出报表 / 打印小票 / **确认交班**（二次确认，弹窗内回显应交现金与单量） | 敏感操作二次确认 |

支付方式分布从主界面移入明细抽屉（属"明细级"信息），主界面因此**不需要滚动**即可看到「确认交班」按钮。

### 2.3 数据清洗规则（唯一口径：明细）

| 分类 | 判定 | 是否计入营业额 | 去向 |
|---|---|---|---|
| 已支付 | `status ∈ {1 已确认, 2 已完成, 4 完成}` 且**有支付方式**且金额 > 0 | ✅ 计入 | 明细抽屉 |
| 已退款 | `status ∈ {5 全额退款, 7 已退}` | ✅ 计入（作为负项） | 明细抽屉 |
| 异常 | 其余一切：`0 待付`、`3 已取消`、`6 退款中`、**无支付方式（未记录流水）**、**金额 ≤ 0** | ❌ 剔除 | 异常抽屉（带原因标签） |

派生指标：

```
现金收入 = Σ(已支付 ∧ 现金)
现金退款 = Σ(已退款 ∧ 现金)
应交现金 = 备用金 + 现金收入 − 现金退款
净营业额 = Σ(已支付) − Σ(已退款)
客单价   = Σ(已支付) / 已支付笔数
支付分布 = 按支付方式求净额（已支付 − 已退款）
```

备用金取值顺序：`pos-shift-active`（开班时写入，最准）→ `/v1/pos/auth/shift/today` 最后一条 `openingCash` → 0 并**显式告警**
（`未读取到本班备用金，现金口径可能不完整`，绝不假装是 0）。

### 2.4 明令"不采信后端脏汇总"的验证

测试里把 `/shift/summary` 的 `orderStats` 故意 mock 成 `{ total_orders: 99, total_amount: 9999 }`，前端实测输出：

- 净营业额 `¥180.00`（= 200 − 20，来自明细清洗）
- 有效订单 `5` 笔
- 应交现金 `¥650.00`

→ 证明统计口径唯一、可解释、不被后端脏数据污染。

---

## 3. 三项 P0 防御

### 3.1 减号删行 → 5 秒撤销条（替代"静默删行"）

- `useCart.ts`：新增 `pendingUndo`（`{item, index, secondsLeft}`）+ `removeItemWithUndo` + `undoRemove`；
  `decreaseItem` 减到 0、`swipeDeleteItem`（垃圾桶/左滑）统一走它。
- `CartUndoBar.vue`（新增）：底部悬浮条「已移除 可乐 ×1 [撤销 5s]」，倒计时逐秒递减，5 秒后自动消失。
- 撤销按**原索引**插回；连续删除以最后一次为准（不会撤错行）。
- **刻意不用 `ElMessageBox` 二次确认**：高峰期每删一行弹一次窗会变成新瓶颈（见第一轮报告建议 3）。
- 清空购物车仍保留模态确认（`CartSection.vue`）。

### 3.2 取单静默覆盖购物车 → 接上护栏（并升级为"不丢单"）

旧实现 `selectHeldOrder` 只问「是否恢复该挂单」，然后 `cart.value = items` 直接覆盖。
新版（`useHangingOrder.ts`）：

1. 购物车非空时，弹确认并**量化损失**：「待恢复 ¥5.00 / 将被替换：当前 1 件商品 / 当前购物车会先自动挂起，之后可从取单里找回」；
2. 确认后**先把当前购物车挂起**（`hangOrder` 深拷贝落库），挂起失败就**取消取单**（宁可重试也不丢单）；
3. 再恢复目标挂单。

→ 从"静默覆盖"变成"覆盖前先存档"，把 P0 从"防呆"提升到"可恢复"。

### 3.3 实收现金 / 找零 / 付款码 → 真正上送并落库

**前端**（此前收集后丢弃）：

```ts
// composables/usePayment.ts（现金）
{ orderId, paymentMethod, amount,
  cashReceived: 10, changeAmount: 2 }
// 扫码支付
{ orderId, paymentMethod, amount, paymentCode: '1345...' }
```

**后端**（增量、向后兼容，3 改 1 增，0 删除）：

| 文件 | 改动 |
|---|---|
| `dto/PayRequestDTO.java` | 新增 `cashReceived` / `changeAmount`（BigDecimal，元，可空）、`paymentCode`（`@Size(max=64)`，可空） |
| `entity/OrderPaymentRecordNew.java` | 新增 `cash_received` / `change_amount`（Long，分）、`payment_code` 映射 |
| `db/migration/V20261028_01__pos_payment_record_cash_fields.sql` | `order_payment_records` 三条 `ADD COLUMN IF NOT EXISTS`（全部可空）+ 注释 |
| `service/impl/PosOrderPaymentServiceImpl.java` | 写支付记录前按非 null 落库；新增 `yuanToFen()`（×100 HALF_UP，与既有 `processBalancePayment` 口径一致） |

兼容性：老客户端不传 → 三列落 `NULL`（不是 0），插入语义与改动前一致；金额校验、状态机、退款、财务入账**未触碰**。
编译验证：`JAVA_HOME=P:\my-new-project\JDK21; mvn -o -q -DskipTests compile` → exit 0。

---

## 4. 验证矩阵（三个套件，全部 exit 0）

| 套件 | 覆盖 | 结果 |
|---|---|---|
| `test-review/pos-round2-verify.cjs` | 交班 A1–A9、头像菜单 B1–B7、撤销条 C1–C2、取单护栏 D1–D4、支付字段 E1–E3 | **27/27 通过** |
| `test-review/pos-ui-verify.cjs`（第一轮） | 开班弹窗 / 刷新不重弹 / 菜单四项 / 密码弹窗 / 偏好 / 退出清缓存 | **20/20 通过**（回归） |
| `test-review/pos-payment-double-enter.cjs`（第一轮） | 单次回车 → 1 确认框 / 1 次 createOrder / 1 次 payOrder | **PASS**（回归） |

关键断言原文：

```
A1 应交现金 = 500+170-20 = ¥650.00            → ¥650.00
A3 净营业额 = 200-20 = ¥180.00（不采信后端 9999）→ ¥180.00
A5 异常红条 3 笔 → 待付 1 笔 · 未记录支付流水 1 笔 · 已取消 1 笔
A6 主界面不铺明细列表 → rows=0
A8 明细抽屉只含 5 笔有效单 → rows=5
B2 接口 404 → Toast「接口暂不可用」→ hits=1 msgs=接口暂不可用：修改密码服务未就绪，请联系管理员
B1 修改密码弹窗几何 → {"w":480,"h":394,"y":115}
C1 减到 0 → 行消失并出现撤销条 → bar=true rows=0
C2 点撤销 → 行回来了 → rows=1
D3 取单前弹出「替换购物车」确认 → 命中
D4 取消后购物车未被覆盖 → 薯条
E1 单次回车只创建 1 单 → create=1
E2 支付请求带上 cashReceived/changeAmount → {"amount":8,"cashReceived":10,"changeAmount":2}
```

截图：`round2-shift-handover.png`、`round2-shift-detail-drawer.png`、`round2-shift-anomaly-drawer.png`、
`round2-password-unavailable.png`、`round2-preferences-dark.png`、`round2-dark-persisted.png`、
`round2-undo-bar.png`、`round2-takeorder-guard.png`。

---

## 5. 变更文件

**前端（修改 10 / 新增 1）**

| 文件 | 说明 |
|---|---|
| `views/ShiftHandover.vue` | 整页重构：应交现金大字、清洗口径、异常隔离、折叠明细抽屉 |
| `views/Home.vue` | 接入撤销条组件、透传 `paying` |
| `composables/useCart.ts` | 5 秒撤销机制（减到 0 / 垃圾桶 / 左滑统一入口） |
| `composables/useHangingOrder.ts` | 取单护栏 + 覆盖前自动挂起 |
| `composables/usePayment.ts` | 上送 cashReceived / changeAmount / paymentCode |
| `components/pos/dialogs/ChangePasswordDialog.vue` | `append-to-body`、失败必 Toast、接口不可用识别 |
| `components/pos/dialogs/PreferenceDialog.vue` | `append-to-body`、主题/缩放切换反馈 |
| `components/pos/dialogs/StartShiftDialog.vue` | `append-to-body` |
| `api/posApi.ts` | `PayRequest` 新增三个可选字段 |
| `styles/pos/components/dialog.css` | 交班弹窗头部隐藏、MessageBox 按钮暖橙化 |
| **`components/pos/CartUndoBar.vue`** | 新增：撤销条 |

**后端（3 改 1 增）**：`PayRequestDTO.java`、`OrderPaymentRecordNew.java`、`PosOrderPaymentServiceImpl.java`、`V20261028_01__pos_payment_record_cash_fields.sql`。

## 6. 遗留与风险

| 项 | 说明 |
|---|---|
| Flyway 版本号 | `V20261028_01` 小于库中已存在的 `20261028.015`，已确认 dev/pg/prod/test 四个 profile 均为 `out-of-order: true`，可正常补跑；若某环境关闭了 out-of-order，需要把版本号改成更晚的时间戳 |
| `OrderNewServiceImpl:268` 的另一处支付记录写入 | 未传现金字段（落 NULL，无害）；若该入口也要落现金字段，需同改 `OrderPayDTO` |
| 明细数据来源 | 交班统计以 `/v1/pos/orders` 明细为准；若该接口不可用会显式提示「营业额请以班次汇总为准」，不会静默显示 0 |
| 状态码映射 | 后端 `order_status` 规范码 0/1/2/3/5 与历史码 4/7 均已在 `cleanOrder` 覆盖；若后端引入新状态码，异常抽屉会以「未知状态（X）」呈现，不会误计 |
