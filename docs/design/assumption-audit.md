# 设计假设回溯审计（assumption audit）

> 范围：P0–P5 期间「基于报告描述采纳、但未做代码级/SQL 级验证」的设计假设。
> 方法：每条结论必须贴**代码位置**或 **SQL 实测**；无法取得证据的一律标 `待确认`，**不以报告描述代替证据**。
> 本轮**只审计**：未改代码、未改数据、未跑迁移。

## 1. 字段语义审计

| # | 假设 | 证据 | 结论 | 影响面 |
|---|---|---|---|---|
| 1.1 | `deposit_slips.expected_amount` = 班次窗口内现金支付合计，**不含**退款扣减 | `DepositSlipServiceImpl:186 long expected = sumCashPayments(shift, storeId);`；`:202-203` TD-03 注释「应交额以 `order_payment_records` 现金支付重算为权威」；退款写入 `order_refund_records`（不在 `order_payment_records`） | **已验证**（与设计 v2 §2.1 一致） | 恒等式必须新增 `refund_component_fen`，不能把 `expected_amount` 当净额 |
| 1.2 | `difference_amount` = `actual_amount − expected_amount` | `:205` 注释「AC-21 口径」；`:207` DRAFT `setDifferenceAmount(0L - expected)`；`:317-318` 收款时同口径 | **已验证** | 与 `difference_net_fen` 不重复，差值恒为 `refund_component_fen` |
| 1.3 | `actual_amount` 在 DRAFT = 0（非 null） | `:204 slip.setActualAmount(0L);`；`:339` 收款回显 | **已验证** | 恒等式无 null 风险 |
| 1.4 | `pos_shifts.closing_cash` 仅展示、不被计算消费 | grep `closingCash|closing_cash` → **16 处**：`DepositSlipServiceImpl:68-69`（短款不改）、`:217`（METRIC）、`:333`（红线注释）、`DepositSlipVO:20`、`PosShift:70/185`、`ShiftRecord:49/163-167`、`SchemaFixMigration:101`；未发现参与金额计算 | **部分验证**：未见计算消费，但 16 处**未逐行读完** | 若大屏/报表用它算现金，会与净额口径冲突 |
| 1.5 | `pos_shifts.total_amount` = 班次营业额（含非现金） | 本轮**未取到计算点** | **待确认** | 若实际只含现金，则与 `expected_amount` 重复 |
| 1.6 | `pos_shifts.closing_cash` 单位是「分」 | `SchemaFixMigration:101` 建列 **`DECIMAL(10,2)`**；`PosShift:70`/`ShiftRecord:49` 为 **`BigDecimal`** | **需修正 P1**：与「金额一律用分（long/BIGINT）」冲突；尺度与类型可能不一致 | NEW-19 一律不引用该字段 |

## 2. 金额单位审计

| # | 假设 | 证据 | 结论 |
|---|---|---|---|
| 2.1 | `fund_flows.amount` / `bank_accounts.balance` 为分（bigint） | `\d fund_flows` → `amount bigint`；余额经 `AtomicBalanceUpdater.addBalanceGuarded(long)` | **已验证** |
| 2.2 | `orders.refund_amount/paid_amount/total_amount` 尺度 | `refund_amount numeric(12,2)` **按分存**（P4-5 实测 `paid=1800,total=1800,refund_amount=1800.00`）；`paid/total` 为 bigint 分 | **已验证** |
| 2.3 | 全项目 ×100 / ÷100 / toFixed(2) / parseFloat 逐条判断 | backend grep → **171 处**命中。抽样：`PerformanceMonitorAspect:140`（百分比）、`RateLimitAspect:75`/`PrintTaskQueueManager:455`/`DeviceConnectionPool:31`（毫秒）——均与金额无关；**`RecruitmentApprovalDataServiceImpl:153`** 与 **`DailySettlementDataServiceImpl:192`** 为 `fen / 100.0`（元转换） | **待确认 P1**：171 处未逐条判定；2 处 `fen/100.0` 需确认是展示还是计算 |
| 2.4 | `recharge_records` 金额单位 | 前端 `RechargeManage.vue` 用 `parseFloat(principalAmount)`+`toFixed(2)`（元） | **待确认 P2**（未取 DDL 类型），NEW-23 前置 |
| 2.5 | `deposit_slips` 金额全为分 | `expected/actual/difference` 为 long 分；`closing_cash` 为 DECIMAL(10,2) | **部分验证** |

## 3. 时间语义审计

