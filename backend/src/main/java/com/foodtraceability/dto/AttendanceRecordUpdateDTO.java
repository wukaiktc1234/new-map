package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 考勤记录更新DTO
 */
@Schema(description = "考勤记录更新DTO")
public class AttendanceRecordUpdateDTO {

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "上班打卡时间", example = "08:55:00")
    private LocalTime clockInTime;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "下班打卡时间", example = "18:05:00")
    private LocalTime clockOutTime;

    @Schema(description = "加班时长（小时）", example = "0.5")
    private BigDecimal overtimeHours;

    /**
     * 请假类型: 0正常 1事假 2病假 3年假 4调休 5其他
     */
    @Schema(description = "请假类型（0正常 1事假 2病假 3年假 4调休 5其他）", example = "0")
    private Integer leaveType;

    @Schema(description = "请假时长（小时）", example = "0")
    private BigDecimal leaveHours;

    /**
     * 考勤状态: 1正常 2迟到 3早退 4缺勤 5加班 6请假
     */
    @Schema(description = "考勤状态（1正常 2迟到 3早退 4缺勤 5加班 6请假）", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "")
    private String remark;

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

    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(BigDecimal overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public Integer getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(Integer leaveType) {
        this.leaveType = leaveType;
    }

    public BigDecimal getLeaveHours() {
        return leaveHours;
    }

    public void setLeaveHours(BigDecimal leaveHours) {
        this.leaveHours = leaveHours;
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
