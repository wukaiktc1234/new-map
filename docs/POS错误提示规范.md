# POS 错误提示规范（第二十八轮定稿）

> 一句话：**有操作点的地方，错误就地显示；没有操作点的地方，才允许全局弹窗。**

## 1. 判定规则

| 场景 | 判定 | 呈现方式 | 实现 |
|---|---|---|---|
| 登录失败 / 无权限 / 限流 | 用户正看着表单 | 表单内红色 `el-alert` | `Login.vue` `error.value` |
| 交班失败、备用金保存失败 | 用户刚点按钮 | 按钮上方内联错误条 | `data-test="shift-action-error"` |
| 作废 / 补收 / 409 冲突 | 用户在抽屉内操作 | 抽屉顶部内联错误条 | `data-test="exc-action-error"` |
| 账目过期 / 无权限读统计 | 面板内 | 面板顶部横幅 | `shift-stats-stale` / `shift-stats-forbidden` |
| 后台轮询失败（15s 异常单刷新、stats 刷新、扫码轮询） | 用户没点任何东西 | **不打扰**：写入状态位，由面板自己显示 | `silent: true` |
| 纯成功反馈（已作废、已补收） | 无 | Toast（允许） | `ElMessage.success` |

## 2. 拦截器层（`api/request.ts`）

```ts
declare module 'axios' {
  interface AxiosRequestConfig { silent?: boolean }
}

const AUTH_SILENT_PATTERNS = ['/v1/pos/auth/login', '/v1/pos/auth/me']
export function isSilentRequest(config?: AxiosRequestConfig): boolean {
  if (config?.silent) return true
  const url = String(config?.url || '')
  return AUTH_SILENT_PATTERNS.some((p) => url.includes(p))
}
```

- 登录接口 **强制 silent**：401/403/429 一律只 `reject`，不弹 toast、不刷新 token、不跳转；
- `reject` 结构统一为 `{ message(中文), status, retryAfter?, silent, originalError }`；
- 429 会把 `Retry-After` 或 body 的 `retryAfter` 带出来，供登录页做倒计时；
- 所有英文网络错误经 `toChineseNetworkMessage()` 汉化后才允许进入 UI。

## 3. 已改造清单（本轮）

| 文件 | 改动 |
|---|---|
| `api/request.ts` | silent 选项 + 登录自动静默 + 401 不跳转 + 429 带 retryAfter + 汉化 |
| `api/shiftExceptions.ts` | `getStats` / `listExceptions` 标 `silent`（后台轮询） |
| `views/Login.vue` | 只保留内联错误；429 → 48s 倒计时禁用按钮；预检加 `silent` |
| `views/ShiftHandover.vue` | 交班/备用金失败 → 内联错误条；60s 过期横幅 |
| `components/pos/shift/ExceptionDrawer.vue` | 作废/补收/409 → 抽屉内联错误条 |
| `components/pos/shift/{Void,Resolve}Dialog.vue` | 草稿暂存 + 扫码开关 |

## 4. 待迁移清单（不影响主流程，按优先级排）

以下文件的 `ElMessage.*` 仍是全局 toast。它们都是**动作型**提示，按规范应迁到内联：

| 文件 | 调用数 | 建议落点 | 优先级 |
|---|---|---|---|
| `views/Home.vue` | 12 | 顶部 `shift-state` 条 / 结算区 | P1 |
| `views/Payment.vue` | 15 | 支付面板内联区 | P1 |
| `views/TableManagement.vue` | 18 | 桌台操作行内联 | P1 |
| `views/CallNumber.vue` | 15 | 叫号列表内联 | P2 |
| `composables/useHangingOrder.ts` | 9 | 挂单列表面板 | P2 |
| `composables/usePayment.ts` | 8 | 同上（支付） | P2 |
| `composables/useCart.ts` | 7 | 购物车行内联 | P2 |
| `views/YoloCheckoutDialog.vue` | 9 | 弹窗内联 | P2 |
| `views/KitchenHome.vue` / `KitchenOrderCard.vue` | 13 | 卡片内联 | P2 |
| `views/CustomerOrder.vue` / `Scan.vue` / `CouponDialog.vue` 等 | 16 | 就近内联 | P3 |

> 迁移方式已统一：`catch` 里写 `xxxError.value = resolveErrorMessage(error, '兜底')`，模板加 `data-test="*-action-error"`；轮询类一律 `silent: true`。

## 5. 验收断言

`test-review/pos-round28-verify.cjs`：
- A1 401 只在表单内提示，无全局弹窗；
- B1 429 后按钮禁用并显示「47 秒后可重试」；B2 无全局弹窗；B3 秒数递减；
- C1 抽屉初始无残留错误条；
- round21 F1/F2：409 走抽屉内联，且不再弹 toast。