| # | 假设 | 证据 | 结论 |
|---|---|---|---|
| 3.1 | `deposit_slips.business_date` = 班次 end_time 的日期 | `DepositSlipServiceImpl:197-199` TD-36 + `setBusinessDate(shift.getEndTime().toLocalDate())` | **已验证** |
| 3.2 | `pos_shifts.start_time/end_time` 本地时间、窗口含端点 | 未取得 `BETWEEN`/闭区间语义证据 | **待确认 P2**：边界归属可能双算/漏算 |
| 3.3 | 可用流水 `create_time` 代替 POS 端 `refund_time` 配对 | 退款流水由 `PosRefundFlowRecorder` 写，`businessDate=LocalDate.now()`（**天粒度**）；`order_refund_records.refund_time` 存在但**未写入流水** | **需修正 P1**：设计 v2 要求以 `refund_time` 配对，但流水侧只有 `business_date`(天)/`create_time`(时刻)；用 create_time 在边界 N 分钟内可能误判 |
| 3.4 | 既有「时间窗匹配」SQL 字段用法 | ①退款↔银行退款：`ReconciliationStatementServiceImpl:513-534` 用 `business_date BETWEEN ps AND pe`（账户+期间）；②`FinancePeriodGuard#assertOpen(LocalDate)`；③缴款单 `sumCashPayments(shift, storeId)`（班次窗口，内部 SQL 未逐行核对） | **部分验证** |

## 4. 断言编号审计

| # | 项 | 证据 | 结论 |
|---|---|---|---|
| 4.1 | 判定 22 + INFO 13 = 35 条，A1–A35（A34 预留） | 头部清单 `:5-6`、`:125-129`；运行输出 22 PASS / 0 FAIL、35 条（13 INFO） | **已验证** |
| 4.2 | A34 占位是否与编号冲突 | 占位 `:1022-1031`（A32-DIST 后、A35 前），**不含 SELECT**；A35 `:1019-1041` | **无冲突** |
| 4.3 | A35 为计数型，无法区分「没数据」与「写对了」 | `count(*) WHERE flow_category=6` → rows=0，仅证明无该类流水 | **需修正 P2**：启用后升级为对拍判定（金额 ≠ 付款单金额的条数） |
| 4.4 | 是否存在「无数据必然 PASS」的判定项 | A30/A33 需 `refund_amount>0`、A32 需 `business_date < CURRENT_DATE-1` 才有样本 | **待确认 P2**：需补「分母行数」，否则 0 行无法区分 |
| 4.5 | A36–A39（计划）与既有断言重叠 | A36（无缴款单配对）vs A32（无银行退款且不在未清明细）：同一批现金户 B 类可能同时落入 | **需修正 P2**：A32 撤 `account_type=1` 后必须与 A36 互斥（银行户/现金户） |

## 5. 退款配对键变更的全链路影响

| # | 假设 | 证据 | 结论 |
|---|---|---|---|
| 5.1 | 引入 `refund_record_id` 后所有「按 order_id 判退款」处需同步 | `V20261026_003` 已建列+唯一索引；唯一写点 `PosRefundFlowRecorder:120`；`autoDetectItems` 按 `refund_record_id` 配对 | **部分验证**：`fund_flows`/`reconciliation_items` 已覆盖；`recon_differences`/`finance_audit_logs` 仍按 `order_id`/`target_id=item_id` 关联 → **待确认 P2** |
| 5.2 | 「一单只能退一次」的历史假设已消除 | `uk_fund_flows_refund_order` → `uk_fund_flows_refund_record(refund_record_id)` | **已验证**（E2E 30+20 分两笔通过） |

## 6. 决策证据等级盘点（近 12 次提交）

| 提交 | 变更 | 证据等级 |
|---|---|---|
| `c50e3a2` | NEW-19 v2（撤宽容窗/字段盘点/伪概念澄清） | 代码级 + **部分待确认**（total_amount、start/end 边界） |
| `4418cbe` | 字面量 1 → 常量；7 统一 | 代码级（grep 0 命中） |
| `1b7555b` | A34 预留占位 | SQL 级（行级统计 PASS=22/INFO=13；同行 SELECT=0） |
| `5ca6ae5` | NEW-19 v1 | **报告描述**（v2 已推翻 3 处）→ 需复核 |
| `8c857ba` | NEW-21 采购付款 2→6 | SQL 级（扫描：category=2 的 36 行全为真退款，疑似采购付款 0 行） |
| `ac51fa2` | A35 INFO | SQL 级 |
| `d8fd938` | A30 去等式 + 收窄 + 回填脚本 | SQL 级（回填前后行 + 幂等 exit=3） |
| `f7d8eec` | R5 唯一写点 | 代码级（grep 唯一写入点）+ E2E 19/19 |
| `f78d4dc` | A32 方案 B + cash-B 桶 | SQL 级（六桶=total）＋**待确认**（当前不可观测，预防性） |
| `f97b980` | e2e_refund_entries | E2E 19/19 |
| `1293cb9` | 512 条措辞 | SQL 级（512/512） |
| `1ea0d88` | e2e_period_guard | E2E 19/19 |

