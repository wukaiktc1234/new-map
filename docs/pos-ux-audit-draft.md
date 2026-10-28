# POS 收银端体验审计草稿（第一视角：门店收银员）

> 审计范围：`frontend-pos/src`（Vue 3 + TS + Element Plus），基线为当前工作区快照。
> 审计方法：只读通读源码，逐条取证；**未修改任何源码、未执行 build**。
> 本文所有结论均带 `文件:行号` 证据，未取证的内容不写入。
> 撰写人：ux-auditor（子代理）／交付对象：lead

---

## 总评

**4.5 / 10（不及格偏下，可用但不敢快用）**

一句话结论：**这套收银台的「展示层」做得比「操作层」好——视觉有设计感、骨架屏和空态齐全，但收银员真正赖以提速的东西（扫码枪商品码入口、全局快捷键、删除/取单的防呆、断网可见性）几乎全缺，且存在 3 处会直接导致「算错钱 / 丢行 / 重复下单」的硬缺陷。**

分维度评分：

| 维度 | 分数 | 一句话 |
|---|---|---|
| 1. 快捷键与硬件联动 | 2 / 10 | 全站只有一个 Enter 绑定，且它在支付弹窗里会**双触发**；扫码枪商品码入口为零 |
| 2. 容错与防呆 | 3 / 10 | 减号到底＝删行、滑动＝删行、取单＝覆盖购物车，三项均无二次确认 |
| 3. 视觉与热区 | 5 / 10 | 主按钮 52px 达标，但顶栏 40px、数量热区 24px、菜品名 11px、`--pos-text-muted` 对比度不足 |
| 4. 高频流转效率 | 4 / 10 | 一单现金结账 **4 次点击 + 3 层弹窗 + 2 次重复确认**，且每单必须点一次「完成」 |

---
## 维度一：快捷键与硬件联动

### 现状（取证）

**1）收银端全局键盘绑定只有 1 条，且只服务支付弹窗**

`frontend-pos/src/views/Home.vue:574-581`

```ts
handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && showPaymentConfirm.value) {
    event.preventDefault()
    event.stopPropagation()
    confirmPayment()
  }
}
window.addEventListener('keydown', handleKeyDown, true)   // ← 捕获阶段，最优先
```

全量检索 `keydown|keyup|KeyboardEvent|event.key`（31 处命中）后可确认：**收银主页之外，POS 侧没有任何全局快捷键注册**，其余命中全部是弹窗内局部 `@keyup.enter`（`CartSection.vue:69`、`PaymentConfirmDialog.vue:30,55`、`CouponDialog.vue:13`、`CallHistoryPanel.vue:9`）。

**2）后厨端有完整快捷键体系，收银端缺失——对比证据**

`frontend-pos/src/composables/kitchen/useKeyboardShortcuts.ts:56-127` 定义了 13 条快捷键（`1`~`5` 选单、`Enter` 开做、`空格` 完成、`Escape` 取消选择、`Tab` 切栏、`F1` 刷新、`F5` 强刷、`?` 帮助、`Ctrl+A` 全选），并在 `:132-177` 统一分发、`:183-197` 统一注册、`:240-249` 随组件生命周期自动注册/注销；还配了 `ShortcutHelp.vue` 帮助面板（`components/kitchen/ShortcutHelp.vue:31-78` 高亮当前按键）。

而收银端的 `Home.vue` 没有引入 `useKeyboardShortcuts`（`Home.vue:133-163` 的 import 段无此引用）。**同一仓库、同一批用户：后厨有，收银没有。**

**3）扫码枪（HID 键盘模拟）商品码录入入口：不存在**

- 菜品区结构里没有任何输入框：`MenuSection.vue:1-96` 模板只有分类栏（`:4-16`）、错误态（`:19-26`）、菜品网格（`:29-83`）、骨架屏（`:86-94`）。**无 el-input、无 input、无搜索框。**
- 菜单侧唯一的筛选是分类点击：`MenuSection.vue:6-14`，`@click="$emit('selectCategory', cat.categoryId)"`；`Home.vue:45` 直接赋值 `activeCategory = $event`。
- 全仓检索 `扫码|barcode|dishCode`：与商品码相关的只有「扫码枪」文案，且出现在**支付**弹窗里（`PaymentConfirmDialog.vue:45,52,74`），以及 `CameraScanner.vue`（摄像头扫二维码核销，属 `Scan.vue` 路由，不在收银主页）。
- 结论：收银员扫一个**商品条码**，键盘事件落到 body，被 `Home.vue:574` 的监听器读到，但它只判断 `event.key === 'Enter' && showPaymentConfirm`，数字串全部丢弃，**不触发任何搜索或加购**。

**4）扫码后焦点归位：无处可归**

没有搜索框，自然没有「扫完回到搜索框」。除此之外，加购、删行、结账完成后**也没有任何 focus() 归位**：`useCart.ts:43-71`（addToCart）纯数据操作无 DOM 焦点处理；`usePayment.ts:131` 结账成功后 `clearCart()`，无 `nextTick` 后的焦点重置。唯一存在的自动聚焦是打开弹窗时聚焦输入框（`Home.vue:487-505`、`PaymentConfirmDialog.vue:119-145`、`CouponDialog.vue:122-130`）。

**5）F1 挂单 / F2 结账 / F3 取单 / ESC 取消 / Delete 删行：全部缺失**

当前只能靠鼠标/触摸点击顶栏与购物车：

| 期望快捷键 | 现状对应的鼠标路径 | 证据 |
|---|---|---|
| F1 挂单 | 顶栏「挂单」按钮 | `PosTopBar.vue:25-28` |
| F2 结账 | 「现金/微信/支付宝」按钮 | `CartSection.vue:132-142` |
| F3 取单 | 顶栏「取单」按钮 | `PosTopBar.vue:30-35` |
| ESC 取消/关弹窗 | 无绑定，仅 Element Plus 默认行为 | `Home.vue:574-581` |
| Delete 删行 | 无绑定，需点垃圾桶或左滑 | `CartSection.vue:80-99` |

### 缺陷分级

