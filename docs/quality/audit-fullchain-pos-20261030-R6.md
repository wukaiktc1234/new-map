# R6 穿透式审计报告 — 首席审计官（资损/状态机/安全/基建/交互五维）

> **产物编号**：AUDIT-FULLCHAIN-POS-20261030-R6
> **性质**：独立审计。只信抓包、a11y、SQL 与代码证据；开发自测叙事一律视作待证命题。
> **审计时间**：2026-10-30 00:15–00:50（凌晨收班窗口）
> **环境**：后端 PID 29608（JVM 2026-10-29 23:27:36，:8081 LISTENING）；类编译 23:51:09（> JVM 启动）——裁决：23:51 后源码零改动（backend/src 最新 mtime 23:22:35），23:51 仅为构建产物时间戳，**运行代码=源码 ✓**；但「构建后未重启」的陈旧痕迹会误导审计据此记 P2-DOC。
> **版本基线**：两日 8 commit（c326981→7d56531）：异常单闭环 V1.0、TTL 关单、权限目录棘轮、测试资金冲销、DBGUARD。
> **纪律遵守**：全程零 UPDATE/DELETE；对 UPDATE 谓词的攻击后果以「只读 WHERE 仿真」机械等价论证。

---

## 0. 审计体检表（P0/P1 问题清单）

| 编号 | 等级 | 领域 | 一句话 | 现场/证据 |
|---|---|---|---|---|
| **P0-A** | **P0** | 账务 | `fund_flows.balance_after` 链条 12+2 处断裂、独立重放漂移 35940 分——**账本不可追溯**，下游两处契约（期末封账/银行对账推导）建立在坏账本上 | SQL 断点集合 + 重放差；`FinancePeriodService:20`、`BankAccountReconciliationServiceImpl:133` |
| **P0-B** | **P0** | 状态机/资金 | `orders.shift_id` 建单未绑定（近两日 148 笔 NULL）→ **异常单交班闸形同虚设**：cashier 带真实 pending 单直调交班 **HTTP 200 关班成功**；3 笔补收款 FF 6600 分**无班次归属**——每班钱箱对账将出现假长短款 | 现场：开班 676→种单 T20261030002→`GET /pos/shift/exceptions`=[]→`POST shift/end`=200 关班；`ExceptionOpsMapper.casMarkPaid` 谓词诈骗链条 |
| **P0-C** | **P0** | 资损 | `casMarkPaid` 谓词只防 `payment_status<>2`，**不防「已作废(3)/已 TTL 关单(EXPIRED_AUTO)」**：对 122 笔已关单+库存已回补的订单，权限话事者可经 API 补收款——**收了钱、库存已回补、无商品交付**的账务黑洞 | 只读谓词仿真 `simulate_casMarkPaid_on_voided_plant=1`、`...on_ttl_expired=1`；`ExceptionOpsMapper:26-28` vs `TtlOrderCloseMapper:43-46` |
| **P0-D** | **P0** | 审计身份 | `PosExceptionController:47/:92` 的 `X-Operator-Id` 请求头可**顶替审计主表的 operator_id**——操作人字段可被客户端任意伪造 | 实测：void 带 `X-Operator-Id:7777777` → `exception_ops.operator_id=7777777` |
| P1-1 | P1 | 安全覆盖 | **71/279 controllers 无任何 PreAuthorize**；authenticated 用户可跨域进入审批/资金敏感 controller（appeal 探针：cashier 直达业务层 body=500）；`SecurityConfig` 大量 GET 域 permitAll（kitchen/tray/asset/label…）+ `:82-83` 的 `securityEnabled=false` 一键全裸开关 | controller 扫描表 + 探针；AppealController 无注解 |
| P1-2 | P1 | 账务口径 | 异常单补收款**资金已入 bank 但班次统计两套口径不同源**：`countShiftOrders` 按时间窗、`countPendingExceptionOrders` 按订单 shift_id——绑定缺口下两者都漏水（统计=0 而钱在 bank） | `PosAuthServiceImpl:956-993` vs `:64-69` |
| P1-3 | P1 | 幂等 | 同单第二操作（不同 key）触发 `exception_ops` 部分唯一冲突 → 全事务 500「系统繁忙」（现场复现于已 void 单 resolve）；语义应为 409/replayed | curl 复现：HTTP 200 + body 500 |
| P1-4 | P1 | 库存对称 | 两套关单路径不一致：**TTL 关单回补 foods.stock（T1 822/…），人工 void 不回补**——作废单的下单扣减就地蒸发，库存虚减、毛利率失真 | `TtlOrderCloseTxBean:52-73`（有回补）vs `PosExceptionTxBean:212-216`（仅 metric skip） |
| P1-5 | P1 | 防呆声明 | **DBGUARD 实际接入 15/27 个含写脚本，12 个无 guard**（含 e2e_setup / p1_acceptance / p2_acceptance / seed_p3_fixture 等）——开发通报「14 个全部接入」与现状不符 | `scripts/qa` 扫描：write-scripts=27 with_guard=15 |
| P1-6 | P1 | 语义/协议 | HTTP 真语义仅 `PosExceptionController` 一族落地（真 409/422）；其余全系统仍是 **HTTP 200 + body code=403/500**（GlobalExceptionHandler 形态），监控/网关双轨受害 | R5 历史 + 本轮 `appeals` HTTP200/body500 探针 |
| P1-7 | P1 | 遗留进程 | `OrderTimeoutTask`（旧表、STATUS_CANCELLED=5）与 `TtlOrderCloseJob`（新表、取消=3）**双定时任务并存**：取消语义 3/5 分裂 + 旧任务仍在 `@Scheduled(fixedRate=300_000)` 活跃 | `OrderTimeoutTask:45,:62` vs `TtlOrderCloseJob` + 双 CAS 注释 |
| P1-8 | P1 | 数据卫生 | 幽灵班次回潮（**active shifts=11**）、prod 表残留开发自测幂等键（`gate-409`/`probe-532`/`T2-KEY-*`）、E2E 样本流水（E21/BL7 于账户 24 无 category 8 豁免标记） | SQL 证 |

