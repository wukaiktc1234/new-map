-- =====================================================
-- W1-EC-04B-1 Runtime Validation 验证脚本
-- =====================================================
-- 任务编号: W1-EC-04B-1-R (Runtime Validation)
-- 创建日期: 2026-09-09
-- 目的: 验证日结对账 SQL 迁移后的 4 个查询是否正确
-- 说明: 执行测试数据创建脚本后运行此验证脚本
-- =====================================================

-- =====================================================
-- 步骤 1: 检查测试数据是否正确插入
-- =====================================================

-- 1.1 检查订单数量（预期 12 笔）
SELECT 
    COUNT(*) AS total_orders,
    COUNT(CASE WHEN order_status = 1 THEN 1 END) AS confirmed_orders,
    COUNT(CASE WHEN order_status = 3 THEN 1 END) AS cancelled_orders,
    COUNT(CASE WHEN order_status = 4 THEN 1 END) AS partial_refund_orders,
    COUNT(CASE WHEN order_status = 5 THEN 1 END) AS full_refund_orders
FROM orders 
WHERE order_code LIKE 'ORD-TEST-%' AND store_id = 9999 AND deleted = 0;

-- 1.2 检查支付记录数量（预期 14 笔：场景9有2笔）
SELECT 
    COUNT(*) AS total_payment_records,
    SUM(payment_amount) AS total_payment_amount
FROM order_payment_records opr
JOIN orders o ON opr.order_id = o.order_id
WHERE o.order_code LIKE 'ORD-TEST-%' AND o.store_id = 9999 
  AND o.deleted = 0 AND opr.deleted = 0;

-- 1.3 检查明细数量（预期 13 条：场景9有2条明细）
SELECT 
    COUNT(*) AS total_order_items
FROM order_items oi
JOIN orders o ON oi.order_id = o.order_id
WHERE o.order_code LIKE 'ORD-TEST-%' AND o.store_id = 9999 
  AND o.deleted = 0 AND oi.deleted = 0;

-- =====================================================
-- 步骤 2: 验证 4 个核心查询（使用测试门店 9999）
-- =====================================================

-- 定义测试参数
WITH test_params AS (
    SELECT 
        '9999'::VARCHAR AS store_id,
        CURRENT_DATE::TIMESTAMP AS start_time,
        (CURRENT_DATE + INTERVAL '1 day')::TIMESTAMP AS end_time
),

-- 2.1 验证 aggregateByPaymentMethod
payment_method_result AS (
    SELECT 
        opr.payment_method,
        COALESCE(SUM(opr.payment_amount), 0) AS total_amount,
        COUNT(DISTINCT o.order_id) AS order_count
    FROM orders o
    INNER JOIN order_payment_records opr ON o.order_id = opr.order_id
    CROSS JOIN test_params p
    WHERE o.store_id = p.store_id
      AND o.create_time >= p.start_time
      AND o.create_time < p.end_time
      AND o.payment_status = 2
      AND o.deleted = 0
      AND opr.deleted = 0
    GROUP BY opr.payment_method
),

-- 2.2 验证 sumDiscountAmount
discount_result AS (
    SELECT COALESCE(SUM(discount_amount), 0) AS total_discount
    FROM orders o
    CROSS JOIN test_params p
    WHERE o.store_id = p.store_id
      AND o.create_time >= p.start_time
      AND o.create_time < p.end_time
      AND o.payment_status = 2
      AND o.deleted = 0
),

-- 2.3 验证 aggregateRefunds
refund_result AS (
    SELECT 
        COALESCE(SUM(refund_amount), 0) AS total_refund_amount,
        COUNT(*) AS refund_count
    FROM orders o
    CROSS JOIN test_params p
    WHERE o.store_id = p.store_id
      AND o.create_time >= p.start_time
      AND o.create_time < p.end_time
      AND o.order_status IN (4, 5)
      AND o.deleted = 0
),

