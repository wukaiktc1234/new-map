-- ============================================================
-- V20260704_003: interviews 表新增人事面谈相关字段
-- ============================================================
-- 修复 6.1.33f/k：招聘流程人事面谈环节（完全缺失）
-- 新增 10 个字段支持人事面谈全流程：
--   - 人事面试官信息（hr_interviewer_id、hr_interviewer_name）
--   - 人事面评内容（hr_evaluation、hr_evaluation_time、hr_evaluation_score）
--   - 学历核验（education_verification、education_verification_remark）
--   - 背景调查（background_check_result、background_check_remark）
--   - 人事面谈状态（hr_interview_status）
-- 使用 IF NOT EXISTS 语法实现幂等，H2/PostgreSQL 均兼容。
-- ============================================================

ALTER TABLE interviews ADD COLUMN IF NOT EXISTS hr_interviewer_id VARCHAR(32);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS hr_interviewer_name VARCHAR(100);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS hr_evaluation TEXT;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS hr_evaluation_time TIMESTAMP;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS hr_evaluation_score INTEGER;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS education_verification VARCHAR(20);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS education_verification_remark TEXT;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS background_check_result VARCHAR(20);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS background_check_remark TEXT;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS hr_interview_status VARCHAR(20);

COMMENT ON COLUMN interviews.hr_interviewer_id IS '人事面试官ID';
COMMENT ON COLUMN interviews.hr_interviewer_name IS '人事面试官姓名';
COMMENT ON COLUMN interviews.hr_evaluation IS '人事面评内容';
COMMENT ON COLUMN interviews.hr_evaluation_time IS '人事面评提交时间';
COMMENT ON COLUMN interviews.hr_evaluation_score IS '人事面评评分';
COMMENT ON COLUMN interviews.education_verification IS '学历核验结果：pending/pass/fail';
COMMENT ON COLUMN interviews.education_verification_remark IS '学历核验备注';
COMMENT ON COLUMN interviews.background_check_result IS '背调结果：pending/pass/fail';
COMMENT ON COLUMN interviews.background_check_remark IS '背调备注';
COMMENT ON COLUMN interviews.hr_interview_status IS '人事面谈状态：pending/scheduled/completed/pass/fail';
