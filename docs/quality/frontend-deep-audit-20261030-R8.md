# R8 全栈前端深度体检报告（契约一致性 / 业务闭环 / 高压可用性）

> **产物编号**：AUDIT-FULLCHAIN-POS-20261030-R8（前端专项）
> **靶对象**：仓库真实前端代码（用户侧切片仍为占位符，按纪律以 `frontend-pos/src` 为准）
> **方法**：request 拦截器 / API 客户端 / 交班 store / shift 组件四件套 / 扫码轮询 / 路由守卫逐行审计；
> 关键结论均给出「文件：行号」与修复代码。

---

## 一、P0 级断头路与逻辑死结

### F-P0-1 扫码补收链路：前端完整 UI × 后端无通路（双端契约脱节）

- **代码定位**（三件套）：
  1. 前端客户端：`frontend-pos/src/api/shiftExceptions.ts:123-128`
     ```ts
     createScanPayment: POST `${BASE}/exceptions/${orderId}/scan`   // → /v1/pos/shift/exceptions/{id}/scan
     queryScanStatus:  GET  `${BASE}/scan/${payNo}`                 // → /v1/pos/shift/scan/{payNo}
     ```
  2. 前端 UI：`ResolveDialog.vue:38-58`（完整扫码 Tab：生成码 → 180px 码框 → `剩余 Ns` 倒计时），下沉轮询 `useScanPaymentPolling`（2s 间隔 / 120s 超时 / 连续 3 次失败中止）
  3. 后端：`PosShiftController.java:109-117` 只存在 `POST /v1/pos/shift/{orderId}/scan`（**路径不同**）且**真 501 未实现**；前端调用的两个路径**不存在任何 mapping**
- **攻击/触发（即真实使用）**：收银员在异常单补收款弹窗点「生成收款码」→ `405 Method Not Allowed`（该路径只有 GET mapping），体自然为空 → `request.ts` 拦截器抛 `Request failed with status code 405`（英文技术词）→ `resolveErrorMessage` 原样上屏 → **收银员看不懂「为什么扫不了码、下一步该干嘛」**。
- **闭环评断**：`ResolveDialog` 的 `onTimeout` 文案（「收款码已超时，请重新生成或改用现金」）说明产品目标里「扫码失败降级现金」存在，但**入口本身死**——降级只能靠用户猜。
- **修复（两段式，任选或同做）**：
  - **立即止损（前端）**：扫码 Tab 上线开关由后端能力探测决定——
    ```ts
    // shiftExceptions.ts
    export const SCAN_BACKEND_READY = false // 后端 /shift/exceptions/{id}/scan 未实现（501），上线前置 false
    // ResolveDialog.vue
    <el-tab-pane v-if="scanEnabled" label="扫码" name="scan">…
    // scanEnabled=false 时 Tab 隐藏，只留现金收款——不呈现「看了会死」的入口
    ```
  - **正路（后端）**：在 `/v1/pos/shift/exceptions/{orderId}/scan` 落地「生成待收码 + payNo」，`GET /v1/pos/shift/scan/{payNo}` 轮询状态（前端契约/轮询/降级代码已就绪，纯等后端）；方块码渲染建议直接换二维码库渲染 `qrContent`（当前 `.rd-qr-box` 只是文本框，**给顾客扫的「码」不可扫**——属另一处断头，若上线即炸）。

---

## 二、P1 级契约脱节与体验缺陷

### F-P1-1 刷新时的权限恢复：单发无重试，弱网误踢登录（维度 1）

- **代码定位**：`router/index.ts:126-137`（`restoreCapabilities()` 失败一律 `next('/login?reason=capabilities')`）× `usePermission.ts:84-99`（`try { …me… } catch { return false }` 单发）。
- **场景**：收银员刷新页面恰逢 WiFi 抖动 / 后端网关 502 → `/v1/pos/auth/me` 失败 1 次 → **token 明明有效却被踢回登录页**。「刷新强制登出」的老问题以「弱网形态」复活。
- **修复代码**：
  ```ts
  // usePermission.ts
  export async function restoreCapabilities(): Promise<boolean> {
    if (capabilities.value.loaded) return true
    for (let attempt = 1; attempt <= 3; attempt++) {
      try {
        const me = await request.get('/v1/pos/auth/me')
        setCapabilities(me.permissions, me.roles); return true
      } catch (e) {
        if (isAuthFailure(e)) { clearAuthAndRedirect(); return false }   // 401/403/401n：真踢
        if (attempt < 3) await delay(300 * attempt)                       // 网络类：退避重试
      }
    }
    throw { kind: 'network' }   // 三连败后向上抛，守卫跳 /login?reason=network 且**不清 token**
  }
  ```
  登录页针对 `reason=network` 显示「网络异常，已保留登录，网络恢复后重试」。

