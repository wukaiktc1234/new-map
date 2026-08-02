package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 时间线员工VO
 * 表示时间线中的一个员工行，包含该员工一周的排班和统计信息
 */
@Schema(description = "时间线员工VO")
public class TimelineEmployeeVO {

    @Schema(description = "员工ID")
    private Long employeeId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "岗位名称")
    private String positionName;

    /**
     * 该员工7天的排班条目
     * 列表长度固定为7，与TimelineResponseVO.dates一一对应
     * 无排班的日期对应null或休息条目
     */
    @Schema(description = "7天排班条目(与dates一一对应)")
    private List<TimelineEntryVO> entries;

    /**
     * 本周总工时(分钟)
     * 仅累加非休息班次的durationMinutes
     */
    @Schema(description = "本周总工时(分钟)")
    private Integer weeklyHours;

    /**
     * 连续工作天数(含当天)
     * 从当前日期向前追溯连续工作的天数
     */
    @Schema(description = "连续工作天数")
    private Integer consecutiveDays;

    /**
     * 是否有冲突
     * 任一天存在error/warning级别冲突则为true
     */
    @Schema(description = "是否有冲突")
    private Boolean hasConflict;

    // ==================== Getter & Setter ====================

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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public List<TimelineEntryVO> getEntries() {
        return entries;
    }

    public void setEntries(List<TimelineEntryVO> entries) {
        this.entries = entries;
    }

    public Integer getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(Integer weeklyHours) {
        this.weeklyHours = weeklyHours;
    }

    public Integer getConsecutiveDays() {
        return consecutiveDays;
    }

    public void setConsecutiveDays(Integer consecutiveDays) {
        this.consecutiveDays = consecutiveDays;
    }

    public Boolean getHasConflict() {
        return hasConflict;
    }

    public void setHasConflict(Boolean hasConflict) {
        this.hasConflict = hasConflict;
    }
}
