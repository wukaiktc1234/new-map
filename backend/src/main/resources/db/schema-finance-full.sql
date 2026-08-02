-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 会计科目表
CREATE TABLE IF NOT EXISTS accounting_subject (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '科目ID',
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '科目编码',
    name VARCHAR(50) NOT NULL COMMENT '科目名称',
    category VARCHAR(20) NOT NULL COMMENT '科目类别：资产、负债、所有者权益、成本、损益',
    type VARCHAR(20) NOT NULL COMMENT '科目类型：一级科目、二级科目、三级科目',
    parent_id BIGINT COMMENT '父科目ID',
    balance_direction VARCHAR(10) NOT NULL COMMENT '余额方向：借、贷',
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE、INACTIVE',
    description VARCHAR(200) COMMENT '科目描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_parent_id (parent_id),
    INDEX idx_code (code)
) COMMENT '会计科目表';

-- 凭证头表
CREATE TABLE IF NOT EXISTS voucher_header (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '凭证ID',
    voucher_no VARCHAR(30) NOT NULL UNIQUE COMMENT '凭证编号',
    voucher_date DATE NOT NULL COMMENT '凭证日期',
    summary VARCHAR(200) NOT NULL COMMENT '凭证摘要',
    period VARCHAR(10) NOT NULL COMMENT '会计期间（格式：YYYY-MM）',
    total_debit DECIMAL(18,2) NOT NULL COMMENT '借方合计',
    total_credit DECIMAL(18,2) NOT NULL COMMENT '贷方合计',
    status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' COMMENT '凭证状态：DRAFT(草稿)、SUBMITTED(已提交)、APPROVED(已审核)、POSTED(已记账)、CANCELLED(已作废)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_voucher_date (voucher_date),
    INDEX idx_period (period),
    INDEX idx_status (status)
) COMMENT '凭证头表';

-- 凭证行表
CREATE TABLE IF NOT EXISTS voucher_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '凭证行ID',
    voucher_id BIGINT NOT NULL COMMENT '凭证ID',
    line_no INT NOT NULL COMMENT '行号',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    subject_code VARCHAR(20) NOT NULL COMMENT '科目编码',
    subject_name VARCHAR(50) NOT NULL COMMENT '科目名称',
    summary VARCHAR(200) COMMENT '行摘要',
    debit DECIMAL(18,2) DEFAULT 0 COMMENT '借方金额',
    credit DECIMAL(18,2) DEFAULT 0 COMMENT '贷方金额',
    business_id BIGINT COMMENT '关联业务ID',
    business_type VARCHAR(20) COMMENT '业务类型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_voucher_id (voucher_id),
    INDEX idx_subject_id (subject_id),
    FOREIGN KEY (voucher_id) REFERENCES voucher_header(id) ON DELETE CASCADE
) COMMENT '凭证行表';

-- 科目余额表
CREATE TABLE IF NOT EXISTS account_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '余额ID',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    subject_code VARCHAR(20) NOT NULL COMMENT '科目编码',
    subject_name VARCHAR(50) NOT NULL COMMENT '科目名称',
    category VARCHAR(20) NOT NULL COMMENT '科目类别：asset(资产)、liability(负债)、equity(所有者权益)、cost(成本)、income(损益)',
    period VARCHAR(10) NOT NULL COMMENT '会计期间（格式：YYYY-MM）',
    begin_debit DECIMAL(18,2) DEFAULT 0 COMMENT '期初借方余额',
    begin_credit DECIMAL(18,2) DEFAULT 0 COMMENT '期初贷方余额',
    current_debit DECIMAL(18,2) DEFAULT 0 COMMENT '本期借方发生额',
    current_credit DECIMAL(18,2) DEFAULT 0 COMMENT '本期贷方发生额',
    end_debit DECIMAL(18,2) DEFAULT 0 COMMENT '期末借方余额',
    end_credit DECIMAL(18,2) DEFAULT 0 COMMENT '期末贷方余额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_subject_period (subject_id, period),
    INDEX idx_period (period),
    INDEX idx_category (category),
    FOREIGN KEY (subject_id) REFERENCES accounting_subject(id) ON DELETE CASCADE
) COMMENT '科目余额表';

-- OCR识别记录表
CREATE TABLE IF NOT EXISTS ocr_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    business_id BIGINT COMMENT '业务ID（如发票ID、凭证ID）',
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型：INVOICE（发票）、CERTIFICATE（凭证）、PACKAGE（外包装）',
    image_url VARCHAR(255) NOT NULL COMMENT '图片URL',
    ocr_result TEXT NOT NULL COMMENT '识别结果JSON',
    confidence INT DEFAULT 0 COMMENT '识别置信度（0-100）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '识别状态：SUCCESS（成功）、FAILED（失败）、PENDING（待处理）',
    error_message VARCHAR(255) COMMENT '失败原因',
    ocr_time DATETIME COMMENT '识别时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_business (business_id, business_type),
    INDEX idx_status (status),
    INDEX idx_ocr_time (ocr_time)
) COMMENT 'OCR识别记录表';

