# ADR-20261030：POS 身份三源、应用层不变量、探针夹具规范

## 🔴 首条结论（今晚最重要的安全认知）

> **权限门禁 ≠ 数据边界。**
> `@PreAuthorize('pos:shift:manage')` 只回答「你能不能调这个接口」；
> **数据边界**回答「你能看到谁的数据」。**有权限 ≠ 有数据权**（店长能查班次，但只能查自己的）。
>
> 实证：vtest01 拥有 `pos:shift:manage`（其角色 7 个 pos 码含 shift:manage），
> 因此权限门禁对他是**放行**的 —— 若接口内部不按 employee 过滤，他就能读到 vcashier01 的钱箱金额。
> 今晚同族泄露共 **五处**，全部是「有权限门禁、无数据边界」。

> **一句话结论**：`pos_shifts.employee_id` 落库是 **user_id 纯数字**，但代码里有三条通路（employeeCode / userId / username），
> 靠 `employee_code` 为 NULL 才碰巧一致 —— **没有断言的不变量，就是下一个静默分裂点**。

## 1. 三套 id 并存（取证）

| 来源 | 实际值 | 代码位置 |
|---|---|---|
| `pos_shifts.employee_id`（落库） | **user_id**（如 2080170081205395463） | `PosAuthServiceImpl.startShift`：`employeeId = selfKey`；`selfKey = user.employeeCode` **但 employee_code 实测全为 NULL** → 回退 `String.valueOf(jwtUserId)` |
| `SecurityUser.getUserId()` / `SecurityUtils.getCurrentUserId()` | **user_id** | `SecurityUtils.java:42-66` |
| 接口入参 `employeeId`（DTO） | **登录名/工号字符串**（如 `vcashier01`） | `PosLoginDTO:47` / `PosStartShiftDTO:39` |

**实测**：`users` 表中 vcashier01 / vtest01 的 `employee_code` 均为 **NULL**；`pos_shifts` 51 行中 active 16 行，
employee_id 全部是 user_id（2080170081205395463 = vcashier01）。

## 2. 规则：权限与归属判断统一用 user_id + 不变量必须有断言

```
新增断言（PosAuthServiceImpl.startShift，落库之前）：
  if (selfKey == null || !selfKey.matches("^\\d+$"))
      throw new BusinessException(500, "班次归属键必须是 user_id 纯数字，禁止混入工号/用户名…");
理由：一旦有人给 users.employee_code 填值，selfKey 会变成工号字符串，
      历史班次（userId）与新班次（code）永久分裂，且【没有任何报错】。
      宁可 500，也不静默分裂。
```

## 3. 应用层不变量：一个终端一个 active 班次（**数据库不保证**）

```
pos_shifts 索引：pkey(shift_id) / idx_pos_shifts_terminal（普通 btree，**非唯一**）/ employee / status
→ 无 UNIQUE(terminal_id)：「一个终端同时只能一个 active 班次」完全由应用层保证：
  PosAuthServiceImpl:356  throw new BusinessException(409, "该终端已有进行中的班次，请先结束当前班次")
→ 教训：应用层不变量必须有对应测试/断言，否则改一处就破，且不报错。
```

## 4. 孤儿班次：随机 terminalId 的现存缺陷（稳定化顺手修掉）

```
现状：terminalId 每次登录重新生成（前端 pos-<host>-<ts>）
  → 浏览器崩溃/刷新后，旧 terminal_id 的 active 班次【永远无人能结束】
     （新 id 查不到它；交班 SQL 限定 employee_id=当前登录人 AND terminal_id=请求终端，也匹配不到）
  → 产生孤儿班次 + 同门店多个 active 班次
改为设备级持久 ID 后：重启能找回自己的班次 → 可继续/交班/结束 → 孤儿消失。

代价（必须一起处理）：
  · 重启后调 startShift 会撞 409「该终端已有进行中的班次」→ 前端需提供「继续该班次 / 交班」
  · 同设备换人（A 未交班，B 登录）→ B 被 409 挡住，而现有交班 SQL 限定 employee_id=当前登录人，B 动不了 A 的班次
    → 记 P1：需产品定义跨员工交接班（涉及钱箱责任，不是技术选择）
```

## 5. 跨人资金信息泄露：同一家族的四处，已全部封堵

| 端点 | 修前 | 修后 |
|---|---|---|
| `GET /v1/pos/auth/current-shift` | `findActiveShiftByTerminalId`（只按终端）→ B 拿到 A 的班次含 openingCash | `getMyCurrentShift`（employee 过滤）→ B 得 `data:null` |
| `GET /v1/pos/shift/opening-cash` | 同上 | 同上 |
| `GET /v1/pos/shift/stats` | 同上 → **B 实测拿到 openingCash=321.00** | 改 `getMyCurrentShift` → B 得 `data:null` |
| `GET /v1/pos/shift/exceptions` | 同上（按 terminal 取班次） | 同上（B 得空列表） |
| `GET /v1/pos/auth/shift/today` | `findTodayShiftsByTerminalId` 返回该终端**今日所有**班次 → B 实测拿到 A 的 shiftId=682 / openingCash=321.0 | 按 selfKey/userId/username 过滤 → B 得 **0 条**，A 得 1 条 |

**新增端点** `GET /v1/pos/auth/terminal-open-shift-exists?terminalId=` → 只返回 `{exists, employeeNameMasked}`；
**禁止**返回 openingCash / shiftId / employeeId（资金与身份信息）。实测：B 得到 `{exists:true, employeeNameMasked:"测***"}`，
A 得到 `{exists:false}`（本人班次不算他人）。

## 6. 探针夹具规范（三条）

```
事实（追责结论）：T-HOLD-PROBE 的落库来自【探针调公开 API】（先 POS 登录 terminalId=T-HOLD-PROBE，再 POST 挂单），
  日志证据："POS收银端登录: employeeId=vcashier01, terminalId=T-HOLD-PROBE" 20:29:45；
            "[METRIC] hold_create … terminalId=T-HOLD-PROBE amountFen=1234" 20:29:45
  → **不是裸 psql 写表**（这点比最初判断好），但仍是夹具污染。

规则：
  ① 探针夹具必须带前缀：terminalId 用 `T-*`，单号用 `AUDIT-*`（便于按前缀清理）
  ② 探针必须自清理（finally），清理后贴 left=0
  ③ scripts/qa/fixture_scan.py 只读扫描已知业务表的 terminal_id / order_no / remark / account_name,
     命中 T-* / AUDIT-* / *PROBE* 的行 → 输出清单；接 preflight 作 WARN（不阻断）
```

**fixture_scan.py 首跑结果**：`pos_shifts.terminal_id` 命中 **33** 行（T-BND*/T-CALC*/T-CASH*/AUDIT-*），
其余表 0 命中 → 说明历史夹具集中在 pos_shifts，需人工分批清理（不在本 ADR 处置）。

## 7. 其余处置

| 项 | 结论 |
|---|---|
| `/v1/pos/auth/shift/today` | **纠正**：该端点**存在**（PosAuthController:180，前缀 /v1/pos/auth），前端 posApi.ts:654 一直调用正确路径。我先前用 /v1/pos/shift/today 探测得 404 是**探针前缀错误**，不是端点缺失 → 撤回原 P2 结论；真实问题它是第五处泄露，已修 |
| 12 行挂单联调数据 | 已备份到 `docs/audit/hold-fixture-20261030.md` 并删除，`pos_held_orders` = 0 行 |
| 跨员工交接班 | **P1**，需产品定义（钱箱责任） |
