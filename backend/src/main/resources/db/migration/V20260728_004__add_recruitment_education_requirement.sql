-- ============================================================
-- 迁移脚本: V20260728_004__add_recruitment_education_requirement.sql
-- 任务: 为招聘需求表补充学历要求字段，支持前端职位表单联动
-- ============================================================

ALTER TABLE recruitment_requirements
    ADD COLUMN IF NOT EXISTS education_requirement VARCHAR(20);

COMMENT ON COLUMN recruitment_requirements.education_requirement IS '学历要求: none-不限 high_school-高中 college-大专 bachelor-本科 master-硕士';
