-- 为操作日志表添加business_id字段，用于关联业务对象（如订单）
-- 执行日期: 2026-02-27

-- 添加business_id字段（PostgreSQL兼容语法）
ALTER TABLE sys_operation_logs 
ADD COLUMN IF NOT EXISTS business_id VARCHAR(100);

COMMENT ON COLUMN sys_operation_logs.business_id IS '业务ID（如订单ID）';

-- 添加索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_operation_logs_business_id ON sys_operation_logs (business_id);