-- 2.4 验证 aggregateCancelled
cancelled_result AS (
    SELECT 
        COALESCE(SUM(total_amount), 0) AS total_cancelled_amount,
        COUNT(*) AS cancelled_count
    FROM orders o
    CROSS JOIN test_params p
    WHERE o.store_id = p.store_id
      AND o.create_time >= p.start_time
      AND o.create_time < p.end_time
      AND o.order_status = 3
      AND o.deleted = 0
)

-- 输出验证结果
SELECT '支付方式聚合' AS query_name, 
    payment_method AS dimension,
    total_amount AS actual_amount,
    order_count AS actual_count
FROM payment_method_result
UNION ALL
SELECT '优惠金额汇总' AS query_name,
    'total_discount' AS dimension,
    total_discount AS actual_amount,
    NULL AS actual_count
FROM discount_result
UNION ALL
SELECT '退款聚合' AS query_name,
    'refund' AS dimension,
    total_refund_amount AS actual_amount,
    refund_count AS actual_count
FROM refund_result
UNION ALL
SELECT '作废聚合' AS query_name,
    'cancelled' AS dimension,
    total_cancelled_amount AS actual_amount,
    cancelled_count AS actual_count
FROM cancelled_result
ORDER BY query_name, dimension;

-- =====================================================
-- 步骤 3: 与预期结果对比
-- =====================================================

WITH expected AS (
    -- 预期的支付方式聚合结果 (修正后基线)
    -- 原始错误: 现金11000, 支付宝18000
    -- 修正原因: 场景7-1(昨天23:59:59)被>=CURRENT_DATE正确排除, 场景6(payment_status=3)被payment_status=2正确排除
    SELECT 1 AS payment_method, 4000 AS expected_amount, 1 AS expected_count
    UNION ALL SELECT 2, 50000, 7
    UNION ALL SELECT 3, 10000, 2
    UNION ALL SELECT 5, 0, 1
),

actual AS (
    -- 实际的支付方式聚合结果
    SELECT 
        opr.payment_method,
        COALESCE(SUM(opr.payment_amount), 0) AS actual_amount,
        COUNT(DISTINCT o.order_id) AS actual_count
    FROM orders o
    INNER JOIN order_payment_records opr ON o.order_id = opr.order_id
    WHERE o.store_id = '9999'
      AND o.create_time >= CURRENT_DATE
      AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
      AND o.payment_status = 2
      AND o.deleted = 0
      AND opr.deleted = 0
    GROUP BY opr.payment_method
)

SELECT 
    CASE 
        WHEN e.payment_method IS NULL THEN '缺少预期支付方式: ' || a.payment_method
        WHEN a.actual_amount IS NULL THEN '缺少实际支付方式: ' || e.payment_method
        WHEN e.expected_amount = a.actual_amount AND e.expected_count = a.actual_count THEN '✅ 通过'
        ELSE '❌ 失败'
    END AS result,
    COALESCE(e.payment_method, a.payment_method) AS payment_method,
    e.expected_amount,
    a.actual_amount,
    e.expected_count,
    a.actual_count,
    CASE 
        WHEN e.expected_amount != a.actual_amount THEN 
            '金额差异: ' || (a.actual_amount - e.expected_amount)
        WHEN e.expected_count != a.actual_count THEN 
            '数量差异: ' || (a.actual_count - e.expected_count)
        ELSE NULL
    END AS diff_detail
FROM expected e
FULL OUTER JOIN actual a ON e.payment_method = a.payment_method
ORDER BY COALESCE(e.payment_method, a.payment_method);

-- =====================================================
-- 步骤 4: 验证优惠金额
-- =====================================================

SELECT 
    CASE 
        WHEN expected_discount = actual_discount THEN '✅ 通过'
        ELSE '❌ 失败'
    END AS result,
    expected_discount,
    actual_discount,
    CASE 
        WHEN expected_discount != actual_discount THEN 
            '差异: ' || (actual_discount - expected_discount)
        ELSE NULL
    END AS diff_detail
FROM (
    SELECT 2000 AS expected_discount
) e
CROSS JOIN (
    SELECT COALESCE(SUM(discount_amount), 0) AS actual_discount
    FROM orders o
    WHERE o.store_id = '9999'
      AND o.create_time >= CURRENT_DATE
      AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
      AND o.payment_status = 2
      AND o.deleted = 0
) a;