## 7. 权限与角色审计

| # | 项 | 证据 | 结论 |
|---|---|---|---|
| 7.1 | 每个权限码都有 `@PreAuthorize` 使用点 | controller 目录 grep `PreAuthorize` → **1659 处** | **未完成 P2**（未逐码对表） |
| 7.2 | 无「有码无角色」/「有角色无码」 | 未查 `permissions` × `role_permissions` 全量差异 | **待确认 P2** |
| 7.3 | POS 退款仅店长（NEW-18） | 代码：需 `order:manage`，`qa_store_b` 通过、其余三账号 403（E2E 实测） | 代码级已验证；**文档未落地（P3 / NEW-20）** |

## 8. 数据回填审计

| # | 回填 | 依据 | 守卫 | 回填后验证 | 结论 |
|---|---|---|---|---|---|
| 8.1 | `refund_record_id=50`（flow 5530） | 同订单、金额相等 1800 分、退款单早 4ms、该订单退款记录恰好 1 条 | 三道守卫 + 写后复核「全库残留=0」 | 复查：仅 1 条流水指向 50、无重复；二次执行 exit=3 中止 | **合理，非绕断言** |
| 8.2 | 512 条 remark 补措辞 | 均带 E2E tag | status=CLOSED + 带 tag + 未含措辞（幂等） | 512/512 含措辞 | **合理** |
| 8.3 | 80 条 items + 2 张 statement 软删 | 本会话测试产物；RK-8 要求「提交或作废」但**无 void 接口**（NEW-16） | 守卫 status=DRAFT + 先备份 | 软删后 DRAFT=0；A19/A32 不受影响 | 有守卫/备份，但**绕过产品缺口** → NEW-16 根治 |
| 8.4 | auditor data_scope、qa_* user_roles | **本轮未取证** | — | — | **待确认 P2** |

## 9. 需修正清单（按严重程度）

### P0（阻塞，会算错账）
- **（无）**：本轮未发现已落地的「立即算错账」缺陷。

### P1（高，NEW-19 实现前必须澄清）
1. **`pos_shifts.closing_cash` 类型/尺度不一致**（§1.6）：`DECIMAL(10,2)` + `BigDecimal` 与「分」约定冲突 → 若被计算消费会与净额（分）混算。处置：定死「元」或「分」，NEW-19 一律不引用。
2. **退款配对的时间列未落**（§3.3）：设计要求 `refund_time`，流水侧只有 `business_date`(天)/`create_time` → 边界 N 分钟内可能误判。处置：显式用 `order_refund_records.refund_time`（补列或 join）。
3. **`fen / 100.0` 两处未判定**（§2.3，`RecruitmentApprovalDataServiceImpl:153`、`DailySettlementDataServiceImpl:192`）：未确认展示/计算。

### P2（中）
4. A35 计数型需升级为对拍（§4.3）；5. 判定断言缺「分母」（§4.4）；6. A32/A36 报出范围需互斥（§4.5）；7. `recon_differences`/`finance_audit_logs` 仍按 order_id 关联退款（§5.1）；8. 权限码×使用点未对表（§7.1/7.2）；9. `pos_shifts.total_amount` 语义未取证（§1.5）；10. `recharge_records` 单位未取证（§2.4）；11. `start/end` 闭区间未取证（§3.2）；12. 历史回填无取证（§8.4）。

### P3（低）
13. POS 退款仅店长未写进权限矩阵（§7.3 / NEW-20）；14. `closing_cash` 16 处未逐行读完（§1.4）。

## 10. 审计结论

- 已落地部分（类别定义、退款写流水与配对、A30/A31/A32/A33/A35、A34 占位、NEW-21）**均有 SQL 级或代码级证据**；
- **无 P0**；**P1 = 3 条**，其中 2 条必须在 NEW-19 实现前澄清；
- NEW-19 v2 已把「`expected_amount` 含不含退款」从推测升级为**代码证据**，但 `total_amount`、`start/end` 边界语义仍是推测。