-- 供应商表结构更新（PostgreSQL兼容）
-- 添加缺失的字段以支持完整的供应商管理功能

ALTER TABLE suppliers 
ADD COLUMN IF NOT EXISTS email VARCHAR(100),
ADD COLUMN IF NOT EXISTS category VARCHAR(50),
ADD COLUMN IF NOT EXISTS rating DECIMAL(2,1) DEFAULT 0,
ADD COLUMN IF NOT EXISTS cooperation_years INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS bank_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS bank_account VARCHAR(50),
ADD COLUMN IF NOT EXISTS tax_id VARCHAR(50),
ADD COLUMN IF NOT EXISTS remark VARCHAR(500);

COMMENT ON COLUMN suppliers.email IS '邮箱';
COMMENT ON COLUMN suppliers.category IS '供应类别';
COMMENT ON COLUMN suppliers.rating IS '评分';
COMMENT ON COLUMN suppliers.cooperation_years IS '合作年限';
COMMENT ON COLUMN suppliers.bank_name IS '开户银行';
COMMENT ON COLUMN suppliers.bank_account IS '银行账号';
COMMENT ON COLUMN suppliers.tax_id IS '纳税人识别号';
COMMENT ON COLUMN suppliers.remark IS '备注';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_suppliers_category ON suppliers (category);
CREATE INDEX IF NOT EXISTS idx_suppliers_status ON suppliers (status);
