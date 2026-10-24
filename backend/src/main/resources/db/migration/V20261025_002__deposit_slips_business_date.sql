-- ============================================================================
-- V20261025_002__deposit_slips_business_date.sql
--
-- BL-8（P3 前置）：deposit_slips 增加 business_date（业务日期 = 交班日）
--
-- 口径（TD-36）：business_date 取该缴款单所属班次 pos_shifts.end_time 的**日期**，
--   即「交班日」；不是生成日、不是提交日，也不是 created_at 的日期。
--   写入方由 ts-purchase 在 DepositSlipServiceImpl.generate 里接线（本迁移不参与业务写入）。
--
-- 阶段策略：**先可空**（DATE NULL），历史数据由
--   scripts/db-fix/2026-10-25-backfill-deposit-slip-business-date.sql 回填；
--   回填完成并观察一个结账周期后再议是否 SET NOT NULL（另起迁移，不在本文件）。
--
-- 幂等：ADD COLUMN IF NOT EXISTS + COMMENT 覆盖写；重复执行 0 结构性变更。
-- 回退：ALTER TABLE deposit_slips DROP COLUMN IF EXISTS business_date;
-- ============================================================================

ALTER TABLE deposit_slips
    ADD COLUMN IF NOT EXISTS business_date DATE NULL;

COMMENT ON COLUMN deposit_slips.business_date IS
    '业务日期（交班日）：TD-36 —— 取所属班次 pos_shifts.end_time 的日期；历史行由 db-fix 脚本回填，暂可空';

-- 覆盖式写注释（ADD COLUMN IF NOT EXISTS 在列已存在时不会更新注释，这里保证幂等一致）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'deposit_slips'
           AND column_name = 'business_date'
    ) THEN
        RAISE EXCEPTION '自检失败：deposit_slips.business_date 未被创建';
    END IF;
    RAISE NOTICE 'BL-8 完成：deposit_slips.business_date 已就绪（DATE NULL，等待回填）。';
END
$$;

-- 列表按日期过滤的索引（可空列，先建普通索引；回填后由后续迁移评估是否改部分索引）
CREATE INDEX IF NOT EXISTS idx_deposit_slips_store_business_date
    ON deposit_slips (store_id, business_date);
