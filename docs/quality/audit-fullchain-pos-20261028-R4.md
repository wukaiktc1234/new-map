# R4 最终回归验证审计报告 — 终验（准予联调前的最后考验）

> **产物编号**：AUDIT-FULLCHAIN-POS-20261028-R4
> **性质**：只读穿透审计。不采信开发自测汇报，全部以抓包报文、前端 a11y 树、SQL 结果为准。
> **审计时间**：2026-10-29 03:00–03:15
> **环境核验**：后端 PID 31048（FoodTraceabilityApplication，JVM 启动 **2:59:15**）＞类编译 **2:58:58** ✓ 部署时序健康；前端 :3001 存活。
> **基线**：orders=126；admin active 班次={554}（攻击前固化）；user_roles 表存在。

---

## 0. 判定总表

| # | 验收项 | 判定 | 一句话证据 |
|---|---|---|---|
| 1 | 开班身份伪造（P1-POS-SHIFT-024） | ✅ **PASS** | 双形态（admin_id/admin）均 **403「不得为他人开班」**；admin active 班次攻击前后均为 {554}，零新增 |
| 2 | 钱箱公式（P1-POS-SHIFT-025） | ✅ **PASS** | closing_cash=**500.00**（期初500+现金22−退款22），remark 双口径明示；**非**净营收 0.00 |
| 3 | price 静默回显（P2-POS-ORDER-018） | ✅ **PASS** | 篡改 0.01 下单：响应 `orderItems[].price`=**22.00**（=DB 真实价），DB unit_price=2200 |
| 4 | admin 通配 + 收银员隔离 | ⚠ **FAIL（半）** | API 双 200 ✓；admin 退款按钮 ✓；**但 cashier UI 退款按钮可见（2 个）✗——要求隐藏** |
| 5 | KDS 退款取消字段（P2-POS-REFUND-022） | ✅ **PASS** | cancel_time=**03:09:21** ✓、cancel_reason=**「订单退款取消」** ✓（双值非空） |
| 6 | role_id 混存清理 | ✅ **PASS** | `role_id NOT LIKE 'ROLE_%'` = **0 行**；24 行全部 ROLE_ 规范前缀 |

**最终判定：【FAIL】— 打回（差一项：Test 4 收银员退款按钮未隐藏 + 权限声明与执行不一致 P2-POS-AUTH-023 存活）。其余 5 项全绿。**

---

## 1. Test 1：开班身份伪造 — ✅ PASS

攻击载荷（cashier token，JWT sub=vcashier01）：
```
A: POST /v1/pos/auth/shift/start {"terminalId":"AUDIT-R4-FORGE-1","employeeId":"admin_id","openingCash":500}
   → HTTP 200 body code=403「不得为他人开班」
B: POST /v1/pos/auth/shift/start {"terminalId":"AUDIT-R4-FORGE-2","employeeId":"admin","openingCash":500}
   → HTTP 200 body code=403「不得为他人开班」
```
DB 断言：
```
BEFORE: admin active 班次 = {shift 554 | admin | AUDIT-R3-AD}
AFTER:  admin active 班次 = {shift 554}（零新增）；employee_name='hacker' 班次 = 0 条
```
R3 的 P1-POS-SHIFT-024（cashier 冒名创建 shift 555）已修复：服务端绑定 JWT 身份。

## 2. Test 2：钱箱公式 — ✅ PASS

操作链（全部真实交易，cashier 通道）：开班 shift **562**（opening=500.00, terminal=AUDIT-R4-CA）→ QA-链路复验菜 ¥22 现金单 T20261029005（FF202610290020 +2200 → acct20 71202）→ 退款（FF202610290021 −2200 → 69002）→ 立即交班。

```
响应：closingCash=500.00
      remark="[钱箱口径] 期初=500.00 + 现金收入=22.00 − 现金退款=22.00 = 钱箱=500.00
              ｜ [营收口径] 营收=22.00 − 退款=22.00 = 净额=0.00"
DB：  pos_shifts#562 closing_cash=500.00, total_amount=22.00, total_orders=1, status=completed
```
- **钱箱公式正确**：500 + 22 − 22 = **500.00** ✓（R3 的「净营收当 closing」缺陷已修复——退款后净营收 0.00 被正确另列，closing_cash 不再被污染）。
- 统计=本班 1 单/¥22（跨终端污染持续修复 ✓）。
- 附注：响应/DB 的 employee_id 显示为 userId 数字（2080170081205395463）——归属正确（vcashier01 的 userId），仅口径显示差异，不计缺陷。

## 3. Test 3：price 静默回显 — ✅ PASS

```
攻击：POST /v1/pos/orders/order items=[{id:FD260926005(¥22), price:0.01}] totalAmount=0.01
响应：code=0, totalAmount=22.00, orderItems[0].price=22.00   ← 回显 DB 真实价，非 0.01 ✓
DB：  unit_price=2200, amount=2200, total=2200, final=2200    ← 三方一致 ✓
      stock7 890→889（下单扣减，正常）
```
R2 P2-POS-ORDER-018（响应回显客户端假价）已修复。

