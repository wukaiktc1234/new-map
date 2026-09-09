# W1-EC-04B-1 Runtime Validation 测试数据方案

> 任务编号: W1-EC-04B-1-R (Runtime Validation)
> 创建日期: 2026-09-09
> 目的: 为日结对账 SQL 迁移提供 9 种场景的受控测试数据

---

## 1. 执行环境要求

### 1.1 数据库环境
- PostgreSQL 14+（项目使用的数据库）
- 测试环境需确保以下表存在且可写入：
  - `orders`
  - `order_items`
  - `order_payment_records`
  - `stores_new`（测试门店）
  - `foods`（测试菜品）

### 1.2 前置条件
1. 测试门店 `store_id=9999` 不存在（或使用 ON CONFLICT DO NOTHING）
2. 测试菜品 `food_id=90001/90002/90003` 不存在（或使用 ON CONFLICT DO NOTHING）
3. 数据库支持 PostgreSQL 时间区间语法（`INTERVAL`）

### 1.3 执行方式
- **推荐**: 使用 SQL 脚本直接执行（`w1-ec-04b-1-test-data.sql`）
- **备选**: 使用 API 调用序列（需确保后端服务运行）

---

## 2. 测试数据场景说明

### 场景 1: 正常订单
| 字段 | 值 |
|------|-----|
| order_code | ORD-TEST-001-NORMAL |
| order_status | 1 (已确认) |
| payment_status | 2 (已支付) |
| final_amount | 10000 (100元) |
| 支付方式 | 微信 (2) |
| create_time | 当前时间 |

**预期查询结果**:
- `aggregateByPaymentMethod`: 微信支付 10000分, 1笔
- `sumDiscountAmount`: 0分
- `aggregateRefunds`: 0条
- `aggregateCancelled`: 0条

---

### 场景 2: 多支付方式（≥3种）
| 订单 | 支付方式 | 金额 |
|------|---------|------|
| ORD-TEST-002-WECHAT | 微信 (2) | 8000 (80元) |
| ORD-TEST-003-ALIPAY | 支付宝 (3) | 6000 (60元) |
| ORD-TEST-004-CASH | 现金 (1) | 4000 (40元) |

**预期查询结果**:
- `aggregateByPaymentMethod`: 3行输出
  - 现金(1): 4000分, 1笔
  - 微信(2): 8000分, 1笔
  - 支付宝(3): 6000分, 1笔
- `totalRevenue`: 18000分 (3笔合计)

---

### 场景 3: 折扣订单
| 字段 | 值 |
|------|-----|
| order_code | ORD-TEST-005-DISCOUNT |
| total_amount | 10000 (100元) |
| discount_amount | 2000 (20元) |
| final_amount | 8000 (80元) |

**预期查询结果**:
- `aggregateByPaymentMethod`: 微信支付 8000分 (实付金额)
- `sumDiscountAmount`: 2000分
- `aggregateRefunds`: 0条
- `aggregateCancelled`: 0条

---

### 场景 4: 已取消订单
| 字段 | 值 |
|------|-----|
| order_code | ORD-TEST-006-CANCELLED |
| order_status | 3 (已取消) |
| total_amount | 5000 (50元) |

**预期查询结果**:
- `aggregateByPaymentMethod`: 不计入（payment_status=2 但 order_status=3）
- `sumDiscountAmount`: 5000分 (已支付订单)
- `aggregateRefunds`: 0条 (3 ∉ (4,5))
- `aggregateCancelled`: 5000分, 1笔

---

### 场景 5: 退款中（部分退款）
| 字段 | 值 |
|------|-----|
| order_code | ORD-TEST-007-PARTIAL-REFUND |
| order_status | 4 (部分退款) |
| refund_amount | 3000 (30元) |

**预期查询结果**:
- `aggregateByPaymentMethod`: 微信支付 10000分 (全额支付记录)
- `sumDiscountAmount`: 10000分 (已支付订单)
- `aggregateRefunds`: 3000分, 1笔
- `aggregateCancelled`: 0条 (4 ≠ 3)

