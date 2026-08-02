package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 考勤差异明细VO
 */
@Schema(description = "考勤差异明细")
public class AttendanceDiffDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细ID */
    @Schema(description = "明细ID")
    private String detailId;

    /** 员工ID */
    @Schema(description = "员工ID")
    private String employeeId;

    /** 员工姓名 */
    @Schema(description = "员工姓名")
    private String employeeName;

    /** 日期 */
    @Schema(description = "日期")
    private String date;

    /** 排班班次 */
    @Schema(description = "排班班次")
    private String scheduleShift;

    /** 排班开始时间 */
    @Schema(description = "排班开始时间")
    private String scheduleStart;

    /** 排班结束时间 */
    @Schema(description = "排班结束时间")
    private String scheduleEnd;

    /** 实际签到时间 */
    @Schema(description = "实际签到时间")
    private String actualCheckIn;

    /** 实际签退时间 */
    @Schema(description = "实际签退时间")
    private String actualCheckOut;

    /** 差异类型: late/early_leave/missing/absent/overtime */
    @Schema(description = "差异类型: late/early_leave/missing/absent/overtime")
    private String diffType;

    /** 差异时长（分钟） */
    @Schema(description = "差异时长(分钟)")
    private Integer diffMinutes;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    public String getDetailId() { return detailId; }
    public void setDetailId(String detailId) { this.detailId = detailId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getScheduleShift() { return scheduleShift; }
    public void setScheduleShift(String scheduleShift) { this.scheduleShift = scheduleShift; }

    public String getScheduleStart() { return scheduleStart; }
    public void setScheduleStart(String scheduleStart) { this.scheduleStart = scheduleStart; }

    public String getScheduleEnd() { return scheduleEnd; }
    public void setScheduleEnd(String scheduleEnd) { this.scheduleEnd = scheduleEnd; }

    public String getActualCheckIn() { return actualCheckIn; }
    public void setActualCheckIn(String actualCheckIn) { this.actualCheckIn = actualCheckIn; }

    public String getActualCheckOut() { return actualCheckOut; }
    public void setActualCheckOut(String actualCheckOut) { this.actualCheckOut = actualCheckOut; }

    public String getDiffType() { return diffType; }
    public void setDiffType(String diffType) { this.diffType = diffType; }

    public Integer getDiffMinutes() { return diffMinutes; }
    public void setDiffMinutes(Integer diffMinutes) { this.diffMinutes = diffMinutes; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