-- 初始化一级会计科目
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('1001', '库存现金', '资产', '一级科目', NULL, '借', 'ACTIVE', '存放在企业财务部门的现金'),
('1002', '银行存款', '资产', '一级科目', NULL, '借', 'ACTIVE', '企业存放在银行或其他金融机构的货币资金'),
('1122', '应收账款', '资产', '一级科目', NULL, '借', 'ACTIVE', '企业因销售商品、提供劳务等经营活动应收取的款项'),
('1221', '其他应收款', '资产', '一级科目', NULL, '借', 'ACTIVE', '企业除应收票据、应收账款、预付账款等以外的其他各种应收及暂付款项'),
('1403', '原材料', '资产', '一级科目', NULL, '借', 'ACTIVE', '企业库存的各种材料'),
('1405', '库存商品', '资产', '一级科目', NULL, '借', 'ACTIVE', '企业库存的各种商品'),
('1601', '固定资产', '资产', '一级科目', NULL, '借', 'ACTIVE', '企业持有的固定资产原价'),
('1602', '累计折旧', '资产', '一级科目', NULL, '贷', 'ACTIVE', '企业固定资产的累计折旧'),
('2001', '短期借款', '负债', '一级科目', NULL, '贷', 'ACTIVE', '企业向银行或其他金融机构等借入的期限在1年以下（含1年）的各种借款'),
('2202', '应付账款', '负债', '一级科目', NULL, '贷', 'ACTIVE', '企业因购买材料、商品和接受劳务供应等经营活动应支付的款项'),
('2211', '应付职工薪酬', '负债', '一级科目', NULL, '贷', 'ACTIVE', '企业根据有关规定应付给职工的各种薪酬'),
('2221', '应交税费', '负债', '一级科目', NULL, '贷', 'ACTIVE', '企业按照税法等规定计算应交纳的各种税费'),
('2241', '其他应付款', '负债', '一级科目', NULL, '贷', 'ACTIVE', '企业除应付票据、应付账款、预收账款、应付职工薪酬、应付利息、应付股利、应交税费、长期应付款等以外的其他各项应付、暂收的款项'),
('3001', '实收资本', '所有者权益', '一级科目', NULL, '贷', 'ACTIVE', '企业接受投资者投入的实收资本'),
('3101', '资本公积', '所有者权益', '一级科目', NULL, '贷', 'ACTIVE', '企业收到投资者出资额超出其在注册资本或股本中所占份额的部分'),
('3103', '本年利润', '所有者权益', '一级科目', NULL, '贷', 'ACTIVE', '企业当期实现的净利润（或发生的净亏损）'),
('3104', '利润分配', '所有者权益', '一级科目', NULL, '贷', 'ACTIVE', '企业利润的分配（或亏损的弥补）和历年分配（或弥补）后的余额'),
('4001', '生产成本', '成本', '一级科目', NULL, '借', 'ACTIVE', '企业进行工业性生产发生的各项生产成本'),
('4101', '制造费用', '成本', '一级科目', NULL, '借', 'ACTIVE', '企业生产车间（部门）为生产产品和提供劳务而发生的各项间接费用'),
('5001', '主营业务收入', '损益', '一级科目', NULL, '贷', 'ACTIVE', '企业确认的销售商品、提供劳务等主营业务的收入'),
('5051', '其他业务收入', '损益', '一级科目', NULL, '贷', 'ACTIVE', '企业确认的除主营业务活动以外的其他经营活动实现的收入'),
('5111', '投资收益', '损益', '一级科目', NULL, '贷', 'ACTIVE', '企业确认的投资收益或投资损失'),
('5301', '营业外收入', '损益', '一级科目', NULL, '贷', 'ACTIVE', '企业发生的各项营业外收入'),
('6001', '主营业务成本', '损益', '一级科目', NULL, '借', 'ACTIVE', '企业确认销售商品、提供劳务等主营业务收入时应结转的成本'),
('6051', '其他业务成本', '损益', '一级科目', NULL, '借', 'ACTIVE', '企业确认的除主营业务活动以外的其他经营活动所发生的支出'),
('6601', '销售费用', '损益', '一级科目', NULL, '借', 'ACTIVE', '企业销售商品和材料、提供劳务的过程中发生的各种费用'),
('6602', '管理费用', '损益', '一级科目', NULL, '借', 'ACTIVE', '企业为组织和管理企业生产经营所发生的管理费用'),
('6603', '财务费用', '损益', '一级科目', NULL, '借', 'ACTIVE', '企业为筹集生产经营所需资金等而发生的筹资费用'),
('6711', '营业外支出', '损益', '一级科目', NULL, '借', 'ACTIVE', '企业发生的各项营业外支出'),
('6801', '所得税费用', '损益', '一级科目', NULL, '借', 'ACTIVE', '企业确认的应从当期利润总额中扣除的所得税费用');