### F-P1-2 弱网/断开的报错不分层，技术词直上屏（维度 1/4）

- **代码定位**：`request.ts:244-245`（非 HTTP 响应的 axios 错误 → `{ message: error.message }`，即 "Network Error"/"timeout of 30000ms exceeded"）；`errorMessage.ts:19` 仅透传。
- **后果**：断网点开抽屉 → 「异常单列表加载失败：Network Error」；POS 触屏收银员读不懂。
- **修复代码**：
  ```ts
  // request.ts response error 兜底前加入：
  if (!error.response) {
    const text = error.code === 'ECONNABORTED'
      ? '网络超时，请检查网络后重试'
      : '网络异常，请检查网络连接'
    return Promise.reject({ message: text, status: 0, isNetworkError: true })
  }
  ```

### F-P1-3 弹窗半途数据无任何暂存（维度 2）

- **代码定位**：`VoidDialog.vue:99-104`（每次打开重置 reason/remark）；`ResolveDialog.vue:123-130`（关闭重置实收输入）；全仓 `sessionStorage` 命中 0。
- **场景**：填到一半「其他（需备注）」作废原因 → 网络闪断被踢到登录页（F-P1-1）或误触遮罩 → **回头全部重填**。高压收银场景这是复发性折磨。
- **修复代码**（最小入侵的轮干 composable）：
  ```ts
  // composables/useDraft.ts
  export function useDraft<T>(key: string) {
    const KEY = 'pos-draft:' + key
    const load = (): T | null => { try { return JSON.parse(sessionStorage.getItem(KEY) || 'null') } catch { return null } }
    const save = (v: T): void => sessionStorage.setItem(KEY, JSON.stringify(v))
    const clear = (): void => sessionStorage.removeItem(KEY)
    return { load, save, clear }
  }
  // VoidDialog：watch([reason, remark]) → save({reason, remark})；成功提交或明确取消后 clear()
  ```

### F-P1-4 异常卡片契约缺字段：状态补全只做了一半（维度 1/2）

- **代码定位**：`ExceptionCard.vue:18`「开单员 {{ order.employeeName || '—' }}」+ `ExceptionOrder` 接口无 payAttempts/itemsSummary；后端 `PosAuthServiceImpl#listShiftExceptions` 的 SELECT（R6 已读）**根本不返回 employee_name / 支付尝试 / 商品摘要**。
- **后果**：卡片永远「开单员 —」；「卡片不能只写其他」的产品目标仅完成 typeLabel 徽标一级；收银员无法在一屏内判断「这单是谁开的、顾客试过什么支付」。
- **修复**：后端 SELECT 联表 employees（_name）+ `order_payment_records` 失败记录聚合 + `order_items` top2；前端 types 同步加字段，卡片渲染行已预留 meta 区。

### F-P1-5 交班按钮不看统计新鲜度（维度 2/信息一致性）

- **代码定位**：`shiftHandover.ts:54-56` `canEndShift = !hasPending && stats !== null && !statsForbidden` —— **未包含 `statsStale`（:63 60s 判据存在但没用）**。
- **场景**：统计 stop refresh 失败（F-P1-1/2 之后 stats 保留旧值）→ 按钮仍亮 → 面板显示的钱箱数字可能是 60s+ 前的快照 → 交班落库数字与面板对不上（后端会按同源重算，**钱不会错，但「收银员看到的账」会错**——对账困惑）。
- **修复代码**：
  ```ts
  const canEndShift = computed(() =>
    !hasPendingException.value && stats.value !== null && !statsForbidden.value && !statsStale.value)
  // ShiftHandover.vue：statsStale 时按钮 tooltip 加「账目超过 1 分钟未刷新，请先点重试」
  ```

### F-P1-6 触控尺寸不达标且域内不统一（维度 3）

