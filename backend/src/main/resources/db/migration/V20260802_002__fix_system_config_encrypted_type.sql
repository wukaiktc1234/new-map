-- ============================================================
-- 修复 system_config.encrypted 列类型：SMALLINT → BOOLEAN
-- 根因: 实体 SystemConfig.encrypted 为 Boolean，MyBatis-Plus 绑定
--       boolean 参数与 SMALLINT 列不兼容，插入时报
--       "字段 encrypted 的类型为 smallint, 但表达式的类型为 boolean"
-- 幂等: 列已是 boolean 时 encrypted::int 仍可求值，可重复执行
-- ============================================================

ALTER TABLE system_config ALTER COLUMN encrypted DROP DEFAULT;
ALTER TABLE system_config ALTER COLUMN encrypted TYPE boolean USING (encrypted::int <> 0);
ALTER TABLE system_config ALTER COLUMN encrypted SET DEFAULT false;