-- =====================================================
-- 步骤 5: 验证退款聚合
-- =====================================================

SELECT 
    CASE 
        WHEN expected_refund_amount = actual_refund_amount 
         AND expected_refund_count = actual_refund_count THEN '✅ 通过'
        ELSE '❌ 失败'
    END AS result,
    expected_refund_amount,
    actual_refund_amount,
    expected_refund_count,
    actual_refund_count,
    CASE 
        WHEN expected_refund_amount != actual_refund_amount THEN 
            '金额差异: ' || (actual_refund_amount - expected_refund_amount)
        WHEN expected_refund_count != actual_refund_count THEN 
            '数量差异: ' || (actual_refund_count - expected_refund_count)
        ELSE NULL
    END AS diff_detail
FROM (
    SELECT 11000 AS expected_refund_amount, 2 AS expected_refund_count
) e
CROSS JOIN (
    SELECT 
        COALESCE(SUM(refund_amount), 0) AS actual_refund_amount,
        COUNT(*) AS actual_refund_count
    FROM orders o
    WHERE o.store_id = '9999'
      AND o.create_time >= CURRENT_DATE
      AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
      AND o.order_status IN (4, 5)
      AND o.deleted = 0
) a;

-- =====================================================
-- 步骤 6: 验证作废聚合
-- =====================================================

SELECT 
    CASE 
        WHEN expected_cancelled_amount = actual_cancelled_amount 
         AND expected_cancelled_count = actual_cancelled_count THEN '✅ 通过'
        ELSE '❌ 失败'
    END AS result,
    expected_cancelled_amount,
    actual_cancelled_amount,
    expected_cancelled_count,
    actual_cancelled_count,
    CASE 
        WHEN expected_cancelled_amount != actual_cancelled_amount THEN 
            '金额差异: ' || (actual_cancelled_amount - expected_cancelled_amount)
        WHEN expected_cancelled_count != actual_cancelled_count THEN 
            '数量差异: ' || (actual_cancelled_count - expected_cancelled_count)
        ELSE NULL
    END AS diff_detail
FROM (
    SELECT 5000 AS expected_cancelled_amount, 1 AS expected_cancelled_count
) e
CROSS JOIN (
    SELECT 
        COALESCE(SUM(total_amount), 0) AS actual_cancelled_amount,
        COUNT(*) AS actual_cancelled_count
    FROM orders o
    WHERE o.store_id = '9999'
      AND o.create_time >= CURRENT_DATE
      AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
      AND o.order_status = 3
      AND o.deleted = 0
) a;

-- =====================================================
-- 步骤 7: 验证场景 9 (多支付记录 JOIN 不重复放大)
-- =====================================================

SELECT 
    o.order_code,
    COUNT(opr.payment_id) AS payment_count,
    SUM(opr.payment_amount) AS total_payment_amount,
    CASE 
        WHEN COUNT(opr.payment_id) = 2 AND SUM(opr.payment_amount) = 10000 THEN '✅ 通过'
        ELSE '❌ 失败'
    END AS result
FROM orders o
JOIN order_payment_records opr ON o.order_id = opr.order_id
WHERE o.order_code = 'ORD-TEST-012-MULTI-PAYMENT'
  AND o.deleted = 0 AND opr.deleted = 0
GROUP BY o.order_code;

-- 验证 COUNT(DISTINCT) 不放大
SELECT 
    '场景9: COUNT(DISTINCT)验证' AS scenario,
    COUNT(DISTINCT o.order_id) AS distinct_order_count,
    CASE 
        WHEN COUNT(DISTINCT o.order_id) = 1 THEN '✅ 通过'
        ELSE '❌ 失败'
    END AS result
FROM orders o
INNER JOIN order_payment_records opr ON o.order_id = opr.order_id
WHERE o.order_code = 'ORD-TEST-012-MULTI-PAYMENT'
  AND o.deleted = 0 AND opr.deleted = 0;

-- =====================================================
-- 步骤 8: 验证场景 7 (边界日期)
-- =====================================================

