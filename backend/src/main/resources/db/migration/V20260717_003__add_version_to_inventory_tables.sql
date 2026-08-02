-- ============================================================
-- 修复 P0-ERP-03：为库存表添加乐观锁 version 字段
-- 关联问题：architecture-review/C-erp-compliance-review.md P0-03 / ERP-015 / ERP-016
-- 修复日期：2026-07-17
-- 说明：
--   1. Inventory / StoreInventory 实体已添加 @Version 注解（MyBatis-Plus 乐观锁）
--   2. 本脚本为库存表补齐 version 字段，使乐观锁插件生效
--   3. 使用 ADD COLUMN IF NOT EXISTS 确保幂等可重复执行
--   4. 现有数据初始化为 version = 0（首次更新时由乐观锁插件递增）
-- 风险场景（修复前）：
--   - 高并发下单：多请求同时读取库存=100，各自扣减 50，最终库存应为 0，实际写入 50 → 超卖
--   - 锁定/扣减交叉：锁定 50 同时扣减 60，可用库存检查通过但实际不足 → 超卖
-- 修复机制：
--   - MyBatis-Plus 在 UPDATE 时自动添加 WHERE version = ? 条件
--   - 版本不匹配时 updateById 返回 0，应用层捕获后重试或抛出 INVENTORY_CONFLICT
-- ============================================================

-- ------------------------------------------------------
-- 1. inventory 表添加 version 字段
-- ------------------------------------------------------
DO $$
BEGIN
    ALTER TABLE inventory ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    COMMENT ON COLUMN inventory.version IS '乐观锁版本号（MyBatis-Plus @Version 自动维护）';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'inventory 表 version 字段添加跳过: %', SQLERRM;
END $$;

-- ------------------------------------------------------
-- 2. store_inventory 表添加 version 字段
-- ------------------------------------------------------
DO $$
BEGIN
    ALTER TABLE store_inventory ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    COMMENT ON COLUMN store_inventory.version IS '乐观锁版本号（MyBatis-Plus @Version 自动维护）';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'store_inventory 表 version 字段添加跳过: %', SQLERRM;
END $$;

-- ------------------------------------------------------
-- 3. 数据初始化：将 NULL 或缺失的 version 设置为 0
--    （NOT NULL DEFAULT 0 已保证新数据，此处兜底历史数据）
-- ------------------------------------------------------
DO $$
BEGIN
    UPDATE inventory SET version = 0 WHERE version IS NULL;
    UPDATE store_inventory SET version = 0 WHERE version IS NULL;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'version 数据初始化跳过: %', SQLERRM;
END $$;
