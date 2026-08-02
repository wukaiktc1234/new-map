package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考勤记录实体类
 * 用于管理员工每日考勤数据，包括打卡、请假、加班等
 */
@TableName("attendance_record")
@Schema(description = "考勤记录实体")
public class AttendanceRecord {

    /** 考勤记录ID */
    @TableId(type = IdType.AUTO, value = "record_id")
    @Schema(description = "考勤记录ID", example = "1")
    private Long recordId;

    /** 员工ID */
    @TableField("employee_id")
    @Schema(description = "员工ID", example = "1234567890")
    private String employeeId;

    /** 考勤日期 */
    @TableField("attendance_date")
    @Schema(description = "考勤日期", example = "2026-04-25")
    private LocalDate attendanceDate;

    /** 上班打卡时间 */
    @TableField("clock_in_time")
    @Schema(description = "上班打卡时间", example = "08:55:00")
    private LocalTime clockInTime;

    /** 下班打卡时间 */
    @TableField("clock_out_time")
    @Schema(description = "下班打卡时间", example = "18:05:00")
    private LocalTime clockOutTime;

    /** 工作时长（小时） */
    @TableField("work_hours")
    @Schema(description = "工作时长（小时）", example = "8.5")
    private BigDecimal workHours;

    /** 加班时长（小时） */
    @TableField("overtime_hours")
    @Schema(description = "加班时长（小时）", example = "0.5")
    private BigDecimal overtimeHours;

    /**
     * 请假类型
     * 0-正常 1-事假 2-病假 3-年假 4-调休 5-其他
     */
    @TableField("leave_type")
    @Schema(description = "请假类型（0正常 1事假 2病假 3年假 4调休 5其他）", example = "0")
    private Integer leaveType;

    /** 请假时长（小时） */
    @TableField("leave_hours")
    @Schema(description = "请假时长（小时）", example = "0")
    private BigDecimal leaveHours;

    /** 迟到分钟数 */
    @TableField("late_minutes")
    @Schema(description = "迟到分钟数", example = "0")
    private Integer lateMinutes;

    /** 早退分钟数 */
    @TableField("early_leave_minutes")
    @Schema(description = "早退分钟数", example = "0")
    private Integer earlyLeaveMinutes;

    /**
     * 考勤状态
     * 1-正常 2-迟到 3-早退 4-缺勤 5-加班 6-请假
     */
    @TableField("status")
    @Schema(description = "考勤状态（1正常 2迟到 3早退 4缺勤 5加班 6请假）", example = "1")
    private Integer status;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注", example = "")
    private String remark;

    /** 关联排班方案ID */
    @TableField("schedule_plan_id")
    @Schema(description = "关联排班方案ID", example = "1234567890")
    private String schedulePlanId;

    /** 关联排班条目ID */
    @TableField("schedule_entry_id")
    @Schema(description = "关联排班条目ID", example = "1234567890")
    private String scheduleEntryId;

    /** 班次类型(morning/noon/evening/night_off) */
    @TableField("shift_type")
    @Schema(description = "班次类型", example = "morning")
    private String shiftType;

    /** 期望上班时间 */
    @TableField("expected_start_time")
    @Schema(description = "期望上班时间")
    private LocalDateTime expectedStartTime;

    /** 期望下班时间 */
    @TableField("expected_end_time")
    @Schema(description = "期望下班时间")
    private LocalDateTime expectedEndTime;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：1-已删除，0-未删除", example = "0")
    private Integer deleted;

    // ==================== 请假类型常量 ====================

    /** 正常出勤 */
    public static final int LEAVE_TYPE_NORMAL = 0;
    /** 事假 */
    public static final int LEAVE_TYPE_PERSONAL = 1;
    /** 病假 */
    public static final int LEAVE_TYPE_SICK = 2;
    /** 年假 */
    public static final int LEAVE_TYPE_ANNUAL = 3;
    /** 调休 */
    public static final int LEAVE_TYPE_COMPENSATORY = 4;
    /** 其他 */
    public static final int LEAVE_TYPE_OTHER = 5;

    // ==================== 考勤状态常量 ====================

    /** 正常 */
    public static final int STATUS_NORMAL = 1;
    /** 迟到 */
    public static final int STATUS_LATE = 2;
    /** 早退 */
    public static final int STATUS_EARLY_LEAVE = 3;
    /** 缺勤 */
    public static final int STATUS_ABSENT = 4;
    /** 加班 */
    public static final int STATUS_OVERTIME = 5;
    /** 请假 */
    public static final int STATUS_ON_LEAVE = 6;

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

    public String getSchedulePlanId() {
        return schedulePlanId;
    }

    public void setSchedulePlanId(String schedulePlanId) {
        this.schedulePlanId = schedulePlanId;
    }

    public String getScheduleEntryId() {
        return scheduleEntryId;
    }

    public void setScheduleEntryId(String scheduleEntryId) {
        this.scheduleEntryId = scheduleEntryId;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public LocalDateTime getExpectedStartTime() {
        return expectedStartTime;
    }

    public void setExpectedStartTime(LocalDateTime expectedStartTime) {
        this.expectedStartTime = expectedStartTime;
    }

    public LocalDateTime getExpectedEndTime() {
        return expectedEndTime;
    }

    public void setExpectedEndTime(LocalDateTime expectedEndTime) {
        this.expectedEndTime = expectedEndTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