## 4. Test 4：admin 通配与收银员隔离 — ⚠ FAIL（半）

**API 层 ✅**：
```
admin  token × GET /v1/product-center/foods/on-sale → HTTP 200, body code=0, items=6
cashier token × GET /v1/product-center/foods/on-sale → HTTP 200, body code=0, items=6（只读已授）
```
**UI 层 ❌（收银员退款按钮未隐藏）**：
```
cashier UI 登录 → 菜单渲染 7 品 ✓（无 403）
→ 订单查询 T20261028002（paid 未退）→ 详情对话框：
   [generic] "菜品明细 关闭 申请退款"
   @e11626 [button] "申请退款"        ← 收银员可见！任务要求必须隐藏
   （按钮出现 2 处：标题区+底部操作区）
admin UI 对照：同单详情同样 2 个退款按钮（admin 正确持有）
```
- 前端未按角色隐藏退款入口；且后端退款 API 对 cashier 实测 **403**（见 Test 5 复查）→ 收银员点「申请退款」**必然报错**——「可见但必败」的防呆倒退（R3 时按钮根本不显示）。
- 根因链（与 P2-POS-AUTH-023 同源）：cashier `userInfo.permissions` 含 `order:refund`/`refund:manage` → 前端按 permissions 渲染按钮 → 后端退款端点却校验其它权限点 → 三者不一致。

## 5. Test 5：KDS 退款取消字段 — ✅ PASS

操作链：QA1 ¥20 微信单 T20261029004 → 支付（code=0）→ KDS 接单（received）→ 退款（cashier 403 复证 P2-023 → admin 成功）。
```
DB：kitchen_order KO1793213975887
    status=cancelled, material_locked=0
    cancel_time=2026-10-29 03:09:21   ← 非空 ✓
    cancel_reason=订单退款取消          ← 非空 ✓
    foods.stock id5 = 895（回补 ✓ 回归确认）
```
R3 P2-POS-REFUND-022（cancel_time/reason 空）已修复。收银员退款 403 复证 → **P2-POS-AUTH-023 存活**（与 Test 4 同根因）。

## 6. Test 6：role_id 混存清理 — ✅ PASS

```
SELECT role_id FROM user_roles WHERE role_id NOT LIKE 'ROLE_%'  → 0 行 ✓
全表 24 行分布：ROLE_ADMIN(1)/ROLE_CASHIER(1)/ROLE_EMPLOYEE(7)/ROLE_STORE_MANAGER(4)/... 全部 ROLE_ 前缀
```

---

## 7. 打回项（仅剩 1+1）

| 编号 | 等级 | 缺陷 | 修复要求 |
|---|---|---|---|
| P1-UI-REFUND-026（新） | P1（UI 防呆） | 收银员 UI 退款按钮可见（任务红线：必须隐藏）；点击必吃 403 | 前端按角色/permissions 判定隐藏退款入口（且 permission 数据源与后端端点校验对齐） |
| P2-POS-AUTH-023（存活） | P2 | cashier userInfo 声明 `order:refund`/`refund:manage`，退款 API 仍 403——声明≠执行 | 统一权限点：要么端点接受 `order:refund`，要么 userInfo 撤回该权限声明 |

**同根因**：permissions 数据（userInfo 声明）↔ 前端按钮渲染 ↔ 后端端点校验 三方未对齐。一次修复可同时清掉两项。

## 8. 与 R3 打回项对照

| R3 编号 | R4 状态 |
|---|---|
| P1-POS-SHIFT-024 开班身份伪造 | ✅ FIXED（403「不得为他人开班」） |
| P1-POS-SHIFT-025 closing_cash 口径 | ✅ FIXED（钱箱口径 500.00 + remark 双口径） |
| P2-POS-ORDER-018 响应假价回显 | ✅ FIXED（回显 22.00=DB 价） |
| P2-POS-REFUND-022 cancel_time/reason 空 | ✅ FIXED（双值非空） |
| P2-POS-AUTH-023 权限声明≠执行 | ❌ 存活（+新衍生 P1-UI-REFUND-026） |

---

## 9. 结论

**【FAIL】— 打回。**

6 项验收 **5 项 PASS**，修复质量很高（R3 全部 4 项打回项中 3 项已实测修复）。剩余唯一硬伤：
**收银员 UI 退款按钮未隐藏**（任务红线）+ 其同根因 **P2-POS-AUTH-023（权限声明与端点执行不一致）**。

这是纯前端/权限数据对齐问题，无资金链风险（后端 403 兜底有效，越权点击只会报错）。**修复该对齐问题后，R5 仅复测 Test 4 双角色 UI + Test 5 收银员退款权限一致性即可《准予联调》——其余 5 项已验收通过，无需重测全量。**

## 10. 取证索引

- token：`tmp/pos-audit-uc1.token`（cashier, terminal AUDIT-R4-CA）、`tmp/audit-admin-kds.token`
- 攻击载荷：FORGE-1（admin_id）/FORGE-2（admin）→ 均 403；伪造班次=0
- 交易：shift 562（closing 500.00）、T20261029005（¥22 现金+退款，FF202610290020/021）、T20261029004（微信退款联动）、T20261029006（篡改价回显单）
- SQL：T6 脏 role_id=0；admin active={554} 前后一致
- 关联：R1/R2/R3 报告（docs/quality/audit-fullchain-pos-20261028*.md）

