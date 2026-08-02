-- HR集成系统邀请码模块数据库表结构
-- 创建日期：2026-01-31
-- 版本：v1.0

-- ============================================
-- 1. 入职档案表 (onboarding_archive)
-- ============================================
CREATE TABLE IF NOT EXISTS onboarding_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '档案ID',
    candidate_id VARCHAR(50) COMMENT '候选人ID（来自招聘系统）',
    candidate_name VARCHAR(100) NOT NULL COMMENT '候选人姓名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    id_card VARCHAR(18) COMMENT '身份证号',
    position VARCHAR(100) NOT NULL COMMENT '职位名称',
    position_level VARCHAR(20) NOT NULL COMMENT '职位级别：STAFF-普通员工, MANAGER-主管/经理, DIRECTOR-部门总监, EXECUTIVE-高管',
    department_id VARCHAR(50) NOT NULL COMMENT '部门ID',
    department_name VARCHAR(100) COMMENT '部门名称（冗余字段，方便查询）',
    role_id VARCHAR(50) COMMENT '角色ID',
    role_name VARCHAR(100) COMMENT '角色名称（冗余字段）',
    expected_salary DECIMAL(10,2) COMMENT '期望薪资',
    final_salary DECIMAL(10,2) COMMENT '最终薪资',
    onboard_date DATE COMMENT '预计入职日期',
    invitation_code_id BIGINT COMMENT '关联邀请码ID',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED-已创建, PENDING_HR-待HR审查, PENDING_SUBSTANTIVE-待实质审查, APPROVED-已通过, REJECTED-已拒绝, REGISTERED-已注册',
    create_by BIGINT NOT NULL COMMENT '创建人（HR）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记：0-未删除, 1-已删除',
    
    INDEX idx_status (status),
    INDEX idx_department (department_id),
    INDEX idx_position_level (position_level),
    INDEX idx_create_time (create_time),
    INDEX idx_invitation_code (invitation_code_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入职档案表';

-- ============================================
-- 2. 审批记录表 (approval_record)
-- ============================================
CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批记录ID',
    archive_id BIGINT NOT NULL COMMENT '档案ID',
    step_order INT NOT NULL COMMENT '步骤序号（1,2,3...）',
    step_name VARCHAR(50) NOT NULL COMMENT '步骤名称：HR形式审查, 部门主管审查, 部门总监审查, 总经理审批, 董事长审批',
    reviewer_id BIGINT COMMENT '审批人ID',
    reviewer_name VARCHAR(100) COMMENT '审批人姓名',
    reviewer_role VARCHAR(50) COMMENT '审批人角色',
    review_type VARCHAR(20) NOT NULL COMMENT '审查类型：FORMAL-形式审查, SUBSTANTIVE-实质审查',
    result VARCHAR(20) COMMENT '审批结果：PASSED-通过, REJECTED-拒绝, PENDING-待审批',
    comment TEXT COMMENT '审批意见',
    review_time DATETIME COMMENT '审批时间',
    signature VARCHAR(255) COMMENT '数字签名（防篡改）',
    is_abnormal TINYINT DEFAULT 0 COMMENT '是否异常：0-正常, 1-异常',
    abnormal_reason VARCHAR(500) COMMENT '异常原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_archive_id (archive_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_result (result),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- ============================================
-- 3. 邀请码发送记录表 (invitation_send_record)
-- ============================================
CREATE TABLE IF NOT EXISTS invitation_send_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    archive_id BIGINT NOT NULL COMMENT '档案ID',
    invitation_code VARCHAR(100) NOT NULL COMMENT '邀请码（唯一）',
    bound_email VARCHAR(100) COMMENT '绑定的邮箱',
    bound_phone VARCHAR(20) COMMENT '绑定的手机',
    bound_name VARCHAR(100) COMMENT '绑定的姓名',
    use_count INT DEFAULT 0 COMMENT '已使用次数',
    max_use_count INT DEFAULT 1 COMMENT '最大使用次数',
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用, USED-已使用, EXPIRED-已过期, REVOKED-已撤销',
    valid_days INT DEFAULT 7 COMMENT '有效天数',
    expire_time DATETIME COMMENT '过期时间',
    used_time DATETIME COMMENT '使用时间',
    used_by BIGINT COMMENT '使用人ID（注册用户ID）',
    send_method VARCHAR(20) COMMENT '发送方式：EMAIL-邮件, SMS-短信, BOTH-两者',
    send_status VARCHAR(20) COMMENT '发送状态：SENT-已发送, FAILED-发送失败, PENDING-待发送',
    send_time DATETIME COMMENT '发送时间',
    error_message VARCHAR(500) COMMENT '错误信息（发送失败时记录）',
    batch_id VARCHAR(50) COMMENT '批次ID（批量发送时使用）',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_invitation_code (invitation_code),
    INDEX idx_archive_id (archive_id),
    INDEX idx_status (status),
    INDEX idx_bound_email (bound_email),
    INDEX idx_bound_phone (bound_phone),
    INDEX idx_expire_time (expire_time),
    INDEX idx_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码发送记录表';

-- ============================================
-- 4. 费用记录表（财务占位符）
-- ============================================
CREATE TABLE IF NOT EXISTS invitation_expense_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '费用记录ID',
    batch_id VARCHAR(50) COMMENT '批次ID',
    archive_id BIGINT COMMENT '档案ID',
    expense_type VARCHAR(50) NOT NULL COMMENT '费用类型：EMAIL-邮件费用, SMS-短信费用',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(10,4) COMMENT '单价',
    total_amount DECIMAL(10,2) COMMENT '总金额',
    department_id VARCHAR(50) COMMENT '部门ID',
    department_name VARCHAR(100) COMMENT '部门名称',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING-待同步, SYNCED-已同步, FAILED-同步失败',
    financial_system_id VARCHAR(100) COMMENT '财务系统ID（对接后回填）',
    sync_time DATETIME COMMENT '同步时间',
    error_message VARCHAR(500) COMMENT '同步失败原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_batch_id (batch_id),
    INDEX idx_archive_id (archive_id),
    INDEX idx_department_id (department_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码费用记录表';

-- ============================================
-- 5. 提醒记录表 (invitation_reminder_log)
-- ============================================
CREATE TABLE IF NOT EXISTS invitation_reminder_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提醒记录ID',
    invitation_code_id BIGINT NOT NULL COMMENT '邀请码ID',
    archive_id BIGINT COMMENT '档案ID',
    reminder_type VARCHAR(20) NOT NULL COMMENT '提醒类型：EXPIRING_24H-24小时内过期, EXPIRED-已过期, NOT_REGISTERED-未注册提醒',
    recipient_id BIGINT NOT NULL COMMENT '接收人ID',
    recipient_name VARCHAR(100) COMMENT '接收人姓名',
    recipient_type VARCHAR(20) NOT NULL COMMENT '接收人类型：MANAGER-部门主管, HR-HR专员, CANDIDATE-候选人',
    send_method VARCHAR(20) COMMENT '发送方式：EMAIL, SMS, SYSTEM-系统通知',
    send_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '发送状态：PENDING-待发送, SENT-已发送, FAILED-发送失败',
    send_time DATETIME COMMENT '发送时间',
    read_status VARCHAR(20) DEFAULT 'UNREAD' COMMENT '阅读状态：UNREAD-未读, READ-已读',
    read_time DATETIME COMMENT '阅读时间',
    error_message VARCHAR(500) COMMENT '发送失败原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_invitation_code_id (invitation_code_id),
    INDEX idx_archive_id (archive_id),
    INDEX idx_recipient_id (recipient_id),
    INDEX idx_reminder_type (reminder_type),
    INDEX idx_send_status (send_status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码提醒记录表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入职位级别枚举说明（可选，用于参考）
-- STAFF: 普通员工
-- MANAGER: 主管/经理
-- DIRECTOR: 部门总监
-- EXECUTIVE: 副总/总经理/董监高

-- 插入审批步骤配置（可选，用于参考）
-- 普通员工：HR形式审查(1) → 直接主管实质审查(2)
-- 主管/经理：HR形式审查(1) → 部门总监实质审查(2)
-- 部门总监：HR形式审查+建议(1) → 总经理审批(2)
-- 高管：HR形式审查+建议+风险评估(1) → 董事长审批(2)

-- ============================================
-- 注释说明
-- ============================================
-- 1. 所有表都包含 create_time 和 update_time 字段，用于审计
-- 2. 使用逻辑删除（deleted字段）而非物理删除
-- 3. 邀请码表使用唯一索引确保邀请码不重复
-- 4. 审批记录表使用数字签名防止篡改
-- 5. 费用记录表预留了与财务系统对接的字段
-- 6. 提醒记录表支持多种提醒类型和接收人类型
