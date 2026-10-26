# 真实门店交接班流程 vs 系统设计 · 差异审计（2026-10-26）

> 只审计，不改代码/数据。每条结论贴代码位置或 SQL 实测。

## 一、备用金（找零金）

### 1.1 是否有备用金字段 —— 有，但没接到缴款单

~~~sql
-- information_schema 实测（ILIKE reserve/petty/float/opening_cash/start_cash）
open_close_store_records | opening_cash_count | bigint     -- 开店收银现金清点
pos_shifts               | opening_cash       | numeric    -- 班次开班现金（= 备用金）
shift_records            | opening_cash       | bigint
-- deposit_slips：0 命中（无任何备用金字段）
~~~

→ pos_shifts.opening_cash 就是备用金的暂存处，但 deposit_slips 完全没有承载它。

### 1.2 expected_amount 是否排除备用金 —— 未使用 opening_cash

~~~
DepositSlipServiceImpl.java:194   long expected = sumCashPayments(shift, storeId);
DepositSlipServiceImpl.java:900   private long sumCashPayments(PosShift shift, long storeId)
~~~
expected 只按 order_payment_records 现金支付重算（TD-03），不含备用金、也不减备用金。

### 1.3 actual_amount 录入入口 —— receive 接口

~~~
POST /v1/finance/deposit-slips/{slipId}/receive      body: receivedAmountFen
DepositSlipServiceImpl.java:349   .set(DepositSlip::getStatus, ST_RECEIVED)
DepositSlipServiceImpl.java:331-341  LambdaUpdateWrapper 写 actual_amount + difference_amount + difference_net_fen
~~~
无任何「请扣除备用金」提示（接口无该字段；本轮新建前端页面也未加）。

### 1.4 模拟场景结论 —— 会产生持续性「假长款」

~~~
expected = 本班现金收入 5000（不含备用金）
收银员按抽屉总额填 actual = 6000（5000 + 1000 备用金）
⇒ difference_amount = +1000（假长款）；difference_net_fen 同样 +1000（refund_component=0）
~~~
→ 每班固定金额假长款，金额恰等于备用金，属系统性口径错误。

**结论：需要修（P1）**。修法建议：
1. deposit_slips 增 reserve_fen（默认取 pos_shifts.opening_cash，注意单位）；
2. 新增 cash_counted_fen = actual − reserve（「应存现金」），difference_net_fen = cash_counted_fen − (expected − refund_component)；
3. 旧 difference_amount 不动（兼容历史报表）；
4. 前端 receive 录入处展示「备用金（自动带出，可改）」+「实存总额」+「扣除后应存」；
5. 断言：A39 增加 reserve 项；新增 A42（判定）reserve_fen 与班次 opening_cash 偏差 = 0。

## 二、清点人角色

### 2.1 receive 当前绑定 —— 只有 ROLE_TELLER

~~~sql
ROLE_TELLER | finance:deposit-slip:deposit | deleted=0
ROLE_TELLER | finance:deposit-slip:receive | deleted=0
（close / verify：0 行绑定）
~~~

### 2.2 ROLE_TELLER 实有用户 —— 2 个

~~~sql
SELECT role_id, count(*) FROM user_roles WHERE role_id='ROLE_TELLER' AND deleted=0 GROUP BY 1;
→ ROLE_TELLER | 2
~~~

### 2.3 门店无专职出纳时的流转 —— 卡流程

| 状态转移 | 需要权限 | 当前可操作角色 |
|---|---|---|
| DRAFT → SUBMITTED | finance:deposit-slip:submit | 已绑（收银员/店长） |
| SUBMITTED → RECEIVED | finance:deposit-slip:receive | 仅 ROLE_TELLER |
| RECEIVED → DEPOSITED | finance:deposit-slip:deposit | 仅 ROLE_TELLER |
| close / verify | finance:deposit-slip:close / verify | 无任何角色 |

→ 真实门店常见「店长清点」，店长无 receive 权限 → 缴款单永久停 SUBMITTED → 阻塞结账（RK-8 会因 DRAFT/SUBMITTED 阻塞）。

**结论：需要 Owner 拍板**。建议：
- receive + deposit 绑 ROLE_STORE_MANAGER（保留 ROLE_TELLER）；
- close / verify 明确归属（ROLE_FINANCE_MANAGER 或 FINANCE_DIRECTOR），否则是死码；
- 或增加门店配置「是否有出纳」，无出纳门店降级为店长清点。

## 三、存行方式

### 3.1 DEPOSITED 是否支持多单汇总 —— 一单一次（1 单 ↔ 1 笔银行流水）

~~~
DepositSlipServiceImpl.java:381   requireStatus(slip, "存行", ST_RECEIVED);
DepositSlipServiceImpl.java:383   throw new BusinessException(…, "bankTransactionId 不能为空");
DepositSlipServiceImpl.java:397   BankTransaction txn = bankTransactionMapper.selectById(dto.getBankTransactionId());
DepositSlipServiceImpl.java:434-443  条件更新 RECEIVED → DEPOSITED，同时落账户/流水/时间
~~~
存行必须传入已存在的 bankTransactionId，每张缴款单各自绑定一笔；代码中没有任何「多张缴款单共享同一 bankTransaction」的关联表或校验。

