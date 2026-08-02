package com.foodtraceability.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.Department;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.mapper.EmployeeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EmployeeCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeCodeGenerator.class);


    public EmployeeCodeGenerator(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    private final EmployeeMapper employeeMapper;

    private static final int FULL_TIME_CODE_LENGTH = 5;

    private static final int PART_TIME_CODE_LENGTH = 6;

    private static final Map<String, String> DEPARTMENT_CODE_MAP = new HashMap<>();

    static {
        DEPARTMENT_CODE_MAP.put("总经办", "GM");
        DEPARTMENT_CODE_MAP.put("人事处", "HR");
        DEPARTMENT_CODE_MAP.put("人事部", "HR");
        DEPARTMENT_CODE_MAP.put("财务室", "FIN");
        DEPARTMENT_CODE_MAP.put("财务部", "FIN");
        DEPARTMENT_CODE_MAP.put("采购办", "PUR");
        DEPARTMENT_CODE_MAP.put("采购部", "PUR");
        DEPARTMENT_CODE_MAP.put("运营部", "OPS");
        DEPARTMENT_CODE_MAP.put("仓储部", "WH");
        DEPARTMENT_CODE_MAP.put("后厨部", "KIT");
        DEPARTMENT_CODE_MAP.put("服务部", "SER");
    }

    public String getDepartmentCode(String departmentName) {
        if (departmentName == null || departmentName.isEmpty()) {
            return "QT";
        }
        return DEPARTMENT_CODE_MAP.getOrDefault(departmentName, "QT");
    }

    public String generateEmployeeCode(String departmentId, String departmentName) {
        try {
            String departmentCode = getDepartmentCode(departmentName);
            
            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
            if (departmentId != null) {
                queryWrapper.eq("department_id", departmentId);
            }
            queryWrapper.likeRight("employee_code", departmentCode);
            queryWrapper.orderByDesc("employee_code");
            
            List<Employee> employees = employeeMapper.selectList(queryWrapper);
            
            int nextSeq = 1;
            if (!employees.isEmpty()) {
                String lastCode = employees.get(0).getEmployeeCode();
                if (lastCode != null && lastCode.length() >= 6) {
                    String seqStr = lastCode.substring(2);
                    try {
                        int lastSeq = Integer.parseInt(seqStr);
                        nextSeq = lastSeq + 1;
                    } catch (NumberFormatException e) {
                        logger.warn("解析工号序号失败: {}", lastCode);
                    }
                }
            }
            
            if (nextSeq > 9999) {
                throw new RuntimeException("该部门员工数量已达上限，无法生成新工号");
            }
            
            String employeeCode = String.format("%s%04d", departmentCode, nextSeq);
            
            logger.info("生成员工工号: {}, 部门: {}, 序号: {}", employeeCode, departmentName, nextSeq);
            
            return employeeCode;
        } catch (Exception e) {
            logger.error("生成员工工号失败", e);
            throw new RuntimeException("生成员工工号失败: " + e.getMessage());
        }
    }

    public String generateEmployeeCode(LocalDate hireDate, String departmentCode, String employmentType) {
        try {
            if (hireDate == null) {
                hireDate = LocalDate.now();
            }
            
            if (departmentCode == null || departmentCode.isEmpty()) {
                departmentCode = "QT";
            }
            
            int year = hireDate.getYear();
            String yearCode = String.valueOf(year % 100);
            
            boolean isPartTime = "part-time".equals(employmentType);
            String prefix = isPartTime ? "P" : "";
            
            String codePrefix = prefix + departmentCode + yearCode;
            
            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
            queryWrapper.likeRight("employee_code", codePrefix);
            queryWrapper.orderByDesc("employee_code");
            
            List<Employee> employees = employeeMapper.selectList(queryWrapper);
            
            int nextSeq = 1;
            if (!employees.isEmpty()) {
                String lastCode = employees.get(0).getEmployeeCode();
                if (lastCode != null && lastCode.length() >= codePrefix.length() + 3) {
                    String seqStr = lastCode.substring(codePrefix.length());
                    try {
                        int lastSeq = Integer.parseInt(seqStr);
                        nextSeq = lastSeq + 1;
                    } catch (NumberFormatException e) {
                        logger.warn("解析工号序号失败: {}", lastCode);
                    }
                }
            }
            
            if (nextSeq > 999) {
                throw new RuntimeException("该部门该年份员工数量已达上限，无法生成新工号");
            }
            
            String employeeCode = String.format("%s%03d", codePrefix, nextSeq);
            
            logger.info("生成员工工号: {}, 部门缩写: {}, 年份: {}, 用工类型: {}, 序号: {}", 
                employeeCode, departmentCode, year, isPartTime ? "兼职" : "全职", nextSeq);
            
            return employeeCode;
        } catch (Exception e) {
            logger.error("生成员工工号失败", e);
            throw new RuntimeException("生成员工工号失败: " + e.getMessage());
        }
    }

    public String generateEmployeeCode(Department department) {
        if (department == null) {
            return generateEmployeeCode(null, null);
        }
        return generateEmployeeCode(department.getDepartmentId() != null ? department.getDepartmentId().toString() : null, department.getDepartmentName());
    }

    public boolean isValidEmployeeCode(String employeeCode) {
        if (employeeCode == null || employeeCode.isEmpty()) {
            return false;
        }
        
        if (employeeCode.startsWith("P")) {
            if (employeeCode.length() != PART_TIME_CODE_LENGTH) {
                return false;
            }
            String yearStr = employeeCode.substring(1, 3);
            String seqStr = employeeCode.substring(3);
            
            try {
                int year = Integer.parseInt(yearStr);
                int seq = Integer.parseInt(seqStr);
                return year >= 0 && year <= 99 && seq >= 1 && seq <= 999;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            if (employeeCode.length() != FULL_TIME_CODE_LENGTH) {
                return false;
            }
            String yearStr = employeeCode.substring(0, 2);
            String seqStr = employeeCode.substring(2);
            
            try {
                int year = Integer.parseInt(yearStr);
                int seq = Integer.parseInt(seqStr);
                return year >= 0 && year <= 99 && seq >= 1 && seq <= 999;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}
