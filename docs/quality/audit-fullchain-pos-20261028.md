# POS 全链路真实业务审计报告（开班→点单→支付→后厨→退单→交班→对账）

> **产物编号**：AUDIT-FULLCHAIN-POS-20261028
> **审计人**：QA 架构（OpenCode 会话 2）
> **审计日期**：2026-10-28 21:00–21:30（本地）
> **性质**：只读审计。未修改业务代码与数据；退单/交班为业务链路内的真实交易动作（已标注）。
> **环境**：后端 :8081（进程 5668，运行 `backend\target\classes`，JVM 启动 14:50，类编译 20:25）；POS 前端 vite :3001；PostgreSQL localhost:5432/food_traceability
> **验证码**：`pos-audit-uc1.token`（vcashier01/location 1）、`audit-admin-kds.token`（admin）
> **表编码**：支付方式新表口径 1现金 2微信 3支付宝 4银行卡 5余额（金额一律「分」）

---

## 0. 执行摘要

六个节点全部真实执行（N0 基线 → N1 开班 → N2 点单/支付 → N3 后厨出餐扣料 → N4 退单 → N5 交班 → N6 财务对账）。

| 节点 | 结论 | 关键证据 |
|---|---|---|
| N0 基线 | ✔ | 今日基线 59 单/¥2399.60（审计前） |
| N1 开班 | ✔ 一致 | shift 512/513 各 ¥500，UI↔API↔DB 一致 |
| N2 点单/支付 | ✔ 金额一致（有高危缺陷旁路） | T20261028002 ¥100 现金 / T20261028003 ¥50 微信，三方 100% 对上（分） |
| N3 后厨出餐扣料 | ✔ 算术一致（台账缺层） | 5 份×0.4 斤=−2.0 斤 #2012，inventory 33: 26.7→24.7 |
| N4 退单 | ✔ 财务一致 / ✗ 库存与后厨不联动 | refund #207 + FF202610280195；库存零回补、KO 仍 pending |
| N5 交班 | ✗ 错关班次 + 统计污染 | 交班关了 SYSTEM 补建班次 514，本人班次 512 仍 active |
| N6 全量对账 | ✔ 今日 100% 勾稽 | 收入 773.60 = 支付记录 = 流水；退款 520.00 = 退款记录 = 退款流水 |

**发布判定：FAIL（禁止放行）**。资损红线 3 条（价格信任客户端/售罄可下单、退款库存双口径、交班身份错配），阻断级前端断流 2 处（菜单 403、后厨 404）。

### 缺陷总量
- **P1 = 11**（含 3 条资损红线）
- **P2 = 6**（数据完整性/卫生/漂移）
- ** Corpora 设计缺口（BLOCKED_PRODUCT_RULE）= 2**（混合支付不存在、微信通道未接外部网关）
- 共 19 项编号：P1-POS-AUTH-001 … P2-POS-GW-017

---

## 1. 节点 N1–N6 实测记录（三方一致核验）

### N1 开班（✔）
- vcashier01 经 UI 开班：pos_shifts#512（opening_cash=500.00, terminal pos-localhost-1793191485342）；admin#513（terminal pos-localhost-1793191756929）。
- UI 显示 = API 返回 = DB 三方一致；`closing_cash` 交班前 NULL（正常）。

### N2 点单/支付（✔ 金额 / ⚠ 旁路缺陷）
真实订单两笔（均走 vcashier01 token，终端 AUDIT-UC1）：

| | 订单1 | 订单2 |
|---|---|---|
| 单号 | T20261028002 (O1793170243516/A167) | T20261028003 (O1793170243518/A168) |
| 明细 | 红烧土豆×2 + 链路复验菜×2 + QA1×1 | 红烧土豆×1 + 「测试菜品销售A」×1(实际解析为QA-D1) + QA1×1 |
| 应收 | 100.00（前端合计 ✓ 36+44+20） | 50.00 |
| 支付 | 现金（收 130 找 30） | 微信 |
| orders | final=10000, pm=1, ps=2 | final=5000, pm=2, ps=2 |
| pay_records | #1344 amount=10000 | #1345 amount=5000, txn **CSH1793192835636**（错前缀） |
| fund_flows | FF202610280193 acct20 +10000 → 47402 ✓ | FF202610280194 acct25 +5000 → 5000 ✓ |
| order_items | food 2×2 + 7×2 + 5×1 = 10000 ✓ | food 2×1 + 4×1(1200) + 5×1 = 5000 ✓ |

