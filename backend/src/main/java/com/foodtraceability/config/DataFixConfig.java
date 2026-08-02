package com.foodtraceability.config;

import com.foodtraceability.entity.ApprovalRecord;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.mapper.ApprovalRecordMapper;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.OnboardingArchiveMapper;
import com.foodtraceability.service.EmployeeService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据修复配置类
 * 用于修复数据库中的数据一致性问题
 *
 * @author System
 * @since 2026-03-20
 */
@Configuration
public class DataFixConfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataFixConfig.class);

    public DataFixConfig(JdbcTemplate jdbcTemplate, OnboardingArchiveMapper archiveMapper, ApprovalRecordMapper approvalRecordMapper, EmployeeMapper employeeMapper, EmployeeService employeeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.archiveMapper = archiveMapper;
        this.approvalRecordMapper = approvalRecordMapper;
        this.employeeMapper = employeeMapper;
        this.employeeService = employeeService;
    }

    private final JdbcTemplate jdbcTemplate;
    private final OnboardingArchiveMapper archiveMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final EmployeeMapper employeeMapper;
    private final EmployeeService employeeService;

    /**
     * 应用启动后执行数据修复
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(rollbackFor = Exception.class)
    public void fixDataOnStartup() {
        log.info("开始执行数据修复检查...");
        try {
            // 1. 修复审批记录表字段
            fixApprovalRecordFields();
            // 2. 为PENDING_HR状态的档案创建审批记录
            createMissingApprovalRecords();
            // 3. 为REGISTERED状态的档案创建员工记录
            createMissingEmployeeRecords();
            log.info("数据修复检查完成");
        } catch (Exception e) {
            log.error("数据修复失败", e);
        }
    }

    /**
     * 修复审批记录表缺失字段
     */
    private void fixApprovalRecordFields() {
        try {
            try {
                jdbcTemplate.queryForObject(
                    "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'APPROVAL_RECORD'",
                    String.class);
            } catch (Exception e) {
                log.info("approval_record表不存在，跳过修复审批记录字段");
                return;
            }
            try {
                jdbcTemplate.queryForObject(
                    "SELECT COLUMN_NAME FROM information_schema.columns WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'APPROVAL_RECORD' AND column_name = 'STEP_NAME'",
                    String.class);
            } catch (Exception e) {
                jdbcTemplate.execute("ALTER TABLE approval_record ADD COLUMN step_name VARCHAR(100)");
                log.info("添加 step_name 字段完成");
            }
            try {
                jdbcTemplate.queryForObject(
                    "SELECT COLUMN_NAME FROM information_schema.columns WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'APPROVAL_RECORD' AND column_name = 'REVIEW_TYPE'",
                    String.class);
            } catch (Exception e) {
                jdbcTemplate.execute("ALTER TABLE approval_record ADD COLUMN review_type VARCHAR(20)");
                log.info("添加 review_type 字段完成");
            }
            // 更新现有审批记录的字段值
            jdbcTemplate.update("UPDATE approval_record SET step_name = \'HR形式审查\', review_type = \'FORMAL\' WHERE step_number = 1 AND step_name IS NULL");
            jdbcTemplate.update("UPDATE approval_record SET step_name = \'部门主管实质审查\', review_type = \'SUBSTANTIVE\' WHERE step_number = 2 AND step_name IS NULL");
            log.info("更新现有审批记录字段值完成");
        } catch (Exception e) {
            log.warn("修复审批记录字段时出错: {}", e.getMessage());
        }
    }

    /**
     * 为PENDING_HR状态的档案创建审批记录
     */
    private void createMissingApprovalRecords() {
        try {
            try {
                jdbcTemplate.queryForObject(
                    "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'ONBOARDING_ARCHIVE'",
                    String.class);
            } catch (Exception e) {
                log.info("onboarding_archive表不存在，跳过创建缺失审批记录");
                return;
            }
            List<OnboardingArchive> pendingHrArchives = archiveMapper.selectPendingHrReview();
            for (OnboardingArchive archive : pendingHrArchives) {
                // 检查是否已存在待审批记录
                List<ApprovalRecord> existingRecords = approvalRecordMapper.selectByArchiveId(archive.getId());
                boolean hasPendingRecord = existingRecords.stream().anyMatch(r -> ApprovalRecord.STATUS_PENDING.equals(r.getStatus()));
                if (!hasPendingRecord) {
                    // 创建审批记录
                    ApprovalRecord record = new ApprovalRecord();
                    record.setArchiveId(archive.getId());
                    record.setStepNumber(1);
                    record.setStepName("HR形式审查");
                    record.setReviewType("FORMAL");
                    record.setReviewerId(1L); // 默认审批人
                    record.setStatus(ApprovalRecord.STATUS_PENDING);
                    record.setCreateTime(LocalDateTime.now());
                    record.setUpdateTime(LocalDateTime.now());
                    approvalRecordMapper.insert(record);
                    log.info("为档案ID: {} 创建审批记录成功", archive.getId());
                }
            }
            log.info("创建缺失审批记录完成，处理档案数: {}", pendingHrArchives.size());
        } catch (Exception e) {
            log.error("创建缺失审批记录失败", e);
        }
    }

    /**
     * 为REGISTERED状态的档案创建员工记录
     */
    private void createMissingEmployeeRecords() {
        try {
            try {
                jdbcTemplate.queryForObject(
                    "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'onboarding_archive'",
                    String.class);
            } catch (Exception e) {
                log.info("onboarding_archive表不存在，跳过创建缺失员工记录");
                return;
            }
            List<OnboardingArchive> registeredArchives = archiveMapper.selectByStatus(OnboardingArchive.STATUS_REGISTERED);
            int createdCount = 0;
            for (OnboardingArchive archive : registeredArchives) {
                // 检查是否已存在员工记录
                Employee existingEmployee = employeeService.getEmployeeByCode(archive.getEmployeeCode());
                if (existingEmployee == null) {
                    // 创建员工记录
                    Employee employee = employeeService.createEmployeeFromArchive(archive);
                    if (employee != null) {
                        createdCount++;
                        log.info("为档案ID: {} 创建员工记录成功，员工编号: {}", archive.getId(), employee.getEmployeeCode());
                    }
                }
            }
            log.info("创建缺失员工记录完成，处理档案数: {}，创建员工数: {}", registeredArchives.size(), createdCount);
        } catch (Exception e) {
            log.error("创建缺失员工记录失败", e);
        }
    }
}
