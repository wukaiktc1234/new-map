-- ============================================================
-- 修复 stores.deleted 列类型：BOOLEAN → INTEGER
-- 根因：V20260707_002 建表脚本误用 BOOLEAN，与 MyBatis-Plus 全局配置
--       (logic-delete-value: 1, logic-not-delete-value: 0) 不兼容
-- 现象：所有查询 stores 表的接口报 500 错误
--       "操作符不存在: boolean = integer"
-- 修复：将 deleted 列从 BOOLEAN 转为 INTEGER，符合项目规范
--       "逻辑删除: deleted字段，INTEGER类型，0未删除，1已删除"
-- ============================================================

-- 1. 移除现有默认值
ALTER TABLE stores ALTER COLUMN deleted DROP DEFAULT;

-- 2. 转换列类型：TRUE → 1, FALSE → 0
ALTER TABLE stores ALTER COLUMN deleted TYPE INTEGER USING CASE WHEN deleted THEN 1 ELSE 0 END;

-- 3. 设置默认值为 0（未删除），与项目规范一致
ALTER TABLE stores ALTER COLUMN deleted SET DEFAULT 0;

-- 4. 确保 NOT NULL 约束
ALTER TABLE stores ALTER COLUMN deleted SET NOT NULL;
