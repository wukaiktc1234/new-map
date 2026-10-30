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

### 传递性核查清单（2026-10-29 补全为 6 类，删父记录前逐项跑）

```text
① 子表外键        —— order_items / kitchen_order / order_payment_records / order_refund_records 等
② 退款/窗口断言    —— pos_shifts 时间窗口是否被 A37 之类断言引用
③ 异常单          —— exception_ops 是否有记录（已处理态）
④ 账户余额快照    —— fund_flows 是否是某 bank_accounts 的**最新一笔**（A7 拿它和账户余额比）
⑤ 归属性          —— refund_shift_attribution 等归属表
⑥ 统计口径分母    —— A1 分母（payment_status=2 的订单数）等
```

**两次实战代价（都是漏了某一类）**：
```text
第一次：漏 ② → 删 46 个班次后 A37 从 0 变 8
第二次：漏 ④ → 删 5 条夹具流水后 A7 从 0 变 1（门店1现金账户 余额 93202 ≠ 最新快照 82202，差 11000 分）
```

### 正面例证：分支证明 gate 抓到了自己的假通过（2026-10-29）

```text
加「断言 body.code == 期望分支码」后立刻抓到三类假绿：
  ① 409/422 用例没带 token → 实际 401（旧版 PASS）
  ② 409 用例固定 idempotencyKey → 命中幂等重放 200（旧版 PASS）
  ③ 夹具被清理 → 409「订单不存在」（旧版 PASS）
→ 「测试通过」≠「测试走到了它想测的分支」。
```

## 假信号范例 5：**「分支证明」本身也要被证明**（2026-10-29）

给 `http_error_body_check.py` 加「断言 `body.code` == 期望分支码」之后，同一小时内又抓到两个假绿：

```text
① HTTP<400 时 check() 直接 return True，**不受 expect_code 约束**
   → 「409 用例实际返回 200（成功）」照样 PASS。
   修：声明了期望码后，HTTP<400 必须判 FAIL（那本身就是「没走到错误分支」）。

② 409 用例用**固定** idempotencyKey
   → 第二次起命中「幂等重放」，返回 200 replayed=true，走不到 409。
   修：每次用新 key（gate-409-<timestamp>）。

③ 夹具被清理后，接口返回 409「订单不存在」，与「已处理 409」**同码不同因**。
   修：夹具登记为永久 fixture（docs/qa/fixtures-registry.md），缺失时 gate 打 [NOT_READY] EXIT=3。
```

**结论（比范例 4 更进一层）**：
> 只加 `expect_code` 不够 —— 还要保证**「不会因为另一个原因而提前 return True」**，
> 以及**「同码不同因」要能区分**（订单不存在的 409 ≠ 已处理的 409）。
> 判据的每一层 return 都要被审视；否则「分支证明」自己就会变成新的假信号来源。

## 假信号范例 6：gate 自己污染真实账（2026-10-29）

```text
第一版 gate 用「真的调一次 resolve」来把 409 夹具变成「已处理」：
  → 每跑一次就往【门店1现金账户】记 2200 分资金流水；
  → 账户余额 93202 → 97602（+44 元测试资金）；
  → 且新流水恰好把 A7 的「最新快照」追平 → **A7 从红变绿，是偶然愈合，根因未修复**。

规则：**gate/巡检脚本一律不得写业务数据**。需要「已处理」态就用预置元数据（如 exception_ops 行），
不得走业务接口去制造状态。
```

### 冲突案例：「gate 零流水」与「A1 已支付必须有 SALE 流水」不可同时满足（2026-10-29）

```text
想造一个「已处理 409」夹具，同时要求 gate 不写资金流水：
  payment_status=2（触发 409） + 无 SALE flow（零流水） → A1 立刻判红（rows=1）
  payment_status=2 + 补 SALE flow（满足 A1）        → 账户余额被改（违反零流水）
→ 无解。处置：该分支交给允许写数据的集成测试（T4/S4''），gate 只覆盖运行时不写库的分支。

**通用教训**：当两个硬约束冲突时，**不要偷偷放宽其中一个**（今天已有先例：为了让 gate 变绿而
给夹具补流水 = 污染真实账）。正确做法是缩小该 gate 的覆盖范围，并把「为什么不覆盖」写进文档。
```

## 🔴 测试与门禁的隔离红线（Owner 2026-10-29 拍板，固化为死命令）

