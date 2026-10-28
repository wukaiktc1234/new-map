# 第二轮全链路回归审计报告（R2）— 修复验证

> **产物编号**：AUDIT-FULLCHAIN-POS-20261028-R2
> **性质**：只读回归审计。不采信开发汇报，仅以抓包报文、前端 a11y 树、SQL 查询结果为准。
> **审计时间**：2026-10-28 22:00–22:25
> **环境变更记录**：后端已于 **21:53:16 重启**（旧进程 5668 已死 → 新进程 23144，jps 确认 FoodTraceabilityApplication，:8081 LISTENING）；`PosOrderPaymentServiceImpl.class` 编译时间 **21:52:56 < JVM 启动 21:53:16** → **新代码确认已加载**（上轮 P2 部署漂移前提已消除）。前端 vite :3001 存活（会话过期重登）。
> **备注**：浏览器 Review 窗口未可见导致 `screenshot` 不可用，UI 证据以 a11y 可达性树快照（原文摘录）+ 网络抓包替代，效力等价。

---

## 0. 总判定

| # | 验证项 | 上轮编号 | R2 判定 |
|---|---|---|---|
| 1a | 价格篡改（服务端定价） | P1-POS-ORDER-004（价格半区） | ✅ **FIXED**（附新 P2：响应回显客户端价） |
| 1b | 售罄拦截 | P1-POS-ORDER-004（售罄半区） | ❌ **FAIL** |
| 2a | 退款回补库存 | P1-POS-REFUND-005 | ❌ **FAIL** |
| 2b | 退款后厨联动 | P1-POS-REFUND-006 | ❌ **FAIL** |
| 3 | 交班身份/统计 | P1-POS-SHIFT-007/008 | ❌ **FAIL**（开发承认未改，实测证实） |
| 4a | 收银员菜单 403 | P1-POS-AUTH-001 | ❌ **FAIL**（JWT 加了 roles 但仍 403） |
| 4b | 退款按钮（cashier 无/admin 有） | 前端防呆 | ❌ **FAIL**（**任何角色都没有退款按钮**） |
| 5 | 售罄/下架防呆 | Test5 | ⚠ **半 FAIL**（UI 隐藏 ✓ / API 强行下单 ✗） |
| + | 现金三字段落库 | P2-POS-PAY-011 | ✅ **FIXED**（重启生效） |
| + | 现金单交易号前缀 | P1-POS-PAY-003 | ❌ **FAIL（新形态 QR-）** |

**最终判定：【FAIL】— 5 项重点 0 项完整修复；3 条资损红线全部存活。**

---

## 1. Test 1：价格篡改（P1-POS-ORDER-004）

**操作**：收银员 token 直发 `POST /v1/pos/orders/order` 三连探测：

| 探测 | 请求 | 结果 |
|---|---|---|
| A | 售罄菜 FD260926002（DB ¥30/status=2）+ 客户端 price=**1** + totalAmount=1 | **code=0 成交** T20261028004；响应 totalAmount=**30.00**（服务端重算）但 items 回显 price=**1** |
| B | 售罄菜 + 正确价 30 | **code=0 成交** T20261028005（售罄未拦） |
| C | 在售菜 FD260925001（DB ¥18）+ price=**1** + total=1 | **code=0 成交** T20261028006；响应 totalAmount=**18.00** |

**SQL 落库核对**：
```
orders:  T20261028004 total=3000 final=3000 | T20261028005 total=3000 | T20261028006 total=1800
items:   全部 unit_price=3000/3000/1800（DB 价），客户端 price=1 未落库 ✓
stock:   food 4: 898→897（售罄菜被扣）；food 2: 891→890（在售菜正常扣）
```

**结论**：
- ✅ **价格信任客户端已修复**：total 与 unit_price 均以 DB `sale_price` 重算落库（服务端定价）。
- ⚠ **新 P2（P2-POS-ORDER-018）**：创建响应的 `orderItems[].price` 仍回显**客户端篡改价**（1 元），与 DB（3000）不一致——报文失真，任何以响应驱动 UI 的下游都会显示假价格。
- ❌ **售罄拦截未修复**：status=2 的菜品依旧可下单并扣减其库存（资损红线半区存活）。

---

## 2. Test 2：退款 → 库存回补 + 后厨联动（P1-POS-REFUND-005/006）

**操作链**（真实业务动作）：
1. T20261028006（红烧土豆×1，服务端定价 ¥18）现金支付成功：`FF202610280196` acct20 +1800 → balance 492.02 ✓；`pay_records#1346`：payment_method=1，amount=1800，**cash_received=2000，change_amount=200 ✓ 落库**（上轮 P2-POS-PAY-011 修复确认）。
2. KDS 接单：`POST /v1/kitchen/orders/KO1793195616741/receive?chefId=2080155339590037507` → `{"code":0,"data":true}`；DB `status=received, chef_name=厨师E, material_locked=1`。
3. 收银员退款 → `{"code":403,"message":"权限不足"}`（上轮 P1-POS-REFUND-010 存活）；admin 退款 → `{"code":0}` 成功。

