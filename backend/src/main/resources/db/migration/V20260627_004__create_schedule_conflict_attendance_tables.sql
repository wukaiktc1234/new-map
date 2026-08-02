-- ============================================================
-- P1-3 Schedule 排班模块补齐：冲突记录 + 考勤同步 + 换班审批意见
-- 兼容 H2 (MODE=PostgreSQL) 与 PostgreSQL 18
-- ============================================================

-- Part 1: 排班冲突记录表（schedule_conflicts）
-- 存储排班冲突检查发现的问题，用于发布前校验和修复提示
CREATE TABLE IF NOT EXISTS schedule_conflicts (
    conflict_id VARCHAR(32) PRIMARY KEY,
    plan_id VARCHAR(32) NOT NULL,
    level VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    entry_id VARCHAR(32),
    employee_id VARCHAR(32),
    employee_name VARCHAR(50),
    conflict_date DATE,
    shift_type VARCHAR(30),
    rule_id VARCHAR(32),
    message VARCHAR(500) NOT NULL,
    suggestion VARCHAR(500),
    auto_fix_available BOOLEAN NOT NULL DEFAULT FALSE,
    fix_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    fixed_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_schedule_conflicts_plan ON schedule_conflicts(plan_id);
CREATE INDEX IF NOT EXISTS idx_schedule_conflicts_level ON schedule_conflicts(plan_id, level);
CREATE INDEX IF NOT EXISTS idx_schedule_conflicts_employee ON schedule_conflicts(employee_id);
CREATE INDEX IF NOT EXISTS idx_schedule_conflicts_fix_status ON schedule_conflicts(fix_status);
CREATE INDEX IF NOT EXISTS idx_schedule_conflicts_deleted ON schedule_conflicts(deleted);

COMMENT ON TABLE schedule_conflicts IS '排班冲突记录表';
COMMENT ON COLUMN schedule_conflicts.level IS '冲突级别: error-严重 warning-警告 info-提示';
COMMENT ON COLUMN schedule_conflicts.fix_status IS '修复状态: pending-待处理 fixed-已修复 ignored-已忽略';
COMMENT ON COLUMN schedule_conflicts.auto_fix_available IS '是否支持自动修复';

ALTER TABLE schedule_conflicts ADD CONSTRAINT chk_conflict_level
    CHECK (level IN ('error', 'warning', 'info'));
ALTER TABLE schedule_conflicts ADD CONSTRAINT chk_conflict_fix_status
    CHECK (fix_status IN ('pending', 'fixed', 'ignored'));

-- Part 2: 排班考勤同步记录表（schedule_attendance_syncs）
-- 记录排班数据同步到考勤系统的执行情况
CREATE TABLE IF NOT EXISTS schedule_attendance_syncs (
    sync_id VARCHAR(32) PRIMARY KEY,
    plan_id VARCHAR(32) NOT NULL,
    sync_status VARCHAR(20) NOT NULL DEFAULT 'not_synced',
    sync_time TIMESTAMP,
    total_entries INTEGER NOT NULL DEFAULT 0,
    synced_entries INTEGER NOT NULL DEFAULT 0,
    failed_entries INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    operator_id VARCHAR(32),
    operator_name VARCHAR(50),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_schedule_attendance_syncs_plan ON schedule_attendance_syncs(plan_id);
CREATE INDEX IF NOT EXISTS idx_schedule_attendance_syncs_status ON schedule_attendance_syncs(sync_status);
CREATE INDEX IF NOT EXISTS idx_schedule_attendance_syncs_time ON schedule_attendance_syncs(sync_time);
CREATE INDEX IF NOT EXISTS idx_schedule_attendance_syncs_deleted ON schedule_attendance_syncs(deleted);

COMMENT ON TABLE schedule_attendance_syncs IS '排班考勤同步记录表';
COMMENT ON COLUMN schedule_attendance_syncs.sync_status IS '同步状态: not_synced-未同步 synced-已同步 failed-失败';

ALTER TABLE schedule_attendance_syncs ADD CONSTRAINT chk_sync_status
    CHECK (sync_status IN ('not_synced', 'synced', 'failed'));

-- Part 3: 给 swap_requests 表补充 approve_comment 列
-- 用于记录审批通过时的审批意见（原有 reject_reason 仅记录拒绝原因）
ALTER TABLE swap_requests ADD COLUMN IF NOT EXISTS approve_comment VARCHAR(500);

COMMENT ON COLUMN swap_requests.approve_comment IS '审批意见(通过时记录)';
