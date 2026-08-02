package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 员工HR查询DTO
 * 用于HR管理场景下的员工筛选查询
 */
@Schema(description = "员工HR查询DTO")
public class EmployeeQueryDTO extends PageQuery {

    @Schema(description = "员工姓名（模糊查询）", example = "张三")
    private String name;

    @Schema(description = "员工编号", example = "EMP001")
    private String employeeNo;

    @Schema(description = "部门ID", example = "1")
    private String departmentId;

    @Schema(description = "职位ID", example = "1")
    private String positionId;

    /**
     * 员工状态: 1在职 2试用期 3离职 4退休
     */
    @Schema(description = "员工状态（1在职 2试用期 3离职 4退休）")
    private Integer employeeStatus;

    /**
     * 员工类型: 1全职 2兼职 3实习 4外包
     */
    @Schema(description = "员工类型（1全职 2兼职 3实习 4外包）")
    private Integer employeeType;

    /**
     * 性别: 0未知 1男 2女
     */
    @Schema(description = "性别（0未知 1男 2女）")
    private Integer genderCode;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "入职日期-开始", example = "2026-01-01")
    private LocalDate entryDateStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "入职日期-结束", example = "2026-04-25")
    private LocalDate entryDateEnd;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "转正日期-开始", example = "2026-01-01")
    private LocalDate regularDateStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "转正日期-结束", example = "2026-07-01")
    private LocalDate regularDateEnd;

    // ==================== Getter & Setter ====================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public Integer getEmployeeStatus() {
        return employeeStatus;
    }

    public void setEmployeeStatus(Integer employeeStatus) {
        this.employeeStatus = employeeStatus;
    }

    public Integer getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(Integer employeeType) {
        this.employeeType = employeeType;
    }

    public Integer getGenderCode() {
        return genderCode;
    }

    public void setGenderCode(Integer genderCode) {
        this.genderCode = genderCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getEntryDateStart() {
        return entryDateStart;
    }

    public void setEntryDateStart(LocalDate entryDateStart) {
        this.entryDateStart = entryDateStart;
    }

    public LocalDate getEntryDateEnd() {
        return entryDateEnd;
    }

    public void setEntryDateEnd(LocalDate entryDateEnd) {
        this.entryDateEnd = entryDateEnd;
    }

    public LocalDate getRegularDateStart() {
        return regularDateStart;
    }

    public void setRegularDateStart(LocalDate regularDateStart) {
        this.regularDateStart = regularDateStart;
    }

    public LocalDate getRegularDateEnd() {
        return regularDateEnd;
    }

    public void setRegularDateEnd(LocalDate regularDateEnd) {
        this.regularDateEnd = regularDateEnd;
    }
}
