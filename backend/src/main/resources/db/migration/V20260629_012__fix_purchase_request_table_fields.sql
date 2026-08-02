-- ============================================================
-- V20260629_012: 修复 purchase_request 表字段命名不一致问题
-- ============================================================
-- 背景：
--   - V1.0.0.100 中 purchase_request 使用 created_at/updated_at/created_by/updated_by
--   - H2 建表脚本（PurchaseDatabaseInitializer）使用 create_time/update_time/create_by/update_by
--   - Entity PurchaseRequest 的 createTime 已显式 @TableField("create_time")
--   - createBy/updateBy/deletedBy/deletedTime 也将补齐 @TableField 注解（指向 create_by/update_by/deleted_by/deleted_time）
--   - 因此 PostgreSQL 表结构需对齐 H2，新增 create_time/update_time/create_by/update_by/deleted_by/deleted_time
--   - 本脚本仅在生产 PostgreSQL 上执行（H2 环境 Flyway 禁用，由 PurchaseDatabaseInitializer 维护）
--
-- 处理策略：
--   1. 新增 create_time/update_time/create_by/update_by/deleted_by/deleted_time 列
--   2. 将旧列（created_at/updated_at/created_by/updated_by）数据迁移到新列
--   3. 旧列保留（避免破坏性变更），由后续清理脚本统一处理
--   4. 删除旧索引 idx_purchase_request_created_at，新增 idx_purchase_request_create_time
-- ============================================================

-- 1. 新增字段（幂等）
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS create_by VARCHAR(50);
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS update_by VARCHAR(50);
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50);
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS deleted_time TIMESTAMP;

-- 2. 数据迁移：将旧列数据复制到新列（仅在新列为空时复制，避免覆盖已有数据）
--    created_at → create_time
UPDATE purchase_request
SET create_time = created_at
WHERE create_time IS NULL AND created_at IS NOT NULL;

--    updated_at → update_time
UPDATE purchase_request
SET update_time = updated_at
WHERE update_time IS NULL AND updated_at IS NOT NULL;

--    created_by → create_by（注意：两者列名仅差一个字符，需仔细区分）
UPDATE purchase_request
SET create_by = created_by
WHERE create_by IS NULL AND created_by IS NOT NULL;

--    updated_by → update_by
UPDATE purchase_request
SET update_by = updated_by
WHERE update_by IS NULL AND updated_by IS NOT NULL;

-- 3. 索引调整：删除旧索引，新建基于新列的索引
DROP INDEX IF EXISTS idx_purchase_request_created_at;
CREATE INDEX IF NOT EXISTS idx_purchase_request_create_time ON purchase_request (create_time);

-- 4. 字段注释
COMMENT ON COLUMN purchase_request.create_time IS '创建时间（与 H2 环境统一为 create_time）';
COMMENT ON COLUMN purchase_request.update_time IS '更新时间（与 H2 环境统一为 update_time）';
COMMENT ON COLUMN purchase_request.create_by IS '创建人';
COMMENT ON COLUMN purchase_request.update_by IS '更新人';
COMMENT ON COLUMN purchase_request.deleted_by IS '删除人';
COMMENT ON COLUMN purchase_request.deleted_time IS '删除时间';
