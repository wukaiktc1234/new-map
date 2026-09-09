-- =====================================================
-- W1-EC-04B-1 Runtime Statistical Validation 测试数据
-- =====================================================
-- 任务编号: W1-EC-04B-1-R (Runtime Validation)
-- 创建日期: 2026-09-09
-- 目的: 为日结对账 SQL 迁移提供 9 种场景的受控测试数据
-- 说明: 所有数据使用 INSERT ... ON CONFLICT DO NOTHING 实现幂等
--       使用固定 order_code 确保可重复执行
-- =====================================================

-- =====================================================
-- 前置条件：确保有测试门店和菜品
-- =====================================================

-- 插入测试门店（如果不存在）
INSERT INTO stores_new (store_id, store_name, store_code, status, deleted)
VALUES (9999, '测试门店-EC04B1', 'TEST-EC04B1', 1, 0)
ON CONFLICT (store_id) DO NOTHING;

-- 插入测试菜品（如果不存在）
INSERT INTO foods (food_id, food_code, food_name, sale_price, status, deleted)
VALUES 
    (90001, 'TEST-FOOD-001', '测试菜品A', 5000, 1, 0),
    (90002, 'TEST-FOOD-002', '测试菜品B', 3000, 1, 0),
    (90003, 'TEST-FOOD-003', '测试菜品C', 2000, 1, 0)
ON CONFLICT (food_code) DO NOTHING;

-- =====================================================
-- 场景 1: 正常订单
-- order_status=1(已确认), payment_status=2(已支付), final_amount=10000(100元)
-- create_time=今天
-- =====================================================
INSERT INTO orders (
    order_code, order_type, order_source, store_id, 
    order_status, payment_status, 
    total_amount, discount_amount, final_amount, paid_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-001-NORMAL', 1, 1, 9999,
    1, 2,  -- 已确认, 已支付
    10000, 0, 10000, 10000,  -- 100元
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
) 
SELECT o.order_id, 1, 90001, '测试菜品A', 5000, 2, 10000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-001-NORMAL'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 2, 10000, 'TXN-TEST-001-WECHAT', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-001-NORMAL'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- =====================================================
-- 场景 2: 多支付方式（≥3 种）
-- 现金/微信/支付宝，各 1 笔
-- =====================================================