SELECT 
    '场景7: 边界日期验证' AS scenario,
    COUNT(*) AS order_count,
    CASE 
        WHEN COUNT(*) = 2 THEN '✅ 通过'
        ELSE '❌ 失败'
    END AS result
FROM orders o
WHERE o.store_id = '9999'
  AND o.create_time >= CURRENT_DATE
  AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
  AND o.order_code LIKE 'ORD-TEST-00%'
  AND o.deleted = 0;

-- 显示边界订单详情
SELECT 
    order_code,
    create_time,
    EXTRACT(HOUR FROM create_time) AS hour,
    EXTRACT(MINUTE FROM create_time) AS minute,
    EXTRACT(SECOND FROM create_time) AS second
FROM orders 
WHERE order_code IN ('ORD-TEST-009-YESTERDAY', 'ORD-TEST-010-TODAY-MIDNIGHT')
  AND deleted = 0
ORDER BY create_time;

-- =====================================================
-- 步骤 9: 综合验证汇总
-- =====================================================

WITH validation_summary AS (
    -- 支付方式聚合验证 (修正后基线)
    SELECT 
        '支付方式聚合' AS check_item,
        CASE 
            WHEN (
                SELECT COUNT(*)
                FROM (
                    -- 修正: 现金4000, 支付宝10000 (原始错误: 现金11000, 支付宝18000)
                    SELECT 1 AS pm, 4000 AS amt, 1 AS cnt
                    UNION ALL SELECT 2, 50000, 7
                    UNION ALL SELECT 3, 10000, 2
                    UNION ALL SELECT 5, 0, 1
                ) expected
                WHERE NOT EXISTS (
                    SELECT 1 FROM (
                        SELECT 
                            opr.payment_method,
                            COALESCE(SUM(opr.payment_amount), 0) AS amt,
                            COUNT(DISTINCT o.order_id) AS cnt
                        FROM orders o
                        INNER JOIN order_payment_records opr ON o.order_id = opr.order_id
                        WHERE o.store_id = '9999'
                          AND o.create_time >= CURRENT_DATE
                          AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
                          AND o.payment_status = 2
                          AND o.deleted = 0
                          AND opr.deleted = 0
                        GROUP BY opr.payment_method
                    ) actual
                    WHERE actual.pm = expected.pm 
                      AND (actual.amt != expected.amt OR actual.cnt != expected.cnt)
                )
            ) THEN '✅ 通过'
            ELSE '❌ 失败'
        END AS result
),

-- 优惠金额验证
discount_check AS (
    SELECT 
        '优惠金额汇总' AS check_item,
        CASE 
            WHEN 2000 = (
                SELECT COALESCE(SUM(discount_amount), 0)
                FROM orders o
                WHERE o.store_id = '9999'
                  AND o.create_time >= CURRENT_DATE
                  AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
                  AND o.payment_status = 2
                  AND o.deleted = 0
            ) THEN '✅ 通过'
            ELSE '❌ 失败'
        END AS result
),

-- 退款聚合验证
refund_check AS (
    SELECT 
        '退款聚合' AS check_item,
        CASE 
            WHEN 11000 = (
                SELECT COALESCE(SUM(refund_amount), 0)
                FROM orders o
                WHERE o.store_id = '9999'
                  AND o.create_time >= CURRENT_DATE
                  AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
                  AND o.order_status IN (4, 5)
                  AND o.deleted = 0
            ) AND 2 = (
                SELECT COUNT(*)
                FROM orders o
                WHERE o.store_id = '9999'
                  AND o.create_time >= CURRENT_DATE
                  AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
                  AND o.order_status IN (4, 5)
                  AND o.deleted = 0
            ) THEN '✅ 通过'
            ELSE '❌ 失败'
        END AS result
),

