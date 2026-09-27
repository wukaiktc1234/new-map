# P1-STOMP-RECONNECT-001 实施记录：长宕机自动重连修复（implementation-record-001）

- **日期**：2026-09-27
- **性质**：PG-005 阶段 3 实施（Owner 授权启动；仅修重连，不含 topic 隔离/门店维度——那是 R-08）。
- **来源**：D #2 QA 报告 §2 发现（`docs/quality/d2-stomp-auth-fix-001-qa-report.md`）+ 任务板 §24.3i。

---

## 0. 开工前置（PG-001）

### 文件白名单

| # | 文件 | 改动性质 |
|---|---|---|
| 1 | `frontend-pos/src/utils/websocket.ts` | 监督重连重构 |
| 2 | `frontend-kitchen/src/utils/websocket.ts` | 同上 |
| 3 | `frontend-pos/src/views/CallNumber.vue` | 内联监督重连 |
| 4 | `frontend-pos/src/views/CallingDisplay.vue` | 内联监督重连 |

禁改清单：`WebSocketConfig`（后端已正确）、topic 命名（R-08）、`/ws/device`（另案）、`usePayment.ts` 等 WIP、后端任何文件。

### 与 P1-INVENTORY-CONSUMPTION-STATS-001（§24.3f）重叠确认

24.3f 文件 = `InventoryLogMapper.xml` + 集成测试 + 实施记录。本批 4 个前端文件与其**零重叠**。✅

### 回归清单

1. 两个前端 `vue-tsc --noEmit` 0 error
2. 正常连接/订阅/收推送不回归（KDS 看板卡片移动断言）
3. 页面卸载（onUnmounted/disconnect）后不再触发重连（定时器清理）
4. D #2 的 CONNECT 帧头行为不回归（0 鉴权 WARN）

### 回滚点

单 commit revert 即可（无 DB/无后端/无迁移）；回滚后恢复 stompjs 内部重连的旧行为（即本卡修复前的停摆现状）。

---

## 1. 根因诊断

### 1.1 复现实验（Node + stompjs v7 + 原生 WebSocket，绕过 SockJS 隔离变量）

连接 `ws://localhost:8081/api/ws/websocket` → 杀后端 → 观测：

```
14:04:08 EVENT ws-close code=1006 clean=false
14:04:08 DBG STOMP: scheduling reconnection in 3000ms      ← close 后正常调度
14:04:11 EVENT ws-error                                    ← 重试尝试失败，且【没有 close 事件】
14:04:21 DBG Issuing close on the websocket                ← connectionTimeout(10s) forceDisconnect
14:04:21 EVENT ws-error                                    ← 依然没有 close
（此后 240s 内零事件，重启后端后客户端永不恢复，CONNECTED 计数恒为 1）
```

### 1.2 根因结论

**stompjs v7.0.0 只从 `ws-close` 事件续期重连**（`client.js:486-497`：`onWebSocketClose → if (this.active) this._schedule_reconnect()`）。当重试尝试以 **`ws-error`（无 close）** 收场时（Node 原生 WS 与浏览器 SockJS 均复现）：

- `_schedule_reconnect` 不会被再次调用（它只挂在 close 上）；
- `connectionTimeout` 的 `forceDisconnect()`（:472 附近 "Issuing close"）对已 error 的 socket 不产生新的 close 事件，也不调度重连；
- 结果：`client.active === true` 但重连链永久停摆——后端恢复后客户端永不恢复。

与 QA 观测完全吻合（4 处客户端在 90s+ 宕机后零重连尝试，刷新页面才恢复）。

## 2. 修复方案（应用层监督重连，不 patch node_modules）

统一模式，四处客户端一致：

1. **关闭 stompjs 内部重连**：`reconnectDelay: 0`（消除双Owner竞争）；
2. **`scheduleReconnect()`**：去重护栏（timer 非空即跳过）→ `reconnectInterval`（3s/5s）后：若仍未连接 → 旧 client `deactivate()`（强制清理 stompjs 内部状态）→ 走既有 `connect()`/`initWebSocket()` 全新建链；
3. 调度点：`onWebSocketClose` + `onWebSocketError` + `onStompError`（ERROR 帧后通常有 close，护栏保证不重复）+ 初始化 catch；
4. `onConnect` 清除定时器；`disconnect()`/`onUnmounted` 清除定时器（防主动断开后被拉起）；
5. **移除旧 `tryReconnect()` 及 10 次上限计数**——旧逻辑既是双 Owner，又会在 10 次失败后永久放弃（对收银/后厨终端不可接受）；重连改为无上限、固定间隔监督；
6. `.ts` 两个服务：字段 `reconnectTimer` + `clearReconnectTimer()` + `scheduleReconnect()`；`.vue` 两页：等价的模块级 `wsReconnectTimer` + `scheduleWsReconnect()`（复用各自 `initWebSocket()`）。

`maxReconnectAttempts` 配置字段保留在 `WebSocketConfig` 类型中但不再生效（避免改动类型文件扩大白名单；行为变化=不再有重试上限，见 §4）。

## 3. 验证证据（2026-09-27 22:05–22:18，活体）

| 项 | 结果 |
|---|---|
| 类型检查 | ✅ frontend-pos / frontend-kitchen `vue-tsc --noEmit` 均 0 error |
| 基线 | ✅ KDS+POS 页面连接：`netstat` 8 条 ESTABLISHED（4 对，浏览器↔后端直连） |
| 停机 | ✅ 杀后端 8s 内连接归零；宕机持续 ~105s |
| **自动重连（核心断言）** | ✅ 后端恢复后 **10s 内** ESTABLISHED 回升至 6 条（3 对，页面未刷新）；每 10s 采样 6 次稳定保持 |
| **重连后收推送** | ✅ `receive`+`start-make` KOTEST-C01 → KDS 看板无刷新："待制作: 37 / 制作中: 2"，ORD-C01 卡片移入制作中——重连后的连接上真实收到 `/topic/orders/status` 并处理 |
| D #2 回归 | ✅ 全程后端日志 0 条鉴权 WARN；CONNECT 帧头行为不变 |
| CallNumber/CallingDisplay | ✅ 代码级同模式修复 + vue-tsc 通过；浏览器级受 vite dev 代理 `ws:true` 缺失限制（D #2 QA 已记录的环境限制），收消息断言留非代理环境补测 |

## 4. 行为变化声明（如实移交）

- 重连从"stompjs 内部 + 10 次上限"变为"应用层监督、无上限、固定间隔"——长期宕机后会无限重试（每 3s/5s 一次，成本低）。若 Owner 希望恢复上限或退避策略，另议。
- CallNumber/CallingDisplay 两页的浏览器级收消息断言依赖非代理环境（同 D #2 QA 遗留项），本卡未宣称完成该项。

## 5. 交接

修复完成 → commit（见 git log）→ 暂停等 Owner 审。测试遗留：后端/前端 dev 进程已清理；测试数据变更：KOTEST-C01/C02/C03 三张测试厨单至 making 状态（dev 库测试数据）。
