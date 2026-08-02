-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 数据库迁移脚本：从配料管理迁移到库存产品管理
-- 步骤1：创建新的库存产品关联表
SOURCE create_dish_inventory_table.sql;
SOURCE create_combo_inventory_table.sql;

-- 步骤2：数据迁移（如果需要保留现有数据）
-- 注意：由于ingredients表与inventory表结构不同，需要手动映射数据
-- 这里只提供迁移脚本框架，实际使用时需要根据具体数据情况进行调整

-- 步骤3：删除旧的配料相关表
-- DROP TABLE IF EXISTS `dish_ingredient`;
-- DROP TABLE IF EXISTS `combo_ingredient`;
-- DROP TABLE IF EXISTS `ingredients`;
-- DROP TABLE IF EXISTS `ingredient_categories`;
