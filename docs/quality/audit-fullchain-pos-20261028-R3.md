# R3 最终验收审计报告 — 穿透复测（最终验收）

> **产物编号**：AUDIT-FULLCHAIN-POS-20261028-R3
> **性质**：只读穿透审计。不采信开发自测/curl 证据，全部以本审计的抓包报文、前端 a11y 树、SQL 查询为准。
> **审计时间**：2026-10-29 02:29–02:40
> **环境核验**：新包确认——`jps` 进程 28956（FoodTraceabilityApplication），`PosOrderPaymentServiceImpl.class`/`OrderNewServiceImpl.class`/`PosAuthServiceImpl.class` 编译时间 **2026-10-29 0:12:02–0:12:04** < JVM 启动 **0:12:27** ✓（部署时序健康）。前端 :3001 存活。
> **基线**：orders(deleted=0)=117；foods stock: id2=874 / id4=896 / id5=895；active shifts=36。

---

## 0. 判定总表

| # | 验收项 | 判定 | 关键证据 |
|---|---|---|---|
| 1a | 价格篡改 ¥0.01 → 服务端重算 | ✅ **PASS** | DB total/final/unit_price 全落 1800（DB 价） |
| 1b | 售罄拦截 + 零副作用 | ✅ **PASS** | 三连发全 400「菜品已售罄」；orders 117 不变；stock4=896/stock6=900 不变 |
| 2 | 退款联动（stock+KO+锁料） | ✅ **PASS** | stock 894→895 ✓；KO received→**cancelled** ✓；inventory.locked_quantity 全 0 ✓；财务冲账 ✓ |
| 3a | 交班防越权（cashier 关 admin 班） | ✅ **PASS** | 400「当前终端没有你的在班班次」；admin 班 554 仍 active |
| 3b | closing_cash 落库且口径正确 | ❌ **FAIL（口径）** | 落库 ✓（18.00 非 NULL），但 = **净营收 18.00**，任务口径「备用金500+现金收入18−退款0」= **518** —— 把「营收」存进了「钱箱现金」字段 |
| 3c | 开班身份一致性 | ❌ **FAIL（新 P1）** | cashier token + body `employeeId=admin` → 成功创建 **shift 555（employeeId=admin）**（冒名开班） |
| 4 | 权限/UI 断流/状态映射 | ✅ **PASS** | cashier 菜单 7 品渲染无 403；admin「申请退款」按钮出现；served→「已上菜」；refunded→「已退款」 |

**最终判定：【FAIL】— 打回。4 项验收 3 项全绿，但交班域出 1 项新 P1 越权（开班身份伪造）+ closing_cash 资金口径错误；另存 3 项 P2 遗留。未达「全绿」，不准联调。**

---

## 1. Test 1：价格篡改与售罄拦截 — ✅ PASS

### 1a 价格篡改（攻击：¥18 菜品按 ¥0.01 下单）
```
POST /v1/pos/orders/order  items=[{id:FD260925001, price:0.01, quantity:1}]  totalAmount=0.01
响应：code=0, totalAmount=18.00（服务端重算）
DB：orders T20261029001 total_amount=1800, final_amount=1800
    order_items unit_price=1800, amount=1800   ← 客户端 0.01 被完全丢弃 ✓
```
- ✅ 落库三方一致（分）。stock2 874→873（下单扣库存，正常业务动作）。
- ⚠ **P2 遗留（R2 P2-POS-ORDER-018 存活）**：响应 `orderItems[].price` 仍回显客户端篡改价 0.01（与 DB 1800 不一致）。落库已安全，报文未消毒。

### 1b 售罄拦截（三连发 + 零副作用断言）
| 探测 | 请求 | 结果 |
|---|---|---|
| B1 | FD260926002（QA-D1-售罄验证菜, status=2）price=30 | **code=400「菜品已售罄：QA-D1-售罄验证菜」** |
| B2 | FD260926002 price=0.01 | **code=400「菜品已售罄：QA-D1-售罄验证菜」** |
| B3 | FD260926004（QA-QA2, status=2） | **code=400「菜品已售罄：QA-QA2-独立验收菜」** |

DB 断言：`orders` 计数 117（前后不变）、`foods.stock` id4=896 / id6=900（不变）、窗口内零新订单 ✓。
（未对 DB 做任何状态改写，复用库中既有 status=2 数据完成验证。）

---

## 2. Test 2：退款联动 — ✅ PASS

