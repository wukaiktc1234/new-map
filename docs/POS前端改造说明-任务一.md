# 任务一 · POS 收银端交互改造说明（修改前 / 修改后）

> 范围：仅 `frontend-pos/`（Vue 3 + TS + Element Plus）的**展示层与交互层**。
> 后端 `backend/` 零改动；接口契约未变（只调用既有端点）；所有敏感操作保留/新增二次确认。
> 验证：`npm run build`（vue-tsc + vite）通过；Playwright 端到端 20 项断言全绿；单次回车只产生 1 笔订单。
> 变更规模：10 个文件修改（+697 / −150），6 个文件新增。

---

## 一、开班备用金弹窗（图一）

| # | 需求 | 修改前 | 修改后 | 关键实现 |
|---|---|---|---|---|
| 1 | 终端信息脱敏 | 直接渲染原始串 `终端：pos-localhost-1793174348494`，收银员看不懂、还暴露内网主机名与终端指纹 | 渲染 `前台收银机A` 徽标，悬浮提示 `终端：前台收银机A（终端标识 ****8494）`，原始串不再上屏 | 新增 `src/utils/terminal.ts`：终端号 → 别名映射持久化在 `pos-terminal-alias`，同机刷新/跨页/跨班次稳定一致；排障只暴露尾 4 位 |
| 2 | 加宽弹窗 | `width="460px"` | `width="560px"`，金额输入 56px 高 / 30px 等宽数字，配 4 个快捷金额（¥0 / 200 / 500 / 1000，44px 热区） | `StartShiftDialog.vue` |
| 3 | 回车键确认 | 只能点「开班」按钮 | 输入框 `@keyup.enter.stop`、弹窗 `@keyup.enter`、外加窗口级兜底监听，三路统一收敛到同一个 `submit()`，`loading` 同步守卫防重复提交；底部有「回车键可直接开班」提示 | 同上 |
| 4 | 智能带出上一班备用金，无历史填 0 | 无历史时输入框为空且**禁止提交**，收银员被卡住 | 优先级：门店固定备用金（后端暂无来源，保留挂点）→ 上一班 `openingCash` → **默认 `0.00`**；提示文案区分三种来源 | `loadDefaults()` 的 `defaultSource: 'store-config' \| 'last-shift' \| 'zero'` |
| 5 | 开班状态持久化，刷新不再弹窗 | 每次进首页都打 `/v1/pos/auth/current-shift`，返回无班次就弹窗（刷新必弹） | 开班成功即写 `pos-shift-active`（含终端号 + 业务日期 + 备用金）；回到首页**先查本地标记，命中直接跳过、连请求都不发**；后端说「有班次」时也会回写标记 | 新增 `src/utils/shiftSession.ts`：`markShiftStarted / readShiftMark / hasActiveShiftMark / clearShiftMark`；脏数据、隔天标记自动失效 |
| 6 | 交班时清除标记 | — | `ShiftHandover.confirmHandover()` 交班成功后 `clearShiftMark()`；登录成功前、退出登录时同样清除 | `ShiftHandover.vue` / `Login.vue` / `PosTopBar.vue` |

### localStorage 键约定（新增）

| 键 | 写入时机 | 清除时机 | 内容 |
|---|---|---|---|
| `pos-shift-active` | 开班成功 / 后端已有班次 | 交班成功、退出登录、重新登录、跨天失效 | `{terminalId, shiftId, employeeId, employeeName, openingCash, startedAt, businessDate}` |
| `pos-terminal-alias` | 首次渲染终端名 | 不主动清除（设备级） | `{ "pos-localhost-1793174348494": "前台收银机A" }` |
| `pos-preferences` | 偏好设置变更 | 「恢复默认」 | `{enterConfirmCheckout, fontScale}` |

---

## 二、用户头像菜单（图二）

