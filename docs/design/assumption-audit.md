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
## 11. 审计补齐（第二轮，2026-10-26 13:30）

### 11.1 P1-3 全量核完：186 处（不是 171）

`grep "(\* 100|/ 100|\*100|/100|toFixed|parseFloat)" backend/src/main/java` → **186 处**；其中**与金额相关 50 处**，逐条判定结果：

| 类别 | 数量 | 判定 | 样例 |
|---|---|---|---|
| **展示层元转换**（返回 `String`/格式化） | **40** | **正确**（分→元仅用于展示） | `String.format("%.2f", fen / 100.0)`：`CustomerAnalysisServiceImpl:282`、`InvoiceReimbursementServiceImpl:624`、`PaymentServiceImpl:527`、`ReceiptServiceImpl:314`、`InventoryAnalysisServiceImpl:280`、`MarketingMemberServiceImpl:353`、`MarketingAnalysisServiceImpl:366`、`RechargeFinanceServiceImpl:116/123/125`、`AnomalyAlertServiceImpl:111`、`RechargePlanServiceImpl:211`、`RechargeRecordServiceImpl:413`、`RechargeStatsServiceImpl:138`、`RefundServiceImpl:257`、`FoodServiceImpl:1010`、`PointsController:68`；`String.valueOf(x / 100.0)`：`RecruitmentApprovalDataServiceImpl:153`、`DailySettlementDataServiceImpl:192`、`CostRecordServiceImpl:312`、`FinanceRecordServiceImpl:209`、`PayableServiceImpl:472`、`ReceivableServiceImpl:335`、`VoucherServiceImpl:549` |
| **输入侧元→分转换**（参与计算，但方向是元→分） | **2** | **正确**（`Math.round(元 * 100)` 得“分”），但**未走 `MoneyYuanToFen` 工具类**（该类 `:17` 明确「任何新代码要做元↔分换算都必须调用本类」）→ 记为 **P3 一致性建议**，非错算 | `FinanceSystemServiceImpl:58 long amountInCents = Math.round(expense.getAmount() * 100.0);`（金融支出单金额=元）；`RefundServiceImpl:248 return Math.round(value * 100);`（`parseYuanToFen(String 元)`） |
| **比率/百分比**（与金额单位无关） | **8** | 正确（*100 得百分数） | `ComboServiceImpl:399/412/416`、`CostAnalysisServiceImpl:307/500/511`、`DishCostServiceImpl:317`、`FoodServiceImpl:738`、`FinancialReportServiceImpl:207`、`InvoiceReimbursementServiceImpl:549` |
| **SQL 内 ` / 100.00`**（数据库修复脚本里把分转元展示/写报表） | **2** | 正确（仅 SELECT 表达式，不写回金额字段） | `DatabaseFixConfig:787`（`f.sale_price / 100.00`）、`:845`（`dc.combo_price / 100.00`） |
| 其余非金额（毫秒/超时/百分比阈值） | 136 | 正确（与金额无关） | `PerformanceMonitorAspect:140`、`RateLimitAspect:75`、`PrintTaskQueueManager:455`、`DeviceConnectionPool:31` |

**结论：186 处中，参与金额计算的仅 2 处，且方向均为「元→分」（正确）；`RecruitmentApprovalDataServiceImpl:153` 与 `DailySettlementDataServiceImpl:192` 经上下文确认是 `String.valueOf(fen / 100.0)` 的展示辅助方法，不参与计算。→ P1-3 降级为「已澄清」，无 P0。**
**遗留 P3 建议**：2 处元→分转换改用 `MoneyYuanToFen`，与项目约定一致。

### 11.2 P2-7 具体位置：**这些表根本没有 `order_id`，无残留**

`information_schema` 实测（`recon_runs` / `recon_items` / `recon_differences` / `reconciliation_statements` / `reconciliation_items` / `finance_audit_logs` 六张表）：

| 表 | 存在的关联列 | 有无 `order_id` | 有无 `refund_record_id` | 退款关联方式 |
|---|---|---|---|---|
| `reconciliation_items` | `item_id`、`flow_id` | **无** | **无** | 经 `flow_id` 反查 `fund_flows.refund_record_id`（A19 例外即此路径） |
| `recon_items` | `item_id` | **无** | **无** | 经 `item_id` 与 `reconciliation_items` 对应 |
| `finance_audit_logs` | `target_id` | **无** | **无** | `target_id` = `item_id`；退款/配对信息在 `operation_data::json`（如 `statementId`） |
| `recon_runs` / `recon_differences` / `reconciliation_statements` | 无上述列 | **无** | **无** | 经 `run_id` / `statement_id` 关联 |

**结论：P2-7 不存在「仍用 `order_id` 判断退款」的残留位置**（这些表连 `order_id` 列都没有）；退款链路的唯一真源是 `fund_flows.refund_record_id`，其余表经 `flow_id` / `item_id` 间接引用。→ P2-7 降级为「已澄清」。
**但新增一条 P2（11.3）**：`reconciliation_items` **无 `refund_record_id` 冗余列**，A19 例外必须经 `flow_id` 二次查询（已实现，用 `to_jsonb(f)->>`），性能与可读性上可考虑后续加冗余列 —— 非错算。

### 11.3 P2-8 权限对表（P3/P4 相关，不是全量 1659 处）

- `permissions` 表共 **429** 个码，其中 `finance%` 相关 **100 个**（如 `finance:bank:import`、`finance:bank:recon`、`finance:bank:reconcile`、`finance:bank:query`、`finance:deposit-slip:*`、`finance:audit-log:view`、`finance:auto-voucher:*`）；
- **「权限码 ↔ 角色绑定」查询未取到结果**：`LEFT JOIN role_permissions rp ON rp.permission_id = p.permission_id` 返回空且无数据 → **`role_permissions` 表名或主键结构与本假设不符**（需按实际表结构重查）；
- 因此 **P2-8 保持「待确认」**，但已把范围收敛到「100 个 finance 码 × 绑定表结构」；
- **POS 退款仅店长**：代码级已验证（`order:manage`），**权限矩阵文档仍未落地**（NEW-20，P3）。

## 12. 第二轮结论汇总

| 原编号 | 原级别 | 结论 |
|---|---|---|
| P1-1 `closing_cash` 尺度 | P1 | **已处置**：定性为「元」，NEW-19 不引用（见 §13） |
| P1-2 配对时间列 | P1 | **已处置**：`fund_flows` 新增 `refund_time` 列（见 §13 与设计 v3） |
| P1-3 单位全量核对 | P1 | **降级为已澄清**（186 处，参与计算的 2 处方向正确） |
| P2-7 order_id 残留 | P2 | **降级为已澄清**（相关表无 `order_id` 列） |
| P2-8 权限对表 | P2 | **仍待确认**（绑定表结构不符，范围已收敛到 100 个 finance 码） |
| P2-5/2-6 断言分母与互斥 | P2 | **已写入设计 v3**（见 §13） |