-- 订单 2-1: 微信支付
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-002-WECHAT', 1, 1, 9999,
    1, 2,  -- 已确认, 已支付
    8000, 0, 8000, 8000,  -- 80元
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90001, '测试菜品A', 5000, 1, 5000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-002-WECHAT'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90002, '测试菜品B', 3000, 1, 3000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-002-WECHAT'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.product_name = '测试菜品B' AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 2, 8000, 'TXN-TEST-002-WECHAT', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-002-WECHAT'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- 订单 2-2: 支付宝支付
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-003-ALIPAY', 1, 1, 9999,
    1, 2,  -- 已确认, 已支付
    6000, 0, 6000, 6000,  -- 60元
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90002, '测试菜品B', 3000, 2, 6000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-003-ALIPAY'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 3, 6000, 'TXN-TEST-003-ALIPAY', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-003-ALIPAY'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- 订单 2-3: 现金支付
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-004-CASH', 1, 1, 9999,
    1, 2,  -- 已确认, 已支付
    4000, 0, 4000, 4000,  -- 40元
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90003, '测试菜品C', 2000, 2, 4000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-004-CASH'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 1, 4000, 'TXN-TEST-004-CASH', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-004-CASH'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- =====================================================
-- 场景 3: 折扣订单
-- discount_amount > 0
-- =====================================================
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-005-DISCOUNT', 1, 1, 9999,
    1, 2,  -- 已确认, 已支付
    10000, 2000, 8000, 8000,  -- 总100元, 折扣20元, 实付80元
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, discount_amount, deleted
)
SELECT o.order_id, 1, 90001, '测试菜品A', 5000, 2, 10000, 2000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-005-DISCOUNT'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 2, 8000, 'TXN-TEST-005-WECHAT', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-005-DISCOUNT'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- =====================================================
-- 场景 4: 已取消订单
-- order_status=3
-- =====================================================
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount,
    cancel_reason, create_time, update_time, deleted
) VALUES (
    'ORD-TEST-006-CANCELLED', 1, 1, 9999,
    3, 2,  -- 已取消, 已支付
    5000, 0, 5000, 5000,  -- 50元
    '测试取消原因', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90001, '测试菜品A', 5000, 1, 5000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-006-CANCELLED'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 2, 5000, 'TXN-TEST-006-WECHAT', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-006-CANCELLED'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- =====================================================
-- 场景 5: 退款中（部分退款）
-- order_status=4
-- =====================================================
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount, refund_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-007-PARTIAL-REFUND', 1, 1, 9999,
    4, 2,  -- 部分退款, 已支付
    10000, 0, 10000, 10000, 3000,  -- 总100元, 已退30元
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90001, '测试菜品A', 5000, 2, 10000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-007-PARTIAL-REFUND'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 2, 10000, 'TXN-TEST-007-WECHAT', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-007-PARTIAL-REFUND'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- =====================================================
-- 场景 6: 已退款（全额退款）
-- order_status=5
-- =====================================================
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount, refund_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-008-FULL-REFUND', 1, 1, 9999,
    5, 3,  -- 全额退款, 已退款
    8000, 0, 8000, 8000, 8000,  -- 总80元, 全额退
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90002, '测试菜品B', 3000, 2, 6000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-008-FULL-REFUND'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90003, '测试菜品C', 2000, 1, 2000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-008-FULL-REFUND'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.product_name = '测试菜品C' AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 3, 8000, 'TXN-TEST-008-ALIPAY', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-008-FULL-REFUND'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- =====================================================
-- 场景 7: 边界日期（跨日边界）
-- 昨天 23:59 和今天 00:00 的订单
-- =====================================================

-- 订单 7-1: 昨天 23:59:59
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-009-YESTERDAY', 1, 1, 9999,
    1, 2,  -- 已确认, 已支付
    7000, 0, 7000, 7000,  -- 70元
    (CURRENT_DATE - INTERVAL '1 day') + TIME '23:59:59',  -- 昨天 23:59:59
    CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90001, '测试菜品A', 5000, 1, 5000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-009-YESTERDAY'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90003, '测试菜品C', 2000, 1, 2000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-009-YESTERDAY'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.product_name = '测试菜品C' AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 1, 7000, 'TXN-TEST-009-CASH', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-009-YESTERDAY'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- 订单 7-2: 今天 00:00:00
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-010-TODAY-MIDNIGHT', 1, 1, 9999,
    1, 2,  -- 已确认, 已支付
    3000, 0, 3000, 3000,  -- 30元
    CURRENT_DATE + TIME '00:00:00',  -- 今天 00:00:00
    CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90003, '测试菜品C', 3000, 1, 3000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-010-TODAY-MIDNIGHT'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 2, 3000, 'TXN-TEST-010-WECHAT', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-010-TODAY-MIDNIGHT'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- =====================================================
-- 场景 8: 零值/最小值
-- final_amount=0（零元订单）
-- =====================================================
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-011-ZERO-AMOUNT', 1, 1, 9999,
    1, 2,  -- 已确认, 已支付
    0, 0, 0, 0,  -- 零元订单
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90001, '测试菜品A', 0, 1, 0, 0  -- 免费菜品
FROM orders o WHERE o.order_code = 'ORD-TEST-011-ZERO-AMOUNT'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 5, 0, 'TXN-TEST-011-POINTS', CURRENT_TIMESTAMP, 0  -- 积分兑换
FROM orders o WHERE o.order_code = 'ORD-TEST-011-ZERO-AMOUNT'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.deleted = 0)
LIMIT 1;

