-- ============================================================
-- 工资记录表 (salary_records) - 补充字段
-- 版本: V20260626_002
-- 说明: salary_records 表由 V1.0.0.100 创建（salary_id_str/created_at/updated_at）
--        此迁移补充新命名兼容字段和注释
-- 日期: 2026-06-26
-- ============================================================

-- 补充 salary_id 字段（兼容新版命名，与 salary_id_str 并存）
ALTER TABLE salary_records ADD COLUMN IF NOT EXISTS salary_id VARCHAR(50);
-- 补充 create_time / update_time 字段（兼容新版命名，与 created_at/updated_at 并存）
ALTER TABLE salary_records ADD COLUMN IF NOT EXISTS create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE salary_records ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 索引（V1.0.0.100 已创建 employee_id/salary_month/status 索引）
CREATE INDEX IF NOT EXISTS idx_salary_records_employee ON salary_records(employee_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_salary_records_month ON salary_records(salary_month) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_salary_records_status ON salary_records(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_salary_records_store ON salary_records(store_id);

-- 注释
COMMENT ON TABLE salary_records IS '工资记录表';
COMMENT ON COLUMN salary_records.id IS '主键ID';
COMMENT ON COLUMN salary_records.salary_id IS '工资单号（新版命名）';
COMMENT ON COLUMN salary_records.employee_id IS '员工ID';
COMMENT ON COLUMN salary_records.employee_name IS '员工姓名';
COMMENT ON COLUMN salary_records.base_salary IS '基本工资';
COMMENT ON COLUMN salary_records.bonus IS '奖金';
COMMENT ON COLUMN salary_records.deduction IS '扣款';
COMMENT ON COLUMN salary_records.actual_salary IS '实发工资';
COMMENT ON COLUMN salary_records.salary_month IS '工资月份（如2026-06）';
COMMENT ON COLUMN salary_records.pay_time IS '发放时间';
COMMENT ON COLUMN salary_records.status IS '状态：pending-待发放/paid-已发放/cancelled-已取消';
COMMENT ON COLUMN salary_records.store_id IS '门店ID';