- **代码定位**：`ExceptionCard.vue:150-153` `.exc-btn { min-height: 36px }`；对照 `styles/kitchen-touch.css` 厨房域全部 44–56px。
- **后果**：同一家店两套肌肉记忆；收银高峰戴手套/满手油污误触率上升（Element Plus 默认按钮 32px 高）。
- **修复**：`.exc-btn { min-height: 44px; padding: 0 18px; font-size: 14px }`（连带 ResolveDialog/VoidDialog 的 footer `el-button` 用 `size="large"` 或全局 `--el-component-size-large`）。

### F-P1-7 双口径金额依赖「文档+注释」约束，无运行时护栏（维度 1）

- **现状**：`stats`（元）/`exceptions`（分）双口径由 `pos-shift-panel-field-contract` 文档 + `shiftExceptions.ts` 注释 + `ExceptionCard:8` 注释约束；`fenToYuan` 显示链一致 ✓（`be6d0a5` 修过一次 100 倍错——**证明该雷真实炸过**）。
- **修复建议**：把「DOM 断言钩子」制成门禁：E2E 固定夹具（amount=2200 分）断言 `data-amount-fen="2200"` 且展示文本 `¥22.00`；任何双口径漂移 CI 红灯。
- 另：`request.ts:109` 兼容 `data.code === 200` 的历史口径仍全站放行——**遗留模糊**，应随「状态码口径清单」（62f1eb3 待办）一并收口。

---

## 三、修复建议与代码级规范（汇总速览）

| 编号 | 修什么 | 关键动作 |
|---|---|---|
| F-P0-1 | 扫码断头 | 短期 `SCAN_BACKEND_READY=false` 隐藏 Tab；正式在 `/shift/exceptions/{id}/scan` + `/shift/scan/{payNo}` 落地 |
| F-P1-1 | 弱网误踢 | restore 3 段退避重试 + 网络类失败不清 token + 登录页 reason=network 提示 |
| F-P1-2 | 网络文案 | axios 兜底「网络超时/网络异常」中文映射 |
| F-P1-3 | 草稿暂存 | `useDraft` sessionStorage（按 orderId+action 键），成功/明确取消 clear |
| F-P1-4 | 卡片契约 | 后端 SELECT 补 employee/payment attempts/top2 items；前端 types 同步 |
| F-P1-5 | 新鲜度闸 | canEndShift 纳入 statsStale + tooltip 提示 |
| F-P1-6 | 触控 | 收银域按钮统一 `min-height:44px`（对齐 kitchen-touch 域约定） |
| F-P1-7 | 单位护栏 | E2E data-amount-fen 断言进 CI；`code===200` 兼容收口 |

## 四、正面记分（如实）

`stores/shiftHandover.ts` 的三条铁律（账目唯一真相在服务端 / pendingExceptionCount 只认后端 / 交班开关只认 canEndShift）**端到端落实**；15s 轮询 + stats 三态分类（404/403/网络/无班次）+ `fallbackPending` 降级数据；409 统一「已被别人处理→提示+刷新」；`replayed` 区分重放/首次文案；429 Retry-After 头优先实现；`fenToYuan` 单位换算统一；`resolveErrorMessage` 原文优先；submitting 全链锁点；VoidDialog 强二次确认+不可撤回警示；扫码轮询「绝不乐观更新账目」+3 连失败容错+`onUnmounted` 清理；data-test 断言钩子齐全（EXE 可直接写 Playwright）。**这套前端的中层纪律是本系统六轮审计里最像样的一段**，问题集中在「契约末端接入」与「高压场景细节」。

## 五、五维覆盖对照

| 用户维度 | 结论 |
|---|---|
| 1 契约/字段/恢复 | F-P0-1（scan 双端脱节）、F-P1-4（卡片字段缺）、F-P1-7（双口径无护栏）、F-P1-1（恢复无重试） |
| 2 闭环/降级/留存 | 扫码降级文案有但入口死；草稿暂存缺失（F-P1-3）；交班/抽屉/按钮闭环合格（除 F-P0-1） |
| 3 可用性/触控 | F-P1-6（36px）；金额字号 16px/tabular-nums 达标；防连点 loading 全链达标 |
| 4 全局异常 | stats 三态不白屏 ✓；网络文案不分级（F-P1-2）；路由层白屏（R7 RT-7 环境态已恢复） |
| （附）心智 | 无「负数收入」展示（找零 Math.max(0) ✓、非现金单列块）；双口径提示文案权威 |