（P2 汇总：`respond()` 用正则从 message 反解析 pendingExceptionCount（`PosExceptionController:135-141`）；void reason 无枚举校验；token 存 localStorage（XSS 面）；匿名 `/v1/device-registrations/activate` 疑似无频率限制；构建后未重启的陈旧类时间戳。）

---

## 1. 全链勾稽结论（维度 1）

**正向样本全绿**：近 30 单 paid 单三方恒等（`three_way` 抽样：final_amount == Σpayment_records == Σ(FF d1−d2)，含 W081/123/124）✓；退款单 FF/退款/库存回补此前 R3~R5 均已验证 ✓。

**但账本链条坏在历史段**：
- 断点集合（account 20）：5729/5737/5739/5741/5872/5886/5902/5913/5924/5935/6019/6045；account 24：6060/6061。
- **铁证 5727/5729/5737/5739**（10-27 17:40~18:20）：四笔 `方向=1 amount=2500` 的「POS销售入账」，`balance_after` 全部 =31502——**余额四次纹丝不动**；随后的 5741 缴款（d=2 amount=2500）at 26502=31502−2500 **正确推进**→ 当时的 d=1 写入路径未走余额原子器。
- **独立重放**：acc20 隐含期初 29002 + Σ净 17260 = 46262 ≠ 尾节点 82202（**Δ=35940 分**）；acc24 Δ=−91356 分——`balance_after` 身上的链叙述与净额重放**无法对齐**。
- 开发 A7 判据（「最新非补账流水 after==账户余额」）**只验尾节点**：尾节点在 6056 冲销时由 AtomicBalanceUpdater 双写保持一致，**中间 14 处断裂被「最后一条永远对」掩盖**——这是「跑绿就行」的教科书标本。
- 好消息：`FundFlowServiceImpl:164-175` 现行代码**强制走 AtomicBalanceUpdater**（R6 现场补收试验中 bank 与 FF 序列保持一致性的能力将在下轮修复复验中确认），断裂集中于 10-27~10-29 旧路径时期；**但这不能免除历史修复责任**——两个下游（期末封账契约、银行对账推导）都读此链。
- **测试冲销（7d56531）自评成立**：remark 扫描干净（`dirty_ff=0`）、6045/6046 保留为痕迹合理、6056 category=8 署名合规。**但 E2E 样本流水（E21-ORDER-1/S3-E21-REF，账户 24）不用 category 8 标注**，未来以 remark 扫描只有恰好关键词才捕获——**建议统一测试产物打标规则**。

## 2. P0 复现步骤与架构重构建议

### P0-B 交班闸失效 + 班次归因黑洞（现场完整复现）

**复现步骤**（2026-10-30 00:34，cashier token）：
```
1) POST /v1/pos/auth/shift/start {terminalId:AUDIT-R6-CA, openingCash:500}     → 200 shiftId=676
2) POST /v1/pos/orders/order（正常收银链路，¥22 现金，不支付）                  → 200 T20261030002
3) GET /v1/pos/shift/exceptions?terminalId=AUDIT-R6-CA                          → 200 data:[]  ← 闸看不见该单
4) POST /v1/pos/auth/shift/end {terminalId:AUDIT-R6-CA}                         → 200 code=0 「班次结束成功」
   closingCash=500.00 totalAmount=0.00 —— pending 单在场却被放行关班
5) DB: orders(T20261030002).shift_id=NULL; 开发自测高频期近两日同型 148 笔
```
**根因**：下单链路没有任何环节绑定 `orders.shift_id`；闸 SQL（`countPendingExceptionOrders`/`listShiftExceptions`）以 `o.shift_id=?` 为硬过滤 → 真实单据被闸「选择性失明」。开发 S3 自测之所以绿，是**夹具里手工赋予了 shift_id**——真实链路从未走过。这就是背景所述「**假前提开发**」的本轮实例。