| 编号 | 缺陷 | 级别 | 证据 |
|---|---|---|---|
| K-1 | Enter 在支付弹窗中被「捕获监听 + 输入框 keyup」双路触发，`confirmPayment()` 并发执行两次 | **P0** | `Home.vue:574-581`（capture=true，未排除输入焦点）＋ `PaymentConfirmDialog.vue:30`＋ `usePayment.ts:64-84`（幂等标志 isConfirming 在 await ElMessageBox.confirm **之后**才置 true，两次调用都穿过守卫） |
| K-2 | 无扫码枪商品码入口，条码商品无法一枪加购 | **P1** | `MenuSection.vue:1-96` 全模板无输入框；`Home.vue:574-581` 吞掉数字串 |
| K-3 | 无任何 POS 全局快捷键（F1/F2/F3/ESC/Delete），对比后厨端已成体系 | **P1** | `useKeyboardShortcuts.ts:56-127` vs `Home.vue:133-163,574-581` |
| K-4 | 操作后无焦点归位策略，扫码枪「下一枪打空」 | **P1** | `useCart.ts:43-71`；`usePayment.ts:129-133` 无 nextTick 焦点重置 |
| K-5 | 键盘监听未做「输入框内不触发」排除（后厨端做了） | **P1** | `Home.vue:574-580` vs `useKeyboardShortcuts.ts:134-142` |

### 影响（收银员一句话）

> 「客人递来一瓶可乐让我扫一下，我扫了——屏幕没反应，只能自己去七列网格里翻分类找图；高峰时我按 Enter 结账，屏幕上突然叠了两个『确认支付 ¥18.00』，我不知道该点哪个，点完发现出了两单。」

---
## 维度二：容错与防呆

### 现状（取证）

#### A. 高危操作逐个对账

| 操作 | 是否有二次确认 | 代码证据 | 判定 |
|---|---|---|---|
| 删除购物车行（PC 垃圾桶） | **无** | `CartSection.vue:80-87` `@click.stop="$emit('delete', index)"` → `Home.vue:31` → `useCart.ts:194-196` `cart.value.splice(index, 1)` | **一点就没** |
| 删除购物车行（触摸左滑） | **无** | `CartSection.vue:93-99` 删除按钮 → 同上；滑动手势在 `useCart.ts:146-165`，仅做位移结算 | **一点就没**（两次触摸） |
| 「−」把数量减到 0 | **无，且静默删行** | `useCart.ts:81-87`：`if (qty > 1) qty-- else cart.splice(index,1)` | **一点就没**（最危险：视觉上只是「减一」） |
| 清空购物车 | **有** | `CartSection.vue:206-217` `ElMessageBox.confirm('确定清空购物车？此操作不可撤销')` | 合格 |
| 改数量（点数字→输入） | 无确认，但可取消 | `CartSection.vue:54-71`；`useCart.ts:101-107`（`Math.min(MAX, Math.max(1, floor(qty)||1))` 夹取，防住 0/负数/NaN） | 合格（夹取是亮点） |
| 改价 / 手动折扣 / 抹零 | **功能不存在** | 全仓 `discountAmount` 仅来自优惠券：`Home.vue:175,461`；无价格编辑入口（`useCart.ts:89-111` 只有数量编辑） | **能力缺失** |
| 退款 | **有**（prompt 需填原因） | `Home.vue:398-423`，`:407` `inputValue: '用户申请退款'` | 半合格（见 R-5） |
| 挂单覆盖 | 不适用（挂单是新建条目） | `useHangingOrder.ts:27-43`；`hanging-order.ts:123-132` `unshift` 新条目 | 合格 |
| **取单（恢复挂单）** | **无「当前购物车将被覆盖」提示** | `useHangingOrder.ts:50-68` `selectHeldOrder`：只确认「是否恢复该挂单」，随后 `cart.value = items`（`:59`）**直接覆盖**；带护栏的 `quickRestoreHeldOrder`（`:84-95`，会提示「恢复挂单将清空当前购物车」）**全仓无调用点** | **P0：静默丢单** |
| 清空全部挂单 | **有** | `useHangingOrder.ts:70-82` | 合格 |
| 退出登录 | **有** | `PosTopBar.vue:228` | 合格 |

#### B. 优惠券折扣不随购物车变化重算（金额错）

- 应收金额定义：`Home.vue:175-176`

```ts
const discountAmount = ref(0)
const totalAmount = computed(() => Math.max(0, originalTotalAmount.value - discountAmount.value))
```

- 折扣只在「应用券」那一刻写死：`Home.vue:461` `discountAmount.value = result.discountAmount ?? ...`
- 门槛只在应用时校验一次：`Home.vue:437-441`（`originalTotalAmount < couponInfo.minAmount` 才拦）
- 之后**没有 watch(cart) 去失效或重算折扣**（`Home.vue:622-637` 的 watch 只同步副屏展示）。
- 后果：60 元订单用「满 50 减 10」，收银员随后删两件菜变 20 元，`totalAmount` 仍等于 `20 - 10 = 10`，**收银员按 10 元收款**。

#### C. 退款流程

`Home.vue:400-413`

```ts
const { value: refundReason } = await ElMessageBox.prompt(
  `订单号: ${order.orderNumber} / 退款金额: ¥${order.totalAmount?.toFixed(2)}`,   // 原文为模板字符串，此处为等价摘录
  '退款确认',
  { confirmButtonText: '确认退款', inputPlaceholder: '请输入退款原因（可选）',
    inputValue: '用户申请退款', type: 'warning' }   // ← 预填 + 输入框自动聚焦
)
await posApi.refundOrder(order.orderNumber || order.orderId, refundReason || '用户申请退款')
```

- 输入框预填且自动聚焦 → **按一下 Enter 就退款成功**，无金额复核、无权限校验、无「部分退款」能力（`posApi.ts:233-238` 的 `RefundRequest` 只有 orderNumber + refundReason，**没有金额字段**）。
- 券码核销在「应用」时就打后端（`Home.vue:444-461`），若随后弃单，**券不会回滚**（无逆操作调用）。

#### D. 网络断开 / 接口超时 / 全局连接提示

- 超时：`request.ts:4-7` `timeout: 30000`（**30 秒**，收银台前等于死机）。
- 无全局 loading、无请求计数；拦截器只做解包与错误拒绝：`request.ts:84-100`。
- 401：`request.ts:106-144` 有并发单飞刷新（isRefreshing + 队列），失败则 `clearAuthAndRedirect()`（`:75-82`）跳登录——**跳转前不提示「登录已过期」**，正在填的输入全丢（购物车有 `useCart.ts:223-228` localStorage 兜底）。
- 业务错误抛裸对象：`request.ts:96`、`:147-149` `Promise.reject({ message, ... })`，**是否提示完全由调用方决定**。
- WebSocket：`websocket.ts:18-21` 3s 重连；`:50-64` 应用层监督重连（注释说明 stompjs v7 内部重连会停摆）；`:87-123` 全事件回调都调 `onConnectionChange`。
- 但 **在收银端只被用来改「菜单兜底刷新间隔」**：`Home.vue:546-559` —— **没有任何 UI 元素表达「断线中」**。`websocket.ts:196-198` 提供了 `isConnectedStatus()`，全仓无一处调用它渲染状态。
- 对照组：后厨端有连接文案 `KitchenHome.vue:324`（reconnecting: '重连中...'），叫号页有在线状态点 `CallNumber.vue:171`。**收银主页没有。**

