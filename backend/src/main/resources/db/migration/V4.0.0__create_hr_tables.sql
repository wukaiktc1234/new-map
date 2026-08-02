-- ============================================================
-- HR人力资源系统 - 数据库迁移脚本
-- 版本: V4.0.0
-- 说明: 创建HR考勤记录表，增强现有HR相关表结构
-- 日期: 2026-04-25
-- ============================================================

-- ----------------------------------------------------------
-- 1. 考勤记录表（attendance_record）
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance_record (
    record_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id        VARCHAR(32) NOT NULL,                -- 员工ID
    attendance_date    DATE NOT NULL,                      -- 考勤日期
    clock_in_time      TIME,                               -- 上班打卡时间
    clock_out_time     TIME,                               -- 下班打卡时间
    work_hours         NUMERIC(5,2) DEFAULT 0,             -- 工作时长（小时）
    overtime_hours     NUMERIC(5,2) DEFAULT 0,             -- 加班时长（小时）
    leave_type         INTEGER DEFAULT 0,                  -- 请假类型（0正常 1事假 2病假 3年假 4调休 5其他）
    leave_hours        NUMERIC(5,2) DEFAULT 0,             -- 请假时长（小时）
    late_minutes       INTEGER DEFAULT 0,                  -- 迟到分钟数
    early_leave_minutes INTEGER DEFAULT 0,                 -- 早退分钟数
    status             INTEGER DEFAULT 1,                  -- 考勤状态（1正常 2迟到 3早退 4缺勤 5加班 6请假）
    remark             TEXT,                               -- 备注
    create_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER NOT NULL DEFAULT 0           -- 逻辑删除（0未删 1已删）
);

COMMENT ON TABLE attendance_record IS '员工考勤记录表';
COMMENT ON COLUMN attendance_record.record_id IS '考勤记录ID';
COMMENT ON COLUMN attendance_record.employee_id IS '员工ID';
COMMENT ON COLUMN attendance_record.attendance_date IS '考勤日期';
COMMENT ON COLUMN attendance_record.clock_in_time IS '上班打卡时间';
COMMENT ON COLUMN attendance_record.clock_out_time IS '下班打卡时间';
COMMENT ON COLUMN attendance_record.work_hours IS '工作时长（小时）';
COMMENT ON COLUMN attendance_record.overtime_hours IS '加班时长（小时）';
COMMENT ON COLUMN attendance_record.leave_type IS '请假类型：0正常 1事假 2病假 3年假 4调休 5其他';
COMMENT ON COLUMN attendance_record.leave_hours IS '请假时长（小时）';
COMMENT ON COLUMN attendance_record.late_minutes IS '迟到分钟数';
COMMENT ON COLUMN attendance_record.early_leave_minutes IS '早退分钟数';
COMMENT ON COLUMN attendance_record.status IS '考勤状态：1正常 2迟到 3早退 4缺勤 5加班 6请假';

-- 索引创建
CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_employee_date
    ON attendance_record (employee_id, attendance_date)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_attendance_employee_id
    ON attendance_record (employee_id);

CREATE INDEX IF NOT EXISTS idx_attendance_date
    ON attendance_record (attendance_date);

CREATE INDEX IF NOT EXISTS idx_attendance_status
    ON attendance_record (status);

-- ----------------------------------------------------------
-- 2. 为salary_records表补充缺失字段（如果不存在）
-- ----------------------------------------------------------

-- 检查并添加position_salary字段（岗位工资，单位：分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'position_salary'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN position_salary BIGINT DEFAULT 0;
        COMMENT ON COLUMN salary_records.position_salary IS '岗位工资（分）';
    END IF;
END $$;

-- 检查并添加performance_bonus字段（绩效奖金，单位：分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'performance_bonus'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN performance_bonus BIGINT DEFAULT 0;
        COMMENT ON COLUMN salary_records.performance_bonus IS '绩效奖金（分）';
    END IF;
END $$;

-- 检查并添加overtime_pay字段（加班费，单位：分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'overtime_pay'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN overtime_pay BIGINT DEFAULT 0;
        COMMENT ON COLUMN salary_records.overtime_pay IS '加班费（分）';
    END IF;
END $$;

-- 检查并添加deduction字段（扣款，单位：分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'deduction'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN deduction BIGINT DEFAULT 0;
        COMMENT ON COLUMN salary_records.deduction IS '扣款（分）';
    END IF;
END $$;

-- 检查并添加social_insurance字段（社保个人部分，单位：分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'social_insurance'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN social_insurance BIGINT DEFAULT 0;
        COMMENT ON COLUMN salary_records.social_insurance IS '社保个人部分（分）';
    END IF;
END $$;

-- 检查并添加housing_fund字段（公积金个人部分，单位：分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'housing_fund'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN housing_fund BIGINT DEFAULT 0;
        COMMENT ON COLUMN salary_records.housing_fund IS '公积金个人部分（分）';
    END IF;
END $$;

-- 检查并添加tax字段（个人所得税，单位：分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'tax'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN tax BIGINT DEFAULT 0;
        COMMENT ON COLUMN salary_records.tax IS '个人所得税（分）';
    END IF;
END $$;

-- 检查并添加actual_salary字段（实发工资，单位：分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'actual_salary'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN actual_salary BIGINT DEFAULT 0;
        COMMENT ON COLUMN salary_records.actual_salary IS '实发工资（分）';
    END IF;
END $$;

-- 检查并添加pay_date字段（发放日期）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'pay_date'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN pay_date DATE;
        COMMENT ON COLUMN salary_records.pay_date IS '发放日期';
    END IF;
END $$;

-- 检查并更新status字段为更精确的状态值
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'pay_status'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN pay_status INTEGER DEFAULT 0;
        COMMENT ON COLUMN salary_records.pay_status IS '发放状态：0未发 1已发';
    END IF;