---

### 场景 6: 已退款（全额退款）
| 字段 | 值 |
|------|-----|
| order_code | ORD-TEST-008-FULL-REFUND |
| order_status | 5 (全额退款) |
| refund_amount | 8000 (80元) |
| payment_status | 3 (已退款) |

**预期查询结果**:
- `aggregateByPaymentMethod`: 不计入（payment_status=3 ≠ 2）
- `sumDiscountAmount`: 0分 (payment_status=3 ≠ 2)
- `aggregateRefunds`: 8000分, 1笔
- `aggregateCancelled`: 0条 (5 ≠ 3)

---

### 场景 7: 边界日期（跨日边界）
| 订单 | create_time | 说明 |
|------|-------------|------|
| ORD-TEST-009-YESTERDAY | 昨天 23:59:59 | 边界内 |
| ORD-TEST-010-TODAY-MIDNIGHT | 今天 00:00:00 | 边界内 |

**预期查询结果** (今天日期范围):
- 两个订单都应被包含
- `aggregateByPaymentMethod`: 现金 7000分 + 微信 3000分
- `sumDiscountAmount`: 10000分 (7000+3000)
- `aggregateRefunds`: 0条
- `aggregateCancelled`: 0条

**验证重点**:
- SQL 条件 `create_time >= startTime AND create_time < endTime`
- `endTime = date.atTime(LocalTime.MAX)` 在 PostgreSQL TIMESTAMP 下包含 23:59:59.999999

---

### 场景 8: 零值/最小值
| 字段 | 值 |
|------|-----|
| order_code | ORD-TEST-011-ZERO-AMOUNT |
| final_amount | 0 (零元订单) |
| 支付方式 | 积分 (5) |

**预期查询结果**:
- `aggregateByPaymentMethod`: 积分支付 0分, 1笔
- `sumDiscountAmount`: 0分
- `aggregateRefunds`: 0条
- `aggregateCancelled`: 0条

---

### 场景 9: 多支付记录（JOIN 不重复放大）
| 订单 | 支付记录数 | 支付方式 | 金额 |
|------|-----------|---------|------|
| ORD-TEST-012-MULTI-PAYMENT | 2笔 | 微信 (2) | 6000 (60元) |
| | | 支付宝 (3) | 4000 (40元) |

**预期查询结果**:
- `aggregateByPaymentMethod`:
  - 微信(2): 6000分, COUNT(DISTINCT)=1
  - 支付宝(3): 4000分, COUNT(DISTINCT)=1
- **关键验证**: COUNT(DISTINCT o.order_id) 确保同一订单不被放大

---

## 3. 预期聚合结果（对账基线）

假设测试日期为 `2026-09-09`，`storeId = 9999`

### 3.1 aggregateByPaymentMethod (修正后基线)

| payment_method | total_amount (分) | order_count | 修正说明 |
|----------------|-------------------|-------------|----------|
| 1 (现金) | 4000 (场景2-3) | 1 | 原始错误: 11000(4000+7000), 修正: 场景7-1(昨天23:59:59)被>=CURRENT_DATE正确排除 |
| 2 (微信) | 50000 (10000+8000+8000+5000+10000+3000+6000) | 7 | 无变化 |
| 3 (支付宝) | 10000 (6000+4000) | 2 | 原始错误: 18000(6000+8000+4000), 修正: 场景6(payment_status=3)被payment_status=2正确排除 |
| 5 (积分) | 0 | 1 | 无变化 |

**总营收**: 4000+50000+10000+0 = 64000分 (640元)

### 3.2 sumDiscountAmount

| 总优惠金额 (分) |
|----------------|
| 2000 (场景3) |

### 3.3 aggregateRefunds

| total_refund_amount (分) | refund_count |
|--------------------------|--------------|
| 11000 (3000+8000) | 2 |

### 3.4 aggregateCancelled

| total_cancelled_amount (分) | cancelled_count |
|-----------------------------|-----------------|
| 5000 (场景4) | 1 |

---

## 4. 运行时验证脚本