**重构建议**：
1. **建单即归班**：下单 service 内强制 `shift_id = findActiveShiftByTerminalId(...)；若无在班班次 → 400「请先开班」`（收银业务本就不应在无班态收单）。配 CI 一条「shift_id NOT NULL 占比 ≈ 1」的数据哨兵。
2. resolve/void 增加订单-班次一致性校验（订单 shift 与操作员在班班次一致性），FF 统计/闸全面改用同一 `shift_id` 事实源（`countShiftOrders` 时间窗口径 仅保留为兼容显示层）。
3. 交班端点做**服务端硬闸兜底**：`pendingExceptionCount>0`（含本设计稿口径的分类）→ 真 409（当前无闸实现于 endShift 路径——S3 之后 endShift 也没有独立校验，因为它依赖的 shift_id 是坏数据）。
4. 存量 147 笔 shift_id=NULL 订单：写数据修复迁移按「create_time + terminalId 映射」回填可确定者，其余挂「未归班」独立桶并纳入对账说明。

### P0-C 已关单可被补收（代码级 P0，未在 prod 主动触发资损）

**复现（只读谓词推理，符合审计纪律）**：
```
casMarkPaid: UPDATE orders SET payment_status=2, paid_amount=?, payment_method=?
            WHERE order_id=? AND COALESCE(payment_status,0)<>2 AND deleted=0        (ExceptionOpsMapper:26-28)
TTL 关单:    UPDATE orders SET order_status=3, cancel_reason='EXPIRED_AUTO' ...
            WHERE order_id=? AND order_status=0 AND COALESCE(payment_status,0)<>2   (TtlOrderCloseMapper:43-46, 已 122 笔)
→ 关单后 payment_status 恒 0；且关单价未写 exception_ops（无 partial-unique 阻挡）
→ 只读仿真 SELECT count(*) WHERE order_id=<TTL单或void单> AND COALESCE(payment_status,0)<>2 AND deleted=0
   = 1 → 该 UPDATE 将改写已关单
→ 唯一挡板=前端不显示已关单（本项目 R5 已确立：前端可见性不是安全控制）
```
**风险叙事**：店长/收银员可对「顾客放弃订单」的已关 单重新收钱（FF 入账），但库存已在关单时回补——**钱进了账、货在前端异常列表中消失、且无任何对账钩子把「补收」与「是否实际出餐」挂钩** → 资损/纠纷两不认。另：人工 void 路径的二次操作因 partial unique 冲突 500 回滚（P1-3），侥幸无资损，但语义契约混乱。

**重构建议**：
1. 状态机谓词**函数化单源**：抽出 `isResolvable(order)` 权威 SQL（建议 `order_status=0 AND COALESCE(payment_status,0)<>2` ——与 TTL 谓词对称），resolve 用它、TTL/void 同族断言用它；单元测试锁死「已关单/已作废/已退款不可 resolve」三例。
2. resolve 前加显式前置 SELECT：订单 status 必须可收态，否则真 409 + currentStatus（结构化字段，别用 message 正则）。
3. **作废路径补齐库存回补**（复用 TTL 同源 food_id→food_code→addStock），TTL/void/退款三路径共用一个 `recoverStockForOrder`，P1-4 同步关闭。
4. 已关单如需真实收钱（顾客回头补付），业务上应引导**重新开单**，异常单只保留「作废」出口——把这条写成产品决定并登记 BLOCKED_PRODUCT_RULE。

### P0-A 账本链条修复

**重构建议**：
1. **A7 判据升级为「全链重放」**：每日 job 按账户逐行重放 `balance_after[i] = balance_after[i-1] ± amount`，断点=0；任一 break 即告警（当前 14 处必红）。
2. 写**修复迁移**（只补历史段）：以 `bank_accounts.balance` 为锚重放 6019 以前所有 `balance_after`（AtomicBalanceUpdater 时代之前的旧数据），并保留 `repair_note` 审计列署名 owner。
3. 新代码侧增加**双重保险**：`AtomicBalanceUpdater` 已有版本号——建议在 create() 里断言「该账户最后一行 FF.after == 更新前 balance」（事务内校验，catch 不匹配抛 409），让「跳写入路径」在未来无处藏身。
4. 期末封账/银行对账推导两个下游在修复前**禁用 after 字段**，改用 Σ 聚合口径。