END $$;

-- 检查并添加deleted字段到salary_records（逻辑删除）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'salary_records' AND column_name = 'deleted'
    ) THEN
        ALTER TABLE salary_records ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;
        CREATE INDEX IF NOT EXISTS idx_salary_records_deleted ON salary_records(deleted);
    END IF;
END $$;

-- ----------------------------------------------------------
-- 3. 为employees表补充HR关键字段（如果不存在）
-- ----------------------------------------------------------

-- 添加工号字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'employee_no'
    ) THEN
        ALTER TABLE employees ADD COLUMN employee_no VARCHAR(32);
        CREATE INDEX IF NOT EXISTS uk_employees_no ON employees(employee_no) WHERE deleted = 0;
    END IF;
END $$;

-- 添加性别字段（INTEGER类型：0未知 1男 2女）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'gender_code'
    ) THEN
        ALTER TABLE employees ADD COLUMN gender_code INTEGER DEFAULT 0;
        COMMENT ON COLUMN employees.gender_code IS '性别代码：0未知 1男 2女';
    END IF;
END $$;

-- 添加身份证号
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'id_card'
    ) THEN
        ALTER TABLE employees ADD COLUMN id_card VARCHAR(18);
    END IF;
END $$;

-- 添加入职日期
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'entry_date'
    ) THEN
        ALTER TABLE employees ADD COLUMN entry_date DATE;
        COMMENT ON COLUMN employees.entry_date IS '入职日期';
    END IF;
END $$;

-- 添加转正日期
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'regular_date'
    ) THEN
        ALTER TABLE employees ADD COLUMN regular_date DATE;
        COMMENT ON COLUMN employees.regular_date IS '转正日期';
    END IF;
END $$;

-- 添加员工类型（1全职 2兼职 3实习 4外包）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'employee_type'
    ) THEN
        ALTER TABLE employees ADD COLUMN employee_type INTEGER DEFAULT 1;
        COMMENT ON COLUMN employees.employee_type IS '员工类型：1全职 2兼职 3实习 4外包';
    END IF;
END $$;

-- 添加员工状态（1在职 2试用期 3离职 4退休）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'employee_status'
    ) THEN
        ALTER TABLE employees ADD COLUMN employee_status INTEGER DEFAULT 1;
        COMMENT ON COLUMN employees.employee_status IS '员工状态：1在职 2试用期 3离职 4退休';
        CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(employee_status) WHERE deleted = 0;
    END IF;
END $$;

-- 添加学历
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'education'
    ) THEN
        ALTER TABLE employees ADD COLUMN education VARCHAR(20);
    END IF;
END $$;

-- 添加紧急联系人
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'emergency_contact'
    ) THEN
        ALTER TABLE employees ADD COLUMN emergency_contact VARCHAR(50);
    END IF;
END $$;

-- 添加紧急联系电话
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'emergency_phone'
    ) THEN
        ALTER TABLE employees ADD COLUMN emergency_phone VARCHAR(20);
    END IF;
END $$;

-- 添加银行账号
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'bank_account'
    ) THEN
        ALTER TABLE employees ADD COLUMN bank_account VARCHAR(50);
    END IF;
END $$;

-- 添加职级ID
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'job_level_id'
    ) THEN
        ALTER TABLE employees ADD COLUMN job_level_id BIGINT;
    END IF;
END $$;

-- ----------------------------------------------------------
-- 4. 为position_levels表补充薪资范围字段（如果不存在）
-- ----------------------------------------------------------

-- 添加最低薪资（分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'position_levels' AND column_name = 'min_salary'
    ) THEN
        ALTER TABLE position_levels ADD COLUMN min_salary BIGINT DEFAULT 0;
        COMMENT ON COLUMN position_levels.min_salary IS '最低薪资（分）';
    END IF;
END $$;

-- 添加最高薪资（分）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'position_levels' AND column_name = 'max_salary'
    ) THEN
        ALTER TABLE position_levels ADD COLUMN max_salary BIGINT DEFAULT 0;
        COMMENT ON COLUMN position_levels.max_salary IS '最高薪资（分）';
    END IF;
END $$;

-- 添加职级类型（1专业序列 2管理序列）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'position_levels' AND column_name = 'level_type'
    ) THEN
        ALTER TABLE position_levels ADD COLUMN level_type INTEGER DEFAULT 1;
        COMMENT ON COLUMN position_levels.level_type IS '职级类型：1专业序列 2管理序列';
    END IF;
END $$;

-- ----------------------------------------------------------
-- 5. 为positions表补充HR字段（如果不存在）
-- ----------------------------------------------------------

-- 添加编制人数
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'positions' AND column_name = 'head_count'
    ) THEN
        ALTER TABLE positions ADD COLUMN head_count INTEGER DEFAULT 0;
        COMMENT ON COLUMN positions.head_count IS '编制人数';
    END IF;
END $$;

-- 添加现有人数
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'positions' AND column_name = 'current_count'
    ) THEN
        ALTER TABLE positions ADD COLUMN current_count INTEGER DEFAULT 0;
        COMMENT ON COLUMN positions.current_count IS '现有人数';
    END IF;
END $$;

-- 添加任职要求
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'positions' AND column_name = 'requirements'
    ) THEN
        ALTER TABLE positions ADD COLUMN requirements TEXT;
        COMMENT ON COLUMN positions.requirements IS '任职要求';
    END IF;
END $$;

-- 添加职位级别
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'positions' AND column_name = 'position_level'
    ) THEN
        ALTER TABLE positions ADD COLUMN position_level VARCHAR(20);
        COMMENT ON COLUMN positions.position_level IS '职位级别';
    END IF;
END $$;
