-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 清除库存相关的假数据

-- 清除库存盘点明细数据
DELETE FROM INVENTORY_CHECK_DETAIL;

-- 清除库存盘点数据
DELETE FROM INVENTORY_CHECK;

-- 清除库存调拨数据
DELETE FROM INVENTORY_TRANSFER;

-- 清除库存报损数据
DELETE FROM INVENTORY_LOSS;

-- 清除库存日志数据
DELETE FROM INVENTORY_LOG;

-- 清除库存预警数据
DELETE FROM INVENTORY_WARNING;

-- 清除库存数据
DELETE FROM INVENTORY;

-- 清除产品数据
DELETE FROM PRODUCT;

-- 清除仓库数据
DELETE FROM WAREHOUSE;
