# D #2 实施记录：STOMP CONNECT 帧头加 Authorization（d2-stomp-auth-fix-001）

- **日期**：2026-09-27
- **性质**：PG-005.1 豁免卡（修复方向唯一，Owner 已批准跳过设计阶段）
- **诊断依据**：`docs/quality/manual-86-prerequisites-diagnosis-001.md` §9-2 / 风险表 D #2（活体验证：后端只读 CONNECT 帧头，4 处客户端均未带头 → 静默丢弃 → WS 全挂，终端退化为轮询）
- **Scope 纪律**：只改 4 处 STOMP 客户端；未动 WebSocketConfig / 拦截器 / topic 命名（R-08 范围）/ /ws/device（另案）

## 1. 改动清单（4 处，全部为前端）

| # | 文件 | 改法 | token 来源（与该端 HTTP 请求一致） |
|---|---|---|---|
| 1 | `frontend-pos/src/utils/websocket.ts` | Client 配置加 `connectHeaders: getConnectHeaders()`；新增 `getConnectHeaders()` 读 `localStorage['pos-token']` | `pos-token`（`api/request.ts:17` 同源） |
| 2 | `frontend-kitchen/src/utils/websocket.ts` | 同上 | `token` / `access_token`（`api/request.ts:24` 同源） |
| 3 | `frontend-pos/src/views/CallNumber.vue` | Client 配置加 `beforeConnect` 回调，每次（重）连接前刷新 `client.connectHeaders` | `pos-token` |
| 4 | `frontend-pos/src/views/CallingDisplay.vue` | 同上 | `pos-token` |

实现说明：
- stompjs v7 的 CONNECT 帧头选项名为 **`connectHeaders`**（`@stomp/stompjs/esm6/client.d.ts:267`）。
- 两个 `.ts` 的 WebSocketService 每次重连都重建 Client（`tryReconnect → connect() → new Client`），构造时读取 localStorage 即取到最新 token。
- 两个 `.vue` 的 Client 只创建一次、由 stompjs 内部自动重连，故用 `beforeConnect`（`client.d.ts:397`）在每次连接前重新读 token，避免 token 刷新后重连携带过期值。
- token 缺失时不带头（维持现状：后端拒绝），不伪造空 Bearer。
- 无帧头对照行为已活体复测确认（见 §2 T5）。

## 2. 活体验证（2026-09-27 19:30，本地后端 pg profile :8081）

验证脚本：与前端同库（`@stomp/stompjs@^7.0.0`，取自 frontend-pos/node_modules），SockJS 端点原生 WS 传输 `ws://localhost:8081/api/ws/websocket`，管理员 JWT 经 `/api/v1/auth/login` 实时获取。

| # | 验证项 | 结果 |
|---|---|---|
| T1 | 帧头带 `Authorization: Bearer <JWT>` CONNECT | ✅ `<<< CONNECTED user-name:admin version:1.2` |
| T2 | 订阅 `/topic/menu/update` | ✅ 订阅建立，client.connected = true |
| T3 | 全链路推送：`SEND /app/device/subscribe` → 服务端 `@SendTo` → 客户端收 `/topic/device/status` MESSAGE | ✅ 收到 `{"deviceType":"printer",...}`；后端日志 `客户端订阅设备状态: printer` |
| T4 | 断线重连（deactivate → activate） | ✅ 两次 CONNECTED |
| T5 | 对照：无帧头 CONNECT（复刻修复前） | ✅ 被静默拒绝（未 CONNECTED），后端 WARN `WebSocket连接被拒绝: 缺少Authorization header`（日志仅此 1 条，时间与 T5 对应；T1–T4 期间零 WARN） |

**类型检查**：`frontend-pos` 与 `frontend-kitchen` 各自 `vue-tsc --noEmit` 均通过（0 error）。

## 3. 未覆盖 / 后续

- 4 处客户端的浏览器端逐页回归（登录后观察 WS 连接状态）交 DS 抽检 / QA 独立验收：改动仅涉及连接头注入，订阅与业务逻辑零改动。
- 诊断 §9-2 ④ 附带发现（服务端 heart-beat:0,0 无心跳；`DeviceWebSocketService.java:177` 手拼 `/user/` 前缀疑似 bug）**未顺手修**——按 scope 纪律留待 R-08 / 另案。
- topic 门店维度（R-08）未动。

## 4. 交接

- 实施完成 → 停 → DS 抽检 → QA 独立验收。
- commit：`fix(ws): add Authorization frame header to STOMP CONNECT`
