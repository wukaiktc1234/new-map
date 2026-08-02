-- 为入职档案表补充现住址和人员类型字段
-- 修复入职管理页面因缺少 address/personnel_type 字段导致的 500 错误

ALTER TABLE onboarding_archive
    ADD COLUMN IF NOT EXISTS address VARCHAR(500);

ALTER TABLE onboarding_archive
    ADD COLUMN IF NOT EXISTS personnel_type VARCHAR(20) DEFAULT 'social';