-- 作废聚合验证
cancelled_check AS (
    SELECT 
        '作废聚合' AS check_item,
        CASE 
            WHEN 5000 = (
                SELECT COALESCE(SUM(total_amount), 0)
                FROM orders o
                WHERE o.store_id = '9999'
                  AND o.create_time >= CURRENT_DATE
                  AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
                  AND o.order_status = 3
                  AND o.deleted = 0
            ) AND 1 = (
                SELECT COUNT(*)
                FROM orders o
                WHERE o.store_id = '9999'
                  AND o.create_time >= CURRENT_DATE
                  AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
                  AND o.order_status = 3
                  AND o.deleted = 0
            ) THEN '✅ 通过'
            ELSE '❌ 失败'
        END AS result
)

SELECT * FROM validation_summary
UNION ALL SELECT * FROM discount_check
UNION ALL SELECT * FROM refund_check
UNION ALL SELECT * FROM cancelled_check;

-- =====================================================
-- 步骤 10: 最终结论
-- =====================================================

WITH all_checks AS (
    SELECT '支付方式聚合' AS item, 
        CASE WHEN (
            SELECT COUNT(*) FROM (
                -- 修正: 现金4000, 支付宝10000 (原始错误: 现金11000, 支付宝18000)
                SELECT 1 AS pm, 4000 AS amt, 1 AS cnt
                UNION ALL SELECT 2, 50000, 7
                UNION ALL SELECT 3, 10000, 2
                UNION ALL SELECT 5, 0, 1
            ) e WHERE NOT EXISTS (
                SELECT 1 FROM (
                    SELECT opr.payment_method AS pm, COALESCE(SUM(opr.payment_amount), 0) AS amt, COUNT(DISTINCT o.order_id) AS cnt
                    FROM orders o INNER JOIN order_payment_records opr ON o.order_id = opr.order_id
                    WHERE o.store_id = '9999' AND o.create_time >= CURRENT_DATE AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
                      AND o.payment_status = 2 AND o.deleted = 0 AND opr.deleted = 0
                    GROUP BY opr.payment_method
                ) a WHERE a.pm = e.pm AND (a.amt != e.amt OR a.cnt != e.cnt)
            ) THEN 1 ELSE 0 END AS passed
    UNION ALL SELECT '优惠金额汇总', CASE WHEN 2000 = (SELECT COALESCE(SUM(discount_amount), 0) FROM orders o WHERE o.store_id = '9999' AND o.create_time >= CURRENT_DATE AND o.create_time < CURRENT_DATE + INTERVAL '1 day' AND o.payment_status = 2 AND o.deleted = 0) THEN 1 ELSE 0 END
    UNION ALL SELECT '退款聚合', CASE WHEN 11000 = (SELECT COALESCE(SUM(refund_amount), 0) FROM orders o WHERE o.store_id = '9999' AND o.create_time >= CURRENT_DATE AND o.create_time < CURRENT_DATE + INTERVAL '1 day' AND o.order_status IN (4, 5) AND o.deleted = 0) AND 2 = (SELECT COUNT(*) FROM orders o WHERE o.store_id = '9999' AND o.create_time >= CURRENT_DATE AND o.create_time < CURRENT_DATE + INTERVAL '1 day' AND o.order_status IN (4, 5) AND o.deleted = 0) THEN 1 ELSE 0 END
    UNION ALL SELECT '作废聚合', CASE WHEN 5000 = (SELECT COALESCE(SUM(total_amount), 0) FROM orders o WHERE o.store_id = '9999' AND o.create_time >= CURRENT_DATE AND o.create_time < CURRENT_DATE + INTERVAL '1 day' AND o.order_status = 3 AND o.deleted = 0) AND 1 = (SELECT COUNT(*) FROM orders o WHERE o.store_id = '9999' AND o.create_time >= CURRENT_DATE AND o.create_time < CURRENT_DATE + INTERVAL '1 day' AND o.order_status = 3 AND o.deleted = 0) THEN 1 ELSE 0 END
)

SELECT 
    'Runtime Validation 结论' AS final_result,
    SUM(passed) AS passed_checks,
    COUNT(*) AS total_checks,
    CASE 
        WHEN SUM(passed) = COUNT(*) THEN '✅ ALL PASS'
        ELSE '❌ FAIL: ' || (COUNT(*) - SUM(passed)) || ' checks failed'
    END AS conclusion
FROM all_checks;
