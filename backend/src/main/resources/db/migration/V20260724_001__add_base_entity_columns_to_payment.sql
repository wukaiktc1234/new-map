-- 为 payment 表补齐 BaseEntity 要求的字段
-- 解决财务付款单查询/保存时 "字段 created_by 不存在" 的错误
ALTER TABLE payment ADD COLUMN IF NOT EXISTS created_by VARCHAR(32);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS updated_by VARCHAR(32);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN payment.created_by IS '创建人（BaseEntity 标准字段）';
COMMENT ON COLUMN payment.updated_by IS '更新人（BaseEntity 标准字段）';
COMMENT ON COLUMN payment.version IS '乐观锁版本号（BaseEntity 标准字段）';
