-- ============================================================================
-- 测试数据清理，DRAFT 表 statement 73（P4-4 真联调 fixture）
-- ----------------------------------------------------------------------------
-- 背景：statement 73（BS2026100008）是 pos-ux 在 P4-4「合计重算」真联调期间用
--       S3 生成接口（POST /v1/finance/recon/statements）创建的**测试数据**：
--       account 25 / 2026-10-25 单日 / 同额 A=1000 分 + C=1000 分，用于验证
--       处置后 A 类合计由 1000 → 0、adjusted_bank 89670 → 88670 的重算行为。
--       残留会被 scripts/qa/consistency_check.sql 的 A19（同额两侧并存）判 FAIL。
-- 范围：仅 statement_id = 73 的 reconciliation_items + reconciliation_statements。
-- 安全性：只允许删除 DRAFT；若状态已变为 SUBMITTED/APPROVED 直接报错中止。
-- 幂等：已清理过时提示并跳过。
-- 执行：psql -h localhost -p 5432 -U postgres -d food_traceability -f 本文件
-- ============================================================================

BEGIN;

DO $$
DECLARE
    v_no     TEXT;
    v_status TEXT;
    v_items  INTEGER := 0;
BEGIN
    SELECT statement_no, status INTO v_no, v_status
      FROM reconciliation_statements
     WHERE statement_id = 73;

    IF v_no IS NULL THEN
        RAISE NOTICE '[clean-p44] statement 73 不存在，已清理过，跳过';
        RETURN;
    END IF;

    IF v_status <> 'DRAFT' THEN
        RAISE EXCEPTION '[clean-p44] statement 73 当前状态为 %，非 DRAFT，拒绝清理（请人工确认）', v_status;
    END IF;

    DELETE FROM reconciliation_items WHERE statement_id = 73;
    GET DIAGNOSTICS v_items = ROW_COUNT;
    DELETE FROM reconciliation_statements WHERE statement_id = 73;

    RAISE NOTICE '[clean-p44] 已删除 statement 73（%）及其 % 条未达账项', v_no, v_items;
END $$;

COMMIT;

-- 验证：两条查询应各返回 0 行
SELECT statement_id, statement_no, status FROM reconciliation_statements WHERE statement_id = 73;
SELECT item_id, statement_id, category, amount FROM reconciliation_items WHERE statement_id = 73;