### P0-D 审计身份注入

**复现**（已实测）：
```
POST /v1/pos/exceptions/<新单>/void  Header: X-Operator-Id:7777777  body{reason:OTHER,note:...}
→ 200; DB: exception_ops 最后一行 operator_id=7777777（伪造值落审计主表）
```
**重构建议**：删除 `X-Operator-Id` 读取；operator 一律 `SecurityUtils.getCurrentUserId()`（与 FF `create_user_id` 同 conventions——.tx 内 ：114/:132 已经如此）。若确有「代理操作」业务场景，需配置侧受控白名单 + 审计双签，而不是任意 Header。

---

## 3. 系统「跑绿假象」论据汇总（为什么不能只看自测）

| 开发叙事 | 审计事实 |
|---|---|
| "S3 交班闸 409 且 pendingExceptionCount=1" | 真实链路 orders.shift_id 未绑定 → 闸失明，endShift 200（P0-B 现场复现） |
| "DBGUARD 14 个含写操作脚本全部硬拦截" | 实测 27 个写脚本仅 15 接入 guard，12 个裸奔（含 e2e_setup/p1_acceptance）（P1-5） |
| "A7 最新非补账流水与账户余额 matched" | 只验尾节点；链内 14 处断裂、重放差 35940 分被 tail-write 掩盖（P0-A） |
| "T2 并发恰好一个成功无双收" | 同单**二次 void** 的幂等重放路径 500 冲突形态（P1-3）；TTL 关单单可被补收未纳入自测（P0-C） |
| 部署通报 PID 18548（R5 期）→ 今 23:51:09 类时间戳 | 通报口径漂移 ≠ 实际部署状态；QA 必须以探针为准（P2-DOC 持续） |

**共性根因**：自测的夹具构造（手工赋 shift_id、只看尾节点、只测正向键）与真实链路存在系统性偏差。按 production-collab 协议，回归基线必须**走真实前端/API 链路**并含反向与并发用例。

## 4. 健康度评级

```
维度1 资金一致性   ★★☆☆☆  正向三方一致✓；账本链坏（P0-A）+ 班次黑洞（P0-B 分支）→ 不可出具「账实相符」
维度2 状态机/异常流 ★☆☆☆☆  交班闸失效（P0-B）、关单可复收（P0-C）、幂等语义破（P1-3）、двух取消语义分裂（P1-7）
维度3 权限防线     ★★☆☆☆  新端点权限码+幂等双读+HTTP真语义合格✓；72 控制器无闸+默认开关隐患（P1-1）
维度4 健壮性/隔离   ★★★☆☆  TTL 三件套（advisory/分批/CAS）设计合格✓；DBGUARD 意识好但覆盖率 15/27（P1-5）
维度5 UI/UX        ★★★☆☆  tooltip/双口径文案/置灰已处理/错误文案统一机制存在✓（Snapshot 复核抽样）
```

**综合评级：C+（黄偏橙）— 禁止扩大生产使用；现有收银试点仅可在「店长每日人工复盘 fund_flows+bank_accounts」的人工兜底下继续。**

放行条件（两阶段）：**阶段一**（P0 清零）：shift 绑定落地 + 交班硬闸复验 + resolve 谓词函数化 + X-Operator-Id 移除 + FF 链修复迁移；**阶段二**：P1-3/4/5/7/8 收编进任务池，回归基线（含反向/并发用例）进 preflight。

## 5. 取证索引

- tokens：`tmp/pos-audit-r6-ca.token`（AUDIT-R6-CA）/ `tmp/audit-r6-ad.token`
- 实验物：班次 676（已关，closing=500.00，暴露交班闸失效）；pending T20261030002（ métodos上游单据 shift_id=NULL 样本，TTL 15 分钟将自行消化）；void 探针单 O1793287678113（operator_id=7777777 注入证据）
- 关键代码锚点：`ExceptionOpsMapper:26-28/45-47/63-69`、`PosExceptionTxBean:74-237`、`PosAuthServiceImpl:950-1043`、`TtlOrderCloseJob/TtlOrderCloseMapper`、`PosSaleFinanceRecorder:165-199`、`FundFlowServiceImpl:164-195`、`SecurityConfig:82-123`、`PosExceptionController:45-150`
- DB 断点集合：acc20 {5729,5737,5739,5741,5872,5886,5902,5913,5924,5935,6019,6045}、acc24 {6060,6061}；重放 Δ：acc20 +35940 分、acc24 −91356 分
- 关联：R1–R5 报告（`docs/quality/audit-fullchain-pos-20261028*.md`）
