# Production Gate 002（Gray 前置阻断处理）

> - 文档编号：W1-PRODGATE-002
> - 日期：2026-09-09
> - 角色：架构总控（会话1）
> - **约束**：只做扫描和计划，不修改生产数据。

---

## 一、OrderTimeoutTask 处置

### 处置结论：Option A（迁移）已实施

| 维度 | 处置前 | 处置后 |
|---|---|---|
| 扫描目标 | orders_legacy（status=0） | orders（order_status=0 AND payment_status=0） |
| 取消状态 | status=5 | order_status=3 |
| 库存回补 | order_items_legacy → food+foods | order_items → food+foods |
| legacy writer | **每 5 分钟写 orders_legacy** | **0**（不再写 legacy） |

**代码变更**：OrderTimeoutTask.java（导入/构造函数/查询条件/取消状态/回补路径全部切换到 orders）

**PRE-GRAY BLOCKER 已解决**：legacy writer = 0（从此任务）。

---

## 二、OrderNewServiceImpl Legacy Read

### 分类结论：**MIGRATE BEFORE GRAY**

| 维度 | 结论 |
|---|---|
| API 端点 | GET /v1/orders/pos + GET /v1/orders/pos/{orderNumber} |
| 前端页面 | **管理端核心页面"订单查询"**（OrderQuery.vue），4 种角色可见，页面 mount 自动加载 |
| 生产消费 | **高频**（管理端核心订单中心，含导出） |
| 能否直接切 orders | **可以**（PosOrderQueryServiceImpl 已证明可行性；字段映射/状态转换已有基础） |
| 最终分类 | **MIGRATE BEFORE GRAY** |

**理由**：这是 EC-04A 迁移遗漏项——POS Terminal/Payment/Kitchen 已迁移，但管理端"订单查询"仍在读 legacy。如果不迁移就进入灰度，管理端将与生产环境读不同的表。

**迁移方案**：queryPosOrders/getPosOrderDetail 改为读 orders（OrderNewMapper），利用已有转换逻辑；前端 orderId 前缀判断需同步调整。

---

## 三、PosOrderCreateServiceImpl order_items_legacy

### 分类结论：**DEAD CODE**

| 维度 | 结论 |
|---|---|
| line 705 方法 | createOrderItemsAndDeductStock（private，672-718 行） |
| 调用者 | **无**（全项目 grep 确认：0 个调用方） |
| 运行时写入 | **不会发生**（方法不可达） |
| 与 canonical 关系 | 无双写（canonical 路径独立，旧路径无调用） |
| 最终分类 | **DEAD CODE** |

**关联死代码簇**（EC-01/02 迁移后残留）：6 个方法（createOrderItemsAndDeductStock/createCanonicalOrderItemsAndDeductStock/recalculateOrderAmount/validateOrderItems/buildCreateOrderResult/createKitchenOrder-旧版）——建议后续 Legacy Cleanup Wave 统一清理。

---

## 四、Production Data Scan 脚本（就绪，待生产执行）

> ⚠️ 当前为开发/测试环境（orders_legacy=0 / finance_records=0）。以下脚本必须在**生产环境只读执行**。

### 4.1 Orders

```sql
SELECT COUNT(*) as cnt FROM orders WHERE deleted = 0;
SELECT * FROM orders WHERE deleted = 0 ORDER BY create_time DESC LIMIT 5;
SELECT order_status, payment_status, COUNT(*) as cnt FROM orders WHERE deleted = 0 GROUP BY order_status, payment_status;
```

### 4.2 Orders Legacy

```sql
SELECT COUNT(*) as cnt FROM orders_legacy WHERE deleted = 0;
SELECT order_status, COUNT(*) as cnt FROM orders_legacy WHERE deleted = 0 GROUP BY order_status;
SELECT * FROM orders_legacy WHERE deleted = 0 ORDER BY create_time DESC LIMIT 5;
-- 24h 内新增（检查活跃写入）
SELECT COUNT(*) as recent_writes FROM orders_legacy WHERE create_time >= NOW() - INTERVAL '24 hours' AND deleted = 0;
```

### 4.3 Finance Record

```sql
SELECT COUNT(*) as total FROM finance_records WHERE deleted = 0;
SELECT COUNT(*) FILTER (WHERE order_code IS NOT NULL) as has_code,
       COUNT(*) FILTER (WHERE order_code IS NULL) as no_code
FROM finance_records WHERE deleted = 0;
SELECT COUNT(*) FROM finance_records WHERE deleted = 0 AND (remark LIKE '%ORD%' OR remark LIKE '%POS-ORD%');
SELECT SUBSTRING(record_no FROM 1 FOR 4) as prefix, COUNT(*) as cnt FROM finance_records WHERE deleted = 0 GROUP BY prefix;
```

### 4.4 Amount Unit Cross-Check

```sql
-- orders 金额（分）
SELECT total_amount, final_amount, paid_amount FROM orders WHERE deleted = 0 LIMIT 5;
-- finance_records 金额（分）
SELECT amount FROM finance_records WHERE deleted = 0 LIMIT 5;
-- 交叉验证
SELECT o.order_code, o.final_amount, f.amount,
       CASE WHEN o.final_amount = f.amount THEN 'Y' ELSE 'N' END as match
FROM orders o JOIN finance_records f ON f.order_code = o.order_code
WHERE o.deleted = 0 AND f.deleted = 0 AND f.order_code IS NOT NULL LIMIT 10;
```

---

## 五、Gray Unlock Criteria（更新版）

| # | 条件 | 状态 |
|---|---|---|
| 1 | Production Data Scan PASS | ⏸ 待生产执行 |
| 2 | Active legacy writer = 0 | ✅ OrderTimeoutTask 已迁移（唯一活跃 writer 已消除） |
| 3 | Critical legacy reader = 0 或正式豁免 | ⏸ OrderNewServiceImpl 待迁移 |
| 4 | No hidden fallback | ✅ 代码扫描确认 |
| 5 | Finance order_code production scan PASS | ⏸ 待生产执行 |
| 6 | Amount unit PASS | ⏸ 待生产验证 |
| 7 | Regression PASS | ✅ |
| 8 | Rollback ready | ✅ |
| 9 | Monitoring ready | ✅（Gray 计划已建） |
| 10 | Product Owner approval | ⏸ |

### Gray Release 计划（更新）

```
前置：Production Data Scan + OrderNewServiceImpl 迁移
  →
灰度 0（只读验证）→ Day 1
灰度 1（单门店写入）→ Day 2-3
灰度 2（50%→100%）→ Day 4-7
灰度稳定期（≥7 天）→ Day 8+
```

---

## 六、EC-06 继续 LOCKED

冻结条件 = 全部 10 项满足。当前缺 #1/#3/#5/#6/#10。

---

*本 Production Gate 002 为 Gray 前置阻断处理交付，等待产品负责人确认。*
*文档生成：架构总控（会话1）· 2026-09-09*
