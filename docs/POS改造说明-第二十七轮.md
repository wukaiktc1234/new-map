# 第二十七轮：R8 审计问题歼灭（F-P0-1 扫码断头路 + 6 项 P1）

> 全部落地，17 套件 **296 项断言全绿**。

## 一、F-P0-1 扫码断头路

- 新增 `src/config/posFlags.ts`：`SCAN_BACKEND_READY = false`（后端上线改 true 即恢复，组件无需改）；
- `ResolveDialog`：`v-if="scanReady"` 只渲染「现金」一个 Tab；
- 误触兜底：任何扫码调用先判开关 → `ElMessage.warning('扫码功能暂未开放，请使用现金收款')` 并切回现金，**不发请求**；
- 验证：A1 Tab 只剩现金 ✅、A2 全程 0 次扫码请求 ✅（不再有 405/501 英文直出）。

## 二、P1 六项

### 1. 弱网误踢登录
`restoreCapabilities()` 改为返回 `'ok' | 'auth-failed' | 'network-error'`：
- 只有明确 401/403 才判 auth-failed；
- 网络错误（abort/timeout/5xx）退避重试最多 3 次，仍失败返回 network-error；
- 路由守卫：`network-error` **放行**，绝不删 token。
验证：B1 弱网后仍停留收银台 ✅、B2 token 保留 ✅。

### 2. 网络报错汉化
`request.ts` 新增 `toChineseNetworkMessage()`：
| 英文原文 | 中文提示 |
|---|---|
| Network Error / Failed to fetch | 网络连接失败，请检查网络后重试 |
| ECONNABORTED / timeout | 请求超时，请检查网络后重试 |
| 5xx 无 message | 服务暂时不可用，请稍后重试 |
| 其它纯英文 | 请求失败，请稍后重试 |
中文 message 原样透传（后端中文优先）。验证：C1 弹窗无英文 ✅、C2 toast 无英文 ✅。

### 3. 新鲜度闸门
- `endShiftBlocked` 增加 `statsStaleBlocked`（`Date.now() - statsUpdatedAt > 60s`），并加了 5s 心跳让 computed 真的会过期；
- 过期时弹出黄色条「账目已超过 60 秒未刷新，请刷新后再交班」+ 刷新按钮；
- tooltip 同步说明。
验证：D1 新账目可交班 ✅、D2 62s 后出现提示 ✅、D3 按钮阻断 ✅、D4 刷新后恢复 ✅。

### 4. 草稿暂存 `useDraft`
- 新 composable：sessionStorage 按 key 隔离，输入实时落盘，脏数据自动清理，支持**动态 key**（弹窗常驻挂载、order 后到也能切换）；
- 作废弹窗（原因/备注）、补收款弹窗（方式/实收金额）均已接入，提交成功后清草稿。
验证：E1 输入实时进入 `pos-draft:void:o1` ✅。

### 5. 触控基准
异常卡片按钮：`min-height 36px → 48px`、`min-width 104px`、字号 15px。验证：F3 实测高度 48 ✅。

### 6. 金额分元防呆
- 所有金额节点加 `data-amount-fen`（指标卡 4 个 + 收款行 + 明细抽屉合计 + 异常单卡片）；
- E2E 门禁：F1 节点数 7 ✅、F2 逐个校验「展示元 × 100 === fen」 ✅、F4 异常单 3000 分 vs ¥30.00 ✅。

## 三、登录页重复提示（附加任务）

图 1 里 toast + 表单内错误重复。现在无权限登录**只保留表单内错误**（登录区域的提示更贴近操作点），toast 已删除。
所有提示均为中文，英文原文一律被汉化层拦截。

## 四、回归

17 套件 **296 项全绿**：round2 43 / retrieve 30 / round21 25 / round12 25 / round18 16 / visual 16 / hold 16 / round13 15 / round8 15 / round27 15 / round24 14 / order-query 14 / round20 13 / round15 12 / round26 11 / round16 9 / negative 5。

截图：round27-cash-only.png（补收款只剩现金）。
