package com.foodtraceability.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 排班条目实体类
 * 存储"谁在哪天哪个班次"的最小排班单元
 */
@TableName("schedule_entries")
public class ScheduleEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "entry_id", type = IdType.ASSIGN_ID)
    private String entryId;

    /** 所属排班方案ID */
    @TableField("plan_id")
    private String planId;

    /** 员工ID */
    @TableField("employee_id")
    private Long employeeId;

    /** 员工姓名(冗余,避免联表) */
    @TableField("employee_name")
    private String employeeName;

    /** 岗位名称(冗余) */
    @TableField("position_name")
    private String positionName;

    /** 工作日期 */
    @TableField("work_date")
    private LocalDate workDate;

    /**
     * 班次类型编码
     * morning-早班 noon-中班 evening-晚班 night_off-休息
     */
    @TableField("shift_type")
    private String shiftType;

    /** 班次显示名称(如"早班(A)") */
    @TableField("shift_name")
    private String shiftName;

    /** 班次开始时间(HH:mm) */
    @TableField("start_time")
    private LocalTime startTime;

    /** 班次结束时间(HH:mm) */
    @TableField("end_time")
    private LocalTime endTime;

    /**
     * 来源标识
     * auto-自动生成 manual-手动编辑 swap-换班产生
     */
    @TableField("source")
    private String source;

    /**
     * 冲突等级
     * error-严重 warning-警告 info-提示 null-无
     */
    @TableField("conflict_level")
    private String conflictLevel;

    /** 冲突描述 */
    @TableField("conflict_message")
    private String conflictMessage;

    /** 显示顺序 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 来源常量 ====================

    /** 自动生成 */
    public static final String SOURCE_AUTO = "auto";
    /** 手动编辑 */
    public static final String SOURCE_MANUAL = "manual";
    /** 换班产生 */
    public static final String SOURCE_SWAP = "swap";

    // ==================== 冲突等级常量 ====================

    /** 严重冲突 */
    public static final String CONFLICT_LEVEL_ERROR = "error";
    /** 警告冲突 */
    public static final String CONFLICT_LEVEL_WARNING = "warning";
    /** 提示信息 */
    public static final String CONFLICT_LEVEL_INFO = "info";

    // ==================== Getter & Setter ====================

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
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

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
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

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
}
