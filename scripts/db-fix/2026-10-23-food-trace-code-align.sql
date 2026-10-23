-- ============================================================
-- food_trace_code 表结构对齐（2026-10-23 无人值守修复）
-- 背景：FoodTraceCode 实体已带 qrCodeUrl/orderNumber/productionStation/produceTime/
--       inventoryDeduction/materialDetails/makeStartTime/serveMethod/remark/createBy/updateBy
--       11 个字段，但活库从未迁移 → 出餐生成追溯码时报 column does not exist，整条出餐链路 500。
-- 口径：只做「表追加列」，不改实体、不改 WIP 业务逻辑；order_no 为历史列，保留不动。
-- 用法：psql -h localhost -U postgres -d food_traceability -f <本文件>
-- ============================================================
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS qr_code_url          varchar(512);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS order_number         varchar(64);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS production_station   varchar(64);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS produce_time         timestamp;
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS inventory_deduction  text;
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS material_details     text;
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS make_start_time      timestamp;
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS serve_method         varchar(32);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS remark               varchar(512);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS create_by            varchar(64);
ALTER TABLE food_trace_code ADD COLUMN IF NOT EXISTS update_by            varchar(64);