-- =====================================================
-- 场景 9: 多支付记录（同一订单 2 笔 order_payment_records）
-- 验证 JOIN 不重复放大
-- =====================================================
INSERT INTO orders (
    order_code, order_type, order_source, store_id,
    order_status, payment_status,
    total_amount, discount_amount, final_amount, paid_amount,
    create_time, update_time, deleted
) VALUES (
    'ORD-TEST-012-MULTI-PAYMENT', 1, 1, 9999,
    1, 2,  -- 已确认, 已支付
    10000, 0, 10000, 10000,  -- 100元
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (order_code) DO NOTHING;

INSERT INTO order_items (
    order_id, product_type, food_id, product_name, unit_price, quantity, amount, deleted
)
SELECT o.order_id, 1, 90001, '测试菜品A', 5000, 2, 10000, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-012-MULTI-PAYMENT'
AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.order_id AND oi.deleted = 0)
LIMIT 1;

-- 两笔支付记录：微信 60元 + 支付宝 40元 = 100元
INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 2, 6000, 'TXN-TEST-012-WECHAT', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-012-MULTI-PAYMENT'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.payment_method = 2 AND opr.deleted = 0)
LIMIT 1;

INSERT INTO order_payment_records (
    order_id, payment_method, payment_amount, transaction_no, payment_time, deleted
)
SELECT o.order_id, 3, 4000, 'TXN-TEST-012-ALIPAY', CURRENT_TIMESTAMP, 0
FROM orders o WHERE o.order_code = 'ORD-TEST-012-MULTI-PAYMENT'
AND NOT EXISTS (SELECT 1 FROM order_payment_records opr WHERE opr.order_id = o.order_id AND opr.payment_method = 3 AND opr.deleted = 0)
LIMIT 1;

-- =====================================================
-- 验证脚本：检查数据是否正确插入
-- =====================================================

-- 检查各场景订单数量
SELECT 
    CASE 
        WHEN order_code LIKE 'ORD-TEST-001%' THEN '场景1: 正常订单'
        WHEN order_code LIKE 'ORD-TEST-002%' THEN '场景2-1: 微信支付'
        WHEN order_code LIKE 'ORD-TEST-003%' THEN '场景2-2: 支付宝支付'
        WHEN order_code LIKE 'ORD-TEST-004%' THEN '场景2-3: 现金支付'
        WHEN order_code LIKE 'ORD-TEST-005%' THEN '场景3: 折扣订单'
        WHEN order_code LIKE 'ORD-TEST-006%' THEN '场景4: 已取消'
        WHEN order_code LIKE 'ORD-TEST-007%' THEN '场景5: 退款中'
        WHEN order_code LIKE 'ORD-TEST-008%' THEN '场景6: 已退款'
        WHEN order_code LIKE 'ORD-TEST-009%' THEN '场景7-1: 昨天边界'
        WHEN order_code LIKE 'ORD-TEST-010%' THEN '场景7-2: 今天边界'
        WHEN order_code LIKE 'ORD-TEST-011%' THEN '场景8: 零值'
        WHEN order_code LIKE 'ORD-TEST-012%' THEN '场景9: 多支付记录'
    END AS scenario,
    order_code,
    order_status,
    payment_status,
    total_amount,
    discount_amount,
    final_amount,
    refund_amount,
    create_time
FROM orders 
WHERE order_code LIKE 'ORD-TEST-%' AND store_id = 9999 AND deleted = 0
ORDER BY order_code;

-- 检查支付记录数量
SELECT 
    o.order_code,
    COUNT(opr.payment_id) AS payment_count,
    SUM(opr.payment_amount) AS total_payment_amount
FROM orders o
JOIN order_payment_records opr ON o.order_id = opr.order_id
WHERE o.order_code LIKE 'ORD-TEST-%' AND o.store_id = 9999 AND o.deleted = 0 AND opr.deleted = 0
GROUP BY o.order_code
ORDER BY o.order_code;

