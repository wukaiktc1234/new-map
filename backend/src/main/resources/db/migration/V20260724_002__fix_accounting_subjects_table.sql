-- ============================================================
-- 修复会计科目表与实体类字段不匹配问题
-- 背景：
--   1. 旧迁移 V20260405__auto_vouchering_foundation.sql 创建的是单数表 accounting_subject，
--      主键为 id，字段为 code/name/category/type/balance_direction 等。
--   2. 实体类 AccountingSubject 使用复数表名 accounting_subjects，
--      主键为 subject_id，字段为 subject_code/subject_name/subject_type/direction 等。
--   3. V20260704_001__create_missing_entity_tables.sql 在表不存在时创建了
--      accounting_subjects 视图指向旧表，导致查询时字段 "subject_id" 不存在。
-- 修复内容：
--   - 若 accounting_subjects 为视图则删除
--   - 创建符合实体类定义的 accounting_subjects 表
--   - 预置付款凭证所需的 2202 应付账款、1002 银行存款科目
-- ============================================================

-- 1. 删除旧的复数视图或表（如果存在）
-- 注意：DROP VIEW IF EXISTS 在对象是表时会报错，因此先判断对象类型
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.views
        WHERE table_schema = 'public' AND table_name = 'accounting_subjects'
    ) THEN
        EXECUTE 'DROP VIEW IF EXISTS accounting_subjects';
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'accounting_subjects'
        AND table_type = 'BASE TABLE'
    ) THEN
        EXECUTE 'DROP TABLE IF EXISTS accounting_subjects';
    END IF;
END $$;

-- 2. 创建正确的 accounting_subjects 表
CREATE TABLE IF NOT EXISTS accounting_subjects (
    subject_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_code        VARCHAR(20) NOT NULL UNIQUE,
    subject_name        VARCHAR(100) NOT NULL,
    parent_id           BIGINT,
    subject_type        INTEGER NOT NULL,
    direction           INTEGER NOT NULL,
    is_leaf             BOOLEAN DEFAULT TRUE,
    status              INTEGER DEFAULT 1,
    remark              TEXT,
    balance             BIGINT NOT NULL DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0,
    version             INTEGER NOT NULL DEFAULT 0,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50)
);

-- 3. 预置付款凭证必需科目
INSERT INTO accounting_subjects (subject_code, subject_name, subject_type, direction, status, remark, balance)
VALUES
    ('2202', '应付账款', 2, 2, 1, 'M5财务闭环初始化', 0),
    ('1002', '银行存款', 1, 1, 1, 'M5财务闭环初始化', 0)
ON CONFLICT (subject_code) DO NOTHING;

-- 4. 索引
CREATE INDEX IF NOT EXISTS idx_accounting_subjects_parent_id ON accounting_subjects(parent_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_accounting_subjects_type ON accounting_subjects(subject_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_accounting_subjects_code ON accounting_subjects(subject_code) WHERE deleted = 0;
