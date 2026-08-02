-- ============================================================
-- Flyway Migration: V20260728_003__drop_position_level_system.sql
-- Description: 清理无效的职级体系相关表与字段
--   职级体系（position_levels / job_levels 等）经调研判定为无效配置：
--   - 无核心业务联动（薪资计算系数硬编码为 1，员工/岗位未真实关联）
--   - 前端独立管理页面无实际使用价值
--   - 入职审批路由使用的 position_level（STAFF/MANAGER/DIRECTOR/EXECUTIVE）
--     属于审批级别，与职级体系表无关，予以保留
-- ============================================================

BEGIN;

-- 1. 删除 employees 表与职级体系相关的外键字段
ALTER TABLE IF EXISTS employees
    DROP COLUMN IF EXISTS position_level_id,
    DROP COLUMN IF EXISTS salary_level;

-- 2. 删除 positions 表与职级体系相关的外键字段及冗余 level 字段
ALTER TABLE IF EXISTS positions
    DROP COLUMN IF EXISTS position_level_id,
    DROP COLUMN IF EXISTS level;

-- 3. 删除职级体系相关表（按依赖顺序：先子表/历史表，再主表）
DROP TABLE IF EXISTS position_level_history;
DROP TABLE IF EXISTS position_levels;
DROP TABLE IF EXISTS position_level_types;
DROP TABLE IF EXISTS job_levels;

-- 4. 清理与职级体系相关的索引（IF EXISTS 避免不存在时报错）
DROP INDEX IF EXISTS idx_employees_position_level_id;
DROP INDEX IF EXISTS idx_positions_position_level_id;
DROP INDEX IF EXISTS idx_position_levels_status;

COMMIT;
