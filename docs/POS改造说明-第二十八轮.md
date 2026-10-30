# 第二十八轮：登录提示去重 + 429 倒计时 + 全站提示规范

> 18 套件 **302 项断言全绿**；本轮改动 10 文件 +317/−61。

## 1. 登录页只保留内联提示

- 拦截器新增 `silent` 选项，`/v1/pos/auth/login`、`/v1/pos/auth/me` **强制静默**：
  401 / 403 / 429 只 `reject`，不弹 toast、不刷新 token、不跳转；
- `Login.vue` 捕获后只写 `error.value`，渲染在登录按钮上方的红色提示里；
- 顶部 toast 彻底移除（图 1 的两处提示 → 一处）。

## 2. 429 倒计时防呆

- 429 时拦截器把 `Retry-After`（或 body.retryAfter）写进 reject；
- 登录页 `startRetryCountdown()`：默认 **48 秒**，每秒递减；
- 按钮 `:disabled="retryLeft > 0"`，文案变成「47 秒后可重试」；
- 键盘 Enter 也被拦住（`handleLogin` 首行判断）。

断言：B1 禁用 + 倒计时、B2 无 toast、B3 秒数递减。

## 3. 全站排查结论

规则与清单见 `docs/POS错误提示规范.md`。

**本轮已迁移到内联**：登录、交班、备用金、作废、补收、409 冲突、账目过期、无权限读统计。
**后台轮询**（异常单 15s、stats 刷新、扫码查询）统一 `silent: true`，失败只写状态位，由面板自己展示。
**待迁移**：Home / Payment / TableManagement 等 10 个文件共约 122 处动作型 toast，已给出落点与优先级（P1~P3），下一轮按 P1 开始。

## 4. 回归

18 套件 302 项全绿：round2 43 / retrieve 30 / round21 26 / round12 25 / round18 16 / visual 16 / hold 16 / round13 15 / round8 15 / round27 15 / round24 14 / order-query 14 / round20 13 / round15 12 / round26 11 / round16 9 / round28 5 / negative 5。

截图：round28-login-inline.png、round28-login-429.png。
