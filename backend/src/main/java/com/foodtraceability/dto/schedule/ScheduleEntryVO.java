package com.foodtraceability.dto.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 排班条目视图对象
 * 用于在方案详情中展示"谁在哪天哪个班次"
 */
@Schema(description = "排班条目VO")
public class ScheduleEntryVO {

    @Schema(description = "主键ID")
    private String entryId;

    @Schema(description = "员工ID")
    private Long employeeId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "岗位名称")
    private String positionName;

    @Schema(description = "工作日期", pattern = "yyyy-MM-dd")
    private String workDate;

    /**
     * 班次类型编码(morning/noon/evening/night_off)
     */
    @Schema(description = "班次类型编码",
            allowableValues = {"morning", "noon", "evening", "night_off"})
    private String shiftType;

    @Schema(description = "班次显示名称", example = "早班(A)")
    private String shiftName;

    @Schema(description = "班次开始时间", pattern = "HH:mm")
    private String startTime;

    @Schema(description = "班次结束时间", pattern = "HH:mm")
    private String endTime;

    /**
     * 来源标识(auto/manual/swap)
     */
    @Schema(description = "来源标识",
            allowableValues = {"auto", "manual", "swap"})
    private String source;

    /**
     * 冲突等级(error/warning/info/null)
     */
    @Schema(description = "冲突等级",
            allowableValues = {"error", "warning", "info"})
    private String conflictLevel;

    @Schema(description = "冲突描述")
    private String conflictMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // ==================== Getter & Setter ====================

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getConflictLevel() {
        return conflictLevel;
    }

    public void setConflictLevel(String conflictLevel) {
        this.conflictLevel = conflictLevel;
    }

    public String getConflictMessage() {
        return conflictMessage;
    }

    public void setConflictMessage(String conflictMessage) {
        this.conflictMessage = conflictMessage;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