---

## 附录 A：R5 前置核查（2026-10-29 03:18–03:20）— 两项打回均未修复，根因已精确定位

**核查方式**：R5 正式复测前的例行进场探针（mtime / git / fresh-login / UI 实测 / API 实测）。
**结论：0/2 项修复落地，R5 启动条件不成立。等待开发提交修改证据并重新部署后进入 R5。**

### A1. 环境增量（vs R4 收口 03:15）

| 项 | R4 时 | 当前 03:18 | 判定 |
|---|---|---|---|
| 后端进程 | PID 31048（JVM 2:59:15） | **同一 PID 31048** | 未重部署 |
| 关键类编译时间 | 2:58:58 | **同 2:58:58** | 未重编译 |
| backend/src mtime | ≤ 2:58:28 | **零变化** | 无「改了源码没部署」 |
| frontend-pos/src mtime | — | **仅 ShiftHandover.vue（3:15:26）** | 与退款按钮/权限无关；未提交 WIP，QA 不验收、R5 不测 |
| DB 权限数据 | — | fresh login 复核 | 未变（见 A2） |

### A2. 两项打回项当场实测

**① P1-UI-REFUND-026（cashier 退款按钮必须隐藏）— ❌ 仍 FAIL**
```
UI 实测：vcashier01 登录 :3001（a11y 身份确认「测试收银员 工号 vcashier01」）
→ 订单查询 → T20261028002（paid 未退）→ 详情弹窗：
   退款按钮 refs ×2（@e12174 / @e12185）—— 仍可见
```

**② P2-POS-AUTH-023（权限声明≠执行）— ❌ 仍 FAIL**
```
fresh login（AUDIT-R5-CA）：cashier userInfo.permissions = 46 项
  refund 四键全部仍在声明：order:refund / order:refund:initiate / refund:manage / refund:view
牺牲单 T20261029007（¥18 现金，已支付）→ cashier POST /order/{no}/refund
  → HTTP 200 + body code=403「权限不足，无法访问该资源」（执行侧照旧拒绝）
admin（AUDIT-R5-AD）：permissions=['*']（通配），R4 已测退款 200 —— admin 侧自洽 ✓
```
（注：权限数 46 vs R3 记录的 47，差 1 项具体键无法从既有取证对账，不影响判定——闸门键 `order:refund` 等四键均在。）

### A3. 根因精确落点（新增证据，补 R4 §7 之「同根因」）

```
backend/.../service/impl/PosOrderPaymentServiceImpl.java:332
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_MANAGER')")   ← 端点校验【角色 authority】

backend/.../service/impl/PosAuthServiceImpl.java:558-588
  非 admin permissions = DB 真实权限（user_permissions ∪ role_permissions）∪ POS 基础集
  → cashier 的 refund 四键由 DB 权限映射声明                       ← userInfo 声明【权限码】

frontend-pos/src/composables/usePermission.ts:30
  PERM.REFUND = 'order:refund'；evaluate() 以 permissions.includes(code) 放行
  OrderDetailDialog.vue:117 v-if="order && allowRefund && canRefund(order)"  ← 前端闸门【权限码】
```
**三方两套体系**：端点按角色拦，声明与前端按权限码判。cashier「有声明、无角色」→ 按钮必现、点击必 403。
且前端唯一隐藏路径是 `markDenied`（403 后反应式隐藏，Home.vue:467-471）——非主动防呆。

### A4. 给开发的修复指令（二选一路径，须同时满足两条红线）

红线：① cashier UI 退款入口 = 0（主动隐藏，不得依赖「点了 403 再隐藏」）；② userInfo 声明与端点执行一致。

- **路径 A（推荐：撤回声明）**：以 migration 形式（禁直接改库）将 `order:refund`、`order:refund:initiate`、`refund:manage` 从 ROLE_CASHIER/cashier 的权限映射中移除（`refund:view` 是否保留视收银端是否需要退款记录只读入口，若保留则前端退款按钮闸门不得绑定该码）。效果：前端闸门自然关闭 + 声明与 403 对齐；端点注解不动。admin（`['*']` + 内置特判）不受影响。
- **路径 B（端点放行）**：@PreAuthorize 增加 `order:refund` → cashier 可真退。**与红线①冲突，不得单独采用**。
- 通用要求：验收环境须清空 localStorage 的 `pos-perm-denied` / `pos-perm-granted` 运行时残留（`markDenied`/`grant` 会污染复测）；OrderQueryDialog.vue:78 列表行退款入口与 OrderDetailDialog 同口径处理。

### A5. 本轮审计操作痕迹
- 新 token：`tmp/pos-audit-r5-ca.token`（AUDIT-R5-CA）、`tmp/audit-r5-ad.token`（AUDIT-R5-AD）
- 新订单：T20261029007（牺牲探针单，paid ¥18 现金，cashier 退款 403 被拒，单据完好留作 R5 样本）
- DB 零写入（只读 + 业务 API 验证）
