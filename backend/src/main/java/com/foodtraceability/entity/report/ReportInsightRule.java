package com.foodtraceability.entity.report;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 智能洞察规则实体类
 * 对应数据库表 report_insight_rules
 */
@TableName("report_insight_rules")
public class ReportInsightRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则ID */
    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;

    /** 规则名称 */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 规则类型
     * 1-异常提醒 2-增长亮点 3-行动建议 4-趋势预警
     */
    @TableField("rule_type")
    private Integer ruleType;

    /**
     * 适用范围
     * 0-全部 1-日报 2-周报 3-月报 4-季报 5-年报 6-利润分析
     */
    @TableField("report_scope")
    private Integer reportScope;

    /** 指标key */
    @TableField("metric_key")
    private String metricKey;

    /**
     * 对比目标
     * 1-日均值 2-上周同期 3-上月同期 4-去年同期 5-目标值
     */
    @TableField("compare_target")
    private Integer compareTarget;

    /**
     * 阈值类型
     * 1-绝对值 2-百分比
     */
    @TableField("threshold_type")
    private Integer thresholdType;

    /** 阈值 */
    @TableField("threshold_value")
    private BigDecimal thresholdValue;

    /** 比较运算符 */
    @TableField("operator")
    private String operator;

    /**
     * 严重级别
     * 1-提示 2-警告 3-严重
     */
    @TableField("severity")
    private Integer severity;

    /** 消息模板 */
    @TableField("message_template")
    private String messageTemplate;

    /** 是否启用 */
    @TableField("is_enabled")
    private Boolean isEnabled;

    /** 优先级 */
    @TableField("priority")
    private Integer priority;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ========== Getter & Setter ==========

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Integer getRuleType() {
        return ruleType;
    }

    public void setRuleType(Integer ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getReportScope() {
        return reportScope;
    }

    public void setReportScope(Integer reportScope) {
        this.reportScope = reportScope;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public Integer getCompareTarget() {
        return compareTarget;
    }

    public void setCompareTarget(Integer compareTarget) {
        this.compareTarget = compareTarget;
    }

    public Integer getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(Integer thresholdType) {
        this.thresholdType = thresholdType;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Integer getSeverity() {
        return severity;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