| # | 需求 | 修改前 | 修改后 |
|---|---|---|---|
| 1 | 静态展示 → 下拉菜单 | 头像旁是**静态 chip**（`cursor: default`）+ 一个独立的「退出」按钮，占顶栏宽度 | 头像 chip 变成 `<button>`（44px 热区、hover/展开态描边、可旋转箭头），点击弹出 `el-popover` 菜单；原独立「退出」按钮已移除，顶栏少一个按钮 |
| 2 | 菜单内容 | 无 | ① 用户信息区：头像 / 姓名 / 工号 + 角色 / 脱敏终端名；② 修改密码；③ 偏好设置；④ 退出登录（红色，置底） |
| 3 | 修改密码 | 无入口 | 新增 `ChangePasswordDialog.vue`：原密码 / 新密码 / 确认新密码，校验长度 6-32、新旧不同、两次一致；调用既有端点 `PUT /v1/auth/password`（管理端同款契约，未改后端）；成功后提示「需重新登录」并二次确认后跳登录页 |
| 4 | 偏好设置 | 无 | 新增 `PreferenceDialog.vue` + `usePosPreferences.ts`：深色模式、回车直接确认结账、界面缩放（标准/大，`zoom:1.08`，热区同步放大）、**高危操作二次确认（恒开启、置灰不可关，安全策略）** |
| 5 | 退出登录二次确认 | 有 `ElMessageBox.confirm` | 保留并强化：文案明确「将清理本机凭证与当前购物车草稿（挂单会保留）」；`closeOnClickModal: false`，避免误点遮罩确认 |
| 6 | 清理缓存 + 跳登录 | 只清 `pos-token / pos-user / pos-terminal` | 新增 `src/utils/posCache.ts#clearPosRuntimeCache()`：清凭证 + 终端 + **开班标记** + 购物车草稿 + 客显广播；**刻意保留** `heldOrders`（挂单是显式保存的单据，清掉=丢单）与设备级偏好；后端 `/v1/auth/logout` 拉黑 token 失败也照常本地清理，随后 `router.push('/login')` |

---

## 三、顺带修掉的 3 个缺陷（都在本次触达的代码路径上）

1. **POS 弹窗统一视觉规范是死代码**：`styles/pos/components/dialog.css`（129 行）全仓没有任何 `import`，导致弹窗按钮退回 Element Plus 默认蓝、关闭按钮 32px 规范不生效。现已由 `main.ts` 全局引入，并把选择器从 `.fastfood-pos .el-dialog` 改为 `body .el-dialog`——**`el-dialog` 默认 teleport 到 body，原选择器根本命中不到**；同时把底部按钮统一抬到 44px 触控热区。
2. **回车在结账弹窗里双触发 → 重复下单**：
   - 旧链路：`Home.vue` 捕获阶段 `keydown` 监听 + `PaymentConfirmDialog` 的 `@keyup.enter` 各触发一次 `confirmPayment()`，而幂等标志 `isConfirming` 被放在 `await ElMessageBox.confirm` **之后**才置 true → 两个「确认支付」框 → 点两次 → 两笔订单。
   - 修复：① `usePayment.confirmPayment` 进函数即同步置位守卫，取消/异常路径复位；② `PaymentConfirmDialog` 增加 `submitted` 一次性提交守卫；③ `Home.vue` 的全局监听增加「焦点在输入控件内则不接管」判断（与后厨端 `useKeyboardShortcuts.isInputFocused` 对齐）。
   - 验证：Playwright 单次回车 → 确认框 1 个 → `createOrder` 1 次 → `payOrder` 1 次（修复前为 2 / 2）。
3. **收银端个人偏好无承载**：新增 `usePosPreferences`，`main.ts` 在挂载前应用字体缩放，避免首屏跳动。

---

## 四、文件清单

**修改（10）**

| 文件 | 改动摘要 |
|---|---|
| `src/components/pos/dialogs/StartShiftDialog.vue` | 脱敏终端、560px、回车三路确认、默认 0、快捷金额、开班落盘、44px 按钮 |
| `src/components/pos/PosTopBar.vue` | 头像 → Popover 菜单、修改密码/偏好设置入口、退出清理缓存、终端别名 |
| `src/views/Home.vue` | 开班判断走本地标记快路径、Enter 监听排除输入控件、消费偏好开关、透传 `paying` |
| `src/views/ShiftHandover.vue` | 交班成功清 `pos-shift-active` |
| `src/views/Login.vue` | 登录前清开班标记（换人=新的资金责任起点） |
| `src/main.ts` | 引入 `dialog.css`；挂载前 `initPosPreferences()` |
| `src/styles/pos/components/dialog.css` | 选择器改为 `body .el-dialog`（teleport 兼容）、按钮 44px、暖橙主色 |
| `src/styles/pos/base.css` | 字体缩放规则、Popover 容器样式 |
| `src/composables/usePayment.ts` | 幂等守卫前置（P0 修复） |
| `src/components/pos/dialogs/PaymentConfirmDialog.vue` | 单次提交守卫、`paying` 由父层传入 |

