# Production Gate 001（Wave 1 生产环境 Gate）

> - 文档编号：W1-PRODGATE-001
> - 日期：2026-09-09
> - 角色：架构总控（会话1）
> - **约束**：只做扫描和计划，不修改生产数据/migration history；禁止提前冻结 orders_legacy。

---

## 1. Production Data Snapshot

### 1.1 当前环境状态

| 表 | 记录数 | 说明 |
|---|---|---|
| orders | 1（测试数据） | 开发/测试环境 |
| orders_legacy | **0** | 开发/测试环境 |
| order_items | 0 | |
| order_items_legacy | 0 | |
| finance_records | 0 | |
| order_payment_records | 0+ | |

> ⚠️ **以上为开发/测试环境事实，不能代表生产环境。** 正式生产灰度前必须重新执行生产环境扫描。

### 1.2 生产环境扫描脚本（待执行）

```sql
-- 1. orders count
SELECT COUNT(*) FROM orders WHERE deleted = 0;

-- 2. orders_legacy count
SELECT COUNT(*) FROM orders_legacy WHERE deleted = 0;

-- 3. orders_legacy status distribution
SELECT order_status, COUNT(*) as cnt FROM orders_legacy WHERE deleted = 0 GROUP BY order_status;

-- 4. orders_legacy recent records
SELECT * FROM orders_legacy WHERE deleted = 0 ORDER BY create_time DESC LIMIT 10;

-- 5. orders legacy active writers（代码层扫描结论）
-- OrderTimeoutTask: cancelExpiredOrders() → write orders_legacy (status=5)
-- PosOrderCreateServiceImpl: createOrderItemsAndDeductStock() line 705 → write order_items_legacy

-- 6. orders_legacy active readers（代码层扫描结论）
-- OrderNewServiceImpl: queryPosOrders/getPosOrderDetail → read orders_legacy + order_items_legacy
-- SalesAnalysisServiceImpl: aggregateSalesData → read orders_legacy
-- PosOrderQueryServiceImpl: getOrderDetail/reprintReceipt → read order_items_legacy
-- PosOrderPaymentServiceImpl: recoverStockForRefund/buildOrderResultDTOFromNew → read order_items_legacy
```

### 1.3 生产环境必须证明

| 证明项 | 需要的数据 |
|---|---|
| orders_legacy 是否有数据 | count > 0 or = 0 |
| legacy write 是否仍在发生 | 检查 create_time 最近 24h 的新增记录 |
| legacy read 路径是否活跃 | 检查 access log / API 调用统计 |
| 新订单是否全部在 orders 表 | 对比 orders count vs orders_legacy count |

---

## 2. Legacy Consumer Map（代码层扫描结果）

### 2.1 活跃 Legacy 引用（~40 处，仍在生产代码路径）

| 模块 | 引用类型 | 方法数 | 风险 |
|---|---|---|---|
| **OrderTimeoutTask** | READ+WRITE orders_legacy + READ order_items_legacy | 3 | **P1**（功能缺口：新订单不被超时扫描） |
| **PosOrderCreateServiceImpl** | WRITE order_items_legacy（旧路径 line 705） | 1 | **P1**（残留写入） |
| **OrderNewServiceImpl** | READ orders_legacy + order_items_legacy（管理端查询） | 3 | P2（管理端显示 legacy 数据） |
| **SalesAnalysisServiceImpl** | READ orders_legacy（销售日报） | 1 | P2（半成品，status 语义错误） |
| **PosOrderQueryServiceImpl** | READ order_items_legacy（POS 详情/小票） | 3 | P2（legacy 订单详情） |
| **PosOrderPaymentServiceImpl** | READ order_items_legacy（退款回补/结果构建） | 3 | P2（legacy 退款链残留） |
| **OrderMapper.java** | 10 个 @Select 统计方法（@Select orders_legacy） | 10 | DEAD CODE（无调用方，EC-04B-2 迁移后弃用） |
| **OrderMapper.java** | 2 个 @Update 方法（updateOrderStatus/updateOrderRefund） | 2 | DEAD CODE（无调用方） |

### 2.2 DEAD CODE（~16 处）

| 文件 | 方法 | 原因 |
|---|---|---|
| OrderServiceImpl（全部） | 5 个方法 | 无 Controller/Service 注入 |
| OrderMapper @Select（10 个统计方法） | 全部 | EC-04B-2 迁移后无调用方 |
| OrderMapper @Update（2 个） | 全部 | 无调用方 |
| OrderEventListener | handleOrderCompletedEvent | 无事件发布方 |
| PosOrderPaymentServiceImpl | checkAndRecoverOrderStatus | CAS 恢复（非主链路） |

