-- 2026-10-23 E2E 夹具库存补数（可复放）
-- 背景：回归反复下单会扣空测试菜品库存，导致「菜品库存不足」红灯（PosOrderCreateServiceImpl 先扣 legacy food.stock）。
-- 本脚本只抬高测试夹具库存下限，不覆盖真实经营数据（按 food_code / food_id 精确命中）。

-- legacy food 表（POS 下单扣料读这张）
UPDATE food SET stock = 100000 WHERE food_code = 'FD260925001' AND stock < 1000;
UPDATE food SET stock = 100000 WHERE food_code = 'FD202608010001' AND stock < 1000;

-- canonical foods 表（管理端/耗料口径）
UPDATE foods SET stock = 1000 WHERE food_id = 2 AND stock < 100;
UPDATE foods SET stock = 1000 WHERE food_code = 'FD260926001' AND stock < 100;

-- 自查
SELECT 'food' AS tbl, food_code, stock FROM food WHERE food_code IN ('FD260925001', 'FD202608010001')
UNION ALL
SELECT 'foods' AS tbl, food_code, stock FROM foods WHERE food_id = 2 OR food_code = 'FD260926001';
