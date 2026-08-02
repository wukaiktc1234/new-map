-- ============================================================
-- 修复 suppliers 表结构：对齐 Supplier Entity 规范字段
-- 创建时间：2026-06-29
-- 说明：
--   1. 旧表 V1.0.0.100 创建时字段与 Entity 不一致（已执行的脚本不可改，故用 ALTER 修复）
--   2. 修复点：
--      a) supplier_id_str → supplier_code（重命名）
--      b) created_at → create_time, updated_at → update_time（重命名）
--      c) status VARCHAR('active') → INTEGER（类型转换，'active'→1, 'inactive'→0, 其它→2）
--      d) deleted SMALLINT → INTEGER（类型转换）
--      e) 新增缺失字段：license_no, license_expiry, bank_account, bank_name,
--         tax_no, category, credit_level, rating, remark
--   3. 兼容 H2 (MODE=PostgreSQL) + PostgreSQL 18
-- ============================================================

-- 1. 重命名字段（H2 和 PostgreSQL 都支持 RENAME COLUMN）
ALTER TABLE suppliers RENAME COLUMN supplier_id_str TO supplier_code;
ALTER TABLE suppliers RENAME COLUMN created_at TO create_time;
ALTER TABLE suppliers RENAME COLUMN updated_at TO update_time;

-- 2. 状态字段类型转换 VARCHAR → INTEGER
--    使用添加新字段 + 数据迁移 + 删旧字段 + 重命名的方式（兼容 H2 + PostgreSQL）
ALTER TABLE suppliers ADD COLUMN status_new INTEGER DEFAULT 1;
UPDATE suppliers SET status_new = CASE
    WHEN status = 'active' THEN 1
    WHEN status = 'inactive' THEN 0
    ELSE 2
END;
ALTER TABLE suppliers DROP COLUMN status;
ALTER TABLE suppliers RENAME COLUMN status_new TO status;

-- 3. deleted 类型转换 SMALLINT → INTEGER
ALTER TABLE suppliers ADD COLUMN deleted_new INTEGER DEFAULT 0;
UPDATE suppliers SET deleted_new = deleted;
ALTER TABLE suppliers DROP COLUMN deleted;
ALTER TABLE suppliers RENAME COLUMN deleted_new TO deleted;

-- 4. 添加缺失字段（IF NOT EXISTS 兼容 H2 + PostgreSQL 9.6+）
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS license_no     VARCHAR(50);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS license_expiry DATE;
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS bank_account   VARCHAR(50);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS bank_name     VARCHAR(100);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS tax_no        VARCHAR(50);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS category      VARCHAR(50);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS credit_level   VARCHAR(10)  DEFAULT 'C';
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS rating         DECIMAL(5,2) DEFAULT 5.00;
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS remark         VARCHAR(500);

-- 5. 修复索引（旧索引基于旧字段名，重建为新字段名）
DROP INDEX IF EXISTS idx_suppliers_status;
CREATE INDEX IF NOT EXISTS idx_suppliers_status ON suppliers (status);
DROP INDEX IF EXISTS idx_suppliers_phone;
CREATE INDEX IF NOT EXISTS idx_suppliers_phone ON suppliers (phone);

-- 6. 表/字段注释
COMMENT ON TABLE  suppliers                  IS '供应商表';
COMMENT ON COLUMN suppliers.supplier_code   IS '供应商编码（业务唯一）';
COMMENT ON COLUMN suppliers.license_no      IS '营业执照号';
COMMENT ON COLUMN suppliers.license_expiry  IS '营业执照有效期';
COMMENT ON COLUMN suppliers.bank_account    IS '银行账号';
COMMENT ON COLUMN suppliers.bank_name       IS '开户银行';
COMMENT ON COLUMN suppliers.tax_no          IS '税号';
COMMENT ON COLUMN suppliers.category        IS '供应商分类（原材料/包装/设备/其他）';
COMMENT ON COLUMN suppliers.status          IS '状态：1合作中 0停用 2黑名单';
COMMENT ON COLUMN suppliers.credit_level    IS '信用等级（A/B/C/D）';
COMMENT ON COLUMN suppliers.rating          IS '综合评分（0-10，默认 5.00）';
COMMENT ON COLUMN suppliers.remark          IS '备注';
COMMENT ON COLUMN suppliers.create_time     IS '创建时间';
COMMENT ON COLUMN suppliers.update_time     IS '更新时间';
COMMENT ON COLUMN suppliers.deleted         IS '逻辑删除：0未删除 1已删除';