### 3.2 一天 3 班 3 单 → 生成几对内部转账 —— 按单生成

~~~sql
SELECT count(*) FROM fund_flows WHERE deleted=0 AND flow_category=5;   -- 内部转账/存行
→ 6 条（当前库 2 张 DEPOSITED 单）
~~~
每张单 2 笔（现金户出、银行户入）× 2 单 = 4，历史另 2 笔 → 与「按单生成」一致；3 张单会生成 3 对（6 笔）。

### 3.3 次日汇总存一次 → 会产生单边假差异

银行侧 1 笔进账（= 3 单合计），企业侧 3 笔入账 + 3 笔出账 → 金额相等但笔数不等；同额 1:1 贪心配对无法把 1 笔银行进账配到 3 笔企业入账 → 产生多笔单边未达项（假差异）。

**结论：需要修（P1，仅当门店实际汇总存行）**。修法建议：
1. 新增存行批次 deposit_batch(batch_id, account_id, total_fen, txn_id, business_date) + deposit_slips.deposit_batch_id；
2. 部分存行：deposit_slip_allocation(slip_id, batch_id, amount_fen)；
3. 对账以批次总金额与银行进账 1:1 配对，批次内再拆到单；
4. 内部转账流水改为「批次级 1 对」，不再按单生成。

## 四、汇总

| # | 差异 | 现状 | 后果 | 结论 |
|---|---|---|---|---|
| 1 | 备用金未从 actual 扣除 | opening_cash 存在但 deposit_slips 无字段、expected 不减、前端无提示 | 每班固定假长款 = 备用金（算错账） | 需要修（P1） |
| 2 | 清点人只有 ROLE_TELLER | receive/deposit 仅绑 TELLER；close/verify 零绑定 | 无出纳门店流程卡死、阻塞结账 | 需要 Owner 拍板 |
| 3 | 存行按单生成、无汇总批次 | store 需 bankTransactionId，一单绑一笔 | 汇总存行产生 N 笔 vs 1 笔单边假差异 | 需要修（P1，若实际汇总存行则必修） |

## 五、本轮未取证项

- pos_shifts.opening_cash 单位（列 numeric 无 scale）：按分还是按元未知，若按元接 reserve_fen 须 ×100；需与 closing_cash（同为 numeric）一并定性；
- close / verify 是否有代码调用点（死码 vs 待接线）未核；
- bank_transactions 是否已有 slip_id 反向关联（影响批次改造）；
- 真实门店「次日汇总存行」比例未知，决定差异 3 的优先级。


### 1.5 接班时输入备用金的环节（补充审计）

**① 字段存在：pos_shifts.opening_cash（单位=元）**

~~~sql
-- \d pos_shifts 实测
opening_cash | numeric(10,2) | not null | default 0.00     ← 备用金（元，与 closing_cash 同尺度）
closing_cash | numeric(10,2) |                          ← 交班现金（元，已定性为「仅展示」）
total_amount | numeric(12,2) | not null | default 0.00
handover_remark | text
~~~

**② 接口层有「输入备用金」：POS 开班接口**

~~~
PosStartShiftDTO.java:30        private BigDecimal openingCash;
PosAuthServiceImpl.java:185     String shiftType, BigDecimal openingCash)
PosAuthServiceImpl.java:210     shift.setOpeningCash(openingCash != null ? openingCash : BigDecimal.ZERO);
~~~

**③ 前端没有「输入备用金」界面**

~~~text
grep "openingCash|opening_cash|备用金" frontend/src  → 仅 2 处命中，且都是知识库文案：
  kb-003-management.ts:22   「…各币格内有足够零钱备用金」
  kb-003-management.ts:172  「- 期初备用金金额」
~~~
→ POS 前端**没有输入框**，运行态 opening_cash 恒为默认 0.00（除非直接调接口）。备用金交接在**界面上完全不存在**，仅数据模型与后端接口预留。

**④ actual_amount 是否用 opening_cash 计算 —— 没有**

~~~
DepositSlipServiceImpl.java:194   long expected = sumCashPayments(shift, storeId);   ← expected 不含备用金
DepositSlipServiceImpl.java:900   private long sumCashPayments(...)                   ← 现金支付重算，未引用 openingCash
receive 接口：body 只有 receivedAmountFen，无 reserve 入参、无「请扣除备用金」提示
~~~

**⑤ 结论：actual 只能按「抽屉总额」填 → 系统性假长款（非偶发）**

~~~
场景：备用金 1000 元 + 本班现金净收入 5000 元
  expected_amount = 5000（不含备用金）
  收银员只能按抽屉总额填 actual = 6000（系统里看不到备用金，也无提示）
  ⇒ difference_amount = +1000「长款」；difference_net_fen = +1000
  ⇒ 每一班固定出现 1000 元假长款，金额恰等于备用金
~~~
**额外风险**：若某门店确实通过接口写入了 opening_cash（例如 1000），但前端不展示，收银员**无从知道**应填 5000 —— 现场只会按总额填，差异依旧。

**⑥ 本条定性：需要修（P1），且优先级高于 1.4 的结论** —— 因为「界面缺失」+「expected 不含备用金」是**双重缺口**：即使补了提示，若 opening_cash 无输入入口，提示的数字也不可信。