### 4.1 执行日结统计查询

```sql
-- 使用 CURRENT_DATE 作为测试日期
WITH params AS (
    SELECT 
        '9999'::VARCHAR AS store_id,
        CURRENT_DATE::TIMESTAMP AS start_time,
        (CURRENT_DATE + INTERVAL '1 day')::TIMESTAMP AS end_time
)
-- 1. 按支付方式聚合
SELECT 
    opr.payment_method,
    COALESCE(SUM(opr.payment_amount), 0) AS total_amount,
    COUNT(DISTINCT o.order_id) AS order_count
FROM orders o
INNER JOIN order_payment_records opr ON o.order_id = opr.order_id
CROSS JOIN params p
WHERE o.store_id = p.store_id
  AND o.create_time >= p.start_time
  AND o.create_time < p.end_time
  AND o.payment_status = 2
  AND o.deleted = 0
  AND opr.deleted = 0
GROUP BY opr.payment_method
ORDER BY opr.payment_method;

-- 2. 汇总优惠金额
SELECT COALESCE(SUM(discount_amount), 0)
FROM orders o
CROSS JOIN params p
WHERE o.store_id = p.store_id
  AND o.create_time >= p.start_time
  AND o.create_time < p.end_time
  AND o.payment_status = 2
  AND o.deleted = 0;

-- 3. 聚合退款数据
SELECT 
    COALESCE(SUM(refund_amount), 0) AS total_refund_amount,
    COUNT(*) AS refund_count
FROM orders o
CROSS JOIN params p
WHERE o.store_id = p.store_id
  AND o.create_time >= p.start_time
  AND o.create_time < p.end_time
  AND o.order_status IN (4, 5)
  AND o.deleted = 0;

-- 4. 聚合作废订单数据
SELECT 
    COALESCE(SUM(total_amount), 0) AS total_cancelled_amount,
    COUNT(*) AS cancelled_count
FROM orders o
CROSS JOIN params p
WHERE o.store_id = p.store_id
  AND o.create_time >= p.start_time
  AND o.create_time < p.end_time
  AND o.order_status = 3
  AND o.deleted = 0;
```

### 4.2 预期结果对比 (修正后基线)

| 查询 | 预期结果 | 修正说明 | 实际结果 | 验证 |
|------|---------|----------|---------|------|
| aggregateByPaymentMethod (现金) | amount=4000, count=1 | 原始错误: 11000,2; 场景7-1昨天边界被排除 | | |
| aggregateByPaymentMethod (微信) | amount=50000, count=7 | 无变化 | | |
| aggregateByPaymentMethod (支付宝) | amount=10000, count=2 | 原始错误: 18000,3; 场景6 payment_status=3被排除 | | |
| aggregateByPaymentMethod (积分) | amount=0, count=1 | 无变化 | | |
| sumDiscountAmount | 2000 | 无变化 | | |
| aggregateRefunds | amount=11000, count=2 | 无变化 | | |
| aggregateCancelled | amount=5000, count=1 | 无变化 | | |

---

## 5. 注意事项

### 5.1 执行顺序
1. 先执行前置条件（门店/菜品数据）
2. 按顺序执行场景 1-9 的插入语句
3. 执行验证脚本确认数据正确
4. 执行运行时验证脚本对比预期结果

### 5.2 幂等性保证
- 所有 INSERT 使用 `ON CONFLICT DO NOTHING` 或 `NOT EXISTS` 检查
- 使用固定 `order_code` 确保可重复执行
- 不会因重复执行产生重复数据

### 5.3 时间依赖
- 场景 7-1 (昨天边界) 使用 `(CURRENT_DATE - INTERVAL '1 day') + TIME '23:59:59'`
- 场景 7-2 (今天边界) 使用 `CURRENT_DATE + TIME '00:00:00'`
- 运行时验证使用 `CURRENT_DATE` 作为查询范围

### 5.4 数据清理
- 测试完成后可执行清理脚本删除测试数据
- 清理脚本在 SQL 文件末尾提供（已注释）