**操作链**（真实交易）：QA1（¥20，在售）创建 T20261029002 → 微信支付（fund FF 入账，transaction_no=**WX-**… ✓ R2 P1-POS-PAY-003 修复确认）→ KDS `receive`（chefId=厨师E）→ `status=received, material_locked=1` → 退款。

**退款后 SQL**：
```
foods.stock food5: 894 → 895        ← 回补 +1 ✓（R2 时 890 纹丝不动）
kitchen_order KO1793203970710:
  status: received → cancelled ✓    ← R2 时 received 不变
  inventory.locked_quantity（material 3 三行）= 0.0000  ← 物理锁料全部释放 ✓
  （kitchen_order.material_locked=1 为「锁料动作已执行」标志位，非当前锁量；
   R1/R2 误读已修正——以 inventory.locked_quantity 为准）
orders: order_status=5, refund_amount=2000 分 ✓
fund_flows: FF202610290008 acct25 direction=2 amount=2000 [POS_REFUND] ✓
```
- ✅ **P1-POS-REFUND-005 FIXED**（库存回补）
- ✅ **P1-POS-REFUND-006 FIXED**（后厨联动 cancelled + 锁料释放）
- ⚠ **P2 遗留（P2-POS-REFUND-022）**：`cancel_time`/`cancel_reason` 为空——status 置 cancelled 但审计字段未写。
- ⚠ **P2（P2-POS-AUTH-023）**：收银员 userInfo 声明含 `order:refund`/`refund:manage`，但实际调退款接口 **403**——权限声明与执行不一致（前端会以为可退款，点击必失败）。

---

## 3. Test 3：交班防越权 — ❌ FAIL（两项遗留）

### 3a 防越权（核心）✅ PASS
```
准备：admin 于 AUDIT-R3-AD 开班（shift 554, active, opening 500）
攻击：cashier token × POST /v1/pos/auth/shift/end {terminalId:"AUDIT-R3-AD"}
响应：code=400「当前终端没有你的在班班次，无法交班」
DB：  shift 554 仍 active（未被关闭）✓ —— R2 的「收银员关掉 admin 513」攻击已被拦截
```

### 3b closing_cash ❌ FAIL（口径错误）
```
cashier 班内：现金单 T20261029003 ¥18（收20找2），退款=0
self end shift → 响应 closingCash=18.00
               remark="[交班口径] 营收=18.00 − 退款=0.00 = 净额=18.00"
DB：           shift 553 closing_cash = 18.00（落库 ✓ 非 NULL）
任务口径：     备用金500 + 现金收入18 − 退款0 = 518.00（钱箱应剩现金）
实际：         18.00 —— 把「净营收」写进了「closing_cash（钱箱现金）」字段
```
- 落库修复 ✓（R2 是 NULL），但**字段语义错误**：交班对账按 closing_cash 对钱箱将**短款 500 元/班**。判 FAIL（资损口径红线旁支）。
- 附：响应与 DB 一致（18.00），统计只含本班 1 单（跨终端污染已修复 ✓——R2 同场景混入 3 单跨终端）。

### 3c 开班身份一致性 ❌ FAIL（新 P1-POS-SHIFT-024）
```
攻击：cashier token × POST /v1/pos/auth/shift/start
      body={"terminalId":"AUDIT-R3-FAKE","employeeId":"admin","openingCash":500}
响应：code=0 班次开始成功 shiftId=555
DB：  shift 555 employee_id=admin terminal_id=AUDIT-R3-FAKE active
```
- shift/start **信任 body.employeeId，不与 JWT 身份（sub=vcashier01）比对**——收银员可冒充任何人开班、伪造终端。endShift 已绑定身份，startShift 没有。**必须打回**。
- （伪班次 555 已由审计以 admin 权限闭环清理，避免残留。）
- 另注：API 通道中文名（employeeName）落库乱码「?????」——复现为本审计请求编码（GBK 字节）所致，UI 通道（shift 530 employee_name=测试收银员）正常，不计系统缺陷。

---

## 4. Test 4：权限与 UI — ✅ PASS

| 检查 | 结果 | 证据 |
|---|---|---|
| 登录返回体含 permissions | ✅ | cashier `userInfo.permissions` = 47 项（product:food:view / pos:menu:query / order:pay / order:refund / pos:shift:manage…），roles=["cashier"]；JWT claims 含 roles |
| 收银员菜单渲染（403 消失） | ✅ | UI 登录 vcashier01 → `.dish-item` ×7（QA-红烧土豆/链路复验菜/QA1/…/生菜套餐），分类树 11 类，**无「权限不足」文本**（R2: 0 菜品+重试按钮） |
| admin 退款按钮 | ✅ | 订单查询 → T20261028002 详情 → 标题栏「申请退款」按钮 + 退款按钮 refs ×2（R2: 任何角色 0 个退款按钮） |
| 后厨状态映射 | ✅ | served 单 T20261028002 → 「已上菜」（R2: 「待处理」失真）；已退款单 T20261028003 → 支付状态「已退款」/后厨状态「已退款」（R2: 「待制作」失真），且已退款单不再显示退款按钮 ✓ |

