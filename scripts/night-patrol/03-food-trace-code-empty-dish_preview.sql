/* =============================================================================
 * 文件    : 03-food-trace-code-empty-dish_preview.sql
 * 标题    : food_trace_code 菜品字段为空的历史追溯码
 * 目标    : 定位 food_trace_code 中 dish_name 为空的历史行，并从 order_items 唯一匹配回填 product_name（只回填 dish_name）
 * 预查影响: 8 行 / 共 14 行（deleted=0；其中 8 行的 dish_id 与 dish_name 同时为空、store_id 也为空；8 行对应订单在 order_items 中恰好各有 1 条明细，可确定性回填）
 * 回滚    : 见 [ROLLBACK] 段：UPDATE food_trace_code SET dish_name = NULL WHERE id IN (...) AND deleted = 0（这些行原本就是 NULL，回滚即置空）
 * 幂等守卫: 变更语句带 (dish_name IS NULL OR btrim(dish_name)='') 条件 + 唯一明细子查询，重复执行影响 0 行
 * 用法    : psql -f 03-food-trace-code-empty-dish_preview.sql     —— 默认**只跑 SELECT 预览**，不产生任何变更
 * 变更段  : 全部包在 [OWNER-EXECUTE] 注释块内，解除注释后才会生效，需 Owner 执行
 * 红线    : 本文件只读 + 软标记 / 字段回填；不含任何物理移除语句、不含 DDL
 * 时点    : 2026-10-23（分支 ai-night-patrol-1，只读巡检产出）
 * ========================================================================== */

SET client_encoding = 'UTF8';

-- ============================ 1. 预览：待处理 8 行 ============================
SELECT id, trace_code, order_id, order_no, dish_id, dish_name, store_id, create_time::date AS create_date
FROM food_trace_code
WHERE deleted = 0
  AND (dish_name IS NULL OR btrim(dish_name) = '')
ORDER BY id;

SELECT count(*) AS affected_rows
FROM food_trace_code
WHERE deleted = 0
  AND (dish_name IS NULL OR btrim(dish_name) = '');

-- ============================ 2. 可回填性验证（每个订单恰好 1 条明细） ============================
-- 只有 distinct_items = 1 的行才允许回填；> 1 的行无法判定属于哪道菜，脚本不会碰
SELECT f.id,
       f.order_no,
       count(oi.item_id) AS item_rows,
       min(oi.product_name) AS candidate_dish_name,
       min(oi.food_id::text) AS order_item_food_id
FROM food_trace_code f
LEFT JOIN order_items oi ON oi.order_id = f.order_id AND oi.deleted = 0
WHERE f.deleted = 0
  AND (f.dish_name IS NULL OR btrim(f.dish_name) = '')
GROUP BY f.id, f.order_no
ORDER BY f.id;

-- ============================ 3. 已知格式冲突（重要，勿盲目回填 dish_id） ============================
-- 已正常回填的样本行 dish_id 形如 'FD260925001'（业务编码），而 order_items.food_id 是整型主键（如 2）。
-- 直接把 food_id 写进 dish_id 会制造新的脏数据 → 本脚本**只回填 dish_name**。
SELECT id, trace_code, dish_id, dish_name
FROM food_trace_code
WHERE deleted = 0
  AND dish_id IS NOT NULL
ORDER BY id
LIMIT 10;

-- ============================ 4. 关联订单真实性（预期 0 行缺失） ============================
SELECT f.id, f.order_id
FROM food_trace_code f
WHERE f.deleted = 0
  AND (f.dish_name IS NULL OR btrim(f.dish_name) = '')
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id::text = f.order_id::text);

/* ============================ [OWNER-EXECUTE] 变更段 ============================
 * 仅当该订单在 order_items 中恰好 1 条有效明细时才回填 dish_name。
 *
 * UPDATE food_trace_code f
 *    SET dish_name = oi.product_name,
 *        update_time = now()
 *   FROM order_items oi
 *  WHERE oi.order_id = f.order_id
 *    AND oi.deleted = 0
 *    AND f.deleted = 0
 *    AND (f.dish_name IS NULL OR btrim(f.dish_name) = '')
 *    AND (SELECT count(*) FROM order_items x
 *          WHERE x.order_id = f.order_id AND x.deleted = 0) = 1;
 * -- 预计影响：8 行
 * ================================================================================ */

/* ============================ [ROLLBACK] 回滚 ============================
 * UPDATE food_trace_code
 *    SET dish_name = NULL,
 *        update_time = now()
 *  WHERE id IN (1,2,3,4,5,12,15,17)
 *    AND deleted = 0;
 * -- 上述 id 即 2026-10-23 预览出的 8 行；回滚前请用第 1 段复核 id 集合未变
 * ======================================================================== */