#### E. 错误只进 console，用户无感知

`logger.ts:22-24`

```ts
const emit = (method, args) => {
  if (!isDev || !native) { return }   // ← 生产环境直接 return，连 console 都不打
  ...
}
```

全仓 logger.errorMessage 命中 56 处，收银主链路的关键失败都是「静默」：

- `Home.vue:221` 开班检查失败（注释写「不影响收银」）；
- `Home.vue:275-276` 叫号统计 **catch {} 空吞**；
- `Home.vue:355,379` 订单查询失败（`:378` 注释「失败时显示空列表」——收银员看到的是「没有订单」，不是「查询失败」）；
- `Home.vue:394` 订单详情失败；`Home.vue:567` WS 连接失败；
- `useCart.ts:219` 购物车恢复失败（**静默丢购物车**）。

**结论：生产环境 logger 是黑洞（不打印、不提示、不上报），收银员面对失败只有「界面没变」这一种反馈。**

#### F. 结算数据不上送（对账缺口）

- `usePayment.ts:88-99` 组单：items 用**原价**、totalAmount 用**折后价**、`tableNumber: 1` **硬编码**。
- `usePayment.ts:112-116` 支付：只传 orderId / paymentMethod / amount。
- `PaymentConfirmDialog.vue:162` 收集了 cashReceived（实收现金）与 paymentCode（扫码枪付款码），`usePayment.ts:158-164` 存进 ref，**但 confirmPayment 全程没有使用它们**（`usePayment.ts:88-133`）。`PayRequest`（`posApi.ts:223-230`）也**没有这两个字段**。
- 后果：现金实收/找零不落库、第三方付款码不落库、明细金额与总额不等、所有 POS 订单都是 1 号桌。

### 缺陷分级

| 编号 | 缺陷 | 级别 | 证据 |
|---|---|---|---|
| R-1 | 「−」减到 1 再按＝静默删行；垃圾桶/左滑删除均无确认 | **P0** | `useCart.ts:81-87,194-196`；`CartSection.vue:47-52,80-87,93-99` |
| R-2 | 取单直接覆盖当前购物车，无提示（护栏函数未被调用） | **P0** | `useHangingOrder.ts:50-68` vs `:84-95`；`Home.vue:78-82` 只绑 select |
| R-3 | 优惠券折扣不随改单失效/重算 → 可按错金额收款 | **P0** | `Home.vue:175-176,437-441,461`；`Home.vue:622-637` 无折扣失效逻辑 |
| R-4 | 现金实收/找零、付款码不上送；明细金额与总额不等；tableNumber 硬编码 1 | **P0** | `usePayment.ts:88-99,112-124,158-164`；`posApi.ts:223-230` |
| R-5 | 退款：原因预填 + 输入框聚焦，Enter 即退；无金额复核、无部分退款 | **P1** | `Home.vue:400-413`；`posApi.ts:233-238` |
| R-6 | 无全局离线/断线横幅；isConnectedStatus() 无人调用 | **P1** | `Home.vue:546-559`；`websocket.ts:196-198`；对照 `KitchenHome.vue:324` |
| R-7 | 生产环境 logger 完全静默，关键失败用户零感知 | **P1** | `logger.ts:22-24`；`Home.vue:221,275,355,379,394,567`；`useCart.ts:218-220` |
| R-8 | 30s 超时无进度提示/无取消 | **P1** | `request.ts:6`；全局无 loading 计数 |
| R-9 | 401 刷新失败直接跳登录，无「登录已过期」提示 | **P2** | `request.ts:75-82,142-143` |
| R-10 | 券码核销不可回滚（弃单后券已核销） | **P1** | `Home.vue:444-471`；`Home.vue:470-474` 只清本地 state |
| R-11 | 挂单仅存 localStorage（单机、清缓存即丢、多终端不共享） | **P1** | `hanging-order.ts:1-7,122-132,198-201` |

### 影响（收银员一句话）

> 「我想把可乐从 2 瓶改成 1 瓶，多点了一下减号——整行消失了，后面排队的客人看着我重新找菜；取单时手快点了一张旧挂单，我正在点的三样菜**没了，也没有一句提示**。」

---
## 维度三：视觉与热区

### 现状（量化取证）

#### 1）热区尺寸（触屏标准 44×44）

| 元素 | 实际尺寸 | 是否 ≥44px | 证据 |
|---|---|---|---|
| 购物车「+/−」 | `width:28px;height:28px` 但 `min-width:44px;min-height:44px` | ✅ 44×44（写法矛盾但生效） | `CartSection.vue:368-381` |
| 购物车数量数字（点它进编辑） | `font-size:16px; min-width:24px`，**无 padding、无高度** | ❌ 约 24×22 | `CartSection.vue:409-421` |
| 数量编辑输入框 | `width:50px; height:28px` | ❌ 高 28 | `CartSection.vue:423-425` |
| 现金 / 微信 / 支付宝 | 52px / 44px | ✅ | `CartSection.vue:558-560,596,604-607` |
| 顶栏 9 个功能按钮 | `height:40px; font-size:13px` | ❌ 40 | `PosTopBar.vue:368-385` |
| 购物车「清空」 | Element `size="small" text` | ❌ 约 24 | `CartSection.vue:10-19` |
| 分类胶囊 | `padding:8px 20px; font-size:14px` → 高约 30 | ❌ 高 30 | `MenuSection.vue:269-274` |
| 菜品卡片 | 固定 7 列网格，卡片高约 90–96px（padding 6 + visual 50 + 4 + 名称 11 + 价格 13） | ✅ 面积大 | `MenuSection.vue:328-334,374-396,465-474,493-520` |
| 弹窗关闭 ✕（自绘） | 30×30px | ❌ | `HoldOrderDialog.vue:124-129`；`RetrieveOrderDialog.vue:119-125` |
| 弹窗关闭 ✕（全局规范） | 32×32px（且该样式文件未生效，见 5） | ❌ | `styles/pos/components/dialog.css:27-34` |
| 弹窗底部按钮 | `min-width:80px; padding:10px 20px`（≈40 高） | ❌ | `styles/pos/components/dialog.css:63-69` |
| 挂单 / 取单弹窗按钮 | `height:44px` | ✅ | `HoldOrderDialog.vue:216-219`；`RetrieveOrderDialog.vue:189-192` |

**统计：可点击元素中约 7 类达不到 44px，其中「数量数字 24px」与「顶栏 40px」是每秒都要碰的位置。**

#### 2）字号层级

