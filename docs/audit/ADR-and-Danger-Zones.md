# ADR 与危险区

### R4 补充：成对 ADJUST 的适用边界（2026-10-28）

> 当**产品路径强制「流水 + 库存」同改**、而实际只需要「补流水」时，
> 可用**成对 ADJUST（正 + 负）在同一个事务内**把净库存还原。**必须同事务**，否则中间态会被并发读到。

**为什么必须同事务（真实隐患）**：
```text
inventory 5/material 26 修复时：+100 → stock 100→200，再 −100 → 200→100
若两次各自 commit，中间态 200 会真实存在；此刻并发的 POS 扣料/库存查询会读到 200（错误可用量），
可能放行本应失败的扣减 → 超卖。组 13 的中间态 40 同理（会导致本可扣的料扣不动）。
```
**证据（本仓库实证）**：`scripts/qa/db/InventoryAlignRunner.java` 用 `TransactionTemplate` 包住成对调用；
两条流水的 `xmin` 相同即同一事务：
```text
movement 1815 change=+2 balance=102 ADJUST xmin=1010672
movement 1816 change=−2 balance=100 ADJUST xmin=1010672   ← 同一 tx
库存 5：100 → 100（净 0，中间态 102 从未提交）
```
**禁止事项**：
1. ❌ 两次独立 Service 调用各自开事务（本项目 Service 方法是 `@Transactional`，直接连调即是两个事务）；
2. ❌ 用「先改库存、后补流水」两步操作；
3. ❌ 手写 `INSERT INTO inventory_movement`（绕过 Service 会漏库存更新与状态刷新）。

## 危险区：清理数据必须做「传递性核查」（2026-10-29 实战教训）

> **删父记录（班次）前，必须先查有哪些子记录（退款/流水/异常单）依赖它的边界条件。**

**实例**：清理 46 个残留 `active` 班次时，只看了班次本身，没看「有 8 笔退款的 `refund_time` 正好落在这 46 个窗口里」。
删完后巡检 **A37（退款落不进班次窗口）从 rows=0 变成 rows=8** —— 清理动作破坏了数据一致性边界。

**补救**：定位这 8 笔 → 确认全部是自测夹具（`idempotency_key` 含 `T-RF*` / `RF2-T-RF*` / `R3P-T-R30*`）
→ **显式 flow_id 列表**软删（`UPDATE fund_flows SET deleted=1 WHERE flow_id IN (…8 个…)`）→ 复核 A37 rows=0。

**规则（今后所有 DELETE 前必做）**：
```text
1. 列出「谁依赖这条数据」：外键引用 / 时间窗口边界 / 聚合口径（如 A37 的窗口覆盖）；
2. 把核查结果贴进清理脚本注释（不是贴到聊天里）；
3. 删除用显式主键列表，不用 LIKE / 不用宽条件；
4. 删后立刻重跑巡检，确认没有断言从 0 变正数。
```

## 假信号范例 4：测试脚本与真实客户端路径不一致（2026-10-29）

**现象**：前端调 `POST /api/v1/pos/exceptions/{id}/resolve` 得到 **HTTP 404 + 空 body**；
但我的自测脚本 S1/S2/T4 全部「通过」。

**根因**：`PosExceptionController` 的 `@RequestMapping` 写成了 `"/api/v1/pos/exceptions"`，
而应用 `servlet.context-path = /api` → **真实生效路径是 `/api/api/v1/pos/exceptions/…`**。

**为什么自测没抓到**：我的脚本写的是
```python
BASE = "http://127.0.0.1:8081/api"        # 含 context-path
post("/api/v1/pos/exceptions/%s/resolve")  # 又写了一遍 /api
# → 拼成 /api/api/v1/... —— **正好命中那个错误路径**，于是「测试通过」
```
**判据自洽但目标错位**：测试验证的是「我拼出来的 URL」，不是「前端真实调用的 URL」。

**规则**：
```text
1. 验证接口可用性时，URL 必须取自「前端真实调用（源码里的 BASE/baseURL）」
   或「Spring 启动日志注册的 mapping」，**不许自己拼**；
2. 自测脚本的 base 只允许出现一次 context-path（推荐 BASE 不含 /api，路径写全）；
3. 任何「我这边测试通过但前端 404」的情况，第一件事是比对两边的**完整最终 URL**，不是看代码。
```

**旁证**：`PosShiftController` 我写的是 `/v1/pos/shift`（正确）→ 该组接口一直 200；
说明「同一个人同一天既写对又写错」，错在**没有统一规则**，不在能力。
