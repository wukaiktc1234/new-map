-- =====================================================================
-- scripts/seed/2026-10-27-seed-food-trace-codes.sql
-- bug-② 补 seed：food_trace_codes（供应链追溯码）清空后无数据 → 追溯查询接口恒空。
--   · 该表与 food_trace_code（出餐/菜品追溯码，80 行历史数据）是两代不同模型，不是同表异名；
--   · 本脚本为 E2E 受控数据，带 tag：trace_code 前缀 TC-E2E-SEED-，extra_info 含 [E2E:trace-seed]
--   · 幂等（按 trace_code 判重）；清理：scripts/seed/2026-10-27-clean-food-trace-codes.sql
-- 字段语义：trace_type 1菜品 2原料批次 3物流 4检验；target_type 1成品 2半成品 3原材料 4包材
-- 约束：chk_ftc_risk_level_domain 要求 risk_level ∈ {1,2,3}（0 会违反）；status ∈ {1..5}；expiry_date > production_date
-- 注意：chain_data 是 jsonb，但实体用 JacksonTypeHandler 反序列化成 String —— 写 '[]'::jsonb 会让 /trace-codes/{code} 500，
--       故 seed 里置 NULL（单码查询已验证 OK）；若要写链数据须用 "[\"...\"]" 这类 JSON 字符串形态。
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO food_trace_codes
    (trace_code, trace_type, target_type, target_id, target_name, batch_no,
     production_date, expiry_date, supplier_id, supplier_name, status, risk_level,
     chain_data, extra_info, create_user_id, create_time, update_time, deleted)
SELECT v.trace_code, v.trace_type, v.target_type, v.target_id, v.target_name, v.batch_no,
       v.production_date, v.expiry_date, v.supplier_id, v.supplier_name, 1, 1,
       NULL::jsonb, '[E2E:trace-seed]', 1, now(), now(), 0
  FROM (VALUES
        ('TC-E2E-SEED-001', 2, 3, 1::bigint, '生菜', 'BATCH-E2E-001', DATE '2026-10-26', DATE '2026-11-02', 1::bigint, 'E2E 测试供应商'),
        ('TC-E2E-SEED-002', 2, 3, 3::bigint, '生菜(物料3)', 'BATCH-E2E-002', DATE '2026-10-26', DATE '2026-11-02', 1::bigint, 'E2E 测试供应商'),
        ('TC-E2E-SEED-003', 1, 1, 1::bigint, '生菜串', 'BATCH-E2E-003', DATE '2026-10-27', DATE '2026-11-03', NULL::bigint, NULL),
        ('TC-E2E-SEED-004', 1, 1, 1::bigint, '链数据验收菜', 'BATCH-E2E-004', DATE '2026-10-27', DATE '2026-11-03', NULL::bigint, NULL)
       ) AS v(trace_code, trace_type, target_type, target_id, target_name, batch_no, production_date, expiry_date, supplier_id, supplier_name)
 WHERE NOT EXISTS (SELECT 1 FROM food_trace_codes t WHERE t.trace_code = v.trace_code AND t.deleted = 0);
-- TC-E2E-SEED-004 的 chain_data：**用于验证 jsonb 直通与规范化哈希，勿删**
--   为什么单独写：jsonb 会规范化（[{"a":1}] 读回变 [{"a": 1}]，9→10 字节），
--   本行必须保留一个**数组**取值，才能复现「写时算 vs 读回算」哈希一致（见 JsonCanonicalHashUtil 注释）。
UPDATE food_trace_codes
   SET chain_data = '[{"a":1},{"node":"验收","ts":"2026-10-27T21:00:00"}]'::jsonb,
       update_time = now()
 WHERE trace_code = 'TC-E2E-SEED-004' AND deleted = 0;
COMMIT;
SELECT trace_code_id, trace_code, trace_type, target_type, target_id, target_name, status,
       length(chain_data::text) AS chain_len, extra_info
  FROM food_trace_codes WHERE trace_code LIKE 'TC-E2E-SEED-%' ORDER BY trace_code;