```text
死命令 1：测试/门禁/巡检脚本【一律不得写真实业务表】
          （含 fund_flows / bank_accounts / orders / inventory / exception_ops 之外的业务表）。
          需要「已处理/已支付」态 → 预置元数据（如 exception_ops 行）或在隔离库构造，
          绝不用真接口 prime。

死命令 2：任何脚本若要写业务数据，必须先声明目标库；
          若目标是生产/共享库（food_traceability）→ 拒绝执行并 exit 2。

死命令 3：发现「测试数据进入真实账」→ 立即停手报 Owner，
          不得自行补账洗平、不得改余额。

死命令 4：清理数据前必须按 6 类传递性核查逐项打勾并贴结果：
          ① 子表外键  ② 退款/窗口断言  ③ 异常单
          ④ 账户余额快照（最新流水）  ⑤ 归属性  ⑥ 统计口径分母
```

### 这条死命令的来历（三次真实代价）

| 时间 | 动作 | 后果 |
|------|------|------|
| 2026-10-29 | 清理 46 个残留班次（漏查②窗口断言） | A37 从 0 变 8 |
| 2026-10-29 | 清理 5 条夹具流水（漏查④余额快照） | A7 从 0 变 1（差 11000 分） |
| 2026-10-29 | gate 第一版用真 resolve prime 409 夹具 | 每跑一次往真实账户记 2200 分，累计 4400 分；A7 偶然愈合 |

## ADR：临时用 localStorage 兜底的功能，够久就变成了默认架构（2026-10-30，挂单越权）

**事故**：Owner 报「挂单越权」——A 收银员挂的单，B 收银员登录后能看到（含菜品与金额）。

**事实层（必须先说清）**：
```text
· 后端 grep 'hold-orders|holdOrders|挂单|HOLD' → 0 命中：**根本不存在服务端实现**
· 因此「SQL 少一个 where employee_id=?」这种根因**不存在**，也没有「加过滤条件」可加
· 真实机制：localStorage 是**浏览器级**存储，不区分登录账号；
  frontend-pos/src/stores/hanging-order.ts 历史上用**全局键** 'heldOrders'
  → 换账号后读到的是同一个键 → 这就是「越权」的全部真相
```

**但它仍是真实的越权隐患**：任何能操作这台机器/浏览器的人，都能看到上一位收银员的挂单内容（菜品、金额、备注）。

**临时修复（2026-10-30，前端侧）**：

| 措施 | 位置 | 效果 |
|------|------|------|
| 键加命名空间 `heldOrders:<userId>:<terminalId>` | `stores/hanging-order.ts` | 换账号读到的是另一个键，天然隔离 |
| 加载前清理非本账号键 + 老全局键 | `purgeForeignHoldKeys()` | 老数据不再被任何账号读到 |
| 跨天失效（holdTime 业务日期 != 今天即丢弃） | `isUsableHeldOrder()` + `initialize()` 在 App 启动执行 | 满足 Owner「凌晨强制失效」 |
| 切号/退出清空所有 `heldOrders:*` | `utils/posCache.ts` | 机器上不留别人的挂单 |

**根治方案（新功能，需单独立项排期）**：后端补建挂单端点
```text
表：pos_held_orders（id, terminal_id, employee_id, store_id, items_json, total_fen, status, hold_at, expire_at）
端点：POST /v1/pos/orders/hold ｜ GET /v1/pos/orders/held ｜ POST /v1/pos/orders/{id}/resume
约束：按 employee_id + store_id 过滤（不是按浏览器）；expire_at 当天 23:59:59 或次日 0 点；状态机 held→resumed/expired
```

**ADR 结论（写给未来的自己）**：
> 「后端还没端点，先放 localStorage」是一个**有期限的临时决定**。
> 一旦它活过了一个迭代，就会有人当成默认架构、在上面继续加功能（本次就是加到了「多收银员共用一台机器」的场景）。
> **凡涉及「多个用户/多台设备共享同一份数据」的功能，禁止用 localStorage 兜底**；
> 只能兜底「纯本地、单人、可随时丢弃」的状态（如主题、草稿、购物车）。

**防回归检查项（代码评审用）**：
```text
1. localStorage 的 key 里有没有 userId/terminalId？没有 → 一律视为隐患
2. 这份数据是否允许「上一位登录者」看到？允许才可放本地
3. 有没有过期/上限策略？只增不删的本地缓存 = 定时炸弹
```

---

