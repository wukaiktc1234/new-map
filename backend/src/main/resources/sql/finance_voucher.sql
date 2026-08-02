-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 财务凭证表
CREATE TABLE IF NOT EXISTS finance_voucher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    voucher_no VARCHAR(50) UNIQUE COMMENT '凭证号',
    voucher_type VARCHAR(50) COMMENT '凭证类型：purchase-采购入库、sales-销售、adjustment-调整',
    business_type VARCHAR(50) COMMENT '业务类型：stockin-入库、order-订单',
    business_id BIGINT COMMENT '业务ID',
    debit_amount DECIMAL(12,2) COMMENT '借方金额',
    credit_amount DECIMAL(12,2) COMMENT '贷方金额',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '货币',
    voucher_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '凭证日期',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待审核、approved-已审核、rejected-已拒绝',
    remark TEXT COMMENT '备注',
    store_id BIGINT COMMENT '门店ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除、1-已删除',
    INDEX idx_voucher_no (voucher_no),
    INDEX idx_voucher_type (voucher_type),
    INDEX idx_business_type (business_type),
    INDEX idx_business_id (business_id),
    INDEX idx_status (status),
    INDEX idx_voucher_date (voucher_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务凭证表';