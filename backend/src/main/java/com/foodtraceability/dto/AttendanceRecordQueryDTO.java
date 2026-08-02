package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 考勤记录查询DTO
 */
@Schema(description = "考勤记录查询DTO")
public class AttendanceRecordQueryDTO extends PageQuery {

    @Schema(description = "员工ID", example = "1234567890")
    private String employeeId;

    @Schema(description = "员工姓名（模糊查询）", example = "张三")
    private String employeeName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "考勤日期-开始", example = "2026-04-01")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "考勤日期-结束", example = "2026-04-30")
    private LocalDate endDate;

    /**
     * 考勤状态: 1正常 2迟到 3早退 4缺勤 5加班 6请假
     */
    @Schema(description = "考勤状态（1正常 2迟到 3早退 4缺勤 5加班 6请假）")
    private Integer status;

    /**
     * 部门ID（用于按部门筛选）
     */
    @Schema(description = "部门ID", example = "1")
    private String departmentId;

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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
}
