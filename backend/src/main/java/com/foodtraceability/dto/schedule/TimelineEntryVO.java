package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 时间线条目VO
 * 表示某个员工在某一天的排班记录
 *
 * <p>前端渲染规则：
 * <ul>
 *   <li>shiftType=null或night_off → 显示为空白/休息格</li>
 *   <li>shiftColor用于单元格背景色</li>
 *   <li>conflictLevel=error时显示红色边框</li>
 * </ul>
 */
@Schema(description = "时间线条目VO")
public class TimelineEntryVO {

    @Schema(description = "条目ID")
    private String entryId;

    /**
     * 工作日期(YYYY-MM-DD)
     */
    @Schema(description = "工作日期", pattern = "yyyy-MM-dd")
    private String date;

    /**
     * 班次类型编码
     * morning-早班 noon-中班 evening-晚班 night_off-休息 null-无排班
     */
    @Schema(description = "班次类型编码",
            allowableValues = {"morning", "noon", "evening", "night_off"})
    private String shiftType;

    /**
     * 班次显示名称(如"早班(A)")
     */
    @Schema(description = "班次显示名称", example = "早班(A)")
    private String shiftName;

    /**
     * CSS颜色变量值
     * 如: --fts-primary, --fts-success等
     * 用于前端单元格背景色
     */
    @Schema(description = "CSS颜色变量值", example = "--fts-primary")
    private String shiftColor;

    /**
     * 班次开始时间(HH:mm)
     */
    @Schema(description = "班次开始时间", pattern = "HH:mm", example = "08:00")
    private String startTime;

    /**
     * 班次结束时间(HH:mm)
     */
    @Schema(description = "班次结束时间", pattern = "HH:mm", example = "16:00")
    private String endTime;

    /**
     * 来源标识
     * auto-自动生成 manual-手动编辑 swap-换班产生
     */
    @Schema(description = "来源标识",
            allowableValues = {"auto", "manual", "swap"})
    private String source;

    /**
     * 冲突等级
     * error-严重 warning-警告 info-提示 null-无冲突
     */
    @Schema(description = "冲突等级",
            allowableValues = {"error", "warning", "info"})
    private String conflictLevel;

    /**
     * 冲突描述信息
     */
    @Schema(description = "冲突描述信息")
    private String conflictMessage;

    // ==================== Getter & Setter ====================

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public String getShiftColor() {
        return shiftColor;
    }

    public void setShiftColor(String shiftColor) {
        this.shiftColor = shiftColor;
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
}
