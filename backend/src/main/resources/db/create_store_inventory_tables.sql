-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 门店库存系统数据库迁移脚本
-- 创建门店表和更新库存表以支持门店库存管理

-- 步骤1: 创建门店表
CREATE TABLE IF NOT EXISTS `stores` (
    `store_id` VARCHAR(50) NOT NULL COMMENT '门店ID',
    `store_name` VARCHAR(100) NOT NULL COMMENT '门店名称',
    `store_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '门店编码（唯一）',
    `address` VARCHAR(255) COMMENT '门店地址',
    `phone` VARCHAR(20) COMMENT '联系电话',
    `manager_id` VARCHAR(50) COMMENT '负责人ID',
    `manager_name` VARCHAR(50) COMMENT '负责人姓名',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '门店状态（active: 正常, inactive: 禁用）',
    `company_id` VARCHAR(50) COMMENT '所属公司ID',
    `region` VARCHAR(50) COMMENT '所属区域',
    `store_type` VARCHAR(20) DEFAULT 'single' COMMENT '门店类型（single: 单店, chain: 连锁店）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` VARCHAR(50) COMMENT '创建人',
    `updated_by` VARCHAR(50) COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    `version` INT DEFAULT 1 COMMENT '版本号（乐观锁）',
    `ext_data` TEXT COMMENT '扩展字段（JSON格式存储）',
    PRIMARY KEY (`store_id`),
    KEY `idx_store_code` (`store_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店表';

-- 步骤2: 更新库存表，添加门店相关字段
ALTER TABLE `INVENTORY`
ADD COLUMN `STORE_ID` BIGINT DEFAULT NULL COMMENT '门店ID（门店库存时使用）' AFTER `WAREHOUSE_NAME`,
ADD COLUMN `STORE_NAME` VARCHAR(100) DEFAULT NULL COMMENT '门店名称（门店库存时使用）' AFTER `STORE_ID`,
ADD COLUMN `INVENTORY_TYPE` TINYINT DEFAULT 1 COMMENT '库存类型（1:仓库库存,2:门店库存）' AFTER `UNIT`,
ADD COLUMN `COST_PRICE` DECIMAL(10, 2) DEFAULT 0 COMMENT '成本单价' AFTER `INVENTORY_TYPE`,
ADD COLUMN `STOCK_VALUE` DECIMAL(12, 2) DEFAULT 0 COMMENT '库存价值' AFTER `COST_PRICE`,
ADD COLUMN `WARNING_LEVEL` TINYINT DEFAULT 0 COMMENT '预警级别（0:正常,1:提示,2:警告,3:严重）' AFTER `STOCK_VALUE`,
ADD INDEX `idx_store_id` (`STORE_ID`),
ADD INDEX `idx_inventory_type` (`INVENTORY_TYPE`);

-- 步骤3: 创建门店库存视图（用于快速查询门店库存）
CREATE OR REPLACE VIEW `v_store_inventory` AS
SELECT 
    i.ID,
    i.PRODUCT_ID,
    i.PRODUCT_NAME,
    i.STORE_ID,
    i.STORE_NAME,
    i.CURRENT_STOCK,
    i.SAFETY_STOCK,
    i.UNIT,
    i.COST_PRICE,
    i.STOCK_VALUE,
    i.WARNING_LEVEL,
    i.INVENTORY_TYPE,
    i.LAST_UPDATE_TIME,
    i.CREATED_AT,
    i.UPDATED_AT,
    s.store_code,
    s.address,
    s.phone,
    s.manager_name
FROM INVENTORY i
LEFT JOIN stores s ON i.STORE_ID = s.store_id
WHERE i.INVENTORY_TYPE = 2 AND i.DELETED = 0;

-- 步骤4: 创建库存汇总视图（包含所有门店和仓库的库存）
CREATE OR REPLACE VIEW `v_inventory_summary` AS
SELECT 
    i.PRODUCT_ID,
    i.PRODUCT_NAME,
    i.UNIT,
    i.COST_PRICE,
    SUM(CASE WHEN i.INVENTORY_TYPE = 1 THEN i.CURRENT_STOCK ELSE 0 END) AS warehouse_stock,
    SUM(CASE WHEN i.INVENTORY_TYPE = 2 THEN i.CURRENT_STOCK ELSE 0 END) AS store_stock,
    SUM(i.CURRENT_STOCK) AS total_stock,
    SUM(i.STOCK_VALUE) AS total_value,
    COUNT(DISTINCT CASE WHEN i.INVENTORY_TYPE = 2 THEN i.STORE_ID END) AS store_count
FROM INVENTORY i
WHERE i.DELETED = 0
GROUP BY i.PRODUCT_ID, i.PRODUCT_NAME, i.UNIT, i.COST_PRICE;

-- 步骤5: 插入默认门店数据（如果不存在）
INSERT IGNORE INTO `stores` (`store_id`, `store_name`, `store_code`, `address`, `phone`, `manager_name`, `status`, `store_type`)
VALUES 
    ('STORE001', '总店', 'ST001', '北京市朝阳区建国路88号', '010-12345678', '张三', 'active', 'chain'),
    ('STORE002', '分店一', 'ST002', '北京市海淀区中关村大街100号', '010-87654321', '李四', 'active', 'chain');

-- 步骤6: 创建门店库存日志表（记录门店库存变动）
CREATE TABLE IF NOT EXISTS `store_inventory_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `inventory_id` BIGINT NOT NULL COMMENT '库存记录ID',
    `product_id` BIGINT NOT NULL COMMENT '产品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '产品名称',
    `store_id` VARCHAR(50) NOT NULL COMMENT '门店ID',
    `store_name` VARCHAR(100) NOT NULL COMMENT '门店名称',
    `type` TINYINT NOT NULL COMMENT '变动类型（1:入库,2:出库,3:调拨,4:盘点,5:损耗）',
    `before_stock` INT NOT NULL COMMENT '变动前库存',
    `after_stock` INT NOT NULL COMMENT '变动后库存',
    `change_quantity` INT NOT NULL COMMENT '变动数量',
    `operator_id` BIGINT COMMENT '操作人ID',
    `operator_name` VARCHAR(50) COMMENT '操作人姓名',
    `remark` VARCHAR(200) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
    KEY `idx_store_id` (`store_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_type` (`type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店库存日志表';