### 2.3 Kitchen / Payment / Finance 链路

| 模块 | orders_legacy 引用 | 说明 |
|---|---|---|
| Kitchen | **0** | 已完全迁移 |
| Payment（Finance） | **0** | 已完全迁移 |
| FinanceRecord | **0** | 已完全迁移（EC-05） |
| Event Listeners | **0** | OrderCompleted/Refund 仅操作 finance_record |

---

## 3. Finance Production Snapshot

### 3.1 当前环境

| 表 | count | order_code non-null | 说明 |
|---|---|---|---|
| finance_records | 0 | 0 | 开发环境为空 |
| payment | 22 | — | 采购订单号（PO 前缀，非销售） |
| payables | 20 | — | 采购订单号（PO 前缀） |
| fund_flows | 43 | — | 无 order_code 字段 |

### 3.2 生产环境扫描脚本

```sql
-- finance_record total count
SELECT COUNT(*) FROM finance_records WHERE deleted = 0;

-- order_code coverage
SELECT COUNT(*) FILTER (WHERE order_code IS NOT NULL) as has_code,
       COUNT(*) FILTER (WHERE order_code IS NULL) as no_code
FROM finance_records WHERE deleted = 0;

-- remark 中销售订单引用
SELECT COUNT(*) FROM finance_records 
WHERE deleted = 0 AND (remark LIKE '%ORD%' OR remark LIKE '%POS-ORD%' OR remark LIKE '%T2026%');

-- record_no 分布
SELECT SUBSTRING(record_no FROM 1 FOR 4) as prefix, COUNT(*) as cnt
FROM finance_records WHERE deleted = 0 GROUP BY prefix;

-- PO 编号不被误判（确认支付/应付的 order_no 是采购前缀）
SELECT COUNT(*) FROM payment WHERE order_no LIKE 'PO%';
SELECT COUNT(*) FROM payables WHERE order_no LIKE 'PO%';
```

### 3.3 确认项

| 确认项 | 代码层证据 | 生产验证 |
|---|---|---|
| order_code = canonical sales order | ✅ EC-05 实施 | ⏸ 待生产扫描 |
| PO 编号不被误判 | ✅ payment.payables 为采购订单号 | ⏸ 待生产扫描 |
| remark 不作为正式关联 | ✅ 无 remark 解析主路径 | ✅ 代码确认 |
| 历史 finance_record 断链 | ⚠️ 开发环境无法验证 | ⏸ 待生产扫描 |

---

## 4. Money Unit Verification

### 4.1 代码层确认

| 表 | 单位 | 证据 |
|---|---|---|
| orders.total_amount / final_amount / paid_amount | **分**（Long） | OrderNew 实体 |
| order_items.unit_price / amount | **分**（Long） | OrderItemNew 实体 |
| order_payment_records.payment_amount | **分**（Long） | OrderPaymentRecordNew 实体 |
| finance_records.amount | **分**（Long） | FinanceRecord 实体 |
| payment.amount | **分**（Long） | Payment 实体 |
| payables.amount | **分**（Long） | Payable 实体 |
| tax_record.amount | **元**（NUMERIC） | ⚠️ 遗留元口径 |
| account_balance | **元**（DECIMAL） | ⚠️ 遗留元口径（PD-031 冻结） |

### 4.2 生产环境抽样验证脚本

```sql
-- orders 金额单位（分）
SELECT total_amount, final_amount, paid_amount FROM orders WHERE deleted = 0 LIMIT 5;

-- finance_records 金额单位（分）
SELECT amount FROM finance_records WHERE deleted = 0 LIMIT 5;

-- 交叉验证：订单支付金额 vs finance_record 金额
SELECT o.order_code, o.final_amount, f.amount, 
       CASE WHEN o.final_amount = f.amount THEN '一致' ELSE '不一致' END as check
FROM orders o
JOIN finance_records f ON f.order_code = o.order_code
WHERE o.deleted = 0 AND f.deleted = 0 AND f.order_code IS NOT NULL
LIMIT 10;
```

---

## 5. Risk Classification

