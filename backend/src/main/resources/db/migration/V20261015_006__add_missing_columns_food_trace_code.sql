-- ============================================================
-- 现场修复（2026-10-23）：补齐 food_trace_code 缺失列
--
-- 背景：
--   FoodTraceCode 实体（@TableName("food_trace_code")）声明了以下列，
--   但活库该表从未包含它们（信息模式核对：26 列，缺 8 列）：
--     serve_time / table_number / store_id / store_name / print_count /
--     material_cost / labor_cost / total_cost
--   （remark 为 @TableField(exist=false)，不需要列）
--
-- 实测影响（E2E 链路验证）：
--   POST /api/v1/kitchen/orders/{id}/complete-make → generateFoodTraceCodes()
--   → INSERT food_trace_code 因列不存在报错 → 该语句令 PostgreSQL 事务进入 aborted 状态；
--   generateFoodTraceCodes 自身 catch 住返回 false（不抛出），
--   但同一事务中随后的 KitchenOrderMapper.updateById 必然失败：
--     SQL state 25P02「当前事务被终止，事务块结束之前的查询被忽略」
--   → 出餐（完成制作）稳定失败，且错误文案被控制器包装成「完成制作失败: ### Error updating database...」。
--
-- 幂等：ADD COLUMN IF NOT EXISTS，可重复执行。
-- 回退：DROP COLUMN 对应 8 列。
-- ============================================================

ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS serve_time    TIMESTAMP;
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS table_number  VARCHAR(32);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS store_id      BIGINT;
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS store_name    VARCHAR(255);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS print_count   INTEGER DEFAULT 0;
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS material_cost NUMERIC(18, 4);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS labor_cost    NUMERIC(18, 4);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS total_cost    NUMERIC(18, 4);

CREATE INDEX IF NOT EXISTS idx_ftc_kitchen_order_id ON food_trace_code (kitchen_order_id);
CREATE INDEX IF NOT EXISTS idx_ftc_order_id         ON food_trace_code (order_id);
CREATE INDEX IF NOT EXISTS idx_ftc_store_id         ON food_trace_code (store_id);
