package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 培训报名员工信息DTO
 */
@Schema(description = "培训报名员工信息")
public class TrainingEnrollEmployeeDTO {

    /** 员工ID */
    @Schema(description = "员工ID")
    private String employeeId;

    /** 员工姓名 */
    @Schema(description = "员工姓名")
    private String employeeName;

    /** 员工工号 */
    @Schema(description = "员工工号")
    private String employeeCode;

    /** 部门名称 */
    @Schema(description = "部门名称")
    private String departmentName;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
