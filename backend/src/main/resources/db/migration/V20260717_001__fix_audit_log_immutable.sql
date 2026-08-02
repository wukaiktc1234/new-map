-- ============================================================
-- 修复审计日志表：强制不可变性（仅允许 INSERT，禁止 UPDATE/DELETE）
-- 关联问题：P0-ERP-02（AuditLog 可逻辑删除，违反会计法/网络安全法）
-- 修复内容：
--   1. 添加 before_value / after_value 字段（防篡改快照）
--   2. 移除 deleted 字段（审计日志不允许任何形式的删除）
--   3. 创建数据库触发器，强制禁止 UPDATE 和 DELETE 操作
--   4. 补齐 audit_log_archive 表的防篡改字段
-- 法规依据：
--   - 《会计法》：会计档案不得伪造、变造、毁灭
--   - 《网络安全法》第二十一条：采取技术措施防止网络日志丢失
--   - 《数据安全法》：重要数据处理活动应当留存操作日志
-- ============================================================

-- ------------------------------------------------------
-- 1. 添加审计日志防篡改字段 before_value / after_value
-- ------------------------------------------------------
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS before_value TEXT;
COMMENT ON COLUMN audit_log.before_value IS '操作前值（JSON 格式，用于防篡改）';

ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS after_value TEXT;
COMMENT ON COLUMN audit_log.after_value IS '操作后值（JSON 格式，用于防篡改）';

-- ------------------------------------------------------
-- 2. 补齐审计日志归档表的防篡改字段
-- ------------------------------------------------------
ALTER TABLE audit_log_archive ADD COLUMN IF NOT EXISTS before_value TEXT;
ALTER TABLE audit_log_archive ADD COLUMN IF NOT EXISTS after_value TEXT;
ALTER TABLE audit_log_archive ADD COLUMN IF NOT EXISTS session_id   VARCHAR(100);

-- ------------------------------------------------------
-- 3. 移除 deleted 字段（审计日志不允许任何形式的删除）
--    注意：此前 deleted=1 的记录将重新可见，符合审计完整性要求
-- ------------------------------------------------------
ALTER TABLE audit_log DROP COLUMN IF EXISTS deleted;

-- ------------------------------------------------------
-- 4. 创建数据库触发器：强制禁止 UPDATE 和 DELETE
--    即使应用层存在 Bug，数据库层面也会拒绝修改/删除操作
--    这是审计日志完整性的最后一道防线
-- ------------------------------------------------------
CREATE OR REPLACE FUNCTION prevent_audit_log_modify()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '审计日志仅允许 INSERT，禁止 UPDATE 和 DELETE（违反会计法/网络安全法/数据安全法）';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_prevent_audit_log_update ON audit_log;
CREATE TRIGGER tr_prevent_audit_log_update
    BEFORE UPDATE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_log_modify();

DROP TRIGGER IF EXISTS tr_prevent_audit_log_delete ON audit_log;
CREATE TRIGGER tr_prevent_audit_log_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_log_modify();
