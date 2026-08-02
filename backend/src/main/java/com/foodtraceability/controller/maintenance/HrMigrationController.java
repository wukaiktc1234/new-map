package com.foodtraceability.controller.maintenance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.foodtraceability.common.Result;

/**
 * HR模块数据库迁移控制器
 * 用于创建电子合同和员工档案相关的数据库表
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@RestController
@RequestMapping("/v1/admin/hr-migration")
@Tag(name = "HR数据库迁移", description = "HR模块数据库表结构迁移")
public class HrMigrationController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HrMigrationController.class);

    public HrMigrationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/create-tables")
    @PreAuthorize("hasAuthority('*')")
    @Operation(summary = "创建HR模块相关表", description = "创建合同模板、档案详情、电子签名等表")
    public Result<String> createHrTables() {
        log.info("开始创建HR模块数据库表...");
        int tableCount = 0;
        // 1. 创建合同模板表
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS contract_template (
                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  template_name VARCHAR(100) NOT NULL,
                  template_code VARCHAR(50) NOT NULL,
                  contract_type VARCHAR(20) NOT NULL,
                  template_content TEXT NOT NULL,
                  template_variables JSON,
                  version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
                  status VARCHAR(20) NOT NULL DEFAULT 'active',
                  description VARCHAR(500),
                  create_by BIGINT,
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  deleted SMALLINT DEFAULT 0,
                  CONSTRAINT uk_template_code UNIQUE (template_code)
                )
                """);
            log.info("合同模板表创建成功");
            tableCount++;
        } catch (Exception e) {
            log.warn("合同模板表已存在或创建失败: {}", e.getMessage());
        }
        // 2. 创建员工档案详细表
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS employee_archive_detail (
                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  archive_id BIGINT NOT NULL,
                  employee_id VARCHAR(36),
                  real_name VARCHAR(100),
                  gender VARCHAR(10),
                  birthday DATE,
                  nation VARCHAR(50),
                  political_status VARCHAR(50),
                  marital_status VARCHAR(20),
                  native_place VARCHAR(200),
                  residence_address VARCHAR(500),
                  current_address VARCHAR(500),
                  education_level VARCHAR(20),
                  graduation_school VARCHAR(200),
                  major VARCHAR(100),
                  graduation_date DATE,
                  degree VARCHAR(50),
                  work_experience JSON,
                  family_members JSON,
                  emergency_contact VARCHAR(100),
                  emergency_relationship VARCHAR(50),
                  emergency_phone VARCHAR(20),
                  emergency_address VARCHAR(500),
                  bank_name VARCHAR(100),
                  bank_branch VARCHAR(200),
                  bank_card VARCHAR(50),
                  social_security_no VARCHAR(50),
                  housing_fund_no VARCHAR(50),
                  id_card_front_url VARCHAR(500),
                  id_card_back_url VARCHAR(500),
                  diploma_url VARCHAR(500),
                  degree_certificate_url VARCHAR(500),
                  photo_url VARCHAR(500),
                  other_attachments JSON,
                  privacy_agreement_signed SMALLINT DEFAULT 0,
                  privacy_agreement_time TIMESTAMP,
                  data_accuracy_confirmed SMALLINT DEFAULT 0,
                  data_confirm_time TIMESTAMP,
                  completeness_score INT DEFAULT 0,
                  missing_fields JSON,
                  review_status VARCHAR(20) DEFAULT 'pending',
                  review_by BIGINT,
                  review_time TIMESTAMP,
                  review_comment VARCHAR(500),
                  create_by BIGINT,
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  deleted SMALLINT DEFAULT 0,
                  CONSTRAINT uk_archive_id UNIQUE (archive_id)
                )
                """);
            log.info("员工档案详细表创建成功");
            tableCount++;
        } catch (Exception e) {
            log.warn("员工档案详细表已存在或创建失败: {}", e.getMessage());
        }
        // 3. 创建电子签名记录表
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS electronic_signature (
                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  contract_id BIGINT NOT NULL,
                  signer_type VARCHAR(20) NOT NULL,
                  signer_id VARCHAR(50),
                  signer_name VARCHAR(100) NOT NULL,
                  signature_data TEXT,
                  signature_image_url VARCHAR(500),
                  sign_time TIMESTAMP,
                  sign_ip VARCHAR(50),
                  sign_device VARCHAR(200),
                  verify_code VARCHAR(10),
                  verify_time TIMESTAMP,
                  status VARCHAR(20) NOT NULL DEFAULT 'pending',
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  deleted SMALLINT DEFAULT 0
                )
                """);
            log.info("电子签名记录表创建成功");
            tableCount++;
        } catch (Exception e) {
            log.warn("电子签名记录表已存在或创建失败: {}", e.getMessage());
        }
        // 4. 创建合同正文表
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS contract_document (
                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  contract_id BIGINT NOT NULL,
                  html_content TEXT NOT NULL,
                  template_id BIGINT,
                  template_version VARCHAR(50),
                  document_hash VARCHAR(64),
                  version INTEGER NOT NULL DEFAULT 1,
                  source_type VARCHAR(20) NOT NULL DEFAULT 'manual',
                  source_desc VARCHAR(200),
                  created_by VARCHAR(50),
                  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_by VARCHAR(50),
                  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  is_current INTEGER NOT NULL DEFAULT 1,
                  edit_remark VARCHAR(200),
                  deleted INTEGER NOT NULL DEFAULT 0
                )
                """);
            // 创建索引：按合同ID查询版本历史
            try {
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_contract_document_contract_id ON contract_document (contract_id)");
            } catch (Exception idxEx) {
                log.info("合同正文表contract_id索引已存在");
            }
            // 创建组合索引：按合同ID查询当前版本
            try {
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_contract_document_current ON contract_document (contract_id, is_current)");
            } catch (Exception idxEx) {
                log.info("合同正文表current索引已存在");
            }
            log.info("合同正文表创建成功");
            tableCount++;
        } catch (Exception e) {
            log.warn("合同正文表已存在或创建失败: {}", e.getMessage());
        }
        // 5. 扩展合同表字段
        try {
            jdbcTemplate.execute("ALTER TABLE employee_labor_contract ADD COLUMN template_id BIGINT");
        } catch (Exception e) {
            log.info("template_id字段已存在");
        }
        try {
            jdbcTemplate.execute("ALTER TABLE employee_labor_contract ADD COLUMN generated_time TIMESTAMP");
        } catch (Exception e) {
            log.info("generated_time字段已存在");
        }
        try {
            jdbcTemplate.execute("ALTER TABLE employee_labor_contract ADD COLUMN pdf_file_url VARCHAR(500)");
        } catch (Exception e) {
            log.info("pdf_file_url字段已存在");
        }
        try {
            jdbcTemplate.execute("ALTER TABLE employee_labor_contract ADD COLUMN signed_pdf_url VARCHAR(500)");
        } catch (Exception e) {
            log.info("signed_pdf_url字段已存在");
        }
        try {
            jdbcTemplate.execute("ALTER TABLE employee_labor_contract ADD COLUMN company_sign_time TIMESTAMP");
        } catch (Exception e) {
            log.info("company_sign_time字段已存在");
        }
        try {
            jdbcTemplate.execute("ALTER TABLE employee_labor_contract ADD COLUMN employee_sign_time TIMESTAMP");
        } catch (Exception e) {
            log.info("employee_sign_time字段已存在");
        }
        log.info("合同表字段扩展完成");
        // 6. 插入默认合同模板
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contract_template WHERE template_code = \'STANDARD_LABOR_CONTRACT\'", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("""
                    INSERT INTO contract_template (template_name, template_code, contract_type, template_content, template_variables, status, description)
                    VALUES (
                      \'标准劳动合同模板\',
                      \'STANDARD_LABOR_CONTRACT\',
                      \'fixed-term\',
                      \'<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>劳动合同</title><style>body{font-family:SimSun,serif;line-height:1.8;margin:40px;}h1{text-align:center;font-size:22px;margin-bottom:30px;}h2{font-size:16px;margin-top:20px;}p{text-indent:2em;margin:10px 0;}.signature-area{margin-top:50px;}.signature-box{display:inline-block;width:45%;text-align:center;}</style></head><body><h1>劳动合同书</h1><p>甲方（用人单位）：________________________</p><p>乙方（劳动者）：{{employeeName}}</p><h2>一、合同期限</h2><p>本合同为{{contractTypeText}}合同，自{{startDate}}起至{{endDate}}止。</p><p>其中试用期为{{probationMonths}}个月。</p><h2>二、工作内容和工作地点</h2><p>乙方同意在甲方{{departmentName}}部门从事{{position}}工作。</p><p>工作地点：{{workLocation}}</p><h2>三、劳动报酬</h2><p>乙方试用期工资为人民币{{probationSalary}}元/月。</p><p>转正后工资为人民币{{salary}}元/月。</p><h2>四、社会保险和福利待遇</h2><p>甲方依法为乙方缴纳社会保险和住房公积金。</p><div class=\"signature-area\"><div class=\"signature-box\"><p>甲方（盖章）：</p><p>日期：______年______月______日</p></div><div class=\"signature-box\"><p>乙方（签字）：</p><p>日期：______年______月______日</p></div></div></body></html>\',
                      \'[{\"name\":\"employeeName\",\"label\":\"员工姓名\",\"type\":\"text\"},{\"name\":\"departmentName\",\"label\":\"部门名称\",\"type\":\"text\"},{\"name\":\"position\",\"label\":\"职位\",\"type\":\"text\"},{\"name\":\"salary\",\"label\":\"薪资\",\"type\":\"number\"},{\"name\":\"startDate\",\"label\":\"开始日期\",\"type\":\"date\"},{\"name\":\"endDate\",\"label\":\"结束日期\",\"type\":\"date\"},{\"name\":\"probationMonths\",\"label\":\"试用期月数\",\"type\":\"number\"},{\"name\":\"probationSalary\",\"label\":\"试用期薪资\",\"type\":\"number\"},{\"name\":\"workLocation\",\"label\":\"工作地点\",\"type\":\"text\"},{\"name\":\"contractTypeText\",\"label\":\"合同类型文本\",\"type\":\"text\"}]\',
                      \'active\',
                      \'标准固定期限劳动合同模板，适用于大多数员工\'
                    )
                    """);
                log.info("默认合同模板插入成功");
            }
        } catch (Exception e) {
            log.warn("默认合同模板插入失败: {}", e.getMessage());
        }
        log.info("HR模块数据库表创建完成，共创建 {} 个表", tableCount);
        return Result.success("成功创建 " + tableCount + " 个数据库表");
    }
}
