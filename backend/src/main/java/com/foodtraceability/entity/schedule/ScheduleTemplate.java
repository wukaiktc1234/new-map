package com.foodtraceability.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 排班模板实体类
 * 存储可复用的排班配置(需求量+规则组合)
 */
@TableName("schedule_templates")
public class ScheduleTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    /** 模板名称 */
    @TableField("template_name")
    private String templateName;

    /** 模板描述 */
    @TableField("description")
    private String description;

    /** 门店ID(模板按门店隔离) */
    @TableField("store_id")
    private Long storeId;

    /**
     * 时段需求矩阵(JSONB, ADR-002决策)
     * {"weekday":{"morning":3,"noon":4,"evening":2},"weekend":{...},"holiday":{...}}
     */
    @TableField("demand_matrix")
    private String demandMatrix;

    /**
     * 启用的规则ID列表(JSONB)
     * ["rule_001","rule_002",...]
     */
    @TableField("enabled_rule_ids")
    private String enabledRuleIds;

    /** 是否默认模板(每个门店仅一个) */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 状态
     * active-启用 inactive-停用
     */
    @TableField("status")
    private String status;

    /** 被使用次数 */
    @TableField("use_count")
    private Integer useCount;

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

    // ==================== 状态常量 ====================

    /** 启用状态 */
    public static final String STATUS_ACTIVE = "active";
    /** 停用状态 */
    public static final String STATUS_INACTIVE = "inactive";

    // ==================== Getter & Setter ====================

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
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

    public String getDemandMatrix() {
        return demandMatrix;
    }

    public void setDemandMatrix(String demandMatrix) {
        this.demandMatrix = demandMatrix;
    }

    public String getEnabledRuleIds() {
        return enabledRuleIds;
    }

    public void setEnabledRuleIds(String enabledRuleIds) {
        this.enabledRuleIds = enabledRuleIds;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
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