| 风险项 | 分类 | 说明 |
|---|---|---|
| OrderTimeoutTask 仍写 orders_legacy | **B. PRE-GRAY REQUIRED** | 每 5 分钟向 legacy 写入取消订单——灰度期间必须监控 legacy 新增 |
| PosOrderCreateServiceImpl 残留写 order_items_legacy（line 705） | **B. PRE-GRAY REQUIRED** | 旧路径残留写入——需确认是否仍被调用 |
| OrderNewServiceImpl 读 orders_legacy（管理端） | **C. MONITOR DURING GRAY** | 管理端仍显示 legacy 数据——灰度期间观察 |
| SalesAnalysis 读 orders_legacy | **D. POST-FREEZE CLEANUP** | 半成品，status 语义错误 |
| OrderMapper 12 个 legacy SQL 方法 | **E. DEAD CODE** | 无调用方，后续清理 |
| OrderServiceImpl 全部方法 | **E. DEAD CODE** | 无调用方 |
| OrderEventListener | **E. DEAD CODE** | 无事件发布方 |
| orders_legacy = 0（开发环境） | **⚠️ 不代表生产** | 正式灰度前必须生产扫描 |

---

## 6. Gray Release Plan

### 6.1 前置条件（全部满足才能启动灰度）

| 条件 | 状态 |
|---|---|
| EC-01 PASS | ✅ |
| EC-02 PASS | ✅ |
| EC-03 N/A | ✅ |
| EC-04B 全部关键路径 PASS | ✅ |
| EC-05 PASS | ✅ |
| **Production Data Scan** | ⏸ **当前任务** |
| Product Owner 确认 | ⏸ |

### 6.2 灰度计划

| 阶段 | 时间 | 范围 | 监控重点 |
|---|---|---|---|
| **灰度 0（只读验证）** | Day 1 | 读取路径全量切换到 orders（无写入变更） | POS 可见 / Kitchen 可见 / Payment 正常 / 统计正确 |
| **灰度 1（写入灰度）** | Day 2-3 | 单门店 POS 写入切换到 orders | orders 新增 / orders_legacy 新增=0 / 支付正常 / 退款正常 |
| **灰度 2（扩大）** | Day 4-7 | 50% → 100% 门店 | 同上 + 统计 / 日结 / 运营看板 |
| **灰度稳定期** | Day 8+ | 全量 | ≥7 天观察 / legacy 新增=0 / 无异常 |

### 6.3 灰度监控指标

| 指标 | 阈值 | 告警 |
|---|---|---|
| orders_legacy 新增写入 | = 0（每 5 分钟检查） | > 0 即告警 |
| orders 新增 | > 0（正常业务） | = 0 即异常 |
| POS 订单创建失败率 | < 0.1% | > 0.1% 告警 |
| POS 支付失败率 | < 0.1% | > 0.1% 告警 |
| Kitchen 订单可见性 | 100% | < 100% 告警 |
| 日结统计偏差 | < 1% | > 1% 告警 |
| finance_record order_code 覆盖率 | > 95%（新订单） | < 95% 告警 |

### 6.4 回滚方案

| 场景 | 回滚操作 |
|---|---|
| POS 写入异常 | 切换 createCanonicalOrder 写入目标回 orders_legacy |
| 支付异常 | 恢复 orders_legacy 读写路径 |
| Kitchen 异常 | 恢复 LEFT JOIN orders_legacy |
| 统计异常 | 恢复 OrderMapper legacy SQL 方法调用 |
| 严重异常 | 全量回滚到 Wave 0 状态 |

---

## 7. EC-06 Unlock Checklist

| # | 条件 | 状态 | 说明 |
|---|---|---|---|
| 1 | EC-01 PASS | ✅ | POS 收银写入迁移 |
| 2 | EC-02 PASS | ✅ | POS 扫码写入迁移 |
| 3 | EC-03 N/A | ✅ | 小程序无订单创建 |
| 4 | EC-04A PASS | ✅ | POS 读取+写入修复 |
| 5 | EC-04B-1 PASS | ✅ | 日结 SQL |
| 6 | EC-04B-2 PASS_WITH_LIMITATION | ⚠️ | 统计（中等风险可接受） |
| 7 | EC-04B-3 PASS | ✅ | 运营看板 |
| 8 | EC-04B-4 MIGRATE | 📋 POST-FREEZE | SalesAnalysis + OrderTimeoutTask |
| 9 | EC-05 PASS | ✅ | finance_record order_code |
| 10 | **Production Data Scan** | ⏸ **当前任务** | 生产环境实际数据 |
| 11 | **Gray Release ≥7 天** | ⏸ 待启动 | 灰度期间 legacy 新增=0 |
| 12 | **Product Owner Final Confirmation** | ⏸ | 最终放行 |

> **EC-06 冻结条件 = 全部 12 项满足**。当前缺 #10/#11/#12。

---

*本 Production Gate 为生产环境扫描与切换前风险评估，等待产品负责人确认。*
*文档生成：架构总控（会话1）· 2026-09-09 · 基于代码层全仓扫描*
