-- ============================================================================
-- 测试数据清理，DRAFT 表 statement 42
-- ----------------------------------------------------------------------------
-- 背景：statement 42（BS2026100006）是 pos-ux 在 P4-3「EXEMPT 排除」联调期间
--       用 S3 生成接口（POST /v1/finance/recon/statements）创建的**测试数据**：
--       同账户同期间两侧同额（A=8888 分 / C=8888 分），用于验证歧义处置与 EXEMPT 审计。
--       该形态会被 scripts/qa/consistency_check.sql 的 A19（同额两侧并存=形态假平衡）
--       判为 FAIL，属于测试残留，必须清理。
-- 范围：仅 statement_id = 42 的 reconciliation_items + reconciliation_statements；
--       不触碰任何其它调节表 / 未达账项 / 资金流水。
-- 安全性：只允许删除 DRAFT 状态；若状态已被改成 SUBMITTED/APPROVED 则直接报错中止。
-- 幂等：表不存在（已清理过）时提示并跳过，不报错。
-- 执行：psql -h localhost -p 5432 -U postgres -d food_traceability -f 本文件
-- ============================================================================

BEGIN;

DO $$
DECLARE
    v_no      TEXT;
    v_status  TEXT;
    v_items   INTEGER := 0;
BEGIN
    SELECT statement_no, status INTO v_no, v_status
      FROM reconciliation_statements
     WHERE statement_id = 42;

    IF v_no IS NULL THEN
        RAISE NOTICE '[clean-p43] statement 42 不存在，已清理过，跳过';
        RETURN;
    END IF;

    IF v_status <> 'DRAFT' THEN
        RAISE EXCEPTION '[clean-p43] statement 42 当前状态为 %，非 DRAFT，拒绝清理（请人工确认）', v_status;
    END IF;

    DELETE FROM reconciliation_items WHERE statement_id = 42;
    GET DIAGNOSTICS v_items = ROW_COUNT;

    DELETE FROM reconciliation_statements WHERE statement_id = 42;

    RAISE NOTICE '[clean-p43] 已删除 statement 42（%）及其 % 条未达账项', v_no, v_items;
END $$;

COMMIT;

-- 验证：下面两条查询应分别返回 0 行
SELECT statement_id, statement_no, status
  FROM reconciliation_statements
 WHERE statement_id = 42;

SELECT item_id, statement_id, category, amount
  FROM reconciliation_items
 WHERE statement_id = 42;