| 语义 | 字号 / 字重 | 位置 |
|---|---|---|
| 应收金额（购物车底部） | **26px / 800** | `CartSection.vue:537-541` |
| 支付弹窗金额 | **32px / 800** | `PaymentConfirmDialog.vue:210-214` |
| 成功页叫号号 | **48px / 800** | `PaymentSuccessDialog.vue:188-195` |
| 挂单预览合计 | 26px / 800 | `HoldOrderDialog.vue:203-207` |
| 取单卡金额 | 22px / 800 | `RetrieveOrderDialog.vue:180-184` |
| 购物车品名 | 15px / 500 | `CartSection.vue:344-352` |
| 购物车行金额 | 14px / 600 | `CartSection.vue:354-358` |
| 数量 | 16px / 600 | `CartSection.vue:409-411` |
| **菜品名（网格）** | **11px / 600** | `MenuSection.vue:493-501` |
| **菜品价格（网格）** | **13px / 700**（货币符号 9px） | `MenuSection.vue:516-520,510-514` |
| **库存 / 套餐 / 热销角标** | **10px** | `MenuSection.vue:399-410,423-433` |
| 分类 | 14px / 500 | `MenuSection.vue:269-276` |
| 金额标签「商品金额 / 应收金额」 | 14px | `CartSection.vue:508-511` |
| 全局基准 | 14px / 1.5 | `styles/pos/base.css:11-12` |

**层级结论：金额层级做得好（26 / 32 / 48 三档递进，符合收银员「先看钱」的认知顺序）；但菜单区严重偏小——11px 品名 + 10px 角标，在 1366×768 收银一体机上基本是「看得见读不清」，而收银员 80% 的操作时间花在这块网格上。**

#### 3）对比度风险（WCAG AA 4.5:1，取设计令牌实际值）

令牌来源：`styles/pos/variables.css:17-26`

| 前景 | 背景 | 近似对比度 | 风险点 |
|---|---|---|---|
| `--pos-text-muted: #94a3b8` | `--pos-bg-primary: #ffffff` | **≈2.8:1** ❌ | `CartSection.vue:508-511` 金额标签、空态文案 |
| `--pos-text-muted: #94a3b8` | `--pos-bg-secondary: #f8fafc` | **≈2.7:1** ❌ | `MenuSection.vue:365` 空态、`RetrieveOrderDialog.vue:185` 备注 |
| `--pos-primary: #ea580c` | `--pos-bg-secondary: #f8fafc` | **≈3.5:1** ❌（13px 小字） | `MenuSection.vue:519` 菜品价格 |
| `--pos-text-secondary: #64748b` | #ffffff | ≈4.8:1 ✅ | 分类未选中 |
| `--pos-text-primary: #1e293b` | #ffffff | ≈14:1 ✅ | 品名、金额主体 |

> 说明：以上为按 sRGB 相对亮度公式的近似值，用于定位风险，不作为验收数值。

#### 4）误触 / 双击缩放处理

- 全局已处理：`styles/index.css:5,16` `-webkit-tap-highlight-color: transparent`；`:20-33` 对 `.dish-item/.category-item/.pay-btn/.qty-btn/.el-button` 设 `touch-action: manipulation`；`:36-43` 输入框保留 `user-select: text`；`:52-64` 滚动区 `pan-y`。
- 双击缩放兜底：`main.ts:22-35` 全局 `touchend` 300ms 双判 `preventDefault()`，并**特意放行 .qty-number**（`:24-28`，因为要连点数量进编辑）。
- 组件内：`MenuSection.vue:279` 分类 `user-select:none`；`CartSection.vue:482` 滑动删除区 `user-select:none`；`PosTopBar.vue:384` 按钮 `user-select:none`；`base.css:44-49` 提供 `.pos-no-select`。
- **缺口：.dish-item 与 .cart-item 本身没有 user-select:none（`index.css:74-78` 只关了长按菜单），长按/拖拽仍会选中文字并出现蓝色选区；取单卡片 .rd-card（`RetrieveOrderDialog.vue:145-157`）也没有 touch-action，滑动手感与购物车不一致。**

**触屏判定三套并存（行为不一致的隐患）**：

- `useCart.ts:37-41` = `ontouchstart && maxTouchPoints>0`（**与**）
- `CartSection.vue:188` = `'ontouchstart' in window`（只看其一）
- `useKeyboardShortcuts.ts:28-36` = `ontouchstart || maxTouchPoints || msMaxTouchPoints`（**或**）
- 后果：某类带触屏的 Windows 一体机可能在购物车显示「可左滑删除」（`CartSection.vue:93-99`）却在 `useCart.ts:167-192` 不绑定滑动事件；或反之**隐藏垃圾桶按钮**（`CartSection.vue:81` `v-if="!isTouchDevice"`）导致鼠标用户找不到删除入口。

#### 5）样式文件失效（配置性缺陷）

`styles/pos/components/dialog.css`（129 行，含弹窗圆角/头部/按钮 40px/遮罩模糊等规则）**在全仓没有任何导入**：

- `main.ts:9-12` 只导入 `styles/variables.css`、`styles/index.css`、`styles/pos/variables.css`、`styles/pos/base.css`；
- 全仓检索 `@import` **0 命中**；检索 `pos/components` **0 命中**。

也就是说 **POS 弹窗的统一视觉规范（含 32px 关闭按钮、80px 最小按钮）是一份死代码**，实际生效的是 Element Plus 默认值 + 各组件 scoped 覆盖。这解释了为什么 `HoldOrderDialog` / `RetrieveOrderDialog` 与自己 `padding:0` 的头部、`PaymentConfirmDialog` 的 520px 宽度能各自为政。

### 缺陷分级