-- =====================================================
-- 预期查询结果（对账基线）
-- =====================================================
-- 以下 SQL 用于验证 W1-EC-04B-1 迁移后的 4 个查询是否正确

-- 假设测试日期为 CURRENT_DATE，storeId = 9999
-- startTime = CURRENT_DATE, endTime = CURRENT_DATE + 1 day

-- 1. aggregateByPaymentMethod 预期结果
-- SELECT opr.payment_method, SUM(opr.payment_amount) AS total_amount, COUNT(DISTINCT o.order_id) AS order_count
-- FROM orders o
-- INNER JOIN order_payment_records opr ON o.order_id = opr.order_id
-- WHERE o.store_id = 9999
--   AND o.create_time >= CURRENT_DATE
--   AND o.create_time < CURRENT_DATE + INTERVAL '1 day'
--   AND o.payment_status = 2
--   AND o.deleted = 0
--   AND opr.deleted = 0
-- GROUP BY opr.payment_method;
--
-- 预期 (修正后基线):
-- payment_method=1 (现金): amount=4000 (场景2-3), count=1
--   修正说明: 场景7-1(昨天23:59:59)被>=CURRENT_DATE正确排除
-- payment_method=2 (微信): amount=10000(场景1) + 8000(场景2-1) + 8000(场景3) + 5000(场景4) + 10000(场景5) + 3000(场景7-2) + 6000(场景9) = 50000, count=7
--   说明: 无变化
-- payment_method=3 (支付宝): amount=6000(场景2-2) + 4000(场景9) = 10000, count=2
--   修正说明: 场景6(payment_status=3)被payment_status=2正确排除
-- payment_method=5 (积分): amount=0 (场景8), count=1
--   说明: 无变化

-- 2. sumDiscountAmount 预期结果
-- SELECT COALESCE(SUM(discount_amount), 0)
-- FROM orders
-- WHERE store_id = 9999
--   AND create_time >= CURRENT_DATE
--   AND create_time < CURRENT_DATE + INTERVAL '1 day'
--   AND payment_status = 2
--   AND deleted = 0;
--
-- 预期: 2000 (场景3的discount_amount)

-- 3. aggregateRefunds 预期结果
-- SELECT COALESCE(SUM(refund_amount), 0) AS total_refund_amount, COUNT(*) AS refund_count
-- FROM orders
-- WHERE store_id = 9999
--   AND create_time >= CURRENT_DATE
--   AND create_time < CURRENT_DATE + INTERVAL '1 day'
--   AND order_status IN (4, 5)
--   AND deleted = 0;
--
-- 预期: total_refund_amount = 3000(场景5) + 8000(场景6) = 11000, refund_count = 2

-- 4. aggregateCancelled 预期结果
-- SELECT COALESCE(SUM(total_amount), 0) AS total_cancelled_amount, COUNT(*) AS cancelled_count
-- FROM orders
-- WHERE store_id = 9999
--   AND create_time >= CURRENT_DATE
--   AND create_time < CURRENT_DATE + INTERVAL '1 day'
--   AND order_status = 3
--   AND deleted = 0;
--
-- 预期: total_cancelled_amount = 5000 (场景4), cancelled_count = 1

-- =====================================================
-- 清理脚本（可选，测试完成后执行）
-- =====================================================
-- DELETE FROM order_payment_records WHERE order_id IN (SELECT order_id FROM orders WHERE order_code LIKE 'ORD-TEST-%' AND store_id = 9999);
-- DELETE FROM order_items WHERE order_id IN (SELECT order_id FROM orders WHERE order_code LIKE 'ORD-TEST-%' AND store_id = 9999);
-- DELETE FROM orders WHERE order_code LIKE 'ORD-TEST-%' AND store_id = 9999;
-- DELETE FROM foods WHERE food_code LIKE 'TEST-FOOD-%';
-- DELETE FROM stores_new WHERE store_code = 'TEST-EC04B1';