**新增（6）**

| 文件 | 职责 |
|---|---|
| `src/utils/terminal.ts` | 终端别名映射 + 脱敏尾号 |
| `src/utils/shiftSession.ts` | 开班标记读写（含跨天失效） |
| `src/utils/posCache.ts` | 退出登录缓存清理（保留挂单与偏好） |
| `src/composables/usePosPreferences.ts` | 个人偏好持久化 + DOM 应用 |
| `src/components/pos/dialogs/ChangePasswordDialog.vue` | 修改密码（复用 `PUT /v1/auth/password`） |
| `src/components/pos/dialogs/PreferenceDialog.vue` | 偏好设置面板 |

---

## 五、验证证据

**1）构建**

```
> frontend-pos@1.0.0 build
> vue-tsc && vite build
✓ 1770 modules transformed.
✓ built in 5.97s          # exit code 0（vue-tsc 无类型错误）
```

**2）端到端（Playwright，headless Chromium，本地静态托管 dist + 接口 mock）**

脚本：`test-review/pos-ui-verify.cjs`（20 项）、`test-review/pos-payment-double-enter.cjs`（重复下单回归）

| # | 断言 | 结果 |
|---|---|---|
| 1 | 弹窗显示「前台收银机A」 | ✅ |
| 2 | 弹窗文本不含 `pos-localhost` / `1793` | ✅ |
| 3 | 弹窗宽度 ≥ 540px（实测 560px） | ✅ |
| 4 | 无历史时默认值 = 0.00 | ✅ |
| 5 | 回车提交开班（`shift/start` 调用 1 次） | ✅ |
| 6 | 提交后弹窗关闭 | ✅ |
| 7 | `pos-shift-active` 已写入且终端号匹配 | ✅ |
| 8 | **刷新页面不再弹窗** | ✅ |
| 9-12 | 头像菜单含用户信息 / 修改密码 / 偏好设置 / 退出登录 | ✅ |
| 13 | 修改密码弹窗可打开 | ✅ |
| 14-15 | 偏好设置可打开、缩放落到 DOM（`data-pos-font-scale=large`） | ✅ |
| 16 | 退出登录弹二次确认（文案含「确认退出登录」） | ✅ |
| 17 | 确认后跳转 `/login` | ✅ |
| 18 | 凭证 + 开班标记 + 购物车全部清空 | ✅ |
| 19 | 后端 `/v1/auth/logout` 被调用 | ✅ |
| 20 | 无 console error / pageerror | ✅ |
| R1 | 单次回车 → 确认框 1 个、`createOrder` 1 次、`payOrder` 1 次 | ✅（修复前 2/2/2） |

**3）视觉留证**

- 开班弹窗：`test-review/after-start-shift-dialog.png`
- 头像菜单：`test-review/after-account-menu.png`
- 偏好设置：`test-review/after-preferences.png`

---

## 六、风险与回滚

| 项 | 说明 |
|---|---|
| 兼容性 | 新增键读取失败一律回落默认值（`pos-shift-active` 脏数据 = 未开班，宁可多弹一次也不卡住收银） |
| 与后端的关系 | 开班仍以 `POST /v1/pos/auth/shift/start` 为准；本地标记只用于「跳过重复弹窗」，不参与任何金额计算 |
| 未改的行为 | `tableNumber: 1` 硬编码、折扣重算、取单覆盖购物车、实收现金上送——均涉及业务规则/后端契约，本任务不动，列入体验报告的建议与待办 |
| 回滚 | 全部改动集中在 `frontend-pos/src`，`git checkout -- frontend-pos/src` 即可整体回滚；无数据库/后端变更 |