| 编号 | 缺陷 | 级别 | 证据 |
|---|---|---|---|
| V-1 | 菜品网格品名 11px、角标 10px、价格 13px——收银主力区可读性不足 | **P1** | `MenuSection.vue:493-520,399-410,423-433` |
| V-2 | `--pos-text-muted`(#94a3b8) 用于「应收金额」等关键标签，对比度 ≈2.7–2.8:1 | **P1** | `variables.css:25`；`CartSection.vue:508-511` |
| V-3 | 顶栏 9 个按钮 40px 高、13px 字 | **P1** | `PosTopBar.vue:368-385` |
| V-4 | 数量数字热区 24px、编辑框 28px 高 | **P1** | `CartSection.vue:409-421,423-425` |
| V-5 | dialog.css 未导入，弹窗规范整体失效（含 32px 关闭按钮） | **P1** | `main.ts:9-12`；`styles/pos/components/dialog.css:27-34,63-69` |
| V-6 | 三套触屏判定不一致，删除入口可见性可能相反 | **P1** | `useCart.ts:37-41`；`CartSection.vue:81,188`；`useKeyboardShortcuts.ts:28-36` |
| V-7 | 分类胶囊仅 30px 高；固定 7 列网格在 1366 宽度下每卡≈118px | **P2** | `MenuSection.vue:269-274,328-334` |
| V-8 | .dish-item / .cart-item 未禁文本选择；.rd-card 缺 touch-action | **P2** | `index.css:74-78`；`RetrieveOrderDialog.vue:145-157` |

### 影响（收银员一句话）

> 「顶栏那排字又小又挤，我总是点错『券码』和『订单』；菜单里 11px 的品名我戴着老花镜也得凑近看，索性全按图记位置——一换菜单就出事。」

---
## 维度四：高频流转效率

### 现状（点击次数实测统计）

> 计数口径：一次「点击」= 一次可点击元素按下；一次键盘输入按 1 次交互计。不含鼠标移动。

#### 1）点单 → 加菜

| 动作 | 点击次数 | 证据 |
|---|---|---|
| 加 1 个菜（分类已选） | **1 次** | `MenuSection.vue:30-40` `@click="item.stock > 0 && $emit('addToCart', item)"` |
| 先切分类再加菜 | 2 次（分类 1 + 菜品 1） | `MenuSection.vue:6-14` |
| 加 1 个菜后如何确认成功？ | **无任何反馈**：无 toast、无角标、无缩放动画、无数量浮标 | `MenuSection.vue:374-396` 的 :active 只有 `transform: translateY(0)`；`useCart.ts:43-71` 无 UI 反馈；成功提示仅存在于异常分支（`:45,50,57`） |
| 扫码枪加 1 个商品 | **不可能（0 入口）** | 见维度一 K-2 |

**加菜的核心问题不是点击次数（1 次已属优秀），而是「没有确认感」→ 收银员会多看一眼或多点一下，实际成本 ≈ 2 次点击 + 1 次视觉扫视。**

#### 2）改数量（3 → 5）

| 路径 | 交互 | 证据 |
|---|---|---|
| 点数字进编辑 | 1 次点击（热区仅 24px）→ 键盘 5（1 次）→ Enter 或失焦（1 次）= **2 次点击 + 1 次键盘** | `CartSection.vue:54-71`；`useCart.ts:89-107` |
| 连点「+」两次 | **2 次点击** | `CartSection.vue:73-78` |

#### 3）删一行

| 路径 | 交互 | 证据 |
|---|---|---|
| PC 垃圾桶 | 1 次（**须先 hover 才出现**，默认 `opacity:0`） | `CartSection.vue:448-453`、`:333-335`、`:80-87` |
| 触摸左滑 | 滑动（1 次手势）+ 点「删除」（1 次）= **2 次**，且**无确认** | `CartSection.vue:93-99`；`useCart.ts:146-165` |
| 「−」到底 | 1 次点击（**静默删除**） | `useCart.ts:84-86` |

#### 4）结账（现金）——最关键的路径

`CartSection.vue:132` 现金 → `Home.vue:36` quickPayment('cash') → `usePayment.ts:35-56` 打开弹窗 → `PaymentConfirmDialog.vue:82-90`「确认支付」→ `Home.vue:56` handlePaymentConfirm → `usePayment.ts:150-167` → confirmPayment() → **`:69-79` 又弹一个 ElMessageBox.confirm（确认支付 ¥X 元？）** → API → PaymentSuccessDialog → `Home.vue:65` 完成。

| 步骤 | 层级 | 点击 |
|---|---|---|
| 1 | 购物车底部「现金」 | 1 |
| 2 | 弹窗「确认支付」 | 1 |
| 3 | **ElMessageBox「确认支付」（重复确认）** | 1 |
| 4 | 支付成功弹窗「完成」（`close-on-click-modal=false`、`close-on-press-escape=false`，**必须点**，无自动关闭） | 1 |
| 5 | 进入下一单（无自动聚焦、无就绪态） | 0 |
| **合计** | **4 次点击 / 3 层弹层 / 2 次「确认支付」** | **4** |

证据：`CartSection.vue:132-134`；`usePayment.ts:35-56,58-84`（第 3 步双确认在 :69-79）；`PaymentSuccessDialog.vue:2-11,39-41`。

**阻塞性**：支付成功弹窗为模态、禁点遮罩关闭、禁 ESC（`PaymentSuccessDialog.vue:7-8`），**在收银员点「完成」之前下一单无法开始**——这是整条链路唯一真正的阻塞点，且每单强制 1 次点击。

**扫码支付路径更差**：非现金方式 `PaymentConfirmDialog.vue:87` `:disabled="selectedPaymentMethod !== 'cash' && !paymentCode"`，**没扫付款码按钮一直灰着**；文案却说「扫码枪扫描付款码后会自动填充并确认支付」（`:74`），代码里没有自动确认逻辑（只有 `:55` 的 `@keyup.enter`）。

#### 5）挂单 / 取单 / 券码

| 流程 | 点击 | 证据 |
|---|---|---|
| 挂单 | 2（顶栏挂单 + 确认挂单） | `PosTopBar.vue:25-28`；`HoldOrderDialog.vue:51-53` |
| 取单 | 3（顶栏取单 + 卡片 + ElMessageBox 确定） | `PosTopBar.vue:30-35`；`RetrieveOrderDialog.vue:29-34`；`useHangingOrder.ts:52-56` |
| 券码 | 3（顶栏券码 + 验证/Enter + 应用优惠券） | `PosTopBar.vue:46-49`；`CouponDialog.vue:13,38-40` |

#### 6）菜单加载与分类切换的性能 / 交互反馈

- **骨架屏有**：`MenuSection.vue:86-94` + 动画 `:554-573`（`skeleton-loading 1.5s`），仅在首次/非静默加载时出现（`useMenu.ts:71-75`）。
- **分类切换零延迟**：`Home.vue:45` 直接改 activeCategory，`useMenu.ts:18-59` computed 端过滤——无 loading、无骨架、无过渡（不闪烁，但也没有「正在筛选」的反馈）。
- **无防抖**：WebSocket 菜单变更直接全量重载——`Home.vue:507-509` `handleMenuUpdate = () => loadMenu(true)`，无 debounce；后端一次批量更新 N 个菜品会触发 N 次「3 接口并行」（`useMenu.ts:78-82`）。
- **深比较开销**：`useMenu.ts:94-96` 三次 `JSON.stringify` 全量比对（菜品 + 套餐 + 分类），菜单 500+ 时每次刷新都要序列化两遍。
- **重复过滤**：`useMenu.ts:55-57` 已按分类过滤，`MenuSection.vue:172-178` 又 filter 一次（无害，但说明职责不清）。
- **无虚拟滚动 / 分页**：`MenuSection.vue:328-354` 一次性渲染全部 filteredItems；7 列固定网格（:332）不随宽度自适应。
- **无搜索 / 无拼音首字母检索**：`MenuSection.vue:1-96` 无输入框，收银员找菜只能「切分类 + 视觉扫描」。
- 静默刷新**不覆盖**已有数据（`useMenu.ts:112-117` 的 `if (!silent)` 保护）——这点做对了，值得保留。

### 缺陷分级

| 编号 | 缺陷 | 级别 | 证据 |
|---|---|---|---|
| E-1 | 结账链路 4 次点击 + 2 次重复「确认支付」（弹窗 + ElMessageBox） | **P1** | `usePayment.ts:58-84`；`PaymentConfirmDialog.vue:82-90` |
| E-2 | 支付成功弹窗强制点击「完成」才能开下一单（阻塞、无自动关闭） | **P1** | `PaymentSuccessDialog.vue:7-11,39-41` |
| E-3 | 加购无任何成功反馈，收银员需二次确认（隐性成本 ≈ +1 动作/菜） | **P1** | `MenuSection.vue:374-396`；`useCart.ts:43-71` |
| E-4 | 非现金支付未扫付款码时无法确认，且文案声称「自动确认」但未实现 | **P1** | `PaymentConfirmDialog.vue:55,74,87` |
| E-5 | 菜单变更无防抖，一次批量更新触发 N 次全量重载 + 3 次 JSON 深比较 | **P2** | `Home.vue:507-509`；`useMenu.ts:78-82,94-96` |
| E-6 | 无菜品搜索/拼音检索，找菜只能切分类目视 | **P2** | `MenuSection.vue:1-96` |
| E-7 | 7 列固定网格 + 全量渲染，无虚拟滚动/响应式列数 | **P2** | `MenuSection.vue:328-354` |
| E-8 | 分类切换无「筛选中」过渡反馈（体感尚可，大菜单无兜底） | **P2** | `Home.vue:45`；`useMenu.ts:18-59` |

### 影响（收银员一句话）

> 「一单现金我得点四下，中间还要在两个长得一模一样的『确认支付』框里各点一次；点完还得再点一下『完成』，不然下一单开不了——排队三个人我就慌。」

---
## 投入产出比最高的 5 条改进建议

> 收益口径统一按 **300 单/天**、平均 **5 个菜/单**（1500 次加菜/天）估算；人时为一人熟悉本仓库前提下的净编码 + 自测。

### 建议 1：收银端全局快捷键基座（复用后厨 useKeyboardShortcuts）+ 输入焦点保护

- **改动点**：新增 `frontend-pos/src/composables/pos/usePosShortcuts.ts`（照搬 `kitchen/useKeyboardShortcuts.ts:132-209` 的分发与注册骨架，保留 `:134-142` 的「输入框内不触发」排除）；在 `views/Home.vue:528-582` 的 `onMounted` 注册 F1 挂单 / F2 现金结账 / F3 取单 / Delete 删当前行 / ESC 关顶层弹窗。
- **工作量**：**8 人时**（含与正在改 Home.vue 的 Lead 排期、冲突处理）。
- **预期收益**：结账路径少 2 次鼠标移动 + 点击 → 每单省约 1.2s；挂单/取单从「移到顶栏点击」变成单键，按 40 次/天计省 80 次点击；**保守按每单省 2 次点击 → 600 次/天**，午餐高峰（120 单）累计省约 2.4 分钟纯操作时间。
- **附带收益**：顺带修掉 K-1（Enter 双触发）——把 `Home.vue:574-581` 的裸监听替换为带输入焦点判断的版本。

### 建议 2：结账链路「三层并一层」+ 幂等令牌

- **改动点**：`composables/usePayment.ts:58-84` 删除 `ElMessageBox.confirm` 二次确认（PaymentConfirmDialog 已是确认层）；`components/pos/dialogs/PaymentSuccessDialog.vue:2-11` 增加 3s 自动关闭（保留手动「完成」）；`usePayment.ts:64-66` 幂等标志改为**进函数即置 true**，并加 `orderRequestId`（时间戳 + 随机）随 `createOrder` 上送。
- **工作量**：**5 人时**。
- **预期收益**：每单 4 次点击 → 2 次，**省 600 次点击/天**；结账耗时约 6s → 3.5s，高峰单笔吞吐 +40%；**同时消除重复下单/重复扣款（0 容忍项）**。

### 建议 3：删除类操作改为「5 秒撤销条」，替换模态确认

- **改动点**：`composables/useCart.ts:81-87`（减到 0）、`:194-196`（滑删/垃圾桶删）改为「先移入 `pendingDelete`，展示 5s 撤销条」；新增 `components/pos/CartUndoBar.vue`；`Home.vue:29-35` 增绑 undo 事件；`CartSection.vue:206-217` 的清空仍保留模态确认。
- **工作量**：**6 人时**。
- **预期收益**：按 **2% 误删率**（300 单 × 5 行 = 1500 行/天 → 约 30 行误删/天），每行重录约 4 次点击 + 8 秒 → **省 120 次点击与约 4 分钟/天**，更重要的是消除「客人面前重新点单」的体验事故；且不打断节奏（比模态确认更快）。
- **注意**：不要用 `ElMessageBox` 做删除确认——高峰期会变成「每删一个菜都弹窗」的新瓶颈。

### 建议 4：折扣状态机化（随车重算 + 手动整单折扣/抹零入口）

- **改动点**：`views/Home.vue:175-176` 把 `discountAmount` 从 ref 改为 computed（依据 appliedCoupon + originalTotalAmount，低于 minAmount 自动归零并 toast）；`Home.vue:437-441` 的门槛校验抽为 `validateCoupon()` 供重算复用；新增 `components/pos/dialogs/DiscountDialog.vue`（整单减免/抹零/折扣率三选一 + 数字键盘 + 二次确认）；`CartSection.vue:117-120` 增加「手动折扣」行与来源标识。
- **工作量**：**10 人时**（含后端是否支持 discountAmount 落库的对接确认）。
- **预期收益**：**直接消灭 R-3 的错金额风险（P0）**；并把「抹零」从「做不到」变成 1 次点击——按 15% 订单需抹零（45 单/天）、此前只能挂券或放弃（实际往往直接放弃 = 顾客体验损失）计，**45 单/天从 0 支持变为 1 次点击完成**。

### 建议 5：常驻扫码枪商品码输入框 + 全局焦点归位

- **改动点**：`components/pos/MenuSection.vue:1-16` 分类栏右侧插入常驻 `<input class="scan-input">`（autofocus，回车/失焦自动重新聚焦）；新增 `composables/pos/useScanGun.ts`：50ms 内连续字符 + Enter 判定为扫码枪输入 → 按 dishCode 精确匹配（`useMenu.ts:33` 已把 id 设为 dishCode，可直接匹配）→ 命中则 addToCart + 角标反馈，未命中弹「未找到该商品码」；`Home.vue:622-637` 的 car watch 内加 nextTick 焦点归位。
- **工作量**：**12 人时**（含键盘误判调参与匹配兜底）。
- **预期收益**：条码商品（饮料/包装食品，快餐场景约占 **1/3 订单 = 100 单/天**）从「切分类 + 目视找图 + 点击」（约 4 次交互、8 秒）→ **1 次扫码、1.5 秒**：**省约 300 次点击与 11 分钟/天**，并彻底消除「扫了没反应」的挫败感。

---
## 代码证据索引

> 路径前缀统一为 `frontend-pos/src/`；「合格 / 对照」为正面证据，便于后续改动时不误伤。

| # | 主题 | 文件:行号 | 关键内容 | 级别 |
|---|---|---|---|---|
| 1 | 支付弹窗 Enter 双触发（源 A） | `frontend-pos/src/views/Home.vue:574-581` | 捕获阶段 keydown，仅判 Enter && showPaymentConfirm，无输入焦点排除 | P0 |
| 2 | Enter 双触发（源 B） | `frontend-pos/src/components/pos/dialogs/PaymentConfirmDialog.vue:30` | 现金输入框 @keyup.enter="handleConfirm" | P0 |
| 3 | 幂等守卫失效 | `frontend-pos/src/composables/usePayment.ts:58-84` | isConfirming 在 await 之后才置 true | P0 |
| 4 | 减到 0 静默删行 | `frontend-pos/src/composables/useCart.ts:81-87` | quantity>1 ? -- : splice | P0 |
| 5 | 删除无确认（逻辑层） | `frontend-pos/src/composables/useCart.ts:194-196` | swipeDeleteItem 直接 splice | P0 |
| 6 | 删除无确认（PC 按钮） | `frontend-pos/src/components/pos/CartSection.vue:80-87` | @click.stop="$emit('delete', index)" | P0 |
| 7 | 删除无确认（触摸滑动） | `frontend-pos/src/components/pos/CartSection.vue:93-99` | 滑动删除按钮直接 emit | P0 |
| 8 | 清空购物车有确认（正面） | `frontend-pos/src/components/pos/CartSection.vue:206-217` | ElMessageBox.confirm | 合格 |
| 9 | 折扣不随车重算 | `frontend-pos/src/views/Home.vue:175-176` | totalAmount = max(0, original - discount)，discount 为死值 | P0 |
| 10 | 折扣仅在应用时写死 | `frontend-pos/src/views/Home.vue:437-461` | minAmount 只校验一次；:461 赋值后再无重算 | P0 |
| 11 | 取单覆盖购物车 | `frontend-pos/src/composables/useHangingOrder.ts:50-68` | selectHeldOrder 直接 cart.value = items | P0 |
| 12 | 护栏函数未被使用 | `frontend-pos/src/composables/useHangingOrder.ts:84-95` | quickRestoreHeldOrder 全仓无调用点 | P0 |
| 13 | 取单事件绑定 | `frontend-pos/src/views/Home.vue:78-82` | 只绑 @select="selectHeldOrder" | P0 |
| 14 | 实收现金/付款码未上送 | `frontend-pos/src/composables/usePayment.ts:112-124,158-164` | payOrder 只用 orderId/method/amount | P0 |
| 15 | 支付契约缺字段 | `frontend-pos/src/api/posApi.ts:223-230` | PayRequest 无 paymentCode / cashReceived | P0 |
| 16 | 订单硬编码桌号 | `frontend-pos/src/composables/usePayment.ts:98` | tableNumber: 1 | P0 |
| 17 | 明细原价 vs 总额折后 | `frontend-pos/src/composables/usePayment.ts:88-99` | items.price 原价、totalAmount 折后 | P0 |
| 18 | 退款预填 + Enter 即退 | `frontend-pos/src/views/Home.vue:400-413` | inputValue:'用户申请退款'，无金额校验 | P1 |
| 19 | 退款无部分退款能力 | `frontend-pos/src/api/posApi.ts:233-238` | RefundRequest 仅 orderNumber + refundReason | P1 |
| 20 | 券码不可回滚 | `frontend-pos/src/views/Home.vue:444-474` | 应用即核销；removeCoupon 只清本地 | P1 |
| 21 | 无离线横幅 | `frontend-pos/src/views/Home.vue:546-559` | onConnectionChange 只改刷新定时器 | P1 |
| 22 | 连接状态能力闲置 | `frontend-pos/src/utils/websocket.ts:196-198` | isConnectedStatus() 无调用方 | P1 |
| 23 | 后厨有断线文案（对照） | `frontend-pos/src/views/kitchen/KitchenHome.vue:324` | reconnecting: '重连中...' | 对照 |
| 24 | 生产环境日志黑洞 | `frontend-pos/src/utils/logger.ts:22-24` | if (!isDev) return | P1 |
| 25 | 静默吞错 A | `frontend-pos/src/views/Home.vue:275-276` | 叫号统计 catch {} | P1 |
| 26 | 静默吞错 B | `frontend-pos/src/views/Home.vue:377-381` | 查询失败 → 空列表 | P1 |
| 27 | 静默吞错 C | `frontend-pos/src/composables/useCart.ts:218-220` | 购物车恢复失败仅 logger | P1 |
| 28 | 30s 超时 | `frontend-pos/src/api/request.ts:4-7` | timeout: 30000 | P1 |
| 29 | 401 无提示跳登录 | `frontend-pos/src/api/request.ts:75-82,142-143` | clearAuthAndRedirect() | P2 |
| 30 | 错误拒绝为裸对象 | `frontend-pos/src/api/request.ts:96,147-149` | 提示责任下放调用方 | P2 |
| 31 | 收银端仅 1 个快捷键 | `frontend-pos/src/views/Home.vue:526,574-581` | 唯一全局绑定 | P1 |
| 32 | 后厨快捷键体系（对照） | `frontend-pos/src/composables/kitchen/useKeyboardShortcuts.ts:56-127,183-209` | 13 条快捷键 + 注册/注销 | 对照 |
| 33 | 后厨输入焦点排除（对照） | `frontend-pos/src/composables/kitchen/useKeyboardShortcuts.ts:134-142` | isInputFocused 判断 | 对照 |
| 34 | 菜品区无任何输入框 | `frontend-pos/src/components/pos/MenuSection.vue:1-96` | 仅分类栏 + 网格 + 骨架 | P1 |
| 35 | 加菜仅 1 次点击（正面） | `frontend-pos/src/components/pos/MenuSection.vue:30-40` | @click → addToCart | 合格 |
| 36 | 加购无成功反馈 | `frontend-pos/src/components/pos/MenuSection.vue:374-396` | 仅 :active translateY(0) | P1 |
| 37 | 菜品字号 11/13/10px | `frontend-pos/src/components/pos/MenuSection.vue:399-410,423-433,493-520` | 品名 11、价格 13、角标 10 | P1 |
| 38 | 7 列固定网格 | `frontend-pos/src/components/pos/MenuSection.vue:328-334` | repeat(7, 1fr) | P2 |
| 39 | 骨架屏存在（正面） | `frontend-pos/src/components/pos/MenuSection.vue:86-94,554-573` | 8 个占位 + 1.5s 动画 | 合格 |
| 40 | 分类胶囊 30px 高 | `frontend-pos/src/components/pos/MenuSection.vue:269-276` | padding:8px 20px; font-size:14px | P2 |
| 41 | 数量数字热区 24px | `frontend-pos/src/components/pos/CartSection.vue:409-421` | 无 padding / 无高度 | P1 |
| 42 | 数量输入框 28px 高 | `frontend-pos/src/components/pos/CartSection.vue:423-425` | width:50px;height:28px | P1 |
| 43 | 加减按钮 44px（正面） | `frontend-pos/src/components/pos/CartSection.vue:368-381` | min-width / min-height: 44px | 合格 |
| 44 | 清空按钮 small | `frontend-pos/src/components/pos/CartSection.vue:10-19` | size="small" text | P2 |
| 45 | PC 删除按钮须 hover | `frontend-pos/src/components/pos/CartSection.vue:448-453,333-335` | opacity:0，hover 才显 | P2 |
| 46 | 应收金额 26px | `frontend-pos/src/components/pos/CartSection.vue:537-541` | font-size:26px;font-weight:800 | 合格 |
| 47 | 金额标签 muted 色 | `frontend-pos/src/components/pos/CartSection.vue:508-511` | color: var(--pos-text-muted) = #94a3b8 | P1 |
| 48 | 顶栏按钮 40px / 13px | `frontend-pos/src/components/pos/PosTopBar.vue:368-385` | 9 个按钮同一规格 | P1 |
| 49 | 支付弹窗金额 32px | `frontend-pos/src/components/pos/dialogs/PaymentConfirmDialog.vue:210-214` | font-size:32px | 合格 |
| 50 | 扫码支付禁用条件 | `frontend-pos/src/components/pos/dialogs/PaymentConfirmDialog.vue:87` | 无付款码 → 按钮 disabled | P1 |
| 51 | 文案称自动确认但未实现 | `frontend-pos/src/components/pos/dialogs/PaymentConfirmDialog.vue:55,74` | 实际只有 keyup.enter | P1 |
| 52 | 成功弹窗强制点击 | `frontend-pos/src/components/pos/dialogs/PaymentSuccessDialog.vue:2-11,39-41` | 禁遮罩/ESC 关闭，仅「完成」 | P1 |
| 53 | 双确认（第 2 层） | `frontend-pos/src/composables/usePayment.ts:69-79` | ElMessageBox.confirm('确认支付…') | P1 |
| 54 | 色彩令牌 | `frontend-pos/src/styles/pos/variables.css:17-26` | #94a3b8 / #ea580c / #f8fafc | P1 |
| 55 | 全局 tap-highlight / touch-action（正面） | `frontend-pos/src/styles/index.css:5,16,20-33,52-64` | 已禁高亮与双击缩放 | 合格 |
| 56 | 双击缩放兜底（正面） | `frontend-pos/src/main.ts:22-35` | 300ms 双判，放行 .qty-number | 合格 |
| 57 | 触屏判定不一致 A | `frontend-pos/src/composables/useCart.ts:37-41` | ontouchstart && maxTouchPoints | P1 |
| 58 | 触屏判定不一致 B | `frontend-pos/src/components/pos/CartSection.vue:81,188` | 'ontouchstart' in window 控制删除按钮可见性 | P1 |
| 59 | 触屏判定不一致 C | `frontend-pos/src/composables/kitchen/useKeyboardShortcuts.ts:28-36` | 三条件 || | P1 |
| 60 | 死样式文件 | `frontend-pos/src/styles/pos/components/dialog.css:27-34,63-69` | 全仓无 @import；main.ts:9-12 未导入 | P1 |
| 61 | 菜单三接口并行（正面） | `frontend-pos/src/composables/useMenu.ts:78-82` | Promise.all | 合格 |
| 62 | JSON 深比较开销 | `frontend-pos/src/composables/useMenu.ts:94-96` | 3 次 JSON.stringify 全量对比 | P2 |
| 63 | 静默刷新保护（正面） | `frontend-pos/src/composables/useMenu.ts:112-117` | if (!silent) 才覆盖错误态 | 合格 |
| 64 | WS 菜单刷新无防抖 | `frontend-pos/src/views/Home.vue:507-509` | 直接 loadMenu(true) | P2 |
| 65 | 挂单仅本地存储 | `frontend-pos/src/stores/hanging-order.ts:1-7,122-132` | localStorage，单终端 | P1 |
| 66 | 挂单清空无服务端 | `frontend-pos/src/stores/hanging-order.ts:198-201` | removeItem 而已 | P2 |
| 67 | 数量夹取（正面） | `frontend-pos/src/composables/useCart.ts:101-107` | min(999, max(1, floor(x)||1)) | 合格 |
| 68 | 购物车持久化（正面） | `frontend-pos/src/composables/useCart.ts:205-228` | localStorage 恢复 + 深 watch | 合格 |
| 69 | 副屏同步 watch | `frontend-pos/src/views/Home.vue:622-637` | 只同步展示，无折扣/校验 | P2 |
| 70 | 菜单切换零反馈 | `frontend-pos/src/views/Home.vue:45` | activeCategory = $event | P2 |

---

## 附：本次审计未覆盖（需后续补）

1. `StartShiftDialog.vue` / `ShiftHandover.vue` / `Home.vue` / `PosTopBar.vue` 正由 Lead 并行修改，本文只读当前快照，**冲突处以上线版本为准**。
2. 真机性能（1366×768 收银一体机 + 电容屏）未实测，热区与字号结论基于 CSS 静态推算。
3. 后端配合项（PayRequest 是否能扩展到 paymentCode / cashReceived、tableNumber 是否必填）未核对后端代码。
4. 未执行 build，未做运行时验证；本文所有结论均为静态代码取证。
