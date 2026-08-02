package com.foodtraceability.entity.schedule;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班冲突记录实体类
 * 记录排班冲突检查发现的问题
 *
 * 级别（level）：
 * - error: 严重冲突（必须修复才能发布）
 * - warning: 警告（建议修复）
 * - info: 提示信息
 *
 * 修复状态（fix_status）：
 * - pending: 待处理
 * - fixed: 已修复
 * - ignored: 已忽略
 */
@TableName("schedule_conflicts")
public class ScheduleConflict implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 冲突ID */
    @TableId(value = "conflict_id", type = IdType.ASSIGN_ID)
    private String conflictId;

    /** 排班方案ID */
    @TableField("plan_id")
    private String planId;

    /** 冲突级别（error/warning/info） */
    @TableField("level")
    private String level;

    /** 冲突类型（如 duplicate_shift, consecutive_days, rest_violation） */
    @TableField("type")
    private String type;

    /** 关联条目ID */
    @TableField("entry_id")
    private String entryId;

    /** 员工ID */
    @TableField("employee_id")
    private String employeeId;

    /** 员工姓名 */
    @TableField("employee_name")
    private String employeeName;

    /** 冲突日期 */
    @TableField("conflict_date")
    private LocalDate conflictDate;

    /** 班次类型 */
    @TableField("shift_type")
    private String shiftType;

    /** 关联规则ID */
    @TableField("rule_id")
    private String ruleId;

    /** 冲突描述 */
    @TableField("message")
    private String message;

    /** 修复建议 */
    @TableField("suggestion")
    private String suggestion;

    /** 是否支持自动修复 */
    @TableField("auto_fix_available")
    private Boolean autoFixAvailable;

    /** 修复状态（pending/fixed/ignored） */
    @TableField("fix_status")
    private String fixStatus;

    /** 修复时间 */
    @TableField("fixed_time")
    private LocalDateTime fixedTime;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 级别：严重 */
    public static final String LEVEL_ERROR = "error";
    /** 级别：警告 */
    public static final String LEVEL_WARNING = "warning";
    /** 级别：提示 */
    public static final String LEVEL_INFO = "info";

    /** 修复状态：待处理 */
    public static final String FIX_PENDING = "pending";
    /** 修复状态：已修复 */
    public static final String FIX_FIXED = "fixed";
    /** 修复状态：已忽略 */
    public static final String FIX_IGNORED = "ignored";

    // ==================== Getter & Setter ====================

    public String getConflictId() { return conflictId; }
    public void setConflictId(String conflictId) { this.conflictId = conflictId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getConflictDate() { return conflictDate; }
    public void setConflictDate(LocalDate conflictDate) { this.conflictDate = conflictDate; }

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

    public LocalDateTime getFixedTime() { return fixedTime; }
    public void setFixedTime(LocalDateTime fixedTime) { this.fixedTime = fixedTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