**退款后 SQL**：
```
foods.stock food2 = 890            ← 退款前 890，未回补（应+1→891）  ✗
kitchen_order KO...6741:
  status=received（未取消）        ✗
  cancel_time/cancel_reason = 空   ✗
  material_locked=1（锁料未释放）  ✗
orders: order_status=5, refund_amount=1800.00 ✓
refund_records #208: type=1, 1800, status=3 ✓
fund_flows FF202610280197: acct20 OUT 1800 → 47402 ✓（财务链正确）
inventory_movement: 0 新行（未出餐，无扣料，正确）
```

**结论**：
- ❌ **P1-POS-REFUND-005 FAIL**：退款后 `foods.stock` 纹丝不动，回补逻辑依旧缺失（源码路径 `refundNewOrder` 仍未调用回补）。
- ❌ **P1-POS-REFUND-006 FAIL**：KDS **未收到任何取消/拦截信号**——后厨单仍 `received` 且锁料 `material_locked=1`，厨师会继续做一份永远无人取餐的菜。
- ✅ 财务侧退款链继续正确（账面自洽掩盖了库存红线——与上轮结论一致）。

---

## 3. Test 3：交班身份错配（P1-POS-SHIFT-007/008）

**3.1 开班（UI 真实通道）**：vcashier01 UI 点「交班」→ 弹「开班 · 备用金」（terminal=前台收银机A）→ 选 ¥500 → 「开班成功，备用金 500.00 元」。
DB：`shift 530 | employee_id=vcashier01 | terminal=pos-localhost-1793197137998 | opening 500.00 | active` ✓ **归属正确**。

**3.2 本人交班（terminal 一致）**：
```
返回：shiftId=530 employeeId=vcashier01 closingCash=null totalOrders=0
DB：  closing_cash 为空（NULL），status=completed
```
- ❌ **closing_cash 仍未落库**（P1-POS-SHIFT-007 后半区存活）。

**3.3 错配探测（vcashier01 token × admin 的 terminal）**：
```
请求：POST /v1/pos/auth/shift/end  body={"terminalId":"pos-localhost-1793191756929"}（admin 的班 513）
返回：{"code":0,"message":"班次结束成功", shiftId=513, employeeId=admin, employeeName=系统管理员,
      totalAmount=168.00, totalOrders=3, cash=118.00, wechat=50.00}
DB：  shift 513 → status=completed（admin 的班被收银员关掉了！）
```
- ❌ **身份错配未修复**：endShift 仅按 terminalId 定位班次、不校验操作人——vcashier01 一键关闭了 admin 的班次（铁证）。
- ❌ **统计跨终端未修复**：3 单全部产生于**其他班次/其他终端**（T20261028002 ¥100 现金 21:04 / T20261028006 ¥18 现金 22:17 / T20261028003 ¥50 微信 21:07），窗口 20:49–22:20 为全局时间窗。
- ❌ **退款不扣减未修复**：`wechat=50.00` 计入的是 **round-1 已全额退款**的 T20261028003——净收入应为 0，仍照收。

**卫生旁证**：`pos_shifts` active 班次由 12 条升至 **21 条**（dev 回归测试期间新增 9 条未交班）。

---

## 4. Test 4：权限与防呆

**4.1 返回体字段实查**：
```
POST /v1/pos/auth/login (cashier)  → data.userInfo = {employeeName, employeeId, terminalId, userId}
                                      【无 roles、无 permissions】
JWT claims (cashier)：{"roles":["cashier"],"locationId":1, ...}  ← roles 新增 ✓，permissions 仍无 ✗
JWT claims (admin)：  {"roles":["admin"], ...}
GET /v1/auth/userInfo → HTTP 404「资源不存在」（端点不存在）
```
**判定：部分改进（roles claim），但 userInfo 端点 404、permissions 依旧不返回。**

**4.2 收银员 UI 菜单（403 是否消失）**：
- UI 登录 vcashier01 成功进入主界面，菜品区渲染：
```
[generic] "" 
  [StaticText] "权限不足，无法访问该资源"
  @e10030 [button] "重试"
```
- a11y 树查 `.dish-item` 数量 = **0**；`QA-D1`/`售罄` 文本均不存在。
- API 三连（foods/on-sale、categories/tree/enabled、combos/on-sale）全部 `{"code":403,"message":"权限不足，无法访问该资源"}`。
- ❌ **P1-POS-AUTH-001 FAIL**：收银员 POS 主链路（菜单断流）原样存活；JWT 加 roles claim 未能改变服务端鉴权结果。