---

## 5. 遗留缺陷清单（R3 打回项）

| 编号 | 等级 | 缺陷 | 复现 | 修复要求 |
|---|---|---|---|---|
| **P1-POS-SHIFT-024** | **P1（新）** | 开班身份伪造：`shift/start` 信任 body.employeeId，不校验 JWT 身份 | cashier token + employeeId=admin → shift 555（admin 名下） | startShift 强制 `body.employeeId == JWT.sub`，否则 403 |
| **P1-POS-SHIFT-025** | P1（口径） | closing_cash = 净营收（18）而非钱箱现金（518=500+18−退款） | self-end 实测 | closing_cash = opening_cash + Σ现金收入 − Σ现金退款（±缴款/划出）；或字段改名 net_revenue 并新增钱箱字段 |
| P2-POS-ORDER-018 | P2（R2 遗留） | 下单响应 items.price 回显客户端篡改价 | 探测 1a 响应 price=0.01 vs DB 1800 | 响应回显 DB 重算价 |
| P2-POS-REFUND-022 | P2（新） | 退款 KO cancelled 但 cancel_time/cancel_reason 空 | Test 2 SQL | 联动时补写审计字段 |
| P2-POS-AUTH-023 | P2（新） | cashier userInfo 含 order:refund 但退款 API 403（声明≠执行） | Test 2 收银员退款 | 对齐权限点命名或授予 refund 端点 |

## 6. 与 R2 对照的修复确认（全绿部分）

| R2 编号 | R3 状态 |
|---|---|
| P1-POS-ORDER-004 售罄半区 | ✅ FIXED（400 拦截+零副作用） |
| P1-POS-ORDER-004 价格半区 | ✅ FIXED（DB 重算落库；仅剩响应回显 P2） |
| P1-POS-REFUND-005/006 | ✅ FIXED（stock 回补+KO cancelled+锁释放） |
| P1-POS-SHIFT-007 越权关班 | ✅ FIXED（400 拒绝） |
| P1-POS-SHIFT-008 统计跨终端 | ✅ FIXED（553 统计=本班 1 单/¥18） |
| P1-POS-SHIFT-007 closing_cash NULL | ✅ FIXED（落库；但口径新缺陷 → R3 打回项） |
| P1-POS-AUTH-001 菜单 403 | ✅ FIXED（permissions 落 DB + userInfo 返回 + UI 渲染 7 品） |
| P1-POS-PAY-003 交易号前缀 | ✅ FIXED（微信 WX- 前缀实测） |
| P2-POS-PAY-011 cash 三字段 | ✅ FIXED（R2 已证，本轮收现 20/2 落库正常） |
| P2-POS-UI-019 后厨状态失真 | ✅ FIXED（已上菜/已退款映射） |

---

## 7. 结论

**【FAIL】— 打回，禁止联调。**

修复质量真实且大幅提升（R2 的 11 项 P1 中 9 项已实测修复），但：
1. **P1-POS-SHIFT-024（开班身份伪造）** 是本轮攻击新打出的越权红线——交班锁了门，开班没锁；
2. **P1-POS-SHIFT-025（closing_cash 口径）** 是资金字段语义错误——每班对账将短款一个备用金额度；
3. 3 项 P2（响应假价回显 / cancel_time 空 / 权限声明与执行矛盾）。

以上 5 项清零后 R4 仅复测 Test 3 全场景 + 抽查 Test 1/2/4 即可放行《准予联调》。

## 8. 取证索引

- token：`tmp/pos-audit-uc1.token`（iat=1793212146, roles=cashier, 47 permissions）、`tmp/audit-admin-kds.token`
- 订单/班次：T20261029001（篡改价探测单）、T20261029002（退款联动单）、T20261029003（交班现金单）；shift 553/554/555
- 流水：FF202610290008（退款）/ FF202610290009（现金收入）
- 类编译：2026-10-29 0:12:02–04；JVM 28956 启动 0:12:27
- 关联：R1 `docs/quality/audit-fullchain-pos-20261028.md`、R2 `docs/quality/audit-fullchain-pos-20261028-R2.md`
