package com.foodtraceability.controller.maintenance;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.ApprovalRecord;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.mapper.ApprovalRecordMapper;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.OnboardingArchiveMapper;
import com.foodtraceability.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据修复控制器
 * 用于修复数据库中的数据一致性问题
 *
 * @author System
 * @since 2026-03-20
 */
@RestController
@RequestMapping("/v1/admin/data-fix")
@Tag(name = "数据修复", description = "用于修复数据一致性问题的管理接口")
public class DataFixController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataFixController.class);

    public DataFixController(JdbcTemplate jdbcTemplate, OnboardingArchiveMapper archiveMapper, ApprovalRecordMapper approvalRecordMapper, EmployeeMapper employeeMapper, EmployeeService employeeService) {
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
     * 执行所有数据修复
     */
    @PostMapping("/execute")
    @Operation(summary = "执行所有数据修复", description = "修复审批记录字段、创建缺失的审批记录和员工记录")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> executeAllFixes() {
        log.info("开始执行数据修复...");
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 修复审批记录表字段
            int approvalFieldFixCount = fixApprovalRecordFields();
            result.put("approvalFieldFixCount", approvalFieldFixCount);
            // 2. 为PENDING_HR状态的档案创建审批记录
            int approvalRecordCreateCount = createMissingApprovalRecords();
            result.put("approvalRecordCreateCount", approvalRecordCreateCount);
            // 3. 为REGISTERED状态的档案创建员工记录
            int employeeCreateCount = createMissingEmployeeRecords();
            result.put("employeeCreateCount", employeeCreateCount);
            result.put("success", true);
            result.put("message", "数据修复完成");
            log.info("数据修复完成: {}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("数据修复失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.error("数据修复失败: " + e.getMessage());
        }
    }

    /**
     * 修复审批记录表缺失字段
     */
    private int fixApprovalRecordFields() {
        int count = 0;
        try {
            // 检查 step_name 字段是否存在
            Integer stepNameExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = \'approval_record\' AND COLUMN_NAME = \'step_name\'", Integer.class);
            if (stepNameExists != null && stepNameExists == 0) {
                jdbcTemplate.execute("ALTER TABLE approval_record ADD COLUMN step_name VARCHAR(100)");
                log.info("成功添加 step_name 字段");
            } else {
                log.info("step_name 字段已存在");
            }
            // 检查 review_type 字段是否存在
            Integer reviewTypeExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = \'approval_record\' AND COLUMN_NAME = \'review_type\'", Integer.class);
            if (reviewTypeExists != null && reviewTypeExists == 0) {
                jdbcTemplate.execute("ALTER TABLE approval_record ADD COLUMN review_type VARCHAR(20)");
                log.info("成功添加 review_type 字段");
            } else {
                log.info("review_type 字段已存在");
            }
            // 更新现有审批记录的字段值
            count += jdbcTemplate.update("UPDATE approval_record SET step_name = \'HR形式审查\', review_type = \'FORMAL\' WHERE step_number = 1 AND (step_name IS NULL OR step_name = \'\')");
            count += jdbcTemplate.update("UPDATE approval_record SET step_name = \'部门主管实质审查\', review_type = \'SUBSTANTIVE\' WHERE step_number = 2 AND (step_name IS NULL OR step_name = \'\')");
            log.info("更新现有审批记录字段值完成，更新数量: {}", count);
        } catch (Exception e) {
            log.warn("修复审批记录字段时出错: {}", e.getMessage());
        }
        return count;
    }

    /**
     * 为PENDING_HR状态的档案创建审批记录
     */
    private int createMissingApprovalRecords() {
        int count = 0;
        try {
            // 查询所有PENDING_HR状态的档案
            List<OnboardingArchive> pendingHrArchives = archiveMapper.selectPendingHrReview();
            log.info("找到 {} 个PENDING_HR状态的档案", pendingHrArchives.size());
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
                    count++;
                    log.info("为档案ID: {} 创建审批记录成功", archive.getId());
                }
            }
            log.info("创建缺失审批记录完成，创建数量: {}", count);
        } catch (Exception e) {
            log.error("创建缺失审批记录失败", e);
        }
        return count;
    }

    /**
     * 为REGISTERED状态的档案创建员工记录
     */
    private int createMissingEmployeeRecords() {
        int count = 0;
        try {
            // 查询所有REGISTERED状态的档案
            List<OnboardingArchive> registeredArchives = archiveMapper.selectByStatus(OnboardingArchive.STATUS_REGISTERED);
            log.info("找到 {} 个REGISTERED状态的档案", registeredArchives.size());
            for (OnboardingArchive archive : registeredArchives) {
                // 检查是否已存在员工记录
                Employee existingEmployee = employeeService.getEmployeeByCode(archive.getEmployeeCode());
                if (existingEmployee == null) {
                    // 创建员工记录
                    Employee employee = employeeService.createEmployeeFromArchive(archive);
                    if (employee != null) {
                        count++;
                        log.info("为档案ID: {} 创建员工记录成功，员工编号: {}", archive.getId(), employee.getEmployeeCode());
                    }
                } else {
                    log.info("档案ID: {} 的员工记录已存在，员工编号: {}", archive.getId(), archive.getEmployeeCode());
                }
            }
            log.info("创建缺失员工记录完成，创建数量: {}", count);
        } catch (Exception e) {
            log.error("创建缺失员工记录失败", e);
        }
        return count;
    }

    /**
     * 查看当前数据状态
     */
    @PostMapping("/status")
    @Operation(summary = "查看数据状态", description = "查看当前数据一致性状态")
    public Result<Map<String, Object>> checkDataStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            // 查询PENDING_HR状态的档案数量
            List<OnboardingArchive> pendingHrArchives = archiveMapper.selectPendingHrReview();
            status.put("pendingHrArchives", pendingHrArchives.size());
            // 查询待审批记录数量
            List<ApprovalRecord> pendingApprovals = approvalRecordMapper.selectAllPending();
            status.put("pendingApprovals", pendingApprovals.size());
            // 查询REGISTERED状态的档案数量
            List<OnboardingArchive> registeredArchives = archiveMapper.selectByStatus(OnboardingArchive.STATUS_REGISTERED);
            status.put("registeredArchives", registeredArchives.size());
            // 查询员工数量
            Long employeeCount = employeeMapper.selectCount(null);
            status.put("totalEmployees", employeeCount);
            // 检查数据一致性
            boolean hasIssues = false;
            StringBuilder issues = new StringBuilder();
            // 检查PENDING_HR档案是否有对应的待审批记录
            for (OnboardingArchive archive : pendingHrArchives) {
                List<ApprovalRecord> records = approvalRecordMapper.selectByArchiveId(archive.getId());
                boolean hasPending = records.stream().anyMatch(r -> ApprovalRecord.STATUS_PENDING.equals(r.getStatus()));
                if (!hasPending) {
                    hasIssues = true;
                    issues.append(String.format("档案ID %d (PENDING_HR) 缺少待审批记录; ", archive.getId()));
                }
            }
            // 检查REGISTERED档案是否有对应的员工记录
            for (OnboardingArchive archive : registeredArchives) {
                Employee employee = employeeService.getEmployeeByCode(archive.getEmployeeCode());
                if (employee == null) {
                    hasIssues = true;
                    issues.append(String.format("档案ID %d (REGISTERED) 缺少员工记录; ", archive.getId()));
                }
            }
            status.put("hasIssues", hasIssues);
            status.put("issues", issues.toString());
            return Result.success(status);
        } catch (Exception e) {
            log.error("检查数据状态失败", e);
            return Result.error("检查数据状态失败: " + e.getMessage());
        }
    }

    /**
     * 修复测试数据中的中文乱码
     */
    @PostMapping("/fix-test-data")
    @Operation(summary = "修复测试数据", description = "修复测试数据中的中文乱码")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> fixTestData() {
        try {
            log.info("开始修复测试数据中的中文乱码...");
            // 修复员工表测试数据
            Map<String, String[]> employeeData = new HashMap<>();
            employeeData.put("EMP_FIN_001", new String[] {"张财务", "male"});
            employeeData.put("EMP_FIN_002", new String[] {"李会计", "female"});
            employeeData.put("EMP_PUR_001", new String[] {"王采购", "male"});
            employeeData.put("EMP_PUR_002", new String[] {"赵采购", "female"});
            employeeData.put("EMP_OPS_001", new String[] {"钱运营", "male"});
            employeeData.put("EMP_OPS_002", new String[] {"孙运营", "female"});
            employeeData.put("EMP_KIT_001", new String[] {"李厨师", "male"});
            employeeData.put("EMP_KIT_002", new String[] {"周厨师", "female"});
            employeeData.put("EMP_SRV_001", new String[] {"吴服务", "male"});
            employeeData.put("EMP_SRV_002", new String[] {"郑服务", "female"});
            int employeeCount = 0;
            for (Map.Entry<String, String[]> entry : employeeData.entrySet()) {
                String code = entry.getKey();
                String name = entry.getValue()[0];
                String gender = entry.getValue()[1];
                int updated = jdbcTemplate.update("UPDATE employees SET EMPLOYEE_NAME = ?, gender = ? WHERE EMPLOYEE_CODE = ?", name, gender, code);
                employeeCount += updated;
            }
            // 修复入职档案表测试数据
            int archiveCount = jdbcTemplate.update("UPDATE onboarding_archive SET candidate_name = \'张三\' WHERE employee_code = \'EMP20260004\'");
            // 修复邀请码记录表测试数据
            int invitationCount = jdbcTemplate.update("UPDATE invitation_send_record SET bound_name = \'张三\' WHERE employee_code = \'EMP20260004\'");
            // 修复EMP20260004员工记录（通过入职创建的员工）
            int newEmployeeCount = jdbcTemplate.update("UPDATE employees SET EMPLOYEE_NAME = \'张三\', gender = \'male\' WHERE EMPLOYEE_CODE = \'EMP20260004\'");
            employeeCount += newEmployeeCount;
            // 修复部门表中文乱码
            Map<Integer, String> deptData = new HashMap<>();
            deptData.put(49, "财务部");
            deptData.put(50, "采购部");
            deptData.put(51, "运营部");
            deptData.put(52, "人力资源部");
            deptData.put(53, "厨房部");
            deptData.put(54, "服务部");
            int deptCount = 0;
            for (Map.Entry<Integer, String> entry : deptData.entrySet()) {
                int updated = jdbcTemplate.update("UPDATE departments SET DEPT_NAME = ? WHERE ID = ?", entry.getValue(), entry.getKey());
                deptCount += updated;
            }
            // 修复入职档案中的部门名称
            jdbcTemplate.update("UPDATE onboarding_archive SET department_name = \'技术部\' WHERE department_id = \'1\'");
            jdbcTemplate.update("UPDATE onboarding_archive SET department_name = \'人力资源部\' WHERE department_id = \'4\'");
            // 修复入职档案中的职位名称
            jdbcTemplate.update("UPDATE onboarding_archive SET position = \'软件工程师\' WHERE id = 6");
            jdbcTemplate.update("UPDATE onboarding_archive SET position = \'实习生\' WHERE id = 2");
            // 修复角色名称
            jdbcTemplate.update("UPDATE onboarding_archive SET role_name = \'普通员工\' WHERE role_id = \'2\'");
            // 修复劳动合同中的职位名称
            jdbcTemplate.update("UPDATE employee_labor_contract SET position = \'软件工程师\' WHERE employee_code = \'EMP20260004\'");
            // 创建技术部（ID=1），因为EMP20260004的department_id是1
            try {
                int techDeptExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM departments WHERE ID = 1", Integer.class);
                if (techDeptExists == 0) {
                    jdbcTemplate.update("INSERT INTO departments (ID, DEPT_CODE, DEPT_NAME, PARENT_ID, LEVEL, SORT_ORDER, employee_count, STATUS, DESCRIPTION) " + "VALUES (1, \'TECH001\', \'技术部\', 0, 1, 0, 1, 1, \'负责技术研发工作\')");
                    deptCount++;
                    log.info("创建技术部成功");
                }
            } catch (Exception e) {
                log.warn("创建技术部时出错: {}", e.getMessage());
            }
            log.info("测试数据修复完成，员工: {}条，档案: {}条，邀请码: {}条，部门: {}条", employeeCount, archiveCount, invitationCount, deptCount);
            return Result.success(String.format("修复完成！员工: %d条, 档案: %d条, 邀请码: %d条, 部门: %d条", employeeCount, archiveCount, invitationCount, deptCount));
        } catch (Exception e) {
            log.error("修复测试数据失败", e);
            return Result.error("修复测试数据失败: " + e.getMessage());
        }
    }

    /**
     * 创建劳动合同表并为已注册员工创建合同
     */
    @PostMapping("/create-labor-contracts")
    @Operation(summary = "创建劳动合同表", description = "创建劳动合同表并为已注册员工创建合同")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> createLaborContracts() {
        try {
            log.info("开始创建劳动合同表...");
            // 先添加onboarding_archive表的contract_id字段
            try {
                jdbcTemplate.execute("ALTER TABLE onboarding_archive ADD COLUMN contract_id BIGINT");
                log.info("添加contract_id字段成功");
            } catch (Exception e) {
                if (e.getMessage().contains("Duplicate column")) {
                    log.info("contract_id字段已存在，跳过添加");
                } else {
                    throw e;
                }
            }
            // 创建劳动合同表
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS employee_labor_contract (" + "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " + "employee_id VARCHAR(36), " + "employee_code VARCHAR(50) NOT NULL, " + "employee_name VARCHAR(100) NOT NULL, " + "contract_no VARCHAR(50) NOT NULL, " + "contract_type VARCHAR(20) NOT NULL DEFAULT \'fixed-term\', " + "start_date DATE NOT NULL, " + "end_date DATE, " + "probation_months INT DEFAULT 3, " + "probation_end_date DATE, " + "salary DECIMAL(10,2), " + "work_location VARCHAR(200), " + "position VARCHAR(100), " + "status VARCHAR(20) DEFAULT \'pending\', " + "sign_date DATE, " + "sign_method VARCHAR(20) DEFAULT \'electronic\', " + "contract_file_url VARCHAR(500), " + "remark VARCHAR(500), " + "archive_id BIGINT, " + "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " + "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " + "create_by BIGINT, " + "deleted SMALLINT DEFAULT 0, " + "CONSTRAINT uk_contract_no UNIQUE (contract_no)" + ")");
            log.info("劳动合同表创建成功");
            // 为已注册的档案创建劳动合同
            int contractCount = jdbcTemplate.update("INSERT INTO employee_labor_contract (employee_code, employee_name, contract_no, " + "contract_type, start_date, end_date, probation_months, salary, position, status, sign_method, archive_id, sign_date, create_time) " + "SELECT employee_code, candidate_name, " + "CONCAT(\'LC\', TO_CHAR(NOW(), \'YYYYMMDD\'), LPAD(SUBSTRING(employee_code, 4), 6, \'0\')), " + "\'fixed-term\', COALESCE(DATE(onboard_date), CURRENT_DATE), " + "COALESCE(DATE(onboard_date), CURRENT_DATE) + INTERVAL \'3 YEAR\', " + "3, expected_salary, position, \'active\', \'electronic\', id, " + "COALESCE(DATE(onboard_date), CURRENT_DATE), NOW() " + "FROM onboarding_archive WHERE status = \'REGISTERED\' AND deleted = 0 " + "AND NOT EXISTS (SELECT 1 FROM employee_labor_contract WHERE archive_id = onboarding_archive.id)");
            log.info("为已注册档案创建劳动合同: {}条", contractCount);
            // 更新档案的合同ID
            jdbcTemplate.update("UPDATE onboarding_archive oa SET contract_id = " + "(SELECT id FROM employee_labor_contract WHERE archive_id = oa.id LIMIT 1) " + "WHERE status = \'REGISTERED\' AND contract_id IS NULL");
            // 更新劳动合同的员工ID
            jdbcTemplate.update("UPDATE employee_labor_contract elc SET employee_id = " + "(SELECT EMPLOYEE_ID FROM employees WHERE EMPLOYEE_CODE = elc.employee_code LIMIT 1) " + "WHERE employee_id IS NULL");
            return Result.success("劳动合同表创建成功！创建合同: " + contractCount + "条");
        } catch (Exception e) {
            log.error("创建劳动合同表失败", e);
            return Result.error("创建劳动合同表失败: " + e.getMessage());
        }
    }

    /**
     * 修复users表缺失字段
     */
    @PostMapping("/fix-users-table")
    @Operation(summary = "修复users表缺失字段", description = "添加employee_code等缺失字段")
    public Result<Map<String, Object>> fixUsersTable() {
        log.info("开始修复users表字段...");
        Map<String, Object> result = new HashMap<>();
        int fixCount = 0;
        String[] columnsToAdd = {"employee_code VARCHAR(50) DEFAULT NULL", "store_id BIGINT DEFAULT NULL", "role VARCHAR(50) DEFAULT NULL", "role_names VARCHAR(255) DEFAULT NULL", "tags VARCHAR(500) DEFAULT NULL", "ext_data TEXT DEFAULT NULL", "version INT DEFAULT 1"};
        for (String columnDef : columnsToAdd) {
            String columnName = columnDef.split(" ")[0];
            try {
                Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = \'users\' AND COLUMN_NAME = \'" + columnName + "\'", Integer.class);
                if (exists != null && exists == 0) {
                    jdbcTemplate.execute("ALTER TABLE users ADD COLUMN " + columnDef);
                    log.info("成功添加 users.{} 字段", columnName);
                    fixCount++;
                } else {
                    log.info("users.{} 字段已存在", columnName);
                }
            } catch (Exception e) {
                log.warn("添加 users.{} 字段失败: {}", columnName, e.getMessage());
            }
        }
        result.put("fixCount", fixCount);
        result.put("message", "users表字段修复完成，添加了" + fixCount + "个字段");
        log.info("users表字段修复完成: {}", result);
        return Result.success(result);
    }
}
