-- ============================================================
-- 修复财务凭证表 voucher_header：会计法合规性加固
-- 关联问题：P0-ERP-04（VoucherHeader 可物理删除）、P0-ERP-05（可并发篡改）、
--          P0-ERP-06（缺核心审计/红冲字段）
-- 法规依据：
--   - 《中华人民共和国会计法》：会计凭证不得伪造、变造、毁灭
--   - 《会计法》：凭证应当连续编号
--   - 《会计基础工作规范》：已审核凭证不可直接修改，须通过红字凭证冲销
-- 修复内容：
--   1. 补齐逻辑删除字段 deleted（默认0，配合应用层 @TableLogic）
--   2. 补齐乐观锁字段 version（默认0，配合应用层 @Version）
--   3. 新增红冲字段 is_reversed / reverse_original_id / reverse_reason
--   4. 添加 voucher_no 唯一索引（保证凭证号唯一，允许历史空值）
--   5. 创建数据库触发器：禁止物理 DELETE（会计法最后一道防线）
-- 注意：
--   - 应用层通过 @TableLogic 走逻辑删除（UPDATE deleted=1），不会触发 DELETE 触发器
--   - 触发器仅拦截绕过应用层的裸 SQL DELETE，防止误操作或恶意删除
-- ============================================================

-- ------------------------------------------------------
-- 1. 补齐逻辑删除字段 deleted
-- ------------------------------------------------------
ALTER TABLE voucher_header ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN voucher_header.deleted IS '逻辑删除标记：0未删除，1已删除。会计法合规：凭证仅允许逻辑删除（归档）';

-- ------------------------------------------------------
-- 2. 补齐乐观锁版本号 version
-- ------------------------------------------------------
ALTER TABLE voucher_header ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN voucher_header.version IS '乐观锁版本号，防止凭证被并发篡改';

-- ------------------------------------------------------
-- 3. 新增红冲字段：is_reversed / reverse_original_id / reverse_reason
-- ------------------------------------------------------
ALTER TABLE voucher_header ADD COLUMN IF NOT EXISTS is_reversed BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN voucher_header.is_reversed IS '是否红冲凭证：FALSE=正常凭证，TRUE=红字冲销凭证';

ALTER TABLE voucher_header ADD COLUMN IF NOT EXISTS reverse_original_id BIGINT;
COMMENT ON COLUMN voucher_header.reverse_original_id IS '红冲原凭证ID（仅红字凭证有值，指向被冲销的原凭证）';

ALTER TABLE voucher_header ADD COLUMN IF NOT EXISTS reverse_reason VARCHAR(500);
COMMENT ON COLUMN voucher_header.reverse_reason IS '红冲原因（红字凭证必填）';

-- ------------------------------------------------------
-- 4. 补齐审计时间字段默认值（若已存在则仅设置默认值）
-- ------------------------------------------------------
ALTER TABLE voucher_header ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE voucher_header ALTER COLUMN update_time SET DEFAULT CURRENT_TIMESTAMP;

-- ------------------------------------------------------
-- 5. 添加 voucher_no 唯一索引（部分唯一索引，仅约束未删除的凭证号）
--    允许历史 NULL 值共存；同期间内凭证号唯一
-- ------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS uk_voucher_header_voucher_no
    ON voucher_header(voucher_no)
    WHERE deleted = 0 AND voucher_no IS NOT NULL;

-- ------------------------------------------------------
-- 6. 添加索引：按期间查询、按红冲原凭证查询
--    幂等保护：period 列可能不存在（部分建表流程未创建该列），用 DO 块检查
-- ------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'voucher_header' AND column_name = 'period'
    ) THEN
        CREATE INDEX IF NOT EXISTS idx_voucher_header_period ON voucher_header(period);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_voucher_header_reverse_original ON voucher_header(reverse_original_id);

-- ------------------------------------------------------
-- 7. 创建数据库触发器：禁止物理 DELETE
--    会计法合规：财务凭证禁止物理删除，仅允许逻辑删除（UPDATE deleted=1）
--    即使应用层存在 Bug 或有人执行裸 SQL，数据库层面也会拒绝
-- ------------------------------------------------------
CREATE OR REPLACE FUNCTION prevent_voucher_physical_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '财务凭证禁止物理删除（违反会计法），请使用逻辑删除或红字凭证冲销';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_prevent_voucher_delete ON voucher_header;
CREATE TRIGGER tr_prevent_voucher_delete
    BEFORE DELETE ON voucher_header
    FOR EACH ROW
    EXECUTE FUNCTION prevent_voucher_physical_delete();
