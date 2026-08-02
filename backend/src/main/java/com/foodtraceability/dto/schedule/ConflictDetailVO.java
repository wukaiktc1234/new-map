package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 冲突详情VO
 */
@Schema(description = "排班冲突详情")
public class ConflictDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 冲突ID */
    @Schema(description = "冲突ID")
    private String conflictId;

    /** 冲突级别（error/warning/info） */
    @Schema(description = "冲突级别: error/warning/info")
    private String level;

    /** 冲突类型 */
    @Schema(description = "冲突类型")
    private String type;

    /** 相关员工ID */
    @Schema(description = "相关员工ID")
    private String employeeId;

    /** 相关员工姓名 */
    @Schema(description = "相关员工姓名")
    private String employeeName;

    /** 冲突日期 */
    @Schema(description = "冲突日期")
    private String date;

    /** 冲突班次 */
    @Schema(description = "冲突班次")
    private String shiftType;

    /** 相关规则ID */
    @Schema(description = "相关规则ID")
    private String ruleId;

    /** 冲突描述 */
    @Schema(description = "冲突描述")
    private String message;

    /** 修复建议 */
    @Schema(description = "修复建议")
    private String suggestion;

    /** 是否支持自动修复 */
    @Schema(description = "是否支持自动修复")
    private Boolean autoFixAvailable;

    /** 修复状态 */
    @Schema(description = "修复状态: pending/fixed/ignored")
    private String fixStatus;

    public String getConflictId() { return conflictId; }
    public void setConflictId(String conflictId) { this.conflictId = conflictId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public Boolean getAutoFixAvailable() { return autoFixAvailable; }
    public void setAutoFixAvailable(Boolean autoFixAvailable) { this.autoFixAvailable = autoFixAvailable; }

    public String getFixStatus() { return fixStatus; }
    public void setFixStatus(String fixStatus) { this.fixStatus = fixStatus; }
}
