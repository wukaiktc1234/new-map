-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 食品溯源系统 - 数据清空脚本
-- 清空所有表中的数据

-- 关闭外键约束检查
SET FOREIGN_KEY_CHECKS = 0;

-- 清空所有表数据

-- 健康证相关表
TRUNCATE TABLE health_certificate;
TRUNCATE TABLE health_certificate_log;

-- 库存相关表
TRUNCATE TABLE inventory_check_detail;
TRUNCATE TABLE inventory_check;
TRUNCATE TABLE inventory_transfer;
TRUNCATE TABLE inventory_loss;
TRUNCATE TABLE inventory_log;
TRUNCATE TABLE inventory_warning;
TRUNCATE TABLE inventory;

-- 产品相关表
TRUNCATE TABLE product;

-- 仓库相关表
TRUNCATE TABLE warehouse;

-- 采购相关表
TRUNCATE TABLE purchase_order_item;
TRUNCATE TABLE purchase_order;
TRUNCATE TABLE supplier;

-- 销售相关表
TRUNCATE TABLE sales_order_item;
TRUNCATE TABLE sales_order;
TRUNCATE TABLE customer;

-- 生产相关表
TRUNCATE TABLE production_batch;
TRUNCATE TABLE production_process;
TRUNCATE TABLE production_plan;

-- 质检相关表
TRUNCATE TABLE quality_inspection;
TRUNCATE TABLE inspection_item;

-- 财务相关表
TRUNCATE TABLE finance_statistics;
TRUNCATE TABLE finance_record;

-- 促销相关表
TRUNCATE TABLE promotions;

-- 系统管理相关表
TRUNCATE TABLE user_roles;
TRUNCATE TABLE role_permissions;
TRUNCATE TABLE permissions;
TRUNCATE TABLE roles;
TRUNCATE TABLE users;
TRUNCATE TABLE departments;

-- 开启外键约束检查
SET FOREIGN_KEY_CHECKS = 1;

-- 清空序列或自增ID（如果有）
ALTER TABLE departments AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE roles AUTO_INCREMENT = 1;
ALTER TABLE permissions AUTO_INCREMENT = 1;
ALTER TABLE product AUTO_INCREMENT = 1;
ALTER TABLE warehouse AUTO_INCREMENT = 1;
ALTER TABLE inventory AUTO_INCREMENT = 1;
ALTER TABLE inventory_warning AUTO_INCREMENT = 1;
ALTER TABLE inventory_log AUTO_INCREMENT = 1;
ALTER TABLE inventory_loss AUTO_INCREMENT = 1;
ALTER TABLE inventory_transfer AUTO_INCREMENT = 1;
ALTER TABLE inventory_check AUTO_INCREMENT = 1;
ALTER TABLE inventory_check_detail AUTO_INCREMENT = 1;
ALTER TABLE promotions AUTO_INCREMENT = 1;
ALTER TABLE finance_record AUTO_INCREMENT = 1;
ALTER TABLE finance_statistics AUTO_INCREMENT = 1;
ALTER TABLE health_certificate AUTO_INCREMENT = 1;
ALTER TABLE health_certificate_log AUTO_INCREMENT = 1;

-- 清空配置表（如果有）
TRUNCATE TABLE system_config;
TRUNCATE TABLE dict_item;
TRUNCATE TABLE dict_type;

-- 清空日志表（如果有）
TRUNCATE TABLE operation_log;
TRUNCATE TABLE login_log;

-- 清空文件表（如果有）
TRUNCATE TABLE file_info;

-- 清空消息表（如果有）
TRUNCATE TABLE message;
TRUNCATE TABLE message_receiver;

-- 清空通知表（如果有）
TRUNCATE TABLE notification;
TRUNCATE TABLE notification_receiver;

-- 清空统计图表相关表（如果有）
TRUNCATE TABLE chart_data;
TRUNCATE TABLE chart_config;
TRUNCATE TABLE report_data;
TRUNCATE TABLE report_template;

-- 开启外键约束检查
SET FOREIGN_KEY_CHECKS = 1;

-- 提交事务
COMMIT;

-- 显示清空结果
SELECT '所有数据已清空完成！' AS result;