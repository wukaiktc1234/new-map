# 第二十四轮：P0 修复（刷新掉登录 / 403 裸奔 / 班次状态恢复）

> 三条 P0 全部落地，新增 11 项断言，15 套件 267 项全绿。

## 1. 刷新不再掉登录

**根因**：上一版安全改造把权限改成「只在内存、来自登录响应」，路由守卫发现内存为空就直接踢回登录页 —— 一按 F5 就掉线。

**修复**（仍然不从 localStorage 读权限，避免自授权）：
- `usePermission` 新增 `restoreCapabilities()`：拿本地 token 静默请求 `GET /v1/pos/auth/me`，成功则写入内存快照；
- 路由守卫改为 async：`内存空 && 有 token && 有 userInfo` → 先尝试恢复，成功放行，失败才跳 `/login?reason=capabilities`；
- 登录凭证 `pos-token`/`pos-user`/`pos-terminal` 依旧持久化在 localStorage（本就有，未改）。

实测：登录后 `reload()` → 停留 `/`，且刷新时确实调用了一次 `/auth/me`；`/auth/me` 返回 403 时才回登录页（带 `reason=capabilities`）。

## 2. shiftStats 403 不再裸奔

**改法**：
- store 新增 `statsForbidden`，`refreshStats()` 捕获 403 → 置位 + `lastError = 当前账号无权限访问收银数据，请联系店长授权`；
- `canEndShift` 增加 `!statsForbidden`，交班按钮 disabled，tooltip 同为授权提示；
- 弹窗内新增红色横幅（危险色，与 warning 的“待处理异常”区分）：文案 + 「重新获取」按钮；
- 授权后点「重新获取」即可恢复，无需重新登录（成功 toast：「已恢复收银数据权限」）。

顺带修掉一个会误导的相邻问题：403 之前被 `classifyShiftFailure` 归为 `error`，会显示「网络或服务异常，请重试」。现在新增 `forbidden` 态，文案是「当前账号无权限查询班次数据，请联系店长授权」。

## 3. 登录/刷新后的班次状态恢复

流程固定为：**先静默查 `GET /v1/pos/auth/current-shift`** → 有进行中班次直接进收银台（并回写本地开班标记）→ 没有才弹「开班（备用金）」弹窗。
（登录成功会清掉上一位收银员的开班标记，所以重新登录必定走一次服务端确认。）

## 验证（新增 round24，11 项）

| 断言 | 结果 |
|---|---|
| 刷新后停留收银台（不掉登录） | ✅ |
| 刷新时用 token 静默调 `/auth/me` | ✅ 1 次 |
| token 仍在本地 | ✅ |
| `/auth/me` 403 → 回登录页并带 reason | ✅ |
| shiftStats 403 → 横幅明确提示无权限 | ✅ |
| 403 → 交班按钮被阻断 | ✅ |
| tooltip 说明需要店长授权 | ✅ |
| 授权后点「重新获取」横幅消失 | ✅ |
| 授权后交班按钮恢复 | ✅ |
| 有班次 → 不弹开班 | ✅ |
| 无班次 → 弹开班 | ✅ |

全量：15 套件 **267 项全绿**（round2 43 / round21 27 / retrieve 30 / round12 25 / round18 16 / visual 16 / hold 16 / round13 15 / round8 15 / order-query 14 / round20 13 / round15 12 / round24 11 / round16 9 / negative 5）。

截图：round24-stats-403.png。

## 需要后端确认

- `GET /v1/pos/auth/me` 必须返回 `{ permissions: [...], roles: [...] }`（与登录响应同一口径）；不可用时会退化为回登录页。
