package com.foodtraceability.controller.maintenance;

import com.foodtraceability.common.Result;
import com.foodtraceability.mapper.DepartmentMapper;
import com.foodtraceability.mapper.EmployeeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/**
 * 编码修复控制器
 * 用于修复数据库中的中文乱码问题
 */
@Tag(name = "编码修复", description = "修复数据库中文乱码")
@RestController
@RequestMapping("/v1/encoding-fix")
public class EncodingFixController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EncodingFixController.class);

    public EncodingFixController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;

    /**
     * 修复部门表中文乱码
     */
    @Operation(summary = "修复部门表乱码", description = "修复部门表中的中文乱码数据")
    @PostMapping("/fix-departments")
    public Result<String> fixDepartmentEncoding() {
        try {
            log.info("开始修复部门表中文乱码...");
            // 1. 查询所有部门
            String querySql = "SELECT ID, DEPT_NAME FROM departments WHERE DELETED = 0";
            List<Map<String, Object>> departments = jdbcTemplate.queryForList(querySql);
            log.info("找到 {} 个部门需要检查", departments.size());
            int fixedCount = 0;
            // 2. 修复每个部门的名称
            for (Map<String, Object> dept : departments) {
                Long id = ((Number) dept.get("ID")).longValue();
                String deptName = (String) dept.get("DEPT_NAME");
                if (deptName != null && !deptName.isEmpty()) {
                    try {
                        // 转换编码：latin1 -> binary -> utf8mb4
                        String fixedName = new String(deptName.getBytes("ISO-8859-1"), "UTF-8");
                        // 如果转换后的名称与原名称不同，则更新
                        if (!fixedName.equals(deptName)) {
                            String updateSql = "UPDATE departments SET DEPT_NAME = ? WHERE ID = ?";
                            jdbcTemplate.update(updateSql, fixedName, id);
                            fixedCount++;
                            log.info("修复部门名称: ID={}, 原名称={}, 修复后={}", id, deptName, fixedName);
                        }
                    } catch (Exception e) {
                        log.error("修复部门名称失败: ID={}, 名称={}", id, deptName, e);
                    }
                }
            }
            log.info("部门表修复完成，共修复 {} 条记录", fixedCount);
            return Result.success("修复完成，共修复 " + fixedCount + " 条部门记录");
        } catch (Exception e) {
            log.error("修复部门表乱码失败", e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }

    /**
     * 修复员工表中文乱码
     */
    @Operation(summary = "修复员工表乱码", description = "修复员工表中的中文乱码数据")
    @PostMapping("/fix-employees")
    public Result<String> fixEmployeeEncoding() {
        try {
            log.info("开始修复员工表中文乱码...");
            // 1. 查询所有员工
            String querySql = "SELECT employee_id, employee_name FROM employees WHERE employee_name IS NOT NULL AND employee_name != \'\'";
            List<Map<String, Object>> employees = jdbcTemplate.queryForList(querySql);
            log.info("找到 {} 个员工需要检查", employees.size());
            int fixedCount = 0;
            // 2. 修复每个员工的姓名
            for (Map<String, Object> emp : employees) {
                String employeeId = (String) emp.get("employee_id");
                String employeeName = (String) emp.get("employee_name");
                if (employeeName != null && !employeeName.isEmpty()) {
                    try {
                        // 转换编码：latin1 -> binary -> utf8mb4
                        String fixedName = new String(employeeName.getBytes("ISO-8859-1"), "UTF-8");
                        // 如果转换后的名称与原名称不同，则更新
                        if (!fixedName.equals(employeeName)) {
                            String updateSql = "UPDATE employees SET employee_name = ? WHERE employee_id = ?";
                            jdbcTemplate.update(updateSql, fixedName, employeeId);
                            fixedCount++;
                            log.info("修复员工姓名: ID={}, 原名称={}, 修复后={}", employeeId, employeeName, fixedName);
                        }
                    } catch (Exception e) {
                        log.error("修复员工姓名失败: ID={}, 名称={}", employeeId, employeeName, e);
                    }
                }
            }
            log.info("员工表修复完成，共修复 {} 条记录", fixedCount);
            return Result.success("修复完成，共修复 " + fixedCount + " 条员工记录");
        } catch (Exception e) {
            log.error("修复员工表乱码失败", e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }

    /**
     * 一键修复所有乱码
     */
    @Operation(summary = "一键修复所有乱码", description = "修复所有表中的中文乱码数据")
    @PostMapping("/fix-all")
    public Result<String> fixAllEncoding() {
        try {
            log.info("开始一键修复所有中文乱码...");
            // 修复部门表
            Result<String> deptResult = fixDepartmentEncoding();
            log.info("部门表修复结果: {}", deptResult.getMessage());
            // 修复员工表
            Result<String> empResult = fixEmployeeEncoding();
            log.info("员工表修复结果: {}", empResult.getMessage());
            return Result.success("所有乱码修复完成！部门: " + deptResult.getMessage() + "; 员工: " + empResult.getMessage());
        } catch (Exception e) {
            log.error("一键修复乱码失败", e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }
}