**4.3 退款按钮（按角色）**：
- cashier：订单列表 → T20261028002（已支付未退）详情 → **0 处「退款」文案，无退款按钮**。
- admin：同单同操作 → **同样 0 处「退款」按钮**。
- ❌ **FAIL（双向）**：权限语义对收银员「无」是合理的，但 admin 也无按钮意味着**退款功能在前端不存在任何入口**（与 round 1 一致：退款只能裸调 API）。
- 附带实锤：已退款单 T20261028003 详情显示「后厨状态：**待制作**」、未退款已出餐单 T20261028002 显示「后厨状态：**待处理**」（实际已 served）——**详情页后厨状态字段失真**（新 P2-POS-UI-019）。

---

## 5. Test 5：下架/售罄防呆

- **菜单显示层 ✅**：`/v1/product-center/foods/on-sale`（admin token）仅返回 status=1 的 7 个菜品，售罄的 FD260926002（QA-D1）/FD260926004（QA-QA2）已不在菜单 → 前端「置灰」因服务端过滤而等效达成。
- **后端强行下单拦截 ❌**：见 Test 1 探测 A/B——售罄菜 POST 下单**全部成交**（并扣其库存 898→897）。
- 说明：未对 DB 做任何状态改写，直接复用库中已有两条 status=2 数据完成验证（避免污染主数据）。
- **判定：半 FAIL** —— 用户可见层防住，接口层裸奔（资损红线仍可被绕过菜单直击）。

---

## 6. 附加回归发现（R2 新增）

| 编号 | 等级 | 内容 | 证据 |
|---|---|---|---|
| P2-POS-ORDER-018 | P2 | 下单响应回显客户端篡改价（items.price=1 vs DB 3000） | 探测 A/C 响应体 |
| P2-POS-UI-019 | P2 | 订单详情「后厨状态」失真（served→待处理；退款单→待制作） | a11y 树摘录 |
| P2-DATA-SHIFT-020 | P2 | active 班次 12→21 条（dev 回归残留，卫生恶化） | pos_shifts count |
| ✅ FIXED | — | cash_received/change_amount 落库（2000/200 分） | pay_records#1346 |
| ✅ FIXED | — | 部署卫生：编译时间早于 JVM 启动 | class 21:52:56 / JVM 21:53:16 |
| ❌ 存活（新形态） | P1 | 交易号前缀错位：现金单（新编码 1）得到 **QR-** 前缀（旧编码 0/1=扫码） | pay_records#1346 `QR-1793197061642115` |

---

## 7. 复测结论与指令

**最终判定：【FAIL】**

- 修复生效仅 2.5 项：服务端定价（半项）、cash 三字段、部署卫生。
- 资损红线 3 条（售罄可下单、退款零回补、交班身份错配）**全部存活**，其中交班错配已用「收银员关掉 admin 班次」的对照实验复现实锤。
- 收银员菜单 403（P0 可用性阻断）原样存活。
- 退款按钮前端无任何角色可见——退款功能 UI 缺失。

**给开发的下一轮修复指令（最小集）**：
1. `createOrder` 服务端校验 `food.status==1`（售罄/下架一律拒绝，返回菜品名与状态）。
2. `refundNewOrder` 补两件事：① 按 order_items 回补 `foods.stock`（复用/重写 `recoverStockForRefund` 到新表路径）；② 将 kitchen_order 置 cancelled（写 cancel_time/cancel_reason，释放 material_locked），或至少推送 KDS 拦截。
3. `endShift`：`WHERE employee_id=当前登录人 AND terminal_id=? AND status='active'`；写 `closing_cash`（≥ opening + 现金净收入−退款）；统计 SQL 真正绑定 terminalId 参数并扣除退款金额。
4. `generateTransactionId` 增加新编码分支：1=CSH 2=WX 3=ALI 4=BANK 5=BAL。
5. ROLE_CASHIER 在 DB 落 `role_permissions`（POS_BASE/产品中心只读），或 `/v1/product-center/*` 对 `roles:['cashier']` 放行——两选一，必须有 DB 侧持久化。
6. 下单响应 `orderItems[].price` 改回 DB 重算值；订单详情后厨状态改为 join kitchen_order 实时状态。

**复测窗口承诺**：以上 6 项修复后，R3 将重复同构实验（价格篡改三连发 / 接单后退款 / 收银员×admin 终端错配探测 / JWT+userInfo / 售罄三连发）。

---

## 8. 取证索引

- token：`tmp/pos-audit-uc1.token`（vcashier01, JWT iat=1793196605, roles=cashier）、`tmp/audit-admin-kds.token`
- 订单：T20261028004/005/006（R2 探测单，均未支付留证）、KO1793195616741（received 未联动）、refund #208、FF202610280196/197
- 班次：530（vcashier01 自开自关 ✓）、513（被错配关闭 ✗）
- 类编译：`backend/target/classes/.../PosOrderPaymentServiceImpl.class` LastWriteTime=2026-10-28 21:52:56；后端进程 jps=23144
- 关联上轮报告：`docs/quality/audit-fullchain-pos-20261028.md`
