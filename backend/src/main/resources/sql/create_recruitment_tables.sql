-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 创建招聘需求表
CREATE TABLE IF NOT EXISTS `recruitment_requirements` (
  `id` VARCHAR(36) NOT NULL COMMENT '主键ID',
  `requirement_code` VARCHAR(50) NOT NULL COMMENT '需求编号',
  `position_name` VARCHAR(100) NOT NULL COMMENT '职位名称',
  `department_id` VARCHAR(36) COMMENT '部门ID',
  `department_name` VARCHAR(100) COMMENT '部门名称',
  `store_id` VARCHAR(36) COMMENT '门店ID',
  `store_name` VARCHAR(100) COMMENT '门店名称',
  `position_id` VARCHAR(36) COMMENT '职位ID',
  `requirement_num` INT NOT NULL DEFAULT 1 COMMENT '需求人数',
  `type` VARCHAR(20) NOT NULL DEFAULT 'hr' COMMENT '需求类型（hr：人事发布，store：门店提报）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'open' COMMENT '状态（open：进行中，closed：已关闭，filled：已招满）',
  `approval_status` VARCHAR(20) DEFAULT 'approved' COMMENT '审批状态（pending：待审批，approved：已通过，rejected：已拒绝）',
  `description` TEXT COMMENT '职位描述',
  `requirements` TEXT COMMENT '任职要求',
  `salary_range` VARCHAR(50) COMMENT '薪资范围',
  `work_location` VARCHAR(200) COMMENT '工作地点',
  `apply_deadline` DATETIME COMMENT '申请截止时间',
  `created_by` VARCHAR(36) COMMENT '创建人ID',
  `created_by_name` VARCHAR(50) COMMENT '创建人姓名',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_requirement_code` (`requirement_code`),
  KEY `idx_department` (`department_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_status` (`status`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招聘需求表';

-- 创建简历表
CREATE TABLE IF NOT EXISTS `resumes` (
  `id` VARCHAR(36) NOT NULL COMMENT '主键ID',
  `resume_code` VARCHAR(50) NOT NULL COMMENT '简历编号',
  `requirement_id` VARCHAR(36) COMMENT '关联的招聘需求ID',
  `candidate_name` VARCHAR(50) NOT NULL COMMENT '候选人姓名',
  `gender` VARCHAR(10) COMMENT '性别',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `email` VARCHAR(100) COMMENT '邮箱',
  `age` INT COMMENT '年龄',
  `education` VARCHAR(50) COMMENT '学历',
  `work_experience` INT COMMENT '工作年限',
  `current_salary` VARCHAR(50) COMMENT '期望薪资',
  `expected_salary` VARCHAR(50) COMMENT '期望薪资',
  `skills` TEXT COMMENT '技能描述',
  `experience` TEXT COMMENT '工作经历',
  `education_detail` TEXT COMMENT '教育经历',
  `self_introduction` TEXT COMMENT '自我介绍',
  `attachment_url` VARCHAR(500) COMMENT '简历附件URL',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态（pending：待处理，reviewing：筛选中，interviewed：已面试，offered：已发offer，rejected：已拒绝，hired：已录用）',
  `source` VARCHAR(20) DEFAULT 'online' COMMENT '来源（online：在线投递，referral：内部推荐，headhunter：猎头）',
  `created_by` VARCHAR(36) COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resume_code` (`resume_code`),
  KEY `idx_requirement` (`requirement_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表';

-- 创建面试记录表
CREATE TABLE IF NOT EXISTS `interviews` (
  `id` VARCHAR(36) NOT NULL COMMENT '主键ID',
  `interview_code` VARCHAR(50) NOT NULL COMMENT '面试编号',
  `resume_id` VARCHAR(36) NOT NULL COMMENT '简历ID',
  `requirement_id` VARCHAR(36) COMMENT '招聘需求ID',
  `interview_round` VARCHAR(20) NOT NULL COMMENT '面试轮次（first：初试，second：复试，final：终试）',
  `interview_type` VARCHAR(20) DEFAULT 'offline' COMMENT '面试类型（online：线上，offline：线下）',
  `interview_date` DATETIME NOT NULL COMMENT '面试时间',
  `interview_location` VARCHAR(200) COMMENT '面试地点',
  `interviewer_id` VARCHAR(36) COMMENT '面试官ID',
  `interviewer_name` VARCHAR(50) COMMENT '面试官姓名',
  `status` VARCHAR(20) NOT NULL DEFAULT 'scheduled' COMMENT '状态（scheduled：已安排，completed：已完成，cancelled：已取消）',
  `result` VARCHAR(20) COMMENT '面试结果（pass：通过，fail：未通过，pending：待定）',
  `score` INT COMMENT '面试分数',
  `feedback` TEXT COMMENT '面试反馈',
  `notes` TEXT COMMENT '备注',
  `created_by` VARCHAR(36) COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interview_code` (`interview_code`),
  KEY `idx_resume` (`resume_id`),
  KEY `idx_requirement` (`requirement_id`),
  KEY `idx_interviewer` (`interviewer_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试记录表';

-- 创建入职记录表
CREATE TABLE IF NOT EXISTS `onboarding_records` (
  `id` VARCHAR(36) NOT NULL COMMENT '主键ID',
  `onboarding_code` VARCHAR(50) NOT NULL COMMENT '入职编号',
  `resume_id` VARCHAR(36) NOT NULL COMMENT '简历ID',
  `requirement_id` VARCHAR(36) COMMENT '招聘需求ID',
  `employee_id` VARCHAR(36) COMMENT '员工ID',
  `employee_code` VARCHAR(50) COMMENT '员工编码',
  `candidate_name` VARCHAR(50) NOT NULL COMMENT '候选人姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `department_id` VARCHAR(36) COMMENT '部门ID',
  `department_name` VARCHAR(100) COMMENT '部门名称',
  `store_id` VARCHAR(36) COMMENT '门店ID',
  `store_name` VARCHAR(100) COMMENT '门店名称',
  `position_id` VARCHAR(36) COMMENT '职位ID',
  `position_name` VARCHAR(100) COMMENT '职位名称',
  `hire_date` DATE NOT NULL COMMENT '入职日期',
  `probation_start` DATE COMMENT '试用期开始日期',
  `probation_end` DATE COMMENT '试用期结束日期',
  `probation_months` INT COMMENT '试用期月数',
  `salary` DECIMAL(10,2) COMMENT '薪资',
  `registration_code` VARCHAR(50) COMMENT '注册码',
  `registration_code_status` VARCHAR(20) DEFAULT 'UNUSED' COMMENT '注册码状态（UNUSED：未使用，USED：已使用，EXPIRED：已过期）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态（pending：待入职，onboarding：入职中，completed：已完成，cancelled：已取消）',
  `notes` TEXT COMMENT '备注',
  `created_by` VARCHAR(36) COMMENT '创建人ID',
  `created_by_name` VARCHAR(50) COMMENT '创建人姓名',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_onboarding_code` (`onboarding_code`),
  KEY `idx_resume` (`resume_id`),
  KEY `idx_employee` (`employee_id`),
  KEY `idx_registration_code` (`registration_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入职记录表';