- **混合支付负测试**：对订单1 发 `amount=50` → 「支付金额与订单金额不一致」（拒）；前端无混付 UI。混付产品级不存在（BLOCKED_PRODUCT_RULE）。
- ⚠ **现金三字段**：请求带 cashReceived=130/changeAmount=30 → 落库 **NULL**。运行字节码（javap）确认含写入逻辑但 JVM 启动(14:50) 早于编译(20:25) 未重载 → 运行态缺字段（P2 漂移，资金三要素对账断链）。

### N3 后厨（✔ 扣料算术 / ⚠ 台账缺层）
- 状态机 pending → received(厨师E) → making → complete → **served**，ko.material_consumed=1，orders.order_status→2。
- 扣料溯源链两层：
  1) **配方真身是 `dish_recipes`（新表，12 行/8 菜）**，不是空的 `dish_inventory`。菜品 2/5/7 各挂「生菜 material 3，0.4 斤/份」→ 5 份合计 **−2.0 斤**；
  2) movement #2012：material 3，change −2.0000，balance 26.7→**24.7** = inventory#33（location 1）当前值 ✓。
- `material_archives`#3 名「生菜」✓，但**库存行 material_name='??'、unit='?'**（库房台账不可读）。
- **`material_consumption` 全库 0 行**：出餐只写 movement，不写耗料明细台账（断层）。
- movement 空洞 1817–2011：全表 166 行/max id 2012 → 序列跳号（回滚/缓存），非丢数；1773–1816 是「[系统修复]」ADJUST 补写流水（数据卫生污点，非业务）。
- 配方空盘чки的另一面：粒度只有 QA 「生菜0.4」假数据；菜单主数据双轨（dish_inventory / dish_recipe旧 / dish_recipes 新）。

### N4 退单（✔ 财务 / ✗ 库存·后厨）
对 T20261028003 全单退（admin 票权；收银员票 403 无权限——单独列缺陷）：
- `orders`: refund_amount=50.00, order_status=**5**, payment_status=2 ✓
- `order_refund_records`#207：type=1 整单，method=1 原路，status=3 完成，amount=5000 分 ✓
- `fund_flows`**FF202610280195**：acct25 direction=2 amount=5000 balance_after=**0** ✓（5000−5000）
- ✗ **库存零回补**：foods.stock 895/899/898/900 退款前=退款后（四行全不变）
- ✗ **后厨单未联动**：KO1793170243517 仍 `pending`（后厨会继续做单）
- ✗ inventory_movement 无变化（该单未出餐，正确）
- 幂等✓：二次退款 HTTP **409**（ConflictException「订单已全额退款」）
- 源码定性：`refundNewOrder` 未调用 `recoverStockForRefund`（旧路径 L374 有），也无 kitchen 通知 —— 双口径实锤。

### N5 交班（✗）
`POST /v1/pos/auth/shift/end`（vcashier01 token，terminal=AUDIT-UC1）返回：
```
shiftId=514 "系统补建" employeeId=SYSTEM openingCash=0 closingCash=null
totalAmount=773.60 totalOrders=19 现金723.60/微信50.00/支付宝0  status=completed
```
- ✗ **身份错配**：endShift 只按 terminalId 定位班次 → 关掉了「系统补建」班次**514**；操作人 vcashier01 真实班次 **512（pos-localhost-1793191485342）依旧 active**（收银员交班交不走）。
- ✗ closing_cash=**NULL**（落库未写，钱箱对账缺轴）。
- ✗ **统计口径污染**：窗口 13:05–21:24 跨班跨终端（countOrdersByTimeRange/sumPaymentBreakdown 的 terminalId 形参未用）→ 19 单/773.60 是**别的班次的单**；off-shift 收银员看不到自己的成绩。
- ✗ 不扣退款：微信 50 照计（实收应为 0）。
- ✗ 补建班次 startTime=13:05:37 = epoch 秒被当本地时间（真实应为 21:05）——8 小时漂移。

### N6 今日全量对账（✔ 100% 勾稽）
```
orders(今日,已付):   现金 18笔 723.60 / 微信 1笔 50.00          paid=77,360  refund=52,000
pay_records:         现金 723.60 / 微信 50.00                    = 77,360   ✓ =A
fund_flows IN cat1:  19笔 77,360                                = A ✓
fund_flows OUT cat2: 11笔 52,000 = refund_records 11笔 52,000     ✓ = A.refund
acct20: IN 72,360(=现金订单) OUT 79,500(缴款/采购等) last 47,402 ✓
acct25: IN 50,00 OUT 50,00 净0 last 0 ✓（微信退款闭环）
内部互转(cat5): 325.00 双向相等对冲 ✓
```
**昨日遗留「基线差 ¥325」不在今日窗口内**（为 28 日前旧数据缺口，另案）。
**但注意**：勾稽通过≠商业正确——金额对上的那 50 元里，含被「信任客户端价格」漏洞买走的 ¥30 售罄菜 ¥12 成交；且若当日漏洞被利用，账面永远自洽（同谋勾稽），必须靠库存三方核对兜底。

