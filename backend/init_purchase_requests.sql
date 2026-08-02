-- 初始化真实采购申请数据（中文标题、真实员工、真实物料）

-- 申请人映射：
-- emp-d (user_id=4, 财务部, 普通员工)
-- emp-g (user_id=7, 采购部, 普通员工)
-- emp-i (user_id=9, 运营部, 普通员工)
-- emp-k (user_id=11, 运营部, 普通员工)

INSERT INTO purchase_request (
    request_id, request_no, title, request_type,
    department_id, department_name, applicant_id, applicant_name,
    total_amount, status, priority, expected_date, description,
    receiver_type, receiver_warehouse_id,
    create_time, update_time, deleted
) VALUES
(
    replace(gen_random_uuid()::text, '-', ''), 'PR20260727001',
    '运营部-7月门店食材补货申请', 'routine',
    '4', '运营部', '9', '员工I',
    125000, 'draft', 'normal', '2026-07-30',
    '为测试门店A、B补充7月下旬所需蔬菜、肉类及主食',
    'warehouse', 1,
    NOW(), NOW(), 0
),
(
    replace(gen_random_uuid()::text, '-', ''), 'PR20260727002',
    '采购部-底料及调料集中采购申请', 'routine',
    '3', '采购部', '7', '员工G',
    87000, 'pending', 'high', '2026-07-29',
    '集中采购麻辣烫底料、芝麻酱、辣椒油等核心调料',
    'warehouse', 1,
    NOW(), NOW(), 0
),
(
    replace(gen_random_uuid()::text, '-', ''), 'PR20260727003',
    '运营部-海鲜类食材采购申请', 'routine',
    '4', '运营部', '11', '员工K',
    168000, 'approved', 'normal', '2026-07-31',
    '采购虾滑、蟹棒等海鲜类食材，供应周末高峰',
    'warehouse', 1,
    NOW(), NOW(), 0
),
(
    replace(gen_random_uuid()::text, '-', ''), 'PR20260727004',
    '财务部-办公及低值易耗品申请', 'routine',
    '2', '财务部', '4', '员工D',
    45000, 'draft', 'low', '2026-08-05',
    '申请采购打印纸、文具等办公耗材及不锈钢汤桶',
    'warehouse', 1,
    NOW(), NOW(), 0
);

-- 插入明细
INSERT INTO purchase_request_item (
    item_id, request_id, food_id, food_name, specification,
    quantity, unit, estimated_price, subtotal_amount,
    planned_receiver_type, planned_warehouse_id,
    create_time, update_time, deleted
)
SELECT
    replace(gen_random_uuid()::text, '-', ''),
    r.request_id,
    i.food_id::text,
    i.food_name,
    i.spec,
    i.quantity,
    i.unit,
    i.price,
    i.quantity * i.price,
    'warehouse',
    1,
    NOW(), NOW(), 0
FROM purchase_request r
JOIN (VALUES
    ('PR20260727001', 1, '娃娃菜', '新鲜', 100, '斤', 3.50),
    ('PR20260727001', 4, '土豆片', '切好真空', 80, '斤', 2.80),
    ('PR20260727001', 6, '撒尿牛丸', '冷冻', 50, '斤', 18.00),
    ('PR20260727001', 12, '方便面', '85g', 200, '包', 1.50),
    ('PR20260727002', 14, '麻辣烫底料', '500g', 30, '包', 25.00),
    ('PR20260727002', 15, '芝麻酱', '300g', 40, '瓶', 12.00),
    ('PR20260727002', 16, '辣椒油', '250ml', 50, '瓶', 9.00),
    ('PR20260727003', 10, '虾滑', '冷冻', 40, '斤', 28.00),
    ('PR20260727003', 11, '蟹棒', '冷冻', 30, '斤', 16.00),
    ('PR20260727004', 21, '不锈钢汤桶', '50L', 1, '个', 280.00),
    ('PR20260727004', 19, '筷子套装', '含勺', 500, '套', 0.20)
) AS i(req_no, food_id, food_name, spec, quantity, unit, price)
ON r.request_no = i.req_no;

-- 验证
SELECT request_id, request_no, title, applicant_name, department_name, status, total_amount
FROM purchase_request WHERE deleted = 0 ORDER BY request_no;
