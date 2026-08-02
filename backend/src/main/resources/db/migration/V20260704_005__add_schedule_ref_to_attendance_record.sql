-- ============================================================
-- 迁移脚本：给考勤记录表添加排班关联字段
-- 用于打通排班管理→考勤管理的同步链路（6.1.44 修复）
-- 兼容 PostgreSQL（生产）与 H2（开发，MODE=PostgreSQL）
-- ============================================================

-- 添加排班关联字段
ALTER TABLE attendance_record ADD COLUMN IF NOT EXISTS schedule_plan_id VARCHAR(32);
ALTER TABLE attendance_record ADD COLUMN IF NOT EXISTS schedule_entry_id VARCHAR(32);
ALTER TABLE attendance_record ADD COLUMN IF NOT EXISTS shift_type VARCHAR(32);
ALTER TABLE attendance_record ADD COLUMN IF NOT EXISTS expected_start_time TIMESTAMP;
ALTER TABLE attendance_record ADD COLUMN IF NOT EXISTS expected_end_time TIMESTAMP;

-- 添加索引（提升按排班方案/条目查询考勤记录的性能）
CREATE INDEX IF NOT EXISTS idx_attendance_record_schedule_plan_id ON attendance_record(schedule_plan_id);
CREATE INDEX IF NOT EXISTS idx_attendance_record_schedule_entry_id ON attendance_record(schedule_entry_id);