### 5.5 限制说明 (修正后)
- 场景 6 (全额退款) 的 `payment_status=3`，不会计入 `aggregateByPaymentMethod`（条件要求 `payment_status=2`）→ **修正基线**：支付宝应为10000而非18000
- 场景 7-1 (昨天边界) 的 `create_time` 为昨天23:59:59，不会计入今日查询（条件要求 `create_time >= CURRENT_DATE`）→ **修正基线**：现金应为4000而非11000
- 场景 4 (已取消) 的 `order_status=3`，不会计入 `aggregateRefunds`（条件要求 `order_status IN (4,5)`）
- 场景 9 (多支付记录) 的 `COUNT(DISTINCT o.order_id)` 确保不放大

---

## 6. API 调用序列（备选方案）

如果使用 API 创建测试数据，需要调用以下端点：

### 6.1 创建订单 (POST /v1/orders)
```json
{
  "orderType": 1,
  "orderSource": 1,
  "storeId": 9999,
  "items": [
    {
      "productType": 1,
      "productId": 90001,
      "productName": "测试菜品A",
      "unitPrice": 5000,
      "quantity": 2,
      "amount": 10000
    }
  ]
}
```

### 6.2 更新订单状态 (PUT /v1/orders/{orderId}/status)
- 场景 4: order_status=3
- 场景 5: order_status=4, refund_amount=3000
- 场景 6: order_status=5, refund_amount=8000, payment_status=3

### 6.3 添加支付记录 (POST /v1/orders/{orderId}/payments)
```json
{
  "paymentMethod": 2,
  "paymentAmount": 10000,
  "transactionNo": "TXN-TEST-001"
}
```

**注意**: API 方案需要确保后端服务运行，且可能涉及复杂的业务逻辑（如状态机验证）。SQL 方案更直接可控。

---

## 7. 验收标准

### 7.1 数据完整性
- [ ] 9 种场景全部创建成功
- [ ] 每条数据包含 orders + order_items + order_payment_records 三表
- [ ] 数据幂等性验证（重复执行无重复数据）

### 7.2 查询正确性
- [ ] `aggregateByPaymentMethod` 返回 4 种支付方式
- [ ] `sumDiscountAmount` 返回 2000 分
- [ ] `aggregateRefunds` 返回 2 笔退款
- [ ] `aggregateCancelled` 返回 1 笔取消

### 7.3 边界验证
- [ ] 场景 7 的跨日边界订单被正确包含
- [ ] 场景 9 的多支付记录不放大 COUNT
- [ ] 场景 8 的零值订单被正确统计

---

## 8. 修正记录

### 8.1 原始错误 baseline (保留记录)

| 支付方式 | 原始预期金额 | 原始预期数量 | 错误来源 |
|----------|--------------|--------------|----------|
| 1 (现金) | 11000 (4000+7000) | 2 | 错误包含场景7-1(昨天23:59:59) |
| 3 (支付宝) | 18000 (6000+8000+4000) | 3 | 错误包含场景6(payment_status=3) |

### 8.2 修正后 baseline

| 支付方式 | 修正后金额 | 修正后数量 | 修正原因 |
|----------|------------|------------|----------|
| 1 (现金) | 4000 (场景2-3) | 1 | 场景7-1创建时间=昨天23:59:59，被 `create_time >= CURRENT_DATE` 正确排除 |
| 3 (支付宝) | 10000 (场景2-2+场景9) | 2 | 场景6 payment_status=3，被 `payment_status = 2` 正确排除 |

### 8.3 修正时间
- 修正日期: 2026-09-09
- 修正角色: developer (开发执行 Agent)
- 修正依据: QA发现测试数据基线错误，非SQL逻辑错误

### 8.4 修正文件清单
1. `docs/quality/w1-ec-04b-1-verification.sql` - 修正验证脚本中的预期值
2. `docs/quality/w1-ec-04b-1-test-data-api.md` - 修正测试数据方案中的预期结果

---

*文档生成时间: 2026-09-09*
*任务: W1-EC-04B-1-R (Runtime Validation)*