---

## 2. 缺陷清单（编号（production-collab 口径）

### P1 — 阻断/资金红线

| # | 编号 | 题目 | 复现 | 预期/实际 | 证据 |
|---|---|---|---|---|---|
| 1 | **P1-POS-AUTH-001** | 收银员菜单断流 403 | vcashier01 登录 POS → 动态菜单走 `/v1/product-center/*` | 实际：所有 item code=403 空 body，菜单空白；老通道 `/v1/pos/api/menu` 正常。根因 ROLE_CASHIER 无 DB role_permissions，POS_BASE_PERMISSIONS 仅内存注入 | 截图+network tab |
| 2 | **P1-POS-ORDER-002** | admin POS 下单 500 | admin 登录点单品单 | users.location_id=NULL → `缺少门店信息` 500。admin 不该能登录 POS 收银端或应兜底默认门店 | HTTP 报文 |
| 3 | **P1-POS-PAY-003** | 微信交易号前缀 **CSH**（现金） | 新表微信单 | transaction_no=CSH1793...；generateTransactionId 是旧编码 switch（0/1→QR, 4→BAL, default→CSH），新编码 2/3/4/5 全错位 | order_payment_records#1345 + 源码 L488-496 |
| 4 | **P1-POS-ORDER-004（资损红线）** | 价格信任客户端+售罄校验缺失 | 以 body {"id":"FD260926002","name":"测试菜品销售A","price":12} 下单 | DB：FD260926002=**QA-D1-售罄验证菜（¥30, status=2）** → 以 ¥12 成交（unit_price=1200 落我传的价），foods stock 899→899… id4 库存被扣（899），真实菜品 id8 不动；order_items.product_name='测试菜品销售A' 却 food_id=4（名/ID 失配） | SQL + 请求体 |
| 5 | **P1-POS-REFUND-005（资损红线）** | 新表退款不回补库存 | 退款 T20261028003 后查 stock | 四个 food stock 全不变；旧路径 (L374 recoverStockForRefund) 有回补，新路径无 → 双口径；也未联动 kitchen | SQL before/after |
| 6 | **P1-POS-REFUND-006** | 退款后厨单不联动 | 同上 | kitchen_order 仍 pending，后厨继续做单（食材白白消耗/无人取餐） | ko_after 查询 |
| 7 | **P1-POS-SHIFT-007（资损红线）** | 交班错关他人/系统班次 | vcashier01 交班 terminal=AUDIT-UC1 | 关了 SYSTEM 补建班次 514，本人 512 仍 active；closing_cash 不写库；交班无 operator 校验 | API 返回 + pos_shifts |
| 8 | **P1-POS-SHIFT-008** | 交班统计跨终端+不扣退款 | 同上 | 19 单/773.60 全是别班别终端；微信 50 已退仍计入收入 | API返回 vs SQL |
| 9 | **P1-FRONT-KDS-009** | 前端后厨全 404 断流 | POS `api/kitchen.ts` 与 frontend-kitchen `kitchenOrderApi.ts` 调 `/v1/kitchen-order/*` | 后端仅 `/v1/kitchen/orders/*`（KitchenOrderController）→ 「资源不存在」404，两套前端后厨看板死 | 4×404 报文 + grep |
| 10 | **P1-POS-REFUND-010** | 收银员无退款权限但也无人能走 UI | vcashier01 调 refund → 403；admin 无 location 但却持退款权限 | 权限矩阵缺陷(与 #2 互动)：能开班的不能退、能退的开不了班 | 两 token 实测 |
| 11 | **P2→P1** | 附加：微信/支付宝「新表支付」后端直接记成功（无二维码无回调） | 微信单 200 code=0 秒成功 | 外部支付网关未接，交易号还是 CSH 假号 —— 资金入账凭证断链 | API 响应 |

### P2 — 数据完整性/运行漂移

| # | 编号 | 内容 | 证据 |
|---|---|---|---|
| 12 | **P2-POS-PAY-011** | cash_received/change_amount 落库 NULL（运行 JVM 未重载 20:25 编译类；代码已含逻辑）→ 前端找零数据进不了库 | POST body+SQL+`jcmd` classpath+javap |
| 13 | **P2-DATA-RECIPE-012** | 配方三轨并存（dish_inventory 0 行 / dish_recipe旧 / dish_recipes新 12行8菜 QA 数据）；inventory 18 行污染（'??' 名、'?' 单位）；material_archives#3=生菜 但库存行名 '??' | 汇总 SQL |
| 14 | **P2-DATA-CONSUM-013** | material_consumption 全库 0 行：出餐只写 movement 不写耗料台账 | SQL count=0 |
| 15 | **P2-DATA-SHIFT-014** | pos_shifts 31 行中 **12 条 active 残留**（307/309/405/420/472/473/480/489/510/511/512/513）；E2E 把「班次」当一单一座（T-E2E-HIK…C9/T3 每单一班）→ 交班基线原生污染 | SQL |
| 16 | **P2-DATA-TIME-015** | 「系统补建」班次 startTime 13:05:37（epoch 秒当本地时间，真实 21:05），8h 漂移 | shift514 vs 日志时间 |
| 17 | **P2-POS-PAY-016** | 微信支付补开在：`amount` 差 50 分也拒（对）但 cash 欠支持「多笔合成一单」—— 部分支付/定金/混付产品级不存在（BLOCKED_PRODUCT_RULE） |付费负测试 |

### 设计缺口（BLOCKED_PRODUCT_RULE）
- **BPR-001**：混合支付（一单多方式/多笔）产品级不存在（DTO 单 method + 前端无 UI）。
- **BPR-002**：扫码支付外部网关未实现（假交易号质保断裂）。

---

## 3. 整体业务健康度评估

### 3.1 六节点健康矩阵

| 能力域 | 等级 | 依据 |
|---|---|---|
| 资金账本（今日勾稽） | **A-** | 今日 orders/payment/refund/fund_flows 100% 勾稽；409 幂等；balance_after 可回溯 |
| 支付可用性 | **C-** | 现金单可走通但找零不落库；微信假通道；混付产品缺失；交易号前缀错位 |
| 订单一致性 | **D** | unit_price 可被客户端任意改写；售罄商品可下单；name/food_id 失配（张冠李戴） |
| 库存红线 | **F** | 退款零回补（新表路径）；配方主数据三轨+污染；库存行名不可读；耗料台账空转；foods.stock 在「未出餐已退款」时也不回 |
| 后厨协同 | **D** | 状态机可用；但两套前端 404 断流；退款不通知；chefName 可被前端任意串改 |
| 交班/日报 | **D-** | 身份错配（关别人班）；统计全局窗口；不扣退款；closing_cash 空；12 条残留班次继承污染 |
| 角色权限 | **D** | 收银员看菜单 403 / 无退款权；admin 能退但不能开单；权限矩阵互相矛盾 |

**综合：FAIL —— 禁止放行生产。**

### 3.2 修复优先级（P0 熔断清单）
1. **下单服务端定价与售罄校验**（P1-POS-ORDER-004）——资损直接路径。
2. **交班身份绑定**（P1-POS-SHIFT-007/008）：`WHERE employee_id=当前人 AND terminal_id=? AND status='active'`；写 closing_cash；统计限定本班次 terminal；扣除退款。
3. **退款回补库存+后厨联动**（P1-POS-REFUND-005/006）。
4. **权限矩阵回填**（P1-POS-AUTH-001/002/010）：ROLE_CASHIER 的 role_permissions 落库；admin 登录 POS 注入门店或禁登录。
5. **前端后厨改址**（P1-FRONT-KDS-009）：`/v1/kitchen-order/*` → `/v1/kitchen/orders/*`；两端同改。
6. **交易号编码加新表 case**（P1-POS-PAY-003）。
7. 重启后端使 cash 三字段生效（P2-POS-PAY-011），并运维门禁：**class 编译时间必须早于 JVM 启动**。

### 3.3 数据卫生专项（不动代码但必做）
- 清理/归档 12 条 active 残留班次 + E2E 班次垃圾（307–509）。
- inventory 18 行名称/单位修复（'??'/'?'）；菜谱主数据收敛到 dish_recipes 单轨。
- movement 系统修复流水打标记源（ADJUST [系统修复]）已可追溯，保持。

---

## 4. 取证索引

- tokens：`P:\my-new-project\tmp\pos-audit-uc1.token`、`tmp\audit-admin-kds.token`
- 关键行号：PosOrderPaymentServiceImpl L488-496(交易号)/L622-672(新表退款)/L374(旧库存回补)；OrderNewServiceImpl L1152-1206(严格 BOM)/L1220(配方来源 dishes_recipes)
- 运动/账：movement#2012、FF202610280193/194/195、refund#207、payment#1344/1345、shift#514
- SQL 快照均在本报告内分行内嵌（可重放）。
- 旧文件提示：根目录 `production-audit-report.md` 为 FIN-UI-AUDIT-003 历史产物，本次未覆盖；本报告即2026-10-28 全链路审计唯一事实源。
