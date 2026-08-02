package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 考勤记录VO（视图对象）
 * 用于返回给前端的考勤详情数据，包含关联信息
 */
@Schema(description = "考勤记录VO")
public class AttendanceRecordVO {

    @Schema(description = "考勤记录ID", example = "1")
    private Long recordId;

    @Schema(description = "员工ID", example = "1234567890")
    private String employeeId;

    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    @Schema(description = "员工编号", example = "EMP001")
    private String employeeNo;

    @Schema(description = "部门名称", example = "后厨部")
    private String departmentName;

    @Schema(description = "职位名称", example = "厨师")
    private String positionName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "考勤日期", example = "2026-04-25")
    private LocalDate attendanceDate;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "上班打卡时间", example = "08:55:00")
    private LocalTime clockInTime;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "下班打卡时间", example = "18:05:00")
    private LocalTime clockOutTime;

    @Schema(description = "工作时长（小时）", example = "8.5")
    private BigDecimal workHours;

    @Schema(description = "加班时长（小时）", example = "0.5")
    private BigDecimal overtimeHours;

    @Schema(description = "请假类型名称", example = "正常")
    private String leaveTypeName;

    @Schema(description = "请假时长（小时）", example = "0")
    private BigDecimal leaveHours;

    @Schema(description = "迟到分钟数", example = "0")
    private Integer lateMinutes;

    @Schema(description = "早退分钟数", example = "0")
    private Integer earlyLeaveMinutes;

    @Schema(description = "考勤状态名称", example = "正常")
    private String statusName;

    @Schema(description = "考勤状态码", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "")
    private String remark;

    // ==================== Getter & Setter ====================

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

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

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public LocalTime getClockInTime() {
        return clockInTime;
    }

    public void setClockInTime(LocalTime clockInTime) {
        this.clockInTime = clockInTime;
    }

    public LocalTime getClockOutTime() {
        return clockOutTime;
    }

    public void setClockOutTime(LocalTime clockOutTime) {
        this.clockOutTime = clockOutTime;
    }

    public BigDecimal getWorkHours() {
        return workHours;
    }

    public void setWorkHours(BigDecimal workHours) {
        this.workHours = workHours;
    }

    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(BigDecimal overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public String getLeaveTypeName() {
        return leaveTypeName;
    }

    public void setLeaveTypeName(String leaveTypeName) {
        this.leaveTypeName = leaveTypeName;
    }

    public BigDecimal getLeaveHours() {
        return leaveHours;
    }

    public void setLeaveHours(BigDecimal leaveHours) {
        this.leaveHours = leaveHours;
    }

    public Integer getLateMinutes() {
        return lateMinutes;
    }

    public void setLateMinutes(Integer lateMinutes) {
        this.lateMinutes = lateMinutes;
    }

    public Integer getEarlyLeaveMinutes() {
        return earlyLeaveMinutes;
    }

    public void setEarlyLeaveMinutes(Integer earlyLeaveMinutes) {
        this.earlyLeaveMinutes = earlyLeaveMinutes;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
