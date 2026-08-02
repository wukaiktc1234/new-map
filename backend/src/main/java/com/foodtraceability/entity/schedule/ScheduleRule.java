package com.foodtraceability.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 排班规则配置实体类
 * 定义冲突检测使用的业务规则
 */
@TableName("schedule_rules")
public class ScheduleRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;

    /**
     * 规则编码(唯一标识)
     * consecutive_days/rest_time/weekly_rest/duplicate_shift/on_leave_check/resigned_check/staff_shortage
     */
    @TableField("rule_code")
    private String ruleCode;

    /** 规则名称 */
    @TableField("rule_name")
    private String ruleName;

    /** 规则描述 */
    @TableField("description")
    private String description;

    /** 门店ID(规则按门店配置) */
    @TableField("store_id")
    private Long storeId;

    /**
     * 规则分类
     * hard_constraint-硬约束(必须满足) soft_constraint-软约束(建议满足)
     */
    @TableField("category")
    private String category;

    /**
     * 规则参数(JSONB,灵活配置不同规则的不同参数)
     * 示例: {"max_consecutive_days":6,"min_rest_hours":10,"min_weekly_rest_days":1}
     */
    @TableField("parameters")
    private String parameters;

    /** 优先级(数值越小优先级越高) */
    @TableField("priority")
    private Integer priority;

    /**
     * 状态
     * active-启用 inactive-停用
     */
    @TableField("status")
    private String status;

    /** 是否为系统预置规则(不可删除) */
    @TableField("is_system")
    private Boolean isSystem;

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

    // ==================== 规则编码常量 ====================

    /** 连续工作天数限制 */
    public static final String RULE_CODE_CONSECUTIVE_DAYS = "consecutive_days";
    /** 最小休息间隔 */
    public static final String RULE_CODE_REST_TIME = "rest_time";
    /** 每周最少休息天数 */
    public static final String RULE_CODE_WEEKLY_REST = "weekly_rest";
    /** 禁止一天多班 */
    public static final String RULE_CODE_DUPLICATE_SHIFT = "duplicate_shift";
    /** 员工请假检查 */
    public static final String RULE_CODE_ON_LEAVE_CHECK = "on_leave_check";
    /** 缺员预警 */
    public static final String RULE_CODE_STAFF_SHORTAGE = "staff_shortage";

    // ==================== 规则分类常量 ====================

    /** 硬约束(必须满足) */
    public static final String CATEGORY_HARD_CONSTRAINT = "hard_constraint";
    /** 软约束(建议满足) */
    public static final String CATEGORY_SOFT_CONSTRAINT = "soft_constraint";

    // ==================== 状态常量 ====================

    /** 启用状态 */
    public static final String STATUS_ACTIVE = "active";
    /** 停用状态 */
    public static final String STATUS_INACTIVE = "inactive";

    // ==================== Getter & Setter ====================

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
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
