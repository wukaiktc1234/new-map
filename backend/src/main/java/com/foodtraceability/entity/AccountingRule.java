package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会计规则实体类
 * 用于配置自动记账的匹配规则和模板关联
 * @author example
 * @since 2026-04-04
 */
@TableName("accounting_rule")
public class AccountingRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规则ID（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 规则编码（唯一标识）
     */
    @TableField("rule_code")
    private String ruleCode;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 事件类型（如：POS_SALE_CASH、PURCHASE_INBOUND）
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 事件描述
     */
    @TableField("event_description")
    private String eventDescription;

    /**
     * 关联的分录模板ID
     */
    @TableField("template_id")
    private Integer templateId;

    /**
     * 优先级（数值越大优先级越高）
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 状态：ACTIVE(启用)、INACTIVE(停用)
     */
    @TableField("status")
    private String status;

    /**
     * 是否自动执行
     */
    @TableField("auto_execute")
    private Boolean autoExecute;

    /**
     * 质量评分阈值（低于此值需要人工审核）
     */
    @TableField("quality_threshold")
    private Integer qualityThreshold;

    /**
     * 规则描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
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

    public Boolean getAutoExecute() {
        return autoExecute;
    }

    public void setAutoExecute(Boolean autoExecute) {
        this.autoExecute = autoExecute;
    }

    public Integer getQualityThreshold() {
        return qualityThreshold;
    }

    public void setQualityThreshold(Integer qualityThreshold) {
        this.qualityThreshold = qualityThreshold;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @Override
    public String toString() {
        return "AccountingRule{" +
            "id=" + id +
            ", ruleCode='" + ruleCode + '\'' +
            ", ruleName='" + ruleName + '\'' +
            ", eventType='" + eventType + '\'' +
            ", templateId=" + templateId +
            ", priority=" + priority +
            ", status='" + status + '\'' +
            ", autoExecute=" + autoExecute +
            ", qualityThreshold=" + qualityThreshold +
            '}';
    }
}
